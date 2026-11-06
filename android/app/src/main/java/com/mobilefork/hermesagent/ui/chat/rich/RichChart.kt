package com.mobilefork.hermesagent.ui.chat.rich

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Colours a chart can tell apart in both light and dark themes, starting from the app's accent. */
@Composable
private fun chartPalette(): List<Color> {
    val accent = MaterialTheme.colorScheme.primary
    val glass = LocalHermesGlassTokens.current
    return remember(accent, glass.success, glass.mutedText) {
        listOf(
            accent,
            glass.success,
            Color(0xFFFF9F0A),
            Color(0xFFAF52DE),
            Color(0xFF5AC8FA),
            Color(0xFFFF6482),
            glass.mutedText,
            Color(0xFFD4A95A),
        )
    }
}

private val ChartPlotHeight = 196.dp
private val ChartTileShape = RoundedCornerShape(16.dp)

/** A chart drawn by the app itself: it needs no network, and scrolls with the conversation. */
@Composable
internal fun RichChart(spec: ChartSpec, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("HermesRichChart"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (spec.title.isNotBlank() || spec.subtitle.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (spec.title.isNotBlank()) {
                    Text(spec.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                if (spec.subtitle.isNotBlank()) {
                    Text(
                        spec.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalHermesGlassTokens.current.mutedText,
                    )
                }
            }
        }
        when (spec.kind) {
            ChartKind.Bar, ChartKind.Line, ChartKind.Area -> CartesianChart(spec)
            ChartKind.Donut, ChartKind.Pie -> SliceChart(spec)
            ChartKind.Scatter -> ScatterChart(spec)
            ChartKind.Kpi -> KpiTiles(spec)
            ChartKind.Table -> SortableTable(spec)
            ChartKind.Funnel -> FunnelChart(spec)
            ChartKind.Gauge -> GaugeChart(spec)
            ChartKind.Radar -> RadarChart(spec)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeriesLegend(names: List<String>, palette: List<Color>) {
    if (names.size < 2) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        names.forEachIndexed { index, name ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(palette[index % palette.size], CircleShape),
                )
                Text(
                    name,
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalHermesGlassTokens.current.mutedText,
                )
            }
        }
    }
}

/** Bars, lines and areas. A tap on a position shows its exact values above the plot. */
@Composable
private fun CartesianChart(spec: ChartSpec) {
    val palette = chartPalette()
    val glass = LocalHermesGlassTokens.current
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = glass.mutedText, fontSize = 10.sp)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    var selected by remember(spec) { mutableIntStateOf(-1) }
    val stackedBars = spec.kind == ChartKind.Bar && spec.stacked
    val horizontal = spec.kind == ChartKind.Bar && spec.horizontal
    val (low, high) = remember(spec) {
        val positions = spec.categories.indices
        if (stackedBars) {
            val totals = positions.map { i -> spec.series.sumOf { it.values.getOrNull(i) ?: 0.0 } }
            min(0.0, totals.min()) to max(0.0, totals.max())
        } else {
            val all = spec.series.flatMap { it.values }.filterNotNull()
            (all.minOrNull() ?: 0.0) to (all.maxOrNull() ?: 1.0)
        }
    }
    val ticks = remember(low, high) { niceAxisTicks(low, high) }
    val axisLow = ticks.first()
    val axisHigh = ticks.last()
    val axisSpan = (axisHigh - axisLow).takeIf { it > 0 } ?: 1.0
    val count = spec.categories.size.coerceAtLeast(1)
    val plotHeight = if (horizontal) max(120, count * 30).dp else ChartPlotHeight

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val pick = selected.takeIf { it in spec.categories.indices }
        if (pick != null) {
            Text(
                text = spec.categories[pick] + " · " + spec.series.joinToString(" · ") { series ->
                    val value = series.values.getOrNull(pick)
                    val shown = value?.let { formatChartValue(it, spec.style) } ?: "—"
                    if (spec.series.size > 1) "${series.name} $shown" else shown
                },
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(plotHeight)
                .pointerInput(spec) {
                    detectTapGestures { tap ->
                        val along = if (horizontal) tap.y / size.height else tap.x / size.width
                        val index = (along * count).toInt().coerceIn(0, count - 1)
                        selected = if (selected == index) -1 else index
                    }
                },
        ) {
            val axisLabels = ticks.map { measurer.measure(formatChartValue(it, spec.style.copy(unit = "")), labelStyle) }
            val gutter = (axisLabels.maxOf { it.size.width } + 6.dp.toPx()).coerceAtMost(size.width * 0.3f)
            val footer = 18.dp.toPx()
            if (horizontal) {
                drawHorizontalBars(spec, palette, axisLow, axisSpan, selected, gridColor, measurer, labelStyle)
                return@Canvas
            }
            val plotW = size.width - gutter
            val plotH = size.height - footer
            fun yOf(value: Double) = plotH - ((value - axisLow) / axisSpan * plotH).toFloat()
            ticks.forEachIndexed { index, tick ->
                val y = yOf(tick)
                drawLine(gridColor, Offset(gutter, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                val label = axisLabels[index]
                drawText(label, topLeft = Offset(0f, (y - label.size.height / 2f).coerceIn(0f, size.height - label.size.height)))
            }
            val slot = plotW / count
            val labelEvery = max(1, (46.dp.toPx() / slot).toInt() + 1)
            spec.categories.forEachIndexed { index, name ->
                if (index % labelEvery == 0) {
                    val measured = measurer.measure(
                        name,
                        labelStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        constraints = androidx.compose.ui.unit.Constraints(maxWidth = (slot * labelEvery).toInt().coerceAtLeast(1)),
                    )
                    val x = gutter + slot * (index + 0.5f) - measured.size.width / 2f
                    drawText(measured, topLeft = Offset(x.coerceIn(gutter, size.width - measured.size.width), plotH + 4.dp.toPx()))
                }
            }
            if (selected in spec.categories.indices) {
                val x = gutter + slot * (selected + 0.5f)
                drawRect(gridColor, Offset(x - slot / 2f, 0f), Size(slot, plotH))
            }
            val barSeries = if (spec.kind == ChartKind.Bar) spec.series.filter { !it.asLine } else emptyList()
            if (barSeries.isNotEmpty()) {
                val groupWidth = slot * 0.72f
                val barWidth = if (stackedBars) groupWidth else groupWidth / barSeries.size
                val radius = CornerRadius(min(barWidth / 2f, 5.dp.toPx()))
                spec.categories.indices.forEach { index ->
                    var stackTop = 0.0
                    barSeries.forEachIndexed { seriesIndex, series ->
                        val value = series.values.getOrNull(index) ?: return@forEachIndexed
                        val color = palette[spec.series.indexOf(series) % palette.size]
                        val left = gutter + slot * index + (slot - groupWidth) / 2f +
                            if (stackedBars) 0f else barWidth * seriesIndex
                        val from = if (stackedBars) stackTop else 0.0
                        val to = from + value
                        stackTop = to
                        val top = min(yOf(from), yOf(to))
                        val height = max(abs(yOf(from) - yOf(to)), 1f)
                        drawRoundRect(
                            color,
                            Offset(left + 1f, top),
                            Size((barWidth - 2f).coerceAtLeast(1f), height),
                            radius,
                        )
                    }
                }
            }
            spec.series.forEachIndexed { seriesIndex, series ->
                if (spec.kind == ChartKind.Bar && !series.asLine) return@forEachIndexed
                val color = palette[seriesIndex % palette.size]
                drawSeriesLine(
                    series = series,
                    color = color,
                    xOf = { gutter + slot * (it + 0.5f) },
                    yOf = ::yOf,
                    baseline = yOf(axisLow.coerceAtLeast(0.0).coerceAtMost(axisHigh)),
                    filled = spec.kind == ChartKind.Area,
                    dots = series.values.size <= 16,
                    highlighted = selected,
                )
            }
        }
        SeriesLegend(spec.series.map { it.name }, palette)
    }
}

private fun DrawScope.drawSeriesLine(
    series: ChartSeries,
    color: Color,
    xOf: (Int) -> Float,
    yOf: (Double) -> Float,
    baseline: Float,
    filled: Boolean,
    dots: Boolean,
    highlighted: Int,
) {
    val line = Path()
    var started = false
    var firstX = 0f
    var lastX = 0f
    series.values.forEachIndexed { index, value ->
        if (value == null) {
            started = false
        } else if (!started) {
            line.moveTo(xOf(index), yOf(value))
            if (index == 0 || firstX == 0f) firstX = xOf(index)
            started = true
            lastX = xOf(index)
        } else {
            line.lineTo(xOf(index), yOf(value))
            lastX = xOf(index)
        }
    }
    if (filled && series.values.none { it == null }) {
        val area = Path().apply {
            addPath(line)
            lineTo(lastX, baseline)
            lineTo(firstX, baseline)
            close()
        }
        drawPath(
            area,
            Brush.verticalGradient(0f to color.copy(alpha = 0.28f), 1f to color.copy(alpha = 0.02f)),
        )
    }
    drawPath(line, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    series.values.forEachIndexed { index, value ->
        if (value != null && (dots || index == highlighted)) {
            drawCircle(color, radius = if (index == highlighted) 4.5.dp.toPx() else 2.5.dp.toPx(), center = Offset(xOf(index), yOf(value)))
        }
    }
}

private fun DrawScope.drawHorizontalBars(
    spec: ChartSpec,
    palette: List<Color>,
    axisLow: Double,
    axisSpan: Double,
    selected: Int,
    gridColor: Color,
    measurer: androidx.compose.ui.text.TextMeasurer,
    labelStyle: TextStyle,
) {
    val rowHeight = size.height / spec.categories.size.coerceAtLeast(1)
    val nameWidth = size.width * 0.32f
    val plotW = size.width - nameWidth
    spec.categories.forEachIndexed { index, name ->
        val rowTop = rowHeight * index
        if (index == selected) drawRect(gridColor, Offset(0f, rowTop), Size(size.width, rowHeight))
        val label = measurer.measure(
            name,
            labelStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            constraints = androidx.compose.ui.unit.Constraints(maxWidth = (nameWidth - 6.dp.toPx()).toInt().coerceAtLeast(1)),
        )
        drawText(label, topLeft = Offset(0f, rowTop + (rowHeight - label.size.height) / 2f))
        val barArea = rowHeight * 0.66f
        val barHeight = if (spec.stacked) barArea else barArea / spec.series.size
        var stack = 0.0
        spec.series.forEachIndexed { seriesIndex, series ->
            val value = series.values.getOrNull(index) ?: return@forEachIndexed
            val from = if (spec.stacked) stack else 0.0
            val to = from + value
            stack = to
            val startX = nameWidth + ((min(from, to) - axisLow) / axisSpan * plotW).toFloat()
            val width = (abs(value) / axisSpan * plotW).toFloat().coerceAtLeast(1f)
            val top = rowTop + (rowHeight - barArea) / 2f + if (spec.stacked) 0f else barHeight * seriesIndex
            drawRoundRect(
                palette[seriesIndex % palette.size],
                Offset(startX, top + 1f),
                Size(width, (barHeight - 2f).coerceAtLeast(1f)),
                CornerRadius(4.dp.toPx()),
            )
        }
    }
}

@Composable
private fun SliceChart(spec: ChartSpec) {
    val palette = chartPalette()
    val glass = LocalHermesGlassTokens.current
    val total = remember(spec) { spec.slices.sumOf { it.value }.takeIf { it > 0 } ?: 1.0 }
    val donut = spec.kind == ChartKind.Donut
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(172.dp)) {
                val gap = if (spec.slices.size > 1) 1.6f else 0f
                var start = -90f
                val stroke = 26.dp.toPx()
                spec.slices.forEachIndexed { index, slice ->
                    val sweep = (slice.value / total * 360f).toFloat()
                    val color = palette[index % palette.size]
                    if (donut) {
                        val inset = stroke / 2f
                        drawArc(
                            color,
                            start + gap / 2f,
                            (sweep - gap).coerceAtLeast(0.5f),
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - stroke, size.height - stroke),
                            style = Stroke(width = stroke),
                        )
                    } else {
                        drawArc(color, start + gap / 2f, (sweep - gap).coerceAtLeast(0.5f), useCenter = true)
                    }
                    start += sweep
                }
            }
            if (donut) {
                Text(
                    formatChartValue(total, spec.style),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            spec.slices.forEachIndexed { index, slice ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(palette[index % palette.size], CircleShape),
                    )
                    Text(
                        slice.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        formatChartValue(slice.value, spec.style),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "${Math.round(slice.value / total * 100)}%",
                        modifier = Modifier.width(36.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.mutedText,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScatterChart(spec: ChartSpec) {
    val palette = chartPalette()
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = LocalHermesGlassTokens.current.mutedText, fontSize = 10.sp)
    val points = remember(spec) { spec.series.flatMap { it.points } }
    val xTicks = remember(points) { niceAxisTicks(points.minOf { it.first }, points.maxOf { it.first }) }
    val yTicks = remember(points) { niceAxisTicks(points.minOf { it.second }, points.maxOf { it.second }) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChartPlotHeight),
        ) {
            val labels = yTicks.map { measurer.measure(formatChartValue(it, spec.style.copy(unit = "")), labelStyle) }
            val gutter = labels.maxOf { it.size.width } + 6.dp.toPx()
            val footer = 18.dp.toPx()
            val plotW = size.width - gutter
            val plotH = size.height - footer
            val xSpan = (xTicks.last() - xTicks.first()).takeIf { it > 0 } ?: 1.0
            val ySpan = (yTicks.last() - yTicks.first()).takeIf { it > 0 } ?: 1.0
            fun xOf(v: Double) = gutter + ((v - xTicks.first()) / xSpan * plotW).toFloat()
            fun yOf(v: Double) = plotH - ((v - yTicks.first()) / ySpan * plotH).toFloat()
            yTicks.forEachIndexed { index, tick ->
                drawLine(gridColor, Offset(gutter, yOf(tick)), Offset(size.width, yOf(tick)), strokeWidth = 1.dp.toPx())
                val label = labels[index]
                drawText(label, topLeft = Offset(0f, (yOf(tick) - label.size.height / 2f).coerceIn(0f, size.height - label.size.height)))
            }
            xTicks.forEach { tick ->
                val label = measurer.measure(formatChartValue(tick, ChartNumberStyle()), labelStyle)
                drawText(label, topLeft = Offset((xOf(tick) - label.size.width / 2f).coerceIn(gutter, size.width - label.size.width), plotH + 4.dp.toPx()))
            }
            spec.series.forEachIndexed { index, series ->
                val color = palette[index % palette.size].copy(alpha = 0.78f)
                series.points.forEach { (x, y) -> drawCircle(color, 3.5.dp.toPx(), Offset(xOf(x), yOf(y))) }
            }
        }
        SeriesLegend(spec.series.map { it.name }, palette)
    }
}

@Composable
private fun KpiTiles(spec: ChartSpec) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        spec.kpis.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { kpi -> KpiTile(kpi, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun chartWell(): Pair<Color, Color> {
    val on = MaterialTheme.colorScheme.onSurface
    return on.copy(alpha = 0.05f) to on.copy(alpha = 0.08f)
}

@Composable
private fun KpiTile(kpi: ChartKpi, modifier: Modifier = Modifier) {
    val glass = LocalHermesGlassTokens.current
    val accent = MaterialTheme.colorScheme.primary
    val (well, wellBorder) = chartWell()
    Column(
        modifier = modifier
            .clip(ChartTileShape)
            .background(well)
            .border(1.dp, wellBorder, ChartTileShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            kpi.label,
            style = MaterialTheme.typography.labelSmall,
            color = glass.mutedText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            kpi.number?.let { formatChartValue(it, kpi.style) } ?: kpi.text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        kpi.delta?.let { delta ->
            val good = if (kpi.lowerIsBetter) delta <= 0 else delta >= 0
            Text(
                (if (delta > 0) "+" else "") + formatChartValue(delta, ChartNumberStyle(ChartFormat.Percent)),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (good) glass.success else MaterialTheme.colorScheme.error,
            )
        }
        if (kpi.hint.isNotBlank()) {
            Text(kpi.hint, style = MaterialTheme.typography.labelSmall, color = glass.mutedText, maxLines = 2)
        }
        if (kpi.trend.size >= 2) {
            Canvas(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .height(24.dp),
            ) {
                val low = kpi.trend.min()
                val span = (kpi.trend.max() - low).takeIf { it > 0 } ?: 1.0
                val step = size.width / (kpi.trend.size - 1)
                val path = Path()
                kpi.trend.forEachIndexed { index, value ->
                    val y = size.height - ((value - low) / span * size.height).toFloat()
                    if (index == 0) path.moveTo(0f, y) else path.lineTo(step * index, y)
                }
                drawPath(path, accent, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

private const val TABLE_COLLAPSED_ROWS = 8
private val TableMinColumn: Dp = 92.dp

/** Numbers line up on the right; a tap on a heading sorts by that column, again to reverse. */
@Composable
private fun SortableTable(spec: ChartSpec) {
    val glass = LocalHermesGlassTokens.current
    val shell = LocalShellStrings.current
    var sortColumn by remember(spec) { mutableIntStateOf(-1) }
    var ascending by remember(spec) { mutableStateOf(true) }
    var showAll by rememberSaveable(spec) { mutableStateOf(false) }
    val numeric = remember(spec) {
        spec.columns.indices.map { column ->
            spec.rows.isNotEmpty() && spec.rows.all { it[column].isBlank() || chartCellNumber(it[column]) != null }
        }
    }
    val rows = remember(spec, sortColumn, ascending) {
        if (sortColumn < 0) {
            spec.rows
        } else {
            val byColumn = if (numeric[sortColumn]) {
                compareBy<List<String>> { chartCellNumber(it[sortColumn]) ?: Double.NEGATIVE_INFINITY }
            } else {
                compareBy { it[sortColumn].lowercase() }
            }
            spec.rows.sortedWith(if (ascending) byColumn else byColumn.reversed())
        }
    }
    val shown = if (showAll) rows else rows.take(TABLE_COLLAPSED_ROWS)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columnWidth = max(maxWidth.value / spec.columns.size, TableMinColumn.value).dp
        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Row(modifier = Modifier.padding(bottom = 6.dp)) {
                spec.columns.forEachIndexed { index, name ->
                    Text(
                        text = name + when {
                            sortColumn != index -> ""
                            ascending -> " ↑"
                            else -> " ↓"
                        },
                        modifier = Modifier
                            .width(columnWidth)
                            .clickable {
                                if (sortColumn == index) ascending = !ascending else {
                                    sortColumn = index
                                    ascending = true
                                }
                            }
                            .padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = glass.mutedText,
                        textAlign = if (numeric[index]) TextAlign.End else TextAlign.Start,
                        maxLines = 2,
                    )
                }
            }
            shown.forEach { row ->
                Box(
                    Modifier
                        .width(columnWidth * spec.columns.size)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                )
                Row(modifier = Modifier.padding(vertical = 7.dp)) {
                    row.forEachIndexed { index, cell ->
                        Text(
                            cell,
                            modifier = Modifier
                                .width(columnWidth)
                                .padding(horizontal = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = if (numeric[index]) TextAlign.End else TextAlign.Start,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
    if (rows.size > TABLE_COLLAPSED_ROWS) {
        Text(
            text = if (showAll) shell.extra.richShowLess else shell.extra.richShowAllRows(rows.size),
            modifier = Modifier
                .clickable { showAll = !showAll }
                .padding(vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun chartCellNumber(cell: String): Double? =
    cell.trim().replace(",", "").removeSuffix("%").trim().trimStart('$', '€', '£').toDoubleOrNull()

/** Each stage shows its share of the first, as wide as that share. */
@Composable
private fun FunnelChart(spec: ChartSpec) {
    val glass = LocalHermesGlassTokens.current
    val accent = MaterialTheme.colorScheme.primary
    val top = spec.slices.first().value.takeIf { it > 0 } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        spec.slices.forEach { slice ->
            val share = (slice.value / top).coerceIn(0.0, 1.0).toFloat()
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        slice.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        formatChartValue(slice.value, spec.style) + "  ·  ${Math.round(share * 100)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.mutedText,
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth(share.coerceAtLeast(0.02f))
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(accent),
                )
            }
        }
    }
}

@Composable
private fun GaugeChart(spec: ChartSpec) {
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f)
    val share = (spec.gaugeValue / spec.gaugeMax).coerceIn(0.0, 1.0).toFloat()
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Canvas(
            modifier = Modifier
                .width(208.dp)
                .height(112.dp),
        ) {
            val stroke = 16.dp.toPx()
            val diameter = min(size.width, size.height * 2f) - stroke
            val topLeft = Offset((size.width - diameter) / 2f, stroke / 2f)
            drawArc(track, 180f, 180f, false, topLeft, Size(diameter, diameter), style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(accent, 180f, 180f * share, false, topLeft, Size(diameter, diameter), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(
            formatChartValue(spec.gaugeValue, spec.style),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RadarChart(spec: ChartSpec) {
    val palette = chartPalette()
    val grid = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = LocalHermesGlassTokens.current.mutedText, fontSize = 10.sp)
    val axes = spec.categories.size.coerceAtLeast(3)
    val top = remember(spec) { spec.series.flatMap { it.values }.filterNotNull().maxOrNull()?.takeIf { it > 0 } ?: 1.0 }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) / 2f - 24.dp.toPx()
            fun point(axis: Int, share: Float): Offset {
                val angle = Math.toRadians(-90.0 + 360.0 * axis / axes)
                return center + Offset((cos(angle) * radius * share).toFloat(), (sin(angle) * radius * share).toFloat())
            }
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { ring ->
                val path = Path()
                repeat(axes) { axis ->
                    val at = point(axis, ring)
                    if (axis == 0) path.moveTo(at.x, at.y) else path.lineTo(at.x, at.y)
                }
                path.close()
                drawPath(path, grid, style = Stroke(1.dp.toPx()))
            }
            spec.categories.forEachIndexed { axis, name ->
                drawLine(grid, center, point(axis, 1f), strokeWidth = 1.dp.toPx())
                val label = measurer.measure(name, labelStyle, maxLines = 1)
                val at = point(axis, 1.12f)
                drawText(
                    label,
                    topLeft = Offset(
                        (at.x - label.size.width / 2f).coerceIn(0f, (size.width - label.size.width).coerceAtLeast(0f)),
                        (at.y - label.size.height / 2f).coerceIn(0f, (size.height - label.size.height).coerceAtLeast(0f)),
                    ),
                )
            }
            spec.series.forEachIndexed { index, series ->
                val color = palette[index % palette.size]
                val path = Path()
                repeat(axes) { axis ->
                    val share = ((series.values.getOrNull(axis) ?: 0.0) / top).toFloat().coerceIn(0f, 1f)
                    val at = point(axis, share)
                    if (axis == 0) path.moveTo(at.x, at.y) else path.lineTo(at.x, at.y)
                }
                path.close()
                drawPath(path, color.copy(alpha = 0.2f))
                drawPath(path, color, style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))
            }
        }
        SeriesLegend(spec.series.map { it.name }, palette)
    }
}
