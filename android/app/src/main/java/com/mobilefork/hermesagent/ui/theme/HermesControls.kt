package com.mobilefork.hermesagent.ui.theme

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.material3.TextButton as MaterialTextButton

/**
 * The app's glass surface: a translucent vertical fill, a bright rim and a discreet shadow
 * drawn only outside the shape. Android's elevation shadow would show through a translucent
 * fill as a dark patch, so the shadow is painted here and clipped out of the interior.
 */
fun Modifier.hermesGlass(
    shape: Shape,
    fillTop: Color,
    fillBottom: Color,
    rimTop: Color,
    rimBottom: Color,
    shadowColor: Color,
    shadowRadius: Dp = 12.dp,
    shadowOffsetY: Dp = 3.dp,
): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }
    val fill = Brush.verticalGradient(listOf(fillTop, fillBottom))
    val rim = Brush.verticalGradient(listOf(rimTop, rimBottom))
    val rimStroke = Stroke(width = 1.dp.toPx())
    // Paint.setShadowLayer on a path is only hardware-accelerated from Android 9.
    val shadowPaint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || Build.FINGERPRINT == "robolectric") {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            setShadowLayer(shadowRadius.toPx(), 0f, shadowOffsetY.toPx(), shadowColor.toArgb())
        }
    } else {
        null
    }
    onDrawBehind {
        if (shadowPaint != null) {
            clipPath(path, clipOp = ClipOp.Difference) {
                drawIntoCanvas { it.nativeCanvas.drawPath(path.asAndroidPath(), shadowPaint) }
            }
        }
        drawPath(path, fill)
        drawPath(path, rim, style = rimStroke)
    }
}

/** The glass surface in the current theme's colours. */
@Composable
fun Modifier.hermesGlassSurface(
    shape: Shape,
    shadowRadius: Dp = 12.dp,
): Modifier {
    val glass = LocalHermesGlassTokens.current
    return hermesGlass(
        shape = shape,
        fillTop = glass.glassFillTop,
        fillBottom = glass.glassFillBottom,
        rimTop = glass.glassRimTop,
        rimBottom = glass.glassRimBottom,
        shadowColor = glass.softShadow,
        shadowRadius = shadowRadius,
    )
}

/** A press settles the control slightly inward and springs back, on every control alike. */
@Composable
fun Modifier.hermesPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    val glass = LocalHermesGlassTokens.current
    val base = if (enabled) colors.containerColor else colors.disabledContainerColor
    val top = if (base.alpha >= 0.99f) base.copy(alpha = 0.96f) else base
    val bottom = if (base.alpha >= 0.99f) base.copy(alpha = 0.84f) else base
    MaterialButton(
        onClick = onClick,
        modifier = modifier
            .hermesPressScale(interaction)
            .hermesGlass(
                shape = shape,
                fillTop = top,
                fillBottom = bottom,
                rimTop = glass.glassRimTop.copy(alpha = 0.45f),
                rimBottom = glass.glassRimBottom,
                shadowColor = if (enabled) glass.softShadow else Color.Transparent,
                shadowRadius = 10.dp,
            ),
        enabled = enabled,
        shape = shape,
        colors = colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interaction,
        content = content,
    )
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    MaterialOutlinedButton(
        onClick = onClick,
        modifier = modifier
            .hermesPressScale(interaction)
            .hermesGlassSurface(shape, shadowRadius = 10.dp),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interaction,
        content = content,
    )
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    MaterialTextButton(
        onClick = onClick,
        modifier = modifier.hermesPressScale(interaction),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interaction,
        content = content,
    )
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    MaterialIconButton(
        onClick = onClick,
        modifier = modifier.hermesPressScale(interaction, pressedScale = 0.9f),
        enabled = enabled,
        colors = colors,
        interactionSource = interaction,
        content = content,
    )
}
