package com.roadguardian.app.warning

import com.roadguardian.app.domain.model.HazardSeverity
import com.roadguardian.app.domain.model.HazardType
import com.roadguardian.app.domain.model.RoadHazard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HazardWarningManagerTest {

    private lateinit var manager: HazardWarningManager

    @Before
    fun setUp() {
        manager = HazardWarningManager(
            warningRadiusMeters = 50.0,
            forwardConeDegrees = 45.0,
            minSpeedForBearingMps = 1.5f,
            audioWarningManager = null
        )
    }

    private fun createHazard(id: String, lat: Double, lon: Double, severity: HazardSeverity = HazardSeverity.HIGH): RoadHazard {
        return RoadHazard(
            id = id,
            deviceLatitude = lat,
            deviceLongitude = lon,
            hazardType = HazardType.POTHOLE,
            confidence = 0.85f,
            severity = severity.value,
            timestamp = 1000L
        )
    }

    @Test
    fun warningManager_calculatesBearingAccurately() {
        val bearingNorth = manager.calculateBearing(26.0, 80.0, 26.001, 80.0)
        assertEquals(0.0, bearingNorth, 1.0)

        val bearingEast = manager.calculateBearing(26.0, 80.0, 26.0, 80.001)
        assertEquals(90.0, bearingEast, 1.0)

        val bearingSouth = manager.calculateBearing(26.0, 80.0, 25.999, 80.0)
        assertEquals(180.0, bearingSouth, 1.0)

        val bearingWest = manager.calculateBearing(26.0, 80.0, 26.0, 79.999)
        assertEquals(270.0, bearingWest, 1.0)
    }

    @Test
    fun warningManager_warnsWhenHazardIsDirectlyAheadWithinCone() {
        val userLat = 26.449900
        val userLon = 80.331900
        val userBearing = 0f
        val userSpeed = 10f

        val hazardAhead = createHazard("h1", 26.450200, 80.331900)

        val state = manager.update(
            userLatitude = userLat,
            userLongitude = userLon,
            userBearing = userBearing,
            userSpeedMps = userSpeed,
            knownHazards = listOf(hazardAhead)
        )

        assertTrue(state.isWarningActive)
        assertEquals("h1", state.hazard?.id)
        assertTrue(state.distanceMeters in 25..40)
        assertTrue(state.isAhead)
    }

    @Test
    fun warningManager_doesNotWarnWhenHazardIsBehindVehicle() {
        val userLat = 26.449900
        val userLon = 80.331900
        val userBearing = 0f
        val userSpeed = 10f

        val hazardBehind = createHazard("h_behind", 26.449600, 80.331900)

        val state = manager.update(
            userLatitude = userLat,
            userLongitude = userLon,
            userBearing = userBearing,
            userSpeedMps = userSpeed,
            knownHazards = listOf(hazardBehind)
        )

        assertFalse(state.isWarningActive)
    }

    @Test
    fun warningManager_doesNotWarnWhenBeyondRadius() {
        val userLat = 26.449900
        val userLon = 80.331900
        val userBearing = 0f
        val userSpeed = 10f

        val hazardFar = createHazard("h_far", 26.455000, 80.331900)

        val state = manager.update(
            userLatitude = userLat,
            userLongitude = userLon,
            userBearing = userBearing,
            userSpeedMps = userSpeed,
            knownHazards = listOf(hazardFar)
        )

        assertFalse(state.isWarningActive)
    }

    @Test
    fun warningManager_doesNotWarnWhenStationary() {
        val userLat = 26.449900
        val userLon = 80.331900
        val userBearing = 0f
        val userSpeed = 0f

        val hazardNearby = createHazard("h_near", 26.450100, 80.331900)

        val state = manager.update(
            userLatitude = userLat,
            userLongitude = userLon,
            userBearing = userBearing,
            userSpeedMps = userSpeed,
            knownHazards = listOf(hazardNearby)
        )

        assertFalse(state.isWarningActive)
    }

    @Test
    fun warningManager_warnsWhenApproachingTrafficJamAhead() {
        val userLat = 26.449900
        val userLon = 80.331900
        val userBearing = 0f
        val userSpeed = 8f

        val trafficJamAhead = com.roadguardian.app.traffic.model.WazeTrafficIncident(
            id = "jam_1",
            type = com.roadguardian.app.traffic.model.WazeTrafficType.JAM,
            severity = com.roadguardian.app.traffic.model.WazeJamSeverity.HEAVY,
            streetName = "Main Highway",
            reportDescription = "Heavy Jam",
            latitude = 26.452900,
            longitude = 80.331900
        )

        val incident = manager.updateTraffic(
            userLatitude = userLat,
            userLongitude = userLon,
            userBearing = userBearing,
            userSpeedMps = userSpeed,
            incidents = listOf(trafficJamAhead)
        )

        org.junit.Assert.assertNotNull(incident)
        org.junit.Assert.assertEquals("jam_1", incident?.id)
    }

    @Test
    fun warningManager_doesNotWarnWhenTrafficJamIsBehind() {
        val userLat = 26.449900
        val userLon = 80.331900
        val userBearing = 0f
        val userSpeed = 8f

        val trafficJamBehind = com.roadguardian.app.traffic.model.WazeTrafficIncident(
            id = "jam_behind",
            type = com.roadguardian.app.traffic.model.WazeTrafficType.JAM,
            severity = com.roadguardian.app.traffic.model.WazeJamSeverity.HEAVY,
            streetName = "Old Road",
            reportDescription = "Heavy Jam",
            latitude = 26.446900,
            longitude = 80.331900
        )

        val incident = manager.updateTraffic(
            userLatitude = userLat,
            userLongitude = userLon,
            userBearing = userBearing,
            userSpeedMps = userSpeed,
            incidents = listOf(trafficJamBehind)
        )

        org.junit.Assert.assertNull(incident)
    }
}
