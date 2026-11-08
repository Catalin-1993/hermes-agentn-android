package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class HermesAppThemeMode {
    LIGHT,             // Muse Light (Alb)
    EMERALD_ORIGINAL,  // Emerald Nocturne (Verde Original)
    CHATGPT_DARK       // ChatGPT Dark (Negru ChatGPT)
}

data class HermesThemeColors(
    val themeMode: HermesAppThemeMode = HermesAppThemeMode.CHATGPT_DARK,
    val isLight: Boolean = false,
    val backgroundCanvas: Color,
    val backgroundSurface: Color,
    val surfaceCard: Color,
    val surfaceCardHigh: Color,
    val surfaceInput: Color,
    val surfaceBottomSheet: Color,
    val surfaceDrawer: Color,
    val surfaceModal: Color,
    val borderSubtle: Color,
    val borderAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val primary: Color,
    val accent: Color,
    val userBubbleBg: Color,
    val userBubbleText: Color,
    val userBubbleBorder: Color,
    val assistantBubbleBg: Color,
    val assistantBubbleText: Color,
    val assistantCardBg: Color,
    val assistantCardBorder: Color,
    val dockBg: Color,
    val dockBorder: Color,
    val dockActiveBg: Color,
    val dockActiveTint: Color,
    val dockInactiveTint: Color,
    val headerButtonBg: Color,
    val headerButtonBorder: Color,
    val headerButtonTint: Color
)

// Static base colors
val DefaultEmeraldPrimary = Color(0xFF00A67E)
val DefaultEmeraldAccent = Color(0xFF10A37F)
val DefaultEmeraldBright = Color(0xFF22C55E)
val DefaultEmeraldDark = Color(0xFF0D6E54)

// 1. Tema Verde Originală (Emerald Nocturne)
val DarkEmeraldColors = HermesThemeColors(
    themeMode = HermesAppThemeMode.EMERALD_ORIGINAL,
    isLight = false,
    backgroundCanvas = Color(0xFF040D09),
    backgroundSurface = Color(0xFF081610),
    surfaceCard = Color(0xFF0D2118),
    surfaceCardHigh = Color(0xFF132E22),
    surfaceInput = Color(0xD80D2219),
    surfaceBottomSheet = Color(0xFF081610),
    surfaceDrawer = Color(0xFF06120D),
    surfaceModal = Color(0xFF0A1B14),
    borderSubtle = Color(0xFF1B3B2B),
    borderAccent = Color(0x5510B981),
    textPrimary = Color(0xFFECFDF5),
    textSecondary = Color(0xFFA7F3D0),
    textMuted = Color(0xFF6EE7B7),
    textDisabled = Color(0xFF3B6E57),
    primary = Color(0xFF10B981),
    accent = Color(0xFF10B981),
    userBubbleBg = Color(0xFF14382A),
    userBubbleText = Color(0xFFECFDF5),
    userBubbleBorder = Color.Transparent,
    assistantBubbleBg = Color(0xFF10281F),
    assistantBubbleText = Color(0xFFECFDF5),
    assistantCardBg = Color(0xFF0D2118),
    assistantCardBorder = Color(0xFF1B3B2B),
    dockBg = Color(0xE00A1B14),
    dockBorder = Color(0x4510B981),
    dockActiveBg = Color(0x4010B981),
    dockActiveTint = Color(0xFF10B981),
    dockInactiveTint = Color(0xFF86A899),
    headerButtonBg = Color(0xD80C2017),
    headerButtonBorder = Color(0x4510B981),
    headerButtonTint = Color(0xFFECFDF5)
)

// 2. Tema Dark în stil ChatGPT Mobile App
val ChatGPTDarkColors = HermesThemeColors(
    themeMode = HermesAppThemeMode.CHATGPT_DARK,
    isLight = false,
    backgroundCanvas = Color(0xFF212121),
    backgroundSurface = Color(0xFF171717),
    surfaceCard = Color(0xFF2F2F2F),
    surfaceCardHigh = Color(0xFF383838),
    surfaceInput = Color(0xD62F2F2F),
    surfaceBottomSheet = Color(0xFF1E1E1E),
    surfaceDrawer = Color(0xFF171717),
    surfaceModal = Color(0xFF212121),
    borderSubtle = Color(0xFF383838),
    borderAccent = Color(0xFF4B4B4B),
    textPrimary = Color(0xFFECECEC),
    textSecondary = Color(0xFFB4B4B4),
    textMuted = Color(0xFF767676),
    textDisabled = Color(0xFF4E4E4E),
    primary = Color(0xFF10A37F),
    accent = Color(0xFF10A37F),
    userBubbleBg = Color(0xFF233954),
    userBubbleText = Color(0xFFECECEC),
    userBubbleBorder = Color.Transparent,
    assistantBubbleBg = Color(0xFF262628),
    assistantBubbleText = Color(0xFFECECEC),
    assistantCardBg = Color(0xFF282828),
    assistantCardBorder = Color.Transparent,
    dockBg = Color(0xD62F2F2F),
    dockBorder = Color(0x35FFFFFF),
    dockActiveBg = Color(0x55383838),
    dockActiveTint = Color(0xFFFFFFFF),
    dockInactiveTint = Color(0xFF8E8E93),
    headerButtonBg = Color(0xD62F2F2F),
    headerButtonBorder = Color(0x35FFFFFF),
    headerButtonTint = Color(0xFFECECEC)
)

// 3. Tema Albă Light (1:1 cu specificația neutră fără turcoaz/albastru intens)
val MuseLightColors = HermesThemeColors(
    themeMode = HermesAppThemeMode.LIGHT,
    isLight = true,
    backgroundCanvas = Color(0xFFE6E8EC),
    backgroundSurface = Color(0xFFFFFFFF),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceCardHigh = Color(0xFFFFFFFF),
    surfaceInput = Color(0xF8EEEEEE),
    surfaceBottomSheet = Color(0xFFFFFFFF),
    surfaceDrawer = Color(0xFFFFFFFF),
    surfaceModal = Color(0xFFFFFFFF),
    borderSubtle = Color(0x18000000),
    borderAccent = Color.Transparent,
    textPrimary = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF8E8E93),
    textMuted = Color(0xFF8E8E93),
    textDisabled = Color(0xFFA0A0A5),
    primary = Color(0xFF1A1A1A),
    accent = Color(0xFF1A1A1A),
    userBubbleBg = Color(0xFFD8E7FA),
    userBubbleText = Color(0xFF111827),
    userBubbleBorder = Color.Transparent,
    assistantBubbleBg = Color(0xFFF2F2F4),
    assistantBubbleText = Color(0xFF1A1A1A),
    assistantCardBg = Color(0xFFF2F2F4),
    assistantCardBorder = Color.Transparent,
    dockBg = Color(0xF4F7F8FA),
    dockBorder = Color(0x60FFFFFF),
    dockActiveBg = Color(0x80E8ECEF),
    dockActiveTint = Color(0xFF1A1A1A),
    dockInactiveTint = Color(0xFF8E8E93),
    headerButtonBg = Color(0xD6EEEEEE),
    headerButtonBorder = Color(0x60FFFFFF),
    headerButtonTint = Color(0xFF1A1A1A)
)

val DarkHermesColors = ChatGPTDarkColors

val LocalHermesTheme = staticCompositionLocalOf { ChatGPTDarkColors }

private val EmeraldColorScheme = darkColorScheme(
    primary = DefaultEmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = DefaultEmeraldDark,
    onPrimaryContainer = Color.White,
    secondary = DefaultEmeraldAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF14241E),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = DefaultEmeraldBright,
    onTertiary = Color.Black,
    background = Color(0xFF070F0C),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0E1A15),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF14241E),
    onSurfaceVariant = Color(0xFF9EABA4),
    outline = Color(0x1FFFFFFF),
    outlineVariant = Color(0x3310A37F)
)

private val ChatGPTDarkColorScheme = darkColorScheme(
    primary = Color(0xFF10A37F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2F2F2F),
    onPrimaryContainer = Color(0xFFECECEC),
    secondary = Color(0xFF10A37F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF282828),
    onSecondaryContainer = Color(0xFFECECEC),
    tertiary = Color(0xFF10A37F),
    onTertiary = Color.White,
    background = Color(0xFF212121),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF171717),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF2F2F2F),
    onSurfaceVariant = Color(0xFFB4B4B4),
    outline = Color(0xFF383838),
    outlineVariant = Color(0xFF4B4B4B)
)

private val MuseLightColorScheme = lightColorScheme(
    primary = Color(0xFF1A1A1A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8ECEF),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = Color(0xFF1A1A1A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB4D8FC),
    onSecondaryContainer = Color(0xFF1A1A1A),
    tertiary = Color(0xFF8E8E93),
    onTertiary = Color.White,
    background = Color(0xFFE6E8EC),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0x18000000),
    outlineVariant = Color(0x18000000)
)

fun parseAccentColor(hexString: String?, defaultColor: Color): Color {
    if (hexString.isNullOrBlank()) return defaultColor
    val trimmed = hexString.trim()
    when (trimmed.lowercase()) {
        "emerald", "emerald green", "verde" -> return Color(0xFF10A37F)
        "electric cyan", "cyan" -> return Color(0xFF00D2FF)
        "royal blue", "blue" -> return Color(0xFF2563EB)
        "cyberpunk purple", "purple" -> return Color(0xFFA855F7)
        "rose crimson", "rose", "crimson" -> return Color(0xFFF43F5E)
        "amber gold", "amber", "gold" -> return Color(0xFFF59E0B)
        "sunset orange", "orange" -> return Color(0xFFFF6B35)
        "neon mint", "mint" -> return Color(0xFF00F5A0)
        "minimal white", "white" -> return Color(0xFFFFFFFF)
    }
    return try {
        val clean = trimmed.removePrefix("#")
        val colorLong = when (clean.length) {
            6 -> "FF$clean".toLong(16)
            8 -> clean.toLong(16)
            else -> return defaultColor
        }
        Color(colorLong)
    } catch (e: Exception) {
        defaultColor
    }
}

fun resolveAppThemeMode(themeName: String): HermesAppThemeMode {
    val lower = themeName.lowercase()
    return when {
        lower.contains("light") || lower.contains("alb") || lower.contains("muse") -> HermesAppThemeMode.LIGHT
        lower.contains("emerald") || lower.contains("verde") || lower.contains("nocturne") -> HermesAppThemeMode.EMERALD_ORIGINAL
        else -> HermesAppThemeMode.CHATGPT_DARK
    }
}

fun isLightAppTheme(themeName: String): Boolean {
    return resolveAppThemeMode(themeName) == HermesAppThemeMode.LIGHT
}

@Composable
fun MyApplicationTheme(
    appTheme: String = "ChatGPT Dark",
    accentColor: String = "#10A37F",
    content: @Composable () -> Unit
) {
    val themeMode = resolveAppThemeMode(appTheme)

    val (baseColorScheme, baseThemeColors) = when (themeMode) {
        HermesAppThemeMode.LIGHT -> Pair(MuseLightColorScheme, MuseLightColors)
        HermesAppThemeMode.EMERALD_ORIGINAL -> Pair(EmeraldColorScheme, DarkEmeraldColors)
        HermesAppThemeMode.CHATGPT_DARK -> Pair(ChatGPTDarkColorScheme, ChatGPTDarkColors)
    }

    val resolvedAccent = parseAccentColor(accentColor, baseThemeColors.accent)
    val isDefaultAccent = accentColor.isBlank() ||
        accentColor.equals("Emerald", ignoreCase = true) ||
        accentColor.equals("#10A37F", ignoreCase = true) ||
        accentColor.equals("#10B981", ignoreCase = true)

    val themeColors = baseThemeColors.copy(
        accent = if (themeMode == HermesAppThemeMode.LIGHT && isDefaultAccent) baseThemeColors.accent else resolvedAccent,
        primary = if (themeMode == HermesAppThemeMode.LIGHT) baseThemeColors.primary else resolvedAccent,
        borderAccent = if (themeMode == HermesAppThemeMode.LIGHT) baseThemeColors.borderAccent else resolvedAccent.copy(alpha = 0.45f),
        dockActiveTint = when (themeMode) {
            HermesAppThemeMode.LIGHT -> Color(0xFF1A1A1A)
            HermesAppThemeMode.CHATGPT_DARK -> if (isDefaultAccent) Color.White else resolvedAccent
            HermesAppThemeMode.EMERALD_ORIGINAL -> if (isDefaultAccent) Color(0xFF10B981) else resolvedAccent
        },
        headerButtonTint = when (themeMode) {
            HermesAppThemeMode.LIGHT -> Color(0xFF1A1A1A)
            HermesAppThemeMode.CHATGPT_DARK -> if (isDefaultAccent) Color(0xFFECECEC) else resolvedAccent
            HermesAppThemeMode.EMERALD_ORIGINAL -> if (isDefaultAccent) Color(0xFFECFDF5) else resolvedAccent
        }
    )

    val colorScheme = baseColorScheme.copy(
        primary = if (themeMode == HermesAppThemeMode.LIGHT) baseColorScheme.primary else resolvedAccent,
        onPrimary = Color.White,
        secondary = if (themeMode == HermesAppThemeMode.LIGHT && isDefaultAccent) baseColorScheme.secondary else resolvedAccent,
        tertiary = if (themeMode == HermesAppThemeMode.LIGHT && isDefaultAccent) baseColorScheme.tertiary else resolvedAccent
    )

    CompositionLocalProvider(
        LocalHermesTheme provides themeColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

