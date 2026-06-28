package com.xanticious.androidgames.view.games.idlecombattraining

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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.idlecombattraining.IdleCombatTrainingController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.idlecombattraining.CombatStepEvent
import com.xanticious.androidgames.model.games.idlecombattraining.CombatUpgrade
import com.xanticious.androidgames.model.games.idlecombattraining.Dummy
import com.xanticious.androidgames.model.games.idlecombattraining.IdleCombatState
import com.xanticious.androidgames.model.games.idlecombattraining.LastMoveResult
import com.xanticious.androidgames.model.games.idlecombattraining.Move
import com.xanticious.androidgames.model.games.idlecombattraining.MoveId
import com.xanticious.androidgames.state.games.idlecombattraining.IdleCombatPhase
import com.xanticious.androidgames.state.games.idlecombattraining.IdleCombatTrainingStateMachine
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
import kotlinx.coroutines.delay

@Composable
fun IdleCombatTrainingGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { IdleCombatTrainingController() }
    val machine = remember { IdleCombatTrainingStateMachine() }
    val phase by machine.phase.collectAsState()
    var state by rememberSaveable(stateSaver = IdleCombatStateSaver) {
        mutableStateOf(IdleCombatState.initial())
    }
    var lastReward by rememberSaveable { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        machine.startGame()
    }

    LaunchedEffect(phase) {
        if (phase == IdleCombatPhase.DUMMY_DESTROYED) {
            delay(1_500L)
            machine.nextDummy()
        }
    }

    GameLoop(running = phase == IdleCombatPhase.TRAINING) { dt ->
        val step = controller.step(state, dt)
        state = step.state
        if (step.event == CombatStepEvent.DUMMY_DEFEATED) {
            lastReward = step.coinsEarned
            machine.dummyDefeated()
        }
    }

    when (phase) {
        IdleCombatPhase.IDLE -> Box(modifier = Modifier.fillMaxSize())
        IdleCombatPhase.SETUP -> GameScaffold(title = "Idle Combat Training", onExit = onExit) {
            CombatSetupScreen(onHowToPlay = machine::openHowToPlay, onStart = machine::confirmStart)
        }
        IdleCombatPhase.HOW_TO_PLAY -> GameScaffold(title = "Idle Combat Training", onExit = onExit) {
            CombatHowToPlayScreen(onBack = machine::backToSetup)
        }
        IdleCombatPhase.TRAINING,
        IdleCombatPhase.UPGRADE_MENU_OPEN,
        IdleCombatPhase.DUMMY_DESTROYED -> {
            val upgradeMenuOpen = phase == IdleCombatPhase.UPGRADE_MENU_OPEN
            GameScaffold(
                title = "Idle Combat Training",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "🥷 Dummies: ${state.dummiesDefeated}",
                        center = "Training",
                        right = "💰 ${state.coins}"
                    )
                },
                status = {
                    CombatStatus(
                        state = state,
                        controller = controller,
                        upgradeMenuOpen = upgradeMenuOpen,
                        showDefeatedMessage = phase == IdleCombatPhase.DUMMY_DESTROYED,
                        lastReward = lastReward,
                        onOpenUpgrades = machine::openUpgradeMenu,
                        onCloseUpgrades = machine::closeUpgradeMenu,
                        onBuyUpgrade = { upgradeId ->
                            controller.buyUpgrade(state, upgradeId)?.let { upgraded ->
                                state = upgraded
                                if (upgradeMenuOpen) {
                                    machine.upgradePurchased()
                                }
                            }
                        }
                    )
                }
            ) {
                CombatBoard(state = state)
            }
        }
    }
}

@Composable
private fun CombatSetupScreen(onHowToPlay: () -> Unit, onStart: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Idle Combat Training",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Train a patient fighter against tougher practice dummies, unlocking sharper techniques and faster drills.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onHowToPlay) { Text("How to Play") }
            Button(onClick = onStart) { Text("Begin Training!") }
        }
    }
}

@Composable
private fun CombatHowToPlayScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Text("How to Play", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("• Your fighter automatically cycles through every unlocked move.")
        Text("• Better stance, footwork, and conditioning raise hit rate and damage.")
        Text("• Speed Drills reduce the delay between attacks.")
        Text("• Combo Finisher unlocks through focus training or a live hit streak of three.")
        Text("• Tougher dummies pay more coins and unlock stronger techniques over time.")
    }
}

@Composable
private fun CombatBoard(state: IdleCombatState) {
    val dojoColor = lerp(GameCourt, GameHazard, 0.28f)
    val ninjaColor = GamePlayer
    val hpFraction = (state.dummy.hp.toFloat() / state.dummy.maxHp.toFloat()).coerceIn(0f, 1f)
    val dummyColor = when {
        hpFraction > 0.66f -> GameSuccess
        hpFraction > 0.33f -> GameHazard
        else -> GameEnemy
    }
    val flashColor = when (state.lastMoveResult) {
        LastMoveResult.HIT -> GameSuccess
        LastMoveResult.MISS -> GameNeutral
        LastMoveResult.CRITICAL_HIT -> GameAccent
        LastMoveResult.NONE -> GameCourtLine
    }
    val flashText = when (state.lastMoveResult) {
        LastMoveResult.HIT -> "HIT"
        LastMoveResult.MISS -> "MISS"
        LastMoveResult.CRITICAL_HIT -> "CRITICAL"
        LastMoveResult.NONE -> "READY"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(dojoColor)
        ) {
            val boardMidY = size.height * 0.6f
            val leftCenterX = size.width * 0.22f
            val rightCenterX = size.width * 0.78f
            val bodyHeight = size.height * 0.22f
            val dummyHeight = size.height * 0.28f
            val hpBarWidth = size.width * 0.22f
            val hpBarHeight = size.height * 0.03f
            val crackFactor = 1f - hpFraction

            drawCircle(
                color = ninjaColor,
                radius = size.minDimension * 0.05f,
                center = androidx.compose.ui.geometry.Offset(leftCenterX, boardMidY - bodyHeight * 0.55f)
            )
            drawLine(
                color = ninjaColor,
                start = androidx.compose.ui.geometry.Offset(leftCenterX, boardMidY - bodyHeight * 0.48f),
                end = androidx.compose.ui.geometry.Offset(leftCenterX, boardMidY + bodyHeight * 0.1f),
                strokeWidth = 10f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = ninjaColor,
                start = androidx.compose.ui.geometry.Offset(leftCenterX, boardMidY - bodyHeight * 0.2f),
                end = androidx.compose.ui.geometry.Offset(leftCenterX - 42f, boardMidY - bodyHeight * 0.02f),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = ninjaColor,
                start = androidx.compose.ui.geometry.Offset(leftCenterX, boardMidY - bodyHeight * 0.2f),
                end = androidx.compose.ui.geometry.Offset(leftCenterX + 38f, boardMidY - bodyHeight * 0.12f),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = ninjaColor,
                start = androidx.compose.ui.geometry.Offset(leftCenterX, boardMidY + bodyHeight * 0.1f),
                end = androidx.compose.ui.geometry.Offset(leftCenterX - 28f, boardMidY + bodyHeight * 0.28f),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = ninjaColor,
                start = androidx.compose.ui.geometry.Offset(leftCenterX, boardMidY + bodyHeight * 0.1f),
                end = androidx.compose.ui.geometry.Offset(leftCenterX + 30f, boardMidY + bodyHeight * 0.28f),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )

            val dummyTopLeft = androidx.compose.ui.geometry.Offset(rightCenterX - 42f, boardMidY - dummyHeight * 0.5f)
            val dummySize = androidx.compose.ui.geometry.Size(84f, dummyHeight)
            drawRoundRect(
                color = dummyColor,
                topLeft = dummyTopLeft,
                size = dummySize,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
            )
            drawRoundRect(
                color = GameCourtLine,
                topLeft = dummyTopLeft,
                size = dummySize,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
            )
            repeat(3) { index ->
                val crackY = dummyTopLeft.y + dummyHeight * (0.25f + index * 0.2f)
                val crackLength = dummySize.width * crackFactor * (0.4f + index * 0.15f)
                drawLine(
                    color = GameCourt,
                    start = androidx.compose.ui.geometry.Offset(rightCenterX - crackLength / 2f, crackY),
                    end = androidx.compose.ui.geometry.Offset(rightCenterX + crackLength / 2f, crackY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }

            drawRoundRect(
                color = GameNeutral,
                topLeft = androidx.compose.ui.geometry.Offset(rightCenterX - hpBarWidth / 2f, dummyTopLeft.y - hpBarHeight - 14f),
                size = androidx.compose.ui.geometry.Size(hpBarWidth, hpBarHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(hpBarHeight / 2f, hpBarHeight / 2f)
            )
            drawRoundRect(
                color = GameSuccess,
                topLeft = androidx.compose.ui.geometry.Offset(rightCenterX - hpBarWidth / 2f, dummyTopLeft.y - hpBarHeight - 14f),
                size = androidx.compose.ui.geometry.Size(hpBarWidth * hpFraction, hpBarHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(hpBarHeight / 2f, hpBarHeight / 2f)
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
        ) {
            Text(
                text = "Dummy #${state.dummy.number}",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold
            )
        }

        Surface(
            modifier = Modifier.align(Alignment.Center),
            color = flashColor.copy(alpha = 0.22f)
        ) {
            Text(
                text = flashText,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                color = flashColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CombatStatus(
    state: IdleCombatState,
    controller: IdleCombatTrainingController,
    upgradeMenuOpen: Boolean,
    showDefeatedMessage: Boolean,
    lastReward: Long,
    onOpenUpgrades: () -> Unit,
    onCloseUpgrades: () -> Unit,
    onBuyUpgrade: (String) -> Unit
) {
    val purchasedIds = remember(state.upgrades) {
        state.upgrades.asSequence().filter { it.purchased }.map { it.id }.toSet()
    }
    val hitChanceBonus = controller.calcHitChanceBonus(state.upgrades)
    val damageBonus = controller.calcDamageBonus(state.upgrades)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showDefeatedMessage) {
            Text(
                text = "Dummy defeated! +$lastReward coins",
                color = GameSuccess,
                fontWeight = FontWeight.Bold
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (upgradeMenuOpen) {
                Button(onClick = onCloseUpgrades) { Text("Close Training Menu") }
            } else {
                Button(onClick = onOpenUpgrades) { Text("Open Training Menu") }
            }
        }
        Text(
            text = "Hit streak: ${state.hitStreak} • Total damage: ${state.totalDamageDealt}",
            style = MaterialTheme.typography.bodySmall
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text("Upgrades", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(state.upgrades, key = { it.id }) { upgrade ->
                    val prereqsMet = upgrade.requires.all { it in purchasedIds }
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(upgrade.name, fontWeight = FontWeight.Bold)
                            Text(upgrade.description, style = MaterialTheme.typography.bodySmall)
                            Text("Cost: ${upgrade.cost}", style = MaterialTheme.typography.bodySmall)
                            when {
                                upgrade.purchased -> Text("✓ Trained", color = GameSuccess, fontWeight = FontWeight.Bold)
                                !prereqsMet -> Text("🔒 Requires: ${upgrade.requires.joinToString()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                else -> Button(
                                    onClick = { onBuyUpgrade(upgrade.id) },
                                    enabled = upgradeMenuOpen && state.coins >= upgrade.cost
                                ) {
                                    Text("Train")
                                }
                            }
                        }
                    }
                }
                item {
                    Text("Moves", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(state.moves, key = { it.id.name }) { move ->
                    val effectiveChance = (move.baseHitChance + hitChanceBonus).coerceIn(0f, 0.99f)
                    val effectiveDamage = move.baseDamage + damageBonus
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(move.name, fontWeight = FontWeight.Bold)
                            Text("Hit chance: ${(effectiveChance * 100f).toInt()}%")
                            Text("Damage: $effectiveDamage")
                            Text(if (move.unlocked) "Unlocked" else "Locked", color = if (move.unlocked) GameSuccess else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private val IdleCombatStateSaver: Saver<IdleCombatState, Any> = listSaver(
    save = { value ->
        listOf(
            value.coins,
            value.dummy.number,
            value.dummy.maxHp,
            value.dummy.hp,
            value.dummy.reward,
            value.dummiesDefeated,
            value.moves.joinToString(",") { if (it.unlocked) "1" else "0" },
            value.upgrades.joinToString(",") { if (it.purchased) "1" else "0" },
            value.currentMoveIndex,
            value.hitStreak,
            value.moveTimer,
            value.moveInterval,
            value.lastMoveResult.name,
            value.totalDamageDealt
        )
    },
    restore = { values ->
        val unlockedFlags = (values[6] as String).split(',').filter { it.isNotBlank() }.map { it == "1" }
        val purchasedFlags = (values[7] as String).split(',').filter { it.isNotBlank() }.map { it == "1" }
        val moves = IdleCombatState.INITIAL_MOVES.mapIndexed { index, move ->
            move.copy(unlocked = unlockedFlags.getOrElse(index) { move.unlocked })
        }
        val upgrades = IdleCombatState.INITIAL_UPGRADES.mapIndexed { index, upgrade ->
            upgrade.copy(purchased = purchasedFlags.getOrElse(index) { upgrade.purchased })
        }
        IdleCombatState(
            coins = values[0] as Long,
            dummy = Dummy(
                number = values[1] as Int,
                maxHp = values[2] as Long,
                hp = values[3] as Long,
                reward = values[4] as Long
            ),
            dummiesDefeated = values[5] as Int,
            moves = moves,
            upgrades = upgrades,
            currentMoveIndex = values[8] as Int,
            hitStreak = values[9] as Int,
            moveTimer = values[10] as Float,
            moveInterval = values[11] as Float,
            lastMoveResult = LastMoveResult.valueOf(values[12] as String),
            totalDamageDealt = values[13] as Long
        )
    }
)
