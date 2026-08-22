package com.roadguardian.app.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadguardian.app.ai.inference.AiBenchmarkSnapshot
import com.roadguardian.app.camera.CameraPreview
import com.roadguardian.app.camera.FrameMetadata
import com.roadguardian.app.camera.RoadFrameAnalyzer
import com.roadguardian.app.domain.model.RoadHazardDetection
import com.roadguardian.app.ui.components.GlassButton
import com.roadguardian.app.ui.components.GlassCard
import com.roadguardian.app.ui.components.GlassStatusChip
import com.roadguardian.app.ui.overlay.HazardDetectionOverlay
import com.roadguardian.app.ui.theme.WayfinderHazardPothole
import com.roadguardian.app.ui.theme.WayfinderPrimaryGreen
import com.roadguardian.app.ui.theme.WayfinderSage
import com.roadguardian.app.ui.theme.WayfinderTextPrimary
import com.roadguardian.app.ui.theme.WayfinderTextSecondary

@Composable
fun LiveMonitoringScreen(
    analyzer: RoadFrameAnalyzer,
    detections: List<RoadHazardDetection>,
    frameMetadata: FrameMetadata?,
    totalDetectionsCount: Int,
    benchmarkSnapshot: AiBenchmarkSnapshot = AiBenchmarkSnapshot(),
    onStopMonitoring: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Real CameraX Preview Feed (UNTOUCHED)
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            analyzer = analyzer
        )

        // Real Hazard Detection Overlay (bounding boxes + orientation-aware labels) (UNTOUCHED LOGIC)
        HazardDetectionOverlay(
            modifier = Modifier.fillMaxSize(),
            detections = detections,
            frameMetadata = frameMetadata
        )

        // Top Floating Glass Status Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC101713),
                            Color(0x66101713),
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
                // Monitoring Active Status Chip
                GlassStatusChip(
                    text = "Monitoring Active",
                    dotColor = WayfinderPrimaryGreen,
                    animateDot = true
                )

                // Status Icons in muted glass circle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SignalCellular4Bar,
                        contentDescription = "Signal",
                        tint = WayfinderSage.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                    Icon(
                        imageVector = Icons.Filled.BatteryFull,
                        contentDescription = "Battery",
                        tint = WayfinderSage.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Developer A/B Benchmark HUD (Floating Glass Overlay)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 20.dp, top = 60.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xDD101713))
                    .border(
                        width = 1.dp,
                        color = WayfinderPrimaryGreen.copy(alpha = 0.30f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (benchmarkSnapshot.isLoading) {
                    Text(
                        text = "Loading ${benchmarkSnapshot.activeModel.displayName}...",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = WayfinderSage
                    )
                } else {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AI: ${benchmarkSnapshot.activeModel.displayName}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = WayfinderPrimaryGreen
                            )
                            if (benchmarkSnapshot.isWarmingUp) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(Warmup ${15 - benchmarkSnapshot.warmupRemaining}/15)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp
                                    ),
                                    color = WayfinderTextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "FPS: ${benchmarkSnapshot.inferenceFps.toInt()}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp
                                ),
                                color = WayfinderTextPrimary
                            )
                            Text(
                                text = "Inference: ${benchmarkSnapshot.lastLatencyMs.toInt()} ms",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp
                                ),
                                color = WayfinderTextSecondary
                            )
                            Text(
                                text = "Detections: ${benchmarkSnapshot.currentDetectionsCount}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp
                                ),
                                color = if (benchmarkSnapshot.currentDetectionsCount > 0) WayfinderPrimaryGreen else WayfinderTextSecondary
                            )
                        }
                        if (benchmarkSnapshot.currentDetectionsCount > 0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Avg Conf: ${String.format("%.2f", benchmarkSnapshot.currentAvgConfidence)} (Max: ${String.format("%.2f", benchmarkSnapshot.currentMaxConfidence)})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp
                                ),
                                color = WayfinderSage
                            )
                        }
                    }
                }
            }
        }

        // Bottom Telemetry & Controls Floating Glass Panel
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
                // Floating Telemetry Glass Card
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
                        // Current Speed
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
                                    text = "0",
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 40.sp,
                                        lineHeight = 44.sp
                                    ),
                                    color = WayfinderTextPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "km/h",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = WayfinderSage,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        // Total Detections Count
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TOTAL DETECTIONS",
                                style = MaterialTheme.typography.labelSmall,
                                color = WayfinderTextSecondary.copy(alpha = 0.75f),
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalDetectionsCount hazards",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (totalDetectionsCount > 0) WayfinderHazardPothole else WayfinderPrimaryGreen
                            )
                        }
                    }
                }

                // Stop Monitoring Glass Button (Soft Red Glass)
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
    }
}
