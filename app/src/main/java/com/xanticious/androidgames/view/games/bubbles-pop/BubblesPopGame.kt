package com.xanticious.androidgames.view.games.bubblespop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.bubblespop.BubblesPopController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.bubblespop.BubbleColor
import com.xanticious.androidgames.model.games.bubblespop.BubbleType
import com.xanticious.androidgames.model.games.bubblespop.BubblesGridEvent
import com.xanticious.androidgames.model.games.bubblespop.BubblesGridState
import com.xanticious.androidgames.model.games.bubblespop.BubblesVariant
import com.xanticious.androidgames.state.games.bubblespop.BubblesPopPhase
import com.xanticious.androidgames.state.games.bubblespop.BubblesPopStateMachine
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Bubbles Pop — turn-based bubble shooter (`design/action-games/bubbles-pop`).
 * The player drags from the cannon to aim, then releases to fire one bubble per turn.
 * Matching groups of 3+ same-colored bubbles pop and clear the cluster.
 */
@Composable
fun BubblesPopGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { BubblesPopController() }
    val config = remember(difficulty) { controller.configFor(difficulty, BubblesVariant.TURN_BASED) }
    val machine = remember { BubblesPopStateMachine(BubblesVariant.TURN_BASED) }
    val phase by machine.phase.collectAsState()

    var gameState by remember { mutableStateOf(controller.initialGridState(config)) }
    var aimAngle by remember { mutableFloatStateOf(0f) }
    var isAiming by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { machine.startGame() }

    // GameLoop drives the FIRE phase (bubble in flight)
    GameLoop(running = phase == BubblesPopPhase.FIRE) { dt ->
        val (newState, event) = controller.stepGrid(gameState, config, dt)
        gameState = newState
        // Only signal resolution when the bubble has actually attached (flying == null)
        if (newState.flying == null) {
            handleGridEvent(event, newState, machine)
        }
    }

    val bestScore = gameState.bestScore.coerceAtLeast(gameState.score)

    GameScaffold(
        title = "Bubbles Pop",
        onExit = onExit,
        hud = {
            GameHud(
                left = "Score: ${gameState.score}",
                center = "Level ${gameState.level}",
                right = if (gameState.missStreak > 0) "Miss: ${gameState.missStreak}⚠" else "Ready",
            )
        },
        status = {
            when (phase) {
                BubblesPopPhase.LEVEL_COMPLETE -> Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    VictoryPanel(
                        score = gameState.score,
                        bestScore = bestScore,
                        stars = starsForScore(gameState.score, gameState.level),
                        onReplay = {
                            gameState = controller.initialGridState(config)
                            machine.resetGame(); machine.startGame()
                        },
                        onMenu = onExit,
                        headline = "Level ${gameState.level} Complete!",
                    )
                    Button(onClick = {
                        gameState = controller.nextLevelGrid(gameState, config)
                        machine.nextLevel()
                    }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Next Level")
                    }
                }
                BubblesPopPhase.GAME_OVER -> DefeatPanel(
                    score = gameState.score,
                    bestScore = bestScore,
                    onTryAgain = {
                        gameState = controller.initialGridState(config)
                        machine.resetGame(); machine.startGame()
                    },
                    onMenu = onExit,
                )
                BubblesPopPhase.AIM -> Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Drag board to aim • Release to fire",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Row {
                        Text("Next: ", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = gameState.nextBubble.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                else -> {}
            }
        },
    ) {
        val canInteract = phase == BubblesPopPhase.AIM
        BubblesPopBoard(
            gameState = gameState,
            controller = controller,
            aimAngle = aimAngle,
            showAimPreview = canInteract && isAiming,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(canInteract) {
                    if (!canInteract) return@pointerInput
                    detectDragGestures(
                        onDragStart = { isAiming = true },
                        onDragEnd = {
                            isAiming = false
                            val fired = controller.fireCannon(gameState, aimAngle)
                            if (fired.flying != null) {
                                gameState = fired
                                machine.bubbleFired()
                            }
                        },
                        onDragCancel = { isAiming = false },
                        onDrag = { change, _ ->
                            val cx = size.width / 2f
                            val cy = size.height * BubblesPopController.CANNON_Y
                            val dx = change.position.x - cx
                            val dy = change.position.y - cy
                            aimAngle = atan2(dx, -dy).toFloat()
                                .coerceIn((-PI * 0.45f).toFloat(), (PI * 0.45f).toFloat())
                        },
                    )
                }
                .pointerInput(canInteract) {
                    if (!canInteract) return@pointerInput
                    detectTapGestures { tap ->
                        val cx = size.width / 2f
                        val cy = size.height * BubblesPopController.CANNON_Y
                        aimAngle = atan2(tap.x - cx, cy - tap.y).toFloat()
                            .coerceIn((-PI * 0.45f).toFloat(), (PI * 0.45f).toFloat())
                        val fired = controller.fireCannon(gameState, aimAngle)
                        if (fired.flying != null) {
                            gameState = fired
                            machine.bubbleFired()
                        }
                    }
                },
        )
    }
}

@Composable
internal fun BubblesPopBoard(
    gameState: BubblesGridState,
    controller: BubblesPopController,
    aimAngle: Float,
    showAimPreview: Boolean,
    modifier: Modifier = Modifier,
) {
    val r = BubblesPopController.BUBBLE_RADIUS
    val dangerY = BubblesPopController.DANGER_LINE_Y
    val cannonX = BubblesPopController.CANNON_X
    val cannonY = BubblesPopController.CANNON_Y

    // Capture theme-independent colors before Canvas
    val bgColor = GameCourt
    val dangerColor = Color(0xFFE03131).copy(alpha = 0.5f)
    val cannonColor = GameNeutral
    val aimDotColor = GameAccent.copy(alpha = 0.6f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Background
        drawRect(bgColor, size = Size(w, h))

        // Danger line
        drawRect(
            dangerColor,
            topLeft = Offset(0f, dangerY * h - 2f),
            size = Size(w, 4f),
        )

        // Grid bubbles
        gameState.grid.values.forEach { cell ->
            val pos = controller.cellPosition(cell.col, cell.row, gameState.topOffset)
            val cx = pos.x * w
            val cy = pos.y * h
            val radius = r * w
            val color = bubbleColor(cell.color)
            drawCircle(color, radius, Offset(cx, cy))
            // Overlay for special types
            when (cell.type) {
                BubbleType.BOMB -> drawCircle(Color.Black.copy(alpha = 0.4f), radius * 0.5f, Offset(cx, cy))
                BubbleType.STONE -> drawCircle(GameNeutral.copy(alpha = 0.7f), radius, Offset(cx, cy))
                BubbleType.RAINBOW -> drawCircle(
                    Color.White.copy(alpha = 0.3f), radius, Offset(cx, cy),
                    style = Stroke(width = 3f)
                )
                BubbleType.POWER_UP -> drawCircle(GameAccent.copy(alpha = 0.5f), radius * 0.5f, Offset(cx, cy))
                else -> {}
            }
            // Highlight
            drawCircle(Color.White.copy(alpha = 0.25f), radius * 0.45f, Offset(cx - radius * 0.25f, cy - radius * 0.3f))
        }

        // Flying bubble
        gameState.flying?.let { fb ->
            val fc = bubbleColor(fb.color)
            drawCircle(fc, r * w, Offset(fb.x * w, fb.y * h))
            drawCircle(Color.White.copy(alpha = 0.3f), r * w * 0.4f, Offset(fb.x * w - r * w * 0.2f, fb.y * h - r * w * 0.3f))
        }

        // Aim preview dots
        if (showAimPreview) {
            var px = cannonX
            var py = cannonY
            var pdx = sin(aimAngle)
            var pdy = -cos(aimAngle)
            var dotCount = 0
            while (py > 0f && dotCount < 40) {
                px += pdx * 0.04f
                py += pdy * 0.04f
                if (px < r) { px = r * 2f - px; pdx = -pdx }
                if (px > 1f - r) { px = (1f - r) * 2f - px; pdx = -pdx }
                if (dotCount % 3 == 0) {
                    drawCircle(aimDotColor, r * w * 0.4f, Offset(px * w, py * h))
                }
                dotCount++
            }
        }

        // Cannon
        val cx = cannonX * w
        val cy = cannonY * h
        val barrelLen = r * w * 3f
        val bx = cx + sin(aimAngle) * barrelLen
        val by = cy - cos(aimAngle) * barrelLen
        drawLine(cannonColor, Offset(cx, cy), Offset(bx, by), strokeWidth = r * w * 1.5f)
        drawCircle(cannonColor, r * w * 1.4f, Offset(cx, cy))

        // Cannon bubble preview
        val cb = gameState.cannonBubble
        drawCircle(bubbleColor(cb), r * w, Offset(cx, cy))

        // Next bubble preview (bottom-left)
        drawCircle(bubbleColor(gameState.nextBubble), r * w * 0.8f, Offset(r * w * 2f, h - r * w * 2f))
    }
}

private fun handleGridEvent(
    event: BubblesGridEvent,
    state: BubblesGridState,
    machine: BubblesPopStateMachine,
) {
    when (event) {
        is BubblesGridEvent.ClusterEmpty -> machine.clusterCleared()
        is BubblesGridEvent.ClusterCrossedDangerLine -> machine.gameOver()
        // BubblePopped, BubbleFallen, or None when flying == null means the turn resolved normally
        is BubblesGridEvent.BubblePopped,
        is BubblesGridEvent.BubbleFallen,
        is BubblesGridEvent.None -> machine.bubbleResolved()
        is BubblesGridEvent.LifeLost -> {} // turn-based has only 1 life
    }
}

internal fun bubbleColor(color: BubbleColor): Color = when (color) {
    BubbleColor.CYAN -> Aqua3
    BubbleColor.RED -> GameEnemy
    BubbleColor.YELLOW -> GameAccent
    BubbleColor.ORANGE -> GameHazard
    BubbleColor.GRAY -> GameNeutral
    BubbleColor.TEAL -> GamePlayer
    BubbleColor.LAVENDER -> Aqua4
}

private fun starsForScore(score: Int, level: Int): Int = when {
    score >= 1000 * level -> 3
    score >= 500 * level -> 2
    else -> 1
}
