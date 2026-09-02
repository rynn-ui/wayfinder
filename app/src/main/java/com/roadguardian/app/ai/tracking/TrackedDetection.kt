package com.roadguardian.app.ai.tracking

import com.roadguardian.app.domain.model.BoundingBox
import com.roadguardian.app.domain.model.HazardType
import com.roadguardian.app.domain.model.RoadHazardDetection

data class TrackedDetection(
    val id: String,
    val hazardType: HazardType = HazardType.POTHOLE,
    val currentBox: BoundingBox,
    val smoothedBox: BoundingBox,
    val confidence: Float,
    val firstSeenFrame: Long,
    val lastSeenFrame: Long,
    val consecutiveHitCount: Int,
    val totalHits: Int,
    val missedFrames: Int = 0,
    val firstSeenTimestamp: Long,
    val lastSeenTimestamp: Long
) {
    val centerX: Float
        get() = currentBox.x + currentBox.width / 2.0f

    val centerY: Float
        get() = currentBox.y + currentBox.height / 2.0f

    val smoothedCenterX: Float
        get() = smoothedBox.x + smoothedBox.width / 2.0f

    val smoothedCenterY: Float
        get() = smoothedBox.y + smoothedBox.height / 2.0f

    val boxArea: Float
        get() = currentBox.area

    fun toRoadHazardDetection(): RoadHazardDetection {
        return RoadHazardDetection(
            id = id,
            hazardType = hazardType,
            confidence = confidence,
            boundingBox = smoothedBox,
            timestamp = lastSeenTimestamp
        )
    }
}

