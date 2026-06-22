package com.xanticious.androidgames.controller.games.typingsprint

import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.typingsprint.FallingWord
import com.xanticious.androidgames.model.games.typingsprint.TypingSprintConfig
import com.xanticious.androidgames.model.games.typingsprint.TypingSprintEvent
import com.xanticious.androidgames.model.games.typingsprint.TypingSprintState
import com.xanticious.androidgames.model.games.typingsprint.TypingSprintStep
import kotlin.random.Random

class TypingSprintController {

    private val missLineY = 0.9f

    fun configFor(
        difficulty: GameDifficulty,
        minLength: Int,
        maxLength: Int
    ): TypingSprintConfig = when (difficulty) {
        GameDifficulty.EASY -> TypingSprintConfig(
            fallSpeed = 0.05f,
            spawnInterval = 3.0f,
            minWordLength = minLength,
            maxWordLength = maxLength,
            maxMisses = 10,
            maxWords = 5
        )
        GameDifficulty.MEDIUM -> TypingSprintConfig(
            fallSpeed = 0.08f,
            spawnInterval = 2.5f,
            minWordLength = minLength,
            maxWordLength = maxLength,
            maxMisses = 7,
            maxWords = 7
        )
        GameDifficulty.HARD -> TypingSprintConfig(
            fallSpeed = 0.12f,
            spawnInterval = 2.0f,
            minWordLength = minLength,
            maxWordLength = maxLength,
            maxMisses = 5,
            maxWords = 10
        )
    }

    fun step(
        state: TypingSprintState,
        config: TypingSprintConfig,
        dt: Float,
        wordData: WordData,
        random: Random
    ): TypingSprintStep {
        if (state.gameOver) return TypingSprintStep(state, TypingSprintEvent.NONE)

        var s = state.copy(elapsedSeconds = state.elapsedSeconds + dt, spawnTimer = state.spawnTimer + dt)

        val words = s.words.map { word -> word.copy(y = word.y + config.fallSpeed * dt) }
        s = s.copy(words = words)

        var event = TypingSprintEvent.NONE
        val missedWords = s.words.filter { it.y >= missLineY && it.isActive }
        if (missedWords.isNotEmpty()) {
            val updatedWords = s.words.map { word ->
                if (word.y >= missLineY && word.isActive) word.copy(isActive = false) else word
            }
            s = s.copy(words = updatedWords, missedWords = s.missedWords + missedWords.size)
            event = TypingSprintEvent.WORD_MISSED
        }

        if (s.spawnTimer >= config.spawnInterval && s.words.size < config.maxWords) {
            val word = wordData.randomWord(config.minWordLength, config.maxWordLength, random)
            if (word != null) {
                val x = 0.1f + random.nextFloat() * 0.8f
                val newWord = FallingWord(word, x, 0f, s.nextWordId)
                s = s.copy(
                    words = s.words + newWord,
                    nextWordId = s.nextWordId + 1,
                    spawnTimer = 0f
                )
            }
        }

        if (s.missedWords >= config.maxMisses) {
            return TypingSprintStep(s.copy(gameOver = true), TypingSprintEvent.GAME_OVER)
        }

        return TypingSprintStep(s, event)
    }

    fun typeChar(state: TypingSprintState, char: Char): TypingSprintState {
        val newInput = state.currentInput + char.lowercase()
        return state.copy(
            currentInput = newInput,
            totalChars = state.totalChars + 1
        )
    }

    fun backspace(state: TypingSprintState): TypingSprintState {
        if (state.currentInput.isEmpty()) return state
        return state.copy(currentInput = state.currentInput.dropLast(1))
    }

    fun checkMatch(state: TypingSprintState): TypingSprintStep {
        val matchingWord = state.words.firstOrNull { it.isActive && it.text == state.currentInput }
        if (matchingWord != null) {
            val updatedWords = state.words.filterNot { it.id == matchingWord.id }
            val newState = state.copy(
                words = updatedWords,
                currentInput = "",
                correctChars = state.correctChars + matchingWord.text.length,
                clearedWords = state.clearedWords + 1
            )
            return TypingSprintStep(newState, TypingSprintEvent.WORD_CLEARED)
        }
        return TypingSprintStep(state, TypingSprintEvent.NONE)
    }

    fun computeWpm(state: TypingSprintState): Float {
        if (state.elapsedSeconds <= 0f) return 0f
        val minutes = state.elapsedSeconds / 60f
        return (state.correctChars / 5f) / minutes
    }

    fun computeAccuracy(state: TypingSprintState): Float {
        if (state.totalChars == 0) return 100f
        return (state.correctChars.toFloat() / state.totalChars.toFloat()) * 100f
    }
}
