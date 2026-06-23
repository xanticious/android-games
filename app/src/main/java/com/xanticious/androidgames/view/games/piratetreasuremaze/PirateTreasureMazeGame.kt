package com.xanticious.androidgames.view.games.piratetreasuremaze

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
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
import com.xanticious.androidgames.controller.games.piratetreasuremaze.PirateTreasureMazeController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.mapcommon.GridDirection
import com.xanticious.androidgames.model.games.piratetreasuremaze.PirateMazeRun
import com.xanticious.androidgames.state.games.piratetreasuremaze.PirateTreasureMazePhase
import com.xanticious.androidgames.state.games.piratetreasuremaze.PirateTreasureMazeStateMachine
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
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.GameLoop
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

@Composable
fun PirateTreasureMazeGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { PirateTreasureMazeController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { PirateTreasureMazeStateMachine() }
    val phase by machine.phase.collectAsState()
    val random = remember { Random(System.currentTimeMillis()) }
    var run by remember { mutableStateOf<PirateMazeRun?>(null) }

    LaunchedEffect(Unit) { machine.startGame() }
    LaunchedEffect(phase) {
        if (phase == PirateTreasureMazePhase.GENERATING) {
            run = PirateMazeRun.initial(controller.generateMaze(config, random))
            machine.mazeReady()
        }
    }

    GameLoop(running = phase == PirateTreasureMazePhase.PLAYING) { dt ->
        run = run?.let { controller.tick(it, dt) }
    }

    com.xanticious.androidgames.view.common.GameScaffold(
        title = "Pirate Treasure Maze",
        onExit = onExit,
        hud = {
            val active = run
            com.xanticious.androidgames.view.common.GameHud(
                left = active?.maze?.algorithm?.tag ?: "Settings",
                center = formatTime(active?.elapsedSeconds ?: 0f),
                right = "Chests ${active?.openedChests?.size ?: 0}/${active?.maze?.chests?.size ?: config.chestCount}"
            )
        },
        status = {
            PirateMazeStatus(
                phase = phase,
                run = run,
                onStart = { machine.confirmSettings() },
                onHowTo = { machine.openHowToPlay() },
                onBack = { machine.backToSettings() },
                onMove = { direction ->
                    run?.let { current ->
                        val result = controller.move(current, direction)
                        run = result.run
                        if (result.completed) machine.mazeCompleted() else machine.moveMade()
                    }
                },
                onNewMaze = { machine.newMaze() },
                onExit = onExit
            )
        }
    ) {
        when (phase) {
            PirateTreasureMazePhase.SETTINGS -> CenterText("Difficulty selects maze size, chest count, and a random algorithm.")
            PirateTreasureMazePhase.HOW_TO_PLAY -> CenterText("Use the arrows or swipe the maze. Open every chest, then reach the gangplank exit.")
            else -> run?.let {
                PirateMazeCanvas(it) { direction ->
                    run?.let { current ->
                        val result = controller.move(current, direction)
                        run = result.run
                        if (result.completed) machine.mazeCompleted() else machine.moveMade()
                    }
                }
            } ?: CenterText("Generating maze…")
        }
    }
}

@Composable
private fun PirateMazeStatus(
    phase: PirateTreasureMazePhase,
    run: PirateMazeRun?,
    onStart: () -> Unit,
    onHowTo: () -> Unit,
    onBack: () -> Unit,
    onMove: (GridDirection) -> Unit,
    onNewMaze: () -> Unit,
    onExit: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (phase) {
            PirateTreasureMazePhase.SETTINGS -> {
                Text("Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Randomly uses Winding backtracker or Organic Prim mazes.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onStart) { Text("Start Maze") }
                    OutlinedButton(onClick = onHowTo) { Text("How to Play") }
                }
            }
            PirateTreasureMazePhase.HOW_TO_PLAY -> {
                Text("How to Play", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Move through open passages. Chests open when stepped on. The exit unlocks after all chests.")
                OutlinedButton(onClick = onBack) { Text("Back") }
            }
            PirateTreasureMazePhase.MAZE_COMPLETE -> {
                com.xanticious.androidgames.view.common.VictoryPanel(
                    score = run?.score ?: 0,
                    bestScore = run?.score ?: 0,
                    stars = when ((run?.score ?: 0)) { in 90..100 -> 3; in 70..89 -> 2; else -> 1 },
                    onReplay = onNewMaze,
                    onMenu = onExit,
                    headline = "Maze Cleared!",
                    primaryLabel = "New Maze"
                )
            }
            else -> DPad(onMove)
        }
    }
}

@Composable
private fun DPad(onMove: (GridDirection) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(onClick = { onMove(GridDirection.NORTH) }) { Text("↑") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onMove(GridDirection.WEST) }) { Text("←") }
            Button(onClick = { onMove(GridDirection.SOUTH) }) { Text("↓") }
            Button(onClick = { onMove(GridDirection.EAST) }) { Text("→") }
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
private fun PirateMazeCanvas(run: PirateMazeRun, onSwipe: (GridDirection) -> Unit) {
    var dragX = 0f
    var dragY = 0f
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .pointerInput(run.maze) {
                detectDragGestures(
                    onDragStart = { dragX = 0f; dragY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragX += dragAmount.x
                        dragY += dragAmount.y
                    },
                    onDragEnd = {
                        val direction = when {
                            abs(dragX) > abs(dragY) && dragX > 24f -> GridDirection.EAST
                            abs(dragX) > abs(dragY) && dragX < -24f -> GridDirection.WEST
                            abs(dragY) >= abs(dragX) && dragY > 24f -> GridDirection.SOUTH
                            abs(dragY) >= abs(dragX) && dragY < -24f -> GridDirection.NORTH
                            else -> null
                        }
                        direction?.let(onSwipe)
                    }
                )
            }
    ) {
        val side = min(size.width, size.height)
        val origin = Offset((size.width - side) / 2f, (size.height - side) / 2f)
        val cellSize = side / run.maze.size.columns
        drawRect(GameCourt, topLeft = origin, size = Size(side, side))
        run.maze.size.cells.forEach { cell ->
            val topLeft = Offset(origin.x + cell.x * cellSize, origin.y + cell.y * cellSize)
            val center = Offset(topLeft.x + cellSize / 2f, topLeft.y + cellSize / 2f)
            drawRect(Aqua0, topLeft = topLeft, size = Size(cellSize, cellSize))
            if (cell == run.maze.exit) {
                drawCircle(if (run.openedChests.size == run.maze.chests.size) GameAccent else GameEnemy, cellSize * 0.35f, center)
                drawRect(Aqua3, topLeft + Offset(cellSize * 0.25f, cellSize * 0.4f), Size(cellSize * 0.5f, cellSize * 0.2f))
            }
            if (cell in run.maze.chests) {
                val color = if (cell in run.openedChests) GameSuccess else GameAccent
                drawRoundRect(color, topLeft + Offset(cellSize * 0.25f, cellSize * 0.35f), Size(cellSize * 0.5f, cellSize * 0.35f), CornerRadius(cellSize * 0.08f))
            }
            val walls = run.maze.wallsAt(cell)
            if (walls.north) drawLine(GameHazard, topLeft, topLeft + Offset(cellSize, 0f), strokeWidth = 4f)
            if (walls.south) drawLine(GameHazard, topLeft + Offset(0f, cellSize), topLeft + Offset(cellSize, cellSize), strokeWidth = 4f)
            if (walls.east) drawLine(GameHazard, topLeft + Offset(cellSize, 0f), topLeft + Offset(cellSize, cellSize), strokeWidth = 4f)
            if (walls.west) drawLine(GameHazard, topLeft, topLeft + Offset(0f, cellSize), strokeWidth = 4f)
            drawRect(GameCourtLine.copy(alpha = 0.12f), topLeft = topLeft, size = Size(cellSize, cellSize), style = Stroke(1f))
        }
        val playerTopLeft = Offset(origin.x + run.player.x * cellSize, origin.y + run.player.y * cellSize)
        val playerCenter = playerTopLeft + Offset(cellSize / 2f, cellSize / 2f)
        drawCircle(GamePlayer, cellSize * 0.28f, playerCenter)
        drawRoundRect(Dark1, playerTopLeft + Offset(cellSize * 0.22f, cellSize * 0.18f), Size(cellSize * 0.56f, cellSize * 0.18f), CornerRadius(cellSize * 0.05f))
        if (run.maze.size.columns <= 16) {
            drawRect(Aqua1.copy(alpha = 0.25f), topLeft = origin + Offset(side - cellSize * 3f, cellSize * 0.4f), size = Size(cellSize * 2.4f, cellSize * 2.4f), style = Stroke(2f))
            drawCircle(Aqua2, cellSize * 0.08f, origin + Offset(side - cellSize * 1.8f, cellSize * 1.6f))
        }
    }
}

private fun formatTime(seconds: Float): String {
    val total = seconds.toInt()
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}
