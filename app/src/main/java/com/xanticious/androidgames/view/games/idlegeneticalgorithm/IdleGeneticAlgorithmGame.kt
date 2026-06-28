package com.xanticious.androidgames.view.games.idlegeneticalgorithm

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.idlegeneticalgorithm.IdleGeneticAlgorithmController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.idlegeneticalgorithm.CarState
import com.xanticious.androidgames.model.games.idlegeneticalgorithm.GaUpgrade
import com.xanticious.androidgames.model.games.idlegeneticalgorithm.IdleGeneticAlgorithmState
import com.xanticious.androidgames.model.games.idlegeneticalgorithm.TrackPoint
import com.xanticious.androidgames.state.games.idlegeneticalgorithm.IdleGaPhase
import com.xanticious.androidgames.state.games.idlegeneticalgorithm.IdleGeneticAlgorithmStateMachine
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
import kotlin.math.max

@Composable
fun IdleGeneticAlgorithmGame(
    @Suppress("UNUSED_PARAMETER") difficulty: GameDifficulty,
    onExit: () -> Unit
) {
    val controller = remember { IdleGeneticAlgorithmController() }
    val machine = remember { IdleGeneticAlgorithmStateMachine() }
    val phase by machine.phase.collectAsState()

    var state by remember { mutableStateOf(controller.initialState()) }
    var cameraX by rememberSaveable { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) { machine.startSimulation() }

    LaunchedEffect(phase) {
        when (phase) {
            IdleGaPhase.SIMULATING -> {
                if (state.generation > 0 && state.simulationTime == 0f) {
                    cameraX = 0f
                }
            }
            IdleGaPhase.GENERATION_SUMMARY -> {
                if (controller.shouldAdvanceTrack(state)) {
                    Unit
                }
            }
            IdleGaPhase.IDLE,
            IdleGaPhase.HOW_TO_PLAY,
            IdleGaPhase.NEW_TRACK_INTRO,
            IdleGaPhase.UPGRADE_MENU_OPEN -> Unit
        }
    }

    GameLoop(running = phase == IdleGaPhase.SIMULATING) { dt ->
        state = controller.step(state, dt)
        cameraX = ((state.cars.maxOfOrNull { it.positionX } ?: 0f) - CAMERA_FOLLOW_OFFSET).coerceAtLeast(0f)
        if (controller.isGenerationOver(state.cars, state.simulationTime, state.fuelDuration)) {
            state = controller.finalizeGeneration(state)
            machine.generationCompleted()
        }
    }

    GameScaffold(
        title = "Idle Genetic Algorithm",
        onExit = onExit,
        hud = {
            when (phase) {
                IdleGaPhase.SIMULATING -> GameHud(
                    left = "💰 ${state.coins}",
                    center = "Gen ${state.generation}",
                    right = "Best: ${state.bestDistanceAllTime.toInt()}m"
                )
                IdleGaPhase.GENERATION_SUMMARY -> GameHud(
                    left = "💰 ${state.coins}",
                    center = "Generation Summary",
                    right = ""
                )
                IdleGaPhase.UPGRADE_MENU_OPEN -> GameHud(
                    left = "💰 ${state.coins}",
                    center = "Upgrades",
                    right = ""
                )
                IdleGaPhase.NEW_TRACK_INTRO -> GameHud(
                    left = "💰 ${state.coins}",
                    center = "New Track",
                    right = ""
                )
                IdleGaPhase.IDLE,
                IdleGaPhase.HOW_TO_PLAY -> Unit
            }
        },
        status = {
            when (phase) {
                IdleGaPhase.SIMULATING -> SimulationStatus(
                    state = state,
                    onOpenUpgrades = machine::openUpgrades
                )
                IdleGaPhase.GENERATION_SUMMARY -> GenerationSummaryStatus(
                    showAdvanceTrack = controller.shouldAdvanceTrack(state),
                    onNextGeneration = {
                        state = controller.startNextGeneration(state)
                        machine.startNextGeneration()
                    },
                    onBuyUpgrades = machine::openUpgrades,
                    onNewTrackReady = {
                        state = controller.advanceToNewTrack(state)
                        cameraX = 0f
                        machine.newTrackUnlocked()
                    }
                )
                IdleGaPhase.UPGRADE_MENU_OPEN -> StatusButtonRow(
                    primaryLabel = "Close",
                    onPrimary = {
                        if (controller.isGenerationOver(state.cars, state.simulationTime, state.fuelDuration)) {
                            state = if (controller.shouldAdvanceTrack(state)) {
                                controller.advanceToNewTrack(state)
                            } else {
                                controller.startNextGeneration(state)
                            }
                            cameraX = 0f
                        }
                        machine.closeUpgrades()
                    }
                )
                IdleGaPhase.NEW_TRACK_INTRO -> StatusButtonRow(
                    primaryLabel = "Start Evolution →",
                    onPrimary = machine::dismissNewTrack
                )
                IdleGaPhase.IDLE,
                IdleGaPhase.HOW_TO_PLAY -> Unit
            }
        }
    ) {
        when (phase) {
            IdleGaPhase.IDLE -> IdleGaStartScreen(onStart = machine::startSimulation)
            IdleGaPhase.HOW_TO_PLAY -> IdleGaHowToPlay(onDismiss = machine::dismissHowToPlay)
            IdleGaPhase.SIMULATING -> TrackBoard(
                controller = controller,
                state = state,
                cameraX = cameraX,
                aliveOnly = true
            )
            IdleGaPhase.GENERATION_SUMMARY -> GenerationSummaryBoard(
                controller = controller,
                state = state,
                cameraX = cameraX
            )
            IdleGaPhase.UPGRADE_MENU_OPEN -> UpgradeBoard(
                state = state,
                controller = controller,
                onPurchase = { upgrade ->
                    state = controller.purchaseUpgrade(state, upgrade.id)
                }
            )
            IdleGaPhase.NEW_TRACK_INTRO -> NewTrackBoard(
                controller = controller,
                state = state
            )
        }
    }
}

@Composable
private fun IdleGaStartScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🧬 Idle Genetic Algorithm",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Breed tiny racers, study the terrain, and spend coins to evolve farther.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onStart) { Text("Start Evolution") }
    }
}

@Composable
private fun IdleGaHowToPlay(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "How to Play",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text("🧪 Each generation launches a full set of AI cars on the current track.")
        Text("📏 Fitness is distance traveled. The best cars become parents for the next generation.")
        Text("🧬 Children inherit mixed traits and small mutations, so the fleet slowly improves.")
        Text("🪙 You earn coins from strong generation finishes and from new checkpoints crossed.")
        Text("⚙️ Spend coins on upgrades to extend fuel, raise speed caps, or improve evolution tools.")
        Text("🗺️ Once your all-time best reaches the track milestone, a fresh procedural course unlocks.")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onDismiss) { Text("Got it! Let's Evolve →") }
    }
}

@Composable
private fun TrackBoard(
    controller: IdleGeneticAlgorithmController,
    state: IdleGeneticAlgorithmState,
    cameraX: Float,
    aliveOnly: Boolean
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        drawTrackScene(
            controller = controller,
            state = state,
            cameraX = cameraX,
            aliveOnly = aliveOnly
        )
    }
}

@Composable
private fun SimulationStatus(
    state: IdleGeneticAlgorithmState,
    onOpenUpgrades: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Current generation progress",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        state.cars.forEach { car ->
            val progress = (car.positionX / state.trackAdvanceThreshold).coerceIn(0f, 1f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${car.id}",
                    modifier = Modifier.fillMaxWidth(0.12f),
                    color = if (car.alive) GamePlayer else GameEnemy
                )
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.weight(1f),
                    color = if (car.alive) GameSuccess else GameHazard,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text("${car.positionX.toInt()}m")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onOpenUpgrades) { Text("Upgrades") }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Generation in progress")
            }
        }
    }
}

@Composable
private fun GenerationSummaryBoard(
    controller: IdleGeneticAlgorithmController,
    state: IdleGeneticAlgorithmState,
    cameraX: Float
) {
    val latestResult = state.generationHistory.lastOrNull()
    val recentHistory = state.generationHistory.takeLast(5)
    val maxRecentDistance = max(1f, recentHistory.maxOfOrNull { it.bestDistance } ?: 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            drawTrackScene(
                controller = controller,
                state = state,
                cameraX = cameraX,
                aliveOnly = false
            )
        }
        latestResult?.let { result ->
            Text(
                text = "Gen ${result.generation} · Best ${result.bestDistance.toInt()}m · +${result.coinsEarned} coins",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        recentHistory.forEach { result ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Gen ${result.generation}: ${result.bestDistance.toInt()}m")
                LinearProgressIndicator(
                    progress = (result.bestDistance / maxRecentDistance).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(),
                    color = GameAccent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GenerationSummaryStatus(
    showAdvanceTrack: Boolean,
    onNextGeneration: () -> Unit,
    onBuyUpgrades: () -> Unit,
    onNewTrackReady: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNextGeneration) { Text("Next Generation") }
            OutlinedButton(onClick = onBuyUpgrades) { Text("Buy Upgrades") }
        }
        if (showAdvanceTrack) {
            Button(onClick = onNewTrackReady) { Text("New Track Ready!") }
        }
    }
}

@Composable
private fun UpgradeBoard(
    state: IdleGeneticAlgorithmState,
    controller: IdleGeneticAlgorithmController,
    onPurchase: (GaUpgrade) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.upgrades, key = { it.id }) { upgrade ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(upgrade.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(upgrade.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cost: ${upgrade.cost}")
                        Button(
                            onClick = { onPurchase(upgrade) },
                            enabled = controller.canPurchase(state, upgrade.id)
                        ) {
                            Text(if (upgrade.purchased) "Bought" else "Buy")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewTrackBoard(
    controller: IdleGeneticAlgorithmController,
    state: IdleGeneticAlgorithmState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🗺️ New Track!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "The next course is ready. Your evolved genomes will test themselves immediately.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            drawTrackScene(
                controller = controller,
                state = state,
                cameraX = 0f,
                aliveOnly = false,
                previewOnly = true
            )
        }
    }
}

@Composable
private fun StatusButtonRow(primaryLabel: String, onPrimary: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Button(onClick = onPrimary) { Text(primaryLabel) }
    }
}

private fun DrawScope.drawTrackScene(
    controller: IdleGeneticAlgorithmController,
    state: IdleGeneticAlgorithmState,
    cameraX: Float,
    aliveOnly: Boolean,
    previewOnly: Boolean = false
) {
    drawRect(GameCourt)

    val visibleWidth = if (previewOnly) {
        (state.track.lastOrNull()?.x ?: 200f).coerceAtLeast(200f)
    } else {
        220f
    }
    val xScale = size.width / visibleWidth
    val yScale = size.height / 140f
    val horizonY = size.height * 0.72f

    fun worldToScreenX(x: Float): Float = (x - cameraX) * xScale
    fun worldToScreenY(y: Float): Float = horizonY - y * yScale

    val surfacePath = Path().apply {
        moveTo(worldToScreenX(state.track.firstOrNull()?.x ?: 0f), size.height)
        state.track.forEach { point ->
            lineTo(worldToScreenX(point.x), worldToScreenY(point.y))
        }
        lineTo(worldToScreenX(state.track.lastOrNull()?.x ?: 0f), size.height)
        close()
    }
    drawPath(path = surfacePath, color = GameNeutral.copy(alpha = 0.85f))

    val linePath = Path().apply {
        state.track.firstOrNull()?.let { first ->
            moveTo(worldToScreenX(first.x), worldToScreenY(first.y))
            state.track.drop(1).forEach { point ->
                lineTo(worldToScreenX(point.x), worldToScreenY(point.y))
            }
        }
    }
    drawPath(path = linePath, color = GameCourtLine, style = Stroke(width = 4f))

    var checkpointX = state.checkpointInterval
    val trackEnd = state.track.lastOrNull()?.x ?: 0f
    while (checkpointX <= trackEnd) {
        val top = worldToScreenY(controller.trackHeightAt(state.track, checkpointX) + 16f)
        val bottom = worldToScreenY(controller.trackHeightAt(state.track, checkpointX) - 10f)
        drawLine(
            color = GameAccent,
            start = Offset(worldToScreenX(checkpointX), top),
            end = Offset(worldToScreenX(checkpointX), bottom),
            strokeWidth = 3f
        )
        checkpointX += state.checkpointInterval
    }

    val cars = if (aliveOnly) state.cars.filter { it.alive } else state.cars
    cars.forEach { car ->
        val carY = controller.trackHeightAt(state.track, car.positionX)
        val center = Offset(worldToScreenX(car.positionX), worldToScreenY(carY + 4f))
        drawCircle(
            color = Color.hsv(car.colorHue, 0.8f, 0.9f).copy(alpha = if (car.alive) 1f else 0.45f),
            radius = 8f,
            center = center
        )
    }
}

private const val CAMERA_FOLLOW_OFFSET = 120f
