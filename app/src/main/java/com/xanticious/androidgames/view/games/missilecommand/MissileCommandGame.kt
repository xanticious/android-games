package com.xanticious.androidgames.view.games.missilecommand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.missilecommand.MissileCommandController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.Vec2
import com.xanticious.androidgames.model.games.missilecommand.InterceptorPhase
import com.xanticious.androidgames.model.games.missilecommand.MissileCommandInput
import com.xanticious.androidgames.model.games.missilecommand.MissileCommandState
import com.xanticious.androidgames.model.games.missilecommand.Silo
import com.xanticious.androidgames.state.games.missilecommand.MissileCommandPhase
import com.xanticious.androidgames.state.games.missilecommand.MissileCommandStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlinx.coroutines.delay

/**
 * Missile Command — tap the sky to launch interceptors from silos; defend six cities
 * across escalating waves of incoming enemy missiles.
 *
 * Entry composable as required by the contract.
 */
@Composable
fun MissileCommandGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { MissileCommandController() }
    val config = remember(difficulty) { controller.configFor(difficulty) }
    val machine = remember { MissileCommandStateMachine() }
    val phase by machine.phase.collectAsState()

    var state by remember { mutableStateOf(MissileCommandState.initial()) }
    var pendingTap by remember { mutableStateOf<Vec2?>(null) }
    var playerWon by remember { mutableStateOf(false) }

    // Phase-driven side effects: spawn waves, handle transitions.
    LaunchedEffect(phase) {
        when (phase) {
            MissileCommandPhase.IDLE -> machine.gameStarted()

            MissileCommandPhase.WAVE_INTRO -> {
                state = controller.spawnWave(state, config)
                delay(1_500L)
                machine.waveBegun()
            }

            MissileCommandPhase.WAVE_TALLY -> {
                delay(2_000L)
                if (state.wave >= MissileCommandState.MAX_WAVE) {
                    playerWon = true
                    state = state.copy(playerWon = true)
                    machine.gameWon()
                } else {
                    state = state.copy(wave = state.wave + 1, waveCleared = false)
                    machine.tallyComplete()
                }
            }

            else -> Unit
        }
    }

    // Main game loop — only ticks while PLAYING.
    GameLoop(running = phase == MissileCommandPhase.PLAYING) { dt ->
        val tap = pendingTap
        pendingTap = null
        val step = controller.step(state, config, dt, MissileCommandInput(tap))
        state = step.state
        when {
            state.gameOver -> machine.allCitiesDestroyed()
            state.waveCleared -> machine.waveCleared()
        }
    }

    // Capture colors outside Canvas (DrawScope is not @Composable).
    val colorCourt = GameCourt
    val colorLine = GameCourtLine
    val colorPlayer = GamePlayer
    val colorEnemy = GameEnemy
    val colorHazard = GameHazard
    val colorAccent = GameAccent
    val colorNeutral = GameNeutral

    GameScaffold(
        title = "Missile Command",
        onExit = onExit,
        hud = {
            GameHud(
                left = "Score ${state.score}",
                center = "Wave ${state.wave}",
                right = "Cities ${state.citiesAlive}",
            )
        },
        status = {
            AmmoStrip(state = state, neutralColor = colorNeutral, accentColor = colorAccent)
            if (phase == MissileCommandPhase.GAME_OVER) {
                if (playerWon) {
                    VictoryPanel(
                        score = state.score,
                        bestScore = maxOf(state.score, state.bestScore),
                        stars = starsForScore(state.score, state.wave),
                        onReplay = {
                            playerWon = false
                            state = MissileCommandState.initial(bestScore = maxOf(state.score, state.bestScore))
                            machine.replay()
                        },
                        onMenu = onExit,
                        headline = "You Survived!",
                    )
                } else {
                    DefeatPanel(
                        score = state.score,
                        bestScore = maxOf(state.score, state.bestScore),
                        onTryAgain = {
                            state = MissileCommandState.initial(bestScore = maxOf(state.score, state.bestScore))
                            machine.replay()
                        },
                        onMenu = onExit,
                        headline = "Cities Destroyed",
                    )
                }
            }
        },
    ) {
        // Board: sky + missiles + interceptors + cities + silos.
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(phase) {
                        detectTapGestures { offset ->
                            if (phase == MissileCommandPhase.PLAYING) {
                                pendingTap = Vec2(
                                    offset.x / size.width.toFloat(),
                                    offset.y / size.height.toFloat(),
                                )
                            }
                        }
                    },
            ) {
                val w = size.width
                val h = size.height

                // Sky background.
                drawRect(color = colorCourt, size = Size(w, h))

                // Ground line.
                drawLine(
                    color = colorLine,
                    start = Offset(0f, MissileCommandState.GROUND_Y * h),
                    end = Offset(w, MissileCommandState.GROUND_Y * h),
                    strokeWidth = 2f,
                )

                // Cities.
                state.cities.forEach { city ->
                    val cx = city.pos.x * w
                    val cy = city.pos.y * h
                    if (city.alive) {
                        // Simple skyline block.
                        drawRect(
                            color = colorPlayer,
                            topLeft = Offset(cx - 12f, cy - 16f),
                            size = Size(24f, 16f),
                        )
                        drawRect(
                            color = colorPlayer,
                            topLeft = Offset(cx - 6f, cy - 24f),
                            size = Size(12f, 8f),
                        )
                    } else {
                        // Rubble.
                        drawRect(
                            color = colorNeutral.copy(alpha = 0.4f),
                            topLeft = Offset(cx - 12f, cy - 5f),
                            size = Size(24f, 5f),
                        )
                    }
                }

                // Silos.
                state.silos.forEach { silo ->
                    val sx = silo.pos.x * w
                    val sy = silo.pos.y * h
                    val color = if (silo.alive) colorPlayer else colorNeutral.copy(alpha = 0.4f)
                    // Silo base.
                    drawRect(
                        color = color,
                        topLeft = Offset(sx - 10f, sy - 14f),
                        size = Size(20f, 14f),
                    )
                    // Launch tube.
                    drawRect(
                        color = color,
                        topLeft = Offset(sx - 3f, sy - 22f),
                        size = Size(6f, 8f),
                    )
                }

                // Enemy missiles — draw trail line from origin to current pos, plus head dot.
                state.missiles.filter { it.alive }.forEach { missile ->
                    val ox = missile.originPos.x * w
                    val oy = missile.originPos.y * h
                    val mx = missile.pos.x * w
                    val my = missile.pos.y * h
                    drawLine(
                        color = colorEnemy.copy(alpha = 0.6f),
                        start = Offset(ox, oy),
                        end = Offset(mx, my),
                        strokeWidth = 1.5f,
                    )
                    drawCircle(color = colorHazard, radius = 4f, center = Offset(mx, my))
                }

                // Interceptors.
                val blastLifetime = MissileCommandState.MAX_BLAST_RADIUS / MissileCommandState.BLAST_EXPAND_SPEED +
                        MissileCommandState.BLAST_LINGER_SECONDS
                val refDim = minOf(w, h)

                state.interceptors.forEach { interceptor ->
                    val ix = interceptor.pos.x * w
                    val iy = interceptor.pos.y * h
                    when (interceptor.phase) {
                        InterceptorPhase.FLYING -> {
                            // Bright cyan dot in flight.
                            drawLine(
                                color = colorAccent.copy(alpha = 0.5f),
                                start = Offset(interceptor.launchPos.x * w, interceptor.launchPos.y * h),
                                end = Offset(ix, iy),
                                strokeWidth = 1f,
                            )
                            drawCircle(color = colorAccent, radius = 5f, center = Offset(ix, iy))
                        }
                        InterceptorPhase.EXPLODING -> {
                            val alpha = (1f - interceptor.blastAge / blastLifetime).coerceIn(0f, 1f)
                            val screenRadius = interceptor.blastRadius * refDim
                            // Inner white core.
                            drawCircle(
                                color = Color.White.copy(alpha = alpha * 0.35f),
                                radius = screenRadius * 0.45f,
                                center = Offset(ix, iy),
                            )
                            // Outer blast ring.
                            drawCircle(
                                color = colorAccent.copy(alpha = alpha * 0.7f),
                                radius = screenRadius,
                                center = Offset(ix, iy),
                                style = Stroke(width = 4f),
                            )
                        }
                        InterceptorPhase.DONE -> Unit
                    }
                }
            }

            // Wave-intro banner overlaid on board (never during active play).
            if (phase == MissileCommandPhase.WAVE_INTRO) {
                Text(
                    text = "Wave ${state.wave}",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Wave-tally overlay.
            if (phase == MissileCommandPhase.WAVE_TALLY) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Wave ${state.wave} Complete",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Score: ${state.score}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** Ammo strip showing dots for each silo below the board, above victory/defeat panels. */
@Composable
private fun AmmoStrip(
    state: MissileCommandState,
    neutralColor: Color,
    accentColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state.silos.forEachIndexed { idx, silo ->
            if (idx > 0) Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Ammo dots (10 per silo).
                Row {
                    repeat(Silo.FULL_AMMO) { i ->
                        val color = when {
                            !silo.alive -> neutralColor.copy(alpha = 0.2f)
                            i < silo.ammo -> accentColor
                            else -> neutralColor.copy(alpha = 0.3f)
                        }
                        Text(
                            text = "●",
                            color = color,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 1.dp),
                        )
                    }
                }
                Text(
                    text = if (silo.alive) "Silo ${idx + 1}" else "Destroyed",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (silo.alive) MaterialTheme.colorScheme.onSurface
                    else neutralColor.copy(alpha = 0.5f),
                )
            }
        }
    }
}

private fun starsForScore(score: Int, wave: Int): Int = when {
    wave >= MissileCommandState.MAX_WAVE && score >= 10_000 -> 3
    wave >= MissileCommandState.MAX_WAVE / 2 -> 2
    else -> 1
}
