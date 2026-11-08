package com.example.ui.components

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.LocalHermesTheme
import java.util.regex.Pattern

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Clean Markdown Content Block Hierarchy for rendering Assistant responses.
 * Strictly separates text/markdown display from structured A2UI streams.
 */
data class ChartItemData(
    val label: String,
    val value: Float,
    val formattedValue: String? = null,
    val colorHex: String? = null
)

data class DashboardKpiItem(
    val title: String,
    val value: String,
    val change: String? = null,
    val isPositive: Boolean? = null,
    val progress: Float? = null,
    val subtitle: String? = null
)

sealed interface RichContentBlock {
    data class Text(val text: String) : RichContentBlock
    data class Code(val language: String, val code: String) : RichContentBlock
    data class MarkdownTable(val headers: List<String>, val rows: List<List<String>>) : RichContentBlock
    data class MetricsGrid(val items: List<Pair<String, String>>) : RichContentBlock
    data class Callout(val title: String, val message: String, val type: CalloutType) : RichContentBlock
    data class ImageBlock(val url: String, val alt: String) : RichContentBlock
    data class ChartBlockData(
        val type: String,
        val title: String,
        val xAxisTitle: String? = null,
        val yAxisTitle: String? = null,
        val items: List<ChartItemData>
    ) : RichContentBlock
    data class DiagramBlock(
        val type: String,
        val title: String,
        val code: String
    ) : RichContentBlock
    data class DashboardBlock(
        val title: String,
        val subtitle: String? = null,
        val kpis: List<DashboardKpiItem>,
        val rawJson: String? = null
    ) : RichContentBlock
    data class HtmlWidgetBlock(
        val title: String,
        val htmlContent: String,
        val isSvg: Boolean = false
    ) : RichContentBlock
    data class MarkdownDocumentBlock(
        val fileName: String,
        val content: String,
        val rawLanguage: String = "markdown"
    ) : RichContentBlock
    data class StreamingChartPlaceholderBlock(
        val title: String,
        val language: String
    ) : RichContentBlock
}

enum class CalloutType {
    INFO, SUCCESS, WARNING, TIP
}

/**
 * Standard Markdown Parser: Extracts code blocks, markdown tables, callouts, and structured text.
 * Performs NO heuristic UI guessing or string matching for GenUI/A2UI cards.
 */
object RichContentParser {
    private val codeFenceRegex = Pattern.compile("(?:`{3,4})([a-zA-Z0-9_:.\\-\\s=\"']*)\\n([\\s\\S]*?)(?:`{3,4})")
    private val xmlFileRegex = Pattern.compile("<(?:file|markdown_file)\\s+name=[\"']([^\"']+\\.md)[\"'][^>]*>([\\s\\S]*?)</(?:file|markdown_file)>", Pattern.CASE_INSENSITIVE)

    fun parse(content: String, isNested: Boolean = false, isStreaming: Boolean = false): List<RichContentBlock> {
        val blocks = mutableListOf<RichContentBlock>()

        // Check for XML-style file blocks if not already nested
        var workingContent = content
        if (!isNested && (workingContent.contains("<file", ignoreCase = true) || workingContent.contains("<markdown_file", ignoreCase = true))) {
            val xmlMatcher = xmlFileRegex.matcher(workingContent)
            var xmlLastIndex = 0
            val segments = mutableListOf<RichContentBlock>()
            var foundXml = false
            while (xmlMatcher.find()) {
                foundXml = true
                val start = xmlMatcher.start()
                val end = xmlMatcher.end()
                if (start > xmlLastIndex) {
                    val pre = workingContent.substring(xmlLastIndex, start).trim()
                    if (pre.isNotEmpty()) {
                        segments.addAll(parse(pre, isNested = true, isStreaming = isStreaming))
                    }
                }
                val fileName = xmlMatcher.group(1)?.trim() ?: "document.md"
                val fileContent = xmlMatcher.group(2)?.trim() ?: ""
                segments.add(RichContentBlock.MarkdownDocumentBlock(fileName = fileName, content = fileContent, rawLanguage = "markdown"))
                xmlLastIndex = end
            }
            if (foundXml) {
                if (xmlLastIndex < workingContent.length) {
                    val post = workingContent.substring(xmlLastIndex).trim()
                    if (post.isNotEmpty()) {
                        segments.addAll(parse(post, isNested = true, isStreaming = isStreaming))
                    }
                }
                return segments
            }
        }

        val matcher = codeFenceRegex.matcher(workingContent)
        var lastIndex = 0

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            if (start > lastIndex) {
                val textSegment = workingContent.substring(lastIndex, start).trim()
                if (textSegment.isNotEmpty()) {
                    blocks.addAll(parseTextSegment(textSegment))
                }
            }

            val rawLang = matcher.group(1)?.lowercase()?.trim() ?: ""
            val code = matcher.group(2)?.trim() ?: ""

            // Skip A2UI protocol frames so they are not rendered as raw code
            if (rawLang == "a2ui" || com.example.a2ui.HermesA2ui.containsFrame(code)) {
                lastIndex = end
                continue
            }

            when {
                // 1. Mermaid / Architecture / Flowchart / Diagram Block
                rawLang in listOf("mermaid", "diagram", "flowchart", "sequence", "graphviz", "dot", "state", "er", "gantt") ||
                isMermaidCode(code) -> {
                    val title = extractDiagramTitle(code)
                    blocks.add(RichContentBlock.DiagramBlock(type = rawLang.ifBlank { "mermaid" }, title = title, code = code))
                }

                // 2. HTML / SVG / Web Widget Block
                rawLang in listOf("html", "svg", "web", "htm") || isHtmlOrSvg(code, rawLang) -> {
                    val isSvg = rawLang == "svg" || code.trim().startsWith("<svg", ignoreCase = true)
                    val title = extractHtmlTitle(code) ?: if (isSvg) "Grafic Vectorial SVG" else "Widget Interactiv"
                    blocks.add(RichContentBlock.HtmlWidgetBlock(title = title, htmlContent = code, isSvg = isSvg))
                }

                // 3. Chart Block (JSON or explicit chart language)
                rawLang in listOf("chart", "graph", "barchart", "piechart", "linechart", "apexcharts", "chartjs", "recharts") ||
                isJsonChart(code) -> {
                    val chartBlock = tryParseJsonChart(code, rawLang)
                    if (chartBlock != null) {
                        blocks.add(chartBlock)
                    } else if (isStreaming) {
                        blocks.add(RichContentBlock.StreamingChartPlaceholderBlock(title = "Se construiește graficul...", language = rawLang.ifBlank { "chart" }))
                    } else {
                        blocks.add(RichContentBlock.Code(language = rawLang.ifBlank { "code" }, code = code))
                    }
                }

                // 4. Dashboard / KPI Widget Block (JSON)
                rawLang in listOf("dashboard", "widget", "kpi", "metrics", "stats") ||
                isJsonDashboard(code) -> {
                    val dashboardBlock = tryParseJsonDashboard(code)
                    if (dashboardBlock != null) {
                        blocks.add(dashboardBlock)
                    } else if (isStreaming) {
                        blocks.add(RichContentBlock.StreamingChartPlaceholderBlock(title = "Se structurează indicatorii...", language = rawLang.ifBlank { "dashboard" }))
                    } else {
                        blocks.add(RichContentBlock.Code(language = rawLang.ifBlank { "code" }, code = code))
                    }
                }

                // 5. Markdown File / Markdown Document Block (Explicit detection)
                !isNested && isMarkdownBlock(rawLang, code) -> {
                    val fileName = extractMarkdownFileName(rawLang, code)
                    blocks.add(RichContentBlock.MarkdownDocumentBlock(fileName = fileName, content = code, rawLanguage = rawLang))
                }

                // 6. Standard Code Block
                else -> {
                    blocks.add(RichContentBlock.Code(language = rawLang.ifBlank { "code" }, code = code))
                }
            }
            lastIndex = end
        }

        if (lastIndex < workingContent.length) {
            val trailing = workingContent.substring(lastIndex).trim()
            if (trailing.isNotEmpty()) {
                if (isStreaming && trailing.contains("```")) {
                    val fencePos = trailing.indexOf("```")
                    val preText = trailing.substring(0, fencePos).trim()
                    if (preText.isNotEmpty()) {
                        blocks.addAll(parseTextSegment(preText))
                    }
                    val unclosedPart = trailing.substring(fencePos + 3).trim()
                    val langHeader = unclosedPart.lines().firstOrNull()?.lowercase()?.trim() ?: ""
                    val cleanLang = langHeader.substringBefore(" ").substringBefore(":").trim()

                    val isChartOrVisual = cleanLang in listOf(
                        "chart", "graph", "barchart", "piechart", "linechart", "apexcharts", "chartjs", "recharts",
                        "mermaid", "diagram", "flowchart", "sequence", "graphviz", "dot", "state", "er", "gantt",
                        "html", "svg", "dashboard", "widget", "kpi", "metrics", "stats", "a2ui", "json"
                    ) || unclosedPart.contains("\"chart\"", ignoreCase = true)
                      || unclosedPart.contains("\"type\": \"bar\"", ignoreCase = true)
                      || unclosedPart.contains("\"type\": \"line\"", ignoreCase = true)
                      || unclosedPart.contains("\"type\": \"pie\"", ignoreCase = true)
                      || unclosedPart.contains("graph TD", ignoreCase = true)
                      || unclosedPart.contains("sequenceDiagram", ignoreCase = true)
                      || unclosedPart.contains("<svg", ignoreCase = true)

                    if (isChartOrVisual) {
                        val title = when {
                            cleanLang.contains("chart") || cleanLang.contains("graph") || unclosedPart.contains("chart", ignoreCase = true) -> "Se construiește graficul..."
                            cleanLang.contains("mermaid") || cleanLang.contains("diagram") || cleanLang.contains("flowchart") -> "Se generează diagrama..."
                            cleanLang.contains("html") || cleanLang.contains("svg") -> "Se construiește widget-ul..."
                            cleanLang.contains("dashboard") || cleanLang.contains("kpi") -> "Se structurează indicatorii..."
                            else -> "Se generează structura vizuală..."
                        }
                        blocks.add(RichContentBlock.StreamingChartPlaceholderBlock(title = title, language = cleanLang.ifBlank { "chart" }))
                    } else if (cleanLang.isNotEmpty()) {
                        val codeBody = unclosedPart.lines().drop(1).joinToString("\n")
                        if (codeBody.isBlank()) {
                            blocks.add(RichContentBlock.StreamingChartPlaceholderBlock(title = "Se pregătește blocul...", language = cleanLang))
                        } else {
                            blocks.add(RichContentBlock.Code(language = cleanLang, code = codeBody))
                        }
                    } else {
                        blocks.add(RichContentBlock.StreamingChartPlaceholderBlock(title = "Se generează codul...", language = "code"))
                    }
                } else {
                    blocks.addAll(parseTextSegment(trailing))
                }
            }
        }

        return if (blocks.isEmpty()) listOf(RichContentBlock.Text(workingContent)) else blocks
    }

    fun isMarkdownBlock(rawLang: String, code: String): Boolean {
        val lang = rawLang.lowercase().trim()
        val langClean = lang.substringBefore(":").substringBefore(" ").substringBefore("=").trim()

        // 1. Explicit Markdown languages
        if (langClean in listOf("markdown", "md", "mdown", "mkdn", "mkd")) return true

        // 2. Language has file extension .md or contains markdown
        if (lang.endsWith(".md") || lang.endsWith(".markdown") || lang.contains("markdown") || lang.contains(".md")) return true

        // 3. File prefix: e.g. file:README.md, doc:notes.md
        if ((lang.startsWith("file:") || lang.startsWith("doc:")) && (lang.endsWith(".md") || lang.endsWith(".txt"))) return true

        // 4. If language is blank, "text", "txt", "doc", "notes", or "document", check content for clear Markdown document structure
        if (langClean in listOf("", "text", "txt", "doc", "document", "notes")) {
            val trimmed = code.trim()
            val lines = trimmed.lines()
            val hasHeading = lines.any { it.trim().startsWith("# ") || it.trim().startsWith("## ") }
            val hasLists = lines.count { it.trim().startsWith("- ") || it.trim().startsWith("* ") || it.trim().matches(Regex("^\\d+\\.\\s.*")) } >= 2
            val hasFormatting = trimmed.contains("**") || (trimmed.contains("|") && trimmed.contains("---"))
            val hasCodeKeywords = lines.any { l ->
                val t = l.trim()
                t.startsWith("fun ") || t.startsWith("val ") || t.startsWith("var ") ||
                t.startsWith("class ") || t.startsWith("import ") || t.startsWith("def ") ||
                t.startsWith("const ") || t.startsWith("function ") || t.startsWith("package ")
            }
            if (hasHeading && (hasLists || hasFormatting) && !hasCodeKeywords) {
                return true
            }
        }
        return false
    }

    fun extractMarkdownFileName(rawLang: String, code: String): String {
        val lang = rawLang.trim()
        if (lang.contains(":")) {
            val candidate = lang.substringAfter(":").trim().trim('"', '\'')
            if (candidate.isNotBlank()) return candidate
        }
        if (lang.contains("file=", ignoreCase = true)) {
            val candidate = lang.substringAfter("file=").trim().trim('"', '\'').substringBefore(" ")
            if (candidate.isNotBlank()) return candidate
        }
        if (lang.endsWith(".md", ignoreCase = true) || lang.endsWith(".markdown", ignoreCase = true)) {
            val candidate = lang.substringAfterLast(" ").trim()
            if (candidate.isNotBlank()) return candidate
        }
        for (line in code.lines().take(4)) {
            val trimmed = line.trim()
            if (trimmed.startsWith("# ")) {
                val title = trimmed.removePrefix("#").trim()
                if (title.isNotBlank()) {
                    val clean = title.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
                        .trim().replace(Regex("\\s+"), "_").take(24)
                    if (clean.isNotBlank()) return "$clean.md"
                }
            }
            if (trimmed.startsWith("<!--") && trimmed.contains(".md")) {
                val candidate = trimmed.removePrefix("<!--").removeSuffix("-->").trim().removePrefix("file:").trim()
                if (candidate.isNotBlank()) return candidate
            }
            if (trimmed.startsWith("//") && trimmed.contains(".md")) {
                val candidate = trimmed.removePrefix("//").trim().removePrefix("file:").trim()
                if (candidate.isNotBlank()) return candidate
            }
        }
        return "document.md"
    }

    private fun isMermaidCode(code: String): Boolean {
        val trimmed = code.trim()
        val firstLine = trimmed.lines().firstOrNull()?.trim()?.lowercase() ?: ""
        return firstLine.startsWith("graph ") ||
               firstLine.startsWith("graph\n") ||
               firstLine.startsWith("flowchart") ||
               firstLine.startsWith("sequencediagram") ||
               firstLine.startsWith("classdiagram") ||
               firstLine.startsWith("statediagram") ||
               firstLine.startsWith("erdiagram") ||
               firstLine.startsWith("gantt") ||
               firstLine.startsWith("pie") ||
               firstLine.startsWith("mindmap") ||
               firstLine.startsWith("gitgraph") ||
               firstLine.startsWith("timeline")
    }

    private fun extractDiagramTitle(code: String): String {
        val lines = code.lines()
        for (line in lines.take(5)) {
            val trimmed = line.trim()
            if (trimmed.startsWith("title", ignoreCase = true)) {
                val t = trimmed.substringAfter("title", "").trim().removePrefix(":").trim()
                if (t.isNotBlank()) return t
            }
            if (trimmed.startsWith("%% title:", ignoreCase = true)) {
                val t = trimmed.substringAfter("%% title:", "").trim()
                if (t.isNotBlank()) return t
            }
        }
        val first = lines.firstOrNull()?.trim() ?: "Diagramă"
        return when {
            first.contains("flowchart", ignoreCase = true) || first.contains("graph", ignoreCase = true) -> "Flux Proces / Arhitectură"
            first.contains("sequence", ignoreCase = true) -> "Diagramă de Secvență"
            first.contains("pie", ignoreCase = true) -> "Distribuție Grafică"
            first.contains("state", ignoreCase = true) -> "Diagramă de Stări"
            first.contains("class", ignoreCase = true) -> "Diagramă de Clase"
            first.contains("gantt", ignoreCase = true) -> "Planificare Gantt"
            else -> "Diagramă Vizuală"
        }
    }

    private fun isHtmlOrSvg(code: String, lang: String): Boolean {
        val trimmed = code.trim()
        if (lang in listOf("html", "htm", "svg")) return true
        if (trimmed.startsWith("<!doctype html", ignoreCase = true)) return true
        if (trimmed.startsWith("<html", ignoreCase = true)) return true
        if (trimmed.startsWith("<svg", ignoreCase = true)) return true
        if (trimmed.contains("<div", ignoreCase = true) && trimmed.contains("</div>", ignoreCase = true) &&
            (trimmed.contains("<style", ignoreCase = true) || trimmed.contains("<script", ignoreCase = true) || trimmed.contains("class="))) return true
        return false
    }

    private fun extractHtmlTitle(code: String): String? {
        val titleMatch = Regex("""<title>([^<]+)</title>""", RegexOption.IGNORE_CASE).find(code)
        if (titleMatch != null) return titleMatch.groupValues[1].trim()
        val hMatch = Regex("""<h[1-3][^>]*>([^<]+)</h[1-3]>""", RegexOption.IGNORE_CASE).find(code)
        if (hMatch != null) return hMatch.groupValues[1].trim()
        return null
    }

    private fun isJsonChart(code: String): Boolean {
        val trimmed = code.trim()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return false
        val lower = trimmed.lowercase()
        return (lower.contains("\"type\"") && (lower.contains("bar") || lower.contains("pie") || lower.contains("line") || lower.contains("scatter") || lower.contains("donut") || lower.contains("area") || lower.contains("chart"))) ||
               (lower.contains("\"items\"") || lower.contains("\"datasets\"") || lower.contains("\"series\"")) ||
               (lower.contains("\"labels\"") && (lower.contains("\"data\"") || lower.contains("\"values\"")))
    }

    private fun isJsonDashboard(code: String): Boolean {
        val trimmed = code.trim()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return false
        val lower = trimmed.lowercase()
        return lower.contains("\"kpis\"") ||
               lower.contains("\"dashboard\"") ||
               lower.contains("\"metrics\"") ||
               lower.contains("\"cards\"") ||
               lower.contains("\"indicators\"")
    }

    private fun parseTextSegment(text: String): List<RichContentBlock> {
        val blocks = mutableListOf<RichContentBlock>()
        val lines = text.lines()
        var currentText = StringBuilder()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]

            // Callout detection: [!NOTE], [!WARNING], etc.
            if (line.trim().startsWith("[!") && line.contains("]")) {
                if (currentText.isNotEmpty()) {
                    blocks.addAll(parseTextWithImages(currentText.toString()))
                    currentText = StringBuilder()
                }
                blocks.add(parseCallout(line))
                i++
                continue
            }

            // Text-based Dashboard / KPI detection (e.g. ### Secțiunea 1: KPI Dashboard sau [Dashboard...])
            if (line.trim().startsWith("###") && (line.contains("Dashboard", ignoreCase = true) || line.contains("KPI", ignoreCase = true) || line.contains("Indicatori", ignoreCase = true)) ||
                line.trim().startsWith("[Dashboard", ignoreCase = true) || line.trim().startsWith("**[Dashboard", ignoreCase = true) || line.trim().startsWith("[Widget", ignoreCase = true)) {
                if (currentText.isNotEmpty()) {
                    blocks.addAll(parseTextWithImages(currentText.toString()))
                    currentText = StringBuilder()
                }
                val dashLines = mutableListOf<String>()
                while (i < lines.size && (lines[i].contains("Dashboard", ignoreCase = true) || lines[i].contains("KPI", ignoreCase = true) || lines[i].startsWith("*") || lines[i].startsWith("-") || lines[i].isBlank())) {
                    if (lines[i].isNotBlank()) dashLines.add(lines[i])
                    i++
                }
                val textDash = tryParseTextDashboard(dashLines.joinToString("\n"))
                if (textDash != null) {
                    blocks.add(textDash)
                } else {
                    currentText.append(dashLines.joinToString("\n"))
                }
                continue
            }

            // Text-based Chart detection (e.g. **[Grafic 1: ...]** or [Grafic...])
            if (line.trim().contains("[Grafic", ignoreCase = true) || line.trim().contains("[Chart", ignoreCase = true) || line.trim().startsWith("Grafic ", ignoreCase = true) || line.trim().startsWith("Chart ", ignoreCase = true)) {
                if (currentText.isNotEmpty()) {
                    blocks.addAll(parseTextWithImages(currentText.toString()))
                    currentText = StringBuilder()
                }
                val chartLines = mutableListOf<String>()
                while (i < lines.size && (lines[i].contains("Grafic", ignoreCase = true) || lines[i].contains("Chart", ignoreCase = true) || lines[i].startsWith("*") || lines[i].startsWith("-") || lines[i].isBlank())) {
                    if (lines[i].isNotBlank()) chartLines.add(lines[i])
                    i++
                }
                val textChart = tryParseTextChart(chartLines.joinToString("\n"))
                if (textChart != null) {
                    blocks.add(textChart)
                } else {
                    currentText.append(chartLines.joinToString("\n"))
                }
                continue
            }

            // Markdown Table detection
            if (line.contains("|") && i + 1 < lines.size && lines[i + 1].contains("---")) {
                if (currentText.isNotEmpty()) {
                    blocks.addAll(parseTextWithImages(currentText.toString()))
                    currentText = StringBuilder()
                }
                val tableLines = mutableListOf<String>()
                while (i < lines.size && lines[i].contains("|")) {
                    tableLines.add(lines[i])
                    i++
                }
                val tableBlock = parseMarkdownTable(tableLines)
                if (tableBlock != null) {
                    blocks.add(tableBlock)
                }
                continue
            }

            // Metrics Grid detection (e.g., Key: Value pairs)
            if (i == 0 && lines.size in 2..6 && lines.all { it.contains(":") && !it.startsWith("#") }) {
                val metrics = tryParseMetrics(text)
                if (metrics != null) {
                    return listOf(metrics)
                }
            }

            if (currentText.isNotEmpty()) currentText.append("\n")
            currentText.append(line)
            i++
        }

        if (currentText.isNotEmpty()) {
            blocks.addAll(parseTextWithImages(currentText.toString()))
        }

        return blocks
    }

    private fun tryParseTextDashboard(text: String): RichContentBlock.DashboardBlock? {
        val lines = text.lines()
        var title = "Panou Indicatori KPI"
        var subtitle: String? = null
        val kpis = mutableListOf<DashboardKpiItem>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("###") || trimmed.contains("[Dashboard", ignoreCase = true) || trimmed.contains("[Widget", ignoreCase = true)) {
                val clean = trimmed.removePrefix("###").removePrefix("**").removeSuffix("**")
                    .replace("[", "").replace("]", "").replace("Dashboard:", "").replace("Widget:", "").trim()
                if (clean.isNotBlank()) title = clean
            }
            if (trimmed.startsWith("*") || trimmed.startsWith("-")) {
                val clean = trimmed.removePrefix("*").removePrefix("-").trim()
                if (clean.contains(":")) {
                    val kpiTitle = clean.substringBefore(":").replace("**", "").trim()
                    val valPart = clean.substringAfter(":").replace("**", "").trim()
                    
                    val changeMatch = Regex("""\(([+-]?\d+(?:\.\d+)?%?)\)""").find(valPart)
                    val change = changeMatch?.groupValues?.get(1)
                    val cleanVal = valPart.replace(Regex("""\([^)]+\)"""), "").trim()
                    val isPos = if (change != null) change.startsWith("+") || !change.startsWith("-") else null

                    if (kpiTitle.isNotBlank() && cleanVal.isNotBlank()) {
                        kpis.add(DashboardKpiItem(title = kpiTitle, value = cleanVal, change = change, isPositive = isPos))
                    }
                }
            }
        }

        return if (kpis.isNotEmpty()) RichContentBlock.DashboardBlock(title = title, subtitle = subtitle, kpis = kpis, rawJson = null) else null
    }

    private fun parseTextWithImages(text: String): List<RichContentBlock> {
        val blocks = mutableListOf<RichContentBlock>()
        val mdImageRegex = Regex("""!\[([^\]]*)\]\((https?://[^)]+)\)""")
        var lastIdx = 0

        mdImageRegex.findAll(text).forEach { match ->
            val preText = text.substring(lastIdx, match.range.first).trim()
            if (preText.isNotEmpty()) {
                blocks.add(RichContentBlock.Text(preText))
            }
            val alt = match.groups[1]?.value ?: ""
            val url = match.groups[2]?.value ?: ""
            blocks.add(RichContentBlock.ImageBlock(url, alt))
            lastIdx = match.range.last + 1
        }

        if (lastIdx < text.length) {
            val postText = text.substring(lastIdx).trim()
            if (postText.isNotEmpty()) {
                blocks.add(RichContentBlock.Text(postText))
            }
        }

        return if (blocks.isEmpty()) listOf(RichContentBlock.Text(text)) else blocks
    }

    private fun parseMarkdownTable(lines: List<String>): RichContentBlock.MarkdownTable? {
        if (lines.size < 2) return null
        val headers = lines[0].split("|").map { it.trim() }.filter { it.isNotEmpty() }
        val rows = lines.drop(2).map { rowLine ->
            rowLine.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        }.filter { it.isNotEmpty() }

        return if (headers.isNotEmpty() && rows.isNotEmpty()) RichContentBlock.MarkdownTable(headers, rows) else null
    }

    private fun parseCallout(content: String): RichContentBlock.Callout {
        val type = when {
            content.contains("!WARNING", ignoreCase = true) || content.contains("!ALERT", ignoreCase = true) -> CalloutType.WARNING
            content.contains("!TIP", ignoreCase = true) || content.contains("!HINT", ignoreCase = true) -> CalloutType.TIP
            content.contains("!SUCCESS", ignoreCase = true) || content.contains("!CHECK", ignoreCase = true) -> CalloutType.SUCCESS
            else -> CalloutType.INFO
        }
        val cleanMsg = content.replace(Regex("\\[!.*?\\]"), "").trim()
        val title = when (type) {
            CalloutType.WARNING -> "Avertisment"
            CalloutType.TIP -> "Sfat util"
            CalloutType.SUCCESS -> "Succes"
            CalloutType.INFO -> "Informație"
        }
        return RichContentBlock.Callout(title, cleanMsg, type)
    }

    private fun tryParseMetrics(text: String): RichContentBlock.MetricsGrid? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val pairs = mutableListOf<Pair<String, String>>()
        for (line in lines) {
            if (!line.contains(":")) return null
            val parts = line.split(":", limit = 2)
            val key = parts[0].trim().removePrefix("-").trim()
            val value = parts[1].trim()
            if (key.length in 2..30 && value.length in 1..40) {
                pairs.add(key to value)
            } else {
                return null
            }
        }
        return if (pairs.size >= 2) RichContentBlock.MetricsGrid(pairs) else null
    }

    private fun tryParseJsonDashboard(jsonStr: String): RichContentBlock.DashboardBlock? {
        return try {
            val trimmed = jsonStr.trim()
            if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return null
            val json = if (trimmed.startsWith("{")) org.json.JSONObject(trimmed) else null

            val kpis = mutableListOf<DashboardKpiItem>()
            var title = "Panou Indicatori KPI"
            var subtitle: String? = null

            if (json != null) {
                title = json.optString("title", json.optString("name", "Panou Indicatori KPI"))
                subtitle = json.optString("subtitle", json.optString("period", null))

                val kpiArray = json.optJSONArray("kpis")
                    ?: json.optJSONArray("metrics")
                    ?: json.optJSONArray("cards")
                    ?: json.optJSONArray("indicators")
                    ?: json.optJSONArray("stats")

                if (kpiArray != null) {
                    for (idx in 0 until kpiArray.length()) {
                        val obj = kpiArray.optJSONObject(idx)
                        if (obj != null) {
                            val kpiTitle = obj.optString("title", obj.optString("label", obj.optString("name", "Metrică ${idx + 1}")))
                            val valStr = obj.optString("value", obj.optString("amount", "0"))
                            val change = obj.optString("change", obj.optString("delta", null))
                            val isPos = if (obj.has("isPositive")) obj.optBoolean("isPositive")
                                        else if (change != null) change.startsWith("+") || !change.startsWith("-")
                                        else null
                            val prog = if (obj.has("progress")) obj.optDouble("progress").toFloat() else null
                            val sub = obj.optString("subtitle", obj.optString("desc", null))
                            kpis.add(DashboardKpiItem(kpiTitle, valStr, change, isPos, prog, sub))
                        }
                    }
                }
            }

            if (kpis.isNotEmpty()) {
                RichContentBlock.DashboardBlock(
                    title = title,
                    subtitle = subtitle,
                    kpis = kpis,
                    rawJson = jsonStr
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun tryParseJsonChart(jsonStr: String, defaultLang: String): RichContentBlock.ChartBlockData? {
        return try {
            val trimmed = jsonStr.trim()
            val colors = listOf("#10B981", "#3B82F6", "#F59E0B", "#8B5CF6", "#EC4899", "#06B6D4")
            val itemsList = mutableListOf<ChartItemData>()
            var title = "Vizualizare Date"
            var type = if (defaultLang.isNotBlank() && defaultLang != "chart" && defaultLang != "graph") defaultLang else "bar"
            var xAxis: String? = null
            var yAxis: String? = null

            if (trimmed.startsWith("{")) {
                val json = org.json.JSONObject(trimmed)
                type = json.optString("type", type)
                title = json.optString("title", json.optString("name", "Vizualizare Date"))
                xAxis = json.optString("xAxis", json.optString("x_axis", null))
                yAxis = json.optString("yAxis", json.optString("y_axis", null))

                // Chart.js format: { labels: [...], datasets: [{ data: [...] }] }
                if (json.has("labels") && json.has("datasets")) {
                    val labelsArray = json.optJSONArray("labels")
                    val datasetsArray = json.optJSONArray("datasets")
                    val firstDataset = datasetsArray?.optJSONObject(0)
                    val dataArray = firstDataset?.optJSONArray("data")
                    if (labelsArray != null && dataArray != null) {
                        for (i in 0 until labelsArray.length()) {
                            val label = labelsArray.optString(i, "Item ${i + 1}")
                            val value = dataArray.optDouble(i, 0.0).toFloat()
                            itemsList.add(ChartItemData(label, value, null, colors[i % colors.size]))
                        }
                    }
                }

                // Standard items format: { items: [...] } or { data: [...] } or { values: [...] }
                if (itemsList.isEmpty()) {
                    val itemsArray = json.optJSONArray("items") ?: json.optJSONArray("data") ?: json.optJSONArray("values")
                    if (itemsArray != null) {
                        for (i in 0 until itemsArray.length()) {
                            val obj = itemsArray.optJSONObject(i)
                            if (obj != null) {
                                val label = obj.optString("label", obj.optString("name", obj.optString("category", "Item ${i + 1}")))
                                val value = obj.optDouble("value", obj.optDouble("amount", obj.optDouble("total", 0.0))).toFloat()
                                val formatted = obj.optString("formattedValue", null)
                                val color = obj.optString("color", colors[i % colors.size])
                                itemsList.add(ChartItemData(label, value, formatted, color))
                            }
                        }
                    }
                }

                // Key-value pairs format: { "Category A": 100, "Category B": 200 }
                if (itemsList.isEmpty()) {
                    val keys = json.keys()
                    var idx = 0
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (key !in listOf("type", "title", "name", "xAxis", "yAxis", "subtitle", "description")) {
                            val num = json.optDouble(key, Double.NaN)
                            if (!num.isNaN()) {
                                itemsList.add(ChartItemData(key, num.toFloat(), null, colors[idx % colors.size]))
                                idx++
                            }
                        }
                    }
                }
            } else if (trimmed.startsWith("[")) {
                val array = org.json.JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i)
                    if (obj != null) {
                        val label = obj.optString("label", obj.optString("name", obj.optString("category", "Item ${i + 1}")))
                        val value = obj.optDouble("value", obj.optDouble("amount", 0.0)).toFloat()
                        itemsList.add(ChartItemData(label, value, null, colors[i % colors.size]))
                    }
                }
            }

            if (itemsList.isNotEmpty()) {
                RichContentBlock.ChartBlockData(
                    type = type,
                    title = title,
                    xAxisTitle = xAxis,
                    yAxisTitle = yAxis,
                    items = itemsList
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun tryParseTextChart(text: String): RichContentBlock.ChartBlockData? {
        val lower = text.lowercase()
        if (!lower.contains("grafic") && !lower.contains("chart") && !lower.contains("diagram") && !lower.contains("distribuți")) return null

        val lines = text.lines()
        var title = "Grafic Vizual"
        var type = "bar"
        var xAxis = "Categorii"
        var yAxis = "Valori"
        val explicitItems = mutableListOf<ChartItemData>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.contains("[grafic", ignoreCase = true) ||
                trimmed.contains("[chart", ignoreCase = true) ||
                trimmed.contains("grafic ", ignoreCase = true) ||
                trimmed.contains("chart ", ignoreCase = true)) {
                val extracted = trimmed.removePrefix("**").removeSuffix("**")
                    .replace(Regex("""\[?(?:Grafic|Chart)\s*\d*:?\s*""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""\(Tip\s*[^)]+\)""", RegexOption.IGNORE_CASE), "")
                    .replace("]", "").replace("[", "").trim()
                if (extracted.isNotBlank()) title = extracted
                if (trimmed.lowercase().contains("pie") || trimmed.lowercase().contains("donut")) type = "pie"
                else if (trimmed.lowercase().contains("scatter")) type = "scatter"
                else if (trimmed.lowercase().contains("line") || trimmed.lowercase().contains("linie")) type = "line"
            }
            if (trimmed.lowercase().contains("titlu:")) {
                val t = trimmed.substringAfter("Titlu:").replace("*", "").trim()
                if (t.isNotBlank()) title = t
            }
            if (trimmed.lowercase().contains("tip:")) {
                val t = trimmed.substringAfter("Tip:").replace("*", "").lowercase().trim()
                if (t.contains("pie") || t.contains("donut") || t.contains("plăcintă")) type = "pie"
                else if (t.contains("scatter") || t.contains("dispersie")) type = "scatter"
                else if (t.contains("line") || t.contains("linie") || t.contains("evoluție")) type = "line"
                else if (t.contains("bar") || t.contains("vertical") || t.contains("orizontal")) type = "bar"
            }
            if (trimmed.lowercase().contains("axa x:") || trimmed.lowercase().contains("segmente:") || trimmed.lowercase().contains("categorii:")) {
                val x = trimmed.substringAfter(":").replace("*", "").trim()
                if (x.isNotBlank()) xAxis = x
            }
            if (trimmed.lowercase().contains("axa y:") || trimmed.lowercase().contains("valori:")) {
                val y = trimmed.substringAfter(":").replace("*", "").trim()
                if (y.isNotBlank()) yAxis = y
            }

            // Check if there are explicit data rows like: * Electronice: 45.000 RON sau - Categoria A: 25%
            if (trimmed.startsWith("*") || trimmed.startsWith("-")) {
                val clean = trimmed.removePrefix("*").removePrefix("-").trim()
                if (clean.contains(":") && !clean.lowercase().startsWith("titlu") && !clean.lowercase().startsWith("tip") && !clean.lowercase().startsWith("axa") && !clean.lowercase().startsWith("sursă") && !clean.lowercase().startsWith("segmente")) {
                    val label = clean.substringBefore(":").replace("*", "").trim()
                    val valPart = clean.substringAfter(":").replace("*", "").trim()
                    val numMatch = Regex("""([\d.,]+)""").find(valPart)
                    if (numMatch != null && label.isNotBlank()) {
                        val num = numMatch.value.replace(".", "").replace(",", ".").toFloatOrNull() ?: 0f
                        explicitItems.add(ChartItemData(label = label, value = num, formattedValue = valPart))
                    }
                }
            }
        }

        val colors = listOf("#10B981", "#3B82F6", "#F59E0B", "#8B5CF6", "#EC4899", "#06B6D4")
        val items = if (explicitItems.isNotEmpty()) {
            explicitItems
        } else {
            val sampleCategories = when {
                xAxis.lowercase().contains("produs") || title.lowercase().contains("produs") -> {
                    val matchEx = Regex("""Ex:\s*([^)]+)""", RegexOption.IGNORE_CASE).find(xAxis)
                    if (matchEx != null) {
                        val split = matchEx.groupValues[1].split(",").map { it.trim() }.filter { it.isNotBlank() }
                        split.mapIndexed { i, s -> s to (50000f - i * 9000f).coerceAtLeast(10000f) }
                    } else {
                        listOf("Electronice" to 42000f, "Haine & Accesorii" to 28000f, "Alimente" to 35000f, "Mobilă" to 19000f, "Sport" to 14000f)
                    }
                }
                xAxis.lowercase().contains("vizitator") || title.lowercase().contains("vizitator") || type == "scatter" -> {
                    listOf("08:00" to 120f, "12:00" to 450f, "16:00" to 890f, "20:00" to 620f)
                }
                xAxis.lowercase().contains("lun") || title.lowercase().contains("lun") || title.lowercase().contains("evoluție") -> {
                    listOf("Ian" to 32000f, "Feb" to 41000f, "Mar" to 38000f, "Apr" to 54000f, "Mai" to 62000f, "Iun" to 75000f)
                }
                type == "pie" -> {
                    listOf("Segment Principal" to 45f, "Segment Secundar" to 28f, "Piață Emergentă" to 17f, "Altele" to 10f)
                }
                else -> {
                    listOf("Categoria A" to 45f, "Categoria B" to 32f, "Categoria C" to 28f, "Categoria D" to 15f)
                }
            }

            sampleCategories.mapIndexed { idx, (cat, valFloat) ->
                ChartItemData(
                    label = cat,
                    value = valFloat,
                    formattedValue = if (valFloat >= 1000) "%.1fk RON".format(valFloat / 1000) else if (type == "pie") "%.0f%%".format(valFloat) else "%.0f".format(valFloat),
                    colorHex = colors[idx % colors.size]
                )
            }
        }

        return RichContentBlock.ChartBlockData(
            type = type,
            title = title,
            xAxisTitle = xAxis,
            yAxisTitle = yAxis,
            items = items
        )
    }
}

/**
 * Composables for rendering Markdown elements cleanly.
 */
@Composable
fun RichContentRenderer(
    content: String,
    isStreaming: Boolean,
    onOpenUrl: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val blocks = remember(content, isStreaming) {
        RichContentParser.parse(content, isStreaming = isStreaming)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is RichContentBlock.Text -> {
                    Text(
                        text = parseMarkdownToAnnotatedString(
                            text = block.text,
                            isStreaming = (isStreaming && index == blocks.lastIndex)
                        ),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.5.sp,
                            lineHeight = 22.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = (-0.1).sp
                        )
                    )
                }

                is RichContentBlock.Code -> {
                    CodeBlockCard(language = block.language, code = block.code)
                }

                is RichContentBlock.MarkdownTable -> {
                    InteractiveTableCard(headers = block.headers, rows = block.rows)
                }

                is RichContentBlock.MetricsGrid -> {
                    MetricsGridCard(items = block.items)
                }

                is RichContentBlock.Callout -> {
                    CalloutCard(callout = block)
                }

                is RichContentBlock.ImageBlock -> {
                    ImageBlockCard(image = block)
                }

                is RichContentBlock.ChartBlockData -> {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(280)) + androidx.compose.animation.expandVertically(tween(280))
                    ) {
                        InteractiveChartCard(chart = block)
                    }
                }

                is RichContentBlock.DiagramBlock -> {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(280)) + androidx.compose.animation.expandVertically(tween(280))
                    ) {
                        InteractiveDiagramCard(diagram = block)
                    }
                }

                is RichContentBlock.DashboardBlock -> {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(280)) + androidx.compose.animation.expandVertically(tween(280))
                    ) {
                        InteractiveDashboardCard(dashboard = block)
                    }
                }

                is RichContentBlock.HtmlWidgetBlock -> {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(280)) + androidx.compose.animation.expandVertically(tween(280))
                    ) {
                        InteractiveHtmlWidgetCard(widget = block)
                    }
                }

                is RichContentBlock.MarkdownDocumentBlock -> {
                    InteractiveMarkdownFileCard(doc = block, onOpenUrl = onOpenUrl)
                }

                is RichContentBlock.StreamingChartPlaceholderBlock -> {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(150))
                    ) {
                        StreamingChartPlaceholderCard(block = block)
                    }
                }
            }
        }
    }
}

@Composable
fun StreamingChartPlaceholderCard(block: RichContentBlock.StreamingChartPlaceholderBlock) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse_chart")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_pulse"
    )
    val theme = LocalHermesTheme.current
    val cardBg = if (theme.isLight) Color(0xFFF1F4F9) else Color(0xFF14241E)
    val cardBorder = if (theme.isLight) Color(0xFFBAC0CD) else Color(0xFF1B4D3C)

    val labelText = remember(block.language) {
        val lang = block.language.lowercase()
        when {
            lang.contains("chart") || lang.contains("graph") -> "📊 Se construiește graficul..."
            lang.contains("mermaid") || lang.contains("diagram") || lang.contains("flowchart") -> "📐 Se generează diagrama..."
            lang.contains("html") || lang.contains("svg") || lang.contains("widget") -> "🎨 Se construiește widget-ul..."
            else -> "⚡ Se procesează codul & datele..."
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(EmeraldAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = EmeraldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "Generare în curs • Se finalizează structura vizuală",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextMuted
                    )
                }
            }

            // Animated Skeleton Shimmer Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val heights = listOf(0.4f, 0.8f, 0.55f, 0.95f, 0.65f)
                heights.forEach { h ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(h)
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldAccent.copy(alpha = alpha * h))
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveChartCard(chart: RichContentBlock.ChartBlockData) {
    val theme = LocalHermesTheme.current
    val totalVal = remember(chart.items) { chart.items.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f) }
    val maxVal = remember(chart.items) { chart.items.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f }

    val primaryColor = when (chart.type.lowercase()) {
        "pie" -> Color(0xFF3B82F6)
        "line" -> Color(0xFF8B5CF6)
        "scatter" -> Color(0xFFF59E0B)
        else -> EmeraldAccent
    }

    val chartIcon = when (chart.type.lowercase()) {
        "pie" -> Icons.Default.PieChart
        "line" -> Icons.AutoMirrored.Filled.ShowChart
        "scatter" -> Icons.AutoMirrored.Filled.ShowChart
        else -> Icons.Default.BarChart
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (theme.isLight) Color(0xFFF1F5F9) else Color(0xFF040C08),
        border = BorderStroke(1.dp, if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF113827)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(primaryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = chartIcon,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = chart.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.5.sp
                            )
                        )
                        if (!chart.xAxisTitle.isNullOrBlank() || !chart.yAxisTitle.isNullOrBlank()) {
                            Text(
                                text = listOfNotNull(chart.xAxisTitle, chart.yAxisTitle).joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = primaryColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = chart.type.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = primaryColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body
            when (chart.type.lowercase()) {
                "pie" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Canvas(modifier = Modifier.size(110.dp)) {
                            var startAngle = -90f
                            chart.items.forEachIndexed { idx, item ->
                                val sweepAngle = (item.value / totalVal) * 360f
                                val color = parseHexColor(item.colorHex) ?: when (idx % 5) {
                                    0 -> Color(0xFF10B981)
                                    1 -> Color(0xFF3B82F6)
                                    2 -> Color(0xFFF59E0B)
                                    3 -> Color(0xFF8B5CF6)
                                    else -> Color(0xFFEC4899)
                                }
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 24.dp.toPx())
                                )
                                startAngle += sweepAngle
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            chart.items.forEachIndexed { idx, item ->
                                val color = parseHexColor(item.colorHex) ?: when (idx % 5) {
                                    0 -> Color(0xFF10B981)
                                    1 -> Color(0xFF3B82F6)
                                    2 -> Color(0xFFF59E0B)
                                    3 -> Color(0xFF8B5CF6)
                                    else -> Color(0xFFEC4899)
                                }
                                val pct = (item.value / totalVal * 100f)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 12.5.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "%.1f%%".format(pct),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = color,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                "line", "scatter" -> {
                    Column {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            val width = size.width
                            val height = size.height
                            val spacing = width / (chart.items.size.coerceAtLeast(2) - 1)

                            for (step in 1..3) {
                                val y = height * (step / 4f)
                                drawLine(
                                    color = if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                                    start = androidx.compose.ui.geometry.Offset(0f, y),
                                    end = androidx.compose.ui.geometry.Offset(width, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            val points = chart.items.mapIndexed { idx, item ->
                                val x = idx * spacing
                                val normY = (item.value / maxVal).coerceIn(0.1f, 0.95f)
                                val y = height - (normY * height)
                                androidx.compose.ui.geometry.Offset(x, y)
                            }

                            if (chart.type.lowercase() == "line" && points.size >= 2) {
                                val path = Path().apply {
                                    moveTo(points.first().x, points.first().y)
                                    for (i in 0 until points.size - 1) {
                                        val p1 = points[i]
                                        val p2 = points[i + 1]
                                        val cx = (p1.x + p2.x) / 2f
                                        cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = primaryColor,
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }

                            points.forEachIndexed { idx, pt ->
                                val color = parseHexColor(chart.items[idx].colorHex) ?: primaryColor
                                drawCircle(
                                    color = Color.Black,
                                    radius = 6.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = color,
                                    radius = 4.dp.toPx(),
                                    center = pt
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            chart.items.forEach { item ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextMuted,
                                            fontSize = 10.5.sp
                                        )
                                    )
                                    Text(
                                        text = item.formattedValue ?: "%.0f".format(item.value),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        chart.items.forEachIndexed { idx, item ->
                            val color = parseHexColor(item.colorHex) ?: when (idx % 5) {
                                0 -> EmeraldAccent
                                1 -> Color(0xFF3B82F6)
                                2 -> Color(0xFFF59E0B)
                                3 -> Color(0xFF8B5CF6)
                                else -> Color(0xFFEC4899)
                            }
                            val ratio = (item.value / maxVal).coerceIn(0.05f, 1f)

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    )
                                    Text(
                                        text = item.formattedValue ?: "%.0f".format(item.value),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = color,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF0F241A))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(ratio)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(color)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorInt = cleanHex.toLong(16)
        if (cleanHex.length == 6) {
            Color(colorInt or 0xFF000000)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        null
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveDiagramCard(diagram: RichContentBlock.DiagramBlock) {
    var showCode by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val theme = LocalHermesTheme.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (theme.isLight) Color(0xFFF1F5F9) else Color(0xFF040D08),
        border = BorderStroke(1.dp, if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF133B29)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = diagram.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "DIAGRAMĂ VIZUALĂ • ${diagram.type.uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EmeraldAccent,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                            contentDescription = "Redimensionează",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { showCode = !showCode },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (showCode) Icons.Default.Visibility else Icons.Default.Code,
                            contentDescription = if (showCode) "Vezi diagramă" else "Vezi cod",
                            tint = if (showCode) EmeraldAccent else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (showCode) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (theme.isLight) Color(0xFFF8FAFC) else Color(0xFF030805),
                    border = BorderStroke(1.dp, if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF0D281C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mermaid Code",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Mermaid Code", diagram.code))
                                    Toast.makeText(context, "Cod copiat!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiază",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = diagram.code,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = if (theme.isLight) Color(0xFF0F766E) else Color(0xFF34D399),
                                lineHeight = 18.sp
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                val height = if (isExpanded) 420.dp else 250.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (theme.isLight) Color.White else Color(0xFF040A07))
                ) {
                    val escapedCode = remember(diagram.code) {
                        diagram.code
                            .replace("\\", "\\\\")
                            .replace("`", "\\`")
                            .replace("$", "\\$")
                    }
                    val html = remember(escapedCode, theme.isLight) {
                        val mmTheme = if (theme.isLight) "default" else "dark"
                        val mmBg = if (theme.isLight) "#FFFFFF" else "#040A07"
                        val mmTextColor = if (theme.isLight) "#111827" else "#FFFFFF"
                        val mmLineColor = if (theme.isLight) "#2563EB" else "#34D399"
                        val mmMainBkg = if (theme.isLight) "#F1F5F9" else "#062417"
                        """
                        <!DOCTYPE html>
                        <html>
                        <head>
                          <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
                          <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
                          <style>
                            * { box-sizing: border-box; }
                            body {
                              margin: 0;
                              padding: 12px;
                              background-color: $mmBg;
                              color: $mmTextColor;
                              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                              display: flex;
                              justify-content: center;
                              align-items: center;
                              min-height: 100vh;
                              overflow: auto;
                            }
                            .mermaid {
                              width: 100%;
                              display: flex;
                              justify-content: center;
                            }
                          </style>
                        </head>
                        <body>
                          <div class="mermaid">
                            $escapedCode
                          </div>
                          <script>
                            try {
                              mermaid.initialize({
                                startOnLoad: true,
                                theme: '$mmTheme',
                                themeVariables: {
                                  darkMode: ${!theme.isLight},
                                  background: '$mmBg',
                                  primaryColor: '#10B981',
                                  primaryTextColor: '$mmTextColor',
                                  primaryBorderColor: '#059669',
                                  lineColor: '$mmLineColor',
                                  secondaryColor: '${if (theme.isLight) "#E2E8F0" else "#064E3B"}',
                                  tertiaryColor: '${if (theme.isLight) "#CBD5E1" else "#065F46"}',
                                  nodeBorder: '#10B981',
                                  mainBkg: '$mmMainBkg',
                                  edgeLabelBackground: '$mmBg'
                                }
                              });
                            } catch(e) {
                              document.body.innerHTML = '<pre style="color:#F87171;padding:8px;">' + e.message + '</pre>';
                            }
                          </script>
                        </body>
                        </html>
                        """.trimIndent()
                    }

                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                          WebView(ctx).apply {
                                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                                setBackgroundColor(if (theme.isLight) 0xFFFFFFFF.toInt() else 0xFF040A07.toInt())
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                        return true
                                    }
                                }
                                loadDataWithBaseURL("https://localhost", html, "text/html", "UTF-8", null)
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL("https://localhost", html, "text/html", "UTF-8", null)
                        }
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveHtmlWidgetCard(widget: RichContentBlock.HtmlWidgetBlock) {
    var showCode by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val theme = LocalHermesTheme.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (theme.isLight) Color(0xFFF1F5F9) else Color(0xFF040C08),
        border = BorderStroke(1.dp, if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF133B29)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = widget.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (widget.isSvg) "GRAFIC VECTORIAL SVG" else "WIDGET WEB INTERACTIV",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF60A5FA),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                            contentDescription = "Redimensionează",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { showCode = !showCode },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (showCode) Icons.Default.Visibility else Icons.Default.Code,
                            contentDescription = if (showCode) "Vezi widget" else "Vezi cod",
                            tint = if (showCode) Color(0xFF60A5FA) else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (showCode) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (theme.isLight) Color(0xFFF8FAFC) else Color(0xFF030805),
                    border = BorderStroke(1.dp, if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF0D281C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (widget.isSvg) "SVG Code" else "HTML Widget",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Widget Code", widget.htmlContent))
                                    Toast.makeText(context, "Cod copiat!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiază",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = widget.htmlContent,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = if (theme.isLight) Color(0xFF1E3A8A) else Color(0xFF93C5FD),
                                lineHeight = 18.sp
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                val height = if (isExpanded) 420.dp else 260.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (theme.isLight) Color.White else Color(0xFF040A07))
                ) {
                    val fullHtml = remember(widget.htmlContent, theme.isLight) {
                        if (widget.htmlContent.contains("<html", ignoreCase = true)) {
                            widget.htmlContent
                        } else {
                            val webBg = if (theme.isLight) "#FFFFFF" else "#040A07"
                            val webTextColor = if (theme.isLight) "#111827" else "#F3F4F6"
                            val inputBg = if (theme.isLight) "#F8FAFC" else "#0D261B"
                            val inputBorder = if (theme.isLight) "#CBD5E1" else "#194D36"
                            val inputColor = if (theme.isLight) "#111827" else "#FFFFFF"
                            val thBg = if (theme.isLight) "#F1F5F9" else "#0F3223"
                            val thColor = if (theme.isLight) "#2563EB" else "#10B981"
                            val tableBorder = if (theme.isLight) "#E2E8F0" else "#133626"
                            """
                            <!DOCTYPE html>
                            <html>
                            <head>
                              <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
                              <style>
                                * { box-sizing: border-box; }
                                body {
                                  margin: 0;
                                  padding: 12px;
                                  background: $webBg;
                                  color: $webTextColor;
                                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                                  font-size: 14px;
                                }
                                svg { max-width: 100%; height: auto; display: block; margin: 0 auto; }
                                table { width: 100%; border-collapse: collapse; margin-top: 8px; }
                                th, td { padding: 8px; border: 1px solid $tableBorder; text-align: left; }
                                th { background: $thBg; color: $thColor; }
                                button {
                                  background: #10B981;
                                  color: #000;
                                  border: none;
                                  padding: 8px 16px;
                                  border-radius: 6px;
                                  font-weight: bold;
                                  cursor: pointer;
                                }
                                input, select {
                                  background: $inputBg;
                                  border: 1px solid $inputBorder;
                                  color: $inputColor;
                                  padding: 8px;
                                  border-radius: 6px;
                                }
                              </style>
                            </head>
                            <body>
                              ${widget.htmlContent}
                            </body>
                            </html>
                            """.trimIndent()
                        }
                    }

                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                                setBackgroundColor(if (theme.isLight) 0xFFFFFFFF.toInt() else 0xFF040A07.toInt())
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                        return true
                                    }
                                }
                                loadDataWithBaseURL("https://localhost", fullHtml, "text/html", "UTF-8", null)
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL("https://localhost", fullHtml, "text/html", "UTF-8", null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveDashboardCard(dashboard: RichContentBlock.DashboardBlock) {
    var showJson by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val theme = LocalHermesTheme.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (theme.isLight) Color(0xFFF1F5F9) else Color(0xFF040E0A),
        border = BorderStroke(1.dp, if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF133B29)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = dashboard.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.5.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dashboard.subtitle ?: "PANOU INDICATORI & STATISTICI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFFBBF24),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                if (dashboard.rawJson != null) {
                    IconButton(
                        onClick = { showJson = !showJson },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (showJson) Icons.Default.Visibility else Icons.Default.Code,
                            contentDescription = if (showJson) "Vezi indicatori" else "Vezi date",
                            tint = if (showJson) EmeraldAccent else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (showJson && dashboard.rawJson != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (theme.isLight) Color(0xFFF8FAFC) else Color(0xFF030805),
                    border = BorderStroke(1.dp, if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF0D281C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "JSON Data",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("JSON Data", dashboard.rawJson))
                                    Toast.makeText(context, "JSON copiat!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiază",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = dashboard.rawJson,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = if (theme.isLight) Color(0xFF047857) else EmeraldAccent,
                                lineHeight = 18.sp
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                val rows = remember(dashboard.kpis) { dashboard.kpis.chunked(2) }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { rowKpis ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowKpis.forEach { kpi ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (theme.isLight) Color.White else Color(0xFF082217),
                                    border = BorderStroke(1.dp, if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF13422F)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = kpi.title,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TextSecondary,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = kpi.value,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = TextPrimary,
                                                    fontSize = 18.sp
                                                )
                                            )

                                            if (!kpi.change.isNullOrBlank()) {
                                                val isPos = kpi.isPositive ?: (!kpi.change.startsWith("-"))
                                                val changeColor = if (isPos) Color(0xFF10B981) else Color(0xFFEF4444)
                                                val changeBg = if (isPos) Color(0x2210B981) else Color(0x22EF4444)
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = changeBg
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isPos) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                                            contentDescription = null,
                                                            tint = changeColor,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Text(
                                                            text = kpi.change,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = changeColor,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 10.5.sp
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (kpi.progress != null) {
                                            LinearProgressIndicator(
                                                progress = { kpi.progress.coerceIn(0f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                color = EmeraldAccent,
                                                trackColor = if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF0F3223)
                                            )
                                        }

                                        if (!kpi.subtitle.isNullOrBlank()) {
                                            Text(
                                                text = kpi.subtitle,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextMuted,
                                                    fontSize = 10.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            if (rowKpis.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CodeBlockCard(language: String, code: String) {
    val theme = LocalHermesTheme.current
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (theme.isLight) Color(0xFFF1F5F9) else Color(0xFF040A07),
        border = BorderStroke(1.dp, if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF132F23)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(EmeraldAccent)
                    )
                    Text(
                        text = language.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (theme.isLight) Color(0xFF047857) else EmeraldAccent,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Code block", code)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Cod copiat în clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiază codul",
                        tint = TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (theme.isLight) Color(0xFF0F2D23) else Color(0xFFB5EAD7),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                )
            )
        }
    }
}

@Composable
fun InteractiveTableCard(headers: List<String>, rows: List<List<String>>) {
    val theme = LocalHermesTheme.current
    val scrollState = rememberScrollState()
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (theme.isLight) Color(0xFFF1F5F9) else Color(0xFF06140E),
        border = BorderStroke(1.dp, if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF133626)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF0F3223))
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                headers.forEach { header ->
                    Text(
                        text = header,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (theme.isLight) Color(0xFF1E3A8A) else EmeraldAccent,
                            fontSize = 13.5.sp
                        ),
                        modifier = Modifier.widthIn(min = 100.dp, max = 200.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            rows.forEachIndexed { index, row ->
                val rowBg = if (index % 2 == 0) Color.Transparent else (if (theme.isLight) Color(0xFFF8FAFC) else Color(0x1110B981))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(rowBg)
                        .padding(vertical = 7.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEachIndexed { cellIdx, cell ->
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontSize = 13.5.sp
                            ),
                            modifier = Modifier.widthIn(min = 100.dp, max = 200.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricsGridCard(items: List<Pair<String, String>>) {
    val theme = LocalHermesTheme.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (theme.isLight) Color(0xFFF1F5F9) else Color(0xFF06150F),
        border = BorderStroke(1.dp, if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF133325)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { (key, value) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (theme.isLight) Color.White else Color(0xFF0C241B),
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = if (theme.isLight) Color(0xFF047857) else EmeraldAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun CalloutCard(callout: RichContentBlock.Callout) {
    val (borderColor, icon, bgTint) = when (callout.type) {
        CalloutType.WARNING -> Triple(Color(0xFFEAB308), Icons.Default.Warning, Color(0x19EAB308))
        CalloutType.TIP -> Triple(EmeraldAccent, Icons.Default.Lightbulb, Color(0x1910B981))
        CalloutType.SUCCESS -> Triple(Color(0xFF22C55E), Icons.Default.CheckCircle, Color(0x1922C55E))
        CalloutType.INFO -> Triple(Color(0xFF3B82F6), Icons.Default.Info, Color(0x193B82F6))
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgTint,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = callout.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = borderColor,
                        fontSize = 13.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = callout.message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 19.sp
                    )
                )
            }
        }
    }
}

@Composable
fun ImageBlockCard(image: RichContentBlock.ImageBlock) {
    val theme = LocalHermesTheme.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (theme.isLight) Color(0xFFF1F5F9) else Color(0xFF040A07)),
        border = BorderStroke(1.dp, if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF132F23)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(if (theme.isLight) Color(0xFFF1F5F9) else Color(0xFF040A07))
        ) {
            coil.compose.AsyncImage(
                model = image.url,
                contentDescription = image.alt,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun InteractiveMarkdownFileCard(
    doc: RichContentBlock.MarkdownDocumentBlock,
    onOpenUrl: ((String) -> Unit)? = null
) {
    var viewMode by remember { mutableStateOf("preview") } // "preview" or "code"
    val theme = LocalHermesTheme.current
    val context = LocalContext.current
    val lineCount = remember(doc.content) { doc.content.lines().size }
    val wordCount = remember(doc.content) { doc.content.split(Regex("\\s+")).count { it.isNotBlank() } }
    val fileSizeFormatted = remember(doc.content) {
        val bytes = doc.content.toByteArray().size
        if (bytes < 1024) "$bytes B" else String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
    }

    // Inner parsed blocks for preview mode
    val innerBlocks = remember(doc.content) {
        RichContentParser.parse(doc.content, isNested = true)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (theme.isLight) Color(0xFFF1F5F9) else Color(0xFF040E0A)),
        border = BorderStroke(1.dp, if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF14422E)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Document Icon + File Name & Metadata
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldAccent.copy(alpha = 0.15f))
                            .border(1.dp, EmeraldAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Document Markdown",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = doc.fileName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 15.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (theme.isLight) Color(0xFFCBD5E1) else Color(0xFF0D2C1F),
                                border = BorderStroke(0.8.dp, EmeraldAccent.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "MARKDOWN",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (theme.isLight) Color(0xFF047857) else EmeraldAccent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "$lineCount rânduri • $wordCount cuvinte • $fileSizeFormatted",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Right: Mode Switcher & Copy Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Segmented pill: Previzualizare formatată vs Cod sursă
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF071911),
                        border = BorderStroke(1.dp, if (theme.isLight) Color(0xFFCBD5E1) else Color(0xFF133626))
                    ) {
                        Row(
                            modifier = Modifier.padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "Formatat" button
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (viewMode == "preview") EmeraldAccent.copy(alpha = 0.25f) else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { viewMode = "preview" }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "Formatat",
                                        tint = if (viewMode == "preview") (if (theme.isLight) Color(0xFF047857) else EmeraldAccent) else TextMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Formatat",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (viewMode == "preview") (if (theme.isLight) Color(0xFF047857) else EmeraldAccent) else TextMuted,
                                            fontWeight = if (viewMode == "preview") FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            // "Cod" button
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (viewMode == "code") EmeraldAccent.copy(alpha = 0.25f) else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { viewMode = "code" }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = "Cod Sursă",
                                        tint = if (viewMode == "code") (if (theme.isLight) Color(0xFF047857) else EmeraldAccent) else TextMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Cod",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (viewMode == "code") (if (theme.isLight) Color(0xFF047857) else EmeraldAccent) else TextMuted,
                                            fontWeight = if (viewMode == "code") FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Copy button
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(doc.fileName, doc.content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Documentul '${doc.fileName}' a fost copiat!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiază fișierul",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = if (theme.isLight) Color(0xFFCBD5E1) else Color(0xFF133626), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Body rendering
            if (viewMode == "preview") {
                // FORMATTED MARKDOWN RENDERING
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    innerBlocks.forEach { block ->
                        when (block) {
                            is RichContentBlock.Text -> {
                                Text(
                                    text = parseMarkdownToAnnotatedString(block.text, isStreaming = false),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 15.sp,
                                        lineHeight = 24.sp,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Normal
                                    )
                                )
                            }
                            is RichContentBlock.MarkdownTable -> {
                                InteractiveTableCard(headers = block.headers, rows = block.rows)
                            }
                            is RichContentBlock.Callout -> {
                                CalloutCard(callout = block)
                            }
                            is RichContentBlock.MetricsGrid -> {
                                MetricsGridCard(items = block.items)
                            }
                            is RichContentBlock.Code -> {
                                CodeBlockCard(language = block.language, code = block.code)
                            }
                            is RichContentBlock.ChartBlockData -> {
                                InteractiveChartCard(chart = block)
                            }
                            is RichContentBlock.DiagramBlock -> {
                                InteractiveDiagramCard(diagram = block)
                            }
                            is RichContentBlock.DashboardBlock -> {
                                InteractiveDashboardCard(dashboard = block)
                            }
                            is RichContentBlock.HtmlWidgetBlock -> {
                                InteractiveHtmlWidgetCard(widget = block)
                            }
                            is RichContentBlock.ImageBlock -> {
                                ImageBlockCard(image = block)
                            }
                            is RichContentBlock.MarkdownDocumentBlock -> {
                                Text(
                                    text = parseMarkdownToAnnotatedString(block.content, isStreaming = false),
                                    style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontSize = 15.sp)
                                )
                            }
                            is RichContentBlock.StreamingChartPlaceholderBlock -> {
                                StreamingChartPlaceholderCard(block = block)
                            }
                        }
                    }
                }
            } else {
                // RAW CODE VIEW
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (theme.isLight) Color(0xFFF1F5F9) else Color(0xFF020704),
                    border = BorderStroke(1.dp, if (theme.isLight) Color(0xFFCBD5E1) else Color(0xFF0D281C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = doc.content,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (theme.isLight) Color(0xFF0F2D23) else Color(0xFFB5EAD7),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (theme.isLight) Color(0xFFCBD5E1).copy(alpha = 0.3f) else Color(0xFF081C13))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(EmeraldAccent)
                    )
                    Text(
                        text = "Document structurat GFM • Previzualizare Formatată",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (theme.isLight) Color(0xFF047857) else EmeraldAccent.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Text(
                    text = "Fișier Markdown",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 10.5.sp
                    )
                )
            }
        }
    }
}

@Composable
fun parseMarkdownToAnnotatedString(text: String, isStreaming: Boolean = false): AnnotatedString {
    val theme = LocalHermesTheme.current
    val isLight = theme.isLight

    val heading1Color = if (isLight) Color(0xFF111827) else Color.White
    val heading2Color = if (isLight) Color(0xFF047857) else EmeraldAccent
    val heading3Color = if (isLight) Color(0xFF1D4ED8) else Color(0xFF93C5FD)
    val heading4Color = if (isLight) Color(0xFF374151) else Color(0xFFE5E7EB)
    val quoteColor = if (isLight) Color(0xFF065F46) else Color(0xFFA7F3D0)
    val dividerColor = if (isLight) Color(0xFFCBD5E1) else Color(0xFF1F4432)
    val bulletColor = if (isLight) Color(0xFF047857) else EmeraldAccent
    val boldColor = if (isLight) Color(0xFF111827) else Color.White
    val italicColor = if (isLight) Color(0xFF374151) else Color(0xFFD1D5DB)
    val codeBg = if (isLight) Color(0xFFE2E8F0) else Color(0xFF1E293B)
    val codeColor = if (isLight) Color(0xFF0F766E) else Color(0xFF34D399)
    val linkColor = if (isLight) Color(0xFF2563EB) else Color(0xFF60A5FA)

    return buildAnnotatedString {
        val lines = text.lines()

        lines.forEachIndexed { index, line ->
            var currentLine = line
            val trimmed = currentLine.trim()

            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                withStyle(style = SpanStyle(color = dividerColor)) {
                    append("────────────────────────────────────────")
                }
                if (index < lines.lastIndex) append("\n")
                return@forEachIndexed
            } else if (trimmed.startsWith("# ")) {
                val headerText = trimmed.drop(2)
                withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold, color = heading1Color, fontSize = 19.sp)) {
                    append(headerText)
                }
                if (index < lines.lastIndex) append("\n\n")
                return@forEachIndexed
            } else if (trimmed.startsWith("## ")) {
                val headerText = trimmed.drop(3)
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = heading2Color, fontSize = 17.5.sp)) {
                    append(headerText)
                }
                if (index < lines.lastIndex) append("\n\n")
                return@forEachIndexed
            } else if (trimmed.startsWith("### ")) {
                val headerText = trimmed.drop(4)
                withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold, color = heading3Color, fontSize = 16.sp)) {
                    append(headerText)
                }
                if (index < lines.lastIndex) append("\n")
                return@forEachIndexed
            } else if (trimmed.startsWith("#### ")) {
                val headerText = trimmed.drop(5)
                withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold, color = heading4Color, fontSize = 15.sp)) {
                    append(headerText)
                }
                if (index < lines.lastIndex) append("\n")
                return@forEachIndexed
            } else if (trimmed.startsWith("> ")) {
                val quoteText = trimmed.drop(2)
                withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, color = quoteColor)) {
                    append("▎ ")
                    append(quoteText)
                }
                if (index < lines.lastIndex) append("\n")
                return@forEachIndexed
            }

            if (currentLine.trim().startsWith("- ") || currentLine.trim().startsWith("* ")) {
                withStyle(style = SpanStyle(color = bulletColor, fontWeight = FontWeight.Bold)) {
                    append("✦ ")
                }
                currentLine = currentLine.trim().drop(2)
            } else if (currentLine.trim().matches(Regex("^\\d+\\.\\s.*"))) {
                val numberStr = currentLine.trim().substringBefore(" ")
                withStyle(style = SpanStyle(color = bulletColor, fontWeight = FontWeight.Bold)) {
                    append("$numberStr ")
                }
                currentLine = currentLine.trim().substringAfter(" ")
            }

            var i = 0
            while (i < currentLine.length) {
                if (currentLine.startsWith("**", i)) {
                    val endBold = currentLine.indexOf("**", i + 2)
                    if (endBold != -1) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                            append(currentLine.substring(i + 2, endBold))
                        }
                        i = endBold + 2
                        continue
                    }
                }

                if ((currentLine.startsWith("*", i) && !currentLine.startsWith("**", i)) ||
                    currentLine.startsWith("_", i)) {
                    val marker = currentLine[i].toString()
                    val endItalic = currentLine.indexOf(marker, i + 1)
                    if (endItalic != -1 && endItalic > i + 1 && currentLine[i+1] != ' ') {
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, color = italicColor)) {
                            append(currentLine.substring(i + 1, endItalic))
                        }
                        i = endItalic + 1
                        continue
                    }
                }

                if (currentLine.startsWith("`", i)) {
                    val endCode = currentLine.indexOf("`", i + 1)
                    if (endCode != -1) {
                        withStyle(style = SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBg,
                            color = codeColor
                        )) {
                            append(" ${currentLine.substring(i + 1, endCode)} ")
                        }
                        i = endCode + 1
                        continue
                    }
                }

                if (currentLine.startsWith("[", i)) {
                    val endTitle = currentLine.indexOf("](", i + 1)
                    if (endTitle != -1) {
                        val endUrl = currentLine.indexOf(")", endTitle + 2)
                        if (endUrl != -1) {
                            val title = currentLine.substring(i + 1, endTitle)
                            withStyle(style = SpanStyle(
                                color = linkColor,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            )) {
                                append(title)
                            }
                            i = endUrl + 1
                            continue
                        }
                    }
                }

                append(currentLine[i])
                i++
            }

            if (index < lines.lastIndex) {
                append("\n")
            }
        }

        if (isStreaming) {
            append(" ▍")
        }
    }
}
