package com.xanticious.androidgames.games.spacedefender

import com.xanticious.androidgames.controller.games.spacedefender.SpaceDefenderController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.spacedefender.Enemy
import com.xanticious.androidgames.model.games.spacedefender.EnemyBehavior
import com.xanticious.androidgames.model.games.spacedefender.EnemyType
import com.xanticious.androidgames.model.games.spacedefender.Projectile
import com.xanticious.androidgames.model.games.spacedefender.ProjectileOwner
import com.xanticious.androidgames.model.games.spacedefender.Shield
import com.xanticious.androidgames.model.games.spacedefender.ShieldHealth
import com.xanticious.androidgames.model.games.spacedefender.SpaceDefenderEvent
import com.xanticious.androidgames.model.games.spacedefender.SpaceDefenderInput
import com.xanticious.androidgames.model.games.spacedefender.SpaceDefenderState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceDefenderControllerTest {

    private val controller = SpaceDefenderController()
    private val config = controller.configFor(GameDifficulty.MEDIUM)
    private val noInput = SpaceDefenderInput(joystickDx = 0f)

    // -----------------------------------------------------------------------
    // Config
    // -----------------------------------------------------------------------

    @Test
    fun configFor_easy_hasSlowerEnemyThanHard() {
        val easy = controller.configFor(GameDifficulty.EASY)
        val hard = controller.configFor(GameDifficulty.HARD)
        assertTrue(easy.enemyBaseSpeed < hard.enemyBaseSpeed)
    }

    @Test
    fun configFor_hard_hasFasterProjectilesThanEasy() {
        val easy = controller.configFor(GameDifficulty.EASY)
        val hard = controller.configFor(GameDifficulty.HARD)
        assertTrue(easy.enemyProjectileSpeed < hard.enemyProjectileSpeed)
    }

    // -----------------------------------------------------------------------
    // Player movement
    // -----------------------------------------------------------------------

    @Test
    fun step_joystickRight_movesPlayerRight() {
        val state = SpaceDefenderState.initial()
        val result = controller.step(state, config, dt = 0.1f, input = SpaceDefenderInput(joystickDx = 1f))
        assertTrue(result.state.playerX > state.playerX)
    }

    @Test
    fun step_joystickLeft_movesPlayerLeft() {
        val state = SpaceDefenderState.initial()
        val result = controller.step(state, config, dt = 0.1f, input = SpaceDefenderInput(joystickDx = -1f))
        assertTrue(result.state.playerX < state.playerX)
    }

    @Test
    fun step_playerCannotMoveOutOfBoundsRight() {
        val state = SpaceDefenderState.initial().copy(playerX = 0.98f)
        val result = controller.step(state, config, dt = 0.5f, input = SpaceDefenderInput(joystickDx = 1f))
        assertTrue(result.state.playerX <= 1f - SpaceDefenderState.PLAYER_HALF_WIDTH)
    }

    @Test
    fun step_playerCannotMoveOutOfBoundsLeft() {
        val state = SpaceDefenderState.initial().copy(playerX = 0.02f)
        val result = controller.step(state, config, dt = 0.5f, input = SpaceDefenderInput(joystickDx = -1f))
        assertTrue(result.state.playerX >= SpaceDefenderState.PLAYER_HALF_WIDTH)
    }

    // -----------------------------------------------------------------------
    // Auto-fire
    // -----------------------------------------------------------------------

    @Test
    fun step_autoFireSpawnsProjectileWhenTimerExpires() {
        val state = SpaceDefenderState.initial().copy(autoFireTimer = 0f)
        val result = controller.step(state, config, dt = 0.01f, noInput)
        assertTrue(result.state.projectiles.any { it.owner == ProjectileOwner.PLAYER })
    }

    @Test
    fun step_playerProjectileMovesUpward() {
        val state = SpaceDefenderState.initial().copy(autoFireTimer = 0f)
        val after = controller.step(state, config, dt = 0.01f, noInput)
        val proj = after.state.projectiles.first { it.owner == ProjectileOwner.PLAYER }
        assertTrue(proj.velocity.y < 0f)
    }

    @Test
    fun step_autoFireDoesNotSpawnWhenTimerPositive() {
        val state = SpaceDefenderState.initial().copy(autoFireTimer = 5f, projectiles = emptyList())
        val result = controller.step(state, config, dt = 0.01f, noInput)
        assertTrue(result.state.projectiles.isEmpty())
    }

    // -----------------------------------------------------------------------
    // Projectile-enemy collision
    // -----------------------------------------------------------------------

    @Test
    fun step_projectileHitsEnemy_reducesEnemyHp() {
        val enemy = Enemy(
            id = 0, type = EnemyType.SCOUT,
            position = Vec2(0.5f, 0.3f), velocity = Vec2.ZERO,
            hp = 3, behavior = EnemyBehavior.NEUTRAL, fireCooldown = 99f
        )
        val proj = Projectile(
            id = 0, position = Vec2(0.5f, 0.32f),
            velocity = Vec2(0f, -0.5f), owner = ProjectileOwner.PLAYER
        )
        val state = SpaceDefenderState.initial().copy(
            enemies = listOf(enemy), projectiles = listOf(proj), autoFireTimer = 99f
        )
        val result = controller.step(state, config, dt = 0.01f, noInput)
        val remaining = result.state.enemies
        // Either enemy HP was reduced or enemy was destroyed
        val hpReduced = remaining.firstOrNull()?.hp?.let { it < 3 } ?: true
        assertTrue(hpReduced)
    }

    @Test
    fun step_projectileDestroysEnemy_emitsEnemyDestroyedEvent() {
        val enemy = Enemy(
            id = 0, type = EnemyType.SCOUT,
            position = Vec2(0.5f, 0.3f), velocity = Vec2.ZERO,
            hp = 1, behavior = EnemyBehavior.NEUTRAL, fireCooldown = 99f
        )
        val proj = Projectile(
            id = 0, position = Vec2(0.5f, 0.32f),
            velocity = Vec2(0f, -0.5f), owner = ProjectileOwner.PLAYER
        )
        val state = SpaceDefenderState.initial().copy(
            enemies = listOf(enemy), projectiles = listOf(proj), autoFireTimer = 99f
        )
        val result = controller.step(state, config, dt = 0.01f, noInput)
        assertTrue(result.events.any { it is SpaceDefenderEvent.EnemyDestroyed })
    }

    @Test
    fun step_destroyingScout_addsCorrectScore() {
        val enemy = Enemy(
            id = 0, type = EnemyType.SCOUT,
            position = Vec2(0.5f, 0.3f), velocity = Vec2.ZERO,
            hp = 1, behavior = EnemyBehavior.NEUTRAL, fireCooldown = 99f
        )
        val proj = Projectile(
            id = 0, position = Vec2(0.5f, 0.32f),
            velocity = Vec2(0f, -0.5f), owner = ProjectileOwner.PLAYER
        )
        // A second enemy that survives the step, so the wave is not cleared
        // (which would add a wave-clear bonus and mask the per-enemy score).
        val survivor = Enemy(
            id = 99, type = EnemyType.SCOUT,
            position = Vec2(0.1f, 0.1f), velocity = Vec2.ZERO,
            hp = 5, behavior = EnemyBehavior.NEUTRAL, fireCooldown = 99f
        )
        val state = SpaceDefenderState.initial().copy(
            enemies = listOf(enemy, survivor), projectiles = listOf(proj), autoFireTimer = 99f, score = 0
        )
        val result = controller.step(state, config, dt = 0.01f, noInput)
        assertEquals(100, result.state.score)
    }

    @Test
    fun step_destroyingCommander_addsHighestScore() {
        val enemy = Enemy(
            id = 0, type = EnemyType.COMMANDER,
            position = Vec2(0.5f, 0.3f), velocity = Vec2.ZERO,
            hp = 1, behavior = EnemyBehavior.NEUTRAL, fireCooldown = 99f
        )
        val proj = Projectile(
            id = 0, position = Vec2(0.5f, 0.32f),
            velocity = Vec2(0f, -0.5f), owner = ProjectileOwner.PLAYER
        )
        // A second enemy that survives the step, so the wave is not cleared
        // (which would add a wave-clear bonus and mask the per-enemy score).
        val survivor = Enemy(
            id = 99, type = EnemyType.SCOUT,
            position = Vec2(0.1f, 0.1f), velocity = Vec2.ZERO,
            hp = 5, behavior = EnemyBehavior.NEUTRAL, fireCooldown = 99f
        )
        val state = SpaceDefenderState.initial().copy(
            enemies = listOf(enemy, survivor), projectiles = listOf(proj), autoFireTimer = 99f, score = 0
        )
        val result = controller.step(state, config, dt = 0.01f, noInput)
        assertEquals(600, result.state.score)
    }

    @Test
    fun step_lastEnemyDestroyed_emitsAllEnemiesDestroyedEvent() {
        val enemy = Enemy(
            id = 0, type = EnemyType.SCOUT,
            position = Vec2(0.5f, 0.3f), velocity = Vec2.ZERO,
            hp = 1, behavior = EnemyBehavior.NEUTRAL, fireCooldown = 99f
        )
        val proj = Projectile(
            id = 0, position = Vec2(0.5f, 0.32f),
            velocity = Vec2(0f, -0.5f), owner = ProjectileOwner.PLAYER
        )
        val state = SpaceDefenderState.initial().copy(
            enemies = listOf(enemy), projectiles = listOf(proj), autoFireTimer = 99f
        )
        val result = controller.step(state, config, dt = 0.01f, noInput)
        assertTrue(result.events.any { it == SpaceDefenderEvent.AllEnemiesDestroyed })
    }

    // -----------------------------------------------------------------------
    // Wave clear bonus scoring
    // -----------------------------------------------------------------------

    @Test
    fun step_waveClearedWithFullLives_addsLifeBonus() {
        val enemy = Enemy(
            id = 0, type = EnemyType.SCOUT,
            position = Vec2(0.5f, 0.3f), velocity = Vec2.ZERO,
            hp = 1, behavior = EnemyBehavior.NEUTRAL, fireCooldown = 99f
        )
        val proj = Projectile(
            id = 0, position = Vec2(0.5f, 0.32f),
            velocity = Vec2(0f, -0.5f), owner = ProjectileOwner.PLAYER
        )
        val state = SpaceDefenderState.initial().copy(
            enemies = listOf(enemy), projectiles = listOf(proj),
            lives = 3, score = 0, autoFireTimer = 99f
        )
        val result = controller.step(state, config, dt = 0.01f, noInput)
        // 100 (Scout) + 500 (full lives) = 600 (shields destroyed so no shield bonus)
        assertTrue(result.state.score >= 600)
    }

    // -----------------------------------------------------------------------
    // Player hit / invincibility
    // -----------------------------------------------------------------------

    @Test
    fun step_enemyProjectileHitsPlayer_reducesLives() {
        val proj = Projectile(
            id = 0,
            position = Vec2(0.5f, SpaceDefenderState.PLAYER_Y),
            velocity = Vec2(0f, 0.5f),
            owner = ProjectileOwner.ENEMY
        )
        val state = SpaceDefenderState.initial().copy(
            projectiles = listOf(proj), lives = 3, autoFireTimer = 99f
        )
        val result = controller.step(state, config, dt = 0.01f, noInput)
        assertEquals(2, result.state.lives)
    }

    @Test
    fun step_playerHitWhileInvincible_doesNotReduceLives() {
        val proj = Projectile(
            id = 0,
            position = Vec2(0.5f, SpaceDefenderState.PLAYER_Y),
            velocity = Vec2(0f, 0.5f),
            owner = ProjectileOwner.ENEMY
        )
        val state = SpaceDefenderState.initial().copy(
            projectiles = listOf(proj), lives = 3, invincibilityTimer = 1.0f, autoFireTimer = 99f
        )
        val result = controller.step(state, config, dt = 0.01f, noInput)
        assertEquals(3, result.state.lives)
    }

    @Test
    fun step_livesReachZero_emitsGameOverEvent() {
        val proj = Projectile(
            id = 0,
            position = Vec2(0.5f, SpaceDefenderState.PLAYER_Y),
            velocity = Vec2(0f, 0.5f),
            owner = ProjectileOwner.ENEMY
        )
        val state = SpaceDefenderState.initial().copy(
            projectiles = listOf(proj), lives = 1, autoFireTimer = 99f
        )
        val result = controller.step(state, config, dt = 0.01f, noInput)
        assertTrue(result.events.any { it == SpaceDefenderEvent.GameOver })
    }

    @Test
    fun step_playerHitGrantsInvincibility() {
        val proj = Projectile(
            id = 0,
            position = Vec2(0.5f, SpaceDefenderState.PLAYER_Y),
            velocity = Vec2(0f, 0.5f),
            owner = ProjectileOwner.ENEMY
        )
        val state = SpaceDefenderState.initial().copy(
            projectiles = listOf(proj), lives = 3, autoFireTimer = 99f
        )
        val result = controller.step(state, config, dt = 0.01f, noInput)
        assertTrue(result.state.invincibilityTimer > 0f)
    }

    // -----------------------------------------------------------------------
    // Wave progression
    // -----------------------------------------------------------------------

    @Test
    fun buildWave_wave1_spawnsOneEnemy() {
        val enemies = controller.buildWave(1, config, 0)
        assertEquals(1, enemies.size)
    }

    @Test
    fun buildWave_wave5_spawnsTwoEnemies() {
        val enemies = controller.buildWave(5, config, 0)
        assertEquals(2, enemies.size)
    }

    @Test
    fun buildWave_wave9_spawnsThreeEnemies() {
        val enemies = controller.buildWave(9, config, 0)
        assertEquals(3, enemies.size)
    }

    @Test
    fun startNextWave_incrementsWaveNumber() {
        val state = SpaceDefenderState.initial().copy(wave = 1)
        val next = controller.startNextWave(state, config)
        assertEquals(2, next.wave)
    }

    @Test
    fun startNextWave_clearsProjectiles() {
        val proj = Projectile(0, Vec2(0.5f, 0.5f), Vec2.ZERO, ProjectileOwner.PLAYER)
        val state = SpaceDefenderState.initial().copy(wave = 1, projectiles = listOf(proj))
        val next = controller.startNextWave(state, config)
        assertTrue(next.projectiles.isEmpty())
    }

    @Test
    fun buildWave_higherWave_hasHigherEnemySpeed() {
        val wave1Enemies = controller.buildWave(1, config, 0)
        val wave10Enemies = controller.buildWave(10, config, 0)
        val wave1Speed = wave1Enemies.first().velocity.length
        val wave10Speed = wave10Enemies.first().velocity.length
        assertTrue(wave10Speed > wave1Speed)
    }

    // -----------------------------------------------------------------------
    // Stars
    // -----------------------------------------------------------------------

    @Test
    fun starsEarned_wave10FullLives_returnsThree() {
        assertEquals(3, controller.starsEarned(wave = 10, lives = 3))
    }

    @Test
    fun starsEarned_wave3_returnsOne() {
        assertEquals(1, controller.starsEarned(wave = 3, lives = 2))
    }

    @Test
    fun starsEarned_wave6_returnsTwo() {
        assertEquals(2, controller.starsEarned(wave = 6, lives = 1))
    }

    // -----------------------------------------------------------------------
    // Auto-fire pause during invincibility
    // -----------------------------------------------------------------------

    @Test
    fun step_autoFire_pausedWhenInvincible() {
        val state = SpaceDefenderState.initial().copy(
            autoFireTimer = 0f,
            invincibilityTimer = 1.0f,
            projectiles = emptyList()
        )
        val result = controller.step(state, config, dt = 0.01f, noInput)
        assertFalse(result.state.projectiles.any { it.owner == ProjectileOwner.PLAYER })
    }

    @Test
    fun step_autoFire_resumesWhenInvincibilityExpires() {
        val state = SpaceDefenderState.initial().copy(
            autoFireTimer = 0f,
            invincibilityTimer = 0f,
            projectiles = emptyList()
        )
        val result = controller.step(state, config, dt = 0.01f, noInput)
        assertTrue(result.state.projectiles.any { it.owner == ProjectileOwner.PLAYER })
    }

    // -----------------------------------------------------------------------
    // Shield invulnerability
    // -----------------------------------------------------------------------

    @Test
    fun step_shield_healthUnchanged_whenHitByEnemyProjectile() {
        val shield = Shield(id = 0, position = Vec2(0.5f, 0.72f), health = ShieldHealth.FULL)
        val proj = Projectile(
            id = 0,
            position = Vec2(0.5f, 0.72f),
            velocity = Vec2(0f, 0.5f),
            owner = ProjectileOwner.ENEMY
        )
        val state = SpaceDefenderState.initial().copy(
            shields = listOf(shield),
            projectiles = listOf(proj),
            autoFireTimer = 99f
        )
        val result = controller.step(state, config, dt = 0.01f, noInput)
        val remainingShield = result.state.shields.first()
        assertEquals(ShieldHealth.FULL, remainingShield.health)
    }

    @Test
    fun step_shield_absorbsEnemyProjectile_removesIt() {
        val shield = Shield(id = 0, position = Vec2(0.5f, 0.72f), health = ShieldHealth.FULL)
        val proj = Projectile(
            id = 0,
            position = Vec2(0.5f, 0.72f),
            velocity = Vec2(0f, 0.5f),
            owner = ProjectileOwner.ENEMY
        )
        val state = SpaceDefenderState.initial().copy(
            shields = listOf(shield),
            projectiles = listOf(proj),
            autoFireTimer = 99f
        )
        val result = controller.step(state, config, dt = 0.01f, noInput)
        assertFalse(result.state.projectiles.any { it.id == proj.id })
    }

    @Test
    fun startNextWave_shieldsPreservedAcrossWaves() {
        val shield = Shield(id = 0, position = Vec2(0.5f, 0.72f), health = ShieldHealth.FULL)
        val state = SpaceDefenderState.initial().copy(wave = 1, shields = listOf(shield))
        val next = controller.startNextWave(state, config)
        assertEquals(listOf(shield), next.shields)
    }
}
