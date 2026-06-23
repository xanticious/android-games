package com.xanticious.androidgames.games.anagrams

import com.xanticious.androidgames.state.games.anagrams.AnagramsPhase
import com.xanticious.androidgames.state.games.anagrams.AnagramsStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class AnagramsStateMachineTest {

    @Test
    fun initialPhase_isIdle() {
        val machine = AnagramsStateMachine(CoroutineScope(Dispatchers.Unconfined))
        assertEquals(AnagramsPhase.IDLE, machine.phase.value)
    }

    @Test
    fun roundStarted_transitionsToPlaying() {
        val machine = AnagramsStateMachine(CoroutineScope(Dispatchers.Unconfined))
        machine.roundStarted()
        assertEquals(AnagramsPhase.PLAYING, machine.phase.value)
    }

    @Test
    fun allWordsFound_transitionsToRoundOver() {
        val machine = AnagramsStateMachine(CoroutineScope(Dispatchers.Unconfined))
        machine.roundStarted()
        machine.allWordsFound()
        assertEquals(AnagramsPhase.ROUND_OVER, machine.phase.value)
    }

    @Test
    fun gaveUp_transitionsToRoundOver() {
        val machine = AnagramsStateMachine(CoroutineScope(Dispatchers.Unconfined))
        machine.roundStarted()
        machine.gaveUp()
        assertEquals(AnagramsPhase.ROUND_OVER, machine.phase.value)
    }

    @Test
    fun newRound_transitionsToIdle() {
        val machine = AnagramsStateMachine(CoroutineScope(Dispatchers.Unconfined))
        machine.roundStarted()
        machine.gaveUp()
        machine.newRound()
        assertEquals(AnagramsPhase.IDLE, machine.phase.value)
    }
}
