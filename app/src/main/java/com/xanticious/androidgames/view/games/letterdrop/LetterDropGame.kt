package com.xanticious.androidgames.view.games.letterdrop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.letterdrop.LetterDropController
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.data.WordDataProvider
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.letterdrop.LetterDropEvent
import com.xanticious.androidgames.model.games.letterdrop.LetterDropState
import com.xanticious.androidgames.state.games.letterdrop.LetterDropPhase
import com.xanticious.androidgames.state.games.letterdrop.LetterDropStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.DifficultyChips
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
fun LetterDropGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { LetterDropController() }
    val machine = remember { LetterDropStateMachine() }
    val phase by machine.phase.collectAsState()

    var selectedDifficulty by rememberSaveable { mutableStateOf(difficulty) }
    var minWordLength by rememberSaveable { mutableStateOf(3) }
    var vowelRich by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    var wordData by remember { mutableStateOf<WordData?>(null) }

    LaunchedEffect(Unit) {
        wordData = withContext(Dispatchers.Default) {
            WordDataProvider.get(context)
        }
    }

    when (phase) {
        LetterDropPhase.SETUP -> {
            WordGameSetup(
                title = "Letter Drop",
                difficulty = selectedDifficulty,
                onDifficulty = { selectedDifficulty = it },
                onHowToPlay = { machine.showHowToPlay() },
                onStart = {
                    if (wordData != null) machine.startPlaying()
                }
            ) {
                Text("Minimum word length", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 4).forEach { len ->
                        FilterChip(
                            selected = minWordLength == len,
                            onClick = { minWordLength = len },
                            label = { Text("$len") }
                        )
                    }
                }
                Text("Letter distribution", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !vowelRich,
                        onClick = { vowelRich = false },
                        label = { Text("Balanced") }
                    )
                    FilterChip(
                        selected = vowelRich,
                        onClick = { vowelRich = true },
                        label = { Text("Vowel-rich") }
                    )
                }
            }
        }
        LetterDropPhase.HOW_TO_PLAY -> {
            WordGameHowToPlay(
                title = "Letter Drop",
                intro = "Letters fall from the top and you must tap them in order to spell words before they pile up.",
                onBack = { machine.backToSetup() }
            ) {
                HowToPlaySection(title = "Objective") {
                    Text("Tap falling tiles to build words, then Submit to clear them and score points.")
                }
                HowToPlaySection(title = "Controls") {
                    Text("• Tap a tile to queue its letter")
                    Text("• Backspace removes the last letter")
                    Text("• Submit validates and clears valid words")
                    Text("• Clear cancels your current entry")
                }
                HowToPlaySection(title = "Game Over") {
                    Text("The game ends when tiles stack to the top of the screen.")
                }
                HowToPlaySection(title = "Scoring") {
                    Text("Longer words score more points. Form long words to maximize your score.")
                }
            }
        }
        LetterDropPhase.PLAYING -> {
            wordData?.let { data ->
                LetterDropPlayingScreen(
                    controller = controller,
                    wordData = data,
                    difficulty = selectedDifficulty,
                    minWordLength = minWordLength,
                    vowelRich = vowelRich,
                    onOverflow = { machine.overflow() },
                    onExit = onExit
                )
            } ?: run {
                Text("Loading word data...", modifier = Modifier.padding(16.dp))
            }
        }
        LetterDropPhase.GAME_OVER -> {
            wordData?.let { data ->
                LetterDropResultsScreen(
                    controller = controller,
                    wordData = data,
                    difficulty = selectedDifficulty,
                    minWordLength = minWordLength,
                    vowelRich = vowelRich,
                    onReplay = { machine.restart() },
                    onMenu = { machine.backToSetup() },
                    onExit = onExit
                )
            }
        }
    }
}

@Composable
private fun LetterDropPlayingScreen(
    controller: LetterDropController,
    wordData: WordData,
    difficulty: GameDifficulty,
    minWordLength: Int,
    vowelRich: Boolean,
    onOverflow: () -> Unit,
    onExit: () -> Unit
) {
    val config = remember(difficulty, minWordLength, vowelRich) {
        controller.configFor(difficulty, minWordLength, vowelRich)
    }
    var state by remember { mutableStateOf(LetterDropState.initial()) }
    val random = remember { Random.Default }

    GameLoop(running = !state.gameOver) { dt ->
        val step = controller.step(state, config, dt, random)
        state = step.state
        if (step.event == LetterDropEvent.OVERFLOW) {
            onOverflow()
        }
    }

    GameScaffold(
        title = "Letter Drop",
        onExit = onExit,
        hud = {
            GameHud(
                left = "Score ${state.score}",
                center = "Words ${state.totalWordsFormed}",
                right = "Time ${state.elapsedSeconds.toInt()}s"
            )
        },
        status = null
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val x = offset.x / size.width
                            val y = offset.y / size.height
                            state.tiles.forEach { tile ->
                                if (!tile.isQueued) {
                                    val dx = x - tile.x
                                    val dy = y - tile.y
                                    if (dx * dx + dy * dy < 0.002f) {
                                        state = controller.queueTile(state, tile.id)
                                    }
                                }
                            }
                        }
                    }
            ) {
                drawRect(color = GameCourt, size = size)
                val tileSize = size.width * 0.06f
                state.tiles.forEach { tile ->
                    val px = tile.x * size.width
                    val py = tile.y * size.height
                    drawRect(
                        color = if (tile.isQueued) Aqua3 else Aqua0,
                        topLeft = Offset(px - tileSize / 2, py - tileSize / 2),
                        size = Size(tileSize, tileSize)
                    )
                    if (tile.isQueued) {
                        drawRect(
                            color = Aqua3,
                            topLeft = Offset(px - tileSize / 2, py - tileSize / 2),
                            size = Size(tileSize, tileSize),
                            style = Stroke(width = 4f)
                        )
                    }
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = Dark1.hashCode()
                            textSize = tileSize * 0.6f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawText(
                            tile.letter.uppercase(),
                            px,
                            py + tileSize * 0.2f,
                            paint
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Current entry: ${state.currentEntry}", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    TextButton(onClick = { state = controller.backspace(state) }) {
                        Text("Backspace")
                    }
                    Button(onClick = {
                        val step = controller.submitWord(state, config, wordData)
                        state = step.state
                    }) {
                        Text("Submit")
                    }
                    OutlinedButton(onClick = { state = controller.clear(state) }) {
                        Text("Clear")
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterDropResultsScreen(
    controller: LetterDropController,
    wordData: WordData,
    difficulty: GameDifficulty,
    minWordLength: Int,
    vowelRich: Boolean,
    onReplay: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit
) {
    val config = remember(difficulty, minWordLength, vowelRich) {
        controller.configFor(difficulty, minWordLength, vowelRich)
    }
    var state by remember { mutableStateOf(LetterDropState.initial()) }

    GameScaffold(
        title = "Letter Drop",
        onExit = onExit,
        hud = {
            GameHud(
                left = "Score ${state.score}",
                center = "Words ${state.totalWordsFormed}",
                right = "Time ${state.elapsedSeconds.toInt()}s"
            )
        },
        status = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                DefeatPanel(
                    score = state.score,
                    bestScore = state.score,
                    onTryAgain = onReplay,
                    onMenu = onMenu,
                    headline = "Game Over"
                )
            }
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = GameCourt, size = size)
        }
    }
}
