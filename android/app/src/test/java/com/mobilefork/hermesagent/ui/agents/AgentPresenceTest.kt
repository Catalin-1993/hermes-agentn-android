package com.mobilefork.hermesagent.ui.agents

import org.junit.Assert.assertEquals
import org.junit.Test

class AgentPresenceTest {
    @Test
    fun theAgentRestsWhenNothingIsBeingSent() {
        assertEquals(AgentPresence.Idle, presenceOf(isSending = false, writing = true, toolRunning = true))
    }

    @Test
    fun itThinksUntilATool_orTheReply_showsWhatItIsDoing() {
        assertEquals(AgentPresence.Thinking, presenceOf(isSending = true, writing = false, toolRunning = false))
        assertEquals(AgentPresence.Working, presenceOf(isSending = true, writing = false, toolRunning = true))
    }

    @Test
    fun writingTheReplyOutranksTheToolItUsedBefore() {
        assertEquals(AgentPresence.Writing, presenceOf(isSending = true, writing = true, toolRunning = true))
    }
}
