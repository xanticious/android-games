package com.xanticious.androidgames.controller.games.missilecommand

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.missilecommand.City
import com.xanticious.androidgames.model.games.missilecommand.EnemyMissile
import com.xanticious.androidgames.model.games.missilecommand.Interceptor
import com.xanticious.androidgames.model.games.missilecommand.InterceptorPhase
import com.xanticious.androidgames.model.games.missilecommand.MissileCommandConfig
import com.xanticious.androidgames.model.games.missilecommand.MissileCommandInput
import com.xanticious.androidgames.model.games.missilecommand.MissileCommandState
import com.xanticious.androidgames.model.games.missilecommand.MissileCommandStep
import com.xanticious.androidgames.model.games.missilecommand.MissileType
import com.xanticious.androidgames.model.games.missilecommand.Silo
import kotlin.math.min

/**
 * Pure Missile Command rules: interceptor physics, blast collisions, missile AI,
 * and wave/game-over logic. No Android or Compose imports — fully JVM unit-testable.
 *
 * All positions are normalised to [0,1] with y=0 at the top and y=1 at the bottom,
 * matching the screen coordinate convention used by the draw layer.
 */
class MissileCommandController {

    fun configFor(difficulty: GameDifficulty): MissileCommandConfig = when (difficulty) {
        GameDifficulty.EASY -> MissileCommandConfig(
            missileBaseSpeed = 0.055f,
            interceptorSpeed = 0.60f,
            missilesWave1 = 6,
            missilesPerWaveGain = 2,
            missileSpeedGainPerWave = 0.004f,
            fastMissileFromWave = 4,
            mirvFromWave = 8,
            smartBombFromWave = 12,
        )
        GameDifficulty.MEDIUM -> MissileCommandConfig(
            missileBaseSpeed = 0.085f,
            interceptorSpeed = 0.55f,
            missilesWave1 = 8,
            missilesPerWaveGain = 3,
            missileSpeedGainPerWave = 0.007f,
            fastMissileFromWave = 3,
            mirvFromWave = 7,
            smartBombFromWave = 10,
        )
        GameDifficulty.HARD -> MissileCommandConfig(
            missileBaseSpeed = 0.120f,
            interceptorSpeed = 0.50f,
            missilesWave1 = 10,
            missilesPerWaveGain = 4,
            missileSpeedGainPerWave = 0.010f,
            fastMissileFromWave = 2,
            mirvFromWave = 5,
            smartBombFromWave = 8,
        )
    }

    /** Prepares [state] for [state.wave], refilling silos and spawning the enemy salvo. */
    fun spawnWave(state: MissileCommandState, config: MissileCommandConfig): MissileCommandState {
        val wave = state.wave
        val refreshedSilos = state.silos.map { silo ->
            when {
                silo.alive -> silo.copy(ammo = Silo.FULL_AMMO)
                wave % 3 == 0 -> silo.copy(alive = true, ammo = Silo.FULL_AMMO) // repair on 3rd-wave multiples
                else -> silo
            }
        }

        val waveSpeed = config.missileBaseSpeed + config.missileSpeedGainPerWave * (wave - 1)
        val count = min(config.missilesWave1 + config.missilesPerWaveGain * (wave - 1), 30)

        val aliveCityPositions = state.cities.filter { it.alive }.map { it.pos }
        val aliveSimoPositions = state.silos.filter { it.alive }.map { it.pos }
        val possibleTargets = (aliveCityPositions + aliveSimoPositions)
            .ifEmpty { listOf(Vec2(0.5f, MissileCommandState.GROUND_Y)) }

        var nextId = state.nextId
        val missiles = (0 until count).map { i ->
            // Deterministic but spread start positions along the top
            val startX = ((i.toFloat() / count) * 0.85f + 0.075f
                    + ((i * 7 + wave * 3) % 100) * 0.001f).coerceIn(0.05f, 0.95f)
            val originPos = Vec2(startX, 0.0f)
            val target = possibleTargets[(i + wave) % possibleTargets.size]
            val type = chooseType(i, wave, config)
            val speed = typeSpeed(type, waveSpeed)
            val dir = (target - originPos).normalized()
            EnemyMissile(
                id = nextId++,
                originPos = originPos,
                pos = originPos,
                velocity = dir * speed,
                target = target,
                alive = true,
                type = type,
            )
        }

        return state.copy(
            silos = refreshedSilos,
            missiles = missiles,
            interceptors = emptyList(),
            nextId = nextId,
            waveCleared = false,
        )
    }

    /** Advances the game by [dt] seconds, processing the [input] tap if present. */
    fun step(
        state: MissileCommandState,
        config: MissileCommandConfig,
        dt: Float,
        input: MissileCommandInput,
    ): MissileCommandStep {
        if (state.gameOver || state.waveCleared) return MissileCommandStep(state, 0)

        var s = state
        var scoreGained = 0

        // 1. Player tap -> launch interceptor from the nearest silo with ammo.
        val tap = input.tapTarget
        if (tap != null && tap.y < MissileCommandState.GROUND_Y) {
            s = launchInterceptor(s, config, tap)
        }

        // 2. Advance interceptors (flying → exploding → done).
        s = advanceInterceptors(s, config, dt)

        // 3. Blast collisions: kill any missile inside an active explosion.
        val (withKills, killScore) = checkBlastCollisions(s)
        s = withKills
        scoreGained += killScore

        // 4. Move enemy missiles; MIRV parents split at mid-screen.
        val (withMoved, mirvChildren) = moveMissiles(s, config, dt)
        s = withMoved.copy(missiles = withMoved.missiles + mirvChildren)

        // 5. Resolve missile arrivals: destroy cities/silos they hit.
        s = checkArrivals(s)

        // 6. Discard fully-spent interceptors.
        s = s.copy(interceptors = s.interceptors.filter { it.phase != InterceptorPhase.DONE })

        // 7. Game-over takes precedence: if every city is gone it's over, even when
        //    the missile that destroyed the last city was also the last one in the wave.
        if (s.citiesAlive == 0) {
            s = s.copy(score = s.score + scoreGained, gameOver = true)
            return MissileCommandStep(s, scoreGained)
        }

        // 8. Wave-clear check: all missiles dead, no interceptors still flying/exploding.
        val allMissilesGone = s.missiles.none { it.alive }
        if (allMissilesGone) {
            val bonus = waveEndBonus(s)
            scoreGained += bonus
            s = s.copy(score = s.score + scoreGained, waveCleared = true)
            return MissileCommandStep(s, scoreGained)
        }

        // 9. Bonus city rebuild every 10 000 points.
        val scoreBefore = s.score
        val scoreAfter = scoreBefore + scoreGained
        val rebuilds = (scoreAfter / 10_000) - (scoreBefore / 10_000)
        val updatedCities = if (rebuilds > 0) rebuildCities(s.cities, rebuilds) else s.cities

        s = s.copy(score = scoreAfter, cities = updatedCities)
        return MissileCommandStep(s, scoreGained)
    }

    /**
     * Launches one interceptor from the silo closest to [target] that still has ammo.
     * Returns the state unchanged if no silo can fire.
     */
    fun launchInterceptor(
        state: MissileCommandState,
        config: MissileCommandConfig,
        target: Vec2,
    ): MissileCommandState {
        val siloEntry = state.silos
            .mapIndexed { i, silo -> i to silo }
            .filter { (_, silo) -> silo.alive && silo.ammo > 0 }
            .minByOrNull { (_, silo) -> silo.pos.distanceTo(target) }
            ?: return state

        val (siloIdx, siloData) = siloEntry
        val interceptor = Interceptor(
            id = state.nextId,
            launchPos = siloData.pos,
            pos = siloData.pos,
            target = target,
            velocity = (target - siloData.pos).normalized() * config.interceptorSpeed,
            phase = InterceptorPhase.FLYING,
            blastRadius = 0f,
            maxBlastRadius = MissileCommandState.MAX_BLAST_RADIUS,
            blastAge = 0f,
        )

        val newSilos = state.silos.toMutableList()
        newSilos[siloIdx] = siloData.copy(ammo = siloData.ammo - 1)

        return state.copy(
            silos = newSilos,
            interceptors = state.interceptors + interceptor,
            nextId = state.nextId + 1,
        )
    }

    fun missileScore(type: MissileType, hasSplit: Boolean): Int = when (type) {
        MissileType.STANDARD -> 25
        MissileType.FAST -> 50
        MissileType.MIRV -> if (!hasSplit) 125 else 25
        MissileType.SMART -> 100
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun advanceInterceptors(
        state: MissileCommandState,
        config: MissileCommandConfig,
        dt: Float,
    ): MissileCommandState {
        val expandDuration = MissileCommandState.MAX_BLAST_RADIUS / MissileCommandState.BLAST_EXPAND_SPEED
        val lifetime = expandDuration + MissileCommandState.BLAST_LINGER_SECONDS

        val updated = state.interceptors.map { it ->
            when (it.phase) {
                InterceptorPhase.FLYING -> {
                    val dist = it.target.distanceTo(it.pos)
                    val step = config.interceptorSpeed * dt
                    if (dist <= step) {
                        it.copy(pos = it.target, phase = InterceptorPhase.EXPLODING, blastRadius = 0f, blastAge = 0f)
                    } else {
                        it.copy(pos = it.pos + it.velocity * dt)
                    }
                }
                InterceptorPhase.EXPLODING -> {
                    val newRadius = (it.blastRadius + MissileCommandState.BLAST_EXPAND_SPEED * dt)
                        .coerceAtMost(it.maxBlastRadius)
                    val newAge = it.blastAge + dt
                    it.copy(
                        blastRadius = newRadius,
                        blastAge = newAge,
                        phase = if (newAge > lifetime) InterceptorPhase.DONE else InterceptorPhase.EXPLODING,
                    )
                }
                InterceptorPhase.DONE -> it
            }
        }
        return state.copy(interceptors = updated)
    }

    private fun checkBlastCollisions(state: MissileCommandState): Pair<MissileCommandState, Int> {
        val blasts = state.interceptors.filter { it.phase == InterceptorPhase.EXPLODING }
        if (blasts.isEmpty()) return state to 0

        // Group which missiles each blast kills (a missile can only be killed once).
        val alreadyKilled = mutableSetOf<Int>()
        var totalScore = 0

        blasts.forEach { blast ->
            val killed = state.missiles.filter { missile ->
                missile.alive && missile.id !in alreadyKilled
                        && missile.pos.distanceTo(blast.pos) <= blast.blastRadius
            }
            if (killed.isNotEmpty()) {
                val scores = killed.map { missileScore(it.type, it.hasSplit) }
                // Multi-kill bonus: first at 1×, each additional at 2×.
                totalScore += scores[0] + scores.drop(1).sumOf { it * 2 }
                alreadyKilled.addAll(killed.map { it.id })
            }
        }

        val updatedMissiles = state.missiles.map { missile ->
            if (missile.id in alreadyKilled) missile.copy(alive = false) else missile
        }
        return state.copy(missiles = updatedMissiles) to totalScore
    }

    private fun moveMissiles(
        state: MissileCommandState,
        config: MissileCommandConfig,
        dt: Float,
    ): Pair<MissileCommandState, List<EnemyMissile>> {
        val blasts = state.interceptors.filter { it.phase == InterceptorPhase.EXPLODING }
        val waveSpeed = config.missileBaseSpeed + config.missileSpeedGainPerWave * (state.wave - 1)
        val mirvChildren = mutableListOf<EnemyMissile>()
        var nextId = state.nextId

        val moved = state.missiles.map { missile ->
            if (!missile.alive) return@map missile

            // Smart-bomb evasion: nudge perpendicular when a blast is nearby.
            val vel = if (missile.type == MissileType.SMART
                && blasts.any { it.pos.distanceTo(missile.pos) < it.blastRadius * 2.5f }
            ) {
                val evade = Vec2(-missile.velocity.y, missile.velocity.x).normalized()
                (missile.velocity + evade * 0.03f).normalized() * missile.velocity.length
            } else missile.velocity

            val newPos = missile.pos + vel * dt

            // MIRV split at mid-descent.
            if (missile.type == MissileType.MIRV && !missile.hasSplit && newPos.y >= 0.45f) {
                val children = createMirvChildren(missile.copy(pos = newPos), nextId, waveSpeed,
                    state.cities, state.silos)
                mirvChildren.addAll(children)
                nextId += children.size
                return@map missile.copy(pos = newPos, velocity = vel, alive = false, hasSplit = true)
            }

            missile.copy(pos = newPos, velocity = vel)
        }

        return state.copy(missiles = moved, nextId = nextId) to mirvChildren
    }

    private fun checkArrivals(state: MissileCommandState): MissileCommandState {
        val cities = state.cities.toMutableList()
        val silos = state.silos.toMutableList()

        val updatedMissiles = state.missiles.map { missile ->
            if (!missile.alive) return@map missile

            val nearTarget = missile.pos.distanceTo(missile.target) <= MissileCommandState.ARRIVAL_THRESHOLD
            val hitGround = missile.pos.y >= MissileCommandState.GROUND_Y
            if (!nearTarget && !hitGround) return@map missile

            // Destroy a city if close enough.
            val cityIdx = cities.indexOfFirst { it.alive && missile.pos.distanceTo(it.pos) < 0.07f }
            if (cityIdx >= 0) {
                cities[cityIdx] = cities[cityIdx].copy(alive = false)
                return@map missile.copy(alive = false)
            }

            // Damage a silo if close enough.
            val siloIdx = silos.indexOfFirst { it.alive && missile.pos.distanceTo(it.pos) < 0.06f }
            if (siloIdx >= 0) {
                silos[siloIdx] = silos[siloIdx].copy(alive = false, ammo = 0)
                return@map missile.copy(alive = false)
            }

            missile.copy(alive = false) // hits ground without a specific target
        }

        return state.copy(missiles = updatedMissiles, cities = cities, silos = silos)
    }

    private fun waveEndBonus(state: MissileCommandState): Int {
        val ammoBonus = state.silos.filter { it.alive }.sumOf { it.ammo } * 5
        val cityBonus = state.citiesAlive * 100
        val siloBonus = state.silosAlive * 50
        return ammoBonus + cityBonus + siloBonus
    }

    private fun rebuildCities(cities: List<City>, count: Int): List<City> {
        if (count <= 0) return cities
        val mutable = cities.toMutableList()
        var remaining = count
        mutable.indices.forEach { i ->
            if (remaining > 0 && !mutable[i].alive) {
                mutable[i] = mutable[i].copy(alive = true)
                remaining--
            }
        }
        return mutable
    }

    private fun createMirvChildren(
        mirv: EnemyMissile,
        startId: Int,
        waveSpeed: Float,
        cities: List<City>,
        silos: List<Silo>,
    ): List<EnemyMissile> {
        val targets = (cities.filter { it.alive }.map { it.pos }
                + silos.filter { it.alive }.map { it.pos })
            .ifEmpty { listOf(Vec2(0.5f, MissileCommandState.GROUND_Y)) }

        return (0 until 3).map { i ->
            val target = targets[(mirv.id + i) % targets.size]
            EnemyMissile(
                id = startId + i,
                originPos = mirv.pos,
                pos = mirv.pos,
                velocity = (target - mirv.pos).normalized() * waveSpeed,
                target = target,
                alive = true,
                type = MissileType.STANDARD,
            )
        }
    }

    private fun chooseType(idx: Int, wave: Int, config: MissileCommandConfig): MissileType {
        val roll = (idx * 3 + wave * 7) % 20
        return when {
            wave >= config.smartBombFromWave && roll == 0 -> MissileType.SMART
            wave >= config.mirvFromWave && roll in 1..3 -> MissileType.MIRV
            wave >= config.fastMissileFromWave && roll in 4..9 -> MissileType.FAST
            else -> MissileType.STANDARD
        }
    }

    private fun typeSpeed(type: MissileType, baseSpeed: Float): Float = when (type) {
        MissileType.FAST, MissileType.SMART -> baseSpeed * 1.9f
        MissileType.MIRV -> baseSpeed * 0.85f
        MissileType.STANDARD -> baseSpeed
    }
}
