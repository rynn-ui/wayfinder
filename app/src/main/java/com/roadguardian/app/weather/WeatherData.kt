package com.roadguardian.app.weather

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

data class CurrentWeather(
    val temperature: Int,
    val apparentTemperature: Int,
    val weatherCode: Int,
    val conditionText: String,
    val secondaryInfo: String,
    val humidity: Int,
    val windSpeed: Double,
    val isDay: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface WeatherState {
    object Loading : WeatherState
    data class Success(val weather: CurrentWeather, val cityName: String, val regionName: String) : WeatherState
    data class LocationUnavailable(val message: String = "Location unavailable") : WeatherState
    data class Error(val message: String, val cachedWeather: CurrentWeather? = null, val cityName: String? = null, val regionName: String? = null) : WeatherState
}

object WeatherCodeMapper {

    fun mapWmoCode(code: Int): Pair<String, ImageVector> {
        return when (code) {
            0 -> Pair("Clear sky", Icons.Filled.WbSunny)
            1 -> Pair("Mainly clear", Icons.Filled.WbSunny)
            2 -> Pair("Partly cloudy", Icons.Filled.Cloud)
            3 -> Pair("Overcast", Icons.Filled.Cloud)
            45, 48 -> Pair("Foggy", Icons.Filled.Cloud)
            51, 53, 55 -> Pair("Drizzle", Icons.Filled.WaterDrop)
            56, 57 -> Pair("Freezing drizzle", Icons.Filled.AcUnit)
            61, 63, 65 -> Pair("Rain", Icons.Filled.WaterDrop)
            66, 67 -> Pair("Freezing rain", Icons.Filled.AcUnit)
            71, 73, 75, 77 -> Pair("Snow", Icons.Filled.AcUnit)
            80, 81, 82 -> Pair("Rain showers", Icons.Filled.WaterDrop)
            85, 86 -> Pair("Snow showers", Icons.Filled.AcUnit)
            95, 96, 99 -> Pair("Thunderstorm", Icons.Filled.Thunderstorm)
            else -> Pair("Fair", Icons.Filled.Cloud)
        }
    }
}
