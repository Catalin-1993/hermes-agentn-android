package com.mobilefork.hermesagent.ui.agents

import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mobilefork.hermesagent.data.AvatarChoice
import com.mobilefork.hermesagent.data.avatarChoiceOf
import com.mobilefork.hermesagent.data.avatarInitials
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.unit.dp
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
    val choice = remember(agent.avatarPath) { avatarChoiceOf(agent.avatarPath) }
    if (agent.isMain && choice == AvatarChoice.Generated) {
        ShellAgentAvatar(size = size, modifier = modifier)
        return
    }
    var picture by remember(agent.avatarPath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(agent.avatarPath) {
        val photo = choice as? AvatarChoice.Photo ?: return@LaunchedEffect
        picture = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(photo.path)?.asImageBitmap() }.getOrNull()
        }
    }
    Box(modifier = modifier.size(size).clip(CircleShape)) {
        val current = picture
        when {
            current != null -> Image(
                bitmap = current,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            choice is AvatarChoice.Preset -> Image(
                painter = painterResource(choice.preset.drawable),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            choice is AvatarChoice.Initials -> AgentInitials(name = agent.name, size = size)
            else -> AgentFace(name = agent.name)
        }
    }
}

/** One or two letters on the agent's own colour, the same colour its automatic face uses. */
@Composable
private fun AgentInitials(name: String, size: Dp) {
    val seed = remember(name) { name.trim().lowercase().hashCode().absoluteValue }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(hermesAvatarPalette[seed % hermesAvatarPalette.size]),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = avatarInitials(name),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.38f).sp,
            maxLines = 1,
        )
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

/** A group's picture, or its first members' avatars overlapping inside the same size. */
@Composable
fun GroupAvatar(
    members: List<AgentProfile>,
    avatarPath: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    if (avatarPath.isNotBlank()) {
        AgentAvatar(
            agent = AgentProfile(id = "group", name = "group", avatarPath = avatarPath),
            size = size,
            modifier = modifier,
        )
        return
    }
    val shown = members.take(2)
    Box(modifier = modifier.size(size)) {
        val small = size * 0.66f
        when (shown.size) {
            0 -> Unit
            1 -> AgentAvatar(agent = shown[0], size = size)
            else -> {
                AgentAvatar(agent = shown[0], size = small, modifier = Modifier.align(Alignment.TopStart))
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(small)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                ) {
                    AgentAvatar(agent = shown[1], size = small)
                }
            }
        }
    }
}
