package com.xanticious.androidgames.controller.games.paircollector

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.paircollector.CardBackStyle
import com.xanticious.androidgames.model.games.paircollector.PairCollectorConfig
import com.xanticious.androidgames.model.games.paircollector.PairCollectorGameState
import com.xanticious.androidgames.model.games.paircollector.PairCollectorOutcome
import com.xanticious.androidgames.model.games.paircollector.PairCollectorRound
import com.xanticious.androidgames.model.games.paircollector.PairCollectorSettings
import com.xanticious.androidgames.model.games.paircollector.PairCollectorStats
import com.xanticious.androidgames.model.games.paircollector.PairCollectorTimeLimit
import com.xanticious.androidgames.model.games.paircollector.PlayingCard
import com.xanticious.androidgames.model.games.paircollector.Rank
import com.xanticious.androidgames.model.games.paircollector.Suit
import kotlin.random.Random

class PairCollectorController {
    private val deck: List<PlayingCard> = Suit.entries.flatMap { suit -> Rank.entries.map { rank -> PlayingCard(rank, suit) } }

    fun configFor(difficulty: GameDifficulty): PairCollectorConfig = when (difficulty) {
        GameDifficulty.EASY -> PairCollectorConfig(cardCount = 10)
        GameDifficulty.MEDIUM -> PairCollectorConfig(cardCount = 20)
        GameDifficulty.HARD -> PairCollectorConfig(cardCount = 30, timeLimit = PairCollectorTimeLimit.TWENTY)
    }

    fun configFor(settings: PairCollectorSettings): PairCollectorConfig = PairCollectorConfig(
        cardCount = settings.cardCount,
        timeLimit = settings.timeLimit,
        cardBackStyle = settings.cardBackStyle,
        showRoundTimer = settings.showRoundTimer
    )

    fun standardDeck(): List<PlayingCard> = deck

    fun generateRound(cardCount: Int, previousDuplicate: PlayingCard?, random: Random): PairCollectorRound {
        require(cardCount in 2..deck.size) { "cardCount must be between 2 and 52" }
        val uniqueCards = deck.shuffled(random).take(cardCount - 1)
        val duplicateOptions = uniqueCards.filter { it != previousDuplicate }.ifEmpty { uniqueCards }
        val duplicate = duplicateOptions[random.nextInt(duplicateOptions.size)]
        return PairCollectorRound(cards = (uniqueCards + duplicate).shuffled(random), duplicate = duplicate)
    }

    fun isDuplicatePair(round: PairCollectorRound, firstIndex: Int, secondIndex: Int): Boolean {
        val first = round.cards.getOrNull(firstIndex)
        val second = round.cards.getOrNull(secondIndex)
        return firstIndex != secondIndex && first == round.duplicate && second == round.duplicate
    }

    fun selectCard(state: PairCollectorGameState, index: Int): PairCollectorGameState {
        if (state.currentRound.cards.getOrNull(index) == null) return state
        return if (state.selectedCardIndex == index) {
            state.copy(selectedCardIndex = PairCollectorGameState.NO_SELECTION)
        } else {
            state.copy(selectedCardIndex = index)
        }
    }

    fun addStrike(state: PairCollectorGameState, config: PairCollectorConfig): PairCollectorGameState {
        val strikes = state.strikes + 1
        return state.copy(
            strikes = strikes,
            selectedCardIndex = PairCollectorGameState.NO_SELECTION,
            outcome = if (strikes >= config.maxStrikes) PairCollectorOutcome.DEFEAT else PairCollectorOutcome.IN_PROGRESS
        )
    }

    fun completeRound(
        state: PairCollectorGameState,
        config: PairCollectorConfig,
        nextRound: PairCollectorRound
    ): PairCollectorGameState {
        val completed = state.completedRounds + 1
        val victory = completed >= config.totalRounds
        return state.copy(
            roundNumber = if (victory) state.roundNumber else state.roundNumber + 1,
            completedRounds = completed,
            roundElapsedSeconds = 0f,
            selectedCardIndex = PairCollectorGameState.NO_SELECTION,
            currentRound = if (victory) state.currentRound else nextRound,
            previousDuplicate = if (victory) state.previousDuplicate else nextRound.duplicate,
            outcome = if (victory) PairCollectorOutcome.VICTORY else PairCollectorOutcome.IN_PROGRESS
        )
    }

    fun advanceTime(state: PairCollectorGameState, dtSeconds: Float): PairCollectorGameState = state.copy(
        totalElapsedSeconds = state.totalElapsedSeconds + dtSeconds.coerceAtLeast(0f),
        roundElapsedSeconds = state.roundElapsedSeconds + dtSeconds.coerceAtLeast(0f)
    )

    fun hasRoundTimedOut(state: PairCollectorGameState, config: PairCollectorConfig): Boolean =
        config.timeLimit.seconds > 0 && state.roundElapsedSeconds >= config.timeLimit.seconds

    fun updateStats(stats: PairCollectorStats, state: PairCollectorGameState, config: PairCollectorConfig): PairCollectorStats {
        val won = state.outcome == PairCollectorOutcome.VICTORY
        val bestTime = when {
            !won -> stats.bestTotalTimeSeconds
            stats.bestTotalTimeSeconds <= 0f -> state.totalElapsedSeconds
            else -> minOf(stats.bestTotalTimeSeconds, state.totalElapsedSeconds)
        }
        return stats.copy(
            highestRoundReached = maxOf(stats.highestRoundReached, state.completedRounds + if (won) 0 else 1),
            bestTotalTimeSeconds = bestTime,
            totalPairsFound = stats.totalPairsFound + state.completedRounds,
            perfectWins = stats.perfectWins + if (won && state.strikes == 0) 1 else 0,
            winStreak = if (won) stats.winStreak + 1 else 0
        )
    }

    fun cardBackLabel(style: CardBackStyle): String = when (style) {
        CardBackStyle.NAVY_GOLD -> "◆"
        CardBackStyle.PIRATE -> "☠"
        CardBackStyle.CLASSIC_RED -> "★"
    }
}
