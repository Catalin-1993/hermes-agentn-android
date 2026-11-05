package com.mobilefork.hermesagent.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.mobilefork.hermesagent.data.AppSettings

data class HermesThemeConfig(
    val primaryHex: String = AppSettings.DEFAULT_THEME_PRIMARY_HEX,
    val secondaryHex: String = AppSettings.DEFAULT_THEME_SECONDARY_HEX,
    val backgroundHex: String = AppSettings.DEFAULT_THEME_BACKGROUND_HEX,
    val surfaceHex: String = AppSettings.DEFAULT_THEME_SURFACE_HEX,
    val surfaceVariantHex: String = AppSettings.DEFAULT_THEME_SURFACE_VARIANT_HEX,
    val cardShape: String = "rounded",
    val fontScale: Float = 1.0f,
)

private val HermesFallbackColors = lightColorScheme(
    primary = Color(0xFF0066D6),
    secondary = Color(0xFF0A9B3C),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEBEBED),
)

/**
 * Applies every saved theme value to a complete, contrast-aware Material 3 scheme.
 *
 * The translucent surface roles create Hermes' own refractive-panel language. They
 * deliberately use simple Android gradients and alpha compositing rather than any
 * copied platform material, image, shader, or proprietary asset.
 */
@Composable
fun HermesTheme(
    config: HermesThemeConfig = HermesThemeConfig(),
    content: @Composable () -> Unit,
) {
    val resolved = remember(config) { resolveHermesTheme(config) }
    val shapes = remember(config.cardShape) { hermesShapes(config.cardShape) }
    val typography = remember(config.fontScale) { scaledTypography(config.fontScale) }
    val view = LocalView.current
    SideEffect {
        view.context.findActivity()?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = resolved.lightCanvas
                isAppearanceLightNavigationBars = resolved.lightCanvas
            }
        }
    }

    CompositionLocalProvider(LocalHermesGlassTokens provides resolved.glass) {
        MaterialTheme(
            colorScheme = resolved.colors,
            shapes = shapes,
            typography = typography,
            content = content,
        )
    }
}

private class HermesResolvedTheme(
    val colors: ColorScheme,
    val glass: HermesGlassTokens,
    val lightCanvas: Boolean,
)

private fun resolveHermesTheme(config: HermesThemeConfig): HermesResolvedTheme {
    val primary = parseThemeColor(config.primaryHex, HermesFallbackColors.primary).opaque()
    val secondary = parseThemeColor(config.secondaryHex, HermesFallbackColors.secondary).opaque()
    val background = parseThemeColor(config.backgroundHex, HermesFallbackColors.background).opaque()
    val surface = parseThemeColor(config.surfaceHex, HermesFallbackColors.surface).opaque()
    val surfaceVariant = parseThemeColor(config.surfaceVariantHex, HermesFallbackColors.surfaceVariant).opaque()
    val lightCanvas = background.luminance() >= 0.46f
    val contentTone = readableOn(background)
    val surfaceContent = readableOn(surface)
    val variantContent = readableOn(surfaceVariant)
    val outline = lerp(surfaceVariant, variantContent, if (lightCanvas) 0.42f else 0.50f).opaque()
    val outlineVariant = if (lightCanvas) {
        lerp(surface, surfaceVariant, 0.92f).opaque()
    } else {
        lerp(surfaceVariant, variantContent, 0.42f).opaque()
    }
    // Light canvases keep cards one step quieter than the chat bubble and read secondary
    // text as grey; dark canvases keep their translucent panel roles.
    val cardSurface = if (lightCanvas) lerp(surface, surfaceVariant, 0.50f).opaque() else surfaceVariant
    val secondaryContent = if (lightCanvas) lerp(variantContent, cardSurface, 0.24f).opaque() else variantContent
    val primaryContainer = lerp(surface, primary, if (lightCanvas) 0.18f else 0.24f).opaque()
    val secondaryContainer = lerp(surface, secondary, if (lightCanvas) 0.16f else 0.22f).opaque()
    val tertiary = lerp(primary, secondary, 0.48f).opaque()
    val tertiaryContainer = lerp(surfaceVariant, tertiary, if (lightCanvas) 0.16f else 0.24f).opaque()
    val error = if (lightCanvas) Color(0xFFB3261E) else Color(0xFFFF6B6B)

    val colors = (if (lightCanvas) lightColorScheme() else darkColorScheme()).copy(
        primary = primary,
        onPrimary = readableOn(primary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = readableOn(primaryContainer),
        secondary = secondary,
        onSecondary = readableOn(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = readableOn(secondaryContainer),
        tertiary = tertiary,
        onTertiary = readableOn(tertiary),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = readableOn(tertiaryContainer),
        background = background,
        onBackground = contentTone,
        surface = if (lightCanvas) surface else surface.copy(alpha = 0.84f),
        onSurface = surfaceContent,
        // Inner-page cards read as the same glass as the shared cards on a light canvas.
        surfaceVariant = if (lightCanvas) surface.copy(alpha = 0.72f) else surfaceVariant.copy(alpha = 0.72f),
        onSurfaceVariant = secondaryContent,
        // Tonal elevation must not tint white surfaces on a light canvas.
        surfaceTint = if (lightCanvas) surface else primary,
        inverseSurface = if (lightCanvas) Color(0xFF202428) else Color(0xFFE9EEF0),
        inverseOnSurface = if (lightCanvas) Color(0xFFF4F7F8) else Color(0xFF1A1E21),
        inversePrimary = lerp(primary, readableOn(primary), 0.28f),
        outline = outline,
        outlineVariant = outlineVariant,
        error = error,
        onError = readableOn(error),
        errorContainer = lerp(surface, error, 0.22f),
        onErrorContainer = readableOn(lerp(surface, error, 0.22f)),
        scrim = Color.Black,
        surfaceDim = surface.copy(alpha = if (lightCanvas) 1f else 0.78f),
        surfaceBright = lerp(surface, contentTone, if (lightCanvas) 0.04f else 0.12f).copy(alpha = if (lightCanvas) 1f else 0.90f),
        surfaceContainerLowest = surface.copy(alpha = if (lightCanvas) 1f else 0.62f),
        surfaceContainerLow = lerp(surface, surfaceVariant, 0.18f).copy(alpha = if (lightCanvas) 1f else 0.68f),
        surfaceContainer = lerp(surface, surfaceVariant, 0.30f).copy(alpha = if (lightCanvas) 1f else 0.74f),
        surfaceContainerHigh = lerp(surface, surfaceVariant, 0.48f).copy(alpha = if (lightCanvas) 1f else 0.80f),
        surfaceContainerHighest = surfaceVariant.copy(alpha = if (lightCanvas) 1f else 0.86f),
    )
    val glassTokens = hermesGlassTokens(
        primary = primary,
        secondary = secondary,
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        onSurface = surfaceContent,
        lightCanvas = lightCanvas,
    )
    return HermesResolvedTheme(colors, glassTokens, lightCanvas)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun scaledTypography(rawScale: Float): Typography {
    val scale = rawScale.coerceIn(0.8f, 1.3f)
    val base = Typography()
    fun TextStyle.scaled() = copy(
        fontSize = fontSize * scale,
        lineHeight = lineHeight * scale,
        letterSpacing = 0.sp,
    )
    return base.copy(
        displayLarge = base.displayLarge.scaled(),
        displayMedium = base.displayMedium.scaled(),
        displaySmall = base.displaySmall.scaled(),
        headlineLarge = base.headlineLarge.scaled(),
        headlineMedium = base.headlineMedium.scaled(),
        headlineSmall = base.headlineSmall.scaled(),
        titleLarge = base.titleLarge.scaled(),
        titleMedium = base.titleMedium.scaled(),
        titleSmall = base.titleSmall.scaled(),
        bodyLarge = base.bodyLarge.scaled(),
        bodyMedium = base.bodyMedium.scaled(),
        bodySmall = base.bodySmall.scaled(),
        labelLarge = base.labelLarge.scaled(),
        labelMedium = base.labelMedium.scaled(),
        labelSmall = base.labelSmall.scaled(),
    )
}

fun normalizeThemeHex(value: String, fallback: String): String {
    val trimmed = value.trim()
    val raw = if (trimmed.startsWith("#")) trimmed.drop(1) else trimmed
    if (!Regex("[0-9a-fA-F]{6}|[0-9a-fA-F]{8}").matches(raw)) {
        return fallback
    }
    return "#${raw.uppercase()}"
}

internal fun normalizeThemeCardShape(value: String): String = when (value.trim().lowercase()) {
    "square", "squared" -> "square"
    "soft" -> "soft"
    else -> "rounded"
}

private fun parseThemeColor(value: String, fallback: Color): Color {
    val normalized = normalizeThemeHex(value, "")
    if (normalized.isBlank()) return fallback
    return runCatching { Color(android.graphics.Color.parseColor(normalized)) }.getOrDefault(fallback)
}

internal fun readableOn(color: Color): Color {
    val background = color.opaque()
    val darkCandidate = Color(0xFF111318)
    val lightCandidate = Color(0xFFF8FAFC)
    val darkContrast = contrastRatio(background, darkCandidate)
    val lightContrast = contrastRatio(background, lightCandidate)
    return if (darkContrast >= lightContrast) darkCandidate else lightCandidate
}

internal fun contrastRatio(first: Color, second: Color): Float {
    val firstLuminance = first.opaque().luminance()
    val secondLuminance = second.opaque().luminance()
    val lighter = maxOf(firstLuminance, secondLuminance)
    val darker = minOf(firstLuminance, secondLuminance)
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun Color.opaque(): Color = copy(alpha = 1f)

internal fun hermesShapes(cardShape: String): Shapes {
    return when (normalizeThemeCardShape(cardShape)) {
        "square" -> Shapes(
            extraSmall = RoundedCornerShape(1.dp),
            small = RoundedCornerShape(2.dp),
            medium = RoundedCornerShape(4.dp),
            large = RoundedCornerShape(6.dp),
            extraLarge = RoundedCornerShape(8.dp),
        )
        "soft" -> Shapes(
            extraSmall = RoundedCornerShape(5.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(18.dp),
            extraLarge = RoundedCornerShape(24.dp),
        )
        else -> Shapes(
            extraSmall = RoundedCornerShape(6.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(34.dp),
        )
    }
}
