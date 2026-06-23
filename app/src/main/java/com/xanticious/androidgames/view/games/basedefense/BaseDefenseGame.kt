package com.xanticious.androidgames.view.games.basedefense

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.basedefense.BaseDefenseController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.GridPos
import com.xanticious.androidgames.model.games.basedefense.BaseTower
import com.xanticious.androidgames.model.games.towerdefense.TdGameState
import com.xanticious.androidgames.model.games.towerdefense.Tower
import com.xanticious.androidgames.model.games.towerdefense.TowerRole
import com.xanticious.androidgames.state.games.basedefense.BaseDefensePhase
import com.xanticious.androidgames.state.games.basedefense.BaseDefenseStateMachine
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
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlinx.coroutines.delay

/**
 * Base Defense — tower-defense game (`design/tower-defense/base-defense.md`).
 *
 * Three tower types: GUN (single-target), MORTAR (AoE), FROST (slow).
 * The player builds towers during the Building phase, then watches combat
 * during the Wave phase.  Victory/defeat panels live in the status slot below
 * the board — never overlaid on the grid.
 */
@Composable
fun BaseDefenseGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val machine = remember { BaseDefenseStateMachine() }
    val phase by machine.phase.collectAsState()

    val seed = rememberSaveable { System.currentTimeMillis() }

    var gameState by remember {
        mutableStateOf(BaseDefenseController.initialState(difficulty, seed))
    }

    // Track how long the current building phase has been running for early-call bonus
    var buildingStartMs by remember { mutableLongStateOf(0L) }

    // Currently selected tile (for tower placement / upgrade UI inline below canvas)
    var selectedTile by remember { mutableStateOf<GridPos?>(null) }
    var selectedTower by remember { mutableStateOf<Tower?>(null) }

    // ── Initialisation ────────────────────────────────────────────────────────

    LaunchedEffect(Unit) {
        machine.levelStarted()
        buildingStartMs = System.currentTimeMillis()
    }

    // ── Phase reactions ───────────────────────────────────────────────────────

    LaunchedEffect(phase) {
        when (phase) {
            BaseDefensePhase.IDLE -> {
                // Wait for levelStarted — fired in the init effect above
            }
            BaseDefensePhase.BUILDING -> {
                buildingStartMs = System.currentTimeMillis()
            }
            BaseDefensePhase.WAVE -> {
                selectedTile = null
                selectedTower = null
            }
            BaseDefensePhase.VICTORY, BaseDefensePhase.DEFEAT -> {
                selectedTile = null
                selectedTower = null
            }
        }
    }

    // ── Combat game loop ──────────────────────────────────────────────────────

    GameLoop(running = phase == BaseDefensePhase.WAVE) { dt ->
        if (gameState.defeated) {
            machine.baseOverrun()
            return@GameLoop
        }
        gameState = BaseDefenseController.resolveTick(gameState, dt)

        // Wave cleared: no enemies left and all wave enemies have been spawned
        if (gameState.enemies.isEmpty() && gameState.currentWave > 0) {
            if (gameState.allWavesCleared) {
                machine.allWavesCleared()
            } else {
                machine.waveCleared()
            }
        }
    }

    // ── Auto-start next wave ──────────────────────────────────────────────────

    LaunchedEffect(phase, gameState.currentWave) {
        if (phase != BaseDefensePhase.BUILDING) return@LaunchedEffect
        if (gameState.wavesRemaining <= 0) return@LaunchedEffect
        delay(BaseDefenseController.AUTO_START_DELAY_MS)
        if (phase == BaseDefensePhase.BUILDING) {
            gameState = BaseDefenseController.startWave(gameState)
            machine.waveStarted()
        }
    }

    // ── Composable tree ───────────────────────────────────────────────────────

    GameScaffold(
        title = "Base Defense",
        onExit = onExit,
        hud = {
            GameHud(
                left = "❤ ${gameState.lives}",
                center = "Wave ${gameState.currentWave}/${gameState.waves.size}",
                right = "💰 ${gameState.money}"
            )
        },
        status = {
            when (phase) {
                BaseDefensePhase.VICTORY -> VictoryPanel(
                    score = gameState.lives * 100,
                    bestScore = gameState.lives * 100,
                    stars = when {
                        gameState.lives >= 20 -> 3
                        gameState.lives >= 10 -> 2
                        else -> 1
                    },
                    onReplay = {
                        gameState = BaseDefenseController.initialState(difficulty, System.currentTimeMillis())
                        machine.retry()
                        machine.levelStarted()
                    },
                    onMenu = { machine.menu(); onExit() }
                )
                BaseDefensePhase.DEFEAT -> DefeatPanel(
                    score = gameState.currentWave * 10,
                    bestScore = gameState.currentWave * 10,
                    onTryAgain = {
                        gameState = BaseDefenseController.initialState(difficulty, System.currentTimeMillis())
                        machine.retry()
                        machine.levelStarted()
                    },
                    onMenu = { machine.menu(); onExit() }
                )
                BaseDefensePhase.BUILDING -> BuildingControls(
                    gameState = gameState,
                    selectedTile = selectedTile,
                    selectedTower = selectedTower,
                    onPlaceTower = { tile, type ->
                        gameState = BaseDefenseController.placeTower(gameState, tile, type)
                            ?: gameState
                        selectedTile = null
                    },
                    onUpgradeTower = { towerId ->
                        gameState = BaseDefenseController.upgradeTower(gameState, towerId)
                            ?: gameState
                        selectedTower = null
                    },
                    onSellTower = { towerId ->
                        gameState = BaseDefenseController.sellTower(gameState, towerId)
                            ?: gameState
                        selectedTower = null
                    },
                    onCallWave = {
                        val skipped = BaseDefenseController.AUTO_START_DELAY_MS -
                            (System.currentTimeMillis() - buildingStartMs)
                        val bonus = BaseDefenseController.earlyCallBonus(skipped.coerceAtLeast(0L))
                        gameState = gameState.copy(money = gameState.money + bonus)
                        gameState = BaseDefenseController.startWave(gameState)
                        machine.waveStarted()
                    },
                    onClearSelection = {
                        selectedTile = null
                        selectedTower = null
                    }
                )
                else -> Unit
            }
        }
    ) {
        TdGrid(
            gameState = gameState,
            phase = phase,
            selectedTile = selectedTile,
            onTileClick = { tile ->
                if (phase != BaseDefensePhase.BUILDING) return@TdGrid
                val tower = gameState.towers.firstOrNull { it.tile == tile }
                if (tower != null) {
                    selectedTower = tower
                    selectedTile = null
                } else if (tile in gameState.map.buildable) {
                    selectedTile = tile
                    selectedTower = null
                } else {
                    selectedTile = null
                    selectedTower = null
                }
            }
        )
    }
}

// ── Grid canvas ───────────────────────────────────────────────────────────────

@Composable
private fun TdGrid(
    gameState: TdGameState,
    phase: BaseDefensePhase,
    selectedTile: GridPos?,
    onTileClick: (GridPos) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(phase) {
                detectTapGestures { offset ->
                    val tileW = size.width.toFloat() / BaseDefenseController.COLS
                    val tileH = size.height.toFloat() / BaseDefenseController.ROWS
                    val col = (offset.x / tileW).toInt().coerceIn(0, BaseDefenseController.COLS - 1)
                    val row = (offset.y / tileH).toInt().coerceIn(0, BaseDefenseController.ROWS - 1)
                    onTileClick(GridPos(col, row))
                }
            }
    ) {
        val tileW = size.width / BaseDefenseController.COLS
        val tileH = size.height / BaseDefenseController.ROWS
        val pathSet = gameState.map.path.toHashSet()

        // ── Draw tiles ────────────────────────────────────────────────────────
        for (col in 0 until BaseDefenseController.COLS) {
            for (row in 0 until BaseDefenseController.ROWS) {
                val tile = GridPos(col, row)
                val tileColor = when {
                    tile in pathSet -> GameCourtLine
                    tile in gameState.map.buildable -> Dark1
                    else -> Dark2
                }
                drawRect(
                    color = tileColor,
                    topLeft = Offset(col * tileW, row * tileH),
                    size = Size(tileW, tileH)
                )
                // Grid lines
                drawRect(
                    color = Dark0.copy(alpha = 0.4f),
                    topLeft = Offset(col * tileW, row * tileH),
                    size = Size(tileW, tileH),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                )
            }
        }

        // ── Highlight selected tile ───────────────────────────────────────────
        selectedTile?.let { tile ->
            drawRect(
                color = GameAccent.copy(alpha = 0.5f),
                topLeft = Offset(tile.x * tileW, tile.y * tileH),
                size = Size(tileW, tileH)
            )
        }

        // ── Draw base marker ──────────────────────────────────────────────────
        val base = gameState.map.basePos
        drawCircle(
            color = GameSuccess,
            radius = (tileW.coerceAtMost(tileH) * 0.35f),
            center = Offset((base.x + 0.5f) * tileW, (base.y + 0.5f) * tileH)
        )

        // ── Draw towers ───────────────────────────────────────────────────────
        for (tower in gameState.towers) {
            val cx = (tower.tile.x + 0.5f) * tileW
            val cy = (tower.tile.y + 0.5f) * tileH
            val r = (tileW.coerceAtMost(tileH) * 0.38f)
            val color = when (tower.role) {
                TowerRole.SINGLE_TARGET -> GamePlayer
                TowerRole.AOE -> GameHazard
                TowerRole.SLOW -> Aqua1
            }
            drawCircle(color = color, radius = r, center = Offset(cx, cy))
            // Level pips
            for (lvl in 0 until tower.level) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = r * 0.18f,
                    center = Offset(cx + (lvl - 0.5f) * r * 0.6f, cy + r * 0.65f)
                )
            }
        }

        // ── Draw enemies ──────────────────────────────────────────────────────
        for (enemy in gameState.enemies) {
            val (wx, wy) = BaseDefenseController.enemyWorldPos(enemy, gameState.map.path)
            val cx = wx * tileW
            val cy = wy * tileH
            val halfW = tileW * 0.28f
            val halfH = tileH * 0.28f
            val enemyColor = if (enemy.slowRemaining > 0f) Aqua1 else GameEnemy
            drawRect(
                color = enemyColor,
                topLeft = Offset(cx - halfW, cy - halfH),
                size = Size(halfW * 2, halfH * 2)
            )
            // HP bar
            val hpFrac = (enemy.hp.toFloat() / enemy.maxHp).coerceIn(0f, 1f)
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(cx - halfW, cy - halfH - tileH * 0.12f),
                size = Size(halfW * 2, tileH * 0.08f)
            )
            drawRect(
                color = GameSuccess,
                topLeft = Offset(cx - halfW, cy - halfH - tileH * 0.12f),
                size = Size(halfW * 2 * hpFrac, tileH * 0.08f)
            )
        }
    }
}

// ── Building-phase controls ───────────────────────────────────────────────────

@Composable
private fun BuildingControls(
    gameState: TdGameState,
    selectedTile: GridPos?,
    selectedTower: Tower?,
    onPlaceTower: (GridPos, BaseTower) -> Unit,
    onUpgradeTower: (Int) -> Unit,
    onSellTower: (Int) -> Unit,
    onCallWave: () -> Unit,
    onClearSelection: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when {
            selectedTile != null -> {
                Text(
                    text = "Place tower at (${selectedTile.x}, ${selectedTile.y})",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TowerButton(
                        label = "GUN\n💰${BaseDefenseController.towerCost(BaseTower.GUN)}",
                        enabled = gameState.money >= BaseDefenseController.towerCost(BaseTower.GUN),
                        color = GamePlayer
                    ) { onPlaceTower(selectedTile, BaseTower.GUN) }
                    TowerButton(
                        label = "MORTAR\n💰${BaseDefenseController.towerCost(BaseTower.MORTAR)}",
                        enabled = gameState.money >= BaseDefenseController.towerCost(BaseTower.MORTAR),
                        color = GameHazard
                    ) { onPlaceTower(selectedTile, BaseTower.MORTAR) }
                    TowerButton(
                        label = "FROST\n💰${BaseDefenseController.towerCost(BaseTower.FROST)}",
                        enabled = gameState.money >= BaseDefenseController.towerCost(BaseTower.FROST),
                        color = Aqua1
                    ) { onPlaceTower(selectedTile, BaseTower.FROST) }
                    OutlinedButton(onClick = onClearSelection) { Text("✕") }
                }
            }
            selectedTower != null -> {
                val baseTower = when (selectedTower.role) {
                    TowerRole.SINGLE_TARGET -> BaseTower.GUN
                    TowerRole.AOE -> BaseTower.MORTAR
                    TowerRole.SLOW -> BaseTower.FROST
                }
                val upgradeCost = BaseDefenseController.upgradeCost(baseTower, selectedTower.level)
                Text(
                    text = "${baseTower.name} Lv${selectedTower.level + 1} at (${selectedTower.tile.x}, ${selectedTower.tile.y})",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (selectedTower.level < 2) {
                        Button(
                            onClick = { onUpgradeTower(selectedTower.id) },
                            enabled = gameState.money >= upgradeCost
                        ) { Text("⬆ Upgrade 💰$upgradeCost", fontWeight = FontWeight.Bold) }
                    }
                    OutlinedButton(onClick = { onSellTower(selectedTower.id) }) {
                        Text("Sell")
                    }
                    OutlinedButton(onClick = onClearSelection) { Text("✕") }
                }
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (gameState.wavesRemaining > 0) {
                        Button(onClick = onCallWave) {
                            Text("▶ Call Wave ${gameState.currentWave + 1} (bonus)")
                        }
                    } else {
                        Text(
                            text = "Waiting for last enemies…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        text = "Tap a buildable tile to place",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TowerButton(
    label: String,
    enabled: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
