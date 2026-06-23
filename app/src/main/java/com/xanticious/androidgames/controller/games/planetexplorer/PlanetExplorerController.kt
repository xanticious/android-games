package com.xanticious.androidgames.controller.games.planetexplorer

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.planetexplorer.AdventurerState
import com.xanticious.androidgames.model.games.planetexplorer.Biome
import com.xanticious.androidgames.model.games.planetexplorer.BiomeWorld
import com.xanticious.androidgames.model.games.planetexplorer.Discoverable
import com.xanticious.androidgames.model.games.planetexplorer.DiscoverableKind
import com.xanticious.androidgames.model.games.planetexplorer.FieldBook
import com.xanticious.androidgames.model.games.planetexplorer.FieldBookEntry
import com.xanticious.androidgames.model.games.planetexplorer.MovementAbility
import com.xanticious.androidgames.model.games.planetexplorer.PlanetExplorerConfig
import com.xanticious.androidgames.model.games.planetexplorer.PlanetExplorerDiscovery
import com.xanticious.androidgames.model.games.planetexplorer.PlanetExplorerGameState
import com.xanticious.androidgames.model.games.planetexplorer.PlanetExplorerInput
import com.xanticious.androidgames.model.games.planetexplorer.PlanetExplorerStats
import com.xanticious.androidgames.model.games.planetexplorer.PlanetExplorerStep
import com.xanticious.androidgames.model.games.planetexplorer.PlanetWorld
import com.xanticious.androidgames.model.games.planetexplorer.TerrainColumn
import kotlin.math.abs
import kotlin.random.Random

class PlanetExplorerController {
    private val playerHalfHeight = 1.2f
    private val startX = 3f
    private val defaultSeed = "planet-001"

    fun configFor(difficulty: GameDifficulty): PlanetExplorerConfig = when (difficulty) {
        GameDifficulty.EASY -> PlanetExplorerConfig(
            worldWidth = 72, walkSpeed = 8.2f, waterWalkSpeed = 5.2f, jumpVelocity = 11f,
            springJumpVelocity = 25f, rocketVelocity = 12f, gravity = 24f,
            discoveryRadius = 1.4f, terrainBaseY = 14, terrainAmplitude = 3
        )
        GameDifficulty.MEDIUM -> PlanetExplorerConfig(
            worldWidth = 84, walkSpeed = 7.2f, waterWalkSpeed = 4.4f, jumpVelocity = 10f,
            springJumpVelocity = 23f, rocketVelocity = 11f, gravity = 26f,
            discoveryRadius = 1.2f, terrainBaseY = 14, terrainAmplitude = 4
        )
        GameDifficulty.HARD -> PlanetExplorerConfig(
            worldWidth = 96, walkSpeed = 6.4f, waterWalkSpeed = 3.6f, jumpVelocity = 9.5f,
            springJumpVelocity = 21f, rocketVelocity = 10f, gravity = 28f,
            discoveryRadius = 1.0f, terrainBaseY = 15, terrainAmplitude = 5
        )
    }

    fun initialState(config: PlanetExplorerConfig, seed: String = defaultSeed): PlanetExplorerGameState {
        val world = generateWorld(seed, config)
        val fieldBook = createFieldBook(world)
        val adventurer = placeAdventurer(world.biomeWorld(Biome.GRASSLAND), Biome.GRASSLAND)
        return PlanetExplorerGameState(
            world = world,
            adventurer = adventurer,
            fieldBook = fieldBook,
            stats = PlanetExplorerStats(
                visitedBiomes = setOf(Biome.GRASSLAND),
                unlockedAbilities = emptySet(),
                totalDistanceWalked = 0f
            ),
            selectedAbility = null
        )
    }

    fun generateWorld(seed: String, config: PlanetExplorerConfig): PlanetWorld {
        val biomes = Biome.entries.associateWith { biome ->
            generateBiome(biome, config, Random(seedFor(seed, biome)))
        }
        return PlanetWorld(seed = seed, biomes = biomes)
    }

    fun generateBiome(biome: Biome, config: PlanetExplorerConfig, random: Random): BiomeWorld {
        val width = biomeWidth(config, biome)
        val terrain = generateTerrain(width, config, random)
        val discoverables = createDiscoverables(biome, terrain, random)
        val toolX = (width * toolFraction(biome)).toInt().coerceIn(4, width - 4)
        val toolPosition = Vec2(toolX.toFloat(), terrain[toolX].groundY - 1.3f)
        return BiomeWorld(
            biome = biome,
            width = width,
            terrain = terrain,
            discoverables = discoverables,
            toolPosition = toolPosition,
            ability = abilityFor(biome)
        )
    }

    fun step(state: PlanetExplorerGameState, config: PlanetExplorerConfig, dt: Float, input: PlanetExplorerInput): PlanetExplorerStep {
        val moved = moveAdventurer(state, config, dt, input)
        val discovered = discoverAt(moved, config, input)
        return discovered
    }

    fun moveAdventurer(
        state: PlanetExplorerGameState,
        config: PlanetExplorerConfig,
        dt: Float,
        input: PlanetExplorerInput
    ): PlanetExplorerGameState {
        val biomeWorld = state.world.biomeWorld(state.adventurer.biome)
        val abilities = state.stats.unlockedAbilities
        val speed = if (state.adventurer.biome == Biome.REEF && MovementAbility.FLIPPERS !in abilities) {
            config.waterWalkSpeed
        } else {
            config.walkSpeed
        }
        val direction = input.directionX.coerceIn(-1f, 1f)
        val facing = when {
            direction > 0.05f -> 1
            direction < -0.05f -> -1
            else -> state.adventurer.facing
        }
        val jumpVelocity = if (MovementAbility.SPRING_BOOTS in abilities) config.springJumpVelocity else config.jumpVelocity
        val jumpVy = if (input.jumpPressed && state.adventurer.onGround) -jumpVelocity else state.adventurer.velocity.y
        val rocketVy = if (input.jumpHeld && MovementAbility.ROCKET_BOOTS in abilities) {
            minOf(jumpVy, -config.rocketVelocity)
        } else {
            jumpVy + config.gravity * dt
        }
        val climbVy = if (MovementAbility.CRAMPONS in abilities && abs(input.climbDirectionY) > 0.05f) {
            input.climbDirectionY.coerceIn(-1f, 1f) * speed
        } else {
            rocketVy
        }
        val velocity = Vec2(direction * speed, climbVy)
        val rawPosition = state.adventurer.position + velocity * dt
        val clampedX = rawPosition.x.coerceIn(0f, biomeWorld.width - 1f)
        val groundY = biomeWorld.groundYAt(clampedX) - playerHalfHeight
        val landed = rawPosition.y >= groundY
        val nextPosition = Vec2(clampedX, if (landed) groundY else rawPosition.y.coerceAtLeast(1f))
        val nextVelocity = if (landed) Vec2(velocity.x, 0f) else velocity
        val distance = abs(nextPosition.x - state.adventurer.position.x)
        return state.copy(
            adventurer = state.adventurer.copy(
                position = nextPosition,
                velocity = nextVelocity,
                facing = facing,
                onGround = landed
            ),
            stats = state.stats.copy(totalDistanceWalked = state.stats.totalDistanceWalked + distance)
        )
    }

    fun discoverAt(state: PlanetExplorerGameState, config: PlanetExplorerConfig, input: PlanetExplorerInput): PlanetExplorerStep {
        val biomeWorld = state.world.biomeWorld(state.adventurer.biome)
        val position = state.adventurer.position
        val entries = biomeWorld.discoverables.asSequence()
            .filter { !state.fieldBook.isDiscovered(it.id) }
            .filter { it.position.distanceTo(position) <= config.discoveryRadius || canCollectMineral(it, position, input, state) }
            .toList()
        val nextBook = entries.fold(state.fieldBook) { book, discoverable -> recordDiscovery(book, discoverable.id) }
        val ability = if (biomeWorld.ability !in state.stats.unlockedAbilities && position.distanceTo(biomeWorld.toolPosition) <= config.discoveryRadius) {
            biomeWorld.ability
        } else {
            null
        }
        val nextAbilities = ability?.let { state.stats.unlockedAbilities + it } ?: state.stats.unlockedAbilities
        val selected = state.selectedAbility ?: ability
        val nextState = state.copy(
            fieldBook = nextBook,
            stats = state.stats.copy(unlockedAbilities = nextAbilities),
            selectedAbility = selected
        )
        return PlanetExplorerStep(
            state = nextState,
            discoveries = entries.map { PlanetExplorerDiscovery(it) },
            unlockedAbility = ability
        )
    }

    fun switchBiome(state: PlanetExplorerGameState, biome: Biome): PlanetExplorerGameState {
        val biomeWorld = state.world.biomeWorld(biome)
        return state.copy(
            adventurer = placeAdventurer(biomeWorld, biome),
            stats = state.stats.copy(visitedBiomes = state.stats.visitedBiomes + biome)
        )
    }

    fun selectAbility(state: PlanetExplorerGameState, ability: MovementAbility): PlanetExplorerGameState =
        if (ability in state.stats.unlockedAbilities) state.copy(selectedAbility = ability) else state

    fun recordDiscovery(fieldBook: FieldBook, id: String): FieldBook = fieldBook.copy(
        entries = fieldBook.entries.map { entry ->
            if (entry.discoverable.id == id) entry.copy(discovered = true) else entry
        }
    )

    fun abilityFor(biome: Biome): MovementAbility = MovementAbility.entries.first { it.foundIn == biome }

    private fun createFieldBook(world: PlanetWorld): FieldBook = FieldBook(
        entries = Biome.entries.flatMap { biome ->
            world.biomeWorld(biome).discoverables.map { FieldBookEntry(it, discovered = false) }
        }
    )

    private fun placeAdventurer(biomeWorld: BiomeWorld, biome: Biome): AdventurerState {
        val groundY = biomeWorld.groundYAt(startX) - playerHalfHeight
        return AdventurerState(
            biome = biome,
            position = Vec2(startX, groundY),
            velocity = Vec2.ZERO,
            facing = 1,
            onGround = true
        )
    }

    private fun generateTerrain(width: Int, config: PlanetExplorerConfig, random: Random): List<TerrainColumn> {
        var y = config.terrainBaseY + random.nextInt(-1, 2)
        return (0 until width).map { x ->
            y = (y + random.nextInt(-1, 2)).coerceIn(
                config.terrainBaseY - config.terrainAmplitude,
                config.terrainBaseY + config.terrainAmplitude
            )
            TerrainColumn(x, y)
        }
    }

    private fun createDiscoverables(biome: Biome, terrain: List<TerrainColumn>, random: Random): List<Discoverable> {
        val names = namesFor(biome)
        return names.mapIndexed { index, item ->
            val x = (((index + 1) * terrain.size) / (names.size + 1) + random.nextInt(-2, 3)).coerceIn(1, terrain.lastIndex)
            val kind = if (index < 2) DiscoverableKind.ANIMAL else DiscoverableKind.MINERAL
            val y = if (kind == DiscoverableKind.ANIMAL) terrain[x].groundY - 1.1f else terrain[x].groundY + 0.4f
            Discoverable(
                id = "${biome.id}-${item.lowercase().replace(" ", "-")}",
                biome = biome,
                kind = kind,
                name = item,
                description = descriptionFor(biome, kind, item),
                position = Vec2(x.toFloat(), y)
            )
        }
    }

    private fun canCollectMineral(
        discoverable: Discoverable,
        position: Vec2,
        input: PlanetExplorerInput,
        state: PlanetExplorerGameState
    ): Boolean {
        if (discoverable.kind != DiscoverableKind.MINERAL || !input.useTool) return false
        val selected = state.selectedAbility ?: return false
        val reachable = discoverable.position.distanceTo(position) <= 2f
        return reachable && selected in state.stats.unlockedAbilities
    }

    private fun biomeWidth(config: PlanetExplorerConfig, biome: Biome): Int = when (biome) {
        Biome.GRASSLAND -> config.worldWidth
        Biome.CAVERN -> (config.worldWidth * 0.75f).toInt()
        Biome.REEF, Biome.DESERT -> (config.worldWidth * 0.9f).toInt()
        Biome.SNOWFIELD, Biome.JUNGLE, Biome.HIGHLAND -> (config.worldWidth * 0.82f).toInt()
    }.coerceAtLeast(36)

    private fun toolFraction(biome: Biome): Float = when (biome) {
        Biome.GRASSLAND -> 0.45f
        Biome.REEF -> 0.55f
        Biome.SNOWFIELD -> 0.62f
        Biome.DESERT -> 0.5f
        Biome.CAVERN -> 0.58f
        Biome.JUNGLE -> 0.66f
        Biome.HIGHLAND -> 0.7f
    }

    private fun seedFor(seed: String, biome: Biome): Int = 31 * seed.hashCode() + biome.id.hashCode()

    private fun namesFor(biome: Biome): List<String> = when (biome) {
        Biome.GRASSLAND -> listOf("Gazelle", "Elephant", "Quartz", "Amber")
        Biome.REEF -> listOf("Clownfish", "Crab", "Coral Stone", "Pearl")
        Biome.SNOWFIELD -> listOf("Arctic Fox", "Penguin", "Blue Ice", "Frost Crystal")
        Biome.DESERT -> listOf("Fennec", "Scarab", "Fossil", "Sunstone")
        Biome.CAVERN -> listOf("Bat", "Cave Newt", "Amethyst", "Gold Vein")
        Biome.JUNGLE -> listOf("Toucan", "Tree Frog", "Jade", "Obsidian")
        Biome.HIGHLAND -> listOf("Ibex", "Eagle", "Granite", "Silver")
    }

    private fun descriptionFor(biome: Biome, kind: DiscoverableKind, name: String): String = when (kind) {
        DiscoverableKind.ANIMAL -> "$name thrives in the ${biome.displayName.lowercase()}."
        DiscoverableKind.MINERAL -> "$name marks the geology of the ${biome.displayName.lowercase()}."
    }
}
