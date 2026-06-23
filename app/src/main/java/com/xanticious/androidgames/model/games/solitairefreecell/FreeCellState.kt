package com.xanticious.androidgames.model.games.solitairefreecell

import com.xanticious.androidgames.model.games.cards.Card

/**
 * Immutable snapshot of a FreeCell game.
 *
 * [tableau] has 8 columns; index 0 is the leftmost.
 * [freeCells] has [freeCellCount] slots; null = empty.
 * [foundations] has 4 entries indexed by Suit.ordinal: CLUBS=0, DIAMONDS=1, HEARTS=2, SPADES=3.
 * Each foundation builds Ace→King for that suit.
 */
data class FreeCellState(
    val tableau: List<List<Card>>,
    val freeCells: List<Card?>,
    val foundations: List<List<Card>>,
    val moveCount: Int = 0,
    val seed: Long = 0L,
    val freeCellCount: Int = 4
)

/** Location of a card (or start of a sequence) the player may have selected. */
sealed class CardLocation {
    data class InTableau(val column: Int, val row: Int) : CardLocation()
    data class InFreeCell(val index: Int) : CardLocation()
}

/** Per-deal settings derived from [com.xanticious.androidgames.model.GameDifficulty]. */
data class FreeCellSettings(
    val freeCellCount: Int = 4,
    val autoMoveToFoundations: Boolean = true,
    val leftHanded: Boolean = false
)
