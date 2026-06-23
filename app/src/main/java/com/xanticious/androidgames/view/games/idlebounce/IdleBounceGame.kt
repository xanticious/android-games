package com.xanticious.androidgames.view.games.idlebounce

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.idlebounce.IdleBounceController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.idlebounce.Ball
import com.xanticious.androidgames.model.games.idlebounce.BounceUpgrade
import com.xanticious.androidgames.model.games.idlebounce.IdleBounceGameState
import com.xanticious.androidgames.model.games.idlebounce.Layer
import com.xanticious.androidgames.model.games.idlebounce.LayerType
import com.xanticious.androidgames.state.games.idlebounce.IdleBouncePhase
import com.xanticious.androidgames.state.games.idlebounce.IdleBounceStateMachine
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun IdleBounceGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { IdleBounceController() }
    val machine = remember { IdleBounceStateMachine() }
    val phase by machine.phase.collectAsState()
    val stateHolder = rememberSaveable(stateSaver = IdleBounceGameStateSaver) {
        mutableStateOf(IdleBounceGameState.initial())
    }
    var state by stateHolder

    LaunchedEffect(Unit) {
        machine.startGame()
    }

    // Ball keeps bouncing even while the upgrade menu is open.
    GameLoop(running = phase == IdleBouncePhase.PLAYING || phase == IdleBouncePhase.UPGRADE_MENU_OPEN) { dt ->
        state = controller.step(state, dt).state
    }

    when (phase) {
        IdleBouncePhase.IDLE -> Box(modifier = Modifier.fillMaxSize())
        IdleBouncePhase.SETUP -> GameScaffold(title = "Idle Bounce", onExit = onExit) {
            SetupScreen(
                onHowToPlay = machine::openHowToPlay,
                onStart = machine::confirmStart
            )
        }
        IdleBouncePhase.HOW_TO_PLAY -> GameScaffold(title = "Idle Bounce", onExit = onExit) {
            HowToPlayScreen(onBack = machine::backToSetup)
        }
        IdleBouncePhase.PLAYING,
        IdleBouncePhase.UPGRADE_MENU_OPEN -> {
            val upgradeMenuOpen = phase == IdleBouncePhase.UPGRADE_MENU_OPEN
            GameScaffold(
                title = "Idle Bounce",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "⛏ Depth: -${state.depth}m",
                        center = "Idle Bounce",
                        right = "💰 ${state.coins}"
                    )
                },
                status = {
                    BounceStatus(
                        state = state,
                        controller = controller,
                        upgradeMenuOpen = upgradeMenuOpen,
                        onOpenUpgrades = machine::openUpgradeMenu,
                        onCloseUpgrades = machine::closeUpgradeMenu,
                        onBuyUpgrade = { upgradeId ->
                            controller.buyUpgrade(state, upgradeId)?.let { upgraded ->
                                state = upgraded
                                if (upgradeMenuOpen) {
                                    machine.upgradePurchased()
                                }
                            }
                        },
                        onPrestige = {
                            controller.prestige(state)?.let { state = it }
                        }
                    )
                }
            ) {
                BounceBoard(state = state)
            }
        }
    }
}

@Composable
private fun SetupScreen(onHowToPlay: () -> Unit, onStart: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Idle Bounce",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Drop a relentless mining ball through deeper layers, then prestige once bedrock is reached.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onHowToPlay) { Text("How to Play") }
            Button(onClick = onStart) { Text("Start Digging!") }
        }
    }
}

@Composable
private fun HowToPlayScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Text("How to Play", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("• The ball automatically bounces and chips away at the current layer.")
        Text("• Each destroyed layer awards coins and reveals the next depth band.")
        Text("• Bounce Power and Bounce Speed improve raw mining pace.")
        Text("• Ricochet doubles each hit, Lucky Strike can crit for 3× damage, and Drill Tip preserves overflow.")
        Text("• Reach bedrock to unlock a prestige reset that boosts all future rewards.")
    }
}

@Composable
private fun BounceBoard(state: IdleBounceGameState) {
    val backgroundColor = GameCourt
    val dirtColor = lerp(GameHazard, GameNeutral, 0.22f)
    val gravelColor = GameNeutral
    val stoneColor = GameCourtLine
    val oreColor = GameAccent
    val deepRockColor = GameEnemy
    val bedrockColor = Dark0
    val hpColor = GameSuccess
    val depletedColor = GameNeutral
    val ballColor = GameAccent

    val layerColor = when (state.currentLayer.type) {
        LayerType.DIRT -> dirtColor
        LayerType.GRAVEL -> gravelColor
        LayerType.STONE -> stoneColor
        LayerType.ORE_VEIN -> oreColor
        LayerType.DEEP_ROCK -> deepRockColor
        LayerType.BEDROCK -> bedrockColor
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            val layerHeight = size.height * 0.35f
            val layerTop = size.height - layerHeight
            val hpFraction = (state.currentLayer.hp.toFloat() / state.currentLayer.maxHp.toFloat()).coerceIn(0f, 1f)
            val bouncePhase = (state.bounceTimer / state.hitInterval).coerceIn(0f, 1f)
            val amplitude = layerTop - size.height * 0.18f
            val ballX = size.width * 0.5f
            val ballY = size.height * 0.18f + sin(bouncePhase * PI.toFloat()) * amplitude
            val barPadding = size.width * 0.08f
            val barTop = layerTop + layerHeight * 0.08f
            val barWidth = size.width - barPadding * 2f
            val barHeight = layerHeight * 0.08f

            drawRect(color = layerColor, topLeft = androidx.compose.ui.geometry.Offset(0f, layerTop), size = androidx.compose.ui.geometry.Size(size.width, layerHeight))
            drawRoundRect(
                color = depletedColor,
                topLeft = androidx.compose.ui.geometry.Offset(barPadding, barTop),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2f, barHeight / 2f)
            )
            drawRoundRect(
                color = hpColor,
                topLeft = androidx.compose.ui.geometry.Offset(barPadding, barTop),
                size = androidx.compose.ui.geometry.Size(barWidth * hpFraction, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2f, barHeight / 2f)
            )
            drawLine(
                color = GameCourtLine,
                start = androidx.compose.ui.geometry.Offset(0f, layerTop),
                end = androidx.compose.ui.geometry.Offset(size.width, layerTop),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
            drawCircle(color = ballColor, radius = size.minDimension * 0.05f, center = androidx.compose.ui.geometry.Offset(ballX, ballY))
            drawLine(
                color = GamePlayer,
                start = androidx.compose.ui.geometry.Offset(ballX, ballY + size.minDimension * 0.05f),
                end = androidx.compose.ui.geometry.Offset(ballX, layerTop),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
        ) {
            Text(
                text = "${state.currentLayer.type.readableName()} • ${state.currentLayer.hp}/${state.currentLayer.maxHp} HP",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BounceStatus(
    state: IdleBounceGameState,
    controller: IdleBounceController,
    upgradeMenuOpen: Boolean,
    onOpenUpgrades: () -> Unit,
    onCloseUpgrades: () -> Unit,
    onBuyUpgrade: (String) -> Unit,
    onPrestige: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (upgradeMenuOpen) {
                Button(onClick = onCloseUpgrades) { Text("Close Upgrades") }
            } else {
                Button(onClick = onOpenUpgrades) { Text("Open Upgrades") }
            }
            if (state.bedrockReached) {
                OutlinedButton(onClick = onPrestige) { Text("Reset Depth (Prestige)") }
            }
        }
        Text(
            text = if (state.lastHitCritical) "Critical bounce landed!" else "Upgrades apply when the menu is open.",
            color = if (state.lastHitCritical) GameAccent else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (state.lastHitCritical) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = "Destroyed layers: ${state.totalLayersDestroyed} • Prestige ${state.prestigeCount} • Reward ×${"%.2f".format(state.prestigeMultiplier)}",
            style = MaterialTheme.typography.bodySmall
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.upgrades, key = { it.id }) { upgrade ->
                    val cost = controller.upgradeCost(state, upgrade.id)
                    val isMaxed = cost == Long.MAX_VALUE
                    val canBuy = upgradeMenuOpen && !isMaxed && state.coins >= cost
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(upgrade.name, fontWeight = FontWeight.Bold)
                            Text(upgrade.description, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = if (upgrade.maxLevel < 0) {
                                    "Level ${upgrade.level}"
                                } else {
                                    "Level ${upgrade.level}/${upgrade.maxLevel}"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (isMaxed) "MAX" else "Cost: $cost")
                                Button(onClick = { onBuyUpgrade(upgrade.id) }, enabled = canBuy) {
                                    Text(if (isMaxed) "MAX" else "Buy")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LayerType.readableName(): String = name.lowercase().split('_').joinToString(" ") {
    it.replaceFirstChar { char -> char.titlecase() }
}

private val IdleBounceGameStateSaver: Saver<androidx.compose.runtime.MutableState<IdleBounceGameState>, Any> = listSaver(
    save = { holder ->
        listOf(
            holder.value.coins,
            holder.value.depth,
            holder.value.currentLayer.type.name,
            holder.value.currentLayer.maxHp,
            holder.value.currentLayer.hp,
            holder.value.currentLayer.reward,
            holder.value.ball.power,
            holder.value.ball.hitsPerSecond,
            holder.value.upgrades.joinToString(",") { it.level.toString() },
            holder.value.prestigeMultiplier,
            holder.value.prestigeCount,
            holder.value.bounceTimer,
            holder.value.bedrockReached,
            holder.value.carryOverDamage,
            holder.value.lastHitCritical,
            holder.value.totalLayersDestroyed
        )
    },
    restore = { values ->
        val upgradeLevels = (values[8] as String).split(',').filter { it.isNotBlank() }.map { it.toInt() }
        val upgrades = IdleBounceGameState.INITIAL_UPGRADES.mapIndexed { index, upgrade ->
            upgrade.copy(level = upgradeLevels.getOrElse(index) { upgrade.level })
        }
        mutableStateOf(
            IdleBounceGameState(
                coins = values[0] as Long,
                depth = values[1] as Int,
                currentLayer = Layer(
                    type = LayerType.valueOf(values[2] as String),
                    maxHp = values[3] as Long,
                    hp = values[4] as Long,
                    reward = values[5] as Long
                ),
                ball = Ball(
                    power = values[6] as Long,
                    hitsPerSecond = values[7] as Float
                ),
                upgrades = upgrades,
                prestigeMultiplier = values[9] as Float,
                prestigeCount = values[10] as Int,
                bounceTimer = values[11] as Float,
                bedrockReached = values[12] as Boolean,
                carryOverDamage = values[13] as Long,
                lastHitCritical = values[14] as Boolean,
                totalLayersDestroyed = values[15] as Int
            )
        )
    }
)
