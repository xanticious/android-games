package com.xanticious.androidgames.controller.games.ageofempirelite

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.ageofempirelite.AgeOfEmpiresLiteState
import com.xanticious.androidgames.model.games.ageofempirelite.ALL_NON_ENLIGHTENMENT_UPGRADES
import com.xanticious.androidgames.model.games.ageofempirelite.ArmyComposition
import com.xanticious.androidgames.model.games.ageofempirelite.EconomyBalance
import com.xanticious.androidgames.model.games.ageofempirelite.King
import com.xanticious.androidgames.model.games.ageofempirelite.MatchSettings
import com.xanticious.androidgames.model.games.ageofempirelite.MilitaryUnit
import com.xanticious.androidgames.model.games.ageofempirelite.Resource
import com.xanticious.androidgames.model.games.ageofempirelite.ResourcePool
import com.xanticious.androidgames.model.games.ageofempirelite.UPGRADE_PREREQUISITES
import com.xanticious.androidgames.model.games.ageofempirelite.UpgradeId
import com.xanticious.androidgames.model.games.ageofempirelite.UpgradePriority
import com.xanticious.androidgames.model.games.ageofempirelite.UnitType
import com.xanticious.androidgames.model.games.ageofempirelite.Worker
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

val UNIT_COSTS: Map<UnitType, Int> = mapOf(
    UnitType.INFANTRY to 50,
    UnitType.ARCHER to 75,
    UnitType.CAVALRY to 100,
    UnitType.CANNON to 300
)

val UNIT_MAX_HP: Map<UnitType, Int> = mapOf(
    UnitType.INFANTRY to 60,
    UnitType.ARCHER to 40,
    UnitType.CAVALRY to 80,
    UnitType.CANNON to 100
)

val UPGRADE_COSTS: Map<UpgradeId, Int> = buildMap {
    UpgradeId.entries.forEach { id ->
        put(id, when {
            id == UpgradeId.ENLIGHTENMENT -> 800
            id.name.endsWith("_III") -> 400
            id.name.endsWith("_II") -> 200
            else -> 100
        })
    }
}

private const val FOOD_RATE_PER_SEC = 1.0f
private const val STUDY_RATE_PER_SEC = 0.5f
private const val WORKER_SPAWN_INTERVAL = 10f
private const val MAX_WORKERS = 10
private const val BATTLEFIELD_WIDTH = 2000f
private const val BATTLEFIELD_HEIGHT = 600f
private const val PLAYER_SPAWN_X = 300f
private const val BOT_SPAWN_X = 1700f
private const val PLAYER_KING_X = 50f
private const val BOT_KING_X = 1950f
private const val KING_Y = 300f
private const val UNIT_ADVANCE_SPEED = 60f   // px/sec
private const val MELEE_RANGE = 3f           // used in logic; scaled to battlefield units (60px)
private const val RANGED_RANGE = 6f
private const val COMBAT_RANGE_PX = 120f     // px equivalent for melee
private const val RANGED_RANGE_PX = 240f
private const val CANNON_RANGE_PX = 360f
private const val ENLIGHTENMENT_DURATION = 60f

// ---------------------------------------------------------------------------
// WorkerAssigner
// ---------------------------------------------------------------------------

data class WorkerAssignment(val foodWorkers: Int, val studyWorkers: Int)

object WorkerAssigner {
    /**
     * Splits [workerCount] workers according to [economy].
     * Uses proportional rounding; guarantees foodWorkers + studyWorkers == workerCount.
     */
    fun assignWorkers(workerCount: Int, economy: EconomyBalance): WorkerAssignment {
        if (workerCount == 0) return WorkerAssignment(0, 0)
        val foodWorkers = (workerCount * economy.foodPct / 100.0).roundToInt()
            .coerceIn(0, workerCount)
        val studyWorkers = workerCount - foodWorkers
        return WorkerAssignment(foodWorkers, studyWorkers)
    }
}

// ---------------------------------------------------------------------------
// ArmyTrainer
// ---------------------------------------------------------------------------

object ArmyTrainer {
    /**
     * Returns the [UnitType] to train next, or null if training is not possible.
     *
     * Priority order:
     * 1. Cannon queue (if food allows)
     * 2. Unit with the largest ratio deficit vs [target]
     * 3. Cheapest affordable unit when exact ratio is met
     */
    fun nextUnitToTrain(
        army: List<MilitaryUnit>,
        target: ArmyComposition,
        cannonQueue: Int,
        food: Int,
        armyCap: Int,
        playerSide: Boolean
    ): UnitType? {
        val sideArmy = army.filter { it.side == playerSide }
        if (sideArmy.size >= armyCap) return null

        if (cannonQueue > 0) {
            val cost = UNIT_COSTS[UnitType.CANNON] ?: return null
            return if (food >= cost) UnitType.CANNON else null
        }

        val ratioUnits = UnitType.entries.filter { it != UnitType.CANNON }
        val totalRatio = target.ratios.values.sum().takeIf { it > 0 } ?: return null

        // Count current composition (excluding cannon)
        val counts = ratioUnits.associateWith { type ->
            sideArmy.count { it.type == type }
        }
        val currentTotal = counts.values.sum().takeIf { it > 0 } ?: 1

        // Find unit with largest deficit relative to target ratio
        val deficits = ratioUnits.mapNotNull { type ->
            val targetRatio = (target.ratios[type] ?: 0).toFloat() / totalRatio
            val currentRatio = counts.getValue(type).toFloat() / currentTotal
            val deficit = targetRatio - currentRatio
            if (deficit > 0f) type to deficit else null
        }

        val candidate = if (deficits.isNotEmpty()) {
            deficits.maxByOrNull { it.second }?.first
        } else {
            // Exact ratio — pick cheapest affordable
            ratioUnits.filter { (UNIT_COSTS[it] ?: Int.MAX_VALUE) <= food }
                .minByOrNull { UNIT_COSTS[it] ?: Int.MAX_VALUE }
        }

        return candidate?.takeIf { (UNIT_COSTS[it] ?: Int.MAX_VALUE) <= food }
    }
}

// ---------------------------------------------------------------------------
// UnitRegenerator
// ---------------------------------------------------------------------------

object UnitRegenerator {
    /**
     * Regenerates hp for out-of-combat units and advances the combat timer for all units.
     *
     * @param cooldown seconds after last combat before regen kicks in
     * @param ratePerSec hp restored per second when regenerating
     */
    fun regenerate(
        units: List<MilitaryUnit>,
        dt: Float,
        cooldown: Float = 5f,
        ratePerSec: Float = 2f
    ): List<MilitaryUnit> = units.map { unit ->
        val newTimer = unit.secondsSinceLastCombat + dt
        val heal = if (newTimer >= cooldown) (ratePerSec * dt).toInt() else 0
        unit.copy(
            hp = (unit.hp + heal).coerceAtMost(unit.maxHp),
            secondsSinceLastCombat = newTimer
        )
    }
}

// ---------------------------------------------------------------------------
// UpgradeScheduler
// ---------------------------------------------------------------------------

object UpgradeScheduler {
    /**
     * Returns the next affordable upgrade from [priority] that has all prerequisites
     * met and has not already been researched. Returns null if nothing is purchasable.
     */
    fun nextUpgrade(
        priority: UpgradePriority,
        researched: Set<UpgradeId>,
        study: Int
    ): UpgradeId? = priority.order.firstOrNull { id ->
        when {
            id in researched -> false
            !prerequisitesMet(id, researched) -> false
            id == UpgradeId.ENLIGHTENMENT && !ALL_NON_ENLIGHTENMENT_UPGRADES.all { it in researched } -> false
            (UPGRADE_COSTS[id] ?: Int.MAX_VALUE) > study -> false
            else -> true
        }
    }

    private fun prerequisitesMet(id: UpgradeId, researched: Set<UpgradeId>): Boolean {
        val prereq = UPGRADE_PREREQUISITES[id] ?: return true
        return prereq in researched
    }
}

// ---------------------------------------------------------------------------
// CombatResolver
// ---------------------------------------------------------------------------

data class CombatTickResult(
    val updatedPlayer: List<MilitaryUnit>,
    val updatedBot: List<MilitaryUnit>
)

object CombatResolver {
    /** Returns the base damage [attacker] deals to [target] per hit. */
    private fun baseDamage(attackerType: UnitType, targetType: UnitType): Float {
        val base = when (attackerType) {
            UnitType.INFANTRY -> 20f
            UnitType.ARCHER -> 15f
            UnitType.CAVALRY -> 25f
            UnitType.CANNON -> 30f   // vs units; 50 vs walls (not modelled here)
        }
        // Rock-paper-scissors bonus
        val bonus = when {
            attackerType == UnitType.INFANTRY && targetType == UnitType.CAVALRY -> 1.5f
            attackerType == UnitType.ARCHER && targetType == UnitType.INFANTRY -> 1.5f
            attackerType == UnitType.CAVALRY && targetType == UnitType.ARCHER -> 1.5f
            else -> 1.0f
        }
        return base * bonus
    }

    private fun rangeOf(type: UnitType): Float = when (type) {
        UnitType.INFANTRY, UnitType.CAVALRY -> COMBAT_RANGE_PX
        UnitType.ARCHER -> RANGED_RANGE_PX
        UnitType.CANNON -> CANNON_RANGE_PX
    }

    /**
     * Resolves one combat frame of duration [dt] seconds.
     *
     * Each unit attacks the nearest enemy within range. Units at 0 hp are removed.
     * Surviving attackers have their [MilitaryUnit.secondsSinceLastCombat] reset.
     */
    fun resolveCombatTick(
        playerArmy: List<MilitaryUnit>,
        botArmy: List<MilitaryUnit>,
        dt: Float
    ): CombatTickResult {
        // Mutable hp maps so multiple attackers can hit the same target
        val playerHp = playerArmy.associate { it.id to it.hp }.toMutableMap()
        val botHp = botArmy.associate { it.id to it.hp }.toMutableMap()
        val playerAttacked = mutableSetOf<Int>()
        val botAttacked = mutableSetOf<Int>()

        // Player units attack nearest bot unit in range
        for (attacker in playerArmy) {
            val target = botArmy
                .filter { (botHp[it.id] ?: 0) > 0 }
                .minByOrNull { attacker.pos.distanceTo(it.pos) }
                ?: continue
            if (attacker.pos.distanceTo(target.pos) <= rangeOf(attacker.type)) {
                val dmg = baseDamage(attacker.type, target.type) * dt
                botHp[target.id] = ((botHp[target.id] ?: 0) - dmg.toInt()).coerceAtLeast(0)
                playerAttacked += attacker.id
            }
        }

        // Bot units attack nearest player unit in range
        for (attacker in botArmy) {
            val target = playerArmy
                .filter { (playerHp[it.id] ?: 0) > 0 }
                .minByOrNull { attacker.pos.distanceTo(it.pos) }
                ?: continue
            if (attacker.pos.distanceTo(target.pos) <= rangeOf(attacker.type)) {
                val dmg = baseDamage(attacker.type, target.type) * dt
                playerHp[target.id] = ((playerHp[target.id] ?: 0) - dmg.toInt()).coerceAtLeast(0)
                botAttacked += attacker.id
            }
        }

        val updatedPlayer = playerArmy
            .filter { (playerHp[it.id] ?: 0) > 0 }
            .map { u ->
                val timer = if (u.id in playerAttacked) 0f else u.secondsSinceLastCombat
                u.copy(hp = playerHp[u.id] ?: u.hp, secondsSinceLastCombat = timer)
            }

        val updatedBot = botArmy
            .filter { (botHp[it.id] ?: 0) > 0 }
            .map { u ->
                val timer = if (u.id in botAttacked) 0f else u.secondsSinceLastCombat
                u.copy(hp = botHp[u.id] ?: u.hp, secondsSinceLastCombat = timer)
            }

        return CombatTickResult(updatedPlayer, updatedBot)
    }
}

// ---------------------------------------------------------------------------
// Main controller
// ---------------------------------------------------------------------------

class AgeOfEmpiresLiteController {

    enum class Outcome {
        PLAYER_KING_DEAD,
        ENEMY_KING_DEAD,
        PLAYER_ENLIGHTENMENT,
        ENEMY_ENLIGHTENMENT,
        PLAYER_PLOUGHSHARES,
        ENEMY_PLOUGHSHARES
    }

    // -----------------------------------------------------------------------
    // Difficulty parameters
    // -----------------------------------------------------------------------

    private fun botArmyCap(difficulty: GameDifficulty) = when (difficulty) {
        GameDifficulty.EASY -> 8
        GameDifficulty.MEDIUM -> 10
        GameDifficulty.HARD -> 12
    }

    private fun botUpgradeSpeedMultiplier(difficulty: GameDifficulty) = when (difficulty) {
        GameDifficulty.EASY -> 0.5f
        GameDifficulty.MEDIUM -> 1.0f
        GameDifficulty.HARD -> 2.0f
    }

    private fun botEconomyMultiplier(difficulty: GameDifficulty) = when (difficulty) {
        GameDifficulty.HARD -> 1.5f
        else -> 1.0f
    }

    private val playerArmyCap = 10

    // -----------------------------------------------------------------------
    // Initial state
    // -----------------------------------------------------------------------

    fun initialState(settings: MatchSettings): AgeOfEmpiresLiteState {
        var id = 0
        val playerWorkers = (0 until 3).map { Worker(id++, Resource.FOOD) }
        val botWorkers = (0 until 3).map { Worker(id++, Resource.FOOD) }

        fun spawnUnit(type: UnitType, side: Boolean, index: Int): MilitaryUnit {
            val spawnX = if (side) PLAYER_SPAWN_X else BOT_SPAWN_X
            val y = 200f + index * 80f
            return MilitaryUnit(
                id = id++,
                type = type,
                side = side,
                hp = UNIT_MAX_HP[type] ?: 60,
                maxHp = UNIT_MAX_HP[type] ?: 60,
                pos = Vec2(spawnX, y)
            )
        }

        val playerArmy = listOf(
            spawnUnit(UnitType.INFANTRY, true, 0),
            spawnUnit(UnitType.INFANTRY, true, 1),
            spawnUnit(UnitType.ARCHER, true, 2)
        )
        val botArmy = listOf(
            spawnUnit(UnitType.INFANTRY, false, 0),
            spawnUnit(UnitType.INFANTRY, false, 1),
            spawnUnit(UnitType.ARCHER, false, 2)
        )

        return AgeOfEmpiresLiteState(
            settings = settings,
            playerResources = ResourcePool(food = 100, study = 50),
            botResources = ResourcePool(food = 100, study = 50),
            playerWorkers = playerWorkers,
            botWorkers = botWorkers,
            playerArmy = playerArmy,
            botArmy = botArmy,
            playerKing = King(side = true, hp = 100, maxHp = 100, pos = Vec2(PLAYER_KING_X, KING_Y)),
            botKing = King(side = false, hp = 100, maxHp = 100, pos = Vec2(BOT_KING_X, KING_Y)),
            playerResearched = emptySet(),
            botResearched = emptySet(),
            playerCannonQueue = 0,
            botCannonQueue = 0,
            elapsedSeconds = 0f,
            enlightenmentCountdown = null,
            enlightenmentSide = null,
            nextUnitId = id
        )
    }

    // -----------------------------------------------------------------------
    // Resource tick (both sides)
    // -----------------------------------------------------------------------

    fun tickResources(state: AgeOfEmpiresLiteState, dt: Float): AgeOfEmpiresLiteState {
        val difficulty = state.settings.difficulty
        val newElapsed = state.elapsedSeconds + dt

        // Spawn workers when elapsed crosses each interval boundary
        fun spawnedWorkerCount(currentCount: Int, prev: Float, next: Float): Int {
            val prevIntervals = (prev / WORKER_SPAWN_INTERVAL).toInt()
            val nextIntervals = (next / WORKER_SPAWN_INTERVAL).toInt()
            val newWorkers = nextIntervals - prevIntervals
            return (currentCount + newWorkers).coerceAtMost(MAX_WORKERS)
        }

        val newPlayerWorkerCount = spawnedWorkerCount(state.playerWorkers.size, state.elapsedSeconds, newElapsed)
        val newBotWorkerCount = spawnedWorkerCount(state.botWorkers.size, state.elapsedSeconds, newElapsed)

        // Re-assign workers to match economy balance
        fun buildWorkers(count: Int, economy: EconomyBalance, startId: Int): List<Worker> {
            val assign = WorkerAssigner.assignWorkers(count, economy)
            var id = startId
            return (0 until assign.foodWorkers).map { Worker(id++, Resource.FOOD) } +
                   (0 until assign.studyWorkers).map { Worker(id++, Resource.STUDY) }
        }

        // Use a large stable id base for bot workers to avoid collision with unit ids
        val playerWorkers = buildWorkers(newPlayerWorkerCount, state.settings.economy, startId = 10_000)
        val botWorkers = buildWorkers(newBotWorkerCount, EconomyBalance(60, 40), startId = 20_000)

        val playerAssign = WorkerAssigner.assignWorkers(newPlayerWorkerCount, state.settings.economy)
        val botAssign = WorkerAssigner.assignWorkers(newBotWorkerCount, EconomyBalance(60, 40))
        val botMult = botEconomyMultiplier(difficulty)

        val playerFood = state.playerResources.food +
                (playerAssign.foodWorkers * FOOD_RATE_PER_SEC * dt).toInt()
        val playerStudy = state.playerResources.study +
                (playerAssign.studyWorkers * STUDY_RATE_PER_SEC * dt).toInt()
        val botFood = state.botResources.food +
                (botAssign.foodWorkers * FOOD_RATE_PER_SEC * dt * botMult).toInt()
        val botStudy = state.botResources.study +
                (botAssign.studyWorkers * STUDY_RATE_PER_SEC * dt * botMult).toInt()

        return state.copy(
            elapsedSeconds = newElapsed,
            playerResources = ResourcePool(food = playerFood, study = playerStudy),
            botResources = ResourcePool(food = botFood, study = botStudy),
            playerWorkers = playerWorkers,
            botWorkers = botWorkers
        )
    }

    // -----------------------------------------------------------------------
    // Army training
    // -----------------------------------------------------------------------

    private fun trainUnit(
        army: List<MilitaryUnit>,
        cannonQueue: Int,
        resources: ResourcePool,
        settings: MatchSettings,
        armyCap: Int,
        side: Boolean,
        nextId: Int
    ): Triple<List<MilitaryUnit>, Int, ResourcePool> {
        val toTrain = ArmyTrainer.nextUnitToTrain(
            army = army,
            target = settings.army,
            cannonQueue = cannonQueue,
            food = resources.food,
            armyCap = armyCap,
            playerSide = side
        ) ?: return Triple(army, cannonQueue, resources)

        val cost = UNIT_COSTS[toTrain] ?: return Triple(army, cannonQueue, resources)
        val spawnX = if (side) PLAYER_SPAWN_X else BOT_SPAWN_X
        val y = 100f + (army.size % 8) * 60f
        val newUnit = MilitaryUnit(
            id = nextId,
            type = toTrain,
            side = side,
            hp = UNIT_MAX_HP[toTrain] ?: 60,
            maxHp = UNIT_MAX_HP[toTrain] ?: 60,
            pos = Vec2(spawnX, y)
        )
        val newCannonQueue = if (toTrain == UnitType.CANNON) (cannonQueue - 1).coerceAtLeast(0) else cannonQueue
        return Triple(
            army + newUnit,
            newCannonQueue,
            resources.copy(food = resources.food - cost)
        )
    }

    // -----------------------------------------------------------------------
    // Upgrade research
    // -----------------------------------------------------------------------

    private fun researchUpgrade(
        researched: Set<UpgradeId>,
        priority: UpgradePriority,
        resources: ResourcePool,
        studySpeedMultiplier: Float = 1f
    ): Pair<Set<UpgradeId>, ResourcePool> {
        val id = UpgradeScheduler.nextUpgrade(priority, researched, resources.study) ?: return researched to resources
        val cost = ((UPGRADE_COSTS[id] ?: Int.MAX_VALUE) / studySpeedMultiplier).toInt()
        return if (resources.study >= cost) {
            (researched + id) to resources.copy(study = resources.study - cost)
        } else {
            researched to resources
        }
    }

    // -----------------------------------------------------------------------
    // Unit movement
    // -----------------------------------------------------------------------

    private fun advanceUnits(units: List<MilitaryUnit>, dt: Float): List<MilitaryUnit> {
        return units.map { unit ->
            val direction = if (unit.side) 1f else -1f
            unit.copy(pos = Vec2(unit.pos.x + direction * UNIT_ADVANCE_SPEED * dt, unit.pos.y))
        }
    }

    // -----------------------------------------------------------------------
    // King combat (units that reach the enemy base attack the king)
    // -----------------------------------------------------------------------

    private fun resolveKingCombat(
        playerArmy: List<MilitaryUnit>,
        botArmy: List<MilitaryUnit>,
        playerKing: King,
        botKing: King,
        dt: Float
    ): Pair<King, King> {
        var pKingHp = playerKing.hp
        var bKingHp = botKing.hp

        // Bot units near player king attack it
        for (unit in botArmy) {
            if (unit.pos.x <= playerKing.pos.x + COMBAT_RANGE_PX * 2) {
                val dmg = (CombatResolver.run {
                    // use a simplified damage just for king
                    when (unit.type) {
                        UnitType.INFANTRY -> 20f
                        UnitType.ARCHER -> 15f
                        UnitType.CAVALRY -> 25f
                        UnitType.CANNON -> 50f
                    }
                } * dt).toInt()
                pKingHp = (pKingHp - dmg).coerceAtLeast(0)
            }
        }

        // Player units near enemy king attack it
        for (unit in playerArmy) {
            if (unit.pos.x >= botKing.pos.x - COMBAT_RANGE_PX * 2) {
                val dmg = (when (unit.type) {
                    UnitType.INFANTRY -> 20f
                    UnitType.ARCHER -> 15f
                    UnitType.CAVALRY -> 25f
                    UnitType.CANNON -> 50f
                } * dt).toInt()
                bKingHp = (bKingHp - dmg).coerceAtLeast(0)
            }
        }

        return playerKing.copy(hp = pKingHp) to botKing.copy(hp = bKingHp)
    }

    // -----------------------------------------------------------------------
    // Enlightenment countdown
    // -----------------------------------------------------------------------

    private fun tickEnlightenment(state: AgeOfEmpiresLiteState, dt: Float): AgeOfEmpiresLiteState {
        val cd = state.enlightenmentCountdown ?: return state
        val newCd = (cd - dt).coerceAtLeast(0f)
        return state.copy(enlightenmentCountdown = newCd)
    }

    private fun startEnlightenmentIfReady(state: AgeOfEmpiresLiteState): AgeOfEmpiresLiteState {
        if (state.enlightenmentCountdown != null) return state

        val playerReady = UpgradeId.ENLIGHTENMENT in state.playerResearched
        val botReady = UpgradeId.ENLIGHTENMENT in state.botResearched

        return when {
            playerReady -> state.copy(
                enlightenmentCountdown = ENLIGHTENMENT_DURATION,
                enlightenmentSide = true
            )
            botReady -> state.copy(
                enlightenmentCountdown = ENLIGHTENMENT_DURATION,
                enlightenmentSide = false
            )
            else -> state
        }
    }

    // -----------------------------------------------------------------------
    // Full tick
    // -----------------------------------------------------------------------

    fun tick(state: AgeOfEmpiresLiteState, dt: Float): AgeOfEmpiresLiteState {
        var s = tickResources(state, dt)

        // Player: train unit
        val (playerArmy1, pCannonQ1, pRes1) = trainUnit(
            s.playerArmy, s.playerCannonQueue, s.playerResources,
            s.settings, playerArmyCap, side = true, s.nextUnitId
        )
        s = s.copy(
            playerArmy = playerArmy1,
            playerCannonQueue = pCannonQ1,
            playerResources = pRes1,
            nextUnitId = s.nextUnitId + if (playerArmy1.size > s.playerArmy.size) 1 else 0
        )

        // Bot: train unit
        val botCap = botArmyCap(s.settings.difficulty)
        val botPriority = defaultBotUpgradePriority()
        val botArmyComp = defaultBotArmyComposition()
        val botSettings = s.settings.copy(army = botArmyComp)
        val (botArmy1, bCannonQ1, bRes1) = trainUnit(
            s.botArmy, s.botCannonQueue, s.botResources,
            botSettings, botCap, side = false, s.nextUnitId
        )
        s = s.copy(
            botArmy = botArmy1,
            botCannonQueue = bCannonQ1,
            botResources = bRes1,
            nextUnitId = s.nextUnitId + if (botArmy1.size > s.botArmy.size) 1 else 0
        )

        // Player: research upgrades (every ~5 seconds based on study)
        val (pResearched, pRes2) = researchUpgrade(
            s.playerResearched, s.settings.upgrades, s.playerResources
        )
        s = s.copy(playerResearched = pResearched, playerResources = pRes2)

        // Bot: research upgrades (scaled by difficulty)
        val botUpgradeMult = botUpgradeSpeedMultiplier(s.settings.difficulty)
        val (bResearched, bRes2) = researchUpgrade(
            s.botResearched, botPriority, s.botResources, botUpgradeMult
        )
        s = s.copy(botResearched = bResearched, botResources = bRes2)

        // Enlightenment tracking
        s = startEnlightenmentIfReady(s)
        s = tickEnlightenment(s, dt)

        // Advance units toward enemy
        s = s.copy(
            playerArmy = advanceUnits(s.playerArmy, dt),
            botArmy = advanceUnits(s.botArmy, dt)
        )

        // Combat between armies
        val combatResult = CombatResolver.resolveCombatTick(s.playerArmy, s.botArmy, dt)

        // Regenerate out-of-combat units
        val regenPlayer = UnitRegenerator.regenerate(combatResult.updatedPlayer, dt)
        val regenBot = UnitRegenerator.regenerate(combatResult.updatedBot, dt)

        s = s.copy(playerArmy = regenPlayer, botArmy = regenBot)

        // King combat
        val (newPlayerKing, newBotKing) = resolveKingCombat(
            s.playerArmy, s.botArmy, s.playerKing, s.botKing, dt
        )
        s = s.copy(playerKing = newPlayerKing, botKing = newBotKing)

        return s
    }

    // -----------------------------------------------------------------------
    // Victory condition check
    // -----------------------------------------------------------------------

    fun checkVictoryConditions(state: AgeOfEmpiresLiteState): Outcome? {
        if (state.botKing.hp <= 0) return Outcome.ENEMY_KING_DEAD
        if (state.playerKing.hp <= 0) return Outcome.PLAYER_KING_DEAD

        val cd = state.enlightenmentCountdown
        if (cd != null && cd <= 0f) {
            return when (state.enlightenmentSide) {
                true -> Outcome.PLAYER_ENLIGHTENMENT
                false -> Outcome.ENEMY_ENLIGHTENMENT
                null -> null
            }
        }

        if (state.playerResources.food >= state.ploughsharesThreshold) return Outcome.PLAYER_PLOUGHSHARES
        if (state.botResources.food >= state.ploughsharesThreshold) return Outcome.ENEMY_PLOUGHSHARES

        return null
    }

    // -----------------------------------------------------------------------
    // Bot default policies
    // -----------------------------------------------------------------------

    private fun defaultBotUpgradePriority(): UpgradePriority = UpgradePriority(
        listOf(
            UpgradeId.ATTACK_I, UpgradeId.DEFENSE_I, UpgradeId.SPEED_I,
            UpgradeId.ATTACK_II, UpgradeId.DEFENSE_II, UpgradeId.SPEED_II,
            UpgradeId.CITY_WALL_I, UpgradeId.AGRICULTURE_I, UpgradeId.LEARNING_I,
            UpgradeId.ATTACK_III, UpgradeId.DEFENSE_III, UpgradeId.SPEED_III,
            UpgradeId.CITY_WALL_II, UpgradeId.AGRICULTURE_II, UpgradeId.LEARNING_II,
            UpgradeId.CITY_WALL_III, UpgradeId.AGRICULTURE_III, UpgradeId.LEARNING_III,
            UpgradeId.ENLIGHTENMENT
        )
    )

    private fun defaultBotArmyComposition(): ArmyComposition = ArmyComposition(
        mapOf(
            UnitType.INFANTRY to 3,
            UnitType.ARCHER to 2,
            UnitType.CAVALRY to 1
        )
    )

    companion object {
        /** Default player upgrade priority (used for UI initialisation). */
        fun defaultPlayerUpgradePriority(): UpgradePriority = UpgradePriority(
            listOf(
                UpgradeId.ATTACK_I, UpgradeId.DEFENSE_I,
                UpgradeId.AGRICULTURE_I, UpgradeId.LEARNING_I,
                UpgradeId.SPEED_I, UpgradeId.CITY_WALL_I,
                UpgradeId.ATTACK_II, UpgradeId.DEFENSE_II,
                UpgradeId.AGRICULTURE_II, UpgradeId.LEARNING_II,
                UpgradeId.SPEED_II, UpgradeId.CITY_WALL_II,
                UpgradeId.ATTACK_III, UpgradeId.DEFENSE_III,
                UpgradeId.AGRICULTURE_III, UpgradeId.LEARNING_III,
                UpgradeId.SPEED_III, UpgradeId.CITY_WALL_III,
                UpgradeId.ENLIGHTENMENT
            )
        )

        /** Default player army composition (used for UI initialisation). */
        fun defaultPlayerArmyComposition(): ArmyComposition = ArmyComposition(
            mapOf(
                UnitType.INFANTRY to 3,
                UnitType.ARCHER to 2,
                UnitType.CAVALRY to 1
            )
        )

        fun defaultMatchSettings(difficulty: GameDifficulty) = MatchSettings(
            difficulty = difficulty,
            economy = EconomyBalance(foodPct = 70, studyPct = 30),
            army = defaultPlayerArmyComposition(),
            upgrades = defaultPlayerUpgradePriority()
        )
    }
}
