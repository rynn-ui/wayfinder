package com.roadguardian.app.ai.tracking

import com.roadguardian.app.domain.model.BoundingBox
import com.roadguardian.app.domain.model.HazardType
import com.roadguardian.app.domain.model.RoadHazardDetection
import java.util.UUID
import kotlin.math.hypot
import kotlin.math.sqrt

class SpatialTemporalTracker(
    val iouMatchingThreshold: Float = 0.10f,
    val maxCentroidDistanceRatio: Float = 0.35f,
    val maxFrameGap: Int = 2,
    val smoothingAlpha: Float = 0.65f
) {

    private val activeTracks = mutableListOf<TrackedDetection>()

    fun reset() {
        activeTracks.clear()
    }

    fun clear() {
        reset()
    }

    fun getActiveTracks(): List<TrackedDetection> = activeTracks.toList()

    @Synchronized
    fun update(
        detections: List<RoadHazardDetection>,
        frameNumber: Long,
        timestamp: Long = System.currentTimeMillis(),
        imageWidth: Float = 640.0f,
        imageHeight: Float = 640.0f
    ): List<TrackedDetection> {
        val unmatchedDetections = detections.toMutableList()
        val updatedTracks = mutableListOf<TrackedDetection>()
        val matchedTrackIds = mutableSetOf<String>()
        val frameDiagonal = hypot(imageWidth, imageHeight)

        for (track in activeTracks) {
            var bestMatch: RoadHazardDetection? = null
            var bestMatchScore = -1.0f

            for (detection in unmatchedDetections) {
                val box = detection.boundingBox ?: continue
                val iou = calculateIoU(track.smoothedBox, box)
                val dist = calculateDistance(track.smoothedBox, box)
                val normalizedDist = if (frameDiagonal > 0.0f) dist / frameDiagonal else dist

                val isIouMatch = iou >= iouMatchingThreshold
                val isCentroidMatch = normalizedDist <= maxCentroidDistanceRatio

                if (isIouMatch || isCentroidMatch) {
                    val score = (iou * 0.5f) + (maxOf(0.0f, 1.0f - (normalizedDist / maxCentroidDistanceRatio)) * 0.5f)
                    if (score > bestMatchScore) {
                        bestMatchScore = score
                        bestMatch = detection
                    }
                }
            }

            if (bestMatch != null) {
                val currentBox = bestMatch.boundingBox!!
                unmatchedDetections.remove(bestMatch)
                matchedTrackIds.add(track.id)

                val smoothed = smoothBoundingBox(track.smoothedBox, currentBox, smoothingAlpha)
                val updated = track.copy(
                    hazardType = bestMatch.hazardType,
                    currentBox = currentBox,
                    smoothedBox = smoothed,
                    confidence = track.confidence * (1.0f - smoothingAlpha) + bestMatch.confidence * smoothingAlpha,
                    lastSeenFrame = frameNumber,
                    consecutiveHitCount = track.consecutiveHitCount + 1,
                    totalHits = track.totalHits + 1,
                    missedFrames = 0,
                    lastSeenTimestamp = timestamp
                )
                updatedTracks.add(updated)
            } else {
                val missed = track.missedFrames + 1
                if (missed <= maxFrameGap) {
                    val decayed = track.copy(
                        missedFrames = missed,
                        consecutiveHitCount = maxOf(1, track.consecutiveHitCount - 1),
                        confidence = track.confidence * 0.90f
                    )
                    updatedTracks.add(decayed)
                }
            }
        }

        for (unmatched in unmatchedDetections) {
            val box = unmatched.boundingBox ?: continue
            val newTrack = TrackedDetection(
                id = UUID.randomUUID().toString(),
                hazardType = unmatched.hazardType,
                currentBox = box,
                smoothedBox = box,
                confidence = unmatched.confidence,
                firstSeenFrame = frameNumber,
                lastSeenFrame = frameNumber,
                consecutiveHitCount = 1,
                totalHits = 1,
                missedFrames = 0,
                firstSeenTimestamp = timestamp,
                lastSeenTimestamp = timestamp
            )
            updatedTracks.add(newTrack)
        }

        activeTracks.clear()
        activeTracks.addAll(updatedTracks)

        return activeTracks.toList()
    }

    private fun smoothBoundingBox(prev: BoundingBox, current: BoundingBox, alpha: Float): BoundingBox {
        return BoundingBox(
            x = prev.x * (1.0f - alpha) + current.x * alpha,
            y = prev.y * (1.0f - alpha) + current.y * alpha,
            width = prev.width * (1.0f - alpha) + current.width * alpha,
            height = prev.height * (1.0f - alpha) + current.height * alpha
        )
    }

    private fun calculateDistance(b1: BoundingBox, b2: BoundingBox): Float {
        val cx1 = b1.x + b1.width / 2.0f
        val cy1 = b1.y + b1.height / 2.0f
        val cx2 = b2.x + b2.width / 2.0f
        val cy2 = b2.y + b2.height / 2.0f
        val dx = cx1 - cx2
        val dy = cy1 - cy2
        return sqrt(dx * dx + dy * dy)
    }

    private fun calculateIoU(b1: BoundingBox, b2: BoundingBox): Float {
        val x1 = maxOf(b1.x, b2.x)
        val y1 = maxOf(b1.y, b2.y)
        val x2 = minOf(b1.x + b1.width, b2.x + b2.width)
        val y2 = minOf(b1.y + b1.height, b2.y + b2.height)

        val intersection = maxOf(0.0f, x2 - x1) * maxOf(0.0f, y2 - y1)
        val union = b1.area + b2.area - intersection

        return if (union <= 0.0f) 0.0f else intersection / union
    }
}
