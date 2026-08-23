package com.roadguardian.app.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationStateTest {

    @Test
    fun locationState_defaultsToUnavailable() {
        val state = LocationState()
        assertFalse(state.isAvailable)
        assertEquals(0.0, state.latitude, 0.0001)
        assertEquals(0.0, state.longitude, 0.0001)
        assertEquals(0f, state.speedKmh, 0.0001f)
        assertEquals("-- km/h", state.formattedSpeed)
        assertEquals("--", state.speedDisplayValue)
        assertEquals("km/h", state.speedDisplayUnit)
    }

    @Test
    fun locationState_convertsSpeedMpsToKmh() {
        val state = LocationState(
            latitude = 26.4499,
            longitude = 80.3319,
            speedMps = 10.0f,
            isAvailable = true
        )

        assertTrue(state.isAvailable)
        assertEquals(36.0f, state.speedKmh, 0.001f)
        assertEquals("36 km/h", state.formattedSpeed)
        assertEquals("36", state.speedDisplayValue)
        assertEquals("km/h", state.speedDisplayUnit)
    }

    @Test
    fun locationState_handlesStationarySpeed() {
        val state = LocationState(
            latitude = 26.4499,
            longitude = 80.3319,
            speedMps = 0.0f,
            isAvailable = true
        )

        assertEquals(0.0f, state.speedKmh, 0.0001f)
        assertEquals("0 km/h", state.formattedSpeed)
        assertEquals("0", state.speedDisplayValue)
    }

    @Test
    fun locationState_handlesFractionalSpeedCorrectly() {
        val state = LocationState(
            latitude = 26.4499,
            longitude = 80.3319,
            speedMps = 6.67f,
            isAvailable = true
        )

        assertEquals(24.012f, state.speedKmh, 0.01f)
        assertEquals("24 km/h", state.formattedSpeed)
        assertEquals("24", state.speedDisplayValue)
    }

    @Test
    fun locationState_unavailableStateOutputsFallback() {
        val state = LocationState(
            latitude = 26.4499,
            longitude = 80.3319,
            speedMps = 15.0f,
            isAvailable = false
        )

        assertEquals("-- km/h", state.formattedSpeed)
        assertEquals("--", state.speedDisplayValue)
    }
}
