package com.xanticious.androidgames.view.games.idleplayerpiano

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.idleplayerpiano.IdlePlayerPianoController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.idleplayerpiano.PianoGameState
import com.xanticious.androidgames.model.games.idleplayerpiano.PianoNote
import com.xanticious.androidgames.model.games.idleplayerpiano.PianoTickResult
import com.xanticious.androidgames.state.games.idleplayerpiano.IdlePlayerPianoPhase
import com.xanticious.androidgames.state.games.idleplayerpiano.IdlePlayerPianoStateMachine
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import kotlinx.coroutines.delay

/**
 * Idle Player Piano — entry composable.
 *
 * A player piano autonomously plays notes every tick; upgrades bias its note
 * selection toward a displayed target sequence. Full sequence matches earn
 * coins that fund more upgrades.
 *
 * [difficulty] is accepted for registry compatibility but unused — difficulty
 * is entirely governed by the upgrade progression.
 */
@Composable
fun IdlePlayerPianoGame(
    @Suppress("UNUSED_PARAMETER") difficulty: GameDifficulty,
    onExit: () -> Unit
) {
    val controller = remember { IdlePlayerPianoController() }
    val machine = remember { IdlePlayerPianoStateMachine() }
    val phase by machine.phase.collectAsState()

    var gameState by remember { mutableStateOf(controller.initialState()) }
    var lastPlayedNote by remember { mutableStateOf<PianoNote?>(null) }

    // ── Piano auto-play loop ─────────────────────────────────────────────────
    // Unit key keeps the coroutine alive across phase changes (e.g. SEQUENCE_MATCHED).
    // rememberUpdatedState gives the lambda access to the latest phase/state without restart.
    val latestPhase by rememberUpdatedState(phase)
    val latestGameState by rememberUpdatedState(gameState)

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)  // ~60 fps
            val currentPhase = latestPhase
            if (currentPhase != IdlePlayerPianoPhase.PLAYING &&
                currentPhase != IdlePlayerPianoPhase.SEQUENCE_MATCHED
            ) continue
            val (newState, result) = controller.tick(latestGameState, 0.016f)
            gameState = newState
            when (result) {
                is PianoTickResult.NotePlayed -> lastPlayedNote = result.note
                is PianoTickResult.SequenceMatched -> {
                    lastPlayedNote = null
                    if (latestPhase == IdlePlayerPianoPhase.PLAYING) {
                        machine.sequenceCompleted()
                        delay(1_500L)
                        gameState = controller.clearCelebration(gameState)
                        machine.celebrationDismissed()
                    }
                }
                PianoTickResult.Nothing -> {}
            }
        }
    }

    GameScaffold(
        title = "Player Piano",
        onExit = onExit,
        hud = {
            if (phase != IdlePlayerPianoPhase.IDLE && phase != IdlePlayerPianoPhase.HOW_TO_PLAY) {
                GameHud(
                    left = "🪙 ${gameState.coins}",
                    center = "Matches: ${gameState.matchesCompleted}",
                    right = "Seq: ${gameState.sequenceLength}"
                )
            }
        },
        status = {
            when (phase) {
                IdlePlayerPianoPhase.IDLE -> {}
                IdlePlayerPianoPhase.HOW_TO_PLAY -> {}
                IdlePlayerPianoPhase.SEQUENCE_MATCHED -> SequenceMatchedBanner()
                IdlePlayerPianoPhase.PLAYING, IdlePlayerPianoPhase.UPGRADE_MENU_OPEN ->
                    PianoStatusBar(
                        state = gameState,
                        controller = controller,
                        isUpgradeMenuOpen = phase == IdlePlayerPianoPhase.UPGRADE_MENU_OPEN,
                        onOpenUpgrades = { machine.openUpgradeMenu() },
                        onCloseUpgrades = { machine.closeUpgradeMenu() },
                        onPurchase = { id ->
                            gameState = controller.purchaseUpgrade(gameState, id)
                            machine.upgradePurchased()
                        }
                    )
            }
        },
        board = {
            when (phase) {
                IdlePlayerPianoPhase.IDLE -> PianoStartScreen(
                    onStart = { machine.startGame() },
                    onHowToPlay = { machine.openHowToPlay() }
                )
                IdlePlayerPianoPhase.HOW_TO_PLAY -> PianoHowToPlayScreen(
                    onBack = { machine.dismissHowToPlay() }
                )
                IdlePlayerPianoPhase.PLAYING,
                IdlePlayerPianoPhase.SEQUENCE_MATCHED,
                IdlePlayerPianoPhase.UPGRADE_MENU_OPEN -> PianoBoardPanel(
                    state = gameState,
                    lastPlayedNote = lastPlayedNote
                )
            }
        }
    )
}

// ── Start Screen ─────────────────────────────────────────────────────────────

@Composable
private fun PianoStartScreen(onStart: () -> Unit, onHowToPlay: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎹", fontSize = 72.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Player Piano",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Watch your piano learn to play\ntarget melodies — one upgrade at a time.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth(0.6f)) {
            Text("Start Piano")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onHowToPlay, modifier = Modifier.fillMaxWidth(0.6f)) {
            Text("How to Play")
        }
    }
}

// ── How to Play Screen ────────────────────────────────────────────────────────

@Composable
private fun PianoHowToPlayScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("How to Play", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("🎹 Your player piano plays one note per second automatically.")
        Text("🎵 A target sequence is shown above the keyboard (2–8 notes). The piano tries to match it note by note.")
        Text("✅ When the piano plays the correct next note, the matching position lights up green.")
        Text("🔄 Any wrong note resets progress on the current sequence.")
        Text("🏆 A full sequence match earns coins! Longer sequences earn much more.")
        Text("⬆️ Spend coins on upgrades to bias the piano toward playing the correct next note.")
        Text("💡 Maximum achievable bias is 90 % — the piano always keeps a random element.")
        Text("📈 After enough matches, the target sequence grows longer (2 → 3 → … → 8 notes).")
        Text("🎛️ Tap 'Upgrades' at the bottom to buy improvements.")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onBack) { Text("← Back") }
    }
}

// ── Piano Board ───────────────────────────────────────────────────────────────

@Composable
private fun PianoBoardPanel(
    state: PianoGameState,
    lastPlayedNote: PianoNote?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Target sequence display
        TargetSequenceRow(state)

        // Progress bar
        ProgressBar(state)

        // Piano keyboard
        PianoKeyboard(state, lastPlayedNote)

        // Note ticker
        NoteTicker(state.recentNotes)

        // Bias & speed info
        BiasInfoRow(state)
    }
}

@Composable
private fun TargetSequenceRow(state: PianoGameState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Target Sequence",
            style = MaterialTheme.typography.labelMedium,
            color = Aqua1,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            state.target.notes.forEachIndexed { index, note ->
                val isMatched = index < state.progressIndex
                val isCurrent = index == state.progressIndex
                NoteBox(
                    label = note.label,
                    bgColor = when {
                        isMatched -> GameSuccess.copy(alpha = 0.8f)
                        isCurrent -> Aqua2.copy(alpha = 0.5f)
                        else -> Dark2.copy(alpha = 0.7f)
                    },
                    textColor = if (isMatched) Color.White else Aqua1,
                    borderColor = when {
                        isMatched -> GameSuccess
                        isCurrent -> Aqua2
                        else -> Dark2
                    }
                )
            }
            // Blank placeholder cells to fill up to 8
            repeat(8 - state.target.notes.size) {
                NoteBox(label = "", bgColor = Dark0.copy(alpha = 0.3f), textColor = Color.Transparent, borderColor = Dark2.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
private fun NoteBox(label: String, bgColor: Color, textColor: Color, borderColor: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProgressBar(state: PianoGameState) {
    val progress = if (state.target.notes.isNotEmpty())
        state.progressIndex.toFloat() / state.target.notes.size.toFloat()
    else 0f
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Progress: ${state.progressIndex} / ${state.target.notes.size}",
            style = MaterialTheme.typography.labelSmall,
            color = Aqua1
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Dark2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(GamePlayer)
            )
        }
    }
}

@Composable
private fun PianoKeyboard(state: PianoGameState, lastPlayedNote: PianoNote?) {
    val nextTargetNote = state.target.notes.getOrNull(state.progressIndex)
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Piano Keys",
            style = MaterialTheme.typography.labelMedium,
            color = Aqua1,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (pitch in 0 until PianoGameState.NOTE_COUNT) {
                val note = PianoNote(pitch)
                val isPlaying = lastPlayedNote?.pitch == pitch
                val isTarget = nextTargetNote?.pitch == pitch
                val isMatched = isPlaying && isTarget
                val keyColor = when {
                    isMatched -> GameSuccess
                    isPlaying -> GameAccent
                    isTarget -> GamePlayer.copy(alpha = 0.6f)
                    else -> Color.White
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                        .background(keyColor)
                        .border(1.dp, Dark0, RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = note.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Dark0,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteTicker(recentNotes: List<PianoNote>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Dark1.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🎵", fontSize = 14.sp)
        val padded = List(PianoGameState.TICKER_SIZE) { i ->
            recentNotes.getOrNull(i)?.label ?: "·"
        }
        padded.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (label == "·") Dark2 else Aqua2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BiasInfoRow(state: PianoGameState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Bias: ${"%.0f".format(state.bias * 100)} %",
            style = MaterialTheme.typography.bodySmall,
            color = GameAccent
        )
        Text(
            text = "Speed: ${"%.2f".format(1f / state.tickIntervalSeconds)} notes/s",
            style = MaterialTheme.typography.bodySmall,
            color = Aqua2
        )
    }
}

// ── Status Panels ─────────────────────────────────────────────────────────────

@Composable
private fun SequenceMatchedBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GameSuccess.copy(alpha = 0.2f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🎉 Sequence matched! Coins earned!",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = GameSuccess,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PianoStatusBar(
    state: PianoGameState,
    controller: IdlePlayerPianoController,
    isUpgradeMenuOpen: Boolean,
    onOpenUpgrades: () -> Unit,
    onCloseUpgrades: () -> Unit,
    onPurchase: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (isUpgradeMenuOpen) {
            UpgradePanel(
                state = state,
                controller = controller,
                onPurchase = onPurchase,
                onClose = onCloseUpgrades
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total notes: ${state.totalNotesPlayed}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Aqua1
                )
                Button(
                    onClick = onOpenUpgrades,
                    colors = ButtonDefaults.buttonColors(containerColor = Aqua3)
                ) {
                    Text("🔧 Upgrades")
                }
            }
        }
    }
}

@Composable
private fun UpgradePanel(
    state: PianoGameState,
    controller: IdlePlayerPianoController,
    onPurchase: (String) -> Unit,
    onClose: () -> Unit
) {
    val availableIds = controller.availableUpgradeIds(state.upgrades)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Dark1)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Upgrades", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onClose) { Text("Close") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            state.upgrades.forEach { upgrade ->
                val isPurchased = upgrade.purchased
                val isAvailable = upgrade.id in availableIds
                val canAfford = state.coins >= upgrade.cost
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = upgrade.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isPurchased -> GameSuccess
                                isAvailable -> Aqua2
                                else -> Color.Gray
                            }
                        )
                        Text(
                            text = upgrade.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                    when {
                        isPurchased -> Text("✅", fontSize = 18.sp)
                        isAvailable -> Button(
                            onClick = { onPurchase(upgrade.id) },
                            enabled = canAfford,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canAfford) Aqua2 else Dark2
                            )
                        ) { Text("🪙 ${upgrade.cost}") }
                        else -> Text(
                            text = "🔒",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
                HorizontalDivider(color = Dark2, thickness = 0.5.dp)
            }
        }
    }
}
