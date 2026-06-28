package com.xanticious.androidgames.view.games.chinesecheckers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.chinesecheckers.ChineseCheckersCoordinate
import com.xanticious.androidgames.model.games.chinesecheckers.ChineseCheckersHole
import com.xanticious.androidgames.model.games.chinesecheckers.ChineseCheckersSide
import com.xanticious.androidgames.model.games.chinesecheckers.ChineseCheckersState
import com.xanticious.androidgames.state.games.chinesecheckers.ChineseCheckersPhase
import com.xanticious.androidgames.state.games.chinesecheckers.ChineseCheckersStateMachine
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlin.math.hypot
import kotlin.math.min

@Composable
fun ChineseCheckersGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val machine = remember(difficulty) { ChineseCheckersStateMachine(difficulty) }
    val phase by machine.phase.collectAsState()
    val state by machine.state.collectAsState()
    val legalTargets = remember(state.selectedPeg, state.occupancy, state.currentPlayer, state.result) {
        machine.legalTargets()
    }

    GameScaffold(
        title = "Chinese Checkers",
        onExit = onExit,
        hud = {
            GameHud(
                left = "You: South",
                center = when (phase) {
                    ChineseCheckersPhase.PLAYER_TURN -> "Your turn"
                    ChineseCheckersPhase.AI_TURN -> "AI thinking"
                    ChineseCheckersPhase.GAME_OVER -> "Game over"
                },
                right = "AI: North ${difficulty.label}"
            )
        },
        status = {
            ChineseCheckersStatus(
                state = state,
                phase = phase,
                onReplay = machine::reset,
                onExit = onExit
            )
        }
    ) {
        ChineseCheckersBoard(
            state = state,
            legalTargets = legalTargets,
            enabled = phase == ChineseCheckersPhase.PLAYER_TURN,
            onHoleTapped = { coordinate ->
                if (state.selectedPeg == null || state.occupancy[coordinate] == state.currentPlayer) {
                    machine.selectPeg(coordinate)
                } else {
                    machine.chooseDestination(coordinate)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ChineseCheckersStatus(
    state: ChineseCheckersState,
    phase: ChineseCheckersPhase,
    onReplay: () -> Unit,
    onExit: () -> Unit
) {
    val result = state.result
    if (result == null) {
        Text(
            text = when (phase) {
                ChineseCheckersPhase.PLAYER_TURN -> state.message
                ChineseCheckersPhase.AI_TURN -> "AI is choosing a race move."
                ChineseCheckersPhase.GAME_OVER -> state.message
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            fontWeight = FontWeight.Bold
        )
        return
    }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (result.winner == ChineseCheckersSide.PLAYER) {
            VictoryPanel(
                score = result.turnsPlayed,
                bestScore = result.turnsPlayed,
                stars = 3,
                onReplay = onReplay,
                onMenu = onExit,
                headline = "You Win!"
            )
        } else {
            DefeatPanel(
                score = result.turnsPlayed,
                bestScore = result.turnsPlayed,
                onTryAgain = onReplay,
                onMenu = onExit,
                headline = "AI Wins"
            )
        }
    }
}

@Composable
private fun ChineseCheckersBoard(
    state: ChineseCheckersState,
    legalTargets: Set<ChineseCheckersCoordinate>,
    enabled: Boolean,
    onHoleTapped: (ChineseCheckersCoordinate) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.pointerInput(enabled, state.holes, state.occupancy, state.selectedPeg, legalTargets) {
            detectTapGestures { offset ->
                if (!enabled) return@detectTapGestures
                val side = min(size.width, size.height).toFloat()
                val left = (size.width - side) / 2f
                val top = (size.height - side) / 2f
                val nearest = state.holes.minByOrNull { hole ->
                    val position = hole.toOffset(left, top, side)
                    hypot((offset.x - position.x).toDouble(), (offset.y - position.y).toDouble())
                }
                val radius = side * 0.035f
                if (nearest != null) {
                    val position = nearest.toOffset(left, top, side)
                    val distance = hypot((offset.x - position.x).toDouble(), (offset.y - position.y).toDouble()).toFloat()
                    if (distance <= radius * 1.8f) onHoleTapped(nearest.coordinate)
                }
            }
        }
    ) {
        val side = min(size.width, size.height)
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val radius = side * 0.025f
        drawRect(color = GameCourt)
        val positions = state.holes.associate { it.coordinate to it.toOffset(left, top, side) }
        state.connections.forEach { (startCoordinate, endCoordinate) ->
            drawLine(
                color = GameCourtLine.copy(alpha = 0.28f),
                start = positions.getValue(startCoordinate),
                end = positions.getValue(endCoordinate),
                strokeWidth = side * 0.003f
            )
        }
        state.holes.forEach { hole ->
            val center = positions.getValue(hole.coordinate)
            val occupant = state.occupancy[hole.coordinate]
            val outline = when {
                hole.coordinate == state.selectedPeg -> Aqua2
                hole.coordinate in legalTargets -> Aqua1
                hole.targetFor == ChineseCheckersSide.PLAYER -> GamePlayer.copy(alpha = 0.55f)
                hole.targetFor == ChineseCheckersSide.AI -> GameEnemy.copy(alpha = 0.55f)
                else -> GameCourtLine.copy(alpha = 0.75f)
            }
            drawCircle(color = GameNeutral.copy(alpha = 0.28f), radius = radius, center = center)
            drawCircle(
                color = outline,
                radius = if (hole.coordinate in legalTargets || hole.coordinate == state.selectedPeg) radius * 1.45f else radius * 1.12f,
                center = center,
                style = Stroke(width = side * 0.006f)
            )
            if (occupant != null) {
                drawCircle(color = occupant.colorToken(), radius = radius * 0.9f, center = center)
            }
        }
    }
}

private fun ChineseCheckersHole.toOffset(left: Float, top: Float, side: Float): Offset = Offset(
    x = left + side * (0.07f + displayX * 0.86f),
    y = top + side * (0.07f + displayY * 0.86f)
)

private fun ChineseCheckersSide.colorToken(): Color = when (this) {
    ChineseCheckersSide.PLAYER -> GamePlayer
    ChineseCheckersSide.AI -> GameEnemy
}
