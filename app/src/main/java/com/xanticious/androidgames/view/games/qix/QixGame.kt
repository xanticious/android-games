package com.xanticious.androidgames.view.games.qix

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.qix.QixController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.JoystickInput
import com.xanticious.androidgames.model.games.qix.CellState
import com.xanticious.androidgames.model.games.qix.DrawSpeed
import com.xanticious.androidgames.model.games.qix.GridPos
import com.xanticious.androidgames.model.games.qix.QIX_COLS
import com.xanticious.androidgames.model.games.qix.QIX_ROWS
import com.xanticious.androidgames.model.games.qix.QixEvent
import com.xanticious.androidgames.model.games.qix.QixInput
import com.xanticious.androidgames.model.games.qix.QixState
import com.xanticious.androidgames.state.games.qix.QixPhase
import com.xanticious.androidgames.state.games.qix.QixStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.FloatingJoystick
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlinx.coroutines.delay

/**
 * Qix — territory-capture arcade game (`design/action-games/qix`).
 *
 * The player draws lines across the unclaimed playfield; reconnecting to the
 * boundary encloses a region which is claimed (the side NOT containing the Qix).
 * Reach [QixState.TOTAL_INNER_CELLS] × target% to complete the level.
 *
 * Victory/defeat UI lives in the [GameScaffold] `status` slot — never over the board.
 */
@Composable
fun QixGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { QixController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { QixStateMachine() }
    val phase by machine.phase.collectAsState()

    var state by remember { mutableStateOf(QixState.initial()) }
    var joystickInput by remember { mutableStateOf(JoystickInput.NONE) }
    var drawSpeed by remember { mutableStateOf(DrawSpeed.SLOW) }

    // Kick off the state machine when the composable first enters the composition.
    LaunchedEffect(Unit) { machine.startGame() }

    // After a collision, pause briefly so the player sees the "life lost" banner,
    // then resume (or the game-over path is already handled via livesExhausted).
    LaunchedEffect(phase) {
        if (phase == QixPhase.LIFE_LOST) {
            delay(1500L)
            machine.respawned()
        }
    }

    // Core game loop — runs only while the game is actively playing.
    GameLoop(running = phase == QixPhase.PLAYING) { dt ->
        val input = QixInput(dx = joystickInput.dx, dy = joystickInput.dy, drawSpeed = drawSpeed)
        val step = controller.step(state, config, dt, input)
        state = step.state
        when (step.event) {
            QixEvent.LifeLost ->
                if (step.state.lives <= 0) machine.livesExhausted()
                else machine.collisionOccurred()
            QixEvent.TerritoryClaimedLevelComplete -> machine.levelAchieved()
            else -> {}
        }
    }

    val claimedPct = controller.claimedPercent(state)
    val stars = when {
        claimedPct >= 95 -> 3
        claimedPct >= 85 -> 2
        else -> 1
    }

    GameScaffold(
        title = "Qix",
        onExit = onExit,
        hud = {
            GameHud(
                left = "${state.score}",
                center = "$claimedPct% / ${config.targetClaimedPercent}%",
                right = "♥".repeat(state.lives.coerceAtLeast(0))
            )
        },
        status = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Draw-speed toggle — visible while the game is running or briefly paused.
                if (phase == QixPhase.PLAYING || phase == QixPhase.LIFE_LOST) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = drawSpeed == DrawSpeed.SLOW,
                            onClick = { drawSpeed = DrawSpeed.SLOW },
                            label = { Text("Slow Draw") }
                        )
                        FilterChip(
                            selected = drawSpeed == DrawSpeed.FAST,
                            onClick = { drawSpeed = DrawSpeed.FAST },
                            label = { Text("Fast Draw  ×2") }
                        )
                    }
                }
                if (phase == QixPhase.LIFE_LOST && state.lives > 0) {
                    Text(
                        text = "Life lost — ${state.lives} remaining…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (phase == QixPhase.LEVEL_COMPLETE) {
                    VictoryPanel(
                        score = state.score,
                        bestScore = state.score,
                        stars = stars,
                        onReplay = onExit,
                        onMenu = onExit,
                        headline = "Level Complete!"
                    )
                }
                if (phase == QixPhase.GAME_OVER) {
                    DefeatPanel(
                        score = state.score,
                        bestScore = state.score,
                        onTryAgain = onExit,
                        onMenu = onExit,
                        headline = "Game Over"
                    )
                }
            }
        }
    ) {
        // ── Board ─────────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellW = size.width / QIX_COLS
                val cellH = size.height / QIX_ROWS

                // Background: unclaimed interior
                drawRect(color = GameCourt, size = size)

                // Boundary ring as four solid rectangles.
                drawRect(color = GameCourtLine, topLeft = Offset(0f, 0f),
                    size = Size(size.width, cellH))
                drawRect(color = GameCourtLine, topLeft = Offset(0f, size.height - cellH),
                    size = Size(size.width, cellH))
                drawRect(color = GameCourtLine, topLeft = Offset(0f, cellH),
                    size = Size(cellW, size.height - 2f * cellH))
                drawRect(color = GameCourtLine, topLeft = Offset(size.width - cellW, cellH),
                    size = Size(cellW, size.height - 2f * cellH))

                // Interior cells: only draw CLAIMED and TRAIL (UNCLAIMED = background).
                for (row in 1 until QIX_ROWS - 1) {
                    for (col in 1 until QIX_COLS - 1) {
                        when (state.cellAt(GridPos(col, row))) {
                            CellState.CLAIMED -> drawRect(
                                color = GamePlayer.copy(alpha = 0.55f),
                                topLeft = Offset(col * cellW, row * cellH),
                                size = Size(cellW, cellH)
                            )
                            CellState.TRAIL -> drawRect(
                                color = GameAccent,
                                topLeft = Offset(col * cellW, row * cellH),
                                size = Size(cellW, cellH)
                            )
                            else -> {}
                        }
                    }
                }

                // Sparx: bright dots on the boundary perimeter.
                for (sparx in state.sparx) {
                    val sp = controller.perimeterCellAt(sparx.perimeterIndex)
                    val cx = (sp.col + 0.5f) * cellW
                    val cy = (sp.row + 0.5f) * cellH
                    drawCircle(color = GameAccent, radius = cellW * 0.75f,
                        center = Offset(cx, cy))
                    drawCircle(color = GameAccent.copy(alpha = 0.35f), radius = cellW * 1.3f,
                        center = Offset(cx, cy))
                }

                // Qix: glowing diamond shape in unclaimed space.
                for (qix in state.qix) {
                    val cx = qix.x * cellW
                    val cy = qix.y * cellH
                    val r = cellW * 1.6f
                    val diamond = Path().apply {
                        moveTo(cx, cy - r)
                        lineTo(cx + r, cy)
                        lineTo(cx, cy + r)
                        lineTo(cx - r, cy)
                        close()
                    }
                    drawPath(diamond, color = GameHazard.copy(alpha = 0.3f))
                    drawPath(diamond, color = GameHazard, style = Stroke(width = 3f))
                }

                // Player cursor: a bright square, semi-transparent while invincible.
                val pAlpha = if (state.invincibleTimer > 0f) 0.45f else 1f
                drawRect(
                    color = GamePlayer.copy(alpha = pAlpha),
                    topLeft = Offset(state.playerPos.col * cellW, state.playerPos.row * cellH),
                    size = Size(cellW, cellH)
                )
            }

            // Floating joystick — bottom-left quadrant so the right side stays visible.
            FloatingJoystick(
                onInput = { joystickInput = it },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.5f)
                    .height(160.dp),
                ringDiameter = 130.dp,
                accent = GamePlayer
            )
        }
    }
}
