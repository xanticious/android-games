package com.xanticious.androidgames.games.reversi

import com.xanticious.androidgames.controller.games.reversi.ReversiController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.reversi.Disc
import com.xanticious.androidgames.model.games.reversi.REVERSI_BOARD_SIZE
import com.xanticious.androidgames.model.games.reversi.ReversiPosition
import com.xanticious.androidgames.model.games.reversi.ReversiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReversiControllerTest {
    private val controller = ReversiController()
    private val config = controller.configFor(GameDifficulty.MEDIUM)

    @Test
    fun initialState_hasFourCenterDiscs() {
        val state = controller.initialState(config)
        assertEquals(4, state.blackCount + state.whiteCount)
    }

    @Test
    fun legalMoves_initialBlack_hasFourMoves() {
        val state = controller.initialState(config)
        assertEquals(4, controller.legalMoves(state).size)
    }

    @Test
    fun applyMove_knownOpening_flipsOneWhiteDisc() {
        val next = controller.applyMove(controller.initialState(config), 2, 3)
        assertEquals(Disc.BLACK, next.board[3][3])
    }

    @Test
    fun legalMoves_initialBlack_containsD3() {
        val moves = controller.legalMoves(controller.initialState(config)).map { it.position }
        assertEquals(true, ReversiPosition(2, 3) in moves)
    }

    @Test
    fun skipUnavailableTurns_whiteHasNoMove_advancesToBlack() {
        val board = mostlyBlackBoard(empty = ReversiPosition(0, 0), white = ReversiPosition(0, 1))
        val state = ReversiState.fromBoard(board, Disc.WHITE, config)
        assertEquals(Disc.BLACK, controller.skipUnavailableTurns(state).currentPlayer)
    }

    @Test
    fun skipUnavailableTurns_whiteHasNoMove_keepsGameRunning() {
        val board = mostlyBlackBoard(empty = ReversiPosition(0, 0), white = ReversiPosition(0, 1))
        val state = ReversiState.fromBoard(board, Disc.WHITE, config)
        assertNull(controller.skipUnavailableTurns(state).result)
    }

    @Test
    fun skipUnavailableTurns_fullBoard_setsWinnerByCount() {
        val board = List(REVERSI_BOARD_SIZE) { row ->
            List(REVERSI_BOARD_SIZE) { col -> if (row < 5 || (row == 5 && col < 4)) Disc.BLACK else Disc.WHITE }
        }
        val state = ReversiState.fromBoard(board, Disc.BLACK, config)
        assertEquals(Disc.BLACK, controller.skipUnavailableTurns(state).result?.winner)
    }

    private fun mostlyBlackBoard(empty: ReversiPosition, white: ReversiPosition): List<List<Disc>> =
        List(REVERSI_BOARD_SIZE) { row ->
            List(REVERSI_BOARD_SIZE) { col ->
                when (ReversiPosition(row, col)) {
                    empty -> Disc.EMPTY
                    white -> Disc.WHITE
                    else -> Disc.BLACK
                }
            }
        }
}
