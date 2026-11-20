package com.mobilefork.hermesagent.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceModelCatalogTest {
    @Test
    fun everyModelCanBeDownloadedSafely() {
        val ids = VoiceModelCatalog.all.map { it.id }
        assertEquals("ids are unique", ids.size, ids.toSet().size)
        VoiceModelCatalog.all.forEach { spec ->
            assertTrue("${spec.id} has something to download", spec.downloads.isNotEmpty())
            spec.downloads.forEach { item ->
                assertTrue("${item.fileName} comes over https", item.url.startsWith("https://"))
                assertTrue("${item.fileName} has a SHA-256", Regex("[0-9a-f]{64}").matches(item.sha256))
                assertTrue("${item.fileName} has a size", item.bytes > 0)
                assertEquals("an archive is opened, a plain file is kept", item.fileName.endsWith(".tar.bz2"), item.extract)
            }
            assertTrue("${spec.id} credit is shown", spec.credit.isNotBlank())
        }
    }

    @Test
    fun noModelNeedsMoreThanAGigabyteToDownload() {
        VoiceModelCatalog.all.forEach { spec ->
            assertTrue("${spec.id} is ${spec.downloadBytes} bytes", spec.downloadBytes < 1_000_000_000L)
        }
    }

    @Test
    fun fiveVoicesAndTwoDetectorsAreOffered() {
        assertEquals(5, VoiceModelCatalog.voices.size)
        assertEquals(2, VoiceModelCatalog.detectors.size)
        assertTrue(VoiceModelCatalog.voices.all { it.isVoice })
        assertTrue(VoiceModelCatalog.detectors.none { it.isVoice })
        assertTrue("at least two voices can copy a voice", VoiceModelCatalog.voices.count { it.canClone } >= 2)
    }

    @Test
    fun romanianIsOfferedWhereItIsReallySpoken() {
        assertTrue(VoiceModelCatalog.find("piper-ro-mihai")!!.speaks("ro-RO"))
        assertTrue(VoiceModelCatalog.find("supertonic-3")!!.speaks("ro"))
        assertFalse(VoiceModelCatalog.find("kokoro-82m")!!.speaks("ro"))
        assertFalse(VoiceModelCatalog.find("pocket-tts")!!.speaks("ro"))
        assertTrue("an unknown language is not refused up front", VoiceModelCatalog.find("kokoro-82m")!!.speaks(""))
    }

    @Test
    fun aCloningVoiceThatNeedsWordsSaysSo() {
        val zip = VoiceModelCatalog.find("zipvoice")!!
        assertTrue(zip.canClone && zip.needsReferenceText)
        val pocket = VoiceModelCatalog.find("pocket-tts")!!
        assertTrue(pocket.canClone && !pocket.needsReferenceText)
    }

    @Test
    fun kokoroVoicesAreNamedInTheOrderTheModelNumbersThem() {
        val names = VoiceModelCatalog.find("kokoro-82m")!!.voiceNames
        assertEquals(54, names.size)
        assertEquals("Heart (American, female)", names[3])
        assertEquals("Michael (American, male)", names[16])
        assertEquals("Emma (British, female)", names[21])
    }

    @Test
    fun everyVoiceHasANameForEachOfItsSpeakers() {
        assertEquals(10, VoiceModelCatalog.find("supertonic-3")!!.voiceNames.size)
        VoiceModelCatalog.voices.forEach { assertTrue(it.voiceNames.isNotEmpty()) }
        assertNotNull(VoiceModelCatalog.find("silero-vad"))
    }
}
