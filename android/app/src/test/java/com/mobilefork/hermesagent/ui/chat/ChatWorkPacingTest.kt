package com.mobilefork.hermesagent.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ChatWorkPacingTest {
    @get:Rule val compose = createComposeRule()

    private fun step(id: String, running: Boolean = false) = ChatWorkStep(id, "web_search", "query $id", running)

    private var steps by mutableStateOf(emptyList<ChatWorkStep>())
    private var sending by mutableStateOf(false)
    private var shown = 0
    private var live = false

    private fun host(turn: String = "turn-1") {
        compose.setContent {
            val paced = rememberPacedWork(steps, sending, turn)
            shown = paced.shown.size
            live = paced.live
        }
    }

    @Test
    fun stepsAppearOneAtATimeEachHeldForTwoSeconds() {
        host()
        compose.mainClock.autoAdvance = false
        sending = true
        steps = listOf(step("a"), step("b"), step("c"))
        compose.mainClock.advanceTimeBy(300)
        assertEquals("the first step shows at once", 1, shown)
        assertTrue(live)
        compose.mainClock.advanceTimeBy(1_500)
        assertEquals("still the first step before two seconds", 1, shown)
        compose.mainClock.advanceTimeBy(700)
        assertEquals("the second comes after two seconds", 2, shown)
        compose.mainClock.advanceTimeBy(2_100)
        assertEquals(3, shown)
    }

    @Test
    fun theCardStaysUpTwoSecondsAfterTheWorkEndsThenSettles() {
        host()
        compose.mainClock.autoAdvance = false
        sending = true
        steps = listOf(step("a"))
        compose.mainClock.advanceTimeBy(300)
        sending = false
        compose.mainClock.advanceTimeBy(1_000)
        assertTrue("a step that finished in a flash is still on screen", live)
        compose.mainClock.advanceTimeBy(1_500)
        assertFalse("then the card settles", live)
        assertEquals(1, shown)
    }

    @Test
    fun aQuickTurnWithNoStepsStillShowsAThinkingMomentButAnOpenedOldTurnShowsNothing() {
        host()
        compose.mainClock.autoAdvance = false
        sending = true
        compose.mainClock.advanceTimeBy(300)
        assertTrue("the card says it is thinking", live)
        assertEquals(0, shown)
        sending = false
        compose.mainClock.advanceTimeBy(2_300)
        assertFalse(live)
    }

    @Test
    fun stepsOfATurnThatIsAlreadyOverAreNotReplayed() {
        steps = listOf(step("a"), step("b"))
        sending = false
        host("old-turn")
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(5_000)
        assertEquals(0, shown)
        assertFalse(live)
    }
}
