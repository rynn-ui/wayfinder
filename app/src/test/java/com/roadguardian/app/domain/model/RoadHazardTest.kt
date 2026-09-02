package com.roadguardian.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoadHazardTest {

    @Test
    fun roadHazard_createsValidInstance() {
        val hazard = RoadHazard(
            id = "hazard-123",
            deviceLatitude = 26.4499,
            deviceLongitude = 80.3319,
            hazardType = HazardType.POTHOLE,
            confidence = 0.85f,
            timestamp = 1000L,
            gpsAccuracy = 4.5f,
            status = PotholeStatus.ACTIVE
        )

        assertEquals("hazard-123", hazard.id)
        assertEquals("hazard-123", hazard.potholeId)
        assertEquals(26.4499, hazard.deviceLatitude, 0.0001)
        assertEquals(80.3319, hazard.deviceLongitude, 0.0001)
        assertEquals(26.4499, hazard.latitude, 0.0001)
        assertEquals(80.3319, hazard.longitude, 0.0001)
        assertEquals(HazardType.POTHOLE, hazard.hazardType)
        assertEquals(0.85f, hazard.confidence, 0.0001f)
        assertEquals("critical", hazard.severity)
        assertEquals(HazardSeverity.CRITICAL, hazard.hazardSeverity)
        assertEquals("Pothole", hazard.displayTitle)
        assertTrue(hazard.isCritical)
        assertEquals(1, hazard.confirmationCount)
        assertEquals(1, hazard.verificationCount)
        assertEquals(4.5f, hazard.gpsAccuracy!!, 0.01f)
        assertEquals(PotholeStatus.ACTIVE, hazard.status)
    }

    @Test
    fun roadHazard_derivesSeverityCorrectly() {
        assertEquals("critical", RoadHazard.deriveSeverity(0.95f))
        assertEquals("critical", RoadHazard.deriveSeverity(0.80f))
        assertEquals("high", RoadHazard.deriveSeverity(0.75f))
        assertEquals("high", RoadHazard.deriveSeverity(0.65f))
        assertEquals("medium", RoadHazard.deriveSeverity(0.55f))
        assertEquals("medium", RoadHazard.deriveSeverity(0.45f))
        assertEquals("low", RoadHazard.deriveSeverity(0.35f))
        assertEquals("low", RoadHazard.deriveSeverity(0.10f))
    }

    @Test
    fun roadHazard_displayTitlesMatchEnum() {
        val pothole = RoadHazard("1", 26.0, 80.0, HazardType.POTHOLE, 0.5f, timestamp = 0L)
        assertEquals("Pothole", pothole.displayTitle)

        val longCrack = RoadHazard("2", 26.0, 80.0, HazardType.LONGITUDINAL_CRACK, 0.5f, timestamp = 0L)
        assertEquals("Longitudinal Crack", longCrack.displayTitle)

        val transCrack = RoadHazard("3", 26.0, 80.0, HazardType.TRANSVERSE_CRACK, 0.5f, timestamp = 0L)
        assertEquals("Transverse Crack", transCrack.displayTitle)
    }

    @Test(expected = IllegalArgumentException::class)
    fun roadHazard_rejectsBlankId() {
        RoadHazard("", 26.0, 80.0, HazardType.POTHOLE, 0.5f, timestamp = 0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun roadHazard_rejectsInvalidConfidence() {
        RoadHazard("1", 26.0, 80.0, HazardType.POTHOLE, 1.5f, timestamp = 0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun roadHazard_rejectsInvalidConfirmationCount() {
        RoadHazard("1", 26.0, 80.0, HazardType.POTHOLE, 0.5f, confirmationCount = 0, timestamp = 0L)
    }
}

