package com.xanticious.androidgames.view.games.endlessrunner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.endlessrunner.EndlessRunnerController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.endlessrunner.EndlessRunnerEvent
import com.xanticious.androidgames.model.games.endlessrunner.EndlessRunnerInput
import com.xanticious.androidgames.model.games.endlessrunner.EndlessRunnerState
import com.xanticious.androidgames.model.games.endlessrunner.ObstacleKind
import com.xanticious.androidgames.state.games.endlessrunner.EndlessRunnerStateMachine
import com.xanticious.androidgames.state.games.endlessrunner.RunnerPhase
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold

/**
 * Endless Runner — ninja auto-scroller (`design/action-games/endless-runner`).
 *
 * Tap anywhere to start / jump / double-jump.
 * Press and hold to slide under overhead banners.
 * One-hit death; distance in metres is the score.
 */
@Composable
fun EndlessRunnerGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { EndlessRunnerController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { EndlessRunnerStateMachine() }
    val phase by machine.phase.collectAsState()

    var state by remember { mutableStateOf(EndlessRunnerState.initial(config.initialSpeed)) }
    var bestDistance by remember { mutableStateOf(0) }

    // Input flags mutated by the gesture handler and consumed by the game loop.
    var jumpRequested by remember { mutableStateOf(false) }
    var slideActive by remember { mutableStateOf(false) }
    var isPointerDown by remember { mutableStateOf(false) }
    var pointerDownTime by remember { mutableLongStateOf(0L) }

    // Drive the physics loop while Running.
    GameLoop(running = phase == RunnerPhase.RUNNING) { dt ->
        // A sustained press (≥ SLIDE_HOLD_MS) activates slide; quick release is a tap.
        if (isPointerDown &&
            (System.currentTimeMillis() - pointerDownTime) >= SLIDE_HOLD_MS
        ) {
            slideActive = true
        }

        val step = controller.step(
            state, config, dt,
            EndlessRunnerInput(jumpPressed = jumpRequested, slideActive = slideActive)
        )
        jumpRequested = false // consume after one frame
        state = step.state

        if (step.event == EndlessRunnerEvent.Died) {
            bestDistance = maxOf(bestDistance, state.distance.toInt())
            machine.runnerDied()
        }
    }

    GameScaffold(
        title = "Endless Runner",
        onExit = onExit,
        hud = {
            GameHud(
                left = "${state.distance.toInt()} m",
                center = difficulty.label,
                right = "Best ${bestDistance} m"
            )
        },
        status = {
            when (phase) {
                RunnerPhase.IDLE -> {
                    Text(
                        text = "Tap to start",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                RunnerPhase.DEAD -> {
                    DefeatPanel(
                        score = state.distance.toInt(),
                        bestScore = bestDistance,
                        headline = "Run Ended",
                        onTryAgain = {
                            state = EndlessRunnerState.initial(config.initialSpeed)
                            slideActive = false
                            jumpRequested = false
                            machine.restart()
                        },
                        onMenu = onExit
                    )
                }
                RunnerPhase.RUNNING -> { /* HUD shows live stats */ }
            }
        }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(phase) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.firstOrNull() ?: continue

                            if (pointer.pressed && !pointer.previousPressed) {
                                // Finger down — record time for tap vs hold discrimination.
                                isPointerDown = true
                                pointerDownTime = System.currentTimeMillis()
                            } else if (!pointer.pressed && pointer.previousPressed) {
                                // Finger up — decide: tap or end-of-slide.
                                val heldMs = System.currentTimeMillis() - pointerDownTime
                                isPointerDown = false
                                slideActive = false
                                if (heldMs < SLIDE_HOLD_MS) {
                                    when (phase) {
                                        RunnerPhase.IDLE -> machine.startRun()
                                        RunnerPhase.RUNNING -> jumpRequested = true
                                        RunnerPhase.DEAD -> { /* buttons handle retry */ }
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // --- Background ---------------------------------------------------
            drawRect(color = GameCourt, size = Size(w, h))

            // Sky gradient bands — two subtle horizontal strips.
            drawRect(
                color = Dark1,
                topLeft = Offset(0f, 0f),
                size = Size(w, h * 0.35f)
            )
            drawRect(
                color = Dark2,
                topLeft = Offset(0f, h * 0.35f),
                size = Size(w, h * 0.40f)
            )

            // Ground strip.
            val groundY = EndlessRunnerState.GROUND_Y * h
            drawRect(
                color = Aqua3.copy(alpha = 0.25f),
                topLeft = Offset(0f, groundY),
                size = Size(w, h - groundY)
            )

            // Ground line.
            drawLine(
                color = GameCourtLine,
                start = Offset(0f, groundY),
                end = Offset(w, groundY),
                strokeWidth = 3f
            )

            // --- Obstacles ----------------------------------------------------
            for (obs in state.obstacles) {
                val obsX = obs.x * w
                val obsHalfW = EndlessRunnerState.OBS_HALF_WIDTH * w
                when (obs.kind) {
                    ObstacleKind.GROUND -> {
                        // Bamboo spike cluster — rises from the ground.
                        val spikeH = EndlessRunnerState.GROUND_OBS_HEIGHT * h
                        val left = obsX - obsHalfW
                        drawRoundRect(
                            color = GameHazard,
                            topLeft = Offset(left, groundY - spikeH),
                            size = Size(obsHalfW * 2f, spikeH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        // Tip highlight.
                        drawRect(
                            color = GameAccent.copy(alpha = 0.6f),
                            topLeft = Offset(left, groundY - spikeH),
                            size = Size(obsHalfW * 2f, 6f)
                        )
                    }
                    ObstacleKind.OVERHEAD -> {
                        // Hanging banner — drops from the top of the board.
                        val hangBottom = EndlessRunnerState.OVERHEAD_HANG_BOTTOM * h
                        val left = obsX - obsHalfW
                        drawRoundRect(
                            color = GameEnemy,
                            topLeft = Offset(left, 0f),
                            size = Size(obsHalfW * 2f, hangBottom),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        // Bottom edge highlight.
                        drawRect(
                            color = GameAccent.copy(alpha = 0.5f),
                            topLeft = Offset(left, hangBottom - 5f),
                            size = Size(obsHalfW * 2f, 5f)
                        )
                    }
                }
            }

            // --- Ninja silhouette --------------------------------------------
            val runnerPx = EndlessRunnerState.RUNNER_X * w
            val feetY = state.runnerY * h
            val bodyH = state.runnerHeight * h
            val bodyW = EndlessRunnerState.RUNNER_HALF_WIDTH * 2f * w
            val bodyLeft = runnerPx - bodyW / 2f
            val bodyTop = feetY - bodyH

            // Body (black silhouette).
            drawRoundRect(
                color = GamePlayer,
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyW, bodyH),
                cornerRadius = CornerRadius(bodyW * 0.2f, bodyW * 0.2f)
            )

            // Glowing eyes (drawn only when not sliding; narrow eyes when sliding).
            if (!state.isSliding) {
                val eyeY = bodyTop + bodyH * 0.22f
                val eyeRadius = bodyW * 0.10f
                val eyeSpacing = bodyW * 0.20f
                // Left eye.
                drawCircle(
                    color = GameAccent,
                    radius = eyeRadius,
                    center = Offset(runnerPx - eyeSpacing / 2f, eyeY)
                )
                // Right eye.
                drawCircle(
                    color = GameAccent,
                    radius = eyeRadius,
                    center = Offset(runnerPx + eyeSpacing / 2f, eyeY)
                )
            } else {
                // Squinting eyes — thin horizontal lines.
                val eyeY = bodyTop + bodyH * 0.35f
                val eyeSpacing = bodyW * 0.20f
                drawLine(
                    color = Aqua1,
                    start = Offset(runnerPx - eyeSpacing / 2f - bodyW * 0.08f, eyeY),
                    end = Offset(runnerPx - eyeSpacing / 2f + bodyW * 0.08f, eyeY),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Aqua1,
                    start = Offset(runnerPx + eyeSpacing / 2f - bodyW * 0.08f, eyeY),
                    end = Offset(runnerPx + eyeSpacing / 2f + bodyW * 0.08f, eyeY),
                    strokeWidth = 3f
                )
            }

            // Ki shield glow ring.
            if (state.hasShield) {
                drawCircle(
                    color = GameAccent.copy(alpha = 0.35f),
                    radius = bodyW * 0.9f,
                    center = Offset(runnerPx, feetY - bodyH / 2f)
                )
            }
        }
    }
}

/** Minimum press duration (ms) before a press-and-hold triggers slide. */
private const val SLIDE_HOLD_MS = 250L
