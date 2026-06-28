package com.xanticious.androidgames.view.games.boggle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.boggle.BoggleController
import com.xanticious.androidgames.data.WordDataProvider
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.boggle.BoggleConfig
import com.xanticious.androidgames.model.games.boggle.BoggleState
import com.xanticious.androidgames.model.games.boggle.GridSize
import com.xanticious.androidgames.state.games.boggle.BogglePhase
import com.xanticious.androidgames.state.games.boggle.BoggleStateMachine
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.HowToPlaySection
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.WordGameHowToPlay
import com.xanticious.androidgames.view.common.WordGameSetup
import com.xanticious.androidgames.view.games.words.CountdownTimer
import com.xanticious.androidgames.view.games.words.CurrentEntry
import com.xanticious.androidgames.view.games.words.FoundWordsList
import com.xanticious.androidgames.view.games.words.WordBuilderActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun BoggleGame(
    difficulty: GameDifficulty,
    onExit: () -> Unit
) {
    var phase by rememberSaveable { mutableStateOf("setup") }
    var gridSize by rememberSaveable { mutableStateOf(GridSize.CLASSIC_4X4) }
    var roundDuration by rememberSaveable { mutableIntStateOf(120) }
    var minLength by rememberSaveable { mutableIntStateOf(3) }

    when (phase) {
        "setup" -> BoggleSetup(
            gridSize = gridSize,
            roundDuration = roundDuration,
            minLength = minLength,
            onGridSize = { gridSize = it },
            onRoundDuration = { roundDuration = it },
            onMinLength = { minLength = it },
            onHowToPlay = { phase = "howtoplay" },
            onStart = { phase = "playing" }
        )
        "howtoplay" -> BoggleHowToPlay(onBack = { phase = "setup" })
        "playing" -> BoggleGameplay(
            config = BoggleConfig(
                gridSize = gridSize,
                roundDuration = roundDuration,
                minLength = minLength
            ),
            onExit = { phase = "setup" }
        )
    }
}

@Composable
private fun BoggleSetup(
    gridSize: GridSize,
    roundDuration: Int,
    minLength: Int,
    onGridSize: (GridSize) -> Unit,
    onRoundDuration: (Int) -> Unit,
    onMinLength: (Int) -> Unit,
    onHowToPlay: () -> Unit,
    onStart: () -> Unit
) {
    WordGameSetup(
        title = "Boggle",
        difficulty = GameDifficulty.MEDIUM,
        onDifficulty = {},
        onHowToPlay = onHowToPlay,
        onStart = onStart
    ) {
        Text("Game Mode", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            GridSize.entries.forEach { size ->
                OutlinedButton(onClick = { onGridSize(size) }) {
                    Text(if (gridSize == size) "✓ ${size.label}" else size.label)
                }
            }
        }
        
        Text("Round Duration", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(90, 120, 180).forEach { duration ->
                OutlinedButton(onClick = { onRoundDuration(duration) }) {
                    Text(if (roundDuration == duration) "✓ ${duration}s" else "${duration}s")
                }
            }
        }
        
        Text("Minimum word length", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(3, 4).forEach { len ->
                OutlinedButton(onClick = { onMinLength(len) }) {
                    Text(if (minLength == len) "✓ $len letters" else "$len letters")
                }
            }
        }
    }
}

@Composable
private fun BoggleHowToPlay(onBack: () -> Unit) {
    WordGameHowToPlay(
        title = "How to Play Boggle",
        intro = "Trace adjacent letters to find words in the grid. Race against the clock!",
        onBack = onBack
    ) {
        HowToPlaySection(title = "Goal") {
            Text("Tap adjacent cells (including diagonals) to trace words. Each cell can only be used once per word.")
        }
        HowToPlaySection(title = "Controls") {
            Text("• Tap cells to build a path\n• Submit to score the word\n• Submit & Keep to keep your path\n• Backspace to undo the last cell\n• Give Up to end early")
        }
        HowToPlaySection(title = "Scoring") {
            Text("Longer words score more points. The results screen shows all possible words!")
        }
    }
}

@Composable
private fun BoggleGameplay(
    config: BoggleConfig,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    var wordData by remember { mutableStateOf<com.xanticious.androidgames.controller.words.WordData?>(null) }
    var controller by remember { mutableStateOf<BoggleController?>(null) }
    val stateMachine = remember { BoggleStateMachine() }
    val phase by stateMachine.phase.collectAsState()
    var gameState by remember { mutableStateOf(BoggleState(timeRemaining = config.roundDuration)) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val data = WordDataProvider.get(context)
            wordData = data
            controller = BoggleController(data)
        }
    }

    LaunchedEffect(controller, config) {
        if (controller != null && phase == BogglePhase.IDLE) {
            val (grid, possibleWords) = controller!!.generateRound(config)
            gameState = BoggleState(
                grid = grid,
                minLength = config.minLength,
                timeRemaining = config.roundDuration,
                allPossibleWords = possibleWords
            )
            stateMachine.roundStarted()
        }
    }

    LaunchedEffect(phase) {
        if (phase == BogglePhase.PLAYING) {
            while (gameState.timeRemaining > 0) {
                delay(1000L)
                gameState = gameState.copy(timeRemaining = gameState.timeRemaining - 1)
                if (gameState.timeRemaining == 0) {
                    stateMachine.timeExpired()
                }
            }
        }
    }

    if (wordData == null || controller == null) {
        GameScaffold(title = "Boggle", onExit = onExit) {
            Text("Loading…")
        }
        return
    }

    when (phase) {
        BogglePhase.IDLE -> {
            GameScaffold(title = "Boggle", onExit = onExit) {
                Text("Preparing…")
            }
        }
        BogglePhase.PLAYING -> {
            BogglePlayingScreen(
                state = gameState,
                controller = controller!!,
                onCellTap = { row, col ->
                    val pos = row to col
                    if (gameState.currentPath.isEmpty()) {
                        gameState = gameState.copy(
                            currentEntry = gameState.grid[row][col].toString(),
                            currentPath = listOf(pos)
                        )
                        stateMachine.cellTapped()
                    } else if (pos !in gameState.currentPath) {
                        val lastPos = gameState.currentPath.last()
                        if (controller!!.isAdjacent(lastPos, pos)) {
                            gameState = gameState.copy(
                                currentEntry = gameState.currentEntry + gameState.grid[row][col],
                                currentPath = gameState.currentPath + pos
                            )
                            stateMachine.cellTapped()
                        }
                    }
                },
                onBackspace = {
                    if (gameState.currentPath.isNotEmpty()) {
                        gameState = gameState.copy(
                            currentEntry = gameState.currentEntry.dropLast(1),
                            currentPath = gameState.currentPath.dropLast(1)
                        )
                        stateMachine.entryBackspaced()
                    }
                },
                onSubmit = {
                    val word = gameState.currentEntry.lowercase()
                    if (word.isEmpty()) return@BogglePlayingScreen
                    
                    when {
                        word in gameState.foundWords -> {
                            gameState = gameState.copy(message = "Already found!")
                        }
                        !controller!!.isValidPath(gameState.currentPath) -> {
                            gameState = gameState.copy(message = "Invalid path!")
                        }
                        !wordData!!.isValidWord(word) -> {
                            gameState = gameState.copy(message = "Not a valid word!")
                        }
                        word.length < gameState.minLength -> {
                            gameState = gameState.copy(message = "Too short!")
                        }
                        else -> {
                            val points = controller!!.scoreWord(word)
                            gameState = gameState.copy(
                                foundWords = gameState.foundWords + word,
                                score = gameState.score + points,
                                currentEntry = "",
                                currentPath = emptyList(),
                                message = "+$points"
                            )
                            stateMachine.wordSubmitted()
                        }
                    }
                },
                onSubmitAndKeep = {
                    val word = gameState.currentEntry.lowercase()
                    if (word.isEmpty()) return@BogglePlayingScreen
                    
                    when {
                        word in gameState.foundWords -> {
                            gameState = gameState.copy(message = "Already found!")
                        }
                        !controller!!.isValidPath(gameState.currentPath) -> {
                            gameState = gameState.copy(message = "Invalid path!")
                        }
                        !wordData!!.isValidWord(word) -> {
                            gameState = gameState.copy(message = "Not a valid word!")
                        }
                        word.length < gameState.minLength -> {
                            gameState = gameState.copy(message = "Too short!")
                        }
                        else -> {
                            val points = controller!!.scoreWord(word)
                            gameState = gameState.copy(
                                foundWords = gameState.foundWords + word,
                                score = gameState.score + points,
                                message = "+$points"
                            )
                            stateMachine.wordSubmitted()
                        }
                    }
                },
                onGiveUp = {
                    stateMachine.gaveUp()
                },
                onExit = onExit
            )
        }
        BogglePhase.ROUND_OVER -> {
            BoggleResultsScreen(
                state = gameState,
                onNewRound = {
                    val (grid, possibleWords) = controller!!.generateRound(config)
                    gameState = BoggleState(
                        grid = grid,
                        minLength = config.minLength,
                        timeRemaining = config.roundDuration,
                        allPossibleWords = possibleWords
                    )
                    stateMachine.newRound()
                    stateMachine.roundStarted()
                },
                onExit = onExit
            )
        }
    }
}

@Composable
private fun BogglePlayingScreen(
    state: BoggleState,
    controller: BoggleController,
    onCellTap: (Int, Int) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onSubmitAndKeep: () -> Unit,
    onGiveUp: () -> Unit,
    onExit: () -> Unit
) {
    GameScaffold(
        title = "Boggle",
        onExit = onExit,
        hud = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Words ${state.foundWords.size}", fontWeight = FontWeight.Bold)
                CountdownTimer(state.timeRemaining)
                Text("Score ${state.score}", fontWeight = FontWeight.Bold)
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BoggleGrid(
                grid = state.grid,
                currentPath = state.currentPath,
                onCellTap = onCellTap
            )
            
            CurrentEntry(entry = state.currentEntry)
            
            WordBuilderActions(
                onBackspace = onBackspace,
                onSubmit = onSubmit,
                onSubmitAndKeep = onSubmitAndKeep,
                onGiveUp = onGiveUp,
                canBackspace = state.currentEntry.isNotEmpty(),
                canSubmit = state.currentEntry.length >= state.minLength
            )
            
            if (state.message.isNotEmpty()) {
                Text(state.message, fontWeight = FontWeight.Bold)
            }
            
            if (state.foundWords.isNotEmpty()) {
                FoundWordsList(foundWords = state.foundWords.toList())
            }
        }
    }
}

@Composable
private fun BoggleGrid(
    grid: List<List<Char>>,
    currentPath: List<Pair<Int, Int>>,
    onCellTap: (Int, Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        grid.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEachIndexed { colIndex, letter ->
                    val pos = rowIndex to colIndex
                    val isInPath = pos in currentPath
                    val isLast = currentPath.lastOrNull() == pos
                    BoggleCell(
                        letter = letter,
                        isInPath = isInPath,
                        isLast = isLast,
                        onClick = { onCellTap(rowIndex, colIndex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BoggleCell(
    letter: Char,
    isInPath: Boolean,
    isLast: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .aspectRatio(1f)
            .background(
                color = when {
                    isLast -> Aqua3
                    isInPath -> Aqua2
                    else -> MaterialTheme.colorScheme.surface
                },
                shape = MaterialTheme.shapes.small
            )
            .border(
                width = if (isInPath) 2.dp else 1.dp,
                color = if (isInPath) Aqua3 else MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.small
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.uppercase(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Dark1
        )
    }
}

@Composable
private fun BoggleResultsScreen(
    state: BoggleState,
    onNewRound: () -> Unit,
    onExit: () -> Unit
) {
    GameScaffold(
        title = "Boggle",
        onExit = onExit,
        status = {
            VictoryPanel(
                score = state.score,
                bestScore = state.score,
                stars = when {
                    state.foundWords.size >= state.allPossibleWords.size * 0.75 -> 3
                    state.foundWords.size >= state.allPossibleWords.size * 0.5 -> 2
                    else -> 1
                },
                onReplay = onNewRound,
                onMenu = onExit,
                headline = if (state.timeRemaining == 0) "Time's Up!" else "Round Over",
                primaryLabel = "New Round"
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Final Score: ${state.score}", fontWeight = FontWeight.Bold)
            Text("Found ${state.foundWords.size} / ${state.allPossibleWords.size} words")
            
            if (state.foundWords.isNotEmpty()) {
                Text("Your Words:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                FoundWordsList(foundWords = state.foundWords.toList())
            }
            
            Text("All Possible Words:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text(
                text = state.allPossibleWords.sorted().joinToString(", ") { it.uppercase() },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
