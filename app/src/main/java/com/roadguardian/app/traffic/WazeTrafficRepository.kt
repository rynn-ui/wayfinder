package com.roadguardian.app.traffic

import com.roadguardian.app.domain.geo.GeoUtils
import com.roadguardian.app.traffic.model.WazeTrafficState
import com.roadguardian.app.traffic.model.WazeTrafficSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WazeTrafficRepository(
    private val trafficService: WazeTrafficService = WazeTrafficService(),
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _trafficState = MutableStateFlow<WazeTrafficState>(WazeTrafficState.Loading)
    val trafficState: StateFlow<WazeTrafficState> = _trafficState.asStateFlow()

    private val _isTrafficLayerVisible = MutableStateFlow(true)
    val isTrafficLayerVisible: StateFlow<Boolean> = _isTrafficLayerVisible.asStateFlow()

    private var lastQueryLat = 0.0
    private var lastQueryLon = 0.0
    private var lastFetchTimestamp = 0L

    fun toggleTrafficLayer() {
        _isTrafficLayerVisible.value = !_isTrafficLayerVisible.value
    }

    fun setTrafficLayerVisible(visible: Boolean) {
        _isTrafficLayerVisible.value = visible
    }

    fun refreshTraffic(
        latitude: Double,
        longitude: Double,
        force: Boolean = false
    ): Job {
        if (!GeoUtils.isValidCoordinate(latitude, longitude)) {
            val fallback = trafficService.generateSimulatedTraffic(26.4499, 80.3319)
            _trafficState.value = WazeTrafficState.Success(fallback)
            return Job().apply { complete() }
        }

        val currentTime = System.currentTimeMillis()
        val distMoved = if (lastQueryLat != 0.0 && lastQueryLon != 0.0) {
            GeoUtils.haversineDistance(latitude, longitude, lastQueryLat, lastQueryLon)
        } else {
            Double.MAX_VALUE
        }

        if (!force && distMoved < 300.0 && (currentTime - lastFetchTimestamp < 45000L)) {
            return Job().apply { complete() }
        }

        return coroutineScope.launch {
            if (_trafficState.value !is WazeTrafficState.Success) {
                _trafficState.value = WazeTrafficState.Loading
            }

            try {
                val summary = trafficService.fetchNearbyTraffic(latitude, longitude)
                lastQueryLat = latitude
                lastQueryLon = longitude
                lastFetchTimestamp = currentTime
                _trafficState.value = WazeTrafficState.Success(summary)
            } catch (e: Exception) {
                val simulated = trafficService.generateSimulatedTraffic(latitude, longitude)
                _trafficState.value = WazeTrafficState.Error(
                    message = e.message ?: "Failed to fetch traffic",
                    fallbackSummary = simulated
                )
            }
        }
    }

    fun getLatestSummary(): WazeTrafficSummary? {
        return when (val state = _trafficState.value) {
            is WazeTrafficState.Success -> state.summary
            is WazeTrafficState.Error -> state.fallbackSummary
            else -> null
        }
    }
}
