package com.xanticious.androidgames.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Vec2Test {
    @Test
    fun normalized_unitLength() {
        val n = Vec2(3f, 4f).normalized()
        assertEquals(1f, n.length, 1e-4f)
    }

    @Test
    fun normalized_zeroVector_returnsZero() {
        assertEquals(Vec2.ZERO, Vec2(0f, 0f).normalized())
    }

    @Test
    fun reflect_offHorizontalSurface_flipsY() {
        val reflected = reflect(Vec2(1f, -1f), Vec2(0f, 1f))
        assertEquals(1f, reflected.x, 1e-4f)
        assertEquals(1f, reflected.y, 1e-4f)
    }

    @Test
    fun distanceTo_computesEuclidean() {
        assertEquals(5f, Vec2(0f, 0f).distanceTo(Vec2(3f, 4f)), 1e-4f)
    }

    @Test
    fun lerp_halfway_isMidpoint() {
        val mid = Vec2(0f, 0f).lerp(Vec2(10f, 20f), 0.5f)
        assertTrue(mid.x == 5f && mid.y == 10f)
    }
}
