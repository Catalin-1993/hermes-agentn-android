package com.mobilefork.hermesagent.ui.agents

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/** One point of the mark, in a square from -1 to 1. */
private data class Point(val x: Float, val y: Float)

/**
 * The mark of Brain: a brain seen from above, drawn as a small constellation of eleven points and their links,
 * two lobes with a line between them. It is the whole picture, so it stays readable from 18 dp upwards.
 */
private val POINTS = listOf(
    // left lobe
    Point(-0.50f, -0.34f), Point(-0.20f, -0.56f), Point(-0.60f, 0.06f), Point(-0.28f, 0.10f), Point(-0.38f, 0.50f),
    // right lobe, mirrored
    Point(0.50f, -0.34f), Point(0.20f, -0.56f), Point(0.60f, 0.06f), Point(0.28f, 0.10f), Point(0.38f, 0.50f),
    // the fissure between them
    Point(0f, -0.28f), Point(0f, 0.34f),
)

private val LINKS = listOf(
    0 to 1, 0 to 2, 0 to 3, 2 to 3, 2 to 4, 3 to 4, 1 to 10, 3 to 10, 3 to 11, 4 to 11,
    5 to 6, 5 to 7, 5 to 8, 7 to 8, 7 to 9, 8 to 9, 6 to 10, 8 to 10, 8 to 11, 9 to 11,
    10 to 11,
)

/**
 * Brain's picture, alive. At rest it breathes. While the agent thinks, light travels along the links one
 * way; while it uses a tool, faster and both ways; while it writes, a wave runs across both lobes. Every
 * state fades into the next, and every loop is a whole number of cycles, so nothing steps or jumps.
 * [animated] false draws the same mark still, for lists.
 */
@Composable
fun BrainMark(
    presence: AgentPresence,
    size: Dp,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val color = scheme.primary
    val light = mixColor(color, Color.White, 0.55f)
    val plate = scheme.surfaceVariant
    val ease = tween<Float>(durationMillis = 480, easing = FastOutSlowInEasing)
    val thinking by animateFloatAsState(if (presence == AgentPresence.Thinking) 1f else 0f, ease, label = "markThinking")
    val working by animateFloatAsState(if (presence == AgentPresence.Working) 1f else 0f, ease, label = "markWorking")
    val writing by animateFloatAsState(if (presence == AgentPresence.Writing) 1f else 0f, ease, label = "markWriting")
    val loop = rememberInfiniteTransition(label = "brainMark")
    // One turn of the clock is 12 s; each motion uses a whole number of cycles per turn.
    val clock by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(12_000, easing = LinearEasing)), label = "markClock")

    Canvas(modifier = modifier.size(size)) {
        val t = if (animated) clock else 0f
        drawMark(t, thinking, working, writing, color, light, plate)
    }
}

private fun DrawScope.drawMark(
    t: Float,
    thinking: Float,
    working: Float,
    writing: Float,
    color: Color,
    light: Color,
    plate: Color,
) {
    val half = this.size.minDimension / 2f
    if (half <= 0f) return
    val centre = Offset(half, half)
    val active = max(thinking, max(working, writing))
    val tau = (2 * PI).toFloat()

    // The plate: a calm disc with a soft light behind the mark that grows while the agent is busy.
    drawCircle(color = plate.copy(alpha = 0.55f), radius = half, center = centre)
    drawCircle(
        brush = Brush.radialGradient(
            0f to color.copy(alpha = 0.10f + 0.26f * active),
            1f to Color.Transparent,
            center = centre,
            radius = half,
        ),
        radius = half,
        center = centre,
    )
    drawCircle(color = color.copy(alpha = 0.22f), radius = half - 0.5f, center = centre, style = Stroke(width = 1f))

    val scale = half * 0.80f
    fun at(index: Int) = Offset(centre.x + POINTS[index].x * scale, centre.y + POINTS[index].y * scale)
    val lineWidth = max(half * 0.045f, 1.2f)
    val breath = 0.5f + 0.5f * sin(tau * t * 3f)

    // The links, dim; and the light travelling along them.
    LINKS.forEachIndexed { index, (from, to) ->
        val a = at(from)
        val b = at(to)
        drawLine(color.copy(alpha = 0.26f + 0.07f * breath), a, b, lineWidth, StrokeCap.Round)
        if (thinking > 0.01f) pulse(a, b, (t * 5f + index * 0.13f) % 1f, thinking, color, light, lineWidth)
        if (working > 0.01f) {
            val forward = (t * 10f + index * 0.21f) % 1f
            pulse(a, b, if (index % 2 == 0) forward else 1f - forward, working, color, light, lineWidth)
        }
    }

    // The points: each brightens with whatever is going on, so the whole mark is seen to move.
    POINTS.forEachIndexed { index, point ->
        var level = 0.30f + 0.14f * breath
        level += thinking * 0.40f * (0.5f + 0.5f * sin(tau * (t * 5f - index * 0.17f)))
        level += working * 0.55f * (0.5f + 0.5f * sin(tau * (t * 10f + index * 0.30f)))
        level += writing * 0.60f * (0.5f + 0.5f * sin(tau * (t * 11f - (point.x + 1f) * 0.5f)))
        level = level.coerceIn(0f, 1f)
        val centreOfPoint = at(index)
        if (level > 0.55f) {
            drawCircle(color.copy(alpha = 0.28f * (level - 0.55f) / 0.45f), half * (0.10f + 0.06f * level), centreOfPoint)
        }
        drawCircle(mixColor(color, light, level).copy(alpha = 0.60f + 0.40f * level), half * (0.050f + 0.032f * level), centreOfPoint)
    }
}

/** A short bright stretch of a link, with a head, at [position] (0 at the start, 1 at the end). */
private fun DrawScope.pulse(a: Offset, b: Offset, position: Float, weight: Float, color: Color, light: Color, width: Float) {
    val tail = (position - 0.30f).coerceAtLeast(0f)
    val head = Offset(a.x + (b.x - a.x) * position, a.y + (b.y - a.y) * position)
    val back = Offset(a.x + (b.x - a.x) * tail, a.y + (b.y - a.y) * tail)
    drawLine(color.copy(alpha = 0.55f * weight), back, head, width * 1.15f, StrokeCap.Round)
    drawCircle(light.copy(alpha = 0.95f * weight), width * 1.5f, head)
}

private fun mixColor(from: Color, to: Color, fraction: Float) = Color(
    red = from.red + (to.red - from.red) * fraction,
    green = from.green + (to.green - from.green) * fraction,
    blue = from.blue + (to.blue - from.blue) * fraction,
    alpha = from.alpha + (to.alpha - from.alpha) * fraction,
)
