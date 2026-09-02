package com.roadguardian.app.ai.confirmation

enum class PotholeConfirmationState(
    val displayName: String,
    val isConfirmedOrHigher: Boolean
) {
    NO_DETECTION("No Detection", false),
    CANDIDATE("Candidate", false),
    CONFIRMING("Confirming", false),
    CONFIRMED("Confirmed", true),
    REPORTED("Reported", true),
    COOLDOWN("Cooldown", false);
}
