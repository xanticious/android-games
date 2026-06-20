package com.xanticious.androidgames.view.games.snakes2d

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.snakes2d.SnakesController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.snakes2d.FoodType
import com.xanticious.androidgames.model.games.snakes2d.PowerUpType
import com.xanticious.androidgames.model.games.snakes2d.SnakesEvent
import com.xanticious.androidgames.model.games.snakes2d.SnakesInput
import com.xanticious.androidgames.model.games.snakes2d.SnakesState
import com.xanticious.androidgames.state.games.snakes2d.SnakesPhase
import com.xanticious.androidgames.state.games.snakes2d.SnakesStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold

/**
 * Snakes 2D — tap anywhere to steer; eat food to grow; avoid walls and your own body.
 *
 * Entry point required by the game registry. Uses [GameScaffold] so victory/defeat
 * content always appears below the board, never overlaying it.
 */
@Composable
fun SnakesGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { SnakesController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { SnakesStateMachine() }
    val phase by machine.phase.collectAsState()

    var state by remember { mutableStateOf(SnakesState.initial(config)) }
    var tapTarget by remember { mutableStateOf<Vec2?>(null) }

    LaunchedEffect(Unit) {
        machine.startGame()
    }

    GameLoop(running = phase == SnakesPhase.PLAYING) { dt ->
        val step = controller.step(state, config, dt, SnakesInput(tapTarget))
        tapTarget = null
        state = step.state
        if (step.event == SnakesEvent.COLLISION) {
            machine.collision()
        }
    }

    // Capture color tokens before entering Canvas (DrawScope is not composable).
    val courtColor = GameCourt
    val lineColor = GameCourtLine.copy(alpha = 0.12f)
    val wallColor = GameCourtLine
    val playerColor = GamePlayer
    val foodColors = mapOf(
        FoodType.STANDARD to GameAccent,
        FoodType.BONUS to GameSuccess,
        FoodType.SPEED_BOOST to GamePlayer,
        FoodType.SLOW to GameNeutral,
        FoodType.SHRINK to GameHazard,
        FoodType.GHOST to Color.White.copy(alpha = 0.65f)
    )
    val markerColor = GameCourtLine

    val hudCenter = when (state.activePowerUp) {
        PowerUpType.SPEED_BOOST -> "⚡ ${state.powerUpTimer.toInt() + 1}s"
        PowerUpType.SLOW -> "🐢 ${state.powerUpTimer.toInt() + 1}s"
        PowerUpType.GHOST -> "👻 ${state.powerUpTimer.toInt() + 1}s"
        null -> "Len: ${state.length}"
    }

    GameScaffold(
        title = "Snakes 2D",
        onExit = onExit,
        hud = {
            GameHud(
                left = "Score: ${state.score}",
                center = hudCenter,
                right = "Best: ${state.bestScore}"
            )
        },
        status = {
            when (phase) {
                SnakesPhase.DEAD -> {
                    DefeatPanel(
                        score = state.score,
                        bestScore = state.bestScore,
                        headline = "Game Over",
                        onTryAgain = {
                            val savedBest = state.bestScore
                            state = SnakesState.initial(config, bestScore = savedBest)
                            tapTarget = null
                            machine.restart()
                        },
                        onMenu = onExit
                    )
                }
                SnakesPhase.PLAYING -> {
                    Text(
                        text = "Tap anywhere to steer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                SnakesPhase.IDLE -> {}
            }
        }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (phase == SnakesPhase.PLAYING) {
                            tapTarget = Vec2(
                                offset.x / size.width.toFloat(),
                                offset.y / size.height.toFloat()
                            )
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            val segRadius = minDim * 0.020f
            val headRadius = minDim * 0.026f
            val foodRadius = minDim * 0.022f

            // Board background
            drawRect(color = courtColor, size = Size(w, h))

            // Subtle grid
            for (i in 1..9) {
                drawLine(lineColor, Offset(i * w / 10f, 0f), Offset(i * w / 10f, h), strokeWidth = 1f)
                drawLine(lineColor, Offset(0f, i * h / 10f), Offset(w, i * h / 10f), strokeWidth = 1f)
            }

            // Wall border
            drawRect(color = wallColor, size = Size(w, h), style = Stroke(width = 3f))

            // Food items
            for (food in state.foods) {
                val pos = Offset(food.position.x * w, food.position.y * h)
                val color = foodColors[food.type] ?: GameAccent
                drawCircle(color = color, radius = foodRadius, center = pos)
                // Outer glow ring for special food
                if (food.type != FoodType.STANDARD) {
                    drawCircle(
                        color = color.copy(alpha = 0.3f),
                        radius = foodRadius * 1.6f,
                        center = pos,
                        style = Stroke(width = 2f)
                    )
                }
            }

            // Snake body — render tail-to-head so head draws on top
            val segCount = state.segments.size
            for (i in state.segments.indices.reversed()) {
                val seg = state.segments[i]
                val pos = Offset(seg.x * w, seg.y * h)
                val t = if (segCount > 1) 1f - i.toFloat() / (segCount - 1) else 1f
                val alpha = (0.35f + 0.65f * t).coerceIn(0f, 1f)
                val radius = if (i == 0) headRadius else segRadius
                drawCircle(
                    color = if (state.ghostMode) playerColor.copy(alpha = alpha * 0.5f)
                    else playerColor.copy(alpha = alpha),
                    radius = radius,
                    center = pos
                )
            }

            // Eyes on the head
            if (state.segments.isNotEmpty()) {
                val headSeg = state.segments.first()
                val hc = Offset(headSeg.x * w, headSeg.y * h)
                val fwdX = state.direction.x
                val fwdY = state.direction.y
                // Perpendicular (right-hand normal)
                val rightX = -fwdY
                val rightY = fwdX
                val eyeForward = headRadius * 0.38f
                val eyeSide = headRadius * 0.32f
                val eyeR = headRadius * 0.22f
                val leftEye = Offset(hc.x + fwdX * eyeForward + rightX * eyeSide,
                    hc.y + fwdY * eyeForward + rightY * eyeSide)
                val rightEye = Offset(hc.x + fwdX * eyeForward - rightX * eyeSide,
                    hc.y + fwdY * eyeForward - rightY * eyeSide)
                drawCircle(Color.White, radius = eyeR, center = leftEye)
                drawCircle(Color.White, radius = eyeR, center = rightEye)
                drawCircle(Color.Black, radius = eyeR * 0.55f, center = leftEye)
                drawCircle(Color.Black, radius = eyeR * 0.55f, center = rightEye)
            }

            // Tap destination marker (fades over 1 second)
            val markerAlpha = state.tapMarkerTimer.coerceIn(0f, 1f)
            if (markerAlpha > 0f && state.tapMarkerPos != null) {
                val mp = Offset(state.tapMarkerPos.x * w, state.tapMarkerPos.y * h)
                drawCircle(
                    color = markerColor.copy(alpha = markerAlpha * 0.7f),
                    radius = 14f,
                    center = mp,
                    style = Stroke(width = 2f)
                )
                drawCircle(
                    color = markerColor.copy(alpha = markerAlpha * 0.35f),
                    radius = 22f,
                    center = mp,
                    style = Stroke(width = 1.5f)
                )
            }
        }
    }
}
