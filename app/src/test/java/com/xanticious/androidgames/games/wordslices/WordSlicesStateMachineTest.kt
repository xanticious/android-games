package com.xanticious.androidgames.games.wordslices

import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.state.games.wordslices.WordSlicesPhase
import com.xanticious.androidgames.state.games.wordslices.WordSlicesStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WordSlicesStateMachineTest {
    private val testWords = listOf("cat", "dog", "crane", "story", "elephant")
    private val wordData = WordData(testWords)
    
    private fun machine() = WordSlicesStateMachine(
        wordData = wordData,
        scope = CoroutineScope(Dispatchers.Unconfined)
    )

    @Test
    fun initialPhase_isSetup() {
        val m = machine()
        assertEquals(WordSlicesPhase.SETUP, m.phase.value)
    }

    @Test
    fun showHowToPlay_movesToHowToPlay() {
        val m = machine()
        m.showHowToPlay()
        assertEquals(WordSlicesPhase.HOW_TO_PLAY, m.phase.value)
    }

    @Test
    fun startPlaying_fromSetup_movesToPlaying() {
        val m = machine()
        m.startPlaying()
        assertEquals(WordSlicesPhase.PLAYING, m.phase.value)
        assertNotNull(m.roundState.value)
    }

    @Test
    fun startPlaying_fromHowToPlay_movesToPlaying() {
        val m = machine()
        m.showHowToPlay()
        m.startPlaying()
        assertEquals(WordSlicesPhase.PLAYING, m.phase.value)
    }

    @Test
    fun backToSetup_fromHowToPlay_returnsToSetup() {
        val m = machine()
        m.showHowToPlay()
        m.backToSetup()
        assertEquals(WordSlicesPhase.SETUP, m.phase.value)
    }

    @Test
    fun guessCorrectLetters_revealWord_movesToWon() {
        val m = machine()
        m.startPlaying()
        val word = m.roundState.value?.word ?: ""
        val uniqueLetters = word.toSet()
        
        uniqueLetters.forEach { letter ->
            m.guessLetter(letter)
        }
        
        assertEquals(WordSlicesPhase.WON, m.phase.value)
    }

    @Test
    fun guessAllWrong_movesToLost() {
        val m = machine()
        m.startPlaying()
        val word = m.roundState.value?.word ?: ""
        
        val wrongLetters = "zqxjkvwbfgyphlmutdinsorace".toList()
        val lettersToGuess = wrongLetters.filter { it !in word.lowercase() }
        
        repeat(12) { i ->
            if (i < lettersToGuess.size) {
                m.guessLetter(lettersToGuess[i])
            }
        }
        
        assertEquals(WordSlicesPhase.LOST, m.phase.value)
    }

    @Test
    fun newWord_fromWon_startsNewRound() {
        val m = machine()
        m.startPlaying()
        val word = m.roundState.value?.word ?: ""
        word.toSet().forEach { m.guessLetter(it) }
        
        m.newWord()
        assertEquals(WordSlicesPhase.PLAYING, m.phase.value)
        assertNotNull(m.roundState.value)
    }

    @Test
    fun newWord_fromLost_startsNewRound() {
        val m = machine()
        m.startPlaying()
        val word = m.roundState.value?.word ?: ""
        val wrongLetters = "zqxjkvwbfgyphlmutdinsorace".toList()
        val lettersToGuess = wrongLetters.filter { it !in word.lowercase() }
        
        repeat(12) { i ->
            if (i < lettersToGuess.size) {
                m.guessLetter(lettersToGuess[i])
            }
        }
        
        m.newWord()
        assertEquals(WordSlicesPhase.PLAYING, m.phase.value)
    }

    @Test
    fun roundState_startsWithTwelveSlices() {
        val m = machine()
        m.startPlaying()
        val state = m.roundState.value
        assertNotNull(state)
        assertEquals(12, state?.slicesRemaining)
    }

    @Test
    fun wrongGuess_reducesSlices() {
        val m = machine()
        m.startPlaying()
        val word = m.roundState.value?.word ?: ""
        
        val wrongLetter = "zqxjkvwbfgyphlmutdinsorace".first { it !in word.lowercase() }
        m.guessLetter(wrongLetter)
        
        val state = m.roundState.value
        assertEquals(11, state?.slicesRemaining)
    }
}
