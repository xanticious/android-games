package com.xanticious.androidgames.games.endlessrunner

import com.xanticious.androidgames.controller.games.endlessrunner.EndlessRunnerController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.endlessrunner.EndlessRunnerEvent
import com.xanticious.androidgames.model.games.endlessrunner.EndlessRunnerInput
import com.xanticious.androidgames.model.games.endlessrunner.EndlessRunnerState
import com.xanticious.androidgames.model.games.endlessrunner.Obstacle
import com.xanticious.androidgames.model.games.endlessrunner.ObstacleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EndlessRunnerControllerTest {

    private val controller = EndlessRunnerController()
    private val easyConfig = controller.configFor(GameDifficulty.EASY)
    private val medConfig = controller.configFor(GameDifficulty.MEDIUM)
    private val hardConfig = controller.configFor(GameDifficulty.HARD)

    private fun noInput() = EndlessRunnerInput(jumpPressed = false, slideActive = false)
    private fun jumpInput() = EndlessRunnerInput(jumpPressed = true, slideActive = false)
    private fun slideInput() = EndlessRunnerInput(jumpPressed = false, slideActive = true)

    // --- Difficulty scaling ---------------------------------------------------

    @Test
    fun configFor_easyHasLowerInitialSpeedThanHard() {
        assertTrue(easyConfig.initialSpeed < hardConfig.initialSpeed)
    }

    @Test
    fun configFor_hardHasHigherMaxSpeedThanEasy() {
        assertTrue(hardConfig.maxSpeed > easyConfig.maxSpeed)
    }

    // --- Jump / gravity physics -----------------------------------------------

    @Test
    fun jump_whenGrounded_movesRunnerUpward() {
        val initial = EndlessRunnerState.initial(easyConfig.initialSpeed)
        val result = controller.step(initial, easyConfig, dt = 0.016f, jumpInput())
        assertTrue("runner should move upward on jump", result.state.runnerY < EndlessRunnerState.GROUND_Y)
    }

    @Test
    fun jump_whenGrounded_setsJumpCountToOne() {
        val initial = EndlessRunnerState.initial(easyConfig.initialSpeed)
        val result = controller.step(initial, easyConfig, dt = 0.016f, jumpInput())
        assertEquals(1, result.state.jumpCount)
    }

    @Test
    fun gravity_pullsAirborneRunnerTowardGround() {
        // Place runner at peak of jump (zero vertical velocity, above ground).
        var state = EndlessRunnerState.initial(easyConfig.initialSpeed).copy(
            runnerY = 0.50f,
            runnerVelY = 0f,
            jumpCount = 1
        )
        // Simulate ~1 s (60 × 16 ms frames) — runner should land.
        repeat(60) {
            state = controller.step(state, easyConfig, dt = 0.016f, noInput()).state
        }
        assertTrue("runner should land on ground after airborne peak", state.isGrounded)
    }

    @Test
    fun doubleJump_secondTapWhileAirborne_appliesUpwardVelocity() {
        val initial = EndlessRunnerState.initial(medConfig.initialSpeed)
        val step1 = controller.step(initial, medConfig, dt = 0.016f, jumpInput())
        // Runner is now airborne with jumpCount == 1.
        assertEquals(1, step1.state.jumpCount)
        assertFalse(step1.state.isGrounded)

        val step2 = controller.step(step1.state, medConfig, dt = 0.016f, jumpInput())
        assertEquals(2, step2.state.jumpCount)
        // Without a double-jump, gravity would decelerate the runner: velY += gravity*dt.
        // With a double-jump, velocity is reset to -jumpVelocity before gravity, so the
        // stored velY should be more negative than the natural-decay result.
        val naturalVelY = step1.state.runnerVelY + medConfig.gravity * 0.016f
        assertTrue("double-jump velocity should be more upward than natural deceleration",
            step2.state.runnerVelY < naturalVelY)
    }

    @Test
    fun thirdJump_whileAirborne_hasNoEffect() {
        val initial = EndlessRunnerState.initial(medConfig.initialSpeed)
        val step1 = controller.step(initial, medConfig, dt = 0.016f, jumpInput())
        val step2 = controller.step(step1.state, medConfig, dt = 0.016f, jumpInput())
        val velAfterDouble = step2.state.runnerVelY
        val step3 = controller.step(step2.state, medConfig, dt = 0.016f, jumpInput())
        // jumpCount unchanged, and no extra impulse was applied.
        assertEquals(2, step3.state.jumpCount)
        // Velocity after step3 should be velAfterDouble + gravity*dt (gravity only, no jump boost).
        val expectedVel = velAfterDouble + medConfig.gravity * 0.016f
        assertEquals(expectedVel, step3.state.runnerVelY, 1e-4f)
    }

    // --- Slide ----------------------------------------------------------------

    @Test
    fun slide_lowersNinjaHitbox() {
        val standing = EndlessRunnerState.initial(easyConfig.initialSpeed)
        val sliding = standing.copy(isSliding = true)
        assertTrue("sliding hitbox should be shorter", sliding.runnerHeight < standing.runnerHeight)
    }

    @Test
    fun slide_onGroundedRunner_setsIsSliding() {
        val initial = EndlessRunnerState.initial(easyConfig.initialSpeed)
        val result = controller.step(initial, easyConfig, dt = 0.016f, slideInput())
        assertTrue(result.state.isSliding)
    }

    // --- Collision: ground obstacle ------------------------------------------

    @Test
    fun groundObstacle_atRunnerX_killsStandingRunner() {
        val state = EndlessRunnerState.initial(easyConfig.initialSpeed).copy(
            obstacles = listOf(Obstacle(x = EndlessRunnerState.RUNNER_X, kind = ObstacleKind.GROUND))
        )
        val result = controller.step(state, easyConfig, dt = 0.016f, noInput())
        assertEquals(EndlessRunnerEvent.Died, result.event)
    }

    @Test
    fun jumpHighEnough_clearsGroundObstacle() {
        // Runner is airborne with feet clearly above the obstacle top.
        val obsTop = EndlessRunnerState.GROUND_Y - EndlessRunnerState.GROUND_OBS_HEIGHT
        val state = EndlessRunnerState.initial(easyConfig.initialSpeed).copy(
            runnerY = obsTop - 0.05f,      // feet well above obstacle top
            runnerVelY = -easyConfig.jumpVelocity,
            jumpCount = 1,
            obstacles = listOf(Obstacle(x = EndlessRunnerState.RUNNER_X, kind = ObstacleKind.GROUND))
        )
        val result = controller.step(state, easyConfig, dt = 0.016f, noInput())
        assertNotEquals(EndlessRunnerEvent.Died, result.event)
        assertTrue(result.state.isAlive)
    }

    // --- Collision: overhead obstacle -----------------------------------------

    @Test
    fun overheadObstacle_atRunnerX_killsStandingRunner() {
        val state = EndlessRunnerState.initial(easyConfig.initialSpeed).copy(
            obstacles = listOf(Obstacle(x = EndlessRunnerState.RUNNER_X, kind = ObstacleKind.OVERHEAD))
        )
        val result = controller.step(state, easyConfig, dt = 0.016f, noInput())
        assertEquals(EndlessRunnerEvent.Died, result.event)
    }

    @Test
    fun slide_passesUnderOverheadObstacle() {
        // Sliding ninja head is below OVERHEAD_HANG_BOTTOM — should pass safely.
        val state = EndlessRunnerState.initial(easyConfig.initialSpeed).copy(
            obstacles = listOf(Obstacle(x = EndlessRunnerState.RUNNER_X, kind = ObstacleKind.OVERHEAD))
        )
        val result = controller.step(state, easyConfig, dt = 0.016f, slideInput())
        assertNotEquals(EndlessRunnerEvent.Died, result.event)
        assertTrue(result.state.isAlive)
    }

    // --- Distance / scoring ---------------------------------------------------

    @Test
    fun distance_increasesEachStep() {
        val initial = EndlessRunnerState.initial(easyConfig.initialSpeed)
        val result = controller.step(initial, easyConfig, dt = 0.016f, noInput())
        assertTrue("distance should grow after a step", result.state.distance > 0f)
    }

    @Test
    fun distance_accumulatesProportionalToSpeed() {
        val initial = EndlessRunnerState.initial(easyConfig.initialSpeed)
        val dt = 1.0f
        val result = controller.step(initial, easyConfig, dt = dt, noInput(), random = Random(0))
        val expected = initial.speed * dt * EndlessRunnerController.METERS_PER_UNIT
        // Allow small delta because speed may have incremented by acceleration during the step.
        assertTrue(result.state.distance > expected * 0.9f)
    }

    // --- Shield ---------------------------------------------------------------

    @Test
    fun shield_absorbsGroundObstacleHit() {
        val state = EndlessRunnerState.initial(easyConfig.initialSpeed).copy(
            hasShield = true,
            obstacles = listOf(Obstacle(x = EndlessRunnerState.RUNNER_X, kind = ObstacleKind.GROUND))
        )
        val result = controller.step(state, easyConfig, dt = 0.016f, noInput())
        assertEquals(EndlessRunnerEvent.ShieldConsumed, result.event)
        assertFalse("shield should be consumed", result.state.hasShield)
        assertTrue("runner should survive", result.state.isAlive)
    }

    // --- Dead state -----------------------------------------------------------

    @Test
    fun stepOnDeadState_returnsNoneEventAndUnchangedState() {
        val dead = EndlessRunnerState.initial(easyConfig.initialSpeed).copy(isAlive = false)
        val result = controller.step(dead, easyConfig, dt = 0.016f, noInput())
        assertEquals(EndlessRunnerEvent.None, result.event)
        assertFalse(result.state.isAlive)
    }
}
