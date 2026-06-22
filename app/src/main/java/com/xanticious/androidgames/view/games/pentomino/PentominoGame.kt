package com.xanticious.androidgames.view.games.pentomino

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.xanticious.androidgames.controller.games.pentomino.PentominoController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.pentomino.BoardSize
import com.xanticious.androidgames.model.games.pentomino.PentominoPiece
import com.xanticious.androidgames.model.games.pentomino.PentominoState
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
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
import com.xanticious.androidgames.ui.theme.PuzzlePlayer
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
 * Pentomino — fit the twelve pentominoes into a rectangular board.
 * Self-configured: owns its Settings and How to Play screens via [PuzzleStateMachine].
 */
@Composable
fun PentominoGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { PentominoController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    val defaultBoardSize = when (difficulty) {
        GameDifficulty.EASY -> BoardSize.SIX_BY_TEN
        GameDifficulty.MEDIUM -> BoardSize.FIVE_BY_TWELVE
        GameDifficulty.HARD -> BoardSize.FOUR_BY_FIFTEEN
    }

    var selectedBoardSize by remember { mutableStateOf(defaultBoardSize) }
    var allowFlip by remember { mutableStateOf(true) }
    var state by remember { mutableStateOf(controller.newGame(defaultBoardSize, allowFlip = true)) }
    var selectedPiece by remember { mutableStateOf<PentominoPiece?>(null) }
    var selectedOrientationIndex by remember { mutableIntStateOf(0) }

    fun startNewGame() {
        state = controller.newGame(selectedBoardSize, allowFlip)
        selectedPiece = null
        selectedOrientationIndex = 0
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Pentomino", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Pentomino",
                onHowToPlay = machine::openHowToPlay,
                onStart = { startNewGame(); machine.startGame() }
            ) {
                OptionChips(
                    label = "Board",
                    options = controller.boardSizes,
                    selected = selectedBoardSize,
                    onSelect = { selectedBoardSize = it },
                    labelOf = { it.label }
                )
                OptionChips(
                    label = "Allow flips",
                    options = listOf(true, false),
                    selected = allowFlip,
                    onSelect = { allowFlip = it },
                    labelOf = { if (it) "On" else "Off" }
                )
            }
        }

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Pentomino", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                Text("Drag a piece from the tray, or tap a tray piece to select it, then tap the board to place it.")
                Text("Tap a placed piece on the board to lift it back to the tray.")
                Text("Use Rotate (↻) to cycle through orientations. Use Flip (⇋) to try reflected orientations.")
                Text("Cover every cell of the board with all twelve pieces — no gaps, no overlaps.")
                Text("Use Undo to revert the last move, or Reset to clear the board.")
            }
        }

        else -> {
            val solved = phase == PuzzlePhase.SOLVED
            val currentOrientations = selectedPiece?.let {
                controller.orientations(it, state.allowFlip)
            } ?: emptyList()
            val safeOrientationIndex = if (currentOrientations.isEmpty()) 0
            else selectedOrientationIndex.coerceIn(0, currentOrientations.lastIndex)

            GameScaffold(
                title = "Pentomino",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "Placed ${state.placedCount}/12",
                        center = state.boardSize.label,
                        right = if (solved) "Solved!" else ""
                    )
                },
                status = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (solved) {
                            VictoryPanel(
                                score = state.placedCount,
                                bestScore = 12,
                                stars = 3,
                                onReplay = { startNewGame(); machine.retry() },
                                onMenu = machine::newGame,
                                headline = "Puzzle Solved!",
                                primaryLabel = "Play Again"
                            )
                        } else {
                            PieceTray(
                                remaining = state.remainingPieces,
                                placed = state.placedPieces,
                                selected = selectedPiece,
                                onSelect = { piece ->
                                    selectedPiece = if (selectedPiece == piece) null else piece
                                    selectedOrientationIndex = 0
                                }
                            )
                            PuzzleActionBar(
                                status = if (selectedPiece != null)
                                    "${selectedPiece!!.name} · orient ${safeOrientationIndex + 1}/${currentOrientations.size}"
                                else "",
                                onUndo = if (state.canUndo) {
                                    {
                                        state = controller.undo(state)
                                        selectedPiece = null
                                        selectedOrientationIndex = 0
                                    }
                                } else null,
                                undoEnabled = state.canUndo,
                                onReset = {
                                    state = controller.newGame(state.boardSize, state.allowFlip)
                                    selectedPiece = null
                                    selectedOrientationIndex = 0
                                },
                                extras = {
                                    if (selectedPiece != null && currentOrientations.isNotEmpty()) {
                                        TextButton(onClick = {
                                            selectedOrientationIndex =
                                                (safeOrientationIndex + 1) % currentOrientations.size
                                        }) { Text("↻") }
                                        if (state.allowFlip && currentOrientations.size > 1) {
                                            TextButton(onClick = {
                                                selectedOrientationIndex =
                                                    (safeOrientationIndex + currentOrientations.size / 2) %
                                                        currentOrientations.size
                                            }) { Text("⇋") }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            ) {
                PentominoBoard(
                    state = state,
                    selectedPiece = selectedPiece,
                    orientationIndex = safeOrientationIndex,
                    currentOrientations = currentOrientations,
                    enabled = !solved,
                    onCellTap = { pos ->
                        val piece = selectedPiece
                        if (piece != null && currentOrientations.isNotEmpty()) {
                            val orientation = currentOrientations[safeOrientationIndex]
                            if (controller.canPlace(state, orientation, pos)) {
                                state = controller.place(state, piece, orientation, pos)
                                selectedPiece = null
                                selectedOrientationIndex = 0
                                if (controller.isSolved(state)) machine.solved()
                            }
                        } else {
                            val occupant = state.cellMap[pos]
                            if (occupant != null) {
                                state = controller.remove(state, occupant)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PieceTray(
    remaining: List<PentominoPiece>,
    placed: Set<PentominoPiece>,
    selected: PentominoPiece?,
    onSelect: (PentominoPiece) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Tray:",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(end = 4.dp)
        )
        remaining.forEach { piece ->
            val isSelected = piece == selected
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = pieceColor(piece),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .then(
                        if (isSelected) Modifier.border(
                            width = 2.dp,
                            color = PuzzleHighlight,
                            shape = RoundedCornerShape(6.dp)
                        ) else Modifier
                    )
                    .clickable { onSelect(piece) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = piece.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = PuzzleCell
                )
            }
        }
        if (placed.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "✓ ${placed.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PentominoBoard(
    state: PentominoState,
    selectedPiece: PentominoPiece?,
    orientationIndex: Int,
    currentOrientations: List<List<GridPos>>,
    enabled: Boolean,
    onCellTap: (GridPos) -> Unit
) {
    val measurer = rememberTextMeasurer()
    val cellMap = state.cellMap

    val previewCells: Set<GridPos> = remember(selectedPiece, orientationIndex) {
        // Preview cells aren't shown on board (only highlight on tap), kept for future use.
        emptySet()
    }

    PuzzleBoard(
        rows = state.boardSize.rows,
        cols = state.boardSize.cols,
        drawGridLines = true,
        maxCellSize = 48.dp,
        onCellTap = if (enabled) onCellTap else null
    ) {
        val piece = cellMap[current]
        if (piece != null) {
            // Placed piece: filled rounded rectangle + letter
            val inset = cellSize * 0.06f
            val r = rect(inset)
            drawRoundRect(
                color = pieceColor(piece),
                topLeft = Offset(r.left, r.top),
                size = Size(r.width, r.height),
                cornerRadius = CornerRadius(cellSize * 0.16f)
            )
            val label = piece.name
            val style = TextStyle(
                color = PuzzleBoard,
                fontSize = (cellSize * 0.38f / density).sp,
                fontWeight = FontWeight.ExtraBold
            )
            val layout = measurer.measure(AnnotatedString(label), style)
            drawText(
                layout,
                topLeft = Offset(
                    center.x - layout.size.width / 2f,
                    center.y - layout.size.height / 2f
                )
            )
        } else {
            // Empty cell: subtle fill
            val inset = cellSize * 0.08f
            val r = rect(inset)
            drawRoundRect(
                color = PuzzleCell.copy(alpha = 0.6f),
                topLeft = Offset(r.left, r.top),
                size = Size(r.width, r.height),
                cornerRadius = CornerRadius(cellSize * 0.10f)
            )
        }
    }
}

/** Maps each pentomino letter to a distinct colour token from the puzzle palette. */
private fun pieceColor(piece: PentominoPiece): Color = when (piece) {
    PentominoPiece.F -> PuzzleHueRed
    PentominoPiece.I -> PuzzleHueOrange
    PentominoPiece.L -> PuzzleHueYellow
    PentominoPiece.N -> PuzzleHueGreen
    PentominoPiece.P -> PuzzleHueTeal
    PentominoPiece.T -> PuzzleHueBlue
    PentominoPiece.U -> PuzzleHueViolet
    PentominoPiece.V -> PuzzleHuePink
    PentominoPiece.W -> GameNeutral
    PentominoPiece.X -> PuzzleGiven
    PentominoPiece.Y -> PuzzlePlayer
    PentominoPiece.Z -> PuzzlePlayerAlt
}
