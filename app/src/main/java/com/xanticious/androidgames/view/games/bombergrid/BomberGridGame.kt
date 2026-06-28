package com.xanticious.androidgames.view.games.bombergrid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.bombergrid.BomberGridController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.bombergrid.BomberGridState
import com.xanticious.androidgames.model.games.bombergrid.BomberTeam
import com.xanticious.androidgames.model.games.bombergrid.CharacterStatus
import com.xanticious.androidgames.state.games.bombergrid.BomberGridPhase
import com.xanticious.androidgames.state.games.bombergrid.BomberGridStateMachine
import com.xanticious.androidgames.ui.theme.Aqua1
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Dark0
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
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlinx.coroutines.delay

/**
 * Bomber Grid — turn-based artillery tactics (`design/board-games/bomber-grid`).
 *
 * Human player vs one AI opponent, each with three characters on a destructible
 * side-view terrain.  Every turn: Move then Aim & Fire.  Characters survive one
 * blast (stunned) but are eliminated by a second, or by falling into the abyss.
 *
 * Controls and victory/defeat messaging live below the battlefield canvas in the
 * [GameScaffold] `status` slot — never over the board.
 */
@Composable
fun BomberGridGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { BomberGridController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { BomberGridStateMachine() }
    val phase by machine.phase.collectAsState()

    var gameState by remember {
        mutableStateOf(
            BomberGridState.initial(config, List(config.terrainCols) { config.terrainMinHeight })
        )
    }

    // ── Initialisation ───────────────────────────────────────────────────────

    LaunchedEffect(Unit) {
        machine.startMatch()
        val terrain = controller.generateTerrain(config)
        gameState = BomberGridState.initial(config, terrain)
        machine.terrainReady()
    }

    // ── Automatic phase transitions & AI turns ────────────────────────────────

    LaunchedEffect(phase) {
        when (phase) {
            BomberGridPhase.SELECTING_ACTOR -> {
                delay(150L)
                machine.actorSelected()
            }
            BomberGridPhase.MOVE_PHASE -> {
                if (gameState.activeTeam == BomberTeam.AI) {
                    delay(500L)
                    gameState = controller.computeAiMove(gameState, config)
                    machine.moveConfirmed()
                }
            }
            BomberGridPhase.AIM_FIRE_PHASE -> {
                if (gameState.activeTeam == BomberTeam.AI) {
                    delay(700L)
                    val (angle, power) = controller.computeAiShot(gameState, config)
                    gameState = gameState.copy(aimAngleDeg = angle, aimPower = power)
                    gameState = controller.launchProjectile(gameState, config)
                    machine.bombFired()
                }
            }
            BomberGridPhase.RESOLVING_FALLS -> {
                delay(600L)
                gameState = controller.resolveFalls(gameState)
                val w = controller.winner(gameState)
                if (w != null) {
                    gameState = gameState.copy(winner = w)
                    machine.teamEliminated()
                } else {
                    gameState = controller.advanceTurn(gameState, config)
                    machine.turnAdvanced()
                }
            }
            else -> Unit
        }
    }

    // ── Projectile animation ─────────────────────────────────────────────────

    GameLoop(running = phase == BomberGridPhase.RESOLVING_EXPLOSION) { dt ->
        if (gameState.activeProjectile == null) return@GameLoop
        val beforeChars = gameState.characters
        val stepped = controller.stepProjectile(gameState, config, dt)
        if (controller.hasImpacted(stepped)) {
            val impactPos = stepped.activeProjectile ?: Vec2(gameState.terrain.size / 2f, 0f)
            val afterExplosion = controller.resolveExplosion(stepped, config, impactPos)
            gameState = afterExplosion
            val anyHit = beforeChars.zip(afterExplosion.characters)
                .any { (before, after) -> before.status != after.status }
            if (anyHit) machine.characterHit() else machine.terrainSettled()
        } else {
            gameState = stepped
        }
    }

    // ── Player input helpers ──────────────────────────────────────────────────

    val onMoveLeft: () -> Unit = {
        gameState = controller.moveCharacter(gameState, config, -1)
        machine.movementChanged()
    }
    val onMoveRight: () -> Unit = {
        gameState = controller.moveCharacter(gameState, config, 1)
        machine.movementChanged()
    }
    val onConfirmMove: () -> Unit = { machine.moveConfirmed() }
    val onAngleDec: () -> Unit = {
        gameState = controller.adjustAim(gameState, -5f)
        machine.aimChanged()
    }
    val onAngleInc: () -> Unit = {
        gameState = controller.adjustAim(gameState, 5f)
        machine.aimChanged()
    }
    val onPowerDec: () -> Unit = {
        gameState = controller.adjustPower(gameState, -0.05f)
        machine.aimChanged()
    }
    val onPowerInc: () -> Unit = {
        gameState = controller.adjustPower(gameState, 0.05f)
        machine.aimChanged()
    }
    val onFire: () -> Unit = {
        gameState = controller.launchProjectile(gameState, config)
        machine.bombFired()
    }

    // ── Roster helper ─────────────────────────────────────────────────────────

    fun rosterString(team: BomberTeam): String =
        gameState.characters.filter { it.team == team }.joinToString(" ") { c ->
            when (c.status) {
                CharacterStatus.HEALTHY -> "${c.name}✓"
                CharacterStatus.STUNNED -> "${c.name}⚠"
                CharacterStatus.ELIMINATED -> "${c.name}✕"
            }
        }

    val windLabel = when {
        gameState.wind > 0.5f -> "Wind→"
        gameState.wind < -0.5f -> "Wind←"
        else -> "No wind"
    }

    // ── Composable tree ───────────────────────────────────────────────────────

    GameScaffold(
        title = "Bombers",
        onExit = onExit,
        hud = {
            GameHud(
                left = rosterString(BomberTeam.PLAYER),
                center = "Round ${gameState.round}  $windLabel",
                right = rosterString(BomberTeam.AI)
            )
        },
        status = {
            BomberGridStatus(
                phase = phase,
                gameState = gameState,
                onMoveLeft = onMoveLeft,
                onMoveRight = onMoveRight,
                onConfirmMove = onConfirmMove,
                onAngleDec = onAngleDec,
                onAngleInc = onAngleInc,
                onPowerDec = onPowerDec,
                onPowerInc = onPowerInc,
                onFire = onFire,
                onExit = onExit
            )
        }
    ) {
        BomberGridCanvas(
            phase = phase,
            gameState = gameState,
            controller = controller,
            config = config
        )
    }
}

// ── Canvas rendering ──────────────────────────────────────────────────────────

@Composable
private fun BomberGridCanvas(
    phase: BomberGridPhase,
    gameState: BomberGridState,
    controller: BomberGridController,
    config: com.xanticious.androidgames.model.games.bombergrid.BomberGridConfig
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // ── Viewport / coordinate transform ───────────────────────────────────
        val viewportCols = 22f
        val cellSize = w / viewportCols
        val terrainCols = gameState.terrain.size.toFloat()
        val activeCol = gameState.activeCharacter?.col?.toFloat() ?: terrainCols / 2f
        val rawPan = activeCol - viewportCols / 2f
        val panOffset = rawPan.coerceIn(0f, (terrainCols - viewportCols).coerceAtLeast(0f))

        fun wx(worldX: Float) = (worldX - panOffset) * cellSize
        fun wy(worldY: Float) = h - worldY * cellSize

        // ── Sky ───────────────────────────────────────────────────────────────
        drawRect(GameCourt, size = Size(w, h))
        // Horizon gradient: lighter strip near terrain level
        val horizonY = h * 0.65f
        drawRect(Aqua1.copy(alpha = 0.08f), topLeft = Offset(0f, horizonY), size = Size(w, h - horizonY))

        // ── Terrain ───────────────────────────────────────────────────────────
        gameState.terrain.forEachIndexed { col, height ->
            val sx = wx(col.toFloat())
            if (sx < -cellSize || sx > w + cellSize) return@forEachIndexed
            if (height <= 0) return@forEachIndexed  // abyss column
            val topY = wy(height.toFloat())
            val rectH = h - topY
            if (rectH <= 0f) return@forEachIndexed
            // Dirt body (slightly below grass cap)
            drawRect(
                color = Dark1,
                topLeft = Offset(sx, topY + cellSize * 0.18f),
                size = Size(cellSize + 1f, rectH)
            )
            // Grass cap
            drawRect(
                color = GameSuccess.copy(alpha = 0.9f),
                topLeft = Offset(sx, topY),
                size = Size(cellSize + 1f, cellSize * 0.22f)
            )
        }

        // ── Characters ────────────────────────────────────────────────────────
        gameState.characters.forEach { char ->
            if (char.status == CharacterStatus.ELIMINATED) return@forEach
            val cx = wx(char.col + 0.5f)
            val cy = wy(char.row.toFloat())
            if (cx < -cellSize * 3 || cx > w + cellSize * 3) return@forEach

            val bodyColor = when {
                char.status == CharacterStatus.STUNNED -> GameHazard
                char.team == BomberTeam.PLAYER -> GamePlayer
                else -> GameEnemy
            }
            val helmetColor = if (char.team == BomberTeam.PLAYER) Aqua2 else GameEnemy
            val isActive = char == gameState.activeCharacter
            val headR = cellSize * 0.28f
            val headCy = cy - cellSize * 1.5f

            // Active-character highlight ring
            if (isActive) {
                drawCircle(
                    GameAccent,
                    radius = headR * 2.4f,
                    center = Offset(cx, headCy),
                    style = Stroke(width = 2.5f)
                )
            }
            // Head
            drawCircle(bodyColor, radius = headR, center = Offset(cx, headCy))
            // Helmet (arc cap on head)
            drawArc(
                color = helmetColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - headR * 1.15f, headCy - headR * 1.1f),
                size = Size(headR * 2.3f, headR * 1.9f),
                style = Stroke(width = cellSize * 0.12f)
            )
            // Body
            drawLine(bodyColor, Offset(cx, headCy + headR), Offset(cx, cy - cellSize * 0.45f), strokeWidth = 3.5f)
            // Arms
            drawLine(
                bodyColor,
                Offset(cx - cellSize * 0.32f, cy - cellSize * 0.95f),
                Offset(cx + cellSize * 0.32f, cy - cellSize * 0.95f),
                strokeWidth = 2.8f
            )
            // Legs
            drawLine(bodyColor, Offset(cx, cy - cellSize * 0.45f), Offset(cx - cellSize * 0.26f, cy), strokeWidth = 2.8f)
            drawLine(bodyColor, Offset(cx, cy - cellSize * 0.45f), Offset(cx + cellSize * 0.26f, cy), strokeWidth = 2.8f)
        }

        // ── In-flight projectile ──────────────────────────────────────────────
        gameState.activeProjectile?.let { proj ->
            val px = wx(proj.x)
            val py = wy(proj.y)
            if (px in -cellSize..w + cellSize && py in -cellSize..h + cellSize) {
                drawCircle(GameAccent, radius = cellSize * 0.22f, center = Offset(px, py))
                // Tiny trail dot
                drawCircle(GameAccent.copy(alpha = 0.35f), radius = cellSize * 0.12f, center = Offset(px - cellSize * 0.35f, py + cellSize * 0.15f))
            }
        }

        // ── Explosion effect ──────────────────────────────────────────────────
        if (phase == BomberGridPhase.RESOLVING_FALLS || phase == BomberGridPhase.RESOLVING_EXPLOSION) {
            gameState.explosionCenter?.let { center ->
                val ex = wx(center.x)
                val ey = wy(center.y)
                val er = gameState.explosionRadius * cellSize
                if (er > 0f) {
                    drawCircle(GameAccent.copy(alpha = 0.25f), radius = er, center = Offset(ex, ey))
                    drawCircle(GameHazard.copy(alpha = 0.55f), radius = er, center = Offset(ex, ey), style = Stroke(width = 5f))
                    drawCircle(GameHazard.copy(alpha = 0.30f), radius = er * 0.55f, center = Offset(ex, ey), style = Stroke(width = 3f))
                }
            }
        }

        // ── Aiming arc preview ────────────────────────────────────────────────
        if (phase == BomberGridPhase.AIM_FIRE_PHASE && gameState.activeTeam == BomberTeam.PLAYER) {
            val char = gameState.activeCharacter
            if (char != null) {
                val arcStart = Vec2(char.col.toFloat(), char.row.toFloat() + 1.5f)
                val arc = controller.computeTrajectory(
                    arcStart, gameState.aimAngleDeg, gameState.aimPower, config,
                    gameState.terrain, stepDt = 0.04f, maxSteps = 28
                )
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f), 0f)
                for (i in 1 until arc.size) {
                    drawLine(
                        color = GameCourtLine.copy(alpha = 0.6f),
                        start = Offset(wx(arc[i - 1].x), wy(arc[i - 1].y)),
                        end = Offset(wx(arc[i].x), wy(arc[i].y)),
                        strokeWidth = 2.2f,
                        pathEffect = dashEffect
                    )
                }
            }
        }

        // ── Abyss indicator line at y = 0 ─────────────────────────────────────
        val abyssY = wy(0f)
        drawLine(Dark0.copy(alpha = 0.7f), Offset(0f, abyssY), Offset(w, abyssY), strokeWidth = 2f)
    }
}

// ── Status / controls strip ───────────────────────────────────────────────────

@Composable
private fun BomberGridStatus(
    phase: BomberGridPhase,
    gameState: BomberGridState,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onConfirmMove: () -> Unit,
    onAngleDec: () -> Unit,
    onAngleInc: () -> Unit,
    onPowerDec: () -> Unit,
    onPowerInc: () -> Unit,
    onFire: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        when {
            phase == BomberGridPhase.GAME_OVER -> {
                if (gameState.winner == BomberTeam.PLAYER) {
                    VictoryPanel(
                        score = gameState.round,
                        bestScore = gameState.round,
                        stars = 3,
                        onReplay = onExit,
                        onMenu = onExit,
                        headline = "Victory! All enemies eliminated."
                    )
                } else {
                    DefeatPanel(
                        score = gameState.round,
                        bestScore = gameState.round,
                        onTryAgain = onExit,
                        onMenu = onExit,
                        headline = "Defeat! Your team was wiped out."
                    )
                }
            }

            phase == BomberGridPhase.MOVE_PHASE && gameState.activeTeam == BomberTeam.PLAYER -> {
                val char = gameState.activeCharacter
                val label = "Move: ${char?.name ?: "?"}  Budget: ${gameState.moveBudget}"
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onMoveLeft) { Text("← Left") }
                    OutlinedButton(onClick = onMoveRight) { Text("Right →") }
                    Button(onClick = onConfirmMove) { Text("Confirm Move") }
                }
            }

            phase == BomberGridPhase.AIM_FIRE_PHASE && gameState.activeTeam == BomberTeam.PLAYER -> {
                val char = gameState.activeCharacter
                Text(
                    "Fire: ${char?.name ?: "?"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onAngleDec) { Text("−") }
                    Text(
                        "Angle: ${gameState.aimAngleDeg.toInt()}°",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    OutlinedButton(onClick = onAngleInc) { Text("+") }
                    OutlinedButton(onClick = onPowerDec) { Text("−") }
                    Text(
                        "Pwr: ${(gameState.aimPower * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    OutlinedButton(onClick = onPowerInc) { Text("+") }
                    Button(onClick = onFire) { Text("Fire!") }
                }
            }

            gameState.activeTeam == BomberTeam.AI &&
                    phase in listOf(
                        BomberGridPhase.MOVE_PHASE,
                        BomberGridPhase.AIM_FIRE_PHASE,
                        BomberGridPhase.RESOLVING_EXPLOSION,
                        BomberGridPhase.RESOLVING_FALLS
                    ) -> {
                Text(
                    "AI is thinking…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> Unit
        }
    }
}
