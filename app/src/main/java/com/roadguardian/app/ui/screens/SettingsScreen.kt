package com.roadguardian.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadguardian.app.ai.inference.AiModelType
import com.roadguardian.app.location.LocationState
import com.roadguardian.app.sensors.WayfinderSensorManager
import com.roadguardian.app.ui.components.GlassButton
import com.roadguardian.app.ui.components.GlassCard
import com.roadguardian.app.ui.components.GlassDivider
import com.roadguardian.app.ui.components.NatureBackground
import com.roadguardian.app.ui.theme.WayfinderDarkSurface
import com.roadguardian.app.ui.theme.WayfinderHazardCrack
import com.roadguardian.app.ui.theme.WayfinderHazardPothole
import com.roadguardian.app.ui.theme.WayfinderPrimaryGreen
import com.roadguardian.app.ui.theme.WayfinderSafeGreen
import com.roadguardian.app.ui.theme.WayfinderSage
import com.roadguardian.app.ui.theme.WayfinderTextPrimary
import com.roadguardian.app.ui.theme.WayfinderTextSecondary
import com.roadguardian.app.warning.AudioWarningManager
import java.util.Locale
import kotlin.math.atan2

enum class SettingsSubPage {
    MAIN,
    CAMERA_ALIGNMENT,
    GPS_GEOLOCATION,
    AUDIO_ALERTS,
    ABOUT
}

@Composable
fun SettingsScreen(
    activeModel: AiModelType = AiModelType.DEFAULT,
    audioWarningManager: AudioWarningManager? = null,
    locationState: LocationState = LocationState(),
    onSelectModel: (AiModelType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentSubPage by remember { mutableStateOf(SettingsSubPage.MAIN) }

    BackHandler(enabled = currentSubPage != SettingsSubPage.MAIN) {
        currentSubPage = SettingsSubPage.MAIN
    }

    when (currentSubPage) {
        SettingsSubPage.MAIN -> {
            MainSettingsContent(
                activeModel = activeModel,
                onSelectModel = onSelectModel,
                onNavigateCalibration = { currentSubPage = SettingsSubPage.CAMERA_ALIGNMENT },
                onNavigateGps = { currentSubPage = SettingsSubPage.GPS_GEOLOCATION },
                onNavigateNotifications = { currentSubPage = SettingsSubPage.AUDIO_ALERTS },
                onNavigateAbout = { currentSubPage = SettingsSubPage.ABOUT },
                modifier = modifier
            )
        }
        SettingsSubPage.CAMERA_ALIGNMENT -> {
            CameraAlignmentSubPage(
                onBack = { currentSubPage = SettingsSubPage.MAIN },
                modifier = modifier
            )
        }
        SettingsSubPage.GPS_GEOLOCATION -> {
            GpsGeolocationSubPage(
                locationState = locationState,
                onBack = { currentSubPage = SettingsSubPage.MAIN },
                modifier = modifier
            )
        }
        SettingsSubPage.AUDIO_ALERTS -> {
            AudioAlertsSubPage(
                audioWarningManager = audioWarningManager,
                onBack = { currentSubPage = SettingsSubPage.MAIN },
                modifier = modifier
            )
        }
        SettingsSubPage.ABOUT -> {
            AboutSubPage(
                onBack = { currentSubPage = SettingsSubPage.MAIN },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun MainSettingsContent(
    activeModel: AiModelType,
    onSelectModel: (AiModelType) -> Unit,
    onNavigateCalibration: () -> Unit,
    onNavigateGps: () -> Unit,
    onNavigateNotifications: () -> Unit,
    onNavigateAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    NatureBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = "DEVICE & SENSORS",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = WayfinderSage,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsActionRow(
                        icon = Icons.Filled.Videocam,
                        title = "Camera Alignment",
                        subtitle = "Live pitch leveler & windshield mounting guide",
                        onClick = onNavigateCalibration
                    )
                    GlassDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsActionRow(
                        icon = Icons.Filled.LocationSearching,
                        title = "GPS & Geolocation",
                        subtitle = "High-accuracy GNSS & 15m hazard clustering",
                        onClick = onNavigateGps
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "SAFETY & PREFERENCES",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = WayfinderSage,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsActionRow(
                        icon = Icons.Filled.Notifications,
                        title = "Driver Audio Alerts",
                        subtitle = "Voice warnings & 50m forward corridor safety",
                        onClick = onNavigateNotifications
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "ABOUT",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = WayfinderSage,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsActionRow(
                        icon = Icons.Filled.Info,
                        title = "About Wayfinder",
                        subtitle = "Version 1.0.0 • YOLO11 Pothole Engine",
                        onClick = onNavigateAbout
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CameraAlignmentSubPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sensorManager = remember { WayfinderSensorManager(context) }
    val telemetry by sensorManager.telemetry.collectAsState()

    DisposableEffect(Unit) {
        sensorManager.startListening()
        onDispose {
            sensorManager.stopListening()
        }
    }

    val pitch = remember(telemetry.accelerometerY, telemetry.accelerometerZ) {
        val y = telemetry.accelerometerY
        val z = telemetry.accelerometerZ
        val rad = atan2(y.toDouble(), z.toDouble())
        (rad * 180.0 / Math.PI).toFloat().coerceIn(-90f, 90f)
    }

    val isOptimal = pitch in 12f..25f
    val pitchColor = if (isOptimal) WayfinderPrimaryGreen else WayfinderHazardCrack

    NatureBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            SubPageHeader(
                title = "Camera Alignment",
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(WayfinderPrimaryGreen.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = null,
                                    tint = WayfinderPrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Live Mount Pitch Leveler",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = WayfinderTextPrimary
                                )
                                Text(
                                    text = "Optimal Target: 15° – 20° downward tilt",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WayfinderTextSecondary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(pitchColor.copy(alpha = 0.20f))
                                .border(1.dp, pitchColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isOptimal) "OPTIMAL ✅" else if (pitch < 12f) "TILT DOWN ⬇" else "TILT UP ⬆",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = pitchColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0C130F))
                            .border(1.dp, pitchColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val cx = w / 2f
                            val cy = h / 2f

                            drawLine(
                                color = Color.White.copy(alpha = 0.15f),
                                start = Offset(0f, cy),
                                end = Offset(w, cy),
                                strokeWidth = 1.5f
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.15f),
                                start = Offset(cx, 0f),
                                end = Offset(cx, h),
                                strokeWidth = 1.5f
                            )

                            drawCircle(
                                color = Color.White.copy(alpha = 0.10f),
                                radius = 45.dp.toPx(),
                                style = Stroke(width = 1.5f)
                            )
                            drawCircle(
                                color = pitchColor.copy(alpha = 0.25f),
                                radius = 22.dp.toPx(),
                                style = Stroke(width = 2f)
                            )

                            val clampedOffset = (pitch - 17.5f) * 3.5f
                            val bubbleY = (cy + clampedOffset).coerceIn(20f, h - 20f)

                            drawCircle(
                                color = pitchColor,
                                radius = 10.dp.toPx()
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 10.dp.toPx(),
                                style = Stroke(width = 2f)
                            )

                            drawLine(
                                color = pitchColor,
                                start = Offset(cx - 35.dp.toPx(), bubbleY),
                                end = Offset(cx + 35.dp.toPx(), bubbleY),
                                strokeWidth = 3f
                            )
                        }

                        Text(
                            text = String.format(Locale.US, "%.1f° PITCH", pitch),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Windshield Mounting Guidelines",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = WayfinderTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Mount phone in portrait orientation at vehicle center.\n2. Tilt downward 15° to 20° so asphalt occupies 70% of view.\n3. Keep windshield wiper swipe area clean to prevent false detections.\n4. Avoid dashboard reflections with matte dash covers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WayfinderTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            GlassButton(
                text = "Back to Settings",
                onClick = onBack,
                isPrimary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GpsGeolocationSubPage(
    locationState: LocationState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var updateRateIndex by remember { mutableIntStateOf(0) }
    var deduplicationRadiusIndex by remember { mutableIntStateOf(1) }
    var isPowerSaverEnabled by remember { mutableStateOf(true) }

    val rateOptions = listOf("1.0s (Live AI)", "2.0s (Normal)", "4.0s (Eco)")
    val radiusOptions = listOf("10 Meters", "15 Meters (Default)", "25 Meters")

    NatureBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            SubPageHeader(
                title = "GPS & Geolocation",
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(WayfinderPrimaryGreen.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GpsFixed,
                                contentDescription = null,
                                tint = WayfinderPrimaryGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Live GNSS Telemetry",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = WayfinderTextPrimary
                            )
                            Text(
                                text = if (locationState.isAvailable) "Active GPS Fix Acquired" else "Indoor / Cellular Fallback Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (locationState.isAvailable) WayfinderPrimaryGreen else WayfinderHazardCrack
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "LATITUDE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = WayfinderTextSecondary
                                )
                                Text(
                                    text = String.format(Locale.US, "%.4f°N", locationState.latitude),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = WayfinderTextPrimary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "LONGITUDE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = WayfinderTextSecondary
                                )
                                Text(
                                    text = String.format(Locale.US, "%.4f°E", locationState.longitude),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = WayfinderTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "GPS Sampling Frequency",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = WayfinderTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rateOptions.forEachIndexed { index, option ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (updateRateIndex == index) WayfinderPrimaryGreen.copy(alpha = 0.25f)
                                        else WayfinderDarkSurface.copy(alpha = 0.6f)
                                    )
                                    .border(
                                        1.dp,
                                        if (updateRateIndex == index) WayfinderPrimaryGreen else Color.White.copy(alpha = 0.12f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { updateRateIndex = index }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (updateRateIndex == index) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 10.sp
                                    ),
                                    color = if (updateRateIndex == index) WayfinderPrimaryGreen else WayfinderTextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Hazard Deduplication Radius",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = WayfinderTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        radiusOptions.forEachIndexed { index, option ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (deduplicationRadiusIndex == index) WayfinderPrimaryGreen.copy(alpha = 0.25f)
                                        else WayfinderDarkSurface.copy(alpha = 0.6f)
                                    )
                                    .border(
                                        1.dp,
                                        if (deduplicationRadiusIndex == index) WayfinderPrimaryGreen else Color.White.copy(alpha = 0.12f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { deduplicationRadiusIndex = index }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (deduplicationRadiusIndex == index) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 10.sp
                                    ),
                                    color = if (deduplicationRadiusIndex == index) WayfinderPrimaryGreen else WayfinderTextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Battery Optimization in Map",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = WayfinderTextPrimary
                            )
                            Text(
                                text = "Slows polling to 4s when viewing road map",
                                style = MaterialTheme.typography.labelSmall,
                                color = WayfinderTextSecondary
                            )
                        }
                        Switch(
                            checked = isPowerSaverEnabled,
                            onCheckedChange = { isPowerSaverEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = WayfinderPrimaryGreen,
                                checkedTrackColor = WayfinderPrimaryGreen.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            GlassButton(
                text = "Back to Settings",
                onClick = onBack,
                isPrimary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AudioAlertsSubPage(
    audioWarningManager: AudioWarningManager?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMuted by remember { mutableStateOf(audioWarningManager?.isMuted ?: false) }
    var warningDistanceIndex by remember { mutableIntStateOf(1) }
    var isCriticalOnly by remember { mutableStateOf(false) }

    val distanceOptions = listOf("30m (City)", "50m (Standard)", "75m (Highway)")

    NatureBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            SubPageHeader(
                title = "Driver Audio Alerts",
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Forward Warning Corridor",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = WayfinderTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Configure road hazard lookahead alert distance",
                        style = MaterialTheme.typography.bodySmall,
                        color = WayfinderTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        distanceOptions.forEachIndexed { index, option ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (warningDistanceIndex == index) WayfinderPrimaryGreen.copy(alpha = 0.25f)
                                        else WayfinderDarkSurface.copy(alpha = 0.6f)
                                    )
                                    .border(
                                        1.dp,
                                        if (warningDistanceIndex == index) WayfinderPrimaryGreen else Color.White.copy(alpha = 0.12f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { warningDistanceIndex = index }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (warningDistanceIndex == index) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 10.sp
                                    ),
                                    color = if (warningDistanceIndex == index) WayfinderPrimaryGreen else WayfinderTextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Filter: Critical Threats Only",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = WayfinderTextPrimary
                            )
                            Text(
                                text = "Only announce severe deep pothole craters",
                                style = MaterialTheme.typography.labelSmall,
                                color = WayfinderTextSecondary
                            )
                        }
                        Switch(
                            checked = isCriticalOnly,
                            onCheckedChange = { isCriticalOnly = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = WayfinderHazardPothole,
                                checkedTrackColor = WayfinderHazardPothole.copy(alpha = 0.3f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mute Driver Voice Alerts",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = WayfinderTextPrimary
                            )
                            Text(
                                text = "Silence spoken hazard audio announcements",
                                style = MaterialTheme.typography.labelSmall,
                                color = WayfinderTextSecondary
                            )
                        }

                        Switch(
                            checked = isMuted,
                            onCheckedChange = {
                                isMuted = it
                                audioWarningManager?.isMuted = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = WayfinderHazardPothole,
                                checkedTrackColor = WayfinderHazardPothole.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            GlassButton(
                text = "Back to Settings",
                onClick = onBack,
                isPrimary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AboutSubPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    NatureBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            SubPageHeader(
                title = "About Wayfinder",
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Wayfinder AI Road Guardian",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = WayfinderTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version 1.0.0 (Production Release)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WayfinderPrimaryGreen
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Next-generation on-device road condition intelligence powered by YOLO11 INT8 computer vision, sensor fusion telemetry, and real-time crowdsourced hazard synchronization.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WayfinderTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            GlassButton(
                text = "Back to Settings",
                onClick = onBack,
                isPrimary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SubPageHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(WayfinderDarkSurface.copy(alpha = 0.8f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = WayfinderTextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = WayfinderTextPrimary
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(WayfinderDarkSurface.copy(alpha = 0.9f))
                    .border(1.dp, WayfinderPrimaryGreen.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = WayfinderPrimaryGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = WayfinderTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = WayfinderTextSecondary
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = WayfinderTextSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}
