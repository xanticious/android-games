package com.xanticious.androidgames.view.games.scrabblechallenge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.data.WordDataProvider
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.scrabble.*
import com.xanticious.androidgames.state.games.scrabblechallenge.ChallengePhase
import com.xanticious.androidgames.state.games.scrabblechallenge.ScrabbleChallengeStateMachine
import com.xanticious.androidgames.ui.theme.*
import com.xanticious.androidgames.view.common.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Main entry composable for Scrabble Single Player Challenge. */
@Composable
fun ScrabbleSinglePlayerChallengeGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val context = LocalContext.current
    var wordData by remember { mutableStateOf<WordData?>(null) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Setup) }
    var boardDensity by remember { mutableStateOf(BoardDensity.MEDIUM) }
    var rackDifficulty by remember { mutableStateOf(RackDifficulty.BALANCED) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            wordData = WordDataProvider.get(context)
        }
    }

    if (wordData == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading dictionary...")
        }
        return
    }

    when (currentScreen) {
        Screen.Setup -> ChallengeSetup(
            density = boardDensity,
            rackDiff = rackDifficulty,
            onDensity = { boardDensity = it },
            onRackDiff = { rackDifficulty = it },
            onHowToPlay = { currentScreen = Screen.HowToPlay },
            onStart = { currentScreen = Screen.Playing }
        )
        Screen.HowToPlay -> ChallengeHowToPlay(
            onBack = { currentScreen = Screen.Setup }
        )
        Screen.Playing -> ChallengeGameplay(
            density = boardDensity,
            rackDiff = rackDifficulty,
            wordData = wordData!!,
            onExit = onExit
        )
    }
}

private enum class Screen { Setup, HowToPlay, Playing }

@Composable
private fun ChallengeSetup(
    density: BoardDensity,
    rackDiff: RackDifficulty,
    onDensity: (BoardDensity) -> Unit,
    onRackDiff: (RackDifficulty) -> Unit,
    onHowToPlay: () -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Scrabble Challenge", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        
        Text("Board Density", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BoardDensity.entries.forEach { d ->
                FilterChip(
                    selected = density == d,
                    onClick = { onDensity(d) },
                    label = { Text(d.name) }
                )
            }
        }
        
        Text("Rack Difficulty", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RackDifficulty.entries.forEach { r ->
                FilterChip(
                    selected = rackDiff == r,
                    onClick = { onRackDiff(r) },
                    label = { Text(r.name) }
                )
            }
        }
        
        Text("Play 10 rounds. Each round: find the best word you can.", style = MaterialTheme.typography.bodyMedium)
        
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onHowToPlay) { Text("How to Play") }
            Button(onClick = onStart) { Text("Start Session") }
        }
    }
}

@Composable
private fun ChallengeHowToPlay(onBack: () -> Unit) {
    WordGameHowToPlay(
        title = "Scrabble Challenge",
        intro = "Solo puzzle mode. Find the highest-scoring word each round.",
        onBack = onBack
    ) {
        HowToPlaySection("Goal") {
            Text("Complete 10 rounds. Each round, play the best word you can find.")
        }
        HowToPlaySection("Gameplay") {
            Text("• Each round has a random board and random rack")
            Text("• Place tiles to form one legal word")
            Text("• After playing, see the top 20 best possible plays")
            Text("• Your session total is the sum of all 10 rounds")
        }
        HowToPlaySection("Settings") {
            Text("Board Density: how many tiles are pre-placed")
            Text("Rack Difficulty: Balanced vs Spicy (awkward high-value letters)")
        }
    }
}

@Composable
private fun ChallengeGameplay(
    density: BoardDensity,
    rackDiff: RackDifficulty,
    wordData: WordData,
    onExit: () -> Unit
) {
    val machine = remember { ScrabbleChallengeStateMachine() }
    val phase by machine.phase.collectAsState()
    val state by machine.state.collectAsState()

    LaunchedEffect(Unit) {
        machine.startSession(density, rackDiff, wordData, 0)
    }

    GameScaffold(
        title = "Scrabble Challenge",
        onExit = onExit,
        hud = {
            ChallengeHud(state)
        },
        status = {
            when (phase) {
                ChallengePhase.ROUND_RESULT -> RoundResultPanel(state, onNext = { 
                    if (state.roundNumber >= 10) {
                        machine.viewSummary()
                    } else {
                        machine.userNextRound()
                    }
                })
                ChallengePhase.SESSION_SUMMARY -> SessionSummaryPanel(state, onExit, onNew = { machine.newSession() })
                else -> {}
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ChallengeBoardView(
                board = state.board,
                tentativeTiles = state.tentativeTiles,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ChallengeRackView(
                rack = state.rack,
                onTileClick = { /* TODO */ },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ChallengeControls(
                enabled = phase == ChallengePhase.PLAYING,
                onPlay = { machine.userSubmitPlay() },
                onRecall = { machine.userRecallTiles() },
                onShuffle = { machine.userShuffleRack() },
                onSkip = { machine.userSkipRound() }
            )
        }
    }
}

@Composable
private fun ChallengeHud(state: ScrabbleChallengeState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Round ${state.roundNumber}/10", fontWeight = FontWeight.Bold)
        Text("Total: ${state.totalScore}", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChallengeBoardView(
    board: ScrabbleBoard,
    tentativeTiles: List<PlacedTile>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Dark1)
            .padding(4.dp)
    ) {
        Column {
            for (row in 0..14) {
                Row(modifier = Modifier.weight(1f)) {
                    for (col in 0..14) {
                        val pos = Position(row, col)
                        val tile = board.getTile(pos)
                        val tentative = tentativeTiles.find { it.position == pos }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(1.dp)
                                .background(
                                    when (board.getPremium(pos)) {
                                        PremiumSquare.TRIPLE_WORD -> Aqua5
                                        PremiumSquare.DOUBLE_WORD -> Aqua4
                                        PremiumSquare.TRIPLE_LETTER -> Aqua3
                                        PremiumSquare.DOUBLE_LETTER -> Aqua2
                                        else -> Dark2
                                    }
                                )
                                .then(
                                    if (tentative != null) Modifier.border(2.dp, Aqua3)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            (tile ?: tentative?.tile)?.let { t ->
                                Text(
                                    t.letter.toString(),
                                    color = Dark1,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeRackView(
    rack: List<ScrabbleTile>,
    onTileClick: (ScrabbleTile) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        rack.forEach { tile ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(2.dp)
                    .background(Aqua1)
                    .clickable { onTileClick(tile) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tile.letter.toString(), fontWeight = FontWeight.Bold)
                    Text(tile.value.toString(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ChallengeControls(
    enabled: Boolean,
    onPlay: () -> Unit,
    onRecall: () -> Unit,
    onShuffle: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = onPlay, enabled = enabled) { Text("Play") }
        OutlinedButton(onClick = onRecall, enabled = enabled) { Text("Recall") }
        OutlinedButton(onClick = onShuffle, enabled = enabled) { Text("Shuffle") }
        OutlinedButton(onClick = onSkip, enabled = enabled) { Text("Skip") }
    }
}

@Composable
private fun RoundResultPanel(state: ScrabbleChallengeState, onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Dark2)
            .padding(16.dp)
    ) {
        Text("Round ${state.roundNumber} Result", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Your Score: ${state.roundScore}")
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("Top 20 Plays:", fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(state.topMoves.take(20)) { move ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(move.primaryWord(state.board.withTiles(move.tiles)))
                    Text("${move.score} pts")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.roundNumber >= 10) "View Summary" else "Next Round")
        }
    }
}

@Composable
private fun SessionSummaryPanel(state: ScrabbleChallengeState, onExit: () -> Unit, onNew: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Dark2)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Session Complete!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Total Score: ${state.totalScore}", style = MaterialTheme.typography.titleLarge)
        
        if (state.totalScore > state.personalBest) {
            Text("New Personal Best!", color = Aqua4, fontWeight = FontWeight.Bold)
        } else {
            Text("Personal Best: ${state.personalBest}")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNew) { Text("New Session") }
            OutlinedButton(onClick = onExit) { Text("Exit") }
        }
    }
}
