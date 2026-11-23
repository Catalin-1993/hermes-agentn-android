package com.mobilefork.hermesagent.ui.chat

import java.io.IOException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GoalClientTest {
    @Test
    fun theGoalIsSetThenEachReplyIsJudged() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"status":"active","goal":"Write it","max_turns":12}"""))
            server.enqueue(
                MockResponse().setBody(
                    """{"should_continue":true,"continuation_prompt":"Keep going on: Write it","verdict":"continue","message":"Continuing (1/12)"}""",
                ),
            )
            server.enqueue(MockResponse().setBody("""{"should_continue":false,"continuation_prompt":null,"verdict":"done","message":"Goal achieved"}"""))
            server.start()
            val client = GoalClient(server.url("/").toString(), "key")
            client.set("chat 1", "Write it", 12)
            val first = client.evaluate("chat 1", "Drafted")
            val last = client.evaluate("chat 1", "Finished")

            assertTrue(first.shouldContinue)
            assertEquals("Keep going on: Write it", first.prompt)
            assertFalse(last.shouldContinue)
            assertEquals("", last.prompt)
            assertEquals("done", last.verdict)

            val setRequest = server.takeRequest()
            assertEquals("/api/goals/chat%201", setRequest.path)
            assertEquals("Bearer key", setRequest.getHeader("Authorization"))
            assertTrue(setRequest.body.readUtf8().contains("\"goal\":\"Write it\""))
        }
    }

    @Test
    fun aServerThatRefusesIsAnError_notASilentStop() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401))
            server.start()
            try {
                GoalClient(server.url("/").toString(), "bad").evaluate("s", "x")
                fail("expected an error")
            } catch (error: IOException) {
                assertTrue(error.message.orEmpty().contains("401"))
            }
        }
    }
}
