package com.xanticious.androidgames.view.games.hex

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.hex.HexController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.hex.HexCell
import com.xanticious.androidgames.model.games.hex.HexEvent
import com.xanticious.androidgames.model.games.hex.HexMove
import com.xanticious.androidgames.model.games.hex.HexResult
import com.xanticious.androidgames.model.games.hex.HexState
import com.xanticious.androidgames.state.games.hex.HexPhase
import com.xanticious.androidgames.state.games.hex.HexStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun HexGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { HexController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { HexStateMachine() }
    val phase by machine.phase.collectAsState()

    var state by remember(config) { mutableStateOf(HexState.initial(config)) }

    fun resetMatch() {
        state = HexState.initial(config)
        machine.rematch()
    }

    LaunchedEffect(phase) {
        if (phase == HexPhase.AI_TURN && state.result == HexResult.IN_PROGRESS) {
            delay(250)
            val move = controller.aiMove(state, config)
            if (move != null) {
                val step = controller.placeStone(state, move)
                state = step.state
                if (step.event == HexEvent.AI_WON) machine.gameEnded() else machine.aiMoved()
            }
        }
    }

    GameScaffold(
        title = "Hex",
        onExit = onExit,
        hud = {
            GameHud(
                left = "You W–E",
                center = "${config.boardSize}×${config.boardSize}",
                right = "AI N–S"
            )
        },
        status = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                when (state.result) {
                    HexResult.PLAYER_WON -> VictoryPanel(
                        score = state.board.count { it != HexCell.EMPTY },
                        bestScore = state.board.count { it != HexCell.EMPTY },
                        stars = 3,
                        onReplay = { resetMatch() },
                        onMenu = onExit,
                        headline = "You connected west to east!"
                    )
                    HexResult.AI_WON -> DefeatPanel(
                        score = state.board.count { it != HexCell.EMPTY },
                        bestScore = state.board.count { it != HexCell.EMPTY },
                        onTryAgain = { resetMatch() },
                        onMenu = onExit,
                        headline = "AI connected north to south"
                    )
                    HexResult.IN_PROGRESS -> Text(
                        text = if (phase == HexPhase.PLAYER_TURN) "Your turn. Connect west to east." else "AI is choosing a north-south move.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state, phase) {
                    detectTapGestures { offset ->
                        if (phase == HexPhase.PLAYER_TURN && state.result == HexResult.IN_PROGRESS) {
                            val move = nearestMove(state.boardSize, size, offset)
                            if (move != null) {
                                val step = controller.placeStone(state, move)
                                if (step.event != HexEvent.INVALID_MOVE) {
                                    state = step.state
                                    if (step.event == HexEvent.PLAYER_WON) machine.gameEnded() else machine.playerMoved()
                                }
                            }
                        }
                    }
                }
        ) {
            drawRect(color = GameCourt)
            val layout = boardLayout(state.boardSize, IntSize(size.width.toInt(), size.height.toInt()))
            for (row in 0 until state.boardSize) {
                for (col in 0 until state.boardSize) {
                    val center = cellCenter(row, col, layout)
                    val cell = state.cell(row, col)
                    drawPath(
                        path = hexPath(center, layout.radius),
                        color = colorFor(cell)
                    )
                    drawPath(
                        path = hexPath(center, layout.radius),
                        color = edgeColorFor(state, row, col),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = layout.radius * 0.08f)
                    )
                    if (state.lastMove == HexMove(row, col)) {
                        drawCircle(
                            color = GameAccent,
                            radius = layout.radius * 0.16f,
                            center = center
                        )
                    }
                }
            }
        }
    }
}

private data class HexBoardLayout(val origin: Offset, val radius: Float, val cellWidth: Float, val rowHeight: Float)

private fun boardLayout(boardSize: Int, size: IntSize): HexBoardLayout {
    val rootThree = sqrt(3f)
    val radiusByWidth = size.width / (rootThree * (boardSize + (boardSize - 1) * 0.5f) + 1f)
    val radiusByHeight = size.height / (1.5f * (boardSize - 1) + 2f)
    val radius = min(radiusByWidth, radiusByHeight) * 0.92f
    val cellWidth = rootThree * radius
    val rowHeight = 1.5f * radius
    val boardWidth = cellWidth * (boardSize + (boardSize - 1) * 0.5f)
    val boardHeight = radius * 2f + rowHeight * (boardSize - 1)
    return HexBoardLayout(
        origin = Offset((size.width - boardWidth) / 2f + cellWidth / 2f, (size.height - boardHeight) / 2f + radius),
        radius = radius,
        cellWidth = cellWidth,
        rowHeight = rowHeight
    )
}

private fun cellCenter(row: Int, col: Int, layout: HexBoardLayout): Offset = Offset(
    x = layout.origin.x + col * layout.cellWidth + row * layout.cellWidth * 0.5f,
    y = layout.origin.y + row * layout.rowHeight
)

private fun nearestMove(boardSize: Int, size: IntSize, offset: Offset): HexMove? {
    val layout = boardLayout(boardSize, size)
    var bestMove: HexMove? = null
    var bestDistance = Float.MAX_VALUE
    for (row in 0 until boardSize) {
        for (col in 0 until boardSize) {
            val center = cellCenter(row, col, layout)
            val dx = center.x - offset.x
            val dy = center.y - offset.y
            val distance = dx * dx + dy * dy
            if (distance < bestDistance) {
                bestDistance = distance
                bestMove = HexMove(row, col)
            }
        }
    }
    return bestMove.takeIf { bestDistance <= layout.radius * layout.radius }
}

private fun hexPath(center: Offset, radius: Float): Path {
    val path = Path()
    for (i in 0 until 6) {
        val angle = Math.toRadians((30 + i * 60).toDouble())
        val point = Offset(
            x = center.x + radius * cos(angle).toFloat(),
            y = center.y + radius * sin(angle).toFloat()
        )
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    return path
}

private fun colorFor(cell: HexCell): Color = when (cell) {
    HexCell.EMPTY -> GameNeutral.copy(alpha = 0.32f)
    HexCell.PLAYER -> GamePlayer
    HexCell.AI -> GameEnemy
}

private fun edgeColorFor(state: HexState, row: Int, col: Int): Color = when {
    state.result == HexResult.PLAYER_WON && state.cell(row, col) == HexCell.PLAYER -> GameAccent
    state.result == HexResult.AI_WON && state.cell(row, col) == HexCell.AI -> GameAccent
    row == 0 || row == state.boardSize - 1 -> GameEnemy.copy(alpha = 0.75f)
    col == 0 || col == state.boardSize - 1 -> GamePlayer.copy(alpha = 0.75f)
    else -> GameCourtLine.copy(alpha = 0.45f)
}
