package com.xanticious.androidgames.games.tictactoe

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.tictactoe.Mark
import com.xanticious.androidgames.state.games.tictactoe.TicTacToePhase
import com.xanticious.androidgames.state.games.tictactoe.TicTacToeStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TicTacToeStateMachineTest {
    private fun machine(seed: Int = 7) = TicTacToeStateMachine(
        difficulty = GameDifficulty.EASY,
        scope = CoroutineScope(Dispatchers.Unconfined),
        random = Random(seed)
    )

    @Test
    fun initialPhase_isPlayerTurn() {
        assertEquals(TicTacToePhase.PLAYER_TURN, machine().phase.value)
    }

    @Test
    fun selectCell_playerMove_placesX() {
        val m = machine()
        m.selectCell(0)
        assertEquals(Mark.X, m.state.value.board[0])
    }

    @Test
    fun selectCell_afterAiMove_returnsToPlayerTurn() {
        val m = machine()
        m.selectCell(0)
        assertEquals(TicTacToePhase.PLAYER_TURN, m.phase.value)
    }

    @Test
    fun selectCell_afterAiMove_placesOneO() {
        val m = machine()
        m.selectCell(0)
        assertEquals(1, m.state.value.board.count { it == Mark.O })
    }

    @Test
    fun selectCell_occupiedCell_ignoresMove() {
        val m = machine()
        m.selectCell(0)
        val board = m.state.value.board
        m.selectCell(0)
        assertEquals(board, m.state.value.board)
    }

    @Test
    fun reset_afterMoves_restoresEmptyBoard() {
        val m = machine()
        m.selectCell(0)
        m.reset()
        assertTrue(m.state.value.board.all { it == Mark.EMPTY })
    }

    @Test
    fun selectCell_completedRound_movesToGameOver() {
        val m = machine(seed = 2)
        listOf(0, 1, 2).forEach { m.selectCell(it) }
        assertEquals(TicTacToePhase.GAME_OVER, m.phase.value)
    }
}
