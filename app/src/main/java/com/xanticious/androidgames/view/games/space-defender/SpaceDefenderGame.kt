package com.xanticious.androidgames.view.games.spacedefender

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.spacedefender.SpaceDefenderController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.JoystickInput
import com.xanticious.androidgames.model.games.spacedefender.Enemy
import com.xanticious.androidgames.model.games.spacedefender.EnemyType
import com.xanticious.androidgames.model.games.spacedefender.Projectile
import com.xanticious.androidgames.model.games.spacedefender.ProjectileOwner
import com.xanticious.androidgames.model.games.spacedefender.Shield
import com.xanticious.androidgames.model.games.spacedefender.ShieldHealth
import com.xanticious.androidgames.model.games.spacedefender.SpaceDefenderEvent
import com.xanticious.androidgames.model.games.spacedefender.SpaceDefenderInput
import com.xanticious.androidgames.model.games.spacedefender.SpaceDefenderState
import com.xanticious.androidgames.state.games.spacedefender.SpaceDefenderPhase
import com.xanticious.androidgames.state.games.spacedefender.SpaceDefenderStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VirtualJoystick

/**
 * Space Defender — defend against descending enemy waves using a sliding auto-firing cannon.
 * Entry composable wired to [GameDifficulty]; uses [GameScaffold] + [GameLoop] + [VirtualJoystick].
 */
@Composable
fun SpaceDefenderGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { SpaceDefenderController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { SpaceDefenderStateMachine() }
    val phase by machine.phase.collectAsState()

    var gameState by remember { mutableStateOf(SpaceDefenderState.initial()) }
    var joystickInput by remember { mutableStateOf(JoystickInput.NONE) }
    var waveIntroCountdown by remember { mutableStateOf(0f) }
    var waveCompleteTimer by remember { mutableStateOf(0f) }
    var bestScore by remember { mutableStateOf(0) }

    // Capture colors outside Canvas (DrawScope is not @Composable)
    val courtColor = GameCourt
    val playerColor = GamePlayer
    val enemyColor = GameEnemy
    val projectilePlayerColor = GameAccent
    val projectileEnemyColor = GameHazard
    val shieldColor = GameSuccess
    val neutralColor = GameNeutral
    val accentColor = GameAccent

    LaunchedEffect(Unit) {
        val first = controller.startFirstWave(SpaceDefenderState.initial(), config)
        gameState = first
        waveIntroCountdown = config.waveIntroDuration
        machine.startGame()
    }

    // Wave intro countdown
    LaunchedEffect(phase) {
        if (phase == SpaceDefenderPhase.WAVE_INTRO) {
            waveIntroCountdown = config.waveIntroDuration
        }
    }

    GameLoop(running = phase == SpaceDefenderPhase.PLAYING || phase == SpaceDefenderPhase.WAVE_INTRO || phase == SpaceDefenderPhase.WAVE_COMPLETE) { dt ->
        when (phase) {
            SpaceDefenderPhase.WAVE_INTRO -> {
                waveIntroCountdown -= dt
                if (waveIntroCountdown <= 0f) machine.introComplete()
            }
            SpaceDefenderPhase.WAVE_COMPLETE -> {
                waveCompleteTimer -= dt
                if (waveCompleteTimer <= 0f) {
                    val next = controller.startNextWave(gameState, config)
                    gameState = next
                    machine.nextWave()
                }
            }
            SpaceDefenderPhase.PLAYING -> {
                val input = SpaceDefenderInput(joystickDx = joystickInput.dx)
                val result = controller.step(gameState, config, dt, input)
                gameState = result.state

                for (event in result.events) {
                    when (event) {
                        is SpaceDefenderEvent.AllEnemiesDestroyed -> {
                            waveCompleteTimer = 2.0f
                            machine.allEnemiesDestroyed()
                        }
                        is SpaceDefenderEvent.GameOver -> {
                            if (gameState.score > bestScore) bestScore = gameState.score
                            machine.playerDied()
                        }
                        else -> Unit
                    }
                }
            }
            else -> Unit
        }
    }

    GameScaffold(
        title = "Space Defender",
        onExit = onExit,
        hud = {
            val livesStr = "♥".repeat(gameState.lives.coerceAtLeast(0)) + "♡".repeat((3 - gameState.lives).coerceAtLeast(0))
            GameHud(
                left = "Score: ${gameState.score}",
                center = "Wave ${gameState.wave}",
                right = livesStr
            )
        },
        status = {
            when (phase) {
                SpaceDefenderPhase.GAME_OVER -> {
                    DefeatPanel(
                        score = gameState.score,
                        bestScore = bestScore,
                        onTryAgain = onExit,
                        onMenu = onExit,
                        headline = "Game Over"
                    )
                }
                else -> Unit
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Background
                drawRect(color = courtColor, size = Size(w, h))
                drawStarfield(w, h, neutralColor)

                // Shields
                for (shield in gameState.shields) {
                    if (shield.health != ShieldHealth.BROKEN) {
                        drawShield(shield, w, h, shieldColor)
                    }
                }

                // Enemies
                for (enemy in gameState.enemies) {
                    drawEnemy(enemy, w, h, enemyColor, accentColor)
                }

                // Projectiles
                for (proj in gameState.projectiles) {
                    val color = if (proj.owner == ProjectileOwner.PLAYER) projectilePlayerColor else projectileEnemyColor
                    drawProjectile(proj, w, h, color)
                }

                // Player cannon
                val invincible = gameState.isInvincible
                drawPlayerCannon(
                    x = gameState.playerX * w,
                    y = SpaceDefenderState.PLAYER_Y * h,
                    w = w,
                    color = if (invincible) playerColor.copy(alpha = 0.5f) else playerColor,
                    accentColor = accentColor
                )
            }

            // Wave intro overlay (shows wave number briefly, non-blocking)
            if (phase == SpaceDefenderPhase.WAVE_INTRO) {
                Text(
                    text = "Wave ${gameState.wave}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = (waveIntroCountdown / config.waveIntroDuration).coerceIn(0f, 1f)),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Joystick — lower-left
            VirtualJoystick(
                onInput = { joystickInput = it },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Canvas draw helpers (pure DrawScope — no @Composable, no MaterialTheme access)
// ---------------------------------------------------------------------------

private fun DrawScope.drawStarfield(w: Float, h: Float, starColor: Color) {
    // Deterministic pseudo-random stars
    var seed = 42L
    repeat(60) {
        seed = seed * 6364136223846793005L + 1442695040888963407L
        val x = ((seed ushr 33).toFloat() / Int.MAX_VALUE.toFloat()).coerceIn(0f, 1f) * w
        seed = seed * 6364136223846793005L + 1442695040888963407L
        val y = ((seed ushr 33).toFloat() / Int.MAX_VALUE.toFloat()).coerceIn(0f, 1f) * h
        drawCircle(color = starColor.copy(alpha = 0.4f), radius = 1.5f, center = Offset(x, y))
    }
}

private fun DrawScope.drawShield(shield: Shield, w: Float, h: Float, baseColor: Color) {
    val cx = shield.position.x * w
    val cy = shield.position.y * h
    val halfW = 0.06f * w
    val halfH = 0.025f * h
    val alpha = when (shield.health) {
        ShieldHealth.FULL -> 0.85f
        ShieldHealth.CRACKED -> 0.45f
        ShieldHealth.BROKEN -> 0f
    }
    drawRoundRect(
        color = baseColor.copy(alpha = alpha),
        topLeft = Offset(cx - halfW, cy - halfH),
        size = Size(halfW * 2f, halfH * 2f),
        cornerRadius = CornerRadius(halfH, halfH)
    )
    if (shield.health == ShieldHealth.CRACKED) {
        // Draw crack lines
        drawLine(baseColor.copy(alpha = 0.3f), Offset(cx - halfW * 0.3f, cy - halfH), Offset(cx, cy + halfH), strokeWidth = 2f)
        drawLine(baseColor.copy(alpha = 0.3f), Offset(cx + halfW * 0.3f, cy - halfH), Offset(cx - halfW * 0.2f, cy + halfH), strokeWidth = 2f)
    }
}

private fun DrawScope.drawEnemy(enemy: Enemy, w: Float, h: Float, enemyColor: Color, accentColor: Color) {
    val cx = enemy.position.x * w
    val cy = enemy.position.y * h
    val radius = 0.04f * w.coerceAtMost(h)
    val hpFrac = enemy.hp.toFloat() / enemy.type.maxHp.toFloat()

    // Body shape differs by type
    when (enemy.type) {
        EnemyType.SCOUT -> {
            // Diamond shape
            val pts = arrayOf(
                Offset(cx, cy - radius),
                Offset(cx + radius * 0.7f, cy),
                Offset(cx, cy + radius * 0.5f),
                Offset(cx - radius * 0.7f, cy)
            )
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    pts.forEach { lineTo(it.x, it.y) }
                    close()
                },
                color = enemyColor.copy(alpha = 0.6f + hpFrac * 0.4f)
            )
        }
        EnemyType.GUNNER -> {
            // Wide hexagon
            drawCircle(color = enemyColor.copy(alpha = 0.7f), radius = radius * 1.1f, center = Offset(cx, cy))
            drawCircle(color = accentColor.copy(alpha = 0.3f), radius = radius * 0.5f, center = Offset(cx, cy))
        }
        EnemyType.SNIPER -> {
            // Elongated shape
            drawOval(
                color = enemyColor.copy(alpha = 0.8f),
                topLeft = Offset(cx - radius * 0.6f, cy - radius * 1.3f),
                size = Size(radius * 1.2f, radius * 2.6f)
            )
        }
        EnemyType.BOMBER -> {
            // Large rounded square
            drawRoundRect(
                color = enemyColor.copy(alpha = 0.75f),
                topLeft = Offset(cx - radius * 1.1f, cy - radius * 0.9f),
                size = Size(radius * 2.2f, radius * 1.8f),
                cornerRadius = CornerRadius(radius * 0.3f)
            )
            // Wing indicators
            drawLine(accentColor.copy(alpha = 0.5f), Offset(cx - radius * 1.5f, cy), Offset(cx - radius * 1.1f, cy), strokeWidth = 3f)
            drawLine(accentColor.copy(alpha = 0.5f), Offset(cx + radius * 1.1f, cy), Offset(cx + radius * 1.5f, cy), strokeWidth = 3f)
        }
        EnemyType.COMMANDER -> {
            // Large complex shape
            drawCircle(color = enemyColor.copy(alpha = 0.9f), radius = radius * 1.4f, center = Offset(cx, cy))
            drawCircle(color = accentColor.copy(alpha = 0.4f), radius = radius * 0.8f, center = Offset(cx, cy), style = Stroke(width = 3f))
            drawCircle(color = accentColor.copy(alpha = 0.6f), radius = radius * 0.3f, center = Offset(cx, cy))
        }
    }

    // HP bar above enemy
    val barW = radius * 2.2f
    val barH = 4f
    drawRect(
        color = Color.DarkGray.copy(alpha = 0.7f),
        topLeft = Offset(cx - barW / 2f, cy - radius * 1.6f),
        size = Size(barW, barH)
    )
    drawRect(
        color = if (hpFrac > 0.5f) accentColor.copy(alpha = 0.9f) else enemyColor,
        topLeft = Offset(cx - barW / 2f, cy - radius * 1.6f),
        size = Size(barW * hpFrac, barH)
    )
}

private fun DrawScope.drawProjectile(proj: Projectile, w: Float, h: Float, color: Color) {
    val cx = proj.position.x * w
    val cy = proj.position.y * h
    if (proj.owner == ProjectileOwner.PLAYER) {
        // Thin neon beam
        drawLine(color, Offset(cx, cy + 0.02f * h), Offset(cx, cy - 0.025f * h), strokeWidth = 4f)
        drawLine(color.copy(alpha = 0.4f), Offset(cx, cy + 0.02f * h), Offset(cx, cy - 0.025f * h), strokeWidth = 8f)
    } else {
        // Enemy projectile — small circle
        drawCircle(color, radius = 0.012f * w.coerceAtMost(h), center = Offset(cx, cy))
        drawCircle(color.copy(alpha = 0.3f), radius = 0.02f * w.coerceAtMost(h), center = Offset(cx, cy))
    }
}

private fun DrawScope.drawPlayerCannon(x: Float, y: Float, w: Float, color: Color, accentColor: Color) {
    val baseW = w * 0.08f
    val baseH = w * 0.03f
    val barrelW = w * 0.012f
    val barrelH = w * 0.04f

    // Hovering platform base
    drawRoundRect(
        color = color,
        topLeft = Offset(x - baseW / 2f, y - baseH / 2f),
        size = Size(baseW, baseH),
        cornerRadius = CornerRadius(baseH / 2f)
    )
    // Barrel
    drawRect(
        color = accentColor,
        topLeft = Offset(x - barrelW / 2f, y - baseH / 2f - barrelH),
        size = Size(barrelW, barrelH)
    )
    // Glow on barrel tip
    drawCircle(
        color = accentColor.copy(alpha = 0.6f),
        radius = barrelW * 1.5f,
        center = Offset(x, y - baseH / 2f - barrelH)
    )
}
