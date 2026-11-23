package com.mobilefork.hermesagent.ui.chat

/** The emoji the person can react with, in the order the menu shows them. */
internal val REACTION_CHOICES = listOf("👍", "❤️", "😂", "🎉", "🤔", "👎")

private val agentReaction = Regex("""^\s*\[react:\s*([^\]\s]{1,12})\s*]\s*""")
private val unfinishedAgentReaction = Regex("""^\s*\[react[^\]]{0,16}$""")

private fun isEmoji(text: String) = text.isNotBlank() && text.none { it.isLetterOrDigit() || it == '[' || it == ']' }

/**
 * An agent reacts to the person's message by opening its reply with `[react:👍]`. Returns the emoji and the
 * reply without that tag, or an empty emoji and the text unchanged when there is no valid tag.
 */
internal fun splitAgentReaction(reply: String): Pair<String, String> {
    val match = agentReaction.find(reply) ?: return "" to reply
    val emoji = match.groupValues[1]
    if (!isEmoji(emoji)) return "" to reply
    return emoji to reply.substring(match.range.last + 1)
}

/** A reply as it is drawn: without the reaction tag, nor the first few letters of one still arriving. */
internal fun withoutReactionTag(reply: String): String {
    if (unfinishedAgentReaction.containsMatchIn(reply)) return ""
    val (emoji, rest) = splitAgentReaction(reply)
    return if (emoji.isEmpty()) reply else rest
}

/** Reacting with the emoji already chosen takes the reaction back. */
internal fun toggledReaction(current: String, chosen: String): String = if (current == chosen) "" else chosen
