package com.xanticious.androidgames.view.games.loveletter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.loveletter.LoveLetterController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.loveletter.LoveLetterCard
import com.xanticious.androidgames.model.games.loveletter.LoveLetterGame
import com.xanticious.androidgames.state.games.loveletter.LoveLetterPhase
import com.xanticious.androidgames.state.games.loveletter.LoveLetterStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Love Letter — human vs AI deduction card game (`design/card-games/love-letter`).
 * Tap a card to play it; inline choosers below the board handle target/guess selection.
 */
@Composable
fun LoveLetterGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { LoveLetterController() }
    val machine = remember { LoveLetterStateMachine() }
    val phase by machine.phase.collectAsState()

    // Player-count selection screen before the first round
    var playerCount by rememberSaveable { mutableIntStateOf(0) }
    var game by remember { mutableStateOf<LoveLetterGame?>(null) }
    val aiRandom = remember { Random.Default }

    // Helper: start a brand-new round, preserving token counts from the current game
    fun startNewRound(base: LoveLetterGame): LoveLetterGame {
        val seed = System.currentTimeMillis()
        return controller.startRound(base, seed)
    }

    // ── Player count lobby ────────────────────────────────────────────────────
    if (playerCount == 0) {
        PlayerCountLobby(difficulty = difficulty) { count ->
            playerCount = count
            val initial = controller.initialGame(count, difficulty, System.currentTimeMillis())
            game = startNewRound(initial)
            machine.startGame()
            machine.roundSetup()
        }
        return
    }

    val currentGame = game ?: return

    // ── Human draw at start of turn ───────────────────────────────────────────
    LaunchedEffect(currentGame.currentPlayerIndex, phase) {
        if (phase == LoveLetterPhase.PLAYING &&
            currentGame.currentPlayer.isHuman &&
            currentGame.currentPlayer.hand.size == 1 &&
            currentGame.deck.isNotEmpty()
        ) {
            game = controller.drawCard(currentGame)
        }
    }

    // ── AI turn auto-processing ───────────────────────────────────────────────
    LaunchedEffect(currentGame.currentPlayerIndex, phase) {
        if (phase == LoveLetterPhase.PLAYING && !currentGame.currentPlayer.isHuman) {
            delay(700L)
            val afterTurn = controller.takeAiTurn(currentGame, aiRandom)
            if (controller.checkRoundOver(afterTurn)) {
                val winnerIdx = controller.roundWinnerIndex(afterTurn)
                val afterToken = controller.awardToken(afterTurn, winnerIdx)
                game = afterToken
                if (controller.gameWinner(afterToken) != null) machine.gameWon() else machine.roundOver()
            } else {
                game = controller.advanceTurn(afterTurn)
            }
        }
    }

    val g = game ?: return
    val humanPlayer = g.players[0]
    val isHumanTurn = phase == LoveLetterPhase.PLAYING && g.currentPlayer.isHuman
    val mustCountess = isHumanTurn && controller.mustPlayCountess(g.currentPlayer.hand)

    GameScaffold(
        title = "Love Letter",
        onExit = onExit,
        hud = {
            GameHud(
                left = "Round ${g.roundNumber}",
                center = if (phase == LoveLetterPhase.PLAYING) {
                    if (isHumanTurn) "Your turn" else "${g.currentPlayer.name}'s turn"
                } else "",
                right = "First to ${g.tokensToWin} tokens"
            )
        },
        status = {
            StatusSlot(
                game = g,
                phase = phase,
                isHumanTurn = isHumanTurn,
                mustCountess = mustCountess,
                controller = controller,
                onNextRound = {
                    val next = controller.nextRound(g)
                    val withNewRound = startNewRound(next)
                    game = withNewRound
                    machine.nextRound()
                },
                onRematch = {
                    val fresh = controller.initialGame(playerCount, difficulty, System.currentTimeMillis())
                    game = startNewRound(fresh)
                    machine.rematch()
                    machine.roundSetup()
                },
                onSetPendingTarget = { targetIdx ->
                    // Guard: target chosen, now need to pick the guess card
                    game = g.copy(pendingTargetIndex = targetIdx)
                },
                onCardPlayed = { card, targetIdx, guardGuess ->
                    val afterPlay = controller.playCard(g, card, targetIdx, guardGuess)
                    val advanced = controller.advanceTurn(afterPlay)
                    if (controller.checkRoundOver(afterPlay)) {
                        val winnerIdx = controller.roundWinnerIndex(afterPlay)
                        val afterToken = controller.awardToken(afterPlay, winnerIdx)
                        game = afterToken
                        if (controller.gameWinner(afterToken) != null) machine.gameWon() else machine.roundOver()
                    } else {
                        game = advanced
                    }
                },
                onExit = onExit
            )
        }
    ) {
        // ── Board ─────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Dark1)
                .padding(8.dp)
        ) {
            // Opponents row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                g.players.drop(1).forEachIndexed { botOffset, player ->
                    OpponentSeat(
                        player = player,
                        isCurrentTurn = g.currentPlayerIndex == botOffset + 1 &&
                            phase == LoveLetterPhase.PLAYING
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Deck / burn info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Dark2, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Deck: ${g.deck.size} left", color = Aqua3, style = MaterialTheme.typography.bodyMedium)
                if (g.revealedBurnCards.isNotEmpty()) {
                    Text(
                        "Revealed: ${g.revealedBurnCards.joinToString { it.displayName }}",
                        color = Aqua3,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text("Burned: 🂠", color = Aqua0, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(8.dp))

            // Last effect description
            if (g.lastEffect.isNotEmpty()) {
                Text(
                    g.lastEffect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Dark2, RoundedCornerShape(6.dp))
                        .padding(6.dp),
                    color = Aqua4,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.weight(1f))

            // Human hand (shown even when it's not human's turn for reference)
            if (phase == LoveLetterPhase.PLAYING || phase == LoveLetterPhase.ROUND_OVER) {
                PlayerHandArea(
                    player = humanPlayer,
                    isYourTurn = isHumanTurn && g.pendingCardPlay == null,
                    mustCountess = mustCountess,
                    selectedCard = g.pendingCardPlay,
                    onCardTapped = { card ->
                        if (!isHumanTurn) return@PlayerHandArea
                        if (mustCountess && card != LoveLetterCard.COUNTESS) return@PlayerHandArea
                        val needs = controller.validTargets(g, card, 0).isNotEmpty()
                        val played = g.copy(pendingCardPlay = card, pendingTargetIndex = null)
                        game = if (!needs) {
                            // No target needed — play immediately
                            val afterPlay = controller.playCard(g, card, null, null)
                            val advanced = controller.advanceTurn(afterPlay)
                            if (controller.checkRoundOver(afterPlay)) {
                                val winnerIdx = controller.roundWinnerIndex(afterPlay)
                                val afterToken = controller.awardToken(afterPlay, winnerIdx)
                                if (controller.gameWinner(afterToken) != null) machine.gameWon() else machine.roundOver()
                                afterToken
                            } else {
                                advanced
                            }
                        } else {
                            played
                        }
                    }
                )
            }
        }
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────────

@Composable
private fun PlayerCountLobby(difficulty: GameDifficulty, onSelect: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Dark1),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("Love Letter", style = MaterialTheme.typography.headlineMedium, color = Aqua4)
            Text("How many players?", style = MaterialTheme.typography.bodyLarge, color = Aqua3)
            Text("(1 human + AI opponents)", style = MaterialTheme.typography.bodySmall, color = Aqua0)
            Spacer(Modifier.height(8.dp))
            listOf(2 to "2 Players (You + 1 Bot)", 3 to "3 Players (You + 2 Bots)", 4 to "4 Players (You + 3 Bots)")
                .forEach { (count, label) ->
                    Button(
                        onClick = { onSelect(count) },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        colors = ButtonDefaults.buttonColors(containerColor = Aqua3)
                    ) {
                        Text(label)
                    }
                }
            Spacer(Modifier.height(8.dp))
            Text("Difficulty: ${difficulty.label}", style = MaterialTheme.typography.bodySmall, color = Aqua0)
        }
    }
}

@Composable
private fun OpponentSeat(player: com.xanticious.androidgames.model.games.loveletter.LoveLetterPlayer, isCurrentTurn: Boolean) {
    val alpha = if (player.isEliminated) 0.4f else 1f
    Column(
        modifier = Modifier
            .alpha(alpha)
            .background(
                if (isCurrentTurn) Aqua3.copy(alpha = 0.2f) else Dark2,
                RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isCurrentTurn) 2.dp else 0.dp,
                color = if (isCurrentTurn) Aqua3 else Dark2,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
            .width(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(player.name, color = Aqua4, fontWeight = FontWeight.Bold, fontSize = 13.sp)

        // Token dots
        Row(horizontalArrangement = Arrangement.Center) {
            repeat(player.tokens) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Aqua4)
                        .padding(1.dp)
                )
                Spacer(Modifier.width(2.dp))
            }
        }

        if (player.isEliminated) {
            Text("OUT", color = GameEnemy, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        } else if (player.isProtected) {
            Text("🛡 Protected", color = Aqua3, fontSize = 10.sp)
        }

        // Discard pile (show card values)
        if (player.discards.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Discards: ${player.discards.joinToString(" ") { it.value.toString() }}",
                color = Aqua0,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }

        // Face-down hand indicator
        if (!player.isEliminated) {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(30.dp, 42.dp)
                    .background(Aqua3.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .border(1.dp, Aqua3.copy(alpha = 0.6f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("?", color = Aqua4, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PlayerHandArea(
    player: com.xanticious.androidgames.model.games.loveletter.LoveLetterPlayer,
    isYourTurn: Boolean,
    mustCountess: Boolean,
    selectedCard: LoveLetterCard?,
    onCardTapped: (LoveLetterCard) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Dark2, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (isYourTurn) {
                if (mustCountess) "You must play Countess!" else "Tap a card to play it"
            } else "Your hand",
            color = if (mustCountess) GameAccent else Aqua3,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            player.hand.forEach { card ->
                val isForced = mustCountess && card != LoveLetterCard.COUNTESS
                val isSelected = card == selectedCard
                LoveLetterCardView(
                    card = card,
                    selected = isSelected,
                    dimmed = isForced,
                    clickable = isYourTurn && !isForced,
                    onClick = { onCardTapped(card) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusSlot(
    game: LoveLetterGame,
    phase: LoveLetterPhase,
    isHumanTurn: Boolean,
    mustCountess: Boolean,
    controller: LoveLetterController,
    onNextRound: () -> Unit,
    onRematch: () -> Unit,
    onSetPendingTarget: (Int) -> Unit,
    onCardPlayed: (LoveLetterCard, Int?, LoveLetterCard?) -> Unit,
    onExit: () -> Unit
) {
    when {
        phase == LoveLetterPhase.GAME_OVER -> {
            val humanWon = controller.gameWinner(game) == 0
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (humanWon) {
                    VictoryPanel(
                        score = game.players[0].tokens,
                        bestScore = game.players[0].tokens,
                        stars = 3,
                        onReplay = onRematch,
                        onMenu = onExit,
                        headline = "You win the game!",
                        primaryLabel = "Play again"
                    )
                } else {
                    DefeatPanel(
                        score = game.players[0].tokens,
                        bestScore = game.players[0].tokens,
                        onTryAgain = onRematch,
                        onMenu = onExit,
                        headline = "Game over!"
                    )
                }
            }
        }

        phase == LoveLetterPhase.ROUND_OVER -> {
            val winnerIdx = controller.roundWinnerIndex(game)
            val winnerName = game.players.getOrNull(winnerIdx)?.name ?: "Unknown"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Dark2)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Round ${game.roundNumber} over!", color = Aqua4, fontWeight = FontWeight.Bold)
                Text("$winnerName wins this round!", color = Aqua3)
                Spacer(Modifier.height(4.dp))
                // Token summary
                game.players.forEach { p ->
                    Text("${p.name}: ${p.tokens} tokens", color = Aqua0, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onNextRound,
                    colors = ButtonDefaults.buttonColors(containerColor = Aqua3)
                ) {
                    Text("Next Round")
                }
            }
        }

        isHumanTurn && game.pendingCardPlay != null -> {
            // Inline target / guess chooser
            val card = game.pendingCardPlay
            val targets = controller.validTargets(game, card, 0)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Dark2)
                    .padding(12.dp)
            ) {
                if (card == LoveLetterCard.GUARD && game.pendingTargetIndex != null) {
                    // Guard: choose the guess card
                    Text("Guess the card ${game.players[game.pendingTargetIndex].name} holds:", color = Aqua3)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LoveLetterCard.values()
                            .filter { it != LoveLetterCard.GUARD }
                            .forEach { guessCard ->
                                OutlinedButton(
                                    onClick = { onCardPlayed(card, game.pendingTargetIndex, guessCard) }
                                ) {
                                    Text("${guessCard.displayName} (${guessCard.value})", fontSize = 11.sp)
                                }
                            }
                    }
                } else if (targets.isEmpty()) {
                    // No valid targets — auto-play with no effect
                    Text("No valid targets — card plays with no effect.", color = Aqua0, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { onCardPlayed(card, null, null) },
                        colors = ButtonDefaults.buttonColors(containerColor = Aqua3)
                    ) {
                        Text("Confirm")
                    }
                } else {
                    // Choose a target
                    Text(
                        "Choose a target for ${card.displayName}:",
                        color = Aqua3,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        targets.forEach { targetIdx ->
                            val target = game.players[targetIdx]
                            OutlinedButton(
                                onClick = {
                                    if (card == LoveLetterCard.GUARD) {
                                        // Guard: set pending target so the guess UI appears next
                                        onSetPendingTarget(targetIdx)
                                    } else {
                                        onCardPlayed(card, targetIdx, null)
                                    }
                                }
                            ) {
                                Text(target.name, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        else -> {
            // Empty status — nothing to show
        }
    }
}

@Composable
private fun LoveLetterCardView(
    card: LoveLetterCard,
    selected: Boolean,
    dimmed: Boolean,
    clickable: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        selected -> Aqua3
        else -> Aqua0.copy(alpha = 0.4f)
    }
    Surface(
        modifier = Modifier
            .size(80.dp, 120.dp)
            .alpha(if (dimmed) 0.4f else 1f)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Aqua3.copy(alpha = 0.25f) else Dark2
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                card.value.toString(),
                color = Aqua4,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                card.displayName,
                color = Aqua3,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
