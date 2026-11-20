package com.mobilefork.hermesagent.desktop

import android.content.Context
import com.mobilefork.hermesagent.data.SecureSecretsStore

/** Where one conversation is answered: by the agent on this phone, or by the agent on the linked desktop. */
enum class ConversationMode { Local, Desktop }

/** What the person said the last time the app found their desktop. */
enum class DesktopOfferAnswer { Ask, StayLocal }

/** The desktop this phone is linked to. The key never leaves [DesktopLinkStore]'s encrypted storage. */
data class DesktopLink(val url: String, val name: String)

/**
 * The linked desktop, the answer to "connect or stay local", and the mode of each conversation.
 * The phone's own agent is the default everywhere: nothing here turns a conversation to the desktop
 * unless the person chose it.
 */
class DesktopLinkStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val secrets by lazy { SecureSecretsStore(appContext) }

    fun link(): DesktopLink? {
        val url = prefs.getString(KEY_URL, "").orEmpty()
        if (url.isBlank()) return null
        return DesktopLink(url = url, name = prefs.getString(KEY_NAME, "").orEmpty())
    }

    fun key(): String = if (link() == null) "" else secrets.loadApiKey(SECRET_NAME)

    fun save(pairing: DesktopPairing) {
        val previous = link()?.url
        prefs.edit()
            .putString(KEY_URL, pairing.url)
            .putString(KEY_NAME, pairing.name.ifBlank { pairing.url.substringAfter("://").substringBefore('/') })
            .apply {
                // A different desktop is a new question, so the earlier "stay local" no longer applies.
                if (previous != null && previous != pairing.url) putString(KEY_ANSWER, DesktopOfferAnswer.Ask.name)
            }
            .apply()
        secrets.saveApiKey(SECRET_NAME, pairing.key)
    }

    fun forget() {
        prefs.edit().remove(KEY_URL).remove(KEY_NAME).remove(KEY_ANSWER).apply()
        secrets.saveApiKey(SECRET_NAME, "")
    }

    fun answer(): DesktopOfferAnswer =
        runCatching { DesktopOfferAnswer.valueOf(prefs.getString(KEY_ANSWER, null).orEmpty()) }
            .getOrDefault(DesktopOfferAnswer.Ask)

    fun setAnswer(answer: DesktopOfferAnswer) {
        prefs.edit().putString(KEY_ANSWER, answer.name).apply()
    }

    /** A conversation answers locally unless it was explicitly turned to the desktop. */
    fun modeOf(sessionId: String): ConversationMode =
        if (prefs.getStringSet(KEY_DESKTOP_SESSIONS, emptySet()).orEmpty().contains(sessionId)) {
            ConversationMode.Desktop
        } else {
            ConversationMode.Local
        }

    fun setMode(sessionId: String, mode: ConversationMode) {
        val sessions = prefs.getStringSet(KEY_DESKTOP_SESSIONS, emptySet()).orEmpty().toMutableSet()
        if (mode == ConversationMode.Desktop) sessions.add(sessionId) else sessions.remove(sessionId)
        prefs.edit().putStringSet(KEY_DESKTOP_SESSIONS, sessions).apply()
    }

    private companion object {
        const val PREFS = "brain_desktop_link"
        const val KEY_URL = "url"
        const val KEY_NAME = "name"
        const val KEY_ANSWER = "answer"
        const val KEY_DESKTOP_SESSIONS = "desktop_sessions"
        const val SECRET_NAME = "desktop-gateway"
    }
}

/**
 * When to ask "connect to the desktop or stay local?". It asks once per sighting: only when a linked
 * desktop answers, the person has not said to stay local, and this app session has not asked yet.
 */
object DesktopOfferPolicy {
    fun shouldAsk(
        link: DesktopLink?,
        probe: DesktopProbeResult?,
        answer: DesktopOfferAnswer,
        alreadyAskedThisSession: Boolean,
    ): Boolean =
        link != null &&
            probe is DesktopProbeResult.Reachable &&
            answer == DesktopOfferAnswer.Ask &&
            !alreadyAskedThisSession
}
