package com.xanticious.androidgames.games.pegsolitaire

import com.xanticious.androidgames.controller.games.pegsolitaire.PegSolitaireController
import com.xanticious.androidgames.model.games.pegsolitaire.BoardVariant
import com.xanticious.androidgames.model.games.pegsolitaire.CellState
import com.xanticious.androidgames.model.games.pegsolitaire.Jump
import com.xanticious.androidgames.model.games.puzzle.GridPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PegSolitaireControllerTest {

    private val controller = PegSolitaireController()

    // ── Board construction ────────────────────────────────────────────────────

    @Test
    fun newGame_english_has33Pegs() {
        val state = controller.newGame(BoardVariant.ENGLISH)
        // 33 holes total, center starts empty → 32 pegs
        assertEquals(32, controller.pegsRemaining(state))
    }

    @Test
    fun newGame_european_has37Holes() {
        val state = controller.newGame(BoardVariant.EUROPEAN)
        val total = state.board.sumOf { row -> row.count { it != CellState.BLOCKED } }
        assertEquals(37, total)
    }

    @Test
    fun newGame_triangle_has15Holes() {
        val state = controller.newGame(BoardVariant.TRIANGLE)
        val total = state.board.sumOf { row -> row.count { it != CellState.BLOCKED } }
        assertEquals(15, total)
    }

    @Test
    fun newGame_plus_has13Holes() {
        val state = controller.newGame(BoardVariant.PLUS)
        val total = state.board.sumOf { row -> row.count { it != CellState.BLOCKED } }
        assertEquals(13, total)
    }

    @Test
    fun newGame_english_centerIsEmpty() {
        val state = controller.newGame(BoardVariant.ENGLISH)
        assertEquals(CellState.EMPTY, state.cell(GridPos(3, 3)))
    }

    @Test
    fun newGame_plus_centerIsEmpty() {
        val state = controller.newGame(BoardVariant.PLUS)
        assertEquals(CellState.EMPTY, state.cell(GridPos(2, 2)))
    }

    // ── Legal moves ───────────────────────────────────────────────────────────

    @Test
    fun legalMovesFrom_emptyCell_returnsEmpty() {
        val state = controller.newGame(BoardVariant.ENGLISH)
        // Center (3,3) is empty at start
        assertTrue(controller.legalMovesFrom(state, GridPos(3, 3)).isEmpty())
    }

    @Test
    fun legalMovesFrom_blockedCell_returnsEmpty() {
        val state = controller.newGame(BoardVariant.ENGLISH)
        // (0,0) is BLOCKED on the English board
        assertTrue(controller.legalMovesFrom(state, GridPos(0, 0)).isEmpty())
    }

    @Test
    fun legalMoves_english_initialBoard_hasCorrectMoves() {
        val state = controller.newGame(BoardVariant.ENGLISH)
        // English board: centre (3,3) empty. Pegs that can jump into it:
        // (3,1) over (3,2) → (3,3), (1,3) over (2,3) → (3,3),
        // (5,3) over (4,3) → (3,3), (3,5) over (3,4) → (3,3)
        val moves = controller.legalMoves(state)
        assertEquals(4, moves.size)
    }

    // ── Jump application ──────────────────────────────────────────────────────

    @Test
    fun applyJump_removesJumpedPegAndMovesPeg() {
        val state = controller.newGame(BoardVariant.ENGLISH)
        // Jump peg at (3,1) over (3,2) into the empty (3,3)
        val jump = Jump(from = GridPos(3, 1), over = GridPos(3, 2), to = GridPos(3, 3))
        val next = controller.applyJump(state, jump)
        assertEquals(CellState.EMPTY, next.cell(GridPos(3, 1)))
        assertEquals(CellState.EMPTY, next.cell(GridPos(3, 2)))
        assertEquals(CellState.PEG, next.cell(GridPos(3, 3)))
    }

    @Test
    fun applyJump_incrementsMoveCount() {
        val state = controller.newGame(BoardVariant.ENGLISH)
        val jump = Jump(from = GridPos(3, 1), over = GridPos(3, 2), to = GridPos(3, 3))
        val next = controller.applyJump(state, jump)
        assertEquals(1, next.moves)
    }

    @Test
    fun applyJump_illegalJump_returnsUnchangedState() {
        val state = controller.newGame(BoardVariant.ENGLISH)
        // (3,3) is EMPTY — cannot jump from there
        val illegalJump = Jump(from = GridPos(3, 3), over = GridPos(3, 4), to = GridPos(3, 5))
        val next = controller.applyJump(state, illegalJump)
        assertEquals(state.board, next.board)
    }

    @Test
    fun applyJump_decreasesPegCount() {
        val state = controller.newGame(BoardVariant.ENGLISH)
        val before = controller.pegsRemaining(state)
        val jump = Jump(from = GridPos(3, 1), over = GridPos(3, 2), to = GridPos(3, 3))
        val next = controller.applyJump(state, jump)
        assertEquals(before - 1, controller.pegsRemaining(next))
    }

    // ── Undo ──────────────────────────────────────────────────────────────────

    @Test
    fun undo_afterJump_restoresBoard() {
        val state = controller.newGame(BoardVariant.ENGLISH)
        val jump = Jump(from = GridPos(3, 1), over = GridPos(3, 2), to = GridPos(3, 3))
        val jumped = controller.applyJump(state, jump)
        val undone = controller.undo(jumped)
        assertEquals(state.board, undone.board)
    }

    @Test
    fun undo_withNoHistory_returnsUnchanged() {
        val state = controller.newGame(BoardVariant.ENGLISH)
        val undone = controller.undo(state)
        assertEquals(state.board, undone.board)
    }

    @Test
    fun undo_clearsHistory() {
        val state = controller.newGame(BoardVariant.ENGLISH)
        val jump = Jump(from = GridPos(3, 1), over = GridPos(3, 2), to = GridPos(3, 3))
        val jumped = controller.applyJump(state, jump)
        val undone = controller.undo(jumped)
        assertFalse(undone.canUndo)
    }

    // ── Solved / hasMoves ─────────────────────────────────────────────────────

    @Test
    fun isSolved_withOnePeg_returnsTrue() {
        val state = controller.newGame(BoardVariant.PLUS)
        // Manually build a board with exactly one peg at center
        val singlePegBoard = state.board.mapIndexed { r, row ->
            row.mapIndexed { c, cell ->
                when {
                    cell == CellState.BLOCKED -> CellState.BLOCKED
                    r == 2 && c == 2 -> CellState.PEG
                    else -> CellState.EMPTY
                }
            }
        }
        val solved = state.copy(board = singlePegBoard)
        assertTrue(controller.isSolved(solved))
    }

    @Test
    fun isSolved_withMultiplePegs_returnsFalse() {
        val state = controller.newGame(BoardVariant.ENGLISH)
        assertFalse(controller.isSolved(state))
    }

    @Test
    fun hasMoves_freshEnglishBoard_returnsTrue() {
        val state = controller.newGame(BoardVariant.ENGLISH)
        assertTrue(controller.hasMoves(state))
    }

    @Test
    fun hasMoves_stuckBoard_returnsFalse() {
        // Build a board where isolated pegs cannot jump (no two adjacent pegs with empty beyond)
        val state = controller.newGame(BoardVariant.PLUS)
        // Place pegs only at corners of the Plus where no jump is possible
        val stuckBoard = state.board.mapIndexed { r, row ->
            row.mapIndexed { c, cell ->
                when {
                    cell == CellState.BLOCKED -> CellState.BLOCKED
                    r == 0 && c == 2 -> CellState.PEG
                    r == 4 && c == 2 -> CellState.PEG
                    else -> CellState.EMPTY
                }
            }
        }
        val stuck = state.copy(board = stuckBoard)
        assertFalse(controller.hasMoves(stuck))
    }

    // ── pegsRemaining ─────────────────────────────────────────────────────────

    @Test
    fun pegsRemaining_afterJump_decreasesByOne() {
        val state = controller.newGame(BoardVariant.ENGLISH)
        val jump = Jump(from = GridPos(3, 1), over = GridPos(3, 2), to = GridPos(3, 3))
        val next = controller.applyJump(state, jump)
        assertEquals(controller.pegsRemaining(state) - 1, controller.pegsRemaining(next))
    }
}
