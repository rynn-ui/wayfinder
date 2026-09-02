package com.roadguardian.app.warning

import com.roadguardian.app.domain.geo.GeoUtils
import com.roadguardian.app.domain.model.HazardSeverity
import com.roadguardian.app.domain.model.RoadHazard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

import com.roadguardian.app.traffic.model.WazeJamSeverity
import com.roadguardian.app.traffic.model.WazeTrafficIncident

class HazardWarningManager(
    val warningRadiusMeters: Double = 50.0,
    val forwardConeDegrees: Double = 45.0,
    val minSpeedForBearingMps: Float = 1.5f,
    private val audioWarningManager: AudioWarningManager? = null
) {

    private val _warningState = MutableStateFlow(HazardWarningState())
    val warningState: StateFlow<HazardWarningState> = _warningState.asStateFlow()

    fun update(
        userLatitude: Double,
        userLongitude: Double,
        userBearing: Float?,
        userSpeedMps: Float?,
        knownHazards: List<RoadHazard>
    ): HazardWarningState {
        val movingWithBearing = userBearing != null && (userSpeedMps ?: 0f) >= minSpeedForBearingMps

        if (!GeoUtils.isValidCoordinate(userLatitude, userLongitude) || knownHazards.isEmpty() || !movingWithBearing) {
            val clearState = HazardWarningState(isWarningActive = false)
            _warningState.value = clearState
            return clearState
        }

        val nearbyHazards = knownHazards.filter {
            it.hazardType == com.roadguardian.app.domain.model.HazardType.POTHOLE || it.displayTitle.contains("pothole", ignoreCase = true)
        }.mapNotNull { hazard ->
            val dist = GeoUtils.haversineDistance(
                userLatitude,
                userLongitude,
                hazard.deviceLatitude,
                hazard.deviceLongitude
            )
            if (dist <= warningRadiusMeters) {
                Pair(hazard, dist)
            } else {
                null
            }
        }

        if (nearbyHazards.isEmpty()) {
            val clearState = HazardWarningState(isWarningActive = false)
            _warningState.value = clearState
            return clearState
        }

        val aheadHazards = nearbyHazards.filter { (hazard, _) ->
            val bearingToHazard = calculateBearing(
                userLatitude,
                userLongitude,
                hazard.deviceLatitude,
                hazard.deviceLongitude
            )
            val diff = calculateAngularDifference(userBearing.toDouble(), bearingToHazard)
            diff <= forwardConeDegrees
        }

        if (aheadHazards.isEmpty()) {
            val clearState = HazardWarningState(isWarningActive = false)
            _warningState.value = clearState
            return clearState
        }

        val (bestHazard, bestDist) = aheadHazards.minWithOrNull(
            compareByDescending<Pair<RoadHazard, Double>> { it.first.hazardSeverity.level }
                .thenBy { it.second }
        ) ?: aheadHazards.first()

        val distanceInt = bestDist.toInt().coerceAtLeast(1)
        val text = "⚠ POTHOLE AHEAD • ${bestHazard.severity.uppercase()} • ${distanceInt}m"

        val activeState = HazardWarningState(
            isWarningActive = true,
            hazard = bestHazard,
            distanceMeters = distanceInt,
            severity = bestHazard.hazardSeverity,
            isAhead = true,
            warningText = text
        )

        _warningState.value = activeState

        audioWarningManager?.warnHazard(bestHazard.severity, distanceInt, bestHazard.id)

        return activeState
    }

    fun updateTraffic(
        userLatitude: Double,
        userLongitude: Double,
        userBearing: Float?,
        userSpeedMps: Float?,
        incidents: List<WazeTrafficIncident>,
        trafficWarningRadiusMeters: Double = 800.0,
        forwardConeDegrees: Double = 60.0
    ): WazeTrafficIncident? {
        val movingWithBearing = userBearing != null && (userSpeedMps ?: 0f) >= minSpeedForBearingMps
        if (!GeoUtils.isValidCoordinate(userLatitude, userLongitude) || incidents.isEmpty() || !movingWithBearing) {
            return null
        }

        val aheadIncidents = incidents.filter { incident ->
            incident.severity != WazeJamSeverity.FREE_FLOW
        }.mapNotNull { incident ->
            val dist = GeoUtils.haversineDistance(
                userLatitude,
                userLongitude,
                incident.latitude,
                incident.longitude
            )
            if (dist in 40.0..trafficWarningRadiusMeters) {
                val bearingToIncident = calculateBearing(
                    userLatitude,
                    userLongitude,
                    incident.latitude,
                    incident.longitude
                )
                val diff = calculateAngularDifference(userBearing.toDouble(), bearingToIncident)
                if (diff <= forwardConeDegrees) {
                    Pair(incident, dist)
                } else {
                    null
                }
            } else {
                null
            }
        }

        if (aheadIncidents.isEmpty()) return null

        val (bestIncident, bestDist) = aheadIncidents.minWithOrNull(
            compareByDescending<Pair<WazeTrafficIncident, Double>> { it.first.severity.level }
                .thenBy { it.second }
        ) ?: aheadIncidents.first()

        audioWarningManager?.warnTraffic(
            type = bestIncident.type.name,
            streetName = bestIncident.streetName,
            severity = bestIncident.severity.name,
            distanceMeters = bestDist.toInt(),
            incidentId = bestIncident.id
        )

        return bestIncident
    }

    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        return (Math.toDegrees(theta) + 360.0) % 360.0
    }

    fun calculateAngularDifference(angle1: Double, angle2: Double): Double {
        val diff = abs((angle1 - angle2 + 540.0) % 360.0 - 180.0)
        return diff
    }

    fun reset() {
        _warningState.value = HazardWarningState(isWarningActive = false)
    }
}
