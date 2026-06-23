package com.xanticious.androidgames.games.wordchain

import com.xanticious.androidgames.state.games.wordchain.WordChainPhase
import com.xanticious.androidgames.state.games.wordchain.WordChainStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class WordChainStateMachineTest {
    private fun machine() = WordChainStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initial_isSetup() {
        val m = machine()
        assertEquals(WordChainPhase.SETUP, m.phase.value)
    }

    @Test
    fun showHowToPlay_movesToHowToPlay() {
        val m = machine()
        m.showHowToPlay()
        assertEquals(WordChainPhase.HOW_TO_PLAY, m.phase.value)
    }

    @Test
    fun backToSetup_returnsToSetup() {
        val m = machine()
        m.showHowToPlay()
        m.backToSetup()
        assertEquals(WordChainPhase.SETUP, m.phase.value)
    }

    @Test
    fun startGame_movesToPlaying() {
        val m = machine()
        m.startGame()
        assertEquals(WordChainPhase.PLAYING, m.phase.value)
    }

    @Test
    fun timeExpired_movesToChainBroken() {
        val m = machine()
        m.startGame()
        m.timeExpired()
        assertEquals(WordChainPhase.CHAIN_BROKEN, m.phase.value)
    }

    @Test
    fun pass_movesToChainBroken() {
        val m = machine()
        m.startGame()
        m.pass()
        assertEquals(WordChainPhase.CHAIN_BROKEN, m.phase.value)
    }

    @Test
    fun newGame_returnsToPlaying() {
        val m = machine()
        m.startGame()
        m.timeExpired()
        m.newGame()
        assertEquals(WordChainPhase.PLAYING, m.phase.value)
    }

    @Test
    fun exit_returnsToSetup() {
        val m = machine()
        m.startGame()
        m.exit()
        assertEquals(WordChainPhase.SETUP, m.phase.value)
    }
}
