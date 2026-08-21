package com.roadguardian.app.camera

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    analyzer: ImageAnalysis.Analyzer
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember {
        ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            ThreadPoolExecutor.DiscardPolicy()
        )
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var previewUseCase: Preview? = null
    var imageAnalysisUseCase: ImageAnalysis? = null
    var currentPreviewView: PreviewView? = null

    val displayManager = remember {
        context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    }

    val orientationEventListener = remember {
        object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                imageAnalysisUseCase?.targetRotation = rotation
            }
        }
    }

    fun updateDisplayRotation(displayRotation: Int) {
        val degrees = when (displayRotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        if (analyzer is RoadFrameAnalyzer) {
            analyzer.displayRotationDegrees = degrees
        }
        previewUseCase?.targetRotation = displayRotation
    }

    val displayListener = remember {
        object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {}
            override fun onDisplayRemoved(displayId: Int) {}
            override fun onDisplayChanged(displayId: Int) {
                val view = currentPreviewView ?: return
                val display = view.display ?: return
                if (display.displayId == displayId) {
                    updateDisplayRotation(display.rotation)
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable()
        }
        displayManager?.registerDisplayListener(displayListener, null)
        onDispose {
            orientationEventListener.disable()
            displayManager?.unregisterDisplayListener(displayListener)
            try {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (_: Exception) {
            }
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            currentPreviewView = previewView

            val initialDisplayRotation = previewView.display?.rotation ?: Surface.ROTATION_0
            updateDisplayRotation(initialDisplayRotation)

            previewView.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
                val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
                updateDisplayRotation(rotation)
                android.util.Log.i(
                    "WayfinderCamera",
                    "PreviewView layout: ${right - left}x${bottom - top}, viewPort=${previewView.viewPort}"
                )
                if (analyzer is RoadFrameAnalyzer) {
                    previewUseCase?.resolutionInfo?.let { resInfo ->
                        analyzer.previewStreamWidth = resInfo.resolution.width
                        analyzer.previewStreamHeight = resInfo.resolution.height
                        analyzer.previewStreamRotation = resInfo.rotationDegrees
                        analyzer.previewCropRect = resInfo.cropRect
                    }
                }
            }

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val currentRotation = previewView.display?.rotation ?: Surface.ROTATION_0

                val preview = Preview.Builder()
                    .setTargetRotation(currentRotation)
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setTargetRotation(currentRotation)
                    .build()
                    .also {
                        it.setAnalyzer(analysisExecutor, analyzer)
                    }

                previewUseCase = preview
                imageAnalysisUseCase = imageAnalysis

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )

                    val prevRes = preview.resolutionInfo
                    val analysisRes = imageAnalysis.resolutionInfo
                    android.util.Log.i(
                        "WayfinderCamera",
                        "Preview bound: res=${prevRes?.resolution}, crop=${prevRes?.cropRect}, rot=${prevRes?.rotationDegrees}"
                    )
                    android.util.Log.i(
                        "WayfinderCamera",
                        "Analysis bound: res=${analysisRes?.resolution}, crop=${analysisRes?.cropRect}, rot=${analysisRes?.rotationDegrees}"
                    )
                    android.util.Log.i(
                        "WayfinderCamera",
                        "PreviewView initial: ${previewView.width}x${previewView.height}, viewPort=${previewView.viewPort}"
                    )

                    if (analyzer is RoadFrameAnalyzer && prevRes != null) {
                        analyzer.previewStreamWidth = prevRes.resolution.width
                        analyzer.previewStreamHeight = prevRes.resolution.height
                        analyzer.previewStreamRotation = prevRes.rotationDegrees
                        analyzer.previewCropRect = prevRes.cropRect
                    }
                } catch (e: Exception) {
                    android.util.Log.e("WayfinderCamera", "Camera bind failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        update = { view ->
            currentPreviewView = view
            val rotation = view.display?.rotation ?: Surface.ROTATION_0
            updateDisplayRotation(rotation)
        }
    )
}
