package com.xanticious.androidgames.controller.games.solitairefreecell

import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.CardColor
import com.xanticious.androidgames.model.games.cards.Decks
import com.xanticious.androidgames.model.games.cards.Rank
import com.xanticious.androidgames.model.games.cards.Suit
import com.xanticious.androidgames.model.games.solitairefreecell.FreeCellState

/**
 * Pure FreeCell rules engine. All functions take immutable state and return new
 * state or null (invalid move). Zero Android or Compose imports.
 *
 * Tableau builds down in alternating colors. Free cells hold one card each.
 * Foundations build up Ace→King by suit. A supermove of N cards is legal when
 * N ≤ (emptyFreeCells + 1) × 2^(emptyColumnsExcludingDestination).
 */
object FreeCellController {

    // ── Dealing ───────────────────────────────────────────────────────────

    /**
     * Deal a shuffled 52-card deck across 8 tableau columns.
     * First 4 columns receive 7 cards each; last 4 receive 6 cards (52 = 4×7 + 4×6).
     * Cards are distributed left-to-right one at a time, all face up.
     */
    fun deal(seed: Long, freeCellCount: Int = 4): FreeCellState {
        val deck = Decks.shuffled(seed)
        val columns = Array(8) { mutableListOf<Card>() }
        deck.forEachIndexed { index, card -> columns[index % 8].add(card) }
        return FreeCellState(
            tableau = columns.map { it.toList() },
            freeCells = List(freeCellCount) { null },
            foundations = List(4) { emptyList() },
            moveCount = 0,
            seed = seed,
            freeCellCount = freeCellCount
        )
    }

    // ── Sequence validation ───────────────────────────────────────────────

    /**
     * True if cards from [fromRow] to the end of [column] form a valid
     * alternating-color descending sequence (each card one rank lower and
     * opposite color from the card above it).
     */
    fun isValidSequence(column: List<Card>, fromRow: Int): Boolean {
        if (fromRow < 0 || fromRow >= column.size) return false
        for (i in fromRow until column.size - 1) {
            val upper = column[i]
            val lower = column[i + 1]
            if (lower.rank.value != upper.rank.value - 1 || lower.color == upper.color) return false
        }
        return true
    }

    /**
     * Length of the longest valid alternating-color descending run
     * at the accessible (bottom) end of [column].
     */
    fun movableSequenceLength(column: List<Card>): Int {
        if (column.isEmpty()) return 0
        var length = 1
        var i = column.size - 1
        while (i > 0) {
            val card = column[i]
            val prev = column[i - 1]
            if (card.rank.value == prev.rank.value - 1 && card.color != prev.color) {
                length++
                i--
            } else break
        }
        return length
    }

    // ── Supermove capacity ────────────────────────────────────────────────

    /**
     * Maximum number of cards movable as a supermove.
     * Formula: (emptyFreeCells + 1) × 2^(emptyColumnsExcludingDest)
     *
     * The destination column, if empty, is NOT counted — it cannot stage
     * cards for itself. Caller must subtract 1 from the empty-column count
     * when the destination is empty.
     */
    fun supermoveCapacity(emptyFreeCells: Int, emptyColumnsExcludingDest: Int): Int =
        (emptyFreeCells + 1) * (1 shl emptyColumnsExcludingDest.coerceAtLeast(0))

    private fun supermoveCapacityForMove(state: FreeCellState, toCol: Int): Int {
        val emptyFreeCells = state.freeCells.count { it == null }
        val emptyColumns = state.tableau.count { it.isEmpty() }
        val emptyColumnsExcludingDest =
            if (state.tableau[toCol].isEmpty()) emptyColumns - 1 else emptyColumns
        return supermoveCapacity(emptyFreeCells, emptyColumnsExcludingDest)
    }

    // ── Foundation compatibility ──────────────────────────────────────────

    /**
     * True if [card] can legally be placed on [foundationPile].
     * Empty pile requires an Ace; otherwise the card must be the same suit
     * and exactly one rank higher than the current top.
     */
    fun canMoveToFoundation(card: Card, foundationPile: List<Card>): Boolean =
        if (foundationPile.isEmpty()) {
            card.rank == Rank.ACE
        } else {
            foundationPile.last().suit == card.suit &&
                foundationPile.last().rank.value + 1 == card.rank.value
        }

    // ── Tableau compatibility ─────────────────────────────────────────────

    /**
     * True if [card] can legally be placed on [targetColumn].
     * Empty column accepts any card. Non-empty column requires one rank lower
     * and opposite color.
     */
    fun canMoveToTableauColumn(card: Card, targetColumn: List<Card>): Boolean =
        if (targetColumn.isEmpty()) true
        else targetColumn.last().rank.value - 1 == card.rank.value &&
             targetColumn.last().color != card.color

    // ── Move: Tableau → Free Cell ─────────────────────────────────────────

    fun canMoveToFreeCell(state: FreeCellState, fromCol: Int, cellIndex: Int): Boolean {
        if (fromCol !in state.tableau.indices) return false
        if (cellIndex !in state.freeCells.indices) return false
        if (state.tableau[fromCol].isEmpty()) return false
        return state.freeCells[cellIndex] == null
    }

    fun moveToFreeCell(state: FreeCellState, fromCol: Int, cellIndex: Int): FreeCellState? {
        if (!canMoveToFreeCell(state, fromCol, cellIndex)) return null
        val card = state.tableau[fromCol].last()
        val newTableau = state.tableau.mapIndexed { i, col ->
            if (i == fromCol) col.dropLast(1) else col
        }
        val newFreeCells = state.freeCells.toMutableList().also { it[cellIndex] = card }
        return state.copy(
            tableau = newTableau,
            freeCells = newFreeCells,
            moveCount = state.moveCount + 1
        )
    }

    // ── Move: Tableau → Foundation ────────────────────────────────────────

    fun canMoveToFoundationFromTableau(state: FreeCellState, fromCol: Int): Boolean {
        if (fromCol !in state.tableau.indices) return false
        if (state.tableau[fromCol].isEmpty()) return false
        val card = state.tableau[fromCol].last()
        return canMoveToFoundation(card, state.foundations[card.suit.ordinal])
    }

    fun moveToFoundationFromTableau(state: FreeCellState, fromCol: Int): FreeCellState? {
        if (!canMoveToFoundationFromTableau(state, fromCol)) return null
        val card = state.tableau[fromCol].last()
        val suitIdx = card.suit.ordinal
        val newTableau = state.tableau.mapIndexed { i, col ->
            if (i == fromCol) col.dropLast(1) else col
        }
        val newFoundations = state.foundations.mapIndexed { i, pile ->
            if (i == suitIdx) pile + card else pile
        }
        return state.copy(
            tableau = newTableau,
            foundations = newFoundations,
            moveCount = state.moveCount + 1
        )
    }

    // ── Move: Tableau → Tableau ───────────────────────────────────────────

    /**
     * True if the sequence [fromRow..end] in column [fromCol] can move to [toCol].
     * Validates: legal alternating sequence, supermove capacity, and destination compatibility.
     */
    fun canMoveTableauToTableau(
        state: FreeCellState,
        fromCol: Int,
        fromRow: Int,
        toCol: Int
    ): Boolean {
        if (fromCol == toCol) return false
        if (fromCol !in state.tableau.indices || toCol !in state.tableau.indices) return false
        val srcCol = state.tableau[fromCol]
        if (fromRow !in srcCol.indices) return false
        if (!isValidSequence(srcCol, fromRow)) return false
        val seqLen = srcCol.size - fromRow
        if (seqLen > supermoveCapacityForMove(state, toCol)) return false
        return canMoveToTableauColumn(srcCol[fromRow], state.tableau[toCol])
    }

    fun moveTableauToTableau(
        state: FreeCellState,
        fromCol: Int,
        fromRow: Int,
        toCol: Int
    ): FreeCellState? {
        if (!canMoveTableauToTableau(state, fromCol, fromRow, toCol)) return null
        val sequence = state.tableau[fromCol].subList(fromRow, state.tableau[fromCol].size).toList()
        val newTableau = state.tableau.mapIndexed { i, col ->
            when (i) {
                fromCol -> col.subList(0, fromRow).toList()
                toCol -> col + sequence
                else -> col
            }
        }
        return state.copy(tableau = newTableau, moveCount = state.moveCount + 1)
    }

    // ── Move: Free Cell → Tableau ─────────────────────────────────────────

    fun canMoveFromFreeCellToTableau(state: FreeCellState, cellIndex: Int, toCol: Int): Boolean {
        if (cellIndex !in state.freeCells.indices) return false
        if (toCol !in state.tableau.indices) return false
        val card = state.freeCells[cellIndex] ?: return false
        return canMoveToTableauColumn(card, state.tableau[toCol])
    }

    fun moveFromFreeCellToTableau(state: FreeCellState, cellIndex: Int, toCol: Int): FreeCellState? {
        if (!canMoveFromFreeCellToTableau(state, cellIndex, toCol)) return null
        val card = state.freeCells[cellIndex]!!
        val newFreeCells = state.freeCells.toMutableList().also { it[cellIndex] = null }
        val newTableau = state.tableau.mapIndexed { i, col ->
            if (i == toCol) col + card else col
        }
        return state.copy(
            tableau = newTableau,
            freeCells = newFreeCells,
            moveCount = state.moveCount + 1
        )
    }

    // ── Move: Free Cell → Foundation ─────────────────────────────────────

    fun canMoveFromFreeCellToFoundation(state: FreeCellState, cellIndex: Int): Boolean {
        if (cellIndex !in state.freeCells.indices) return false
        val card = state.freeCells[cellIndex] ?: return false
        return canMoveToFoundation(card, state.foundations[card.suit.ordinal])
    }

    fun moveFromFreeCellToFoundation(state: FreeCellState, cellIndex: Int): FreeCellState? {
        if (!canMoveFromFreeCellToFoundation(state, cellIndex)) return null
        val card = state.freeCells[cellIndex]!!
        val suitIdx = card.suit.ordinal
        val newFreeCells = state.freeCells.toMutableList().also { it[cellIndex] = null }
        val newFoundations = state.foundations.mapIndexed { i, pile ->
            if (i == suitIdx) pile + card else pile
        }
        return state.copy(
            freeCells = newFreeCells,
            foundations = newFoundations,
            moveCount = state.moveCount + 1
        )
    }

    // ── Win / Loss detection ──────────────────────────────────────────────

    /** True when all 52 cards are on the four foundations. */
    fun isWon(state: FreeCellState): Boolean =
        state.foundations.sumOf { it.size } == 52

    /** True when at least one legal move remains. */
    fun hasLegalMoves(state: FreeCellState): Boolean {
        val hasEmptyFreeCell = state.freeCells.any { it == null }
        val hasEmptyColumn = state.tableau.any { it.isEmpty() }
        val hasNonEmptyTableau = state.tableau.any { it.isNotEmpty() }

        // A non-empty column can always move its top card to an empty free cell or empty column.
        if (hasNonEmptyTableau && (hasEmptyFreeCell || hasEmptyColumn)) return true

        // Check foundation and tableau-to-tableau for each top card.
        state.tableau.forEachIndexed { col, column ->
            if (column.isEmpty()) return@forEachIndexed
            if (canMoveToFoundationFromTableau(state, col)) return true
            state.tableau.indices.forEach { toCol ->
                if (toCol != col && canMoveTableauToTableau(state, col, column.size - 1, toCol))
                    return true
            }
        }

        // Check free-cell cards.
        state.freeCells.forEachIndexed { cellIdx, card ->
            if (card == null) return@forEachIndexed
            if (canMoveFromFreeCellToFoundation(state, cellIdx)) return true
            state.tableau.indices.forEach { toCol ->
                if (canMoveFromFreeCellToTableau(state, cellIdx, toCol)) return true
            }
        }

        return false
    }

    // ── Auto-move to foundations ──────────────────────────────────────────

    /**
     * Repeatedly move cards to their foundations whenever it is safe to do so.
     * Safe = rank ≤ 2, OR both opposite-color predecessor cards are already
     * on their foundations (classic FreeCell heuristic — the card will never
     * be needed back on the tableau).
     */
    fun autoMoveToFoundations(state: FreeCellState): FreeCellState {
        var current = state
        var changed = true
        while (changed) {
            changed = false
            for (col in current.tableau.indices) {
                val column = current.tableau[col]
                if (column.isEmpty()) continue
                val card = column.last()
                if (isSafeToAutoMove(card, current.foundations) &&
                    canMoveToFoundationFromTableau(current, col)
                ) {
                    current = moveToFoundationFromTableau(current, col) ?: continue
                    changed = true
                }
            }
            for (cellIdx in current.freeCells.indices) {
                val card = current.freeCells[cellIdx] ?: continue
                if (isSafeToAutoMove(card, current.foundations) &&
                    canMoveFromFreeCellToFoundation(current, cellIdx)
                ) {
                    current = moveFromFreeCellToFoundation(current, cellIdx) ?: continue
                    changed = true
                }
            }
        }
        return current
    }

    /**
     * A card (rank R, color C) is safe to auto-move when:
     * - R ≤ 2 (Aces and Twos are always safe), OR
     * - both opposite-color foundation piles have reached rank R-1
     *   (so this card will never need to return to the tableau).
     */
    private fun isSafeToAutoMove(card: Card, foundations: List<List<Card>>): Boolean {
        if (card.rank.value <= 2) return true
        val oppositeColor = if (card.color == CardColor.RED) CardColor.BLACK else CardColor.RED
        return Suit.entries
            .filter { it.color == oppositeColor }
            .all { suit -> foundations[suit.ordinal].size >= card.rank.value - 1 }
    }
}
