package com.xanticious.androidgames.controller.games.basedefense

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.GridPos
import com.xanticious.androidgames.model.games.basedefense.BaseTower
import com.xanticious.androidgames.model.games.towerdefense.TdEnemy
import com.xanticious.androidgames.model.games.towerdefense.TdGameState
import com.xanticious.androidgames.model.games.towerdefense.TdMap
import com.xanticious.androidgames.model.games.towerdefense.TdWave
import com.xanticious.androidgames.model.games.towerdefense.Tower
import com.xanticious.androidgames.model.games.towerdefense.TowerRole
import com.xanticious.androidgames.model.games.towerdefense.TowerStats
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Pure Base Defense rules engine — no Android or Compose imports.
 *
 * All public functions are stateless: they receive the current [TdGameState]
 * and return a new value, making the entire domain layer trivially testable
 * on a plain JVM without any emulator.
 */
object BaseDefenseController {

    // ── Grid dimensions ───────────────────────────────────────────────────────

    const val COLS = 12
    const val ROWS = 9

    // ── Difficulty parameters ─────────────────────────────────────────────────

    private data class DifficultyParams(
        val startingMoney: Int,
        val startingLives: Int,
        val enemyHpMultiplier: Float,
        val enemySpeedMultiplier: Float,
        val waveBountyMultiplier: Float
    )

    private fun difficultyParams(difficulty: GameDifficulty): DifficultyParams = when (difficulty) {
        GameDifficulty.EASY -> DifficultyParams(
            startingMoney = 350,
            startingLives = 25,
            enemyHpMultiplier = 0.75f,
            enemySpeedMultiplier = 0.85f,
            waveBountyMultiplier = 1.25f
        )
        GameDifficulty.MEDIUM -> DifficultyParams(
            startingMoney = 250,
            startingLives = 15,
            enemyHpMultiplier = 1.0f,
            enemySpeedMultiplier = 1.0f,
            waveBountyMultiplier = 1.0f
        )
        GameDifficulty.HARD -> DifficultyParams(
            startingMoney = 175,
            startingLives = 10,
            enemyHpMultiplier = 1.35f,
            enemySpeedMultiplier = 1.2f,
            waveBountyMultiplier = 0.85f
        )
    }

    // ── Tower catalogue ───────────────────────────────────────────────────────

    private val GUN_STATS = listOf(
        TowerStats(range = 2.5f, damage = 12, fireRate = 2.0f),
        TowerStats(range = 2.8f, damage = 20, fireRate = 2.4f),
        TowerStats(range = 3.2f, damage = 32, fireRate = 2.8f)
    )

    private val MORTAR_STATS = listOf(
        TowerStats(range = 3.5f, damage = 18, fireRate = 0.8f),
        TowerStats(range = 3.8f, damage = 28, fireRate = 1.0f),
        TowerStats(range = 4.2f, damage = 42, fireRate = 1.2f)
    )

    // FROST: no damage, applies slow via slowPct in TowerStats
    private val FROST_STATS = listOf(
        TowerStats(range = 2.5f, damage = 0, fireRate = 1.5f, slowPct = 50),
        TowerStats(range = 2.8f, damage = 0, fireRate = 1.8f, slowPct = 60),
        TowerStats(range = 3.2f, damage = 0, fireRate = 2.0f, slowPct = 70)
    )

    private val BASE_COSTS = mapOf(
        BaseTower.GUN to 80,
        BaseTower.MORTAR to 120,
        BaseTower.FROST to 100
    )

    fun towerCost(baseTower: BaseTower): Int = BASE_COSTS.getValue(baseTower)

    /** Upgrade cost rises by 20 % of the base price per level purchased. */
    fun upgradeCost(baseTower: BaseTower, currentLevel: Int): Int {
        val base = BASE_COSTS.getValue(baseTower)
        return (base * (currentLevel + 1) * 1.2f).roundToInt()
    }

    fun upgradedStats(baseTower: BaseTower, level: Int): TowerStats {
        val clamped = level.coerceIn(0, 2)
        return when (baseTower) {
            BaseTower.GUN -> GUN_STATS[clamped]
            BaseTower.MORTAR -> MORTAR_STATS[clamped]
            BaseTower.FROST -> FROST_STATS[clamped]
        }
    }

    fun roleFor(baseTower: BaseTower): TowerRole = when (baseTower) {
        BaseTower.GUN -> TowerRole.SINGLE_TARGET
        BaseTower.MORTAR -> TowerRole.AOE
        BaseTower.FROST -> TowerRole.SLOW
    }

    // ── Economy ───────────────────────────────────────────────────────────────

    /**
     * Bonus money for calling the next wave before the auto-start timer fires.
     * Scales at 5 gold per second of time skipped.
     */
    fun earlyCallBonus(timeSkippedMs: Long): Int =
        ((timeSkippedMs / 1000.0) * 5).roundToInt().coerceAtLeast(0)

    // ── Map generation ────────────────────────────────────────────────────────

    /**
     * Generates a [TdMap] via a seeded random walk from the left edge to the
     * right edge.  Tiles adjacent to the path (but not on it) are marked
     * buildable.  The same [seed] always produces the same map.
     */
    fun generateMap(seed: Long, difficulty: GameDifficulty): TdMap {
        val rng = Random(seed)
        val startRow = rng.nextInt(1, ROWS - 1)
        val endRow = rng.nextInt(1, ROWS - 1)
        val path = buildPath(rng, startRow, endRow)
        val pathSet = path.toHashSet()
        val buildable = buildBuildableSet(path, pathSet)
        return TdMap(
            cols = COLS,
            rows = ROWS,
            path = path,
            buildable = buildable,
            basePos = path.last(),
            seed = seed
        )
    }

    private fun buildPath(rng: Random, startRow: Int, endRow: Int): List<GridPos> {
        val visited = mutableSetOf<GridPos>()
        val path = mutableListOf(GridPos(0, startRow))
        visited.add(GridPos(0, startRow))
        var current = path.first()

        while (current.x < COLS - 1) {
            val dy = endRow - current.y
            val candidates = mutableListOf<GridPos>()

            val right = GridPos(current.x + 1, current.y)
            if (right.y in 0 until ROWS && right !in visited) {
                // Double-weight right to bias horizontal progress
                candidates.add(right)
                candidates.add(right)
            }
            if (dy > 0) {
                val down = GridPos(current.x, current.y + 1)
                if (down.y in 0 until ROWS && down !in visited) candidates.add(down)
            }
            if (dy < 0) {
                val up = GridPos(current.x, current.y - 1)
                if (up.y in 0 until ROWS && up !in visited) candidates.add(up)
            }

            val next = if (candidates.isNotEmpty()) {
                candidates[rng.nextInt(candidates.size)]
            } else {
                // Force right to break a dead end
                GridPos(current.x + 1, current.y.coerceIn(0, ROWS - 1))
            }

            path.add(next)
            visited.add(next)
            current = next
        }

        return path
    }

    private fun buildBuildableSet(path: List<GridPos>, pathSet: Set<GridPos>): Set<GridPos> {
        val buildable = mutableSetOf<GridPos>()
        for (tile in path) {
            for (nb in tile.neighbours()) {
                if (nb.x in 0 until COLS && nb.y in 0 until ROWS && nb !in pathSet) {
                    buildable.add(nb)
                }
            }
        }
        return buildable
    }

    // ── Wave generation ───────────────────────────────────────────────────────

    private const val WAVE_COUNT = 10
    const val AUTO_START_DELAY_MS = 15_000L

    /**
     * Produces [WAVE_COUNT] waves with scaling difficulty.  Enemy HP, speed,
     * and count all increase each wave.
     */
    fun generateWaves(difficulty: GameDifficulty, mapSeed: Long): List<TdWave> {
        val params = difficultyParams(difficulty)
        return List(WAVE_COUNT) { waveIndex ->
            val count = 4 + waveIndex * 2
            val baseHp = ((30 + waveIndex * 15) * params.enemyHpMultiplier).roundToInt()
            val baseSpeed = (1.2f + waveIndex * 0.08f) * params.enemySpeedMultiplier
            val bounty = ((8 + waveIndex) * params.waveBountyMultiplier).roundToInt()
            val enemies = List(count) { enemyIndex ->
                TdEnemy(
                    id = waveIndex * 100 + enemyIndex,
                    maxHp = baseHp,
                    hp = baseHp,
                    pathProgress = -(enemyIndex * 0.6f), // stagger behind spawn
                    baseSpeed = baseSpeed,
                    currentSpeed = baseSpeed,
                    bounty = bounty
                )
            }
            TdWave(enemies = enemies, autoStartDelayMs = AUTO_START_DELAY_MS)
        }
    }

    // ── Initial game state ────────────────────────────────────────────────────

    fun initialState(difficulty: GameDifficulty, seed: Long): TdGameState {
        val params = difficultyParams(difficulty)
        val map = generateMap(seed, difficulty)
        val waves = generateWaves(difficulty, seed)
        return TdGameState(
            map = map,
            towers = emptyList(),
            enemies = emptyList(),
            waves = waves,
            currentWave = 0,
            money = params.startingMoney,
            lives = params.startingLives,
            nextEnemyId = waves.sumOf { it.enemies.size },
            nextTowerId = 0
        )
    }

    // ── Tower placement & upgrades ────────────────────────────────────────────

    /**
     * Places a new tower of [baseTower] type at [tile].
     * Returns `null` if the tile is not buildable, is already occupied, or the
     * player cannot afford it.
     */
    fun placeTower(state: TdGameState, tile: GridPos, baseTower: BaseTower): TdGameState? {
        if (tile !in state.map.buildable) return null
        if (state.towers.any { it.tile == tile }) return null
        val cost = towerCost(baseTower)
        if (state.money < cost) return null

        val tower = Tower(
            id = state.nextTowerId,
            role = roleFor(baseTower),
            tile = tile,
            level = 0,
            stats = upgradedStats(baseTower, 0)
        )
        return state.copy(
            towers = state.towers + tower,
            money = state.money - cost,
            nextTowerId = state.nextTowerId + 1
        )
    }

    /**
     * Upgrades the tower with [towerId] to the next level.
     * Returns `null` if tower is at max level, does not exist, or player
     * cannot afford the upgrade.
     */
    fun upgradeTower(state: TdGameState, towerId: Int): TdGameState? {
        val tower = state.towers.firstOrNull { it.id == towerId } ?: return null
        if (tower.level >= 2) return null
        val baseTower = baseTowerFor(tower.role) ?: return null
        val cost = upgradeCost(baseTower, tower.level)
        if (state.money < cost) return null

        val upgraded = tower.copy(
            level = tower.level + 1,
            stats = upgradedStats(baseTower, tower.level + 1)
        )
        return state.copy(
            towers = state.towers.map { if (it.id == towerId) upgraded else it },
            money = state.money - cost
        )
    }

    /** Sells a tower, refunding half the total amount invested in it. */
    fun sellTower(state: TdGameState, towerId: Int): TdGameState? {
        val tower = state.towers.firstOrNull { it.id == towerId } ?: return null
        val baseTower = baseTowerFor(tower.role) ?: return null
        var invested = towerCost(baseTower)
        for (lvl in 0 until tower.level) invested += upgradeCost(baseTower, lvl)
        val refund = invested / 2
        return state.copy(
            towers = state.towers.filter { it.id != towerId },
            money = state.money + refund
        )
    }

    // ── Wave management ───────────────────────────────────────────────────────

    /**
     * Moves the next wave's enemies onto the board and advances [currentWave].
     * Returns the state unchanged when there are no remaining waves.
     */
    fun startWave(state: TdGameState): TdGameState {
        if (state.currentWave >= state.waves.size) return state
        val wave = state.waves[state.currentWave]
        return state.copy(
            enemies = state.enemies + wave.enemies,
            currentWave = state.currentWave + 1
        )
    }

    // ── Combat simulation ─────────────────────────────────────────────────────

    /**
     * Advances the combat simulation by [dt] seconds.
     *
     * Processing order:
     * 1. Decay slow timers; restore enemy speed when timers expire.
     * 2. Move every enemy forward along the path.
     * 3. Remove enemies that reached the base; decrement [lives].
     * 4. Each tower fires at enemies within range using role-specific logic:
     *    - SINGLE_TARGET: hits the furthest-progressed enemy in range.
     *    - AOE: damages every enemy within the tower's range.
     *    - SLOW: applies the tower's [TowerStats.slowPct] to all enemies in range.
     * 5. Remove dead enemies; award [bounty] money for each kill.
     */
    fun resolveTick(state: TdGameState, dt: Float): TdGameState {
        // Step 1 & 2: decay slow timers and advance positions
        var enemies = state.enemies.map { enemy ->
            val slowRemaining = (enemy.slowRemaining - dt).coerceAtLeast(0f)
            val speed = if (slowRemaining > 0f) enemy.currentSpeed else enemy.baseSpeed
            enemy.copy(
                pathProgress = enemy.pathProgress + speed * dt,
                currentSpeed = speed,
                slowRemaining = slowRemaining
            )
        }

        // Step 3: remove enemies that reached the end of the path
        val pathLength = state.map.path.size.toFloat()
        val (leaked, inPlay) = enemies.partition { it.pathProgress >= pathLength }
        val livesLost = leaked.size
        enemies = inPlay

        // Step 4: towers fire
        // slowApply: enemy id → (slowFraction, slowDuration)
        val hpDelta = mutableMapOf<Int, Int>()
        val slowApply = mutableMapOf<Int, Pair<Float, Float>>()

        for (tower in state.towers) {
            val (tx, ty) = tileCenter(tower.tile)
            val inRange = enemies.filter { enemy ->
                val (ex, ey) = enemyWorldPos(enemy, state.map.path)
                hypot(ex - tx, ey - ty) <= tower.stats.range
            }
            if (inRange.isEmpty()) continue

            when (tower.role) {
                TowerRole.SINGLE_TARGET -> {
                    val target = inRange.maxByOrNull { it.pathProgress } ?: continue
                    hpDelta[target.id] = (hpDelta[target.id] ?: 0) + tower.stats.damage
                }
                TowerRole.AOE -> {
                    for (target in inRange) {
                        hpDelta[target.id] = (hpDelta[target.id] ?: 0) + tower.stats.damage
                    }
                }
                TowerRole.SLOW -> {
                    val slowFraction = tower.stats.slowPct / 100f
                    val slowDuration = 1f / tower.stats.fireRate.coerceAtLeast(0.01f)
                    for (target in inRange) {
                        val existing = slowApply[target.id]
                        // Keep the strongest slow if multiple FROST towers overlap
                        if (existing == null || slowFraction > existing.first) {
                            slowApply[target.id] = slowFraction to slowDuration
                        }
                    }
                }
            }
        }

        // Apply damage and slow effects
        val afterCombat = enemies.map { enemy ->
            val damage = hpDelta[enemy.id] ?: 0
            val slow = slowApply[enemy.id]
            var updated = enemy.copy(hp = enemy.hp - damage)
            if (slow != null) {
                val (fraction, duration) = slow
                if (duration > updated.slowRemaining) {
                    updated = updated.copy(
                        slowRemaining = duration,
                        currentSpeed = updated.baseSpeed * (1f - fraction)
                    )
                }
            }
            updated
        }

        // Step 5: remove dead enemies and award bounties
        val (dead, alive) = afterCombat.partition { it.hp <= 0 }
        val earned = dead.sumOf { it.bounty }

        return state.copy(
            enemies = alive,
            money = state.money + earned,
            lives = (state.lives - livesLost).coerceAtLeast(0)
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Interpolates an enemy's 2-D world position along the path.
     * Each path tile occupies 1×1 world units; tile centres are at (x+0.5, y+0.5).
     */
    fun enemyWorldPos(enemy: TdEnemy, path: List<GridPos>): Pair<Float, Float> {
        val progress = enemy.pathProgress.coerceAtLeast(0f)
        val index = progress.toInt().coerceIn(0, path.size - 1)
        val frac = progress - index.toFloat()
        val current = path[index]
        val next = path.getOrElse(index + 1) { current }
        val x = current.x + (next.x - current.x) * frac + 0.5f
        val y = current.y + (next.y - current.y) * frac + 0.5f
        return x to y
    }

    /** Returns the world-space centre of a grid tile. */
    fun tileCenter(tile: GridPos): Pair<Float, Float> =
        (tile.x + 0.5f) to (tile.y + 0.5f)

    private fun baseTowerFor(role: TowerRole): BaseTower? = when (role) {
        TowerRole.SINGLE_TARGET -> BaseTower.GUN
        TowerRole.AOE -> BaseTower.MORTAR
        TowerRole.SLOW -> BaseTower.FROST
    }
}
