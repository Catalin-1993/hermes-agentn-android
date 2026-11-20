package com.mobilefork.hermesagent.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsPocketModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsZipVoiceModelConfig
import java.io.File
import java.util.concurrent.Executors

/** What to say and how: the model, which of its voices, how fast, and the recording a cloning voice copies. */
class LocalTtsRequest(
    val spec: VoiceModelSpec,
    val text: String,
    val languageTag: String,
    val voice: Int = 0,
    val speed: Float = 1f,
    val reference: ReferenceVoice? = null,
)

/**
 * Reads text aloud with a voice model that runs on this phone. The model is loaded when it is first
 * needed and released after a quiet minute and a half, because the larger ones hold a few hundred
 * megabytes. Sound is played while the rest is still being made, so the first words come quickly.
 */
object LocalTtsEngine {
    private const val IDLE_RELEASE_MS = 90_000L

    private val lock = Any()
    private var tts: OfflineTts? = null
    private var loadedId: String? = null
    private val main = Handler(Looper.getMainLooper())
    private val releaseLater = Runnable { unload() }
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "hermes-local-tts") }

    @Volatile private var stopRequested = false
    @Volatile private var track: AudioTrack? = null

    // How loud the voice is over time, one value per 25 ms, so the screen can move with what is being heard.
    private val envelopeLock = Any()
    private var envelope = FloatArray(2048)
    private var envelopeCount = 0
    private var envelopeHop = 600
    private var hopSum = 0.0
    private var hopSamples = 0

    private fun resetEnvelope(sampleRate: Int) = synchronized(envelopeLock) {
        envelopeCount = 0
        envelopeHop = sampleRate / 40
        hopSum = 0.0
        hopSamples = 0
    }

    private fun addToEnvelope(samples: FloatArray) = synchronized(envelopeLock) {
        for (value in samples) {
            hopSum += value.toDouble() * value
            hopSamples += 1
            if (hopSamples >= envelopeHop) {
                if (envelopeCount == envelope.size) envelope = envelope.copyOf(envelope.size * 2)
                envelope[envelopeCount++] = kotlin.math.sqrt(hopSum / hopSamples).toFloat()
                hopSum = 0.0
                hopSamples = 0
            }
        }
    }

    /** How loud the voice is at the very moment it is heard, 0..1; 0 when nothing is playing. */
    fun outputLevel(): Float {
        val playing = track ?: return 0f
        val head = runCatching { playing.playbackHeadPosition }.getOrDefault(0)
        return synchronized(envelopeLock) {
            val index = head / envelopeHop
            if (index < 0 || index >= envelopeCount) 0f else (envelope[index] * 6f).coerceIn(0f, 1f)
        }
    }

    /** Starts reading. [onFinished] is called on the main thread with true only when it could not run; being stopped is not a failure. */
    fun speak(context: Context, request: LocalTtsRequest, onFinished: (failed: Boolean, message: String) -> Unit = { _, _ -> }) {
        val app = context.applicationContext
        stop()
        main.removeCallbacks(releaseLater)
        worker.execute {
            stopRequested = false
            val result = try {
                play(app, request)
                false to ""
            } catch (error: Throwable) {
                true to (error.message ?: error.javaClass.simpleName)
            } finally {
                track?.let { runCatching { it.release() } }
                track = null
                main.postDelayed(releaseLater, IDLE_RELEASE_MS)
            }
            main.post { onFinished(result.first, result.second) }
        }
    }

    /** Stops the voice at once. */
    fun stop() {
        stopRequested = true
        track?.let { runCatching { it.pause(); it.flush() } }
    }

    fun unload() {
        main.removeCallbacks(releaseLater)
        synchronized(lock) {
            tts?.release()
            tts = null
            loadedId = null
        }
    }

    /** How many voices the loaded model offers: loads it if needed. Blocking, so call it off the main thread. */
    fun voiceCount(context: Context, spec: VoiceModelSpec): Int =
        runCatching { engineFor(context.applicationContext, spec).numSpeakers() }.getOrDefault(1).coerceAtLeast(1)

    private fun engineFor(context: Context, spec: VoiceModelSpec): OfflineTts = synchronized(lock) {
        tts?.takeIf { loadedId == spec.id }?.let { return it }
        tts?.release()
        tts = null
        loadedId = null
        val dir = VoiceModelStore.directory(context, spec)
        if (!VoiceModelStore.isReady(context, spec)) error("${spec.name} is not downloaded.")
        OfflineTts(config = configFor(spec, dir)).also {
            tts = it
            loadedId = spec.id
        }
    }

    private fun play(context: Context, request: LocalTtsRequest) {
        val spec = request.spec
        val text = VoiceText.speakable(request.text)
        if (text.isBlank()) return
        val engine = engineFor(context, spec)
        val generation = generationFor(request)
        val audioTrack = newTrack(engine.sampleRate())
        resetEnvelope(engine.sampleRate())
        track = audioTrack
        audioTrack.play()
        var frames = 0L
        engine.generateWithConfigAndCallback(text, generation) { samples ->
            if (stopRequested) {
                0
            } else {
                addToEnvelope(samples)
                audioTrack.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
                frames += samples.size
                1
            }
        }
        if (stopRequested) return
        // Everything has been handed over: let what is queued finish playing.
        audioTrack.stop()
        val deadline = System.currentTimeMillis() + frames * 1000L / engine.sampleRate() + 1_500L
        while (!stopRequested && audioTrack.playbackHeadPosition < frames && System.currentTimeMillis() < deadline) {
            Thread.sleep(40)
        }
    }

    private fun newTrack(sampleRate: Int): AudioTrack {
        val minimum = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minimum, sampleRate * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun generationFor(request: LocalTtsRequest): GenerationConfig {
        val spec = request.spec
        val speed = request.speed.coerceIn(0.5f, 2f)
        return when (spec.family) {
            VoiceFamily.Vits -> GenerationConfig(sid = 0, speed = speed)
            VoiceFamily.Kokoro -> GenerationConfig(sid = request.voice, speed = speed)
            VoiceFamily.Supertonic -> GenerationConfig(
                sid = request.voice,
                speed = speed,
                numSteps = 6,
                extra = mapOf("lang" to VoiceText.supertonicLanguage(spec, request.languageTag)),
            )
            VoiceFamily.Pocket -> {
                val reference = request.reference ?: error("Record your voice first, or use the demo voice.")
                GenerationConfig(
                    referenceAudio = reference.samples,
                    referenceSampleRate = reference.sampleRate,
                    numSteps = 4,
                    extra = mapOf("temperature" to "0.7", "chunk_size" to "15"),
                )
            }
            VoiceFamily.ZipVoice -> {
                val reference = request.reference ?: error("Record your voice first, or use the demo voice.")
                GenerationConfig(
                    speed = speed,
                    referenceAudio = reference.samples,
                    referenceSampleRate = reference.sampleRate,
                    referenceText = reference.text,
                    numSteps = 4,
                    extra = mapOf("min_char_in_sentence" to "10"),
                )
            }
            else -> error("${spec.name} is not a voice.")
        }
    }

    private fun configFor(spec: VoiceModelSpec, dir: File): OfflineTtsConfig {
        fun path(name: String): String = File(dir, name).absolutePath
        fun first(vararg names: String): String = path(names.firstOrNull { File(dir, it).isFile } ?: names.first())
        val threads = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(2, 4)
        val model = when (spec.family) {
            VoiceFamily.Vits -> {
                val onnx = dir.listFiles { file -> file.extension == "onnx" }?.singleOrNull() ?: error("The voice file is missing.")
                OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(model = onnx.absolutePath, tokens = path("tokens.txt"), dataDir = path("espeak-ng-data")),
                    numThreads = threads,
                )
            }
            VoiceFamily.Kokoro -> OfflineTtsModelConfig(
                kokoro = OfflineTtsKokoroModelConfig(
                    model = path("model.int8.onnx"),
                    voices = path("voices.bin"),
                    tokens = path("tokens.txt"),
                    dataDir = path("espeak-ng-data"),
                    dictDir = path("dict"),
                    lexicon = path("lexicon-us-en.txt") + "," + path("lexicon-zh.txt"),
                ),
                numThreads = threads,
            )
            VoiceFamily.Supertonic -> OfflineTtsModelConfig(
                supertonic = OfflineTtsSupertonicModelConfig(
                    durationPredictor = path("duration_predictor.int8.onnx"),
                    textEncoder = path("text_encoder.int8.onnx"),
                    vectorEstimator = path("vector_estimator.int8.onnx"),
                    vocoder = path("vocoder.int8.onnx"),
                    ttsJson = path("tts.json"),
                    unicodeIndexer = path("unicode_indexer.bin"),
                    voiceStyle = path("voice.bin"),
                ),
                numThreads = threads,
            )
            VoiceFamily.Pocket -> OfflineTtsModelConfig(
                pocket = OfflineTtsPocketModelConfig(
                    lmFlow = path("lm_flow.int8.onnx"),
                    lmMain = path("lm_main.int8.onnx"),
                    encoder = path("encoder.onnx"),
                    decoder = path("decoder.int8.onnx"),
                    textConditioner = path("text_conditioner.onnx"),
                    vocabJson = path("vocab.json"),
                    tokenScoresJson = path("token_scores.json"),
                ),
                numThreads = threads,
            )
            VoiceFamily.ZipVoice -> OfflineTtsModelConfig(
                zipvoice = OfflineTtsZipVoiceModelConfig(
                    tokens = path("tokens.txt"),
                    encoder = first("encoder.onnx", "encoder.int8.onnx"),
                    decoder = first("decoder.onnx", "decoder.int8.onnx"),
                    vocoder = path("vocos_24khz.onnx"),
                    dataDir = path("espeak-ng-data"),
                    lexicon = path("lexicon.txt"),
                ),
                numThreads = threads,
            )
            else -> error("${spec.name} is not a voice.")
        }
        val rules = if (spec.family == VoiceFamily.Kokoro) {
            listOf("phone-zh.fst", "date-zh.fst", "number-zh.fst").joinToString(",") { path(it) }
        } else {
            ""
        }
        return OfflineTtsConfig(model = model, ruleFsts = rules)
    }
}
