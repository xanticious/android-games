package com.xanticious.androidgames.games.paircollector

import com.xanticious.androidgames.state.games.paircollector.PairCollectorPhase
import com.xanticious.androidgames.state.games.paircollector.PairCollectorStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class PairCollectorStateMachineTest {
    private fun machine() = PairCollectorStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun startGame_movesToDealingRound() {
        val m = machine()
        m.startGame()
        assertEquals(PairCollectorPhase.DEALING_ROUND, m.phase.value)
    }

    @Test
    fun roundReady_movesToPlayingRound() {
        val m = machine()
        m.startGame()
        m.roundReady()
        assertEquals(PairCollectorPhase.PLAYING_ROUND, m.phase.value)
    }

    @Test
    fun secondCardMatched_movesToRoundComplete() {
        val m = machine()
        m.startGame()
        m.roundReady()
        m.secondCardMatched()
        assertEquals(PairCollectorPhase.ROUND_COMPLETE, m.phase.value)
    }

    @Test
    fun strikesExhausted_movesToGameOver() {
        val m = machine()
        m.startGame()
        m.roundReady()
        m.strikesExhausted()
        assertEquals(PairCollectorPhase.GAME_OVER, m.phase.value)
    }
}
