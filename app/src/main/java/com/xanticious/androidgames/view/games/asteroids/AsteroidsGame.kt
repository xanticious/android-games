package com.xanticious.androidgames.view.games.asteroids

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.asteroids.AsteroidsController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.JoystickInput
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.asteroids.Asteroid
import com.xanticious.androidgames.model.games.asteroids.AsteroidsInput
import com.xanticious.androidgames.model.games.asteroids.AsteroidsState
import com.xanticious.androidgames.model.games.asteroids.AsteroidsStepEvent
import com.xanticious.androidgames.model.games.asteroids.Beacon
import com.xanticious.androidgames.model.games.asteroids.Projectile
import com.xanticious.androidgames.model.games.asteroids.Ship
import com.xanticious.androidgames.state.games.asteroids.AsteroidsPhase
import com.xanticious.androidgames.state.games.asteroids.AsteroidsStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VirtualJoystick
import kotlinx.coroutines.delay
import kotlin.math.PI

/**
 * Asteroids — entry composable.
 *
 * Left-thumb [VirtualJoystick] rotates/thrusts the ship; tap the right half of
 * the board to fire within a ±20° cone of the ship's heading (Variant A,
 * design/common/tap-targeting.md). Collect 5 beacons per level to advance.
 */
@Composable
fun AsteroidsGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { AsteroidsController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { AsteroidsStateMachine() }
    val phase by machine.phase.collectAsState()

    var state by remember { mutableStateOf(AsteroidsState.initial()) }
    var joystick by remember { mutableStateOf(JoystickInput.NONE) }
    var pendingTap by remember { mutableStateOf<Vec2?>(null) }

    // ── Phase-change side-effects ───────────────────────────────────────────
    LaunchedEffect(Unit) { machine.startGame() }

    LaunchedEffect(phase) {
        when (phase) {
            AsteroidsPhase.SPAWNING -> {
                state = controller.spawnAsteroids(state, config)
                machine.fieldReady()
            }
            AsteroidsPhase.RESPAWNING -> {
                delay(2_000L)
                state = controller.respawnShip(state, config)
                machine.respawnComplete()
            }
            AsteroidsPhase.LEVEL_COMPLETE -> {
                delay(2_500L)
                state = controller.advanceLevel(state)
                machine.nextLevel()
            }
            else -> Unit
        }
    }

    // ── Game loop ───────────────────────────────────────────────────────────
    GameLoop(running = phase == AsteroidsPhase.PLAYING) { dt ->
        val tap = pendingTap
        pendingTap = null
        val step = controller.step(state, config, dt, AsteroidsInput(joystick, tap))
        state = step.state
        when (step.event) {
            AsteroidsStepEvent.PLAYER_HIT -> {
                if (state.lives <= 0) machine.playerDied()
                else machine.playerHitWithLives()
            }
            AsteroidsStepEvent.ALL_BEACONS_COLLECTED -> {
                // Level complete bonus
                state = state.copy(score = state.score + 1000 * state.level)
                machine.allBeaconsCollected()
            }
            AsteroidsStepEvent.BEACON_COLLECTED, AsteroidsStepEvent.NONE -> Unit
        }
    }

    // ── Capture theme colors before entering DrawScope ──────────────────────
    val levelCompleteColor = MaterialTheme.colorScheme.primary

    // ── UI ──────────────────────────────────────────────────────────────────
    GameScaffold(
        title = "Asteroids",
        onExit = onExit,
        hud = {
            GameHud(
                left = livesLabel(state.lives),
                center = "${state.score}",
                right = beaconTrackerLabel(state.beaconsCollectedThisLevel)
            )
        },
        status = {
            when (phase) {
                AsteroidsPhase.GAME_OVER -> {
                    DefeatPanel(
                        score = state.score,
                        bestScore = state.score,
                        onTryAgain = {
                            state = AsteroidsState.initial()
                            machine.retry()
                        },
                        onMenu = onExit
                    )
                }
                AsteroidsPhase.LEVEL_COMPLETE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Level ${state.level} Complete!  +${1000 * state.level} pts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = levelCompleteColor
                        )
                    }
                }
                else -> Unit
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ── Board canvas ──────────────────────────────────────────────────
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            // Only right half fires
                            if (offset.x / size.width > 0.5f) {
                                pendingTap = Vec2(
                                    offset.x / size.width,
                                    offset.y / size.height
                                )
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val scale = minOf(w, h)

                // Background
                drawRect(GameCourt, size = Size(w, h))

                // Subtle star field (deterministic, no random per-frame)
                drawStars(w, h)

                // Beacon
                state.beacon?.let { beacon ->
                    drawBeacon(beacon, w, h, scale)
                }

                // Asteroids
                for (asteroid in state.asteroids) {
                    drawAsteroid(asteroid, w, h, scale)
                }

                // Projectiles
                for (p in state.projectiles) {
                    drawCircle(
                        color = GamePlayer,
                        radius = Projectile.RADIUS * scale,
                        center = Offset(p.position.x * w, p.position.y * h)
                    )
                }

                // Ship (blink when invincible)
                val isVisible = when {
                    phase == AsteroidsPhase.PLAYING || phase == AsteroidsPhase.RESPAWNING -> {
                        !state.ship.isInvincible ||
                            (state.ship.invincibilityTimer * 6).toInt() % 2 == 0
                    }
                    else -> false
                }
                if (isVisible) {
                    drawShip(state.ship, w, h, scale)
                }
            }

            // ── Joystick overlay (lower-left) ─────────────────────────────────
            if (phase == AsteroidsPhase.PLAYING || phase == AsteroidsPhase.RESPAWNING) {
                VirtualJoystick(
                    onInput = { joystick = it },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }
        }
    }
}

// ── DrawScope helpers ────────────────────────────────────────────────────────

private fun DrawScope.drawStars(w: Float, h: Float) {
    // Fixed star positions (seeded pattern, no Random per frame)
    val positions = listOf(
        0.07f to 0.12f, 0.23f to 0.45f, 0.41f to 0.08f, 0.65f to 0.31f,
        0.87f to 0.18f, 0.15f to 0.72f, 0.52f to 0.63f, 0.78f to 0.55f,
        0.33f to 0.89f, 0.90f to 0.77f, 0.05f to 0.53f, 0.60f to 0.92f,
        0.44f to 0.37f, 0.72f to 0.06f, 0.28f to 0.21f, 0.95f to 0.44f
    )
    for ((sx, sy) in positions) {
        drawCircle(
            color = GameNeutral.copy(alpha = 0.35f),
            radius = 2.5f,
            center = Offset(sx * w, sy * h)
        )
    }
}

private fun DrawScope.drawBeacon(beacon: Beacon, w: Float, h: Float, scale: Float) {
    val cx = beacon.position.x * w
    val cy = beacon.position.y * h
    val r = Beacon.RADIUS * scale
    drawCircle(GameAccent.copy(alpha = 0.25f), r * 2f, Offset(cx, cy))
    drawCircle(GameAccent.copy(alpha = 0.55f), r * 1.3f, Offset(cx, cy))
    drawCircle(GameAccent, r, Offset(cx, cy))
}

private fun DrawScope.drawAsteroid(asteroid: Asteroid, w: Float, h: Float, scale: Float) {
    val cx = asteroid.position.x * w
    val cy = asteroid.position.y * h
    val r = Asteroid.radiusFor(asteroid.size) * scale
    val color = GameNeutral
    drawCircle(color = color.copy(alpha = 0.25f), radius = r, center = Offset(cx, cy))
    drawCircle(color = color, radius = r, center = Offset(cx, cy), style = Stroke(width = 3f))
    // HP cracks: inner ring per remaining HP below max
    val maxHp = Asteroid.hpFor(asteroid.size)
    if (asteroid.hp < maxHp) {
        drawCircle(
            color = GameHazard.copy(alpha = 0.6f),
            radius = r * 0.55f,
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )
    }
}

private fun DrawScope.drawShip(ship: Ship, w: Float, h: Float, scale: Float) {
    val forward = Vec2.fromAngle(ship.angle)
    val side = Vec2.fromAngle(ship.angle + (PI / 2).toFloat())
    val size = Ship.RADIUS * scale

    val cx = ship.position.x * w
    val cy = ship.position.y * h

    val tip = Offset(cx + forward.x * size, cy + forward.y * size)
    val leftWing = Offset(
        cx - forward.x * size * 0.55f + side.x * size * 0.75f,
        cy - forward.y * size * 0.55f + side.y * size * 0.75f
    )
    val rightWing = Offset(
        cx - forward.x * size * 0.55f - side.x * size * 0.75f,
        cy - forward.y * size * 0.55f - side.y * size * 0.75f
    )
    val tail = Offset(
        cx - forward.x * size * 0.3f,
        cy - forward.y * size * 0.3f
    )

    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(leftWing.x, leftWing.y)
        lineTo(tail.x, tail.y)
        lineTo(rightWing.x, rightWing.y)
        close()
    }
    drawPath(path, GamePlayer, style = Stroke(width = 3f))

    // Engine glow when thrusting
    if (ship.velocity.length > 0.04f) {
        val exhaustCenter = Offset(
            cx - forward.x * size * 0.4f,
            cy - forward.y * size * 0.4f
        )
        drawCircle(GameSuccess.copy(alpha = 0.7f), size * 0.25f, exhaustCenter)
    }
}

// ── HUD label helpers ────────────────────────────────────────────────────────

private fun livesLabel(lives: Int): String =
    "♦ ".repeat(lives.coerceIn(0, 3)).trimEnd()

private fun beaconTrackerLabel(collected: Int): String {
    val filled = "●".repeat(collected.coerceIn(0, AsteroidsState.BEACONS_PER_LEVEL))
    val empty = "○".repeat((AsteroidsState.BEACONS_PER_LEVEL - collected).coerceAtLeast(0))
    return filled + empty
}
