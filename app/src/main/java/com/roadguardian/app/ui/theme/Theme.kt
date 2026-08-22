package com.roadguardian.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LeafyGreenColorScheme = darkColorScheme(
    primary = WayfinderPrimaryGreen,
    onPrimary = WayfinderOnPrimary,
    primaryContainer = WayfinderDeepNaturalGreen,
    onPrimaryContainer = WayfinderLightGreenery,
    inversePrimary = WayfinderSage,

    secondary = WayfinderSage,
    onSecondary = WayfinderOnSecondary,
    secondaryContainer = WayfinderSecondaryContainer,
    onSecondaryContainer = WayfinderLightGreenery,

    tertiary = WayfinderMutedForest,
    onTertiary = WayfinderOnTertiary,
    tertiaryContainer = WayfinderTertiaryContainer,
    onTertiaryContainer = WayfinderSage,

    error = WayfinderHazardPothole,
    onError = WayfinderOnError,
    errorContainer = WayfinderErrorContainer,
    onErrorContainer = WayfinderOnErrorContainer,

    background = WayfinderDarkBackground,
    onBackground = WayfinderTextPrimary,

    surface = WayfinderDarkSurface,
    onSurface = WayfinderTextPrimary,
    surfaceVariant = WayfinderDarkSurfaceVariant,
    onSurfaceVariant = WayfinderTextSecondary,
    surfaceContainer = WayfinderDarkSurfaceContainer,
    surfaceContainerHigh = WayfinderDarkSurfaceContainerHigh,
    surfaceContainerHighest = WayfinderDarkSurfaceContainerHighest,
    surfaceContainerLow = WayfinderDarkSurfaceContainerLow,
    surfaceContainerLowest = WayfinderDarkSurfaceContainerLowest,
    surfaceDim = WayfinderDarkSurfaceContainerLowest,
    surfaceBright = WayfinderDarkSurfaceVariant,

    outline = WayfinderOutline,
    outlineVariant = WayfinderOutlineVariant
)

@Composable
fun AIRoadGuardianTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LeafyGreenColorScheme,
        typography = Typography,
        content = content
    )
}
