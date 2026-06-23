package com.xanticious.androidgames.controller.games.anomalydefense

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.GridPos
import com.xanticious.androidgames.model.games.anomalydefense.AnomalyDefenseState
import com.xanticious.androidgames.model.games.anomalydefense.Assignment
import com.xanticious.androidgames.model.games.anomalydefense.AssaultResult
import com.xanticious.androidgames.model.games.anomalydefense.AttackPlan
import com.xanticious.androidgames.model.games.anomalydefense.AttackUnit
import com.xanticious.androidgames.model.games.anomalydefense.Defense
import com.xanticious.androidgames.model.games.anomalydefense.Level
import com.xanticious.androidgames.model.games.anomalydefense.Route
import com.xanticious.androidgames.model.games.anomalydefense.Turret
import com.xanticious.androidgames.model.games.anomalydefense.UnitCost
import com.xanticious.androidgames.model.games.anomalydefense.UnitInTransit
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

private const val GRID_COLS = 10
private const val GRID_ROWS = 8
private const val SABOTEUR_DISABLE_DURATION = 2.0f

/** Per-unit base statistics used by the simulator. */
private data class UnitStats(
    val hp: Int,
    val speed: Float,
    val shieldHits: Int = 0
)

private fun statsFor(unit: AttackUnit): UnitStats = when (unit) {
    AttackUnit.RUNNER   -> UnitStats(hp = 30,  speed = 3.0f)
    AttackUnit.TANK     -> UnitStats(hp = 120, speed = 1.0f)
    AttackUnit.SHIELDED -> UnitStats(hp = 60,  speed = 2.0f, shieldHits = 3)
    AttackUnit.SABOTEUR -> UnitStats(hp = 40,  speed = 2.0f)
}

/** Returns the float (x, y) of a unit interpolated along [route] at [progress]. */
internal fun unitFloatPos(progress: Float, route: Route): Pair<Float, Float> {
    val clamped = progress.coerceIn(0f, (route.tiles.size - 1).toFloat())
    val idx = clamped.toInt().coerceIn(0, route.tiles.size - 2)
    val frac = clamped - idx
    val a = route.tiles[idx]
    val b = route.tiles.getOrElse(idx + 1) { a }
    return Pair(a.x + (b.x - a.x) * frac, a.y + (b.y - a.y) * frac)
}

/** Euclidean distance from a float position to a grid tile centre. */
internal fun floatDist(pos: Pair<Float, Float>, grid: GridPos): Float {
    val dx = pos.first - grid.x
    val dy = pos.second - grid.y
    return sqrt(dx * dx + dy * dy)
}

// ---------------------------------------------------------------------------
// Level generator
// ---------------------------------------------------------------------------

object LevelGenerator {

    private val defaultUnitCosts = listOf(
        UnitCost(AttackUnit.RUNNER,   20),
        UnitCost(AttackUnit.TANK,     60),
        UnitCost(AttackUnit.SHIELDED, 40),
        UnitCost(AttackUnit.SABOTEUR, 45)
    )

    /**
     * Produces a procedurally generated, guaranteed-beatable [Level].
     * Tries up to 12 seeds before falling back to a safe default.
     */
    fun generateLevel(seed: Long, difficulty: GameDifficulty): Level {
        var currentSeed = seed
        repeat(12) {
            val rand = Random(currentSeed)
            val defense = buildDefense(rand)
            val report = SolvabilityVerifier.verify(defense, defaultUnitCosts, 1000, currentSeed)
            if (report.beatable && report.referenceCost > 0) {
                val budget = BudgetSetter.setBudget(report.referenceCost, difficulty)
                return Level(defense, budget, currentSeed, defaultUnitCosts)
            }
            currentSeed += 1
        }
        // Fallback: open level that any single RUNNER can clear
        val fallbackDefense = buildOpenDefense()
        val fallbackBudget = BudgetSetter.setBudget(20, difficulty)
        return Level(fallbackDefense, fallbackBudget, currentSeed, defaultUnitCosts)
    }

    private fun buildDefense(rand: Random): Defense {
        val numRoutes = rand.nextInt(2, 4) // 2 or 3
        val routes = buildRoutes(rand, numRoutes)
        val routeTileSet = routes.flatMap { it.tiles }.toSet()
        val numTurrets = rand.nextInt(4, 9) // 4..8
        val turrets = buildTurrets(rand, routeTileSet, numTurrets)
        val objective = GridPos(GRID_COLS - 1, (GRID_ROWS / 2))
        return Defense(routes, turrets, objective, quota = 1)
    }

    private fun buildRoutes(rand: Random, numRoutes: Int): List<Route> {
        val rowSpacing = GRID_ROWS / (numRoutes + 1)
        return List(numRoutes) { idx ->
            var y = (idx + 1) * rowSpacing
            val tiles = mutableListOf<GridPos>()
            for (x in 0 until GRID_COLS) {
                tiles += GridPos(x, y)
                if (x < GRID_COLS - 1) {
                    val delta = rand.nextInt(-1, 2) // -1, 0, or 1
                    y = (y + delta).coerceIn(1, GRID_ROWS - 2)
                }
            }
            Route(id = idx, tiles = tiles)
        }
    }

    private fun buildTurrets(rand: Random, routeTiles: Set<GridPos>, count: Int): List<Turret> {
        val candidates = mutableListOf<GridPos>()
        for (pos in routeTiles) {
            for (dy in -2..2) {
                for (dx in -2..2) {
                    if (dx == 0 && dy == 0) continue
                    val c = GridPos(pos.x + dx, pos.y + dy)
                    if (c !in routeTiles && c.x in 1 until GRID_COLS - 1 && c.y in 0 until GRID_ROWS && c !in candidates) {
                        candidates += c
                    }
                }
            }
        }
        candidates.shuffle(rand)
        return candidates.take(count).mapIndexed { i, pos ->
            Turret(
                id = i,
                pos = pos,
                range = rand.nextFloat() * 1.5f + 1.5f,   // 1.5 – 3.0
                damage = rand.nextInt(5, 21),              // 5 – 20
                fireRate = rand.nextFloat() * 0.8f + 0.5f // 0.5 – 1.3 fires/sec
            )
        }
    }

    /** A single straight-line route with no turrets — always beatable. */
    private fun buildOpenDefense(): Defense {
        val route = Route(id = 0, tiles = List(GRID_COLS) { x -> GridPos(x, GRID_ROWS / 2) })
        val objective = GridPos(GRID_COLS - 1, GRID_ROWS / 2)
        return Defense(routes = listOf(route), turrets = emptyList(), objective = objective, quota = 1)
    }
}

// ---------------------------------------------------------------------------
// Assault simulator
// ---------------------------------------------------------------------------

object AssaultSimulator {

    /**
     * Full deterministic simulation of [plan] against [defense].
     * Uses internal mutable structures for performance; returns an immutable [AssaultResult].
     */
    fun simulate(
        defense: Defense,
        plan: AttackPlan,
        unitCosts: List<UnitCost>,
        seed: Long,
        stepDt: Float = 0.1f
    ): AssaultResult {
        // Mutable simulation nodes (local only, never escape this function)
        class SimUnit(
            val id: Int,
            val unit: AttackUnit,
            val routeId: Int,
            var progress: Float,
            val maxHp: Int,
            var hp: Int,
            var shieldHits: Int,
            val speed: Float
        )

        class SimTurret(
            val id: Int,
            val pos: GridPos,
            val range: Float,
            val damage: Int,
            val fireRate: Float,
            var disabled: Boolean = false,
            var disableRemaining: Float = 0f,
            var fireCooldown: Float = 0f
        )

        var idCounter = 0
        val units = plan.assignments.flatMap { a ->
            a.units.map { unit ->
                val s = statsFor(unit)
                SimUnit(idCounter++, unit, a.routeId, 0f, s.hp, s.hp, s.shieldHits, s.speed)
            }
        }.toMutableList()

        val turrets = defense.turrets.map { t ->
            SimTurret(t.id, t.pos, t.range, t.damage, t.fireRate)
        }.toMutableList()

        var unitsThrough = 0
        val maxTicks = 3000

        repeat(maxTicks) {
            if (units.isEmpty()) return@repeat

            // 1. Reduce turret disable timers
            for (t in turrets) {
                if (t.disabled) {
                    t.disableRemaining -= stepDt
                    if (t.disableRemaining <= 0f) {
                        t.disabled = false
                        t.disableRemaining = 0f
                    }
                }
            }

            // 2. Reduce turret fire cooldowns
            for (t in turrets) {
                if (!t.disabled) t.fireCooldown = (t.fireCooldown - stepDt).coerceAtLeast(0f)
            }

            // 3. Move units forward
            for (u in units) u.progress += u.speed * stepDt

            // 4. Collect units that reached the objective
            val reached = units.filter { u ->
                val route = defense.routes.firstOrNull { it.id == u.routeId } ?: return@filter false
                u.progress >= route.tiles.size
            }
            unitsThrough += reached.size
            units.removeAll(reached.toSet())
            if (units.isEmpty()) return@repeat

            // 5. SABOTEUR: disable turrets within range
            for (u in units) {
                if (u.unit != AttackUnit.SABOTEUR) continue
                val route = defense.routes.firstOrNull { it.id == u.routeId } ?: continue
                val pos = unitFloatPos(u.progress, route)
                for (t in turrets) {
                    if (floatDist(pos, t.pos) <= t.range) {
                        t.disabled = true
                        t.disableRemaining = SABOTEUR_DISABLE_DURATION
                    }
                }
            }

            // 6. Turrets fire at first unit in range
            for (t in turrets) {
                if (t.disabled || t.fireCooldown > 0f) continue
                val target = units.firstOrNull { u ->
                    val route = defense.routes.firstOrNull { it.id == u.routeId } ?: return@firstOrNull false
                    floatDist(unitFloatPos(u.progress, route), t.pos) <= t.range
                } ?: continue
                t.fireCooldown = 1f / t.fireRate
                if (target.shieldHits > 0) target.shieldHits-- else target.hp -= t.damage
            }

            // 7. Remove dead units
            units.removeAll { it.hp <= 0 }
        }

        return AssaultResult(unitsThrough, unitsThrough >= defense.quota)
    }
}

// ---------------------------------------------------------------------------
// Solvability verifier
// ---------------------------------------------------------------------------

data class SolvabilityReport(val beatable: Boolean, val referenceCost: Int)

object SolvabilityVerifier {

    /**
     * Runs [iterations] random attack plans within [budget] against [defense] and
     * returns whether any succeeded along with the minimum winning cost.
     */
    fun verify(
        defense: Defense,
        unitCosts: List<UnitCost>,
        budget: Int,
        seed: Long,
        iterations: Int = 50
    ): SolvabilityReport {
        val rand = Random(seed)
        var beatable = false
        var minWinCost = Int.MAX_VALUE

        repeat(iterations) {
            val (plan, spent) = randomPlan(rand, defense.routes, unitCosts, budget)
            if (plan.assignments.isEmpty()) return@repeat
            val result = AssaultSimulator.simulate(defense, plan, unitCosts, seed)
            if (result.won) {
                beatable = true
                if (spent < minWinCost) minWinCost = spent
            }
        }

        return SolvabilityReport(beatable, if (beatable) minWinCost else budget)
    }

    private fun randomPlan(
        rand: Random,
        routes: List<Route>,
        unitCosts: List<UnitCost>,
        budget: Int
    ): Pair<AttackPlan, Int> {
        var remaining = budget
        val unitsByRoute = mutableMapOf<Int, MutableList<AttackUnit>>()
        routes.forEach { unitsByRoute[it.id] = mutableListOf() }

        while (remaining > 0) {
            val affordable = unitCosts.filter { it.cost <= remaining }
            if (affordable.isEmpty()) break
            val chosen = affordable.random(rand)
            remaining -= chosen.cost
            val routeId = routes.random(rand).id
            unitsByRoute[routeId]?.add(chosen.unit)
        }

        val assignments = unitsByRoute
            .filter { it.value.isNotEmpty() }
            .map { (routeId, units) -> Assignment(routeId, units.toList()) }
        return Pair(AttackPlan(assignments), budget - remaining)
    }
}

// ---------------------------------------------------------------------------
// Budget setter
// ---------------------------------------------------------------------------

object BudgetSetter {
    fun setBudget(referenceCost: Int, difficulty: GameDifficulty): Int = when (difficulty) {
        GameDifficulty.EASY   -> referenceCost * 2
        GameDifficulty.MEDIUM -> (referenceCost * 1.5).roundToInt()
        GameDifficulty.HARD   -> (referenceCost * 1.1).roundToInt()
    }
}

// ---------------------------------------------------------------------------
// Game-state helpers
// ---------------------------------------------------------------------------

/** Purchases [unit] and deducts its cost. Returns null if budget is insufficient. */
fun buyUnit(state: AnomalyDefenseState, unit: AttackUnit): AnomalyDefenseState? {
    val cost = state.level.unitCosts.firstOrNull { it.unit == unit }?.cost ?: return null
    if (state.budgetRemaining < cost) return null
    return state.copy(
        purchasedUnits = state.purchasedUnits + unit,
        budgetRemaining = state.budgetRemaining - cost
    )
}

/** Removes the unit at [unitIndex] from [purchasedUnits] and appends it to [routeId]'s assignment. */
fun assignUnit(state: AnomalyDefenseState, unitIndex: Int, routeId: Int): AnomalyDefenseState {
    val unit = state.purchasedUnits.getOrNull(unitIndex) ?: return state
    val updatedPurchased = state.purchasedUnits.toMutableList().also { it.removeAt(unitIndex) }

    val existing = state.plan.assignments.firstOrNull { it.routeId == routeId }
    val updatedAssignments = if (existing != null) {
        state.plan.assignments.map { a ->
            if (a.routeId == routeId) a.copy(units = a.units + unit) else a
        }
    } else {
        state.plan.assignments + Assignment(routeId, listOf(unit))
    }

    return state.copy(
        purchasedUnits = updatedPurchased,
        plan = state.plan.copy(assignments = updatedAssignments)
    )
}

/**
 * Advances the assault simulation by [dt] seconds.
 * Handles movement, SABOTEUR disabling, turret firing, and terminal conditions.
 */
fun tickAssault(state: AnomalyDefenseState, dt: Float): AnomalyDefenseState {
    if (!state.assaultInProgress) return state
    val defense = state.level.defense

    // ── 1. Update turret timers ──────────────────────────────────────────────
    val turretsStep1 = defense.turrets.map { t ->
        when {
            t.disabled -> {
                val rem = (t.disableRemaining - dt).coerceAtLeast(0f)
                t.copy(disableRemaining = rem, disabled = rem > 0f)
            }
            else -> t.copy(fireCooldown = (t.fireCooldown - dt).coerceAtLeast(0f))
        }
    }

    // ── 2. Move units ────────────────────────────────────────────────────────
    val movedUnits = state.unitsInTransit.map { u -> u.copy(progress = u.progress + u.speed * dt) }

    // ── 3. Separate units that reached the objective ─────────────────────────
    val (through, still) = movedUnits.partition { u ->
        val route = defense.routes.firstOrNull { it.id == u.routeId } ?: return@partition false
        u.progress >= route.tiles.size
    }
    val newUnitsThrough = state.unitsThrough + through.size

    // ── 4. SABOTEUR disabling ────────────────────────────────────────────────
    val turretsStep2 = turretsStep1.map { t ->
        val disabledBySaboteur = still.asSequence()
            .filter { it.unit == AttackUnit.SABOTEUR }
            .any { u ->
                val route = defense.routes.firstOrNull { it.id == u.routeId } ?: return@any false
                floatDist(unitFloatPos(u.progress, route), t.pos) <= t.range
            }
        if (disabledBySaboteur) t.copy(disabled = true, disableRemaining = SABOTEUR_DISABLE_DURATION)
        else t
    }

    // ── 5. Turret fire — use mutable list for target mutation ────────────────
    val mutableUnits = still.toMutableList()
    val turretsStep3 = turretsStep2.map { t ->
        if (t.disabled || t.fireCooldown > 0f) return@map t
        val targetIdx = mutableUnits.indexOfFirst { u ->
            val route = defense.routes.firstOrNull { it.id == u.routeId } ?: return@indexOfFirst false
            floatDist(unitFloatPos(u.progress, route), t.pos) <= t.range
        }
        if (targetIdx < 0) return@map t
        val hit = mutableUnits[targetIdx]
        mutableUnits[targetIdx] = if (hit.shieldHits > 0) {
            hit.copy(shieldHits = hit.shieldHits - 1)
        } else {
            hit.copy(hp = hit.hp - t.damage)
        }
        t.copy(fireCooldown = 1f / t.fireRate)
    }

    // ── 6. Remove dead units ─────────────────────────────────────────────────
    val aliveUnits = mutableUnits.filter { it.hp > 0 }

    // ── 7. Terminal condition checks ─────────────────────────────────────────
    val quota = defense.quota
    val won = newUnitsThrough >= quota
    val allGone = aliveUnits.isEmpty()
    val done = won || allGone
    val result = if (done) AssaultResult(newUnitsThrough, won) else null

    return state.copy(
        level = state.level.copy(defense = defense.copy(turrets = turretsStep3)),
        unitsInTransit = if (done) emptyList() else aliveUnits,
        unitsThrough = newUnitsThrough,
        assaultInProgress = !done,
        result = result
    )
}

/**
 * Spawns all units from [plan] onto their routes, producing the initial
 * [AnomalyDefenseState.unitsInTransit] list for the assault phase.
 */
fun spawnUnits(state: AnomalyDefenseState): AnomalyDefenseState {
    var idCounter = 0
    val inTransit = state.plan.assignments.flatMap { a ->
        a.units.map { unit ->
            val s = statsFor(unit)
            UnitInTransit(
                id = idCounter++,
                unit = unit,
                routeId = a.routeId,
                progress = 0f,
                maxHp = s.hp,
                hp = s.hp,
                shieldHits = s.shieldHits,
                speed = s.speed
            )
        }
    }
    return state.copy(unitsInTransit = inTransit, assaultInProgress = inTransit.isNotEmpty())
}
