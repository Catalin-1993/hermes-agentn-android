package com.mobilefork.hermesagent.ui.calls

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.ui.chat.ChatViewModel
import com.mobilefork.hermesagent.ui.i18n.CallStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID

enum class CallPhase { Ringing, Listening, Thinking, Speaking, Ended }

data class VoiceCallState(
    val phase: CallPhase = CallPhase.Ringing,
    val seconds: Int = 0,
    val muted: Boolean = false,
    val speakerOn: Boolean = true,
    /** What the person is saying, or said last. */
    val heard: String = "",
    /** What the agent said last, as plain text. */
    val reply: String = "",
    val error: String = "",
)

/**
 * A voice call with one agent. The person's words are recognised, sent to the agent in its own chat
 * (so the call is kept there, in writing), and its answer is read aloud before listening again.
 * Everything runs on the main thread, where speech recognition has to.
 */
class VoiceCallController(
    context: Context,
    private val chat: ChatViewModel,
    private val agent: AgentProfile,
    languageTag: String,
    private val words: CallStrings,
    private val onFinished: (CallRecord) -> Unit,
) {
    private val context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val locale = Locale.forLanguageTag(languageTag.ifBlank { Locale.getDefault().toLanguageTag() })
    private val audio = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val _state = MutableStateFlow(VoiceCallState())
    val state: StateFlow<VoiceCallState> = _state.asStateFlow()

    private val startedAtWall = System.currentTimeMillis()
    private var connectedAt = 0L
    private var ended = false
    private var recognizer: SpeechRecognizer? = null
    private var recognizerErrors = 0
    private var speech: TextToSpeech? = null
    private var speechReady = false
    private var speechFailed = false
    private var queuedSpeech: Pair<String, () -> Unit>? = null
    private var whenSpeechDone: (() -> Unit)? = null
    private var tones: ToneGenerator? = null
    private var focus: AudioFocusRequest? = null
    private var turn: Job? = null
    private var previousMode = AudioManager.MODE_NORMAL

    /** Rings for a moment, as a phone does, then the agent picks up and greets. */
    fun start() {
        if (chat.uiState.value.isSending) {
            _state.update { it.copy(error = words.busy) }
            scope.launch { delay(1600); finish() }
            return
        }
        chat.openAgentChat(agent.id)
        beginAudio()
        speech = TextToSpeech(context) { status ->
            handler.post {
                if (ended) return@post
                if (status == TextToSpeech.SUCCESS) {
                    speechReady = true
                    speech?.language = locale
                    speech?.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    speech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit
                        override fun onDone(utteranceId: String?) {
                            if (utteranceId == LAST_UTTERANCE) handler.post { speechDone() }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            if (utteranceId == LAST_UTTERANCE) handler.post { speechDone() }
                        }
                    })
                    queuedSpeech?.let { (text, done) -> queuedSpeech = null; speak(text, done) }
                } else {
                    speechFailed = true
                    queuedSpeech?.let { (_, done) -> queuedSpeech = null; done() }
                }
            }
        }
        scope.launch {
            runCatching {
                tones = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 45).also { it.startTone(ToneGenerator.TONE_SUP_RINGTONE, RING_MS) }
            }
            delay(RING_MS.toLong())
            runCatching { tones?.stopTone(); tones?.release() }
            tones = null
            if (ended) return@launch
            connectedAt = SystemClock.elapsedRealtime()
            launch {
                while (!ended) {
                    _state.update { it.copy(seconds = ((SystemClock.elapsedRealtime() - connectedAt) / 1000).toInt()) }
                    delay(500)
                }
            }
            _state.update { it.copy(phase = CallPhase.Speaking, reply = words.greeting(agent.name)) }
            speak(words.greeting(agent.name)) { listen() }
        }
    }

    fun toggleMute() {
        val muted = !_state.value.muted
        _state.update { it.copy(muted = muted) }
        if (muted) {
            recognizer?.cancel()
        } else if (_state.value.phase == CallPhase.Listening) {
            listen()
        }
    }

    fun toggleSpeaker() {
        val on = !_state.value.speakerOn
        _state.update { it.copy(speakerOn = on) }
        applySpeaker(on)
    }

    fun hangUp() = finish()

    private fun finish() {
        if (ended) return
        ended = true
        val seconds = if (connectedAt == 0L) 0 else ((SystemClock.elapsedRealtime() - connectedAt) / 1000).toInt()
        turn?.cancel()
        runCatching { recognizer?.cancel(); recognizer?.destroy() }
        recognizer = null
        runCatching { speech?.stop(); speech?.shutdown() }
        speech = null
        runCatching { tones?.stopTone(); tones?.release() }
        endAudio()
        _state.update { it.copy(phase = CallPhase.Ended, seconds = seconds) }
        if (connectedAt != 0L) {
            onFinished(
                CallRecord(
                    id = UUID.randomUUID().toString(),
                    kind = CallKind.Agent,
                    target = agent.id,
                    name = agent.name,
                    startedAtEpochMs = startedAtWall,
                    durationSeconds = seconds,
                ),
            )
        }
        scope.cancel()
    }

    // ---- listening ----

    private fun listen() {
        if (ended) return
        _state.update { it.copy(phase = CallPhase.Listening, heard = "", error = "") }
        if (_state.value.muted) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.update { it.copy(error = words.speechUnavailable) }
            return
        }
        val active = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(listener)
            recognizer = it
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        runCatching { active.startListening(intent) }.onFailure { _state.update { s -> s.copy(error = words.speechUnavailable) } }
    }

    private fun listenSoon(afterMs: Long = 350) {
        handler.postDelayed({ if (!ended && _state.value.phase == CallPhase.Listening) listen() }, afterMs)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (text.isNotBlank() && _state.value.phase == CallPhase.Listening) _state.update { it.copy(heard = text) }
        }

        override fun onResults(results: Bundle?) {
            recognizerErrors = 0
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
            if (text.isBlank()) listenSoon() else submit(text)
        }

        override fun onError(error: Int) {
            if (ended || _state.value.phase != CallPhase.Listening || _state.value.muted) return
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> listenSoon()
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> _state.update { it.copy(error = words.micNeeded) }
                SpeechRecognizer.ERROR_CLIENT -> Unit
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    runCatching { recognizer?.destroy() }
                    recognizer = null
                    listenSoon(700)
                }
                else -> if (++recognizerErrors <= 3) listenSoon(700) else _state.update { it.copy(error = words.speechUnavailable) }
            }
        }
    }

    // ---- the agent's turn ----

    private fun submit(text: String) {
        _state.update { it.copy(phase = CallPhase.Thinking, heard = text, error = "") }
        val sentAt = System.currentTimeMillis()
        chat.sendQuickPrompt(text)
        turn = scope.launch {
            val started = withTimeoutOrNull(SEND_START_MS) { chat.uiState.first { it.isSending } } != null
            if (!started) {
                _state.update { it.copy(error = words.couldNotSend) }
                delay(1800)
                listen()
                return@launch
            }
            chat.uiState.first { !it.isSending }
            val answer = chat.uiState.value.messages
                .lastOrNull { it.role == "assistant" && it.content.isNotBlank() && it.createdAtEpochMs >= sentAt - 1000 }
                ?.content
                .orEmpty()
            val spoken = spokenText(answer)
            if (spoken.isBlank()) {
                _state.update { it.copy(error = words.couldNotSend) }
                delay(1800)
                listen()
            } else {
                _state.update { it.copy(phase = CallPhase.Speaking, reply = spoken, error = "") }
                speak(spoken) { listen() }
            }
        }
    }

    // ---- speaking ----

    private fun speak(text: String, done: () -> Unit) {
        if (ended) return
        if (speechFailed || text.isBlank()) { done(); return }
        val engine = speech
        if (!speechReady || engine == null) { queuedSpeech = text to done; return }
        whenSpeechDone = done
        val pieces = splitForSpeech(text, runCatching { TextToSpeech.getMaxSpeechInputLength() }.getOrDefault(3900) - 100)
        pieces.forEachIndexed { index, piece ->
            val last = index == pieces.lastIndex
            engine.speak(piece, if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, if (last) LAST_UTTERANCE else "part-$index")
        }
    }

    private fun speechDone() {
        val next = whenSpeechDone ?: return
        whenSpeechDone = null
        if (!ended) next()
    }

    // ---- audio routing, like a call ----

    @Suppress("DEPRECATION")
    private fun beginAudio() {
        runCatching {
            previousMode = audio.mode
            audio.mode = AudioManager.MODE_IN_COMMUNICATION
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .build()
            focus = request
            audio.requestAudioFocus(request)
            applySpeaker(_state.value.speakerOn)
        }
    }

    @Suppress("DEPRECATION")
    private fun applySpeaker(on: Boolean) {
        runCatching { audio.isSpeakerphoneOn = on }
    }

    @Suppress("DEPRECATION")
    private fun endAudio() {
        runCatching {
            audio.isSpeakerphoneOn = false
            audio.mode = previousMode
            focus?.let { audio.abandonAudioFocusRequest(it) }
        }
    }

    private companion object {
        const val RING_MS = 2200
        const val SEND_START_MS = 2500L
        const val LAST_UTTERANCE = "call-last"
    }
}

/** An answer as it is said aloud: no code, links, or markup symbols. */
internal fun spokenText(markdown: String): String =
    markdown
        .replace(Regex("```[\\s\\S]*?```"), " ")
        .replace(Regex("`([^`]*)`"), "$1")
        .replace(Regex("!?\\[([^\\]]*)]\\([^)]*\\)"), "$1")
        .replace(Regex("https?://\\S+"), " ")
        .replace(Regex("(?m)^\\s*(#{1,6}|>|[-*+]|\\d+[.)])\\s+"), "")
        .replace(Regex("[*_~|]+"), "")
        .replace(Regex("\\s*\\n+\\s*"), ". ")
        .replace(Regex("\\.\\s*\\.+"), ".")
        .replace(Regex("\\s{2,}"), " ")
        .trim()

/** Splits at sentence ends so no piece is longer than the speech engine accepts. */
internal fun splitForSpeech(text: String, limit: Int): List<String> {
    val max = limit.coerceAtLeast(200)
    if (text.length <= max) return listOf(text)
    val pieces = mutableListOf<String>()
    var rest = text
    while (rest.length > max) {
        val cut = rest.lastIndexOfAny(charArrayOf('.', '!', '?'), max - 1).takeIf { it > 0 } ?: rest.lastIndexOf(' ', max - 1).takeIf { it > 0 } ?: (max - 1)
        pieces += rest.substring(0, cut + 1).trim()
        rest = rest.substring(cut + 1).trim()
    }
    if (rest.isNotBlank()) pieces += rest
    return pieces
}
