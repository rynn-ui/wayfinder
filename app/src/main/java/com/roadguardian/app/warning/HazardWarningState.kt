package com.roadguardian.app.warning

import com.roadguardian.app.domain.model.HazardSeverity
import com.roadguardian.app.domain.model.RoadHazard

data class HazardWarningState(
    val isWarningActive: Boolean = false,
    val hazard: RoadHazard? = null,
    val distanceMeters: Int = 0,
    val severity: HazardSeverity = HazardSeverity.LOW,
    val isAhead: Boolean = false,
    val warningText: String = ""
)
