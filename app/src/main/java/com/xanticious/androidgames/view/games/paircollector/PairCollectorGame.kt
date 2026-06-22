package com.xanticious.androidgames.view.games.paircollector

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.paircollector.PairCollectorController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.paircollector.CardBackStyle
import com.xanticious.androidgames.model.games.paircollector.PairCollectorGameState
import com.xanticious.androidgames.model.games.paircollector.PairCollectorOutcome
import com.xanticious.androidgames.model.games.paircollector.PairCollectorSettings
import com.xanticious.androidgames.model.games.paircollector.PairCollectorTimeLimit
import com.xanticious.androidgames.model.games.paircollector.PlayingCard
import com.xanticious.androidgames.state.games.paircollector.PairCollectorPhase
import com.xanticious.androidgames.state.games.paircollector.PairCollectorStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlinx.coroutines.delay
import kotlin.random.Random

private enum class PairSetupScreen { SETTINGS, HOW_TO_PLAY, GAME }

@Composable
fun PairCollectorGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { PairCollectorController() }
    val defaultConfig = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { PairCollectorStateMachine() }
    val phase by machine.phase.collectAsState()
    val random = remember { Random(System.currentTimeMillis()) }

    var setupScreen by rememberSaveable { mutableStateOf(PairSetupScreen.SETTINGS) }
    var cardCount by rememberSaveable { mutableStateOf(defaultConfig.cardCount) }
    var timeLimit by rememberSaveable { mutableStateOf(defaultConfig.timeLimit) }
    var cardBackStyle by rememberSaveable { mutableStateOf(defaultConfig.cardBackStyle) }
    var showRoundTimer by rememberSaveable { mutableStateOf(defaultConfig.showRoundTimer) }
    var bestTime by rememberSaveable { mutableStateOf(0f) }
    var bestRound by rememberSaveable { mutableStateOf(0) }
    var gameState by remember { mutableStateOf<PairCollectorGameState?>(null) }

    val settings = PairCollectorSettings(cardCount, timeLimit, cardBackStyle, showRoundTimer)
    val config = remember(settings) { controller.configFor(settings) }

    fun startGame() {
        val round = controller.generateRound(config.cardCount, null, random)
        gameState = PairCollectorGameState.initial(round)
        setupScreen = PairSetupScreen.GAME
        if (phase == PairCollectorPhase.GAME_OVER) machine.rematch() else machine.startGame()
        machine.roundReady()
    }

    LaunchedEffect(phase, gameState?.outcome) {
        if (phase == PairCollectorPhase.ROUND_COMPLETE) {
            delay(350L)
            val state = gameState ?: return@LaunchedEffect
            if (state.outcome == PairCollectorOutcome.VICTORY) {
                bestRound = config.totalRounds
                bestTime = if (bestTime <= 0f) state.totalElapsedSeconds else minOf(bestTime, state.totalElapsedSeconds)
                machine.allRoundsComplete()
            } else {
                machine.moreRoundsRemaining()
                machine.roundReady()
            }
        }
    }

    GameLoop(running = setupScreen == PairSetupScreen.GAME && phase == PairCollectorPhase.PLAYING_ROUND) { dt ->
        val state = gameState ?: return@GameLoop
        val timed = controller.advanceTime(state, dt)
        if (controller.hasRoundTimedOut(timed, config)) {
            val struck = controller.addStrike(timed, config)
            if (struck.outcome == PairCollectorOutcome.DEFEAT) {
                bestRound = maxOf(bestRound, struck.completedRounds + 1)
                gameState = struck
                machine.strikesExhausted()
            } else {
                gameState = struck.copy(
                    currentRound = controller.generateRound(config.cardCount, struck.previousDuplicate, random),
                    roundElapsedSeconds = 0f,
                    selectedCardIndex = PairCollectorGameState.NO_SELECTION
                )
                machine.roundTimeExpired()
            }
        } else {
            gameState = timed
        }
    }

    when (setupScreen) {
        PairSetupScreen.SETTINGS -> PairCollectorSettingsScreen(
            cardCount = cardCount,
            timeLimit = timeLimit,
            cardBackStyle = cardBackStyle,
            showRoundTimer = showRoundTimer,
            onCardCount = { cardCount = it },
            onTimeLimit = { timeLimit = it },
            onCardBackStyle = { cardBackStyle = it },
            onShowRoundTimer = { showRoundTimer = it },
            onHowToPlay = { setupScreen = PairSetupScreen.HOW_TO_PLAY },
            onStart = ::startGame,
            onExit = onExit
        )
        PairSetupScreen.HOW_TO_PLAY -> PairCollectorHowToPlay(onBack = { setupScreen = PairSetupScreen.SETTINGS }, onStart = ::startGame, onExit = onExit)
        PairSetupScreen.GAME -> PairCollectorBoard(
            state = gameState,
            phase = phase,
            configTotalRounds = config.totalRounds,
            bestTime = bestTime,
            bestRound = bestRound,
            cardBackLabel = controller.cardBackLabel(config.cardBackStyle),
            showRoundTimer = config.showRoundTimer,
            onTapCard = { index ->
                val state = gameState
                if (state != null) {
                    val selected = state.selectedCardIndex
                    when {
                    selected == PairCollectorGameState.NO_SELECTION -> {
                        gameState = controller.selectCard(state, index)
                        machine.firstCardTapped()
                    }
                    selected == index -> {
                        gameState = controller.selectCard(state, index)
                        machine.cardDeselected()
                    }
                    controller.isDuplicatePair(state.currentRound, selected, index) -> {
                        val next = controller.generateRound(config.cardCount, state.currentRound.duplicate, random)
                        gameState = controller.completeRound(state, config, next)
                        machine.secondCardMatched()
                    }
                    else -> {
                        val struck = controller.addStrike(state, config)
                        gameState = struck
                        if (struck.outcome == PairCollectorOutcome.DEFEAT) {
                            bestRound = maxOf(bestRound, struck.completedRounds + 1)
                            machine.strikesExhausted()
                        } else {
                            machine.secondCardMismatched()
                        }
                    }
                }
                }
            },
            onRematch = ::startGame,
            onExit = onExit
        )
    }
}

@Composable
private fun PairCollectorSettingsScreen(
    cardCount: Int,
    timeLimit: PairCollectorTimeLimit,
    cardBackStyle: CardBackStyle,
    showRoundTimer: Boolean,
    onCardCount: (Int) -> Unit,
    onTimeLimit: (PairCollectorTimeLimit) -> Unit,
    onCardBackStyle: (CardBackStyle) -> Unit,
    onShowRoundTimer: (Boolean) -> Unit,
    onHowToPlay: () -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit
) {
    GameScaffold(title = "Pair Collector", onExit = onExit) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            ChoiceRow("Card count", listOf(10, 16, 20, 30), cardCount, onCardCount) { it.toString() }
            ChoiceRow("Time limit", PairCollectorTimeLimit.entries.toList(), timeLimit, onTimeLimit) { if (it.seconds == 0) "Off" else "${it.seconds}s" }
            ChoiceRow("Card back", CardBackStyle.entries.toList(), cardBackStyle, onCardBackStyle) { it.label }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Show round timer", fontWeight = FontWeight.Bold)
                Switch(checked = showRoundTimer, onCheckedChange = onShowRoundTimer)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onHowToPlay) { Text("How to Play") }
                Button(onClick = onStart) { Text("Start") }
            }
        }
    }
}

@Composable
private fun PairCollectorHowToPlay(onBack: () -> Unit, onStart: () -> Unit, onExit: () -> Unit) {
    GameScaffold(title = "Pair Collector", onExit = onExit) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("How to Play", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("One card appears exactly twice. Tap one copy, then tap the matching copy.")
            Text("Wrong pairs add strikes. Three strikes ends the game. Complete 10 rounds to win.")
            Text("If a round time limit is enabled, timing out adds a strike and deals a fresh round.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack) { Text("Settings") }
                Button(onClick = onStart) { Text("Start") }
            }
        }
    }
}

@Composable
private fun PairCollectorBoard(
    state: PairCollectorGameState?,
    phase: PairCollectorPhase,
    configTotalRounds: Int,
    bestTime: Float,
    bestRound: Int,
    cardBackLabel: String,
    showRoundTimer: Boolean,
    onTapCard: (Int) -> Unit,
    onRematch: () -> Unit,
    onExit: () -> Unit
) {
    GameScaffold(
        title = "Pair Collector",
        onExit = onExit,
        hud = {
            val s = state
            GameHud(
                left = "R:${s?.roundNumber ?: 1}/$configTotalRounds",
                center = formatTime(s?.totalElapsedSeconds ?: 0f),
                right = "❤".repeat((3 - (s?.strikes ?: 0)).coerceAtLeast(0)) + "♡".repeat(s?.strikes ?: 0)
            )
        },
        status = {
            val s = state
            if (phase == PairCollectorPhase.GAME_OVER && s != null) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    if (s.outcome == PairCollectorOutcome.VICTORY) {
                        VictoryPanel(
                            score = s.completedRounds,
                            bestScore = if (bestRound == 0) s.completedRounds else bestRound,
                            stars = if (s.strikes == 0) 3 else 2,
                            onReplay = onRematch,
                            onMenu = onExit,
                            headline = "All pairs found!",
                            primaryLabel = "Rematch"
                        )
                        Text("Time ${formatTime(s.totalElapsedSeconds)}  Best ${formatTime(if (bestTime <= 0f) s.totalElapsedSeconds else bestTime)}")
                    } else {
                        DefeatPanel(
                            score = s.completedRounds,
                            bestScore = bestRound,
                            onTryAgain = onRematch,
                            onMenu = onExit,
                            headline = "Three strikes"
                        )
                    }
                }
            }
        }
    ) {
        val s = state ?: return@GameScaffold
        Surface(modifier = Modifier.fillMaxSize(), color = Dark1) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showRoundTimer) Text("Round ${formatTime(s.roundElapsedSeconds)}", color = Aqua0, fontWeight = FontWeight.Bold)
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val columns = when {
                        maxWidth < 420.dp -> 4
                        maxWidth < 700.dp -> 5
                        else -> 7
                    }
                    val cardWidth = (maxWidth - 8.dp * (columns - 1)) / columns
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        s.currentRound.cards.chunked(columns).forEachIndexed { row, cards ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                cards.forEachIndexed { column, card ->
                                    val index = row * columns + column
                                    PlayingCardView(
                                        card = card,
                                        width = cardWidth,
                                        selected = s.selectedCardIndex == index,
                                        cardBackLabel = cardBackLabel,
                                        onClick = { onTapCard(index) }
                                    )
                                }
                            }
                        }
                    }
                }
                if (s.roundNumber == 1 && phase == PairCollectorPhase.PLAYING_ROUND) {
                    Text("Find the duplicate card", color = Aqua1, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PlayingCardView(card: PlayingCard, width: androidx.compose.ui.unit.Dp, selected: Boolean, cardBackLabel: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(width).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Aqua0),
        border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) Aqua3 else GameAccent)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(card.label, color = if (card.suit.isRed) GameEnemy else Dark0, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(cardBackLabel, color = GameAccent, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun <T> ChoiceRow(label: String, options: List<T>, selected: T, onSelected: (T) -> Unit, text: (T) -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                if (option == selected) {
                    Button(onClick = { onSelected(option) }) { Text(text(option)) }
                } else {
                    OutlinedButton(onClick = { onSelected(option) }) { Text(text(option)) }
                }
            }
        }
    }
}

private fun formatTime(seconds: Float): String {
    val total = seconds.toInt().coerceAtLeast(0)
    val minutes = total / 60
    val sec = total % 60
    return "$minutes:${sec.toString().padStart(2, '0')}"
}
