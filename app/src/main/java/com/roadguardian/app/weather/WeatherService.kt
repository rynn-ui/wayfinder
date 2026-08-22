package com.roadguardian.app.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

object WeatherService {

    private const val CONNECT_TIMEOUT_MS = 6000
    private const val READ_TIMEOUT_MS = 6000

    suspend fun fetchCurrentWeather(
        latitude: Double,
        longitude: Double
    ): CurrentWeather = withContext(Dispatchers.IO) {
        val urlString = String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m,visibility,is_day&timezone=auto",
            latitude,
            longitude
        )

        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "Wayfinder-Android/1.0")

        try {
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Weather API returned status $responseCode")
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()

            parseOpenMeteoResponse(response.toString())
        } finally {
            connection.disconnect()
        }
    }

    fun parseOpenMeteoResponse(jsonString: String): CurrentWeather {
        val root = JSONObject(jsonString)
        val current = root.getJSONObject("current")

        val temp = current.optDouble("temperature_2m", 25.0).roundToInt()
        val apparentTemp = current.optDouble("apparent_temperature", temp.toDouble()).roundToInt()
        val weatherCode = current.optInt("weather_code", 0)
        val humidity = current.optInt("relative_humidity_2m", 60)
        val windSpeed = current.optDouble("wind_speed_10m", 0.0)
        val visibility = current.optDouble("visibility", 10000.0)
        val isDay = current.optInt("is_day", 1) == 1

        val (conditionText, _) = WeatherCodeMapper.mapWmoCode(weatherCode)

        val secondaryInfo = when {
            visibility >= 10000 -> "Good visibility"
            visibility >= 5000 -> "Moderate visibility"
            visibility > 0 -> "Low visibility (${(visibility / 1000).roundToInt()} km)"
            windSpeed > 25.0 -> "Windy (${windSpeed.roundToInt()} km/h)"
            humidity > 80 -> "High humidity ($humidity%)"
            else -> "Feels like $apparentTemp°C"
        }

        return CurrentWeather(
            temperature = temp,
            apparentTemperature = apparentTemp,
            weatherCode = weatherCode,
            conditionText = conditionText,
            secondaryInfo = secondaryInfo,
            humidity = humidity,
            windSpeed = windSpeed,
            isDay = isDay
        )
    }
}
