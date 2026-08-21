package com.roadguardian.app.ai.inference

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.roadguardian.app.ai.postprocessing.YoloPostProcessor
import com.roadguardian.app.ai.preprocessing.ImagePreprocessor
import com.roadguardian.app.domain.model.RoadHazardDetection
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

fun interface TfliteRunner : AutoCloseable {
    fun run(input: ByteBuffer, output: Array<Array<FloatArray>>)
    override fun close() {}
}

class InterpreterTfliteRunner(
    private val interpreter: Interpreter
) : TfliteRunner {
    val inputTensorShape: IntArray
        get() = interpreter.getInputTensor(0).shape()

    val inputTensorDataType: DataType
        get() = interpreter.getInputTensor(0).dataType()

    val outputTensorShape: IntArray
        get() = interpreter.getOutputTensor(0).shape()

    val outputTensorDataType: DataType
        get() = interpreter.getOutputTensor(0).dataType()

    override fun run(input: ByteBuffer, output: Array<Array<FloatArray>>) {
        interpreter.run(input, output)
    }

    override fun close() {
        interpreter.close()
    }
}

class RoadHazardDetector(
    private val runner: TfliteRunner,
    val preprocessor: ImagePreprocessor = ImagePreprocessor(),
    val postProcessor: YoloPostProcessor = YoloPostProcessor()
) : AutoCloseable {

    companion object {
        const val DEFAULT_MODEL_ASSET = "yolo12n_seed0_best_dynamic_range_quant.tflite"

        fun fromAsset(
            context: Context,
            assetPath: String = DEFAULT_MODEL_ASSET,
            options: Interpreter.Options = createDefaultOptions()
        ): RoadHazardDetector {
            val byteBuffer = loadModelFileFromAsset(context, assetPath)
            val interpreter = Interpreter(byteBuffer, options)
            return RoadHazardDetector(InterpreterTfliteRunner(interpreter))
        }

        fun fromInterpreter(
            interpreter: Interpreter,
            preprocessor: ImagePreprocessor = ImagePreprocessor(),
            postProcessor: YoloPostProcessor = YoloPostProcessor()
        ): RoadHazardDetector {
            return RoadHazardDetector(InterpreterTfliteRunner(interpreter), preprocessor, postProcessor)
        }

        fun fromRunner(
            runner: TfliteRunner,
            preprocessor: ImagePreprocessor = ImagePreprocessor(),
            postProcessor: YoloPostProcessor = YoloPostProcessor()
        ): RoadHazardDetector {
            return RoadHazardDetector(runner, preprocessor, postProcessor)
        }

        fun createDefaultOptions(): Interpreter.Options {
            return Interpreter.Options().apply {
                setNumThreads(4)
            }
        }

        fun loadModelFileFromAsset(context: Context, assetPath: String): ByteBuffer {
            val fileDescriptor = context.assets.openFd(assetPath)
            FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                val fileChannel = inputStream.channel
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            }
        }
    }

    @Volatile
    private var isClosed = false

    private val inputBuffer: ByteBuffer = preprocessor.createDirectByteBuffer()

    private val outputBuffer: Array<Array<FloatArray>> = Array(1) {
        Array(postProcessor.classCount + 4) {
            FloatArray(postProcessor.totalPredictions)
        }
    }

    @Synchronized
    fun detect(bitmap: Bitmap, timestamp: Long = System.currentTimeMillis()): List<RoadHazardDetection> {
        if (isClosed) return emptyList()
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        preprocessor.preprocess(bitmap, inputBuffer)
        runner.run(inputBuffer, outputBuffer)
        return postProcessor.process(outputBuffer, originalWidth, originalHeight, timestamp)
    }

    @Synchronized
    fun detect(pixels: IntArray, width: Int, height: Int, timestamp: Long = System.currentTimeMillis()): List<RoadHazardDetection> {
        if (isClosed) return emptyList()
        preprocessor.preprocess(pixels, width, height, inputBuffer)
        runner.run(inputBuffer, outputBuffer)
        return postProcessor.process(outputBuffer, width, height, timestamp)
    }

    @Synchronized
    fun detect(
        imageProxy: ImageProxy,
        timestamp: Long = System.currentTimeMillis()
    ): List<RoadHazardDetection> {
        if (isClosed) return emptyList()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val uprightWidth = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.height else imageProxy.width
        val uprightHeight = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.width else imageProxy.height

        preprocessor.preprocess(imageProxy, inputBuffer)
        runner.run(inputBuffer, outputBuffer)
        return postProcessor.process(outputBuffer, uprightWidth, uprightHeight, timestamp)
    }

    @Synchronized
    fun detect(
        preprocessedBuffer: ByteBuffer,
        originalWidth: Int,
        originalHeight: Int,
        timestamp: Long = System.currentTimeMillis()
    ): List<RoadHazardDetection> {
        if (isClosed) return emptyList()
        preprocessedBuffer.rewind()
        runner.run(preprocessedBuffer, outputBuffer)
        return postProcessor.process(outputBuffer, originalWidth, originalHeight, timestamp)
    }

    @Synchronized
    override fun close() {
        if (!isClosed) {
            isClosed = true
            runner.close()
        }
    }
}
