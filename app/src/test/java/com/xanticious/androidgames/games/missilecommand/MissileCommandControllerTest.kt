package com.xanticious.androidgames.games.missilecommand

import com.xanticious.androidgames.controller.games.missilecommand.MissileCommandController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.missilecommand.EnemyMissile
import com.xanticious.androidgames.model.games.missilecommand.Interceptor
import com.xanticious.androidgames.model.games.missilecommand.InterceptorPhase
import com.xanticious.androidgames.model.games.missilecommand.MissileCommandInput
import com.xanticious.androidgames.model.games.missilecommand.MissileCommandState
import com.xanticious.androidgames.model.games.missilecommand.MissileType
import com.xanticious.androidgames.model.games.missilecommand.Silo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MissileCommandControllerTest {

    private val controller = MissileCommandController()
    private val config = controller.configFor(GameDifficulty.MEDIUM)
    private val noInput = MissileCommandInput(tapTarget = null)

    // -------------------------------------------------------------------------
    // configFor
    // -------------------------------------------------------------------------

    @Test
    fun configFor_easy_hasLowerSpeedThanMedium() {
        val easy = controller.configFor(GameDifficulty.EASY)
        val medium = controller.configFor(GameDifficulty.MEDIUM)
        assertTrue(easy.missileBaseSpeed < medium.missileBaseSpeed)
    }

    @Test
    fun configFor_hard_hasHigherSpeedThanMedium() {
        val hard = controller.configFor(GameDifficulty.HARD)
        val medium = controller.configFor(GameDifficulty.MEDIUM)
        assertTrue(hard.missileBaseSpeed > medium.missileBaseSpeed)
    }

    // -------------------------------------------------------------------------
    // spawnWave
    // -------------------------------------------------------------------------

    @Test
    fun spawnWave_wave1_spawnsMissilesWave1Count() {
        val state = MissileCommandState.initial()
        val spawned = controller.spawnWave(state, config)
        assertEquals(config.missilesWave1, spawned.missiles.size)
    }

    @Test
    fun spawnWave_refillsAliveSiloAmmo() {
        val depletedSilos = MissileCommandState.SILO_POSITIONS.map { Silo(it, 0, alive = true) }
        val state = MissileCommandState.initial().copy(silos = depletedSilos)
        val spawned = controller.spawnWave(state, config)
        assertTrue(spawned.silos.all { it.ammo == Silo.FULL_AMMO })
    }

    @Test
    fun spawnWave_onWave3_repairsDestroyedSilo() {
        val brokenSilos = MissileCommandState.SILO_POSITIONS.map { Silo(it, 0, alive = false) }
        val state = MissileCommandState.initial().copy(silos = brokenSilos, wave = 3)
        val spawned = controller.spawnWave(state, config)
        assertTrue(spawned.silos.all { it.alive })
    }

    @Test
    fun spawnWave_wave2_spawnsMissilesMoreThanWave1() {
        val state1 = controller.spawnWave(MissileCommandState.initial(), config)
        val state2 = controller.spawnWave(MissileCommandState.initial().copy(wave = 2), config)
        assertTrue(state2.missiles.size > state1.missiles.size)
    }

    // -------------------------------------------------------------------------
    // launchInterceptor
    // -------------------------------------------------------------------------

    @Test
    fun launchInterceptor_depletsOneAmmoFromNearestSilo() {
        val state = MissileCommandState.initial()
        val target = MissileCommandState.SILO_POSITIONS[0] + Vec2(0f, -0.2f) // directly above left silo
        val after = controller.launchInterceptor(state, config, target)
        assertEquals(Silo.FULL_AMMO - 1, after.silos[0].ammo)
    }

    @Test
    fun launchInterceptor_addsInterceptorToList() {
        val state = MissileCommandState.initial()
        val after = controller.launchInterceptor(state, config, Vec2(0.5f, 0.4f))
        assertEquals(1, after.interceptors.size)
    }

    @Test
    fun launchInterceptor_noAmmoAvailable_returnsUnchangedState() {
        val emptySilos = MissileCommandState.SILO_POSITIONS.map { Silo(it, 0, alive = true) }
        val state = MissileCommandState.initial().copy(silos = emptySilos)
        val after = controller.launchInterceptor(state, config, Vec2(0.5f, 0.3f))
        assertTrue(after.interceptors.isEmpty())
    }

    @Test
    fun launchInterceptor_destroyedSilo_picksNextAvailable() {
        val silos = MissileCommandState.SILO_POSITIONS.mapIndexed { i, pos ->
            Silo(pos, if (i == 0) 0 else Silo.FULL_AMMO, alive = true)
        }
        val state = MissileCommandState.initial().copy(silos = silos)
        // Tap near left silo — it has no ammo, so center silo should fire.
        val target = Vec2(0.05f, 0.3f)
        val after = controller.launchInterceptor(state, config, target)
        assertEquals(0, after.silos[0].ammo)
        assertEquals(Silo.FULL_AMMO - 1, after.silos[1].ammo)
    }

    // -------------------------------------------------------------------------
    // Blast collision: interceptor kills missile in range
    // -------------------------------------------------------------------------

    @Test
    fun step_blastRadiusCoversEnemy_missileKilled() {
        val missilePos = Vec2(0.5f, 0.3f)
        val missile = EnemyMissile(
            id = 0, originPos = Vec2(0.5f, 0f), pos = missilePos,
            velocity = Vec2.ZERO, target = missilePos, alive = true, type = MissileType.STANDARD,
        )
        // Interceptor already exploding at the same position with full blast radius.
        val interceptor = Interceptor(
            id = 1, launchPos = missilePos, pos = missilePos, target = missilePos,
            velocity = Vec2.ZERO, phase = InterceptorPhase.EXPLODING,
            blastRadius = MissileCommandState.MAX_BLAST_RADIUS,
            maxBlastRadius = MissileCommandState.MAX_BLAST_RADIUS,
            blastAge = 0f,
        )
        val state = MissileCommandState.initial().copy(
            missiles = listOf(missile),
            interceptors = listOf(interceptor),
        )
        val result = controller.step(state, config, dt = 0.01f, noInput)
        assertFalse(result.state.missiles[0].alive)
    }

    @Test
    fun step_blastRadiusTooSmall_missileNotKilled() {
        val missilePos = Vec2(0.5f, 0.3f)
        val missile = EnemyMissile(
            id = 0, originPos = Vec2(0.5f, 0f), pos = missilePos,
            velocity = Vec2.ZERO, target = Vec2(0.5f, 1.0f), alive = true, type = MissileType.STANDARD,
        )
        // Interceptor exploding far away.
        val interceptor = Interceptor(
            id = 1, launchPos = Vec2(0.1f, 0.1f), pos = Vec2(0.1f, 0.1f), target = Vec2(0.1f, 0.1f),
            velocity = Vec2.ZERO, phase = InterceptorPhase.EXPLODING,
            blastRadius = 0.001f,
            maxBlastRadius = MissileCommandState.MAX_BLAST_RADIUS,
            blastAge = 0f,
        )
        val state = MissileCommandState.initial().copy(
            missiles = listOf(missile),
            interceptors = listOf(interceptor),
        )
        val result = controller.step(state, config, dt = 0.01f, noInput)
        assertTrue(result.state.missiles[0].alive)
    }

    @Test
    fun step_blastKillsMissile_scoreGained() {
        val missilePos = Vec2(0.5f, 0.3f)
        val missile = EnemyMissile(
            id = 0, originPos = Vec2(0.5f, 0f), pos = missilePos,
            velocity = Vec2.ZERO, target = missilePos, alive = true, type = MissileType.STANDARD,
        )
        val interceptor = Interceptor(
            id = 1, launchPos = missilePos, pos = missilePos, target = missilePos,
            velocity = Vec2.ZERO, phase = InterceptorPhase.EXPLODING,
            blastRadius = MissileCommandState.MAX_BLAST_RADIUS,
            maxBlastRadius = MissileCommandState.MAX_BLAST_RADIUS,
            blastAge = 0f,
        )
        val state = MissileCommandState.initial().copy(
            missiles = listOf(missile),
            interceptors = listOf(interceptor),
        )
        val result = controller.step(state, config, dt = 0.01f, noInput)
        assertTrue(result.scoreGained > 0)
    }

    @Test
    fun step_multiKillInOneBlast_earnsMoreThanSingleKill() {
        val center = Vec2(0.5f, 0.3f)
        fun missile(id: Int) = EnemyMissile(
            id = id, originPos = center, pos = center,
            velocity = Vec2.ZERO, target = center, alive = true, type = MissileType.STANDARD,
        )
        val interceptor = Interceptor(
            id = 99, launchPos = center, pos = center, target = center,
            velocity = Vec2.ZERO, phase = InterceptorPhase.EXPLODING,
            blastRadius = MissileCommandState.MAX_BLAST_RADIUS,
            maxBlastRadius = MissileCommandState.MAX_BLAST_RADIUS,
            blastAge = 0f,
        )
        val singleState = MissileCommandState.initial().copy(
            missiles = listOf(missile(0)),
            interceptors = listOf(interceptor),
        )
        val multiState = MissileCommandState.initial().copy(
            missiles = listOf(missile(0), missile(1), missile(2)),
            interceptors = listOf(interceptor),
        )
        val single = controller.step(singleState, config, 0.01f, noInput).scoreGained
        val multi = controller.step(multiState, config, 0.01f, noInput).scoreGained
        assertTrue(multi > single)
    }

    // -------------------------------------------------------------------------
    // Ammo depletion
    // -------------------------------------------------------------------------

    @Test
    fun step_tapTarget_launchesInterceptorAndDepletesAmmo() {
        val state = MissileCommandState.initial()
        val tapPos = Vec2(0.5f, 0.3f)
        val result = controller.step(state, config, dt = 0.01f, MissileCommandInput(tapPos))
        assertTrue(result.state.interceptors.isNotEmpty())
        assertTrue(result.state.silos.any { it.ammo < Silo.FULL_AMMO })
    }

    @Test
    fun step_allAmmoGone_tapHasNoEffect() {
        val emptySilos = MissileCommandState.SILO_POSITIONS.map { Silo(it, 0, alive = true) }
        val state = MissileCommandState.initial().copy(silos = emptySilos)
        val result = controller.step(state, config, dt = 0.01f, MissileCommandInput(Vec2(0.5f, 0.3f)))
        assertTrue(result.state.interceptors.isEmpty())
    }

    // -------------------------------------------------------------------------
    // City destruction
    // -------------------------------------------------------------------------

    @Test
    fun step_missileArrivesAtCity_cityDestroyed() {
        val cityPos = MissileCommandState.CITY_POSITIONS[0]
        val missile = EnemyMissile(
            id = 0,
            originPos = Vec2(cityPos.x, 0f),
            pos = Vec2(cityPos.x, cityPos.y - 0.005f), // just above city
            velocity = Vec2(0f, 0.1f),
            target = cityPos,
            alive = true,
            type = MissileType.STANDARD,
        )
        val state = MissileCommandState.initial().copy(missiles = listOf(missile))
        val result = controller.step(state, config, dt = 0.1f, noInput)
        assertFalse(result.state.cities[0].alive)
    }

    @Test
    fun step_missileDestroysLastCity_gameOverTrue() {
        val cityPos = MissileCommandState.CITY_POSITIONS[0]
        // Only the first city is still standing; the incoming missile destroys it.
        val deadCities = MissileCommandState.CITY_POSITIONS.mapIndexed { i, pos ->
            com.xanticious.androidgames.model.games.missilecommand.City(pos, alive = i == 0)
        }
        val missile = EnemyMissile(
            id = 0,
            originPos = Vec2(cityPos.x, 0f),
            pos = Vec2(cityPos.x, cityPos.y - 0.005f),
            velocity = Vec2(0f, 0.1f),
            target = cityPos,
            alive = true,
            type = MissileType.STANDARD,
        )
        val state = MissileCommandState.initial().copy(
            cities = deadCities,
            missiles = listOf(missile),
        )
        val result = controller.step(state, config, dt = 0.1f, noInput)
        assertTrue(result.state.gameOver)
    }

    // -------------------------------------------------------------------------
    // Silo destruction
    // -------------------------------------------------------------------------

    @Test
    fun step_missileHitsSilo_siloDestroyed() {
        val siloPos = MissileCommandState.SILO_POSITIONS[1]
        val missile = EnemyMissile(
            id = 0,
            originPos = Vec2(siloPos.x, 0f),
            pos = Vec2(siloPos.x, siloPos.y - 0.005f),
            velocity = Vec2(0f, 0.1f),
            target = siloPos,
            alive = true,
            type = MissileType.STANDARD,
        )
        val state = MissileCommandState.initial().copy(missiles = listOf(missile))
        val result = controller.step(state, config, dt = 0.1f, noInput)
        assertFalse(result.state.silos[1].alive)
    }

    // -------------------------------------------------------------------------
    // Wave clear
    // -------------------------------------------------------------------------

    @Test
    fun step_allMissilesDeadAtStart_waveClearedImmediately() {
        val state = MissileCommandState.initial().copy(missiles = emptyList())
        val result = controller.step(state, config, dt = 0.016f, noInput)
        assertTrue(result.state.waveCleared)
    }

    @Test
    fun step_oneMissileAlive_waveNotCleared() {
        val missile = EnemyMissile(
            id = 0, originPos = Vec2(0.5f, 0f), pos = Vec2(0.5f, 0.3f),
            velocity = Vec2(0f, 0.05f), target = Vec2(0.5f, 0.9f),
            alive = true, type = MissileType.STANDARD,
        )
        val state = MissileCommandState.initial().copy(missiles = listOf(missile))
        val result = controller.step(state, config, dt = 0.016f, noInput)
        assertFalse(result.state.waveCleared)
    }

    @Test
    fun step_waveClear_awardsUnusedAmmoBonus() {
        val silos = MissileCommandState.SILO_POSITIONS.map { Silo(it, Silo.FULL_AMMO, alive = true) }
        val state = MissileCommandState.initial().copy(silos = silos, missiles = emptyList())
        val result = controller.step(state, config, dt = 0.016f, noInput)
        // Unused ammo bonus: 30 missiles * 5 = 150 + city/silo bonuses
        assertTrue(result.scoreGained >= 150)
    }

    @Test
    fun step_waveClear_awardsAliveCity100EachBonus() {
        val state = MissileCommandState.initial().copy(missiles = emptyList())
        val result = controller.step(state, config, dt = 0.016f, noInput)
        // 6 cities * 100 = 600 from city bonus alone
        assertTrue(result.scoreGained >= 600)
    }

    // -------------------------------------------------------------------------
    // Game over
    // -------------------------------------------------------------------------

    @Test
    fun step_gameOverState_returnsImmediatelyWithNoChanges() {
        val state = MissileCommandState.initial().copy(gameOver = true, score = 500)
        val result = controller.step(state, config, dt = 0.016f, noInput)
        assertEquals(500, result.state.score)
        assertEquals(0, result.scoreGained)
    }

    // -------------------------------------------------------------------------
    // Scoring
    // -------------------------------------------------------------------------

    @Test
    fun missileScore_standard_returns25() {
        assertEquals(25, controller.missileScore(MissileType.STANDARD, hasSplit = false))
    }

    @Test
    fun missileScore_fast_returns50() {
        assertEquals(50, controller.missileScore(MissileType.FAST, hasSplit = false))
    }

    @Test
    fun missileScore_mirvBeforeSplit_returns125() {
        assertEquals(125, controller.missileScore(MissileType.MIRV, hasSplit = false))
    }

    @Test
    fun missileScore_mirvAfterSplit_returns25() {
        assertEquals(25, controller.missileScore(MissileType.MIRV, hasSplit = true))
    }

    @Test
    fun missileScore_smart_returns100() {
        assertEquals(100, controller.missileScore(MissileType.SMART, hasSplit = false))
    }
}
