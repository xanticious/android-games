package com.xanticious.androidgames.controller.games.wordle

import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.games.wordle.ConstraintState
import com.xanticious.androidgames.model.games.wordle.GuessResult
import com.xanticious.androidgames.model.games.wordle.LetterHint
import com.xanticious.androidgames.model.games.wordle.WordleSettings
import kotlin.random.Random

class WordleController {

    fun selectTarget(
        wordData: WordData,
        settings: WordleSettings,
        random: Random = Random.Default
    ): String {
        return wordData.randomWordOfLength(settings.wordLength, random)
            ?: throw IllegalStateException("No words of length ${settings.wordLength}")
    }

    fun computeHints(guess: String, target: String): List<LetterHint> {
        val hints = MutableList(guess.length) { LetterHint.ABSENT }
        val targetLetters = target.toMutableList()
        
        // First pass: mark correct positions (green)
        for (i in guess.indices) {
            if (guess[i] == target[i]) {
                hints[i] = LetterHint.CORRECT
                targetLetters[i] = '\u0000' // Mark as used
            }
        }
        
        // Second pass: mark present letters (yellow)
        for (i in guess.indices) {
            if (hints[i] == LetterHint.ABSENT) {
                val idx = targetLetters.indexOf(guess[i])
                if (idx >= 0) {
                    hints[i] = LetterHint.PRESENT
                    targetLetters[idx] = '\u0000' // Mark as used
                }
            }
        }
        
        return hints
    }

    fun deriveConstraints(guesses: List<GuessResult>): ConstraintState {
        val correctPositions = mutableMapOf<Int, Char>()
        val presentLetters = mutableSetOf<Char>()
        val absentLetters = mutableSetOf<Char>()
        
        for (result in guesses) {
            for (i in result.hints.indices) {
                val letter = result.word[i]
                when (result.hints[i]) {
                    LetterHint.CORRECT -> correctPositions[i] = letter
                    LetterHint.PRESENT -> presentLetters.add(letter)
                    LetterHint.ABSENT -> {
                        // Only mark as absent if it's never correct or present
                        if (letter !in correctPositions.values && letter !in presentLetters) {
                            absentLetters.add(letter)
                        }
                    }
                }
            }
        }
        
        return ConstraintState(correctPositions, presentLetters, absentLetters)
    }

    fun isConsistentWithConstraints(
        word: String,
        constraints: ConstraintState
    ): Boolean {
        // Check correct positions
        for ((pos, letter) in constraints.correctPositions) {
            if (pos >= word.length || word[pos] != letter) {
                return false
            }
        }
        
        // Check present letters are included
        for (letter in constraints.presentLetters) {
            if (letter !in word) {
                return false
            }
        }
        
        // Check absent letters are not included
        for (letter in constraints.absentLetters) {
            if (letter in word) {
                return false
            }
        }
        
        return true
    }

    fun findValidGuess(
        wordData: WordData,
        constraints: ConstraintState,
        wordLength: Int,
        random: Random = Random.Default
    ): String? {
        val candidates = wordData.wordsOfLength(wordLength)
            .filter { isConsistentWithConstraints(it, constraints) }
        
        return if (candidates.isEmpty()) null
        else candidates[random.nextInt(candidates.size)]
    }

    fun validateGuess(
        word: String,
        wordData: WordData,
        settings: WordleSettings,
        constraints: ConstraintState?
    ): Boolean {
        if (word.length != settings.wordLength) return false
        if (!wordData.isValidWord(word)) return false
        
        if (settings.enforceConsistency && constraints != null) {
            if (!isConsistentWithConstraints(word, constraints)) return false
        }
        
        return true
    }

    fun getFirstGuess(
        previousTarget: String?,
        settings: WordleSettings,
        wordData: WordData,
        random: Random = Random.Default
    ): String {
        return if (settings.carryFirstGuess && previousTarget != null) {
            previousTarget
        } else {
            selectTarget(wordData, settings, random)
        }
    }
}
