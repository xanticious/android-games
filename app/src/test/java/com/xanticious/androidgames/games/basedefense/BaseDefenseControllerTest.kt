package com.xanticious.androidgames.games.basedefense

import com.xanticious.androidgames.controller.games.basedefense.BaseDefenseController
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseDefenseControllerTest {

    // ── Shared helpers ────────────────────────────────────────────────────────

    private fun simplePath(): List<GridPos> =
        (0 until BaseDefenseController.COLS).map { x -> GridPos(x, 4) }

    private fun simpleMap(path: List<GridPos> = simplePath()): TdMap {
        val pathSet = path.toHashSet()
        val buildable = path.flatMap { it.neighbours() }
            .filter { nb -> nb.x in 0 until BaseDefenseController.COLS && nb.y in 0 until BaseDefenseController.ROWS && nb !in pathSet }
            .toSet()
        return TdMap(
            cols = BaseDefenseController.COLS,
            rows = BaseDefenseController.ROWS,
            path = path,
            buildable = buildable,
            basePos = path.last(),
            seed = 42L
        )
    }

    private fun emptyState(
        map: TdMap = simpleMap(),
        money: Int = 500,
        lives: Int = 15
    ): TdGameState = TdGameState(
        map = map,
        towers = emptyList(),
        enemies = emptyList(),
        waves = emptyList(),
        currentWave = 0,
        money = money,
        lives = lives,
        nextEnemyId = 0,
        nextTowerId = 0
    )

    private fun makeEnemy(
        id: Int = 0,
        hp: Int = 100,
        pathProgress: Float = 0f,
        speed: Float = 1.5f,
        bounty: Int = 10,
        slowRemaining: Float = 0f
    ) = TdEnemy(
        id = id,
        maxHp = hp,
        hp = hp,
        pathProgress = pathProgress,
        baseSpeed = speed,
        currentSpeed = speed,
        bounty = bounty,
        slowRemaining = slowRemaining
    )

    // ── earlyCallBonus ────────────────────────────────────────────────────────

    @Test
    fun earlyCallBonus_moreTimeSkipped_higherBonus() {
        val bonusLow = BaseDefenseController.earlyCallBonus(2_000L)
        val bonusHigh = BaseDefenseController.earlyCallBonus(8_000L)
        assertTrue(bonusHigh > bonusLow)
    }

    @Test
    fun earlyCallBonus_zeroTimeSkipped_returnsZero() {
        val bonus = BaseDefenseController.earlyCallBonus(0L)
        assertEquals(0, bonus)
    }

    // ── upgradeCost ───────────────────────────────────────────────────────────

    @Test
    fun upgradeCost_levelTwo_higherThanLevelOne() {
        val costLvl1 = BaseDefenseController.upgradeCost(BaseTower.GUN, 0)
        val costLvl2 = BaseDefenseController.upgradeCost(BaseTower.GUN, 1)
        assertTrue(costLvl2 > costLvl1)
    }

    @Test
    fun upgradeCost_risesPerLevel() {
        val cost0 = BaseDefenseController.upgradeCost(BaseTower.MORTAR, 0)
        val cost1 = BaseDefenseController.upgradeCost(BaseTower.MORTAR, 1)
        val cost2 = BaseDefenseController.upgradeCost(BaseTower.MORTAR, 2)
        assertTrue(cost1 > cost0 && cost2 > cost1)
    }

    // ── placeTower ────────────────────────────────────────────────────────────

    @Test
    fun placeTower_onBuildableTile_addsTower() {
        val map = simpleMap()
        val buildableTile = map.buildable.first()
        val state = emptyState(map = map)
        val result = BaseDefenseController.placeTower(state, buildableTile, BaseTower.GUN)
        assertEquals(1, result?.towers?.size)
    }

    @Test
    fun placeTower_onPathTile_returnsNull() {
        val map = simpleMap()
        val pathTile = map.path.first()
        val state = emptyState(map = map)
        val result = BaseDefenseController.placeTower(state, pathTile, BaseTower.GUN)
        assertNull(result)
    }

    @Test
    fun placeTower_occupiedTile_returnsNull() {
        val map = simpleMap()
        val buildableTile = map.buildable.first()
        val state = emptyState(map = map)
        val after = BaseDefenseController.placeTower(state, buildableTile, BaseTower.GUN)
        val result = after?.let { BaseDefenseController.placeTower(it, buildableTile, BaseTower.GUN) }
        assertNull(result)
    }

    // ── upgradeTower ──────────────────────────────────────────────────────────

    @Test
    fun upgradeTower_insufficientMoney_returnsNull() {
        val map = simpleMap()
        val buildableTile = map.buildable.first()
        // Place tower then drain money
        val state = emptyState(map = map, money = BaseDefenseController.towerCost(BaseTower.GUN))
        val withTower = BaseDefenseController.placeTower(state, buildableTile, BaseTower.GUN)
        requireNotNull(withTower)
        val broke = withTower.copy(money = 0)
        val result = BaseDefenseController.upgradeTower(broke, withTower.towers.first().id)
        assertNull(result)
    }

    // ── resolveTick ───────────────────────────────────────────────────────────

    @Test
    fun resolveTick_enemyReachesBase_decrementsLives() {
        val path = simplePath()
        val map = simpleMap(path)
        // Place enemy at nearly the end of the path so it leaks after one tick
        val enemy = makeEnemy(pathProgress = path.size - 0.1f, speed = 2f)
        val state = emptyState(map = map, lives = 15).copy(enemies = listOf(enemy))
        val result = BaseDefenseController.resolveTick(state, 0.1f)
        assertTrue(result.lives < 15)
    }

    @Test
    fun resolveTick_enemyKilled_awardsMoney() {
        val path = simplePath()
        val map = simpleMap(path)
        val buildableTile = GridPos(0, 3) // row above the path row 4
        val tower = Tower(
            id = 0,
            role = TowerRole.SINGLE_TARGET,
            tile = buildableTile,
            level = 0,
            stats = TowerStats(range = 2.0f, damage = 9999, fireRate = 60f)
        )
        val enemy = makeEnemy(id = 1, hp = 50, pathProgress = 0.5f, bounty = 15)
        val state = emptyState(map = map, money = 100).copy(
            towers = listOf(tower),
            enemies = listOf(enemy)
        )
        val result = BaseDefenseController.resolveTick(state, 0.016f)
        assertTrue(result.money > 100)
    }

    @Test
    fun resolveTick_frostTower_slowsEnemy() {
        val path = simplePath()
        val map = simpleMap(path)
        val buildableTile = GridPos(0, 3)
        val frostTower = Tower(
            id = 0,
            role = TowerRole.SLOW,
            tile = buildableTile,
            level = 0,
            stats = TowerStats(range = 3.0f, damage = 0, fireRate = 2f, slowPct = 50)
        )
        val enemy = makeEnemy(id = 1, hp = 200, pathProgress = 0.5f, speed = 2f)
        val state = emptyState(map = map).copy(
            towers = listOf(frostTower),
            enemies = listOf(enemy)
        )
        val result = BaseDefenseController.resolveTick(state, 0.016f)
        val slowed = result.enemies.firstOrNull { it.id == 1 }
        assertTrue((slowed?.slowRemaining ?: 0f) > 0f)
    }

    @Test
    fun resolveTick_aoemortar_damagesAllInRadius() {
        val path = simplePath()
        val map = simpleMap(path)
        val buildableTile = GridPos(3, 3) // adjacent above path
        val mortarTower = Tower(
            id = 0,
            role = TowerRole.AOE,
            tile = buildableTile,
            level = 0,
            stats = TowerStats(range = 3.5f, damage = 50, fireRate = 1f)
        )
        // Two enemies near the tower
        val enemy1 = makeEnemy(id = 1, hp = 100, pathProgress = 2.5f)
        val enemy2 = makeEnemy(id = 2, hp = 100, pathProgress = 3.5f)
        val state = emptyState(map = map).copy(
            towers = listOf(mortarTower),
            enemies = listOf(enemy1, enemy2)
        )
        val result = BaseDefenseController.resolveTick(state, 0.016f)
        val bothDamaged = result.enemies.all { it.hp < 100 } ||
            (result.enemies.size < 2) // some may have died
        assertTrue(bothDamaged)
    }

    // ── generateMap ───────────────────────────────────────────────────────────

    @Test
    fun generateMap_sameSeed_sameMap() {
        val map1 = BaseDefenseController.generateMap(12345L, GameDifficulty.MEDIUM)
        val map2 = BaseDefenseController.generateMap(12345L, GameDifficulty.MEDIUM)
        assertEquals(map1.path, map2.path)
    }

    @Test
    fun generateMap_pathConnected_spawnToBase() {
        val map = BaseDefenseController.generateMap(99L, GameDifficulty.MEDIUM)
        // Each consecutive path tile must be adjacent (Manhattan distance == 1)
        val allAdjacent = map.path.zipWithNext().all { (a, b) -> a.manhattanDistanceTo(b) == 1 }
        assertTrue(allAdjacent)
    }
}
