package com.roadguardian.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadguardian.app.ui.theme.WayfinderDarkBackground
import com.roadguardian.app.ui.theme.WayfinderHazardPothole
import com.roadguardian.app.ui.theme.WayfinderLightGreenery
import com.roadguardian.app.ui.theme.WayfinderPrimaryGreen
import com.roadguardian.app.ui.theme.WayfinderSage
import com.roadguardian.app.ui.theme.WayfinderTextPrimary
import com.roadguardian.app.ui.theme.WayfinderTextSecondary

@Composable
fun WayfinderBrandHeading(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Wayfinder",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                fontSize = fontSize,
                letterSpacing = 0.5.sp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF1F6EE),
                        WayfinderLightGreenery,
                        WayfinderPrimaryGreen
                    )
                )
            )
        )

        Canvas(
            modifier = Modifier
                .width(118.dp)
                .height(6.dp)
                .padding(top = 1.dp)
        ) {
            val path = Path().apply {
                moveTo(size.width * 0.04f, size.height * 0.25f)
                quadraticTo(
                    size.width * 0.42f, size.height * 1.0f,
                    size.width * 0.96f, size.height * 0.20f
                )
            }
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    listOf(
                        WayfinderPrimaryGreen.copy(alpha = 0.25f),
                        WayfinderPrimaryGreen.copy(alpha = 0.90f),
                        WayfinderSage.copy(alpha = 0.85f),
                        Color.Transparent
                    )
                ),
                style = Stroke(
                    width = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

@Composable
fun WayfinderTopAppBar(
    title: String = "Wayfinder",
    showBackButton: Boolean = false,
    hazardCount: Int? = null,
    onMenuClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        WayfinderDarkBackground.copy(alpha = 0.96f),
                        WayfinderDarkBackground.copy(alpha = 0.85f)
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (showBackButton) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = WayfinderTextSecondary
                    )
                }
            } else {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = "Menu",
                        tint = WayfinderPrimaryGreen
                    )
                }
            }

            if (title == "Wayfinder") {
                Box(
                    modifier = if (hazardCount == null && !showBackButton) Modifier.weight(1f) else Modifier,
                    contentAlignment = Alignment.Center
                ) {
                    WayfinderBrandHeading()
                }
            } else {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp,
                        letterSpacing = 0.3.sp,
                        color = WayfinderTextPrimary
                    ),
                    modifier = if (hazardCount == null && !showBackButton) Modifier.weight(1f) else Modifier,
                    textAlign = if (hazardCount == null && !showBackButton) TextAlign.Center else TextAlign.Start
                )
            }

            if (hazardCount != null) {
                GlassStatusChip(
                    text = "$hazardCount Hazards",
                    dotColor = if (hazardCount > 0) WayfinderHazardPothole else WayfinderPrimaryGreen,
                    animateDot = hazardCount > 0
                )
            } else {
                Spacer(modifier = Modifier.size(40.dp))
            }
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.08f),
            thickness = 1.dp
        )
    }
}
