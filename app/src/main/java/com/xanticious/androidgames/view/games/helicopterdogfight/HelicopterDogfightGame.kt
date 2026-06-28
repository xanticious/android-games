package com.xanticious.androidgames.view.games.helicopterdogfight

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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.helicopterdogfight.HelicopterDogfightController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.JoystickInput
import com.xanticious.androidgames.model.games.helicopterdogfight.HeliEnemy
import com.xanticious.androidgames.model.games.helicopterdogfight.HeliEnemyType
import com.xanticious.androidgames.model.games.helicopterdogfight.HeliProjectile
import com.xanticious.androidgames.model.games.helicopterdogfight.HeliProjectileOwner
import com.xanticious.androidgames.model.games.helicopterdogfight.HelicopterDogfightEvent
import com.xanticious.androidgames.model.games.helicopterdogfight.HelicopterDogfightInput
import com.xanticious.androidgames.model.games.helicopterdogfight.HelicopterDogfightState
import com.xanticious.androidgames.state.games.helicopterdogfight.HelicopterDogfightPhase
import com.xanticious.androidgames.state.games.helicopterdogfight.HelicopterDogfightStateMachine
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.Dark2
import com.xanticious.androidgames.ui.theme.GameAccent
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
 * Helicopter Dogfight — side-scrolling combat helicopter game.
 * The player pilots a heli that auto-fires rightward and must destroy all enemies
 * per screen before the game advances. VirtualJoystick on the right controls altitude
 * and horizontal drift.
 */
@Composable
fun HelicopterDogfightGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { HelicopterDogfightController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { HelicopterDogfightStateMachine() }
    val phase by machine.phase.collectAsState()

    var gameState by remember { mutableStateOf(HelicopterDogfightState.initial()) }
    var joystick by remember { mutableStateOf(JoystickInput.NONE) }
    var allClearTimer by remember { mutableFloatStateOf(0f) }
    var scrollTimer by remember { mutableFloatStateOf(0f) }
    var respawnTimer by remember { mutableFloatStateOf(0f) }
    var bestScore by remember { mutableIntStateOf(0) }
    var rotorAngle by remember { mutableFloatStateOf(0f) }

    // Capture color tokens outside the Canvas lambda (DrawScope is not @Composable).
    val playerColor = GamePlayer
    val enemyColor = GameEnemy
    val accentColor = GameAccent
    val hazardColor = GameHazard
    val neutralColor = GameNeutral
    val successColor = GameSuccess
    val skyFarColor = Dark0
    val skyMidColor = Dark1
    val skyNearColor = Dark2

    LaunchedEffect(Unit) {
        val first = controller.startFirstScreen(HelicopterDogfightState.initial(), config)
        gameState = first
        machine.startGame()
        machine.screenReady()
    }

    LaunchedEffect(phase) {
        when (phase) {
            HelicopterDogfightPhase.SCREEN_CLEAR -> allClearTimer = config.allClearDuration
            HelicopterDogfightPhase.SCROLLING -> scrollTimer = config.scrollDuration
            HelicopterDogfightPhase.RESPAWNING -> respawnTimer = config.respawnDuration
            else -> Unit
        }
    }

    GameLoop(
        running = phase == HelicopterDogfightPhase.PLAYING ||
            phase == HelicopterDogfightPhase.SCREEN_CLEAR ||
            phase == HelicopterDogfightPhase.SCROLLING ||
            phase == HelicopterDogfightPhase.RESPAWNING
    ) { dt ->
        // Advance rotor animation regardless of phase.
        rotorAngle = (rotorAngle + dt * 720f) % 360f

        when (phase) {
            HelicopterDogfightPhase.PLAYING -> {
                val input = HelicopterDogfightInput(
                    joystickDx = joystick.dx,
                    joystickDy = joystick.dy
                )
                val step = controller.step(gameState, config, dt, input)
                gameState = step.state

                for (event in step.events) {
                    when (event) {
                        is HelicopterDogfightEvent.AllEnemiesDestroyed -> machine.allEnemiesDestroyed()
                        is HelicopterDogfightEvent.PlayerCrashed -> machine.playerCrashed()
                        is HelicopterDogfightEvent.GameOver -> {
                            if (gameState.score > bestScore) bestScore = gameState.score
                            machine.playerDied()
                        }
                        else -> Unit
                    }
                }
            }

            HelicopterDogfightPhase.SCREEN_CLEAR -> {
                allClearTimer -= dt
                if (allClearTimer <= 0f) machine.allClearDelayOver()
            }

            HelicopterDogfightPhase.SCROLLING -> {
                scrollTimer -= dt
                if (scrollTimer <= 0f) {
                    val next = controller.startNextScreen(gameState, config)
                    gameState = next
                    machine.nextScreenReady()
                }
            }

            HelicopterDogfightPhase.RESPAWNING -> {
                respawnTimer -= dt
                if (respawnTimer <= 0f) {
                    gameState = controller.respawnPlayer(gameState, config)
                    machine.respawnComplete()
                }
            }

            else -> Unit
        }
    }

    GameScaffold(
        title = "Helicopter Dogfight",
        onExit = onExit,
        hud = {
            val hpBar = "█".repeat(gameState.playerHp.coerceAtLeast(0)) +
                "░".repeat((5 - gameState.playerHp).coerceAtLeast(0))
            GameHud(
                left = "HP $hpBar  ✈${gameState.playerLives}",
                center = "Score: ${gameState.score}",
                right = "Screen ${gameState.screenNumber}"
            )
        },
        status = {
            when (phase) {
                HelicopterDogfightPhase.SCREEN_CLEAR -> {
                    Text(
                        text = "✓ All Clear!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = successColor,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                HelicopterDogfightPhase.GAME_OVER -> {
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

                // Parallax background layers
                drawBackground(w, h, skyFarColor, skyMidColor, skyNearColor, neutralColor)

                // Ground line at bottom
                drawRect(
                    color = neutralColor.copy(alpha = 0.5f),
                    topLeft = Offset(0f, h * 0.90f),
                    size = Size(w, h * 0.10f)
                )

                // Enemy projectiles
                for (proj in gameState.projectiles.filter { it.owner == HeliProjectileOwner.ENEMY }) {
                    drawEnemyProjectile(proj, w, h, hazardColor)
                }

                // Player projectiles
                for (proj in gameState.projectiles.filter { it.owner == HeliProjectileOwner.PLAYER }) {
                    drawPlayerProjectile(proj, w, h, accentColor)
                }

                // Enemies
                for (enemy in gameState.enemies) {
                    drawEnemy(enemy, w, h, enemyColor, hazardColor, accentColor)
                }

                // Player helicopter (flashes when invincible)
                val visible = !gameState.isInvincible ||
                    (rotorAngle.toInt() / 45) % 2 == 0
                if (visible) {
                    drawPlayerHeli(
                        x = gameState.playerPos.x * w,
                        y = gameState.playerPos.y * h,
                        rotorAngle = rotorAngle,
                        w = w,
                        color = playerColor,
                        accentColor = accentColor
                    )
                }
            }

            VirtualJoystick(
                onInput = { joystick = it },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Canvas draw helpers (pure DrawScope — no @Composable, no MaterialTheme access)
// ---------------------------------------------------------------------------

private fun DrawScope.drawBackground(
    w: Float, h: Float,
    skyFar: Color, skyMid: Color, skyNear: Color, ground: Color
) {
    // Gradient sky layers
    drawRect(color = skyFar, size = Size(w, h * 0.45f))
    drawRect(color = skyMid, topLeft = Offset(0f, h * 0.45f), size = Size(w, h * 0.30f))
    drawRect(color = skyNear, topLeft = Offset(0f, h * 0.75f), size = Size(w, h * 0.25f))

    // Distant mountains (silhouette)
    val mountainPath = Path().apply {
        moveTo(0f, h * 0.55f)
        lineTo(w * 0.12f, h * 0.35f)
        lineTo(w * 0.22f, h * 0.48f)
        lineTo(w * 0.35f, h * 0.28f)
        lineTo(w * 0.50f, h * 0.44f)
        lineTo(w * 0.62f, h * 0.32f)
        lineTo(w * 0.78f, h * 0.46f)
        lineTo(w * 0.88f, h * 0.30f)
        lineTo(w, h * 0.42f)
        lineTo(w, h * 0.55f)
        close()
    }
    drawPath(mountainPath, color = ground.copy(alpha = 0.18f))
}

private fun DrawScope.drawPlayerProjectile(proj: HeliProjectile, w: Float, h: Float, color: Color) {
    val cx = proj.position.x * w
    val cy = proj.position.y * h
    val len = w * 0.028f
    drawLine(color, Offset(cx - len, cy), Offset(cx + len * 0.3f, cy), strokeWidth = 4f)
    drawLine(color.copy(alpha = 0.35f), Offset(cx - len, cy), Offset(cx + len * 0.3f, cy), strokeWidth = 9f)
}

private fun DrawScope.drawEnemyProjectile(proj: HeliProjectile, w: Float, h: Float, color: Color) {
    val cx = proj.position.x * w
    val cy = proj.position.y * h
    val r = w.coerceAtMost(h) * 0.013f
    drawCircle(color, radius = r, center = Offset(cx, cy))
    drawCircle(color.copy(alpha = 0.30f), radius = r * 2f, center = Offset(cx, cy))
}

private fun DrawScope.drawEnemy(
    enemy: HeliEnemy,
    w: Float,
    h: Float,
    baseColor: Color,
    hazardColor: Color,
    accentColor: Color
) {
    val cx = enemy.position.x * w
    val cy = enemy.position.y * h
    val unit = w.coerceAtMost(h) * 0.038f
    val hpFrac = enemy.hp.toFloat() / enemy.type.maxHp.toFloat()

    when (enemy.type) {
        HeliEnemyType.GROUND_TURRET -> {
            // Base platform
            drawRect(
                color = baseColor.copy(alpha = 0.8f),
                topLeft = Offset(cx - unit * 1.4f, cy + unit * 0.2f),
                size = Size(unit * 2.8f, unit * 0.8f)
            )
            // Barrel pointing left
            drawRect(
                color = hazardColor.copy(alpha = 0.9f),
                topLeft = Offset(cx - unit * 1.8f, cy - unit * 0.2f),
                size = Size(unit * 1.8f, unit * 0.4f)
            )
            // Turret body
            drawCircle(
                color = baseColor,
                radius = unit * 0.7f,
                center = Offset(cx, cy)
            )
        }
        HeliEnemyType.AA_GUN -> {
            // Thin tall barrel
            drawRect(
                color = hazardColor.copy(alpha = 0.9f),
                topLeft = Offset(cx - unit * 0.2f, cy - unit * 1.6f),
                size = Size(unit * 0.4f, unit * 1.6f)
            )
            // Mount
            drawRect(
                color = baseColor.copy(alpha = 0.85f),
                topLeft = Offset(cx - unit * 0.8f, cy),
                size = Size(unit * 1.6f, unit * 0.7f)
            )
            // Angled aiming arm
            drawLine(
                hazardColor.copy(alpha = 0.6f),
                Offset(cx - unit * 1.5f, cy - unit * 0.5f),
                Offset(cx, cy - unit * 1.6f),
                strokeWidth = 3f
            )
        }
        HeliEnemyType.ENEMY_HELI -> {
            // Fuselage
            drawOval(
                color = baseColor.copy(alpha = 0.85f),
                topLeft = Offset(cx - unit * 1.5f, cy - unit * 0.45f),
                size = Size(unit * 3.0f, unit * 0.9f)
            )
            // Tail boom
            drawLine(
                baseColor.copy(alpha = 0.7f),
                Offset(cx + unit * 1.2f, cy),
                Offset(cx + unit * 2.2f, cy - unit * 0.3f),
                strokeWidth = 5f
            )
            // Rotor (static representation — enemy helis don't rotate for simplicity)
            drawLine(
                accentColor.copy(alpha = 0.7f),
                Offset(cx - unit * 1.3f, cy - unit * 0.55f),
                Offset(cx + unit * 1.3f, cy - unit * 0.55f),
                strokeWidth = 3f
            )
            // Gun barrel pointing left
            drawLine(
                hazardColor.copy(alpha = 0.9f),
                Offset(cx - unit * 1.5f, cy + unit * 0.1f),
                Offset(cx - unit * 2.2f, cy + unit * 0.1f),
                strokeWidth = 4f
            )
        }
    }

    // HP bar above enemy
    val barW = unit * 2.8f
    val barH = 5f
    drawRect(
        color = Color.DarkGray.copy(alpha = 0.6f),
        topLeft = Offset(cx - barW / 2f, cy - unit * 1.8f),
        size = Size(barW, barH)
    )
    drawRect(
        color = if (hpFrac > 0.5f) accentColor.copy(alpha = 0.9f) else hazardColor.copy(alpha = 0.9f),
        topLeft = Offset(cx - barW / 2f, cy - unit * 1.8f),
        size = Size(barW * hpFrac, barH)
    )
}

private fun DrawScope.drawPlayerHeli(
    x: Float, y: Float,
    rotorAngle: Float,
    w: Float,
    color: Color,
    accentColor: Color
) {
    val unit = w * 0.038f

    // Fuselage — elongated body facing right
    drawOval(
        color = color,
        topLeft = Offset(x - unit * 1.8f, y - unit * 0.5f),
        size = Size(unit * 3.6f, unit * 1.0f)
    )

    // Cockpit bubble
    drawOval(
        color = accentColor.copy(alpha = 0.35f),
        topLeft = Offset(x + unit * 0.4f, y - unit * 0.55f),
        size = Size(unit * 1.0f, unit * 0.85f)
    )

    // Tail boom
    drawLine(
        color.copy(alpha = 0.8f),
        Offset(x - unit * 1.6f, y + unit * 0.1f),
        Offset(x - unit * 2.8f, y - unit * 0.5f),
        strokeWidth = 5f
    )

    // Tail rotor (small)
    drawLine(
        color.copy(alpha = 0.7f),
        Offset(x - unit * 2.8f, y - unit * 1.0f),
        Offset(x - unit * 2.8f, y),
        strokeWidth = 3f
    )

    // Main rotor blades (rotates)
    val rotRad = Math.toRadians(rotorAngle.toDouble()).toFloat()
    val bladeLen = unit * 2.2f
    for (i in 0..1) {
        val angle = rotRad + i * (Math.PI / 2).toFloat()
        val bx = kotlin.math.cos(angle) * bladeLen
        val by = kotlin.math.sin(angle) * bladeLen * 0.25f
        drawLine(
            color.copy(alpha = 0.75f),
            Offset(x - bx, y - unit * 0.6f - by),
            Offset(x + bx, y - unit * 0.6f + by),
            strokeWidth = 4f
        )
    }

    // Gun barrel pointing right
    drawLine(
        accentColor,
        Offset(x + unit * 1.6f, y + unit * 0.15f),
        Offset(x + unit * 2.4f, y + unit * 0.15f),
        strokeWidth = 5f
    )

    // Muzzle flash (tiny accent dot at barrel tip)
    drawCircle(
        accentColor.copy(alpha = 0.6f),
        radius = unit * 0.25f,
        center = Offset(x + unit * 2.4f, y + unit * 0.15f)
    )
}
