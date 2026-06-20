package com.xanticious.androidgames.model

import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Immutable 2D vector used by action-game controllers for positions, velocities
 * and directions. Pure Kotlin — no Android or Compose imports — so it stays
 * unit-testable on the JVM.
 *
 * Coordinates follow the screen convention used by the game boards: x grows to
 * the right, y grows downward.
 */
data class Vec2(val x: Float, val y: Float) {

    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float): Vec2 = Vec2(x * scalar, y * scalar)
    operator fun div(scalar: Float): Vec2 = Vec2(x / scalar, y / scalar)
    operator fun unaryMinus(): Vec2 = Vec2(-x, -y)

    val length: Float get() = hypot(x, y)
    val lengthSquared: Float get() = x * x + y * y

    fun dot(other: Vec2): Float = x * other.x + y * other.y

    /** Returns a unit-length copy, or [ZERO] when this vector has no length. */
    fun normalized(): Vec2 {
        val len = length
        return if (len <= EPSILON) ZERO else Vec2(x / len, y / len)
    }

    /** Distance to [other]. */
    fun distanceTo(other: Vec2): Float = (this - other).length

    /** Clamps each component independently into the given inclusive ranges. */
    fun coerceIn(minX: Float, minY: Float, maxX: Float, maxY: Float): Vec2 =
        Vec2(x.coerceIn(minX, maxX), y.coerceIn(minY, maxY))

    /** Linear interpolation toward [target] by fraction [t] in `[0,1]`. */
    fun lerp(target: Vec2, t: Float): Vec2 = this + (target - this) * t.coerceIn(0f, 1f)

    companion object {
        val ZERO = Vec2(0f, 0f)
        private const val EPSILON = 1e-6f

        /** Unit vector for an angle in radians (0 = +x axis, increasing clockwise). */
        fun fromAngle(radians: Float): Vec2 = Vec2(kotlin.math.cos(radians), kotlin.math.sin(radians))
    }
}

/** Reflects an incoming velocity [v] off a surface with unit normal [normal]. */
fun reflect(v: Vec2, normal: Vec2): Vec2 {
    val n = normal.normalized()
    return v - n * (2f * v.dot(n))
}

/** Length of a 2D vector expressed as raw components. */
fun magnitude(x: Float, y: Float): Float = sqrt(x * x + y * y)
