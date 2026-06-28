package com.xanticious.androidgames.view.games.gomoku

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.gomoku.GomokuController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.gomoku.GomokuResult
import com.xanticious.androidgames.model.games.gomoku.GomokuState
import com.xanticious.androidgames.model.games.gomoku.Stone
import com.xanticious.androidgames.state.games.gomoku.GomokuPhase
import com.xanticious.androidgames.state.games.gomoku.GomokuStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun GomokuGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { GomokuController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { GomokuStateMachine() }
    val phase by machine.phase.collectAsState()
    val aiRandom = remember { Random(7L) }

    var state by remember { mutableStateOf(GomokuState.initial(config)) }
    var prompt by remember { mutableStateOf("Your turn. Create five in a row.") }

    fun resetMatch() {
        state = controller.initialState(config)
        prompt = "Your turn. Create five in a row."
        machine.reset()
    }

    fun applyMove(row: Int, col: Int) {
        val placement = controller.placeStone(state, row, col)
        prompt = placement.message.ifEmpty { if (state.turn == Stone.BLACK) "AI is thinking." else "Your turn. Create five in a row." }
        if (!placement.accepted) return
        state = placement.state
        if (placement.state.result == GomokuResult.IN_PROGRESS) {
            if (placement.state.turn == Stone.WHITE) machine.playerMoved() else machine.aiMoved()
        } else {
            machine.gameEnded()
        }
    }

    LaunchedEffect(phase, state.moveCount) {
        if (phase == GomokuPhase.AI_TURN && state.turn == Stone.WHITE && state.result == GomokuResult.IN_PROGRESS) {
            val move = controller.chooseAiMove(state, aiRandom)
            if (move != null) applyMove(move.row, move.col)
        }
    }

    GameScaffold(
        title = "Gomoku",
        onExit = onExit,
        hud = {
            GameHud(
                left = "You: Black ●",
                center = "${config.boardSize}×${config.boardSize} ${difficulty.label}",
                right = "AI: White ○"
            )
        },
        status = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                when (state.result) {
                    GomokuResult.BLACK_WON -> VictoryPanel(
                        score = state.moveCount,
                        bestScore = state.moveCount,
                        stars = 3,
                        onReplay = ::resetMatch,
                        onMenu = onExit,
                        headline = "Win"
                    )
                    GomokuResult.WHITE_WON -> DefeatPanel(
                        score = state.moveCount,
                        bestScore = state.moveCount,
                        onTryAgain = ::resetMatch,
                        onMenu = onExit,
                        headline = "Lose"
                    )
                    GomokuResult.DRAW -> DrawPanel(state.moveCount, ::resetMatch, onExit)
                    GomokuResult.IN_PROGRESS -> Text(
                        text = if (phase == GomokuPhase.AI_TURN) "AI is thinking." else prompt,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .pointerInput(phase, state.result, state.moveCount) {
                    detectTapGestures { offset ->
                        if (
                            phase != GomokuPhase.PLAYER_TURN ||
                            state.turn != Stone.BLACK ||
                            state.result != GomokuResult.IN_PROGRESS
                        ) return@detectTapGestures
                        val side = min(size.width, size.height).toFloat()
                        val left = (size.width - side) / 2f
                        val top = (size.height - side) / 2f
                        val boardPadding = side * 0.06f
                        val gridSize = side - boardPadding * 2f
                        val cell = gridSize / (config.boardSize - 1)
                        val col = ((offset.x - left - boardPadding) / cell).roundToInt()
                        val row = ((offset.y - top - boardPadding) / cell).roundToInt()
                        if (row in 0 until config.boardSize && col in 0 until config.boardSize) applyMove(row, col)
                    }
                }
        ) {
            val side = min(size.width, size.height)
            val left = (size.width - side) / 2f
            val top = (size.height - side) / 2f
            val boardPadding = side * 0.06f
            val gridSize = side - boardPadding * 2f
            val cell = gridSize / (config.boardSize - 1)
            val origin = Offset(left + boardPadding, top + boardPadding)
            val stoneRadius = cell * 0.38f

            drawRect(color = Dark1, topLeft = Offset(left, top), size = androidx.compose.ui.geometry.Size(side, side))
            for (index in 0 until config.boardSize) {
                val pos = index * cell
                drawLine(Aqua1, Offset(origin.x, origin.y + pos), Offset(origin.x + gridSize, origin.y + pos), strokeWidth = 2f)
                drawLine(Aqua1, Offset(origin.x + pos, origin.y), Offset(origin.x + pos, origin.y + gridSize), strokeWidth = 2f)
            }
            val starIndices = if (config.boardSize == 15) listOf(3, 7, 11) else listOf(3, 9, 15)
            for (row in starIndices) {
                for (col in starIndices) {
                    drawCircle(Aqua2, radius = stoneRadius * 0.18f, center = Offset(origin.x + col * cell, origin.y + row * cell))
                }
            }
            for (row in 0 until config.boardSize) {
                for (col in 0 until config.boardSize) {
                    val stone = state.board[row][col]
                    if (stone != Stone.EMPTY) {
                        val center = Offset(origin.x + col * cell, origin.y + row * cell)
                        if (stone == Stone.BLACK) {
                            drawCircle(Dark0, radius = stoneRadius, center = center)
                            drawCircle(Aqua2, radius = stoneRadius, center = center, style = Stroke(width = 2f))
                        } else {
                            drawCircle(Aqua0, radius = stoneRadius, center = center)
                            drawCircle(Dark2, radius = stoneRadius, center = center, style = Stroke(width = 2f))
                        }
                    }
                }
            }
            state.lastMove?.let { move ->
                drawCircle(
                    Aqua3,
                    radius = stoneRadius * 1.18f,
                    center = Offset(origin.x + move.col * cell, origin.y + move.row * cell),
                    style = Stroke(width = 3f)
                )
            }
            state.winningLine.forEach { move ->
                drawCircle(
                    Aqua3,
                    radius = stoneRadius * 1.32f,
                    center = Offset(origin.x + move.col * cell, origin.y + move.row * cell),
                    style = Stroke(width = 4f)
                )
            }
        }
    }
}

@Composable
private fun DrawPanel(score: Int, onReplay: () -> Unit, onMenu: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Draw", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(text = "Moves: $score")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onReplay) { Text("Replay") }
            OutlinedButton(onClick = onMenu) { Text("Menu") }
        }
    }
}
