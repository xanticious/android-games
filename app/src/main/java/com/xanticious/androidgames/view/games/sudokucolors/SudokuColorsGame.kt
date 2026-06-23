package com.xanticious.androidgames.view.games.sudokucolors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.sudokucolors.SudokuColorsController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.model.games.puzzle.starsFor
import com.xanticious.androidgames.model.games.sudokucolors.SudokuColorsState
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.PuzzleBoard
import com.xanticious.androidgames.ui.theme.PuzzleCell
import com.xanticious.androidgames.ui.theme.PuzzleGiven
import com.xanticious.androidgames.ui.theme.PuzzleGridLine
import com.xanticious.androidgames.ui.theme.PuzzleHighlight
import com.xanticious.androidgames.ui.theme.PuzzleHueBlue
import com.xanticious.androidgames.ui.theme.PuzzleHueGreen
import com.xanticious.androidgames.ui.theme.PuzzleHueOrange
import com.xanticious.androidgames.ui.theme.PuzzleHuePink
import com.xanticious.androidgames.ui.theme.PuzzleHueRed
import com.xanticious.androidgames.ui.theme.PuzzleHueTeal
import com.xanticious.androidgames.ui.theme.PuzzleHueViolet
import com.xanticious.androidgames.ui.theme.PuzzleHueYellow
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
 * Nine distinct color tokens used as Sudoku symbols, indexed 1–9.
 * The 9th slot uses GameNeutral (gray) since only 8 PuzzleHue tokens exist.
 */
private val sudokuPalette: List<Color> = listOf(
    PuzzleHueRed,
    PuzzleHueOrange,
    PuzzleHueYellow,
    PuzzleHueGreen,
    PuzzleHueTeal,
    PuzzleHueBlue,
    PuzzleHueViolet,
    PuzzleHuePink,
    GameNeutral
)

/**
 * Sudoku Colors — classic 9×9 Sudoku where symbols are colors.
 *
 * Self-configured: owns Settings, How To Play, and the board via [PuzzleStateMachine].
 * Each row, column, and 3×3 box must contain all nine colors exactly once.
 * Givens are locked; tap a cell then a palette color to fill it; pencil-mode
 * taps toggle candidate marks instead.
 */
@Composable
fun SudokuColorsGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { SudokuColorsController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    var selectedDifficulty by rememberSaveable { mutableStateOf(difficulty) }
    var state by remember { mutableStateOf(controller.newGame(difficulty)) }
    var selectedColor by remember { mutableStateOf<Int?>(null) }

    fun deal() {
        state = controller.newGame(selectedDifficulty)
        selectedColor = null
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Sudoku Colors", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Sudoku Colors",
                onHowToPlay = machine::openHowToPlay,
                onStart = { deal(); machine.startGame() }
            ) {
                DifficultyChips(selected = selectedDifficulty, onSelect = { selectedDifficulty = it })
                FilterChip(
                    selected = state.showDigits,
                    onClick = { state = controller.toggleShowDigits(state) },
                    label = { Text("Show digit overlay") }
                )
            }
        }

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Sudoku Colors", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                HowToPlaySection(title = "Goal") {
                    Text("Fill every cell so each row, column, and 3×3 box contains all nine colors exactly once.")
                }
                HowToPlaySection(title = "Controls") {
                    Text("Tap a cell to select it, then tap a color on the palette to place it.")
                    Text("Tap the same color again to clear the cell.")
                    Text("Toggle Pencil to note candidate colors in a cell without committing.")
                    Text("Long-press a cell while a color is selected to toggle that pencil mark directly.")
                }
                HowToPlaySection(title = "Locked cells") {
                    Text("Cells shown with a slightly inset swatch are givens — they cannot be changed.")
                }
                HowToPlaySection(title = "Conflicts") {
                    Text("A red outline appears when the same color is placed twice in a row, column, or box.")
                }
            }
        }

        else -> {
            val solved = phase == PuzzlePhase.SOLVED
            val stars = starsFor(
                earned = true,
                good = state.moves <= 81,
                great = state.moves <= 50
            )
            val peers = state.selectedPos?.let { controller.peersOf(it) } ?: emptySet()
            val selectedValue =
                state.selectedPos?.let { state.cells[it.row * 9 + it.col].value } ?: 0

            GameScaffold(
                title = "Sudoku Colors",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "Moves: ${state.moves}",
                        center = selectedDifficulty.label,
                        right = "Left: ${state.emptyCellCount}"
                    )
                },
                status = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (solved) {
                            VictoryPanel(
                                score = state.moves,
                                bestScore = state.moves,
                                stars = stars,
                                onReplay = { deal(); machine.retry() },
                                onMenu = machine::newGame,
                                headline = "Solved!",
                                primaryLabel = "New Puzzle"
                            )
                        } else {
                            ColorPalette(
                                selectedColor = selectedColor,
                                pencilMode = state.pencilMode,
                                onColorTap = { colorIndex ->
                                    selectedColor = colorIndex
                                    val pos = state.selectedPos ?: return@ColorPalette
                                    state = if (state.pencilMode) {
                                        controller.togglePencilMark(state, pos, colorIndex)
                                    } else {
                                        val updated = controller.setValue(state, pos, colorIndex)
                                        if (controller.isSolved(updated)) machine.solved()
                                        updated
                                    }
                                }
                            )
                            PuzzleActionBar(
                                status = "${state.emptyCellCount} cells left",
                                onUndo = {
                                    state = controller.undo(state)
                                },
                                undoEnabled = state.canUndo,
                                onNew = { deal() },
                                extras = {
                                    FilterChip(
                                        selected = state.pencilMode,
                                        onClick = { state = controller.togglePencilMode(state) },
                                        label = { Text("✏ Pencil") }
                                    )
                                }
                            )
                        }
                    }
                }
            ) {
                SudokuBoard(
                    state = state,
                    peers = peers,
                    selectedValue = selectedValue,
                    enabled = !solved,
                    onCellTap = { pos ->
                        state = controller.selectCell(state, pos)
                        // If a color is already chosen, immediately place it
                        val sc = selectedColor ?: return@SudokuBoard
                        val updated = if (state.pencilMode) {
                            controller.togglePencilMark(state, pos, sc)
                        } else {
                            val result = controller.setValue(state, pos, sc)
                            if (controller.isSolved(result)) machine.solved()
                            result
                        }
                        state = updated
                    },
                    onCellLongPress = { pos ->
                        // Long-press toggles pencil mark for the current selected color
                        val sc = selectedColor ?: run {
                            state = controller.selectCell(state, pos)
                            return@SudokuBoard
                        }
                        state = controller.selectCell(state, pos)
                        state = controller.togglePencilMark(state, pos, sc)
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorPalette(
    selectedColor: Int?,
    pencilMode: Boolean,
    onColorTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..9) {
            val color = sudokuPalette[i - 1]
            val isSelected = selectedColor == i
            Box(
                modifier = Modifier
                    .size(if (isSelected) 40.dp else 34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
                    .then(
                        if (isSelected) Modifier.border(
                            width = 3.dp,
                            color = if (pencilMode) PuzzleGiven else PuzzlePlayerAlt,
                            shape = RoundedCornerShape(6.dp)
                        ) else Modifier
                    )
                    .clickable { onColorTap(i) },
                contentAlignment = Alignment.Center
            ) {}
        }
    }
}

@Composable
private fun SudokuBoard(
    state: SudokuColorsState,
    peers: Set<GridPos>,
    selectedValue: Int,
    enabled: Boolean,
    onCellTap: (GridPos) -> Unit,
    onCellLongPress: (GridPos) -> Unit
) {
    val measurer = rememberTextMeasurer()
    PuzzleBoard(
        rows = 9,
        cols = 9,
        drawGridLines = false,
        onCellTap = if (enabled) onCellTap else null,
        onCellLongPress = if (enabled) onCellLongPress else null
    ) {
        val r = current.row
        val c = current.col
        val idx = r * 9 + c
        val cell = state.cells[idx]
        val isSelected = state.selectedPos == current
        val isPeer = current in peers
        val isSameValue = selectedValue != 0 && cell.value == selectedValue

        // ── Cell background ──────────────────────────────────────────────
        val bgColor = when {
            isSelected -> PuzzleHighlight
            isPeer || isSameValue -> PuzzleHighlight.copy(alpha = 0.35f)
            cell.isGiven -> PuzzleGiven.copy(alpha = 0.12f)
            else -> PuzzleCell
        }
        drawRect(bgColor, topLeft = topLeft, size = Size(cellSize, cellSize))

        // ── Cell content ─────────────────────────────────────────────────
        if (cell.value != 0) {
            val swatchInset = if (cell.isGiven) cellSize * 0.12f else cellSize * 0.05f
            val sw = rect(swatchInset)
            val swColor = sudokuPalette[cell.value - 1]
            drawRoundRect(
                color = swColor,
                topLeft = Offset(sw.left, sw.top),
                size = Size(sw.width, sw.height),
                cornerRadius = CornerRadius(cellSize * 0.12f)
            )
            if (state.highlightConflicts && cell.isConflict) {
                drawRoundRect(
                    color = GameHazard,
                    topLeft = Offset(sw.left, sw.top),
                    size = Size(sw.width, sw.height),
                    cornerRadius = CornerRadius(cellSize * 0.12f),
                    style = Stroke(width = cellSize * 0.08f)
                )
            }
            if (state.showDigits) {
                val label = cell.value.toString()
                val fontSize = (cellSize * 0.32f / density).sp
                val style = TextStyle(
                    color = PuzzleBoard,
                    fontSize = fontSize,
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
        } else if (cell.pencilMarks.isNotEmpty()) {
            // Draw pencil marks as tiny color dots in a 3×3 grid within the cell
            val dotSize = cellSize * 0.22f
            val padX = (cellSize - dotSize * 3) / 4f
            val padY = (cellSize - dotSize * 3) / 4f
            for (markValue in cell.pencilMarks) {
                val markIdx = markValue - 1
                val markRow = markIdx / 3
                val markCol = markIdx % 3
                val dotX = topLeft.x + padX + markCol * (dotSize + padX)
                val dotY = topLeft.y + padY + markRow * (dotSize + padY)
                drawRoundRect(
                    color = sudokuPalette[markIdx].copy(alpha = 0.85f),
                    topLeft = Offset(dotX, dotY),
                    size = Size(dotSize, dotSize),
                    cornerRadius = CornerRadius(dotSize * 0.3f)
                )
            }
        }

        // ── Grid lines (thin for cells, thick at box boundaries) ─────────
        val thinStroke = (cellSize * 0.02f).coerceIn(1f, 2f)
        val thickStroke = (cellSize * 0.06f).coerceIn(3f, 5f)

        // Right border
        val rightIsBox = (c + 1) % 3 == 0
        drawLine(
            color = PuzzleGridLine,
            start = Offset(topLeft.x + cellSize, topLeft.y),
            end = Offset(topLeft.x + cellSize, topLeft.y + cellSize),
            strokeWidth = if (rightIsBox) thickStroke else thinStroke
        )
        // Bottom border
        val bottomIsBox = (r + 1) % 3 == 0
        drawLine(
            color = PuzzleGridLine,
            start = Offset(topLeft.x, topLeft.y + cellSize),
            end = Offset(topLeft.x + cellSize, topLeft.y + cellSize),
            strokeWidth = if (bottomIsBox) thickStroke else thinStroke
        )
        // Left border for first column (outer border)
        if (c == 0) {
            drawLine(
                color = PuzzleGridLine,
                start = Offset(topLeft.x, topLeft.y),
                end = Offset(topLeft.x, topLeft.y + cellSize),
                strokeWidth = thickStroke
            )
        }
        // Top border for first row (outer border)
        if (r == 0) {
            drawLine(
                color = PuzzleGridLine,
                start = Offset(topLeft.x, topLeft.y),
                end = Offset(topLeft.x + cellSize, topLeft.y),
                strokeWidth = thickStroke
            )
        }
    }
}
