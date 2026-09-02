package com.roadguardian.app.domain.model

enum class PotholeStatus(
    val value: String,
    val displayName: String
) {
    ACTIVE("ACTIVE", "Active"),
    UNCERTAIN("UNCERTAIN", "Uncertain"),
    RESOLVED("RESOLVED", "Resolved");

    companion object {
        fun fromValue(value: String?): PotholeStatus {
            if (value.isNullOrBlank()) return ACTIVE
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: ACTIVE
        }
    }
}
