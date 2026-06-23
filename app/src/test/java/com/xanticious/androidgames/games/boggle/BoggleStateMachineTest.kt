package com.xanticious.androidgames.games.boggle

import com.xanticious.androidgames.state.games.boggle.BogglePhase
import com.xanticious.androidgames.state.games.boggle.BoggleStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class BoggleStateMachineTest {

    @Test
    fun initialPhase_isIdle() {
        val machine = BoggleStateMachine(CoroutineScope(Dispatchers.Unconfined))
        assertEquals(BogglePhase.IDLE, machine.phase.value)
    }

    @Test
    fun roundStarted_transitionsToPlaying() {
        val machine = BoggleStateMachine(CoroutineScope(Dispatchers.Unconfined))
        machine.roundStarted()
        assertEquals(BogglePhase.PLAYING, machine.phase.value)
    }

    @Test
    fun timeExpired_transitionsToRoundOver() {
        val machine = BoggleStateMachine(CoroutineScope(Dispatchers.Unconfined))
        machine.roundStarted()
        machine.timeExpired()
        assertEquals(BogglePhase.ROUND_OVER, machine.phase.value)
    }

    @Test
    fun gaveUp_transitionsToRoundOver() {
        val machine = BoggleStateMachine(CoroutineScope(Dispatchers.Unconfined))
        machine.roundStarted()
        machine.gaveUp()
        assertEquals(BogglePhase.ROUND_OVER, machine.phase.value)
    }

    @Test
    fun newRound_transitionsToIdle() {
        val machine = BoggleStateMachine(CoroutineScope(Dispatchers.Unconfined))
        machine.roundStarted()
        machine.timeExpired()
        machine.newRound()
        assertEquals(BogglePhase.IDLE, machine.phase.value)
    }
}
