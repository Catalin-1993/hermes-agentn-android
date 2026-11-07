package com.mobilefork.hermesagent.data

import android.content.Context

enum class AgentTone { Concise, Friendly, Professional }

enum class AgentPurpose { Productivity, Coding, Phone, Research, Learning }

/** What the person answered in the setup guide, kept so the guide can be opened again later. */
data class OnboardingAnswers(
    val userName: String = "",
    val userAvatar: String = "",
    val tone: AgentTone? = null,
    val purposes: Set<AgentPurpose> = emptySet(),
)

class OnboardingStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isDone(): Boolean = prefs.getBoolean(KEY_DONE, false)

    /**
     * Only a fresh install opens the guide by itself: someone updating already set the app up.
     * Never under instrumentation, where it would cover the screens the tests drive.
     */
    fun shouldOpenOnFirstLaunch(): Boolean {
        if (isDone() || runningUnderInstrumentation()) return false
        @Suppress("DEPRECATION")
        val info = runCatching { appContext.packageManager.getPackageInfo(appContext.packageName, 0) }.getOrNull()
            ?: return false
        return info.firstInstallTime > 0 && info.firstInstallTime == info.lastUpdateTime
    }

    fun answers(): OnboardingAnswers = OnboardingAnswers(
        userName = prefs.getString(KEY_USER_NAME, "").orEmpty(),
        userAvatar = prefs.getString(KEY_USER_AVATAR, "").orEmpty(),
        tone = prefs.getString(KEY_TONE, null)?.let { name -> AgentTone.entries.firstOrNull { it.name == name } },
        purposes = prefs.getStringSet(KEY_PURPOSES, emptySet()).orEmpty()
            .mapNotNull { name -> AgentPurpose.entries.firstOrNull { it.name == name } }
            .toSet(),
    )

    fun lastGeneratedPersona(): String = prefs.getString(KEY_LAST_PERSONA, "").orEmpty()

    fun finish(answers: OnboardingAnswers, generatedPersona: String) {
        prefs.edit()
            .putBoolean(KEY_DONE, true)
            .putString(KEY_USER_NAME, answers.userName.trim())
            .putString(KEY_USER_AVATAR, answers.userAvatar)
            .putString(KEY_TONE, answers.tone?.name)
            .putStringSet(KEY_PURPOSES, answers.purposes.map { it.name }.toSet())
            .putString(KEY_LAST_PERSONA, generatedPersona)
            .apply()
    }

    /** Leaving the guide early still counts: it never opens by itself again. */
    fun skip() {
        prefs.edit().putBoolean(KEY_DONE, true).apply()
    }

    private companion object {
        const val PREFS_NAME = "hermes_onboarding"
        const val KEY_DONE = "done"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_AVATAR = "user_avatar"
        const val KEY_TONE = "tone"
        const val KEY_PURPOSES = "purposes"
        const val KEY_LAST_PERSONA = "last_persona"

        fun runningUnderInstrumentation(): Boolean =
            runCatching { Class.forName("androidx.test.platform.app.InstrumentationRegistry") }.isSuccess
    }
}

/**
 * The main agent's instructions from the guide's answers. Short, in English for the model, and
 * only about manner: the agent stays a generalist whatever is chosen.
 */
fun onboardingPersona(agentName: String, answers: OnboardingAnswers): String = buildString {
    val name = agentName.trim()
    if (name.isNotEmpty() && name != AgentProfile.Main.name) append("Your name is ").append(name).append(". ")
    val user = answers.userName.trim()
    if (user.isNotEmpty()) append("The person you help is called ").append(user).append(". ")
    when (answers.tone) {
        AgentTone.Concise -> append("Keep answers short and to the point. ")
        AgentTone.Friendly -> append("Be warm and friendly. ")
        AgentTone.Professional -> append("Be precise and professional. ")
        null -> Unit
    }
    if (answers.purposes.isNotEmpty()) {
        val uses = answers.purposes.sortedBy { it.ordinal }.joinToString(", ") {
            when (it) {
                AgentPurpose.Productivity -> "productivity"
                AgentPurpose.Coding -> "coding"
                AgentPurpose.Phone -> "controlling this phone"
                AgentPurpose.Research -> "research"
                AgentPurpose.Learning -> "learning"
            }
        }
        append("They mainly use you for ").append(uses).append(", but help with anything they ask.")
    }
}.trim()

/**
 * The persona to save: the guide's text replaces the previous guide text or an empty persona,
 * and never instructions the person wrote themselves.
 */
fun personaAfterOnboarding(current: String, lastGenerated: String, generated: String): String =
    if (current.isBlank() || current.trim() == lastGenerated.trim()) generated else current
