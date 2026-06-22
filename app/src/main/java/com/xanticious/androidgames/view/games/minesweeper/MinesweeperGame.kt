package com.xanticious.androidgames.view.games.minesweeper

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.minesweeper.MinesweeperController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.minesweeper.CellMark
import com.xanticious.androidgames.model.games.minesweeper.MinesweeperDifficulty
import com.xanticious.androidgames.model.games.minesweeper.MinesweeperState
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.model.games.puzzle.starsFor
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import com.xanticious.androidgames.ui.theme.GameHazard
import com.xanticious.androidgames.ui.theme.GameNeutral
import com.xanticious.androidgames.ui.theme.PuzzleBoard
import com.xanticious.androidgames.ui.theme.PuzzleCell
import com.xanticious.androidgames.ui.theme.PuzzleGiven
import com.xanticious.androidgames.ui.theme.PuzzleHighlight
import com.xanticious.androidgames.ui.theme.PuzzleHueBlue
import com.xanticious.androidgames.ui.theme.PuzzleHueGreen
import com.xanticious.androidgames.ui.theme.PuzzleHueOrange
import com.xanticious.androidgames.ui.theme.PuzzleHuePink
import com.xanticious.androidgames.ui.theme.PuzzleHueRed
import com.xanticious.androidgames.ui.theme.PuzzleHueTeal
import com.xanticious.androidgames.ui.theme.PuzzleHueViolet
import com.xanticious.androidgames.ui.theme.PuzzleHueYellow
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.puzzle.DifficultyChips
import com.xanticious.androidgames.view.common.puzzle.HowToPlaySection
import com.xanticious.androidgames.view.common.puzzle.PuzzleActionBar
import com.xanticious.androidgames.view.common.puzzle.PuzzleBoard
import com.xanticious.androidgames.view.common.puzzle.PuzzleHowToPlayScreen
import com.xanticious.androidgames.view.common.puzzle.PuzzleSettingsScreen
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Minesweeper — reveal every safe cell without hitting a mine
 * (`design/puzzle-games/mine-sweeper`). Self-configured: owns its Settings,
 * How to Play, and board screens via the shared [PuzzleStateMachine].
 *
 * First-click safety: mines are placed by the controller after the first tap so
 * the first revealed cell is never a mine (and ideally opens an area).
 */
@Composable
fun MinesweeperGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { MinesweeperController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    val defaultDifficulty = when (difficulty) {
        GameDifficulty.EASY -> MinesweeperDifficulty.BEGINNER
        GameDifficulty.MEDIUM -> MinesweeperDifficulty.INTERMEDIATE
        GameDifficulty.HARD -> MinesweeperDifficulty.EXPERT
    }
    var selectedDifficulty by rememberSaveable { mutableStateOf(defaultDifficulty) }

    var state by remember {
        mutableStateOf(controller.newGame(
            defaultDifficulty.config.rows,
            defaultDifficulty.config.cols,
            defaultDifficulty.config.mines
        ))
    }

    // Timer: starts after first reveal (when mines are placed).
    var elapsedSeconds by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(state.minesPlaced, phase) {
        if (state.minesPlaced && phase == PuzzlePhase.PLAYING) {
            while (true) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    fun deal() {
        val cfg = selectedDifficulty.config
        state = controller.newGame(cfg.rows, cfg.cols, cfg.mines)
        elapsedSeconds = 0
    }

    fun handleTap(pos: GridPos) {
        var current = state
        if (!current.minesPlaced) {
            current = controller.placeMines(current, pos, Random.Default)
        }
        current = controller.reveal(current, pos)
        state = current
        when {
            controller.hitMine(current) -> machine.failed()
            controller.isSolved(current) -> machine.solved()
        }
    }

    fun handleLongPress(pos: GridPos) {
        state = controller.toggleFlag(state, pos)
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Minesweeper", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Minesweeper",
                onHowToPlay = machine::openHowToPlay,
                onStart = { deal(); machine.startGame() }
            ) {
                DifficultyChips(
                    selected = when (selectedDifficulty) {
                        MinesweeperDifficulty.BEGINNER -> GameDifficulty.EASY
                        MinesweeperDifficulty.INTERMEDIATE -> GameDifficulty.MEDIUM
                        MinesweeperDifficulty.EXPERT -> GameDifficulty.HARD
                    },
                    onSelect = { gameDifficulty ->
                        selectedDifficulty = when (gameDifficulty) {
                            GameDifficulty.EASY -> MinesweeperDifficulty.BEGINNER
                            GameDifficulty.MEDIUM -> MinesweeperDifficulty.INTERMEDIATE
                            GameDifficulty.HARD -> MinesweeperDifficulty.EXPERT
                        }
                    }
                )
                val cfg = selectedDifficulty.config
                Text("${cfg.rows}×${cfg.cols} grid · ${cfg.mines} mines")
            }
        }

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Minesweeper", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                HowToPlaySection(title = "Goal") {
                    Text("Reveal every cell that doesn't hide a mine. Flag suspected mines to keep track of them.")
                }
                HowToPlaySection(title = "Controls") {
                    Text("Tap a covered cell to reveal it.")
                    Text("Long-press a covered cell to place or remove a flag.")
                }
                HowToPlaySection(title = "Numbers") {
                    Text("A revealed number tells you how many of its 8 neighbors contain mines. A blank cell means zero — it automatically reveals all neighbors.")
                }
                HowToPlaySection(title = "Winning & Losing") {
                    Text("You win when all safe cells are revealed. You lose if you reveal a mine.")
                }
            }
        }

        else -> {
            val isSolved = phase == PuzzlePhase.SOLVED
            val isFailed = phase == PuzzlePhase.FAILED
            val active = phase == PuzzlePhase.PLAYING

            val minesRemaining = state.config.mines - state.flagCount
            val timeStr = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)

            GameScaffold(
                title = "Minesweeper",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "💣 $minesRemaining",
                        center = timeStr,
                        right = "✓ ${state.revealedCount}/${state.totalSafeCells}"
                    )
                },
                status = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        when {
                            isSolved -> {
                                val stars = starsFor(
                                    earned = true,
                                    good = elapsedSeconds <= state.config.mines * 3,
                                    great = elapsedSeconds <= state.config.mines
                                )
                                VictoryPanel(
                                    score = elapsedSeconds,
                                    bestScore = elapsedSeconds,
                                    stars = stars,
                                    onReplay = { deal(); machine.retry() },
                                    onMenu = machine::newGame,
                                    headline = "Cleared! 🎉",
                                    primaryLabel = "Play Again"
                                )
                            }
                            isFailed -> {
                                DefeatPanel(
                                    score = elapsedSeconds,
                                    bestScore = elapsedSeconds,
                                    onTryAgain = { deal(); machine.retry() },
                                    onMenu = machine::newGame,
                                    headline = "Boom! 💥"
                                )
                            }
                            else -> {
                                PuzzleActionBar(
                                    status = "${selectedDifficulty.label}",
                                    onNew = { deal() }
                                )
                            }
                        }
                    }
                }
            ) {
                MinesweeperBoard(
                    state = state,
                    phase = phase,
                    onCellTap = if (active) ::handleTap else null,
                    onCellLongPress = if (active) ::handleLongPress else null
                )
            }
        }
    }
}

@Composable
private fun MinesweeperBoard(
    state: MinesweeperState,
    phase: PuzzlePhase,
    onCellTap: ((GridPos) -> Unit)?,
    onCellLongPress: ((GridPos) -> Unit)?
) {
    val rows = state.config.rows
    val cols = state.config.cols
    val isFailed = phase == PuzzlePhase.FAILED
    val measurer = rememberTextMeasurer()

    PuzzleBoard(
        rows = rows,
        cols = cols,
        drawGridLines = true,
        maxCellSize = if (cols >= 20) 36.dp else 56.dp,
        onCellTap = onCellTap,
        onCellLongPress = onCellLongPress
    ) {
        val cell = state.cellAt(current)
        val inset = cellSize * 0.06f

        when {
            // On failure: always show mines; emphasize the hit mine.
            isFailed && cell.hasMine -> {
                val isHit = current == state.explodedPos
                drawRect(color = if (isHit) PuzzleHighlight else PuzzleCell,
                    topLeft = topLeft, size = cellSquare)
                val mineColor = if (isHit) GameHazard else GameNeutral
                drawCircle(color = mineColor, radius = cellSize * 0.28f, center = center)
                // Spikes: 4 lines through centre
                val r = cellSize * 0.38f
                for (angle in listOf(0f, 45f, 90f, 135f)) {
                    val rad = Math.toRadians(angle.toDouble()).toFloat()
                    val dx = r * kotlin.math.cos(rad)
                    val dy = r * kotlin.math.sin(rad)
                    drawLine(mineColor, center - Offset(dx, dy), center + Offset(dx, dy),
                        strokeWidth = cellSize * 0.06f)
                }
            }

            // Wrongly flagged cell on failure.
            isFailed && cell.mark == CellMark.FLAGGED -> {
                drawRoundRect(color = PuzzleHighlight,
                    topLeft = Offset(topLeft.x + inset, topLeft.y + inset),
                    size = Size(cellSize - inset * 2, cellSize - inset * 2),
                    cornerRadius = CornerRadius(cellSize * 0.12f))
                drawFlagShape(PuzzleHueRed)
            }

            // Flagged cell (normal play).
            cell.mark == CellMark.FLAGGED -> {
                drawRoundRect(color = PuzzleHighlight,
                    topLeft = Offset(topLeft.x + inset, topLeft.y + inset),
                    size = Size(cellSize - inset * 2, cellSize - inset * 2),
                    cornerRadius = CornerRadius(cellSize * 0.12f))
                drawFlagShape(GameHazard)
            }

            // Covered cell.
            cell.mark == CellMark.COVERED -> {
                drawRoundRect(color = PuzzleGiven,
                    topLeft = Offset(topLeft.x + inset, topLeft.y + inset),
                    size = Size(cellSize - inset * 2, cellSize - inset * 2),
                    cornerRadius = CornerRadius(cellSize * 0.12f))
            }

            // Revealed cell with a number.
            cell.neighborCount > 0 -> {
                drawRect(color = PuzzleBoard, topLeft = topLeft, size = cellSquare)
                val numColor = numberColor(cell.neighborCount)
                val style = TextStyle(
                    color = numColor,
                    fontSize = (cellSize * 0.45f / density).sp,
                    fontWeight = FontWeight.Bold
                )
                val layout = measurer.measure(AnnotatedString(cell.neighborCount.toString()), style)
                drawText(layout, topLeft = Offset(
                    center.x - layout.size.width / 2f,
                    center.y - layout.size.height / 2f
                ))
            }

            // Revealed blank cell.
            else -> drawRect(color = PuzzleCell, topLeft = topLeft, size = cellSquare)
        }
    }
}

/** Draws a simple flag: vertical pole + triangular pennant. */
private fun com.xanticious.androidgames.view.common.puzzle.PuzzleCellScope.drawFlagShape(color: androidx.compose.ui.graphics.Color) {
    val poleX = center.x - cellSize * 0.05f
    val poleTop = topLeft.y + cellSize * 0.18f
    val poleBottom = topLeft.y + cellSize * 0.78f
    // Pole
    drawLine(color, Offset(poleX, poleTop), Offset(poleX, poleBottom), strokeWidth = cellSize * 0.07f)
    // Triangular pennant
    val path = Path().apply {
        moveTo(poleX, poleTop)
        lineTo(poleX + cellSize * 0.38f, poleTop + cellSize * 0.18f)
        lineTo(poleX, poleTop + cellSize * 0.35f)
        close()
    }
    drawPath(path, color = color)
}

/** Maps a neighbor count (1–8) to a distinct palette color token. */
private fun numberColor(count: Int): androidx.compose.ui.graphics.Color = when (count) {
    1 -> PuzzleHueBlue
    2 -> PuzzleHueGreen
    3 -> PuzzleHueRed
    4 -> PuzzleHueViolet
    5 -> PuzzleHueOrange
    6 -> PuzzleHueTeal
    7 -> PuzzleHuePink
    else -> PuzzleHueYellow
}
