package com.xanticious.androidgames.view.games.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.memory.MemoryController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.memory.MemoryCard
import com.xanticious.androidgames.model.games.memory.MemoryOutcome
import com.xanticious.androidgames.model.games.memory.MemoryPlayer
import com.xanticious.androidgames.model.games.memory.MemoryResolve
import com.xanticious.androidgames.state.games.memory.MemoryPhase
import com.xanticious.androidgames.state.games.memory.MemoryStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun MemoryGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { MemoryController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { MemoryStateMachine() }
    val phase by machine.phase.collectAsState()

    var dealSeed by rememberSaveable(difficulty) { mutableIntStateOf(2026) }
    var state by remember(difficulty, dealSeed) { mutableStateOf(controller.deal(config, dealSeed)) }

    fun reset() {
        dealSeed += 1
        machine.rematch()
    }

    LaunchedEffect(phase, state.flippedIndices, state.moves) {
        if (phase != MemoryPhase.RESOLVING || state.flippedIndices.size != 2) return@LaunchedEffect
        delay(500)
        val resolved = controller.resolveFlipped(state)
        state = resolved.state
        when (resolved.resolve) {
            MemoryResolve.GAME_OVER -> machine.gameFinished()
            MemoryResolve.MATCH -> {
                if (resolved.state.currentPlayer == MemoryPlayer.AI) machine.aiTurnStarted() else machine.humanTurnStarted()
            }
            MemoryResolve.MISS -> {
                delay(700)
                val hidden = controller.hideUnmatched(resolved.state)
                state = hidden
                if (hidden.currentPlayer == MemoryPlayer.AI) machine.aiTurnStarted() else machine.humanTurnStarted()
            }
            MemoryResolve.NONE -> Unit
        }
    }

    LaunchedEffect(phase, state.turnNumber, state.moves) {
        if (phase != MemoryPhase.AI_TURN || state.result != MemoryOutcome.IN_PROGRESS) return@LaunchedEffect
        val choice = controller.aiChooseCards(
            state = state,
            memoryAccuracy = config.memoryAccuracy,
            random = Random(dealSeed + state.turnNumber + state.moves)
        ) ?: return@LaunchedEffect
        delay(450)
        state = controller.flipCard(state, choice.first)
        delay(450)
        state = controller.flipCard(state, choice.second)
        machine.aiCardsChosen()
    }

    GameScaffold(
        title = "Memory",
        onExit = onExit,
        hud = {
            GameHud(
                left = "You ${state.humanPairs}",
                center = "${config.rows}×${config.columns}  ${difficulty.label}",
                right = "AI ${state.aiPairs}"
            )
        },
        status = {
            MemoryStatus(
                phase = phase,
                result = state.result,
                moves = state.moves,
                humanPairs = state.humanPairs,
                aiPairs = state.aiPairs,
                onReplay = ::reset,
                onMenu = onExit
            )
        }
    ) {
        MemoryBoard(
            cards = state.cards,
            rows = config.rows,
            columns = config.columns,
            canTap = phase == MemoryPhase.FIRST_FLIP || phase == MemoryPhase.SECOND_FLIP,
            onTap = { index ->
                if (state.currentPlayer == MemoryPlayer.HUMAN) {
                    val next = controller.flipCard(state, index)
                    if (next != state) {
                        state = next
                        if (next.flippedIndices.size == 1) {
                            machine.firstCardFlipped()
                        } else if (next.flippedIndices.size == 2) {
                            machine.secondCardFlipped()
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun MemoryStatus(
    phase: MemoryPhase,
    result: MemoryOutcome,
    moves: Int,
    humanPairs: Int,
    aiPairs: Int,
    onReplay: () -> Unit,
    onMenu: () -> Unit
) {
    if (phase == MemoryPhase.GAME_OVER) {
        when (result) {
            MemoryOutcome.HUMAN_WIN -> VictoryPanel(
                score = humanPairs,
                bestScore = humanPairs,
                stars = 3,
                onReplay = onReplay,
                onMenu = onMenu,
                headline = "Memory Won!",
                primaryLabel = "Rematch"
            )
            MemoryOutcome.AI_WIN -> DefeatPanel(
                score = aiPairs,
                bestScore = aiPairs,
                onTryAgain = onReplay,
                onMenu = onMenu,
                headline = "Memory Lost"
            )
            MemoryOutcome.TIE -> VictoryPanel(
                score = humanPairs,
                bestScore = humanPairs,
                stars = 1,
                onReplay = onReplay,
                onMenu = onMenu,
                headline = "Tie Game",
                primaryLabel = "Rematch"
            )
            MemoryOutcome.IN_PROGRESS -> Unit
        }
    } else {
        val prompt = when (phase) {
            MemoryPhase.FIRST_FLIP -> "Your turn. Flip a first card."
            MemoryPhase.SECOND_FLIP -> "Choose a second card."
            MemoryPhase.RESOLVING -> "Checking the pair..."
            MemoryPhase.AI_TURN -> "AI is remembering cards..."
            MemoryPhase.GAME_OVER -> ""
        }
        Text(
            text = "$prompt  Moves: $moves",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MemoryBoard(
    cards: List<MemoryCard>,
    rows: Int,
    columns: Int,
    canTap: Boolean,
    onTap: (Int) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(GameCourt).padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(rows) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(columns) { column ->
                        val index = row * columns + column
                        val card = cards.getOrNull(index)
                        if (card == null) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            MemoryCardFace(
                                card = card,
                                enabled = canTap && !card.faceUp && !card.matched,
                                modifier = Modifier.weight(1f).aspectRatio(1f),
                                onTap = { onTap(index) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryCardFace(
    card: MemoryCard,
    enabled: Boolean,
    modifier: Modifier,
    onTap: () -> Unit
) {
    val faceVisible = card.faceUp || card.matched
    val ownerColor = when (card.matchedBy) {
        MemoryPlayer.HUMAN -> GamePlayer
        MemoryPlayer.AI -> GameEnemy
        null -> GameCourtLine
    }
    Surface(
        modifier = modifier
            .alpha(if (card.matched) 0.55f else 1f)
            .clickable(enabled = enabled, onClick = onTap),
        shape = RoundedCornerShape(10.dp),
        color = if (faceVisible) Aqua0 else Dark2,
        tonalElevation = if (faceVisible) 4.dp else 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(if (faceVisible) Aqua0 else Dark1),
            contentAlignment = Alignment.Center
        ) {
            if (faceVisible) {
                Text(
                    text = symbolFor(card.symbolId),
                    color = symbolColor(card.symbolId),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "◇",
                    color = Aqua3,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (card.matched) {
                Text(
                    text = "✓",
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                    color = ownerColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun symbolFor(symbolId: Int): String {
    val symbols = listOf(
        "△", "○", "□", "◇", "★", "✚", "⬟", "A", "B",
        "C", "D", "E", "F", "G", "H", "I", "J", "K"
    )
    return symbols[symbolId % symbols.size]
}

@Composable
private fun symbolColor(symbolId: Int): Color {
    val colors = listOf(Aqua2, Aqua3, Aqua4, GameAccent, GamePlayer, GameEnemy, Aqua1)
    return colors[symbolId % colors.size]
}
