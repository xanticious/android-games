package com.xanticious.androidgames.model.games.solitaireclock

import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.Rank

/**
 * Whether the deal is guaranteed winnable (Always Possible) or a true random
 * shuffle (~1/13 chance of winning). Exposed via [ClockSolitaireState.mode].
 */
enum class ClockMode { ALWAYS_POSSIBLE, CLASSIC }

/**
 * One of the 13 piles in Clock Solitaire.
 *
 * [pileIndex] 0–11 map to the 1–12 o'clock positions (Ace through Queen);
 * index 12 is the center King pile.
 *
 * [faceDownCards]: cards waiting to be flipped, ordered bottom (index 0) to top
 * (last); the top is the next card available to flip.
 *
 * [faceUpCards]: correctly routed cards already revealed at this position, in the
 * order they arrived.
 */
data class ClockPile(
    val pileIndex: Int,
    val faceDownCards: List<Card>,
    val faceUpCards: List<Card>
) {
    /** The rank this pile collects (ACE for index 0 … KING for index 12). */
    val targetRank: Rank get() = Rank.entries[pileIndex]

    /** True when all four matching cards are face-up and none remain face-down. */
    val isComplete: Boolean get() = faceDownCards.isEmpty() && faceUpCards.size == 4

    val totalCards: Int get() = faceDownCards.size + faceUpCards.size
}

/**
 * Full snapshot of a Clock Solitaire game. Immutable; every player action
 * produces a new copy via the controller.
 */
data class ClockSolitaireState(
    val piles: List<ClockPile>,
    /** The pile the player must flip from on the next action. */
    val currentPileIndex: Int,
    /** How many Kings have been turned face-up so far (0–4). */
    val kingsUp: Int,
    val elapsedSeconds: Float,
    val mode: ClockMode,
    /** Most-recently revealed card (hints for UI animation). */
    val lastFlippedCard: Card? = null,
    /** Pile index where the last card landed (hints for UI highlight). */
    val lastTargetIndex: Int = -1
) {
    val faceDownRemaining: Int get() = piles.sumOf { it.faceDownCards.size }
}
