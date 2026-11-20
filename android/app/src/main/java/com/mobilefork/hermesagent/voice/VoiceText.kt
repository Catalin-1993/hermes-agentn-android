package com.mobilefork.hermesagent.voice

/** What is said aloud, and in which language a model is asked to say it. */
internal object VoiceText {
    private const val MAX_CHARS = 3_000

    fun supertonicLanguage(spec: VoiceModelSpec, languageTag: String): String {
        val code = languageTag.substringBefore('-').substringBefore('_').lowercase()
        return if (code in spec.languages) code else "en"
    }

    /** Markdown marks and the like are not read aloud; long text is cut at a sentence end. */
    fun speakable(raw: String): String {
        val cleaned = raw
            .replace(Regex("```[\\s\\S]*?```"), " ")
            .replace(Regex("[*_`#>|~]+"), "")
            .replace(Regex("\\[([^\\]]*)]\\([^)]*\\)"), "$1")
            .replace(Regex("[\\u2022\\u25CF]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleaned.length <= MAX_CHARS) return cleaned
        val cut = cleaned.take(MAX_CHARS)
        val end = maxOf(cut.lastIndexOf(". "), cut.lastIndexOf("! "), cut.lastIndexOf("? "))
        return if (end > MAX_CHARS / 2) cut.take(end + 1) else cut
    }
}
