package com.xanticious.androidgames.view.games.ageofempirelite

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.ageofempirelite.AgeOfEmpiresLiteController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.ageofempirelite.AgeOfEmpiresLiteState
import com.xanticious.androidgames.model.games.ageofempirelite.ArmyComposition
import com.xanticious.androidgames.model.games.ageofempirelite.EconomyBalance
import com.xanticious.androidgames.model.games.ageofempirelite.King
import com.xanticious.androidgames.model.games.ageofempirelite.MilitaryUnit
import com.xanticious.androidgames.model.games.ageofempirelite.UpgradeId
import com.xanticious.androidgames.model.games.ageofempirelite.UpgradePriority
import com.xanticious.androidgames.model.games.ageofempirelite.UnitType
import com.xanticious.androidgames.state.games.ageofempirelite.AgeOfEmpiresLitePhase
import com.xanticious.androidgames.state.games.ageofempirelite.AgeOfEmpiresLiteStateMachine
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.ui.theme.GameCourt
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

private const val BOARD_WIDTH = 2000f
private const val BOARD_HEIGHT = 600f

@Composable
fun AgeOfEmpiresLiteGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val stateMachine = remember { AgeOfEmpiresLiteStateMachine() }
    val phase by stateMachine.phase.collectAsState()
    val gameState by stateMachine.gameState.collectAsState()

    var foodPct by rememberSaveable { mutableStateOf(70) }
    var armyRatios by remember {
        mutableStateOf(
            mapOf(UnitType.INFANTRY to 3, UnitType.ARCHER to 2, UnitType.CAVALRY to 1)
        )
    }
    var upgradePriority by remember {
        mutableStateOf(AgeOfEmpiresLiteController.defaultPlayerUpgradePriority())
    }
    var cannonQueueBuffer by rememberSaveable { mutableStateOf(0) }

    // Start match on first composition
    LaunchedEffect(Unit) {
        stateMachine.startMatch(
            AgeOfEmpiresLiteController.defaultMatchSettings(difficulty)
        )
    }

    GameLoop(running = phase == AgeOfEmpiresLitePhase.PLAYING) { dt ->
        stateMachine.setPolicy(
            economy = EconomyBalance(foodPct, 100 - foodPct),
            army = ArmyComposition(armyRatios),
            upgrades = upgradePriority,
            additionalCannonQueued = cannonQueueBuffer
        )
        cannonQueueBuffer = 0
        stateMachine.tick(dt)
    }

    val state = gameState

    GameScaffold(
        title = "Age of Empires Lite",
        onExit = onExit,
        hud = {
            if (state != null) {
                GameHud(
                    left = "🍖 ${state.playerResources.food}  📚 ${state.playerResources.study}",
                    center = "Workers: ${state.playerWorkers.size}/${ 10 }",
                    right = "Army: ${state.playerArmy.size}"
                )
            }
        },
        board = {
            BattlefieldCanvas(state = state)
        },
        status = {
            when (phase) {
                AgeOfEmpiresLitePhase.PLAYING -> {
                    if (state != null) {
                        PlayingStatusPanel(
                            state = state,
                            foodPct = foodPct,
                            armyRatios = armyRatios,
                            upgradePriority = upgradePriority,
                            onFoodPctChanged = { foodPct = it },
                            onArmyRatioChanged = { type, delta ->
                                armyRatios = armyRatios.toMutableMap().also { m ->
                                    m[type] = (m.getOrDefault(type, 0) + delta).coerceAtLeast(0)
                                }
                            },
                            onQueueCannon = { cannonQueueBuffer += it },
                            onUpgradePriorityChanged = { upgradePriority = it }
                        )
                    }
                }
                AgeOfEmpiresLitePhase.VICTORY -> {
                    VictoryPanel(
                        score = state?.elapsedSeconds?.toInt() ?: 0,
                        bestScore = 0,
                        stars = 3,
                        onReplay = {
                            stateMachine.returnToIdle()
                            stateMachine.startMatch(
                                AgeOfEmpiresLiteController.defaultMatchSettings(difficulty)
                            )
                        },
                        onMenu = {
                            stateMachine.returnToIdle()
                            onExit()
                        }
                    )
                }
                AgeOfEmpiresLitePhase.DEFEAT -> {
                    DefeatPanel(
                        score = state?.elapsedSeconds?.toInt() ?: 0,
                        bestScore = 0,
                        onTryAgain = {
                            stateMachine.returnToIdle()
                            stateMachine.startMatch(
                                AgeOfEmpiresLiteController.defaultMatchSettings(difficulty)
                            )
                        },
                        onMenu = {
                            stateMachine.returnToIdle()
                            onExit()
                        }
                    )
                }
                else -> {}
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Battlefield canvas
// ---------------------------------------------------------------------------

@Composable
private fun BattlefieldCanvas(state: AgeOfEmpiresLiteState?) {
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameCourt)
            .horizontalScroll(scrollState)
    ) {
        Canvas(
            modifier = Modifier
                .width(BOARD_WIDTH.dp)
                .fillMaxHeight()
        ) {
            val scaleX = size.width / BOARD_WIDTH
            val scaleY = size.height / BOARD_HEIGHT

            // Ground
            drawRect(color = Color(0xFF1A3A1A), size = Size(size.width, size.height))
            // Battlefield path line
            drawLine(
                color = GameNeutral.copy(alpha = 0.3f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2f
            )

            // Player village (left)
            drawVillage(
                centerX = 80f * scaleX,
                isPlayer = true,
                scaleX = scaleX,
                scaleY = scaleY
            )

            // Bot village (right)
            drawVillage(
                centerX = (BOARD_WIDTH - 80f) * scaleX,
                isPlayer = false,
                scaleX = scaleX,
                scaleY = scaleY
            )

            if (state != null) {
                // Kings
                drawKing(state.playerKing, scaleX, scaleY, GameSuccess)
                drawKing(state.botKing, scaleX, scaleY, GameHazard)

                // Player workers
                state.playerWorkers.forEachIndexed { i, _ ->
                    val x = (50f + (i % 4) * 25f) * scaleX
                    val y = (80f + (i / 4) * 25f) * scaleY
                    drawCircle(color = GamePlayer.copy(alpha = 0.7f), radius = 6f * scaleX, center = Offset(x, y))
                }

                // Bot workers
                state.botWorkers.forEachIndexed { i, _ ->
                    val x = (BOARD_WIDTH - 50f - (i % 4) * 25f) * scaleX
                    val y = (80f + (i / 4) * 25f) * scaleY
                    drawCircle(color = GameEnemy.copy(alpha = 0.7f), radius = 6f * scaleX, center = Offset(x, y))
                }

                // Military units
                state.playerArmy.forEach { unit -> drawUnit(unit, scaleX, scaleY) }
                state.botArmy.forEach { unit -> drawUnit(unit, scaleX, scaleY) }

                // Enlightenment countdown banner
                val cd = state.enlightenmentCountdown
                if (cd != null && cd > 0f) {
                    val side = if (state.enlightenmentSide == true) "YOU" else "ENEMY"
                    // Draw a highlighted banner text region (text drawn via Canvas text is complex;
                    // use a rect indicator instead and show text in the status panel)
                    drawRect(
                        color = GameAccent.copy(alpha = 0.3f),
                        topLeft = Offset(size.width * 0.3f, 8f),
                        size = Size(size.width * 0.4f, 28f)
                    )
                }
            }
        }
    }
}

private val GameAccent = Color(0xFFFFD43B)

private fun DrawScope.drawVillage(centerX: Float, isPlayer: Boolean, scaleX: Float, scaleY: Float) {
    val color = if (isPlayer) GamePlayer else GameEnemy
    val baseY = size.height / 2

    // Town Center
    drawRect(
        color = Dark2,
        topLeft = Offset(centerX - 30f * scaleX, baseY - 50f * scaleY),
        size = Size(60f * scaleX, 60f * scaleY)
    )

    // Library
    drawRect(
        color = Aqua3,
        topLeft = Offset(centerX - 20f * scaleX, baseY + 20f * scaleY),
        size = Size(40f * scaleX, 30f * scaleY)
    )

    // Barracks
    drawRect(
        color = GameNeutral,
        topLeft = Offset(centerX - 25f * scaleX, baseY - 100f * scaleY),
        size = Size(50f * scaleX, 40f * scaleY)
    )

    // HP bar for king (shown on village)
    drawRect(
        color = color.copy(alpha = 0.5f),
        topLeft = Offset(centerX - 30f * scaleX, baseY - 110f * scaleY),
        size = Size(60f * scaleX, 6f * scaleY)
    )
}

private fun DrawScope.drawKing(king: King, scaleX: Float, scaleY: Float, color: Color) {
    val x = king.pos.x * (size.width / 2000f)
    val y = king.pos.y * (size.height / 600f)
    val hpFraction = king.hp.toFloat() / king.maxHp.toFloat()

    // Crown shape (simplified diamond)
    val path = Path().apply {
        moveTo(x, y - 20f * scaleY)
        lineTo(x + 14f * scaleX, y)
        lineTo(x, y + 8f * scaleY)
        lineTo(x - 14f * scaleX, y)
        close()
    }
    drawPath(path, color)

    // HP bar
    drawRect(
        color = GameNeutral.copy(alpha = 0.4f),
        topLeft = Offset(x - 20f * scaleX, y - 30f * scaleY),
        size = Size(40f * scaleX, 5f * scaleY)
    )
    drawRect(
        color = color,
        topLeft = Offset(x - 20f * scaleX, y - 30f * scaleY),
        size = Size(40f * scaleX * hpFraction, 5f * scaleY)
    )
}

private fun DrawScope.drawUnit(unit: MilitaryUnit, scaleX: Float, scaleY: Float) {
    val x = unit.pos.x * (size.width / 2000f)
    val y = unit.pos.y * (size.height / 600f)
    val color = if (unit.side) GamePlayer else GameEnemy
    val hpFraction = unit.hp.toFloat() / unit.maxHp.toFloat()

    when (unit.type) {
        UnitType.INFANTRY -> drawCircle(color = color, radius = 8f * scaleX, center = Offset(x, y))
        UnitType.ARCHER -> {
            val path = Path().apply {
                moveTo(x, y - 10f * scaleY)
                lineTo(x + 8f * scaleX, y + 5f * scaleY)
                lineTo(x - 8f * scaleX, y + 5f * scaleY)
                close()
            }
            drawPath(path, color)
        }
        UnitType.CAVALRY -> {
            val path = Path().apply {
                moveTo(x, y - 10f * scaleY)
                lineTo(x + 10f * scaleX, y)
                lineTo(x, y + 10f * scaleY)
                lineTo(x - 10f * scaleX, y)
                close()
            }
            drawPath(path, color)
        }
        UnitType.CANNON -> drawRect(
            color = color,
            topLeft = Offset(x - 10f * scaleX, y - 6f * scaleY),
            size = Size(20f * scaleX, 12f * scaleY)
        )
    }

    // HP bar
    drawRect(
        color = GameNeutral.copy(alpha = 0.3f),
        topLeft = Offset(x - 8f * scaleX, y - 18f * scaleY),
        size = Size(16f * scaleX, 3f * scaleY)
    )
    drawRect(
        color = color.copy(alpha = 0.8f),
        topLeft = Offset(x - 8f * scaleX, y - 18f * scaleY),
        size = Size(16f * scaleX * hpFraction, 3f * scaleY)
    )
}

// ---------------------------------------------------------------------------
// Playing status panel
// ---------------------------------------------------------------------------

@Composable
private fun PlayingStatusPanel(
    state: AgeOfEmpiresLiteState,
    foodPct: Int,
    armyRatios: Map<UnitType, Int>,
    upgradePriority: UpgradePriority,
    onFoodPctChanged: (Int) -> Unit,
    onArmyRatioChanged: (UnitType, Int) -> Unit,
    onQueueCannon: (Int) -> Unit,
    onUpgradePriorityChanged: (UpgradePriority) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Enlightenment countdown
        val cd = state.enlightenmentCountdown
        if (cd != null && cd > 0f) {
            val side = if (state.enlightenmentSide == true) "YOU" else "ENEMY"
            Text(
                text = "⚡ $side: Enlightenment in ${cd.toInt()}s",
                style = MaterialTheme.typography.bodyMedium,
                color = GameAccent,
                fontWeight = FontWeight.Bold
            )
        }

        // Economy slider
        Text("Economy — Food ${foodPct}% / Study ${100 - foodPct}%",
            style = MaterialTheme.typography.labelSmall)
        Slider(
            value = foodPct.toFloat(),
            onValueChange = { onFoodPctChanged(it.toInt()) },
            valueRange = 0f..100f,
            steps = 9,
            modifier = Modifier.fillMaxWidth()
        )

        // Army composition
        Text("Army Composition", style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(UnitType.INFANTRY, UnitType.ARCHER, UnitType.CAVALRY).forEach { type ->
                val ratio = armyRatios.getOrDefault(type, 0)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(type.name.take(3), style = MaterialTheme.typography.labelSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { onArmyRatioChanged(type, -1) },
                            modifier = Modifier.width(32.dp).height(28.dp)) { Text("-") }
                        Text("$ratio", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                        Button(onClick = { onArmyRatioChanged(type, 1) },
                            modifier = Modifier.width(32.dp).height(28.dp)) { Text("+") }
                    }
                }
            }
        }

        // Cannon queue
        Text("Queue Cannons (300 food each)", style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(1, 2, 3).forEach { n ->
                Button(onClick = { onQueueCannon(n) },
                    modifier = Modifier.height(32.dp)) {
                    Text("Queue $n")
                }
            }
        }
        if (state.playerCannonQueue > 0) {
            Text("Cannons queued: ${state.playerCannonQueue}",
                style = MaterialTheme.typography.bodySmall, color = GameAccent)
        }

        // Victory conditions
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("🏆 Ploughshares: ${state.playerResources.food}/${state.ploughsharesThreshold}",
                style = MaterialTheme.typography.bodySmall)
            Text("🤖 Enemy: ${state.botResources.food}/${state.ploughsharesThreshold}",
                style = MaterialTheme.typography.bodySmall, color = GameEnemy)
        }

        // King HP
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("👑 Your King: ${state.playerKing.hp}/${state.playerKing.maxHp} HP",
                style = MaterialTheme.typography.bodySmall, color = GameSuccess)
            Text("👑 Enemy King: ${state.botKing.hp}/${state.botKing.maxHp} HP",
                style = MaterialTheme.typography.bodySmall, color = GameEnemy)
        }

        // Upgrades
        Text("Upgrade Priority (tap to move to top)", style = MaterialTheme.typography.labelSmall)
        upgradePriority.order.forEach { id ->
            val researched = id in state.playerResearched
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (if (researched) "✓ " else "  ") + id.name.replace("_", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (researched) GameSuccess else MaterialTheme.colorScheme.onSurface
                )
                if (!researched) {
                    Button(
                        onClick = {
                            val newOrder = mutableListOf(id)
                            newOrder.addAll(upgradePriority.order.filter { it != id })
                            onUpgradePriorityChanged(UpgradePriority(newOrder))
                        },
                        modifier = Modifier.height(24.dp)
                    ) { Text("↑", style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}
