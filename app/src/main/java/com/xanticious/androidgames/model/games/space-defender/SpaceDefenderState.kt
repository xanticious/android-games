package com.xanticious.androidgames.model.games.spacedefender

import com.xanticious.androidgames.model.Vec2

// ---------------------------------------------------------------------------
// Enumerations
// ---------------------------------------------------------------------------

enum class EnemyType(val maxHp: Int, val scoreValue: Int) {
    SCOUT(3, 100),
    GUNNER(5, 200),
    SNIPER(4, 250),
    BOMBER(8, 350),
    COMMANDER(12, 600)
}

enum class EnemyBehavior { FLANKER, BAITER, NEUTRAL }

enum class ProjectileOwner { PLAYER, ENEMY }

enum class ShieldHealth { FULL, CRACKED, BROKEN }

// ---------------------------------------------------------------------------
// Component data classes
// ---------------------------------------------------------------------------

data class Enemy(
    val id: Int,
    val type: EnemyType,
    val position: Vec2,
    val velocity: Vec2,
    val hp: Int,
    val behavior: EnemyBehavior,
    val fireCooldown: Float,
    val erraticTimer: Float = 0f
)

data class Projectile(
    val id: Int,
    val position: Vec2,
    val velocity: Vec2,
    val owner: ProjectileOwner,
    /** True for homing shots from Snipers. */
    val homing: Boolean = false,
    /** Target position for homing shots (snapped at fire time). */
    val homingTarget: Vec2 = Vec2.ZERO
)

data class Shield(
    val id: Int,
    val position: Vec2,
    val health: ShieldHealth
)

// ---------------------------------------------------------------------------
// Top-level game state (immutable)
// ---------------------------------------------------------------------------

/**
 * Normalized coordinates: x and y in [0,1]. Y=0 is top, Y=1 is bottom.
 * The player cannon slides along y ≈ 0.88.
 */
data class SpaceDefenderState(
    val playerX: Float,
    val lives: Int,
    val score: Int,
    val wave: Int,
    val enemies: List<Enemy>,
    val projectiles: List<Projectile>,
    val shields: List<Shield>,
    val autoFireTimer: Float,
    val invincibilityTimer: Float,
    val waveIntroTimer: Float,
    val backToBackNoHit: Boolean,
    val scoreMultiplier: Float,
    val noHitCurrentWave: Boolean,
    val nextProjectileId: Int,
    val nextEnemyId: Int
) {
    val isInvincible: Boolean get() = invincibilityTimer > 0f

    companion object {
        const val PLAYER_Y = 0.88f
        const val PLAYER_HALF_WIDTH = 0.04f

        fun initial(): SpaceDefenderState = SpaceDefenderState(
            playerX = 0.5f,
            lives = 3,
            score = 0,
            wave = 1,
            enemies = emptyList(),
            projectiles = emptyList(),
            shields = emptyList(),
            autoFireTimer = 0f,
            invincibilityTimer = 0f,
            waveIntroTimer = 0f,
            backToBackNoHit = false,
            scoreMultiplier = 1f,
            noHitCurrentWave = true,
            nextProjectileId = 0,
            nextEnemyId = 0
        )
    }
}

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------

data class SpaceDefenderConfig(
    val playerSpeed: Float,
    val playerProjectileSpeed: Float,
    val autoFireInterval: Float,
    val enemyBaseSpeed: Float,
    val enemyProjectileSpeed: Float,
    val enemyFireInterval: Float,
    val invincibilityDuration: Float = 1.5f,
    val waveIntroDuration: Float = 2.0f
)

// ---------------------------------------------------------------------------
// Per-frame input
// ---------------------------------------------------------------------------

data class SpaceDefenderInput(
    val joystickDx: Float
)

// ---------------------------------------------------------------------------
// Step result
// ---------------------------------------------------------------------------

sealed interface SpaceDefenderEvent {
    data object None : SpaceDefenderEvent
    data class EnemyDestroyed(val enemyType: EnemyType, val points: Int) : SpaceDefenderEvent
    data object AllEnemiesDestroyed : SpaceDefenderEvent
    data object PlayerHit : SpaceDefenderEvent
    data object GameOver : SpaceDefenderEvent
}

data class SpaceDefenderStep(
    val state: SpaceDefenderState,
    val events: List<SpaceDefenderEvent>
)
