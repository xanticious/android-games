package com.xanticious.androidgames.model.games.wordslices

import com.xanticious.androidgames.model.GameDifficulty

const val TOTAL_SLICES = 12

data class WordSlicesSettings(
    val difficulty: GameDifficulty = GameDifficulty.MEDIUM,
    val revealDefinition: Boolean = true,
    val letterCase: LetterCase = LetterCase.UPPER
)

enum class LetterCase { UPPER, LOWER }

data class WordSlicesRoundState(
    val word: String,
    val revealedLetters: Set<Char>,
    val guessedLetters: Set<Char>,
    val wrongGuesses: Set<Char>,
    val slicesRemaining: Int,
    val won: Boolean = false,
    val lost: Boolean = false,
    val definition: String? = null
)
