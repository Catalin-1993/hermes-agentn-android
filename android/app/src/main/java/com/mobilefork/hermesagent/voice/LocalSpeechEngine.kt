package com.mobilefork.hermesagent.voice

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import java.io.File
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal const val SPEECH_SAMPLE_RATE = 16_000

/**
 * The loaded Parakeet recognizer. It holds about 700 MB of memory while loaded, so it is loaded when
 * the person starts to speak and released after a quiet minute.
 */
object LocalSpeechEngine {
    private const val IDLE_RELEASE_MS = 60_000L

    private val lock = Any()
    private var recognizer: OfflineRecognizer? = null
    private val main = Handler(Looper.getMainLooper())
    private val releaseLater = Runnable { unload() }

    /** True when the model is on the phone. Loading it can still fail; [transcribe] then throws. */
    fun isAvailable(context: Context): Boolean = LocalSpeechModelStore.isReady(context)

    private fun recognizerFor(context: Context): OfflineRecognizer = synchronized(lock) {
        recognizer?.let { return it }
        val dir = LocalSpeechModelStore.directory(context)
        val path = { name: String -> File(dir, name).absolutePath }
        val threads = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(2, 4)
        val model = OfflineModelConfig(
            transducer = OfflineTransducerModelConfig(
                encoder = path("encoder.int8.onnx"),
                decoder = path("decoder.int8.onnx"),
                joiner = path("joiner.int8.onnx"),
            ),
            tokens = path("tokens.txt"),
            numThreads = threads,
            debug = false,
            provider = "cpu",
            modelType = "nemo_transducer",
        )
        OfflineRecognizer(
            config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = SPEECH_SAMPLE_RATE, featureDim = 80),
                modelConfig = model,
                decodingMethod = "greedy_search",
            ),
        ).also { recognizer = it }
    }

    /** Recognizes one stretch of speech (16 kHz, mono, -1..1). Blocking: call it off the main thread. */
    fun transcribe(context: Context, samples: FloatArray): String {
        if (samples.isEmpty()) return ""
        main.removeCallbacks(releaseLater)
        try {
            val engine = recognizerFor(context.applicationContext)
            val stream = engine.createStream()
            return try {
                stream.acceptWaveform(samples, SPEECH_SAMPLE_RATE)
                engine.decode(stream)
                engine.getResult(stream).text.trim()
            } finally {
                stream.release()
            }
        } finally {
            main.postDelayed(releaseLater, IDLE_RELEASE_MS)
        }
    }

    /** Warms the model up, so the first words after a tap are not delayed by loading it. */
    fun preload(context: Context) {
        runCatching { recognizerFor(context.applicationContext) }
        main.removeCallbacks(releaseLater)
        main.postDelayed(releaseLater, IDLE_RELEASE_MS)
    }

    fun unload() {
        main.removeCallbacks(releaseLater)
        synchronized(lock) {
            recognizer?.release()
            recognizer = null
        }
    }
}

/** What a voice session reports, always on the main thread. */
interface LocalVoiceListener {
    fun onListening()
    fun onTranscribing()
    fun onResult(text: String)
    fun onFailed(message: String)
}

/**
 * One spoken message. It listens, notices when the person has finished (a pause after speech), then
 * recognizes everything said in one go. [finish] ends the listening at once and still recognizes what
 * was said; [cancel] throws it away. The caller has already checked the microphone permission.
 */
class LocalVoiceSession(
    context: Context,
    private val listener: LocalVoiceListener,
) {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    @Volatile private var finishRequested = false
    @Volatile private var cancelled = false
    private var thread: Thread? = null

    val isRunning: Boolean get() = thread?.isAlive == true

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRunning) return
        finishRequested = false
        cancelled = false
        thread = Thread({ run() }, "hermes-local-voice").also { it.start() }
    }

    fun finish() {
        finishRequested = true
    }

    fun cancel() {
        cancelled = true
        finishRequested = true
    }

    @SuppressLint("MissingPermission")
    private fun run() {
        val minimum = AudioRecord.getMinBufferSize(SPEECH_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val record = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SPEECH_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
                max(minimum, FRAME_SAMPLES * 8 * 4),
            )
        }.getOrNull()
        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            record?.release()
            post { listener.onFailed("The microphone could not start. Close other apps that use it and try again.") }
            return
        }
        val heard = SampleBuffer()
        try {
            // Loading the model while the person starts to speak hides most of its start-up time.
            Thread { LocalSpeechEngine.preload(context) }.start()
            record.startRecording()
            post { listener.onListening() }
            val frame = FloatArray(FRAME_SAMPLES)
            var noiseDb = Double.POSITIVE_INFINITY
            var spoke = false
            var loudFrames = 0
            var quietAfterSpeech = 0
            var waitingFrames = 0
            while (!finishRequested) {
                var filled = 0
                while (filled < FRAME_SAMPLES && !finishRequested) {
                    val read = record.read(frame, filled, FRAME_SAMPLES - filled, AudioRecord.READ_BLOCKING)
                    if (read <= 0) throw IllegalStateException("The microphone stopped.")
                    filled += read
                }
                if (filled < FRAME_SAMPLES) break
                heard.append(frame)
                val db = decibels(frame)
                // The room noise is the quietest level seen, creeping up slowly so a noisy room adapts.
                noiseDb = if (db < noiseDb) db else min(db, noiseDb + 0.2)
                val speaking = db > max(NOISE_FLOOR_DB, noiseDb + SPEECH_ABOVE_NOISE_DB)
                if (speaking) {
                    loudFrames += 1
                    if (loudFrames >= SPEECH_START_FRAMES) spoke = true
                    quietAfterSpeech = 0
                } else {
                    loudFrames = 0
                    if (spoke) quietAfterSpeech += 1 else waitingFrames += 1
                }
                if (spoke && quietAfterSpeech >= PAUSE_FRAMES) break
                if (!spoke && waitingFrames >= WAIT_FOR_SPEECH_FRAMES) break
                if (heard.size >= MAX_SECONDS * SPEECH_SAMPLE_RATE) break
            }
        } catch (error: Exception) {
            record.release()
            if (!cancelled) post { listener.onFailed(error.message ?: "The microphone stopped.") }
            return
        }
        runCatching { record.stop() }
        record.release()
        if (cancelled) return
        if (heard.size < MIN_SPEECH_SAMPLES) {
            post { listener.onResult("") }
            return
        }
        post { listener.onTranscribing() }
        val text = try {
            LocalSpeechEngine.transcribe(context, heard.toArray())
        } catch (error: Throwable) {
            post { listener.onFailed(error.message ?: "The speech model could not run.") }
            return
        }
        if (!cancelled) post { listener.onResult(text) }
    }

    private fun post(block: () -> Unit) {
        main.post { block() }
    }

    private fun decibels(frame: FloatArray): Double {
        var sum = 0.0
        for (sample in frame) sum += sample.toDouble() * sample
        return 20 * log10(max(sqrt(sum / frame.size), 1e-5))
    }

    /** The samples heard so far, in one array that grows as needed. */
    private class SampleBuffer {
        private var samples = FloatArray(SPEECH_SAMPLE_RATE * 4)
        var size = 0
            private set

        fun append(frame: FloatArray) {
            if (size + frame.size > samples.size) samples = samples.copyOf(max(samples.size * 2, size + frame.size))
            frame.copyInto(samples, size)
            size += frame.size
        }

        fun toArray(): FloatArray = samples.copyOf(size)
    }

    private companion object {
        /** 100 ms per frame. */
        const val FRAME_SAMPLES = SPEECH_SAMPLE_RATE / 10
        const val NOISE_FLOOR_DB = -52.0
        const val SPEECH_ABOVE_NOISE_DB = 11.0
        /** Speech has started after this many loud frames in a row. */
        const val SPEECH_START_FRAMES = 2
        /** A pause of 1.4 s after speech ends the message. */
        const val PAUSE_FRAMES = 14
        /** Nothing said for 9 s ends the listening. */
        const val WAIT_FOR_SPEECH_FRAMES = 90
        const val MAX_SECONDS = 60
        /** Less than a third of a second of sound is not speech. */
        const val MIN_SPEECH_SAMPLES = SPEECH_SAMPLE_RATE / 3
    }
}
