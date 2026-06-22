package com.xanticious.androidgames.games.letterdrop

import com.xanticious.androidgames.state.games.letterdrop.LetterDropPhase
import com.xanticious.androidgames.state.games.letterdrop.LetterDropStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class LetterDropStateMachineTest {
    private fun machine() = LetterDropStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun showHowToPlay_movesToHowToPlay() {
        val m = machine()
        m.showHowToPlay()
        assertEquals(LetterDropPhase.HOW_TO_PLAY, m.phase.value)
    }

    @Test
    fun startPlaying_movesToPlaying() {
        val m = machine()
        m.startPlaying()
        assertEquals(LetterDropPhase.PLAYING, m.phase.value)
    }

    @Test
    fun overflow_movesToGameOver() {
        val m = machine()
        m.startPlaying()
        m.overflow()
        assertEquals(LetterDropPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun restart_returnsToPlaying() {
        val m = machine()
        m.startPlaying()
        m.overflow()
        m.restart()
        assertEquals(LetterDropPhase.PLAYING, m.phase.value)
    }
}
