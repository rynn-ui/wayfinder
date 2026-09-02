package com.roadguardian.app.ai.inference

enum class AiModelType(
    val displayName: String,
    val assetPath: String,
    val description: String,
    val precisionLabel: String,
    val isSingleClass: Boolean
) {
    YOLO11(
        displayName = "1-Class YOLO11 (Top Accuracy)",
        assetPath = "pothole_yolo11n_dynamic_int8.tflite",
        description = "High-accuracy YOLO11 pothole detector",
        precisionLabel = "INT8 (2.76 MB)",
        isSingleClass = true
    );

    companion object {
        val DEFAULT = YOLO11
    }
}
