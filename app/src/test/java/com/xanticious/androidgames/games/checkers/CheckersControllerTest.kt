package com.xanticious.androidgames.games.checkers

import com.xanticious.androidgames.controller.games.checkers.CheckersController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.checkers.CheckersPiece
import com.xanticious.androidgames.model.games.checkers.CheckersSide
import com.xanticious.androidgames.model.games.checkers.CheckersSquare
import com.xanticious.androidgames.model.games.checkers.CheckersState
import com.xanticious.androidgames.model.games.checkers.CheckersWinReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckersControllerTest {
    private val controller = CheckersController()
    private val config = controller.configFor(GameDifficulty.MEDIUM)

    @Test
    fun legalMoves_initialRedPiece_hasSimpleMove() {
        val state = CheckersState.initial(config)
        assertTrue(controller.legalMovesForPiece(state, CheckersSquare(5, 0)).any { it.to == CheckersSquare(4, 1) })
    }

    @Test
    fun applyMove_singleCapture_countsCapture() {
        val state = emptyState(
            CheckersSquare(5, 0) to CheckersPiece(CheckersSide.RED),
            CheckersSquare(4, 1) to CheckersPiece(CheckersSide.BLACK)
        )
        val after = controller.applySelectedMove(controller.selectSquare(state, CheckersSquare(5, 0)), CheckersSquare(3, 2))
        assertEquals(1, after.redCaptures)
    }

    @Test
    fun applyMove_multiJump_keepsSamePieceSelected() {
        val state = emptyState(
            CheckersSquare(5, 0) to CheckersPiece(CheckersSide.RED),
            CheckersSquare(4, 1) to CheckersPiece(CheckersSide.BLACK),
            CheckersSquare(2, 3) to CheckersPiece(CheckersSide.BLACK)
        )
        val after = controller.applySelectedMove(controller.selectSquare(state, CheckersSquare(5, 0)), CheckersSquare(3, 2))
        assertEquals(CheckersSquare(3, 2), after.continuingCaptureFrom)
    }

    @Test
    fun applyMove_backRow_promotesToKing() {
        val state = emptyState(CheckersSquare(1, 2) to CheckersPiece(CheckersSide.RED))
        val after = controller.applySelectedMove(controller.selectSquare(state, CheckersSquare(1, 2)), CheckersSquare(0, 1))
        assertEquals(true, after.board[0][1]?.isKing)
    }

    @Test
    fun finishIfGameOver_currentSideHasNoMoves_winsForOpponent() {
        val state = emptyState(
            CheckersSquare(7, 0) to CheckersPiece(CheckersSide.BLACK),
            CheckersSquare(0, 1) to CheckersPiece(CheckersSide.RED)
        ).copy(currentPlayer = CheckersSide.BLACK)
        val result = controller.finishIfGameOver(state).result
        assertEquals(CheckersWinReason.NO_LEGAL_MOVES, result?.reason)
    }

    private fun emptyState(vararg pieces: Pair<CheckersSquare, CheckersPiece>): CheckersState {
        var board = CheckersState.empty(config).board
        pieces.forEach { (square, piece) ->
            board = board.mapIndexed { row, cells ->
                if (row != square.row) cells else cells.mapIndexed { col, current -> if (col == square.col) piece else current }
            }
        }
        return CheckersState.empty(config).copy(board = board)
    }
}
