package com.mobilefork.hermesagent.ui.chat

import com.mobilefork.hermesagent.api.HermesToolActivity
import com.mobilefork.hermesagent.api.HermesToolPhase
import com.mobilefork.hermesagent.data.ConversationStore
import com.mobilefork.hermesagent.data.StoredConversationMessage
import kotlinx.coroutines.flow.MutableStateFlow
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
class ChatToolActivityRobotest {
    @Test
    fun toolEventsPersistBeforeFinalAndStopRejectsLateOrDeletedConversationEvents() {
        val store = ConversationStore(RuntimeEnvironment.getApplication())
        store.clearAll()
        try {
            val conversation = store.createNewConversation("Pinned title")
            val coordinator = ChatSendRequestCoordinator()
            val request = coordinator.begin(conversation.sessionId, "final") {
                store.upsertMessage(conversation.sessionId, StoredConversationMessage("final", "assistant", "", 1))
            }!!
            val state = MutableStateFlow(ChatUiState(activeConversationId = conversation.sessionId,
                messages = listOf(ChatUiMessage("final", "assistant", "", 1))))
            val started = HermesToolActivity("call", "terminal", HermesToolPhase.RUNNING, "pwd").toTimelineEvent()
            val result = HermesToolActivity("call", "terminal", HermesToolPhase.COMPLETED, "/workspace").toTimelineEvent()
            appendOwnedToolActivity(coordinator, request, store, state, started, "start")
            appendOwnedToolActivity(coordinator, request, store, state, started, "start")
            appendOwnedToolActivity(coordinator, request, store, state, result, "result")
            assertEquals(listOf("tool_call", "tool_result", "assistant"), state.value.messages.map { it.role })
            assertEquals(state.value.messages.map { it.id }, store.loadConversation(conversation.sessionId)!!.messages.map { it.id })
            assertEquals("Pinned title", store.loadConversation(conversation.sessionId)!!.title)
            coordinator.stopActive {}
            appendOwnedToolActivity(coordinator, request, store, state, result, "late")
            assertFalse(state.value.messages.any { it.id == "late" })
            store.clearConversation(conversation.sessionId)
            appendOwnedToolActivity(coordinator, request, store, state, result, "after-delete")
            assertEquals(null, store.loadConversation(conversation.sessionId))
        } finally {
            store.clearAll()
        }
    }

    @Test
    fun streamFailureDetailsAreShownWithCredentialsRedactedAndLengthBounded() {
        val message = streamFailureUiMessage(
            "The stream was interrupted; the request was not resent.",
            "HTTP 403: invalid key Bearer secret-token sk-or-v1-1234567890abcdef\n" + "x".repeat(800),
        )

        assertTrue(message.contains("HTTP 403"))
        assertTrue(message.contains("Bearer [redacted]"))
        assertTrue(message.contains("[redacted API key]"))
        assertFalse(message.contains("secret-token"))
        assertFalse(message.contains("sk-or-v1-"))
        assertTrue(message.length < 600)
    }

    @Test
    fun streamMayBeReplayedOnlyBeforeAnyToolActivityWasObserved() {
        assertTrue(mayReplayFailedStream(receivedToolActivity = false))
        assertFalse(mayReplayFailedStream(receivedToolActivity = true))
        assertFalse(mayReplayFailedStream(receivedToolActivity = false, agentEndpoint = true))
    }
}
