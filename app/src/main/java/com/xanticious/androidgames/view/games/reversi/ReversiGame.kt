package com.xanticious.androidgames.view.games.reversi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.reversi.ReversiController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.reversi.Disc
import com.xanticious.androidgames.model.games.reversi.REVERSI_BOARD_SIZE
import com.xanticious.androidgames.state.games.reversi.ReversiPhase
import com.xanticious.androidgames.state.games.reversi.ReversiStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlinx.coroutines.delay

@Composable
fun ReversiGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { ReversiController() }
    val machine = remember(difficulty) { ReversiStateMachine(difficulty = difficulty, controller = controller) }
    val state by machine.state.collectAsState()
    val phase by machine.phase.collectAsState()
    val legalMoves = remember(state) { controller.legalMoves(state).associateBy { it.position } }

    LaunchedEffect(phase, state) {
        if (phase == ReversiPhase.AI_TURN) {
            delay(350L)
            machine.performAiTurn()
        }
    }

    val turnLabel = when (phase) {
        ReversiPhase.PLAYER_TURN -> "Turn: You"
        ReversiPhase.AI_TURN -> "Turn: AI"
        ReversiPhase.GAME_OVER -> "Game over"
    }

    GameScaffold(
        title = "Reversi",
        onExit = onExit,
        hud = {
            GameHud(
                left = "You ● ${if (state.config.playerDisc == Disc.BLACK) state.blackCount else state.whiteCount}",
                center = turnLabel,
                right = "AI ○ ${if (state.config.aiDisc == Disc.BLACK) state.blackCount else state.whiteCount}"
            )
        },
        status = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.lastPass?.let { Text("${it.label()} had no legal move and passed.") }
                if (phase != ReversiPhase.GAME_OVER) {
                    Text(
                        text = if (phase == ReversiPhase.PLAYER_TURN) "Tap a highlighted legal square." else "AI is choosing a move…",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (phase == ReversiPhase.GAME_OVER) {
                    val result = state.result
                    val playerWon = result?.winner == state.config.playerDisc
                    val aiWon = result?.winner == state.config.aiDisc
                    when {
                        playerWon -> VictoryPanel(
                            score = state.blackCount + state.whiteCount,
                            bestScore = state.blackCount + state.whiteCount,
                            stars = 3,
                            onReplay = { machine.reset() },
                            onMenu = onExit,
                            headline = "You Win!"
                        )
                        aiWon -> DefeatPanel(
                            score = state.blackCount + state.whiteCount,
                            bestScore = state.blackCount + state.whiteCount,
                            onTryAgain = { machine.reset() },
                            onMenu = onExit,
                            headline = "You Lose"
                        )
                        else -> DrawPanel(onReplay = { machine.reset() }, onMenu = onExit)
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(16.dp)
                    .pointerInput(phase, state) {
                        detectTapGestures { offset ->
                            if (phase == ReversiPhase.PLAYER_TURN) {
                                val cell = size.width / REVERSI_BOARD_SIZE.toFloat()
                                machine.playerMoveSelected((offset.y / cell).toInt(), (offset.x / cell).toInt())
                            }
                        }
                    }
            ) {
                val cell = size.minDimension / REVERSI_BOARD_SIZE
                val boardSize = cell * REVERSI_BOARD_SIZE
                drawRect(color = Dark1, size = Size(boardSize, boardSize))
                for (row in 0 until REVERSI_BOARD_SIZE) {
                    for (col in 0 until REVERSI_BOARD_SIZE) {
                        val topLeft = Offset(col * cell, row * cell)
                        drawRect(color = Dark2, topLeft = topLeft, size = Size(cell, cell))
                        drawRect(color = Aqua3.copy(alpha = 0.55f), topLeft = topLeft, size = Size(cell, cell), style = Stroke(width = 1.5f))
                        val center = Offset(topLeft.x + cell / 2f, topLeft.y + cell / 2f)
                        when (state.board[row][col]) {
                            Disc.BLACK -> drawCircle(color = Dark0, radius = cell * 0.36f, center = center)
                            Disc.WHITE -> drawCircle(color = Aqua0, radius = cell * 0.36f, center = center)
                            Disc.EMPTY -> if (state.config.showHints && legalMoves.keys.any { it.row == row && it.col == col }) {
                                drawCircle(color = Aqua2.copy(alpha = 0.75f), radius = cell * 0.11f, center = center)
                            }
                        }
                        if (state.lastMove?.row == row && state.lastMove?.col == col) {
                            drawCircle(color = Aqua3, radius = cell * 0.42f, center = center, style = Stroke(width = 4f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawPanel(onReplay: () -> Unit, onMenu: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Draw", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onReplay) { Text("Replay") }
            OutlinedButton(onClick = onMenu) { Text("Menu") }
        }
    }
}

private fun Disc.label(): String = when (this) {
    Disc.BLACK -> "Black"
    Disc.WHITE -> "White"
    Disc.EMPTY -> "No one"
}
