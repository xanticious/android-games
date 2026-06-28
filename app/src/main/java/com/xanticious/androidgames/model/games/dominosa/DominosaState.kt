package com.xanticious.androidgames.model.games.dominosa

import com.xanticious.androidgames.model.games.puzzle.GridPos

/**
 * A canonical domino value-pair where [a] ≤ [b].
 *
 * The ordering is important: (1,3) and (3,1) refer to the same physical domino
 * and are both represented as DominosaPair(1,3).
 */
data class DominosaPair(val a: Int, val b: Int) : Comparable<DominosaPair> {
    init { require(a <= b) { "DominosaPair requires a ≤ b, got ($a,$b)" } }

    override fun compareTo(other: DominosaPair): Int =
        compareValuesBy(this, other, { it.a }, { it.b })

    companion object {
        /** Factory that normalises so [a] ≤ [b]. */
        fun of(x: Int, y: Int): DominosaPair = if (x <= y) DominosaPair(x, y) else DominosaPair(y, x)
    }
}

/**
 * A placed domino covering two orthogonally adjacent cells.
 *
 * [cell1] is always the canonical "first" cell in row-major order: the topmost
 * cell for vertical dominoes and the leftmost cell for horizontal ones.
 */
data class DominoPlacement(val cell1: GridPos, val cell2: GridPos)

/**
 * Full Dominosa game state for a puzzle of size [n] (dominoes 0–[n]).
 *
 * Grid dimensions: [rows] = n+1, [cols] = n+2. [grid] stores the pip number for
 * every cell in row-major order. [solution] is the hidden reference tiling used
 * for hints and solve-check. [placed] is the player's current partition. [selected]
 * holds the first cell of a pending two-tap placement.
 */
data class DominosaState(
    /** Highest pip value; determines domino set and grid size. */
    val n: Int,
    /** Pip values indexed as grid[row][col]. */
    val grid: List<List<Int>>,
    /** Hidden reference tiling — every domino placed exactly once. */
    val solution: List<DominoPlacement>,
    /** Player's placed dominoes. */
    val placed: List<DominoPlacement>,
    /** First cell of a pending two-tap placement; null when none is pending. */
    val selected: GridPos?,
    /** Previous [placed] snapshots, oldest first; used for undo. */
    val history: List<List<DominoPlacement>> = emptyList()
) {
    val rows: Int get() = n + 1
    val cols: Int get() = n + 2

    /** Pip value at [pos]. */
    fun pipAt(pos: GridPos): Int = grid[pos.row][pos.col]

    /** True when [pos] is covered by any placed domino. */
    fun isOccupied(pos: GridPos): Boolean =
        placed.any { it.cell1 == pos || it.cell2 == pos }

    /** Returns the placed domino that covers [pos], or null if the cell is free. */
    fun dominoAt(pos: GridPos): DominoPlacement? =
        placed.firstOrNull { it.cell1 == pos || it.cell2 == pos }

    val canUndo: Boolean get() = history.isNotEmpty()
}
