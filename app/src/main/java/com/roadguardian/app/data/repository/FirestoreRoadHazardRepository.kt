package com.roadguardian.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.roadguardian.app.auth.FirebaseAuthManager
import com.roadguardian.app.data.remote.HazardFirestoreMapper
import com.roadguardian.app.domain.geo.GeoUtils
import com.roadguardian.app.domain.model.HazardSeverity
import com.roadguardian.app.domain.model.HazardType
import com.roadguardian.app.domain.model.RoadHazard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.cos

class FirestoreRoadHazardRepository(
    private val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull(),
    private val authManager: FirebaseAuthManager? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    override val deduplicationRadiusMeters: Double = 15.0,
    val regionSyncThresholdMeters: Double = DEFAULT_REGION_SYNC_THRESHOLD_METERS
) : InMemoryRoadHazardRepository(deduplicationRadiusMeters) {

    companion object {
        private const val TAG = "FirestoreHazardRepo"
        const val DEFAULT_REGION_SYNC_THRESHOLD_METERS = 1500.0
        const val DEFAULT_SYNC_RADIUS_KM = 25.0
    }

    private fun logDebug(message: String) {
        runCatching { Log.d(TAG, message) }
    }

    private fun logWarn(message: String) {
        runCatching { Log.w(TAG, message) }
    }

    private var activeRegionListener: ListenerRegistration? = null
    private var lastSyncLatitude: Double? = null
    private var lastSyncLongitude: Double? = null

    override val isRegionSyncActive: Boolean
        @Synchronized get() = activeRegionListener != null

    val currentSyncCenter: Pair<Double, Double>?
        @Synchronized get() = if (lastSyncLatitude != null && lastSyncLongitude != null) {
            Pair(lastSyncLatitude!!, lastSyncLongitude!!)
        } else {
            null
        }

    init {
        authManager?.ensureAnonymousAuth(
            onSuccess = { uid ->
                logDebug("Firebase Anonymous Auth active for Firestore: $uid")
                flushPendingSyncs()
            },
            onError = { ex ->
                logWarn("Anonymous auth warning: ${ex.message}")
            }
        )
    }

    @Synchronized
    override fun recordDetection(
        hazardType: HazardType,
        confidence: Float,
        latitude: Double,
        longitude: Double,
        timestamp: Long
    ): RoadHazard? {
        val updatedHazard = super.recordDetection(
            hazardType = hazardType,
            confidence = confidence,
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp
        ) ?: return null

        persistHazardAsynchronously(updatedHazard)
        return updatedHazard
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
        val updatedHazard = super.recordPothole(
            latitude = latitude,
            longitude = longitude,
            confidence = confidence,
            severity = severity,
            gpsAccuracy = gpsAccuracy,
            timestamp = timestamp
        ) ?: return null

        persistHazardAsynchronously(updatedHazard)
        return updatedHazard
    }

    fun flushPendingSyncs() {
        val db = firestore ?: return
        val pending = getPendingSyncs()
        if (pending.isEmpty()) return

        coroutineScope.launch {
            val isAuthReady = authManager?.awaitAuthenticated() ?: true
            if (!isAuthReady) return@launch

            for (hazard in pending) {
                try {
                    val data = HazardFirestoreMapper.toFirestoreMap(hazard)
                    db.collection(HazardFirestoreMapper.COLLECTION_ROAD_HAZARDS)
                        .document(hazard.id)
                        .set(data, SetOptions.merge())
                        .addOnSuccessListener {
                            clearPendingSync(hazard.id)
                            logDebug("Pending hazard synced: ${hazard.id}")
                        }
                } catch (_: Exception) {}
            }
        }
    }

    private fun persistHazardAsynchronously(hazard: RoadHazard) {
        val db = firestore ?: return

        coroutineScope.launch {
            try {
                val isAuthReady = authManager?.awaitAuthenticated() ?: true
                if (!isAuthReady) {
                    logWarn("Skipping Firestore write for hazard ${hazard.id}: Auth not ready")
                    return@launch
                }

                val data = HazardFirestoreMapper.toFirestoreMap(hazard)
                db.collection(HazardFirestoreMapper.COLLECTION_ROAD_HAZARDS)
                    .document(hazard.id)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener {
                        clearPendingSync(hazard.id)
                        logDebug("Hazard persisted to Firestore: ${hazard.id} (${hazard.displayTitle}, x${hazard.confirmationCount})")
                    }
                    .addOnFailureListener { ex ->
                        logWarn("Firestore write failed for hazard ${hazard.id}: ${ex.message}")
                    }
            } catch (e: Exception) {
                logWarn("Error initiating Firestore write: ${e.message}")
            }
        }
    }

    @Synchronized
    override fun startRegionSync(latitude: Double, longitude: Double, radiusKm: Double) {
        if (!GeoUtils.isValidCoordinate(latitude, longitude)) return
        val db = firestore ?: return

        val lastLat = lastSyncLatitude
        val lastLon = lastSyncLongitude
        if (lastLat != null && lastLon != null && activeRegionListener != null) {
            val dist = GeoUtils.haversineDistance(lastLat, lastLon, latitude, longitude)
            if (dist < regionSyncThresholdMeters) {
                logDebug("User within current sync region (${dist.toInt()}m < ${regionSyncThresholdMeters.toInt()}m). Keeping active listener.")
                return
            }
            logDebug("User moved ${dist.toInt()}m (>= threshold ${regionSyncThresholdMeters.toInt()}m). Replacing geographic listener.")
        }

        stopRegionSync()

        lastSyncLatitude = latitude
        lastSyncLongitude = longitude

        coroutineScope.launch {
            val isAuthReady = authManager?.awaitAuthenticated() ?: true
            if (!isAuthReady) {
                logWarn("Cannot start region sync: Auth not ready")
                return@launch
            }

            val latDelta = radiusKm / 111.0
            val cosLat = cos(Math.toRadians(latitude)).coerceAtLeast(0.1)
            val lonDelta = radiusKm / (111.0 * cosLat)

            val minLat = (latitude - latDelta).coerceIn(-90.0, 90.0)
            val maxLat = (latitude + latDelta).coerceIn(-90.0, 90.0)
            val minLon = (longitude - lonDelta).coerceIn(-180.0, 180.0)
            val maxLon = (longitude + lonDelta).coerceIn(-180.0, 180.0)
            val radiusMeters = radiusKm * 1000.0

            try {
                logDebug("Starting scoped region sync: lat in [$minLat, $maxLat], lon in [$minLon, $maxLon]")

                val query = db.collection(HazardFirestoreMapper.COLLECTION_ROAD_HAZARDS)
                    .whereGreaterThanOrEqualTo(HazardFirestoreMapper.FIELD_DEVICE_LATITUDE, minLat)
                    .whereLessThanOrEqualTo(HazardFirestoreMapper.FIELD_DEVICE_LATITUDE, maxLat)

                val listener = query.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        logWarn("Region sync snapshot error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val remoteHazards = mutableListOf<RoadHazard>()
                        for (doc in snapshot.documents) {
                            val data = doc.data ?: continue
                            val hazard = HazardFirestoreMapper.fromFirestoreMap(doc.id, data) ?: continue

                            val inBounds = GeoUtils.isInBounds(
                                latitude = hazard.deviceLatitude,
                                longitude = hazard.deviceLongitude,
                                minLatitude = minLat,
                                maxLatitude = maxLat,
                                minLongitude = minLon,
                                maxLongitude = maxLon
                            )

                            val withinRadius = GeoUtils.haversineDistance(
                                lat1 = latitude,
                                lon1 = longitude,
                                lat2 = hazard.deviceLatitude,
                                lon2 = hazard.deviceLongitude
                            ) <= radiusMeters

                            if (inBounds && withinRadius) {
                                remoteHazards.add(hazard)
                            }
                        }

                        mergeRemoteHazards(remoteHazards)
                    }
                }

                synchronized(this@FirestoreRoadHazardRepository) {
                    activeRegionListener = listener
                }
            } catch (e: Exception) {
                logWarn("Failed to start region sync: ${e.message}")
            }
        }
    }

    @Synchronized
    private fun mergeRemoteHazards(remoteHazards: List<RoadHazard>) {
        for (remote in remoteHazards) {
            val local = hazardsMap[remote.id]
            if (local == null) {
                hazardsMap[remote.id] = remote
            } else {
                val highestCount = maxOf(local.confirmationCount, remote.confirmationCount)
                val highestConfidence = maxOf(local.confidence, remote.confidence)
                val latestLastSeen = maxOf(local.lastSeenAt, remote.lastSeenAt)
                val highestSeverity = if (remote.hazardSeverity.level > local.hazardSeverity.level) {
                    remote.severity
                } else {
                    local.severity
                }

                val merged = local.copy(
                    confirmationCount = highestCount,
                    confidence = highestConfidence,
                    severity = highestSeverity,
                    lastSeenAt = latestLastSeen,
                    gpsAccuracy = remote.gpsAccuracy ?: local.gpsAccuracy,
                    status = remote.status
                )
                hazardsMap[remote.id] = merged
            }
        }
        _hazardsState.value = hazardsMap.values.toList()
    }

    @Synchronized
    override fun stopRegionSync() {
        if (activeRegionListener != null) {
            logDebug("Removing active Firestore region snapshot listener.")
            activeRegionListener?.remove()
            activeRegionListener = null
        }
        lastSyncLatitude = null
        lastSyncLongitude = null
    }
}

