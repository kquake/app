package com.anafro.kquake.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

/** One raw accelerometer reading. */
data class AccelerationSample(
    /**
     * Nanoseconds in the [android.os.SystemClock.elapsedRealtimeNanos] time base,
     * the same as [android.location.Location.getElapsedRealtimeNanos].
     */
    val timestampNanos: Long,
    /** Acceleration in m/s², gravity included. */
    val acceleration: Vector3,
)

/**
 * 100 Hz is the usual rate for strong-motion seismology, and stays below the
 * 200 Hz cap above which Android requires the HIGH_SAMPLING_RATE_SENSORS permission.
 */
const val SEISMIC_SAMPLING_PERIOD_US = 10_000

/** Room for ~10 s of samples at 100 Hz if the collector falls behind. */
private const val SAMPLE_BUFFER_CAPACITY = 1024

fun SensorManager.hasAccelerometer(): Boolean = getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

/**
 * Streams raw [Sensor.TYPE_ACCELEROMETER] readings. Raw rather than TYPE_LINEAR_ACCELERATION on purpose:
 * linear acceleration goes through vendor sensor fusion with unknown filtering, while seismic
 * processing needs to control the filter itself (see [GravityFilter]).
 *
 * Events are delivered on a dedicated thread so UI work never delays sampling.
 */
fun SensorManager.accelerationSamples(
    samplingPeriodUs: Int = SEISMIC_SAMPLING_PERIOD_US,
): Flow<AccelerationSample> = callbackFlow {
    val sensor = getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        ?: throw IllegalStateException("Device has no accelerometer")

    val thread = HandlerThread("KQuake-accelerometer").apply { start() }
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val (x, y, z) = event.values
            trySend(AccelerationSample(event.timestamp, Vector3(x, y, z)))
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
    }
    registerListener(listener, sensor, samplingPeriodUs, Handler(thread.looper))

    awaitClose {
        unregisterListener(listener)
        thread.quitSafely()
    }
}.buffer(SAMPLE_BUFFER_CAPACITY)
