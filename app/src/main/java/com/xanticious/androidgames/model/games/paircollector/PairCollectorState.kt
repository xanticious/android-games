package com.xanticious.androidgames.model.games.paircollector

private const val FIRST_ROUND = 1
private const val DEFAULT_CARD_COUNT = 20
private const val DEFAULT_TOTAL_ROUNDS = 10
private const val DEFAULT_MAX_STRIKES = 3

enum class Suit(val symbol: String, val isRed: Boolean) {
    CLUBS("♣", false),
    DIAMONDS("♦", true),
    HEARTS("♥", true),
    SPADES("♠", false)
}

enum class Rank(val label: String) {
    ACE("A"), TWO("2"), THREE("3"), FOUR("4"), FIVE("5"), SIX("6"), SEVEN("7"),
    EIGHT("8"), NINE("9"), TEN("10"), JACK("J"), QUEEN("Q"), KING("K")
}

data class PlayingCard(val rank: Rank, val suit: Suit) {
    val label: String get() = rank.label + suit.symbol
}

enum class PairCollectorTimeLimit(val seconds: Int) {
    OFF(0), TEN(10), TWENTY(20), THIRTY(30)
}

enum class CardBackStyle(val label: String) {
    NAVY_GOLD("Navy/Gold"), PIRATE("Pirate"), CLASSIC_RED("Classic Red")
}

data class PairCollectorSettings(
    val cardCount: Int = DEFAULT_CARD_COUNT,
    val timeLimit: PairCollectorTimeLimit = PairCollectorTimeLimit.OFF,
    val cardBackStyle: CardBackStyle = CardBackStyle.NAVY_GOLD,
    val showRoundTimer: Boolean = true
)

data class PairCollectorConfig(
    val cardCount: Int,
    val totalRounds: Int = DEFAULT_TOTAL_ROUNDS,
    val maxStrikes: Int = DEFAULT_MAX_STRIKES,
    val timeLimit: PairCollectorTimeLimit = PairCollectorTimeLimit.OFF,
    val cardBackStyle: CardBackStyle = CardBackStyle.NAVY_GOLD,
    val showRoundTimer: Boolean = true
)

data class PairCollectorRound(
    val cards: List<PlayingCard>,
    val duplicate: PlayingCard
)

data class PairCollectorGameState(
    val roundNumber: Int,
    val completedRounds: Int,
    val strikes: Int,
    val totalElapsedSeconds: Float,
    val roundElapsedSeconds: Float,
    val selectedCardIndex: Int,
    val currentRound: PairCollectorRound,
    val previousDuplicate: PlayingCard,
    val outcome: PairCollectorOutcome = PairCollectorOutcome.IN_PROGRESS
) {
    companion object {
        const val NO_SELECTION = -1

        fun initial(round: PairCollectorRound): PairCollectorGameState = PairCollectorGameState(
            roundNumber = FIRST_ROUND,
            completedRounds = 0,
            strikes = 0,
            totalElapsedSeconds = 0f,
            roundElapsedSeconds = 0f,
            selectedCardIndex = NO_SELECTION,
            currentRound = round,
            previousDuplicate = round.duplicate
        )
    }
}

enum class PairCollectorOutcome { IN_PROGRESS, VICTORY, DEFEAT }

data class PairCollectorStats(
    val highestRoundReached: Int = 0,
    val bestTotalTimeSeconds: Float = 0f,
    val totalPairsFound: Int = 0,
    val perfectWins: Int = 0,
    val winStreak: Int = 0
)
