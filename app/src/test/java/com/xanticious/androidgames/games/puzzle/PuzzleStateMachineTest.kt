package com.xanticious.androidgames.games.puzzle

import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class PuzzleStateMachineTest {
    private fun machine() = PuzzleStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun start_isSettings() {
        assertEquals(PuzzlePhase.SETTINGS, machine().phase.value)
    }

    @Test
    fun startGame_movesToPlaying() {
        val m = machine()
        m.startGame()
        assertEquals(PuzzlePhase.PLAYING, m.phase.value)
    }

    @Test
    fun howToPlay_thenBack_returnsToSettings() {
        val m = machine()
        m.openHowToPlay()
        assertEquals(PuzzlePhase.HOW_TO_PLAY, m.phase.value)
        m.backToSettings()
        assertEquals(PuzzlePhase.SETTINGS, m.phase.value)
    }

    @Test
    fun playing_thenSolved_reachesSolved() {
        val m = machine()
        m.startGame()
        m.solved()
        assertEquals(PuzzlePhase.SOLVED, m.phase.value)
    }

    @Test
    fun playing_thenFailed_reachesFailed() {
        val m = machine()
        m.startGame()
        m.failed()
        assertEquals(PuzzlePhase.FAILED, m.phase.value)
    }

    @Test
    fun solved_thenRetry_returnsToPlaying() {
        val m = machine()
        m.startGame()
        m.solved()
        m.retry()
        assertEquals(PuzzlePhase.PLAYING, m.phase.value)
    }

    @Test
    fun solved_thenNewGame_returnsToSettings() {
        val m = machine()
        m.startGame()
        m.solved()
        m.newGame()
        assertEquals(PuzzlePhase.SETTINGS, m.phase.value)
    }
}
