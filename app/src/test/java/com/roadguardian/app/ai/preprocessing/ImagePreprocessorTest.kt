package com.roadguardian.app.ai.preprocessing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ImagePreprocessorTest {

    @Test
    fun requiredBufferSizeBytes_returnsCorrectSize() {
        val preprocessor = ImagePreprocessor(640, 640)
        assertEquals(1 * 640 * 640 * 3 * 4, preprocessor.requiredBufferSizeBytes)
    }

    @Test
    fun createDirectByteBuffer_hasCorrectCapacityAndByteOrder() {
        val preprocessor = ImagePreprocessor(640, 640)
        val buffer = preprocessor.createDirectByteBuffer()
        assertTrue(buffer.isDirect)
        assertEquals(4915200, buffer.capacity())
        assertEquals(ByteOrder.nativeOrder(), buffer.order())
    }

    @Test
    fun preprocess_normalizesPixelsToZeroToOneRange() {
        val preprocessor = ImagePreprocessor(2, 2)
        val pixels = intArrayOf(
            0x000000,
            0xFF0000,
            0x00FF00,
            0x0000FF
        )

        val buffer = preprocessor.preprocess(pixels, 2, 2)
        assertEquals(0.0f, buffer.getFloat(0), 0.001f)
        assertEquals(0.0f, buffer.getFloat(4), 0.001f)
        assertEquals(0.0f, buffer.getFloat(8), 0.001f)

        assertEquals(1.0f, buffer.getFloat(12), 0.001f)
        assertEquals(0.0f, buffer.getFloat(16), 0.001f)
        assertEquals(0.0f, buffer.getFloat(20), 0.001f)
    }

    @Test
    fun preprocess_scalesDimensionsToTargetResolution() {
        val preprocessor = ImagePreprocessor(640, 640)
        val pixels = IntArray(100 * 100) { 0xFFFFFF }

        val buffer = preprocessor.preprocess(pixels, 100, 100)
        assertEquals(0, buffer.position())
        assertEquals(4915200, buffer.remaining())

        assertEquals(1.0f, buffer.getFloat(), 0.001f)
        assertEquals(1.0f, buffer.getFloat(), 0.001f)
        assertEquals(1.0f, buffer.getFloat(), 0.001f)
    }

    @Test
    fun preprocessRgbaBuffer_handlesZeroRotation() {
        val preprocessor = ImagePreprocessor(2, 2)
        val rgbaBuffer = ByteBuffer.allocateDirect(2 * 2 * 4).apply {
            put(byteArrayOf(-1, 0, 0, -1))
            put(byteArrayOf(0, -1, 0, -1))
            put(byteArrayOf(0, 0, -1, -1))
            put(byteArrayOf(-1, -1, -1, -1))
            rewind()
        }

        val targetBuffer = preprocessor.createDirectByteBuffer()
        preprocessor.preprocessRgbaBuffer(
            planeBuffer = rgbaBuffer,
            width = 2,
            height = 2,
            rowStride = 8,
            pixelStride = 4,
            rotationDegrees = 0,
            targetBuffer = targetBuffer
        )

        assertEquals(1.0f, targetBuffer.getFloat(0), 0.001f)
        assertEquals(0.0f, targetBuffer.getFloat(4), 0.001f)
        assertEquals(0.0f, targetBuffer.getFloat(8), 0.001f)
    }

    @Test
    fun preprocessRgbaBuffer_handles90DegreesRotation() {
        val preprocessor = ImagePreprocessor(2, 2)
        val rgbaBuffer = ByteBuffer.allocateDirect(2 * 2 * 4).apply {
            put(byteArrayOf(-1, 0, 0, -1))
            put(byteArrayOf(0, -1, 0, -1))
            put(byteArrayOf(0, 0, -1, -1))
            put(byteArrayOf(-1, -1, -1, -1))
            rewind()
        }

        val targetBuffer = preprocessor.createDirectByteBuffer()
        preprocessor.preprocessRgbaBuffer(
            planeBuffer = rgbaBuffer,
            width = 2,
            height = 2,
            rowStride = 8,
            pixelStride = 4,
            rotationDegrees = 90,
            targetBuffer = targetBuffer
        )

        assertEquals(0.0f, targetBuffer.getFloat(0), 0.001f)
        assertEquals(0.0f, targetBuffer.getFloat(4), 0.001f)
        assertEquals(1.0f, targetBuffer.getFloat(8), 0.001f)
    }

    @Test
    fun preprocessRgbaBuffer_handles180DegreesRotation() {
        val preprocessor = ImagePreprocessor(2, 2)
        val rgbaBuffer = ByteBuffer.allocateDirect(2 * 2 * 4).apply {
            put(byteArrayOf(-1, 0, 0, -1))
            put(byteArrayOf(0, -1, 0, -1))
            put(byteArrayOf(0, 0, -1, -1))
            put(byteArrayOf(-1, -1, -1, -1))
            rewind()
        }

        val targetBuffer = preprocessor.createDirectByteBuffer()
        preprocessor.preprocessRgbaBuffer(
            planeBuffer = rgbaBuffer,
            width = 2,
            height = 2,
            rowStride = 8,
            pixelStride = 4,
            rotationDegrees = 180,
            targetBuffer = targetBuffer
        )

        assertEquals(1.0f, targetBuffer.getFloat(0), 0.001f)
        assertEquals(1.0f, targetBuffer.getFloat(4), 0.001f)
        assertEquals(1.0f, targetBuffer.getFloat(8), 0.001f)
    }

    @Test
    fun preprocessRgbaBuffer_handles270DegreesRotation() {
        val preprocessor = ImagePreprocessor(2, 2)
        val rgbaBuffer = ByteBuffer.allocateDirect(2 * 2 * 4).apply {
            put(byteArrayOf(-1, 0, 0, -1))
            put(byteArrayOf(0, -1, 0, -1))
            put(byteArrayOf(0, 0, -1, -1))
            put(byteArrayOf(-1, -1, -1, -1))
            rewind()
        }

        val targetBuffer = preprocessor.createDirectByteBuffer()
        preprocessor.preprocessRgbaBuffer(
            planeBuffer = rgbaBuffer,
            width = 2,
            height = 2,
            rowStride = 8,
            pixelStride = 4,
            rotationDegrees = 270,
            targetBuffer = targetBuffer
        )

        assertEquals(0.0f, targetBuffer.getFloat(0), 0.001f)
        assertEquals(1.0f, targetBuffer.getFloat(4), 0.001f)
        assertEquals(0.0f, targetBuffer.getFloat(8), 0.001f)
    }
}
