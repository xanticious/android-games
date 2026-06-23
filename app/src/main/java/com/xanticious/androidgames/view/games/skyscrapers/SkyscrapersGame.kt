package com.xanticious.androidgames.view.games.skyscrapers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.xanticious.androidgames.controller.games.skyscrapers.ClueState
import com.xanticious.androidgames.controller.games.skyscrapers.ClueStatuses
import com.xanticious.androidgames.controller.games.skyscrapers.SkyscrapersController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.model.games.skyscrapers.SkyscrapersState
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.ui.theme.PuzzleBoard
import com.xanticious.androidgames.ui.theme.PuzzleCell
import com.xanticious.androidgames.ui.theme.PuzzleGiven
import com.xanticious.androidgames.ui.theme.PuzzleGridLine
import com.xanticious.androidgames.ui.theme.PuzzleHighlight
import com.xanticious.androidgames.ui.theme.PuzzlePlayer
import com.xanticious.androidgames.ui.theme.PuzzlePlayerAlt
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.puzzle.HowToPlaySection
import com.xanticious.androidgames.view.common.puzzle.OptionChips
import com.xanticious.androidgames.view.common.puzzle.PuzzleActionBar
import com.xanticious.androidgames.view.common.puzzle.PuzzleHowToPlayScreen
import com.xanticious.androidgames.view.common.puzzle.PuzzleSettingsScreen
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Skyscrapers — fill an N×N Latin square so edge clues (visible buildings) all
 * match (`design/puzzle-games/skyscrapers`). Self-configured: owns Settings, How
 * to Play, and the game board via the shared [PuzzleStateMachine].
 */
@Composable
fun SkyscrapersGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { SkyscrapersController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    val defaultSize = when (difficulty) {
        GameDifficulty.EASY -> 4
        GameDifficulty.MEDIUM -> 5
        GameDifficulty.HARD -> 6
    }
    var selectedSize by rememberSaveable { mutableStateOf(defaultSize) }
    var state by remember { mutableStateOf(controller.newGame(defaultSize, difficulty)) }
    var pencilMode by remember { mutableStateOf(false) }

    fun deal() {
        state = controller.newGame(selectedSize, difficulty)
        pencilMode = false
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Skyscrapers", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Skyscrapers",
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

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Skyscrapers", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                HowToPlaySection(title = "Goal") {
                    Text("Fill the N×N grid so every row and column contains each height 1..N exactly once.")
                }
                HowToPlaySection(title = "Edge clues") {
                    Text("The numbers around the border count how many buildings are visible looking inward from that edge. Taller buildings hide shorter ones behind them.")
                }
                HowToPlaySection(title = "Controls") {
                    Text("Tap a cell to select it, then tap a number to enter a height.")
                    Text("Enable Pencil mode to jot candidate heights as small marks instead of filling the cell.")
                    Text("Long-press a cell to enable Pencil mode and select that cell in one step.")
                    Text("Tap ✕ to clear a selected cell. Use Undo to revert the last entry.")
                }
                HowToPlaySection(title = "Solved") {
                    Text("The puzzle is solved when all rows and columns are complete and every edge clue is satisfied. Satisfied clues turn green; violated clues turn orange.")
                }
            }
        }

        else -> {
            val solved = phase == PuzzlePhase.SOLVED
            val conflicts = remember(state.grid) { controller.conflictIndices(state) }
            val clueStatuses = remember(state.grid) { controller.clueStatuses(state) }

            GameScaffold(
                title = "Skyscrapers",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "Remaining: ${controller.remainingCells(state)}",
                        center = "${state.size}×${state.size}",
                        right = ""
                    )
                },
                status = {
                    if (solved) {
                        VictoryPanel(
                            score = 0,
                            bestScore = 0,
                            stars = 3,
                            onReplay = { deal(); machine.retry() },
                            onMenu = machine::newGame,
                            headline = "Solved!",
                            primaryLabel = "New Puzzle"
                        )
                    } else {
                        Column {
                            NumberPad(
                                n = state.size,
                                enabled = state.selectedIndex >= 0,
                                onNumber = { h ->
                                    state.selectedPos?.let { pos ->
                                        state = if (pencilMode && h > 0) {
                                            controller.togglePencil(state, pos, h)
                                        } else {
                                            controller.setHeight(state, pos, h)
                                        }
                                        if (!pencilMode && controller.isSolved(state)) machine.solved()
                                    }
                                }
                            )
                            PuzzleActionBar(
                                status = "Left: ${controller.remainingCells(state)}",
                                onUndo = { state = controller.undo(state) },
                                undoEnabled = state.canUndo,
                                onNew = { deal() },
                                extras = {
                                    FilterChip(
                                        selected = pencilMode,
                                        onClick = { pencilMode = !pencilMode },
                                        label = { Text("✏") }
                                    )
                                }
                            )
                        }
                    }
                }
            ) {
                SkyscrapersBoard(
                    state = state,
                    conflicts = conflicts,
                    clueStatuses = clueStatuses,
                    enabled = !solved,
                    onCellTap = { pos ->
                        state = controller.selectCell(state, pos)
                    },
                    onCellLongPress = { pos ->
                        state = controller.selectCell(state, pos)
                        pencilMode = true
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Board composable
// ---------------------------------------------------------------------------

/**
 * Single-canvas board that draws both the edge-clue gutters and the inner grid.
 *
 * Geometry: the total drawable area is divided into (N+2) × (N+2) cells so the
 * gutter on each side is exactly one cell wide. Cell size is capped at 72 dp so
 * large grids still look good on tablets.
 */
@Composable
private fun SkyscrapersBoard(
    state: SkyscrapersState,
    conflicts: Set<Int>,
    clueStatuses: ClueStatuses,
    enabled: Boolean,
    onCellTap: (GridPos) -> Unit,
    onCellLongPress: (GridPos) -> Unit
) {
    val n = state.size
    val textMeasurer = rememberTextMeasurer()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(n, enabled) {
                    if (!enabled) return@pointerInput
                    val maxPx = 72.dp.toPx()
                    detectTapGestures(
                        onTap = { offset ->
                            gridPosAt(offset, n, size.width.toFloat(), size.height.toFloat(), maxPx)
                                ?.let(onCellTap)
                        },
                        onLongPress = { offset ->
                            gridPosAt(offset, n, size.width.toFloat(), size.height.toFloat(), maxPx)
                                ?.let(onCellLongPress)
                        }
                    )
                }
        ) {
            val maxCellPx = 72.dp.toPx()
            val cell = minOf(size.width / (n + 2), size.height / (n + 2), maxCellPx)
            val totalW = cell * (n + 2)
            val totalH = cell * (n + 2)
            val ox = (size.width - totalW) / 2f   // outer left edge
            val oy = (size.height - totalH) / 2f  // outer top edge
            val bx = ox + cell                     // board left edge
            val by = oy + cell                     // board top edge

            // Board background
            drawRect(
                color = PuzzleBoard,
                topLeft = Offset(bx, by),
                size = Size(cell * n, cell * n)
            )

            // Cell backgrounds: selected → highlight; given → given colour; conflict → hazard tint
            for (r in 0 until n) {
                for (c in 0 until n) {
                    val idx = r * n + c
                    val cellBg: Color? = when {
                        state.selectedIndex == idx -> PuzzleHighlight
                        idx in state.givens -> PuzzleGiven
                        idx in conflicts -> GameHazard.copy(alpha = 0.25f)
                        else -> null
                    }
                    if (cellBg != null) {
                        drawRect(
                            color = cellBg,
                            topLeft = Offset(bx + c * cell, by + r * cell),
                            size = Size(cell, cell)
                        )
                    }
                }
            }

            // Grid lines
            val stroke = (cell * 0.02f).coerceIn(1f, 3f)
            for (r in 0..n) {
                val y = by + r * cell
                drawLine(PuzzleGridLine, Offset(bx, y), Offset(bx + n * cell, y), strokeWidth = stroke)
            }
            for (c in 0..n) {
                val x = bx + c * cell
                drawLine(PuzzleGridLine, Offset(x, by), Offset(x, by + n * cell), strokeWidth = stroke)
            }

            // Cell values and pencil marks
            for (r in 0 until n) {
                for (c in 0 until n) {
                    val idx = r * n + c
                    val value = state.grid[idx]
                    val cx = bx + c * cell + cell / 2f
                    val cy = by + r * cell + cell / 2f

                    if (value != 0) {
                        val textColor = when {
                            idx in conflicts -> GameHazard
                            idx in state.givens -> if (state.selectedIndex == idx) PuzzleCell else PuzzlePlayerAlt
                            else -> PuzzlePlayer
                        }
                        val fs = (cell * 0.44f / density).sp
                        val layout = textMeasurer.measure(
                            AnnotatedString(value.toString()),
                            TextStyle(color = textColor, fontSize = fs, fontWeight = FontWeight.Bold)
                        )
                        drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, cy - layout.size.height / 2f))
                    } else {
                        val marks = state.pencilMarks[idx] ?: emptySet()
                        if (marks.isNotEmpty()) {
                            val markCols = ceil(sqrt(n.toDouble())).toInt().coerceAtLeast(1)
                            val markRows = ceil(n.toDouble() / markCols).toInt().coerceAtLeast(1)
                            val mfs = (cell * 0.18f / density).sp
                            val markStyle = TextStyle(color = PuzzlePlayerAlt, fontSize = mfs)
                            for (mark in marks.sorted()) {
                                val mi = mark - 1
                                val mc = mi % markCols
                                val mr = mi / markCols
                                val mx = bx + c * cell + (mc + 0.5f) * cell / markCols
                                val my = by + r * cell + (mr + 0.5f) * cell / markRows
                                val ml = textMeasurer.measure(AnnotatedString(mark.toString()), markStyle)
                                drawText(ml, topLeft = Offset(mx - ml.size.width / 2f, my - ml.size.height / 2f))
                            }
                        }
                    }
                }
            }

            // Edge clue numbers in the gutter
            fun drawClue(text: String, centerX: Float, centerY: Float, clueState: ClueState) {
                val clueColor = when (clueState) {
                    ClueState.SATISFIED -> GameSuccess
                    ClueState.VIOLATED -> GameHazard
                    ClueState.PENDING -> PuzzlePlayer
                }
                val fs = (cell * 0.38f / density).sp
                val layout = textMeasurer.measure(
                    AnnotatedString(text),
                    TextStyle(color = clueColor, fontSize = fs, fontWeight = FontWeight.Bold)
                )
                drawText(layout, topLeft = Offset(centerX - layout.size.width / 2f, centerY - layout.size.height / 2f))
            }

            for (c in 0 until n) {
                val cx = bx + c * cell + cell / 2f
                drawClue(state.clues.top[c].toString(), cx, oy + cell / 2f, clueStatuses.top[c])
                drawClue(state.clues.bottom[c].toString(), cx, oy + (n + 1.5f) * cell, clueStatuses.bottom[c])
            }
            for (r in 0 until n) {
                val cy = by + r * cell + cell / 2f
                drawClue(state.clues.left[r].toString(), ox + cell / 2f, cy, clueStatuses.left[r])
                drawClue(state.clues.right[r].toString(), ox + (n + 1.5f) * cell, cy, clueStatuses.right[r])
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Number pad
// ---------------------------------------------------------------------------

@Composable
private fun NumberPad(n: Int, enabled: Boolean, onNumber: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        for (h in 1..n) {
            OutlinedButton(
                onClick = { onNumber(h) },
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text("$h", style = MaterialTheme.typography.labelMedium)
            }
        }
        OutlinedButton(
            onClick = { onNumber(0) },
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 2.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text("✕", style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ---------------------------------------------------------------------------
// Gesture helper
// ---------------------------------------------------------------------------

/** Maps a pointer offset to the grid cell under it, ignoring the gutter zone. */
private fun gridPosAt(
    offset: Offset,
    n: Int,
    width: Float,
    height: Float,
    maxCell: Float
): GridPos? {
    val cell = minOf(width / (n + 2), height / (n + 2), maxCell)
    val ox = (width - cell * (n + 2)) / 2f
    val oy = (height - cell * (n + 2)) / 2f
    val bx = ox + cell
    val by = oy + cell
    val col = ((offset.x - bx) / cell).toInt()
    val row = ((offset.y - by) / cell).toInt()
    return if (row in 0 until n && col in 0 until n) GridPos(row, col) else null
}
