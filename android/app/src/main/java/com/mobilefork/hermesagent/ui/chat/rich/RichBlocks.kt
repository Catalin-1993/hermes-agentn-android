package com.mobilefork.hermesagent.ui.chat.rich

/** The fenced blocks a reply can draw instead of printing, as the desktop app's chat does. */
internal enum class RichLanguage(val fence: String) {
    Widget("widget"),
    Html("html"),
    Chart("chart"),
    Mermaid("mermaid"),
    Svg("svg");

    companion object {
        fun fromFence(name: String): RichLanguage? = entries.firstOrNull { it.fence == name.lowercase() }
    }
}

internal sealed interface RichSegment {
    data class Text(val text: String) : RichSegment

    /** [closed] is false while the block is still being written; it is only drawn once closed. */
    data class Block(val language: RichLanguage, val code: String, val closed: Boolean) : RichSegment
}

private val FENCE_OPEN = Regex("""^ {0,3}(`{3,})\s*([A-Za-z0-9_-]*)\s*$""")
private val WHOLE_PAGE_START = Regex("""^\s*(<!doctype\s+html|<html[\s>])""", RegexOption.IGNORE_CASE)
private val WHOLE_PAGE_END = Regex("""</html\s*>\s*$""", RegexOption.IGNORE_CASE)

/**
 * Splits a reply into text and drawable blocks. Other code fences stay in the text, and a drawable
 * fence quoted inside one of them stays code. A reply that is nothing but an HTML page counts as a
 * block too, because models often answer "show me a page" without a fence.
 */
internal fun parseRichSegments(content: String): List<RichSegment> {
    if ("```" !in content) return wholePageOrText(content)
    val segments = mutableListOf<RichSegment>()
    val text = StringBuilder()
    val code = StringBuilder()
    var open: RichLanguage? = null
    var openTicks = 0
    var plainFenceTicks = 0

    fun flushText() {
        if (text.isNotBlank()) segments += RichSegment.Text(text.toString().trim('\n'))
        text.clear()
    }

    for (line in content.replace("\r\n", "\n").split('\n')) {
        val fence = FENCE_OPEN.find(line)
        val ticks = fence?.groupValues?.get(1)?.length ?: 0
        val name = fence?.groupValues?.get(2).orEmpty()
        when {
            open != null -> {
                if (fence != null && name.isEmpty() && ticks >= openTicks) {
                    flushText()
                    segments += RichSegment.Block(open!!, code.toString().trim('\n'), closed = true)
                    code.clear()
                    open = null
                } else {
                    code.append(line).append('\n')
                }
            }
            plainFenceTicks > 0 -> {
                text.append(line).append('\n')
                if (fence != null && name.isEmpty() && ticks >= plainFenceTicks) plainFenceTicks = 0
            }
            fence != null -> {
                val language = RichLanguage.fromFence(name)
                if (language != null) {
                    open = language
                    openTicks = ticks
                } else {
                    plainFenceTicks = ticks
                    text.append(line).append('\n')
                }
            }
            else -> text.append(line).append('\n')
        }
    }
    flushText()
    open?.let { segments += RichSegment.Block(it, code.toString().trim('\n'), closed = false) }
    return segments.ifEmpty { listOf(RichSegment.Text(content)) }
}

private fun wholePageOrText(content: String): List<RichSegment> =
    if (WHOLE_PAGE_START.containsMatchIn(content) && WHOLE_PAGE_END.containsMatchIn(content)) {
        listOf(RichSegment.Block(RichLanguage.Html, content.trim(), closed = true))
    } else {
        listOf(RichSegment.Text(content))
    }

/** The reply without its drawn blocks: what a voice reads aloud and a list preview shows. */
internal fun textWithoutRichBlocks(content: String): String =
    parseRichSegments(content)
        .filterIsInstance<RichSegment.Text>()
        .joinToString("\n\n") { it.text }
        .trim()
