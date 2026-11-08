package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalHermesTheme

/**
 * Premium Apple Glass circle button used across settings screens & dialogs.
 * Identical in shape, size (42dp), frosted translucency, hairline glass border, and soft 360° shadow
 * to the top-left drawer menu button in HermesMainScreen.
 */
@Composable
fun AppleGlassBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    contentDescription: String = "Înapoi",
    testTag: String = "btn_apple_glass_back"
) {
    val currentTheme = LocalHermesTheme.current
    Surface(
        shape = CircleShape,
        color = currentTheme.headerButtonBg,
        border = BorderStroke(1.dp, currentTheme.headerButtonBorder),
        shadowElevation = if (currentTheme.isLight) 8.dp else 4.dp,
        modifier = modifier
            .size(42.dp)
            .softGreyShadow(
                color = Color(0x38000000),
                blur = 16.dp,
                spread = 0.5.dp,
                offsetY = 1.dp,
                isCircle = true,
                enabled = currentTheme.isLight
            )
            .shadow(
                elevation = if (currentTheme.isLight) 12.dp else 6.dp,
                shape = CircleShape,
                spotColor = if (currentTheme.isLight) Color(0x44000000) else Color(0x55000000),
                ambientColor = if (currentTheme.isLight) Color(0x30000000) else Color(0x38000000)
            )
            .clip(CircleShape)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (currentTheme.isLight) Color(0xFF1A1A1A) else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
