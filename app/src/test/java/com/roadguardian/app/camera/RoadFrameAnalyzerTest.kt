package com.roadguardian.app.camera

import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import com.roadguardian.app.ai.inference.RoadHazardDetector
import com.roadguardian.app.ai.inference.TfliteRunner
import com.roadguardian.app.domain.model.HazardType
import com.roadguardian.app.domain.model.RoadHazardDetection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.nio.ByteBuffer

class RoadFrameAnalyzerTest {

    @Test
    fun frameMetadata_storesValuesCorrectly() {
        val metadata = FrameMetadata(
            width = 640,
            height = 480,
            rotationDegrees = 90,
            timestamp = 1000L,
            frameNumber = 1L
        )
        assertEquals(640, metadata.width)
        assertEquals(480, metadata.height)
        assertEquals(90, metadata.rotationDegrees)
        assertEquals(1000L, metadata.timestamp)
        assertEquals(1L, metadata.frameNumber)
    }

    @Test
    fun analyze_withoutDetector_invokesFrameAnalyzedAndClosesImageProxy() {
        var analyzedMetadata: FrameMetadata? = null
        var isClosed = false

        val analyzer = RoadFrameAnalyzer(
            detector = null,
            onFrameAnalyzed = { analyzedMetadata = it }
        )

        val imageProxy = createFakeImageProxy(width = 640, height = 480, rotation = 90, timestamp = 2000L) {
            isClosed = true
        }

        analyzer.analyze(imageProxy)

        val meta = analyzedMetadata
        assertNotNull(meta)
        assertEquals(640, meta!!.width)
        assertEquals(480, meta.height)
        assertEquals(90, meta.rotationDegrees)
        assertEquals(2000L, meta.timestamp)
        assertEquals(1L, meta.frameNumber)
        assertTrue(isClosed)
    }

    @Test
    fun analyze_withDetector_invokesInferenceAndDispatchesDetections() {
        var receivedDetections: List<RoadHazardDetection>? = null
        var isClosed = false

        val runner = TfliteRunner { _, output ->
            output[0][7][0] = 0.75f
            output[0][0][0] = 320.0f
            output[0][1][0] = 320.0f
            output[0][2][0] = 100.0f
            output[0][3][0] = 100.0f
        }
        val detector = RoadHazardDetector.fromRunner(runner)

        val analyzer = RoadFrameAnalyzer(
            detector = detector,
            onInferenceResult = { detections, _ ->
                receivedDetections = detections
            }
        )

        val imageProxy = createFakeImageProxy(width = 640, height = 480, rotation = 0, timestamp = 3000L) {
            isClosed = true
        }

        analyzer.analyze(imageProxy)

        val results = receivedDetections
        assertNotNull(results)
        assertEquals(1, results!!.size)
        assertEquals(HazardType.POTHOLE, results[0].hazardType)
        assertTrue(isClosed)
    }

    @Test
    fun analyze_whenInferenceFails_callsOnErrorAndGuaranteesClose() {
        var errorThrown: Throwable? = null
        var isClosed = false

        val runner = TfliteRunner { _, _ ->
            throw IllegalStateException("Inference failed")
        }
        val detector = RoadHazardDetector.fromRunner(runner)

        val analyzer = RoadFrameAnalyzer(
            detector = detector,
            onError = { errorThrown = it }
        )

        val imageProxy = createFakeImageProxy(width = 640, height = 480, rotation = 0, timestamp = 4000L) {
            isClosed = true
        }

        analyzer.analyze(imageProxy)

        assertNotNull(errorThrown)
        assertTrue(errorThrown is IllegalStateException)
        assertTrue(isClosed)
    }

    private fun createFakeImageProxy(
        width: Int = 640,
        height: Int = 480,
        rotation: Int = 0,
        timestamp: Long = 12345L,
        onClose: () -> Unit = {}
    ): ImageProxy {
        val imageInfo = Proxy.newProxyInstance(
            ImageInfo::class.java.classLoader,
            arrayOf(ImageInfo::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getRotationDegrees" -> rotation
                "getTimestamp" -> timestamp
                else -> null
            }
        } as ImageInfo

        val buffer = ByteBuffer.allocateDirect(width * height * 4)
        val plane = Proxy.newProxyInstance(
            ImageProxy.PlaneProxy::class.java.classLoader,
            arrayOf(ImageProxy.PlaneProxy::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getBuffer" -> buffer
                "getRowStride" -> width * 4
                "getPixelStride" -> 4
                else -> null
            }
        } as ImageProxy.PlaneProxy

        return Proxy.newProxyInstance(
            ImageProxy::class.java.classLoader,
            arrayOf(ImageProxy::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getWidth" -> width
                "getHeight" -> height
                "getImageInfo" -> imageInfo
                "getPlanes" -> arrayOf(plane)
                "getCropRect" -> null
                "close" -> {
                    onClose()
                    null
                }
                else -> null
            }
        } as ImageProxy
    }
}
