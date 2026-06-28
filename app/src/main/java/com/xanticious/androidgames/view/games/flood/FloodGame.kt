package com.xanticious.androidgames.view.games.flood

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.flood.FloodController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.flood.FloodState
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.model.games.puzzle.starsFor
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import com.xanticious.androidgames.ui.theme.PuzzleHueBlue
import com.xanticious.androidgames.ui.theme.PuzzleHueGreen
import com.xanticious.androidgames.ui.theme.PuzzleHueOrange
import com.xanticious.androidgames.ui.theme.PuzzleHuePink
import com.xanticious.androidgames.ui.theme.PuzzleHueRed
import com.xanticious.androidgames.ui.theme.PuzzleHueTeal
import com.xanticious.androidgames.ui.theme.PuzzleHueViolet
import com.xanticious.androidgames.ui.theme.PuzzleHueYellow
import com.xanticious.androidgames.ui.theme.PuzzleHighlight
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.puzzle.OptionChips
import com.xanticious.androidgames.view.common.puzzle.PuzzleActionBar
import com.xanticious.androidgames.view.common.puzzle.PuzzleBoard
import com.xanticious.androidgames.view.common.puzzle.PuzzleHowToPlayScreen
import com.xanticious.androidgames.view.common.puzzle.PuzzleSettingsScreen

/** The 8 flood colors mapped to Puzzle hue tokens, in order of color index 0–7. */
private val floodPalette: List<Color> = listOf(
    PuzzleHueRed,
    PuzzleHueOrange,
    PuzzleHueYellow,
    PuzzleHueGreen,
    PuzzleHueTeal,
    PuzzleHueBlue,
    PuzzleHueViolet,
    PuzzleHuePink
)

/**
 * Flood — color-flooding puzzle game (`design/puzzle-games/flood/flood-design.md`).
 *
 * Self-configured: owns its Settings, How to Play and board screens via the shared
 * [PuzzleStateMachine]. The player repeatedly selects a color to recolor the
 * top-left flood region, absorbing adjacent matching cells, until the whole board
 * is one color.
 */
@Composable
fun FloodGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { FloodController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    val defaultSize = when (difficulty) {
        GameDifficulty.EASY -> 10
        GameDifficulty.MEDIUM -> 14
        GameDifficulty.HARD -> 18
    }
    val defaultColorCount = when (difficulty) {
        GameDifficulty.EASY -> 4
        GameDifficulty.MEDIUM -> 6
        GameDifficulty.HARD -> 8
    }

    var selectedSize by rememberSaveable { mutableIntStateOf(defaultSize) }
    var selectedColorCount by rememberSaveable { mutableIntStateOf(defaultColorCount) }
    var selectedHandicap by rememberSaveable { mutableIntStateOf(4) }
    var state by remember {
        mutableStateOf(controller.newGame(defaultSize, defaultColorCount, handicap = 4))
    }

    fun deal() {
        state = controller.newGame(selectedSize, selectedColorCount, handicap = selectedHandicap)
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Flood", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Flood",
                onHowToPlay = machine::openHowToPlay,
                onStart = { deal(); machine.startGame() }
            ) {
                OptionChips(
                    label = "Board size",
                    options = controller.boardSizes,
                    selected = selectedSize,
                    onSelect = { selectedSize = it },
                    labelOf = { "${it}×$it" }
                )
                OptionChips(
                    label = "Number of colors",
                    options = controller.colorCounts,
                    selected = selectedColorCount,
                    onSelect = { selectedColorCount = it },
                    labelOf = { "$it" }
                )
                OptionChips(
                    label = "Target (above optimal)",
                    options = controller.handicaps,
                    selected = selectedHandicap,
                    onSelect = { selectedHandicap = it },
                    labelOf = { "+$it" }
                )
            }
        }

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Flood", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                Text("The top-left region is your flood. Tap a color button to repaint it and absorb any touching cells of that color.")
                Text("Keep choosing colors until the entire board is one solid color.")
                Text("Each board shows a par (optimal + your target). Try to finish at or under par — the result shows exactly how many moves above optimal you used.")
                Text("Use Undo freely at any time to rethink a move; it never counts against you.")
            }
        }

        else -> {
            val solved = phase == PuzzlePhase.SOLVED
            val region = remember(state.grid) {
                controller.floodRegionSet(state.grid, state.size)
            }
            val aboveOptimal = state.moves - state.minMoves
            val stars = starsFor(
                earned = true,
                good = state.moves <= state.par,
                great = aboveOptimal == 0
            )
            val parStatus = "Par: ${state.par}  (min ${state.minMoves}, +${state.handicap})"

            GameScaffold(
                title = "Flood",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "Moves: ${state.moves}",
                        center = "${state.size}×${state.size} · ${state.colorCount} colors",
                        right = "Par: ${state.par}"
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
                                headline = buildSolveHeadline(state.moves, state.minMoves),
                                primaryLabel = "New Board"
                            )
                        } else {
                            ColorPaletteRow(
                                colorCount = state.colorCount,
                                currentColor = state.currentColor,
                                enabled = true,
                                onColorSelect = { colorIndex ->
                                    state = controller.applyColor(state, colorIndex)
                                    if (controller.isSolved(state)) machine.solved()
                                }
                            )
                            PuzzleActionBar(
                                status = parStatus,
                                onUndo = { state = controller.undo(state) },
                                undoEnabled = state.canUndo,
                                onNew = { deal() }
                            )
                        }
                    }
                }
            ) {
                FloodBoard(
                    state = state,
                    region = region
                )
            }
        }
    }
}

private fun buildSolveHeadline(moves: Int, minMoves: Int): String {
    val above = moves - minMoves
    return if (above == 0) "Solved in $moves — optimal! 🎉"
    else "Solved in $moves — $above above optimal (+$above)"
}

@Composable
private fun ColorPaletteRow(
    colorCount: Int,
    currentColor: Int,
    enabled: Boolean,
    onColorSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until colorCount) {
            val isCurrent = i == currentColor
            val swatchColor = floodPalette[i % floodPalette.size]
            val swatchAlpha = if (isCurrent) 0.45f else 1f
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(swatchColor.copy(alpha = swatchAlpha))
                    .border(
                        width = if (isCurrent) 3.dp else 1.dp,
                        color = Color.White.copy(alpha = if (isCurrent) 0.9f else 0.25f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(enabled = enabled && !isCurrent) { onColorSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(22.dp)) {
                    drawColorGlyph(i, center, size.width * 0.9f)
                }
            }
        }
    }
}

@Composable
private fun FloodBoard(state: FloodState, region: Set<Int>) {
    val n = state.size
    PuzzleBoard(
        rows = n,
        cols = n,
        drawGridLines = false
    ) {
        val idx = current.row * n + current.col
        val colorIndex = state.grid[idx]
        val cellColor = floodPalette[colorIndex % floodPalette.size]
        val inRegion = idx in region

        // Fill cell
        drawRect(color = cellColor, topLeft = topLeft, size = Size(cellSize, cellSize))

        // Region outline: bright border around flood region cells
        if (inRegion) {
            drawRect(
                color = PuzzleHighlight.copy(alpha = 0.7f),
                topLeft = topLeft,
                size = Size(cellSize, cellSize),
                style = Stroke(width = cellSize * 0.09f)
            )
        }

        // Accessibility glyph (small shape inside cell; differs per color index)
        val glyphSize = cellSize * 0.38f
        drawColorGlyph(colorIndex, center, glyphSize)
    }
}

/**
 * Draws a small distinguishing glyph for each of the 8 possible color indices.
 * Shapes ensure color is never the sole differentiator (accessibility).
 * 0=Red: ● circle  1=Orange: ▲ triangle up  2=Yellow: ■ square  3=Green: ◆ diamond
 * 4=Teal: + plus   5=Blue: × cross           6=Violet: ○ ring   7=Pink: ▼ triangle down
 */
private fun DrawScope.drawColorGlyph(colorIndex: Int, center: Offset, glyphSize: Float) {
    val paint = Color.White.copy(alpha = 0.72f)
    val h = glyphSize / 2f
    val strokeW = glyphSize * 0.22f
    when (colorIndex % 8) {
        0 -> drawCircle(color = paint, radius = h, center = center)
        1 -> {
            val path = Path().apply {
                moveTo(center.x, center.y - h)
                lineTo(center.x + h, center.y + h)
                lineTo(center.x - h, center.y + h)
                close()
            }
            drawPath(path, paint)
        }
        2 -> drawRect(
            color = paint,
            topLeft = Offset(center.x - h, center.y - h),
            size = Size(glyphSize, glyphSize)
        )
        3 -> {
            val path = Path().apply {
                moveTo(center.x, center.y - h)
                lineTo(center.x + h, center.y)
                lineTo(center.x, center.y + h)
                lineTo(center.x - h, center.y)
                close()
            }
            drawPath(path, paint)
        }
        4 -> {
            drawLine(paint, Offset(center.x - h, center.y), Offset(center.x + h, center.y), strokeW)
            drawLine(paint, Offset(center.x, center.y - h), Offset(center.x, center.y + h), strokeW)
        }
        5 -> {
            drawLine(paint, Offset(center.x - h, center.y - h), Offset(center.x + h, center.y + h), strokeW)
            drawLine(paint, Offset(center.x + h, center.y - h), Offset(center.x - h, center.y + h), strokeW)
        }
        6 -> drawCircle(
            color = paint,
            radius = h,
            center = center,
            style = Stroke(width = strokeW)
        )
        7 -> {
            val path = Path().apply {
                moveTo(center.x, center.y + h)
                lineTo(center.x + h, center.y - h)
                lineTo(center.x - h, center.y - h)
                close()
            }
            drawPath(path, paint)
        }
    }
}
