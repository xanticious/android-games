package com.xanticious.androidgames.view.games.holeswallowing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.holeswallowing.HoleSwallowingController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.JoystickInput
import com.xanticious.androidgames.model.games.holeswallowing.HoleSwallowingConfig
import com.xanticious.androidgames.model.games.holeswallowing.HoleSwallowingEvent
import com.xanticious.androidgames.model.games.holeswallowing.HoleSwallowingState
import com.xanticious.androidgames.model.games.holeswallowing.ObjectTier
import com.xanticious.androidgames.state.games.holeswallowing.HoleSwallowingPhase
import com.xanticious.androidgames.state.games.holeswallowing.HoleSwallowingStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.FloatingJoystick
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel

/**
 * Hole-Swallowing Game — roll the sinkhole to devour the city before the timer
 * runs out. Steered by a floating joystick; objects are consumed automatically
 * when the hole diameter exceeds their size.
 */
@Composable
fun HoleSwallowingGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { HoleSwallowingController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { HoleSwallowingStateMachine() }
    val phase by machine.phase.collectAsState()

    var state by remember(config) { mutableStateOf(controller.initialState(config)) }
    var joystickInput by remember { mutableStateOf(JoystickInput.NONE) }

    LaunchedEffect(Unit) { machine.startLevel() }

    GameLoop(running = phase == HoleSwallowingPhase.PLAYING) { dt ->
        val step = controller.step(state, config, dt, joystickInput)
        state = step.state
        when (step.event) {
            HoleSwallowingEvent.TARGET_REACHED -> machine.targetReached()
            HoleSwallowingEvent.TIMER_EXPIRED -> machine.timerExpired()
            else -> {}
        }
    }

    val scoreProgress = (state.score.toFloat() / config.targetScore).coerceIn(0f, 1f)
    val timeInt = state.timeRemaining.toInt()
    val timerLow = state.timeRemaining <= 10f

    GameScaffold(
        title = "Hole Game",
        onExit = onExit,
        hud = {
            Column {
                GameHud(
                    left = "Score: ${state.score}",
                    center = "Target: ${config.targetScore}",
                    right = if (timerLow) "⚠ ${timeInt}s" else "${timeInt}s"
                )
                LinearProgressIndicator(
                    progress = { scoreProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .height(6.dp),
                    color = GamePlayer,
                    trackColor = Dark2.copy(alpha = 0.5f)
                )
            }
        },
        status = {
            when (phase) {
                HoleSwallowingPhase.LEVEL_COMPLETE -> VictoryPanel(
                    score = state.score,
                    bestScore = state.score,
                    stars = starsForGame(state, config),
                    onReplay = onExit,
                    onMenu = onExit,
                    headline = "Level Complete!"
                )
                HoleSwallowingPhase.GAME_OVER -> DefeatPanel(
                    score = state.score,
                    bestScore = state.score,
                    onTryAgain = onExit,
                    onMenu = onExit,
                    headline = "Time's Up!"
                )
                else -> {}
            }
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val (w, h) = Pair(size.width, size.height)
            val holePos = state.holePosition

            // Viewport: scales with hole size so the player always sees context.
            val vpRadius = maxOf(12f, state.holeRadius * 8f)
            val scale = minOf(w, h) / 2f / vpRadius
            val cx = w / 2f
            val cy = h / 2f

            fun worldToScreen(wx: Float, wy: Float): Offset =
                Offset(cx + (wx - holePos.x) * scale, cy + (wy - holePos.y) * scale)

            // City ground
            drawRect(color = Dark1, size = size)

            // Subtle city grid
            val gridStep = 10f
            val startGx = ((holePos.x - vpRadius) / gridStep).toInt() * gridStep
            val startGy = ((holePos.y - vpRadius) / gridStep).toInt() * gridStep
            var gx = startGx
            while (gx <= holePos.x + vpRadius) {
                val sx = cx + (gx - holePos.x) * scale
                drawLine(GameCourtLine.copy(alpha = 0.08f), Offset(sx, 0f), Offset(sx, h), strokeWidth = 1f)
                gx += gridStep
            }
            var gy = startGy
            while (gy <= holePos.y + vpRadius) {
                val sy = cy + (gy - holePos.y) * scale
                drawLine(GameCourtLine.copy(alpha = 0.08f), Offset(0f, sy), Offset(w, sy), strokeWidth = 1f)
                gy += gridStep
            }

            // City objects
            for (obj in state.objects) {
                if (obj.swallowed) continue
                val objScreenRadius = obj.tier.sizeValue / 2f * scale
                val sc = worldToScreen(obj.position.x, obj.position.y)
                if (sc.x < -objScreenRadius || sc.x > w + objScreenRadius) continue
                if (sc.y < -objScreenRadius || sc.y > h + objScreenRadius) continue

                val color = when (obj.tier) {
                    ObjectTier.TIER_1, ObjectTier.TIER_2 -> GameNeutral
                    ObjectTier.TIER_3 -> GameAccent
                    ObjectTier.TIER_4 -> GameHazard
                    ObjectTier.TIER_5, ObjectTier.TIER_6 -> GameEnemy
                }
                drawCircle(color = color, radius = objScreenRadius, center = sc)
                if (obj.tier.sizeValue >= 2f) {
                    drawCircle(
                        color = Dark0,
                        radius = objScreenRadius,
                        center = sc,
                        style = Stroke(width = 2f)
                    )
                }
            }

            // Time-bonus pickups (glowing clocks)
            for (bonus in state.bonusPickups) {
                if (bonus.collected) continue
                val bonusRadius = scale * 0.8f
                val sc = worldToScreen(bonus.position.x, bonus.position.y)
                if (sc.x < -bonusRadius * 2 || sc.x > w + bonusRadius * 2) continue
                if (sc.y < -bonusRadius * 2 || sc.y > h + bonusRadius * 2) continue
                drawCircle(color = GameSuccess.copy(alpha = 0.35f), radius = bonusRadius * 2f, center = sc)
                drawCircle(color = GameSuccess, radius = bonusRadius, center = sc)
            }

            // The hole — dark void with a rim
            val holeScreenRadius = state.holeRadius * scale
            drawCircle(color = GameCourt, radius = holeScreenRadius, center = Offset(cx, cy))
            drawCircle(
                color = GameCourtLine,
                radius = holeScreenRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 3f)
            )
            // Inner swirl ring for depth
            drawCircle(
                color = Dark2.copy(alpha = 0.6f),
                radius = holeScreenRadius * 0.6f,
                center = Offset(cx, cy),
                style = Stroke(width = 2f)
            )
        }

        FloatingJoystick(
            onInput = { joystickInput = it },
            modifier = Modifier.fillMaxSize(),
            accent = GamePlayer
        )
    }
}

private fun starsForGame(state: HoleSwallowingState, config: HoleSwallowingConfig): Int = when {
    state.timeRemaining > config.timeLimit * 0.5f -> 3
    state.timeRemaining > config.timeLimit * 0.25f -> 2
    else -> 1
}
