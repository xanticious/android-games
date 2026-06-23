package com.xanticious.androidgames.model.games.randomizeddice

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.towerdefense.TdGameState

// ---------------------------------------------------------------------------
// Randomized Dice TD — model layer.
// Per design/randomized-dice-td.md.
// ---------------------------------------------------------------------------

/** The four purchase/upgrade actions the player may take during the Building phase. */
enum class DiceAction { BUY_RANDOM, BUY_SPECIFIC, UPGRADE_RANDOM, UPGRADE_SPECIFIC }

/**
 * Gold costs for each action.
 *
 * [upgradeSpecificBase] + [upgradeSpecificPerLevel] * targetLevel = upgrade cost for
 * a tower currently at [targetLevel].  Cost ordering invariant:
 * buyRandom < upgradeRandom < buySpecific.
 */
data class ActionCosts(
    val buyRandom: Int,
    val buySpecific: Int,
    val upgradeRandom: Int,
    val upgradeSpecificBase: Int,
    val upgradeSpecificPerLevel: Int
)

/** Settings chosen on the pre-game screen. [seed] drives all randomness. */
data class DiceTdSettings(val difficulty: GameDifficulty, val seed: Long)

/**
 * Full snapshot of a Randomized Dice TD session.
 *
 * [base] holds the shared TD game state (map, towers, enemies, waves, money, lives, …).
 * [rngSeed] advances after every random action to keep outcomes reproducible.
 * [lastAction] records which action was last performed (used by the view for feedback).
 */
data class DiceTdGameState(
    val base: TdGameState,
    val costs: ActionCosts,
    val rngSeed: Long,
    val lastAction: DiceAction? = null
)
