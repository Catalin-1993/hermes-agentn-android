package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Hermes Theme Palette & Dynamic Accents
val AppAccent: Color
    @Composable
    get() = LocalHermesTheme.current.accent

val AppPrimary: Color
    @Composable
    get() = LocalHermesTheme.current.primary

val EmeraldPrimary: Color
    @Composable
    get() = LocalHermesTheme.current.accent

val EmeraldPrimaryDark: Color
    @Composable
    get() = LocalHermesTheme.current.accent.copy(alpha = 0.8f)

val EmeraldAccent: Color
    @Composable
    get() = LocalHermesTheme.current.accent

val EmeraldBright: Color
    @Composable
    get() = LocalHermesTheme.current.accent

val EmeraldHighlight: Color
    @Composable
    get() = LocalHermesTheme.current.accent

val EmeraldBadge: Color
    @Composable
    get() = LocalHermesTheme.current.accent

// Dynamic theme-aware colors delegating to LocalHermesTheme
val BackgroundDeep: Color
    @Composable
    get() = LocalHermesTheme.current.backgroundCanvas

val BackgroundSurface: Color
    @Composable
    get() = LocalHermesTheme.current.backgroundSurface

val SurfaceCard: Color
    @Composable
    get() = LocalHermesTheme.current.surfaceCard

val SurfaceCardHigh: Color
    @Composable
    get() = LocalHermesTheme.current.surfaceCardHigh

val SurfaceInput: Color
    @Composable
    get() = LocalHermesTheme.current.surfaceInput

val SurfaceBottomSheet: Color
    @Composable
    get() = LocalHermesTheme.current.surfaceBottomSheet

val SurfaceDrawer: Color
    @Composable
    get() = LocalHermesTheme.current.surfaceDrawer

val SurfaceModal: Color
    @Composable
    get() = LocalHermesTheme.current.surfaceModal

val BorderSubtle: Color
    @Composable
    get() = LocalHermesTheme.current.borderSubtle

val BorderAccent: Color
    @Composable
    get() = LocalHermesTheme.current.borderAccent

val TextPrimary: Color
    @Composable
    get() = LocalHermesTheme.current.textPrimary

val TextSecondary: Color
    @Composable
    get() = LocalHermesTheme.current.textSecondary

val TextMuted: Color
    @Composable
    get() = LocalHermesTheme.current.textMuted

val TextDisabled: Color
    @Composable
    get() = LocalHermesTheme.current.textDisabled

val UserBubbleBg: Color
    @Composable
    get() = LocalHermesTheme.current.userBubbleBg

val UserBubbleBorder: Color
    @Composable
    get() = LocalHermesTheme.current.userBubbleBorder

val UserBubbleText: Color
    @Composable
    get() = LocalHermesTheme.current.userBubbleText

val AssistantBubbleBg: Color
    @Composable
    get() = LocalHermesTheme.current.assistantBubbleBg

val AssistantBubbleText: Color
    @Composable
    get() = LocalHermesTheme.current.assistantBubbleText

// Avatar and Action Badges
val AvatarGradientStart = Color(0xFF06B6D4)
val AvatarGradientEnd = Color(0xFF3B82F6)
val AvatarVGBg = Color(0xFF00A67E)

// Bottom Sheet Attachment Colors
val AttachCamera = Color(0xFF10B981)
val AttachPhotos = Color(0xFF3B82F6)
val AttachFiles = Color(0xFF8B5CF6)
val AttachLibraries = Color(0xFFF59E0B)

val EmeraldGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF00C48C), Color(0xFF007555))
)

val AvatarGradient = Brush.linearGradient(
    colors = listOf(AvatarGradientStart, AvatarGradientEnd)
)

/**
 * Screen Background:
 * - In Light (Alb) theme: Clean pure off-white canvas with gentle diffuse upper aura.
 * - In ChatGPT Dark theme: Matte charcoal canvas in authentic ChatGPT mobile app styling.
 * - In Emerald (Verde Original) theme: Radiant emerald aura diffusing from top-left into dark canvas.
 */
/** True where the host app already paints the canvas, so a page leaves its own background out. */
val LocalTransparentPageBackground = androidx.compose.runtime.staticCompositionLocalOf { false }

@Composable
fun Modifier.hermesPageBackground(): Modifier {
    if (LocalTransparentPageBackground.current) return this
    val theme = LocalHermesTheme.current
    return when (theme.themeMode) {
        HermesAppThemeMode.LIGHT -> {
            this
                .background(Color(0xFFE6E8EC))
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFFDFE2E6),
                            0.15f to Color(0xFFE2E5E9),
                            0.50f to Color(0xFFE6E8EC),
                            0.85f to Color(0xFFE2E5E9),
                            1.00f to Color(0xFFDFE2E6)
                        )
                    )
                )
        }
        HermesAppThemeMode.CHATGPT_DARK -> {
            this
                .background(Color(0xFF212121))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF191919),
                            Color(0xFF212121),
                            Color(0xFF212121)
                        )
                    )
                )
        }
        HermesAppThemeMode.EMERALD_ORIGINAL -> {
            this
                .background(Color(0xFF020805))
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0x9510B981), // Radiant emerald green in top-left
                            0.18f to Color(0x7810B981), // Soft luminous core
                            0.36f to Color(0x52059669), // Warm emerald midtones
                            0.55f to Color(0x34047857), // Gentle gradual transition
                            0.72f to Color(0x1D064E3B), // Deep subtle forest green hue
                            0.88f to Color(0x8A020B07), // Soft transition into dark
                            1.00f to Color(0xEA020805)  // Deep elegant canvas finish
                        ),
                        center = Offset(0f, 0f),
                        radius = 2200f
                    )
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x15020805),
                            Color(0x60010503)
                        )
                    )
                )
        }
    }
}

/**
 * Settings Background:
 * Exactly 5% darker than the Home / Chat canvas (#F2F3F5) in Light theme (#E6E8EC),
 * providing consistent, elegant contrast across all settings pages and subpages.
 */
@Composable
fun Modifier.hermesSettingsBackground(): Modifier {
    val theme = LocalHermesTheme.current
    return when (theme.themeMode) {
        HermesAppThemeMode.LIGHT -> {
            this
                .background(Color(0xFFE6E8EC))
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFFDFE2E6),
                            0.15f to Color(0xFFE2E5E9),
                            0.50f to Color(0xFFE6E8EC),
                            0.85f to Color(0xFFE2E5E9),
                            1.00f to Color(0xFFDFE2E6)
                        )
                    )
                )
        }
        HermesAppThemeMode.CHATGPT_DARK -> {
            this
                .background(Color(0xFF1E1E1E))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF181818),
                            Color(0xFF1E1E1E),
                            Color(0xFF1E1E1E)
                        )
                    )
                )
        }
        HermesAppThemeMode.EMERALD_ORIGINAL -> {
            this
                .background(Color(0xFF020805))
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0x9510B981),
                            0.18f to Color(0x7810B981),
                            0.36f to Color(0x52059669),
                            0.55f to Color(0x34047857),
                            0.72f to Color(0x1D064E3B),
                            0.88f to Color(0x8A020B07),
                            1.00f to Color(0xEA020805)
                        ),
                        center = Offset(0f, 0f),
                        radius = 2200f
                    )
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x15020805),
                            Color(0x60010503)
                        )
                    )
                )
        }
    }
}

