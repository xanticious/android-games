package com.xanticious.androidgames.controller.games.simcityblocks

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.simcityblocks.ActiveDisaster
import com.xanticious.androidgames.model.games.simcityblocks.CivicBuilding
import com.xanticious.androidgames.model.games.simcityblocks.CivicType
import com.xanticious.androidgames.model.games.simcityblocks.CityBuilding
import com.xanticious.androidgames.model.games.simcityblocks.CityGrid
import com.xanticious.androidgames.model.games.simcityblocks.CityPoint
import com.xanticious.androidgames.model.games.simcityblocks.CityPurchase
import com.xanticious.androidgames.model.games.simcityblocks.CityTerrain
import com.xanticious.androidgames.model.games.simcityblocks.CityTile
import com.xanticious.androidgames.model.games.simcityblocks.DisasterFrequency
import com.xanticious.androidgames.model.games.simcityblocks.DisasterType
import com.xanticious.androidgames.model.games.simcityblocks.PurchaseOutcome
import com.xanticious.androidgames.model.games.simcityblocks.PurchaseValidation
import com.xanticious.androidgames.model.games.simcityblocks.ResourceSnapshot
import com.xanticious.androidgames.model.games.simcityblocks.SimCityBlocksConfig
import com.xanticious.androidgames.model.games.simcityblocks.SimCityBlocksState
import com.xanticious.androidgames.model.games.simcityblocks.TickOutcome
import com.xanticious.androidgames.model.games.simcityblocks.UpgradeLevels
import com.xanticious.androidgames.model.games.simcityblocks.UpgradeType
import com.xanticious.androidgames.model.games.simcityblocks.ZoneBuilding
import com.xanticious.androidgames.model.games.simcityblocks.ZoneType
import kotlin.math.abs
import kotlin.random.Random

class SimCityBlocksController {
    private val gridWidth = 10
    private val gridHeight = 8
    private val centerRailY = 4
    private val noRoom = "No room"
    private val needFunds = "Need funds"
    private val needEnergy = "Need Energy Upgrade"
    private val bankrupt = "Deficit spiral: city collapsed"

    fun configFor(difficulty: GameDifficulty): SimCityBlocksConfig = when (difficulty) {
        GameDifficulty.EASY -> SimCityBlocksConfig(760, 0.03f, 90, 140, 190, 260, 240, 150, 170, 75, 10)
        GameDifficulty.MEDIUM -> SimCityBlocksConfig(620, 0.06f, 100, 160, 220, 300, 280, 170, 200, 65, 10)
        GameDifficulty.HARD -> SimCityBlocksConfig(520, 0.1f, 115, 180, 250, 340, 320, 190, 230, 55, 10)
    }

    fun disasterChanceFor(frequency: DisasterFrequency, config: SimCityBlocksConfig): Float = when (frequency) {
        DisasterFrequency.OFF -> 0f
        DisasterFrequency.RARE -> config.disasterChance
        DisasterFrequency.OCCASIONAL -> config.disasterChance * 1.8f
        DisasterFrequency.FREQUENT -> config.disasterChance * 3f
    }

    fun initialState(config: SimCityBlocksConfig): SimCityBlocksState {
        val tiles = initialTiles()
        val zones = listOf(
            ZoneBuilding(1, CityPoint(3, 3), ZoneType.RESIDENTIAL, 1, true, 10, 80, false),
            ZoneBuilding(2, CityPoint(6, 5), ZoneType.RESIDENTIAL, 1, true, 10, 80, false)
        )
        val grid = CityGrid(gridWidth, gridHeight, tiles, zones, emptyList(), nextBuildingId = 3)
        val resources = ResourceSnapshot(
            population = 20,
            jobs = 0,
            employed = 0,
            income = 0,
            expenses = 0,
            netBalance = 0,
            budget = config.startingBudget,
            energyUsed = energyUsed(grid.buildings),
            energyCapacity = energyCapacity(UpgradeLevels.initial(), config),
            day = 1,
            negativeCycles = 0,
            criticalCycles = 0,
            peakPopulation = 20,
            peakBudget = config.startingBudget,
            disastersSurvived = 0,
            totalZonesBuilt = 2
        )
        return SimCityBlocksState(grid, resources, UpgradeLevels.initial(), emptyList(), "City founded", gameOver = false)
    }

    fun validatePurchase(state: SimCityBlocksState, config: SimCityBlocksConfig, purchase: CityPurchase): PurchaseValidation {
        val cost = costFor(state.upgrades, config, purchase)
        val hasRoom = purchase !is CityPurchase.UpgradeCity || state.grid.buildings.isNotEmpty()
        val placement = when (purchase) {
            is CityPurchase.BuyZone -> choosePlacement(state.grid, purchase.type)
            is CityPurchase.BuildCivic -> choosePlacement(state.grid, purchase.type)
            is CityPurchase.UpgradeCity -> CityPoint(0, 0)
        }
        val prerequisite = purchase !is CityPurchase.UpgradeCity || hasUpgradePrerequisite(state.upgrades, purchase.type)
        val reason = when {
            state.gameOver -> "Game over"
            state.resources.negativeCycles >= 3 && purchase is CityPurchase.BuyZone -> "Budget locked"
            placement == null -> noRoom
            !prerequisite -> needEnergy
            state.resources.budget < cost -> needFunds
            else -> "Ready"
        }
        return PurchaseValidation(reason == "Ready", cost, reason)
    }

    fun purchase(state: SimCityBlocksState, config: SimCityBlocksConfig, purchase: CityPurchase): PurchaseOutcome {
        val validation = validatePurchase(state, config, purchase)
        if (!validation.canPurchase) return PurchaseOutcome(state.copy(message = validation.reason), validation)
        val budget = state.resources.budget - validation.cost
        val next = when (purchase) {
            is CityPurchase.BuyZone -> addZone(state, purchase.type)
            is CityPurchase.BuildCivic -> addCivic(state, purchase.type)
            is CityPurchase.UpgradeCity -> applyUpgrade(state, purchase.type)
        }
        val resources = recalculate(next.grid, next.upgrades, next.activeDisasters, next.resources.copy(budget = budget), config)
        return PurchaseOutcome(next.copy(resources = resources, message = "Built ${labelFor(purchase)}"), validation)
    }

    fun advanceCycle(
        state: SimCityBlocksState,
        config: SimCityBlocksConfig,
        random: Random,
        disasterChance: Float = config.disasterChance
    ): TickOutcome {
        if (state.gameOver) return TickOutcome(state, disasterTriggered = false, zoneAbandoned = false)
        val disasters = ageDisasters(state.activeDisasters)
        val poweredGrid = applyBrownout(state.grid, state.upgrades, disasters, config, random)
        val balanced = balanceZones(poweredGrid, state.upgrades, disasters)
        val abandoned = balanced.zones.size < poweredGrid.zones.size
        val beforeEconomy = state.copy(grid = balanced, activeDisasters = disasters)
        val economy = computeEconomy(beforeEconomy, config)
        var triggeredDisaster = false
        val maybeDisaster = if (random.nextFloat() < disasterChance) {
            triggeredDisaster = true
            applyDisaster(beforeEconomy.copy(grid = balanced, resources = economy), random)
        } else beforeEconomy.copy(grid = balanced, resources = economy, message = "Day ${economy.day}")
        val finalResources = maybeDisaster.resources
        val gameOver = finalResources.criticalCycles >= config.criticalDeficitLimit
        val message = if (gameOver) bankrupt else maybeDisaster.message
        return TickOutcome(maybeDisaster.copy(resources = finalResources, message = message, gameOver = gameOver), triggeredDisaster, abandoned)
    }

    fun applyDisaster(state: SimCityBlocksState, random: Random, type: DisasterType = DisasterType.entries[random.nextInt(DisasterType.entries.size)]): SimCityBlocksState = when (type) {
        DisasterType.FIRE -> applyFire(state, random)
        DisasterType.ECONOMIC_SLUMP -> state.copy(
            activeDisasters = state.activeDisasters + ActiveDisaster(type, 3, emptyList()),
            resources = state.resources.copy(disastersSurvived = state.resources.disastersSurvived + 1),
            message = "Economic slump hit commercial income"
        )
        DisasterType.BLACKOUT -> state.copy(
            activeDisasters = state.activeDisasters + ActiveDisaster(type, 2, state.grid.buildings.map { it.position }),
            resources = state.resources.copy(disastersSurvived = state.resources.disastersSurvived + 1),
            message = "Blackout disabled low-level buildings"
        )
        DisasterType.EXODUS -> applyExodus(state, random)
    }

    private fun initialTiles(): List<CityTile> {
        val roadColumns = setOf(3, 6)
        return (0 until gridHeight).flatMap { y ->
            (0 until gridWidth).map { x ->
                val terrain = when {
                    y == centerRailY -> CityTerrain.SINGLE_RAIL
                    x in roadColumns -> CityTerrain.DIRT_ROAD
                    y == centerRailY - 1 && x in 2..7 -> CityTerrain.DIRT_ROAD
                    y == centerRailY + 1 && x in 2..7 -> CityTerrain.DIRT_ROAD
                    else -> CityTerrain.EMPTY
                }
                CityTile(CityPoint(x, y), terrain)
            }
        }
    }

    private fun addZone(state: SimCityBlocksState, type: ZoneType): SimCityBlocksState {
        val position = choosePlacement(state.grid, type) ?: return state
        val zone = ZoneBuilding(state.grid.nextBuildingId, position, type, levelForZone(state.upgrades, type), true, if (type == ZoneType.RESIDENTIAL) 8 else 0, 75, false)
        val grid = state.grid.copy(zones = state.grid.zones + zone, nextBuildingId = state.grid.nextBuildingId + 1)
        return state.copy(grid = grid, resources = state.resources.copy(totalZonesBuilt = state.resources.totalZonesBuilt + 1))
    }

    private fun addCivic(state: SimCityBlocksState, type: CivicType): SimCityBlocksState {
        val position = choosePlacement(state.grid, type) ?: return state
        val civic = CivicBuilding(state.grid.nextBuildingId, position, type, level = if (type == CivicType.PARK) state.upgrades.parks else 1, powered = true)
        return state.copy(grid = state.grid.copy(civics = state.grid.civics + civic, nextBuildingId = state.grid.nextBuildingId + 1))
    }

    private fun applyUpgrade(state: SimCityBlocksState, type: UpgradeType): SimCityBlocksState {
        val upgrades = when (type) {
            UpgradeType.ROADS -> state.upgrades.copy(roads = state.upgrades.roads + 1)
            UpgradeType.RAILROADS -> state.upgrades.copy(railroads = state.upgrades.railroads + 1)
            UpgradeType.RESIDENTIAL -> state.upgrades.copy(residential = state.upgrades.residential + 1)
            UpgradeType.COMMERCIAL -> state.upgrades.copy(commercial = state.upgrades.commercial + 1)
            UpgradeType.INDUSTRIAL -> state.upgrades.copy(industrial = state.upgrades.industrial + 1)
            UpgradeType.PARKS -> state.upgrades.copy(parks = state.upgrades.parks + 1)
            UpgradeType.ENERGY -> state.upgrades.copy(energy = state.upgrades.energy + 1)
        }
        val tiles = state.grid.tiles.map { tile ->
            when {
                type == UpgradeType.ROADS && tile.terrain == CityTerrain.DIRT_ROAD -> tile.copy(terrain = CityTerrain.PAVED_ROAD)
                type == UpgradeType.RAILROADS && tile.terrain == CityTerrain.SINGLE_RAIL -> tile.copy(terrain = CityTerrain.DOUBLE_RAIL)
                else -> tile
            }
        }
        val zones = state.grid.zones.map { zone -> zone.copy(level = levelForZone(upgrades, zone.type)) }
        val civics = state.grid.civics.map { civic -> if (civic.type == CivicType.PARK) civic.copy(level = upgrades.parks) else civic }
        return state.copy(upgrades = upgrades, grid = state.grid.copy(tiles = tiles, zones = zones, civics = civics))
    }

    private fun choosePlacement(grid: CityGrid, type: ZoneType): CityPoint? = emptyCells(grid)
        .map { it to scoreZone(grid, it, type) }
        .filter { it.second > Int.MIN_VALUE }
        .sortedWith(compareByDescending<Pair<CityPoint, Int>> { it.second }.thenBy { it.first.y }.thenBy { it.first.x })
        .firstOrNull()
        ?.first

    private fun choosePlacement(grid: CityGrid, type: CivicType): CityPoint? = emptyCells(grid)
        .map { it to scoreCivic(grid, it, type) }
        .sortedWith(compareByDescending<Pair<CityPoint, Int>> { it.second }.thenBy { it.first.y }.thenBy { it.first.x })
        .firstOrNull()
        ?.first

    private fun emptyCells(grid: CityGrid): List<CityPoint> {
        val occupied = grid.buildings.map { it.position }.toSet()
        return grid.tiles.asSequence()
            .filter { it.terrain == CityTerrain.EMPTY }
            .map { it.position }
            .filter { it !in occupied }
            .toList()
    }

    private fun scoreZone(grid: CityGrid, point: CityPoint, type: ZoneType): Int {
        val spreadPenalty = grid.zones.any { it.type == type && distance(it.position, point) == 1 }
        val roads = nearbyTerrain(grid, point, setOf(CityTerrain.DIRT_ROAD, CityTerrain.PAVED_ROAD))
        val rail = nearbyTerrain(grid, point, setOf(CityTerrain.SINGLE_RAIL, CityTerrain.DOUBLE_RAIL))
        val residential = nearbyZones(grid, point, ZoneType.RESIDENTIAL)
        val industrial = nearbyZones(grid, point, ZoneType.INDUSTRIAL)
        val parks = nearbyCivics(grid, point, CivicType.PARK)
        val base = when (type) {
            ZoneType.RESIDENTIAL -> roads * 8 + residential * 4 + parks * 9 - industrial * 10
            ZoneType.COMMERCIAL -> roads * 8 + residential * 10 - industrial * 8
            ZoneType.INDUSTRIAL -> rail * 14 + roads * 5 - residential * 9
        }
        return base - if (spreadPenalty) 8 else 0
    }

    private fun scoreCivic(grid: CityGrid, point: CityPoint, type: CivicType): Int = when (type) {
        CivicType.FIRE_STATION, CivicType.POLICE_STATION -> grid.buildings.count { distance(it.position, point) <= 3 } * 8 + nearbyTerrain(grid, point, setOf(CityTerrain.DIRT_ROAD, CityTerrain.PAVED_ROAD))
        CivicType.PARK -> nearbyZones(grid, point, ZoneType.RESIDENTIAL) * 10 + nearbyTerrain(grid, point, setOf(CityTerrain.DIRT_ROAD, CityTerrain.PAVED_ROAD))
    }

    private fun balanceZones(grid: CityGrid, upgrades: UpgradeLevels, disasters: List<ActiveDisaster>): CityGrid {
        val powerBlocked = disasters.any { it.type == DisasterType.BLACKOUT }
        val exodusCells = disasters.filter { it.type == DisasterType.EXODUS }.flatMap { it.affectedCells }.toSet()
        val availableJobs = grid.zones.filter { it.powered && it.type != ZoneType.RESIDENTIAL && !(powerBlocked && it.level == 1) }.sumOf { jobsFor(it) }
        var remainingJobs = availableJobs
        val zones = grid.zones.map { zone ->
            val powered = zone.powered && !(powerBlocked && zone.level == 1)
            when (zone.type) {
                ZoneType.RESIDENTIAL -> {
                    val capacity = residentialCapacity(zone.level)
                    val adjustedPopulation = if (zone.position in exodusCells) (zone.population / 2).coerceAtLeast(1) else zone.population
                    val unemployed = adjustedPopulation > remainingJobs
                    remainingJobs = (remainingJobs - adjustedPopulation).coerceAtLeast(0)
                    val parkBonus = nearbyCivics(grid, zone.position, CivicType.PARK) * upgrades.parks * 3
                    val happinessDelta = when {
                        !powered -> -25
                        availableJobs == 0 -> -35
                        unemployed -> -18
                        else -> 6 + parkBonus
                    }
                    val happiness = (zone.happiness + happinessDelta).coerceIn(0, 100)
                    val population = if (happiness > 55 && powered) (zone.population + 2).coerceAtMost(capacity) else adjustedPopulation
                    zone.copy(powered = powered, population = population, happiness = happiness, idle = false)
                }
                ZoneType.COMMERCIAL, ZoneType.INDUSTRIAL -> zone.copy(powered = powered, idle = !powered || availableJobs <= 0)
            }
        }.filterNot { it.type == ZoneType.RESIDENTIAL && it.happiness == 0 }
        return grid.copy(zones = zones)
    }

    private fun computeEconomy(state: SimCityBlocksState, config: SimCityBlocksConfig): ResourceSnapshot {
        val grid = state.grid
        val residents = grid.zones.filter { it.type == ZoneType.RESIDENTIAL && it.powered }.sumOf { it.population }
        val jobs = grid.zones.filter { it.type != ZoneType.RESIDENTIAL && it.powered }.sumOf { jobsFor(it) }
        val employed = minOf(residents, jobs)
        val commercialJobs = grid.zones.filter { it.type == ZoneType.COMMERCIAL && it.powered }.sumOf { jobsFor(it) }
        val industrialJobs = grid.zones.filter { it.type == ZoneType.INDUSTRIAL && it.powered }.sumOf { jobsFor(it) }
        val commercialWorkers = minOf(employed, commercialJobs)
        val industrialWorkers = (employed - commercialWorkers).coerceAtLeast(0).coerceAtMost(industrialJobs)
        val slump = state.activeDisasters.any { it.type == DisasterType.ECONOMIC_SLUMP }
        val police = grid.civics.any { it.type == CivicType.POLICE_STATION && it.powered }
        val commercialRate = if (state.upgrades.commercial > 1) 14 else 9
        val industrialRate = if (state.upgrades.industrial > 1) 18 else 12
        val crimeFactor = if (police) 1.0f else 0.9f
        val commercialIncome = (commercialWorkers * commercialRate * crimeFactor * if (slump) 0.5f else 1.0f).toInt()
        val industrialCap = if (state.upgrades.railroads > 1) 360 else 180
        val industrialIncome = minOf(industrialWorkers * industrialRate, industrialCap)
        val income = commercialIncome + industrialIncome
        val expenses = grid.buildings.sumOf { maintenanceFor(it) } + energyUsed(grid.buildings)
        val net = income - expenses
        val budget = state.resources.budget + net
        val negative = if (net < 0) state.resources.negativeCycles + 1 else 0
        val critical = if (net <= -40 || budget < 0) state.resources.criticalCycles + 1 else 0
        val day = state.resources.day + 1
        return state.resources.copy(
            population = residents,
            jobs = jobs,
            employed = employed,
            income = income,
            expenses = expenses,
            netBalance = net,
            budget = budget,
            energyUsed = energyUsed(grid.buildings),
            energyCapacity = energyCapacity(state.upgrades, config),
            day = day,
            negativeCycles = negative,
            criticalCycles = critical,
            peakPopulation = maxOf(state.resources.peakPopulation, residents),
            peakBudget = maxOf(state.resources.peakBudget, budget)
        )
    }

    private fun recalculate(grid: CityGrid, upgrades: UpgradeLevels, disasters: List<ActiveDisaster>, resources: ResourceSnapshot, config: SimCityBlocksConfig): ResourceSnapshot =
        computeEconomy(SimCityBlocksState(grid, resources, upgrades, disasters, "", gameOver = false), config).copy(
            budget = resources.budget,
            day = resources.day,
            negativeCycles = resources.negativeCycles,
            criticalCycles = resources.criticalCycles,
            peakBudget = maxOf(resources.peakBudget, resources.budget)
        )

    private fun applyBrownout(grid: CityGrid, upgrades: UpgradeLevels, disasters: List<ActiveDisaster>, config: SimCityBlocksConfig, random: Random): CityGrid {
        val blackout = disasters.any { it.type == DisasterType.BLACKOUT }
        val capacity = energyCapacity(upgrades, config)
        var remainingUse = 0
        val powerById = mutableMapOf<Int, Boolean>()
        grid.buildings.map { it to random.nextInt() }.sortedBy { it.second }.map { it.first }.forEach { building ->
            val demand = energyFor(building)
            val powered = !blackout && remainingUse + demand <= capacity
            if (powered) remainingUse += demand
            powerById[building.id] = powered
        }
        return grid.copy(
            zones = grid.zones.map { it.copy(powered = powerById[it.id] ?: true) },
            civics = grid.civics.map { it.copy(powered = powerById[it.id] ?: true) }
        )
    }

    private fun applyFire(state: SimCityBlocksState, random: Random): SimCityBlocksState {
        val target = state.grid.buildings.firstOrNull { random.nextBoolean() } ?: state.grid.buildings.firstOrNull() ?: return state
        val protected = state.grid.civics.any { it.type == CivicType.FIRE_STATION && distance(it.position, target.position) <= 3 }
        val radius = if (protected) 0 else 1
        val affected = state.grid.buildings.filter { distance(it.position, target.position) <= radius }.take(if (protected) 1 else 3)
        val affectedIds = affected.map { it.id }.toSet()
        val grid = state.grid.copy(
            zones = state.grid.zones.filterNot { it.id in affectedIds },
            civics = state.grid.civics.filterNot { it.id in affectedIds }
        )
        return state.copy(
            grid = grid,
            activeDisasters = state.activeDisasters + ActiveDisaster(DisasterType.FIRE, 1, affected.map { it.position }),
            resources = state.resources.copy(disastersSurvived = state.resources.disastersSurvived + 1),
            message = "Fire damaged ${affected.size} block(s)"
        )
    }

    private fun applyExodus(state: SimCityBlocksState, random: Random): SimCityBlocksState {
        val residential = state.grid.zones.filter { it.type == ZoneType.RESIDENTIAL }
        val target = residential.getOrNull(if (residential.isEmpty()) 0 else random.nextInt(residential.size)) ?: return state
        return state.copy(
            activeDisasters = state.activeDisasters + ActiveDisaster(DisasterType.EXODUS, 2, listOf(target.position)),
            resources = state.resources.copy(disastersSurvived = state.resources.disastersSurvived + 1),
            message = "Residents are leaving one neighborhood"
        )
    }

    private fun ageDisasters(disasters: List<ActiveDisaster>): List<ActiveDisaster> = disasters
        .map { it.copy(remainingCycles = it.remainingCycles - 1) }
        .filter { it.remainingCycles > 0 }

    private fun costFor(upgrades: UpgradeLevels, config: SimCityBlocksConfig, purchase: CityPurchase): Int = when (purchase) {
        is CityPurchase.BuyZone -> when (purchase.type) {
            ZoneType.RESIDENTIAL -> config.residentialCost
            ZoneType.COMMERCIAL -> config.commercialCost
            ZoneType.INDUSTRIAL -> config.industrialCost
        }
        is CityPurchase.BuildCivic -> when (purchase.type) {
            CivicType.FIRE_STATION -> config.fireStationCost
            CivicType.POLICE_STATION -> config.policeStationCost
            CivicType.PARK -> config.parkCost
        }
        is CityPurchase.UpgradeCity -> config.upgradeBaseCost * (upgradeLevel(upgrades, purchase.type) + 1)
    }

    private fun hasUpgradePrerequisite(upgrades: UpgradeLevels, type: UpgradeType): Boolean = when (type) {
        UpgradeType.RESIDENTIAL, UpgradeType.COMMERCIAL, UpgradeType.INDUSTRIAL -> upgrades.energy > upgradeLevel(upgrades, type)
        else -> true
    }

    private fun upgradeLevel(upgrades: UpgradeLevels, type: UpgradeType): Int = when (type) {
        UpgradeType.ROADS -> upgrades.roads
        UpgradeType.RAILROADS -> upgrades.railroads
        UpgradeType.RESIDENTIAL -> upgrades.residential
        UpgradeType.COMMERCIAL -> upgrades.commercial
        UpgradeType.INDUSTRIAL -> upgrades.industrial
        UpgradeType.PARKS -> upgrades.parks
        UpgradeType.ENERGY -> upgrades.energy
    }

    private fun levelForZone(upgrades: UpgradeLevels, type: ZoneType): Int = when (type) {
        ZoneType.RESIDENTIAL -> upgrades.residential
        ZoneType.COMMERCIAL -> upgrades.commercial
        ZoneType.INDUSTRIAL -> upgrades.industrial
    }

    private fun residentialCapacity(level: Int): Int = if (level > 1) 36 else 18
    private fun jobsFor(zone: ZoneBuilding): Int = when (zone.type) {
        ZoneType.RESIDENTIAL -> 0
        ZoneType.COMMERCIAL -> if (zone.level > 1) 28 else 14
        ZoneType.INDUSTRIAL -> if (zone.level > 1) 42 else 24
    }

    private fun energyFor(building: CityBuilding): Int = when (building) {
        is ZoneBuilding -> when (building.type) {
            ZoneType.RESIDENTIAL -> if (building.level > 1) 6 else 3
            ZoneType.COMMERCIAL -> if (building.level > 1) 10 else 6
            ZoneType.INDUSTRIAL -> if (building.level > 1) 16 else 11
        }
        is CivicBuilding -> when (building.type) {
            CivicType.FIRE_STATION -> 8
            CivicType.POLICE_STATION -> 7
            CivicType.PARK -> 1
        }
    }

    private fun maintenanceFor(building: CityBuilding): Int = when (building) {
        is ZoneBuilding -> when (building.type) {
            ZoneType.RESIDENTIAL -> 3
            ZoneType.COMMERCIAL -> 7
            ZoneType.INDUSTRIAL -> 10
        }
        is CivicBuilding -> when (building.type) {
            CivicType.FIRE_STATION -> 14
            CivicType.POLICE_STATION -> 13
            CivicType.PARK -> 5
        }
    }

    private fun energyUsed(buildings: List<CityBuilding>): Int = buildings.sumOf { energyFor(it) }
    private fun energyCapacity(upgrades: UpgradeLevels, config: SimCityBlocksConfig): Int = config.energyCapacityPerLevel * upgrades.energy

    private fun nearbyTerrain(grid: CityGrid, point: CityPoint, terrain: Set<CityTerrain>): Int = grid.tiles.count { it.terrain in terrain && distance(it.position, point) <= 2 }
    private fun nearbyZones(grid: CityGrid, point: CityPoint, type: ZoneType): Int = grid.zones.count { it.type == type && distance(it.position, point) <= 3 }
    private fun nearbyCivics(grid: CityGrid, point: CityPoint, type: CivicType): Int = grid.civics.count { it.type == type && distance(it.position, point) <= 3 }
    private fun distance(a: CityPoint, b: CityPoint): Int = abs(a.x - b.x) + abs(a.y - b.y)

    private fun labelFor(purchase: CityPurchase): String = when (purchase) {
        is CityPurchase.BuyZone -> purchase.type.name.lowercase().replaceFirstChar { it.uppercase() }
        is CityPurchase.BuildCivic -> purchase.type.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
        is CityPurchase.UpgradeCity -> purchase.type.name.lowercase().replaceFirstChar { it.uppercase() }
    }
}
