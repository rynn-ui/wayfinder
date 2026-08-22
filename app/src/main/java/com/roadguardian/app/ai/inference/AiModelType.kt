package com.roadguardian.app.ai.inference

/**
 * Supported TFLite model variants for A/B testing and performance benchmarking.
 */
enum class AiModelType(
    val displayName: String,
    val assetPath: String,
    val description: String,
    val precisionLabel: String
) {
    INT8(
        displayName = "INT8",
        assetPath = "yolo12n_seed0_best_dynamic_range_quant.tflite",
        description = "Dynamic INT8 quantized",
        precisionLabel = "INT8 (2.83 MB)"
    ),
    FP16(
        displayName = "FP16",
        assetPath = "yolo12n_seed0_best_float16.tflite",
        description = "Float16 precision",
        precisionLabel = "FP16 (5.08 MB)"
    );

    companion object {
        val DEFAULT = INT8
    }
}
