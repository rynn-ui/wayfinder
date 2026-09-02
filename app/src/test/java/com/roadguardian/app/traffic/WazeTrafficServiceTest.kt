package com.roadguardian.app.traffic

import com.roadguardian.app.traffic.model.WazeJamSeverity
import com.roadguardian.app.traffic.model.WazeTrafficType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WazeTrafficServiceTest {

    private val service = WazeTrafficService()

    @Test
    fun parseWazeResponse_withValidAlertsAndJams_returnsCorrectSummary() {
        val json = """
            {
                "alerts": [
                    {
                        "uuid": "alert-101",
                        "type": "JAM",
                        "subtype": "JAM_HEAVY_TRAFFIC",
                        "street": "Outer Ring Road",
                        "city": "Bengaluru",
                        "reportDescription": "Heavy congestion near flyover",
                        "reliability": 9,
                        "confidence": 8,
                        "nThumbsUp": 5,
                        "pubMillis": 1700000000000,
                        "location": { "x": 77.5946, "y": 12.9716 }
                    },
                    {
                        "uuid": "alert-102",
                        "type": "ACCIDENT",
                        "subtype": "ACCIDENT_MAJOR",
                        "street": "MG Road",
                        "city": "Bengaluru",
                        "reportDescription": "Accident in middle lane",
                        "reliability": 10,
                        "confidence": 9,
                        "nThumbsUp": 12,
                        "pubMillis": 1700000010000,
                        "location": { "x": 77.6000, "y": 12.9750 }
                    }
                ],
                "jams": [
                    {
                        "uuid": "jam-201",
                        "street": "Old Airport Road",
                        "city": "Bengaluru",
                        "level": 3,
                        "speedKMH": 9.5,
                        "delay": 420,
                        "length": 800,
                        "pubMillis": 1700000020000,
                        "line": [
                            { "x": 77.5900, "y": 12.9700 },
                            { "x": 77.5950, "y": 12.9720 }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val summary = service.parseWazeResponse(json, 12.9716, 77.5946)

        assertEquals(3, summary.totalAlertsCount)
        assertEquals(2, summary.jamsCount)
        assertEquals(1, summary.accidentsCount)
        assertEquals(7, summary.maxDelayMinutes)
        assertEquals(WazeJamSeverity.HEAVY, summary.overallSeverity)
        assertNotNull(summary.nearestIncident)

        val firstIncident = summary.incidents.first()
        assertTrue(firstIncident.distanceMetersFromUser < 200.0)
    }

    @Test
    fun parseWazeResponse_withEmptyOrCorruptJson_returnsSimulatedFallback() {
        val summary = service.parseWazeResponse("INVALID_JSON", 26.4499, 80.3319)

        assertTrue(summary.totalAlertsCount > 0)
        assertNotNull(summary.nearestIncident)
        assertTrue(summary.jamsCount >= 1)
    }

    @Test
    fun generateSimulatedTraffic_producesSensibleMetrics() {
        val simulated = service.generateSimulatedTraffic(26.4499, 80.3319)

        assertTrue(simulated.incidents.isNotEmpty())
        assertTrue(simulated.maxDelayMinutes > 0)
        assertTrue(simulated.jamsCount >= 1)
        val jam = simulated.incidents.find { it.type == WazeTrafficType.JAM }
        assertNotNull(jam)
        assertTrue(jam!!.jamPolyline.isNotEmpty())
    }
}
