package com.roadguardian.app.ai.postprocessing

import com.roadguardian.app.domain.model.BoundingBox
import com.roadguardian.app.domain.model.HazardType
import com.roadguardian.app.domain.model.RoadHazardDetection
import java.util.UUID

class YoloPostProcessor(
    val confidenceThreshold: Float = 0.15f,
    val iouThreshold: Float = 0.45f,
    val inputSize: Int = 640,
    val totalPredictions: Int = 8400,
    val classCount: Int = 4
) {

    fun process(
        rawOutput: Array<Array<FloatArray>>,
        originalWidth: Int,
        originalHeight: Int,
        timestamp: Long = System.currentTimeMillis()
    ): List<RoadHazardDetection> {
        require(rawOutput.isNotEmpty() && rawOutput[0].size >= 4 + classCount)
        require(originalWidth > 0 && originalHeight > 0)

        val output = rawOutput[0]
        val scaleX = originalWidth.toFloat() / inputSize
        val scaleY = originalHeight.toFloat() / inputSize

        val candidates = mutableListOf<RoadHazardDetection>()

        for (i in 0 until totalPredictions) {
            var maxScore = 0.0f
            var maxClassId = -1

            for (c in 0 until classCount) {
                val score = output[4 + c][i]
                if (score > maxScore) {
                    maxScore = score
                    maxClassId = c
                }
            }

            if (maxScore >= confidenceThreshold && maxClassId >= 0) {
                val hazardType = HazardType.fromClassId(maxClassId) ?: continue
                val cx = output[0][i]
                val cy = output[1][i]
                val w = output[2][i]
                val h = output[3][i]

                val minX = maxOf(0.0f, (cx - w / 2.0f) * scaleX)
                val minY = maxOf(0.0f, (cy - h / 2.0f) * scaleY)
                val boxW = minOf(originalWidth.toFloat() - minX, w * scaleX)
                val boxH = minOf(originalHeight.toFloat() - minY, h * scaleY)

                if (boxW > 0.0f && boxH > 0.0f) {
                    val bbox = BoundingBox(
                        x = minX,
                        y = minY,
                        width = boxW,
                        height = boxH
                    )
                    candidates.add(
                        RoadHazardDetection(
                            id = UUID.randomUUID().toString(),
                            hazardType = hazardType,
                            confidence = maxScore,
                            timestamp = timestamp,
                            boundingBox = bbox
                        )
                    )
                }
            }
        }

        return applyNms(candidates, iouThreshold)
    }

    fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x, box2.x)
        val y1 = maxOf(box1.y, box2.y)
        val x2 = minOf(box1.x + box1.width, box2.x + box2.width)
        val y2 = minOf(box1.y + box1.height, box2.y + box2.height)

        val intersection = maxOf(0.0f, x2 - x1) * maxOf(0.0f, y2 - y1)
        val union = box1.area + box2.area - intersection

        return if (union <= 0.0f) 0.0f else intersection / union
    }

    fun applyNms(
        detections: List<RoadHazardDetection>,
        threshold: Float = iouThreshold
    ): List<RoadHazardDetection> {
        if (detections.isEmpty()) return emptyList()

        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val kept = mutableListOf<RoadHazardDetection>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            kept.add(best)

            val iterator = sorted.iterator()
            while (iterator.hasNext()) {
                val current = iterator.next()
                if (current.hazardType == best.hazardType) {
                    val box1 = best.boundingBox
                    val box2 = current.boundingBox
                    if (box1 != null && box2 != null) {
                        val iou = calculateIoU(box1, box2)
                        if (iou >= threshold) {
                            iterator.remove()
                        }
                    }
                }
            }
        }

        return kept
    }
}
