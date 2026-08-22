package com.roadguardian.app.ui.components

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadguardian.app.ui.theme.WayfinderGlassBorder
import com.roadguardian.app.ui.theme.WayfinderGlassHighlight
import com.roadguardian.app.ui.theme.WayfinderPrimaryGreen
import com.roadguardian.app.ui.theme.WayfinderSage
import com.roadguardian.app.ui.theme.WayfinderTextPrimary
import com.roadguardian.app.ui.theme.WayfinderTextSecondary
import com.roadguardian.app.weather.CurrentWeather
import com.roadguardian.app.weather.WeatherCodeMapper
import com.roadguardian.app.weather.WeatherState

/**
 * Compact horizontal glass weather pill matching the visual reference:
 * [ 🌧️ 24°C │ Drizzle / Good visibility ]
 */
@Composable
fun WeatherPill(
    weatherState: WeatherState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                spotColor = WayfinderPrimaryGreen.copy(alpha = 0.20f),
                ambientColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x301A2B20),
                        Color(0x18101713)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        WayfinderGlassHighlight.copy(alpha = 0.18f),
                        WayfinderGlassBorder.copy(alpha = 0.10f)
                    )
                ),
                shape = CircleShape
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        when (weatherState) {
            is WeatherState.Success -> {
                WeatherContent(weather = weatherState.weather)
            }
            is WeatherState.Error -> {
                if (weatherState.cachedWeather != null) {
                    WeatherContent(weather = weatherState.cachedWeather)
                } else {
                    WeatherSimpleText(
                        title = "Weather Offline",
                        subtitle = "Using cached data"
                    )
                }
            }
            is WeatherState.LocationUnavailable -> {
                WeatherSimpleText(
                    title = "Weather",
                    subtitle = weatherState.message
                )
            }
            is WeatherState.Loading -> {
                WeatherLoadingState()
            }
        }
    }
}

@Composable
private fun WeatherContent(weather: CurrentWeather) {
    val (_, icon) = WeatherCodeMapper.mapWmoCode(weather.weatherCode)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Left: Weather Condition Icon + Temperature
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = weather.conditionText,
                tint = WayfinderPrimaryGreen,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${weather.temperature}°C",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 21.sp,
                    letterSpacing = (-0.3).sp
                ),
                color = Color.White
            )
        }

        // Delicate Glass Vertical Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(24.dp)
                .background(Color.White.copy(alpha = 0.18f))
        )

        // Right: Condition text (White) + Secondary status (Leafy Green)
        Column(
            modifier = Modifier.padding(start = 12.dp)
        ) {
            Text(
                text = weather.conditionText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    letterSpacing = 0.2.sp
                ),
                color = Color.White
            )
            Text(
                text = weather.secondaryInfo,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    letterSpacing = 0.2.sp
                ),
                color = WayfinderPrimaryGreen
            )
        }
    }
}

@Composable
private fun WeatherSimpleText(
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Cloud,
            contentDescription = null,
            tint = WayfinderPrimaryGreen,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp
                ),
                color = WayfinderPrimaryGreen
            )
        }
    }
}

@Composable
private fun WeatherLoadingState() {
    val infiniteTransition = rememberInfiniteTransition(label = "weather_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "weather_loading_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(WayfinderPrimaryGreen.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Loading weather...",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            ),
            color = WayfinderTextSecondary.copy(alpha = alpha)
        )
    }
}
