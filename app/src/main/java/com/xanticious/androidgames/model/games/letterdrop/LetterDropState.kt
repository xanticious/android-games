package com.xanticious.androidgames.model.games.letterdrop

data class FallingTile(
    val letter: Char,
    val x: Float,
    val y: Float,
    val id: Int,
    val isQueued: Boolean = false
)

data class LetterDropState(
    val tiles: List<FallingTile>,
    val currentEntry: String,
    val score: Int,
    val longestWord: String,
    val totalWordsFormed: Int,
    val elapsedSeconds: Float,
    val nextTileId: Int,
    val spawnTimer: Float,
    val gameOver: Boolean
) {
    companion object {
        fun initial(): LetterDropState = LetterDropState(
            tiles = emptyList(),
            currentEntry = "",
            score = 0,
            longestWord = "",
            totalWordsFormed = 0,
            elapsedSeconds = 0f,
            nextTileId = 0,
            spawnTimer = 0f,
            gameOver = false
        )
    }
}

data class LetterDropConfig(
    val dropSpeed: Float,
    val spawnInterval: Float,
    val minWordLength: Int,
    val vowelRichness: Float
)

enum class LetterDropEvent { NONE, WORD_CLEARED, OVERFLOW }

data class LetterDropStep(val state: LetterDropState, val event: LetterDropEvent)
