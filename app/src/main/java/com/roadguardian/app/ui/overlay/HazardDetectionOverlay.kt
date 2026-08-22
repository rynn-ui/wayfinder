package com.roadguardian.app.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.roadguardian.app.camera.FrameMetadata
import com.roadguardian.app.domain.model.HazardType
import com.roadguardian.app.domain.model.RoadHazardDetection
import com.roadguardian.app.ui.theme.WayfinderHazardCrack
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

        // Authoritative buffer->upright rotation from CameraX for THIS frame.
        // Do not derive it from display rotation: imageInfo.rotationDegrees
        // already encodes sensor vs target-rotation, so a difference of the
        // two cancels to 0 in every orientation.
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

            val label = "${detection.hazardType.label.replace('_', ' ').uppercase()} ${"%.2f".format(detection.confidence)}"
            val textBounds = android.graphics.Rect()
            textPaint.getTextBounds(label, 0, label.length, textBounds)

            val padding = 8.0f
            val labelWidth = textBounds.width().toFloat() + padding * 2
            val labelHeight = textBounds.height().toFloat() + padding * 2

            // ── Single unified transform: polygon + edge + anchor + rotation ──
            val overlay = CoordinateTransformer.transformDetection(
                boundingBox = bbox,
                imageWidth = uprightWidth,
                imageHeight = uprightHeight,
                viewWidth = viewWidth,
                viewHeight = viewHeight,
                relativeRotationDegrees = bufferRotation,
                labelWidth = labelWidth,
                labelHeight = labelHeight,
                gap = 4.0f
            )

            val polygon = overlay.polygon

            // Calm, muted leafy/nature color accents for detected hazards
            val boxColor = when (detection.hazardType) {
                HazardType.POTHOLE -> WayfinderHazardPothole
                HazardType.ALLIGATOR_CRACK -> WayfinderHazardCrack
                HazardType.LONGITUDINAL_CRACK -> WayfinderHazardLongitudinal
                HazardType.TRANSVERSE_CRACK -> WayfinderHazardTransverse
            }

            // Draw detection polygon
            val path = Path().apply {
                moveTo(polygon.topLeft.x, polygon.topLeft.y)
                lineTo(polygon.topRight.x, polygon.topRight.y)
                lineTo(polygon.bottomRight.x, polygon.bottomRight.y)
                lineTo(polygon.bottomLeft.x, polygon.bottomLeft.y)
                close()
            }
            drawPath(path = path, color = boxColor, style = Stroke(width = 5.0f))

            // ── Draw rotated label badge with counter-rotated readable text ──
            textBgPaint.color = android.graphics.Color.argb(
                (0.78f * 255).toInt(),
                (boxColor.red * 255).toInt(),
                (boxColor.green * 255).toInt(),
                (boxColor.blue * 255).toInt()
            )

            val nativeCanvas = drawContext.canvas.nativeCanvas
            nativeCanvas.save()

            // Translate to label centre, rotate badge to match the selected edge
            nativeCanvas.translate(overlay.labelAnchor.x, overlay.labelAnchor.y)
            nativeCanvas.rotate(overlay.labelRotationDegrees)

            // Draw background rect centred at origin (rotated with canvas)
            nativeCanvas.drawRect(
                -labelWidth / 2, -labelHeight / 2,
                labelWidth / 2, labelHeight / 2,
                textBgPaint
            )

            // Counter-rotate so text stays horizontal and readable
            nativeCanvas.rotate(-overlay.labelRotationDegrees)

            // Draw text centred at origin
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
