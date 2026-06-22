package com.xanticious.androidgames.view.games.anagramsarcade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.anagramsarcade.AnagramsArcadeController
import com.xanticious.androidgames.data.WordDataProvider
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.anagramsarcade.AnagramsArcadeConfig
import com.xanticious.androidgames.model.games.anagramsarcade.AnagramsArcadeState
import com.xanticious.androidgames.model.games.anagramsarcade.LetterSetBias
import com.xanticious.androidgames.state.games.anagramsarcade.AnagramsArcadePhase
import com.xanticious.androidgames.state.games.anagramsarcade.AnagramsArcadeStateMachine
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.HowToPlaySection
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.WordGameHowToPlay
import com.xanticious.androidgames.view.common.WordGameSetup
import com.xanticious.androidgames.view.games.words.CountdownTimer
import com.xanticious.androidgames.view.games.words.CurrentEntry
import com.xanticious.androidgames.view.games.words.LetterBank
import com.xanticious.androidgames.view.games.words.ProgressStrip
import com.xanticious.androidgames.view.games.words.TargetWordsDisplay
import com.xanticious.androidgames.view.games.words.WordBuilderActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun AnagramsArcadeGame(
    difficulty: GameDifficulty,
    onExit: () -> Unit
) {
    var phase by rememberSaveable { mutableStateOf("setup") }
    var minLength by rememberSaveable { mutableIntStateOf(3) }
    var showFoundCount by rememberSaveable { mutableStateOf(true) }
    var vowelHeavy by rememberSaveable { mutableStateOf(false) }
    var roundDuration by rememberSaveable { mutableIntStateOf(90) }

    when (phase) {
        "setup" -> AnagramsArcadeSetup(
            minLength = minLength,
            showFoundCount = showFoundCount,
            vowelHeavy = vowelHeavy,
            roundDuration = roundDuration,
            onMinLength = { minLength = it },
            onShowFoundCount = { showFoundCount = it },
            onVowelHeavy = { vowelHeavy = it },
            onRoundDuration = { roundDuration = it },
            onHowToPlay = { phase = "howtoplay" },
            onStart = { phase = "playing" }
        )
        "howtoplay" -> AnagramsArcadeHowToPlay(onBack = { phase = "setup" })
        "playing" -> AnagramsArcadeGameplay(
            config = AnagramsArcadeConfig(
                minLength = minLength,
                letterSetBias = if (vowelHeavy) LetterSetBias.VOWEL_HEAVY else LetterSetBias.BALANCED,
                roundDuration = roundDuration
            ),
            showFoundCount = showFoundCount,
            onExit = { phase = "setup" }
        )
    }
}

@Composable
private fun AnagramsArcadeSetup(
    minLength: Int,
    showFoundCount: Boolean,
    vowelHeavy: Boolean,
    roundDuration: Int,
    onMinLength: (Int) -> Unit,
    onShowFoundCount: (Boolean) -> Unit,
    onVowelHeavy: (Boolean) -> Unit,
    onRoundDuration: (Int) -> Unit,
    onHowToPlay: () -> Unit,
    onStart: () -> Unit
) {
    WordGameSetup(
        title = "Anagrams (Arcade)",
        difficulty = GameDifficulty.MEDIUM,
        onDifficulty = {},
        onHowToPlay = onHowToPlay,
        onStart = onStart
    ) {
        Text("Round Duration", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(60, 90, 120).forEach { duration ->
                OutlinedButton(onClick = { onRoundDuration(duration) }) {
                    Text(if (roundDuration == duration) "✓ ${duration}s" else "${duration}s")
                }
            }
        }
        
        Text("Minimum word length", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2, 3, 4).forEach { len ->
                OutlinedButton(onClick = { onMinLength(len) }) {
                    Text(if (minLength == len) "✓ $len letters" else "$len letters")
                }
            }
        }
        
        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LabeledSwitch("Show found count target", showFoundCount, onShowFoundCount)
            LabeledSwitch("Vowel-heavy letter sets", vowelHeavy, onVowelHeavy)
        }
    }
}

@Composable
private fun LabeledSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AnagramsArcadeHowToPlay(onBack: () -> Unit) {
    WordGameHowToPlay(
        title = "How to Play Anagrams (Arcade)",
        intro = "Find as many words as possible in a six-letter set before time runs out!",
        onBack = onBack
    ) {
        HowToPlaySection(title = "Goal") {
            Text("Tap letters to spell words. Race against the clock to find as many as you can!")
        }
        HowToPlaySection(title = "Controls") {
            Text("• Tap bank tiles to build a word\n• Submit to score it\n• Submit & Keep to tweak the end\n• Backspace to undo\n• Give Up to end early")
        }
        HowToPlaySection(title = "Scoring") {
            Text("Longer words score more points. Find words quickly for bonus points!")
        }
    }
}

@Composable
private fun AnagramsArcadeGameplay(
    config: AnagramsArcadeConfig,
    showFoundCount: Boolean,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    var wordData by remember { mutableStateOf<com.xanticious.androidgames.controller.words.WordData?>(null) }
    var controller by remember { mutableStateOf<AnagramsArcadeController?>(null) }
    val stateMachine = remember { AnagramsArcadeStateMachine() }
    val phase by stateMachine.phase.collectAsState()
    var gameState by remember { mutableStateOf(AnagramsArcadeState(timeRemaining = config.roundDuration)) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val data = WordDataProvider.get(context)
            wordData = data
            controller = AnagramsArcadeController(data)
        }
    }

    LaunchedEffect(controller, config) {
        if (controller != null && phase == AnagramsArcadePhase.IDLE) {
            val (letters, targets) = controller!!.generateRound(config)
            gameState = AnagramsArcadeState(
                letters = letters,
                targetWords = targets,
                minLength = config.minLength,
                timeRemaining = config.roundDuration
            )
            stateMachine.roundStarted()
        }
    }

    LaunchedEffect(phase) {
        if (phase == AnagramsArcadePhase.PLAYING) {
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
        GameScaffold(title = "Anagrams (Arcade)", onExit = onExit) {
            Text("Loading…")
        }
        return
    }

    when (phase) {
        AnagramsArcadePhase.IDLE -> {
            GameScaffold(title = "Anagrams (Arcade)", onExit = onExit) {
                Text("Preparing…")
            }
        }
        AnagramsArcadePhase.PLAYING -> {
            AnagramsArcadePlayingScreen(
                state = gameState,
                showFoundCount = showFoundCount,
                onLetterTap = { index ->
                    if (index !in gameState.usedIndices) {
                        gameState = gameState.copy(
                            currentEntry = gameState.currentEntry + gameState.letters[index],
                            usedIndices = gameState.usedIndices + index
                        )
                        stateMachine.letterTapped()
                    }
                },
                onBackspace = {
                    if (gameState.usedIndices.isNotEmpty()) {
                        val lastIndex = gameState.usedIndices.maxOrNull()!!
                        gameState = gameState.copy(
                            currentEntry = gameState.currentEntry.dropLast(1),
                            usedIndices = gameState.usedIndices - lastIndex
                        )
                        stateMachine.entryBackspaced()
                    }
                },
                onSubmit = {
                    val word = gameState.currentEntry.lowercase()
                    if (word.isEmpty()) return@AnagramsArcadePlayingScreen
                    
                    when {
                        word in gameState.foundWords -> {
                            gameState = gameState.copy(message = "Already found!")
                        }
                        word !in gameState.targetWords -> {
                            gameState = gameState.copy(message = "Not a valid word!")
                        }
                        else -> {
                            val points = controller!!.scoreWord(word, gameState.timeRemaining)
                            gameState = gameState.copy(
                                foundWords = gameState.foundWords + word,
                                score = gameState.score + points,
                                currentEntry = "",
                                usedIndices = emptySet(),
                                message = "+$points"
                            )
                            stateMachine.wordSubmitted()
                            if (gameState.foundWords.size == gameState.targetWords.size) {
                                stateMachine.allWordsFound()
                            }
                        }
                    }
                },
                onSubmitAndKeep = {
                    val word = gameState.currentEntry.lowercase()
                    if (word.isEmpty()) return@AnagramsArcadePlayingScreen
                    
                    when {
                        word in gameState.foundWords -> {
                            gameState = gameState.copy(message = "Already found!")
                        }
                        word !in gameState.targetWords -> {
                            gameState = gameState.copy(message = "Not a valid word!")
                        }
                        else -> {
                            val points = controller!!.scoreWord(word, gameState.timeRemaining)
                            gameState = gameState.copy(
                                foundWords = gameState.foundWords + word,
                                score = gameState.score + points,
                                message = "+$points"
                            )
                            stateMachine.wordSubmitted()
                            if (gameState.foundWords.size == gameState.targetWords.size) {
                                stateMachine.allWordsFound()
                            }
                        }
                    }
                },
                onGiveUp = {
                    stateMachine.gaveUp()
                },
                onExit = onExit
            )
        }
        AnagramsArcadePhase.ROUND_OVER -> {
            AnagramsArcadeResultsScreen(
                state = gameState,
                onNewRound = {
                    val (letters, targets) = controller!!.generateRound(config)
                    gameState = AnagramsArcadeState(
                        letters = letters,
                        targetWords = targets,
                        minLength = config.minLength,
                        timeRemaining = config.roundDuration
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
private fun AnagramsArcadePlayingScreen(
    state: AnagramsArcadeState,
    showFoundCount: Boolean,
    onLetterTap: (Int) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onSubmitAndKeep: () -> Unit,
    onGiveUp: () -> Unit,
    onExit: () -> Unit
) {
    GameScaffold(
        title = "Anagrams (Arcade)",
        onExit = onExit,
        hud = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val foundText = if (showFoundCount) "Found ${state.foundWords.size} / ${state.targetWords.size}" else "Found ${state.foundWords.size}"
                Text(foundText, fontWeight = FontWeight.Bold)
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
            TargetWordsDisplay(
                targetWords = state.targetWords,
                foundWords = state.foundWords,
                groupByLength = true
            )
            
            CurrentEntry(entry = state.currentEntry)
            
            LetterBank(
                letters = state.letters,
                usedIndices = state.usedIndices,
                onLetterTap = onLetterTap
            )
            
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
        }
    }
}

@Composable
private fun AnagramsArcadeResultsScreen(
    state: AnagramsArcadeState,
    onNewRound: () -> Unit,
    onExit: () -> Unit
) {
    GameScaffold(
        title = "Anagrams (Arcade)",
        onExit = onExit,
        status = {
            VictoryPanel(
                score = state.score,
                bestScore = state.score,
                stars = if (state.foundWords.size == state.targetWords.size) 3 else if (state.foundWords.size >= state.targetWords.size / 2) 2 else 1,
                onReplay = onNewRound,
                onMenu = onExit,
                headline = if (state.timeRemaining == 0) "Time's Up!" else if (state.foundWords.size == state.targetWords.size) "All Words Found!" else "Round Over",
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
            Text("Found ${state.foundWords.size} / ${state.targetWords.size} words")
            
            TargetWordsDisplay(
                targetWords = state.targetWords,
                foundWords = state.foundWords,
                groupByLength = true
            )
        }
    }
}
