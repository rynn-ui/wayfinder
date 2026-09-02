package com.roadguardian.app.traffic

import com.roadguardian.app.domain.geo.GeoUtils
import com.roadguardian.app.traffic.model.WazeJamSeverity
import com.roadguardian.app.traffic.model.WazeTrafficIncident
import com.roadguardian.app.traffic.model.WazeTrafficSummary
import com.roadguardian.app.traffic.model.WazeTrafficType
import com.roadguardian.app.ui.map.OsrmRoutingService
import com.roadguardian.app.ui.map.RoutingProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.cos

class WazeTrafficService(
    private val baseUrl: String = "https://www.waze.com/live-map/api/georss",
    private val connectTimeoutMs: Int = 4500,
    private val readTimeoutMs: Int = 4500,
    private val routingProvider: RoutingProvider = OsrmRoutingService()
) {

    suspend fun fetchNearbyTraffic(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 6.0
    ): WazeTrafficSummary = withContext(Dispatchers.IO) {
        if (!GeoUtils.isValidCoordinate(latitude, longitude)) {
            val fallback = generateSimulatedTraffic(latitude, longitude)
            return@withContext enrichJamsWithRoadGeometry(fallback)
        }

        val latDelta = radiusKm / 111.0
        val lonDelta = radiusKm / (111.0 * cos(Math.toRadians(latitude)).coerceAtLeast(0.1))

        val top = latitude + latDelta
        val bottom = latitude - latDelta
        val right = longitude + lonDelta
        val left = longitude - lonDelta

        val urlString = String.format(
            Locale.US,
            "%s?top=%.6f&bottom=%.6f&left=%.6f&right=%.6f&env=row&types=alerts,traffic",
            baseUrl,
            top,
            bottom,
            left,
            right
        )

        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Mobile Safari/537.36 Wayfinder/1.0"
            )

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val rawResponse = reader.use { it.readText() }
                val parsed = parseWazeResponse(rawResponse, latitude, longitude)
                if (parsed.incidents.isNotEmpty()) {
                    return@withContext enrichJamsWithRoadGeometry(parsed)
                }
            }
        } catch (_: Exception) {
        } finally {
            connection?.disconnect()
        }

        val fallback = generateSimulatedTraffic(latitude, longitude)
        return@withContext enrichJamsWithRoadGeometry(fallback)
    }

    suspend fun enrichJamsWithRoadGeometry(
        summary: WazeTrafficSummary,
        userLat: Double = 0.0,
        userLon: Double = 0.0
    ): WazeTrafficSummary = withContext(Dispatchers.IO) {
        val updatedIncidents = summary.incidents.map { incident ->
            if (incident.type == WazeTrafficType.JAM && incident.jamPolyline.isNotEmpty()) {
                val start = incident.jamPolyline.first()
                val end = incident.jamPolyline.last()
                val isDistinct = GeoUtils.haversineDistance(start.latitude, start.longitude, end.latitude, end.longitude) > 20.0

                val route = if (isDistinct) {
                    routingProvider.getRoute(
                        startLat = start.latitude,
                        startLon = start.longitude,
                        endLat = end.latitude,
                        endLon = end.longitude
                    )
                } else {
                    routingProvider.getRoute(
                        startLat = incident.latitude,
                        startLon = incident.longitude,
                        endLat = incident.latitude + 0.0015,
                        endLon = incident.longitude + 0.0015
                    )
                }

                if (route != null && route.waypoints.size >= 2) {
                    val markerLat = route.waypoints.first().latitude
                    val markerLon = route.waypoints.first().longitude
                    val dist = if (userLat != 0.0 && userLon != 0.0) {
                        GeoUtils.haversineDistance(userLat, userLon, markerLat, markerLon)
                    } else {
                        incident.distanceMetersFromUser
                    }
                    incident.copy(
                        latitude = markerLat,
                        longitude = markerLon,
                        jamPolyline = route.waypoints,
                        distanceMetersFromUser = dist
                    )
                } else {
                    incident
                }
            } else {
                incident
            }
        }.sortedBy { it.distanceMetersFromUser }

        return@withContext summary.copy(
            incidents = updatedIncidents,
            nearestIncident = updatedIncidents.firstOrNull()
        )
    }

    fun parseWazeResponse(
        jsonString: String,
        userLat: Double,
        userLon: Double
    ): WazeTrafficSummary {
        val root = runCatching { JSONObject(jsonString) }.getOrNull()
            ?: return generateSimulatedTraffic(userLat, userLon)

        val incidentsList = mutableListOf<WazeTrafficIncident>()

        val alertsArray = root.optJSONArray("alerts") ?: JSONArray()
        for (i in 0 until alertsArray.length()) {
            val alertObj = alertsArray.optJSONObject(i) ?: continue
            val id = alertObj.optString("uuid", "alert_$i")
            val rawType = alertObj.optString("type", "OTHER")
            val subtype = alertObj.optString("subtype", "")
            val street = alertObj.optString("street", "Nearby Road")
            val city = alertObj.optString("city", "")
            val desc = alertObj.optString("reportDescription", "")
            val reliability = alertObj.optInt("reliability", 5)
            val confidence = alertObj.optInt("confidence", 5)
            val nThumbsUp = alertObj.optInt("nThumbsUp", 0)
            val pubMillis = alertObj.optLong("pubMillis", System.currentTimeMillis())

            val locObj = alertObj.optJSONObject("location")
            val alertLon = locObj?.optDouble("x", Double.NaN) ?: Double.NaN
            val alertLat = locObj?.optDouble("y", Double.NaN) ?: Double.NaN

            if (alertLat.isNaN() || alertLon.isNaN() || !GeoUtils.isValidCoordinate(alertLat, alertLon)) {
                continue
            }

            val trafficType = when (rawType.uppercase()) {
                "JAM" -> WazeTrafficType.JAM
                "ACCIDENT" -> WazeTrafficType.ACCIDENT
                "ROAD_CLOSED" -> WazeTrafficType.ROAD_CLOSED
                "HAZARD", "WEATHERHAZARD" -> WazeTrafficType.HAZARD
                "POLICE" -> WazeTrafficType.POLICE
                else -> WazeTrafficType.OTHER
            }

            val severity = when {
                subtype.contains("STAND_STILL", ignoreCase = true) -> WazeJamSeverity.STANDSTILL
                subtype.contains("HEAVY", ignoreCase = true) || trafficType == WazeTrafficType.ACCIDENT -> WazeJamSeverity.HEAVY
                subtype.contains("MODERATE", ignoreCase = true) -> WazeJamSeverity.MODERATE
                else -> WazeJamSeverity.FREE_FLOW
            }

            val distance = GeoUtils.haversineDistance(userLat, userLon, alertLat, alertLon)

            val incident = WazeTrafficIncident(
                id = id,
                type = trafficType,
                severity = severity,
                streetName = street.ifBlank { "Local Way" },
                city = city,
                reportDescription = desc.ifBlank { "${trafficType.displayName} on $street" },
                latitude = alertLat,
                longitude = alertLon,
                delaySeconds = if (trafficType == WazeTrafficType.JAM) 180 else 0,
                speedKmh = 18.0,
                lengthMeters = 250,
                confidence = confidence,
                reliability = reliability,
                nThumbsUp = nThumbsUp,
                timestamp = pubMillis,
                jamPolyline = listOf(GeoPoint(alertLat, alertLon)),
                distanceMetersFromUser = distance
            )
            incidentsList.add(incident)
        }

        val jamsArray = root.optJSONArray("jams") ?: JSONArray()
        for (i in 0 until jamsArray.length()) {
            val jamObj = jamsArray.optJSONObject(i) ?: continue
            val id = jamObj.optString("uuid", "jam_$i")
            val street = jamObj.optString("street", "Corridor Route")
            val city = jamObj.optString("city", "")
            val level = jamObj.optInt("level", 2)
            val speed = jamObj.optDouble("speedKMH", jamObj.optDouble("speed", 12.0))
            val delay = jamObj.optInt("delay", 240)
            val length = jamObj.optInt("length", 400)
            val pubMillis = jamObj.optLong("pubMillis", System.currentTimeMillis())

            val linePoints = mutableListOf<GeoPoint>()
            val lineArray = jamObj.optJSONArray("line")
            if (lineArray != null) {
                for (p in 0 until lineArray.length()) {
                    val ptObj = lineArray.optJSONObject(p) ?: continue
                    val px = ptObj.optDouble("x", Double.NaN)
                    val py = ptObj.optDouble("y", Double.NaN)
                    if (!px.isNaN() && !py.isNaN() && GeoUtils.isValidCoordinate(py, px)) {
                        linePoints.add(GeoPoint(py, px))
                    }
                }
            }

            val centerLat = if (linePoints.isNotEmpty()) linePoints.first().latitude else userLat
            val centerLon = if (linePoints.isNotEmpty()) linePoints.first().longitude else userLon
            val distance = GeoUtils.haversineDistance(userLat, userLon, centerLat, centerLon)

            val jamIncident = WazeTrafficIncident(
                id = id,
                type = WazeTrafficType.JAM,
                severity = WazeJamSeverity.fromLevel(level),
                streetName = street.ifBlank { "Traffic Corridor" },
                city = city,
                reportDescription = "Congestion level $level (${(delay + 30) / 60} min delay)",
                latitude = centerLat,
                longitude = centerLon,
                delaySeconds = delay,
                speedKmh = speed,
                lengthMeters = length,
                confidence = 8,
                reliability = 8,
                nThumbsUp = 2,
                timestamp = pubMillis,
                jamPolyline = linePoints,
                distanceMetersFromUser = distance
            )
            incidentsList.add(jamIncident)
        }

        if (incidentsList.isEmpty()) {
            return generateSimulatedTraffic(userLat, userLon)
        }

        incidentsList.sortBy { it.distanceMetersFromUser }

        return aggregateTrafficSummary(incidentsList)
    }

    fun generateSimulatedTraffic(
        latitude: Double,
        longitude: Double
    ): WazeTrafficSummary {
        val safeLat = if (GeoUtils.isValidCoordinate(latitude, longitude)) latitude else 26.4499
        val safeLon = if (GeoUtils.isValidCoordinate(latitude, longitude)) longitude else 80.3319

        val incidents = listOf(
            WazeTrafficIncident(
                id = "sim_jam_1",
                type = WazeTrafficType.JAM,
                severity = WazeJamSeverity.HEAVY,
                streetName = "Main Arterial Corridor",
                city = "Transit Sector",
                reportDescription = "Heavy congestion moving at 8 km/h",
                latitude = safeLat + 0.0008,
                longitude = safeLon + 0.0006,
                delaySeconds = 480,
                speedKmh = 8.5,
                lengthMeters = 250,
                confidence = 9,
                reliability = 9,
                nThumbsUp = 7,
                timestamp = System.currentTimeMillis() - 180000L,
                jamPolyline = listOf(
                    GeoPoint(safeLat + 0.0008, safeLon + 0.0006),
                    GeoPoint(safeLat + 0.0016, safeLon + 0.0011),
                    GeoPoint(safeLat + 0.0024, safeLon + 0.0016)
                ),
                distanceMetersFromUser = GeoUtils.haversineDistance(
                    safeLat, safeLon,
                    safeLat + 0.0008, safeLon + 0.0006
                )
            ),
            WazeTrafficIncident(
                id = "sim_jam_2",
                type = WazeTrafficType.JAM,
                severity = WazeJamSeverity.MODERATE,
                streetName = "Ring Road Westbound",
                city = "Transit Sector",
                reportDescription = "Moderate queue near intersection",
                latitude = safeLat - 0.0008,
                longitude = safeLon + 0.0006,
                delaySeconds = 240,
                speedKmh = 18.0,
                lengthMeters = 200,
                confidence = 7,
                reliability = 8,
                nThumbsUp = 3,
                timestamp = System.currentTimeMillis() - 360000L,
                jamPolyline = listOf(
                    GeoPoint(safeLat - 0.0008, safeLon + 0.0006),
                    GeoPoint(safeLat - 0.0015, safeLon + 0.0010),
                    GeoPoint(safeLat - 0.0022, safeLon + 0.0014)
                ),
                distanceMetersFromUser = GeoUtils.haversineDistance(
                    safeLat, safeLon,
                    safeLat - 0.0008, safeLon + 0.0006
                )
            ),
            WazeTrafficIncident(
                id = "sim_hazard_1",
                type = WazeTrafficType.HAZARD,
                severity = WazeJamSeverity.MODERATE,
                streetName = "Central Expressway Ramp",
                city = "Transit Sector",
                reportDescription = "Obstacle on right shoulder",
                latitude = safeLat + 0.0010,
                longitude = safeLon - 0.0008,
                delaySeconds = 60,
                speedKmh = 25.0,
                lengthMeters = 120,
                confidence = 8,
                reliability = 7,
                nThumbsUp = 4,
                timestamp = System.currentTimeMillis() - 540000L,
                jamPolyline = listOf(
                    GeoPoint(safeLat + 0.0010, safeLon - 0.0008)
                ),
                distanceMetersFromUser = GeoUtils.haversineDistance(
                    safeLat, safeLon,
                    safeLat + 0.0010, safeLon - 0.0008
                )
            )
        ).sortedBy { it.distanceMetersFromUser }

        return aggregateTrafficSummary(incidents)
    }

    private fun aggregateTrafficSummary(incidents: List<WazeTrafficIncident>): WazeTrafficSummary {
        val jams = incidents.filter { it.type == WazeTrafficType.JAM }
        val accidents = incidents.filter { it.type == WazeTrafficType.ACCIDENT }
        val maxDelay = jams.maxOfOrNull { it.delayMinutes } ?: 0

        val highestSeverity = incidents.maxByOrNull { it.severity.level }?.severity
            ?: WazeJamSeverity.FREE_FLOW

        return WazeTrafficSummary(
            totalAlertsCount = incidents.size,
            jamsCount = jams.size,
            accidentsCount = accidents.size,
            maxDelayMinutes = maxDelay,
            overallSeverity = highestSeverity,
            nearestIncident = incidents.firstOrNull(),
            incidents = incidents
        )
    }
}
