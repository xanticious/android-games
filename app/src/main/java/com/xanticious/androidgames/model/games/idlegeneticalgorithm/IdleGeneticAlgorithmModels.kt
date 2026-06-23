package com.xanticious.androidgames.model.games.idlegeneticalgorithm

/** Car parameters evolved by the genetic algorithm. All values are normalized floats. */
data class CarGenome(
    val frontWheelRadius: Float,
    val rearWheelRadius: Float,
    val torque: Float,
    val springStiffness: Float,
    val weightDistribution: Float
)

/** A single point on the 2D terrain. x increases to the right, y is the height. */
data class TrackPoint(val x: Float, val y: Float)

/** Live simulation state for one car in the current generation. */
data class CarState(
    val id: Int,
    val genome: CarGenome,
    val positionX: Float,
    val velocityX: Float,
    val alive: Boolean,
    val stuckTimer: Float,
    val colorHue: Float
)

/** Summary of a completed generation, stored in history. */
data class GenerationResult(
    val generation: Int,
    val bestDistance: Float,
    val averageDistance: Float,
    val coinsEarned: Long
)

enum class GaUpgradeId {
    EXTENDED_FUEL_TANK,
    EFFICIENT_ENGINE,
    HIGHER_TOP_SPEED,
    REINFORCED_FRAME,
    SHOCK_ABSORBERS,
    LARGER_GENE_POOL,
    MUTATION_BOOST,
    ELITISM_LOCK
}

data class GaUpgrade(
    val id: GaUpgradeId,
    val name: String,
    val description: String,
    val cost: Long,
    val purchased: Boolean = false
)

data class IdleGeneticAlgorithmState(
    val coins: Long,
    val generation: Int,
    val cars: List<CarState>,
    val genomePool: List<CarGenome>,
    val track: List<TrackPoint>,
    val trackSeed: Long,
    val checkpointInterval: Float,
    val checkpointsReached: Int,
    val generationHistory: List<GenerationResult>,
    val upgrades: List<GaUpgrade>,
    val populationSize: Int,
    val mutationRate: Float,
    val fuelDuration: Float,
    val elitismEnabled: Boolean,
    val mutationBoostRemaining: Int,
    val simulationTime: Float,
    val bestDistanceAllTime: Float,
    val trackAdvanceThreshold: Float
)
