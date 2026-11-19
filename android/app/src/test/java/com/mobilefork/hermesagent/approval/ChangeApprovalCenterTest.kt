package com.mobilefork.hermesagent.approval

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ChangeApprovalCenterTest {
    private val proposal = JSONObject()
        .put("tool", "brain_calendar")
        .put("kind", "create")
        .put("title", "Add a calendar event")
        .put("fields", JSONObject().put("action", "create").put("title", "Dentist").put("content", "long"))
        .put("editable", org.json.JSONArray(listOf("title", "content")))
        .put("truncated", org.json.JSONArray(listOf("content")))
        .toString()

    private fun answerFirst(decide: (PendingChange) -> Unit): JSONObject {
        val pool = Executors.newSingleThreadExecutor()
        val reply = pool.submit<String> { ChangeApprovalCenter.request(proposal, timeoutMs = 5_000) }
        val deadline = System.currentTimeMillis() + 5_000
        while (ChangeApprovalCenter.pending.value.isEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(5)
        decide(ChangeApprovalCenter.pending.value.first())
        val answer = JSONObject(reply.get(5, TimeUnit.SECONDS))
        pool.shutdown()
        return answer
    }

    @Test
    fun theAgentWaitsAndGetsOnlyTheFieldsThePersonChanged() {
        val answer = answerFirst { change ->
            ChangeApprovalCenter.accept(change.id, mapOf("title" to "Dentist 10:00", "content" to "cut", "action" to "delete"))
        }
        assertEquals("accept", answer.getString("decision"))
        val edited = answer.getJSONObject("arguments")
        assertEquals("Dentist 10:00", edited.getString("title"))
        assertFalse("a shortened field is never sent back", edited.has("content"))
        assertFalse("a field the card does not offer is never sent back", edited.has("action"))
        assertTrue(ChangeApprovalCenter.pending.value.isEmpty())
    }

    @Test
    fun rejectAndSilenceBothMeanNoChange() {
        assertEquals("reject", answerFirst { ChangeApprovalCenter.reject(it.id) }.getString("decision"))
        val silent = JSONObject(ChangeApprovalCenter.request(proposal, timeoutMs = 20))
        assertEquals("reject", silent.getString("decision"))
        assertTrue(ChangeApprovalCenter.pending.value.isEmpty())
    }
}
