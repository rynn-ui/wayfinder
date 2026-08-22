package com.roadguardian.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.roadguardian.app.ui.theme.WayfinderDarkSurfaceContainerLowest
import com.roadguardian.app.ui.theme.WayfinderDeepNaturalGreen
import com.roadguardian.app.ui.theme.WayfinderLightGreenery
import com.roadguardian.app.ui.theme.WayfinderMutedForest
import com.roadguardian.app.ui.theme.WayfinderPrimaryGreen
import com.roadguardian.app.ui.theme.WayfinderSage

/**
 * AmbientNatureLayer
 *
 * Adds a clean, minimal, and subtle nature atmosphere to the Wayfinder HomeScreen:
 * - Soft organic leaf silhouettes framing the outer edges of the screen
 * - Sparse, delicate drizzle streaks and soft mist droplets
 * - Natural ambient green illumination and soft edge vignettes
 * - Slow, calm 60 FPS motion simulating a gentle post-rain breeze
 *
 * Designed to keep the center completely pristine for primary navigation and status UI.
 */
@Composable
fun AmbientNatureLayer(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_nature_atmosphere")

    // Ultra-slow breeze drift (14-second cycle)
    val breezeSway by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "leaf_breeze_sway"
    )

    // Gentle ambient light breath (12-second cycle)
    val lightBreath by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_light_breath"
    )

    // Slow rain drizzle continuous progress (8-second cycle)
    val rainProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_drizzle_progress"
    )

    // Immutable sparse drizzle particle layout (strictly positioned outside center focus)
    val rainParticles = remember {
        listOf(
            // Left outer column
            RainParticle(relX = 0.05f, relY = 0.10f, lengthDp = 18f, speed = 1.0f, alpha = 0.18f),
            RainParticle(relX = 0.11f, relY = 0.35f, lengthDp = 24f, speed = 1.15f, alpha = 0.20f),
            RainParticle(relX = 0.07f, relY = 0.62f, lengthDp = 20f, speed = 0.9f, alpha = 0.16f),
            RainParticle(relX = 0.14f, relY = 0.80f, lengthDp = 16f, speed = 1.1f, alpha = 0.18f),
            RainParticle(relX = 0.03f, relY = 0.92f, lengthDp = 22f, speed = 1.05f, alpha = 0.19f),

            // Right outer column
            RainParticle(relX = 0.88f, relY = 0.07f, lengthDp = 20f, speed = 1.05f, alpha = 0.19f),
            RainParticle(relX = 0.94f, relY = 0.26f, lengthDp = 26f, speed = 1.2f, alpha = 0.22f),
            RainParticle(relX = 0.86f, relY = 0.50f, lengthDp = 18f, speed = 0.95f, alpha = 0.16f),
            RainParticle(relX = 0.93f, relY = 0.72f, lengthDp = 24f, speed = 1.1f, alpha = 0.20f),
            RainParticle(relX = 0.85f, relY = 0.88f, lengthDp = 18f, speed = 1.0f, alpha = 0.17f),

            // Top / Bottom peripheral whispers
            RainParticle(relX = 0.30f, relY = 0.03f, lengthDp = 15f, speed = 0.85f, alpha = 0.14f),
            RainParticle(relX = 0.70f, relY = 0.05f, lengthDp = 17f, speed = 0.95f, alpha = 0.15f),
            RainParticle(relX = 0.26f, relY = 0.96f, lengthDp = 16f, speed = 1.0f, alpha = 0.15f),
            RainParticle(relX = 0.74f, relY = 0.95f, lengthDp = 18f, speed = 1.1f, alpha = 0.16f)
        )
    }

    // Dew / mist droplets on outer periphery & leaf edges
    val mistDroplets = remember {
        listOf(
            MistDroplet(relX = 0.17f, relY = 0.16f, radiusDp = 2.8f, alpha = 0.26f),
            MistDroplet(relX = 0.08f, relY = 0.24f, radiusDp = 2.2f, alpha = 0.22f),
            MistDroplet(relX = 0.80f, relY = 0.82f, radiusDp = 3.0f, alpha = 0.28f),
            MistDroplet(relX = 0.90f, relY = 0.76f, radiusDp = 2.4f, alpha = 0.24f),
            MistDroplet(relX = 0.10f, relY = 0.82f, radiusDp = 2.0f, alpha = 0.20f),
            MistDroplet(relX = 0.88f, relY = 0.14f, radiusDp = 2.2f, alpha = 0.22f)
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Soft Ambient Green Illumination & Edge Vignette
        drawAmbientAtmosphere(
            width = width,
            height = height,
            breath = lightBreath
        )

        // 2. Outer Edge Leaf Silhouettes (Upper-Left, Lower-Right, Upper-Right, Lower-Left)
        drawFoliageSilhouettes(
            width = width,
            height = height,
            sway = breezeSway,
            breath = lightBreath
        )

        // 3. Sparse Soft Rain Drizzle Streaks & Gentle Mist Droplets
        drawSoftRainAtmosphere(
            width = width,
            height = height,
            rainProgress = rainProgress,
            particles = rainParticles,
            droplets = mistDroplets
        )
    }
}

/**
 * Renders subtle ambient illumination and gentle natural depth layers around borders.
 * Uses soft linear gradients without any circular shapes in the background.
 */
private fun DrawScope.drawAmbientAtmosphere(
    width: Float,
    height: Float,
    breath: Float
) {
    // Subtle top canopy light (linear downward dissipation)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                WayfinderPrimaryGreen.copy(alpha = 0.06f * breath),
                WayfinderSage.copy(alpha = 0.02f * breath),
                Color.Transparent
            ),
            startY = 0f,
            endY = height * 0.20f
        )
    )

    // Subtle bottom forest vignette (linear upward dissipation)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                WayfinderDeepNaturalGreen.copy(alpha = 0.04f * breath),
                WayfinderDarkSurfaceContainerLowest.copy(alpha = 0.20f)
            ),
            startY = height * 0.75f,
            endY = height
        )
    )
}

/**
 * Draws soft organic leaf silhouettes bleeding off screen edges.
 * Visible, elegant, organic, yet non-intrusive.
 */
private fun DrawScope.drawFoliageSilhouettes(
    width: Float,
    height: Float,
    sway: Float,
    breath: Float
) {
    val swayPx = sway * density

    // -------------------------------------------------------------
    // Upper-Left Foliage (Canopy Leaf extending downward-right)
    // -------------------------------------------------------------
    val ulLeafPath = Path().apply {
        // Base starting off top-left screen
        moveTo(-width * 0.05f + swayPx, -height * 0.03f)
        // Upper convex curve
        cubicTo(
            width * 0.06f + swayPx, height * 0.01f,
            width * 0.18f + (swayPx * 1.4f), height * 0.06f,
            width * 0.25f + (swayPx * 1.8f), height * 0.16f // Leaf tip
        )
        // Lower return curve
        cubicTo(
            width * 0.16f + (swayPx * 1.2f), height * 0.18f,
            width * 0.02f + swayPx, height * 0.13f,
            -width * 0.05f + swayPx, height * 0.07f
        )
        close()
    }

    drawPath(
        path = ulLeafPath,
        brush = Brush.linearGradient(
            colors = listOf(
                WayfinderPrimaryGreen.copy(alpha = 0.22f * breath),
                WayfinderSage.copy(alpha = 0.14f * breath),
                WayfinderMutedForest.copy(alpha = 0.06f * breath),
                Color.Transparent
            ),
            start = Offset(-width * 0.03f, -height * 0.03f),
            end = Offset(width * 0.27f, height * 0.18f)
        ),
        style = Fill
    )

    // Delicate leaf vein for organic structure
    val ulVeinPath = Path().apply {
        moveTo(-width * 0.03f + swayPx, -height * 0.01f)
        quadraticTo(
            width * 0.10f + swayPx, height * 0.07f,
            width * 0.23f + (swayPx * 1.6f), height * 0.15f
        )
    }
    drawPath(
        path = ulVeinPath,
        brush = Brush.linearGradient(
            listOf(
                WayfinderLightGreenery.copy(alpha = 0.22f),
                Color.Transparent
            ),
            start = Offset(0f, 0f),
            end = Offset(width * 0.24f, height * 0.16f)
        ),
        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
    )

    // Secondary smaller companion leaf in upper-left
    val ulSmallLeaf = Path().apply {
        moveTo(-width * 0.04f, height * 0.09f)
        cubicTo(
            width * 0.04f + swayPx, height * 0.11f,
            width * 0.12f + swayPx, height * 0.18f,
            width * 0.16f + (swayPx * 1.3f), height * 0.22f
        )
        cubicTo(
            width * 0.09f + swayPx, height * 0.22f,
            -width * 0.01f, height * 0.18f,
            -width * 0.04f, height * 0.14f
        )
        close()
    }
    drawPath(
        path = ulSmallLeaf,
        brush = Brush.linearGradient(
            colors = listOf(
                WayfinderSage.copy(alpha = 0.18f * breath),
                WayfinderDeepNaturalGreen.copy(alpha = 0.08f * breath),
                Color.Transparent
            ),
            start = Offset(-width * 0.03f, height * 0.09f),
            end = Offset(width * 0.17f, height * 0.23f)
        )
    )

    // -------------------------------------------------------------
    // Lower-Right Foliage (Faint leaf silhouette rising upward-left)
    // -------------------------------------------------------------
    val lrLeafPath = Path().apply {
        // Base at bottom-right corner
        moveTo(width * 1.06f - swayPx, height * 1.03f)
        cubicTo(
            width * 0.95f - swayPx, height * 0.92f,
            width * 0.85f - (swayPx * 1.4f), height * 0.85f,
            width * 0.74f - (swayPx * 1.8f), height * 0.77f // Leaf tip
        )
        cubicTo(
            width * 0.84f - (swayPx * 1.2f), height * 0.74f,
            width * 0.98f - swayPx, height * 0.81f,
            width * 1.06f - swayPx, height * 0.90f
        )
        close()
    }

    drawPath(
        path = lrLeafPath,
        brush = Brush.linearGradient(
            colors = listOf(
                WayfinderPrimaryGreen.copy(alpha = 0.20f * breath),
                WayfinderSage.copy(alpha = 0.12f * breath),
                WayfinderMutedForest.copy(alpha = 0.05f * breath),
                Color.Transparent
            ),
            start = Offset(width * 1.05f, height * 1.02f),
            end = Offset(width * 0.73f, height * 0.76f)
        ),
        style = Fill
    )

    // Vein for lower-right leaf
    val lrVeinPath = Path().apply {
        moveTo(width * 1.03f - swayPx, height * 1.0f)
        quadraticTo(
            width * 0.89f - swayPx, height * 0.87f,
            width * 0.76f - (swayPx * 1.6f), height * 0.78f
        )
    }
    drawPath(
        path = lrVeinPath,
        brush = Brush.linearGradient(
            listOf(
                WayfinderLightGreenery.copy(alpha = 0.20f),
                Color.Transparent
            ),
            start = Offset(width * 1.0f, height * 0.98f),
            end = Offset(width * 0.76f, height * 0.78f)
        ),
        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
    )

    // -------------------------------------------------------------
    // Upper-Right Canopy Whisper (Partial leaf tip off-edge)
    // -------------------------------------------------------------
    val urLeafPath = Path().apply {
        moveTo(width * 1.05f, -height * 0.03f)
        cubicTo(
            width * 0.93f - swayPx, height * 0.01f,
            width * 0.85f - swayPx, height * 0.06f,
            width * 0.80f - (swayPx * 1.3f), height * 0.12f
        )
        cubicTo(
            width * 0.89f - swayPx, height * 0.13f,
            width * 0.99f, height * 0.07f,
            width * 1.05f, height * 0.03f
        )
        close()
    }
    drawPath(
        path = urLeafPath,
        brush = Brush.linearGradient(
            colors = listOf(
                WayfinderSage.copy(alpha = 0.16f * breath),
                WayfinderMutedForest.copy(alpha = 0.06f * breath),
                Color.Transparent
            ),
            start = Offset(width * 1.03f, 0f),
            end = Offset(width * 0.79f, height * 0.13f)
        )
    )

    // -------------------------------------------------------------
    // Lower-Left Edge Whisper (Partial foliage near lower-left margin)
    // -------------------------------------------------------------
    val llLeafPath = Path().apply {
        moveTo(-width * 0.05f, height * 0.72f)
        cubicTo(
            width * 0.05f + swayPx, height * 0.74f,
            width * 0.13f + swayPx, height * 0.80f,
            width * 0.17f + (swayPx * 1.2f), height * 0.86f
        )
        cubicTo(
            width * 0.08f + swayPx, height * 0.88f,
            -width * 0.01f, height * 0.83f,
            -width * 0.05f, height * 0.78f
        )
        close()
    }
    drawPath(
        path = llLeafPath,
        brush = Brush.linearGradient(
            colors = listOf(
                WayfinderPrimaryGreen.copy(alpha = 0.17f * breath),
                WayfinderSage.copy(alpha = 0.09f * breath),
                Color.Transparent
            ),
            start = Offset(-width * 0.03f, height * 0.72f),
            end = Offset(width * 0.18f, height * 0.87f)
        )
    )
}

/**
 * Renders sparse drizzle streaks and delicate mist droplets
 * to evoke fresh post-rain atmosphere.
 */
private fun DrawScope.drawSoftRainAtmosphere(
    width: Float,
    height: Float,
    rainProgress: Float,
    particles: List<RainParticle>,
    droplets: List<MistDroplet>
) {
    // 1. Draw sparse downward-drifting drizzle streaks
    for (particle in particles) {
        val yOffset = ((particle.relY + rainProgress * particle.speed) % 1.0f) * height
        val xOffset = particle.relX * width
        val lengthPx = particle.lengthDp.dp.toPx()

        // Vertical line with soft gradient fade on ends
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    WayfinderLightGreenery.copy(alpha = particle.alpha),
                    Color.White.copy(alpha = particle.alpha * 0.7f),
                    Color.Transparent
                ),
                startY = yOffset,
                endY = yOffset + lengthPx
            ),
            start = Offset(xOffset, yOffset),
            end = Offset(xOffset, yOffset + lengthPx),
            strokeWidth = 1.2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // 2. Draw mist / dew droplets on outer corners and leaf margins
    for (droplet in droplets) {
        val cx = droplet.relX * width
        val cy = droplet.relY * height
        val radiusPx = droplet.radiusDp.dp.toPx()

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = droplet.alpha),
                    WayfinderLightGreenery.copy(alpha = droplet.alpha * 0.6f),
                    WayfinderSage.copy(alpha = droplet.alpha * 0.2f),
                    Color.Transparent
                ),
                center = Offset(cx - radiusPx * 0.3f, cy - radiusPx * 0.3f),
                radius = radiusPx * 1.3f
            ),
            radius = radiusPx,
            center = Offset(cx, cy)
        )
    }
}

/**
 * Data model for a single sparse rain streak.
 */
private data class RainParticle(
    val relX: Float,
    val relY: Float,
    val lengthDp: Float,
    val speed: Float,
    val alpha: Float
)

/**
 * Data model for a subtle mist / dew droplet on the edge foliage.
 */
private data class MistDroplet(
    val relX: Float,
    val relY: Float,
    val radiusDp: Float,
    val alpha: Float
)
