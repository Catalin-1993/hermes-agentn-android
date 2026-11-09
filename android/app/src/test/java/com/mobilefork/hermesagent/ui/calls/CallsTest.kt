package com.mobilefork.hermesagent.ui.calls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CallsTest {
    @Test
    fun anAnswerIsReadAloudWithoutCodeLinksOrMarkup() {
        val spoken = spokenText(
            "## Plan\n- **Prima** idee, vezi [site](https://exemplu.ro)\n```kotlin\nval x = 1\n```\nGata `ok` https://exemplu.ro/x",
        )
        assertFalse(spoken.contains("```"))
        assertFalse(spoken.contains("http"))
        assertFalse(spoken.contains("*"))
        assertFalse(spoken.contains("#"))
        assertTrue(spoken.contains("Prima idee"))
        assertTrue(spoken.contains("vezi site"))
        assertTrue(spoken.contains("Gata ok"))
        assertFalse("code is never read out", spoken.contains("val x"))
    }

    @Test
    fun longAnswersAreSplitAtSentencesWithinTheEngineLimit() {
        val sentence = "Aceasta este o propoziție destul de lungă. "
        val pieces = splitForSpeech(sentence.repeat(200), 500)
        assertTrue(pieces.size > 1)
        assertTrue(pieces.all { it.length <= 500 })
        assertTrue("pieces end at a sentence", pieces.dropLast(1).all { it.endsWith(".") })
        assertEquals(listOf("Scurt."), splitForSpeech("Scurt.", 500))
    }

    @Test
    fun onlyDialableCharactersSurviveInANumber() {
        assertEquals("+40722123456", dialableNumber("+40 (722) 123-456"))
        assertEquals("*21#", dialableNumber("*21#"))
        assertEquals("", dialableNumber("abc"))
    }

    @Test
    fun theCallLogKeepsNewestFirstAndOnlyTwoHundredCalls() {
        val log = CallLogStore(RuntimeEnvironment.getApplication())
        repeat(205) { index ->
            log.add(CallRecord("id-$index", CallKind.Agent, "main", "Brain", startedAtEpochMs = 1_000L + index, durationSeconds = index))
        }
        val calls = log.list()
        assertEquals(200, calls.size)
        assertEquals("id-204", calls.first().id)
        assertEquals("id-5", calls.last().id)
        log.add(CallRecord("id-204", CallKind.Person, "0722", "Ana", startedAtEpochMs = 5_000L))
        assertEquals("the same call is kept once", 200, log.list().size)
        assertEquals("Ana", log.list().first().name)
    }

    @Test
    fun theTimerReadsLikeAPhone() {
        assertEquals("00:00", formatTimer(0))
        assertEquals("01:05", formatTimer(65))
        assertEquals("12:34", formatTimer(754))
    }
}
