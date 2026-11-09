package com.mobilefork.hermesagent.ui.calls

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.ui.agents.AgentAvatar
import com.mobilefork.hermesagent.ui.chat.ChatViewModel
import com.mobilefork.hermesagent.ui.i18n.CallStrings
import com.mobilefork.hermesagent.ui.i18n.LocalCallStrings
import kotlinx.coroutines.delay

private val CallTop = Color(0xFF1B2733)
private val CallBottom = Color(0xFF080C10)
private val CallEnd = Color(0xFFE5453B)

/**
 * A call with an agent, over the whole app: the agent's face, its name, the time, and the same round
 * controls as a phone. It starts when it opens and ends when it closes.
 */
@Composable
internal fun AgentCallHost(
    agent: AgentProfile,
    chat: ChatViewModel,
    languageTag: String,
    onClose: () -> Unit,
    onCallLogged: () -> Unit,
) {
    val context = LocalContext.current
    val words = LocalCallStrings.current
    var micGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var micAsked by remember { mutableStateOf(false) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micGranted = granted
        micAsked = true
    }
    LaunchedEffect(Unit) { if (!micGranted) micLauncher.launch(Manifest.permission.RECORD_AUDIO) }

    val controller = remember(agent.id) {
        VoiceCallController(context, chat, agent, languageTag, words) { record ->
            CallLogStore(context).add(record)
            onCallLogged()
        }
    }
    LaunchedEffect(micGranted) { if (micGranted) controller.start() }
    DisposableEffect(controller) { onDispose { controller.hangUp() } }
    BackHandler(enabled = true) { controller.hangUp() }

    val state by controller.state.collectAsState()
    LaunchedEffect(state.phase) {
        if (state.phase == CallPhase.Ended) {
            delay(700)
            onClose()
        }
    }
    val shown = if (!micGranted && micAsked) state.copy(error = words.micNeeded) else state
    CallScreen(
        agent = agent,
        state = shown,
        words = words,
        onMute = controller::toggleMute,
        onSpeaker = controller::toggleSpeaker,
        onEnd = {
            if (micGranted) controller.hangUp() else onClose()
        },
    )
}

@Composable
internal fun CallScreen(
    agent: AgentProfile,
    state: VoiceCallState,
    words: CallStrings,
    onMute: () -> Unit,
    onSpeaker: () -> Unit,
    onEnd: () -> Unit,
) {
    var captionsOn by rememberSaveable { mutableStateOf(true) }
    val ringing = state.phase == CallPhase.Ringing
    val talking = state.phase == CallPhase.Speaking
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CallTop, CallBottom)))
            .testTag("HermesCallScreen"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            Text(
                text = agent.name,
                modifier = Modifier.testTag("HermesCallName"),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    state.phase == CallPhase.Ended -> words.callEnded
                    ringing -> words.callingStatus
                    else -> formatTimer(state.seconds)
                },
                modifier = Modifier.testTag("HermesCallStatus"),
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.78f),
            )
            Text(
                text = when {
                    state.muted && state.phase != CallPhase.Ended && !ringing -> words.micOff
                    state.phase == CallPhase.Listening -> words.listening
                    state.phase == CallPhase.Thinking -> words.thinking
                    state.phase == CallPhase.Speaking -> words.speaking
                    else -> ""
                },
                modifier = Modifier.height(24.dp).testTag("HermesCallPhase"),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.55f),
            )
            Spacer(Modifier.height(36.dp))
            Box(contentAlignment = Alignment.Center) {
                if (ringing || talking) PulseRing()
                AgentAvatar(agent = agent, size = 168.dp)
            }
            Spacer(Modifier.height(24.dp))
            val caption = when {
                state.error.isNotBlank() -> state.error
                state.phase == CallPhase.Listening || state.phase == CallPhase.Thinking -> state.heard
                state.phase == CallPhase.Speaking -> state.reply
                else -> ""
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                if (captionsOn || state.error.isNotBlank()) {
                    Text(
                        text = caption,
                        modifier = Modifier.padding(horizontal = 8.dp).testTag("HermesCallCaption"),
                        fontSize = 17.sp,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center,
                        color = if (state.error.isNotBlank()) Color(0xFFFFB4A8) else Color.White.copy(alpha = 0.88f),
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                CallControl(
                    icon = if (state.muted) R.drawable.ic_call_mic_off else R.drawable.ic_call_mic,
                    label = if (state.muted) words.unmute else words.mute,
                    active = state.muted,
                    tag = "HermesCallMute",
                    onClick = onMute,
                )
                CallControl(
                    icon = R.drawable.ic_call_volume,
                    label = words.speaker,
                    active = state.speakerOn,
                    tag = "HermesCallSpeaker",
                    onClick = onSpeaker,
                )
                CallControl(
                    icon = R.drawable.ic_call_captions,
                    label = words.captions,
                    active = captionsOn,
                    tag = "HermesCallCaptions",
                    onClick = { captionsOn = !captionsOn },
                )
            }
            Box(
                modifier = Modifier
                    .padding(bottom = 36.dp)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(CallEnd)
                    .clickable(role = Role.Button, onClick = onEnd)
                    .semantics { contentDescription = words.endCall }
                    .testTag("HermesCallEnd"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_call_end), contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
        }
    }
}

@Composable
private fun PulseRing() {
    val transition = rememberInfiniteTransition(label = "callPulse")
    val grow by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "pulseScale",
    )
    val fade by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "pulseFade",
    )
    Box(
        modifier = Modifier
            .size(168.dp)
            .scale(grow)
            .alpha(fade)
            .background(Color.White, CircleShape),
    )
}

@Composable
private fun CallControl(icon: Int, label: String, active: Boolean, tag: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (active) Color.White else Color.White.copy(alpha = 0.14f))
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label }
                .testTag(tag),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (active) CallBottom else Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
        Text(text = label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.78f), maxLines = 1)
    }
}

internal fun formatTimer(seconds: Int): String =
    "%02d:%02d".format(seconds / 60, seconds % 60)
