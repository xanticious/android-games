package com.xanticious.androidgames.controller.games.wordslices

import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.GameDifficulty
import kotlin.random.Random

class WordSlicesController {

    fun selectWord(
        wordData: WordData,
        difficulty: GameDifficulty,
        random: Random = Random.Default
    ): String {
        val (minLen, maxLen) = when (difficulty) {
            GameDifficulty.EASY -> 4 to 6
            GameDifficulty.MEDIUM -> 6 to 9
            GameDifficulty.HARD -> 9 to 12
        }
        
        return wordData.randomWord(minLen, maxLen, random)
            ?: wordData.randomWord(4, 12, random)
            ?: throw IllegalStateException("No words available")
    }

    fun isLetterInWord(letter: Char, word: String): Boolean {
        return letter.lowercaseChar() in word.lowercase()
    }

    fun revealedPositions(word: String, guessedLetters: Set<Char>): Set<Int> {
        return word.indices.filter { i ->
            word[i].lowercaseChar() in guessedLetters.map { it.lowercaseChar() }
        }.toSet()
    }

    fun isWordFullyRevealed(word: String, guessedLetters: Set<Char>): Boolean {
        return word.all { letter ->
            letter.lowercaseChar() in guessedLetters.map { it.lowercaseChar() }
        }
    }

    fun computeSlicesRemaining(wrongGuessCount: Int, totalSlices: Int = 12): Int {
        return (totalSlices - wrongGuessCount).coerceAtLeast(0)
    }

    fun isGameWon(word: String, guessedLetters: Set<Char>, slicesRemaining: Int): Boolean {
        return isWordFullyRevealed(word, guessedLetters) && slicesRemaining > 0
    }

    fun isGameLost(slicesRemaining: Int): Boolean {
        return slicesRemaining <= 0
    }

    fun processGuess(
        letter: Char,
        word: String,
        guessedLetters: Set<Char>,
        wrongGuesses: Set<Char>,
        slicesRemaining: Int
    ): GuessResult {
        val normalizedLetter = letter.lowercaseChar()
        
        if (normalizedLetter in guessedLetters.map { it.lowercaseChar() }) {
            return GuessResult(
                alreadyGuessed = true,
                correct = false,
                newGuessedLetters = guessedLetters,
                newWrongGuesses = wrongGuesses,
                newSlicesRemaining = slicesRemaining
            )
        }
        
        val isCorrect = isLetterInWord(normalizedLetter, word)
        val newGuessedLetters = guessedLetters + normalizedLetter
        val newWrongGuesses = if (isCorrect) wrongGuesses else wrongGuesses + normalizedLetter
        val newSlicesRemaining = if (isCorrect) slicesRemaining else slicesRemaining - 1
        
        return GuessResult(
            alreadyGuessed = false,
            correct = isCorrect,
            newGuessedLetters = newGuessedLetters,
            newWrongGuesses = newWrongGuesses,
            newSlicesRemaining = newSlicesRemaining
        )
    }
}

data class GuessResult(
    val alreadyGuessed: Boolean,
    val correct: Boolean,
    val newGuessedLetters: Set<Char>,
    val newWrongGuesses: Set<Char>,
    val newSlicesRemaining: Int
)
