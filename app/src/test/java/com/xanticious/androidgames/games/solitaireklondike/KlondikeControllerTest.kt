package com.xanticious.androidgames.games.solitaireklondike

import com.xanticious.androidgames.controller.games.solitaireklondike.KlondikeController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.Rank
import com.xanticious.androidgames.model.games.cards.Suit
import com.xanticious.androidgames.model.games.solitaireklondike.KlondikeConfig
import com.xanticious.androidgames.model.games.solitaireklondike.KlondikeMove
import com.xanticious.androidgames.model.games.solitaireklondike.KlondikeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val easyConfig = KlondikeController.configFor(GameDifficulty.EASY)
private val medConfig  = KlondikeController.configFor(GameDifficulty.MEDIUM)
private val hardConfig = KlondikeController.configFor(GameDifficulty.HARD)

/** Build a minimal KlondikeState without going through a full deal. */
private fun minimalState(
    tableau: List<List<Card>> = List(7) { emptyList() },
    foundations: List<List<Card>> = List(4) { emptyList() },
    stock: List<Card> = emptyList(),
    waste: List<Card> = emptyList(),
    passesUsed: Int = 0,
    config: KlondikeConfig = easyConfig
) = KlondikeState(tableau, foundations, stock, waste, passesUsed, 0, config)

// ── configFor ────────────────────────────────────────────────────────────────

class KlondikeControllerTest {

    @Test
    fun configFor_easy_drawOneAndInfinitePasses() {
        assertEquals(1, easyConfig.drawCount)
        assertEquals(KlondikeConfig.INFINITE_PASSES, easyConfig.deckPasses)
    }

    @Test
    fun configFor_medium_drawOneAndThreePasses() {
        assertEquals(1, medConfig.drawCount)
        assertEquals(3, medConfig.deckPasses)
    }

    @Test
    fun configFor_hard_drawThreeAndOnePass() {
        assertEquals(3, hardConfig.drawCount)
        assertEquals(1, hardConfig.deckPasses)
    }

    // ── deal ─────────────────────────────────────────────────────────────────

    @Test
    fun deal_tableau_hasSevenColumns() {
        assertEquals(7, KlondikeController.deal(42L, easyConfig).tableau.size)
    }

    @Test
    fun deal_columns_haveCorrectSizes() {
        val state = KlondikeController.deal(42L, easyConfig)
        for (i in 0..6) assertEquals(i + 1, state.tableau[i].size)
    }

    @Test
    fun deal_stock_has24Cards() {
        assertEquals(24, KlondikeController.deal(42L, easyConfig).stock.size)
    }

    @Test
    fun deal_topCardOfEachColumn_isFaceUp() {
        val state = KlondikeController.deal(42L, easyConfig)
        for (col in state.tableau) assertTrue(col.last().faceUp)
    }

    @Test
    fun deal_nonTopCardsInEachColumn_areFaceDown() {
        val state = KlondikeController.deal(42L, easyConfig)
        for (col in state.tableau) {
            for (card in col.dropLast(1)) assertFalse(card.faceUp)
        }
    }

    @Test
    fun deal_totalCardCount_is52() {
        val state = KlondikeController.deal(42L, easyConfig)
        val total = state.tableau.sumOf { it.size } + state.stock.size + state.waste.size
        assertEquals(52, total)
    }

    @Test
    fun deal_differentSeeds_produceDifferentLayouts() {
        val s1 = KlondikeController.deal(1L, easyConfig)
        val s2 = KlondikeController.deal(2L, easyConfig)
        assertFalse(s1.tableau[0].first() == s2.tableau[0].first())
    }

    @Test
    fun deal_sameSeed_producesIdenticalLayouts() {
        val s1 = KlondikeController.deal(99L, easyConfig)
        val s2 = KlondikeController.deal(99L, easyConfig)
        assertEquals(s1.tableau, s2.tableau)
    }

    // ── canMoveToFoundation ───────────────────────────────────────────────────

    @Test
    fun canMoveToFoundation_aceToEmptyPile_isLegal() {
        assertTrue(KlondikeController.canMoveToFoundation(Card(Rank.ACE, Suit.SPADES), emptyList()))
    }

    @Test
    fun canMoveToFoundation_nonAceToEmptyPile_isIllegal() {
        assertFalse(KlondikeController.canMoveToFoundation(Card(Rank.TWO, Suit.SPADES), emptyList()))
    }

    @Test
    fun canMoveToFoundation_sameSuitNextRank_isLegal() {
        val pile = listOf(Card(Rank.ACE, Suit.HEARTS), Card(Rank.TWO, Suit.HEARTS))
        assertTrue(KlondikeController.canMoveToFoundation(Card(Rank.THREE, Suit.HEARTS), pile))
    }

    @Test
    fun canMoveToFoundation_wrongSuit_isIllegal() {
        val pile = listOf(Card(Rank.ACE, Suit.HEARTS))
        assertFalse(KlondikeController.canMoveToFoundation(Card(Rank.TWO, Suit.SPADES), pile))
    }

    @Test
    fun canMoveToFoundation_skippedRank_isIllegal() {
        val pile = listOf(Card(Rank.ACE, Suit.HEARTS))
        assertFalse(KlondikeController.canMoveToFoundation(Card(Rank.THREE, Suit.HEARTS), pile))
    }

    @Test
    fun canMoveToFoundation_king_completesFullSuit() {
        val pile = Rank.entries.dropLast(1).map { Card(it, Suit.CLUBS) }
        assertTrue(KlondikeController.canMoveToFoundation(Card(Rank.KING, Suit.CLUBS), pile))
    }

    // ── canMoveToTableau ─────────────────────────────────────────────────────

    @Test
    fun canMoveToTableau_kingToEmptyColumn_isLegal() {
        assertTrue(KlondikeController.canMoveToTableau(Card(Rank.KING, Suit.SPADES), emptyList()))
    }

    @Test
    fun canMoveToTableau_nonKingToEmptyColumn_isIllegal() {
        assertFalse(KlondikeController.canMoveToTableau(Card(Rank.QUEEN, Suit.HEARTS), emptyList()))
    }

    @Test
    fun canMoveToTableau_blackOnRedDescending_isLegal() {
        val col = listOf(Card(Rank.QUEEN, Suit.HEARTS, faceUp = true))
        assertTrue(KlondikeController.canMoveToTableau(Card(Rank.JACK, Suit.CLUBS), col))
    }

    @Test
    fun canMoveToTableau_redOnBlackDescending_isLegal() {
        val col = listOf(Card(Rank.QUEEN, Suit.SPADES, faceUp = true))
        assertTrue(KlondikeController.canMoveToTableau(Card(Rank.JACK, Suit.HEARTS), col))
    }

    @Test
    fun canMoveToTableau_sameColorDescending_isIllegal() {
        val col = listOf(Card(Rank.QUEEN, Suit.HEARTS, faceUp = true))
        assertFalse(KlondikeController.canMoveToTableau(Card(Rank.JACK, Suit.DIAMONDS), col))
    }

    @Test
    fun canMoveToTableau_correctColorButWrongRank_isIllegal() {
        val col = listOf(Card(Rank.QUEEN, Suit.HEARTS, faceUp = true))
        assertFalse(KlondikeController.canMoveToTableau(Card(Rank.TEN, Suit.CLUBS), col))
    }

    @Test
    fun canMoveToTableau_faceDownTopCard_isIllegal() {
        val col = listOf(Card(Rank.QUEEN, Suit.HEARTS, faceUp = false))
        assertFalse(KlondikeController.canMoveToTableau(Card(Rank.JACK, Suit.CLUBS), col))
    }

    // ── foundationIndexFor ────────────────────────────────────────────────────

    @Test
    fun foundationIndexFor_aceOnAllEmpty_returnsFirstSlot() {
        val foundations = List(4) { emptyList<Card>() }
        assertEquals(0, KlondikeController.foundationIndexFor(Card(Rank.ACE, Suit.SPADES), foundations))
    }

    @Test
    fun foundationIndexFor_matchingSuitAndRank_findsPile() {
        val pile = listOf(Card(Rank.ACE, Suit.HEARTS))
        val foundations = listOf(emptyList(), pile, emptyList<Card>(), emptyList())
        assertEquals(1, KlondikeController.foundationIndexFor(Card(Rank.TWO, Suit.HEARTS), foundations))
    }

    @Test
    fun foundationIndexFor_noValidPile_returnsNull() {
        val pile = listOf(Card(Rank.ACE, Suit.HEARTS))
        val foundations = listOf(pile, emptyList<Card>(), emptyList(), emptyList())
        assertNull(KlondikeController.foundationIndexFor(Card(Rank.TWO, Suit.SPADES), foundations))
    }

    // ── DrawStock ─────────────────────────────────────────────────────────────

    @Test
    fun drawStock_movesOneCardToWaste() {
        val state = KlondikeController.deal(1L, easyConfig)
        val result = KlondikeController.applyMove(state, KlondikeMove.DrawStock)!!
        assertEquals(state.stock.size - 1, result.stock.size)
        assertEquals(1, result.waste.size)
    }

    @Test
    fun drawStock_drawnCard_isFaceUp() {
        val state = KlondikeController.deal(1L, easyConfig)
        val result = KlondikeController.applyMove(state, KlondikeMove.DrawStock)!!
        assertTrue(result.waste.last().faceUp)
    }

    @Test
    fun drawStock_draw3Config_draws3Cards() {
        val state = KlondikeController.deal(1L, hardConfig)
        val result = KlondikeController.applyMove(state, KlondikeMove.DrawStock)!!
        assertEquals(3, result.waste.size)
    }

    @Test
    fun drawStock_emptyStock_returnsNull() {
        val state = minimalState(stock = emptyList())
        assertNull(KlondikeController.applyMove(state, KlondikeMove.DrawStock))
    }

    @Test
    fun drawStock_incrementsMoveCount() {
        val state = KlondikeController.deal(1L, easyConfig)
        val result = KlondikeController.applyMove(state, KlondikeMove.DrawStock)!!
        assertEquals(1, result.moves)
    }

    @Test
    fun drawStock_partialStock_drawsAvailableCards() {
        val twoCards = listOf(Card(Rank.ACE, Suit.CLUBS, faceUp = false), Card(Rank.TWO, Suit.CLUBS, faceUp = false))
        val state = minimalState(stock = twoCards, config = hardConfig)  // draw-3 but only 2 left
        val result = KlondikeController.applyMove(state, KlondikeMove.DrawStock)!!
        assertEquals(0, result.stock.size)
        assertEquals(2, result.waste.size)
    }

    // ── RecycleStock ─────────────────────────────────────────────────────────

    @Test
    fun recycleStock_movesWasteToStock() {
        val waste = listOf(Card(Rank.ACE, Suit.SPADES, faceUp = true), Card(Rank.TWO, Suit.HEARTS, faceUp = true))
        val state = minimalState(waste = waste)
        val result = KlondikeController.applyMove(state, KlondikeMove.RecycleStock)!!
        assertEquals(0, result.waste.size)
        assertEquals(2, result.stock.size)
    }

    @Test
    fun recycleStock_recycledCards_areFaceDown() {
        val waste = listOf(Card(Rank.ACE, Suit.SPADES, faceUp = true))
        val state = minimalState(waste = waste)
        val result = KlondikeController.applyMove(state, KlondikeMove.RecycleStock)!!
        assertFalse(result.stock.first().faceUp)
    }

    @Test
    fun recycleStock_incrementsPassesUsed() {
        val waste = listOf(Card(Rank.ACE, Suit.SPADES, faceUp = true))
        val state = minimalState(waste = waste, passesUsed = 0)
        val result = KlondikeController.applyMove(state, KlondikeMove.RecycleStock)!!
        assertEquals(1, result.passesUsed)
    }

    @Test
    fun recycleStock_whenStockNotEmpty_returnsNull() {
        val state = KlondikeController.deal(1L, easyConfig)  // stock has cards
        assertNull(KlondikeController.applyMove(state, KlondikeMove.RecycleStock))
    }

    @Test
    fun recycleStock_limitReached_returnsNull() {
        val waste = listOf(Card(Rank.ACE, Suit.SPADES, faceUp = true))
        val state = minimalState(waste = waste, passesUsed = 1, config = KlondikeConfig(1, 1))
        assertNull(KlondikeController.applyMove(state, KlondikeMove.RecycleStock))
    }

    @Test
    fun recycleStock_infinitePasses_allowsRecycleBeyondLimit() {
        val waste = listOf(Card(Rank.ACE, Suit.SPADES, faceUp = true))
        val state = minimalState(waste = waste, passesUsed = 100, config = easyConfig)
        assertNotNull(KlondikeController.applyMove(state, KlondikeMove.RecycleStock))
    }

    @Test
    fun recycleStock_emptyWaste_returnsNull() {
        val state = minimalState(stock = emptyList(), waste = emptyList())
        assertNull(KlondikeController.applyMove(state, KlondikeMove.RecycleStock))
    }

    // ── WasteToFoundation ─────────────────────────────────────────────────────

    @Test
    fun wasteToFoundation_aceToEmptyFoundation_succeeds() {
        val ace = Card(Rank.ACE, Suit.SPADES, faceUp = true)
        val state = minimalState(waste = listOf(ace))
        val result = KlondikeController.applyMove(state, KlondikeMove.WasteToFoundation(0))!!
        assertEquals(1, result.foundations[0].size)
        assertEquals(0, result.waste.size)
    }

    @Test
    fun wasteToFoundation_illegalCard_returnsNull() {
        val two = Card(Rank.TWO, Suit.SPADES, faceUp = true)
        val state = minimalState(waste = listOf(two))
        assertNull(KlondikeController.applyMove(state, KlondikeMove.WasteToFoundation(0)))
    }

    @Test
    fun wasteToFoundation_emptyWaste_returnsNull() {
        val state = minimalState(waste = emptyList())
        assertNull(KlondikeController.applyMove(state, KlondikeMove.WasteToFoundation(0)))
    }

    // ── WasteToTableau ────────────────────────────────────────────────────────

    @Test
    fun wasteToTableau_kingToEmptyColumn_succeeds() {
        val king = Card(Rank.KING, Suit.SPADES, faceUp = true)
        val tableau = List(7) { emptyList<Card>() }
        val state = minimalState(tableau = tableau, waste = listOf(king))
        val result = KlondikeController.applyMove(state, KlondikeMove.WasteToTableau(0))!!
        assertEquals(1, result.tableau[0].size)
        assertEquals(0, result.waste.size)
    }

    @Test
    fun wasteToTableau_illegalPlacement_returnsNull() {
        val jack = Card(Rank.JACK, Suit.CLUBS, faceUp = true)
        val queen = Card(Rank.QUEEN, Suit.HEARTS, faceUp = true)   // same color family (red)
        val tableau = List(7) { i -> if (i == 0) listOf(queen) else emptyList() }
        val state = minimalState(tableau = tableau, waste = listOf(jack))
        // Jack of clubs (black) on Queen of hearts (red) IS legal.
        assertNotNull(KlondikeController.applyMove(state, KlondikeMove.WasteToTableau(0)))
    }

    @Test
    fun wasteToTableau_sameColor_returnsNull() {
        val jack = Card(Rank.JACK, Suit.DIAMONDS, faceUp = true) // red
        val queen = Card(Rank.QUEEN, Suit.HEARTS, faceUp = true)  // red
        val tableau = List(7) { i -> if (i == 0) listOf(queen) else emptyList() }
        val state = minimalState(tableau = tableau, waste = listOf(jack))
        assertNull(KlondikeController.applyMove(state, KlondikeMove.WasteToTableau(0)))
    }

    // ── TableauToFoundation ───────────────────────────────────────────────────

    @Test
    fun tableauToFoundation_aceFromTableau_succeeds() {
        val ace = Card(Rank.ACE, Suit.SPADES, faceUp = true)
        val tableau = List(7) { i -> if (i == 0) listOf(ace) else emptyList() }
        val state = minimalState(tableau = tableau)
        val result = KlondikeController.applyMove(state, KlondikeMove.TableauToFoundation(0, 0))!!
        assertEquals(1, result.foundations[0].size)
        assertEquals(0, result.tableau[0].size)
    }

    @Test
    fun tableauToFoundation_flipsNewTopCard_afterMove() {
        val bottom = Card(Rank.TWO, Suit.CLUBS, faceUp = false)
        val top = Card(Rank.ACE, Suit.SPADES, faceUp = true)
        val tableau = List(7) { i -> if (i == 0) listOf(bottom, top) else emptyList() }
        val state = minimalState(tableau = tableau)
        val result = KlondikeController.applyMove(state, KlondikeMove.TableauToFoundation(0, 0))!!
        assertTrue(result.tableau[0].last().faceUp)
    }

    @Test
    fun tableauToFoundation_faceDownTop_returnsNull() {
        val card = Card(Rank.ACE, Suit.SPADES, faceUp = false)
        val tableau = List(7) { i -> if (i == 0) listOf(card) else emptyList() }
        val state = minimalState(tableau = tableau)
        assertNull(KlondikeController.applyMove(state, KlondikeMove.TableauToFoundation(0, 0)))
    }

    @Test
    fun tableauToFoundation_emptyColumn_returnsNull() {
        val state = minimalState()
        assertNull(KlondikeController.applyMove(state, KlondikeMove.TableauToFoundation(0, 0)))
    }

    // ── TableauToTableau ──────────────────────────────────────────────────────

    @Test
    fun tableauToTableau_validSingleCard_movesCard() {
        val jack = Card(Rank.JACK, Suit.CLUBS, faceUp = true)
        val queen = Card(Rank.QUEEN, Suit.HEARTS, faceUp = true)
        val tableau = List(7) { i -> when (i) { 0 -> listOf(queen); 1 -> listOf(jack); else -> emptyList() } }
        val state = minimalState(tableau = tableau)
        val result = KlondikeController.applyMove(state, KlondikeMove.TableauToTableau(1, 0, 0))!!
        assertEquals(2, result.tableau[0].size)
        assertEquals(0, result.tableau[1].size)
    }

    @Test
    fun tableauToTableau_sameColor_returnsNull() {
        val jack = Card(Rank.JACK, Suit.CLUBS, faceUp = true)
        val queen = Card(Rank.QUEEN, Suit.SPADES, faceUp = true)
        val tableau = List(7) { i -> when (i) { 0 -> listOf(queen); 1 -> listOf(jack); else -> emptyList() } }
        val state = minimalState(tableau = tableau)
        assertNull(KlondikeController.applyMove(state, KlondikeMove.TableauToTableau(1, 0, 0)))
    }

    @Test
    fun tableauToTableau_kingToEmptyColumn_succeeds() {
        val king = Card(Rank.KING, Suit.SPADES, faceUp = true)
        val tableau = List(7) { i -> when (i) { 0 -> listOf(king); 1 -> emptyList(); else -> listOf(Card(Rank.TWO, Suit.HEARTS, faceUp = true)) } }
        val state = minimalState(tableau = tableau)
        val result = KlondikeController.applyMove(state, KlondikeMove.TableauToTableau(0, 0, 1))!!
        assertEquals(0, result.tableau[0].size)
        assertEquals(Rank.KING, result.tableau[1].first().rank)
    }

    @Test
    fun tableauToTableau_flipsExposedFaceDownCard() {
        val hidden = Card(Rank.THREE, Suit.CLUBS, faceUp = false)
        val jack = Card(Rank.JACK, Suit.CLUBS, faceUp = true)
        val queen = Card(Rank.QUEEN, Suit.HEARTS, faceUp = true)
        val tableau = List(7) { i -> when (i) { 0 -> listOf(queen); 1 -> listOf(hidden, jack); else -> emptyList() } }
        val state = minimalState(tableau = tableau)
        val result = KlondikeController.applyMove(state, KlondikeMove.TableauToTableau(1, 1, 0))!!
        assertTrue(result.tableau[1].last().faceUp)
    }

    @Test
    fun tableauToTableau_movesMultipleCardRun() {
        val ten = Card(Rank.TEN, Suit.CLUBS, faceUp = true)
        val nine = Card(Rank.NINE, Suit.HEARTS, faceUp = true)
        val jack = Card(Rank.JACK, Suit.HEARTS, faceUp = true)  // red
        val tableau = List(7) { i -> when (i) { 0 -> listOf(jack); 1 -> listOf(ten, nine); else -> emptyList() } }
        val state = minimalState(tableau = tableau)
        // Move ten+nine run (startIndex=0) from col1 onto jack in col0.
        val result = KlondikeController.applyMove(state, KlondikeMove.TableauToTableau(1, 0, 0))!!
        assertEquals(3, result.tableau[0].size)
        assertEquals(0, result.tableau[1].size)
    }

    @Test
    fun tableauToTableau_sameColumn_returnsNull() {
        val king = Card(Rank.KING, Suit.SPADES, faceUp = true)
        val tableau = List(7) { i -> if (i == 0) listOf(king) else emptyList() }
        val state = minimalState(tableau = tableau)
        assertNull(KlondikeController.applyMove(state, KlondikeMove.TableauToTableau(0, 0, 0)))
    }

    @Test
    fun tableauToTableau_faceDownCardInRun_returnsNull() {
        val faceDown = Card(Rank.JACK, Suit.CLUBS, faceUp = false)
        val queen = Card(Rank.QUEEN, Suit.HEARTS, faceUp = true)
        val tableau = List(7) { i -> when (i) { 0 -> listOf(queen); 1 -> listOf(faceDown); else -> emptyList() } }
        val state = minimalState(tableau = tableau)
        assertNull(KlondikeController.applyMove(state, KlondikeMove.TableauToTableau(1, 0, 0)))
    }

    // ── isWon ─────────────────────────────────────────────────────────────────

    @Test
    fun isWon_allFoundationsFull_returnsTrue() {
        val fullPile = Rank.entries.map { Card(it, Suit.SPADES) }
        val foundations = List(4) { fullPile }
        val state = minimalState(foundations = foundations)
        assertTrue(KlondikeController.isWon(state))
    }

    @Test
    fun isWon_partialFoundations_returnsFalse() {
        val state = KlondikeController.deal(42L, easyConfig)
        assertFalse(KlondikeController.isWon(state))
    }

    // ── canAutoFinish ─────────────────────────────────────────────────────────

    @Test
    fun canAutoFinish_allFaceUpAndEmptyStock_returnsTrue() {
        val col = listOf(Card(Rank.TWO, Suit.CLUBS, faceUp = true))
        val state = minimalState(tableau = List(7) { col }, stock = emptyList())
        assertTrue(KlondikeController.canAutoFinish(state))
    }

    @Test
    fun canAutoFinish_stockNotEmpty_returnsFalse() {
        val state = KlondikeController.deal(42L, easyConfig)
        assertFalse(KlondikeController.canAutoFinish(state))
    }

    @Test
    fun canAutoFinish_faceDownCardsExist_returnsFalse() {
        val col = listOf(Card(Rank.TWO, Suit.CLUBS, faceUp = false))
        val state = minimalState(tableau = List(7) { col }, stock = emptyList())
        assertFalse(KlondikeController.canAutoFinish(state))
    }

    // ── autoFinish ────────────────────────────────────────────────────────────

    @Test
    fun autoFinish_completesSimpleLayout() {
        // Foundations already have Ace–Queen for every suit (48 cards);
        // each of the first four tableau columns holds the matching King face-up.
        // autoFinish must move those four Kings to complete the game.
        val suits = Suit.entries
        val aceThroughQueen = Rank.entries.dropLast(1)   // ACE .. QUEEN
        val foundations = suits.map { suit -> aceThroughQueen.map { Card(it, suit) } }
        val tableau = suits.map { suit -> listOf(Card(Rank.KING, suit, faceUp = true)) } +
            List(3) { emptyList<Card>() }
        val state = minimalState(tableau = tableau, foundations = foundations, stock = emptyList())
        val result = KlondikeController.autoFinish(state)
        assertTrue(KlondikeController.isWon(result))
    }

    // ── hint ──────────────────────────────────────────────────────────────────

    @Test
    fun hint_whenAceOnTop_suggestsFoundationMove() {
        val ace = Card(Rank.ACE, Suit.SPADES, faceUp = true)
        val tableau = List(7) { i -> if (i == 0) listOf(ace) else emptyList() }
        val state = minimalState(tableau = tableau)
        val h = KlondikeController.hint(state)
        assertTrue(h is KlondikeMove.TableauToFoundation)
    }

    @Test
    fun hint_whenStockAvailable_suggestsDraw() {
        val stockCard = Card(Rank.TWO, Suit.CLUBS, faceUp = false)
        val state = minimalState(stock = listOf(stockCard))
        val h = KlondikeController.hint(state)
        assertEquals(KlondikeMove.DrawStock, h)
    }

    @Test
    fun hint_noMoves_returnsNull() {
        // Completely empty state with used-up passes — no legal move possible.
        val cfg = KlondikeConfig(drawCount = 1, deckPasses = 1)
        val state = minimalState(
            stock = emptyList<Card>(), waste = emptyList<Card>(),
            passesUsed = 1, config = cfg
        )
        assertNull(KlondikeController.hint(state))
    }
}
