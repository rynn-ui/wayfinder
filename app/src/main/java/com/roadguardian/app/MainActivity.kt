package com.roadguardian.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.roadguardian.app.ai.inference.AiBenchmarkTracker
import com.roadguardian.app.ai.inference.AiModelType
import com.roadguardian.app.ai.inference.RoadHazardDetector
import com.roadguardian.app.camera.FrameMetadata
import com.roadguardian.app.camera.RoadFrameAnalyzer
import com.roadguardian.app.domain.model.HazardType
import com.roadguardian.app.domain.model.RoadHazardDetection
import com.roadguardian.app.location.LocationProvider
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
import com.roadguardian.app.weather.WeatherRepository
import com.roadguardian.app.weather.WeatherState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIRoadGuardianTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
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

    // Weather & Location Infrastructure
    val weatherRepository = remember { WeatherRepository(context) }
    val weatherState by weatherRepository.weatherState.collectAsState()
    val scope = rememberCoroutineScope()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        scope.launch {
            weatherRepository.refreshWeather(force = true)
        }
    }

    LaunchedEffect(Unit) {
        if (!LocationProvider.hasLocationPermission(context)) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            weatherRepository.refreshWeather(force = true)
        }
    }

    // Periodically and on screen activation refresh weather
    LaunchedEffect(currentScreen) {
        if (currentScreen == WayfinderScreen.HOME) {
            weatherRepository.refreshWeather()
        }
    }

    // Derive displayed city and region
    val (cityName, regionName) = when (val state = weatherState) {
        is WeatherState.Success -> Pair(state.cityName, state.regionName)
        is WeatherState.Error -> Pair(state.cityName ?: "Current Area", state.regionName ?: "")
        is WeatherState.LocationUnavailable -> Pair(state.message, "")
        is WeatherState.Loading -> Pair("Locating...", "")
    }

    // AI Model A/B Testing & Benchmark Infrastructure
    var activeModelType by remember { mutableStateOf(AiModelType.DEFAULT) }
    val benchmarkTracker = remember { AiBenchmarkTracker(initialModel = AiModelType.DEFAULT) }
    var benchmarkSnapshot by remember { mutableStateOf(benchmarkTracker.getSnapshot()) }

    var detector by remember {
        mutableStateOf<RoadHazardDetector?>(null)
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val initial = runCatching { RoadHazardDetector.fromModelType(context, activeModelType) }.getOrNull()
            detector = initial
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            detector?.close()
        }
    }

    var totalDetectionsCount by remember { mutableIntStateOf(0) }
    var latestHazardType by remember { mutableStateOf<HazardType?>(null) }
    var latestConfidence by remember { mutableFloatStateOf(0.0f) }
    var frameCount by remember { mutableLongStateOf(0L) }
    var lastMetadata by remember { mutableStateOf<FrameMetadata?>(null) }
    var currentDetections by remember { mutableStateOf<List<RoadHazardDetection>>(emptyList()) }

    val analyzer = remember {
        RoadFrameAnalyzer(
            detector = detector,
            onInferenceResult = { detections, metadata ->
                frameCount = metadata.frameNumber
                lastMetadata = metadata
                currentDetections = detections
                if (detections.isNotEmpty()) {
                    val best = detections.maxByOrNull { it.confidence } ?: detections[0]
                    latestHazardType = best.hazardType
                    latestConfidence = best.confidence
                    totalDetectionsCount += detections.size
                }
            },
            onInferenceResultWithTiming = { detections, metadata, latencyMs ->
                val snap = benchmarkTracker.recordInference(latencyMs, detections)
                benchmarkSnapshot = snap
            },
            onError = { _ -> },
            onFrameAnalyzed = { metadata ->
                frameCount = metadata.frameNumber
                lastMetadata = metadata
            }
        )
    }

    // Keep analyzer's detector in sync when detector state changes
    LaunchedEffect(detector) {
        analyzer.updateDetector(detector)
    }

    // Safe mutual-exclusive model switching
    fun switchModel(newModel: AiModelType) {
        if (activeModelType == newModel) return
        activeModelType = newModel
        benchmarkTracker.startModelSwitch(newModel)
        benchmarkSnapshot = benchmarkTracker.getSnapshot()
        currentDetections = emptyList()

        scope.launch(Dispatchers.IO) {
            // 1. Temporarily detach detector from analyzer to prevent in-flight race conditions
            analyzer.updateDetector(null)

            // 2. Safely close old detector
            val oldDetector = detector
            detector = null
            oldDetector?.close()

            // 3. Load newly selected model
            val newDetector = runCatching {
                RoadHazardDetector.fromModelType(context, newModel)
            }.getOrNull()

            // 4. Attach new detector and complete switch
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

    // Live Monitoring Mode (full screen with camera and detection HUD)
    if (currentScreen == WayfinderScreen.LIVE_MONITORING) {
        if (hasCameraPermission) {
            LiveMonitoringScreen(
                analyzer = analyzer,
                detections = currentDetections,
                frameMetadata = lastMetadata,
                totalDetectionsCount = totalDetectionsCount,
                benchmarkSnapshot = benchmarkSnapshot,
                onStopMonitoring = {
                    navigateTo(WayfinderScreen.HOME)
                }
            )
        } else {
            // Calm Leafy Glass Permission Screen
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
        // Standard Navigation Scaffolding (Home, Map, History, Settings)
        Scaffold(
            containerColor = WayfinderDarkBackground,
            topBar = {
                WayfinderTopAppBar(
                    title = "Wayfinder",
                    showBackButton = currentScreen == WayfinderScreen.SETTINGS,
                    hazardCount = if (currentScreen == WayfinderScreen.MAP) 0 else null,
                    onMenuClick = { navigateTo(WayfinderScreen.SETTINGS) },
                    onBackClick = { navigateTo(previousScreen) }
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
                            cityName = cityName,
                            regionName = regionName,
                            onStartMonitoring = {
                                if (hasCameraPermission) {
                                    navigateTo(WayfinderScreen.LIVE_MONITORING)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        )
                    }
                    WayfinderScreen.MAP -> {
                        RoadHealthMapScreen()
                    }
                    WayfinderScreen.HISTORY -> {
                        HistoryScreen()
                    }
                    WayfinderScreen.SETTINGS -> {
                        SettingsScreen(
                            activeModel = activeModelType,
                            onSelectModel = { switchModel(it) }
                        )
                    }
                    WayfinderScreen.LIVE_MONITORING -> {
                        // Handled above
                    }
                }
            }
        }
    }
}
