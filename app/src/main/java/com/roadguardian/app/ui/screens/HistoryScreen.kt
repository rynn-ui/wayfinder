package com.roadguardian.app.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadguardian.app.domain.model.RoadHazard
import com.roadguardian.app.ui.components.GlassButton
import com.roadguardian.app.ui.components.GlassCard
import com.roadguardian.app.ui.components.NatureBackground
import com.roadguardian.app.ui.theme.WayfinderDarkSurface
import com.roadguardian.app.ui.theme.WayfinderHazardCrack
import com.roadguardian.app.ui.theme.WayfinderHazardPothole
import com.roadguardian.app.ui.theme.WayfinderPrimaryGreen
import com.roadguardian.app.ui.theme.WayfinderSafeGreen
import com.roadguardian.app.ui.theme.WayfinderSage
import com.roadguardian.app.ui.theme.WayfinderTextPrimary
import com.roadguardian.app.ui.theme.WayfinderTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class HistoryFilter {
    ALL,
    CRITICAL,
    HIGH_CONFIDENCE,
    RECENT
}

@Composable
fun HistoryScreen(
    hazards: List<RoadHazard> = emptyList(),
    onClearHistory: () -> Unit = {},
    onNavigateToHazardOnMap: (RoadHazard) -> Unit = {},
    onStartMonitoring: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var showClearDialog by remember { mutableStateOf(false) }

    val thirtyDaysAgo = remember { System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L) }
    val validHazards = remember(hazards) {
        hazards.filter { it.lastSeenAt >= thirtyDaysAgo || it.timestamp >= thirtyDaysAgo }
    }

    val sortedHazards = remember(validHazards) {
        validHazards.sortedByDescending { it.lastSeenAt }
    }

    val filteredHazards = remember(sortedHazards, selectedFilter) {
        when (selectedFilter) {
            HistoryFilter.ALL -> sortedHazards
            HistoryFilter.CRITICAL -> sortedHazards.filter { it.isCritical }
            HistoryFilter.HIGH_CONFIDENCE -> sortedHazards.filter { it.confidence >= 0.75f }
            HistoryFilter.RECENT -> {
                val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
                sortedHazards.filter { it.lastSeenAt >= oneDayAgo }
            }
        }
    }

    val totalCount = validHazards.size
    val criticalCount = validHazards.count { it.isCritical }
    val avgConfidence = if (validHazards.isNotEmpty()) {
        (validHazards.map { it.confidence }.average() * 100).toInt()
    } else 0

    val healthScore = if (validHazards.isEmpty()) 100 else maxOf(35, 100 - (criticalCount * 8 + (totalCount - criticalCount) * 3))

    fun exportRoadAudit() {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val sb = StringBuilder()
        sb.appendLine("WAYFINDER ROAD CONDITION AUDIT REPORT")
        sb.appendLine("Generated: $dateStr")
        sb.appendLine("Total Hazards Logged: $totalCount")
        sb.appendLine("Critical Threats: $criticalCount")
        sb.appendLine("Road Health Score: $healthScore/100")
        sb.appendLine("30-Day Auto Retention: Active")
        sb.appendLine("----------------------------------------")
        sortedHazards.take(50).forEachIndexed { index, hazard ->
            val coord = String.format(Locale.US, "%.5f, %.5f", hazard.deviceLatitude, hazard.deviceLongitude)
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(hazard.lastSeenAt))
            sb.appendLine("#${index + 1} • ${hazard.displayTitle.uppercase()} (${hazard.severity.uppercase()})")
            sb.appendLine("  Coords: $coord")
            sb.appendLine("  AI Conf: ${(hazard.confidence * 100).toInt()}% • Verifications: ${hazard.confirmationCount}")
            sb.appendLine("  Detected: $time")
            sb.appendLine("----------------------------------------")
        }
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Road Audit Report"))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "Clear Hazard History?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = WayfinderTextPrimary
                )
            },
            text = {
                Text(
                    text = "This will remove all logged road hazard records from your history. Old records past 30 days are also automatically pruned.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WayfinderTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearHistory()
                    }
                ) {
                    Text(
                        text = "Clear All",
                        color = WayfinderHazardPothole,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = "Cancel", color = WayfinderTextSecondary)
                }
            },
            containerColor = WayfinderDarkSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    NatureBackground(modifier = modifier) {
        if (validHazards.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(WayfinderDarkSurface.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = "No History",
                        tint = WayfinderPrimaryGreen.copy(alpha = 0.7f),
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "No Hazard History Logged",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = WayfinderTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Potholes detected while driving or monitoring will appear here with telemetry and cloud sync logs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WayfinderTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(280.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                GlassButton(
                    text = "Start Road Monitoring",
                    onClick = onStartMonitoring,
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth(0.75f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hazard History",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = WayfinderTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Real-time road condition audit and detection logs.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WayfinderTextSecondary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showClearDialog = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(WayfinderDarkSurface.copy(alpha = 0.8f))
                                    .border(1.dp, WayfinderHazardPothole.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Clear History",
                                    tint = WayfinderHazardPothole,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { exportRoadAudit() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(WayfinderDarkSurface.copy(alpha = 0.8f))
                                    .border(1.dp, WayfinderPrimaryGreen.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Export Report",
                                    tint = WayfinderPrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HistoryMetricCard(
                            label = "TOTAL THREATS",
                            value = "$totalCount",
                            accentColor = WayfinderHazardPothole,
                            modifier = Modifier.weight(1f)
                        )
                        HistoryMetricCard(
                            label = "CRITICAL",
                            value = "$criticalCount",
                            accentColor = WayfinderHazardCrack,
                            modifier = Modifier.weight(1f)
                        )
                        HistoryMetricCard(
                            label = "AVG CONF",
                            value = "$avgConfidence%",
                            accentColor = WayfinderPrimaryGreen,
                            modifier = Modifier.weight(1f)
                        )
                        HistoryMetricCard(
                            label = "ROAD SCORE",
                            value = "$healthScore/100",
                            accentColor = if (healthScore > 75) WayfinderSafeGreen else WayfinderHazardCrack,
                            modifier = Modifier.weight(1.1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoDelete,
                            contentDescription = null,
                            tint = WayfinderSage,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "30-Day Auto Retention Active • Older logs pruned automatically",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = WayfinderSage
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                label = "All ($totalCount)",
                                isSelected = selectedFilter == HistoryFilter.ALL,
                                onClick = { selectedFilter = HistoryFilter.ALL }
                            )
                        }
                        item {
                            FilterChip(
                                label = "Critical ($criticalCount)",
                                isSelected = selectedFilter == HistoryFilter.CRITICAL,
                                onClick = { selectedFilter = HistoryFilter.CRITICAL }
                            )
                        }
                        item {
                            FilterChip(
                                label = "High Conf (>75%)",
                                isSelected = selectedFilter == HistoryFilter.HIGH_CONFIDENCE,
                                onClick = { selectedFilter = HistoryFilter.HIGH_CONFIDENCE }
                            )
                        }
                        item {
                            FilterChip(
                                label = "Recent (24h)",
                                isSelected = selectedFilter == HistoryFilter.RECENT,
                                onClick = { selectedFilter = HistoryFilter.RECENT }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (filteredHazards.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hazards match the selected filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WayfinderTextSecondary
                            )
                        }
                    }
                } else {
                    items(filteredHazards, key = { it.id }) { hazard ->
                        HistoryHazardCard(
                            hazard = hazard,
                            onViewOnMap = { onNavigateToHazardOnMap(hazard) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun HistoryMetricCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = WayfinderTextSecondary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) WayfinderPrimaryGreen.copy(alpha = 0.22f)
                else WayfinderDarkSurface.copy(alpha = 0.6f)
            )
            .border(
                1.dp,
                if (isSelected) WayfinderPrimaryGreen else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) WayfinderPrimaryGreen else WayfinderTextSecondary
        )
    }
}

@Composable
private fun HistoryHazardCard(
    hazard: RoadHazard,
    onViewOnMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCritical = hazard.isCritical
    val cardBorderColor = if (isCritical) WayfinderHazardPothole.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.10f)

    val formattedCoords = String.format(Locale.US, "%.4f°N, %.4f°E", hazard.deviceLatitude, hazard.deviceLongitude)
    val relativeTime = remember(hazard.lastSeenAt) {
        val diff = System.currentTimeMillis() - hazard.lastSeenAt
        when {
            diff < 60_000L -> "Just now"
            diff < 3600_000L -> "${diff / 60_000L} min ago"
            diff < 86400_000L -> "${diff / 3600_000L} hr ago"
            else -> SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date(hazard.lastSeenAt))
        }
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCritical) WayfinderHazardPothole.copy(alpha = 0.22f)
                                else WayfinderHazardCrack.copy(alpha = 0.18f)
                            )
                            .border(
                                1.dp,
                                if (isCritical) WayfinderHazardPothole else WayfinderHazardCrack,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (isCritical) WayfinderHazardPothole else WayfinderHazardCrack,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = hazard.displayTitle.ifBlank { "Pothole Hazard" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = WayfinderTextPrimary
                        )
                        Text(
                            text = relativeTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = WayfinderTextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isCritical) WayfinderHazardPothole.copy(alpha = 0.20f)
                            else WayfinderPrimaryGreen.copy(alpha = 0.15f)
                        )
                        .border(
                            1.dp,
                            if (isCritical) WayfinderHazardPothole else WayfinderPrimaryGreen,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = hazard.severity.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isCritical) WayfinderHazardPothole else WayfinderPrimaryGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryTag(
                    label = "CONF",
                    value = "${(hazard.confidence * 100).toInt()}%",
                    accentColor = WayfinderPrimaryGreen,
                    modifier = Modifier.weight(1f)
                )
                TelemetryTag(
                    label = "REPORTS",
                    value = "${hazard.confirmationCount}x",
                    accentColor = WayfinderSage,
                    modifier = Modifier.weight(1f)
                )
                TelemetryTag(
                    label = "SYNC",
                    value = "Cloud",
                    accentColor = WayfinderSafeGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = WayfinderSage,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = formattedCoords,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = WayfinderTextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(WayfinderPrimaryGreen.copy(alpha = 0.12f))
                    .clickable(onClick = onViewOnMap)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "View Threat on Interactive Map",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = WayfinderPrimaryGreen
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = WayfinderPrimaryGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TelemetryTag(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = WayfinderTextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor
            )
        }
    }
}
