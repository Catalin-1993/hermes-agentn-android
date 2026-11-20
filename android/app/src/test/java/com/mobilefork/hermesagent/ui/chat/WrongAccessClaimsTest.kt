package com.mobilefork.hermesagent.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WrongAccessClaimsTest {
    @Test
    fun theRefusalThatStuckIsRecognised() {
        val reply = "Îmi cer scuze, dar în această sesiune nu am acces la uneltele librăriei, Brain Library. " +
            "Nu apare în lista de instrumente disponibile acum, deci nu pot crea pagini în librărie direct."
        assertTrue(WrongAccessClaims.denies(reply))
        assertEquals(WrongAccessClaims.CORRECTION, WrongAccessClaims.forModel(reply))
    }

    @Test
    fun englishRefusalsAreRecognisedToo() {
        assertTrue(WrongAccessClaims.denies("Sorry, I don't have access to the Library tools in this session."))
        assertTrue(WrongAccessClaims.denies("The calendar tools aren't available right now."))
    }

    @Test
    fun anAnswerThatDoneTheWorkIsLeftAlone() {
        val done = "Am salvat nota în Library, cu titlul Grafice."
        assertFalse(WrongAccessClaims.denies(done))
        assertEquals(done, WrongAccessClaims.forModel(done))
        assertFalse(WrongAccessClaims.denies("I can't reach the weather service."))
        assertFalse(WrongAccessClaims.denies(""))
    }

    @Test
    fun onlyTheAnswerHalfOfAMemoryLineCanBeARefusal() {
        val bad = "Conversation 1: user asked: salvează în librărie | assistant answered: Nu am acces la uneltele librăriei."
        val good = "Conversation 2: user asked: nu am acces la wifi, librăria e lentă | assistant answered: Am salvat nota."
        assertTrue(WrongAccessClaims.memoryLineDenies(bad))
        assertFalse(WrongAccessClaims.memoryLineDenies(good))
    }

    @Test
    fun aRefusalIsNeverMadeIntoAMemory() {
        assertEquals("", conversationMemoryFact("s1", "salvează în librărie", "Nu am acces la uneltele librăriei."))
        assertTrue(conversationMemoryFact("s1", "salvează în librărie", "Gata, am salvat.").isNotBlank())
    }
}
