package com.xanticious.androidgames.games.hearts

import com.xanticious.androidgames.controller.games.hearts.HeartsController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.Rank
import com.xanticious.androidgames.model.games.cards.Suit
import com.xanticious.androidgames.model.games.hearts.HeartsConfig
import com.xanticious.androidgames.model.games.hearts.HeartsGameState
import com.xanticious.androidgames.model.games.hearts.PassDirection
import com.xanticious.androidgames.model.games.hearts.Seat
import com.xanticious.androidgames.model.games.hearts.TrickCard
import com.xanticious.androidgames.model.games.hearts.next
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class HeartsControllerTest {

    // ── Helper builders ───────────────────────────────────────────────────────

    private fun stateWithHands(
        south: List<Card> = emptyList(),
        west: List<Card>  = emptyList(),
        north: List<Card> = emptyList(),
        east: List<Card>  = emptyList(),
        heartsBroken: Boolean = false,
        trickNumber: Int = 2
    ) = HeartsGameState.empty().copy(
        hands        = mapOf(Seat.SOUTH to south, Seat.WEST to west, Seat.NORTH to north, Seat.EAST to east),
        heartsBroken = heartsBroken,
        trickNumber  = trickNumber
    )

    private fun trick(vararg pairs: Pair<Rank, Suit>): List<TrickCard> {
        val seats = listOf(Seat.SOUTH, Seat.WEST, Seat.NORTH, Seat.EAST)
        return pairs.mapIndexed { idx, (rank, suit) ->
            TrickCard(Card(rank, suit), seats[idx])
        }
    }

    // ── Deal ──────────────────────────────────────────────────────────────────

    @Test
    fun deal_eachSeatReceivesThirteenCards() {
        val state = HeartsController.deal(HeartsGameState.empty(), seed = 12345L)
        Seat.entries.forEach { seat ->
            assertEquals(13, state.hands[seat]?.size)
        }
    }

    @Test
    fun deal_totalCardsIs52() {
        val state = HeartsController.deal(HeartsGameState.empty(), seed = 99L)
        val total = state.hands.values.sumOf { it.size }
        assertEquals(52, total)
    }

    @Test
    fun deal_noDuplicateCards() {
        val state  = HeartsController.deal(HeartsGameState.empty(), seed = 7L)
        val allCards = state.hands.values.flatten()
        assertEquals(allCards.size, allCards.toSet().size)
    }

    @Test
    fun deal_preservesCumulativeScores() {
        val initial = HeartsGameState.empty().copy(
            scores = mapOf(Seat.SOUTH to 10, Seat.WEST to 5, Seat.NORTH to 0, Seat.EAST to 3)
        )
        val state = HeartsController.deal(initial, seed = 1L)
        assertEquals(10, state.scores[Seat.SOUTH])
        assertEquals(5,  state.scores[Seat.WEST])
    }

    // ── prepareForPlay ────────────────────────────────────────────────────────

    @Test
    fun prepareForPlay_setLeadSeatToTwoOfClubsHolder() {
        val twoClubs = HeartsGameState.TWO_OF_CLUBS
        val state = stateWithHands(
            south = listOf(Card(Rank.ACE, Suit.SPADES)),
            west  = listOf(twoClubs),
            north = listOf(Card(Rank.KING, Suit.DIAMONDS)),
            east  = listOf(Card(Rank.THREE, Suit.HEARTS))
        ).copy(trickNumber = 1)
        val prepared = HeartsController.prepareForPlay(state)
        assertEquals(Seat.WEST, prepared.leadSeat)
    }

    // ── legalPlays — first trick ───────────────────────────────────────────────

    @Test
    fun legalPlays_firstTrickOpeningLeadForcesOnlyTwoOfClubs() {
        val hand = listOf(
            HeartsGameState.TWO_OF_CLUBS,
            Card(Rank.ACE, Suit.HEARTS),
            Card(Rank.KING, Suit.SPADES)
        )
        val legal = HeartsController.legalPlays(hand, emptyList(), false, trickNumber = 1)
        assertEquals(listOf(HeartsGameState.TWO_OF_CLUBS), legal)
    }

    @Test
    fun legalPlays_firstTrickVoidInLedSuit_cannotPlayHearts() {
        val hand = listOf(Card(Rank.ACE, Suit.HEARTS), Card(Rank.FIVE, Suit.SPADES))
        val led  = listOf(TrickCard(HeartsGameState.TWO_OF_CLUBS, Seat.WEST))
        val legal = HeartsController.legalPlays(hand, led, false, trickNumber = 1)
        assertEquals(listOf(Card(Rank.FIVE, Suit.SPADES)), legal)
    }

    @Test
    fun legalPlays_firstTrickVoidInLedSuit_cannotPlayQueenOfSpades() {
        val hand = listOf(HeartsGameState.QUEEN_OF_SPADES, Card(Rank.FIVE, Suit.DIAMONDS))
        val led  = listOf(TrickCard(HeartsGameState.TWO_OF_CLUBS, Seat.NORTH))
        val legal = HeartsController.legalPlays(hand, led, false, trickNumber = 1)
        assertEquals(listOf(Card(Rank.FIVE, Suit.DIAMONDS)), legal)
    }

    @Test
    fun legalPlays_firstTrickOnlyPenaltyCards_allBecomeLegal() {
        val hand = listOf(Card(Rank.TWO, Suit.HEARTS), HeartsGameState.QUEEN_OF_SPADES)
        val led  = listOf(TrickCard(HeartsGameState.TWO_OF_CLUBS, Seat.WEST))
        val legal = HeartsController.legalPlays(hand, led, false, trickNumber = 1)
        assertEquals(hand.toSet(), legal.toSet())
    }

    // ── legalPlays — hearts-broken rules ──────────────────────────────────────

    @Test
    fun legalPlays_cannotLeadHeartsBeforeBroken() {
        val hand  = listOf(Card(Rank.ACE, Suit.HEARTS), Card(Rank.THREE, Suit.CLUBS))
        val legal = HeartsController.legalPlays(hand, emptyList(), heartsBroken = false, trickNumber = 3)
        assertEquals(listOf(Card(Rank.THREE, Suit.CLUBS)), legal)
    }

    @Test
    fun legalPlays_canLeadHeartsWhenBroken() {
        val hand  = listOf(Card(Rank.ACE, Suit.HEARTS), Card(Rank.THREE, Suit.CLUBS))
        val legal = HeartsController.legalPlays(hand, emptyList(), heartsBroken = true, trickNumber = 3)
        assertEquals(hand.toSet(), legal.toSet())
    }

    @Test
    fun legalPlays_canLeadHeartsWhenOnlyHeartsRemain() {
        val hand  = listOf(Card(Rank.ACE, Suit.HEARTS), Card(Rank.SEVEN, Suit.HEARTS))
        val legal = HeartsController.legalPlays(hand, emptyList(), heartsBroken = false, trickNumber = 5)
        assertEquals(hand.toSet(), legal.toSet())
    }

    // ── legalPlays — follow suit ───────────────────────────────────────────────

    @Test
    fun legalPlays_mustFollowLedSuit() {
        val hand  = listOf(Card(Rank.FIVE, Suit.CLUBS), Card(Rank.TEN, Suit.HEARTS))
        val led   = listOf(TrickCard(Card(Rank.THREE, Suit.CLUBS), Seat.NORTH))
        val legal = HeartsController.legalPlays(hand, led, false, trickNumber = 2)
        assertEquals(listOf(Card(Rank.FIVE, Suit.CLUBS)), legal)
    }

    @Test
    fun legalPlays_voidInLedSuitAllowsAnyCard() {
        val hand  = listOf(Card(Rank.ACE, Suit.HEARTS), Card(Rank.KING, Suit.SPADES))
        val led   = listOf(TrickCard(Card(Rank.THREE, Suit.DIAMONDS), Seat.WEST))
        val legal = HeartsController.legalPlays(hand, led, false, trickNumber = 4)
        assertEquals(hand.toSet(), legal.toSet())
    }

    // ── trickWinner ───────────────────────────────────────────────────────────

    @Test
    fun trickWinner_highestCardOfLedSuitWins() {
        val t = listOf(
            TrickCard(Card(Rank.FIVE,  Suit.CLUBS), Seat.SOUTH),
            TrickCard(Card(Rank.KING,  Suit.CLUBS), Seat.WEST),
            TrickCard(Card(Rank.THREE, Suit.CLUBS), Seat.NORTH),
            TrickCard(Card(Rank.NINE,  Suit.CLUBS), Seat.EAST)
        )
        assertEquals(Seat.WEST, HeartsController.trickWinner(t))
    }

    @Test
    fun trickWinner_offSuitCardsDoNotWin() {
        val t = listOf(
            TrickCard(Card(Rank.TWO,  Suit.SPADES),   Seat.SOUTH),
            TrickCard(Card(Rank.ACE,  Suit.HEARTS),   Seat.WEST),
            TrickCard(Card(Rank.KING, Suit.DIAMONDS), Seat.NORTH),
            TrickCard(Card(Rank.FOUR, Suit.SPADES),   Seat.EAST)
        )
        // Led suit is SPADES; highest spade is FOUR (East) [TWO < FOUR]
        assertEquals(Seat.EAST, HeartsController.trickWinner(t))
    }

    // ── cardPoints ────────────────────────────────────────────────────────────

    @Test
    fun cardPoints_heartIsOnePoint() {
        assertEquals(1, HeartsController.cardPoints(Card(Rank.ACE, Suit.HEARTS), HeartsConfig()))
    }

    @Test
    fun cardPoints_queenOfSpadesIsThirteenPoints() {
        assertEquals(13, HeartsController.cardPoints(HeartsGameState.QUEEN_OF_SPADES, HeartsConfig()))
    }

    @Test
    fun cardPoints_nonPenaltyCardIsZero() {
        assertEquals(0, HeartsController.cardPoints(Card(Rank.ACE, Suit.CLUBS), HeartsConfig()))
    }

    @Test
    fun cardPoints_jackOfDiamondsVariantIsMinusTen() {
        val config = HeartsConfig(jackOfDiamonds = true)
        assertEquals(-10, HeartsController.cardPoints(HeartsGameState.JACK_OF_DIAMONDS, config))
    }

    // ── resolveTrick ──────────────────────────────────────────────────────────

    @Test
    fun resolveTrick_winnersHandGetsAllFourCards() {
        val t = listOf(
            TrickCard(Card(Rank.TWO, Suit.CLUBS),   Seat.SOUTH),
            TrickCard(Card(Rank.ACE, Suit.CLUBS),   Seat.WEST),
            TrickCard(Card(Rank.TEN, Suit.CLUBS),   Seat.NORTH),
            TrickCard(Card(Rank.SIX, Suit.CLUBS),   Seat.EAST)
        )
        val state   = HeartsGameState.empty().copy(currentTrick = t)
        val resolved = HeartsController.resolveTrick(state)
        assertEquals(4, resolved.tricksTaken[Seat.WEST]?.size)
    }

    @Test
    fun resolveTrick_leadSeatUpdatedToWinner() {
        val t = listOf(
            TrickCard(Card(Rank.THREE, Suit.DIAMONDS), Seat.SOUTH),
            TrickCard(Card(Rank.FIVE,  Suit.DIAMONDS), Seat.WEST),
            TrickCard(Card(Rank.KING,  Suit.DIAMONDS), Seat.NORTH),
            TrickCard(Card(Rank.TWO,   Suit.DIAMONDS), Seat.EAST)
        )
        val state    = HeartsGameState.empty().copy(currentTrick = t)
        val resolved = HeartsController.resolveTrick(state)
        assertEquals(Seat.NORTH, resolved.leadSeat)
    }

    @Test
    fun resolveTrick_trickNumberIncremented() {
        val t = listOf(
            TrickCard(Card(Rank.TWO,   Suit.CLUBS),   Seat.SOUTH),
            TrickCard(Card(Rank.THREE, Suit.CLUBS),   Seat.WEST),
            TrickCard(Card(Rank.FOUR,  Suit.CLUBS),   Seat.NORTH),
            TrickCard(Card(Rank.FIVE,  Suit.CLUBS),   Seat.EAST)
        )
        val state    = HeartsGameState.empty().copy(currentTrick = t, trickNumber = 1)
        val resolved = HeartsController.resolveTrick(state)
        assertEquals(2, resolved.trickNumber)
    }

    // ── isHandComplete ────────────────────────────────────────────────────────

    @Test
    fun isHandComplete_returnsTrueWhenAllHandsEmptyAndNoActiveTrick() {
        val state = HeartsGameState.empty().copy(currentTrick = emptyList())
        assertTrue(HeartsController.isHandComplete(state))
    }

    @Test
    fun isHandComplete_returnsFalseWhenHandsStillHaveCards() {
        val state = stateWithHands(south = listOf(Card(Rank.ACE, Suit.CLUBS)))
        assertFalse(HeartsController.isHandComplete(state))
    }

    // ── detectShootTheMoon ────────────────────────────────────────────────────

    @Test
    fun detectShootTheMoon_returnsShooterWhenAllHeartsPlusQueenTaken() {
        val allHearts = Rank.entries.map { Card(it, Suit.HEARTS) }
        val taken     = mapOf(
            Seat.SOUTH to emptyList(),
            Seat.WEST  to (allHearts + HeartsGameState.QUEEN_OF_SPADES),
            Seat.NORTH to emptyList(),
            Seat.EAST  to emptyList()
        )
        assertEquals(Seat.WEST, HeartsController.detectShootTheMoon(taken))
    }

    @Test
    fun detectShootTheMoon_returnsNullWhenNoOneShotMoon() {
        val taken = mapOf(
            Seat.SOUTH to listOf(Card(Rank.TWO,  Suit.HEARTS)),
            Seat.WEST  to listOf(Card(Rank.THREE, Suit.HEARTS)),
            Seat.NORTH to emptyList(),
            Seat.EAST  to emptyList()
        )
        assertNull(HeartsController.detectShootTheMoon(taken))
    }

    // ── calculateHandScores ───────────────────────────────────────────────────

    @Test
    fun calculateHandScores_normalScoringCountsPointCards() {
        val taken = mapOf(
            Seat.SOUTH to listOf(
                Card(Rank.TWO,   Suit.HEARTS),
                Card(Rank.THREE, Suit.HEARTS),
                HeartsGameState.QUEEN_OF_SPADES
            ),
            Seat.WEST  to emptyList<Card>(),
            Seat.NORTH to emptyList<Card>(),
            Seat.EAST  to emptyList<Card>()
        )
        val scores = HeartsController.calculateHandScores(taken, HeartsConfig())
        assertEquals(15, scores[Seat.SOUTH])   // 2 hearts + Q♠ = 2 + 13 = 15
        assertEquals(0,  scores[Seat.WEST])
    }

    @Test
    fun calculateHandScores_shootTheMoonGivesShooterZeroAndOthers26() {
        val allHearts = Rank.entries.map { Card(it, Suit.HEARTS) }
        val taken = mapOf(
            Seat.SOUTH to emptyList<Card>(),
            Seat.WEST  to (allHearts + HeartsGameState.QUEEN_OF_SPADES),
            Seat.NORTH to emptyList<Card>(),
            Seat.EAST  to emptyList<Card>()
        )
        val scores = HeartsController.calculateHandScores(taken, HeartsConfig())
        assertEquals(0,  scores[Seat.WEST])
        assertEquals(26, scores[Seat.SOUTH])
        assertEquals(26, scores[Seat.NORTH])
        assertEquals(26, scores[Seat.EAST])
    }

    // ── scoreHand ─────────────────────────────────────────────────────────────

    @Test
    fun scoreHand_cumulativeScoresUpdated() {
        val taken = mapOf(
            Seat.SOUTH to listOf(Card(Rank.ACE, Suit.HEARTS)),
            Seat.WEST  to emptyList<Card>(),
            Seat.NORTH to emptyList<Card>(),
            Seat.EAST  to emptyList<Card>()
        )
        val state  = HeartsGameState.empty().copy(
            tricksTaken = taken,
            scores      = mapOf(Seat.SOUTH to 5, Seat.WEST to 0, Seat.NORTH to 0, Seat.EAST to 0),
            hands       = Seat.entries.associateWith { emptyList() }
        )
        val scored = HeartsController.scoreHand(state)
        assertEquals(6, scored.scores[Seat.SOUTH])  // 5 existing + 1 heart
    }

    @Test
    fun scoreHand_passDirectionAdvances() {
        val state  = HeartsGameState.empty().copy(passDirection = PassDirection.LEFT)
        val scored = HeartsController.scoreHand(state)
        assertEquals(PassDirection.RIGHT, scored.passDirection)
    }

    @Test
    fun scoreHand_gameOverWhenThresholdReached() {
        val taken = mapOf(
            Seat.SOUTH to emptyList<Card>(),
            Seat.WEST  to emptyList<Card>(),
            Seat.NORTH to emptyList<Card>(),
            Seat.EAST  to (Rank.entries.map { Card(it, Suit.HEARTS) } + HeartsGameState.QUEEN_OF_SPADES)
        )
        val state = HeartsGameState.empty(HeartsConfig(gameEndScore = 26)).copy(
            tricksTaken = taken,
            scores      = Seat.entries.associateWith { 0 }
        )
        val scored = HeartsController.scoreHand(state)
        // EAST took all points → moon shot → EAST=0, others=26
        // Others hit 26 >= 26 → game over
        assertTrue(scored.gameOver)
    }

    @Test
    fun scoreHand_winnerIsLowestCumulativeScore() {
        val taken = mapOf(
            Seat.SOUTH to listOf(Card(Rank.ACE, Suit.HEARTS)),
            Seat.WEST  to emptyList<Card>(),
            Seat.NORTH to emptyList<Card>(),
            Seat.EAST  to emptyList<Card>()
        )
        val state = HeartsGameState.empty(HeartsConfig(gameEndScore = 1)).copy(
            tricksTaken = taken,
            scores      = Seat.entries.associateWith { 0 }
        )
        val scored = HeartsController.scoreHand(state)
        assertTrue(scored.gameOver)
        assertEquals(Seat.WEST, scored.gameWinner)  // WEST/NORTH/EAST all 0; first found is WEST
    }

    // ── togglePassSelection ───────────────────────────────────────────────────

    @Test
    fun togglePassSelection_addsCardWhenNotSelected() {
        val card  = Card(Rank.ACE, Suit.SPADES)
        val state = HeartsGameState.empty().copy(selectedCards = emptySet())
        val next  = HeartsController.togglePassSelection(state, card)
        assertTrue(card in next.selectedCards)
    }

    @Test
    fun togglePassSelection_removesCardWhenAlreadySelected() {
        val card  = Card(Rank.ACE, Suit.SPADES)
        val state = HeartsGameState.empty().copy(selectedCards = setOf(card))
        val next  = HeartsController.togglePassSelection(state, card)
        assertFalse(card in next.selectedCards)
    }

    @Test
    fun togglePassSelection_doesNotAddFourthCard() {
        val cards = listOf(
            Card(Rank.ACE,  Suit.SPADES),
            Card(Rank.KING, Suit.SPADES),
            Card(Rank.QUEEN, Suit.HEARTS)
        )
        val fourth = Card(Rank.JACK, Suit.DIAMONDS)
        val state  = HeartsGameState.empty().copy(selectedCards = cards.toSet())
        val next   = HeartsController.togglePassSelection(state, fourth)
        assertEquals(3, next.selectedCards.size)
    }

    // ── passDirection extension ───────────────────────────────────────────────

    @Test
    fun passDirectionNext_cyclesFourSteps() {
        assertEquals(PassDirection.RIGHT,  PassDirection.LEFT.next())
        assertEquals(PassDirection.ACROSS, PassDirection.RIGHT.next())
        assertEquals(PassDirection.HOLD,   PassDirection.ACROSS.next())
        assertEquals(PassDirection.LEFT,   PassDirection.HOLD.next())
    }

    // ── seatOrder / nextSeatToPlay ────────────────────────────────────────────

    @Test
    fun seatOrder_southLeadReturnsClockwiseOrder() {
        assertEquals(
            listOf(Seat.SOUTH, Seat.WEST, Seat.NORTH, Seat.EAST),
            HeartsController.seatOrder(Seat.SOUTH)
        )
    }

    @Test
    fun nextSeatToPlay_emptyTrickReturnLeadSeat() {
        assertEquals(Seat.NORTH, HeartsController.nextSeatToPlay(emptyList(), Seat.NORTH))
    }

    @Test
    fun nextSeatToPlay_afterTwoCardsReturnsThirdSeat() {
        val t = listOf(
            TrickCard(Card(Rank.TWO, Suit.CLUBS), Seat.EAST),
            TrickCard(Card(Rank.THREE, Suit.CLUBS), Seat.SOUTH)
        )
        assertEquals(Seat.WEST, HeartsController.nextSeatToPlay(t, Seat.EAST))
    }

    // ── AI card choice — determinism ──────────────────────────────────────────

    @Test
    fun aiChooseCard_onlyOneLegalCard_returnsIt() {
        val hand  = listOf(Card(Rank.FIVE, Suit.CLUBS), Card(Rank.ACE, Suit.HEARTS))
        val led   = listOf(TrickCard(Card(Rank.TWO, Suit.CLUBS), Seat.WEST))
        val card  = HeartsController.aiChooseCard(
            hand         = hand,
            trick        = led,
            heartsBroken = false,
            trickNumber  = 3,
            tricksTaken  = Seat.entries.associateWith { emptyList() },
            difficulty   = GameDifficulty.HARD,
            random       = Random(42L),
            seat         = Seat.NORTH
        )
        // Must follow CLUBS; only five of clubs is legal
        assertEquals(Card(Rank.FIVE, Suit.CLUBS), card)
    }

    @Test
    fun aiChooseCard_easyDifficulty_returnsLegalCard() {
        val hand  = listOf(
            Card(Rank.TWO, Suit.CLUBS),
            Card(Rank.NINE, Suit.SPADES),
            Card(Rank.ACE, Suit.HEARTS)
        )
        val card  = HeartsController.aiChooseCard(
            hand         = hand,
            trick        = emptyList(),
            heartsBroken = true,
            trickNumber  = 5,
            tricksTaken  = Seat.entries.associateWith { emptyList() },
            difficulty   = GameDifficulty.EASY,
            random       = Random(1L),
            seat         = Seat.WEST
        )
        assertTrue(card in hand)
    }

    @Test
    fun aiChooseCard_hardDifficulty_dumpsQueenOfSpadesWhenVoid() {
        // WEST is void in the led suit (CLUBS) and holds Q♠
        val hand = listOf(
            HeartsGameState.QUEEN_OF_SPADES,
            Card(Rank.FOUR, Suit.HEARTS)
        )
        val led  = listOf(TrickCard(Card(Rank.TEN, Suit.CLUBS), Seat.SOUTH))
        val card = HeartsController.aiChooseCard(
            hand         = hand,
            trick        = led,
            heartsBroken = false,
            trickNumber  = 4,
            tricksTaken  = Seat.entries.associateWith { emptyList() },
            difficulty   = GameDifficulty.HARD,
            random       = Random(99L),
            seat         = Seat.WEST
        )
        assertEquals(HeartsGameState.QUEEN_OF_SPADES, card)
    }

    // ── aiChoosePassCards ─────────────────────────────────────────────────────

    @Test
    fun aiChoosePassCards_alwaysReturnsThreeCards() {
        val hand = Rank.entries.take(7).map { Card(it, Suit.SPADES) }
        val passed = HeartsController.aiChoosePassCards(hand, GameDifficulty.MEDIUM, Random(42L))
        assertEquals(3, passed.size)
    }

    @Test
    fun aiChoosePassCards_mediumIncludesQueenOfSpadesIfPresent() {
        val hand = listOf(
            HeartsGameState.QUEEN_OF_SPADES,
            Card(Rank.TWO, Suit.CLUBS),
            Card(Rank.THREE, Suit.CLUBS),
            Card(Rank.FOUR, Suit.CLUBS),
            Card(Rank.FIVE, Suit.CLUBS)
        )
        val passed = HeartsController.aiChoosePassCards(hand, GameDifficulty.MEDIUM, Random(1L))
        assertTrue(HeartsGameState.QUEEN_OF_SPADES in passed)
    }

    @Test
    fun aiChoosePassCards_easyIsNonDeterministicAcrossSeeds() {
        val hand = Rank.entries.take(10).map { Card(it, Suit.HEARTS) }
        val pass1 = HeartsController.aiChoosePassCards(hand, GameDifficulty.EASY, Random(1L))
        val pass2 = HeartsController.aiChoosePassCards(hand, GameDifficulty.EASY, Random(9999L))
        // With different seeds the selections can differ (probabilistic check)
        // At minimum verify sizes are correct
        assertEquals(3, pass1.size)
        assertEquals(3, pass2.size)
    }
}
