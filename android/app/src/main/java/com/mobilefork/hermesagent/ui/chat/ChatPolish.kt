package com.mobilefork.hermesagent.ui.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens

private const val DOT_CYCLE_MILLIS = 1_200
private const val DOT_STAGGER_MILLIS = 160

/**
 * Three dots that rise and fall one after the other, as in a chat app when the other person writes.
 * Each dot lifts, brightens and swells a little at the top of its turn, then settles back.
 */
@Composable
internal fun TypingDots(
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp,
) {
    val glass = LocalHermesGlassTokens.current
    val transition = rememberInfiniteTransition(label = "typingDots")
    val label = LocalShellStrings.current.extra.stepThinking
    Row(
        modifier = modifier
            .semantics { contentDescription = label }
            .testTag("HermesTypingDots"),
        horizontalArrangement = Arrangement.spacedBy(dotSize * 0.6f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val phase by transition.animateFloat(
                initialValue = 0f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = DOT_CYCLE_MILLIS
                        0f at 0 using FastOutSlowInEasing
                        1f at DOT_CYCLE_MILLIS / 4 using FastOutSlowInEasing
                        0f at DOT_CYCLE_MILLIS / 2 using FastOutSlowInEasing
                        0f at DOT_CYCLE_MILLIS
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = androidx.compose.animation.core.StartOffset(index * DOT_STAGGER_MILLIS),
                ),
                label = "dot$index",
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .graphicsLayer {
                        translationY = -phase * dotSize.toPx() * 0.7f
                        val grow = 1f + phase * 0.18f
                        scaleX = grow
                        scaleY = grow
                        alpha = 0.4f + phase * 0.6f
                    }
                    .background(glass.mutedText, CircleShape),
            )
        }
    }
}

/** A small dot that breathes, for the step in progress. */
@Composable
internal fun PulseDot(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulseDot")
    val grow by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale",
    )
    val halo by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1_400, easing = LinearEasing), RepeatMode.Restart),
        label = "pulseHalo",
    )
    val color = MaterialTheme.colorScheme.primary
    Box(modifier = modifier.size(16.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(16.dp).scale(0.6f + (0.35f - halo) * 2.2f).alpha(halo).background(color, CircleShape))
        Box(Modifier.size(8.dp).scale(grow).background(color, CircleShape))
    }
}
