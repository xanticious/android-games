package com.xanticious.androidgames.games.idlegeneticalgorithm

import com.xanticious.androidgames.controller.games.idlegeneticalgorithm.IdleGeneticAlgorithmController
import com.xanticious.androidgames.model.games.idlegeneticalgorithm.CarGenome
import com.xanticious.androidgames.model.games.idlegeneticalgorithm.CarState
import com.xanticious.androidgames.model.games.idlegeneticalgorithm.GaUpgradeId
import com.xanticious.androidgames.model.games.idlegeneticalgorithm.IdleGeneticAlgorithmState
import com.xanticious.androidgames.model.games.idlegeneticalgorithm.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class IdleGeneticAlgorithmControllerTest {
    private val controller = IdleGeneticAlgorithmController()

    @Test
    fun initialState_hasCorrectPopulationSize() {
        assertEquals(10, controller.initialState(seed = 1L).populationSize)
    }

    @Test
    fun initialState_coinsAreZero() {
        assertEquals(0L, controller.initialState(seed = 1L).coins)
    }

    @Test
    fun initialState_generationIsOne() {
        assertEquals(1, controller.initialState(seed = 1L).generation)
    }

    @Test
    fun generateTrack_returnsTwoHundredPoints() {
        assertEquals(200, controller.generateTrack(7L).size)
    }

    @Test
    fun generateTrack_differentSeeds_produceDifferentTracks() {
        assertTrue(controller.generateTrack(1L) != controller.generateTrack(2L))
    }

    @Test
    fun generateTrack_sameSeeds_produceIdenticalTracks() {
        assertEquals(controller.generateTrack(5L), controller.generateTrack(5L))
    }

    @Test
    fun trackHeightAt_atFirstPoint_returnsFirstY() {
        assertEquals(3f, controller.trackHeightAt(listOf(TrackPoint(0f, 3f), TrackPoint(5f, 3f)), 0f))
    }

    @Test
    fun trackSlopeAt_flatSection_isNearZero() {
        assertTrue(abs(controller.trackSlopeAt(listOf(TrackPoint(0f, 2f), TrackPoint(5f, 2f), TrackPoint(10f, 2f)), 5f)) < 0.0001f)
    }

    @Test
    fun stepCars_aliveCarWithPositiveTorque_increasesPosition() {
        val car = simpleCar(positionX = 0f, velocityX = 0f, alive = true)
        val stepped = controller.stepCars(
            cars = listOf(car),
            track = flatTrack(),
            dt = 1f,
            simulationTime = 0f,
            upgrades = controller.upgradesCatalog()
        )
        assertTrue(stepped.first().positionX > car.positionX)
    }

    @Test
    fun stepCars_carExceedingFuelDuration_becomesNotAlive() {
        val stepped = controller.stepCars(
            cars = listOf(simpleCar()),
            track = flatTrack(),
            dt = 0.2f,
            simulationTime = 8f,
            upgrades = controller.upgradesCatalog()
        )
        assertFalse(stepped.first().alive)
    }

    @Test
    fun isGenerationOver_allCarsDead_returnsTrue() {
        assertTrue(controller.isGenerationOver(listOf(simpleCar(alive = false)), simulationTime = 0f, fuelDuration = 8f))
    }

    @Test
    fun isGenerationOver_carAlive_returnsFalse() {
        assertFalse(controller.isGenerationOver(listOf(simpleCar(alive = true)), simulationTime = 0f, fuelDuration = 8f))
    }

    @Test
    fun computeCoinsForGeneration_zeroDistance_returnsOne() {
        assertEquals(1L, controller.computeCoinsForGeneration(0f, 50f))
    }

    @Test
    fun computeCoinsForGeneration_largeDist_coinsScaleWithDistance() {
        assertEquals(140L, controller.computeCoinsForGeneration(120f, 50f))
    }

    @Test
    fun selectParents_returnsBestForty_percent() {
        val cars = (0 until 10).map { index ->
            simpleCar(
                genome = simpleGenome(torque = index.toFloat()),
                positionX = index.toFloat()
            )
        }
        assertEquals(setOf(9f, 8f, 7f, 6f), controller.selectParents(cars).map { it.torque }.toSet())
    }

    @Test
    fun crossover_resultParamsAreFromEitherParent() {
        val parent1 = CarGenome(0.2f, 0.3f, 1f, 1.2f, 0.1f)
        val parent2 = CarGenome(2.0f, 1.9f, 10f, 4.8f, 0.9f)
        val child = controller.crossover(parent1, parent2, Random(0))
        assertTrue(
            child.frontWheelRadius in setOf(parent1.frontWheelRadius, parent2.frontWheelRadius) &&
                child.rearWheelRadius in setOf(parent1.rearWheelRadius, parent2.rearWheelRadius) &&
                child.torque in setOf(parent1.torque, parent2.torque) &&
                child.springStiffness in setOf(parent1.springStiffness, parent2.springStiffness) &&
                child.weightDistribution in setOf(parent1.weightDistribution, parent2.weightDistribution)
        )
    }

    @Test
    fun mutate_producesNearbyGenome() {
        val genome = simpleGenome()
        val mutated = controller.mutate(genome, 0.05f, Random(4))
        val totalDelta = abs(mutated.frontWheelRadius - genome.frontWheelRadius) +
            abs(mutated.rearWheelRadius - genome.rearWheelRadius) +
            abs(mutated.torque - genome.torque) +
            abs(mutated.springStiffness - genome.springStiffness) +
            abs(mutated.weightDistribution - genome.weightDistribution)
        assertTrue(
            mutated.frontWheelRadius in 0.2f..2.0f &&
                mutated.rearWheelRadius in 0.2f..2.0f &&
                mutated.torque in 1.0f..10.0f &&
                mutated.springStiffness in 1.0f..5.0f &&
                mutated.weightDistribution in 0.1f..0.9f &&
                totalDelta > 0f
        )
    }

    @Test
    fun canPurchase_sufficientCoins_returnsTrue() {
        val state = controller.initialState(seed = 2L).copy(coins = 500L)
        assertTrue(controller.canPurchase(state, GaUpgradeId.LARGER_GENE_POOL))
    }

    @Test
    fun canPurchase_alreadyPurchased_returnsFalse() {
        val state = controller.purchaseUpgrade(controller.initialState(seed = 2L).copy(coins = 500L), GaUpgradeId.LARGER_GENE_POOL)
        assertFalse(controller.canPurchase(state, GaUpgradeId.LARGER_GENE_POOL))
    }

    @Test
    fun purchaseUpgrade_largerGenePool_increasesPopulationSize() {
        val upgraded = controller.purchaseUpgrade(controller.initialState(seed = 3L).copy(coins = 500L), GaUpgradeId.LARGER_GENE_POOL)
        assertEquals(15, upgraded.populationSize)
    }

    @Test
    fun finalizeGeneration_recordsResultInHistory() {
        val finalized = controller.finalizeGeneration(
            controller.initialState(seed = 4L).copy(cars = listOf(simpleCar(positionX = 42f), simpleCar(positionX = 21f)))
        )
        assertEquals(1, finalized.generationHistory.size)
    }

    private fun flatTrack(): List<TrackPoint> = listOf(
        TrackPoint(0f, 0f),
        TrackPoint(5f, 0f),
        TrackPoint(10f, 0f),
        TrackPoint(15f, 0f),
        TrackPoint(20f, 0f),
        TrackPoint(25f, 0f)
    )

    private fun simpleGenome(torque: Float = 5f): CarGenome = CarGenome(
        frontWheelRadius = 1f,
        rearWheelRadius = 1f,
        torque = torque,
        springStiffness = 2f,
        weightDistribution = 0.5f
    )

    private fun simpleCar(
        genome: CarGenome = simpleGenome(),
        positionX: Float = 0f,
        velocityX: Float = 0f,
        alive: Boolean = true
    ): CarState = CarState(
        id = 0,
        genome = genome,
        positionX = positionX,
        velocityX = velocityX,
        alive = alive,
        stuckTimer = 0f,
        colorHue = 0f
    )
}
