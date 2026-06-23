package com.xanticious.androidgames.model.games.wordladder

import com.xanticious.androidgames.model.GameDifficulty

data class WordLadderPuzzle(
    val startWord: String,
    val targetWord: String,
    val shortestPathLength: Int
)

data class WordLadderState(
    val puzzle: WordLadderPuzzle,
    val ladder: List<String>,
    val currentInput: String = ""
) {
    val currentWord: String get() = ladder.lastOrNull() ?: puzzle.startWord
    val steps: Int get() = ladder.size - 1
    val solved: Boolean get() = currentWord == puzzle.targetWord
}

data class WordLadderConfig(
    val wordLength: Int,
    val difficulty: GameDifficulty
) {
    companion object {
        fun default() = WordLadderConfig(wordLength = 4, difficulty = GameDifficulty.MEDIUM)
    }
}

enum class WordLadderInputMode { FIELD, ON_SCREEN_KEYBOARD }

sealed class WordLadderValidationResult {
    data object Valid : WordLadderValidationResult()
    data class Invalid(val reason: String) : WordLadderValidationResult()
}
