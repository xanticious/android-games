package com.xanticious.androidgames.controller.games.pegsolitaire

import com.xanticious.androidgames.model.games.puzzle.Direction
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.step
import com.xanticious.androidgames.model.games.pegsolitaire.BoardVariant
import com.xanticious.androidgames.model.games.pegsolitaire.CellState
import com.xanticious.androidgames.model.games.pegsolitaire.Jump
import com.xanticious.androidgames.model.games.pegsolitaire.PegSolitaireState

/**
 * Pure Peg Solitaire rules: board construction, legal-move generation, jump
 * application, undo and solve/stall detection.
 *
 * No Android or Compose imports — fully JVM unit-testable.
 */
class PegSolitaireController {

    /** Returns a fresh board for the given [variant] with the standard start hole empty. */
    fun newGame(variant: BoardVariant): PegSolitaireState =
        PegSolitaireState(variant = variant, board = buildBoard(variant))

    /** All legal jumps across the entire board. */
    fun legalMoves(state: PegSolitaireState): List<Jump> =
        buildList {
            for (r in 0 until state.rows) {
                for (c in 0 until state.cols) {
                    addAll(legalMovesFrom(state, GridPos(r, c)))
                }
            }
        }

    /** Legal jumps originating from a single cell [from]. */
    fun legalMovesFrom(state: PegSolitaireState, from: GridPos): List<Jump> {
        if (!state.inBounds(from) || state.cell(from) != CellState.PEG) return emptyList()
        return Direction.entries.mapNotNull { dir ->
            val over = from.step(dir)
            val to = over.step(dir)
            if (state.inBounds(over) && state.inBounds(to) &&
                state.cell(over) == CellState.PEG &&
                state.cell(to) == CellState.EMPTY
            ) Jump(from, over, to) else null
        }
    }

    /**
     * Applies [jump] if it is legal; returns the unchanged [state] otherwise.
     * The jumped peg is removed and the move is pushed to history for undo.
     */
    fun applyJump(state: PegSolitaireState, jump: Jump): PegSolitaireState {
        if (jump !in legalMovesFrom(state, jump.from)) return state
        val newBoard = state.board.map { it.toMutableList() }
        newBoard[jump.from.row][jump.from.col] = CellState.EMPTY
        newBoard[jump.over.row][jump.over.col] = CellState.EMPTY
        newBoard[jump.to.row][jump.to.col] = CellState.PEG
        return state.copy(
            board = newBoard.map { it.toList() },
            moves = state.moves + 1,
            history = state.history + listOf(state.board)
        )
    }

    /** Reverts the most recent jump; returns [state] unchanged if history is empty. */
    fun undo(state: PegSolitaireState): PegSolitaireState {
        val previous = state.history.lastOrNull() ?: return state
        return state.copy(
            board = previous,
            history = state.history.dropLast(1)
        )
    }

    /** True when exactly one peg remains on the board. */
    fun isSolved(state: PegSolitaireState): Boolean = pegsRemaining(state) == 1

    /** True when at least one legal jump exists. */
    fun hasMoves(state: PegSolitaireState): Boolean = legalMoves(state).isNotEmpty()

    /** Count of pegs currently on the board. */
    fun pegsRemaining(state: PegSolitaireState): Int =
        state.board.sumOf { row -> row.count { it == CellState.PEG } }

    /** Returns the center cell for [variant]'s board — used for the "finish in center" goal. */
    fun centerOf(variant: BoardVariant): GridPos = when (variant) {
        BoardVariant.ENGLISH -> GridPos(3, 3)
        BoardVariant.EUROPEAN -> GridPos(3, 3)
        BoardVariant.TRIANGLE -> GridPos(2, 1)
        BoardVariant.PLUS -> GridPos(2, 2)
    }

    // ── Board construction ────────────────────────────────────────────────────

    private fun buildBoard(variant: BoardVariant): List<List<CellState>> {
        val startEmpty = centerOf(variant)
        return List(variant.rows) { r ->
            List(variant.cols) { c ->
                when {
                    !isPartOfBoard(variant, r, c) -> CellState.BLOCKED
                    r == startEmpty.row && c == startEmpty.col -> CellState.EMPTY
                    else -> CellState.PEG
                }
            }
        }
    }

    private fun isPartOfBoard(variant: BoardVariant, r: Int, c: Int): Boolean = when (variant) {
        BoardVariant.ENGLISH ->
            // Rows 0-1 and 5-6 only cover cols 2-4; rows 2-4 span the full 7 columns.
            (c in 2..4) || (r in 2..4)

        BoardVariant.EUROPEAN ->
            // Rows 0,6 cover cols 2-4; rows 1,5 cover cols 1-5; rows 2-4 span all 7.
            (c in 2..4) || (r in 1..5 && c in 1..5) || (r in 2..4)

        BoardVariant.TRIANGLE ->
            // Right-triangle: row r has valid columns 0..r.
            c <= r

        BoardVariant.PLUS ->
            // Plus/cross: centre row/col plus the inner 3×3 ring.
            (r == 2) || (c == 2) || (r in 1..3 && c in 1..3)
    }
}
