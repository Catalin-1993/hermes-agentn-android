package com.mobilefork.hermesagent.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OnboardingTest {
    private val answers = OnboardingAnswers(
        userName = "Cătălin",
        tone = AgentTone.Concise,
        purposes = setOf(AgentPurpose.Research, AgentPurpose.Coding),
    )

    @Test
    fun thePersonaCarriesEveryAnswerAndKeepsTheAgentAGeneralist() {
        val persona = onboardingPersona("Nova", answers)
        assertTrue(persona.contains("Nova"))
        assertTrue(persona.contains("Cătălin"))
        assertTrue(persona.contains("short"))
        assertTrue(persona.contains("coding, research"))
        assertTrue(persona.contains("help with anything"))
        assertTrue(persona.length <= AgentProfile.MAX_PERSONA_CHARS)
        assertEquals("", onboardingPersona(AgentProfile.Main.name, OnboardingAnswers()))
    }

    @Test
    fun theGuideNeverOverwritesInstructionsThePersonWrote() {
        assertEquals("new", personaAfterOnboarding(current = "", lastGenerated = "", generated = "new"))
        assertEquals("new", personaAfterOnboarding(current = "old guide", lastGenerated = "old guide", generated = "new"))
        assertEquals("mine", personaAfterOnboarding(current = "mine", lastGenerated = "old guide", generated = "new"))
    }

    @Test
    fun answersAreKeptForTheNextTimeTheGuideOpens() {
        val store = OnboardingStore(RuntimeEnvironment.getApplication())
        assertFalse(store.isDone())
        store.finish(answers, "generated")
        assertTrue(store.isDone())
        assertEquals(answers, store.answers())
        assertEquals("generated", store.lastGeneratedPersona())
        // A run under tests never opens the guide by itself.
        assertFalse(store.shouldOpenOnFirstLaunch())
    }

    @Test
    fun theMainAgentCanBeRenamedButNotToATakenName() {
        val store = AgentStore(RuntimeEnvironment.getApplication())
        store.save(AgentProfile(id = "", name = "Mira"))
        store.saveMainName("Nova")
        assertEquals("Nova", store.main().name)
        assertEquals("Nova", store.get(AgentProfile.MAIN_ID).name)
        assertTrue(runCatching { store.saveMainName("mira") }.isFailure)
        assertEquals("Nova", store.main().name)
    }
}
