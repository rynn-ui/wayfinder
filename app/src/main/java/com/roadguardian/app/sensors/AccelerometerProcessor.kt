package com.roadguardian.app.sensors

import java.util.ArrayDeque
import kotlin.math.sqrt

class AccelerometerProcessor(
    val impactThreshold: Float = DEFAULT_IMPACT_THRESHOLD,
    val cooldownMillis: Long = DEFAULT_COOLDOWN_MILLIS,
    val filterSize: Int = DEFAULT_FILTER_SIZE,
    val alpha: Float = DEFAULT_ALPHA
) {

    companion object {
        const val DEFAULT_GRAVITY = 9.81f
        const val DEFAULT_IMPACT_THRESHOLD = 4.0f
        const val DEFAULT_COOLDOWN_MILLIS = 2000L
        const val DEFAULT_FILTER_SIZE = 5
        const val DEFAULT_ALPHA = 0.8f
    }

    private val gravity = FloatArray(3)
    private var gravityInitialized = false
    private val accelerationHistory = ArrayDeque<Float>(filterSize)
    private var lastDetectionTime = 0L
    private var lastResult = AccelerometerResult()

    @Synchronized
    fun process(
        x: Float,
        y: Float,
        z: Float,
        timestampMillis: Long = System.currentTimeMillis()
    ): AccelerometerResult {
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) {
            return lastResult
        }

        val magnitude = sqrt(x * x + y * y + z * z)

        if (!gravityInitialized) {
            gravity[0] = x
            gravity[1] = y
            gravity[2] = z
            gravityInitialized = true
        } else {
            gravity[0] = alpha * gravity[0] + (1f - alpha) * x
            gravity[1] = alpha * gravity[1] + (1f - alpha) * y
            gravity[2] = alpha * gravity[2] + (1f - alpha) * z
        }

        val linearX = x - gravity[0]
        val linearY = y - gravity[1]
        val linearZ = z - gravity[2]
        val linearAcceleration = sqrt(linearX * linearX + linearY * linearY + linearZ * linearZ)

        if (accelerationHistory.size >= filterSize) {
            accelerationHistory.removeFirst()
        }
        accelerationHistory.addLast(linearAcceleration)

        val filteredAcceleration = accelerationHistory.sum() / accelerationHistory.size

        var impactDetected = false
        if (filteredAcceleration > impactThreshold) {
            if (lastDetectionTime == 0L || timestampMillis - lastDetectionTime >= cooldownMillis) {
                impactDetected = true
                lastDetectionTime = timestampMillis
            }
        }

        val result = AccelerometerResult(
            x = x,
            y = y,
            z = z,
            magnitude = magnitude,
            linearAcceleration = linearAcceleration,
            filteredAcceleration = filteredAcceleration,
            impactDetected = impactDetected,
            timestamp = timestampMillis
        )
        lastResult = result
        return result
    }

    @Synchronized
    fun reset() {
        gravity[0] = 0f
        gravity[1] = 0f
        gravity[2] = 0f
        gravityInitialized = false
        accelerationHistory.clear()
        lastDetectionTime = 0L
        lastResult = AccelerometerResult()
    }
}
