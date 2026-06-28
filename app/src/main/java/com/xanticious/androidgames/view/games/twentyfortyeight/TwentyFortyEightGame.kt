package com.xanticious.androidgames.view.games.twentyfortyeight

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.twentyfortyeight.TwentyFortyEightController
import com.xanticious.androidgames.controller.games.twentyfortyeight.abbreviate
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.puzzle.Direction
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.model.games.puzzle.starsFor
import com.xanticious.androidgames.model.games.twentyfortyeight.TwentyFortyEightState
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.PuzzleCell
import com.xanticious.androidgames.ui.theme.PuzzleGiven
import com.xanticious.androidgames.ui.theme.PuzzleHighlight
import com.xanticious.androidgames.ui.theme.PuzzleHueBlue
import com.xanticious.androidgames.ui.theme.PuzzleHueOrange
import com.xanticious.androidgames.ui.theme.PuzzleHuePink
import com.xanticious.androidgames.ui.theme.PuzzleHueRed
import com.xanticious.androidgames.ui.theme.PuzzleHueViolet
import com.xanticious.androidgames.ui.theme.PuzzleHueYellow
import com.xanticious.androidgames.ui.theme.PuzzlePlayer
import com.xanticious.androidgames.ui.theme.PuzzlePlayerAlt
import com.xanticious.androidgames.ui.theme.PuzzleSolved
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.puzzle.OptionChips
import com.xanticious.androidgames.view.common.puzzle.PuzzleActionBar
import com.xanticious.androidgames.view.common.puzzle.PuzzleBoard
import com.xanticious.androidgames.view.common.puzzle.PuzzleHowToPlayScreen
import com.xanticious.androidgames.view.common.puzzle.PuzzleSettingsScreen
import kotlin.math.abs

/**
 * 2048 — slide tiles and merge them to reach 2048 (`design/puzzle-games/2048`).
 *
 * Self-configured: owns its Settings, How to Play and board screens via the shared
 * [PuzzleStateMachine].
 */
@Composable
fun TwentyFortyEightGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { TwentyFortyEightController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    val defaultSize = when (difficulty) {
        GameDifficulty.EASY -> 4
        GameDifficulty.MEDIUM -> 5
        GameDifficulty.HARD -> 6
    }
    var selectedSize by rememberSaveable { mutableIntStateOf(defaultSize) }
    var state by remember { mutableStateOf(controller.newGame(selectedSize)) }

    fun deal() {
        state = controller.newGame(selectedSize)
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "2048", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "2048",
                onHowToPlay = machine::openHowToPlay,
                onStart = { deal(); machine.startGame() }
            ) {
                OptionChips(
                    label = "Board size",
                    options = controller.sizes,
                    selected = selectedSize,
                    onSelect = { selectedSize = it },
                    labelOf = { "${it}×$it" }
                )
            }
        }

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "2048", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                Text("Swipe up, down, left or right to slide every tile on the board in that direction.")
                Text("When two tiles with the same number collide, they merge into one tile with double the value. A tile can only merge once per swipe.")
                Text("After each move, a new tile (2 or 4) appears in a random empty cell.")
                Text("Reach the 2048 tile to win! The game ends when the board is full and no merge is possible.")
                Text("Use Undo to revert your last move.")
            }
        }

        else -> {
            val solved = phase == PuzzlePhase.SOLVED
            val failed = phase == PuzzlePhase.FAILED

            GameScaffold(
                title = "2048",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "Score ${state.score}",
                        center = "${selectedSize}×$selectedSize",
                        right = "Best ${state.bestTile}"
                    )
                },
                status = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        when {
                            solved -> VictoryPanel(
                                score = state.score.toInt(),
                                bestScore = state.score.toInt(),
                                stars = starsFor(
                                    earned = true,
                                    good = state.moves <= 200,
                                    great = state.moves <= 100
                                ),
                                onReplay = { deal(); machine.retry() },
                                onMenu = machine::newGame,
                                headline = "You reached 2048!",
                                primaryLabel = "Keep Going"
                            )
                            failed -> DefeatPanel(
                                score = state.score.toInt(),
                                bestScore = state.score.toInt(),
                                onTryAgain = { deal(); machine.retry() },
                                onMenu = machine::newGame,
                                headline = "No moves left!"
                            )
                            else -> PuzzleActionBar(
                                status = "Moves ${state.moves}",
                                onUndo = {
                                    state = controller.undo(state)
                                },
                                undoEnabled = state.canUndo,
                                onNew = { deal() }
                            )
                        }
                    }
                }
            ) {
                TwentyFortyEightBoard(
                    state = state,
                    enabled = !solved && !failed,
                    onSwipe = { direction ->
                        val result = controller.move(state, direction)
                        if (result.moved) {
                            val spawned = controller.spawnTile(result.state)
                            state = spawned
                            when {
                                controller.hasWon(spawned) && !solved -> machine.solved()
                                !controller.canMove(spawned) -> machine.failed()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TwentyFortyEightBoard(
    state: TwentyFortyEightState,
    enabled: Boolean,
    onSwipe: (Direction) -> Unit
) {
    val n = state.size
    val measurer = rememberTextMeasurer()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                var dragAccum = Offset.Zero
                val swipeThresholdPx = 40.dp.toPx()
                detectDragGestures(
                    onDragStart = { dragAccum = Offset.Zero },
                    onDragEnd = {
                        val dx = dragAccum.x
                        val dy = dragAccum.y
                        if (maxOf(abs(dx), abs(dy)) >= swipeThresholdPx) {
                            val direction = if (abs(dx) >= abs(dy)) {
                                if (dx > 0) Direction.RIGHT else Direction.LEFT
                            } else {
                                if (dy > 0) Direction.DOWN else Direction.UP
                            }
                            onSwipe(direction)
                        }
                        dragAccum = Offset.Zero
                    },
                    onDragCancel = { dragAccum = Offset.Zero }
                ) { _, delta -> dragAccum += delta }
            }
    ) {
        PuzzleBoard(
            rows = n,
            cols = n,
            drawGridLines = false,
            onCellTap = null
        ) {
            val value = state.tile(current.row, current.col)
            val inset = cellSize * 0.05f
            val rect = rect(inset)
            val bg = tileColor(value)
            drawRoundRect(
                color = bg,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                cornerRadius = CornerRadius(cellSize * 0.12f)
            )
            if (value > 0L) {
                val label = abbreviate(value)
                val textColor = tileLabelColor(value)
                val baseFontSp = (cellSize * 0.32f / density).sp
                val fontSp = if (label.length > 3) (cellSize * 0.24f / density).sp else baseFontSp
                val style = TextStyle(
                    color = textColor,
                    fontSize = fontSp,
                    fontWeight = FontWeight.Bold
                )
                val layout = measurer.measure(AnnotatedString(label), style)
                drawText(
                    layout,
                    topLeft = Offset(
                        center.x - layout.size.width / 2f,
                        center.y - layout.size.height / 2f
                    )
                )
            }
        }
    }
}

private fun tileColor(value: Long): Color = when {
    value <= 0L -> PuzzleCell
    value == 2L -> PuzzlePlayerAlt
    value == 4L -> PuzzlePlayer
    value == 8L -> PuzzleSolved
    value == 16L -> PuzzleHighlight
    value == 32L -> PuzzleHueBlue
    value == 64L -> PuzzleGiven
    value == 128L -> PuzzleHueViolet
    value == 256L -> PuzzleHuePink
    value == 512L -> PuzzleHueRed
    value == 1024L -> PuzzleHueOrange
    value == 2048L -> GameAccent
    value == 4096L -> PuzzleHueYellow
    else -> GameNeutral
}

private fun tileLabelColor(value: Long): Color = when {
    value <= 4L -> PuzzleGiven
    else -> PuzzlePlayerAlt
}
