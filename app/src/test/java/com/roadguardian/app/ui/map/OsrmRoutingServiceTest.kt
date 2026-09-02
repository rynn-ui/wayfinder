package com.roadguardian.app.ui.map

import com.roadguardian.app.domain.model.HazardSeverity
import com.roadguardian.app.domain.model.HazardType
import com.roadguardian.app.domain.model.RoadHazard
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.osmdroid.util.GeoPoint

class OsrmRoutingServiceTest {

    private lateinit var routingService: OsrmRoutingService

    @Before
    fun setUp() {
        routingService = OsrmRoutingService()
    }

    private fun createHazard(id: String, lat: Double, lon: Double, severity: HazardSeverity): RoadHazard {
        return RoadHazard(
            id = id,
            deviceLatitude = lat,
            deviceLongitude = lon,
            hazardType = HazardType.POTHOLE,
            confidence = 0.85f,
            severity = severity.value,
            timestamp = 1000L
        )
    }

    @Test
    fun calculateRouteHazardScore_emptyRoute_returnsLow() {
        val analysis = routingService.calculateRouteHazardScore(emptyList(), emptyList())
        assertEquals(0, analysis.totalHazardsCount)
        assertEquals(HazardSeverity.LOW, analysis.score)
    }

    @Test
    fun calculateRouteHazardScore_hazardsInCorridor_computesAccurateSummary() {
        val routePoints = listOf(
            GeoPoint(26.4490, 80.3310),
            GeoPoint(26.4500, 80.3320),
            GeoPoint(26.4510, 80.3330)
        )

        val nearHazard1 = createHazard("h1", 26.45001, 80.33201, HazardSeverity.HIGH)
        val nearHazard2 = createHazard("h2", 26.45002, 80.33202, HazardSeverity.HIGH)
        val farHazard = createHazard("h3", 26.4600, 80.3400, HazardSeverity.CRITICAL)

        val hazards = listOf(nearHazard1, nearHazard2, farHazard)

        val analysis = routingService.calculateRouteHazardScore(routePoints, hazards, corridorRadiusMeters = 30.0)

        assertEquals(2, analysis.totalHazardsCount)
        assertEquals(2, analysis.highSeverityCount)
        assertEquals(0, analysis.mediumSeverityCount)
        assertEquals(HazardSeverity.HIGH, analysis.score)
    }
}
