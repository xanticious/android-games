package com.xanticious.androidgames.games.idlefarmers

import com.xanticious.androidgames.controller.games.idlefarmers.IdleFarmersController
import com.xanticious.androidgames.model.games.idlefarmers.Era
import com.xanticious.androidgames.model.games.idlefarmers.RandomEvent
import com.xanticious.androidgames.model.games.idlefarmers.RandomEventType
import org.junit.Assert.*
import org.junit.Test

class IdleFarmersControllerTest {
    private val controller = IdleFarmersController()

    @Test
    fun initialState_hasSingleFarmer() {
        assertEquals(1, controller.initialState().farmers)
    }

    @Test
    fun initialState_hasSinglePlot() {
        assertEquals(1, controller.initialState().plots)
    }

    @Test
    fun initialState_coinsAreZero() {
        assertEquals(0L, controller.initialState().coins)
    }

    @Test
    fun initialState_subsistenceFarmingIsPurchased() {
        assertTrue(controller.initialState().upgrades.first { it.id == "subsistence-farming" }.purchased)
    }

    @Test
    fun computeHarvestCrops_baseFarmerAndPlot_returnsOne() {
        assertEquals(1f, controller.computeHarvestCrops(controller.initialState()), 0.0001f)
    }

    @Test
    fun computeCoins_baseCrop_returnsAtLeastOne() {
        assertTrue(controller.computeCoins(1f, controller.initialState()) >= 1L)
    }

    @Test
    fun harvest_addsCoinsToState() {
        assertTrue(controller.harvest(controller.initialState()).coins > 0L)
    }

    @Test
    fun harvest_incrementsCycleCount() {
        assertEquals(1L, controller.harvest(controller.initialState()).cycleCount)
    }

    @Test
    fun canPurchase_withInsufficientCoins_returnsFalse() {
        assertFalse(controller.canPurchase(controller.initialState(), "cleared-land"))
    }

    @Test
    fun canPurchase_withSufficientCoins_andPrereqMet_returnsTrue() {
        assertTrue(controller.canPurchase(controller.initialState().copy(coins = 10), "cleared-land"))
    }

    @Test
    fun canPurchase_whenAlreadyPurchased_returnsFalse() {
        assertFalse(controller.canPurchase(controller.initialState().copy(coins = 999), "subsistence-farming"))
    }

    @Test
    fun purchaseUpgrade_clearedLand_incrementsPlots() {
        val upgraded = controller.purchaseUpgrade(controller.initialState().copy(coins = 10), "cleared-land")
        assertEquals(2, upgraded.plots)
    }

    @Test
    fun purchaseUpgrade_trainFarmer_incrementsFarmers() {
        val prepared = controller.purchaseUpgrade(controller.initialState().copy(coins = 210), "cleared-land")
        val upgraded = controller.purchaseUpgrade(prepared, "train-farmer")
        assertEquals(2, upgraded.farmers)
    }

    @Test
    fun computeEra_withNoUpgrades_returnsNeolithic() {
        assertEquals(Era.NEOLITHIC, controller.computeEra(emptyList()))
    }

    @Test
    fun computeEra_withAncientUpgradePurchased_returnsAtLeastAncient() {
        val upgrades = controller.upgradeTree().map { upgrade ->
            if (upgrade.id == "discover-barley") upgrade.copy(purchased = true) else upgrade
        }
        assertTrue(controller.computeEra(upgrades).ordinal >= Era.ANCIENT.ordinal)
    }

    @Test
    fun availableUpgrades_initialState_containsClearedLand() {
        assertTrue(controller.availableUpgrades(controller.initialState()).any { it.id == "cleared-land" })
    }

    @Test
    fun tickEvent_singleCycle_decrementsRemaining() {
        assertEquals(1, controller.tickEvent(RandomEvent(RandomEventType.BUMPER_HARVEST, 2, 2))?.remainingCycles)
    }

    @Test
    fun tickEvent_lastCycle_returnsNull() {
        assertNull(controller.tickEvent(RandomEvent(RandomEventType.FESTIVAL, 1, 1)))
    }
}
