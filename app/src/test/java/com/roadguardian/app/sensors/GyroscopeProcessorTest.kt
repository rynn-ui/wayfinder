package com.roadguardian.app.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GyroscopeProcessorTest {

    @Test
    fun processor_zeroRotation_producesZeroAngularVelocity() {
        val processor = GyroscopeProcessor()
        val result = processor.process(0f, 0f, 0f, 1000L)

        assertEquals(0f, result.angularVelocity, 0.001f)
        assertEquals(0f, result.filteredRotation, 0.001f)
        assertFalse(result.rotationDetected)
    }

    @Test
    fun processor_gentleRotationBelowThreshold_doesNotTriggerEvent() {
        val processor = GyroscopeProcessor(rotationThreshold = 2.0f, filterSize = 5)
        var result = processor.process(0.5f, 0.5f, 0.5f, 1000L)

        for (i in 1..10) {
            result = processor.process(0.5f, 0.5f, 0.5f, 1000L + i * 20L)
        }

        assertTrue(result.filteredRotation < 2.0f)
        assertFalse(result.rotationDetected)
    }

    @Test
    fun processor_suddenRotationAboveThreshold_triggersRotationEvent() {
        val processor = GyroscopeProcessor(rotationThreshold = 2.0f, cooldownMillis = 2000L, filterSize = 3)

        processor.process(0f, 0f, 0f, 1000L)
        processor.process(0f, 0f, 0f, 1020L)

        var result = processor.process(2.5f, 0f, 0f, 1040L)
        result = processor.process(2.5f, 0f, 0f, 1060L)
        result = processor.process(2.5f, 0f, 0f, 1080L)

        assertTrue(result.filteredRotation >= 2.0f)
        assertTrue(result.rotationDetected)
    }

    @Test
    fun processor_cooldownPeriod_suppressesRepeatedEvents() {
        val processor = GyroscopeProcessor(rotationThreshold = 2.0f, cooldownMillis = 2000L, filterSize = 1)

        var result = processor.process(3.0f, 0f, 0f, 1000L)
        assertTrue(result.rotationDetected)

        result = processor.process(3.0f, 0f, 0f, 1500L)
        assertFalse(result.rotationDetected)

        result = processor.process(3.0f, 0f, 0f, 3100L)
        assertTrue(result.rotationDetected)
    }

    @Test
    fun processor_invalidInput_isSafelyIgnored() {
        val processor = GyroscopeProcessor()
        val initial = processor.process(1.0f, 0f, 0f, 1000L)

        val nanResult = processor.process(Float.NaN, 0f, 0f, 1020L)
        assertEquals(initial.angularVelocity, nanResult.angularVelocity, 0.001f)

        val infResult = processor.process(Float.NEGATIVE_INFINITY, 0f, 0f, 1040L)
        assertEquals(initial.angularVelocity, infResult.angularVelocity, 0.001f)
    }

    @Test
    fun processor_reset_clearsHistory() {
        val processor = GyroscopeProcessor(rotationThreshold = 2.0f, filterSize = 3)
        processor.process(5.0f, 0f, 0f, 1000L)
        processor.process(5.0f, 0f, 0f, 1020L)

        processor.reset()
        val fresh = processor.process(0f, 0f, 0f, 2000L)
        assertEquals(0f, fresh.filteredRotation, 0.001f)
        assertFalse(fresh.rotationDetected)
    }
}
