package com.roadguardian.app.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// Wayfinder Leafy-Green Glassmorphic Palette
// ==========================================

// Core Backgrounds
val WayfinderDarkBackground = Color(0xFF101713)       // Deep natural dark
val WayfinderDarkSurface = Color(0xFF16211B)          // Dark forest surface
val WayfinderDarkSurfaceVariant = Color(0xFF1E2B24)   // Elevated forest container
val WayfinderDarkSurfaceContainer = Color(0xFF1A2620)
val WayfinderDarkSurfaceContainerHigh = Color(0xFF22322A)
val WayfinderDarkSurfaceContainerHighest = Color(0xFF2B3D34)
val WayfinderDarkSurfaceContainerLow = Color(0xFF131D17)
val WayfinderDarkSurfaceContainerLowest = Color(0xFF0C120F)

// Primary Leafy Greens & Sages
val WayfinderPrimaryGreen = Color(0xFFA8C98F)          // Primary leafy green
val WayfinderSage = Color(0xFFB9D5A5)                  // Soft sage accent
val WayfinderLightGreenery = Color(0xFFD5E7C8)          // Pale greenery highlight
val WayfinderMutedForest = Color(0xFF506B58)           // Muted deep forest
val WayfinderDeepNaturalGreen = Color(0xFF263A2E)      // Deep natural container green

// Text & Typography
val WayfinderTextPrimary = Color(0xFFF1F5EF)           // Soft off-white
val WayfinderTextSecondary = Color(0xFFC4CEC4)         // Muted fog / sage gray
val WayfinderTextTertiary = Color(0xFF8B9B8E)          // Subtle muted sage

// Glass & Borders (Translucent & Soft)
val WayfinderGlassSurface = Color(0x1AFFFFFF)          // ~10% white for glass cards
val WayfinderGlassSurfaceElevated = Color(0x26FFFFFF)  // ~15% white for interactive glass
val WayfinderGlassBorder = Color(0x24FFFFFF)           // ~14% white for delicate borders
val WayfinderGlassHighlight = Color(0x40FFFFFF)        // ~25% white for top rim highlights
val WayfinderGlassGreenTint = Color(0x1FA8C98F)        // ~12% leafy green tint

// Hazard & State Indicators (Muted & Calm, Non-Neon)
val WayfinderHazardPothole = Color(0xFFC46054)         // Muted warm red
val WayfinderHazardCrack = Color(0xFFC4884D)           // Muted amber
val WayfinderHazardLongitudinal = Color(0xFFA8C98F)    // Leafy green
val WayfinderHazardTransverse = Color(0xFF6B9E7A)      // Forest green
val WayfinderSafeGreen = Color(0xFF6B9E7A)             // Soft safe green

// Semantic Palette for Material 3 Compatibility
val WayfinderPrimary = WayfinderPrimaryGreen
val WayfinderOnPrimary = Color(0xFF101713)
val WayfinderPrimaryContainer = WayfinderDeepNaturalGreen
val WayfinderOnPrimaryContainer = WayfinderLightGreenery

val WayfinderSecondary = WayfinderSage
val WayfinderOnSecondary = Color(0xFF101713)
val WayfinderSecondaryContainer = Color(0xFF24352B)
val WayfinderOnSecondaryContainer = WayfinderLightGreenery

val WayfinderTertiary = WayfinderMutedForest
val WayfinderOnTertiary = Color(0xFFF1F5EF)
val WayfinderTertiaryContainer = Color(0xFF2E4236)
val WayfinderOnTertiaryContainer = WayfinderSage

val WayfinderError = WayfinderHazardPothole
val WayfinderOnError = Color(0xFFFFFFFF)
val WayfinderErrorContainer = Color(0xFF4A1F1B)
val WayfinderOnErrorContainer = Color(0xFFFFDAD6)

val WayfinderOutline = Color(0xFF425347)
val WayfinderOutlineVariant = Color(0x24FFFFFF)
