package com.roadguardian.app.ai.config

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class DetectionConfig {
    var confidenceThreshold by mutableFloatStateOf(0.35f)
    var iouThreshold by mutableFloatStateOf(0.45f)
    var minConsecutiveDetections by mutableIntStateOf(3)
    var maxFrameGap by mutableIntStateOf(2)
    var cooldownMillis by mutableLongStateOf(4000L)
    var debugHudEnabled by mutableStateOf(false)
}
