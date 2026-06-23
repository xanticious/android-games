package com.xanticious.androidgames.view.games.spades

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.spades.SpadesController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.spades.SpadesBid
import com.xanticious.androidgames.model.games.spades.SpadesGameState
import com.xanticious.androidgames.model.games.spades.SpadesPlayer
import com.xanticious.androidgames.model.games.spades.SpadesTeam
import com.xanticious.androidgames.model.games.spades.SpadesTeamScore
import com.xanticious.androidgames.model.games.spades.SpadesTrick
import com.xanticious.androidgames.model.games.spades.SpadesTrickCard
import com.xanticious.androidgames.state.games.spades.SpadesPhase
import com.xanticious.androidgames.state.games.spades.SpadesStateMachine
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.ui.theme.CardTableFelt
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.cards.CardAspectRatio
import com.xanticious.androidgames.view.common.cards.PlayingCardView
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Spades — partnership trick-taking card game (design/card-games/spades).
 *
 * Human (SOUTH) + AI partner (NORTH) vs two AI opponents (WEST, EAST).
 * All game logic lives in [SpadesController]; this composable is thin view code
 * that renders state and dispatches human actions.
 */
@Composable
fun SpadesGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val machine = remember { SpadesStateMachine() }
    val phase   by machine.phase.collectAsState()
    val rng     = remember { Random(System.currentTimeMillis()) }

    var gameState  by remember { mutableStateOf(SpadesGameState()) }
    var pendingBid by remember { mutableIntStateOf(3) }

    // ── Initialise ──────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        machine.startGame()
        gameState = SpadesController.initGame(rng)
        machine.dealt()
    }

    // ── AI bidding: fire whenever the active bidder changes ─────────────────
    // Uses currentBidder as key so each AI seat gets exactly one turn.
    LaunchedEffect(gameState.currentBidder) {
        val bidder = gameState.currentBidder ?: return@LaunchedEffect
        if (bidder == SpadesPlayer.SOUTH) return@LaunchedEffect
        if (phase != SpadesPhase.BIDDING) return@LaunchedEffect
        delay(650L)
        val bid = SpadesController.aiBid(
            player       = bidder,
            hand         = gameState.hands[bidder] ?: emptyList(),
            existingBids = gameState.bids,
            nilAllowed   = gameState.nilAllowed,
            difficulty   = difficulty,
            random       = rng
        )
        gameState = SpadesController.placeBid(gameState, bidder, bid)
        if (SpadesController.isAllBidsIn(gameState)) {
            gameState = SpadesController.startPlayPhase(gameState)
            machine.allBidsIn()
        }
    }

    // ── AI play + trick resolution: fires on every trick-size change ─────────
    // Mirrors the Hearts pattern (LaunchedEffect on trick size).
    LaunchedEffect(phase, gameState.currentTrick?.plays?.size) {
        if (phase != SpadesPhase.PLAYING) return@LaunchedEffect
        val trick = gameState.currentTrick ?: return@LaunchedEffect

        if (trick.isComplete) {
            delay(950L)  // show all four cards briefly before resolving
            var resolved = SpadesController.resolveTrick(gameState)
            gameState = resolved
            if (SpadesController.isHandComplete(resolved)) {
                resolved = SpadesController.scoreHand(resolved)
                gameState = resolved
                machine.handComplete()
                if (SpadesController.isGameOver(resolved)) {
                    machine.gameEndReached()
                }
            }
            return@LaunchedEffect
        }

        val nextPlayer = trick.nextPlayer
        if (nextPlayer == SpadesPlayer.SOUTH) return@LaunchedEffect  // human's turn

        delay(700L)
        val card = SpadesController.aiPlay(nextPlayer, gameState, difficulty, rng)
        gameState = SpadesController.playCard(gameState, nextPlayer, card)
    }

    // ── Convenience accessors ────────────────────────────────────────────────
    val usScore   = gameState.teamScores[SpadesTeam.US]   ?: SpadesTeamScore()
    val themScore = gameState.teamScores[SpadesTeam.THEM] ?: SpadesTeamScore()

    val humanHandLegal: List<Card> = run {
        val trick = gameState.currentTrick
        val isHumanTurn = phase == SpadesPhase.PLAYING &&
            trick?.nextPlayer == SpadesPlayer.SOUTH &&
            trick.isComplete.not()
        if (isHumanTurn) {
            SpadesController.legalPlays(
                gameState.hands[SpadesPlayer.SOUTH] ?: emptyList(),
                trick,
                gameState.spadesBroken
            )
        } else emptyList()
    }

    // ── Callbacks ────────────────────────────────────────────────────────────
    val onHumanBid: (SpadesBid) -> Unit = { bid ->
        gameState = SpadesController.placeBid(gameState, SpadesPlayer.SOUTH, bid)
        if (SpadesController.isAllBidsIn(gameState)) {
            gameState = SpadesController.startPlayPhase(gameState)
            machine.allBidsIn()
        }
    }

    val onHumanPlay: (Card) -> Unit = { card ->
        gameState = SpadesController.playCard(gameState, SpadesPlayer.SOUTH, card)
    }

    val onNextHand: () -> Unit = {
        gameState = SpadesController.dealNewHand(gameState, rng)
        machine.nextHand()
        machine.dealt()
    }

    val onNewGame: () -> Unit = {
        pendingBid = 3
        gameState = SpadesController.initGame(
            rng,
            gameState.gameEndScore,
            gameState.nilAllowed,
            gameState.bagPenaltyEnabled
        )
        machine.rematch()
        machine.dealt()
    }

    // ── Layout ───────────────────────────────────────────────────────────────
    GameScaffold(
        title  = "Spades",
        onExit = onExit,
        hud    = {
            GameHud(
                left   = "Us: ${usScore.score} (b${usScore.bags})",
                center = "Round ${gameState.round}${if (gameState.spadesBroken) " \u2660" else ""}",
                right  = "Them: ${themScore.score} (b${themScore.bags})"
            )
        },
        status = {
            when {
                phase == SpadesPhase.GAME_OVER -> {
                    val won = gameState.winner == SpadesTeam.US
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (won) {
                            VictoryPanel(
                                score       = usScore.score,
                                bestScore   = usScore.score,
                                stars       = 3,
                                onReplay    = onNewGame,
                                onMenu      = onExit,
                                headline    = "You Win!  ${usScore.score}\u2013${themScore.score}"
                            )
                        } else {
                            DefeatPanel(
                                score       = themScore.score,
                                bestScore   = themScore.score,
                                onTryAgain  = onNewGame,
                                onMenu      = onExit,
                                headline    = "They Win!  ${usScore.score}\u2013${themScore.score}"
                            )
                        }
                    }
                }
                phase == SpadesPhase.HAND_SCORED -> {
                    HandResultPanel(
                        gameState  = gameState,
                        usScore    = usScore,
                        themScore  = themScore,
                        onNextHand = onNextHand
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CardTableFelt)
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── North seat (partner) ──────────────────────────────────────
            SeatInfoRow(
                player    = SpadesPlayer.NORTH,
                gameState = gameState,
                phase     = phase,
                modifier  = Modifier.fillMaxWidth()
            )

            // ── Middle row: West | trick well | East ──────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SeatInfoColumn(
                    player    = SpadesPlayer.WEST,
                    gameState = gameState,
                    phase     = phase,
                    modifier  = Modifier.width(72.dp)
                )
                TrickWell(
                    trick    = gameState.currentTrick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                SeatInfoColumn(
                    player    = SpadesPlayer.EAST,
                    gameState = gameState,
                    phase     = phase,
                    modifier  = Modifier.width(72.dp)
                )
            }

            // ── South (human) area ────────────────────────────────────────
            if (phase == SpadesPhase.BIDDING && gameState.currentBidder == SpadesPlayer.SOUTH) {
                BidPanel(
                    pendingBid    = pendingBid,
                    nilAllowed    = gameState.nilAllowed,
                    onBidChange   = { pendingBid = it.coerceIn(0, 13) },
                    onConfirmBid  = { onHumanBid(SpadesBid(pendingBid, pendingBid == 0)) },
                    onNil         = { onHumanBid(SpadesBid(0, isNil = true)) }
                )
            } else {
                HumanHandRow(
                    hand       = gameState.hands[SpadesPlayer.SOUTH] ?: emptyList(),
                    legalCards = humanHandLegal,
                    onPlay     = onHumanPlay
                )
            }
        }
    }
}

// ─────────────────────────── Seat info composables ───────────────────────────

@Composable
private fun SeatInfoRow(
    player: SpadesPlayer,
    gameState: SpadesGameState,
    phase: SpadesPhase,
    modifier: Modifier = Modifier
) {
    val tint = if (player.team == SpadesTeam.US) Aqua2 else Aqua4
    Row(
        modifier              = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text       = "${player.displayName}  ${bidLabel(player, gameState, phase)}",
            color      = tint,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 14.sp
        )
        val handSize = gameState.hands[player]?.size ?: 0
        if (handSize > 0) {
            Spacer(Modifier.width(8.dp))
            Text("[$handSize]", color = tint.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun SeatInfoColumn(
    player: SpadesPlayer,
    gameState: SpadesGameState,
    phase: SpadesPhase,
    modifier: Modifier = Modifier
) {
    val tint = if (player.team == SpadesTeam.US) Aqua2 else Aqua4
    Column(
        modifier              = modifier.padding(4.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Center
    ) {
        Text(player.displayName, color = tint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            text     = bidLabel(player, gameState, phase),
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            fontSize = 11.sp
        )
    }
}

private fun bidLabel(player: SpadesPlayer, state: SpadesGameState, phase: SpadesPhase): String {
    val bid    = state.bids[player]
    val tricks = state.tricksWon[player] ?: 0
    return when {
        bid == null && phase == SpadesPhase.BIDDING -> "…"
        bid == null                                 -> "–"
        bid.isNil                                   -> "Nil \u2713$tricks"
        else                                        -> "bid ${bid.amount} / \u2713$tricks"
    }
}

// ─────────────────────────── Trick well ──────────────────────────────────────

@Composable
private fun TrickWell(trick: SpadesTrick?, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (trick == null || trick.plays.isEmpty()) {
            // Empty table — show a subtle spades icon as placeholder
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .border(2.dp, Aqua3.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("\u2660", color = Aqua3.copy(alpha = 0.35f), fontSize = 28.sp)
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                TrickSlot(trick, SpadesPlayer.NORTH)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TrickSlot(trick, SpadesPlayer.WEST)
                    Spacer(Modifier.width(4.dp))
                    TrickSlot(trick, SpadesPlayer.EAST)
                }
                TrickSlot(trick, SpadesPlayer.SOUTH)
            }
        }
    }
}

@Composable
private fun TrickSlot(trick: SpadesTrick, player: SpadesPlayer) {
    val play = trick.plays.find { it.player == player }
    if (play != null) {
        PlayingCardView(card = play.card, width = 44.dp)
    } else {
        // Empty placeholder keeping layout stable while others play
        Box(Modifier.size(width = 44.dp, height = (44.dp / CardAspectRatio)))
    }
}

// ─────────────────────────── Bid panel ───────────────────────────────────────

@Composable
private fun BidPanel(
    pendingBid: Int,
    nilAllowed: Boolean,
    onBidChange: (Int) -> Unit,
    onConfirmBid: () -> Unit,
    onNil: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Dark1.copy(alpha = 0.92f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Your Bid", style = MaterialTheme.typography.titleMedium, color = Aqua1)
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedButton(
                onClick = { onBidChange(pendingBid - 1) },
                enabled = pendingBid > 1
            ) { Text("–") }
            Text(
                "$pendingBid",
                style      = MaterialTheme.typography.headlineMedium,
                color      = Aqua1,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = { onBidChange(pendingBid + 1) },
                enabled = pendingBid < 13
            ) { Text("+") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onConfirmBid) { Text("Bid $pendingBid") }
            if (nilAllowed) {
                OutlinedButton(onClick = onNil) { Text("Nil (0)") }
            }
        }
    }
}

// ─────────────────────────── Human hand ──────────────────────────────────────

@Composable
private fun HumanHandRow(
    hand: List<Card>,
    legalCards: List<Card>,
    onPlay: (Card) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Dark1.copy(alpha = 0.75f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .padding(vertical = 8.dp)
    ) {
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy((-10).dp)
        ) {
            items(hand) { card ->
                val legal = card in legalCards
                PlayingCardView(
                    card     = card,
                    width    = 50.dp,
                    selected = legal,
                    onClick  = if (legal) ({ onPlay(card) }) else null
                )
            }
        }
    }
}

// ─────────────────────────── Hand-result panel ───────────────────────────────

@Composable
private fun HandResultPanel(
    gameState: SpadesGameState,
    usScore: SpadesTeamScore,
    themScore: SpadesTeamScore,
    onNextHand: () -> Unit
) {
    val result = gameState.lastHandResult ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Hand ${gameState.round - 1} complete",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        val usDelta   = result.usScoreDelta
        val themDelta = result.themScoreDelta
        Text("Us:   ${if (usDelta >= 0) "+" else ""}$usDelta  \u2192  ${usScore.score}  (bags ${usScore.bags})")
        Text("Them: ${if (themDelta >= 0) "+" else ""}$themDelta  \u2192  ${themScore.score}  (bags ${themScore.bags})")
        result.nilResults.forEach { nr ->
            val label = if (nr.succeeded) "\u2713 Nil +100" else "\u2717 Nil \u2212100"
            Text("${nr.player.displayName}: $label")
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick  = onNextHand,
            modifier = Modifier.align(Alignment.End)
        ) { Text("Next Hand") }
    }
}
