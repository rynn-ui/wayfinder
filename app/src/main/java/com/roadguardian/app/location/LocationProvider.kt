package com.roadguardian.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val regionName: String
)

object LocationProvider {

    // Sensible fallback coordinates if device is offline or without GPS
    private const val DEFAULT_LAT = 26.4499
    private const val DEFAULT_LON = 80.3319
    private const val DEFAULT_CITY = "Kanpur"
    private const val DEFAULT_REGION = "Uttar Pradesh"

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    suspend fun getBestLocation(context: Context): DeviceLocation = withContext(Dispatchers.IO) {
        // 1. Try system LocationManager (last known)
        val lastKnown = getLastKnownLocation(context)
        if (lastKnown != null) {
            return@withContext lastKnown
        }

        // 2. Try single active GPS/Network update
        if (hasLocationPermission(context)) {
            val freshLocation = withTimeoutOrNull(3000L) {
                requestSingleLocation(context)
            }
            if (freshLocation != null) {
                val (city, region) = reverseGeocode(context, freshLocation.latitude, freshLocation.longitude)
                return@withContext DeviceLocation(
                    latitude = freshLocation.latitude,
                    longitude = freshLocation.longitude,
                    cityName = city,
                    regionName = region
                )
            }
        }

        // 3. Try IP-based Geolocation fallback (works indoors, in emulators, and offline-graceful)
        val ipLocation = withTimeoutOrNull(3000L) {
            fetchIpLocation()
        }
        if (ipLocation != null) {
            return@withContext ipLocation
        }

        // 4. Default fallback so weather always functions
        DeviceLocation(
            latitude = DEFAULT_LAT,
            longitude = DEFAULT_LON,
            cityName = DEFAULT_CITY,
            regionName = DEFAULT_REGION
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(context: Context): DeviceLocation? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) return@withContext null

        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return@withContext null

            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )

            var bestLocation: Location? = null
            for (provider in providers) {
                try {
                    val loc = locationManager.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || loc.time > bestLocation.time) {
                        bestLocation = loc
                    }
                } catch (_: Exception) { }
            }

            if (bestLocation != null) {
                val (city, region) = reverseGeocode(context, bestLocation.latitude, bestLocation.longitude)
                DeviceLocation(
                    latitude = bestLocation.latitude,
                    longitude = bestLocation.longitude,
                    cityName = city,
                    regionName = region
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleLocation(context: Context): Location? = suspendCancellableCoroutine { continuation ->
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager == null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            val provider = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                LocationManager.NETWORK_PROVIDER
            } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                LocationManager.GPS_PROVIDER
            } else {
                null
            }

            if (provider != null) {
                locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                continuation.invokeOnCancellation {
                    locationManager.removeUpdates(listener)
                }
            } else {
                continuation.resume(null)
            }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }

    private suspend fun fetchIpLocation(): DeviceLocation? = withContext(Dispatchers.IO) {
        // Try ip-api.com first (fast & reliable without key)
        val ipApiLoc = tryFetchIpApiCom()
        if (ipApiLoc != null) return@withContext ipApiLoc

        // Try ipapi.co as secondary
        tryFetchIpApiCo()
    }

    private fun tryFetchIpApiCom(): DeviceLocation? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("http://ip-api.com/json/?fields=status,city,regionName,lat,lon")
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.setRequestProperty("User-Agent", "Wayfinder-Android/1.0")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()

                val json = JSONObject(sb.toString())
                if (json.optString("status") == "success") {
                    val lat = json.optDouble("lat", DEFAULT_LAT)
                    val lon = json.optDouble("lon", DEFAULT_LON)
                    val city = json.optString("city", DEFAULT_CITY)
                    val region = json.optString("regionName", DEFAULT_REGION)

                    DeviceLocation(
                        latitude = lat,
                        longitude = lon,
                        cityName = city,
                        regionName = region
                    )
                } else null
            } else null
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun tryFetchIpApiCo(): DeviceLocation? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("https://ipapi.co/json/")
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.setRequestProperty("User-Agent", "Wayfinder-Android/1.0")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()

                val json = JSONObject(sb.toString())
                val lat = json.optDouble("latitude", DEFAULT_LAT)
                val lon = json.optDouble("longitude", DEFAULT_LON)
                val city = json.optString("city", DEFAULT_CITY)
                val region = json.optString("region", DEFAULT_REGION)

                DeviceLocation(
                    latitude = lat,
                    longitude = lon,
                    cityName = city,
                    regionName = region
                )
            } else null
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) {
                return@withContext Pair(DEFAULT_CITY, DEFAULT_REGION)
            }

            val geocoder = Geocoder(context, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                val address = addresses.firstOrNull()
                                val city = address?.locality
                                    ?: address?.subAdminArea
                                    ?: address?.featureName
                                    ?: DEFAULT_CITY
                                val region = address?.adminArea ?: address?.countryName ?: DEFAULT_REGION
                                continuation.resume(Pair(city, region))
                            }

                            override fun onError(errorMessage: String?) {
                                continuation.resume(Pair(DEFAULT_CITY, DEFAULT_REGION))
                            }
                        }
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull()
                val city = address?.locality
                    ?: address?.subAdminArea
                    ?: address?.featureName
                    ?: DEFAULT_CITY
                val region = address?.adminArea ?: address?.countryName ?: DEFAULT_REGION
                Pair(city, region)
            }
        } catch (_: Exception) {
            Pair(DEFAULT_CITY, DEFAULT_REGION)
        }
    }
}
