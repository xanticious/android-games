package com.xanticious.androidgames.view.games.scrabble

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
import com.xanticious.androidgames.state.games.scrabble.ScrabblePhase
import com.xanticious.androidgames.state.games.scrabble.ScrabbleStateMachine
import com.xanticious.androidgames.ui.theme.*
import com.xanticious.androidgames.view.common.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Main entry composable for Scrabble game. */
@Composable
fun ScrabbleGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val context = LocalContext.current
    var wordData by remember { mutableStateOf<WordData?>(null) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Setup) }
    var selectedDifficulty by remember { mutableStateOf(ScrabbleDifficulty.MEDIUM) }

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
        Screen.Setup -> ScrabbleSetup(
            difficulty = selectedDifficulty,
            onDifficulty = { selectedDifficulty = it },
            onHowToPlay = { currentScreen = Screen.HowToPlay },
            onStart = { currentScreen = Screen.Playing }
        )
        Screen.HowToPlay -> ScrabbleHowToPlay(
            onBack = { currentScreen = Screen.Setup }
        )
        Screen.Playing -> ScrabbleGameplay(
            difficulty = selectedDifficulty,
            wordData = wordData!!,
            onExit = onExit
        )
    }
}

private enum class Screen { Setup, HowToPlay, Playing }

@Composable
private fun ScrabbleSetup(
    difficulty: ScrabbleDifficulty,
    onDifficulty: (ScrabbleDifficulty) -> Unit,
    onHowToPlay: () -> Unit,
    onStart: () -> Unit
) {
    WordGameSetup(
        title = "Scrabble",
        difficulty = when (difficulty) {
            ScrabbleDifficulty.EASY -> GameDifficulty.EASY
            ScrabbleDifficulty.MEDIUM -> GameDifficulty.MEDIUM
            ScrabbleDifficulty.HARD -> GameDifficulty.HARD
        },
        onDifficulty = {
            onDifficulty(when (it) {
                GameDifficulty.EASY -> ScrabbleDifficulty.EASY
                GameDifficulty.MEDIUM -> ScrabbleDifficulty.MEDIUM
                GameDifficulty.HARD -> ScrabbleDifficulty.HARD
            })
        },
        onHowToPlay = onHowToPlay,
        onStart = onStart
    ) {
        Text("AI Opponent Difficulty:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Text("Easy: Plays simple legal moves", style = MaterialTheme.typography.bodySmall)
        Text("Medium: Evaluates good scoring plays", style = MaterialTheme.typography.bodySmall)
        Text("Hard: Near-optimal strategic play", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ScrabbleHowToPlay(onBack: () -> Unit) {
    WordGameHowToPlay(
        title = "How to Play Scrabble",
        intro = "Classic tile-placement word game. Play words on a 15×15 board for points.",
        onBack = onBack
    ) {
        HowToPlaySection("Goal") {
            Text("Score more points than your AI opponent by placing valid words on the board.")
        }
        HowToPlaySection("Gameplay") {
            Text("• Each player keeps a rack of 7 tiles")
            Text("• On your turn, place tiles to form a word that connects to existing tiles")
            Text("• First move must cross the center square")
            Text("• All formed words must be valid dictionary words")
        }
        HowToPlaySection("Scoring") {
            Text("• Tiles have point values (A=1, Q=10, etc.)")
            Text("• Premium squares multiply letter or word scores")
            Text("• Using all 7 tiles in one turn: +50 bonus (bingo)")
        }
    }
}

@Composable
private fun ScrabbleGameplay(
    difficulty: ScrabbleDifficulty,
    wordData: WordData,
    onExit: () -> Unit
) {
    val machine = remember { ScrabbleStateMachine() }
    val phase by machine.phase.collectAsState()
    val gameState by machine.gameState.collectAsState()

    LaunchedEffect(Unit) {
        machine.startGame(difficulty, wordData)
    }

    GameScaffold(
        title = "Scrabble",
        onExit = onExit,
        hud = {
            ScrabbleHud(gameState)
        },
        status = {
            if (phase == ScrabblePhase.GAME_OVER) {
                ScrabbleResultPanel(gameState, onExit = onExit, onRematch = { machine.rematch() })
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScrabbleBoardView(
                board = gameState.board,
                tentativeTiles = gameState.tentativeTiles,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ScrabbleRackView(
                rack = gameState.playerRack,
                onTileClick = { /* TODO: tile placement */ },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ScrabbleControls(
                enabled = phase == ScrabblePhase.PLAYING && gameState.currentTurn == ScrabbleGameState.Player.HUMAN,
                onPlay = { machine.userSubmitPlay() },
                onRecall = { machine.userRecallTiles() },
                onShuffle = { machine.userShuffleRack() },
                onPass = { machine.pass() }
            )
        }
    }
}

@Composable
private fun ScrabbleHud(state: ScrabbleGameState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("You: ${state.playerScore}", fontWeight = FontWeight.Bold)
        Text("Bag: ${state.bag.size}")
        Text("AI: ${state.aiScore}", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ScrabbleBoardView(
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
                                    fontWeight = FontWeight.Bold
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
private fun ScrabbleRackView(
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
private fun ScrabbleControls(
    enabled: Boolean,
    onPlay: () -> Unit,
    onRecall: () -> Unit,
    onShuffle: () -> Unit,
    onPass: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = onPlay, enabled = enabled) { Text("Play") }
        OutlinedButton(onClick = onRecall, enabled = enabled) { Text("Recall") }
        OutlinedButton(onClick = onShuffle, enabled = enabled) { Text("Shuffle") }
        OutlinedButton(onClick = onPass, enabled = enabled) { Text("Pass") }
    }
}

@Composable
private fun ScrabbleResultPanel(
    state: ScrabbleGameState,
    onExit: () -> Unit,
    onRematch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Dark2)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (state.winner == ScrabbleGameState.Player.HUMAN) "You Win!" else "AI Wins!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text("Final Score: ${state.playerScore} - ${state.aiScore}")
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRematch) { Text("Rematch") }
            OutlinedButton(onClick = onExit) { Text("Exit") }
        }
    }
}
