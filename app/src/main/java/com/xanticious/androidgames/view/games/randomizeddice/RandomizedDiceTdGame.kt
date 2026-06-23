package com.xanticious.androidgames.view.games.randomizeddice

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
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.randomizeddice.CombatResolver
import com.xanticious.androidgames.controller.games.randomizeddice.DicePurchaser
import com.xanticious.androidgames.controller.games.randomizeddice.GameInitializer
import com.xanticious.androidgames.controller.games.randomizeddice.WaveStarter
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.GridPos
import com.xanticious.androidgames.model.games.randomizeddice.DiceTdGameState
import com.xanticious.androidgames.model.games.towerdefense.TowerRole
import com.xanticious.androidgames.state.games.randomizeddice.DiceTdPhase
import com.xanticious.androidgames.state.games.randomizeddice.RandomizedDiceTdStateMachine
import com.xanticious.androidgames.ui.theme.Aqua1
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
import androidx.compose.ui.input.pointer.pointerInput
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel

/** Randomized Dice TD — four purchase actions drive placement via seeded RNG. */
@Composable
fun RandomizedDiceTdGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val machine = remember { RandomizedDiceTdStateMachine() }
    val phase by machine.phase.collectAsState()

    val seed = rememberSaveable { System.currentTimeMillis() }

    var gameState by remember {
        mutableStateOf(GameInitializer.newGame(difficulty, seed))
    }

    // Inline selection state for "buy specific" and "upgrade specific" flows
    var buySpecificRole by rememberSaveable { mutableStateOf<TowerRole?>(null) }
    var upgradeSpecificMode by rememberSaveable { mutableStateOf(false) }

    // ── Initialisation ───────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        machine.startLevel()
    }

    // ── Combat loop ──────────────────────────────────────────────────────────
    GameLoop(running = phase == DiceTdPhase.WAVE) { dt ->
        val updated = CombatResolver.resolveTick(gameState, dt)
        gameState = updated
        val b = updated.base
        when {
            b.defeated -> machine.baseOverrun()
            b.allWavesCleared -> machine.allWavesCleared()
            b.enemies.isEmpty() && b.currentWave > 0 -> machine.waveCleared()
            else -> machine.tick()
        }
    }

    val b = gameState.base
    val costs = gameState.costs

    GameScaffold(
        title = "Randomized Dice TD",
        onExit = onExit,
        hud = {
            GameHud(
                left = "♥ ${b.lives}",
                center = "Wave ${b.currentWave}/${b.waves.size}",
                right = "$ ${b.money}"
            )
        },
        board = {
            DiceTdBoard(
                gameState = gameState,
                upgradeSpecificMode = upgradeSpecificMode,
                onTileClick = { tile ->
                    if (upgradeSpecificMode) {
                        val tower = b.towers.firstOrNull { it.tile == tile }
                        if (tower != null) {
                            val next = DicePurchaser.upgradeSpecific(gameState, tower.id)
                            if (next != null) {
                                gameState = next
                                machine.towerAction()
                            }
                        }
                        upgradeSpecificMode = false
                    } else {
                        val role = buySpecificRole
                        if (role != null) {
                            val next = DicePurchaser.buySpecific(gameState, role, tile)
                            if (next != null) {
                                gameState = next
                                machine.towerAction()
                            }
                            buySpecificRole = null
                        }
                    }
                }
            )
        },
        status = {
            when (phase) {
                DiceTdPhase.BUILDING -> BuildingControls(
                    gameState = gameState,
                    buySpecificRole = buySpecificRole,
                    upgradeSpecificMode = upgradeSpecificMode,
                    onBuyRandom = {
                        val next = DicePurchaser.buyRandom(gameState)
                        if (next != null) { gameState = next; machine.towerAction() }
                    },
                    onBuySpecificSelect = { buySpecificRole = it; upgradeSpecificMode = false },
                    onUpgradeRandom = {
                        val next = DicePurchaser.upgradeRandom(gameState)
                        if (next != null) { gameState = next; machine.towerAction() }
                    },
                    onUpgradeSpecificToggle = {
                        upgradeSpecificMode = !upgradeSpecificMode
                        buySpecificRole = null
                    },
                    onCallWave = {
                        gameState = WaveStarter.startWave(gameState)
                        machine.startWave()
                        buySpecificRole = null
                        upgradeSpecificMode = false
                    }
                )
                DiceTdPhase.WAVE -> {
                    Text(
                        text = "Wave in progress…",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                DiceTdPhase.VICTORY -> VictoryPanel(
                    score = b.money,
                    bestScore = b.money,
                    stars = 3,
                    onReplay = {
                        gameState = GameInitializer.newGame(difficulty, System.currentTimeMillis())
                        machine.retry()
                        buySpecificRole = null
                        upgradeSpecificMode = false
                    },
                    onMenu = { machine.goToMenu(); onExit() }
                )
                DiceTdPhase.DEFEAT -> DefeatPanel(
                    score = b.currentWave,
                    bestScore = b.waves.size,
                    onTryAgain = {
                        gameState = GameInitializer.newGame(difficulty, System.currentTimeMillis())
                        machine.retry()
                        buySpecificRole = null
                        upgradeSpecificMode = false
                    },
                    onMenu = { machine.goToMenu(); onExit() }
                )
                DiceTdPhase.IDLE -> {}
            }
        }
    )
}

// ─── Board Canvas ─────────────────────────────────────────────────────────────

@Composable
private fun DiceTdBoard(
    gameState: DiceTdGameState,
    upgradeSpecificMode: Boolean,
    onTileClick: (GridPos) -> Unit
) {
    val b = gameState.base
    val cols = b.map.cols
    val rows = b.map.rows
    val pathSet = b.map.path.toSet()
    val towerMap = b.towers.associateBy { it.tile }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(cols, rows) {
                detectTapGestures { offset ->
                    val cellW = this.size.width.toFloat() / cols
                    val cellH = this.size.height.toFloat() / rows
                    val tileX = (offset.x / cellW).toInt().coerceIn(0, cols - 1)
                    val tileY = (offset.y / cellH).toInt().coerceIn(0, rows - 1)
                    onTileClick(GridPos(tileX, tileY))
                }
            }
    ) {
        val cellW = size.width / cols
        val cellH = size.height / rows

        // ── Grid background ──────────────────────────────────────────────────
        for (x in 0 until cols) {
            for (y in 0 until rows) {
                val pos = GridPos(x, y)
                val color = when {
                    pos == b.map.basePos -> GameHazard
                    pos in pathSet -> Dark2
                    else -> GameCourt
                }
                drawRect(
                    color = color,
                    topLeft = Offset(x * cellW, y * cellH),
                    size = Size(cellW, cellH)
                )
                drawRect(
                    color = GameCourtLine,
                    topLeft = Offset(x * cellW, y * cellH),
                    size = Size(cellW, cellH),
                    style = Stroke(width = 0.5f)
                )
            }
        }

        // ── Path arrows (highlight direction) ───────────────────────────────
        b.map.path.forEachIndexed { i, pos ->
            if (i == b.map.path.lastIndex) return@forEachIndexed
            val cx = pos.x * cellW + cellW / 2
            val cy = pos.y * cellH + cellH / 2
            drawCircle(
                color = Dark1,
                radius = cellW * 0.12f,
                center = Offset(cx, cy)
            )
        }

        // ── Towers ──────────────────────────────────────────────────────────
        for ((tile, tower) in towerMap) {
            val cx = tile.x * cellW + cellW / 2
            val cy = tile.y * cellH + cellH / 2
            val towerColor = when (tower.role) {
                TowerRole.SINGLE_TARGET -> GamePlayer
                TowerRole.AOE -> GameAccent
                TowerRole.SLOW -> Aqua1
            }
            drawRect(
                color = towerColor,
                topLeft = Offset(tile.x * cellW + cellW * 0.15f, tile.y * cellH + cellH * 0.15f),
                size = Size(cellW * 0.7f, cellH * 0.7f)
            )
            // Level indicator dots
            for (lv in 1..tower.level) {
                drawCircle(
                    color = GameSuccess,
                    radius = cellW * 0.07f,
                    center = Offset(cx - cellW * 0.2f + (lv - 1) * cellW * 0.2f, cy + cellH * 0.28f)
                )
            }
            // Highlight if upgrade-specific mode
            if (upgradeSpecificMode && tower.level < 3) {
                drawRect(
                    color = GameAccent,
                    topLeft = Offset(tile.x * cellW + 2f, tile.y * cellH + 2f),
                    size = Size(cellW - 4f, cellH - 4f),
                    style = Stroke(width = 3f)
                )
            }
        }

        // ── Enemies ─────────────────────────────────────────────────────────
        for (enemy in b.enemies) {
            val progress = enemy.pathProgress.coerceIn(0f, (b.map.path.size - 1).toFloat())
            val idx = progress.toInt().coerceIn(0, b.map.path.lastIndex)
            val tile = b.map.path[idx]
            val frac = progress - idx
            val nextTile = b.map.path.getOrElse(idx + 1) { tile }
            val ex = (tile.x + (nextTile.x - tile.x) * frac) * cellW + cellW / 2
            val ey = (tile.y + (nextTile.y - tile.y) * frac) * cellH + cellH / 2

            val hpFrac = enemy.hp.toFloat() / enemy.maxHp.toFloat()
            val enemyColor = when {
                hpFrac > 0.6f -> GameEnemy
                hpFrac > 0.3f -> GameHazard
                else -> GameNeutral
            }
            drawCircle(
                color = enemyColor,
                radius = cellW * 0.28f,
                center = Offset(ex, ey)
            )
        }
    }
}

// ─── Building Controls ───────────────────────────────────────────────────────

@Composable
private fun BuildingControls(
    gameState: DiceTdGameState,
    buySpecificRole: TowerRole?,
    upgradeSpecificMode: Boolean,
    onBuyRandom: () -> Unit,
    onBuySpecificSelect: (TowerRole?) -> Unit,
    onUpgradeRandom: () -> Unit,
    onUpgradeSpecificToggle: () -> Unit,
    onCallWave: () -> Unit
) {
    val b = gameState.base
    val costs = gameState.costs

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ── Primary action buttons ────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedButton(
                onClick = onBuyRandom,
                modifier = Modifier.weight(1f),
                enabled = b.money >= costs.buyRandom
            ) { Text("Buy Rnd\n\$${costs.buyRandom}", style = MaterialTheme.typography.labelSmall) }

            OutlinedButton(
                onClick = { onBuySpecificSelect(if (buySpecificRole != null) null else TowerRole.SINGLE_TARGET) },
                modifier = Modifier.weight(1f),
                enabled = b.money >= costs.buySpecific
            ) { Text("Buy Spec\n\$${costs.buySpecific}", style = MaterialTheme.typography.labelSmall) }

            OutlinedButton(
                onClick = onUpgradeRandom,
                modifier = Modifier.weight(1f),
                enabled = b.money >= costs.upgradeRandom
            ) { Text("Upg Rnd\n\$${costs.upgradeRandom}", style = MaterialTheme.typography.labelSmall) }

            OutlinedButton(
                onClick = onUpgradeSpecificToggle,
                modifier = Modifier.weight(1f),
                enabled = b.towers.any { it.level < 3 }
            ) {
                val label = if (upgradeSpecificMode) "Cancel Upg" else "Upg Spec"
                Text("$label\n(tap tower)", style = MaterialTheme.typography.labelSmall)
            }
        }

        // ── Buy-specific inline role selector ────────────────────────────────
        if (buySpecificRole != null) {
            Text(
                text = "Select tower type, then tap a tile:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (role in TowerRole.entries) {
                    val selected = buySpecificRole == role
                    if (selected) {
                        Button(onClick = {}, modifier = Modifier.weight(1f)) {
                            Text(role.name.take(4), style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onBuySpecificSelect(role) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(role.name.take(4), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // ── Upgrade-specific cost hint ────────────────────────────────────────
        if (upgradeSpecificMode) {
            Text(
                text = "Tap a highlighted tower to upgrade it. Cost = base + perLevel × level",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // ── Call wave ─────────────────────────────────────────────────────────
        if (b.currentWave < b.waves.size) {
            Button(
                onClick = onCallWave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Call Next Wave Early (${b.currentWave + 1}/${b.waves.size})")
            }
        }
    }
}
