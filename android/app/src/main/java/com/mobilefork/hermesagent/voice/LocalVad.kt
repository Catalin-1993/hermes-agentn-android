package com.mobilefork.hermesagent.voice

import android.content.Context
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.TenVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import java.io.File

/**
 * Tells, frame by frame, whether someone is speaking right now. A small neural model (a few hundred
 * kilobytes) does it, which holds up far better than a volume threshold in a noisy room.
 */
class SpeechDetector internal constructor(private val vad: Vad) {
    /** Feed the next stretch of 16 kHz mono sound; true while speech is going on. */
    fun isSpeech(frame: FloatArray): Boolean {
        vad.acceptWaveform(frame)
        val speaking = vad.isSpeechDetected()
        // Only the live state matters here, so finished segments are dropped instead of piling up.
        while (!vad.empty()) vad.pop()
        return speaking
    }

    fun close() {
        runCatching { vad.release() }
    }
}

object LocalVad {
    /** A detector for the downloaded model [id], or null when it is not there or cannot start. */
    fun open(context: Context, id: String): SpeechDetector? {
        val spec = VoiceModelCatalog.find(id)?.takeIf { !it.isVoice } ?: return null
        val app = context.applicationContext
        if (!VoiceModelStore.isReady(app, spec)) return null
        val file = File(VoiceModelStore.directory(app, spec), spec.downloads.first().fileName).absolutePath
        val config = when (spec.family) {
            VoiceFamily.SileroVad -> VadModelConfig(
                sileroVadModelConfig = SileroVadModelConfig(
                    model = file,
                    threshold = 0.5f,
                    minSilenceDuration = 0.25f,
                    minSpeechDuration = 0.1f,
                    windowSize = 512,
                    maxSpeechDuration = 60f,
                ),
                sampleRate = SPEECH_SAMPLE_RATE,
                numThreads = 1,
            )
            else -> VadModelConfig(
                tenVadModelConfig = TenVadModelConfig(
                    model = file,
                    threshold = 0.5f,
                    minSilenceDuration = 0.25f,
                    minSpeechDuration = 0.1f,
                    windowSize = 256,
                    maxSpeechDuration = 60f,
                ),
                sampleRate = SPEECH_SAMPLE_RATE,
                numThreads = 1,
            )
        }
        return runCatching { SpeechDetector(Vad(config = config)) }.getOrNull()
    }

    /** Nothing is kept loaded between recordings, so there is nothing to release. */
    fun unload() = Unit
}
