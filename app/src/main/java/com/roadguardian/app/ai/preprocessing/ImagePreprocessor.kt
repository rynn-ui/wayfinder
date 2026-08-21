package com.roadguardian.app.ai.preprocessing

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ImagePreprocessor(
    val targetWidth: Int = 640,
    val targetHeight: Int = 640
) {

    val requiredBufferSizeBytes: Int
        get() = 1 * targetWidth * targetHeight * 3 * Float.SIZE_BYTES

    fun createDirectByteBuffer(): ByteBuffer {
        return ByteBuffer.allocateDirect(requiredBufferSizeBytes).apply {
            order(ByteOrder.nativeOrder())
        }
    }

    fun preprocess(bitmap: Bitmap, outputBuffer: ByteBuffer? = null): ByteBuffer {
        val buffer = outputBuffer ?: createDirectByteBuffer()
        buffer.rewind()

        val resizedBitmap = if (bitmap.width == targetWidth && bitmap.height == targetHeight) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        }

        val pixels = IntArray(targetWidth * targetHeight)
        resizedBitmap.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            buffer.putFloat(r)
            buffer.putFloat(g)
            buffer.putFloat(b)
        }

        buffer.rewind()
        return buffer
    }

    fun preprocess(pixels: IntArray, width: Int, height: Int, outputBuffer: ByteBuffer? = null): ByteBuffer {
        require(pixels.size == width * height)
        val buffer = outputBuffer ?: createDirectByteBuffer()
        buffer.rewind()

        val scaleX = width.toFloat() / targetWidth
        val scaleY = height.toFloat() / targetHeight

        for (y in 0 until targetHeight) {
            val srcY = minOf(height - 1, (y * scaleY).toInt())
            for (x in 0 until targetWidth) {
                val srcX = minOf(width - 1, (x * scaleX).toInt())
                val pixel = pixels[srcY * width + srcX]

                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f

                buffer.putFloat(r)
                buffer.putFloat(g)
                buffer.putFloat(b)
            }
        }

        buffer.rewind()
        return buffer
    }

    fun preprocess(imageProxy: ImageProxy, outputBuffer: ByteBuffer? = null): ByteBuffer {
        val buffer = outputBuffer ?: createDirectByteBuffer()
        buffer.rewind()

        val plane = imageProxy.planes.firstOrNull()
        if (plane != null && plane.buffer.remaining() > 0) {
            preprocessRgbaBuffer(
                planeBuffer = plane.buffer,
                width = imageProxy.width,
                height = imageProxy.height,
                rowStride = plane.rowStride,
                pixelStride = plane.pixelStride,
                rotationDegrees = imageProxy.imageInfo.rotationDegrees,
                targetBuffer = buffer
            )
        } else {
            val bitmap = imageProxy.toBitmap()
            preprocess(bitmap, buffer)
        }

        buffer.rewind()
        return buffer
    }

    fun preprocessRgbaBuffer(
        planeBuffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
        rotationDegrees: Int,
        targetBuffer: ByteBuffer
    ) {
        val maxReadableOffset = planeBuffer.limit() - 3
        val bufferStart = planeBuffer.position()

        for (ty in 0 until targetHeight) {
            for (tx in 0 until targetWidth) {
                val srcX: Int
                val srcY: Int

                when (rotationDegrees) {
                    90 -> {
                        srcX = minOf(width - 1, (ty * width) / targetHeight)
                        srcY = maxOf(0, minOf(height - 1, ((targetWidth - 1 - tx) * height) / targetWidth))
                    }
                    180 -> {
                        srcX = maxOf(0, minOf(width - 1, ((targetWidth - 1 - tx) * width) / targetWidth))
                        srcY = maxOf(0, minOf(height - 1, ((targetHeight - 1 - ty) * height) / targetHeight))
                    }
                    270 -> {
                        srcX = maxOf(0, minOf(width - 1, ((targetHeight - 1 - ty) * width) / targetHeight))
                        srcY = minOf(height - 1, (tx * height) / targetWidth)
                    }
                    else -> {
                        srcX = minOf(width - 1, (tx * width) / targetWidth)
                        srcY = minOf(height - 1, (ty * height) / targetHeight)
                    }
                }

                val offset = bufferStart + srcY * rowStride + srcX * pixelStride
                if (offset in 0..maxReadableOffset) {
                    val r = (planeBuffer.get(offset).toInt() and 0xFF) / 255.0f
                    val g = (planeBuffer.get(offset + 1).toInt() and 0xFF) / 255.0f
                    val b = (planeBuffer.get(offset + 2).toInt() and 0xFF) / 255.0f

                    targetBuffer.putFloat(r)
                    targetBuffer.putFloat(g)
                    targetBuffer.putFloat(b)
                } else {
                    targetBuffer.putFloat(0.0f)
                    targetBuffer.putFloat(0.0f)
                    targetBuffer.putFloat(0.0f)
                }
            }
        }
    }
}
