package com.xanticious.androidgames.controller.games.solitairepyramid

import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.Decks
import com.xanticious.androidgames.model.games.cards.deal
import com.xanticious.androidgames.model.games.solitairepyramid.CardLocation
import com.xanticious.androidgames.model.games.solitairepyramid.PyramidConfig
import com.xanticious.androidgames.model.games.solitairepyramid.PyramidGameState

/**
 * Pure-function rules engine for Pyramid Solitaire.
 *
 * Every public function takes the current [PyramidGameState] and returns a new
 * one (or null when the action is illegal).  No Android, Compose, or coroutine
 * imports — fully testable on the JVM.
 *
 * ## Pyramid index layout
 * Row r contains cards at indices `r*(r+1)/2 .. r*(r+1)/2 + r`.
 * Card at (row, col) is **covered** by (row+1, col) and (row+1, col+1).
 * Base-row cards (row 6, indices 21–27) have no covering cards.
 *
 * ## Matching rule
 * Two exposed cards whose [rank values][com.xanticious.androidgames.model.games.cards.Rank.value]
 * sum to 13 may be removed together.  A King (value 13) is removed alone.
 */
object PyramidRules {

    // -------------------------------------------------------------------------
    // Index helpers
    // -------------------------------------------------------------------------

    /** Row (0 = apex, 6 = base) for the card at linear [index]. */
    fun rowOf(index: Int): Int {
        var row = 0
        while ((row + 1) * (row + 2) / 2 <= index) row++
        return row
    }

    /** Column within its row for the card at linear [index]. */
    fun colOf(index: Int): Int {
        val row = rowOf(index)
        return index - row * (row + 1) / 2
    }

    /** Linear index for the card at pyramid position ([row], [col]). */
    fun indexAt(row: Int, col: Int): Int = row * (row + 1) / 2 + col

    /**
     * Returns the indices of the two cards that physically cover the card at
     * [index] (i.e., the row directly below it).  Returns `null` for base-row
     * cards (row 6), which are never covered.
     */
    fun coveringIndices(index: Int): Pair<Int, Int>? {
        val row = rowOf(index)
        if (row >= PyramidGameState.ROWS - 1) return null
        val col = colOf(index)
        val below = row + 1
        return indexAt(below, col) to indexAt(below, col + 1)
    }

    // -------------------------------------------------------------------------
    // Exposure
    // -------------------------------------------------------------------------

    /**
     * Returns `true` when the card at [index] is present and not covered by any
     * card below it (both covering slots have been removed, or it is on the
     * base row).
     */
    fun isExposed(pyramid: List<Card?>, index: Int): Boolean {
        if (pyramid[index] == null) return false
        val (a, b) = coveringIndices(index) ?: return true   // base row: always exposed
        return pyramid[a] == null && pyramid[b] == null
    }

    // -------------------------------------------------------------------------
    // Match predicates
    // -------------------------------------------------------------------------

    /** Two cards may be removed together when their rank values sum to 13. */
    fun canRemovePair(c1: Card, c2: Card): Boolean = c1.rank.value + c2.rank.value == 13

    /** A King (rank value 13) may be removed on its own. */
    fun canRemoveAlone(card: Card): Boolean = card.rank.value == 13

    // -------------------------------------------------------------------------
    // Deal
    // -------------------------------------------------------------------------

    /** Deals a fresh game from the 52-card deck shuffled with [seed]. */
    fun deal(seed: Long): PyramidGameState {
        val (pyramidCards, stockCards) = Decks.shuffled(seed).deal(PyramidGameState.SIZE)
        return PyramidGameState(
            pyramid = pyramidCards,  // all face-up (Decks.shuffled returns face-up cards)
            stock = stockCards,
            waste = emptyList(),
            selected = null,
            score = 0,
            stockCycles = 0,
            history = emptyList()
        )
    }

    // -------------------------------------------------------------------------
    // Card access
    // -------------------------------------------------------------------------

    private fun cardAt(state: PyramidGameState, location: CardLocation): Card? = when (location) {
        is CardLocation.Pyramid -> state.pyramid.getOrNull(location.index)
        CardLocation.Waste      -> state.waste.lastOrNull()
    }

    private fun isAccessible(state: PyramidGameState, location: CardLocation): Boolean = when (location) {
        is CardLocation.Pyramid -> isExposed(state.pyramid, location.index)
        CardLocation.Waste      -> state.waste.isNotEmpty()
    }

    // -------------------------------------------------------------------------
    // Player actions
    // -------------------------------------------------------------------------

    /**
     * Processes a tap on [location].
     *
     * - Tapping an inaccessible card is a no-op.
     * - Tapping a King removes it immediately (+5 score).
     * - Tapping with no prior selection selects the card.
     * - Tapping the already-selected card deselects it.
     * - Tapping a second card that pairs with the selection removes both (+10 score).
     * - Tapping a non-pairing second card replaces the selection.
     */
    fun selectCard(state: PyramidGameState, location: CardLocation): PyramidGameState {
        val card = cardAt(state, location) ?: return state
        if (!isAccessible(state, location)) return state

        // King: remove alone
        if (canRemoveAlone(card)) {
            val (newPyramid, newWaste) = applyRemovals(state, listOf(location))
            return state.copy(
                pyramid = newPyramid,
                waste = newWaste,
                selected = null,
                score = state.score + 5,
                history = state.history + state.toSnapshot()
            )
        }

        // No prior selection: select this card
        if (state.selected == null) {
            return state.copy(selected = location)
        }

        // Deselect if same card tapped again
        if (state.selected == location) {
            return state.copy(selected = null)
        }

        val prevCard = cardAt(state, state.selected) ?: return state.copy(selected = location)

        // Verify prior selection is still accessible (safety guard)
        if (!isAccessible(state, state.selected)) {
            return state.copy(selected = location)
        }

        return if (canRemovePair(prevCard, card)) {
            val (newPyramid, newWaste) = applyRemovals(state, listOf(state.selected, location))
            state.copy(
                pyramid = newPyramid,
                waste = newWaste,
                selected = null,
                score = state.score + 10,
                history = state.history + state.toSnapshot()
            )
        } else {
            // Replace selection with newly tapped card
            state.copy(selected = location)
        }
    }

    /**
     * Draws the top card from the stock onto the waste pile.
     * If the stock is empty, attempts to recycle the waste pile back into the
     * stock (subject to [config.maxRedeals]).
     * Returns `null` when no draw or recycle is possible.
     */
    fun drawFromStock(state: PyramidGameState, config: PyramidConfig): PyramidGameState? {
        if (state.stock.isEmpty()) {
            val canRecycle = state.waste.isNotEmpty() &&
                (config.maxRedeals < 0 || state.stockCycles < config.maxRedeals)
            if (!canRecycle) return null
            // Flip the waste pile back into the stock; clear any waste selection
            val newSelected = if (state.selected == CardLocation.Waste) null else state.selected
            return state.copy(
                stock = state.waste.reversed(),
                waste = emptyList(),
                stockCycles = state.stockCycles + 1,
                selected = newSelected,
                history = state.history + state.toSnapshot()
            )
        }
        val drawn = state.stock.first()
        return state.copy(
            stock = state.stock.drop(1),
            waste = state.waste + drawn,
            history = state.history + state.toSnapshot()
        )
    }

    /**
     * Reverts the game to the state captured by the last undo snapshot.
     * Returns `null` when there is no history to undo.
     */
    fun undo(state: PyramidGameState): PyramidGameState? {
        if (state.history.isEmpty()) return null
        val snap = state.history.last()
        return PyramidGameState(
            pyramid = snap.pyramid,
            stock = snap.stock,
            waste = snap.waste,
            selected = snap.selected,
            score = snap.score,
            stockCycles = snap.stockCycles,
            history = state.history.dropLast(1)
        )
    }

    // -------------------------------------------------------------------------
    // Win / lose detection
    // -------------------------------------------------------------------------

    /** The player wins when every pyramid slot has been cleared. */
    fun isWon(state: PyramidGameState): Boolean = state.pyramid.all { it == null }

    /**
     * Returns `true` when at least one legal move still exists:
     * - An exposed King (pyramid or waste) can be removed.
     * - Any pair of accessible cards (exposed pyramid + exposed pyramid, or
     *   exposed pyramid + waste top) whose ranks sum to 13.
     * - The stock can be drawn from (or the waste can be recycled).
     */
    fun hasLegalMoves(state: PyramidGameState, config: PyramidConfig): Boolean {
        val exposedCards = state.pyramid.indices
            .asSequence()
            .filter { isExposed(state.pyramid, it) }
            .mapNotNull { state.pyramid[it] }
            .toList()

        val wasteTop = state.waste.lastOrNull()
        val candidates = if (wasteTop != null) exposedCards + wasteTop else exposedCards

        if (candidates.any { canRemoveAlone(it) }) return true

        for (i in candidates.indices) {
            for (j in i + 1 until candidates.size) {
                if (canRemovePair(candidates[i], candidates[j])) return true
            }
        }

        if (state.stock.isNotEmpty()) return true
        val canRecycle = state.waste.isNotEmpty() &&
            (config.maxRedeals < 0 || state.stockCycles < config.maxRedeals)
        return canRecycle
    }

    // -------------------------------------------------------------------------
    // Hint
    // -------------------------------------------------------------------------

    /**
     * Finds the first available legal move, returned as the one or two
     * [CardLocation]s involved.  The second element is `null` for a lone King.
     * Returns `null` when no move exists.
     */
    fun findHint(state: PyramidGameState): Pair<CardLocation, CardLocation?>? {
        val exposedIndices = state.pyramid.indices.filter { isExposed(state.pyramid, it) }
        val wasteTop = state.waste.lastOrNull()

        // King in pyramid?
        for (idx in exposedIndices) {
            val c = state.pyramid[idx] ?: continue
            if (canRemoveAlone(c)) return CardLocation.Pyramid(idx) to null
        }
        // King as waste top?
        if (wasteTop != null && canRemoveAlone(wasteTop)) {
            return CardLocation.Waste to null
        }

        // Pair among exposed pyramid cards
        for (i in exposedIndices.indices) {
            val ci = state.pyramid[exposedIndices[i]] ?: continue
            for (j in i + 1 until exposedIndices.size) {
                val cj = state.pyramid[exposedIndices[j]] ?: continue
                if (canRemovePair(ci, cj)) {
                    return CardLocation.Pyramid(exposedIndices[i]) to CardLocation.Pyramid(exposedIndices[j])
                }
            }
            // Pair with waste top
            if (wasteTop != null && canRemovePair(ci, wasteTop)) {
                return CardLocation.Pyramid(exposedIndices[i]) to CardLocation.Waste
            }
        }

        return null
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Removes cards at the given [locations] from pyramid/waste, returning the
     * updated pyramid list and waste list.
     */
    private fun applyRemovals(
        state: PyramidGameState,
        locations: List<CardLocation>
    ): Pair<List<Card?>, List<Card>> {
        val newPyramid = state.pyramid.toMutableList()
        var newWaste = state.waste
        for (loc in locations) {
            when (loc) {
                is CardLocation.Pyramid -> newPyramid[loc.index] = null
                CardLocation.Waste      -> newWaste = newWaste.dropLast(1)
            }
        }
        return newPyramid to newWaste
    }
}
