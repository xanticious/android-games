package com.xanticious.androidgames.view.games.jigsaw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.jigsaw.JigsawController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.jigsaw.JigsawState
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.ui.theme.PuzzleBoard
import com.xanticious.androidgames.ui.theme.PuzzleCell
import com.xanticious.androidgames.ui.theme.PuzzleGiven
import com.xanticious.androidgames.ui.theme.PuzzleGridLine
import com.xanticious.androidgames.ui.theme.PuzzleHighlight
import com.xanticious.androidgames.ui.theme.PuzzleSolved
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.puzzle.DifficultyChips
import com.xanticious.androidgames.view.common.puzzle.HowToPlaySection
import com.xanticious.androidgames.view.common.puzzle.OptionChips
import com.xanticious.androidgames.view.common.puzzle.PuzzleBoard
import com.xanticious.androidgames.view.common.puzzle.PuzzleHowToPlayScreen
import com.xanticious.androidgames.view.common.puzzle.PuzzleSettingsScreen

private val PieceBankItemSize = 72.dp
private val PieceBankPadding = 8.dp

/**
 * Jigsaw Puzzle — rebuild a procedurally-drawn picture from shuffled pieces
 * (`design/puzzle-games/jigsaw`). Self-configured via [PuzzleStateMachine].
 *
 * Interaction: tap a piece in the bank to select it (highlighted); tap the board
 * cell where it belongs to snap it in. Pieces only lock at their correct position
 * (snap-only-correct). The VictoryPanel appears below the board on completion.
 */
@Composable
fun JigsawGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { JigsawController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    val defaultGrid = when (difficulty) {
        GameDifficulty.EASY -> controller.gridSizes[0]
        GameDifficulty.MEDIUM -> controller.gridSizes[1]
        GameDifficulty.HARD -> controller.gridSizes[2]
    }
    var selectedImageId by rememberSaveable { mutableStateOf(0) }
    var selectedGrid by rememberSaveable { mutableStateOf(defaultGrid) }
    var gameState by remember { mutableStateOf<JigsawState?>(null) }
    var selectedPieceId by remember { mutableStateOf<Int?>(null) }
    var showReference by rememberSaveable { mutableStateOf(true) }

    fun deal() {
        selectedPieceId = null
        gameState = controller.newGame(selectedImageId, selectedGrid.first, selectedGrid.second)
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Jigsaw", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Jigsaw Puzzle",
                onHowToPlay = machine::openHowToPlay,
                onStart = { deal(); machine.startGame() }
            ) {
                // Image picker
                OptionChips(
                    label = "Image",
                    options = controller.imageIds,
                    selected = selectedImageId,
                    onSelect = { selectedImageId = it },
                    labelOf = { imageForId(it).title }
                )
                Spacer(Modifier.height(4.dp))
                // Board size
                OptionChips(
                    label = "Pieces",
                    options = controller.gridSizes,
                    selected = selectedGrid,
                    onSelect = { selectedGrid = it },
                    labelOf = { "${it.first * it.second} (${it.first}×${it.second})" }
                )
                Spacer(Modifier.height(4.dp))
                // Reference image toggle
                OptionChips(
                    label = "Reference",
                    options = listOf(true, false),
                    selected = showReference,
                    onSelect = { showReference = it },
                    labelOf = { if (it) "Show" else "Hide" }
                )
            }
        }

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Jigsaw", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                HowToPlaySection(title = "Goal") {
                    Text("Rebuild the picture by placing all pieces onto the board.")
                }
                HowToPlaySection(title = "Placing pieces") {
                    Text("Tap a piece in the piece bank at the bottom to select it (it will glow).")
                    Text("Then tap the board cell where you think it belongs. It only locks in at its correct spot — wrong taps are simply ignored.")
                }
                HowToPlaySection(title = "Hints") {
                    Text("Enable the reference image in Settings to see a faint guide behind empty board cells.")
                    Text("Pieces are never rotated — every piece is already in the right orientation.")
                }
                HowToPlaySection(title = "Completion") {
                    Text("The puzzle is complete when all pieces are placed. There is no timer and no losing!")
                }
            }
        }

        else -> {
            val state = gameState ?: return@let
            val solved = phase == PuzzlePhase.SOLVED
            val image = imageForId(state.imageId)

            GameScaffold(
                title = "Jigsaw",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "${state.placedCount}/${state.totalPieces} placed",
                        center = "${state.rows}×${state.cols}",
                        right = image.title
                    )
                },
                status = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (solved) {
                            VictoryPanel(
                                score = state.placedCount,
                                bestScore = state.totalPieces,
                                stars = 3,
                                onReplay = { deal(); machine.retry() },
                                onMenu = machine::newGame,
                                headline = "Puzzle Complete!",
                                primaryLabel = "New Puzzle"
                            )
                        } else {
                            PieceBankStrip(
                                state = state,
                                selectedPieceId = selectedPieceId,
                                image = image,
                                onPieceSelected = { pieceId ->
                                    selectedPieceId = if (selectedPieceId == pieceId) null else pieceId
                                }
                            )
                            if (selectedPieceId != null) {
                                Text(
                                    text = "Tap the board to place the selected piece",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PuzzleHighlight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            ) {
                JigsawBoard(
                    state = state,
                    image = image,
                    selectedPieceId = selectedPieceId,
                    showReference = showReference,
                    enabled = !solved,
                    onCellTap = { pos ->
                        val pid = selectedPieceId ?: return@JigsawBoard
                        val next = controller.placePiece(state, pid, pos.row, pos.col)
                        if (next != null) {
                            gameState = next
                            selectedPieceId = null
                            if (controller.isSolved(next)) machine.solved()
                        }
                        // wrong cell: ignore silently (snap-only-correct)
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Board
// ---------------------------------------------------------------------------

@Composable
private fun JigsawBoard(
    state: JigsawState,
    image: ProceduralImage,
    selectedPieceId: Int?,
    showReference: Boolean,
    enabled: Boolean,
    onCellTap: (GridPos) -> Unit
) {
    // Determine which cell (if any) the selected piece belongs to, for highlight.
    val selectedPiece = selectedPieceId?.let { pid -> state.pieces.firstOrNull { it.id == pid } }

    PuzzleBoard(
        rows = state.rows,
        cols = state.cols,
        drawGridLines = false,
        maxCellSize = 88.dp,
        onCellTap = if (enabled) onCellTap else null
    ) {
        val boardW = state.cols * cellSize
        val boardH = state.rows * cellSize
        val pos = current
        val isPlaced = state.placedPieces.containsKey(pos)
        val isTarget = selectedPiece != null &&
                selectedPiece.correctRow == pos.row &&
                selectedPiece.correctCol == pos.col

        if (isPlaced) {
            // Draw this piece's crop of the procedural image.
            // Clip to cell bounds, then translate so the full-image origin aligns
            // with the board's top-left corner (originX, originY).
            withTransform({
                clipRect(
                    left = topLeft.x, top = topLeft.y,
                    right = topLeft.x + cellSize, bottom = topLeft.y + cellSize
                )
                translate(left = originX, top = originY)
            }) {
                image.draw(this, boardW, boardH)
            }
            // Subtle solved border
            drawRect(
                color = PuzzleSolved.copy(alpha = 0.35f),
                topLeft = topLeft,
                size = Size(cellSize, cellSize),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = cellSize * 0.03f)
            )
        } else {
            // Empty cell background
            drawRect(color = PuzzleCell, topLeft = topLeft, size = Size(cellSize, cellSize))
            if (showReference) {
                // Faint full-image reference behind empty cells
                withTransform({
                    clipRect(
                        left = topLeft.x, top = topLeft.y,
                        right = topLeft.x + cellSize, bottom = topLeft.y + cellSize
                    )
                    translate(left = originX, top = originY)
                }) {
                    val alphaLayer = androidx.compose.ui.graphics.Paint().also { it.alpha = 0.18f }
                    drawContext.canvas.saveLayer(
                        androidx.compose.ui.geometry.Rect(
                            topLeft.x - originX, topLeft.y - originY,
                            topLeft.x - originX + cellSize, topLeft.y - originY + cellSize
                        ),
                        alphaLayer
                    )
                    image.draw(this, boardW, boardH)
                    drawContext.canvas.restore()
                }
            }
            // Target cell highlight when this is the selected piece's home
            if (isTarget) {
                drawRect(
                    color = PuzzleHighlight.copy(alpha = 0.35f),
                    topLeft = topLeft,
                    size = Size(cellSize, cellSize)
                )
            }
            // Grid line
            drawRect(
                color = PuzzleGridLine,
                topLeft = topLeft,
                size = Size(cellSize, cellSize),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = cellSize * 0.02f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Piece bank
// ---------------------------------------------------------------------------

@Composable
private fun PieceBankStrip(
    state: JigsawState,
    selectedPieceId: Int?,
    image: ProceduralImage,
    onPieceSelected: (Int) -> Unit
) {
    val unplacedPieces = state.bankOrder.mapNotNull { pid -> state.pieces.firstOrNull { it.id == pid } }

    Column(modifier = Modifier.fillMaxWidth().background(Dark0).padding(vertical = PieceBankPadding)) {
        if (unplacedPieces.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(PieceBankItemSize), contentAlignment = Alignment.Center) {
                Text("All pieces placed!", color = PuzzleSolved, fontWeight = FontWeight.Bold)
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PieceBankPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = PieceBankPadding)
            ) {
                items(items = unplacedPieces, key = { it.id }) { piece ->
                    val isSelected = piece.id == selectedPieceId
                    PieceBankItem(
                        piece = piece,
                        rows = state.rows,
                        cols = state.cols,
                        image = image,
                        isSelected = isSelected,
                        onClick = { onPieceSelected(piece.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PieceBankItem(
    piece: com.xanticious.androidgames.model.games.jigsaw.JigsawPiece,
    rows: Int,
    cols: Int,
    image: ProceduralImage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) PuzzleHighlight else Dark2
    val borderWidth = if (isSelected) 3.dp else 1.dp

    Box(
        modifier = Modifier
            .size(PieceBankItemSize)
            .clip(RoundedCornerShape(6.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.size(PieceBankItemSize)) {
            val itemPx = size.width
            val fullW = itemPx * cols
            val fullH = itemPx * rows
            // Clip to item bounds and translate to show only this piece's crop
            withTransform({
                clipRect(0f, 0f, itemPx, itemPx)
                translate(
                    left = -piece.correctCol * itemPx,
                    top = -piece.correctRow * itemPx
                )
            }) {
                image.draw(this, fullW, fullH)
            }
        }
        // Selection glow overlay
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(PieceBankItemSize)
                    .background(PuzzleHighlight.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            )
        }
    }
}
