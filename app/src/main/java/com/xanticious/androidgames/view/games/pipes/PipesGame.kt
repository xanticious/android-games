package com.xanticious.androidgames.view.games.pipes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import com.xanticious.androidgames.controller.games.pipes.PipesController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.pipes.PipesState
import com.xanticious.androidgames.model.games.puzzle.Direction
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.PuzzleCell
import com.xanticious.androidgames.ui.theme.PuzzleGridLine
import com.xanticious.androidgames.ui.theme.PuzzleGiven
import com.xanticious.androidgames.ui.theme.PuzzleHighlight
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.puzzle.OptionChips
import com.xanticious.androidgames.view.common.puzzle.PuzzleActionBar
import com.xanticious.androidgames.view.common.puzzle.PuzzleBoard
import com.xanticious.androidgames.view.common.puzzle.PuzzleHowToPlayScreen
import com.xanticious.androidgames.view.common.puzzle.PuzzleSettingsScreen

/**
 * Pipes — rotate pipe tiles to build a fully connected leak-free network from the
 * source (`design/puzzle-games/pipes`). Self-configured via [PuzzleStateMachine].
 */
@Composable
fun PipesGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { PipesController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    val defaultSize = when (difficulty) {
        GameDifficulty.EASY   -> 5
        GameDifficulty.MEDIUM -> 8
        GameDifficulty.HARD   -> 11
    }
    var selectedSize by remember { mutableStateOf(defaultSize) }
    var state by remember { mutableStateOf(controller.newGame(defaultSize)) }

    fun deal() {
        state = controller.newGame(selectedSize)
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Pipes", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Pipes",
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

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Pipes", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                Text("Tap a tile to rotate it 90° clockwise. Long-press to rotate counter-clockwise.")
                Text("Connect every pipe segment into one network fed from the glowing source tile — no open ends, no leaks.")
                Text("Pipes fill with colour as they connect to the source. The puzzle is solved when the whole network lights up.")
                Text("Use Undo to reverse the last rotation, Reset to restore the starting layout, or New for a fresh puzzle.")
            }
        }

        else -> {
            val solved = phase == PuzzlePhase.SOLVED
            val connected = remember(state.rotations) { controller.connectedCells(state) }
            val total = selectedSize * selectedSize

            GameScaffold(
                title = "Pipes",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "Rot ${state.rotationCount}",
                        center = "${selectedSize}×$selectedSize",
                        right = "Connected ${connected.size}/$total"
                    )
                },
                status = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (solved) {
                            VictoryPanel(
                                score = state.rotationCount,
                                bestScore = state.rotationCount,
                                stars = 3,
                                onReplay = { deal(); machine.retry() },
                                onMenu = machine::newGame,
                                headline = "Network Complete!",
                                primaryLabel = "New Puzzle"
                            )
                        } else {
                            PuzzleActionBar(
                                status = "Connected ${connected.size}/$total",
                                onUndo = { state = controller.undo(state) },
                                undoEnabled = state.canUndo,
                                onReset = { state = controller.reset(state) },
                                onNew = { deal() }
                            )
                        }
                    }
                }
            ) {
                PipesBoard(
                    state = state,
                    connected = connected,
                    enabled = !solved,
                    controller = controller,
                    onTap = { pos ->
                        state = controller.rotate(state, pos, clockwise = true)
                        if (controller.isSolved(state)) machine.solved()
                    },
                    onLongPress = { pos ->
                        state = controller.rotate(state, pos, clockwise = false)
                        if (controller.isSolved(state)) machine.solved()
                    }
                )
            }
        }
    }
}

@Composable
private fun PipesBoard(
    state: PipesState,
    connected: Set<GridPos>,
    enabled: Boolean,
    controller: PipesController,
    onTap: (GridPos) -> Unit,
    onLongPress: (GridPos) -> Unit
) {
    PuzzleBoard(
        rows = state.size,
        cols = state.size,
        drawGridLines = false,
        onCellTap = if (enabled) onTap else null,
        onCellLongPress = if (enabled) onLongPress else null
    ) {
        val isPowered = current in connected
        val isSource = current == state.sourcePos
        val pipeColor = if (isPowered) PuzzleHighlight else PuzzleGridLine
        val strokeWidth = cellSize * 0.3f

        // Subtle cell background
        drawRoundRect(
            color = PuzzleCell,
            topLeft = topLeft,
            size = Size(cellSize, cellSize),
            cornerRadius = CornerRadius(cellSize * 0.08f)
        )

        // Pipe segments: line from center to each edge midpoint
        for (dir in controller.connectorsAt(state, current)) {
            val endOffset = when (dir) {
                Direction.UP    -> Offset(center.x, topLeft.y)
                Direction.DOWN  -> Offset(center.x, topLeft.y + cellSize)
                Direction.LEFT  -> Offset(topLeft.x, center.y)
                Direction.RIGHT -> Offset(topLeft.x + cellSize, center.y)
            }
            drawLine(
                color = pipeColor,
                start = center,
                end = endOffset,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        // Hub dot at center; source uses GameAccent, powered uses PuzzleGiven
        val hubColor = when {
            isSource -> GameAccent
            isPowered -> PuzzleGiven
            else -> PuzzleGridLine
        }
        drawCircle(color = hubColor, radius = cellSize * 0.14f, center = center)
    }
}
