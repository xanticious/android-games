package com.xanticious.androidgames.games.zilch

import com.xanticious.androidgames.state.games.zilch.ZilchPhase
import com.xanticious.androidgames.state.games.zilch.ZilchStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class ZilchStateMachineTest {
    private fun machine() = ZilchStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun startMatch_movesToPlayerRolling() {
        val m = machine()
        m.startMatch()
        assertEquals(ZilchPhase.PLAYER_ROLLING, m.phase.value)
    }

    @Test
    fun diceRolled_movesToPlayerDeciding() {
        val m = machine()
        m.startMatch()
        m.diceRolled()
        assertEquals(ZilchPhase.PLAYER_DECIDING, m.phase.value)
    }

    @Test
    fun playerBanked_movesToAiTurn() {
        val m = machine()
        m.startMatch()
        m.diceRolled()
        m.playerBanked()
        assertEquals(ZilchPhase.AI_TURN, m.phase.value)
    }

    @Test
    fun aiTurnResolved_movesToPlayerRolling() {
        val m = machine()
        m.startMatch()
        m.diceRolled()
        m.playerBanked()
        m.aiTurnResolved()
        assertEquals(ZilchPhase.PLAYER_ROLLING, m.phase.value)
    }

    @Test
    fun matchEnded_movesToGameOver() {
        val m = machine()
        m.startMatch()
        m.matchEnded()
        assertEquals(ZilchPhase.GAME_OVER, m.phase.value)
    }
}
