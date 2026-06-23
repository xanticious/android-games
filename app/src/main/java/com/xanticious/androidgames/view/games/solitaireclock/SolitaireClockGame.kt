package com.xanticious.androidgames.view.games.solitaireclock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.controller.games.solitaireclock.ClockSolitaireController
import com.xanticious.androidgames.controller.games.solitaireclock.FlipOutcome
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.solitaireclock.ClockMode
import com.xanticious.androidgames.model.games.solitaireclock.ClockPile
import com.xanticious.androidgames.state.games.solitaireclock.ClockSolitairePhase
import com.xanticious.androidgames.state.games.solitaireclock.ClockSolitaireStateMachine
import com.xanticious.androidgames.ui.theme.Aqua3
import com.xanticious.androidgames.ui.theme.Aqua4
import com.xanticious.androidgames.ui.theme.CardTableFelt
import com.xanticious.androidgames.view.common.DefeatPanel
import com.xanticious.androidgames.view.common.GameHud
import com.xanticious.androidgames.view.common.GameLoop
import com.xanticious.androidgames.view.common.GameScaffold
import com.xanticious.androidgames.view.common.VictoryPanel
import com.xanticious.androidgames.view.common.cards.CardAspectRatio
import com.xanticious.androidgames.view.common.cards.CardBackView
import com.xanticious.androidgames.view.common.cards.EmptyCardSlot
import com.xanticious.androidgames.view.common.cards.PlayingCardView
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Clock Solitaire — fortune-telling card solitaire against a stopwatch.
 *
 * Thirteen piles are arranged as a clock face (Ace=1 o'clock … Queen=12 o'clock,
 * Kings in the centre). Tap anywhere to flip the top card of the current pile and
 * route it to its matching hour; the goal is to turn all four Kings up last.
 *
 * [difficulty] → [ClockMode]: EASY = Always Possible (guaranteed winnable deal);
 * MEDIUM/HARD = Classic (random ~1/13 win chance).
 */
@Composable
fun SolitaireClockGame(difficulty: GameDifficulty, onExit: () -> Unit) {
    val mode = remember(difficulty) {
        if (difficulty == GameDifficulty.EASY) ClockMode.ALWAYS_POSSIBLE else ClockMode.CLASSIC
    }

    var state by remember {
        mutableStateOf(ClockSolitaireController.deal(System.currentTimeMillis(), mode))
    }
    val machine = remember { ClockSolitaireStateMachine() }
    val phase by machine.phase.collectAsState()

    // Best winning time this session (not persisted — would need Room/DataStore in a real app).
    var bestTime by rememberSaveable { mutableStateOf<Float?>(null) }

    LaunchedEffect(Unit) { machine.startGame() }

    // Advance the stopwatch one frame at a time while the game is live.
    GameLoop(running = phase == ClockSolitairePhase.PLAYING) { dt ->
        state = ClockSolitaireController.tick(state, dt)
    }

    fun handleFlip() {
        if (phase != ClockSolitairePhase.PLAYING) return
        val result = ClockSolitaireController.flip(state)
        state = result.state
        when (result.outcome) {
            FlipOutcome.WON -> {
                bestTime = bestTime?.coerceAtMost(result.state.elapsedSeconds)
                    ?: result.state.elapsedSeconds
                machine.gameWon()
            }
            FlipOutcome.LOST -> machine.gameLost()
            FlipOutcome.CONTINUE -> { /* timer continues */ }
        }
    }

    fun startNewDeal() {
        state = ClockSolitaireController.deal(System.currentTimeMillis(), mode)
        machine.replay()
    }

    GameScaffold(
        title = "Solitaire (Clock Timed)",
        onExit = onExit,
        hud = {
            GameHud(
                left = "⏱ ${ClockSolitaireController.formatTime(state.elapsedSeconds)}",
                center = "♛ Kings ${state.kingsUp}/4",
                right = "Best: ${bestTime?.let { ClockSolitaireController.formatTime(it) } ?: "--:--"}"
            )
        },
        status = {
            when (phase) {
                ClockSolitairePhase.WON ->
                    VictoryPanel(
                        score = state.elapsedSeconds.toInt(),
                        bestScore = (bestTime ?: state.elapsedSeconds).toInt(),
                        stars = 3,
                        onReplay = ::startNewDeal,
                        onMenu = onExit,
                        headline = "All Kings last! 🎉",
                        primaryLabel = "New Deal"
                    )
                ClockSolitairePhase.LOST ->
                    DefeatPanel(
                        score = state.kingsUp,
                        bestScore = 4,
                        onTryAgain = ::startNewDeal,
                        onMenu = onExit,
                        headline = "Kings came too early!"
                    )
                else -> { /* nothing shown while playing or paused */ }
            }
        }
    ) {
        ClockFaceBoard(
            piles = state.piles,
            activePileIndex = if (phase == ClockSolitairePhase.PLAYING) state.currentPileIndex else -1,
            lastTargetIndex = state.lastTargetIndex,
            onTap = ::handleFlip
        )
    }
}

// ── Clock face board ─────────────────────────────────────────────────────────

@Composable
private fun ClockFaceBoard(
    piles: List<com.xanticious.androidgames.model.games.solitaireclock.ClockPile>,
    activePileIndex: Int,
    lastTargetIndex: Int,
    onTap: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CardTableFelt)
            .clickable(onClick = onTap)
    ) {
        val availW: Dp = maxWidth
        val availH: Dp = maxHeight
        // Radius is sized so the outer piles never exceed the smaller screen dimension.
        val clockRadius: Dp = minOf(availW, availH) * 0.36f
        val cardWidth: Dp = clockRadius * 0.30f
        // Card height follows the standard 5:7 aspect ratio.
        val cardHeight: Dp = cardWidth / CardAspectRatio
        val centerX: Dp = availW / 2f
        val centerY: Dp = availH / 2f

        // ── Hour piles 0–11 around the clock face ──
        for (i in 0..11) {
            // Hour H = i + 1; angle: 0° = right, 90° = down (screen coords).
            val angleDeg = (i + 1) * 30.0 - 90.0
            val angle = angleDeg * PI / 180.0
            // Top-left corner of this card centered on the clock position.
            val pileX: Dp = centerX + clockRadius * cos(angle).toFloat() - cardWidth / 2f
            val pileY: Dp = centerY + clockRadius * sin(angle).toFloat() - cardHeight / 2f

            Box(modifier = Modifier.offset(x = pileX, y = pileY)) {
                ClockPileView(
                    pile = piles[i],
                    cardWidth = cardWidth,
                    isActive = i == activePileIndex,
                    isLastTarget = i == lastTargetIndex,
                    label = (i + 1).toString()
                )
            }
        }

        // ── Center pile (Kings) ──
        Box(
            modifier = Modifier.offset(
                x = centerX - cardWidth / 2f,
                y = centerY - cardHeight / 2f
            )
        ) {
            ClockPileView(
                pile = piles[12],
                cardWidth = cardWidth,
                isActive = 12 == activePileIndex,
                isLastTarget = 12 == lastTargetIndex,
                label = "K"
            )
        }
    }
}

// ── Single pile ───────────────────────────────────────────────────────────────

/**
 * Renders one Clock Solitaire pile:
 * - Face-down card back while the pile still has hidden cards; a count badge
 *   shows how many remain.
 * - The top face-up card once all hidden cards have been flipped out.
 * - An empty slot label when the pile has no cards at all (transient state).
 *
 * [isActive]: teal border — "tap to flip from here".
 * [isLastTarget]: accent border — "last card landed here".
 */
@Composable
private fun ClockPileView(
    pile: ClockPile,
    cardWidth: Dp,
    isActive: Boolean,
    isLastTarget: Boolean,
    label: String
) {
    val highlightColor: Color = when {
        isActive -> Aqua3
        isLastTarget -> Aqua4
        else -> Color.Transparent
    }
    val cardHeight = cardWidth / CardAspectRatio

    Box(
        modifier = Modifier
            .size(width = cardWidth, height = cardHeight)
            .then(
                if (highlightColor != Color.Transparent)
                    Modifier
                        .clip(RoundedCornerShape(10))
                        .background(highlightColor.copy(alpha = 0.30f))
                else Modifier
            )
    ) {
        when {
            pile.faceDownCards.isNotEmpty() ->
                CardBackView(modifier = Modifier.matchParentSize())

            pile.faceUpCards.isNotEmpty() ->
                PlayingCardView(
                    card = pile.faceUpCards.last(),
                    modifier = Modifier.matchParentSize()
                )

            else ->
                EmptyCardSlot(
                    modifier = Modifier.matchParentSize(),
                    label = label
                )
        }

        // Remaining face-down count badge.
        if (pile.faceDownCards.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp),
                color = Aqua4.copy(alpha = 0.88f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = pile.faceDownCards.size.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                )
            }
        }
    }
}
