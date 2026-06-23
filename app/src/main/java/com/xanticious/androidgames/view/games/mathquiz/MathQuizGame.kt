package com.xanticious.androidgames.view.games.mathquiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.mathquiz.MathQuizController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.mathquiz.MathAnswerResult
import com.xanticious.androidgames.model.games.mathquiz.MathDifficulty
import com.xanticious.androidgames.model.games.mathquiz.MathOperation
import com.xanticious.androidgames.model.games.mathquiz.MathQuestion
import com.xanticious.androidgames.model.games.mathquiz.MathQuizSession
import com.xanticious.androidgames.model.games.mathquiz.MathQuizSettings
import com.xanticious.androidgames.model.games.mathquiz.MathTimingMode
import com.xanticious.androidgames.state.games.mathquiz.MathQuizPhase
import com.xanticious.androidgames.state.games.mathquiz.MathQuizStateMachine
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Dark0
import com.xanticious.androidgames.ui.theme.Dark1
import com.xanticious.androidgames.ui.theme.GameAccent
import com.xanticious.androidgames.ui.theme.GameEnemy
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GamePlayer
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.GameScaffold
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Math Quiz — self-configured entry composable.
 *
 * The [difficulty] parameter seeds the initial difficulty preset shown in
 * Settings; the player can freely adjust it before starting.
 */
@Composable
fun MathQuizGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { MathQuizController() }
    val machine = remember { MathQuizStateMachine() }
    val phase by machine.phase.collectAsState()

    var settings by remember {
        mutableStateOf(
            MathQuizSettings(
                operations = setOf(MathOperation.ADDITION, MathOperation.SUBTRACTION),
                difficulties = when (difficulty) {
                    GameDifficulty.EASY -> setOf(MathDifficulty.EASY)
                    GameDifficulty.MEDIUM -> setOf(MathDifficulty.EASY, MathDifficulty.MEDIUM)
                    GameDifficulty.HARD -> setOf(MathDifficulty.MEDIUM, MathDifficulty.HARD, MathDifficulty.EXPERT)
                },
                timingMode = MathTimingMode.UNTIMED
            )
        )
    }
    var session by remember { mutableStateOf<MathQuizSession?>(null) }
    var currentQuestion by remember { mutableStateOf<MathQuestion?>(null) }
    var answerInput by rememberSaveable { mutableStateOf("") }
    var elapsedSeconds by rememberSaveable { mutableStateOf(0) }
    var questionStartMs by remember { mutableStateOf(0L) }
    var lastResult by remember { mutableStateOf<MathAnswerResult?>(null) }

    // Transition from IDLE to SETTINGS on first composition.
    LaunchedEffect(Unit) { machine.openGame() }

    LaunchedEffect(phase) {
        when (phase) {
            MathQuizPhase.GENERATING_QUESTION -> {
                val q = controller.generateQuestion(settings)
                currentQuestion = q
                machine.questionReady()
            }
            MathQuizPhase.AWAITING_ANSWER -> {
                questionStartMs = System.currentTimeMillis()
            }
            MathQuizPhase.SHOWING_FEEDBACK -> {
                delay(300L)
                val s = session
                val hasMore = when {
                    s == null -> false
                    settings.timingMode == MathTimingMode.COUNTDOWN ->
                        elapsedSeconds < settings.countdownMinutes * 60
                    else -> !controller.isSessionComplete(s)
                }
                machine.feedbackDone(hasMore)
            }
            else -> Unit
        }
    }

    // Timer ticks while the session is actively in progress.
    val timerActive = phase == MathQuizPhase.AWAITING_ANSWER ||
            phase == MathQuizPhase.GENERATING_QUESTION ||
            phase == MathQuizPhase.SHOWING_FEEDBACK

    LaunchedEffect(timerActive) {
        if (timerActive) {
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    // Trigger countdown expiry only from AWAITING_ANSWER state.
    LaunchedEffect(elapsedSeconds) {
        if (settings.timingMode == MathTimingMode.COUNTDOWN &&
            phase == MathQuizPhase.AWAITING_ANSWER &&
            elapsedSeconds >= settings.countdownMinutes * 60
        ) {
            machine.countdownExpired()
        }
    }

    when (phase) {
        MathQuizPhase.IDLE -> Unit

        MathQuizPhase.SETTINGS -> MathQuizSettingsScreen(
            settings = settings,
            onSettingsChange = { settings = it },
            onStart = {
                session = MathQuizSession(
                    settings = settings,
                    questions = emptyList(),
                    currentIndex = 0,
                    correctCount = 0,
                    totalScore = 0,
                    startTimeMs = System.currentTimeMillis()
                )
                elapsedSeconds = 0
                machine.startSession()
            },
            onBack = onExit
        )

        MathQuizPhase.GENERATING_QUESTION,
        MathQuizPhase.AWAITING_ANSWER,
        MathQuizPhase.SHOWING_FEEDBACK -> {
            val q = currentQuestion
            if (q != null) {
                MathQuizGameScreen(
                    settings = settings,
                    session = session,
                    question = q,
                    answerInput = answerInput,
                    elapsedSeconds = elapsedSeconds,
                    phase = phase,
                    lastResult = lastResult,
                    onKeyPress = { key ->
                        when {
                            key == "⌫" -> {
                                if (answerInput.isNotEmpty()) answerInput = answerInput.dropLast(1)
                            }
                            key == "✓" && answerInput.isNotEmpty() -> {
                                val answerTime = System.currentTimeMillis() - questionStartMs
                                val result = controller.checkAnswer(
                                    question = q,
                                    userAnswer = answerInput,
                                    answerTimeMs = answerTime,
                                    isFirstAttempt = true
                                )
                                lastResult = result
                                session = session?.let { controller.recordAnswer(it, result) }
                                answerInput = ""
                                if (result.isCorrect) machine.answerCorrect() else machine.answerIncorrect()
                            }
                            key == "✓" -> Unit
                            else -> answerInput += key
                        }
                    },
                    onExit = onExit
                )
            }
        }

        MathQuizPhase.RESULTS -> MathQuizResultsScreen(
            session = session,
            settings = settings,
            elapsedSeconds = elapsedSeconds,
            onPlayAgain = {
                session = MathQuizSession(
                    settings = settings,
                    questions = emptyList(),
                    currentIndex = 0,
                    correctCount = 0,
                    totalScore = 0,
                    startTimeMs = System.currentTimeMillis()
                )
                elapsedSeconds = 0
                machine.playAgain()
            },
            onAdjustSettings = { machine.adjustSettings() },
            onExit = onExit
        )
    }
}

// ── Settings screen ───────────────────────────────────────────────────────────

@Composable
private fun MathQuizSettingsScreen(
    settings: MathQuizSettings,
    onSettingsChange: (MathQuizSettings) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    val canStart = settings.operations.isNotEmpty() && settings.difficulties.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Dark0)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Math Quiz",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Aqua2
        )

        Text(
            text = "Operations",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf(MathOperation.ADDITION to "Addition +", MathOperation.SUBTRACTION to "Subtraction −")
                    .forEach { (op, label) ->
                        FilterChip(
                            selected = op in settings.operations,
                            onClick = {
                                val updated = if (op in settings.operations) settings.operations - op
                                else settings.operations + op
                                onSettingsChange(settings.copy(operations = updated))
                            },
                            label = { Text(label) }
                        )
                    }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf(MathOperation.MULTIPLICATION to "Multiplication ×", MathOperation.DIVISION to "Division ÷")
                    .forEach { (op, label) ->
                        FilterChip(
                            selected = op in settings.operations,
                            onClick = {
                                val updated = if (op in settings.operations) settings.operations - op
                                else settings.operations + op
                                onSettingsChange(settings.copy(operations = updated))
                            },
                            label = { Text(label) }
                        )
                    }
            }
        }

        Text(
            text = "Difficulty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            MathDifficulty.entries.forEach { diff ->
                FilterChip(
                    selected = diff in settings.difficulties,
                    onClick = {
                        val updated = if (diff in settings.difficulties) settings.difficulties - diff
                        else settings.difficulties + diff
                        onSettingsChange(settings.copy(difficulties = updated))
                    },
                    label = { Text(diff.displayLabel) }
                )
            }
        }

        Text(
            text = "Timing Mode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            MathTimingMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.timingMode == mode,
                    onClick = { onSettingsChange(settings.copy(timingMode = mode)) },
                    label = { Text(mode.displayLabel) }
                )
            }
        }

        when (settings.timingMode) {
            MathTimingMode.UNTIMED, MathTimingMode.STOPWATCH -> {
                Text(
                    text = "Questions: ${settings.questionCount}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Slider(
                    value = settings.questionCount.toFloat(),
                    onValueChange = { v ->
                        onSettingsChange(settings.copy(questionCount = v.roundToInt().coerceIn(5, 100)))
                    },
                    valueRange = 5f..100f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            MathTimingMode.COUNTDOWN -> {
                val minuteLabel = if (settings.countdownMinutes == 1) "minute" else "minutes"
                Text(
                    text = "Duration: ${settings.countdownMinutes} $minuteLabel",
                    style = MaterialTheme.typography.bodyLarge
                )
                Slider(
                    value = settings.countdownMinutes.toFloat(),
                    onValueChange = { v ->
                        onSettingsChange(settings.copy(countdownMinutes = v.roundToInt().coerceIn(1, 30)))
                    },
                    valueRange = 1f..30f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Button(onClick = onStart, enabled = canStart) { Text("Start") }
        }
    }
}

// ── Game screen ───────────────────────────────────────────────────────────────

@Composable
private fun MathQuizGameScreen(
    settings: MathQuizSettings,
    session: MathQuizSession?,
    question: MathQuestion,
    answerInput: String,
    elapsedSeconds: Int,
    phase: MathQuizPhase,
    lastResult: MathAnswerResult?,
    onKeyPress: (String) -> Unit,
    onExit: () -> Unit
) {
    val isFeedback = phase == MathQuizPhase.SHOWING_FEEDBACK
    val isCorrectFeedback = isFeedback && lastResult?.isCorrect == true
    val isIncorrectFeedback = isFeedback && lastResult?.isCorrect == false

    val answerBorderColor = when {
        isCorrectFeedback -> GameSuccess
        isIncorrectFeedback -> GameEnemy
        else -> GamePlayer
    }
    val answerBgColor = when {
        isCorrectFeedback -> GameSuccess.copy(alpha = 0.15f)
        isIncorrectFeedback -> GameEnemy.copy(alpha = 0.15f)
        else -> Dark0
    }
    val answerTextColor = when {
        isCorrectFeedback -> GameSuccess
        isIncorrectFeedback -> GameEnemy
        else -> Aqua2
    }

    // Show the correct answer during feedback so the player can learn from mistakes.
    val displayAnswer = if (isFeedback && lastResult != null) lastResult.correctAnswer else answerInput

    val timerColor = timerColorFor(settings, elapsedSeconds)
    val timerText = timerTextFor(settings, elapsedSeconds)
    val progressText = when (settings.timingMode) {
        MathTimingMode.COUNTDOWN -> ""
        else -> "Q: ${(session?.currentIndex ?: 0) + 1} / ${settings.questionCount}"
    }

    val showNegativeKey = settings.difficulties.any { it == MathDifficulty.HARD || it == MathDifficulty.EXPERT }

    GameScaffold(
        title = "Math Quiz",
        onExit = onExit,
        hud = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = progressText, fontWeight = FontWeight.Bold, color = GamePlayer)
                if (settings.timingMode != MathTimingMode.UNTIMED) {
                    Text(
                        text = timerText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = timerColor
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Dark1)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Question text — centered in the available space above the input area.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${question.expression} = ?",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Aqua2
                )
            }

            // Answer display field — read-only; flashes on feedback.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 2.dp, color = answerBorderColor, shape = RoundedCornerShape(8.dp))
                    .background(color = answerBgColor, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = displayAnswer.ifEmpty { " " },
                    style = MaterialTheme.typography.headlineMedium,
                    color = answerTextColor,
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            NumericKeypad(
                showNegativeKey = showNegativeKey,
                showFractionalKeys = question.canHaveFractionalAnswer,
                enabled = phase == MathQuizPhase.AWAITING_ANSWER,
                onKeyPress = onKeyPress
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ── Results screen ────────────────────────────────────────────────────────────

@Composable
private fun MathQuizResultsScreen(
    session: MathQuizSession?,
    settings: MathQuizSettings,
    elapsedSeconds: Int,
    onPlayAgain: () -> Unit,
    onAdjustSettings: () -> Unit,
    onExit: () -> Unit
) {
    val totalAnswered = session?.currentIndex ?: 0
    val correctCount = session?.correctCount ?: 0
    val totalScore = session?.totalScore ?: 0
    val accuracy = if (totalAnswered > 0) (correctCount * 100f / totalAnswered).roundToInt() else 0
    val avgTimeSeconds = if (totalAnswered > 0 && settings.timingMode != MathTimingMode.UNTIMED) {
        elapsedSeconds.toFloat() / totalAnswered
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Dark0)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Session Complete",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Aqua2
        )

        Spacer(modifier = Modifier.height(4.dp))

        ResultStatRow(label = "Score", value = "$totalScore pts")
        ResultStatRow(label = "Correct", value = "$correctCount / $totalAnswered")
        ResultStatRow(label = "Accuracy", value = "$accuracy%")
        if (avgTimeSeconds != null) {
            ResultStatRow(label = "Avg Time", value = "%.1f s".format(avgTimeSeconds))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) {
            Text("Play Again")
        }
        OutlinedButton(onClick = onAdjustSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Adjust Settings")
        }
        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text("Exit")
        }
    }
}

@Composable
private fun ResultStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Aqua2
        )
    }
}

// ── Numeric keypad ────────────────────────────────────────────────────────────

/**
 * Custom numeric keypad — no system keyboard involved.
 *
 * Layout:
 * ```
 * [7] [8] [9] [⌫]
 * [4] [5] [6] [−]   ← [−] only when [showNegativeKey]
 * [1] [2] [3] [✓]
 * [·] [0] [R] [ ]   ← [·]/space and [R] only when [showFractionalKeys]
 * ```
 * The last-row extras enable typing remainder-format answers (e.g. "7 R 2")
 * produced by Hard/Expert division questions.
 */
@Composable
private fun NumericKeypad(
    showNegativeKey: Boolean,
    showFractionalKeys: Boolean,
    enabled: Boolean,
    onKeyPress: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        KeypadRow(keys = listOf("7", "8", "9", "⌫"), enabled = enabled, onKeyPress = onKeyPress)
        KeypadRow(
            keys = listOf("4", "5", "6", if (showNegativeKey) "−" else ""),
            enabled = enabled,
            onKeyPress = onKeyPress
        )
        KeypadRow(keys = listOf("1", "2", "3", "✓"), enabled = enabled, onKeyPress = onKeyPress)

        // Bottom row: space and R for remainder answers; 0-digit always present.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(
                label = if (showFractionalKeys) "·" else "",
                modifier = Modifier.weight(1f).height(56.dp),
                enabled = enabled && showFractionalKeys,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onPress = { if (showFractionalKeys) onKeyPress(" ") }
            )
            KeypadButton(
                label = "0",
                modifier = Modifier.weight(1f).height(56.dp),
                enabled = enabled,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onPress = { onKeyPress("0") }
            )
            KeypadButton(
                label = if (showFractionalKeys) "R" else "",
                modifier = Modifier.weight(1f).height(56.dp),
                enabled = enabled && showFractionalKeys,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onPress = { if (showFractionalKeys) onKeyPress("R") }
            )
            // Empty placeholder to maintain grid alignment.
            Box(modifier = Modifier.weight(1f).height(56.dp))
        }
    }
}

@Composable
private fun KeypadRow(
    keys: List<String>,
    enabled: Boolean,
    onKeyPress: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEach { key ->
            if (key.isEmpty()) {
                // Empty cell keeps column widths uniform.
                Box(modifier = Modifier.weight(1f).height(56.dp))
            } else {
                val contentColor = when (key) {
                    "⌫" -> GameHazard
                    "✓" -> GameSuccess
                    "−" -> GameEnemy
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                KeypadButton(
                    label = key,
                    modifier = Modifier.weight(1f).height(56.dp),
                    enabled = enabled,
                    contentColor = contentColor,
                    onPress = { onKeyPress(key) }
                )
            }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    contentColor: Color,
    onPress: () -> Unit
) {
    Surface(
        onClick = onPress,
        modifier = modifier,
        enabled = enabled && label.isNotEmpty(),
        shape = RoundedCornerShape(8.dp),
        color = if (label.isEmpty()) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = contentColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun timerTextFor(settings: MathQuizSettings, elapsedSeconds: Int): String =
    when (settings.timingMode) {
        MathTimingMode.UNTIMED -> ""
        MathTimingMode.STOPWATCH -> formatElapsed(elapsedSeconds)
        MathTimingMode.COUNTDOWN -> {
            val remaining = (settings.countdownMinutes * 60 - elapsedSeconds).coerceAtLeast(0)
            formatElapsed(remaining)
        }
    }

@Composable
private fun timerColorFor(settings: MathQuizSettings, elapsedSeconds: Int): Color =
    when (settings.timingMode) {
        MathTimingMode.COUNTDOWN -> {
            val remaining = settings.countdownMinutes * 60 - elapsedSeconds
            when {
                remaining < 30 -> GameEnemy
                remaining < 60 -> GameHazard
                else -> GameAccent
            }
        }
        else -> GameAccent
    }

private fun formatElapsed(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

private val MathDifficulty.displayLabel: String
    get() = when (this) {
        MathDifficulty.EASY -> "Easy"
        MathDifficulty.MEDIUM -> "Medium"
        MathDifficulty.HARD -> "Hard"
        MathDifficulty.EXPERT -> "Expert"
    }

private val MathTimingMode.displayLabel: String
    get() = when (this) {
        MathTimingMode.UNTIMED -> "Untimed"
        MathTimingMode.STOPWATCH -> "Stopwatch"
        MathTimingMode.COUNTDOWN -> "Countdown"
    }
