package com.xanticious.androidgames.view.games.anomalydefense

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.anomalydefense.AssaultSimulator
import com.xanticious.androidgames.controller.games.anomalydefense.LevelGenerator
import com.xanticious.androidgames.controller.games.anomalydefense.assignUnit
import com.xanticious.androidgames.controller.games.anomalydefense.buyUnit
import com.xanticious.androidgames.controller.games.anomalydefense.spawnUnits
import com.xanticious.androidgames.controller.games.anomalydefense.tickAssault
import com.xanticious.androidgames.controller.games.anomalydefense.unitFloatPos
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.anomalydefense.AnomalyDefenseState
import com.xanticious.androidgames.model.games.anomalydefense.AttackPlan
import com.xanticious.androidgames.model.games.anomalydefense.AttackUnit
import com.xanticious.androidgames.state.games.anomalydefense.AnomalyDefensePhase
import com.xanticious.androidgames.state.games.anomalydefense.AnomalyDefenseStateMachine
import com.xanticious.androidgames.ui.theme.Aqua1
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
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlinx.coroutines.delay

private const val GRID_COLS = 10
private const val GRID_ROWS = 8
private const val TICK_MS = 16L     // ~60 fps for assault animation

/**
 * Anomaly Defense — attacker-side tower-defense.
 *
 * The player studies the AI-controlled defense (routes + turrets), buys attacking
 * units within a budget, assigns them to routes, then commits to a fully
 * automatic assault simulation. Victory if enough units breach the objective.
 */
@Composable
fun AnomalyDefenseGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val machine = remember { AnomalyDefenseStateMachine() }
    val phase by machine.phase.collectAsState()

    var gameState by remember {
        mutableStateOf<AnomalyDefenseState?>(null)
    }
    var bestUnitsThrough by rememberSaveable { mutableStateOf(0) }

    // ── Level loading ─────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        machine.loadLevel()
        val level = LevelGenerator.generateLevel(seed = System.currentTimeMillis(), difficulty = difficulty)
        gameState = AnomalyDefenseState(
            level = level,
            purchasedUnits = emptyList(),
            plan = AttackPlan(emptyList()),
            budgetRemaining = level.budget,
            assaultInProgress = false,
            unitsInTransit = emptyList(),
            unitsThrough = 0,
            result = null
        )
        machine.startRecon()
    }

    // ── Assault tick loop ─────────────────────────────────────────────────────
    LaunchedEffect(phase) {
        if (phase != AnomalyDefensePhase.ASSAULT) return@LaunchedEffect
        while (true) {
            delay(TICK_MS)
            val current = gameState ?: break
            if (!current.assaultInProgress) break
            val next = tickAssault(current, TICK_MS / 1000f)
            gameState = next
            machine.assaultTick()
            if (!next.assaultInProgress) {
                val result = next.result
                if (result != null) {
                    if (result.won) machine.objectiveReached() else machine.forceWiped()
                }
                break
            }
        }
    }

    // ── Retry / back-to-menu resets ───────────────────────────────────────────
    val onRetry: () -> Unit = {
        machine.retry()
        val level = LevelGenerator.generateLevel(
            seed = System.currentTimeMillis(),
            difficulty = difficulty
        )
        gameState = AnomalyDefenseState(
            level = level,
            purchasedUnits = emptyList(),
            plan = AttackPlan(emptyList()),
            budgetRemaining = level.budget,
            assaultInProgress = false,
            unitsInTransit = emptyList(),
            unitsThrough = 0,
            result = null
        )
        machine.loadLevel()
        machine.startRecon()
    }

    val state = gameState ?: run {
        GameScaffold(title = "Anomaly Defense", onExit = onExit) { }
        return
    }

    // Track personal best
    LaunchedEffect(state.result) {
        val r = state.result ?: return@LaunchedEffect
        if (r.unitsThrough > bestUnitsThrough) bestUnitsThrough = r.unitsThrough
    }

    val quota = state.level.defense.quota

    GameScaffold(
        title = "Anomaly Defense",
        onExit = onExit,
        hud = {
            GameHud(
                left = "Budget: \$${state.budgetRemaining}",
                center = "${state.unitsThrough}/${quota}",
                right = "Best: $bestUnitsThrough"
            )
        },
        board = {
            AnomalyBoard(state = state)
        },
        status = {
            when (phase) {
                AnomalyDefensePhase.RECON -> ReconStatus(
                    onPlan = { machine.planningStarted() }
                )

                AnomalyDefensePhase.PLANNING -> {
                    var selectedUnitIndex by remember { mutableStateOf<Int?>(null) }
                    PlanningStatus(
                        state = state,
                        selectedUnitIndex = selectedUnitIndex,
                        onBuy = { unit ->
                            val next = buyUnit(state, unit)
                            if (next != null) {
                                gameState = next
                                machine.unitBought()
                            }
                        },
                        onSelectUnit = { idx -> selectedUnitIndex = idx },
                        onAssignRoute = { routeId ->
                            val sel = selectedUnitIndex ?: return@PlanningStatus
                            gameState = assignUnit(state, sel, routeId)
                            selectedUnitIndex = null
                            machine.unitAssigned()
                        },
                        onCommit = {
                            gameState = spawnUnits(state)
                            machine.committed()
                        }
                    )
                }

                AnomalyDefensePhase.ASSAULT -> AssaultStatus()

                AnomalyDefensePhase.VICTORY -> {
                    val result = state.result
                    VictoryPanel(
                        score = result?.unitsThrough ?: 0,
                        bestScore = bestUnitsThrough,
                        stars = if ((result?.unitsThrough ?: 0) >= quota * 2) 3
                                else if ((result?.unitsThrough ?: 0) >= quota) 2
                                else 1,
                        onReplay = onRetry,
                        onMenu = { machine.goToMenu(); onExit() },
                        headline = "Breach Successful!"
                    )
                }

                AnomalyDefensePhase.DEFEAT -> {
                    val result = state.result
                    DefeatPanel(
                        score = result?.unitsThrough ?: 0,
                        bestScore = bestUnitsThrough,
                        onTryAgain = onRetry,
                        onMenu = { machine.goToMenu(); onExit() },
                        headline = "Force Wiped"
                    )
                }

                else -> Unit // IDLE / LEVEL_LOADED: show nothing
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Board canvas
// ---------------------------------------------------------------------------

@Composable
private fun AnomalyBoard(state: AnomalyDefenseState) {
    val defense = state.level.defense

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cellW = size.width / GRID_COLS
        val cellH = size.height / GRID_ROWS

        fun gridToOffset(x: Float, y: Float) = Offset(x * cellW + cellW / 2, y * cellH + cellH / 2)
        fun gridCentre(x: Int, y: Int) = gridToOffset(x.toFloat(), y.toFloat())

        // Background
        drawRect(color = GameCourt, size = size)

        // Grid lines (faint)
        val gridStroke = Stroke(width = 1f)
        for (x in 0..GRID_COLS) {
            drawLine(
                color = Dark2.copy(alpha = 0.4f),
                start = Offset(x * cellW, 0f),
                end = Offset(x * cellW, size.height),
                strokeWidth = 1f
            )
        }
        for (y in 0..GRID_ROWS) {
            drawLine(
                color = Dark2.copy(alpha = 0.4f),
                start = Offset(0f, y * cellH),
                end = Offset(size.width, y * cellH),
                strokeWidth = 1f
            )
        }

        // Routes as polylines
        for (route in defense.routes) {
            if (route.tiles.size < 2) continue
            val path = Path()
            route.tiles.forEachIndexed { idx, pos ->
                val o = gridCentre(pos.x, pos.y)
                if (idx == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
            }
            drawPath(path, color = GameCourtLine, style = Stroke(width = 3f))
        }

        // Turrets: filled circle + range ring
        for (turret in defense.turrets) {
            val c = gridCentre(turret.pos.x, turret.pos.y)
            val radius = minOf(cellW, cellH) * 0.35f
            val turretColor = if (turret.disabled) GameNeutral else GameEnemy
            // Range ring
            drawCircle(
                color = turretColor.copy(alpha = 0.18f),
                radius = turret.range * minOf(cellW, cellH),
                center = c
            )
            drawCircle(
                color = turretColor.copy(alpha = 0.5f),
                radius = turret.range * minOf(cellW, cellH),
                center = c,
                style = Stroke(width = 1.5f)
            )
            // Body
            drawCircle(color = turretColor, radius = radius, center = c)
        }

        // Units in transit: small squares coloured by type
        for (u in state.unitsInTransit) {
            val route = defense.routes.firstOrNull { it.id == u.routeId } ?: continue
            val (fx, fy) = unitFloatPos(u.progress, route)
            val centre = gridToOffset(fx, fy)
            val half = minOf(cellW, cellH) * 0.25f
            val unitColor = when (u.unit) {
                AttackUnit.RUNNER   -> GamePlayer
                AttackUnit.TANK     -> GameHazard
                AttackUnit.SHIELDED -> Aqua1
                AttackUnit.SABOTEUR -> GameAccent
            }
            drawRect(
                color = unitColor,
                topLeft = Offset(centre.x - half, centre.y - half),
                size = Size(half * 2, half * 2)
            )
            // HP bar above unit
            val hpFrac = u.hp.toFloat() / u.maxHp.toFloat()
            val barW = half * 2
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(centre.x - half, centre.y - half - 6f),
                size = Size(barW, 4f)
            )
            drawRect(
                color = GameSuccess,
                topLeft = Offset(centre.x - half, centre.y - half - 6f),
                size = Size(barW * hpFrac, 4f)
            )
        }

        // Objective: gold star-ish cross marker
        val obj = defense.objective
        val oc = gridCentre(obj.x, obj.y)
        val arm = minOf(cellW, cellH) * 0.4f
        drawLine(GameSuccess, Offset(oc.x - arm, oc.y), Offset(oc.x + arm, oc.y), strokeWidth = 4f)
        drawLine(GameSuccess, Offset(oc.x, oc.y - arm), Offset(oc.x, oc.y + arm), strokeWidth = 4f)
        drawCircle(GameSuccess, radius = arm * 0.5f, center = oc)
    }
}

// ---------------------------------------------------------------------------
// Status composables
// ---------------------------------------------------------------------------

@Composable
private fun ReconStatus(onPlan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Study the defenses — tap Plan to start",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onPlan, modifier = Modifier.fillMaxWidth()) {
            Text("Plan Assault")
        }
    }
}

@Composable
private fun PlanningStatus(
    state: AnomalyDefenseState,
    selectedUnitIndex: Int?,
    onBuy: (AttackUnit) -> Unit,
    onSelectUnit: (Int) -> Unit,
    onAssignRoute: (Int) -> Unit,
    onCommit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Buy palette
        Text("Buy units  (Budget: \$${state.budgetRemaining})", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            state.level.unitCosts.forEach { uc ->
                OutlinedButton(
                    onClick = { onBuy(uc.unit) },
                    enabled = state.budgetRemaining >= uc.cost,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("${uc.unit.name.take(3)}\n\$${uc.cost}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Purchased but unassigned units
        if (state.purchasedUnits.isNotEmpty()) {
            Text("Purchased — tap a unit, then choose a route:", style = MaterialTheme.typography.bodySmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(state.purchasedUnits) { idx, unit ->
                    val isSelected = selectedUnitIndex == idx
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(unitColor(unit).copy(alpha = if (isSelected) 1f else 0.5f))
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = Color.White
                            )
                            .clickable { onSelectUnit(idx) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            unit.name.take(3),
                            style = MaterialTheme.typography.labelSmall,
                            color = Dark0
                        )
                    }
                }
            }
            // Route selection buttons (only shown when a unit is selected)
            if (selectedUnitIndex != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.level.defense.routes.forEachIndexed { _, route ->
                        Button(
                            onClick = { onAssignRoute(route.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Route ${route.id + 1}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Current plan summary
        val totalAssigned = state.plan.assignments.sumOf { it.units.size }
        if (totalAssigned > 0) {
            Text("Assigned: ${state.plan.assignments.joinToString("  ") { a ->
                "R${a.routeId + 1}: ${a.units.size}"
            }}", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(2.dp))
        Button(
            onClick = onCommit,
            enabled = totalAssigned > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Commit Assault")
        }
    }
}

@Composable
private fun AssaultStatus() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Assault in progress…",
            style = MaterialTheme.typography.titleMedium,
            color = GameHazard,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun unitColor(unit: AttackUnit): Color = when (unit) {
    AttackUnit.RUNNER   -> GamePlayer
    AttackUnit.TANK     -> GameHazard
    AttackUnit.SHIELDED -> Aqua1
    AttackUnit.SABOTEUR -> GameAccent
}
