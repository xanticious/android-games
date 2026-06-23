package com.xanticious.androidgames.view.games.morsedecoder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.morse.MorseBeeper
import com.xanticious.androidgames.controller.games.morsedecoder.OptionPicker
import com.xanticious.androidgames.controller.games.morsedecoder.ResultCalculator
import com.xanticious.androidgames.controller.games.morsedecoder.SentenceBank
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.morse.MORSE
import com.xanticious.androidgames.model.games.morse.MorseTiming
import com.xanticious.androidgames.model.games.morse.glyphs
import com.xanticious.androidgames.model.games.morsedecoder.DecodePrompt
import com.xanticious.androidgames.model.games.morsedecoder.DecoderResult
import com.xanticious.androidgames.model.games.morsedecoder.LetterOutcome
import com.xanticious.androidgames.model.games.morsedecoder.decoderConfigFor
import com.xanticious.androidgames.state.games.morsedecoder.DecoderPhase
import com.xanticious.androidgames.state.games.morsedecoder.MorseDecoderStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Morse Decoder — entry composable.
 *
 * Full self-configured flow: Settings → How to Play → Gameplay → Results.
 * The game plays a letter's Morse beeps on a loop; the player taps which of
 * 5 choices they heard.  A wrong guess disables that choice and replays the
 * beeps; the correct answer is always selectable.
 */
@Composable
fun MorseDecoderGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val machine = remember { MorseDecoderStateMachine() }
    val phase by machine.phase.collectAsState()

    var selectedDifficulty by rememberSaveable { mutableStateOf(difficulty) }
    var wpm by rememberSaveable { mutableIntStateOf(decoderConfigFor(difficulty).wpm) }
    var useRandomSeed by rememberSaveable { mutableStateOf(true) }
    var seedInput by rememberSaveable { mutableStateOf("") }
    var activeSeed by rememberSaveable { mutableLongStateOf(Random.nextLong()) }

    // ── Game state ─────────────────────────────────────────────────────────
    var sentence by remember { mutableStateOf("") }
    var letterIndices by remember { mutableStateOf(emptyList<Int>()) }
    var currentSlot by remember { mutableIntStateOf(0) }   // index into letterIndices
    var prompt by remember { mutableStateOf<DecodePrompt?>(null) }
    var outcomes by remember { mutableStateOf(emptyList<LetterOutcome>()) }
    var solvedLetters by remember { mutableStateOf(emptySet<Int>()) }
    var letterStartMs by remember { mutableLongStateOf(0L) }
    var wrongGuessCount by remember { mutableIntStateOf(0) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var gameStartMs by remember { mutableLongStateOf(0L) }
    var result by remember { mutableStateOf<DecoderResult?>(null) }

    // Beep loop state
    var replayTrigger by remember { mutableIntStateOf(0) }
    var activeBeepIndex by remember { mutableIntStateOf(-1) }

    // ── Helpers ────────────────────────────────────────────────────────────
    fun buildPrompt(answer: Char, slotSeed: Long): DecodePrompt =
        DecodePrompt(
            answer = answer,
            options = OptionPicker.build(answer, slotSeed, selectedDifficulty),
            disabled = emptySet()
        )

    fun accuracy(): Float {
        if (outcomes.isEmpty()) return 1f
        return outcomes.count { it.wrongGuesses == 0 }.toFloat() / outcomes.size
    }

    fun letterCount(): Pair<Int, Int> = Pair(currentSlot + 1, letterIndices.size)

    fun startLetterTimer() {
        letterStartMs = System.currentTimeMillis()
        wrongGuessCount = 0
    }

    fun advanceToNext() {
        if (currentSlot + 1 >= letterIndices.size) {
            // Sentence complete
            result = ResultCalculator.summarize(outcomes, activeSeed)
            machine.sentenceCompleted()
        } else {
            currentSlot++
            val nextChar = sentence[letterIndices[currentSlot]]
            prompt = buildPrompt(nextChar, activeSeed xor currentSlot.toLong())
            startLetterTimer()
            replayTrigger++
        }
    }

    // ── Init ────────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) { machine.startGame() }

    // ── Phase: LISTENING — start a letter timer ────────────────────────────
    LaunchedEffect(currentSlot, sentence) {
        if (sentence.isNotEmpty() && letterIndices.isNotEmpty()) {
            startLetterTimer()
        }
    }

    // ── Timer for elapsed display ──────────────────────────────────────────
    LaunchedEffect(phase) {
        if (phase == DecoderPhase.LISTENING) {
            gameStartMs = System.currentTimeMillis()
            while (true) {
                delay(500L)
                elapsedMs = System.currentTimeMillis() - gameStartMs
            }
        }
    }

    // ── Beep loop for the current letter ───────────────────────────────────
    LaunchedEffect(currentSlot, replayTrigger) {
        if (phase != DecoderPhase.LISTENING) return@LaunchedEffect
        val currentPrompt = prompt ?: return@LaunchedEffect
        val timing = MorseTiming(ditUnitMs = wpm.let { 1200 / it })
        val schedule = MorseBeeper.schedule(currentPrompt.answer, timing)

        while (true) {
            schedule.forEachIndexed { i, event ->
                activeBeepIndex = if (event.on) i else -1
                delay(event.durationMs.toLong())
            }
            activeBeepIndex = -1
            machine.beepsFinished()
            // Gap between repeats: 2× letter gap
            delay((timing.letterGapMs * 2).toLong())
        }
    }

    // ── UI ─────────────────────────────────────────────────────────────────
    GameScaffold(
        title = "Morse Decoder",
        onExit = onExit,
        hud = {
            if (phase == DecoderPhase.LISTENING) {
                val (cur, total) = letterCount()
                GameHud(
                    left = "Letter $cur/$total",
                    center = "%.0f%%".format(accuracy() * 100),
                    right = formatTime(elapsedMs)
                )
            }
        },
        status = {
            if (phase == DecoderPhase.RESULTS) {
                result?.let { r ->
                    ResultsPanel(
                        result = r,
                        onReplay = {
                            // Reset game state for replay
                            sentence = ""
                            letterIndices = emptyList()
                            currentSlot = 0
                            prompt = null
                            outcomes = emptyList()
                            solvedLetters = emptySet()
                            result = null
                            machine.replay()
                        },
                        onMenu = onExit
                    )
                }
            }
        }
    ) {
        when (phase) {
            DecoderPhase.IDLE -> { /* waiting for startGame */ }

            DecoderPhase.SETUP -> {
                SetupScreen(
                    difficulty = selectedDifficulty,
                    wpm = wpm,
                    useRandomSeed = useRandomSeed,
                    seedInput = seedInput,
                    onDifficulty = { d ->
                        selectedDifficulty = d
                        wpm = decoderConfigFor(d).wpm
                    },
                    onWpm = { wpm = it },
                    onUseRandomSeed = { useRandomSeed = it },
                    onSeedInput = { seedInput = it },
                    onHowToPlay = machine::openHowToPlay,
                    onStart = {
                        activeSeed = if (useRandomSeed || seedInput.isBlank()) {
                            Random.nextLong()
                        } else {
                            seedInput.toLongOrNull() ?: Random.nextLong()
                        }
                        sentence = SentenceBank.pick(activeSeed, selectedDifficulty)
                        letterIndices = sentence.indices.filter { sentence[it] != ' ' }
                        currentSlot = 0
                        outcomes = emptyList()
                        solvedLetters = emptySet()
                        elapsedMs = 0L

                        val firstChar = sentence[letterIndices[0]]
                        prompt = buildPrompt(firstChar, activeSeed)
                        replayTrigger = 0
                        machine.sentenceLoaded()
                    }
                )
            }

            DecoderPhase.HOW_TO_PLAY -> {
                HowToPlayScreen(onBack = machine::backToSetup)
            }

            DecoderPhase.LISTENING -> {
                val currentPrompt = prompt
                if (currentPrompt != null) {
                    val timing = MorseTiming(ditUnitMs = 1200 / wpm)
                    val schedule = MorseBeeper.schedule(currentPrompt.answer, timing)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Sentence display
                        SentenceDisplay(
                            sentence = sentence,
                            letterIndices = letterIndices,
                            solvedLetters = solvedLetters,
                            activeSlot = currentSlot,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Beep visualizer + Replay button
                        BeepVisualizer(
                            schedule = schedule,
                            activeBeepIndex = activeBeepIndex,
                            onReplay = { replayTrigger++ },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 5 choice buttons
                        ChoiceButtons(
                            options = currentPrompt.options,
                            disabled = currentPrompt.disabled,
                            onChoice = { chosen ->
                                if (chosen == currentPrompt.answer) {
                                    val timeMs = System.currentTimeMillis() - letterStartMs
                                    val outcome = LetterOutcome(
                                        answer = currentPrompt.answer,
                                        wrongGuesses = wrongGuessCount,
                                        timeMs = timeMs
                                    )
                                    outcomes = outcomes + outcome
                                    solvedLetters = solvedLetters + letterIndices[currentSlot]
                                    machine.guessedCorrect()
                                    advanceToNext()
                                } else {
                                    wrongGuessCount++
                                    prompt = currentPrompt.copy(
                                        disabled = currentPrompt.disabled + chosen
                                    )
                                    machine.guessedWrong()
                                    replayTrigger++
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            DecoderPhase.RESULTS -> {
                // Board is dimmed while results are shown in status slot
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.3f)
                        .background(GameCourt)
                ) {
                    SentenceDisplay(
                        sentence = sentence,
                        letterIndices = letterIndices,
                        solvedLetters = letterIndices.toSet().map { it }.toSet(),
                        activeSlot = -1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}

// ── Sub-composables ─────────────────────────────────────────────────────────

@Composable
private fun SetupScreen(
    difficulty: GameDifficulty,
    wpm: Int,
    useRandomSeed: Boolean,
    seedInput: String,
    onDifficulty: (GameDifficulty) -> Unit,
    onWpm: (Int) -> Unit,
    onUseRandomSeed: (Boolean) -> Unit,
    onSeedInput: (String) -> Unit,
    onHowToPlay: () -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text("Difficulty", style = MaterialTheme.typography.labelLarge)
        GameDifficulty.entries.forEach { d ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(selected = difficulty == d, onClick = { onDifficulty(d) })
                Spacer(Modifier.width(8.dp))
                Text(d.label)
            }
        }

        Text("Speed: $wpm WPM (${1200 / wpm} ms/unit)", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = wpm.toFloat(),
            onValueChange = { onWpm(it.toInt().coerceIn(3, 30)) },
            valueRange = 3f..30f,
            steps = 26
        )

        Text("Seed", style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = useRandomSeed, onClick = { onUseRandomSeed(true) })
            Text("Random")
            Spacer(Modifier.width(16.dp))
            RadioButton(selected = !useRandomSeed, onClick = { onUseRandomSeed(false) })
            Text("Enter seed:")
        }
        if (!useRandomSeed) {
            TextField(
                value = seedInput,
                onValueChange = onSeedInput,
                label = { Text("Seed number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onHowToPlay, modifier = Modifier.weight(1f)) {
                Text("How to Play")
            }
            Button(onClick = onStart, modifier = Modifier.weight(1f)) {
                Text("Start")
            }
        }
    }
}

@Composable
private fun HowToPlayScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "How to Play",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            """
            Morse Decoder plays each letter of a secret sentence as beeps.
            Your job is to listen and pick which letter you heard.

            • Dots (·) are short beeps, dashes (—) are long beeps.
            • The beeps loop automatically — listen as many times as you need.
            • Tap the [Replay] button to hear the letter again on demand.

            • You have 5 choices. Tap the correct letter.
            • A wrong guess: that button is greyed out and the beeps replay.
              The correct answer is always selectable.

            • Solve every letter to complete the sentence.
            • Accuracy = letters you got right on the first try.
            """.trimIndent(),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Settings")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SentenceDisplay(
    sentence: String,
    letterIndices: List<Int>,
    solvedLetters: Set<Int>,
    activeSlot: Int,
    modifier: Modifier = Modifier
) {
    val activeCharIndex = if (activeSlot >= 0 && activeSlot < letterIndices.size)
        letterIndices[activeSlot] else -1

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        sentence.forEachIndexed { idx, ch ->
            if (ch == ' ') {
                Spacer(Modifier.width(12.dp))
            } else {
                val isSolved = idx in solvedLetters
                val isActive = idx == activeCharIndex
                val bg = when {
                    isActive -> GameAccent
                    isSolved -> GameSuccess
                    else     -> GameCourt
                }
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(width = 28.dp, height = 36.dp)
                        .background(bg, shape = MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSolved || isActive) ch.toString() else "_",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun BeepVisualizer(
    schedule: List<com.xanticious.androidgames.controller.games.morse.BeepEvent>,
    activeBeepIndex: Int,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("♪ ", color = GameAccent, fontWeight = FontWeight.Bold)
            val toneEvents = schedule.filter { it.on }
            toneEvents.forEachIndexed { i, event ->
                val isActive = schedule.indexOf(event) == activeBeepIndex
                val isDah = event.durationMs > schedule.firstOrNull { it.on }?.durationMs ?: 1
                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .width(if (isDah) 24.dp else 10.dp)
                        .background(
                            if (isActive) GameAccent else GameNeutral,
                            shape = MaterialTheme.shapes.extraSmall
                        )
                )
            }
        }
        FilledTonalButton(
            onClick = onReplay,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text("Replay")
        }
    }
}

@Composable
private fun ChoiceButtons(
    options: List<Char>,
    disabled: Set<Char>,
    onChoice: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { ch ->
            val isDisabled = ch in disabled
            val glyphs = MORSE[ch]?.let { s ->
                s.joinToString("") { sym ->
                    if (sym == com.xanticious.androidgames.model.games.morse.Symbol.DIT) "·" else "—"
                }
            } ?: ""
            Button(
                onClick = { if (!isDisabled) onChoice(ch) },
                enabled = !isDisabled,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDisabled) GameNeutral else GameCourt,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = GameNeutral,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = ch.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = glyphs,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultsPanel(
    result: DecoderResult,
    onReplay: () -> Unit,
    onMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Complete!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = GameSuccess
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatChip(label = "Accuracy", value = "%.0f%%".format(result.accuracy * 100))
            StatChip(label = "Time", value = formatTime(result.totalMs))
            StatChip(label = "Streak", value = "${result.longestStreak}")
        }
        if (result.hardestLetter != '?') {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatChip(label = "Hardest", value = result.hardestLetter.toString())
                StatChip(label = "Seed", value = result.seed.toString())
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onMenu, modifier = Modifier.weight(1f)) {
                Text("Menu")
            }
            Button(onClick = onReplay, modifier = Modifier.weight(1f)) {
                Text("Replay")
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return "%d:%02d".format(minutes, seconds)
}
