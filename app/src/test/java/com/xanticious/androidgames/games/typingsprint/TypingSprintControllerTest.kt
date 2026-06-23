package com.xanticious.androidgames.games.typingsprint

import com.xanticious.androidgames.controller.games.typingsprint.TypingSprintController
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.typingsprint.FallingWord
import com.xanticious.androidgames.model.games.typingsprint.TypingSprintEvent
import com.xanticious.androidgames.model.games.typingsprint.TypingSprintState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TypingSprintControllerTest {
    private val controller = TypingSprintController()
    private val wordData = WordData(listOf("cat", "dog", "rat", "cart", "dogs", "rats", "carts"))

    @Test
    fun step_advancesWordsByDt() {
        val config = controller.configFor(GameDifficulty.MEDIUM, 3, 5)
        val state = TypingSprintState.initial().copy(
            words = listOf(FallingWord("cat", 0.5f, 0.2f, 0))
        )
        val result = controller.step(state, config, dt = 0.1f, wordData, Random.Default)
        assertTrue(result.state.words[0].y > 0.2f)
    }

    @Test
    fun step_spawnsNewWordAfterInterval() {
        val config = controller.configFor(GameDifficulty.MEDIUM, 3, 5)
        val state = TypingSprintState.initial().copy(spawnTimer = 2.6f)
        val result = controller.step(state, config, dt = 0.1f, wordData, Random.Default)
        assertTrue(result.state.words.isNotEmpty())
    }

    @Test
    fun step_detectsMissedWord() {
        val config = controller.configFor(GameDifficulty.MEDIUM, 3, 5)
        val state = TypingSprintState.initial().copy(
            words = listOf(FallingWord("cat", 0.5f, 0.89f, 0))
        )
        val result = controller.step(state, config, dt = 0.2f, wordData, Random.Default)
        assertEquals(TypingSprintEvent.WORD_MISSED, result.event)
        assertEquals(1, result.state.missedWords)
    }

    @Test
    fun step_endsGameAfterTooManyMisses() {
        val config = controller.configFor(GameDifficulty.MEDIUM, 3, 5)
        val state = TypingSprintState.initial().copy(missedWords = 7)
        val result = controller.step(state, config, dt = 0.1f, wordData, Random.Default)
        assertEquals(TypingSprintEvent.GAME_OVER, result.event)
        assertTrue(result.state.gameOver)
    }

    @Test
    fun typeChar_addsCharToInput() {
        val state = TypingSprintState.initial()
        val result = controller.typeChar(state, 'c')
        assertEquals("c", result.currentInput)
        assertEquals(1, result.totalChars)
    }

    @Test
    fun backspace_removesLastChar() {
        val state = TypingSprintState.initial().copy(currentInput = "ca")
        val result = controller.backspace(state)
        assertEquals("c", result.currentInput)
    }

    @Test
    fun checkMatch_clearsMatchingWord() {
        val state = TypingSprintState.initial().copy(
            words = listOf(FallingWord("cat", 0.5f, 0.5f, 0)),
            currentInput = "cat"
        )
        val result = controller.checkMatch(state)
        assertEquals(TypingSprintEvent.WORD_CLEARED, result.event)
        assertTrue(result.state.words.isEmpty())
        assertEquals("", result.state.currentInput)
        assertEquals(3, result.state.correctChars)
        assertEquals(1, result.state.clearedWords)
    }

    @Test
    fun checkMatch_noMatchReturnsNone() {
        val state = TypingSprintState.initial().copy(
            words = listOf(FallingWord("cat", 0.5f, 0.5f, 0)),
            currentInput = "do"
        )
        val result = controller.checkMatch(state)
        assertEquals(TypingSprintEvent.NONE, result.event)
    }

    @Test
    fun computeWpm_calculatesCorrectly() {
        val state = TypingSprintState.initial().copy(
            correctChars = 25,
            elapsedSeconds = 60f
        )
        val wpm = controller.computeWpm(state)
        assertEquals(5f, wpm, 0.1f)
    }

    @Test
    fun computeAccuracy_calculatesCorrectly() {
        val state = TypingSprintState.initial().copy(
            correctChars = 80,
            totalChars = 100
        )
        val accuracy = controller.computeAccuracy(state)
        assertEquals(80f, accuracy, 0.1f)
    }
}
