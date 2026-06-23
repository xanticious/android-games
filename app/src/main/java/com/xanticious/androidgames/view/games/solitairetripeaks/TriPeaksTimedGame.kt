package com.xanticious.androidgames.view.games.solitairetripeaks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.games.solitairetripeaks.TriPeaksController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.solitairetripeaks.TriPeaksBoard
import com.xanticious.androidgames.model.games.solitairetripeaks.TriPeaksVariant
import com.xanticious.androidgames.state.games.solitairetripeaks.TriPeaksTimedPhase
import com.xanticious.androidgames.state.games.solitairetripeaks.TriPeaksTimedStateMachine
import com.xanticious.androidgames.ui.theme.Aqua2
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import kotlin.random.Random

/**
 * TIMED TriPeaks Solitaire — race-the-clock variant.
 *
 * Drives [TriPeaksController.tick] via [GameLoop] to drain the countdown timer
 * each frame. Clearing cards adds bonus seconds; building a chain adds extra.
 * The run ends when the timer hits zero. Victory is impossible (the board always
 * re-deals in continuous mode); the goal is the highest score before time runs out.
 *
 * Rendering is shared with the CLASSIC variant via [TriPeaksBoardContent].
 */
@Composable
fun TriPeaksTimedGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val controller = remember { TriPeaksController() }
    val config = remember(difficulty) { controller.configFor(TriPeaksVariant.TIMED, difficulty) }
    val machine = remember { TriPeaksTimedStateMachine() }
    val phase by machine.phase.collectAsState()

    var board by remember { mutableStateOf<TriPeaksBoard?>(null) }
    var seed by remember { mutableLongStateOf(Random.nextLong()) }

    // Deal on first launch.
    LaunchedEffect(Unit) {
        machine.startRun()
        board = controller.deal(seed, config)
        machine.dealt()
    }

    // Re-deal when the board is cleared (continuous mode).
    LaunchedEffect(phase) {
        if (phase == TriPeaksTimedPhase.DEALING) {
            seed = Random.nextLong()
            val currentBoard = board
            board = controller.deal(seed, config).copy(
                // Carry score and timer forward from the current board.
                score = currentBoard?.score ?: 0,
                timerSeconds = currentBoard?.timerSeconds ?: config.timerStartSeconds,
            )
            machine.dealt()
        }
    }

    // Tick the timer every frame while playing.
    GameLoop(running = phase == TriPeaksTimedPhase.PLAYING) { dt ->
        val b = board ?: return@GameLoop
        val ticked = controller.tick(b, dt)
        board = ticked
        if (controller.isLost(ticked)) {
            machine.timerExpired()
        }
    }

    val currentBoard = board ?: return

    GameScaffold(
        title = "Solitaire (TriPeaks Timed)",
        onExit = onExit,
        hud = {
            GameHud(
                left = "Stock: ${currentBoard.stock.size}",
                center = "Score: ${currentBoard.score}",
                right = if (currentBoard.combo > 0) "Chain ×${currentBoard.combo}" else "",
            )
        },
        status = {
            when (phase) {
                TriPeaksTimedPhase.PLAYING, TriPeaksTimedPhase.DEALING, TriPeaksTimedPhase.PAUSED -> {
                    TimerStrip(board = currentBoard, config = config)
                }
                TriPeaksTimedPhase.RUN_OVER -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        DefeatPanel(
                            score = currentBoard.score,
                            bestScore = currentBoard.score,
                            onTryAgain = {
                                seed = Random.nextLong()
                                board = controller.deal(seed, config)
                                machine.retry()
                            },
                            onMenu = onExit,
                            headline = "Time's Up!",
                        )
                    }
                }
                else -> {}
            }
        },
    ) {
        TriPeaksBoardContent(
            board = currentBoard,
            playing = phase == TriPeaksTimedPhase.PLAYING,
            onCardTap = { position ->
                val b = board ?: return@TriPeaksBoardContent
                if (phase == TriPeaksTimedPhase.PLAYING && controller.canPlay(b, position)) {
                    val played = controller.playCard(b, position)
                    board = played
                    machine.cardPlayed()
                    when {
                        controller.isWon(played) -> machine.boardCleared()
                        controller.isLost(played) -> machine.timerExpired()
                        else -> {}
                    }
                }
            },
            onStockTap = {
                val b = board ?: return@TriPeaksBoardContent
                if (phase == TriPeaksTimedPhase.PLAYING && b.stock.isNotEmpty()) {
                    val drawn = controller.draw(b)
                    board = drawn
                    machine.stockDrawn()
                    if (controller.isLost(drawn)) machine.timerExpired()
                }
            },
        )
    }
}

/**
 * Timer progress bar and remaining-time label in the status strip.
 * Color transitions from Aqua2 (plenty of time) to Aqua4 (nearly expired).
 */
@Composable
private fun TimerStrip(
    board: TriPeaksBoard,
    config: com.xanticious.androidgames.model.games.solitairetripeaks.TriPeaksConfig,
) {
    val fraction = (board.timerSeconds / config.timerStartSeconds).coerceIn(0f, 1f)
    val barColor = if (fraction > 0.3f) Aqua2 else Aqua4
    val seconds = board.timerSeconds.toInt()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                text = "${seconds}s",
                style = MaterialTheme.typography.labelMedium,
                color = barColor,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}
