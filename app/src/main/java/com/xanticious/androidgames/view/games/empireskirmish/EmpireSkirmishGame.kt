package com.xanticious.androidgames.view.games.empireskirmish

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.empireskirmish.attackablePositions
import com.xanticious.androidgames.controller.games.empireskirmish.checkWinner
import com.xanticious.androidgames.controller.games.empireskirmish.computeEnemyTurn
import com.xanticious.androidgames.controller.games.empireskirmish.endPlayerTurn
import com.xanticious.androidgames.controller.games.empireskirmish.generateBattle
import com.xanticious.androidgames.controller.games.empireskirmish.initialState
import com.xanticious.androidgames.controller.games.empireskirmish.moveUnit
import com.xanticious.androidgames.controller.games.empireskirmish.reachableTiles
import com.xanticious.androidgames.controller.games.empireskirmish.resolveAttack
import com.xanticious.androidgames.controller.games.empireskirmish.undoMove
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.GridPos
import com.xanticious.androidgames.model.games.empireskirmish.EmpireSkirmishState
import com.xanticious.androidgames.model.games.empireskirmish.Side
import com.xanticious.androidgames.model.games.empireskirmish.SkirmishUnit
import com.xanticious.androidgames.model.games.empireskirmish.UnitType
import com.xanticious.androidgames.state.games.empireskirmish.EmpireSkirmishPhase
import com.xanticious.androidgames.state.games.empireskirmish.EmpireSkirmishStateMachine
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlinx.coroutines.delay

/**
 * Empire Skirmish — turn-based tactics game on a 10×8 grid.
 *
 * Player commands a squad (SWORDSMAN×2, BOWMAN, MAGE, KING) against an AI enemy.
 * Losing your King ends the game immediately.
 * Victory/defeat panels live below the board; they never overlay the grid.
 */
@Composable
fun EmpireSkirmishGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val machine = remember { EmpireSkirmishStateMachine() }
    val phase by machine.phase.collectAsState()
    val textMeasurer = rememberTextMeasurer()

    var gameState by remember {
        val battle = generateBattle(seed = System.currentTimeMillis(), difficulty = difficulty)
        mutableStateOf(initialState(battle))
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    LaunchedEffect(Unit) {
        machine.startBattle()
    }

    // ── Enemy Turn Auto-Processing ────────────────────────────────────────────

    LaunchedEffect(phase) {
        if (phase == EmpireSkirmishPhase.ENEMY_TURN) {
            delay(800L)
            val afterEnemy = computeEnemyTurn(gameState, difficulty)
            gameState = afterEnemy
            val winner = checkWinner(gameState)
            when (winner) {
                Side.PLAYER -> machine.enemyKingDied()
                Side.ENEMY  -> machine.playerKingDied()
                null        -> machine.endEnemyTurn()
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    val playerUnits = gameState.battle.units.filter { it.side == Side.PLAYER }
    val enemyUnits  = gameState.battle.units.filter { it.side == Side.ENEMY }
    val selectedUnit = gameState.selectedUnitId?.let { id ->
        gameState.battle.units.firstOrNull { it.id == id }
    }

    fun onTileClicked(pos: GridPos) {
        if (phase != EmpireSkirmishPhase.PLAYER_TURN) return
        val sel = selectedUnit

        when {
            // Tap own unit → select it (only if it hasn't attacked yet)
            gameState.battle.units.any { it.side == Side.PLAYER && it.pos == pos && !it.hasAttacked } -> {
                val tapped = gameState.battle.units.first { it.side == Side.PLAYER && it.pos == pos }
                if (tapped.hasAttacked) return
                val reachable = if (!tapped.hasMoved) reachableTiles(gameState, tapped) else emptySet()
                val attackable = attackablePositions(gameState, tapped)
                gameState = gameState.copy(
                    selectedUnitId = tapped.id,
                    reachableTiles = reachable,
                    attackablePositions = attackable,
                    pendingMove = null
                )
            }

            // Tap reachable tile → move
            sel != null && pos in gameState.reachableTiles && !sel.hasMoved -> {
                gameState = moveUnit(gameState, sel.id, pos)
            }

            // Tap attackable position → attack
            sel != null && pos in gameState.attackablePositions -> {
                gameState = resolveAttack(gameState, sel.id, pos)
                val winner = checkWinner(gameState)
                when (winner) {
                    Side.PLAYER -> machine.enemyKingDied()
                    Side.ENEMY  -> machine.playerKingDied()
                    null        -> Unit
                }
            }

            // Tap elsewhere → deselect
            else -> {
                gameState = gameState.copy(
                    selectedUnitId = null,
                    reachableTiles = emptySet(),
                    attackablePositions = emptySet()
                )
            }
        }
    }

    val selectedHasMoved = selectedUnit?.hasMoved == true && gameState.pendingMove != null

    // ── Composable Tree ───────────────────────────────────────────────────────

    GameScaffold(
        title = "Empire Skirmish",
        onExit = onExit,
        hud = {
            GameHud(
                left  = "You: ${playerUnits.size}",
                center = when (phase) {
                    EmpireSkirmishPhase.PLAYER_TURN -> "Your Turn  T${gameState.turnNumber}"
                    EmpireSkirmishPhase.ENEMY_TURN  -> "Enemy Turn T${gameState.turnNumber}"
                    EmpireSkirmishPhase.VICTORY     -> "Victory! 🎉"
                    EmpireSkirmishPhase.DEFEAT      -> "Defeated 💀"
                    EmpireSkirmishPhase.IDLE        -> "Empire Skirmish"
                },
                right = "Enemy: ${enemyUnits.size}"
            )
        },
        board = {
            SkirmishBoard(
                state = gameState,
                textMeasurer = textMeasurer,
                onTileClicked = ::onTileClicked
            )
        },
        status = {
            when (phase) {
                EmpireSkirmishPhase.PLAYER_TURN -> {
                    PlayerControls(
                        selectedUnit = selectedUnit,
                        selectedHasMoved = selectedHasMoved,
                        onEndTurn = {
                            gameState = endPlayerTurn(gameState)
                            machine.endPlayerTurn()
                        },
                        onUndoMove = {
                            val id = gameState.selectedUnitId ?: return@PlayerControls
                            gameState = undoMove(gameState, id)
                        }
                    )
                }

                EmpireSkirmishPhase.ENEMY_TURN -> {
                    Text(
                        text = "Enemy is thinking…",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                EmpireSkirmishPhase.VICTORY -> {
                    VictoryPanel(
                        score = gameState.turnNumber,
                        bestScore = gameState.turnNumber,
                        stars = when {
                            gameState.turnNumber <= 5  -> 3
                            gameState.turnNumber <= 10 -> 2
                            else                       -> 1
                        },
                        onReplay = {
                            machine.replay()
                            val battle = generateBattle(System.currentTimeMillis(), difficulty)
                            gameState = initialState(battle)
                            machine.startBattle()
                        },
                        onMenu = {
                            machine.goToMenu()
                            onExit()
                        },
                        headline = "Victory!"
                    )
                }

                EmpireSkirmishPhase.DEFEAT -> {
                    DefeatPanel(
                        score = gameState.turnNumber,
                        bestScore = gameState.turnNumber,
                        onTryAgain = {
                            machine.replay()
                            val battle = generateBattle(System.currentTimeMillis(), difficulty)
                            gameState = initialState(battle)
                            machine.startBattle()
                        },
                        onMenu = {
                            machine.goToMenu()
                            onExit()
                        },
                        headline = "Defeated"
                    )
                }

                EmpireSkirmishPhase.IDLE -> Unit
            }
        }
    )
}

// ─── Board Canvas ─────────────────────────────────────────────────────────────

@Composable
private fun SkirmishBoard(
    state: EmpireSkirmishState,
    textMeasurer: TextMeasurer,
    onTileClicked: (GridPos) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(state) {
                detectTapGestures { offset ->
                    val cols = state.battle.cols.toFloat()
                    val rows = state.battle.rows.toFloat()
                    val cellW = size.width  / cols
                    val cellH = size.height / rows
                    val col = (offset.x / cellW).toInt().coerceIn(0, state.battle.cols - 1)
                    val row = (offset.y / cellH).toInt().coerceIn(0, state.battle.rows - 1)
                    onTileClicked(GridPos(col, row))
                }
            }
    ) {
        val cols = state.battle.cols
        val rows = state.battle.rows
        val cellW = size.width  / cols
        val cellH = size.height / rows

        // Background
        drawRect(color = GameCourt, size = size)

        // Tiles
        for (tile in state.battle.grid) {
            val left = tile.pos.x * cellW
            val top  = tile.pos.y * cellH
            when {
                tile.blocking -> drawRect(
                    color = Dark2,
                    topLeft = Offset(left, top),
                    size = Size(cellW, cellH)
                )
                tile.cover -> drawRect(
                    color = Dark1,
                    topLeft = Offset(left, top),
                    size = Size(cellW, cellH)
                )
            }
        }

        // Reachable tiles (blue overlay)
        for (pos in state.reachableTiles) {
            drawRect(
                color = GamePlayer.copy(alpha = 0.30f),
                topLeft = Offset(pos.x * cellW, pos.y * cellH),
                size = Size(cellW, cellH)
            )
        }

        // Attackable positions (red overlay)
        for (pos in state.attackablePositions) {
            drawRect(
                color = GameEnemy.copy(alpha = 0.30f),
                topLeft = Offset(pos.x * cellW, pos.y * cellH),
                size = Size(cellW, cellH)
            )
        }

        // Grid lines
        for (c in 0..cols) {
            val x = c * cellW
            drawLine(color = GameCourtLine.copy(alpha = 0.4f), start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
        }
        for (r in 0..rows) {
            val y = r * cellH
            drawLine(color = GameCourtLine.copy(alpha = 0.4f), start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
        }

        // Units
        for (unit in state.battle.units) {
            drawUnit(unit, cellW, cellH, state.selectedUnitId == unit.id, textMeasurer)
        }
    }
}

private fun DrawScope.drawUnit(
    unit: SkirmishUnit,
    cellW: Float,
    cellH: Float,
    selected: Boolean,
    textMeasurer: TextMeasurer
) {
    val left   = unit.pos.x * cellW
    val top    = unit.pos.y * cellH
    val cx     = left + cellW / 2f
    val cy     = top  + cellH / 2f
    val radius = minOf(cellW, cellH) * 0.36f

    val baseColor = if (unit.side == Side.PLAYER) GamePlayer else GameEnemy

    // Circle body
    drawCircle(color = baseColor, radius = radius, center = Offset(cx, cy))

    // HP arc (thin arc on top showing health fraction)
    val hpFrac = unit.hp.toFloat() / unit.maxHp
    drawArc(
        color = if (hpFrac > 0.5f) GameSuccess else GameHazard,
        startAngle = -90f,
        sweepAngle = 360f * hpFrac,
        useCenter = false,
        topLeft = Offset(left + cellW * 0.1f, top + cellH * 0.1f),
        size = Size(cellW * 0.8f, cellH * 0.8f),
        style = Stroke(width = 3f)
    )

    // Selected border
    if (selected) {
        drawRect(
            color = GameAccent,
            topLeft = Offset(left + 2f, top + 2f),
            size = Size(cellW - 4f, cellH - 4f),
            style = Stroke(width = 3f)
        )
    }

    // Unit type letter
    val letter = when (unit.type) {
        UnitType.SWORDSMAN -> "S"
        UnitType.BOWMAN    -> "B"
        UnitType.MAGE      -> "M"
        UnitType.KING      -> "K"
    }
    val textStyle = TextStyle(
        color = Dark0,
        fontSize = (minOf(cellW, cellH) * 0.30f).sp,
        fontWeight = FontWeight.Bold
    )
    val measured = textMeasurer.measure(letter, textStyle)
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(cx - measured.size.width / 2f, cy - measured.size.height / 2f)
    )

    // Dimmed overlay for used units (hasMoved AND hasAttacked)
    if (unit.hasMoved && unit.hasAttacked) {
        drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = radius, center = Offset(cx, cy))
    }
}

// ─── Player Controls ──────────────────────────────────────────────────────────

@Composable
private fun PlayerControls(
    selectedUnit: SkirmishUnit?,
    selectedHasMoved: Boolean,
    onEndTurn: () -> Unit,
    onUndoMove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Selected unit info
        if (selectedUnit != null) {
            Text(
                text = "${selectedUnit.type.name}  HP ${selectedUnit.hp}/${selectedUnit.maxHp}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onEndTurn) { Text("End Turn") }
            if (selectedHasMoved) {
                OutlinedButton(onClick = onUndoMove) { Text("Undo Move") }
            }
        }
    }
}
