package com.xanticious.androidgames.games.dominoes

import com.xanticious.androidgames.state.games.dominoes.DominoesPhase
import com.xanticious.androidgames.state.games.dominoes.DominoesStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class DominoesStateMachineTest {
    private fun machine() = DominoesStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun playerMoved_movesToAiTurn() {
        val m = machine()
        m.playerMoved()
        assertEquals(DominoesPhase.AI_TURN, m.phase.value)
    }

    @Test
    fun aiMoved_returnsToPlayerTurn() {
        val m = machine()
        m.playerMoved()
        m.aiMoved()
        assertEquals(DominoesPhase.PLAYER_TURN, m.phase.value)
    }

    @Test
    fun gameEnded_reachesGameOver() {
        val m = machine()
        m.gameEnded()
        assertEquals(DominoesPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun reset_afterGameOver_returnsToPlayerTurn() {
        val m = machine()
        m.gameEnded()
        m.reset()
        assertEquals(DominoesPhase.PLAYER_TURN, m.phase.value)
    }
}
