package com.xanticious.androidgames.games.wordslices

import com.xanticious.androidgames.controller.games.wordslices.WordSlicesController
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.GameDifficulty
import org.junit.Assert.*
import org.junit.Test

class WordSlicesControllerTest {
    private val controller = WordSlicesController()
    private val testWords = listOf("cat", "dog", "crane", "story", "elephant", "accomplish")
    private val wordData = WordData(testWords)

    @Test
    fun selectWord_easyDifficulty_selectsShorterWord() {
        val word = controller.selectWord(wordData, GameDifficulty.EASY)
        assertNotNull(word)
        assertTrue(word.length in 4..6)
    }

    @Test
    fun selectWord_mediumDifficulty_selectsMediumWord() {
        val word = controller.selectWord(wordData, GameDifficulty.MEDIUM)
        assertNotNull(word)
        assertTrue(word.length in 6..9)
    }

    @Test
    fun selectWord_hardDifficulty_selectsLongerWord() {
        val word = controller.selectWord(wordData, GameDifficulty.HARD)
        assertNotNull(word)
        assertTrue(word.length in 9..12)
    }

    @Test
    fun isLetterInWord_presentLetter_returnsTrue() {
        assertTrue(controller.isLetterInWord('c', "crane"))
        assertTrue(controller.isLetterInWord('C', "crane"))
    }

    @Test
    fun isLetterInWord_absentLetter_returnsFalse() {
        assertFalse(controller.isLetterInWord('z', "crane"))
    }

    @Test
    fun revealedPositions_noGuesses_returnsEmpty() {
        val positions = controller.revealedPositions("crane", emptySet())
        assertTrue(positions.isEmpty())
    }

    @Test
    fun revealedPositions_someGuesses_returnsCorrectPositions() {
        val positions = controller.revealedPositions("crane", setOf('c', 'a'))
        assertEquals(setOf(0, 2), positions)
    }

    @Test
    fun revealedPositions_allGuesses_returnsAllPositions() {
        val positions = controller.revealedPositions("cat", setOf('c', 'a', 't'))
        assertEquals(setOf(0, 1, 2), positions)
    }

    @Test
    fun isWordFullyRevealed_notAllLetters_returnsFalse() {
        assertFalse(controller.isWordFullyRevealed("crane", setOf('c', 'r')))
    }

    @Test
    fun isWordFullyRevealed_allLetters_returnsTrue() {
        assertTrue(controller.isWordFullyRevealed("crane", setOf('c', 'r', 'a', 'n', 'e')))
    }

    @Test
    fun computeSlicesRemaining_noWrongGuesses_returnsFullSlices() {
        assertEquals(12, controller.computeSlicesRemaining(0, 12))
    }

    @Test
    fun computeSlicesRemaining_someWrongGuesses_reducesSlices() {
        assertEquals(9, controller.computeSlicesRemaining(3, 12))
    }

    @Test
    fun computeSlicesRemaining_allWrongGuesses_returnsZero() {
        assertEquals(0, controller.computeSlicesRemaining(12, 12))
    }

    @Test
    fun computeSlicesRemaining_moreWrongThanTotal_returnsZero() {
        assertEquals(0, controller.computeSlicesRemaining(15, 12))
    }

    @Test
    fun isGameWon_wordRevealed_slicesRemaining_returnsTrue() {
        assertTrue(controller.isGameWon("cat", setOf('c', 'a', 't'), 5))
    }

    @Test
    fun isGameWon_wordRevealed_noSlices_returnsFalse() {
        assertFalse(controller.isGameWon("cat", setOf('c', 'a', 't'), 0))
    }

    @Test
    fun isGameWon_wordNotRevealed_slicesRemaining_returnsFalse() {
        assertFalse(controller.isGameWon("cat", setOf('c', 'a'), 5))
    }

    @Test
    fun isGameLost_noSlicesRemaining_returnsTrue() {
        assertTrue(controller.isGameLost(0))
    }

    @Test
    fun isGameLost_slicesRemaining_returnsFalse() {
        assertFalse(controller.isGameLost(5))
    }

    @Test
    fun processGuess_correctLetter_addsToGuessed() {
        val result = controller.processGuess('c', "crane", emptySet(), emptySet(), 12)
        assertFalse(result.alreadyGuessed)
        assertTrue(result.correct)
        assertTrue('c' in result.newGuessedLetters)
        assertEquals(12, result.newSlicesRemaining)
    }

    @Test
    fun processGuess_wrongLetter_removesSlice() {
        val result = controller.processGuess('z', "crane", emptySet(), emptySet(), 12)
        assertFalse(result.alreadyGuessed)
        assertFalse(result.correct)
        assertTrue('z' in result.newWrongGuesses)
        assertEquals(11, result.newSlicesRemaining)
    }

    @Test
    fun processGuess_alreadyGuessed_noChange() {
        val result = controller.processGuess('c', "crane", setOf('c'), emptySet(), 12)
        assertTrue(result.alreadyGuessed)
        assertEquals(12, result.newSlicesRemaining)
    }

    @Test
    fun processGuess_caseInsensitive_worksCorrectly() {
        val result = controller.processGuess('C', "crane", emptySet(), emptySet(), 12)
        assertTrue(result.correct)
        assertTrue('c' in result.newGuessedLetters)
    }

    @Test
    fun processGuess_multipleWrong_accumulatesSliceLoss() {
        var state = controller.processGuess('z', "crane", emptySet(), emptySet(), 12)
        assertEquals(11, state.newSlicesRemaining)
        
        state = controller.processGuess('q', "crane", state.newGuessedLetters, state.newWrongGuesses, state.newSlicesRemaining)
        assertEquals(10, state.newSlicesRemaining)
        
        state = controller.processGuess('x', "crane", state.newGuessedLetters, state.newWrongGuesses, state.newSlicesRemaining)
        assertEquals(9, state.newSlicesRemaining)
    }

    @Test
    fun processGuess_reachesZeroSlices_staysAtZero() {
        var slices = 1
        val result = controller.processGuess('z', "crane", emptySet(), emptySet(), slices)
        assertEquals(0, result.newSlicesRemaining)
    }
}
