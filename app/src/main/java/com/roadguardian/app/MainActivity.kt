package com.roadguardian.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.roadguardian.app.ai.inference.RoadHazardDetector
import com.roadguardian.app.camera.CameraPreview
import com.roadguardian.app.camera.FrameMetadata
import com.roadguardian.app.camera.RoadFrameAnalyzer
import com.roadguardian.app.domain.model.HazardType
import com.roadguardian.app.domain.model.RoadHazardDetection
import com.roadguardian.app.ui.overlay.HazardDetectionOverlay
import com.roadguardian.app.ui.theme.AIRoadGuardianTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIRoadGuardianTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        val detector = remember {
            runCatching { RoadHazardDetector.fromAsset(context) }.getOrNull()
        }

        DisposableEffect(detector) {
            onDispose {
                detector?.close()
            }
        }

        var inferenceStatus by remember {
            mutableStateOf(if (detector != null) "Active" else "Model Load Error")
        }
        var detectionCount by remember { mutableIntStateOf(0) }
        var latestHazardType by remember { mutableStateOf<HazardType?>(null) }
        var latestConfidence by remember { mutableFloatStateOf(0.0f) }
        var frameCount by remember { mutableLongStateOf(0L) }
        var lastMetadata by remember { mutableStateOf<FrameMetadata?>(null) }
        var currentDetections by remember { mutableStateOf<List<RoadHazardDetection>>(emptyList()) }

        val analyzer = remember(detector) {
            RoadFrameAnalyzer(
                detector = detector,
                onInferenceResult = { detections, metadata ->
                    inferenceStatus = "Active"
                    frameCount = metadata.frameNumber
                    lastMetadata = metadata
                    currentDetections = detections
                    if (detections.isNotEmpty()) {
                        val best = detections.maxByOrNull { it.confidence } ?: detections[0]
                        latestHazardType = best.hazardType
                        latestConfidence = best.confidence
                        detectionCount += detections.size
                    }
                },
                onError = { throwable ->
                    inferenceStatus = "Error: ${throwable.message ?: "Unknown"}"
                },
                onFrameAnalyzed = { metadata ->
                    frameCount = metadata.frameNumber
                    lastMetadata = metadata
                }
            )
        }

        Box(modifier = modifier.fillMaxSize()) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                analyzer = analyzer
            )

            HazardDetectionOverlay(
                modifier = Modifier.fillMaxSize(),
                detections = currentDetections,
                frameMetadata = lastMetadata
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Wayfinder",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Inference: $inferenceStatus",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (inferenceStatus == "Active") Color.Green else Color.Red
                )
                Text(
                    text = "Detection: ${latestHazardType?.label ?: "None"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                if (latestHazardType != null) {
                    Text(
                        text = "Confidence: ${"%.2f".format(latestConfidence)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Yellow
                    )
                }
                Text(
                    text = "Total Detections: $detectionCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
                Text(
                    text = "Frames: $frameCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
                lastMetadata?.let { meta ->
                    Text(
                        text = "Analysis: ${meta.width}x${meta.height} (${meta.rotationDegrees}°) crop=[${meta.cropRectLeft},${meta.cropRectTop},${meta.cropRectRight},${meta.cropRectBottom}]",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    if (meta.previewStreamWidth > 0) {
                        Text(
                            text = "Preview: ${meta.previewStreamWidth}x${meta.previewStreamHeight} (${meta.previewStreamRotation}°) crop=[${meta.previewCropRectLeft},${meta.previewCropRectTop},${meta.previewCropRectRight},${meta.previewCropRectBottom}]",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Cyan
                        )
                    }
                    Text(
                        text = "Display: ${meta.displayRotationDegrees}°",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            }
        }
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Wayfinder",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera permission is required for road monitoring.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(text = "Grant Camera Permission")
                }
            }
        }
    }
}
