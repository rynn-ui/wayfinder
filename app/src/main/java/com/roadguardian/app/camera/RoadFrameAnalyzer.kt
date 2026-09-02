package com.roadguardian.app.camera

import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.roadguardian.app.ai.inference.RoadHazardDetector
import com.roadguardian.app.ai.postprocessing.RawDetectionStats
import com.roadguardian.app.domain.model.RoadHazardDetection
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

data class FrameMetadata(
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val timestamp: Long,
    val frameNumber: Long,
    val displayRotationDegrees: Int = 0,
    // ImageProxy.cropRect — the valid/intended area of the analysis buffer
    val cropRectLeft: Int = 0,
    val cropRectTop: Int = 0,
    val cropRectRight: Int = 0,
    val cropRectBottom: Int = 0,
    // Preview stream resolution (set after binding, 0 if unavailable)
    val previewStreamWidth: Int = 0,
    val previewStreamHeight: Int = 0,
    val previewStreamRotation: Int = 0,
    // Preview stream cropRect (set after binding)
    val previewCropRectLeft: Int = 0,
    val previewCropRectTop: Int = 0,
    val previewCropRectRight: Int = 0,
    val previewCropRectBottom: Int = 0
) {
    /** Width of the ImageProxy crop rect */
    val cropRectWidth: Int get() = cropRectRight - cropRectLeft
    /** Height of the ImageProxy crop rect */
    val cropRectHeight: Int get() = cropRectBottom - cropRectTop
    /** Whether the cropRect covers the full buffer */
    val isCropRectFullBuffer: Boolean get() =
        cropRectLeft == 0 && cropRectTop == 0 && cropRectRight == width && cropRectBottom == height
    /** Preview crop rect width */
    val previewCropWidth: Int get() = previewCropRectRight - previewCropRectLeft
    /** Preview crop rect height */
    val previewCropHeight: Int get() = previewCropRectBottom - previewCropRectTop
}

class RoadFrameAnalyzer(
    detector: RoadHazardDetector? = null,
    private val onInferenceResult: (List<RoadHazardDetection>, FrameMetadata) -> Unit = { _, _ -> },
    private val onInferenceResultWithTiming: (List<RoadHazardDetection>, FrameMetadata, Float) -> Unit = { _, _, _ -> },
    private val onError: (Throwable) -> Unit = {},
    private val onFrameAnalyzed: (FrameMetadata) -> Unit = {},
    private val onRawDiagnostics: (RawDetectionStats?) -> Unit = {}
) : ImageAnalysis.Analyzer {

    constructor(
        detector: RoadHazardDetector?,
        onInferenceResult: (List<RoadHazardDetection>, FrameMetadata) -> Unit,
        onError: (Throwable) -> Unit,
        onFrameAnalyzed: (FrameMetadata) -> Unit
    ) : this(
        detector = detector,
        onInferenceResult = onInferenceResult,
        onInferenceResultWithTiming = { _, _, _ -> },
        onError = onError,
        onFrameAnalyzed = onFrameAnalyzed,
        onRawDiagnostics = {}
    )

    private val detectorRef = AtomicReference<RoadHazardDetector?>(detector)

    @Volatile
    var displayRotationDegrees: Int = 0

    // Set after camera binding from Preview.resolutionInfo
    @Volatile
    var previewStreamWidth: Int = 0
    @Volatile
    var previewStreamHeight: Int = 0
    @Volatile
    var previewStreamRotation: Int = 0
    @Volatile
    var previewCropRect: Rect = Rect()

    private val isProcessing = AtomicBoolean(false)
    private val frameCounter = AtomicLong(0L)

    fun updateDetector(newDetector: RoadHazardDetector?) {
        detectorRef.set(newDetector)
    }

    override fun analyze(imageProxy: ImageProxy) {
        try {
            val frameNumber = frameCounter.incrementAndGet()
            val crop = try { imageProxy.cropRect } catch (_: Throwable) { null }
            val cropLeft = crop?.left ?: 0
            val cropTop = crop?.top ?: 0
            val cropRight = if (crop != null && crop.right > 0) crop.right else imageProxy.width
            val cropBottom = if (crop != null && crop.bottom > 0) crop.bottom else imageProxy.height

            val metadata = FrameMetadata(
                width = imageProxy.width,
                height = imageProxy.height,
                rotationDegrees = imageProxy.imageInfo.rotationDegrees,
                timestamp = imageProxy.imageInfo.timestamp,
                frameNumber = frameNumber,
                displayRotationDegrees = displayRotationDegrees,
                cropRectLeft = cropLeft,
                cropRectTop = cropTop,
                cropRectRight = cropRight,
                cropRectBottom = cropBottom,
                previewStreamWidth = previewStreamWidth,
                previewStreamHeight = previewStreamHeight,
                previewStreamRotation = previewStreamRotation,
                previewCropRectLeft = previewCropRect.left,
                previewCropRectTop = previewCropRect.top,
                previewCropRectRight = previewCropRect.right,
                previewCropRectBottom = previewCropRect.bottom
            )

            onFrameAnalyzed(metadata)

            val currentDetector = detectorRef.get()
            if (currentDetector != null && isProcessing.compareAndSet(false, true)) {
                try {
                    val result = currentDetector.detectWithTiming(imageProxy, metadata.timestamp)
                    onInferenceResult(result.detections, metadata)
                    onInferenceResultWithTiming(result.detections, metadata, result.latencyMs)
                    onRawDiagnostics(result.rawStats)
                } catch (t: Throwable) {
                    onError(t)
                    onRawDiagnostics(null)
                } finally {
                    isProcessing.set(false)
                }
            }
        } finally {
            imageProxy.close()
        }
    }
}
