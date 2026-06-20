package com.xanticious.androidgames.controller.games.snakes2d

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.snakes2d.FoodItem
import com.xanticious.androidgames.model.games.snakes2d.FoodType
import com.xanticious.androidgames.model.games.snakes2d.PowerUpType
import com.xanticious.androidgames.model.games.snakes2d.SnakesConfig
import com.xanticious.androidgames.model.games.snakes2d.SnakesEvent
import com.xanticious.androidgames.model.games.snakes2d.SnakesInput
import com.xanticious.androidgames.model.games.snakes2d.SnakesState
import com.xanticious.androidgames.model.games.snakes2d.SnakesStep
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pure Snakes 2D game rules: steering, movement, chain-following body, food
 * effects, collision detection and scoring.
 *
 * All positions are normalized to `[0, 1]`. No Android or Compose imports — the
 * entire rule set is JVM unit-testable.
 *
 * [random] is injectable so tests can pass a deterministic [Random] instance.
 */
class SnakesController(private val random: Random = Random.Default) {

    fun configFor(difficulty: GameDifficulty): SnakesConfig = when (difficulty) {
        GameDifficulty.EASY -> SnakesConfig(
            snakeSpeed = 0.12f,
            maxTurnRate = 4f * PI_F,
            specialFoodSpawnChance = 0.30f
        )
        GameDifficulty.MEDIUM -> SnakesConfig(
            snakeSpeed = 0.18f,
            maxTurnRate = 2.5f * PI_F,
            specialFoodSpawnChance = 0.25f
        )
        GameDifficulty.HARD -> SnakesConfig(
            snakeSpeed = 0.25f,
            maxTurnRate = 1.5f * PI_F,
            specialFoodSpawnChance = 0.20f
        )
    }

    /**
     * Advances the game by [dt] seconds given [input] from the player.
     *
     * Steps in order:
     * 1. Update tap target / marker from input.
     * 2. Steer direction toward target (rate-limited to prevent instant reversals).
     * 3. Move head; chain-follow body segments.
     * 4. Wall-collision check.
     * 5. Self-collision check (bypassed in ghost mode).
     * 6. Food collision and effects.
     * 7. Timer updates and food maintenance.
     */
    fun step(state: SnakesState, config: SnakesConfig, dt: Float, input: SnakesInput): SnakesStep {
        var s = processInput(state, input, dt)
        s = s.copy(direction = steer(s.direction, s.segments.first(), s.target, config, dt))

        val speed = config.snakeSpeed * s.speedMultiplier * lengthSpeedBonus(s.segments.size)
        val newHead = s.segments.first() + s.direction * (speed * dt)
        s = s.copy(segments = chainFollow(newHead, s.segments, config.segmentLength))

        if (isWallCollision(newHead)) return SnakesStep(s, SnakesEvent.COLLISION, null)
        if (!s.ghostMode && isSelfCollision(newHead, s.segments, config)) {
            return SnakesStep(s, SnakesEvent.COLLISION, null)
        }

        val eaten = s.foods.firstOrNull { it.position.distanceTo(newHead) < config.foodEatRadius }
        if (eaten != null) {
            s = applyFoodEffect(s.copy(foods = s.foods.filter { it.id != eaten.id }), config, eaten)
            s = updateTimers(s, config, dt)
            s = ensureFood(s, config, dt)
            return SnakesStep(s, SnakesEvent.FOOD_EATEN, eaten)
        }

        s = updateTimers(s, config, dt)
        s = ensureFood(s, config, dt)
        return SnakesStep(s, SnakesEvent.NONE, null)
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun processInput(state: SnakesState, input: SnakesInput, dt: Float): SnakesState =
        if (input.tapTarget != null) {
            state.copy(
                target = input.tapTarget,
                tapMarkerPos = input.tapTarget,
                tapMarkerTimer = TAP_MARKER_DURATION
            )
        } else {
            state.copy(tapMarkerTimer = (state.tapMarkerTimer - dt).coerceAtLeast(0f))
        }

    /**
     * Rotates [direction] toward [target] by at most [config.maxTurnRate] * [dt]
     * radians, preventing instant 180° reversals while allowing tight curves.
     */
    internal fun steer(
        direction: Vec2,
        headPos: Vec2,
        target: Vec2?,
        config: SnakesConfig,
        dt: Float
    ): Vec2 {
        target ?: return direction
        val toTarget = target - headPos
        if (toTarget.length < 1e-4f) return direction

        val desired = toTarget.normalized()
        // Cross and dot give sin/cos of the signed angle from direction to desired.
        val cross = direction.x * desired.y - direction.y * desired.x
        val dot = direction.dot(desired)
        val angleDiff = atan2(cross, dot) // already in [-PI, PI]

        val maxTurn = config.maxTurnRate * dt
        val turn = angleDiff.coerceIn(-maxTurn, maxTurn)
        val cosT = cos(turn)
        val sinT = sin(turn)
        return Vec2(
            direction.x * cosT - direction.y * sinT,
            direction.x * sinT + direction.y * cosT
        ).normalized()
    }

    /**
     * Moves [newHead] to the front and repositions each body segment so that the
     * distance to the segment ahead never exceeds [segmentLength].
     */
    internal fun chainFollow(newHead: Vec2, segments: List<Vec2>, segmentLength: Float): List<Vec2> {
        val result = ArrayList<Vec2>(segments.size)
        result.add(newHead)
        for (i in 1 until segments.size) {
            val prev = result[i - 1]
            val curr = segments[i]
            val diff = curr - prev
            val dist = diff.length
            result.add(if (dist > segmentLength) prev + diff.normalized() * segmentLength else curr)
        }
        return result
    }

    private fun isWallCollision(pos: Vec2): Boolean =
        pos.x < 0f || pos.x > 1f || pos.y < 0f || pos.y > 1f

    private fun isSelfCollision(head: Vec2, segments: List<Vec2>, config: SnakesConfig): Boolean {
        if (segments.size <= SELF_COLLISION_SKIP) return false
        return segments.drop(SELF_COLLISION_SKIP).any { it.distanceTo(head) < config.selfCollisionRadius }
    }

    private fun applyFoodEffect(state: SnakesState, config: SnakesConfig, food: FoodItem): SnakesState {
        var s = state
        when (food.type) {
            FoodType.STANDARD -> {
                s = s.copy(segments = growTail(s.segments, 1), score = s.score + 10)
            }
            FoodType.BONUS -> {
                s = s.copy(segments = growTail(s.segments, 3), score = s.score + 50)
            }
            FoodType.SPEED_BOOST -> {
                s = s.copy(
                    segments = growTail(s.segments, 1),
                    score = s.score + 20,
                    activePowerUp = PowerUpType.SPEED_BOOST,
                    speedMultiplier = config.speedBoostMultiplier,
                    powerUpTimer = config.powerUpDuration
                )
            }
            FoodType.SLOW -> {
                s = s.copy(
                    segments = growTail(s.segments, 1),
                    score = s.score + 20,
                    activePowerUp = PowerUpType.SLOW,
                    speedMultiplier = config.slowMultiplier,
                    powerUpTimer = config.powerUpDuration
                )
            }
            FoodType.SHRINK -> {
                val removeCount = minOf(3, s.segments.size - MIN_SEGMENTS)
                s = s.copy(
                    segments = if (removeCount > 0) s.segments.dropLast(removeCount) else s.segments,
                    score = s.score + 100
                )
            }
            FoodType.GHOST -> {
                s = s.copy(
                    segments = growTail(s.segments, 1),
                    score = s.score + 20,
                    activePowerUp = PowerUpType.GHOST,
                    ghostMode = true,
                    powerUpTimer = config.powerUpDuration
                )
            }
        }
        // Length milestone bonus (only when snake grew)
        if (s.segments.size > state.segments.size && s.segments.size % config.milestoneEvery == 0) {
            s = s.copy(score = s.score + 200)
        }
        return s.copy(bestScore = maxOf(s.bestScore, s.score))
    }

    private fun growTail(segments: List<Vec2>, count: Int): List<Vec2> =
        segments + List(count) { segments.last() }

    private fun updateTimers(state: SnakesState, config: SnakesConfig, dt: Float): SnakesState {
        var s = state.copy(elapsedTime = state.elapsedTime + dt)

        // Survival bonus every [survivalBonusInterval] seconds.
        val newSurvivalTimer = s.survivalTimer + dt
        if (newSurvivalTimer >= config.survivalBonusInterval) {
            s = s.copy(
                score = s.score + 150,
                bestScore = maxOf(s.bestScore, s.score + 150),
                survivalTimer = newSurvivalTimer - config.survivalBonusInterval
            )
        } else {
            s = s.copy(survivalTimer = newSurvivalTimer)
        }

        // Power-up countdown.
        if (s.powerUpTimer > 0f) {
            val remaining = (s.powerUpTimer - dt).coerceAtLeast(0f)
            s = if (remaining == 0f) {
                s.copy(powerUpTimer = 0f, activePowerUp = null, speedMultiplier = 1f, ghostMode = false)
            } else {
                s.copy(powerUpTimer = remaining)
            }
        }

        // Bonus-food countdown — remove expired items.
        val updatedFoods = s.foods.mapNotNull { food ->
            if (food.timeRemaining <= 0f) food  // permanent
            else {
                val remaining = food.timeRemaining - dt
                if (remaining <= 0f) null else food.copy(timeRemaining = remaining)
            }
        }
        return s.copy(foods = updatedFoods)
    }

    private fun ensureFood(state: SnakesState, config: SnakesConfig, dt: Float): SnakesState {
        var s = state

        // Always maintain at least one standard food.
        if (s.foods.none { it.type == FoodType.STANDARD }) {
            val pos = randomFoodPosition(s.segments, s.foods)
            s = s.copy(
                foods = s.foods + FoodItem(s.foodIdCounter, pos, FoodType.STANDARD, -1f),
                foodIdCounter = s.foodIdCounter + 1
            )
        }

        // Occasionally spawn one special food alongside standard food.
        if (s.foods.none { it.type != FoodType.STANDARD }
            && random.nextFloat() < config.specialFoodSpawnChance * dt
        ) {
            val type = randomSpecialFoodType()
            val pos = randomFoodPosition(s.segments, s.foods)
            val duration = if (type == FoodType.BONUS) config.bonusFoodDuration else -1f
            s = s.copy(
                foods = s.foods + FoodItem(s.foodIdCounter, pos, type, duration),
                foodIdCounter = s.foodIdCounter + 1
            )
        }

        return s
    }

    private fun randomFoodPosition(segments: List<Vec2>, existingFoods: List<FoodItem>): Vec2 {
        repeat(20) {
            val pos = Vec2(random.nextFloat() * 0.8f + 0.1f, random.nextFloat() * 0.8f + 0.1f)
            val clearOfSnake = segments.none { it.distanceTo(pos) < 0.08f }
            val clearOfFood = existingFoods.none { it.position.distanceTo(pos) < 0.06f }
            if (clearOfSnake && clearOfFood) return pos
        }
        return Vec2(random.nextFloat() * 0.6f + 0.2f, random.nextFloat() * 0.6f + 0.2f)
    }

    private fun randomSpecialFoodType(): FoodType = when (random.nextInt(5)) {
        0 -> FoodType.BONUS
        1 -> FoodType.SPEED_BOOST
        2 -> FoodType.SLOW
        3 -> FoodType.SHRINK
        else -> FoodType.GHOST
    }

    /** +5 % speed per 25 segments of length. */
    private fun lengthSpeedBonus(segmentCount: Int): Float = 1f + (segmentCount / 25) * 0.05f

    companion object {
        private val PI_F = Math.PI.toFloat()
        private const val SELF_COLLISION_SKIP = 5
        private const val MIN_SEGMENTS = 5
        private const val TAP_MARKER_DURATION = 1f
    }
}
