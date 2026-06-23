package com.xanticious.androidgames.model.games.wordchain

data class WordChainState(
    val chain: List<String>,
    val usedWords: Set<String>,
    val currentInput: String = "",
    val timeRemaining: Float? = null,
    val chainBroken: Boolean = false
) {
    val lastWord: String? get() = chain.lastOrNull()
    val chainLength: Int get() = chain.size
    val totalLetters: Int get() = chain.sumOf { it.length }
}

data class WordChainConfig(
    val linkRule: LinkRule,
    val minWordLength: Int,
    val perWordTimer: Float?
) {
    companion object {
        fun default() = WordChainConfig(
            linkRule = LinkRule.LAST_LETTER,
            minWordLength = 3,
            perWordTimer = 20f
        )
    }
}

enum class LinkRule(val chars: Int, val label: String) {
    LAST_LETTER(1, "Last letter"),
    LAST_TWO_LETTERS(2, "Last two letters")
}

enum class WordChainInputMode { FIELD, ON_SCREEN_KEYBOARD }

sealed class WordChainValidationResult {
    data object Valid : WordChainValidationResult()
    data class Invalid(val reason: String) : WordChainValidationResult()
}
