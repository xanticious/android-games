package com.xanticious.androidgames.view.games.mastermind

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.mastermind.MastermindController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.mastermind.MastermindFeedback
import com.xanticious.androidgames.model.games.mastermind.MastermindState
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import com.xanticious.androidgames.ui.theme.PuzzleBoard
import com.xanticious.androidgames.ui.theme.PuzzleCell
import com.xanticious.androidgames.ui.theme.PuzzleGiven
import com.xanticious.androidgames.ui.theme.PuzzleGridLine
import com.xanticious.androidgames.ui.theme.PuzzleHighlight
import com.xanticious.androidgames.ui.theme.PuzzleHueBlue
import com.xanticious.androidgames.ui.theme.PuzzleHueGreen
import com.xanticious.androidgames.ui.theme.PuzzleHueOrange
import com.xanticious.androidgames.ui.theme.PuzzleHuePink
import com.xanticious.androidgames.ui.theme.PuzzleHueRed
import com.xanticious.androidgames.ui.theme.PuzzleHueTeal
import com.xanticious.androidgames.ui.theme.PuzzleHueViolet
import com.xanticious.androidgames.ui.theme.PuzzleHueYellow
import com.xanticious.androidgames.ui.theme.PuzzlePlayer
import com.xanticious.androidgames.ui.theme.PuzzlePlayerAlt
import com.xanticious.androidgames.ui.theme.PuzzleSolved
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.puzzle.OptionChips
import com.xanticious.androidgames.view.common.puzzle.PuzzleActionBar
import com.xanticious.androidgames.view.common.puzzle.PuzzleBoard
import com.xanticious.androidgames.view.common.puzzle.PuzzleHowToPlayScreen
import com.xanticious.androidgames.view.common.puzzle.PuzzleSettingsScreen
import kotlin.math.ceil

/**
 * Mastermind — crack the hidden color code within the guess limit.
 *
 * Black feedback pegs = right color, right position.
 * White feedback pegs = right color, wrong position (standard non-double-counting rule).
 *
 * Self-configured: owns Settings, How-to-Play, and board screens via the shared
 * [PuzzleStateMachine]. See `design/puzzle-games/mastermind/mastermind-design.md`.
 */
@Composable
fun MastermindGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { MastermindController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    // Settings state (pre-game)
    val (defaultLength, defaultColors, defaultDuplicates, defaultGuesses) = when (difficulty) {
        GameDifficulty.EASY -> Quad(4, 6, true, 10)
        GameDifficulty.MEDIUM -> Quad(5, 7, true, 10)
        GameDifficulty.HARD -> Quad(6, 8, false, 8)
    }
    var selectedLength by remember { mutableStateOf(defaultLength) }
    var selectedColors by remember { mutableStateOf(defaultColors) }
    var selectedDuplicates by remember { mutableStateOf(defaultDuplicates) }
    var selectedGuesses by remember { mutableStateOf(defaultGuesses) }

    // In-game state
    var state by remember {
        mutableStateOf(controller.newGame(defaultLength, defaultColors, defaultDuplicates, defaultGuesses))
    }
    var selectedColor by remember { mutableStateOf<Int?>(null) }

    fun deal() {
        state = controller.newGame(selectedLength, selectedColors, selectedDuplicates, selectedGuesses)
        selectedColor = null
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Mastermind", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Mastermind",
                onHowToPlay = machine::openHowToPlay,
                onStart = { deal(); machine.startGame() }
            ) {
                OptionChips(
                    label = "Code length",
                    options = controller.codeLengths,
                    selected = selectedLength,
                    onSelect = { selectedLength = it },
                    labelOf = { "$it pegs" }
                )
                OptionChips(
                    label = "Colors",
                    options = controller.colorCounts,
                    selected = selectedColors,
                    onSelect = { selectedColors = it },
                    labelOf = { "$it colors" }
                )
                OptionChips(
                    label = "Guess limit",
                    options = controller.guessLimits,
                    selected = selectedGuesses,
                    onSelect = { selectedGuesses = it },
                    labelOf = { "$it rows" }
                )
                OptionChips(
                    label = "Duplicates in code",
                    options = listOf(true, false),
                    selected = selectedDuplicates,
                    onSelect = { selectedDuplicates = it },
                    labelOf = { if (it) "Allowed" else "Off" }
                )
            }
        }

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Mastermind", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                Text("The game hides a secret code of colored pegs. Guess the code within the row limit.")
                Text("After each guess you receive feedback pegs beside the row:")
                Text("• Black peg = correct color in the correct position.")
                Text("• White peg = correct color in the wrong position.")
                Text("No peg = that color does not appear (or has already been fully accounted for).")
                Text("Pick a color from the palette, then tap a slot to place it. Tap a filled slot to clear it. Press Submit when the row is complete.")
                Text("Win by guessing the exact code. Lose if you run out of rows — the secret is then revealed.")
            }
        }

        else -> {
            val playing = phase == PuzzlePhase.PLAYING
            val solved = phase == PuzzlePhase.SOLVED
            val failed = phase == PuzzlePhase.FAILED
            val revealSecret = solved || failed

            fun handleSlotTap(pos: GridPos) {
                if (!playing) return
                // Row 0 is the secret row; rows 1..maxGuesses are guess rows.
                val guessRowIndex = pos.row - 1
                val isActiveRow = guessRowIndex == state.guessCount
                val isPegCol = pos.col < state.codeLength
                if (!isActiveRow || !isPegCol) return
                state = if (selectedColor != null) {
                    controller.setSlot(state, pos.col, selectedColor)
                } else {
                    controller.setSlot(state, pos.col, null)
                }
            }

            fun submitGuess() {
                val guess = state.currentGuess.map { it ?: return }
                state = controller.submit(state, guess)
                selectedColor = null
                when {
                    controller.isSolved(state) -> machine.solved()
                    controller.isLost(state) -> machine.failed()
                }
            }

            val secretLabel = state.secret.joinToString("-") { (it + 1).toString() }

            GameScaffold(
                title = "Mastermind",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "Guess ${state.guessCount + if (playing) 1 else 0} / ${state.maxGuesses}",
                        center = "${state.codeLength} pegs · ${state.colorCount} colors",
                        right = if (state.allowDuplicates) "Dups" else "Unique"
                    )
                },
                status = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        when {
                            solved -> VictoryPanel(
                                score = state.guessCount,
                                bestScore = state.guessCount,
                                stars = starsForGuesses(state.guessCount, state.maxGuesses),
                                onReplay = { deal(); machine.retry() },
                                onMenu = machine::newGame,
                                headline = "Cracked in ${state.guessCount}!",
                                primaryLabel = "Play Again"
                            )
                            failed -> DefeatPanel(
                                score = state.guessCount,
                                bestScore = state.guessCount,
                                onTryAgain = { deal(); machine.retry() },
                                onMenu = machine::newGame,
                                headline = "Out of guesses! Code: $secretLabel"
                            )
                            else -> {
                                ColorPalette(
                                    colorCount = state.colorCount,
                                    selectedColor = selectedColor,
                                    onColorSelected = { color ->
                                        selectedColor = if (selectedColor == color) null else color
                                    }
                                )
                                PuzzleActionBar(
                                    status = "Guess ${state.guessCount + 1} / ${state.maxGuesses}",
                                    onUndo = { state = controller.clearCurrentGuess(state) },
                                    undoEnabled = state.currentGuess.any { it != null },
                                    extras = {
                                        Button(
                                            onClick = ::submitGuess,
                                            enabled = state.isCurrentGuessFull
                                        ) { Text("Submit") }
                                    }
                                )
                            }
                        }
                    }
                }
            ) {
                MastermindBoard(
                    state = state,
                    revealSecret = revealSecret,
                    onCellTap = if (playing) ::handleSlotTap else null
                )
            }
        }
    }
}

// ── Board ────────────────────────────────────────────────────────────────────

@Composable
private fun MastermindBoard(
    state: MastermindState,
    revealSecret: Boolean,
    onCellTap: ((GridPos) -> Unit)?
) {
    val measurer = rememberTextMeasurer()
    // Row 0 = secret; rows 1..maxGuesses = guess rows.
    // Col 0..codeLength-1 = pegs; col codeLength = feedback.
    val totalRows = state.maxGuesses + 1
    val totalCols = state.codeLength + 1

    PuzzleBoard(
        rows = totalRows,
        cols = totalCols,
        drawGridLines = false,
        maxCellSize = 72.dp,
        onCellTap = onCellTap
    ) {
        val r = current.row
        val c = current.col
        val isSecretRow = r == 0
        val isFeedbackCol = c == state.codeLength
        val guessRowIndex = r - 1
        val isSubmittedRow = guessRowIndex >= 0 && guessRowIndex < state.guesses.size
        val isActiveRow = guessRowIndex == state.guessCount

        // Subtle background for current active row
        if (isActiveRow && !isFeedbackCol) {
            drawRect(
                color = PuzzleBoard.copy(alpha = 0.0f),
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
            )
        }

        when {
            // ── Feedback column ──────────────────────────────────────────────
            isFeedbackCol && isSecretRow -> {
                // Lock indicator (hidden pegs)
                if (!revealSecret) {
                    drawCircle(
                        color = PuzzleGridLine,
                        radius = cellSize * 0.15f,
                        center = center,
                        style = Stroke(width = cellSize * 0.04f)
                    )
                }
            }
            isFeedbackCol && isSubmittedRow -> {
                drawFeedbackPegs(
                    feedback = state.guesses[guessRowIndex].feedback,
                    codeLength = state.codeLength,
                    cellSize = cellSize,
                    topLeft = topLeft
                )
            }
            isFeedbackCol -> Unit // future rows: empty

            // ── Secret row ───────────────────────────────────────────────────
            isSecretRow -> {
                if (revealSecret) {
                    drawPeg(
                        color = pegColor(state.secret[c]),
                        label = (state.secret[c] + 1).toString(),
                        radius = cellSize * 0.38f,
                        cellSize = cellSize,
                        center = center,
                        measurer = measurer
                    )
                } else {
                    // Hidden peg
                    drawCircle(
                        color = PuzzleGridLine,
                        radius = cellSize * 0.38f,
                        center = center
                    )
                    val style = TextStyle(
                        color = PuzzleCell,
                        fontSize = (cellSize * 0.28f / density).sp,
                        fontWeight = FontWeight.Bold
                    )
                    val layout = measurer.measure(AnnotatedString("?"), style)
                    drawText(
                        layout,
                        topLeft = Offset(
                            center.x - layout.size.width / 2f,
                            center.y - layout.size.height / 2f
                        )
                    )
                }
            }

            // ── Submitted row ────────────────────────────────────────────────
            isSubmittedRow -> {
                val colorIndex = state.guesses[guessRowIndex].guess[c]
                drawPeg(
                    color = pegColor(colorIndex),
                    label = (colorIndex + 1).toString(),
                    radius = cellSize * 0.38f,
                    cellSize = cellSize,
                    center = center,
                    measurer = measurer
                )
            }

            // ── Current active row ───────────────────────────────────────────
            isActiveRow -> {
                val colorIndex = state.currentGuess[c]
                if (colorIndex != null) {
                    drawPeg(
                        color = pegColor(colorIndex),
                        label = (colorIndex + 1).toString(),
                        radius = cellSize * 0.38f,
                        cellSize = cellSize,
                        center = center,
                        measurer = measurer
                    )
                    // Highlight border for active row peg
                    drawCircle(
                        color = PuzzleSolved,
                        radius = cellSize * 0.4f,
                        center = center,
                        style = Stroke(width = cellSize * 0.05f)
                    )
                } else {
                    // Empty slot in active row — dashed outline
                    drawCircle(
                        color = PuzzleHighlight,
                        radius = cellSize * 0.38f,
                        center = center,
                        style = Stroke(width = cellSize * 0.05f)
                    )
                }
            }

            // ── Future empty rows ────────────────────────────────────────────
            else -> {
                drawCircle(
                    color = PuzzleGridLine,
                    radius = cellSize * 0.28f,
                    center = center,
                    style = Stroke(width = cellSize * 0.03f)
                )
            }
        }
    }
}

/** Draws a colored peg circle with an accessibility number label. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPeg(
    color: Color,
    label: String,
    radius: Float,
    cellSize: Float,
    center: Offset,
    measurer: androidx.compose.ui.text.TextMeasurer
) {
    drawCircle(color = color, radius = radius, center = center)
    val style = TextStyle(
        color = PuzzleCell,
        fontSize = (cellSize * 0.28f / density).sp,
        fontWeight = FontWeight.Bold
    )
    val layout = measurer.measure(AnnotatedString(label), style)
    drawText(
        layout,
        topLeft = Offset(
            center.x - layout.size.width / 2f,
            center.y - layout.size.height / 2f
        )
    )
}

/**
 * Draws the black/white feedback mini-peg cluster inside a single board cell.
 *
 * Pegs are arranged in 2 columns × ceil(n/2) rows. Black pegs are drawn first
 * (left-to-right, top-to-bottom), then white, then empty placeholders.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFeedbackPegs(
    feedback: MastermindFeedback,
    codeLength: Int,
    cellSize: Float,
    topLeft: Offset
) {
    val pegRadius = cellSize * 0.11f
    val colCount = 2
    val rowCount = ceil(codeLength / 2.0).toInt()
    val hStep = cellSize / (colCount + 1)
    val vStep = cellSize / (rowCount + 1)

    var index = 0
    for (row in 0 until rowCount) {
        for (col in 0 until colCount) {
            if (index >= codeLength) break
            val px = topLeft.x + hStep * (col + 1)
            val py = topLeft.y + vStep * (row + 1)
            val pegColor = when {
                index < feedback.black -> PuzzleGiven         // "black" key peg (dark aqua)
                index < feedback.black + feedback.white -> PuzzlePlayerAlt  // "white" key peg (light aqua)
                else -> PuzzleGridLine                         // empty placeholder
            }
            drawCircle(color = pegColor, radius = pegRadius, center = Offset(px, py))
            index++
        }
    }
}

// ── Color palette ────────────────────────────────────────────────────────────

@Composable
private fun ColorPalette(
    colorCount: Int,
    selectedColor: Int?,
    onColorSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(colorCount) { index ->
            val isSelected = selectedColor == index
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(pegColor(index))
                    .then(
                        if (isSelected) Modifier.border(3.dp, PuzzleSolved, CircleShape)
                        else Modifier
                    )
                    .clickable { onColorSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (index + 1).toString(),
                    color = PuzzleCell,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/** Maps a 0-based color index to its themed hue token. */
private fun pegColor(index: Int): Color = when (index % 8) {
    0 -> PuzzleHueRed
    1 -> PuzzleHueOrange
    2 -> PuzzleHueYellow
    3 -> PuzzleHueGreen
    4 -> PuzzleHueTeal
    5 -> PuzzleHueBlue
    6 -> PuzzleHueViolet
    7 -> PuzzleHuePink
    else -> PuzzlePlayer
}

/** Star rating: 3 for ≤ 1/3 of max guesses, 2 for ≤ 2/3, else 1. */
private fun starsForGuesses(guesses: Int, max: Int): Int = when {
    guesses <= max / 3 -> 3
    guesses <= max * 2 / 3 -> 2
    else -> 1
}

/** Tiny helper to destructure quadruples from `when` expressions. */
private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

private operator fun <A, B, C, D> Quad<A, B, C, D>.component1() = a
private operator fun <A, B, C, D> Quad<A, B, C, D>.component2() = b
private operator fun <A, B, C, D> Quad<A, B, C, D>.component3() = c
private operator fun <A, B, C, D> Quad<A, B, C, D>.component4() = d
