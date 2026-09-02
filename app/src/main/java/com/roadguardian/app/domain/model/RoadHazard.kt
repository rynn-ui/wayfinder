package com.roadguardian.app.domain.model

data class RoadHazard(
    val id: String,
    val deviceLatitude: Double,
    val deviceLongitude: Double,
    val hazardType: HazardType = HazardType.POTHOLE,
    val confidence: Float,
    val severity: String = deriveSeverity(confidence),
    val timestamp: Long,
    val source: String = "YOLO",
    val confirmationCount: Int = 1,
    val lastSeenAt: Long = timestamp,
    val gpsAccuracy: Float? = null,
    val status: PotholeStatus = PotholeStatus.ACTIVE,
    val firstDetectedAt: Long = timestamp,
    val imageUrl: String? = null
) {
    init {
        require(id.isNotBlank())
        require(confidence in 0.0f..1.0f)
        require(confirmationCount >= 1)
        require(timestamp >= 0L)
        require(lastSeenAt >= 0L)
        require(firstDetectedAt >= 0L)
    }

    val potholeId: String
        get() = id

    val verificationCount: Int
        get() = confirmationCount

    val latitude: Double
        get() = deviceLatitude

    val longitude: Double
        get() = deviceLongitude

    val hazardSeverity: HazardSeverity
        get() = HazardSeverity.fromValue(severity)

    val displayTitle: String
        get() = when (hazardType) {
            HazardType.POTHOLE -> "Pothole"
            HazardType.LONGITUDINAL_CRACK -> "Longitudinal Crack"
            HazardType.TRANSVERSE_CRACK -> "Transverse Crack"
        }

    val isCritical: Boolean
        get() = severity.equals("critical", ignoreCase = true) || severity.equals("high", ignoreCase = true)

    companion object {
        fun deriveSeverity(confidence: Float): String = when {
            confidence >= 0.80f -> "critical"
            confidence >= 0.65f -> "high"
            confidence >= 0.45f -> "medium"
            else -> "low"
        }
    }
}

