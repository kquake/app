package com.anafro.kquake.sensor

import kotlin.math.sqrt

/** A 3-axis vector in device coordinates (x: right, y: up along the screen, z: out of the screen). */
data class Vector3(val x: Float, val y: Float, val z: Float) {
    val magnitude: Float get() = sqrt(x * x + y * y + z * z)

    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
    }
}
