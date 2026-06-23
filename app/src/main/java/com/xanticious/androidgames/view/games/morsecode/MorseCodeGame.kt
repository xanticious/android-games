package com.xanticious.androidgames.view.games.morsecode

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.morsecode.HardestWords
import com.xanticious.androidgames.controller.games.morsecode.MorseKeyer
import com.xanticious.androidgames.controller.games.morsecode.PhraseBank
import com.xanticious.androidgames.controller.games.morsecode.StatsCalculator
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.morse.MORSE
import com.xanticious.androidgames.model.games.morse.MorseTiming
import com.xanticious.androidgames.model.games.morse.Symbol
import com.xanticious.androidgames.model.games.morse.glyphs
import com.xanticious.androidgames.model.games.morse.letterFor
import com.xanticious.androidgames.model.games.morse.wpmToUnitMs
import com.xanticious.androidgames.model.games.morsecode.DifficultyConfig
import com.xanticious.androidgames.model.games.morsecode.KeyStroke
import com.xanticious.androidgames.model.games.morsecode.LetterAttempt
import com.xanticious.androidgames.model.games.morsecode.PENALTY_DELAY_MS
import com.xanticious.androidgames.model.games.morsecode.Phrase
import com.xanticious.androidgames.model.games.morsecode.PhraseStats
import com.xanticious.androidgames.model.games.morsecode.TRAINING_MODE_DEFAULT
import com.xanticious.androidgames.model.games.morsecode.WordAttempt
import com.xanticious.androidgames.model.games.morsecode.difficultyConfig
import com.xanticious.androidgames.state.games.morsecode.MorseCodePhase
import com.xanticious.androidgames.state.games.morsecode.MorseCodeStateMachine
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameCourt
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.GameScaffold
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Entry point for the Morse Code single-button sender game.
 * Self-configured: owns the Settings → How to Play → Gameplay → Stats flow.
 */
@Composable
fun MorseCodeGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val scope = rememberCoroutineScope()
    val stateMachine = remember { MorseCodeStateMachine(scope) }
    val phase by stateMachine.phase.collectAsState()

    // ── Config state ────────────────────────────────────────────────────────
    var trainingMode by remember { mutableStateOf(TRAINING_MODE_DEFAULT) }
    var gameDifficulty by remember { mutableStateOf(difficulty) }
    var seed by remember { mutableStateOf(System.currentTimeMillis()) }

    // ── Phrase / word / letter progress ─────────────────────────────────────
    var phrase by remember { mutableStateOf(Phrase(emptyList())) }
    var wordIndex by remember { mutableStateOf(0) }
    var letterIndex by remember { mutableStateOf(0) }
    // Per-position mistake accumulator; survives word retries
    val perPositionMistakes = remember { mutableStateMapOf<Int, Int>() }
    var wordRetries by remember { mutableStateOf(0) }
    var wordStartMs by remember { mutableStateOf(0L) }
    var letterStartMs by remember { mutableStateOf(0L) }
    var completedWordAttempts by remember { mutableStateOf(listOf<WordAttempt>()) }
    var completedLetters by remember { mutableStateOf(listOf<LetterAttempt>()) }
    var stats by remember { mutableStateOf<PhraseStats?>(null) }
    var retrainWords by remember { mutableStateOf(listOf<String>()) }

    // Current timing derived from gameDifficulty
    fun currentTiming(): MorseTiming {
        val config = difficultyConfig(gameDifficulty)
        return MorseTiming(wpmToUnitMs(config.wpmTarget))
    }

    // ── Start the state machine on first composition ─────────────────────────
    LaunchedEffect(Unit) {
        stateMachine.startGame()
    }

    // ── Phase routing ────────────────────────────────────────────────────────
    when (phase) {
        MorseCodePhase.IDLE -> Unit // brief; LaunchedEffect fires startGame()

        MorseCodePhase.SETUP -> {
            SettingsScreen(
                trainingMode = trainingMode,
                difficulty = gameDifficulty,
                seed = seed,
                onTrainingModeChange = { trainingMode = it },
                onDifficultyChange = { gameDifficulty = it },
                onSeedChange = { seed = it },
                onConfirm = { stateMachine.confirmConfig() },
                onExit = onExit
            )
        }

        MorseCodePhase.HOW_TO_PLAY -> {
            HowToPlayScreen(
                onBack = { stateMachine.backToSetup() },
                onPlay = {
                    // Load phrase
                    phrase = if (retrainWords.isEmpty()) {
                        PhraseBank.pick(seed, gameDifficulty)
                    } else {
                        Phrase(retrainWords).also { retrainWords = emptyList() }
                    }
                    wordIndex = 0
                    letterIndex = 0
                    perPositionMistakes.clear()
                    wordRetries = 0
                    completedWordAttempts = emptyList()
                    completedLetters = emptyList()
                    val now = System.currentTimeMillis()
                    wordStartMs = now
                    letterStartMs = now
                    stateMachine.phraseLoaded()
                }
            )
        }

        MorseCodePhase.KEYING, MorseCodePhase.PENALTY -> {
            val isPenalty = phase == MorseCodePhase.PENALTY
            val timing = currentTiming()

            GameplayScreen(
                phrase = phrase,
                wordIndex = wordIndex,
                letterIndex = letterIndex,
                completedLetters = completedLetters,
                timing = timing,
                trainingMode = trainingMode,
                isPenalty = isPenalty,
                onLetterDecoded = { decoded ->
                    if (isPenalty) return@GameplayScreen // ignore input during penalty
                    val word = phrase.words.getOrNull(wordIndex) ?: return@GameplayScreen
                    val expected = word.getOrNull(letterIndex)?.uppercaseChar() ?: return@GameplayScreen
                    val now = System.currentTimeMillis()

                    if (decoded == expected) {
                        val letterMs = now - letterStartMs
                        val mistakes = perPositionMistakes[letterIndex] ?: 0
                        completedLetters = completedLetters + LetterAttempt(expected, letterMs, mistakes)
                        perPositionMistakes.remove(letterIndex)
                        letterStartMs = now

                        if (letterIndex + 1 >= word.length) {
                            // ── Word complete ────────────────────────────────
                            val wordMs = now - wordStartMs
                            completedWordAttempts = completedWordAttempts +
                                    WordAttempt(word, wordMs, wordRetries, completedLetters)
                            completedLetters = emptyList()
                            perPositionMistakes.clear()
                            wordRetries = 0

                            if (wordIndex + 1 >= phrase.words.size) {
                                // ── Phrase complete ──────────────────────────
                                stats = StatsCalculator.summarize(completedWordAttempts)
                                stateMachine.phraseCompleted()
                            } else {
                                wordIndex++
                                letterIndex = 0
                                wordStartMs = now
                                letterStartMs = now
                                stateMachine.wordCompleted()
                            }
                        } else {
                            letterIndex++
                            stateMachine.letterCommitted()
                        }
                    } else {
                        // ── Mistake ──────────────────────────────────────────
                        perPositionMistakes[letterIndex] = (perPositionMistakes[letterIndex] ?: 0) + 1
                        wordRetries++
                        letterIndex = 0
                        completedLetters = emptyList()
                        letterStartMs = System.currentTimeMillis()
                        stateMachine.mistakeMade()
                        scope.launch {
                            delay(PENALTY_DELAY_MS)
                            stateMachine.penaltyElapsed()
                        }
                    }
                },
                onExit = onExit
            )
        }

        MorseCodePhase.STATS -> {
            val currentStats = stats
            if (currentStats != null) {
                StatsScreen(
                    stats = currentStats,
                    seed = seed,
                    completedWords = completedWordAttempts,
                    onRetrain = {
                        retrainWords = HardestWords.pick(completedWordAttempts)
                        // Prepare retrain phrase and go directly to KEYING
                        phrase = Phrase(retrainWords)
                        retrainWords = emptyList()
                        wordIndex = 0
                        letterIndex = 0
                        perPositionMistakes.clear()
                        wordRetries = 0
                        completedWordAttempts = emptyList()
                        completedLetters = emptyList()
                        val now = System.currentTimeMillis()
                        wordStartMs = now
                        letterStartMs = now
                        stateMachine.retrainHardest()
                    },
                    onExit = onExit
                )
            }
        }
    }
}

// ── Settings screen ──────────────────────────────────────────────────────────

@Composable
private fun SettingsScreen(
    trainingMode: Boolean,
    difficulty: GameDifficulty,
    seed: Long,
    onTrainingModeChange: (Boolean) -> Unit,
    onDifficultyChange: (GameDifficulty) -> Unit,
    onSeedChange: (Long) -> Unit,
    onConfirm: () -> Unit,
    onExit: () -> Unit
) {
    GameScaffold(title = "Morse Code", onExit = onExit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            // Training Mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Training Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        "Show Morse code under each letter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = trainingMode, onCheckedChange = onTrainingModeChange)
            }

            Divider()

            // Difficulty picker
            Text("Difficulty", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameDifficulty.entries.forEach { d ->
                    FilterChip(
                        selected = d == difficulty,
                        onClick = { onDifficultyChange(d) },
                        label = { Text(d.label) }
                    )
                }
            }
            val wpmLabel = difficultyConfig(difficulty).wpmTarget
            Text(
                "Target: $wpmLabel WPM  •  Unit = ${wpmToUnitMs(wpmLabel)} ms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider()

            // Seed
            Text("Phrase Seed", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    seed.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = { onSeedChange(System.currentTimeMillis()) }) {
                    Text("Random")
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("How to Play →")
            }
        }
    }
}

// ── How to Play screen ───────────────────────────────────────────────────────

@Composable
private fun HowToPlayScreen(
    onBack: () -> Unit,
    onPlay: () -> Unit
) {
    GameScaffold(title = "How to Play", onExit = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Morse Code Sender", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            InstructionSection(
                title = "The Button",
                body = "Use the single TAP/KEY button to send Morse code.\n" +
                        "• Short press (dit) → "." \n" +
                        "• Long press (dah)  → "-"\n\n" +
                        "The threshold is 2× the dit unit. Aim to feel the rhythm, not count milliseconds."
            )

            InstructionSection(
                title = "Gaps = Boundaries",
                body = "The silence after you release the button decides what happens:\n" +
                        "• Short gap (< 2U)  → next symbol of the same letter\n" +
                        "• Medium gap (2–5U) → letter complete; the decoder locks it in\n" +
                        "• Long gap (≥ 5U)   → word boundary (also commits the letter)"
            )

            InstructionSection(
                title = "Active Word",
                body = "One word is highlighted at a time. Key it letter-by-letter from left to right.\n" +
                        "When a word is complete it dims and the next word lights up."
            )

            InstructionSection(
                title = "Mistakes",
                body = "If the decoded letter doesn't match the expected one, you'll hear a buzz " +
                        "and the active word resets to its first letter. Retry as many times as needed — " +
                        "retry time counts towards your word timing."
            )

            InstructionSection(
                title = "Training Mode",
                body = "With Training Mode on, the Morse code pattern for each letter of the " +
                        "active word is shown as a dot/dash strip. Great for building muscle memory!"
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Sending")
            }
        }
    }
}

@Composable
private fun InstructionSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Gameplay screen ──────────────────────────────────────────────────────────

@Composable
private fun GameplayScreen(
    phrase: Phrase,
    wordIndex: Int,
    letterIndex: Int,
    completedLetters: List<LetterAttempt>,
    timing: MorseTiming,
    trainingMode: Boolean,
    isPenalty: Boolean,
    onLetterDecoded: (Char) -> Unit,
    onExit: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // Current Morse symbols being accumulated for the in-progress letter
    var currentSymbols by remember { mutableStateOf(listOf<Symbol>()) }
    var commitJob by remember { mutableStateOf<Job?>(null) }
    var pressStartMs by remember { mutableStateOf(0L) }
    var isPressed by remember { mutableStateOf(false) }

    // Reset symbol buffer when letter/word changes
    LaunchedEffect(wordIndex, letterIndex) {
        currentSymbols = emptyList()
        commitJob?.cancel()
    }

    // Cancel and reset buffer when penalty starts
    LaunchedEffect(isPenalty) {
        if (isPenalty) {
            commitJob?.cancel()
            currentSymbols = emptyList()
        }
    }

    GameScaffold(
        title = "Morse Code",
        onExit = onExit,
        hud = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Word ${wordIndex + 1}/${phrase.words.size}",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    if (isPenalty) "⚠ Mistake — retry" else "U = ${timing.ditUnitMs} ms",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isPenalty) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Words row ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                phrase.words.forEachIndexed { idx, word ->
                    val isActive = idx == wordIndex
                    val isDone   = idx < wordIndex
                    val alpha    = when {
                        isActive -> 1f
                        isDone   -> 0.35f
                        else     -> 0.6f
                    }
                    Text(
                        text = word,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isActive && isPenalty -> MaterialTheme.colorScheme.error
                            isActive -> GameAccent
                            else     -> MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                        }
                    )
                }
            }

            // ── Code strip (Training Mode) ─────────────────────────────────
            if (trainingMode) {
                val activeWord = phrase.words.getOrElse(wordIndex) { "" }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    activeWord.forEachIndexed { idx, ch ->
                        val glyphs = MORSE[ch.uppercaseChar()]?.glyphs() ?: "?"
                        val isCurrentLetter = idx == letterIndex
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = ch.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isCurrentLetter) GameAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = glyphs,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isCurrentLetter) GameAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isCurrentLetter) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // ── Per-letter progress for active word ────────────────────────
            val activeWord = phrase.words.getOrElse(wordIndex) { "" }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                activeWord.forEachIndexed { idx, ch ->
                    val isDoneLetter = idx < completedLetters.size
                    val isCurrentLetter = idx == letterIndex
                    Text(
                        text = when {
                            isDoneLetter   -> "✓"
                            isCurrentLetter -> ch.toString()
                            else           -> "_"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isCurrentLetter) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isDoneLetter   -> GameSuccess
                            isCurrentLetter -> GameAccent
                            else           -> GameNeutral
                        }
                    )
                }
            }

            // ── Live symbol buffer ─────────────────────────────────────────
            Text(
                text = if (currentSymbols.isEmpty()) "·" else currentSymbols.glyphs(),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = GameNeutral,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            // ── TAP / KEY button ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(
                        when {
                            isPenalty -> GameHazard.copy(alpha = 0.4f)
                            isPressed -> GameAccent
                            else      -> GameCourt
                        }
                    )
                    .then(
                        if (!isPenalty) Modifier.pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    commitJob?.cancel()
                                    pressStartMs = System.currentTimeMillis()
                                    isPressed = true

                                    waitForUpOrCancellation()
                                    val pressMs = System.currentTimeMillis() - pressStartMs
                                    isPressed = false

                                    val symbol = if (pressMs < timing.ditUnitMs * 2L) Symbol.DIT else Symbol.DAH
                                    val updated = currentSymbols + symbol
                                    currentSymbols = updated

                                    // Schedule letter commit after 2U ms of silence
                                    val capturedSymbols = updated
                                    commitJob = scope.launch {
                                        delay(timing.ditUnitMs * 2L)
                                        letterFor(capturedSymbols)?.let { onLetterDecoded(it) }
                                        currentSymbols = emptyList()
                                        commitJob = null
                                    }
                                }
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPenalty) "⚠  Wait…" else if (isPressed) "▮" else "TAP / KEY",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isPenalty -> MaterialTheme.colorScheme.error
                        isPressed -> MaterialTheme.colorScheme.onPrimary
                        else      -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

// ── Stats screen ─────────────────────────────────────────────────────────────

@Composable
private fun StatsScreen(
    stats: PhraseStats,
    seed: Long,
    completedWords: List<WordAttempt>,
    onRetrain: () -> Unit,
    onExit: () -> Unit
) {
    GameScaffold(title = "Results", onExit = onExit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dim play area recap
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    completedWords.forEach { w ->
                        Text(
                            text = w.word,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            Divider()

            Text("Statistics", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            StatRow("WPM", "%.1f".format(stats.wpm))
            StatRow("Distinct letters", stats.distinctLetters.toString())
            StatRow("Easiest letter", "${stats.easiestLetter}  (${MORSE[stats.easiestLetter]?.glyphs() ?: "?"})")
            StatRow("Hardest letter",  "${stats.hardestLetter}  (${MORSE[stats.hardestLetter]?.glyphs() ?: "?"})")
            StatRow("Fastest word", "${stats.fastestWord.first}  –  ${stats.fastestWord.second} ms")
            StatRow("Slowest word", "${stats.slowestWord.first}  –  ${stats.slowestWord.second} ms")
            StatRow("Seed", seed.toString())

            Divider()

            Button(
                onClick = onRetrain,
                modifier = Modifier.fillMaxWidth(),
                enabled = completedWords.size >= 1,
                colors = ButtonDefaults.buttonColors(containerColor = GameAccent)
            ) {
                Text("Re-train hardest ${minOf(5, completedWords.size)} words")
            }

            OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Menu")
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
