package com.xanticious.androidgames.controller.games.holeswallowing

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.JoystickInput
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.holeswallowing.CityObject
import com.xanticious.androidgames.model.games.holeswallowing.HoleSwallowingConfig
import com.xanticious.androidgames.model.games.holeswallowing.HoleSwallowingEvent
import com.xanticious.androidgames.model.games.holeswallowing.HoleSwallowingState
import com.xanticious.androidgames.model.games.holeswallowing.HoleSwallowingStep
import com.xanticious.androidgames.model.games.holeswallowing.ObjectTier
import com.xanticious.androidgames.model.games.holeswallowing.TimeBonusPickup
import kotlin.math.sqrt

/**
 * Pure Hole-Swallowing rules: movement, collision/swallow detection, scoring,
 * growth and timer logic. No Android or Compose imports — fully JVM unit-testable.
 */
class HoleSwallowingController {

    private companion object {
        const val INITIAL_HOLE_RADIUS = 0.6f
        const val BONUS_CONTACT_RADIUS = 0.5f
    }

    fun configFor(difficulty: GameDifficulty): HoleSwallowingConfig = when (difficulty) {
        GameDifficulty.EASY -> HoleSwallowingConfig(
            holeSpeed = 6f, growthFactor = 0.08f, targetScore = 500,
            timeLimit = 180f, timeBonusSeconds = 5f,
            objects = generateObjects(), bonusPickups = generateBonuses()
        )
        GameDifficulty.MEDIUM -> HoleSwallowingConfig(
            holeSpeed = 7f, growthFactor = 0.08f, targetScore = 1000,
            timeLimit = 120f, timeBonusSeconds = 5f,
            objects = generateObjects(), bonusPickups = generateBonuses()
        )
        GameDifficulty.HARD -> HoleSwallowingConfig(
            holeSpeed = 8f, growthFactor = 0.08f, targetScore = 2000,
            timeLimit = 90f, timeBonusSeconds = 5f,
            objects = generateObjects(), bonusPickups = generateBonuses()
        )
    }

    /** Builds a fresh game state from a config. */
    fun initialState(config: HoleSwallowingConfig): HoleSwallowingState =
        HoleSwallowingState(
            holePosition = Vec2(HoleSwallowingState.WORLD_W / 2f, HoleSwallowingState.WORLD_H / 2f),
            holeRadius = INITIAL_HOLE_RADIUS,
            score = 0,
            timeRemaining = config.timeLimit,
            objects = config.objects,
            bonusPickups = config.bonusPickups
        )

    /**
     * Advances the game by [dt] seconds given [joystick] input.
     *
     * Swallow rule: an object is consumed only when the hole's diameter exceeds
     * the object's [ObjectTier.sizeValue] AND the circles are in contact.
     * On swallow, the hole radius grows by the area formula:
     *   newRadius = sqrt(r² + mass × growthFactor)
     */
    fun step(
        state: HoleSwallowingState,
        config: HoleSwallowingConfig,
        dt: Float,
        joystick: JoystickInput
    ): HoleSwallowingStep {
        // Move hole in joystick direction, clamped to world bounds.
        val dir = Vec2(joystick.dx, joystick.dy).normalized()
        val newPos = (state.holePosition + dir * config.holeSpeed * dt).coerceIn(
            state.holeRadius, state.holeRadius,
            HoleSwallowingState.WORLD_W - state.holeRadius,
            HoleSwallowingState.WORLD_H - state.holeRadius
        )

        // Use the radius at tick-start for all collision checks so that objects
        // swallowed within the same tick don't chain-react.
        val checkRadius = state.holeRadius
        var accRadius = state.holeRadius
        var accScore = state.score
        var hadSwallow = false

        val newObjects = state.objects.map { obj ->
            if (!obj.swallowed) {
                val dist = newPos.distanceTo(obj.position)
                val canSwallow = checkRadius * 2f > obj.tier.sizeValue
                val inContact = dist <= checkRadius + obj.tier.sizeValue / 2f
                if (canSwallow && inContact) {
                    accRadius = sqrt(accRadius * accRadius + obj.tier.mass * config.growthFactor)
                    accScore += obj.tier.score
                    hadSwallow = true
                    obj.copy(swallowed = true)
                } else obj
            } else obj
        }

        var newTime = state.timeRemaining - dt
        var hadBonus = false

        val newBonuses = state.bonusPickups.map { bonus ->
            if (!bonus.collected) {
                val dist = newPos.distanceTo(bonus.position)
                if (dist <= checkRadius + BONUS_CONTACT_RADIUS) {
                    newTime += config.timeBonusSeconds
                    accScore += 50
                    hadBonus = true
                    bonus.copy(collected = true)
                } else bonus
            } else bonus
        }

        val event = when {
            accScore >= config.targetScore -> HoleSwallowingEvent.TARGET_REACHED
            newTime <= 0f -> HoleSwallowingEvent.TIMER_EXPIRED
            hadBonus -> HoleSwallowingEvent.BONUS_COLLECTED
            hadSwallow -> HoleSwallowingEvent.SWALLOWED
            else -> HoleSwallowingEvent.NONE
        }

        return HoleSwallowingStep(
            state = state.copy(
                holePosition = newPos,
                holeRadius = accRadius,
                score = accScore,
                timeRemaining = newTime.coerceAtLeast(0f),
                objects = newObjects,
                bonusPickups = newBonuses
            ),
            event = event
        )
    }

    // Deterministic level layout using a fixed seed so all difficulties share the same map.
    private fun generateObjects(): List<CityObject> {
        val rng = kotlin.random.Random(12345)
        val objects = mutableListOf<CityObject>()
        var id = 0
        val distribution = listOf(
            ObjectTier.TIER_1 to 80,
            ObjectTier.TIER_2 to 40,
            ObjectTier.TIER_3 to 20,
            ObjectTier.TIER_4 to 10,
            ObjectTier.TIER_5 to 5,
            ObjectTier.TIER_6 to 2
        )
        for ((tier, count) in distribution) {
            repeat(count) {
                val x = rng.nextFloat() * 90f + 5f
                val y = rng.nextFloat() * 90f + 5f
                objects.add(CityObject(id++, Vec2(x, y), tier))
            }
        }
        return objects
    }

    private fun generateBonuses(): List<TimeBonusPickup> {
        val rng = kotlin.random.Random(99999)
        return List(5) { i ->
            TimeBonusPickup(
                id = i,
                position = Vec2(rng.nextFloat() * 80f + 10f, rng.nextFloat() * 80f + 10f)
            )
        }
    }
}
