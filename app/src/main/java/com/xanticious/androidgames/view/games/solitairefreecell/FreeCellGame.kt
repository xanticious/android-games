package com.xanticious.androidgames.view.games.solitairefreecell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.solitairefreecell.FreeCellController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.cards.Suit
import com.xanticious.androidgames.model.games.solitairefreecell.CardLocation
import com.xanticious.androidgames.model.games.solitairefreecell.FreeCellState
import com.xanticious.androidgames.state.games.solitairefreecell.FreeCellPhase
import com.xanticious.androidgames.state.games.solitairefreecell.FreeCellStateMachine
import com.xanticious.androidgames.ui.theme.CardHighlight
import com.xanticious.androidgames.ui.theme.CardSlot
import com.xanticious.androidgames.ui.theme.CardTableFelt
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.cards.CardAspectRatio
import com.xanticious.androidgames.view.common.cards.EmptyCardSlot
import com.xanticious.androidgames.view.common.cards.PlayingCardView
import kotlinx.coroutines.delay

/**
 * FreeCell solitaire top-level composable.
 *
 * [difficulty] maps to free-cell count: EASY=4 cells (standard), MEDIUM=3, HARD=2.
 * Layout: free cells (top-left) + foundations (top-right), then 8 tableau columns.
 * Tap a card to select; tap a destination to move. Undo in the top bar.
 */
@Composable
fun FreeCellGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val freeCellCount = remember(difficulty) {
        when (difficulty) {
            GameDifficulty.EASY -> 4
            GameDifficulty.MEDIUM -> 3
            GameDifficulty.HARD -> 2
        }
    }
    val seed = remember { System.currentTimeMillis() }
    val machine = remember { FreeCellStateMachine() }
    val phase by machine.phase.collectAsState()

    var gameState by remember { mutableStateOf(FreeCellController.deal(seed, freeCellCount)) }
    var history by remember { mutableStateOf(listOf<FreeCellState>()) }
    var selected by remember { mutableStateOf<CardLocation?>(null) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        machine.startDeal()
        machine.dealComplete()
    }

    // Timer — only ticks while playing.
    LaunchedEffect(phase) {
        if (phase == FreeCellPhase.PLAYING) {
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    fun applyAndAdvance(newState: FreeCellState, autoMove: Boolean = true) {
        val afterAuto = if (autoMove) FreeCellController.autoMoveToFoundations(newState) else newState
        gameState = afterAuto
        selected = null
        when {
            FreeCellController.isWon(afterAuto) -> {
                machine.foundationsComplete()
                machine.cascadeComplete()
            }
            !FreeCellController.hasLegalMoves(afterAuto) -> machine.noMovesLeft()
            else -> machine.moveMade()
        }
    }

    fun tryMove(target: CardLocation?) {
        val src = selected
        if (src == null || target == null) {
            selected = target
            return
        }
        if (src == target) {
            selected = null
            return
        }

        val moved: FreeCellState? = when {
            // Free cell → tableau
            src is CardLocation.InFreeCell && target is CardLocation.InTableau -> {
                val toCol = target.column
                FreeCellController.moveFromFreeCellToTableau(gameState, src.index, toCol)
            }
            // Free cell → foundation (tap on any foundation slot while free cell selected)
            src is CardLocation.InFreeCell && target is CardLocation.InTableau -> null
            // Tableau → free cell
            src is CardLocation.InTableau && target is CardLocation.InFreeCell -> {
                // Only top card can go to free cell
                if (src.row == gameState.tableau[src.column].lastIndex) {
                    FreeCellController.moveToFreeCell(gameState, src.column, target.index)
                } else null
            }
            // Tableau → tableau
            src is CardLocation.InTableau && target is CardLocation.InTableau -> {
                FreeCellController.moveTableauToTableau(gameState, src.column, src.row, target.column)
            }
            else -> null
        }

        if (moved != null) {
            history = history + gameState
            applyAndAdvance(moved)
        } else {
            // Move failed — try to re-select the tapped location instead.
            selected = target
        }
    }

    fun onTableauTap(col: Int, row: Int) {
        val loc = CardLocation.InTableau(col, row)
        val src = selected
        if (src == null) {
            selected = loc
            return
        }
        if (src == loc) { selected = null; return }

        // If src is a free cell, try moving to this tableau column.
        if (src is CardLocation.InFreeCell) {
            val moved = FreeCellController.moveFromFreeCellToTableau(gameState, src.index, col)
            if (moved != null) {
                history = history + gameState
                applyAndAdvance(moved)
                return
            }
        }

        // If src is a tableau card, try moving the sequence starting at src.row to this column.
        if (src is CardLocation.InTableau) {
            val moved = FreeCellController.moveTableauToTableau(gameState, src.column, src.row, col)
            if (moved != null) {
                history = history + gameState
                applyAndAdvance(moved)
                return
            }
        }

        // Move failed — re-select the tapped card.
        selected = loc
    }

    fun onFreeCellTap(cellIdx: Int) {
        val loc = CardLocation.InFreeCell(cellIdx)
        val src = selected
        if (src == null) {
            if (gameState.freeCells[cellIdx] != null) selected = loc
            return
        }
        if (src == loc) { selected = null; return }

        // Tableau card → free cell.
        if (src is CardLocation.InTableau) {
            val col = src.column
            val topRow = gameState.tableau[col].lastIndex
            if (src.row == topRow) {
                val moved = FreeCellController.moveToFreeCell(gameState, col, cellIdx)
                if (moved != null) {
                    history = history + gameState
                    applyAndAdvance(moved)
                    return
                }
            }
        }

        // Re-select the free cell if it has a card.
        selected = if (gameState.freeCells[cellIdx] != null) loc else null
    }

    fun onFoundationTap(suit: Suit) {
        val src = selected ?: return
        val suitIdx = suit.ordinal
        val moved: FreeCellState? = when (src) {
            is CardLocation.InTableau -> {
                val col = src.column
                val column = gameState.tableau[col]
                if (column.isNotEmpty() && src.row == column.lastIndex) {
                    FreeCellController.moveToFoundationFromTableau(gameState, col)
                } else null
            }
            is CardLocation.InFreeCell -> {
                val card = gameState.freeCells[src.index]
                if (card != null && card.suit.ordinal == suitIdx) {
                    FreeCellController.moveFromFreeCellToFoundation(gameState, src.index)
                } else null
            }
        }
        if (moved != null) {
            history = history + gameState
            applyAndAdvance(moved)
        } else {
            selected = null
        }
    }

    fun onUndo() {
        if (history.isEmpty()) return
        gameState = history.last()
        history = history.dropLast(1)
        selected = null
        machine.undoRequested()
    }

    fun onNewDeal() {
        val newSeed = System.currentTimeMillis()
        gameState = FreeCellController.deal(newSeed, freeCellCount)
        history = emptyList()
        selected = null
        elapsedSeconds = 0
        machine.newDeal()
        machine.startDeal()
        machine.dealComplete()
    }

    val timeStr = remember(elapsedSeconds) {
        val m = elapsedSeconds / 60
        val s = elapsedSeconds % 60
        "%d:%02d".format(m, s)
    }
    val freeCellsUsed = gameState.freeCells.count { it != null }

    GameScaffold(
        title = "Solitaire (FreeCell)",
        onExit = onExit,
        hud = {
            GameHud(
                left = "⏱ $timeStr",
                center = "Moves ${gameState.moveCount}",
                right = "Free ${freeCellCount - freeCellsUsed}/$freeCellCount"
            )
        },
        status = {
            when (phase) {
                FreeCellPhase.WON -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    VictoryPanel(
                        score = gameState.moveCount,
                        bestScore = gameState.moveCount,
                        stars = 3,
                        onReplay = { onNewDeal() },
                        onMenu = onExit,
                        headline = "You Won!",
                        primaryLabel = "New Deal"
                    )
                }
                FreeCellPhase.LOST -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    DefeatPanel(
                        score = gameState.moveCount,
                        bestScore = gameState.moveCount,
                        onTryAgain = { onNewDeal() },
                        onMenu = onExit,
                        headline = "No More Moves"
                    )
                }
                else -> {}
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CardTableFelt)
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            // Top row: free cells (left) + foundations (right).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Free cells.
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(freeCellCount) { i ->
                        val card = gameState.freeCells[i]
                        val isSelected = selected == CardLocation.InFreeCell(i)
                        Box(modifier = Modifier.weight(1f)) {
                            if (card != null) {
                                PlayingCardView(
                                    card = card,
                                    modifier = Modifier.fillMaxWidth(),
                                    selected = isSelected,
                                    onClick = { onFreeCellTap(i) }
                                )
                            } else {
                                val isHighlighted = selected is CardLocation.InTableau ||
                                    selected is CardLocation.InFreeCell
                                EmptyCardSlot(
                                    modifier = Modifier.fillMaxWidth(),
                                    label = "FC",
                                    highlighted = isHighlighted,
                                    onClick = { onFreeCellTap(i) }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Foundations.
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Suit.entries.forEach { suit ->
                        val pile = gameState.foundations[suit.ordinal]
                        val topCard = pile.lastOrNull()
                        val isHighlighted = selected != null
                        Box(modifier = Modifier.weight(1f)) {
                            if (topCard != null) {
                                PlayingCardView(
                                    card = topCard,
                                    modifier = Modifier.fillMaxWidth(),
                                    selected = false,
                                    onClick = { onFoundationTap(suit) }
                                )
                            } else {
                                EmptyCardSlot(
                                    modifier = Modifier.fillMaxWidth(),
                                    label = suit.symbol,
                                    highlighted = isHighlighted,
                                    onClick = { onFoundationTap(suit) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tableau — 8 columns, scrollable vertically when tall.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val colWidth = (maxWidth - 14.dp) / 8  // 7 gaps × 2dp
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    (0 until 8).forEach { col ->
                        TableauColumn(
                            cards = gameState.tableau[col],
                            selectedStartRow = (selected as? CardLocation.InTableau)
                                ?.takeIf { it.column == col }?.row,
                            isHighlighted = selected != null && gameState.tableau[col]
                                .let { tCol ->
                                    val s = selected
                                    tCol.isEmpty() ||
                                        (s != null && s != CardLocation.InTableau(col, tCol.lastIndex) &&
                                            canBeTarget(gameState, s, col))
                                },
                            columnWidth = colWidth,
                            onCardTap = { row -> onTableauTap(col, row) },
                            onEmptyTap = { onTableauTap(col, 0) },
                            modifier = Modifier.width(colWidth)
                        )
                    }
                }
            }

            // Undo row.
            if (phase == FreeCellPhase.PLAYING && history.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "↶ Undo",
                        color = CardHighlight,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clickable { onUndo() }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

/** True if the current selection can legally move to the given tableau column. */
private fun canBeTarget(state: FreeCellState, src: CardLocation, toCol: Int): Boolean =
    when (src) {
        is CardLocation.InFreeCell ->
            FreeCellController.canMoveFromFreeCellToTableau(state, src.index, toCol)
        is CardLocation.InTableau ->
            src.column != toCol &&
                FreeCellController.canMoveTableauToTableau(state, src.column, src.row, toCol)
    }

/** A single tableau column with overlapping (peek-style) card rendering. */
@Composable
private fun TableauColumn(
    cards: List<com.xanticious.androidgames.model.games.cards.Card>,
    selectedStartRow: Int?,
    isHighlighted: Boolean,
    columnWidth: Dp,
    onCardTap: (row: Int) -> Unit,
    onEmptyTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val peekDp = 26.dp
    val cardHeight = columnWidth / CardAspectRatio

    Box(modifier = modifier) {
        if (cards.isEmpty()) {
            EmptyCardSlot(
                modifier = Modifier.size(columnWidth, cardHeight),
                highlighted = isHighlighted,
                onClick = onEmptyTap
            )
        } else {
            val totalHeight = if (cards.size == 1) cardHeight
                              else peekDp * (cards.size - 1) + cardHeight
            Box(modifier = Modifier.size(columnWidth, totalHeight)) {
                cards.forEachIndexed { row, card ->
                    val isSelected = selectedStartRow != null && row >= selectedStartRow
                    PlayingCardView(
                        card = card,
                        modifier = Modifier
                            .width(columnWidth)
                            .absoluteOffset(y = peekDp * row),
                        selected = isSelected,
                        onClick = { onCardTap(row) }
                    )
                }
            }
        }
    }
}
