package com.xanticious.androidgames.model.games.idlefarmers

enum class Era(val label: String, val year: String) {
    NEOLITHIC("Neolithic", "10,000 BCE"),
    ANCIENT("Ancient", "3,000 BCE"),
    CLASSICAL("Classical", "500 BCE"),
    MEDIEVAL("Medieval", "1200 CE"),
    INDUSTRIAL("Industrial", "1800 CE"),
    MODERN("Modern", "1950 CE"),
    BIOTECH("Biotech", "2080 CE"),
    FUTURE("Future", "2200 CE")
}

sealed interface UpgradeEffect {
    data class YieldMultiplier(val multiplier: Float) : UpgradeEffect
    data class ExtraFarmerSlot(val count: Int) : UpgradeEffect
    data class ExtraPlot(val count: Int) : UpgradeEffect
    data class DebuffImmunity(val debuff: String) : UpgradeEffect
    data object AutomationSlot : UpgradeEffect
    data class SellMultiplier(val multiplier: Float) : UpgradeEffect
    data object PrestigeUnlock : UpgradeEffect
}

data class FarmUpgrade(
    val id: String,
    val name: String,
    val era: Era,
    val cost: Long,
    val requires: List<String>,
    val effect: UpgradeEffect,
    val purchased: Boolean = false,
    val description: String = ""
)

enum class RandomEventType(val label: String) {
    DROUGHT("Drought"),
    PEST_SWARM("Pest Swarm"),
    BUMPER_HARVEST("Bumper Harvest"),
    FESTIVAL("Festival")
}

data class RandomEvent(
    val type: RandomEventType,
    val durationCycles: Int,
    val remainingCycles: Int
)

data class IdleFarmersState(
    val coins: Long,
    val farmers: Int,
    val plots: Int,
    val yieldMultiplier: Float,
    val sellMultiplier: Float,
    val cycleCount: Long,
    val upgrades: List<FarmUpgrade>,
    val activeEvent: RandomEvent?,
    val lastHarvestAmount: Long,
    val era: Era,
    val debuffImmunities: Set<String>,
    val automationCount: Int,
    val eventLog: List<String>
)
