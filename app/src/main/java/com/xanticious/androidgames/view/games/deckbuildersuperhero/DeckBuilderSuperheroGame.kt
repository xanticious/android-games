package com.xanticious.androidgames.view.games.deckbuildersuperhero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.deckbuildersuperhero.DeckBuilderController
import com.xanticious.androidgames.controller.games.deckbuildersuperhero.EndCondition
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.deckbuildersuperhero.DeckBuilderState
import com.xanticious.androidgames.model.games.deckbuildersuperhero.DeckCard
import com.xanticious.androidgames.model.games.deckbuildersuperhero.Hero
import com.xanticious.androidgames.model.games.deckbuildersuperhero.HeroArchetype
import com.xanticious.androidgames.model.games.deckbuildersuperhero.Villain
import com.xanticious.androidgames.state.games.deckbuildersuperhero.DeckBuilderPhase
import com.xanticious.androidgames.state.games.deckbuildersuperhero.DeckBuilderStateMachine
import com.xanticious.androidgames.ui.theme.Aqua0
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.ui.theme.CardHighlight
import com.xanticious.androidgames.ui.theme.CardSlot
import com.xanticious.androidgames.ui.theme.CardTableFelt
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlin.random.Random

/**
 * Top-level composable for Deck Builder Superhero.
 *
 * Cooperative deck-building card game: the human hero plus AI teammates vs
 * the Super Villain. Defeat the villain before the scheme track completes.
 *
 * Game id: `deck-builder-superhero`
 */
@Composable
fun DeckBuilderSuperheroGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { DeckBuilderController() }
    val machine = remember { DeckBuilderStateMachine() }
    val phase by machine.phase.collectAsState()

    // Random used for all game randomness — single instance for the session.
    val random = remember { Random(System.currentTimeMillis()) }

    var gameState by remember { mutableStateOf<DeckBuilderState?>(null) }
    var selectedArchetype by remember { mutableStateOf(HeroArchetype.CULL) }
    // Number of AI teammates: 1 by default for all difficulties.
    val numBots = remember(difficulty) { 1 }

    // Launch the state machine on first composition.
    LaunchedEffect(Unit) { machine.startGame() }

    // SETUP → draw human hand and fire SetupComplete.
    LaunchedEffect(phase) {
        if (phase == DeckBuilderPhase.SETUP) {
            val initial = controller.buildInitialState(difficulty, selectedArchetype, numBots, random)
            gameState = controller.startRound(initial, random)
            machine.setupComplete()
        }
    }

    // VILLAIN_PHASE → process AI turns + scheme advance, then continue/win/lose.
    LaunchedEffect(phase) {
        if (phase == DeckBuilderPhase.VILLAIN_PHASE) {
            val s = gameState ?: return@LaunchedEffect
            val afterTurn = controller.endPlayerTurn(s, random)
            gameState = afterTurn
            when (controller.checkEndCondition(afterTurn)) {
                EndCondition.WON  -> machine.villainDefeated()
                EndCondition.LOST -> machine.schemeCompleted()
                EndCondition.ONGOING -> {
                    gameState = controller.startRound(afterTurn, random)
                    machine.continueGame()
                }
            }
        }
    }

    GameScaffold(
        title = "Deck Builder Superhero",
        onExit = onExit,
        hud = {
            val s = gameState
            if (s != null && phase == DeckBuilderPhase.HERO_TURN) {
                GameHud(
                    left = "Round ${s.round}",
                    center = "⚡${s.currentPower}  ⚔️${s.currentAttack}",
                    right = "Scheme ${s.villain.schemeProgress}/${s.villain.schemeTotal}"
                )
            }
        },
        status = {
            val s = gameState
            when (phase) {
                DeckBuilderPhase.WON -> {
                    VictoryPanel(
                        score = s?.round ?: 0,
                        bestScore = s?.round ?: 0,
                        stars = 3,
                        onReplay = { machine.replay() },
                        onMenu = onExit,
                        headline = "Villain Defeated!"
                    )
                }
                DeckBuilderPhase.LOST -> {
                    DefeatPanel(
                        score = s?.villain.let { it?.schemeProgress ?: 0 },
                        bestScore = s?.villain?.schemeTotal ?: 0,
                        onTryAgain = { machine.replay() },
                        onMenu = onExit,
                        headline = "Scheme Completed!"
                    )
                }
                else -> {}
            }
        }
    ) {
        when (phase) {
            DeckBuilderPhase.IDLE,
            DeckBuilderPhase.HERO_SELECT -> HeroSelectScreen(selectedArchetype) { arch ->
                selectedArchetype = arch
                machine.heroesAssigned()
            }

            DeckBuilderPhase.SETUP -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Setting up…", color = Aqua1, fontSize = 18.sp)
                }
            }

            DeckBuilderPhase.HERO_TURN -> {
                val s = gameState
                if (s != null) {
                    GameBoard(
                        state = s,
                        onPlayCard = { idx ->
                            gameState = controller.playCard(s, idx, random)
                        },
                        onRecruitAlly = { idx ->
                            gameState = controller.recruitAlly(s, idx)
                        },
                        onAttackVillain = {
                            gameState = controller.attackVillain(s)
                        },
                        onEndTurn = { machine.endTurn() }
                    )
                }
            }

            DeckBuilderPhase.VILLAIN_PHASE -> {
                val s = gameState
                if (s != null) {
                    Box(Modifier.fillMaxSize().background(Dark0), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Villain Phase…",
                                color = GameEnemy,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Scheme advancing!",
                                color = Aqua1,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            DeckBuilderPhase.WON, DeckBuilderPhase.LOST -> {
                val s = gameState
                if (s != null) {
                    // Show a frozen board behind the status panel.
                    GameBoard(
                        state = s,
                        onPlayCard = {},
                        onRecruitAlly = {},
                        onAttackVillain = {},
                        onEndTurn = {}
                    )
                }
            }
        }
    }
}

// ── Hero select ───────────────────────────────────────────────────────────────

@Composable
private fun HeroSelectScreen(selected: HeroArchetype, onSelect: (HeroArchetype) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Dark0)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Choose Your Hero",
            color = Aqua1,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Tap a hero then tap Start",
            color = Aqua3,
            fontSize = 13.sp
        )

        val heroes = listOf(
            HeroArchetype.CULL     to "Monk",
            HeroArchetype.BRAWL    to "Tigerman",
            HeroArchetype.SWARM    to "Echo",
            HeroArchetype.BANKROLL to "Bling",
            HeroArchetype.DEFENSE  to "Aegis",
            HeroArchetype.TEMPO    to "Volt"
        )

        heroes.forEach { (arch, heroName) ->
            val isSelected = arch == selected
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) CardHighlight else Dark2,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { /* just select, don't start yet */ },
                color = if (isSelected) Dark2 else Dark1,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clickable { },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(heroName, color = if (isSelected) CardHighlight else Aqua1, fontWeight = FontWeight.Bold)
                        Text(arch.displayName, color = Aqua3, fontSize = 12.sp)
                        Text(arch.flavorText, color = Aqua3, fontSize = 11.sp)
                    }
                }
            }
            // Invisible tap target so the whole row is tappable without nesting clickable.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.dp)
                    .clickable { /* no-op placeholder */ }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Show hero selection buttons as a separate step for clarity.
        heroes.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (arch, heroName) ->
                    val isSelected = arch == selected
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isSelected) CardHighlight else Aqua2
                        )
                    ) {
                        Text(heroName, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        Button(
            onClick = { onSelect(selected) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Aqua2)
        ) {
            Text("Start as ${selected.displayName}", color = Dark0, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Main game board ──────────────────────────────────────────────────────────

@Composable
private fun GameBoard(
    state: DeckBuilderState,
    onPlayCard: (Int) -> Unit,
    onRecruitAlly: (Int) -> Unit,
    onAttackVillain: () -> Unit,
    onEndTurn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Dark0)
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Villain panel.
        VillainPanel(villain = state.villain, onAttack = onAttackVillain, attackAvailable = state.currentAttack > 0)

        // Recruit row.
        Text("Recruit Row", color = Aqua3, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
        RecruitRow(
            row = state.recruitRow,
            availablePower = state.currentPower,
            onRecruit = onRecruitAlly
        )

        // Teammate status (AI heroes).
        val bots = state.heroes.filter { !it.isHuman }
        if (bots.isNotEmpty()) {
            Text("Teammates", color = Aqua3, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                bots.forEach { bot ->
                    TeammateStatus(hero = bot, modifier = Modifier.weight(1f))
                }
            }
        }

        // Human hand.
        val human = state.humanHero
        Text(
            "Your Hand (⚡${state.currentPower}  ⚔️${state.currentAttack})",
            color = Aqua1,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
        if (human.hand.isEmpty()) {
            Text("Hand empty — tap End Turn", color = Aqua3, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                itemsIndexed(human.hand) { idx, card ->
                    HandCard(card = card, onClick = { onPlayCard(idx) })
                }
            }
        }

        // Action buttons.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.currentAttack > 0) {
                Button(
                    onClick = onAttackVillain,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GameEnemy)
                ) {
                    Text("⚔️ Attack (${state.currentAttack})", color = Aqua0, fontSize = 12.sp)
                }
            }
            Button(
                onClick = onEndTurn,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Aqua3)
            ) {
                Text("End Turn", color = Dark0, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Deck info.
        Text(
            "Deck: ${human.deck.size}  Discard: ${human.discard.size}",
            color = Aqua3,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

// ── Villain panel ─────────────────────────────────────────────────────────────

@Composable
private fun VillainPanel(villain: Villain, onAttack: () -> Unit, attackAvailable: Boolean) {
    val hpFraction = (villain.hp.toFloat() / villain.maxHp).coerceIn(0f, 1f)
    val schemeFraction = (villain.schemeProgress.toFloat() / villain.schemeTotal).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Dark2,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "☠ ${villain.name}",
                    color = GameEnemy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "HP ${villain.hp}/${villain.maxHp}",
                    color = Aqua1,
                    fontSize = 13.sp
                )
            }
            // HP bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Dark0)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(hpFraction)
                        .height(8.dp)
                        .background(GameEnemy)
                )
            }
            // Scheme track
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Scheme ", color = Aqua3, fontSize = 12.sp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Dark0)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(schemeFraction)
                            .height(6.dp)
                            .background(Aqua4)
                    )
                }
                Text(
                    " ${villain.schemeProgress}/${villain.schemeTotal}",
                    color = Aqua3,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ── Recruit row ───────────────────────────────────────────────────────────────

@Composable
private fun RecruitRow(
    row: List<DeckCard?>,
    availablePower: Int,
    onRecruit: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        itemsIndexed(row) { idx, card ->
            if (card == null) {
                EmptySlot()
            } else {
                val affordable = availablePower >= card.cost
                MarketCard(card = card, affordable = affordable, onClick = { onRecruit(idx) })
            }
        }
    }
}

@Composable
private fun EmptySlot() {
    Box(
        modifier = Modifier
            .size(width = 72.dp, height = 96.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(CardSlot)
            .border(1.dp, Dark2, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("Empty", color = Aqua3, fontSize = 10.sp)
    }
}

@Composable
private fun MarketCard(card: DeckCard, affordable: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(width = 72.dp, height = 96.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(
                width = if (affordable) 2.dp else 1.dp,
                color = if (affordable) Aqua2 else Dark2,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(enabled = affordable, onClick = onClick),
        color = if (affordable) Dark2 else Dark1,
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "⚡${card.cost}",
                color = if (affordable) CardHighlight else Aqua3,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                card.name,
                color = if (affordable) Aqua1 else Aqua3,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.Center) {
                if (card.power > 0) Text("⚡${card.power}", color = Aqua2, fontSize = 9.sp)
                if (card.power > 0 && card.attack > 0) Spacer(Modifier.width(2.dp))
                if (card.attack > 0) Text("⚔${card.attack}", color = GameEnemy, fontSize = 9.sp)
                if (card.extraDraw > 0) Text("+${card.extraDraw}✦", color = GameSuccess, fontSize = 9.sp)
            }
        }
    }
}

// ── Hand card ─────────────────────────────────────────────────────────────────

@Composable
private fun HandCard(card: DeckCard, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(width = 72.dp, height = 108.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, CardHighlight, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = CardTableFelt,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                card.name,
                color = Aqua0,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (card.power > 0) Text("⚡${card.power}", color = CardHighlight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (card.attack > 0) Text("⚔${card.attack}", color = GameEnemy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (card.extraDraw > 0) Text("+${card.extraDraw}✦", color = GameSuccess, fontSize = 12.sp)
                if (card.power == 0 && card.attack == 0 && card.extraDraw == 0)
                    Text("✦", color = Aqua3, fontSize = 14.sp)
            }
            Text("Tap to play", color = Aqua3, fontSize = 8.sp)
        }
    }
}

// ── Teammate status ───────────────────────────────────────────────────────────

@Composable
private fun TeammateStatus(hero: Hero, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(6.dp)),
        color = Dark1,
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(hero.name, color = Aqua2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(hero.archetype.displayName, color = Aqua3, fontSize = 9.sp)
            Text("Deck:${hero.deck.size} Disc:${hero.discard.size}", color = Aqua3, fontSize = 9.sp)
        }
    }
}
