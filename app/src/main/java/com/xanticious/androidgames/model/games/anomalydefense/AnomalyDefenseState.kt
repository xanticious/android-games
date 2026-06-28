package com.xanticious.androidgames.model.games.anomalydefense

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.GridPos

enum class AttackUnit { RUNNER, TANK, SHIELDED, SABOTEUR }

data class Turret(
    val id: Int,
    val pos: GridPos,
    val range: Float,
    val damage: Int,
    val fireRate: Float,
    val disabled: Boolean = false,
    val disableRemaining: Float = 0f,
    /** Seconds until next shot; 0 means ready to fire. */
    val fireCooldown: Float = 0f
)

data class Route(
    val id: Int,
    val tiles: List<GridPos>
)

data class Defense(
    val routes: List<Route>,
    val turrets: List<Turret>,
    val objective: GridPos,
    val quota: Int      // units needed to reach objective for victory
)

data class UnitCost(val unit: AttackUnit, val cost: Int)

data class UnitInTransit(
    val id: Int,
    val unit: AttackUnit,
    val routeId: Int,
    /** Progress along the route [0, route.tiles.size). */
    val progress: Float,
    val maxHp: Int,
    val hp: Int,
    val shieldHits: Int,    // remaining shield hits (SHIELDED only; 0 = no shield)
    val speed: Float
)

data class Assignment(val routeId: Int, val units: List<AttackUnit>)
data class AttackPlan(val assignments: List<Assignment>)

data class AssaultResult(val unitsThrough: Int, val won: Boolean)

data class Level(
    val defense: Defense,
    val budget: Int,
    val seed: Long,
    val unitCosts: List<UnitCost>
)

data class AnomalyDefenseState(
    val level: Level,
    val purchasedUnits: List<AttackUnit>,
    val plan: AttackPlan,
    val budgetRemaining: Int,
    val assaultInProgress: Boolean,
    val unitsInTransit: List<UnitInTransit>,
    val unitsThrough: Int,
    val result: AssaultResult?
)
