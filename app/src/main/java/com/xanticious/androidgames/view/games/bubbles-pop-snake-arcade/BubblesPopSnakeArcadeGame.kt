package com.xanticious.androidgames.view.games.bubblessnakearcade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.bubblespop.BubblesPopController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.bubblespop.BubbleType
import com.xanticious.androidgames.model.games.bubblespop.BubblesPopConfig
import com.xanticious.androidgames.model.games.bubblespop.BubblesSnakeEvent
import com.xanticious.androidgames.model.games.bubblespop.BubblesSnakeState
import com.xanticious.androidgames.model.games.bubblespop.BubblesVariant
import com.xanticious.androidgames.state.games.bubblespop.BubblesPopPhase
import com.xanticious.androidgames.state.games.bubblespop.BubblesPopStateMachine
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Dark2
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

/**
 * Bubbles Pop Snake (Arcade) — Zuma-style chain shooter (`design/action-games/bubbles-pop-snake-arcade`).
 * A chain of colored bubbles winds along an S-shaped track toward an exit vortex.
 * The player fires bubbles from a central launcher to create 3+ color matches and eliminate the chain.
 * Backfire: misses add penalty bubbles. Three lives; game over when lives run out.
 */
@Composable
fun BubblesPopSnakeArcadeGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { BubblesPopController() }
    val config = remember(difficulty) { controller.configFor(difficulty, BubblesVariant.SNAKE_ARCADE) }
    val machine = remember { BubblesPopStateMachine(BubblesVariant.SNAKE_ARCADE) }
    val phase by machine.phase.collectAsState()

    var gameState by remember { mutableStateOf(controller.initialSnakeState(config)) }

    LaunchedEffect(Unit) { machine.startGame() }

    LaunchedEffect(phase) {
        if (phase == BubblesPopPhase.LIFE_LOST) {
            delay(1500L)
            machine.lifeReset()
        }
    }

    // Next level when level-complete panel's "Next" button is tapped — handled in status slot
    GameLoop(running = phase == BubblesPopPhase.PLAYING) { dt ->
        val (newState, event) = controller.stepSnake(gameState, config, dt)
        gameState = newState
        when (event) {
            is BubblesSnakeEvent.ChainCleared -> machine.clusterCleared()
            is BubblesSnakeEvent.BubbleExited -> {
                if (newState.lives <= 0) machine.gameOver() else machine.lifeLost()
            }
            else -> {}
        }
    }

    val bestScore = gameState.bestScore.coerceAtLeast(gameState.score)
    val heartsText = "♥".repeat(gameState.lives) + "♡".repeat((3 - gameState.lives).coerceAtLeast(0))

    GameScaffold(
        title = "Bubbles Pop Snake",
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
                        "A bubble escaped! Resuming…",
                        style = MaterialTheme.typography.titleMedium,
                        color = GameHazard,
                    )
                }
                BubblesPopPhase.LEVEL_COMPLETE -> Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    VictoryPanel(
                        score = gameState.score,
                        bestScore = bestScore,
                        stars = starsSnake(gameState.score, gameState.level),
                        onReplay = {
                            gameState = controller.initialSnakeState(config)
                            machine.resetGame(); machine.startGame()
                        },
                        onMenu = onExit,
                        headline = "Chain Cleared!",
                    )
                    Button(onClick = {
                        gameState = controller.nextLevelSnake(gameState, config)
                        machine.nextLevel()
                    }) { Text("Next Level") }
                }
                BubblesPopPhase.GAME_OVER -> DefeatPanel(
                    score = gameState.score,
                    bestScore = bestScore,
                    onTryAgain = {
                        gameState = controller.initialSnakeState(config)
                        machine.resetGame(); machine.startGame()
                    },
                    onMenu = onExit,
                )
                BubblesPopPhase.PLAYING -> {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Row {
                            Text("Swaps: ${gameState.swapRemaining}  ", style = MaterialTheme.typography.labelSmall)
                            gameState.activePowerUps.forEach { p ->
                                Text(
                                    "${p.type.name.take(4)} ${p.remainingSeconds.toInt()}s  ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GameAccent,
                                )
                            }
                        }
                        if (gameState.colorStormTimer > 0f) {
                            Text(
                                "Color Storm! ${gameState.colorStormTimer.toInt()}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = GameAccent,
                            )
                        }
                    }
                }
                else -> {}
            }
        },
    ) {
        val canFire = phase == BubblesPopPhase.PLAYING
        SnakeBoard(
            gameState = gameState,
            controller = controller,
            config = config,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(canFire) {
                    if (!canFire) return@pointerInput
                    detectTapGestures { tap ->
                        val tx = tap.x / size.width
                        val ty = tap.y / size.height
                        val fired = controller.fireSnakeCannon(gameState, tx, ty)
                        if (fired.flying != null) gameState = fired
                    }
                },
        )
    }
}

@Composable
private fun SnakeBoard(
    gameState: BubblesSnakeState,
    controller: BubblesPopController,
    config: BubblesPopConfig,
    modifier: Modifier = Modifier,
) {
    val r = BubblesPopController.BUBBLE_RADIUS
    val waypoints = BubblesPopController.TRACK_WAYPOINTS
    val launcherPos = BubblesPopController.LAUNCHER_POS
    val totalLen = controller.trackLength()

    // Pre-capture colors
    val bgColor = GameCourt
    val trackColor = Dark2
    val trackOuterColor = Aqua3.copy(alpha = 0.3f)
    val launcherColor = GameNeutral
    val exitGlowColor = GameHazard

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Background
        drawRect(bgColor, size = Size(w, h))

        // Track tube (draw twice for outer glow + inner fill)
        val trackPath = Path().apply {
            val first = waypoints.first()
            moveTo(first.x * w, first.y * h)
            waypoints.drop(1).forEach { wp -> lineTo(wp.x * w, wp.y * h) }
        }
        drawPath(trackPath, trackOuterColor, style = Stroke(width = r * w * 4.5f))
        drawPath(trackPath, trackColor, style = Stroke(width = r * w * 3f))

        // Exit vortex glow
        val exitWp = waypoints.last()
        drawCircle(exitGlowColor.copy(alpha = 0.5f), r * w * 2.5f, Offset(exitWp.x * w, exitWp.y * h))
        drawCircle(exitGlowColor.copy(alpha = 0.3f), r * w * 4f, Offset(exitWp.x * w, exitWp.y * h), style = Stroke(3f))

        // Chain bubbles
        gameState.chain.forEach { bubble ->
            val pos = controller.trackPosition(bubble.t)
            val bx = pos.x * w
            val by = pos.y * h
            val radius = r * w
            val color = if (gameState.colorStormColor != null) bubbleColor(gameState.colorStormColor) else bubbleColor(bubble.color)
            drawCircle(color, radius, Offset(bx, by))
            when (bubble.type) {
                BubbleType.BOMB -> drawCircle(Color.Black.copy(alpha = 0.4f), radius * 0.5f, Offset(bx, by))
                BubbleType.POWER_UP -> drawCircle(GameAccent.copy(alpha = 0.5f), radius * 0.5f, Offset(bx, by))
                BubbleType.RAINBOW -> drawCircle(Color.White.copy(alpha = 0.3f), radius, Offset(bx, by), style = Stroke(2f))
                else -> {}
            }
            // Highlight
            drawCircle(Color.White.copy(alpha = 0.22f), radius * 0.4f, Offset(bx - radius * 0.2f, by - radius * 0.25f))
        }

        // Danger indicator: highlight chain bubbles near the exit
        gameState.chain.filter { it.t > totalLen * 0.8f }.forEach { bubble ->
            val pos = controller.trackPosition(bubble.t)
            drawCircle(GameHazard.copy(alpha = 0.5f), r * w * 1.15f, Offset(pos.x * w, pos.y * h), style = Stroke(3f))
        }

        // Flying bubble
        gameState.flying?.let { fb ->
            drawCircle(bubbleColor(fb.color), r * w, Offset(fb.x * w, fb.y * h))
            drawCircle(Color.White.copy(alpha = 0.3f), r * w * 0.4f, Offset(fb.x * w - r * w * 0.2f, fb.y * h - r * w * 0.3f))
        }

        // Launcher
        val lx = launcherPos.x * w
        val ly = launcherPos.y * h
        drawCircle(Aqua2.copy(alpha = 0.3f), r * w * 3f, Offset(lx, ly))
        drawCircle(launcherColor, r * w * 1.8f, Offset(lx, ly))
        // Launcher barrel (points toward launcher angle)
        val angle = gameState.launcherAngle
        val barrelLen = r * w * 2.5f
        val bx2 = lx + kotlin.math.sin(angle) * barrelLen
        val by2 = ly - kotlin.math.cos(angle) * barrelLen
        drawLine(launcherColor, Offset(lx, ly), Offset(bx2, by2), strokeWidth = r * w * 1.2f)

        // Current bubble on launcher
        drawCircle(bubbleColor(gameState.cannonBubble), r * w * 0.9f, Offset(lx, ly))

        // Next bubble preview (bottom-left)
        val nx = r * w * 2.5f
        val ny = h - r * w * 2.5f
        drawCircle(launcherColor.copy(alpha = 0.5f), r * w * 1.2f, Offset(nx, ny))
        drawCircle(bubbleColor(gameState.nextBubble), r * w * 0.8f, Offset(nx, ny))

        // Cooldown ring on launcher
        if (gameState.cannonCooldown > 0f && config.cannonCooldown > 0f) {
            val fraction = gameState.cannonCooldown / config.cannonCooldown
            drawArc(
                color = GameHazard.copy(alpha = 0.7f),
                startAngle = -90f,
                sweepAngle = fraction * 360f,
                useCenter = false,
                topLeft = Offset(lx - r * w * 2.2f, ly - r * w * 2.2f),
                size = Size(r * w * 4.4f, r * w * 4.4f),
                style = Stroke(width = 4f),
            )
        }
    }
}

/**
 * Stars based on score performance.
 */
private fun starsSnake(score: Int, level: Int): Int = when {
    score >= 3000 * level -> 3
    score >= 1000 * level -> 2
    else -> 1
}
