package com.mobilefork.hermesagent.ui.live

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import com.mobilefork.hermesagent.ui.calls.CallPhase
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/** The three colours a state glows with: the body, the moving light, and the halo. */
private data class OrbPalette(val deep: Color, val light: Color, val halo: Color)

private fun paletteFor(phase: CallPhase, muted: Boolean): OrbPalette = when {
    muted -> OrbPalette(Color(0xFF2A2F36), Color(0xFF5B6470), Color(0xFF6C7683))
    phase == CallPhase.Thinking -> OrbPalette(Color(0xFF2B1B6B), Color(0xFFB07CFF), Color(0xFF7B5CFF))
    phase == CallPhase.Speaking -> OrbPalette(Color(0xFF0E4D57), Color(0xFF6FF2C2), Color(0xFF2FD9A6))
    else -> OrbPalette(Color(0xFF0F2E6E), Color(0xFF59B7FF), Color(0xFF2F8BFF))
}

/**
 * A glass-like sphere with coloured light drifting inside it. It swells with the person's voice while
 * listening, breathes slowly while the agent thinks, and moves in a speaking rhythm while it talks.
 * Everything runs on the frame clock and is eased, so it never steps or jitters.
 */
@Composable
internal fun LiveOrb(phase: CallPhase, muted: Boolean, level: () -> Float, modifier: Modifier = Modifier) {
    val palette = paletteFor(phase, muted)
    val deep by animateColorAsState(palette.deep, tween(800), label = "orbDeep")
    val light by animateColorAsState(palette.light, tween(800), label = "orbLight")
    val halo by animateColorAsState(palette.halo, tween(800), label = "orbHalo")
    // One slow turn every 24 s; each light uses a whole number of turns, so the loop has no seam.
    val turn by rememberInfiniteTransition(label = "orbTurn").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "orbTurnValue",
    )
    var swell by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(phase, muted) {
        var last = withFrameNanos { it }
        var clock = 0f
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
            last = now
            clock += dt
            val target = when {
                muted -> 0f
                phase == CallPhase.Listening -> level()
                // The controller measures the voice that is being heard; a small floor keeps the orb alive between words.
                phase == CallPhase.Speaking -> 0.08f + 0.92f * level()
                phase == CallPhase.Thinking -> 0.2f + 0.12f * sin(clock * 1.7f)
                else -> 0.1f
            }.coerceIn(0f, 1f)
            val rate = if (target > swell) 14f else 5f
            swell += (target - swell) * (1f - exp(-rate * dt))
        }
    }
    Canvas(modifier = modifier) {
        val centre = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) / 2f * (0.66f + 0.14f * swell)
        val circle = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(centre, radius))
        }
        // The halo: soft light around the sphere that grows with the voice.
        val haloRadius = radius * (1.45f + 0.35f * swell)
        drawCircle(
            brush = Brush.radialGradient(
                0f to halo.copy(alpha = 0.34f + 0.25f * swell),
                0.55f to halo.copy(alpha = 0.10f),
                1f to Color.Transparent,
                center = centre,
                radius = haloRadius,
            ),
            radius = haloRadius,
            center = centre,
        )
        clipPath(circle) {
            // The body, lit from the upper left.
            drawCircle(
                brush = Brush.radialGradient(
                    0f to light.copy(alpha = 0.55f),
                    0.55f to deep,
                    1f to Color.Black.copy(alpha = 0.9f),
                    center = Offset(centre.x - radius * 0.3f, centre.y - radius * 0.35f),
                    radius = radius * 1.5f,
                ),
                radius = radius,
                center = centre,
            )
            // Three lights drifting at different speeds and directions.
            val spin = listOf(5, -7, 11)
            val tint = listOf(light, halo, light.copy(alpha = 0.8f))
            spin.forEachIndexed { index, turns ->
                val angle = (2f * PI.toFloat()) * turn * turns + index * 2.1f
                val reach = radius * (0.30f + 0.16f * swell) * (1f + 0.2f * sin(angle * 0.5f))
                val at = Offset(centre.x + cos(angle) * reach, centre.y + sin(angle * 1.1f) * reach)
                val blob = radius * (0.72f + 0.2f * swell)
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to tint[index].copy(alpha = 0.75f),
                        1f to Color.Transparent,
                        center = at,
                        radius = blob,
                    ),
                    radius = blob,
                    center = at,
                    blendMode = BlendMode.Plus,
                )
            }
            // Roundness: the edge darkens, so it reads as a ball and not a disc.
            drawCircle(
                brush = Brush.radialGradient(
                    0.55f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.55f),
                    center = centre,
                    radius = radius,
                ),
                radius = radius,
                center = centre,
            )
            // The glint of a window on glass.
            val glint = Offset(centre.x - radius * 0.36f, centre.y - radius * 0.42f)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color.White.copy(alpha = 0.42f),
                    1f to Color.Transparent,
                    center = glint,
                    radius = radius * 0.5f,
                ),
                radius = radius * 0.5f,
                center = glint,
            )
        }
        drawCircle(
            brush = Brush.sweepGradient(
                0f to Color.White.copy(alpha = 0.55f),
                0.35f to Color.White.copy(alpha = 0.08f),
                0.7f to Color.White.copy(alpha = 0.3f),
                1f to Color.White.copy(alpha = 0.55f),
                center = centre,
            ),
            radius = radius,
            center = centre,
            style = Stroke(width = 1.5f * density),
        )
    }
}
