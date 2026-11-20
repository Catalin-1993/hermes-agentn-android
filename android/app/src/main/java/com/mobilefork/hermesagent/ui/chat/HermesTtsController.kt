package com.mobilefork.hermesagent.ui.chat

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import com.mobilefork.hermesagent.data.AppSettingsStore
import com.mobilefork.hermesagent.data.SecureSecretsStore
import com.mobilefork.hermesagent.voice.LocalTtsEngine
import com.mobilefork.hermesagent.voice.LocalTtsRequest
import com.mobilefork.hermesagent.voice.VoiceModelCatalog
import com.mobilefork.hermesagent.voice.VoiceModelStore
import com.mobilefork.hermesagent.voice.VoiceSamples
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Which voice reads replies aloud, kept on the phone. The ElevenLabs key lives in the encrypted store. */
class VoiceSettings(context: Context) {
    private val app = context.applicationContext
    private val preferences = app.getSharedPreferences("brain_voice", Context.MODE_PRIVATE)

    var provider: String
        get() = preferences.getString("provider", SYSTEM) ?: SYSTEM
        set(value) = preferences.edit().putString("provider", value).apply()

    var elevenLabsVoiceId: String
        get() = preferences.getString("elevenlabs_voice", DEFAULT_ELEVENLABS_VOICE).orEmpty().ifBlank { DEFAULT_ELEVENLABS_VOICE }
        set(value) = preferences.edit().putString("elevenlabs_voice", value.trim()).apply()

    var elevenLabsKey: String
        get() = runCatching { SecureSecretsStore(app).loadApiKey("elevenlabs") }.getOrDefault("")
        set(value) = runCatching { SecureSecretsStore(app).saveApiKey("elevenlabs", value.trim()) }.let { }

    val usesElevenLabs: Boolean get() = provider == ELEVENLABS && elevenLabsKey.isNotBlank()

    /** The person chose the phone's own recognizer when offered the download: do not ask again. */
    var skipLocalModelPrompt: Boolean
        get() = preferences.getBoolean("skip_local_prompt", false)
        set(value) = preferences.edit().putBoolean("skip_local_prompt", value).apply()

    /** Whether the microphone uses the speech model on this phone, once it is downloaded. */
    var useLocalRecognition: Boolean
        get() = preferences.getBoolean("local_recognition", true)
        set(value) = preferences.edit().putBoolean("local_recognition", value).apply()

    /** The voice model on this phone that reads replies when the provider is [LOCAL]. Empty: none chosen. */
    var localVoiceModel: String
        get() = preferences.getString("local_voice_model", "").orEmpty()
        set(value) = preferences.edit().putString("local_voice_model", value).apply()

    /** Which of a model's voices to use. Kokoro starts on af_heart, the others on their first voice. */
    fun localVoice(modelId: String): Int =
        preferences.getInt("local_voice_$modelId", if (modelId == "kokoro-82m") 3 else 0)

    fun setLocalVoice(modelId: String, voice: Int) {
        preferences.edit().putInt("local_voice_$modelId", voice.coerceAtLeast(0)).apply()
    }

    var localVoiceSpeed: Float
        get() = preferences.getFloat("local_voice_speed", 1f).coerceIn(0.6f, 1.6f)
        set(value) = preferences.edit().putFloat("local_voice_speed", value.coerceIn(0.6f, 1.6f)).apply()

    /** The downloaded speech detector that hears when the person starts and stops speaking. Empty: volume only. */
    var speechDetector: String
        get() = preferences.getString("speech_detector", "").orEmpty()
        set(value) = preferences.edit().putString("speech_detector", value).apply()

    companion object {
        const val SYSTEM = "system"
        const val ELEVENLABS = "elevenlabs"
        const val LOCAL = "local"
        const val DEFAULT_ELEVENLABS_VOICE = "21m00Tcm4TlvDq8ikWAM"
    }
}

/**
 * Reads text aloud with the phone's own voice in the app's language, or with ElevenLabs when a key is
 * set. If the online voice fails (no network, a bad key), the phone's voice reads it instead.
 */
class HermesTtsController(context: Context) : TextToSpeech.OnInitListener {
    private val app = context.applicationContext
    private val textToSpeech = TextToSpeech(app, this)
    private val settings = VoiceSettings(app)
    private var ready = false
    private var player: MediaPlayer? = null

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            val tag = AppSettingsStore(app).load().languageTag
            val wanted = if (tag.isBlank()) Locale.getDefault() else Locale.forLanguageTag(tag)
            val result = textToSpeech.setLanguage(wanted)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech.language = Locale.getDefault()
            }
        }
    }

    fun speak(text: String): Boolean {
        if (text.isBlank()) return false
        if (settings.provider == VoiceSettings.LOCAL && speakLocal(text)) return true
        if (settings.usesElevenLabs) {
            stopPlayer()
            Thread {
                val file = runCatching { fetchElevenLabs(text.take(2_500)) }.getOrNull()
                if (file != null && playFile(file)) return@Thread
                speakWithSystem(text)
            }.start()
            return true
        }
        return speakWithSystem(text)
    }

    /**
     * Reads with the voice model chosen on this phone. False when there is none, it is not downloaded,
     * it does not speak the app's language, or it has no recording to copy: the phone's own voice then reads.
     */
    private fun speakLocal(text: String): Boolean {
        val request = localRequest(app, text) ?: return false
        stopPlayer()
        textToSpeech.stop()
        LocalTtsEngine.speak(app, request) { failed, _ -> if (failed) speakWithSystem(text) }
        return true
    }

    private fun speakWithSystem(text: String): Boolean {
        if (!ready) return false
        LocalTtsEngine.stop()
        textToSpeech.stop()
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hermes-${System.currentTimeMillis()}")
        return true
    }

    companion object {
        /** The request for the voice chosen on this phone, or null when it cannot read [text] (see [speakLocal]'s rules). */
        fun localRequest(context: Context, text: String): LocalTtsRequest? {
            val app = context.applicationContext
            val settings = VoiceSettings(app)
            if (settings.provider != VoiceSettings.LOCAL) return null
            val spec = VoiceModelCatalog.find(settings.localVoiceModel)?.takeIf { it.isVoice } ?: return null
            if (!VoiceModelStore.isReady(app, spec)) return null
            val tag = AppSettingsStore(app).load().languageTag.ifBlank { Locale.getDefault().toLanguageTag() }
            if (!spec.speaks(tag)) return null
            val reference = if (spec.canClone) VoiceSamples.referenceFor(app, spec) ?: return null else null
            return LocalTtsRequest(
                spec = spec,
                text = text,
                languageTag = tag,
                voice = settings.localVoice(spec.id),
                speed = settings.localVoiceSpeed,
                reference = reference,
            )
        }
    }

    private fun fetchElevenLabs(text: String): File = ElevenLabsVoice.fetch(app, settings, text)

    private fun playFile(file: File): Boolean = runCatching {
        val next = MediaPlayer()
        next.setDataSource(file.absolutePath)
        next.setOnCompletionListener { it.release(); file.delete() }
        next.prepare()
        next.start()
        player = next
        true
    }.getOrDefault(false)

    private fun stopPlayer() {
        runCatching { player?.stop(); player?.release() }
        player = null
    }

    fun shutdown() {
        LocalTtsEngine.stop()
        stopPlayer()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}

/** One online voice request: the text in, an mp3 file out. */
object ElevenLabsVoice {
    fun fetch(context: Context, settings: VoiceSettings, text: String): File {
        val connection = URL("https://api.elevenlabs.io/v1/text-to-speech/${settings.elevenLabsVoiceId}").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 8_000
        connection.readTimeout = 30_000
        connection.doOutput = true
        connection.setRequestProperty("xi-api-key", settings.elevenLabsKey)
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "audio/mpeg")
        val body = org.json.JSONObject().put("text", text).put("model_id", "eleven_multilingual_v2").toString()
        connection.outputStream.use { it.write(body.toByteArray()) }
        if (connection.responseCode != 200) error("ElevenLabs answered ${connection.responseCode}")
        val file = File(context.applicationContext.cacheDir, "voice-${System.currentTimeMillis()}.mp3")
        connection.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
        return file
    }
}
