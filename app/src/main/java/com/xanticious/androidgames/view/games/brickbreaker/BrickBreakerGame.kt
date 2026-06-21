package com.xanticious.androidgames.view.games.brickbreaker

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.brickbreaker.BrickBreakerController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.brickbreaker.BrickBreakerInput
import com.xanticious.androidgames.model.games.brickbreaker.BrickBreakerState
import com.xanticious.androidgames.model.games.brickbreaker.BrickBreakerVariant
import com.xanticious.androidgames.state.games.brickbreaker.BrickBreakerPhase
import com.xanticious.androidgames.state.games.brickbreaker.BrickBreakerStateMachine
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel

/**
 * Brick Breaker — turn-based variant.
 *
 * Player slides a cannon left/right along the bottom, adjusts the aim angle,
 * then fires a 20-ball volley. After all balls land, bricks descend one row.
 * Clear all bricks before any reach the bottom.
 */
@Composable
fun BrickBreakerGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { BrickBreakerController() }
    val config = remember(difficulty) { controller.configFor(BrickBreakerVariant.CLASSIC, difficulty) }
    val machine = remember { BrickBreakerStateMachine() }
    val phase by machine.phase.collectAsState()

    var state by remember { mutableStateOf(BrickBreakerState()) }
    var paddleX by remember { mutableFloatStateOf(0.5f) }
    var aimAngleDeg by remember { mutableFloatStateOf(90f) }

    LaunchedEffect(Unit) {
        machine.levelStarted()
        state = controller.generateLevel(config, 1)
        machine.readyForAim()
    }

    // Game loop only runs during FIRE_PHASE.
    GameLoop(running = phase == BrickBreakerPhase.FIRE_PHASE) { dt ->
        val result = controller.step(
            state, config, dt,
            BrickBreakerInput(paddleX = paddleX, aimAngleDeg = aimAngleDeg),
        )
        state = result.state
        when {
            result.fieldCleared -> {
                machine.allBallsLanded()
                machine.fieldCleared()
            }
            result.allBallsDone -> {
                machine.allBallsLanded()
                // Resolution phase: drop bricks.
                val (dropped, atBottom) = controller.dropBricks(state)
                state = dropped.copy(turnsPlayed = state.turnsPlayed + 1)
                if (atBottom) {
                    machine.bricksRemain()
                    machine.bricksAtBottom()
                } else {
                    machine.bricksRemain()
                    machine.noBricksAtBottom()
                    state = controller.beginVolley(state, config)
                }
            }
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val trajectoryPoints = remember(aimAngleDeg, paddleX) {
        controller.classicTrajectoryPreview(paddleX, aimAngleDeg)
    }
    val bricksNearBoundary = controller.bricksNearBoundary(state)
    val boundaryPulse by rememberInfiniteTransition(label = "boundaryPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "boundaryPulseValue",
    )

    GameScaffold(
        title = "Brick Breaker",
        onExit = onExit,
        hud = {
            GameHud(
                left = "Score ${state.score}",
                center = "Level ${state.level}",
                right = "Turn ${state.turnsPlayed}",
            )
        },
        status = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                // Active power-up strip.
                if (state.activePowerUps.isNotEmpty()) {
                    Text(
                        text = activePowerUpLabel(state.activePowerUps),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
                when (phase) {
                    BrickBreakerPhase.AIM_PHASE -> {
                        // Cannon position slider.
                        Text("Balls: ${config.volleySize}", style = MaterialTheme.typography.labelMedium)
                        Text("Position", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = paddleX,
                            onValueChange = { paddleX = it },
                            valueRange = 0.05f..0.95f,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        // Angle slider.
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Angle ${aimAngleDeg.toInt()}°", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            Slider(
                                value = aimAngleDeg,
                                onValueChange = { aimAngleDeg = it },
                                valueRange = 10f..170f,
                                modifier = Modifier.weight(2f),
                            )
                            Button(onClick = {
                                state = controller.beginVolley(state, config)
                                machine.fireTapped()
                            }) { Text("Fire") }
                        }
                    }
                    BrickBreakerPhase.FIRE_PHASE -> {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Balls: ${state.balls.size + state.ballsToFire}", style = MaterialTheme.typography.labelMedium)
                            OutlinedButton(onClick = {
                                state = state.copy(balls = emptyList(), ballsToFire = 0)
                                machine.clearTapped()
                                val (dropped, atBottom) = controller.dropBricks(state)
                                state = dropped
                                if (atBottom) machine.bricksAtBottom()
                                else { machine.bricksRemain(); machine.noBricksAtBottom(); state = controller.beginVolley(state, config) }
                            }) { Text("Clear") }
                        }
                    }
                    BrickBreakerPhase.LEVEL_COMPLETE -> {
                        VictoryPanel(
                            score = state.score,
                            bestScore = state.score,
                            stars = 3,
                            onReplay = {
                                machine.nextLevel()
                                state = controller.generateLevel(config, state.level + 1)
                                    .copy(score = state.score)
                                state = controller.beginVolley(state, config)
                                machine.readyForAim()
                            },
                            onMenu = onExit,
                            headline = "Level ${state.level} Clear!",
                            primaryLabel = "Next Level",
                        )
                    }
                    BrickBreakerPhase.GAME_OVER -> {
                        DefeatPanel(
                            score = state.score,
                            bestScore = state.score,
                            onTryAgain = {
                                machine.restart()
                                machine.levelStarted()
                                state = controller.generateLevel(config, 1)
                                state = controller.beginVolley(state, config)
                                machine.readyForAim()
                            },
                            onMenu = onExit,
                        )
                    }
                    else -> {}
                }
            }
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCourt()
            drawBricks(state, textMeasurer)
            drawBoundaryLine(danger = bricksNearBoundary, pulse = boundaryPulse)
            drawBalls(state)
            if (phase == BrickBreakerPhase.AIM_PHASE) {
                drawTrajectory(trajectoryPoints)
            }
            drawDroppingPowerUps(state)
            drawBottomPaddle(paddleX)
        }
    }
}
