package com.roadguardian.app.domain.model

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val altitude: Double? = null,
    val geohash: String? = null
)
