package com.xanticious.androidgames.view.games.brickbreakercannon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.brickbreaker.BrickBreakerController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.brickbreaker.BrickBreakerInput
import com.xanticious.androidgames.model.games.brickbreaker.BrickBreakerState
import com.xanticious.androidgames.model.games.brickbreaker.BrickBreakerVariant
import com.xanticious.androidgames.state.games.brickbreaker.BrickBreakerPhase
import com.xanticious.androidgames.state.games.brickbreaker.BrickBreakerStateMachine
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.games.brickbreaker.activePowerUpLabel
import com.xanticious.androidgames.view.games.brickbreaker.drawBalls
import com.xanticious.androidgames.view.games.brickbreaker.drawBricks
import com.xanticious.androidgames.view.games.brickbreaker.drawCourt
import com.xanticious.androidgames.view.games.brickbreaker.drawDroppingPowerUps
import com.xanticious.androidgames.view.games.brickbreaker.drawGround
import com.xanticious.androidgames.view.games.brickbreaker.drawLeftCannon
import com.xanticious.androidgames.view.games.brickbreaker.drawTrajectory

/**
 * Brick Breaker Cannon — turn-based arc-physics variant.
 *
 * A cannon on the left wall fires cannonballs along gravity-affected parabolic
 * arcs. Destroy all target (gold) bricks within the turn limit to win each
 * level.
 */
@Composable
fun BrickBreakerCannonGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { BrickBreakerController() }
    val config = remember(difficulty) { controller.configFor(BrickBreakerVariant.CANNON, difficulty) }
    val machine = remember { BrickBreakerStateMachine() }
    val phase by machine.phase.collectAsState()

    var state by remember { mutableStateOf(BrickBreakerState()) }
    var aimAngleDeg by remember { mutableFloatStateOf(45f) }

    LaunchedEffect(Unit) {
        machine.levelStarted()
        state = controller.generateLevel(config, 1)
        machine.readyForAim()
    }

    // Ball physics run during FIRE_PHASE.
    GameLoop(running = phase == BrickBreakerPhase.FIRE_PHASE) { dt ->
        val result = controller.step(
            state, config, dt,
            BrickBreakerInput(aimAngleDeg = aimAngleDeg),
        )
        state = result.state
        when {
            result.allTargetsCleared -> {
                machine.allBallsLanded()
                machine.allTargetsDestroyed()
            }
            result.fieldCleared -> {
                machine.allBallsLanded()
                machine.fieldCleared()
            }
            result.allBallsDone -> {
                machine.allBallsLanded()
                // Decrement turns and decide next phase.
                state = state.copy(turnsLeft = state.turnsLeft - 1)
                if (state.turnsLeft <= 0) {
                    machine.turnsExhausted()
                } else {
                    machine.nextTurnReady()
                }
            }
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val trajectoryPoints = remember(aimAngleDeg) {
        controller.trajectoryPreview(config, aimAngleDeg)
    }

    GameScaffold(
        title = "Brick Breaker Cannon",
        onExit = onExit,
        hud = {
            GameHud(
                left = "Score ${state.score}",
                center = "Targets ${controller.targetBrickCount(state)}",
                right = "Turns ${state.turnsLeft}",
            )
        },
        status = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                if (state.activePowerUps.isNotEmpty()) {
                    Text(
                        text = activePowerUpLabel(state.activePowerUps),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
                when (phase) {
                    BrickBreakerPhase.AIM_PHASE -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "Aim ${aimAngleDeg.toInt()}°",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Slider(
                                value = aimAngleDeg,
                                onValueChange = { aimAngleDeg = it },
                                valueRange = 5f..85f,
                                modifier = Modifier.weight(2f),
                            )
                            Button(onClick = {
                                state = controller.launchCannonBall(
                                    state.copy(cannonAngleDeg = aimAngleDeg), config,
                                )
                                machine.fireTapped()
                            }) { Text("Fire") }
                        }
                    }
                    BrickBreakerPhase.FIRE_PHASE -> {
                        Text(
                            "Ball in flight…",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    BrickBreakerPhase.LEVEL_COMPLETE -> {
                        val bonus = controller.turnsRemainingBonus(state)
                        VictoryPanel(
                            score = state.score + bonus,
                            bestScore = state.score + bonus,
                            stars = when {
                                state.turnsLeft > 6 -> 3
                                state.turnsLeft > 3 -> 2
                                else -> 1
                            },
                            onReplay = {
                                machine.nextLevel()
                                state = controller.generateLevel(config, state.level + 1)
                                    .copy(score = state.score + bonus)
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
            drawGround()
            drawBricks(state, textMeasurer)
            drawBalls(state)
            if (phase == BrickBreakerPhase.AIM_PHASE) {
                drawTrajectory(trajectoryPoints)
            }
            drawDroppingPowerUps(state)
            drawLeftCannon(aimAngleDeg)
        }
    }
}
