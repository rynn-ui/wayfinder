package com.roadguardian.app.domain.geo

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {

    private const val EARTH_RADIUS_METERS = 6371000.0

    fun haversineDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        val a = sin(latDistance / 2) * sin(latDistance / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(lonDistance / 2) * sin(lonDistance / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
        if (latitude.isNaN() || longitude.isNaN()) return false
        if (latitude < -90.0 || latitude > 90.0) return false
        if (longitude < -180.0 || longitude > 180.0) return false
        if (latitude == 0.0 && longitude == 0.0) return false
        return true
    }

    fun isInBounds(
        latitude: Double,
        longitude: Double,
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): Boolean {
        if (!isValidCoordinate(latitude, longitude)) return false
        return latitude in minLatitude..maxLatitude && longitude in minLongitude..maxLongitude
    }
}
