package com.roadguardian.app.ui.overlay

import com.roadguardian.app.domain.model.BoundingBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class CoordinateTransformerTest {

    private val delta = 0.001f

    private fun distance(p1: PreviewPoint, p2: PreviewPoint): Float {
        return hypot(p1.x - p2.x, p1.y - p2.y)
    }

    // ==========================================
    // ROTATE POINT TESTS (0, 90, 180, 270)
    // ==========================================

    @Test
    fun rotatePoint_0degrees_remainsSame() {
        val (pt, dims) = CoordinateTransformer.rotatePoint(100.0f, 150.0f, 640.0f, 480.0f, 0)
        assertEquals(100.0f, pt.x, delta)
        assertEquals(150.0f, pt.y, delta)
        assertEquals(640.0f, dims.first, delta)
        assertEquals(480.0f, dims.second, delta)
    }

    @Test
    fun rotatePoint_90degrees_rotatesAndTranslates() {
        // (x, y) -> (imageHeight - y, x)
        val (pt, dims) = CoordinateTransformer.rotatePoint(100.0f, 150.0f, 640.0f, 480.0f, 90)
        assertEquals(480.0f - 150.0f, pt.x, delta)
        assertEquals(100.0f, pt.y, delta)
        assertEquals(480.0f, dims.first, delta)
        assertEquals(640.0f, dims.second, delta)
    }

    @Test
    fun rotatePoint_180degrees_invertsBothAxes() {
        // (x, y) -> (imageWidth - x, imageHeight - y)
        val (pt, dims) = CoordinateTransformer.rotatePoint(100.0f, 150.0f, 640.0f, 480.0f, 180)
        assertEquals(640.0f - 100.0f, pt.x, delta)
        assertEquals(480.0f - 150.0f, pt.y, delta)
        assertEquals(640.0f, dims.first, delta)
        assertEquals(480.0f, dims.second, delta)
    }

    @Test
    fun rotatePoint_270degrees_rotatesAndTranslates() {
        // (x, y) -> (y, imageWidth - x)
        val (pt, dims) = CoordinateTransformer.rotatePoint(100.0f, 150.0f, 640.0f, 480.0f, 270)
        assertEquals(150.0f, pt.x, delta)
        assertEquals(640.0f - 100.0f, pt.y, delta)
        assertEquals(480.0f, dims.first, delta)
        assertEquals(640.0f, dims.second, delta)
    }

    // ==========================================
    // TRANSFORM TO POLYGON (NON-SQUARE BOUNDING BOX)
    // ==========================================

    @Test
    fun transformToPolygon_0degrees_preservesCornersOrderingAndCenter() {
        val bbox = BoundingBox(x = 100.0f, y = 150.0f, width = 200.0f, height = 80.0f)
        val polygon = CoordinateTransformer.transformToPolygon(
            boundingBox = bbox,
            imageWidth = 640.0f,
            imageHeight = 480.0f,
            viewWidth = 1280.0f,
            viewHeight = 960.0f,
            relativeRotationDegrees = 0
        )

        // Scale = 2.0, OffsetX = 0, OffsetY = 0
        assertEquals(200.0f, polygon.topLeft.x, delta)
        assertEquals(300.0f, polygon.topLeft.y, delta)
        assertEquals(600.0f, polygon.topRight.x, delta)
        assertEquals(300.0f, polygon.topRight.y, delta)
        assertEquals(600.0f, polygon.bottomRight.x, delta)
        assertEquals(460.0f, polygon.bottomRight.y, delta)
        assertEquals(200.0f, polygon.bottomLeft.x, delta)
        assertEquals(460.0f, polygon.bottomLeft.y, delta)

        // Center consistency
        assertEquals(400.0f, polygon.center.x, delta)
        assertEquals(380.0f, polygon.center.y, delta)

        // Geometry preservation
        assertEquals(400.0f, distance(polygon.topLeft, polygon.topRight), delta)
        assertEquals(160.0f, distance(polygon.topRight, polygon.bottomRight), delta)
        assertEquals(400.0f, distance(polygon.bottomRight, polygon.bottomLeft), delta)
        assertEquals(160.0f, distance(polygon.bottomLeft, polygon.topLeft), delta)

        // Positive bounds
        assertTrue(polygon.boundingWidth > 0)
        assertTrue(polygon.boundingHeight > 0)
        assertEquals(400.0f, polygon.boundingWidth, delta)
        assertEquals(160.0f, polygon.boundingHeight, delta)
    }

    @Test
    fun transformToPolygon_90degrees_rotatesCornersAndPreservesCenter() {
        val bbox = BoundingBox(x = 100.0f, y = 150.0f, width = 200.0f, height = 80.0f)
        val polygon = CoordinateTransformer.transformToPolygon(
            boundingBox = bbox,
            imageWidth = 640.0f,
            imageHeight = 480.0f,
            viewWidth = 960.0f,
            viewHeight = 1280.0f,
            relativeRotationDegrees = 90
        )

        // Effective rotated image dims: 480 x 640. View: 960 x 1280 -> scale = 2.0
        // Point (x, y) -> (480 - y, x) -> * 2.0
        // TL (100, 150) -> (330, 100) -> (660, 200)
        // TR (300, 150) -> (330, 300) -> (660, 600)
        // BR (300, 230) -> (250, 300) -> (500, 600)
        // BL (100, 230) -> (250, 100) -> (500, 200)
        assertEquals(660.0f, polygon.topLeft.x, delta)
        assertEquals(200.0f, polygon.topLeft.y, delta)
        assertEquals(660.0f, polygon.topRight.x, delta)
        assertEquals(600.0f, polygon.topRight.y, delta)
        assertEquals(500.0f, polygon.bottomRight.x, delta)
        assertEquals(600.0f, polygon.bottomRight.y, delta)
        assertEquals(500.0f, polygon.bottomLeft.x, delta)
        assertEquals(200.0f, polygon.bottomLeft.y, delta)

        // Center consistency: original center (200, 190) -> rotated (290, 200) -> canvas (580, 400)
        assertEquals(580.0f, polygon.center.x, delta)
        assertEquals(400.0f, polygon.center.y, delta)

        // Geometry preservation
        assertEquals(400.0f, distance(polygon.topLeft, polygon.topRight), delta)
        assertEquals(160.0f, distance(polygon.topRight, polygon.bottomRight), delta)
        assertEquals(400.0f, distance(polygon.bottomRight, polygon.bottomLeft), delta)
        assertEquals(160.0f, distance(polygon.bottomLeft, polygon.topLeft), delta)

        // Positive bounds
        assertEquals(160.0f, polygon.boundingWidth, delta)
        assertEquals(400.0f, polygon.boundingHeight, delta)
    }

    @Test
    fun transformToPolygon_180degrees_invertsAxesAndPreservesCenter() {
        val bbox = BoundingBox(x = 100.0f, y = 150.0f, width = 200.0f, height = 80.0f)
        val polygon = CoordinateTransformer.transformToPolygon(
            boundingBox = bbox,
            imageWidth = 640.0f,
            imageHeight = 480.0f,
            viewWidth = 1280.0f,
            viewHeight = 960.0f,
            relativeRotationDegrees = 180
        )

        // Effective rotated image dims: 640 x 480. View: 1280 x 960 -> scale = 2.0
        // Point (x, y) -> (640 - x, 480 - y) -> * 2.0
        // TL (100, 150) -> (540, 330) -> (1080, 660)
        // TR (300, 150) -> (340, 330) -> (680, 660)
        // BR (300, 230) -> (340, 250) -> (680, 500)
        // BL (100, 230) -> (540, 250) -> (1080, 500)
        assertEquals(1080.0f, polygon.topLeft.x, delta)
        assertEquals(660.0f, polygon.topLeft.y, delta)
        assertEquals(680.0f, polygon.topRight.x, delta)
        assertEquals(660.0f, polygon.topRight.y, delta)
        assertEquals(680.0f, polygon.bottomRight.x, delta)
        assertEquals(500.0f, polygon.bottomRight.y, delta)
        assertEquals(1080.0f, polygon.bottomLeft.x, delta)
        assertEquals(500.0f, polygon.bottomLeft.y, delta)

        // Center consistency: original center (200, 190) -> rotated (440, 290) -> canvas (880, 580)
        assertEquals(880.0f, polygon.center.x, delta)
        assertEquals(580.0f, polygon.center.y, delta)

        // Geometry preservation
        assertEquals(400.0f, distance(polygon.topLeft, polygon.topRight), delta)
        assertEquals(160.0f, distance(polygon.topRight, polygon.bottomRight), delta)
        assertEquals(400.0f, distance(polygon.bottomRight, polygon.bottomLeft), delta)
        assertEquals(160.0f, distance(polygon.bottomLeft, polygon.topLeft), delta)

        // Positive bounds
        assertEquals(400.0f, polygon.boundingWidth, delta)
        assertEquals(160.0f, polygon.boundingHeight, delta)
    }

    @Test
    fun transformToPolygon_270degrees_rotatesCornersAndPreservesCenter() {
        val bbox = BoundingBox(x = 100.0f, y = 150.0f, width = 200.0f, height = 80.0f)
        val polygon = CoordinateTransformer.transformToPolygon(
            boundingBox = bbox,
            imageWidth = 640.0f,
            imageHeight = 480.0f,
            viewWidth = 960.0f,
            viewHeight = 1280.0f,
            relativeRotationDegrees = 270
        )

        // Effective rotated image dims: 480 x 640. View: 960 x 1280 -> scale = 2.0
        // Point (x, y) -> (y, 640 - x) -> * 2.0
        // TL (100, 150) -> (150, 540) -> (300, 1080)
        // TR (300, 150) -> (150, 340) -> (300, 680)
        // BR (300, 230) -> (230, 340) -> (460, 680)
        // BL (100, 230) -> (230, 540) -> (460, 1080)
        assertEquals(300.0f, polygon.topLeft.x, delta)
        assertEquals(1080.0f, polygon.topLeft.y, delta)
        assertEquals(300.0f, polygon.topRight.x, delta)
        assertEquals(680.0f, polygon.topRight.y, delta)
        assertEquals(460.0f, polygon.bottomRight.x, delta)
        assertEquals(680.0f, polygon.bottomRight.y, delta)
        assertEquals(460.0f, polygon.bottomLeft.x, delta)
        assertEquals(1080.0f, polygon.bottomLeft.y, delta)

        // Center consistency: original center (200, 190) -> rotated (190, 440) -> canvas (380, 880)
        assertEquals(380.0f, polygon.center.x, delta)
        assertEquals(880.0f, polygon.center.y, delta)

        // Geometry preservation
        assertEquals(400.0f, distance(polygon.topLeft, polygon.topRight), delta)
        assertEquals(160.0f, distance(polygon.topRight, polygon.bottomRight), delta)
        assertEquals(400.0f, distance(polygon.bottomRight, polygon.bottomLeft), delta)
        assertEquals(160.0f, distance(polygon.bottomLeft, polygon.topLeft), delta)

        // Positive bounds
        assertEquals(160.0f, polygon.boundingWidth, delta)
        assertEquals(400.0f, polygon.boundingHeight, delta)
    }

    // ==========================================
    // BACKWARD COMPATIBILITY / ENCLOSING RECT TESTS
    // ==========================================

    @Test
    fun transform_sameAspectRatio_scalesLinearly() {
        val bbox = BoundingBox(100.0f, 100.0f, 200.0f, 200.0f)
        val rect = CoordinateTransformer.transform(
            boundingBox = bbox,
            imageWidth = 640.0f,
            imageHeight = 640.0f,
            viewWidth = 1280.0f,
            viewHeight = 1280.0f
        )

        assertEquals(200.0f, rect.left, delta)
        assertEquals(200.0f, rect.top, delta)
        assertEquals(600.0f, rect.right, delta)
        assertEquals(600.0f, rect.bottom, delta)
    }

    @Test
    fun transform_tallView_fillCenter_cropsHorizontally() {
        val bbox = BoundingBox(240.0f, 320.0f, 100.0f, 100.0f)
        val rect = CoordinateTransformer.transform(
            boundingBox = bbox,
            imageWidth = 480.0f,
            imageHeight = 640.0f,
            viewWidth = 1080.0f,
            viewHeight = 2400.0f
        )

        assertEquals(540.0f, rect.left, delta)
        assertEquals(1200.0f, rect.top, delta)
        assertEquals(915.0f, rect.right, delta)
        assertEquals(1575.0f, rect.bottom, delta)
    }

    @Test
    fun transform_wideView_fillCenter_cropsVertically() {
        val bbox = BoundingBox(240.0f, 320.0f, 100.0f, 100.0f)
        val rect = CoordinateTransformer.transform(
            boundingBox = bbox,
            imageWidth = 480.0f,
            imageHeight = 640.0f,
            viewWidth = 1080.0f,
            viewHeight = 1080.0f
        )

        assertEquals(540.0f, rect.left, delta)
        assertEquals(540.0f, rect.top, delta)
        assertEquals(765.0f, rect.right, delta)
        assertEquals(765.0f, rect.bottom, delta)
    }

    @Test
    fun transform_landscapeScreen_cropsVerticallyAndCenters() {
        val bbox = BoundingBox(320.0f, 240.0f, 100.0f, 80.0f)
        val rect = CoordinateTransformer.transform(
            boundingBox = bbox,
            imageWidth = 640.0f,
            imageHeight = 480.0f,
            viewWidth = 2400.0f,
            viewHeight = 1080.0f
        )

        assertEquals(1200.0f, rect.left, delta)
        assertEquals(540.0f, rect.top, delta)
        assertEquals(1575.0f, rect.right, delta)
        assertEquals(840.0f, rect.bottom, delta)
        assertEquals(375.0f, rect.width, delta)
        assertEquals(300.0f, rect.height, delta)
    }

    @Test
    fun transform_withRelativeRotation90_rotatesAndScales() {
        val bbox = BoundingBox(270.0f, 200.0f, 100.0f, 80.0f)
        val rect = CoordinateTransformer.transform(
            boundingBox = bbox,
            imageWidth = 640.0f,
            imageHeight = 480.0f,
            viewWidth = 1080.0f,
            viewHeight = 2400.0f,
            relativeRotationDegrees = 90
        )

        val (rotatedBox, _) = CoordinateTransformer.rotateBoundingBox(bbox, 640.0f, 480.0f, 90)
        assertEquals(200.0f, rotatedBox.x, delta)
        assertEquals(270.0f, rotatedBox.y, delta)
        assertEquals(80.0f, rotatedBox.width, delta)
        assertEquals(100.0f, rotatedBox.height, delta)

        assertEquals(390.0f, rect.left, delta)
        assertEquals(1012.5f, rect.top, delta)
    }

    @Test
    fun rotateBoundingBox_handlesAllAngles() {
        val bbox = BoundingBox(10.0f, 20.0f, 30.0f, 40.0f)
        val (box0, dims0) = CoordinateTransformer.rotateBoundingBox(bbox, 640.0f, 480.0f, 0)
        assertEquals(10.0f, box0.x, delta)
        assertEquals(20.0f, box0.y, delta)
        assertEquals(640.0f, dims0.first, delta)
        assertEquals(480.0f, dims0.second, delta)

        val (box90, dims90) = CoordinateTransformer.rotateBoundingBox(bbox, 640.0f, 480.0f, 90)
        assertEquals(420.0f, box90.x, delta)
        assertEquals(10.0f, box90.y, delta)
        assertEquals(480.0f, dims90.first, delta)
        assertEquals(640.0f, dims90.second, delta)

        val (box180, dims180) = CoordinateTransformer.rotateBoundingBox(bbox, 640.0f, 480.0f, 180)
        assertEquals(600.0f, box180.x, delta)
        assertEquals(420.0f, box180.y, delta)
        assertEquals(640.0f, dims180.first, delta)
        assertEquals(480.0f, dims180.second, delta)

        val (box270, dims270) = CoordinateTransformer.rotateBoundingBox(bbox, 640.0f, 480.0f, 270)
        assertEquals(20.0f, box270.x, delta)
        assertEquals(600.0f, box270.y, delta)
        assertEquals(480.0f, dims270.first, delta)
        assertEquals(640.0f, dims270.second, delta)
    }

    @Test
    fun transform_invalidDimensions_returnsZeroRect() {
        val bbox = BoundingBox(10.0f, 10.0f, 20.0f, 20.0f)
        val rect = CoordinateTransformer.transform(
            boundingBox = bbox,
            imageWidth = 0.0f,
            imageHeight = 640.0f,
            viewWidth = 1080.0f,
            viewHeight = 2400.0f
        )

        assertEquals(0.0f, rect.left, delta)
        assertEquals(0.0f, rect.top, delta)
        assertEquals(0.0f, rect.right, delta)
        assertEquals(0.0f, rect.bottom, delta)
    }

    @Test
    fun transformToPolygon_invalidDimensions_returnsZeroPolygon() {
        val bbox = BoundingBox(10.0f, 10.0f, 20.0f, 20.0f)
        val polygon = CoordinateTransformer.transformToPolygon(
            boundingBox = bbox,
            imageWidth = 640.0f,
            imageHeight = 0.0f,
            viewWidth = 1080.0f,
            viewHeight = 2400.0f
        )

        assertEquals(0.0f, polygon.topLeft.x, delta)
        assertEquals(0.0f, polygon.topLeft.y, delta)
        assertEquals(0.0f, polygon.topRight.x, delta)
        assertEquals(0.0f, polygon.topRight.y, delta)
        assertEquals(0.0f, polygon.bottomRight.x, delta)
        assertEquals(0.0f, polygon.bottomRight.y, delta)
        assertEquals(0.0f, polygon.bottomLeft.x, delta)
        assertEquals(0.0f, polygon.bottomLeft.y, delta)
        assertEquals(0.0f, polygon.boundingWidth, delta)
        assertEquals(0.0f, polygon.boundingHeight, delta)
    }

    // ==========================================
    // VISUAL TOP EDGE & LABEL ANCHOR TESTS
    // ==========================================

    @Test
    fun findVisualTopEdge_0degrees_selectsTopEdge() {
        val polygon = TransformedPolygon(
            topLeft = PreviewPoint(100.0f, 200.0f),
            topRight = PreviewPoint(300.0f, 200.0f),
            bottomRight = PreviewPoint(300.0f, 400.0f),
            bottomLeft = PreviewPoint(100.0f, 400.0f)
        )
        val topEdge = CoordinateTransformer.findVisualTopEdge(polygon)
        assertEquals(0, topEdge.edgeIndex) // Edge 0: TL -> TR
        assertEquals(200.0f, topEdge.midpoint.y, delta)
    }

    @Test
    fun findVisualTopEdge_90degrees_selectsLeftEdgeAsVisualTop() {
        // Transformed after 90° CW rotation
        val polygon = TransformedPolygon(
            topLeft = PreviewPoint(430.0f, 100.0f),
            topRight = PreviewPoint(430.0f, 300.0f),
            bottomRight = PreviewPoint(330.0f, 300.0f),
            bottomLeft = PreviewPoint(330.0f, 100.0f)
        )
        val topEdge = CoordinateTransformer.findVisualTopEdge(polygon)
        assertEquals(3, topEdge.edgeIndex) // Edge 3: BL -> TL
        assertEquals(100.0f, topEdge.midpoint.y, delta)
    }

    @Test
    fun findVisualTopEdge_180degrees_selectsBottomEdgeAsVisualTop() {
        // Transformed after 180° rotation
        val polygon = TransformedPolygon(
            topLeft = PreviewPoint(300.0f, 400.0f),
            topRight = PreviewPoint(100.0f, 400.0f),
            bottomRight = PreviewPoint(100.0f, 200.0f),
            bottomLeft = PreviewPoint(300.0f, 200.0f)
        )
        val topEdge = CoordinateTransformer.findVisualTopEdge(polygon)
        assertEquals(2, topEdge.edgeIndex) // Edge 2: BR -> BL
        assertEquals(200.0f, topEdge.midpoint.y, delta)
    }

    @Test
    fun findVisualTopEdge_270degrees_selectsRightEdgeAsVisualTop() {
        // Transformed after 270° rotation
        val polygon = TransformedPolygon(
            topLeft = PreviewPoint(100.0f, 400.0f),
            topRight = PreviewPoint(100.0f, 200.0f),
            bottomRight = PreviewPoint(300.0f, 200.0f),
            bottomLeft = PreviewPoint(300.0f, 400.0f)
        )
        val topEdge = CoordinateTransformer.findVisualTopEdge(polygon)
        assertEquals(1, topEdge.edgeIndex) // Edge 1: TR -> BR
        assertEquals(200.0f, topEdge.midpoint.y, delta)
    }

    @Test
    fun calculateLabelAnchor_0degrees_positionsAboveTopEdge() {
        val polygon = TransformedPolygon(
            topLeft = PreviewPoint(100.0f, 200.0f),
            topRight = PreviewPoint(300.0f, 200.0f),
            bottomRight = PreviewPoint(300.0f, 400.0f),
            bottomLeft = PreviewPoint(100.0f, 400.0f)
        )
        val anchor = CoordinateTransformer.calculateLabelAnchor(
            polygon = polygon,
            labelWidth = 120.0f,
            labelHeight = 30.0f,
            viewWidth = 1080.0f,
            viewHeight = 1920.0f,
            gap = 4.0f
        )
        assertEquals(100.0f, anchor.x, delta)
        assertEquals(166.0f, anchor.y, delta) // 200 - 30 - 4
    }

    @Test
    fun calculateLabelAnchor_90degrees_positionsAboveVisualTopEdge() {
        val polygon = TransformedPolygon(
            topLeft = PreviewPoint(430.0f, 100.0f),
            topRight = PreviewPoint(430.0f, 300.0f),
            bottomRight = PreviewPoint(330.0f, 300.0f),
            bottomLeft = PreviewPoint(330.0f, 100.0f)
        )
        val anchor = CoordinateTransformer.calculateLabelAnchor(
            polygon = polygon,
            labelWidth = 120.0f,
            labelHeight = 30.0f,
            viewWidth = 1080.0f,
            viewHeight = 1920.0f,
            gap = 4.0f
        )
        assertEquals(330.0f, anchor.x, delta) // minX of Edge 3 (BL->TL)
        assertEquals(66.0f, anchor.y, delta) // 100 - 30 - 4
    }

    @Test
    fun calculateLabelAnchor_180degrees_positionsAboveVisualTopEdge() {
        val polygon = TransformedPolygon(
            topLeft = PreviewPoint(300.0f, 400.0f),
            topRight = PreviewPoint(100.0f, 400.0f),
            bottomRight = PreviewPoint(100.0f, 200.0f),
            bottomLeft = PreviewPoint(300.0f, 200.0f)
        )
        val anchor = CoordinateTransformer.calculateLabelAnchor(
            polygon = polygon,
            labelWidth = 120.0f,
            labelHeight = 30.0f,
            viewWidth = 1080.0f,
            viewHeight = 1920.0f,
            gap = 4.0f
        )
        assertEquals(100.0f, anchor.x, delta) // minX of Edge 2 (BR->BL)
        assertEquals(166.0f, anchor.y, delta) // 200 - 30 - 4
    }

    @Test
    fun calculateLabelAnchor_270degrees_positionsAboveVisualTopEdge() {
        val polygon = TransformedPolygon(
            topLeft = PreviewPoint(100.0f, 400.0f),
            topRight = PreviewPoint(100.0f, 200.0f),
            bottomRight = PreviewPoint(300.0f, 200.0f),
            bottomLeft = PreviewPoint(300.0f, 400.0f)
        )
        val anchor = CoordinateTransformer.calculateLabelAnchor(
            polygon = polygon,
            labelWidth = 120.0f,
            labelHeight = 30.0f,
            viewWidth = 1080.0f,
            viewHeight = 1920.0f,
            gap = 4.0f
        )
        assertEquals(100.0f, anchor.x, delta) // minX of Edge 1 (TR->BR)
        assertEquals(166.0f, anchor.y, delta) // 200 - 30 - 4
    }

    @Test
    fun calculateLabelAnchor_tallPolygon_correctlyAnchored() {
        val polygon = TransformedPolygon(
            topLeft = PreviewPoint(200.0f, 100.0f),
            topRight = PreviewPoint(250.0f, 100.0f),
            bottomRight = PreviewPoint(250.0f, 800.0f),
            bottomLeft = PreviewPoint(200.0f, 800.0f)
        )
        val anchor = CoordinateTransformer.calculateLabelAnchor(
            polygon = polygon,
            labelWidth = 120.0f,
            labelHeight = 30.0f,
            viewWidth = 1080.0f,
            viewHeight = 1920.0f,
            gap = 4.0f
        )
        assertEquals(200.0f, anchor.x, delta)
        assertEquals(66.0f, anchor.y, delta)
    }

    @Test
    fun calculateLabelAnchor_widePolygon_correctlyAnchored() {
        val polygon = TransformedPolygon(
            topLeft = PreviewPoint(50.0f, 300.0f),
            topRight = PreviewPoint(800.0f, 300.0f),
            bottomRight = PreviewPoint(800.0f, 350.0f),
            bottomLeft = PreviewPoint(50.0f, 350.0f)
        )
        val anchor = CoordinateTransformer.calculateLabelAnchor(
            polygon = polygon,
            labelWidth = 120.0f,
            labelHeight = 30.0f,
            viewWidth = 1080.0f,
            viewHeight = 1920.0f,
            gap = 4.0f
        )
        assertEquals(50.0f, anchor.x, delta)
        assertEquals(266.0f, anchor.y, delta)
    }

    @Test
    fun calculateLabelAnchor_nearTopScreenEdge_flipsInside() {
        val polygon = TransformedPolygon(
            topLeft = PreviewPoint(100.0f, 10.0f),
            topRight = PreviewPoint(300.0f, 10.0f),
            bottomRight = PreviewPoint(300.0f, 200.0f),
            bottomLeft = PreviewPoint(100.0f, 200.0f)
        )
        val anchor = CoordinateTransformer.calculateLabelAnchor(
            polygon = polygon,
            labelWidth = 120.0f,
            labelHeight = 30.0f,
            viewWidth = 1080.0f,
            viewHeight = 1920.0f,
            gap = 4.0f
        )
        assertEquals(100.0f, anchor.x, delta)
        assertEquals(14.0f, anchor.y, delta) // 10 + 4 = 14 (flipped inside top edge)
    }

    @Test
    fun calculateLabelAnchor_nearRightScreenEdge_clampsWithinBounds() {
        val polygon = TransformedPolygon(
            topLeft = PreviewPoint(1000.0f, 200.0f),
            topRight = PreviewPoint(1070.0f, 200.0f),
            bottomRight = PreviewPoint(1070.0f, 400.0f),
            bottomLeft = PreviewPoint(1000.0f, 400.0f)
        )
        val anchor = CoordinateTransformer.calculateLabelAnchor(
            polygon = polygon,
            labelWidth = 120.0f,
            labelHeight = 30.0f,
            viewWidth = 1080.0f,
            viewHeight = 1920.0f,
            gap = 4.0f
        )
        assertEquals(960.0f, anchor.x, delta) // 1080 - 120 = 960 (clamped within view)
        assertEquals(166.0f, anchor.y, delta)
    }

    @Test
    fun calculateLabelAnchor_nearLeftScreenEdge_clampsWithinBounds() {
        val polygon = TransformedPolygon(
            topLeft = PreviewPoint(-20.0f, 200.0f),
            topRight = PreviewPoint(80.0f, 200.0f),
            bottomRight = PreviewPoint(80.0f, 400.0f),
            bottomLeft = PreviewPoint(-20.0f, 400.0f)
        )
        val anchor = CoordinateTransformer.calculateLabelAnchor(
            polygon = polygon,
            labelWidth = 120.0f,
            labelHeight = 30.0f,
            viewWidth = 1080.0f,
            viewHeight = 1920.0f,
            gap = 4.0f
        )
        assertEquals(0.0f, anchor.x, delta) // Clamped to left border 0.0
        assertEquals(166.0f, anchor.y, delta)
    }

    @Test
    fun calculateLabelAnchor_zeroViewDimensions_returnsZero() {
        val polygon = TransformedPolygon(
            topLeft = PreviewPoint(100.0f, 200.0f),
            topRight = PreviewPoint(300.0f, 200.0f),
            bottomRight = PreviewPoint(300.0f, 400.0f),
            bottomLeft = PreviewPoint(100.0f, 400.0f)
        )
        val anchor = CoordinateTransformer.calculateLabelAnchor(
            polygon = polygon,
            labelWidth = 120.0f,
            labelHeight = 30.0f,
            viewWidth = 0.0f,
            viewHeight = 0.0f
        )
        assertEquals(0.0f, anchor.x, delta)
        assertEquals(0.0f, anchor.y, delta)
    }

    // ==========================================
    // EDGE INDEX FOR ROTATION TESTS
    // ==========================================

    @Test
    fun edgeIndexForRotation_0degrees_returnsE0() {
        assertEquals(0, CoordinateTransformer.edgeIndexForRotation(0))
    }

    @Test
    fun edgeIndexForRotation_90degrees_returnsE3() {
        assertEquals(3, CoordinateTransformer.edgeIndexForRotation(90))
    }

    @Test
    fun edgeIndexForRotation_180degrees_returnsE2() {
        assertEquals(2, CoordinateTransformer.edgeIndexForRotation(180))
    }

    @Test
    fun edgeIndexForRotation_270degrees_returnsE1() {
        assertEquals(1, CoordinateTransformer.edgeIndexForRotation(270))
    }

    @Test
    fun edgeIndexForRotation_negative90_returnsE1() {
        // -90 % 360 = 270 -> E1
        assertEquals(1, CoordinateTransformer.edgeIndexForRotation(-90))
    }

    @Test
    fun edgeIndexForRotation_450_returnsE3() {
        // 450 % 360 = 90 -> E3
        assertEquals(3, CoordinateTransformer.edgeIndexForRotation(450))
    }

    // ==========================================
    // EDGE ANGLE TESTS
    // ==========================================

    @Test
    fun calculateEdgeAngle_horizontalRight_returns0() {
        val edge = PolygonEdge(PreviewPoint(0.0f, 100.0f), PreviewPoint(200.0f, 100.0f), 0)
        assertEquals(0.0f, CoordinateTransformer.calculateEdgeAngle(edge), delta)
    }

    @Test
    fun calculateEdgeAngle_verticalDown_returns90() {
        val edge = PolygonEdge(PreviewPoint(100.0f, 0.0f), PreviewPoint(100.0f, 200.0f), 0)
        assertEquals(90.0f, CoordinateTransformer.calculateEdgeAngle(edge), delta)
    }

    @Test
    fun calculateEdgeAngle_horizontalLeft_returns180() {
        val edge = PolygonEdge(PreviewPoint(200.0f, 100.0f), PreviewPoint(0.0f, 100.0f), 0)
        assertEquals(180.0f, CoordinateTransformer.calculateEdgeAngle(edge), delta)
    }

    @Test
    fun calculateEdgeAngle_verticalUp_returnsNeg90() {
        val edge = PolygonEdge(PreviewPoint(100.0f, 200.0f), PreviewPoint(100.0f, 0.0f), 0)
        assertEquals(-90.0f, CoordinateTransformer.calculateEdgeAngle(edge), delta)
    }

    // ==========================================
    // TRANSFORM DETECTION TESTS
    // ==========================================

    @Test
    fun transformDetection_0degrees_selectsE0AndLabelsAbove() {
        val bbox = BoundingBox(x = 100.0f, y = 150.0f, width = 200.0f, height = 80.0f)
        val overlay = CoordinateTransformer.transformDetection(
            boundingBox = bbox,
            imageWidth = 640.0f,
            imageHeight = 480.0f,
            viewWidth = 1280.0f,
            viewHeight = 960.0f,
            relativeRotationDegrees = 0,
            labelWidth = 120.0f,
            labelHeight = 30.0f,
            gap = 4.0f
        )

        assertEquals(0, overlay.selectedEdge.edgeIndex)
        assertEquals(0.0f, overlay.labelRotationDegrees, delta)
        // Label should be above the polygon top edge (Y < 300)
        assertTrue(overlay.labelAnchor.y < 300.0f)
    }

    @Test
    fun transformDetection_90degrees_selectsE3AndLabelsAbove() {
        val bbox = BoundingBox(x = 100.0f, y = 150.0f, width = 200.0f, height = 80.0f)
        val overlay = CoordinateTransformer.transformDetection(
            boundingBox = bbox,
            imageWidth = 640.0f,
            imageHeight = 480.0f,
            viewWidth = 960.0f,
            viewHeight = 1280.0f,
            relativeRotationDegrees = 90,
            labelWidth = 120.0f,
            labelHeight = 30.0f,
            gap = 4.0f
        )

        assertEquals(3, overlay.selectedEdge.edgeIndex)
        // E3 at 90deg is horizontal -> label rotation ~0
        assertEquals(0.0f, overlay.labelRotationDegrees, delta)
    }

    @Test
    fun transformDetection_180degrees_selectsE2() {
        val bbox = BoundingBox(x = 100.0f, y = 150.0f, width = 200.0f, height = 80.0f)
        val overlay = CoordinateTransformer.transformDetection(
            boundingBox = bbox,
            imageWidth = 640.0f,
            imageHeight = 480.0f,
            viewWidth = 1280.0f,
            viewHeight = 960.0f,
            relativeRotationDegrees = 180,
            labelWidth = 120.0f,
            labelHeight = 30.0f,
            gap = 4.0f
        )

        assertEquals(2, overlay.selectedEdge.edgeIndex)
        assertEquals(0.0f, overlay.labelRotationDegrees, delta)
    }

    @Test
    fun transformDetection_270degrees_selectsE1() {
        val bbox = BoundingBox(x = 100.0f, y = 150.0f, width = 200.0f, height = 80.0f)
        val overlay = CoordinateTransformer.transformDetection(
            boundingBox = bbox,
            imageWidth = 640.0f,
            imageHeight = 480.0f,
            viewWidth = 960.0f,
            viewHeight = 1280.0f,
            relativeRotationDegrees = 270,
            labelWidth = 120.0f,
            labelHeight = 30.0f,
            gap = 4.0f
        )

        assertEquals(1, overlay.selectedEdge.edgeIndex)
        assertEquals(0.0f, overlay.labelRotationDegrees, delta)
    }

    // ==========================================
    // LABEL ANCHOR FOR EDGE TESTS
    // ==========================================

    @Test
    fun calculateLabelAnchorForEdge_horizontalEdge_positionsAbove() {
        val polygon = TransformedPolygon(
            topLeft = PreviewPoint(100.0f, 200.0f),
            topRight = PreviewPoint(300.0f, 200.0f),
            bottomRight = PreviewPoint(300.0f, 400.0f),
            bottomLeft = PreviewPoint(100.0f, 400.0f)
        )
        val edge = PolygonEdge(PreviewPoint(100.0f, 200.0f), PreviewPoint(300.0f, 200.0f), 0)
        val anchor = CoordinateTransformer.calculateLabelAnchorForEdge(
            edge = edge,
            polygon = polygon,
            labelWidth = 120.0f,
            labelHeight = 30.0f,
            viewWidth = 1080.0f,
            viewHeight = 1920.0f,
            gap = 4.0f
        )
        // Edge midpoint (200, 200), outward = up (-Y), offset = 15 + 4 = 19
        assertEquals(200.0f, anchor.x, delta)
        assertEquals(181.0f, anchor.y, delta)
    }

    @Test
    fun calculateLabelAnchorForEdge_zeroViewDimensions_returnsZero() {
        val polygon = TransformedPolygon(
            topLeft = PreviewPoint(100.0f, 200.0f),
            topRight = PreviewPoint(300.0f, 200.0f),
            bottomRight = PreviewPoint(300.0f, 400.0f),
            bottomLeft = PreviewPoint(100.0f, 400.0f)
        )
        val edge = PolygonEdge(PreviewPoint(100.0f, 200.0f), PreviewPoint(300.0f, 200.0f), 0)
        val anchor = CoordinateTransformer.calculateLabelAnchorForEdge(
            edge = edge,
            polygon = polygon,
            labelWidth = 120.0f,
            labelHeight = 30.0f,
            viewWidth = 0.0f,
            viewHeight = 0.0f
        )
        assertEquals(0.0f, anchor.x, delta)
        assertEquals(0.0f, anchor.y, delta)
    }

    @Test
    fun calculateLabelAnchorForEdge_verticalEdge_positionsLeft() {
        // After 270 rotation: TR(100,200) -> BR(100,400), a vertical edge
        val polygon = TransformedPolygon(
            topLeft = PreviewPoint(100.0f, 400.0f),
            topRight = PreviewPoint(100.0f, 200.0f),
            bottomRight = PreviewPoint(300.0f, 200.0f),
            bottomLeft = PreviewPoint(300.0f, 400.0f)
        )
        // E1: TR(100,200) -> BR(300,200), horizontal top edge
        val edge = PolygonEdge(PreviewPoint(100.0f, 200.0f), PreviewPoint(300.0f, 200.0f), 1)
        val anchor = CoordinateTransformer.calculateLabelAnchorForEdge(
            edge = edge,
            polygon = polygon,
            labelWidth = 120.0f,
            labelHeight = 30.0f,
            viewWidth = 1080.0f,
            viewHeight = 1920.0f,
            gap = 4.0f
        )
        // Edge midpoint (200, 200), outward = up (-Y)
        assertEquals(200.0f, anchor.x, delta)
        assertEquals(181.0f, anchor.y, delta)
    }

    @Test
    fun calculateLabelAnchorForEdge_nearTopScreenEdge_clampsWithinBounds() {
        val polygon = TransformedPolygon(
            topLeft = PreviewPoint(100.0f, 10.0f),
            topRight = PreviewPoint(300.0f, 10.0f),
            bottomRight = PreviewPoint(300.0f, 200.0f),
            bottomLeft = PreviewPoint(100.0f, 200.0f)
        )
        val edge = PolygonEdge(PreviewPoint(100.0f, 10.0f), PreviewPoint(300.0f, 10.0f), 0)
        val anchor = CoordinateTransformer.calculateLabelAnchorForEdge(
            edge = edge,
            polygon = polygon,
            labelWidth = 120.0f,
            labelHeight = 30.0f,
            viewWidth = 1080.0f,
            viewHeight = 1920.0f,
            gap = 4.0f
        )
        // Would be at 10 - 15 - 4 = -9, but clamped to extentY (15)
        assertEquals(15.0f, anchor.y, delta)
    }

    // ==========================================
    // TRANSFORM DETECTION: EDGE SELECTION PER ORIENTATION
    // ==========================================

    @Test
    fun transformDetection_labelFollowesCorrectEdgePerRotation() {
        val bbox = BoundingBox(x = 100.0f, y = 150.0f, width = 200.0f, height = 80.0f)

        // Verify the label anchor changes meaningfully across rotations
        val anchor0 = CoordinateTransformer.transformDetection(
            bbox, 640f, 480f, 1280f, 960f, 0, 120f, 30f
        )
        val anchor90 = CoordinateTransformer.transformDetection(
            bbox, 640f, 480f, 960f, 1280f, 90, 120f, 30f
        )
        val anchor180 = CoordinateTransformer.transformDetection(
            bbox, 640f, 480f, 1280f, 960f, 180, 120f, 30f
        )
        val anchor270 = CoordinateTransformer.transformDetection(
            bbox, 640f, 480f, 960f, 1280f, 270, 120f, 30f
        )

        // All different -> label moves to a different edge each rotation
        assertTrue(distance(anchor0.labelAnchor, anchor90.labelAnchor) > 1.0f)
        assertTrue(distance(anchor0.labelAnchor, anchor180.labelAnchor) > 1.0f)
        assertTrue(distance(anchor0.labelAnchor, anchor270.labelAnchor) > 1.0f)

        // Different edge indices
        assertEquals(0, anchor0.selectedEdge.edgeIndex)
        assertEquals(3, anchor90.selectedEdge.edgeIndex)
        assertEquals(2, anchor180.selectedEdge.edgeIndex)
        assertEquals(1, anchor270.selectedEdge.edgeIndex)
    }

    @Test
    fun transformDetection_labelStaysWithinViewport_allRotations() {
        val bbox = BoundingBox(x = 10.0f, y = 10.0f, width = 620.0f, height = 460.0f)

        for (rot in listOf(0, 90, 180, 270)) {
            val vw = if (rot == 90 || rot == 270) 960f else 1280f
            val vh = if (rot == 90 || rot == 270) 1280f else 960f
            val overlay = CoordinateTransformer.transformDetection(
                bbox, 640f, 480f, vw, vh, rot, 120f, 30f
            )
            assertTrue(
                "Label anchor at ${rot}deg outside viewport: ${overlay.labelAnchor}",
                overlay.labelAnchor.x in 0.0f..vw &&
                    overlay.labelAnchor.y in 0.0f..vh
            )
        }
    }
}


