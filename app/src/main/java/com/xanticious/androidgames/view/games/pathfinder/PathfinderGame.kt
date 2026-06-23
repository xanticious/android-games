package com.xanticious.androidgames.view.games.pathfinder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.xanticious.androidgames.controller.games.pathfinder.PathfinderController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.pathfinder.EndpointGlyph
import com.xanticious.androidgames.model.games.pathfinder.PathfinderState
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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val pairHues = listOf(
    PuzzleHueRed, PuzzleHueOrange, PuzzleHueYellow, PuzzleHueGreen,
    PuzzleHueTeal, PuzzleHueBlue, PuzzleHueViolet, PuzzleHuePink
)

private fun hueFor(pairIndex: Int): Color = pairHues[pairIndex % pairHues.size]

/**
 * Pathfinder — draw non-crossing colored routes connecting matching dot pairs;
 * a perfect solve fills every cell (`design/puzzle-games/pathfinder/pathfinder-design.md`).
 *
 * Distinguished from Numberlink by its colored dot endpoints with shape glyphs (for
 * color-blind accessibility) and support for larger grids up to 13×13. Self-configured:
 * owns its Settings, How to Play and board screens via the shared [PuzzleStateMachine].
 */
@Composable
fun PathfinderGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { PathfinderController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    val defaultSize = when (difficulty) {
        GameDifficulty.EASY -> 5
        GameDifficulty.MEDIUM -> 8
        GameDifficulty.HARD -> 10
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
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Pathfinder", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Pathfinder",
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

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Pathfinder", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                Text("Connect each pair of matching colored dots with a route.")
                Text("Drag from a dot (or any part of an existing route) to draw.")
                Text("Routes cannot cross; for a perfect solve, cover every cell.")
                Text("Dragging through another route trims it from the crossing point.")
                Text("Tap a dot to clear its route. Undo reverts the last drawn route.")
            }
        }

        else -> {
            val solved = phase == PuzzlePhase.SOLVED
            var dragActive by remember { mutableStateOf(false) }

            GameScaffold(
                title = "Pathfinder",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "Pairs ${state.connectedPairCount}/${state.numPairs}",
                        center = "${state.size}×${state.size}",
                        right = if (state.requireFullCoverage)
                            "Filled ${state.filledCellCount}/${state.size * state.size}"
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
                                    "Filled ${state.filledCellCount}/${state.size * state.size}"
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
                PathfinderBoard(
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
private fun PathfinderBoard(
    state: PathfinderState,
    enabled: Boolean,
    onDragStart: (GridPos) -> Unit,
    onDragCell: (GridPos) -> Unit,
    onDragEnd: () -> Unit,
    onEndpointTap: (Int) -> Unit
) {
    val cellInfo = remember(state.paths) { buildCellInfo(state) }
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
        val info = cellInfo[current] ?: return@PuzzleBoard
        val color = hueFor(info.pairIndex)
        val tubeRadius = cellSize * 0.25f
        val isEndpoint = current in endpointMap
        val isActive = info.pairIndex == state.activePairIndex
        val drawColor = if (isActive) color else color.copy(alpha = 0.88f)

        // Tube arms extending toward each connected neighbour
        for (dir in info.connections) {
            val (tl, sz) = tubeRect(dir, center, cellSize, tubeRadius)
            drawRect(color = drawColor, topLeft = tl, size = sz)
        }

        // Centre cap — covers rectangle joints with a circle for rounded appearance
        drawCircle(color = drawColor, radius = tubeRadius, center = center)

        if (isEndpoint) {
            val glyph = EndpointGlyph.forPair(info.pairIndex)
            val glyphRadius = tubeRadius * 1.1f

            // White ring to mark this cell as an endpoint
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = glyphRadius + cellSize * 0.07f,
                center = center,
                style = Stroke(width = cellSize * 0.06f)
            )

            // Distinct shape glyph for color-blind accessibility
            drawEndpointGlyph(glyph, center, glyphRadius, Color.White.copy(alpha = 0.9f))
        }
    }
}

/** Draws the shape [glyph] centered at [center] with the given [radius] and [color]. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEndpointGlyph(
    glyph: EndpointGlyph,
    center: Offset,
    radius: Float,
    color: Color
) {
    when (glyph) {
        EndpointGlyph.CIRCLE ->
            drawCircle(color = color, radius = radius * 0.6f, center = center)

        EndpointGlyph.SQUARE ->
            drawRect(
                color = color,
                topLeft = Offset(center.x - radius * 0.75f, center.y - radius * 0.75f),
                size = Size(radius * 1.5f, radius * 1.5f)
            )

        EndpointGlyph.TRIANGLE -> {
            val path = Path().apply {
                moveTo(center.x, center.y - radius)
                lineTo(center.x + radius * 0.866f, center.y + radius * 0.5f)
                lineTo(center.x - radius * 0.866f, center.y + radius * 0.5f)
                close()
            }
            drawPath(path, color)
        }

        EndpointGlyph.DIAMOND -> {
            val path = Path().apply {
                moveTo(center.x, center.y - radius)
                lineTo(center.x + radius * 0.7f, center.y)
                lineTo(center.x, center.y + radius)
                lineTo(center.x - radius * 0.7f, center.y)
                close()
            }
            drawPath(path, color)
        }

        EndpointGlyph.PENTAGON -> {
            val path = Path().apply {
                for (i in 0 until 5) {
                    val angle = Math.toRadians(-90.0 + i * 72.0)
                    val x = center.x + radius * cos(angle).toFloat()
                    val y = center.y + radius * sin(angle).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(path, color)
        }

        EndpointGlyph.HEXAGON -> {
            val path = Path().apply {
                for (i in 0 until 6) {
                    val angle = Math.toRadians(i * 60.0)
                    val x = center.x + radius * cos(angle).toFloat()
                    val y = center.y + radius * sin(angle).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(path, color)
        }

        EndpointGlyph.CROSS -> {
            val arm = radius * 0.35f
            drawRect(
                color = color,
                topLeft = Offset(center.x - radius, center.y - arm),
                size = Size(radius * 2f, arm * 2f)
            )
            drawRect(
                color = color,
                topLeft = Offset(center.x - arm, center.y - radius),
                size = Size(arm * 2f, radius * 2f)
            )
        }

        EndpointGlyph.STAR -> {
            val path = Path().apply {
                for (i in 0 until 10) {
                    val angle = Math.toRadians(-90.0 + i * 36.0)
                    val r = if (i % 2 == 0) radius else radius * 0.45f
                    val x = center.x + r * cos(angle).toFloat()
                    val y = center.y + r * sin(angle).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(path, color)
        }
    }
}

/** Per-cell drawing data: which pair occupies the cell and which directions connect. */
private data class CellDrawInfo(val pairIndex: Int, val connections: Set<Direction>)

private fun buildCellInfo(state: PathfinderState): Map<GridPos, CellDrawInfo> {
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
