package com.mobilefork.hermesagent.ui.chat

/**
 * One wrong answer such as "I have no access to the Library tools" used to stay for good: the app
 * keeps every exchange as a memory and sends the conversation so far with each message, so the model
 * read its own refusal back and repeated it, in the same chat and in new ones. These helpers find
 * such answers so they are neither remembered nor sent back to the model.
 */
internal object WrongAccessClaims {
    private val TOPIC = Regex(
        "libr[aă]r|bibliotec|calendar|caiet|notit|notes?\\b|pagin|\\bpages?\\b",
        RegexOption.IGNORE_CASE,
    )

    private val DENIAL = Regex(
        listOf(
            // Romanian
            "nu\\s+am\\s+acces", "n-am\\s+acces", "nu\\s+(?:pot|reușesc|reusesc)\\b", "nu\\s+dispun",
            "nu\\s+(?:apare|apar|sunt|este|e)\\s+(?:în|in|printre|disponibil)", "nu\\s+am\\s+(?:unelte|instrumente|uneltele|instrumentele)",
            "nu\\s+primesc\\s+uneltele",
            // English
            "(?:don't|do\\s+not|doesn't|cannot|can't|can\\s+not|unable\\s+to|no)\\s+(?:have\\s+)?(?:any\\s+)?access",
            "not\\s+(?:in|among)\\s+(?:the|my)\\s+(?:list|available|tools)", "(?:isn't|is\\s+not|aren't|are\\s+not)\\s+available",
            "(?:don't|do\\s+not)\\s+have\\s+(?:the\\s+)?(?:tools?|ability)", "no\\s+(?:library|calendar)\\s+tools?",
        ).joinToString("|"),
        RegexOption.IGNORE_CASE,
    )

    /** Spoken out loud to the model in place of a removed refusal, so it does not copy the habit. */
    const val CORRECTION =
        "(An earlier reply here wrongly said the Library or Calendar tools were unavailable. " +
            "They are available: use brain_library and brain_calendar.)"

    /** True for a reply that says the Library or Calendar cannot be reached. Plain mentions do not count. */
    fun denies(text: String): Boolean {
        val reply = text.trim()
        if (reply.isEmpty() || reply.length > 1_500) return false
        return TOPIC.containsMatchIn(reply) && DENIAL.containsMatchIn(reply)
    }

    /** Keeps a refusal out of what the model reads; any other reply passes unchanged. */
    fun forModel(assistantText: String): String = if (denies(assistantText)) CORRECTION else assistantText

    /** Memory lines are "user asked: … | assistant answered: …": only the answer part can be a refusal. */
    fun memoryLineDenies(line: String): Boolean {
        val answer = line.substringAfter("assistant answered:", missingDelimiterValue = "")
        return answer.isNotBlank() && denies(answer)
    }
}
