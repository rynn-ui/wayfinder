package com.roadguardian.app.ai.confirmation

import com.roadguardian.app.ai.tracking.TrackedDetection
import com.roadguardian.app.domain.model.BoundingBox
import com.roadguardian.app.domain.model.HazardSeverity
import com.roadguardian.app.domain.model.HazardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PotholeConfirmationManagerTest {

    private lateinit var manager: PotholeConfirmationManager
    private var confirmedCandidate: ConfirmedPotholeCandidate? = null

    @Before
    fun setUp() {
        confirmedCandidate = null
        manager = PotholeConfirmationManager(
            confidenceThreshold = 0.45f,
            minConsecutiveDetections = 3,
            maxFrameGap = 2,
            cooldownDurationMillis = 3000L,
            onPotholeConfirmed = { candidate ->
                confirmedCandidate = candidate
            }
        )
    }

    private fun createTrack(trackId: String, hits: Int, confidence: Float = 0.8f): TrackedDetection {
        return TrackedDetection(
            id = trackId,
            hazardType = HazardType.POTHOLE,
            currentBox = BoundingBox(0.3f, 0.3f, 0.4f, 0.4f),
            smoothedBox = BoundingBox(0.3f, 0.3f, 0.4f, 0.4f),
            confidence = confidence,
            firstSeenFrame = 1L,
            lastSeenFrame = hits.toLong(),
            consecutiveHitCount = hits,
            totalHits = hits,
            missedFrames = 0,
            firstSeenTimestamp = 1000L,
            lastSeenTimestamp = 1000L + (hits * 33L)
        )
    }

    @Test
    fun confirmationManager_singleHitDoesNotConfirm() {
        val track = createTrack("t1", 1)
        val res = manager.processFrame(listOf(track), frameNumber = 1L, timestamp = 1000L)

        assertEquals(PotholeConfirmationState.CANDIDATE, res.state)
        assertNull(confirmedCandidate)
    }

    @Test
    fun confirmationManager_twoHitsBecomesConfirming() {
        val track = createTrack("t1", 2)
        val res = manager.processFrame(listOf(track), frameNumber = 2L, timestamp = 1033L)

        assertEquals(PotholeConfirmationState.CONFIRMING, res.state)
        assertNull(confirmedCandidate)
    }

    @Test
    fun confirmationManager_threeHitsConfirmsAndFiresCallback() {
        val track = createTrack("t1", 3, confidence = 0.88f)
        val res = manager.processFrame(listOf(track), frameNumber = 3L, timestamp = 1066L)

        assertEquals(PotholeConfirmationState.CONFIRMED, res.state)
        assertNotNull(confirmedCandidate)
        assertEquals(0.88f, confirmedCandidate!!.confidence, 0.001f)
        assertTrue(confirmedCandidate!!.consecutiveCount >= 3)
    }

    @Test
    fun confirmationManager_sensorImpactElevatesConfirmation() {
        val track = createTrack("t1", 2, confidence = 0.85f)
        val res = manager.processFrame(
            trackedDetections = listOf(track),
            frameNumber = 2L,
            timestamp = 1033L,
            hasSensorImpact = true
        )

        assertEquals(PotholeConfirmationState.CONFIRMED, res.state)
        assertNotNull(confirmedCandidate)
        assertEquals(HazardSeverity.CRITICAL, confirmedCandidate!!.severity)
        assertTrue(confirmedCandidate!!.hasSensorImpact)
    }

    @Test
    fun confirmationManager_cooldownSuppressesRapidRepeats() {
        val track = createTrack("t1", 3)
        manager.processFrame(listOf(track), frameNumber = 3L, timestamp = 1066L)
        assertNotNull(confirmedCandidate)
        confirmedCandidate = null

        val resInCooldown = manager.processFrame(listOf(track), frameNumber = 4L, timestamp = 1500L)
        assertEquals(PotholeConfirmationState.COOLDOWN, resInCooldown.state)
        assertNull(confirmedCandidate)
    }
}


