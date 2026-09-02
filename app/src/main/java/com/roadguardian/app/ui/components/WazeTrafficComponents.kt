package com.roadguardian.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadguardian.app.traffic.WazeNavigationLauncher
import com.roadguardian.app.traffic.model.WazeJamSeverity
import com.roadguardian.app.traffic.model.WazeTrafficIncident
import com.roadguardian.app.traffic.model.WazeTrafficState
import com.roadguardian.app.traffic.model.WazeTrafficSummary
import com.roadguardian.app.traffic.model.WazeTrafficType
import com.roadguardian.app.ui.theme.WayfinderDarkSurface
import com.roadguardian.app.ui.theme.WayfinderDarkSurfaceVariant
import com.roadguardian.app.ui.theme.WayfinderGlassBorder
import com.roadguardian.app.ui.theme.WayfinderGlassHighlight
import com.roadguardian.app.ui.theme.WayfinderPrimaryGreen
import com.roadguardian.app.ui.theme.WayfinderSafeGreen
import com.roadguardian.app.ui.theme.WayfinderSage
import com.roadguardian.app.ui.theme.WayfinderTextPrimary
import com.roadguardian.app.ui.theme.WayfinderTextSecondary
import com.roadguardian.app.ui.theme.WayfinderTextTertiary

val WazeCyanColor = Color(0xFF33CCFF)
val WazeBrandBlue = Color(0xFF00B0FF)

@Composable
fun NearbyTrafficCard(
    trafficState: WazeTrafficState,
    onViewOnMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(WazeCyanColor.copy(alpha = 0.18f))
                            .border(1.dp, WazeCyanColor.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Traffic,
                            contentDescription = "Traffic",
                            tint = WazeCyanColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Live Traffic",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = WayfinderTextPrimary
                        )
                        Text(
                            text = "Real-time road conditions",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp
                            ),
                            color = WayfinderTextTertiary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (trafficState) {
                        is WazeTrafficState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = WazeCyanColor
                            )
                        }
                        is WazeTrafficState.Success -> {
                            TrafficStatusChip(severity = trafficState.summary.overallSeverity)
                        }
                        is WazeTrafficState.Error -> {
                            val fallback = trafficState.fallbackSummary
                            if (fallback != null) {
                                TrafficStatusChip(severity = fallback.overallSeverity)
                            }
                        }
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = WayfinderTextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Compact summary row when collapsed
            if (!isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                when (trafficState) {
                    is WazeTrafficState.Loading -> {
                        Text(
                            text = "Checking real-time road conditions...",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = WayfinderTextSecondary
                        )
                    }
                    is WazeTrafficState.Success -> {
                        CompactTrafficBanner(summary = trafficState.summary)
                    }
                    is WazeTrafficState.Error -> {
                        val fallback = trafficState.fallbackSummary
                        if (fallback != null) {
                            CompactTrafficBanner(summary = fallback)
                        } else {
                            Text(
                                text = trafficState.message,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = WayfinderTextSecondary
                            )
                        }
                    }
                }
            }

            // Expanded details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    when (trafficState) {
                        is WazeTrafficState.Loading -> {
                            Text(
                                text = "Querying live traffic feeds...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = WayfinderTextSecondary
                            )
                        }
                        is WazeTrafficState.Success -> {
                            TrafficSummaryContent(summary = trafficState.summary, onViewOnMap = onViewOnMap)
                        }
                        is WazeTrafficState.Error -> {
                            val fallback = trafficState.fallbackSummary
                            if (fallback != null) {
                                TrafficSummaryContent(summary = fallback, onViewOnMap = onViewOnMap)
                            } else {
                                Text(
                                    text = trafficState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WayfinderTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactTrafficBanner(summary: WazeTrafficSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(WayfinderDarkSurface.copy(alpha = 0.6f))
            .border(1.dp, WayfinderGlassBorder.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val message = if (summary.jamsCount > 0) {
            "${summary.jamsCount} Jam${if (summary.jamsCount > 1) "s" else ""} • +${summary.maxDelayMinutes}m delay"
        } else {
            "Normal traffic flow • No delays"
        }

        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.5.sp
            ),
            color = if (summary.jamsCount > 0) Color(0xFFFBBF24) else WayfinderSafeGreen
        )

        Text(
            text = "Details ▸",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            color = WazeCyanColor
        )
    }
}

@Composable
private fun TrafficSummaryContent(
    summary: WazeTrafficSummary,
    onViewOnMap: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TrafficStatMiniCard(
                title = "Jams Nearby",
                value = summary.jamsCount.toString(),
                highlightColor = if (summary.jamsCount > 0) Color(0xFFF59E0B) else WayfinderSafeGreen,
                modifier = Modifier.weight(1f)
            )
            TrafficStatMiniCard(
                title = "Max Delay",
                value = if (summary.maxDelayMinutes > 0) "+${summary.maxDelayMinutes}m" else "None",
                highlightColor = if (summary.maxDelayMinutes > 5) Color(0xFFEF4444) else WayfinderSafeGreen,
                modifier = Modifier.weight(1f)
            )
            TrafficStatMiniCard(
                title = "Alerts",
                value = summary.totalAlertsCount.toString(),
                highlightColor = WayfinderPrimaryGreen,
                modifier = Modifier.weight(1f)
            )
        }

        val nearest = summary.nearestIncident
        if (nearest != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(WayfinderDarkSurfaceVariant.copy(alpha = 0.7f))
                    .border(1.dp, WayfinderGlassBorder.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .clickable { onViewOnMap() }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.DirectionsCar,
                    contentDescription = null,
                    tint = nearest.severity.color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nearest.streetName,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = WayfinderTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${nearest.formattedDelay} • ${nearest.formattedSpeed} • ${nearest.formattedDistance}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = WayfinderTextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "View on Map",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = WazeCyanColor
                )
            }
        }
    }
}

@Composable
private fun TrafficStatMiniCard(
    title: String,
    value: String,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(WayfinderDarkSurface.copy(alpha = 0.85f))
            .border(1.dp, WayfinderGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = WayfinderTextTertiary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = highlightColor
            )
        }
    }
}

@Composable
fun TrafficStatusChip(
    severity: WazeJamSeverity,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(severity.color.copy(alpha = 0.18f))
            .border(1.dp, severity.color.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = severity.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            color = severity.color
        )
    }
}

@Composable
fun WazeIncidentDetailCard(
    incident: WazeTrafficIncident,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Solid dark card with high contrast for crystal clear text readability over map tiles
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.7f),
                ambientColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        WayfinderDarkSurface.copy(alpha = 0.98f),
                        Color(0xFF0F1813).copy(alpha = 0.99f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        WayfinderGlassHighlight.copy(alpha = 0.35f),
                        WayfinderGlassBorder.copy(alpha = 0.20f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(incident.severity.color.copy(alpha = 0.25f))
                            .border(1.dp, incident.severity.color.copy(alpha = 0.60f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (incident.type) {
                                WazeTrafficType.JAM -> Icons.Filled.Traffic
                                WazeTrafficType.ACCIDENT -> Icons.Filled.Warning
                                WazeTrafficType.ROAD_CLOSED -> Icons.Filled.Close
                                else -> Icons.Filled.DirectionsCar
                            },
                            contentDescription = null,
                            tint = incident.severity.color,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = incident.type.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = incident.streetName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = WayfinderTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // High-contrast report description
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF142019).copy(alpha = 0.90f))
                    .border(1.dp, WayfinderGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = incident.reportDescription,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFFF1F5F9)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    label = "Delay",
                    value = incident.formattedDelay,
                    icon = Icons.AutoMirrored.Filled.AltRoute,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "Moving Speed",
                    value = incident.formattedSpeed,
                    icon = Icons.Filled.Speed,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "Distance",
                    value = incident.formattedDistance,
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    modifier = Modifier.weight(1f)
                )
            }

            if (incident.nThumbsUp > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ThumbUp,
                        contentDescription = null,
                        tint = WayfinderSage,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${incident.nThumbsUp} drivers confirmed this traffic report",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = WayfinderSage
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            GlassButton(
                text = "Navigate via Waze",
                onClick = {
                    WazeNavigationLauncher.launchWazeNavigation(
                        context = context,
                        latitude = incident.latitude,
                        longitude = incident.longitude
                    )
                },
                icon = Icons.Filled.Navigation,
                isPrimary = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF16231B).copy(alpha = 0.95f))
            .border(1.dp, WayfinderGlassBorder.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = WayfinderSage,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = WayfinderTextSecondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TrafficLayerPill(
    summary: WazeTrafficSummary?,
    isLayerVisible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isLayerVisible) {
                    Brush.horizontalGradient(
                        listOf(
                            WazeCyanColor.copy(alpha = 0.25f),
                            WayfinderDarkSurface.copy(alpha = 0.90f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            WayfinderDarkSurface.copy(alpha = 0.85f),
                            WayfinderDarkSurface.copy(alpha = 0.85f)
                        )
                    )
                }
            )
            .border(
                1.dp,
                if (isLayerVisible) WazeCyanColor.copy(alpha = 0.50f) else WayfinderGlassBorder.copy(alpha = 0.15f),
                RoundedCornerShape(20.dp)
            )
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Traffic,
                contentDescription = "Traffic Layer",
                tint = if (isLayerVisible) WazeCyanColor else WayfinderTextTertiary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isLayerVisible) {
                    if (summary != null && summary.jamsCount > 0) {
                        "Traffic: ${summary.jamsCount} Jams (+${summary.maxDelayMinutes}m)"
                    } else {
                        "Traffic Layer ON"
                    }
                } else {
                    "Traffic OFF"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp
                ),
                color = if (isLayerVisible) WayfinderTextPrimary else WayfinderTextTertiary
            )
        }
    }
}
