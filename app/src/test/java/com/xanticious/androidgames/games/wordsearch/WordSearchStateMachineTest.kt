package com.xanticious.androidgames.games.wordsearch

import com.xanticious.androidgames.model.games.wordsearch.GridPosition
import com.xanticious.androidgames.model.games.wordsearch.SelectionState
import com.xanticious.androidgames.model.games.wordsearch.WordSearchGrid
import com.xanticious.androidgames.model.games.wordsearch.WordSearchState
import com.xanticious.androidgames.state.games.wordsearch.WordSearchPhase
import com.xanticious.androidgames.state.games.wordsearch.WordSearchStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WordSearchStateMachineTest {
    private fun machine() = WordSearchStateMachine(CoroutineScope(Dispatchers.Unconfined))

    private fun testState(): WordSearchState = WordSearchState(
        grid = WordSearchGrid.empty(10),
        targetWords = listOf("cat", "dog", "bird"),
        foundWords = emptySet(),
        currentSelection = SelectionState(null, null),
        timeRemainingSeconds = 180
    )

    @Test
    fun initialPhase_isSetup() {
        val m = machine()
        assertEquals(WordSearchPhase.SETUP, m.phase.value)
    }

    @Test
    fun showHowToPlay_movesToHowToPlay() {
        val m = machine()
        m.showHowToPlay()
        assertEquals(WordSearchPhase.HOW_TO_PLAY, m.phase.value)
    }

    @Test
    fun backToSetup_returnsToSetup() {
        val m = machine()
        m.showHowToPlay()
        m.backToSetup()
        assertEquals(WordSearchPhase.SETUP, m.phase.value)
    }

    @Test
    fun startGame_movesToPlaying() {
        val m = machine()
        val state = testState()
        m.startGame(state)
        assertEquals(WordSearchPhase.PLAYING, m.phase.value)
        assertEquals(state, m.gameState.value)
    }

    @Test
    fun allWordsFound_movesToSolved() {
        val m = machine()
        m.startGame(testState())
        m.allWordsFound()
        assertEquals(WordSearchPhase.SOLVED, m.phase.value)
    }

    @Test
    fun timeExpired_movesToTimeUp() {
        val m = machine()
        m.startGame(testState())
        m.timeExpired()
        assertEquals(WordSearchPhase.TIME_UP, m.phase.value)
    }

    @Test
    fun newGameFromSolved_returnsToSetup() {
        val m = machine()
        m.startGame(testState())
        m.allWordsFound()
        m.newGame()
        assertEquals(WordSearchPhase.SETUP, m.phase.value)
        assertNull(m.gameState.value)
    }

    @Test
    fun newGameFromTimeUp_returnsToSetup() {
        val m = machine()
        m.startGame(testState())
        m.timeExpired()
        m.newGame()
        assertEquals(WordSearchPhase.SETUP, m.phase.value)
        assertNull(m.gameState.value)
    }

    @Test
    fun updateGameState_updatesStateFlow() {
        val m = machine()
        val state1 = testState()
        m.startGame(state1)
        
        val state2 = state1.copy(foundWords = setOf("cat"))
        m.updateGameState(state2)
        
        assertEquals(state2, m.gameState.value)
        assertEquals(1, m.gameState.value?.foundWords?.size)
    }
}
