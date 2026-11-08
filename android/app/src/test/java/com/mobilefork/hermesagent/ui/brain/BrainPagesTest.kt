package com.mobilefork.hermesagent.ui.brain

import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.data.AgentStore
import com.mobilefork.hermesagent.ui.chat.ChatConversationSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class BrainPagesTest {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun theMenuListsChatsNewestFirstAndFilesThemUnderTheirProject() {
        val sessions = conversationsAsSessions(
            listOf(
                ChatConversationSummary(id = "c1", title = "Prima", preview = "", updatedLabel = "", messageCount = 3),
                ChatConversationSummary(id = "c2", title = "", preview = "Un avanpremieră", updatedLabel = "", messageCount = 1, agentId = "p-olivia"),
            ),
        )
        assertEquals(listOf("c1", "c2"), sessions.map { it.id })
        assertTrue("the list order stands in for time", sessions[0].updatedAt > sessions[1].updatedAt)
        assertEquals("Un avanpremieră", sessions[1].title)
        assertEquals(BrainViewModel.MAIN_WORKSPACE_ID, sessions[0].workspaceId)
        assertEquals("p-olivia", sessions[1].workspaceId)
    }

    @Test
    fun theCoreProjectIsTheMainAgentAndOthersBecomeAgentsWithTheirOwnId() {
        val workspaces = defaultBrainWorkspaces()
        assertTrue(workspaces.size >= 2)
        val core = workspaces.first { it.id == BrainViewModel.MAIN_WORKSPACE_ID }
        assertEquals(AgentProfile.MAIN_ID, agentForProject(context, core))

        val specialist = workspaces.first { it.id != BrainViewModel.MAIN_WORKSPACE_ID }
        val agentId = agentForProject(context, specialist)
        assertEquals(specialist.id, agentId)
        val agent = AgentStore(context).list().first { it.id == agentId }
        assertEquals(specialist.name, agent.name)
        assertTrue(agent.toolsEnabled)
        // A second time finds the same agent instead of failing on its own name.
        assertEquals(agentId, agentForProject(context, specialist))
    }

    @Test
    fun aProjectWhoseNameAnAgentAlreadyHasLeavesThatAgentAlone() {
        val specialist = defaultBrainWorkspaces().last { it.id != BrainViewModel.MAIN_WORKSPACE_ID }
        AgentStore(context).save(AgentProfile(id = "mine", name = specialist.name))
        assertNull(agentForProject(context, specialist))
    }

    @Test
    fun projectsSurviveRestartsAndAnUnreadableFileIsKeptAside() {
        val store = BrainWorkspaceStore(context)
        val file = File(context.filesDir, "brain/projects.bin")
        file.delete()
        assertNull(store.load())
        val edited = defaultBrainWorkspaces().let { list -> list.map { if (it.id == list.first().id) it.copy(purpose = "Schimbat de mine") else it } }
        store.save(edited)
        assertEquals("Schimbat de mine", store.load()!!.first().purpose)
        assertEquals(edited.size, store.load()!!.size)

        file.writeBytes(byteArrayOf(1, 2, 3))
        assertNull(store.load())
        assertTrue("an unreadable file is set aside, not overwritten", File(context.filesDir, "brain/projects.unreadable.bin").isFile)
        assertNotNull(defaultBrainWorkspaces().firstOrNull())
    }

    @Test
    fun everyDefaultProjectHasWhatTheProjectPagesNeed() {
        val workspaces = defaultBrainWorkspaces()
        assertEquals(workspaces.size, workspaces.map { it.id }.toSet().size)
        workspaces.forEach { ws ->
            assertTrue(ws.id, ws.name.isNotBlank())
            assertTrue(ws.id, ws.agentRole.isNotBlank())
        }
    }
}
