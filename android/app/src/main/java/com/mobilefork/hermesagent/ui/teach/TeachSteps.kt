package com.mobilefork.hermesagent.ui.teach

import org.json.JSONObject

/** One thing the person did in the browser while recording. */
internal sealed interface TeachStep {
    data class Open(val url: String) : TeachStep
    data class Click(val target: String, val selector: String) : TeachStep
    data class Type(val target: String, val selector: String, val value: String) : TeachStep
}

private const val MAX_STEPS = 80

/** Reads one event sent by the page's recorder; null for anything that is not a step worth keeping. */
internal fun parseTeachEvent(json: String): TeachStep? {
    val event = runCatching { JSONObject(json) }.getOrNull() ?: return null
    val selector = event.optString("selector").take(200)
    val target = event.optString("text").trim().replace(Regex("\\s+"), " ").take(80)
    return when (event.optString("type")) {
        "click" -> if (selector.isBlank() && target.isBlank()) null else TeachStep.Click(target, selector)
        "input" -> TeachStep.Type(target, selector, event.optString("value").take(200))
        else -> null
    }
}

/**
 * Adds a step to the recording. Two page loads of the same address in a row count once, a field typed into
 * again replaces its earlier text, and the recording never grows past [MAX_STEPS].
 */
internal fun List<TeachStep>.plusStep(step: TeachStep): List<TeachStep> {
    val last = lastOrNull()
    return when {
        step is TeachStep.Open && last is TeachStep.Open && last.url == step.url -> this
        step is TeachStep.Type && last is TeachStep.Type && last.selector == step.selector -> dropLast(1) + step
        size >= MAX_STEPS -> this
        else -> this + step
    }
}

/** A step in plain words, as the agent will read it. */
internal fun TeachStep.describe(): String = when (this) {
    is TeachStep.Open -> "Open $url"
    is TeachStep.Click -> "Click ${target.ifBlank { "the element" }.let { "\"$it\"" }}" + selectorNote(selector)
    is TeachStep.Type -> "Type \"$value\" into ${target.ifBlank { "the field" }.let { "\"$it\"" }}" + selectorNote(selector)
}

private fun selectorNote(selector: String) = if (selector.isBlank()) "" else " (selector: $selector)"

/** What the agent is asked after a recording: learn it, and say how to run it again. */
internal fun buildTeachPrompt(steps: List<TeachStep>): String = buildString {
    appendLine("I just did a task once in a browser. Learn it so you can do it for me by yourself next time.")
    appendLine()
    steps.forEachIndexed { index, step -> appendLine("${index + 1}. ${step.describe()}") }
    appendLine()
    append(
        "Save it as a skill with skill_manage, with a short clear name. Turn what changes from run to run " +
            "(the text I typed, the address) into inputs, and keep what stays the same as fixed steps. " +
            "Then tell me, in one sentence, how to ask you to run it.",
    )
}
