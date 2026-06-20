package com.xanticious.androidgames.model.games.pong

import com.xanticious.androidgames.model.Vec2

/**
 * Immutable Pong rally/match state. All positions use a normalized court where
 * x and y are in `[0, 1]`; the view scales to pixels. The court is landscape:
 * the AI defends the left wall (x = 0) and the player defends the right wall
 * (x = 1). Walls at y = 0 (top) and y = 1 (bottom) reflect the ball.
 */
enum class Stance { FOREHAND, BACKHAND }

data class PongState(
    val ball: Vec2,
    val ballVelocity: Vec2,
    val playerBatY: Float,
    val aiBatY: Float,
    val playerStance: Stance,
    val playerCooldown: Float,
    val aiScore: Int,
    val playerScore: Int,
    val playerSets: Int,
    val aiSets: Int,
    val rally: Int,
    val longestRally: Int
) {
    companion object {
        fun initial(): PongState = PongState(
            ball = Vec2(0.5f, 0.5f),
            ballVelocity = Vec2.ZERO,
            playerBatY = 0.5f,
            aiBatY = 0.5f,
            playerStance = Stance.FOREHAND,
            playerCooldown = 0f,
            aiScore = 0,
            playerScore = 0,
            playerSets = 0,
            aiSets = 0,
            rally = 0,
            longestRally = 0
        )
    }
}

/** Tuning values derived from the selected difficulty. */
data class PongConfig(
    val ballSpeed: Float,
    val ballSpeedGain: Float,
    val batHalfHeight: Float,
    val aiBatSpeed: Float,
    val aiTracking: Float,
    val swingCooldown: Float = 0.8f,
    val pointsToWin: Int = 11,
    val setsToWin: Int = 2
)

/** Outcome of a single physics step. */
enum class PongEvent { NONE, POINT_PLAYER, POINT_AI }

data class PongStep(val state: PongState, val event: PongEvent)

/** Per-frame player input fed into the controller. */
data class PongInput(
    val targetBatY: Float?,
    val swingRequested: Boolean,
    val stance: Stance
)
