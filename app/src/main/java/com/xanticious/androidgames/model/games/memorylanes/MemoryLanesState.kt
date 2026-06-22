package com.xanticious.androidgames.model.games.memorylanes

private const val DEFAULT_BANK_SIZE = 4
private const val DEFAULT_REVEAL_SECONDS = 2
private const val FIRST_ROUND = 1

data class MemoryTile(val id: Int)

enum class MemoryTileLabelStyle { NUMBERS, COLORS_ONLY, LETTERS }

data class MemoryLanesSettings(
    val bankSize: Int = DEFAULT_BANK_SIZE,
    val revealDurationSeconds: Int = DEFAULT_REVEAL_SECONDS,
    val labelStyle: MemoryTileLabelStyle = MemoryTileLabelStyle.NUMBERS,
    val confirmWrong: Boolean = true
)

data class MemoryLanesConfig(
    val bankSize: Int,
    val revealDurationSeconds: Int,
    val labelStyle: MemoryTileLabelStyle = MemoryTileLabelStyle.NUMBERS,
    val confirmWrong: Boolean = true
)

data class MemoryTileCount(val tile: MemoryTile, val remaining: Int)

data class MemoryValidation(
    val correct: Boolean,
    val firstWrongIndex: Int,
    val expectedTile: MemoryTile
) {
    companion object {
        val CORRECT = MemoryValidation(correct = true, firstWrongIndex = -1, expectedTile = MemoryTile(0))
    }
}

data class MemoryLanesGameState(
    val round: Int,
    val sequence: List<MemoryTile>,
    val builtSequence: List<MemoryTile>,
    val validation: MemoryValidation = MemoryValidation.CORRECT,
    val bestSequence: Int = 0
) {
    val currentTile: MemoryTile get() = sequence.last()

    companion object {
        fun initial(firstTile: MemoryTile): MemoryLanesGameState = MemoryLanesGameState(
            round = FIRST_ROUND,
            sequence = listOf(firstTile),
            builtSequence = emptyList()
        )
    }
}

data class MemoryLanesStats(
    val longestSequenceReached: Int = 0,
    val longestByBankSize: Map<Int, Int> = emptyMap(),
    val totalRoundsPlayed: Int = 0,
    val totalGamesPlayed: Int = 0,
    val gamesEndedOnRoundOne: Int = 0
)
