package com.xanticious.androidgames.model.games.holeswallowing

import com.xanticious.androidgames.model.Vec2

/**
 * Size tiers for city objects. [sizeValue] is the object's effective diameter in world units.
 * [mass] drives hole growth on swallow. [score] is added to the player's total on swallow.
 */
enum class ObjectTier(val sizeValue: Float, val mass: Float, val score: Int) {
    TIER_1(0.5f, 0.5f, 10),
    TIER_2(1.0f, 1.0f, 25),
    TIER_3(2.0f, 2.0f, 50),
    TIER_4(5.0f, 5.0f, 150),
    TIER_5(10.0f, 10.0f, 400),
    TIER_6(25.0f, 25.0f, 1000)
}

/** A single city object the hole can swallow. */
data class CityObject(
    val id: Int,
    val position: Vec2,
    val tier: ObjectTier,
    val swallowed: Boolean = false
)

/** A glowing clock pickup that extends the countdown timer when collected. */
data class TimeBonusPickup(
    val id: Int,
    val position: Vec2,
    val collected: Boolean = false
)

/**
 * Tuning values derived from the selected difficulty. Includes the level layout
 * so the controller can assemble everything in one place.
 */
data class HoleSwallowingConfig(
    val holeSpeed: Float,
    val growthFactor: Float,
    val targetScore: Int,
    val timeLimit: Float,
    val timeBonusSeconds: Float,
    val objects: List<CityObject>,
    val bonusPickups: List<TimeBonusPickup>
)

/**
 * Full game state snapshot. All positions use a 100×100 world grid.
 * The camera in the view centres on [holePosition].
 */
data class HoleSwallowingState(
    val holePosition: Vec2,
    val holeRadius: Float,
    val score: Int,
    val timeRemaining: Float,
    val objects: List<CityObject>,
    val bonusPickups: List<TimeBonusPickup>
) {
    companion object {
        const val WORLD_W = 100f
        const val WORLD_H = 100f
    }
}

/** Outcome of a single physics step. */
enum class HoleSwallowingEvent {
    NONE, SWALLOWED, BONUS_COLLECTED, TARGET_REACHED, TIMER_EXPIRED
}

data class HoleSwallowingStep(
    val state: HoleSwallowingState,
    val event: HoleSwallowingEvent
)
