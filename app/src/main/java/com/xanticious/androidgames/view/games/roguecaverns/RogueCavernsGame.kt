package com.xanticious.androidgames.view.games.roguecaverns

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.roguecaverns.CombatResolver
import com.xanticious.androidgames.controller.games.roguecaverns.Exploration
import com.xanticious.androidgames.controller.games.roguecaverns.LevelGenerator
import com.xanticious.androidgames.controller.games.roguecaverns.MetaProgression
import com.xanticious.androidgames.controller.games.roguecaverns.MonsterAi
import com.xanticious.androidgames.controller.games.roguecaverns.XpCalculator
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.GridPos
import com.xanticious.androidgames.model.games.roguecaverns.CombatAction
import com.xanticious.androidgames.model.games.roguecaverns.Hero
import com.xanticious.androidgames.model.games.roguecaverns.HeroStats
import com.xanticious.androidgames.model.games.roguecaverns.MetaProfile
import com.xanticious.androidgames.model.games.roguecaverns.PermanentUpgrades
import com.xanticious.androidgames.model.games.roguecaverns.RogueCavernsState
import com.xanticious.androidgames.model.games.roguecaverns.RoomType
import com.xanticious.androidgames.model.games.roguecaverns.RunSummary
import com.xanticious.androidgames.state.games.roguecaverns.RogueCavernsPhase
import com.xanticious.androidgames.state.games.roguecaverns.RogueCavernsStateMachine
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
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

/**
 * Rogue Caverns — roguelite dungeon crawler with meta-progression.
 *
 * Architecture:
 * - [RogueCavernsStateMachine] drives the phase (IDLE → META_HUB → EXPLORING ↔ COMBAT → RUN_RESULTS).
 * - [RogueCavernsState] holds all game/run data, mutated exclusively via controller functions.
 * - Each phase renders a distinct sub-composable in the [GameScaffold] board slot.
 */
@Composable
fun RogueCavernsGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val stateMachine = remember { RogueCavernsStateMachine() }
    val phase by stateMachine.phase.collectAsState()

    // Game state is held in plain remember — no Saver defined yet for v1.
    var gameState by remember {
        val hero = MetaProgression.startingHero(PermanentUpgrades())
        mutableStateOf(
            RogueCavernsState(
                metaProfile = MetaProfile(),
                currentLevel = null,
                heroPos = GridPos.ZERO,
                hero = hero,
                depth = 0,
                kills = 0,
                currentMonster = null,
                combatLog = emptyList(),
                runSummary = null,
                seed = System.currentTimeMillis()
            )
        )
    }

    LaunchedEffect(Unit) { stateMachine.hubOpened() }

    // ── Callbacks ──────────────────────────────────────────────────────────────

    val onUpgrade: (String) -> Unit = { type ->
        val newProfile = MetaProgression.applyUpgrade(gameState.metaProfile, type)
        if (newProfile != null) {
            gameState = gameState.copy(metaProfile = newProfile)
            stateMachine.upgradePurchased()
        }
    }

    val onBeginDescent: () -> Unit = {
        val seed = System.currentTimeMillis()
        val level = LevelGenerator.generateLevel(1, seed)
        val hero = MetaProgression.startingHero(gameState.metaProfile.upgrades)
        gameState = gameState.copy(
            currentLevel = level,
            heroPos = level.heroStartPos,
            hero = hero,
            depth = 1,
            kills = 0,
            currentMonster = null,
            combatLog = emptyList(),
            runSummary = null,
            seed = seed
        )
        stateMachine.runStarted()
    }

    val onMove: (GridPos) -> Unit = onMove@{ target ->
        val moved = Exploration.moveHero(gameState, target)
        val level = moved.currentLevel ?: return@onMove
        val room = level.rooms.firstOrNull { it.pos == target } ?: return@onMove
        gameState = moved
        if (room.type == RoomType.MONSTER && room.monster != null) {
            gameState = gameState.copy(currentMonster = room.monster)
            stateMachine.encounterStarted()
        } else {
            stateMachine.movedRoom()
        }
    }

    val onDescend: () -> Unit = {
        val nextSeed = System.currentTimeMillis()
        gameState = Exploration.descend(gameState, nextSeed)
        stateMachine.descendedLevel()
    }

    val onBankAndExit: () -> Unit = {
        gameState = Exploration.bankAndExit(gameState)
        stateMachine.bankedAndExited()
    }

    val onCombatAction: (CombatAction) -> Unit = onCombatAction@{ action ->
        val hero = gameState.hero
        val monster = gameState.currentMonster ?: return@onCombatAction
        val monsterAction = MonsterAi.chooseMonsterAction(monster, hero.hp, difficulty)
        val result = CombatResolver.resolveTurn(hero, monster, action, monsterAction)

        val newHero = hero.copy(hp = result.heroHp)
        val newMonster = monster.copy(hp = result.monsterHp)
        gameState = gameState.copy(
            hero = newHero,
            currentMonster = newMonster,
            combatLog = gameState.combatLog + result.log
        )

        when {
            result.fled -> {
                gameState = gameState.copy(currentMonster = null)
                stateMachine.movedRoom()
            }
            result.monsterHp <= 0 -> {
                // Clear the defeated monster from its room
                val level = gameState.currentLevel
                val updatedLevel = level?.copy(
                    rooms = level.rooms.map { room ->
                        if (room.pos == gameState.heroPos) room.copy(type = RoomType.EMPTY, monster = null)
                        else room
                    }
                )
                gameState = gameState.copy(
                    currentLevel = updatedLevel,
                    currentMonster = null,
                    kills = gameState.kills + 1
                )
                stateMachine.monsterDefeated()
            }
            result.heroHp <= 0 -> {
                val baseSummary = RunSummary(gameState.depth, gameState.kills, banked = false, xpEarned = 0L)
                val xp = XpCalculator.award(baseSummary, gameState.metaProfile.upgrades.fortune)
                val summary = baseSummary.copy(xpEarned = xp)
                val updatedProfile = gameState.metaProfile.copy(
                    totalXp = gameState.metaProfile.totalXp + xp,
                    bestDepth = maxOf(gameState.metaProfile.bestDepth, gameState.depth)
                )
                gameState = gameState.copy(metaProfile = updatedProfile, runSummary = summary)
                stateMachine.heroFainted()
            }
            else -> stateMachine.turnTaken()
        }
    }

    val onReturnToHub: () -> Unit = {
        gameState = gameState.copy(
            currentLevel = null,
            heroPos = GridPos.ZERO,
            depth = 0,
            kills = 0,
            currentMonster = null,
            combatLog = emptyList(),
            runSummary = null
        )
        stateMachine.continued()
    }

    // ── Scaffold ───────────────────────────────────────────────────────────────

    GameScaffold(
        title = "Rogue Caverns",
        onExit = onExit,
        hud = {
            if (phase == RogueCavernsPhase.EXPLORING) {
                GameHud(
                    left = "Depth: ${gameState.depth}",
                    center = "HP: ${gameState.hero.hp}/${gameState.hero.stats.maxHp}",
                    right = "Kills: ${gameState.kills}"
                )
            }
        },
        board = {
            when (phase) {
                RogueCavernsPhase.META_HUB ->
                    MetaHubBoard(
                        state = gameState,
                        onUpgrade = onUpgrade,
                        onBeginDescent = onBeginDescent,
                        modifier = Modifier.fillMaxSize()
                    )
                RogueCavernsPhase.EXPLORING ->
                    CavernGrid(
                        state = gameState,
                        modifier = Modifier.fillMaxSize()
                    )
                RogueCavernsPhase.COMBAT ->
                    CombatBoard(
                        state = gameState,
                        modifier = Modifier.fillMaxSize()
                    )
                RogueCavernsPhase.RUN_RESULTS ->
                    RunResultsBoard(
                        state = gameState,
                        modifier = Modifier.fillMaxSize()
                    )
                RogueCavernsPhase.IDLE -> Unit
            }
        },
        status = {
            when (phase) {
                RogueCavernsPhase.EXPLORING ->
                    ExploringStatusBar(
                        state = gameState,
                        onMove = onMove,
                        onDescend = onDescend,
                        onBankAndExit = onBankAndExit
                    )
                RogueCavernsPhase.COMBAT ->
                    CombatActionBar(onAction = onCombatAction)
                RogueCavernsPhase.RUN_RESULTS ->
                    ReturnToHubBar(onReturnToHub = onReturnToHub)
                else -> Unit
            }
        }
    )
}

// ─── Meta Hub ──────────────────────────────────────────────────────────────────

@Composable
private fun MetaHubBoard(
    state: RogueCavernsState,
    onUpgrade: (String) -> Unit,
    onBeginDescent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = state.metaProfile
    val upgrades = profile.upgrades

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Total XP: ${profile.totalXp}", style = MaterialTheme.typography.titleLarge)
        Text("Best Depth: ${profile.bestDepth}", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Permanent Upgrades",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        UpgradeRow("Vitality",    upgrades.vitality, profile.totalXp, "vitality", "+20 max HP",   onUpgrade)
        UpgradeRow("Power",       upgrades.power,    profile.totalXp, "power",    "+5 attack",     onUpgrade)
        UpgradeRow("Guard",       upgrades.guard,    profile.totalXp, "guard",    "+3 defense",    onUpgrade)
        UpgradeRow("Fortune",     upgrades.fortune,  profile.totalXp, "fortune",  "+20% XP/run",  onUpgrade)

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onBeginDescent,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Aqua2)
        ) {
            Text("Begin Descent", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun UpgradeRow(
    label: String,
    rank: Int,
    totalXp: Long,
    type: String,
    description: String,
    onUpgrade: (String) -> Unit
) {
    val cost = MetaProgression.upgradeCost(rank)
    val canAfford = totalXp >= cost

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("$label  (Rank $rank)", fontWeight = FontWeight.SemiBold)
            Text("$description · Next upgrade: $cost XP", style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = { onUpgrade(type) },
            enabled = canAfford,
            colors = ButtonDefaults.buttonColors(containerColor = GameAccent)
        ) {
            Text("+", fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Cavern Grid ───────────────────────────────────────────────────────────────

@Composable
private fun CavernGrid(
    state: RogueCavernsState,
    modifier: Modifier = Modifier
) {
    val level = state.currentLevel

    Canvas(modifier = modifier.background(GameCourt)) {
        if (level == null) return@Canvas

        val cellW = size.width / level.cols
        val cellH = size.height / level.rows
        val padding = 3f

        level.rooms.forEach { room ->
            val ox = room.pos.x * cellW
            val oy = room.pos.y * cellH

            val fillColor = when {
                room.pos == state.heroPos -> GamePlayer
                !room.explored -> Dark0
                room.type == RoomType.MONSTER && room.monster != null -> GameEnemy.copy(alpha = 0.75f)
                room.type == RoomType.TREASURE -> GameAccent
                room.type == RoomType.DESCENT -> Aqua2
                room.type == RoomType.SAFE -> GameSuccess.copy(alpha = 0.8f)
                else -> Dark1
            }

            drawRect(
                color = fillColor,
                topLeft = Offset(ox + padding, oy + padding),
                size = Size(cellW - padding * 2, cellH - padding * 2)
            )

            // Subtle grid line
            drawRect(
                color = GameCourtLine.copy(alpha = 0.25f),
                topLeft = Offset(ox, oy),
                size = Size(cellW, cellH),
                style = Stroke(width = 1f)
            )
        }
    }
}

// ─── Exploring Status Bar ──────────────────────────────────────────────────────

@Composable
private fun ExploringStatusBar(
    state: RogueCavernsState,
    onMove: (GridPos) -> Unit,
    onDescend: () -> Unit,
    onBankAndExit: () -> Unit
) {
    val level = state.currentLevel ?: return
    val heroPos = state.heroPos
    val currentRoom = level.rooms.firstOrNull { it.pos == heroPos }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Direction buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                "↑" to GridPos(heroPos.x, heroPos.y - 1),
                "↓" to GridPos(heroPos.x, heroPos.y + 1),
                "←" to GridPos(heroPos.x - 1, heroPos.y),
                "→" to GridPos(heroPos.x + 1, heroPos.y)
            ).forEach { (arrow, target) ->
                val valid = target.x in 0 until level.cols && target.y in 0 until level.rows
                Button(
                    onClick = { onMove(target) },
                    enabled = valid,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(arrow)
                }
            }
        }

        // Context-sensitive actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (currentRoom?.type == RoomType.DESCENT) {
                Button(
                    onClick = onDescend,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Aqua2)
                ) {
                    Text("Descend Deeper")
                }
            }
            if (currentRoom?.type == RoomType.SAFE && state.depth > 0) {
                Button(
                    onClick = onBankAndExit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GameSuccess)
                ) {
                    Text("Bank & Exit")
                }
            }
        }
    }
}

// ─── Combat Board ──────────────────────────────────────────────────────────────

@Composable
private fun CombatBoard(
    state: RogueCavernsState,
    modifier: Modifier = Modifier
) {
    val monster = state.currentMonster ?: return
    val hero = state.hero

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Combatant panels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Monster panel
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    monster.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = GameEnemy,
                    fontWeight = FontWeight.Bold
                )
                Text("HP: ${monster.hp} / ${monster.maxHp}", style = MaterialTheme.typography.bodySmall)
                HpBar(current = monster.hp, max = monster.maxHp, color = GameEnemy)
                Text(
                    "ATK ${monster.attack}  DEF ${monster.defense}  ${monster.element.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GameNeutral
                )
            }

            // Hero panel
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Hero",
                    style = MaterialTheme.typography.titleMedium,
                    color = GamePlayer,
                    fontWeight = FontWeight.Bold
                )
                Text("HP: ${hero.hp} / ${hero.stats.maxHp}", style = MaterialTheme.typography.bodySmall)
                HpBar(current = hero.hp, max = hero.stats.maxHp, color = GamePlayer)
                Text(
                    "ATK ${hero.stats.attack}  DEF ${hero.stats.defense}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GameNeutral
                )
            }
        }

        // Combat log — last 3 entries
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            state.combatLog.takeLast(3).forEach { entry ->
                Text(entry, style = MaterialTheme.typography.bodySmall, color = Aqua1)
            }
        }
    }
}

@Composable
private fun HpBar(current: Int, max: Int, color: Color) {
    val fraction = (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(Dark2)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(color)
        )
    }
}

// ─── Combat Action Bar ─────────────────────────────────────────────────────────

@Composable
private fun CombatActionBar(onAction: (CombatAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = { onAction(CombatAction.ATTACK) }) { Text("Attack") }
        Button(
            onClick = { onAction(CombatAction.SKILL) },
            colors = ButtonDefaults.buttonColors(containerColor = Aqua2)
        ) { Text("Skill") }
        Button(
            onClick = { onAction(CombatAction.USE_ITEM) },
            colors = ButtonDefaults.buttonColors(containerColor = GameSuccess)
        ) { Text("Item") }
        OutlinedButton(onClick = { onAction(CombatAction.FLEE) }) { Text("Flee") }
    }
}

// ─── Run Results ───────────────────────────────────────────────────────────────

@Composable
private fun RunResultsBoard(
    state: RogueCavernsState,
    modifier: Modifier = Modifier
) {
    val summary = state.runSummary ?: return
    val profile = state.metaProfile

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (summary.banked) "Run Complete!" else "Hero Fainted",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (summary.banked) GameSuccess else GameEnemy
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text("Depth Reached: ${summary.depthReached}")
        Text("Monsters Slain: ${summary.kills}")
        Text("XP Earned: ${summary.xpEarned}")
        Text("Total XP: ${profile.totalXp}")
        Text("Best Depth: ${profile.bestDepth}")
    }
}

@Composable
private fun ReturnToHubBar(onReturnToHub: () -> Unit) {
    Button(
        onClick = onReturnToHub,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Aqua2)
    ) {
        Text("Return to Hub", fontWeight = FontWeight.Bold)
    }
}
