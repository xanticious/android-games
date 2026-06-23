package com.xanticious.androidgames.view.games.checkers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.checkers.CheckersPiece
import com.xanticious.androidgames.model.games.checkers.CheckersSide
import com.xanticious.androidgames.model.games.checkers.CheckersSquare
import com.xanticious.androidgames.model.games.checkers.CheckersState
import com.xanticious.androidgames.state.games.checkers.CheckersPhase
import com.xanticious.androidgames.state.games.checkers.CheckersStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.min
import kotlin.random.Random

@Composable
fun CheckersGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val machine = remember { CheckersStateMachine() }
    val phase by machine.phase.collectAsState()
    val state by machine.state.collectAsState()

    LaunchedEffect(difficulty) {
        machine.startMatch(difficulty)
    }

    LaunchedEffect(phase, state.currentPlayer, state.continuingCaptureFrom) {
        if (phase == CheckersPhase.AI_TURN) {
            delay(450L)
            machine.playAiTurn(Random(17))
        }
    }

    val playerWon = state.result?.winner == state.config.playerSide
    val legalMoves = remember(state) {
        com.xanticious.androidgames.controller.games.checkers.CheckersController().legalMoves(state)
    }
    val legalDestinations = state.selectedSquare?.let { selected ->
        legalMoves.filter { it.from == selected }.map { it.to }.toSet()
    } ?: emptySet()
    val captureDestinations = state.selectedSquare?.let { selected ->
        legalMoves.filter { it.from == selected && it.isCapture }.map { it.to }.toSet()
    } ?: emptySet()

    GameScaffold(
        title = "Checkers",
        onExit = onExit,
        hud = {
            GameHud(
                left = "Red captures ${state.redCaptures}",
                center = if (phase == CheckersPhase.AI_TURN) "AI thinking" else state.message,
                right = "Black captures ${state.blackCaptures}"
            )
        },
        status = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (phase == CheckersPhase.GAME_OVER) {
                    if (playerWon) {
                        VictoryPanel(
                            score = state.redCaptures,
                            bestScore = state.redCaptures,
                            stars = 3,
                            onReplay = { machine.rematch() },
                            onMenu = onExit,
                            headline = "You Win!"
                        )
                    } else {
                        DefeatPanel(
                            score = state.blackCaptures,
                            bestScore = state.blackCaptures,
                            onTryAgain = { machine.rematch() },
                            onMenu = onExit,
                            headline = "You Lose"
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { machine.clearSelection() }) { Text("Undo selection") }
                    }
                }
            }
        }
    ) {
        CheckersBoard(
            state = state,
            legalDestinations = legalDestinations,
            captureDestinations = captureDestinations,
            onTap = { square -> machine.tapSquare(square) }
        )
    }
}

@Composable
private fun CheckersBoard(
    state: CheckersState,
    legalDestinations: Set<CheckersSquare>,
    captureDestinations: Set<CheckersSquare>,
    onTap: (CheckersSquare) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(state) {
                detectTapGestures { offset ->
                    val boardSize = min(size.width, size.height).toFloat()
                    val left = (size.width - boardSize) / 2f
                    val top = (size.height - boardSize) / 2f
                    if (offset.x < left || offset.y < top || offset.x >= left + boardSize || offset.y >= top + boardSize) return@detectTapGestures
                    val cell = boardSize / CheckersState.BOARD_SIZE
                    val col = floor((offset.x - left) / cell).toInt()
                    val row = floor((offset.y - top) / cell).toInt()
                    onTap(CheckersSquare(row, col))
                }
            }
    ) {
        val boardSize = min(size.width, size.height)
        val cell = boardSize / CheckersState.BOARD_SIZE
        val left = (size.width - boardSize) / 2f
        val top = (size.height - boardSize) / 2f
        drawRect(color = Dark1, topLeft = Offset(left - cell * 0.08f, top - cell * 0.08f), size = Size(boardSize + cell * 0.16f, boardSize + cell * 0.16f))
        for (row in 0 until CheckersState.BOARD_SIZE) {
            for (col in 0 until CheckersState.BOARD_SIZE) {
                val topLeft = Offset(left + col * cell, top + row * cell)
                val square = CheckersSquare(row, col)
                val dark = CheckersState.isDarkSquare(row, col)
                drawRect(color = if (dark) Dark2 else Aqua0, topLeft = topLeft, size = Size(cell, cell))
                if (square == state.selectedSquare) {
                    drawRect(color = Aqua4, topLeft = topLeft, size = Size(cell, cell), style = Stroke(width = cell * 0.08f))
                }
                if (square in legalDestinations) {
                    drawCircle(
                        color = if (square in captureDestinations) Aqua1 else Aqua3,
                        radius = if (square in captureDestinations) cell * 0.18f else cell * 0.12f,
                        center = Offset(topLeft.x + cell / 2f, topLeft.y + cell / 2f)
                    )
                }
                state.board[row][col]?.let { piece ->
                    drawPiece(piece, Offset(topLeft.x + cell / 2f, topLeft.y + cell / 2f), cell)
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPiece(piece: CheckersPiece, center: Offset, cell: Float) {
    val fill = when (piece.side) {
        CheckersSide.RED -> GameEnemy
        CheckersSide.BLACK -> Dark0
    }
    drawCircle(color = fill, radius = cell * 0.36f, center = center)
    drawCircle(color = Aqua3, radius = cell * 0.36f, center = center, style = Stroke(width = cell * 0.035f))
    if (piece.isKing) {
        drawCircle(color = GameAccent, radius = cell * 0.13f, center = center)
        drawCircle(color = Aqua0, radius = cell * 0.07f, center = center)
    }
}
