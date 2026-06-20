package com.xanticious.androidgames.games.snakes2d

import com.xanticious.androidgames.controller.games.snakes2d.SnakesController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.snakes2d.FoodItem
import com.xanticious.androidgames.model.games.snakes2d.FoodType
import com.xanticious.androidgames.model.games.snakes2d.SnakesEvent
import com.xanticious.androidgames.model.games.snakes2d.SnakesInput
import com.xanticious.androidgames.model.games.snakes2d.SnakesState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SnakesControllerTest {

    private val controller = SnakesController(Random(seed = 42))
    private val config = controller.configFor(GameDifficulty.MEDIUM)
    private val noInput = SnakesInput(tapTarget = null)

    private fun freshState() = SnakesState.initial(config)

    // -------------------------------------------------------------------------
    // configFor
    // -------------------------------------------------------------------------

    @Test
    fun configFor_easy_hasSlowerSpeedThanMedium() {
        val easy = controller.configFor(GameDifficulty.EASY)
        val medium = controller.configFor(GameDifficulty.MEDIUM)
        assertTrue(easy.snakeSpeed < medium.snakeSpeed)
    }

    @Test
    fun configFor_hard_hasFasterSpeedThanMedium() {
        val medium = controller.configFor(GameDifficulty.MEDIUM)
        val hard = controller.configFor(GameDifficulty.HARD)
        assertTrue(hard.snakeSpeed > medium.snakeSpeed)
    }

    @Test
    fun configFor_easy_hasHigherTurnRateThanHard() {
        val easy = controller.configFor(GameDifficulty.EASY)
        val hard = controller.configFor(GameDifficulty.HARD)
        assertTrue(easy.maxTurnRate > hard.maxTurnRate)
    }

    // -------------------------------------------------------------------------
    // step — movement
    // -------------------------------------------------------------------------

    @Test
    fun step_noInput_headMovesInCurrentDirection() {
        val state = freshState()
        val headBefore = state.segments.first()
        val result = controller.step(state, config, dt = 0.016f, noInput)
        val headAfter = result.state.segments.first()
        assertTrue(headAfter.x > headBefore.x) // starts moving right
    }

    @Test
    fun step_noInput_returnsNoEvent() {
        val state = freshState().copy(foods = listOf(farAwayFood(0)))
        val result = controller.step(state, config, dt = 0.016f, noInput)
        assertEquals(SnakesEvent.NONE, result.event)
    }

    @Test
    fun step_noInput_bodyChainFollowsHead() {
        var state = freshState().copy(foods = listOf(farAwayFood(0)))
        repeat(30) { state = controller.step(state, config, dt = 0.016f, noInput).state }
        // Every consecutive pair must be within segmentLength of each other.
        for (i in 1 until state.segments.size) {
            val dist = state.segments[i].distanceTo(state.segments[i - 1])
            assertTrue(dist <= config.segmentLength + 1e-4f)
        }
    }

    // -------------------------------------------------------------------------
    // step — wall collision
    // -------------------------------------------------------------------------

    @Test
    fun step_headPastRightWall_returnsCollisionEvent() {
        val state = freshState().copy(
            segments = listOf(Vec2(0.99f, 0.5f)) + freshState().segments.drop(1),
            direction = Vec2(1f, 0f)
        )
        val result = controller.step(state, config, dt = 0.1f, noInput)
        assertEquals(SnakesEvent.COLLISION, result.event)
    }

    @Test
    fun step_headPastTopWall_returnsCollisionEvent() {
        val state = freshState().copy(
            segments = listOf(Vec2(0.5f, 0.01f)) + freshState().segments.drop(1),
            direction = Vec2(0f, -1f)
        )
        val result = controller.step(state, config, dt = 0.1f, noInput)
        assertEquals(SnakesEvent.COLLISION, result.event)
    }

    // -------------------------------------------------------------------------
    // step — self collision
    // -------------------------------------------------------------------------

    @Test
    fun step_headOverlapsTailSegment_returnsCollisionEvent() {
        // Coil the snake back on itself so a segment beyond the skip window sits
        // under the head. Spacing (0.025) stays within segmentLength so chainFollow
        // preserves the curve; segment[5] ends up ~0.015 from the head (< selfCollisionRadius).
        val segments = listOf(
            Vec2(0.500f, 0.500f), // head
            Vec2(0.475f, 0.500f),
            Vec2(0.450f, 0.500f),
            Vec2(0.450f, 0.515f),
            Vec2(0.475f, 0.515f),
            Vec2(0.500f, 0.515f), // index 5 — directly below the head
            Vec2(0.525f, 0.515f),
            Vec2(0.525f, 0.500f)
        )
        val state = freshState().copy(segments = segments, direction = Vec2(1f, 0f))
        val result = controller.step(state, config, dt = 0.001f, noInput)
        assertEquals(SnakesEvent.COLLISION, result.event)
    }

    @Test
    fun step_ghostMode_ignoresSelfCollision() {
        val segments = (0..20).map { i ->
            if (i == 0) Vec2(0.5f, 0.5f)
            else Vec2(0.5f, 0.5f + i * config.segmentLength)
        }
        val state = freshState().copy(segments = segments, direction = Vec2(0f, 1f), ghostMode = true)
        val result = controller.step(state, config, dt = 0.001f, noInput)
        assertFalse(result.event == SnakesEvent.COLLISION)
    }

    // -------------------------------------------------------------------------
    // step — food
    // -------------------------------------------------------------------------

    @Test
    fun step_headOnStandardFood_returnsFoodEatenEvent() {
        val headPos = Vec2(0.5f, 0.5f)
        val food = FoodItem(1, headPos, FoodType.STANDARD, -1f)
        val state = freshState().copy(
            segments = listOf(headPos) + freshState().segments.drop(1),
            direction = Vec2(0f, 1f),
            foods = listOf(food)
        )
        val result = controller.step(state, config, dt = 0.001f, noInput)
        assertEquals(SnakesEvent.FOOD_EATEN, result.event)
    }

    @Test
    fun step_standardFoodEaten_increasesScoreByTen() {
        val headPos = Vec2(0.5f, 0.5f)
        val food = FoodItem(1, headPos, FoodType.STANDARD, -1f)
        val state = freshState().copy(
            segments = listOf(headPos) + freshState().segments.drop(1),
            direction = Vec2(0f, 1f),
            foods = listOf(food),
            score = 0
        )
        val result = controller.step(state, config, dt = 0.001f, noInput)
        assertEquals(10, result.state.score)
    }

    @Test
    fun step_standardFoodEaten_growsSnakeByOne() {
        val headPos = Vec2(0.5f, 0.5f)
        val food = FoodItem(1, headPos, FoodType.STANDARD, -1f)
        val state = freshState().copy(
            segments = listOf(headPos) + freshState().segments.drop(1),
            direction = Vec2(0f, 1f),
            foods = listOf(food)
        )
        val before = state.segments.size
        val result = controller.step(state, config, dt = 0.001f, noInput)
        assertEquals(before + 1, result.state.segments.size)
    }

    @Test
    fun step_bonusFoodEaten_increasesScoreByFifty() {
        val headPos = Vec2(0.5f, 0.5f)
        val food = FoodItem(1, headPos, FoodType.BONUS, 5f)
        val state = freshState().copy(
            segments = listOf(headPos) + freshState().segments.drop(1),
            direction = Vec2(0f, 1f),
            foods = listOf(food),
            score = 0
        )
        val result = controller.step(state, config, dt = 0.001f, noInput)
        assertEquals(50, result.state.score)
    }

    @Test
    fun step_bonusFoodEaten_growsSnakeByThree() {
        val headPos = Vec2(0.5f, 0.5f)
        val food = FoodItem(1, headPos, FoodType.BONUS, 5f)
        val state = freshState().copy(
            segments = listOf(headPos) + freshState().segments.drop(1),
            direction = Vec2(0f, 1f),
            foods = listOf(food)
        )
        val before = state.segments.size
        val result = controller.step(state, config, dt = 0.001f, noInput)
        assertEquals(before + 3, result.state.segments.size)
    }

    @Test
    fun step_shrinkFoodEaten_increasesScoreByHundred() {
        val headPos = Vec2(0.5f, 0.5f)
        val food = FoodItem(1, headPos, FoodType.SHRINK, -1f)
        val longSegments = (0 until 10).map { i -> Vec2(headPos.x - i * config.segmentLength, headPos.y) }
        val state = freshState().copy(
            segments = longSegments,
            direction = Vec2(0f, 1f),
            foods = listOf(food),
            score = 0
        )
        val result = controller.step(state, config, dt = 0.001f, noInput)
        assertEquals(100, result.state.score)
    }

    @Test
    fun step_shrinkFoodEaten_reducesLength() {
        val headPos = Vec2(0.5f, 0.5f)
        val food = FoodItem(1, headPos, FoodType.SHRINK, -1f)
        val longSegments = (0 until 10).map { i -> Vec2(headPos.x - i * config.segmentLength, headPos.y) }
        val state = freshState().copy(
            segments = longSegments,
            direction = Vec2(0f, 1f),
            foods = listOf(food)
        )
        val before = state.segments.size
        val result = controller.step(state, config, dt = 0.001f, noInput)
        assertTrue(result.state.segments.size < before)
    }

    @Test
    fun step_shrinkFood_neverReducesBelowMinimum() {
        val headPos = Vec2(0.5f, 0.5f)
        val food = FoodItem(1, headPos, FoodType.SHRINK, -1f)
        val minSegments = (0 until 5).map { i -> Vec2(headPos.x - i * config.segmentLength, headPos.y) }
        val state = freshState().copy(
            segments = minSegments,
            direction = Vec2(0f, 1f),
            foods = listOf(food)
        )
        val result = controller.step(state, config, dt = 0.001f, noInput)
        assertTrue(result.state.segments.size >= 5)
    }

    @Test
    fun step_speedBoostFoodEaten_setsSpeedMultiplier() {
        val headPos = Vec2(0.5f, 0.5f)
        val food = FoodItem(1, headPos, FoodType.SPEED_BOOST, -1f)
        val state = freshState().copy(
            segments = listOf(headPos) + freshState().segments.drop(1),
            direction = Vec2(0f, 1f),
            foods = listOf(food)
        )
        val result = controller.step(state, config, dt = 0.001f, noInput)
        assertEquals(config.speedBoostMultiplier, result.state.speedMultiplier, 1e-4f)
    }

    @Test
    fun step_ghostFoodEaten_enablesGhostMode() {
        val headPos = Vec2(0.5f, 0.5f)
        val food = FoodItem(1, headPos, FoodType.GHOST, -1f)
        val state = freshState().copy(
            segments = listOf(headPos) + freshState().segments.drop(1),
            direction = Vec2(0f, 1f),
            foods = listOf(food)
        )
        val result = controller.step(state, config, dt = 0.001f, noInput)
        assertTrue(result.state.ghostMode)
    }

    // -------------------------------------------------------------------------
    // step — tap input / steering
    // -------------------------------------------------------------------------

    @Test
    fun step_tapInput_updatesTarget() {
        val state = freshState()
        val target = Vec2(0.8f, 0.8f)
        val result = controller.step(state, config, dt = 0.016f, SnakesInput(tapTarget = target))
        assertEquals(target, result.state.target)
    }

    @Test
    fun step_tapInput_setsTapMarkerPosition() {
        val state = freshState()
        val target = Vec2(0.7f, 0.3f)
        val result = controller.step(state, config, dt = 0.016f, SnakesInput(tapTarget = target))
        assertEquals(target, result.state.tapMarkerPos)
    }

    @Test
    fun step_noTap_markerTimerDecays() {
        val state = freshState().copy(tapMarkerTimer = 0.5f)
        val result = controller.step(state.copy(foods = listOf(farAwayFood(0))), config, dt = 0.1f, noInput)
        assertTrue(result.state.tapMarkerTimer < 0.5f)
    }

    // -------------------------------------------------------------------------
    // steer helper
    // -------------------------------------------------------------------------

    @Test
    fun steer_noTarget_returnsSameDirection() {
        val dir = Vec2(1f, 0f)
        val result = controller.steer(dir, Vec2(0.5f, 0.5f), null, config, dt = 0.016f)
        assertEquals(dir.x, result.x, 1e-4f)
        assertEquals(dir.y, result.y, 1e-4f)
    }

    @Test
    fun steer_targetAhead_noRotation() {
        val dir = Vec2(1f, 0f)
        val head = Vec2(0.3f, 0.5f)
        val target = Vec2(0.9f, 0.5f)  // directly ahead
        val result = controller.steer(dir, head, target, config, dt = 0.1f)
        assertEquals(dir.x, result.x, 1e-3f)
        assertEquals(dir.y, result.y, 1e-3f)
    }

    @Test
    fun steer_targetToRight_turnsClockwise() {
        val dir = Vec2(1f, 0f)           // facing right
        val head = Vec2(0.5f, 0.5f)
        val target = Vec2(0.5f, 0.8f)  // below (clockwise turn needed)
        val result = controller.steer(dir, head, target, config, dt = 0.1f)
        assertTrue(result.y > 0f)  // y-component grew (turning down)
    }

    @Test
    fun steer_largeAngle_clampedToMaxTurnRate() {
        // Config with small max turn rate to make clamping obvious.
        val slowConfig = config.copy(maxTurnRate = 0.1f)
        val dir = Vec2(1f, 0f)
        val head = Vec2(0.5f, 0.5f)
        val target = Vec2(0.5f, 0.1f)  // requires nearly 90° turn
        val result = controller.steer(dir, head, target, slowConfig, dt = 0.016f)
        // Should rotate by at most maxTurnRate * dt = 0.1 * 0.016 = 0.0016 radians.
        val angleTurned = Math.atan2(result.y.toDouble(), result.x.toDouble()) -
            Math.atan2(dir.y.toDouble(), dir.x.toDouble())
        assertTrue(Math.abs(angleTurned) <= 0.1 * 0.016 + 1e-4)
    }

    // -------------------------------------------------------------------------
    // chainFollow helper
    // -------------------------------------------------------------------------

    @Test
    fun chainFollow_allSegmentsWithinMaxDistance() {
        val newHead = Vec2(0.6f, 0.5f)
        val segments = (0 until 5).map { i -> Vec2(0.5f - i * 0.03f, 0.5f) }
        val result = controller.chainFollow(newHead, segments, 0.03f)
        for (i in 1 until result.size) {
            val dist = result[i].distanceTo(result[i - 1])
            assertTrue(dist <= 0.03f + 1e-4f)
        }
    }

    @Test
    fun chainFollow_firstElementIsNewHead() {
        val newHead = Vec2(0.7f, 0.4f)
        val segments = listOf(Vec2(0.5f, 0.5f), Vec2(0.47f, 0.5f))
        val result = controller.chainFollow(newHead, segments, 0.03f)
        assertEquals(newHead, result.first())
    }

    @Test
    fun chainFollow_preservesSegmentCount() {
        val newHead = Vec2(0.6f, 0.5f)
        val segments = (0 until 8).map { i -> Vec2(0.5f - i * 0.03f, 0.5f) }
        val result = controller.chainFollow(newHead, segments, 0.03f)
        assertEquals(segments.size, result.size)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun farAwayFood(id: Int) = FoodItem(id, Vec2(0.9f, 0.9f), FoodType.STANDARD, -1f)
}
