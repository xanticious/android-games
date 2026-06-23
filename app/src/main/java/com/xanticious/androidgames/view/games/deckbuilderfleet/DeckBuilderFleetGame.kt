package com.xanticious.androidgames.view.games.deckbuilderfleet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.xanticious.androidgames.controller.games.deckbuilderfleet.DeckBuilderFleetController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.deckbuilderfleet.FleetCard
import com.xanticious.androidgames.model.games.deckbuilderfleet.FleetGameState
import com.xanticious.androidgames.model.games.deckbuilderfleet.FortInPlay
import com.xanticious.androidgames.model.games.deckbuilderfleet.Winner
import com.xanticious.androidgames.state.games.deckbuilderfleet.FleetDeckStateMachine
import com.xanticious.androidgames.state.games.deckbuilderfleet.FleetPhase
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.ui.theme.CardFace
import com.xanticious.androidgames.ui.theme.CardHighlight
import com.xanticious.androidgames.ui.theme.CardTableFelt
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlin.random.Random

/**
 * Deck Builder Fleet — head-to-head deck-building duel, human vs AI.
 * Buy ships and forts from the central Trade Row, generate Trade and Combat,
 * destroy the opponent's forts then reduce their health to zero to win.
 */
@Composable
fun DeckBuilderFleetGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { DeckBuilderFleetController }
    val config     = remember(difficulty) { controller.configFor(difficulty) }
    val machine    = remember { FleetDeckStateMachine() }
    val phase      by machine.phase.collectAsState()

    var gameState by remember { mutableStateOf<FleetGameState?>(null) }
    val random     = remember { Random.Default }

    // Initialise once on first composition
    LaunchedEffect(Unit) {
        machine.startMatch()
        val initial  = controller.createInitialState(config, random)
        machine.decksBuilt()
        val withHand = controller.startTurn(initial, random)
        gameState    = withHand
        machine.handDrawn()
    }

    // Run the bot turn automatically whenever the phase enters OPPONENT_TURN
    LaunchedEffect(phase) {
        if (phase == FleetPhase.OPPONENT_TURN) {
            val gs       = gameState ?: return@LaunchedEffect
            val afterBot = controller.botTurn(gs, difficulty, random)
            gameState    = afterBot
            machine.opponentResolved()
            when (afterBot.winner) {
                Winner.PLAYER -> machine.opponentHealthZero()
                Winner.BOT    -> machine.playerHealthZero()
                null -> {
                    machine.continueGame()
                    val withHand = controller.startTurn(afterBot, random)
                    gameState    = withHand
                    machine.handDrawn()
                }
            }
        }
    }

    val gs = gameState

    GameScaffold(
        title  = "Deck Builder Fleet",
        onExit = onExit,
        hud    = {
            if (gs != null) FleetHud(gs)
        },
        status = {
            if (gs != null) {
                when (phase) {
                    FleetPhase.VICTORY -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        VictoryPanel(
                            score       = gs.turnCount,
                            bestScore   = gs.turnCount,
                            stars       = when (difficulty) {
                                GameDifficulty.HARD   -> 3
                                GameDifficulty.MEDIUM -> 2
                                else                  -> 1
                            },
                            onReplay    = onExit,
                            onMenu      = onExit,
                            headline    = "Fleet Victory!"
                        )
                    }
                    FleetPhase.DEFEAT -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        DefeatPanel(
                            score      = gs.turnCount,
                            bestScore  = gs.turnCount,
                            onTryAgain = onExit,
                            onMenu     = onExit,
                            headline   = "Fleet Sunk!"
                        )
                    }
                    else -> {}
                }
            }
        }
    ) {
        if (gs == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Aqua2)
            }
        } else {
            FleetBoard(
                state     = gs,
                phase     = phase,
                onPlay    = { id  -> gameState = controller.playCard(gs, id, random) },
                onBuy     = { idx -> gameState = controller.buyCard(gs, idx, random) },
                onAttack  = { gameState = controller.applyAllCombat(gs) },
                onEndTurn = {
                    val ended = controller.endTurn(gs)
                    gameState = ended
                    machine.turnEnded()
                }
            )
        }
    }
}

// ── HUD ───────────────────────────────────────────────────────────────────────

@Composable
private fun FleetHud(state: FleetGameState) {
    val botFortHp = state.bot.forts.sumOf { it.remainingDefense }
    val botLabel  = "Bot ❤${state.bot.health}" + if (botFortHp > 0) " 🛡$botFortHp" else ""
    val youLabel  = "❤${state.player.health} 💰${state.currentCoins} ⚔${state.currentCombat + state.currentSubCombat}"
    GameHud(
        left   = botLabel,
        center = "Turn ${state.turnCount + 1}",
        right  = youLabel
    )
}

// ── Board ─────────────────────────────────────────────────────────────────────

@Composable
private fun FleetBoard(
    state:     FleetGameState,
    phase:     FleetPhase,
    onPlay:    (String) -> Unit,
    onBuy:     (Int) -> Unit,
    onAttack:  () -> Unit,
    onEndTurn: () -> Unit
) {
    val isInteractive = phase == FleetPhase.PLAYER_ACTIONS

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CardTableFelt)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Bot status strip ─────────────────────────────────────────────────
        BotStatusRow(state)

        // ── Trade Row ────────────────────────────────────────────────────────
        TradeRowSection(
            tradeRow  = state.tradeRow,
            coins     = state.currentCoins,
            isEnabled = isInteractive,
            onBuy     = onBuy
        )

        Spacer(Modifier.weight(1f))

        // ── Player play area preview ─────────────────────────────────────────
        if (state.player.playArea.isNotEmpty()) {
            PlayAreaRow(state.player.playArea)
        }

        // ── Player hand ──────────────────────────────────────────────────────
        PlayerHandSection(
            hand          = state.player.hand,
            isInteractive = isInteractive,
            onPlay        = onPlay
        )

        // ── Action buttons ───────────────────────────────────────────────────
        ActionButtons(
            state         = state,
            isInteractive = isInteractive,
            onAttack      = onAttack,
            onEndTurn     = onEndTurn
        )
    }
}

// ── Bot status row ────────────────────────────────────────────────────────────

@Composable
private fun BotStatusRow(state: FleetGameState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Dark1)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text       = "Opponent",
            color      = Aqua4,
            fontWeight = FontWeight.Bold,
            style      = MaterialTheme.typography.labelMedium
        )
        Spacer(Modifier.weight(1f))
        if (state.bot.forts.isEmpty()) {
            Text("No forts", color = Aqua1, style = MaterialTheme.typography.labelSmall)
        } else {
            state.bot.forts.forEach { fort -> FortChip(fort) }
        }
    }
}

@Composable
private fun FortChip(fort: FortInPlay) {
    Box(
        modifier = Modifier
            .background(Dark2, shape = MaterialTheme.shapes.small)
            .border(1.dp, Aqua3, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text  = "🛡 ${fort.card.type.name} ${fort.remainingDefense}HP",
            color = Aqua1,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// ── Trade Row ─────────────────────────────────────────────────────────────────

@Composable
private fun TradeRowSection(
    tradeRow:  List<FleetCard>,
    coins:     Int,
    isEnabled: Boolean,
    onBuy:     (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text     = "Trade Row",
            color    = Aqua0,
            style    = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(tradeRow) { idx, card ->
                val affordable = coins >= card.type.cost
                MarketCard(
                    card      = card,
                    canAfford = affordable && isEnabled,
                    onClick   = { if (isEnabled && affordable) onBuy(idx) }
                )
            }
            if (tradeRow.isEmpty()) {
                item {
                    Text(
                        text  = "Market empty",
                        color = Aqua3,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketCard(card: FleetCard, canAfford: Boolean, onClick: () -> Unit) {
    val border = if (canAfford) CardHighlight else Aqua3
    Column(
        modifier = Modifier
            .width(80.dp)
            .background(if (canAfford) Dark2 else Dark0, MaterialTheme.shapes.small)
            .border(1.dp, border, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text      = card.type.name,
            color     = Aqua0,
            style     = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines  = 2
        )
        Text(
            text  = "💰${card.type.cost}",
            color = if (canAfford) CardHighlight else Aqua3,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (card.type.tradeValue > 0)
                Text("T${card.type.tradeValue}", color = Aqua2, style = MaterialTheme.typography.labelSmall)
            if (card.type.combatValue > 0)
                Text("C${card.type.combatValue}", color = GameEnemy, style = MaterialTheme.typography.labelSmall)
            if (card.isFort)
                Text("🛡${card.type.fortDefense}", color = Aqua3, style = MaterialTheme.typography.labelSmall)
        }
        Text(
            text  = card.type.shipClass.label,
            color = Aqua3,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// ── Play area preview ─────────────────────────────────────────────────────────

@Composable
private fun PlayAreaRow(playArea: List<FleetCard>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text     = "In Play",
            color    = Aqua1,
            style    = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            itemsIndexed(playArea) { _, card ->
                Box(
                    modifier = Modifier
                        .background(Dark2, MaterialTheme.shapes.small)
                        .border(1.dp, GamePlayer, MaterialTheme.shapes.small)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(card.type.name, color = Aqua0, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ── Player hand ───────────────────────────────────────────────────────────────

@Composable
private fun PlayerHandSection(
    hand:          List<FleetCard>,
    isInteractive: Boolean,
    onPlay:        (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text     = "Hand (${hand.size})",
            color    = Aqua0,
            style    = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if (hand.isEmpty()) {
            Text(
                text  = "No cards in hand",
                color = Aqua3,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(4.dp)
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(hand) { _, card ->
                    HandCard(card = card, isInteractive = isInteractive, onClick = { onPlay(card.instanceId) })
                }
            }
        }
    }
}

@Composable
private fun HandCard(card: FleetCard, isInteractive: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .background(CardFace, MaterialTheme.shapes.small)
            .border(
                width = 1.5.dp,
                color = if (isInteractive) GamePlayer else Aqua3,
                shape = MaterialTheme.shapes.small
            )
            .clickable(enabled = isInteractive, onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text      = card.type.name,
            color     = Dark0,
            style     = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines  = 2,
            fontWeight = FontWeight.Bold
        )
        Text(
            text  = card.type.shipClass.label,
            color = Dark2,
            style = MaterialTheme.typography.labelSmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (card.type.tradeValue > 0)
                Text("T${card.type.tradeValue}", color = Aqua3, style = MaterialTheme.typography.labelSmall)
            if (card.type.combatValue > 0)
                Text("C${card.type.combatValue}", color = GameEnemy, style = MaterialTheme.typography.labelSmall)
            if (card.isFort)
                Text("🛡${card.type.fortDefense}", color = Aqua4, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ── Action buttons ────────────────────────────────────────────────────────────

@Composable
private fun ActionButtons(
    state:         FleetGameState,
    isInteractive: Boolean,
    onAttack:      () -> Unit,
    onEndTurn:     () -> Unit
) {
    val hasCombat  = (state.currentCombat + state.currentSubCombat) > 0
    val turnLabel  = if (isInteractive) "End Turn" else "Waiting…"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick  = onAttack,
            enabled  = isInteractive && hasCombat,
            colors   = ButtonDefaults.buttonColors(containerColor = GameEnemy),
            modifier = Modifier.weight(1f)
        ) {
            Text("⚔ Attack (${state.currentCombat + state.currentSubCombat})")
        }
        OutlinedButton(
            onClick  = onEndTurn,
            enabled  = isInteractive,
            modifier = Modifier.weight(1f)
        ) {
            Text(turnLabel)
        }
    }
}
