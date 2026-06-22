package com.xanticious.androidgames.model.games.helicopterdogfight

import com.xanticious.androidgames.model.Vec2

// ---------------------------------------------------------------------------
// Enumerations
// ---------------------------------------------------------------------------

enum class HeliEnemyType(val maxHp: Int, val scoreValue: Int) {
    GROUND_TURRET(3, 100),
    AA_GUN(2, 75),
    ENEMY_HELI(4, 150)
}

enum class HeliProjectileOwner { PLAYER, ENEMY }

// ---------------------------------------------------------------------------
// Component data classes
// ---------------------------------------------------------------------------

data class HeliEnemy(
    val id: Int,
    val type: HeliEnemyType,
    val position: Vec2,
    /** For ENEMY_HELI: non-zero y-velocity that reverses at bounds. Others: Vec2.ZERO. */
    val velocity: Vec2,
    val hp: Int,
    val fireCooldown: Float
)

data class HeliProjectile(
    val id: Int,
    val position: Vec2,
    val velocity: Vec2,
    val owner: HeliProjectileOwner
)

// ---------------------------------------------------------------------------
// Top-level game state (immutable)
// ---------------------------------------------------------------------------

/**
 * Normalized coordinates: x and y in [0,1]. y=0 is top, y=1 is bottom.
 * Player helicopter occupies the left portion of the screen (x ≤ MAX_PLAYER_X).
 * Enemies are placed in the right portion (x > 0.5).
 */
data class HelicopterDogfightState(
    val playerPos: Vec2,
    val playerHp: Int,
    val playerLives: Int,
    val invincibilityTimer: Float,
    val autoFireTimer: Float,
    val enemies: List<HeliEnemy>,
    val projectiles: List<HeliProjectile>,
    val score: Int,
    val screenNumber: Int,
    val noHitCurrentScreen: Boolean,
    val nextProjectileId: Int,
    val nextEnemyId: Int
) {
    val isInvincible: Boolean get() = invincibilityTimer > 0f

    companion object {
        const val PLAYER_HALF_WIDTH = 0.05f
        const val PLAYER_HALF_HEIGHT = 0.04f
        const val MIN_PLAYER_X = 0.05f
        const val MAX_PLAYER_X = 0.40f
        const val MIN_PLAYER_Y = 0.05f
        const val MAX_PLAYER_Y = 0.88f

        fun initial(): HelicopterDogfightState = HelicopterDogfightState(
            playerPos = Vec2(0.15f, 0.5f),
            playerHp = 5,
            playerLives = 3,
            invincibilityTimer = 0f,
            autoFireTimer = 0f,
            enemies = emptyList(),
            projectiles = emptyList(),
            score = 0,
            screenNumber = 1,
            noHitCurrentScreen = true,
            nextProjectileId = 0,
            nextEnemyId = 0
        )
    }
}

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------

data class HelicopterDogfightConfig(
    val playerSpeed: Float,
    val playerProjectileSpeed: Float,
    val autoFireInterval: Float,
    val enemyProjectileSpeed: Float,
    val enemyFireInterval: Float,
    /** Base number of enemies spawned per screen (scales with screen number). */
    val enemyCountPerScreen: Int,
    val invincibilityDuration: Float = 2.0f,
    val allClearDuration: Float = 1.5f,
    val scrollDuration: Float = 1.0f,
    val respawnDuration: Float = 2.0f
)

// ---------------------------------------------------------------------------
// Per-frame input
// ---------------------------------------------------------------------------

data class HelicopterDogfightInput(
    val joystickDx: Float,
    val joystickDy: Float
)

// ---------------------------------------------------------------------------
// Step result
// ---------------------------------------------------------------------------

sealed interface HelicopterDogfightEvent {
    data object None : HelicopterDogfightEvent
    data class EnemyDestroyed(val type: HeliEnemyType, val points: Int) : HelicopterDogfightEvent
    data object AllEnemiesDestroyed : HelicopterDogfightEvent
    /** Player took damage but HP > 0. Triggers brief invincibility flash. */
    data object PlayerHit : HelicopterDogfightEvent
    /** Player HP reached 0 and a life is deducted; lives > 0 remain. Triggers respawn. */
    data object PlayerCrashed : HelicopterDogfightEvent
    /** Player HP reached 0 and lives are exhausted. Game over. */
    data object GameOver : HelicopterDogfightEvent
}

data class HelicopterDogfightStep(
    val state: HelicopterDogfightState,
    val events: List<HelicopterDogfightEvent>
)
