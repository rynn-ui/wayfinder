package com.roadguardian.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadguardian.app.ui.components.GlassButton
import com.roadguardian.app.ui.components.GlassCard
import com.roadguardian.app.ui.components.GlassIconButton
import com.roadguardian.app.ui.theme.WayfinderDarkBackground
import com.roadguardian.app.ui.theme.WayfinderDarkSurface
import com.roadguardian.app.ui.theme.WayfinderGlassBorder
import com.roadguardian.app.ui.theme.WayfinderGlassHighlight
import com.roadguardian.app.ui.theme.WayfinderHazardCrack
import com.roadguardian.app.ui.theme.WayfinderHazardPothole
import com.roadguardian.app.ui.theme.WayfinderMutedForest
import com.roadguardian.app.ui.theme.WayfinderPrimaryGreen
import com.roadguardian.app.ui.theme.WayfinderSage
import com.roadguardian.app.ui.theme.WayfinderTextPrimary
import com.roadguardian.app.ui.theme.WayfinderTextSecondary

data class MapHazardItem(
    val id: String,
    val title: String,
    val typeName: String,
    val location: String,
    val timeAgo: String,
    val icon: ImageVector,
    val normalizedX: Float,
    val normalizedY: Float,
    val isCritical: Boolean = true
)

@Composable
fun RoadHealthMapScreen(
    hazards: List<MapHazardItem> = defaultMapHazards,
    modifier: Modifier = Modifier
) {
    var selectedHazard by remember { mutableStateOf<MapHazardItem?>(null) }
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "map_animations")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    val userPingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "user_ping"
    )

    val userPingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "user_ping_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WayfinderDarkBackground)
    ) {
        // Nature Road Grid Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Background roads (Muted forest & sage tones)
            val secondaryRoadColor = WayfinderMutedForest.copy(alpha = 0.22f)
            val majorRoadColor = WayfinderSage.copy(alpha = 0.45f)

            // Major arterial roads
            drawLine(
                color = majorRoadColor,
                start = Offset(0f, height * 0.35f),
                end = Offset(width, height * 0.35f),
                strokeWidth = 24f * zoomLevel
            )
            drawLine(
                color = majorRoadColor,
                start = Offset(0f, height * 0.68f),
                end = Offset(width, height * 0.68f),
                strokeWidth = 22f * zoomLevel
            )
            drawLine(
                color = majorRoadColor,
                start = Offset(width * 0.32f, 0f),
                end = Offset(width * 0.32f, height),
                strokeWidth = 24f * zoomLevel
            )
            drawLine(
                color = majorRoadColor,
                start = Offset(width * 0.72f, 0f),
                end = Offset(width * 0.72f, height),
                strokeWidth = 20f * zoomLevel
            )

            // Secondary street grid
            val gridSpacing = 64f * zoomLevel
            var y = 0f
            while (y < height) {
                drawLine(
                    color = secondaryRoadColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 8f * zoomLevel
                )
                y += gridSpacing
            }
            var x = 0f
            while (x < width) {
                drawLine(
                    color = secondaryRoadColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 8f * zoomLevel
                )
                x += gridSpacing
            }
        }

        // Hazard Markers Layer
        Box(modifier = Modifier.fillMaxSize()) {
            hazards.forEach { hazard ->
                HazardMapMarker(
                    hazard = hazard,
                    pulseScale = pulseScale,
                    pulseAlpha = pulseAlpha,
                    onClick = { selectedHazard = hazard },
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }

            // User Location Marker (Center)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                // Expanding Sage/Green Radar Ping
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .scale(userPingScale)
                        .clip(CircleShape)
                        .background(WayfinderPrimaryGreen.copy(alpha = userPingAlpha))
                )

                // Outer soft glowing ring
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(WayfinderPrimaryGreen.copy(alpha = 0.3f))
                )

                // Central Leafy Green Dot
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(WayfinderPrimaryGreen)
                        .border(2.dp, Color.White, CircleShape)
                )
            }
        }

        // Floating Map Controls (Zoom In, Zoom Out, Center)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlassIconButton(
                icon = Icons.Filled.Add,
                contentDescription = "Zoom In",
                onClick = { zoomLevel = (zoomLevel + 0.2f).coerceAtMost(2.0f) }
            )

            GlassIconButton(
                icon = Icons.Filled.Remove,
                contentDescription = "Zoom Out",
                onClick = { zoomLevel = (zoomLevel - 0.2f).coerceAtLeast(0.6f) }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // My Location button with Leafy Green highlight
            Surface(
                shape = CircleShape,
                color = WayfinderPrimaryGreen,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            ) {
                IconButton(onClick = { /* Center Map */ }) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = "My Location",
                        tint = WayfinderDarkBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Sliding Glass Bottom Sheet for Hazard Details
        AnimatedVisibility(
            visible = selectedHazard != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedHazard?.let { item ->
                HazardDetailsBottomSheet(
                    item = item,
                    onClose = { selectedHazard = null },
                    onConfirm = { selectedHazard = null },
                    onReroute = { selectedHazard = null }
                )
            }
        }
    }
}

@Composable
private fun HazardMapMarker(
    hazard: MapHazardItem,
    pulseScale: Float,
    pulseAlpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val markerColor = if (hazard.isCritical) WayfinderHazardPothole else WayfinderHazardCrack

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = (hazard.normalizedX * 300).dp,
                top = (hazard.normalizedY * 500).dp
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated Pulse Ring
        Box(
            modifier = Modifier
                .size(44.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(markerColor.copy(alpha = pulseAlpha))
        )

        // Outer Translucent Glass Container
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            markerColor.copy(alpha = 0.35f),
                            WayfinderDarkSurface.copy(alpha = 0.85f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                .clickable(interactionSource = interactionSource, indication = null) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            // Inner Core Icon Button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(markerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = hazard.icon,
                    contentDescription = hazard.title,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun HazardDetailsBottomSheet(
    item: MapHazardItem,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
    onReroute: () -> Unit
) {
    val markerColor = if (item.isCritical) WayfinderHazardPothole else WayfinderHazardCrack

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                spotColor = Color.Black.copy(alpha = 0.6f),
                ambientColor = Color.Black.copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        WayfinderDarkSurface.copy(alpha = 0.96f),
                        Color(0xFF0F1713).copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        WayfinderGlassHighlight.copy(alpha = 0.3f),
                        WayfinderGlassBorder.copy(alpha = 0.15f)
                    )
                ),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
            // Drag handle pill
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(markerColor.copy(alpha = 0.18f))
                            .border(1.dp, markerColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = markerColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Hazard: ${item.typeName}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = WayfinderTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Reported ${item.timeAgo}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WayfinderTextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = WayfinderTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "Location",
                    tint = WayfinderSage,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = item.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WayfinderTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Confirm Button (Subtle Glass)
                GlassButton(
                    text = "Confirm",
                    onClick = onConfirm,
                    icon = Icons.Filled.ThumbUp,
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                )

                // Reroute Button (Leafy Green Glass CTA)
                GlassButton(
                    text = "Reroute",
                    onClick = onReroute,
                    icon = Icons.AutoMirrored.Filled.AltRoute,
                    isPrimary = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

val defaultMapHazards = emptyList<MapHazardItem>()
