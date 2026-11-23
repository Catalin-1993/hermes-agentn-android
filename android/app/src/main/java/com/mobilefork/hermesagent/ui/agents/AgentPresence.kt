package com.mobilefork.hermesagent.ui.agents

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.data.AvatarChoice
import com.mobilefork.hermesagent.data.avatarChoiceOf

/** What the agent is doing right now, as the avatar shows it. */
enum class AgentPresence {
    /** Waiting for the person: a slow breath. */
    Idle,

    /** Working on the answer, nothing to show yet: one arc circling the core. */
    Thinking,

    /** Using a tool: two arcs circling faster, in opposite directions. */
    Working,

    /** The reply is being written: waves leave the core. */
    Writing,
}

/** The presence for the turn in progress; [writing] is true once reply text has started to arrive. */
fun presenceOf(isSending: Boolean, writing: Boolean, toolRunning: Boolean): AgentPresence = when {
    !isSending -> AgentPresence.Idle
    writing -> AgentPresence.Writing
    toolRunning -> AgentPresence.Working
    else -> AgentPresence.Thinking
}

/**
 * The agent's picture, alive. The built-in mark becomes a small orb that follows what the agent does; a
 * picture the person chose keeps its look and gets the same motion as a thin ring around it.
 */
@Composable
fun AnimatedAgentAvatar(
    agent: AgentProfile,
    presence: AgentPresence,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val choice = remember(agent.avatarPath) { avatarChoiceOf(agent.avatarPath) }
    if (agent.isMain && choice == AvatarChoice.Generated) {
        BrainMark(presence = presence, size = size, modifier = modifier)
    } else {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            AgentAvatar(agent = agent, size = size * 0.78f)
            AgentOrb(presence = presence, size = size, modifier = Modifier, core = false)
        }
    }
}

/** The orb itself: a glow, a core, circling arcs and outgoing waves, each fading in and out with the state. */
@Composable
fun AgentOrb(
    presence: AgentPresence,
    size: Dp,
    modifier: Modifier = Modifier,
    /** False draws only the ring and waves, for a picture placed in the middle. */
    core: Boolean = true,
) {
    val color = MaterialTheme.colorScheme.primary
    val light = lerpColor(color, Color.White, 0.55f)
    val feel = tween<Float>(durationMillis = 420, easing = FastOutSlowInEasing)
    val arcOne by animateFloatAsState(if (presence == AgentPresence.Idle || presence == AgentPresence.Writing) 0f else 1f, feel, label = "arcOne")
    val arcTwo by animateFloatAsState(if (presence == AgentPresence.Working) 1f else 0f, feel, label = "arcTwo")
    val waves by animateFloatAsState(if (presence == AgentPresence.Writing) 1f else 0f, feel, label = "waves")
    val glow by animateFloatAsState(if (presence == AgentPresence.Idle) 0.35f else 0.7f, feel, label = "glow")
    val spinMillis = if (presence == AgentPresence.Working) 1500 else 2600

    val loop = rememberInfiniteTransition(label = "orb")
    val breath by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    val spin by loop.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(spinMillis, easing = LinearEasing)),
        label = "spin",
    )
    val wave by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "wave",
    )

    Canvas(modifier = modifier.size(size)) {
        val half = this.size.minDimension / 2f
        val center = Offset(half, half)
        if (core) {
            // The glow behind the core.
            drawCircle(
                brush = Brush.radialGradient(listOf(color.copy(alpha = 0.32f * glow), Color.Transparent), center, half),
                radius = half,
                center = center,
            )
            // The core breathes a little at rest and a little more while it works.
            val coreRadius = half * (0.40f + 0.025f * breath + 0.03f * (arcOne + waves).coerceAtMost(1f) * breath)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(light, color),
                    center = center.copy(x = center.x - coreRadius * 0.3f, y = center.y - coreRadius * 0.35f),
                    radius = coreRadius * 1.6f,
                ),
                radius = coreRadius,
                center = center,
            )
        }
        val ringRadius = half * 0.86f
        val stroke = Stroke(width = half * 0.07f, cap = StrokeCap.Round)
        if (arcOne > 0.01f) arc(color.copy(alpha = 0.9f * arcOne), spin, 96f, ringRadius, center, stroke)
        if (arcTwo > 0.01f) arc(light.copy(alpha = 0.8f * arcTwo), -spin * 1.35f + 180f, 70f, ringRadius * 0.9f, center, stroke)
        if (waves > 0.01f) {
            for (offset in listOf(0f, 0.5f)) {
                val progress = (wave + offset) % 1f
                drawCircle(
                    color = color.copy(alpha = 0.5f * (1f - progress) * waves),
                    radius = half * (0.45f + 0.5f * progress),
                    center = center,
                    style = Stroke(width = half * 0.05f),
                )
            }
        }
    }
}

private fun DrawScope.arc(color: Color, start: Float, sweep: Float, radius: Float, center: Offset, stroke: Stroke) {
    drawArc(
        color = color,
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = stroke,
    )
}

private fun lerpColor(from: Color, to: Color, fraction: Float): Color = Color(
    red = from.red + (to.red - from.red) * fraction,
    green = from.green + (to.green - from.green) * fraction,
    blue = from.blue + (to.blue - from.blue) * fraction,
    alpha = from.alpha + (to.alpha - from.alpha) * fraction,
)
