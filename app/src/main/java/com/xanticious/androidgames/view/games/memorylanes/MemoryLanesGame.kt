package com.xanticious.androidgames.view.games.memorylanes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.memorylanes.MemoryLanesController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.memorylanes.MemoryLanesGameState
import com.xanticious.androidgames.model.games.memorylanes.MemoryLanesSettings
import com.xanticious.androidgames.model.games.memorylanes.MemoryTile
import com.xanticious.androidgames.model.games.memorylanes.MemoryTileCount
import com.xanticious.androidgames.model.games.memorylanes.MemoryTileLabelStyle
import com.xanticious.androidgames.state.games.memorylanes.MemoryLanesPhase
import com.xanticious.androidgames.state.games.memorylanes.MemoryLanesStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import kotlinx.coroutines.delay
import kotlin.random.Random

private enum class MemorySetupScreen { SETTINGS, HOW_TO_PLAY, GAME }

@Composable
fun MemoryLanesGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { MemoryLanesController() }
    val defaultConfig = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { MemoryLanesStateMachine() }
    val phase by machine.phase.collectAsState()
    val random = remember { Random(System.currentTimeMillis()) }

    var setupScreen by rememberSaveable { mutableStateOf(MemorySetupScreen.SETTINGS) }
    var bankSize by rememberSaveable { mutableStateOf(defaultConfig.bankSize) }
    var revealDuration by rememberSaveable { mutableStateOf(defaultConfig.revealDurationSeconds) }
    var labelStyle by rememberSaveable { mutableStateOf(defaultConfig.labelStyle) }
    var confirmWrong by rememberSaveable { mutableStateOf(defaultConfig.confirmWrong) }
    var longestSequence by rememberSaveable { mutableStateOf(0) }
    var gameState by remember { mutableStateOf<MemoryLanesGameState?>(null) }

    val settings = MemoryLanesSettings(bankSize, revealDuration, labelStyle, confirmWrong)
    val config = remember(settings) { controller.configFor(settings) }

    fun startGame() {
        gameState = controller.startState(config, random)
        setupScreen = MemorySetupScreen.GAME
        if (phase == MemoryLanesPhase.GAME_OVER) machine.playAgain() else machine.startGame()
    }

    LaunchedEffect(phase, config.revealDurationSeconds, gameState?.round) {
        if (phase == MemoryLanesPhase.REVEALING) {
            delay(config.revealDurationSeconds * 1000L)
            machine.revealComplete()
        }
    }

    LaunchedEffect(phase, gameState?.builtSequence) {
        if (phase == MemoryLanesPhase.VALIDATING) {
            val state = gameState ?: return@LaunchedEffect
            val validation = controller.validate(state)
            gameState = state.copy(validation = validation)
            if (validation.correct) {
                longestSequence = maxOf(longestSequence, state.sequence.size)
                delay(500L)
                gameState = controller.advanceRound(state, config, random)
                machine.sequenceCorrect()
            } else {
                longestSequence = maxOf(longestSequence, (state.sequence.size - 1).coerceAtLeast(0))
                if (config.confirmWrong) delay(1000L)
                machine.sequenceWrong()
            }
        }
    }

    when (setupScreen) {
        MemorySetupScreen.SETTINGS -> MemoryLanesSettingsScreen(
            bankSize = bankSize,
            revealDuration = revealDuration,
            labelStyle = labelStyle,
            confirmWrong = confirmWrong,
            onBankSize = { bankSize = it },
            onRevealDuration = { revealDuration = it },
            onLabelStyle = { labelStyle = it },
            onConfirmWrong = { confirmWrong = it },
            onHowToPlay = { setupScreen = MemorySetupScreen.HOW_TO_PLAY },
            onStart = ::startGame,
            onExit = onExit
        )
        MemorySetupScreen.HOW_TO_PLAY -> MemoryLanesHowToPlay(onBack = { setupScreen = MemorySetupScreen.SETTINGS }, onStart = ::startGame, onExit = onExit)
        MemorySetupScreen.GAME -> MemoryLanesBoard(
            state = gameState,
            phase = phase,
            bankSize = config.bankSize,
            labelStyle = config.labelStyle,
            longestSequence = longestSequence,
            remainingCounts = { state -> controller.remainingCounts(state.sequence, state.builtSequence, config.bankSize) },
            labelFor = { tile -> controller.labelFor(tile, config.labelStyle) },
            onTile = { tile ->
                val state = gameState
                if (state != null) {
                    gameState = controller.addTile(state, tile, config)
                    machine.tileAdded()
                }
            },
            onUndo = {
                val state = gameState
                if (state != null) {
                    gameState = controller.undoTile(state)
                    machine.tileUndo()
                }
            },
            canSubmit = { state -> controller.canSubmit(state) },
            onDone = { machine.doneSubmitted() },
            onPlayAgain = ::startGame,
            onExit = onExit
        )
    }
}

@Composable
private fun MemoryLanesSettingsScreen(
    bankSize: Int,
    revealDuration: Int,
    labelStyle: MemoryTileLabelStyle,
    confirmWrong: Boolean,
    onBankSize: (Int) -> Unit,
    onRevealDuration: (Int) -> Unit,
    onLabelStyle: (MemoryTileLabelStyle) -> Unit,
    onConfirmWrong: (Boolean) -> Unit,
    onHowToPlay: () -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit
) {
    GameScaffold(title = "Memory Lanes", onExit = onExit) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            MemoryChoiceRow("Bank size", (2..10).toList(), bankSize, onBankSize) { it.toString() }
            MemoryChoiceRow("Reveal", listOf(1, 2, 3), revealDuration, onRevealDuration) { "${it}s" }
            MemoryChoiceRow("Labels", MemoryTileLabelStyle.entries.toList(), labelStyle, onLabelStyle) { labelName(it) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Confirm wrong tile", fontWeight = FontWeight.Bold)
                Switch(checked = confirmWrong, onCheckedChange = onConfirmWrong)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onHowToPlay) { Text("How to Play") }
                Button(onClick = onStart) { Text("Start") }
            }
        }
    }
}

@Composable
private fun MemoryLanesHowToPlay(onBack: () -> Unit, onStart: () -> Unit, onExit: () -> Unit) {
    GameScaffold(title = "Memory Lanes", onExit = onExit) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("How to Play", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Each round reveals one new tile. Remember where it belongs in the growing sequence.")
            Text("The tile bank tells you which tiles and counts are in the sequence. Tap tiles to arrange the order.")
            Text("Undo fixes the lane before you commit. Done checks the full order; one mistake ends the game.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack) { Text("Settings") }
                Button(onClick = onStart) { Text("Start") }
            }
        }
    }
}

@Composable
private fun MemoryLanesBoard(
    state: MemoryLanesGameState?,
    phase: MemoryLanesPhase,
    bankSize: Int,
    labelStyle: MemoryTileLabelStyle,
    longestSequence: Int,
    remainingCounts: (MemoryLanesGameState) -> List<MemoryTileCount>,
    labelFor: (MemoryTile) -> String,
    onTile: (MemoryTile) -> Unit,
    onUndo: () -> Unit,
    canSubmit: (MemoryLanesGameState) -> Boolean,
    onDone: () -> Unit,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    GameScaffold(
        title = "Memory Lanes",
        onExit = onExit,
        hud = { GameHud(left = "Memory Lanes", center = "Round ${state?.round ?: 1}", right = "Best $longestSequence") },
        status = {
            val s = state
            if (phase == MemoryLanesPhase.GAME_OVER && s != null) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    DefeatPanel(
                        score = (s.round - 1).coerceAtLeast(0),
                        bestScore = longestSequence,
                        onTryAgain = onPlayAgain,
                        onMenu = onExit,
                        headline = "Sequence ended"
                    )
                    if (!s.validation.correct) Text("First wrong slot ${s.validation.firstWrongIndex + 1}: expected ${labelFor(s.validation.expectedTile)}")
                }
            }
        }
    ) {
        val s = state ?: return@GameScaffold
        Surface(modifier = Modifier.fillMaxSize(), color = Dark0) {
            Column(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RevealZone(phase = phase, tile = s.currentTile, label = labelFor(s.currentTile), color = tileColor(s.currentTile.id))
                SequenceLane(sequence = s.sequence, built = s.builtSequence, labelFor = labelFor)
                Text("Tile bank", color = Aqua1, fontWeight = FontWeight.Bold)
                TileBank(
                    counts = remainingCounts(s),
                    interactive = phase == MemoryLanesPhase.BUILDING,
                    labelFor = labelFor,
                    onTile = onTile
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(enabled = phase == MemoryLanesPhase.BUILDING && s.builtSequence.isNotEmpty(), onClick = onUndo) { Text("Undo") }
                    Button(enabled = phase == MemoryLanesPhase.BUILDING && canSubmit(s), onClick = onDone) { Text("Done") }
                }
                Text("Bank size $bankSize • ${labelName(labelStyle)}", color = Aqua0, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun RevealZone(phase: MemoryLanesPhase, tile: MemoryTile, label: String, color: Color) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        if (phase == MemoryLanesPhase.REVEALING) {
            MemoryTileSurface(tile = tile, label = label, color = color, sizeDp = 120, enabled = false, onClick = {})
        } else {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = RoundedCornerShape(18.dp),
                color = Dark1,
                border = BorderStroke(2.dp, Dark2)
            ) { Box(contentAlignment = Alignment.Center) { Text("Remember", color = Aqua0) } }
        }
    }
}

@Composable
private fun SequenceLane(sequence: List<MemoryTile>, built: List<MemoryTile>, labelFor: (MemoryTile) -> String) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sequence.indices.forEach { index ->
            val tile = built.getOrNull(index)
            if (tile == null) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Dark1,
                    border = BorderStroke(1.dp, Dark2)
                ) { Box(contentAlignment = Alignment.Center) { Text((index + 1).toString(), color = Aqua1) } }
            } else {
                MemoryTileSurface(tile = tile, label = labelFor(tile), color = tileColor(tile.id), sizeDp = 52, enabled = false, onClick = {})
            }
        }
    }
}

@Composable
private fun TileBank(counts: List<MemoryTileCount>, interactive: Boolean, labelFor: (MemoryTile) -> String, onTile: (MemoryTile) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = if (maxWidth > 650.dp && counts.size > 5) 5 else counts.size
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            counts.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { count ->
                        val enabled = interactive && count.remaining > 0
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(if (count.remaining == 0) 0.45f else 1f)) {
                            MemoryTileSurface(
                                tile = count.tile,
                                label = labelFor(count.tile),
                                color = tileColor(count.tile.id),
                                sizeDp = 60,
                                enabled = enabled,
                                onClick = { onTile(count.tile) }
                            )
                            Text("×${count.remaining}", color = Aqua0, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryTileSurface(tile: MemoryTile, label: String, color: Color, sizeDp: Int, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(sizeDp.dp).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = color,
        border = BorderStroke(2.dp, Aqua0)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = Dark0,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
private fun <T> MemoryChoiceRow(label: String, options: List<T>, selected: T, onSelected: (T) -> Unit, text: (T) -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            options.forEach { option ->
                if (option == selected) {
                    Button(onClick = { onSelected(option) }) { Text(text(option)) }
                } else {
                    OutlinedButton(onClick = { onSelected(option) }) { Text(text(option)) }
                }
            }
        }
    }
}

private fun tileColor(id: Int): Color {
    val colors = listOf(Aqua2, Aqua3, Aqua4, GameAccent, GamePlayer, GameSuccess, GameEnemy, Aqua1, Dark2, Aqua0)
    return colors[(id - 1).coerceAtLeast(0) % colors.size]
}

private fun labelName(style: MemoryTileLabelStyle): String = when (style) {
    MemoryTileLabelStyle.NUMBERS -> "Numbers"
    MemoryTileLabelStyle.COLORS_ONLY -> "Colors only"
    MemoryTileLabelStyle.LETTERS -> "Letters"
}
