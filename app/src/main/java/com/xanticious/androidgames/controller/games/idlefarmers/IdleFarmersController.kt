package com.xanticious.androidgames.controller.games.idlefarmers

import com.xanticious.androidgames.model.games.idlefarmers.Era
import com.xanticious.androidgames.model.games.idlefarmers.FarmUpgrade
import com.xanticious.androidgames.model.games.idlefarmers.IdleFarmersState
import com.xanticious.androidgames.model.games.idlefarmers.RandomEvent
import com.xanticious.androidgames.model.games.idlefarmers.RandomEventType
import com.xanticious.androidgames.model.games.idlefarmers.UpgradeEffect
import kotlin.math.ceil
import kotlin.random.Random

class IdleFarmersController {
    fun upgradeTree(): List<FarmUpgrade> = listOf(
        FarmUpgrade(
            id = "subsistence-farming",
            name = "Subsistence Farming",
            era = Era.NEOLITHIC,
            cost = 0,
            requires = emptyList(),
            effect = UpgradeEffect.ExtraPlot(0),
            purchased = true,
            description = "Starter farm"
        ),
        FarmUpgrade(
            id = "cleared-land",
            name = "Cleared Land",
            era = Era.NEOLITHIC,
            cost = 10,
            requires = listOf("subsistence-farming"),
            effect = UpgradeEffect.ExtraPlot(1),
            description = "+1 plot, +10% yield"
        ),
        FarmUpgrade(
            id = "simple-tools",
            name = "Simple Tools",
            era = Era.NEOLITHIC,
            cost = 25,
            requires = listOf("cleared-land"),
            effect = UpgradeEffect.YieldMultiplier(1.2f),
            description = "+20% yield per farmer"
        ),
        FarmUpgrade(
            id = "seed-selection",
            name = "Seed Selection",
            era = Era.NEOLITHIC,
            cost = 40,
            requires = listOf("simple-tools"),
            effect = UpgradeEffect.YieldMultiplier(1.15f),
            description = "+15% yield"
        ),
        FarmUpgrade(
            id = "discover-barley",
            name = "Discover Barley",
            era = Era.ANCIENT,
            cost = 100,
            requires = listOf("seed-selection"),
            effect = UpgradeEffect.YieldMultiplier(1.1f),
            description = "Second crop type, +10% yield"
        ),
        FarmUpgrade(
            id = "irrigation-ditch",
            name = "Irrigation Ditch",
            era = Era.ANCIENT,
            cost = 80,
            requires = listOf("cleared-land"),
            effect = UpgradeEffect.DebuffImmunity("drought"),
            description = "Drought immunity"
        ),
        FarmUpgrade(
            id = "domesticated-oxen",
            name = "Domesticated Oxen",
            era = Era.ANCIENT,
            cost = 150,
            requires = listOf("simple-tools"),
            effect = UpgradeEffect.ExtraPlot(2),
            description = "Doubles field size (+2 plots)"
        ),
        FarmUpgrade(
            id = "train-farmer",
            name = "Train Another Farmer",
            era = Era.ANCIENT,
            cost = 200,
            requires = listOf("cleared-land"),
            effect = UpgradeEffect.ExtraFarmerSlot(1),
            description = "+1 farmer"
        ),
        FarmUpgrade(
            id = "crop-rotation",
            name = "Crop Rotation",
            era = Era.CLASSICAL,
            cost = 500,
            requires = listOf("discover-barley", "irrigation-ditch"),
            effect = UpgradeEffect.YieldMultiplier(1.25f),
            description = "Eliminates soil exhaustion debuff"
        ),
        FarmUpgrade(
            id = "bronze-plowshare",
            name = "Bronze Plowshare",
            era = Era.CLASSICAL,
            cost = 400,
            requires = listOf("domesticated-oxen"),
            effect = UpgradeEffect.YieldMultiplier(1.4f),
            description = "+40% tilling speed"
        ),
        FarmUpgrade(
            id = "grain-storage",
            name = "Grain Storage",
            era = Era.CLASSICAL,
            cost = 600,
            requires = listOf("crop-rotation"),
            effect = UpgradeEffect.YieldMultiplier(1.1f),
            description = "Buffer against bad seasons"
        ),
        FarmUpgrade(
            id = "three-field-system",
            name = "Three-Field System",
            era = Era.MEDIEVAL,
            cost = 1500,
            requires = listOf("crop-rotation"),
            effect = UpgradeEffect.YieldMultiplier(1.33f),
            description = "33% more active land"
        ),
        FarmUpgrade(
            id = "windmill",
            name = "Windmill",
            era = Era.MEDIEVAL,
            cost = 1200,
            requires = listOf("grain-storage"),
            effect = UpgradeEffect.YieldMultiplier(1.15f),
            description = "Passive processing bonus"
        ),
        FarmUpgrade(
            id = "selective-breeding",
            name = "Selective Breeding",
            era = Era.MEDIEVAL,
            cost = 2000,
            requires = listOf("seed-selection", "three-field-system"),
            effect = UpgradeEffect.YieldMultiplier(1.5f),
            description = "Compounding yield increase"
        ),
        FarmUpgrade(
            id = "steam-tractor",
            name = "Steam Tractor",
            era = Era.INDUSTRIAL,
            cost = 8000,
            requires = listOf("bronze-plowshare", "windmill"),
            effect = UpgradeEffect.AutomationSlot,
            description = "Replaces one farmer with automation"
        ),
        FarmUpgrade(
            id = "chemical-fertiliser",
            name = "Chemical Fertiliser",
            era = Era.INDUSTRIAL,
            cost = 6000,
            requires = listOf("selective-breeding"),
            effect = UpgradeEffect.YieldMultiplier(1.8f),
            description = "+80% yield"
        ),
        FarmUpgrade(
            id = "rail-distribution",
            name = "Rail Distribution",
            era = Era.INDUSTRIAL,
            cost = 10000,
            requires = listOf("grain-storage", "steam-tractor"),
            effect = UpgradeEffect.SellMultiplier(2.0f),
            description = "Sell surplus at 2× price"
        ),
        FarmUpgrade(
            id = "pesticides",
            name = "Pesticides",
            era = Era.MODERN,
            cost = 15000,
            requires = listOf("chemical-fertiliser"),
            effect = UpgradeEffect.DebuffImmunity("pest_swarm"),
            description = "Eliminates pest swarm debuff"
        ),
        FarmUpgrade(
            id = "combine-harvester",
            name = "Combine Harvester",
            era = Era.MODERN,
            cost = 20000,
            requires = listOf("steam-tractor"),
            effect = UpgradeEffect.YieldMultiplier(10f),
            description = "Harvests 10× faster"
        ),
        FarmUpgrade(
            id = "refrigeration",
            name = "Refrigeration",
            era = Era.MODERN,
            cost = 30000,
            requires = listOf("rail-distribution"),
            effect = UpgradeEffect.YieldMultiplier(1.2f),
            description = "Allows perishable crops"
        ),
        FarmUpgrade(
            id = "genetic-engineering",
            name = "Genetic Engineering",
            era = Era.BIOTECH,
            cost = 100000,
            requires = listOf("selective-breeding", "pesticides"),
            effect = UpgradeEffect.YieldMultiplier(5f),
            description = "Custom high-yield crops"
        ),
        FarmUpgrade(
            id = "drone-fleet",
            name = "Drone Fleet",
            era = Era.BIOTECH,
            cost = 80000,
            requires = listOf("combine-harvester"),
            effect = UpgradeEffect.AutomationSlot,
            description = "Fully automated planting (+automation)"
        ),
        FarmUpgrade(
            id = "vertical-farm",
            name = "Vertical Farm",
            era = Era.BIOTECH,
            cost = 150000,
            requires = listOf("refrigeration", "genetic-engineering"),
            effect = UpgradeEffect.ExtraPlot(10),
            description = "Stacked plots, no land limit"
        ),
        FarmUpgrade(
            id = "robotic-farmers",
            name = "Robotic Farmers",
            era = Era.FUTURE,
            cost = 500000,
            requires = listOf("drone-fleet", "vertical-farm"),
            effect = UpgradeEffect.AutomationSlot,
            description = "Replace all human farmer slots"
        ),
        FarmUpgrade(
            id = "synthetic-soil",
            name = "Synthetic Soil",
            era = Era.FUTURE,
            cost = 400000,
            requires = listOf("genetic-engineering", "chemical-fertiliser"),
            effect = UpgradeEffect.YieldMultiplier(20f),
            description = "Produce anywhere"
        ),
        FarmUpgrade(
            id = "orbital-greenhouse",
            name = "Orbital Greenhouse",
            era = Era.FUTURE,
            cost = 2_000_000,
            requires = listOf("robotic-farmers", "synthetic-soil"),
            effect = UpgradeEffect.PrestigeUnlock,
            description = "Endgame prestige unlock"
        )
    )

    fun initialState(): IdleFarmersState {
        val upgrades = upgradeTree()
        return IdleFarmersState(
            coins = 0,
            farmers = 1,
            plots = 1,
            yieldMultiplier = 1f,
            sellMultiplier = 1f,
            cycleCount = 0,
            upgrades = upgrades,
            activeEvent = null,
            lastHarvestAmount = 0,
            era = computeEra(upgrades),
            debuffImmunities = emptySet(),
            automationCount = 0,
            eventLog = listOf("A new farm begins in the ${Era.NEOLITHIC.label} era.")
        )
    }

    fun computeHarvestCrops(state: IdleFarmersState): Float =
        (state.farmers + state.automationCount) * state.plots * state.yieldMultiplier

    fun computeCoins(crops: Float, state: IdleFarmersState): Long {
        val eventCropMultiplier = when (state.activeEvent?.type) {
            RandomEventType.DROUGHT ->
                if ("drought" in state.debuffImmunities) 1f else 0.5f

            RandomEventType.PEST_SWARM ->
                if ("pest_swarm" in state.debuffImmunities) 1f else 0.7f

            RandomEventType.BUMPER_HARVEST -> 2f
            RandomEventType.FESTIVAL, null -> 1f
        }
        val eventSellMultiplier = when (state.activeEvent?.type) {
            RandomEventType.FESTIVAL -> 1.5f
            else -> 1f
        }
        val rawCoins = ceil(crops * eventCropMultiplier * state.sellMultiplier * eventSellMultiplier).toLong()
        return if (crops > 0f) rawCoins.coerceAtLeast(1L) else 0L
    }

    fun harvest(state: IdleFarmersState): IdleFarmersState {
        val baseCrops = computeHarvestCrops(state)
        val earnedCoins = computeCoins(baseCrops, state)
        val nextEvent = state.activeEvent?.let(::tickEvent)
        val endedEvent = if (state.activeEvent != null && nextEvent == null) {
            "${state.activeEvent.type.label} ended."
        } else {
            null
        }
        val harvestSummary = buildString {
            append("Cycle ${state.cycleCount + 1}: +$earnedCoins coins")
            state.activeEvent?.let { append(" during ${it.type.label.lowercase()}") }
        }

        return state.copy(
            coins = state.coins + earnedCoins,
            cycleCount = state.cycleCount + 1,
            activeEvent = nextEvent,
            lastHarvestAmount = earnedCoins,
            eventLog = appendLog(state.eventLog, listOfNotNull(harvestSummary, endedEvent))
        )
    }

    fun canPurchase(state: IdleFarmersState, upgradeId: String): Boolean {
        val upgrade = state.upgrades.firstOrNull { it.id == upgradeId } ?: return false
        if (upgrade.purchased || state.coins < upgrade.cost) return false
        val purchasedIds = state.upgrades.asSequence()
            .filter { it.purchased }
            .map { it.id }
            .toSet()
        return upgrade.requires.all { it in purchasedIds }
    }

    fun purchaseUpgrade(state: IdleFarmersState, upgradeId: String): IdleFarmersState {
        if (!canPurchase(state, upgradeId)) {
            throw IllegalArgumentException("Cannot purchase upgrade: $upgradeId")
        }

        val target = state.upgrades.firstOrNull { it.id == upgradeId }
            ?: throw IllegalArgumentException("Unknown upgrade: $upgradeId")
        val updatedUpgrades = state.upgrades.map { upgrade ->
            if (upgrade.id == upgradeId) upgrade.copy(purchased = true) else upgrade
        }

        var nextState = state.copy(
            coins = state.coins - target.cost,
            upgrades = updatedUpgrades
        )

        nextState = when (val effect = target.effect) {
            is UpgradeEffect.YieldMultiplier ->
                nextState.copy(yieldMultiplier = nextState.yieldMultiplier * effect.multiplier)

            is UpgradeEffect.ExtraFarmerSlot ->
                nextState.copy(farmers = nextState.farmers + effect.count)

            is UpgradeEffect.ExtraPlot ->
                nextState.copy(plots = nextState.plots + effect.count)

            is UpgradeEffect.DebuffImmunity ->
                nextState.copy(debuffImmunities = nextState.debuffImmunities + effect.debuff)

            UpgradeEffect.AutomationSlot ->
                nextState.copy(automationCount = nextState.automationCount + 1)

            is UpgradeEffect.SellMultiplier ->
                nextState.copy(sellMultiplier = nextState.sellMultiplier * effect.multiplier)

            UpgradeEffect.PrestigeUnlock -> nextState
        }

        val purchaseLog = if (target.effect == UpgradeEffect.PrestigeUnlock) {
            "Purchased ${target.name}. Prestige is now unlocked."
        } else {
            "Purchased ${target.name}."
        }

        return nextState.copy(
            era = computeEra(updatedUpgrades),
            eventLog = appendLog(nextState.eventLog, listOf(purchaseLog))
        )
    }

    fun maybeTriggeredEvent(
        state: IdleFarmersState,
        random: Random = Random.Default
    ): RandomEvent? {
        if (state.activeEvent != null || random.nextFloat() >= 0.1f) return null

        val type = when (random.nextInt(RandomEventType.entries.size)) {
            0 -> RandomEventType.DROUGHT
            1 -> RandomEventType.PEST_SWARM
            2 -> RandomEventType.BUMPER_HARVEST
            else -> RandomEventType.FESTIVAL
        }
        val duration = when (type) {
            RandomEventType.DROUGHT ->
                if ("drought" in state.debuffImmunities) 1 else 3

            RandomEventType.PEST_SWARM ->
                if ("pest_swarm" in state.debuffImmunities) 1 else 2

            RandomEventType.BUMPER_HARVEST -> 2
            RandomEventType.FESTIVAL -> 1
        }

        return RandomEvent(type = type, durationCycles = duration, remainingCycles = duration)
    }

    fun tickEvent(event: RandomEvent): RandomEvent? =
        if (event.remainingCycles <= 1) {
            null
        } else {
            event.copy(remainingCycles = event.remainingCycles - 1)
        }

    fun computeEra(upgrades: List<FarmUpgrade>): Era =
        upgrades.asSequence()
            .filter { it.purchased }
            .map { it.era }
            .maxByOrNull { it.ordinal }
            ?: Era.NEOLITHIC

    fun availableUpgrades(state: IdleFarmersState): List<FarmUpgrade> {
        val purchasedIds = state.upgrades.asSequence()
            .filter { it.purchased }
            .map { it.id }
            .toSet()
        return state.upgrades.filter { upgrade ->
            !upgrade.purchased && upgrade.requires.all { it in purchasedIds }
        }
    }

    private fun appendLog(existing: List<String>, additions: List<String>): List<String> =
        (existing + additions).takeLast(10)
}
