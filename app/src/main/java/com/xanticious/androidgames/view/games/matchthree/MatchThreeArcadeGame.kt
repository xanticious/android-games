package com.xanticious.androidgames.view.games.matchthree

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.matchthree.MatchThreeController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.matchthree.MatchThreeBoard
import com.xanticious.androidgames.model.games.matchthree.MatchThreeState
import com.xanticious.androidgames.model.games.puzzle.GridPos
import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
import com.xanticious.androidgames.state.games.puzzle.PuzzleStateMachine
import com.xanticious.androidgames.ui.theme.PuzzleHueOrange
import com.xanticious.androidgames.ui.theme.PuzzleHueTeal
import com.xanticious.androidgames.ui.theme.PuzzleHueYellow
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.puzzle.OptionChips
import com.xanticious.androidgames.view.common.puzzle.PuzzleHowToPlayScreen
import com.xanticious.androidgames.view.common.puzzle.PuzzleSettingsScreen
import kotlin.random.Random

/**
 * Match Three (Arcade) — same swap/match/cascade rules as the Zen variant but
 * with a per-move countdown timer. Each valid swap resets the timer; the run ends
 * when time expires. Score = total valid swaps.
 * (`design/puzzle-games/match-three-arcade/match-three-arcade-design.md`)
 */
@Composable
fun MatchThreeArcadeGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { MatchThreeController() }
    val machine = remember { PuzzleStateMachine() }
    val phase by machine.phase.collectAsState()

    val defaultSize = when (difficulty) {
        GameDifficulty.EASY -> 6
        GameDifficulty.MEDIUM -> 7
        GameDifficulty.HARD -> 8
    }
    var boardSize by remember { mutableIntStateOf(defaultSize) }
    var gemTypes by remember { mutableIntStateOf(6) }
    var timerLimit by remember { mutableFloatStateOf(5f) }
    var cascadeBonus by remember { mutableStateOf(true) }

    var gameState by remember {
        mutableStateOf(MatchThreeState(board = MatchThreeBoard.empty(defaultSize, defaultSize)))
    }
    var timerRemaining by remember { mutableFloatStateOf(-1f) }
    var timerStarted by remember { mutableStateOf(false) }
    // Paused while cascades resolve (board is mid-resolution)
    var boardResolving by remember { mutableStateOf(false) }

    // Best score across runs in this session
    var bestScore by remember { mutableIntStateOf(0) }

    fun deal() {
        gameState = MatchThreeState(
            board = controller.newBoard(boardSize, boardSize, gemTypes, Random.Default),
            gemTypes = gemTypes
        )
        timerRemaining = -1f
        timerStarted = false
        boardResolving = false
    }

    // Countdown runs only while PLAYING, timer started, and board is stable
    val timerTicking = phase == PuzzlePhase.PLAYING && timerStarted && !boardResolving
    GameLoop(running = timerTicking) { dt ->
        timerRemaining -= dt
        if (timerRemaining <= 0f) {
            if (gameState.swapCount > bestScore) bestScore = gameState.swapCount
            machine.failed()
        }
    }

    when (phase) {
        PuzzlePhase.SETTINGS -> GameScaffold(title = "Match Three Arcade", onExit = onExit) {
            PuzzleSettingsScreen(
                title = "Match Three Arcade",
                onHowToPlay = machine::openHowToPlay,
                onStart = { deal(); machine.startGame() }
            ) {
                OptionChips(
                    label = "Board size",
                    options = controller.boardSizeOptions,
                    selected = boardSize,
                    onSelect = { boardSize = it },
                    labelOf = { "${it}×$it" }
                )
                OptionChips(
                    label = "Gem types",
                    options = controller.gemTypeOptions,
                    selected = gemTypes,
                    onSelect = { gemTypes = it },
                    labelOf = { "$it types" }
                )
                OptionChips(
                    label = "Move time",
                    options = listOf(3f, 5f, 8f, 10f),
                    selected = timerLimit,
                    onSelect = { timerLimit = it },
                    labelOf = { "${it.toInt()} s" }
                )
                OptionChips(
                    label = "Cascade bonus",
                    options = listOf(true, false),
                    selected = cascadeBonus,
                    onSelect = { cascadeBonus = it },
                    labelOf = { if (it) "On" else "Off" }
                )
            }
        }

        PuzzlePhase.HOW_TO_PLAY -> GameScaffold(title = "Match Three Arcade", onExit = onExit) {
            PuzzleHowToPlayScreen(title = "How to Play", onBack = machine::backToSettings) {
                Text("Swap adjacent gems to create matches of 3 or more — same rules as Match Three.")
                Text("After each valid swap, a countdown timer starts. Make your next valid swap before it hits zero or the run ends.")
                Text("Invalid swaps (no match) do not reset the timer — choose wisely.")
                Text("Cascades resolve automatically. With the cascade bonus on, each extra chain level adds 0.5 s.")
                Text("Your score is the total number of valid swaps. Survive as long as possible.")
            }
        }

        PuzzlePhase.FAILED -> {
            GameScaffold(
                title = "Match Three Arcade",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "Swaps: ${gameState.swapCount}",
                        center = "",
                        right = "Best: $bestScore"
                    )
                },
                status = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        DefeatPanel(
                            score = gameState.swapCount,
                            bestScore = bestScore,
                            onTryAgain = { deal(); machine.retry() },
                            onMenu = machine::newGame,
                            headline = "Time's Up!"
                        )
                    }
                }
            ) {
                GemBoard(
                    board = gameState.board,
                    selectedCell = null,
                    onCellTap = null
                )
            }
        }

        else -> {
            var selectedCell by remember { mutableStateOf<GridPos?>(null) }
            var dragStart by remember { mutableStateOf<GridPos?>(null) }

            fun performSwap(a: GridPos, b: GridPos) {
                boardResolving = true
                val result = controller.applySwap(
                    gameState.board, a, b, gameState.gemTypes, Random.Default
                )
                if (result.valid) {
                    val bonus = if (cascadeBonus)
                        controller.cascadeTimeBonus(result.cascadeCount)
                    else 0f
                    val newTimer = (timerLimit + bonus).coerceAtMost(timerLimit)
                    val needsReshuffle = !controller.hasValidMove(result.board)
                    val finalBoard = if (needsReshuffle)
                        controller.reshuffle(result.board, Random.Default)
                    else result.board
                    gameState = gameState.copy(
                        board = finalBoard,
                        selected = null,
                        totalCleared = gameState.totalCleared + result.totalCleared,
                        swapCount = gameState.swapCount + 1,
                        cascadeBest = maxOf(gameState.cascadeBest, result.cascadeCount)
                    )
                    timerRemaining = newTimer
                    timerStarted = true
                }
                boardResolving = false
            }

            fun onCellTap(pos: GridPos) {
                val sel = selectedCell
                when {
                    sel == null -> selectedCell = pos
                    sel == pos -> selectedCell = null
                    controller.areAdjacent(sel, pos) -> {
                        selectedCell = null
                        performSwap(sel, pos)
                    }
                    else -> selectedCell = pos
                }
            }

            GameScaffold(
                title = "Match Three Arcade",
                onExit = onExit,
                hud = {
                    GameHud(
                        left = "Swaps: ${gameState.swapCount}",
                        center = "${boardSize}×$boardSize",
                        right = "Best: $bestScore"
                    )
                    if (timerStarted) {
                        ArcadeTimerBar(
                            remaining = timerRemaining,
                            limit = timerLimit
                        )
                    }
                },
                status = {}
            ) {
                GemBoard(
                    board = gameState.board,
                    selectedCell = selectedCell,
                    onCellTap = ::onCellTap,
                    onCellDrag = { pos ->
                        if (dragStart == null) {
                            dragStart = pos
                        } else {
                            val start = dragStart!!
                            if (start != pos && controller.areAdjacent(start, pos)) {
                                dragStart = null
                                selectedCell = null
                                performSwap(start, pos)
                            }
                        }
                    },
                    onDragEnd = { dragStart = null }
                )
            }
        }
    }
}

// ── Timer bar ─────────────────────────────────────────────────────────────────

/**
 * Full-width bar that depletes as the timer counts down. Colour shifts from
 * [PuzzleHueTeal] → [PuzzleHueYellow] → [PuzzleHueOrange] as time runs low.
 * Lives in the HUD slot — never over the board.
 */
@Composable
private fun ArcadeTimerBar(remaining: Float, limit: Float) {
    val fraction = (remaining / limit).coerceIn(0f, 1f)
    val barColor = when {
        fraction > 0.5f -> PuzzleHueTeal
        fraction > 0.25f -> PuzzleHueYellow
        else -> PuzzleHueOrange
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .height(8.dp)
            .background(
                color = barColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(4.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(8.dp)
                .background(barColor, RoundedCornerShape(4.dp))
        )
    }
}
