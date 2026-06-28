package com.xanticious.androidgames.controller.games.twentyfortyeight

import com.xanticious.androidgames.model.games.puzzle.Direction
import com.xanticious.androidgames.model.games.twentyfortyeight.MoveResult
import com.xanticious.androidgames.model.games.twentyfortyeight.TwentyFortyEightSnapshot
import com.xanticious.androidgames.model.games.twentyfortyeight.TwentyFortyEightState
import kotlin.random.Random

/**
 * Pure 2048 rules: slide-and-merge, tile spawning, win/lose checks.
 *
 * No Android or Compose imports, so the entire rule set is JVM unit-testable.
 * Pass a seeded [Random] to any function that needs randomness to get deterministic
 * behaviour in tests.
 */
class TwentyFortyEightController {

    /** Board sizes offered in settings. */
    val sizes: List<Int> = listOf(4, 5, 6)

    /**
     * Returns a fresh board of the given [size] with two randomly spawned tiles.
     */
    fun newGame(size: Int, random: Random = Random.Default): TwentyFortyEightState {
        val empty = TwentyFortyEightState(
            size = size,
            tiles = List(size * size) { 0L }
        )
        val withFirst = spawnTile(empty, random)
        val withSecond = spawnTile(withFirst, random)
        return withSecond
    }

    /**
     * Slides all tiles in [direction], merges equal neighbours once from the
     * leading edge, and returns a [MoveResult] containing the updated state, the
     * score gained from merges, and a flag indicating whether the board changed.
     *
     * The snapshot for undo is recorded only when the board actually moves.
     */
    fun move(state: TwentyFortyEightState, direction: Direction): MoveResult {
        val n = state.size
        val newTiles = state.tiles.toMutableList()
        var gainedScore = 0L
        var moved = false

        when (direction) {
            Direction.LEFT -> {
                for (r in 0 until n) {
                    val line = extractRow(newTiles, r, n)
                    val (merged, score, changed) = mergeLine(line)
                    if (changed) { moved = true; writeRow(newTiles, r, n, merged) }
                    gainedScore += score
                }
            }
            Direction.RIGHT -> {
                for (r in 0 until n) {
                    val line = extractRow(newTiles, r, n).reversed()
                    val (merged, score, changed) = mergeLine(line)
                    if (changed) { moved = true; writeRow(newTiles, r, n, merged.reversed()) }
                    gainedScore += score
                }
            }
            Direction.UP -> {
                for (c in 0 until n) {
                    val line = extractCol(newTiles, c, n)
                    val (merged, score, changed) = mergeLine(line)
                    if (changed) { moved = true; writeCol(newTiles, c, n, merged) }
                    gainedScore += score
                }
            }
            Direction.DOWN -> {
                for (c in 0 until n) {
                    val line = extractCol(newTiles, c, n).reversed()
                    val (merged, score, changed) = mergeLine(line)
                    if (changed) { moved = true; writeCol(newTiles, c, n, merged.reversed()) }
                    gainedScore += score
                }
            }
        }

        val snapshot = if (moved) TwentyFortyEightSnapshot(state.tiles, state.score) else state.undoSnapshot
        val newBestTile = if (moved) maxOf(state.bestTile, newTiles.maxOrNull() ?: 0L) else state.bestTile
        val newState = state.copy(
            tiles = newTiles,
            score = state.score + gainedScore,
            bestTile = newBestTile,
            moves = state.moves + if (moved) 1 else 0,
            undoSnapshot = snapshot
        )
        return MoveResult(newState, gainedScore, moved)
    }

    /**
     * Reverts the last move. Returns the state unchanged when undo is not available.
     */
    fun undo(state: TwentyFortyEightState): TwentyFortyEightState {
        val snap = state.undoSnapshot ?: return state
        return state.copy(
            tiles = snap.tiles,
            score = snap.score,
            moves = state.moves,
            undoSnapshot = null
        )
    }

    /**
     * Spawns a new tile (90% chance of 2, 10% chance of 4) in a random empty cell.
     * Returns the state unchanged when the board is full.
     */
    fun spawnTile(state: TwentyFortyEightState, random: Random = Random.Default): TwentyFortyEightState {
        val empty = state.tiles.indices.filter { state.tiles[it] == 0L }
        if (empty.isEmpty()) return state
        val pos = empty[random.nextInt(empty.size)]
        val value = if (random.nextFloat() < 0.9f) 2L else 4L
        return state.copy(tiles = state.tiles.toMutableList().also { it[pos] = value })
    }

    /** Returns true if any tile on the board equals 2048. */
    fun hasWon(state: TwentyFortyEightState): Boolean =
        state.tiles.any { it >= 2048L }

    /**
     * Returns true if the player can make any valid move: either an empty cell
     * exists or two orthogonally adjacent tiles share the same value.
     */
    fun canMove(state: TwentyFortyEightState): Boolean {
        if (state.tiles.any { it == 0L }) return true
        val n = state.size
        for (r in 0 until n) {
            for (c in 0 until n) {
                val v = state.tile(r, c)
                if (c + 1 < n && state.tile(r, c + 1) == v) return true
                if (r + 1 < n && state.tile(r + 1, c) == v) return true
            }
        }
        return false
    }

    // -------------------------------------------------------------------------
    // Merge helpers
    // -------------------------------------------------------------------------

    /**
     * Merges a single line toward the leading edge (index 0).
     *
     * Algorithm:
     * 1. Collect non-zero values.
     * 2. Scan from the front; if two consecutive values are equal, merge them into
     *    one doubled tile (each tile merges at most once per move).
     * 3. Pad the result with zeros to preserve the original line length.
     *
     * Returns the merged line, the score gained from merges, and whether the line
     * differed from the input.
     */
    internal fun mergeLine(line: List<Long>): Triple<List<Long>, Long, Boolean> {
        val nonZero = line.filter { it != 0L }
        val result = mutableListOf<Long>()
        var score = 0L
        var i = 0
        while (i < nonZero.size) {
            if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
                val merged = nonZero[i] * 2
                result += merged
                score += merged
                i += 2
            } else {
                result += nonZero[i]
                i++
            }
        }
        repeat(line.size - result.size) { result += 0L }
        val changed = result != line
        return Triple(result, score, changed)
    }

    private fun extractRow(tiles: List<Long>, row: Int, n: Int): List<Long> =
        (0 until n).map { c -> tiles[row * n + c] }

    private fun writeRow(tiles: MutableList<Long>, row: Int, n: Int, values: List<Long>) {
        for (c in 0 until n) tiles[row * n + c] = values[c]
    }

    private fun extractCol(tiles: List<Long>, col: Int, n: Int): List<Long> =
        (0 until n).map { r -> tiles[r * n + col] }

    private fun writeCol(tiles: MutableList<Long>, col: Int, n: Int, values: List<Long>) {
        for (r in 0 until n) tiles[r * n + col] = values[r]
    }
}

/**
 * Abbreviates a large tile value to at most ~4 characters for display.
 *
 * Examples: 512 → "512", 1024 → "1k", 65536 → "65k", 1048576 → "1m".
 * Exact integer arithmetic (no rounding): always rounds down to the leading digits.
 */
fun abbreviate(value: Long): String = when {
    value < 1_000L -> value.toString()
    value < 1_000_000L -> "${value / 1_000}k"
    value < 1_000_000_000L -> "${value / 1_000_000}m"
    value < 1_000_000_000_000L -> "${value / 1_000_000_000}b"
    else -> "${value / 1_000_000_000_000}t"
}
