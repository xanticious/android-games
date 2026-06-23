package com.xanticious.androidgames.view.games.numberlink

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.numberlink.NumberlinkController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.numberlink.NumberlinkState
import com.xanticious.androidgames.model.games.puzzle.Direction
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.model.games.puzzle.step
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import com.xanticious.androidgames.ui.theme.PuzzleHueBlue
import com.xanticious.androidgames.ui.theme.PuzzleHueGreen
import com.xanticious.androidgames.ui.theme.PuzzleHueOrange
import com.xanticious.androidgames.ui.theme.PuzzleHuePink
import com.xanticious.androidgames.ui.theme.PuzzleHueRed
import com.xanticious.androidgames.ui.theme.PuzzleHueTeal
import com.xanticious.androidgames.ui.theme.PuzzleHueViolet
import com.xanticious.androidgames.ui.theme.PuzzleHueYellow
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.puzzle.OptionChips
import com.xanticious.androidgames.view.common.puzzle.PuzzleActionBar
import com.xanticious.androidgames.view.common.puzzle.PuzzleBoard
import com.xanticious.androidgames.view.common.puzzle.PuzzleHowToPlayScreen
import com.xanticious.androidgames.view.common.puzzle.PuzzleSettingsScreen
import kotlin.random.Random

private val pairHues = listOf(
    PuzzleHueRed, PuzzleHueOrange, PuzzleHueYellow, PuzzleHueGreen,
    PuzzleHueTeal, PuzzleHueBlue, PuzzleHueViolet, PuzzleHuePink
)

private fun hueFor(pairIndex: Int): Color = pairHues[pairIndex % pairHues.size]

/**
 * Numberlink — connect matching numbered pairs with non-crossing paths that fill
 * every cell (`design/puzzle-games/numberlink/numberlink-design.md`).
 *
 * Self-configured: owns its Settings, How to Play and board screens via the shared
 * [PuzzleStateMachine].
 */
@Composable
fun NumberlinkGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { NumberlinkController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    val defaultSize = when (difficulty) {
        GameDifficulty.EASY -> 5
        GameDifficulty.MEDIUM -> 7
        GameDifficulty.HARD -> 8
    }
    var selectedSize by rememberSaveable { mutableStateOf(defaultSize) }
    var requireCoverage by rememberSaveable { mutableStateOf(true) }

    var state by remember {
        mutableStateOf(
            controller.newGame(defaultSize, requireFullCoverage = true, random = Random(System.currentTimeMillis()))
        )
    }

    fun deal() {
        state = controller.newGame(
            size = selectedSize,
            requireFullCoverage = requireCoverage,
            random = Random(System.currentTimeMillis())
        )
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Numberlink", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Numberlink",
                onHowToPlay = machine::openHowToPlay,
                onStart = { deal(); machine.startGame() }
            ) {
                OptionChips(
                    label = "Grid size",
                    options = controller.sizes,
                    selected = selectedSize,
                    onSelect = { selectedSize = it },
                    labelOf = { "${it}×$it" }
                )
                OptionChips(
                    label = "Full coverage",
                    options = listOf(true, false),
                    selected = requireCoverage,
                    onSelect = { requireCoverage = it },
                    labelOf = { if (it) "Required" else "Optional" }
                )
            }
        }

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Numberlink", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                Text("Connect each pair of matching numbers with a path.")
                Text("Drag from a numbered endpoint and route the path to its twin.")
                Text("Paths cannot cross or share cells.")
                Text("In full-coverage mode, every cell must be used to win.")
                Text("Tap an endpoint to clear its path. Undo removes the last drawn path.")
            }
        }

        else -> {
            val solved = phase == PuzzlePhase.SOLVED
            var dragActive by remember { mutableStateOf(false) }

            GameScaffold(
                title = "Numberlink",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "Pairs ${state.connectedPairCount}/${state.numPairs}",
                        center = "${state.size}×${state.size}",
                        right = if (state.requireFullCoverage)
                            "Cells ${state.filledCellCount}/${state.size * state.size}"
                        else
                            "${state.connectedPairCount}/${state.numPairs}"
                    )
                },
                status = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (solved) {
                            VictoryPanel(
                                score = state.connectedPairCount,
                                bestScore = state.numPairs,
                                stars = 3,
                                onReplay = { deal(); machine.retry() },
                                onMenu = machine::newGame,
                                headline = "Solved!",
                                primaryLabel = "New Puzzle"
                            )
                        } else {
                            PuzzleActionBar(
                                status = if (state.requireFullCoverage)
                                    "Cells ${state.filledCellCount}/${state.size * state.size}"
                                else
                                    "Pairs ${state.connectedPairCount}/${state.numPairs}",
                                onUndo = { state = controller.undo(state) },
                                undoEnabled = state.canUndo,
                                onReset = { state = controller.resetAllPaths(state) },
                                onNew = { deal() }
                            )
                        }
                    }
                }
            ) {
                NumberlinkBoard(
                    state = state,
                    enabled = !solved,
                    onDragStart = { pos ->
                        if (!dragActive) {
                            dragActive = true
                            state = controller.startPath(state, pos)
                        }
                    },
                    onDragCell = { pos ->
                        state = controller.extendPath(state, pos)
                    },
                    onDragEnd = {
                        if (dragActive) {
                            state = controller.recordHistory(state)
                            state = state.copy(activePairIndex = null)
                            dragActive = false
                            if (controller.isSolved(state)) machine.solved()
                        }
                    },
                    onEndpointTap = { pairIndex ->
                        state = controller.clearPath(state, pairIndex)
                    }
                )
            }
        }
    }
}

@Composable
private fun NumberlinkBoard(
    state: NumberlinkState,
    enabled: Boolean,
    onDragStart: (GridPos) -> Unit,
    onDragCell: (GridPos) -> Unit,
    onDragEnd: () -> Unit,
    onEndpointTap: (Int) -> Unit
) {
    val measurer = rememberTextMeasurer()

    // Precompute per-cell drawing info (updated whenever paths change)
    val cellInfo = remember(state.paths) {
        buildCellInfo(state)
    }
    val endpointMap = remember(state.endpoints) {
        state.endpoints.associate { it.pos to it.pairIndex }
    }

    var firstDragCell by remember { mutableStateOf<GridPos?>(null) }

    PuzzleBoard(
        rows = state.size,
        cols = state.size,
        drawGridLines = true,
        onCellTap = if (enabled) { pos ->
            val pairIdx = endpointMap[pos]
            if (pairIdx != null) onEndpointTap(pairIdx)
        } else null,
        onCellDrag = if (enabled) { pos ->
            if (firstDragCell == null) {
                firstDragCell = pos
                onDragStart(pos)
            } else {
                onDragCell(pos)
            }
        } else null,
        onDragEnd = if (enabled) {
            {
                firstDragCell = null
                onDragEnd()
            }
        } else null
    ) {
        val (pairIndex, connections) = cellInfo[current] ?: return@PuzzleBoard
        val color = hueFor(pairIndex)
        val tubeRadius = cellSize * 0.23f
        val isEndpoint = current in endpointMap
        val isActive = pairIndex == state.activePairIndex

        // Highlight active path with slight brightness overlay
        val drawColor = if (isActive) color else color.copy(alpha = 0.85f)

        // Draw tube segments toward each connected neighbour
        for (dir in connections) {
            val (rectTl, rectSize) = tubeRect(dir, center, cellSize, tubeRadius)
            drawRect(color = drawColor, topLeft = rectTl, size = rectSize)
        }

        // Centre cap (circle) — covers rect joints
        drawCircle(color = drawColor, radius = tubeRadius, center = center)

        if (isEndpoint) {
            // Outer ring to mark endpoint
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = tubeRadius + cellSize * 0.06f,
                center = center,
                style = Stroke(width = cellSize * 0.05f)
            )
            // Number glyph for accessibility
            val label = ((pairIndex % pairHues.size) + 1).toString()
            val style = TextStyle(
                color = Color.White,
                fontSize = (tubeRadius * 1.5f / density).sp,
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

/**
 * Computes [CellDrawInfo] for every occupied cell in [state].
 * For each cell in a path, records which orthogonal directions connect to the
 * previous or next cell in that same path.
 */
private fun buildCellInfo(state: NumberlinkState): Map<GridPos, CellDrawInfo> {
    val result = mutableMapOf<GridPos, CellDrawInfo>()
    for (path in state.paths) {
        val cells = path.cells
        for (idx in cells.indices) {
            val pos = cells[idx]
            val connections = Direction.entries.filter { dir ->
                val neighbor = pos.step(dir)
                (idx > 0 && cells[idx - 1] == neighbor) ||
                    (idx < cells.size - 1 && cells[idx + 1] == neighbor)
            }.toSet()
            result[pos] = CellDrawInfo(path.pairIndex, connections)
        }
    }
    return result
}

private data class CellDrawInfo(val pairIndex: Int, val connections: Set<Direction>)

/** Returns the [topLeft, size] rectangle for a tube arm extending in [dir] from [center]. */
private fun tubeRect(
    dir: Direction,
    center: Offset,
    cellSize: Float,
    tubeRadius: Float
): Pair<Offset, Size> {
    val halfCell = cellSize / 2f
    return when (dir) {
        Direction.LEFT -> Offset(center.x - halfCell, center.y - tubeRadius) to Size(halfCell, tubeRadius * 2)
        Direction.RIGHT -> Offset(center.x, center.y - tubeRadius) to Size(halfCell, tubeRadius * 2)
        Direction.UP -> Offset(center.x - tubeRadius, center.y - halfCell) to Size(tubeRadius * 2, halfCell)
        Direction.DOWN -> Offset(center.x - tubeRadius, center.y) to Size(tubeRadius * 2, halfCell)
    }
}
