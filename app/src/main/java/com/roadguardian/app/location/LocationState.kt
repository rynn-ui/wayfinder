package com.roadguardian.app.location

data class LocationState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float? = null,
    val speedMps: Float? = null,
    val bearing: Float? = null,
    val altitude: Double? = null,
    val timestamp: Long = 0L,
    val isAvailable: Boolean = false
) {
    val speedKmh: Float
        get() = if (speedMps != null && speedMps > 0f) speedMps * 3.6f else 0f

    val formattedSpeed: String
        get() {
            if (!isAvailable || speedMps == null) return "-- km/h"
            val kmh = speedKmh
            return if (kmh < 0.5f) "0 km/h" else "${kmh.toInt()} km/h"
        }

    val speedDisplayValue: String
        get() {
            if (!isAvailable || speedMps == null) return "--"
            val kmh = speedKmh
            return if (kmh < 0.5f) "0" else "${kmh.toInt()}"
        }

    val speedDisplayUnit: String
        get() = "km/h"
}
