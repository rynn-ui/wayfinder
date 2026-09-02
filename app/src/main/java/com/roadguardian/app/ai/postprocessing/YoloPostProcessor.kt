package com.roadguardian.app.ai.postprocessing

import com.roadguardian.app.domain.model.BoundingBox
import com.roadguardian.app.domain.model.HazardType
import com.roadguardian.app.domain.model.RoadHazardDetection
import java.util.UUID

class YoloPostProcessor(
    val confidenceThreshold: Float = 0.35f,
    val iouThreshold: Float = 0.45f,
    val inputSize: Int = 640,
    val totalPredictions: Int = 8400,
    val classCount: Int = 1,
    val potholeClassId: Int = 0,
    val singleClassMode: Boolean = true,
    val classLabels: List<String> = listOf("pothole")
) {

    companion object {
        const val MODEL_CLASS_LONGITUDINAL = 0
        const val MODEL_CLASS_TRANSVERSE = 1
        const val MODEL_CLASS_ALLIGATOR = 2
        const val MODEL_CLASS_POTHOLE = 3

        val DEFAULT_CLASS_LABELS = listOf(
            "longitudinal_crack",
            "transverse_crack",
            "alligator_crack",
            "pothole"
        )
    }

    private val effectiveClassCount = if (singleClassMode) 1 else classCount

    fun classLabel(classIndex: Int): String =
        classLabels.getOrElse(classIndex) { "class_$classIndex" }

    fun process(
        rawOutput: Array<Array<FloatArray>>,
        originalWidth: Int,
        originalHeight: Int,
        timestamp: Long = System.currentTimeMillis()
    ): List<RoadHazardDetection> {
        require(rawOutput.isNotEmpty() && rawOutput[0].size >= 4 + (if (singleClassMode) 1 else classCount))
        require(originalWidth > 0 && originalHeight > 0)

        val output = rawOutput[0]
        val scaleX = originalWidth.toFloat() / inputSize
        val scaleY = originalHeight.toFloat() / inputSize

        val candidates = mutableListOf<RoadHazardDetection>()

        for (i in 0 until totalPredictions) {
            val score: Float
            val isPothole: Boolean

            if (singleClassMode) {
                score = output[4][i]
                isPothole = true
            } else {
                val pScore = if (potholeClassId < classCount) output[4 + potholeClassId][i] else 0f
                var maxOtherScore = 0f
                for (c in 0 until classCount) {
                    if (c != potholeClassId) {
                        val other = output[4 + c][i]
                        if (other > maxOtherScore) {
                            maxOtherScore = other
                        }
                    }
                }
                score = pScore
                isPothole = pScore >= maxOtherScore && pScore >= confidenceThreshold
            }

            if (isPothole && score >= confidenceThreshold) {
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
                            hazardType = HazardType.POTHOLE,
                            confidence = score,
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

        return kept
    }

    fun computeRawStats(rawOutput: Array<Array<FloatArray>>): RawDetectionStats? {
        if (rawOutput.isEmpty()) return null
        val output = rawOutput[0]
        val rows = output.size
        if (rows < 4 + effectiveClassCount) return null
        val predictions = minOf(totalPredictions, output[0].size)
        if (predictions == 0) return null

        val hints = DetectionLayoutHint(rows, effectiveClassCount)

        var tensorMin = Float.MAX_VALUE
        var tensorMax = -Float.MAX_VALUE
        var sum = 0.0
        var count = 0L
        for (row in output) {
            for (v in row) {
                if (v < tensorMin) tensorMin = v
                if (v > tensorMax) tensorMax = v
                sum += v
                count++
            }
        }
        val tensorMean = if (count == 0L) 0f else (sum / count).toFloat()

        var rawMaxScore = -Float.MAX_VALUE
        var rawMaxClass = 0
        var rawMaxIndex = 0
        var bestAnchorScores = FloatArray(effectiveClassCount)
        var potholeMaxScore = -Float.MAX_VALUE
        var candidatesAtThreshold = 0

        for (i in 0 until predictions) {
            var bestClassScore = -Float.MAX_VALUE
            var bestClass = 0
            val anchorScores = FloatArray(effectiveClassCount)
            for (c in 0 until effectiveClassCount) {
                val s = output[hints.classRow(c)][i]
                anchorScores[c] = s
                if (s > bestClassScore) {
                    bestClassScore = s
                    bestClass = c
                }
            }

            if (bestClassScore > rawMaxScore) {
                rawMaxScore = bestClassScore
                rawMaxClass = bestClass
                rawMaxIndex = i
                bestAnchorScores = anchorScores.copyOf()
            }

            val potholeScore = output[hints.classRow(if (singleClassMode) 0 else potholeClassId)][i]
            if (potholeScore > potholeMaxScore) potholeMaxScore = potholeScore

            if (potholeScore >= confidenceThreshold && potholeScore >= bestClassScore) {
                candidatesAtThreshold++
            }
        }

        return RawDetectionStats(
            tensorMin = tensorMin,
            tensorMax = tensorMax,
            tensorMean = tensorMean,
            rawMaxScore = rawMaxScore,
            rawMaxClass = rawMaxClass,
            rawMaxClassLabel = classLabel(rawMaxClass),
            rawMaxBoxX = output[0][rawMaxIndex],
            rawMaxBoxY = output[1][rawMaxIndex],
            rawMaxBoxW = output[2][rawMaxIndex],
            rawMaxBoxH = output[3][rawMaxIndex],
            rawMaxTopAnchorClassScores = bestAnchorScores,
            potholeMaxScore = potholeMaxScore,
            candidatesAtThreshold = candidatesAtThreshold
        )
    }

    private data class DetectionLayoutHint(
        val totalRows: Int,
        val effectiveClassCount: Int
    ) {
        fun classRow(classIndex: Int): Int {
            val base = if (effectiveClassCount == 1) 0 else classIndex
            val candidate = 4 + base
            return if (candidate < totalRows) candidate else 4
        }
    }
}
