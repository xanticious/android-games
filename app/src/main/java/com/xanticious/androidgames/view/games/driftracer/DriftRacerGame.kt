package com.xanticious.androidgames.view.games.driftracer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.driftracer.DriftRacerController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.JoystickInput
import com.xanticious.androidgames.model.games.driftracer.DriftRacerEvent
import com.xanticious.androidgames.model.games.driftracer.DriftRacerInput
import com.xanticious.androidgames.model.games.driftracer.DriftRacerState
import com.xanticious.androidgames.state.games.driftracer.DriftRacerPhase
import com.xanticious.androidgames.state.games.driftracer.DriftRacerStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.FloatingJoystick
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import kotlinx.coroutines.delay
import kotlin.math.PI

/** World units visible across the width of the canvas (camera zoom level). */
private const val VIEW_WORLD_SIZE = 40f

/**
 * Top-down time-trial racer. The player steers with a left [FloatingJoystick]
 * and holds the right-side GAS zone to accelerate; the BRAKE zone decelerates.
 * Entering a corner above speed threshold with heavy steering triggers a drift.
 * Crossing the start/finish line records lap times; race ends after
 * [com.xanticious.androidgames.model.games.driftracer.DriftRacerConfig.totalLaps] laps.
 *
 * [difficulty] is accepted per the shared composable contract but is intentionally
 * ignored — the design specifies no in-game difficulty setting.
 */
@Composable
fun DriftRacerGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { DriftRacerController() }
    val config     = remember(difficulty) { controller.configFor(difficulty) }
    val machine    = remember { DriftRacerStateMachine() }
    val phase      by machine.phase.collectAsState()

    var state          by remember { mutableStateOf(DriftRacerState.initial()) }
    var steeringInput  by remember { mutableStateOf(JoystickInput.NONE) }
    var throttleOn     by remember { mutableStateOf(false) }
    var brakeOn        by remember { mutableStateOf(false) }
    var countdownTick  by remember { mutableIntStateOf(3) }

    // Kick off the initial race.
    LaunchedEffect(Unit) { machine.startRace() }

    // Run the 3-2-1-GO countdown whenever we enter COUNTDOWN (start or retry).
    LaunchedEffect(phase) {
        if (phase != DriftRacerPhase.COUNTDOWN) return@LaunchedEffect
        for (i in 3 downTo 1) {
            countdownTick = i
            delay(1000L)
        }
        countdownTick = 0   // signals "GO!"
        delay(500L)
        machine.countdownComplete()
    }

    // Physics loop — only ticks while RACING.
    GameLoop(running = phase == DriftRacerPhase.RACING) { dt ->
        val input = DriftRacerInput(
            steering = steeringInput.dx,
            throttle = throttleOn,
            brake    = brakeOn
        )
        val step = controller.step(state, config, dt, input)
        state = step.state
        when (step.event) {
            DriftRacerEvent.RACE_FINISHED  -> machine.raceFinished()
            DriftRacerEvent.LAP_COMPLETED,
            DriftRacerEvent.NONE           -> Unit
        }
    }

    val track = DriftRacerState.TRACKS.getOrElse(state.courseIndex) { DriftRacerState.TRACKS[0] }

    GameScaffold(
        title  = "Drift Racer",
        onExit = onExit,
        hud = {
            GameHud(
                left   = "Lap ${state.lap}/${config.totalLaps}",
                center = formatTime(state.currentLapTime),
                right  = "Best: ${if (state.bestLap == Float.MAX_VALUE) "--:--.---"
                                   else formatTime(state.bestLap)}"
            )
        },
        status = {
            when (phase) {
                // ── Race Finished ──────────────────────────────────────────────
                DriftRacerPhase.RACE_FINISHED -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        state.lapTimes.forEachIndexed { idx, t ->
                            val isBest = t == state.bestLap
                            Text(
                                text       = "Lap ${idx + 1}:  ${formatTime(t)}${if (isBest) "  ★" else ""}",
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal,
                                color      = if (isBest) GameAccent
                                             else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        VictoryPanel(
                            score        = (state.totalTime * 1000f).toInt(),
                            bestScore    = (state.totalTime * 1000f).toInt(),
                            stars        = 3,
                            onReplay     = {
                                state = DriftRacerState.initial(state.courseIndex)
                                machine.retry()
                            },
                            onMenu       = onExit,
                            headline     = "Finished!  Total: ${formatTime(state.totalTime)}",
                            primaryLabel = "Retry"
                        )
                    }
                }

                // ── Countdown (and brief idle before first startRace fires) ───
                DriftRacerPhase.COUNTDOWN,
                DriftRacerPhase.IDLE -> {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .height(88.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = if (countdownTick > 0) countdownTick.toString() else "GO!",
                            style      = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color      = GameAccent
                        )
                    }
                }

                // ── Controls (shown only while racing) ────────────────────────
                DriftRacerPhase.RACING -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        // Left half — floating steering joystick
                        FloatingJoystick(
                            onInput  = { steeringInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            accent = GamePlayer
                        )

                        // Right half — GAS (hold) and BRAKE (hold) zones
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Box(
                                modifier         = Modifier
                                    .weight(2f)
                                    .fillMaxHeight()
                                    .background(GameSuccess.copy(alpha = 0.15f))
                                    .pointerInput(Unit) {
                                        detectTapGestures(onPress = { _ ->
                                            throttleOn = true
                                            tryAwaitRelease()
                                            throttleOn = false
                                        })
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("GAS",   fontWeight = FontWeight.Bold, color = GameSuccess)
                            }
                            Box(
                                modifier         = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(GameHazard.copy(alpha = 0.15f))
                                    .pointerInput(Unit) {
                                        detectTapGestures(onPress = { _ ->
                                            brakeOn = true
                                            tryAwaitRelease()
                                            brakeOn = false
                                        })
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("BRAKE", fontWeight = FontWeight.Bold, color = GameHazard)
                            }
                        }
                    }
                }
            }
        }
    ) {
        // ── Track canvas ───────────────────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cw    = size.width
            val ch    = size.height
            val scale = cw / VIEW_WORLD_SIZE
            val vh    = ch / scale          // world-height of the visible viewport

            // Camera: keep the car centred.
            val camX = state.position.x - VIEW_WORLD_SIZE / 2f
            val camY = state.position.y - vh / 2f

            fun wx(wx: Float) = (wx - camX) * scale
            fun wy(wy: Float) = (wy - camY) * scale
            fun wo(x: Float, y: Float) = Offset(wx(x), wy(y))

            // Grass background
            drawRect(color = GameSuccess.copy(alpha = 0.30f), size = size)

            // Track surface — wide stroked lines along the centerline
            val trackStroke = track.halfWidth * 2f * scale
            val n = track.centerline.size
            for (i in 0 until n) {
                val a = track.centerline[i]
                val b = track.centerline[(i + 1) % n]
                drawLine(color = GameNeutral, start = wo(a.x, a.y), end = wo(b.x, b.y),
                         strokeWidth = trackStroke)
            }

            // Centreline dashes for reference
            for (i in 0 until n) {
                val a = track.centerline[i]
                val b = track.centerline[(i + 1) % n]
                drawLine(color = GameCourtLine.copy(alpha = 0.45f),
                         start = wo(a.x, a.y), end = wo(b.x, b.y), strokeWidth = 2f)
            }

            // Start / finish line (perpendicular to track direction)
            val fp    = track.centerline[0]
            val fn    = controller.finishLineNormal(track)
            val perp  = Offset(-fn.y, fn.x)          // 90° CCW from forward
            val hw    = track.halfWidth * scale
            val fpOff = wo(fp.x, fp.y)
            drawLine(color = GameAccent,
                     start = fpOff - perp * hw, end = fpOff + perp * hw,
                     strokeWidth = 4f)

            // Car — rectangle rotated to match heading, colour changes while drifting
            val carSx = wx(state.position.x)
            val carSy = wy(state.position.y)
            val carW  = scale * 2.2f
            val carH  = scale * 3.8f
            val headDeg = (state.heading * 180f / PI.toFloat())

            rotate(degrees = headDeg, pivot = Offset(carSx, carSy)) {
                // Body
                drawRect(
                    color   = if (state.isDrifting) GameHazard else GamePlayer,
                    topLeft = Offset(carSx - carW / 2f, carSy - carH / 2f),
                    size    = Size(carW, carH)
                )
                // Hood stripe (front)
                drawRect(
                    color   = GameCourt,
                    topLeft = Offset(carSx - carW / 2f, carSy - carH / 2f),
                    size    = Size(carW, carH * 0.28f)
                )
            }

            // Speed bar — subtle arc at bottom-centre of board
            val barMaxW  = cw * 0.3f
            val barW     = barMaxW * (state.speed / config.maxSpeed).coerceIn(0f, 1f)
            val barY     = ch - scale * 0.8f
            drawRect(color = GameCourtLine.copy(alpha = 0.3f),
                     topLeft = Offset((cw - barMaxW) / 2f, barY - scale * 0.3f),
                     size = Size(barMaxW, scale * 0.3f))
            drawRect(color = if (state.isDrifting) GameHazard else GameAccent,
                     topLeft = Offset((cw - barMaxW) / 2f, barY - scale * 0.3f),
                     size = Size(barW, scale * 0.3f))
        }
    }
}

/** Formats [seconds] as `m:ss.mmm`. */
private fun formatTime(seconds: Float): String {
    val ms    = (seconds * 1000f).toInt().coerceAtLeast(0)
    val mins  = ms / 60000
    val secs  = (ms % 60000) / 1000
    val millis = ms % 1000
    return "%d:%02d.%03d".format(mins, secs, millis)
}
