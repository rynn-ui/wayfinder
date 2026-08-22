package com.roadguardian.app.ai.inference

import com.roadguardian.app.domain.model.HazardType
import com.roadguardian.app.domain.model.RoadHazardDetection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class RoadHazardDetectorTest {

    @Test
    fun int8ModelAsset_existsInMainAssets_andHasValidFlatBufferHeader() {
        val modelFile = resolveFile(
            "src/main/assets/${AiModelType.INT8.assetPath}",
            "app/src/main/assets/${AiModelType.INT8.assetPath}"
        )
        assertTrue("INT8 model file must exist", modelFile.exists())
        assertTrue("INT8 model file size should be > 2MB", modelFile.length() > 2_000_000L)

        val header = ByteArray(8)
        FileInputStream(modelFile).use { stream ->
            stream.read(header)
        }

        val identifier = String(header, 4, 4, Charsets.US_ASCII)
        assertEquals("TFL3", identifier)
    }

    @Test
    fun fp16ModelAsset_existsInMainAssets_andHasValidFlatBufferHeader() {
        val modelFile = resolveFile(
            "src/main/assets/${AiModelType.FP16.assetPath}",
            "app/src/main/assets/${AiModelType.FP16.assetPath}"
        )
        assertTrue("FP16 model file must exist", modelFile.exists())
        assertTrue("FP16 model file size should be > 4MB", modelFile.length() > 4_000_000L)

        val header = ByteArray(8)
        FileInputStream(modelFile).use { stream ->
            stream.read(header)
        }

        val identifier = String(header, 4, 4, Charsets.US_ASCII)
        assertEquals("TFL3", identifier)
    }

    @Test
    fun aiModelType_enumEntries_haveConsistentNamesAndDescriptions() {
        assertEquals("INT8", AiModelType.INT8.displayName)
        assertEquals("FP16", AiModelType.FP16.displayName)
        assertTrue(AiModelType.INT8.precisionLabel.contains("2.83"))
        assertTrue(AiModelType.FP16.precisionLabel.contains("5.08"))
    }

    @Test
    fun aiBenchmarkTracker_warmupAndFpsCalculation_worksCorrectly() {
        val tracker = AiBenchmarkTracker(initialModel = AiModelType.INT8, warmupTargetFrames = 3, rollingWindowSize = 5)

        // Warmup frame 1
        var snap = tracker.recordInference(50f, emptyList())
        assertTrue(snap.isWarmingUp)
        assertEquals(2, snap.warmupRemaining)
        assertEquals(0L, snap.framesProcessed)

        // Warmup frame 2 & 3
        tracker.recordInference(50f, emptyList())
        snap = tracker.recordInference(50f, emptyList())
        assertFalse(snap.isWarmingUp)
        assertEquals(0, snap.warmupRemaining)
        assertEquals(0L, snap.framesProcessed)

        // Post-warmup frame 1 with detection
        val mockDetection = RoadHazardDetection(
            id = "d1",
            hazardType = HazardType.POTHOLE,
            confidence = 0.60f,
            boundingBox = null,
            timestamp = 1000L
        )
        snap = tracker.recordInference(40f, listOf(mockDetection))
        assertEquals(1L, snap.framesProcessed)
        assertEquals(1L, snap.framesWithDetection)
        assertEquals(1L, snap.totalDetections)
        assertEquals(0.60f, snap.currentAvgConfidence, 0.001f)
        assertEquals(0.60f, snap.currentMaxConfidence, 0.001f)
        assertEquals(40f, snap.rollingAvgLatencyMs, 0.001f)
        assertEquals(25.0f, snap.inferenceFps, 0.001f)

        // Switch model
        tracker.startModelSwitch(AiModelType.FP16)
        snap = tracker.getSnapshot()
        assertTrue(snap.isLoading)
        assertEquals(AiModelType.FP16, snap.activeModel)
        assertEquals(0L, snap.framesProcessed)

        tracker.completeModelSwitch()
        snap = tracker.getSnapshot()
        assertFalse(snap.isLoading)
        assertTrue(snap.isWarmingUp)
    }

    @Test
    fun detector_runsInferenceWithRunner_andProducesPotholeDetection() {
        val simulatedRunner = TfliteRunner { input, output ->
            assertTrue(input.capacity() == 4915200)
            output[0][0][0] = 320.0f
            output[0][1][0] = 378.0f
            output[0][2][0] = 171.0f
            output[0][3][0] = 50.0f
            output[0][7][0] = 0.35f
        }

        val detector = RoadHazardDetector.fromRunner(simulatedRunner, AiModelType.INT8)
        val pixels = IntArray(1024 * 1024) { 0x555555 }

        val detections = detector.detect(pixels, 1024, 1024)
        assertEquals(1, detections.size)

        val detection = detections[0]
        assertEquals(HazardType.POTHOLE, detection.hazardType)
        assertEquals(0.35f, detection.confidence, 0.001f)

        val bbox = detection.boundingBox
        assertNotNull(bbox)
        assertTrue(bbox!!.x > 0.0f)
        assertTrue(bbox.y > 0.0f)
        assertTrue(bbox.width > 0.0f)
        assertTrue(bbox.height > 0.0f)
        assertTrue(bbox.area > 0.0f)
    }

    @Test
    fun detector_runsInferenceWithRunner_andProducesCrackDetection() {
        val simulatedRunner = TfliteRunner { input, output ->
            assertTrue(input.capacity() == 4915200)
            output[0][0][0] = 320.0f
            output[0][1][0] = 300.0f
            output[0][2][0] = 200.0f
            output[0][3][0] = 200.0f
            output[0][6][0] = 0.82f
        }

        val detector = RoadHazardDetector.fromRunner(simulatedRunner, AiModelType.FP16)
        val pixels = IntArray(1024 * 1024) { 0x333333 }

        val detections = detector.detect(pixels, 1024, 1024)
        assertEquals(1, detections.size)

        val detection = detections[0]
        assertEquals(HazardType.ALLIGATOR_CRACK, detection.hazardType)
        assertEquals(0.82f, detection.confidence, 0.001f)

        val bbox = detection.boundingBox
        assertNotNull(bbox)
        assertTrue(bbox!!.area > 0.0f)
    }

    @Test
    fun detector_preprocessedBufferDirectCall_executesCorrectly() {
        var runInvoked = false
        val runner = TfliteRunner { _, output ->
            runInvoked = true
            output[0][7][0] = 0.60f
            output[0][0][0] = 100.0f
            output[0][1][0] = 100.0f
            output[0][2][0] = 50.0f
            output[0][3][0] = 50.0f
        }

        val detector = RoadHazardDetector.fromRunner(runner)
        val buffer = detector.preprocessor.createDirectByteBuffer()

        val detections = detector.detect(buffer, 640, 640)
        assertTrue(runInvoked)
        assertEquals(1, detections.size)
        assertEquals(HazardType.POTHOLE, detections[0].hazardType)
    }

    private fun resolveFile(vararg candidatePaths: String): File {
        for (path in candidatePaths) {
            val file = File(path)
            if (file.exists()) return file
        }
        return File(candidatePaths[0])
    }
}
