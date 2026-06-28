package com.xanticious.androidgames.view.games.wordchain

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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.wordchain.WordChainController
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.data.WordDataProvider
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.wordchain.LinkRule
import com.xanticious.androidgames.model.games.wordchain.WordChainConfig
import com.xanticious.androidgames.model.games.wordchain.WordChainInputMode
import com.xanticious.androidgames.model.games.wordchain.WordChainState
import com.xanticious.androidgames.model.games.wordchain.WordChainValidationResult
import com.xanticious.androidgames.state.games.wordchain.WordChainPhase
import com.xanticious.androidgames.state.games.wordchain.WordChainStateMachine
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.HowToPlaySection
import com.xanticious.androidgames.view.common.WordGameHowToPlay
import com.xanticious.androidgames.view.common.WordGameSetup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WordChainGame(
    difficulty: GameDifficulty,
    onExit: () -> Unit
) {
    val controller = remember { WordChainController() }
    val stateMachine = remember { WordChainStateMachine() }
    val phase by stateMachine.phase.collectAsState()
    
    var config by rememberSaveable(stateSaver = configSaver()) {
        mutableStateOf(WordChainConfig.default())
    }
    var inputMode by rememberSaveable { mutableStateOf(WordChainInputMode.FIELD) }
    
    when (phase) {
        WordChainPhase.SETUP -> WordChainSetup(
            config = config,
            inputMode = inputMode,
            onConfigChange = { config = it },
            onInputModeChange = { inputMode = it },
            onHowToPlay = { stateMachine.showHowToPlay() },
            onStart = { stateMachine.startGame() }
        )
        WordChainPhase.HOW_TO_PLAY -> WordChainHowToPlay(
            onBack = { stateMachine.backToSetup() }
        )
        WordChainPhase.PLAYING, WordChainPhase.CHAIN_BROKEN -> {
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
                WordChainGameplay(
                    config = config,
                    inputMode = inputMode,
                    controller = controller,
                    wordData = wd,
                    isChainBroken = phase == WordChainPhase.CHAIN_BROKEN,
                    onChainBroken = { stateMachine.timeExpired() },
                    onPass = { stateMachine.pass() },
                    onNewGame = { stateMachine.newGame() },
                    onExit = { stateMachine.exit(); onExit() }
                )
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading word data...")
            }
        }
    }
}

@Composable
private fun WordChainSetup(
    config: WordChainConfig,
    inputMode: WordChainInputMode,
    onConfigChange: (WordChainConfig) -> Unit,
    onInputModeChange: (WordChainInputMode) -> Unit,
    onHowToPlay: () -> Unit,
    onStart: () -> Unit
) {
    WordGameSetup(
        title = "Word Chain",
        difficulty = GameDifficulty.MEDIUM,
        onDifficulty = { },
        onHowToPlay = onHowToPlay,
        onStart = onStart
    ) {
        Text("Link Rule", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LinkRule.entries.forEach { rule ->
                FilterChip(
                    selected = config.linkRule == rule,
                    onClick = { onConfigChange(config.copy(linkRule = rule)) },
                    label = { Text(rule.label) }
                )
            }
        }
        
        Text("Minimum Word Length", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2, 3, 4).forEach { len ->
                FilterChip(
                    selected = config.minWordLength == len,
                    onClick = { onConfigChange(config.copy(minWordLength = len)) },
                    label = { Text("$len letters") }
                )
            }
        }
        
        Text("Per-word Timer", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null to "Off", 10f to "10s", 20f to "20s").forEach { (timer, label) ->
                FilterChip(
                    selected = config.perWordTimer == timer,
                    onClick = { onConfigChange(config.copy(perWordTimer = timer)) },
                    label = { Text(label) }
                )
            }
        }
        
        Text("Input Mode", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = inputMode == WordChainInputMode.FIELD,
                onClick = { onInputModeChange(WordChainInputMode.FIELD) },
                label = { Text("Keyboard") }
            )
            FilterChip(
                selected = inputMode == WordChainInputMode.ON_SCREEN_KEYBOARD,
                onClick = { onInputModeChange(WordChainInputMode.ON_SCREEN_KEYBOARD) },
                label = { Text("On-screen") }
            )
        }
    }
}

@Composable
private fun WordChainHowToPlay(onBack: () -> Unit) {
    WordGameHowToPlay(
        title = "How to Play Word Chain",
        intro = "Build the longest chain of words where each word starts with the last letter(s) of the previous word.",
        onBack = onBack
    ) {
        HowToPlaySection("Goal") {
            Text("Create the longest possible chain without repeating words.")
            Text("Each word must start with the last letter(s) of the previous word.")
        }
        
        HowToPlaySection("Rules") {
            Text("• Each word must be valid")
            Text("• No repeating words")
            Text("• Follow the link rule (last letter or last two letters)")
            Text("• Meet minimum word length")
            Text("• Submit before time expires (if timer is on)")
        }
        
        HowToPlaySection("Example (Last Letter)") {
            Text("apple → eagle → echo → orbit → tiger → rapids")
            Text("E→E, O→O, T→T, S→?")
        }
        
        HowToPlaySection("Scoring") {
            Text("Score = chain length × 10 + total letters")
            Text("Try to beat your best chain!")
        }
    }
}

@Composable
private fun WordChainGameplay(
    config: WordChainConfig,
    inputMode: WordChainInputMode,
    controller: WordChainController,
    wordData: WordData,
    isChainBroken: Boolean,
    onChainBroken: () -> Unit,
    onPass: () -> Unit,
    onNewGame: () -> Unit,
    onExit: () -> Unit
) {
    var gameState by remember { 
        mutableStateOf(
            WordChainState(
                chain = listOf("apple"),
                usedWords = setOf("apple")
            )
        )
    }
    var input by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var timeRemaining by remember { mutableFloatStateOf(config.perWordTimer ?: 0f) }
    
    LaunchedEffect(config) {
        val seedWord = withContext(Dispatchers.Default) {
            wordData.randomWord(minLength = config.minWordLength, maxLength = 8)
        } ?: "apple"
        gameState = WordChainState(
            chain = listOf(seedWord),
            usedWords = setOf(seedWord),
            timeRemaining = config.perWordTimer
        )
        timeRemaining = config.perWordTimer ?: 0f
    }
    
    GameLoop(running = !isChainBroken && config.perWordTimer != null) { dt ->
        timeRemaining -= dt
        if (timeRemaining <= 0f && !isChainBroken) {
            onChainBroken()
        }
    }
    
    GameScaffold(
        title = "Word Chain",
        onExit = onExit,
        hud = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Chain: ${gameState.chainLength}", style = MaterialTheme.typography.bodyLarge)
                if (config.perWordTimer != null && !isChainBroken) {
                    Text(
                        "Time: ${timeRemaining.toInt()}s",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (timeRemaining < 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        status = {
            if (isChainBroken) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DefeatPanel(
                        score = controller.calculateScore(gameState.chainLength, gameState.totalLetters),
                        bestScore = 0,
                        onTryAgain = {
                            val seedWord = wordData.randomWord(minLength = config.minWordLength, maxLength = 8) ?: "apple"
                            gameState = WordChainState(
                                chain = listOf(seedWord),
                                usedWords = setOf(seedWord),
                                timeRemaining = config.perWordTimer
                            )
                            input = ""
                            errorMessage = null
                            timeRemaining = config.perWordTimer ?: 0f
                            onNewGame()
                        },
                        onMenu = onExit,
                        headline = "Chain Broken!"
                    )
                    Text(
                        "Chain length: ${gameState.chainLength} words (${gameState.totalLetters} letters)",
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
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(gameState.chain) { word ->
                    Text(
                        word.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .background(Aqua4.copy(alpha = 0.2f), MaterialTheme.shapes.small)
                            .padding(8.dp)
                    )
                }
            }
            
            if (!isChainBroken) {
                val requiredStart = controller.getRequiredStart(gameState.lastWord, config.linkRule)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Aqua3.copy(alpha = 0.2f), MaterialTheme.shapes.medium)
                        .padding(16.dp)
                ) {
                    Text(
                        "Next word must start with: $requiredStart",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
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
                                val result = controller.validateWord(
                                    input,
                                    gameState.lastWord,
                                    config.linkRule,
                                    config.minWordLength,
                                    gameState.usedWords,
                                    wordData
                                )
                                when (result) {
                                    is WordChainValidationResult.Valid -> {
                                        val normalized = input.lowercase()
                                        gameState = gameState.copy(
                                            chain = gameState.chain + normalized,
                                            usedWords = gameState.usedWords + normalized
                                        )
                                        input = ""
                                        errorMessage = null
                                        timeRemaining = config.perWordTimer ?: 0f
                                    }
                                    is WordChainValidationResult.Invalid -> {
                                        errorMessage = result.reason
                                    }
                                }
                            }
                        })
                    )
                    
                    Button(onClick = {
                        if (input.isNotBlank()) {
                            val result = controller.validateWord(
                                input,
                                gameState.lastWord,
                                config.linkRule,
                                config.minWordLength,
                                gameState.usedWords,
                                wordData
                            )
                            when (result) {
                                is WordChainValidationResult.Valid -> {
                                    val normalized = input.lowercase()
                                    gameState = gameState.copy(
                                        chain = gameState.chain + normalized,
                                        usedWords = gameState.usedWords + normalized
                                    )
                                    input = ""
                                    errorMessage = null
                                    timeRemaining = config.perWordTimer ?: 0f
                                }
                                is WordChainValidationResult.Invalid -> {
                                    errorMessage = result.reason
                                }
                            }
                        }
                    }) {
                        Text("Submit")
                    }
                    
                    OutlinedButton(onClick = onPass) {
                        Text("Pass")
                    }
                }
            }
        }
    }
}

private fun configSaver() = androidx.compose.runtime.saveable.Saver<WordChainConfig, List<Any?>>(
    save = { listOf(it.linkRule.ordinal, it.minWordLength, it.perWordTimer) },
    restore = { WordChainConfig(LinkRule.entries[it[0] as Int], it[1] as Int, it[2] as? Float) }
)
