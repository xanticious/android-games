package com.xanticious.androidgames.controller.games.spacedefender

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.spacedefender.Enemy
import com.xanticious.androidgames.model.games.spacedefender.EnemyBehavior
import com.xanticious.androidgames.model.games.spacedefender.EnemyType
import com.xanticious.androidgames.model.games.spacedefender.Projectile
import com.xanticious.androidgames.model.games.spacedefender.ProjectileOwner
import com.xanticious.androidgames.model.games.spacedefender.Shield
import com.xanticious.androidgames.model.games.spacedefender.ShieldHealth
import com.xanticious.androidgames.model.games.spacedefender.SpaceDefenderConfig
import com.xanticious.androidgames.model.games.spacedefender.SpaceDefenderEvent
import com.xanticious.androidgames.model.games.spacedefender.SpaceDefenderInput
import com.xanticious.androidgames.model.games.spacedefender.SpaceDefenderState
import com.xanticious.androidgames.model.games.spacedefender.SpaceDefenderStep
import kotlin.math.abs
import kotlin.math.max

/**
 * Pure Space Defender rules. No Android or Compose imports — fully JVM testable.
 *
 * Coordinate system: normalized [0,1] × [0,1], origin top-left.
 * Player slides along y = [PLAYER_Y].
 */
class SpaceDefenderController {

    // Collision radii in normalized units
    private val playerProjectileRadius = 0.012f
    private val enemyProjectileRadius = 0.015f
    private val enemyRadius = 0.05f
    private val shieldHalfWidth = 0.06f
    private val shieldHalfHeight = 0.025f

    fun configFor(difficulty: GameDifficulty): SpaceDefenderConfig = when (difficulty) {
        GameDifficulty.EASY -> SpaceDefenderConfig(
            playerSpeed = 0.55f,
            playerProjectileSpeed = 0.65f,
            autoFireInterval = 0.45f,
            enemyBaseSpeed = 0.08f,
            enemyProjectileSpeed = 0.22f,
            enemyFireInterval = 2.2f
        )
        GameDifficulty.MEDIUM -> SpaceDefenderConfig(
            playerSpeed = 0.55f,
            playerProjectileSpeed = 0.75f,
            autoFireInterval = 0.38f,
            enemyBaseSpeed = 0.14f,
            enemyProjectileSpeed = 0.30f,
            enemyFireInterval = 1.6f
        )
        GameDifficulty.HARD -> SpaceDefenderConfig(
            playerSpeed = 0.55f,
            playerProjectileSpeed = 0.85f,
            autoFireInterval = 0.30f,
            enemyBaseSpeed = 0.22f,
            enemyProjectileSpeed = 0.40f,
            enemyFireInterval = 1.1f
        )
    }

    // -----------------------------------------------------------------------
    // Wave spawning
    // -----------------------------------------------------------------------

    /** Builds the enemy roster and shields for [wave] according to the design doc. */
    fun buildWave(wave: Int, config: SpaceDefenderConfig, startEnemyId: Int): List<Enemy> {
        val types = enemyTypesForWave(wave)
        val count = types.size
        return types.mapIndexed { index, type ->
            val xFraction = if (count == 1) 0.5f else 0.2f + index * (0.6f / (count - 1).coerceAtLeast(1))
            val behavior = when {
                count <= 1 -> EnemyBehavior.NEUTRAL
                index == 0 -> EnemyBehavior.FLANKER
                index == count - 1 -> EnemyBehavior.BAITER
                else -> EnemyBehavior.NEUTRAL
            }
            val speed = config.enemyBaseSpeed * waveSpeedMultiplier(wave, type)
            Enemy(
                id = startEnemyId + index,
                type = type,
                position = Vec2(xFraction, 0.08f + index * 0.005f),
                velocity = Vec2(speed, 0f),
                hp = type.maxHp,
                behavior = behavior,
                fireCooldown = (0.5f + index * 0.3f)
            )
        }
    }

    fun buildShields(wave: Int): List<Shield> {
        // Shields spawn on wave 1 and every 5th wave
        if (wave != 1 && (wave - 1) % 5 != 0) return emptyList()
        return listOf(
            Shield(0, Vec2(0.25f, 0.72f), ShieldHealth.FULL),
            Shield(1, Vec2(0.50f, 0.72f), ShieldHealth.FULL),
            Shield(2, Vec2(0.75f, 0.72f), ShieldHealth.FULL)
        )
    }

    // -----------------------------------------------------------------------
    // Main step
    // -----------------------------------------------------------------------

    fun step(
        state: SpaceDefenderState,
        config: SpaceDefenderConfig,
        dt: Float,
        input: SpaceDefenderInput
    ): SpaceDefenderStep {
        val events = mutableListOf<SpaceDefenderEvent>()
        var s = state

        // -- Player movement --
        val newX = (s.playerX + input.joystickDx * config.playerSpeed * dt)
            .coerceIn(SpaceDefenderState.PLAYER_HALF_WIDTH, 1f - SpaceDefenderState.PLAYER_HALF_WIDTH)
        s = s.copy(playerX = newX)

        // -- Auto-fire --
        val newFireTimer = s.autoFireTimer - dt
        s = if (newFireTimer <= 0f) {
            val proj = Projectile(
                id = s.nextProjectileId,
                position = Vec2(s.playerX, SpaceDefenderState.PLAYER_Y - 0.04f),
                velocity = Vec2(0f, -config.playerProjectileSpeed),
                owner = ProjectileOwner.PLAYER
            )
            s.copy(
                projectiles = s.projectiles + proj,
                autoFireTimer = config.autoFireInterval,
                nextProjectileId = s.nextProjectileId + 1
            )
        } else {
            s.copy(autoFireTimer = newFireTimer)
        }

        // -- Move projectiles --
        val movedProj = s.projectiles.map { p ->
            if (p.homing) {
                val dir = (p.homingTarget - p.position).normalized()
                p.copy(position = p.position + dir * config.enemyProjectileSpeed * dt)
            } else {
                p.copy(position = p.position + p.velocity * dt)
            }
        }.filter { p -> p.position.y in -0.05f..1.05f && p.position.x in -0.05f..1.05f }
        s = s.copy(projectiles = movedProj)

        // -- Enemy AI and movement --
        val updatedEnemies = mutableListOf<Enemy>()
        val newProjectiles = mutableListOf<Projectile>()
        var nextId = s.nextProjectileId
        val playerPos = Vec2(s.playerX, SpaceDefenderState.PLAYER_Y)

        for (enemy in s.enemies) {
            var e = enemy

            // Speed boost when HP is low
            val aggressionFactor = if (e.hp <= e.type.maxHp / 2) 1.4f else 1f

            // Behavioral movement
            val speed = config.enemyBaseSpeed * waveSpeedMultiplier(s.wave, e.type) * aggressionFactor
            val newVelocity = when (e.behavior) {
                EnemyBehavior.FLANKER -> {
                    // Move toward player's horizontal position
                    val dx = s.playerX - e.position.x
                    val dirX = if (abs(dx) > 0.01f) dx / abs(dx) else 0f
                    Vec2(dirX * speed, e.velocity.y)
                }
                EnemyBehavior.BAITER -> {
                    // Move opposite to the player
                    val dx = s.playerX - e.position.x
                    val dirX = if (abs(dx) > 0.01f) -dx / abs(dx) else 0f
                    Vec2(dirX * speed, e.velocity.y)
                }
                EnemyBehavior.NEUTRAL -> {
                    // Side-to-side sweep, reverse at edges
                    val vel = if (e.position.x <= 0.05f) Vec2(speed, e.velocity.y)
                    else if (e.position.x >= 0.95f) Vec2(-speed, e.velocity.y)
                    else e.velocity.copy(x = if (e.velocity.x == 0f) speed else e.velocity.x)
                    vel
                }
            }

            // Erratic movement for commanders and low-HP enemies
            val erraticTimer = (e.erraticTimer - dt).coerceAtLeast(0f)
            val finalVelocity = if (e.type == EnemyType.COMMANDER && e.hp <= e.type.maxHp / 2) {
                newVelocity + Vec2(
                    (if (erraticTimer <= 0f) ((nextId % 3) - 1).toFloat() else 0f) * speed * 0.5f,
                    0f
                )
            } else newVelocity

            var newPos = e.position + finalVelocity * dt
            newPos = newPos.coerceIn(0.05f, 0.05f, 0.95f, 0.55f)

            // Fire logic
            val newCooldown = e.fireCooldown - dt
            val fireInterval = config.enemyFireInterval * if (e.hp <= e.type.maxHp / 2) 0.7f else 1f
            if (newCooldown <= 0f) {
                val shots = buildEnemyShots(e, playerPos, config, nextId)
                newProjectiles.addAll(shots)
                nextId += shots.size
                e = e.copy(fireCooldown = fireInterval)
            } else {
                e = e.copy(fireCooldown = newCooldown)
            }

            updatedEnemies.add(e.copy(position = newPos, velocity = finalVelocity, erraticTimer = erraticTimer))
        }

        s = s.copy(
            enemies = updatedEnemies,
            projectiles = s.projectiles.drop(s.projectiles.size) + movedProj + newProjectiles,
            nextProjectileId = nextId
        )
        // Reassemble: moved player projectiles + new enemy projectiles
        s = s.copy(projectiles = movedProj + newProjectiles)

        // -- Collision: player projectiles vs enemies --
        val hitEnemyIds = mutableSetOf<Int>()
        val hitPlayerProjIds = mutableSetOf<Int>()
        val survivingEnemies = mutableListOf<Enemy>()

        for (enemy in s.enemies) {
            var e = enemy
            for (proj in s.projectiles.filter { it.owner == ProjectileOwner.PLAYER && it.id !in hitPlayerProjIds }) {
                if (proj.position.distanceTo(e.position) < enemyRadius + playerProjectileRadius) {
                    hitPlayerProjIds.add(proj.id)
                    e = e.copy(hp = e.hp - 1)
                    if (e.hp <= 0) {
                        hitEnemyIds.add(e.id)
                        val bonus = (s.scoreMultiplier * e.type.scoreValue).toInt()
                        events.add(SpaceDefenderEvent.EnemyDestroyed(e.type, bonus))
                        break
                    }
                }
            }
            if (e.id !in hitEnemyIds) survivingEnemies.add(e)
        }

        // -- Collision: projectiles vs shields --
        val hitShieldProjIds = mutableSetOf<Int>()
        val updatedShields = s.shields.map { shield ->
            if (shield.health == ShieldHealth.BROKEN) return@map shield
            var sh = shield
            for (proj in s.projectiles.filter { it.id !in hitPlayerProjIds && it.id !in hitShieldProjIds }) {
                val dx = abs(proj.position.x - sh.position.x)
                val dy = abs(proj.position.y - sh.position.y)
                if (dx < shieldHalfWidth && dy < shieldHalfHeight) {
                    hitShieldProjIds.add(proj.id)
                    sh = sh.copy(health = when (sh.health) {
                        ShieldHealth.FULL -> ShieldHealth.CRACKED
                        ShieldHealth.CRACKED -> ShieldHealth.BROKEN
                        ShieldHealth.BROKEN -> ShieldHealth.BROKEN
                    })
                    break
                }
            }
            sh
        }

        val survivingProjectiles = s.projectiles.filter { p ->
            p.id !in hitPlayerProjIds && p.id !in hitShieldProjIds
        }
        s = s.copy(
            enemies = survivingEnemies,
            projectiles = survivingProjectiles,
            shields = updatedShields
        )

        // -- Collision: enemy projectiles vs player --
        val invTimer = (s.invincibilityTimer - dt).coerceAtLeast(0f)
        s = s.copy(invincibilityTimer = invTimer)

        if (!s.isInvincible) {
            val playerHit = s.projectiles.any { p ->
                p.owner == ProjectileOwner.ENEMY &&
                    abs(p.position.x - s.playerX) < SpaceDefenderState.PLAYER_HALF_WIDTH + enemyProjectileRadius &&
                    abs(p.position.y - SpaceDefenderState.PLAYER_Y) < 0.04f
            }
            if (playerHit) {
                val newLives = s.lives - 1
                s = s.copy(
                    lives = newLives,
                    invincibilityTimer = config.invincibilityDuration,
                    noHitCurrentWave = false,
                    projectiles = s.projectiles.filter { it.owner != ProjectileOwner.ENEMY }
                )
                events.add(if (newLives <= 0) SpaceDefenderEvent.GameOver else SpaceDefenderEvent.PlayerHit)
            }
        }

        // -- Compute score from enemy destroyed events --
        val pointsThisFrame = events.filterIsInstance<SpaceDefenderEvent.EnemyDestroyed>().sumOf { it.points }
        if (pointsThisFrame > 0) {
            s = s.copy(score = s.score + pointsThisFrame)
        }

        // -- Check wave clear --
        if (s.enemies.isEmpty() && events.none { it == SpaceDefenderEvent.GameOver }) {
            // Wave bonus scoring
            val shieldsRemaining = s.shields.count { it.health != ShieldHealth.BROKEN }
            var bonus = 0
            bonus += 150 * shieldsRemaining
            if (s.lives == 3) bonus += 500
            if (s.backToBackNoHit && s.noHitCurrentWave) bonus = (bonus * 1.2f).toInt()

            val newMultiplier = if (s.backToBackNoHit && s.noHitCurrentWave) s.scoreMultiplier * 1.2f else 1f
            s = s.copy(
                score = s.score + bonus,
                scoreMultiplier = newMultiplier,
                backToBackNoHit = s.noHitCurrentWave
            )
            events.add(SpaceDefenderEvent.AllEnemiesDestroyed)
        }

        return SpaceDefenderStep(s, events)
    }

    // -----------------------------------------------------------------------
    // Wave setup helpers
    // -----------------------------------------------------------------------

    fun startNextWave(state: SpaceDefenderState, config: SpaceDefenderConfig): SpaceDefenderState {
        val newWave = state.wave + 1
        val enemies = buildWave(newWave, config, state.nextEnemyId)
        val shields = if (newWave == 1 || (newWave - 1) % 5 == 0) buildShields(newWave) else state.shields
        return state.copy(
            wave = newWave,
            enemies = enemies,
            projectiles = emptyList(),
            shields = shields,
            autoFireTimer = 0f,
            waveIntroTimer = config.waveIntroDuration,
            noHitCurrentWave = true,
            nextEnemyId = state.nextEnemyId + enemies.size
        )
    }

    fun startFirstWave(state: SpaceDefenderState, config: SpaceDefenderConfig): SpaceDefenderState {
        val enemies = buildWave(1, config, 0)
        val shields = buildShields(1)
        return state.copy(
            wave = 1,
            enemies = enemies,
            projectiles = emptyList(),
            shields = shields,
            autoFireTimer = 0f,
            waveIntroTimer = config.waveIntroDuration,
            noHitCurrentWave = true,
            nextEnemyId = enemies.size
        )
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private fun enemyTypesForWave(wave: Int): List<EnemyType> = when {
        wave <= 3 -> if (wave == 1) listOf(EnemyType.SCOUT)
                     else listOf(EnemyType.SCOUT, EnemyType.GUNNER).take(1 + (wave - 1) % 2)
        wave <= 6 -> listOf(EnemyType.GUNNER, EnemyType.SNIPER).let {
            if (wave % 2 == 0) it else listOf(EnemyType.SCOUT) + it
        }.take(2)
        wave <= 10 -> buildList {
            add(EnemyType.GUNNER)
            add(EnemyType.SNIPER)
            if (wave >= 8) add(EnemyType.BOMBER)
        }.take(3)
        wave <= 15 -> buildList {
            add(EnemyType.SNIPER)
            add(EnemyType.BOMBER)
            if (wave >= 12) add(EnemyType.COMMANDER)
        }.take(3)
        else -> listOf(EnemyType.GUNNER, EnemyType.BOMBER, EnemyType.COMMANDER)
    }

    private fun waveSpeedMultiplier(wave: Int, type: EnemyType): Float {
        val waveBoost = 1f + (wave - 1) * 0.06f
        val typeBase = when (type) {
            EnemyType.SCOUT -> 1.4f
            EnemyType.GUNNER -> 1.1f
            EnemyType.SNIPER -> 0.7f
            EnemyType.BOMBER -> 0.6f
            EnemyType.COMMANDER -> 0.9f
        }
        return typeBase * waveBoost
    }

    private fun buildEnemyShots(
        enemy: Enemy,
        playerPos: Vec2,
        config: SpaceDefenderConfig,
        startId: Int
    ): List<Projectile> {
        val speed = config.enemyProjectileSpeed
        val origin = enemy.position + Vec2(0f, 0.04f)
        return when (enemy.type) {
            EnemyType.SCOUT -> {
                // Single fast shot aimed at player
                val dir = (playerPos - origin).normalized()
                listOf(Projectile(startId, origin, dir * speed, ProjectileOwner.ENEMY))
            }
            EnemyType.GUNNER -> {
                // 3-shot spread
                val angles = listOf(-0.25f, 0f, 0.25f)
                angles.mapIndexed { i, angle ->
                    val base = (playerPos - origin).normalized()
                    val spread = Vec2(base.x + angle, base.y).normalized()
                    Projectile(startId + i, origin, spread * speed, ProjectileOwner.ENEMY)
                }
            }
            EnemyType.SNIPER -> {
                // Single homing shot
                listOf(
                    Projectile(
                        startId, origin,
                        Vec2(0f, speed),
                        ProjectileOwner.ENEMY,
                        homing = true,
                        homingTarget = playerPos
                    )
                )
            }
            EnemyType.BOMBER -> {
                // Vertical drop + wide spread
                val dropShot = Projectile(startId, origin, Vec2(0f, speed), ProjectileOwner.ENEMY)
                val leftSpread = Projectile(startId + 1, origin, Vec2(-speed * 0.6f, speed * 0.8f).normalized() * speed, ProjectileOwner.ENEMY)
                val rightSpread = Projectile(startId + 2, origin, Vec2(speed * 0.6f, speed * 0.8f).normalized() * speed, ProjectileOwner.ENEMY)
                listOf(dropShot, leftSpread, rightSpread)
            }
            EnemyType.COMMANDER -> {
                // 5-way burst
                val spreadAngles = listOf(-0.5f, -0.25f, 0f, 0.25f, 0.5f)
                spreadAngles.mapIndexed { i, angle ->
                    val base = (playerPos - origin).normalized()
                    val dir = Vec2(base.x + angle, base.y).normalized()
                    Projectile(startId + i, origin, dir * speed, ProjectileOwner.ENEMY)
                }
            }
        }
    }

    /** Stars earned for a completed game (1–3 based on wave reached and lives remaining). */
    fun starsEarned(wave: Int, lives: Int): Int = when {
        wave >= 10 && lives == 3 -> 3
        wave >= 6 -> 2
        else -> 1
    }
}
