package com.xanticious.androidgames.view.games.dominosa

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.dominosa.DominosaController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.dominosa.DominoPlacement
import com.xanticious.androidgames.model.games.dominosa.DominosaState
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import com.xanticious.androidgames.ui.theme.PuzzleCell
import com.xanticious.androidgames.ui.theme.PuzzleGiven
import com.xanticious.androidgames.ui.theme.PuzzleHighlight
import com.xanticious.androidgames.ui.theme.PuzzlePlayer
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.puzzle.OptionChips
import com.xanticious.androidgames.view.common.puzzle.PuzzleActionBar
import com.xanticious.androidgames.view.common.puzzle.PuzzleBoard
import com.xanticious.androidgames.view.common.puzzle.PuzzleHowToPlayScreen
import com.xanticious.androidgames.view.common.puzzle.PuzzleSettingsScreen

/**
 * Dominosa — cover the pip-number grid with every domino of the 0..N set exactly
 * once (`design/puzzle-games/dominosa`). Self-configured: owns Settings, How to
 * Play, and board screens via the shared [PuzzleStateMachine].
 */
@Composable
fun DominosaGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { DominosaController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    val defaultN = when (difficulty) {
        GameDifficulty.EASY -> 4
        GameDifficulty.MEDIUM -> 6
        GameDifficulty.HARD -> 8
    }
    var selectedN by rememberSaveable { mutableIntStateOf(defaultN) }
    var showConflicts by rememberSaveable { mutableStateOf(true) }
    var state by remember { mutableStateOf(controller.newGame(defaultN)) }

    // Drag gesture tracking: first cell touched during a drag gesture.
    var dragStart by remember { mutableStateOf<GridPos?>(null) }

    fun deal() {
        state = controller.newGame(selectedN)
        dragStart = null
    }

    fun onCellAction(pos: GridPos) {
        state = controller.tapCell(state, pos)
        if (controller.isSolved(state)) machine.solved()
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Dominosa", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Dominosa",
                onHowToPlay = machine::openHowToPlay,
                onStart = { deal(); machine.startGame() }
            ) {
                OptionChips(
                    label = "Puzzle size (N)",
                    options = controller.sizes,
                    selected = selectedN,
                    onSelect = { selectedN = it },
                    labelOf = { "N=$it" }
                )
                OptionChips(
                    label = "Highlight conflicts",
                    options = listOf(true, false),
                    selected = showConflicts,
                    onSelect = { showConflicts = it },
                    labelOf = { if (it) "On" else "Off" }
                )
            }
        }

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Dominosa", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                Text("Cover every cell with dominoes so each number-pair (0-0, 0-1, … N-N) is used exactly once.")
                Text("Tap a cell to select it, then tap an adjacent cell to place a domino joining them.")
                Text("You can also drag from one cell to an adjacent cell to place a domino.")
                Text("Tap any placed domino to remove it.")
                Text("Use Undo to revert your last action, Hint to place one forced domino, or New for a fresh puzzle.")
            }
        }

        else -> {
            val solved = phase == PuzzlePhase.SOLVED
            val conflicts: Set<DominoPlacement> =
                if (showConflicts && !solved) controller.conflictPlacements(state) else emptySet()
            val placed = controller.placedCount(state)
            val total = controller.totalCount(state)

            GameScaffold(
                title = "Dominosa",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "N=${state.n}",
                        center = "${state.rows}×${state.cols}",
                        right = "$placed/$total"
                    )
                },
                status = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (solved) {
                            VictoryPanel(
                                score = placed,
                                bestScore = placed,
                                stars = 3,
                                onReplay = { deal(); machine.retry() },
                                onMenu = machine::newGame,
                                headline = "Solved!",
                                primaryLabel = "New Puzzle"
                            )
                        } else {
                            PuzzleActionBar(
                                status = "${total - placed} pairs left",
                                onUndo = { state = controller.undo(state) },
                                undoEnabled = state.canUndo,
                                onHint = {
                                    state = controller.hint(state)
                                    if (controller.isSolved(state)) machine.solved()
                                },
                                onNew = { deal() }
                            )
                        }
                    }
                }
            ) {
                DominosaBoard(
                    state = state,
                    conflicts = conflicts,
                    enabled = !solved,
                    onCellTap = ::onCellAction,
                    onCellDrag = { pos ->
                        if (dragStart == null) dragStart = pos
                    },
                    onDragEnd = {
                        val start = dragStart
                        dragStart = null
                        if (start != null) {
                            // dragStart is still pointing at the original cell;
                            // the last onCellDrag gave us the destination.
                            // We rely on state.selected being set to start (from
                            // first drag event) then a tap-style placement fires.
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DominosaBoard(
    state: DominosaState,
    conflicts: Set<DominoPlacement>,
    enabled: Boolean,
    onCellTap: (GridPos) -> Unit,
    onCellDrag: (GridPos) -> Unit,
    onDragEnd: () -> Unit
) {
    val measurer = rememberTextMeasurer()
    val errorColor = MaterialTheme.colorScheme.error

    // Drag-to-place: remember start cell across recompositions.
    var dragCurrent by remember { mutableStateOf<GridPos?>(null) }
    var dragAnchor by remember { mutableStateOf<GridPos?>(null) }

    PuzzleBoard(
        rows = state.rows,
        cols = state.cols,
        maxCellSize = 72.dp,
        drawGridLines = true,
        onCellTap = if (enabled) onCellTap else null,
        onCellDrag = if (enabled) { pos ->
            if (dragAnchor == null) {
                dragAnchor = pos
                dragCurrent = pos
            } else {
                dragCurrent = pos
            }
            onCellDrag(pos)
        } else null,
        onDragEnd = if (enabled) {
            {
                val anchor = dragAnchor
                val current = dragCurrent
                dragAnchor = null
                dragCurrent = null
                onDragEnd()
                if (anchor != null && current != null && anchor != current) {
                    onCellTap(anchor)
                    onCellTap(current)
                }
            }
        } else null
    ) {
        val pos = current
        val placement = state.dominoAt(pos)
        val isSelected = state.selected == pos
        val inset = cellSize * 0.06f

        when {
            placement != null -> {
                val isCell1 = placement.cell1 == pos
                val isConflict = conflicts.contains(placement)
                val capsuleColor = if (isConflict) errorColor else PuzzleHighlight

                if (isCell1) {
                    // Draw the rounded capsule spanning both cells from cell1.
                    val isHoriz = placement.cell1.row == placement.cell2.row
                    val capsuleW = if (isHoriz) cellSize * 2f - inset * 2 else cellSize - inset * 2
                    val capsuleH = if (isHoriz) cellSize - inset * 2 else cellSize * 2f - inset * 2
                    drawRoundRect(
                        color = capsuleColor,
                        topLeft = Offset(topLeft.x + inset, topLeft.y + inset),
                        size = Size(capsuleW, capsuleH),
                        cornerRadius = CornerRadius(cellSize * 0.3f)
                    )
                }
                drawPipNumber(measurer, state.pipAt(pos), PuzzleCell, cellSize, center, density)
            }

            isSelected -> {
                drawRoundRect(
                    color = PuzzlePlayer,
                    topLeft = Offset(topLeft.x + inset, topLeft.y + inset),
                    size = Size(cellSize - inset * 2, cellSize - inset * 2),
                    cornerRadius = CornerRadius(cellSize * 0.2f)
                )
                drawPipNumber(measurer, state.pipAt(pos), PuzzleCell, cellSize, center, density)
            }

            else -> {
                val tileInset = cellSize * 0.04f
                drawRoundRect(
                    color = PuzzleGiven,
                    topLeft = Offset(topLeft.x + tileInset, topLeft.y + tileInset),
                    size = Size(cellSize - tileInset * 2, cellSize - tileInset * 2),
                    cornerRadius = CornerRadius(cellSize * 0.12f)
                )
                drawPipNumber(measurer, state.pipAt(pos), PuzzleCell, cellSize, center, density)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPipNumber(
    measurer: androidx.compose.ui.text.TextMeasurer,
    pip: Int,
    color: Color,
    cellSize: Float,
    center: Offset,
    density: Float
) {
    val label = pip.toString()
    val style = TextStyle(
        color = color,
        fontSize = (cellSize * 0.40f / density).sp,
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
