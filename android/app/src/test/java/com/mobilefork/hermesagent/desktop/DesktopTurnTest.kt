package com.mobilefork.hermesagent.desktop

import java.util.concurrent.ConcurrentLinkedQueue
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopTurnTest {
    private class Recorder : DesktopTurnListener {
        val log = mutableListOf<String>()
        override fun onDelta(text: String) { log += "delta:$text" }
        override fun onToolStarted(name: String, preview: String) { log += "tool:$name" }
        override fun onToolFinished(name: String, failed: Boolean) { log += "toolEnd:$name:$failed" }
        override fun onCompleted(content: String) { log += "done:$content" }
        override fun onFailed(message: String) { log += "failed:$message" }
    }

    private fun frame(name: String, data: String) = "event: $name\ndata: $data\n\n"

    private fun waitFor(condition: () -> Boolean) {
        val until = System.currentTimeMillis() + 3000
        while (!condition() && System.currentTimeMillis() < until) Thread.sleep(20)
    }

    private fun desktop(seen: ConcurrentLinkedQueue<String>, stream: String, sessionKnown: Boolean) = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            seen += "${request.method} ${request.path}"
            return when {
                request.method == "GET" && request.path == "/api/sessions/chat-1" ->
                    if (sessionKnown) MockResponse().setBody("""{"id":"chat-1"}""") else MockResponse().setResponseCode(404)
                request.method == "POST" && request.path == "/api/sessions" -> MockResponse().setBody("""{"id":"chat-1"}""")
                request.path == "/api/sessions/chat-1/chat/stream" -> MockResponse().setBody(stream)
                request.path?.endsWith("/approval") == true -> MockResponse().setBody("{}")
                else -> MockResponse().setResponseCode(404)
            }
        }
    }

    @Test
    fun aMissingConversationIsCreatedUnderThePhonesOwnIdThenAnswered() {
        val seen = ConcurrentLinkedQueue<String>()
        MockWebServer().use { server ->
            server.dispatcher = desktop(
                seen,
                frame("run.started", """{"run_id":"run_1"}""") +
                    frame("assistant.delta", """{"delta":"Hi"}""") +
                    frame("tool.started", """{"tool_name":"web","preview":"q"}""") +
                    frame("tool.completed", """{"tool_name":"web"}""") +
                    frame("assistant.completed", """{"content":"Hi there"}""") +
                    frame("done", "{}"),
                sessionKnown = false,
            )
            server.start()
            val listener = Recorder()
            DesktopTurn(RemoteGatewayClient(server.url("/").toString(), "k")).run("chat-1", "hello", listener)
            assertEquals(listOf("delta:Hi", "tool:web", "toolEnd:web:false", "done:Hi there"), listener.log)
            assertTrue("POST /api/sessions" in seen)
        }
    }

    @Test
    fun aCommandTheDesktopWantsToRunIsAnsweredByThePerson() {
        val seen = ConcurrentLinkedQueue<String>()
        MockWebServer().use { server ->
            server.dispatcher = desktop(
                seen,
                frame("run.started", """{"run_id":"run_1"}""") +
                    frame("approval.request", """{"run_id":"run_1","command":"rm -rf x","description":"d","choices":["once","deny"]}""") +
                    frame("assistant.completed", """{"content":"ok"}""") +
                    frame("done", "{}"),
                sessionKnown = true,
            )
            server.start()
            var asked = ""
            val listener = Recorder()
            DesktopTurn(RemoteGatewayClient(server.url("/").toString(), "k"), askPerson = { command, _ -> asked = command; false })
                .run("chat-1", "clean up", listener)
            waitFor { "POST /v1/runs/run_1/approval" in seen }
            assertEquals("rm -rf x", asked)
            assertTrue("POST /v1/runs/run_1/approval" in seen)
            assertEquals(listOf("done:ok"), listener.log)
        }
    }

    @Test
    fun aTurnThatEndsWithoutAnAnswerIsReportedNotLeftHanging() {
        val seen = ConcurrentLinkedQueue<String>()
        MockWebServer().use { server ->
            server.dispatcher = desktop(seen, frame("error", """{"message":"model unavailable"}""") + frame("done", "{}"), true)
            server.start()
            val listener = Recorder()
            DesktopTurn(RemoteGatewayClient(server.url("/").toString(), "k")).run("chat-1", "hi", listener)
            assertEquals(listOf("failed:model unavailable"), listener.log)
        }
    }

    @Test
    fun aDesktopThatIsOffIsReportedInPlainWords() {
        val listener = Recorder()
        DesktopTurn(RemoteGatewayClient("http://127.0.0.1:1", "k")).run("chat-1", "hi", listener)
        assertEquals(1, listener.log.size)
        assertTrue(listener.log[0].startsWith("failed:"))
    }
}
