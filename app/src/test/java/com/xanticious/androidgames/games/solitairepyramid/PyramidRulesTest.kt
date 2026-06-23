package com.xanticious.androidgames.games.solitairepyramid

import com.xanticious.androidgames.controller.games.solitairepyramid.PyramidRules
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.Rank
import com.xanticious.androidgames.model.games.cards.Suit
import com.xanticious.androidgames.model.games.solitairepyramid.CardLocation
import com.xanticious.androidgames.model.games.solitairepyramid.PyramidConfig
import com.xanticious.androidgames.model.games.solitairepyramid.PyramidGameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PyramidRulesTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun card(rank: Rank, suit: Suit = Suit.CLUBS) = Card(rank, suit, faceUp = true)

    /** Build a state with a fully specified pyramid and optional stock/waste. */
    private fun stateWith(
        pyramid: List<Card?>,
        stock: List<Card> = emptyList(),
        waste: List<Card> = emptyList(),
        selected: CardLocation? = null,
        score: Int = 0,
        stockCycles: Int = 0
    ) = PyramidGameState(
        pyramid     = pyramid,
        stock       = stock,
        waste       = waste,
        selected    = selected,
        score       = score,
        stockCycles = stockCycles,
        history     = emptyList()
    )

    /** 28-card pyramid all filled with the given card. */
    private fun filledPyramid(c: Card = card(Rank.ACE)): List<Card?> = List(28) { c }

    /** 28-card pyramid all null (all removed). */
    private fun emptyPyramid(): List<Card?> = List(28) { null }

    // ─────────────────────────────────────────────────────────────────────────
    // Deal
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun deal_withSeed_pyramidHas28Cards() {
        val state = PyramidRules.deal(42L)
        assertEquals(28, state.pyramid.count { it != null })
    }

    @Test
    fun deal_withSeed_stockHas24Cards() {
        val state = PyramidRules.deal(42L)
        assertEquals(24, state.stock.size)
    }

    @Test
    fun deal_withSeed_initialWasteIsEmpty() {
        val state = PyramidRules.deal(42L)
        assertTrue(state.waste.isEmpty())
    }

    @Test
    fun deal_sameSeed_producesSamePyramid() {
        val s1 = PyramidRules.deal(99L)
        val s2 = PyramidRules.deal(99L)
        assertEquals(s1.pyramid, s2.pyramid)
    }

    @Test
    fun deal_differentSeeds_produceDifferentPyramids() {
        val s1 = PyramidRules.deal(1L)
        val s2 = PyramidRules.deal(2L)
        assertFalse(s1.pyramid == s2.pyramid)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Index helpers
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun indexAt_row0col0_returns0() {
        assertEquals(0, PyramidRules.indexAt(0, 0))
    }

    @Test
    fun indexAt_row6col6_returns27() {
        assertEquals(27, PyramidRules.indexAt(6, 6))
    }

    @Test
    fun indexAt_row6col0_returns21() {
        assertEquals(21, PyramidRules.indexAt(6, 0))
    }

    @Test
    fun rowOf_index27_returns6() {
        assertEquals(6, PyramidRules.rowOf(27))
    }

    @Test
    fun rowOf_index0_returns0() {
        assertEquals(0, PyramidRules.rowOf(0))
    }

    @Test
    fun coveringIndices_baseRow_returnsNull() {
        assertNull(PyramidRules.coveringIndices(21))  // index 21 = row 6, col 0
    }

    @Test
    fun coveringIndices_apexCard_returnsCoverers1And2() {
        val (a, b) = PyramidRules.coveringIndices(0)!!
        assertEquals(1, a)
        assertEquals(2, b)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exposure rules
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun isExposed_baseRowCardPresent_isExposed() {
        val pyramid = filledPyramid()
        assertTrue(PyramidRules.isExposed(pyramid, 21))  // row 6, col 0
    }

    @Test
    fun isExposed_apexCardBothCoveringPresent_isNotExposed() {
        val pyramid = filledPyramid()
        assertFalse(PyramidRules.isExposed(pyramid, 0))
    }

    @Test
    fun isExposed_apexCardOneCoveringRemoved_isStillBlocked() {
        val pyramid = filledPyramid().toMutableList().also { it[1] = null }
        assertFalse(PyramidRules.isExposed(pyramid, 0))
    }

    @Test
    fun isExposed_apexCardBothCoveringRemoved_isExposed() {
        val pyramid = filledPyramid().toMutableList().also {
            it[1] = null
            it[2] = null
        }
        assertTrue(PyramidRules.isExposed(pyramid, 0))
    }

    @Test
    fun isExposed_nullSlot_returnsFalse() {
        val pyramid = emptyPyramid()
        assertFalse(PyramidRules.isExposed(pyramid, 21))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pair predicates
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun canRemovePair_aceAndQueen_returnsTrue() {
        assertTrue(PyramidRules.canRemovePair(card(Rank.ACE), card(Rank.QUEEN)))
    }

    @Test
    fun canRemovePair_twoAndJack_returnsTrue() {
        assertTrue(PyramidRules.canRemovePair(card(Rank.TWO), card(Rank.JACK)))
    }

    @Test
    fun canRemovePair_sixAndSeven_returnsTrue() {
        assertTrue(PyramidRules.canRemovePair(card(Rank.SIX), card(Rank.SEVEN)))
    }

    @Test
    fun canRemovePair_aceAndKing_returnsFalse() {
        assertFalse(PyramidRules.canRemovePair(card(Rank.ACE), card(Rank.KING)))
    }

    @Test
    fun canRemoveAlone_king_returnsTrue() {
        assertTrue(PyramidRules.canRemoveAlone(card(Rank.KING)))
    }

    @Test
    fun canRemoveAlone_queen_returnsFalse() {
        assertFalse(PyramidRules.canRemoveAlone(card(Rank.QUEEN)))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // selectCard — basic selection
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun selectCard_exposedCard_setsSelection() {
        val state = stateWith(filledPyramid())   // bottom row exposed
        val result = PyramidRules.selectCard(state, CardLocation.Pyramid(21))
        assertEquals(CardLocation.Pyramid(21), result.selected)
    }

    @Test
    fun selectCard_unexposedCard_doesNotChange() {
        val state = stateWith(filledPyramid())   // index 0 = apex, covered
        val result = PyramidRules.selectCard(state, CardLocation.Pyramid(0))
        assertNull(result.selected)
    }

    @Test
    fun selectCard_sameLocation_deselects() {
        val state = stateWith(
            pyramid  = filledPyramid(),
            selected = CardLocation.Pyramid(21)
        )
        val result = PyramidRules.selectCard(state, CardLocation.Pyramid(21))
        assertNull(result.selected)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // selectCard — King removal
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun selectCard_exposedKingInPyramid_removesAlone() {
        // Put a King on the base row (always exposed)
        val pyramid = filledPyramid(card(Rank.TWO)).toMutableList()
        pyramid[21] = card(Rank.KING)
        val state = stateWith(pyramid)
        val result = PyramidRules.selectCard(state, CardLocation.Pyramid(21))
        assertNull(result.pyramid[21])
    }

    @Test
    fun selectCard_exposedKingInPyramid_scores5() {
        val pyramid = filledPyramid(card(Rank.TWO)).toMutableList()
        pyramid[21] = card(Rank.KING)
        val state = stateWith(pyramid)
        val result = PyramidRules.selectCard(state, CardLocation.Pyramid(21))
        assertEquals(5, result.score)
    }

    @Test
    fun selectCard_wasteKing_removesAlone() {
        val state = stateWith(
            pyramid = emptyPyramid(),
            waste   = listOf(card(Rank.KING))
        )
        val result = PyramidRules.selectCard(state, CardLocation.Waste)
        assertTrue(result.waste.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // selectCard — pair removal
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun selectCard_validPairBothPyramid_removesBoth() {
        // Bottom row (row 6) is always exposed; use index 21 and 22
        val pyramid = filledPyramid(card(Rank.TWO)).toMutableList()
        pyramid[21] = card(Rank.ACE)    // value 1
        pyramid[22] = card(Rank.QUEEN)  // value 12; 1+12 = 13
        val s0 = stateWith(pyramid)
        val s1 = PyramidRules.selectCard(s0, CardLocation.Pyramid(21))
        val s2 = PyramidRules.selectCard(s1, CardLocation.Pyramid(22))
        assertNull(s2.pyramid[21])
        assertNull(s2.pyramid[22])
    }

    @Test
    fun selectCard_validPairBothPyramid_scores10() {
        val pyramid = filledPyramid(card(Rank.TWO)).toMutableList()
        pyramid[21] = card(Rank.ACE)
        pyramid[22] = card(Rank.QUEEN)
        val s0 = stateWith(pyramid)
        val s1 = PyramidRules.selectCard(s0, CardLocation.Pyramid(21))
        val s2 = PyramidRules.selectCard(s1, CardLocation.Pyramid(22))
        assertEquals(10, s2.score)
    }

    @Test
    fun selectCard_validPairWasteAndPyramid_removesBoth() {
        val pyramid = filledPyramid(card(Rank.TWO)).toMutableList()
        pyramid[21] = card(Rank.ACE)   // value 1, exposed
        val state = stateWith(
            pyramid = pyramid,
            waste   = listOf(card(Rank.QUEEN)) // value 12; 1+12=13
        )
        val s1 = PyramidRules.selectCard(state, CardLocation.Pyramid(21))
        val s2 = PyramidRules.selectCard(s1, CardLocation.Waste)
        assertNull(s2.pyramid[21])
        assertTrue(s2.waste.isEmpty())
    }

    @Test
    fun selectCard_invalidPair_replacesSelection() {
        // TWO + THREE = 5, not 13
        val pyramid = filledPyramid(card(Rank.ACE)).toMutableList()
        pyramid[21] = card(Rank.TWO)
        pyramid[22] = card(Rank.THREE)
        val s0 = stateWith(pyramid)
        val s1 = PyramidRules.selectCard(s0, CardLocation.Pyramid(21))
        val s2 = PyramidRules.selectCard(s1, CardLocation.Pyramid(22))
        // Both cards still in pyramid, selection moved to 22
        assertNotNull(s2.pyramid[21])
        assertEquals(CardLocation.Pyramid(22), s2.selected)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // drawFromStock
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun drawFromStock_withStock_movesFirstCardToWaste() {
        val drawn = card(Rank.FIVE)
        val state = stateWith(
            pyramid = emptyPyramid(),
            stock   = listOf(drawn, card(Rank.SIX))
        )
        val config = PyramidConfig(maxRedeals = 0)
        val result = PyramidRules.drawFromStock(state, config)!!
        assertEquals(drawn, result.waste.last())
        assertEquals(1, result.stock.size)
    }

    @Test
    fun drawFromStock_emptyStockWithRedealsLeft_recyclesWaste() {
        val state = stateWith(
            pyramid     = emptyPyramid(),
            stock       = emptyList(),
            waste       = listOf(card(Rank.ACE), card(Rank.TWO)),
            stockCycles = 0
        )
        val config = PyramidConfig(maxRedeals = 1)
        val result = PyramidRules.drawFromStock(state, config)!!
        assertTrue(result.waste.isEmpty())
        assertEquals(2, result.stock.size)
        assertEquals(1, result.stockCycles)
    }

    @Test
    fun drawFromStock_emptyStockNoRedealsAllowed_returnsNull() {
        val state = stateWith(
            pyramid = emptyPyramid(),
            stock   = emptyList(),
            waste   = listOf(card(Rank.ACE))
        )
        val config = PyramidConfig(maxRedeals = 0)
        assertNull(PyramidRules.drawFromStock(state, config))
    }

    @Test
    fun drawFromStock_emptyStockRedealsExhausted_returnsNull() {
        val state = stateWith(
            pyramid     = emptyPyramid(),
            stock       = emptyList(),
            waste       = listOf(card(Rank.ACE)),
            stockCycles = 2
        )
        val config = PyramidConfig(maxRedeals = 2)
        assertNull(PyramidRules.drawFromStock(state, config))
    }

    @Test
    fun drawFromStock_emptyStockEmptyWaste_returnsNull() {
        val state = stateWith(
            pyramid = emptyPyramid(),
            stock   = emptyList(),
            waste   = emptyList()
        )
        val config = PyramidConfig(maxRedeals = -1)
        assertNull(PyramidRules.drawFromStock(state, config))
    }

    @Test
    fun drawFromStock_unlimitedRedeals_canRecycleRepeatedly() {
        var state = stateWith(
            pyramid = emptyPyramid(),
            stock   = emptyList(),
            waste   = listOf(card(Rank.ACE)),
            stockCycles = 999
        )
        val config = PyramidConfig(maxRedeals = -1)
        val result = PyramidRules.drawFromStock(state, config)!!
        assertEquals(1000, result.stockCycles)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Win / lose
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun isWon_allNullPyramid_returnsTrue() {
        val state = stateWith(emptyPyramid())
        assertTrue(PyramidRules.isWon(state))
    }

    @Test
    fun isWon_anyCardPresent_returnsFalse() {
        val pyramid = emptyPyramid().toMutableList()
        pyramid[27] = card(Rank.ACE)
        val state = stateWith(pyramid)
        assertFalse(PyramidRules.isWon(state))
    }

    @Test
    fun hasLegalMoves_exposedKingInBaseRow_returnsTrue() {
        val pyramid = emptyPyramid().toMutableList()
        pyramid[21] = card(Rank.KING)  // base row, exposed
        val state = stateWith(pyramid)
        val config = PyramidConfig(maxRedeals = 0)
        assertTrue(PyramidRules.hasLegalMoves(state, config))
    }

    @Test
    fun hasLegalMoves_validPairAmongExposed_returnsTrue() {
        val pyramid = emptyPyramid().toMutableList()
        pyramid[21] = card(Rank.ACE)   // base row
        pyramid[22] = card(Rank.QUEEN) // base row; 1+12=13
        val state = stateWith(pyramid)
        val config = PyramidConfig(maxRedeals = 0)
        assertTrue(PyramidRules.hasLegalMoves(state, config))
    }

    @Test
    fun hasLegalMoves_drawableStock_returnsTrue() {
        val state = stateWith(
            pyramid = filledPyramid(card(Rank.TWO)),
            stock   = listOf(card(Rank.ACE))
        )
        val config = PyramidConfig(maxRedeals = 0)
        assertTrue(PyramidRules.hasLegalMoves(state, config))
    }

    @Test
    fun hasLegalMoves_noCardsNoPairs_returnsFalse() {
        // All remaining cards sum to some non-13 value; no stock; no redeals
        // Easiest: completely empty pyramid, empty stock, empty waste
        val state = stateWith(emptyPyramid())
        val config = PyramidConfig(maxRedeals = 0)
        // isWon would be true but hasLegalMoves is still meaningful here
        assertFalse(PyramidRules.hasLegalMoves(state, config))
    }

    @Test
    fun hasLegalMoves_recycleAvailable_returnsTrue() {
        val state = stateWith(
            pyramid     = filledPyramid(card(Rank.TWO)),
            stock       = emptyList(),
            waste       = listOf(card(Rank.ACE)),
            stockCycles = 0
        )
        val config = PyramidConfig(maxRedeals = 1)
        assertTrue(PyramidRules.hasLegalMoves(state, config))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Undo
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun undo_emptyHistory_returnsNull() {
        val state = stateWith(filledPyramid())
        assertNull(PyramidRules.undo(state))
    }

    @Test
    fun undo_afterDraw_revertsStockAndWaste() {
        val drawn = card(Rank.FIVE)
        val state = stateWith(
            pyramid = emptyPyramid(),
            stock   = listOf(drawn)
        )
        val config = PyramidConfig(maxRedeals = 0)
        val afterDraw = PyramidRules.drawFromStock(state, config)!!
        val reverted  = PyramidRules.undo(afterDraw)!!
        assertEquals(listOf(drawn), reverted.stock)
        assertTrue(reverted.waste.isEmpty())
    }

    @Test
    fun undo_afterKingRemoval_restoresCard() {
        val pyramid = filledPyramid(card(Rank.TWO)).toMutableList()
        pyramid[21] = card(Rank.KING)
        val state  = stateWith(pyramid)
        val after  = PyramidRules.selectCard(state, CardLocation.Pyramid(21))
        val undone = PyramidRules.undo(after)!!
        assertNotNull(undone.pyramid[21])
        assertEquals(0, undone.score)
    }

    @Test
    fun undo_afterPairRemoval_restoresBothCards() {
        val pyramid = filledPyramid(card(Rank.TWO)).toMutableList()
        pyramid[21] = card(Rank.ACE)
        pyramid[22] = card(Rank.QUEEN)
        val s0 = stateWith(pyramid)
        val s1 = PyramidRules.selectCard(s0, CardLocation.Pyramid(21))
        val s2 = PyramidRules.selectCard(s1, CardLocation.Pyramid(22))
        val s3 = PyramidRules.undo(s2)!!
        assertNotNull(s3.pyramid[21])
        assertNotNull(s3.pyramid[22])
    }

    // ─────────────────────────────────────────────────────────────────────────
    // History depth
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun history_afterMultipleMoves_growsByOnePerMove() {
        val config = PyramidConfig(maxRedeals = 1)
        var state  = stateWith(emptyPyramid(), stock = listOf(card(Rank.ACE), card(Rank.TWO)))
        state = PyramidRules.drawFromStock(state, config)!!
        state = PyramidRules.drawFromStock(state, config)!!
        assertEquals(2, state.history.size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hint
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun findHint_exposedKing_returnsKingLocation() {
        val pyramid = emptyPyramid().toMutableList()
        pyramid[21] = card(Rank.KING)
        val state = stateWith(pyramid)
        val hint  = PyramidRules.findHint(state)!!
        assertEquals(CardLocation.Pyramid(21), hint.first)
        assertNull(hint.second)
    }

    @Test
    fun findHint_noPossibleMoves_returnsNull() {
        val state = stateWith(emptyPyramid())
        assertNull(PyramidRules.findHint(state))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Config
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun config_easy_hasUnlimitedRedeals() {
        assertEquals(-1, PyramidConfig.forDifficulty(GameDifficulty.EASY).maxRedeals)
    }

    @Test
    fun config_medium_has2Redeals() {
        assertEquals(2, PyramidConfig.forDifficulty(GameDifficulty.MEDIUM).maxRedeals)
    }

    @Test
    fun config_hard_hasNoRedeals() {
        assertEquals(0, PyramidConfig.forDifficulty(GameDifficulty.HARD).maxRedeals)
    }
}
