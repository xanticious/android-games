package com.xanticious.androidgames.games.randomizeddice

import com.xanticious.androidgames.controller.games.randomizeddice.CostCalculator
import com.xanticious.androidgames.controller.games.randomizeddice.DicePurchaser
import com.xanticious.androidgames.controller.games.randomizeddice.GameInitializer
import com.xanticious.androidgames.controller.games.randomizeddice.MapGenerator
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.GridPos
import com.xanticious.androidgames.model.games.towerdefense.TowerRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RandomizedDiceTdControllerTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun freshState(
        difficulty: GameDifficulty = GameDifficulty.MEDIUM,
        seed: Long = 42L
    ) = GameInitializer.newGame(difficulty, seed)

    private fun stateWithMoney(money: Int, seed: Long = 42L) =
        freshState(seed = seed).let { s ->
            s.copy(base = s.base.copy(money = money))
        }

    // ── buyRandom ────────────────────────────────────────────────────────────

    @Test
    fun buyRandom_validState_placesTowerOnBuildableTile() {
        val state = freshState()
        val result = DicePurchaser.buyRandom(state)
        assertNotNull(result)
        val newTower = result!!.base.towers.first()
        assertTrue(state.base.map.buildable.contains(newTower.tile))
    }

    @Test
    fun buyRandom_noBuildableTiles_returnsNull() {
        val state = freshState()
        // Occupy every buildable tile with towers by filling them via buySpecific
        val buildable = state.base.map.buildable.toList()
        var s = state
        for (tile in buildable) {
            val role = TowerRole.SINGLE_TARGET
            val bought = DicePurchaser.buySpecific(s, role, tile)
                ?: s.copy(base = s.base.copy(
                    towers = s.base.towers + com.xanticious.androidgames.model.games.towerdefense.Tower(
                        id = s.base.nextTowerId,
                        role = role,
                        tile = tile,
                        level = 1,
                        stats = com.xanticious.androidgames.model.games.towerdefense.TowerStats(2.5f, 10, 1.0f)
                    ),
                    nextTowerId = s.base.nextTowerId + 1,
                    money = s.base.money  // bypass cost by injecting directly
                ))
            s = bought
        }
        // Give plenty of money so money isn't the blocker
        s = s.copy(base = s.base.copy(money = 100_000))
        val result = DicePurchaser.buyRandom(s)
        assertNull(result)
    }

    @Test
    fun buyRandom_insufficientMoney_returnsNull() {
        val state = stateWithMoney(0)
        val result = DicePurchaser.buyRandom(state)
        assertNull(result)
    }

    // ── upgradeRandom ────────────────────────────────────────────────────────

    @Test
    fun upgradeRandom_allTowersMaxed_returnsNull() {
        val state = freshState()
        // Place one tower and manually set it to max level
        val b = state.base
        val tile = b.map.buildable.first()
        val maxTower = com.xanticious.androidgames.model.games.towerdefense.Tower(
            id = 1,
            role = TowerRole.SINGLE_TARGET,
            tile = tile,
            level = 3,
            stats = com.xanticious.androidgames.model.games.towerdefense.TowerStats(4f, 30, 1.75f)
        )
        val s = state.copy(base = b.copy(towers = listOf(maxTower)))
        val result = DicePurchaser.upgradeRandom(s)
        assertNull(result)
    }

    @Test
    fun upgradeRandom_randomlyPicksNonMaxedTower() {
        val state = freshState()
        val b = state.base
        val tiles = b.map.buildable.take(2).toList()

        val tower1 = com.xanticious.androidgames.model.games.towerdefense.Tower(
            id = 1, role = TowerRole.SINGLE_TARGET, tile = tiles[0], level = 1,
            stats = com.xanticious.androidgames.model.games.towerdefense.TowerStats(3f, 10, 1.0f)
        )
        val tower2 = com.xanticious.androidgames.model.games.towerdefense.Tower(
            id = 2, role = TowerRole.AOE, tile = tiles[1], level = 3,
            stats = com.xanticious.androidgames.model.games.towerdefense.TowerStats(2.8f, 18, 0.65f)
        )
        val s = state.copy(
            base = b.copy(towers = listOf(tower1, tower2), money = 100_000)
        )
        val result = DicePurchaser.upgradeRandom(s)
        assertNotNull(result)
        // Only tower1 is upgradeable, so it must be the one upgraded
        val upgraded = result!!.base.towers.first { it.id == 1 }
        assertEquals(2, upgraded.level)
    }

    // ── upgradeSpecificCost ───────────────────────────────────────────────────

    @Test
    fun upgradeSpecificCost_higherLevelTower_higherCost() {
        val costs = CostCalculator.costsFor(GameDifficulty.MEDIUM)
        val costLevel1 = CostCalculator.upgradeSpecificCost(costs, 1)
        val costLevel2 = CostCalculator.upgradeSpecificCost(costs, 2)
        assertTrue(costLevel2 > costLevel1)
    }

    // ── Cost ordering invariants ──────────────────────────────────────────────

    @Test
    fun costOrder_buyRandomCheaperThanUpgradeRandom() {
        for (difficulty in GameDifficulty.entries) {
            val costs = CostCalculator.costsFor(difficulty)
            assertTrue(
                "buyRandom < upgradeRandom for $difficulty",
                costs.buyRandom < costs.upgradeRandom
            )
        }
    }

    @Test
    fun costOrder_upgradeRandomCheaperThanBuySpecific() {
        for (difficulty in GameDifficulty.entries) {
            val costs = CostCalculator.costsFor(difficulty)
            assertTrue(
                "upgradeRandom < buySpecific for $difficulty",
                costs.upgradeRandom < costs.buySpecific
            )
        }
    }

    // ── Determinism ───────────────────────────────────────────────────────────

    @Test
    fun sameSeed_sameRandomResults() {
        val stateA = freshState(seed = 99L)
        val stateB = freshState(seed = 99L)
        val resultA = DicePurchaser.buyRandom(stateA)
        val resultB = DicePurchaser.buyRandom(stateB)
        assertNotNull(resultA)
        assertNotNull(resultB)
        assertEquals(resultA!!.base.towers.first().tile, resultB!!.base.towers.first().tile)
        assertEquals(resultA.base.towers.first().role, resultB.base.towers.first().role)
    }

    // ── buySpecific ───────────────────────────────────────────────────────────

    @Test
    fun buySpecific_insufficientMoney_returnsNull() {
        val state = stateWithMoney(0)
        val tile = state.base.map.buildable.first()
        val result = DicePurchaser.buySpecific(state, TowerRole.SINGLE_TARGET, tile)
        assertNull(result)
    }

    @Test
    fun buySpecific_onOccupiedTile_returnsNull() {
        val state = freshState()
        val tile = state.base.map.buildable.first()
        // Place a tower there first
        val after = DicePurchaser.buySpecific(state, TowerRole.SINGLE_TARGET, tile)
        assertNotNull(after)
        // Try again on the same tile with plenty of money
        val again = DicePurchaser.buySpecific(
            after!!.copy(base = after.base.copy(money = 100_000)),
            TowerRole.AOE,
            tile
        )
        assertNull(again)
    }

    @Test
    fun buySpecific_onPathTile_returnsNull() {
        val state = freshState()
        val pathTile = state.base.map.path.first()
        val result = DicePurchaser.buySpecific(
            state.copy(base = state.base.copy(money = 100_000)),
            TowerRole.SINGLE_TARGET,
            pathTile
        )
        assertNull(result)
    }
}
