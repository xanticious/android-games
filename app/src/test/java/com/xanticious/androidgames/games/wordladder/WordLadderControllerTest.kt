package com.xanticious.androidgames.games.wordladder

import com.xanticious.androidgames.controller.games.wordladder.WordLadderController
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.wordladder.WordLadderValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordLadderControllerTest {
    private val controller = WordLadderController()
    private val testWords = listOf("cat", "cot", "cog", "dog", "bat", "hot", "hat", "cold", "cord", "word", "ward", "warm")
    private val wordData = WordData(testWords)

    @Test
    fun differsByOneLetter_sameLength_oneDifference_returnsTrue() {
        assertTrue(controller.differsByOneLetter("cat", "cot"))
    }

    @Test
    fun differsByOneLetter_sameLength_twoDifferences_returnsFalse() {
        assertFalse(controller.differsByOneLetter("cat", "dog"))
    }

    @Test
    fun differsByOneLetter_differentLength_returnsFalse() {
        assertFalse(controller.differsByOneLetter("cat", "cats"))
    }

    @Test
    fun differsByOneLetter_identical_returnsFalse() {
        assertFalse(controller.differsByOneLetter("cat", "cat"))
    }

    @Test
    fun validateStep_validMove_returnsValid() {
        val result = controller.validateStep("cat", "cot", emptySet(), wordData)
        assertTrue(result is WordLadderValidationResult.Valid)
    }

    @Test
    fun validateStep_notOneLetter_returnsInvalid() {
        val result = controller.validateStep("cat", "dog", emptySet(), wordData)
        assertTrue(result is WordLadderValidationResult.Invalid)
    }

    @Test
    fun validateStep_notValidWord_returnsInvalid() {
        val result = controller.validateStep("cat", "cxt", emptySet(), wordData)
        assertTrue(result is WordLadderValidationResult.Invalid)
    }

    @Test
    fun validateStep_alreadyUsed_returnsInvalid() {
        val result = controller.validateStep("cat", "cot", setOf("cot"), wordData)
        assertTrue(result is WordLadderValidationResult.Invalid)
    }

    @Test
    fun findShortestPath_connectedWords_returnsPath() {
        val path = controller.findShortestPath("cat", "cog", wordData)
        assertNotNull(path)
        assertEquals("cat", path?.first())
        assertEquals("cog", path?.last())
        assertTrue((path?.size ?: 0) >= 2)
    }

    @Test
    fun findShortestPath_sameWord_returnsSingleWord() {
        val path = controller.findShortestPath("cat", "cat", wordData)
        assertNotNull(path)
        assertEquals(1, path?.size)
    }

    @Test
    fun findShortestPath_differentLengths_returnsNull() {
        val path = controller.findShortestPath("cat", "dogs", wordData)
        assertNull(path)
    }

    @Test
    fun findShortestPath_coldToWarm_returnsPath() {
        val path = controller.findShortestPath("cold", "warm", wordData)
        assertNotNull(path)
        assertEquals("cold", path?.first())
        assertEquals("warm", path?.last())
    }

    @Test
    fun generatePuzzle_validLength_returnsPuzzle() {
        val puzzle = controller.generatePuzzle(3, GameDifficulty.EASY, wordData)
        assertNotNull(puzzle)
        assertEquals(3, puzzle?.startWord?.length)
        assertEquals(3, puzzle?.targetWord?.length)
        assertTrue((puzzle?.shortestPathLength ?: 0) >= 2)
    }
}
