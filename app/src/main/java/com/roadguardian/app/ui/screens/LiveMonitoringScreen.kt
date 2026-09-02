package com.roadguardian.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadguardian.app.ai.confirmation.ConfirmationResult
import com.roadguardian.app.ai.inference.AiBenchmarkSnapshot
import com.roadguardian.app.ai.inference.DetectorModelSignature
import com.roadguardian.app.ai.postprocessing.RawDetectionStats
import com.roadguardian.app.camera.CameraPreview
import com.roadguardian.app.camera.FrameMetadata
import com.roadguardian.app.camera.RoadFrameAnalyzer
import com.roadguardian.app.domain.model.HazardSeverity
import com.roadguardian.app.domain.model.RoadHazardDetection
import com.roadguardian.app.sensors.SensorTelemetry
import com.roadguardian.app.ui.components.GlassButton
import com.roadguardian.app.ui.components.GlassCard
import com.roadguardian.app.ui.components.GlassStatusChip
import com.roadguardian.app.ui.overlay.HazardDetectionOverlay
import com.roadguardian.app.ui.theme.WayfinderHazardCrack
import com.roadguardian.app.ui.theme.WayfinderHazardPothole
import com.roadguardian.app.ui.theme.WayfinderPrimaryGreen
import com.roadguardian.app.ui.theme.WayfinderSafeGreen
import com.roadguardian.app.ui.theme.WayfinderSage
import com.roadguardian.app.ui.theme.WayfinderTextPrimary
import com.roadguardian.app.ui.theme.WayfinderTextSecondary
import com.roadguardian.app.warning.HazardWarningState

@Composable
fun LiveMonitoringScreen(
    analyzer: RoadFrameAnalyzer,
    detections: List<RoadHazardDetection>,
    frameMetadata: FrameMetadata?,
    hazardsCount: Int,
    speedValue: String = "0",
    speedUnit: String = "km/h",
    benchmarkSnapshot: AiBenchmarkSnapshot = AiBenchmarkSnapshot(),
    sensorTelemetry: SensorTelemetry = SensorTelemetry(),
    confirmationResult: ConfirmationResult? = null,
    warningState: HazardWarningState = HazardWarningState(),
    isTtsEnabled: Boolean = true,
    debugRawCount: Int = 0,
    debugNmsCount: Int = 0,
    debugTrackedCount: Int = 0,
    debugConfirmedCount: Int = 0,
    debugReportedCount: Int = 0,
    modelSignature: DetectorModelSignature? = null,
    rawDiagStats: RawDetectionStats? = null,
    onToggleTts: () -> Unit = {},
    onStopMonitoring: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onStopMonitoring()
    }

    val bootProgress = remember { Animatable(0f) }
    var isBooting by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        bootProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1350, easing = FastOutSlowInEasing)
        )
        isBooting = false
    }

    val infiniteTransition = rememberInfiniteTransition(label = "warning_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            analyzer = analyzer
        )

        HazardDetectionOverlay(
            modifier = Modifier.fillMaxSize(),
            detections = detections,
            frameMetadata = frameMetadata
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xDD101713),
                            Color(0x88101713),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassStatusChip(
                    text = "Monitoring Active",
                    dotColor = Color(0xFF10B981),
                    animateDot = true,
                    backgroundColor = Color(0xF0071A10),
                    borderColor = Color(0x6610B981)
                )

                IconButton(
                    onClick = onToggleTts,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xF0071A10))
                        .border(1.dp, Color(0x6610B981), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isTtsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = "Voice Warning Toggle",
                        tint = if (isTtsEnabled) WayfinderPrimaryGreen else WayfinderTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = warningState.isWarningActive,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 58.dp, start = 16.dp, end = 16.dp)
        ) {
            val warnColor = when (warningState.severity) {
                HazardSeverity.CRITICAL, HazardSeverity.HIGH -> WayfinderHazardPothole
                HazardSeverity.MEDIUM -> WayfinderHazardCrack
                HazardSeverity.LOW -> WayfinderSafeGreen
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(pulseScale)
                    .shadow(16.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xEE1A0E0E))
                    .border(2.dp, warnColor, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(warnColor.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Warning",
                            tint = warnColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = warningState.warningText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = warnColor
                        )
                        Text(
                            text = "Approaching hazard corridor (${warningState.distanceMeters}m)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = WayfinderTextPrimary
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x80101713),
                            Color(0xF0101713)
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CURRENT SPEED",
                                style = MaterialTheme.typography.labelSmall,
                                color = WayfinderTextSecondary.copy(alpha = 0.75f),
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = speedValue,
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 40.sp,
                                        lineHeight = 44.sp
                                    ),
                                    color = WayfinderTextPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = speedUnit,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = WayfinderSage,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "CONFIRMED POTHOLES",
                                style = MaterialTheme.typography.labelSmall,
                                color = WayfinderTextSecondary.copy(alpha = 0.75f),
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$hazardsCount hazards",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (hazardsCount > 0) WayfinderHazardPothole else WayfinderPrimaryGreen
                            )
                        }
                    }
                }

                GlassButton(
                    text = "Stop Monitoring",
                    onClick = onStopMonitoring,
                    icon = Icons.Filled.StopCircle,
                    isPrimary = false,
                    isDanger = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (isBooting || bootProgress.value < 1f) {
            FuturisticHudBootOverlay(
                progress = bootProgress.value,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun FuturisticHudBootOverlay(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val alpha = if (progress < 0.75f) 1f else (1f - ((progress - 0.75f) / 0.25f)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .alpha(alpha)
            .background(Color.Black.copy(alpha = (1f - progress * 0.7f).coerceIn(0.3f, 1f)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            if (progress < 0.35f) {
                val flareRatio = (progress / 0.35f)
                val flareW = w * flareRatio
                val flareH = (h * 0.008f) + (h * 0.05f * (1f - flareRatio))

                drawRect(
                    color = Color(0x9934D399),
                    topLeft = Offset(cx - flareW / 2f, cy - flareH / 2f),
                    size = Size(flareW, flareH)
                )

                drawCircle(
                    color = Color.White,
                    radius = 24f * (1f - flareRatio),
                    center = Offset(cx, cy)
                )
            }

            if (progress >= 0.20f) {
                val hudRatio = ((progress - 0.20f) / 0.80f).coerceIn(0f, 1f)
                val inset = 36f * (1f - hudRatio) + 24f
                val bracketLen = 64f

                val bracketColor = Color(0xFF34D399).copy(alpha = 0.9f)
                val strokeW = 3.5f

                val tlPath = Path().apply {
                    moveTo(inset, inset + bracketLen)
                    lineTo(inset, inset)
                    lineTo(inset + bracketLen, inset)
                }
                drawPath(tlPath, bracketColor, style = Stroke(width = strokeW))

                val trPath = Path().apply {
                    moveTo(w - inset - bracketLen, inset)
                    lineTo(w - inset, inset)
                    lineTo(w - inset, inset + bracketLen)
                }
                drawPath(trPath, bracketColor, style = Stroke(width = strokeW))

                val blPath = Path().apply {
                    moveTo(inset, h - inset - bracketLen)
                    lineTo(inset, h - inset)
                    lineTo(inset + bracketLen, h - inset)
                }
                drawPath(blPath, bracketColor, style = Stroke(width = strokeW))

                val brPath = Path().apply {
                    moveTo(w - inset - bracketLen, h - inset)
                    lineTo(w - inset, h - inset)
                    lineTo(w - inset, h - inset - bracketLen)
                }
                drawPath(brPath, bracketColor, style = Stroke(width = strokeW))

                val reticleRadius = 40f + (hudRatio * 80f)
                drawCircle(
                    color = Color(0x5534D399),
                    radius = reticleRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2f)
                )

                val sweepAngle = hudRatio * 360f
                drawArc(
                    color = Color(0xFF34D399),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(cx - reticleRadius, cy - reticleRadius),
                    size = Size(reticleRadius * 2f, reticleRadius * 2f),
                    style = Stroke(width = 3f)
                )

                drawLine(
                    color = Color(0x8834D399),
                    start = Offset(cx - reticleRadius - 20f, cy),
                    end = Offset(cx + reticleRadius + 20f, cy),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color(0x8834D399),
                    start = Offset(cx, cy - reticleRadius - 20f),
                    end = Offset(cx, cy + reticleRadius + 20f),
                    strokeWidth = 1.5f
                )
            }
        }

        if (progress in 0.25f..0.95f) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xE60D1410))
                    .border(1.dp, Color(0xFF34D399), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34D399))
                    )
                    Text(
                        text = "OPTICAL NEURAL HUD ONLINE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            fontSize = 11.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}
