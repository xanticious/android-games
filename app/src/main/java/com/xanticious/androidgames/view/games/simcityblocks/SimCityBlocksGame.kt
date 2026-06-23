package com.xanticious.androidgames.view.games.simcityblocks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.simcityblocks.SimCityBlocksController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.simcityblocks.CivicBuilding
import com.xanticious.androidgames.model.games.simcityblocks.CivicType
import com.xanticious.androidgames.model.games.simcityblocks.CityBuilding
import com.xanticious.androidgames.model.games.simcityblocks.CityPoint
import com.xanticious.androidgames.model.games.simcityblocks.CityPurchase
import com.xanticious.androidgames.model.games.simcityblocks.CityTerrain
import com.xanticious.androidgames.model.games.simcityblocks.CycleSpeed
import com.xanticious.androidgames.model.games.simcityblocks.DisasterFrequency
import com.xanticious.androidgames.model.games.simcityblocks.SimCityBlocksState
import com.xanticious.androidgames.model.games.simcityblocks.UpgradeType
import com.xanticious.androidgames.model.games.simcityblocks.ZoneBuilding
import com.xanticious.androidgames.model.games.simcityblocks.ZoneType
import com.xanticious.androidgames.state.games.simcityblocks.SimCityBlocksPhase
import com.xanticious.androidgames.state.games.simcityblocks.SimCityBlocksStateMachine
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.ui.theme.Dark1
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
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.random.Random

private enum class SimCityBlocksScreen { SETTINGS, HOW_TO_PLAY, GAME }

@Composable
fun SimCityBlocksGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { SimCityBlocksController() }
    val machine = remember { SimCityBlocksStateMachine() }
    val phase by machine.phase.collectAsState()
    var screen by rememberSaveable { mutableStateOf(SimCityBlocksScreen.SETTINGS) }
    var selectedDifficulty by rememberSaveable { mutableStateOf(difficulty) }
    var cycleSpeed by rememberSaveable { mutableStateOf(CycleSpeed.NORMAL) }
    var disasterFrequency by rememberSaveable { mutableStateOf(DisasterFrequency.RARE) }
    var showJobs by rememberSaveable { mutableStateOf(true) }
    var showEnergy by rememberSaveable { mutableStateOf(false) }
    val config = remember(selectedDifficulty) { controller.configFor(selectedDifficulty) }
    var state by remember { mutableStateOf(controller.initialState(config)) }
    var selectedBuildingId by rememberSaveable { mutableStateOf<Int?>(null) }
    var dayAccumulator by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(config) {
        if (screen != SimCityBlocksScreen.GAME) {
            state = controller.initialState(config)
        }
    }

    LaunchedEffect(phase, state.message) {
        if (phase == SimCityBlocksPhase.DISASTER) {
            delay(DISASTER_BANNER_MILLIS)
            machine.disasterAcknowledged()
        }
    }

    GameLoop(running = screen == SimCityBlocksScreen.GAME && phase == SimCityBlocksPhase.SIMULATING) { dt ->
        dayAccumulator += dt
        if (dayAccumulator >= cycleSpeed.secondsPerDay) {
            dayAccumulator = 0f
            val outcome = controller.advanceCycle(
                state = state,
                config = config,
                random = Random.Default,
                disasterChance = controller.disasterChanceFor(disasterFrequency, config)
            )
            state = outcome.state
            when {
                outcome.state.gameOver -> machine.deficitCritical()
                outcome.disasterTriggered -> machine.disasterTriggered()
                outcome.zoneAbandoned -> machine.zoneAbandoned()
                else -> machine.cycleAdvanced()
            }
        }
    }

    GameScaffold(
        title = "Sim City Blocks",
        onExit = onExit,
        hud = {
            if (screen == SimCityBlocksScreen.GAME) {
                GameHud(
                    left = "Pop ${state.resources.population}",
                    center = "${'$'}${state.resources.budget}",
                    right = "Day ${state.resources.day}"
                )
                GameHud(
                    left = "Jobs ${state.resources.employed}/${state.resources.jobs}",
                    center = "Energy ${state.resources.energyUsed}/${state.resources.energyCapacity}",
                    right = "Net ${state.resources.netBalance}"
                )
            }
        },
        status = {
            if (phase == SimCityBlocksPhase.GAME_OVER) {
                DefeatPanel(
                    score = state.resources.peakPopulation,
                    bestScore = state.resources.peakPopulation,
                    headline = "City Collapsed",
                    onTryAgain = {
                        state = controller.initialState(config)
                        selectedBuildingId = null
                        dayAccumulator = 0f
                        machine.continueCity()
                    },
                    onMenu = onExit
                )
            }
        }
    ) {
        when (screen) {
            SimCityBlocksScreen.SETTINGS -> SimCityBlocksSettings(
                difficulty = selectedDifficulty,
                onDifficulty = { selectedDifficulty = it },
                cycleSpeed = cycleSpeed,
                onCycleSpeed = { cycleSpeed = it },
                disasterFrequency = disasterFrequency,
                onDisasterFrequency = { disasterFrequency = it },
                showJobs = showJobs,
                onShowJobs = { showJobs = it },
                showEnergy = showEnergy,
                onShowEnergy = { showEnergy = it },
                onHowToPlay = { screen = SimCityBlocksScreen.HOW_TO_PLAY },
                onStart = {
                    state = controller.initialState(config)
                    selectedBuildingId = null
                    dayAccumulator = 0f
                    screen = SimCityBlocksScreen.GAME
                    machine.startGame()
                }
            )
            SimCityBlocksScreen.HOW_TO_PLAY -> SimCityBlocksHowToPlay(onBack = { screen = SimCityBlocksScreen.SETTINGS })
            SimCityBlocksScreen.GAME -> SimCityBlocksBoard(
                state = state,
                phase = phase,
                selectedBuildingId = selectedBuildingId,
                showJobs = showJobs,
                showEnergy = showEnergy,
                onSelect = { selectedBuildingId = it },
                onPurchase = { purchase ->
                    machine.actionTaken()
                    val outcome = controller.purchase(state, config, purchase)
                    state = outcome.state
                    machine.buildResolved()
                },
                validation = { purchase -> controller.validatePurchase(state, config, purchase) }
            )
        }
    }
}

@Composable
private fun SimCityBlocksSettings(
    difficulty: GameDifficulty,
    onDifficulty: (GameDifficulty) -> Unit,
    cycleSpeed: CycleSpeed,
    onCycleSpeed: (CycleSpeed) -> Unit,
    disasterFrequency: DisasterFrequency,
    onDisasterFrequency: (DisasterFrequency) -> Unit,
    showJobs: Boolean,
    onShowJobs: (Boolean) -> Unit,
    showEnergy: Boolean,
    onShowEnergy: (Boolean) -> Unit,
    onHowToPlay: () -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Balance a growing offline city. The city planner places each purchase automatically.")
        Text("Difficulty", fontWeight = FontWeight.Bold)
        ChipRow { GameDifficulty.entries.forEach { level -> FilterChip(selected = difficulty == level, onClick = { onDifficulty(level) }, label = { Text(level.label) }) } }
        Text("Cycle speed", fontWeight = FontWeight.Bold)
        ChipRow { CycleSpeed.entries.forEach { speed -> FilterChip(selected = cycleSpeed == speed, onClick = { onCycleSpeed(speed) }, label = { Text(speed.label) }) } }
        Text("Disaster frequency", fontWeight = FontWeight.Bold)
        ChipRow { DisasterFrequency.entries.forEach { frequency -> FilterChip(selected = disasterFrequency == frequency, onClick = { onDisasterFrequency(frequency) }, label = { Text(frequency.label) }) } }
        Text("Overlays", fontWeight = FontWeight.Bold)
        ChipRow {
            FilterChip(selected = showJobs, onClick = { onShowJobs(!showJobs) }, label = { Text("Jobs") })
            FilterChip(selected = showEnergy, onClick = { onShowEnergy(!showEnergy) }, label = { Text("Energy") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onHowToPlay) { Text("How to Play") }
            Button(onClick = onStart) { Text("Start City") }
        }
    }
}

@Composable
private fun SimCityBlocksHowToPlay(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("How to Play", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Buy residential zones for workers, commercial and industrial zones for jobs, then add parks and civic coverage.")
        Text("Upgrade energy before upgrading zones. If energy use exceeds capacity, rolling brownouts shut buildings off.")
        Text("Residents without jobs lose happiness. Empty neighborhoods abandon the city. Ten critical deficit days ends the run.")
        Text("Random disasters can burn blocks, cause slumps, trigger blackouts, or spark an exodus.")
        OutlinedButton(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun SimCityBlocksBoard(
    state: SimCityBlocksState,
    phase: SimCityBlocksPhase,
    selectedBuildingId: Int?,
    showJobs: Boolean,
    showEnergy: Boolean,
    onSelect: (Int?) -> Unit,
    onPurchase: (CityPurchase) -> Unit,
    validation: (CityPurchase) -> com.xanticious.androidgames.model.games.simcityblocks.PurchaseValidation
) {
    val selected = state.grid.buildings.firstOrNull { it.id == selectedBuildingId }
    Column(modifier = Modifier.fillMaxSize()) {
        if (state.resources.negativeCycles >= 3) {
            Text(
                text = "Deficit warning: purchases locked until revenue recovers",
                modifier = Modifier.fillMaxWidth().background(GameHazard.copy(alpha = 0.15f)).padding(8.dp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
        if (phase == SimCityBlocksPhase.DISASTER) {
            Text(
                text = state.message,
                modifier = Modifier.fillMaxWidth().background(GameEnemy.copy(alpha = 0.15f)).padding(8.dp),
                fontWeight = FontWeight.Bold
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp)) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            Canvas(
                modifier = Modifier.fillMaxSize().pointerInput(state.grid.buildings) {
                    detectTapGestures { offset ->
                        val cell = min(size.width / state.grid.width, size.height / state.grid.height)
                        val originX = (size.width - cell * state.grid.width) / 2f
                        val originY = (size.height - cell * state.grid.height) / 2f
                        val x = ((offset.x - originX) / cell).toInt()
                        val y = ((offset.y - originY) / cell).toInt()
                        onSelect(state.grid.buildings.firstOrNull { it.position == CityPoint(x, y) }?.id)
                    }
                }
            ) {
                val cell = min(width / state.grid.width, height / state.grid.height)
                val origin = Offset((width - cell * state.grid.width) / 2f, (height - cell * state.grid.height) / 2f)
                drawCity(state, cell, origin, selectedBuildingId, showJobs, showEnergy)
            }
        }
        selected?.let { BuildingDetail(it) }
        Text(
            text = state.message,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        ShopPanel(onPurchase = onPurchase, validation = validation)
    }
}

@Composable
private fun BuildingDetail(building: CityBuilding) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        val text = when (building) {
            is ZoneBuilding -> "${building.type.name.lowercase()} L${building.level} • pop ${building.population} • happiness ${building.happiness} • ${if (building.powered) "powered" else "brownout"}"
            is CivicBuilding -> "${building.type.name.lowercase().replace('_', ' ')} L${building.level} • ${if (building.powered) "powered" else "brownout"}"
        }
        Text(text = text, modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ShopPanel(
    onPurchase: (CityPurchase) -> Unit,
    validation: (CityPurchase) -> com.xanticious.androidgames.model.games.simcityblocks.PurchaseValidation
) {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PurchaseButton("Buy Res", CityPurchase.BuyZone(ZoneType.RESIDENTIAL), validation, onPurchase)
            PurchaseButton("Buy Com", CityPurchase.BuyZone(ZoneType.COMMERCIAL), validation, onPurchase)
            PurchaseButton("Buy Ind", CityPurchase.BuyZone(ZoneType.INDUSTRIAL), validation, onPurchase)
            PurchaseButton("Fire", CityPurchase.BuildCivic(CivicType.FIRE_STATION), validation, onPurchase)
            PurchaseButton("Police", CityPurchase.BuildCivic(CivicType.POLICE_STATION), validation, onPurchase)
            PurchaseButton("Park", CityPurchase.BuildCivic(CivicType.PARK), validation, onPurchase)
        }
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UpgradeType.entries.forEach { upgrade ->
                PurchaseButton("Up ${upgrade.name.lowercase().take(5)}", CityPurchase.UpgradeCity(upgrade), validation, onPurchase)
            }
        }
    }
}

@Composable
private fun PurchaseButton(
    label: String,
    purchase: CityPurchase,
    validation: (CityPurchase) -> com.xanticious.androidgames.model.games.simcityblocks.PurchaseValidation,
    onPurchase: (CityPurchase) -> Unit
) {
    val result = validation(purchase)
    Button(enabled = result.canPurchase, onClick = { onPurchase(purchase) }) {
        Text("$label ${'$'}${result.cost}${if (result.canPurchase) "" else " ${result.reason}"}")
    }
}

@Composable
private fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

private fun DrawScope.drawCity(
    state: SimCityBlocksState,
    cell: Float,
    origin: Offset,
    selectedBuildingId: Int?,
    showJobs: Boolean,
    showEnergy: Boolean
) {
    drawRect(GameCourt, topLeft = origin, size = Size(cell * state.grid.width, cell * state.grid.height))
    state.grid.tiles.forEach { tile ->
        val topLeft = origin + Offset(tile.position.x * cell, tile.position.y * cell)
        drawRect(Dark1, topLeft = topLeft, size = Size(cell, cell), alpha = 0.35f)
        drawRect(GameCourtLine.copy(alpha = 0.25f), topLeft = topLeft, size = Size(cell, cell), style = Stroke(width = 1f))
        drawTerrain(tile.terrain, topLeft, cell)
    }
    state.grid.buildings.forEach { building -> drawBuilding(building, origin, cell, selectedBuildingId == building.id, showJobs, showEnergy) }
    state.activeDisasters.flatMap { it.affectedCells }.forEach { point ->
        drawRoundRect(GameHazard.copy(alpha = 0.28f), topLeft = origin + Offset(point.x * cell, point.y * cell), size = Size(cell, cell), cornerRadius = CornerRadius(cell * 0.18f))
    }
}

private fun DrawScope.drawTerrain(terrain: CityTerrain, topLeft: Offset, cell: Float) {
    when (terrain) {
        CityTerrain.EMPTY -> Unit
        CityTerrain.DIRT_ROAD, CityTerrain.PAVED_ROAD -> drawRect(if (terrain == CityTerrain.PAVED_ROAD) GameNeutral else GameHazard.copy(alpha = 0.45f), topLeft = topLeft + Offset(0f, cell * 0.42f), size = Size(cell, cell * 0.16f))
        CityTerrain.SINGLE_RAIL, CityTerrain.DOUBLE_RAIL -> {
            drawRect(GameNeutral, topLeft = topLeft + Offset(0f, cell * 0.34f), size = Size(cell, cell * 0.08f))
            drawRect(GameNeutral, topLeft = topLeft + Offset(0f, cell * 0.58f), size = Size(cell, cell * 0.08f))
            if (terrain == CityTerrain.DOUBLE_RAIL) drawRect(Aqua4, topLeft = topLeft + Offset(0f, cell * 0.46f), size = Size(cell, cell * 0.06f))
        }
    }
}

private fun DrawScope.drawBuilding(building: CityBuilding, origin: Offset, cell: Float, selected: Boolean, showJobs: Boolean, showEnergy: Boolean) {
    val topLeft = origin + Offset(building.position.x * cell, building.position.y * cell)
    if (selected) drawRoundRect(GameAccent.copy(alpha = 0.35f), topLeft = topLeft, size = Size(cell, cell), cornerRadius = CornerRadius(cell * 0.2f))
    val color = if (building.powered) colorFor(building) else GameNeutral
    when (building) {
        is ZoneBuilding -> drawZone(building, topLeft, cell, color)
        is CivicBuilding -> drawCivic(building, topLeft, cell, color)
    }
    if (showJobs && building is ZoneBuilding) drawJobOverlay(building, topLeft, cell)
    if (showEnergy) drawCircle(if (building.powered) GameAccent else GameHazard, radius = cell * 0.07f, center = topLeft + Offset(cell * 0.82f, cell * 0.2f))
}

private fun DrawScope.drawZone(zone: ZoneBuilding, topLeft: Offset, cell: Float, color: Color) {
    when (zone.type) {
        ZoneType.RESIDENTIAL -> {
            val roof = Path().apply {
                moveTo(topLeft.x + cell * 0.2f, topLeft.y + cell * 0.48f)
                lineTo(topLeft.x + cell * 0.5f, topLeft.y + cell * 0.2f)
                lineTo(topLeft.x + cell * 0.8f, topLeft.y + cell * 0.48f)
                close()
            }
            drawPath(roof, GameAccent)
            drawRoundRect(color, topLeft + Offset(cell * 0.27f, cell * 0.45f), Size(cell * 0.46f, cell * if (zone.level > 1) 0.42f else 0.34f), CornerRadius(cell * 0.06f))
            drawCircle(if (zone.happiness > 65) GameSuccess else if (zone.happiness > 30) GameAccent else GameEnemy, radius = cell * 0.07f, center = topLeft + Offset(cell * 0.5f, cell * 0.12f))
        }
        ZoneType.COMMERCIAL -> {
            drawRoundRect(color, topLeft + Offset(cell * 0.2f, cell * 0.28f), Size(cell * 0.6f, cell * 0.5f), CornerRadius(cell * 0.05f))
            repeat(if (zone.level > 1) 3 else 1) { i -> drawRect(Aqua1, topLeft + Offset(cell * (0.3f + i * 0.15f), cell * 0.42f), Size(cell * 0.08f, cell * 0.12f)) }
        }
        ZoneType.INDUSTRIAL -> {
            drawRect(color, topLeft + Offset(cell * 0.18f, cell * 0.45f), Size(cell * 0.58f, cell * 0.32f))
            drawRect(GameNeutral, topLeft + Offset(cell * 0.6f, cell * 0.2f), Size(cell * 0.12f, cell * 0.32f))
            if (zone.level > 1) drawRect(GameNeutral, topLeft + Offset(cell * 0.35f, cell * 0.25f), Size(cell * 0.1f, cell * 0.25f))
        }
    }
}

private fun DrawScope.drawCivic(civic: CivicBuilding, topLeft: Offset, cell: Float, color: Color) {
    when (civic.type) {
        CivicType.FIRE_STATION -> {
            drawRoundRect(color, topLeft + Offset(cell * 0.22f, cell * 0.28f), Size(cell * 0.56f, cell * 0.5f), CornerRadius(cell * 0.06f))
            drawRect(GameEnemy, topLeft + Offset(cell * 0.35f, cell * 0.18f), Size(cell * 0.3f, cell * 0.18f))
        }
        CivicType.POLICE_STATION -> {
            drawCircle(color, radius = cell * 0.28f, center = topLeft + Offset(cell * 0.5f, cell * 0.5f))
            drawCircle(Aqua1, radius = cell * 0.12f, center = topLeft + Offset(cell * 0.5f, cell * 0.5f))
        }
        CivicType.PARK -> {
            drawCircle(GameSuccess, radius = cell * 0.25f * civic.level.toFloat(), center = topLeft + Offset(cell * 0.5f, cell * 0.42f))
            drawRect(GameHazard, topLeft + Offset(cell * 0.46f, cell * 0.52f), Size(cell * 0.08f, cell * 0.24f))
        }
    }
}

private fun DrawScope.drawJobOverlay(zone: ZoneBuilding, topLeft: Offset, cell: Float) {
    val color = if (zone.type == ZoneType.RESIDENTIAL) GamePlayer else if (zone.idle) GameNeutral else GameSuccess
    drawRoundRect(color.copy(alpha = 0.85f), topLeft + Offset(cell * 0.08f, cell * 0.08f), Size(cell * 0.22f, cell * 0.12f), CornerRadius(cell * 0.04f))
}

private fun colorFor(building: CityBuilding): Color = when (building) {
    is ZoneBuilding -> when (building.type) {
        ZoneType.RESIDENTIAL -> GamePlayer
        ZoneType.COMMERCIAL -> Aqua3
        ZoneType.INDUSTRIAL -> GameHazard
    }
    is CivicBuilding -> when (building.type) {
        CivicType.FIRE_STATION -> GameEnemy
        CivicType.POLICE_STATION -> Aqua4
        CivicType.PARK -> GameSuccess
    }
}

private const val DISASTER_BANNER_MILLIS = 3_000L
