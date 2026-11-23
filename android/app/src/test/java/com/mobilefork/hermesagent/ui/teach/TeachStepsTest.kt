package com.mobilefork.hermesagent.ui.teach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeachStepsTest {
    @Test
    fun aClickAndATypedValueAreRead() {
        assertEquals(
            TeachStep.Click("Search", "#go"),
            parseTeachEvent("""{"type":"click","selector":"#go","text":"  Search  "}"""),
        )
        assertEquals(
            TeachStep.Type("Email", "input[name=email]", "me@example.com"),
            parseTeachEvent("""{"type":"input","selector":"input[name=email]","text":"Email","value":"me@example.com"}"""),
        )
    }

    @Test
    fun anythingElseIsIgnored() {
        assertNull(parseTeachEvent("not json"))
        assertNull(parseTeachEvent("""{"type":"scroll"}"""))
        assertNull(parseTeachEvent("""{"type":"click"}"""))
    }

    @Test
    fun repeatedPageLoadsAndRetypedFieldsCountOnce() {
        val steps = emptyList<TeachStep>()
            .plusStep(TeachStep.Open("https://a.test"))
            .plusStep(TeachStep.Open("https://a.test"))
            .plusStep(TeachStep.Type("q", "#q", "he"))
            .plusStep(TeachStep.Type("q", "#q", "hello"))
        assertEquals(listOf(TeachStep.Open("https://a.test"), TeachStep.Type("q", "#q", "hello")), steps)
    }

    @Test
    fun aRecordingNeverGrowsWithoutLimit() {
        var steps = emptyList<TeachStep>()
        repeat(200) { steps = steps.plusStep(TeachStep.Click("b$it", "#b$it")) }
        assertEquals(80, steps.size)
    }

    @Test
    fun thePromptListsTheStepsAndAsksForASkill() {
        val prompt = buildTeachPrompt(listOf(TeachStep.Open("https://a.test"), TeachStep.Click("Search", "#go")))
        assertTrue(prompt.contains("1. Open https://a.test"))
        assertTrue(prompt.contains("2. Click \"Search\" (selector: #go)"))
        assertTrue(prompt.contains("skill_manage"))
    }
}
