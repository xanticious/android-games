package com.xanticious.androidgames.controller.games.pong

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.pong.PongConfig
import com.xanticious.androidgames.model.games.pong.PongEvent
import com.xanticious.androidgames.model.games.pong.PongInput
import com.xanticious.androidgames.model.games.pong.PongState
import com.xanticious.androidgames.model.games.pong.PongStep
import com.xanticious.androidgames.model.games.pong.Stance
import kotlin.math.abs

/**
 * Pure Pong rules: ball physics, bat collisions, AI tracking and scoring.
 * No Android or Compose imports so the whole rule set is JVM unit-testable.
 */
class PongController {

    private val playerBatX = 0.92f
    private val aiBatX = 0.08f

    fun configFor(difficulty: GameDifficulty): PongConfig = when (difficulty) {
        GameDifficulty.EASY -> PongConfig(
            ballSpeed = 0.55f, ballSpeedGain = 1.03f, batHalfHeight = 0.14f,
            aiBatSpeed = 0.6f, aiTracking = 0.55f
        )
        GameDifficulty.MEDIUM -> PongConfig(
            ballSpeed = 0.7f, ballSpeedGain = 1.05f, batHalfHeight = 0.11f,
            aiBatSpeed = 0.9f, aiTracking = 0.8f
        )
        GameDifficulty.HARD -> PongConfig(
            ballSpeed = 0.9f, ballSpeedGain = 1.06f, batHalfHeight = 0.09f,
            aiBatSpeed = 1.25f, aiTracking = 0.95f
        )
    }

    /** Builds a fresh rally with the ball served toward the receiver. */
    fun serve(state: PongState, config: PongConfig, towardPlayer: Boolean, verticalBias: Float): PongState {
        val dirX = if (towardPlayer) 1f else -1f
        val velocity = Vec2(dirX, verticalBias.coerceIn(-0.6f, 0.6f)).normalized() * config.ballSpeed
        return state.copy(
            ball = Vec2(0.5f, 0.5f),
            ballVelocity = velocity,
            playerCooldown = 0f,
            rally = 0
        )
    }

    /** Advances the rally by [dt] seconds. */
    fun step(state: PongState, config: PongConfig, dt: Float, input: PongInput): PongStep {
        var s = state

        // Player bat: move toward tap target, decay swing cooldown.
        val target = input.targetBatY?.coerceIn(0f, 1f) ?: s.playerBatY
        val playerBatY = approach(s.playerBatY, target, config.aiBatSpeed * 1.6f * dt)
        val cooldown = (s.playerCooldown - dt).coerceAtLeast(0f)
        s = s.copy(playerBatY = playerBatY, playerStance = input.stance, playerCooldown = cooldown)

        // AI bat: track the ball with difficulty-scaled responsiveness.
        val aiTarget = s.aiBatY + (s.ball.y - s.aiBatY) * config.aiTracking
        val aiBatY = approach(s.aiBatY, aiTarget, config.aiBatSpeed * dt)
        s = s.copy(aiBatY = aiBatY)

        // Integrate ball.
        var ball = s.ball + s.ballVelocity * dt
        var vel = s.ballVelocity

        // Wall bounces (top/bottom).
        if (ball.y < 0f) { ball = Vec2(ball.x, -ball.y); vel = Vec2(vel.x, -vel.y) }
        if (ball.y > 1f) { ball = Vec2(ball.x, 2f - ball.y); vel = Vec2(vel.x, -vel.y) }

        // Player bat collision (ball moving right toward x = playerBatX).
        if (vel.x > 0f && ball.x >= playerBatX && s.ball.x < playerBatX) {
            if (abs(ball.y - s.playerBatY) <= config.batHalfHeight) {
                vel = bounceOffBat(vel, ball.y, s.playerBatY, config, input.stance, towardAi = true)
                ball = Vec2(playerBatX, ball.y)
                s = registerHit(s)
            }
        }
        // AI bat collision (ball moving left toward x = aiBatX).
        if (vel.x < 0f && ball.x <= aiBatX && s.ball.x > aiBatX) {
            if (abs(ball.y - s.aiBatY) <= config.batHalfHeight) {
                vel = bounceOffBat(vel, ball.y, s.aiBatY, config, Stance.FOREHAND, towardAi = false)
                ball = Vec2(aiBatX, ball.y)
                s = registerHit(s)
            }
        }

        s = s.copy(ball = ball, ballVelocity = vel)

        // Scoring: ball escaped past a side wall.
        return when {
            ball.x > 1f -> PongStep(scorePoint(s, playerScored = false), PongEvent.POINT_AI)
            ball.x < 0f -> PongStep(scorePoint(s, playerScored = true), PongEvent.POINT_PLAYER)
            else -> PongStep(s, PongEvent.NONE)
        }
    }

    /** True when the given side has clinched the match. */
    fun matchWinner(state: PongState, config: PongConfig): Boolean =
        state.playerSets >= config.setsToWin || state.aiSets >= config.setsToWin

    private fun registerHit(s: PongState): PongState {
        val rally = s.rally + 1
        return s.copy(rally = rally, longestRally = maxOf(s.longestRally, rally), playerCooldown = 0f)
    }

    private fun bounceOffBat(
        vel: Vec2,
        ballY: Float,
        batY: Float,
        config: PongConfig,
        stance: Stance,
        towardAi: Boolean
    ): Vec2 {
        val offset = ((ballY - batY) / config.batHalfHeight).coerceIn(-1f, 1f)
        val stanceBias = if (towardAi) {
            if (stance == Stance.FOREHAND) -0.25f else 0.25f
        } else 0f
        val newDirX = if (towardAi) -1f else 1f
        val speed = (vel.length * config.ballSpeedGain)
        return Vec2(newDirX, (offset * 0.8f + stanceBias)).normalized() * speed
    }

    private fun scorePoint(s: PongState, playerScored: Boolean): PongState {
        val playerScore = if (playerScored) s.playerScore + 1 else s.playerScore
        val aiScore = if (playerScored) s.aiScore else s.aiScore + 1
        // Resolve set wins.
        return if (playerScore >= 11 && playerScore - aiScore >= 2) {
            s.copy(playerScore = 0, aiScore = 0, playerSets = s.playerSets + 1, rally = 0)
        } else if (aiScore >= 11 && aiScore - playerScore >= 2) {
            s.copy(playerScore = 0, aiScore = 0, aiSets = s.aiSets + 1, rally = 0)
        } else {
            s.copy(playerScore = playerScore, aiScore = aiScore, rally = 0)
        }
    }

    private fun approach(current: Float, target: Float, maxStep: Float): Float {
        val delta = target - current
        return when {
            abs(delta) <= maxStep -> target
            delta > 0 -> current + maxStep
            else -> current - maxStep
        }
    }
}
