package com.xanticious.androidgames.view.games.asteroids

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.asteroids.AsteroidsController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.JoystickInput
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.asteroids.Asteroid
import com.xanticious.androidgames.model.games.asteroids.AsteroidsInput
import com.xanticious.androidgames.model.games.asteroids.AsteroidsMode
import com.xanticious.androidgames.model.games.asteroids.AsteroidsState
import com.xanticious.androidgames.model.games.asteroids.AsteroidsStepEvent
import com.xanticious.androidgames.model.games.asteroids.Beacon
import com.xanticious.androidgames.model.games.asteroids.KnobPlacement
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
import com.xanticious.androidgames.view.common.FloatingJoystick
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VirtualJoystick
import kotlinx.coroutines.delay
import kotlin.math.PI

/**
 * Asteroids — entry composable.
 *
 * A 360° analog knob accelerates the ship in the knob's direction; the ship
 * auto-aligns to its velocity and is always drifting (clamped between a small
 * min speed and a max speed). Firing is automatic, always toward the nearest
 * asteroid. The knob can be left-fixed, right-fixed, or floating. Taking damage
 * teleports the ship to a safe gap and freezes asteroids for a moment.
 */
@Composable
fun AsteroidsGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { AsteroidsController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { AsteroidsStateMachine() }
    val phase by machine.phase.collectAsState()

    var state by remember { mutableStateOf(AsteroidsState.initial()) }
    var joystick by remember { mutableStateOf(JoystickInput.NONE) }
    var knobPlacement by remember { mutableStateOf(KnobPlacement.LEFT_FIXED) }

    // ── Phase-change side-effects ───────────────────────────────────────────
    LaunchedEffect(Unit) { machine.startGame() }

    LaunchedEffect(phase) {
        when (phase) {
            AsteroidsPhase.SPAWNING -> {
                state = controller.spawnAsteroids(state, config)
                machine.fieldReady()
            }
            AsteroidsPhase.LEVEL_COMPLETE -> {
                delay(2_000L)
                val mode = state.mode
                if (mode is AsteroidsMode.LevelChallenge && state.level >= mode.targetLevels) {
                    machine.gameEnded()
                } else {
                    state = controller.advanceLevel(state)
                    machine.nextLevel()
                }
            }
            else -> Unit
        }
    }

    // ── Game loop ───────────────────────────────────────────────────────────
    GameLoop(running = phase == AsteroidsPhase.PLAYING) { dt ->
        val step = controller.step(state, config, dt, AsteroidsInput(joystick))
        state = step.state
        when (step.event) {
            AsteroidsStepEvent.PLAYER_HIT -> {
                if (!state.mode.infiniteLives && state.lives <= 0) machine.gameEnded()
            }
            AsteroidsStepEvent.ALL_BEACONS_COLLECTED -> {
                state = state.copy(score = state.score + 1000 * state.level)
                machine.allBeaconsCollected()
            }
            AsteroidsStepEvent.TIME_EXPIRED -> machine.gameEnded()
            AsteroidsStepEvent.BEACON_COLLECTED, AsteroidsStepEvent.NONE -> Unit
        }
    }

    // ── Capture theme colors before entering DrawScope ──────────────────────
    val levelCompleteColor = MaterialTheme.colorScheme.primary
    val showControls = phase == AsteroidsPhase.PLAYING

    // ── UI ──────────────────────────────────────────────────────────────────
    GameScaffold(
        title = "Asteroids",
        onExit = onExit,
        hud = {
            if (phase != AsteroidsPhase.SETUP && phase != AsteroidsPhase.IDLE) {
                GameHud(
                    left = hudLeft(state),
                    center = "${state.score}",
                    right = hudRight(state)
                )
            }
        },
        status = {
            when (phase) {
                AsteroidsPhase.GAME_OVER -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        gameOverDetail(state)?.let { detail ->
                            Text(
                                text = detail,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = levelCompleteColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        DefeatPanel(
                            score = state.score,
                            bestScore = state.score,
                            headline = gameOverHeadline(state),
                            onTryAgain = {
                                state = AsteroidsState.initial(state.mode)
                                machine.retry()
                            },
                            onMenu = onExit
                        )
                    }
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
            if (phase == AsteroidsPhase.SETUP) {
                AsteroidsSetup(
                    knobPlacement = knobPlacement,
                    onKnobPlacement = { knobPlacement = it },
                    onStart = { mode ->
                        state = AsteroidsState.initial(mode)
                        machine.confirmConfig()
                    }
                )
            } else {
                // ── Board canvas ──────────────────────────────────────────────
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val scale = minOf(w, h)

                    drawRect(GameCourt, size = Size(w, h))
                    drawStars(w, h)

                    state.beacon?.let { beacon -> drawBeacon(beacon, w, h, scale) }

                    for (asteroid in state.asteroids) {
                        drawAsteroid(asteroid, w, h, scale)
                    }

                    for (p in state.projectiles) {
                        drawCircle(
                            color = GamePlayer,
                            radius = Projectile.RADIUS * scale,
                            center = Offset(p.position.x * w, p.position.y * h)
                        )
                    }

                    val isVisible = when (phase) {
                        AsteroidsPhase.PLAYING ->
                            !state.ship.isInvincible ||
                                (state.ship.invincibilityTimer * 6).toInt() % 2 == 0
                        AsteroidsPhase.LEVEL_COMPLETE, AsteroidsPhase.GAME_OVER -> true
                        else -> false
                    }
                    if (isVisible) {
                        drawShip(state.ship, w, h, scale)
                    }
                }

                // ── Acceleration knob ─────────────────────────────────────────
                if (showControls) {
                    when (knobPlacement) {
                        KnobPlacement.LEFT_FIXED -> VirtualJoystick(
                            onInput = { joystick = it },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            ringDiameter = 240.dp,
                            knobDiameter = 100.dp
                        )
                        KnobPlacement.RIGHT_FIXED -> VirtualJoystick(
                            onInput = { joystick = it },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            ringDiameter = 240.dp,
                            knobDiameter = 100.dp
                        )
                        KnobPlacement.FLOATING -> FloatingJoystick(
                            onInput = { joystick = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

// ── Setup screen ─────────────────────────────────────────────────────────────

private val LEVEL_OPTIONS = listOf(5, 10, 15, 20)
private val TIME_OPTIONS_MIN = listOf(1, 3, 5, 10, 20)

@Composable
private fun AsteroidsSetup(
    knobPlacement: KnobPlacement,
    onKnobPlacement: (KnobPlacement) -> Unit,
    onStart: (AsteroidsMode) -> Unit
) {
    var modeKind by remember { mutableStateOf(0) } // 0 Classic, 1 Level, 2 Time
    var targetLevels by remember { mutableStateOf(10) }
    var durationMinutes by remember { mutableStateOf(5) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Knob Placement", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KnobPlacement.entries.forEach { placement ->
                FilterChip(
                    selected = knobPlacement == placement,
                    onClick = { onKnobPlacement(placement) },
                    label = { Text(placement.label()) }
                )
            }
        }

        Text("Game Mode", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Classic", "Level Challenge", "Time Challenge").forEachIndexed { index, label ->
                FilterChip(
                    selected = modeKind == index,
                    onClick = { modeKind = index },
                    label = { Text(label) }
                )
            }
        }

        when (modeKind) {
            1 -> {
                Text("Levels to complete")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LEVEL_OPTIONS) { n ->
                        FilterChip(
                            selected = targetLevels == n,
                            onClick = { targetLevels = n },
                            label = { Text("$n") }
                        )
                    }
                }
            }
            2 -> {
                Text("Time limit (minutes)")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TIME_OPTIONS_MIN) { m ->
                        FilterChip(
                            selected = durationMinutes == m,
                            onClick = { durationMinutes = m },
                            label = { Text("$m") }
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                val mode = when (modeKind) {
                    1 -> AsteroidsMode.LevelChallenge(targetLevels)
                    2 -> AsteroidsMode.TimeChallenge(durationMinutes * 60)
                    else -> AsteroidsMode.Classic
                }
                onStart(mode)
            },
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("Start Game")
        }
    }
}

private fun KnobPlacement.label(): String = when (this) {
    KnobPlacement.LEFT_FIXED -> "Left Thumb"
    KnobPlacement.RIGHT_FIXED -> "Right Thumb"
    KnobPlacement.FLOATING -> "Floating"
}

// ── DrawScope helpers ────────────────────────────────────────────────────────

private fun DrawScope.drawStars(w: Float, h: Float) {
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

    if (ship.velocity.length > 0.04f) {
        val exhaustCenter = Offset(
            cx - forward.x * size * 0.4f,
            cy - forward.y * size * 0.4f
        )
        drawCircle(GameSuccess.copy(alpha = 0.7f), size * 0.25f, exhaustCenter)
    }
}

// ── HUD label helpers ────────────────────────────────────────────────────────

private fun formatTime(seconds: Float): String {
    val total = seconds.toInt().coerceAtLeast(0)
    val mm = total / 60
    val ss = total % 60
    return "%d:%02d".format(mm, ss)
}

private fun hudLeft(state: AsteroidsState): String = when (val mode = state.mode) {
    AsteroidsMode.Classic -> livesLabel(state.lives)
    is AsteroidsMode.LevelChallenge ->
        "⏱ ${formatTime(state.elapsedTime)}  Lv ${state.level}/${mode.targetLevels}"
    is AsteroidsMode.TimeChallenge ->
        "⏱ ${formatTime(mode.durationSeconds - state.elapsedTime)}"
}

private fun hudRight(state: AsteroidsState): String = when (state.mode) {
    is AsteroidsMode.TimeChallenge -> "☄ ${state.asteroidsDestroyed}"
    else -> beaconTrackerLabel(state.beaconsCollectedThisLevel)
}

private fun livesLabel(lives: Int): String =
    "♦ ".repeat(lives.coerceIn(0, 3)).trimEnd()

private fun beaconTrackerLabel(collected: Int): String {
    val filled = "●".repeat(collected.coerceIn(0, AsteroidsState.BEACONS_PER_LEVEL))
    val empty = "○".repeat((AsteroidsState.BEACONS_PER_LEVEL - collected).coerceAtLeast(0))
    return filled + empty
}

private fun gameOverHeadline(state: AsteroidsState): String = when (state.mode) {
    AsteroidsMode.Classic -> "Game Over"
    is AsteroidsMode.LevelChallenge -> "Challenge Complete!"
    is AsteroidsMode.TimeChallenge -> "Time's Up!"
}

private fun gameOverDetail(state: AsteroidsState): String? = when (val mode = state.mode) {
    AsteroidsMode.Classic -> "Reached level ${state.level}"
    is AsteroidsMode.LevelChallenge ->
        "${mode.targetLevels} levels in ${formatTime(state.elapsedTime)}"
    is AsteroidsMode.TimeChallenge -> "Destroyed ${state.asteroidsDestroyed} asteroids"
}
