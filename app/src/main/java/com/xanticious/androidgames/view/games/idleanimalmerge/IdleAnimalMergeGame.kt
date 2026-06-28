package com.xanticious.androidgames.view.games.idleanimalmerge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.xanticious.androidgames.controller.games.idleanimalmerge.IdleAnimalMergeController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.idleanimalmerge.Animal
import com.xanticious.androidgames.model.games.idleanimalmerge.AnimalMergeGameState
import com.xanticious.androidgames.model.games.idleanimalmerge.AnimalType
import com.xanticious.androidgames.state.games.idleanimalmerge.IdleAnimalMergePhase
import com.xanticious.androidgames.state.games.idleanimalmerge.IdleAnimalMergeStateMachine
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Idle Animal Merge — entry composable.
 *
 * A new animal arrives every hour (real-time); matching animals can be merged
 * into a higher-tier creature. The game is self-configured (no external
 * difficulty or settings screen needed).
 *
 * [difficulty] is accepted for registry compatibility but unused — the game
 * has no difficulty axis.
 */
@Composable
fun IdleAnimalMergeGame(
    @Suppress("UNUSED_PARAMETER") difficulty: GameDifficulty,
    onExit: () -> Unit
) {
    val controller = remember { IdleAnimalMergeController() }
    val machine = remember { IdleAnimalMergeStateMachine() }
    val phase by machine.phase.collectAsState()

    var gameState by remember { mutableStateOf(AnimalMergeGameState.initial()) }

    // ── Second ticker ────────────────────────────────────────────────────────
    // Unit key so the coroutine isn't restarted on phase transitions.
    val latestPhase by rememberUpdatedState(phase)
    val latestGameState by rememberUpdatedState(gameState)

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            val currentPhase = latestPhase
            if (currentPhase != IdleAnimalMergePhase.PLAYING && currentPhase != IdleAnimalMergePhase.ANIMAL_ARRIVED) continue
            val (updated, spawnType) = controller.tickAndSpawn(latestGameState, 1.0)
            gameState = updated
            if (spawnType != null) {
                val stateAfterSpawn = controller.placeAnimal(updated, spawnType)
                gameState = stateAfterSpawn
                if (controller.isAtCapacity(stateAfterSpawn)) {
                    machine.fieldCapacityReached()
                } else {
                    machine.hourlySpawn()
                }
            }
        }
    }

    GameScaffold(
        title = "Animal Merge",
        onExit = onExit,
        hud = {
            if (phase != IdleAnimalMergePhase.IDLE && phase != IdleAnimalMergePhase.HOW_TO_PLAY) {
                val mins = gameState.secondsUntilNextSpawn / 60
                val secs = gameState.secondsUntilNextSpawn % 60
                val timerText = String.format(Locale.US, "%02d:%02d", mins, secs)
                GameHud(
                    left = "🪙 ${gameState.coins}",
                    center = "Next: $timerText",
                    right = "📖 ${gameState.discoveredIds.size}/100"
                )
            }
        },
        status = {
            when (phase) {
                IdleAnimalMergePhase.IDLE -> {}
                IdleAnimalMergePhase.HOW_TO_PLAY -> {}
                IdleAnimalMergePhase.ANIMAL_ARRIVED -> AnimalArrivedBanner(gameState)
                IdleAnimalMergePhase.FIELD_FULL -> FieldFullBanner(
                    onRelease = { id ->
                        val prev = gameState
                        gameState = controller.releaseAnimal(prev, id)
                        if (!controller.isAtCapacity(gameState)) machine.spaceFreed()
                    }
                )
                IdleAnimalMergePhase.PLAYING -> PassiveIncomeStatus(gameState, controller)
            }
        },
        board = {
            when (phase) {
                IdleAnimalMergePhase.IDLE -> IdleStartScreen(
                    onStart = { machine.startGame() },
                    onHowToPlay = { machine.openHowToPlay() }
                )
                IdleAnimalMergePhase.HOW_TO_PLAY -> HowToPlayScreen(
                    onBack = { machine.dismissHowToPlay() }
                )
                IdleAnimalMergePhase.PLAYING,
                IdleAnimalMergePhase.ANIMAL_ARRIVED,
                IdleAnimalMergePhase.FIELD_FULL -> AnimalFieldBoard(
                    gameState = gameState,
                    controller = controller,
                    onTap = { instanceId ->
                        val (newState, mergeResult) = controller.tapAnimal(gameState, instanceId)
                        gameState = newState
                        if (mergeResult != null && phase == IdleAnimalMergePhase.ANIMAL_ARRIVED) {
                            machine.animalPlaced()
                        }
                    },
                    onPlaceArrival = { type ->
                        gameState = controller.placeAnimal(gameState, type)
                        machine.animalPlaced()
                    }
                )
            }
        }
    )
}

// ── Idle Start Screen ────────────────────────────────────────────────────────

@Composable
private fun IdleStartScreen(onStart: () -> Unit, onHowToPlay: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🐾", fontSize = 72.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Animal Merge",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Merge matching animals to discover\nhigher-tier creatures!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth(0.6f)) {
            Text("Start Playing")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onHowToPlay, modifier = Modifier.fillMaxWidth(0.6f)) {
            Text("How to Play")
        }
    }
}

// ── How to Play Screen ───────────────────────────────────────────────────────

@Composable
private fun HowToPlayScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("How to Play", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("🕐 A new animal arrives every hour. Watch the countdown timer at the top.")
        Text("🐾 Tap an animal to select it (highlighted in gold). Then tap another animal of the same type to merge them.")
        Text("⬆️ Merging two same-type animals produces one animal of the next tier (10 tiers, 100 animals total).")
        Text("🌾 Your field holds up to 20 animals. When full, arriving animals queue (up to 3). Merge or release to make space.")
        Text("🪙 Animals generate passive coins based on their tier² × 0.5 per minute. Higher tiers earn much more!")
        Text("💫 Merge bonus: earn tier × 50 coins per successful merge.")
        Text("📖 Discover all 100 animals in the Bestiary (shown in the top-right counter).")
        Text("🗑️ You can release an animal (swipe/long-press placeholder) to earn a small bonus and free its slot.")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onBack) { Text("← Back") }
    }
}

// ── Main Field Board ─────────────────────────────────────────────────────────

@Composable
private fun AnimalFieldBoard(
    gameState: AnimalMergeGameState,
    controller: IdleAnimalMergeController,
    onTap: (Int) -> Unit,
    onPlaceArrival: (AnimalType) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Pending arrival notice
        gameState.pendingArrival?.let { arrival ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = GameAccent.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${arrival.emoji} ${arrival.name} (Tier ${arrival.tier}) arrived!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { onPlaceArrival(arrival) },
                        colors = ButtonDefaults.buttonColors(containerColor = GameSuccess)
                    ) { Text("Place") }
                }
            }
        }

        // Animal grid (4 columns × 5 rows = 20 slots)
        val occupiedMap = gameState.field.associateBy { it.fieldSlot }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items((0 until AnimalMergeGameState.MAX_FIELD_SLOTS).toList()) { slot ->
                val animal = occupiedMap[slot]
                AnimalSlotTile(
                    animal = animal,
                    isSelected = animal != null && animal.instanceId == gameState.selectedInstanceId,
                    onTap = { animal?.let { onTap(it.instanceId) } }
                )
            }
        }

        // Queue strip
        if (gameState.queue.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Dark2.copy(alpha = 0.5f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Queue:", style = MaterialTheme.typography.labelMedium, color = Aqua1)
                gameState.queue.forEach { type ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(tierColor(type.tier).copy(alpha = 0.4f))
                            .border(1.dp, tierColor(type.tier), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(type.emoji, fontSize = 18.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimalSlotTile(
    animal: Animal?,
    isSelected: Boolean,
    onTap: () -> Unit
) {
    val borderColor = when {
        isSelected -> GameAccent
        animal != null -> tierColor(animal.type.tier).copy(alpha = 0.7f)
        else -> Dark2
    }
    val bgColor = when {
        isSelected -> GameAccent.copy(alpha = 0.2f)
        animal != null -> tierColor(animal.type.tier).copy(alpha = 0.15f)
        else -> Dark1.copy(alpha = 0.4f)
    }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = animal != null, onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        if (animal != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(animal.type.emoji, fontSize = 24.sp, textAlign = TextAlign.Center)
                Text(
                    text = "T${animal.type.tier}",
                    fontSize = 10.sp,
                    color = tierColor(animal.type.tier),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Status Panels ────────────────────────────────────────────────────────────

@Composable
private fun AnimalArrivedBanner(gameState: AnimalMergeGameState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GameSuccess.copy(alpha = 0.15f))
            .padding(12.dp)
    ) {
        Text(
            text = "✨ A new animal has arrived! Select matching animals to merge, or tap Place.",
            style = MaterialTheme.typography.bodySmall,
            color = GameSuccess,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FieldFullBanner(onRelease: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GameHazard.copy(alpha = 0.15f))
            .padding(12.dp)
    ) {
        Text(
            text = "⚠️ Field is full! Merge or release an animal to make space.",
            style = MaterialTheme.typography.bodySmall,
            color = GameHazard,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PassiveIncomeStatus(
    gameState: AnimalMergeGameState,
    controller: IdleAnimalMergeController
) {
    val income = controller.passiveIncomePerMinute(gameState)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "⚡ %.1f coins/min".format(income),
            style = MaterialTheme.typography.bodySmall,
            color = GameAccent
        )
        Text(
            text = "Animals: ${gameState.field.size}/${AnimalMergeGameState.MAX_FIELD_SLOTS}",
            style = MaterialTheme.typography.bodySmall,
            color = Aqua1
        )
    }
}

// ── Tier color helper ────────────────────────────────────────────────────────

private fun tierColor(tier: Int): Color = when (tier) {
    1 -> Color(0xFF9ECEFA)   // light blue
    2 -> Color(0xFF63C96A)   // green
    3 -> Color(0xFFFFD43B)   // yellow
    4 -> Color(0xFFFFA94D)   // orange
    5 -> Color(0xFFFF6B6B)   // red
    6 -> Color(0xFFCC5DE8)   // purple
    7 -> Color(0xFF339AF0)   // bright blue
    8 -> Color(0xFFFF922B)   // deep orange
    9 -> Color(0xFFDA77F2)   // lavender
    10 -> Color(0xFFFFD700)  // gold
    else -> Color.White
}
