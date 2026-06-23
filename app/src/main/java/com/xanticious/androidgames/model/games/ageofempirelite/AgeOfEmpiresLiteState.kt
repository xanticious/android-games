package com.xanticious.androidgames.model.games.ageofempirelite

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2

enum class Resource { FOOD, STUDY }

enum class UnitType { INFANTRY, ARCHER, CAVALRY, CANNON }

enum class UpgradeId {
    ATTACK_I, ATTACK_II, ATTACK_III,
    DEFENSE_I, DEFENSE_II, DEFENSE_III,
    SPEED_I, SPEED_II, SPEED_III,
    CITY_WALL_I, CITY_WALL_II, CITY_WALL_III,
    AGRICULTURE_I, AGRICULTURE_II, AGRICULTURE_III,
    LEARNING_I, LEARNING_II, LEARNING_III,
    ENLIGHTENMENT
}

/** Economy split between food and study workers; foodPct + studyPct must equal 100. */
data class EconomyBalance(val foodPct: Int, val studyPct: Int)

/** Target army composition ratios. CANNON is excluded (it is queued separately). */
data class ArmyComposition(val ratios: Map<UnitType, Int>)

/** Ordered list of upgrades to research. Earlier entries are prioritised. */
data class UpgradePriority(val order: List<UpgradeId>)

data class MatchSettings(
    val difficulty: GameDifficulty,
    val economy: EconomyBalance,
    val army: ArmyComposition,
    val upgrades: UpgradePriority
)

data class ResourcePool(val food: Int, val study: Int)

data class Worker(val id: Int, val assignedTo: Resource)

data class MilitaryUnit(
    val id: Int,
    val type: UnitType,
    val side: Boolean,          // true = player, false = bot
    val hp: Int,
    val maxHp: Int,
    val pos: Vec2,
    val secondsSinceLastCombat: Float = 0f
)

data class King(val side: Boolean, val hp: Int, val maxHp: Int = 100, val pos: Vec2)

/** Maps each upgrade to its direct prerequisite (null = no prerequisite). */
val UPGRADE_PREREQUISITES: Map<UpgradeId, UpgradeId?> = mapOf(
    UpgradeId.ATTACK_I to null,
    UpgradeId.ATTACK_II to UpgradeId.ATTACK_I,
    UpgradeId.ATTACK_III to UpgradeId.ATTACK_II,
    UpgradeId.DEFENSE_I to null,
    UpgradeId.DEFENSE_II to UpgradeId.DEFENSE_I,
    UpgradeId.DEFENSE_III to UpgradeId.DEFENSE_II,
    UpgradeId.SPEED_I to null,
    UpgradeId.SPEED_II to UpgradeId.SPEED_I,
    UpgradeId.SPEED_III to UpgradeId.SPEED_II,
    UpgradeId.CITY_WALL_I to null,
    UpgradeId.CITY_WALL_II to UpgradeId.CITY_WALL_I,
    UpgradeId.CITY_WALL_III to UpgradeId.CITY_WALL_II,
    UpgradeId.AGRICULTURE_I to null,
    UpgradeId.AGRICULTURE_II to UpgradeId.AGRICULTURE_I,
    UpgradeId.AGRICULTURE_III to UpgradeId.AGRICULTURE_II,
    UpgradeId.LEARNING_I to null,
    UpgradeId.LEARNING_II to UpgradeId.LEARNING_I,
    UpgradeId.LEARNING_III to UpgradeId.LEARNING_II,
    // ENLIGHTENMENT also requires ALL non-ENLIGHTENMENT upgrades (checked in UpgradeScheduler)
    UpgradeId.ENLIGHTENMENT to UpgradeId.LEARNING_III
)

/** All non-ENLIGHTENMENT upgrades that must be researched before ENLIGHTENMENT unlocks. */
val ALL_NON_ENLIGHTENMENT_UPGRADES: Set<UpgradeId> =
    UpgradeId.entries.filter { it != UpgradeId.ENLIGHTENMENT }.toSet()

data class AgeOfEmpiresLiteState(
    val settings: MatchSettings,
    val playerResources: ResourcePool,
    val botResources: ResourcePool,
    val playerWorkers: List<Worker>,
    val botWorkers: List<Worker>,
    val playerArmy: List<MilitaryUnit>,
    val botArmy: List<MilitaryUnit>,
    val playerKing: King,
    val botKing: King,
    val playerResearched: Set<UpgradeId>,
    val botResearched: Set<UpgradeId>,
    val playerCannonQueue: Int,     // remaining cannon builds queued
    val botCannonQueue: Int,
    val elapsedSeconds: Float,
    val enlightenmentCountdown: Float?,  // seconds remaining; null if not started
    val enlightenmentSide: Boolean?,     // true = player researching, false = bot
    val ploughsharesThreshold: Int = 5000,
    val nextUnitId: Int = 0
)
