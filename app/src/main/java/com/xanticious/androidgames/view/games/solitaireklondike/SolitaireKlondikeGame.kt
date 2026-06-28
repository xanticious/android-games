package com.xanticious.androidgames.view.games.solitaireklondike

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.solitaireklondike.KlondikeController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.cards.Suit
import com.xanticious.androidgames.model.games.solitaireklondike.KlondikeConfig
import com.xanticious.androidgames.model.games.solitaireklondike.KlondikeMove
import com.xanticious.androidgames.model.games.solitaireklondike.KlondikeState
import com.xanticious.androidgames.state.games.solitaireklondike.KlondikePhase
import com.xanticious.androidgames.state.games.solitaireklondike.KlondikeStateMachine
import com.xanticious.androidgames.ui.theme.CardHighlight
import com.xanticious.androidgames.ui.theme.CardTableFelt
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.cards.CardBackView
import com.xanticious.androidgames.view.common.cards.CardAspectRatio
import com.xanticious.androidgames.view.common.cards.EmptyCardSlot
import com.xanticious.androidgames.view.common.cards.PlayingCardView
import kotlinx.coroutines.delay

/** Source of the currently selected card or run. */
private sealed interface MoveSource {
    data class FromTableau(val col: Int) : MoveSource
    data object FromWaste : MoveSource
}

/** Tracks which card / run the user has tapped as the move source. */
private data class Selection(val source: MoveSource, val startIndex: Int)

/**
 * Solitaire (Klondike) — game id `solitaire-klondike`.
 *
 * Tap a face-up card to select it. Tap a valid destination (tableau column or
 * foundation) to complete the move. Tap the stock to draw. Double-tap a face-up
 * card to auto-move it to the appropriate foundation when legal.
 */
@Composable
fun SolitaireKlondikeGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val config = remember(difficulty) { KlondikeController.configFor(difficulty) }
    val machine = remember { KlondikeStateMachine() }
    val phase by machine.phase.collectAsState()

    var gameState by remember { mutableStateOf(KlondikeController.deal(System.currentTimeMillis(), config)) }
    var selection by remember { mutableStateOf<Selection?>(null) }
    val undoStack = remember { ArrayDeque<KlondikeState>() }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var hintMove by remember { mutableStateOf<KlondikeMove?>(null) }

    LaunchedEffect(Unit) {
        machine.startDeal()
        machine.dealComplete()
    }

    LaunchedEffect(phase) {
        if (phase == KlondikePhase.PLAYING) {
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    LaunchedEffect(phase) {
        if (phase == KlondikePhase.AUTO_FINISHING) {
            gameState = KlondikeController.autoFinish(gameState)
            machine.cascadeDone()
        }
    }

    fun checkEndConditions(newState: KlondikeState) {
        when {
            KlondikeController.isWon(newState) -> machine.allFoundationsComplete()
            !KlondikeController.hasLegalMove(newState) -> machine.noMovesLeft()
        }
    }

    fun commitMove(move: KlondikeMove) {
        val newState = KlondikeController.applyMove(gameState, move) ?: return
        undoStack.addLast(gameState)
        if (undoStack.size > 64) undoStack.removeFirst()
        gameState = newState
        selection = null
        hintMove = null
        if (phase == KlondikePhase.PLAYING) checkEndConditions(newState)
    }

    fun onStockClick() {
        if (gameState.stock.isNotEmpty()) commitMove(KlondikeMove.DrawStock)
        else commitMove(KlondikeMove.RecycleStock)
    }

    fun onWasteClick() {
        val sel = selection
        if (sel != null) {
            // Tapping waste while something is selected → deselect.
            selection = null
            return
        }
        if (gameState.waste.isNotEmpty()) {
            selection = Selection(MoveSource.FromWaste, gameState.waste.size - 1)
        }
    }

    fun onFoundationClick(foundIdx: Int) {
        val sel = selection
        if (sel != null) {
            val move = when (val src = sel.source) {
                is MoveSource.FromTableau -> KlondikeMove.TableauToFoundation(src.col, foundIdx)
                is MoveSource.FromWaste -> KlondikeMove.WasteToFoundation(foundIdx)
            }
            commitMove(move)
        } else {
            // Auto-move waste top to this foundation.
            val wasteTop = gameState.waste.lastOrNull() ?: return
            val fi = KlondikeController.foundationIndexFor(wasteTop, gameState.foundations)
            if (fi == foundIdx) commitMove(KlondikeMove.WasteToFoundation(foundIdx))
        }
    }

    fun onTableauCardClick(colIdx: Int, cardIdx: Int) {
        val sel = selection
        val card = gameState.tableau[colIdx].getOrNull(cardIdx) ?: return
        if (sel == null) {
            if (card.faceUp) selection = Selection(MoveSource.FromTableau(colIdx), cardIdx)
            return
        }
        // Try to move selected source to this column.
        val move: KlondikeMove = when (val src = sel.source) {
            is MoveSource.FromTableau -> KlondikeMove.TableauToTableau(src.col, sel.startIndex, colIdx)
            is MoveSource.FromWaste -> KlondikeMove.WasteToTableau(colIdx)
        }
        val newState = KlondikeController.applyMove(gameState, move)
        if (newState != null) {
            undoStack.addLast(gameState)
            if (undoStack.size > 64) undoStack.removeFirst()
            gameState = newState
            selection = null
            hintMove = null
            if (phase == KlondikePhase.PLAYING) checkEndConditions(newState)
        } else {
            // Move failed — reselect the tapped card if face-up.
            selection = if (card.faceUp) Selection(MoveSource.FromTableau(colIdx), cardIdx) else null
        }
    }

    fun onTableauEmptyClick(colIdx: Int) {
        val sel = selection ?: return
        val move: KlondikeMove = when (val src = sel.source) {
            is MoveSource.FromTableau -> KlondikeMove.TableauToTableau(src.col, sel.startIndex, colIdx)
            is MoveSource.FromWaste -> KlondikeMove.WasteToTableau(colIdx)
        }
        commitMove(move)
    }

    fun onDoubleTapCard(colIdx: Int, cardIdx: Int) {
        val col = gameState.tableau[colIdx]
        val card = col.getOrNull(cardIdx)?.takeIf { it.faceUp } ?: return
        // Only auto-move the top card of the column on double-tap.
        if (cardIdx != col.size - 1) return
        val fi = KlondikeController.foundationIndexFor(card, gameState.foundations) ?: return
        commitMove(KlondikeMove.TableauToFoundation(colIdx, fi))
    }

    fun onDoubleTapWaste() {
        val card = gameState.waste.lastOrNull() ?: return
        val fi = KlondikeController.foundationIndexFor(card, gameState.foundations) ?: return
        commitMove(KlondikeMove.WasteToFoundation(fi))
    }

    val passesDisplay = if (config.deckPasses == KlondikeConfig.INFINITE_PASSES)
        "∞" else "${gameState.passesUsed}/${config.deckPasses}"
    val timeDisplay = "%d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)

    GameScaffold(
        title = "Solitaire",
        onExit = onExit,
        hud = {
            GameHud(
                left = "Moves: ${gameState.moves}",
                center = timeDisplay,
                right = "Passes: $passesDisplay"
            )
        },
        status = {
            when (phase) {
                KlondikePhase.WON -> {
                    VictoryPanel(
                        score = gameState.moves,
                        bestScore = gameState.moves,
                        stars = 3,
                        onReplay = {
                            gameState = KlondikeController.deal(System.currentTimeMillis(), config)
                            selection = null
                            undoStack.clear()
                            elapsedSeconds = 0
                            hintMove = null
                            machine.newDeal()
                            machine.startDeal()
                            machine.dealComplete()
                        },
                        onMenu = onExit,
                        headline = "You Win!",
                        primaryLabel = "New Deal"
                    )
                }
                KlondikePhase.LOST -> {
                    DefeatPanel(
                        score = gameState.moves,
                        bestScore = gameState.moves,
                        onTryAgain = {
                            gameState = KlondikeController.deal(System.currentTimeMillis(), config)
                            selection = null
                            undoStack.clear()
                            elapsedSeconds = 0
                            hintMove = null
                            machine.newDeal()
                            machine.startDeal()
                            machine.dealComplete()
                        },
                        onMenu = onExit,
                        headline = "No Moves Left"
                    )
                }
                else -> {}
            }
        }
    ) {
        // Extra action buttons in the board area (undo, hint, new deal).
        Column(modifier = Modifier.fillMaxSize().background(CardTableFelt)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = {
                        val prev = undoStack.removeLastOrNull() ?: return@IconButton
                        gameState = prev
                        selection = null
                        hintMove = null
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Undo", tint = CardHighlight)
                }
                IconButton(onClick = { hintMove = KlondikeController.hint(gameState) }) {
                    Icon(Icons.Default.Star, contentDescription = "Hint", tint = CardHighlight)
                }
                IconButton(
                    onClick = {
                        gameState = KlondikeController.deal(System.currentTimeMillis(), config)
                        selection = null
                        undoStack.clear()
                        elapsedSeconds = 0
                        hintMove = null
                        machine.newDeal()
                        machine.startDeal()
                        machine.dealComplete()
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "New Deal", tint = CardHighlight)
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cardWidth: Dp = (maxWidth - 16.dp) / 7

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    // ── Top row: Stock | Waste | Spacer | Foundations ──────────────────────
                    TopRow(
                        gameState = gameState,
                        cardWidth = cardWidth,
                        selection = selection,
                        hintMove = hintMove,
                        onStockClick = { onStockClick() },
                        onWasteClick = { onWasteClick() },
                        onWasteDoubleTap = { onDoubleTapWaste() },
                        onFoundationClick = { fi -> onFoundationClick(fi) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Tableau ────────────────────────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        TableauRow(
                            gameState = gameState,
                            cardWidth = cardWidth,
                            selection = selection,
                            hintMove = hintMove,
                            onCardClick = { col, idx -> onTableauCardClick(col, idx) },
                            onCardDoubleTap = { col, idx -> onDoubleTapCard(col, idx) },
                            onEmptyClick = { col -> onTableauEmptyClick(col) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopRow(
    gameState: KlondikeState,
    cardWidth: Dp,
    selection: Selection?,
    hintMove: KlondikeMove?,
    onStockClick: () -> Unit,
    onWasteClick: () -> Unit,
    onWasteDoubleTap: () -> Unit,
    onFoundationClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Stock
        if (gameState.stock.isNotEmpty()) {
            CardBackView(
                modifier = Modifier.width(cardWidth),
                onClick = onStockClick
            )
        } else {
            // Empty stock shows a recycle indicator.
            EmptyCardSlot(
                modifier = Modifier.width(cardWidth),
                label = "↺",
                highlighted = gameState.waste.isNotEmpty(),
                onClick = onStockClick
            )
        }

        // Waste — show up to top 3 cards fanned, only the topmost is tappable.
        val wasteTop = gameState.waste.lastOrNull()
        val wasteSelected = selection?.source == MoveSource.FromWaste
        val wasteHinted = hintMove.let {
            it is KlondikeMove.WasteToFoundation || it is KlondikeMove.WasteToTableau
        }
        if (wasteTop != null) {
            Box(modifier = Modifier.width(cardWidth)) {
                // Show up to 2 face-up cards beneath the top for visual context (draw-3 mode).
                val visible = gameState.waste.takeLast(3)
                visible.forEachIndexed { idx, card ->
                    val isTop = idx == visible.size - 1
                    val xOff = (idx * 4).dp
                    PlayingCardView(
                        card = card,
                        modifier = Modifier
                            .width(cardWidth)
                            .padding(start = xOff),
                        selected = isTop && (wasteSelected || wasteHinted),
                        onClick = if (isTop) ({ onWasteClick() }) else null
                    )
                }
            }
        } else {
            EmptyCardSlot(modifier = Modifier.width(cardWidth), label = "W")
        }

        // Spacer
        Spacer(modifier = Modifier.width(cardWidth))

        // Four foundations (Clubs, Diamonds, Hearts, Spades).
        Suit.entries.forEachIndexed { fi, suit ->
            val pile = gameState.foundations[fi]
            val foundHinted = hintMove.let {
                (it is KlondikeMove.WasteToFoundation && it.foundIdx == fi) ||
                (it is KlondikeMove.TableauToFoundation && it.foundIdx == fi)
            }
            val selHighlight = selection != null // any selection makes foundations glow
            if (pile.isEmpty()) {
                EmptyCardSlot(
                    modifier = Modifier.width(cardWidth),
                    label = suit.symbol,
                    highlighted = selHighlight || foundHinted,
                    onClick = { onFoundationClick(fi) }
                )
            } else {
                PlayingCardView(
                    card = pile.last(),
                    modifier = Modifier.width(cardWidth),
                    selected = foundHinted,
                    onClick = { onFoundationClick(fi) }
                )
            }
        }
    }
}

@Composable
private fun TableauRow(
    gameState: KlondikeState,
    cardWidth: Dp,
    selection: Selection?,
    hintMove: KlondikeMove?,
    onCardClick: (col: Int, cardIdx: Int) -> Unit,
    onCardDoubleTap: (col: Int, cardIdx: Int) -> Unit,
    onEmptyClick: (col: Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (colIdx in 0..6) {
            val col = gameState.tableau[colIdx]
            val cardHeight: Dp = cardWidth / CardAspectRatio
            val faceDownOffset: Dp = cardHeight * 0.22f
            val faceUpOffset: Dp = cardHeight * 0.38f

            // Determine hint/selection highlights for this column.
            val hintFrom = hintMove.let {
                when (it) {
                    is KlondikeMove.TableauToTableau -> if (it.fromCol == colIdx) it.startIndex else null
                    is KlondikeMove.TableauToFoundation -> if (it.fromCol == colIdx) col.size - 1 else null
                    else -> null
                }
            }
            val hintDest = hintMove.let {
                when (it) {
                    is KlondikeMove.TableauToTableau -> it.toCol == colIdx
                    is KlondikeMove.WasteToTableau -> (it as KlondikeMove.WasteToTableau).col == colIdx
                    else -> false
                }
            }
            val selFrom = selection?.let {
                if (it.source is MoveSource.FromTableau && (it.source as MoveSource.FromTableau).col == colIdx)
                    it.startIndex else null
            }
            val destHighlight = selection != null || hintDest

            if (col.isEmpty()) {
                EmptyCardSlot(
                    modifier = Modifier.width(cardWidth),
                    label = "K",
                    highlighted = destHighlight,
                    onClick = { onEmptyClick(colIdx) }
                )
            } else {
                // Compute total column height.
                var totalHeight = cardHeight
                for (i in 0 until col.size - 1) {
                    totalHeight += if (col[i].faceUp) faceUpOffset else faceDownOffset
                }

                Box(modifier = Modifier.width(cardWidth).height(totalHeight)) {
                    var yOffset = 0.dp
                    col.forEachIndexed { cardIdx, card ->
                        val isSelectedCard = selFrom != null && cardIdx >= selFrom
                        val isHintedCard = hintFrom != null && cardIdx >= hintFrom
                        val highlighted = isSelectedCard || isHintedCard

                        if (card.faceUp) {
                            PlayingCardView(
                                card = card,
                                modifier = Modifier
                                    .width(cardWidth)
                                    .padding(top = yOffset),
                                selected = highlighted,
                                onClick = { onCardClick(colIdx, cardIdx) }
                            )
                        } else {
                            CardBackView(
                                modifier = Modifier
                                    .width(cardWidth)
                                    .padding(top = yOffset),
                                onClick = null
                            )
                        }

                        yOffset += if (card.faceUp) faceUpOffset else faceDownOffset
                    }
                }
            }
        }
    }
}
