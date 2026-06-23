package com.xanticious.androidgames.controller.games.memorylanes

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.memorylanes.MemoryLanesConfig
import com.xanticious.androidgames.model.games.memorylanes.MemoryLanesGameState
import com.xanticious.androidgames.model.games.memorylanes.MemoryLanesSettings
import com.xanticious.androidgames.model.games.memorylanes.MemoryLanesStats
import com.xanticious.androidgames.model.games.memorylanes.MemoryTile
import com.xanticious.androidgames.model.games.memorylanes.MemoryTileCount
import com.xanticious.androidgames.model.games.memorylanes.MemoryTileLabelStyle
import com.xanticious.androidgames.model.games.memorylanes.MemoryValidation
import kotlin.random.Random

class MemoryLanesController {
    fun configFor(difficulty: GameDifficulty): MemoryLanesConfig = when (difficulty) {
        GameDifficulty.EASY -> MemoryLanesConfig(bankSize = 3, revealDurationSeconds = 3)
        GameDifficulty.MEDIUM -> MemoryLanesConfig(bankSize = 4, revealDurationSeconds = 2)
        GameDifficulty.HARD -> MemoryLanesConfig(bankSize = 6, revealDurationSeconds = 1)
    }

    fun configFor(settings: MemoryLanesSettings): MemoryLanesConfig {
        require(settings.bankSize in 2..10) { "bankSize must be between 2 and 10" }
        require(settings.revealDurationSeconds in 1..3) { "revealDurationSeconds must be 1, 2, or 3" }
        return MemoryLanesConfig(
            bankSize = settings.bankSize,
            revealDurationSeconds = settings.revealDurationSeconds,
            labelStyle = settings.labelStyle,
            confirmWrong = settings.confirmWrong
        )
    }

    fun nextTile(config: MemoryLanesConfig, random: Random): MemoryTile = MemoryTile(random.nextInt(config.bankSize) + 1)

    fun startState(config: MemoryLanesConfig, random: Random): MemoryLanesGameState =
        MemoryLanesGameState.initial(nextTile(config, random))

    fun advanceRound(state: MemoryLanesGameState, config: MemoryLanesConfig, random: Random): MemoryLanesGameState {
        val sequence = state.sequence + nextTile(config, random)
        return state.copy(round = state.round + 1, sequence = sequence, builtSequence = emptyList(), validation = MemoryValidation.CORRECT)
    }

    fun remainingCounts(sequence: List<MemoryTile>, builtSequence: List<MemoryTile>, bankSize: Int): List<MemoryTileCount> =
        (1..bankSize).map { id ->
            val targetCount = sequence.count { it.id == id }
            val usedCount = builtSequence.count { it.id == id }
            MemoryTileCount(MemoryTile(id), (targetCount - usedCount).coerceAtLeast(0))
        }

    fun addTile(state: MemoryLanesGameState, tile: MemoryTile, config: MemoryLanesConfig): MemoryLanesGameState {
        val remaining = remainingCounts(state.sequence, state.builtSequence, config.bankSize)
            .firstOrNull { it.tile == tile }
            ?.remaining ?: 0
        return if (remaining > 0 && state.builtSequence.size < state.sequence.size) {
            state.copy(builtSequence = state.builtSequence + tile)
        } else {
            state
        }
    }

    fun undoTile(state: MemoryLanesGameState): MemoryLanesGameState =
        if (state.builtSequence.isEmpty()) state else state.copy(builtSequence = state.builtSequence.dropLast(1))

    fun canSubmit(state: MemoryLanesGameState): Boolean = state.builtSequence.size == state.sequence.size

    fun validate(state: MemoryLanesGameState): MemoryValidation {
        val wrongIndex = state.sequence.indices.firstOrNull { state.builtSequence.getOrNull(it) != state.sequence[it] }
        return if (wrongIndex == null && state.builtSequence.size == state.sequence.size) {
            MemoryValidation.CORRECT
        } else {
            val index = wrongIndex ?: state.builtSequence.size.coerceAtMost(state.sequence.lastIndex)
            MemoryValidation(correct = false, firstWrongIndex = index, expectedTile = state.sequence[index])
        }
    }

    fun labelFor(tile: MemoryTile, style: MemoryTileLabelStyle): String = when (style) {
        MemoryTileLabelStyle.NUMBERS -> tile.id.toString()
        MemoryTileLabelStyle.COLORS_ONLY -> ""
        MemoryTileLabelStyle.LETTERS -> ('A'.code + tile.id - 1).toChar().toString()
    }

    fun updateStats(stats: MemoryLanesStats, state: MemoryLanesGameState, config: MemoryLanesConfig): MemoryLanesStats {
        val survived = (state.round - 1).coerceAtLeast(0)
        val byBank = stats.longestByBankSize + (config.bankSize to maxOf(stats.longestByBankSize[config.bankSize] ?: 0, survived))
        return stats.copy(
            longestSequenceReached = maxOf(stats.longestSequenceReached, survived),
            longestByBankSize = byBank,
            totalRoundsPlayed = stats.totalRoundsPlayed + state.round,
            totalGamesPlayed = stats.totalGamesPlayed + 1,
            gamesEndedOnRoundOne = stats.gamesEndedOnRoundOne + if (state.round == 1) 1 else 0
        )
    }
}
