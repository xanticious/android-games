package com.xanticious.androidgames.view.games.brickbreakerarcade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.xanticious.androidgames.view.games.brickbreaker.drawBottomPaddle
import com.xanticious.androidgames.view.games.brickbreaker.drawBricks
import com.xanticious.androidgames.view.games.brickbreaker.drawCourt
import com.xanticious.androidgames.view.games.brickbreaker.drawDroppingPowerUps

/**
 * Brick Breaker Arcade — real-time variant.
 *
 * The cannon at the bottom auto-fires a 3-ball spread. The player slides it
 * left/right to aim. Bricks descend continuously; losing all 3 lives ends the
 * game. Clear enough rows to advance levels.
 */
@Composable
fun BrickBreakerArcadeGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { BrickBreakerController() }
    val config = remember(difficulty) { controller.configFor(BrickBreakerVariant.ARCADE, difficulty) }
    val machine = remember { BrickBreakerStateMachine() }
    val phase by machine.phase.collectAsState()

    var state by remember { mutableStateOf(BrickBreakerState()) }
    var paddleX by remember { mutableFloatStateOf(0.5f) }
    var respawnTimer by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        machine.levelStarted()
        state = controller.generateLevel(config, 1)
        machine.readyForPlay()
    }

    GameLoop(running = phase == BrickBreakerPhase.PLAYING) { dt ->
        val result = controller.step(
            state, config, dt,
            BrickBreakerInput(paddleX = paddleX),
        )
        state = result.state
        when {
            result.livesGone -> machine.allLivesLost()
            result.brickReachedBottom -> machine.brickHitBottom()
            result.levelRowsCleared -> machine.levelRowsCleared()
            result.fieldCleared -> machine.fieldCleared()
        }
    }

    // Respawn delay (2 s) after life loss.
    GameLoop(running = phase == BrickBreakerPhase.LIFE_LOST) { dt ->
        respawnTimer += dt
        if (respawnTimer >= 2f) {
            respawnTimer = 0f
            machine.respawnReady()
        }
    }

    val textMeasurer = rememberTextMeasurer()

    GameScaffold(
        title = "Brick Breaker Arcade",
        onExit = onExit,
        hud = {
            GameHud(
                left = "Score ${state.score}",
                center = "Level ${state.level}",
                right = "Lives ${"♥".repeat(state.lives.coerceAtLeast(0))}",
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
                    BrickBreakerPhase.PLAYING, BrickBreakerPhase.LIFE_LOST -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("◄", style = MaterialTheme.typography.labelLarge)
                            Slider(
                                value = paddleX,
                                onValueChange = { paddleX = it },
                                valueRange = 0.05f..0.95f,
                                modifier = Modifier.weight(1f),
                            )
                            Text("►", style = MaterialTheme.typography.labelLarge)
                        }
                        if (phase == BrickBreakerPhase.LIFE_LOST) {
                            Text(
                                "Life lost! Resuming…",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    BrickBreakerPhase.LEVEL_COMPLETE -> {
                        VictoryPanel(
                            score = state.score,
                            bestScore = state.score,
                            stars = state.lives.coerceIn(1, 3),
                            onReplay = {
                                machine.nextLevel()
                                state = controller.generateLevel(config, state.level + 1)
                                    .copy(score = state.score, lives = state.lives)
                                machine.readyForPlay()
                            },
                            onMenu = onExit,
                            headline = "Level ${state.level} Clear!",
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
            drawBricks(state, textMeasurer, state.descentOffset)
            drawBalls(state)
            drawDroppingPowerUps(state)
            drawBottomPaddle(paddleX)
        }
    }
}
