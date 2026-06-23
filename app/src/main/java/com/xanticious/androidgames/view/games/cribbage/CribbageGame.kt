package com.xanticious.androidgames.view.games.cribbage

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.cribbage.CribbageController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cribbage.CribbagePhase
import com.xanticious.androidgames.model.games.cribbage.CribbagePlayer
import com.xanticious.androidgames.model.games.cribbage.CribbageState
import com.xanticious.androidgames.state.games.cribbage.CribbageStateMachine
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
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
import kotlin.random.Random

/**
 * Cribbage — two-player (Human vs AI) card game.
 *
 * Top-level composable wired by the game registry.
 * All game logic lives in [CribbageController]; this composable only renders
 * state and fires callbacks.
 */
@Composable
fun CribbageGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val machine = remember { CribbageStateMachine() }
    var gameState by remember { mutableStateOf(CribbageController.initialState()) }
    var seed by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Kick off the first deal
    LaunchedEffect(Unit) {
        gameState = CribbageController.deal(gameState, seed)
        machine.cardsDealt()
    }

    // Auto-trigger AI discard and AI pegging whenever it is AI's turn
    LaunchedEffect(gameState.phase, gameState.pegTurn) {
        when {
            gameState.phase == CribbagePhase.DISCARDING && gameState.humanDiscardSelection.size == 2 -> {
                // Waiting for human to confirm — don't auto-act
            }
            gameState.phase == CribbagePhase.PLAYING && gameState.pegTurn == CribbagePlayer.AI -> {
                kotlinx.coroutines.delay(600)
                val next = CribbageController.aiPlayCard(gameState, difficulty, Random(seed++))
                gameState = next
                if (next.phase == CribbagePhase.GAME_OVER) machine.gameWon()
                else if (next.phase == CribbagePhase.SHOW_NON_DEALER) machine.playFinished()
            }
            else -> {}
        }
    }

    val phase = gameState.phase

    GameScaffold(
        title = "Cribbage",
        onExit = onExit,
        hud = {
            CribbageHud(gameState)
        },
        status = {
            when (phase) {
                CribbagePhase.GAME_OVER -> {
                    val playerWon = gameState.winner == CribbagePlayer.HUMAN
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (playerWon) {
                            VictoryPanel(
                                score = gameState.humanScore,
                                bestScore = gameState.humanScore,
                                stars = if (gameState.aiScore < 61) 3 else if (gameState.aiScore < 91) 2 else 1,
                                onReplay = {
                                    gameState = CribbageController.initialState()
                                    seed = System.currentTimeMillis()
                                    machine.rematch()
                                    gameState = CribbageController.deal(gameState, seed)
                                },
                                onMenu = onExit,
                                headline = "You Win! ${gameState.humanScore}–${gameState.aiScore}"
                            )
                        } else {
                            DefeatPanel(
                                score = gameState.humanScore,
                                bestScore = gameState.humanScore,
                                onTryAgain = {
                                    gameState = CribbageController.initialState()
                                    seed = System.currentTimeMillis()
                                    machine.rematch()
                                    gameState = CribbageController.deal(gameState, seed)
                                },
                                onMenu = onExit,
                                headline = "You Lose! ${gameState.humanScore}–${gameState.aiScore}"
                            )
                        }
                    }
                }
                else -> {
                    // Score events and action prompts below the board
                    CribbageStatusStrip(
                        gameState = gameState,
                        phase = phase,
                        difficulty = difficulty,
                        onConfirmDiscard = { state ->
                            val next = CribbageController.confirmDiscard(state, difficulty, Random(seed++))
                            gameState = next
                            machine.cribCompleted()
                            // Cut the starter
                            val cut = CribbageController.cut(next, Random(seed++))
                            gameState = cut
                            if (cut.phase == CribbagePhase.GAME_OVER) machine.gameWon()
                            else machine.starterCut()
                        },
                        onCardPlay = { card ->
                            val next = CribbageController.humanPlayCard(gameState, card)
                            gameState = next
                            if (next.phase == CribbagePhase.GAME_OVER) machine.gameWon()
                            else if (next.phase == CribbagePhase.SHOW_NON_DEALER) machine.playFinished()
                        },
                        onGo = {
                            val next = CribbageController.humanPlayCard(gameState, null)
                            gameState = next
                            if (next.phase == CribbagePhase.GAME_OVER) machine.gameWon()
                            else if (next.phase == CribbagePhase.SHOW_NON_DEALER) machine.playFinished()
                        },
                        onCountNonDealer = {
                            val next = CribbageController.scoreNonDealerHand(gameState)
                            gameState = next
                            if (next.phase == CribbagePhase.GAME_OVER) machine.gameWon()
                            else machine.nonDealerCounted()
                        },
                        onCountDealer = {
                            val next = CribbageController.scoreDealerHand(gameState)
                            gameState = next
                            if (next.phase == CribbagePhase.GAME_OVER) machine.gameWon()
                            else machine.dealerCounted()
                        },
                        onCountCrib = {
                            val next = CribbageController.scoreCrib(gameState)
                            gameState = next
                            if (next.phase == CribbagePhase.GAME_OVER) machine.gameWon()
                            else {
                                machine.nextHand()
                                // Deal next hand
                                val dealt = CribbageController.deal(next, seed++)
                                gameState = dealt
                                machine.cardsDealt()
                            }
                        }
                    )
                }
            }
        }
    ) {
        // Board: AI hand (face down), crib indicator, play area, human hand
        CribbageBoard(
            gameState = gameState,
            onToggleDiscard = { idx ->
                if (gameState.phase == CribbagePhase.DISCARDING) {
                    gameState = CribbageController.toggleHumanDiscard(gameState, idx)
                }
            }
        )
    }
}

// ─── HUD ─────────────────────────────────────────────────────────────────────

@Composable
private fun CribbageHud(state: CribbageState) {
    val dealerLabel = if (state.dealer == CribbagePlayer.HUMAN) "You deal" else "AI deals"
    GameHud(
        left = "You: ${state.humanScore}",
        center = dealerLabel,
        right = "AI: ${state.aiScore}"
    )
    // Peg-board strip
    PegBoardStrip(humanScore = state.humanScore, aiScore = state.aiScore, target = state.targetScore)
}

@Composable
private fun PegBoardStrip(humanScore: Int, aiScore: Int, target: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Dark1)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("0", color = Aqua1, fontSize = 10.sp, modifier = Modifier.width(20.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
                .background(Dark2, RoundedCornerShape(8.dp))
        ) {
            // Human peg
            val humanFrac = (humanScore.toFloat() / target).coerceIn(0f, 1f)
            val aiFrac = (aiScore.toFloat() / target).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(12.dp)
                        .background(Aqua2, CircleShape)
                        .then(
                            Modifier.padding(start = (humanFrac * 1000).dp.coerceAtMost(Dp.Infinity))
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(12.dp)
                        .background(Aqua4, CircleShape)
                        .padding(start = (aiFrac * 1000).dp.coerceAtMost(Dp.Infinity))
                )
            }
        }
        Text("$target", color = Aqua1, fontSize = 10.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
    }
}

// ─── Board ────────────────────────────────────────────────────────────────────

@Composable
private fun CribbageBoard(gameState: CribbageState, onToggleDiscard: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CardTableFelt)
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // AI hand (face-down) + crib slot
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "AI Hand",
                    color = Aqua1,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val aiCardCount = when (gameState.phase) {
                        CribbagePhase.DEALING, CribbagePhase.DISCARDING -> gameState.aiHand.size
                        CribbagePhase.CUTTING, CribbagePhase.PLAYING -> gameState.aiPlayHand.size
                        else -> gameState.aiHand.size
                    }
                    repeat(aiCardCount) {
                        CardBackView(modifier = Modifier.width(36.dp))
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Crib (${if (gameState.dealer == CribbagePlayer.HUMAN) "Yours" else "AI's"})",
                    color = Aqua1,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (gameState.crib.isEmpty()) {
                        EmptyCardSlot(
                            label = "—",
                            highlighted = false,
                            modifier = Modifier.width(36.dp)
                        )
                    } else {
                        repeat(gameState.crib.size.coerceAtMost(4)) {
                            CardBackView(modifier = Modifier.width(36.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Starter + Play pile
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Starter", color = Aqua1, fontSize = 11.sp)
                val starter = gameState.starter
                if (starter != null) {
                    Box(
                        modifier = Modifier
                            .border(2.dp, Aqua3, RoundedCornerShape(10))
                            .padding(2.dp)
                    ) {
                        PlayingCardView(card = starter, modifier = Modifier.width(40.dp))
                    }
                } else {
                    EmptyCardSlot(label = "cut", highlighted = false, modifier = Modifier.width(40.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Play pile  (${gameState.pegCount})",
                    color = Aqua1,
                    fontSize = 11.sp
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    items(gameState.playPile) { played ->
                        PlayingCardView(card = played.card, modifier = Modifier.width(32.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Human hand
        Column {
            Text(
                "Your hand${if (gameState.phase == CribbagePhase.DISCARDING) " — tap to discard" else ""}",
                color = Aqua1,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                val displayHand = when (gameState.phase) {
                    CribbagePhase.PLAYING -> gameState.humanPlayHand
                    CribbagePhase.SHOW_NON_DEALER, CribbagePhase.SHOW_DEALER, CribbagePhase.SHOW_CRIB -> gameState.humanHand
                    else -> gameState.humanHand
                }
                displayHand.forEachIndexed { idx, card ->
                    val isSelected = gameState.humanDiscardSelection.contains(idx)
                    val canPlay = gameState.phase == CribbagePhase.PLAYING &&
                        gameState.pegTurn == CribbagePlayer.HUMAN &&
                        CribbageController.legalPeggingCards(gameState, CribbagePlayer.HUMAN).contains(card)
                    PlayingCardView(
                        card = card,
                        modifier = Modifier
                            .width(44.dp)
                            .then(if (isSelected) Modifier.padding(bottom = 8.dp) else Modifier),
                        selected = isSelected || canPlay,
                        onClick = if (gameState.phase == CribbagePhase.DISCARDING) {
                            { onToggleDiscard(idx) }
                        } else null
                    )
                }
            }
        }
    }
}

// ─── Status Strip ─────────────────────────────────────────────────────────────

@Composable
private fun CribbageStatusStrip(
    gameState: CribbageState,
    phase: CribbagePhase,
    difficulty: GameDifficulty,
    onConfirmDiscard: (CribbageState) -> Unit,
    onCardPlay: (Card) -> Unit,
    onGo: () -> Unit,
    onCountNonDealer: () -> Unit,
    onCountDealer: () -> Unit,
    onCountCrib: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Dark0)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Score event callout
        if (gameState.lastScorePoints > 0) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Dark2,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "+${gameState.lastScorePoints}: ${gameState.lastScoreBreakdown}",
                    color = Aqua2,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp),
                    fontSize = 12.sp
                )
            }
        }

        when (phase) {
            CribbagePhase.DEALING -> {
                Text("Dealing cards…", color = Aqua1)
            }

            CribbagePhase.DISCARDING -> {
                val sel = gameState.humanDiscardSelection.size
                Text(
                    "Select 2 cards to discard to the crib (${sel}/2 selected)",
                    color = Aqua1,
                    fontSize = 13.sp
                )
                if (sel == 2) {
                    Button(
                        onClick = { onConfirmDiscard(gameState) },
                        colors = ButtonDefaults.buttonColors(containerColor = Aqua3)
                    ) {
                        Text("Discard & Cut")
                    }
                }
            }

            CribbagePhase.CUTTING -> {
                Text("Cutting for starter…", color = Aqua1)
            }

            CribbagePhase.PLAYING -> {
                val isHumanTurn = gameState.pegTurn == CribbagePlayer.HUMAN
                if (isHumanTurn) {
                    val legal = CribbageController.legalPeggingCards(gameState, CribbagePlayer.HUMAN)
                    Text(
                        "Your turn — count: ${gameState.pegCount}  (tap a card to play)",
                        color = Aqua1, fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        legal.forEach { card ->
                            Button(
                                onClick = { onCardPlay(card) },
                                colors = ButtonDefaults.buttonColors(containerColor = Aqua3)
                            ) {
                                Text(card.label)
                            }
                        }
                        if (legal.isEmpty()) {
                            Button(
                                onClick = onGo,
                                colors = ButtonDefaults.buttonColors(containerColor = Dark2)
                            ) {
                                Text("Go")
                            }
                        }
                    }
                } else {
                    Text("AI is playing… (count: ${gameState.pegCount})", color = Aqua1, fontSize = 13.sp)
                }
            }

            CribbagePhase.SHOW_NON_DEALER -> {
                val nonDealer = if (gameState.dealer == CribbagePlayer.HUMAN) "AI" else "You"
                Text("Count $nonDealer's hand (non-dealer first)", color = Aqua1, fontSize = 13.sp)
                if (gameState.dealer == CribbagePlayer.AI) {
                    // Human is non-dealer — human counts
                    Button(
                        onClick = onCountNonDealer,
                        colors = ButtonDefaults.buttonColors(containerColor = Aqua3)
                    ) {
                        Text("Count My Hand")
                    }
                } else {
                    // AI is non-dealer — auto count
                    Button(
                        onClick = onCountNonDealer,
                        colors = ButtonDefaults.buttonColors(containerColor = Dark2)
                    ) {
                        Text("Count AI Hand")
                    }
                }
            }

            CribbagePhase.SHOW_DEALER -> {
                val dealer = if (gameState.dealer == CribbagePlayer.HUMAN) "Your" else "AI"
                Text("Count $dealer hand (dealer)", color = Aqua1, fontSize = 13.sp)
                val label = if (gameState.dealer == CribbagePlayer.HUMAN) "Count My Hand (Dealer)" else "Count AI Hand (Dealer)"
                Button(
                    onClick = onCountDealer,
                    colors = ButtonDefaults.buttonColors(containerColor = Aqua3)
                ) {
                    Text(label)
                }
            }

            CribbagePhase.SHOW_CRIB -> {
                val cribOwner = if (gameState.dealer == CribbagePlayer.HUMAN) "Your" else "AI"
                Text("Count $cribOwner crib", color = Aqua1, fontSize = 13.sp)
                Button(
                    onClick = onCountCrib,
                    colors = ButtonDefaults.buttonColors(containerColor = Aqua3)
                ) {
                    Text("Count Crib")
                }
                // Show the show breakdown so far
                gameState.showBreakdown.forEach { line ->
                    Text(line, color = Aqua1, fontSize = 11.sp)
                }
            }

            else -> {}
        }
    }
}
