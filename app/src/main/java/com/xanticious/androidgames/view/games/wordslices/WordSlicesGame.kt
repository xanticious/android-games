package com.xanticious.androidgames.view.games.wordslices

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.data.WordDataProvider
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.wordslices.LetterCase
import com.xanticious.androidgames.model.games.wordslices.WordSlicesSettings
import com.xanticious.androidgames.state.games.wordslices.WordSlicesPhase
import com.xanticious.androidgames.state.games.wordslices.WordSlicesStateMachine
import com.xanticious.androidgames.ui.theme.*
import com.xanticious.androidgames.view.common.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WordSlicesGame(
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
    
    val machine = remember { WordSlicesStateMachine(wordData!!) }
    val phase by machine.phase.collectAsState()
    val settings by machine.settings.collectAsState()
    val roundState by machine.roundState.collectAsState()
    
    LaunchedEffect(difficulty) {
        machine.updateSettings(settings.copy(difficulty = difficulty))
    }
    
    when (phase) {
        WordSlicesPhase.SETUP -> WordSlicesSetupScreen(
            settings = settings,
            onSettingsChange = { machine.updateSettings(it) },
            onHowToPlay = { machine.showHowToPlay() },
            onStart = { machine.startPlaying() },
            onExit = onExit
        )
        WordSlicesPhase.HOW_TO_PLAY -> WordSlicesHowToPlayScreen(
            onBack = { machine.backToSetup() },
            onStart = { machine.startPlaying() }
        )
        WordSlicesPhase.PLAYING, WordSlicesPhase.WON, WordSlicesPhase.LOST -> {
            roundState?.let { state ->
                WordSlicesGameplayScreen(
                    state = state,
                    settings = settings,
                    phase = phase,
                    onGuess = { machine.guessLetter(it) },
                    onNewWord = { machine.newWord() },
                    onMenu = { machine.backToSetup() },
                    onExit = onExit
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordSlicesSetupScreen(
    settings: WordSlicesSettings,
    onSettingsChange: (WordSlicesSettings) -> Unit,
    onHowToPlay: () -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Word Slices Setup") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        WordGameSetup(
            title = "Word Slices",
            difficulty = settings.difficulty,
            onDifficulty = { onSettingsChange(settings.copy(difficulty = it)) },
            onHowToPlay = onHowToPlay,
            onStart = onStart,
            modifier = Modifier.padding(padding)
        ) {
            Row(
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reveal Definition on Win")
                Switch(
                    checked = settings.revealDefinition,
                    onCheckedChange = { onSettingsChange(settings.copy(revealDefinition = it)) }
                )
            }
            
            Text("Letter Case", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.letterCase == LetterCase.UPPER,
                    onClick = { onSettingsChange(settings.copy(letterCase = LetterCase.UPPER)) },
                    label = { Text("UPPER") }
                )
                FilterChip(
                    selected = settings.letterCase == LetterCase.LOWER,
                    onClick = { onSettingsChange(settings.copy(letterCase = LetterCase.LOWER)) },
                    label = { Text("lower") }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordSlicesHowToPlayScreen(
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
            title = "Word Slices",
            intro = "A hidden word appears as blanks; the cake starts with all 12 slices.",
            onBack = onBack,
            modifier = Modifier.padding(padding)
        ) {
            HowToPlaySection("Gameplay") {
                Text("Tap a letter. If it is in the word, every matching blank fills in. If it is not, one slice of cake disappears.")
            }
            
            HowToPlaySection("Win Condition") {
                Text("Reveal the whole word before the last slice is gone to win.")
            }
            
            HowToPlaySection("Loss") {
                Text("If the cake runs out, you have lost the round; remaining guesses simply finish revealing the word so you can see the answer.")
            }
            
            Button(onClick = onStart, modifier = Modifier.padding(top = 12.dp)) {
                Text("Start Playing")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordSlicesGameplayScreen(
    state: com.xanticious.androidgames.model.games.wordslices.WordSlicesRoundState,
    settings: WordSlicesSettings,
    phase: WordSlicesPhase,
    onGuess: (Char) -> Unit,
    onNewWord: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Word Slices") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        GameScaffold(
            title = "Word Slices",
            onExit = onExit,
            hud = {
                GameHud(
                    left = { Text("Slices: ${state.slicesRemaining}/12") },
                    center = null,
                    right = null
                )
            },
            status = {
                when (phase) {
                    WordSlicesPhase.WON -> VictoryPanel(
                        message = if (settings.revealDefinition && state.definition != null) {
                            "${state.word.uppercase()}: ${state.definition}"
                        } else {
                            "You got it!"
                        },
                        onNext = onNewWord,
                        onMenu = onMenu,
                        nextLabel = "New Word"
                    )
                    WordSlicesPhase.LOST -> DefeatPanel(
                        message = "Out of cake! The word was: ${state.word.uppercase()}",
                        onRetry = onNewWord,
                        onMenu = onMenu,
                        retryLabel = "New Word"
                    )
                    else -> {}
                }
            },
            modifier = Modifier.padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cake
                CakeView(slicesRemaining = state.slicesRemaining, totalSlices = 12)
                
                // Word display
                WordDisplay(
                    word = state.word,
                    guessedLetters = state.guessedLetters,
                    letterCase = settings.letterCase
                )
                
                // Misses
                if (state.wrongGuesses.isNotEmpty()) {
                    Text(
                        text = "Misses: ${state.wrongGuesses.sorted().joinToString(" ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GameHazard
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                // Letter keyboard
                if (phase == WordSlicesPhase.PLAYING) {
                    AlphabetKeyboard(
                        onLetter = onGuess,
                        usedLetters = state.guessedLetters
                    )
                }
            }
        }
    }
}

@Composable
private fun CakeView(slicesRemaining: Int, totalSlices: Int) {
    Canvas(
        modifier = Modifier.size(200.dp)
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = size.minDimension / 2.5f
        
        val anglePerSlice = 360f / totalSlices
        
        for (i in 0 until totalSlices) {
            val isPresent = i < slicesRemaining
            val color = if (isPresent) GameAccent else GameNeutral.copy(alpha = 0.3f)
            
            val startAngle = i * anglePerSlice - 90f
            val sweepAngle = anglePerSlice - 2f
            
            val path = Path().apply {
                moveTo(centerX, centerY)
                val startRad = Math.toRadians(startAngle.toDouble())
                val endRad = Math.toRadians((startAngle + sweepAngle).toDouble())
                
                lineTo(
                    centerX + radius * cos(startRad).toFloat(),
                    centerY + radius * sin(startRad).toFloat()
                )
                
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        centerX - radius,
                        centerY - radius,
                        centerX + radius,
                        centerY + radius
                    ),
                    startAngleDegrees = startAngle,
                    sweepAngleDegrees = sweepAngle,
                    forceMoveTo = false
                )
                
                close()
            }
            
            drawPath(path, color, style = Fill)
        }
    }
}

@Composable
private fun WordDisplay(
    word: String,
    guessedLetters: Set<Char>,
    letterCase: LetterCase
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        word.forEach { letter ->
            val isRevealed = letter.lowercaseChar() in guessedLetters.map { it.lowercaseChar() }
            val displayLetter = if (isRevealed) {
                when (letterCase) {
                    LetterCase.UPPER -> letter.uppercase()
                    LetterCase.LOWER -> letter.lowercase()
                }
            } else {
                "_"
            }
            
            Text(
                text = displayLetter,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 24.dp)
            )
        }
    }
}

@Composable
private fun AlphabetKeyboard(
    onLetter: (Char) -> Unit,
    usedLetters: Set<Char>
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val rows = listOf(
            "QWERTYUIOP",
            "ASDFGHJKL",
            "ZXCVBNM"
        )
        
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { letter ->
                    val isUsed = letter.lowercaseChar() in usedLetters.map { it.lowercaseChar() }
                    
                    Button(
                        onClick = { onLetter(letter.lowercaseChar()) },
                        enabled = !isUsed,
                        modifier = Modifier.height(40.dp).weight(1f),
                        colors = if (isUsed) {
                            ButtonDefaults.buttonColors(
                                containerColor = GameNeutral.copy(alpha = 0.3f),
                                disabledContainerColor = GameNeutral.copy(alpha = 0.3f)
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        }
                    ) {
                        Text(
                            text = letter.toString(),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
