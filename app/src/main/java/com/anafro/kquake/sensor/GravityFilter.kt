package com.anafro.kquake.sensor

import kotlin.math.PI

/**
 * Separates gravity from shaking with a first-order high-pass filter.
 *
 * Gravity is tracked by an exponential low-pass whose coefficient is derived from the real time
 * between samples, so it stays correct when the sensor delivers at an uneven rate.
 *
 * [cutoffHz] is a trade-off: earthquake ground motion of interest is roughly 0.1–10 Hz, so a lower
 * cutoff keeps more of the signal, but the filter then takes longer to settle after the phone is
 * moved or rotated.
 */
class GravityFilter(cutoffHz: Double = DEFAULT_CUTOFF_HZ) {
    private val timeConstantSec = 1.0 / (2.0 * PI * cutoffHz)
    private var gravity: DoubleArray? = null
    private var lastTimestampNanos = 0L

    /** Returns [sample]'s acceleration with gravity removed, in m/s². */
    fun filter(sample: AccelerationSample): Vector3 {
        val a = sample.acceleration
        val g = gravity
        if (g == null) {
            gravity = doubleArrayOf(a.x.toDouble(), a.y.toDouble(), a.z.toDouble())
            lastTimestampNanos = sample.timestampNanos
            return Vector3.ZERO
        }

        val dtSec = (sample.timestampNanos - lastTimestampNanos).coerceAtLeast(0) / 1e9
        lastTimestampNanos = sample.timestampNanos
        val alpha = timeConstantSec / (timeConstantSec + dtSec)

        g[0] = alpha * g[0] + (1 - alpha) * a.x
        g[1] = alpha * g[1] + (1 - alpha) * a.y
        g[2] = alpha * g[2] + (1 - alpha) * a.z
        return Vector3((a.x - g[0]).toFloat(), (a.y - g[1]).toFloat(), (a.z - g[2]).toFloat())
    }

    companion object {
        const val DEFAULT_CUTOFF_HZ = 0.2
    }
}
