package com.xanticious.androidgames.model.games.memory

import com.xanticious.androidgames.model.GameDifficulty

enum class MemoryPlayer { HUMAN, AI }

enum class MemoryOutcome { IN_PROGRESS, HUMAN_WIN, AI_WIN, TIE }

data class MemoryCard(
    val id: Int,
    val symbolId: Int,
    val faceUp: Boolean = false,
    val matched: Boolean = false,
    val matchedBy: MemoryPlayer? = null
)

data class MemoryConfig(
    val rows: Int,
    val columns: Int,
    val difficulty: GameDifficulty,
    val memoryAccuracy: Float,
    val firstPlayer: MemoryPlayer = MemoryPlayer.HUMAN
) {
    val pairCount: Int
        get() = rows * columns / 2
}

data class MemoryState(
    val config: MemoryConfig,
    val cards: List<MemoryCard>,
    val currentPlayer: MemoryPlayer,
    val flippedIndices: List<Int>,
    val humanPairs: Int,
    val aiPairs: Int,
    val moves: Int,
    val elapsedSeconds: Int,
    val turnNumber: Int,
    val seenSymbolByIndex: Map<Int, Int>,
    val result: MemoryOutcome
) {
    companion object {
        fun empty(config: MemoryConfig): MemoryState = MemoryState(
            config = config,
            cards = emptyList(),
            currentPlayer = config.firstPlayer,
            flippedIndices = emptyList(),
            humanPairs = 0,
            aiPairs = 0,
            moves = 0,
            elapsedSeconds = 0,
            turnNumber = 1,
            seenSymbolByIndex = emptyMap(),
            result = MemoryOutcome.IN_PROGRESS
        )
    }
}

enum class MemoryResolve { MATCH, MISS, GAME_OVER, NONE }

data class MemoryStep(
    val state: MemoryState,
    val resolve: MemoryResolve
)
