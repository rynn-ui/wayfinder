package com.roadguardian.app.traffic.model

import androidx.compose.ui.graphics.Color
import org.osmdroid.util.GeoPoint

enum class WazeTrafficType {
    JAM,
    ACCIDENT,
    ROAD_CLOSED,
    HAZARD,
    POLICE,
    OTHER;

    val displayName: String
        get() = when (this) {
            JAM -> "Traffic Jam"
            ACCIDENT -> "Accident Reported"
            ROAD_CLOSED -> "Road Closed"
            HAZARD -> "Hazard on Road"
            POLICE -> "Police Reported"
            OTHER -> "Traffic Alert"
        }
}

enum class WazeJamSeverity(
    val level: Int,
    val label: String,
    val color: Color
) {
    FREE_FLOW(1, "Light Traffic", Color(0xFF10B981)),
    MODERATE(2, "Moderate Slowdown", Color(0xFFF59E0B)),
    HEAVY(3, "Heavy Congestion", Color(0xFFEF4444)),
    STANDSTILL(4, "Traffic Standstill", Color(0xFF991B1B));

    companion object {
        fun fromLevel(level: Int): WazeJamSeverity {
            return when {
                level >= 4 -> STANDSTILL
                level == 3 -> HEAVY
                level == 2 -> MODERATE
                else -> FREE_FLOW
            }
        }
    }
}

data class WazeTrafficIncident(
    val id: String,
    val type: WazeTrafficType,
    val severity: WazeJamSeverity,
    val streetName: String,
    val city: String = "",
    val reportDescription: String,
    val latitude: Double,
    val longitude: Double,
    val delaySeconds: Int = 0,
    val speedKmh: Double = 0.0,
    val lengthMeters: Int = 0,
    val confidence: Int = 5,
    val reliability: Int = 5,
    val nThumbsUp: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val jamPolyline: List<GeoPoint> = emptyList(),
    val distanceMetersFromUser: Double = 0.0
) {
    val delayMinutes: Int
        get() = (delaySeconds + 30) / 60

    val formattedDelay: String
        get() = if (delayMinutes <= 0) "Minor delay" else "+$delayMinutes min delay"

    val formattedSpeed: String
        get() = if (speedKmh <= 0.0) "Slow" else "${speedKmh.toInt()} km/h"

    val formattedDistance: String
        get() = if (distanceMetersFromUser >= 1000.0) {
            "%.1f km away".format(distanceMetersFromUser / 1000.0)
        } else {
            "${distanceMetersFromUser.toInt()} m away"
        }
}

data class WazeTrafficSummary(
    val totalAlertsCount: Int = 0,
    val jamsCount: Int = 0,
    val accidentsCount: Int = 0,
    val maxDelayMinutes: Int = 0,
    val overallSeverity: WazeJamSeverity = WazeJamSeverity.FREE_FLOW,
    val nearestIncident: WazeTrafficIncident? = null,
    val incidents: List<WazeTrafficIncident> = emptyList()
) {
    val summaryText: String
        get() = when {
            totalAlertsCount == 0 -> "Smooth Traffic Nearby"
            jamsCount > 0 && maxDelayMinutes > 0 -> "$jamsCount Jams Nearby (+$maxDelayMinutes min delay)"
            jamsCount > 0 -> "$jamsCount Traffic Slowdowns Detected"
            accidentsCount > 0 -> "$accidentsCount Accidents Reported Nearby"
            else -> "$totalAlertsCount Traffic Alerts Active"
        }
}

sealed class WazeTrafficState {
    data object Loading : WazeTrafficState()
    data class Success(val summary: WazeTrafficSummary) : WazeTrafficState()
    data class Error(val message: String, val fallbackSummary: WazeTrafficSummary? = null) : WazeTrafficState()
}
