package com.roadguardian.app.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.roadguardian.app.camera.FrameMetadata
import com.roadguardian.app.domain.model.HazardType
import com.roadguardian.app.domain.model.RoadHazardDetection
import com.roadguardian.app.ui.theme.WayfinderHazardLongitudinal
import com.roadguardian.app.ui.theme.WayfinderHazardPothole
import com.roadguardian.app.ui.theme.WayfinderHazardTransverse

@Composable
fun HazardDetectionOverlay(
    modifier: Modifier = Modifier,
    detections: List<RoadHazardDetection>,
    frameMetadata: FrameMetadata?
) {
    if (detections.isEmpty() || frameMetadata == null) return

    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 34.0f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }

    val textBgPaint = remember {
        android.graphics.Paint().apply {
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val viewWidth = size.width
        val viewHeight = size.height
        if (viewWidth <= 0.0f || viewHeight <= 0.0f) return@Canvas

        val bufferRotation = frameMetadata.rotationDegrees

        val uprightWidth = if (frameMetadata.rotationDegrees == 90 || frameMetadata.rotationDegrees == 270) {
            frameMetadata.height.toFloat()
        } else {
            frameMetadata.width.toFloat()
        }

        val uprightHeight = if (frameMetadata.rotationDegrees == 90 || frameMetadata.rotationDegrees == 270) {
            frameMetadata.width.toFloat()
        } else {
            frameMetadata.height.toFloat()
        }

        for (detection in detections) {
            val bbox = detection.boundingBox ?: continue

            val confPercent = (detection.confidence * 100).toInt()
            val label = "POTHOLE $confPercent%"
            val textBounds = android.graphics.Rect()
            textPaint.getTextBounds(label, 0, label.length, textBounds)

            val padding = 8.0f
            val labelWidth = textBounds.width().toFloat() + padding * 2
            val labelHeight = textBounds.height().toFloat() + padding * 2

            val overlay = CoordinateTransformer.transformDetection(
                boundingBox = bbox,
                imageWidth = uprightWidth,
                imageHeight = uprightHeight,
                viewWidth = viewWidth,
                viewHeight = viewHeight,
                relativeRotationDegrees = 0,
                labelWidth = labelWidth,
                labelHeight = labelHeight,
                gap = 4.0f
            )

            val polygon = overlay.polygon

            val boxColor = when (detection.hazardType) {
                HazardType.POTHOLE -> WayfinderHazardPothole
                HazardType.LONGITUDINAL_CRACK -> WayfinderHazardLongitudinal
                HazardType.TRANSVERSE_CRACK -> WayfinderHazardTransverse
            }

            val path = Path().apply {
                moveTo(polygon.topLeft.x, polygon.topLeft.y)
                lineTo(polygon.topRight.x, polygon.topRight.y)
                lineTo(polygon.bottomRight.x, polygon.bottomRight.y)
                lineTo(polygon.bottomLeft.x, polygon.bottomLeft.y)
                close()
            }
            drawPath(path = path, color = boxColor, style = Stroke(width = 5.0f))

            textBgPaint.color = android.graphics.Color.argb(
                (0.85f * 255).toInt(),
                (boxColor.red * 255).toInt(),
                (boxColor.green * 255).toInt(),
                (boxColor.blue * 255).toInt()
            )

            val nativeCanvas = drawContext.canvas.nativeCanvas
            nativeCanvas.save()

            nativeCanvas.translate(overlay.labelAnchor.x, overlay.labelAnchor.y)
            nativeCanvas.rotate(overlay.labelRotationDegrees)

            nativeCanvas.drawRect(
                -labelWidth / 2, -labelHeight / 2,
                labelWidth / 2, labelHeight / 2,
                textBgPaint
            )

            nativeCanvas.rotate(-overlay.labelRotationDegrees)

            val textWidth = textPaint.measureText(label)
            val fm = textPaint.fontMetrics
            nativeCanvas.drawText(
                label,
                -textWidth / 2,
                -(fm.ascent + fm.descent) / 2,
                textPaint
            )

            nativeCanvas.restore()
        }
    }
}

