package com.roadguardian.app.ai.tracking

import com.roadguardian.app.domain.model.BoundingBox
import com.roadguardian.app.domain.model.HazardType
import com.roadguardian.app.domain.model.RoadHazardDetection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SpatialTemporalTrackerTest {

    private lateinit var tracker: SpatialTemporalTracker

    @Before
    fun setUp() {
        tracker = SpatialTemporalTracker(
            iouMatchingThreshold = 0.15f,
            maxCentroidDistanceRatio = 0.35f,
            maxFrameGap = 2,
            smoothingAlpha = 0.6f
        )
    }

    @Test
    fun tracker_createsTrackOnFirstFrame() {
        val detection = RoadHazardDetection(
            id = "raw_1",
            hazardType = HazardType.POTHOLE,
            confidence = 0.80f,
            boundingBox = BoundingBox(0.4f, 0.4f, 0.2f, 0.2f),
            timestamp = 1000L
        )

        val tracked = tracker.update(listOf(detection), frameNumber = 1L, timestamp = 1000L)
        assertEquals(1, tracked.size)
        assertEquals(1, tracked[0].consecutiveHitCount)
        assertEquals(0.80f, tracked[0].confidence, 0.001f)
    }

    @Test
    fun tracker_matchesAcrossFramesAndSmoothsBox() {
        val det1 = RoadHazardDetection(
            id = "raw_1",
            hazardType = HazardType.POTHOLE,
            confidence = 0.70f,
            boundingBox = BoundingBox(0.40f, 0.40f, 0.20f, 0.20f),
            timestamp = 1000L
        )
        tracker.update(listOf(det1), frameNumber = 1L, timestamp = 1000L)

        val det2 = RoadHazardDetection(
            id = "raw_2",
            hazardType = HazardType.POTHOLE,
            confidence = 0.85f,
            boundingBox = BoundingBox(0.42f, 0.41f, 0.21f, 0.19f),
            timestamp = 1033L
        )
        val tracked = tracker.update(listOf(det2), frameNumber = 2L, timestamp = 1033L)

        assertEquals(1, tracked.size)
        assertEquals(2, tracked[0].consecutiveHitCount)
        assertEquals(0.79f, tracked[0].confidence, 0.001f)
        assertTrue(tracked[0].smoothedBox.x > 0.40f && tracked[0].smoothedBox.x < 0.42f)
    }

    @Test
    fun tracker_expiresTrackAfterMaxMissedFrames() {
        val det = RoadHazardDetection(
            id = "raw_1",
            hazardType = HazardType.POTHOLE,
            confidence = 0.80f,
            boundingBox = BoundingBox(0.4f, 0.4f, 0.2f, 0.2f),
            timestamp = 1000L
        )
        tracker.update(listOf(det), frameNumber = 1L, timestamp = 1000L)

        val t2 = tracker.update(emptyList(), frameNumber = 2L, timestamp = 1033L)
        assertEquals(1, t2.size)

        val t3 = tracker.update(emptyList(), frameNumber = 3L, timestamp = 1066L)
        assertEquals(1, t3.size)

        val t4 = tracker.update(emptyList(), frameNumber = 4L, timestamp = 1100L)
        assertEquals(0, t4.size)
    }
}

