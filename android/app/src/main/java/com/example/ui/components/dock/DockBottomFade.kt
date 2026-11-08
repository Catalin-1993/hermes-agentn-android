package com.example.ui.components.dock

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standardized, high-fidelity progressive bottom fade for all menus, pages, and subpages.
 * As menu items, text, cards, and subpage contents scroll down toward the bottom dock area,
 * they smoothly and gradually lose opacity, becoming progressively less visible and almost
 * imperceptible at the very bottom edge behind the dock.
 *
 * Uses hardware-accelerated Offscreen compositing with BlendMode.DstIn to ensure natural,
 * theme-independent vertical alpha fading across Light, Dark, and Emerald Nocturne palettes.
 *
 * @param enabled Whether the fade effect is active (defaults to true).
 * @param fadeHeight Vertical height span of the fade zone (default 180.dp).
 */
fun Modifier.dockBottomGradientFade(
    enabled: Boolean = true,
    fadeHeight: Dp = 180.dp
): Modifier {
    if (!enabled) return this

    return this
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .drawWithContent {
            drawContent()
            val fadePx = fadeHeight.toPx()
            val fadeStartY = (size.height - fadePx).coerceAtLeast(0f)
            if (fadePx > 0f && size.height > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Black,                     // 100% visible before entering the lower dock zone
                            0.20f to Color.Black.copy(alpha = 0.88f), // Soft, subtle initial decay
                            0.45f to Color.Black.copy(alpha = 0.58f), // Noticeably softer text/content visibility
                            0.70f to Color.Black.copy(alpha = 0.28f), // Deep progressive fading entering the dock area
                            0.88f to Color.Black.copy(alpha = 0.08f), // Almost imperceptible behind the dock
                            1.00f to Color.Transparent               // Completely faded out at the very bottom edge
                        ),
                        startY = fadeStartY,
                        endY = size.height
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
        }
}
