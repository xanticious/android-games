package com.xanticious.androidgames.games.connectfour

import com.xanticious.androidgames.state.games.connectfour.ConnectFourPhase
import com.xanticious.androidgames.state.games.connectfour.ConnectFourStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectFourStateMachineTest {
    private fun machine() = ConnectFourStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initialPhase_isPlayerTurn() {
        assertEquals(ConnectFourPhase.PLAYER_TURN, machine().phase.value)
    }

    @Test
    fun playerMoved_movesToAiTurn() {
        val machine = machine()
        machine.playerMoved()
        assertEquals(ConnectFourPhase.AI_TURN, machine.phase.value)
    }

    @Test
    fun aiMoved_movesToPlayerTurn() {
        val machine = machine()
        machine.playerMoved()
        machine.aiMoved()
        assertEquals(ConnectFourPhase.PLAYER_TURN, machine.phase.value)
    }

    @Test
    fun gameEnded_movesToGameOver() {
        val machine = machine()
        machine.gameEnded()
        assertEquals(ConnectFourPhase.GAME_OVER, machine.phase.value)
    }

    @Test
    fun reset_afterGameOver_movesToPlayerTurn() {
        val machine = machine()
        machine.gameEnded()
        machine.reset()
        assertEquals(ConnectFourPhase.PLAYER_TURN, machine.phase.value)
    }
}
