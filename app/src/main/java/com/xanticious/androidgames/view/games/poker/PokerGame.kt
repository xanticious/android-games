package com.xanticious.androidgames.view.games.poker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.poker.PokerController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.poker.PokerGameState
import com.xanticious.androidgames.model.games.poker.PokerPlayer
import com.xanticious.androidgames.state.games.poker.PokerPhase
import com.xanticious.androidgames.state.games.poker.PokerStateMachine
import com.xanticious.androidgames.ui.theme.CardHighlight
import com.xanticious.androidgames.ui.theme.CardTableFelt
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.cards.CardBackView
import com.xanticious.androidgames.view.common.cards.PlayingCardView
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Five-Card Draw Poker — human vs three AI opponents.
 *
 * Layout follows `design/card-games/poker/poker-design.md`:
 * - HUD strip: pot + each player's stack.
 * - Board: opponents (face-down) at top, human's hand (face-up) at bottom.
 * - Status slot (below board): action buttons during play; result panel at end.
 *
 * All game logic lives in [PokerController]; this composable only renders state
 * and fires callbacks.
 */
@Composable
fun PokerGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val machine = remember { PokerStateMachine() }
    val phase by machine.phase.collectAsState()
    val random = remember { Random(System.currentTimeMillis()) }

    var gameState by remember {
        mutableStateOf(PokerController.createGame(difficulty))
    }

    // ---- AI advancement ----
    // Re-runs whenever the active player or phase changes.
    LaunchedEffect(gameState.activePlayerIndex, phase) {
        val isBettingPhase = phase == PokerPhase.BETTING_ROUND_1 || phase == PokerPhase.BETTING_ROUND_2
        if (!isBettingPhase) return@LaunchedEffect
        val active = gameState.players.getOrNull(gameState.activePlayerIndex) ?: return@LaunchedEffect
        if (active.isHuman || active.isFolded || active.isAllIn) return@LaunchedEffect

        delay(700L)
        val updated = PokerController.aiAction(gameState, random)
        gameState = updated

        when {
            PokerController.allButOneFolded(updated) -> {
                gameState = PokerController.awardPotToLastPlayer(updated)
                if (phase == PokerPhase.BETTING_ROUND_1) machine.allFoldedRound1()
                else machine.allFoldedRound2()
            }
            PokerController.isBettingRoundComplete(updated) -> {
                if (phase == PokerPhase.BETTING_ROUND_1) {
                    gameState = PokerController.startBettingRound(updated)
                    machine.round1Closed()
                } else {
                    machine.round2Closed()
                }
            }
        }
    }

    // Auto-transition: Anteing -> Dealing -> BettingRound1
    LaunchedEffect(phase) {
        if (phase == PokerPhase.ANTEING) {
            machine.antesPosted()
        }
        if (phase == PokerPhase.DEALING) {
            machine.dealt()
        }
        if (phase == PokerPhase.SHOWDOWN) {
            gameState = PokerController.resolveShowdown(gameState)
            machine.showdownResolved()
        }
    }

    val humanPlayer = gameState.humanPlayer
    val isHumanTurn = phase == PokerPhase.BETTING_ROUND_1 || phase == PokerPhase.BETTING_ROUND_2
        && PokerController.isHumanTurn(gameState)
    val toCall = gameState.toCall

    GameScaffold(
        title = "Poker — 5-Card Draw",
        onExit = onExit,
        hud = {
            GameHud(
                left = "You: \$${humanPlayer.bankroll}",
                center = "Pot: \$${gameState.pot}",
                right = if (phase == PokerPhase.BETTING_ROUND_1 || phase == PokerPhase.BETTING_ROUND_2)
                    "To call: \$${toCall}" else ""
            )
        },
        status = {
            PokerStatusStrip(
                phase = phase,
                gameState = gameState,
                isHumanTurn = isHumanTurn,
                toCall = toCall,
                onFold = {
                    val updated = PokerController.fold(gameState)
                    gameState = updated
                    advanceBettingRound(updated, phase, machine) { gameState = it }
                },
                onCheck = {
                    val updated = PokerController.check(gameState)
                    gameState = updated
                    advanceBettingRound(updated, phase, machine) { gameState = it }
                },
                onCall = {
                    val updated = PokerController.call(gameState)
                    gameState = updated
                    advanceBettingRound(updated, phase, machine) { gameState = it }
                },
                onRaise = {
                    val updated = PokerController.raise(gameState, PokerController.MIN_BET * 2)
                    gameState = updated
                    advanceBettingRound(updated, phase, machine) { gameState = it }
                },
                onConfirmDraw = {
                    gameState = PokerController.confirmDraw(gameState, random)
                    machine.drawsComplete()
                    gameState = PokerController.startBettingRound(gameState)
                },
                onNextHand = {
                    if (gameState.sessionOver) {
                        machine.humanBusted()
                    } else {
                        val seed = System.currentTimeMillis()
                        gameState = PokerController.startHand(gameState, seed)
                        machine.nextHand()
                        machine.antesPosted()
                        machine.dealt()
                    }
                },
                onNewSession = {
                    gameState = PokerController.createGame(difficulty)
                    machine.newSession()
                },
                onExit = onExit
            )
        }
    ) {
        // Board: felt table with opponents at top, human's hand at bottom.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CardTableFelt)
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Opponents row.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                gameState.players.drop(1).forEach { bot ->
                    BotSeat(
                        player = bot,
                        isActive = gameState.activePlayerIndex == bot.index && (
                            phase == PokerPhase.BETTING_ROUND_1 || phase == PokerPhase.BETTING_ROUND_2),
                        showCards = phase == PokerPhase.SHOWDOWN || phase == PokerPhase.AWARD_POT,
                        isWinner = gameState.handResult?.winnerIndices?.contains(bot.index) == true
                    )
                }
            }

            // Center info.
            if (phase == PokerPhase.IDLE) {
                StartHandButton {
                    val seed = System.currentTimeMillis()
                    gameState = PokerController.startHand(gameState, seed)
                    machine.handStarted()
                }
            }

            // Human's hand.
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (gameState.sessionOver.not() && humanPlayer.hand.isNotEmpty()) {
                    Text(
                        text = if (phase == PokerPhase.DRAWING) "Tap cards to discard" else "Your hand",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        humanPlayer.hand.forEachIndexed { idx, card ->
                            val isSelected = idx in gameState.selectedDiscards
                            val canDiscard = phase == PokerPhase.DRAWING
                            Box(
                                modifier = if (isSelected) Modifier.offset(y = (-10).dp) else Modifier
                            ) {
                                PlayingCardView(
                                    card = card,
                                    modifier = Modifier.width(56.dp),
                                    selected = isSelected,
                                    onClick = if (canDiscard) {
                                        { gameState = PokerController.toggleDiscard(gameState, idx) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---- private helpers ----

private fun advanceBettingRound(
    state: PokerGameState,
    phase: PokerPhase,
    machine: PokerStateMachine,
    updateState: (PokerGameState) -> Unit
) {
    when {
        PokerController.allButOneFolded(state) -> {
            val awarded = PokerController.awardPotToLastPlayer(state)
            updateState(awarded)
            if (phase == PokerPhase.BETTING_ROUND_1) machine.allFoldedRound1()
            else machine.allFoldedRound2()
        }
        PokerController.isBettingRoundComplete(state) -> {
            if (phase == PokerPhase.BETTING_ROUND_1) {
                updateState(PokerController.startBettingRound(state))
                machine.round1Closed()
            } else {
                machine.round2Closed()
            }
        }
    }
}

@Composable
private fun BotSeat(
    player: PokerPlayer,
    isActive: Boolean,
    showCards: Boolean,
    isWinner: Boolean
) {
    val borderColor = when {
        isWinner -> CardHighlight
        isActive -> CardHighlight.copy(alpha = 0.6f)
        else -> CardHighlight.copy(alpha = 0f)
    }
    Column(
        modifier = Modifier
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = player.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (player.isFolded) "Folded" else "\$${player.bankroll}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        if (player.currentRoundBet > 0) {
            Text(
                text = "Bet: \$${player.currentRoundBet}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (player.hand.isNotEmpty() && !player.isFolded) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (showCards) {
                    player.hand.forEach { card ->
                        PlayingCardView(card = card, modifier = Modifier.width(36.dp))
                    }
                } else {
                    repeat(player.hand.size) {
                        CardBackView(modifier = Modifier.width(36.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StartHandButton(onStart: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Button(onClick = onStart) {
            Text("Deal Hand")
        }
    }
}

@Composable
private fun PokerStatusStrip(
    phase: PokerPhase,
    gameState: PokerGameState,
    isHumanTurn: Boolean,
    toCall: Int,
    onFold: () -> Unit,
    onCheck: () -> Unit,
    onCall: () -> Unit,
    onRaise: () -> Unit,
    onConfirmDraw: () -> Unit,
    onNextHand: () -> Unit,
    onNewSession: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        when (phase) {
            PokerPhase.BETTING_ROUND_1, PokerPhase.BETTING_ROUND_2 -> {
                if (isHumanTurn) {
                    BettingActionBar(toCall = toCall, onFold = onFold, onCheck = onCheck, onCall = onCall, onRaise = onRaise)
                } else {
                    val active = gameState.players.getOrNull(gameState.activePlayerIndex)
                    Text(
                        text = "${active?.name ?: "?"} is thinking…",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            PokerPhase.DRAWING -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    val discardCount = gameState.selectedDiscards.size
                    Text(
                        text = if (discardCount == 0) "Tap cards to discard, then confirm (or keep all)"
                        else "$discardCount card${if (discardCount != 1) "s" else ""} selected for discard",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onConfirmDraw) {
                        Text(if (discardCount == 0) "Keep All" else "Draw $discardCount Card${if (discardCount != 1) "s" else ""}")
                    }
                }
            }
            PokerPhase.AWARD_POT -> {
                HandResultBar(
                    gameState = gameState,
                    onNextHand = onNextHand,
                    onNewSession = onNewSession,
                    onExit = onExit
                )
            }
            PokerPhase.SESSION_OVER -> {
                DefeatPanel(
                    score = gameState.humanPlayer.bankroll,
                    bestScore = gameState.humanPlayer.bankroll,
                    onTryAgain = onNewSession,
                    onMenu = onExit,
                    headline = "Busted Out!"
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun BettingActionBar(
    toCall: Int,
    onFold: () -> Unit,
    onCheck: () -> Unit,
    onCall: () -> Unit,
    onRaise: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onFold, modifier = Modifier.weight(1f)) {
            Text("Fold")
        }
        if (toCall == 0) {
            Button(onClick = onCheck, modifier = Modifier.weight(1f)) {
                Text("Check")
            }
        } else {
            Button(onClick = onCall, modifier = Modifier.weight(1f)) {
                Text("Call \$$toCall")
            }
        }
        Button(onClick = onRaise, modifier = Modifier.weight(1f)) {
            Text(if (toCall == 0) "Bet" else "Raise")
        }
    }
}

@Composable
private fun HandResultBar(
    gameState: PokerGameState,
    onNextHand: () -> Unit,
    onNewSession: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val result = gameState.handResult
        if (result != null) {
            val humanWon = 0 in result.winnerIndices
            Text(
                text = gameState.statusMessage,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (humanWon) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
            // Show all hand names.
            if (result.handNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                result.handNames.entries.forEach { (idx, name) ->
                    val playerName = gameState.players.getOrNull(idx)?.name ?: "?"
                    Text(
                        text = "$playerName: $name",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (gameState.sessionOver) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onNewSession) { Text("New Game") }
                OutlinedButton(onClick = onExit) { Text("Exit") }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onNextHand) { Text("Next Hand") }
                OutlinedButton(onClick = onExit) { Text("Exit") }
            }
        }
    }
}
