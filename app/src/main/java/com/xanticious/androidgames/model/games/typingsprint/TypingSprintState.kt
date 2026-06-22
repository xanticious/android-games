package com.xanticious.androidgames.model.games.typingsprint

data class FallingWord(
    val text: String,
    val x: Float,
    val y: Float,
    val id: Int,
    val isActive: Boolean = true
)

data class TypingSprintState(
    val words: List<FallingWord>,
    val currentInput: String,
    val correctChars: Int,
    val totalChars: Int,
    val missedWords: Int,
    val clearedWords: Int,
    val elapsedSeconds: Float,
    val nextWordId: Int,
    val spawnTimer: Float,
    val gameOver: Boolean
) {
    companion object {
        fun initial(): TypingSprintState = TypingSprintState(
            words = emptyList(),
            currentInput = "",
            correctChars = 0,
            totalChars = 0,
            missedWords = 0,
            clearedWords = 0,
            elapsedSeconds = 0f,
            nextWordId = 0,
            spawnTimer = 0f,
            gameOver = false
        )
    }
}

data class TypingSprintConfig(
    val fallSpeed: Float,
    val spawnInterval: Float,
    val minWordLength: Int,
    val maxWordLength: Int,
    val maxMisses: Int,
    val maxWords: Int
)

enum class TypingSprintEvent { NONE, WORD_CLEARED, WORD_MISSED, GAME_OVER }

data class TypingSprintStep(val state: TypingSprintState, val event: TypingSprintEvent)
