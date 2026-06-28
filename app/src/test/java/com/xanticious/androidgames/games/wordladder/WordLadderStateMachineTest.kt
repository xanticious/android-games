package com.xanticious.androidgames.games.wordladder

import com.xanticious.androidgames.state.games.wordladder.WordLadderPhase
import com.xanticious.androidgames.state.games.wordladder.WordLadderStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class WordLadderStateMachineTest {
    private fun machine() = WordLadderStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initial_isSetup() {
        val m = machine()
        assertEquals(WordLadderPhase.SETUP, m.phase.value)
    }

    @Test
    fun showHowToPlay_movesToHowToPlay() {
        val m = machine()
        m.showHowToPlay()
        assertEquals(WordLadderPhase.HOW_TO_PLAY, m.phase.value)
    }

    @Test
    fun backToSetup_returnsToSetup() {
        val m = machine()
        m.showHowToPlay()
        m.backToSetup()
        assertEquals(WordLadderPhase.SETUP, m.phase.value)
    }

    @Test
    fun startGame_movesToPlaying() {
        val m = machine()
        m.startGame()
        assertEquals(WordLadderPhase.PLAYING, m.phase.value)
    }

    @Test
    fun targetReached_movesToSolved() {
        val m = machine()
        m.startGame()
        m.targetReached()
        assertEquals(WordLadderPhase.SOLVED, m.phase.value)
    }

    @Test
    fun newPuzzle_returnsToPlaying() {
        val m = machine()
        m.startGame()
        m.targetReached()
        m.newPuzzle()
        assertEquals(WordLadderPhase.PLAYING, m.phase.value)
    }

    @Test
    fun exit_returnsToSetup() {
        val m = machine()
        m.startGame()
        m.exit()
        assertEquals(WordLadderPhase.SETUP, m.phase.value)
    }
}
