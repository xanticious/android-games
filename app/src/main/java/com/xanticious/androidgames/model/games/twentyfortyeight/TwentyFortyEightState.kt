package com.xanticious.androidgames.model.games.twentyfortyeight

/**
 * A point-in-time snapshot of the 2048 board used for single-level undo.
 */
data class TwentyFortyEightSnapshot(
    val tiles: List<Long>,
    val score: Long
)

/**
 * Immutable 2048 board state (`design/puzzle-games/2048`).
 *
 * [tiles] is a row-major flat list of size [size]×[size]; a value of 0 means the
 * cell is empty. [score] accumulates the sum of all merge values this game.
 * [bestTile] is the highest tile value that has appeared on the board.
 * [undoSnapshot] holds the previous board + score so the player can revert one move.
 */
data class TwentyFortyEightState(
    val size: Int,
    val tiles: List<Long>,
    val score: Long = 0L,
    val bestTile: Long = 0L,
    val moves: Int = 0,
    val undoSnapshot: TwentyFortyEightSnapshot? = null
) {
    val canUndo: Boolean get() = undoSnapshot != null

    fun tile(row: Int, col: Int): Long = tiles[row * size + col]
}

/**
 * Result returned by [com.xanticious.androidgames.controller.games.twentyfortyeight.TwentyFortyEightController.move].
 */
data class MoveResult(
    val state: TwentyFortyEightState,
    val gainedScore: Long,
    val moved: Boolean
)
