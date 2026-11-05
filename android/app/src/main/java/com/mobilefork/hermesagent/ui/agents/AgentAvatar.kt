package com.mobilefork.hermesagent.ui.agents

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.ui.shell.ShellAgentAvatar
import com.mobilefork.hermesagent.ui.theme.hermesAvatarPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

/** The main agent's A mark, an uploaded picture, or a face generated from the agent's name. */
@Composable
fun AgentAvatar(
    agent: AgentProfile,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    if (agent.isMain) {
        ShellAgentAvatar(size = size, modifier = modifier)
        return
    }
    var picture by remember(agent.avatarPath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(agent.avatarPath) {
        if (agent.avatarPath.isBlank()) return@LaunchedEffect
        picture = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(agent.avatarPath)?.asImageBitmap() }.getOrNull()
        }
    }
    Box(modifier = modifier.size(size).clip(CircleShape)) {
        val current = picture
        if (current != null) {
            Image(
                bitmap = current,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AgentFace(name = agent.name)
        }
    }
}

/** Same name, same face: the look comes only from a hash of the name. */
@Composable
private fun AgentFace(name: String) {
    val seed = remember(name) { name.trim().lowercase().hashCode().absoluteValue }
    val background = hermesAvatarPalette[seed % hermesAvatarPalette.size]
    val eyeSpread = 0.15f + (seed % 5) * 0.012f
    val eyeHeight = 0.075f + ((seed / 5) % 3) * 0.018f
    val smileWidth = 0.20f + ((seed / 15) % 4) * 0.02f
    val smileDepth = 18f + ((seed / 60) % 4) * 12f
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        drawRect(background)
        val eyeWidth = w * 0.075f
        listOf(-1f, 1f).forEach { side ->
            drawOval(
                color = Color.White,
                topLeft = Offset(w * (0.5f + side * eyeSpread) - eyeWidth / 2, w * 0.40f - w * eyeHeight / 2),
                size = Size(eyeWidth, w * eyeHeight),
            )
        }
        drawArc(
            color = Color.White,
            startAngle = 90f - smileDepth * 1.5f,
            sweepAngle = smileDepth * 3f,
            useCenter = false,
            topLeft = Offset(w * (0.5f - smileWidth), w * 0.40f),
            size = Size(w * smileWidth * 2, w * 0.28f),
            style = Stroke(width = w * 0.055f, cap = StrokeCap.Round),
        )
    }
}
