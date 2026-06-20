package com.xanticious.androidgames.games.pong

import com.xanticious.androidgames.controller.games.pong.PongController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.pong.PongEvent
import com.xanticious.androidgames.model.games.pong.PongInput
import com.xanticious.androidgames.model.games.pong.PongState
import com.xanticious.androidgames.model.games.pong.Stance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PongControllerTest {
    private val controller = PongController()
    private val config = controller.configFor(GameDifficulty.MEDIUM)

    private fun input(target: Float? = null) = PongInput(targetBatY = target, swingRequested = false, stance = Stance.FOREHAND)

    @Test
    fun serve_towardPlayer_movesBallRight() {
        val served = controller.serve(PongState.initial(), config, towardPlayer = true, verticalBias = 0f)
        assertTrue(served.ballVelocity.x > 0f)
    }

    @Test
    fun serve_setsBallSpeedMagnitude() {
        val served = controller.serve(PongState.initial(), config, towardPlayer = false, verticalBias = 0.2f)
        assertEquals(config.ballSpeed, served.ballVelocity.length, 1e-3f)
    }

    @Test
    fun step_bouncesOffTopWall() {
        val state = PongState.initial().copy(ball = Vec2(0.5f, 0.02f), ballVelocity = Vec2(0.1f, -0.5f))
        val result = controller.step(state, config, dt = 0.1f, input = input())
        assertTrue(result.state.ballVelocity.y > 0f)
    }

    @Test
    fun step_playerBatReflectsBallAndCountsRally() {
        val state = PongState.initial().copy(
            ball = Vec2(0.9f, 0.5f),
            ballVelocity = Vec2(0.5f, 0f),
            playerBatY = 0.5f
        )
        val result = controller.step(state, config, dt = 0.1f, input = input(0.5f))
        assertTrue("ball should travel left after bat hit", result.state.ballVelocity.x < 0f)
        assertEquals(1, result.state.rally)
    }

    @Test
    fun step_ballPastPlayerWall_scoresForAi() {
        val state = PongState.initial().copy(ball = Vec2(0.99f, 0.5f), ballVelocity = Vec2(0.5f, 0f), playerBatY = 0.0f)
        val result = controller.step(state, config, dt = 0.1f, input = input(0.0f))
        assertEquals(PongEvent.POINT_AI, result.event)
        assertEquals(1, result.state.aiScore)
    }

    @Test
    fun step_ballPastAiWall_scoresForPlayer() {
        val state = PongState.initial().copy(ball = Vec2(0.01f, 0.5f), ballVelocity = Vec2(-0.5f, 0f), aiBatY = 0.0f)
        val result = controller.step(state, config, dt = 0.1f, input = input())
        assertEquals(PongEvent.POINT_PLAYER, result.event)
        assertEquals(1, result.state.playerScore)
    }
}
