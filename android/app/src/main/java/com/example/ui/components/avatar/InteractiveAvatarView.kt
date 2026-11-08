package com.example.ui.components.avatar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Ultra-fluid, lifelike 3D Avatar Composable that moves with human-grade organic grace,
 * reacts to agent tasks with dignified choreography, paces tool transitions so actions
 * don't flash rapidly, and pulls out/stows tools with realistic hand & gaze coordination.
 */
@Composable
fun InteractiveAvatarView(
    avatarProfile: AvatarProfile,
    activity: AvatarActivity,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    customization: com.example.data.model.AvatarCustomization? = null,
    showContactShadow: Boolean = true,
    showSpeechBubble: Boolean = true,
    onSpeechBubbleChange: ((String?) -> Unit)? = null,
    onAvatarClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Interactive scale & bounce physics
    val scaleX = remember { Animatable(1f) }
    val scaleY = remember { Animatable(1f) }
    val jumpOffsetY = remember { Animatable(0f) }
    val tiltRotation = remember { Animatable(0f) }

    // Multi-tap detection
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    // Speech / emotional reaction bubble
    var activeBubbleText by remember { mutableStateOf<String?>(null) }
    var activePropOverride by remember { mutableStateOf<AvatarPropType?>(null) }

    // --- 1. CHOREOGRAPHED ACTIVITY PACER & TOOL TRANSITION ENGINE ---
    // Prevents rapid 3-4 tools/sec flickering; ensures each tool is displayed calmly (min 2.8s)
    var displayedActivity by remember { mutableStateOf(activity) }
    var lastActivityTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Tool emergence from pocket/side into hands (0f = hidden in pocket, 1f = fully held in hand)
    val propEmergence = remember { Animatable(if (activity.propType != AvatarPropType.NONE) 1f else 0f) }
    
    // Gaze and torso lean towards pocket when retrieving or stowing tools
    val retrievalLean = remember { Animatable(0f) }

    // Harmonize activity changes with smooth human choreography
    LaunchedEffect(activity, activePropOverride) {
        val targetActivity = if (activePropOverride != null) {
            AvatarActivity.InteractiveReact("Reacție", "", activePropOverride!!)
        } else {
            activity
        }

        if (targetActivity.id == displayedActivity.id && activePropOverride == null) {
            return@LaunchedEffect
        }

        val now = System.currentTimeMillis()
        val elapsed = now - lastActivityTime
        val minDisplayDuration = 2800L // 2.8s display window so user can clearly see and appreciate the tool

        if (elapsed < minDisplayDuration && displayedActivity != AvatarActivity.Idle) {
            delay(minDisplayDuration - elapsed)
        }

        // Step 1: Smoothly stow the previous tool into the pocket/side if one was held
        if (displayedActivity.propType != AvatarPropType.NONE) {
            val stowSide = if (displayedActivity.propType == AvatarPropType.TOOL) -3.5f else 3.5f
            coroutineScope.launch {
                retrievalLean.animateTo(stowSide, tween(240, easing = FastOutSlowInEasing))
            }
            propEmergence.animateTo(0f, tween(360, easing = FastOutSlowInEasing))
            retrievalLean.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
        }

        // Switch to the newly chosen activity
        displayedActivity = targetActivity
        lastActivityTime = System.currentTimeMillis()

        // Step 2: Smoothly reach towards pocket and draw out the new tool
        if (targetActivity.propType != AvatarPropType.NONE) {
            val retrieveSide = if (targetActivity.propType == AvatarPropType.TOOL) -4.5f else 4.5f
            retrievalLean.animateTo(retrieveSide, tween(260, easing = FastOutSlowInEasing))
            
            coroutineScope.launch {
                retrievalLean.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
            }
            propEmergence.animateTo(
                targetValue = 1f,
                animationSpec = tween(550, easing = CubicBezierEasing(0.2f, 0.8f, 0.25f, 1f))
            )
        } else {
            propEmergence.snapTo(0f)
            retrievalLean.animateTo(0f, tween(280))
        }
    }

    val activeProp = activePropOverride ?: displayedActivity.propType

    // --- 2. DYNAMIC ACTIVITY POSTURE MATRIX ---
    // Calculates target body lean, gaze tilt, scale, and offset for each specific activity
    val (targetTilt, targetOffsetX, targetOffsetY, targetScaleX, targetScaleY) = remember(displayedActivity, activeProp) {
        when {
            displayedActivity is AvatarActivity.ReadingBook || activeProp == AvatarPropType.BOOK -> {
                // Posture: Leans forward & turns gaze down-right (+11.5°) toward open 3D illuminated book
                Tuple5(11.5f, 2.0f, 0f, 1.02f, 1.02f)
            }
            displayedActivity is AvatarActivity.WritingCode || activeProp == AvatarPropType.LAPTOP -> {
                // Posture: Leans forward into terminal screen with focused stance
                Tuple5(6.5f, 1.8f, -0.8f, 1.02f, 0.99f)
            }
            displayedActivity is AvatarActivity.SearchingWeb || activeProp == AvatarPropType.MAGNIFIER -> {
                // Posture: Scanning gaze looking through magnifying glass
                Tuple5(0f, 0f, -1.2f, 1.0f, 1.0f)
            }
            displayedActivity is AvatarActivity.Thinking || activeProp == AvatarPropType.LIGHTBULB -> {
                // Posture: Head tilted up-left (-11.0°) gazing toward floating stardust idea bulb
                Tuple5(-11.0f, -2.0f, -1.8f, 0.99f, 1.03f)
            }
            displayedActivity is AvatarActivity.BuildingTools || activeProp == AvatarPropType.TOOL -> {
                // Posture: Dignified engineering working tilt
                Tuple5(-5.5f, -1.8f, -0.8f, 1.02f, 1.01f)
            }
            displayedActivity is AvatarActivity.AwaitingApproval || activeProp == AvatarPropType.SHIELD -> {
                // Posture: Attentive questioning tilt
                Tuple5(4.5f, 0f, -0.8f, 1.0f, 1.0f)
            }
            displayedActivity is AvatarActivity.SuccessCelebration || activeProp == AvatarPropType.STARS -> {
                // Posture: Jubilant stance
                Tuple5(0f, 0f, -3.0f, 1.06f, 1.06f)
            }
            else -> Tuple5(0f, 0f, 0f, 1.0f, 1.0f)
        }
    }

    // --- 3. ULTRA-SMOOTH POSTURE TRANSITIONS (Fluid 650ms easing curve, zero snapping) ---
    val postureTilt by animateFloatAsState(
        targetValue = targetTilt,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "posture_tilt"
    )
    val postureOffsetX by animateFloatAsState(
        targetValue = targetOffsetX,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "posture_offset_x"
    )
    val postureOffsetY by animateFloatAsState(
        targetValue = targetOffsetY,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "posture_offset_y"
    )
    val postureScaleX by animateFloatAsState(
        targetValue = targetScaleX,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "posture_scale_x"
    )
    val postureScaleY by animateFloatAsState(
        targetValue = targetScaleY,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "posture_scale_y"
    )

    // --- 4. LIVING CREATURE PHYSIOLOGICAL BREATHING & ALIVE HARMONICS ---
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_life")

    // Realistic dual-axis respiratory wave (slow 3.8s natural human/animal respiration cycle)
    val breathingX by infiniteTransition.animateFloat(
        initialValue = 0.990f,
        targetValue = 1.018f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_x"
    )
    val breathingY by infiniteTransition.animateFloat(
        initialValue = 1.026f,
        targetValue = 0.985f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_y"
    )

    // Calm living vertical float
    val idleFloatOffset by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_float"
    )

    // Slow, focused reading gaze nods (2.4s cycle mimicking human eye sweep across book text)
    val readingGazeNod by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reading_gaze_nod"
    )

    // Calm horizontal scanning gaze when using magnifier (3.4s wide ease)
    val scanningHorizonSway by infiniteTransition.animateFloat(
        initialValue = -4.5f,
        targetValue = 4.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanning_horizon_sway"
    )

    // Continuous lifelike stance weight shift (4.8s gentle sinusoidal harmonic)
    val livingPetSway by infiniteTransition.animateFloat(
        initialValue = -1.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "living_pet_sway"
    )

    // Focused, dignified pulse during code generation / typing
    val codingTypingPulse by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coding_typing_pulse"
    )

    // Parse accent color
    val accentColor = remember(avatarProfile.accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(avatarProfile.accentColorHex))
        } catch (_: Exception) {
            Color(0xFF10B981)
        }
    }

    // Dismiss active speech bubble after timeout and sync with external listener
    LaunchedEffect(activeBubbleText) {
        onSpeechBubbleChange?.invoke(activeBubbleText)
        if (activeBubbleText != null) {
            delay(3200)
            activeBubbleText = null
            activePropOverride = null
            onSpeechBubbleChange?.invoke(null)
        }
    }

    // Compute active motion tilt and composite transform
    val activeMotionTilt = when (activeProp) {
        AvatarPropType.BOOK -> readingGazeNod
        AvatarPropType.MAGNIFIER -> scanningHorizonSway
        AvatarPropType.LIGHTBULB -> 0f
        else -> livingPetSway
    }

    val currentActivityOffset = when (displayedActivity) {
        is AvatarActivity.WritingCode -> codingTypingPulse
        else -> 0f
    }

    // Harmonize final rotation with retrieval lean (so avatar glances down towards pocket when drawing out tools)
    val finalRotation = postureTilt + tiltRotation.value + activeMotionTilt + retrievalLean.value
    val finalOffsetX = postureOffsetX + (retrievalLean.value * 0.3f)
    val finalOffsetY = postureOffsetY + idleFloatOffset + jumpOffsetY.value.coerceAtMost(0f) + currentActivityOffset.coerceAtMost(0f)

    val finalScaleX = scaleX.value * postureScaleX * breathingX
    val finalScaleY = scaleY.value * postureScaleY * breathingY

    Box(
        modifier = modifier
            .wrapContentSize()
            .pointerInput(avatarProfile.id) {
                detectTapGestures(
                    onPress = { offset ->
                        coroutineScope.launch {
                            scaleX.animateTo(1.05f, tween(100, easing = FastOutSlowInEasing))
                            scaleY.animateTo(0.96f, tween(100, easing = FastOutSlowInEasing))
                        }
                        tryAwaitRelease()
                        coroutineScope.launch {
                            scaleX.animateTo(1.0f, tween(260, easing = FastOutSlowInEasing))
                            scaleY.animateTo(1.0f, tween(260, easing = FastOutSlowInEasing))
                        }
                    },
                    onTap = { offset ->
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastTapTime < 600) {
                            tapCount++
                        } else {
                            tapCount = 1
                        }
                        lastTapTime = currentTime

                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                        if (tapCount >= 3) {
                            coroutineScope.launch {
                                tiltRotation.animateTo(360f, tween(600, easing = FastOutSlowInEasing))
                                tiltRotation.snapTo(0f)
                            }
                            val feedback = AvatarActivityResolver.getTouchFeedback(avatarProfile.id, AvatarTouchGesture.RapidTripleTap)
                            activeBubbleText = feedback.quote
                            activePropOverride = feedback.prop
                            tapCount = 0
                        } else {
                            val isHeadTap = offset.y < (size.toPx() * 0.45f)
                            val gesture = if (isHeadTap) AvatarTouchGesture.SingleTapHead else AvatarTouchGesture.SingleTapBelly
                            val feedback = AvatarActivityResolver.getTouchFeedback(avatarProfile.id, gesture)
                            activeBubbleText = feedback.quote
                            activePropOverride = feedback.prop

                            coroutineScope.launch {
                                if (isHeadTap) {
                                    tiltRotation.animateTo(-6f, tween(180, easing = FastOutSlowInEasing))
                                    tiltRotation.animateTo(0f, tween(280, easing = FastOutSlowInEasing))
                                } else {
                                    jumpOffsetY.animateTo(-4f, tween(180, easing = FastOutSlowInEasing))
                                    jumpOffsetY.animateTo(0f, tween(280, easing = FastOutSlowInEasing))
                                }
                            }
                        }
                        onAvatarClick?.invoke()
                    },
                    onDoubleTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val feedback = AvatarActivityResolver.getTouchFeedback(avatarProfile.id, AvatarTouchGesture.DoubleTap)
                        activeBubbleText = feedback.quote
                        activePropOverride = feedback.prop

                        coroutineScope.launch {
                            jumpOffsetY.animateTo(-8f, tween(250, easing = FastOutSlowInEasing))
                            jumpOffsetY.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
                        }
                    },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val feedback = AvatarActivityResolver.getTouchFeedback(avatarProfile.id, AvatarTouchGesture.LongPress)
                        activeBubbleText = feedback.quote
                        activePropOverride = feedback.prop

                        coroutineScope.launch {
                            scaleX.animateTo(1.08f, tween(250, easing = FastOutSlowInEasing))
                            scaleY.animateTo(1.08f, tween(250, easing = FastOutSlowInEasing))
                            delay(300)
                            scaleX.animateTo(1.0f, tween(350, easing = FastOutSlowInEasing))
                            scaleY.animateTo(1.0f, tween(350, easing = FastOutSlowInEasing))
                        }
                    }
                )
            }
            .pointerInput(avatarProfile.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    coroutineScope.launch {
                        val sway = (dragAmount.x * 0.35f).coerceIn(-12f, 12f)
                        tiltRotation.animateTo(sway, tween(90, easing = FastOutSlowInEasing))
                        tiltRotation.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
                    }
                    if (activeBubbleText == null) {
                        val feedback = AvatarActivityResolver.getTouchFeedback(avatarProfile.id, AvatarTouchGesture.PettingDrag)
                        activeBubbleText = feedback.quote
                        activePropOverride = feedback.prop
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Avatar Character Body + 3D Props Container (Fixed position, smoothly leans & reacts)
            Box(
                modifier = Modifier
                    .size(size)
                    .offset {
                        IntOffset(
                            x = with(density) { finalOffsetX.dp.toPx().roundToInt() },
                            y = with(density) { finalOffsetY.dp.toPx().roundToInt() }
                        )
                    }
                    .scale(finalScaleX, finalScaleY)
                    .rotate(finalRotation),
                contentAlignment = Alignment.Center
            ) {
                val skinToneFilter = remember(customization?.skinTone) {
                    when (customization?.skinTone) {
                        "Warm Sand", "Nisip Cald" -> androidx.compose.ui.graphics.ColorFilter.tint(Color(0x18F59E0B), androidx.compose.ui.graphics.BlendMode.SrcAtop)
                        "Cream Zen", "Crem Zen" -> androidx.compose.ui.graphics.ColorFilter.tint(Color(0x18FDE68A), androidx.compose.ui.graphics.BlendMode.SrcAtop)
                        "Pearl Lilac", "Liliac Perlat" -> androidx.compose.ui.graphics.ColorFilter.tint(Color(0x18C084FC), androidx.compose.ui.graphics.BlendMode.SrcAtop)
                        "Arctic Mint", "Mentă Arctică" -> androidx.compose.ui.graphics.ColorFilter.tint(Color(0x1834D399), androidx.compose.ui.graphics.BlendMode.SrcAtop)
                        "Stardust Violet", "Violet Stardust" -> androidx.compose.ui.graphics.ColorFilter.tint(Color(0x18A855F7), androidx.compose.ui.graphics.BlendMode.SrcAtop)
                        else -> null
                    }
                }

                // Pure character body cutout - direct rendering with no frame or background, exactly like the fox and panda
                Image(
                    painter = painterResource(id = avatarProfile.drawableRes),
                    contentDescription = avatarProfile.name,
                    contentScale = ContentScale.Fit,
                    colorFilter = skinToneFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                )

                // Personalized Accessory Overlay (Smart Glasses, Monocle, Headphones, Halo)
                if (customization != null && customization.accessory.isNotBlank()) {
                    AvatarAccessoryOverlay(
                        accessory = customization.accessory,
                        avatarSize = size,
                        accentColor = accentColor
                    )
                }

                // 3D Activity Props (Book, Laptop, Tool, Magnifier, Hearts, etc.) with Emergence Animation
                Avatar3DPropOverlay(
                    propType = activeProp,
                    accentColor = accentColor,
                    avatarSize = size,
                    emergenceProgress = propEmergence.value
                )
            }

            // Contact Drop Shadow underneath (softly expands and contracts with breathing/jumping)
            if (showContactShadow) {
                val shadowAlpha = ((1f - (jumpOffsetY.value / -14f)).coerceIn(0.2f, 1f))
                val shadowWidth = (size.value * 0.82f * finalScaleX).dp

                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(width = shadowWidth, height = 7.dp)
                        .alpha(shadowAlpha)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0x55000000),
                                    Color(0x22000000),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }

            // If showSpeechBubble is true AND no external receiver is registered, display UNDER avatar:
            if (showSpeechBubble && onSpeechBubbleChange == null) {
                AnimatedVisibility(
                    visible = activeBubbleText != null,
                    enter = scaleIn(tween(280)) + fadeIn(tween(280)),
                    exit = scaleOut(tween(220)) + fadeOut(tween(220))
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xEE1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = activeBubbleText.orEmpty(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

// Helper tuple for posture configuration
private data class Tuple5<A, B, C, D, E>(
    val a: A, val b: B, val c: C, val d: D, val e: E
)
