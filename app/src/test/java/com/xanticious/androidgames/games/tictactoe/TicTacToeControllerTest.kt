package com.xanticious.androidgames.games.tictactoe

import com.xanticious.androidgames.controller.games.tictactoe.TicTacToeController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.tictactoe.Mark
import com.xanticious.androidgames.model.games.tictactoe.TicTacToeConfig
import com.xanticious.androidgames.model.games.tictactoe.TicTacToeResult
import com.xanticious.androidgames.model.games.tictactoe.TicTacToeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TicTacToeControllerTest {
    private val controller = TicTacToeController()

    @Test
    fun applyMove_topRowCompleted_returnsWin() {
        val state = state(listOf(Mark.X, Mark.X, Mark.EMPTY, Mark.O, Mark.O, Mark.EMPTY, Mark.EMPTY, Mark.EMPTY, Mark.EMPTY), Mark.X)
        assertTrue(controller.applyMove(state, 2).result is TicTacToeResult.Win)
    }

    @Test
    fun chooseAiMove_mediumCanWin_takesWinningCell() {
        val state = state(listOf(Mark.O, Mark.O, Mark.EMPTY, Mark.X, Mark.X, Mark.EMPTY, Mark.EMPTY, Mark.EMPTY, Mark.EMPTY), Mark.O, GameDifficulty.MEDIUM)
        assertEquals(2, controller.chooseAiMove(state))
    }

    @Test
    fun chooseAiMove_mediumPlayerThreat_blocksThreat() {
        val state = state(listOf(Mark.X, Mark.X, Mark.EMPTY, Mark.O, Mark.EMPTY, Mark.EMPTY, Mark.EMPTY, Mark.EMPTY, Mark.O), Mark.O, GameDifficulty.MEDIUM)
        assertEquals(2, controller.chooseAiMove(state))
    }

    @Test
    fun chooseAiMove_hardPlayerHasFork_blocksFork() {
        val state = state(listOf(Mark.X, Mark.EMPTY, Mark.EMPTY, Mark.EMPTY, Mark.O, Mark.EMPTY, Mark.EMPTY, Mark.EMPTY, Mark.X), Mark.O, GameDifficulty.HARD)
        assertTrue(controller.chooseAiMove(state) in listOf(1, 3, 5, 7))
    }

    @Test
    fun chooseAiMove_hardAgainstAllPlayerLines_neverLoses() {
        assertFalse(playerCanForceWin(TicTacToeState.initial(TicTacToeConfig(GameDifficulty.HARD))))
    }

    private fun playerCanForceWin(state: TicTacToeState): Boolean {
        when (val result = state.result) {
            TicTacToeResult.Draw -> return false
            TicTacToeResult.InProgress -> Unit
            is TicTacToeResult.Win -> return result.winner == state.config.playerMark
        }
        return controller.legalMoves(state).any { playerMove ->
            val afterPlayer = controller.applyMove(state, playerMove)
            when (val result = afterPlayer.result) {
                TicTacToeResult.Draw -> false
                is TicTacToeResult.Win -> result.winner == state.config.playerMark
                TicTacToeResult.InProgress -> {
                    val aiMove = controller.chooseAiMove(afterPlayer)
                    playerCanForceWin(controller.applyMove(afterPlayer, aiMove))
                }
            }
        }
    }

    private fun state(board: List<Mark>, currentMark: Mark, difficulty: GameDifficulty = GameDifficulty.HARD): TicTacToeState {
        val config = TicTacToeConfig(difficulty = difficulty)
        return TicTacToeState(board = board, config = config, currentMark = currentMark, result = controller.evaluate(board))
    }
}
