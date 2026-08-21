package com.roadguardian.app.domain.model

enum class HazardType(
    val classId: Int,
    val code: String,
    val label: String
) {
    LONGITUDINAL_CRACK(0, "D00", "longitudinal_crack"),
    TRANSVERSE_CRACK(1, "D10", "transverse_crack"),
    ALLIGATOR_CRACK(2, "D20", "alligator_crack"),
    POTHOLE(3, "D40", "pothole");

    companion object {
        fun fromClassId(classId: Int): HazardType? = entries.firstOrNull { it.classId == classId }

        fun fromCode(code: String): HazardType? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }

        fun fromLabel(label: String): HazardType? = entries.firstOrNull { it.label.equals(label, ignoreCase = true) }
    }
}
