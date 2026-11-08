package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SelectedAttachment
import com.example.ui.components.softGreyShadow
import com.example.ui.theme.HermesAppThemeMode
import com.example.ui.theme.LocalHermesTheme
import kotlin.math.max

private val DiagonalExpandIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "DiagonalExpand",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            // Top-right arrowhead (↗)
            moveTo(21f, 3f)
            lineTo(14f, 3f)
            lineTo(14f, 5f)
            lineTo(18.59f, 5f)
            lineTo(13.17f, 10.41f)
            lineTo(14.59f, 11.83f)
            lineTo(20f, 6.41f)
            lineTo(20f, 11f)
            lineTo(22f, 11f)
            lineTo(22f, 3f)
            close()
            // Bottom-left arrowhead (↙)
            moveTo(3f, 21f)
            lineTo(10f, 21f)
            lineTo(10f, 19f)
            lineTo(5.41f, 19f)
            lineTo(10.83f, 13.59f)
            lineTo(9.41f, 12.17f)
            lineTo(4f, 17.59f)
            lineTo(4f, 13f)
            lineTo(2f, 13f)
            lineTo(2f, 21f)
            close()
        }
    }.build()
}

/**
 * Unified, discreet gray chat bottom bar used consistently across the application.
 * Sits cleanly above the system navigation bar with subtle margins, neutral gray styling,
 * attachment preview, slash commands carousel, and action buttons (+, 🧠, 🎤, 〰️, ➤).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatBottomBar(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    placeholderText: String = "Type to ask...",
    activeModelName: String = "",
    onSelectModelClick: () -> Unit = {},
    currentAttachment: SelectedAttachment? = null,
    onClearAttachment: () -> Unit = {},
    onOpenAttachments: () -> Unit = {},
    onOpenModelSkills: () -> Unit = {},
    onOpenVoice: () -> Unit = {},
    onMicrophoneClick: () -> Unit = onOpenVoice,
    onLiveVoiceClick: () -> Unit = onOpenVoice,
    isVoiceRecording: Boolean = false,
    voiceIntensity: Float = 0f,
    voicePartialText: String = "",
    onCancelVoiceRecording: () -> Unit = {},
    onConfirmVoiceRecording: () -> Unit = {},
    onSendVoiceRecording: () -> Unit = {},
    isLoading: Boolean = false,
    onStopGeneration: () -> Unit = {},
    onSlashCommandClick: ((String) -> Unit)? = null,
    horizontalPadding: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible
    var isFocused by remember { mutableStateOf(false) }
    val isInputActive = isFocused || isImeVisible || inputText.isNotEmpty()
    val isEffectActive = isInputActive
    val theme = LocalHermesTheme.current

    val intensityHistory = remember { mutableStateListOf<Float>().apply { repeat(34) { add(0f) } } }
    LaunchedEffect(isVoiceRecording) {
        if (!isVoiceRecording) {
            intensityHistory.clear()
            repeat(34) { intensityHistory.add(0f) }
        }
    }
    LaunchedEffect(voiceIntensity) {
        if (isVoiceRecording) {
            if (intensityHistory.size >= 34) {
                intensityHistory.removeAt(0)
            }
            intensityHistory.add(voiceIntensity)
        }
    }

    var isFullScreenOpen by remember { mutableStateOf(false) }

    val inputSurfaceColor by animateColorAsState(
        targetValue = when (theme.themeMode) {
            HermesAppThemeMode.LIGHT -> Color(0xFFEEEEEE)
            HermesAppThemeMode.CHATGPT_DARK -> if (isInputActive) Color(0xD8383838) else Color(0xD62F2F2F)
            HermesAppThemeMode.EMERALD_ORIGINAL -> if (isInputActive) Color(0xE0112A1F) else Color(0xD80D2219)
        },
        label = "input_surface_color"
    )

    val dynamicAccent = theme.accent
    val dynamicPrimary = theme.primary

    val infiniteTransition = rememberInfiniteTransition(label = "bottom_bar_moving_border_transition")
    
    // Balanced, elegant 360-degree rotation around the input bar perimeter (smooth 5.5s loop)
    val borderAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_angle"
    )

    // Soft breathing aura glow multiplier (discreet, not overly prominent)
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    // Smooth, slow fluid wave phase for voice visualization
    val voiceWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "voice_wave_phase"
    )

    val sweepGradientBrush = remember(dynamicAccent, dynamicPrimary, theme.themeMode, pulseGlow) {
        when (theme.themeMode) {
            HermesAppThemeMode.LIGHT -> {
                // Light Mode: Subtle transparent gradient without blue or turquoise
                Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent
                    )
                )
            }
            HermesAppThemeMode.EMERALD_ORIGINAL -> {
                // Emerald Nocturne: Neon Mint, Emerald, Jade Green
                val emerald = Color(0xFF10B981)
                val deep = Color(0xFF00A67E)
                val mint = Color(0xFF34D399)
                val darkMint = Color(0xFF059669)
                Brush.sweepGradient(
                    colors = listOf(
                        emerald.copy(alpha = 0.70f * pulseGlow),
                        deep.copy(alpha = 0.45f * pulseGlow),
                        Color.Transparent,
                        mint.copy(alpha = 0.35f * pulseGlow),
                        darkMint.copy(alpha = 0.60f * pulseGlow),
                        emerald.copy(alpha = 0.40f * pulseGlow),
                        Color.Transparent,
                        emerald.copy(alpha = 0.70f * pulseGlow)
                    )
                )
            }
            HermesAppThemeMode.CHATGPT_DARK -> {
                // ChatGPT Dark: Dynamic Accent, Slate & Zinc highlights
                val accent = dynamicAccent
                val zinc = Color(0xFF71717A)
                val slate = Color(0xFF52525B)
                val emeraldSoft = Color(0xFF34D399)
                Brush.sweepGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.65f * pulseGlow),
                        zinc.copy(alpha = 0.40f * pulseGlow),
                        Color.Transparent,
                        slate.copy(alpha = 0.25f * pulseGlow),
                        emeraldSoft.copy(alpha = 0.55f * pulseGlow),
                        accent.copy(alpha = 0.35f * pulseGlow),
                        Color.Transparent,
                        accent.copy(alpha = 0.65f * pulseGlow)
                    )
                )
            }
        }
    }

    val isImeOpen = WindowInsets.isImeVisible

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                bottom = if (isImeOpen) 2.dp else 4.dp,
                top = 2.dp
            )
    ) {
        // Attachment preview bar if attachment selected
        if (currentAttachment != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (theme.isLight) Color(0xFFF1F5F9) else Color(0xFF26282C),
                border = BorderStroke(1.dp, if (theme.isLight) Color(0xFFCBD5E1) else Color(0xFF383B40)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (currentAttachment.type) {
                                "IMAGE" -> Icons.Default.Image
                                "DOCUMENT" -> Icons.Default.Description
                                "CAMERA" -> Icons.Default.PhotoCamera
                                else -> Icons.Default.AttachFile
                            },
                            contentDescription = null,
                            tint = if (theme.isLight) Color(0xFF64748B) else Color(0xFF9CA3AF),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = currentAttachment.name,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = if (theme.isLight) Color(0xFF1E293B) else Color(0xFFE5E7EB),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 240.dp)
                        )
                    }
                    IconButton(
                        onClick = onClearAttachment,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Șterge atașamentul",
                            tint = if (theme.isLight) Color(0xFF64748B) else Color(0xFF9CA3AF),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Quick Slash Commands Carousel (Shown ONLY when typing '/')
        AnimatedVisibility(
            visible = inputText.startsWith("/") || inputText.contains("/"),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChatQuickChip(label = "🔍 /caută", onClick = { onInputTextChange("/caută ") })
                ChatQuickChip(label = "💻 /cod", onClick = { onInputTextChange("/cod ") })
                ChatQuickChip(label = "🧠 /memorie", onClick = { onInputTextChange("/memorie ") })
                ChatQuickChip(label = "📋 /plan", onClick = { onInputTextChange("/plan ") })
                ChatQuickChip(label = "⚡ /termux", onClick = { onInputTextChange("/termux ") })
                ChatQuickChip(label = "📊 /analiză", onClick = { onInputTextChange("/analiză ") })
                if (onSlashCommandClick != null) {
                    ChatQuickChip(label = "🌐 /web", onClick = { onSlashCommandClick("web") })
                    ChatQuickChip(label = "📊 /dashboard", onClick = { onSlashCommandClick("dashboard") })
                }
            }
        }

        val outerPillShape = RoundedCornerShape(28.dp)
        val innerPillShape = RoundedCornerShape(28.dp)

        val baseBorderColor = when (theme.themeMode) {
            HermesAppThemeMode.LIGHT -> Color.Transparent
            HermesAppThemeMode.CHATGPT_DARK -> if (isInputActive) Color(0x55484848) else Color(0x35383838)
            HermesAppThemeMode.EMERALD_ORIGINAL -> if (isInputActive) Color(0x55484848) else Color(0x35383838)
        }

        val shadowSpotColor = when (theme.themeMode) {
            HermesAppThemeMode.LIGHT -> Color(0x45000000)
            HermesAppThemeMode.CHATGPT_DARK -> dynamicAccent.copy(alpha = 0.35f)
            HermesAppThemeMode.EMERALD_ORIGINAL -> Color(0x5510B981)
        }
        val shadowAmbientColor = when (theme.themeMode) {
            HermesAppThemeMode.LIGHT -> Color(0x35000000)
            HermesAppThemeMode.CHATGPT_DARK -> Color(0x22000000)
            HermesAppThemeMode.EMERALD_ORIGINAL -> Color(0x33000000)
        }

        // Dedicated Single-Row Pill Capsule Input Container with Discreet Moving Border & Rich 360° Ambient Shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .softGreyShadow(
                    color = Color(0x42000000),
                    blur = 16.dp,
                    spread = 1.dp,
                    offsetY = 1.dp,
                    cornerRadius = 28.dp,
                    enabled = theme.isLight
                )
                .shadow(
                    elevation = if (theme.isLight) 14.dp else 8.dp,
                    shape = outerPillShape,
                    spotColor = shadowSpotColor,
                    ambientColor = shadowAmbientColor
                )
                .clip(outerPillShape)
                .background(baseBorderColor)
                .drawBehind {
                    if (!theme.isLight) {
                        rotate(borderAngle) {
                            drawRect(
                                brush = sweepGradientBrush,
                                topLeft = Offset(-size.width * 0.5f, -size.height * 0.5f),
                                size = Size(size.width * 2f, size.height * 2f)
                            )
                        }
                    }
                }
                .padding(if (theme.isLight) 0.dp else 1.3.dp)
                .clip(innerPillShape)
                .testTag("bottom_input_bar")
        ) {
            Surface(
                shape = innerPillShape,
                color = inputSurfaceColor,
                border = BorderStroke(1.dp, if (theme.isLight) Color(0x60FFFFFF) else Color(0x35FFFFFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
            ) {
            val currentDisplayValue = remember(inputText, voicePartialText, isVoiceRecording) {
                if (isVoiceRecording && voicePartialText.isNotBlank()) {
                    if (inputText.isBlank()) voicePartialText else "$inputText $voicePartialText"
                } else {
                    inputText
                }
            }

            var renderedLineCount by remember { mutableStateOf(1) }
            LaunchedEffect(currentDisplayValue) {
                if (currentDisplayValue.isEmpty()) {
                    renderedLineCount = 1
                }
            }
            val explicitLineCount = currentDisplayValue.count { it == '\n' } + 1
            val effectiveLineCount = maxOf(renderedLineCount, explicitLineCount)
            val isMultilineMode = effectiveLineCount > 1 || currentDisplayValue.contains('\n') || currentDisplayValue.length >= 45

            // Diagonal fullscreen expand button appears only AFTER 2 lines are already written (i.e. line 3+)
            val showFullScreenButton = effectiveLineCount > 2 || currentDisplayValue.length >= 85

            Layout(
                content = {
                    // Slot 0: Plus Button (+) for Attachments or Cancel (✕) when recording
                    Box(
                        modifier = Modifier.layoutId("plus_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isVoiceRecording) {
                            IconButton(
                                onClick = onCancelVoiceRecording,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("btn_cancel_voice_recording")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Anulează înregistrarea",
                                    tint = if (theme.isLight) Color(0xFF64748B) else Color(0xFFD1D5DB),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = onOpenAttachments,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("btn_open_attachments")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Atașează",
                                    tint = if (theme.isLight) Color(0xFF1A1A1A) else Color(0xFFD1D5DB),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Slot 1: Text Input Area (Single stable text field that maintains fixed container size)
                    Box(
                        modifier = Modifier
                            .layoutId("text_input")
                            .padding(
                                start = if (isMultilineMode) 10.dp else 2.dp,
                                end = if (isMultilineMode) 10.dp else 2.dp,
                                top = if (isMultilineMode) 4.dp else 2.dp,
                                bottom = if (isMultilineMode) 4.dp else 2.dp
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = currentDisplayValue,
                            onValueChange = { newValue ->
                                onInputTextChange(newValue)
                            },
                            onTextLayout = { textLayoutResult ->
                                renderedLineCount = textLayoutResult.lineCount
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = if (theme.isLight) Color(0xFF1A1A1A) else Color(0xFFF3F4F6),
                                fontSize = 15.sp,
                                lineHeight = 21.sp
                            ),
                            cursorBrush = SolidColor(if (theme.isLight) Color(0xFF1A1A1A) else theme.accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (currentDisplayValue.isEmpty()) {
                                        Text(
                                            text = if (isVoiceRecording) "Dictare în curs..." else "Message",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = if (isVoiceRecording) dynamicAccent else if (theme.isLight) Color(0xFF8E8E93) else Color(0xFF6B7280),
                                                fontSize = 15.sp
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 24.dp, max = 135.dp)
                                .verticalScroll(rememberScrollState())
                                .onFocusChanged { isFocused = it.isFocused }
                                .testTag("chat_input_textfield")
                        )
                    }

                    // Slot 2: Right Action Controls
                    Box(
                        modifier = Modifier.layoutId("actions_row"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isLoading) {
                                // Circular Stop Button (Black instead of red)
                                IconButton(
                                    onClick = onStopGeneration,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("btn_stop_generation")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF2A2A2A))
                                            .border(1.2.dp, if (theme.isLight) Color(0xFF1A1A1A) else Color(0xFFF1F5F9), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(if (theme.isLight) Color(0xFF1A1A1A) else Color(0xFFF1F5F9))
                                        )
                                    }
                                }
                            } else if (isVoiceRecording) {
                                // Live Waveform Visualizer + Stop & Keep / Direct Send Controls
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(end = 2.dp)
                                ) {
                                    // Animated Waveform Bars (10 bars)
                                    val inactiveBarColor = if (theme.isLight) Color(0xFF94A3B8) else Color(0xFF6B7280)
                                    val bars = intensityHistory.takeLast(10)
                                    val paddedBars = if (bars.size < 10) List(10 - bars.size) { 0f } + bars else bars
                                    Row(
                                        modifier = Modifier
                                            .width(48.dp)
                                            .height(20.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        paddedBars.forEachIndexed { index, histVal ->
                                            val harmonicSine = (kotlin.math.sin(voiceWavePhase + (index * 0.4f)).toFloat() * 1.2f + 2.5f)
                                            val rawHeight = if (histVal < 0.04f) harmonicSine else 3.0f + (histVal * 16f)
                                            val barHeight = rawHeight.coerceIn(2.5f, 18.0f)
                                            val animatedBarHeight by animateFloatAsState(
                                                targetValue = barHeight,
                                                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                                                label = "wave_bar"
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(2.5.dp)
                                                    .height(animatedBarHeight.dp)
                                                    .clip(RoundedCornerShape(1.dp))
                                                    .background(
                                                        if (histVal > 0.04f) dynamicAccent else inactiveBarColor.copy(alpha = 0.7f)
                                                    )
                                            )
                                        }
                                    }

                                    // Confirm and keep text button (✓)
                                    IconButton(
                                        onClick = onConfirmVoiceRecording,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .testTag("btn_stop_keep_text")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF334155))
                                                .border(1.dp, if (theme.isLight) Color(0xFFCBD5E1) else Color(0xFF475569), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Confirmă dictarea",
                                                tint = if (theme.isLight) Color(0xFF0F172A) else Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Direct Send button
                                    IconButton(
                                        onClick = onSendVoiceRecording,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .testTag("btn_send_voice_directly")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(dynamicAccent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowUpward,
                                                contentDescription = "Trimite direct",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Fullscreen Button: Shown ONLY after 2 full lines are already written
                                AnimatedVisibility(
                                    visible = showFullScreenButton,
                                    enter = fadeIn() + expandHorizontally(),
                                    exit = fadeOut() + shrinkHorizontally()
                                ) {
                                    IconButton(
                                        onClick = { isFullScreenOpen = true },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .testTag("btn_fullscreen_input")
                                    ) {
                                        Icon(
                                            imageVector = DiagonalExpandIcon,
                                            contentDescription = "Editare ecran complet",
                                            tint = if (theme.isLight) Color(0xFF8E8E93) else Color(0xFF9CA3AF),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }

                                // Persistent Microphone Action Button (ALWAYS VISIBLE when not loading)
                                IconButton(
                                    onClick = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        onMicrophoneClick()
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("btn_microphone")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Dictare vocală",
                                        tint = if (theme.isLight) Color(0xFF1A1A1A) else Color(0xFFE5E7EB),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Send Message Button (Appears next to Mic button when text is entered)
                                AnimatedVisibility(
                                    visible = currentDisplayValue.isNotBlank() || currentAttachment != null,
                                    enter = fadeIn() + expandHorizontally(),
                                    exit = fadeOut() + shrinkHorizontally()
                                ) {
                                    IconButton(
                                        onClick = {
                                            val trimmed = currentDisplayValue.trim()
                                            if (trimmed.isNotBlank() || currentAttachment != null) {
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                                onSendMessage(trimmed)
                                                onInputTextChange("")
                                                renderedLineCount = 1
                                            }
                                        },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .testTag("btn_send_message")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (theme.isLight) Color(0xFF1A1A1A) else dynamicAccent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowUpward,
                                                contentDescription = "Trimite mesaj",
                                                tint = Color.White,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { measurables, constraints ->
                    val plusMeasurable = measurables.first { it.layoutId == "plus_btn" }
                    val inputMeasurable = measurables.first { it.layoutId == "text_input" }
                    val actionsMeasurable = measurables.first { it.layoutId == "actions_row" }

                    val maxWidth = constraints.maxWidth
                    val horizontalMargin = 6.dp.roundToPx()
                    val topPadding = 6.dp.roundToPx()
                    val bottomPadding = 6.dp.roundToPx()

                    if (isMultilineMode) {
                        // Multiline ChatGPT Style: Text Field above the action buttons
                        val inputPlaceable = inputMeasurable.measure(
                            constraints.copy(
                                minWidth = maxWidth,
                                maxWidth = maxWidth,
                                minHeight = 0
                            )
                        )
                        val plusPlaceable = plusMeasurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                        val actionsPlaceable = actionsMeasurable.measure(constraints.copy(minWidth = 0, minHeight = 0))

                        val bottomRowHeight = maxOf(plusPlaceable.height, actionsPlaceable.height)
                        val totalHeight = topPadding + inputPlaceable.height + 4.dp.roundToPx() + bottomRowHeight + bottomPadding

                        layout(maxWidth, totalHeight) {
                            // Text field placed at the top
                            inputPlaceable.placeRelative(0, topPadding)

                            // Bottom actions row (Plus on the left, Actions on the right)
                            val bottomRowY = topPadding + inputPlaceable.height + 4.dp.roundToPx()
                            plusPlaceable.placeRelative(
                                horizontalMargin,
                                bottomRowY + (bottomRowHeight - plusPlaceable.height) / 2
                            )
                            actionsPlaceable.placeRelative(
                                maxWidth - horizontalMargin - actionsPlaceable.width,
                                bottomRowY + (bottomRowHeight - actionsPlaceable.height) / 2
                            )
                        }
                    } else {
                        // Single-line Mode: Plus (+) on left, Text in center, Actions on right
                        val plusPlaceable = plusMeasurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                        val actionsPlaceable = actionsMeasurable.measure(constraints.copy(minWidth = 0, minHeight = 0))

                        val availableInputWidth = (maxWidth - plusPlaceable.width - actionsPlaceable.width - (horizontalMargin * 2) - 8.dp.roundToPx()).coerceAtLeast(0)
                        val inputPlaceable = inputMeasurable.measure(
                            constraints.copy(
                                minWidth = 0,
                                maxWidth = availableInputWidth,
                                minHeight = 0
                            )
                        )

                        val totalHeight = maxOf(
                            plusPlaceable.height,
                            inputPlaceable.height,
                            actionsPlaceable.height,
                            48.dp.roundToPx()
                        ) + (topPadding + bottomPadding)

                        layout(maxWidth, totalHeight) {
                            val centerY = totalHeight / 2
                            plusPlaceable.placeRelative(
                                horizontalMargin,
                                centerY - (plusPlaceable.height / 2)
                            )
                            inputPlaceable.placeRelative(
                                horizontalMargin + plusPlaceable.width + 4.dp.roundToPx(),
                                centerY - (inputPlaceable.height / 2)
                            )
                            actionsPlaceable.placeRelative(
                                maxWidth - horizontalMargin - actionsPlaceable.width,
                                centerY - (actionsPlaceable.height / 2)
                            )
                        }
                    }
                }
            }
        }
    }

    // PROFESSIONAL FULL SCREEN TEXT EDITOR DIALOG
    if (isFullScreenOpen) {
        val focusRequester = remember { FocusRequester() }
        val clipboardManager = LocalClipboardManager.current
        val wordCount = remember(inputText) {
            val trimmed = inputText.trim()
            if (trimmed.isEmpty()) 0 else trimmed.split(Regex("\\s+")).size
        }

        LaunchedEffect(isFullScreenOpen) {
            if (isFullScreenOpen) {
                kotlinx.coroutines.delay(120)
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        Dialog(
            onDismissRequest = { isFullScreenOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (theme.isLight) Color(0xFFF8FAFC) else Color(0xFF121316)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    // Top Navigation & Actions Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (theme.isLight) Color.White else Color(0xFF1C1D21))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isFullScreenOpen = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Închide",
                                tint = if (theme.isLight) Color(0xFF1E293B) else Color.White
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Editor Text",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = if (theme.isLight) Color(0xFF0F172A) else Color.White
                            )
                            Text(
                                text = "$wordCount cuvinte · ${inputText.length} caractere",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = if (theme.isLight) Color(0xFF64748B) else Color(0xFF9CA3AF)
                            )
                        }

                        // Apply / Done Checkmark Button
                        IconButton(onClick = { isFullScreenOpen = false }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Gata",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Direct Send Button
                        IconButton(
                            onClick = {
                                val trimmed = inputText.trim()
                                if (trimmed.isNotBlank() || currentAttachment != null) {
                                    keyboardController?.hide()
                                    onSendMessage(trimmed)
                                    onInputTextChange("")
                                    isFullScreenOpen = false
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(dynamicAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Trimite direct",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Professional Text Manipulation Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (theme.isLight) Color(0xFFF1F5F9) else Color(0xFF181A1E))
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Copy Button
                        ChatQuickChip(
                            label = "📋 Copiază",
                            onClick = {
                                clipboardManager.setText(AnnotatedString(inputText))
                            }
                        )

                        // Paste Button
                        ChatQuickChip(
                            label = "📥 Lipește",
                            onClick = {
                                val pasted = clipboardManager.getText()?.text
                                if (!pasted.isNullOrEmpty()) {
                                    onInputTextChange(inputText + pasted)
                                }
                            }
                        )

                        // Toggle Case (UPPERCASE / lowercase)
                        ChatQuickChip(
                            label = "🔤 Majuscule/minuscule",
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val isAllUpper = inputText == inputText.uppercase()
                                    onInputTextChange(if (isAllUpper) inputText.lowercase() else inputText.uppercase())
                                }
                            }
                        )

                        // Bold Markdown Format
                        ChatQuickChip(
                            label = "<b>Bold</b>",
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    onInputTextChange("**$inputText**")
                                }
                            }
                        )

                        // Clear Button
                        ChatQuickChip(
                            label = "🗑️ Șterge tot",
                            onClick = {
                                onInputTextChange("")
                            }
                        )
                    }

                    // Main Professional Editor Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        BasicTextField(
                            value = inputText,
                            onValueChange = onInputTextChange,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = if (theme.isLight) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                fontSize = 17.sp,
                                lineHeight = 26.sp
                            ),
                            cursorBrush = SolidColor(theme.accent),
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(focusRequester)
                                .testTag("fullscreen_editor_textfield")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatQuickChip(
    label: String,
    onClick: () -> Unit
) {
    val theme = LocalHermesTheme.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (theme.isLight) Color.White else Color(0xFF26282C),
        border = BorderStroke(1.dp, if (theme.isLight) Color(0xFFE2E8F0) else Color(0xFF383B40)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            ),
            color = if (theme.isLight) Color(0xFF334155) else Color(0xFFE5E7EB),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
