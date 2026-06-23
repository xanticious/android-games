package com.xanticious.androidgames.model.games.basedefense

import com.xanticious.androidgames.model.GameDifficulty

/**
 * The three placeable towers in Base Defense.
 * GUN → SINGLE_TARGET, MORTAR → AOE, FROST → SLOW.
 */
enum class BaseTower {
    GUN,
    MORTAR,
    FROST
}

/**
 * Immutable settings snapshot created at game-start.
 * The [seed] drives the procedural map so the same seed always
 * produces the same layout.
 */
data class BaseDefenseSettings(
    val difficulty: GameDifficulty,
    val seed: Long
)
