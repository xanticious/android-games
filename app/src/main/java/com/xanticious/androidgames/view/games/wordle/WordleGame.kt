package com.xanticious.androidgames.view.games.wordle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.data.WordDataProvider
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.wordle.LetterHint
import com.xanticious.androidgames.model.games.wordle.WordleSettings
import com.xanticious.androidgames.state.games.wordle.WordlePhase
import com.xanticious.androidgames.state.games.wordle.WordleStateMachine
import com.xanticious.androidgames.ui.theme.*
import com.xanticious.androidgames.view.common.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WordleGame(
    difficulty: GameDifficulty,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    var wordData by remember { mutableStateOf<WordData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            wordData = WordDataProvider.get(context)
            isLoading = false
        }
    }
    
    if (isLoading || wordData == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    
    val machine = remember { WordleStateMachine(wordData!!) }
    val phase by machine.phase.collectAsState()
    val settings by machine.settings.collectAsState()
    val roundState by machine.roundState.collectAsState()
    
    when (phase) {
        WordlePhase.SETUP -> WordleSetupScreen(
            settings = settings,
            onSettingsChange = { machine.updateSettings(it) },
            onHowToPlay = { machine.showHowToPlay() },
            onStart = { machine.startPlaying() },
            onExit = onExit
        )
        WordlePhase.HOW_TO_PLAY -> WordleHowToPlayScreen(
            onBack = { machine.backToSetup() },
            onStart = { machine.startPlaying() }
        )
        WordlePhase.PLAYING, WordlePhase.WON, WordlePhase.LOST -> {
            roundState?.let { state ->
                WordleGameplayScreen(
                    state = state,
                    settings = settings,
                    phase = phase,
                    onGuess = { machine.submitGuess(it) },
                    onHelp = { machine.requestHelp() },
                    onNextRound = { machine.nextRound() },
                    onMenu = { machine.backToSetup() },
                    onExit = onExit,
                    wordData = wordData!!
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordleSetupScreen(
    settings: WordleSettings,
    onSettingsChange: (WordleSettings) -> Unit,
    onHowToPlay: () -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wordle Setup") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        WordGameSetup(
            title = "Wordle",
            difficulty = GameDifficulty.MEDIUM,
            onDifficulty = { },
            onHowToPlay = onHowToPlay,
            onStart = onStart,
            modifier = Modifier.padding(padding)
        ) {
            Text("Max Guesses", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 6, 7).forEach { count ->
                    FilterChip(
                        selected = settings.maxGuesses == count,
                        onClick = { onSettingsChange(settings.copy(maxGuesses = count)) },
                        label = { Text(count.toString()) }
                    )
                }
            }
            
            Row(
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enforce Clue Consistency")
                Switch(
                    checked = settings.enforceConsistency,
                    onCheckedChange = { onSettingsChange(settings.copy(enforceConsistency = it)) }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Carry First Guess")
                Switch(
                    checked = settings.carryFirstGuess,
                    onCheckedChange = { onSettingsChange(settings.copy(carryFirstGuess = it)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordleHowToPlayScreen(
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How to Play") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        WordGameHowToPlay(
            title = "Wordle",
            intro = "Guess the hidden five-letter word. After each guess, tiles color in: correct (right spot), present (wrong spot), absent.",
            onBack = onBack,
            modifier = Modifier.padding(padding)
        ) {
            HowToPlaySection("First Guess") {
                Text("Your first guess each round is pre-filled with the previous round's target word (or a random word the first time you play); submit it or edit it.")
            }
            
            HowToPlaySection("Clue Consistency") {
                Text("With clue-consistency on, each new guess must be compatible with every clue so far — reuse greens in place, include yellows, and avoid grays; inconsistent guesses are rejected inline.")
            }
            
            HowToPlaySection("Help") {
                Text("Tap Help to auto-play a random valid word that fits the current clues.")
            }
            
            HowToPlaySection("Win Condition") {
                Text("Win by guessing the target; otherwise the round ends when guesses run out. Then start another round whenever you like.")
            }
            
            Button(onClick = onStart, modifier = Modifier.padding(top = 12.dp)) {
                Text("Start Playing")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordleGameplayScreen(
    state: com.xanticious.androidgames.model.games.wordle.WordleRoundState,
    settings: WordleSettings,
    phase: WordlePhase,
    onGuess: (String) -> Unit,
    onHelp: () -> Unit,
    onNextRound: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit,
    wordData: WordData
) {
    var currentInput by rememberSaveable { mutableStateOf(state.currentInput) }
    
    LaunchedEffect(state.currentInput) {
        if (state.isFirstGuess) {
            currentInput = state.currentInput
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wordle") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        GameScaffold(
            title = "Wordle",
            onExit = onExit,
            hud = {
                GameHud(
                    left = "Round ${state.guesses.size}/${settings.maxGuesses}",
                    center = "",
                    right = ""
                )
            },
            status = {
                when (phase) {
                    WordlePhase.WON -> VictoryPanel(
                        score = state.guesses.size,
                        bestScore = state.guesses.size,
                        stars = 3,
                        onReplay = onNextRound,
                        onMenu = onMenu,
                        headline = "You guessed it!",
                        primaryLabel = "Next Round"
                    )
                    WordlePhase.LOST -> DefeatPanel(
                        score = state.guesses.size,
                        bestScore = state.guesses.size,
                        onTryAgain = onNextRound,
                        onMenu = onMenu,
                        headline = "The word was: ${state.targetWord}"
                    )
                    else -> {}
                }
            },
            modifier = Modifier.padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Guess grid
                WordleGrid(
                    guesses = state.guesses,
                    currentInput = currentInput,
                    maxGuesses = settings.maxGuesses,
                    wordLength = settings.wordLength
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Keyboard
                if (phase == WordlePhase.PLAYING) {
                    WordleKeyboard(
                        onLetter = { letter ->
                            if (currentInput.length < settings.wordLength) {
                                currentInput += letter.lowercase()
                            }
                        },
                        onDelete = {
                            if (currentInput.isNotEmpty()) {
                                currentInput = currentInput.dropLast(1)
                            }
                        },
                        onEnter = {
                            if (currentInput.length == settings.wordLength) {
                                onGuess(currentInput)
                                currentInput = ""
                            }
                        },
                        onHelp = onHelp,
                        letterHints = computeLetterHints(state.guesses)
                    )
                }
            }
        }
    }
}

@Composable
private fun WordleGrid(
    guesses: List<com.xanticious.androidgames.model.games.wordle.GuessResult>,
    currentInput: String,
    maxGuesses: Int,
    wordLength: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until maxGuesses) {
            if (i < guesses.size) {
                WordleRow(word = guesses[i].word, hints = guesses[i].hints)
            } else if (i == guesses.size) {
                WordleRow(word = currentInput.padEnd(wordLength), hints = null)
            } else {
                WordleRow(word = " ".repeat(wordLength), hints = null)
            }
        }
    }
}

@Composable
private fun WordleRow(
    word: String,
    hints: List<LetterHint>?
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        word.forEachIndexed { index, char ->
            val backgroundColor = when (hints?.getOrNull(index)) {
                LetterHint.CORRECT -> GameSuccess
                LetterHint.PRESENT -> GameAccent
                LetterHint.ABSENT -> GameNeutral
                null -> Color.Transparent
            }
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(backgroundColor, RoundedCornerShape(4.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (char.isLetter()) {
                    Text(
                        text = char.uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hints != null) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun WordleKeyboard(
    onLetter: (Char) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    onHelp: () -> Unit,
    letterHints: Map<Char, LetterHint>
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val rows = listOf(
            "QWERTYUIOP",
            "ASDFGHJKL",
            "ZXCVBNM"
        )
        
        rows.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (rowIndex == 2) {
                    Button(
                        onClick = onHelp,
                        modifier = Modifier.height(40.dp).weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = GameAccent)
                    ) {
                        Text("Help", fontSize = 12.sp)
                    }
                }
                
                row.forEach { letter ->
                    val hint = letterHints[letter.lowercaseChar()]
                    val backgroundColor = when (hint) {
                        LetterHint.CORRECT -> GameSuccess
                        LetterHint.PRESENT -> GameAccent
                        LetterHint.ABSENT -> GameNeutral
                        null -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .weight(1f)
                            .background(backgroundColor, RoundedCornerShape(4.dp))
                            .clickable { onLetter(letter) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter.toString(),
                            fontWeight = FontWeight.Bold,
                            color = if (hint != null) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                if (rowIndex == 2) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.height(40.dp).weight(1.5f)
                    ) {
                        Icon(Icons.Default.Delete, "Delete")
                    }
                    
                    Button(
                        onClick = onEnter,
                        modifier = Modifier.height(40.dp).weight(1.5f)
                    ) {
                        Text("Enter", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun computeLetterHints(guesses: List<com.xanticious.androidgames.model.games.wordle.GuessResult>): Map<Char, LetterHint> {
    val hints = mutableMapOf<Char, LetterHint>()
    
    for (guess in guesses) {
        for (i in guess.hints.indices) {
            val letter = guess.word[i].lowercaseChar()
            val hint = guess.hints[i]
            
            val current = hints[letter]
            if (current == null || hint.ordinal < current.ordinal) {
                hints[letter] = hint
            }
        }
    }
    
    return hints
}
