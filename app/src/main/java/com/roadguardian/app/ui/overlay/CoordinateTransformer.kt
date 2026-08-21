package com.roadguardian.app.ui.overlay

import com.roadguardian.app.domain.model.BoundingBox

data class PreviewPoint(
    val x: Float,
    val y: Float
)

data class TransformedPolygon(
    val topLeft: PreviewPoint,
    val topRight: PreviewPoint,
    val bottomRight: PreviewPoint,
    val bottomLeft: PreviewPoint
) {
    val points: List<PreviewPoint>
        get() = listOf(topLeft, topRight, bottomRight, bottomLeft)

    val minX: Float
        get() = minOf(topLeft.x, topRight.x, bottomRight.x, bottomLeft.x)

    val maxX: Float
        get() = maxOf(topLeft.x, topRight.x, bottomRight.x, bottomLeft.x)

    val minY: Float
        get() = minOf(topLeft.y, topRight.y, bottomRight.y, bottomLeft.y)

    val maxY: Float
        get() = maxOf(topLeft.y, topRight.y, bottomRight.y, bottomLeft.y)

    val boundingWidth: Float
        get() = maxX - minX

    val boundingHeight: Float
        get() = maxY - minY

    val center: PreviewPoint
        get() = PreviewPoint(
            (topLeft.x + topRight.x + bottomRight.x + bottomLeft.x) / 4.0f,
            (topLeft.y + topRight.y + bottomRight.y + bottomLeft.y) / 4.0f
        )

    fun toBoundingRect(): PreviewRect = PreviewRect(minX, minY, maxX, maxY)
}

data class PreviewRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float
        get() = right - left

    val height: Float
        get() = bottom - top
}

object CoordinateTransformer {

    fun rotatePoint(
        x: Float,
        y: Float,
        imageWidth: Float,
        imageHeight: Float,
        rotationDegrees: Int
    ): Pair<PreviewPoint, Pair<Float, Float>> {
        return when ((rotationDegrees % 360 + 360) % 360) {
            90 -> {
                val newX = imageHeight - y
                val newY = x
                Pair(PreviewPoint(newX, newY), Pair(imageHeight, imageWidth))
            }
            180 -> {
                val newX = imageWidth - x
                val newY = imageHeight - y
                Pair(PreviewPoint(newX, newY), Pair(imageWidth, imageHeight))
            }
            270 -> {
                val newX = y
                val newY = imageWidth - x
                Pair(PreviewPoint(newX, newY), Pair(imageHeight, imageWidth))
            }
            else -> Pair(PreviewPoint(x, y), Pair(imageWidth, imageHeight))
        }
    }

    fun rotateBoundingBox(
        boundingBox: BoundingBox,
        imageWidth: Float,
        imageHeight: Float,
        rotationDegrees: Int
    ): Pair<BoundingBox, Pair<Float, Float>> {
        return when ((rotationDegrees % 360 + 360) % 360) {
            90 -> {
                val newX = imageHeight - (boundingBox.y + boundingBox.height)
                val newY = boundingBox.x
                Pair(BoundingBox(newX, newY, boundingBox.height, boundingBox.width), Pair(imageHeight, imageWidth))
            }
            180 -> {
                val newX = imageWidth - (boundingBox.x + boundingBox.width)
                val newY = imageHeight - (boundingBox.y + boundingBox.height)
                Pair(BoundingBox(newX, newY, boundingBox.width, boundingBox.height), Pair(imageWidth, imageHeight))
            }
            270 -> {
                val newX = boundingBox.y
                val newY = imageWidth - (boundingBox.x + boundingBox.width)
                Pair(BoundingBox(newX, newY, boundingBox.height, boundingBox.width), Pair(imageHeight, imageWidth))
            }
            else -> Pair(boundingBox, Pair(imageWidth, imageHeight))
        }
    }

    fun transformPoint(
        x: Float,
        y: Float,
        imageWidth: Float,
        imageHeight: Float,
        viewWidth: Float,
        viewHeight: Float,
        relativeRotationDegrees: Int = 0
    ): PreviewPoint {
        if (imageWidth <= 0.0f || imageHeight <= 0.0f || viewWidth <= 0.0f || viewHeight <= 0.0f) {
            return PreviewPoint(0.0f, 0.0f)
        }

        val (rotatedPoint, effDims) = rotatePoint(x, y, imageWidth, imageHeight, relativeRotationDegrees)
        val (effWidth, effHeight) = effDims

        val scale = maxOf(viewWidth / effWidth, viewHeight / effHeight)
        val scaledWidth = effWidth * scale
        val scaledHeight = effHeight * scale
        val offsetX = (viewWidth - scaledWidth) / 2.0f
        val offsetY = (viewHeight - scaledHeight) / 2.0f

        val canvasX = rotatedPoint.x * scale + offsetX
        val canvasY = rotatedPoint.y * scale + offsetY

        return PreviewPoint(canvasX, canvasY)
    }

    fun transformToPolygon(
        boundingBox: BoundingBox,
        imageWidth: Float,
        imageHeight: Float,
        viewWidth: Float,
        viewHeight: Float,
        relativeRotationDegrees: Int = 0
    ): TransformedPolygon {
        if (imageWidth <= 0.0f || imageHeight <= 0.0f || viewWidth <= 0.0f || viewHeight <= 0.0f) {
            val zeroPoint = PreviewPoint(0.0f, 0.0f)
            return TransformedPolygon(zeroPoint, zeroPoint, zeroPoint, zeroPoint)
        }

        val pTL = transformPoint(
            x = boundingBox.x,
            y = boundingBox.y,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            relativeRotationDegrees = relativeRotationDegrees
        )
        val pTR = transformPoint(
            x = boundingBox.x + boundingBox.width,
            y = boundingBox.y,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            relativeRotationDegrees = relativeRotationDegrees
        )
        val pBR = transformPoint(
            x = boundingBox.x + boundingBox.width,
            y = boundingBox.y + boundingBox.height,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            relativeRotationDegrees = relativeRotationDegrees
        )
        val pBL = transformPoint(
            x = boundingBox.x,
            y = boundingBox.y + boundingBox.height,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            relativeRotationDegrees = relativeRotationDegrees
        )

        return TransformedPolygon(
            topLeft = pTL,
            topRight = pTR,
            bottomRight = pBR,
            bottomLeft = pBL
        )
    }

    fun transform(
        boundingBox: BoundingBox,
        imageWidth: Float,
        imageHeight: Float,
        viewWidth: Float,
        viewHeight: Float,
        relativeRotationDegrees: Int = 0
    ): PreviewRect {
        val polygon = transformToPolygon(
            boundingBox = boundingBox,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            relativeRotationDegrees = relativeRotationDegrees
        )
        return polygon.toBoundingRect()
    }

    fun getPolygonEdges(polygon: TransformedPolygon): List<PolygonEdge> {
        return listOf(
            PolygonEdge(polygon.topLeft, polygon.topRight, 0),
            PolygonEdge(polygon.topRight, polygon.bottomRight, 1),
            PolygonEdge(polygon.bottomRight, polygon.bottomLeft, 2),
            PolygonEdge(polygon.bottomLeft, polygon.topLeft, 3)
        )
    }

    fun findVisualTopEdge(polygon: TransformedPolygon): PolygonEdge {
        val edges = getPolygonEdges(polygon)
        return edges.minByOrNull { it.midpoint.y } ?: edges[0]
    }

    fun calculateLabelAnchor(
        polygon: TransformedPolygon,
        labelWidth: Float,
        labelHeight: Float,
        viewWidth: Float,
        viewHeight: Float,
        gap: Float = 4.0f
    ): PreviewPoint {
        if (viewWidth <= 0.0f || viewHeight <= 0.0f) {
            return PreviewPoint(0.0f, 0.0f)
        }

        val topEdge = findVisualTopEdge(polygon)

        // Align horizontally with the start/left of the visual top edge, clamped to screen bounds
        val rawLeft = topEdge.minX
        val maxLabelLeft = maxOf(0.0f, viewWidth - labelWidth)
        val labelLeft = rawLeft.coerceIn(0.0f, maxLabelLeft)

        // Position vertically: above the visual top edge if space permits; flip inside if near screen top
        val nominalTop = topEdge.minY - labelHeight - gap
        val maxLabelTop = maxOf(0.0f, viewHeight - labelHeight)

        val labelTop = if (nominalTop >= 0.0f) {
            nominalTop.coerceIn(0.0f, maxLabelTop)
        } else {
            // Not enough room above the top edge; place immediately inside the top edge
            (topEdge.minY + gap).coerceIn(0.0f, maxLabelTop)
        }

        return PreviewPoint(labelLeft, labelTop)
    }

    /**
     * Maps relativeRotationDegrees to the edge index that becomes the visual top
     * after the same rotation applied by transformToPolygon.
     *
     * 0°  -> E0 (topLeft -> topRight)
     * 90° -> E3 (bottomLeft -> topLeft)
     * 180° -> E2 (bottomRight -> bottomLeft)
     * 270° -> E1 (topRight -> bottomRight)
     */
    fun edgeIndexForRotation(relativeRotationDegrees: Int): Int {
        return when ((relativeRotationDegrees % 360 + 360) % 360) {
            90 -> 3
            180 -> 2
            270 -> 1
            else -> 0
        }
    }

    /** Returns the angle (degrees) of the edge vector start -> end, in standard math convention. */
    fun calculateEdgeAngle(edge: PolygonEdge): Float {
        val dx = edge.end.x - edge.start.x
        val dy = edge.end.y - edge.start.y
        return Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }

    /**
     * Creates a unified [TransformedDetectionOverlay] from a BoundingBox,
     * deriving the polygon, selected edge, label anchor, and label rotation
     * from the SAME transformation.
     *
     * The label edge is chosen from the resulting on-screen geometry
     * (the edge whose midpoint has the minimum Y), which coincides with
     * [edgeIndexForRotation] for axis-aligned upright transforms but stays
     * correct even if the display itself never rotates.
     */
    fun transformDetection(
        boundingBox: BoundingBox,
        imageWidth: Float,
        imageHeight: Float,
        viewWidth: Float,
        viewHeight: Float,
        relativeRotationDegrees: Int,
        labelWidth: Float,
        labelHeight: Float,
        gap: Float = 4.0f
    ): TransformedDetectionOverlay {
        val polygon = transformToPolygon(
            boundingBox = boundingBox,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            relativeRotationDegrees = relativeRotationDegrees
        )

        val edges = getPolygonEdges(polygon)
        val selectedEdge = edges.minByOrNull { it.midpoint.y } ?: edges[0]
        val labelRotation = calculateEdgeAngle(selectedEdge)

        val labelAnchor = calculateLabelAnchorForEdge(
            edge = selectedEdge,
            polygon = polygon,
            labelWidth = labelWidth,
            labelHeight = labelHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            gap = gap
        )

        return TransformedDetectionOverlay(
            polygon = polygon,
            labelAnchor = labelAnchor,
            labelRotationDegrees = labelRotation,
            selectedEdge = selectedEdge
        )
    }

    /**
     * Positions the label center perpendicular to the given edge, on the side
     * away from the polygon centre (i.e. outside the detection box).
     */
    fun calculateLabelAnchorForEdge(
        edge: PolygonEdge,
        polygon: TransformedPolygon,
        labelWidth: Float,
        labelHeight: Float,
        viewWidth: Float,
        viewHeight: Float,
        gap: Float = 4.0f
    ): PreviewPoint {
        if (viewWidth <= 0.0f || viewHeight <= 0.0f) {
            return PreviewPoint(0.0f, 0.0f)
        }

        val center = polygon.center
        val midpoint = edge.midpoint

        val edgeDx = edge.end.x - edge.start.x
        val edgeDy = edge.end.y - edge.start.y

        // Perpendicular to edge: (-dy, dx)
        val perpX = -edgeDy
        val perpY = edgeDx

        // Direction from edge midpoint to polygon centre
        val toCenterX = center.x - midpoint.x
        val toCenterY = center.y - midpoint.y

        // Choose the perpendicular that points AWAY from centre
        val dot = perpX * toCenterX + perpY * toCenterY
        val outwardX = if (dot > 0) -perpX else perpX
        val outwardY = if (dot > 0) -perpY else perpY

        val len = kotlin.math.hypot(outwardX, outwardY)
        val unitOutX = if (len > 0) outwardX / len else 0.0f
        val unitOutY = if (len > 0) outwardY / len else 0.0f

        // Place label centre at edge midpoint + outward offset
        var labelCenterX = midpoint.x + unitOutX * (labelHeight / 2.0f + gap)
        var labelCenterY = midpoint.y + unitOutY * (labelHeight / 2.0f + gap)

        // Clamp so the rotated label stays inside the viewport
        val edgeAngle = calculateEdgeAngle(edge)
        val cos = kotlin.math.cos(Math.toRadians(edgeAngle.toDouble())).toFloat()
        val sin = kotlin.math.sin(Math.toRadians(edgeAngle.toDouble())).toFloat()
        val halfW = labelWidth / 2.0f
        val halfH = labelHeight / 2.0f
        val extentX = halfW * kotlin.math.abs(cos) + halfH * kotlin.math.abs(sin)
        val extentY = halfW * kotlin.math.abs(sin) + halfH * kotlin.math.abs(cos)

        labelCenterX = labelCenterX.coerceIn(extentX, viewWidth - extentX)
        labelCenterY = labelCenterY.coerceIn(extentY, viewHeight - extentY)

        return PreviewPoint(labelCenterX, labelCenterY)
    }
}

data class PolygonEdge(
    val start: PreviewPoint,
    val end: PreviewPoint,
    val edgeIndex: Int
) {
    val midpoint: PreviewPoint
        get() = PreviewPoint((start.x + end.x) / 2.0f, (start.y + end.y) / 2.0f)

    val minX: Float get() = minOf(start.x, end.x)
    val maxX: Float get() = maxOf(start.x, end.x)
    val minY: Float get() = minOf(start.y, end.y)
    val maxY: Float get() = maxOf(start.y, end.y)

    val length: Float
        get() {
            val dx = end.x - start.x
            val dy = end.y - start.y
            return kotlin.math.hypot(dx, dy)
        }
}

data class TransformedDetectionOverlay(
    val polygon: TransformedPolygon,
    val labelAnchor: PreviewPoint,
    val labelRotationDegrees: Float,
    val selectedEdge: PolygonEdge
)
