package com.xanticious.androidgames.games.asteroids

import com.xanticious.androidgames.controller.games.asteroids.AsteroidsController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.JoystickInput
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.asteroids.Asteroid
import com.xanticious.androidgames.model.games.asteroids.AsteroidSize
import com.xanticious.androidgames.model.games.asteroids.AsteroidsInput
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
import kotlin.random.Random

class AsteroidsControllerTest {
    private val controller = AsteroidsController()
    private val config = controller.configFor(GameDifficulty.MEDIUM)
    private val seed = Random(42)

    private fun noInput() = AsteroidsInput(JoystickInput.NONE, null)

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

    // ── wrappedDistance ───────────────────────────────────────────────────────

    @Test
    fun wrappedDistance_acrossLeftRightEdge_isShortDistance() {
        val a = Vec2(0.02f, 0.5f)
        val b = Vec2(0.98f, 0.5f)
        val dist = controller.wrappedDistance(a, b)
        assertTrue("wrapped dist should be ~0.04, was $dist", dist < 0.1f)
    }

    @Test
    fun wrappedDistance_acrossTopBottomEdge_isShortDistance() {
        val a = Vec2(0.5f, 0.02f)
        val b = Vec2(0.5f, 0.98f)
        val dist = controller.wrappedDistance(a, b)
        assertTrue("wrapped dist should be ~0.04, was $dist", dist < 0.1f)
    }

    // ── computeFireDirection ──────────────────────────────────────────────────

    @Test
    fun computeFireDirection_noTap_returnsNull() {
        val ship = Ship.initial()
        val result = controller.computeFireDirection(ship, null)
        assertNull(result)
    }

    @Test
    fun computeFireDirection_tapWithinCone_firesInTapDirection() {
        // Ship points up (angle = -PI/2); tap directly above ship (in cone)
        val ship = Ship.initial()  // at (0.5, 0.5) pointing up
        val tap = Vec2(0.5f, 0.3f)  // directly above → within cone
        val dir = controller.computeFireDirection(ship, tap)
        assertNotNull(dir)
        assertTrue("should fire mostly upward, y=${dir!!.y}", dir.y < 0f)
    }

    @Test
    fun computeFireDirection_tapOutsideCone_clampsToEdge() {
        // Ship points up; tap to the far right → outside ±20° cone
        val ship = Ship.initial()
        val tap = Vec2(1.0f, 0.5f)  // far right, ~90° off heading
        val dir = controller.computeFireDirection(ship, tap)
        assertNotNull(dir)
        // Result must be unit-length (clamped direction)
        assertEquals(1.0f, dir!!.length, 0.01f)
    }

    // ── spawnAsteroids ────────────────────────────────────────────────────────

    @Test
    fun spawnAsteroids_level1_spawns6Asteroids() {
        val state = AsteroidsState.initial()
        val result = controller.spawnAsteroids(state, config, seed)
        assertEquals(6, result.asteroids.size)
    }

    @Test
    fun spawnAsteroids_allAsteroidsAreLarge() {
        val state = AsteroidsState.initial()
        val result = controller.spawnAsteroids(state, config, seed)
        assertTrue(result.asteroids.all { it.size == AsteroidSize.LARGE })
    }

    @Test
    fun spawnAsteroids_noneSpawnInSafeZone() {
        val state = AsteroidsState.initial()
        val result = controller.spawnAsteroids(state, config, seed)
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
        val state = AsteroidsState.initial()
        val result = controller.spawnAsteroids(state, config, seed)
        assertTrue(result.ship.isInvincible)
    }

    // ── step: ship movement ───────────────────────────────────────────────────

    @Test
    fun step_thrustForward_increasesShipSpeed() {
        val state = AsteroidsState.initial().copy(asteroids = emptyList())
        val input = AsteroidsInput(JoystickInput(dx = 0f, dy = -1f), null)
        val result = controller.step(state, config, 0.1f, input)
        assertTrue(result.state.ship.velocity.length > 0f)
    }

    @Test
    fun step_rotateRight_increasesShipAngle() {
        val state = AsteroidsState.initial().copy(asteroids = emptyList())
        val input = AsteroidsInput(JoystickInput(dx = 1f, dy = 0f), null)
        val result = controller.step(state, config, 0.1f, input)
        assertTrue(result.state.ship.angle > state.ship.angle)
    }

    @Test
    fun step_shipWrapsMovesPastRightEdge() {
        val ship = Ship.initial().copy(position = Vec2(0.98f, 0.5f), velocity = Vec2(0.5f, 0f))
        val state = AsteroidsState.initial().copy(ship = ship, asteroids = emptyList())
        val result = controller.step(state, config, 0.1f, noInput())
        assertTrue("should wrap to left side", result.state.ship.position.x < 0.5f)
    }

    // ── step: projectile firing ───────────────────────────────────────────────

    @Test
    fun step_fireTap_addsProjectile() {
        // Ship at center pointing up; tap above ship (in cone)
        val state = AsteroidsState.initial().copy(asteroids = emptyList())
        val input = AsteroidsInput(JoystickInput.NONE, Vec2(0.5f, 0.2f))
        val result = controller.step(state, config, 0.016f, input)
        assertEquals(1, result.state.projectiles.size)
    }

    @Test
    fun step_noTap_noProjectileSpawned() {
        val state = AsteroidsState.initial().copy(asteroids = emptyList())
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(0, result.state.projectiles.size)
    }

    // ── step: collision detection ─────────────────────────────────────────────

    @Test
    fun step_projectileHitsAsteroid_destroysProjectile() {
        val asteroid = Asteroid(1, Vec2(0.5f, 0.4f), Vec2.ZERO, AsteroidSize.SMALL, 1)
        val proj = Projectile(
            id = 2, position = Vec2(0.5f, 0.41f), velocity = Vec2(0f, -0.5f), age = 0f
        )
        val state = AsteroidsState.initial().copy(
            ship = Ship.initial().copy(invincibilityTimer = 10f),
            asteroids = listOf(asteroid),
            projectiles = listOf(proj),
            nextId = 3
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(0, result.state.projectiles.size)
    }

    @Test
    fun step_projectileKillsSmallAsteroid_awardsScore() {
        val asteroid = Asteroid(1, Vec2(0.5f, 0.4f), Vec2.ZERO, AsteroidSize.SMALL, 1)
        val proj = Projectile(
            id = 2, position = Vec2(0.5f, 0.41f), velocity = Vec2(0f, -0.5f), age = 0f
        )
        val state = AsteroidsState.initial().copy(
            ship = Ship.initial().copy(invincibilityTimer = 10f),
            asteroids = listOf(asteroid),
            projectiles = listOf(proj),
            nextId = 3
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(100, result.state.score)
    }

    @Test
    fun step_projectileKillsLargeAsteroid_splitsTwoMedium() {
        val asteroid = Asteroid(1, Vec2(0.5f, 0.4f), Vec2(0.1f, 0f), AsteroidSize.LARGE, 1)
        val proj = Projectile(
            id = 2, position = Vec2(0.5f, 0.41f), velocity = Vec2(0f, -0.5f), age = 0f
        )
        val state = AsteroidsState.initial().copy(
            ship = Ship.initial().copy(invincibilityTimer = 10f),
            asteroids = listOf(asteroid),
            projectiles = listOf(proj),
            nextId = 3
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(2, result.state.asteroids.size)
        assertTrue(result.state.asteroids.all { it.size == AsteroidSize.MEDIUM })
    }

    @Test
    fun step_shipHitsAsteroid_emitsPlayerHitEvent() {
        val asteroid = Asteroid(1, Vec2(0.5f, 0.5f), Vec2.ZERO, AsteroidSize.LARGE, 3)
        val state = AsteroidsState.initial().copy(
            ship = Ship.initial().copy(invincibilityTimer = 0f),
            asteroids = listOf(asteroid),
            projectiles = emptyList()
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(AsteroidsStepEvent.PLAYER_HIT, result.event)
    }

    @Test
    fun step_shipHitsAsteroid_decrementsLives() {
        val asteroid = Asteroid(1, Vec2(0.5f, 0.5f), Vec2.ZERO, AsteroidSize.LARGE, 3)
        val state = AsteroidsState.initial().copy(
            ship = Ship.initial().copy(invincibilityTimer = 0f),
            asteroids = listOf(asteroid),
            projectiles = emptyList()
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(2, result.state.lives)
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
            beaconsCollectedThisLevel = 4  // this is the 5th
        )
        val result = controller.step(state, config, 0.016f, noInput())
        assertEquals(AsteroidsStepEvent.ALL_BEACONS_COLLECTED, result.event)
    }

    // ── advanceLevel ──────────────────────────────────────────────────────────

    @Test
    fun advanceLevel_incrementsLevelByOne() {
        val state = AsteroidsState.initial()
        val result = controller.advanceLevel(state)
        assertEquals(2, result.level)
    }
}
