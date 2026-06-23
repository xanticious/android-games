package com.xanticious.androidgames.games.ageofempirelite

import com.xanticious.androidgames.controller.games.ageofempirelite.ArmyTrainer
import com.xanticious.androidgames.controller.games.ageofempirelite.CombatResolver
import com.xanticious.androidgames.controller.games.ageofempirelite.UNIT_COSTS
import com.xanticious.androidgames.controller.games.ageofempirelite.UNIT_MAX_HP
import com.xanticious.androidgames.controller.games.ageofempirelite.UPGRADE_COSTS
import com.xanticious.androidgames.controller.games.ageofempirelite.UnitRegenerator
import com.xanticious.androidgames.controller.games.ageofempirelite.UpgradeScheduler
import com.xanticious.androidgames.controller.games.ageofempirelite.WorkerAssigner
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.ageofempirelite.ArmyComposition
import com.xanticious.androidgames.model.games.ageofempirelite.EconomyBalance
import com.xanticious.androidgames.model.games.ageofempirelite.MilitaryUnit
import com.xanticious.androidgames.model.games.ageofempirelite.UpgradeId
import com.xanticious.androidgames.model.games.ageofempirelite.UpgradePriority
import com.xanticious.androidgames.model.games.ageofempirelite.UnitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgeOfEmpiresLiteControllerTest {

    // -----------------------------------------------------------------------
    // WorkerAssigner
    // -----------------------------------------------------------------------

    @Test
    fun workerAssigner_10workers_70_30_returns7food3study() {
        val result = WorkerAssigner.assignWorkers(10, EconomyBalance(70, 30))
        assertEquals(7, result.foodWorkers)
        assertEquals(3, result.studyWorkers)
    }

    @Test
    fun workerAssigner_totalPreserved_workerCountUnchanged() {
        val workerCount = 7
        val result = WorkerAssigner.assignWorkers(workerCount, EconomyBalance(60, 40))
        assertEquals(workerCount, result.foodWorkers + result.studyWorkers)
    }

    @Test
    fun workerAssigner_0workers_returns0and0() {
        val result = WorkerAssigner.assignWorkers(0, EconomyBalance(50, 50))
        assertEquals(0, result.foodWorkers)
        assertEquals(0, result.studyWorkers)
    }

    // -----------------------------------------------------------------------
    // ArmyTrainer
    // -----------------------------------------------------------------------

    private fun makeArmy(vararg types: UnitType, side: Boolean = true): List<MilitaryUnit> =
        types.mapIndexed { i, type ->
            MilitaryUnit(i, type, side, UNIT_MAX_HP[type] ?: 60, UNIT_MAX_HP[type] ?: 60, Vec2(300f, 200f + i * 50f))
        }

    @Test
    fun armyTrainer_cannonQueue_producesCannon() {
        val result = ArmyTrainer.nextUnitToTrain(
            army = emptyList(),
            target = ArmyComposition(mapOf(UnitType.INFANTRY to 3)),
            cannonQueue = 1,
            food = 300,
            armyCap = 10,
            playerSide = true
        )
        assertEquals(UnitType.CANNON, result)
    }

    @Test
    fun armyTrainer_afterCannon_revertsToRatio() {
        // cannonQueue = 0, should revert to ratio-based training
        val result = ArmyTrainer.nextUnitToTrain(
            army = emptyList(),
            target = ArmyComposition(mapOf(UnitType.INFANTRY to 3, UnitType.ARCHER to 1)),
            cannonQueue = 0,
            food = 500,
            armyCap = 10,
            playerSide = true
        )
        assertNotNull(result)
        assertTrue(result != UnitType.CANNON)
    }

    @Test
    fun armyTrainer_largestDeficit_picksCorrectUnit() {
        // Have 2 infantry, 0 archers; target 1 infantry 2 archers → archer has largest deficit
        val army = makeArmy(UnitType.INFANTRY, UnitType.INFANTRY)
        val result = ArmyTrainer.nextUnitToTrain(
            army = army,
            target = ArmyComposition(mapOf(UnitType.INFANTRY to 1, UnitType.ARCHER to 2)),
            cannonQueue = 0,
            food = 500,
            armyCap = 10,
            playerSide = true
        )
        assertEquals(UnitType.ARCHER, result)
    }

    @Test
    fun armyTrainer_exactRatio_picksCheapest() {
        // Army exactly matches 1 infantry 1 archer → next should be cheapest (INFANTRY costs 50)
        val army = makeArmy(UnitType.INFANTRY, UnitType.ARCHER)
        val result = ArmyTrainer.nextUnitToTrain(
            army = army,
            target = ArmyComposition(mapOf(UnitType.INFANTRY to 1, UnitType.ARCHER to 1)),
            cannonQueue = 0,
            food = 500,
            armyCap = 10,
            playerSide = true
        )
        assertEquals(UnitType.INFANTRY, result)
    }

    @Test
    fun armyTrainer_armyAtCap_returnsNull() {
        val army = (0 until 10).map { i ->
            MilitaryUnit(i, UnitType.INFANTRY, true, 60, 60, Vec2(300f, i * 50f))
        }
        val result = ArmyTrainer.nextUnitToTrain(
            army = army,
            target = ArmyComposition(mapOf(UnitType.INFANTRY to 1)),
            cannonQueue = 0,
            food = 1000,
            armyCap = 10,
            playerSide = true
        )
        assertNull(result)
    }

    // -----------------------------------------------------------------------
    // UnitRegenerator
    // -----------------------------------------------------------------------

    private fun unitWithTimer(timer: Float, hp: Int = 50): MilitaryUnit =
        MilitaryUnit(
            id = 0, type = UnitType.INFANTRY, side = true,
            hp = hp, maxHp = 60, pos = Vec2(300f, 300f),
            secondsSinceLastCombat = timer
        )

    @Test
    fun unitRegenerator_outOfCombat_gainsHp() {
        val unit = unitWithTimer(timer = 10f, hp = 40)
        val result = UnitRegenerator.regenerate(listOf(unit), dt = 1f, cooldown = 5f, ratePerSec = 2f)
        assertTrue(result.first().hp > 40)
    }

    @Test
    fun unitRegenerator_recentlyDamaged_noRegen() {
        val unit = unitWithTimer(timer = 1f, hp = 40)
        val result = UnitRegenerator.regenerate(listOf(unit), dt = 1f, cooldown = 5f, ratePerSec = 2f)
        assertEquals(40, result.first().hp)
    }

    @Test
    fun unitRegenerator_atMaxHp_doesNotExceedMax() {
        val unit = unitWithTimer(timer = 10f, hp = 60)
        val result = UnitRegenerator.regenerate(listOf(unit), dt = 10f, cooldown = 5f, ratePerSec = 2f)
        assertEquals(60, result.first().hp)
    }

    // -----------------------------------------------------------------------
    // UpgradeScheduler
    // -----------------------------------------------------------------------

    @Test
    fun upgradeScheduler_prerequisiteNotMet_skipsHigherTier() {
        val priority = UpgradePriority(listOf(UpgradeId.ATTACK_II, UpgradeId.ATTACK_I))
        val result = UpgradeScheduler.nextUpgrade(
            priority = priority,
            researched = emptySet(),
            study = 1000
        )
        // ATTACK_II requires ATTACK_I; should skip to ATTACK_I
        assertEquals(UpgradeId.ATTACK_I, result)
    }

    @Test
    fun upgradeScheduler_enlightenmentRequiresAllOthers() {
        // Only LEARNING_III researched — ENLIGHTENMENT should not be returned
        val priority = UpgradePriority(listOf(UpgradeId.ENLIGHTENMENT))
        val researched = setOf(UpgradeId.LEARNING_III)
        val result = UpgradeScheduler.nextUpgrade(
            priority = priority,
            researched = researched,
            study = 1000
        )
        assertNull(result)
    }

    @Test
    fun upgradeScheduler_returnsFirstAffordable() {
        val priority = UpgradePriority(listOf(UpgradeId.ATTACK_I, UpgradeId.DEFENSE_I))
        // ATTACK_I costs 100; provide enough study
        val result = UpgradeScheduler.nextUpgrade(
            priority = priority,
            researched = emptySet(),
            study = 150
        )
        assertEquals(UpgradeId.ATTACK_I, result)
    }

    // -----------------------------------------------------------------------
    // CombatResolver
    // -----------------------------------------------------------------------

    private fun unit(id: Int, type: UnitType, side: Boolean, x: Float): MilitaryUnit =
        MilitaryUnit(id, type, side, UNIT_MAX_HP[type] ?: 60, UNIT_MAX_HP[type] ?: 60, Vec2(x, 300f))

    @Test
    fun combatResolver_infantryVsCavalry_bonusDamage() {
        // Infantry gets +50% damage vs Cavalry
        val infantry = unit(0, UnitType.INFANTRY, side = true, x = 300f)
        val cavalry = unit(1, UnitType.CAVALRY, side = false, x = 350f) // within melee range (120px)

        val result = CombatResolver.resolveCombatTick(
            playerArmy = listOf(infantry),
            botArmy = listOf(cavalry),
            dt = 1f
        )

        // Cavalry should have taken 20 * 1.5 = 30 damage → 80 - 30 = 50
        val updatedCavalryHp = result.updatedBot.first().hp
        assertEquals(50, updatedCavalryHp)
    }

    @Test
    fun combatResolver_archerVsInfantry_bonusDamage() {
        // Archer gets +50% damage vs Infantry
        val archer = unit(0, UnitType.ARCHER, side = true, x = 300f)
        val infantry = unit(1, UnitType.INFANTRY, side = false, x = 450f) // within archer range (240px)

        val result = CombatResolver.resolveCombatTick(
            playerArmy = listOf(archer),
            botArmy = listOf(infantry),
            dt = 1f
        )

        // Infantry should have taken 15 * 1.5 = 22.5 → 22 damage → 60 - 22 = 38
        val updatedInfantryHp = result.updatedBot.first().hp
        assertEquals(38, updatedInfantryHp)
    }
}
