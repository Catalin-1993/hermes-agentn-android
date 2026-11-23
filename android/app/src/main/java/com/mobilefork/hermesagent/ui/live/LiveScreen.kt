package com.mobilefork.hermesagent.ui.live

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import com.mobilefork.hermesagent.ui.theme.HermesBackdrop
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import com.mobilefork.hermesagent.ui.theme.hermesGlassSurface
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.content.ActivityNotFoundException
import android.net.Uri
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
import java.io.File

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

    // A photo instead of video: the camera app takes one, it is attached to the chat, and it goes out
    // with the next thing the person says. Listening and speaking wait while the camera is in front.
    val chatState by chat.uiState.collectAsState()
    val photos = chatState.attachments.filter { it.mimeType.startsWith("image/") }
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraNote by remember { mutableStateOf("") }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val file = pendingPhotoPath?.let(::File)
        pendingPhotoPath = null
        if (taken && file != null && file.length() > 0L) {
            chat.attachImage(Uri.fromFile(file).toString())
            cameraNote = ""
        } else {
            file?.delete()
        }
        controller.resume()
    }
    fun openCamera() {
        val directory = File(context.cacheDir, "hermes-camera").apply { mkdirs() }
        val file = File(directory, "live-${System.currentTimeMillis()}.jpg")
        controller.pause()
        runCatching {
            val target = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            pendingPhotoPath = file.absolutePath
            cameraLauncher.launch(target)
        }.onFailure { error ->
            pendingPhotoPath = null
            cameraNote = if (error is ActivityNotFoundException) words.liveNoCamera else words.liveCameraDenied
            controller.resume()
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openCamera() else cameraNote = words.liveCameraDenied
    }
    fun startCamera() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) openCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
    }

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
        photoUris = photos.map { it.uri },
        note = cameraNote,
        onCamera = ::startCamera,
        onRemovePhoto = chat::removeAttachment,
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
    photoUris: List<String>,
    note: String,
    onCamera: () -> Unit,
    onRemovePhoto: (String) -> Unit,
    onMute: () -> Unit,
    onSpeaker: () -> Unit,
    onEnd: () -> Unit,
) {
    var captionsOn by rememberSaveable { mutableStateOf(true) }
    val scheme = MaterialTheme.colorScheme
    val speaking = state.phase == CallPhase.Speaking
    val heard = state.phase == CallPhase.Listening || state.phase == CallPhase.Thinking
    val caption = when {
        state.error.isNotBlank() -> state.error
        heard && state.heard.isNotBlank() -> state.heard
        else -> state.reply
    }
    val shown = if (speaking) rememberPacedText(caption) else caption
    val hasText = shown.isNotBlank() && (captionsOn || state.error.isNotBlank())
    // The orb gives the words room: it settles into the top and shrinks as soon as there is text to read.
    val orbSize by animateDpAsState(
        targetValue = if (hasText) 128.dp else 264.dp,
        animationSpec = spring(dampingRatio = 0.92f, stiffness = Spring.StiffnessLow),
        label = "liveOrbSize",
    )
    val tint by animateColorAsState(
        when {
            state.muted -> scheme.onSurfaceVariant
            state.phase == CallPhase.Thinking -> scheme.tertiary
            speaking -> LocalHermesGlassTokens.current.success
            else -> scheme.primary
        },
        tween(900),
        label = "liveTint",
    )
    HermesBackdrop(modifier = Modifier.testTag("HermesLiveScreen")) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0f to tint.copy(alpha = 0.14f), 0.5f to Color.Transparent)),
        )
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundButton(R.drawable.ic_call_volume, words.speaker, state.speakerOn, 44, "HermesLiveSpeaker", onSpeaker)
                RoundButton(R.drawable.ic_call_captions, words.captions, captionsOn, 44, "HermesLiveCaptions") { captionsOn = !captionsOn }
            }
            Box(modifier = Modifier.fillMaxWidth().height(orbSize + 8.dp), contentAlignment = Alignment.Center) {
                LiveOrb(
                    phase = state.phase,
                    muted = state.muted,
                    level = level,
                    modifier = Modifier.size(orbSize).testTag("HermesLiveOrb"),
                )
            }
            Text(
                text = when {
                    state.error.isNotBlank() -> ""
                    state.muted -> words.liveMuted
                    state.phase == CallPhase.Listening -> if (state.heard.isBlank()) words.liveHint else words.listening
                    state.phase == CallPhase.Thinking -> words.thinking
                    speaking -> words.speaking
                    else -> ""
                },
                modifier = Modifier.fillMaxWidth().height(22.dp).testTag("HermesLiveStatus"),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onBackground.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            val scroll = rememberScrollState()
            LaunchedEffect(shown.length / 24) { scroll.animateScrollTo(scroll.maxValue) }
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp)) {
                if (hasText) {
                    val base = when {
                        state.error.isNotBlank() -> scheme.error
                        heard -> scheme.onBackground.copy(alpha = 0.70f)
                        else -> scheme.onBackground
                    }
                    Text(
                        text = rememberFadedTail(shown, base),
                        modifier = Modifier
                            .fillMaxSize()
                            .fadingEdges()
                            .verticalScroll(scroll)
                            .padding(vertical = 18.dp)
                            .testTag("HermesLiveCaption"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 32.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (photoUris.isNotEmpty() || note.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    photoUris.forEach { uri ->
                        Box(modifier = Modifier.size(64.dp).testTag("HermesLivePhoto")) {
                            coil.compose.AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .clickable(role = Role.Button) { onRemovePhoto(uri) }
                                    .semantics { contentDescription = words.liveRemovePhoto },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(painterResource(R.drawable.ic_action_close), contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                    Text(
                        text = if (note.isNotBlank()) note else words.livePhotoReady,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (note.isNotBlank()) scheme.error else scheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RoundButton(R.drawable.ic_live_camera, words.liveCamera, photoUris.isNotEmpty(), 54, "HermesLiveCamera", onCamera)
                RoundButton(
                    if (state.muted) R.drawable.ic_call_mic_off else R.drawable.ic_call_mic,
                    if (state.muted) words.unmute else words.mute,
                    state.muted, 54, "HermesLiveMute", onMute,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .hermesGlassSurface(RoundedCornerShape(27.dp), shadowRadius = 6.dp)
                        .padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LiveWaveform(
                        level = if (state.muted || state.phase != CallPhase.Listening) ({ 0f }) else level,
                        color = scheme.primary,
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(LiveEnd)
                        .clickable(role = Role.Button, onClick = onEnd)
                        .semantics { contentDescription = words.liveEnd }
                        .testTag("HermesLiveEnd"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_action_close), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun RoundButton(icon: Int, label: String, active: Boolean, size: Int, tag: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(size.dp)
            .then(
                if (active) Modifier.clip(CircleShape).background(scheme.primary)
                else Modifier.hermesGlassSurface(CircleShape, shadowRadius = 6.dp).clip(CircleShape),
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = if (active) scheme.onPrimary else scheme.onSurface,
            modifier = Modifier.size((size * 0.44f).dp),
        )
    }
}

/**
 * The reply as it arrives: the settled words in full colour and the last few still coming in, each a
 * little fainter, so the text seems to materialise rather than appear.
 */
@Composable
private fun rememberFadedTail(text: String, color: Color): androidx.compose.ui.text.AnnotatedString =
    remember(text, color) {
        val steps = floatArrayOf(0.85f, 0.62f, 0.38f)
        val parts = Regex("\\S+\\s*").findAll(text).toList()
        androidx.compose.ui.text.buildAnnotatedString {
            val settled = parts.dropLast(steps.size)
            val tail = parts.takeLast(steps.size)
            settled.forEach { append(it.value) }
            tail.forEachIndexed { index, word ->
                // The newest word is the faintest.
                val factor = steps[steps.size - tail.size + index]
                withStyle(androidx.compose.ui.text.SpanStyle(color = color.copy(alpha = color.alpha * factor))) { append(word.value) }
            }
        }
    }

/** The top and bottom of a scrolling text melt away instead of being cut off. */
private fun Modifier.fadingEdges(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.07f to Color.Black,
                0.93f to Color.Black,
                1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
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
