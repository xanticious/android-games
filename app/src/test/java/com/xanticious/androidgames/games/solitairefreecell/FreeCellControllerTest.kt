package com.xanticious.androidgames.games.solitairefreecell

import com.xanticious.androidgames.controller.games.solitairefreecell.FreeCellController
import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.CardColor
import com.xanticious.androidgames.model.games.cards.Rank
import com.xanticious.androidgames.model.games.cards.Suit
import com.xanticious.androidgames.model.games.solitairefreecell.FreeCellState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun emptyState(freeCellCount: Int = 4): FreeCellState = FreeCellState(
    tableau = List(8) { emptyList() },
    freeCells = List(freeCellCount) { null },
    foundations = List(4) { emptyList() },
    freeCellCount = freeCellCount
)

private fun stateWithTableau(
    vararg columns: List<Card>,
    freeCells: List<Card?> = List(4) { null },
    foundations: List<List<Card>> = List(4) { emptyList() },
    freeCellCount: Int = 4
): FreeCellState {
    val fullTableau = columns.toList() + List(8 - columns.size) { emptyList<Card>() }
    return FreeCellState(
        tableau = fullTableau.take(8),
        freeCells = freeCells,
        foundations = foundations,
        freeCellCount = freeCellCount
    )
}

class FreeCellControllerTest {

    // ── Deal ─────────────────────────────────────────────────────────────────

    @Test
    fun deal_seed_createsEightTableauColumns() {
        val state = FreeCellController.deal(42L)
        assertEquals(8, state.tableau.size)
    }

    @Test
    fun deal_seed_firstFourColumnsHaveSevenCards() {
        val state = FreeCellController.deal(42L)
        (0..3).forEach { col -> assertEquals(7, state.tableau[col].size) }
    }

    @Test
    fun deal_seed_lastFourColumnsHaveSixCards() {
        val state = FreeCellController.deal(42L)
        (4..7).forEach { col -> assertEquals(6, state.tableau[col].size) }
    }

    @Test
    fun deal_seed_allFiftyTwoCardsPresent() {
        val state = FreeCellController.deal(42L)
        val allCards = state.tableau.flatten()
        assertEquals(52, allCards.size)
        assertEquals(52, allCards.toSet().size)
    }

    @Test
    fun deal_seed_allCardsFaceUp() {
        val state = FreeCellController.deal(42L)
        assertTrue(state.tableau.flatten().all { it.faceUp })
    }

    @Test
    fun deal_seed_allFreeCellsEmpty() {
        val state = FreeCellController.deal(42L, freeCellCount = 4)
        assertEquals(4, state.freeCells.size)
        assertTrue(state.freeCells.all { it == null })
    }

    @Test
    fun deal_seed_allFoundationsEmpty() {
        val state = FreeCellController.deal(42L)
        assertTrue(state.foundations.all { it.isEmpty() })
    }

    @Test
    fun deal_deterministic_sameSeedGivesSameDeal() {
        val s1 = FreeCellController.deal(99L)
        val s2 = FreeCellController.deal(99L)
        assertEquals(s1.tableau, s2.tableau)
    }

    @Test
    fun deal_freeCellCount3_createsThreeFreeCells() {
        val state = FreeCellController.deal(1L, freeCellCount = 3)
        assertEquals(3, state.freeCells.size)
    }

    // ── isValidSequence ───────────────────────────────────────────────────────

    @Test
    fun isValidSequence_singleCard_returnsTrue() {
        val col = listOf(Card(Rank.FIVE, Suit.HEARTS))
        assertTrue(FreeCellController.isValidSequence(col, 0))
    }

    @Test
    fun isValidSequence_descendingAlternatingColors_returnsTrue() {
        val col = listOf(
            Card(Rank.FIVE, Suit.SPADES),  // black
            Card(Rank.FOUR, Suit.HEARTS),  // red
            Card(Rank.THREE, Suit.CLUBS)   // black
        )
        assertTrue(FreeCellController.isValidSequence(col, 0))
    }

    @Test
    fun isValidSequence_sameColor_returnsFalse() {
        val col = listOf(
            Card(Rank.FIVE, Suit.SPADES),  // black
            Card(Rank.FOUR, Suit.CLUBS)    // black — invalid
        )
        assertFalse(FreeCellController.isValidSequence(col, 0))
    }

    @Test
    fun isValidSequence_notDescending_returnsFalse() {
        val col = listOf(
            Card(Rank.FIVE, Suit.HEARTS),  // red
            Card(Rank.FIVE, Suit.CLUBS)    // same rank — invalid
        )
        assertFalse(FreeCellController.isValidSequence(col, 0))
    }

    @Test
    fun isValidSequence_fromMidRow_checksOnlySubsequence() {
        // Full column is NOT a valid sequence, but from row 1 onward it is.
        val col = listOf(
            Card(Rank.KING, Suit.HEARTS),  // red — not connected to next
            Card(Rank.FOUR, Suit.SPADES),  // black
            Card(Rank.THREE, Suit.HEARTS)  // red
        )
        assertFalse(FreeCellController.isValidSequence(col, 0))
        assertTrue(FreeCellController.isValidSequence(col, 1))
    }

    @Test
    fun isValidSequence_outOfBoundsFromRow_returnsFalse() {
        val col = listOf(Card(Rank.ACE, Suit.SPADES))
        assertFalse(FreeCellController.isValidSequence(col, 5))
    }

    // ── movableSequenceLength ─────────────────────────────────────────────────

    @Test
    fun movableSequenceLength_emptyColumn_returnsZero() {
        assertEquals(0, FreeCellController.movableSequenceLength(emptyList()))
    }

    @Test
    fun movableSequenceLength_singleCard_returnsOne() {
        val col = listOf(Card(Rank.ACE, Suit.SPADES))
        assertEquals(1, FreeCellController.movableSequenceLength(col))
    }

    @Test
    fun movableSequenceLength_fullValidSequence_returnsLength() {
        val col = listOf(
            Card(Rank.FOUR, Suit.SPADES),  // black
            Card(Rank.THREE, Suit.HEARTS), // red
            Card(Rank.TWO, Suit.CLUBS)     // black
        )
        assertEquals(3, FreeCellController.movableSequenceLength(col))
    }

    @Test
    fun movableSequenceLength_brokenSequenceAtTop_returnsPartialLength() {
        val col = listOf(
            Card(Rank.KING, Suit.SPADES),  // black — breaks sequence
            Card(Rank.THREE, Suit.HEARTS), // red
            Card(Rank.TWO, Suit.CLUBS)     // black
        )
        assertEquals(2, FreeCellController.movableSequenceLength(col))
    }

    // ── supermoveCapacity ─────────────────────────────────────────────────────

    @Test
    fun supermoveCapacity_zeroFreeZeroEmpty_returnsOne() {
        assertEquals(1, FreeCellController.supermoveCapacity(0, 0))
    }

    @Test
    fun supermoveCapacity_oneFreeZeroEmpty_returnsTwo() {
        assertEquals(2, FreeCellController.supermoveCapacity(1, 0))
    }

    @Test
    fun supermoveCapacity_zeroFreeOneEmpty_returnsTwo() {
        assertEquals(2, FreeCellController.supermoveCapacity(0, 1))
    }

    @Test
    fun supermoveCapacity_twoFreeOneEmpty_returnsSix() {
        assertEquals(6, FreeCellController.supermoveCapacity(2, 1))
    }

    @Test
    fun supermoveCapacity_threeFreeThreeEmpty_returnsSixteen() {
        // (3+1) * 2^2 = 16
        assertEquals(16, FreeCellController.supermoveCapacity(3, 2))
    }

    // ── canMoveToFoundation ───────────────────────────────────────────────────

    @Test
    fun canMoveToFoundation_aceOnEmptyPile_returnsTrue() {
        val ace = Card(Rank.ACE, Suit.SPADES)
        assertTrue(FreeCellController.canMoveToFoundation(ace, emptyList()))
    }

    @Test
    fun canMoveToFoundation_nonAceOnEmptyPile_returnsFalse() {
        val two = Card(Rank.TWO, Suit.SPADES)
        assertFalse(FreeCellController.canMoveToFoundation(two, emptyList()))
    }

    @Test
    fun canMoveToFoundation_correctSuitNextRank_returnsTrue() {
        val pile = listOf(Card(Rank.ACE, Suit.HEARTS))
        val two = Card(Rank.TWO, Suit.HEARTS)
        assertTrue(FreeCellController.canMoveToFoundation(two, pile))
    }

    @Test
    fun canMoveToFoundation_wrongSuit_returnsFalse() {
        val pile = listOf(Card(Rank.ACE, Suit.HEARTS))
        val twoSpades = Card(Rank.TWO, Suit.SPADES)
        assertFalse(FreeCellController.canMoveToFoundation(twoSpades, pile))
    }

    @Test
    fun canMoveToFoundation_rightSuitWrongRank_returnsFalse() {
        val pile = listOf(Card(Rank.ACE, Suit.HEARTS))
        val threeHearts = Card(Rank.THREE, Suit.HEARTS)
        assertFalse(FreeCellController.canMoveToFoundation(threeHearts, pile))
    }

    // ── canMoveToTableauColumn ────────────────────────────────────────────────

    @Test
    fun canMoveToTableauColumn_anyCardOnEmptyColumn_returnsTrue() {
        assertTrue(FreeCellController.canMoveToTableauColumn(Card(Rank.SEVEN, Suit.HEARTS), emptyList()))
    }

    @Test
    fun canMoveToTableauColumn_oneLowerOppositeColor_returnsTrue() {
        val target = listOf(Card(Rank.SIX, Suit.SPADES))  // black 6
        val card = Card(Rank.FIVE, Suit.HEARTS)            // red 5
        assertTrue(FreeCellController.canMoveToTableauColumn(card, target))
    }

    @Test
    fun canMoveToTableauColumn_sameColor_returnsFalse() {
        val target = listOf(Card(Rank.SIX, Suit.SPADES))  // black 6
        val card = Card(Rank.FIVE, Suit.CLUBS)             // black 5 — same color
        assertFalse(FreeCellController.canMoveToTableauColumn(card, target))
    }

    @Test
    fun canMoveToTableauColumn_twoLowerRank_returnsFalse() {
        val target = listOf(Card(Rank.SIX, Suit.SPADES))
        val card = Card(Rank.FOUR, Suit.HEARTS)            // 2 ranks lower
        assertFalse(FreeCellController.canMoveToTableauColumn(card, target))
    }

    @Test
    fun canMoveToTableauColumn_sameRank_returnsFalse() {
        val target = listOf(Card(Rank.SIX, Suit.SPADES))
        val card = Card(Rank.SIX, Suit.HEARTS)
        assertFalse(FreeCellController.canMoveToTableauColumn(card, target))
    }

    // ── moveToFreeCell ────────────────────────────────────────────────────────

    @Test
    fun moveToFreeCell_validMove_placesTopCardInCell() {
        val card = Card(Rank.ACE, Suit.SPADES)
        val state = stateWithTableau(listOf(card))
        val result = FreeCellController.moveToFreeCell(state, fromCol = 0, cellIndex = 0)
        assertNotNull(result)
        assertEquals(card, result!!.freeCells[0])
        assertTrue(result.tableau[0].isEmpty())
    }

    @Test
    fun moveToFreeCell_occupiedCell_returnsNull() {
        val card = Card(Rank.ACE, Suit.SPADES)
        val occupier = Card(Rank.KING, Suit.HEARTS)
        val state = stateWithTableau(
            listOf(card),
            freeCells = listOf(occupier, null, null, null)
        )
        assertNull(FreeCellController.moveToFreeCell(state, fromCol = 0, cellIndex = 0))
    }

    @Test
    fun moveToFreeCell_emptyColumn_returnsNull() {
        val state = stateWithTableau(emptyList())
        assertNull(FreeCellController.moveToFreeCell(state, fromCol = 0, cellIndex = 0))
    }

    @Test
    fun moveToFreeCell_incrementsMoveCount() {
        val state = stateWithTableau(listOf(Card(Rank.ACE, Suit.SPADES)))
        val result = FreeCellController.moveToFreeCell(state, fromCol = 0, cellIndex = 0)
        assertEquals(1, result!!.moveCount)
    }

    // ── moveToFoundationFromTableau ───────────────────────────────────────────

    @Test
    fun moveToFoundationFromTableau_aceOnEmpty_placesOnFoundation() {
        val ace = Card(Rank.ACE, Suit.SPADES)
        val state = stateWithTableau(listOf(ace))
        val result = FreeCellController.moveToFoundationFromTableau(state, fromCol = 0)
        assertNotNull(result)
        assertEquals(listOf(ace), result!!.foundations[Suit.SPADES.ordinal])
        assertTrue(result.tableau[0].isEmpty())
    }

    @Test
    fun moveToFoundationFromTableau_nonAceOnEmpty_returnsNull() {
        val two = Card(Rank.TWO, Suit.SPADES)
        val state = stateWithTableau(listOf(two))
        assertNull(FreeCellController.moveToFoundationFromTableau(state, fromCol = 0))
    }

    @Test
    fun moveToFoundationFromTableau_twoOnAce_succeeds() {
        val ace = Card(Rank.ACE, Suit.HEARTS)
        val two = Card(Rank.TWO, Suit.HEARTS)
        val state = stateWithTableau(
            listOf(two),
            foundations = listOf(emptyList(), emptyList(), listOf(ace), emptyList())
        )
        val result = FreeCellController.moveToFoundationFromTableau(state, fromCol = 0)
        assertNotNull(result)
        assertEquals(listOf(ace, two), result!!.foundations[Suit.HEARTS.ordinal])
    }

    // ── moveTableauToTableau ──────────────────────────────────────────────────

    @Test
    fun moveTableauToTableau_singleCardValidMove_succeeds() {
        val twoSpades = Card(Rank.TWO, Suit.SPADES)   // black
        val threeHearts = Card(Rank.THREE, Suit.HEARTS) // red
        val state = stateWithTableau(listOf(twoSpades), listOf(threeHearts))
        val result = FreeCellController.moveTableauToTableau(state, fromCol = 0, fromRow = 0, toCol = 1)
        assertNotNull(result)
        assertTrue(result!!.tableau[0].isEmpty())
        assertEquals(listOf(threeHearts, twoSpades), result.tableau[1])
    }

    @Test
    fun moveTableauToTableau_singleCardToEmptyColumn_succeeds() {
        val card = Card(Rank.SEVEN, Suit.HEARTS)
        val state = stateWithTableau(listOf(card), emptyList())
        val result = FreeCellController.moveTableauToTableau(state, fromCol = 0, fromRow = 0, toCol = 1)
        assertNotNull(result)
        assertTrue(result!!.tableau[0].isEmpty())
        assertEquals(listOf(card), result.tableau[1])
    }

    @Test
    fun moveTableauToTableau_sameColorTarget_returnsNull() {
        val twoSpades = Card(Rank.TWO, Suit.SPADES)  // black
        val threeClubs = Card(Rank.THREE, Suit.CLUBS) // also black
        val state = stateWithTableau(listOf(twoSpades), listOf(threeClubs))
        assertNull(FreeCellController.moveTableauToTableau(state, fromCol = 0, fromRow = 0, toCol = 1))
    }

    @Test
    fun moveTableauToTableau_sameColumn_returnsNull() {
        val card = Card(Rank.FIVE, Suit.HEARTS)
        val state = stateWithTableau(listOf(card))
        assertNull(FreeCellController.moveTableauToTableau(state, fromCol = 0, fromRow = 0, toCol = 0))
    }

    @Test
    fun moveTableauToTableau_twoCardSequenceWithCapacity_succeeds() {
        // Sequence: [3H(red), 2S(black)] → valid; capacity with 2 free cells = (2+1)*1 = 3 ≥ 2
        val threeHearts = Card(Rank.THREE, Suit.HEARTS)
        val twoSpades = Card(Rank.TWO, Suit.SPADES)
        val fourSpades = Card(Rank.FOUR, Suit.SPADES) // black 4 — 3H can go on it
        val state = stateWithTableau(
            listOf(threeHearts, twoSpades),
            listOf(fourSpades),
            freeCells = listOf(null, null, Card(Rank.ACE, Suit.CLUBS), Card(Rank.ACE, Suit.DIAMONDS))
        )
        val result = FreeCellController.moveTableauToTableau(state, fromCol = 0, fromRow = 0, toCol = 1)
        assertNotNull(result)
        assertEquals(listOf(fourSpades, threeHearts, twoSpades), result!!.tableau[1])
    }

    @Test
    fun moveTableauToTableau_exceedsSupermoveCapacity_returnsNull() {
        // Capacity = 1 (0 empty free cells, 0 empty columns). Moving 2 cards must fail.
        val threeHearts = Card(Rank.THREE, Suit.HEARTS)
        val twoSpades = Card(Rank.TWO, Suit.SPADES)
        val fourSpades = Card(Rank.FOUR, Suit.SPADES)
        val filler = listOf(Card(Rank.KING, Suit.CLUBS))
        // All free cells occupied, fill remaining columns so no empties.
        val allFreeCellsFull = listOf(
            Card(Rank.ACE, Suit.CLUBS),
            Card(Rank.ACE, Suit.DIAMONDS),
            Card(Rank.ACE, Suit.HEARTS),
            Card(Rank.ACE, Suit.SPADES)
        )
        val state = FreeCellState(
            tableau = listOf(
                listOf(threeHearts, twoSpades), // col 0: sequence to move
                listOf(fourSpades),             // col 1: target
                filler, filler, filler, filler, filler, filler
            ),
            freeCells = allFreeCellsFull,
            foundations = List(4) { emptyList() },
            freeCellCount = 4
        )
        assertNull(FreeCellController.moveTableauToTableau(state, fromCol = 0, fromRow = 0, toCol = 1))
    }

    @Test
    fun moveTableauToTableau_toEmptyColumn_doesNotCountEmptyAsExtra() {
        // 1 empty free cell, destination is the 1 empty column.
        // emptyColumnsExcludingDest = 1-1 = 0 → capacity = (1+1)*1 = 2.
        // Moving 2 cards should succeed.
        val threeHearts = Card(Rank.THREE, Suit.HEARTS)
        val twoSpades = Card(Rank.TWO, Suit.SPADES)
        val filler = listOf(Card(Rank.KING, Suit.CLUBS))
        val state = FreeCellState(
            tableau = listOf(
                listOf(threeHearts, twoSpades), // col 0: 2-card sequence to move
                emptyList(),                    // col 1: destination (empty)
                filler, filler, filler, filler, filler, filler
            ),
            freeCells = listOf(null, Card(Rank.JACK, Suit.CLUBS), Card(Rank.JACK, Suit.DIAMONDS), Card(Rank.JACK, Suit.HEARTS)),
            foundations = List(4) { emptyList() },
            freeCellCount = 4
        )
        val result = FreeCellController.moveTableauToTableau(state, fromCol = 0, fromRow = 0, toCol = 1)
        assertNotNull(result)
        assertEquals(listOf(threeHearts, twoSpades), result!!.tableau[1])
    }

    @Test
    fun moveTableauToTableau_invalidSequenceSource_returnsNull() {
        // col 0: [5S(black), 4S(black)] — same color, not a valid sequence
        val fiveSpades = Card(Rank.FIVE, Suit.SPADES)
        val fourSpades = Card(Rank.FOUR, Suit.SPADES)
        val sixHearts = Card(Rank.SIX, Suit.HEARTS)
        val state = stateWithTableau(listOf(fiveSpades, fourSpades), listOf(sixHearts))
        assertNull(FreeCellController.moveTableauToTableau(state, fromCol = 0, fromRow = 0, toCol = 1))
    }

    // ── moveFromFreeCellToTableau ─────────────────────────────────────────────

    @Test
    fun moveFromFreeCellToTableau_valid_placesCard() {
        val twoSpades = Card(Rank.TWO, Suit.SPADES)
        val threeHearts = Card(Rank.THREE, Suit.HEARTS)
        val state = stateWithTableau(
            listOf(threeHearts),
            freeCells = listOf(twoSpades, null, null, null)
        )
        val result = FreeCellController.moveFromFreeCellToTableau(state, cellIndex = 0, toCol = 0)
        assertNotNull(result)
        assertNull(result!!.freeCells[0])
        assertEquals(listOf(threeHearts, twoSpades), result.tableau[0])
    }

    @Test
    fun moveFromFreeCellToTableau_incompatible_returnsNull() {
        val twoSpades = Card(Rank.TWO, Suit.SPADES)   // black
        val threeSpades = Card(Rank.THREE, Suit.SPADES) // also black — invalid
        val state = stateWithTableau(
            listOf(threeSpades),
            freeCells = listOf(twoSpades, null, null, null)
        )
        assertNull(FreeCellController.moveFromFreeCellToTableau(state, cellIndex = 0, toCol = 0))
    }

    @Test
    fun moveFromFreeCellToTableau_emptyCell_returnsNull() {
        val state = stateWithTableau(listOf(Card(Rank.THREE, Suit.HEARTS)))
        assertNull(FreeCellController.moveFromFreeCellToTableau(state, cellIndex = 0, toCol = 0))
    }

    // ── moveFromFreeCellToFoundation ──────────────────────────────────────────

    @Test
    fun moveFromFreeCellToFoundation_aceToEmpty_succeeds() {
        val ace = Card(Rank.ACE, Suit.CLUBS)
        val state = stateWithTableau(freeCells = listOf(ace, null, null, null))
        val result = FreeCellController.moveFromFreeCellToFoundation(state, cellIndex = 0)
        assertNotNull(result)
        assertNull(result!!.freeCells[0])
        assertEquals(listOf(ace), result.foundations[Suit.CLUBS.ordinal])
    }

    @Test
    fun moveFromFreeCellToFoundation_nonFitCard_returnsNull() {
        val two = Card(Rank.TWO, Suit.CLUBS)
        val state = stateWithTableau(freeCells = listOf(two, null, null, null))
        assertNull(FreeCellController.moveFromFreeCellToFoundation(state, cellIndex = 0))
    }

    // ── isWon ─────────────────────────────────────────────────────────────────

    @Test
    fun isWon_allFoundationsComplete_returnsTrue() {
        val suits = Suit.entries
        val fullFoundations = suits.map { suit ->
            Rank.entries.map { rank -> Card(rank, suit) }
        }
        val state = emptyState().copy(foundations = fullFoundations)
        assertTrue(FreeCellController.isWon(state))
    }

    @Test
    fun isWon_partialFoundations_returnsFalse() {
        val state = stateWithTableau(
            foundations = listOf(
                listOf(Card(Rank.ACE, Suit.CLUBS)),
                emptyList(), emptyList(), emptyList()
            )
        )
        assertFalse(FreeCellController.isWon(state))
    }

    @Test
    fun isWon_freshDeal_returnsFalse() {
        val state = FreeCellController.deal(42L)
        assertFalse(FreeCellController.isWon(state))
    }

    // ── hasLegalMoves ─────────────────────────────────────────────────────────

    @Test
    fun hasLegalMoves_emptyFreeCellAndNonEmptyTableau_returnsTrue() {
        val state = stateWithTableau(listOf(Card(Rank.FIVE, Suit.HEARTS)))
        assertTrue(FreeCellController.hasLegalMoves(state))
    }

    @Test
    fun hasLegalMoves_aceOnTableauCanGoToFoundation_returnsTrue() {
        val state = stateWithTableau(
            listOf(Card(Rank.ACE, Suit.SPADES)),
            freeCells = List(4) { Card(Rank.KING, Suit.HEARTS) } // all free cells full
        )
        assertTrue(FreeCellController.hasLegalMoves(state))
    }

    // ── autoMoveToFoundations ─────────────────────────────────────────────────

    @Test
    fun autoMoveToFoundations_acesOnTableau_movedToFoundations() {
        val aceSpades = Card(Rank.ACE, Suit.SPADES)
        val aceHearts = Card(Rank.ACE, Suit.HEARTS)
        val state = stateWithTableau(listOf(aceSpades), listOf(aceHearts))
        val result = FreeCellController.autoMoveToFoundations(state)
        assertEquals(listOf(aceSpades), result.foundations[Suit.SPADES.ordinal])
        assertEquals(listOf(aceHearts), result.foundations[Suit.HEARTS.ordinal])
    }

    @Test
    fun autoMoveToFoundations_aceInFreeCell_movedToFoundation() {
        val ace = Card(Rank.ACE, Suit.DIAMONDS)
        val state = stateWithTableau(freeCells = listOf(ace, null, null, null))
        val result = FreeCellController.autoMoveToFoundations(state)
        assertEquals(listOf(ace), result.foundations[Suit.DIAMONDS.ordinal])
        assertNull(result.freeCells[0])
    }

    @Test
    fun autoMoveToFoundations_twoAfterAce_movedWhenSafe() {
        // Ace already on foundation; Two on tableau. Two rank ≤ 2 so always safe.
        val ace = Card(Rank.ACE, Suit.CLUBS)
        val two = Card(Rank.TWO, Suit.CLUBS)
        val state = stateWithTableau(
            listOf(two),
            foundations = listOf(listOf(ace), emptyList(), emptyList(), emptyList())
        )
        val result = FreeCellController.autoMoveToFoundations(state)
        assertEquals(listOf(ace, two), result.foundations[Suit.CLUBS.ordinal])
    }

    @Test
    fun autoMoveToFoundations_highCardNotSafe_notMoved() {
        // 5 of hearts on tableau; no red/black predecessor guarantee → NOT safe
        val five = Card(Rank.FIVE, Suit.HEARTS)
        val state = stateWithTableau(listOf(five))
        val result = FreeCellController.autoMoveToFoundations(state)
        // 5H should still be on tableau (foundation pile for hearts has < 4 cards)
        assertTrue(result.tableau[0].contains(five))
    }

    // ── misc / integration ────────────────────────────────────────────────────

    @Test
    fun moveToFreeCell_thenMoveToTableau_roundTrip() {
        val card = Card(Rank.TWO, Suit.SPADES)
        val threeHearts = Card(Rank.THREE, Suit.HEARTS)
        val initial = stateWithTableau(listOf(card), listOf(threeHearts))
        val afterToCell = FreeCellController.moveToFreeCell(initial, fromCol = 0, cellIndex = 0)
        assertNotNull(afterToCell)
        val afterBack = FreeCellController.moveFromFreeCellToTableau(afterToCell!!, cellIndex = 0, toCol = 1)
        assertNotNull(afterBack)
        assertEquals(listOf(threeHearts, card), afterBack!!.tableau[1])
        assertNull(afterBack.freeCells[0])
    }

    @Test
    fun moveCounts_incrementPerMove() {
        val state = FreeCellController.deal(7L)
        // Find the first free column we can move to from col 0
        val initialMoves = state.moveCount
        // Move to free cell should increment by 1
        val afterFreeCell = FreeCellController.moveToFreeCell(state, fromCol = 0, cellIndex = 0)
        assertNotNull(afterFreeCell)
        assertEquals(initialMoves + 1, afterFreeCell!!.moveCount)
    }
}
