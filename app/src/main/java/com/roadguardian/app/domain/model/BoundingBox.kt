package com.roadguardian.app.domain.model

data class BoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    init {
        require(x >= 0.0f)
        require(y >= 0.0f)
        require(width >= 0.0f)
        require(height >= 0.0f)
    }

    val area: Float
        get() = width * height
}
