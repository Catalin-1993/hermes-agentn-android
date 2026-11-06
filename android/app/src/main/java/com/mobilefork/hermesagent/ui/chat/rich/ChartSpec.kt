package com.mobilefork.hermesagent.ui.chat.rich

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

/** A ```chart block: the same format as the desktop app, forgiving where models drift. */
internal enum class ChartKind { Bar, Line, Area, Donut, Pie, Scatter, Kpi, Table, Funnel, Gauge, Radar }

internal enum class ChartFormat { Number, Compact, Currency, Percent }

internal data class ChartSeries(
    val name: String,
    val values: List<Double?> = emptyList(),
    val points: List<Pair<Double, Double>> = emptyList(),
    val asLine: Boolean = false,
)

internal data class ChartSlice(val name: String, val value: Double)

internal data class ChartKpi(
    val label: String,
    val number: Double?,
    val text: String,
    val delta: Double?,
    val trend: List<Double>,
    val hint: String,
    val lowerIsBetter: Boolean,
    val style: ChartNumberStyle,
)

internal data class ChartNumberStyle(
    val format: ChartFormat = ChartFormat.Number,
    val currency: String = "USD",
    val unit: String = "",
)

internal data class ChartSpec(
    val kind: ChartKind,
    val title: String = "",
    val subtitle: String = "",
    val style: ChartNumberStyle = ChartNumberStyle(),
    val categories: List<String> = emptyList(),
    val series: List<ChartSeries> = emptyList(),
    val slices: List<ChartSlice> = emptyList(),
    val kpis: List<ChartKpi> = emptyList(),
    val columns: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
    val stacked: Boolean = false,
    val horizontal: Boolean = false,
    val gaugeValue: Double = 0.0,
    val gaugeMax: Double = 100.0,
)

internal object ChartLimits {
    const val SERIES = 8
    const val POINTS = 2000
    const val SLICES = 12
    const val KPIS = 8
    const val ROWS = 200
}

internal sealed interface ChartParse {
    data class Ok(val spec: ChartSpec) : ChartParse
    data class Refused(val reason: String) : ChartParse
}

internal fun parseChartSpec(source: String): ChartParse {
    val root = try {
        JSONObject(source.trim())
    } catch (e: JSONException) {
        return ChartParse.Refused("not JSON")
    }
    val kind = when (root.optString("type", root.optString("kind")).lowercase()) {
        "bar", "column" -> ChartKind.Bar
        "line" -> ChartKind.Line
        "area" -> ChartKind.Area
        "donut", "doughnut" -> ChartKind.Donut
        "pie" -> ChartKind.Pie
        "scatter" -> ChartKind.Scatter
        "kpi", "kpis" -> ChartKind.Kpi
        "table" -> ChartKind.Table
        "funnel" -> ChartKind.Funnel
        "gauge" -> ChartKind.Gauge
        "radar" -> ChartKind.Radar
        else -> return ChartParse.Refused("unknown chart type")
    }
    val base = ChartSpec(
        kind = kind,
        title = root.optString("title").trim().take(160),
        subtitle = root.optString("subtitle").trim().take(160),
        style = root.numberStyle(ChartNumberStyle()),
        stacked = root.optBoolean("stacked"),
        horizontal = root.optBoolean("horizontal"),
    )
    return when (kind) {
        ChartKind.Bar, ChartKind.Line, ChartKind.Area, ChartKind.Radar -> parseCategorySeries(root, base)
        ChartKind.Scatter -> parseScatter(root, base)
        ChartKind.Donut, ChartKind.Pie, ChartKind.Funnel -> parseSlices(root, base)
        ChartKind.Kpi -> parseKpis(root, base)
        ChartKind.Table -> parseTable(root, base)
        ChartKind.Gauge -> parseGauge(root, base)
    }
}

private fun JSONObject.numberStyle(fallback: ChartNumberStyle) = ChartNumberStyle(
    format = when (optString("format").lowercase()) {
        "compact" -> ChartFormat.Compact
        "currency" -> ChartFormat.Currency
        "percent" -> ChartFormat.Percent
        "number" -> ChartFormat.Number
        else -> fallback.format
    },
    currency = optString("currency").ifBlank { fallback.currency },
    unit = optString("unit").ifEmpty { fallback.unit }.take(16),
)

private fun JSONArray.strings(): List<String> = (0 until length()).map { optString(it) }

private fun JSONArray.numbers(): List<Double?> = (0 until length()).map { index ->
    if (isNull(index)) null else opt(index).let { (it as? Number)?.toDouble() ?: Double.NaN }
}

private fun JSONObject.firstArray(vararg names: String): JSONArray? =
    names.firstNotNullOfOrNull { optJSONArray(it) }

private fun parseCategorySeries(root: JSONObject, base: ChartSpec): ChartParse {
    val categories = root.firstArray("categories", "x", "labels")?.strings().orEmpty()
    val list = seriesObjects(root) ?: return ChartParse.Refused("needs series")
    if (list.size > ChartLimits.SERIES) return ChartParse.Refused("too many series")
    val series = list.mapIndexed { index, item ->
        val values = item.optJSONArray("data")?.numbers().orEmpty()
        if (values.isEmpty() || values.size > ChartLimits.POINTS || values.any { it != null && !it.isFinite() }) {
            return ChartParse.Refused("series ${index + 1} needs numbers")
        }
        ChartSeries(
            name = item.optString("name").trim().ifBlank { "Series ${index + 1}" },
            values = values,
            asLine = item.optString("type") == "line",
        )
    }
    val longest = series.maxOf { it.values.size }
    return ChartParse.Ok(
        base.copy(
            categories = List(longest) { categories.getOrNull(it) ?: "${it + 1}" },
            series = series,
        ),
    )
}

/** `series` as objects or bare arrays, or one top-level `data` array. */
private fun seriesObjects(root: JSONObject): List<JSONObject>? {
    val raw = root.optJSONArray("series")
    if (raw != null && raw.length() > 0) {
        return (0 until raw.length()).map { index ->
            raw.optJSONObject(index) ?: JSONObject().put("data", raw.optJSONArray(index) ?: JSONArray())
        }
    }
    val data = root.optJSONArray("data") ?: return null
    return listOf(JSONObject().put("data", data).put("name", root.optString("name")))
}

private fun parseScatter(root: JSONObject, base: ChartSpec): ChartParse {
    val list = seriesObjects(root) ?: return ChartParse.Refused("needs series")
    if (list.size > ChartLimits.SERIES) return ChartParse.Refused("too many series")
    val series = list.mapIndexed { index, item ->
        val data = item.optJSONArray("data") ?: JSONArray()
        val points = (0 until data.length()).map { at ->
            val pair = data.optJSONArray(at)
            val x = pair?.opt(0) as? Number
            val y = pair?.opt(1) as? Number
            if (x == null || y == null) return ChartParse.Refused("scatter points are [x, y]")
            x.toDouble() to y.toDouble()
        }
        if (points.isEmpty() || points.size > ChartLimits.POINTS) return ChartParse.Refused("series needs points")
        ChartSeries(name = item.optString("name").trim().ifBlank { "Series ${index + 1}" }, points = points)
    }
    return ChartParse.Ok(base.copy(series = series))
}

private fun parseSlices(root: JSONObject, base: ChartSpec): ChartParse {
    val raw = root.firstArray("slices", "items", "data") ?: return ChartParse.Refused("needs slices")
    val slices = (0 until raw.length()).mapNotNull { index ->
        val item = raw.optJSONObject(index) ?: return@mapNotNull null
        val value = (item.opt("value") as? Number)?.toDouble() ?: return@mapNotNull null
        ChartSlice(item.optString("name", item.optString("label")).trim(), value)
    }
    if (slices.isEmpty() || slices.size > ChartLimits.SLICES) return ChartParse.Refused("needs 1 to 12 slices")
    if (slices.any { it.value < 0 || !it.value.isFinite() }) return ChartParse.Refused("slices cannot be negative")
    return ChartParse.Ok(base.copy(slices = slices))
}

private fun parseKpis(root: JSONObject, base: ChartSpec): ChartParse {
    val raw = root.firstArray("kpis", "items") ?: return ChartParse.Refused("needs kpis")
    val kpis = (0 until raw.length()).mapNotNull { index ->
        val item = raw.optJSONObject(index) ?: return@mapNotNull null
        val value = item.opt("value")
        ChartKpi(
            label = item.optString("label").trim(),
            number = (value as? Number)?.toDouble(),
            text = (value as? String).orEmpty(),
            delta = (item.opt("delta") as? Number)?.toDouble(),
            trend = item.optJSONArray("trend")?.numbers()?.filterNotNull().orEmpty().filter { it.isFinite() },
            hint = item.optString("hint").trim(),
            lowerIsBetter = item.optString("better") == "down",
            style = item.numberStyle(base.style),
        )
    }
    if (kpis.isEmpty() || kpis.size > ChartLimits.KPIS) return ChartParse.Refused("needs 1 to 8 kpis")
    return ChartParse.Ok(base.copy(kpis = kpis))
}

private fun parseTable(root: JSONObject, base: ChartSpec): ChartParse {
    val columns = root.optJSONArray("columns")?.strings().orEmpty()
    val raw = root.optJSONArray("rows") ?: return ChartParse.Refused("needs rows")
    if (columns.isEmpty() || raw.length() > ChartLimits.ROWS) return ChartParse.Refused("needs columns and rows")
    val rows = (0 until raw.length()).map { index ->
        val row = raw.optJSONArray(index)?.strings().orEmpty()
        List(columns.size) { row.getOrElse(it) { "" } }
    }
    return ChartParse.Ok(base.copy(columns = columns, rows = rows))
}

private fun parseGauge(root: JSONObject, base: ChartSpec): ChartParse {
    val value = (root.opt("value") as? Number)?.toDouble() ?: return ChartParse.Refused("needs value")
    val max = (root.opt("max") as? Number)?.toDouble()?.takeIf { it > 0 } ?: 100.0
    return ChartParse.Ok(base.copy(gaugeValue = value, gaugeMax = max))
}

/** How a value reads on screen. Percentages are written as people write them: 12.5 means 12.5%. */
internal fun formatChartValue(value: Double, style: ChartNumberStyle, locale: Locale = Locale.getDefault()): String {
    val text = when (style.format) {
        ChartFormat.Percent -> plainNumber(value, locale) + "%"
        ChartFormat.Compact -> compactNumber(value, locale)
        ChartFormat.Currency -> runCatching {
            NumberFormat.getCurrencyInstance(locale).apply {
                currency = Currency.getInstance(style.currency.uppercase())
                maximumFractionDigits = if (abs(value) >= 100 || value == Math.rint(value)) 0 else 2
            }.format(value)
        }.getOrElse { plainNumber(value, locale) }
        ChartFormat.Number -> plainNumber(value, locale)
    }
    return text + style.unit
}

private fun plainNumber(value: Double, locale: Locale): String =
    NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = if (abs(value) >= 100) 0 else 2
    }.format(value)

private fun compactNumber(value: Double, locale: Locale): String {
    val size = abs(value)
    return when {
        size >= 1e9 -> plainNumber(value / 1e9, locale) + "B"
        size >= 1e6 -> plainNumber(value / 1e6, locale) + "M"
        size >= 1e3 -> plainNumber(value / 1e3, locale) + "K"
        else -> plainNumber(value, locale)
    }
}

/** Round axis marks covering [min, max], always including zero when the data is all on one side. */
internal fun niceAxisTicks(min: Double, max: Double, target: Int = 4): List<Double> {
    val low = minOf(min, 0.0)
    val high = maxOf(max, 0.0).let { if (it == low) low + 1.0 else it }
    val rough = (high - low) / target
    val power = Math.pow(10.0, Math.floor(Math.log10(rough)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).first { it * power >= rough } * power
    val first = Math.floor(low / step) * step
    val last = Math.ceil(high / step) * step
    val count = Math.round((last - first) / step).toInt()
    return List(count + 1) { first + it * step }
}
