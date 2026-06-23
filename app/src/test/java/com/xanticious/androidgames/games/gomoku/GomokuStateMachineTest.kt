package com.xanticious.androidgames.games.gomoku

import com.xanticious.androidgames.state.games.gomoku.GomokuPhase
import com.xanticious.androidgames.state.games.gomoku.GomokuStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class GomokuStateMachineTest {
    private fun machine() = GomokuStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initialPhase_startsAtPlayerTurn() {
        assertEquals(GomokuPhase.PLAYER_TURN, machine().phase.value)
    }

    @Test
    fun playerMoved_movesToAiTurn() {
        val m = machine()
        m.playerMoved()
        assertEquals(GomokuPhase.AI_TURN, m.phase.value)
    }

    @Test
    fun aiMoved_movesToPlayerTurn() {
        val m = machine()
        m.playerMoved()
        m.aiMoved()
        assertEquals(GomokuPhase.PLAYER_TURN, m.phase.value)
    }

    @Test
    fun gameEnded_movesToGameOver() {
        val m = machine()
        m.gameEnded()
        assertEquals(GomokuPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun reset_afterGameOver_movesToPlayerTurn() {
        val m = machine()
        m.gameEnded()
        m.reset()
        assertEquals(GomokuPhase.PLAYER_TURN, m.phase.value)
    }
}
