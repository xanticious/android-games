package com.xanticious.androidgames.model

/**
 * Shared integer grid coordinate used by grid-based games (tower defense, tactics, roguelite).
 * Pure Kotlin — no Android or Compose imports.
 */
data class GridPos(val x: Int, val y: Int) {
    operator fun plus(other: GridPos): GridPos = GridPos(x + other.x, y + other.y)
    operator fun minus(other: GridPos): GridPos = GridPos(x - other.x, y - other.y)

    fun manhattanDistanceTo(other: GridPos): Int = kotlin.math.abs(x - other.x) + kotlin.math.abs(y - other.y)

    fun neighbours(): List<GridPos> = listOf(
        GridPos(x - 1, y), GridPos(x + 1, y),
        GridPos(x, y - 1), GridPos(x, y + 1)
    )

    companion object {
        val ZERO = GridPos(0, 0)
    }
}
