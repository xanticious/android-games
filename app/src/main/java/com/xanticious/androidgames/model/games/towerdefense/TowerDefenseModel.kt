package com.xanticious.androidgames.model.games.towerdefense

import com.xanticious.androidgames.model.GridPos

// ---------------------------------------------------------------------------
// Shared tower-defense types used by Base Defense and Randomized Dice TD.
// Per design/common/tower-defense.md.
// ---------------------------------------------------------------------------

/** The three standard roles every lite TD tower belongs to. */
enum class TowerRole { SINGLE_TARGET, AOE, SLOW }

/** Per-level statistics for a tower. Upgraded copies have higher values. */
data class TowerStats(
    val range: Float,
    val damage: Int,
    val fireRate: Float,        // attacks per second
    val slowPct: Int = 0        // 0 unless role == SLOW
)

/** A placed tower on the board. */
data class Tower(
    val id: Int,
    val role: TowerRole,
    val tile: GridPos,
    val level: Int,
    val stats: TowerStats
)

/** A single enemy unit traversing the path. */
data class TdEnemy(
    val id: Int,
    val maxHp: Int,
    val hp: Int,
    /** Fractional progress along the path tiles [0, path.size). */
    val pathProgress: Float,
    val baseSpeed: Float,       // tiles per second
    /** Effective speed after slow effects. */
    val currentSpeed: Float,
    val bounty: Int,
    /** Remaining slow duration in seconds, or 0 when not slowed. */
    val slowRemaining: Float = 0f
)

/** One enemy group that spawns together. */
data class TdWave(
    val enemies: List<TdEnemy>,
    val autoStartDelayMs: Long
)

/** The static map for a tower-defense level. */
data class TdMap(
    /** Grid dimensions. */
    val cols: Int,
    val rows: Int,
    /** Ordered path tiles from spawn to base. */
    val path: List<GridPos>,
    /** Tiles on which towers may be placed. */
    val buildable: Set<GridPos>,
    val basePos: GridPos,
    val seed: Long
)

/** Full snapshot of a tower-defense game. */
data class TdGameState(
    val map: TdMap,
    val towers: List<Tower>,
    val enemies: List<TdEnemy>,
    val waves: List<TdWave>,
    val currentWave: Int,
    val money: Int,
    val lives: Int,
    val nextEnemyId: Int,
    val nextTowerId: Int
) {
    val wavesRemaining: Int get() = waves.size - currentWave
    val allWavesCleared: Boolean get() = currentWave >= waves.size && enemies.isEmpty()
    val defeated: Boolean get() = lives <= 0
}
