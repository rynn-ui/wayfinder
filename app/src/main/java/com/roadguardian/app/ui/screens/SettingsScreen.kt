package com.roadguardian.app.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadguardian.app.ai.inference.AiModelType
import com.roadguardian.app.ui.components.GlassCard
import com.roadguardian.app.ui.components.GlassDivider
import com.roadguardian.app.ui.components.NatureBackground
import com.roadguardian.app.ui.theme.WayfinderPrimaryGreen
import com.roadguardian.app.ui.theme.WayfinderSage
import com.roadguardian.app.ui.theme.WayfinderTextPrimary
import com.roadguardian.app.ui.theme.WayfinderTextSecondary

@Composable
fun SettingsScreen(
    activeModel: AiModelType = AiModelType.INT8,
    onSelectModel: (AiModelType) -> Unit = {},
    onNavigateCalibration: () -> Unit = {},
    onNavigateGps: () -> Unit = {},
    onNavigateNotifications: () -> Unit = {},
    onNavigateAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    NatureBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = WayfinderTextPrimary
            )

            Spacer(modifier = Modifier.height(18.dp))

            // AI Model A/B Testing Section
            Text(
                text = "AI MODEL (A/B BENCHMARK)",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = WayfinderPrimaryGreen.copy(alpha = 0.85f),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column {
                    AiModelType.entries.forEachIndexed { index, model ->
                        if (index > 0) {
                            GlassDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        SettingsModelSelectionItem(
                            model = model,
                            isSelected = activeModel == model,
                            onClick = { onSelectModel(model) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Camera & Sensors Group Card
            Text(
                text = "SYSTEM & SENSORS",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = WayfinderPrimaryGreen.copy(alpha = 0.85f),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column {
                    SettingsNavigationItem(
                        icon = Icons.Filled.Videocam,
                        title = "Camera Calibration",
                        subtitle = "Adjust lens mapping and alignment",
                        onClick = onNavigateCalibration
                    )

                    GlassDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsNavigationItem(
                        icon = Icons.Filled.LocationSearching,
                        title = "GPS Accuracy Settings",
                        subtitle = "Manage location precision and battery usage",
                        onClick = onNavigateGps
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Preferences Group Card
            Text(
                text = "PREFERENCES",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = WayfinderPrimaryGreen.copy(alpha = 0.85f),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                SettingsNavigationItem(
                    icon = Icons.Filled.Notifications,
                    title = "Notification Preferences",
                    subtitle = "Safety alerts and route updates",
                    onClick = onNavigateNotifications
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // About Group Card
            Text(
                text = "ABOUT",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = WayfinderPrimaryGreen.copy(alpha = 0.85f),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                SettingsNavigationItem(
                    icon = Icons.Filled.Info,
                    title = "About Wayfinder",
                    subtitle = "Version 1.0.0 (A/B Benchmark Edition)",
                    onClick = onNavigateAbout
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SettingsModelSelectionItem(
    model: AiModelType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Filled.Psychology,
                contentDescription = model.displayName,
                tint = if (isSelected) WayfinderPrimaryGreen else WayfinderTextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = model.displayName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = WayfinderTextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = model.precisionLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp
                        ),
                        color = if (isSelected) WayfinderSage else WayfinderTextSecondary.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WayfinderTextSecondary
                )
            }
        }

        // Radio circle indicator
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = if (isSelected) WayfinderPrimaryGreen else WayfinderTextSecondary.copy(alpha = 0.4f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(WayfinderPrimaryGreen)
                )
            }
        }
    }
}

@Composable
private fun SettingsNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = WayfinderPrimaryGreen,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = WayfinderTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WayfinderTextSecondary
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = WayfinderTextSecondary.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}
