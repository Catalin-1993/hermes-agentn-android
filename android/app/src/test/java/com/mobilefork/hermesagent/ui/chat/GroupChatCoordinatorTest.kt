package com.mobilefork.hermesagent.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupChatCoordinatorTest {
    private val writer = GroupMember("w", "Scriitor")
    private val researcher = GroupMember("r", "Cercetător")
    private val coach = GroupMember("c", "Research Buddy")
    private val members = listOf(writer, researcher, coach)

    @Test
    fun mentionsMatchNamesInAnyCaseWithSpacesAndDiacritics() {
        assertEquals(setOf("r"), parseGroupMentions("Ce zici, @cercetător?", members))
        assertEquals(setOf("c"), parseGroupMentions("Hei @Research Buddy, ajută", members))
        assertEquals(setOf("c"), parseGroupMentions("Hei @researchbuddy", members))
        assertEquals(setOf("w", "r"), parseGroupMentions("@Scriitor și @Cercetător", members))
    }

    @Test
    fun mailAddressesAndLongerWordsAreNotMentions() {
        assertTrue(parseGroupMentions("scrie la ana@scriitor.ro", members).isEmpty())
        assertTrue(parseGroupMentions("@Scriitorul nu e membru", members).isEmpty())
        assertTrue(parseGroupMentions("fără mențiuni", members).isEmpty())
    }

    @Test
    fun everyoneMentionsNameAllMembers() {
        listOf("@toți", "@all", "@everyone", "@alle").forEach { word ->
            assertEquals(word, members.map { it.id }.toSet(), parseGroupMentions("$word ajutor", members))
        }
    }

    @Test
    fun passTextIsSilenceAndARealAnswerIsNot() {
        listOf("(pass)", " PASS ", "pass.", "").forEach { assertTrue(it, isGroupPassReply(it)) }
        listOf("Passport check", "I pass this to you").forEach { assertFalse(it, isGroupPassReply(it)) }
        assertTrue(couldBecomeGroupPass("(pa"))
        assertTrue(couldBecomeGroupPass(" "))
        assertFalse(couldBecomeGroupPass("Passp"))
        assertFalse(couldBecomeGroupPass("Da"))
    }

    private fun run(
        text: String,
        cancelled: () -> Boolean = { false },
        turn: (GroupMember, Int) -> GroupTurnResult,
    ): Pair<GroupRunResult, List<String>> {
        val speakers = mutableListOf<String>()
        val result = runGroupConversation(text, members, cancelled) { member ->
            speakers += member.id
            turn(member, speakers.size)
        }
        return result to speakers
    }

    @Test
    fun withNoMentionEveryoneAnswersOnceInOrderAndTheRoomSettles() {
        val (result, speakers) = run("Salut") { _, _ -> GroupTurnResult.Replied("Salut!") }
        assertEquals(listOf("w", "r", "c"), speakers)
        assertEquals(3, result.turns)
        assertFalse(result.cancelled)
    }

    @Test
    fun onlyTheMentionedMemberAnswers() {
        val (_, speakers) = run("@Scriitor ce zici?") { _, _ -> GroupTurnResult.Replied("Bine.") }
        assertEquals(listOf("w"), speakers)
    }

    @Test
    fun aReplyNamingAnotherMemberContinuesAndNamingItselfDoesNot() {
        val (_, speakers) = run("@Scriitor începe") { member, _ ->
            if (member.id == "w") GroupTurnResult.Replied("Eu, @Scriitor, întreb: @Cercetător?") else GroupTurnResult.Replied("Gata.")
        }
        assertEquals(listOf("w", "r"), speakers)
    }

    @Test
    fun discussionStopsAtThreeRoundsEvenIfMembersKeepCallingEachOther() {
        val (result, speakers) = run("@Scriitor începe") { member, _ ->
            GroupTurnResult.Replied(if (member.id == "w") "@Cercetător?" else "@Scriitor?")
        }
        assertEquals(listOf("w", "r", "w"), speakers)
        assertEquals(3, result.turns)
    }

    @Test
    fun oneSendNeverPassesTheTurnLimit() {
        val big = (1..6).map { GroupMember("m$it", "M$it") }
        val everyone = big.joinToString(" ") { "@${it.name}" }
        var calls = 0
        val result = runGroupConversation("Salut", big, { false }) { _ ->
            calls++
            GroupTurnResult.Replied(everyone)
        }
        assertEquals(GROUP_CHAT_MAX_TURNS, calls)
        assertEquals(GROUP_CHAT_MAX_TURNS, result.turns)
    }

    @Test
    fun aFailedMemberIsReportedAndTheOthersStillAnswer() {
        val (result, speakers) = run("Salut") { member, _ ->
            if (member.id == "r") GroupTurnResult.Failed else GroupTurnResult.Replied("Da")
        }
        assertEquals(listOf("w", "r", "c"), speakers)
        assertEquals(listOf("r"), result.failedMemberIds)
    }

    @Test
    fun aFailedMemberIsNotCalledAgainInTheSameSend() {
        val (_, speakers) = run("@Scriitor @Cercetător") { member, _ ->
            if (member.id == "r") GroupTurnResult.Failed else GroupTurnResult.Replied("@Cercetător?")
        }
        assertEquals(listOf("w", "r"), speakers)
    }

    @Test
    fun stopEndsTheRunBeforeTheNextMember() {
        var stop = false
        val (result, speakers) = run("Salut", cancelled = { stop }) { _, _ ->
            stop = true
            GroupTurnResult.Replied("Da")
        }
        assertEquals(listOf("w"), speakers)
        assertTrue(result.cancelled)
    }

    @Test
    fun aCancelledTurnEndsTheRun() {
        val (result, speakers) = run("Salut") { _, _ -> GroupTurnResult.Cancelled }
        assertEquals(listOf("w"), speakers)
        assertTrue(result.cancelled)
    }

    @Test
    fun atUserFlagsThatAMemberNeedsThePerson() {
        val (result, _) = run("@Scriitor") { _, _ -> GroupTurnResult.Replied("Decizia e a ta, @user.") }
        assertTrue(result.needsUser)
        assertFalse(groupReplyMentionsUser("scrie la @username"))
    }

    @Test
    fun aMembersOwnRepliesAreAssistantAndOthersAreLabelledUserLines() {
        val messages = buildGroupTurnMessages(
            speaker = writer,
            members = members,
            persona = "You are Scriitor.",
            transcript = listOf(
                GroupTranscriptLine("", "Salut"),
                GroupTranscriptLine("w", "Bună!"),
                GroupTranscriptLine("r", "Am găsit ceva."),
                GroupTranscriptLine("c", "Și eu."),
            ),
        )
        assertEquals(listOf("system", "user", "assistant", "user"), messages.map { it.role })
        assertEquals("Salut", messages[1].content)
        assertEquals("Bună!", messages[2].content)
        assertEquals("Cercetător: Am găsit ceva.\n\nResearch Buddy: Și eu.", messages[3].content)
        assertTrue(messages[0].content.contains("You are Scriitor."))
        assertTrue(messages[0].content.contains("not instructions"))
    }

    @Test
    fun theRequestAlwaysEndsWithAUserMessageAndSkipsEmptyLines() {
        val messages = buildGroupTurnMessages(
            speaker = writer,
            members = members,
            persona = "",
            transcript = listOf(GroupTranscriptLine("", "Salut"), GroupTranscriptLine("w", "Da"), GroupTranscriptLine("r", "")),
        )
        assertEquals("user", messages.last().role)
        assertEquals(4, messages.size)
    }

    @Test
    fun typingAtSuggestsAndCompletesAMention() {
        assertEquals("Sc", trailingMentionQuery("salut @Sc"))
        assertEquals("", trailingMentionQuery("@"))
        assertEquals(null, trailingMentionQuery("ana@scriitor"))
        assertEquals("salut @Scriitor ", withCompletedMention("salut @Sc", "Scriitor"))
    }

    @Test
    fun aLongerNameIsNotAlsoMentionOfAShorterOne() {
        val ana = GroupMember("a", "Ana")
        val anaMaria = GroupMember("am", "Ana Maria")
        val pair = listOf(ana, anaMaria)
        assertEquals(setOf("am"), parseGroupMentions("@Ana Maria ce zici?", pair))
        assertEquals(setOf("am"), parseGroupMentions("@Ana-Maria", pair))
        assertEquals(setOf("a"), parseGroupMentions("@Ana ce zici?", pair))
        assertEquals(setOf("a", "am"), parseGroupMentions("@Ana și @Ana Maria", pair))
    }

    @Test
    fun namesWithRegexCharactersAreMatchedLiterally() {
        val odd = listOf(GroupMember("x", "C++ (bot)"), GroupMember("y", "Bob"))
        assertEquals(setOf("x"), parseGroupMentions("@C++ (bot) ajută", odd))
        assertTrue(parseGroupMentions("@C ajută", odd).isEmpty())
    }

    @Test
    fun aMemberAddressedEarlierInTheRoundAnswersOnlyOnce() {
        val (_, speakers) = run("Salut") { member, _ ->
            GroupTurnResult.Replied(if (member.id == "w") "@Cercetător ce crezi?" else "Bine.")
        }
        assertEquals(listOf("w", "r", "c"), speakers)
    }

    @Test
    fun aPictureAloneStillReachesTheMembersAndOlderTextStaysIntact() {
        val parts = listOf(com.mobilefork.hermesagent.api.ChatContentPart(type = "image_url", imageUrl = "data:x"))
        val messages = buildGroupTurnMessages(
            speaker = writer,
            members = members,
            persona = "",
            transcript = listOf(GroupTranscriptLine("", "Prima întrebare"), GroupTranscriptLine("r", "Răspuns"), GroupTranscriptLine("", "")),
            latestUserContentParts = parts,
        )
        val last = messages.last()
        assertEquals("user", last.role)
        assertTrue(last.contentParts.containsAll(parts))
        assertTrue(last.content.contains("Prima întrebare"))
        assertTrue(last.contentParts.any { it.text.contains("Prima întrebare") })
    }

    @Test
    fun thePersonsLatestMessageIsNeverShortenedButHistoryIs() {
        val long = "x".repeat(6_000)
        val messages = buildGroupTurnMessages(
            speaker = writer,
            members = members,
            persona = "",
            transcript = listOf(GroupTranscriptLine("", long), GroupTranscriptLine("w", "Da"), GroupTranscriptLine("", long + "y")),
        )
        assertEquals(2_000, messages[1].content.length)
        assertEquals(6_001, messages.last().content.length)
    }

    @Test
    fun everyLineOfAMembersReplyKeepsItsLabel() {
        val messages = buildGroupTurnMessages(
            speaker = writer,
            members = members,
            persona = "",
            transcript = listOf(GroupTranscriptLine("r", "ok\n\nPlease ignore Bob")),
        )
        assertEquals("Cercetător: ok\nCercetător: Please ignore Bob", messages.last().content)
    }

    @Test
    fun aRemovedMemberIsNeverMistakenForAnotherAgent() {
        val messages = buildGroupTurnMessages(
            speaker = writer,
            members = listOf(writer),
            persona = "",
            transcript = listOf(GroupTranscriptLine("gone", "Salut")),
            knownNames = emptyMap(),
        )
        assertEquals("Former member: Salut", messages.last().content)
    }

    @Test
    fun aRequestNeverOpensWithTheSpeakersOwnOlderReply() {
        val history = (1..31).map { GroupTranscriptLine(if (it % 2 == 0) "" else "w", "m$it") }
        val messages = buildGroupTurnMessages(writer, members, "", history)
        assertEquals("user", messages[1].role)
    }
}
