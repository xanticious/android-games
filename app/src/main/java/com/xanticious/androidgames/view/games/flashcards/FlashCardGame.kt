package com.xanticious.androidgames.view.games.flashcards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.flashcards.FlashCardController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.flashcards.FlashCard
import com.xanticious.androidgames.model.games.flashcards.FlashCardDuration
import com.xanticious.androidgames.model.games.flashcards.FlashCardMode
import com.xanticious.androidgames.model.games.flashcards.FlashCardPack
import com.xanticious.androidgames.model.games.flashcards.FlashCardPacks
import com.xanticious.androidgames.model.games.flashcards.FlashCardProgress
import com.xanticious.androidgames.model.games.flashcards.FlashCardSession
import com.xanticious.androidgames.model.games.flashcards.FlashCardSettings
import com.xanticious.androidgames.model.games.flashcards.SchoolLevel
import com.xanticious.androidgames.model.games.flashcards.ShowSide
import com.xanticious.androidgames.state.games.flashcards.FlashCardPhase
import com.xanticious.androidgames.state.games.flashcards.FlashCardStateMachine
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.GameSuccess
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * Flash Cards — entry composable.
 *
 * Self-configured game: owns its full settings flow (pack picker → settings → gameplay → results).
 * All phase transitions are driven by [FlashCardStateMachine]; card logic lives in
 * [FlashCardController].
 */
@Composable
fun FlashCardGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { FlashCardController() }
    val machine = remember { FlashCardStateMachine() }
    val phase by machine.phase.collectAsState()

    // ── Saveable configuration state (survives configuration changes) ─────────
    var selectedPackId by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // ── Session state (re-created naturally when a new session starts) ────────
    var appliedSettings by remember {
        mutableStateOf(FlashCardSettings("", ShowSide.FRONT, FlashCardDuration.FullDeck, FlashCardMode.QUIZ))
    }
    var session by remember { mutableStateOf<FlashCardSession?>(null) }
    var currentCard by remember { mutableStateOf<FlashCard?>(null) }
    var progressMap by remember { mutableStateOf<Map<String, FlashCardProgress>>(emptyMap()) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var lastResultCorrect by remember { mutableStateOf(false) }
    var reviewMissedMode by remember { mutableStateOf(false) }
    // Tracks which physical face of the card is shown first this draw.
    var firstSideIsFront by remember { mutableStateOf(true) }

    // ── Boot the state machine ────────────────────────────────────────────────
    LaunchedEffect(Unit) { machine.openGame() }

    // ── Real-time timer for NumberOfMinutes sessions ──────────────────────────
    // Keyed on session startTimeMs so it restarts only when a new session begins.
    LaunchedEffect(session?.startTimeMs) {
        val s = session ?: return@LaunchedEffect
        val dur = s.settings.duration
        if (dur !is FlashCardDuration.NumberOfMinutes) return@LaunchedEffect
        val limitMs = dur.m * 60_000L
        while (true) {
            delay(500L)
            val elapsed = System.currentTimeMillis() - s.startTimeMs
            elapsedMs = elapsed
            if (elapsed >= limitMs) {
                // Try to end the session from whichever showing phase we're in.
                when (machine.phase.value) {
                    FlashCardPhase.SHOWING_FRONT -> machine.sessionEnded()
                    FlashCardPhase.SHOWING_BACK -> {
                        // Force-record the current card as incorrect so RecordingResult
                        // can detect session completion naturally.
                        lastResultCorrect = false
                        machine.oops()
                    }
                    else -> Unit
                }
                break
            }
        }
    }

    // ── DRAWING_CARD: fetch next card or end session ──────────────────────────
    LaunchedEffect(phase) {
        if (phase != FlashCardPhase.DRAWING_CARD) return@LaunchedEffect
        val pack = FlashCardPacks.byId(appliedSettings.packId) ?: return@LaunchedEffect

        val currentSession: FlashCardSession = when {
            session == null -> {
                controller.startSession(pack, appliedSettings, progressMap, System.currentTimeMillis())
            }
            reviewMissedMode -> {
                val missedIds = session!!.incorrectCardIds.toSet()
                reviewMissedMode = false
                FlashCardSession(
                    settings = appliedSettings,
                    cards = pack.cards.filter { it.id in missedIds },
                    currentIndex = 0,
                    correctCardIds = emptyList(),
                    incorrectCardIds = emptyList(),
                    startTimeMs = System.currentTimeMillis(),
                    isComplete = false
                )
            }
            else -> session!!
        }

        session = currentSession
        val (updatedSession, card) = controller.drawNextCard(currentSession, progressMap)
        session = updatedSession

        if (card != null) {
            currentCard = card
            firstSideIsFront = when (appliedSettings.showSide) {
                ShowSide.FRONT -> true
                ShowSide.BACK -> false
                ShowSide.RANDOM -> Random.nextBoolean()
            }
            machine.cardReady()
        } else {
            machine.sessionComplete()
        }
    }

    // ── RECORDING_RESULT: persist result and advance ──────────────────────────
    LaunchedEffect(phase) {
        if (phase != FlashCardPhase.RECORDING_RESULT) return@LaunchedEffect
        val card = currentCard ?: return@LaunchedEffect
        val s = session ?: return@LaunchedEffect
        val correct = lastResultCorrect
        progressMap = progressMap + (card.id to controller.updateProgress(
            progressMap[card.id], card.id, correct, System.currentTimeMillis()
        ))
        session = controller.recordResult(s, card.id, correct)
        val updatedSession = session!!
        val elapsed = System.currentTimeMillis() - updatedSession.startTimeMs
        if (controller.isSessionComplete(updatedSession, elapsed)) {
            machine.sessionComplete()
        } else {
            machine.moreCards()
        }
    }

    // ── Screen routing ────────────────────────────────────────────────────────
    when (phase) {
        FlashCardPhase.IDLE, FlashCardPhase.PACK_PICKER -> {
            PackPickerScreen(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onPackSelected = { packId ->
                    selectedPackId = packId
                    appliedSettings = appliedSettings.copy(packId = packId)
                    machine.packSelected()
                },
                onExit = onExit
            )
        }

        FlashCardPhase.SETTINGS -> {
            val pack = FlashCardPacks.byId(selectedPackId)
            SettingsScreen(
                packName = pack?.name ?: "Flash Cards",
                initialShowSide = appliedSettings.showSide,
                initialMode = appliedSettings.mode,
                initialDuration = appliedSettings.duration,
                onStart = { showSide, mode, duration ->
                    appliedSettings = FlashCardSettings(selectedPackId, showSide, duration, mode)
                    session = null
                    machine.startSession()
                },
                onBack = machine::back
            )
        }

        FlashCardPhase.DRAWING_CARD, FlashCardPhase.RECORDING_RESULT -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Aqua2)
            }
        }

        FlashCardPhase.SHOWING_FRONT, FlashCardPhase.SHOWING_BACK -> {
            val card = currentCard
            val pack = FlashCardPacks.byId(appliedSettings.packId)
            val s = session
            if (card != null && pack != null && s != null) {
                CardFaceScreen(
                    packName = pack.name,
                    card = card,
                    showingFirstSide = phase == FlashCardPhase.SHOWING_FRONT,
                    firstSideIsFront = firstSideIsFront,
                    frontLabel = pack.frontLabel,
                    backLabel = pack.backLabel,
                    session = s,
                    settings = appliedSettings,
                    elapsedMs = elapsedMs,
                    onFlip = machine::flipTapped,
                    onGotIt = {
                        lastResultCorrect = true
                        machine.gotIt()
                    },
                    onOops = {
                        lastResultCorrect = false
                        machine.oops()
                    },
                    onExit = onExit
                )
            }
        }

        FlashCardPhase.RESULTS -> {
            val pack = FlashCardPacks.byId(appliedSettings.packId)
            val s = session
            if (pack != null && s != null) {
                ResultsScreen(
                    pack = pack,
                    session = s,
                    settings = appliedSettings,
                    onPlayAgain = {
                        session = null
                        machine.playAgain()
                    },
                    onReviewMissed = {
                        reviewMissedMode = true
                        machine.reviewMissed()
                    },
                    onBackToSettings = machine::backToSettings,
                    onExit = onExit
                )
            }
        }
    }
}

// ── Pack Picker ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackPickerScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onPackSelected: (String) -> Unit,
    onExit: () -> Unit
) {
    val allPacks = remember { FlashCardPacks.all }
    val filteredPacks = remember(searchQuery) {
        if (searchQuery.isBlank()) allPacks
        else allPacks.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val grouped = remember(filteredPacks) {
        filteredPacks.groupBy { it.schoolLevel }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flash Cards") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Clear, contentDescription = "Exit")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search packs…") },
                singleLine = true
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                SchoolLevel.entries.forEach { level ->
                    val packs = grouped[level] ?: emptyList()
                    if (packs.isNotEmpty()) {
                        item(key = "header_${level.name}") {
                            Text(
                                text = level.displayName(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Aqua2,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                            )
                        }
                        items(packs, key = { it.id }) { pack ->
                            PackItem(pack = pack, onClick = { onPackSelected(pack.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PackItem(pack: FlashCardPack, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = pack.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pack.description,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${pack.cards.size} cards",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// ── Settings ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    packName: String,
    initialShowSide: ShowSide,
    initialMode: FlashCardMode,
    initialDuration: FlashCardDuration,
    onStart: (showSide: ShowSide, mode: FlashCardMode, duration: FlashCardDuration) -> Unit,
    onBack: () -> Unit
) {
    var showSideIdx by rememberSaveable { mutableIntStateOf(initialShowSide.ordinal) }
    var modeIdx by rememberSaveable { mutableIntStateOf(initialMode.ordinal) }
    var durationTypeIdx by rememberSaveable {
        mutableIntStateOf(
            when (initialDuration) {
                is FlashCardDuration.FullDeck -> 0
                is FlashCardDuration.NumberOfCards -> 1
                is FlashCardDuration.NumberOfMinutes -> 2
            }
        )
    }
    var durationCards by rememberSaveable {
        mutableIntStateOf(
            if (initialDuration is FlashCardDuration.NumberOfCards) initialDuration.n else 20
        )
    }
    var durationMinutes by rememberSaveable {
        mutableIntStateOf(
            if (initialDuration is FlashCardDuration.NumberOfMinutes) initialDuration.m else 5
        )
    }

    val currentMode = FlashCardMode.entries[modeIdx]

    // FullDeck is only valid for QUIZ mode; fall back to NumberOfCards for other modes.
    LaunchedEffect(modeIdx) {
        if (currentMode != FlashCardMode.QUIZ && durationTypeIdx == 0) {
            durationTypeIdx = 1
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(packName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Clear, contentDescription = "Back to pack picker")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ── Show Side ─────────────────────────────────────────────────────
            Text(
                "Show Side First",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            listOf(ShowSide.FRONT to "Front First", ShowSide.BACK to "Back First", ShowSide.RANDOM to "Random")
                .forEach { (side, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = showSideIdx == side.ordinal,
                            onClick = { showSideIdx = side.ordinal }
                        )
                        Text(label, modifier = Modifier.padding(start = 4.dp))
                    }
                }

            // ── Mode ──────────────────────────────────────────────────────────
            Text(
                "Mode",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp)
            )
            listOf(
                FlashCardMode.QUIZ to "Quiz – full deck, track right / wrong",
                FlashCardMode.SHUFFLED to "Shuffled – deck in random order",
                FlashCardMode.RANDOM to "Random – uniform random pick each time",
                FlashCardMode.FOCUSED to "Focused – prioritises cards you miss most"
            ).forEach { (mode, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = modeIdx == mode.ordinal,
                        onClick = { modeIdx = mode.ordinal }
                    )
                    Text(label, modifier = Modifier.padding(start = 4.dp))
                }
            }

            // ── Duration ──────────────────────────────────────────────────────
            Text(
                "Duration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp)
            )
            val durationOptions = buildList {
                if (currentMode == FlashCardMode.QUIZ) add(0 to "Full Deck")
                add(1 to "Number of Cards")
                add(2 to "Number of Minutes")
            }
            durationOptions.forEach { (idx, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = durationTypeIdx == idx,
                        onClick = { durationTypeIdx = idx }
                    )
                    Text(label, modifier = Modifier.padding(start = 4.dp))
                }
            }

            when (durationTypeIdx) {
                1 -> {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Cards: $durationCards",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = durationCards.toFloat(),
                        onValueChange = { durationCards = ((it.roundToInt() / 5) * 5).coerceIn(5, 500) },
                        valueRange = 5f..500f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                2 -> {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Minutes: $durationMinutes",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = durationMinutes.toFloat(),
                        onValueChange = { durationMinutes = it.roundToInt().coerceIn(1, 60) },
                        valueRange = 1f..60f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val showSide = ShowSide.entries[showSideIdx]
                    val mode = FlashCardMode.entries[modeIdx]
                    val duration: FlashCardDuration = when (durationTypeIdx) {
                        1 -> FlashCardDuration.NumberOfCards(durationCards)
                        2 -> FlashCardDuration.NumberOfMinutes(durationMinutes)
                        else -> FlashCardDuration.FullDeck
                    }
                    onStart(showSide, mode, duration)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start")
            }
        }
    }
}

// ── Card Face (SHOWING_FRONT / SHOWING_BACK) ──────────────────────────────────

@Composable
private fun CardFaceScreen(
    packName: String,
    card: FlashCard,
    showingFirstSide: Boolean,
    firstSideIsFront: Boolean,
    frontLabel: String,
    backLabel: String,
    session: FlashCardSession,
    settings: FlashCardSettings,
    elapsedMs: Long,
    onFlip: () -> Unit,
    onGotIt: () -> Unit,
    onOops: () -> Unit,
    onExit: () -> Unit
) {
    val (displayText, displayLabel) = if (showingFirstSide) {
        if (firstSideIsFront) card.front to frontLabel else card.back to backLabel
    } else {
        if (firstSideIsFront) card.back to backLabel else card.front to frontLabel
    }

    val progressText = sessionProgressText(session, settings, elapsedMs)

    GameScaffold(
        title = "Flash Cards",
        onExit = onExit,
        hud = {
            GameHud(
                left = packName,
                center = "",
                right = progressText
            )
        },
        status = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = displayLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (showingFirstSide) {
                    OutlinedButton(
                        onClick = onFlip,
                        modifier = Modifier.fillMaxWidth(0.6f),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text("Flip")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onOops,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GameNeutral)
                        ) {
                            Text("Oops", color = MaterialTheme.colorScheme.surface)
                        }
                        Button(
                            onClick = onGotIt,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GameSuccess)
                        ) {
                            Text("Got It!")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ── Results ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultsScreen(
    pack: FlashCardPack,
    session: FlashCardSession,
    settings: FlashCardSettings,
    onPlayAgain: () -> Unit,
    onReviewMissed: () -> Unit,
    onBackToSettings: () -> Unit,
    onExit: () -> Unit
) {
    val correctCount = session.correctCardIds.size
    val totalAnswered = session.currentIndex
    val percentage = if (totalAnswered > 0) correctCount * 100 / totalAnswered else 0

    val missedCards = remember(session) {
        val missedIds = session.incorrectCardIds.toSet()
        pack.cards.filter { it.id in missedIds }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pack.name) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Clear, contentDescription = "Exit")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "$correctCount / $totalAnswered correct",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (percentage >= 80) GameSuccess else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (percentage >= 80) GameSuccess else MaterialTheme.colorScheme.secondary
            )

            if (missedCards.isNotEmpty()) {
                Text(
                    text = "Missed Cards",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                missedCards.forEach { card ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = card.front,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = card.back,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) {
                Text("Play Again")
            }

            if (missedCards.isNotEmpty() && settings.mode == FlashCardMode.QUIZ) {
                OutlinedButton(onClick = onReviewMissed, modifier = Modifier.fillMaxWidth()) {
                    Text("Review Missed (${missedCards.size})")
                }
            }

            OutlinedButton(onClick = onBackToSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Settings")
            }
        }
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

private fun sessionProgressText(
    session: FlashCardSession,
    settings: FlashCardSettings,
    elapsedMs: Long
): String = when (val dur = settings.duration) {
    is FlashCardDuration.FullDeck -> "${session.currentIndex + 1} / ${session.cards.size}"
    is FlashCardDuration.NumberOfCards -> "${session.currentIndex + 1} / ${dur.n}"
    is FlashCardDuration.NumberOfMinutes -> {
        val remainingMs = (dur.m * 60_000L - elapsedMs).coerceAtLeast(0L)
        val minutes = remainingMs / 60_000L
        val seconds = (remainingMs % 60_000L) / 1_000L
        "%d:%02d".format(minutes, seconds)
    }
}

private fun SchoolLevel.displayName(): String = when (this) {
    SchoolLevel.ELEMENTARY -> "Elementary"
    SchoolLevel.MIDDLE_SCHOOL -> "Middle School"
    SchoolLevel.HIGH_SCHOOL -> "High School"
    SchoolLevel.COLLEGE -> "College"
}
