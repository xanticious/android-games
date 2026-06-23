package com.xanticious.androidgames.games.wordchain

import com.xanticious.androidgames.controller.games.wordchain.WordChainController
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.games.wordchain.LinkRule
import com.xanticious.androidgames.model.games.wordchain.WordChainValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordChainControllerTest {
    private val controller = WordChainController()
    private val testWords = listOf("apple", "eagle", "echo", "orbit", "tiger", "rapids", "sat", "tea", "ant")
    private val wordData = WordData(testWords)

    @Test
    fun validateWord_validFirstWord_returnsValid() {
        val result = controller.validateWord("apple", null, LinkRule.LAST_LETTER, 3, emptySet(), wordData)
        assertTrue(result is WordChainValidationResult.Valid)
    }

    @Test
    fun validateWord_validNextWord_lastLetter_returnsValid() {
        val result = controller.validateWord("eagle", "apple", LinkRule.LAST_LETTER, 3, emptySet(), wordData)
        assertTrue(result is WordChainValidationResult.Valid)
    }

    @Test
    fun validateWord_invalidStartLetter_lastLetter_returnsInvalid() {
        val result = controller.validateWord("tiger", "apple", LinkRule.LAST_LETTER, 3, emptySet(), wordData)
        assertTrue(result is WordChainValidationResult.Invalid)
    }

    @Test
    fun validateWord_validNextWord_lastTwoLetters_returnsValid() {
        val result = controller.validateWord("eagle", "apple", LinkRule.LAST_TWO_LETTERS, 3, emptySet(), wordData)
        assertTrue(result is WordChainValidationResult.Invalid)
    }

    @Test
    fun validateWord_tooShort_returnsInvalid() {
        val result = controller.validateWord("at", "sat", LinkRule.LAST_LETTER, 3, emptySet(), wordData)
        assertTrue(result is WordChainValidationResult.Invalid)
    }

    @Test
    fun validateWord_notValidWord_returnsInvalid() {
        val result = controller.validateWord("xyz", "apple", LinkRule.LAST_LETTER, 3, emptySet(), wordData)
        assertTrue(result is WordChainValidationResult.Invalid)
    }

    @Test
    fun validateWord_alreadyUsed_returnsInvalid() {
        val result = controller.validateWord("eagle", "apple", LinkRule.LAST_LETTER, 3, setOf("eagle"), wordData)
        assertTrue(result is WordChainValidationResult.Invalid)
    }

    @Test
    fun getRequiredStart_lastLetter_returnsLastLetter() {
        val required = controller.getRequiredStart("apple", LinkRule.LAST_LETTER)
        assertEquals("E", required)
    }

    @Test
    fun getRequiredStart_lastTwoLetters_returnsLastTwo() {
        val required = controller.getRequiredStart("apple", LinkRule.LAST_TWO_LETTERS)
        assertEquals("LE", required)
    }

    @Test
    fun getRequiredStart_noWord_returnsEmpty() {
        val required = controller.getRequiredStart(null, LinkRule.LAST_LETTER)
        assertEquals("", required)
    }

    @Test
    fun calculateScore_returnsCorrectScore() {
        val score = controller.calculateScore(5, 25)
        assertEquals(75, score)
    }
}
