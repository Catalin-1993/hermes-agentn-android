package com.mobilefork.hermesagent.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatReactionsTest {
    @Test
    fun anAgentReactionIsTakenOffTheFrontOfTheReply() {
        assertEquals("🎉" to "Congratulations!", splitAgentReaction("[react:🎉] Congratulations!"))
        assertEquals("👍" to "Sure.", splitAgentReaction("  [react: 👍 ]\nSure."))
    }

    @Test
    fun anythingThatIsNotAnEmojiIsLeftAlone() {
        assertEquals("" to "[react:yes] ok", splitAgentReaction("[react:yes] ok"))
        assertEquals("" to "Plain reply", splitAgentReaction("Plain reply"))
        assertEquals("" to "text [react:👍] later", splitAgentReaction("text [react:👍] later"))
    }

    @Test
    fun theTagIsHiddenWhileItArrivesAndAfter() {
        assertEquals("", withoutReactionTag("[rea"))
        assertEquals("", withoutReactionTag("[react:👍"))
        assertEquals("Done.", withoutReactionTag("[react:👍] Done."))
        assertEquals("A [link] here", withoutReactionTag("A [link] here"))
    }

    @Test
    fun choosingTheSameEmojiAgainTakesItBack() {
        assertEquals("❤️", toggledReaction("", "❤️"))
        assertEquals("", toggledReaction("❤️", "❤️"))
        assertEquals("👍", toggledReaction("❤️", "👍"))
    }
}
