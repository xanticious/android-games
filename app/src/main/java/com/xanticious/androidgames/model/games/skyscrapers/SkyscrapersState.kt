package com.xanticious.androidgames.model.games.skyscrapers

import com.xanticious.androidgames.model.games.puzzle.GridPos

/**
 * Edge clues for all four sides of the grid.
 * Each list has [size] entries corresponding to each column (top/bottom) or row (left/right).
 */
data class SkyscrapersClues(
    val top: List<Int>,
    val bottom: List<Int>,
    val left: List<Int>,
    val right: List<Int>
)

/** Saved checkpoint for undo support. */
data class SkyscrapersSnapshot(
    val grid: List<Int>,
    val pencilMarks: Map<Int, Set<Int>>
)

/**
 * Immutable game state for a Skyscrapers puzzle (`design/puzzle-games/skyscrapers`).
 *
 * The board is an N×N grid stored flat in row-major order. Values are heights
 * 1..N; 0 means the cell is empty. [solution] is the unique correct answer used
 * only to derive clues — it is never shown to the player.
 */
data class SkyscrapersState(
    val size: Int,
    /** Flat row-major solution grid, values 1..size. */
    val solution: List<Int>,
    val clues: SkyscrapersClues,
    /** Flat row-major player grid, 0 = empty. */
    val grid: List<Int>,
    /** Flat indices of cells pre-filled as givens (immutable during play). */
    val givens: Set<Int>,
    /** Pencil-mark candidates per flat cell index. */
    val pencilMarks: Map<Int, Set<Int>> = emptyMap(),
    /** Currently selected flat cell index; -1 = nothing selected. */
    val selectedIndex: Int = -1,
    val history: List<SkyscrapersSnapshot> = emptyList()
) {
    val canUndo: Boolean get() = history.isNotEmpty()

    /** GridPos of the selected cell, or null when nothing is selected. */
    val selectedPos: GridPos?
        get() = if (selectedIndex < 0) null else GridPos(selectedIndex / size, selectedIndex % size)

    /** Flat index from a [GridPos]. */
    fun indexOf(pos: GridPos): Int = pos.row * size + pos.col
}
