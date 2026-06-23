package com.xanticious.androidgames.games.anagramsarcade

import com.xanticious.androidgames.state.games.anagramsarcade.AnagramsArcadePhase
import com.xanticious.androidgames.state.games.anagramsarcade.AnagramsArcadeStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class AnagramsArcadeStateMachineTest {

    @Test
    fun initialPhase_isIdle() {
        val machine = AnagramsArcadeStateMachine(CoroutineScope(Dispatchers.Unconfined))
        assertEquals(AnagramsArcadePhase.IDLE, machine.phase.value)
    }

    @Test
    fun roundStarted_transitionsToPlaying() {
        val machine = AnagramsArcadeStateMachine(CoroutineScope(Dispatchers.Unconfined))
        machine.roundStarted()
        assertEquals(AnagramsArcadePhase.PLAYING, machine.phase.value)
    }

    @Test
    fun timeExpired_transitionsToRoundOver() {
        val machine = AnagramsArcadeStateMachine(CoroutineScope(Dispatchers.Unconfined))
        machine.roundStarted()
        machine.timeExpired()
        assertEquals(AnagramsArcadePhase.ROUND_OVER, machine.phase.value)
    }

    @Test
    fun allWordsFound_transitionsToRoundOver() {
        val machine = AnagramsArcadeStateMachine(CoroutineScope(Dispatchers.Unconfined))
        machine.roundStarted()
        machine.allWordsFound()
        assertEquals(AnagramsArcadePhase.ROUND_OVER, machine.phase.value)
    }

    @Test
    fun gaveUp_transitionsToRoundOver() {
        val machine = AnagramsArcadeStateMachine(CoroutineScope(Dispatchers.Unconfined))
        machine.roundStarted()
        machine.gaveUp()
        assertEquals(AnagramsArcadePhase.ROUND_OVER, machine.phase.value)
    }

    @Test
    fun newRound_transitionsToIdle() {
        val machine = AnagramsArcadeStateMachine(CoroutineScope(Dispatchers.Unconfined))
        machine.roundStarted()
        machine.timeExpired()
        machine.newRound()
        assertEquals(AnagramsArcadePhase.IDLE, machine.phase.value)
    }
}
