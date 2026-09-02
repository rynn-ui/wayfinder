package com.roadguardian.app.data.repository

import com.roadguardian.app.domain.model.HazardSeverity
import com.roadguardian.app.domain.model.HazardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoadHazardRepositoryTest {

    private lateinit var repository: RoadHazardRepository
    private lateinit var firestoreRepo: FirestoreRoadHazardRepository

    @Before
    fun setUp() {
        repository = InMemoryRoadHazardRepository(deduplicationRadiusMeters = 15.0)
        firestoreRepo = FirestoreRoadHazardRepository(
            firestore = null,
            authManager = null,
            deduplicationRadiusMeters = 15.0
        )
    }

    @Test
    fun recordDetection_createsNewHazardOnFirstDetection() {
        val hazard = repository.recordDetection(
            hazardType = HazardType.POTHOLE,
            confidence = 0.82f,
            latitude = 26.4499,
            longitude = 80.3319,
            timestamp = 1000L
        )

        assertNotNull(hazard)
        assertEquals(1, repository.allHazards.size)
        assertEquals(HazardType.POTHOLE, hazard!!.hazardType)
        assertEquals(0.82f, hazard.confidence, 0.0001f)
        assertEquals(1, hazard.confirmationCount)
        assertEquals(26.4499, hazard.deviceLatitude, 0.0001)
        assertEquals(80.3319, hazard.deviceLongitude, 0.0001)
    }

    @Test
    fun recordPothole_updatesAndSmoothsCoordinatesWhenWithinRadius() {
        val first = repository.recordPothole(
            latitude = 26.44990,
            longitude = 80.33190,
            confidence = 0.70f,
            severity = HazardSeverity.MEDIUM,
            gpsAccuracy = 3.5f,
            timestamp = 1000L
        )
        assertNotNull(first)

        val second = repository.recordPothole(
            latitude = 26.44993,
            longitude = 80.33192,
            confidence = 0.85f,
            severity = HazardSeverity.HIGH,
            gpsAccuracy = 2.0f,
            timestamp = 2000L
        )

        assertNotNull(second)
        assertEquals(1, repository.allHazards.size)
        assertEquals(first!!.id, second!!.id)
        assertEquals(2, second.confirmationCount)
        assertEquals(2000L, second.lastSeenAt)
        assertEquals(0.85f, second.confidence, 0.0001f)
        assertEquals("high", second.severity)
    }

    @Test
    fun recordDetection_createsSeparateHazardWhenBeyondRadius() {
        val first = repository.recordDetection(
            hazardType = HazardType.POTHOLE,
            confidence = 0.70f,
            latitude = 26.4499,
            longitude = 80.3319,
            timestamp = 1000L
        )
        assertNotNull(first)

        val second = repository.recordDetection(
            hazardType = HazardType.POTHOLE,
            confidence = 0.80f,
            latitude = 26.4550,
            longitude = 80.3390,
            timestamp = 2000L
        )

        assertNotNull(second)
        assertEquals(2, repository.allHazards.size)
        assertTrue(first!!.id != second!!.id)
        assertEquals(1, first.confirmationCount)
        assertEquals(1, second.confirmationCount)
    }

    @Test
    fun recordDetection_createsSeparateHazardForDifferentTypeAtSameLocation() {
        val pothole = repository.recordDetection(
            hazardType = HazardType.POTHOLE,
            confidence = 0.75f,
            latitude = 26.4499,
            longitude = 80.3319,
            timestamp = 1000L
        )

        val crack = repository.recordDetection(
            hazardType = HazardType.LONGITUDINAL_CRACK,
            confidence = 0.70f,
            latitude = 26.4499,
            longitude = 80.3319,
            timestamp = 1500L
        )

        assertNotNull(pothole)
        assertNotNull(crack)
        assertEquals(2, repository.allHazards.size)
        assertTrue(pothole!!.id != crack!!.id)
    }

    @Test
    fun recordDetection_rejectsInvalidCoordinates() {
        val invalidZero = repository.recordDetection(
            hazardType = HazardType.POTHOLE,
            confidence = 0.8f,
            latitude = 0.0,
            longitude = 0.0
        )
        assertNull(invalidZero)

        val invalidLat = repository.recordDetection(
            hazardType = HazardType.POTHOLE,
            confidence = 0.8f,
            latitude = 95.0,
            longitude = 80.0
        )
        assertNull(invalidLat)

        assertEquals(0, repository.allHazards.size)
    }

    @Test
    fun getHazardsNearby_returnsCorrectSubset() {
        repository.recordDetection(HazardType.POTHOLE, 0.8f, 26.4499, 80.3319, 1000L)
        repository.recordDetection(HazardType.POTHOLE, 0.8f, 26.4600, 80.3400, 2000L)
        repository.recordDetection(HazardType.POTHOLE, 0.8f, 26.8500, 80.9500, 3000L)

        val nearby500m = repository.getHazardsNearby(26.4499, 80.3319, radiusMeters = 500.0)
        assertEquals(1, nearby500m.size)

        val nearby5km = repository.getHazardsNearby(26.4499, 80.3319, radiusMeters = 5000.0)
        assertEquals(2, nearby5km.size)

        val allNearby = repository.getHazardsNearby(26.4499, 80.3319, radiusMeters = 100000.0)
        assertEquals(3, allNearby.size)
    }

    @Test
    fun getHazardsInBounds_filtersBothLatitudeAndLongitude() {
        repository.recordDetection(HazardType.POTHOLE, 0.8f, 26.4499, 80.3319, 1000L)
        repository.recordDetection(HazardType.POTHOLE, 0.8f, 26.8467, 80.9462, 2000L)
        repository.recordDetection(HazardType.POTHOLE, 0.8f, 26.4499, 82.0000, 3000L)

        val kanpurBounds = repository.getHazardsInBounds(
            minLatitude = 26.40,
            maxLatitude = 26.50,
            minLongitude = 80.30,
            maxLongitude = 80.40
        )
        assertEquals(1, kanpurBounds.size)
        assertEquals(26.4499, kanpurBounds[0].deviceLatitude, 0.001)
        assertEquals(80.3319, kanpurBounds[0].deviceLongitude, 0.001)
    }

    @Test
    fun firestoreRoadHazardRepository_operatesSafelyWithoutLiveFirebase() {
        val hazard = firestoreRepo.recordPothole(
            latitude = 26.4499,
            longitude = 80.3319,
            confidence = 0.88f,
            severity = HazardSeverity.CRITICAL
        )
        assertNotNull(hazard)
        assertEquals(1, firestoreRepo.allHazards.size)
        assertEquals(HazardType.POTHOLE, firestoreRepo.allHazards[0].hazardType)
        assertEquals("critical", firestoreRepo.allHazards[0].severity)

        val repeat = firestoreRepo.recordPothole(
            latitude = 26.44992,
            longitude = 80.33191,
            confidence = 0.92f,
            severity = HazardSeverity.CRITICAL
        )
        assertNotNull(repeat)
        assertEquals(1, firestoreRepo.allHazards.size)
        assertEquals(2, repeat!!.confirmationCount)
        assertEquals(0.92f, repeat.confidence, 0.001f)
    }

    @Test
    fun firestoreRoadHazardRepository_handlesRegionSyncLifecycleSafely() {
        org.junit.Assert.assertFalse(firestoreRepo.isRegionSyncActive)
        assertNull(firestoreRepo.currentSyncCenter)

        firestoreRepo.startRegionSync(26.4499, 80.3319)

        firestoreRepo.stopRegionSync()
        org.junit.Assert.assertFalse(firestoreRepo.isRegionSyncActive)
        assertNull(firestoreRepo.currentSyncCenter)
    }

    @Test
    fun firestoreRoadHazardRepository_configurableThreshold() {
        val customRepo = FirestoreRoadHazardRepository(
            firestore = null,
            authManager = null,
            deduplicationRadiusMeters = 15.0,
            regionSyncThresholdMeters = 1200.0
        )
        assertEquals(1200.0, customRepo.regionSyncThresholdMeters, 0.001)
        assertEquals(FirestoreRoadHazardRepository.DEFAULT_REGION_SYNC_THRESHOLD_METERS, firestoreRepo.regionSyncThresholdMeters, 0.001)
    }

    @Test
    fun inMemoryRepository_addHazardsAndClear_managesStateFlowProperly() {
        val h1 = repository.recordDetection(HazardType.POTHOLE, 0.8f, 26.4499, 80.3319)!!
        assertEquals(1, repository.hazardsState.value.size)

        repository.clear()
        assertEquals(0, repository.hazardsState.value.size)
        assertEquals(0, repository.allHazards.size)

        repository.addHazards(listOf(h1))
        assertEquals(1, repository.hazardsState.value.size)
        assertEquals(h1.id, repository.hazardsState.value[0].id)
    }
}

