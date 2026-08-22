package com.roadguardian.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadguardian.app.ui.navigation.WayfinderScreen
import com.roadguardian.app.ui.theme.WayfinderDarkSurface
import com.roadguardian.app.ui.theme.WayfinderGlassBorder
import com.roadguardian.app.ui.theme.WayfinderGlassHighlight
import com.roadguardian.app.ui.theme.WayfinderPrimaryGreen
import com.roadguardian.app.ui.theme.WayfinderTextSecondary

@Composable
fun WayfinderBottomNav(
    currentScreen: WayfinderScreen,
    onNavigate: (WayfinderScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating glass pill navigation bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color.Black.copy(alpha = 0.5f),
                    ambientColor = Color.Black.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            WayfinderDarkSurface.copy(alpha = 0.88f),
                            Color(0xFF0F1713).copy(alpha = 0.94f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            WayfinderGlassHighlight.copy(alpha = 0.28f),
                            WayfinderGlassBorder.copy(alpha = 0.14f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                label = "Map",
                activeIcon = Icons.Filled.Map,
                inactiveIcon = Icons.Outlined.Map,
                isSelected = currentScreen == WayfinderScreen.MAP,
                onClick = { onNavigate(WayfinderScreen.MAP) }
            )

            BottomNavItem(
                label = "Home",
                activeIcon = Icons.Filled.Shield,
                inactiveIcon = Icons.Outlined.Shield,
                isSelected = currentScreen == WayfinderScreen.HOME,
                onClick = { onNavigate(WayfinderScreen.HOME) }
            )

            BottomNavItem(
                label = "History",
                activeIcon = Icons.Filled.History,
                inactiveIcon = Icons.Outlined.History,
                isSelected = currentScreen == WayfinderScreen.HISTORY,
                onClick = { onNavigate(WayfinderScreen.HISTORY) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) WayfinderPrimaryGreen.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(250),
        label = "nav_container"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) WayfinderPrimaryGreen else WayfinderTextSecondary,
        animationSpec = tween(250),
        label = "nav_content"
    )

    val borderStrokeColor by animateColorAsState(
        targetValue = if (isSelected) WayfinderPrimaryGreen.copy(alpha = 0.32f) else Color.Transparent,
        animationSpec = tween(250),
        label = "nav_border"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, borderStrokeColor, CircleShape)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = if (isSelected) activeIcon else inactiveIcon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                letterSpacing = 0.4.sp
            ),
            color = contentColor
        )
    }
}
