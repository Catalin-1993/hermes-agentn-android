package com.mobilefork.hermesagent.desktop

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopPairingTest {
    @Test
    fun aJsonCodeCarriesTheAddressTheKeyAndTheName() {
        val pairing = DesktopPairingCode.parse(
            """{"url":"https://mac.tailnet.ts.net/","key":"abc123","name":"Studio Mac"}""",
        )
        assertEquals(DesktopPairing("https://mac.tailnet.ts.net", "abc123", "Studio Mac"), pairing)
    }

    @Test
    fun aPairLinkIsReadAsAnAddressWithAKey() {
        val pairing = DesktopPairingCode.parse("hermes-pair://mac.local:8642?key=k%201&name=Work&tls=0")
        assertEquals("http://mac.local:8642", pairing?.url)
        assertEquals("k 1", pairing?.key)
        assertEquals("Work", pairing?.name)
    }

    @Test
    fun aBareAddressIsEnough() {
        assertEquals("https://mac.tailnet.ts.net", DesktopPairingCode.parse("  mac.tailnet.ts.net ")?.url)
    }

    @Test
    fun nothingUsefulIsNotAPairing() {
        assertNull(DesktopPairingCode.parse(""))
        assertNull(DesktopPairingCode.parse("{not json"))
        assertNull(DesktopPairingCode.parse("""{"key":"only a key"}"""))
        assertNull(DesktopPairingCode.parse("two words"))
    }

    @Test
    fun theOfferIsMadeOnceAndOnlyToADesktopThatAnswers() {
        val link = DesktopLink("https://mac", "Mac")
        val reachable = DesktopProbeResult.Reachable("api_server")
        assertTrue(DesktopOfferPolicy.shouldAsk(link, reachable, DesktopOfferAnswer.Ask, false))
        assertFalse(DesktopOfferPolicy.shouldAsk(link, reachable, DesktopOfferAnswer.Ask, true))
        assertFalse(DesktopOfferPolicy.shouldAsk(link, reachable, DesktopOfferAnswer.StayLocal, false))
        assertFalse(DesktopOfferPolicy.shouldAsk(link, DesktopProbeResult.NeedsKey, DesktopOfferAnswer.Ask, false))
        assertFalse(DesktopOfferPolicy.shouldAsk(link, DesktopProbeResult.Unreachable("off"), DesktopOfferAnswer.Ask, false))
        assertFalse(DesktopOfferPolicy.shouldAsk(null, reachable, DesktopOfferAnswer.Ask, false))
    }

    @Test
    fun theProbeSeesAGatewayThatAcceptsTheKey() {
        MockWebServer().use { server ->
            server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
                override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest): MockResponse = when (request.path) {
                    "/health" -> MockResponse().setBody("""{"status":"ok","platform":"hermes-agent"}""")
                    "/v1/models" ->
                        if (request.getHeader("Authorization") == "Bearer good") {
                            MockResponse().setBody("""{"data":[]}""")
                        } else {
                            MockResponse().setResponseCode(401)
                        }
                    else -> MockResponse().setResponseCode(404)
                }
            }
            server.start()
            val url = server.url("/").toString()
            val ok = DesktopProbe().probe(url, "good")
            assertEquals(DesktopProbeResult.Reachable("hermes-agent"), ok)
            assertEquals(DesktopProbeResult.NeedsKey, DesktopProbe().probe(url, "wrong"))
        }
    }

    @Test
    fun theProbeDoesNotMistakeAnotherServerForTheDesktop() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("<html>router login</html>"))
            server.start()
            val result = DesktopProbe().probe(server.url("/").toString())
            assertTrue(result is DesktopProbeResult.Unreachable)
        }
    }

    @Test
    fun aDesktopThatIsOffIsAnAnswerNotACrash() {
        val result = DesktopProbe().probe("http://127.0.0.1:1")
        assertNotNull(result)
        assertTrue(result is DesktopProbeResult.Unreachable)
    }
}
