package com.xanticious.androidgames.controller.games.asteroids

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.asteroids.Asteroid
import com.xanticious.androidgames.model.games.asteroids.AsteroidSize
import com.xanticious.androidgames.model.games.asteroids.AsteroidsConfig
import com.xanticious.androidgames.model.games.asteroids.AsteroidsInput
import com.xanticious.androidgames.model.games.asteroids.AsteroidsMode
import com.xanticious.androidgames.model.games.asteroids.AsteroidsState
import com.xanticious.androidgames.model.games.asteroids.AsteroidsStep
import com.xanticious.androidgames.model.games.asteroids.AsteroidsStepEvent
import com.xanticious.androidgames.model.games.asteroids.Beacon
import com.xanticious.androidgames.model.games.asteroids.Projectile
import com.xanticious.androidgames.model.games.asteroids.Ship
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Pure Asteroids rules: ship physics, asteroid splitting, beacon explosions,
 * autofire and collision detection. No Android or Compose imports.
 *
 * Movement model: the knob applies acceleration in its own direction (full 360°);
 * the ship never rotates manually. Speed is clamped between [AsteroidsConfig.minShipSpeed]
 * and [AsteroidsConfig.maxShipSpeed], so the ship is always drifting. The ship's heading
 * auto-aligns to its velocity. Firing is automatic, always toward the nearest asteroid.
 */
class AsteroidsController {

    fun configFor(difficulty: GameDifficulty): AsteroidsConfig = when (difficulty) {
        GameDifficulty.EASY -> AsteroidsConfig(asteroidBaseSpeed = 0.07f)
        GameDifficulty.MEDIUM -> AsteroidsConfig(asteroidBaseSpeed = 0.11f)
        GameDifficulty.HARD -> AsteroidsConfig(asteroidBaseSpeed = 0.16f)
    }

    /** Populates the field with large asteroids for [state.level]. Resets ship and per-level timers. */
    fun spawnAsteroids(
        state: AsteroidsState,
        config: AsteroidsConfig,
        random: Random = Random.Default
    ): AsteroidsState {
        val count = (4 + state.level * 2).coerceAtMost(20)
        val speedMult = min(
            1f + config.asteroidSpeedGainPerLevel * (state.level - 1),
            config.asteroidMaxSpeedMultiplier
        )
        val speed = config.asteroidBaseSpeed * speedMult

        var nextId = state.nextId
        val asteroids = mutableListOf<Asteroid>()
        repeat(count) {
            asteroids += Asteroid(
                id = nextId++,
                position = safeSpawnPosition(Vec2(0.5f, 0.5f), 0.25f, random),
                velocity = Vec2.fromAngle(random.nextFloat() * 2f * Math.PI.toFloat()) * speed,
                size = AsteroidSize.LARGE,
                hp = Asteroid.hpFor(AsteroidSize.LARGE)
            )
        }

        return state.copy(
            ship = Ship.initial(config.minShipSpeed).copy(invincibilityTimer = config.invincibilityDuration),
            asteroids = asteroids,
            projectiles = emptyList(),
            beacon = null,
            beaconsCollectedThisLevel = 0,
            beaconSpawnTimer = config.beaconSpawnDelay,
            freezeTimer = 0f,
            fireCooldown = 0f,
            nextId = nextId
        )
    }

    /** Increments level counter (call before spawning asteroids for the next level). */
    fun advanceLevel(state: AsteroidsState): AsteroidsState =
        state.copy(level = state.level + 1)

    /**
     * Advances the game by [dt] seconds.
     *
     * Order: clocks → ship movement → projectile lifetime → autofire → asteroid
     * drift → beacon timer → projectile/asteroid collisions → ship/asteroid
     * collision (teleport + freeze) → ship/beacon collection → time-expiry check.
     */
    fun step(
        state: AsteroidsState,
        config: AsteroidsConfig,
        dt: Float,
        input: AsteroidsInput,
        random: Random = Random.Default
    ): AsteroidsStep {

        // ── 0. Clocks (never paused) ───────────────────────────────────────────
        val elapsed = state.elapsedTime + dt
        val freezeTimer = (state.freezeTimer - dt).coerceAtLeast(0f)
        val frozen = state.freezeTimer > 0f

        // ── 1. Ship movement: knob = acceleration (360°), auto-aligned heading ──
        val accel = Vec2(input.joystick.dx, input.joystick.dy) * config.accelerationForce
        var shipVel = state.ship.velocity + accel * dt
        // Velocity damping toward zero.
        val damped = shipVel.length - config.dampening * dt
        shipVel = if (damped <= 0f) Vec2.ZERO else shipVel.normalized() * damped
        // Clamp into [minSpeed, maxSpeed] so the ship is never fully still.
        val spd = shipVel.length
        shipVel = when {
            spd > config.maxShipSpeed -> shipVel.normalized() * config.maxShipSpeed
            spd < config.minShipSpeed -> {
                val dir = if (spd > 1e-5f) shipVel.normalized() else Vec2.fromAngle(state.ship.angle)
                dir * config.minShipSpeed
            }
            else -> shipVel
        }
        val newAngle = atan2(shipVel.y, shipVel.x)
        val shipPos = wrap(state.ship.position + shipVel * dt)
        val invTimer = (state.ship.invincibilityTimer - dt).coerceAtLeast(0f)
        var ship = state.ship.copy(
            position = shipPos,
            velocity = shipVel,
            angle = newAngle,
            invincibilityTimer = invTimer
        )

        // ── 2. Projectiles: age + move ─────────────────────────────────────────
        var nextId = state.nextId
        var projectiles = state.projectiles
            .map { p -> p.copy(position = wrap(p.position + p.velocity * dt), age = p.age + dt) }
            .filter { it.age < config.projectileLifetime }

        // ── 3. Autofire toward the nearest asteroid ────────────────────────────
        var fireCooldown = (state.fireCooldown - dt).coerceAtLeast(0f)
        if (fireCooldown <= 0f) {
            val dir = nearestAsteroidDirection(ship.position, state.asteroids)
            if (dir != null) {
                projectiles = projectiles + Projectile(
                    id = nextId++,
                    position = ship.position,
                    velocity = dir * config.projectileSpeed,
                    age = 0f
                )
                fireCooldown = config.fireInterval
            }
        }

        // ── 4. Asteroids: drift (frozen during the damage animation) ───────────
        var asteroids = if (frozen) {
            state.asteroids
        } else {
            state.asteroids.map { a -> a.copy(position = wrap(a.position + a.velocity * dt)) }
        }

        // ── 5. Beacon spawn timer ──────────────────────────────────────────────
        var beaconTimer = (state.beaconSpawnTimer - dt)
        var beacon = state.beacon
        if (beacon == null
            && beaconTimer <= 0f
            && state.beaconsCollectedThisLevel < AsteroidsState.BEACONS_PER_LEVEL
        ) {
            beacon = Beacon(safeSpawnPosition(ship.position, 0.2f, random))
            beaconTimer = 0f
        }

        // ── 6. Projectile–asteroid collisions ──────────────────────────────────
        var score = state.score
        var destroyed = state.asteroidsDestroyed
        val hitProjectileIds = mutableSetOf<Int>()
        val asteroidById = asteroids.associateBy { it.id }.toMutableMap()
        val splits = mutableListOf<Asteroid>()

        for (p in projectiles) {
            if (p.id in hitProjectileIds) continue
            for (a in asteroidById.values.toList()) {
                val threshold = Asteroid.radiusFor(a.size) + Projectile.RADIUS
                if (wrappedDistance(p.position, a.position) < threshold) {
                    hitProjectileIds += p.id
                    val damaged = a.copy(hp = a.hp - 1)
                    if (damaged.hp <= 0) {
                        asteroidById -= a.id
                        score += scoreForDestroy(a.size)
                        destroyed++
                        val children = splitAsteroid(a, nextId)
                        nextId += children.size
                        splits += children
                    } else {
                        asteroidById[a.id] = damaged
                    }
                    break
                }
            }
        }

        asteroids = asteroidById.values.toList() + splits
        projectiles = projectiles.filter { it.id !in hitProjectileIds }

        // ── 7. Ship–asteroid collision: teleport to safety + freeze ────────────
        var lives = state.lives
        var event = AsteroidsStepEvent.NONE
        var resultFreeze = freezeTimer

        if (!ship.isInvincible && !frozen) {
            val crashed = asteroids.any { a ->
                wrappedDistance(ship.position, a.position) < Ship.RADIUS + Asteroid.radiusFor(a.size)
            }
            if (crashed) {
                if (!state.mode.infiniteLives) lives--
                val safePos = safePositionAwayFromAsteroids(asteroids, random)
                ship = ship.copy(
                    position = safePos,
                    velocity = Vec2.fromAngle(Ship.INITIAL_ANGLE) * config.minShipSpeed,
                    angle = Ship.INITIAL_ANGLE,
                    invincibilityTimer = maxOf(config.invincibilityDuration, config.freezeDuration)
                )
                resultFreeze = config.freezeDuration
                event = AsteroidsStepEvent.PLAYER_HIT
            }
        }

        // ── 8. Ship–beacon collection ──────────────────────────────────────────
        var beaconsCollected = state.beaconsCollectedThisLevel
        if (event == AsteroidsStepEvent.NONE && beacon != null) {
            if (wrappedDistance(ship.position, beacon.position) < Ship.RADIUS + Beacon.RADIUS) {
                val beaconNum = beaconsCollected + 1
                score += 500 + beaconNum * 100
                beaconsCollected++

                val expl = applyBeaconExplosion(asteroids, beacon.position, config, nextId)
                asteroids = expl.asteroids
                score += expl.scoreGained
                destroyed += expl.destroyed
                nextId = expl.nextId

                beacon = null
                val allDone = beaconsCollected >= AsteroidsState.BEACONS_PER_LEVEL
                beaconTimer = if (allDone) Float.MAX_VALUE else config.beaconSpawnDelay
                event = if (allDone) AsteroidsStepEvent.ALL_BEACONS_COLLECTED
                        else AsteroidsStepEvent.BEACON_COLLECTED
            }
        }

        // ── 9. Time-challenge expiry ───────────────────────────────────────────
        val mode = state.mode
        if (mode is AsteroidsMode.TimeChallenge && elapsed >= mode.durationSeconds) {
            event = AsteroidsStepEvent.TIME_EXPIRED
        }

        return AsteroidsStep(
            state = state.copy(
                ship = ship,
                asteroids = asteroids,
                projectiles = projectiles,
                beacon = beacon,
                beaconsCollectedThisLevel = beaconsCollected,
                beaconSpawnTimer = beaconTimer,
                lives = lives,
                score = score,
                nextId = nextId,
                elapsedTime = elapsed,
                asteroidsDestroyed = destroyed,
                freezeTimer = resultFreeze,
                fireCooldown = fireCooldown
            ),
            event = event
        )
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /** Wraps a position into [0, 1] on both axes (screen wrap-around). */
    internal fun wrap(pos: Vec2): Vec2 =
        Vec2(((pos.x % 1f) + 1f) % 1f, ((pos.y % 1f) + 1f) % 1f)

    /** Minimum toroidal (wrap-around) distance between two normalized positions. */
    internal fun wrappedDistance(a: Vec2, b: Vec2): Float {
        val dx = abs(a.x - b.x).let { minOf(it, 1f - it) }
        val dy = abs(a.y - b.y).let { minOf(it, 1f - it) }
        return sqrt(dx * dx + dy * dy)
    }

    /** Shortest toroidal vector from [from] to [to]. */
    internal fun wrappedDelta(from: Vec2, to: Vec2): Vec2 {
        var dx = to.x - from.x
        var dy = to.y - from.y
        if (dx > 0.5f) dx -= 1f else if (dx < -0.5f) dx += 1f
        if (dy > 0.5f) dy -= 1f else if (dy < -0.5f) dy += 1f
        return Vec2(dx, dy)
    }

    /** Unit direction toward the nearest asteroid (wrap-aware), or null when none. */
    internal fun nearestAsteroidDirection(shipPos: Vec2, asteroids: List<Asteroid>): Vec2? {
        val nearest = asteroids.minByOrNull { wrappedDistance(shipPos, it.position) } ?: return null
        val delta = wrappedDelta(shipPos, nearest.position)
        return if (delta.length < 1e-4f) null else delta.normalized()
    }

    private fun splitAsteroid(asteroid: Asteroid, startId: Int): List<Asteroid> {
        val childSize = when (asteroid.size) {
            AsteroidSize.LARGE -> AsteroidSize.MEDIUM
            AsteroidSize.MEDIUM -> AsteroidSize.SMALL
            AsteroidSize.SMALL -> return emptyList()
        }
        val parentSpeed = asteroid.velocity.length.coerceAtLeast(0.05f)
        val speed = parentSpeed * 1.4f
        val baseAngle = atan2(asteroid.velocity.y, asteroid.velocity.x)
        return listOf(
            Asteroid(startId,     asteroid.position, Vec2.fromAngle(baseAngle + 0.5f) * speed, childSize, Asteroid.hpFor(childSize)),
            Asteroid(startId + 1, asteroid.position, Vec2.fromAngle(baseAngle - 0.5f) * speed, childSize, Asteroid.hpFor(childSize))
        )
    }

    private fun scoreForDestroy(size: AsteroidSize): Int = when (size) {
        AsteroidSize.SMALL -> 100
        AsteroidSize.MEDIUM -> 50
        AsteroidSize.LARGE -> 20
    }

    private data class ExplosionResult(
        val asteroids: List<Asteroid>,
        val scoreGained: Int,
        val destroyed: Int,
        val nextId: Int
    )

    private fun applyBeaconExplosion(
        asteroids: List<Asteroid>,
        beaconPos: Vec2,
        config: AsteroidsConfig,
        startNextId: Int
    ): ExplosionResult {
        var score = 0
        var destroyed = 0
        var nextId = startNextId
        val survivors = mutableListOf<Asteroid>()
        val newSplits = mutableListOf<Asteroid>()

        for (a in asteroids) {
            if (wrappedDistance(a.position, beaconPos) < config.beaconExplosionRadius) {
                val damaged = a.copy(hp = a.hp - 1)
                if (damaged.hp <= 0) {
                    score += scoreForDestroy(a.size)
                    destroyed++
                    val children = splitAsteroid(a, nextId)
                    nextId += children.size
                    newSplits += children
                } else {
                    survivors += damaged
                }
            } else {
                survivors += a
            }
        }

        return ExplosionResult(survivors + newSplits, score, destroyed, nextId)
    }

    private fun safeSpawnPosition(avoidCenter: Vec2, minDist: Float, random: Random): Vec2 {
        repeat(50) {
            val pos = Vec2(random.nextFloat(), random.nextFloat())
            if (pos.distanceTo(avoidCenter) >= minDist) return pos
        }
        // Fallback: guaranteed safe position opposite the avoid center
        return Vec2(
            (avoidCenter.x + 0.5f) % 1f,
            (avoidCenter.y + 0.5f) % 1f
        )
    }

    /** Picks the sampled position with the largest gap to every asteroid (teleport-to-safety). */
    internal fun safePositionAwayFromAsteroids(asteroids: List<Asteroid>, random: Random): Vec2 {
        if (asteroids.isEmpty()) return Vec2(0.5f, 0.5f)
        var best = Vec2(0.5f, 0.5f)
        var bestGap = -1f
        repeat(40) {
            val pos = Vec2(random.nextFloat(), random.nextFloat())
            val gap = asteroids.minOf { wrappedDistance(pos, it.position) }
            if (gap > bestGap) {
                bestGap = gap
                best = pos
            }
        }
        return best
    }
}
