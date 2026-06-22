package com.xanticious.androidgames.model.games.anagramsarcade

data class AnagramsArcadeState(
    val letters: List<Char> = emptyList(),
    val targetWords: List<String> = emptyList(),
    val foundWords: Set<String> = emptySet(),
    val currentEntry: String = "",
    val usedIndices: Set<Int> = emptySet(),
    val score: Int = 0,
    val minLength: Int = 3,
    val message: String = "",
    val timeRemaining: Int = 90
)

data class AnagramsArcadeConfig(
    val minLength: Int = 3,
    val letterSetBias: LetterSetBias = LetterSetBias.BALANCED,
    val roundDuration: Int = 90
)

enum class LetterSetBias {
    BALANCED,
    VOWEL_HEAVY
}
