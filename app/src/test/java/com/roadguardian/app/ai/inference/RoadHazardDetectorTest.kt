package com.roadguardian.app.ai.inference

import com.roadguardian.app.domain.model.HazardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

class RoadHazardDetectorTest {

    @Test
    fun modelAsset_existsInMainAssets_andHasValidFlatBufferHeader() {
        val modelFile = resolveFile(
            "src/main/assets/yolo12n_seed0_best_dynamic_range_quant.tflite",
            "app/src/main/assets/yolo12n_seed0_best_dynamic_range_quant.tflite"
        )
        assertTrue(modelFile.exists())
        assertTrue(modelFile.length() > 2_000_000L)

        val header = ByteArray(8)
        FileInputStream(modelFile).use { stream ->
            stream.read(header)
        }

        val identifier = String(header, 4, 4, Charsets.US_ASCII)
        assertEquals("TFL3", identifier)
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

        val detector = RoadHazardDetector.fromRunner(simulatedRunner)
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

        val detector = RoadHazardDetector.fromRunner(simulatedRunner)
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
