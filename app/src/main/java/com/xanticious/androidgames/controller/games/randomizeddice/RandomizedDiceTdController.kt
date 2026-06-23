package com.xanticious.androidgames.controller.games.randomizeddice

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.GridPos
import com.xanticious.androidgames.model.games.randomizeddice.ActionCosts
import com.xanticious.androidgames.model.games.randomizeddice.DiceAction
import com.xanticious.androidgames.model.games.randomizeddice.DiceTdGameState
import com.xanticious.androidgames.model.games.towerdefense.TdEnemy
import com.xanticious.androidgames.model.games.towerdefense.TdGameState
import com.xanticious.androidgames.model.games.towerdefense.TdMap
import com.xanticious.androidgames.model.games.towerdefense.TdWave
import com.xanticious.androidgames.model.games.towerdefense.Tower
import com.xanticious.androidgames.model.games.towerdefense.TowerRole
import com.xanticious.androidgames.model.games.towerdefense.TowerStats
import kotlin.math.sqrt
import kotlin.random.Random

// ---------------------------------------------------------------------------
// Randomized Dice TD — pure controller.  No Android or Compose imports.
// ---------------------------------------------------------------------------

// ─── Constants ───────────────────────────────────────────────────────────────

private const val GRID_COLS = 12
private const val GRID_ROWS = 9
private const val INITIAL_MONEY = 200
private const val INITIAL_LIVES = 20
private const val MAX_TOWER_LEVEL = 3
private const val WAVE_COUNT = 10

// ─── CostCalculator ──────────────────────────────────────────────────────────

object CostCalculator {

    fun costsFor(difficulty: GameDifficulty): ActionCosts = when (difficulty) {
        GameDifficulty.EASY -> ActionCosts(
            buyRandom = 30,
            buySpecific = 100,
            upgradeRandom = 50,
            upgradeSpecificBase = 40,
            upgradeSpecificPerLevel = 30
        )
        GameDifficulty.MEDIUM -> ActionCosts(
            buyRandom = 50,
            buySpecific = 150,
            upgradeRandom = 75,
            upgradeSpecificBase = 60,
            upgradeSpecificPerLevel = 50
        )
        GameDifficulty.HARD -> ActionCosts(
            buyRandom = 75,
            buySpecific = 200,
            upgradeRandom = 100,
            upgradeSpecificBase = 80,
            upgradeSpecificPerLevel = 70
        )
    }

    /** cost = base + perLevel * targetLevel (where targetLevel is the tower's current level). */
    fun upgradeSpecificCost(costs: ActionCosts, towerLevel: Int): Int =
        costs.upgradeSpecificBase + costs.upgradeSpecificPerLevel * towerLevel
}

// ─── Tower stats by role ──────────────────────────────────────────────────────

private fun statsForRole(role: TowerRole, level: Int): TowerStats {
    val lvl = level.coerceAtLeast(1)
    return when (role) {
        TowerRole.SINGLE_TARGET -> TowerStats(
            range = 2.5f + lvl * 0.5f,
            damage = 10 * lvl,
            fireRate = 1.0f + lvl * 0.25f
        )
        TowerRole.AOE -> TowerStats(
            range = 2.0f + lvl * 0.4f,
            damage = 6 * lvl,
            fireRate = 0.5f + lvl * 0.15f
        )
        TowerRole.SLOW -> TowerStats(
            range = 2.5f + lvl * 0.5f,
            damage = 2 * lvl,
            fireRate = 0.8f + lvl * 0.2f,
            slowPct = 30 + lvl * 10
        )
    }
}

// ─── MapGenerator ────────────────────────────────────────────────────────────

object MapGenerator {

    /**
     * Generates a 12×9 grid with a snake path from left edge to the base tile.
     * Buildable tiles are all non-path, non-base cells.
     * The path and buildable set are deterministic for a given [seed].
     */
    fun generateMap(seed: Long, difficulty: GameDifficulty): TdMap {
        val rng = Random(seed)

        val path = buildSnakePath(rng)
        val pathSet = path.toSet()
        val basePos = path.last()

        val buildable = mutableSetOf<GridPos>()
        for (x in 0 until GRID_COLS) {
            for (y in 0 until GRID_ROWS) {
                val pos = GridPos(x, y)
                if (pos !in pathSet) buildable.add(pos)
            }
        }

        return TdMap(
            cols = GRID_COLS,
            rows = GRID_ROWS,
            path = path,
            buildable = buildable,
            basePos = basePos,
            seed = seed
        )
    }

    /**
     * Snake path: starts at (0, random row) and winds right-to-left and
     * left-to-right across all columns, ending at (GRID_COLS-1, some row).
     */
    private fun buildSnakePath(rng: Random): List<GridPos> {
        val path = mutableListOf<GridPos>()
        var row = rng.nextInt(GRID_ROWS)
        path.add(GridPos(0, row))

        for (col in 1 until GRID_COLS) {
            // Optionally shift row by ±1 before moving along the column
            val shift = rng.nextInt(3) - 1  // -1, 0, +1
            row = (row + shift).coerceIn(0, GRID_ROWS - 1)
            path.add(GridPos(col, row))
        }

        return path
    }
}

// ─── WaveGenerator ───────────────────────────────────────────────────────────

object WaveGenerator {

    fun generateWaves(seed: Long, difficulty: GameDifficulty): List<TdWave> {
        val rng = Random(seed xor 0xDEAD_BEEF)
        val diffMult = when (difficulty) {
            GameDifficulty.EASY -> 0.7f
            GameDifficulty.MEDIUM -> 1.0f
            GameDifficulty.HARD -> 1.5f
        }
        return (1..WAVE_COUNT).map { waveIndex ->
            val count = (4 + waveIndex * 2 + rng.nextInt(3))
            val enemies = (1..count).map { i ->
                val hp = ((40 + waveIndex * 12) * diffMult).toInt()
                TdEnemy(
                    id = waveIndex * 100 + i,
                    maxHp = hp,
                    hp = hp,
                    pathProgress = -i.toFloat() * 0.6f,  // staggered spawn
                    baseSpeed = (0.8f + waveIndex * 0.05f) * diffMult,
                    currentSpeed = (0.8f + waveIndex * 0.05f) * diffMult,
                    bounty = 5 + waveIndex,
                    slowRemaining = 0f
                )
            }
            TdWave(enemies = enemies, autoStartDelayMs = 5_000L)
        }
    }
}

// ─── GameInitializer ─────────────────────────────────────────────────────────

object GameInitializer {

    fun newGame(difficulty: GameDifficulty, seed: Long): DiceTdGameState {
        val costs = CostCalculator.costsFor(difficulty)
        val map = MapGenerator.generateMap(seed, difficulty)
        val waves = WaveGenerator.generateWaves(seed, difficulty)
        val base = TdGameState(
            map = map,
            towers = emptyList(),
            enemies = emptyList(),
            waves = waves,
            currentWave = 0,
            money = INITIAL_MONEY,
            lives = INITIAL_LIVES,
            nextEnemyId = 1,
            nextTowerId = 1
        )
        return DiceTdGameState(base = base, costs = costs, rngSeed = seed)
    }
}

// ─── DicePurchaser ───────────────────────────────────────────────────────────

object DicePurchaser {

    /**
     * Picks a random [TowerRole] and a random unoccupied buildable tile,
     * places a level-1 tower, deducts [ActionCosts.buyRandom].
     * Returns null if no buildable tiles are available or insufficient money.
     */
    fun buyRandom(state: DiceTdGameState): DiceTdGameState? {
        val b = state.base
        if (b.money < state.costs.buyRandom) return null

        val occupied = b.towers.map { it.tile }.toSet()
        val available = (b.map.buildable - occupied).toList()
        if (available.isEmpty()) return null

        val rng = Random(state.rngSeed)
        val tile = available[rng.nextInt(available.size)]
        val role = TowerRole.entries.toTypedArray()[rng.nextInt(TowerRole.entries.size)]
        val newSeed = rng.nextLong()

        val tower = Tower(
            id = b.nextTowerId,
            role = role,
            tile = tile,
            level = 1,
            stats = statsForRole(role, 1)
        )
        return state.copy(
            base = b.copy(
                towers = b.towers + tower,
                money = b.money - state.costs.buyRandom,
                nextTowerId = b.nextTowerId + 1
            ),
            rngSeed = newSeed,
            lastAction = DiceAction.BUY_RANDOM
        )
    }

    /**
     * Player specifies [role] and [tile].
     * Returns null if insufficient money, tile not buildable, or tile occupied.
     */
    fun buySpecific(state: DiceTdGameState, role: TowerRole, tile: GridPos): DiceTdGameState? {
        val b = state.base
        if (b.money < state.costs.buySpecific) return null
        if (tile !in b.map.buildable) return null
        if (b.towers.any { it.tile == tile }) return null

        val tower = Tower(
            id = b.nextTowerId,
            role = role,
            tile = tile,
            level = 1,
            stats = statsForRole(role, 1)
        )
        return state.copy(
            base = b.copy(
                towers = b.towers + tower,
                money = b.money - state.costs.buySpecific,
                nextTowerId = b.nextTowerId + 1
            ),
            lastAction = DiceAction.BUY_SPECIFIC
        )
    }

    /**
     * Picks a random non-maxed tower and upgrades it.
     * Returns null if no non-maxed towers exist or insufficient money.
     */
    fun upgradeRandom(state: DiceTdGameState): DiceTdGameState? {
        val b = state.base
        if (b.money < state.costs.upgradeRandom) return null

        val upgradeable = b.towers.filter { it.level < MAX_TOWER_LEVEL }
        if (upgradeable.isEmpty()) return null

        val rng = Random(state.rngSeed)
        val target = upgradeable[rng.nextInt(upgradeable.size)]
        val newSeed = rng.nextLong()

        val upgraded = target.copy(
            level = target.level + 1,
            stats = statsForRole(target.role, target.level + 1)
        )
        return state.copy(
            base = b.copy(
                towers = b.towers.map { if (it.id == target.id) upgraded else it },
                money = b.money - state.costs.upgradeRandom
            ),
            rngSeed = newSeed,
            lastAction = DiceAction.UPGRADE_RANDOM
        )
    }

    /**
     * Upgrades the tower identified by [towerId].
     * Cost = upgradeSpecificBase + upgradeSpecificPerLevel * tower.level.
     * Returns null if tower not found, already maxed, or insufficient money.
     */
    fun upgradeSpecific(state: DiceTdGameState, towerId: Int): DiceTdGameState? {
        val b = state.base
        val target = b.towers.firstOrNull { it.id == towerId } ?: return null
        if (target.level >= MAX_TOWER_LEVEL) return null

        val cost = CostCalculator.upgradeSpecificCost(state.costs, target.level)
        if (b.money < cost) return null

        val upgraded = target.copy(
            level = target.level + 1,
            stats = statsForRole(target.role, target.level + 1)
        )
        return state.copy(
            base = b.copy(
                towers = b.towers.map { if (it.id == towerId) upgraded else it },
                money = b.money - cost
            ),
            lastAction = DiceAction.UPGRADE_SPECIFIC
        )
    }
}

// ─── WaveStarter ─────────────────────────────────────────────────────────────

object WaveStarter {

    /**
     * Loads the next wave's enemies into the active enemy list.
     * Returns the same state unchanged if all waves have already started.
     */
    fun startWave(state: DiceTdGameState): DiceTdGameState {
        val b = state.base
        if (b.currentWave >= b.waves.size) return state

        val wave = b.waves[b.currentWave]
        val nextId = b.nextEnemyId
        val withIds = wave.enemies.mapIndexed { i, e -> e.copy(id = nextId + i) }

        return state.copy(
            base = b.copy(
                enemies = b.enemies + withIds,
                currentWave = b.currentWave + 1,
                nextEnemyId = nextId + withIds.size
            )
        )
    }
}

// ─── CombatResolver ──────────────────────────────────────────────────────────

object CombatResolver {

    /**
     * Advances the combat simulation by [dt] seconds:
     * 1. Tick slow timers down on each enemy.
     * 2. Move enemies along the path.
     * 3. Enemies that reach the base lose a life and are removed.
     * 4. Each tower fires at enemies in range (respecting role).
     * 5. Dead enemies award their bounty and are removed.
     *
     * Returns the updated [DiceTdGameState].
     */
    fun resolveTick(state: DiceTdGameState, dt: Float): DiceTdGameState {
        val b = state.base
        val path = b.map.path
        if (path.isEmpty()) return state

        // 1 & 2: advance slow timers and move enemies
        val movedEnemies = b.enemies.map { enemy ->
            val slowLeft = (enemy.slowRemaining - dt).coerceAtLeast(0f)
            val speed = if (slowLeft > 0f) enemy.currentSpeed else enemy.baseSpeed
            enemy.copy(
                pathProgress = enemy.pathProgress + speed * dt,
                slowRemaining = slowLeft,
                currentSpeed = if (slowLeft > 0f) enemy.currentSpeed else enemy.baseSpeed
            )
        }

        // 3: enemies that reached base — those with pathProgress >= path.size
        val maxProgress = path.size.toFloat()
        val reachedBase = movedEnemies.filter { it.pathProgress >= maxProgress }
        val livesLost = reachedBase.size
        var activeEnemies = movedEnemies.filter { it.pathProgress < maxProgress }

        // 4: towers fire
        var moneyGained = 0
        // Track accumulated damage per enemy id
        val damageMap = mutableMapOf<Int, Int>()

        for (tower in b.towers) {
            val towerCenter = tileCenter(tower.tile)
            val range = tower.stats.range

            when (tower.role) {
                TowerRole.SINGLE_TARGET -> {
                    // Highest progress enemy in range
                    val target = activeEnemies
                        .filter { distanceTo(towerCenter, it, path) <= range }
                        .maxByOrNull { it.pathProgress }
                    if (target != null) {
                        val shots = (tower.stats.fireRate * dt).coerceAtLeast(0f)
                        val dmg = (tower.stats.damage * shots).toInt().coerceAtLeast(if (shots > 0f) 1 else 0)
                        damageMap[target.id] = (damageMap[target.id] ?: 0) + dmg
                    }
                }
                TowerRole.AOE -> {
                    val inRange = activeEnemies.filter { distanceTo(towerCenter, it, path) <= range }
                    if (inRange.isNotEmpty()) {
                        val shots = (tower.stats.fireRate * dt).coerceAtLeast(0f)
                        val dmg = (tower.stats.damage * shots).toInt().coerceAtLeast(if (shots > 0f) 1 else 0)
                        for (enemy in inRange) {
                            damageMap[enemy.id] = (damageMap[enemy.id] ?: 0) + dmg
                        }
                    }
                }
                TowerRole.SLOW -> {
                    val inRange = activeEnemies.filter { distanceTo(towerCenter, it, path) <= range }
                    if (inRange.isNotEmpty()) {
                        val shots = (tower.stats.fireRate * dt).coerceAtLeast(0f)
                        val dmg = (tower.stats.damage * shots).toInt().coerceAtLeast(if (shots > 0f) 1 else 0)
                        val slowFraction = tower.stats.slowPct / 100f
                        activeEnemies = activeEnemies.map { enemy ->
                            if (enemy in inRange) {
                                val newSpeed = enemy.baseSpeed * (1f - slowFraction)
                                enemy.copy(
                                    currentSpeed = newSpeed,
                                    slowRemaining = 2f,  // 2 s slow window
                                    hp = (enemy.hp - dmg).coerceAtLeast(0)
                                ).also { if (it.hp <= 0) damageMap[enemy.id] = enemy.hp }
                            } else enemy
                        }
                    }
                }
            }
        }

        // Apply damage from non-slow towers
        activeEnemies = activeEnemies.map { enemy ->
            val dmg = damageMap[enemy.id] ?: 0
            if (dmg > 0) enemy.copy(hp = (enemy.hp - dmg).coerceAtLeast(0)) else enemy
        }

        // 5: collect bounties and remove dead enemies
        val dead = activeEnemies.filter { it.hp <= 0 }
        moneyGained = dead.sumOf { it.bounty }
        activeEnemies = activeEnemies.filter { it.hp > 0 }

        return state.copy(
            base = b.copy(
                enemies = activeEnemies,
                lives = (b.lives - livesLost).coerceAtLeast(0),
                money = b.money + moneyGained
            )
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** World-space center of a grid tile (tile (x,y) → float (x+0.5, y+0.5)). */
    private fun tileCenter(pos: GridPos): Pair<Float, Float> =
        Pair(pos.x + 0.5f, pos.y + 0.5f)

    /** Euclidean distance from a tower center to the interpolated enemy position on the path. */
    private fun distanceTo(
        towerCenter: Pair<Float, Float>,
        enemy: TdEnemy,
        path: List<GridPos>
    ): Float {
        val progress = enemy.pathProgress.coerceIn(0f, (path.size - 1).toFloat())
        val idx = progress.toInt().coerceIn(0, path.size - 1)
        val tile = path[idx]
        val ex = tile.x + 0.5f
        val ey = tile.y + 0.5f
        val dx = towerCenter.first - ex
        val dy = towerCenter.second - ey
        return sqrt(dx * dx + dy * dy)
    }
}
