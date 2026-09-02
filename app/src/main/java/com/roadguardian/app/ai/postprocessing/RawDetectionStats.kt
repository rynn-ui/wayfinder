package com.roadguardian.app.ai.postprocessing

/**
 * DEBUG-only (BuildConfig.DEBUG) diagnostic snapshot of the raw TFLite output
 * tensor BEFORE any confidence/NMS filtering.
 *
 * Populated directly from the decoded `[1, 4+nc, 8400]` tensor so an operator can
 * see exactly what the model emitted for the current frame:
 *
 *   - whole-tensor min/max/mean
 *   - the highest raw class score of any anchor (no threshold applied)
 *   - the class index and box of that highest-scoring anchor (input-space, 640x640)
 *   - the highest pothole-class score
 *   - how many anchors would have passed the confidence + class gate
 */
data class RawDetectionStats(
    val tensorMin: Float,
    val tensorMax: Float,
    val tensorMean: Float,
    val rawMaxScore: Float,
    val rawMaxClass: Int,
    val rawMaxClassLabel: String?,
    val rawMaxBoxX: Float,
    val rawMaxBoxY: Float,
    val rawMaxBoxW: Float,
    val rawMaxBoxH: Float,
    val rawMaxTopAnchorClassScores: FloatArray,
    val potholeMaxScore: Float,
    val candidatesAtThreshold: Int
) {
    val rawMaxBox: String
        get() = "[%.1f, %.1f, %.1f, %.1f] (cx, cy, w, h @640)".format(
            rawMaxBoxX, rawMaxBoxY, rawMaxBoxW, rawMaxBoxH
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RawDetectionStats) return false
        if (tensorMin != other.tensorMin) return false
        if (tensorMax != other.tensorMax) return false
        if (tensorMean != other.tensorMean) return false
        if (rawMaxScore != other.rawMaxScore) return false
        if (rawMaxClass != other.rawMaxClass) return false
        if (rawMaxClassLabel != other.rawMaxClassLabel) return false
        if (rawMaxBoxX != other.rawMaxBoxX) return false
        if (rawMaxBoxY != other.rawMaxBoxY) return false
        if (rawMaxBoxW != other.rawMaxBoxW) return false
        if (rawMaxBoxH != other.rawMaxBoxH) return false
        if (!rawMaxTopAnchorClassScores.contentEquals(other.rawMaxTopAnchorClassScores)) return false
        if (potholeMaxScore != other.potholeMaxScore) return false
        if (candidatesAtThreshold != other.candidatesAtThreshold) return false
        return true
    }

    override fun hashCode(): Int {
        var result = tensorMin.hashCode()
        result = 31 * result + tensorMax.hashCode()
        result = 31 * result + tensorMean.hashCode()
        result = 31 * result + rawMaxScore.hashCode()
        result = 31 * result + rawMaxClass
        result = 31 * result + (rawMaxClassLabel?.hashCode() ?: 0)
        result = 31 * result + rawMaxBoxX.hashCode()
        result = 31 * result + rawMaxBoxY.hashCode()
        result = 31 * result + rawMaxBoxW.hashCode()
        result = 31 * result + rawMaxBoxH.hashCode()
        result = 31 * result + rawMaxTopAnchorClassScores.contentHashCode()
        result = 31 * result + potholeMaxScore.hashCode()
        result = 31 * result + candidatesAtThreshold
        return result
    }
}