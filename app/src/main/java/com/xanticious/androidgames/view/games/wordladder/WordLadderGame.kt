package com.xanticious.androidgames.view.games.wordladder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.wordladder.WordLadderController
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.data.WordDataProvider
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.wordladder.WordLadderConfig
import com.xanticious.androidgames.model.games.wordladder.WordLadderInputMode
import com.xanticious.androidgames.model.games.wordladder.WordLadderPuzzle
import com.xanticious.androidgames.model.games.wordladder.WordLadderState
import com.xanticious.androidgames.model.games.wordladder.WordLadderValidationResult
import com.xanticious.androidgames.state.games.wordladder.WordLadderPhase
import com.xanticious.androidgames.state.games.wordladder.WordLadderStateMachine
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.HowToPlaySection
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.WordGameHowToPlay
import com.xanticious.androidgames.view.common.WordGameSetup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WordLadderGame(
    difficulty: GameDifficulty,
    onExit: () -> Unit
) {
    val controller = remember { WordLadderController() }
    val stateMachine = remember { WordLadderStateMachine() }
    val phase by stateMachine.phase.collectAsState()
    
    var config by rememberSaveable(stateSaver = configSaver()) {
        mutableStateOf(WordLadderConfig(wordLength = 4, difficulty = difficulty))
    }
    var inputMode by rememberSaveable { mutableStateOf(WordLadderInputMode.ON_SCREEN_KEYBOARD) }
    
    when (phase) {
        WordLadderPhase.SETUP -> WordLadderSetup(
            config = config,
            inputMode = inputMode,
            onConfigChange = { config = it },
            onInputModeChange = { inputMode = it },
            onHowToPlay = { stateMachine.showHowToPlay() },
            onStart = { stateMachine.startGame() }
        )
        WordLadderPhase.HOW_TO_PLAY -> WordLadderHowToPlay(
            onBack = { stateMachine.backToSetup() }
        )
        WordLadderPhase.PLAYING, WordLadderPhase.SOLVED -> {
            val context = LocalContext.current
            var wordData by remember { mutableStateOf<WordData?>(null) }
            
            LaunchedEffect(Unit) {
                if (wordData == null) {
                    wordData = withContext(Dispatchers.Default) {
                        WordDataProvider.get(context)
                    }
                }
            }
            
            wordData?.let { wd ->
                WordLadderGameplay(
                    config = config,
                    inputMode = inputMode,
                    controller = controller,
                    wordData = wd,
                    isSolved = phase == WordLadderPhase.SOLVED,
                    onSolved = { stateMachine.targetReached() },
                    onNewPuzzle = { stateMachine.newPuzzle() },
                    onExit = { stateMachine.exit(); onExit() }
                )
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading word data...")
            }
        }
    }
}

@Composable
private fun WordLadderSetup(
    config: WordLadderConfig,
    inputMode: WordLadderInputMode,
    onConfigChange: (WordLadderConfig) -> Unit,
    onInputModeChange: (WordLadderInputMode) -> Unit,
    onHowToPlay: () -> Unit,
    onStart: () -> Unit
) {
    WordGameSetup(
        title = "Word Ladder",
        difficulty = config.difficulty,
        onDifficulty = { onConfigChange(config.copy(difficulty = it)) },
        onHowToPlay = onHowToPlay,
        onStart = onStart
    ) {
        Text("Word Length", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(3, 4, 5).forEach { len ->
                FilterChip(
                    selected = config.wordLength == len,
                    onClick = { onConfigChange(config.copy(wordLength = len)) },
                    label = { Text("$len letters") }
                )
            }
        }
        
        Text("Input Mode", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = inputMode == WordLadderInputMode.FIELD,
                onClick = { onInputModeChange(WordLadderInputMode.FIELD) },
                label = { Text("Keyboard") }
            )
            FilterChip(
                selected = inputMode == WordLadderInputMode.ON_SCREEN_KEYBOARD,
                onClick = { onInputModeChange(WordLadderInputMode.ON_SCREEN_KEYBOARD) },
                label = { Text("On-screen") }
            )
        }
    }
}

@Composable
private fun WordLadderHowToPlay(onBack: () -> Unit) {
    WordGameHowToPlay(
        title = "How to Play Word Ladder",
        intro = "Transform the start word into the target word by changing one letter at a time.",
        onBack = onBack
    ) {
        HowToPlaySection("Goal") {
            Text("Change one letter at a time to transform the start word into the target word.")
            Text("Each step must be a valid word.")
        }
        
        HowToPlaySection("Rules") {
            Text("• Change exactly ONE letter per step")
            Text("• Each new word must be valid")
            Text("• You cannot reuse words")
            Text("• Any valid path wins!")
        }
        
        HowToPlaySection("Example") {
            Text("COLD → CORD → WORD → WARD → WARM")
            Text("Changed: O→O, C→W, O→A, D→M")
        }
        
        HowToPlaySection("Scoring") {
            Text("After solving, you'll see the shortest possible path.")
            Text("Try to match or beat it!")
        }
    }
}

@Composable
private fun WordLadderGameplay(
    config: WordLadderConfig,
    inputMode: WordLadderInputMode,
    controller: WordLadderController,
    wordData: WordData,
    isSolved: Boolean,
    onSolved: () -> Unit,
    onNewPuzzle: () -> Unit,
    onExit: () -> Unit
) {
    var puzzle by remember { mutableStateOf<WordLadderPuzzle?>(null) }
    var gameState by remember { mutableStateOf<WordLadderState?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(config) {
        val newPuzzle = withContext(Dispatchers.Default) {
            controller.generatePuzzle(config.wordLength, config.difficulty, wordData)
        }
        if (newPuzzle != null) {
            puzzle = newPuzzle
            gameState = WordLadderState(
                puzzle = newPuzzle,
                ladder = listOf(newPuzzle.startWord)
            )
        }
    }
    
    val currentPuzzle = puzzle
    val currentState = gameState
    
    if (currentPuzzle == null || currentState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Generating puzzle...")
        }
        return
    }
    
    GameScaffold(
        title = "Word Ladder",
        onExit = onExit,
        hud = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Steps: ${currentState.steps}", style = MaterialTheme.typography.bodyLarge)
            }
        },
        status = {
            if (isSolved) {
                Column(modifier = Modifier.padding(16.dp)) {
                    VictoryPanel(
                        score = currentState.steps,
                        bestScore = currentPuzzle.shortestPathLength,
                        stars = if (currentState.steps == currentPuzzle.shortestPathLength) 3 else if (currentState.steps <= currentPuzzle.shortestPathLength + 2) 2 else 1,
                        onReplay = {
                            puzzle = null
                            gameState = null
                            onNewPuzzle()
                        },
                        onMenu = onExit,
                        headline = "Solved!",
                        primaryLabel = "New Puzzle"
                    )
                    Text(
                        "Shortest path: ${currentPuzzle.shortestPathLength} steps",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Aqua4.copy(alpha = 0.2f), MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                Text(
                    "Target: ${currentPuzzle.targetWord.uppercase()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentState.ladder) { word ->
                    val previous = currentState.ladder.getOrNull(currentState.ladder.indexOf(word) - 1)
                    Text(
                        buildAnnotatedString {
                            word.forEachIndexed { i, c ->
                                if (previous != null && i < previous.length && previous[i] != c) {
                                    withStyle(SpanStyle(color = Aqua3, fontWeight = FontWeight.Bold)) {
                                        append(c.uppercase())
                                    }
                                } else {
                                    append(c.uppercase())
                                }
                            }
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            if (!isSolved) {
                var input by remember { mutableStateOf("") }
                
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { 
                            input = it
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Next word") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (input.isNotBlank()) {
                                val result = controller.validateStep(
                                    currentState.currentWord,
                                    input,
                                    currentState.ladder.toSet(),
                                    wordData
                                )
                                when (result) {
                                    is WordLadderValidationResult.Valid -> {
                                        val newLadder = currentState.ladder + input.lowercase()
                                        gameState = currentState.copy(ladder = newLadder)
                                        input = ""
                                        errorMessage = null
                                        if (input.lowercase() == currentPuzzle.targetWord) {
                                            onSolved()
                                        }
                                    }
                                    is WordLadderValidationResult.Invalid -> {
                                        errorMessage = result.reason
                                    }
                                }
                            }
                        })
                    )
                    
                    Button(onClick = {
                        if (input.isNotBlank()) {
                            val result = controller.validateStep(
                                currentState.currentWord,
                                input,
                                currentState.ladder.toSet(),
                                wordData
                            )
                            when (result) {
                                is WordLadderValidationResult.Valid -> {
                                    val normalized = input.lowercase()
                                    val newLadder = currentState.ladder + normalized
                                    gameState = currentState.copy(ladder = newLadder)
                                    input = ""
                                    errorMessage = null
                                    if (normalized == currentPuzzle.targetWord) {
                                        onSolved()
                                    }
                                }
                                is WordLadderValidationResult.Invalid -> {
                                    errorMessage = result.reason
                                }
                            }
                        }
                    }) {
                        Text("Submit")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            if (currentState.ladder.size > 1) {
                                gameState = currentState.copy(
                                    ladder = currentState.ladder.dropLast(1)
                                )
                                errorMessage = null
                            }
                        },
                        enabled = currentState.ladder.size > 1
                    ) {
                        Text("Undo")
                    }
                }
            }
        }
    }
}

private fun configSaver() = androidx.compose.runtime.saveable.Saver<WordLadderConfig, List<Any>>(
    save = { listOf(it.wordLength, it.difficulty.ordinal) },
    restore = { WordLadderConfig(it[0] as Int, GameDifficulty.entries[it[1] as Int]) }
)
