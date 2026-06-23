package com.xanticious.androidgames.controller.games.empireskirmish

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.GridPos
import com.xanticious.androidgames.model.games.empireskirmish.Battle
import com.xanticious.androidgames.model.games.empireskirmish.EmpireSkirmishState
import com.xanticious.androidgames.model.games.empireskirmish.Side
import com.xanticious.androidgames.model.games.empireskirmish.SkirmishUnit
import com.xanticious.androidgames.model.games.empireskirmish.Tile
import com.xanticious.androidgames.model.games.empireskirmish.UnitStats
import com.xanticious.androidgames.model.games.empireskirmish.UnitType
import kotlin.random.Random

// ─── Unit Stats ────────────────────────────────────────────────────────────────

fun statsFor(type: UnitType): UnitStats = when (type) {
    UnitType.SWORDSMAN -> UnitStats(moveRange = 3, attackRange = 1, damage = 20, maxHp = 80)
    UnitType.BOWMAN    -> UnitStats(moveRange = 3, attackRange = 3, damage = 15, maxHp = 60)
    UnitType.MAGE      -> UnitStats(moveRange = 2, attackRange = 2, damage = 25, maxHp = 50)
    UnitType.KING      -> UnitStats(moveRange = 2, attackRange = 1, damage = 8,  maxHp = 40)
}

// ─── Level Generator ──────────────────────────────────────────────────────────

private const val COLS = 10
private const val ROWS = 8

/**
 * Generates a [Battle] with a 10x8 grid, random blocking/cover terrain,
 * and opposing rosters scaled by [difficulty].
 */
fun generateBattle(seed: Long, difficulty: GameDifficulty): Battle {
    val rng = Random(seed)

    val tiles = buildList {
        for (y in 0 until ROWS) {
            for (x in 0 until COLS) {
                val pos = GridPos(x, y)
                // Keep a clear corridor on far left (x<=1) and far right (x>=8) for spawns
                val isSafeZone = x <= 1 || x >= 8
                val blocking = !isSafeZone && rng.nextFloat() < 0.20f
                val cover    = !isSafeZone && !blocking && rng.nextFloat() < 0.15f
                add(Tile(pos = pos, blocking = blocking, cover = cover))
            }
        }
    }

    val playerUnits = buildPlayerRoster(difficulty, tiles)
    val enemyUnits  = buildEnemyRoster(difficulty, tiles, playerUnits.size)
    val allUnits    = playerUnits + enemyUnits

    return Battle(grid = tiles, cols = COLS, rows = ROWS, units = allUnits, seed = seed)
}

private fun buildPlayerRoster(difficulty: GameDifficulty, tiles: List<Tile>): List<SkirmishUnit> {
    // Player spawns on left side (x=0..1)
    val spawnPositions = listOf(
        GridPos(0, 1), GridPos(0, 3), GridPos(0, 5), GridPos(1, 2), GridPos(1, 6)
    ).filter { pos -> tiles.none { it.pos == pos && it.blocking } }

    val types = listOf(UnitType.SWORDSMAN, UnitType.SWORDSMAN, UnitType.BOWMAN, UnitType.MAGE, UnitType.KING)
    return types.take(spawnPositions.size).mapIndexed { i, type ->
        val stats = statsFor(type)
        SkirmishUnit(
            id = i,
            type = type,
            side = Side.PLAYER,
            pos = spawnPositions[i],
            hp = stats.maxHp,
            maxHp = stats.maxHp
        )
    }
}

private fun buildEnemyRoster(difficulty: GameDifficulty, tiles: List<Tile>, playerCount: Int): List<SkirmishUnit> {
    // Enemy spawns on right side (x=8..9), mirrored
    val spawnPositions = listOf(
        GridPos(9, 1), GridPos(9, 3), GridPos(9, 5), GridPos(8, 2), GridPos(8, 6)
    ).filter { pos -> tiles.none { it.pos == pos && it.blocking } }

    val basTypes = listOf(UnitType.SWORDSMAN, UnitType.SWORDSMAN, UnitType.BOWMAN, UnitType.MAGE, UnitType.KING)
    val types = when (difficulty) {
        GameDifficulty.EASY   -> basTypes.drop(1) // one fewer swordsman on easy
        GameDifficulty.MEDIUM -> basTypes
        GameDifficulty.HARD   -> basTypes
    }

    return types.take(spawnPositions.size).mapIndexed { i, type ->
        val stats = statsFor(type)
        // HARD: enemy gets a small HP bonus
        val hpBonus = if (difficulty == GameDifficulty.HARD) (stats.maxHp * 0.25f).toInt() else 0
        val effectiveMaxHp = stats.maxHp + hpBonus
        SkirmishUnit(
            id = playerCount + i,
            type = type,
            side = Side.ENEMY,
            pos = spawnPositions[i],
            hp = effectiveMaxHp,
            maxHp = effectiveMaxHp
        )
    }
}

// ─── Move Resolver ────────────────────────────────────────────────────────────

/** BFS reachable tiles for [unit] given move range, blocking tiles, and ally positions. */
fun reachableTiles(state: EmpireSkirmishState, unit: SkirmishUnit): Set<GridPos> {
    val battle = state.battle
    val blockingSet = battle.grid.filter { it.blocking }.map { it.pos }.toSet()
    val allyPositions = battle.units
        .filter { it.side == unit.side && it.id != unit.id }
        .map { it.pos }.toSet()
    val stats = statsFor(unit.type)

    val visited = mutableSetOf<GridPos>()
    val queue = ArrayDeque<Pair<GridPos, Int>>()
    queue.add(unit.pos to 0)
    visited.add(unit.pos)

    while (queue.isNotEmpty()) {
        val (pos, dist) = queue.removeFirst()
        if (dist >= stats.moveRange) continue
        for (neighbour in pos.neighbours()) {
            if (neighbour in visited) continue
            if (neighbour.x < 0 || neighbour.x >= battle.cols) continue
            if (neighbour.y < 0 || neighbour.y >= battle.rows) continue
            if (neighbour in blockingSet) continue
            if (neighbour in allyPositions) continue
            visited.add(neighbour)
            queue.add(neighbour to dist + 1)
        }
    }
    visited.remove(unit.pos) // standing tile is not a move destination
    return visited
}

fun isValidMove(state: EmpireSkirmishState, unit: SkirmishUnit, target: GridPos): Boolean =
    target in reachableTiles(state, unit)

// ─── Combat Resolver ──────────────────────────────────────────────────────────

/** Enemy units within attack range of [unit] standing at [fromPos]. */
fun attackablePositions(
    state: EmpireSkirmishState,
    unit: SkirmishUnit,
    fromPos: GridPos = unit.pos
): Set<GridPos> {
    val stats = statsFor(unit.type)
    return state.battle.units
        .asSequence()
        .filter { it.side != unit.side }
        .filter { it.pos.manhattanDistanceTo(fromPos) <= stats.attackRange }
        .map { it.pos }
        .toSet()
}

/**
 * Rock-paper-scissors type modifier:
 * SWORDSMAN beats MAGE, BOWMAN beats SWORDSMAN, MAGE beats BOWMAN → +50% damage.
 */
private fun typeMultiplier(attackerType: UnitType, defenderType: UnitType): Float = when {
    attackerType == UnitType.SWORDSMAN && defenderType == UnitType.MAGE      -> 1.5f
    attackerType == UnitType.BOWMAN    && defenderType == UnitType.SWORDSMAN -> 1.5f
    attackerType == UnitType.MAGE      && defenderType == UnitType.BOWMAN    -> 1.5f
    else -> 1.0f
}

/**
 * Applies an attack from [attackerId] toward [targetPos].
 * MAGE hits a 3×3 AoE around the target; others hit the single target unit.
 * Cover reduces damage received by 25%.
 * Kills remove units from the battle.
 */
fun resolveAttack(
    state: EmpireSkirmishState,
    attackerId: Int,
    targetPos: GridPos
): EmpireSkirmishState {
    val attacker = state.battle.units.firstOrNull { it.id == attackerId } ?: return state
    val stats = statsFor(attacker.type)
    val coverSet = state.battle.grid.filter { it.cover }.map { it.pos }.toSet()

    val affectedPositions: Set<GridPos> = if (attacker.type == UnitType.MAGE) {
        // 3×3 area around target
        buildSet {
            for (dy in -1..1) for (dx in -1..1) add(GridPos(targetPos.x + dx, targetPos.y + dy))
        }
    } else {
        setOf(targetPos)
    }

    val updatedUnits = state.battle.units.map { defender ->
        if (defender.side == attacker.side || defender.pos !in affectedPositions) {
            defender
        } else {
            val coverReduction = if (defender.pos in coverSet) 0.75f else 1.0f
            val typeMul = typeMultiplier(attacker.type, defender.type)
            val rawDamage = (stats.damage * typeMul * coverReduction).toInt()
            defender.copy(hp = (defender.hp - rawDamage).coerceAtLeast(0))
        }
    }.filter { it.hp > 0 }

    val attackerUpdated = updatedUnits.firstOrNull { it.id == attacker.id }
        ?.let { it.copy(hasAttacked = true) }
        ?: return state.copy(
            battle = state.battle.copy(units = updatedUnits.map { u ->
                if (u.id == attacker.id) u.copy(hasAttacked = true) else u
            })
        )

    val finalUnits = updatedUnits.map { u ->
        if (u.id == attacker.id) attackerUpdated else u
    }

    val newBattle = state.battle.copy(units = finalUnits)
    return state.copy(
        battle = newBattle,
        winner = checkWinner(state.copy(battle = newBattle)),
        selectedUnitId = null,
        reachableTiles = emptySet(),
        attackablePositions = emptySet()
    )
}

// ─── Move / Undo ──────────────────────────────────────────────────────────────

fun moveUnit(state: EmpireSkirmishState, unitId: Int, target: GridPos): EmpireSkirmishState {
    val unit = state.battle.units.firstOrNull { it.id == unitId } ?: return state
    if (!isValidMove(state, unit, target)) return state

    val updatedUnits = state.battle.units.map { u ->
        if (u.id == unitId) u.copy(pos = target, hasMoved = true) else u
    }
    val movedUnit = updatedUnits.first { it.id == unitId }
    val newBattle = state.battle.copy(units = updatedUnits)
    val attackable = attackablePositions(state.copy(battle = newBattle), movedUnit)

    return state.copy(
        battle = newBattle,
        pendingMove = unit.pos,
        reachableTiles = emptySet(),
        attackablePositions = attackable
    )
}

/** Restores the unit to its pre-move position; only valid before attacking. */
fun undoMove(state: EmpireSkirmishState, unitId: Int): EmpireSkirmishState {
    val prevPos = state.pendingMove ?: return state
    val updatedUnits = state.battle.units.map { u ->
        if (u.id == unitId) u.copy(pos = prevPos, hasMoved = false) else u
    }
    val restoredUnit = updatedUnits.firstOrNull { it.id == unitId } ?: return state
    val newBattle = state.battle.copy(units = updatedUnits)
    val reachable = reachableTiles(state.copy(battle = newBattle), restoredUnit)

    return state.copy(
        battle = newBattle,
        pendingMove = null,
        reachableTiles = reachable,
        attackablePositions = attackablePositions(state.copy(battle = newBattle), restoredUnit)
    )
}

// ─── Win Detection ────────────────────────────────────────────────────────────

fun checkWinner(state: EmpireSkirmishState): Side? {
    val enemyKingAlive  = state.battle.units.any { it.side == Side.ENEMY  && it.type == UnitType.KING }
    val playerKingAlive = state.battle.units.any { it.side == Side.PLAYER && it.type == UnitType.KING }
    return when {
        !enemyKingAlive  -> Side.PLAYER
        !playerKingAlive -> Side.ENEMY
        else             -> null
    }
}

// ─── End Turn ─────────────────────────────────────────────────────────────────

/** Resets hasMoved/hasAttacked for all units and flips turn to enemy. */
fun endPlayerTurn(state: EmpireSkirmishState): EmpireSkirmishState {
    val refreshed = state.battle.units.map { u -> u.copy(hasMoved = false, hasAttacked = false) }
    return state.copy(
        battle = state.battle.copy(units = refreshed),
        isPlayerTurn = false,
        selectedUnitId = null,
        reachableTiles = emptySet(),
        attackablePositions = emptySet(),
        pendingMove = null
    )
}

private fun beginPlayerTurn(state: EmpireSkirmishState): EmpireSkirmishState {
    val refreshed = state.battle.units.map { u -> u.copy(hasMoved = false, hasAttacked = false) }
    return state.copy(
        battle = state.battle.copy(units = refreshed),
        isPlayerTurn = true,
        selectedUnitId = null,
        reachableTiles = emptySet(),
        attackablePositions = emptySet(),
        pendingMove = null,
        turnNumber = state.turnNumber + 1
    )
}

// ─── Enemy AI ─────────────────────────────────────────────────────────────────

/**
 * Computes a complete enemy turn and returns the new state.
 *
 * - EASY: random movement/attacks
 * - MEDIUM/NORMAL: move toward nearest player unit, attack if possible
 * - HARD: prioritise attacking player King; focus-fire weakest unit; AoE with MAGE
 */
fun computeEnemyTurn(
    state: EmpireSkirmishState,
    difficulty: GameDifficulty,
    random: Random = Random.Default
): EmpireSkirmishState {
    var current = state
    val enemyIds = current.battle.units.filter { it.side == Side.ENEMY }.map { it.id }

    for (id in enemyIds) {
        val unit = current.battle.units.firstOrNull { it.id == id } ?: continue
        current = when (difficulty) {
            GameDifficulty.EASY   -> aiActRandom(current, unit, random)
            GameDifficulty.MEDIUM -> aiActGreedy(current, unit, random)
            GameDifficulty.HARD   -> aiActHard(current, unit, random)
        }
        if (current.winner != null) break
    }

    return beginPlayerTurn(current)
}

private fun aiActRandom(state: EmpireSkirmishState, unit: SkirmishUnit, random: Random): EmpireSkirmishState {
    var s = state
    // Random move
    val reachable = reachableTiles(s, unit).toList()
    if (reachable.isNotEmpty()) {
        s = moveUnit(s, unit.id, reachable[random.nextInt(reachable.size)])
    }
    // Random attack
    val movedUnit = s.battle.units.firstOrNull { it.id == unit.id } ?: return s
    val targets = attackablePositions(s, movedUnit).toList()
    if (targets.isNotEmpty()) {
        s = resolveAttack(s, unit.id, targets[random.nextInt(targets.size)])
    }
    return s
}

private fun aiActGreedy(state: EmpireSkirmishState, unit: SkirmishUnit, random: Random): EmpireSkirmishState {
    var s = state
    val playerUnits = s.battle.units.filter { it.side == Side.PLAYER }
    if (playerUnits.isEmpty()) return s

    // Move toward nearest player unit
    val nearest = playerUnits.minByOrNull { it.pos.manhattanDistanceTo(unit.pos) }
    if (nearest != null) {
        val reachable = reachableTiles(s, unit)
        val bestMove = reachable.minByOrNull { it.manhattanDistanceTo(nearest.pos) }
        if (bestMove != null) {
            s = moveUnit(s, unit.id, bestMove)
        }
    }

    // Attack if possible
    val movedUnit = s.battle.units.firstOrNull { it.id == unit.id } ?: return s
    val targets = attackablePositions(s, movedUnit).toList()
    if (targets.isNotEmpty()) {
        s = resolveAttack(s, unit.id, targets[random.nextInt(targets.size)])
    }
    return s
}

private fun aiActHard(state: EmpireSkirmishState, unit: SkirmishUnit, random: Random): EmpireSkirmishState {
    var s = state
    val playerUnits = s.battle.units.filter { it.side == Side.PLAYER }
    if (playerUnits.isEmpty()) return s

    // Prioritise player King; fall back to weakest unit
    val primaryTarget = playerUnits.firstOrNull { it.type == UnitType.KING }
        ?: playerUnits.minByOrNull { it.hp }
        ?: return s

    // MAGE: find best move to maximise AoE hits
    if (unit.type == UnitType.MAGE) {
        val reachable = reachableTiles(s, unit)
        val bestMove = reachable.maxByOrNull { fromPos ->
            attackablePositions(s, unit, fromPos).size
        }
        if (bestMove != null) s = moveUnit(s, unit.id, bestMove)
    } else {
        val reachable = reachableTiles(s, unit)
        val bestMove = reachable.minByOrNull { it.manhattanDistanceTo(primaryTarget.pos) }
        if (bestMove != null) s = moveUnit(s, unit.id, bestMove)
    }

    // Attack: prefer king, else weakest, else random
    val movedUnit = s.battle.units.firstOrNull { it.id == unit.id } ?: return s
    val attackable = attackablePositions(s, movedUnit)
    if (attackable.isNotEmpty()) {
        val targetPos = attackable.firstOrNull { pos ->
            s.battle.units.any { it.pos == pos && it.type == UnitType.KING }
        } ?: attackable.minByOrNull { pos ->
            s.battle.units.firstOrNull { it.pos == pos }?.hp ?: Int.MAX_VALUE
        } ?: attackable.first()

        s = resolveAttack(s, unit.id, targetPos)
    }
    return s
}

// ─── Solvability Verifier ─────────────────────────────────────────────────────

data class SolvabilityReport(val beatable: Boolean)

/**
 * Runs [iterations] simulated battles where the player uses a greedy AI and
 * the enemy uses [difficulty] AI. Returns [SolvabilityReport.beatable] = true
 * if the player wins at least once.
 */
fun verify(battle: Battle, seed: Long, iterations: Int = 20): SolvabilityReport {
    repeat(iterations) { i ->
        var state = EmpireSkirmishState(
            battle = battle,
            isPlayerTurn = true,
            selectedUnitId = null,
            reachableTiles = emptySet(),
            attackablePositions = emptySet(),
            pendingMove = null,
            turnNumber = 1,
            winner = null
        )
        val rng = Random(seed + i)
        var safetyBrake = 0
        while (state.winner == null && safetyBrake < 200) {
            state = if (state.isPlayerTurn) {
                simulatePlayerTurn(state, rng)
            } else {
                computeEnemyTurn(state, GameDifficulty.MEDIUM, rng)
            }
            safetyBrake++
        }
        if (state.winner == Side.PLAYER) return SolvabilityReport(beatable = true)
    }
    return SolvabilityReport(beatable = false)
}

private fun simulatePlayerTurn(state: EmpireSkirmishState, random: Random): EmpireSkirmishState {
    var s = state
    val playerIds = s.battle.units.filter { it.side == Side.PLAYER }.map { it.id }
    for (id in playerIds) {
        val unit = s.battle.units.firstOrNull { it.id == id } ?: continue
        s = aiActGreedy(s, unit, random)
        if (s.winner != null) break
    }
    return endPlayerTurn(s)
}

// ─── Initial State Builder ────────────────────────────────────────────────────

fun initialState(battle: Battle): EmpireSkirmishState = EmpireSkirmishState(
    battle = battle,
    isPlayerTurn = true,
    selectedUnitId = null,
    reachableTiles = emptySet(),
    attackablePositions = emptySet(),
    pendingMove = null,
    turnNumber = 1,
    winner = null
)
