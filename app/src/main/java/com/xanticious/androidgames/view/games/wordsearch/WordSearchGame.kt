package com.xanticious.androidgames.view.games.wordsearch

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.wordsearch.WordSearchController
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.data.WordDataProvider
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.wordsearch.GridPosition
import com.xanticious.androidgames.model.games.wordsearch.SelectionState
import com.xanticious.androidgames.model.games.wordsearch.WordSearchGrid
import com.xanticious.androidgames.model.games.wordsearch.WordSearchState
import com.xanticious.androidgames.state.games.wordsearch.WordSearchPhase
import com.xanticious.androidgames.state.games.wordsearch.WordSearchStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.HowToPlaySection
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.WordGameHowToPlay
import com.xanticious.androidgames.view.common.WordGameSetup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min

@Composable
fun WordSearchGame(
    difficulty: GameDifficulty,
    onExit: () -> Unit
) {
    val machine = remember { WordSearchStateMachine() }
    val phase by machine.phase.collectAsState()
    val gameState by machine.gameState.collectAsState()
    var selectedDifficulty by rememberSaveable { mutableStateOf(difficulty) }

    LaunchedEffect(selectedDifficulty) {
        machine.setDifficulty(selectedDifficulty)
    }

    when (phase) {
        WordSearchPhase.SETUP -> WordSearchSetup(
            difficulty = selectedDifficulty,
            onDifficultyChanged = { selectedDifficulty = it },
            onHowToPlay = { machine.showHowToPlay() },
            onStart = { state -> machine.startGame(state) }
        )
        WordSearchPhase.HOW_TO_PLAY -> WordSearchHowToPlayScreen(
            onBack = { machine.backToSetup() }
        )
        WordSearchPhase.PLAYING,
        WordSearchPhase.SOLVED,
        WordSearchPhase.TIME_UP -> {
            if (gameState != null) {
                WordSearchGameplay(
                    initialState = gameState!!,
                    phase = phase,
                    onAllWordsFound = { machine.allWordsFound() },
                    onTimeExpired = { machine.timeExpired() },
                    onNewGame = { machine.newGame() },
                    onExit = onExit,
                    onStateChange = { machine.updateGameState(it) }
                )
            }
        }
    }
}

@Composable
private fun WordSearchSetup(
    difficulty: GameDifficulty,
    onDifficultyChanged: (GameDifficulty) -> Unit,
    onHowToPlay: () -> Unit,
    onStart: (WordSearchState) -> Unit
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

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading...", style = MaterialTheme.typography.headlineSmall)
        }
        return
    }

    WordGameSetup(
        title = "Word Search",
        difficulty = difficulty,
        onDifficulty = onDifficultyChanged,
        onHowToPlay = onHowToPlay,
        onStart = {
            val controller = WordSearchController()
            val config = controller.configFor(difficulty)
            val grid = controller.generateGrid(wordData!!, config, System.currentTimeMillis())
            val state = WordSearchState(
                grid = grid,
                targetWords = grid.placedWords.map { it.word },
                foundWords = emptySet(),
                currentSelection = SelectionState(null, null),
                timeRemainingSeconds = config.timeLimitSeconds
            )
            onStart(state)
        }
    )
}

@Composable
private fun WordSearchHowToPlayScreen(onBack: () -> Unit) {
    WordGameHowToPlay(
        title = "Word Search",
        intro = "Find all hidden words in the grid before time runs out.",
        onBack = onBack
    ) {
        HowToPlaySection(title = "Goal") {
            Text("Locate and select every word from the list. Words can appear horizontally, vertically, diagonally, and backwards depending on difficulty.")
        }
        HowToPlaySection(title = "Controls") {
            Text("Drag from a word's first letter to its last letter. Release to submit. Correct selections are marked with colored pills; incorrect ones clear without penalty.")
        }
        HowToPlaySection(title = "First-Letter Rule") {
            Text("Each word starts on a unique cell. No remaining word begins inside a found word's pill, letting you scan past completed areas.")
        }
    }
}

@Composable
private fun WordSearchGameplay(
    initialState: WordSearchState,
    phase: WordSearchPhase,
    onAllWordsFound: () -> Unit,
    onTimeExpired: () -> Unit,
    onNewGame: () -> Unit,
    onExit: () -> Unit,
    onStateChange: (WordSearchState) -> Unit
) {
    var state by rememberSaveable { mutableStateOf(initialState) }
    var dragStart by remember { mutableStateOf<GridPosition?>(null) }
    var dragCurrent by remember { mutableStateOf<GridPosition?>(null) }
    val controller = remember { WordSearchController() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.allWordsFound) {
        if (state.allWordsFound && phase == WordSearchPhase.PLAYING) {
            onAllWordsFound()
        }
    }

    LaunchedEffect(state.timeRemainingSeconds) {
        if (state.timeRemainingSeconds <= 0 && phase == WordSearchPhase.PLAYING) {
            onTimeExpired()
        }
    }

    GameLoop(running = phase == WordSearchPhase.PLAYING) { dt ->
        val newTime = (state.timeRemainingSeconds - dt.toInt()).coerceAtLeast(0)
        if (newTime != state.timeRemainingSeconds) {
            state = state.copy(timeRemainingSeconds = newTime)
            onStateChange(state)
        }
    }

    GameScaffold(
        title = "Word Search",
        onExit = onExit,
        hud = {
            GameHud(
                left = { Text("Found: ${state.foundWords.size}/${state.targetWords.size}") },
                center = {},
                right = {
                    val mins = state.timeRemainingSeconds / 60
                    val secs = state.timeRemainingSeconds % 60
                    Text("Time: %d:%02d".format(mins, secs))
                }
            )
        },
        status = {
            when (phase) {
                WordSearchPhase.SOLVED -> VictoryPanel(
                    message = "Puzzle Solved!",
                    onContinue = onNewGame,
                    continueLabel = "New Puzzle"
                )
                WordSearchPhase.TIME_UP -> DefeatPanel(
                    message = "Time's Up!",
                    onRetry = onNewGame,
                    retryLabel = "Try Again"
                )
                else -> {}
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WordSearchGridView(
                grid = state.grid,
                foundWords = state.foundWords,
                dragStart = dragStart,
                dragCurrent = dragCurrent,
                onDragStart = { pos -> dragStart = pos },
                onDragUpdate = { pos -> dragCurrent = pos },
                onDragEnd = {
                    if (dragStart != null && dragCurrent != null) {
                        val remaining = state.targetWords.filter { it !in state.foundWords }
                        val result = controller.validateSelection(
                            state.grid,
                            dragStart!!,
                            dragCurrent!!,
                            remaining
                        )
                        if (result.isValid && result.matchedWord != null) {
                            state = state.copy(
                                foundWords = state.foundWords + result.matchedWord
                            )
                            onStateChange(state)
                        }
                    }
                    dragStart = null
                    dragCurrent = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            WordListView(
                targetWords = state.targetWords,
                foundWords = state.foundWords,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WordSearchGridView(
    grid: WordSearchGrid,
    foundWords: Set<String>,
    dragStart: GridPosition?,
    dragCurrent: GridPosition?,
    onDragStart: (GridPosition) -> Unit,
    onDragUpdate: (GridPosition) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val foundPlacedWords = grid.placedWords.filter { it.word in foundWords }
    val pillColors = listOf(Aqua0, Aqua1, Aqua2, Aqua3, Aqua4)

    Card(
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = Dark1),
        shape = RoundedCornerShape(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val cellSize = size.width / grid.size
                            val col = (offset.x / cellSize).toInt().coerceIn(0, grid.size - 1)
                            val row = (offset.y / cellSize).toInt().coerceIn(0, grid.size - 1)
                            onDragStart(GridPosition(row, col))
                        },
                        onDrag = { change, _ ->
                            val cellSize = size.width / grid.size
                            val col = (change.position.x / cellSize)
                                .toInt()
                                .coerceIn(0, grid.size - 1)
                            val row = (change.position.y / cellSize)
                                .toInt()
                                .coerceIn(0, grid.size - 1)
                            onDragUpdate(GridPosition(row, col))
                        },
                        onDragEnd = { onDragEnd() }
                    )
                }
        ) {
            val cellSize = size.width / grid.size

            for (r in 0 until grid.size) {
                for (c in 0 until grid.size) {
                    val x = c * cellSize
                    val y = r * cellSize

                    drawRect(
                        color = Dark0,
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                    )

                    drawRect(
                        color = Dark2,
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(cellSize, cellSize),
                        style = Stroke(width = 1f)
                    )
                }
            }

            foundPlacedWords.forEachIndexed { index, placed ->
                val color = pillColors[index % pillColors.size].copy(alpha = 0.4f)
                val positions = (0 until placed.word.length).map { i ->
                    GridPosition(
                        placed.startPos.row + i * placed.direction.dr,
                        placed.startPos.col + i * placed.direction.dc
                    )
                }

                if (positions.size >= 2) {
                    val startX = positions.first().col * cellSize + cellSize / 2
                    val startY = positions.first().row * cellSize + cellSize / 2
                    val endX = positions.last().col * cellSize + cellSize / 2
                    val endY = positions.last().row * cellSize + cellSize / 2

                    drawLine(
                        color = color,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = cellSize * 0.7f,
                        cap = StrokeCap.Round
                    )
                }
            }

            if (dragStart != null && dragCurrent != null) {
                val startX = dragStart.col * cellSize + cellSize / 2
                val startY = dragStart.row * cellSize + cellSize / 2
                val endX = dragCurrent.col * cellSize + cellSize / 2
                val endY = dragCurrent.row * cellSize + cellSize / 2

                drawLine(
                    color = Aqua2.copy(alpha = 0.6f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = cellSize * 0.5f,
                    cap = StrokeCap.Round
                )
            }

            for (r in 0 until grid.size) {
                for (c in 0 until grid.size) {
                    val x = c * cellSize + cellSize / 2
                    val y = r * cellSize + cellSize / 2
                    val char = grid.cells[r][c].toString()

                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = cellSize * 0.5f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                        val textY = y + (paint.textSize / 3)
                        drawText(char, x, textY, paint)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordListView(
    targetWords: List<String>,
    foundWords: Set<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Dark1),
        shape = RoundedCornerShape(8.dp)
    ) {
        FlowRow(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            targetWords.forEach { word ->
                val isFound = word in foundWords
                Text(
                    text = word.uppercase(),
                    fontSize = 14.sp,
                    fontWeight = if (isFound) FontWeight.Normal else FontWeight.Bold,
                    textDecoration = if (isFound) TextDecoration.LineThrough else null,
                    color = if (isFound) Aqua2.copy(alpha = 0.5f) else Aqua0
                )
            }
        }
    }
}
