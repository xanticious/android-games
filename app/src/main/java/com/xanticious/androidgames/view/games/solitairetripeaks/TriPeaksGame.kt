package com.xanticious.androidgames.view.games.solitairetripeaks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.solitairetripeaks.TriPeaksController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.solitairetripeaks.TriPeaksBoard
import com.xanticious.androidgames.model.games.solitairetripeaks.TriPeaksVariant
import com.xanticious.androidgames.state.games.solitairetripeaks.TriPeaksPhase
import com.xanticious.androidgames.state.games.solitairetripeaks.TriPeaksStateMachine
import com.xanticious.androidgames.ui.theme.CardTableFelt
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlin.random.Random

/**
 * CLASSIC TriPeaks Solitaire — no timer, relaxed play.
 *
 * Shared rendering lives in [TriPeaksBoardContent]; this composable owns only
 * the state-machine wiring, the HUD, and the status strip.
 */
@Composable
fun TriPeaksGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { TriPeaksController() }
    val config = remember(difficulty) { controller.configFor(TriPeaksVariant.CLASSIC, difficulty) }
    val machine = remember { TriPeaksStateMachine() }
    val phase by machine.phase.collectAsState()

    var board by remember { mutableStateOf<TriPeaksBoard?>(null) }
    var seed by remember { mutableLongStateOf(Random.nextLong()) }

    // Deal on first launch and whenever seed changes.
    LaunchedEffect(seed) {
        machine.startDeal()
        board = controller.deal(seed, config)
        machine.dealt()
    }

    val currentBoard = board ?: return

    GameScaffold(
        title = "Solitaire (TriPeaks)",
        onExit = onExit,
        hud = {
            GameHud(
                left = "Stock: ${currentBoard.stock.size}",
                center = "Score: ${currentBoard.score}",
                right = if (currentBoard.combo > 0) "Chain ×${currentBoard.combo}" else "",
            )
        },
        status = {
            when (phase) {
                TriPeaksPhase.WON -> VictoryPanel(
                    score = currentBoard.score + controller.winBonus(currentBoard),
                    bestScore = currentBoard.score + controller.winBonus(currentBoard),
                    stars = when {
                        currentBoard.combo >= 10 -> 3
                        currentBoard.combo >= 5 -> 2
                        else -> 1
                    },
                    onReplay = {
                        seed = Random.nextLong()
                    },
                    onMenu = onExit,
                )
                TriPeaksPhase.LOST -> DefeatPanel(
                    score = currentBoard.score,
                    bestScore = currentBoard.score,
                    onTryAgain = {
                        seed = Random.nextLong()
                    },
                    onMenu = onExit,
                )
                else -> {}
            }
        },
    ) {
        TriPeaksBoardContent(
            board = currentBoard,
            playing = phase == TriPeaksPhase.PLAYING,
            onCardTap = { position ->
                if (phase == TriPeaksPhase.PLAYING && controller.canPlay(currentBoard, position)) {
                    board = controller.playCard(currentBoard, position)
                    machine.cardPlayed()
                    val b = board ?: return@TriPeaksBoardContent
                    when {
                        controller.isWon(b) -> machine.allPeaksCleared()
                        controller.isLost(b) -> machine.noMovesAndNoStock()
                        else -> {}
                    }
                }
            },
            onStockTap = {
                if (phase == TriPeaksPhase.PLAYING && currentBoard.stock.isNotEmpty()) {
                    board = controller.draw(currentBoard)
                    machine.stockDrawn()
                    val b = board ?: return@TriPeaksBoardContent
                    if (controller.isLost(b)) machine.noMovesAndNoStock()
                }
            },
        )
    }
}

/**
 * Shared board rendering for both TriPeaks variants.
 *
 * Draws the three overlapping peaks (28 cards in four rows), stock pile, and
 * waste pile. All game logic stays in the controller; this composable only
 * renders state and forwards taps.
 */
@Composable
fun TriPeaksBoardContent(
    board: TriPeaksBoard,
    playing: Boolean,
    onCardTap: (position: Int) -> Unit,
    onStockTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = remember { TriPeaksController() }
    val hintPos = if (playing) controller.hintPosition(board) else null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CardTableFelt)
            .padding(8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ---- Peaks (rows 0–2) ----
            PeakRows(board = board, playing = playing, hintPos = hintPos, onCardTap = onCardTap)

            Spacer(modifier = Modifier.height(8.dp))

            // ---- Control row: stock + waste ----
            StockWasteRow(
                board = board,
                playing = playing,
                onStockTap = onStockTap,
            )
        }
    }
}

/** Renders rows 0–2 (the three peaks) as three stacked rows. */
@Composable
private fun PeakRows(
    board: TriPeaksBoard,
    playing: Boolean,
    hintPos: Int?,
    onCardTap: (Int) -> Unit,
) {
    val controller = remember { TriPeaksController() }

    // Row 0: tips at positions 0, 1, 2 — gap between each peak
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.width(20.dp))
        BoardCardSlot(board, 0, playing, hintPos, controller, onCardTap)
        Spacer(modifier = Modifier.width(40.dp))
        BoardCardSlot(board, 1, playing, hintPos, controller, onCardTap)
        Spacer(modifier = Modifier.width(40.dp))
        BoardCardSlot(board, 2, playing, hintPos, controller, onCardTap)
        Spacer(modifier = Modifier.width(20.dp))
    }

    // Row 1: positions 3–8 (two below each tip)
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.width(8.dp))
        BoardCardSlot(board, 3, playing, hintPos, controller, onCardTap)
        BoardCardSlot(board, 4, playing, hintPos, controller, onCardTap)
        Spacer(modifier = Modifier.width(24.dp))
        BoardCardSlot(board, 5, playing, hintPos, controller, onCardTap)
        BoardCardSlot(board, 6, playing, hintPos, controller, onCardTap)
        Spacer(modifier = Modifier.width(24.dp))
        BoardCardSlot(board, 7, playing, hintPos, controller, onCardTap)
        BoardCardSlot(board, 8, playing, hintPos, controller, onCardTap)
        Spacer(modifier = Modifier.width(8.dp))
    }

    // Row 2: positions 9–17 (three below each row-1 pair)
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        for (pos in 9..17) {
            BoardCardSlot(board, pos, playing, hintPos, controller, onCardTap)
        }
    }

    // Row 3: base row positions 18–27 (always exposed)
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        for (pos in 18..27) {
            BoardCardSlot(board, pos, playing, hintPos, controller, onCardTap)
        }
    }
}

/** Stock + waste row. */
@Composable
private fun StockWasteRow(
    board: TriPeaksBoard,
    playing: Boolean,
    onStockTap: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        // Stock pile
        if (board.stock.isNotEmpty()) {
            com.xanticious.androidgames.view.common.cards.CardBackView(
                modifier = Modifier.width(52.dp),
                onClick = if (playing) onStockTap else null,
            )
        } else {
            com.xanticious.androidgames.view.common.cards.EmptyCardSlot(
                label = "×",
                highlighted = false,
                onClick = null,
                modifier = Modifier.width(52.dp),
            )
        }

        Text(
            text = "→",
            style = MaterialTheme.typography.titleLarge,
            color = androidx.compose.ui.graphics.Color.White,
        )

        // Waste pile top card
        val wasteTop = board.wasteTop
        if (wasteTop != null) {
            com.xanticious.androidgames.view.common.cards.PlayingCardView(
                card = wasteTop,
                modifier = Modifier.width(52.dp),
                selected = true,
            )
        } else {
            com.xanticious.androidgames.view.common.cards.EmptyCardSlot(
                label = "W",
                highlighted = false,
                onClick = null,
                modifier = Modifier.width(52.dp),
            )
        }
    }
}

/** Renders one board position: invisible gap if removed, card otherwise. */
@Composable
private fun BoardCardSlot(
    board: TriPeaksBoard,
    position: Int,
    playing: Boolean,
    hintPos: Int?,
    controller: TriPeaksController,
    onCardTap: (Int) -> Unit,
) {
    val bc = board.boardCards[position]
    if (bc.removed) {
        // Keep visual spacing with a transparent placeholder.
        Spacer(modifier = Modifier.width(40.dp).height(56.dp))
        return
    }

    val exposed = controller.isExposed(board, position)
    val canPlay = playing && controller.canPlay(board, position)
    val isHint = position == hintPos

    com.xanticious.androidgames.view.common.cards.PlayingCardView(
        card = bc.card,
        modifier = Modifier
            .width(40.dp)
            .then(if (!exposed) Modifier.then(Modifier) else Modifier),
        selected = isHint || canPlay,
        onClick = if (canPlay) ({ onCardTap(position) }) else null,
    )
}
