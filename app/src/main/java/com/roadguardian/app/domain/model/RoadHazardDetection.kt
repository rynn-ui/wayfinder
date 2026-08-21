package com.roadguardian.app.domain.model

data class RoadHazardDetection(
    val id: String,
    val hazardType: HazardType,
    val confidence: Float,
    val timestamp: Long,
    val boundingBox: BoundingBox? = null,
    val location: LocationData? = null,
    val accelerometerData: AccelerometerData? = null,
    val gyroscopeData: GyroscopeData? = null
) {
    init {
        require(id.isNotBlank())
        require(confidence in 0.0f..1.0f)
        require(timestamp >= 0L)
    }

    val latitude: Double?
        get() = location?.latitude

    val longitude: Double?
        get() = location?.longitude

    val locationAccuracy: Float?
        get() = location?.accuracy

    val vehicleSpeed: Float?
        get() = location?.speed
}

typealias Detection = RoadHazardDetection
