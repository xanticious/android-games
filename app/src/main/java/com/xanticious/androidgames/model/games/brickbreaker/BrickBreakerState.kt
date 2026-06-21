package com.xanticious.androidgames.model.games.brickbreaker

import com.xanticious.androidgames.model.Vec2

/** Which of the four brick-breaker variants is active. */
enum class BrickBreakerVariant {
    /** Turn-based: bottom paddle, all bricks must be cleared per level. */
    CLASSIC,
    /** Real-time: bottom paddle auto-fires 3-ball spread, lives system, bricks descend. */
    ARCADE,
    /** Turn-based: left-side cannon, parabolic arc, destroy target bricks within turn limit. */
    CANNON,
    /** Real-time: left-side cannon, timer countdown, destroy target bricks. */
    CANNON_ARCADE
}

/** Brick categories with distinct behaviour. */
enum class BrickType {
    STANDARD,  // counts toward clear condition; HP scales with level
    POWERUP,   // same as STANDARD but drops a power-up when destroyed
    STEEL,     // immune to standard hits; requires PowerShot or 2× listed HP
    TARGET     // must all be destroyed to win (CANNON / CANNON_ARCADE)
}

/** All collectible modifiers across the four variants. */
enum class PowerUpType {
    EXPLODE,       // instant: destroy bricks adjacent to the collection point
    MULTI_SHOT,    // timed: 3 balls per tick (CLASSIC)
    POWER_SHOT,    // timed: double damage
    CLEAR_SCREEN,  // instant: destroy all STANDARD bricks on screen
    WIDE_SHOT,     // timed: larger ball radius
    RAPID_FIRE,    // timed: shorter auto-fire interval (ARCADE)
    TIME_BONUS,    // instant: +10 s to countdown (CANNON_ARCADE)
    EXTRA_BALL,    // instant: +1 ball to the player's ball count
    EXTRA_STRENGTH // instant: +1 to the player's damage multiplier
}

/** A single brick cell in the grid. */
data class Brick(
    val col: Int,
    val row: Int,
    val hp: Int,
    val maxHp: Int,
    val type: BrickType,
    val powerUp: PowerUpType? = null
)

/** An in-flight ball.  [bounces] tracks wall/floor contacts for cannon variants. */
data class Ball(
    val pos: Vec2,
    val vel: Vec2,
    val bounces: Int = 0
)

/**
 * A power-up icon drifting downward after its container brick was destroyed.
 * [age] ticks up each frame and the icon disappears after 3 s if uncollected.
 */
data class DroppingPowerUp(
    val type: PowerUpType,
    val pos: Vec2,
    val vel: Vec2 = Vec2(0f, 0.10f),
    val age: Float = 0f
)

/** A currently active timed power-up effect. */
data class ActivePowerUp(
    val type: PowerUpType,
    val remainingSeconds: Float
)

/** Canonical normalized field constants shared by controller and views. */
object BrickField {
    const val COLS = 8
    const val ROWS = 10
    const val TOP_MARGIN = 0.04f
    const val ROW_HEIGHT = 0.072f
    const val BRICK_PAD = 0.006f
    const val CANNON_Y = 0.90f    // classic/arcade: launch Y position
    const val CANNON_X = 0.04f    // cannon variants: launch X position
    const val BALL_RADIUS = 0.016f
    const val POWERUP_RADIUS = 0.025f
    const val POWERUP_MAX_AGE = 3f

    /** CLASSIC: a level's row target is the level number, capped here. */
    const val CLASSIC_MAX_ROWS = 20
    /** CLASSIC: how many rows are on screen at the start of a level. */
    const val CLASSIC_VISIBLE_ROWS = 6
}

/**
 * Complete immutable game state.  All four variants share this type; fields
 * unused by a variant default to neutral values.
 */
data class BrickBreakerState(
    // ---- shared ----
    val bricks: List<Brick> = emptyList(),
    val balls: List<Ball> = emptyList(),
    val score: Int = 0,
    val level: Int = 1,
    val activePowerUps: List<ActivePowerUp> = emptyList(),
    val droppingPowerUps: List<DroppingPowerUp> = emptyList(),

    // Player ball bank and damage multiplier (CLASSIC / ARCADE).
    val ballCount: Int = 20,         // total balls available; grows via EXTRA_BALL
    val strength: Int = 1,           // damage multiplier; grows via EXTRA_STRENGTH

    // ---- CLASSIC / ARCADE (bottom paddle) ----
    val paddleX: Float = 0.5f,       // normalized x-centre of the cannon
    val ballsToFire: Int = 0,        // remaining balls in the classic volley
    val fireTimer: Float = 0f,       // countdown to next auto/volley ball launch
    val turnsPlayed: Int = 0,        // CLASSIC: turns elapsed

    // ---- ARCADE ----
    val lives: Int = 3,
    val descentOffset: Float = 0f,   // fractional row offset (0..1) accumulated each frame
    val nextRowTimer: Float = 0f,    // time until the next top-row is added
    val rowsGenerated: Int = 0,      // rows generated so far this level
    val totalRowsForLevel: Int = 12,

    // ---- CANNON / CANNON_ARCADE (left-side cannon) ----
    val cannonAngleDeg: Float = 45f, // degrees above horizontal; 5..85 for CANNON
    val turnsLeft: Int = 10,         // CANNON: turns remaining
    val timerSeconds: Float = 60f,   // CANNON_ARCADE: countdown
    val fireCooldown: Float = 0f,    // CANNON_ARCADE: time until next shot allowed
) {
    val hasMultiShot: Boolean get() = activePowerUps.any { it.type == PowerUpType.MULTI_SHOT }
    val hasPowerShot: Boolean get() = activePowerUps.any { it.type == PowerUpType.POWER_SHOT }
    val hasWideShot: Boolean get() = activePowerUps.any { it.type == PowerUpType.WIDE_SHOT }
    val hasRapidFire: Boolean get() = activePowerUps.any { it.type == PowerUpType.RAPID_FIRE }
}

/** Difficulty-tuned constants for one variant. */
data class BrickBreakerConfig(
    val variant: BrickBreakerVariant,
    // physics
    val ballSpeed: Float,
    val gravity: Float = 0f,         // CANNON variants: positive downward
    val maxBounces: Int = 20,        // CANNON variants: 2
    // classic
    val volleySize: Int = 20,
    val volleyInterval: Float = 0.1f,
    // arcade
    val descentSpeed: Float,         // rows per second the grid sinks
    val autoFireInterval: Float,     // seconds between auto-volleys
    val newRowInterval: Float,       // seconds between new top-rows
    val livesStart: Int = 3,
    val totalRowsForLevel: Int = 12,
    // cannon / cannon-arcade
    val startingTurns: Int = 10,
    val startTimerSeconds: Float = 60f,
    val shotCooldown: Float = 0.5f,
    // damage
    val baseDamage: Int = 1,
    // scoring
    val scorePerHpUnit: Int = 10,    // × maxHp for standard bricks
    val powerUpBrickMultiplier: Float = 1.5f,
    val steelBrickMultiplier: Float = 2f,
    val levelClearBonus: Int = 500,
    val clearScreenBonusPerBrick: Int = 200,
    // power-up durations
    val multiShotDuration: Float = 15f,
    val powerShotDuration: Float = 20f,
    val wideShotDuration: Float = 15f,
    val rapidFireDuration: Float = 10f,
    val timeBonusSeconds: Float = 10f,
    val wideShotRadiusMultiplier: Float = 2f,
    val rapidFireMultiplier: Float = 0.5f,
)

/** Per-frame player intent fed into the controller. */
data class BrickBreakerInput(
    val paddleX: Float = 0.5f,
    val aimAngleDeg: Float = 45f,   // CANNON variants: degrees above horizontal
    val fireRequested: Boolean = false
)

/** Result of one controller step. */
data class BrickBreakerStepResult(
    val state: BrickBreakerState,
    val allBallsDone: Boolean = false,
    val brickReachedBottom: Boolean = false,
    val allTargetsCleared: Boolean = false,
    val fieldCleared: Boolean = false,
    val timerExpired: Boolean = false,
    val levelRowsCleared: Boolean = false,
    val livesGone: Boolean = false,
    /** CLASSIC: no bricks remain on screen (row >= 0); off-screen rows may linger. */
    val visibleCleared: Boolean = false,
)
