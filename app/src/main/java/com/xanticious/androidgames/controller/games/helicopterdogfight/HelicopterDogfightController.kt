package com.xanticious.androidgames.controller.games.helicopterdogfight

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.helicopterdogfight.HeliEnemy
import com.xanticious.androidgames.model.games.helicopterdogfight.HeliEnemyType
import com.xanticious.androidgames.model.games.helicopterdogfight.HeliProjectile
import com.xanticious.androidgames.model.games.helicopterdogfight.HeliProjectileOwner
import com.xanticious.androidgames.model.games.helicopterdogfight.HelicopterDogfightConfig
import com.xanticious.androidgames.model.games.helicopterdogfight.HelicopterDogfightEvent
import com.xanticious.androidgames.model.games.helicopterdogfight.HelicopterDogfightInput
import com.xanticious.androidgames.model.games.helicopterdogfight.HelicopterDogfightState
import com.xanticious.androidgames.model.games.helicopterdogfight.HelicopterDogfightStep
import kotlin.math.abs

/**
 * Pure Helicopter Dogfight rules. No Android or Compose imports — fully JVM-testable.
 *
 * Coordinate system: normalized [0,1]×[0,1], origin top-left.
 * Player heli occupies the left portion; enemies occupy the right portion.
 * Player fires rightward (+x); enemies fire leftward (−x).
 */
class HelicopterDogfightController {

    companion object {
        private const val PLAYER_PROJ_RADIUS = 0.015f
        private const val ENEMY_RADIUS = 0.045f
        private const val ENEMY_PROJ_RADIUS = 0.015f
        private const val ENEMY_HELI_SPEED = 0.12f
        private const val ENEMY_MIN_Y = 0.08f
        private const val ENEMY_MAX_Y = 0.87f
    }

    fun configFor(difficulty: GameDifficulty): HelicopterDogfightConfig = when (difficulty) {
        GameDifficulty.EASY -> HelicopterDogfightConfig(
            playerSpeed = 0.50f,
            playerProjectileSpeed = 0.70f,
            autoFireInterval = 0.35f,
            enemyProjectileSpeed = 0.25f,
            enemyFireInterval = 2.0f,
            enemyCountPerScreen = 2
        )
        GameDifficulty.MEDIUM -> HelicopterDogfightConfig(
            playerSpeed = 0.50f,
            playerProjectileSpeed = 0.75f,
            autoFireInterval = 0.32f,
            enemyProjectileSpeed = 0.35f,
            enemyFireInterval = 1.5f,
            enemyCountPerScreen = 3
        )
        GameDifficulty.HARD -> HelicopterDogfightConfig(
            playerSpeed = 0.50f,
            playerProjectileSpeed = 0.80f,
            autoFireInterval = 0.28f,
            enemyProjectileSpeed = 0.45f,
            enemyFireInterval = 1.0f,
            enemyCountPerScreen = 4
        )
    }

    // -----------------------------------------------------------------------
    // Screen setup
    // -----------------------------------------------------------------------

    /** Builds the enemy roster for [screenNumber] with IDs beginning at [startEnemyId]. */
    fun buildScreen(
        screenNumber: Int,
        config: HelicopterDogfightConfig,
        startEnemyId: Int
    ): List<HeliEnemy> {
        val layout = enemyLayout(screenNumber, config.enemyCountPerScreen)
        val extraHp = if (screenNumber > 10) 1 else 0
        return layout.mapIndexed { index, (type, x, y) ->
            val vel = if (type == HeliEnemyType.ENEMY_HELI) Vec2(0f, ENEMY_HELI_SPEED) else Vec2.ZERO
            HeliEnemy(
                id = startEnemyId + index,
                type = type,
                position = Vec2(x, y),
                velocity = vel,
                hp = type.maxHp + extraHp,
                fireCooldown = 0.5f + index * 0.4f
            )
        }
    }

    fun startFirstScreen(
        state: HelicopterDogfightState,
        config: HelicopterDogfightConfig
    ): HelicopterDogfightState {
        val enemies = buildScreen(1, config, 0)
        return state.copy(
            screenNumber = 1,
            enemies = enemies,
            projectiles = emptyList(),
            autoFireTimer = 0f,
            noHitCurrentScreen = true,
            nextEnemyId = enemies.size,
            nextProjectileId = 0
        )
    }

    fun startNextScreen(
        state: HelicopterDogfightState,
        config: HelicopterDogfightConfig
    ): HelicopterDogfightState {
        val next = state.screenNumber + 1
        val enemies = buildScreen(next, config, state.nextEnemyId)
        return state.copy(
            screenNumber = next,
            enemies = enemies,
            projectiles = emptyList(),
            autoFireTimer = 0f,
            noHitCurrentScreen = true,
            nextEnemyId = state.nextEnemyId + enemies.size
        )
    }

    /** Resets the player for respawn: full HP, brief invincibility, centred position. */
    fun respawnPlayer(
        state: HelicopterDogfightState,
        config: HelicopterDogfightConfig
    ): HelicopterDogfightState = state.copy(
        playerPos = Vec2(0.15f, 0.5f),
        playerHp = 5,
        invincibilityTimer = config.invincibilityDuration,
        projectiles = state.projectiles.filter { it.owner == HeliProjectileOwner.PLAYER }
    )

    // -----------------------------------------------------------------------
    // Main step
    // -----------------------------------------------------------------------

    fun step(
        state: HelicopterDogfightState,
        config: HelicopterDogfightConfig,
        dt: Float,
        input: HelicopterDogfightInput
    ): HelicopterDogfightStep {
        val events = mutableListOf<HelicopterDogfightEvent>()
        var s = state

        // -- Player movement --
        val newX = (s.playerPos.x + input.joystickDx * config.playerSpeed * dt)
            .coerceIn(HelicopterDogfightState.MIN_PLAYER_X, HelicopterDogfightState.MAX_PLAYER_X)
        val newY = (s.playerPos.y + input.joystickDy * config.playerSpeed * dt)
            .coerceIn(HelicopterDogfightState.MIN_PLAYER_Y, HelicopterDogfightState.MAX_PLAYER_Y)
        s = s.copy(playerPos = Vec2(newX, newY))

        // -- Invincibility decay --
        s = s.copy(invincibilityTimer = (s.invincibilityTimer - dt).coerceAtLeast(0f))

        // -- Auto-fire --
        val newFireTimer = s.autoFireTimer - dt
        s = if (newFireTimer <= 0f) {
            val bullet = HeliProjectile(
                id = s.nextProjectileId,
                position = Vec2(
                    s.playerPos.x + HelicopterDogfightState.PLAYER_HALF_WIDTH + 0.01f,
                    s.playerPos.y
                ),
                velocity = Vec2(config.playerProjectileSpeed, 0f),
                owner = HeliProjectileOwner.PLAYER
            )
            s.copy(
                projectiles = s.projectiles + bullet,
                autoFireTimer = config.autoFireInterval,
                nextProjectileId = s.nextProjectileId + 1
            )
        } else {
            s.copy(autoFireTimer = newFireTimer)
        }

        // -- Move projectiles; cull anything that leaves the arena --
        val movedProjectiles = s.projectiles
            .map { p -> p.copy(position = p.position + p.velocity * dt) }
            .filter { p -> p.position.x in -0.05f..1.05f && p.position.y in -0.05f..1.05f }
        s = s.copy(projectiles = movedProjectiles)

        // -- Enemy AI: movement + fire --
        val newEnemyProjectiles = mutableListOf<HeliProjectile>()
        var nextProjId = s.nextProjectileId
        val updatedEnemies = s.enemies.map { enemy ->
            var e = enemy

            // ENEMY_HELI oscillates vertically; others are stationary.
            val newPos = if (e.type == HeliEnemyType.ENEMY_HELI) {
                var vel = e.velocity
                var pos = e.position + vel * dt
                if (pos.y < ENEMY_MIN_Y || pos.y > ENEMY_MAX_Y) {
                    vel = vel.copy(y = -vel.y)
                    pos = e.position + vel * dt
                }
                e = e.copy(velocity = vel)
                pos
            } else {
                e.position
            }
            e = e.copy(position = newPos)

            // Fire when cooldown expires.
            val newCooldown = e.fireCooldown - dt
            e = if (newCooldown <= 0f) {
                val shots = buildEnemyShots(e, s.playerPos, config, nextProjId)
                newEnemyProjectiles.addAll(shots)
                nextProjId += shots.size
                e.copy(fireCooldown = config.enemyFireInterval)
            } else {
                e.copy(fireCooldown = newCooldown)
            }
            e
        }
        s = s.copy(
            enemies = updatedEnemies,
            projectiles = s.projectiles + newEnemyProjectiles,
            nextProjectileId = nextProjId
        )

        // -- Collision: player projectiles vs enemies --
        val destroyedEnemyIds = mutableSetOf<Int>()
        val usedPlayerProjIds = mutableSetOf<Int>()
        val survivingEnemies = mutableListOf<HeliEnemy>()

        for (enemy in s.enemies) {
            var e = enemy
            for (proj in s.projectiles.filter {
                it.owner == HeliProjectileOwner.PLAYER && it.id !in usedPlayerProjIds
            }) {
                if (proj.position.distanceTo(e.position) < ENEMY_RADIUS + PLAYER_PROJ_RADIUS) {
                    usedPlayerProjIds.add(proj.id)
                    e = e.copy(hp = e.hp - 1)
                    if (e.hp <= 0) {
                        destroyedEnemyIds.add(e.id)
                        events.add(HelicopterDogfightEvent.EnemyDestroyed(e.type, e.type.scoreValue))
                        break
                    }
                }
            }
            if (e.id !in destroyedEnemyIds) survivingEnemies.add(e)
        }

        val pointsThisFrame = events
            .filterIsInstance<HelicopterDogfightEvent.EnemyDestroyed>()
            .sumOf { it.points }
        s = s.copy(
            enemies = survivingEnemies,
            projectiles = s.projectiles.filter { it.id !in usedPlayerProjIds },
            score = s.score + pointsThisFrame
        )

        // -- Collision: enemy projectiles vs player --
        if (!s.isInvincible) {
            val hit = s.projectiles.any { p ->
                p.owner == HeliProjectileOwner.ENEMY &&
                    abs(p.position.x - s.playerPos.x) < HelicopterDogfightState.PLAYER_HALF_WIDTH + ENEMY_PROJ_RADIUS &&
                    abs(p.position.y - s.playerPos.y) < HelicopterDogfightState.PLAYER_HALF_HEIGHT + ENEMY_PROJ_RADIUS
            }
            if (hit) {
                val newHp = s.playerHp - 1
                // Clear enemy projectiles on hit (classic arcade: projectile absorbed)
                val clearedProjectiles = s.projectiles.filter { it.owner != HeliProjectileOwner.ENEMY }
                if (newHp > 0) {
                    s = s.copy(
                        playerHp = newHp,
                        invincibilityTimer = config.invincibilityDuration,
                        noHitCurrentScreen = false,
                        projectiles = clearedProjectiles
                    )
                    events.add(HelicopterDogfightEvent.PlayerHit)
                } else {
                    val newLives = s.playerLives - 1
                    s = s.copy(
                        playerHp = 0,
                        playerLives = newLives,
                        noHitCurrentScreen = false,
                        projectiles = clearedProjectiles
                    )
                    events.add(
                        if (newLives > 0) HelicopterDogfightEvent.PlayerCrashed
                        else HelicopterDogfightEvent.GameOver
                    )
                }
            }
        }

        // -- Screen clear: all enemies destroyed --
        val fatalEvent = events.any {
            it is HelicopterDogfightEvent.GameOver || it is HelicopterDogfightEvent.PlayerCrashed
        }
        if (s.enemies.isEmpty() && !fatalEvent) {
            if (s.noHitCurrentScreen) {
                s = s.copy(score = s.score + 500)
            }
            events.add(HelicopterDogfightEvent.AllEnemiesDestroyed)
        }

        return HelicopterDogfightStep(s, events)
    }

    /** Stars earned for end-of-game summary (1–3). */
    fun starsEarned(screenNumber: Int, lives: Int): Int = when {
        screenNumber >= 8 && lives == 3 -> 3
        screenNumber >= 4 -> 2
        else -> 1
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private fun buildEnemyShots(
        enemy: HeliEnemy,
        playerPos: Vec2,
        config: HelicopterDogfightConfig,
        startId: Int
    ): List<HeliProjectile> {
        val origin = enemy.position
        val speed = config.enemyProjectileSpeed
        return when (enemy.type) {
            HeliEnemyType.GROUND_TURRET -> listOf(
                HeliProjectile(startId, origin, Vec2(-speed, 0f), HeliProjectileOwner.ENEMY)
            )
            HeliEnemyType.AA_GUN -> {
                // Aim at the player's current altitude.
                val dir = Vec2(-1f, playerPos.y - origin.y).normalized()
                listOf(HeliProjectile(startId, origin, dir * speed, HeliProjectileOwner.ENEMY))
            }
            HeliEnemyType.ENEMY_HELI -> listOf(
                HeliProjectile(startId, origin, Vec2(-speed, 0f), HeliProjectileOwner.ENEMY),
                HeliProjectile(startId + 1, origin, Vec2(-speed * 0.87f, -speed * 0.5f), HeliProjectileOwner.ENEMY),
                HeliProjectile(startId + 2, origin, Vec2(-speed * 0.87f, speed * 0.5f), HeliProjectileOwner.ENEMY)
            )
        }
    }

    /**
     * Returns a list of (type, x, y) placements for [screenNumber].
     * Count is clamped to `[1, 6]` and grows gradually with screen progression.
     */
    private fun enemyLayout(
        screenNumber: Int,
        baseCount: Int
    ): List<Triple<HeliEnemyType, Float, Float>> {
        val count = (baseCount + screenNumber / 5).coerceIn(1, 6)
        return buildList {
            add(Triple(HeliEnemyType.GROUND_TURRET, 0.70f, 0.82f))
            if (count >= 2) add(Triple(HeliEnemyType.GROUND_TURRET, 0.88f, 0.78f))
            if (count >= 3 || screenNumber >= 2) add(Triple(HeliEnemyType.AA_GUN, 0.75f, 0.35f))
            if (count >= 4 || screenNumber >= 3) add(Triple(HeliEnemyType.ENEMY_HELI, 0.82f, 0.50f))
            if (count >= 5) add(Triple(HeliEnemyType.AA_GUN, 0.65f, 0.22f))
            if (count >= 6) add(Triple(HeliEnemyType.ENEMY_HELI, 0.92f, 0.65f))
        }.take(count)
    }
}
