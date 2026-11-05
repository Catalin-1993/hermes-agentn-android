package com.mobilefork.hermesagent.ui.chat

import com.mobilefork.hermesagent.data.AgentProfile

enum class AgentEventType(val persistedRole: String) {
    Thought("thought"),
    ToolCall("tool_call"),
    ToolResult("tool_result"),
    FileAccess("file_access"),
    ProcessLog("process_log"),
    FinalAnswer("assistant");

    companion object {
        fun fromRole(role: String): AgentEventType? = entries.firstOrNull { it.persistedRole == role }
    }
}

data class ChatUiMessage(
    val id: String,
    val role: String,
    val content: String,
    val createdAtEpochMs: Long,
    val attachments: List<ChatAttachment> = emptyList(),
    /** The agent that wrote it. Blank for the person's own messages and for older replies. */
    val senderAgentId: String = "",
) {
    val eventType: AgentEventType?
        get() = if (role == "user") null else AgentEventType.fromRole(role) ?: AgentEventType.FinalAnswer

    val timelineDepth: Int
        get() = when (eventType) {
            AgentEventType.ToolResult, AgentEventType.FileAccess, AgentEventType.ProcessLog -> 2
            AgentEventType.ToolCall -> 1
            else -> 0
        }
}

data class ChatTurn(
    val id: String,
    val userMessage: ChatUiMessage?,
    val assistantMessages: List<ChatUiMessage>,
)

data class ChatConversationSummary(
    val id: String,
    val title: String,
    val preview: String,
    val updatedLabel: String,
    val messageCount: Int,
    val isDefaultTitle: Boolean = false,
    val agentId: String = AgentProfile.MAIN_ID,
    val isGroup: Boolean = false,
    val memberIds: List<String> = emptyList(),
    val groupAvatarPath: String = "",
    val needsUser: Boolean = false,
)

/** Who a conversation is with, as its header and bubbles show it. */
data class ChatConversationParticipants(
    val agent: AgentProfile = AgentProfile.Main,
    val isGroup: Boolean = false,
    val groupName: String = "",
    val groupAvatarPath: String = "",
    val members: List<AgentProfile> = emptyList(),
    /** Everyone who ever wrote here may include agents since removed from the group. */
    val agentsById: Map<String, AgentProfile> = emptyMap(),
)

data class ChatAttachment(
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long = 0L,
)

data class ChatUiState(
    val activeConversationId: String = "",
    val activeConversationTitle: String = "New chat",
    val conversationSummaries: List<ChatConversationSummary> = emptyList(),
    val isShowingHistory: Boolean = false,
    val messages: List<ChatUiMessage> = emptyList(),
    val input: String = "",
    val attachments: List<ChatAttachment> = emptyList(),
    val isSending: Boolean = false,
    val isListening: Boolean = false,
    val showIntermediateSteps: Boolean = false,
    /** The group member answering right now. */
    val activeSpeakerId: String = "",
    val status: String = "",
    val error: String = "",
)

fun buildChatTurns(messages: List<ChatUiMessage>): List<ChatTurn> {
    val turns = mutableListOf<ChatTurn>()
    var pendingUser: ChatUiMessage? = null
    var assistantMessages = mutableListOf<ChatUiMessage>()

    fun flush() {
        if (pendingUser == null && assistantMessages.isEmpty()) return
        val id = pendingUser?.id ?: assistantMessages.first().id
        turns += ChatTurn(
            id = id,
            userMessage = pendingUser,
            assistantMessages = assistantMessages.toList(),
        )
        pendingUser = null
        assistantMessages = mutableListOf()
    }

    messages.forEach { message ->
        if (message.role == "user") {
            flush()
            pendingUser = message
        } else {
            assistantMessages += message
        }
    }
    flush()
    return turns
}

fun minuteBucket(epochMs: Long): Long = epochMs / 60_000L

internal data class QuickPromptSendDecision(
    val shouldSend: Boolean,
    val blockedStatus: String? = null,
)

internal fun evaluateQuickPromptSend(prompt: String, state: ChatUiState): QuickPromptSendDecision {
    val normalized = prompt.trim()
    if (normalized.isEmpty() || state.isSending) {
        return QuickPromptSendDecision(shouldSend = false)
    }
    if (state.input.isNotBlank() || state.attachments.isNotEmpty()) {
        return QuickPromptSendDecision(
            shouldSend = false,
            blockedStatus = "Send or clear the current draft before running a signal quick action.",
        )
    }
    return QuickPromptSendDecision(shouldSend = true)
}

fun shortPromptPreview(text: String, maxLength: Int = 96): String {
    val singleLine = text
        .lineSequence()
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
        .replace(Regex("\\s+"), " ")
        .trim()
    return when {
        singleLine.isBlank() -> "Attachment"
        singleLine.length <= maxLength -> singleLine
        else -> singleLine.take(maxLength - 1).trimEnd() + "…"
    }
}
