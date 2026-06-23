package com.xanticious.androidgames.games.hex

import com.xanticious.androidgames.state.games.hex.HexPhase
import com.xanticious.androidgames.state.games.hex.HexStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class HexStateMachineTest {
    private fun machine() = HexStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initialPhase_newMachine_startsOnPlayerTurn() {
        assertEquals(HexPhase.PLAYER_TURN, machine().phase.value)
    }

    @Test
    fun playerMoved_validTurn_movesToAiTurn() {
        val m = machine()
        m.playerMoved()
        assertEquals(HexPhase.AI_TURN, m.phase.value)
    }

    @Test
    fun aiMoved_validTurn_movesToPlayerTurn() {
        val m = machine()
        m.playerMoved()
        m.aiMoved()
        assertEquals(HexPhase.PLAYER_TURN, m.phase.value)
    }

    @Test
    fun gameEnded_fromPlayerTurn_movesToGameOver() {
        val m = machine()
        m.gameEnded()
        assertEquals(HexPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun rematch_afterGameOver_movesToPlayerTurn() {
        val m = machine()
        m.gameEnded()
        m.rematch()
        assertEquals(HexPhase.PLAYER_TURN, m.phase.value)
    }
}
