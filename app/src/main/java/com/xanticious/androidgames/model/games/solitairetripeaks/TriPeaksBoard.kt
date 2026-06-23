package com.xanticious.androidgames.model.games.solitairetripeaks

import com.xanticious.androidgames.model.games.cards.Card

/**
 * One of the 28 positions on the TriPeaks board.
 *
 * The board is laid out in four rows:
 * - Row 0 (3 cards):  peak tips at positions 0, 1, 2
 * - Row 1 (6 cards):  positions 3–8  (two below each tip)
 * - Row 2 (9 cards):  positions 9–17 (three below each Row-1 pair)
 * - Row 3 (10 cards): positions 18–27 (shared base row, all initially exposed)
 *
 * Total: 3 + 6 + 9 + 10 = 28 cards.
 */
data class BoardCard(
    val card: Card,
    val position: Int,
    val removed: Boolean = false,
)

/**
 * Complete, immutable snapshot of a TriPeaks game.
 *
 * [boardCards] holds all 28 peak positions; a card is "removed" once played to
 * the waste. [stock] feeds new waste cards; [waste] grows with each draw or
 * played card (top = last element). [combo] is the current unbroken chain length
 * (reset to 0 when drawing from stock). [timerSeconds] is only meaningful for
 * [TriPeaksVariant.TIMED].
 */
data class TriPeaksBoard(
    val boardCards: List<BoardCard>,
    val stock: List<Card>,
    val waste: List<Card>,
    val score: Int,
    /** Current chain length (0 = no active chain). */
    val combo: Int,
    val config: TriPeaksConfig,
    /** Remaining countdown in seconds. Only meaningful for TIMED. */
    val timerSeconds: Float = 0f,
) {
    /** The card currently on top of the waste pile (last element), or null if waste is empty. */
    val wasteTop: Card? get() = waste.lastOrNull()
}
