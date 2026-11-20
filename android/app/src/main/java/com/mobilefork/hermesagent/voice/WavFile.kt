package com.mobilefork.hermesagent.voice

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Reads and writes the plain WAV files the voice samples are kept in (16-bit PCM or 32-bit float). */
internal object WavFile {
    class Wav(val samples: FloatArray, val sampleRate: Int)

    fun read(file: File): Wav? = runCatching { parse(file.readBytes()) }.getOrNull()

    internal fun parse(bytes: ByteArray): Wav? {
        if (bytes.size < 44) return null
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (String(bytes, 0, 4, Charsets.US_ASCII) != "RIFF" || String(bytes, 8, 4, Charsets.US_ASCII) != "WAVE") return null
        var position = 12
        var format = 0
        var channels = 1
        var rate = 0
        var bits = 16
        while (position + 8 <= bytes.size) {
            val id = String(bytes, position, 4, Charsets.US_ASCII)
            val size = buffer.getInt(position + 4)
            val body = position + 8
            if (size < 0) return null
            if (id == "fmt " && body + 16 <= bytes.size) {
                format = buffer.getShort(body).toInt() and 0xFFFF
                channels = buffer.getShort(body + 2).toInt()
                rate = buffer.getInt(body + 4)
                bits = buffer.getShort(body + 14).toInt()
            } else if (id == "data") {
                val length = minOf(size, bytes.size - body)
                return decode(buffer, body, length, format, channels, rate, bits)
            }
            position = body + size + (size and 1)
        }
        return null
    }

    private fun decode(buffer: ByteBuffer, start: Int, length: Int, format: Int, channels: Int, rate: Int, bits: Int): Wav? {
        if (channels < 1 || rate <= 0) return null
        val bytesPerSample = bits / 8
        if (!((format == 1 && bits == 16) || (format == 3 && bits == 32))) return null
        val frames = length / (bytesPerSample * channels)
        val out = FloatArray(frames)
        for (frame in 0 until frames) {
            var sum = 0f
            for (channel in 0 until channels) {
                val at = start + (frame * channels + channel) * bytesPerSample
                sum += if (bits == 16) buffer.getShort(at) / 32768f else buffer.getFloat(at)
            }
            out[frame] = sum / channels
        }
        return Wav(out, rate)
    }

    fun write(file: File, samples: FloatArray, sampleRate: Int) {
        val data = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in samples) data.putShort((sample.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray()).putInt(36 + data.capacity()).put("WAVE".toByteArray())
        header.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(sampleRate)
        header.putInt(sampleRate * 2).putShort(2).putShort(16)
        header.put("data".toByteArray()).putInt(data.capacity())
        file.outputStream().use { it.write(header.array()); it.write(data.array()) }
    }
}
