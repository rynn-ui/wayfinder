package com.roadguardian.app.sensors

data class AccelerometerResult(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val magnitude: Float = 0f,
    val linearAcceleration: Float = 0f,
    val filteredAcceleration: Float = 0f,
    val impactDetected: Boolean = false,
    val timestamp: Long = 0L
)

data class GyroscopeResult(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val angularVelocity: Float = 0f,
    val filteredRotation: Float = 0f,
    val rotationDetected: Boolean = false,
    val timestamp: Long = 0L
)

data class SensorTelemetry(
    val accelerometerX: Float = 0f,
    val accelerometerY: Float = 0f,
    val accelerometerZ: Float = 0f,
    val accelerationMagnitude: Float = 0f,
    val linearAcceleration: Float = 0f,
    val filteredAcceleration: Float = 0f,
    val impactDetected: Boolean = false,
    val gyroscopeX: Float = 0f,
    val gyroscopeY: Float = 0f,
    val gyroscopeZ: Float = 0f,
    val angularVelocity: Float = 0f,
    val filteredRotation: Float = 0f,
    val rotationDetected: Boolean = false,
    val accelerometerAvailable: Boolean = true,
    val gyroscopeAvailable: Boolean = true,
    val timestamp: Long = 0L
)
