package com.xanticious.androidgames.games.wordle

import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.state.games.wordle.WordlePhase
import com.xanticious.androidgames.state.games.wordle.WordleStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WordleStateMachineTest {
    private val testWords = listOf("crane", "story", "slate", "plumb", "dodge", "whale", "charm", "frost")
    private val wordData = WordData(testWords)
    
    private fun machine() = WordleStateMachine(
        wordData = wordData,
        scope = CoroutineScope(Dispatchers.Unconfined)
    )

    @Test
    fun initialPhase_isSetup() {
        val m = machine()
        assertEquals(WordlePhase.SETUP, m.phase.value)
    }

    @Test
    fun showHowToPlay_movesToHowToPlay() {
        val m = machine()
        m.showHowToPlay()
        assertEquals(WordlePhase.HOW_TO_PLAY, m.phase.value)
    }

    @Test
    fun startPlaying_fromSetup_movesToPlaying() {
        val m = machine()
        m.startPlaying()
        assertEquals(WordlePhase.PLAYING, m.phase.value)
        assertNotNull(m.roundState.value)
    }

    @Test
    fun startPlaying_fromHowToPlay_movesToPlaying() {
        val m = machine()
        m.showHowToPlay()
        m.startPlaying()
        assertEquals(WordlePhase.PLAYING, m.phase.value)
    }

    @Test
    fun backToSetup_fromHowToPlay_returnsToSetup() {
        val m = machine()
        m.showHowToPlay()
        m.backToSetup()
        assertEquals(WordlePhase.SETUP, m.phase.value)
    }

    @Test
    fun submitCorrectGuess_movesToWon() {
        val m = machine()
        m.startPlaying()
        val target = m.roundState.value?.targetWord ?: ""
        m.submitGuess(target)
        assertEquals(WordlePhase.WON, m.phase.value)
    }

    @Test
    fun submitMaxIncorrectGuesses_movesToLost() {
        val m = machine()
        // Disable consistency checking so all valid words count
        m.updateSettings(m.settings.value.copy(enforceConsistency = false))
        m.startPlaying()
        val target = m.roundState.value?.targetWord ?: ""
        val otherWords = testWords.filter { it != target }
        
        // Submit 6 wrong guesses
        var submitted = 0
        for (word in otherWords) {
            if (submitted < 6) {
                m.submitGuess(word)
                submitted++
            }
        }
        
        assertEquals(WordlePhase.LOST, m.phase.value)
    }

    @Test
    fun nextRound_fromWon_startsNewRound() {
        val m = machine()
        m.startPlaying()
        val target = m.roundState.value?.targetWord ?: ""
        m.submitGuess(target)
        m.nextRound()
        assertEquals(WordlePhase.PLAYING, m.phase.value)
        assertNotNull(m.roundState.value)
    }

    @Test
    fun nextRound_fromLost_startsNewRound() {
        val m = machine()
        m.updateSettings(m.settings.value.copy(enforceConsistency = false))
        m.startPlaying()
        val target = m.roundState.value?.targetWord ?: ""
        val otherWords = testWords.filter { it != target }
        
        var submitted = 0
        for (word in otherWords) {
            if (submitted < 6) {
                m.submitGuess(word)
                submitted++
            }
        }
        
        m.nextRound()
        assertEquals(WordlePhase.PLAYING, m.phase.value)
    }

    @Test
    fun roundState_hasFirstGuessPreFilled() {
        val m = machine()
        m.startPlaying()
        val state = m.roundState.value
        assertNotNull(state)
        assertEquals(5, state?.currentInput?.length)
    }
}
