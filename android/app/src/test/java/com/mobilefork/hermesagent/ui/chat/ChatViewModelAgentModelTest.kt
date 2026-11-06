package com.mobilefork.hermesagent.ui.chat

import android.app.Application
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.data.AgentStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The model picker in the chat's "+" sheet changes one agent without touching the others. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ChatViewModelAgentModelTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()

    @Test
    fun anAgentGetsItsOwnModelAndDropsDeviceTools() {
        val store = AgentStore(app)
        val agent = store.save(AgentProfile(id = "", name = "Mira", toolsEnabled = true))
        val viewModel = ChatViewModel(app)

        assertTrue(viewModel.setAgentModel(agent.id, "openai", "gpt-4.1"))

        val saved = store.get(agent.id)
        assertEquals("openai", saved.provider)
        assertEquals("gpt-4.1", saved.model)
        assertFalse(saved.usesDeviceTools)
    }

    @Test
    fun blankValuesGoBackToTheMainModel() {
        val store = AgentStore(app)
        val agent = store.save(AgentProfile(id = "", name = "Noor", provider = "openai", model = "gpt-4.1"))
        val viewModel = ChatViewModel(app)

        assertTrue(viewModel.setAgentModel(agent.id, "", ""))

        assertEquals("", store.get(agent.id).provider)
        assertEquals("", store.get(agent.id).model)
    }

    @Test
    fun theMainAgentIsNotChangedThisWay() {
        assertFalse(ChatViewModel(app).setAgentModel(AgentProfile.MAIN_ID, "openai", "gpt-4.1"))
    }
}
