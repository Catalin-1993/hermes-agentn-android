package com.mobilefork.hermesagent.voice

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Handler
import android.os.Looper
import com.mobilefork.hermesagent.ui.chat.VoiceSettings
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Listens while the agent is speaking, so the person can cut in. It uses the call microphone, whose
 * echo canceller removes most of the agent's own voice, and needs speech to go on for a third of a
 * second before it counts: a cough or a bump does not stop the agent. When it hears the person, it
 * hands back the sound it has kept from just before, so the first words are not lost.
 */
class BargeInMonitor(context: Context, private val onSpeech: (preRoll: FloatArray) -> Unit) {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var stopRequested = false
    private var thread: Thread? = null

    val isRunning: Boolean get() = thread?.isAlive == true

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRunning) return
        stopRequested = false
        // Nothing in this thread may end the app: a microphone or model fault only stops the listening.
        thread = Thread({ runCatching { run() } }, "hermes-barge-in").also { it.start() }
    }

    /** Stops listening and waits briefly for the microphone to be given back. */
    fun stop() {
        stopRequested = true
        val running = thread ?: return
        if (running !== Thread.currentThread()) runCatching { running.join(400) }
        thread = null
    }

    @SuppressLint("MissingPermission")
    private fun run() {
        val minimum = AudioRecord.getMinBufferSize(SPEECH_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val record = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SPEECH_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
                max(minimum, FRAME * 8 * 4),
            )
        }.getOrNull()
        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            record?.release()
            return
        }
        val echo = runCatching { if (AcousticEchoCanceler.isAvailable()) AcousticEchoCanceler.create(record.audioSessionId)?.also { it.enabled = true } else null }.getOrNull()
        val noise = runCatching { if (NoiseSuppressor.isAvailable()) NoiseSuppressor.create(record.audioSessionId)?.also { it.enabled = true } else null }.getOrNull()
        val detector = runCatching {
            VoiceSettings(context).speechDetector.takeIf { it.isNotBlank() }?.let { LocalVad.open(context, it) }
        }.getOrNull()
        val keep = FloatArray(KEEP_FRAMES * FRAME)
        var kept = 0
        var speechFrames = 0
        var quietest = Double.POSITIVE_INFINITY
        var frames = 0
        try {
            record.startRecording()
            val frame = FloatArray(FRAME)
            while (!stopRequested) {
                var filled = 0
                while (filled < FRAME && !stopRequested) {
                    val read = record.read(frame, filled, FRAME - filled, AudioRecord.READ_BLOCKING)
                    if (read <= 0) return
                    filled += read
                }
                if (filled < FRAME) break
                frames += 1
                // Keep the last second and a half, oldest first.
                if (kept + FRAME > keep.size) {
                    System.arraycopy(keep, FRAME, keep, 0, keep.size - FRAME)
                    kept = keep.size - FRAME
                }
                frame.copyInto(keep, kept)
                kept += FRAME
                val db = decibels(frame)
                quietest = if (db < quietest) db else minOf(db, quietest + 0.2)
                // The first moments are the agent's own voice starting up; they are never taken for the person.
                if (frames <= SETTLE_FRAMES) continue
                val speaking = if (detector != null) detector.isSpeech(frame) && db > LOUD_DB else db > max(LOUD_DB, quietest + ABOVE_ROOM_DB)
                speechFrames = if (speaking) speechFrames + 1 else 0
                if (speechFrames >= NEEDED_FRAMES) {
                    val heard = keep.copyOf(kept)
                    stopRequested = true
                    main.post { onSpeech(heard) }
                }
            }
        } catch (_: Throwable) {
            // The microphone or the detector went away: nothing to listen to.
        } finally {
            runCatching { record.stop() }
            runCatching { echo?.release() }
            runCatching { noise?.release() }
            detector?.close()
            record.release()
        }
    }

    private fun decibels(frame: FloatArray): Double {
        var sum = 0.0
        for (sample in frame) sum += sample.toDouble() * sample
        return 20 * log10(max(sqrt(sum / frame.size), 1e-5))
    }

    private companion object {
        /** 100 ms. */
        const val FRAME = SPEECH_SAMPLE_RATE / 10
        const val KEEP_FRAMES = 15
        const val SETTLE_FRAMES = 4
        /** 0.4 s of speech in a row. */
        const val NEEDED_FRAMES = 4
        const val LOUD_DB = -38.0
        const val ABOVE_ROOM_DB = 14.0
    }
}
