package com.xanticious.androidgames.model.games.missilecommand

import com.xanticious.androidgames.model.Vec2

enum class MissileType { STANDARD, FAST, MIRV, SMART }

enum class InterceptorPhase { FLYING, EXPLODING, DONE }

data class Silo(
    val pos: Vec2,
    val ammo: Int,
    val alive: Boolean,
) {
    companion object {
        const val FULL_AMMO = 10
    }
}

data class City(val pos: Vec2, val alive: Boolean)

data class EnemyMissile(
    val id: Int,
    val originPos: Vec2,
    val pos: Vec2,
    val velocity: Vec2,
    val target: Vec2,
    val alive: Boolean,
    val type: MissileType,
    val hasSplit: Boolean = false,
)

data class Interceptor(
    val id: Int,
    val launchPos: Vec2,
    val pos: Vec2,
    val target: Vec2,
    val velocity: Vec2,
    val phase: InterceptorPhase,
    val blastRadius: Float,
    val maxBlastRadius: Float,
    val blastAge: Float,
)

data class MissileCommandState(
    val silos: List<Silo>,
    val cities: List<City>,
    val missiles: List<EnemyMissile>,
    val interceptors: List<Interceptor>,
    val score: Int,
    val bestScore: Int,
    val wave: Int,
    val nextId: Int,
    val waveCleared: Boolean,
    val gameOver: Boolean,
    val playerWon: Boolean,
) {
    val citiesAlive: Int get() = cities.count { it.alive }
    val silosAlive: Int get() = silos.count { it.alive }

    companion object {
        val SILO_POSITIONS = listOf(
            Vec2(0.05f, 0.87f),
            Vec2(0.50f, 0.87f),
            Vec2(0.95f, 0.87f),
        )
        val CITY_POSITIONS = listOf(
            Vec2(0.14f, 0.92f),
            Vec2(0.27f, 0.92f),
            Vec2(0.38f, 0.92f),
            Vec2(0.62f, 0.92f),
            Vec2(0.73f, 0.92f),
            Vec2(0.86f, 0.92f),
        )
        const val GROUND_Y = 0.94f
        const val MAX_BLAST_RADIUS = 0.08f
        const val BLAST_EXPAND_SPEED = 0.22f
        const val BLAST_LINGER_SECONDS = 0.50f
        const val ARRIVAL_THRESHOLD = 0.04f
        const val MAX_WAVE = 12

        fun initial(bestScore: Int = 0) = MissileCommandState(
            silos = SILO_POSITIONS.map { Silo(it, Silo.FULL_AMMO, alive = true) },
            cities = CITY_POSITIONS.map { City(it, alive = true) },
            missiles = emptyList(),
            interceptors = emptyList(),
            score = 0,
            bestScore = bestScore,
            wave = 1,
            nextId = 0,
            waveCleared = false,
            gameOver = false,
            playerWon = false,
        )
    }
}

data class MissileCommandConfig(
    val missileBaseSpeed: Float,
    val interceptorSpeed: Float,
    val missilesWave1: Int,
    val missilesPerWaveGain: Int,
    val missileSpeedGainPerWave: Float,
    val fastMissileFromWave: Int,
    val mirvFromWave: Int,
    val smartBombFromWave: Int,
)

data class MissileCommandInput(val tapTarget: Vec2?)

data class MissileCommandStep(
    val state: MissileCommandState,
    val scoreGained: Int,
)
