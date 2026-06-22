package com.xanticious.androidgames.model.games.nonogram

/** The possible states a player can assign to a nonogram cell. */
enum class CellState { EMPTY, FILLED, MARKED }

/**
 * Immutable nonogram puzzle state.
 *
 * [solution] is the target bitmap (true = filled), stored row-major.
 * [cells] is the player's current mark grid, also row-major.
 * [rowClues] / [colClues] are the run-length sequences derived from [solution].
 * [history] supports undo — each entry is the full cells list before the last action.
 */
data class NonogramState(
    val size: Int,
    val solution: List<Boolean>,
    val cells: List<CellState>,
    val rowClues: List<List<Int>>,
    val colClues: List<List<Int>>,
    val history: List<List<CellState>> = emptyList()
) {
    val canUndo: Boolean get() = history.isNotEmpty()
}
