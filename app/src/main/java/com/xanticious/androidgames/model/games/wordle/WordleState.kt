package com.xanticious.androidgames.model.games.wordle

data class WordleSettings(
    val wordLength: Int = 5,
    val maxGuesses: Int = 6,
    val enforceConsistency: Boolean = true,
    val carryFirstGuess: Boolean = true
)

enum class LetterHint { CORRECT, PRESENT, ABSENT }

data class GuessResult(
    val word: String,
    val hints: List<LetterHint>
)

data class WordleRoundState(
    val targetWord: String,
    val guesses: List<GuessResult>,
    val currentInput: String = "",
    val previousTarget: String? = null,
    val isFirstGuess: Boolean = true,
    val won: Boolean = false,
    val lost: Boolean = false
)

data class ConstraintState(
    val correctPositions: Map<Int, Char>,
    val presentLetters: Set<Char>,
    val absentLetters: Set<Char>
)
