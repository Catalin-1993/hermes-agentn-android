package com.mobilefork.hermesagent.ui.live

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mobilefork.hermesagent.ui.calls.CallPhase
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/** The colour a state glows with, taken from the app's theme so the orb always belongs to it. */
@Composable
private fun glowFor(phase: CallPhase, muted: Boolean): Color {
    val scheme = MaterialTheme.colorScheme
    return when {
        muted -> scheme.onSurfaceVariant
        phase == CallPhase.Thinking -> scheme.tertiary
        phase == CallPhase.Speaking -> LocalHermesGlassTokens.current.success
        else -> scheme.primary
    }
}

private fun mix(from: Color, to: Color, fraction: Float) = Color(
    red = from.red + (to.red - from.red) * fraction,
    green = from.green + (to.green - from.green) * fraction,
    blue = from.blue + (to.blue - from.blue) * fraction,
    alpha = 1f,
)

/**
 * A calm sphere of light. It swells with the person's voice while listening, breathes while the agent
 * thinks, and sends soft rings outward while it speaks. One colour, taken from the theme, eased between
 * states; everything runs on the frame clock, so it never steps or jitters.
 */
@Composable
internal fun LiveOrb(phase: CallPhase, muted: Boolean, level: () -> Float, modifier: Modifier = Modifier) {
    val glow by animateColorAsState(glowFor(phase, muted), tween(900), label = "orbGlow")
    val loop = rememberInfiniteTransition(label = "orb")
    // One slow turn every 20 s for the light inside; the rings repeat every 2.4 s.
    val turn by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(20_000, easing = LinearEasing), RepeatMode.Restart), label = "turn")
    val ring by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2_400, easing = LinearEasing), RepeatMode.Restart), label = "ring")
    var swell by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(phase, muted) {
        var last = withFrameNanos { it }
        var clock = 0f
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.1f)
            last = now
            clock += dt
            val heard = level().let { if (it.isFinite()) it.coerceIn(0f, 1f) else 0f }
            val target = when {
                muted -> 0f
                phase == CallPhase.Listening -> heard
                phase == CallPhase.Speaking -> 0.10f + 0.90f * heard
                phase == CallPhase.Thinking -> 0.22f + 0.12f * sin(clock * 1.5f)
                else -> 0.1f
            }
            val rate = if (target > swell) 12f else 4f
            swell += (target - swell) * (1f - exp(-rate * dt))
        }
    }
    val speaking = phase == CallPhase.Speaking && !muted
    Canvas(modifier = modifier) {
        val centre = Offset(size.width / 2f, size.height / 2f)
        val half = min(size.width, size.height) / 2f
        if (half <= 0f) return@Canvas
        val radius = half * (0.50f + 0.10f * swell)
        // The halo: a soft light around the sphere, wider as the voice grows.
        val haloRadius = half * (0.95f + 0.05f * swell)
        drawCircle(
            brush = Brush.radialGradient(
                0f to glow.copy(alpha = 0.30f + 0.20f * swell),
                0.55f to glow.copy(alpha = 0.08f),
                1f to Color.Transparent,
                center = centre,
                radius = haloRadius,
            ),
            radius = haloRadius,
            center = centre,
        )
        if (speaking) {
            // Two rings leave the sphere in turn and fade as they widen.
            for (offset in listOf(0f, 0.5f)) {
                val progress = (ring + offset) % 1f
                drawCircle(
                    color = glow.copy(alpha = 0.32f * (1f - progress)),
                    radius = radius * (1.02f + 0.55f * progress),
                    center = centre,
                    style = Stroke(width = 1.4f * density),
                )
            }
        }
        // The body, lit from the upper left.
        val lit = mix(glow, Color.White, 0.55f)
        val deep = mix(glow, Color.Black, 0.55f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to lit,
                0.5f to glow,
                1f to deep,
                center = Offset(centre.x - radius * 0.32f, centre.y - radius * 0.38f),
                radius = radius * 1.7f,
            ),
            radius = radius,
            center = centre,
        )
        // One soft light drifting inside, slowly.
        val angle = 2f * PI.toFloat() * turn
        val drift = Offset(centre.x + cos(angle) * radius * 0.28f, centre.y + sin(angle) * radius * 0.28f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color.White.copy(alpha = 0.30f),
                1f to Color.Transparent,
                center = drift,
                radius = radius * 0.8f,
            ),
            radius = radius * 0.8f,
            center = drift,
        )
        // A fine rim, brighter where the light falls.
        drawCircle(
            brush = Brush.sweepGradient(
                0f to Color.White.copy(alpha = 0.50f),
                0.4f to Color.White.copy(alpha = 0.06f),
                0.75f to Color.White.copy(alpha = 0.22f),
                1f to Color.White.copy(alpha = 0.50f),
                center = centre,
            ),
            radius = radius,
            center = centre,
            style = Stroke(width = 1.2f * density),
        )
    }
}
