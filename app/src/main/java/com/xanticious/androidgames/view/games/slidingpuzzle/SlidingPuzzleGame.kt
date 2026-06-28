package com.xanticious.androidgames.view.games.slidingpuzzle

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.slidingpuzzle.SlidingPuzzleController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.model.games.slidingpuzzle.SlidingPuzzleState
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import com.xanticious.androidgames.ui.theme.PuzzleCell
import com.xanticious.androidgames.ui.theme.PuzzleHighlight
import com.xanticious.androidgames.ui.theme.PuzzleSolved
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.puzzle.OptionChips
import com.xanticious.androidgames.view.common.puzzle.PuzzleActionBar
import com.xanticious.androidgames.view.common.puzzle.PuzzleBoard
import com.xanticious.androidgames.view.common.puzzle.PuzzleHowToPlayScreen
import com.xanticious.androidgames.view.common.puzzle.PuzzleSettingsScreen

/**
 * Sliding Puzzle — slide tiles into the gap to restore ascending order
 * (`design/puzzle-games/sliding-puzzle`). Self-configured: owns its Settings,
 * How to Play and board screens via the shared puzzle [PuzzleStateMachine].
 */
@Composable
fun SlidingPuzzleGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { SlidingPuzzleController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    val defaultSize = when (difficulty) {
        GameDifficulty.EASY -> 3
        GameDifficulty.MEDIUM -> 4
        GameDifficulty.HARD -> 5
    }
    var selectedSize by remember { mutableIntStateOf(defaultSize) }
    var state by remember { mutableStateOf(SlidingPuzzleState.solved(defaultSize)) }

    fun deal() {
        state = controller.newGame(selectedSize)
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Sliding Puzzle", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Sliding Puzzle",
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

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Sliding Puzzle", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                Text("Tap a tile in the same row or column as the empty space to slide it (and any tiles between it and the gap) toward the gap.")
                Text("Arrange the tiles in order — 1, 2, 3, … — with the empty space in the bottom-right corner.")
                Text("Use Undo to revert the last slide, or New to deal a fresh scramble.")
            }
        }

        else -> {
            val solved = phase == PuzzlePhase.SOLVED
            GameScaffold(
                title = "Sliding Puzzle",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "Moves ${state.moves}",
                        center = "${selectedSize}×$selectedSize",
                        right = "${controller.correctlyPlaced(state)}/${selectedSize * selectedSize - 1}"
                    )
                },
                status = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (solved) {
                            VictoryPanel(
                                score = state.moves,
                                bestScore = state.moves,
                                stars = 3,
                                onReplay = { deal(); machine.retry() },
                                onMenu = machine::newGame,
                                headline = "Solved!",
                                primaryLabel = "New Puzzle"
                            )
                        } else {
                            PuzzleActionBar(
                                status = "Moves ${state.moves}",
                                onUndo = { state = controller.undo(state) },
                                undoEnabled = state.canUndo,
                                onNew = { deal() }
                            )
                        }
                    }
                }
            ) {
                SlidingBoard(
                    state = state,
                    enabled = !solved,
                    onTileTap = { pos ->
                        state = controller.slide(state, pos)
                        if (controller.isSolved(state)) machine.solved()
                    }
                )
            }
        }
    }
}

@Composable
private fun SlidingBoard(
    state: SlidingPuzzleState,
    enabled: Boolean,
    onTileTap: (com.xanticious.androidgames.model.games.puzzle.GridPos) -> Unit
) {
    val n = state.size
    val goal = SlidingPuzzleState.goalTiles(n)
    val measurer = rememberTextMeasurer()
    PuzzleBoard(
        rows = n,
        cols = n,
        drawGridLines = false,
        onCellTap = if (enabled) onTileTap else null
    ) {
        val index = current.row * n + current.col
        val value = state.tiles[index]
        if (value == 0) return@PuzzleBoard
        val inset = cellSize * 0.04f
        val rect = rect(inset)
        val placed = state.tiles[index] == goal[index]
        drawRoundRect(
            color = if (placed) PuzzleSolved else PuzzleHighlight,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cellSize * 0.12f)
        )
        val label = value.toString()
        val style = TextStyle(
            color = PuzzleCell,
            fontSize = (cellSize * 0.36f / density).sp,
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
