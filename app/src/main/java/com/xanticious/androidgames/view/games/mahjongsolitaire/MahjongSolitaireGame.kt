package com.xanticious.androidgames.view.games.mahjongsolitaire

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.mahjongsolitaire.MahjongSolitaireController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.mahjongsolitaire.MahjongLayout
import com.xanticious.androidgames.model.games.mahjongsolitaire.MahjongSolitaireState
import com.xanticious.androidgames.model.games.mahjongsolitaire.TileFace
import com.xanticious.androidgames.model.games.mahjongsolitaire.TileFaceSet
import com.xanticious.androidgames.model.games.mahjongsolitaire.TileSlot
import com.xanticious.androidgames.model.games.mahjongsolitaire.TileSuit
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
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
import com.xanticious.androidgames.ui.theme.PuzzleHueYellow
import com.xanticious.androidgames.ui.theme.PuzzlePlayerAlt
import com.xanticious.androidgames.ui.theme.PuzzleSolved
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.puzzle.OptionChips
import com.xanticious.androidgames.view.common.puzzle.PuzzleActionBar
import com.xanticious.androidgames.view.common.puzzle.PuzzleHowToPlayScreen
import com.xanticious.androidgames.view.common.puzzle.PuzzleSettingsScreen
import kotlin.random.Random

/**
 * Mahjong Solitaire — match free tile pairs to clear the board.
 * (`design/puzzle-games/mahjong-solitaire`). Self-configured via [PuzzleStateMachine].
 *
 * Tiles are rendered back-to-front on a Canvas with a per-layer pixel offset to
 * convey depth. Faces are drawn procedurally from glyphs and palette tokens —
 * no image assets are used. Victory appears below the board in the status slot
 * per the non-negotiable rule.
 */
@Composable
fun MahjongSolitaireGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { MahjongSolitaireController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    var selectedLayout by rememberSaveable { mutableStateOf(MahjongLayout.TURTLE) }
    var hintsEnabled by rememberSaveable { mutableStateOf(true) }
    var guaranteedSolvable by rememberSaveable { mutableStateOf(true) }
    var faceSet by rememberSaveable { mutableStateOf(TileFaceSet.TRADITIONAL) }

    var state by remember {
        mutableStateOf(MahjongSolitaireState(slots = emptyList()))
    }

    fun deal() {
        state = controller.newGame(selectedLayout, Random.Default, guaranteedSolvable)
            .copy(
                hintsEnabled = hintsEnabled,
                faceSet = faceSet,
                layout = selectedLayout,
                guaranteedSolvable = guaranteedSolvable
            )
    }

    fun onTileTap(slotId: Int) {
        val next = controller.tryMatch(state, slotId)
        state = next
        if (controller.isSolved(next)) machine.solved()
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Mahjong Solitaire", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Mahjong Solitaire",
                onHowToPlay = machine::openHowToPlay,
                onStart = { deal(); machine.startGame() }
            ) {
                OptionChips(
                    label = "Layout",
                    options = MahjongLayout.entries,
                    selected = selectedLayout,
                    onSelect = { selectedLayout = it },
                    labelOf = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
                )
                OptionChips(
                    label = "Guaranteed solvable",
                    options = listOf(true, false),
                    selected = guaranteedSolvable,
                    onSelect = { guaranteedSolvable = it },
                    labelOf = { if (it) "On" else "Off" }
                )
                OptionChips(
                    label = "Match hints",
                    options = listOf(true, false),
                    selected = hintsEnabled,
                    onSelect = { hintsEnabled = it },
                    labelOf = { if (it) "On" else "Off" }
                )
                OptionChips(
                    label = "Tile face set",
                    options = TileFaceSet.entries,
                    selected = faceSet,
                    onSelect = { faceSet = it },
                    labelOf = { if (it == TileFaceSet.TRADITIONAL) "Traditional" else "High-contrast" }
                )
            }
        }

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Mahjong Solitaire", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                Text("Tap a free tile to select it, then tap a matching free tile to remove the pair.")
                Text("A tile is free if nothing sits on top of it and at least one of its left or right sides is open.")
                Text("Tiles match if they are identical, or both Flowers, or both Seasons.")
                Text("Clear all 72 pairs to win. Use Hint to highlight a valid pair, Undo to restore the last pair, or Shuffle to rearrange remaining tiles.")
            }
        }

        else -> {
            val solved = phase == PuzzlePhase.SOLVED
            val stuck = !solved && controller.isStuck(state)

            GameScaffold(
                title = "Mahjong Solitaire",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "${state.pairsRemoved}/${state.totalPairs} pairs",
                        center = state.layout.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        right = "${state.activeTiles.size} tiles"
                    )
                },
                status = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (solved) {
                            VictoryPanel(
                                score = state.pairsRemoved,
                                bestScore = state.totalPairs,
                                stars = 3,
                                onReplay = { deal(); machine.retry() },
                                onMenu = machine::newGame,
                                headline = "Board cleared!",
                                primaryLabel = "New Game"
                            )
                        } else {
                            if (stuck) {
                                Text(
                                    "No moves available — use Shuffle!",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                            PuzzleActionBar(
                                status = if (state.shufflesUsed > 0) "Shuffles: ${state.shufflesUsed}" else "",
                                onUndo = if (state.canUndo) {
                                    { state = controller.undo(state) }
                                } else null,
                                undoEnabled = state.canUndo,
                                onHint = if (hintsEnabled) {
                                    { state = controller.showHint(state) }
                                } else null,
                                onNew = { deal() },
                                extras = {
                                    TextButton(onClick = {
                                        state = controller.shuffle(state)
                                    }) { Text("Shuffle") }
                                }
                            )
                        }
                    }
                }
            ) {
                if (state.slots.isNotEmpty()) {
                    MahjongBoard(
                        state = state,
                        onTileTap = { if (!solved) onTileTap(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

// ── Board rendering ───────────────────────────────────────────────────────────

@Composable
private fun MahjongBoard(
    state: MahjongSolitaireState,
    onTileTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val positions = remember(state.layout) {
        MahjongSolitaireController().layoutPositions(state.layout)
    }

    BoxWithConstraints(modifier = modifier) {
        val canvasW = constraints.maxWidth.toFloat()
        val canvasH = constraints.maxHeight.toFloat()

        val geometry = remember(state.layout, canvasW, canvasH) {
            computeGeometry(positions, canvasW, canvasH)
        }

        fun slotScreen(slot: TileSlot): Offset {
            val col = (slot.x - geometry.minX) / 2
            val row = (slot.y - geometry.minY) / 2
            return Offset(
                geometry.marginX + col * geometry.tileSize + slot.layer * geometry.layerOff,
                geometry.marginY + row * geometry.tileSize - slot.layer * geometry.layerOff
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state, geometry) {
                    detectTapGestures { tapOffset ->
                        // Hit-test front-to-back (highest layer first, then highest y)
                        val tapped = state.activeTiles
                            .sortedWith(compareByDescending<TileSlot> { it.layer }.thenByDescending { it.y })
                            .firstOrNull { slot ->
                                val pos = slotScreen(slot)
                                tapOffset.x >= pos.x && tapOffset.x <= pos.x + geometry.tileSize &&
                                    tapOffset.y >= pos.y && tapOffset.y <= pos.y + geometry.tileSize
                            }
                        if (tapped != null) onTileTap(tapped.id)
                    }
                }
        ) {
            drawRect(color = PuzzleBoard, size = Size(canvasW, canvasH))

            // Draw back-to-front: lower layers first, lower y first within a layer
            val drawOrder = state.activeTiles
                .sortedWith(compareBy<TileSlot> { it.layer }.thenBy { it.y }.thenBy { it.x })

            val active = state.activeTiles

            drawOrder.forEach { slot ->
                val pos = slotScreen(slot)
                val isSelected = slot.id == state.selectedId
                val isHinted = state.hintIds?.let { slot.id == it.first || slot.id == it.second } == true
                val isFreeSlot = MahjongSolitaireController().isFree(slot, active)

                drawTile(
                    slot = slot,
                    topLeft = pos,
                    size = geometry.tileSize,
                    isFree = isFreeSlot,
                    isSelected = isSelected,
                    isHinted = isHinted,
                    faceSet = state.faceSet,
                    textMeasurer = textMeasurer
                )
            }
        }
    }
}

// ── Geometry helper ───────────────────────────────────────────────────────────

private data class BoardGeometry(
    val tileSize: Float,
    val layerOff: Float,
    val marginX: Float,
    val marginY: Float,
    val minX: Int,
    val minY: Int
)

private fun computeGeometry(
    positions: List<Triple<Int, Int, Int>>,
    canvasW: Float,
    canvasH: Float
): BoardGeometry {
    if (positions.isEmpty()) return BoardGeometry(32f, 4f, 0f, 0f, 0, 0)
    val minX = positions.minOf { it.first }
    val minY = positions.minOf { it.second }
    val maxX = positions.maxOf { it.first }
    val maxY = positions.maxOf { it.second }
    val maxLayer = positions.maxOf { it.third }
    val numCols = (maxX - minX) / 2 + 1
    val numRows = (maxY - minY) / 2 + 1
    val layerRatio = 0.25f
    val tileSize = minOf(
        canvasW / (numCols + maxLayer * layerRatio),
        canvasH / (numRows + maxLayer * layerRatio)
    ).coerceAtLeast(8f)
    val layerOff = tileSize * layerRatio
    val totalW = numCols * tileSize + maxLayer * layerOff
    val totalH = numRows * tileSize + maxLayer * layerOff
    val marginX = ((canvasW - totalW) / 2f).coerceAtLeast(0f)
    val marginY = ((canvasH - totalH) / 2f).coerceAtLeast(0f) + maxLayer * layerOff
    return BoardGeometry(tileSize, layerOff, marginX, marginY, minX, minY)
}

// ── Tile drawing ──────────────────────────────────────────────────────────────

private fun DrawScope.drawTile(
    slot: TileSlot,
    topLeft: Offset,
    size: Float,
    isFree: Boolean,
    isSelected: Boolean,
    isHinted: Boolean,
    faceSet: TileFaceSet,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val inset = size * 0.06f
    val tl = Offset(topLeft.x + inset, topLeft.y + inset)
    val innerSize = Size(size - inset * 2, size - inset * 2)
    val cornerR = CornerRadius(size * 0.14f)
    val alpha = if (isFree) 1f else 0.35f

    // Shadow strip (gives depth illusion for stacking)
    drawRoundRect(
        color = PuzzleCell.copy(alpha = alpha * 0.7f),
        topLeft = Offset(topLeft.x + inset + size * 0.05f, topLeft.y + inset + size * 0.05f),
        size = innerSize,
        cornerRadius = cornerR
    )

    // Tile body
    val bodyColor = when {
        isSelected -> PuzzleHighlight.copy(alpha = alpha)
        isHinted -> GameAccent.copy(alpha = alpha)
        isFree -> PuzzleGridLine.copy(alpha = alpha)
        else -> PuzzleCell.copy(alpha = alpha)
    }
    drawRoundRect(color = bodyColor, topLeft = tl, size = innerSize, cornerRadius = cornerR)

    // Outline
    val outlineColor = when {
        isSelected -> PuzzleHighlight
        isHinted -> GameAccent
        isFree -> PuzzleGiven.copy(alpha = 0.6f)
        else -> PuzzleCell.copy(alpha = 0.3f)
    }
    drawRoundRect(
        color = outlineColor,
        topLeft = tl,
        size = innerSize,
        cornerRadius = cornerR,
        style = Stroke(width = if (isSelected || isHinted) size * 0.06f else size * 0.03f)
    )

    // Tile face glyph
    slot.face?.let { face ->
        val glyph = tileGlyph(face, faceSet)
        val suitColor = suitColor(face.suit).copy(alpha = alpha)
        val fontSize = if (faceSet == TileFaceSet.HIGH_CONTRAST) (size * 0.38f / density).sp
        else (size * 0.30f / density).sp
        val style = TextStyle(color = suitColor, fontSize = fontSize, fontWeight = FontWeight.Bold)
        val layout = textMeasurer.measure(AnnotatedString(glyph), style)
        drawText(
            layout,
            topLeft = Offset(
                tl.x + (innerSize.width - layout.size.width) / 2f,
                tl.y + (innerSize.height - layout.size.height) / 2f
            )
        )
    }
}

private fun tileGlyph(face: TileFace, faceSet: TileFaceSet): String {
    if (faceSet == TileFaceSet.HIGH_CONTRAST) return highContrastGlyph(face)
    return when (face.suit) {
        TileSuit.DOTS -> "${face.rank}\u25CF"           // number + filled circle
        TileSuit.BAMBOO -> "${face.rank}|"              // number + bamboo stalk
        TileSuit.CHARACTERS -> "${face.rank}C"          // number + C
        TileSuit.WIND -> windGlyph(face.rank)
        TileSuit.DRAGON -> dragonGlyph(face.rank)
        TileSuit.FLOWER -> "F${face.rank}"
        TileSuit.SEASON -> seasonGlyph(face.rank)
    }
}

private fun highContrastGlyph(face: TileFace): String = when (face.suit) {
    TileSuit.DOTS -> face.rank.toString()
    TileSuit.BAMBOO -> "B${face.rank}"
    TileSuit.CHARACTERS -> "C${face.rank}"
    TileSuit.WIND -> windGlyph(face.rank)
    TileSuit.DRAGON -> dragonGlyph(face.rank)
    TileSuit.FLOWER -> "FL${face.rank}"
    TileSuit.SEASON -> "SE${face.rank}"
}

private fun windGlyph(rank: Int): String = when (rank) {
    1 -> "E"; 2 -> "S"; 3 -> "W"; else -> "N"
}

private fun dragonGlyph(rank: Int): String = when (rank) {
    1 -> "Rd"; 2 -> "Gn"; else -> "Wh"
}

private fun seasonGlyph(rank: Int): String = when (rank) {
    1 -> "Sp"; 2 -> "Su"; 3 -> "Au"; else -> "Wi"
}

private fun suitColor(suit: TileSuit): Color = when (suit) {
    TileSuit.DOTS -> PuzzleHueBlue
    TileSuit.BAMBOO -> PuzzleHueGreen
    TileSuit.CHARACTERS -> PuzzleHueRed
    TileSuit.WIND -> PuzzleHueYellow
    TileSuit.DRAGON -> PuzzleHuePink
    TileSuit.FLOWER -> PuzzleHueTeal
    TileSuit.SEASON -> PuzzleHueOrange
}
