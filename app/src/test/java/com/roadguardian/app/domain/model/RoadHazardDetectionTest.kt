package com.roadguardian.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoadHazardDetectionTest {

    @Test
    fun hazardType_resolvesFromClassIdCorrectly() {
        assertEquals(HazardType.POTHOLE, HazardType.fromClassId(0))
        assertEquals(HazardType.LONGITUDINAL_CRACK, HazardType.fromClassId(1))
        assertEquals(HazardType.TRANSVERSE_CRACK, HazardType.fromClassId(2))
        assertEquals(HazardType.POTHOLE, HazardType.fromClassId(3))
        assertNull(HazardType.fromClassId(4))
    }

    @Test
    fun hazardType_resolvesFromCodeAndLabelCorrectly() {
        assertEquals(HazardType.POTHOLE, HazardType.fromCode("D40"))
        assertEquals(HazardType.POTHOLE, HazardType.fromLabel("pothole"))
        assertNull(HazardType.fromCode("UNKNOWN"))
        assertNull(HazardType.fromLabel("unknown"))
    }

    @Test
    fun boundingBox_computesAreaCorrectly() {
        val bbox = BoundingBox(
            x = 0.1f,
            y = 0.2f,
            width = 0.4f,
            height = 0.5f
        )
        assertEquals(0.2f, bbox.area, 0.0001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun boundingBox_rejectsNegativeWidth() {
        BoundingBox(x = 0.1f, y = 0.1f, width = -0.1f, height = 0.2f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun roadHazardDetection_rejectsBlankId() {
        RoadHazardDetection(
            id = "",
            hazardType = HazardType.POTHOLE,
            confidence = 0.85f,
            timestamp = 1000L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun roadHazardDetection_rejectsOutOfRangeConfidence() {
        RoadHazardDetection(
            id = "det_1",
            hazardType = HazardType.POTHOLE,
            confidence = 1.2f,
            timestamp = 1000L
        )
    }

    @Test
    fun roadHazardDetection_exposesLocationPropertiesConveniently() {
        val detection = RoadHazardDetection(
            id = "det_1",
            hazardType = HazardType.POTHOLE,
            confidence = 0.9f,
            timestamp = 123456789L,
            boundingBox = BoundingBox(0.2f, 0.3f, 0.4f, 0.5f),
            location = LocationData(
                latitude = 26.8451,
                longitude = 80.9467,
                accuracy = 5.0f,
                speed = 12.5f,
                bearing = 180.0f,
                altitude = 110.0,
                geohash = "ttu0wk3"
            ),
            accelerometerData = AccelerometerData(
                impactMagnitude = 4.2f,
                isImpactConfirmed = true
            ),
            gyroscopeData = GyroscopeData(
                pitchRate = 0.6f
            )
        )

        assertEquals("det_1", detection.id)
        assertEquals(HazardType.POTHOLE, detection.hazardType)
        assertEquals(0.9f, detection.confidence, 0.0001f)
        assertEquals(123456789L, detection.timestamp)
        assertNotNull(detection.boundingBox)
        assertEquals(26.8451, detection.latitude!!, 0.0001)
        assertEquals(80.9467, detection.longitude!!, 0.0001)
        assertEquals(5.0f, detection.locationAccuracy!!, 0.0001f)
        assertEquals(12.5f, detection.vehicleSpeed!!, 0.0001f)
        assertEquals(4.2f, detection.accelerometerData?.impactMagnitude ?: 0f, 0.0001f)
        assertTrue(detection.accelerometerData?.isImpactConfirmed == true)
        assertEquals(0.6f, detection.gyroscopeData?.pitchRate ?: 0f, 0.0001f)
    }
}

