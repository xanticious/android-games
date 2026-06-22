package com.xanticious.androidgames.view.games.battleships

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.battleships.BattleshipsBoard
import com.xanticious.androidgames.model.games.battleships.BattleshipsCellMarker
import com.xanticious.androidgames.model.games.battleships.BattleshipsFireOutcome
import com.xanticious.androidgames.model.games.battleships.BattleshipsResult
import com.xanticious.androidgames.model.games.battleships.BattleshipsShotReport
import com.xanticious.androidgames.state.games.battleships.BattleshipsPhase
import com.xanticious.androidgames.state.games.battleships.BattleshipsStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlinx.coroutines.delay

@Composable
fun BattleshipsGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val machine = remember(difficulty) { BattleshipsStateMachine(difficulty = difficulty) }
    val state by machine.state.collectAsState()
    val phase by machine.phase.collectAsState()
    val errorColor = MaterialTheme.colorScheme.error

    LaunchedEffect(phase) {
        if (phase == BattleshipsPhase.AI_TURN) {
            delay(450L)
            machine.performAiTurn()
        }
    }

    val turnLabel = when (phase) {
        BattleshipsPhase.PLAYER_TURN -> "Turn: You"
        BattleshipsPhase.AI_TURN -> "Turn: AI"
        BattleshipsPhase.GAME_OVER -> "Game over"
    }

    GameScaffold(
        title = "Battleships",
        onExit = onExit,
        hud = {
            GameHud(
                left = "Enemy ships ${remainingShips(state.aiBoard)}",
                center = turnLabel,
                right = "Your ships ${remainingShips(state.playerBoard)}"
            )
        },
        status = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (phase != BattleshipsPhase.GAME_OVER) {
                    val hint = when (phase) {
                        BattleshipsPhase.PLAYER_TURN -> "Tap a cell in Enemy waters to fire."
                        else -> "AI is taking aim…"
                    }
                    state.lastShot?.let { report -> report.describe().takeIf { it.isNotEmpty() }?.let { Text(it) } }
                    Text(text = hint, style = MaterialTheme.typography.bodyMedium)
                }
                if (phase == BattleshipsPhase.GAME_OVER) {
                    when (state.result) {
                        BattleshipsResult.PLAYER_WIN -> VictoryPanel(
                            score = state.playerShots,
                            bestScore = state.playerShots,
                            stars = 3,
                            onReplay = { machine.reset() },
                            onMenu = onExit,
                            headline = "Fleet Destroyed!"
                        )
                        else -> DefeatPanel(
                            score = state.playerShots,
                            bestScore = state.playerShots,
                            onTryAgain = { machine.reset() },
                            onMenu = onExit,
                            headline = "Your Fleet Sank"
                        )
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Enemy waters", style = MaterialTheme.typography.titleSmall)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(phase, state) {
                        detectTapGestures { offset ->
                            if (phase == BattleshipsPhase.PLAYER_TURN) {
                                val cellSize = size.minDimension / state.aiBoard.size
                                val col = (offset.x / cellSize).toInt()
                                val row = (offset.y / cellSize).toInt()
                                machine.playerFireSelected(row, col)
                            }
                        }
                    }
            ) {
                drawBoard(board = state.aiBoard, revealShips = false, errorColor = errorColor)
            }
            Text("Your fleet", style = MaterialTheme.typography.titleSmall)
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                drawBoard(board = state.playerBoard, revealShips = true, errorColor = errorColor)
            }
        }
    }
}

private fun DrawScope.drawBoard(board: BattleshipsBoard, revealShips: Boolean, errorColor: Color) {
    val cellSize = size.minDimension / board.size
    val extent = cellSize * board.size
    drawRect(color = Dark1, size = Size(extent, extent))
    for (row in 0 until board.size) {
        for (col in 0 until board.size) {
            val topLeft = Offset(col * cellSize, row * cellSize)
            val gridCell = board.cells[row][col]
            val fill = if (revealShips && gridCell.hasShip) Dark2 else Dark1
            drawRect(color = fill, topLeft = topLeft, size = Size(cellSize, cellSize))
            drawRect(color = Aqua3.copy(alpha = 0.5f), topLeft = topLeft, size = Size(cellSize, cellSize), style = Stroke(width = 1.5f))
            val center = Offset(topLeft.x + cellSize / 2f, topLeft.y + cellSize / 2f)
            when (gridCell.marker) {
                BattleshipsCellMarker.HIT -> {
                    drawCircle(color = errorColor, radius = cellSize * 0.3f, center = center)
                    drawCircle(color = Aqua0, radius = cellSize * 0.3f, center = center, style = Stroke(width = 2f))
                }
                BattleshipsCellMarker.MISS -> drawCircle(color = Aqua1.copy(alpha = 0.8f), radius = cellSize * 0.14f, center = center)
                BattleshipsCellMarker.UNKNOWN -> Unit
            }
        }
    }
}

private fun remainingShips(board: BattleshipsBoard): Int =
    board.fleet.count { ship -> ship.cells.any { board.cellAt(it)?.marker != BattleshipsCellMarker.HIT } }

private fun BattleshipsShotReport.describe(): String = when (outcome) {
    BattleshipsFireOutcome.HIT -> "Hit!"
    BattleshipsFireOutcome.MISS -> "Miss."
    BattleshipsFireOutcome.SUNK -> "Sunk the $shipName!"
    else -> ""
}
