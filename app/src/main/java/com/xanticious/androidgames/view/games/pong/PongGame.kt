package com.xanticious.androidgames.view.games.pong

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.pong.PongController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.pong.PongEvent
import com.xanticious.androidgames.model.games.pong.PongInput
import com.xanticious.androidgames.model.games.pong.PongState
import com.xanticious.androidgames.model.games.pong.Stance
import com.xanticious.androidgames.state.games.pong.PongPhase
import com.xanticious.androidgames.state.games.pong.PongStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlin.random.Random

/**
 * Pong — bat-and-ball court game (`design/action-games/pong`). Tap your half to
 * swing the bat. First to win [com.xanticious.androidgames.model.games.pong.PongConfig.setsToWin]
 * sets wins.
 */
@Composable
fun PongGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { PongController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { PongStateMachine() }
    val phase by machine.phase.collectAsState()

    var state by remember { mutableStateOf(PongState.initial()) }
    var targetBatY by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(Unit) {
        machine.startMatch()
        state = controller.serve(state, config, towardPlayer = Random.nextBoolean(), verticalBias = Random.nextFloat() - 0.5f)
        machine.ballServed()
    }

    GameLoop(running = phase == PongPhase.PLAYING) { dt ->
        val step = controller.step(state, config, dt, PongInput(targetBatY, swingRequested = false, stance = Stance.FOREHAND))
        state = step.state
        if (step.event != PongEvent.NONE) {
            targetBatY = null
            machine.pointEnded()
            if (controller.matchWinner(state, config)) {
                machine.setEnded()
                machine.matchEnded()
            } else {
                machine.continueSet()
                state = controller.serve(
                    state, config,
                    towardPlayer = step.event == PongEvent.POINT_AI,
                    verticalBias = Random.nextFloat() - 0.5f
                )
                machine.ballServed()
            }
        }
    }

    val playerWon = state.playerSets > state.aiSets

    GameScaffold(
        title = "Pong",
        onExit = onExit,
        hud = {
            GameHud(
                left = "AI ${state.aiScore}  (${state.aiSets})",
                center = "Rally ${state.rally}",
                right = "(${state.playerSets})  ${state.playerScore} You"
            )
        },
        status = {
            if (phase == PongPhase.MATCH_OVER) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    if (playerWon) {
                        VictoryPanel(
                            score = state.playerSets,
                            bestScore = state.playerSets,
                            stars = 3,
                            onReplay = onExit,
                            onMenu = onExit,
                            headline = "Match Won!"
                        )
                    } else {
                        DefeatPanel(
                            score = state.aiSets,
                            bestScore = state.aiSets,
                            onTryAgain = onExit,
                            onMenu = onExit,
                            headline = "Match Lost"
                        )
                    }
                }
            }
        }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        targetBatY = (offset.y / size.height).coerceIn(0f, 1f)
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            drawRect(color = GameCourt, size = Size(w, h))
            // Net (center line).
            var y = 0f
            while (y < h) {
                drawRect(color = GameCourtLine.copy(alpha = 0.4f), topLeft = Offset(w / 2f - 2f, y), size = Size(4f, h * 0.04f))
                y += h * 0.08f
            }
            val batHalf = config.batHalfHeight * h
            val batWidth = w * 0.012f
            // AI bat (left).
            drawRect(
                color = GameEnemy,
                topLeft = Offset(0.08f * w - batWidth / 2f, state.aiBatY * h - batHalf),
                size = Size(batWidth, batHalf * 2f)
            )
            // Player bat (right).
            drawRect(
                color = GamePlayer,
                topLeft = Offset(0.92f * w - batWidth / 2f, state.playerBatY * h - batHalf),
                size = Size(batWidth, batHalf * 2f)
            )
            // Ball.
            drawCircle(color = GameAccent, radius = h * 0.018f, center = Offset(state.ball.x * w, state.ball.y * h))
        }
    }
}
