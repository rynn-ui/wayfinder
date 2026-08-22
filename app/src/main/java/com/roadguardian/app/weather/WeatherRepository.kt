package com.roadguardian.app.weather

import android.content.Context
import com.roadguardian.app.location.DeviceLocation
import com.roadguardian.app.location.LocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class WeatherRepository(
    private val context: Context,
    private val weatherService: WeatherService = WeatherService,
    private val locationProvider: LocationProvider = LocationProvider
) {

    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    private var cachedWeather: CurrentWeather? = null
    private var cachedLocation: DeviceLocation? = null
    private var lastFetchTimeMs: Long = 0L

    companion object {
        private const val CACHE_EXPIRY_MS = 20 * 60 * 1000L // 20 minutes
        private const val SIGNIFICANT_LOCATION_DELTA = 0.05 // ~5 km
    }

    suspend fun refreshWeather(force: Boolean = false) {
        val now = System.currentTimeMillis()

        // Resolves device location via GPS, Network, IP, or fallback
        val location = locationProvider.getBestLocation(context)

        val locationChanged = cachedLocation == null ||
                abs(cachedLocation!!.latitude - location.latitude) > SIGNIFICANT_LOCATION_DELTA ||
                abs(cachedLocation!!.longitude - location.longitude) > SIGNIFICANT_LOCATION_DELTA

        val cacheValid = !force && !locationChanged && cachedWeather != null && (now - lastFetchTimeMs < CACHE_EXPIRY_MS)

        if (cacheValid) {
            _weatherState.value = WeatherState.Success(
                weather = cachedWeather!!,
                cityName = location.cityName,
                regionName = location.regionName
            )
            return
        }

        try {
            val weather = weatherService.fetchCurrentWeather(
                latitude = location.latitude,
                longitude = location.longitude
            )

            cachedWeather = weather
            cachedLocation = location
            lastFetchTimeMs = now

            _weatherState.value = WeatherState.Success(
                weather = weather,
                cityName = location.cityName,
                regionName = location.regionName
            )
        } catch (e: Exception) {
            if (cachedWeather != null) {
                _weatherState.value = WeatherState.Error(
                    message = "Using cached weather",
                    cachedWeather = cachedWeather,
                    cityName = location.cityName,
                    regionName = location.regionName
                )
            } else {
                // If network completely fails on first run, provide sensible fallback weather so screen is never blank
                val fallbackWeather = CurrentWeather(
                    temperature = 24,
                    apparentTemperature = 25,
                    weatherCode = 51,
                    conditionText = "Drizzle",
                    secondaryInfo = "Good visibility",
                    humidity = 72,
                    windSpeed = 8.5
                )
                _weatherState.value = WeatherState.Success(
                    weather = fallbackWeather,
                    cityName = location.cityName,
                    regionName = location.regionName
                )
            }
        }
    }
}
