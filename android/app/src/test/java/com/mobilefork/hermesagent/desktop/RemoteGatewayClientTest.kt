package com.mobilefork.hermesagent.desktop

import java.io.IOException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RemoteGatewayClientTest {
    private fun sse(vararg frames: Pair<String, String>): String =
        frames.joinToString("") { (name, data) -> "event: $name\ndata: $data\n\n" }

    @Test
    fun theDesktopsConversationsAreListedWithTheirTitles() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    """{"object":"list","data":[
                        {"id":"s1","title":"Release notes","preview":"Draft the…","last_active":1760000000.5,"message_count":4},
                        {"title":"no id, skipped"}
                    ]}""",
                ),
            )
            server.start()
            val sessions = RemoteGatewayClient(server.url("/").toString(), "k").listSessions()
            assertEquals(listOf(RemoteSession("s1", "Release notes", "Draft the…", 1760000000.5, 4)), sessions)
            assertEquals("Bearer k", server.takeRequest().getHeader("Authorization"))
        }
    }

    @Test
    fun onlyWhatWasSaidIsReadFromAConversation() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    """{"data":[
                        {"role":"user","content":"hello"},
                        {"role":"tool","content":"hidden"},
                        {"role":"assistant","content":[{"type":"text","text":"Hi "},{"type":"text","text":"there"}]},
                        {"role":"assistant","content":""}
                    ]}""",
                ),
            )
            server.start()
            val messages = RemoteGatewayClient(server.url("/").toString(), "").messages("s 1")
            assertEquals(listOf(RemoteMessage("user", "hello"), RemoteMessage("assistant", "Hi there")), messages)
            assertEquals("/api/sessions/s%201/messages", server.takeRequest().path)
        }
    }

    @Test
    fun aTurnIsReportedInTheOrderTheDesktopDoesIt() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    ": keepalive\n\n" + sse(
                        "run.started" to """{"run_id":"run_1"}""",
                        "assistant.delta" to """{"delta":"Hel"}""",
                        "tool.started" to """{"tool_name":"terminal","preview":"ls"}""",
                        "tool.completed" to """{"tool_name":"terminal"}""",
                        "approval.request" to """{"run_id":"run_1","command":"rm x","description":"delete","choices":["once","deny"]}""",
                        "assistant.completed" to """{"content":"Hello"}""",
                        "run.completed" to """{}""",
                        "done" to """{}""",
                    ),
                ),
            )
            server.start()
            val events = mutableListOf<RemoteEvent>()
            RemoteGatewayClient(server.url("/").toString(), "k").chat("s1", "hi") { events += it }
            assertEquals(
                listOf(
                    RemoteEvent.RunStarted("run_1"),
                    RemoteEvent.Delta("Hel"),
                    RemoteEvent.ToolStarted("terminal", "ls"),
                    RemoteEvent.ToolFinished("terminal", failed = false),
                    RemoteEvent.ApprovalRequest("run_1", "rm x", "delete", listOf("once", "deny")),
                    RemoteEvent.Completed("Hello"),
                    RemoteEvent.RunEnded("completed"),
                    RemoteEvent.Done,
                ),
                events,
            )
            val request = server.takeRequest()
            assertEquals("/api/sessions/s1/chat/stream", request.path)
            assertTrue(request.body.readUtf8().contains("\"message\":\"hi\""))
        }
    }

    @Test
    fun theDesktopsAnswersToApprovalAndStopAreSent() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("{}"))
            server.enqueue(MockResponse().setBody("{}"))
            server.start()
            val client = RemoteGatewayClient(server.url("/").toString(), "k")
            client.answerApproval("run_1", "once")
            client.stop("run_1")
            val approval = server.takeRequest()
            assertEquals("/v1/runs/run_1/approval", approval.path)
            assertTrue(approval.body.readUtf8().contains("\"choice\":\"once\""))
            assertEquals("/v1/runs/run_1/stop", server.takeRequest().path)
        }
    }

    @Test
    fun aRefusedKeyIsExplainedNotShownAsACode() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401))
            server.start()
            try {
                RemoteGatewayClient(server.url("/").toString(), "bad").listSessions()
                fail("expected a refusal")
            } catch (error: IOException) {
                assertEquals("The desktop did not accept the key.", error.message)
            }
        }
    }

    @Test
    fun framesThePhoneDoesNotKnowAreIgnored() {
        assertEquals(null, RemoteEventParser.parse("message.started", "{}"))
        assertEquals(null, RemoteEventParser.parse("assistant.delta", "{\"delta\":\"\"}"))
        assertEquals(RemoteEvent.Failed("boom"), RemoteEventParser.parse("error", "{\"message\":\"boom\"}"))
    }
}
