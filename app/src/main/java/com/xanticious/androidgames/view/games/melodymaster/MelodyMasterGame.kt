package com.xanticious.androidgames.view.games.melodymaster

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.melodymaster.MelodyConfig
import com.xanticious.androidgames.controller.games.melodymaster.MelodyGenerator
import com.xanticious.androidgames.controller.games.rhythm.RhythmJudge
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.melodymaster.MelodyDifficultyConfig
import com.xanticious.androidgames.model.games.melodymaster.MelodyTrack
import com.xanticious.androidgames.model.games.melodymaster.MelodyTrackRequest
import com.xanticious.androidgames.model.games.melodymaster.Scale
import com.xanticious.androidgames.model.games.rhythm.Judgment
import com.xanticious.androidgames.model.games.rhythm.ScoreState
import com.xanticious.androidgames.model.games.rhythm.accuracyOf
import com.xanticious.androidgames.state.games.melodymaster.MelodyMasterPhase
import com.xanticious.androidgames.state.games.melodymaster.MelodyMasterStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameCourtLine
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import kotlinx.coroutines.delay
import kotlin.random.Random

// ── Lane color tokens (no inline hex) ────────────────────────────────────────
private val laneColors = listOf(GamePlayer, GameAccent, GameSuccess, GameHazard, GameEnemy)

private const val LANE_COUNT_MAX = 5
private const val COUNT_IN_BEATS = 4
private const val COUNT_IN_BPM = 100
private const val COUNT_IN_DURATION_MS = COUNT_IN_BEATS * 60_000L / COUNT_IN_BPM // ≈2400ms

/** Scale drop-down option including "Surprise" (random pick). */
private enum class ScaleChoice { MAJOR, MINOR, SURPRISE }

/**
 * Melody Master — entry composable.
 *
 * Owns the full Settings → How To Play → Count-In → Gameplay → Results flow,
 * driven by [MelodyMasterStateMachine]. Track generation is seeded and
 * deterministic so results can be replayed. Hit judging uses the shared
 * [RhythmJudge]. Victory/defeat content lives below the board in the status
 * slot (never overlays the highway).
 */
@Composable
fun MelodyMasterGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val machine = remember { MelodyMasterStateMachine() }
    val phase by machine.phase.collectAsState()

    // Settings state (survives config change)
    var selectedDifficulty by rememberSaveable { mutableStateOf(difficulty) }
    var scaleChoice by rememberSaveable { mutableStateOf(ScaleChoice.MAJOR) }
    var useRandomSeed by rememberSaveable { mutableStateOf(true) }
    var savedSeedInput by rememberSaveable { mutableStateOf("") }

    // Active game state (reset each run)
    var melodyTrack by remember { mutableStateOf<MelodyTrack?>(null) }
    var currentSeed by remember { mutableLongStateOf(0L) }
    var songTimeMs by remember { mutableLongStateOf(0L) }
    var hitIndices by remember { mutableStateOf(setOf<Int>()) }
    var scoreState by remember { mutableStateOf(ScoreState()) }
    var flourishAllHit by remember { mutableStateOf(true) }
    var countInBeat by remember { mutableStateOf(0) }

    val config = remember(selectedDifficulty) { MelodyConfig.configFor(selectedDifficulty) }

    // ── Phase-change side-effects ─────────────────────────────────────────────
    LaunchedEffect(Unit) { machine.startGame() }

    LaunchedEffect(phase) {
        when (phase) {
            MelodyMasterPhase.COUNT_IN -> {
                // Generate track if we don't already have one (replay reuses currentSeed)
                val seed = if (melodyTrack == null || useRandomSeed) {
                    if (useRandomSeed) {
                        val s = System.currentTimeMillis()
                        currentSeed = s
                        s
                    } else {
                        val s = savedSeedInput.toLongOrNull() ?: System.currentTimeMillis()
                        currentSeed = s
                        s
                    }
                } else {
                    currentSeed
                }
                val scalePreference = when (scaleChoice) {
                    ScaleChoice.MAJOR -> Scale.MAJOR
                    ScaleChoice.MINOR -> Scale.MINOR
                    ScaleChoice.SURPRISE -> if (Random(seed).nextBoolean()) Scale.MAJOR else Scale.MINOR
                }
                melodyTrack = MelodyGenerator.generate(
                    MelodyTrackRequest(selectedDifficulty, seed, scalePreference)
                )
                songTimeMs = -COUNT_IN_DURATION_MS
                hitIndices = emptySet()
                scoreState = ScoreState()
                flourishAllHit = true
                countInBeat = 0

                // Tick through count-in beats then fire CountInFinished
                val beatMs = COUNT_IN_DURATION_MS / COUNT_IN_BEATS
                for (beat in 1..COUNT_IN_BEATS) {
                    delay(beatMs)
                    countInBeat = beat
                }
                machine.countInFinished()
            }
            MelodyMasterPhase.PLAYING -> {
                songTimeMs = 0L
            }
            else -> Unit
        }
    }

    // ── Game loop: advance song clock and auto-miss passed notes ──────────────
    GameLoop(running = phase == MelodyMasterPhase.PLAYING) { dt ->
        val dt2 = (dt * 1000L).toLong()
        val newTime = songTimeMs + dt2
        songTimeMs = newTime

        val track = melodyTrack ?: return@GameLoop
        val windows = config.windows

        // Auto-miss notes that have scrolled past the good window
        val passedMissIndices = track.track.notes.indices.filter { idx ->
            if (idx in hitIndices) return@filter false
            val note = track.track.notes[idx]
            newTime - note.hitTimeMs > windows.goodMs
        }
        if (passedMissIndices.isNotEmpty()) {
            hitIndices = hitIndices + passedMissIndices
            var s = scoreState
            for (idx in passedMissIndices) {
                s = RhythmJudge.accumulate(s, Judgment.MISS)
                // If this missed note is in the flourish, clear the perfect finish flag
                if (track.track.notes[idx].hitTimeMs >= track.flourishStartMs) {
                    flourishAllHit = false
                }
            }
            scoreState = s
        }

        // Finish track when all notes are judged
        if (hitIndices.size >= track.track.notes.size) {
            machine.trackFinished()
        }
    }

    // ── Tap handler ───────────────────────────────────────────────────────────
    fun onLaneTap(lane: Int) {
        if (phase != MelodyMasterPhase.PLAYING) return
        val track = melodyTrack ?: return
        val windows = config.windows
        val t = songTimeMs

        // Find closest un-hit note in this lane within the good window
        val candidate = track.track.notes.indices
            .asSequence()
            .filter { idx ->
                idx !in hitIndices &&
                    track.track.notes[idx].lane == lane &&
                    kotlin.math.abs(track.track.notes[idx].hitTimeMs - t) <= windows.goodMs
            }
            .minByOrNull { idx -> kotlin.math.abs(track.track.notes[idx].hitTimeMs - t) }

        if (candidate != null) {
            val note = track.track.notes[candidate]
            val delta = t - note.hitTimeMs
            val judgment = RhythmJudge.judge(delta, windows)
            hitIndices = hitIndices + candidate
            scoreState = RhythmJudge.accumulate(scoreState, judgment)
            if (judgment == Judgment.MISS && note.hitTimeMs >= track.flourishStartMs) {
                flourishAllHit = false
            }
        }
        // Overtap (no note in window): no score change, no combo break
    }

    // ── Grade ─────────────────────────────────────────────────────────────────
    fun gradeFor(accuracy: Float): String = when {
        accuracy >= 1.0f -> "S"
        accuracy >= 0.9f -> "A"
        accuracy >= 0.8f -> "B"
        accuracy >= 0.65f -> "C"
        else -> "D"
    }

    // ── Determine if board should be dimmed (results) ─────────────────────────
    val boardDim = phase == MelodyMasterPhase.RESULTS

    // ── UI ────────────────────────────────────────────────────────────────────
    GameScaffold(
        title = "Melody Master",
        onExit = onExit,
        hud = {
            if (phase == MelodyMasterPhase.PLAYING || phase == MelodyMasterPhase.RESULTS) {
                val acc = accuracyOf(scoreState.counts)
                GameHud(
                    left = "${scoreState.score}",
                    center = "×${RhythmJudge.multiplierFor(scoreState.combo)}  ${scoreState.combo} combo",
                    right = "${(acc * 100).toInt()}%"
                )
            }
        },
        status = {
            when (phase) {
                MelodyMasterPhase.RESULTS -> {
                    val result = scoreState.toResult()
                    val grade = gradeFor(result.accuracy)
                    val track = melodyTrack
                    ResultsPanel(
                        result = result,
                        grade = grade,
                        seed = currentSeed,
                        flourishBonus = flourishAllHit && track != null &&
                            track.track.notes.any { it.hitTimeMs >= track.flourishStartMs },
                        onReplay = {
                            useRandomSeed = false
                            savedSeedInput = currentSeed.toString()
                            melodyTrack = null
                            machine.replay()
                        },
                        onNewTrack = {
                            useRandomSeed = true
                            melodyTrack = null
                            machine.newTrack()
                        },
                        onMenu = {
                            machine.menu()
                            onExit()
                        }
                    )
                }
                else -> Unit
            }
        }
    ) {
        when (phase) {
            MelodyMasterPhase.SETUP -> {
                SettingsScreen(
                    difficulty = selectedDifficulty,
                    onDifficulty = { selectedDifficulty = it },
                    scaleChoice = scaleChoice,
                    onScaleChoice = { scaleChoice = it },
                    useRandomSeed = useRandomSeed,
                    onUseRandomSeed = { useRandomSeed = it },
                    savedSeedInput = savedSeedInput,
                    onSavedSeedInput = { savedSeedInput = it },
                    onHowToPlay = machine::openHowToPlay,
                    onStart = { machine.confirmConfig() }
                )
            }
            MelodyMasterPhase.HOW_TO_PLAY -> {
                HowToPlayScreen(onBack = machine::backToSetup)
            }
            MelodyMasterPhase.COUNT_IN -> {
                CountInOverlay(beat = countInBeat, totalBeats = COUNT_IN_BEATS)
            }
            else -> {
                // PLAYING or RESULTS: draw the highway
                val track = melodyTrack
                if (track != null) {
                    HighwayBoard(
                        melodyTrack = track,
                        config = config,
                        songTimeMs = songTimeMs,
                        hitIndices = hitIndices,
                        dimmed = boardDim,
                        onLaneTap = ::onLaneTap
                    )
                }
            }
        }
    }
}

// ── Settings screen ───────────────────────────────────────────────────────────

@Composable
private fun SettingsScreen(
    difficulty: GameDifficulty,
    onDifficulty: (GameDifficulty) -> Unit,
    scaleChoice: ScaleChoice,
    onScaleChoice: (ScaleChoice) -> Unit,
    useRandomSeed: Boolean,
    onUseRandomSeed: (Boolean) -> Unit,
    savedSeedInput: String,
    onSavedSeedInput: (String) -> Unit,
    onHowToPlay: () -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Difficulty", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GameDifficulty.entries.forEach { d ->
                FilterChip(
                    selected = difficulty == d,
                    onClick = { onDifficulty(d) },
                    label = { Text(d.label) }
                )
            }
        }

        Text("Scale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScaleChoice.entries.forEach { s ->
                FilterChip(
                    selected = scaleChoice == s,
                    onClick = { onScaleChoice(s) },
                    label = {
                        Text(
                            when (s) {
                                ScaleChoice.MAJOR -> "Major"
                                ScaleChoice.MINOR -> "Minor"
                                ScaleChoice.SURPRISE -> "Surprise"
                            }
                        )
                    }
                )
            }
        }

        Text("Track Seed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = useRandomSeed,
                onClick = { onUseRandomSeed(true) },
                label = { Text("Random") }
            )
            FilterChip(
                selected = !useRandomSeed,
                onClick = { onUseRandomSeed(false) },
                label = { Text("Enter seed") }
            )
        }
        if (!useRandomSeed) {
            androidx.compose.material3.OutlinedTextField(
                value = savedSeedInput,
                onValueChange = onSavedSeedInput,
                label = { Text("Seed") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onHowToPlay) { Text("How to Play") }
            Button(onClick = onStart) { Text("Start") }
        }
    }
}

// ── How To Play screen ────────────────────────────────────────────────────────

@Composable
private fun HowToPlayScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Melody Master", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Gems scroll down 5 color-coded lanes toward the hit line at the bottom. Tap each lane button the moment a gem crosses the line.")

        SectionHeader("Lanes")
        Text("Each lane maps to a pitch — left is low, right is high. The colored gems trace the melody's contour as they fall.")

        SectionHeader("Timing")
        Text("• Perfect (±30 ms) — 100 pts\n• Great (±60 ms) — 70 pts\n• Good (±100 ms) — 40 pts\n• Miss — 0 pts, combo breaks")

        SectionHeader("Combo & Multiplier")
        Text("Landing notes without a miss builds your combo and raises your score multiplier up to ×4 (at 50 combo).")

        SectionHeader("Closing Flourish")
        Text("The final bar of every track is the Closing Flourish. Land every note without a miss to earn a Perfect Finish bonus on the results screen.")

        SectionHeader("Results")
        Text("After the track ends you'll see your score, max combo, accuracy, a grade (S/A/B/C/D), and the track seed — share it with friends to challenge them on the same melody.")

        Button(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) {
            Text("Back to Settings")
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

// ── Count-in overlay ──────────────────────────────────────────────────────────

@Composable
private fun CountInOverlay(beat: Int, totalBeats: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameCourt),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (beat == 0) "Get Ready" else "$beat",
                style = MaterialTheme.typography.displayLarge,
                color = GameAccent,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 1..totalBeats) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                if (i <= beat) GameAccent else GameNeutral.copy(alpha = 0.4f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            }
        }
    }
}

// ── Highway board (Canvas + tap buttons) ─────────────────────────────────────

@Composable
private fun HighwayBoard(
    melodyTrack: MelodyTrack,
    config: MelodyDifficultyConfig,
    songTimeMs: Long,
    hitIndices: Set<Int>,
    dimmed: Boolean,
    onLaneTap: (Int) -> Unit
) {
    val notes = melodyTrack.track.notes
    val laneCount = config.laneCount
    val leadTimeMs = config.leadTimeMs
    val hitLineRatio = 0.82f

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Highway canvas ────────────────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val w = size.width
            val h = size.height
            val laneW = w / laneCount
            val hitLineY = h * hitLineRatio
            val alpha = if (dimmed) 0.35f else 1f

            // Background
            drawRect(GameCourt.copy(alpha = alpha), size = size)

            // Lane dividers
            for (lane in 1 until laneCount) {
                drawLine(
                    color = GameCourtLine.copy(alpha = alpha * 0.4f),
                    start = Offset(lane * laneW, 0f),
                    end = Offset(lane * laneW, h),
                    strokeWidth = 1f
                )
            }

            // Hit line
            drawLine(
                color = GameCourtLine.copy(alpha = alpha),
                start = Offset(0f, hitLineY),
                end = Offset(w, hitLineY),
                strokeWidth = 3f
            )

            // Gems
            for ((idx, note) in notes.withIndex()) {
                if (idx in hitIndices) continue
                val distanceMs = note.hitTimeMs - songTimeMs
                if (distanceMs < -config.windows.goodMs || distanceMs > leadTimeMs) continue

                val gemY = hitLineY - (distanceMs.toFloat() / leadTimeMs) * hitLineY
                val color = laneColors.getOrElse(note.lane) { GameNeutral }
                val gemW = laneW * 0.7f
                val gemH = 22f
                val gemX = note.lane * laneW + (laneW - gemW) / 2f
                drawRoundRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(gemX, gemY - gemH / 2f),
                    size = Size(gemW, gemH),
                    cornerRadius = CornerRadius(6f)
                )
            }
        }

        // ── Lane tap buttons ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (lane in 0 until laneCount) {
                val color = laneColors.getOrElse(lane) { GameNeutral }
                Button(
                    onClick = { onLaneTap(lane) },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.75f))
                ) {
                    Text(
                        text = (lane + 1).toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Results panel ─────────────────────────────────────────────────────────────

@Composable
private fun ResultsPanel(
    result: com.xanticious.androidgames.model.games.rhythm.TrackResult,
    grade: String,
    seed: Long,
    flourishBonus: Boolean,
    onReplay: () -> Unit,
    onNewTrack: () -> Unit,
    onMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Score: ${result.score}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = grade,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = gradeColor(grade)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Max Combo: ${result.maxCombo}")
            Text("Accuracy: ${(result.accuracy * 100).toInt()}%")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("✦ ${result.counts[Judgment.PERFECT] ?: 0}", color = GameAccent)
            Text("◆ ${result.counts[Judgment.GREAT] ?: 0}", color = GamePlayer)
            Text("● ${result.counts[Judgment.GOOD] ?: 0}", color = GameNeutral)
            Text("✕ ${result.counts[Judgment.MISS] ?: 0}", color = GameEnemy)
        }

        if (flourishBonus) {
            Text(
                "✦ Perfect Finish!",
                color = GameAccent,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
        }

        Text(
            "Seed: $seed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onReplay) { Text("Replay") }
            OutlinedButton(onClick = onNewTrack) { Text("New Track") }
            OutlinedButton(onClick = onMenu) { Text("Menu") }
        }
    }
}

private fun gradeColor(grade: String): Color = when (grade) {
    "S" -> GameAccent
    "A" -> GameSuccess
    "B" -> GamePlayer
    "C" -> GameNeutral
    else -> GameEnemy
}
