package com.xanticious.androidgames.model.games.slidingpuzzle

/**
 * Immutable Sliding Puzzle board (`design/puzzle-games/sliding-puzzle`).
 *
 * The board is an N×N grid flattened row-major into [tiles]. Each entry is a tile
 * number `1..N*N-1`; the single `0` is the empty space. The goal is tiles in
 * ascending order with the blank in the bottom-right corner.
 */
data class SlidingPuzzleState(
    val size: Int,
    val tiles: List<Int>,
    val moves: Int = 0,
    val history: List<List<Int>> = emptyList()
) {
    val canUndo: Boolean get() = history.isNotEmpty()

    companion object {
        /** The solved arrangement for an [size]×[size] board: 1,2,…,N*N-1,0. */
        fun goalTiles(size: Int): List<Int> =
            (1 until size * size).toList() + 0

        fun solved(size: Int): SlidingPuzzleState =
            SlidingPuzzleState(size = size, tiles = goalTiles(size))
    }
}
