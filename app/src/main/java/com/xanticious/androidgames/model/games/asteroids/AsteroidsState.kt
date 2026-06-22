package com.xanticious.androidgames.model.games.asteroids

import com.xanticious.androidgames.model.JoystickInput
import com.xanticious.androidgames.model.Vec2
import kotlin.math.PI

enum class AsteroidSize { LARGE, MEDIUM, SMALL }

data class Asteroid(
    val id: Int,
    val position: Vec2,
    val velocity: Vec2,
    val size: AsteroidSize,
    val hp: Int
) {
    companion object {
        fun hpFor(size: AsteroidSize): Int = when (size) {
            AsteroidSize.LARGE -> 3
            AsteroidSize.MEDIUM -> 2
            AsteroidSize.SMALL -> 1
        }

        fun radiusFor(size: AsteroidSize): Float = when (size) {
            AsteroidSize.LARGE -> RADIUS_LARGE
            AsteroidSize.MEDIUM -> RADIUS_MEDIUM
            AsteroidSize.SMALL -> RADIUS_SMALL
        }

        const val RADIUS_LARGE = 0.07f
        const val RADIUS_MEDIUM = 0.045f
        const val RADIUS_SMALL = 0.025f
    }
}

data class Projectile(
    val id: Int,
    val position: Vec2,
    val velocity: Vec2,
    val age: Float,
    /** Total distance flown so far; bullets vanish once this exceeds [AsteroidsConfig.projectileMaxDistance]. */
    val distanceTraveled: Float = 0f
) {
    companion object {
        /** Larger, visually prominent bullets. */
        const val RADIUS = 0.016f
    }
}

data class Beacon(val position: Vec2) {
    companion object {
        const val RADIUS = 0.03f
    }
}

data class Ship(
    val position: Vec2,
    val velocity: Vec2,
    val angle: Float,
    val invincibilityTimer: Float
) {
    val isInvincible: Boolean get() = invincibilityTimer > 0f

    companion object {
        const val RADIUS = 0.025f

        /**
         * Collision radius — smaller than the render [RADIUS] so the hit boundary
         * hugs the sparse triangular ship sprite instead of its bounding circle.
         */
        const val COLLISION_RADIUS = 0.014f
        val INITIAL_ANGLE: Float = (-PI / 2.0).toFloat()

        /** Default floor speed used when no config-driven value is supplied. */
        const val DEFAULT_MIN_SPEED = 0.06f

        /** Ship always starts drifting at [minSpeed] in the initial heading. */
        fun initial(minSpeed: Float = DEFAULT_MIN_SPEED): Ship = Ship(
            position = Vec2(0.5f, 0.5f),
            velocity = Vec2.fromAngle(INITIAL_ANGLE) * minSpeed,
            angle = INITIAL_ANGLE,
            invincibilityTimer = 0f
        )
    }
}

/**
 * Selectable game modes.
 *
 * - [Classic]: survive as many levels as possible starting with [AsteroidsState.INITIAL_LIVES] lives.
 * - [LevelChallenge]: complete [targetLevels] levels as fast as possible; count-up timer, infinite lives.
 * - [TimeChallenge]: destroy as many asteroids as possible before [durationSeconds] elapse;
 *   count-down timer, infinite lives.
 */
sealed interface AsteroidsMode {
    data object Classic : AsteroidsMode
    data class LevelChallenge(val targetLevels: Int) : AsteroidsMode
    data class TimeChallenge(val durationSeconds: Int) : AsteroidsMode

    /** True for modes that show a timer (and infinite lives) instead of hearts. */
    val hasTimer: Boolean get() = this !is Classic

    /** True for modes where the player never loses lives on damage. */
    val infiniteLives: Boolean get() = this !is Classic
}

/** Where the acceleration knob is anchored on screen. */
enum class KnobPlacement { LEFT_FIXED, RIGHT_FIXED, FLOATING }

data class AsteroidsState(
    val ship: Ship,
    val asteroids: List<Asteroid>,
    val projectiles: List<Projectile>,
    val beacon: Beacon?,
    val beaconsCollectedThisLevel: Int,
    val lives: Int,
    val score: Int,
    val level: Int,
    val nextId: Int,
    val mode: AsteroidsMode,
    /** Seconds of play elapsed; never paused, even during the damage freeze. */
    val elapsedTime: Float,
    val asteroidsDestroyed: Int,
    /** While > 0, asteroids are frozen for the teleport/damage animation. */
    val freezeTimer: Float,
    /** Counts down to the next autofire shot. */
    val fireCooldown: Float
) {
    companion object {
        const val BEACONS_PER_LEVEL = 5
        const val INITIAL_LIVES = 3

        fun initial(mode: AsteroidsMode = AsteroidsMode.Classic): AsteroidsState = AsteroidsState(
            ship = Ship.initial(),
            asteroids = emptyList(),
            projectiles = emptyList(),
            beacon = null,
            beaconsCollectedThisLevel = 0,
            lives = INITIAL_LIVES,
            score = 0,
            level = 1,
            nextId = 0,
            mode = mode,
            elapsedTime = 0f,
            asteroidsDestroyed = 0,
            freezeTimer = 0f,
            fireCooldown = 0f
        )
    }
}

/** Tuning values derived from difficulty. */
data class AsteroidsConfig(
    val asteroidBaseSpeed: Float,
    val asteroidSpeedGainPerLevel: Float = 0.125f,
    val asteroidMaxSpeedMultiplier: Float = 1.5f,
    val projectileSpeed: Float = 0.8f,
    val projectileLifetime: Float = 1.5f,
    /** Bullets are removed once they have flown this far across the field. */
    val projectileMaxDistance: Float = 0.6f,
    /** Seconds between autofire shots. */
    val fireInterval: Float = 0.35f,
    /** Acceleration applied per second at full knob deflection. */
    val accelerationForce: Float = 0.45f,
    val minShipSpeed: Float = 0.06f,
    val maxShipSpeed: Float = 0.6f,
    val dampening: Float = 0.1f,
    val invincibilityDuration: Float = 2.0f,
    /** Asteroids stay frozen this long after the player takes damage. */
    val freezeDuration: Float = 2.0f,
    val beaconExplosionRadius: Float = 0.25f
)

/** Per-frame input fed into the controller. */
data class AsteroidsInput(
    val joystick: JoystickInput
)

enum class AsteroidsStepEvent {
    NONE,
    PLAYER_HIT,
    BEACON_COLLECTED,
    ALL_BEACONS_COLLECTED,
    TIME_EXPIRED
}

data class AsteroidsStep(
    val state: AsteroidsState,
    val event: AsteroidsStepEvent
)
