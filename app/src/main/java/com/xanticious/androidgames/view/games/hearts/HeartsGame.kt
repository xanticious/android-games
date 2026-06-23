package com.xanticious.androidgames.view.games.hearts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.hearts.HeartsController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.hearts.HeartsConfig
import com.xanticious.androidgames.model.games.hearts.HeartsGameState
import com.xanticious.androidgames.model.games.hearts.PassDirection
import com.xanticious.androidgames.model.games.hearts.Seat
import com.xanticious.androidgames.model.games.hearts.TrickCard
import com.xanticious.androidgames.model.games.hearts.displayName
import com.xanticious.androidgames.model.games.hearts.next
import com.xanticious.androidgames.state.games.hearts.HeartsPhase
import com.xanticious.androidgames.state.games.hearts.HeartsStateMachine
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.ui.theme.CardHighlight
import com.xanticious.androidgames.ui.theme.CardTableFelt
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.cards.CardBackView
import com.xanticious.androidgames.view.common.cards.CardAspectRatio
import com.xanticious.androidgames.view.common.cards.EmptyCardSlot
import com.xanticious.androidgames.view.common.cards.PlayingCardView
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Hearts — trick-avoidance card game (design/card-games/hearts).
 * One human (South) vs three AI opponents. Tap cards to pass or play; lowest
 * score wins when someone reaches the game-end threshold.
 */
@Composable
fun HeartsGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val config  = remember { HeartsConfig() }
    val machine = remember { HeartsStateMachine() }
    val phase   by machine.phase.collectAsState()

    var gameState by remember { mutableStateOf(HeartsGameState.empty(config)) }
    var seed      by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // ── Initial deal ─────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        machine.startGame()
        gameState = HeartsController.deal(gameState, seed)
        if (gameState.passDirection == PassDirection.HOLD) {
            gameState = HeartsController.prepareForPlay(gameState)
            machine.dealtHold()
        } else {
            machine.dealtPass()
        }
    }

    // ── Phase-driven automation (trick resolution, hand scoring, re-deal) ────
    LaunchedEffect(phase) {
        when (phase) {
            HeartsPhase.TRICK_END -> {
                delay(1_200L)  // show the completed trick briefly
                gameState = HeartsController.resolveTrick(gameState)
                if (HeartsController.isHandComplete(gameState)) {
                    machine.handComplete()
                } else {
                    machine.trickResolved()
                }
            }
            HeartsPhase.SCORING -> {
                delay(2_500L)  // player reads the hand scores
                gameState = HeartsController.scoreHand(gameState)
                if (gameState.gameOver) {
                    machine.gameEnded()
                } else {
                    seed = System.currentTimeMillis()
                    gameState = HeartsController.deal(gameState, seed)
                    if (gameState.passDirection == PassDirection.HOLD) {
                        gameState = HeartsController.prepareForPlay(gameState)
                        machine.nextHandHold()
                    } else {
                        machine.nextHandPass()
                    }
                }
            }
            HeartsPhase.DEALING -> {
                // Dealt → pass or play
                if (gameState.passDirection == PassDirection.HOLD) {
                    gameState = HeartsController.prepareForPlay(gameState)
                    machine.dealtHold()
                } else {
                    machine.dealtPass()
                }
            }
            else -> {}
        }
    }

    // ── AI card-play: runs whenever trick size changes during PLAYING phase ──
    LaunchedEffect(phase, gameState.currentTrick.size) {
        if (phase != HeartsPhase.PLAYING) return@LaunchedEffect
        if (gameState.currentTrick.size == 4) {
            machine.trickComplete()
            return@LaunchedEffect
        }
        val nextSeat = HeartsController.nextSeatToPlay(gameState.currentTrick, gameState.leadSeat)
        if (nextSeat == Seat.SOUTH) return@LaunchedEffect   // human's turn

        delay(700L)
        val aiCard = HeartsController.aiChooseCard(
            hand         = gameState.hands[nextSeat] ?: emptyList(),
            trick        = gameState.currentTrick,
            heartsBroken = gameState.heartsBroken,
            trickNumber  = gameState.trickNumber,
            tricksTaken  = gameState.tricksTaken,
            difficulty   = difficulty,
            random       = Random.Default,
            seat         = nextSeat
        )
        gameState = HeartsController.playCard(gameState, aiCard, nextSeat)
    }

    // ── Human pass callback ───────────────────────────────────────────────────
    fun onHumanPassConfirm() {
        if (gameState.selectedCards.size != 3) return
        gameState = HeartsController.executePass(gameState, difficulty, Random.Default)
        machine.passConfirmed()
    }

    // ── Human play callback ───────────────────────────────────────────────────
    fun onHumanPlay(card: Card) {
        if (phase != HeartsPhase.PLAYING) return
        val nextSeat = HeartsController.nextSeatToPlay(gameState.currentTrick, gameState.leadSeat)
        if (nextSeat != Seat.SOUTH) return
        gameState = HeartsController.playCard(gameState, card, Seat.SOUTH)
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    val passLabel = when (gameState.passDirection) {
        PassDirection.LEFT   -> "← Pass Left"
        PassDirection.RIGHT  -> "Pass Right →"
        PassDirection.ACROSS -> "↑ Pass Across"
        PassDirection.HOLD   -> "Hold (no pass)"
    }

    GameScaffold(
        title  = "Hearts",
        onExit = onExit,
        hud    = {
            GameHud(
                left   = buildString {
                    append("R${gameState.handNumber}")
                    if (gameState.heartsBroken) append("  ♥ broken")
                },
                center = Seat.entries.joinToString("  ") { seat ->
                    "${seat.displayName()[0]}:${gameState.scores[seat] ?: 0}"
                },
                right  = if (phase == HeartsPhase.PASSING) passLabel else
                    if (gameState.heartsBroken) "♥ broken" else "♥ safe"
            )
        },
        status = {
            HeartsStatusSlot(
                phase     = phase,
                gameState = gameState,
                onRematch = {
                    seed      = System.currentTimeMillis()
                    gameState = HeartsController.deal(
                        HeartsGameState.empty(config), seed
                    )
                    machine.rematch()
                },
                onExit    = onExit
            )
        }
    ) {
        // ── Board ─────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CardTableFelt),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // North opponent
            OpponentRow(
                seat      = Seat.NORTH,
                cardCount = gameState.hands[Seat.NORTH]?.size ?: 0,
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // Middle row: West | Trick | East
            Row(
                modifier            = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OpponentColumn(
                    seat      = Seat.WEST,
                    cardCount = gameState.hands[Seat.WEST]?.size ?: 0,
                    modifier  = Modifier.width(44.dp).fillMaxHeight()
                )

                TrickArea(
                    currentTrick = gameState.currentTrick,
                    leadSeat     = gameState.leadSeat,
                    modifier     = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                OpponentColumn(
                    seat      = Seat.EAST,
                    cardCount = gameState.hands[Seat.EAST]?.size ?: 0,
                    modifier  = Modifier.width(44.dp).fillMaxHeight()
                )
            }

            // Human hand
            HumanHand(
                hand          = gameState.hands[Seat.SOUTH] ?: emptyList(),
                selectedCards = gameState.selectedCards,
                phase         = phase,
                heartsBroken  = gameState.heartsBroken,
                trick         = gameState.currentTrick,
                trickNumber   = gameState.trickNumber,
                leadSeat      = gameState.leadSeat,
                onCardTap     = { card ->
                    when (phase) {
                        HeartsPhase.PASSING -> gameState =
                            HeartsController.togglePassSelection(gameState, card)
                        HeartsPhase.PLAYING -> onHumanPlay(card)
                        else                -> {}
                    }
                },
                onPassConfirm = ::onHumanPassConfirm,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
    }
}

// ── Status slot ───────────────────────────────────────────────────────────────

@Composable
private fun HeartsStatusSlot(
    phase: HeartsPhase,
    gameState: HeartsGameState,
    onRematch: () -> Unit,
    onExit: () -> Unit
) {
    when (phase) {
        HeartsPhase.SCORING -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text       = "Hand ${gameState.handNumber - 1} scores",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Seat.entries.forEach { seat ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(seat.displayName(), fontSize = 12.sp)
                            val pts = gameState.handScores[seat] ?: 0
                            Text(
                                text  = "+$pts",
                                color = if (pts > 0) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text("= ${gameState.scores[seat] ?: 0}", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        HeartsPhase.GAME_OVER -> {
            val humanScore   = gameState.scores[Seat.SOUTH] ?: 0
            val humanWon     = gameState.gameWinner == Seat.SOUTH
            val winnerName   = gameState.gameWinner?.displayName() ?: "Unknown"
            val bestScore    = gameState.scores.values.minOrNull() ?: 0
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (humanWon) {
                    VictoryPanel(
                        score       = humanScore,
                        bestScore   = bestScore,
                        stars       = when {
                            humanScore <= 20 -> 3
                            humanScore <= 50 -> 2
                            else             -> 1
                        },
                        onReplay    = onRematch,
                        onMenu      = onExit,
                        headline    = "You Win! 🎉",
                        primaryLabel = "Play Again"
                    )
                } else {
                    DefeatPanel(
                        score      = humanScore,
                        bestScore  = bestScore,
                        onTryAgain = onRematch,
                        onMenu     = onExit,
                        headline   = "$winnerName wins with ${gameState.scores[gameState.gameWinner] ?: 0} pts"
                    )
                }
            }
        }

        else -> {}
    }
}

// ── Trick area ────────────────────────────────────────────────────────────────

@Composable
private fun TrickArea(
    currentTrick: List<TrickCard>,
    leadSeat: Seat,
    modifier: Modifier = Modifier
) {
    val cardW = 56.dp
    Box(
        modifier          = modifier
            .background(Dark2, RoundedCornerShape(8.dp))
            .border(1.dp, Aqua2.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment  = Alignment.Center
    ) {
        Column(
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(4.dp)
        ) {
            TrickSlot(Seat.NORTH, currentTrick, cardW)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                TrickSlot(Seat.WEST, currentTrick, cardW)
                Spacer(Modifier.width(4.dp))
                TrickSlot(Seat.EAST, currentTrick, cardW)
            }
            TrickSlot(Seat.SOUTH, currentTrick, cardW)
        }
    }
}

@Composable
private fun TrickSlot(seat: Seat, trick: List<TrickCard>, cardWidth: Dp) {
    val played = trick.firstOrNull { it.seat == seat }
    if (played != null) {
        PlayingCardView(
            card     = played.card,
            modifier = Modifier.width(cardWidth)
        )
    } else {
        EmptyCardSlot(
            label       = seat.displayName()[0].toString(),
            highlighted = false,
            onClick     = null,
            modifier    = Modifier.width(cardWidth)
        )
    }
}

// ── Opponent displays ─────────────────────────────────────────────────────────

@Composable
private fun OpponentRow(seat: Seat, cardCount: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text      = seat.displayName(),
            fontSize  = 11.sp,
            color     = Aqua2,
            modifier  = Modifier.padding(bottom = 2.dp)
        )
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            repeat(cardCount) {
                CardBackView(modifier = Modifier.width(20.dp).padding(horizontal = 1.dp))
            }
        }
    }
}

@Composable
private fun OpponentColumn(seat: Seat, cardCount: Int, modifier: Modifier = Modifier) {
    Column(
        modifier              = modifier,
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Center
    ) {
        Text(
            text      = seat.displayName()[0].toString(),
            fontSize  = 10.sp,
            color     = Aqua2,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )
        repeat(cardCount) {
            CardBackView(modifier = Modifier.width(32.dp).padding(vertical = 1.dp))
        }
    }
}

// ── Human hand ────────────────────────────────────────────────────────────────

@Composable
private fun HumanHand(
    hand: List<Card>,
    selectedCards: Set<Card>,
    phase: HeartsPhase,
    heartsBroken: Boolean,
    trick: List<TrickCard>,
    trickNumber: Int,
    leadSeat: Seat,
    onCardTap: (Card) -> Unit,
    onPassConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPassing   = phase == HeartsPhase.PASSING
    val isMyTurn    = phase == HeartsPhase.PLAYING &&
        HeartsController.nextSeatToPlay(trick, leadSeat) == Seat.SOUTH

    val legalCards  = if (isMyTurn)
        HeartsController.legalPlays(hand, trick, heartsBroken, trickNumber).toSet()
    else
        emptySet()

    Column(modifier = modifier) {
        if (isPassing) {
            Text(
                text     = "Select 3 cards to pass  (${selectedCards.size}/3)",
                fontSize = 12.sp,
                color    = Aqua2,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier              = Modifier.fillMaxWidth()
        ) {
            items(hand.sortedWith(compareBy({ it.suit.ordinal }, { it.rank.value }))) { card ->
                val selected = card in selectedCards
                val isLegal  = !isMyTurn || card in legalCards
                val dimmed   = isMyTurn && !isLegal

                PlayingCardView(
                    card     = card,
                    modifier = Modifier
                        .width(52.dp)
                        .then(if (dimmed) Modifier else Modifier)
                        .then(
                            if (selected) Modifier.border(2.dp, CardHighlight, RoundedCornerShape(8.dp))
                            else Modifier
                        )
                        .then(if (dimmed) Modifier.background(Dark0.copy(alpha = 0.5f)) else Modifier),
                    selected = selected,
                    onClick  = if (isPassing || (isMyTurn && isLegal)) {
                        { onCardTap(card) }
                    } else null
                )
            }
        }

        if (isPassing && selectedCards.size == 3) {
            Button(
                onClick  = onPassConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Text("Confirm Pass")
            }
        }
    }
}
