package com.xanticious.androidgames.games.idlebounce

import com.xanticious.androidgames.controller.games.idlebounce.IdleBounceController
import com.xanticious.androidgames.model.games.idlebounce.IdleBounceGameState
import com.xanticious.androidgames.model.games.idlebounce.IdleBounceStepEvent
import com.xanticious.androidgames.model.games.idlebounce.Layer
import com.xanticious.androidgames.model.games.idlebounce.LayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class IdleBounceControllerTest {
    private val controller = IdleBounceController(Random(seed = 42))

    @Test
    fun step_shortDt_doesNotFireHit() {
        val step = controller.step(IdleBounceGameState.initial(), 0.25f)
        assertEquals(IdleBounceStepEvent.NONE, step.event)
    }

    @Test
    fun step_fullIntervalDt_firesBallHit() {
        val step = controller.step(IdleBounceGameState.initial(), 1f)
        assertEquals(IdleBounceStepEvent.BALL_HIT, step.event)
    }

    @Test
    fun step_enoughDamage_returnsLayerDestroyed() {
        val state = IdleBounceGameState.initial().copy(currentLayer = Layer(LayerType.DIRT, 1L, 1L, 2L))
        val step = controller.step(state, 1f)
        assertEquals(IdleBounceStepEvent.LAYER_DESTROYED, step.event)
    }

    @Test
    fun step_layerDestroyed_incrementsDepth() {
        val state = IdleBounceGameState.initial().copy(currentLayer = Layer(LayerType.DIRT, 1L, 1L, 2L))
        val step = controller.step(state, 1f)
        assertEquals(1, step.state.depth)
    }

    @Test
    fun step_layerDestroyed_awardsCoins() {
        val state = IdleBounceGameState.initial().copy(currentLayer = Layer(LayerType.DIRT, 1L, 1L, 5L))
        val step = controller.step(state, 1f)
        assertEquals(5L, step.coinsEarned)
    }

    @Test
    fun upgradeCost_level0_returnsBaseCost() {
        assertEquals(10L, controller.upgradeCost(IdleBounceGameState.initial(), "bounce-power"))
    }

    @Test
    fun upgradeCost_level1_returnsScaledCost() {
        val state = IdleBounceGameState.initial().copy(
            upgrades = IdleBounceGameState.INITIAL_UPGRADES.map {
                if (it.id == "bounce-power") it.copy(level = 1) else it
            }
        )
        assertEquals(15L, controller.upgradeCost(state, "bounce-power"))
    }

    @Test
    fun buyUpgrade_whenAffordable_returnsNewState() {
        val upgraded = controller.buyUpgrade(IdleBounceGameState.initial().copy(coins = 10L), "bounce-power")
        assertNotNull(upgraded)
    }

    @Test
    fun buyUpgrade_whenNotAffordable_returnsNull() {
        assertNull(controller.buyUpgrade(IdleBounceGameState.initial(), "bounce-power"))
    }

    @Test
    fun buyUpgrade_atMaxLevel_returnsNull() {
        val state = IdleBounceGameState.initial().copy(
            coins = 999_999L,
            upgrades = IdleBounceGameState.INITIAL_UPGRADES.map {
                if (it.id == "bounce-power") it.copy(level = 50) else it
            }
        )
        assertNull(controller.buyUpgrade(state, "bounce-power"))
    }

    @Test
    fun buyUpgrade_bouncePower_increasesBallPower() {
        val upgraded = controller.buyUpgrade(IdleBounceGameState.initial().copy(coins = 10L), "bounce-power")
        assertEquals(2L, upgraded?.ball?.power)
    }

    @Test
    fun buyUpgrade_bounceSpeed_increasesBallSpeed() {
        val upgraded = controller.buyUpgrade(IdleBounceGameState.initial().copy(coins = 25L), "bounce-speed")
        assertEquals(1.5f, upgraded?.ball?.hitsPerSecond)
    }

    @Test
    fun prestige_whenBedrockReached_resetsDepth() {
        val prestiged = controller.prestige(IdleBounceGameState.initial().copy(bedrockReached = true, depth = 82))
        assertEquals(0, prestiged?.depth)
    }

    @Test
    fun prestige_whenBedrockNotReached_returnsNull() {
        assertNull(controller.prestige(IdleBounceGameState.initial()))
    }

    @Test
    fun generateLayer_depth0_returnsDirt() {
        assertEquals(LayerType.DIRT, controller.generateLayer(0, 1f).type)
    }

    @Test
    fun generateLayer_depth80_returnsBedrock() {
        assertEquals(LayerType.BEDROCK, controller.generateLayer(80, 1f).type)
    }

    @Test
    fun recalcBall_afterBouncePowerLevel2_hasPower3() {
        val ball = controller.recalcBall(
            IdleBounceGameState.INITIAL_UPGRADES.map {
                if (it.id == "bounce-power") it.copy(level = 2) else it
            }
        )
        assertEquals(3L, ball.power)
    }
}
