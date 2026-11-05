package com.mobilefork.hermesagent.ui.chat

import android.app.Application
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.data.AgentStore
import com.mobilefork.hermesagent.data.AppSettings
import com.mobilefork.hermesagent.data.AppSettingsStore
import com.mobilefork.hermesagent.data.ConversationStore
import com.mobilefork.hermesagent.privacy.RemoteProcessingConsentStore
import com.mobilefork.hermesagent.privacy.RemoteProcessingTarget
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList

/** Whole turns through the real view model and HTTP client, against a local fake provider. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class GroupChatRoundTripRobotest {
    private val server = MockWebServer()
    private val requests = CopyOnWriteArrayList<JSONObject>()
    private lateinit var app: Application
    private lateinit var writer: AgentProfile
    private lateinit var researcher: AgentProfile
    private lateinit var viewModel: ChatViewModel
    private var replies: (String, JSONObject) -> MockResponse = { _, _ -> sse("(pass)") }

    private fun sse(vararg chunks: String): MockResponse {
        val body = chunks.joinToString("") { chunk ->
            "data: " + JSONObject().put(
                "choices",
                org.json.JSONArray().put(JSONObject().put("delta", JSONObject().put("content", chunk))),
            ) + "\n\n"
        } + "data: [DONE]\n\n"
        return MockResponse().setHeader("Content-Type", "text/event-stream").setBody(body)
    }

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        listOf("hermes_android_agents", "hermes_android_conversation").forEach { name ->
            app.getSharedPreferences(name, android.content.Context.MODE_PRIVATE).edit().clear().commit()
        }
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val body = JSONObject(request.body.readUtf8())
                requests += body
                val system = body.getJSONArray("messages").getJSONObject(0).getString("content")
                val speaker = Regex("""You are (\S+) in a group chat""").find(system)?.groupValues?.get(1).orEmpty()
                return replies(speaker, body)
            }
        }
        server.start()
        val settings = AppSettingsStore(app).update { AppSettings(provider = "openrouter", baseUrl = "https://example.test/v1", model = "m") }
        RemoteProcessingConsentStore(app).accept(requireNotNull(RemoteProcessingTarget.fromSettings(settings)))
        val agents = AgentStore(app)
        writer = agents.save(AgentProfile(id = "", name = "Scriitor"))
        researcher = agents.save(AgentProfile(id = "", name = "Cercetator"))
        viewModel = ChatViewModel(app)
        viewModel.groupEndpointForTest = { server.url("/v1").toString() }
        viewModel.createGroup("Echipa", listOf(writer.id, researcher.id), "")
    }

    @After
    fun tearDown() {
        viewModel.stopCurrentTask()
        // A response deliberately held open by the Stop test can outlast the server's own timeout.
        runCatching { server.shutdown() }
    }

    private fun send(text: String) {
        viewModel.updateInput(text)
        viewModel.sendMessage()
        val deadline = System.currentTimeMillis() + 15_000
        while (viewModel.uiState.value.isSending && System.currentTimeMillis() < deadline) Thread.sleep(20)
        assertFalse("The run should have finished", viewModel.uiState.value.isSending)
    }

    private fun stored() = ConversationStore(app).currentConversation().messages

    @Test
    fun membersAnswerInTurnAndEachReplyKeepsItsAuthor() {
        replies = { speaker, _ -> sse("Răspuns ", "de la $speaker") }
        send("Salut")
        val messages = stored()
        assertEquals(listOf("user", "assistant", "assistant"), messages.map { it.role })
        assertEquals(writer.id, messages[1].senderId)
        assertEquals("agent", messages[1].senderKind)
        assertEquals("Răspuns de la Scriitor", messages[1].content)
        assertEquals(researcher.id, messages[2].senderId)
        assertEquals("", viewModel.uiState.value.activeSpeakerId)
    }

    @Test
    fun theLaterMemberSeesTheEarlierReplyLabelledAndNoRequestOffersTools() {
        replies = { speaker, _ -> sse("Eu sunt $speaker") }
        send("Salut")
        val second = requests[1].getJSONArray("messages").toString()
        assertTrue(second, second.contains("Scriitor: Eu sunt Scriitor"))
        requests.forEach { assertFalse(it.toString(), it.has("tools")) }
    }

    @Test
    fun aPassLeavesNoMessageBehindAndNothingFlashesOnScreen() {
        replies = { speaker, _ -> if (speaker == "Scriitor") sse("(pa", "ss)") else sse("Am un răspuns") }
        send("Salut")
        val messages = stored()
        assertEquals(2, messages.size)
        assertEquals(researcher.id, messages[1].senderId)
        assertEquals(2, viewModel.uiState.value.messages.size)
    }

    @Test
    fun whenEveryonePassesThePersonIsTold() {
        replies = { _, _ -> sse("(pass)") }
        send("Salut")
        assertEquals(1, stored().size)
        assertTrue(viewModel.uiState.value.status.isNotBlank())
    }

    @Test
    fun aMentionInAReplyGivesTheOtherMemberAnotherTurn() {
        replies = { speaker, body ->
            val text = body.getJSONArray("messages").toString()
            when {
                speaker == "Scriitor" && !text.contains("Gata") -> sse("Ce crezi, @Cercetator?")
                else -> sse("Gata")
            }
        }
        send("@Scriitor începe")
        assertEquals(listOf(writer.id, researcher.id), stored().drop(1).map { it.senderId })
    }

    @Test
    fun aFailingMemberIsNamedAndTheOtherStillAnswers() {
        replies = { speaker, _ ->
            if (speaker == "Scriitor") MockResponse().setResponseCode(500).setBody("boom") else sse("Sunt aici")
        }
        send("Salut")
        val messages = stored()
        assertEquals(2, messages.size)
        assertEquals(researcher.id, messages[1].senderId)
        assertTrue(viewModel.uiState.value.error.contains("Scriitor"))
    }

    @Test
    fun stopEndsTheCurrentAnswerAndNoOtherMemberIsCalled() {
        replies = { _, _ -> sse("Încep").setBodyDelay(30, java.util.concurrent.TimeUnit.SECONDS) }
        viewModel.updateInput("Salut")
        viewModel.sendMessage()
        val deadline = System.currentTimeMillis() + 10_000
        while (requests.isEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(20)
        viewModel.stopCurrentTask()
        assertFalse(viewModel.uiState.value.isSending)
        Thread.sleep(400)
        assertEquals(1, requests.size)
    }

    @Test
    fun clearingAGroupEmptiesItButKeepsTheGroup() {
        replies = { _, _ -> sse("Da") }
        send("Salut")
        val id = viewModel.uiState.value.activeConversationId
        viewModel.clearCurrentConversation()
        val conversation = ConversationStore(app).currentConversation()
        assertEquals(id, conversation.sessionId)
        assertTrue(conversation.isGroup && conversation.messages.isEmpty())
    }
}
