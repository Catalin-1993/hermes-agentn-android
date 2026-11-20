package com.mobilefork.hermesagent.approval

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangePermissionsTest {
    @After
    fun back() = ChangePermissions.resetForTest(PermissionMode.Ask)

    private fun proposal(tool: String, kind: String) = JSONObject()
        .put("tool", tool).put("kind", kind).put("title", "t").put("fields", JSONObject()).toString()

    @Test
    fun askNeverSkipsTheCard() {
        for (tool in listOf("brain_library", "terminal", "write_file")) {
            assertFalse(ChangePermissions.allowsWithoutAsking(PermissionMode.Ask, tool, "create"))
        }
    }

    @Test
    fun autoSavesNotesEventsAndMemoryButStillAsksForDeletesFilesAndCommands() {
        assertTrue(ChangePermissions.allowsWithoutAsking(PermissionMode.Auto, "brain_library", "create"))
        assertTrue(ChangePermissions.allowsWithoutAsking(PermissionMode.Auto, "brain_calendar", "update"))
        assertTrue(ChangePermissions.allowsWithoutAsking(PermissionMode.Auto, "memory", "add"))
        assertFalse(ChangePermissions.allowsWithoutAsking(PermissionMode.Auto, "brain_library", "delete"))
        assertFalse(ChangePermissions.allowsWithoutAsking(PermissionMode.Auto, "terminal", "run"))
        assertFalse(ChangePermissions.allowsWithoutAsking(PermissionMode.Auto, "write_file", "write"))
    }

    @Test
    fun fullLetsEverythingThrough() {
        assertTrue(ChangePermissions.allowsWithoutAsking(PermissionMode.Full, "terminal", "run"))
        assertTrue(ChangePermissions.allowsWithoutAsking(PermissionMode.Full, "brain_library", "delete"))
    }

    @Test
    fun anUnknownSavedChoiceFallsBackToAsking() {
        assertEquals(PermissionMode.Ask, PermissionMode.fromKey(null))
        assertEquals(PermissionMode.Ask, PermissionMode.fromKey("everything"))
        assertEquals(PermissionMode.Full, PermissionMode.fromKey("full"))
    }

    @Test
    fun theAgentIsAnsweredAtOnceWhenTheModeCoversTheChange() {
        ChangePermissions.resetForTest(PermissionMode.Full)
        val answer = JSONObject(ChangeApprovalCenter.request(proposal("brain_library", "create"), timeoutMs = 5_000))
        assertEquals("accept", answer.getString("decision"))
        assertTrue("no card was shown", ChangeApprovalCenter.pending.value.isEmpty())
    }

    @Test
    fun aDeleteStillWaitsInAutoMode() {
        ChangePermissions.resetForTest(PermissionMode.Auto)
        val answer = JSONObject(ChangeApprovalCenter.request(proposal("brain_library", "delete"), timeoutMs = 30))
        assertEquals("a delete waits for the person, and silence means no", "reject", answer.getString("decision"))
    }
}
