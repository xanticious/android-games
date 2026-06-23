package com.xanticious.androidgames.model.games.solitaireklondike

import com.xanticious.androidgames.model.games.cards.Card

/**
 * Configures a single Klondike deal. [drawCount] controls how many cards are
 * turned from the stock each time; [deckPasses] is the maximum number of times
 * the waste may be recycled back to stock ([INFINITE_PASSES] = unlimited).
 */
data class KlondikeConfig(
    val drawCount: Int,
    val deckPasses: Int
) {
    companion object {
        const val INFINITE_PASSES = Int.MAX_VALUE
    }
}

/**
 * Complete, immutable Klondike game state.
 *
 * - [tableau]: 7 columns; within each column index 0 = buried bottom card,
 *   last index = accessible top card. Face-down cards are unrevealed; the
 *   topmost card in each column is always face-up after dealing.
 * - [foundations]: 4 piles. Index 0 is the first pile started; any pile may
 *   accept any Ace to begin. Once a pile has a card its suit is locked.
 * - [stock]: face-down draw pile. Index 0 = next card to be drawn.
 * - [waste]: face-up discard. Index 0 = oldest card; [waste].last() is the
 *   accessible top card available for play.
 * - [passesUsed]: number of times waste has been recycled to stock.
 */
data class KlondikeState(
    val tableau: List<List<Card>>,
    val foundations: List<List<Card>>,
    val stock: List<Card>,
    val waste: List<Card>,
    val passesUsed: Int,
    val moves: Int,
    val config: KlondikeConfig
) {
    /** True once all 52 cards have been moved to the foundations. */
    val isWon: Boolean
        get() = foundations.sumOf { it.size } == 52
}

/** All legal player actions in a Klondike deal. */
sealed interface KlondikeMove {
    /** Turn the next [config.drawCount] cards from stock onto the waste. */
    data object DrawStock : KlondikeMove

    /** Flip the entire waste pile face-down back onto the stock (costs one pass). */
    data object RecycleStock : KlondikeMove

    /** Move the accessible waste-top card onto tableau column [col]. */
    data class WasteToTableau(val col: Int) : KlondikeMove

    /** Move the accessible waste-top card to foundation pile [foundIdx]. */
    data class WasteToFoundation(val foundIdx: Int) : KlondikeMove

    /** Move the face-up top card of tableau column [fromCol] to foundation [foundIdx]. */
    data class TableauToFoundation(val fromCol: Int, val foundIdx: Int) : KlondikeMove

    /**
     * Move the sub-sequence starting at [startIndex] in tableau column [fromCol]
     * onto the top of tableau column [toCol]. All cards from [startIndex] to the
     * end of [fromCol] must be face-up and form a valid descending alternating-color run.
     */
    data class TableauToTableau(val fromCol: Int, val startIndex: Int, val toCol: Int) : KlondikeMove
}
