package com.xanticious.androidgames.view.games.yahtzee

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.yahtzee.YahtzeeController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.yahtzee.YahtzeeCategory
import com.xanticious.androidgames.model.games.yahtzee.YahtzeePlayer
import com.xanticious.androidgames.model.games.yahtzee.YahtzeeResult
import com.xanticious.androidgames.model.games.yahtzee.YahtzeeState
import com.xanticious.androidgames.state.games.yahtzee.YahtzeePhase
import com.xanticious.androidgames.state.games.yahtzee.YahtzeeStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun YahtzeeGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { YahtzeeController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { YahtzeeStateMachine() }
    val phase by machine.phase.collectAsState()
    val random = remember(difficulty) { Random(difficulty.ordinal + 17) }

    var state by remember(config) { mutableStateOf(YahtzeeState.initial(config)) }

    fun reset() {
        state = YahtzeeState.initial(config)
        machine.rematch()
    }

    LaunchedEffect(Unit) {
        machine.startMatch()
    }

    LaunchedEffect(phase, state.currentPlayer, state.result) {
        if (phase == YahtzeePhase.AI_TURN && state.currentPlayer == YahtzeePlayer.AI && state.result == null) {
            delay(350)
            state = controller.playAiTurn(state, random)
            machine.aiFinished(state.result != null)
        }
    }

    val playerTotal = controller.total(state.playerScorecard, state.playerYahtzeeBonus, config)
    val aiTotal = controller.total(state.aiScorecard, state.aiYahtzeeBonus, config)
    val canRoll = phase == YahtzeePhase.PLAYER_ROLLING && state.currentPlayer == YahtzeePlayer.HUMAN && state.rollsLeft > 0
    val canScore = state.currentPlayer == YahtzeePlayer.HUMAN && state.rollsLeft < YahtzeeState.MaxRolls && state.result == null

    GameScaffold(
        title = "Yahtzee",
        onExit = onExit,
        hud = {
            GameHud(
                left = "You $playerTotal",
                center = "Round ${state.round}/${config.totalRounds}",
                right = "AI $aiTotal  ${difficulty.label}"
            )
        },
        status = {
            if (phase == YahtzeePhase.GAME_OVER || state.result != null) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    when (state.result) {
                        YahtzeeResult.HUMAN_WIN, YahtzeeResult.DRAW -> VictoryPanel(
                            score = playerTotal,
                            bestScore = maxOf(playerTotal, aiTotal),
                            stars = if (state.result == YahtzeeResult.DRAW) 1 else 3,
                            onReplay = ::reset,
                            onMenu = onExit,
                            headline = if (state.result == YahtzeeResult.DRAW) "Draw" else "Yahtzee Won!"
                        )
                        YahtzeeResult.AI_WIN -> DefeatPanel(
                            score = playerTotal,
                            bestScore = aiTotal,
                            onTryAgain = ::reset,
                            onMenu = onExit,
                            headline = "Yahtzee Lost"
                        )
                        null -> Unit
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GameCourt)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = promptFor(phase, state),
                color = Aqua0,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                state.dice.forEachIndexed { index, value ->
                    DieFace(
                        value = value,
                        held = state.held[index],
                        enabled = state.currentPlayer == YahtzeePlayer.HUMAN && state.rollsLeft in 1 until YahtzeeState.MaxRolls,
                        onClick = {
                            state = controller.toggleHeld(state, index)
                            machine.diceLocked()
                        }
                    )
                }
            }
            Button(
                onClick = {
                    state = controller.rollDice(state, random)
                    machine.diceRolled(state.rollsLeft)
                },
                enabled = canRoll,
                colors = ButtonDefaults.buttonColors(containerColor = Aqua3)
            ) {
                Text("Roll (${YahtzeeState.MaxRolls - state.rollsLeft + 1}/${YahtzeeState.MaxRolls})")
            }
            Scorecard(
                state = state,
                controller = controller,
                canScore = canScore,
                onScore = { category ->
                    state = controller.applyScore(state, category)
                    machine.categorySelected(state.result != null)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DieFace(value: Int, held: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .size(52.dp)
                .border(width = if (held) 3.dp else 1.dp, color = if (held) Aqua2 else GameCourtLine, shape = RoundedCornerShape(10.dp))
                .clickable(enabled = enabled, onClick = onClick)
        ) {
            drawRoundRect(color = Aqua0, size = size, cornerRadius = CornerRadius(10.dp.toPx()))
            drawPips(value = value, color = Dark2)
        }
        Text(text = if (held) "Lock" else "·", color = if (held) GameAccent else Aqua0, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun Scorecard(
    state: YahtzeeState,
    controller: YahtzeeController,
    canScore: Boolean,
    onScore: (YahtzeeCategory) -> Unit
) {
    Surface(color = Dark0, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
            ScoreHeader()
            YahtzeeCategory.entries.forEach { category ->
                ScoreRow(
                    category = category,
                    playerScore = state.playerScorecard[category],
                    aiScore = state.aiScorecard[category],
                    preview = if (canScore && state.playerScorecard[category] == null) controller.scoreFor(category, state.dice) else null,
                    enabled = canScore && state.playerScorecard[category] == null,
                    onClick = { onScore(category) }
                )
            }
            LedgerRow("Upper Bonus", controller.upperBonus(state.playerScorecard, state.config), controller.upperBonus(state.aiScorecard, state.config), null)
            LedgerRow("Yahtzee Bonus", state.playerYahtzeeBonus, state.aiYahtzeeBonus, null)
            LedgerRow(
                label = "Total",
                player = controller.total(state.playerScorecard, state.playerYahtzeeBonus, state.config),
                ai = controller.total(state.aiScorecard, state.aiYahtzeeBonus, state.config),
                preview = null,
                bold = true
            )
        }
    }
}

@Composable
private fun ScoreHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Category", modifier = Modifier.weight(1.45f), color = Aqua0, fontWeight = FontWeight.Bold)
        Text("You", modifier = Modifier.weight(0.55f), color = GamePlayer, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("AI", modifier = Modifier.weight(0.55f), color = GameEnemy, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("Prev", modifier = Modifier.weight(0.55f), color = GameAccent, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ScoreRow(
    category: YahtzeeCategory,
    playerScore: Int?,
    aiScore: Int?,
    preview: Int?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val rowColor = if (enabled && preview.orZero() >= 25) Aqua4.copy(alpha = 0.35f) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowColor, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(category.label, modifier = Modifier.weight(1.45f), color = Aqua0)
        Text(playerScore?.toString() ?: "-", modifier = Modifier.weight(0.55f), color = Aqua0, textAlign = TextAlign.Center)
        Text(aiScore?.toString() ?: "-", modifier = Modifier.weight(0.55f), color = Aqua0, textAlign = TextAlign.Center)
        Text(preview?.toString() ?: "", modifier = Modifier.weight(0.55f), color = GameAccent, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LedgerRow(label: String, player: Int, ai: Int, preview: Int?, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp, horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        val weight = if (bold) FontWeight.Bold else FontWeight.Normal
        Text(label, modifier = Modifier.weight(1.45f), color = Aqua2, fontWeight = weight)
        Text(player.toString(), modifier = Modifier.weight(0.55f), color = Aqua2, fontWeight = weight, textAlign = TextAlign.Center)
        Text(ai.toString(), modifier = Modifier.weight(0.55f), color = Aqua2, fontWeight = weight, textAlign = TextAlign.Center)
        Text(preview?.toString() ?: "", modifier = Modifier.weight(0.55f), color = Aqua2, textAlign = TextAlign.Center)
    }
}

private fun promptFor(phase: YahtzeePhase, state: YahtzeeState): String = when {
    state.result != null -> "Final score: You vs AI"
    phase == YahtzeePhase.AI_TURN -> "AI is rolling..."
    state.rollsLeft == YahtzeeState.MaxRolls -> "Roll five dice to start your turn."
    state.rollsLeft == 0 -> "Choose a category to score."
    else -> "Hold dice, roll again, or tap a category. Rolls left: ${state.rollsLeft}"
}

private fun DrawScope.drawPips(value: Int, color: Color) {
    val positions = when (value.coerceIn(1, 6)) {
        1 -> listOf(Offset(0.5f, 0.5f))
        2 -> listOf(Offset(0.28f, 0.28f), Offset(0.72f, 0.72f))
        3 -> listOf(Offset(0.28f, 0.28f), Offset(0.5f, 0.5f), Offset(0.72f, 0.72f))
        4 -> listOf(Offset(0.28f, 0.28f), Offset(0.72f, 0.28f), Offset(0.28f, 0.72f), Offset(0.72f, 0.72f))
        5 -> listOf(Offset(0.28f, 0.28f), Offset(0.72f, 0.28f), Offset(0.5f, 0.5f), Offset(0.28f, 0.72f), Offset(0.72f, 0.72f))
        else -> listOf(Offset(0.28f, 0.25f), Offset(0.72f, 0.25f), Offset(0.28f, 0.5f), Offset(0.72f, 0.5f), Offset(0.28f, 0.75f), Offset(0.72f, 0.75f))
    }
    positions.forEach { point ->
        drawCircle(color = color, radius = size.minDimension * 0.07f, center = Offset(point.x * size.width, point.y * size.height))
    }
}

private fun Int?.orZero(): Int = this ?: 0
