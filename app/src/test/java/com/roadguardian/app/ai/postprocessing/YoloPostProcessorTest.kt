package com.roadguardian.app.ai.postprocessing

import com.roadguardian.app.domain.model.BoundingBox
import com.roadguardian.app.domain.model.HazardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoloPostProcessorTest {

    @Test
    fun calculateIoU_identicalBoxes_returnsOne() {
        val processor = YoloPostProcessor()
        val box = BoundingBox(10.0f, 10.0f, 50.0f, 50.0f)
        val iou = processor.calculateIoU(box, box)
        assertEquals(1.0f, iou, 0.001f)
    }

    @Test
    fun calculateIoU_nonOverlappingBoxes_returnsZero() {
        val processor = YoloPostProcessor()
        val box1 = BoundingBox(0.0f, 0.0f, 10.0f, 10.0f)
        val box2 = BoundingBox(20.0f, 20.0f, 10.0f, 10.0f)
        val iou = processor.calculateIoU(box1, box2)
        assertEquals(0.0f, iou, 0.001f)
    }

    @Test
    fun calculateIoU_partiallyOverlappingBoxes_computesCorrectly() {
        val processor = YoloPostProcessor()
        val box1 = BoundingBox(0.0f, 0.0f, 20.0f, 20.0f)
        val box2 = BoundingBox(10.0f, 0.0f, 20.0f, 20.0f)
        val iou = processor.calculateIoU(box1, box2)
        assertEquals(200.0f / 600.0f, iou, 0.001f)
    }

    @Test
    fun process_decodesHighConfidencePotholeCorrectly() {
        val processor = YoloPostProcessor(confidenceThreshold = 0.20f)
        val raw = Array(1) { Array(5) { FloatArray(8400) } }

        raw[0][0][0] = 320.0f
        raw[0][1][0] = 320.0f
        raw[0][2][0] = 100.0f
        raw[0][3][0] = 60.0f
        raw[0][4][0] = 0.85f

        val detections = processor.process(raw, originalWidth = 1280, originalHeight = 1280)

        assertEquals(1, detections.size)
        val detection = detections[0]
        assertEquals(HazardType.POTHOLE, detection.hazardType)
        assertEquals(0.85f, detection.confidence, 0.001f)

        val bbox = detection.boundingBox
        assertNotNull(bbox)
        assertEquals(540.0f, bbox!!.x, 0.1f)
        assertEquals(580.0f, bbox.y, 0.1f)
        assertEquals(200.0f, bbox.width, 0.1f)
        assertEquals(120.0f, bbox.height, 0.1f)
    }

    @Test
    fun process_filtersOutLowConfidencePredictions() {
        val processor = YoloPostProcessor(confidenceThreshold = 0.50f)
        val raw = Array(1) { Array(5) { FloatArray(8400) } }

        raw[0][0][0] = 100.0f
        raw[0][1][0] = 100.0f
        raw[0][2][0] = 50.0f
        raw[0][3][0] = 50.0f
        raw[0][4][0] = 0.30f

        val detections = processor.process(raw, originalWidth = 640, originalHeight = 640)
        assertTrue(detections.isEmpty())
    }

    @Test
    fun applyNms_removesOverlappingSameClassDetections() {
        val processor = YoloPostProcessor(iouThreshold = 0.45f)
        val raw = Array(1) { Array(5) { FloatArray(8400) } }

        raw[0][0][0] = 320.0f
        raw[0][1][0] = 320.0f
        raw[0][2][0] = 100.0f
        raw[0][3][0] = 60.0f
        raw[0][4][0] = 0.90f

        raw[0][0][1] = 322.0f
        raw[0][1][1] = 321.0f
        raw[0][2][1] = 98.0f
        raw[0][3][1] = 59.0f
        raw[0][4][1] = 0.75f

        val detections = processor.process(raw, originalWidth = 640, originalHeight = 640)
        assertEquals(1, detections.size)
        assertEquals(0.90f, detections[0].confidence, 0.001f)
    }

    @Test
    fun applyNms_filtersOutNonPotholesInMultiClassMode() {
        val processor = YoloPostProcessor(
            classCount = 4,
            potholeClassId = 3,
            singleClassMode = false,
            iouThreshold = 0.45f,
            classLabels = YoloPostProcessor.DEFAULT_CLASS_LABELS
        )
        val raw = Array(1) { Array(8) { FloatArray(8400) } }

        raw[0][0][0] = 320.0f
        raw[0][1][0] = 320.0f
        raw[0][2][0] = 100.0f
        raw[0][3][0] = 60.0f
        raw[0][7][0] = 0.90f

        raw[0][0][1] = 100.0f
        raw[0][1][1] = 100.0f
        raw[0][2][1] = 50.0f
        raw[0][3][1] = 50.0f
        raw[0][6][1] = 0.85f

        val detections = processor.process(raw, originalWidth = 640, originalHeight = 640)
        assertEquals(1, detections.size)
        assertEquals(HazardType.POTHOLE, detections[0].hazardType)
    }

    @Test
    fun computeRawStats_reportsHighestRawCandidateBeforeFiltering() {
        val processor = YoloPostProcessor(
            classCount = 4,
            potholeClassId = 3,
            singleClassMode = false,
            classLabels = YoloPostProcessor.DEFAULT_CLASS_LABELS
        )
        val raw = Array(1) { Array(8) { FloatArray(8400) } }

        raw[0][0][0] = 320.0f
        raw[0][1][0] = 300.0f
        raw[0][2][0] = 100.0f
        raw[0][3][0] = 60.0f
        raw[0][7][0] = 0.85f

        raw[0][0][1] = 100.0f
        raw[0][1][1] = 100.0f
        raw[0][2][1] = 50.0f
        raw[0][3][1] = 50.0f
        raw[0][6][1] = 0.90f

        val stats = processor.computeRawStats(raw)
        assertNotNull(stats)
        assertEquals(0.90f, stats!!.rawMaxScore, 0.001f)
        assertEquals(2, stats.rawMaxClass)
        assertEquals("alligator_crack", stats.rawMaxClassLabel)
        assertEquals(0.85f, stats.potholeMaxScore, 0.001f)
        assertEquals(1, stats.candidatesAtThreshold)
        assertEquals(100.0f, stats.rawMaxBoxX, 0.001f)
    }

    @Test
    fun computeRawStats_zeroTensor_reportsZeroScoresAndZeroCandidates() {
        val processor = YoloPostProcessor()
        val raw = Array(1) { Array(5) { FloatArray(8400) } }

        val stats = processor.computeRawStats(raw)
        assertNotNull(stats)
        assertEquals(0.0f, stats!!.rawMaxScore, 0.001f)
        assertEquals(0.0f, stats.potholeMaxScore, 0.001f)
        assertEquals(0, stats.candidatesAtThreshold)
        assertEquals(0.0f, stats.tensorMin, 0.001f)
        assertEquals(0.0f, stats.tensorMax, 0.001f)
    }

    @Test
    fun computeRawStats_singleClassMode_usesRowFourAsClassScore() {
        val processor = YoloPostProcessor(singleClassMode = true, classCount = 1)
        val raw = Array(1) { Array(5) { FloatArray(8400) } }

        raw[0][0][0] = 320.0f
        raw[0][1][0] = 320.0f
        raw[0][2][0] = 200.0f
        raw[0][3][0] = 200.0f
        raw[0][4][0] = 0.91f

        val stats = processor.computeRawStats(raw)
        assertNotNull(stats)
        assertEquals(0.91f, stats!!.rawMaxScore, 0.001f)
        assertEquals(1, stats.candidatesAtThreshold)
    }
}
