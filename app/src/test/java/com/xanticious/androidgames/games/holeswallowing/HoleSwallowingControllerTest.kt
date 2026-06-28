package com.xanticious.androidgames.games.holeswallowing

import com.xanticious.androidgames.controller.games.holeswallowing.HoleSwallowingController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.JoystickInput
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.holeswallowing.CityObject
import com.xanticious.androidgames.model.games.holeswallowing.HoleSwallowingConfig
import com.xanticious.androidgames.model.games.holeswallowing.HoleSwallowingEvent
import com.xanticious.androidgames.model.games.holeswallowing.HoleSwallowingState
import com.xanticious.androidgames.model.games.holeswallowing.ObjectTier
import com.xanticious.androidgames.model.games.holeswallowing.TimeBonusPickup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HoleSwallowingControllerTest {

    private val controller = HoleSwallowingController()
    private val config = controller.configFor(GameDifficulty.EASY)

    private fun baseState(
        holeRadius: Float = 0.6f,
        score: Int = 0,
        timeRemaining: Float = 60f,
        objects: List<CityObject> = emptyList(),
        bonusPickups: List<TimeBonusPickup> = emptyList()
    ) = HoleSwallowingState(
        holePosition = Vec2(50f, 50f),
        holeRadius = holeRadius,
        score = score,
        timeRemaining = timeRemaining,
        objects = objects,
        bonusPickups = bonusPickups
    )

    private fun objectAt(pos: Vec2, tier: ObjectTier) = CityObject(0, pos, tier)

    @Test
    fun swallow_holeTooSmall_doesNotSwallowObject() {
        // Tier 3 sizeValue=2.0; need holeDiameter > 2.0, so holeRadius > 1.0.
        val obj = objectAt(Vec2(50.5f, 50f), ObjectTier.TIER_3)
        val state = baseState(holeRadius = 0.9f, objects = listOf(obj))
        val step = controller.step(state, config, 0f, JoystickInput.NONE)
        assertFalse(step.state.objects[0].swallowed)
    }

    @Test
    fun swallow_holeLargeEnough_swallowsObject() {
        // holeRadius=1.1 => diameter=2.2 > 2.0 => can swallow tier 3.
        val obj = objectAt(Vec2(50.5f, 50f), ObjectTier.TIER_3)
        val state = baseState(holeRadius = 1.1f, objects = listOf(obj))
        val step = controller.step(state, config, 0f, JoystickInput.NONE)
        assertTrue(step.state.objects[0].swallowed)
    }

    @Test
    fun swallow_growsHoleRadius() {
        val obj = objectAt(Vec2(50.5f, 50f), ObjectTier.TIER_1)
        val state = baseState(holeRadius = 0.6f, objects = listOf(obj))
        val step = controller.step(state, config, 0f, JoystickInput.NONE)
        assertTrue(step.state.holeRadius > state.holeRadius)
    }

    @Test
    fun swallow_tier1Object_addsCorrectScore() {
        val obj = objectAt(Vec2(50.5f, 50f), ObjectTier.TIER_1)
        val state = baseState(holeRadius = 0.6f, score = 0, objects = listOf(obj))
        val step = controller.step(state, config, 0f, JoystickInput.NONE)
        assertEquals(10, step.state.score)
    }

    @Test
    fun swallow_tier4Object_addsCorrectScore() {
        // holeRadius=3.0 => diameter=6.0 > 5.0 => can swallow tier 4 (sizeValue=5.0).
        val obj = objectAt(Vec2(50.5f, 50f), ObjectTier.TIER_4)
        val state = baseState(holeRadius = 3f, score = 0, objects = listOf(obj))
        val step = controller.step(state, config, 0f, JoystickInput.NONE)
        assertEquals(150, step.state.score)
    }

    @Test
    fun step_targetScoreReached_returnsTargetReachedEvent() {
        // Score is one tier-1 swallow away from target.
        val obj = objectAt(Vec2(50.5f, 50f), ObjectTier.TIER_1)
        val state = baseState(
            holeRadius = 0.6f,
            score = config.targetScore - 10,
            objects = listOf(obj)
        )
        val step = controller.step(state, config, 0f, JoystickInput.NONE)
        assertEquals(HoleSwallowingEvent.TARGET_REACHED, step.event)
    }

    @Test
    fun step_timerExpires_returnsTimerExpiredEvent() {
        val state = baseState(timeRemaining = 0.01f)
        val step = controller.step(state, config, 0.1f, JoystickInput.NONE)
        assertEquals(HoleSwallowingEvent.TIMER_EXPIRED, step.event)
    }

    @Test
    fun step_timeBonusPickup_extendsTimer() {
        val bonus = TimeBonusPickup(0, Vec2(50.3f, 50f))
        val state = baseState(timeRemaining = 10f, bonusPickups = listOf(bonus))
        val step = controller.step(state, config, 0f, JoystickInput.NONE)
        assertTrue(step.state.timeRemaining > 10f)
    }

    @Test
    fun step_alreadySwallowedObject_isNotSwallowedAgain() {
        val obj = CityObject(0, Vec2(50.5f, 50f), ObjectTier.TIER_1, swallowed = true)
        val state = baseState(holeRadius = 0.6f, score = 0, objects = listOf(obj))
        val step = controller.step(state, config, 0f, JoystickInput.NONE)
        assertEquals(0, step.state.score)
    }
}
