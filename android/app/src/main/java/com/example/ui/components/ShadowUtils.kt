package com.example.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a professional 360-degree ambient grey shadow around any UI element in Light Theme.
 * Ensures consistent grey contrast on top, bottom, left and right.
 */
fun Modifier.softGreyShadow(
    color: Color = Color(0x35000000),
    blur: Dp = 12.dp,
    offsetY: Dp = 1.dp,
    offsetX: Dp = 0.dp,
    spread: Dp = 0.5.dp,
    cornerRadius: Dp = 28.dp,
    isCircle: Boolean = false,
    enabled: Boolean = true
): Modifier = if (!enabled) this else this.drawBehind {
    if (color.alpha <= 0.001f) return@drawBehind
    val paint = Paint().apply {
        isAntiAlias = true
        this.color = color.toArgb()
        if (blur > 0.dp) {
            maskFilter = BlurMaskFilter(blur.toPx().coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
    }
    drawIntoCanvas { canvas ->
        val left = -spread.toPx() + offsetX.toPx()
        val top = -spread.toPx() + offsetY.toPx()
        val right = size.width + spread.toPx() + offsetX.toPx()
        val bottom = size.height + spread.toPx() + offsetY.toPx()
        if (isCircle) {
            val radius = (size.minDimension / 2f) + spread.toPx()
            val centerX = (size.width / 2f) + offsetX.toPx()
            val centerY = (size.height / 2f) + offsetY.toPx()
            canvas.nativeCanvas.drawCircle(centerX, centerY, radius, paint)
        } else {
            val cr = cornerRadius.toPx()
            canvas.nativeCanvas.drawRoundRect(left, top, right, bottom, cr, cr, paint)
        }
    }
}
