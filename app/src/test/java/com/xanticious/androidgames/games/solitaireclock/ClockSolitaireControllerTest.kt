package com.xanticious.androidgames.games.solitaireclock

import com.xanticious.androidgames.controller.games.solitaireclock.ClockSolitaireController
import com.xanticious.androidgames.controller.games.solitaireclock.FlipOutcome
import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.Rank
import com.xanticious.androidgames.model.games.cards.Suit
import com.xanticious.androidgames.model.games.solitaireclock.ClockMode
import com.xanticious.androidgames.model.games.solitaireclock.ClockPile
import com.xanticious.androidgames.model.games.solitaireclock.ClockSolitaireState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockSolitaireControllerTest {

    // ── Deal ────────────────────────────────────────────────────────────────

    @Test
    fun deal_classicMode_creates13Piles() {
        val state = ClockSolitaireController.deal(42L, ClockMode.CLASSIC)
        assertEquals(13, state.piles.size)
    }

    @Test
    fun deal_classicMode_eachPileHas4Cards() {
        val state = ClockSolitaireController.deal(42L, ClockMode.CLASSIC)
        state.piles.forEach { pile ->
            assertEquals(4, pile.faceDownCards.size + pile.faceUpCards.size)
        }
    }

    @Test
    fun deal_classicMode_allCardsFaceDown() {
        val state = ClockSolitaireController.deal(42L, ClockMode.CLASSIC)
        state.piles.forEach { pile ->
            assertTrue(pile.faceUpCards.isEmpty())
        }
    }

    @Test
    fun deal_classicMode_startingPileIsCenter() {
        val state = ClockSolitaireController.deal(42L, ClockMode.CLASSIC)
        assertEquals(12, state.currentPileIndex)
    }

    @Test
    fun deal_classicMode_kingsUpIsZero() {
        val state = ClockSolitaireController.deal(42L, ClockMode.CLASSIC)
        assertEquals(0, state.kingsUp)
    }

    @Test
    fun deal_alwaysPossibleMode_producesWinnableDeal() {
        val state = ClockSolitaireController.deal(42L, ClockMode.ALWAYS_POSSIBLE)
        var s = state
        var outcome = FlipOutcome.CONTINUE
        for (i in 0 until 52) {
            val result = ClockSolitaireController.flip(s)
            s = result.state
            outcome = result.outcome
            if (outcome != FlipOutcome.CONTINUE) break
        }
        assertEquals(FlipOutcome.WON, outcome)
    }

    // ── Placement routing by rank ────────────────────────────────────────────

    @Test
    fun flip_aceCard_routesToPileZero() {
        val state = stateWithTopCard(Rank.ACE, Suit.SPADES, withTargetFaceDown = true)
        val result = ClockSolitaireController.flip(state)
        assertEquals(0, result.state.currentPileIndex)
    }

    @Test
    fun flip_queenCard_routesToPileEleven() {
        val state = stateWithTopCard(Rank.QUEEN, Suit.HEARTS, withTargetFaceDown = true)
        val result = ClockSolitaireController.flip(state)
        assertEquals(11, result.state.currentPileIndex)
    }

    @Test
    fun flip_kingCard_routesToPileTwelve() {
        // King self-routes to center; center pile must have another face-down card below.
        val kingCard = Card(Rank.KING, Suit.SPADES, faceUp = false)
        val spare = Card(Rank.TWO, Suit.CLUBS, faceUp = false)
        val centerPile = ClockPile(12, faceDownCards = listOf(spare, kingCard), faceUpCards = emptyList())
        val piles = (0 until 13).map { i -> if (i == 12) centerPile else emptyPile(i) }
        val state = ClockSolitaireState(piles, currentPileIndex = 12, kingsUp = 0, elapsedSeconds = 0f, mode = ClockMode.CLASSIC)
        val result = ClockSolitaireController.flip(state)
        assertEquals(12, result.state.currentPileIndex)
    }

    @Test
    fun flip_kingCard_incrementsKingsUp() {
        val kingCard = Card(Rank.KING, Suit.SPADES, faceUp = false)
        val spare = Card(Rank.TWO, Suit.CLUBS, faceUp = false)
        val twosPile = ClockPile(1, faceDownCards = listOf(spare), faceUpCards = emptyList())
        val centerPile = ClockPile(12, faceDownCards = listOf(spare, kingCard), faceUpCards = emptyList())
        val piles = (0 until 13).map { i ->
            when (i) {
                1 -> twosPile
                12 -> centerPile
                else -> emptyPile(i)
            }
        }
        val state = ClockSolitaireState(piles, currentPileIndex = 12, kingsUp = 0, elapsedSeconds = 0f, mode = ClockMode.CLASSIC)
        val result = ClockSolitaireController.flip(state)
        assertEquals(1, result.state.kingsUp)
    }

    @Test
    fun flip_placesCardInTargetPileFaceUpCards() {
        val state = stateWithTopCard(Rank.ACE, Suit.SPADES, withTargetFaceDown = true)
        val result = ClockSolitaireController.flip(state)
        val acePile = result.state.piles[0]
        assertTrue(acePile.faceUpCards.any { it.rank == Rank.ACE && it.suit == Suit.SPADES && it.faceUp })
    }

    // ── Fourth-King loss / win detection ────────────────────────────────────

    @Test
    fun flip_fourthKing_withAllOtherPilesDone_returnsWon() {
        val lastKing = Card(Rank.KING, Suit.SPADES, faceUp = false)
        val centerPile = ClockPile(
            pileIndex = 12,
            faceDownCards = listOf(lastKing),
            faceUpCards = listOf(
                Card(Rank.KING, Suit.CLUBS, true),
                Card(Rank.KING, Suit.DIAMONDS, true),
                Card(Rank.KING, Suit.HEARTS, true)
            )
        )
        val completedHourPiles = (0 until 12).map { i ->
            val rank = Rank.entries[i]
            ClockPile(
                pileIndex = i,
                faceDownCards = emptyList(),
                faceUpCards = listOf(
                    Card(rank, Suit.CLUBS, true),
                    Card(rank, Suit.DIAMONDS, true),
                    Card(rank, Suit.HEARTS, true),
                    Card(rank, Suit.SPADES, true)
                )
            )
        }
        val piles = completedHourPiles + centerPile
        val state = ClockSolitaireState(piles, currentPileIndex = 12, kingsUp = 3, elapsedSeconds = 0f, mode = ClockMode.CLASSIC)
        val result = ClockSolitaireController.flip(state)
        assertEquals(FlipOutcome.WON, result.outcome)
    }

    @Test
    fun flip_fourthKing_withOtherPilesIncomplete_returnsLost() {
        val lastKing = Card(Rank.KING, Suit.SPADES, faceUp = false)
        val centerPile = ClockPile(
            pileIndex = 12,
            faceDownCards = listOf(lastKing),
            faceUpCards = listOf(
                Card(Rank.KING, Suit.CLUBS, true),
                Card(Rank.KING, Suit.DIAMONDS, true),
                Card(Rank.KING, Suit.HEARTS, true)
            )
        )
        val incompletePile = ClockPile(
            pileIndex = 0,
            faceDownCards = listOf(Card(Rank.ACE, Suit.CLUBS, false)),
            faceUpCards = emptyList()
        )
        val piles = (0 until 13).map { i ->
            when (i) {
                0 -> incompletePile
                12 -> centerPile
                else -> {
                    val rank = Rank.entries[i]
                    ClockPile(
                        i, emptyList(),
                        listOf(
                            Card(rank, Suit.CLUBS, true),
                            Card(rank, Suit.DIAMONDS, true),
                            Card(rank, Suit.HEARTS, true),
                            Card(rank, Suit.SPADES, true)
                        )
                    )
                }
            }
        }
        val state = ClockSolitaireState(piles, currentPileIndex = 12, kingsUp = 3, elapsedSeconds = 0f, mode = ClockMode.CLASSIC)
        val result = ClockSolitaireController.flip(state)
        assertEquals(FlipOutcome.LOST, result.outcome)
    }

    // ── Stuck-pile loss ──────────────────────────────────────────────────────

    @Test
    fun flip_currentPileHasNoFaceDownCards_returnsLost() {
        val piles = (0 until 13).map { emptyPile(it) }
        val state = ClockSolitaireState(piles, currentPileIndex = 12, kingsUp = 0, elapsedSeconds = 0f, mode = ClockMode.CLASSIC)
        val result = ClockSolitaireController.flip(state)
        assertEquals(FlipOutcome.LOST, result.outcome)
    }

    // ── Timer ────────────────────────────────────────────────────────────────

    @Test
    fun tick_withPositiveDelta_advancesElapsedSeconds() {
        val state = minimalState().copy(elapsedSeconds = 10f)
        val newState = ClockSolitaireController.tick(state, 1.5f)
        assertEquals(11.5f, newState.elapsedSeconds, 0.001f)
    }

    @Test
    fun tick_withZeroDelta_doesNotChangeElapsed() {
        val state = minimalState().copy(elapsedSeconds = 5f)
        val newState = ClockSolitaireController.tick(state, 0f)
        assertEquals(5f, newState.elapsedSeconds, 0.001f)
    }

    // ── Time formatting ──────────────────────────────────────────────────────

    @Test
    fun formatTime_zero_returnsDoubleZero() {
        assertEquals("00:00", ClockSolitaireController.formatTime(0f))
    }

    @Test
    fun formatTime_65seconds_returnsOneMinuteFiveSeconds() {
        assertEquals("01:05", ClockSolitaireController.formatTime(65f))
    }

    @Test
    fun formatTime_3599seconds_returnsMaxBeforeHour() {
        assertEquals("59:59", ClockSolitaireController.formatTime(3599f))
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** An empty pile at [pileIndex] with no cards in either list. */
    private fun emptyPile(pileIndex: Int) =
        ClockPile(pileIndex, emptyList(), emptyList())

    /**
     * A state where pile 12 (center) has [rank]+[suit] as its top face-down card.
     * If [withTargetFaceDown] is true, the target pile also gets a spare face-down
     * card so the flip result is CONTINUE rather than LOST (stuck).
     */
    private fun stateWithTopCard(rank: Rank, suit: Suit, withTargetFaceDown: Boolean): ClockSolitaireState {
        val topCard = Card(rank, suit, faceUp = false)
        val spare = Card(Rank.FIVE, Suit.CLUBS, faceUp = false)
        val targetIndex = rank.value - 1
        val piles = (0 until 13).map { i ->
            when {
                i == 12 && i == targetIndex -> ClockPile(i, listOf(topCard), emptyList())
                i == 12 -> ClockPile(i, listOf(topCard), emptyList())
                i == targetIndex && withTargetFaceDown -> ClockPile(i, listOf(spare), emptyList())
                else -> emptyPile(i)
            }
        }
        return ClockSolitaireState(piles, currentPileIndex = 12, kingsUp = 0, elapsedSeconds = 0f, mode = ClockMode.CLASSIC)
    }

    private fun minimalState() = ClockSolitaireState(
        piles = (0 until 13).map { emptyPile(it) },
        currentPileIndex = 12,
        kingsUp = 0,
        elapsedSeconds = 0f,
        mode = ClockMode.CLASSIC
    )
}
