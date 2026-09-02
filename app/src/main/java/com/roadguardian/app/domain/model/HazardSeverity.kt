package com.roadguardian.app.domain.model

enum class HazardSeverity(
    val level: Int,
    val value: String,
    val displayName: String
) {
    LOW(1, "low", "Low"),
    MEDIUM(2, "medium", "Medium"),
    HIGH(3, "high", "High"),
    CRITICAL(4, "critical", "Critical");

    companion object {
        fun fromValue(value: String?): HazardSeverity {
            if (value.isNullOrBlank()) return LOW
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: LOW
        }

        fun fromConfidence(confidence: Float): HazardSeverity = when {
            confidence >= 0.80f -> CRITICAL
            confidence >= 0.65f -> HIGH
            confidence >= 0.45f -> MEDIUM
            else -> LOW
        }

        fun estimateSeverity(
            confidence: Float,
            boxAreaFraction: Float = 0f,
            hasSensorImpact: Boolean = false,
            confirmationCount: Int = 1
        ): HazardSeverity {
            var score = when {
                confidence >= 0.80f -> 3
                confidence >= 0.65f -> 2
                confidence >= 0.45f -> 1
                else -> 0
            }

            if (boxAreaFraction > 0.08f) {
                score += 1
            }

            if (hasSensorImpact) {
                score += 1
            }

            if (confirmationCount >= 3) {
                score = maxOf(score, 2)
            }

            return when {
                score >= 4 -> CRITICAL
                score == 3 -> HIGH
                score == 2 -> MEDIUM
                else -> LOW
            }
        }
    }
}
