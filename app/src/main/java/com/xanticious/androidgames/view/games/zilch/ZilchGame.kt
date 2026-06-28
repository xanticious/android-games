package com.xanticious.androidgames.view.games.zilch

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.zilch.ZilchController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.zilch.ZilchPlayer
import com.xanticious.androidgames.model.games.zilch.ZilchResult
import com.xanticious.androidgames.model.games.zilch.ZilchState
import com.xanticious.androidgames.state.games.zilch.ZilchPhase
import com.xanticious.androidgames.state.games.zilch.ZilchStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.random.Random

@Composable
fun ZilchGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { ZilchController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { ZilchStateMachine() }
    val phase by machine.phase.collectAsState()
    val random = remember { Random(System.currentTimeMillis()) }

    var state by remember { mutableStateOf(ZilchState.initial(config)) }

    fun reset() {
        state = ZilchState.initial(config)
        machine.rematch()
    }

    fun rollForPlayer() {
        val prepared = if (phase == ZilchPhase.PLAYER_DECIDING && state.selected.any { it }) controller.setAside(state) else state
        if (prepared.turnPoints == state.turnPoints && state.selected.any { it }) {
            state = prepared
            return
        }
        state = controller.rollDice(prepared, random)
        if (state.lastRollZilch) machine.playerZilched() else machine.diceRolled()
    }

    fun bankForPlayer() {
        val prepared = if (state.selected.any { it }) controller.setAside(state) else state
        if (prepared.turnPoints <= 0 || (state.selected.any { it } && prepared.turnPoints == state.turnPoints)) {
            state = prepared
            return
        }
        state = controller.bank(prepared)
        if (state.result != null) machine.matchEnded() else machine.playerBanked()
    }

    LaunchedEffect(Unit) {
        machine.startMatch()
    }

    LaunchedEffect(phase) {
        if (phase == ZilchPhase.AI_TURN && state.result == null) {
            delay(450)
            state = controller.aiTurn(state, random)
            if (state.result != null) machine.matchEnded() else machine.aiTurnResolved()
        }
    }

    val playerWon = state.result == ZilchResult.PLAYER_WIN
    GameScaffold(
        title = "Zilch",
        onExit = onExit,
        hud = {
            Column {
                GameHud(
                    left = "AI ${state.aiBanked}",
                    center = "Turn ${state.turnPoints}",
                    right = "You ${state.playerBanked}"
                )
                GameHud(
                    left = "Target ${state.config.winningTarget}",
                    center = if (state.currentPlayer == ZilchPlayer.PLAYER) "Your turn" else "AI turn",
                    right = "Roll ${state.remainingDice} dice"
                )
            }
        },
        status = {
            if (phase == ZilchPhase.GAME_OVER) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    if (playerWon) {
                        VictoryPanel(
                            score = state.playerBanked,
                            bestScore = state.playerBanked,
                            stars = 3,
                            onReplay = ::reset,
                            onMenu = onExit,
                            headline = "Zilch Won!"
                        )
                    } else {
                        DefeatPanel(
                            score = state.aiBanked,
                            bestScore = state.aiBanked,
                            onTryAgain = ::reset,
                            onMenu = onExit,
                            headline = "Zilch Lost"
                        )
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = state.message,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap newly rolled dice to select a scoring single or combo. Locked dice stay set aside above the table.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            ZilchDiceTray(
                state = state,
                enabled = phase == ZilchPhase.PLAYER_DECIDING && state.currentPlayer == ZilchPlayer.PLAYER,
                onToggle = { index -> state = controller.toggleSelection(state, index) },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = ::bankForPlayer,
                    enabled = phase == ZilchPhase.PLAYER_DECIDING && state.currentPlayer == ZilchPlayer.PLAYER && (state.turnPoints > 0 || state.selected.any { it })
                ) { Text("Bank ${state.turnPoints}") }
                OutlinedButton(
                    onClick = ::rollForPlayer,
                    enabled = (phase == ZilchPhase.PLAYER_ROLLING || phase == ZilchPhase.PLAYER_DECIDING) && state.currentPlayer == ZilchPlayer.PLAYER
                ) { Text(if (phase == ZilchPhase.PLAYER_ROLLING) "Roll dice" else "Roll ${state.remainingDice} dice") }
            }
        }
    }
}

@Composable
private fun ZilchDiceTray(
    state: ZilchState,
    enabled: Boolean,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.pointerInput(enabled, state.locked, state.selected) {
            detectTapGestures { offset ->
                if (!enabled) return@detectTapGestures
                val tapped = diceIndexAt(offset, size.width.toFloat(), size.height.toFloat())
                if (tapped >= 0) onToggle(tapped)
            }
        }
    ) {
        drawRect(color = GameCourt, size = size)
        drawLine(
            color = GameCourtLine.copy(alpha = 0.45f),
            start = Offset(0f, size.height * 0.36f),
            end = Offset(size.width, size.height * 0.36f),
            strokeWidth = 3f
        )
        state.dice.forEachIndexed { index, value ->
            val center = diceCenter(index, size.width, size.height)
            val dieSize = min(size.width / 4.3f, size.height / 3.2f)
            val topLeft = Offset(center.x - dieSize / 2f, center.y - dieSize / 2f)
            val faceColor = when {
                state.locked[index] -> GameSuccess
                state.selected[index] -> GameAccent
                else -> Aqua0
            }
            drawRoundRect(
                color = if (state.selected[index]) GamePlayer else faceColor,
                topLeft = topLeft - Offset(dieSize * 0.08f, dieSize * 0.08f),
                size = Size(dieSize * 1.16f, dieSize * 1.16f),
                cornerRadius = CornerRadius(dieSize * 0.22f)
            )
            drawRoundRect(
                color = faceColor,
                topLeft = topLeft,
                size = Size(dieSize, dieSize),
                cornerRadius = CornerRadius(dieSize * 0.18f)
            )
            drawPips(value, topLeft, dieSize, if (state.locked[index]) Aqua0 else Dark1)
        }
    }
}

private fun diceIndexAt(offset: Offset, width: Float, height: Float): Int {
    var bestIndex = -1
    var bestDistance = Float.MAX_VALUE
    for (index in 0 until ZilchState.DICE_COUNT) {
        val center = diceCenter(index, width, height)
        val distance = (center - offset).getDistance()
        if (distance < bestDistance) {
            bestIndex = index
            bestDistance = distance
        }
    }
    val hitRadius = min(width / 4.3f, height / 3.2f) * 0.65f
    return if (bestDistance <= hitRadius) bestIndex else -1
}

private fun diceCenter(index: Int, width: Float, height: Float): Offset {
    val row = index / 3
    val column = index % 3
    return Offset(width * (column + 1) / 4f, height * (if (row == 0) 0.24f else 0.68f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPips(value: Int, topLeft: Offset, dieSize: Float, color: Color) {
    val low = dieSize * 0.28f
    val mid = dieSize * 0.5f
    val high = dieSize * 0.72f
    val positions = when (value) {
        1 -> listOf(Offset(mid, mid))
        2 -> listOf(Offset(low, low), Offset(high, high))
        3 -> listOf(Offset(low, low), Offset(mid, mid), Offset(high, high))
        4 -> listOf(Offset(low, low), Offset(high, low), Offset(low, high), Offset(high, high))
        5 -> listOf(Offset(low, low), Offset(high, low), Offset(mid, mid), Offset(low, high), Offset(high, high))
        else -> listOf(Offset(low, low), Offset(high, low), Offset(low, mid), Offset(high, mid), Offset(low, high), Offset(high, high))
    }
    positions.forEach { pip -> drawCircle(color = color, radius = dieSize * 0.055f, center = topLeft + pip) }
}
