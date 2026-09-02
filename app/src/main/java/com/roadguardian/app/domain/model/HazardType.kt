package com.roadguardian.app.domain.model

enum class HazardType(
    val classId: Int,
    val code: String,
    val label: String
) {
    POTHOLE(0, "D40", "pothole"),
    LONGITUDINAL_CRACK(1, "D00", "longitudinal_crack"),
    TRANSVERSE_CRACK(2, "D10", "transverse_crack");

    companion object {
        fun fromClassId(classId: Int): HazardType? = when (classId) {
            0, 3 -> POTHOLE
            1 -> LONGITUDINAL_CRACK
            2 -> TRANSVERSE_CRACK
            else -> null
        }

        fun fromCode(code: String): HazardType? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }

        fun fromLabel(label: String): HazardType? = entries.firstOrNull { it.label.equals(label, ignoreCase = true) }
    }
}

