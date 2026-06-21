package com.xanticious.androidgames.games.asteroids

import com.xanticious.androidgames.controller.games.asteroids.AsteroidsController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.JoystickInput
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.asteroids.Asteroid
import com.xanticious.androidgames.model.games.asteroids.AsteroidSize
import com.xanticious.androidgames.model.games.asteroids.AsteroidsInput
import com.xanticious.androidgames.model.games.asteroids.AsteroidsMode
import com.xanticious.androidgames.model.games.asteroids.AsteroidsState
import com.xanticious.androidgames.model.games.asteroids.AsteroidsStepEvent
import com.xanticious.androidgames.model.games.asteroids.Beacon
import com.xanticious.androidgames.model.games.asteroids.Projectile
import com.xanticious.androidgames.model.games.asteroids.Ship
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class AsteroidsControllerTest {
    private val controller = AsteroidsController()
    private val config = controller.configFor(GameDifficulty.MEDIUM)
    private val seed = Random(42)

    private fun noInput() = AsteroidsInput(JoystickInput.NONE)

    // ── wrap ──────────────────────────────────────────────────────────────────

    @Test
    fun wrap_positionAboveOne_wrapsToZeroRange() {
        val result = controller.wrap(Vec2(1.1f, 0.5f))
        assertTrue(result.x < 1f && result.x >= 0f)
    }

    @Test
    fun wrap_negativePosition_wrapsToPositive() {
        val result = controller.wrap(Vec2(-0.1f, 0.5f))
        assertTrue(result.x >= 0f && result.x < 1f)
    }

    @Test
    fun wrap_exactlyOne_wrapsToZero() {
        val result = controller.wrap(Vec2(1.0f, 1.0f))
        assertEquals(0f, result.x, 1e-5f)
    }

    // ── wrappedDistance / wrappedDelta ────────────────────────────────────────

    @Test
    fun wrappedDistance_acrossLeftRightEdge_isShortDistance() {
        val dist = controller.wrappedDistance(Vec2(0.02f, 0.5f), Vec2(0.98f, 0.5f))
        assertTrue("wrapped dist should be ~0.04, was $dist", dist < 0.1f)
    }

    @Test
    fun wrappedDistance_acrossTopBottomEdge_isShortDistance() {
        val dist = controller.wrappedDistance(Vec2(0.5f, 0.02f), Vec2(0.5f, 0.98f))
        assertTrue("wrapped dist should be ~0.04, was $dist", dist < 0.1f)
    }

    @Test
    fun wrappedDelta_acrossEdge_takesShortWayAround() {
        val delta = controller.wrappedDelta(Vec2(0.98f, 0.5f), Vec2(0.02f, 0.5f))
        assertTrue("should move +x the short way, was ${delta.x}", delta.x in 0f..0.1f)
    }

    // ── nearestAsteroidDirection ──────────────────────────────────────────────

    @Test
    fun nearestAsteroidDirection_noAsteroids_returnsNull() {
        val dir = controller.nearestAsteroidDirection(Vec2(0.5f, 0.5f), emptyList())
        assertNull(dir)
    }

    @Test
    fun nearestAsteroidDirection_picksClosestTarget() {
        val near = Asteroid(1, Vec2(0.5f, 0.3f), Vec2.ZERO, AsteroidSize.SMALL, 1)
        val far = Asteroid(2, Vec2(0.9f, 0.9f), Vec2.ZERO, AsteroidSize.SMALL, 1)
        val dir = controller.nearestAsteroidDirection(Vec2(0.5f, 0.5f), listOf(near, far))
        assertNotNull(dir)
        assertTrue("should aim upward at the near asteroid, y=${dir!!.y}", dir.y < 0f)
        assertEquals(1f, dir.length, 0.01f)
    }

    // ── spawnAsteroids ────────────────────────────────────────────────────────

    @Test
    fun spawnAsteroids_level1_spawns6Asteroids() {
        val result = controller.spawnAsteroids(AsteroidsState.initial(), config, seed)
        assertEquals(6, result.asteroids.size)
    }

    @Test
    fun spawnAsteroids_allAsteroidsAreLarge() {
        val result = controller.spawnAsteroids(AsteroidsState.initial(), config, seed)
        assertTrue(result.asteroids.all { it.size == AsteroidSize.LARGE })
    }

    @Test
    fun spawnAsteroids_noneSpawnInSafeZone() {
        val result = controller.spawnAsteroids(AsteroidsState.initial(), config, seed)
        val tooClose = result.asteroids.any { it.position.distanceTo(Vec2(0.5f, 0.5f)) < 0.25f }
        assertFalse(tooClose)
    }

    @Test
    fun spawnAsteroids_level5_capsAt14Asteroids() {
        val state = AsteroidsState.initial().copy(level = 5)
        val result = controller.spawnAsteroids(state, config, seed)
        assertEquals(14, result.asteroids.size)
    }

    @Test
    fun spawnAsteroids_shipStartsWithInvincibility() {
        val result = controller.spawnAsteroids(AsteroidsState.initial(), config, seed)
        assertTrue(result.ship.isInvincible)
    }

    @Test
    fun spawnAsteroids_shipStartsMovingAtMinSpeed() {
        val result = controller.spawnAsteroids(AsteroidsState.initial(), config, seed)
        assertEquals(config.minShipSpeed, result.ship.velocity.length, 1e-4f)
    }

    // ── step: ship movement ───────────────────────────────────────────────────

    @Test
    fun step_accelerate_increasesShipSpeed() {
        val state = AsteroidsState.initial().copy(asteroids = emptyList())
        val before = state.ship.velocity.length
        val input = AsteroidsInput(JoystickInput(dx = 0f, dy = -1f))
        val result = controller.step(state, config, 0.1f, input)
        assertTrue(result.state.ship.velocity.length > before)
    }

    @Test
    fun step_noInput_shipNeverStopsBelowMinSpeed() {
        var state = AsteroidsState.initial().copy(asteroids = emptyList())
        repeat(30) { state = controller.step(state, config, 0.05f, noInput()).state }
        assertTrue(
            "ship should keep at least min speed, was ${state.ship.velocity.length}",
            state.ship.velocity.length >= config.minShipSpeed - 1e-3f
        )
    }

    @Test
    fun step_excessiveSpeed_clampedToMax() {
        val ship = Ship.initial().copy(velocity = Vec2(10f, 0f))
        val state = AsteroidsState.initial().copy(ship = ship, asteroids = emptyList())
        val result = controller.step(state, config, 0.1f, noInput())
        assertTrue(result.state.ship.velocity.length <= config.maxShipSpeed + 1e-3f)
    }

    @Test
    fun step_shipHeadingAutoAlignsToVelocity() {
        val ship = Ship.initial().copy(velocity = Vec2(0.3f, 0f)) // moving +x
        val state = AsteroidsState.initial().copy(ship = ship, asteroids = emptyList())
        val result = controller.step(state, config, 0.05f, noInput())
        assertTrue("angle should align toward 0, was ${result.state.ship.angle}", abs(result.state.ship.angle) < 0.1f)
    }

    @Test
    fun step_shipWrapsMovesPastRightEdge() {
        val ship = Ship.initial().copy(position = Vec2(0.98f, 0.5f), velocity = Vec2(0.5f, 0f))
        val state = AsteroidsState.initial().copy(ship = ship, asteroids = emptyList())
        val result = controller.step(state, config, 0.1f, noInput())
        assertTrue("should wrap to left side", result.state.ship.position.x < 0.5f)
    }

    // ── step: autofire ────────────────────────────────────────────────────────

    @Test
    fun step_asteroidPresent_autofiresProjectile() {
        val asteroid = Asteroid(1, Vec2(0.5f, 0.2f), Vec2.ZERO, AsteroidSize.SMALL, 1)
        val state = AsteroidsState.initial().copy(
            ship = Ship.initial().copy(invincibilityTimer = 10f),
            asteroids = listOf(asteroid)
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(1, result.state.projectiles.size)
    }

    @Test
    fun step_noAsteroids_noProjectileSpawned() {
        val state = AsteroidsState.initial().copy(asteroids = emptyList())
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(0, result.state.projectiles.size)
    }

    @Test
    fun step_recentlyFired_doesNotFireAgainDuringCooldown() {
        val asteroid = Asteroid(1, Vec2(0.5f, 0.2f), Vec2.ZERO, AsteroidSize.SMALL, 1)
        val state = AsteroidsState.initial().copy(
            ship = Ship.initial().copy(invincibilityTimer = 10f),
            asteroids = listOf(asteroid),
            fireCooldown = config.fireInterval
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(0, result.state.projectiles.size)
    }

    // ── step: collision detection ─────────────────────────────────────────────

    @Test
    fun step_projectileHitsAsteroid_destroysProjectile() {
        val asteroid = Asteroid(1, Vec2(0.5f, 0.4f), Vec2.ZERO, AsteroidSize.SMALL, 1)
        val proj = Projectile(id = 2, position = Vec2(0.5f, 0.41f), velocity = Vec2(0f, -0.5f), age = 0f)
        val state = AsteroidsState.initial().copy(
            ship = Ship.initial().copy(invincibilityTimer = 10f),
            asteroids = listOf(asteroid),
            projectiles = listOf(proj),
            nextId = 3,
            fireCooldown = 5f
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(0, result.state.projectiles.size)
    }

    @Test
    fun step_projectileKillsSmallAsteroid_awardsScore() {
        val asteroid = Asteroid(1, Vec2(0.5f, 0.4f), Vec2.ZERO, AsteroidSize.SMALL, 1)
        val proj = Projectile(id = 2, position = Vec2(0.5f, 0.41f), velocity = Vec2(0f, -0.5f), age = 0f)
        val state = AsteroidsState.initial().copy(
            ship = Ship.initial().copy(invincibilityTimer = 10f),
            asteroids = listOf(asteroid),
            projectiles = listOf(proj),
            nextId = 3,
            fireCooldown = 5f
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(100, result.state.score)
    }

    @Test
    fun step_projectileKillsAsteroid_incrementsDestroyedCount() {
        val asteroid = Asteroid(1, Vec2(0.5f, 0.4f), Vec2.ZERO, AsteroidSize.SMALL, 1)
        val proj = Projectile(id = 2, position = Vec2(0.5f, 0.41f), velocity = Vec2(0f, -0.5f), age = 0f)
        val state = AsteroidsState.initial().copy(
            ship = Ship.initial().copy(invincibilityTimer = 10f),
            asteroids = listOf(asteroid),
            projectiles = listOf(proj),
            nextId = 3,
            fireCooldown = 5f
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(1, result.state.asteroidsDestroyed)
    }

    @Test
    fun step_projectileKillsLargeAsteroid_splitsTwoMedium() {
        val asteroid = Asteroid(1, Vec2(0.5f, 0.4f), Vec2(0.1f, 0f), AsteroidSize.LARGE, 1)
        val proj = Projectile(id = 2, position = Vec2(0.5f, 0.41f), velocity = Vec2(0f, -0.5f), age = 0f)
        val state = AsteroidsState.initial().copy(
            ship = Ship.initial().copy(invincibilityTimer = 10f),
            asteroids = listOf(asteroid),
            projectiles = listOf(proj),
            nextId = 3,
            fireCooldown = 5f
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(2, result.state.asteroids.size)
        assertTrue(result.state.asteroids.all { it.size == AsteroidSize.MEDIUM })
    }

    // ── step: ship damage / teleport ──────────────────────────────────────────

    @Test
    fun step_shipHitsAsteroid_emitsPlayerHitEvent() {
        val asteroid = Asteroid(1, Vec2(0.5f, 0.5f), Vec2.ZERO, AsteroidSize.LARGE, 3)
        val state = AsteroidsState.initial().copy(
            ship = Ship.initial().copy(invincibilityTimer = 0f),
            asteroids = listOf(asteroid)
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(AsteroidsStepEvent.PLAYER_HIT, result.event)
    }

    @Test
    fun step_classicShipHitsAsteroid_decrementsLives() {
        val asteroid = Asteroid(1, Vec2(0.5f, 0.5f), Vec2.ZERO, AsteroidSize.LARGE, 3)
        val state = AsteroidsState.initial(AsteroidsMode.Classic).copy(
            ship = Ship.initial().copy(invincibilityTimer = 0f),
            asteroids = listOf(asteroid)
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(2, result.state.lives)
    }

    @Test
    fun step_shipHit_teleportsToSafetyAndFreezes() {
        val asteroids = listOf(
            Asteroid(1, Vec2(0.5f, 0.5f), Vec2.ZERO, AsteroidSize.LARGE, 3),
            Asteroid(2, Vec2(0.55f, 0.5f), Vec2.ZERO, AsteroidSize.LARGE, 3)
        )
        val state = AsteroidsState.initial().copy(
            ship = Ship.initial().copy(invincibilityTimer = 0f),
            asteroids = asteroids
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertTrue("freeze should be active", result.state.freezeTimer > 0f)
        assertTrue("ship should be invincible after teleport", result.state.ship.isInvincible)
        val gap = result.state.asteroids.minOf {
            controller.wrappedDistance(result.state.ship.position, it.position)
        }
        assertTrue("ship should teleport clear of asteroids, gap=$gap", gap > Ship.RADIUS + Asteroid.RADIUS_LARGE)
    }

    @Test
    fun step_shipHit_clampsVelocityToMinSpeed() {
        val asteroid = Asteroid(1, Vec2(0.5f, 0.5f), Vec2.ZERO, AsteroidSize.LARGE, 3)
        val state = AsteroidsState.initial().copy(
            ship = Ship.initial().copy(velocity = Vec2(0.5f, 0f), invincibilityTimer = 0f),
            asteroids = listOf(asteroid)
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(config.minShipSpeed, result.state.ship.velocity.length, 1e-3f)
    }

    @Test
    fun step_infiniteLivesShipHit_doesNotDecrementLives() {
        val asteroid = Asteroid(1, Vec2(0.5f, 0.5f), Vec2.ZERO, AsteroidSize.LARGE, 3)
        val state = AsteroidsState.initial(AsteroidsMode.TimeChallenge(60)).copy(
            ship = Ship.initial().copy(invincibilityTimer = 0f),
            asteroids = listOf(asteroid)
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(AsteroidsStepEvent.PLAYER_HIT, result.event)
        assertEquals(AsteroidsState.INITIAL_LIVES, result.state.lives)
    }

    @Test
    fun step_invincibleShip_doesNotDieFromAsteroid() {
        val asteroid = Asteroid(1, Vec2(0.5f, 0.5f), Vec2.ZERO, AsteroidSize.LARGE, 3)
        val state = AsteroidsState.initial().copy(
            ship = Ship.initial().copy(invincibilityTimer = 5f),
            asteroids = listOf(asteroid)
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(AsteroidsStepEvent.NONE, result.event)
        assertEquals(3, result.state.lives)
    }

    @Test
    fun step_frozen_asteroidsDoNotDrift() {
        val asteroid = Asteroid(1, Vec2(0.5f, 0.2f), Vec2(0.3f, 0f), AsteroidSize.LARGE, 3)
        val state = AsteroidsState.initial().copy(
            ship = Ship.initial().copy(invincibilityTimer = 10f),
            asteroids = listOf(asteroid),
            freezeTimer = 1f
        )
        val result = controller.step(state, config, 0.1f, noInput())
        assertEquals(0.5f, result.state.asteroids.first().position.x, 1e-4f)
    }

    // ── step: timers & modes ──────────────────────────────────────────────────

    @Test
    fun step_advancesElapsedTime() {
        val state = AsteroidsState.initial().copy(asteroids = emptyList())
        val result = controller.step(state, config, 0.1f, noInput())
        assertEquals(0.1f, result.state.elapsedTime, 1e-4f)
    }

    @Test
    fun step_timeChallengeExpires_emitsTimeExpired() {
        val state = AsteroidsState.initial(AsteroidsMode.TimeChallenge(1)).copy(
            ship = Ship.initial().copy(invincibilityTimer = 10f),
            asteroids = emptyList(),
            elapsedTime = 0.99f
        )
        val result = controller.step(state, config, 0.1f, noInput())
        assertEquals(AsteroidsStepEvent.TIME_EXPIRED, result.event)
    }

    // ── step: beacon collection ───────────────────────────────────────────────

    @Test
    fun step_shipCollectsBeacon_awardsBonusScore() {
        val beacon = Beacon(Vec2(0.5f, 0.5f))
        val state = AsteroidsState.initial().copy(
            ship = Ship.initial().copy(invincibilityTimer = 0f),
            asteroids = emptyList(),
            beacon = beacon,
            beaconsCollectedThisLevel = 0
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(AsteroidsStepEvent.BEACON_COLLECTED, result.event)
        assertTrue("score should include beacon bonus", result.state.score >= 600)
    }

    @Test
    fun step_collectFifthBeacon_emitsAllBeaconsCollected() {
        val beacon = Beacon(Vec2(0.5f, 0.5f))
        val state = AsteroidsState.initial().copy(
            ship = Ship.initial().copy(invincibilityTimer = 0f),
            asteroids = emptyList(),
            beacon = beacon,
            beaconsCollectedThisLevel = 4
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(AsteroidsStepEvent.ALL_BEACONS_COLLECTED, result.event)
    }

    // ── advanceLevel ──────────────────────────────────────────────────────────

    @Test
    fun advanceLevel_incrementsLevelByOne() {
        val result = controller.advanceLevel(AsteroidsState.initial())
        assertEquals(2, result.level)
    }
}
