package com.xanticious.androidgames.controller.games.idlegeneticalgorithm

import com.xanticious.androidgames.model.games.idlegeneticalgorithm.CarGenome
import com.xanticious.androidgames.model.games.idlegeneticalgorithm.CarState
import com.xanticious.androidgames.model.games.idlegeneticalgorithm.GaUpgrade
import com.xanticious.androidgames.model.games.idlegeneticalgorithm.GaUpgradeId
import com.xanticious.androidgames.model.games.idlegeneticalgorithm.GenerationResult
import com.xanticious.androidgames.model.games.idlegeneticalgorithm.IdleGeneticAlgorithmState
import com.xanticious.androidgames.model.games.idlegeneticalgorithm.TrackPoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.random.Random

class IdleGeneticAlgorithmController {
    fun upgradesCatalog(): List<GaUpgrade> = listOf(
        GaUpgrade(
            id = GaUpgradeId.EXTENDED_FUEL_TANK,
            name = "Extended Fuel Tank",
            description = "Run each generation 4 seconds longer.",
            cost = 50L
        ),
        GaUpgrade(
            id = GaUpgradeId.EFFICIENT_ENGINE,
            name = "Efficient Engine",
            description = "Raises the speed cap by 3.",
            cost = 120L
        ),
        GaUpgrade(
            id = GaUpgradeId.HIGHER_TOP_SPEED,
            name = "Higher Top Speed",
            description = "Raises the speed cap by 5.",
            cost = 100L
        ),
        GaUpgrade(
            id = GaUpgradeId.REINFORCED_FRAME,
            name = "Reinforced Frame",
            description = "Cars that get moving no longer die from being stuck.",
            cost = 80L
        ),
        GaUpgrade(
            id = GaUpgradeId.SHOCK_ABSORBERS,
            name = "Shock Absorbers",
            description = "Adds extra suspension stiffness on rough terrain.",
            cost = 60L
        ),
        GaUpgrade(
            id = GaUpgradeId.LARGER_GENE_POOL,
            name = "Larger Gene Pool",
            description = "Adds 5 more cars to each generation.",
            cost = 200L
        ),
        GaUpgrade(
            id = GaUpgradeId.MUTATION_BOOST,
            name = "Mutation Boost",
            description = "Boosts mutation for the next 3 generations.",
            cost = 30L
        ),
        GaUpgrade(
            id = GaUpgradeId.ELITISM_LOCK,
            name = "Elitism Lock",
            description = "Preserves the top 2 genomes unchanged.",
            cost = 150L
        )
    )

    fun initialState(seed: Long = System.currentTimeMillis()): IdleGeneticAlgorithmState {
        val random = Random(seed)
        val populationSize = DEFAULT_POPULATION_SIZE
        val genomes = List(populationSize) { randomGenome(random) }
        return IdleGeneticAlgorithmState(
            coins = 0L,
            generation = 1,
            cars = carsFromGenomes(genomes),
            genomePool = genomes,
            track = generateTrack(seed),
            trackSeed = seed,
            checkpointInterval = DEFAULT_CHECKPOINT_INTERVAL,
            checkpointsReached = 0,
            generationHistory = emptyList(),
            upgrades = upgradesCatalog(),
            populationSize = populationSize,
            mutationRate = DEFAULT_MUTATION_RATE,
            fuelDuration = DEFAULT_FUEL_DURATION,
            elitismEnabled = false,
            mutationBoostRemaining = 0,
            simulationTime = 0f,
            bestDistanceAllTime = 0f,
            trackAdvanceThreshold = INITIAL_TRACK_ADVANCE_THRESHOLD
        )
    }

    fun generateTrack(seed: Long): List<TrackPoint> {
        val random = Random(seed)
        val track = ArrayList<TrackPoint>(TRACK_POINT_COUNT)
        track += TrackPoint(x = 0f, y = 0f)

        var currentY = 0f
        var drift = 0f
        for (index in 1 until TRACK_POINT_COUNT) {
            val targetDelta = nextFloat(random, -10f, 10f)
            drift = drift * 0.65f + targetDelta * 0.35f
            currentY = (currentY + drift).coerceIn(TRACK_MIN_Y, TRACK_MAX_Y)
            track += TrackPoint(x = index * TRACK_POINT_SPACING, y = currentY)
        }
        return track
    }

    fun trackHeightAt(track: List<TrackPoint>, x: Float): Float {
        if (track.isEmpty()) return 0f
        if (x < track.first().x || x > track.last().x) return 0f
        if (x == track.last().x) return track.last().y

        val lowerIndex = (x / TRACK_POINT_SPACING).toInt().coerceIn(0, track.lastIndex - 1)
        val left = track[lowerIndex]
        val right = track[lowerIndex + 1]
        val segmentWidth = (right.x - left.x).takeIf { it != 0f } ?: return left.y
        val fraction = ((x - left.x) / segmentWidth).coerceIn(0f, 1f)
        return left.y + (right.y - left.y) * fraction
    }

    fun trackSlopeAt(track: List<TrackPoint>, x: Float): Float {
        if (track.size < 2) return 0f
        if (x < track.first().x || x > track.last().x) return 0f

        val leftX = (x - TRACK_POINT_SPACING).coerceAtLeast(track.first().x)
        val rightX = (x + TRACK_POINT_SPACING).coerceAtMost(track.last().x)
        if (rightX == leftX) return 0f
        return (trackHeightAt(track, rightX) - trackHeightAt(track, leftX)) / (rightX - leftX)
    }

    fun stepCars(
        cars: List<CarState>,
        track: List<TrackPoint>,
        dt: Float,
        simulationTime: Float,
        upgrades: List<GaUpgrade>
    ): List<CarState> {
        val upgradeIds = purchasedUpgradeIds(upgrades)
        val fuelDuration = DEFAULT_FUEL_DURATION +
            if (GaUpgradeId.EXTENDED_FUEL_TANK in upgradeIds) 4f else 0f
        val maxSpeed = 15f +
            (if (GaUpgradeId.HIGHER_TOP_SPEED in upgradeIds) 5f else 0f) +
            (if (GaUpgradeId.EFFICIENT_ENGINE in upgradeIds) 3f else 0f)
        val reinforcedFrame = GaUpgradeId.REINFORCED_FRAME in upgradeIds
        val shockAbsorbers = GaUpgradeId.SHOCK_ABSORBERS in upgradeIds
        val trackEnd = track.lastOrNull()?.x ?: 0f

        return cars.map { car ->
            if (!car.alive) {
                car
            } else {
                val slope = trackSlopeAt(track, car.positionX)
                val futureSlope = trackSlopeAt(track, car.positionX + 10f)
                val roughness = abs(futureSlope - slope)
                val effectiveSpring = car.genome.springStiffness + if (shockAbsorbers) 1f else 0f
                val effectiveTorque =
                    car.genome.torque * ((car.genome.rearWheelRadius + car.genome.frontWheelRadius) / 2f)
                val slopeResistance = slope * 5f
                val springBonus = 1f + (effectiveSpring - 1f) * 0.1f * roughness
                val acceleration = (effectiveTorque * springBonus - slopeResistance).coerceAtLeast(0f)
                val velocity = (car.velocityX + acceleration * dt).coerceIn(0f, maxSpeed)
                val unclampedPosition = car.positionX + velocity * dt
                val offTrack = unclampedPosition >= trackEnd && trackEnd > 0f
                val position = unclampedPosition.coerceAtMost(trackEnd)
                val stuckTimer = if (velocity < STUCK_SPEED_THRESHOLD) car.stuckTimer + dt else 0f
                val diesFromStuck = stuckTimer > STUCK_TIME_LIMIT &&
                    !(reinforcedFrame && position > 0f)
                val outOfFuel = simulationTime + dt >= fuelDuration
                car.copy(
                    positionX = position,
                    velocityX = velocity,
                    alive = !diesFromStuck && !outOfFuel && !offTrack,
                    stuckTimer = stuckTimer
                )
            }
        }
    }

    fun newCheckpointsReached(
        prevCars: List<CarState>,
        newCars: List<CarState>,
        checkpointsReachedSoFar: Int,
        checkpointInterval: Float
    ): Int {
        if (checkpointInterval <= 0f) return 0
        val prevMax = floor((prevCars.maxOfOrNull { it.positionX } ?: 0f) / checkpointInterval).toInt()
        val newMax = floor((newCars.maxOfOrNull { it.positionX } ?: 0f) / checkpointInterval).toInt()
        return (newMax - max(prevMax, checkpointsReachedSoFar)).coerceAtLeast(0)
    }

    fun isGenerationOver(cars: List<CarState>, simulationTime: Float, fuelDuration: Float): Boolean =
        cars.none { it.alive } || simulationTime >= fuelDuration

    fun computeCoinsForGeneration(bestDistance: Float, checkpointInterval: Float): Long {
        val distanceCoins = bestDistance.roundToLong()
        val checkpointCoins = floor(bestDistance / checkpointInterval).toLong() * 10L
        return max(1L, distanceCoins + checkpointCoins)
    }

    fun selectParents(cars: List<CarState>): List<CarGenome> {
        val parentCount = max(2, ceil(cars.size * PARENT_SELECTION_RATIO).toInt())
        return cars
            .sortedByDescending { it.positionX }
            .take(parentCount)
            .map { it.genome }
    }

    fun crossover(parent1: CarGenome, parent2: CarGenome, random: Random): CarGenome = CarGenome(
        frontWheelRadius = choose(parent1.frontWheelRadius, parent2.frontWheelRadius, random),
        rearWheelRadius = choose(parent1.rearWheelRadius, parent2.rearWheelRadius, random),
        torque = choose(parent1.torque, parent2.torque, random),
        springStiffness = choose(parent1.springStiffness, parent2.springStiffness, random),
        weightDistribution = choose(parent1.weightDistribution, parent2.weightDistribution, random)
    )

    fun mutate(genome: CarGenome, mutationRate: Float, random: Random): CarGenome = CarGenome(
        frontWheelRadius = mutateValue(
            value = genome.frontWheelRadius,
            min = 0.2f,
            max = 2.0f,
            mutationRate = mutationRate,
            random = random
        ),
        rearWheelRadius = mutateValue(
            value = genome.rearWheelRadius,
            min = 0.2f,
            max = 2.0f,
            mutationRate = mutationRate,
            random = random
        ),
        torque = mutateValue(
            value = genome.torque,
            min = 1.0f,
            max = 10.0f,
            mutationRate = mutationRate,
            random = random
        ),
        springStiffness = mutateValue(
            value = genome.springStiffness,
            min = 1.0f,
            max = 5.0f,
            mutationRate = mutationRate,
            random = random
        ),
        weightDistribution = mutateValue(
            value = genome.weightDistribution,
            min = 0.1f,
            max = 0.9f,
            mutationRate = mutationRate,
            random = random
        )
    )

    fun createNextGeneration(
        state: IdleGeneticAlgorithmState,
        random: Random = Random.Default
    ): List<CarState> {
        val parents = selectParents(state.cars)
        val elites = if (state.elitismEnabled) {
            state.cars
                .sortedByDescending { it.positionX }
                .take(2)
                .map { it.genome }
        } else {
            emptyList()
        }
        val effectiveMutationRate = if (state.mutationBoostRemaining > 0) {
            (state.mutationRate + MUTATION_BOOST_BONUS).coerceAtMost(1f)
        } else {
            state.mutationRate
        }

        val genomes = buildList {
            addAll(elites)
            while (size < state.populationSize) {
                val parent1 = parents[random.nextInt(parents.size)]
                val parent2 = parents[random.nextInt(parents.size)]
                val child = crossover(parent1, parent2, random)
                add(mutate(child, effectiveMutationRate, random))
            }
        }.take(state.populationSize)

        return carsFromGenomes(genomes)
    }

    fun step(state: IdleGeneticAlgorithmState, dt: Float): IdleGeneticAlgorithmState {
        val steppedCars = stepCars(
            cars = state.cars,
            track = state.track,
            dt = dt,
            simulationTime = state.simulationTime,
            upgrades = state.upgrades
        )
        val gainedCheckpoints = newCheckpointsReached(
            prevCars = state.cars,
            newCars = steppedCars,
            checkpointsReachedSoFar = state.checkpointsReached,
            checkpointInterval = state.checkpointInterval
        )
        return state.copy(
            coins = state.coins + gainedCheckpoints,
            cars = steppedCars,
            checkpointsReached = state.checkpointsReached + gainedCheckpoints,
            simulationTime = state.simulationTime + dt
        )
    }

    fun finalizeGeneration(state: IdleGeneticAlgorithmState): IdleGeneticAlgorithmState {
        val bestDistance = state.cars.maxOfOrNull { it.positionX } ?: 0f
        val averageDistance = state.cars.map { it.positionX }.average().toFloat()
        val coinsEarned = computeCoinsForGeneration(bestDistance, state.checkpointInterval)
        val result = GenerationResult(
            generation = state.generation,
            bestDistance = bestDistance,
            averageDistance = averageDistance,
            coinsEarned = coinsEarned
        )
        return state.copy(
            coins = state.coins + coinsEarned,
            genomePool = selectParents(state.cars),
            generationHistory = state.generationHistory + result,
            bestDistanceAllTime = max(state.bestDistanceAllTime, bestDistance)
        )
    }

    fun startNextGeneration(
        state: IdleGeneticAlgorithmState,
        random: Random = Random.Default
    ): IdleGeneticAlgorithmState {
        val nextCars = createNextGeneration(state, random)
        return state.copy(
            generation = state.generation + 1,
            cars = nextCars,
            genomePool = nextCars.map { it.genome },
            simulationTime = 0f,
            mutationBoostRemaining = (state.mutationBoostRemaining - 1).coerceAtLeast(0)
        )
    }

    fun shouldAdvanceTrack(state: IdleGeneticAlgorithmState): Boolean =
        state.bestDistanceAllTime >= state.trackAdvanceThreshold

    fun advanceToNewTrack(
        state: IdleGeneticAlgorithmState,
        random: Random = Random.Default
    ): IdleGeneticAlgorithmState {
        val nextTrackSeed = state.trackSeed + 1L
        val nextCars = createNextGeneration(state, random)
        return state.copy(
            generation = state.generation + 1,
            cars = nextCars,
            genomePool = nextCars.map { it.genome },
            track = generateTrack(nextTrackSeed),
            trackSeed = nextTrackSeed,
            checkpointsReached = 0,
            simulationTime = 0f,
            mutationBoostRemaining = (state.mutationBoostRemaining - 1).coerceAtLeast(0),
            trackAdvanceThreshold = state.trackAdvanceThreshold + TRACK_ADVANCE_STEP
        )
    }

    fun purchaseUpgrade(state: IdleGeneticAlgorithmState, id: GaUpgradeId): IdleGeneticAlgorithmState {
        if (!canPurchase(state, id)) return state

        val purchasedUpgrade = state.upgrades.firstOrNull { it.id == id } ?: return state
        val updatedUpgrades = state.upgrades.map { upgrade ->
            if (upgrade.id == id) upgrade.copy(purchased = true) else upgrade
        }

        return when (id) {
            GaUpgradeId.EXTENDED_FUEL_TANK -> state.copy(
                coins = state.coins - purchasedUpgrade.cost,
                upgrades = updatedUpgrades,
                fuelDuration = state.fuelDuration + 4f
            )
            GaUpgradeId.EFFICIENT_ENGINE,
            GaUpgradeId.HIGHER_TOP_SPEED,
            GaUpgradeId.REINFORCED_FRAME,
            GaUpgradeId.SHOCK_ABSORBERS -> state.copy(
                coins = state.coins - purchasedUpgrade.cost,
                upgrades = updatedUpgrades
            )
            GaUpgradeId.LARGER_GENE_POOL -> state.copy(
                coins = state.coins - purchasedUpgrade.cost,
                upgrades = updatedUpgrades,
                populationSize = state.populationSize + 5
            )
            GaUpgradeId.MUTATION_BOOST -> state.copy(
                coins = state.coins - purchasedUpgrade.cost,
                upgrades = updatedUpgrades,
                mutationBoostRemaining = 3
            )
            GaUpgradeId.ELITISM_LOCK -> state.copy(
                coins = state.coins - purchasedUpgrade.cost,
                upgrades = updatedUpgrades,
                elitismEnabled = true
            )
        }
    }

    fun canPurchase(state: IdleGeneticAlgorithmState, id: GaUpgradeId): Boolean {
        val upgrade = state.upgrades.firstOrNull { it.id == id } ?: return false
        return !upgrade.purchased && state.coins >= upgrade.cost
    }

    private fun randomGenome(random: Random): CarGenome = CarGenome(
        frontWheelRadius = nextFloat(random, 0.2f, 2.0f),
        rearWheelRadius = nextFloat(random, 0.2f, 2.0f),
        torque = nextFloat(random, 1.0f, 10.0f),
        springStiffness = nextFloat(random, 1.0f, 5.0f),
        weightDistribution = nextFloat(random, 0.1f, 0.9f)
    )

    private fun carsFromGenomes(genomes: List<CarGenome>): List<CarState> =
        genomes.mapIndexed { index, genome ->
            CarState(
                id = index,
                genome = genome,
                positionX = 0f,
                velocityX = 0f,
                alive = true,
                stuckTimer = 0f,
                colorHue = colorHueFor(genome)
            )
        }

    private fun colorHueFor(genome: CarGenome): Float =
        ((genome.torque * 37f) + (genome.frontWheelRadius * 73f)) % 360f

    private fun purchasedUpgradeIds(upgrades: List<GaUpgrade>): Set<GaUpgradeId> =
        upgrades.asSequence().filter { it.purchased }.map { it.id }.toSet()

    private fun choose(first: Float, second: Float, random: Random): Float =
        if (random.nextBoolean()) first else second

    private fun mutateValue(
        value: Float,
        min: Float,
        max: Float,
        mutationRate: Float,
        random: Random
    ): Float {
        if (mutationRate <= 0f) return value
        val delta = gaussian(random) * mutationRate * (max - min)
        return (value + delta).coerceIn(min, max)
    }

    private fun gaussian(random: Random): Float {
        val u1 = random.nextDouble().coerceAtLeast(1e-12)
        val u2 = random.nextDouble()
        val magnitude = sqrt(-2.0 * ln(u1))
        return (magnitude * cos(2.0 * PI * u2)).toFloat()
    }

    private fun nextFloat(random: Random, min: Float, max: Float): Float =
        min + random.nextFloat() * (max - min)

    private companion object {
        const val DEFAULT_POPULATION_SIZE = 10
        const val DEFAULT_CHECKPOINT_INTERVAL = 50f
        const val DEFAULT_FUEL_DURATION = 8f
        const val DEFAULT_MUTATION_RATE = 0.12f
        const val INITIAL_TRACK_ADVANCE_THRESHOLD = 250f
        const val TRACK_ADVANCE_STEP = 250f
        const val TRACK_POINT_COUNT = 200
        const val TRACK_POINT_SPACING = 5f
        const val TRACK_MIN_Y = -50f
        const val TRACK_MAX_Y = 50f
        const val STUCK_SPEED_THRESHOLD = 0.5f
        const val STUCK_TIME_LIMIT = 2f
        const val PARENT_SELECTION_RATIO = 0.4
        const val MUTATION_BOOST_BONUS = 0.15f
    }
}
