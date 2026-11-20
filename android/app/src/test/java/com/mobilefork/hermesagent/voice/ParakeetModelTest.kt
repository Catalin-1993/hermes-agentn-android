package com.mobilefork.hermesagent.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParakeetModelTest {
    @Test
    fun everyFileIsPinnedToAFixedRevisionWithAFullChecksum() {
        assertEquals(setOf("encoder.int8.onnx", "decoder.int8.onnx", "joiner.int8.onnx", "tokens.txt"), ParakeetModel.files.map { it.name }.toSet())
        for (file in ParakeetModel.files) {
            assertTrue("${file.name} must come over https", file.url.startsWith("https://"))
            assertTrue("${file.name} must not follow a moving branch", "/resolve/main/" !in file.url)
            assertTrue("${file.name} needs a SHA-256", Regex("[0-9a-f]{64}").matches(file.sha256))
            assertTrue(file.bytes > 0)
        }
    }

    @Test
    fun theTotalIsTheSumOfTheFiles() {
        assertEquals(ParakeetModel.files.sumOf { it.bytes }, ParakeetModel.totalBytes)
    }

    @Test
    fun theLicenceCreditNamesTheModel() {
        assertTrue("CC BY 4.0" in ParakeetModel.CREDIT && "Parakeet" in ParakeetModel.CREDIT)
    }
}
