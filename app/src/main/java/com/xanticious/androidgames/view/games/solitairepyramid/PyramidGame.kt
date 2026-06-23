package com.xanticious.androidgames.view.games.solitairepyramid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.BoxWithConstraints
import com.xanticious.androidgames.controller.games.solitairepyramid.PyramidRules
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.solitairepyramid.CardLocation
import com.xanticious.androidgames.model.games.solitairepyramid.PyramidConfig
import com.xanticious.androidgames.model.games.solitairepyramid.PyramidGameState
import com.xanticious.androidgames.state.games.solitairepyramid.PyramidPhase
import com.xanticious.androidgames.state.games.solitairepyramid.PyramidStateMachine
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.cards.CardAspectRatio
import com.xanticious.androidgames.view.common.cards.CardBackView
import com.xanticious.androidgames.view.common.cards.EmptyCardSlot
import com.xanticious.androidgames.view.common.cards.PlayingCardView

/**
 * Pyramid Solitaire — match exposed pairs that sum to 13; clear the pyramid to win.
 *
 * Entry point wired by [com.xanticious.androidgames.view.GameRegistry].
 * All game rules live in [PyramidRules]; this composable is thin presentation only.
 */
@Composable
fun PyramidGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val config  = remember(difficulty) { PyramidConfig.forDifficulty(difficulty) }
    val machine = remember { PyramidStateMachine() }
    val phase   by machine.phase.collectAsState()

    var gameState by remember { mutableStateOf(PyramidRules.deal(System.currentTimeMillis())) }

    LaunchedEffect(Unit) {
        machine.startDeal()
        gameState = PyramidRules.deal(System.currentTimeMillis())
        machine.dealt()
    }

    fun startNewDeal() {
        machine.newDeal()
        gameState = PyramidRules.deal(System.currentTimeMillis())
        machine.dealt()
    }

    fun handleCardTap(location: CardLocation) {
        if (phase != PyramidPhase.PLAYING) return
        val updated = PyramidRules.selectCard(gameState, location)
        gameState = updated
        if (PyramidRules.isWon(updated)) {
            machine.pyramidCleared()
        } else if (!PyramidRules.hasLegalMoves(updated, config)) {
            machine.noMovesLeft()
        }
    }

    fun handleStockTap() {
        if (phase != PyramidPhase.PLAYING) return
        val updated = PyramidRules.drawFromStock(gameState, config) ?: return
        gameState = updated
        if (!PyramidRules.hasLegalMoves(updated, config)) {
            machine.noMovesLeft()
        }
    }

    fun handleUndo() {
        if (phase != PyramidPhase.PLAYING) return
        val reverted = PyramidRules.undo(gameState) ?: return
        gameState = reverted
    }

    val redealsLeft = when {
        config.maxRedeals < 0 -> "∞"
        else                  -> "${(config.maxRedeals - gameState.stockCycles).coerceAtLeast(0)}"
    }

    GameScaffold(
        title = "Solitaire (Pyramid)",
        onExit = onExit,
        hud = {
            GameHud(
                left   = "Stock: ${gameState.stock.size}",
                center = "Score: ${gameState.score}",
                right  = "Redeals: $redealsLeft"
            )
        },
        status = {
            when (phase) {
                PyramidPhase.WON -> {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)) {
                        VictoryPanel(
                            score      = gameState.score,
                            bestScore  = gameState.score,
                            stars      = 3,
                            onReplay   = { startNewDeal() },
                            onMenu     = onExit,
                            headline   = "Pyramid Cleared!"
                        )
                    }
                }
                PyramidPhase.LOST -> {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)) {
                        DefeatPanel(
                            score     = gameState.score,
                            bestScore = gameState.score,
                            onTryAgain = { startNewDeal() },
                            onMenu     = onExit,
                            headline   = "No More Moves"
                        )
                    }
                }
                else -> {}
            }
        }
    ) {
        PyramidBoardView(
            gameState    = gameState,
            onCardTapped = ::handleCardTap,
            onStockTapped = ::handleStockTap,
            onUndo       = ::handleUndo
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Board composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PyramidBoardView(
    gameState: PyramidGameState,
    onCardTapped: (CardLocation) -> Unit,
    onStockTapped: () -> Unit,
    onUndo: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        // Size cards so 7 fit across the bottom row with small margins.
        val cardW   = maxWidth / 8f         // Dp
        val cardH   = cardW / CardAspectRatio
        val rowStep = cardH * 0.60f         // vertical offset per row (40 % overlap)
        val pyramidH = rowStep * 6f + cardH // total height of the 7-row pyramid

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Pyramid ──────────────────────────────────────────────────────
            // A single Box lets us stack rows and maintain natural z-order:
            // later rows (larger index = closer to base) are drawn on top,
            // which matches the physical overlap and lets click events fall
            // through unexposed (non-clickable) cards to exposed ones below.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pyramidH)
            ) {
                for (row in 0 until PyramidGameState.ROWS) {
                    val cols      = row + 1
                    val rowTotalW = cardW * cols.toFloat()
                    val rowStartX = (maxWidth - rowTotalW) / 2f
                    val yOffset   = rowStep * row.toFloat()

                    for (col in 0 until cols) {
                        val idx      = PyramidRules.indexAt(row, col)
                        val card     = gameState.pyramid[idx]
                        val exposed  = PyramidRules.isExposed(gameState.pyramid, idx)
                        val selected = gameState.selected == CardLocation.Pyramid(idx)
                        val xOffset  = rowStartX + cardW * col.toFloat()

                        // Only render if the card is still in the pyramid.
                        if (card != null) {
                            Box(
                                modifier = Modifier
                                    .offset(x = xOffset, y = yOffset)
                                    .width(cardW - 1.dp)
                            ) {
                                PlayingCardView(
                                    card     = card,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            alpha = if (exposed) 1f else 0.50f
                                        },
                                    selected = selected,
                                    onClick  = if (exposed) {
                                        { onCardTapped(CardLocation.Pyramid(idx)) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            // ── Stock / Waste / Undo bar ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val slotW = cardW * 1.1f

                // Stock pile — shown as card back; tapping draws or recycles.
                if (gameState.stock.isNotEmpty()) {
                    CardBackView(
                        modifier = Modifier.width(slotW),
                        onClick  = onStockTapped
                    )
                } else {
                    EmptyCardSlot(
                        modifier    = Modifier.width(slotW),
                        label       = "↺",
                        highlighted = gameState.waste.isNotEmpty(),
                        onClick     = onStockTapped
                    )
                }

                // Waste top
                val wasteCard = gameState.waste.lastOrNull()
                if (wasteCard != null) {
                    PlayingCardView(
                        card     = wasteCard,
                        modifier = Modifier.width(slotW),
                        selected = gameState.selected == CardLocation.Waste,
                        onClick  = { onCardTapped(CardLocation.Waste) }
                    )
                } else {
                    EmptyCardSlot(
                        modifier = Modifier.width(slotW),
                        label    = "W"
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Undo button
                if (gameState.history.isNotEmpty()) {
                    androidx.compose.material3.TextButton(onClick = onUndo) {
                        Text("↶ Undo", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
