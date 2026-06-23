package com.xanticious.androidgames.model.games.puzzle

/**
 * Shared model types for grid-based puzzle games.
 *
 * These types carry no Android or Compose imports so the whole puzzle rule set
 * stays JVM unit-testable. See `design/common/puzzle-grid-board.md` and
 * `design/common/puzzle-flow.md` for the conventions they encode.
 */

/** A cell coordinate on a puzzle grid; `row` and `col` are zero-based. */
data class GridPos(val row: Int, val col: Int)

/** The four orthogonal directions used by most grid puzzles. */
enum class Direction(val dRow: Int, val dCol: Int) {
    UP(-1, 0),
    DOWN(1, 0),
    LEFT(0, -1),
    RIGHT(0, 1);

    val opposite: Direction
        get() = when (this) {
            UP -> DOWN
            DOWN -> UP
            LEFT -> RIGHT
            RIGHT -> LEFT
        }
}

/** Translates a position one step in [direction]. */
fun GridPos.step(direction: Direction): GridPos =
    GridPos(row + direction.dRow, col + direction.dCol)

/**
 * High-level lifecycle phase shared by every self-configured puzzle game.
 *
 * The flow mirrors `design/common/puzzle-flow.md`: the game opens on its own
 * [SETTINGS] screen, optionally shows [HOW_TO_PLAY], then plays. [FAILED] is
 * only used by games that can be lost (e.g. Minesweeper).
 */
enum class PuzzlePhase {
    SETTINGS,
    HOW_TO_PLAY,
    PLAYING,
    SOLVED,
    FAILED
}

/**
 * Computes a 1–3 star rating from up to three escalating quality conditions.
 * Returns 0 when the base [earned] condition is not met (e.g. on defeat).
 */
fun starsFor(earned: Boolean, good: Boolean, great: Boolean): Int {
    if (!earned) return 0
    var stars = 1
    if (good) stars++
    if (great) stars++
    return stars
}
