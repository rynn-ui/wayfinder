package com.roadguardian.app.data.remote

import com.roadguardian.app.domain.model.HazardType
import com.roadguardian.app.domain.model.PotholeStatus
import com.roadguardian.app.domain.model.RoadHazard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HazardFirestoreMapperTest {

    @Test
    fun toFirestoreMap_serializesAllExpectedFields() {
        val hazard = RoadHazard(
            id = "test-doc-123",
            deviceLatitude = 26.4499,
            deviceLongitude = 80.3319,
            hazardType = HazardType.POTHOLE,
            confidence = 0.85f,
            severity = "critical",
            timestamp = 1755930000000L,
            source = "YOLO",
            confirmationCount = 3,
            lastSeenAt = 1755930500000L,
            gpsAccuracy = 4.2f,
            status = PotholeStatus.ACTIVE
        )

        val map = HazardFirestoreMapper.toFirestoreMap(hazard)

        assertEquals(26.4499, map[HazardFirestoreMapper.FIELD_DEVICE_LATITUDE])
        assertEquals(80.3319, map[HazardFirestoreMapper.FIELD_DEVICE_LONGITUDE])
        assertEquals("POTHOLE", map[HazardFirestoreMapper.FIELD_HAZARD_TYPE])
        assertEquals(0.85, (map[HazardFirestoreMapper.FIELD_CONFIDENCE] as Double), 0.001)
        assertEquals("critical", map[HazardFirestoreMapper.FIELD_SEVERITY])
        assertEquals(1755930000000L, map[HazardFirestoreMapper.FIELD_TIMESTAMP])
        assertEquals("YOLO", map[HazardFirestoreMapper.FIELD_SOURCE])
        assertEquals(3, map[HazardFirestoreMapper.FIELD_CONFIRMATION_COUNT])
        assertEquals(1755930500000L, map[HazardFirestoreMapper.FIELD_LAST_SEEN_AT])
        assertEquals(4.2, (map[HazardFirestoreMapper.FIELD_GPS_ACCURACY] as Double), 0.01)
        assertEquals("ACTIVE", map[HazardFirestoreMapper.FIELD_STATUS])
    }

    @Test
    fun fromFirestoreMap_deserializesValidDocument() {
        val map = mapOf<String, Any?>(
            "deviceLatitude" to 26.4499,
            "deviceLongitude" to 80.3319,
            "hazardType" to "POTHOLE",
            "confidence" to 0.85,
            "severity" to "critical",
            "timestamp" to 1755930000000L,
            "source" to "YOLO",
            "confirmationCount" to 1L,
            "lastSeenAt" to 1755930000000L,
            "gpsAccuracy" to 5.0,
            "status" to "ACTIVE"
        )

        val hazard = HazardFirestoreMapper.fromFirestoreMap("hazard-xyz", map)

        assertNotNull(hazard)
        assertEquals("hazard-xyz", hazard!!.id)
        assertEquals(26.4499, hazard.deviceLatitude, 0.0001)
        assertEquals(80.3319, hazard.deviceLongitude, 0.0001)
        assertEquals(HazardType.POTHOLE, hazard.hazardType)
        assertEquals(0.85f, hazard.confidence, 0.001f)
        assertEquals("critical", hazard.severity)
        assertEquals(1755930000000L, hazard.timestamp)
        assertEquals("YOLO", hazard.source)
        assertEquals(1, hazard.confirmationCount)
        assertEquals(1755930000000L, hazard.lastSeenAt)
        assertEquals(5.0f, hazard.gpsAccuracy!!, 0.01f)
        assertEquals(PotholeStatus.ACTIVE, hazard.status)
    }

    @Test
    fun fromFirestoreMap_handlesAllHazardTypes() {
        val types = listOf(
            HazardType.POTHOLE to "POTHOLE",
            HazardType.LONGITUDINAL_CRACK to "LONGITUDINAL_CRACK",
            HazardType.TRANSVERSE_CRACK to "TRANSVERSE_CRACK"
        )

        for ((expectedType, typeString) in types) {
            val map = mapOf<String, Any?>(
                "deviceLatitude" to 26.4499,
                "deviceLongitude" to 80.3319,
                "hazardType" to typeString,
                "confidence" to 0.70,
                "timestamp" to 1000L
            )
            val hazard = HazardFirestoreMapper.fromFirestoreMap("id-$typeString", map)
            assertNotNull(hazard)
            assertEquals(expectedType, hazard!!.hazardType)
        }
    }

    @Test
    fun fromFirestoreMap_rejectsInvalidCoordinates() {
        val invalidZero = mapOf<String, Any?>(
            "deviceLatitude" to 0.0,
            "deviceLongitude" to 0.0,
            "hazardType" to "POTHOLE",
            "confidence" to 0.8,
            "timestamp" to 1000L
        )
        assertNull(HazardFirestoreMapper.fromFirestoreMap("bad-zero", invalidZero))

        val invalidLat = mapOf<String, Any?>(
            "deviceLatitude" to 95.0,
            "deviceLongitude" to 80.0,
            "hazardType" to "POTHOLE",
            "confidence" to 0.8,
            "timestamp" to 1000L
        )
        assertNull(HazardFirestoreMapper.fromFirestoreMap("bad-lat", invalidLat))

        val invalidLon = mapOf<String, Any?>(
            "deviceLatitude" to 26.0,
            "deviceLongitude" to 185.0,
            "hazardType" to "POTHOLE",
            "confidence" to 0.8,
            "timestamp" to 1000L
        )
        assertNull(HazardFirestoreMapper.fromFirestoreMap("bad-lon", invalidLon))
    }

    @Test
    fun fromFirestoreMap_rejectsBlankId() {
        val map = mapOf<String, Any?>(
            "deviceLatitude" to 26.4499,
            "deviceLongitude" to 80.3319,
            "hazardType" to "POTHOLE",
            "confidence" to 0.85,
            "timestamp" to 1000L
        )
        assertNull(HazardFirestoreMapper.fromFirestoreMap("", map))
        assertNull(HazardFirestoreMapper.fromFirestoreMap("   ", map))
    }

    @Test
    fun fromFirestoreMap_coercesConfidenceAndSuppliesDefaults() {
        val map = mapOf<String, Any?>(
            "deviceLatitude" to 26.4499,
            "deviceLongitude" to 80.3319,
            "confidence" to 1.5,
            "timestamp" to 1000L
        )
        val hazard = HazardFirestoreMapper.fromFirestoreMap("h1", map)
        assertNotNull(hazard)
        assertEquals(1.0f, hazard!!.confidence, 0.0001f)
        assertEquals(HazardType.POTHOLE, hazard.hazardType)
        assertEquals("critical", hazard.severity)
        assertEquals("YOLO", hazard.source)
        assertEquals(1, hazard.confirmationCount)
    }
}

