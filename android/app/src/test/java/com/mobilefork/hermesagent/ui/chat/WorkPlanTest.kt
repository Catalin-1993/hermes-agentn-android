package com.mobilefork.hermesagent.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkPlanTest {
    private val answer = """{"todos":[
        {"id":"1","content":"Read the code","status":"completed"},
        {"id":"2","content":"Write the fix","status":"in_progress"},
        {"id":"3","content":"Run the tests","status":"pending"},
        {"id":"4","content":"Old idea","status":"cancelled"}],"revision":3}"""

    @Test
    fun theAgentsListBecomesAPlanWithProgress() {
        val plan = parsePlan(answer)!!
        assertEquals(4, plan.items.size)
        assertEquals(3, plan.total)
        assertEquals(1, plan.done)
        assertEquals("Write the fix", plan.current?.text)
        assertEquals(1f / 3f, plan.fraction, 0.001f)
    }

    @Test
    fun textThatIsNotAPlanGivesNone() {
        assertNull(parsePlan("not json"))
        assertNull(parsePlan("""{"todos":[]}"""))
        assertNull(parsePlan("""{"other":1}"""))
    }

    @Test
    fun theNewestAnswerWinsAndTheToolIsNotListedAsActivity() {
        val older = ChatWorkStep("a", "todo_list", """{"todos":[{"id":"1","content":"One","status":"pending"}]}""", running = false)
        val newer = ChatWorkStep("b", "todo_list", "{}", running = false, result = answer)
        assertEquals("Write the fix", workPlanOf(listOf(older, newer))?.current?.text)
        assertTrue(isPlanStep(newer))
        assertTrue(!isPlanStep(ChatWorkStep("c", "terminal", "ls", running = false)))
    }
}
