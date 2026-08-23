package com.roadguardian.app.sensors

import java.util.ArrayDeque
import kotlin.math.sqrt

class GyroscopeProcessor(
    val rotationThreshold: Float = DEFAULT_ROTATION_THRESHOLD,
    val cooldownMillis: Long = DEFAULT_COOLDOWN_MILLIS,
    val filterSize: Int = DEFAULT_FILTER_SIZE
) {

    companion object {
        const val DEFAULT_ROTATION_THRESHOLD = 2.0f
        const val DEFAULT_COOLDOWN_MILLIS = 2000L
        const val DEFAULT_FILTER_SIZE = 5
    }

    private val rotationHistory = ArrayDeque<Float>(filterSize)
    private var lastDetectionTime = 0L
    private var lastResult = GyroscopeResult()

    @Synchronized
    fun process(
        x: Float,
        y: Float,
        z: Float,
        timestampMillis: Long = System.currentTimeMillis()
    ): GyroscopeResult {
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) {
            return lastResult
        }

        val angularVelocity = sqrt(x * x + y * y + z * z)

        if (rotationHistory.size >= filterSize) {
            rotationHistory.removeFirst()
        }
        rotationHistory.addLast(angularVelocity)

        val filteredRotation = rotationHistory.sum() / rotationHistory.size

        var rotationDetected = false
        if (filteredRotation > rotationThreshold) {
            if (lastDetectionTime == 0L || timestampMillis - lastDetectionTime >= cooldownMillis) {
                rotationDetected = true
                lastDetectionTime = timestampMillis
            }
        }

        val result = GyroscopeResult(
            x = x,
            y = y,
            z = z,
            angularVelocity = angularVelocity,
            filteredRotation = filteredRotation,
            rotationDetected = rotationDetected,
            timestamp = timestampMillis
        )
        lastResult = result
        return result
    }

    @Synchronized
    fun reset() {
        rotationHistory.clear()
        lastDetectionTime = 0L
        lastResult = GyroscopeResult()
    }
}
