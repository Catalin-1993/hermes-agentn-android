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
    /** A live conversation: no ringing and no greeting, it listens at once and keeps no call record. */
    private val live: Boolean = false,
    private val onFinished: (CallRecord) -> Unit,
) {
    private val context = context.applicationContext
    /** A fault inside a turn is shown on the call and the call goes on or ends; it never takes the app down. */
    private val faults = kotlinx.coroutines.CoroutineExceptionHandler { _, error ->
        if (!ended) _state.update { it.copy(error = error.message ?: words.speechUnavailable) }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + faults)
    private val handler = Handler(Looper.getMainLooper())
    private val locale = Locale.forLanguageTag(languageTag.ifBlank { Locale.getDefault().toLanguageTag() })
    private val audio = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val _state = MutableStateFlow(VoiceCallState())
    val state: StateFlow<VoiceCallState> = _state.asStateFlow()
    private val _level = MutableStateFlow(0f)
    /** How loud the person is, 0..1, kept apart from [state] because it changes ten times a second. */
    val level: StateFlow<Float> = _level.asStateFlow()
    private var localSession: com.mobilefork.hermesagent.voice.LocalVoiceSession? = null
    private var bargeIn: com.mobilefork.hermesagent.voice.BargeInMonitor? = null
    private var preRoll: FloatArray? = null
    private var paused = false
    private var onlinePlayer: android.media.MediaPlayer? = null
    private var visualizer: android.media.audiofx.Visualizer? = null
    /** The beat of the phone's own voice: it jumps at each word and fades. */
    private var wordPulse = 0f

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
        speech = runCatching { TextToSpeech(context) { status ->
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

                        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                            wordPulse = 0.55f + 0.45f * kotlin.random.Random.nextFloat()
                        }

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
        } }.getOrElse {
            speechFailed = true
            null
        }
        scope.launch {
            if (!live) {
                runCatching {
                    tones = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 45).also { it.startTone(ToneGenerator.TONE_SUP_RINGTONE, RING_MS) }
                }
                delay(RING_MS.toLong())
                runCatching { tones?.stopTone(); tones?.release() }
                tones = null
            }
            if (ended) return@launch
            connectedAt = SystemClock.elapsedRealtime()
            launch {
                // While the agent speaks, the level is how loud its voice is right now.
                while (!ended) {
                    if (_state.value.phase == CallPhase.Speaking) _level.value = outputLevel()
                    delay(33)
                }
            }
            launch {
                while (!ended) {
                    _state.update { it.copy(seconds = ((SystemClock.elapsedRealtime() - connectedAt) / 1000).toInt()) }
                    delay(500)
                }
            }
            if (live) {
                listen()
            } else {
                _state.update { it.copy(phase = CallPhase.Speaking, reply = words.greeting(agent.name)) }
                speak(words.greeting(agent.name)) { listen() }
            }
        }
    }

    fun toggleMute() {
        val muted = !_state.value.muted
        _state.update { it.copy(muted = muted) }
        if (muted) {
            stopBargeIn()
            recognizer?.cancel()
            localSession?.cancel()
            localSession = null
            _level.value = 0f
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

    /** Stops listening and speaking while another app (the camera) is in front; [resume] picks up again. */
    fun pause() {
        paused = true
        stopBargeIn()
        interruptSpeech()
        localSession?.cancel()
        localSession = null
        runCatching { recognizer?.cancel() }
        _level.value = 0f
        if (!ended && _state.value.phase == CallPhase.Speaking) _state.update { it.copy(phase = CallPhase.Listening, heard = "") }
    }

    fun resume() {
        if (!paused) return
        paused = false
        if (!ended && _state.value.phase == CallPhase.Listening) listen()
    }

    // ---- cutting in ----

    private fun startBargeIn() {
        if (ended || paused || _state.value.muted) return
        stopBargeIn()
        bargeIn = runCatching {
            com.mobilefork.hermesagent.voice.BargeInMonitor(context) { heard -> cutIn(heard) }.also { it.start() }
        }.getOrNull()
    }

    private fun stopBargeIn() {
        bargeIn?.stop()
        bargeIn = null
    }

    /** The person started to speak over the agent: it stops at once and listens, keeping the first words. */
    private fun cutIn(heard: FloatArray) {
        // Waits for the monitor to give the microphone back before the recognizer takes it.
        stopBargeIn()
        if (ended || paused || _state.value.phase != CallPhase.Speaking) return
        interruptSpeech()
        preRoll = heard
        listen()
    }

    private fun interruptSpeech() {
        whenSpeechDone = null
        queuedSpeech = null
        runCatching { com.mobilefork.hermesagent.voice.LocalTtsEngine.stop() }
        runCatching { speech?.stop() }
        releaseOnlinePlayer()
    }

    private fun releaseOnlinePlayer() {
        runCatching { visualizer?.enabled = false; visualizer?.release() }
        visualizer = null
        runCatching { onlinePlayer?.stop(); onlinePlayer?.release() }
        onlinePlayer = null
    }

    /** How loud what is being said is right now, 0..1, from whichever voice is reading. */
    private fun outputLevel(): Float {
        val local = runCatching { com.mobilefork.hermesagent.voice.LocalTtsEngine.outputLevel() }.getOrDefault(0f)
        if (local > 0f) return local
        val viz = visualizer
        if (viz != null) {
            val level = runCatching {
                val wave = ByteArray(viz.captureSize)
                viz.getWaveForm(wave)
                var sum = 0.0
                for (b in wave) { val v = ((b.toInt() and 0xFF) - 128) / 128.0; sum += v * v }
                (kotlin.math.sqrt(sum / wave.size) * 4.0).toFloat().coerceIn(0f, 1f)
            }.getOrDefault(0f)
            if (level > 0f) return level
        }
        // The phone's voice gives no sound to measure, only the moment each word starts.
        wordPulse *= 0.86f
        return wordPulse
    }

    private fun finish() {
        if (ended) return
        ended = true
        val seconds = if (connectedAt == 0L) 0 else ((SystemClock.elapsedRealtime() - connectedAt) / 1000).toInt()
        turn?.cancel()
        stopBargeIn()
        releaseOnlinePlayer()
        localSession?.cancel()
        localSession = null
        runCatching { com.mobilefork.hermesagent.voice.LocalTtsEngine.stop() }
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
        stopBargeIn()
        _state.update { it.copy(phase = CallPhase.Listening, heard = "", error = "") }
        if (_state.value.muted || paused) return
        if (localListeningReady()) {
            listenLocal()
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.update { it.copy(error = words.speechUnavailable) }
            return
        }
        val active = recognizer ?: runCatching {
            SpeechRecognizer.createSpeechRecognizer(context).also {
                it.setRecognitionListener(listener)
                recognizer = it
            }
        }.getOrNull()
        if (active == null) {
            _state.update { it.copy(error = words.speechUnavailable) }
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        runCatching { active.startListening(intent) }.onFailure { _state.update { s -> s.copy(error = words.speechUnavailable) } }
    }

    private fun localListeningReady(): Boolean =
        !com.mobilefork.hermesagent.BuildConfig.HERMES_PLAY_EDITION &&
            com.mobilefork.hermesagent.ui.chat.VoiceSettings(context).useLocalRecognition &&
            com.mobilefork.hermesagent.voice.LocalSpeechEngine.isAvailable(context)

    /** Listens with the speech model on this phone: live words while speaking, and a pause ends the turn. */
    private fun listenLocal() {
        if (localSession?.isRunning == true) return
        val session = com.mobilefork.hermesagent.voice.LocalVoiceSession(
            context,
            object : com.mobilefork.hermesagent.voice.LocalVoiceListener {
                override fun onLevel(level: Float) {
                    _level.value = level
                }

                override fun onPartial(text: String) {
                    if (_state.value.phase == CallPhase.Listening) _state.update { it.copy(heard = text) }
                }

                override fun onListening() = Unit
                override fun onTranscribing() = Unit

                override fun onResult(text: String) {
                    localSession = null
                    _level.value = 0f
                    if (ended || _state.value.muted || _state.value.phase != CallPhase.Listening) return
                    if (text.isBlank()) listenSoon() else submit(text)
                }

                override fun onFailed(message: String) {
                    localSession = null
                    _level.value = 0f
                    if (!ended) _state.update { it.copy(error = message) }
                }
            },
            preRoll.also { preRoll = null },
        )
        localSession = session
        session.start()
    }

    private fun listenSoon(afterMs: Long = 350) {
        handler.postDelayed({ if (!ended && _state.value.phase == CallPhase.Listening) listen() }, afterMs)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) {
            _level.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        }
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
        chat.sendSpoken(text)
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
                if (live) startBargeIn()
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
        val online = com.mobilefork.hermesagent.ui.chat.VoiceSettings(context)
        val local = runCatching { com.mobilefork.hermesagent.ui.chat.HermesTtsController.localRequest(context, text) }.getOrNull()
        if (local != null) {
            // The voice downloaded on this phone; if it cannot run, the phone's own voice reads instead.
            com.mobilefork.hermesagent.voice.LocalTtsEngine.speak(context, local) { failed, _ ->
                if (!ended) {
                    if (failed) speakWithPhone(engine, text) else speechDone()
                }
            }
            return
        }
        if (online.usesElevenLabs) {
            // The online voice; if it fails, fall through to the phone's voice below.
            Thread {
                val file = runCatching { com.mobilefork.hermesagent.ui.chat.ElevenLabsVoice.fetch(context, online, text.take(2_500)) }.getOrNull()
                handler.post {
                    if (ended) { file?.delete(); return@post }
                    val played = file != null && runCatching {
                        val player = android.media.MediaPlayer()
                        onlinePlayer = player
                        player.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                        player.setDataSource(file.absolutePath)
                        player.setOnCompletionListener { releaseOnlinePlayer(); file.delete(); speechDone() }
                        player.prepare()
                        player.start()
                        visualizer = runCatching {
                            android.media.audiofx.Visualizer(player.audioSessionId).also {
                                it.captureSize = android.media.audiofx.Visualizer.getCaptureSizeRange()[0]
                                it.enabled = true
                            }
                        }.getOrNull()
                    }.isSuccess
                    if (!played) speakWithPhone(engine, text)
                }
            }.start()
            return
        }
        speakWithPhone(engine, text)
    }

    private fun speakWithPhone(engine: TextToSpeech, text: String) {
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
