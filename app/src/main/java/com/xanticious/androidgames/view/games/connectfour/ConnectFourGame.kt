package com.xanticious.androidgames.view.games.connectfour

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.connectfour.ConnectFourController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.connectfour.ConnectFourResult
import com.xanticious.androidgames.model.games.connectfour.ConnectFourState
import com.xanticious.androidgames.model.games.connectfour.Disc
import com.xanticious.androidgames.state.games.connectfour.ConnectFourPhase
import com.xanticious.androidgames.state.games.connectfour.ConnectFourStateMachine
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.min
import kotlin.random.Random

/** Connect Four — classic 7×6 vertical alignment game against local AI. */
@Composable
fun ConnectFourGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { ConnectFourController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { ConnectFourStateMachine() }
    val phase by machine.phase.collectAsState()

    var state by remember { mutableStateOf(ConnectFourState.initial(config)) }

    fun resetMatch() {
        state = ConnectFourState.initial(config)
        machine.reset()
    }

    LaunchedEffect(phase, state.moveCount) {
        if (phase == ConnectFourPhase.AI_TURN && !state.isGameOver) {
            delay(250L)
            val column = controller.aiMove(
                state = state,
                config = config,
                difficulty = difficulty,
                random = Random(seed = 73 + state.moveCount + difficulty.ordinal * 1_000)
            )
            if (column != null) {
                val step = controller.dropDisc(state, config, column, Disc.AI)
                if (step.accepted) {
                    state = step.state
                    if (step.state.isGameOver) machine.gameEnded() else machine.aiMoved()
                }
            }
        }
    }

    GameScaffold(
        title = "Connect Four",
        onExit = onExit,
        hud = {
            GameHud(
                left = "You ${state.grid.sumOf { row -> row.count { it == Disc.PLAYER } }}",
                center = when (phase) {
                    ConnectFourPhase.PLAYER_TURN -> "Your turn"
                    ConnectFourPhase.AI_TURN -> "AI thinking"
                    ConnectFourPhase.GAME_OVER -> "Game over"
                },
                right = "AI ${state.grid.sumOf { row -> row.count { it == Disc.AI } }}  ${difficulty.label}"
            )
        },
        status = {
            ConnectFourStatus(
                result = state.result,
                moves = state.moveCount,
                onReplay = ::resetMatch,
                onExit = onExit
            )
        }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(phase, state.moveCount) {
                    detectTapGestures { offset ->
                        if (phase != ConnectFourPhase.PLAYER_TURN || state.isGameOver) return@detectTapGestures
                        val cell = min(size.width / config.columns.toFloat(), size.height / config.rows.toFloat())
                        val boardWidth = cell * config.columns
                        val boardLeft = (size.width - boardWidth) / 2f
                        val relativeX = offset.x - boardLeft
                        if (relativeX < 0f || relativeX >= boardWidth) return@detectTapGestures
                        val column = floor(relativeX / cell).toInt().coerceIn(0, config.columns - 1)
                        val step = controller.dropDisc(state, config, column, Disc.PLAYER)
                        if (step.accepted) {
                            state = step.state
                            if (step.state.isGameOver) machine.gameEnded() else machine.playerMoved()
                        }
                    }
                }
        ) {
            val cell = min(size.width / config.columns.toFloat(), size.height / config.rows.toFloat())
            val boardWidth = cell * config.columns
            val boardHeight = cell * config.rows
            val left = (size.width - boardWidth) / 2f
            val top = (size.height - boardHeight) / 2f
            val radius = cell * 0.36f
            drawRect(color = GameCourt, size = Size(size.width, size.height))
            drawRoundRect(
                color = Dark1,
                topLeft = Offset(left, top),
                size = Size(boardWidth, boardHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * 0.12f, cell * 0.12f)
            )
            for (row in 0 until config.rows) {
                for (column in 0 until config.columns) {
                    val center = Offset(left + column * cell + cell / 2f, top + row * cell + cell / 2f)
                    val slot = state.grid[row][column]
                    val isWinning = state.winningLine.any { it.row == row && it.column == column }
                    if (isWinning) drawCircle(color = GameSuccess, radius = radius * 1.18f, center = center)
                    drawCircle(color = GameCourtLine, radius = radius * 1.05f, center = center)
                    drawCircle(
                        color = when (slot) {
                            Disc.EMPTY -> GameNeutral
                            Disc.PLAYER -> GamePlayer
                            Disc.AI -> GameAccent
                        },
                        radius = radius,
                        center = center
                    )
                    if (slot != Disc.EMPTY) {
                        drawCircle(color = GameCourt, radius = radius, center = center, style = Stroke(width = cell * 0.04f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectFourStatus(
    result: ConnectFourResult,
    moves: Int,
    onReplay: () -> Unit,
    onExit: () -> Unit
) {
    when (result) {
        ConnectFourResult.PLAYER_WIN -> Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            VictoryPanel(score = moves, bestScore = moves, stars = 3, onReplay = onReplay, onMenu = onExit, headline = "You Win!")
        }
        ConnectFourResult.AI_WIN -> Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            DefeatPanel(score = moves, bestScore = moves, onTryAgain = onReplay, onMenu = onExit, headline = "You Lose")
        }
        ConnectFourResult.DRAW -> Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = "Draw in $moves moves")
            Row(modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = onReplay) { Text("Replay") }
                OutlinedButton(onClick = onExit, modifier = Modifier.padding(start = 8.dp)) { Text("Menu") }
            }
        }
        ConnectFourResult.NONE -> Text(
            text = "Choose a column.",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
