package com.anafro.kquake.sensor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** An accelerometer sample split into gravity and ground motion. */
data class MotionReading(
    val timestampNanos: Long,
    /** Acceleration as reported by the sensor, gravity included, m/s². */
    val raw: Vector3,
    /** Acceleration with gravity removed, m/s². The input for PGA and MMI. */
    val dynamic: Vector3,
    /** Measured (not requested) delivery rate. Phones often deviate from the requested rate. */
    val sampleRateHz: Float,
)

private const val RATE_SMOOTHING = 0.05

fun Flow<AccelerationSample>.toMotionReadings(
    cutoffHz: Double = GravityFilter.DEFAULT_CUTOFF_HZ,
): Flow<MotionReading> = flow {
    val gravityFilter = GravityFilter(cutoffHz)
    var lastTimestampNanos = 0L
    var meanIntervalNanos = 0.0

    collect { sample ->
        if (lastTimestampNanos != 0L) {
            val interval = (sample.timestampNanos - lastTimestampNanos).toDouble()
            meanIntervalNanos = if (meanIntervalNanos == 0.0) interval
            else meanIntervalNanos + RATE_SMOOTHING * (interval - meanIntervalNanos)
        }
        lastTimestampNanos = sample.timestampNanos

        emit(
            MotionReading(
                timestampNanos = sample.timestampNanos,
                raw = sample.acceleration,
                dynamic = gravityFilter.filter(sample),
                sampleRateHz = if (meanIntervalNanos > 0) (1e9 / meanIntervalNanos).toFloat() else 0f,
            )
        )
    }
}
