package com.xanticious.androidgames.model.games.sudokucolors

import com.xanticious.androidgames.model.games.puzzle.GridPos

/**
 * Immutable data for one Sudoku Colors cell.
 *
 * @param value     0 = empty; 1–9 = color index placed by player or given.
 * @param isGiven   true if this cell was pre-filled by the puzzle generator (locked).
 * @param pencilMarks candidate color indices (1–9) the player has noted.
 * @param isConflict true if this cell's value duplicates another value in its row,
 *                   column, or 3×3 box.
 */
data class SudokuColorsCellData(
    val value: Int = 0,
    val isGiven: Boolean = false,
    val pencilMarks: Set<Int> = emptySet(),
    val isConflict: Boolean = false
)

/**
 * Complete immutable state for a Sudoku Colors game in progress.
 *
 * @param cells              81 cells in row-major order (row 0 col 0 … row 8 col 8).
 * @param solution           The unique solution — 81 ints, each 1–9.
 * @param selectedPos        Currently highlighted cell, or null.
 * @param pencilMode         When true, color taps toggle pencil marks instead of setting values.
 * @param showDigits         When true, digit overlays (1–9) are drawn on each color swatch.
 * @param highlightConflicts When true, conflicting cells are visually flagged.
 * @param history            Snapshots of [cells] before each mutating action (for undo).
 * @param moves              Number of mutating actions taken so far (includes undone moves).
 */
data class SudokuColorsState(
    val cells: List<SudokuColorsCellData>,
    val solution: List<Int>,
    val selectedPos: GridPos? = null,
    val pencilMode: Boolean = false,
    val showDigits: Boolean = false,
    val highlightConflicts: Boolean = true,
    val history: List<List<SudokuColorsCellData>> = emptyList(),
    val moves: Int = 0
) {
    val canUndo: Boolean get() = history.isNotEmpty()

    /** Number of unfilled cells remaining. */
    val emptyCellCount: Int get() = cells.count { it.value == 0 }
}
