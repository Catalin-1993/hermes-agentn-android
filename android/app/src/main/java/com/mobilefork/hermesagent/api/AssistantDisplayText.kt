package com.mobilefork.hermesagent.api

private val emptyThinkingPreface = Regex("""\A[\s]*(?:<think\s*>[\s]*</think\s*>[\t ]*\r?\n[\s]*)+""", RegexOption.IGNORE_CASE)

/** Remove empty model prefaces only; preserve literal inline/fenced markup and all substantive text. */
fun assistantDisplayText(raw: String): String {
    // The end markers of "Work until done" are for the app, not for the reader.
    val text = raw.replace("[[DONE]]", "").replace("[[NEEDS_INPUT]]", "").let { if (it.length != raw.length) it.trimEnd() else it }
    val preface = emptyThinkingPreface.find(text) ?: return text
    val remainder = text.substring(preface.range.last + 1)
    return if (remainder.isNotBlank()) remainder else text
}
