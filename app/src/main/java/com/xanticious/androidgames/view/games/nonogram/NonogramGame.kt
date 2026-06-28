package com.xanticious.androidgames.view.games.nonogram

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.nonogram.NonogramController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.nonogram.CellState
import com.xanticious.androidgames.model.games.nonogram.NonogramState
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import com.xanticious.androidgames.ui.theme.PuzzleBoard
import com.xanticious.androidgames.ui.theme.PuzzleCell
import com.xanticious.androidgames.ui.theme.PuzzleGiven
import com.xanticious.androidgames.ui.theme.PuzzleGridLine
import com.xanticious.androidgames.ui.theme.PuzzlePlayer
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.puzzle.OptionChips
import com.xanticious.androidgames.view.common.puzzle.PuzzleActionBar
import com.xanticious.androidgames.view.common.puzzle.PuzzleHowToPlayScreen
import com.xanticious.androidgames.view.common.puzzle.PuzzleSettingsScreen

/**
 * Nonogram (Picross) — fill cells to match row and column run-length clues.
 * Self-configured: owns Settings, How to Play and game screens via the shared
 * [PuzzleStateMachine]. Clue gutters are rendered via Canvas alongside the board
 * so clue text aligns precisely with grid cells.
 */
@Composable
fun NonogramGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { NonogramController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    val defaultSize = when (difficulty) {
        GameDifficulty.EASY -> 5
        GameDifficulty.MEDIUM -> 10
        GameDifficulty.HARD -> 15
    }
    var selectedSize by remember { mutableIntStateOf(defaultSize) }
    var state by remember { mutableStateOf(controller.newGame(defaultSize, difficulty)) }

    fun deal() {
        state = controller.newGame(selectedSize, difficulty)
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Nonogram", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Nonogram",
                onHowToPlay = machine::openHowToPlay,
                onStart = { deal(); machine.startGame() }
            ) {
                OptionChips(
                    label = "Board size",
                    options = controller.sizes,
                    selected = selectedSize,
                    onSelect = { selectedSize = it },
                    labelOf = { "${it}×${it}" }
                )
            }
        }

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Nonogram", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                Text("Use the number clues on each row and column to decide which cells are filled.")
                Text("A clue like \"3 1\" means: a run of exactly 3 filled cells, a gap of at least one empty, then a run of exactly 1.")
                Text("Tap a cell to fill or unfill it.")
                Text("Long-press a cell to mark it with ✕ as a reminder that it must stay empty.")
                Text("Drag across cells to fill multiple cells at once.")
                Text("Complete every row and column clue to solve the puzzle!")
            }
        }

        else -> {
            val solved = phase == PuzzlePhase.SOLVED
            val n = state.size

            val satisfiedRows = (0 until n).count { r -> controller.isRowSatisfied(state, r) }
            val satisfiedCols = (0 until n).count { c -> controller.isColSatisfied(state, c) }
            val totalLines = n * 2

            val visitedInDrag = remember { mutableSetOf<GridPos>() }

            GameScaffold(
                title = "Nonogram",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "Lines: ${satisfiedRows + satisfiedCols}/$totalLines",
                        center = "${n}×${n}",
                        right = "${state.cells.count { it == CellState.FILLED }} filled"
                    )
                },
                status = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (solved) {
                            VictoryPanel(
                                score = satisfiedRows + satisfiedCols,
                                bestScore = totalLines,
                                stars = 3,
                                onReplay = { deal(); machine.retry() },
                                onMenu = machine::newGame,
                                headline = "Puzzle Solved!",
                                primaryLabel = "New Puzzle"
                            )
                        } else {
                            PuzzleActionBar(
                                status = "${satisfiedRows + satisfiedCols}/$totalLines lines",
                                onUndo = { state = controller.undo(state) },
                                undoEnabled = state.canUndo,
                                onReset = {
                                    state = state.copy(
                                        cells = List(n * n) { CellState.EMPTY },
                                        history = emptyList()
                                    )
                                },
                                onNew = { deal() }
                            )
                        }
                    }
                }
            ) {
                NonogramBoard(
                    state = state,
                    controller = controller,
                    enabled = !solved,
                    onCellTap = { pos ->
                        state = controller.toggle(state, pos)
                        if (controller.isSolved(state)) machine.solved()
                    },
                    onCellLongPress = { pos ->
                        state = controller.toggleMark(state, pos)
                    },
                    onCellDrag = { pos ->
                        if (visitedInDrag.add(pos)) {
                            val idx = pos.row * n + pos.col
                            if (state.cells.getOrNull(idx) == CellState.EMPTY) {
                                state = controller.toggle(state, pos)
                                if (controller.isSolved(state)) machine.solved()
                            }
                        }
                    },
                    onDragEnd = { visitedInDrag.clear() }
                )
            }
        }
    }
}

/**
 * Renders the nonogram grid together with its row (left) and column (top) clue
 * gutters in a single Canvas so clue text aligns precisely with board cells.
 *
 * Cell size is computed to fit the combined board + gutters within the available
 * space while respecting [maxCellDp]. Clue numbers dim when their line is already
 * satisfied to give the player progress feedback.
 */
@Composable
private fun NonogramBoard(
    state: NonogramState,
    controller: NonogramController,
    enabled: Boolean,
    onCellTap: (GridPos) -> Unit,
    onCellLongPress: (GridPos) -> Unit,
    onCellDrag: (GridPos) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val n = state.size
    val textMeasurer = rememberTextMeasurer()
    val maxRowLen = state.rowClues.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1
    val maxColLen = state.colClues.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1
    val maxCellDp = 56.dp

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(n, maxRowLen, maxColLen, enabled, onCellTap, onCellLongPress) {
                if (!enabled) return@pointerInput
                val maxCellPx = maxCellDp.toPx()
                detectTapGestures(
                    onTap = { offset ->
                        val cell = cellSize(size.width.toFloat(), size.height.toFloat(), n, maxRowLen, maxColLen, maxCellPx)
                        hitTest(offset, size.width.toFloat(), size.height.toFloat(), n, maxRowLen, maxColLen, cell)
                            ?.let(onCellTap)
                    },
                    onLongPress = { offset ->
                        val cell = cellSize(size.width.toFloat(), size.height.toFloat(), n, maxRowLen, maxColLen, maxCellPx)
                        hitTest(offset, size.width.toFloat(), size.height.toFloat(), n, maxRowLen, maxColLen, cell)
                            ?.let(onCellLongPress)
                    }
                )
            }
            .pointerInput(n, maxRowLen, maxColLen, enabled, onCellDrag, onDragEnd) {
                if (!enabled) return@pointerInput
                val maxCellPx = maxCellDp.toPx()
                detectDragGestures(
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                ) { change, _ ->
                    val cell = cellSize(size.width.toFloat(), size.height.toFloat(), n, maxRowLen, maxColLen, maxCellPx)
                    hitTest(change.position, size.width.toFloat(), size.height.toFloat(), n, maxRowLen, maxColLen, cell)
                        ?.let(onCellDrag)
                }
            }
    ) {
        val maxCellPx = maxCellDp.toPx()
        val cell = cellSize(size.width, size.height, n, maxRowLen, maxColLen, maxCellPx)

        val gutterW = maxRowLen * cell
        val gutterH = maxColLen * cell
        val boardW = n * cell
        val boardH = n * cell
        val totalW = gutterW + boardW
        val totalH = gutterH + boardH
        val ox = (size.width - totalW) / 2f   // left edge of gutter+board block
        val oy = (size.height - totalH) / 2f  // top edge of gutter+board block
        val bx = ox + gutterW                  // left edge of board cells
        val by = oy + gutterH                  // top edge of board cells

        // Board background
        drawRect(color = PuzzleBoard, topLeft = Offset(bx, by), size = Size(boardW, boardH))

        // Grid lines
        val lineW = (cell * 0.04f).coerceIn(1f, 3f)
        for (r in 0..n) {
            val y = by + r * cell
            drawLine(PuzzleGridLine, Offset(bx, y), Offset(bx + boardW, y), strokeWidth = lineW)
        }
        for (c in 0..n) {
            val x = bx + c * cell
            drawLine(PuzzleGridLine, Offset(x, by), Offset(x, by + boardH), strokeWidth = lineW)
        }

        // Cell contents
        val inset = cell * 0.08f
        val markInset = cell * 0.25f
        val markStroke = lineW * 2.5f
        for (r in 0 until n) {
            for (c in 0 until n) {
                val idx = r * n + c
                val cx = bx + c * cell
                val cy = by + r * cell
                when (state.cells[idx]) {
                    CellState.FILLED -> drawRect(
                        color = PuzzlePlayer,
                        topLeft = Offset(cx + inset, cy + inset),
                        size = Size(cell - 2 * inset, cell - 2 * inset)
                    )
                    CellState.MARKED -> {
                        drawLine(
                            PuzzleGiven,
                            Offset(cx + markInset, cy + markInset),
                            Offset(cx + cell - markInset, cy + cell - markInset),
                            strokeWidth = markStroke
                        )
                        drawLine(
                            PuzzleGiven,
                            Offset(cx + cell - markInset, cy + markInset),
                            Offset(cx + markInset, cy + cell - markInset),
                            strokeWidth = markStroke
                        )
                    }
                    CellState.EMPTY -> Unit
                }
            }
        }

        // Row clues — right-aligned in the left gutter
        val fontSize = (cell * 0.38f / density).sp
        for (r in 0 until n) {
            val clue = state.rowClues[r]
            val display = if (clue.isEmpty()) listOf(0) else clue
            val satisfied = controller.isRowSatisfied(state, r)
            val clueColor = if (satisfied) PuzzleGridLine else PuzzleGiven
            val startX = ox + (maxRowLen - display.size) * cell
            val clueY = by + r * cell + cell / 2f
            display.forEachIndexed { i, v ->
                val style = TextStyle(color = clueColor, fontSize = fontSize, fontWeight = FontWeight.Bold)
                val layout = textMeasurer.measure(AnnotatedString(v.toString()), style)
                val clueX = startX + i * cell + cell / 2f
                drawText(
                    layout,
                    topLeft = Offset(clueX - layout.size.width / 2f, clueY - layout.size.height / 2f)
                )
            }
        }

        // Column clues — bottom-aligned in the top gutter
        for (c in 0 until n) {
            val clue = state.colClues[c]
            val display = if (clue.isEmpty()) listOf(0) else clue
            val satisfied = controller.isColSatisfied(state, c)
            val clueColor = if (satisfied) PuzzleGridLine else PuzzleCell
            val startY = oy + (maxColLen - display.size) * cell
            val clueX = bx + c * cell + cell / 2f
            display.forEachIndexed { i, v ->
                val style = TextStyle(color = clueColor, fontSize = fontSize, fontWeight = FontWeight.Bold)
                val layout = textMeasurer.measure(AnnotatedString(v.toString()), style)
                val clueY = startY + i * cell + cell / 2f
                drawText(
                    layout,
                    topLeft = Offset(clueX - layout.size.width / 2f, clueY - layout.size.height / 2f)
                )
            }
        }
    }
}

/** Computes a uniform cell size that fits board + gutters within the available space. */
private fun cellSize(
    width: Float,
    height: Float,
    n: Int,
    maxRowLen: Int,
    maxColLen: Int,
    maxCellPx: Float
): Float = minOf(
    width / (n + maxRowLen),
    height / (n + maxColLen),
    maxCellPx
)

/**
 * Maps a pointer [offset] to the board cell under it, or null if outside the board.
 * Uses the same layout formula as the draw block for exact correspondence.
 */
private fun hitTest(
    offset: Offset,
    width: Float,
    height: Float,
    n: Int,
    maxRowLen: Int,
    maxColLen: Int,
    cell: Float
): GridPos? {
    val gutterW = maxRowLen * cell
    val gutterH = maxColLen * cell
    val boardW = n * cell
    val boardH = n * cell
    val ox = (width - gutterW - boardW) / 2f
    val oy = (height - gutterH - boardH) / 2f
    val bx = ox + gutterW
    val by = oy + gutterH
    val col = ((offset.x - bx) / cell).toInt()
    val row = ((offset.y - by) / cell).toInt()
    return if (row in 0 until n && col in 0 until n) GridPos(row, col) else null
}
