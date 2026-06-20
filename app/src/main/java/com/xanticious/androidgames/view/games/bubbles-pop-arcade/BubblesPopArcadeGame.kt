package com.xanticious.androidgames.view.games.bubblesarcade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.bubblespop.BubblesPopController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.bubblespop.BubbleType
import com.xanticious.androidgames.model.games.bubblespop.BubblesGridEvent
import com.xanticious.androidgames.model.games.bubblespop.BubblesGridState
import com.xanticious.androidgames.model.games.bubblespop.BubblesVariant
import com.xanticious.androidgames.state.games.bubblespop.BubblesPopPhase
import com.xanticious.androidgames.state.games.bubblespop.BubblesPopStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.games.bubblespop.bubbleColor
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.cos

/**
 * Bubbles Pop (Arcade) — real-time bubble shooter (`design/action-games/bubbles-pop-arcade`).
 * The cluster descends continuously; tap to aim and fire freely with a 0.4-second cooldown.
 * Power-ups spawn in the cluster. Three lives reset the cluster on danger-line touches.
 */
@Composable
fun BubblesPopArcadeGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { BubblesPopController() }
    val config = remember(difficulty) { controller.configFor(difficulty, BubblesVariant.ARCADE) }
    val machine = remember { BubblesPopStateMachine(BubblesVariant.ARCADE) }
    val phase by machine.phase.collectAsState()

    var gameState by remember { mutableStateOf(controller.initialGridState(config)) }
    var aimAngle by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) { machine.startGame() }

    // Pause play briefly after a life is lost, then resume
    LaunchedEffect(phase) {
        if (phase == BubblesPopPhase.LIFE_LOST) {
            delay(1500L)
            machine.lifeReset()
        }
    }

    GameLoop(running = phase == BubblesPopPhase.PLAYING) { dt ->
        val (newState, event) = controller.stepGrid(gameState, config, dt)
        gameState = newState
        when (event) {
            is BubblesGridEvent.ClusterEmpty -> machine.clusterCleared()
            is BubblesGridEvent.ClusterCrossedDangerLine -> machine.gameOver()
            is BubblesGridEvent.LifeLost -> machine.lifeLost()
            else -> {}
        }
    }

    val bestScore = gameState.bestScore.coerceAtLeast(gameState.score)
    val heartsText = "♥".repeat(gameState.lives) + "♡".repeat((3 - gameState.lives).coerceAtLeast(0))
    val dangerNear = gameState.topOffset > BubblesPopController.DANGER_LINE_Y * 0.6f

    GameScaffold(
        title = "Bubbles Pop Arcade",
        onExit = onExit,
        hud = {
            GameHud(
                left = "Score: ${gameState.score}",
                center = "Level ${gameState.level}",
                right = heartsText,
            )
        },
        status = {
            when (phase) {
                BubblesPopPhase.LIFE_LOST -> Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text(
                        "Life Lost! Resuming…",
                        style = MaterialTheme.typography.titleMedium,
                        color = GameHazard,
                    )
                    Text("Lives remaining: ${gameState.lives}", style = MaterialTheme.typography.bodyMedium)
                }
                BubblesPopPhase.LEVEL_COMPLETE -> VictoryPanel(
                    score = gameState.score,
                    bestScore = bestScore,
                    stars = starsArcade(gameState.score, gameState.level),
                    onReplay = {
                        gameState = controller.initialGridState(config)
                        machine.resetGame(); machine.startGame()
                    },
                    onMenu = onExit,
                    headline = "Level ${gameState.level} Cleared!",
                )
                BubblesPopPhase.GAME_OVER -> DefeatPanel(
                    score = gameState.score,
                    bestScore = bestScore,
                    onTryAgain = {
                        gameState = controller.initialGridState(config)
                        machine.resetGame(); machine.startGame()
                    },
                    onMenu = onExit,
                )
                BubblesPopPhase.PLAYING -> {
                    if (gameState.activePowerUps.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                            gameState.activePowerUps.forEach { p ->
                                Text(
                                    text = "${p.type.name.take(3)} ${p.remainingSeconds.toInt()}s  ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GameAccent,
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        },
    ) {
        val canFire = phase == BubblesPopPhase.PLAYING
        val borderColor = if (dangerNear && phase == BubblesPopPhase.PLAYING) GameHazard else Color.Transparent

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(canFire) {
                    if (!canFire) return@pointerInput
                    detectTapGestures { tap ->
                        val cx = size.width / 2f
                        val cy = size.height * BubblesPopController.CANNON_Y
                        aimAngle = atan2(tap.x - cx, cy - tap.y)
                            .coerceIn((-PI * 0.45f).toFloat(), (PI * 0.45f).toFloat())
                        val fired = controller.fireCannon(gameState, aimAngle)
                        if (fired.flying != null) gameState = fired
                    }
                }
                .pointerInput(canFire) {
                    if (!canFire) return@pointerInput
                    detectDragGestures { change, _ ->
                        val cx = size.width / 2f
                        val cy = size.height * BubblesPopController.CANNON_Y
                        aimAngle = atan2(change.position.x - cx, cy - change.position.y)
                            .coerceIn((-PI * 0.45f).toFloat(), (PI * 0.45f).toFloat())
                        val fired = controller.fireCannon(gameState, aimAngle)
                        if (fired.flying != null) gameState = fired
                    }
                },
        ) {
            val w = size.width
            val h = size.height
            val r = BubblesPopController.BUBBLE_RADIUS

            // Background
            drawRect(GameCourt, size = Size(w, h))

            // Danger border pulsing when cluster is near
            if (borderColor != Color.Transparent) {
                drawRect(borderColor, topLeft = Offset.Zero, size = Size(w, h), style = Stroke(width = 6f))
            }

            // Danger line
            drawRect(
                GameHazard.copy(alpha = 0.35f),
                topLeft = Offset(0f, BubblesPopController.DANGER_LINE_Y * h - 2f),
                size = Size(w, 4f),
            )

            // Descent indicator (subtle arrows on bottom row of cluster)
            if (phase == BubblesPopPhase.PLAYING) {
                val maxRowY = (gameState.grid.values.maxOfOrNull { c ->
                    gameState.topOffset + c.row * BubblesPopController.ROW_HEIGHT + r * 2f
                } ?: 0f) * h
                drawLine(
                    GameHazard.copy(alpha = 0.5f),
                    Offset(w * 0.3f, maxRowY + 4f),
                    Offset(w * 0.7f, maxRowY + 4f),
                    strokeWidth = 3f,
                )
                drawLine(
                    GameHazard.copy(alpha = 0.5f),
                    Offset(w * 0.48f, maxRowY + 4f),
                    Offset(w * 0.5f, maxRowY + 12f),
                    strokeWidth = 3f,
                )
                drawLine(
                    GameHazard.copy(alpha = 0.5f),
                    Offset(w * 0.52f, maxRowY + 4f),
                    Offset(w * 0.5f, maxRowY + 12f),
                    strokeWidth = 3f,
                )
            }

            // Grid bubbles
            gameState.grid.values.forEach { cell ->
                val pos = controller.cellPosition(cell.col, cell.row, gameState.topOffset)
                val bx = pos.x * w
                val by = pos.y * h
                val radius = r * w
                val color = bubbleColor(cell.color)
                drawCircle(color, radius, Offset(bx, by))
                when (cell.type) {
                    BubbleType.BOMB -> drawCircle(Color.Black.copy(alpha = 0.4f), radius * 0.5f, Offset(bx, by))
                    BubbleType.STONE -> drawCircle(GameNeutral.copy(alpha = 0.6f), radius, Offset(bx, by))
                    BubbleType.RAINBOW -> drawCircle(Color.White.copy(alpha = 0.3f), radius, Offset(bx, by), style = Stroke(3f))
                    BubbleType.POWER_UP -> drawCircle(GameAccent.copy(alpha = 0.5f), radius * 0.5f, Offset(bx, by))
                    else -> {}
                }
                drawCircle(Color.White.copy(alpha = 0.22f), radius * 0.45f, Offset(bx - radius * 0.22f, by - radius * 0.28f))
            }

            // Flying bubble
            gameState.flying?.let { fb ->
                drawCircle(bubbleColor(fb.color), r * w, Offset(fb.x * w, fb.y * h))
                drawCircle(Color.White.copy(alpha = 0.3f), r * w * 0.4f, Offset(fb.x * w - r * w * 0.2f, fb.y * h - r * w * 0.3f))
            }

            // Cannon
            val cx = BubblesPopController.CANNON_X * w
            val cy = BubblesPopController.CANNON_Y * h
            val barrelLen = r * w * 3f
            drawLine(GameNeutral, Offset(cx, cy), Offset(cx + sin(aimAngle) * barrelLen, cy - cos(aimAngle) * barrelLen), strokeWidth = r * w * 1.5f)
            drawCircle(GameNeutral, r * w * 1.4f, Offset(cx, cy))
            drawCircle(bubbleColor(gameState.cannonBubble), r * w, Offset(cx, cy))

            // Next bubble
            drawCircle(bubbleColor(gameState.nextBubble), r * w * 0.8f, Offset(r * w * 2f, h - r * w * 2f))

            // Cooldown arc
            if (gameState.cannonCooldown > 0f && config.cannonCooldown > 0f) {
                val fraction = gameState.cannonCooldown / config.cannonCooldown
                drawArc(
                    color = GameHazard.copy(alpha = 0.6f),
                    startAngle = -90f,
                    sweepAngle = fraction * 360f,
                    useCenter = false,
                    topLeft = Offset(cx - r * w * 1.8f, cy - r * w * 1.8f),
                    size = Size(r * w * 3.6f, r * w * 3.6f),
                    style = Stroke(width = 4f),
                )
            }
        }
    }
}

private fun starsArcade(score: Int, level: Int): Int = when {
    score >= 2000 * level -> 3
    score >= 800 * level -> 2
    else -> 1
}
