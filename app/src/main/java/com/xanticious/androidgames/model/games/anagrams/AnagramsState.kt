package com.xanticious.androidgames.model.games.anagrams

/**
 * Immutable model for Anagrams game state. Per `design/word-games/anagrams/anagrams-design.md`.
 */
data class AnagramsState(
    val letters: List<Char> = emptyList(),
    val targetWords: List<String> = emptyList(),
    val foundWords: Set<String> = emptySet(),
    val currentEntry: String = "",
    val usedIndices: Set<Int> = emptySet(),
    val score: Int = 0,
    val minLength: Int = 3,
    val message: String = ""
)

data class AnagramsConfig(
    val minLength: Int = 3,
    val letterSetBias: LetterSetBias = LetterSetBias.BALANCED
)

enum class LetterSetBias {
    BALANCED,
    VOWEL_HEAVY
}
