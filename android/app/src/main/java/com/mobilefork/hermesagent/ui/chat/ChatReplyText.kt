package com.mobilefork.hermesagent.ui.chat

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.mobilefork.hermesagent.ui.i18n.HermesStrings

internal const val CHAT_BOLD_START = '\u0001'
internal const val CHAT_BOLD_END = '\u0002'

private val HEADING = Regex("""^\s{0,3}#{1,6}\s+(.*?)\s*#*\s*$""")
private val BULLET = Regex("""^(\s*)[-*+•]\s+(.*)$""")
private val RULE = Regex("""^\s*([-*_])(\s*\1){2,}\s*$""")
private val QUOTE = Regex("""^\s*>\s?(.*)$""")

/**
 * An agent's reply as the chat draws it: Markdown's symbols become what they mean. Headings turn
 * into bold lines, `-`/`*` lists into bullets, rules and quote marks go, `**bold**` is drawn bold, and
 * runs of blank lines shrink to one. Everything [sanitizeChatDisplayText] already cleans stays clean.
 */
internal fun chatReplyAnnotated(raw: String, strings: HermesStrings? = null): AnnotatedString {
    val cleaned = sanitizeChatDisplayText(raw, strings, keepBold = true)
    val lines = cleaned.lines().mapNotNull { line ->
        when {
            RULE.matches(line) -> null
            HEADING.matches(line) -> HEADING.find(line)!!.groupValues[1].let { title ->
                CHAT_BOLD_START + title.withoutBoldMarks() + CHAT_BOLD_END
            }
            BULLET.matches(line) -> BULLET.find(line)!!.let { m ->
                "  ".repeat(m.groupValues[1].length / 2) + "•  " + m.groupValues[2]
            }
            QUOTE.matches(line) -> QUOTE.find(line)!!.groupValues[1]
            else -> line
        }
    }
    val text = lines.joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim('\n')
    return boldMarksToSpans(text)
}

/** Drops the marks and draws what was between them bold. An unclosed mark bolds nothing. */
internal fun boldMarksToSpans(text: String): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    while (cursor < text.length) {
        val start = text.indexOf(CHAT_BOLD_START, cursor)
        if (start < 0) {
            append(text.substring(cursor).withoutBoldMarks())
            break
        }
        val end = text.indexOf(CHAT_BOLD_END, start + 1)
        append(text.substring(cursor, start).withoutBoldMarks())
        if (end < 0) {
            append(text.substring(start + 1).withoutBoldMarks())
            break
        }
        val from = length
        append(text.substring(start + 1, end).withoutBoldMarks())
        addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), from, length)
        cursor = end + 1
    }
}

private fun String.withoutBoldMarks(): String = filter { it != CHAT_BOLD_START && it != CHAT_BOLD_END }
