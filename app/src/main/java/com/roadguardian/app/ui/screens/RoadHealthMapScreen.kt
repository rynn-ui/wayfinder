package com.roadguardian.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadguardian.app.domain.model.HazardSeverity
import com.roadguardian.app.domain.model.RoadHazard
import com.roadguardian.app.location.LocationState
import com.roadguardian.app.traffic.WazeNavigationLauncher
import com.roadguardian.app.traffic.model.WazeTrafficIncident
import com.roadguardian.app.traffic.model.WazeTrafficState
import com.roadguardian.app.traffic.model.WazeTrafficSummary
import com.roadguardian.app.ui.components.GlassButton
import com.roadguardian.app.ui.components.GlassCard
import com.roadguardian.app.ui.components.NatureBackground
import com.roadguardian.app.ui.components.TrafficLayerPill
import com.roadguardian.app.ui.components.WazeCyanColor
import com.roadguardian.app.ui.components.WazeIncidentDetailCard
import com.roadguardian.app.ui.map.MapViewControls
import com.roadguardian.app.ui.map.OsmMapView
import com.roadguardian.app.ui.map.OsrmRoutingService
import com.roadguardian.app.ui.map.RouteHazardAnalysis
import com.roadguardian.app.ui.theme.WayfinderDarkBackground
import com.roadguardian.app.ui.theme.WayfinderDarkSurface
import com.roadguardian.app.ui.theme.WayfinderGlassBorder
import com.roadguardian.app.ui.theme.WayfinderGlassHighlight
import com.roadguardian.app.ui.theme.WayfinderHazardCrack
import com.roadguardian.app.ui.theme.WayfinderHazardPothole
import com.roadguardian.app.ui.theme.WayfinderPrimaryGreen
import com.roadguardian.app.ui.theme.WayfinderSafeGreen
import com.roadguardian.app.ui.theme.WayfinderSage
import com.roadguardian.app.ui.theme.WayfinderTextPrimary
import com.roadguardian.app.ui.theme.WayfinderTextSecondary
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.util.Locale

@Composable
fun RoadHealthMapScreen(
    hazards: List<RoadHazard> = emptyList(),
    locationState: LocationState = LocationState(),
    hasLocationPermission: Boolean = true,
    isLocationEnabled: Boolean = true,
    initialSelectedHazard: RoadHazard? = null,
    trafficState: WazeTrafficState = WazeTrafficState.Loading,
    isTrafficLayerVisible: Boolean = true,
    onToggleTrafficLayer: () -> Unit = {},
    onRequestLocationPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedHazard by remember(initialSelectedHazard) { mutableStateOf(initialSelectedHazard) }
    var selectedTrafficIncident by remember { mutableStateOf<WazeTrafficIncident?>(null) }
    var isThreatGuideExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val trafficSummary = when (trafficState) {
        is WazeTrafficState.Success -> trafficState.summary
        is WazeTrafficState.Error -> trafficState.fallbackSummary
        else -> null
    }
    val trafficIncidents = trafficSummary?.incidents ?: emptyList()

    LaunchedEffect(initialSelectedHazard) {
        if (initialSelectedHazard != null && initialSelectedHazard.deviceLatitude != 0.0) {
            selectedHazard = initialSelectedHazard
        }
    }

    BackHandler(enabled = selectedHazard != null || selectedTrafficIncident != null) {
        selectedHazard = null
        selectedTrafficIncident = null
    }
    val routingService = remember { OsrmRoutingService() }

    var routeWaypoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var routeAnalysis by remember { mutableStateOf<RouteHazardAnalysis?>(null) }
    var isCalculatingRoute by remember { mutableStateOf(false) }

    var isFollowUser by remember { mutableStateOf(true) }
    var isFollowHeading by remember { mutableStateOf(false) }
    var mapControls by remember { mutableStateOf(MapViewControls()) }

    fun calculateDemoRoute() {
        if (!locationState.isAvailable || locationState.latitude == 0.0) return
        val targetHazard = hazards.maxByOrNull { it.confirmationCount } ?: return

        scope.launch {
            isCalculatingRoute = true
            val result = routingService.getRoute(
                startLat = locationState.latitude,
                startLon = locationState.longitude,
                endLat = targetHazard.deviceLatitude,
                endLon = targetHazard.deviceLongitude
            )
            if (result != null) {
                routeWaypoints = result.waypoints
                routeAnalysis = routingService.calculateRouteHazardScore(result.waypoints, hazards)
            }
            isCalculatingRoute = false
        }
    }

    if (!hasLocationPermission) {
        NatureBackground(modifier = modifier) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(28.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(WayfinderDarkSurface.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "Location Permission",
                                tint = WayfinderPrimaryGreen,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Location Access Required",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = WayfinderTextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Wayfinder uses OpenStreetMap to display real-time crowdsourced road condition intelligence around your journey.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WayfinderTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        GlassButton(
                            text = "Grant Permission",
                            onClick = onRequestLocationPermission,
                            isPrimary = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    } else if (!isLocationEnabled) {
        NatureBackground(modifier = modifier) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(28.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(WayfinderDarkSurface.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOff,
                                contentDescription = "Location Disabled",
                                tint = WayfinderHazardCrack,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Location Services Disabled",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = WayfinderTextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Turn on Android Location in Quick Settings or system settings to center the map on your position.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WayfinderTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        GlassButton(
                            text = "Open Location Settings",
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                }
                            },
                            isPrimary = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(WayfinderDarkBackground)
        ) {
            OsmMapView(
                hazards = hazards,
                locationState = locationState,
                routePoints = routeWaypoints,
                selectedHazard = selectedHazard,
                onHazardSelected = { hazard ->
                    selectedTrafficIncident = null
                    selectedHazard = hazard
                },
                trafficIncidents = trafficIncidents,
                isTrafficLayerVisible = isTrafficLayerVisible,
                selectedTrafficIncident = selectedTrafficIncident,
                onTrafficIncidentSelected = { incident ->
                    selectedHazard = null
                    selectedTrafficIncident = incident
                },
                isFollowUser = isFollowUser,
                isFollowHeading = isFollowHeading,
                onUserPan = { isFollowUser = false },
                onMapReady = { controls -> mapControls = controls },
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (routeAnalysis == null) {
                    MapThreatGuideCard(
                        isExpanded = isThreatGuideExpanded,
                        onToggleExpand = { isThreatGuideExpanded = !isThreatGuideExpanded },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TrafficLayerPill(
                    summary = trafficSummary,
                    isLayerVisible = isTrafficLayerVisible,
                    onToggle = onToggleTrafficLayer
                )
            }

            if (routeAnalysis != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val scoreColor = when (routeAnalysis!!.score) {
                                    HazardSeverity.CRITICAL, HazardSeverity.HIGH -> WayfinderHazardPothole
                                    HazardSeverity.MEDIUM -> WayfinderHazardCrack
                                    HazardSeverity.LOW -> WayfinderSafeGreen
                                }
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(scoreColor)
                                )
                                Column {
                                    Text(
                                        text = routeAnalysis!!.summaryText,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = WayfinderTextPrimary
                                    )
                                    Text(
                                        text = "Corridor check: ${routeAnalysis!!.totalHazardsCount} hazards detected",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WayfinderTextSecondary
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    routeWaypoints = emptyList()
                                    routeAnalysis = null
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear Route",
                                    tint = WayfinderTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = WayfinderDarkSurface,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .border(
                            1.dp,
                            if (isTrafficLayerVisible) WazeCyanColor else Color.White.copy(alpha = 0.25f),
                            CircleShape
                        )
                ) {
                    IconButton(onClick = onToggleTrafficLayer) {
                        Icon(
                            imageVector = Icons.Filled.Traffic,
                            contentDescription = "Toggle Traffic Layer",
                            tint = if (isTrafficLayerVisible) WazeCyanColor else WayfinderTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = WayfinderDarkSurface,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .border(
                            1.dp,
                            if (isFollowHeading) WayfinderPrimaryGreen else Color.White.copy(alpha = 0.25f),
                            CircleShape
                        )
                ) {
                    IconButton(
                        onClick = {
                            if (isFollowHeading) {
                                isFollowHeading = false
                                mapControls.resetNorth()
                            } else {
                                isFollowHeading = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isFollowHeading) Icons.Filled.Navigation else Icons.Filled.Explore,
                            contentDescription = "Compass / Heading Mode",
                            tint = if (isFollowHeading) WayfinderPrimaryGreen else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = WayfinderDarkSurface,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, WayfinderPrimaryGreen.copy(alpha = 0.3f), CircleShape)
                ) {
                    IconButton(onClick = { mapControls.zoomIn() }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Zoom In",
                            tint = WayfinderPrimaryGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = WayfinderDarkSurface,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, WayfinderPrimaryGreen.copy(alpha = 0.3f), CircleShape)
                ) {
                    IconButton(onClick = { mapControls.zoomOut() }) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Zoom Out",
                            tint = WayfinderPrimaryGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (hazards.isNotEmpty() && routeWaypoints.isEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = WayfinderDarkSurface,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.dp, WayfinderPrimaryGreen.copy(alpha = 0.4f), CircleShape)
                    ) {
                        IconButton(
                            onClick = { calculateDemoRoute() },
                            enabled = !isCalculatingRoute
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.AltRoute,
                                contentDescription = "Route Hazard Check",
                                tint = WayfinderPrimaryGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = WayfinderPrimaryGreen,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(52.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                ) {
                    IconButton(
                        onClick = {
                            selectedHazard = null
                            selectedTrafficIncident = null
                            isFollowUser = true
                            if (locationState.isAvailable && locationState.latitude != 0.0) {
                                mapControls.animateToLocation(
                                    locationState.latitude,
                                    locationState.longitude,
                                    17.0
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = "My Location",
                            tint = WayfinderDarkBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "© OpenStreetMap contributors",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp
                    ),
                    color = WayfinderTextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp)
                )

                if (locationState.isAvailable && locationState.latitude != 0.0) {
                    GlassCard(
                        modifier = Modifier,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Place,
                                contentDescription = null,
                                tint = WayfinderPrimaryGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = String.format(Locale.US, "%.4f°N, %.4f°E", locationState.latitude, locationState.longitude),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = WayfinderTextPrimary
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedHazard != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            ) {
                selectedHazard?.let { item ->
                    HazardDetailsBottomSheet(
                        hazard = item,
                        onClose = { selectedHazard = null }
                    )
                }
            }

            AnimatedVisibility(
                visible = selectedTrafficIncident != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            ) {
                selectedTrafficIncident?.let { incident ->
                    WazeIncidentDetailCard(
                        incident = incident,
                        onClose = { selectedTrafficIncident = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun HazardDetailsBottomSheet(
    hazard: RoadHazard,
    onClose: () -> Unit
) {
    val markerColor = when (hazard.severity.lowercase()) {
        "critical", "high" -> WayfinderHazardPothole
        "medium" -> WayfinderHazardCrack
        else -> WayfinderSafeGreen
    }

    val timeAgoText = formatTimeAgo(hazard.lastSeenAt)
    val confidencePercentage = (hazard.confidence * 100).toInt()
    val coordinatesText = String.format(
        Locale.US,
        "%.4f\u00B0N, %.4f\u00B0E",
        hazard.deviceLatitude,
        hazard.deviceLongitude
    )

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
                            imageVector = Icons.Filled.Warning,
                            contentDescription = hazard.displayTitle,
                            tint = markerColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = hazard.displayTitle.uppercase(Locale.US),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = WayfinderTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Reported $timeAgoText \u2022 Status: ${hazard.status.displayName}",
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CONFIDENCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            ),
                            color = WayfinderTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$confidencePercentage%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = WayfinderPrimaryGreen
                        )
                    }
                }

                GlassCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "REPORTS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            ),
                            color = WayfinderTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${hazard.confirmationCount}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = WayfinderSage
                        )
                    }
                }

                GlassCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SEVERITY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            ),
                            color = WayfinderTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = hazard.severity.replaceFirstChar { it.uppercase(Locale.US) },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = markerColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "Coordinates",
                    tint = WayfinderSage,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = coordinatesText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WayfinderTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val context = LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassButton(
                    text = "Navigate with Waze",
                    onClick = {
                        WazeNavigationLauncher.launchWazeNavigation(
                            context = context,
                            latitude = hazard.deviceLatitude,
                            longitude = hazard.deviceLongitude
                        )
                    },
                    icon = Icons.Filled.Navigation,
                    isPrimary = true,
                    modifier = Modifier.weight(1.3f)
                )

                GlassButton(
                    text = "Close",
                    onClick = onClose,
                    isPrimary = false,
                    modifier = Modifier.weight(0.9f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diffMs = System.currentTimeMillis() - timestamp
    val seconds = diffMs / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        diffMs < 0L -> "Just now"
        seconds < 60 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        else -> "$days d ago"
    }
}

@Composable
fun MapThreatGuideCard(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "pulse_red")
    val redPulseAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "red_dot"
    )

    Surface(
        modifier = modifier
            .animateContentSize()
            .border(1.dp, WayfinderPrimaryGreen.copy(alpha = 0.40f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xF50E1612),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(WayfinderHazardPothole.copy(alpha = redPulseAlpha))
                    )
                    Text(
                        text = "ROAD THREAT & TRAFFIC GUIDE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 12.sp
                        ),
                        color = Color.White
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(WayfinderHazardPothole.copy(alpha = 0.25f))
                            .border(1.dp, WayfinderHazardPothole.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "Hide" else "Guide ▶",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFFF6B6B)
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Toggle Guide",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))

                ThreatSimulationVideoPlayer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LegendItemRow(
                        color = Color(0xFFE11D48),
                        symbol = "!",
                        title = "Critical Pothole",
                        subtitle = "Severe deep road crater — High danger to tires & suspension"
                    )
                    LegendItemRow(
                        color = Color(0xFFEF4444),
                        symbol = "!",
                        title = "High Severity Pothole",
                        subtitle = "Confirmed road pothole requiring speed reduction"
                    )
                    LegendItemRow(
                        color = Color(0xFFEA580C),
                        symbol = "!",
                        title = "Medium Pothole / Road Crack",
                        subtitle = "Surface fracture or moderate road depression"
                    )
                    LegendItemRow(
                        color = Color(0xFFCA8A04),
                        symbol = "!",
                        title = "Minor Surface Anomaly",
                        subtitle = "Uneven road surface, bump, or minor patch"
                    )
                    LegendItemRow(
                        color = Color(0xFFB91C1C),
                        symbol = "JAM",
                        title = "Traffic Jam & Congestion Pin",
                        subtitle = "Live traffic slowdown — Shows delay minutes & moving speed"
                    )
                    LegendItemRow(
                        color = Color(0xFFF59E0B),
                        symbol = "━",
                        isLine = true,
                        title = "Congested Traffic Lane",
                        subtitle = "Active road queue traced along OpenStreetMap driving lanes"
                    )
                    LegendItemRow(
                        color = Color(0xFFBE123C),
                        symbol = "ACC",
                        title = "Road Accident Pin",
                        subtitle = "Active vehicle collision or lane obstruction"
                    )
                    LegendItemRow(
                        color = Color(0xFF4B5563),
                        symbol = "X",
                        title = "Road Closure Pin",
                        subtitle = "Road closed to vehicular traffic or detour"
                    )
                    LegendItemRow(
                        color = WayfinderPrimaryGreen,
                        symbol = "●",
                        isOrb = true,
                        title = "Live Vehicle GPS Position",
                        subtitle = "Your real-time GPS location and driving heading"
                    )
                }
            }
        }
    }
}

@Composable
private fun ThreatSimulationVideoPlayer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sim_video")
    val animProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "road_anim"
    )

    Box(
        modifier = modifier
            .background(Color(0xFF0D1410))
            .border(1.dp, WayfinderHazardPothole.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val horizonY = h * 0.25f

            drawLine(
                color = Color(0x334E6554),
                start = androidx.compose.ui.geometry.Offset(w * 0.45f, horizonY),
                end = androidx.compose.ui.geometry.Offset(w * 0.05f, h),
                strokeWidth = 3f
            )
            drawLine(
                color = Color(0x334E6554),
                start = androidx.compose.ui.geometry.Offset(w * 0.55f, horizonY),
                end = androidx.compose.ui.geometry.Offset(w * 0.95f, h),
                strokeWidth = 3f
            )

            val stripeOffset = (animProgress * h * 0.4f)
            for (i in 0..3) {
                val sy = horizonY + ((i * h * 0.25f) + stripeOffset) % (h - horizonY)
                if (sy in horizonY..h) {
                    val progressRatio = (sy - horizonY) / (h - horizonY)
                    val sw = 2f + progressRatio * 4f
                    val sl = 6f + progressRatio * 18f
                    drawLine(
                        color = Color(0x88F59E0B),
                        start = androidx.compose.ui.geometry.Offset(w * 0.5f, sy),
                        end = androidx.compose.ui.geometry.Offset(w * 0.5f, minOf(h, sy + sl)),
                        strokeWidth = sw
                    )
                }
            }

            val potholeY = horizonY + (animProgress * (h - horizonY - 20f))
            val potholeSize = 8f + animProgress * 26f
            val potholeX = w * 0.52f

            drawOval(
                color = Color(0xFF1B0C0C),
                topLeft = androidx.compose.ui.geometry.Offset(potholeX - potholeSize, potholeY - potholeSize * 0.45f),
                size = androidx.compose.ui.geometry.Size(potholeSize * 2f, potholeSize * 0.9f)
            )

            val boxColor = Color(0xFFEF4444)
            drawRect(
                color = boxColor.copy(alpha = 0.85f),
                topLeft = androidx.compose.ui.geometry.Offset(potholeX - potholeSize - 4f, potholeY - potholeSize * 0.45f - 4f),
                size = androidx.compose.ui.geometry.Size(potholeSize * 2f + 8f, potholeSize * 0.9f + 8f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )

            val pinY = potholeY - (potholeSize * 0.5f)
            drawCircle(
                color = boxColor.copy(alpha = 0.35f),
                radius = potholeSize * 0.8f,
                center = androidx.compose.ui.geometry.Offset(potholeX, pinY)
            )
            drawCircle(
                color = boxColor,
                radius = maxOf(4f, potholeSize * 0.35f),
                center = androidx.compose.ui.geometry.Offset(potholeX, pinY)
            )
            drawCircle(
                color = Color.White,
                radius = maxOf(4f, potholeSize * 0.35f),
                center = androidx.compose.ui.geometry.Offset(potholeX, pinY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
            )
            Text(
                text = "LIVE AI ROAD CAMERA PIPELINE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = Color.White.copy(alpha = 0.9f)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xDD000000))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "RED PIN = POTHOLE THREAT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = WayfinderHazardPothole
            )
        }
    }
}

@Composable
private fun LegendItemRow(
    color: Color,
    symbol: String,
    title: String,
    subtitle: String,
    isLine: Boolean = false,
    isOrb: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Box(
            modifier = Modifier.size(width = 30.dp, height = 34.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLine -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                }
                isOrb -> {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.3f))
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
                else -> {
                    LegendPinIcon(
                        color = color,
                        symbol = symbol
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = WayfinderTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp
                ),
                color = WayfinderTextSecondary
            )
        }
    }
}

@Composable
private fun LegendPinIcon(
    color: Color,
    symbol: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(width = 24.dp, height = 30.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val bulbCenterY = h * 0.38f
            val bulbRadius = w * 0.40f
            val tipY = h - 2f

            val pinPath = Path().apply {
                val leftX = cx - bulbRadius
                val rightX = cx + bulbRadius
                moveTo(cx, tipY)
                cubicTo(
                    cx - (bulbRadius * 0.6f), tipY - (h * 0.28f),
                    leftX, bulbCenterY + (bulbRadius * 0.4f),
                    leftX, bulbCenterY
                )
                arcTo(
                    Rect(leftX, bulbCenterY - bulbRadius, rightX, bulbCenterY + bulbRadius),
                    180f,
                    180f,
                    false
                )
                cubicTo(
                    rightX, bulbCenterY + (bulbRadius * 0.4f),
                    cx + (bulbRadius * 0.6f), tipY - (h * 0.28f),
                    cx, tipY
                )
                close()
            }

            drawPath(
                path = pinPath,
                color = color
            )
            drawPath(
                path = pinPath,
                color = Color.White.copy(alpha = 0.85f),
                style = Stroke(width = 1.5f)
            )

            drawCircle(
                color = Color.White,
                radius = bulbRadius * 0.55f,
                center = Offset(cx, bulbCenterY)
            )
        }

        Box(
            modifier = Modifier
                .padding(bottom = 7.dp)
                .size(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (symbol.length > 2) 7.sp else 9.sp,
                    fontWeight = FontWeight.Black
                ),
                color = color
            )
        }
    }
}
