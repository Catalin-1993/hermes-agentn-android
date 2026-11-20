package com.mobilefork.hermesagent.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sin

class VoiceSamplesAndTextTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun aRecordingSurvivesBeingSavedAndRead() {
        val tone = FloatArray(16_000) { sin(it * 0.05f) * 0.5f }
        val file = folder.newFile("tone.wav")
        WavFile.write(file, tone, 16_000)
        val back = WavFile.read(file)!!
        assertEquals(16_000, back.sampleRate)
        assertEquals(tone.size, back.samples.size)
        assertTrue(tone.indices.all { abs(tone[it] - back.samples[it]) < 0.001f })
    }

    @Test
    fun aStereoFloatFileIsMixedToMono() {
        val frames = 4
        val data = ByteBuffer.allocate(frames * 2 * 4).order(ByteOrder.LITTLE_ENDIAN)
        repeat(frames) { data.putFloat(0.2f).putFloat(0.6f) }
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray()).putInt(36 + data.capacity()).put("WAVE".toByteArray())
        header.put("fmt ".toByteArray()).putInt(16).putShort(3).putShort(2).putInt(24_000)
        header.putInt(24_000 * 8).putShort(8).putShort(32)
        header.put("data".toByteArray()).putInt(data.capacity())
        val wav = WavFile.parse(header.array() + data.array())!!
        assertEquals(24_000, wav.sampleRate)
        assertEquals(frames, wav.samples.size)
        assertEquals(0.4f, wav.samples[0], 0.0001f)
    }

    @Test
    fun somethingThatIsNotAWavIsNotRead() {
        assertNull(WavFile.parse(ByteArray(100)))
        assertNull(WavFile.parse("RIFF".toByteArray()))
    }

    @Test
    fun markdownAndCodeAreNotReadAloud() {
        val spoken = VoiceText.speakable("# Titlu\n**Bună** ziua, `x = 1` vezi [aici](https://exemplu.ro).\n```kotlin\nval a = 1\n```\n- un punct")
        assertEquals("Titlu Bună ziua, x = 1 vezi aici. - un punct", spoken)
    }

    @Test
    fun aLongReplyIsCutAtASentenceEnd() {
        val long = "O propoziție scurtă. ".repeat(400)
        val spoken = VoiceText.speakable(long)
        assertTrue(spoken.length <= 3_000)
        assertTrue(spoken.endsWith("."))
    }

    @Test
    fun supertonicSpeaksTheAppLanguageWhenItKnowsItAndEnglishOtherwise() {
        val supertonic = VoiceModelCatalog.find("supertonic-3")!!
        assertEquals("ro", VoiceText.supertonicLanguage(supertonic, "ro-RO"))
        assertEquals("fr", VoiceText.supertonicLanguage(supertonic, "fr"))
        assertEquals("en", VoiceText.supertonicLanguage(supertonic, "xx"))
        assertNotNull(supertonic)
    }
}
