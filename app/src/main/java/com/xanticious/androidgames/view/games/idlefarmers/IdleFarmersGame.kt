package com.xanticious.androidgames.view.games.idlefarmers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.idlefarmers.IdleFarmersController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.idlefarmers.Era
import com.xanticious.androidgames.model.games.idlefarmers.FarmUpgrade
import com.xanticious.androidgames.model.games.idlefarmers.IdleFarmersState
import com.xanticious.androidgames.model.games.idlefarmers.RandomEvent
import com.xanticious.androidgames.model.games.idlefarmers.RandomEventType
import com.xanticious.androidgames.model.games.idlefarmers.UpgradeEffect
import com.xanticious.androidgames.state.games.idlefarmers.IdleFarmersPhase
import com.xanticious.androidgames.state.games.idlefarmers.IdleFarmersStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.min

@Composable
fun IdleFarmersGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { IdleFarmersController() }
    val machine = remember { IdleFarmersStateMachine() }
    val phase by machine.phase.collectAsState()
    val displayDifficulty by rememberSaveable { mutableStateOf(difficulty) }

    var state by remember { mutableStateOf(controller.initialState()) }

    LaunchedEffect(Unit) { machine.startGame() }

    LaunchedEffect(phase) {
        if (phase != IdleFarmersPhase.PLAYING && phase != IdleFarmersPhase.EVENT_ACTIVE) {
            return@LaunchedEffect
        }
        if (phase == IdleFarmersPhase.EVENT_ACTIVE && state.activeEvent == null) {
            machine.eventResolved()
            return@LaunchedEffect
        }
        while (true) {
            delay(3_000L)
            state = controller.harvest(state)
            val newEvent = if (state.activeEvent == null) controller.maybeTriggeredEvent(state) else null
            if (newEvent != null) {
                state = state.copy(
                    activeEvent = newEvent,
                    eventLog = (state.eventLog + "Event started: ${newEvent.type.label}.").takeLast(10)
                )
                machine.eventTriggered()
            } else if (state.activeEvent == null && phase == IdleFarmersPhase.EVENT_ACTIVE) {
                machine.eventResolved()
            }
        }
    }

    val prestigeReached = state.upgrades.any { it.id == "orbital-greenhouse" && it.purchased }

    GameScaffold(
        title = "Idle Farmers",
        onExit = onExit,
        hud = {
            when (phase) {
                IdleFarmersPhase.PLAYING, IdleFarmersPhase.EVENT_ACTIVE -> {
                    GameHud(
                        left = "💰 ${state.coins} coins",
                        center = "${state.era.label} Era",
                        right = "Cycle ${state.cycleCount}"
                    )
                }

                IdleFarmersPhase.UPGRADE_MENU_OPEN -> {
                    GameHud(
                        left = "💰 ${state.coins}",
                        center = "Upgrades",
                        right = state.era.label
                    )
                }

                IdleFarmersPhase.IDLE, IdleFarmersPhase.SETUP, IdleFarmersPhase.HOW_TO_PLAY -> Unit
            }
        },
        status = {
            when (phase) {
                IdleFarmersPhase.PLAYING -> IdleFarmersStatus(
                    state = state,
                    prestigeReached = prestigeReached,
                    highlightEvent = false,
                    onOpenUpgrades = machine::openUpgrades
                )

                IdleFarmersPhase.EVENT_ACTIVE -> IdleFarmersStatus(
                    state = state,
                    prestigeReached = prestigeReached,
                    highlightEvent = true,
                    onOpenUpgrades = null
                )

                IdleFarmersPhase.UPGRADE_MENU_OPEN -> UpgradeMenuStatus(onClose = machine::closeUpgrades)
                IdleFarmersPhase.IDLE, IdleFarmersPhase.SETUP, IdleFarmersPhase.HOW_TO_PLAY -> Unit
            }
        }
    ) {
        when (phase) {
            IdleFarmersPhase.IDLE, IdleFarmersPhase.SETUP -> WelcomeScreen(
                difficulty = displayDifficulty,
                onStart = {
                    state = controller.initialState()
                    machine.startPlaying()
                },
                onHowToPlay = machine::openHowToPlay
            )

            IdleFarmersPhase.HOW_TO_PLAY -> HowToPlayScreen(onBack = machine::backToSetup)

            IdleFarmersPhase.PLAYING, IdleFarmersPhase.EVENT_ACTIVE -> IdleFarmersBoard(
                state = state,
                activeEvent = state.activeEvent
            )

            IdleFarmersPhase.UPGRADE_MENU_OPEN -> UpgradeMenu(
                state = state,
                controller = controller,
                onPurchase = { upgradeId ->
                    if (controller.canPurchase(state, upgradeId)) {
                        state = controller.purchaseUpgrade(state, upgradeId)
                    }
                }
            )
        }
    }
}

@Composable
private fun WelcomeScreen(
    difficulty: GameDifficulty,
    onStart: () -> Unit,
    onHowToPlay: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text("🌾 Idle Farmers", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "Grow a village farm from rough fields to orbital greenhouses.",
                textAlign = TextAlign.Center
            )
            Text(
                text = "Profile difficulty: ${difficulty.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onStart) { Text("Start Game") }
            OutlinedButton(onClick = onHowToPlay) { Text("How to Play") }
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
        OutlinedButton(onClick = onBack) { Text("← Back") }
        Text("How to Play", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("• Every 3 seconds your farm completes one harvest cycle.")
        Text("• Coins are earned from farmers, plots, automation, and yield multipliers.")
        Text("• Buy upgrades to unlock new eras, extra workers, more plots, and better sales.")
        Text("• Random events can hurt or help harvests for a few cycles.")
        Text("• Drought halves crops, pest swarms reduce them, bumper harvests double them, and festivals boost selling.")
        Text("• Prestige unlocks when you reach Orbital Greenhouse.")
    }
}

@Composable
private fun IdleFarmersBoard(state: IdleFarmersState, activeEvent: RandomEvent?) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(eraGradient(state.era)),
                size = size
            )

            val columns = min(4, state.plots.coerceAtLeast(1))
            val rows = ceil(state.plots / columns.toFloat()).toInt().coerceAtLeast(1)
            val boardPadding = 24.dp.toPx()
            val bannerSpace = 42.dp.toPx()
            val footerSpace = 36.dp.toPx()
            val plotGap = 10.dp.toPx()
            val plotWidth = ((size.width - (boardPadding * 2)) - plotGap * (columns - 1)) / columns
            val plotHeight = ((size.height - boardPadding - bannerSpace - footerSpace) - plotGap * (rows - 1)) / rows
            val plotTop = boardPadding + bannerSpace

            repeat(state.plots) { index ->
                val column = index % columns
                val row = index / columns
                val left = boardPadding + column * (plotWidth + plotGap)
                val top = plotTop + row * (plotHeight + plotGap)
                drawRect(
                    color = GameHazard.copy(alpha = 0.68f),
                    topLeft = Offset(left, top),
                    size = Size(plotWidth, plotHeight)
                )
                drawRect(
                    color = GameCourtLine.copy(alpha = 0.9f),
                    topLeft = Offset(left, top),
                    size = Size(plotWidth, plotHeight),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }

            val entityY = plotTop + rows * (plotHeight + plotGap) + 14.dp.toPx()
            repeat(state.farmers) { index ->
                val spacing = 28.dp.toPx()
                val x = boardPadding + 16.dp.toPx() + index * spacing
                drawCircle(color = GameSuccess, radius = 9.dp.toPx(), center = Offset(x, entityY))
                drawRect(
                    color = GamePlayer,
                    topLeft = Offset(x - 6.dp.toPx(), entityY + 10.dp.toPx()),
                    size = Size(12.dp.toPx(), 18.dp.toPx())
                )
            }

            repeat(state.automationCount) { index ->
                val spacing = 24.dp.toPx()
                val x = size.width - boardPadding - 18.dp.toPx() - index * spacing
                drawRect(
                    color = GameAccent,
                    topLeft = Offset(x - 9.dp.toPx(), entityY - 9.dp.toPx()),
                    size = Size(18.dp.toPx(), 18.dp.toPx())
                )
                drawRect(
                    color = GameCourtLine,
                    topLeft = Offset(x - 4.dp.toPx(), entityY + 12.dp.toPx()),
                    size = Size(8.dp.toPx(), 12.dp.toPx())
                )
            }
        }

        activeEvent?.let { event ->
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            ) {
                Text(
                    text = "${event.type.label} • ${event.remainingCycles} cycle(s) left",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    color = eventColor(event.type)
                )
            }
        }

        Text(
            text = eraFlavor(state.era),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun IdleFarmersStatus(
    state: IdleFarmersState,
    prestigeReached: Boolean,
    highlightEvent: Boolean,
    onOpenUpgrades: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.activeEvent?.let { event ->
            Text(
                text = "${event.type.label}: ${eventDescription(event, state)}",
                color = if (highlightEvent) eventColor(event.type) else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Text("Last harvest: 💰 ${state.lastHarvestAmount}")
        Text("Farmers ${state.farmers} • Plots ${state.plots} • Automation ${state.automationCount}")
        if (prestigeReached) {
            Text(
                text = "🎉 Orbital Greenhouse Reached!",
                color = GameSuccess,
                fontWeight = FontWeight.Bold
            )
        }
        onOpenUpgrades?.let {
            Button(onClick = it) { Text("Upgrades") }
        }
        if (state.eventLog.isNotEmpty()) {
            Text(
                text = state.eventLog.last(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UpgradeMenuStatus(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Button(onClick = onClose) { Text("Close Upgrades") }
    }
}

@Composable
private fun UpgradeMenu(
    state: IdleFarmersState,
    controller: IdleFarmersController,
    onPurchase: (String) -> Unit
) {
    val availableIds = controller.availableUpgrades(state).map { it.id }.toSet()
    val unpurchased = state.upgrades.filterNot { it.purchased }
    val purchased = state.upgrades.filter { it.purchased }
    val nameById = state.upgrades.associate { it.id to it.name }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GameCourt.copy(alpha = 0.08f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Available Upgrades", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        items(unpurchased, key = { it.id }) { upgrade ->
            UpgradeCard(
                upgrade = upgrade,
                canBuy = controller.canPurchase(state, upgrade.id),
                available = upgrade.id in availableIds,
                requiresLabel = upgrade.requires.joinToString { id -> nameById[id] ?: id },
                onBuy = { onPurchase(upgrade.id) }
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Purchased", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        items(purchased, key = { it.id }) { upgrade ->
            UpgradeCard(
                upgrade = upgrade,
                canBuy = false,
                available = true,
                requiresLabel = upgrade.requires.joinToString { id -> nameById[id] ?: id },
                onBuy = {}
            )
        }
    }
}

@Composable
private fun UpgradeCard(
    upgrade: FarmUpgrade,
    canBuy: Boolean,
    available: Boolean,
    requiresLabel: String,
    onBuy: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (upgrade.purchased) {
                MaterialTheme.colorScheme.secondaryContainer
            } else if (available) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(upgrade.name, fontWeight = FontWeight.Bold)
                Text("💰 ${upgrade.cost}")
            }
            Text(upgrade.description)
            Text("Era: ${upgrade.era.label}")
            if (requiresLabel.isNotBlank()) {
                Text(
                    text = "Requires: $requiresLabel",
                    color = if (available || upgrade.purchased) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        GameNeutral
                    }
                )
            }
            Text(effectLabel(upgrade.effect))
            if (!upgrade.purchased) {
                Button(onClick = onBuy, enabled = canBuy) { Text("Buy") }
            } else {
                Text("Purchased", color = GameSuccess, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun eraGradient(era: Era): List<Color> = when (era) {
    Era.NEOLITHIC -> listOf(GameHazard.copy(alpha = 0.45f), GameCourt)
    Era.ANCIENT -> listOf(GameAccent.copy(alpha = 0.35f), GameCourt)
    Era.CLASSICAL -> listOf(GameNeutral.copy(alpha = 0.35f), GameCourt)
    Era.MEDIEVAL -> listOf(GameSuccess.copy(alpha = 0.3f), GameCourt)
    Era.INDUSTRIAL -> listOf(GameEnemy.copy(alpha = 0.28f), GameCourt)
    Era.MODERN -> listOf(GamePlayer.copy(alpha = 0.3f), GameCourt)
    Era.BIOTECH -> listOf(GameSuccess.copy(alpha = 0.28f), GamePlayer.copy(alpha = 0.22f))
    Era.FUTURE -> listOf(GameAccent.copy(alpha = 0.22f), GamePlayer.copy(alpha = 0.35f))
}

private fun eraFlavor(era: Era): String = when (era) {
    Era.NEOLITHIC -> "Hand-tended fields and hardy seeds."
    Era.ANCIENT -> "Early surplus brings trade and irrigation."
    Era.CLASSICAL -> "Plows and storage stabilize the harvest."
    Era.MEDIEVAL -> "Systems and mills expand the village."
    Era.INDUSTRIAL -> "Steam and rail transform the farm."
    Era.MODERN -> "Mechanised harvests scale the operation."
    Era.BIOTECH -> "Precision crops and drones dominate."
    Era.FUTURE -> "Agriculture leaves the ground behind."
}

private fun eventDescription(event: RandomEvent, state: IdleFarmersState): String = when (event.type) {
    RandomEventType.DROUGHT ->
        if ("drought" in state.debuffImmunities) "Mitigated by irrigation." else "Crops are halved."

    RandomEventType.PEST_SWARM ->
        if ("pest_swarm" in state.debuffImmunities) "Mitigated by pesticides." else "Crops are reduced by 30%."

    RandomEventType.BUMPER_HARVEST -> "Crops are doubled."
    RandomEventType.FESTIVAL -> "Sales pay 1.5× this cycle."
}

private fun eventColor(type: RandomEventType): Color = when (type) {
    RandomEventType.DROUGHT -> GameHazard
    RandomEventType.PEST_SWARM -> GameEnemy
    RandomEventType.BUMPER_HARVEST -> GameSuccess
    RandomEventType.FESTIVAL -> GameAccent
}

private fun effectLabel(effect: UpgradeEffect): String = when (effect) {
    is UpgradeEffect.YieldMultiplier -> "Effect: ${effect.multiplier}× yield"
    is UpgradeEffect.ExtraFarmerSlot -> "Effect: +${effect.count} farmer"
    is UpgradeEffect.ExtraPlot -> "Effect: +${effect.count} plot"
    is UpgradeEffect.DebuffImmunity -> "Effect: Immune to ${effect.debuff.replace('_', ' ')}"
    UpgradeEffect.AutomationSlot -> "Effect: +1 automation slot"
    is UpgradeEffect.SellMultiplier -> "Effect: ${effect.multiplier}× sell price"
    UpgradeEffect.PrestigeUnlock -> "Effect: Prestige unlock"
}
