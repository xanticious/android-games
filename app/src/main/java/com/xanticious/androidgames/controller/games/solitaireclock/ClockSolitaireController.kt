package com.xanticious.androidgames.controller.games.solitaireclock

import com.xanticious.androidgames.model.games.cards.Decks
import com.xanticious.androidgames.model.games.cards.Rank
import com.xanticious.androidgames.model.games.solitaireclock.ClockMode
import com.xanticious.androidgames.model.games.solitaireclock.ClockPile
import com.xanticious.androidgames.model.games.solitaireclock.ClockSolitaireState

/** Result of a single card flip. */
enum class FlipOutcome { CONTINUE, WON, LOST }

data class FlipResult(
    val state: ClockSolitaireState,
    val outcome: FlipOutcome
)

/**
 * Pure-function controller for Clock Solitaire. Takes state in, returns state
 * out. No Android, Compose, or randomness — deterministic given an explicit seed.
 *
 * Game rules:
 * - Flip the top face-down card of the current pile.
 * - Route the revealed card face-up to the pile matching its rank
 *   (Ace → index 0, Two → 1 … Queen → 11, King → 12 / center).
 * - The target pile becomes the new current pile.
 * - WIN: the 4th King is turned up AND every pile has no face-down cards left.
 * - LOSE: the 4th King is turned up while any pile still has face-down cards,
 *   OR the current pile has no face-down cards left with < 4 Kings up (stuck).
 */
object ClockSolitaireController {

    /**
     * Deal a new game. [ClockMode.CLASSIC] uses the raw shuffle of [seed].
     * [ClockMode.ALWAYS_POSSIBLE] increments [seed] until it finds a shuffle
     * that is provably winnable (the 4th King comes up last).
     */
    fun deal(seed: Long, mode: ClockMode): ClockSolitaireState {
        val rawState = when (mode) {
            ClockMode.CLASSIC -> dealRandom(seed)
            ClockMode.ALWAYS_POSSIBLE ->
                generateSequence(seed) { it + 1 }
                    .map { dealRandom(it) }
                    .first { isWinnable(it) }
        }
        return rawState.copy(mode = mode)
    }

    /**
     * Flip the top face-down card of the current pile and route it to its
     * matching pile. Returns the updated state and the flip outcome.
     */
    fun flip(state: ClockSolitaireState): FlipResult {
        val currentPile = state.piles[state.currentPileIndex]

        // Stuck: current pile has no face-down cards to flip.
        if (currentPile.faceDownCards.isEmpty()) {
            return FlipResult(state, FlipOutcome.LOST)
        }

        val card = currentPile.faceDownCards.last().faceUp()
        val targetIndex = card.rank.value - 1   // ACE=0 … KING=12
        val newKingsUp = if (card.rank == Rank.KING) state.kingsUp + 1 else state.kingsUp

        val newPiles = state.piles.mapIndexed { idx, pile ->
            when {
                // Self-routing: the card returns to the pile it came from (e.g. King in center).
                idx == state.currentPileIndex && idx == targetIndex ->
                    pile.copy(
                        faceDownCards = pile.faceDownCards.dropLast(1),
                        faceUpCards = pile.faceUpCards + card
                    )
                idx == state.currentPileIndex ->
                    pile.copy(faceDownCards = pile.faceDownCards.dropLast(1))
                idx == targetIndex ->
                    pile.copy(faceUpCards = pile.faceUpCards + card)
                else -> pile
            }
        }

        val newState = state.copy(
            piles = newPiles,
            currentPileIndex = targetIndex,
            kingsUp = newKingsUp,
            lastFlippedCard = card,
            lastTargetIndex = targetIndex
        )

        val outcome = when {
            newKingsUp == 4 && newPiles.all { it.faceDownCards.isEmpty() } -> FlipOutcome.WON
            newKingsUp == 4 -> FlipOutcome.LOST
            // Stuck: routed to a pile with no face-down cards left (incomplete or exhausted).
            newPiles[targetIndex].faceDownCards.isEmpty() -> FlipOutcome.LOST
            else -> FlipOutcome.CONTINUE
        }

        return FlipResult(newState, outcome)
    }

    /** Advance the stopwatch by [dtSeconds] without changing any game logic. */
    fun tick(state: ClockSolitaireState, dtSeconds: Float): ClockSolitaireState =
        state.copy(elapsedSeconds = state.elapsedSeconds + dtSeconds)

    /** Format elapsed seconds as "MM:SS" for the HUD. */
    fun formatTime(seconds: Float): String {
        val total = seconds.toInt()
        val m = total / 60
        val s = total % 60
        return "%02d:%02d".format(m, s)
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun dealRandom(seed: Long): ClockSolitaireState {
        val deck = Decks.shuffledFaceDown(seed)
        val piles = (0 until 13).map { pileIndex ->
            ClockPile(
                pileIndex = pileIndex,
                faceDownCards = deck.subList(pileIndex * 4, pileIndex * 4 + 4),
                faceUpCards = emptyList()
            )
        }
        return ClockSolitaireState(
            piles = piles,
            currentPileIndex = 12,  // always start at the center King pile
            kingsUp = 0,
            elapsedSeconds = 0f,
            mode = ClockMode.CLASSIC  // overwritten by deal()
        )
    }

    /**
     * Simulate a full game from [initialState] without any randomness.
     * Returns true iff the 4th King is the last card flipped (winning deal).
     */
    private fun isWinnable(initialState: ClockSolitaireState): Boolean {
        var state = initialState
        for (i in 0 until 52) {
            val result = flip(state)
            state = result.state
            when (result.outcome) {
                FlipOutcome.WON -> return true
                FlipOutcome.LOST -> return false
                FlipOutcome.CONTINUE -> { /* advance to next flip */ }
            }
        }
        return false
    }
}
