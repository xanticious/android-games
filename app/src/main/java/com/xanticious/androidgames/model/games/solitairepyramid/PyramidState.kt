package com.xanticious.androidgames.model.games.solitairepyramid

import com.xanticious.androidgames.model.games.cards.Card

/**
 * Identifies a card the player has tapped — either a specific slot in the
 * 28-card pyramid or the top card of the waste pile.
 */
sealed interface CardLocation {
    /** A pyramid slot at linear [index] 0–27. */
    data class Pyramid(val index: Int) : CardLocation
    /** The topmost card of the waste pile. */
    data object Waste : CardLocation
}

/**
 * A lightweight snapshot of all mutable fields, stored in [PyramidGameState.history]
 * so that undo can restore the previous state without carrying full history.
 */
data class PyramidSnapshot(
    val pyramid: List<Card?>,
    val stock: List<Card>,
    val waste: List<Card>,
    val selected: CardLocation?,
    val score: Int,
    val stockCycles: Int
)

/**
 * Complete, immutable snapshot of a Pyramid Solitaire game.
 *
 * ## Pyramid layout (28 positions)
 * ```
 * Row 0 (apex):                [0]
 * Row 1:                     [1][2]
 * Row 2:                   [3][4][5]
 * Row 3:                 [6][7][8][9]
 * Row 4:              [10][11][12][13][14]
 * Row 5:           [15][16][17][18][19][20]
 * Row 6 (base):  [21][22][23][24][25][26][27]
 * ```
 * Index formula: `row*(row+1)/2 + col`.
 * A card is **exposed** (playable) only when both cards that cover it from the row
 * below have been removed.  Base-row cards (21–27) are always exposed if present.
 *
 * [pyramid] has exactly [SIZE] entries; a `null` entry means the card was removed.
 * [stock] feeds new waste cards (first element = next to draw).
 * [waste] accumulates drawn cards (last element = current top).
 * [stockCycles] counts how many times the waste was recycled back into the stock.
 * [history] holds snapshots for undo; the most recent snapshot is the last element.
 */
data class PyramidGameState(
    val pyramid: List<Card?>,
    val stock: List<Card>,
    val waste: List<Card>,
    val selected: CardLocation?,
    val score: Int,
    val stockCycles: Int,
    val history: List<PyramidSnapshot>
) {
    /** Capture the current mutable fields for undo. */
    fun toSnapshot() = PyramidSnapshot(
        pyramid = pyramid,
        stock = stock,
        waste = waste,
        selected = selected,
        score = score,
        stockCycles = stockCycles
    )

    companion object {
        const val ROWS = 7
        const val SIZE = 28  // 1 + 2 + … + 7
    }
}
