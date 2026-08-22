package com.roadguardian.app.ai.inference

import com.roadguardian.app.domain.model.RoadHazardDetection
import java.util.ArrayDeque

/**
 * Snapshot of real-time and cumulative AI performance metrics for developer A/B benchmarking.
 */
data class AiBenchmarkSnapshot(
    val activeModel: AiModelType = AiModelType.INT8,
    val isLoading: Boolean = false,
    val isWarmingUp: Boolean = false,
    val warmupRemaining: Int = 0,
    val lastLatencyMs: Float = 0f,
    val rollingAvgLatencyMs: Float = 0f,
    val inferenceFps: Float = 0f,
    val currentDetectionsCount: Int = 0,
    val currentAvgConfidence: Float = 0f,
    val currentMaxConfidence: Float = 0f,
    // Cumulative A/B Session Metrics (excluding warmup)
    val framesProcessed: Long = 0L,
    val framesWithDetection: Long = 0L,
    val totalDetections: Long = 0L,
    val sessionAvgConfidence: Float = 0f,
    val sessionMaxConfidence: Float = 0f,
    val sessionAvgLatencyMs: Float = 0f
)

/**
 * Thread-safe benchmark tracker for measuring actual inference throughput,
 * latencies, detection rates, and confidences per active model.
 */
class AiBenchmarkTracker(
    initialModel: AiModelType = AiModelType.INT8,
    private val warmupTargetFrames: Int = 15,
    private val rollingWindowSize: Int = 20
) {
    @Volatile
    var activeModel: AiModelType = initialModel
        private set

    @Volatile
    var isLoading: Boolean = false
        private set

    private val lock = Any()

    private var warmupRemaining: Int = warmupTargetFrames
    private val recentLatencies = ArrayDeque<Float>(rollingWindowSize)
    private var lastLatencyMs: Float = 0f
    private var currentDetectionsCount: Int = 0
    private var currentAvgConfidence: Float = 0f
    private var currentMaxConfidence: Float = 0f

    // Cumulative stats for active model
    private var framesProcessed: Long = 0L
    private var framesWithDetection: Long = 0L
    private var totalDetections: Long = 0L
    private var sumConfidence: Double = 0.0
    private var maxConfidence: Float = 0f
    private var sumLatencyMs: Double = 0.0

    fun startModelSwitch(newModel: AiModelType) {
        synchronized(lock) {
            activeModel = newModel
            isLoading = true
            resetMetricsLocked()
        }
    }

    fun completeModelSwitch() {
        synchronized(lock) {
            isLoading = false
            warmupRemaining = warmupTargetFrames
        }
    }

    fun recordInference(
        latencyMs: Float,
        detections: List<RoadHazardDetection>
    ): AiBenchmarkSnapshot {
        synchronized(lock) {
            lastLatencyMs = latencyMs
            currentDetectionsCount = detections.size
            if (detections.isNotEmpty()) {
                var maxC = 0f
                var sumC = 0.0
                for (d in detections) {
                    sumC += d.confidence
                    if (d.confidence > maxC) maxC = d.confidence
                }
                currentAvgConfidence = (sumC / detections.size).toFloat()
                currentMaxConfidence = maxC
            } else {
                currentAvgConfidence = 0f
                currentMaxConfidence = 0f
            }

            // Warmup filter: avoid skewing rolling/session averages during initial warmup frames
            if (warmupRemaining > 0) {
                warmupRemaining--
                return createSnapshotLocked()
            }

            // Update rolling latency window
            if (recentLatencies.size >= rollingWindowSize) {
                recentLatencies.removeFirst()
            }
            recentLatencies.addLast(latencyMs)

            // Update session cumulative stats
            framesProcessed++
            if (detections.isNotEmpty()) {
                framesWithDetection++
                totalDetections += detections.size
                for (d in detections) {
                    sumConfidence += d.confidence
                    if (d.confidence > maxConfidence) {
                        maxConfidence = d.confidence
                    }
                }
            }
            sumLatencyMs += latencyMs

            return createSnapshotLocked()
        }
    }

    fun getSnapshot(): AiBenchmarkSnapshot {
        synchronized(lock) {
            return createSnapshotLocked()
        }
    }

    fun resetSessionMetrics() {
        synchronized(lock) {
            resetMetricsLocked()
            warmupRemaining = warmupTargetFrames
        }
    }

    private fun resetMetricsLocked() {
        warmupRemaining = warmupTargetFrames
        recentLatencies.clear()
        lastLatencyMs = 0f
        currentDetectionsCount = 0
        currentAvgConfidence = 0f
        currentMaxConfidence = 0f
        framesProcessed = 0L
        framesWithDetection = 0L
        totalDetections = 0L
        sumConfidence = 0.0
        maxConfidence = 0f
        sumLatencyMs = 0.0
    }

    private fun createSnapshotLocked(): AiBenchmarkSnapshot {
        val rollingAvg = if (recentLatencies.isNotEmpty()) {
            recentLatencies.sum() / recentLatencies.size
        } else {
            lastLatencyMs
        }

        val fps = if (rollingAvg > 0f) {
            1000f / rollingAvg
        } else {
            0f
        }

        val sessionAvgConf = if (totalDetections > 0L) {
            (sumConfidence / totalDetections).toFloat()
        } else {
            0f
        }

        val sessionAvgLat = if (framesProcessed > 0L) {
            (sumLatencyMs / framesProcessed).toFloat()
        } else {
            0f
        }

        return AiBenchmarkSnapshot(
            activeModel = activeModel,
            isLoading = isLoading,
            isWarmingUp = warmupRemaining > 0,
            warmupRemaining = warmupRemaining,
            lastLatencyMs = lastLatencyMs,
            rollingAvgLatencyMs = rollingAvg,
            inferenceFps = fps,
            currentDetectionsCount = currentDetectionsCount,
            currentAvgConfidence = currentAvgConfidence,
            currentMaxConfidence = currentMaxConfidence,
            framesProcessed = framesProcessed,
            framesWithDetection = framesWithDetection,
            totalDetections = totalDetections,
            sessionAvgConfidence = sessionAvgConf,
            sessionMaxConfidence = maxConfidence,
            sessionAvgLatencyMs = sessionAvgLat
        )
    }
}
