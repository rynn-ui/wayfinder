package com.roadguardian.app.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.util.concurrent.atomic.AtomicLong

data class FrameMetadata(
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val timestamp: Long,
    val frameNumber: Long
)

class RoadFrameAnalyzer(
    private val onFrameAnalyzed: (FrameMetadata) -> Unit = {}
) : ImageAnalysis.Analyzer {

    private val frameCounter = AtomicLong(0L)

    override fun analyze(imageProxy: ImageProxy) {
        try {
            val frameNumber = frameCounter.incrementAndGet()
            val metadata = FrameMetadata(
                width = imageProxy.width,
                height = imageProxy.height,
                rotationDegrees = imageProxy.imageInfo.rotationDegrees,
                timestamp = imageProxy.imageInfo.timestamp,
                frameNumber = frameNumber
            )
            onFrameAnalyzed(metadata)
        } finally {
            imageProxy.close()
        }
    }
}
