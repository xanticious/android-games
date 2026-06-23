package com.xanticious.androidgames.view.games.pegsolitaire

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import com.xanticious.androidgames.controller.games.pegsolitaire.PegSolitaireController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.pegsolitaire.BoardVariant
import com.xanticious.androidgames.model.games.pegsolitaire.CellState
import com.xanticious.androidgames.model.games.pegsolitaire.PegSolitaireState
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.model.games.puzzle.starsFor
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import com.xanticious.androidgames.ui.theme.PuzzleCell
import com.xanticious.androidgames.ui.theme.PuzzleGiven
import com.xanticious.androidgames.ui.theme.PuzzleGridLine
import com.xanticious.androidgames.ui.theme.PuzzleHighlight
import com.xanticious.androidgames.ui.theme.PuzzleSolved
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.puzzle.OptionChips
import com.xanticious.androidgames.view.common.puzzle.PuzzleActionBar
import com.xanticious.androidgames.view.common.puzzle.PuzzleBoard
import com.xanticious.androidgames.view.common.puzzle.PuzzleHowToPlayScreen
import com.xanticious.androidgames.view.common.puzzle.PuzzleSettingsScreen

/**
 * Peg Solitaire — jump pegs over each other to leave a single peg on the board
 * (`design/puzzle-games/peg-solitaire`). Self-configured via the shared
 * [PuzzleStateMachine]: Settings → How to Play → Playing → Solved/Failed.
 */
@Composable
fun PegSolitaireGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { PegSolitaireController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    val defaultVariant = when (difficulty) {
        GameDifficulty.EASY -> BoardVariant.PLUS
        GameDifficulty.MEDIUM -> BoardVariant.ENGLISH
        GameDifficulty.HARD -> BoardVariant.EUROPEAN
    }
    var selectedVariant by remember { mutableStateOf(defaultVariant) }
    var state by remember { mutableStateOf(controller.newGame(defaultVariant)) }
    // Selected peg for the tap-to-select → tap-destination interaction
    var selected by remember { mutableStateOf<GridPos?>(null) }

    fun deal() {
        state = controller.newGame(selectedVariant)
        selected = null
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Peg Solitaire", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Peg Solitaire",
                onHowToPlay = machine::openHowToPlay,
                onStart = { deal(); machine.startGame() }
            ) {
                OptionChips(
                    label = "Board",
                    options = BoardVariant.entries,
                    selected = selectedVariant,
                    onSelect = { selectedVariant = it },
                    labelOf = { it.label }
                )
            }
        }

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Peg Solitaire", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                Text("Tap a peg to select it (it will highlight), then tap an empty hole two cells away in a straight line.")
                Text("The peg in between is removed. Keep jumping until only one peg remains.")
                Text("If no jumps are left and more than one peg remains, the game is stuck — use Undo to backtrack or Reset to start over.")
                Text("Tip: finishing with the last peg in the center earns bonus stars!")
            }
        }

        else -> {
            val solved = phase == PuzzlePhase.SOLVED
            val failed = phase == PuzzlePhase.FAILED
            val pegs = controller.pegsRemaining(state)
            val legalFromSelected = selected?.let { controller.legalMovesFrom(state, it) } ?: emptyList()
            val legalDestinations = legalFromSelected.map { it.to }.toSet()

            GameScaffold(
                title = "Peg Solitaire",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "Pegs $pegs",
                        center = selectedVariant.label,
                        right = "Moves ${state.moves}"
                    )
                },
                status = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        when {
                            solved -> {
                                val center = controller.centerOf(selectedVariant)
                                val centerFinish = state.cell(center) == CellState.PEG
                                VictoryPanel(
                                    score = pegs,
                                    bestScore = 1,
                                    stars = starsFor(
                                        earned = true,
                                        good = true,         // always two stars for any win
                                        great = centerFinish // third star for center finish
                                    ),
                                    onReplay = { deal(); machine.retry() },
                                    onMenu = machine::newGame,
                                    headline = if (centerFinish) "Perfect! Center finish!" else "Solved!",
                                    primaryLabel = "New Game"
                                )
                            }
                            failed -> {
                                DefeatPanel(
                                    score = pegs,
                                    bestScore = 1,
                                    onTryAgain = {
                                        if (state.canUndo) {
                                            state = controller.undo(state)
                                            selected = null
                                            machine.retry()
                                        } else {
                                            deal()
                                            machine.retry()
                                        }
                                    },
                                    onMenu = machine::newGame,
                                    headline = "Stuck! No moves left."
                                )
                            }
                            else -> {
                                PuzzleActionBar(
                                    status = "$pegs pegs left",
                                    onUndo = {
                                        state = controller.undo(state)
                                        selected = null
                                    },
                                    undoEnabled = state.canUndo,
                                    onReset = { deal() },
                                    onNew = { machine.newGame() }
                                )
                            }
                        }
                    }
                }
            ) {
                PegBoard(
                    state = state,
                    selected = selected,
                    legalDestinations = legalDestinations,
                    enabled = !solved && !failed,
                    onCellTap = { pos ->
                        val cell = state.board[pos.row][pos.col]
                        val cur = selected
                        when {
                            cell == CellState.BLOCKED -> Unit
                            cur == null -> {
                                if (cell == CellState.PEG) selected = pos
                            }
                            pos == cur -> selected = null
                            legalDestinations.contains(pos) -> {
                                val jump = legalFromSelected.first { it.to == pos }
                                state = controller.applyJump(state, jump)
                                selected = null
                                when {
                                    controller.isSolved(state) -> machine.solved()
                                    !controller.hasMoves(state) -> machine.failed()
                                }
                            }
                            cell == CellState.PEG -> selected = pos
                            else -> selected = null
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PegBoard(
    state: PegSolitaireState,
    selected: GridPos?,
    legalDestinations: Set<GridPos>,
    enabled: Boolean,
    onCellTap: (GridPos) -> Unit
) {
    PuzzleBoard(
        rows = state.rows,
        cols = state.cols,
        drawGridLines = false,
        onCellTap = if (enabled) onCellTap else null
    ) {
        when (state.cell(current)) {
            CellState.BLOCKED -> Unit  // draw nothing — let the board background show

            CellState.EMPTY -> {
                // Small recessed hole; highlight if it's a legal jump destination
                val isTarget = legalDestinations.contains(current)
                val holeRadius = cellSize * 0.18f
                val ringRadius = cellSize * 0.30f
                drawCircle(
                    color = PuzzleCell,
                    radius = holeRadius,
                    center = center
                )
                if (isTarget) {
                    drawCircle(
                        color = PuzzleSolved,
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = cellSize * 0.06f)
                    )
                }
            }

            CellState.PEG -> {
                val isSelected = current == selected
                val pegRadius = cellSize * 0.38f
                // Peg body
                drawCircle(
                    color = if (isSelected) PuzzleSolved else PuzzleHighlight,
                    radius = pegRadius,
                    center = center
                )
                // Subtle border to give depth
                drawCircle(
                    color = if (isSelected) PuzzleGiven else PuzzleGridLine,
                    radius = pegRadius,
                    center = center,
                    style = Stroke(width = cellSize * 0.05f)
                )
                // Highlight glint on selected peg
                if (isSelected) {
                    drawCircle(
                        color = PuzzleSolved.copy(alpha = 0.5f),
                        radius = pegRadius * 0.35f,
                        center = Offset(center.x - pegRadius * 0.25f, center.y - pegRadius * 0.25f)
                    )
                }
            }
        }
    }
}
