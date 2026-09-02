package com.roadguardian.app.data.remote

import android.util.Log
import com.roadguardian.app.domain.geo.GeoUtils
import com.roadguardian.app.domain.model.HazardType
import com.roadguardian.app.domain.model.PotholeStatus
import com.roadguardian.app.domain.model.RoadHazard

object HazardFirestoreMapper {

    private const val TAG = "HazardFirestoreMapper"

    const val COLLECTION_ROAD_HAZARDS = "road_hazards"

    const val FIELD_DEVICE_LATITUDE = "deviceLatitude"
    const val FIELD_DEVICE_LONGITUDE = "deviceLongitude"
    const val FIELD_HAZARD_TYPE = "hazardType"
    const val FIELD_CONFIDENCE = "confidence"
    const val FIELD_SEVERITY = "severity"
    const val FIELD_TIMESTAMP = "timestamp"
    const val FIELD_SOURCE = "source"
    const val FIELD_CONFIRMATION_COUNT = "confirmationCount"
    const val FIELD_LAST_SEEN_AT = "lastSeenAt"
    const val FIELD_GPS_ACCURACY = "gpsAccuracy"
    const val FIELD_STATUS = "status"
    const val FIELD_FIRST_DETECTED_AT = "firstDetectedAt"
    const val FIELD_IMAGE_URL = "imageUrl"

    private fun logWarn(message: String) {
        runCatching { Log.w(TAG, message) }
    }

    fun toFirestoreMap(hazard: RoadHazard): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            FIELD_DEVICE_LATITUDE to hazard.deviceLatitude,
            FIELD_DEVICE_LONGITUDE to hazard.deviceLongitude,
            FIELD_HAZARD_TYPE to hazard.hazardType.name,
            FIELD_CONFIDENCE to hazard.confidence.toDouble(),
            FIELD_SEVERITY to hazard.severity,
            FIELD_TIMESTAMP to hazard.timestamp,
            FIELD_SOURCE to hazard.source,
            FIELD_CONFIRMATION_COUNT to hazard.confirmationCount,
            FIELD_LAST_SEEN_AT to hazard.lastSeenAt,
            FIELD_STATUS to hazard.status.name,
            FIELD_FIRST_DETECTED_AT to hazard.firstDetectedAt
        )

        hazard.gpsAccuracy?.let { map[FIELD_GPS_ACCURACY] = it.toDouble() }
        hazard.imageUrl?.let { map[FIELD_IMAGE_URL] = it }

        return map
    }

    fun fromFirestoreMap(id: String, data: Map<String, Any?>): RoadHazard? {
        if (id.isBlank()) {
            logWarn("Rejecting hazard document with blank id")
            return null
        }

        return try {
            val lat = (data[FIELD_DEVICE_LATITUDE] as? Number)?.toDouble() ?: return null
            val lon = (data[FIELD_DEVICE_LONGITUDE] as? Number)?.toDouble() ?: return null

            if (!GeoUtils.isValidCoordinate(lat, lon)) {
                logWarn("Rejecting hazard document $id with invalid coordinates: ($lat, $lon)")
                return null
            }

            val rawType = data[FIELD_HAZARD_TYPE] as? String
            val hazardType = parseHazardType(rawType)

            val rawConfidence = (data[FIELD_CONFIDENCE] as? Number)?.toFloat() ?: return null
            val confidence = rawConfidence.coerceIn(0.0f, 1.0f)

            val timestamp = (data[FIELD_TIMESTAMP] as? Number)?.toLong() ?: return null
            if (timestamp < 0L) return null

            val severity = (data[FIELD_SEVERITY] as? String)?.takeIf { it.isNotBlank() }
                ?: RoadHazard.deriveSeverity(confidence)

            val source = (data[FIELD_SOURCE] as? String)?.takeIf { it.isNotBlank() } ?: "YOLO"
            val confirmationCount = ((data[FIELD_CONFIRMATION_COUNT] as? Number)?.toInt() ?: 1).coerceAtLeast(1)
            val lastSeenAt = ((data[FIELD_LAST_SEEN_AT] as? Number)?.toLong() ?: timestamp).coerceAtLeast(0L)
            val firstDetectedAt = ((data[FIELD_FIRST_DETECTED_AT] as? Number)?.toLong() ?: timestamp).coerceAtLeast(0L)
            val gpsAccuracy = (data[FIELD_GPS_ACCURACY] as? Number)?.toFloat()
            val rawStatus = data[FIELD_STATUS] as? String
            val status = PotholeStatus.fromValue(rawStatus)
            val imageUrl = data[FIELD_IMAGE_URL] as? String

            RoadHazard(
                id = id,
                deviceLatitude = lat,
                deviceLongitude = lon,
                hazardType = hazardType,
                confidence = confidence,
                severity = severity,
                timestamp = timestamp,
                source = source,
                confirmationCount = confirmationCount,
                lastSeenAt = lastSeenAt,
                gpsAccuracy = gpsAccuracy,
                status = status,
                firstDetectedAt = firstDetectedAt,
                imageUrl = imageUrl
            )
        } catch (e: Exception) {
            logWarn("Error mapping Firestore document $id to RoadHazard: ${e.message}")
            null
        }
    }

    fun parseHazardType(raw: String?): HazardType {
        if (raw.isNullOrBlank()) return HazardType.POTHOLE
        return HazardType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
            ?: HazardType.fromLabel(raw)
            ?: HazardType.fromCode(raw)
            ?: HazardType.POTHOLE
    }
}

