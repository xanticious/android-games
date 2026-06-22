package com.xanticious.androidgames.controller.games.slidingpuzzle

import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.slidingpuzzle.SlidingPuzzleState
import kotlin.random.Random

/**
 * Pure Sliding Puzzle rules: scramble generation, slide application, undo and the
 * solved check. No Android or Compose imports, so the whole rule set is JVM
 * unit-testable.
 *
 * Scrambles are produced by applying random *legal* single-tile slides starting
 * from the solved board, which guarantees every dealt puzzle is solvable
 * (`design/puzzle-games/sliding-puzzle` — "Only solvable scrambles are dealt").
 */
class SlidingPuzzleController {

    /** Board sizes offered in settings, smallest (easiest) first. */
    val sizes: List<Int> = listOf(3, 4, 5, 6)

    fun isSolved(state: SlidingPuzzleState): Boolean =
        state.tiles == SlidingPuzzleState.goalTiles(state.size)

    /** Deals a fresh, guaranteed-solvable scramble of the given [size]. */
    fun newGame(size: Int, random: Random = Random.Default): SlidingPuzzleState {
        var tiles = SlidingPuzzleState.goalTiles(size)
        val shuffles = size * size * 40
        var lastBlank = -1
        repeat(shuffles) {
            val blank = tiles.indexOf(0)
            val neighbours = neighbourIndices(blank, size).filter { it != lastBlank }
            val pick = neighbours[random.nextInt(neighbours.size)]
            tiles = swap(tiles, blank, pick)
            lastBlank = blank
        }
        // Extremely unlikely, but never deal an already-solved board.
        if (tiles == SlidingPuzzleState.goalTiles(size)) {
            return newGame(size, random)
        }
        return SlidingPuzzleState(size = size, tiles = tiles)
    }

    /**
     * Slides the run of tiles between [pos] and the blank toward the blank when
     * [pos] shares the blank's row or column. Returns the new state, or the
     * unchanged state when the tap is not a legal slide.
     */
    fun slide(state: SlidingPuzzleState, pos: GridPos): SlidingPuzzleState {
        val n = state.size
        if (pos.row !in 0 until n || pos.col !in 0 until n) return state
        val blank = state.tiles.indexOf(0)
        val br = blank / n
        val bc = blank % n
        val tiles = state.tiles.toMutableList()

        when {
            pos.row == br && pos.col != bc -> {
                if (pos.col < bc) {
                    for (c in bc downTo pos.col + 1) tiles[br * n + c] = tiles[br * n + c - 1]
                } else {
                    for (c in bc until pos.col) tiles[br * n + c] = tiles[br * n + c + 1]
                }
                tiles[pos.row * n + pos.col] = 0
            }
            pos.col == bc && pos.row != br -> {
                if (pos.row < br) {
                    for (r in br downTo pos.row + 1) tiles[r * n + bc] = tiles[(r - 1) * n + bc]
                } else {
                    for (r in br until pos.row) tiles[r * n + bc] = tiles[(r + 1) * n + bc]
                }
                tiles[pos.row * n + pos.col] = 0
            }
            else -> return state
        }
        return state.copy(tiles = tiles, moves = state.moves + 1, history = state.history + listOf(state.tiles))
    }

    /** Reverts the most recent slide, if any. */
    fun undo(state: SlidingPuzzleState): SlidingPuzzleState {
        val previous = state.history.lastOrNull() ?: return state
        return state.copy(
            tiles = previous,
            moves = state.moves + 1,
            history = state.history.dropLast(1)
        )
    }

    /** Count of tiles already in their goal position (for progress feedback). */
    fun correctlyPlaced(state: SlidingPuzzleState): Int {
        val goal = SlidingPuzzleState.goalTiles(state.size)
        return state.tiles.indices.count { state.tiles[it] != 0 && state.tiles[it] == goal[it] }
    }

    private fun neighbourIndices(index: Int, size: Int): List<Int> {
        val r = index / size
        val c = index % size
        val result = mutableListOf<Int>()
        if (r > 0) result += (r - 1) * size + c
        if (r < size - 1) result += (r + 1) * size + c
        if (c > 0) result += r * size + (c - 1)
        if (c < size - 1) result += r * size + (c + 1)
        return result
    }

    private fun swap(list: List<Int>, a: Int, b: Int): List<Int> {
        val copy = list.toMutableList()
        val tmp = copy[a]
        copy[a] = copy[b]
        copy[b] = tmp
        return copy
    }
}
