package com.xanticious.androidgames.view.games.anagrams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.xanticious.androidgames.controller.games.anagrams.AnagramsController
import com.xanticious.androidgames.data.WordDataProvider
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.anagrams.AnagramsConfig
import com.xanticious.androidgames.model.games.anagrams.AnagramsState
import com.xanticious.androidgames.model.games.anagrams.LetterSetBias
import com.xanticious.androidgames.state.games.anagrams.AnagramsPhase
import com.xanticious.androidgames.state.games.anagrams.AnagramsStateMachine
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.HowToPlaySection
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.WordGameHowToPlay
import com.xanticious.androidgames.view.common.WordGameSetup
import com.xanticious.androidgames.view.games.words.CurrentEntry
import com.xanticious.androidgames.view.games.words.LetterBank
import com.xanticious.androidgames.view.games.words.ProgressStrip
import com.xanticious.androidgames.view.games.words.TargetWordsDisplay
import com.xanticious.androidgames.view.games.words.WordBuilderActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AnagramsGame(
    difficulty: GameDifficulty,
    onExit: () -> Unit
) {
    var phase by rememberSaveable { mutableStateOf("setup") }
    var minLength by rememberSaveable { mutableIntStateOf(3) }
    var showFoundCount by rememberSaveable { mutableStateOf(true) }
    var vowelHeavy by rememberSaveable { mutableStateOf(false) }

    when (phase) {
        "setup" -> AnagramsSetup(
            minLength = minLength,
            showFoundCount = showFoundCount,
            vowelHeavy = vowelHeavy,
            onMinLength = { minLength = it },
            onShowFoundCount = { showFoundCount = it },
            onVowelHeavy = { vowelHeavy = it },
            onHowToPlay = { phase = "howtoplay" },
            onStart = { phase = "playing" }
        )
        "howtoplay" -> AnagramsHowToPlay(onBack = { phase = "setup" })
        "playing" -> AnagramsGameplay(
            config = AnagramsConfig(minLength = minLength, letterSetBias = if (vowelHeavy) LetterSetBias.VOWEL_HEAVY else LetterSetBias.BALANCED),
            showFoundCount = showFoundCount,
            onExit = { phase = "setup" }
        )
    }
}

@Composable
private fun AnagramsSetup(
    minLength: Int,
    showFoundCount: Boolean,
    vowelHeavy: Boolean,
    onMinLength: (Int) -> Unit,
    onShowFoundCount: (Boolean) -> Unit,
    onVowelHeavy: (Boolean) -> Unit,
    onHowToPlay: () -> Unit,
    onStart: () -> Unit
) {
    WordGameSetup(
        title = "Anagrams",
        difficulty = GameDifficulty.MEDIUM,
        onDifficulty = {},
        onHowToPlay = onHowToPlay,
        onStart = onStart
    ) {
        Text("Minimum word length", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(2, 3, 4).forEach { len ->
                OutlinedButton(
                    onClick = { onMinLength(len) },
                    modifier = Modifier
                ) {
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
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AnagramsHowToPlay(onBack: () -> Unit) {
    WordGameHowToPlay(
        title = "How to Play Anagrams",
        intro = "Find all hidden words in a six-letter set. No time limit!",
        onBack = onBack
    ) {
        HowToPlaySection(title = "Goal") {
            Text("Tap letters to spell words. Every valid word that can be made from the six letters is a target.")
        }
        HowToPlaySection(title = "Controls") {
            Text("• Tap bank tiles to build a word\n• Submit to score it\n• Submit & Keep to tweak the end\n• Backspace to undo\n• Give Up to end early")
        }
        HowToPlaySection(title = "Scoring") {
            Text("Longer words score more points. Find all targets to complete the round!")
        }
    }
}

@Composable
private fun AnagramsGameplay(
    config: AnagramsConfig,
    showFoundCount: Boolean,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    var wordData by remember { mutableStateOf<com.xanticious.androidgames.controller.words.WordData?>(null) }
    var controller by remember { mutableStateOf<AnagramsController?>(null) }
    val stateMachine = remember { AnagramsStateMachine() }
    val phase by stateMachine.phase.collectAsState()
    var gameState by remember { mutableStateOf(AnagramsState()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val data = WordDataProvider.get(context)
            wordData = data
            controller = AnagramsController(data)
        }
    }

    LaunchedEffect(controller, config) {
        if (controller != null && phase == AnagramsPhase.IDLE) {
            val (letters, targets) = controller!!.generateRound(config)
            gameState = AnagramsState(
                letters = letters,
                targetWords = targets,
                minLength = config.minLength
            )
            stateMachine.roundStarted()
        }
    }

    if (wordData == null || controller == null) {
        GameScaffold(title = "Anagrams", onExit = onExit) {
            Text("Loading…")
        }
        return
    }

    when (phase) {
        AnagramsPhase.IDLE -> {
            GameScaffold(title = "Anagrams", onExit = onExit) {
                Text("Preparing…")
            }
        }
        AnagramsPhase.PLAYING -> {
            AnagramsPlayingScreen(
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
                    if (word.isEmpty()) return@AnagramsPlayingScreen
                    
                    when {
                        word in gameState.foundWords -> {
                            gameState = gameState.copy(message = "Already found!")
                        }
                        word !in gameState.targetWords -> {
                            gameState = gameState.copy(message = "Not a valid word!")
                        }
                        else -> {
                            val points = controller!!.scoreWord(word)
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
                    if (word.isEmpty()) return@AnagramsPlayingScreen
                    
                    when {
                        word in gameState.foundWords -> {
                            gameState = gameState.copy(message = "Already found!")
                        }
                        word !in gameState.targetWords -> {
                            gameState = gameState.copy(message = "Not a valid word!")
                        }
                        else -> {
                            val points = controller!!.scoreWord(word)
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
        AnagramsPhase.ROUND_OVER -> {
            AnagramsResultsScreen(
                state = gameState,
                onNewRound = {
                    val (letters, targets) = controller!!.generateRound(config)
                    gameState = AnagramsState(
                        letters = letters,
                        targetWords = targets,
                        minLength = config.minLength
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
private fun AnagramsPlayingScreen(
    state: AnagramsState,
    showFoundCount: Boolean,
    onLetterTap: (Int) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onSubmitAndKeep: () -> Unit,
    onGiveUp: () -> Unit,
    onExit: () -> Unit
) {
    GameScaffold(
        title = "Anagrams",
        onExit = onExit,
        hud = {
            val foundText = if (showFoundCount) "Found ${state.foundWords.size} / ${state.targetWords.size}" else "Found ${state.foundWords.size}"
            ProgressStrip(left = foundText, right = "Score ${state.score}")
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
private fun AnagramsResultsScreen(
    state: AnagramsState,
    onNewRound: () -> Unit,
    onExit: () -> Unit
) {
    GameScaffold(
        title = "Anagrams",
        onExit = onExit,
        status = {
            VictoryPanel(
                score = state.score,
                bestScore = state.score,
                stars = if (state.foundWords.size == state.targetWords.size) 3 else 1,
                onReplay = onNewRound,
                onMenu = onExit,
                headline = if (state.foundWords.size == state.targetWords.size) "All Words Found!" else "Round Over",
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
