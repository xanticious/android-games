package com.xanticious.androidgames.games.simcityblocks

import com.xanticious.androidgames.controller.games.simcityblocks.SimCityBlocksController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.simcityblocks.CityPurchase
import com.xanticious.androidgames.model.games.simcityblocks.DisasterType
import com.xanticious.androidgames.model.games.simcityblocks.ZoneType
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimCityBlocksControllerTest {
    private val controller = SimCityBlocksController()
    private val config = controller.configFor(GameDifficulty.MEDIUM)

    @Test
    fun initialState_startsWithTwoResidentialZones() {
        val state = controller.initialState(config)
        assertEquals(2, state.grid.zones.count { it.type == ZoneType.RESIDENTIAL })
    }

    @Test
    fun purchase_commercialZone_placesBuildingAutomatically() {
        val state = controller.initialState(config)
        val outcome = controller.purchase(state, config, CityPurchase.BuyZone(ZoneType.COMMERCIAL))
        assertEquals(1, outcome.state.grid.zones.count { it.type == ZoneType.COMMERCIAL })
    }

    @Test
    fun validatePurchase_withoutFunds_blocksPurchase() {
        val state = controller.initialState(config).let { it.copy(resources = it.resources.copy(budget = 0)) }
        val validation = controller.validatePurchase(state, config, CityPurchase.BuyZone(ZoneType.INDUSTRIAL))
        assertFalse(validation.canPurchase)
    }

    @Test
    fun advanceCycle_withCommercialJobs_generatesIncome() {
        val state = controller.purchase(controller.initialState(config), config, CityPurchase.BuyZone(ZoneType.COMMERCIAL)).state
        val outcome = controller.advanceCycle(state, config, Random(1), disasterChance = 0f)
        assertTrue(outcome.state.resources.income > 0)
    }

    @Test
    fun applyDisaster_fire_removesAtLeastOneBuilding() {
        val state = controller.purchase(controller.initialState(config), config, CityPurchase.BuyZone(ZoneType.COMMERCIAL)).state
        val burned = controller.applyDisaster(state, Random(2), DisasterType.FIRE)
        assertTrue(burned.grid.buildings.size < state.grid.buildings.size)
    }

    @Test
    fun advanceCycle_withoutJobs_abandonsUnhappyResidentialZone() {
        val state = controller.initialState(config).let {
            it.copy(grid = it.grid.copy(zones = it.grid.zones.map { zone -> zone.copy(happiness = 10) }))
        }
        val outcome = controller.advanceCycle(state, config, Random(3), disasterChance = 0f)
        assertTrue(outcome.zoneAbandoned)
    }

    @Test
    fun applyDisaster_economicSlump_reducesIncome() {
        val state = controller.purchase(controller.initialState(config), config, CityPurchase.BuyZone(ZoneType.COMMERCIAL)).state
        val normal = controller.advanceCycle(state, config, Random(4), disasterChance = 0f).state.resources.income
        val slumped = controller.advanceCycle(controller.applyDisaster(state, Random(5), DisasterType.ECONOMIC_SLUMP), config, Random(6), disasterChance = 0f).state.resources.income
        assertTrue(slumped < normal)
    }

    @Test
    fun advanceCycle_criticalDeficitLimit_setsGameOver() {
        val state = controller.initialState(config).let { it.copy(resources = it.resources.copy(budget = -1, criticalCycles = 9)) }
        val outcome = controller.advanceCycle(state, config, Random(7), disasterChance = 0f)
        assertTrue(outcome.state.gameOver)
    }
}
