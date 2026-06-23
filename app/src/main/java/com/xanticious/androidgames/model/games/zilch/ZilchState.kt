package com.xanticious.androidgames.model.games.zilch

import com.xanticious.androidgames.model.GameDifficulty

enum class ZilchPlayer { PLAYER, AI }

enum class ZilchResult { PLAYER_WIN, AI_WIN }

data class ZilchConfig(
    val winningTarget: Int = 10_000,
    val difficulty: GameDifficulty = GameDifficulty.MEDIUM,
    val aiBankThreshold: Int = 650,
    val aiHotDiceBankThreshold: Int = 1_000,
    val aiMinimumRemainingDiceToPush: Int = 3
)

data class ZilchScore(
    val points: Int,
    val valid: Boolean,
    val description: String = ""
)

data class ZilchAiDecision(
    val selectedIndices: Set<Int>,
    val shouldBank: Boolean
)

data class ZilchState(
    val dice: List<Int>,
    val locked: List<Boolean>,
    val selected: List<Boolean>,
    val turnPoints: Int,
    val playerBanked: Int,
    val aiBanked: Int,
    val currentPlayer: ZilchPlayer,
    val config: ZilchConfig,
    val result: ZilchResult?,
    val lastRollZilch: Boolean,
    val message: String
) {
    val remainingDice: Int
        get() = locked.count { !it }.let { if (it == 0) 6 else it }

    val selectedIndices: Set<Int>
        get() = selected.mapIndexedNotNull { index, isSelected -> if (isSelected) index else null }.toSet()

    companion object {
        const val DICE_COUNT = 6

        fun initial(config: ZilchConfig = ZilchConfig()): ZilchState = ZilchState(
            dice = List(DICE_COUNT) { 1 },
            locked = List(DICE_COUNT) { false },
            selected = List(DICE_COUNT) { false },
            turnPoints = 0,
            playerBanked = 0,
            aiBanked = 0,
            currentPlayer = ZilchPlayer.PLAYER,
            config = config,
            result = null,
            lastRollZilch = false,
            message = "Roll six dice to start."
        )
    }
}
