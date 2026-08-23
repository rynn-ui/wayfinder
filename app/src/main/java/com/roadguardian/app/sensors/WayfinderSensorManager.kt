package com.roadguardian.app.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WayfinderSensorManager(
    context: Context,
    val accelerometerProcessor: AccelerometerProcessor = AccelerometerProcessor(),
    val gyroscopeProcessor: GyroscopeProcessor = GyroscopeProcessor(),
    private val throttleIntervalMillis: Long = 50L
) : SensorEventListener {

    companion object {
        private const val TAG = "WayfinderSensors"
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscopeSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    val isAccelerometerAvailable: Boolean = accelerometerSensor != null
    val isGyroscopeAvailable: Boolean = gyroscopeSensor != null

    private val _telemetry = MutableStateFlow(
        SensorTelemetry(
            accelerometerAvailable = isAccelerometerAvailable,
            gyroscopeAvailable = isGyroscopeAvailable
        )
    )
    val telemetry: StateFlow<SensorTelemetry> = _telemetry.asStateFlow()

    @Volatile
    private var isListening = false

    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null

    private var lastAccelerometerResult = AccelerometerResult()
    private var lastGyroscopeResult = GyroscopeResult()
    private var lastEmitTime = 0L

    @Synchronized
    fun startListening() {
        if (isListening) return
        if (sensorManager == null) {
            Log.w(TAG, "SensorManager unavailable on this device")
            return
        }

        val thread = HandlerThread("WayfinderSensorThread").apply { start() }
        val handler = Handler(thread.looper)
        sensorThread = thread
        sensorHandler = handler

        accelerometerProcessor.reset()
        gyroscopeProcessor.reset()

        if (accelerometerSensor != null) {
            val registered = sensorManager.registerListener(
                this,
                accelerometerSensor,
                SensorManager.SENSOR_DELAY_GAME,
                handler
            )
            if (registered) {
                Log.i(TAG, "Accelerometer registered")
            } else {
                Log.w(TAG, "Failed to register accelerometer")
            }
        } else {
            Log.w(TAG, "Accelerometer unavailable")
        }

        if (gyroscopeSensor != null) {
            val registered = sensorManager.registerListener(
                this,
                gyroscopeSensor,
                SensorManager.SENSOR_DELAY_GAME,
                handler
            )
            if (registered) {
                Log.i(TAG, "Gyroscope registered")
            } else {
                Log.w(TAG, "Failed to register gyroscope")
            }
        } else {
            Log.w(TAG, "Gyroscope unavailable")
        }

        isListening = true
    }

    @Synchronized
    fun stopListening() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)

        sensorThread?.quitSafely()
        sensorThread = null
        sensorHandler = null

        accelerometerProcessor.reset()
        gyroscopeProcessor.reset()

        isListening = false
        lastAccelerometerResult = AccelerometerResult()
        lastGyroscopeResult = GyroscopeResult()

        _telemetry.value = SensorTelemetry(
            accelerometerAvailable = isAccelerometerAvailable,
            gyroscopeAvailable = isGyroscopeAvailable
        )
        Log.i(TAG, "Sensors unregistered")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isListening || event == null) return

        val now = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values.getOrNull(0) ?: 0f
                val y = event.values.getOrNull(1) ?: 0f
                val z = event.values.getOrNull(2) ?: 0f
                val result = accelerometerProcessor.process(x, y, z, now)
                lastAccelerometerResult = result
                if (result.impactDetected) {
                    Log.i(TAG, "Road impact detected: ${result.filteredAcceleration} m/s²")
                    emitTelemetry(now, force = true)
                    return
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                val x = event.values.getOrNull(0) ?: 0f
                val y = event.values.getOrNull(1) ?: 0f
                val z = event.values.getOrNull(2) ?: 0f
                val result = gyroscopeProcessor.process(x, y, z, now)
                lastGyroscopeResult = result
                if (result.rotationDetected) {
                    Log.i(TAG, "Rotation event detected: ${result.filteredRotation} rad/s")
                    emitTelemetry(now, force = true)
                    return
                }
            }
        }

        if (now - lastEmitTime >= throttleIntervalMillis) {
            emitTelemetry(now, force = false)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun emitTelemetry(timestamp: Long, force: Boolean) {
        lastEmitTime = timestamp
        _telemetry.value = SensorTelemetry(
            accelerometerX = lastAccelerometerResult.x,
            accelerometerY = lastAccelerometerResult.y,
            accelerometerZ = lastAccelerometerResult.z,
            accelerationMagnitude = lastAccelerometerResult.magnitude,
            linearAcceleration = lastAccelerometerResult.linearAcceleration,
            filteredAcceleration = lastAccelerometerResult.filteredAcceleration,
            impactDetected = lastAccelerometerResult.impactDetected,
            gyroscopeX = lastGyroscopeResult.x,
            gyroscopeY = lastGyroscopeResult.y,
            gyroscopeZ = lastGyroscopeResult.z,
            angularVelocity = lastGyroscopeResult.angularVelocity,
            filteredRotation = lastGyroscopeResult.filteredRotation,
            rotationDetected = lastGyroscopeResult.rotationDetected,
            accelerometerAvailable = isAccelerometerAvailable,
            gyroscopeAvailable = isGyroscopeAvailable,
            timestamp = timestamp
        )
    }
}
