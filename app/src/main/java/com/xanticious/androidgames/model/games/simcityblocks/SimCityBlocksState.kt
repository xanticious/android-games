package com.xanticious.androidgames.model.games.simcityblocks

enum class CityTerrain { EMPTY, DIRT_ROAD, PAVED_ROAD, SINGLE_RAIL, DOUBLE_RAIL }

enum class ZoneType { RESIDENTIAL, COMMERCIAL, INDUSTRIAL }

enum class CivicType { FIRE_STATION, POLICE_STATION, PARK }

enum class UpgradeType { ROADS, RAILROADS, RESIDENTIAL, COMMERCIAL, INDUSTRIAL, PARKS, ENERGY }

enum class DisasterType { FIRE, ECONOMIC_SLUMP, BLACKOUT, EXODUS }

enum class DisasterFrequency(val label: String) { OFF("Off"), RARE("Rare"), OCCASIONAL("Occasional"), FREQUENT("Frequent") }

enum class CycleSpeed(val label: String, val secondsPerDay: Float) {
    SLOW("Slow", 60f), NORMAL("Normal", 30f), FAST("Fast", 15f)
}

data class CityPoint(val x: Int, val y: Int)

data class CityTile(val position: CityPoint, val terrain: CityTerrain)

sealed interface CityBuilding {
    val id: Int
    val position: CityPoint
    val level: Int
    val powered: Boolean
}

data class ZoneBuilding(
    override val id: Int,
    override val position: CityPoint,
    val type: ZoneType,
    override val level: Int,
    override val powered: Boolean,
    val population: Int,
    val happiness: Int,
    val idle: Boolean
) : CityBuilding

data class CivicBuilding(
    override val id: Int,
    override val position: CityPoint,
    val type: CivicType,
    override val level: Int,
    override val powered: Boolean
) : CityBuilding

data class CityGrid(
    val width: Int,
    val height: Int,
    val tiles: List<CityTile>,
    val zones: List<ZoneBuilding>,
    val civics: List<CivicBuilding>,
    val nextBuildingId: Int
) {
    val buildings: List<CityBuilding>
        get() = zones + civics
}

data class UpgradeLevels(
    val roads: Int,
    val railroads: Int,
    val residential: Int,
    val commercial: Int,
    val industrial: Int,
    val parks: Int,
    val energy: Int
) {
    companion object {
        fun initial(): UpgradeLevels = UpgradeLevels(roads = 1, railroads = 1, residential = 1, commercial = 1, industrial = 1, parks = 1, energy = 1)
    }
}

data class ResourceSnapshot(
    val population: Int,
    val jobs: Int,
    val employed: Int,
    val income: Int,
    val expenses: Int,
    val netBalance: Int,
    val budget: Int,
    val energyUsed: Int,
    val energyCapacity: Int,
    val day: Int,
    val negativeCycles: Int,
    val criticalCycles: Int,
    val peakPopulation: Int,
    val peakBudget: Int,
    val disastersSurvived: Int,
    val totalZonesBuilt: Int
)

data class ActiveDisaster(
    val type: DisasterType,
    val remainingCycles: Int,
    val affectedCells: List<CityPoint>
)

data class SimCityBlocksConfig(
    val startingBudget: Int,
    val disasterChance: Float,
    val residentialCost: Int,
    val commercialCost: Int,
    val industrialCost: Int,
    val fireStationCost: Int,
    val policeStationCost: Int,
    val parkCost: Int,
    val upgradeBaseCost: Int,
    val energyCapacityPerLevel: Int,
    val criticalDeficitLimit: Int
)

data class SimCityBlocksState(
    val grid: CityGrid,
    val resources: ResourceSnapshot,
    val upgrades: UpgradeLevels,
    val activeDisasters: List<ActiveDisaster>,
    val message: String,
    val gameOver: Boolean
)

sealed interface CityPurchase {
    data class BuyZone(val type: ZoneType) : CityPurchase
    data class BuildCivic(val type: CivicType) : CityPurchase
    data class UpgradeCity(val type: UpgradeType) : CityPurchase
}

data class PurchaseValidation(
    val canPurchase: Boolean,
    val cost: Int,
    val reason: String
)

data class PurchaseOutcome(
    val state: SimCityBlocksState,
    val validation: PurchaseValidation
)

data class TickOutcome(
    val state: SimCityBlocksState,
    val disasterTriggered: Boolean,
    val zoneAbandoned: Boolean
)
