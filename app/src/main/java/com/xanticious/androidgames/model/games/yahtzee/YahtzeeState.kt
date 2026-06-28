package com.xanticious.androidgames.model.games.yahtzee

import com.xanticious.androidgames.model.GameDifficulty

enum class YahtzeeCategory(val label: String) {
    ONES("Ones"),
    TWOS("Twos"),
    THREES("Threes"),
    FOURS("Fours"),
    FIVES("Fives"),
    SIXES("Sixes"),
    THREE_OF_A_KIND("3 of a Kind"),
    FOUR_OF_A_KIND("4 of a Kind"),
    FULL_HOUSE("Full House"),
    SMALL_STRAIGHT("Small Straight"),
    LARGE_STRAIGHT("Large Straight"),
    YAHTZEE("Yahtzee"),
    CHANCE("Chance");

    val upperFace: Int?
        get() = when (this) {
            ONES -> 1
            TWOS -> 2
            THREES -> 3
            FOURS -> 4
            FIVES -> 5
            SIXES -> 6
            else -> null
        }

    val isUpper: Boolean get() = upperFace != null
}

enum class YahtzeePlayer { HUMAN, AI }

enum class YahtzeeResult { HUMAN_WIN, AI_WIN, DRAW }

data class YahtzeeConfig(
    val difficulty: GameDifficulty,
    val aiRandomness: Double,
    val aiRolls: Int = 3,
    val upperBonusThreshold: Int = 63,
    val upperBonusPoints: Int = 35,
    val yahtzeeBonusPoints: Int = 100,
    val totalRounds: Int = 13
)

data class YahtzeeState(
    val dice: List<Int>,
    val held: List<Boolean>,
    val rollsLeft: Int,
    val playerScorecard: Map<YahtzeeCategory, Int?>,
    val aiScorecard: Map<YahtzeeCategory, Int?>,
    val currentPlayer: YahtzeePlayer,
    val round: Int,
    val config: YahtzeeConfig,
    val playerYahtzeeBonus: Int,
    val aiYahtzeeBonus: Int,
    val result: YahtzeeResult?
) {
    companion object {
        const val DiceCount = 5
        const val MaxRolls = 3

        fun initial(config: YahtzeeConfig): YahtzeeState {
            val emptyCard = YahtzeeCategory.entries.associateWith { null as Int? }
            return YahtzeeState(
                dice = List(DiceCount) { 1 },
                held = List(DiceCount) { false },
                rollsLeft = MaxRolls,
                playerScorecard = emptyCard,
                aiScorecard = emptyCard,
                currentPlayer = YahtzeePlayer.HUMAN,
                round = 1,
                config = config,
                playerYahtzeeBonus = 0,
                aiYahtzeeBonus = 0,
                result = null
            )
        }
    }
}
