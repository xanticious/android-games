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
    val age: Float
) {
    companion object {
        const val RADIUS = 0.008f
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
        val INITIAL_ANGLE: Float = (-PI / 2.0).toFloat()

        fun initial(): Ship = Ship(
            position = Vec2(0.5f, 0.5f),
            velocity = Vec2.ZERO,
            angle = INITIAL_ANGLE,
            invincibilityTimer = 0f
        )
    }
}

data class AsteroidsState(
    val ship: Ship,
    val asteroids: List<Asteroid>,
    val projectiles: List<Projectile>,
    val beacon: Beacon?,
    val beaconsCollectedThisLevel: Int,
    val beaconSpawnTimer: Float,
    val lives: Int,
    val score: Int,
    val level: Int,
    val nextId: Int
) {
    companion object {
        const val BEACONS_PER_LEVEL = 5
        const val INITIAL_LIVES = 3

        fun initial(): AsteroidsState = AsteroidsState(
            ship = Ship.initial(),
            asteroids = emptyList(),
            projectiles = emptyList(),
            beacon = null,
            beaconsCollectedThisLevel = 0,
            beaconSpawnTimer = 10f,
            lives = INITIAL_LIVES,
            score = 0,
            level = 1,
            nextId = 0
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
    val rotationSpeed: Float = 2.5f,
    val thrustForce: Float = 0.3f,
    val maxShipSpeed: Float = 0.5f,
    val dampening: Float = 0.15f,
    val invincibilityDuration: Float = 2.0f,
    val beaconSpawnDelay: Float = 10f,
    val beaconExplosionRadius: Float = 0.25f
)

/** Per-frame input fed into the controller. */
data class AsteroidsInput(
    val joystick: JoystickInput,
    /** Normalized [0,1] position of a fire tap; null means no tap this frame. */
    val fireTapNormalized: Vec2?
)

enum class AsteroidsStepEvent { NONE, PLAYER_HIT, BEACON_COLLECTED, ALL_BEACONS_COLLECTED }

data class AsteroidsStep(
    val state: AsteroidsState,
    val event: AsteroidsStepEvent
)
