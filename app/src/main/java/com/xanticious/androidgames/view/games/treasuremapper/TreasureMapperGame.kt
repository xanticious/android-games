package com.xanticious.androidgames.view.games.treasuremapper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.treasuremapper.TreasureMapperController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.mapcommon.GridCell
import com.xanticious.androidgames.model.games.treasuremapper.TreasureMapperRound
import com.xanticious.androidgames.model.games.treasuremapper.TreasureTile
import com.xanticious.androidgames.state.games.treasuremapper.TreasureMapperPhase
import com.xanticious.androidgames.state.games.treasuremapper.TreasureMapperStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GameSuccess
import kotlin.math.min
import kotlin.random.Random

@Composable
fun TreasureMapperGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { TreasureMapperController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { TreasureMapperStateMachine() }
    val phase by machine.phase.collectAsState()
    val random = remember { Random(System.currentTimeMillis()) }
    var round by remember { mutableStateOf<TreasureMapperRound?>(null) }

    LaunchedEffect(Unit) { machine.startGame() }
    LaunchedEffect(phase) {
        if (phase == TreasureMapperPhase.GENERATING_MAP) {
            round = TreasureMapperRound.initial(controller.generateWorld(config, random), config.maxTries)
            machine.mapReady()
        }
    }

    GameScaffoldForTreasure(
        phase = phase,
        round = round,
        onExit = onExit,
        onHowTo = { machine.openHowToPlay() },
        onBack = { machine.backToSettings() },
        onStart = { machine.confirmSettings() },
        onCell = { cell ->
            round?.let { current ->
                round = controller.selectCell(current, cell)
                machine.cellSelected()
            }
        },
        onDig = {
            round?.let { current ->
                machine.digSubmitted()
                val next = controller.submitDig(current)
                round = next
                when {
                    next.solved -> machine.correctDig()
                    next.revealed -> machine.wrongDigNoTries()
                    else -> machine.wrongDigWithTries()
                }
            }
        },
        onNewMap = { machine.newMap() }
    )
}

@Composable
private fun GameScaffoldForTreasure(
    phase: TreasureMapperPhase,
    round: TreasureMapperRound?,
    onExit: () -> Unit,
    onHowTo: () -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onCell: (GridCell) -> Unit,
    onDig: () -> Unit,
    onNewMap: () -> Unit
) {
    com.xanticious.androidgames.view.common.GameScaffold(
        title = "Treasure Mapper",
        onExit = onExit,
        hud = {
            val tries = round?.triesRemaining ?: 3
            com.xanticious.androidgames.view.common.GameHud(
                left = "Map clues",
                center = "Tries ${"♥".repeat(tries)}",
                right = if (round?.world?.clue?.landmark != null) "Compass N↑" else "Settings"
            )
        },
        status = {
            TreasureMapperStatus(phase, round, onHowTo, onBack, onStart, onDig, onNewMap)
        }
    ) {
        when (phase) {
            TreasureMapperPhase.SETTINGS -> CenterText("Choose a map size and clue complexity from difficulty, then start.")
            TreasureMapperPhase.HOW_TO_PLAY -> CenterText("Read the clue, count cells from the landmark, tap your target, then dig.")
            else -> round?.let { TreasureMapCanvas(it, onCell) } ?: CenterText("Drawing map…")
        }
    }
}

@Composable
private fun TreasureMapperStatus(
    phase: TreasureMapperPhase,
    round: TreasureMapperRound?,
    onHowTo: () -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onDig: () -> Unit,
    onNewMap: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (phase) {
            TreasureMapperPhase.SETTINGS -> {
                Text("Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Difficulty selects grid size and clue complexity. Compass is enabled.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onStart) { Text("Start Map") }
                    OutlinedButton(onClick = onHowTo) { Text("How to Play") }
                }
            }
            TreasureMapperPhase.HOW_TO_PLAY -> {
                Text("How to Play", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Identify the named landmark, follow cardinal paces, select the cell, and tap Dig Here.")
                OutlinedButton(onClick = onBack) { Text("Back") }
            }
            TreasureMapperPhase.ROUND_COMPLETE -> {
                val result = round?.result
                com.xanticious.androidgames.view.common.VictoryPanel(
                    score = result?.score ?: 0,
                    bestScore = result?.score ?: 0,
                    stars = (4 - (result?.triesUsed ?: 3)).coerceIn(1, 3),
                    onReplay = onNewMap,
                    onMenu = onNewMap,
                    headline = "Treasure Found!",
                    primaryLabel = "New Map"
                )
            }
            TreasureMapperPhase.ROUND_FAILED -> {
                com.xanticious.androidgames.view.common.DefeatPanel(
                    score = 0,
                    bestScore = 0,
                    onTryAgain = onNewMap,
                    onMenu = onNewMap,
                    headline = "Treasure Revealed"
                )
            }
            else -> {
                Text(round?.world?.clue?.text ?: "Generating clue…", color = Dark1, fontWeight = FontWeight.Bold)
                Button(onClick = onDig, enabled = round?.selected != null) { Text("Dig Here") }
            }
        }
    }
}

@Composable
private fun CenterText(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun TreasureMapCanvas(round: TreasureMapperRound, onCell: (GridCell) -> Unit) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .pointerInput(round.world.size) {
                detectTapGestures { offset ->
                    val side = min(size.width, size.height).toFloat()
                    val origin = Offset((size.width - side) / 2f, (size.height - side) / 2f)
                    val cellSize = side / round.world.size.columns
                    val x = ((offset.x - origin.x) / cellSize).toInt()
                    val y = ((offset.y - origin.y) / cellSize).toInt()
                    val cell = GridCell(x, y)
                    if (round.world.size.contains(cell)) onCell(cell)
                }
            }
    ) {
        val side = min(size.width, size.height)
        val origin = Offset((size.width - side) / 2f, (size.height - side) / 2f)
        val cellSize = side / round.world.size.columns
        drawRect(GameCourt, topLeft = origin, size = Size(side, side))
        round.world.size.cells.forEach { cell ->
            val topLeft = Offset(origin.x + cell.x * cellSize, origin.y + cell.y * cellSize)
            val center = Offset(topLeft.x + cellSize / 2f, topLeft.y + cellSize / 2f)
            drawRect(tileColor(round.world.tileAt(cell)), topLeft = topLeft, size = Size(cellSize, cellSize))
            drawRect(GameCourtLine.copy(alpha = 0.35f), topLeft = topLeft, size = Size(cellSize, cellSize), style = Stroke(1f))
            when (round.world.tileAt(cell)) {
                TreasureTile.BIG_TREE -> drawCircle(GameSuccess, cellSize * 0.32f, center)
                TreasureTile.SMALL_TREE -> drawCircle(GameSuccess, cellSize * 0.22f, center)
                TreasureTile.ROCK -> drawRoundRect(GameNeutral, topLeft + Offset(cellSize * 0.22f, cellSize * 0.28f), Size(cellSize * 0.56f, cellSize * 0.44f), CornerRadius(cellSize * 0.15f))
                TreasureTile.FENCE_POST -> drawRect(GameHazard, topLeft + Offset(cellSize * 0.42f, cellSize * 0.22f), Size(cellSize * 0.16f, cellSize * 0.56f))
                else -> Unit
            }
            if (cell in round.wrongDigs) {
                drawLine(GameEnemy, topLeft + Offset(cellSize * 0.25f, cellSize * 0.25f), topLeft + Offset(cellSize * 0.75f, cellSize * 0.75f), strokeWidth = 5f)
                drawLine(GameEnemy, topLeft + Offset(cellSize * 0.75f, cellSize * 0.25f), topLeft + Offset(cellSize * 0.25f, cellSize * 0.75f), strokeWidth = 5f)
            }
            if ((round.solved || round.revealed) && cell == round.world.treasure) {
                drawCircle(GameAccent.copy(alpha = 0.35f), cellSize * 0.45f, center)
                drawRoundRect(GameAccent, topLeft + Offset(cellSize * 0.24f, cellSize * 0.34f), Size(cellSize * 0.52f, cellSize * 0.36f), CornerRadius(cellSize * 0.08f))
            }
            if (round.selected == cell) {
                drawRect(Aqua3, topLeft = topLeft + Offset(2f, 2f), size = Size(cellSize - 4f, cellSize - 4f), style = Stroke(5f))
            }
        }
        drawCircle(Aqua2, radius = cellSize * 0.35f, center = origin + Offset(side - cellSize * 0.7f, cellSize * 0.7f), style = Stroke(3f))
    }
}

private fun tileColor(tile: TreasureTile) = when (tile) {
    TreasureTile.CLEARING -> Aqua0
    TreasureTile.BIG_TREE -> Aqua1
    TreasureTile.SMALL_TREE -> Aqua1.copy(alpha = 0.85f)
    TreasureTile.ROCK -> GameNeutral.copy(alpha = 0.65f)
    TreasureTile.WATER -> Aqua3.copy(alpha = 0.8f)
    TreasureTile.FENCE_POST -> GameHazard.copy(alpha = 0.55f)
}
