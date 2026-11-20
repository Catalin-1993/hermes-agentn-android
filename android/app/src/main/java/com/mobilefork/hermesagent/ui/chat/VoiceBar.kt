package com.mobilefork.hermesagent.ui.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mobilefork.hermesagent.R
import kotlinx.coroutines.delay
import kotlin.math.exp
import kotlin.math.pow

/** What the recording bar shows: the words heard so far, and whether they are being finalised. */
internal data class VoiceBarUi(val heard: String, val finishing: Boolean)

/**
 * The bar that replaces the composer while the person speaks: their words appear above it as they
 * are said, and below are cancel, the live sound line, confirm (the text goes into the field) and send.
 */
@Composable
internal fun VoiceBarContent(
    ui: VoiceBarUi,
    level: () -> Float,
    cancelLabel: String,
    confirmLabel: String,
    sendLabel: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onSend: () -> Unit,
) {
    val typed = rememberTypedText(ui.heard)
    Column(modifier = Modifier.fillMaxWidth().testTag("HermesVoiceBar")) {
        if (typed.isNotEmpty()) {
            Text(
                text = typed,
                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 2.dp).testTag("HermesVoiceBarText"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 7,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BarButton(R.drawable.ic_action_close, cancelLabel, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), MaterialTheme.colorScheme.onSurface, "HermesVoiceCancel", true, onCancel)
            LiveWaveform(
                level = if (ui.finishing) ({ 0f }) else level,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).height(36.dp).testTag("HermesVoiceWave"),
            )
            BarButton(R.drawable.ic_voice_check, confirmLabel, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), MaterialTheme.colorScheme.onSurface, "HermesVoiceConfirm", !ui.finishing, onConfirm)
            BarButton(R.drawable.ic_ui_send, sendLabel, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, "HermesVoiceSend", !ui.finishing, onSend)
        }
    }
}

@Composable
private fun BarButton(icon: Int, label: String, container: Color, content: Color, tag: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(container.copy(alpha = if (enabled) container.alpha else container.alpha * 0.4f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = content.copy(alpha = if (enabled) 1f else 0.4f), modifier = Modifier.size(22.dp))
    }
}

/**
 * Words reach the screen a few characters at a time, so a new reading of the speech glides in instead
 * of jumping. When the new reading changes earlier words, only the changed end is rewritten.
 */
@Composable
internal fun rememberTypedText(target: String): String {
    var shown by remember { mutableStateOf("") }
    LaunchedEffect(target) {
        val common = shown.commonPrefixWith(target).length
        shown = target.take(common)
        while (shown.length < target.length) {
            val remaining = target.length - shown.length
            shown = target.take(shown.length + maxOf(1, remaining / 10))
            delay(16)
        }
    }
    return shown
}

/**
 * A line of dots that turns into bars as the voice gets louder, scrolling smoothly to the left. The
 * scroll follows the display's frame clock, so it never steps; the level is eased, so it never jitters.
 */
@Composable
internal fun LiveWaveform(level: () -> Float, color: Color, modifier: Modifier = Modifier) {
    val slots = 46
    val history = remember { FloatArray(slots) }
    var scroll by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = androidx.compose.runtime.withFrameNanos { it }
        var smooth = 0f
        var carried = 0f
        while (true) {
            val now = androidx.compose.runtime.withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
            last = now
            val target = level().coerceIn(0f, 1f).let { if (it < 0.1f) 0f else ((it - 0.1f) / 0.9f).pow(1.2f) }
            // Quick to rise, slow to fall, whatever the frame rate.
            val rate = if (target > smooth) 22f else 7f
            smooth += (target - smooth) * (1f - exp(-rate * dt))
            carried += dt
            while (carried >= STEP_SECONDS) {
                carried -= STEP_SECONDS
                System.arraycopy(history, 1, history, 0, slots - 1)
                history[slots - 1] = smooth
            }
            scroll = carried / STEP_SECONDS
        }
    }
    Canvas(modifier = modifier) {
        val gap = size.width / (slots - 1)
        val dot = 2.2.dp.toPx()
        val maxHeight = size.height
        val centre = size.height / 2f
        for (i in 0 until slots) {
            val x = (i - scroll) * gap
            if (x < -gap || x > size.width + gap) continue
            val edge = (minOf(x, size.width - x) / (size.width * 0.18f)).coerceIn(0f, 1f)
            val amount = history[i]
            val height = dot + amount * (maxHeight - dot)
            drawRoundRect(
                color = color.copy(alpha = (0.35f + 0.55f * amount) * edge),
                topLeft = Offset(x - dot / 2f, centre - height / 2f),
                size = Size(dot, height),
                cornerRadius = CornerRadius(dot / 2f),
            )
        }
    }
}

private const val STEP_SECONDS = 0.045f
