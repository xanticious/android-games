package com.xanticious.androidgames.view.games.taprhythm

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.taprhythm.BeamGenerator
import com.xanticious.androidgames.controller.games.taprhythm.TapRhythmController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.rhythm.Judgment
import com.xanticious.androidgames.model.games.rhythm.ScoreState
import com.xanticious.androidgames.model.games.taprhythm.Beam
import com.xanticious.androidgames.model.games.taprhythm.ColorBucket
import com.xanticious.androidgames.model.games.taprhythm.Edge
import com.xanticious.androidgames.model.games.taprhythm.HealthState
import com.xanticious.androidgames.model.games.taprhythm.Lane
import com.xanticious.androidgames.model.games.taprhythm.RunResult
import com.xanticious.androidgames.state.games.taprhythm.TapRhythmPhase
import com.xanticious.androidgames.state.games.taprhythm.TapRhythmStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import kotlinx.coroutines.delay

// ── Lead time: how many ms ahead of the catch line beams are visible ─────────
private const val LEAD_TIME_MS = 1_500L

// ── Catch-line position (fraction of canvas height from top) ─────────────────
private const val CATCH_LINE_FRACTION = 0.82f

// ── Keys used to persist best survival times across Settings visits ───────────
private data class BestTimes(
    val easy: Long = 0L,
    val medium: Long = 0L,
    val hard: Long = 0L
) {
    fun forDifficulty(d: GameDifficulty) = when (d) {
        GameDifficulty.EASY   -> easy
        GameDifficulty.MEDIUM -> medium
        GameDifficulty.HARD   -> hard
    }
    fun updated(d: GameDifficulty, ms: Long) = when (d) {
        GameDifficulty.EASY   -> copy(easy   = maxOf(easy,   ms))
        GameDifficulty.MEDIUM -> copy(medium = maxOf(medium, ms))
        GameDifficulty.HARD   -> copy(hard   = maxOf(hard,   ms))
    }
}

/**
 * Tap Rhythm entry composable — full Settings → How to Play → Count-in → Gameplay
 * → Results flow. Self-configures via an internal [TapRhythmStateMachine].
 *
 * @param difficulty  Pre-selected difficulty (can be changed on the Settings screen).
 * @param onExit      Called when the user exits to the lobby.
 */
@Composable
fun TapRhythmGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val machine = remember { TapRhythmStateMachine() }
    val phase by machine.phase.collectAsState()

    // ── Persistent config ─────────────────────────────────────────────────────
    var selectedDifficulty by rememberSaveable { mutableStateOf(difficulty) }
    var useRandomSeed by rememberSaveable { mutableStateOf(true) }
    var customSeedText by rememberSaveable { mutableStateOf("") }
    var bestTimes by rememberSaveable { mutableStateOf(BestTimes()) }

    // ── Mutable run-time state (reset each run) ───────────────────────────────
    var seed by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var health by remember { mutableStateOf(HealthState()) }
    var scoreState by remember { mutableStateOf(ScoreState()) }
    var survivedMs by remember { mutableLongStateOf(0L) }
    var visibleBeams by remember { mutableStateOf<List<Beam>>(emptyList()) }
    var generatedUpToMs by remember { mutableLongStateOf(0L) }
    var allGeneratedBeams by remember { mutableStateOf<List<Beam>>(emptyList()) }
    var leftHeld by remember { mutableStateOf(false) }
    var rightHeld by remember { mutableStateOf(false) }
    var activeLeftBeam by remember { mutableStateOf<Beam?>(null) }
    var activeRightBeam by remember { mutableStateOf<Beam?>(null) }
    var lastJudgmentLeft by remember { mutableStateOf<Judgment?>(null) }
    var lastJudgmentRight by remember { mutableStateOf<Judgment?>(null) }
    var runResult by remember { mutableStateOf<RunResult?>(null) }
    var countInValue by remember { mutableStateOf(3) }

    // ── Open game on first composition ───────────────────────────────────────
    LaunchedEffect(Unit) { machine.openGame() }

    // ── Phase side-effects ────────────────────────────────────────────────────
    LaunchedEffect(phase) {
        when (phase) {
            TapRhythmPhase.COUNT_IN -> {
                // Reset run state
                seed = if (useRandomSeed) {
                    System.currentTimeMillis()
                } else {
                    customSeedText.toLongOrNull() ?: System.currentTimeMillis()
                }
                health = HealthState()
                scoreState = ScoreState()
                survivedMs = 0L
                visibleBeams = emptyList()
                generatedUpToMs = 0L
                allGeneratedBeams = emptyList()
                activeLeftBeam = null
                activeRightBeam = null
                lastJudgmentLeft = null
                lastJudgmentRight = null
                runResult = null

                // Count-in: 3 … 2 … 1
                for (i in 3 downTo 1) {
                    countInValue = i
                    delay(700L)
                }
                machine.countInFinished()
            }
            TapRhythmPhase.RESULTS -> {
                val result = TapRhythmController.buildResult(survivedMs, scoreState, seed)
                runResult = result
                bestTimes = bestTimes.updated(selectedDifficulty, survivedMs)
            }
            else -> Unit
        }
    }

    // ── Game loop ─────────────────────────────────────────────────────────────
    GameLoop(running = phase == TapRhythmPhase.PLAYING) { dtMs ->
        survivedMs += dtMs

        // Generate lookahead
        val lookaheadMs = survivedMs + LEAD_TIME_MS + 2_000L
        if (lookaheadMs > generatedUpToMs) {
            val newBatch = BeamGenerator.generateUpTo(selectedDifficulty, seed, lookaheadMs)
            allGeneratedBeams = newBatch
            generatedUpToMs = lookaheadMs
        }

        // Trim visible beams to those within the display window
        visibleBeams = allGeneratedBeams.filter { beam ->
            beam.startMs + beam.durationMs + 500L >= survivedMs &&
            beam.startMs <= survivedMs + LEAD_TIME_MS
        }

        // Auto-miss beams whose entire window has passed without a press
        val windows = TapRhythmController.windowsFor(selectedDifficulty, survivedMs)
        allGeneratedBeams.filter { beam ->
            beam.startMs + windows.goodMs < survivedMs &&
            beam.lane == Lane.LEFT && activeLeftBeam == null &&
            !scoreState.counts.keys.any { false }  // simplified: just check by time
        }

        // Miss beams that scrolled completely past the press window (neither lane caught them)
        val pressWindowEnd = survivedMs - windows.goodMs
        for (beam in allGeneratedBeams) {
            if (beam.startMs + beam.durationMs < survivedMs - LEAD_TIME_MS) continue
            if (beam.startMs > pressWindowEnd) break
            // Press edge miss: if this beam has never been pressed
            val isPressedLeft  = activeLeftBeam?.startMs == beam.startMs && beam.lane == Lane.LEFT
            val isPressedRight = activeRightBeam?.startMs == beam.startMs && beam.lane == Lane.RIGHT
            if (!isPressedLeft && !isPressedRight) {
                val missed = TapRhythmController.judgeEdge(beam, Edge.PRESS, pressWindowEnd, windows)
                if (missed.judgment == Judgment.MISS) {
                    scoreState = TapRhythmController.updateScore(scoreState, Judgment.MISS)
                    health = TapRhythmController.updateHealth(health, Judgment.MISS)
                }
            }
        }

        // Check death
        if (health.isDead) {
            machine.healthDepleted()
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    when (phase) {
        TapRhythmPhase.IDLE -> Unit

        TapRhythmPhase.SETTINGS -> TapRhythmSettings(
            selectedDifficulty = selectedDifficulty,
            useRandomSeed = useRandomSeed,
            customSeedText = customSeedText,
            onDifficultyChange = { selectedDifficulty = it },
            onRandomSeedToggle = { useRandomSeed = it },
            onCustomSeedChange = { customSeedText = it },
            onPlay = { machine.confirmConfig() },
            onHowToPlay = { machine.openHowToPlay() },
            onExit = onExit
        )

        TapRhythmPhase.HOW_TO_PLAY -> TapRhythmHowToPlay(
            onBack = { machine.backToSettings() },
            onExit = onExit
        )

        TapRhythmPhase.COUNT_IN -> TapRhythmCountIn(
            countValue = countInValue,
            onExit = onExit
        )

        TapRhythmPhase.PLAYING -> TapRhythmPlayfield(
            survivedMs = survivedMs,
            health = health,
            scoreState = scoreState,
            visibleBeams = visibleBeams,
            activeLeftBeam = activeLeftBeam,
            activeRightBeam = activeRightBeam,
            lastJudgmentLeft = lastJudgmentLeft,
            lastJudgmentRight = lastJudgmentRight,
            onLeftPress = {
                if (!leftHeld) {
                    leftHeld = true
                    val windows = TapRhythmController.windowsFor(selectedDifficulty, survivedMs)
                    val candidate = visibleBeams
                        .filter { it.lane == Lane.LEFT }
                        .minByOrNull { kotlin.math.abs(it.startMs - survivedMs) }
                    if (candidate != null) {
                        val ej = TapRhythmController.judgeEdge(candidate, Edge.PRESS, survivedMs, windows)
                        if (ej.judgment != Judgment.MISS) {
                            activeLeftBeam = candidate
                            lastJudgmentLeft = ej.judgment
                            scoreState = TapRhythmController.updateScore(scoreState, ej.judgment)
                            health = TapRhythmController.updateHealth(health, ej.judgment)
                        } else {
                            lastJudgmentLeft = Judgment.MISS
                            scoreState = TapRhythmController.updateScore(scoreState, Judgment.MISS)
                            health = TapRhythmController.updateHealth(health, Judgment.MISS)
                        }
                    }
                }
            },
            onLeftRelease = {
                leftHeld = false
                val beam = activeLeftBeam
                if (beam != null) {
                    val windows = TapRhythmController.windowsFor(selectedDifficulty, survivedMs)
                    val ej = TapRhythmController.judgeEdge(beam, Edge.RELEASE, survivedMs, windows)
                    lastJudgmentLeft = ej.judgment
                    scoreState = TapRhythmController.updateScore(scoreState, ej.judgment)
                    health = TapRhythmController.updateHealth(health, ej.judgment)
                    activeLeftBeam = null
                }
            },
            onRightPress = {
                if (!rightHeld) {
                    rightHeld = true
                    val windows = TapRhythmController.windowsFor(selectedDifficulty, survivedMs)
                    val candidate = visibleBeams
                        .filter { it.lane == Lane.RIGHT }
                        .minByOrNull { kotlin.math.abs(it.startMs - survivedMs) }
                    if (candidate != null) {
                        val ej = TapRhythmController.judgeEdge(candidate, Edge.PRESS, survivedMs, windows)
                        if (ej.judgment != Judgment.MISS) {
                            activeRightBeam = candidate
                            lastJudgmentRight = ej.judgment
                            scoreState = TapRhythmController.updateScore(scoreState, ej.judgment)
                            health = TapRhythmController.updateHealth(health, ej.judgment)
                        } else {
                            lastJudgmentRight = Judgment.MISS
                            scoreState = TapRhythmController.updateScore(scoreState, Judgment.MISS)
                            health = TapRhythmController.updateHealth(health, Judgment.MISS)
                        }
                    }
                }
            },
            onRightRelease = {
                rightHeld = false
                val beam = activeRightBeam
                if (beam != null) {
                    val windows = TapRhythmController.windowsFor(selectedDifficulty, survivedMs)
                    val ej = TapRhythmController.judgeEdge(beam, Edge.RELEASE, survivedMs, windows)
                    lastJudgmentRight = ej.judgment
                    scoreState = TapRhythmController.updateScore(scoreState, ej.judgment)
                    health = TapRhythmController.updateHealth(health, ej.judgment)
                    activeRightBeam = null
                }
            },
            onExit = onExit
        )

        TapRhythmPhase.RESULTS -> {
            val result = runResult
            if (result != null) {
                TapRhythmResults(
                    result = result,
                    difficulty = selectedDifficulty,
                    bestMs = bestTimes.forDifficulty(selectedDifficulty),
                    onReplay = { machine.replay() },
                    onNewRun = { machine.newRun() },
                    onMenu = {
                        machine.menu()
                        onExit()
                    }
                )
            }
        }
    }
}

// ── Settings screen ───────────────────────────────────────────────────────────

@Composable
private fun TapRhythmSettings(
    selectedDifficulty: GameDifficulty,
    useRandomSeed: Boolean,
    customSeedText: String,
    onDifficultyChange: (GameDifficulty) -> Unit,
    onRandomSeedToggle: (Boolean) -> Unit,
    onCustomSeedChange: (String) -> Unit,
    onPlay: () -> Unit,
    onHowToPlay: () -> Unit,
    onExit: () -> Unit
) {
    GameScaffold(title = "Tap Rhythm", onExit = onExit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Tap Rhythm",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Hold the beam as it reaches the catch line — press when it arrives, release when it ends. Survive as long as you can.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text("Difficulty", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(GameDifficulty.EASY, GameDifficulty.MEDIUM, GameDifficulty.HARD).forEach { d ->
                    val label = when (d) {
                        GameDifficulty.EASY   -> "Easy"
                        GameDifficulty.MEDIUM -> "Normal"
                        GameDifficulty.HARD   -> "Hard"
                    }
                    if (d == selectedDifficulty) {
                        Button(onClick = {}) { Text(label) }
                    } else {
                        OutlinedButton(onClick = { onDifficultyChange(d) }) { Text(label) }
                    }
                }
            }

            Text("Seed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (useRandomSeed) {
                    Button(onClick = {}) { Text("Random") }
                    OutlinedButton(onClick = { onRandomSeedToggle(false) }) { Text("Enter Seed") }
                } else {
                    OutlinedButton(onClick = { onRandomSeedToggle(true) }) { Text("Random") }
                    Button(onClick = {}) { Text("Enter Seed") }
                }
            }
            if (!useRandomSeed) {
                TextField(
                    value = customSeedText,
                    onValueChange = onCustomSeedChange,
                    label = { Text("Seed (number)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onPlay, modifier = Modifier.fillMaxWidth()) {
                Text("Play", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(onClick = onHowToPlay, modifier = Modifier.fillMaxWidth()) {
                Text("How to Play")
            }
        }
    }
}

// ── How to Play screen ────────────────────────────────────────────────────────

@Composable
private fun TapRhythmHowToPlay(onBack: () -> Unit, onExit: () -> Unit) {
    GameScaffold(title = "How to Play", onExit = onExit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Tap Rhythm", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            HowToPlaySection("The Goal") {
                Text("Survive as long as you can. Beams fall from the top — press and hold the matching button until the beam ends.")
            }
            HowToPlaySection("Two Lanes, Two Thumbs") {
                Text("LEFT column: press and hold with your left thumb.")
                Text("RIGHT column: press and hold with your right thumb.")
                Text("Both lanes are independent — you may need to hold both at once.")
            }
            HowToPlaySection("Press & Release") {
                Text("• Press the button the moment the beam's TOP edge crosses the catch line.")
                Text("• Release the moment the beam's BOTTOM edge crosses the catch line.")
                Text("• Timing accuracy earns Perfect / Great / Good / Miss on each edge.")
            }
            HowToPlaySection("Beam Colors") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(16.dp).background(GameAccent))
                    Text("Yellow — short hold (quick tap-and-release)")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(16.dp).background(GameNeutral))
                    Text("Grey — medium hold")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(16.dp).background(GameHazard))
                    Text("Orange — long sustained hold")
                }
            }
            HowToPlaySection("Health") {
                Text("Miss edges drain your health. Perfect edges restore a little. Health reaches zero → you die.")
            }
            HowToPlaySection("It Gets Harder") {
                Text("Beams fall faster, gaps shrink, and both lanes overlap more as you survive longer.")
            }

            Button(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
                Text("Back to Settings")
            }
        }
    }
}

@Composable
private fun HowToPlaySection(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    content()
}

// ── Count-in screen ───────────────────────────────────────────────────────────

@Composable
private fun TapRhythmCountIn(countValue: Int, onExit: () -> Unit) {
    GameScaffold(title = "Get Ready", onExit = onExit) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (countValue > 0) countValue.toString() else "GO!",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = GameAccent
            )
        }
    }
}

// ── Gameplay screen ───────────────────────────────────────────────────────────

@Composable
private fun TapRhythmPlayfield(
    survivedMs: Long,
    health: HealthState,
    scoreState: ScoreState,
    visibleBeams: List<Beam>,
    activeLeftBeam: Beam?,
    activeRightBeam: Beam?,
    lastJudgmentLeft: Judgment?,
    lastJudgmentRight: Judgment?,
    onLeftPress: () -> Unit,
    onLeftRelease: () -> Unit,
    onRightPress: () -> Unit,
    onRightRelease: () -> Unit,
    onExit: () -> Unit
) {
    val multiplier = com.xanticious.androidgames.controller.games.rhythm.RhythmJudge
        .multiplierFor(scoreState.combo)

    GameScaffold(
        title = "Tap Rhythm",
        onExit = onExit,
        hud = {
            GameHud(
                left = "♥ ${(health.fraction * 100).toInt()}%",
                center = "${scoreState.combo} streak  ×$multiplier",
                right = formatTime(survivedMs)
            )
            LinearProgressIndicator(
                progress = { health.fraction },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                color = when {
                    health.fraction > 0.6f -> GameSuccess
                    health.fraction > 0.3f -> GameAccent
                    else -> GameHazard
                },
                trackColor = GameCourt
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Highway canvas ──────────────────────────────────────────────
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(GameCourt)
            ) {
                val canvasW = size.width
                val canvasH = size.height
                val laneW = canvasW / 2f
                val catchY = canvasH * CATCH_LINE_FRACTION

                // Lane divider
                drawLine(
                    color = GameCourtLine.copy(alpha = 0.4f),
                    start = Offset(laneW, 0f),
                    end = Offset(laneW, canvasH),
                    strokeWidth = 2f
                )

                // Catch lines
                drawLine(
                    color = GameCourtLine,
                    start = Offset(0f, catchY),
                    end = Offset(laneW, catchY),
                    strokeWidth = 3f
                )
                drawLine(
                    color = GameCourtLine,
                    start = Offset(laneW, catchY),
                    end = Offset(canvasW, catchY),
                    strokeWidth = 3f
                )

                // Draw beams
                for (beam in visibleBeams) {
                    val xLeft = if (beam.lane == Lane.LEFT) 8f else laneW + 8f
                    val xRight = if (beam.lane == Lane.LEFT) laneW - 8f else canvasW - 8f
                    val beamWidth = xRight - xLeft

                    // Map beam time to y position:
                    // At survivedMs, the catch line is at catchY.
                    // A beam at startMs is (startMs - survivedMs) ms in the future,
                    // meaning it's above the catch line.
                    val pixelsPerMs = catchY / LEAD_TIME_MS.toFloat()

                    val topTime = beam.startMs          // press edge time
                    val botTime = beam.startMs + beam.durationMs  // release edge time

                    val topY = catchY - (topTime - survivedMs) * pixelsPerMs
                    val botY = catchY - (botTime - survivedMs) * pixelsPerMs

                    if (botY < 0f || topY > canvasH) continue

                    val clampedTopY = topY.coerceAtLeast(0f)
                    val clampedBotY = botY.coerceAtMost(canvasH)
                    val beamHeight = (clampedBotY - clampedTopY).coerceAtLeast(4f)

                    val isActive = beam == activeLeftBeam || beam == activeRightBeam
                    val beamColor = beamColor(beam.color).let {
                        if (isActive) it else it.copy(alpha = 0.75f)
                    }

                    drawRoundRect(
                        color = beamColor,
                        topLeft = Offset(xLeft, clampedTopY),
                        size = Size(beamWidth, beamHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )

                    // Active fill indicator (held progress)
                    if (isActive) {
                        val progress = ((survivedMs - beam.startMs).toFloat() /
                                       beam.durationMs.toFloat()).coerceIn(0f, 1f)
                        drawRect(
                            color = GamePlayer.copy(alpha = 0.3f),
                            topLeft = Offset(xLeft + 2f, clampedTopY + 2f),
                            size = Size(beamWidth - 4f, (beamHeight - 4f) * progress)
                        )
                    }
                }

                // Judgment flashes
                fun drawJudgmentFlash(judgment: Judgment?, laneX: Float) {
                    if (judgment == null) return
                    val flashColor = when (judgment) {
                        Judgment.PERFECT -> GameSuccess
                        Judgment.GREAT   -> GamePlayer
                        Judgment.GOOD    -> GameAccent
                        Judgment.MISS    -> GameHazard
                    }
                    drawLine(
                        color = flashColor,
                        start = Offset(laneX, catchY - 8f),
                        end = Offset(laneX + laneW, catchY + 8f),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                }
                drawJudgmentFlash(lastJudgmentLeft, 0f)
                drawJudgmentFlash(lastJudgmentRight, laneW)
            }

            // ── Thumb buttons ───────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth().height(96.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(GameCourtLine.copy(alpha = 0.15f))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown()
                                onLeftPress()
                                awaitPointerEvent()
                                onLeftRelease()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "L",
                        color = if (activeLeftBeam != null) GamePlayer else GameCourtLine,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(GameCourtLine.copy(alpha = 0.15f))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown()
                                onRightPress()
                                awaitPointerEvent()
                                onRightRelease()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "R",
                        color = if (activeRightBeam != null) GamePlayer else GameCourtLine,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Score HUD strip ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Score: ${scoreState.score}", style = MaterialTheme.typography.bodyMedium)
                Text("Best combo: ${scoreState.maxCombo}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ── Results screen ────────────────────────────────────────────────────────────

@Composable
private fun TapRhythmResults(
    result: RunResult,
    difficulty: GameDifficulty,
    bestMs: Long,
    onReplay: () -> Unit,
    onNewRun: () -> Unit,
    onMenu: () -> Unit
) {
    val isNewRecord = result.survivedMs >= bestMs

    GameScaffold(
        title = "Tap Rhythm",
        onExit = onMenu,
        status = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Game Over",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isNewRecord) {
                    Text("★ New Record!", color = GameAccent, fontWeight = FontWeight.Bold)
                }

                Text(
                    "Survived: ${formatTime(result.survivedMs)}",
                    style = MaterialTheme.typography.titleLarge
                )
                Text("Score: ${result.score}")
                Text("Max Streak: ${result.maxStreak}")

                val p = result.counts[Judgment.PERFECT] ?: 0
                val g = result.counts[Judgment.GREAT] ?: 0
                val ok = result.counts[Judgment.GOOD] ?: 0
                val m = result.counts[Judgment.MISS] ?: 0
                Text("Perfect: $p   Great: $g   Good: $ok   Miss: $m",
                    style = MaterialTheme.typography.bodySmall)

                Text(
                    "Best: ${formatTime(bestMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Seed: ${result.seed}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onReplay, modifier = Modifier.weight(1f)) {
                        Text("Replay")
                    }
                    OutlinedButton(onClick = onNewRun, modifier = Modifier.weight(1f)) {
                        Text("New Run")
                    }
                    OutlinedButton(onClick = onMenu, modifier = Modifier.weight(1f)) {
                        Text("Menu")
                    }
                }
            }
        }
    ) {
        // Dimmed board while results are shown
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GameCourt.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "♦",
                style = MaterialTheme.typography.displayLarge,
                color = GameCourtLine.copy(alpha = 0.2f)
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun beamColor(bucket: ColorBucket): Color = when (bucket) {
    ColorBucket.SHORT  -> GameAccent   // yellow: quick
    ColorBucket.MEDIUM -> GameNeutral  // grey: moderate
    ColorBucket.LONG   -> GameHazard   // orange: sustained
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000L).toInt().coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
