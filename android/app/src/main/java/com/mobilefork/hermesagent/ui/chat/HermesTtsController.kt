package com.mobilefork.hermesagent.ui.chat

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import com.mobilefork.hermesagent.data.AppSettingsStore
import com.mobilefork.hermesagent.data.SecureSecretsStore
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

    companion object {
        const val SYSTEM = "system"
        const val ELEVENLABS = "elevenlabs"
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

    private fun speakWithSystem(text: String): Boolean {
        if (!ready) return false
        textToSpeech.stop()
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hermes-${System.currentTimeMillis()}")
        return true
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
