package com.xanticious.androidgames.games.jigsaw

import com.xanticious.androidgames.controller.games.jigsaw.JigsawController
import com.xanticious.androidgames.model.games.puzzle.GridPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class JigsawControllerTest {
    private val ctrl = JigsawController()

    // ── newGame ──────────────────────────────────────────────────────────────

    @Test
    fun newGame_pieceCountEqualsRowsTimesCols() {
        val state = ctrl.newGame(imageId = 0, rows = 3, cols = 4, random = Random(1))
        assertEquals(12, state.pieces.size)
    }

    @Test
    fun newGame_allPiecesInBank() {
        val state = ctrl.newGame(imageId = 0, rows = 3, cols = 4, random = Random(1))
        assertEquals(12, state.bankOrder.size)
    }

    @Test
    fun newGame_nonePiecesPlaced() {
        val state = ctrl.newGame(imageId = 0, rows = 3, cols = 4, random = Random(1))
        assertTrue(state.placedPieces.isEmpty())
    }

    @Test
    fun newGame_bankContainsAllPieceIds() {
        val state = ctrl.newGame(imageId = 0, rows = 4, cols = 5, random = Random(42))
        assertEquals(state.pieces.map { it.id }.sorted(), state.bankOrder.sorted())
    }

    @Test
    fun newGame_sameSeed_sameShuffleOrder() {
        val a = ctrl.newGame(imageId = 1, rows = 3, cols = 4, random = Random(99))
        val b = ctrl.newGame(imageId = 1, rows = 3, cols = 4, random = Random(99))
        assertEquals(a.bankOrder, b.bankOrder)
    }

    @Test
    fun newGame_differentSeeds_differentOrder() {
        val a = ctrl.newGame(imageId = 0, rows = 4, cols = 5, random = Random(7))
        val b = ctrl.newGame(imageId = 0, rows = 4, cols = 5, random = Random(8))
        // With 20 pieces and two different seeds it is astronomically unlikely they match.
        assertFalse(a.bankOrder == b.bankOrder)
    }

    @Test
    fun newGame_eachPieceHasCorrectHomeCell() {
        val rows = 3; val cols = 4
        val state = ctrl.newGame(imageId = 0, rows = rows, cols = cols, random = Random(0))
        state.pieces.forEach { piece ->
            assertEquals(piece.id / cols, piece.correctRow)
            assertEquals(piece.id % cols, piece.correctCol)
        }
    }

    // ── placePiece ───────────────────────────────────────────────────────────

    @Test
    fun placePiece_correctCell_returnsUpdatedState() {
        val state = ctrl.newGame(imageId = 0, rows = 3, cols = 4, random = Random(1))
        val piece = state.pieces.first()
        val result = ctrl.placePiece(state, piece.id, piece.correctRow, piece.correctCol)
        assertNotNull(result)
    }

    @Test
    fun placePiece_correctCell_pieceRemovedFromBank() {
        val state = ctrl.newGame(imageId = 0, rows = 3, cols = 4, random = Random(1))
        val piece = state.pieces.first()
        val next = ctrl.placePiece(state, piece.id, piece.correctRow, piece.correctCol)!!
        assertFalse(next.bankOrder.contains(piece.id))
    }

    @Test
    fun placePiece_correctCell_pieceAppearsOnBoard() {
        val state = ctrl.newGame(imageId = 0, rows = 3, cols = 4, random = Random(1))
        val piece = state.pieces.first()
        val next = ctrl.placePiece(state, piece.id, piece.correctRow, piece.correctCol)!!
        assertEquals(piece.id, next.placedPieces[GridPos(piece.correctRow, piece.correctCol)])
    }

    @Test
    fun placePiece_wrongCell_isRejected() {
        val state = ctrl.newGame(imageId = 0, rows = 3, cols = 4, random = Random(1))
        val piece = state.pieces.first()
        val wrongRow = (piece.correctRow + 1) % 3
        val result = ctrl.placePiece(state, piece.id, wrongRow, piece.correctCol)
        assertNull(result)
    }

    @Test
    fun placePiece_wrongColumn_isRejected() {
        val state = ctrl.newGame(imageId = 0, rows = 3, cols = 4, random = Random(1))
        val piece = state.pieces.first()
        val wrongCol = (piece.correctCol + 1) % 4
        val result = ctrl.placePiece(state, piece.id, piece.correctRow, wrongCol)
        assertNull(result)
    }

    @Test
    fun placePiece_unknownPieceId_isRejected() {
        val state = ctrl.newGame(imageId = 0, rows = 3, cols = 4, random = Random(1))
        val result = ctrl.placePiece(state, 999, 0, 0)
        assertNull(result)
    }

    @Test
    fun placePiece_occupiedCell_isRejected() {
        var state = ctrl.newGame(imageId = 0, rows = 3, cols = 4, random = Random(1))
        val p1 = state.pieces[0]
        state = ctrl.placePiece(state, p1.id, p1.correctRow, p1.correctCol)!!
        // Try to place another piece at the same cell (using a wrong piece that has correctCell at (0,0))
        // Actually we just try to place p1 again — but it's no longer in the bank, so the call won't find it.
        // Instead find a second piece that also has correctRow/Col != 0,0 and try placing it there.
        val p2 = state.pieces.first { it.id != p1.id }
        val result = ctrl.placePiece(state, p2.id, p1.correctRow, p1.correctCol)
        assertNull(result)
    }

    // ── isSolved ─────────────────────────────────────────────────────────────

    @Test
    fun isSolved_freshGame_isFalse() {
        val state = ctrl.newGame(imageId = 0, rows = 3, cols = 4, random = Random(1))
        assertFalse(ctrl.isSolved(state))
    }

    @Test
    fun isSolved_partialBoard_isFalse() {
        var state = ctrl.newGame(imageId = 0, rows = 3, cols = 4, random = Random(1))
        val piece = state.pieces.first()
        state = ctrl.placePiece(state, piece.id, piece.correctRow, piece.correctCol)!!
        assertFalse(ctrl.isSolved(state))
    }

    @Test
    fun isSolved_allPiecesCorrectlyPlaced_isTrue() {
        var state = ctrl.newGame(imageId = 0, rows = 3, cols = 4, random = Random(1))
        for (piece in state.pieces.toList()) {
            state = ctrl.placePiece(state, piece.id, piece.correctRow, piece.correctCol)!!
        }
        assertTrue(ctrl.isSolved(state))
    }

    // ── removePiece ──────────────────────────────────────────────────────────

    @Test
    fun removePiece_placedPiece_returnsToBank() {
        var state = ctrl.newGame(imageId = 0, rows = 3, cols = 4, random = Random(1))
        val piece = state.pieces.first()
        state = ctrl.placePiece(state, piece.id, piece.correctRow, piece.correctCol)!!
        state = ctrl.removePiece(state, piece.correctRow, piece.correctCol)
        assertTrue(state.bankOrder.contains(piece.id))
        assertFalse(state.placedPieces.containsKey(GridPos(piece.correctRow, piece.correctCol)))
    }

    @Test
    fun removePiece_emptyCell_stateUnchanged() {
        val state = ctrl.newGame(imageId = 0, rows = 3, cols = 4, random = Random(1))
        val next = ctrl.removePiece(state, 0, 0)
        assertEquals(state, next)
    }

    // ── placedCount ──────────────────────────────────────────────────────────

    @Test
    fun placedCount_increments_withEachPlacement() {
        var state = ctrl.newGame(imageId = 0, rows = 3, cols = 4, random = Random(1))
        assertEquals(0, ctrl.placedCount(state))
        val piece = state.pieces.first()
        state = ctrl.placePiece(state, piece.id, piece.correctRow, piece.correctCol)!!
        assertEquals(1, ctrl.placedCount(state))
    }
}
