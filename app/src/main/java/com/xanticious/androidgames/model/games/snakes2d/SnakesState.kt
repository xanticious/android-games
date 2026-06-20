package com.xanticious.androidgames.model.games.snakes2d

import com.xanticious.androidgames.model.Vec2

/** Visual and mechanical food categories. */
enum class FoodType { STANDARD, BONUS, SPEED_BOOST, SLOW, SHRINK, GHOST }

/** Active power-up applied to the snake. */
enum class PowerUpType { SPEED_BOOST, SLOW, GHOST }

/**
 * A single food item on the play field.
 *
 * [timeRemaining] is -1f for permanent items and a positive countdown (seconds)
 * for timed items such as [FoodType.BONUS].
 */
data class FoodItem(
    val id: Int,
    val position: Vec2,
    val type: FoodType,
    val timeRemaining: Float
)

/**
 * Tuning values derived from the selected difficulty.
 * All positions/speeds use a normalized board where x and y are in `[0, 1]`.
 */
data class SnakesConfig(
    val snakeSpeed: Float,
    val maxTurnRate: Float,
    val segmentLength: Float = 0.030f,
    val startingSegments: Int = 5,
    val foodEatRadius: Float = 0.032f,
    val selfCollisionRadius: Float = 0.022f,
    val bonusFoodDuration: Float = 5f,
    val powerUpDuration: Float = 5f,
    val speedBoostMultiplier: Float = 1.25f,
    val slowMultiplier: Float = 0.75f,
    val milestoneEvery: Int = 10,
    val survivalBonusInterval: Float = 30f,
    /** Expected special-food spawns per second (Poisson-style probability). */
    val specialFoodSpawnChance: Float
)

/**
 * Full immutable snake game state.
 *
 * [segments] is head-first; each entry is a normalized `[0,1]` position.
 * [direction] is always a unit vector pointing in the snake's current travel direction.
 * [target] is the tap-targeted destination in `[0,1]`; null means continue straight.
 */
data class SnakesState(
    val segments: List<Vec2>,
    val direction: Vec2,
    val target: Vec2?,
    val foods: List<FoodItem>,
    val score: Int,
    val bestScore: Int,
    val elapsedTime: Float,
    val speedMultiplier: Float,
    val powerUpTimer: Float,
    val activePowerUp: PowerUpType?,
    val ghostMode: Boolean,
    val tapMarkerPos: Vec2?,
    val tapMarkerTimer: Float,
    val foodIdCounter: Int,
    val survivalTimer: Float
) {
    val length: Int get() = segments.size

    companion object {
        fun initial(config: SnakesConfig, bestScore: Int = 0): SnakesState {
            val segments = List(config.startingSegments) { i ->
                Vec2(0.5f - i * config.segmentLength, 0.5f)
            }
            return SnakesState(
                segments = segments,
                direction = Vec2(1f, 0f),
                target = null,
                foods = emptyList(),
                score = 0,
                bestScore = bestScore,
                elapsedTime = 0f,
                speedMultiplier = 1f,
                powerUpTimer = 0f,
                activePowerUp = null,
                ghostMode = false,
                tapMarkerPos = null,
                tapMarkerTimer = 0f,
                foodIdCounter = 0,
                survivalTimer = 0f
            )
        }
    }
}

/** Per-frame player input: latest tap destination in normalized coords, or null. */
data class SnakesInput(val tapTarget: Vec2?)

/** Significant events that can arise from a single physics step. */
enum class SnakesEvent { NONE, FOOD_EATEN, COLLISION }

/** Result of one [com.xanticious.androidgames.controller.games.snakes2d.SnakesController.step] call. */
data class SnakesStep(
    val state: SnakesState,
    val event: SnakesEvent,
    val foodEaten: FoodItem?
)
