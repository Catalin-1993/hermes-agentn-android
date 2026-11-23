package com.mobilefork.hermesagent.ui.chat

/** The agent ends its last reply with one of these when it is finished, or when it needs the person. */
internal const val GOAL_DONE = "[[DONE]]"
internal const val GOAL_NEEDS_INPUT = "[[NEEDS_INPUT]]"

/** The most turns one request may run on its own before it hands back to the person. */
internal const val GOAL_MAX_TURNS = 12

/** What the agent is told while "Work until done" is on. */
internal const val GOAL_DIRECTIVE =
    "Work-until-done mode is on. Carry the whole task through with your tools, taking sensible decisions yourself " +
        "instead of asking about small things. When everything is finished, end your final reply with $GOAL_DONE. " +
        "If you cannot go on without the person, say exactly what you need and end with $GOAL_NEEDS_INPUT."

/** The message sent between turns while the task is not yet finished. */
internal const val GOAL_CONTINUE_PROMPT =
    "Keep going until the task is done. When everything is finished, end your reply with $GOAL_DONE; " +
        "if you need me, say what and end with $GOAL_NEEDS_INPUT."

internal enum class GoalStep { Continue, Stop }

/**
 * Whether to send another turn on the agent's own. It stops when the agent said it is done or needs the
 * person, when a turn failed or came back empty, and after [GOAL_MAX_TURNS] turns, so it can never run away.
 */
internal fun nextGoalStep(reply: String, turnsRun: Int, failed: Boolean): GoalStep = when {
    failed || reply.isBlank() -> GoalStep.Stop
    GOAL_DONE in reply || GOAL_NEEDS_INPUT in reply -> GoalStep.Stop
    turnsRun >= GOAL_MAX_TURNS -> GoalStep.Stop
    else -> GoalStep.Continue
}

/** A reply as the person reads it: without the end markers. */
internal fun withoutGoalMarkers(text: String): String =
    if (GOAL_DONE !in text && GOAL_NEEDS_INPUT !in text) text
    else text.replace(GOAL_DONE, "").replace(GOAL_NEEDS_INPUT, "").trimEnd()
