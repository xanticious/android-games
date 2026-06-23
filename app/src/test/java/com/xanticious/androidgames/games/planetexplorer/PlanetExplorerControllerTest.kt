package com.xanticious.androidgames.games.planetexplorer

import com.xanticious.androidgames.controller.games.planetexplorer.PlanetExplorerController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.planetexplorer.Biome
import com.xanticious.androidgames.model.games.planetexplorer.MovementAbility
import com.xanticious.androidgames.model.games.planetexplorer.PlanetExplorerInput
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanetExplorerControllerTest {
    private val controller = PlanetExplorerController()
    private val config = controller.configFor(GameDifficulty.MEDIUM)
    private val idleInput = PlanetExplorerInput(0f, jumpPressed = false, jumpHeld = false, climbDirectionY = 0f, useTool = false)

    @Test
    fun generateBiome_sameSeed_reusesTerrain() {
        val first = controller.generateBiome(Biome.GRASSLAND, config, Random(7)).terrain
        val second = controller.generateBiome(Biome.GRASSLAND, config, Random(7)).terrain
        assertEquals(first, second)
    }

    @Test
    fun moveAdventurer_rightInput_movesRight() {
        val state = controller.initialState(config, "movement")
        val moved = controller.moveAdventurer(state, config, 0.25f, idleInput.copy(directionX = 1f))
        assertTrue(moved.adventurer.position.x > state.adventurer.position.x)
    }

    @Test
    fun discoverAt_touchingAnimal_recordsEntry() {
        val state = controller.initialState(config, "discovery")
        val animal = state.world.biomeWorld(Biome.GRASSLAND).discoverables.first()
        val placed = state.copy(adventurer = state.adventurer.copy(position = animal.position))
        val result = controller.discoverAt(placed, config, idleInput)
        assertTrue(result.state.fieldBook.isDiscovered(animal.id))
    }

    @Test
    fun discoverAt_touchingTool_unlocksAbility() {
        val state = controller.initialState(config, "tool")
        val world = state.world.biomeWorld(Biome.GRASSLAND)
        val placed = state.copy(adventurer = state.adventurer.copy(position = world.toolPosition))
        val result = controller.discoverAt(placed, config, idleInput)
        assertEquals(MovementAbility.SHOVEL, result.unlockedAbility)
    }

    @Test
    fun recordDiscovery_newEntry_incrementsCounter() {
        val state = controller.initialState(config, "book")
        val id = state.world.biomeWorld(Biome.GRASSLAND).discoverables.first().id
        val book = controller.recordDiscovery(state.fieldBook, id)
        assertEquals(1, book.discoveredCount)
    }

    @Test
    fun switchBiome_selectingReef_placesReefAdventurer() {
        val state = controller.initialState(config, "biome")
        val switched = controller.switchBiome(state, Biome.REEF)
        assertEquals(Biome.REEF, switched.adventurer.biome)
    }

    @Test
    fun moveAdventurer_withSpringBoots_jumpsHigher() {
        val state = controller.initialState(config, "spring").copy(
            stats = controller.initialState(config, "spring").stats.copy(unlockedAbilities = setOf(MovementAbility.SPRING_BOOTS)),
            adventurer = controller.initialState(config, "spring").adventurer.copy(velocity = Vec2.ZERO, onGround = true)
        )
        val moved = controller.moveAdventurer(state, config, 0.1f, idleInput.copy(jumpPressed = true))
        assertTrue(moved.adventurer.velocity.y < -config.jumpVelocity)
    }
}
