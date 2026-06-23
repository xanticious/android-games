package com.xanticious.androidgames.view.games.planetexplorer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.planetexplorer.PlanetExplorerController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.planetexplorer.Biome
import com.xanticious.androidgames.model.games.planetexplorer.BiomeWorld
import com.xanticious.androidgames.model.games.planetexplorer.Discoverable
import com.xanticious.androidgames.model.games.planetexplorer.ControlLayout
import com.xanticious.androidgames.model.games.planetexplorer.DiscoverableKind
import com.xanticious.androidgames.model.games.planetexplorer.MovementAbility
import com.xanticious.androidgames.model.games.planetexplorer.PlanetExplorerGameState
import com.xanticious.androidgames.model.games.planetexplorer.PlanetExplorerInput
import com.xanticious.androidgames.model.games.planetexplorer.PlanetExplorerSettings
import com.xanticious.androidgames.model.games.planetexplorer.TerrainDetail
import com.xanticious.androidgames.state.games.planetexplorer.PlanetExplorerPhase
import com.xanticious.androidgames.state.games.planetexplorer.PlanetExplorerStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import kotlin.math.max

private enum class SetupScreen { SETTINGS, HOW_TO_PLAY, GAME }

@Composable
fun PlanetExplorerGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { PlanetExplorerController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { PlanetExplorerStateMachine() }
    val phase by machine.phase.collectAsState()
    val initialSeed = rememberSaveable { "planet-${(1000..9999).random()}" }
    var worldSeed by rememberSaveable { mutableStateOf(initialSeed) }
    var terrainDetailName by rememberSaveable { mutableStateOf(TerrainDetail.MEDIUM.name) }
    var showDiscoveryHints by rememberSaveable { mutableStateOf(true) }
    var showToolHint by rememberSaveable { mutableStateOf(true) }
    val settings = PlanetExplorerSettings(
        worldSeed = worldSeed,
        terrainDetail = TerrainDetail.valueOf(terrainDetailName),
        showDiscoveryHints = showDiscoveryHints,
        showToolHint = showToolHint,
        controlLayout = ControlLayout.D_PAD
    )
    var setupScreen by rememberSaveable { mutableStateOf(SetupScreen.SETTINGS) }
    var state by remember(settings.worldSeed, difficulty) { mutableStateOf(controller.initialState(config, settings.worldSeed)) }
    var walkDirection by remember { mutableFloatStateOf(0f) }
    var jumpHeld by remember { mutableStateOf(false) }
    var jumpPressed by remember { mutableStateOf(false) }
    var lastDiscovery by remember { mutableStateOf<String?>(null) }
    var pendingAbility by remember { mutableStateOf<MovementAbility?>(null) }

    LaunchedEffect(setupScreen) {
        if (setupScreen == SetupScreen.GAME && phase == PlanetExplorerPhase.IDLE) {
            machine.startGame()
            machine.worldReady()
        }
    }

    GameLoop(running = phase == PlanetExplorerPhase.EXPLORING && setupScreen == SetupScreen.GAME) { dt ->
        val result = controller.step(
            state = state,
            config = config,
            dt = dt,
            input = PlanetExplorerInput(
                directionX = walkDirection,
                jumpPressed = jumpPressed,
                jumpHeld = jumpHeld,
                climbDirectionY = 0f,
                useTool = false
            )
        )
        jumpPressed = false
        state = result.state
        result.discoveries.firstOrNull()?.let { lastDiscovery = "+ ${it.discoverable.name}" }
        result.unlockedAbility?.let { ability ->
            pendingAbility = ability
            machine.toolFound()
        }
    }

    when (setupScreen) {
        SetupScreen.SETTINGS -> SetupScaffold(onExit = onExit, title = "Planet Explorer Settings") {
            SettingsContent(
                settings = settings,
                onSettingsChanged = { next ->
                    worldSeed = next.worldSeed
                    terrainDetailName = next.terrainDetail.name
                    showDiscoveryHints = next.showDiscoveryHints
                    showToolHint = next.showToolHint
                },
                onContinue = { setupScreen = SetupScreen.HOW_TO_PLAY }
            )
        }
        SetupScreen.HOW_TO_PLAY -> SetupScaffold(onExit = onExit, title = "How to Play") {
            HowToPlayContent(onBack = { setupScreen = SetupScreen.SETTINGS }, onStart = { setupScreen = SetupScreen.GAME })
        }
        SetupScreen.GAME -> ExplorerScaffold(
            state = state,
            phase = phase,
            settings = settings,
            lastDiscovery = lastDiscovery,
            pendingAbility = pendingAbility,
            onExit = onExit,
            onOpenBook = { machine.openFieldBook() },
            onOpenBiomes = { machine.openBiomeSwitcher() },
            onDismiss = {
                pendingAbility = null
                machine.dismiss()
            },
            onCloseBook = { machine.closeFieldBook() },
            onBiomeSelected = { biome ->
                state = controller.switchBiome(state, biome)
                machine.selectBiome()
            },
            onWalk = { walkDirection = it },
            onJump = {
                jumpPressed = true
                jumpHeld = true
            },
            onJumpRelease = { jumpHeld = false },
            onSelectAbility = { ability -> state = controller.selectAbility(state, ability) }
        )
    }
}

@Composable
private fun SetupScaffold(onExit: () -> Unit, title: String, content: @Composable () -> Unit) {
    GameScaffold(title = title, onExit = onExit, status = {}, hud = {}) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsContent(
    settings: PlanetExplorerSettings,
    onSettingsChanged: (PlanetExplorerSettings) -> Unit,
    onContinue: () -> Unit
) {
    OutlinedTextField(
        value = settings.worldSeed,
        onValueChange = { seed -> onSettingsChanged(settings.copy(worldSeed = seed.ifBlank { "planet" })) },
        label = { Text("World seed") },
        singleLine = true
    )
    Text("Terrain detail")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TerrainDetail.entries.forEach { detail ->
            val selected = settings.terrainDetail == detail
            if (selected) Button(onClick = {}) { Text(detail.name.lowercase().replaceFirstChar { it.titlecase() }) }
            else OutlinedButton(onClick = { onSettingsChanged(settings.copy(terrainDetail = detail)) }) {
                Text(detail.name.lowercase().replaceFirstChar { it.titlecase() })
            }
        }
    }
    SettingSwitch("Show discovery hints", settings.showDiscoveryHints) {
        onSettingsChanged(settings.copy(showDiscoveryHints = it))
    }
    SettingSwitch("Show tool hint", settings.showToolHint) {
        onSettingsChanged(settings.copy(showToolHint = it))
    }
    Button(onClick = onContinue) { Text("Continue") }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

@Composable
private fun HowToPlayContent(onBack: () -> Unit, onStart: () -> Unit) {
    Text("Walk left and right to explore each biome.")
    Text("Jump to reach ledges. Hidden tools unlock movement for every adventurer.")
    Text("Touch animals and use tools near minerals to fill the Field Book.")
    Text("Open the biome switcher any time to change adventurer.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Button(onClick = onStart) { Text("Start Exploring") }
    }
}

@Composable
private fun ExplorerScaffold(
    state: PlanetExplorerGameState,
    phase: PlanetExplorerPhase,
    settings: PlanetExplorerSettings,
    lastDiscovery: String?,
    pendingAbility: MovementAbility?,
    onExit: () -> Unit,
    onOpenBook: () -> Unit,
    onOpenBiomes: () -> Unit,
    onDismiss: () -> Unit,
    onCloseBook: () -> Unit,
    onBiomeSelected: (Biome) -> Unit,
    onWalk: (Float) -> Unit,
    onJump: () -> Unit,
    onJumpRelease: () -> Unit,
    onSelectAbility: (MovementAbility) -> Unit
) {
    GameScaffold(
        title = "Planet Explorer",
        onExit = onExit,
        hud = {
            GameHud(
                left = "${state.adventurer.biome.displayName} · ${state.adventurer.biome.adventurerName}",
                center = "📖 ${state.fieldBook.discoveredCount}/${state.fieldBook.totalCount}",
                right = "Tools ${state.stats.unlockedAbilities.size}/${MovementAbility.entries.size}"
            )
        },
        status = {
            StatusPanel(
                state = state,
                phase = phase,
                lastDiscovery = lastDiscovery,
                pendingAbility = pendingAbility,
                onOpenBook = onOpenBook,
                onOpenBiomes = onOpenBiomes,
                onDismiss = onDismiss,
                onCloseBook = onCloseBook,
                onBiomeSelected = onBiomeSelected,
                onSelectAbility = onSelectAbility,
                onWalk = onWalk,
                onJump = onJump,
                onJumpRelease = onJumpRelease
            )
        }
    ) {
        PlanetExplorerCanvas(state = state, settings = settings)
    }
}

@Composable
private fun StatusPanel(
    state: PlanetExplorerGameState,
    phase: PlanetExplorerPhase,
    lastDiscovery: String?,
    pendingAbility: MovementAbility?,
    onOpenBook: () -> Unit,
    onOpenBiomes: () -> Unit,
    onDismiss: () -> Unit,
    onCloseBook: () -> Unit,
    onBiomeSelected: (Biome) -> Unit,
    onSelectAbility: (MovementAbility) -> Unit,
    onWalk: (Float) -> Unit,
    onJump: () -> Unit,
    onJumpRelease: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lastDiscovery?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        when (phase) {
            PlanetExplorerPhase.FIELD_BOOK -> FieldBookPanel(state = state, onClose = onCloseBook)
            PlanetExplorerPhase.BIOME_SELECT -> BiomeSwitcherPanel(state = state, onBiomeSelected = onBiomeSelected, onClose = onDismiss)
            PlanetExplorerPhase.DISCOVERY -> ToolDiscoveryPanel(ability = pendingAbility, onDismiss = onDismiss)
            else -> ControlPanel(state = state, onOpenBook = onOpenBook, onOpenBiomes = onOpenBiomes, onSelectAbility = onSelectAbility, onWalk = onWalk, onJump = onJump, onJumpRelease = onJumpRelease)
        }
    }
}

@Composable
private fun ControlPanel(
    state: PlanetExplorerGameState,
    onOpenBook: () -> Unit,
    onOpenBiomes: () -> Unit,
    onSelectAbility: (MovementAbility) -> Unit,
    onWalk: (Float) -> Unit,
    onJump: () -> Unit,
    onJumpRelease: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        HoldButton(label = "← Walk", onDown = { onWalk(-1f) }, onUp = { onWalk(0f) })
        HoldButton(label = "Jump", onDown = onJump, onUp = onJumpRelease)
        HoldButton(label = "Walk →", onDown = { onWalk(1f) }, onUp = { onWalk(0f) })
        OutlinedButton(onClick = onOpenBook) { Text("📖") }
        OutlinedButton(onClick = onOpenBiomes) { Text("🌿") }
    }
    if (state.stats.unlockedAbilities.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            state.stats.unlockedAbilities.forEach { ability ->
                TextButton(onClick = { onSelectAbility(ability) }) {
                    Text(if (state.selectedAbility == ability) "✓ ${ability.displayName}" else ability.displayName)
                }
            }
        }
    }
}

@Composable
private fun HoldButton(label: String, onDown: () -> Unit, onUp: () -> Unit) {
    Box(
        modifier = Modifier
            .border(1.dp, GameCourtLine, RoundedCornerShape(24.dp))
            .background(GamePlayer.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
            .pointerInput(label) {
                detectTapGestures(
                    onPress = { _ ->
                        onDown()
                        try {
                            tryAwaitRelease()
                        } finally {
                            onUp()
                        }
                    }
                )
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FieldBookPanel(state: PlanetExplorerGameState, onClose: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Field Book", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClose) { Text("Close") }
            }
            state.fieldBook.byBiome(state.adventurer.biome).forEach { entry ->
                Text(if (entry.discovered) "✓ ${entry.discoverable.name}: ${entry.discoverable.description}" else "○ ???")
            }
        }
    }
}

@Composable
private fun BiomeSwitcherPanel(state: PlanetExplorerGameState, onBiomeSelected: (Biome) -> Unit, onClose: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Switch Adventurer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClose) { Text("Close") }
            }
            Biome.entries.forEach { biome ->
                val visited = biome in state.stats.visitedBiomes
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onBiomeSelected(biome) },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(if (visited) "✅ ${biome.displayName}" else "□ ${biome.displayName}")
                    Text(biome.adventurerName)
                }
            }
        }
    }
}

@Composable
private fun ToolDiscoveryPanel(ability: MovementAbility?, onDismiss: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().border(2.dp, GameAccent, RoundedCornerShape(12.dp))) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("You found the ${ability?.displayName ?: "tool"}!", fontWeight = FontWeight.Bold)
            Text("Every adventurer can use it now.")
            Button(onClick = onDismiss) { Text("Keep Exploring") }
        }
    }
}

@Composable
private fun PlanetExplorerCanvas(state: PlanetExplorerGameState, settings: PlanetExplorerSettings) {
    Canvas(modifier = Modifier.fillMaxSize().background(Dark0)) {
        val biomeWorld = state.world.biomeWorld(state.adventurer.biome)
        val tile = max(8f, size.height / 20f)
        val cameraX = (state.adventurer.position.x * tile - size.width * 0.42f).coerceIn(0f, max(0f, biomeWorld.width * tile - size.width))
        drawBackground(state.adventurer.biome, settings.terrainDetail)
        drawTerrain(biomeWorld, tile, cameraX)
        biomeWorld.discoverables.forEach { discoverable ->
            val discovered = state.fieldBook.isDiscovered(discoverable.id)
            drawDiscoverable(discoverable, tile, cameraX, discovered, settings.showDiscoveryHints)
        }
        if (biomeWorld.ability !in state.stats.unlockedAbilities) {
            drawTool(biomeWorld.toolPosition, tile, cameraX, settings.showToolHint)
        }
        drawAdventurer(state.adventurer.biome, state.adventurer.position, tile, cameraX)
    }
}

private fun DrawScope.drawBackground(biome: Biome, detail: TerrainDetail) {
    val sky = when (biome) {
        Biome.CAVERN -> Dark1
        Biome.REEF -> Aqua4
        Biome.SNOWFIELD -> Aqua0
        else -> Dark2
    }
    drawRect(sky, size = size)
    val layers = when (detail) {
        TerrainDetail.LOW -> 1
        TerrainDetail.MEDIUM -> 2
        TerrainDetail.HIGH -> 3
    }
    repeat(layers) { layer ->
        val y = size.height * (0.2f + layer * 0.14f)
        drawRect(Aqua3.copy(alpha = 0.08f + layer * 0.04f), topLeft = Offset(0f, y), size = Size(size.width, size.height * 0.08f))
    }
}

private fun DrawScope.drawTerrain(biomeWorld: BiomeWorld, tile: Float, cameraX: Float) {
    val top = when (biomeWorld.biome) {
        Biome.GRASSLAND, Biome.JUNGLE -> GameSuccess
        Biome.REEF -> Aqua1
        Biome.SNOWFIELD, Biome.HIGHLAND -> Aqua0
        Biome.DESERT -> GameAccent
        Biome.CAVERN -> GameNeutral
    }
    val body = when (biomeWorld.biome) {
        Biome.CAVERN -> Dark0
        Biome.REEF -> Aqua3
        else -> Dark2
    }
    biomeWorld.terrain.forEach { column ->
        val x = column.x * tile - cameraX
        if (x > -tile && x < size.width + tile) {
            val ground = column.groundY * tile
            drawRect(body, topLeft = Offset(x, ground), size = Size(tile, size.height - ground))
            drawRect(top, topLeft = Offset(x, ground), size = Size(tile, tile * 0.28f))
        }
    }
}

private fun DrawScope.drawDiscoverable(discoverable: Discoverable, tile: Float, cameraX: Float, discovered: Boolean, hints: Boolean) {
    val center = Offset(discoverable.position.x * tile - cameraX, discoverable.position.y * tile)
    if (center.x < -tile || center.x > size.width + tile) return
    val color = if (discovered) GameCourtLine else if (discoverable.kind == DiscoverableKind.ANIMAL) GameEnemy else GameNeutral
    if (!discovered && hints) {
        drawCircle(GameAccent.copy(alpha = 0.22f), radius = tile * 0.7f, center = center)
    }
    if (discoverable.kind == DiscoverableKind.ANIMAL) {
        drawOval(color, topLeft = center - Offset(tile * 0.45f, tile * 0.25f), size = Size(tile * 0.9f, tile * 0.5f))
        drawCircle(color, radius = tile * 0.18f, center = center + Offset(tile * 0.42f, -tile * 0.12f))
    } else {
        drawRoundRect(color, topLeft = center - Offset(tile * 0.28f, tile * 0.28f), size = Size(tile * 0.56f, tile * 0.56f), cornerRadius = CornerRadius(tile * 0.12f))
    }
}

private fun DrawScope.drawTool(position: Vec2, tile: Float, cameraX: Float, showHint: Boolean) {
    val center = Offset(position.x * tile - cameraX, position.y * tile)
    if (showHint) drawCircle(GameAccent.copy(alpha = 0.28f), radius = tile * 0.85f, center = center)
    drawCircle(GameAccent, radius = tile * 0.26f, center = center)
    drawCircle(Aqua0, radius = tile * 0.12f, center = center, style = Stroke(width = tile * 0.06f))
}

private fun DrawScope.drawAdventurer(biome: Biome, position: Vec2, tile: Float, cameraX: Float) {
    val foot = Offset(position.x * tile - cameraX, position.y * tile)
    val bodyColor = when (biome) {
        Biome.GRASSLAND, Biome.DESERT -> GameAccent
        Biome.REEF -> Aqua2
        Biome.SNOWFIELD, Biome.HIGHLAND -> Aqua0
        Biome.CAVERN -> GameNeutral
        Biome.JUNGLE -> GameSuccess
    }
    drawCircle(GamePlayer, radius = tile * 0.32f, center = foot + Offset(0f, -tile * 1.15f))
    drawRoundRect(bodyColor, topLeft = foot + Offset(-tile * 0.32f, -tile * 1.0f), size = Size(tile * 0.64f, tile * 0.9f), cornerRadius = CornerRadius(tile * 0.16f))
    drawLine(Dark0, foot + Offset(-tile * 0.2f, -tile * 0.1f), foot + Offset(-tile * 0.38f, tile * 0.32f), strokeWidth = tile * 0.08f)
    drawLine(Dark0, foot + Offset(tile * 0.2f, -tile * 0.1f), foot + Offset(tile * 0.38f, tile * 0.32f), strokeWidth = tile * 0.08f)
}

