package com.xanticious.androidgames.view.games.dominoes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.dominoes.DominoesController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.dominoes.DominoTile
import com.xanticious.androidgames.model.games.dominoes.DominoesEnd
import com.xanticious.androidgames.model.games.dominoes.DominoesMove
import com.xanticious.androidgames.model.games.dominoes.DominoesPlayer
import com.xanticious.androidgames.model.games.dominoes.DominoesResult
import com.xanticious.androidgames.model.games.dominoes.DominoesState
import com.xanticious.androidgames.model.games.dominoes.PlayedDomino
import com.xanticious.androidgames.state.games.dominoes.DominoesPhase
import com.xanticious.androidgames.state.games.dominoes.DominoesStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Dominoes — draw-dominoes/Fives against local AI. */
@Composable
fun DominoesGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { DominoesController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { DominoesStateMachine() }
    val phase by machine.phase.collectAsState()
    var dealSeed by remember { mutableStateOf(41) }
    var state by remember { mutableStateOf(controller.deal(Random(dealSeed), config)) }
    var selectedTile by remember { mutableStateOf<DominoTile?>(null) }

    fun resetMatch() {
        dealSeed += 1
        state = controller.deal(Random(dealSeed), config)
        selectedTile = null
        machine.reset()
        if (state.currentPlayer == DominoesPlayer.AI) machine.playerMoved()
    }

    fun advanceAfter(newState: DominoesState) {
        state = newState
        selectedTile = null
        when {
            newState.isGameOver -> machine.gameEnded()
            newState.currentPlayer == DominoesPlayer.AI && phase == DominoesPhase.PLAYER_TURN -> machine.playerMoved()
            newState.currentPlayer == DominoesPlayer.PLAYER && phase == DominoesPhase.AI_TURN -> machine.aiMoved()
        }
    }

    LaunchedEffect(Unit) {
        if (state.currentPlayer == DominoesPlayer.AI) machine.playerMoved()
    }

    LaunchedEffect(phase, state.moveCount, state.currentPlayer, state.roundOver) {
        if (phase == DominoesPhase.AI_TURN && state.currentPlayer == DominoesPlayer.AI && !state.roundOver && !state.isGameOver) {
            delay(350L)
            var aiState = state
            if (controller.legalMoves(aiState).isEmpty()) {
                aiState = controller.drawUntilPlayable(aiState)
                state = aiState
            }
            if (aiState.currentPlayer != DominoesPlayer.AI || aiState.roundOver || aiState.isGameOver) {
                advanceAfter(aiState)
            } else {
                val decision = controller.aiDecision(aiState, difficulty, Random(700 + aiState.moveCount + difficulty.ordinal * 37))
                val next = decision.move?.let { controller.applyMove(aiState, it) } ?: controller.pass(aiState)
                advanceAfter(next)
            }
        }
    }

    GameScaffold(
        title = "Dominoes",
        onExit = onExit,
        hud = {
            GameHud(
                left = "You ${state.playerScore} (${state.playerHand.size})",
                center = when {
                    phase == DominoesPhase.GAME_OVER -> "Game over"
                    state.roundOver -> "Round over"
                    phase == DominoesPhase.PLAYER_TURN -> "Your turn"
                    else -> "AI thinking"
                },
                right = "AI ${state.aiScore} (${state.aiHand.size})"
            )
        },
        status = {
            DominoesStatus(
                state = state,
                phase = phase,
                selectedTile = selectedTile,
                controller = controller,
                onDraw = { advanceAfter(controller.drawUntilPlayable(state)) },
                onPass = { advanceAfter(controller.pass(state)) },
                onEnd = { end -> selectedTile?.let { advanceAfter(controller.applyMove(state, DominoesMove(it, end))) } },
                onNextRound = {
                    dealSeed += 1
                    state = controller.continueRound(state, Random(dealSeed))
                    selectedTile = null
                    machine.reset()
                    if (state.currentPlayer == DominoesPlayer.AI) machine.playerMoved()
                },
                onReplay = ::resetMatch,
                onExit = onExit
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Dark1)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Boneyard ${state.boneyard.size}   Open ${state.leftOpen ?: "-"} / ${state.rightOpen ?: "-"}",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        state.line.forEach { DominoTileView(played = it, selected = false, playable = false, onClick = {}) }
                    }
                }
            }
            Text(text = "Your hand", fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val legalTiles = controller.legalMoves(state, DominoesPlayer.PLAYER).map { it.tile }.toSet()
                state.playerHand.forEach { tile ->
                    DominoTileView(
                        tile = tile,
                        selected = tile == selectedTile,
                        playable = phase == DominoesPhase.PLAYER_TURN && tile in legalTiles,
                        onClick = {
                            if (phase == DominoesPhase.PLAYER_TURN && !state.roundOver && !state.isGameOver) {
                                val moves = controller.legalMoves(state, DominoesPlayer.PLAYER).filter { it.tile == tile }
                                when (moves.size) {
                                    0 -> selectedTile = tile
                                    1 -> advanceAfter(controller.applyMove(state, moves.first()))
                                    else -> selectedTile = tile
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DominoesStatus(
    state: DominoesState,
    phase: DominoesPhase,
    selectedTile: DominoTile?,
    controller: DominoesController,
    onDraw: () -> Unit,
    onPass: () -> Unit,
    onEnd: (DominoesEnd) -> Unit,
    onNextRound: () -> Unit,
    onReplay: () -> Unit,
    onExit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (state.result) {
            DominoesResult.PLAYER_WIN -> VictoryPanel(score = state.playerScore, bestScore = state.playerScore, stars = 3, onReplay = onReplay, onMenu = onExit, headline = "Dominoes Won")
            DominoesResult.AI_WIN -> DefeatPanel(score = state.aiScore, bestScore = state.aiScore, onTryAgain = onReplay, onMenu = onExit, headline = "Dominoes Lost")
            DominoesResult.NONE -> {
                Text(text = state.lastMessage)
                if (state.roundOver) {
                    Button(onClick = onNextRound) { Text("Next round") }
                } else if (phase == DominoesPhase.PLAYER_TURN) {
                    val moves = selectedTile?.let { tile -> controller.legalMoves(state, DominoesPlayer.PLAYER).filter { it.tile == tile } }.orEmpty()
                    if (moves.size > 1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            moves.forEach { move -> OutlinedButton(onClick = { onEnd(move.end) }) { Text("Play ${move.end.name.lowercase()}") } }
                        }
                    } else {
                        val legal = controller.legalMoves(state, DominoesPlayer.PLAYER)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (legal.isEmpty() && state.boneyard.isNotEmpty()) Button(onClick = onDraw) { Text("Draw") }
                            if (legal.isEmpty() && state.boneyard.isEmpty()) OutlinedButton(onClick = onPass) { Text("Pass") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DominoTileView(
    tile: DominoTile,
    selected: Boolean,
    playable: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(width = 72.dp, height = 40.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Aqua0,
        border = BorderStroke(width = if (selected || playable) 2.dp else 1.dp, color = if (selected) Aqua3 else if (playable) Aqua2 else Dark1)
    ) {
        DominoCanvas(left = tile.low, right = tile.high)
    }
}

@Composable
private fun DominoTileView(
    played: PlayedDomino,
    selected: Boolean,
    playable: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(width = 72.dp, height = 40.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Aqua0,
        border = BorderStroke(width = if (selected || playable) 2.dp else 1.dp, color = if (selected) Aqua3 else if (playable) Aqua2 else Dark1)
    ) {
        DominoCanvas(left = played.leftPips, right = played.rightPips)
    }
}

@Composable
private fun DominoCanvas(left: Int, right: Int) {
    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        drawRoundRect(color = Aqua0, cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()))
        drawRoundRect(color = GameCourt, style = Stroke(width = 1.dp.toPx()), cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()))
        drawLine(color = GameCourt, start = Offset(size.width / 2f, 4f), end = Offset(size.width / 2f, size.height - 4f), strokeWidth = 1.dp.toPx())
        drawPips(left, Offset.Zero, Size(size.width / 2f, size.height))
        drawPips(right, Offset(size.width / 2f, 0f), Size(size.width / 2f, size.height))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPips(value: Int, topLeft: Offset, area: Size) {
    val positions = pipPositions(value, topLeft, area)
    positions.forEach { drawCircle(color = Dark1, radius = area.minDimension * 0.07f, center = it) }
}

private fun pipPositions(value: Int, topLeft: Offset, area: Size): List<Offset> {
    val x1 = topLeft.x + area.width * 0.28f
    val x2 = topLeft.x + area.width * 0.5f
    val x3 = topLeft.x + area.width * 0.72f
    val y1 = topLeft.y + area.height * 0.28f
    val y2 = topLeft.y + area.height * 0.5f
    val y3 = topLeft.y + area.height * 0.72f
    return when (value) {
        0 -> emptyList()
        1 -> listOf(Offset(x2, y2))
        2 -> listOf(Offset(x1, y1), Offset(x3, y3))
        3 -> listOf(Offset(x1, y1), Offset(x2, y2), Offset(x3, y3))
        4 -> listOf(Offset(x1, y1), Offset(x3, y1), Offset(x1, y3), Offset(x3, y3))
        5 -> listOf(Offset(x1, y1), Offset(x3, y1), Offset(x2, y2), Offset(x1, y3), Offset(x3, y3))
        else -> listOf(Offset(x1, y1), Offset(x3, y1), Offset(x1, y2), Offset(x3, y2), Offset(x1, y3), Offset(x3, y3))
    }
}
