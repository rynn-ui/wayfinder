package com.roadguardian.app.data.repository

import com.roadguardian.app.domain.geo.GeoUtils
import com.roadguardian.app.domain.model.HazardSeverity
import com.roadguardian.app.domain.model.HazardType
import com.roadguardian.app.domain.model.PotholeStatus
import com.roadguardian.app.domain.model.RoadHazard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface RoadHazardRepository {
    val deduplicationRadiusMeters: Double
    val hazardsState: StateFlow<List<RoadHazard>>
    val allHazards: List<RoadHazard>

    val isRegionSyncActive: Boolean
        get() = false

    fun recordDetection(
        hazardType: HazardType = HazardType.POTHOLE,
        confidence: Float,
        latitude: Double,
        longitude: Double,
        timestamp: Long = System.currentTimeMillis()
    ): RoadHazard?

    fun recordPothole(
        latitude: Double,
        longitude: Double,
        confidence: Float,
        severity: HazardSeverity = HazardSeverity.fromConfidence(confidence),
        gpsAccuracy: Float? = null,
        timestamp: Long = System.currentTimeMillis()
    ): RoadHazard? {
        return recordDetection(
            hazardType = HazardType.POTHOLE,
            confidence = confidence,
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp
        )
    }

    fun getHazardsNearby(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double
    ): List<RoadHazard>

    fun getHazardsInBounds(
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): List<RoadHazard>

    fun addHazards(hazards: List<RoadHazard>)
    fun clear()

    fun startRegionSync(latitude: Double, longitude: Double, radiusKm: Double = 25.0) {}
    fun stopRegionSync() {}
}

open class InMemoryRoadHazardRepository(
    override val deduplicationRadiusMeters: Double = 15.0
) : RoadHazardRepository {

    protected val hazardsMap = ConcurrentHashMap<String, RoadHazard>()
    protected val _hazardsState = MutableStateFlow<List<RoadHazard>>(emptyList())
    override val hazardsState: StateFlow<List<RoadHazard>> = _hazardsState.asStateFlow()

    protected val pendingSyncList = mutableListOf<RoadHazard>()

    override val allHazards: List<RoadHazard>
        get() = _hazardsState.value

    fun getPendingSyncs(): List<RoadHazard> = synchronized(pendingSyncList) { pendingSyncList.toList() }

    fun clearPendingSync(hazardId: String) = synchronized(pendingSyncList) {
        pendingSyncList.removeAll { it.id == hazardId }
    }

    @Synchronized
    override fun recordDetection(
        hazardType: HazardType,
        confidence: Float,
        latitude: Double,
        longitude: Double,
        timestamp: Long
    ): RoadHazard? {
        return recordPotholeInternal(
            latitude = latitude,
            longitude = longitude,
            confidence = confidence,
            severity = HazardSeverity.fromConfidence(confidence),
            gpsAccuracy = null,
            hazardType = hazardType,
            timestamp = timestamp
        )
    }

    @Synchronized
    override fun recordPothole(
        latitude: Double,
        longitude: Double,
        confidence: Float,
        severity: HazardSeverity,
        gpsAccuracy: Float?,
        timestamp: Long
    ): RoadHazard? {
        return recordPotholeInternal(
            latitude = latitude,
            longitude = longitude,
            confidence = confidence,
            severity = severity,
            gpsAccuracy = gpsAccuracy,
            hazardType = HazardType.POTHOLE,
            timestamp = timestamp
        )
    }

    private fun recordPotholeInternal(
        latitude: Double,
        longitude: Double,
        confidence: Float,
        severity: HazardSeverity,
        gpsAccuracy: Float?,
        hazardType: HazardType,
        timestamp: Long
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
            val weight = minOf(existingCandidate.confirmationCount, 10)
            val updatedLat = (existingCandidate.deviceLatitude * weight + latitude) / (weight + 1)
            val updatedLon = (existingCandidate.deviceLongitude * weight + longitude) / (weight + 1)
            val updatedConfidence = maxOf(existingCandidate.confidence, clampedConfidence)
            val finalSeverity = if (severity.level > existingCandidate.hazardSeverity.level) {
                severity.value
            } else {
                existingCandidate.severity
            }

            existingCandidate.copy(
                deviceLatitude = updatedLat,
                deviceLongitude = updatedLon,
                confidence = updatedConfidence,
                severity = finalSeverity,
                confirmationCount = newCount,
                lastSeenAt = timestamp,
                gpsAccuracy = gpsAccuracy ?: existingCandidate.gpsAccuracy,
                status = PotholeStatus.ACTIVE
            )
        } else {
            RoadHazard(
                id = UUID.randomUUID().toString(),
                deviceLatitude = latitude,
                deviceLongitude = longitude,
                hazardType = hazardType,
                confidence = clampedConfidence,
                severity = severity.value,
                timestamp = timestamp,
                source = "YOLO",
                confirmationCount = 1,
                lastSeenAt = timestamp,
                gpsAccuracy = gpsAccuracy,
                status = PotholeStatus.ACTIVE,
                firstDetectedAt = timestamp
            )
        }

        hazardsMap[hazard.id] = hazard
        _hazardsState.value = hazardsMap.values.toList()
        synchronized(pendingSyncList) {
            pendingSyncList.removeAll { it.id == hazard.id }
            pendingSyncList.add(hazard)
        }
        return hazard
    }

    override fun getHazardsNearby(
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

    override fun getHazardsInBounds(
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
    override fun addHazards(hazards: List<RoadHazard>) {
        hazards.forEach { hazardsMap[it.id] = it }
        _hazardsState.value = hazardsMap.values.toList()
    }

    @Synchronized
    override fun clear() {
        hazardsMap.clear()
        _hazardsState.value = emptyList()
        synchronized(pendingSyncList) { pendingSyncList.clear() }
    }
}
