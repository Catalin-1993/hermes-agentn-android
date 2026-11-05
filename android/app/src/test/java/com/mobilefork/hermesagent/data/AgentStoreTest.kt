package com.mobilefork.hermesagent.data

import android.app.Application
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AgentStoreTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()

    @Test
    fun theMainAgentAlwaysComesFirstAndCannotBeOverwritten() {
        val store = AgentStore(app)
        assertEquals(AgentProfile.MAIN_ID, store.list().first().id)
        store.save(AgentProfile(id = "", name = "Researcher"))
        assertEquals(listOf(AgentProfile.MAIN_ID, "Researcher"), store.list().map { if (it.isMain) it.id else it.name })
        assertThrows(IllegalArgumentException::class.java) { store.save(AgentProfile.Main) }
    }

    @Test
    fun namesAreUniqueBecauseMentionsAddressAgentsByName() {
        val store = AgentStore(app)
        val first = store.save(AgentProfile(id = "", name = "Researcher"))
        assertThrows(IllegalArgumentException::class.java) { store.save(AgentProfile(id = "", name = " researcher ")) }
        assertThrows(IllegalArgumentException::class.java) { store.save(AgentProfile(id = "", name = "agent")) }
        listOf("All", "toți", "User").forEach { reserved ->
            assertFalse(reserved, store.isNameAvailable(reserved))
        }
        assertTrue(store.isNameAvailable("Researcher", excludingId = first.id))
        assertFalse(store.isNameAvailable("Researcher"))
    }

    @Test
    fun aDeletedAgentsConversationsFallBackToTheMainAgent() {
        val store = AgentStore(app)
        val agent = store.save(AgentProfile(id = "", name = "Researcher"))
        store.delete(agent.id)
        assertTrue(store.get(agent.id).isMain)
    }

    @Test
    fun aPinnedProviderNeverKeepsDeviceToolsAndDropsTheOnDeviceBackend() {
        val saved = AgentStore(app).save(
            AgentProfile(id = "", name = "Writer", provider = "OpenRouter", model = "m", toolsEnabled = true),
        )
        assertFalse(saved.usesDeviceTools)
        assertEquals("openrouter", saved.provider)
        val effective = saved.effectiveSettings(AppSettings(provider = "openai", onDeviceBackend = "llama_cpp"))
        assertEquals("openrouter", effective.provider)
        assertEquals("m", effective.model)
        assertEquals("none", effective.onDeviceBackend)
    }

    @Test
    fun onlyAnExplicitSwitchGivesANewAgentDeviceTools() {
        val store = AgentStore(app)
        assertFalse(store.save(AgentProfile(id = "", name = "Plain")).usesDeviceTools)
        assertTrue(store.save(AgentProfile(id = "", name = "Helper", toolsEnabled = true)).usesDeviceTools)
    }

    @Test
    fun theMainAgentRunsWithTheAppSettingsUntouched() {
        val global = AppSettings(provider = "openai", customSystemPrompt = "Be brief", onDeviceBackend = "llama_cpp")
        assertSame(global, AgentProfile.Main.effectiveSettings(global))
    }

    @Test
    fun anAgentsIdentityReachesItsSystemPrompt() {
        val agent = AgentProfile(id = "x", name = "Writer", title = "Editor", persona = "Prefer short sentences.")
        val prompt = agent.effectiveSettings(AppSettings(customSystemPrompt = "Main persona")).customSystemPrompt
        assertTrue(prompt.contains("Writer") && prompt.contains("Editor") && prompt.contains("Prefer short sentences."))
        assertFalse(prompt.contains("Main persona"))
    }

    @Test
    fun conversationsSavedBeforeAgentsExistedBelongToTheMainAgent() {
        val legacy = JSONArray().put(
            JSONObject()
                .put("sessionId", "old")
                .put("title", "Old chat")
                .put("updatedAtEpochMs", 5L)
                .put(
                    "messages",
                    JSONArray().put(
                        JSONObject().put("id", "m").put("role", "assistant").put("content", "hi").put("createdAtEpochMs", 5L),
                    ),
                ),
        )
        val prefs = app.getSharedPreferences("hermes_android_conversation", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("conversations_json", legacy.toString()).commit()
        val conversation = ConversationStore(app).loadConversation("old")
        assertEquals(AgentProfile.MAIN_ID, conversation?.primaryAgentId)
        assertEquals("", conversation?.messages?.single()?.senderId)
    }

    @Test
    fun aConversationRemembersItsAgentAndEachMessageItsAuthor() {
        val store = ConversationStore(app)
        val conversation = store.createNewConversation(agentIds = listOf("a1"))
        store.upsertMessage(
            conversation.sessionId,
            StoredConversationMessage("m", "assistant", "hello", 1L, senderKind = "agent", senderId = "a1"),
        )
        val reloaded = ConversationStore(app).loadConversation(conversation.sessionId)
        assertEquals("a1", reloaded?.primaryAgentId)
        assertEquals("a1", reloaded?.messages?.single()?.senderId)
        assertEquals(conversation.sessionId, store.latestConversationForAgent("a1")?.sessionId)
    }

    @Test
    fun clearingAConversationNeverLandsInAnotherAgentsConversation() {
        val store = ConversationStore(app)
        store.createNewConversation(agentIds = listOf("other"))
        val mine = store.createNewConversation(agentIds = listOf("mine"))
        val next = store.clearConversation(mine.sessionId)
        assertEquals("mine", next.primaryAgentId)
        assertTrue(next.messages.isEmpty())
    }
}
