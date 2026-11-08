package com.example.ui.components.avatar

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ultra-professional 3D props and visual activity indicators rendered alongside the avatar.
 * Supports smooth emergence progress (pulling out from pocket/side and stowing away gracefully).
 */
@Composable
fun Avatar3DPropOverlay(
    propType: AvatarPropType,
    accentColor: Color,
    avatarSize: Dp,
    modifier: Modifier = Modifier,
    emergenceProgress: Float = 1f
) {
    if (propType == AvatarPropType.NONE || emergenceProgress <= 0.01f) return

    val infiniteTransition = rememberInfiniteTransition(label = "prop_anim")
    
    // Slow, dignified floating wave (calm 2.2s cycle, not robotic)
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -1.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )
    
    // Slow, dignified rotation for tools
    val slowSpinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "slow_spin_angle"
    )
    
    // Subtle organic pulsation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Holographic shimmer wave for screens and book pages
    val shimmerWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_wave"
    )

    // Emergence dynamics: scales smoothly from 0.25 to 1.0, slides up from pocket position (+16dp)
    val emergenceScale = (0.25f + 0.75f * emergenceProgress).coerceIn(0.1f, 1f)
    val emergenceAlpha = emergenceProgress.coerceIn(0f, 1f)
    val emergenceSlideY = (1f - emergenceProgress) * 16f

    Box(
        modifier = modifier
            .size(avatarSize)
            .alpha(emergenceAlpha),
        contentAlignment = Alignment.Center
    ) {
        when (propType) {
            AvatarPropType.BOOK -> {
                // 3D Illuminated Codex with visible hands holding both pages
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(
                            x = 7.dp,
                            y = (-2 + floatOffset + emergenceSlideY).dp
                        )
                        .scale(emergenceScale * pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    // Page light rays rising softly toward avatar face
                    Canvas(modifier = Modifier.size(42.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.65f, size.height * 0.75f)
                            lineTo(size.width * 0.05f, size.height * 0.05f)
                            lineTo(size.width * 0.45f, 0f)
                            close()
                        }
                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x99FDE047),
                                    Color(0x33FDE047),
                                    Color.Transparent
                                )
                            )
                        )
                    }

                    // Open 3D Codex with gilded leather cover and parchment glow
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .shadow(8.dp, RoundedCornerShape(8.dp), spotColor = Color(0x66FDE047))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E3A8A), Color(0xFF1E40AF), Color(0xFF0F172A))
                                ),
                                RoundedCornerShape(8.dp)
                            )
                            .border(1.5.dp, Brush.linearGradient(listOf(Color(0xFFFDE047), Color(0xFF60A5FA))), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Carte 3D Studiata",
                            tint = Color(0xFFFEF08A),
                            modifier = Modifier.size(20.dp)
                        )

                        // Left Hand gripping book edge
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = (-3).dp)
                                .size(width = 6.dp, height = 12.dp)
                                .background(accentColor.copy(alpha = 0.9f), CircleShape)
                                .border(0.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                        )
                        // Right Hand gripping book edge
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .offset(x = 3.dp)
                                .size(width = 6.dp, height = 12.dp)
                                .background(accentColor.copy(alpha = 0.9f), CircleShape)
                                .border(0.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                        )
                    }
                }
            }

            AvatarPropType.LAPTOP -> {
                // 3D Ultra-thin Obsidian Slate & Glowing Matrix Terminal Screen with typing hands
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(
                            x = 8.dp,
                            y = (-1 + floatOffset + emergenceSlideY).dp
                        )
                        .scale(emergenceScale * pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 34.dp, height = 26.dp)
                            .shadow(8.dp, RoundedCornerShape(7.dp), spotColor = Color(0x7710B981))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF022C22), Color(0xFF0F172A))
                                ),
                                RoundedCornerShape(7.dp)
                            )
                            .border(1.2.dp, Color(0xFF34D399), RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Terminal Laptop 3D",
                            tint = Color(0xFF6EE7B7),
                            modifier = Modifier.size(18.dp)
                        )

                        // Animated Typing Hands over Keyboard
                        val typingOffset = if ((shimmerWave * 10).toInt() % 2 == 0) (-1).dp else 1.dp
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 7.dp, height = 5.dp)
                                    .offset(y = typingOffset)
                                    .background(accentColor.copy(alpha = 0.95f), CircleShape)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 7.dp, height = 5.dp)
                                    .offset(y = -typingOffset)
                                    .background(accentColor.copy(alpha = 0.95f), CircleShape)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                            )
                        }
                    }
                }
            }

            AvatarPropType.MAGNIFIER -> {
                // 3D Polished Brass & Sapphire Glass Magnifier with gripping hand scanning gracefully
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(
                            x = 6.dp,
                            y = (floatOffset + emergenceSlideY).dp
                        )
                        .scale(emergenceScale * pulseScale)
                        .rotate(14f + floatOffset * 1.5f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .shadow(6.dp, CircleShape, spotColor = Color(0x66F59E0B))
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFFBBF24), Color(0xFFD97706), Color(0xFF78350F))
                                ),
                                CircleShape
                            )
                            .border(1.5.dp, Color(0xFFFFFBEB), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Lupă 3D de Cercetare",
                            tint = Color(0xFF451A03),
                            modifier = Modifier.size(18.dp)
                        )

                        // Hand gripping the handle of the magnifying glass
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 2.dp, y = 2.dp)
                                .size(width = 7.dp, height = 7.dp)
                                .background(accentColor.copy(alpha = 0.95f), CircleShape)
                                .border(0.6.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                        )
                    }
                }
            }

            AvatarPropType.TOOL -> {
                // 3D Aerospace Chrome & Flame Orange Precision Tool with gripping hand turning with torque
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(
                            x = (-4).dp,
                            y = (-2 + floatOffset + emergenceSlideY).dp
                        )
                        .scale(emergenceScale * pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(29.dp)
                            .shadow(6.dp, CircleShape, spotColor = Color(0x66F97316))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFEA580C), Color(0xFFC2410C), Color(0xFF7C2D12))
                                ),
                                CircleShape
                            )
                            .border(1.4.dp, Color(0xFFFED7AA), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Unealtă 3D de Precizie",
                            tint = Color.White,
                            modifier = Modifier
                                .size(17.dp)
                                .rotate(slowSpinAngle)
                        )

                        // Hand holding tool handle
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = (-2).dp)
                                .size(width = 6.dp, height = 9.dp)
                                .background(accentColor.copy(alpha = 0.95f), CircleShape)
                                .border(0.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                        )
                    }
                }
            }

            AvatarPropType.LIGHTBULB -> {
                // 3D Incandescent Stardust Idea Beacon with thoughtful hand poised under chin
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(
                            y = (-14 + floatOffset - (1f - emergenceProgress) * 12f).dp
                        )
                        .scale(emergenceScale * pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .shadow(10.dp, CircleShape, spotColor = Color(0xAAFEF08A))
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFFEF08A), Color(0xFFFACC15), Color(0xFFCA8A04))
                                ),
                                CircleShape
                            )
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Idee & Inspirație 3D",
                            tint = Color(0xFF713F12),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Pondering hand near chin
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(x = 10.dp, y = 24.dp)
                            .size(width = 8.dp, height = 7.dp)
                            .background(accentColor.copy(alpha = 0.95f), CircleShape)
                            .border(0.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    )
                }
            }

            AvatarPropType.SHIELD -> {
                // Golden Approval Shield with soft energy rim
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(
                            x = 6.dp,
                            y = (floatOffset + emergenceSlideY).dp
                        )
                        .scale(emergenceScale * pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(27.dp)
                            .shadow(6.dp, CircleShape, spotColor = Color(0x6610B981))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF10B981), Color(0xFF047857))
                                ),
                                CircleShape
                            )
                            .border(1.4.dp, Color(0xFFA7F3D0), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirmare 3D",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            AvatarPropType.STARS -> {
                // Celebration Sparks & Confetti Burst with gentle float
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(
                            x = 8.dp,
                            y = (-8 + floatOffset + emergenceSlideY).dp
                        )
                        .scale(emergenceScale * pulseScale)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Succes 3D",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(23.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = (-6).dp,
                            y = (-6 - floatOffset + emergenceSlideY).dp
                        )
                        .scale(emergenceScale * pulseScale * 0.85f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Succes 3D",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            AvatarPropType.HEARTS -> {
                // Floating Love Hearts on Petting / Long Press
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(
                            y = (-14 + floatOffset - (1f - emergenceProgress) * 10f).dp
                        )
                        .scale(emergenceScale * pulseScale)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Iubire",
                        tint = Color(0xFFF43F5E),
                        modifier = Modifier.size(23.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(
                            x = 8.dp,
                            y = (-6 - floatOffset).dp
                        )
                        .scale(emergenceScale * pulseScale * 0.75f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Iubire",
                        tint = Color(0xFFFB7185),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            AvatarPropType.SWEAT_DROP -> {
                // Comic Confusion / Sweat Droplet
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(
                            x = 6.dp,
                            y = (-4 + floatOffset + emergenceSlideY).dp
                        )
                        .size(15.dp)
                        .scale(emergenceScale)
                ) {
                    val path = Path().apply {
                        moveTo(size.width * 0.5f, 0f)
                        cubicTo(
                            size.width, size.height * 0.5f,
                            size.width, size.height,
                            size.width * 0.5f, size.height
                        )
                        cubicTo(
                            0f, size.height,
                            0f, size.height * 0.5f,
                            size.width * 0.5f, 0f
                        )
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF67E8F9), Color(0xFF0284C7))
                        )
                    )
                }
            }

            AvatarPropType.SPARKLE -> {
                // Orbiting Stardust Sparkles with gentle planetary rotation
                Canvas(
                    modifier = Modifier
                        .size(avatarSize + 12.dp)
                        .scale(emergenceScale)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.width / 2f - 4f
                    val angleRad = Math.toRadians(slowSpinAngle.toDouble())
                    val spark1 = Offset(
                        (center.x + radius * cos(angleRad)).toFloat(),
                        (center.y + radius * sin(angleRad)).toFloat()
                    )
                    val spark2 = Offset(
                        (center.x + radius * cos(angleRad + Math.PI)).toFloat(),
                        (center.y + radius * sin(angleRad + Math.PI)).toFloat()
                    )

                    drawCircle(
                        color = accentColor.copy(alpha = 0.85f * emergenceAlpha),
                        radius = 2.8f,
                        center = spark1
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.95f * emergenceAlpha),
                        radius = 2.2f,
                        center = spark2
                    )
                }
            }

            AvatarPropType.NONE -> {}
        }
    }
}

/**
 * Vector-rendered accessory overlays for avatar personalization:
 * - Smart Glasses
 * - Designer Monocle
 * - Studio Headphones
 * - Halo Spark
 */
@Composable
fun AvatarAccessoryOverlay(
    accessory: String,
    avatarSize: Dp,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (accessory.isBlank() || accessory.equals("None", ignoreCase = true) || accessory.equals("Niciunul", ignoreCase = true)) return

    Box(
        modifier = modifier.size(avatarSize),
        contentAlignment = Alignment.Center
    ) {
        when {
            accessory.contains("Glasses", ignoreCase = true) || accessory.contains("Ochelari", ignoreCase = true) -> {
                // Sleek Smart Glasses over eyes
                Canvas(
                    modifier = Modifier
                        .size(width = avatarSize * 0.65f, height = avatarSize * 0.28f)
                        .offset(y = (-avatarSize * 0.12f))
                ) {
                    val frameColor = Color(0xFF1E293B)
                    val lensColor = Color(0x3338BDF8)
                    val stroke = Stroke(width = 1.6.dp.toPx())
                    // Left Lens
                    drawRoundRect(
                        color = lensColor,
                        topLeft = Offset(2f, 2f),
                        size = Size(size.width * 0.44f, size.height - 4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                    )
                    drawRoundRect(
                        color = frameColor,
                        topLeft = Offset(2f, 2f),
                        size = Size(size.width * 0.44f, size.height - 4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                        style = stroke
                    )
                    // Right Lens
                    drawRoundRect(
                        color = lensColor,
                        topLeft = Offset(size.width * 0.54f, 2f),
                        size = Size(size.width * 0.44f, size.height - 4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                    )
                    drawRoundRect(
                        color = frameColor,
                        topLeft = Offset(size.width * 0.54f, 2f),
                        size = Size(size.width * 0.44f, size.height - 4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                        style = stroke
                    )
                    // Bridge
                    drawLine(
                        color = frameColor,
                        start = Offset(size.width * 0.44f, size.height * 0.45f),
                        end = Offset(size.width * 0.54f, size.height * 0.45f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            accessory.contains("Monocle", ignoreCase = true) -> {
                // Cyber Monocle on right eye
                Canvas(
                    modifier = Modifier
                        .size(avatarSize * 0.32f)
                        .offset(x = avatarSize * 0.15f, y = (-avatarSize * 0.12f))
                ) {
                    drawCircle(
                        color = Color(0x44F59E0B),
                        radius = size.minDimension / 2f - 2f
                    )
                    drawCircle(
                        color = Color(0xFFF59E0B),
                        radius = size.minDimension / 2f - 2f,
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                    // Crosshair
                    drawLine(
                        color = Color(0xAAFDE047),
                        start = Offset(size.width / 2f, 4f),
                        end = Offset(size.width / 2f, size.height - 4f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            accessory.contains("Headphones", ignoreCase = true) || accessory.contains("Căști", ignoreCase = true) -> {
                // Studio Headphones wrapping over head
                Canvas(
                    modifier = Modifier.size(avatarSize * 0.95f)
                ) {
                    val hpColor = Color(0xFF0F172A)
                    val accentHp = accentColor
                    // Headband arch
                    drawArc(
                        color = hpColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx()),
                        topLeft = Offset(size.width * 0.15f, size.height * 0.05f),
                        size = Size(size.width * 0.7f, size.height * 0.65f)
                    )
                    // Left Earcup
                    drawRoundRect(
                        color = accentHp,
                        topLeft = Offset(size.width * 0.08f, size.height * 0.38f),
                        size = Size(size.width * 0.14f, size.height * 0.28f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                    // Right Earcup
                    drawRoundRect(
                        color = accentHp,
                        topLeft = Offset(size.width * 0.78f, size.height * 0.38f),
                        size = Size(size.width * 0.14f, size.height * 0.28f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                }
            }
            accessory.contains("Halo", ignoreCase = true) || accessory.contains("Aură", ignoreCase = true) -> {
                // Radiant Celestial Halo floating over head
                Canvas(
                    modifier = Modifier
                        .size(width = avatarSize * 0.7f, height = avatarSize * 0.22f)
                        .offset(y = (-avatarSize * 0.38f))
                ) {
                    drawOval(
                        color = Color(0xFFFDE047),
                        style = Stroke(width = 2.2.dp.toPx()),
                        topLeft = Offset(2f, 2f),
                        size = Size(size.width - 4f, size.height - 4f)
                    )
                }
            }
        }
    }
}
