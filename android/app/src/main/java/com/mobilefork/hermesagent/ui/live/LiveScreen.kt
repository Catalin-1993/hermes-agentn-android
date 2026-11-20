package com.mobilefork.hermesagent.ui.live

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.ui.calls.CallPhase
import com.mobilefork.hermesagent.ui.calls.VoiceCallController
import com.mobilefork.hermesagent.ui.calls.VoiceCallState
import com.mobilefork.hermesagent.ui.chat.ChatViewModel
import com.mobilefork.hermesagent.ui.chat.LiveWaveform
import com.mobilefork.hermesagent.ui.i18n.CallStrings
import com.mobilefork.hermesagent.ui.i18n.LocalCallStrings
import kotlinx.coroutines.delay

private val LiveBlack = Color(0xFF05070A)
private val LiveEnd = Color(0xFFE5453B)

/**
 * A live conversation with an agent over the whole app: talk, it answers out loud, talk again, with
 * no tapping. It runs in the agent's own chat, so the conversation is kept there in writing.
 */
@Composable
internal fun LiveModeHost(
    agent: AgentProfile,
    chat: ChatViewModel,
    languageTag: String,
    onClose: () -> Unit,
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
        VoiceCallController(context, chat, agent, languageTag, words, live = true) { }
    }
    LaunchedEffect(micGranted) { if (micGranted) controller.start() }
    DisposableEffect(controller) { onDispose { controller.hangUp() } }
    BackHandler(enabled = true) { controller.hangUp() }

    val state by controller.state.collectAsState()
    LaunchedEffect(state.phase) {
        if (state.phase == CallPhase.Ended) {
            delay(250)
            onClose()
        }
    }
    LiveScreen(
        state = if (!micGranted && micAsked) state.copy(error = words.micNeeded) else state,
        level = { controller.level.value },
        words = words,
        onMute = controller::toggleMute,
        onSpeaker = controller::toggleSpeaker,
        onEnd = { if (micGranted) controller.hangUp() else onClose() },
    )
}

@Composable
private fun LiveScreen(
    state: VoiceCallState,
    level: () -> Float,
    words: CallStrings,
    onMute: () -> Unit,
    onSpeaker: () -> Unit,
    onEnd: () -> Unit,
) {
    var captionsOn by rememberSaveable { mutableStateOf(true) }
    val glow by animateColorAsState(
        when {
            state.muted -> Color(0xFF3A4048)
            state.phase == CallPhase.Thinking -> Color(0xFF6F52F0)
            state.phase == CallPhase.Speaking -> Color(0xFF2FD9A6)
            else -> Color(0xFF2F8BFF)
        },
        tween(900),
        label = "liveGlow",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LiveBlack)
            .background(Brush.verticalGradient(0f to glow.copy(alpha = 0.30f), 0.55f to Color.Transparent))
            .testTag("HermesLiveScreen"),
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundButton(R.drawable.ic_call_volume, words.speaker, state.speakerOn, 46, "HermesLiveSpeaker", onSpeaker)
                RoundButton(R.drawable.ic_call_captions, words.captions, captionsOn, 46, "HermesLiveCaptions") { captionsOn = !captionsOn }
            }
            Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                LiveOrb(
                    phase = state.phase,
                    muted = state.muted,
                    level = level,
                    modifier = Modifier.size(250.dp).testTag("HermesLiveOrb"),
                )
            }
            Text(
                text = when {
                    state.error.isNotBlank() -> ""
                    state.muted -> words.liveMuted
                    state.phase == CallPhase.Listening -> if (state.heard.isBlank()) words.liveHint else words.listening
                    state.phase == CallPhase.Thinking -> words.thinking
                    state.phase == CallPhase.Speaking -> words.speaking
                    else -> ""
                },
                modifier = Modifier.fillMaxWidth().height(24.dp).testTag("HermesLiveStatus"),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.55f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            val speaking = state.phase == CallPhase.Speaking
            val heard = state.phase == CallPhase.Listening || state.phase == CallPhase.Thinking
            val caption = when {
                state.error.isNotBlank() -> state.error
                heard && state.heard.isNotBlank() -> state.heard
                else -> state.reply
            }
            val shown = if (speaking) rememberPacedText(caption) else caption
            val scroll = rememberScrollState()
            LaunchedEffect(shown.length / 24) { scroll.animateScrollTo(scroll.maxValue) }
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 26.dp)) {
                if (captionsOn || state.error.isNotBlank()) {
                    Text(
                        text = shown,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scroll)
                            .alpha(if (state.error.isBlank() && heard && state.heard.isBlank()) 0.5f else 1f)
                            .testTag("HermesLiveCaption"),
                        fontSize = 22.sp,
                        lineHeight = 31.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (state.error.isNotBlank()) Color(0xFFFFB4A8) else if (heard) Color.White.copy(alpha = 0.72f) else Color.White,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RoundButton(
                    if (state.muted) R.drawable.ic_call_mic_off else R.drawable.ic_call_mic,
                    if (state.muted) words.unmute else words.mute,
                    state.muted, 56, "HermesLiveMute", onMute,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LiveWaveform(
                        level = if (state.muted || state.phase != CallPhase.Listening) ({ 0f }) else level,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth().height(30.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(LiveEnd)
                        .clickable(role = Role.Button, onClick = onEnd)
                        .semantics { contentDescription = words.liveEnd }
                        .testTag("HermesLiveEnd"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_action_close), contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

@Composable
private fun RoundButton(icon: Int, label: String, active: Boolean, size: Int, tag: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (active) Color.White else Color.White.copy(alpha = 0.12f))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = if (active) LiveBlack else Color.White,
            modifier = Modifier.size((size * 0.46f).dp),
        )
    }
}

/** The agent's words arrive at about the pace of speech, instead of all at once. */
@Composable
private fun rememberPacedText(full: String): String {
    var count by remember { mutableStateOf(0) }
    LaunchedEffect(full) {
        // A new, different reply starts again; a longer version of the same one carries on.
        if (count > full.length) count = 0
        var last = withFrameNanos { it }
        var carry = 0f
        while (count < full.length) {
            val now = withFrameNanos { it }
            carry += (now - last) / 1_000_000_000f * CHARS_PER_SECOND
            last = now
            val whole = carry.toInt()
            if (whole > 0) {
                carry -= whole
                count = minOf(full.length, count + whole)
            }
        }
    }
    return full.take(count)
}

private const val CHARS_PER_SECOND = 24f
