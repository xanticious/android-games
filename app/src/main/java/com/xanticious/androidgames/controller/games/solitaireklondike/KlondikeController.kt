package com.xanticious.androidgames.controller.games.solitaireklondike

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.Decks
import com.xanticious.androidgames.model.games.cards.Rank
import com.xanticious.androidgames.model.games.solitaireklondike.KlondikeConfig
import com.xanticious.androidgames.model.games.solitaireklondike.KlondikeMove
import com.xanticious.androidgames.model.games.solitaireklondike.KlondikeState

/**
 * Pure Klondike rules engine. Every function is stateless: model values in,
 * model values out. No Android, Compose, or uncontrolled randomness — callers
 * pass an explicit seed so deals are reproducible.
 */
object KlondikeController {

    // ── Configuration ────────────────────────────────────────────────────────

    /**
     * Map the selected [difficulty] to a [KlondikeConfig].
     * - Easy  : Draw-1, unlimited recycling
     * - Medium: Draw-1, 3 recycles
     * - Hard  : Draw-3, 1 recycle
     */
    fun configFor(difficulty: GameDifficulty): KlondikeConfig = when (difficulty) {
        GameDifficulty.EASY -> KlondikeConfig(drawCount = 1, deckPasses = KlondikeConfig.INFINITE_PASSES)
        GameDifficulty.MEDIUM -> KlondikeConfig(drawCount = 1, deckPasses = 3)
        GameDifficulty.HARD -> KlondikeConfig(drawCount = 3, deckPasses = 1)
    }

    // ── Deal ─────────────────────────────────────────────────────────────────

    /**
     * Deal a fresh Klondike layout from [seed].
     *
     * Column i receives i+1 cards; the top card of each column is flipped face-up;
     * all other cards remain face-down. The remaining 24 cards form the stock.
     */
    fun deal(seed: Long, config: KlondikeConfig): KlondikeState {
        var remaining = Decks.shuffledFaceDown(seed)
        val tableau = buildList {
            for (colSize in 1..7) {
                val col = remaining.take(colSize)
                remaining = remaining.drop(colSize)
                add(col.dropLast(1) + col.last().faceUp())
            }
        }
        return KlondikeState(
            tableau = tableau,
            foundations = List(4) { emptyList() },
            stock = remaining,          // 24 cards, already face-down
            waste = emptyList(),
            passesUsed = 0,
            moves = 0,
            config = config
        )
    }

    // ── Move validation helpers ───────────────────────────────────────────────

    /**
     * True when [card] may legally be placed on top of [foundation].
     * An empty pile accepts only an Ace; a non-empty pile requires the same suit
     * and the next rank up.
     */
    fun canMoveToFoundation(card: Card, foundation: List<Card>): Boolean =
        if (foundation.isEmpty()) card.rank == Rank.ACE
        else foundation.last().suit == card.suit &&
             foundation.last().rank.value + 1 == card.rank.value

    /**
     * True when [card] may legally be placed on top of [column].
     * An empty column accepts only a King; a non-empty column requires a face-up
     * top card of the next-higher rank and opposite color.
     */
    fun canMoveToTableau(card: Card, column: List<Card>): Boolean =
        if (column.isEmpty()) card.rank == Rank.KING
        else {
            val top = column.last()
            top.faceUp && top.rank.value - 1 == card.rank.value && top.color != card.color
        }

    /**
     * Return the index of the foundation pile that legally accepts [card], or null.
     * A non-empty pile of the same suit is preferred over an empty pile so that
     * Aces always start the correct slot.
     */
    fun foundationIndexFor(card: Card, foundations: List<List<Card>>): Int? {
        // Prefer a pile already started for this suit (faster path for non-aces).
        val bySuit = foundations.indexOfFirst { pile ->
            pile.isNotEmpty() && pile.last().suit == card.suit &&
            pile.last().rank.value + 1 == card.rank.value
        }
        if (bySuit >= 0) return bySuit
        // Ace may open any empty pile.
        if (card.rank == Rank.ACE) return foundations.indexOfFirst { it.isEmpty() }.takeIf { it >= 0 }
        return null
    }

    // ── Move application ─────────────────────────────────────────────────────

    /**
     * Apply [move] to [state] and return the updated state, or null if the move
     * is illegal. A null return signals "snap back" to the view.
     */
    fun applyMove(state: KlondikeState, move: KlondikeMove): KlondikeState? = when (move) {
        is KlondikeMove.DrawStock -> drawStock(state)
        is KlondikeMove.RecycleStock -> recycleStock(state)
        is KlondikeMove.WasteToTableau -> wasteToTableau(state, move.col)
        is KlondikeMove.WasteToFoundation -> wasteToFoundation(state, move.foundIdx)
        is KlondikeMove.TableauToFoundation -> tableauToFoundation(state, move.fromCol, move.foundIdx)
        is KlondikeMove.TableauToTableau -> tableauToTableau(state, move.fromCol, move.startIndex, move.toCol)
    }

    private fun drawStock(state: KlondikeState): KlondikeState? {
        if (state.stock.isEmpty()) return null
        val n = state.config.drawCount.coerceAtMost(state.stock.size)
        val drawn = state.stock.take(n).map { it.faceUp() }
        return state.copy(
            stock = state.stock.drop(n),
            waste = state.waste + drawn,
            moves = state.moves + 1
        )
    }

    private fun recycleStock(state: KlondikeState): KlondikeState? {
        if (state.stock.isNotEmpty() || state.waste.isEmpty()) return null
        val limit = state.config.deckPasses
        if (limit != KlondikeConfig.INFINITE_PASSES && state.passesUsed >= limit) return null
        // Flip the entire waste face-down to create new stock. The card that was
        // on the physical bottom of the waste (waste[0], the oldest) ends up on
        // top of the new stock and will be drawn first — matching real-game behaviour.
        return state.copy(
            stock = state.waste.map { it.faceDown() },
            waste = emptyList(),
            passesUsed = state.passesUsed + 1,
            moves = state.moves + 1
        )
    }

    private fun wasteToTableau(state: KlondikeState, col: Int): KlondikeState? {
        if (col !in 0..6) return null
        val card = state.waste.lastOrNull() ?: return null
        if (!canMoveToTableau(card, state.tableau[col])) return null
        return state.copy(
            waste = state.waste.dropLast(1),
            tableau = state.tableau.mapIndexed { i, c -> if (i == col) c + card else c },
            moves = state.moves + 1
        )
    }

    private fun wasteToFoundation(state: KlondikeState, foundIdx: Int): KlondikeState? {
        if (foundIdx !in 0..3) return null
        val card = state.waste.lastOrNull() ?: return null
        if (!canMoveToFoundation(card, state.foundations[foundIdx])) return null
        return state.copy(
            waste = state.waste.dropLast(1),
            foundations = state.foundations.mapIndexed { i, f -> if (i == foundIdx) f + card else f },
            moves = state.moves + 1
        )
    }

    private fun tableauToFoundation(state: KlondikeState, fromCol: Int, foundIdx: Int): KlondikeState? {
        if (fromCol !in 0..6 || foundIdx !in 0..3) return null
        val col = state.tableau[fromCol]
        val card = col.lastOrNull()?.takeIf { it.faceUp } ?: return null
        if (!canMoveToFoundation(card, state.foundations[foundIdx])) return null
        val newCol = flipNewTop(col.dropLast(1))
        return state.copy(
            tableau = state.tableau.mapIndexed { i, c -> if (i == fromCol) newCol else c },
            foundations = state.foundations.mapIndexed { i, f -> if (i == foundIdx) f + card else f },
            moves = state.moves + 1
        )
    }

    private fun tableauToTableau(
        state: KlondikeState,
        fromCol: Int,
        startIndex: Int,
        toCol: Int
    ): KlondikeState? {
        if (fromCol !in 0..6 || toCol !in 0..6 || fromCol == toCol) return null
        val col = state.tableau[fromCol]
        if (startIndex !in col.indices) return null
        val moving = col.drop(startIndex)
        // The entire sub-sequence must be face-up.
        if (moving.any { !it.faceUp }) return null
        if (!canMoveToTableau(moving.first(), state.tableau[toCol])) return null
        val newFrom = flipNewTop(col.take(startIndex))
        return state.copy(
            tableau = state.tableau.mapIndexed { i, c ->
                when (i) {
                    fromCol -> newFrom
                    toCol -> c + moving
                    else -> c
                }
            },
            moves = state.moves + 1
        )
    }

    /** Flip the top card of [pile] face-up if it exists and is still face-down. */
    private fun flipNewTop(pile: List<Card>): List<Card> {
        if (pile.isEmpty() || pile.last().faceUp) return pile
        return pile.dropLast(1) + pile.last().faceUp()
    }

    // ── Win / stuck detection ─────────────────────────────────────────────────

    /** True when all 52 cards are on the foundations (game won). */
    fun isWon(state: KlondikeState): Boolean = state.isWon

    /**
     * True when at least one legal move exists. Used to detect a stuck game.
     * Checks in order: draw, recycle, waste-to-foundation, waste-to-tableau,
     * tableau-to-foundation, tableau-to-tableau.
     */
    fun hasLegalMove(state: KlondikeState): Boolean {
        if (state.stock.isNotEmpty()) return true
        val limit = state.config.deckPasses
        if (state.stock.isEmpty() && state.waste.isNotEmpty() &&
            (limit == KlondikeConfig.INFINITE_PASSES || state.passesUsed < limit)) return true

        val wasteTop = state.waste.lastOrNull()
        if (wasteTop != null) {
            if (foundationIndexFor(wasteTop, state.foundations) != null) return true
            if (state.tableau.any { canMoveToTableau(wasteTop, it) }) return true
        }

        // Check tableau tops and sequences using asSequence for chained filtering.
        val hasTableauMove = state.tableau.asSequence().withIndex().any { (colIdx, col) ->
            val firstFaceUpIdx = col.indexOfFirst { it.faceUp }.takeIf { it >= 0 } ?: return@any false
            col.asSequence().drop(firstFaceUpIdx).withIndex().any { (offset, card) ->
                val startIdx = firstFaceUpIdx + offset
                foundationIndexFor(card, state.foundations) != null ||
                state.tableau.asSequence().withIndex()
                    .filter { (di, _) -> di != colIdx }
                    .any { (_, dest) ->
                        // Only move the bottom-most face-up card in the sequence if moving whole run.
                        startIdx == firstFaceUpIdx && canMoveToTableau(card, dest) ||
                        // Or the single top card to a foundation is already caught above.
                        (startIdx == col.size - 1 && canMoveToTableau(card, dest))
                    }
            }
        }
        return hasTableauMove
    }

    // ── Hint ──────────────────────────────────────────────────────────────────

    /**
     * Suggest one helpful move. Prefers sending cards to foundations, then
     * uncovering face-down cards, then general tableau reshuffling.
     * Returns null when no move exists.
     */
    fun hint(state: KlondikeState): KlondikeMove? {
        // 1. Auto-move waste top to foundation.
        state.waste.lastOrNull()?.let { wt ->
            foundationIndexFor(wt, state.foundations)?.let { return KlondikeMove.WasteToFoundation(it) }
        }
        // 2. Auto-move tableau top to foundation.
        state.tableau.forEachIndexed { i, col ->
            col.lastOrNull()?.takeIf { it.faceUp }?.let { top ->
                foundationIndexFor(top, state.foundations)?.let {
                    return KlondikeMove.TableauToFoundation(i, it)
                }
            }
        }
        // 3. Move waste top to tableau to unblock.
        state.waste.lastOrNull()?.let { wt ->
            state.tableau.forEachIndexed { i, col ->
                if (canMoveToTableau(wt, col)) return KlondikeMove.WasteToTableau(i)
            }
        }
        // 4. Tableau-to-tableau: prefer moves that reveal a face-down card.
        for ((fromIdx, fromCol) in state.tableau.withIndex()) {
            val firstFaceUp = fromCol.indexOfFirst { it.faceUp }.takeIf { it >= 0 } ?: continue
            // Prefer exposing a face-down card by moving the full face-up run.
            if (firstFaceUp > 0) {
                val card = fromCol[firstFaceUp]
                for ((toIdx, toCol) in state.tableau.withIndex()) {
                    if (toIdx == fromIdx) continue
                    if (canMoveToTableau(card, toCol)) return KlondikeMove.TableauToTableau(fromIdx, firstFaceUp, toIdx)
                }
            }
        }
        // 5. Any tableau-to-tableau.
        for ((fromIdx, fromCol) in state.tableau.withIndex()) {
            val firstFaceUp = fromCol.indexOfFirst { it.faceUp }.takeIf { it >= 0 } ?: continue
            for (startIdx in firstFaceUp until fromCol.size) {
                val card = fromCol[startIdx]
                for ((toIdx, toCol) in state.tableau.withIndex()) {
                    if (toIdx == fromIdx) continue
                    if (canMoveToTableau(card, toCol)) return KlondikeMove.TableauToTableau(fromIdx, startIdx, toIdx)
                }
            }
        }
        // 6. Draw from stock.
        if (state.stock.isNotEmpty()) return KlondikeMove.DrawStock
        // 7. Recycle waste.
        val limit = state.config.deckPasses
        if (state.waste.isNotEmpty() &&
            (limit == KlondikeConfig.INFINITE_PASSES || state.passesUsed < limit))
            return KlondikeMove.RecycleStock
        return null
    }

    // ── Auto-finish ───────────────────────────────────────────────────────────

    /**
     * True when all face-down cards have been revealed and the stock is empty,
     * meaning the game can be completed automatically without further decisions.
     */
    fun canAutoFinish(state: KlondikeState): Boolean =
        state.stock.isEmpty() &&
        state.tableau.all { col -> col.all { it.faceUp } }

    /**
     * Greedily move every card to a foundation pile. Called after [canAutoFinish]
     * returns true. Returns the fully-won state.
     */
    fun autoFinish(state: KlondikeState): KlondikeState {
        var s = state
        var progress = true
        while (!s.isWon && progress) {
            progress = false

            // Drain waste top first.
            val wt = s.waste.lastOrNull()
            if (wt != null) {
                val fi = foundationIndexFor(wt, s.foundations)
                if (fi != null) {
                    s = wasteToFoundation(s, fi) ?: s
                    progress = true
                    continue
                }
            }

            // Move any tableau top.
            var moved = false
            for ((i, col) in s.tableau.withIndex()) {
                val top = col.lastOrNull()?.takeIf { it.faceUp } ?: continue
                val fi = foundationIndexFor(top, s.foundations) ?: continue
                s = tableauToFoundation(s, i, fi) ?: s
                moved = true
                progress = true
                break
            }
            if (moved) continue

            // Draw remaining stock cards when nothing on tableau can move yet.
            if (s.stock.isNotEmpty()) {
                s = drawStock(s) ?: s
                progress = true
            }
        }
        return s
    }
}
