package com.xanticious.androidgames.games.typingsprint

import com.xanticious.androidgames.state.games.typingsprint.TypingSprintPhase
import com.xanticious.androidgames.state.games.typingsprint.TypingSprintStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class TypingSprintStateMachineTest {
    private fun machine() = TypingSprintStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun showHowToPlay_movesToHowToPlay() {
        val m = machine()
        m.showHowToPlay()
        assertEquals(TypingSprintPhase.HOW_TO_PLAY, m.phase.value)
    }

    @Test
    fun startPlaying_movesToPlaying() {
        val m = machine()
        m.startPlaying()
        assertEquals(TypingSprintPhase.PLAYING, m.phase.value)
    }

    @Test
    fun gameEnded_movesToGameOver() {
        val m = machine()
        m.startPlaying()
        m.gameEnded()
        assertEquals(TypingSprintPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun restart_returnsToPlaying() {
        val m = machine()
        m.startPlaying()
        m.gameEnded()
        m.restart()
        assertEquals(TypingSprintPhase.PLAYING, m.phase.value)
    }
}
