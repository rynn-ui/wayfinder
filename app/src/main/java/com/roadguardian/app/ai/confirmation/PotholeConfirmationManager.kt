package com.roadguardian.app.ai.confirmation

import com.roadguardian.app.ai.tracking.TrackedDetection
import com.roadguardian.app.domain.model.HazardSeverity

data class ConfirmedPotholeCandidate(
    val id: String,
    val confidence: Float,
    val severity: HazardSeverity,
    val consecutiveCount: Int,
    val timestamp: Long,
    val hasSensorImpact: Boolean
)

data class ConfirmationResult(
    val state: PotholeConfirmationState,
    val targetDetection: TrackedDetection? = null,
    val confidence: Float = 0.0f,
    val severity: HazardSeverity = HazardSeverity.LOW,
    val consecutiveCount: Int = 0,
    val shouldTriggerReport: Boolean = false,
    val statusText: String = ""
)

class PotholeConfirmationManager(
    val confidenceThreshold: Float = 0.25f,
    val minConsecutiveDetections: Int = 2,
    val maxFrameGap: Int = 2,
    val cooldownDurationMillis: Long = 4000L,
    val onPotholeConfirmed: ((ConfirmedPotholeCandidate) -> Unit)? = null
) {

    private var currentState: PotholeConfirmationState = PotholeConfirmationState.NO_DETECTION
    private var lastReportedTime = 0L
    private var activeReportedTrackId: String? = null

    fun reset() {
        currentState = PotholeConfirmationState.NO_DETECTION
        lastReportedTime = 0L
        activeReportedTrackId = null
    }

    @Synchronized
    fun processFrame(
        trackedDetections: List<TrackedDetection>,
        frameNumber: Long,
        timestamp: Long = System.currentTimeMillis(),
        hasSensorImpact: Boolean = false,
        imageArea: Float = 640.0f * 640.0f
    ): ConfirmationResult {
        return process(
            trackedDetections = trackedDetections,
            timestamp = timestamp,
            hasSensorImpact = hasSensorImpact,
            imageArea = imageArea
        )
    }

    @Synchronized
    fun process(
        trackedDetections: List<TrackedDetection>,
        timestamp: Long = System.currentTimeMillis(),
        hasSensorImpact: Boolean = false,
        imageArea: Float = 640.0f * 640.0f
    ): ConfirmationResult {
        if (lastReportedTime > 0L && (timestamp - lastReportedTime) < cooldownDurationMillis) {
            currentState = PotholeConfirmationState.COOLDOWN
            val best = trackedDetections.maxByOrNull { it.confidence }
            return ConfirmationResult(
                state = PotholeConfirmationState.COOLDOWN,
                targetDetection = best,
                confidence = best?.confidence ?: 0.0f,
                severity = HazardSeverity.LOW,
                consecutiveCount = best?.consecutiveHitCount ?: 0,
                shouldTriggerReport = false,
                statusText = "Cooldown active"
            )
        }

        val eligibleTracks = trackedDetections.filter { it.confidence >= confidenceThreshold }

        if (eligibleTracks.isEmpty()) {
            currentState = PotholeConfirmationState.NO_DETECTION
            return ConfirmationResult(
                state = PotholeConfirmationState.NO_DETECTION,
                statusText = "Road clear"
            )
        }

        val bestTrack = eligibleTracks.maxByOrNull { it.consecutiveHitCount * 10.0f + it.confidence } ?: eligibleTracks.first()
        val areaFraction = if (imageArea > 0f) bestTrack.boxArea / imageArea else 0f
        val hits = bestTrack.consecutiveHitCount

        val effectiveHits = if (hasSensorImpact && hits >= 2) hits + 1 else hits

        val isConfirmed = effectiveHits >= minConsecutiveDetections

        val severity = HazardSeverity.estimateSeverity(
            confidence = bestTrack.confidence,
            boxAreaFraction = areaFraction,
            hasSensorImpact = hasSensorImpact,
            confirmationCount = effectiveHits
        )

        return if (isConfirmed) {
            val isNewReport = activeReportedTrackId != bestTrack.id
            if (isNewReport) {
                currentState = PotholeConfirmationState.CONFIRMED
                lastReportedTime = timestamp
                activeReportedTrackId = bestTrack.id

                val candidate = ConfirmedPotholeCandidate(
                    id = bestTrack.id,
                    confidence = bestTrack.confidence,
                    severity = severity,
                    consecutiveCount = hits,
                    timestamp = timestamp,
                    hasSensorImpact = hasSensorImpact
                )
                onPotholeConfirmed?.invoke(candidate)

                ConfirmationResult(
                    state = PotholeConfirmationState.CONFIRMED,
                    targetDetection = bestTrack,
                    confidence = bestTrack.confidence,
                    severity = severity,
                    consecutiveCount = hits,
                    shouldTriggerReport = true,
                    statusText = "Pothole confirmed (${severity.displayName})"
                )
            } else {
                currentState = PotholeConfirmationState.REPORTED
                ConfirmationResult(
                    state = PotholeConfirmationState.REPORTED,
                    targetDetection = bestTrack,
                    confidence = bestTrack.confidence,
                    severity = severity,
                    consecutiveCount = hits,
                    shouldTriggerReport = false,
                    statusText = "Pothole recorded"
                )
            }
        } else if (hits > 1) {
            currentState = PotholeConfirmationState.CONFIRMING
            ConfirmationResult(
                state = PotholeConfirmationState.CONFIRMING,
                targetDetection = bestTrack,
                confidence = bestTrack.confidence,
                severity = severity,
                consecutiveCount = hits,
                shouldTriggerReport = false,
                statusText = "Confirming ($hits/$minConsecutiveDetections)"
            )
        } else {
            currentState = PotholeConfirmationState.CANDIDATE
            ConfirmationResult(
                state = PotholeConfirmationState.CANDIDATE,
                targetDetection = bestTrack,
                confidence = bestTrack.confidence,
                severity = severity,
                consecutiveCount = hits,
                shouldTriggerReport = false,
                statusText = "Candidate detected"
            )
        }
    }
}
