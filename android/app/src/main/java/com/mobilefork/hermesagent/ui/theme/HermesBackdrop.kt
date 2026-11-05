package com.mobilefork.hermesagent.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Semantic colors for Hermes' original translucent, refractive panel language. */
@Immutable
data class HermesGlassTokens(
    val panel: Color,
    val elevatedPanel: Color,
    val border: Color,
    val highlight: Color,
    val accentGlow: Color,
    val secondaryGlow: Color,
    val backdropTop: Color,
    val backdropMiddle: Color,
    val backdropBottom: Color,
    val tabBar: Color,
    val tabSelected: Color,
    val bubbleAgent: Color,
    val bubbleUser: Color,
    val accentContainer: Color,
    val success: Color,
    val successContainer: Color,
    val mutedText: Color,
    val softShadow: Color,
    val glassFillTop: Color,
    val glassFillBottom: Color,
    val glassRimTop: Color,
    val glassRimBottom: Color,
    val lightCanvas: Boolean,
) {
    val rim: Brush = Brush.verticalGradient(listOf(highlight, border, border, accentGlow))
}

val LocalHermesGlassTokens = staticCompositionLocalOf {
    HermesGlassTokens(
        panel = Color(0xFFFFFFFF),
        elevatedPanel = Color(0xFFF5F5F6),
        border = Color(0xFFEBEBED),
        highlight = Color(0x57FFFFFF),
        accentGlow = Color(0x2E0066D6),
        secondaryGlow = Color(0x1F0A9B3C),
        backdropTop = Color(0xFFEBF3FC),
        backdropMiddle = Color(0xFFFFFFFF),
        backdropBottom = Color(0xFFF2FAF5),
        tabBar = Color(0xFFFFFFFF),
        tabSelected = Color(0xFFEBEBED),
        bubbleAgent = Color(0xFFEBEBED),
        bubbleUser = Color(0xFFCFE5FB),
        accentContainer = Color(0xFFCCE0F7),
        success = Color(0xFF0A9B3C),
        successContainer = Color(0xFFCEEBD8),
        mutedText = Color(0xFF6B6F76),
        softShadow = Color(0x1F16324F),
        glassFillTop = Color(0xDBFFFFFF),
        glassFillBottom = Color(0xA8FFFFFF),
        glassRimTop = Color(0xF2FFFFFF),
        glassRimBottom = Color(0x14111318),
        lightCanvas = true,
    )
}

internal fun hermesGlassTokens(
    primary: Color,
    secondary: Color,
    background: Color,
    surface: Color,
    surfaceVariant: Color,
    onSurface: Color,
    lightCanvas: Boolean,
): HermesGlassTokens {
    return HermesGlassTokens(
        panel = surface.copy(alpha = if (lightCanvas) 0.82f else 0.76f),
        elevatedPanel = surfaceVariant.copy(alpha = if (lightCanvas) 0.92f else 0.88f),
        border = lerp(surfaceVariant, onSurface, if (lightCanvas) 0.04f else 0.50f).copy(alpha = 0.92f),
        highlight = Color.White.copy(alpha = if (lightCanvas) 0.34f else 0.20f),
        accentGlow = primary.copy(alpha = if (lightCanvas) 0.18f else 0.24f),
        secondaryGlow = secondary.copy(alpha = if (lightCanvas) 0.12f else 0.18f),
        backdropTop = lerp(background, primary, if (lightCanvas) 0.08f else 0.16f),
        backdropMiddle = background,
        backdropBottom = lerp(background, secondary, if (lightCanvas) 0.05f else 0.09f),
        tabBar = surface,
        tabSelected = surfaceVariant,
        bubbleAgent = surfaceVariant,
        bubbleUser = lerp(surface, primary, if (lightCanvas) 0.19f else 0.34f),
        accentContainer = lerp(surface, primary, if (lightCanvas) 0.20f else 0.28f),
        success = secondary,
        successContainer = lerp(surface, secondary, if (lightCanvas) 0.20f else 0.28f),
        mutedText = lerp(onSurface, surface, if (lightCanvas) 0.42f else 0.32f),
        softShadow = if (lightCanvas) lerp(Color.Black, primary, 0.25f).copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.40f),
        glassFillTop = surface.copy(alpha = if (lightCanvas) 0.86f else 0.62f),
        glassFillBottom = surface.copy(alpha = if (lightCanvas) 0.66f else 0.48f),
        glassRimTop = Color.White.copy(alpha = if (lightCanvas) 0.95f else 0.26f),
        glassRimBottom = onSurface.copy(alpha = if (lightCanvas) 0.08f else 0.12f),
        lightCanvas = lightCanvas,
    )
}

/** The one canvas under every page: pages draw on it and never paint their own background. */
@Composable
fun HermesBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = LocalHermesGlassTokens.current
    val backdrop = remember(tokens) {
        // A tinted light canvas stays tinted all the way down, so white cards lift off it.
        val middle = if (tokens.lightCanvas) lerp(tokens.backdropTop, tokens.backdropBottom, 0.5f) else tokens.backdropMiddle
        Brush.verticalGradient(0.0f to tokens.backdropTop, 0.44f to middle, 1.0f to tokens.backdropBottom)
    }
    val glow = remember(tokens) { Brush.radialGradient(
        0.0f to tokens.accentGlow, 0.56f to Color.Transparent, radius = 920f,
    ) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backdrop)
            .then(if (tokens.lightCanvas) Modifier else Modifier.background(glow)),
        content = content,
    )
}

/** Transparent top-level surface color; panels use Material surface-container roles. */
fun hermesPanelColor(): Color = Color.Transparent

/** Backgrounds for generated agent faces: each carries white features on any canvas. */
val hermesAvatarPalette = listOf(
    Color(0xFF3F6FD8),
    Color(0xFF7A5CC9),
    Color(0xFFC2558B),
    Color(0xFFD0704A),
    Color(0xFF2F9486),
    Color(0xFF4C8F3F),
    Color(0xFF4A5A73),
    Color(0xFFB0822C),
)
