package com.xanticious.androidgames.view.games.tictactoe

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.tictactoe.Mark
import com.xanticious.androidgames.model.games.tictactoe.TicTacToeResult
import com.xanticious.androidgames.model.games.tictactoe.TicTacToeState
import com.xanticious.androidgames.state.games.tictactoe.TicTacToePhase
import com.xanticious.androidgames.state.games.tictactoe.TicTacToeStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlin.math.min

@Composable
fun TicTacToeGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val machine = remember(difficulty) { TicTacToeStateMachine(difficulty) }
    val phase by machine.phase.collectAsState()
    val state by machine.state.collectAsState()
    val player = state.config.playerMark
    val ai = state.config.aiMark

    GameScaffold(
        title = "Tic Tac Toe",
        onExit = onExit,
        hud = {
            GameHud(
                left = "You: ${player.name}",
                center = when (phase) {
                    TicTacToePhase.PLAYER_TURN -> "${player.name} to move"
                    TicTacToePhase.AI_TURN -> "AI thinking"
                    TicTacToePhase.GAME_OVER -> "Round over"
                },
                right = "AI: ${ai.name}  ${difficulty.label}"
            )
        },
        status = {
            TicTacToeStatus(
                state = state,
                phase = phase,
                onReplay = machine::reset,
                onExit = onExit
            )
        }
    ) {
        TicTacToeBoard(
            state = state,
            enabled = phase == TicTacToePhase.PLAYER_TURN,
            onCellSelected = machine::selectCell,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun TicTacToeStatus(
    state: TicTacToeState,
    phase: TicTacToePhase,
    onReplay: () -> Unit,
    onExit: () -> Unit
) {
    when (val result = state.result) {
        TicTacToeResult.InProgress -> Text(
            text = if (phase == TicTacToePhase.PLAYER_TURN) "Choose a square." else "AI is choosing a square.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            fontWeight = FontWeight.Bold
        )
        TicTacToeResult.Draw -> DrawPanel(onReplay = onReplay, onExit = onExit)
        is TicTacToeResult.Win -> {
            if (result.winner == state.config.playerMark) {
                VictoryPanel(
                    score = state.board.count { it == state.config.playerMark },
                    bestScore = state.board.count { it == state.config.playerMark },
                    stars = 3,
                    onReplay = onReplay,
                    onMenu = onExit,
                    headline = "You Win!"
                )
            } else {
                DefeatPanel(
                    score = state.board.count { it == state.config.aiMark },
                    bestScore = state.board.count { it == state.config.aiMark },
                    onTryAgain = onReplay,
                    onMenu = onExit,
                    headline = "AI Wins"
                )
            }
        }
    }
}

@Composable
private fun DrawPanel(onReplay: () -> Unit, onExit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Draw",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = "No line completed before the board filled.", modifier = Modifier.padding(top = 8.dp))
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = onReplay) { Text("Replay") }
            OutlinedButton(onClick = onExit, modifier = Modifier.padding(start = 8.dp)) { Text("Menu") }
        }
    }
}

@Composable
private fun TicTacToeBoard(
    state: TicTacToeState,
    enabled: Boolean,
    onCellSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .sizeIn(minWidth = 240.dp, minHeight = 240.dp)
            .pointerInput(enabled, state.board) {
                detectTapGestures { offset ->
                    if (!enabled) return@detectTapGestures
                    val side = min(size.width, size.height).toFloat()
                    val left = (size.width - side) / 2f
                    val top = (size.height - side) / 2f
                    if (offset.x !in left..(left + side) || offset.y !in top..(top + side)) return@detectTapGestures
                    val cellSize = side / TicTacToeState.ClassicBoardSize
                    val column = ((offset.x - left) / cellSize).toInt().coerceIn(0, 2)
                    val row = ((offset.y - top) / cellSize).toInt().coerceIn(0, 2)
                    onCellSelected(row * TicTacToeState.ClassicBoardSize + column)
                }
            }
    ) {
        val side = min(size.width, size.height)
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val cellSize = side / TicTacToeState.ClassicBoardSize
        drawRect(color = GameCourt, topLeft = Offset(left, top), size = Size(side, side))
        for (index in 1 until TicTacToeState.ClassicBoardSize) {
            val p = index * cellSize
            drawLine(GameCourtLine, Offset(left + p, top), Offset(left + p, top + side), strokeWidth = side * 0.015f)
            drawLine(GameCourtLine, Offset(left, top + p), Offset(left + side, top + p), strokeWidth = side * 0.015f)
        }
        state.board.forEachIndexed { index, mark ->
            val row = index / TicTacToeState.ClassicBoardSize
            val column = index % TicTacToeState.ClassicBoardSize
            val cellLeft = left + column * cellSize
            val cellTop = top + row * cellSize
            val padding = cellSize * 0.22f
            when (mark) {
                Mark.X -> {
                    drawLine(
                        color = GamePlayer,
                        start = Offset(cellLeft + padding, cellTop + padding),
                        end = Offset(cellLeft + cellSize - padding, cellTop + cellSize - padding),
                        strokeWidth = side * 0.02f
                    )
                    drawLine(
                        color = GamePlayer,
                        start = Offset(cellLeft + cellSize - padding, cellTop + padding),
                        end = Offset(cellLeft + padding, cellTop + cellSize - padding),
                        strokeWidth = side * 0.02f
                    )
                }
                Mark.O -> drawCircle(
                    color = GameEnemy,
                    radius = cellSize * 0.28f,
                    center = Offset(cellLeft + cellSize / 2f, cellTop + cellSize / 2f),
                    style = Stroke(width = side * 0.02f)
                )
                Mark.EMPTY -> Unit
            }
        }
        val winningCells = (state.result as? TicTacToeResult.Win)?.winningCells.orEmpty()
        if (winningCells.isNotEmpty()) {
            val start = winningCells.first().cellCenter(left, top, cellSize)
            val end = winningCells.last().cellCenter(left, top, cellSize)
            drawLine(color = GameAccent, start = start, end = end, strokeWidth = side * 0.025f)
        }
    }
}

private fun Int.cellCenter(left: Float, top: Float, cellSize: Float): Offset {
    val row = this / TicTacToeState.ClassicBoardSize
    val column = this % TicTacToeState.ClassicBoardSize
    return Offset(left + column * cellSize + cellSize / 2f, top + row * cellSize + cellSize / 2f)
}
