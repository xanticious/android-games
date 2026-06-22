package com.xanticious.androidgames.view.games.brickbreakercannonarcade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
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
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GameSuccess
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
 * Brick Breaker Cannon Arcade — real-time timer variant.
 *
 * The cannon on the left fires parabolic cannonballs on demand (tap Fire).
 * Destroy all target bricks before the countdown timer hits zero.
 */
@Composable
fun BrickBreakerCannonArcadeGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { BrickBreakerController() }
    val config = remember(difficulty) { controller.configFor(BrickBreakerVariant.CANNON_ARCADE, difficulty) }
    val machine = remember { BrickBreakerStateMachine() }
    val phase by machine.phase.collectAsState()

    var state by remember { mutableStateOf(BrickBreakerState()) }
    var aimAngleDeg by remember { mutableFloatStateOf(45f) }
    var fireRequested by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        machine.levelStarted()
        state = controller.generateLevel(config, 1)
        machine.readyForPlay()
    }

    GameLoop(running = phase == BrickBreakerPhase.PLAYING) { dt ->
        val result = controller.step(
            state, config, dt,
            BrickBreakerInput(aimAngleDeg = aimAngleDeg, fireRequested = fireRequested),
        )
        fireRequested = false  // consume the fire request after one step
        state = result.state
        when {
            result.allTargetsCleared -> machine.allTargetsDestroyed()
            result.fieldCleared -> machine.fieldCleared()
            result.timerExpired -> machine.timerExpired()
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val trajectoryPoints = remember(aimAngleDeg) {
        controller.trajectoryPreview(config, aimAngleDeg)
    }
    val startTimer = remember(state.level) { config.startTimerSeconds - (state.level - 1) * 5f }
    val timerFrac = (state.timerSeconds / startTimer.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val timerLow = state.timerSeconds <= 10f

    GameScaffold(
        title = "Brick Breaker Cannon Arcade",
        onExit = onExit,
        hud = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Full-width timer bar.
                LinearProgressIndicator(
                    progress = { timerFrac },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (timerLow) GameEnemy else GameSuccess,
                    trackColor = GameSuccess.copy(alpha = 0.2f),
                )
                GameHud(
                    left = "Score ${state.score}",
                    center = "Targets ${controller.targetBrickCount(state)}",
                    right = if (timerLow) "⚠ ${state.timerSeconds.toInt()}s" else "${state.timerSeconds.toInt()}s",
                )
            }
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
                    BrickBreakerPhase.PLAYING -> {
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
                            val cooldownReady = state.fireCooldown <= 0f
                            Button(
                                onClick = { if (cooldownReady) fireRequested = true },
                                enabled = cooldownReady,
                            ) { Text("Fire") }
                        }
                        if (state.fireCooldown > 0f) {
                            LinearProgressIndicator(
                                progress = { 1f - state.fireCooldown / config.shotCooldown },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                            )
                        }
                    }
                    BrickBreakerPhase.LEVEL_COMPLETE -> {
                        val bonus = controller.timeRemainingBonus(state)
                        VictoryPanel(
                            score = state.score + bonus,
                            bestScore = state.score + bonus,
                            stars = when {
                                state.timerSeconds > startTimer * 0.5f -> 3
                                state.timerSeconds > startTimer * 0.2f -> 2
                                else -> 1
                            },
                            onReplay = {
                                machine.nextLevel()
                                state = controller.generateLevel(config, state.level + 1)
                                    .copy(score = state.score + bonus)
                                machine.readyForPlay()
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
                                machine.readyForPlay()
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
            if (phase == BrickBreakerPhase.PLAYING) {
                drawTrajectory(trajectoryPoints)
            }
            drawDroppingPowerUps(state)
            drawLeftCannon(aimAngleDeg)
        }
    }
}
