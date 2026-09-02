package com.roadguardian.app.ai.inference

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import com.roadguardian.app.ai.postprocessing.RawDetectionStats
import com.roadguardian.app.ai.postprocessing.YoloPostProcessor
import com.roadguardian.app.ai.preprocessing.ImagePreprocessor
import com.roadguardian.app.domain.model.RoadHazardDetection
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

private const val TAG = "RoadHazardDetector"

data class TimedDetectionResult(
    val detections: List<RoadHazardDetection>,
    val latencyMs: Float,
    val rawStats: RawDetectionStats? = null
)

data class ModelTensorSignature(
    val name: String,
    val shape: String,
    val dataType: String,
    val quantScale: Float,
    val quantZeroPoint: Int
) {
    val summary: String
        get() = "$shape $dataType (scale=$quantScale, zp=$quantZeroPoint)"
}

data class DetectorModelSignature(
    val modelLabel: String,
    val input: ModelTensorSignature,
    val output: ModelTensorSignature
) {
    val summary: String
        get() = "MODEL: $modelLabel | INPUT: ${input.summary} | OUTPUT: ${output.summary}"
}

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

    val inputSignature: ModelTensorSignature
        get() = toSignature("input", interpreter.getInputTensor(0))

    val outputSignature: ModelTensorSignature
        get() = toSignature("output", interpreter.getOutputTensor(0))

    private fun toSignature(prefix: String, tensor: Tensor): ModelTensorSignature {
        val name = runCatching { tensor.name() }.getOrDefault("$prefix-tensor")
        val quantScale = runCatching { tensor.quantizationParams().scale }.getOrDefault(0f)
        val quantZeroPoint = runCatching { tensor.quantizationParams().zeroPoint }.getOrDefault(0)
        return ModelTensorSignature(
            name = name,
            shape = tensor.shape().joinToString(prefix = "[", postfix = "]", separator = ", "),
            dataType = tensor.dataType().name,
            quantScale = quantScale,
            quantZeroPoint = quantZeroPoint
        )
    }

    override fun run(input: ByteBuffer, output: Array<Array<FloatArray>>) {
        interpreter.run(input, output)
    }

    override fun close() {
        interpreter.close()
    }
}

class RoadHazardDetector(
    private val runner: TfliteRunner,
    val modelType: AiModelType = AiModelType.DEFAULT,
    val preprocessor: ImagePreprocessor = ImagePreprocessor(),
    val postProcessor: YoloPostProcessor = if (modelType.isSingleClass) {
        YoloPostProcessor(
            confidenceThreshold = 0.25f,
            iouThreshold = 0.45f,
            classCount = 1,
            potholeClassId = 0,
            singleClassMode = true,
            classLabels = listOf("pothole")
        )
    } else {
        YoloPostProcessor(
            confidenceThreshold = 0.35f,
            iouThreshold = 0.45f,
            classCount = 4,
            potholeClassId = 3,
            singleClassMode = false
        )
    },
    val modelSignature: DetectorModelSignature? = null
) : AutoCloseable {

    companion object {
        const val DEFAULT_MODEL_ASSET = "pothole_yolo11n_dynamic_int8.tflite"

        fun fromAsset(
            context: Context,
            assetPath: String = DEFAULT_MODEL_ASSET,
            options: Interpreter.Options = createDefaultOptions()
        ): RoadHazardDetector {
            return fromModelType(context, AiModelType.DEFAULT, assetPath, options)
        }

        fun fromModelType(
            context: Context,
            modelType: AiModelType = AiModelType.DEFAULT,
            assetPath: String = modelType.assetPath,
            options: Interpreter.Options = createDefaultOptions()
        ): RoadHazardDetector {
            val byteBuffer = loadModelFileFromAsset(context, assetPath)
            val interpreter = Interpreter(byteBuffer, options)
            val runner = InterpreterTfliteRunner(interpreter)
            val signature = DetectorModelSignature(
                modelLabel = modelType.displayName,
                input = runner.inputSignature,
                output = runner.outputSignature
            )
            val postProcessor = if (modelType.isSingleClass) {
                YoloPostProcessor(
                    confidenceThreshold = 0.25f,
                    iouThreshold = 0.45f,
                    classCount = 1,
                    potholeClassId = 0,
                    singleClassMode = true,
                    classLabels = listOf("pothole")
                )
            } else {
                YoloPostProcessor(
                    confidenceThreshold = 0.35f,
                    iouThreshold = 0.45f,
                    classCount = 4,
                    potholeClassId = 3,
                    singleClassMode = false
                )
            }
            Log.i(TAG, "TFLite model loaded ($assetPath): ${signature.summary}")
            return RoadHazardDetector(
                runner = runner,
                modelType = modelType,
                preprocessor = ImagePreprocessor(),
                postProcessor = postProcessor,
                modelSignature = signature
            )
        }

        fun fromInterpreter(
            interpreter: Interpreter,
            modelType: AiModelType = AiModelType.DEFAULT,
            preprocessor: ImagePreprocessor = ImagePreprocessor(),
            postProcessor: YoloPostProcessor = if (modelType.isSingleClass) {
                YoloPostProcessor(
                    confidenceThreshold = 0.35f,
                    iouThreshold = 0.45f,
                    classCount = 1,
                    potholeClassId = 0,
                    singleClassMode = true,
                    classLabels = listOf("pothole")
                )
            } else {
                YoloPostProcessor(
                    confidenceThreshold = 0.35f,
                    iouThreshold = 0.45f,
                    classCount = 4,
                    potholeClassId = 3,
                    singleClassMode = false
                )
            }
        ): RoadHazardDetector {
            val runner = InterpreterTfliteRunner(interpreter)
            val signature = DetectorModelSignature(
                modelLabel = modelType.displayName,
                input = runner.inputSignature,
                output = runner.outputSignature
            )
            return RoadHazardDetector(runner, modelType, preprocessor, postProcessor, signature)
        }

        fun fromRunner(
            runner: TfliteRunner,
            modelType: AiModelType = AiModelType.DEFAULT,
            preprocessor: ImagePreprocessor = ImagePreprocessor(),
            postProcessor: YoloPostProcessor = if (modelType.isSingleClass) {
                YoloPostProcessor(
                    confidenceThreshold = 0.35f,
                    iouThreshold = 0.45f,
                    classCount = 1,
                    potholeClassId = 0,
                    singleClassMode = true,
                    classLabels = listOf("pothole")
                )
            } else {
                YoloPostProcessor(
                    confidenceThreshold = 0.35f,
                    iouThreshold = 0.45f,
                    classCount = 4,
                    potholeClassId = 3,
                    singleClassMode = false
                )
            }
        ): RoadHazardDetector {
            return RoadHazardDetector(runner, modelType, preprocessor, postProcessor)
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
        Array(if (postProcessor.singleClassMode) 5 else (postProcessor.classCount + 4)) {
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
        return detectWithTiming(imageProxy, timestamp).detections
    }

    @Synchronized
    fun detectWithTiming(
        imageProxy: ImageProxy,
        timestamp: Long = System.currentTimeMillis()
    ): TimedDetectionResult {
        if (isClosed) return TimedDetectionResult(emptyList(), 0f)
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val uprightWidth = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.height else imageProxy.width
        val uprightHeight = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.width else imageProxy.height

        preprocessor.preprocess(imageProxy, inputBuffer)

        val startTime = System.nanoTime()
        runner.run(inputBuffer, outputBuffer)
        val endTime = System.nanoTime()
        val latencyMs = (endTime - startTime) / 1_000_000.0f

        val rawStats = postProcessor.computeRawStats(outputBuffer)
        logRawStats(rawStats, latencyMs)
        val detections = postProcessor.process(outputBuffer, uprightWidth, uprightHeight, timestamp)
        return TimedDetectionResult(detections, latencyMs, rawStats)
    }

    private fun logRawStats(rawStats: RawDetectionStats?, latencyMs: Float) {
        if (rawStats == null) {
            Log.d(TAG, "rawStats unavailable")
            return
        }
        Log.d(
            TAG,
            "RAW tensor[min=${rawStats.tensorMin} max=${rawStats.tensorMax} mean=${rawStats.tensorMean} | " +
                "topScore=${rawStats.rawMaxScore} class=${rawStats.rawMaxClassLabel}(${rawStats.rawMaxClass}) " +
                "box640=${rawStats.rawMaxBox} | potholeMax=${rawStats.potholeMaxScore} " +
                "cand@${postProcessor.confidenceThreshold}=${rawStats.candidatesAtThreshold} " +
                "anchorClassScores=${rawStats.rawMaxTopAnchorClassScores.joinToString { String.format("%.4f", it) }} " +
                "latencyMs=$latencyMs"
        )
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
