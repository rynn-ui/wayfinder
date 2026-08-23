package com.roadguardian.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WayfinderLocationManager(
    private val context: Context
) {

    companion object {
        private const val TAG = "WayfinderLocation"
    }

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val systemLocationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    @Volatile
    private var isUpdating = false

    @Volatile
    private var currentModeHighFrequency = false

    private var locationCallback: LocationCallback? = null
    private var fallbackListener: LocationListener? = null

    fun hasPermission(): Boolean {
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

    @Synchronized
    fun startHighFrequencyUpdates() {
        if (isUpdating && currentModeHighFrequency) return
        stopUpdates()
        currentModeHighFrequency = true
        startFusedUpdates(
            intervalMs = 2000L,
            minIntervalMs = 1000L,
            priority = Priority.PRIORITY_HIGH_ACCURACY
        )
    }

    @Synchronized
    fun startLowFrequencyUpdates() {
        if (isUpdating && !currentModeHighFrequency) return
        stopUpdates()
        currentModeHighFrequency = false
        startFusedUpdates(
            intervalMs = 8000L,
            minIntervalMs = 4000L,
            priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY
        )
    }

    @SuppressLint("MissingPermission")
    private fun startFusedUpdates(
        intervalMs: Long,
        minIntervalMs: Long,
        priority: Int
    ) {
        if (!hasPermission()) {
            _locationState.value = _locationState.value.copy(isAvailable = false)
            return
        }

        try {
            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null) {
                    updateFromLocation(lastLoc)
                }
            }

            val request = LocationRequest.Builder(priority, intervalMs)
                .setMinUpdateIntervalMillis(minIntervalMs)
                .setWaitForAccurateLocation(false)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val last = result.lastLocation ?: return
                    updateFromLocation(last)
                }
            }
            locationCallback = callback

            fusedClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            ).addOnFailureListener { error ->
                Log.w(TAG, "Fused location request failed, using system fallback: ${error.message}")
                startSystemLocationFallback(intervalMs, priority)
            }

            isUpdating = true
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting location updates", e)
            _locationState.value = _locationState.value.copy(isAvailable = false)
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting location updates", e)
            startSystemLocationFallback(intervalMs, priority)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startSystemLocationFallback(intervalMs: Long, priority: Int) {
        if (!hasPermission() || systemLocationManager == null) return
        try {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    updateFromLocation(location)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            fallbackListener = listener

            val provider = if (priority == Priority.PRIORITY_HIGH_ACCURACY &&
                systemLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            ) {
                LocationManager.GPS_PROVIDER
            } else if (systemLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                LocationManager.NETWORK_PROVIDER
            } else if (systemLocationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                LocationManager.PASSIVE_PROVIDER
            } else {
                null
            }

            if (provider != null) {
                systemLocationManager.requestLocationUpdates(
                    provider,
                    intervalMs,
                    1.0f,
                    listener,
                    Looper.getMainLooper()
                )
                isUpdating = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "System location fallback failed", e)
        }
    }

    @Synchronized
    fun stopUpdates() {
        if (!isUpdating && locationCallback == null && fallbackListener == null) return

        locationCallback?.let {
            fusedClient.removeLocationUpdates(it)
            locationCallback = null
        }

        fallbackListener?.let {
            systemLocationManager?.removeUpdates(it)
            fallbackListener = null
        }

        isUpdating = false
    }

    private fun updateFromLocation(location: Location) {
        _locationState.value = LocationState(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = if (location.hasAccuracy()) location.accuracy else null,
            speedMps = if (location.hasSpeed()) location.speed else null,
            bearing = if (location.hasBearing()) location.bearing else null,
            altitude = if (location.hasAltitude()) location.altitude else null,
            timestamp = location.time,
            isAvailable = true
        )
    }
}
