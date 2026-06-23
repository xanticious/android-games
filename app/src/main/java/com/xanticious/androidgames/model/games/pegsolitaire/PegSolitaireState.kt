package com.xanticious.androidgames.model.games.pegsolitaire

import com.xanticious.androidgames.model.games.puzzle.GridPos

/** State of a single board cell. */
enum class CellState { BLOCKED, EMPTY, PEG }

/** Available board layouts. rows/cols describe the bounding grid. */
enum class BoardVariant(val label: String, val rows: Int, val cols: Int) {
    ENGLISH("English (33)", 7, 7),
    EUROPEAN("European (37)", 7, 7),
    TRIANGLE("Triangle (15)", 5, 5),
    PLUS("Plus (13)", 5, 5)
}

/**
 * A single legal jump: peg at [from] leaps over the peg at [over] and lands
 * at [to] (which must be empty). The peg at [over] is removed.
 */
data class Jump(
    val from: GridPos,
    val over: GridPos,
    val to: GridPos
)

/**
 * Immutable snapshot of a Peg Solitaire game
 * (`design/puzzle-games/peg-solitaire`).
 *
 * [board] is a rows×cols grid; cells outside the playable shape are [CellState.BLOCKED].
 * [history] stores previous [board] snapshots so unlimited undo is supported.
 */
data class PegSolitaireState(
    val variant: BoardVariant,
    val board: List<List<CellState>>,
    val moves: Int = 0,
    val history: List<List<List<CellState>>> = emptyList()
) {
    val rows: Int get() = variant.rows
    val cols: Int get() = variant.cols
    val canUndo: Boolean get() = history.isNotEmpty()

    fun cell(pos: GridPos): CellState = board[pos.row][pos.col]
    fun inBounds(pos: GridPos): Boolean = pos.row in 0 until rows && pos.col in 0 until cols
}
