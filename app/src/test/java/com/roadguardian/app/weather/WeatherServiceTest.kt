package com.roadguardian.app.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WeatherServiceTest {

    @Test
    fun parseOpenMeteoResponse_parsesCorrectly() {
        val sampleJson = """
            {
                "latitude": 26.4499,
                "longitude": 80.3319,
                "current": {
                    "time": "2026-08-22T11:00",
                    "interval": 900,
                    "temperature_2m": 24.3,
                    "relative_humidity_2m": 72,
                    "apparent_temperature": 25.1,
                    "precipitation": 0.2,
                    "weather_code": 51,
                    "wind_speed_10m": 8.5,
                    "visibility": 10000.0,
                    "is_day": 1
                }
            }
        """.trimIndent()

        val result = WeatherService.parseOpenMeteoResponse(sampleJson)

        assertEquals(24, result.temperature)
        assertEquals(25, result.apparentTemperature)
        assertEquals(51, result.weatherCode)
        assertEquals("Drizzle", result.conditionText)
        assertEquals("Good visibility", result.secondaryInfo)
        assertEquals(72, result.humidity)
    }

    @Test
    fun weatherCodeMapper_mapsKnownWmoCodes() {
        val (clearText, _) = WeatherCodeMapper.mapWmoCode(0)
        assertEquals("Clear sky", clearText)

        val (drizzleText, _) = WeatherCodeMapper.mapWmoCode(51)
        assertEquals("Drizzle", drizzleText)

        val (rainText, _) = WeatherCodeMapper.mapWmoCode(61)
        assertEquals("Rain", rainText)

        val (thunderstormText, _) = WeatherCodeMapper.mapWmoCode(95)
        assertEquals("Thunderstorm", thunderstormText)
    }
}
