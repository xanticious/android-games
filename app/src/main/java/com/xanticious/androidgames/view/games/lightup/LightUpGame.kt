package com.xanticious.androidgames.view.games.lightup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.lightup.LightUpController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.lightup.LightUpState
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.PuzzleGiven
import com.xanticious.androidgames.ui.theme.PuzzlePlayer
import com.xanticious.androidgames.ui.theme.PuzzlePlayerAlt
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.puzzle.DifficultyChips
import com.xanticious.androidgames.view.common.puzzle.HowToPlaySection
import com.xanticious.androidgames.view.common.puzzle.PuzzleActionBar
import com.xanticious.androidgames.view.common.puzzle.PuzzleBoard
import com.xanticious.androidgames.view.common.puzzle.PuzzleHowToPlayScreen
import com.xanticious.androidgames.view.common.puzzle.PuzzleSettingsScreen

/**
 * Light Up (Akari) — place bulbs to illuminate every white cell without any two
 * bulbs seeing each other (`design/puzzle-games/light-up/light-up-design.md`).
 *
 * Self-configured: owns its Settings, How to Play and board screens via the
 * shared [PuzzleStateMachine].
 */
@Composable
fun LightUpGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { LightUpController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    var selectedDifficulty by remember { mutableStateOf(difficulty) }
    var resetState by remember { mutableStateOf(controller.newGame(difficulty)) }
    var state by remember { mutableStateOf(resetState) }

    fun deal() {
        resetState = controller.newGame(selectedDifficulty)
        state = resetState
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Light Up", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Light Up",
                onHowToPlay = machine::openHowToPlay,
                onStart = { deal(); machine.startGame() }
            ) {
                DifficultyChips(selected = selectedDifficulty, onSelect = { selectedDifficulty = it })
            }
        }

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Light Up", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                HowToPlaySection(title = "Goal") {
                    Text("Fill the grid with light bulbs so every white cell is illuminated and no bulb shines on another bulb.")
                }
                HowToPlaySection(title = "Bulbs and Light") {
                    Text("Tap a white cell to place a bulb. It lights its entire row and column until blocked by a black wall. Tap again to remove the bulb.")
                }
                HowToPlaySection(title = "Wall Clues") {
                    Text("A number on a black wall tells you exactly how many of its four adjacent cells must contain a bulb. Walls without a number have no constraint.")
                }
                HowToPlaySection(title = "Marks") {
                    Text("Long-press a cell to leave an X-mark as a personal note — 'no bulb here'. Marks don't emit light and are ignored when checking the solution. Long-press again to clear.")
                }
                HowToPlaySection(title = "Victory") {
                    Text("The puzzle is solved when every white cell is lit, no two bulbs can see each other, and all numbered walls are satisfied.")
                }
            }
        }

        else -> {
            val solved = phase == PuzzlePhase.SOLVED
            val lit = controller.computeLit(state)
            val conflicting = controller.conflicts(state)
            val whiteCells = state.cells.count { !it.isWall }
            val litCount = state.cells.indices.count { idx ->
                !state.cells[idx].isWall &&
                    GridPos(idx / state.cols, idx % state.cols) in lit
            }

            GameScaffold(
                title = "Light Up",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "Lit $litCount/$whiteCells",
                        center = "${state.rows}×${state.cols}",
                        right = if (conflicting.isNotEmpty())
                            "${conflicting.size} conflict${if (conflicting.size != 1) "s" else ""}"
                        else ""
                    )
                },
                status = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (solved) {
                            VictoryPanel(
                                score = 0,
                                bestScore = 0,
                                stars = 3,
                                onReplay = { deal(); machine.retry() },
                                onMenu = machine::newGame,
                                headline = "Illuminated!",
                                primaryLabel = "New Puzzle"
                            )
                        } else {
                            PuzzleActionBar(
                                status = "Unlit: ${whiteCells - litCount}",
                                onUndo = { state = controller.undo(state) },
                                undoEnabled = state.canUndo,
                                onHint = { state = controller.hint(state) },
                                onReset = { state = resetState },
                                onNew = { deal() }
                            )
                        }
                    }
                }
            ) {
                LightUpBoard(
                    state = state,
                    lit = lit,
                    conflicting = conflicting,
                    enabled = !solved,
                    onCellTap = { pos ->
                        state = controller.toggleBulb(state, pos)
                        if (controller.isSolved(state)) machine.solved()
                    },
                    onCellLongPress = { pos ->
                        state = controller.toggleMark(state, pos)
                    }
                )
            }
        }
    }
}

@Composable
private fun LightUpBoard(
    state: LightUpState,
    lit: Set<GridPos>,
    conflicting: Set<GridPos>,
    enabled: Boolean,
    onCellTap: (GridPos) -> Unit,
    onCellLongPress: (GridPos) -> Unit
) {
    val measurer = rememberTextMeasurer()
    PuzzleBoard(
        rows = state.rows,
        cols = state.cols,
        drawGridLines = false,
        onCellTap = if (enabled) onCellTap else null,
        onCellLongPress = if (enabled) onCellLongPress else null
    ) {
        val cell = state.cellAt(current)
        val isConflict = current in conflicting
        val isLit = current in lit
        val inset = cellSize * 0.04f

        if (cell.isWall) {
            // Dark wall fill — PuzzleGiven matches "given / locked" cell semantics.
            drawRect(color = PuzzleGiven, topLeft = topLeft, size = cellSquare)

            // Conflict overlay on numbered walls that have the wrong count.
            if (isConflict) {
                drawRect(
                    color = GameHazard.copy(alpha = 0.45f),
                    topLeft = topLeft,
                    size = cellSquare
                )
            }

            // Clue number centred in the wall cell.
            if (cell.wallNumber != null) {
                val style = TextStyle(
                    color = if (isConflict) GameHazard else PuzzlePlayerAlt,
                    fontSize = (cellSize * 0.48f / density).sp,
                    fontWeight = FontWeight.Bold
                )
                val layout = measurer.measure(AnnotatedString(cell.wallNumber.toString()), style)
                drawText(
                    layout,
                    topLeft = Offset(
                        center.x - layout.size.width / 2f,
                        center.y - layout.size.height / 2f
                    )
                )
            }
        } else {
            // White cell — light base fill.
            val cellLeft = topLeft.x + inset
            val cellTop = topLeft.y + inset
            val cellSide = cellSize - 2 * inset

            drawRect(
                color = PuzzlePlayerAlt,
                topLeft = Offset(cellLeft, cellTop),
                size = Size(cellSide, cellSide)
            )

            // Warm lit tint when illuminated by a bulb.
            if (isLit) {
                drawRect(
                    color = GameAccent.copy(alpha = 0.28f),
                    topLeft = Offset(cellLeft, cellTop),
                    size = Size(cellSide, cellSide)
                )
            }

            when {
                cell.hasBulb -> {
                    val radius = cellSize * 0.32f
                    // Outer circle — yellow normally, hazard orange when conflicting.
                    drawCircle(
                        color = if (isConflict) GameHazard else GameAccent,
                        radius = radius,
                        center = center
                    )
                    // Inner highlight simulates the glass bulb.
                    drawCircle(
                        color = PuzzlePlayerAlt.copy(alpha = 0.55f),
                        radius = radius * 0.42f,
                        center = Offset(center.x - radius * 0.14f, center.y - radius * 0.14f)
                    )
                    // Conflict ring drawn on top for extra emphasis.
                    if (isConflict) {
                        drawCircle(
                            color = GameHazard,
                            radius = radius,
                            center = center,
                            style = Stroke(width = cellSize * 0.07f)
                        )
                    }
                }

                cell.hasMark -> {
                    // X-mark: two crossed lines as a player annotation.
                    val markR = cellSize * 0.18f
                    val sw = cellSize * 0.07f
                    drawLine(
                        color = PuzzlePlayer,
                        start = Offset(center.x - markR, center.y - markR),
                        end = Offset(center.x + markR, center.y + markR),
                        strokeWidth = sw
                    )
                    drawLine(
                        color = PuzzlePlayer,
                        start = Offset(center.x + markR, center.y - markR),
                        end = Offset(center.x - markR, center.y + markR),
                        strokeWidth = sw
                    )
                }
            }
        }
    }
}
