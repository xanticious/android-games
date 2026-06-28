package com.xanticious.androidgames.view.games.logicgrid

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.logicgrid.LogicGridController
import com.xanticious.androidgames.controller.games.logicgrid.LogicGridSize
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.logicgrid.CellMark
import com.xanticious.androidgames.model.games.logicgrid.LogicGridClue
import com.xanticious.androidgames.model.games.logicgrid.LogicGridState
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import com.xanticious.androidgames.ui.theme.PuzzleBoard
import com.xanticious.androidgames.ui.theme.PuzzleGiven
import com.xanticious.androidgames.ui.theme.PuzzleHighlight
import com.xanticious.androidgames.ui.theme.PuzzleHueRed
import com.xanticious.androidgames.ui.theme.PuzzlePlayerAlt
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.puzzle.OptionChips
import com.xanticious.androidgames.view.common.puzzle.PuzzleActionBar
import com.xanticious.androidgames.view.common.puzzle.PuzzleBoard
import com.xanticious.androidgames.view.common.puzzle.PuzzleHowToPlayScreen
import com.xanticious.androidgames.view.common.puzzle.PuzzleSettingsScreen

/**
 * Logic Grid — "Einstein/Zebra" deduction puzzle.
 *
 * Screen layout (playing phase):
 *   [HUD]
 *   [Board area]:
 *     • Clue list (scrollable, tappable to strike through)
 *     • Composite cross-reference matrices (one block per category pair)
 *   [Status]: VictoryPanel on solve, PuzzleActionBar otherwise
 *
 * Victory/defeat content lives in the GameScaffold `status` slot — never
 * overlaying the board.
 */
@Composable
fun LogicGridGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { LogicGridController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    val defaultSize: LogicGridSize = when (difficulty) {
        GameDifficulty.EASY -> controller.sizes[0]
        GameDifficulty.MEDIUM -> controller.sizes[1]
        GameDifficulty.HARD -> controller.sizes[2]
    }

    var selectedSize by rememberSaveable { mutableStateOf(defaultSize) }
    var autoEliminate by rememberSaveable { mutableStateOf(true) }
    var state by remember { mutableStateOf(controller.newGame(selectedSize)) }

    fun deal() {
        state = controller.newGame(selectedSize).copy(autoEliminate = autoEliminate)
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Logic Grid", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Logic Grid",
                onHowToPlay = machine::openHowToPlay,
                onStart = { deal(); machine.startGame() }
            ) {
                OptionChips(
                    label = "Size",
                    options = controller.sizes,
                    selected = selectedSize,
                    onSelect = { selectedSize = it },
                    labelOf = { "${it.numCats}×${it.numItems}" }
                )
                OptionChips(
                    label = "Auto-eliminate",
                    options = listOf(true, false),
                    selected = autoEliminate,
                    onSelect = { autoEliminate = it },
                    labelOf = { if (it) "On" else "Off" }
                )
            }
        }

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Logic Grid", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                Text("Read the clues and fill in the cross-reference grid.")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Tap a cell to cycle: blank → ✕ (no) → ● (yes) → blank.")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Each item belongs to exactly one item in every other category.")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "With auto-eliminate on, marking ● in a row/column " +
                        "automatically places ✕ everywhere else in that row/column."
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("The puzzle is solved when all categories are fully and consistently linked.")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Tap a clue to strike through it and track which ones you have used.")
            }
        }

        else -> {
            val solved = phase == PuzzlePhase.SOLVED
            val hasError = controller.hasContradiction(state)

            val totalPairs = state.numCats * (state.numCats - 1) / 2
            val filledYes = state.marks.values.count { it == CellMark.YES }
            val remaining = state.numItems * totalPairs - filledYes

            GameScaffold(
                title = "Logic Grid",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = if (hasError) "⚠ Error" else "Left: $remaining",
                        center = "${state.numCats}×${state.numItems}",
                        right = "Moves: ${state.moveCount}"
                    )
                },
                status = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (solved) {
                            VictoryPanel(
                                score = state.moveCount,
                                bestScore = state.moveCount,
                                stars = 3,
                                onReplay = { deal(); machine.retry() },
                                onMenu = machine::newGame,
                                headline = "Solved!",
                                primaryLabel = "New Puzzle"
                            )
                        } else {
                            PuzzleActionBar(
                                status = if (remaining == 0) "Check solution" else "$remaining left",
                                onNew = { deal() }
                            )
                        }
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    // ── Clue list ──────────────────────────────────────────
                    ClueList(
                        state = state,
                        onToggleStrike = { idx ->
                            state = state.copy(
                                struckClues = if (idx in state.struckClues)
                                    state.struckClues - idx
                                else
                                    state.struckClues + idx
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Cross-reference grid ───────────────────────────────
                    // Cell size scales down for larger puzzles
                    val cellDp: Dp = when {
                        state.numCats <= 3 -> 40.dp
                        state.numCats <= 4 -> 34.dp
                        else -> 28.dp
                    }
                    val labelDp = 52.dp

                    // Classic upper-triangular layout: row catA, columns catA+1..K-1
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        Column {
                            for (cA in 0 until state.numCats - 1) {
                                Row(verticalAlignment = Alignment.Top) {
                                    // Left spacer mirrors skipped columns for catA > 0
                                    Spacer(
                                        modifier = Modifier.width(
                                            labelDp + (cellDp * state.numItems + 2.dp) * cA
                                        )
                                    )
                                    // Sub-matrices for this row (catA vs each catB > catA)
                                    for (cB in cA + 1 until state.numCats) {
                                        SubMatrixBlock(
                                            cA = cA,
                                            cB = cB,
                                            state = state,
                                            enabled = !solved,
                                            cellDp = cellDp,
                                            labelDp = labelDp,
                                            onTap = { iA, iB ->
                                                val current = state.getMark(cA, iA, cB, iB)
                                                val next = current.cycle()
                                                state = controller.mark(state, cA, iA, cB, iB, next)
                                                if (controller.isSolved(state)) machine.solved()
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Clue list
// ---------------------------------------------------------------------------

@Composable
private fun ClueList(
    state: LogicGridState,
    onToggleStrike: (Int) -> Unit
) {
    Text(
        text = "Clues",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 2.dp)
    )
    state.clues.forEachIndexed { idx, clue ->
        val struck = idx in state.struckClues
        TextButton(
            onClick = { onToggleStrike(idx) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${idx + 1}. ${clue.toDisplayText(state.categories, state.items)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    textDecoration = if (struck) TextDecoration.LineThrough else null,
                    color = if (struck)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else
                        MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun LogicGridClue.toDisplayText(
    categories: List<String>,
    items: List<List<String>>
): String = when (this) {
    is LogicGridClue.Direct ->
        "${items[catA][itemA]}'s ${categories[catB]} is ${items[catB][itemB]}"
    is LogicGridClue.Negative ->
        "${items[catA][itemA]}'s ${categories[catB]} is not ${items[catB][itemB]}"
}

// ---------------------------------------------------------------------------
// Sub-matrix block
// ---------------------------------------------------------------------------

/**
 * One cross-reference sub-matrix for category pair ([cA], [cB]).
 * Rows = items of [cA], columns = items of [cB].
 * Column headers appear above; row labels appear to the left of each PuzzleBoard row.
 */
@Composable
private fun SubMatrixBlock(
    cA: Int,
    cB: Int,
    state: LogicGridState,
    enabled: Boolean,
    cellDp: Dp,
    labelDp: Dp,
    onTap: (itemA: Int, itemB: Int) -> Unit
) {
    val m = state.numItems
    val labelFontSp = (cellDp.value * 0.30f).sp

    Column {
        // Column headers (catB items)
        Row {
            Spacer(modifier = Modifier.width(labelDp))
            for (iB in 0 until m) {
                Box(
                    modifier = Modifier.size(cellDp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.items[cB][iB],
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = labelFontSp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        color = PuzzleGiven
                    )
                }
            }
        }
        // Rows: row label + one row of cells per catA item
        for (iA in 0 until m) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(labelDp)
                        .height(cellDp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = state.items[cA][iA],
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = labelFontSp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        color = PuzzlePlayerAlt,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                // One PuzzleBoard row for this catA item
                PuzzleBoard(
                    rows = 1,
                    cols = m,
                    maxCellSize = cellDp,
                    drawGridLines = true,
                    onCellTap = if (enabled) { pos -> onTap(iA, pos.col) } else null
                ) {
                    val mark = state.getMark(cA, iA, cB, current.col)
                    // Cell background
                    drawRect(
                        color = PuzzleBoard,
                        topLeft = topLeft,
                        size = Size(cellSize, cellSize)
                    )
                    when (mark) {
                        CellMark.YES -> {
                            // Filled circle (●)
                            val radius = (cellSize / 2f) * 0.65f
                            drawCircle(color = PuzzleHighlight, radius = radius, center = center)
                        }
                        CellMark.NO -> {
                            // Cross (✕)
                            val pad = cellSize * 0.22f
                            val sw = cellSize * 0.12f
                            drawLine(
                                color = PuzzleHueRed,
                                start = Offset(topLeft.x + pad, topLeft.y + pad),
                                end = Offset(topLeft.x + cellSize - pad, topLeft.y + cellSize - pad),
                                strokeWidth = sw
                            )
                            drawLine(
                                color = PuzzleHueRed,
                                start = Offset(topLeft.x + cellSize - pad, topLeft.y + pad),
                                end = Offset(topLeft.x + pad, topLeft.y + cellSize - pad),
                                strokeWidth = sw
                            )
                        }
                        CellMark.BLANK -> { /* empty — background already drawn */ }
                    }
                }
            }
        }
        // Pair label underneath this sub-matrix
        Text(
            text = "${state.categories[cA]} / ${state.categories[cB]}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            modifier = Modifier.padding(start = labelDp, top = 1.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Cycles the mark for a tap: BLANK → NO → YES → BLANK. */
private fun CellMark.cycle(): CellMark = when (this) {
    CellMark.BLANK -> CellMark.NO
    CellMark.NO -> CellMark.YES
    CellMark.YES -> CellMark.BLANK
}
