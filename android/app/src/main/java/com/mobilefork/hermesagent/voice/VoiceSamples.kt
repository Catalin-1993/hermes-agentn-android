package com.mobilefork.hermesagent.voice

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import java.io.File
import kotlin.math.abs
import kotlin.math.max

/** A short recording a cloning voice copies, with the words said in it when the model needs them. */
class ReferenceVoice(val samples: FloatArray, val sampleRate: Int, val text: String) {
    val seconds: Float get() = samples.size / sampleRate.toFloat()
}

/**
 * Where the person's own recording is kept, and which reference a cloning voice uses: that recording
 * when there is one, otherwise the demo voice that comes with the model.
 */
object VoiceSamples {
    const val MIN_SECONDS = 3f
    const val MAX_SECONDS = 20

    private fun directory(context: Context) = File(context.applicationContext.filesDir, "voice-samples")
    private fun wavFile(context: Context) = File(directory(context), "mine.wav")
    private fun textFile(context: Context) = File(directory(context), "mine.txt")

    fun hasOwn(context: Context): Boolean = wavFile(context).isFile

    fun saveOwn(context: Context, samples: FloatArray, sampleRate: Int) {
        directory(context).mkdirs()
        WavFile.write(wavFile(context), samples, sampleRate)
    }

    /** The person's recording as 16 kHz samples, for recognizing the words in it. */
    fun ownSamples(context: Context): FloatArray? = if (hasOwn(context)) WavFile.read(wavFile(context))?.samples else null

    fun ownText(context: Context): String = runCatching { textFile(context).readText() }.getOrDefault("")

    fun saveOwnText(context: Context, text: String) {
        directory(context).mkdirs()
        textFile(context).writeText(text.trim())
    }

    fun removeOwn(context: Context) {
        wavFile(context).delete()
        textFile(context).delete()
    }

    /** The recording and its words for [spec], or null when a voice that clones has nothing to copy yet. */
    fun referenceFor(context: Context, spec: VoiceModelSpec): ReferenceVoice? {
        if (!spec.canClone) return null
        if (hasOwn(context)) {
            val wav = WavFile.read(wavFile(context)) ?: return null
            val text = ownText(context)
            if (spec.needsReferenceText && text.isBlank()) return null
            return ReferenceVoice(wav.samples, wav.sampleRate, text)
        }
        val demo = demoFor(spec) ?: return null
        val wav = WavFile.read(File(VoiceModelStore.directory(context, spec), demo.first)) ?: return null
        return ReferenceVoice(wav.samples, wav.sampleRate, demo.second)
    }

    /** True when the voice has something to copy: the person's recording, or the model's demo voice. */
    fun hasReference(context: Context, spec: VoiceModelSpec): Boolean = referenceFor(context, spec) != null

    /**
     * The demo voice that ships inside a cloning model's folder, with the words it says. Only Pocket
     * has one: ZipVoice must copy a voice in the language it is to speak, so it needs the person's own.
     */
    private fun demoFor(spec: VoiceModelSpec): Pair<String, String>? = when (spec.family) {
        VoiceFamily.Pocket -> "test_wavs/bria.wav" to ""
        else -> null
    }
}

/** Records the person's voice, 16 kHz mono, until [stop] or [VoiceSamples.MAX_SECONDS]. */
class VoiceSampleRecorder(private val onTick: (seconds: Int) -> Unit, private val onDone: (FloatArray?) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var stopRequested = false
    private var thread: Thread? = null

    val isRecording: Boolean get() = thread?.isAlive == true

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRecording) return
        stopRequested = false
        thread = Thread({ record() }, "hermes-voice-sample").also { it.start() }
    }

    fun stop() {
        stopRequested = true
    }

    @SuppressLint("MissingPermission")
    private fun record() {
        val minimum = AudioRecord.getMinBufferSize(SPEECH_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val recorder = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.MIC, SPEECH_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT, max(minimum, SPEECH_SAMPLE_RATE * 4),
            )
        }.getOrNull()
        if (recorder == null || recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder?.release()
            main.post { onDone(null) }
            return
        }
        val all = FloatArray(SPEECH_SAMPLE_RATE * VoiceSamples.MAX_SECONDS)
        var size = 0
        try {
            recorder.startRecording()
            val frame = FloatArray(SPEECH_SAMPLE_RATE / 10)
            var lastSecond = -1
            while (!stopRequested && size + frame.size <= all.size) {
                val read = recorder.read(frame, 0, frame.size, AudioRecord.READ_BLOCKING)
                if (read <= 0) break
                frame.copyInto(all, size, 0, read)
                size += read
                val second = size / SPEECH_SAMPLE_RATE
                if (second != lastSecond) {
                    lastSecond = second
                    main.post { onTick(second) }
                }
            }
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
        }
        val samples = all.copyOf(size)
        // A quiet recording is raised to a clear level, so the copy does not come out faint.
        val peak = samples.maxOfOrNull { abs(it) } ?: 0f
        if (peak in 0.001f..0.8f) for (i in samples.indices) samples[i] = samples[i] * (0.9f / peak)
        main.post { onDone(samples) }
    }
}
