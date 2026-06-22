package com.xanticious.androidgames.games.helicopterdogfight

import com.xanticious.androidgames.controller.games.helicopterdogfight.HelicopterDogfightController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.helicopterdogfight.HeliEnemy
import com.xanticious.androidgames.model.games.helicopterdogfight.HeliEnemyType
import com.xanticious.androidgames.model.games.helicopterdogfight.HeliProjectile
import com.xanticious.androidgames.model.games.helicopterdogfight.HeliProjectileOwner
import com.xanticious.androidgames.model.games.helicopterdogfight.HelicopterDogfightEvent
import com.xanticious.androidgames.model.games.helicopterdogfight.HelicopterDogfightInput
import com.xanticious.androidgames.model.games.helicopterdogfight.HelicopterDogfightState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HelicopterDogfightControllerTest {

    private val controller = HelicopterDogfightController()
    private val config = controller.configFor(GameDifficulty.MEDIUM)
    private val noInput = HelicopterDogfightInput(joystickDx = 0f, joystickDy = 0f)

    // -----------------------------------------------------------------------
    // Config
    // -----------------------------------------------------------------------

    @Test
    fun configFor_hard_hasMoreEnemiesThanEasy() {
        val easy = controller.configFor(GameDifficulty.EASY)
        val hard = controller.configFor(GameDifficulty.HARD)
        assertTrue(easy.enemyCountPerScreen < hard.enemyCountPerScreen)
    }

    @Test
    fun configFor_hard_hasFasterEnemyProjectilesThanEasy() {
        val easy = controller.configFor(GameDifficulty.EASY)
        val hard = controller.configFor(GameDifficulty.HARD)
        assertTrue(easy.enemyProjectileSpeed < hard.enemyProjectileSpeed)
    }

    // -----------------------------------------------------------------------
    // Player movement — clamped to bounds
    // -----------------------------------------------------------------------

    @Test
    fun heliMovement_joystickUp_movesPlayerUp() {
        val state = HelicopterDogfightState.initial().copy(autoFireTimer = 1f)
        val result = controller.step(state, config, dt = 0.1f, input = HelicopterDogfightInput(0f, -1f))
        assertTrue(result.state.playerPos.y < state.playerPos.y)
    }

    @Test
    fun heliMovement_joystickDown_movesPlayerDown() {
        val state = HelicopterDogfightState.initial().copy(autoFireTimer = 1f)
        val result = controller.step(state, config, dt = 0.1f, input = HelicopterDogfightInput(0f, 1f))
        assertTrue(result.state.playerPos.y > state.playerPos.y)
    }

    @Test
    fun heliMovement_joystickRight_movesPlayerRight() {
        val state = HelicopterDogfightState.initial().copy(autoFireTimer = 1f)
        val result = controller.step(state, config, dt = 0.1f, input = HelicopterDogfightInput(1f, 0f))
        assertTrue(result.state.playerPos.x > state.playerPos.x)
    }

    @Test
    fun heliMovement_clampedToTopBound() {
        val state = HelicopterDogfightState.initial()
            .copy(playerPos = Vec2(0.15f, HelicopterDogfightState.MIN_PLAYER_Y), autoFireTimer = 1f)
        val result = controller.step(state, config, dt = 0.5f, input = HelicopterDogfightInput(0f, -1f))
        assertTrue(result.state.playerPos.y >= HelicopterDogfightState.MIN_PLAYER_Y)
    }

    @Test
    fun heliMovement_clampedToBottomBound() {
        val state = HelicopterDogfightState.initial()
            .copy(playerPos = Vec2(0.15f, HelicopterDogfightState.MAX_PLAYER_Y), autoFireTimer = 1f)
        val result = controller.step(state, config, dt = 0.5f, input = HelicopterDogfightInput(0f, 1f))
        assertTrue(result.state.playerPos.y <= HelicopterDogfightState.MAX_PLAYER_Y)
    }

    @Test
    fun heliMovement_clampedToLeftBound() {
        val state = HelicopterDogfightState.initial()
            .copy(playerPos = Vec2(HelicopterDogfightState.MIN_PLAYER_X, 0.5f), autoFireTimer = 1f)
        val result = controller.step(state, config, dt = 0.5f, input = HelicopterDogfightInput(-1f, 0f))
        assertTrue(result.state.playerPos.x >= HelicopterDogfightState.MIN_PLAYER_X)
    }

    @Test
    fun heliMovement_clampedToRightBound() {
        val state = HelicopterDogfightState.initial()
            .copy(playerPos = Vec2(HelicopterDogfightState.MAX_PLAYER_X, 0.5f), autoFireTimer = 1f)
        val result = controller.step(state, config, dt = 0.5f, input = HelicopterDogfightInput(1f, 0f))
        assertTrue(result.state.playerPos.x <= HelicopterDogfightState.MAX_PLAYER_X)
    }

    // -----------------------------------------------------------------------
    // Auto-fire cadence
    // -----------------------------------------------------------------------

    @Test
    fun autoFire_spawnsProjectileWhenTimerExpires() {
        // autoFireTimer = 0 → fires on first step
        val state = HelicopterDogfightState.initial().copy(autoFireTimer = 0f, projectiles = emptyList())
        val result = controller.step(state, config, dt = 0.01f, input = noInput)
        assertTrue(result.state.projectiles.any { it.owner == HeliProjectileOwner.PLAYER })
    }

    @Test
    fun autoFire_doesNotFireBeforeTimerExpires() {
        val state = HelicopterDogfightState.initial()
            .copy(autoFireTimer = 0.5f, projectiles = emptyList())
        val result = controller.step(state, config, dt = 0.01f, input = noInput)
        assertTrue(result.state.projectiles.none { it.owner == HeliProjectileOwner.PLAYER })
    }

    @Test
    fun autoFire_playerBulletTravelsRight() {
        val state = HelicopterDogfightState.initial().copy(autoFireTimer = 0f, projectiles = emptyList())
        val result = controller.step(state, config, dt = 0.01f, input = noInput)
        val bullet = result.state.projectiles.firstOrNull { it.owner == HeliProjectileOwner.PLAYER }
        requireNotNull(bullet)
        assertTrue(bullet.velocity.x > 0f)
    }

    @Test
    fun autoFire_timerResetAfterFire() {
        val state = HelicopterDogfightState.initial().copy(autoFireTimer = 0f)
        val result = controller.step(state, config, dt = 0.01f, input = noInput)
        assertTrue(result.state.autoFireTimer > 0f)
    }

    // -----------------------------------------------------------------------
    // Bullet vs enemy: HP reduction and destruction
    // -----------------------------------------------------------------------

    @Test
    fun bulletVsEnemy_hit_reducesEnemyHp() {
        val enemy = HeliEnemy(
            id = 0, type = HeliEnemyType.GROUND_TURRET,
            position = Vec2(0.6f, 0.5f), velocity = Vec2.ZERO,
            hp = 3, fireCooldown = 999f
        )
        val bullet = HeliProjectile(
            id = 0, position = Vec2(0.57f, 0.5f),
            velocity = Vec2(0.5f, 0f), owner = HeliProjectileOwner.PLAYER
        )
        val state = HelicopterDogfightState.initial().copy(
            enemies = listOf(enemy),
            projectiles = listOf(bullet),
            autoFireTimer = 1f
        )
        val result = controller.step(state, config, dt = 0.1f, input = noInput)
        val surviving = result.state.enemies.firstOrNull()
        requireNotNull(surviving)
        assertEquals(2, surviving.hp)
    }

    @Test
    fun bulletVsEnemy_lethalHit_destroysEnemy() {
        val enemy = HeliEnemy(
            id = 0, type = HeliEnemyType.GROUND_TURRET,
            position = Vec2(0.6f, 0.5f), velocity = Vec2.ZERO,
            hp = 1, fireCooldown = 999f
        )
        val bullet = HeliProjectile(
            id = 0, position = Vec2(0.57f, 0.5f),
            velocity = Vec2(0.5f, 0f), owner = HeliProjectileOwner.PLAYER
        )
        val state = HelicopterDogfightState.initial().copy(
            enemies = listOf(enemy),
            projectiles = listOf(bullet),
            autoFireTimer = 1f
        )
        val result = controller.step(state, config, dt = 0.1f, input = noInput)
        assertTrue(result.state.enemies.isEmpty())
    }

    @Test
    fun bulletVsEnemy_lethalHit_addsScore() {
        val enemy = HeliEnemy(
            id = 0, type = HeliEnemyType.GROUND_TURRET,
            position = Vec2(0.6f, 0.5f), velocity = Vec2.ZERO,
            hp = 1, fireCooldown = 999f
        )
        val bullet = HeliProjectile(
            id = 0, position = Vec2(0.57f, 0.5f),
            velocity = Vec2(0.5f, 0f), owner = HeliProjectileOwner.PLAYER
        )
        val state = HelicopterDogfightState.initial().copy(
            enemies = listOf(enemy),
            projectiles = listOf(bullet),
            autoFireTimer = 1f,
            score = 0
        )
        val result = controller.step(state, config, dt = 0.1f, input = noInput)
        assertTrue(result.state.score >= HeliEnemyType.GROUND_TURRET.scoreValue)
    }

    @Test
    fun bulletVsEnemy_lethalHit_emitsEnemyDestroyedEvent() {
        val enemy = HeliEnemy(
            id = 0, type = HeliEnemyType.AA_GUN,
            position = Vec2(0.6f, 0.5f), velocity = Vec2.ZERO,
            hp = 1, fireCooldown = 999f
        )
        val bullet = HeliProjectile(
            id = 0, position = Vec2(0.57f, 0.5f),
            velocity = Vec2(0.5f, 0f), owner = HeliProjectileOwner.PLAYER
        )
        val state = HelicopterDogfightState.initial().copy(
            enemies = listOf(enemy),
            projectiles = listOf(bullet),
            autoFireTimer = 1f
        )
        val result = controller.step(state, config, dt = 0.1f, input = noInput)
        assertTrue(result.events.any { it is HelicopterDogfightEvent.EnemyDestroyed })
    }

    // -----------------------------------------------------------------------
    // All enemies destroyed → AllEnemiesDestroyed event
    // -----------------------------------------------------------------------

    @Test
    fun allEnemiesDestroyed_emitsAllEnemiesDestroyedEvent() {
        val enemy = HeliEnemy(
            id = 0, type = HeliEnemyType.GROUND_TURRET,
            position = Vec2(0.6f, 0.5f), velocity = Vec2.ZERO,
            hp = 1, fireCooldown = 999f
        )
        val bullet = HeliProjectile(
            id = 0, position = Vec2(0.57f, 0.5f),
            velocity = Vec2(0.5f, 0f), owner = HeliProjectileOwner.PLAYER
        )
        val state = HelicopterDogfightState.initial().copy(
            enemies = listOf(enemy),
            projectiles = listOf(bullet),
            autoFireTimer = 1f
        )
        val result = controller.step(state, config, dt = 0.1f, input = noInput)
        assertTrue(result.events.contains(HelicopterDogfightEvent.AllEnemiesDestroyed))
    }

    @Test
    fun allEnemiesDestroyed_noDamageBonus_addsFiveHundredPoints() {
        val enemy = HeliEnemy(
            id = 0, type = HeliEnemyType.GROUND_TURRET,
            position = Vec2(0.6f, 0.5f), velocity = Vec2.ZERO,
            hp = 1, fireCooldown = 999f
        )
        val bullet = HeliProjectile(
            id = 0, position = Vec2(0.57f, 0.5f),
            velocity = Vec2(0.5f, 0f), owner = HeliProjectileOwner.PLAYER
        )
        val state = HelicopterDogfightState.initial().copy(
            enemies = listOf(enemy),
            projectiles = listOf(bullet),
            autoFireTimer = 1f,
            noHitCurrentScreen = true,
            score = 0
        )
        val result = controller.step(state, config, dt = 0.1f, input = noInput)
        // score = enemy value (100) + no-damage bonus (500)
        assertEquals(600, result.state.score)
    }

    // -----------------------------------------------------------------------
    // Enemy projectile vs player
    // -----------------------------------------------------------------------

    @Test
    fun enemyProjectileVsPlayer_reducesPlayerHp() {
        val enemyProj = HeliProjectile(
            id = 0,
            position = HelicopterDogfightState.initial().playerPos,
            velocity = Vec2(-0.5f, 0f),
            owner = HeliProjectileOwner.ENEMY
        )
        val state = HelicopterDogfightState.initial().copy(
            playerHp = 3,
            playerLives = 3,
            invincibilityTimer = 0f,
            projectiles = listOf(enemyProj),
            autoFireTimer = 1f,
            enemies = listOf(
                HeliEnemy(1, HeliEnemyType.GROUND_TURRET, Vec2(0.8f, 0.5f), Vec2.ZERO, 5, 999f)
            )
        )
        val result = controller.step(state, config, dt = 0.01f, input = noInput)
        assertEquals(2, result.state.playerHp)
    }

    @Test
    fun enemyProjectileVsPlayer_emitsPlayerHitEvent() {
        val enemyProj = HeliProjectile(
            id = 0,
            position = HelicopterDogfightState.initial().playerPos,
            velocity = Vec2(-0.5f, 0f),
            owner = HeliProjectileOwner.ENEMY
        )
        val state = HelicopterDogfightState.initial().copy(
            playerHp = 3,
            playerLives = 3,
            invincibilityTimer = 0f,
            projectiles = listOf(enemyProj),
            autoFireTimer = 1f,
            enemies = listOf(
                HeliEnemy(1, HeliEnemyType.GROUND_TURRET, Vec2(0.8f, 0.5f), Vec2.ZERO, 5, 999f)
            )
        )
        val result = controller.step(state, config, dt = 0.01f, input = noInput)
        assertTrue(result.events.contains(HelicopterDogfightEvent.PlayerHit))
    }

    @Test
    fun enemyProjectileVsPlayer_lastHp_livesRemaining_emitsPlayerCrashed() {
        val enemyProj = HeliProjectile(
            id = 0,
            position = HelicopterDogfightState.initial().playerPos,
            velocity = Vec2(-0.5f, 0f),
            owner = HeliProjectileOwner.ENEMY
        )
        val state = HelicopterDogfightState.initial().copy(
            playerHp = 1,
            playerLives = 2,
            invincibilityTimer = 0f,
            projectiles = listOf(enemyProj),
            autoFireTimer = 1f,
            enemies = listOf(
                HeliEnemy(1, HeliEnemyType.GROUND_TURRET, Vec2(0.8f, 0.5f), Vec2.ZERO, 5, 999f)
            )
        )
        val result = controller.step(state, config, dt = 0.01f, input = noInput)
        assertTrue(result.events.contains(HelicopterDogfightEvent.PlayerCrashed))
    }

    @Test
    fun enemyProjectileVsPlayer_lastHp_lastLife_emitsGameOver() {
        val enemyProj = HeliProjectile(
            id = 0,
            position = HelicopterDogfightState.initial().playerPos,
            velocity = Vec2(-0.5f, 0f),
            owner = HeliProjectileOwner.ENEMY
        )
        val state = HelicopterDogfightState.initial().copy(
            playerHp = 1,
            playerLives = 1,
            invincibilityTimer = 0f,
            projectiles = listOf(enemyProj),
            autoFireTimer = 1f,
            enemies = listOf(
                HeliEnemy(1, HeliEnemyType.GROUND_TURRET, Vec2(0.8f, 0.5f), Vec2.ZERO, 5, 999f)
            )
        )
        val result = controller.step(state, config, dt = 0.01f, input = noInput)
        assertTrue(result.events.contains(HelicopterDogfightEvent.GameOver))
    }

    @Test
    fun playerInvincible_ignoresEnemyProjectile() {
        val enemyProj = HeliProjectile(
            id = 0,
            position = HelicopterDogfightState.initial().playerPos,
            velocity = Vec2(-0.5f, 0f),
            owner = HeliProjectileOwner.ENEMY
        )
        val state = HelicopterDogfightState.initial().copy(
            playerHp = 3,
            playerLives = 3,
            invincibilityTimer = 2.0f, // currently invincible
            projectiles = listOf(enemyProj),
            autoFireTimer = 1f,
            enemies = listOf(
                HeliEnemy(1, HeliEnemyType.GROUND_TURRET, Vec2(0.8f, 0.5f), Vec2.ZERO, 5, 999f)
            )
        )
        val result = controller.step(state, config, dt = 0.01f, input = noInput)
        assertEquals(3, result.state.playerHp)
        assertFalse(result.events.contains(HelicopterDogfightEvent.PlayerHit))
    }

    @Test
    fun playerLives_decrementedOnCrash() {
        val enemyProj = HeliProjectile(
            id = 0,
            position = HelicopterDogfightState.initial().playerPos,
            velocity = Vec2(-0.5f, 0f),
            owner = HeliProjectileOwner.ENEMY
        )
        val state = HelicopterDogfightState.initial().copy(
            playerHp = 1,
            playerLives = 3,
            invincibilityTimer = 0f,
            projectiles = listOf(enemyProj),
            autoFireTimer = 1f,
            enemies = listOf(
                HeliEnemy(1, HeliEnemyType.GROUND_TURRET, Vec2(0.8f, 0.5f), Vec2.ZERO, 5, 999f)
            )
        )
        val result = controller.step(state, config, dt = 0.01f, input = noInput)
        assertEquals(2, result.state.playerLives)
    }
}
