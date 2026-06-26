package com.xanticious.androidgames.view.games.typingsprint

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.typingsprint.TypingSprintController
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.data.WordDataProvider
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.typingsprint.TypingSprintEvent
import com.xanticious.androidgames.model.games.typingsprint.TypingSprintState
import com.xanticious.androidgames.state.games.typingsprint.TypingSprintPhase
import com.xanticious.androidgames.state.games.typingsprint.TypingSprintStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.HowToPlaySection
import com.xanticious.androidgames.view.common.WordGameHowToPlay
import com.xanticious.androidgames.view.common.WordGameSetup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

@Composable
fun TypingSprintGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { TypingSprintController() }
    val machine = remember { TypingSprintStateMachine() }
    val phase by machine.phase.collectAsState()

    var selectedDifficulty by rememberSaveable { mutableStateOf(difficulty) }
    var wordLength by rememberSaveable { mutableStateOf("Mixed") }

    val context = LocalContext.current
    var wordData by remember { mutableStateOf<WordData?>(null) }

    LaunchedEffect(Unit) {
        wordData = withContext(Dispatchers.Default) {
            WordDataProvider.get(context)
        }
    }

    when (phase) {
        TypingSprintPhase.SETUP -> {
            WordGameSetup(
                title = "Typing Sprint",
                difficulty = selectedDifficulty,
                onDifficulty = { selectedDifficulty = it },
                onHowToPlay = { machine.showHowToPlay() },
                onStart = {
                    if (wordData != null) machine.startPlaying()
                }
            ) {
                Text("Word length", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Short", "Mixed", "Long").forEach { len ->
                        FilterChip(
                            selected = wordLength == len,
                            onClick = { wordLength = len },
                            label = { Text(len) }
                        )
                    }
                }
            }
        }
        TypingSprintPhase.HOW_TO_PLAY -> {
            WordGameHowToPlay(
                title = "Typing Sprint",
                intro = "Words fall from the top. Type each word completely before it crosses the miss line to clear it and score points.",
                onBack = { machine.backToSetup() }
            ) {
                HowToPlaySection(title = "Objective") {
                    Text("Type falling words quickly and accurately before they reach the bottom.")
                }
                HowToPlaySection(title = "Controls") {
                    Text("• Type the word in the input field")
                    Text("• Press Enter or the word will auto-match when complete")
                    Text("• Cleared words add to your WPM and accuracy")
                }
                HowToPlaySection(title = "Game Over") {
                    Text("The game ends when you miss too many words.")
                }
                HowToPlaySection(title = "Stats") {
                    Text("WPM = (correct characters / 5) per minute")
                    Text("Accuracy = (correct / total) characters typed")
                }
            }
        }
        TypingSprintPhase.PLAYING -> {
            wordData?.let { data ->
                val (minLen, maxLen) = when (wordLength) {
                    "Short" -> 3 to 5
                    "Long" -> 6 to 10
                    else -> 3 to 8
                }
                TypingSprintPlayingScreen(
                    controller = controller,
                    wordData = data,
                    difficulty = selectedDifficulty,
                    minLength = minLen,
                    maxLength = maxLen,
                    onGameOver = { machine.gameEnded() },
                    onExit = onExit
                )
            } ?: run {
                Text("Loading word data...", modifier = Modifier.padding(16.dp))
            }
        }
        TypingSprintPhase.GAME_OVER -> {
            wordData?.let { data ->
                val (minLen, maxLen) = when (wordLength) {
                    "Short" -> 3 to 5
                    "Long" -> 6 to 10
                    else -> 3 to 8
                }
                TypingSprintResultsScreen(
                    controller = controller,
                    wordData = data,
                    difficulty = selectedDifficulty,
                    minLength = minLen,
                    maxLength = maxLen,
                    onReplay = { machine.restart() },
                    onMenu = { machine.backToSetup() },
                    onExit = onExit
                )
            }
        }
    }
}

@Composable
private fun TypingSprintPlayingScreen(
    controller: TypingSprintController,
    wordData: WordData,
    difficulty: GameDifficulty,
    minLength: Int,
    maxLength: Int,
    onGameOver: () -> Unit,
    onExit: () -> Unit
) {
    val config = remember(difficulty, minLength, maxLength) {
        controller.configFor(difficulty, minLength, maxLength)
    }
    var state by remember { mutableStateOf(TypingSprintState.initial()) }
    val random = remember { Random.Default }

    GameLoop(running = !state.gameOver) { dt ->
        val step = controller.step(state, config, dt, wordData, random)
        state = step.state
        if (step.event == TypingSprintEvent.GAME_OVER) {
            onGameOver()
        }
    }

    val wpm = controller.computeWpm(state)
    val accuracy = controller.computeAccuracy(state)

    GameScaffold(
        title = "Typing Sprint",
        onExit = onExit,
        hud = {
            GameHud(
                left = "WPM ${wpm.toInt()}",
                center = "Acc ${accuracy.toInt()}%",
                right = "Missed ${state.missedWords}/${config.maxMisses}"
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
                drawRect(color = GameCourt, size = size)
                
                drawLine(
                    color = Dark1.copy(alpha = 0.5f),
                    start = Offset(0f, size.height * 0.9f),
                    end = Offset(size.width, size.height * 0.9f),
                    strokeWidth = 3f
                )

                state.words.forEach { word ->
                    if (word.isActive) {
                        val px = word.x * size.width
                        val py = word.y * size.height
                        val textPaint = android.graphics.Paint().apply {
                            color = if (word.text == state.currentInput) Aqua3.hashCode() else Aqua0.hashCode()
                            textSize = 40f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawContext.canvas.nativeCanvas.drawText(word.text, px, py, textPaint)
                    }
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = state.currentInput,
                    onValueChange = { newValue ->
                        if (newValue.length > state.currentInput.length) {
                            val char = newValue.last()
                            state = controller.typeChar(state, char)
                        } else if (newValue.length < state.currentInput.length) {
                            state = controller.backspace(state)
                        }
                        val matchResult = controller.checkMatch(state)
                        state = matchResult.state
                    },
                    label = { Text("Type here") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TypingSprintResultsScreen(
    controller: TypingSprintController,
    wordData: WordData,
    difficulty: GameDifficulty,
    minLength: Int,
    maxLength: Int,
    onReplay: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit
) {
    val config = remember(difficulty, minLength, maxLength) {
        controller.configFor(difficulty, minLength, maxLength)
    }
    var state by remember { mutableStateOf(TypingSprintState.initial()) }

    val wpm = controller.computeWpm(state)
    val accuracy = controller.computeAccuracy(state)

    GameScaffold(
        title = "Typing Sprint",
        onExit = onExit,
        hud = {
            GameHud(
                left = "WPM ${wpm.toInt()}",
                center = "Acc ${accuracy.toInt()}%",
                right = "Cleared ${state.clearedWords}"
            )
        },
        status = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                DefeatPanel(
                    score = state.clearedWords,
                    bestScore = state.clearedWords,
                    onTryAgain = onReplay,
                    onMenu = onMenu,
                    headline = "Sprint Complete"
                )
            }
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = GameCourt, size = size)
        }
    }
}
