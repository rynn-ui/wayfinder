package com.roadguardian.app.domain.model

data class AccelerometerData(
    val x: Float = 0.0f,
    val y: Float = 0.0f,
    val z: Float = 0.0f,
    val impactMagnitude: Float = 0.0f,
    val isImpactConfirmed: Boolean = false
)
