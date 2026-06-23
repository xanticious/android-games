package com.xanticious.androidgames.games.loveletter

import com.xanticious.androidgames.state.games.loveletter.LoveLetterPhase
import com.xanticious.androidgames.state.games.loveletter.LoveLetterStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class LoveLetterStateMachineTest {

    private fun machine() = LoveLetterStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initialPhase_isIdle() {
        val m = machine()
        assertEquals(LoveLetterPhase.IDLE, m.phase.value)
    }

    @Test
    fun startGame_movesToSetup() {
        val m = machine()
        m.startGame()
        assertEquals(LoveLetterPhase.SETUP, m.phase.value)
    }

    @Test
    fun roundSetup_movesToPlaying() {
        val m = machine()
        m.startGame()
        m.roundSetup()
        assertEquals(LoveLetterPhase.PLAYING, m.phase.value)
    }

    @Test
    fun roundOver_fromPlaying_movesToRoundOver() {
        val m = machine()
        m.startGame()
        m.roundSetup()
        m.roundOver()
        assertEquals(LoveLetterPhase.ROUND_OVER, m.phase.value)
    }

    @Test
    fun nextRound_fromRoundOver_movesToPlaying() {
        val m = machine()
        m.startGame()
        m.roundSetup()
        m.roundOver()
        m.nextRound()
        assertEquals(LoveLetterPhase.PLAYING, m.phase.value)
    }

    @Test
    fun gameWon_fromRoundOver_movesToGameOver() {
        val m = machine()
        m.startGame()
        m.roundSetup()
        m.roundOver()
        m.gameWon()
        assertEquals(LoveLetterPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun rematch_fromGameOver_movesToSetup() {
        val m = machine()
        m.startGame()
        m.roundSetup()
        m.roundOver()
        m.gameWon()
        m.rematch()
        assertEquals(LoveLetterPhase.SETUP, m.phase.value)
    }
}
