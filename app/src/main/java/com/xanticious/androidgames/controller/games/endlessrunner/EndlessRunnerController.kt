package com.xanticious.androidgames.controller.games.endlessrunner

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.endlessrunner.EndlessRunnerConfig
import com.xanticious.androidgames.model.games.endlessrunner.EndlessRunnerEvent
import com.xanticious.androidgames.model.games.endlessrunner.EndlessRunnerInput
import com.xanticious.androidgames.model.games.endlessrunner.EndlessRunnerState
import com.xanticious.androidgames.model.games.endlessrunner.EndlessRunnerStep
import com.xanticious.androidgames.model.games.endlessrunner.Obstacle
import com.xanticious.androidgames.model.games.endlessrunner.ObstacleKind
import kotlin.random.Random

/**
 * Pure endless-runner physics and rules. No Android or Compose imports — fully
 * JVM unit-testable.
 *
 * Coordinate convention (matches the normalised board in [EndlessRunnerState]):
 *   x grows rightward, y grows downward, all values in [0, 1] on the court.
 *
 * The ninja sits at a fixed x ([EndlessRunnerState.RUNNER_X]) and the world
 * scrolls leftward. Vertical physics: upward jump applies a negative initial
 * velocity; gravity pulls the runner back down to [EndlessRunnerState.GROUND_Y].
 */
class EndlessRunnerController {

    fun configFor(difficulty: GameDifficulty): EndlessRunnerConfig = when (difficulty) {
        GameDifficulty.EASY -> EndlessRunnerConfig(
            initialSpeed = 0.30f,
            acceleration = 0.008f,
            maxSpeed = 0.80f,
            spawnIntervalBase = 2.8f,
            gravity = 2.0f,
            jumpVelocity = 0.90f
        )
        GameDifficulty.MEDIUM -> EndlessRunnerConfig(
            initialSpeed = 0.40f,
            acceleration = 0.012f,
            maxSpeed = 1.00f,
            spawnIntervalBase = 2.2f,
            gravity = 2.2f,
            jumpVelocity = 0.95f
        )
        GameDifficulty.HARD -> EndlessRunnerConfig(
            initialSpeed = 0.55f,
            acceleration = 0.020f,
            maxSpeed = 1.30f,
            spawnIntervalBase = 1.6f,
            gravity = 2.5f,
            jumpVelocity = 1.00f
        )
    }

    /**
     * Advances the game by [dt] seconds given [input].
     *
     * Step order (each stage receives the result of the previous):
     * 1. Speed escalation + distance accumulation.
     * 2. Slide activation / deactivation (grounded only).
     * 3. Jump input — may cancel slide.
     * 4. Gravity integration + ground-landing clamp.
     * 5. Obstacle scrolling and culling.
     * 6. Obstacle spawning.
     * 7. Collision detection → Died / ShieldConsumed / None.
     */
    fun step(
        state: EndlessRunnerState,
        config: EndlessRunnerConfig,
        dt: Float,
        input: EndlessRunnerInput,
        random: Random = Random.Default
    ): EndlessRunnerStep {
        if (!state.isAlive) return EndlessRunnerStep(state, EndlessRunnerEvent.None)

        var s = state

        // 1. Speed escalation and distance.
        val newSpeed = (s.speed + config.acceleration * dt).coerceAtMost(config.maxSpeed)
        s = s.copy(
            speed = newSpeed,
            distance = s.distance + newSpeed * dt * METERS_PER_UNIT
        )

        // 2. Slide (grounded only; jump in next step may override).
        s = if (input.slideActive && s.isGrounded) {
            s.copy(isSliding = true)
        } else {
            s.copy(isSliding = false)
        }

        // 3. Jump (single then double; cancels slide).
        if (input.jumpPressed) {
            s = applyJump(s, config)
        }

        // 4. Gravity + integration.
        val newVelY = s.runnerVelY + config.gravity * dt
        val newY = (s.runnerY + newVelY * dt).coerceAtLeast(0f)
        s = s.copy(runnerVelY = newVelY, runnerY = newY)

        // Land on ground.
        if (s.runnerY >= EndlessRunnerState.GROUND_Y) {
            s = s.copy(runnerY = EndlessRunnerState.GROUND_Y, runnerVelY = 0f, jumpCount = 0)
        }

        // 5. Scroll obstacles leftward and cull those past the left edge.
        val scrolled = s.obstacles
            .map { it.copy(x = it.x - newSpeed * dt) }
            .filter { it.x > -0.25f }
        s = s.copy(obstacles = scrolled)

        // 6. Spawn obstacle when the spawn timer fires.
        val spawnInterval = config.spawnIntervalBase /
            (newSpeed / config.initialSpeed).coerceAtLeast(1f)
        val newTimeSinceSpawn = s.timeSinceLastSpawn + dt
        if (newTimeSinceSpawn >= spawnInterval) {
            val kind = if (random.nextFloat() < 0.5f) ObstacleKind.GROUND else ObstacleKind.OVERHEAD
            s = s.copy(
                obstacles = s.obstacles + Obstacle(x = SPAWN_X, kind = kind),
                timeSinceLastSpawn = 0f
            )
        } else {
            s = s.copy(timeSinceLastSpawn = newTimeSinceSpawn)
        }

        // 7. Collision detection.
        val hitObs = s.obstacles.firstOrNull { obs -> collides(s, obs) }
        if (hitObs != null) {
            return if (s.hasShield) {
                val cleared = s.obstacles.filter { !collides(s, it) }
                EndlessRunnerStep(
                    s.copy(hasShield = false, obstacles = cleared),
                    EndlessRunnerEvent.ShieldConsumed
                )
            } else {
                EndlessRunnerStep(s.copy(isAlive = false), EndlessRunnerEvent.Died)
            }
        }

        return EndlessRunnerStep(s, EndlessRunnerEvent.None)
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun applyJump(s: EndlessRunnerState, config: EndlessRunnerConfig): EndlessRunnerState =
        when {
            s.isGrounded && s.jumpCount == 0 -> s.copy(
                runnerVelY = -config.jumpVelocity,
                jumpCount = 1,
                isSliding = false
            )
            !s.isGrounded && s.jumpCount == 1 -> s.copy(
                runnerVelY = -config.jumpVelocity,
                jumpCount = 2
            )
            else -> s
        }

    /**
     * Returns true when the ninja's AABB overlaps [obs]'s AABB.
     *
     * Ground obstacle:  occupies [groundY − height, groundY] — ninja must jump
     *                   so that its bottom clears the obstacle top.
     * Overhead obstacle: hangs from y = 0 down to [OVERHEAD_HANG_BOTTOM] —
     *                    ninja must slide so that its head clears the hang bottom.
     */
    private fun collides(s: EndlessRunnerState, obs: Obstacle): Boolean {
        val runnerLeft = EndlessRunnerState.RUNNER_X - EndlessRunnerState.RUNNER_HALF_WIDTH
        val runnerRight = EndlessRunnerState.RUNNER_X + EndlessRunnerState.RUNNER_HALF_WIDTH
        val obsLeft = obs.x - EndlessRunnerState.OBS_HALF_WIDTH
        val obsRight = obs.x + EndlessRunnerState.OBS_HALF_WIDTH

        if (runnerRight <= obsLeft || runnerLeft >= obsRight) return false

        val runnerTop = s.runnerTop
        val runnerBottom = s.runnerY

        return when (obs.kind) {
            ObstacleKind.GROUND -> {
                val obsTop = EndlessRunnerState.GROUND_Y - EndlessRunnerState.GROUND_OBS_HEIGHT
                runnerBottom > obsTop && runnerTop < EndlessRunnerState.GROUND_Y
            }
            ObstacleKind.OVERHEAD -> {
                runnerTop < EndlessRunnerState.OVERHEAD_HANG_BOTTOM
            }
        }
    }

    companion object {
        /** World-unit-to-meters conversion for distance scoring. */
        const val METERS_PER_UNIT = 10f

        /** X position at which new obstacles are spawned (just off-screen right). */
        const val SPAWN_X = 1.2f
    }
}
