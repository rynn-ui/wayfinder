package com.roadguardian.app.data.repository

import com.roadguardian.app.domain.geo.GeoUtils
import com.roadguardian.app.domain.model.HazardType
import com.roadguardian.app.domain.model.RoadHazard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RoadHazardRepository(
    val deduplicationRadiusMeters: Double = 15.0
) {

    private val hazardsMap = ConcurrentHashMap<String, RoadHazard>()
    private val _hazardsState = MutableStateFlow<List<RoadHazard>>(emptyList())
    val hazardsState: StateFlow<List<RoadHazard>> = _hazardsState.asStateFlow()

    val allHazards: List<RoadHazard>
        get() = _hazardsState.value

    @Synchronized
    fun recordDetection(
        hazardType: HazardType,
        confidence: Float,
        latitude: Double,
        longitude: Double,
        timestamp: Long = System.currentTimeMillis()
    ): RoadHazard? {
        if (!GeoUtils.isValidCoordinate(latitude, longitude)) {
            return null
        }

        val clampedConfidence = confidence.coerceIn(0.0f, 1.0f)

        val existingCandidate = hazardsMap.values.firstOrNull { existing ->
            existing.hazardType == hazardType &&
                    GeoUtils.haversineDistance(
                        existing.deviceLatitude,
                        existing.deviceLongitude,
                        latitude,
                        longitude
                    ) <= deduplicationRadiusMeters
        }

        val hazard = if (existingCandidate != null) {
            val newCount = existingCandidate.confirmationCount + 1
            val updatedLat = (existingCandidate.deviceLatitude * existingCandidate.confirmationCount + latitude) / newCount
            val updatedLon = (existingCandidate.deviceLongitude * existingCandidate.confirmationCount + longitude) / newCount
            val updatedConfidence = maxOf(existingCandidate.confidence, clampedConfidence)

            existingCandidate.copy(
                deviceLatitude = updatedLat,
                deviceLongitude = updatedLon,
                confidence = updatedConfidence,
                severity = RoadHazard.deriveSeverity(updatedConfidence),
                confirmationCount = newCount,
                lastSeenAt = timestamp
            )
        } else {
            RoadHazard(
                id = UUID.randomUUID().toString(),
                deviceLatitude = latitude,
                deviceLongitude = longitude,
                hazardType = hazardType,
                confidence = clampedConfidence,
                severity = RoadHazard.deriveSeverity(clampedConfidence),
                timestamp = timestamp,
                source = "YOLO",
                confirmationCount = 1,
                lastSeenAt = timestamp
            )
        }

        hazardsMap[hazard.id] = hazard
        _hazardsState.value = hazardsMap.values.toList()
        return hazard
    }

    fun getHazardsNearby(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double
    ): List<RoadHazard> {
        if (!GeoUtils.isValidCoordinate(latitude, longitude)) return emptyList()
        return hazardsMap.values.filter {
            GeoUtils.haversineDistance(
                it.deviceLatitude,
                it.deviceLongitude,
                latitude,
                longitude
            ) <= radiusMeters
        }
    }

    fun getHazardsInBounds(
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): List<RoadHazard> {
        return hazardsMap.values.filter {
            GeoUtils.isInBounds(
                it.deviceLatitude,
                it.deviceLongitude,
                minLatitude,
                maxLatitude,
                minLongitude,
                maxLongitude
            )
        }
    }

    @Synchronized
    fun addHazards(hazards: List<RoadHazard>) {
        hazards.forEach { hazardsMap[it.id] = it }
        _hazardsState.value = hazardsMap.values.toList()
    }

    @Synchronized
    fun clear() {
        hazardsMap.clear()
        _hazardsState.value = emptyList()
    }
}
