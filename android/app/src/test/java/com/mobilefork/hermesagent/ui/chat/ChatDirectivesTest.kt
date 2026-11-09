package com.mobilefork.hermesagent.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatDirectivesTest {
    @Test
    fun autoThinkingWithoutResearchAddsNothing() {
        assertEquals("", chatDirectives(ChatOptions()))
    }

    @Test
    fun researchAsksForSeveralSourcesAndAReport() {
        val text = chatDirectives(ChatOptions(research = true))
        assertTrue(text.contains("several independent sources"))
        assertTrue(text.contains("sources with links"))
    }

    @Test
    fun eachThinkingLevelAsksForADifferentDepth() {
        val low = chatDirectives(ChatOptions(thinkingLevel = 1))
        val medium = chatDirectives(ChatOptions(thinkingLevel = 2))
        val high = chatDirectives(ChatOptions(thinkingLevel = 3))
        assertTrue(low.contains("low"))
        assertTrue(medium.contains("medium"))
        assertTrue(high.contains("high"))
        assertEquals(3, setOf(low, medium, high).size)
        assertTrue(chatDirectives(ChatOptions(research = true, thinkingLevel = 3)).contains("Research mode"))
    }

    @Test
    fun onlyTextFilesAreReadIntoAMessageAndPicturesStayPictures() {
        assertTrue(isReadableTextAttachment("text/markdown", "a.md"))
        assertTrue(isReadableTextAttachment("application/octet-stream", "notes.TXT"))
        assertTrue(isReadableTextAttachment("application/json", "data"))
        assertFalse(isReadableTextAttachment("application/pdf", "paper.pdf"))
        assertTrue(isImageAttachment("image/jpeg", "p.jpg"))
        assertTrue(isImageAttachment("image/*", "camera.jpg"))
        assertFalse(isImageAttachment("text/plain", "a.txt"))
    }
}
