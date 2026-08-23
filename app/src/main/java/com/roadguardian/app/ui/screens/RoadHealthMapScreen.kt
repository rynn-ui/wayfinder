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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.roadguardian.app.domain.model.RoadHazard
import com.roadguardian.app.location.LocationState
import com.roadguardian.app.ui.components.GlassButton
import com.roadguardian.app.ui.components.GlassCard
import com.roadguardian.app.ui.components.NatureBackground
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
import java.util.Locale

private const val DEFAULT_LATITUDE = 26.4499
private const val DEFAULT_LONGITUDE = 80.3319

private const val DARK_MAP_STYLE_JSON = """
[
  {"elementType": "geometry", "stylers": [{"color": "#18221b"}]},
  {"elementType": "labels.text.fill", "stylers": [{"color": "#8b9b8e"}]},
  {"elementType": "labels.text.stroke", "stylers": [{"color": "#101713"}]},
  {"featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [{"color": "#c4cec4"}]},
  {"featureType": "poi", "elementType": "labels.text.fill", "stylers": [{"color": "#a8c98f"}]},
  {"featureType": "poi.park", "elementType": "geometry", "stylers": [{"color": "#1f2f26"}]},
  {"featureType": "road", "elementType": "geometry", "stylers": [{"color": "#263a2e"}]},
  {"featureType": "road", "elementType": "geometry.stroke", "stylers": [{"color": "#1a2620"}]},
  {"featureType": "road", "elementType": "labels.text.fill", "stylers": [{"color": "#9cb886"}]},
  {"featureType": "road.highway", "elementType": "geometry", "stylers": [{"color": "#334e3e"}]},
  {"featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [{"color": "#1a2620"}]},
  {"featureType": "transit", "elementType": "geometry", "stylers": [{"color": "#202c25"}]},
  {"featureType": "water", "elementType": "geometry", "stylers": [{"color": "#0d1410"}]},
  {"featureType": "water", "elementType": "labels.text.fill", "stylers": [{"color": "#506b58"}]}
]
"""

@Composable
fun RoadHealthMapScreen(
    hazards: List<RoadHazard> = emptyList(),
    locationState: LocationState = LocationState(),
    hasLocationPermission: Boolean = true,
    onRequestLocationPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedHazard by remember { mutableStateOf<RoadHazard?>(null) }
    val scope = rememberCoroutineScope()

    val initialTarget = remember {
        if (locationState.isAvailable && locationState.latitude != 0.0) {
            LatLng(locationState.latitude, locationState.longitude)
        } else {
            LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialTarget, 16f)
    }

    var hasCenteredOnUser by remember { mutableStateOf(false) }

    LaunchedEffect(locationState.isAvailable) {
        if (locationState.isAvailable && !hasCenteredOnUser && locationState.latitude != 0.0) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(locationState.latitude, locationState.longitude),
                    16f
                )
            )
            hasCenteredOnUser = true
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "marker_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

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
                            text = "Wayfinder requires location access to display real-time road conditions and hazard markers around your journey.",
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
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(WayfinderDarkBackground)
        ) {
            val mapProperties = remember(hasLocationPermission) {
                MapProperties(
                    isMyLocationEnabled = hasLocationPermission,
                    mapStyleOptions = MapStyleOptions(DARK_MAP_STYLE_JSON)
                )
            }

            val mapUiSettings = remember {
                MapUiSettings(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false,
                    compassEnabled = true,
                    rotationGesturesEnabled = true,
                    scrollGesturesEnabled = true,
                    tiltGesturesEnabled = false,
                    zoomGesturesEnabled = true
                )
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = mapUiSettings,
                onMapClick = { selectedHazard = null }
            ) {
                hazards.forEach { hazard ->
                    val markerState = rememberMarkerState(
                        key = hazard.id,
                        position = LatLng(hazard.deviceLatitude, hazard.deviceLongitude)
                    )
                    markerState.position = LatLng(hazard.deviceLatitude, hazard.deviceLongitude)

                    MarkerComposable(
                        state = markerState,
                        title = hazard.displayTitle,
                        onClick = {
                            selectedHazard = hazard
                            true
                        }
                    ) {
                        HazardMapMarkerIcon(
                            hazard = hazard,
                            pulseScale = pulseScale,
                            pulseAlpha = pulseAlpha
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = WayfinderPrimaryGreen,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 84.dp)
                    .size(52.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            ) {
                IconButton(
                    onClick = {
                        val target = if (locationState.isAvailable && locationState.latitude != 0.0) {
                            LatLng(locationState.latitude, locationState.longitude)
                        } else {
                            LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
                        }
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(target, 16f)
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

            AnimatedVisibility(
                visible = selectedHazard != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                selectedHazard?.let { item ->
                    HazardDetailsBottomSheet(
                        hazard = item,
                        onClose = { selectedHazard = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun HazardMapMarkerIcon(
    hazard: RoadHazard,
    pulseScale: Float,
    pulseAlpha: Float
) {
    val markerColor = when (hazard.severity.lowercase()) {
        "critical" -> WayfinderHazardPothole
        "high" -> WayfinderHazardPothole
        "medium" -> WayfinderHazardCrack
        else -> WayfinderSafeGreen
    }

    Box(
        modifier = Modifier.size(42.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(markerColor.copy(alpha = pulseAlpha))
        )

        Box(
            modifier = Modifier
                .size(28.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            markerColor,
                            markerColor.copy(alpha = 0.85f)
                        )
                    )
                )
                .border(1.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = hazard.displayTitle,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun HazardDetailsBottomSheet(
    hazard: RoadHazard,
    onClose: () -> Unit
) {
    val markerColor = when (hazard.severity.lowercase()) {
        "critical" -> WayfinderHazardPothole
        "high" -> WayfinderHazardPothole
        "medium" -> WayfinderHazardCrack
        else -> WayfinderSafeGreen
    }

    val timeAgoText = formatTimeAgo(hazard.lastSeenAt)
    val confidencePercentage = (hazard.confidence * 100).toInt()
    val coordinatesText = String.format(
        Locale.US,
        "%.4f°N, %.4f°E",
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
                            text = "Reported $timeAgoText",
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
                            text = "CONFIRMATIONS",
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

            GlassButton(
                text = "Close",
                onClick = onClose,
                isPrimary = false,
                modifier = Modifier.fillMaxWidth()
            )

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
