package com.roadguardian.app.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class AccelerometerProcessorTest {

    @Test
    fun processor_stationaryGravity_producesNearZeroLinearAcceleration() {
        val processor = AccelerometerProcessor()

        var result = processor.process(0f, 0f, 9.81f, 1000L)
        assertEquals(9.81f, result.magnitude, 0.01f)
        assertEquals(0f, result.linearAcceleration, 0.01f)
        assertEquals(0f, result.filteredAcceleration, 0.01f)
        assertFalse(result.impactDetected)

        for (i in 1..10) {
            result = processor.process(0f, 0f, 9.81f, 1000L + i * 20L)
        }
        assertEquals(9.81f, result.magnitude, 0.01f)
        assertTrue(result.linearAcceleration < 0.01f)
        assertTrue(result.filteredAcceleration < 0.01f)
        assertFalse(result.impactDetected)
    }

    @Test
    fun processor_tiltedStationaryPhone_isolatesGravityCorrectly() {
        val processor = AccelerometerProcessor()
        val gx = 9.81f / sqrt(2.0f)
        val gy = 9.81f / sqrt(2.0f)
        val gz = 0f

        var result = processor.process(gx, gy, gz, 1000L)
        assertEquals(9.81f, result.magnitude, 0.01f)

        for (i in 1..15) {
            result = processor.process(gx, gy, gz, 1000L + i * 20L)
        }
        assertEquals(9.81f, result.magnitude, 0.01f)
        assertTrue(result.linearAcceleration < 0.01f)
        assertFalse(result.impactDetected)
    }

    @Test
    fun processor_roadImpactAboveThreshold_triggersImpactEvent() {
        val processor = AccelerometerProcessor(impactThreshold = 4.0f, cooldownMillis = 2000L, filterSize = 3)

        for (i in 1..5) {
            processor.process(0f, 0f, 9.81f, 1000L + i * 20L)
        }

        var detected = false
        for (i in 1..5) {
            val result = processor.process(0f, 0f, 9.81f + 10.0f, 1120L + i * 20L)
            if (result.impactDetected) {
                detected = true
            }
        }
        assertTrue(detected)
    }

    @Test
    fun processor_cooldownPeriod_suppressesRepeatedDetections() {
        val processor = AccelerometerProcessor(impactThreshold = 4.0f, cooldownMillis = 2000L, filterSize = 1)
        processor.process(0f, 0f, 9.81f, 500L)

        val first = processor.process(0f, 0f, 9.81f + 6.0f, 1000L)
        assertTrue(first.impactDetected)

        val second = processor.process(0f, 0f, 9.81f + 6.0f, 1500L)
        assertFalse(second.impactDetected)

        for (i in 1..5) {
            processor.process(0f, 0f, 9.81f, 1500L + i * 200L)
        }

        val third = processor.process(0f, 0f, 9.81f + 6.0f, 3100L)
        assertTrue(third.impactDetected)
    }

    @Test
    fun processor_invalidInput_isSafelyIgnored() {
        val processor = AccelerometerProcessor()
        val initial = processor.process(0f, 0f, 9.81f, 1000L)

        val nanResult = processor.process(Float.NaN, 0f, 9.81f, 1020L)
        assertEquals(initial.magnitude, nanResult.magnitude, 0.001f)

        val infResult = processor.process(Float.POSITIVE_INFINITY, 0f, 9.81f, 1040L)
        assertEquals(initial.magnitude, infResult.magnitude, 0.001f)
    }

    @Test
    fun processor_reset_clearsState() {
        val processor = AccelerometerProcessor()
        for (i in 1..5) {
            processor.process(0f, 0f, 9.81f + 10f, 1000L + i * 20L)
        }

        processor.reset()
        val fresh = processor.process(0f, 0f, 9.81f, 5000L)
        assertEquals(0f, fresh.linearAcceleration, 0.01f)
        assertFalse(fresh.impactDetected)
    }
}
