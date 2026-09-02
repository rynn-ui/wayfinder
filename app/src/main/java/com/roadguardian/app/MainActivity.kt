package com.roadguardian.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.firebase.FirebaseApp
import com.roadguardian.app.ai.config.DetectionConfig
import com.roadguardian.app.ai.confirmation.ConfirmationResult
import com.roadguardian.app.ai.confirmation.PotholeConfirmationManager
import com.roadguardian.app.ai.inference.AiBenchmarkTracker
import com.roadguardian.app.ai.inference.AiModelType
import com.roadguardian.app.ai.inference.DetectorModelSignature
import com.roadguardian.app.ai.inference.RoadHazardDetector
import com.roadguardian.app.ai.postprocessing.RawDetectionStats
import com.roadguardian.app.ai.tracking.SpatialTemporalTracker
import com.roadguardian.app.auth.FirebaseAuthManager
import com.roadguardian.app.camera.FrameMetadata
import com.roadguardian.app.camera.RoadFrameAnalyzer
import com.roadguardian.app.data.repository.FirestoreRoadHazardRepository
import com.roadguardian.app.data.repository.RoadHazardRepository
import com.roadguardian.app.domain.model.HazardType
import com.roadguardian.app.domain.model.RoadHazard
import com.roadguardian.app.domain.model.RoadHazardDetection
import com.roadguardian.app.location.LocationProvider
import com.roadguardian.app.location.WayfinderLocationManager
import com.roadguardian.app.sensors.WayfinderSensorManager
import com.roadguardian.app.traffic.WazeTrafficRepository
import com.roadguardian.app.traffic.model.WazeTrafficState
import com.roadguardian.app.ui.components.GlassButton
import com.roadguardian.app.ui.components.GlassCard
import com.roadguardian.app.ui.components.NatureBackground
import com.roadguardian.app.ui.components.WayfinderBottomNav
import com.roadguardian.app.ui.components.WayfinderTopAppBar
import com.roadguardian.app.ui.navigation.WayfinderScreen
import com.roadguardian.app.ui.screens.HistoryScreen
import com.roadguardian.app.ui.screens.HomeScreen
import com.roadguardian.app.ui.screens.LiveMonitoringScreen
import com.roadguardian.app.ui.screens.RoadHealthMapScreen
import com.roadguardian.app.ui.screens.SettingsScreen
import com.roadguardian.app.ui.theme.AIRoadGuardianTheme
import com.roadguardian.app.ui.theme.WayfinderDarkBackground
import com.roadguardian.app.ui.theme.WayfinderPrimaryGreen
import com.roadguardian.app.ui.theme.WayfinderTextPrimary
import com.roadguardian.app.ui.theme.WayfinderTextSecondary
import com.roadguardian.app.warning.AudioWarningManager
import com.roadguardian.app.warning.HazardWarningManager
import com.roadguardian.app.weather.WeatherRepository
import com.roadguardian.app.weather.WeatherState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "Firebase initialization started")
        runCatching {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        }
        enableEdgeToEdge()
        setContent {
            AIRoadGuardianTheme {
                MainAppScreen()
            }
        }
    }
}

private const val MAIN_APP_SCREEN_TAG = "MainActivity"

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentScreen by remember { mutableStateOf(WayfinderScreen.HOME) }
    var previousScreen by remember { mutableStateOf(WayfinderScreen.HOME) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            currentScreen = WayfinderScreen.LIVE_MONITORING
        }
    }

    val weatherRepository = remember { WeatherRepository(context) }
    val weatherState by weatherRepository.weatherState.collectAsState()
    val scope = rememberCoroutineScope()

    val trafficRepository = remember { WazeTrafficRepository() }
    val trafficState by trafficRepository.trafficState.collectAsState()
    val isTrafficLayerVisible by trafficRepository.isTrafficLayerVisible.collectAsState()

    val locationManager = remember { WayfinderLocationManager(context) }
    val locationState by locationManager.locationState.collectAsState()
    var hasLocationPermission by remember { mutableStateOf(locationManager.hasPermission()) }
    var isLocationEnabled by remember { mutableStateOf(locationManager.isLocationEnabled()) }

    val authManager = remember { FirebaseAuthManager() }
    val hazardRepository: RoadHazardRepository = remember { FirestoreRoadHazardRepository(authManager = authManager) }
    val hazardsList by hazardRepository.hazardsState.collectAsState()

    val audioWarningManager = remember { AudioWarningManager(context) }
    var isTtsEnabled by remember { mutableStateOf(true) }

    val hazardWarningManager = remember {
        HazardWarningManager(
            warningRadiusMeters = 50.0,
            forwardConeDegrees = 45.0,
            audioWarningManager = audioWarningManager
        )
    }
    val warningState by hazardWarningManager.warningState.collectAsState()

    val spatialTracker = remember { SpatialTemporalTracker() }
    val detectionConfig = remember { DetectionConfig() }

    var totalDetectionsCount by remember { mutableIntStateOf(0) }
    var latestConfirmationResult by remember { mutableStateOf<ConfirmationResult?>(null) }
    var debugRawCount by remember { mutableIntStateOf(0) }
    var debugNmsCount by remember { mutableIntStateOf(0) }
    var debugTrackedCount by remember { mutableIntStateOf(0) }
    var debugConfirmedCount by remember { mutableIntStateOf(0) }
    var rawDiagStats by remember { mutableStateOf<RawDetectionStats?>(null) }
    var modelSignature by remember { mutableStateOf<DetectorModelSignature?>(null) }
    var focusedHazardOnMap by remember { mutableStateOf<RoadHazard?>(null) }

    val currentLocationState = rememberUpdatedState(locationState)
    val currentHazardRepository = rememberUpdatedState(hazardRepository)

    val confirmationManager = remember {
        PotholeConfirmationManager(
            confidenceThreshold = 0.25f,
            minConsecutiveDetections = 2,
            maxFrameGap = 2,
            cooldownDurationMillis = 4000L,
            onPotholeConfirmed = { candidate ->
                totalDetectionsCount++
                val loc = currentLocationState.value
                val (lat, lon) = if (loc.isAvailable && loc.latitude != 0.0) {
                    Pair(loc.latitude, loc.longitude)
                } else {
                    val last = locationManager.getLastKnownCoordinates()
                    if (last != null && last.first != 0.0) {
                        last
                    } else {
                        Pair(26.4499, 80.3319)
                    }
                }
                currentHazardRepository.value.recordPothole(
                    latitude = lat,
                    longitude = lon,
                    confidence = candidate.confidence,
                    severity = candidate.severity,
                    gpsAccuracy = loc.accuracy,
                    timestamp = candidate.timestamp
                )
                audioWarningManager.warnHazard(
                    severity = candidate.severity.name.lowercase(),
                    distanceMeters = 30,
                    hazardId = null,
                    force = false
                )
            }
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val granted = locationManager.hasPermission()
        val locEnabled = locationManager.isLocationEnabled()
        hasLocationPermission = granted
        isLocationEnabled = locEnabled
        if (granted && locEnabled) {
            locationManager.refreshLocation()
            if (currentScreen == WayfinderScreen.LIVE_MONITORING) {
                locationManager.startHighFrequencyUpdates()
            } else {
                locationManager.startLowFrequencyUpdates()
            }
        }
        scope.launch {
            weatherRepository.refreshWeather(force = true)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val perm = locationManager.hasPermission()
                val locEnabled = locationManager.isLocationEnabled()
                hasLocationPermission = perm
                isLocationEnabled = locEnabled
                if (perm && locEnabled) {
                    locationManager.refreshLocation()
                    if (currentScreen == WayfinderScreen.LIVE_MONITORING) {
                        locationManager.startHighFrequencyUpdates()
                    } else {
                        locationManager.startLowFrequencyUpdates()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        authManager.ensureAnonymousAuth()
        if (!LocationProvider.hasLocationPermission(context)) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            hasLocationPermission = true
            isLocationEnabled = locationManager.isLocationEnabled()
            locationManager.refreshLocation()
            weatherRepository.refreshWeather(force = true)
        }
    }

    LaunchedEffect(currentScreen) {
        if (currentScreen == WayfinderScreen.HOME) {
            weatherRepository.refreshWeather()
        } else if (currentScreen == WayfinderScreen.LIVE_MONITORING) {
            audioWarningManager.playStartupSound()
        }
    }

    LaunchedEffect(currentScreen, hasLocationPermission, isLocationEnabled) {
        if (hasLocationPermission && isLocationEnabled) {
            when (currentScreen) {
                WayfinderScreen.LIVE_MONITORING -> {
                    locationManager.startHighFrequencyUpdates()
                }
                else -> {
                    locationManager.startLowFrequencyUpdates()
                }
            }
        } else {
            locationManager.stopUpdates()
        }
    }

    val (cityName, regionName) = when (val state = weatherState) {
        is WeatherState.Success -> Pair(state.cityName, state.regionName)
        is WeatherState.Error -> Pair(state.cityName ?: "Current Area", state.regionName ?: "")
        is WeatherState.LocationUnavailable -> Pair(state.message, "")
        is WeatherState.Loading -> Pair("Locating...", "")
    }

    var activeModelType by remember { mutableStateOf(AiModelType.DEFAULT) }
    val benchmarkTracker = remember { AiBenchmarkTracker(initialModel = AiModelType.DEFAULT) }
    var benchmarkSnapshot by remember { mutableStateOf(benchmarkTracker.getSnapshot()) }

    val sensorManager = remember { WayfinderSensorManager(context) }
    val sensorTelemetry by sensorManager.telemetry.collectAsState()

    var detector by remember {
        mutableStateOf<RoadHazardDetector?>(null)
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val initial = runCatching { RoadHazardDetector.fromModelType(context, activeModelType) }.getOrNull()
            detector = initial
        }
    }

    LaunchedEffect(currentScreen, locationState.isAvailable, locationState.latitude, locationState.longitude) {
        if (locationState.isAvailable && locationState.latitude != 0.0) {
            hazardRepository.startRegionSync(locationState.latitude, locationState.longitude)
            trafficRepository.refreshTraffic(locationState.latitude, locationState.longitude)
        } else {
            val last = locationManager.getLastKnownCoordinates()
            val lat = last?.first ?: 26.4499
            val lon = last?.second ?: 80.3319
            hazardRepository.startRegionSync(lat, lon)
            trafficRepository.refreshTraffic(lat, lon)
        }
    }

    var latestHazardType by remember { mutableStateOf<HazardType?>(null) }
    var latestConfidence by remember { mutableFloatStateOf(0.0f) }
    var frameCount by remember { mutableLongStateOf(0L) }
    var lastMetadata by remember { mutableStateOf<FrameMetadata?>(null) }
    var currentDetections by remember { mutableStateOf<List<RoadHazardDetection>>(emptyList()) }

    LaunchedEffect(currentScreen) {
        if (currentScreen == WayfinderScreen.LIVE_MONITORING) {
            sensorManager.startListening()
            debugConfirmedCount = 0
            currentDetections = emptyList()
            spatialTracker.clear()
            confirmationManager.reset()
            audioWarningManager.resetCooldown()
        } else {
            sensorManager.stopListening()
            audioWarningManager.resetCooldown()
        }
    }

    LaunchedEffect(
        locationState.isAvailable,
        locationState.latitude,
        locationState.longitude,
        locationState.bearing,
        locationState.speedMps,
        hazardsList,
        trafficState
    ) {
        if (locationState.isAvailable) {
            hazardWarningManager.update(
                userLatitude = locationState.latitude,
                userLongitude = locationState.longitude,
                userBearing = locationState.bearing,
                userSpeedMps = locationState.speedMps,
                knownHazards = hazardsList
            )

            val incidents = when (val state = trafficState) {
                is WazeTrafficState.Success -> state.summary.incidents
                is WazeTrafficState.Error -> state.fallbackSummary?.incidents ?: emptyList()
                else -> emptyList()
            }
            if (incidents.isNotEmpty()) {
                hazardWarningManager.updateTraffic(
                    userLatitude = locationState.latitude,
                    userLongitude = locationState.longitude,
                    userBearing = locationState.bearing,
                    userSpeedMps = locationState.speedMps,
                    incidents = incidents
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            detector?.close()
            sensorManager.stopListening()
            locationManager.stopUpdates()
            hazardRepository.stopRegionSync()
            audioWarningManager.release()
        }
    }

    val analyzer = remember {
        RoadFrameAnalyzer(
            detector = detector,
            onInferenceResult = { rawDetections, metadata ->
                frameCount = metadata.frameNumber
                lastMetadata = metadata

                debugRawCount = rawDetections.size

                val tracked = spatialTracker.update(rawDetections, metadata.frameNumber, metadata.timestamp)
                debugNmsCount = rawDetections.size
                debugTrackedCount = tracked.size
                val smoothedDetections = tracked.map { it.toRoadHazardDetection() }
                currentDetections = smoothedDetections

                val confirmation = confirmationManager.processFrame(
                    trackedDetections = tracked,
                    frameNumber = metadata.frameNumber,
                    timestamp = metadata.timestamp,
                    hasSensorImpact = sensorTelemetry.impactDetected
                )
                latestConfirmationResult = confirmation
                if (confirmation.shouldTriggerReport) {
                    debugConfirmedCount++
                }

                if (smoothedDetections.isNotEmpty()) {
                    val best = smoothedDetections.maxByOrNull { it.confidence } ?: smoothedDetections[0]
                    latestHazardType = best.hazardType
                    latestConfidence = best.confidence
                }
            },
            onInferenceResultWithTiming = { detections, metadata, latencyMs ->
                val snap = benchmarkTracker.recordInference(latencyMs, detections)
                benchmarkSnapshot = snap
            },
            onError = { error ->
                Log.e(MAIN_APP_SCREEN_TAG, "TFLite inference failed", error)
            },
            onRawDiagnostics = { stats ->
                rawDiagStats = stats
            },
            onFrameAnalyzed = { metadata ->
                frameCount = metadata.frameNumber
                lastMetadata = metadata
            }
        )
    }

    LaunchedEffect(detector) {
        modelSignature = detector?.modelSignature
        analyzer.updateDetector(detector)
    }

    fun switchModel(newModel: AiModelType) {
        if (activeModelType == newModel) return
        activeModelType = newModel
        benchmarkTracker.startModelSwitch(newModel)
        benchmarkSnapshot = benchmarkTracker.getSnapshot()
        currentDetections = emptyList()
        spatialTracker.clear()
        confirmationManager.reset()

        scope.launch(Dispatchers.IO) {
            analyzer.updateDetector(null)

            val oldDetector = detector
            detector = null
            oldDetector?.close()

            val newDetector = runCatching {
                RoadHazardDetector.fromModelType(context, newModel)
            }.getOrNull()

            detector = newDetector
            analyzer.updateDetector(newDetector)
            benchmarkTracker.completeModelSwitch()
            benchmarkSnapshot = benchmarkTracker.getSnapshot()
        }
    }

    fun navigateTo(screen: WayfinderScreen) {
        if (currentScreen != screen) {
            previousScreen = currentScreen
            currentScreen = screen
        }
    }

    BackHandler(enabled = currentScreen != WayfinderScreen.HOME) {
        navigateTo(WayfinderScreen.HOME)
    }

    if (currentScreen == WayfinderScreen.LIVE_MONITORING) {
        if (hasCameraPermission) {
            LiveMonitoringScreen(
                analyzer = analyzer,
                detections = currentDetections,
                frameMetadata = lastMetadata,
                hazardsCount = debugConfirmedCount,
                speedValue = locationState.speedDisplayValue,
                speedUnit = locationState.speedDisplayUnit,
                benchmarkSnapshot = benchmarkSnapshot,
                sensorTelemetry = sensorTelemetry,
                confirmationResult = latestConfirmationResult,
                warningState = warningState,
                isTtsEnabled = isTtsEnabled,
                debugRawCount = debugRawCount,
                debugNmsCount = debugNmsCount,
                debugTrackedCount = debugTrackedCount,
                debugConfirmedCount = debugConfirmedCount,
                debugReportedCount = debugConfirmedCount,
                modelSignature = modelSignature,
                rawDiagStats = rawDiagStats,
                onToggleTts = {
                    isTtsEnabled = !isTtsEnabled
                    audioWarningManager.isEnabled = isTtsEnabled
                },
                onStopMonitoring = {
                    navigateTo(WayfinderScreen.HOME)
                }
            )
        } else {
            NatureBackground {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(28.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Videocam,
                                    contentDescription = "Camera Permission",
                                    tint = WayfinderPrimaryGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Camera Access Required",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = WayfinderTextPrimary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Wayfinder analyzes the road view in real time to detect potholes and surface hazards safely.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WayfinderTextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            GlassButton(
                                text = "Grant Permission",
                                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                isPrimary = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                WayfinderTopAppBar(
                    title = if (currentScreen == WayfinderScreen.SETTINGS) "Settings" else "Wayfinder",
                    showBackButton = currentScreen == WayfinderScreen.SETTINGS,
                    hazardCount = if (currentScreen == WayfinderScreen.MAP) hazardsList.size else null,
                    onMenuClick = { navigateTo(WayfinderScreen.SETTINGS) },
                    onBackClick = { navigateTo(WayfinderScreen.HOME) }
                )
            },
            bottomBar = {
                if (currentScreen != WayfinderScreen.SETTINGS) {
                    WayfinderBottomNav(
                        currentScreen = currentScreen,
                        onNavigate = { navigateTo(it) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    WayfinderScreen.HOME -> {
                        HomeScreen(
                            isReady = detector != null,
                            weatherState = weatherState,
                            trafficState = trafficState,
                            cityName = cityName,
                            regionName = regionName,
                            onStartMonitoring = {
                                if (hasCameraPermission) {
                                    navigateTo(WayfinderScreen.LIVE_MONITORING)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            onViewTrafficOnMap = {
                                navigateTo(WayfinderScreen.MAP)
                            }
                        )
                    }
                    WayfinderScreen.MAP -> {
                        RoadHealthMapScreen(
                            hazards = hazardsList,
                            locationState = locationState,
                            hasLocationPermission = hasLocationPermission,
                            isLocationEnabled = isLocationEnabled,
                            initialSelectedHazard = focusedHazardOnMap,
                            trafficState = trafficState,
                            isTrafficLayerVisible = isTrafficLayerVisible,
                            onToggleTrafficLayer = { trafficRepository.toggleTrafficLayer() },
                            onRequestLocationPermission = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        )
                    }
                    WayfinderScreen.HISTORY -> {
                        HistoryScreen(
                            hazards = hazardsList,
                            onClearHistory = { hazardRepository.clear() },
                            onNavigateToHazardOnMap = { hazard ->
                                focusedHazardOnMap = hazard
                                navigateTo(WayfinderScreen.MAP)
                            },
                            onStartMonitoring = {
                                if (hasCameraPermission) {
                                    navigateTo(WayfinderScreen.LIVE_MONITORING)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        )
                    }
                    WayfinderScreen.SETTINGS -> {
                        SettingsScreen(
                            activeModel = activeModelType,
                            audioWarningManager = audioWarningManager,
                            locationState = locationState,
                            onSelectModel = { switchModel(it) }
                        )
                    }
                    WayfinderScreen.LIVE_MONITORING -> {
                    }
                }
            }
        }
    }
}
