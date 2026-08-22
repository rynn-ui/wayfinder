package com.roadguardian.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadguardian.app.ui.theme.WayfinderDarkBackground
import com.roadguardian.app.ui.theme.WayfinderGlassBorder
import com.roadguardian.app.ui.theme.WayfinderGlassHighlight
import com.roadguardian.app.ui.theme.WayfinderGlassSurface
import com.roadguardian.app.ui.theme.WayfinderGlassSurfaceElevated
import com.roadguardian.app.ui.theme.WayfinderOnPrimary
import com.roadguardian.app.ui.theme.WayfinderPrimaryGreen
import com.roadguardian.app.ui.theme.WayfinderSage
import com.roadguardian.app.ui.theme.WayfinderTextPrimary
import com.roadguardian.app.ui.theme.WayfinderTextSecondary

/**
 * Ambient leafy-nature background with calm, slow-moving ambient lighting
 * and mist-like translucent depth layers.
 */
@Composable
fun NatureBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nature_drift")

    // Slow ambient glow shift
    val glowOffset1 by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_drift_1"
    )

    val glowOffset2 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = -0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_drift_2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WayfinderDarkBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top-right soft leafy glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        WayfinderPrimaryGreen.copy(alpha = 0.09f),
                        WayfinderMutedForestColor.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(width * (0.85f + glowOffset1), height * 0.15f),
                    radius = width * 0.9f
                )
            )

            // Bottom-left subtle sage glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        WayfinderSage.copy(alpha = 0.07f),
                        WayfinderDeepGreenColor.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    center = Offset(width * (0.15f + glowOffset2), height * 0.85f),
                    radius = width * 0.85f
                )
            )

            // Center faint mist diffusion
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFD5E7C8).copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.5f, height * 0.5f),
                    radius = width * 0.7f
                )
            )
        }

        content()
    }
}

private val WayfinderMutedForestColor = Color(0xFF506B58)
private val WayfinderDeepGreenColor = Color(0xFF263A2E)

/**
 * Reusable translucent glass card with soft borders and delicate gradient surface.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = WayfinderGlassSurface,
    borderColor: Color = WayfinderGlassBorder,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = shape,
                spotColor = Color.Black.copy(alpha = 0.35f),
                ambientColor = Color.Black.copy(alpha = 0.2f)
            )
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.16f),
                        backgroundColor.copy(alpha = 0.08f)
                    )
                )
            )
            .border(
                width = borderWidth,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        WayfinderGlassHighlight.copy(alpha = 0.22f),
                        borderColor.copy(alpha = 0.12f)
                    )
                ),
                shape = shape
            )
    ) {
        content()
    }
}

/**
 * Premium leafy-green glass action button with translucent depth,
 * specular rim highlight, and soft natural illumination.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    isDanger: Boolean = false
) {
    val containerBrush = when {
        isDanger -> Brush.verticalGradient(
            listOf(
                Color(0x40FF6B6B),
                Color(0x28C46054),
                Color(0x14101713)
            )
        )
        isPrimary -> Brush.verticalGradient(
            listOf(
                Color(0x38D5E7C8),
                Color(0x22A8C98F),
                Color(0x10101713)
            )
        )
        else -> Brush.verticalGradient(
            listOf(
                Color(0x22FFFFFF),
                Color(0x0CFFFFFF)
            )
        )
    }

    val borderBrush = when {
        isDanger -> Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.65f),
                Color(0x44C46054),
                Color.White.copy(alpha = 0.15f)
            )
        )
        isPrimary -> Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.70f),
                WayfinderPrimaryGreen.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.16f)
            )
        )
        else -> Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.45f),
                Color.White.copy(alpha = 0.12f)
            )
        )
    }

    val contentColor = WayfinderTextPrimary

    val iconTint = when {
        isDanger -> Color(0xFFFFB4AB)
        isPrimary -> WayfinderPrimaryGreen
        else -> WayfinderTextSecondary
    }

    val shadowSpotColor = when {
        isDanger -> Color(0xFFC46054).copy(alpha = 0.40f)
        isPrimary -> WayfinderPrimaryGreen.copy(alpha = 0.35f)
        else -> Color.Black.copy(alpha = 0.25f)
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            disabledContainerColor = Color.White.copy(alpha = 0.04f),
            disabledContentColor = WayfinderTextSecondary.copy(alpha = 0.4f)
        ),
        shape = CircleShape,
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = modifier
            .height(56.dp)
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = CircleShape,
                spotColor = shadowSpotColor,
                ambientColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(CircleShape)
            .background(containerBrush)
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = CircleShape
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = contentColor
            )
        }
    }
}

/**
 * Small translucent status badge / chip for active indicators.
 */
@Composable
fun GlassStatusChip(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color = WayfinderPrimaryGreen,
    animateDot: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "chip_dot_pulse")
    val dotAlpha by if (animateDot) {
        infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_alpha"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(1.0f) }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.06f)
                    )
                )
            )
            .border(
                1.dp,
                Color.White.copy(alpha = 0.16f),
                CircleShape
            )
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = dotAlpha))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            ),
            color = WayfinderTextPrimary
        )
    }
}

/**
 * Circular glass icon button with subtle border and elevation.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = WayfinderTextPrimary,
    containerColor: Color = WayfinderGlassSurfaceElevated,
    size: Dp = 48.dp
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        shape = CircleShape,
        color = containerColor.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, WayfinderGlassBorder),
        shadowElevation = 4.dp,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Subtle translucent horizontal divider for lists.
 */
@Composable
fun GlassDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        color = Color.White.copy(alpha = 0.08f),
        thickness = 1.dp,
        modifier = modifier
    )
}
