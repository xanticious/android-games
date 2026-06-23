package com.xanticious.androidgames.model.games.planetexplorer

import com.xanticious.androidgames.model.Vec2

private const val GRASSLAND_ID = "grassland"
private const val REEF_ID = "reef"
private const val SNOWFIELD_ID = "snowfield"
private const val DESERT_ID = "desert"
private const val CAVERN_ID = "cavern"
private const val JUNGLE_ID = "jungle"
private const val HIGHLAND_ID = "highland"

private const val SHOVEL_LABEL = "Shovel"
private const val PICK_AXE_LABEL = "Pick Axe"
private const val SPRING_BOOTS_LABEL = "Spring Boots"
private const val ROCKET_BOOTS_LABEL = "Rocket Boots"
private const val GRAPPLING_HOOK_LABEL = "Grappling Hook"
private const val FLIPPERS_LABEL = "Flippers"
private const val CRAMPONS_LABEL = "Crampons"

enum class Biome(val id: String, val displayName: String, val adventurerName: String) {
    GRASSLAND(GRASSLAND_ID, "Grassland", "Safari Explorer"),
    REEF(REEF_ID, "Reef", "Deep-Sea Diver"),
    SNOWFIELD(SNOWFIELD_ID, "Snowfield", "Polar Researcher"),
    DESERT(DESERT_ID, "Desert", "Archaeologist"),
    CAVERN(CAVERN_ID, "Cavern", "Spelunker"),
    JUNGLE(JUNGLE_ID, "Jungle", "Jungle Scout"),
    HIGHLAND(HIGHLAND_ID, "Highland", "Mountaineer")
}

enum class MovementAbility(val displayName: String, val foundIn: Biome) {
    SHOVEL(SHOVEL_LABEL, Biome.GRASSLAND),
    PICK_AXE(PICK_AXE_LABEL, Biome.CAVERN),
    SPRING_BOOTS(SPRING_BOOTS_LABEL, Biome.DESERT),
    ROCKET_BOOTS(ROCKET_BOOTS_LABEL, Biome.SNOWFIELD),
    GRAPPLING_HOOK(GRAPPLING_HOOK_LABEL, Biome.JUNGLE),
    FLIPPERS(FLIPPERS_LABEL, Biome.REEF),
    CRAMPONS(CRAMPONS_LABEL, Biome.HIGHLAND)
}

enum class DiscoverableKind { ANIMAL, MINERAL }

enum class TerrainDetail { LOW, MEDIUM, HIGH }

enum class ControlLayout { D_PAD, TILT }

data class PlanetExplorerSettings(
    val worldSeed: String,
    val terrainDetail: TerrainDetail,
    val showDiscoveryHints: Boolean,
    val showToolHint: Boolean,
    val controlLayout: ControlLayout
) {
    companion object {
        fun default(seed: String): PlanetExplorerSettings = PlanetExplorerSettings(
            worldSeed = seed,
            terrainDetail = TerrainDetail.MEDIUM,
            showDiscoveryHints = true,
            showToolHint = true,
            controlLayout = ControlLayout.D_PAD
        )
    }
}

data class Discoverable(
    val id: String,
    val biome: Biome,
    val kind: DiscoverableKind,
    val name: String,
    val description: String,
    val position: Vec2
)

data class FieldBookEntry(
    val discoverable: Discoverable,
    val discovered: Boolean
)

data class FieldBook(val entries: List<FieldBookEntry>) {
    val discoveredCount: Int get() = entries.count { it.discovered }
    val totalCount: Int get() = entries.size

    fun isDiscovered(id: String): Boolean = entries.firstOrNull { it.discoverable.id == id }?.discovered ?: false

    fun byBiome(biome: Biome): List<FieldBookEntry> = entries.filter { it.discoverable.biome == biome }
}

data class TerrainColumn(val x: Int, val groundY: Int)

data class BiomeWorld(
    val biome: Biome,
    val width: Int,
    val terrain: List<TerrainColumn>,
    val discoverables: List<Discoverable>,
    val toolPosition: Vec2,
    val ability: MovementAbility
) {
    fun groundYAt(tileX: Float): Float {
        val index = tileX.toInt().coerceIn(0, terrain.lastIndex)
        return terrain.getOrElse(index) { terrain.last() }.groundY.toFloat()
    }
}

data class PlanetWorld(
    val seed: String,
    val biomes: Map<Biome, BiomeWorld>
) {
    fun biomeWorld(biome: Biome): BiomeWorld = requireNotNull(biomes[biome]) { "Missing biome world: $biome" }
}

data class AdventurerState(
    val biome: Biome,
    val position: Vec2,
    val velocity: Vec2,
    val facing: Int,
    val onGround: Boolean
)

data class PlanetExplorerStats(
    val visitedBiomes: Set<Biome>,
    val unlockedAbilities: Set<MovementAbility>,
    val totalDistanceWalked: Float
)

data class PlanetExplorerGameState(
    val world: PlanetWorld,
    val adventurer: AdventurerState,
    val fieldBook: FieldBook,
    val stats: PlanetExplorerStats,
    val selectedAbility: MovementAbility?
)

data class PlanetExplorerConfig(
    val worldWidth: Int,
    val walkSpeed: Float,
    val waterWalkSpeed: Float,
    val jumpVelocity: Float,
    val springJumpVelocity: Float,
    val rocketVelocity: Float,
    val gravity: Float,
    val discoveryRadius: Float,
    val terrainBaseY: Int,
    val terrainAmplitude: Int
)

data class PlanetExplorerInput(
    val directionX: Float,
    val jumpPressed: Boolean,
    val jumpHeld: Boolean,
    val climbDirectionY: Float,
    val useTool: Boolean
)

data class PlanetExplorerDiscovery(
    val discoverable: Discoverable)

data class PlanetExplorerStep(
    val state: PlanetExplorerGameState,
    val discoveries: List<PlanetExplorerDiscovery>,
    val unlockedAbility: MovementAbility?
)
