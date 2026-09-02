package com.roadguardian.app.ui.map

import com.roadguardian.app.domain.geo.GeoUtils
import com.roadguardian.app.domain.model.HazardSeverity
import com.roadguardian.app.domain.model.RoadHazard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class RouteResult(
    val waypoints: List<GeoPoint>,
    val distanceMeters: Double,
    val durationSeconds: Double
)

data class RouteHazardAnalysis(
    val totalHazardsCount: Int,
    val highSeverityCount: Int,
    val mediumSeverityCount: Int,
    val lowSeverityCount: Int,
    val score: HazardSeverity
) {
    val summaryText: String
        get() = when (score) {
            HazardSeverity.CRITICAL, HazardSeverity.HIGH -> "Hazard: HIGH ($highSeverityCount critical)"
            HazardSeverity.MEDIUM -> "Hazard: MEDIUM ($totalHazardsCount potholes)"
            HazardSeverity.LOW -> if (totalHazardsCount == 0) "Hazard: CLEAR" else "Hazard: LOW ($totalHazardsCount potholes)"
        }
}

interface RoutingProvider {
    suspend fun getRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double
    ): RouteResult?

    fun calculateRouteHazardScore(
        routePoints: List<GeoPoint>,
        hazards: List<RoadHazard>,
        corridorRadiusMeters: Double = 25.0
    ): RouteHazardAnalysis
}

class OsrmRoutingService(
    private val baseUrl: String = "https://router.project-osrm.org/route/v1/driving",
    private val userAgent: String = "WayFinder-Android/1.0"
) : RoutingProvider {

    private var cachedStartLat = 0.0
    private var cachedStartLon = 0.0
    private var cachedEndLat = 0.0
    private var cachedEndLon = 0.0
    private var cachedResult: RouteResult? = null

    override suspend fun getRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double
    ): RouteResult? = withContext(Dispatchers.IO) {
        if (!GeoUtils.isValidCoordinate(startLat, startLon) || !GeoUtils.isValidCoordinate(endLat, endLon)) {
            return@withContext null
        }

        val dStart = GeoUtils.haversineDistance(startLat, startLon, cachedStartLat, cachedStartLon)
        val dEnd = GeoUtils.haversineDistance(endLat, endLon, cachedEndLat, cachedEndLon)
        if (cachedResult != null && dStart < 30.0 && dEnd < 30.0) {
            return@withContext cachedResult
        }

        var connection: HttpURLConnection? = null
        try {
            val urlString = String.format(
                Locale.US,
                "%s/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
                baseUrl,
                startLon,
                startLat,
                endLon,
                endLat
            )

            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.setRequestProperty("User-Agent", userAgent)

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.use { it.readText() }
            val json = JSONObject(response)

            if (json.optString("code") != "Ok") {
                return@withContext null
            }

            val routes = json.optJSONArray("routes") ?: return@withContext null
            if (routes.length() == 0) return@withContext null

            val firstRoute = routes.getJSONObject(0)
            val distance = firstRoute.optDouble("distance", 0.0)
            val duration = firstRoute.optDouble("duration", 0.0)

            val geometry = firstRoute.optJSONObject("geometry") ?: return@withContext null
            val coordinates = geometry.optJSONArray("coordinates") ?: return@withContext null

            val waypoints = mutableListOf<GeoPoint>()
            for (i in 0 until coordinates.length()) {
                val coord = coordinates.getJSONArray(i)
                val lon = coord.getDouble(0)
                val lat = coord.getDouble(1)
                waypoints.add(GeoPoint(lat, lon))
            }

            val result = RouteResult(
                waypoints = waypoints,
                distanceMeters = distance,
                durationSeconds = duration
            )

            cachedStartLat = startLat
            cachedStartLon = startLon
            cachedEndLat = endLat
            cachedEndLon = endLon
            cachedResult = result

            result
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    override fun calculateRouteHazardScore(
        routePoints: List<GeoPoint>,
        hazards: List<RoadHazard>,
        corridorRadiusMeters: Double
    ): RouteHazardAnalysis {
        if (routePoints.isEmpty() || hazards.isEmpty()) {
            return RouteHazardAnalysis(0, 0, 0, 0, HazardSeverity.LOW)
        }

        var totalHazards = 0
        var highCount = 0
        var mediumCount = 0
        var lowCount = 0

        for (hazard in hazards) {
            var isNearRoute = false
            for (point in routePoints) {
                val dist = GeoUtils.haversineDistance(
                    hazard.deviceLatitude,
                    hazard.deviceLongitude,
                    point.latitude,
                    point.longitude
                )
                if (dist <= corridorRadiusMeters) {
                    isNearRoute = true
                    break
                }
            }

            if (isNearRoute) {
                totalHazards++
                when (hazard.hazardSeverity) {
                    HazardSeverity.CRITICAL, HazardSeverity.HIGH -> highCount++
                    HazardSeverity.MEDIUM -> mediumCount++
                    HazardSeverity.LOW -> lowCount++
                }
            }
        }

        val score = when {
            highCount >= 2 || totalHazards >= 6 -> HazardSeverity.HIGH
            highCount == 1 || totalHazards >= 2 -> HazardSeverity.MEDIUM
            else -> HazardSeverity.LOW
        }

        return RouteHazardAnalysis(
            totalHazardsCount = totalHazards,
            highSeverityCount = highCount,
            mediumSeverityCount = mediumCount,
            lowSeverityCount = lowCount,
            score = score
        )
    }
}
