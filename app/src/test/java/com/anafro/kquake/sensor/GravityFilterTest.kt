package com.anafro.kquake.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class GravityFilterTest {
    private val periodNanos = 10_000_000L // 100 Hz

    private fun sample(i: Int, x: Float = 0f, y: Float = 0f, z: Float = 9.81f) =
        AccelerationSample(i * periodNanos, Vector3(x, y, z))

    @Test
    fun stationaryPhone_hasNoDynamicAcceleration() {
        val filter = GravityFilter()
        repeat(500) { i ->
            val dynamic = filter.filter(sample(i, x = 0.3f, y = 1.2f, z = 9.7f))
            assertEquals(0f, dynamic.magnitude, 1e-4f)
        }
    }

    @Test
    fun shakingWellAboveCutoff_passesThrough() {
        val filter = GravityFilter(cutoffHz = 0.2)
        val amplitude = 0.5f
        var peak = 0f
        // 5 Hz shaking on the x axis for 10 s; measure the peak after the filter has settled.
        repeat(1000) { i ->
            val t = i * periodNanos / 1e9
            val x = amplitude * sin(2 * PI * 5.0 * t).toFloat()
            val dynamic = filter.filter(sample(i, x = x))
            if (i > 500) peak = maxOf(peak, abs(dynamic.x))
        }
        assertEquals(amplitude, peak, amplitude * 0.05f)
    }

    @Test
    fun orientationChange_settlesBackToZero() {
        val filter = GravityFilter(cutoffHz = 0.2)
        repeat(100) { i -> filter.filter(sample(i, z = 9.81f)) }
        // Phone tipped onto its side: gravity moves from z to x.
        var dynamic = Vector3.ZERO
        for (i in 100 until 1100) dynamic = filter.filter(sample(i, x = 9.81f, z = 0f))
        assertTrue("residual ${dynamic.magnitude}", dynamic.magnitude < 0.01f)
    }
}
