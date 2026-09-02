package com.roadguardian.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadguardian.app.traffic.model.WazeTrafficState
import com.roadguardian.app.ui.components.AmbientNatureLayer
import com.roadguardian.app.ui.components.GlassButton
import com.roadguardian.app.ui.components.NatureBackground
import com.roadguardian.app.ui.components.NearbyTrafficCard
import com.roadguardian.app.ui.components.WeatherPill
import com.roadguardian.app.ui.theme.WayfinderDarkSurface
import com.roadguardian.app.ui.theme.WayfinderGlassBorder
import com.roadguardian.app.ui.theme.WayfinderGlassHighlight
import com.roadguardian.app.ui.theme.WayfinderPrimaryGreen
import com.roadguardian.app.ui.theme.WayfinderSage
import com.roadguardian.app.ui.theme.WayfinderTextPrimary
import com.roadguardian.app.ui.theme.WayfinderTextSecondary
import com.roadguardian.app.weather.WeatherState

@Composable
fun HomeScreen(
    isReady: Boolean = true,
    statusMessage: String = "Device calibrated and GPS signal strong.",
    weatherState: WeatherState = WeatherState.Loading,
    trafficState: WazeTrafficState = WazeTrafficState.Loading,
    cityName: String = "Locating...",
    regionName: String = "",
    onStartMonitoring: () -> Unit,
    onViewTrafficOnMap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "home_calm_animations")

    val spinRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_spin_slow"
    )

    val scrollState = rememberScrollState()

    NatureBackground(modifier = modifier) {
        AmbientNatureLayer(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                WeatherPill(
                    weatherState = weatherState
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Text(
                    text = cityName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = WayfinderTextPrimary,
                    textAlign = TextAlign.Center
                )
                if (regionName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = regionName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            letterSpacing = 0.2.sp
                        ),
                        color = WayfinderSage.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        spotColor = WayfinderPrimaryGreen.copy(alpha = 0.25f),
                        ambientColor = Color.Black.copy(alpha = 0.3f)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                WayfinderDarkSurface.copy(alpha = 0.95f),
                                Color(0xFF131E18).copy(alpha = 0.75f),
                                Color.Transparent.copy(alpha = 0.20f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                WayfinderGlassHighlight.copy(alpha = 0.35f),
                                WayfinderGlassBorder.copy(alpha = 0.15f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .rotate(spinRotation)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    WayfinderPrimaryGreen.copy(alpha = 0.85f),
                                    WayfinderSage.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = "Radar",
                    tint = WayfinderPrimaryGreen,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isReady) "Ready to Monitor" else "Initializing Sensors",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 21.sp,
                    letterSpacing = (-0.2).sp
                ),
                color = WayfinderTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 17.sp,
                    fontSize = 12.5.sp
                ),
                color = WayfinderTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(280.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassButton(
                text = "Start Monitoring",
                onClick = onStartMonitoring,
                icon = Icons.Filled.PlayArrow,
                isPrimary = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            NearbyTrafficCard(
                trafficState = trafficState,
                onViewOnMap = onViewTrafficOnMap,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
