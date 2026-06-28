package com.xanticious.androidgames.model.games.endlessrunner

/**
 * Immutable model for the Endless Runner game. All positions are normalised to
 * a [0, 1] coordinate space (x grows right, y grows downward). The view scales
 * to pixels. Zero Android or Compose imports.
 */

/** Kinds of obstacles the ninja must react to. */
enum class ObstacleKind {
    /** Bamboo-spike cluster on the ground — must jump over. */
    GROUND,
    /** Banner / noren hanging from above — must slide under. */
    OVERHEAD
}

/** A single obstacle in the world (leading-edge x, scrolls left). */
data class Obstacle(
    val x: Float,
    val kind: ObstacleKind
)

/** Per-difficulty tuning values for the runner. */
data class EndlessRunnerConfig(
    /** Initial world-scroll speed (normalised units / second). */
    val initialSpeed: Float,
    /** Speed increase per second of play (auto-escalation). */
    val acceleration: Float,
    /** Maximum scroll speed cap. */
    val maxSpeed: Float,
    /** Base seconds between obstacle spawns at initial speed; decreases as speed rises. */
    val spawnIntervalBase: Float,
    /** Downward gravitational acceleration (normalised units / s²). */
    val gravity: Float,
    /** Initial upward velocity applied on a jump (normalised units / s). */
    val jumpVelocity: Float,
    /** How long a slide lasts when the player holds down (seconds). */
    val slideMaxDuration: Float = 0.6f
)

/** Events emitted by a single physics step. */
sealed interface EndlessRunnerEvent {
    data object None : EndlessRunnerEvent
    /** The ninja struck an obstacle with no shield active — run ends. */
    data object Died : EndlessRunnerEvent
    /** The ninja struck an obstacle but the Ki Shield absorbed the hit. */
    data object ShieldConsumed : EndlessRunnerEvent
}

/** Result of one [com.xanticious.androidgames.controller.games.endlessrunner.EndlessRunnerController.step] call. */
data class EndlessRunnerStep(
    val state: EndlessRunnerState,
    val event: EndlessRunnerEvent
)

/** Per-frame input fed into the controller. */
data class EndlessRunnerInput(
    /** True on the single frame a tap/jump was detected. */
    val jumpPressed: Boolean,
    /** True for the duration of a hold/swipe-down slide gesture. */
    val slideActive: Boolean
)

/**
 * Full immutable runner game state. The board is landscape; the ninja always
 * runs rightward at [RUNNER_X] (fixed horizontal position) while the world
 * scrolls left.
 */
data class EndlessRunnerState(
    /** Y position of the ninja's feet (ground = [GROUND_Y]). */
    val runnerY: Float,
    /** Vertical velocity (positive = moving downward). */
    val runnerVelY: Float,
    /** 0 = grounded, 1 = single-jump airborne, 2 = double-jumped. */
    val jumpCount: Int,
    /** Whether the ninja is currently in a slide posture. */
    val isSliding: Boolean,
    /** Active obstacles in the world, ordered by spawn time. */
    val obstacles: List<Obstacle>,
    /** Current world-scroll speed (may exceed [EndlessRunnerConfig.initialSpeed]). */
    val speed: Float,
    /** Total meters run (distance == score). */
    val distance: Float,
    /** Seconds elapsed since the last obstacle was spawned. */
    val timeSinceLastSpawn: Float,
    /** True when the Ki Shield power-up is protecting the ninja. */
    val hasShield: Boolean,
    /** False once the ninja has died; step() becomes a no-op. */
    val isAlive: Boolean
) {
    /** True while the ninja's feet are on the ground. */
    val isGrounded: Boolean
        get() = runnerY >= GROUND_Y - GROUNDED_EPSILON

    /** Current ninja body height (shrinks while sliding). */
    val runnerHeight: Float
        get() = if (isSliding) RUNNER_HEIGHT_SLIDE else RUNNER_HEIGHT_NORMAL

    /** Y coordinate of the top of the ninja's bounding box. */
    val runnerTop: Float
        get() = runnerY - runnerHeight

    companion object {
        /** Y of the ground surface (feet rest here). */
        const val GROUND_Y = 0.75f

        /** Full standing body height. */
        const val RUNNER_HEIGHT_NORMAL = 0.18f

        /** Lowered slide body height. */
        const val RUNNER_HEIGHT_SLIDE = 0.09f

        /** Half-width of the ninja's horizontal hitbox. */
        const val RUNNER_HALF_WIDTH = 0.04f

        /** Fixed horizontal position of the ninja on the board. */
        const val RUNNER_X = 0.2f

        /**
         * Y position of the bottom edge of an overhead obstacle.
         * Standing ninja head (y ≈ 0.57) is above this → collision.
         * Sliding ninja head (y ≈ 0.66) is below this → clear.
         */
        const val OVERHEAD_HANG_BOTTOM = 0.63f

        /** Height of a ground-spike obstacle (top = [GROUND_Y] − height). */
        const val GROUND_OBS_HEIGHT = 0.15f

        /** Half-width of any obstacle's horizontal hitbox. */
        const val OBS_HALF_WIDTH = 0.04f

        /** How close to ground the ninja must be to count as "grounded". */
        private const val GROUNDED_EPSILON = 0.002f

        fun initial(speed: Float): EndlessRunnerState = EndlessRunnerState(
            runnerY = GROUND_Y,
            runnerVelY = 0f,
            jumpCount = 0,
            isSliding = false,
            obstacles = emptyList(),
            speed = speed,
            distance = 0f,
            timeSinceLastSpawn = 0f,
            hasShield = false,
            isAlive = true
        )
    }
}
