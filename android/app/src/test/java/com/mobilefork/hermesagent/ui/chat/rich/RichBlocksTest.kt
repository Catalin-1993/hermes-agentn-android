package com.mobilefork.hermesagent.ui.chat.rich

import com.mobilefork.hermesagent.ui.chat.AgentEventType
import com.mobilefork.hermesagent.ui.chat.ChatUiMessage
import com.mobilefork.hermesagent.ui.chat.chatEdgeAlpha
import com.mobilefork.hermesagent.ui.chat.workStepsOfLastTurn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.mobilefork.hermesagent.ui.chat.chatEdgeVeilStops
import com.mobilefork.hermesagent.ui.theme.HermesGlassTokens
import com.mobilefork.hermesagent.ui.theme.hermesGlassTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class RichBlocksTest {
    @Test
    fun textAroundABlockStaysInOrder() {
        val segments = parseRichSegments("Before\n```widget\n<div>x</div>\n```\nAfter")
        assertEquals(RichSegment.Text("Before"), segments[0])
        assertEquals(RichSegment.Block(RichLanguage.Widget, "<div>x</div>", closed = true), segments[1])
        assertEquals(RichSegment.Text("After"), segments[2])
    }

    @Test
    fun aBlockStillBeingWrittenIsNotClosed() {
        val block = parseRichSegments("```html\n<div>half").last() as RichSegment.Block
        assertFalse(block.closed)
    }

    @Test
    fun otherCodeStaysTextAndADrawableFenceQuotedInsideItIsNotDrawn() {
        val content = "````markdown\n```widget\n<div>x</div>\n```\n````"
        assertTrue(parseRichSegments(content).all { it is RichSegment.Text })
        assertTrue(parseRichSegments("```python\nprint(1)\n```").single() is RichSegment.Text)
    }

    @Test
    fun aReplyThatIsOnlyAPageIsDrawn() {
        val block = parseRichSegments("<!doctype html><html><body>hi</body></html>").single() as RichSegment.Block
        assertEquals(RichLanguage.Html, block.language)
        assertTrue(parseRichSegments("Use <html> tags like this").single() is RichSegment.Text)
    }

    @Test
    fun voiceAndPreviewsSkipTheBlock() {
        assertEquals("Before\n\nAfter", textWithoutRichBlocks("Before\n```chart\n{}\n```\nAfter"))
    }

    @Test
    fun theFrameOnlyReachesListedHostsOverHttps() {
        val hosts = WIDGET_CDN_HOSTS
        assertTrue(WidgetDocument.isRequestAllowed("https://cdn.jsdelivr.net/npm/x@1/x.js", hosts))
        assertFalse(WidgetDocument.isRequestAllowed("http://cdn.jsdelivr.net/x.js", hosts))
        assertFalse(WidgetDocument.isRequestAllowed("https://cdn.jsdelivr.net.evil.example/x.js", hosts))
        assertFalse(WidgetDocument.isRequestAllowed("https://evil.example/?u=cdn.jsdelivr.net", hosts))
        assertFalse(WidgetDocument.isRequestAllowed("https://user@evil.example/", hosts))
        assertFalse(WidgetDocument.isRequestAllowed("https://cdn.jsdelivr.net/x.js", emptySet()))
    }

    @Test
    fun aPolicyComesBeforeAnythingTheWidgetWrote() {
        val theme = WidgetTheme(false, "#000000", "#111111", "#222222", "#333333", "#444444", "#555555", "#666666")
        val document = WidgetDocument.forHtml("<!doctype html><html><body><script>x()</script></body></html>", theme, emptySet())
        val policy = document.indexOf("Content-Security-Policy")
        assertTrue(policy in 0 until document.indexOf("<script>x()"))
        assertTrue(document.startsWith("<!doctype html>"))
        assertTrue(document.contains("connect-src 'none'"))
        assertTrue(document.indexOf(WIDGET_BRIDGE_NAME) < document.indexOf("</body>"))
    }

    @Test
    fun mermaidSourceCannotBreakOutOfItsContainer() {
        val theme = WidgetTheme(false, "#000000", "#111111", "#222222", "#333333", "#444444", "#555555", "#666666")
        val document = WidgetDocument.forMermaid("graph TD;A-->B;</div><script>x()</script>", theme, WIDGET_CDN_HOSTS)
        assertFalse(document.contains("<script>x()"))
    }

    @Test
    fun aWidgetSpeaksOnlyRightAfterATouchAndAtHumanSpeed() {
        val now = 100_000L
        assertEquals("no_touch", WidgetIntentGate.evaluate("go", now, 0L, 0L))
        assertEquals("no_touch", WidgetIntentGate.evaluate("go", now, now - 60_000L, 0L))
        assertEquals("throttled", WidgetIntentGate.evaluate("go", now, now - 10L, now - 200L))
        assertEquals("too_long", WidgetIntentGate.evaluate("x".repeat(501), now, now - 10L, 0L))
        assertEquals("invalid", WidgetIntentGate.evaluate("  ", now, now - 10L, 0L))
        assertNull(WidgetIntentGate.evaluate("go", now, now - 10L, 0L))
    }

    @Test
    fun chartsAcceptWhatModelsDriftToAndRefuseWhatCannotBeDrawn() {
        val loose = parseChartSpec("""{"type":"bar","labels":["a","b"],"data":[1,2]}""") as ChartParse.Ok
        assertEquals(listOf("a", "b"), loose.spec.categories)
        assertEquals(listOf(1.0, 2.0), loose.spec.series.single().values)
        assertTrue(parseChartSpec("""{"type":"hologram"}""") is ChartParse.Refused)
        assertTrue(parseChartSpec("not json") is ChartParse.Refused)
        assertTrue(parseChartSpec("""{"type":"line","series":[{"data":["x"]}]}""") is ChartParse.Refused)
        assertTrue(parseChartSpec("""{"type":"pie","slices":[{"name":"a","value":-1}]}""") is ChartParse.Refused)
    }

    @Test
    fun everyChartKindParses() {
        listOf(
            """{"type":"area","categories":["a"],"series":[{"name":"s","data":[1]}]}""",
            """{"type":"radar","categories":["a","b","c"],"series":[{"name":"s","data":[1,2,3]}]}""",
            """{"type":"scatter","series":[{"name":"s","data":[[1,2]]}]}""",
            """{"type":"donut","slices":[{"name":"a","value":1}]}""",
            """{"type":"funnel","slices":[{"name":"a","value":3},{"name":"b","value":1}]}""",
            """{"type":"kpi","kpis":[{"label":"a","value":1,"delta":2.5,"trend":[1,2]}]}""",
            """{"type":"table","columns":["a","b"],"rows":[[1,"x"]]}""",
            """{"type":"gauge","value":40}""",
        ).forEach { source -> assertNotNull(source, parseChartSpec(source) as? ChartParse.Ok) }
    }

    @Test
    fun valuesReadAsPeopleWriteThem() {
        assertEquals("12.5%", formatChartValue(12.5, ChartNumberStyle(ChartFormat.Percent), Locale.US))
        assertEquals("1.5K", formatChartValue(1500.0, ChartNumberStyle(ChartFormat.Compact), Locale.US))
        assertEquals("€42", formatChartValue(42.0, ChartNumberStyle(ChartFormat.Currency, "EUR"), Locale.US))
        assertEquals("3 ms", formatChartValue(3.0, ChartNumberStyle(unit = " ms"), Locale.US))
    }

    @Test
    fun axisMarksCoverTheDataAndStartAtZero() {
        val ticks = niceAxisTicks(12.0, 87.0)
        assertEquals(0.0, ticks.first(), 0.0)
        assertTrue(ticks.last() >= 87.0)
        assertTrue(niceAxisTicks(-5.0, 5.0).first() <= -5.0)
        assertTrue(niceAxisTicks(0.0, 0.0).size >= 2)
    }

    @Test
    fun theEdgesFadeAlongOneCurveAndTheReadingAreaStaysClear() {
        val density = 3f
        val header = 300f
        val composerTop = 1800f
        val composerBottom = 1960f
        fun alpha(y: Float) = chartAlpha(y, header, composerTop, composerBottom, density)
        assertEquals(1f, alpha(1000f), 0.0001f)
        assertEquals(0f, alpha(0f), 0.0001f)
        assertEquals(0f, alpha(2400f), 0.0001f)
        // Faint but readable right under the composer; never rising as it nears the screen's edge.
        assertTrue(alpha(composerBottom) in 0.1f..0.4f)
        var previous = 1f
        for (y in 1000..2400 step 10) {
            val value = alpha(y.toFloat())
            assertTrue(value <= previous + 0.0001f)
            previous = value
        }
        previous = 1f
        for (y in 1000 downTo 0 step 10) {
            val value = alpha(y.toFloat())
            assertTrue(value <= previous + 0.0001f)
            previous = value
        }
    }

    @Test
    fun theFadeIsPaintedOnlyInTheThemesOwnColours() {
        val accent = Color(0xFF0066D6)
        val light = hermesGlassTokens(accent, Color(0xFF0A9B3C), Color.White, Color.White, Color(0xFFEBEBED), Color(0xFF111111), lightCanvas = true)
        val dark = hermesGlassTokens(accent, Color(0xFF0A9B3C), Color(0xFF0E0F12), Color(0xFF1C1D21), Color(0xFF2A2B30), Color.White, lightCanvas = false)
        val stopsOf = { tokens: HermesGlassTokens ->
            chatEdgeVeilStops(2400f, 300f, 1800f, 1960f, 3f, 80f, 2556f, tokens)
        }
        stopsOf(light).forEach { (_, color) -> assertTrue("a light theme must never fade to dark", color.luminance() > 0.8f) }
        stopsOf(dark).forEach { (_, color) -> assertTrue("a dark theme must never fade to light", color.luminance() < 0.1f) }
        // The veil is clear over the reading area and solid where the text is gone.
        val stops = stopsOf(light)
        assertEquals(0f, stops[stops.size / 2].second.alpha, 0.001f)
        assertEquals(1f, stops.first().second.alpha, 0.001f)
        assertEquals(1f, stops.last().second.alpha, 0.001f)
    }

    private fun chartAlpha(y: Float, header: Float, top: Float, bottom: Float, density: Float) =
        chatEdgeAlpha(y, header, top, bottom, density)

    private fun message(role: String, content: String, id: String = content) =
        ChatUiMessage(id = id, role = role, content = content, createdAtEpochMs = 0L)

    @Test
    fun aToolCallAndItsResultAreOneStepAndOnlyThisTurnCounts() {
        val call = AgentEventType.ToolCall.persistedRole
        val result = AgentEventType.ToolResult.persistedRole
        val steps = workStepsOfLastTurn(
            listOf(
                message("user", "old"),
                message(call, "old_tool\nargs", "old-call"),
                message("user", "new"),
                message(call, "web_search\n{\"q\":\"x\"}", "a"),
                message(result, "web_search result\nfound", "b"),
                message(call, "terminal_tool\nls", "c"),
                message("assistant", "writing…", "d"),
            ),
        )
        assertEquals(listOf("web_search", "terminal_tool"), steps.map { it.tool })
        assertEquals(listOf(false, true), steps.map { it.running })
    }
}
