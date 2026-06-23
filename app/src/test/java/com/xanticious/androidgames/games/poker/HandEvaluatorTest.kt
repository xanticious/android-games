package com.xanticious.androidgames.games.poker

import com.xanticious.androidgames.controller.games.poker.HandCategory
import com.xanticious.androidgames.controller.games.poker.HandEvaluator
import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.Rank
import com.xanticious.androidgames.model.games.cards.Suit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [HandEvaluator].
 *
 * Covers every hand category, the wheel straight edge case, and all key
 * tie-breaking rules (pair rank, kicker, two-pair kicker, etc.).
 */
class HandEvaluatorTest {

    // ---- helpers ----

    private fun card(rank: Rank, suit: Suit) = Card(rank, suit)
    private fun c(rank: Rank, suit: Suit) = card(rank, suit)

    private fun category(hand: List<Card>) = HandEvaluator.evaluate(hand).category

    // ---- Royal Flush ----

    @Test
    fun evaluate_royalFlush_returnsRoyalFlush() {
        val hand = listOf(
            c(Rank.ACE, Suit.SPADES), c(Rank.KING, Suit.SPADES),
            c(Rank.QUEEN, Suit.SPADES), c(Rank.JACK, Suit.SPADES),
            c(Rank.TEN, Suit.SPADES)
        )
        assertEquals(HandCategory.ROYAL_FLUSH, category(hand))
    }

    // ---- Straight Flush ----

    @Test
    fun evaluate_straightFlush_returnsStraightFlush() {
        val hand = listOf(
            c(Rank.NINE, Suit.HEARTS), c(Rank.EIGHT, Suit.HEARTS),
            c(Rank.SEVEN, Suit.HEARTS), c(Rank.SIX, Suit.HEARTS),
            c(Rank.FIVE, Suit.HEARTS)
        )
        assertEquals(HandCategory.STRAIGHT_FLUSH, category(hand))
    }

    @Test
    fun evaluate_straightFlushWheelAceLow_returnsStraightFlush() {
        // A-2-3-4-5 all same suit (wheel straight flush, not royal).
        val hand = listOf(
            c(Rank.ACE, Suit.CLUBS), c(Rank.TWO, Suit.CLUBS),
            c(Rank.THREE, Suit.CLUBS), c(Rank.FOUR, Suit.CLUBS),
            c(Rank.FIVE, Suit.CLUBS)
        )
        assertEquals(HandCategory.STRAIGHT_FLUSH, category(hand))
    }

    // ---- Four of a Kind ----

    @Test
    fun evaluate_fourOfAKind_returnsFourOfAKind() {
        val hand = listOf(
            c(Rank.ACE, Suit.SPADES), c(Rank.ACE, Suit.HEARTS),
            c(Rank.ACE, Suit.DIAMONDS), c(Rank.ACE, Suit.CLUBS),
            c(Rank.KING, Suit.SPADES)
        )
        assertEquals(HandCategory.FOUR_OF_A_KIND, category(hand))
    }

    // ---- Full House ----

    @Test
    fun evaluate_fullHouse_returnsFullHouse() {
        val hand = listOf(
            c(Rank.KING, Suit.SPADES), c(Rank.KING, Suit.HEARTS),
            c(Rank.KING, Suit.DIAMONDS), c(Rank.QUEEN, Suit.CLUBS),
            c(Rank.QUEEN, Suit.SPADES)
        )
        assertEquals(HandCategory.FULL_HOUSE, category(hand))
    }

    // ---- Flush ----

    @Test
    fun evaluate_flush_returnsFlush() {
        val hand = listOf(
            c(Rank.ACE, Suit.DIAMONDS), c(Rank.TEN, Suit.DIAMONDS),
            c(Rank.SEVEN, Suit.DIAMONDS), c(Rank.FOUR, Suit.DIAMONDS),
            c(Rank.TWO, Suit.DIAMONDS)
        )
        assertEquals(HandCategory.FLUSH, category(hand))
    }

    @Test
    fun evaluate_flushBeatsNormalStraight() {
        val flush = HandEvaluator.evaluate(
            listOf(
                c(Rank.ACE, Suit.DIAMONDS), c(Rank.TEN, Suit.DIAMONDS),
                c(Rank.SEVEN, Suit.DIAMONDS), c(Rank.FOUR, Suit.DIAMONDS),
                c(Rank.TWO, Suit.DIAMONDS)
            )
        )
        val straight = HandEvaluator.evaluate(
            listOf(
                c(Rank.NINE, Suit.SPADES), c(Rank.EIGHT, Suit.HEARTS),
                c(Rank.SEVEN, Suit.DIAMONDS), c(Rank.SIX, Suit.CLUBS),
                c(Rank.FIVE, Suit.SPADES)
            )
        )
        assertTrue(flush > straight)
    }

    // ---- Straight ----

    @Test
    fun evaluate_straightAceHigh_returnsStraight() {
        val hand = listOf(
            c(Rank.ACE, Suit.SPADES), c(Rank.KING, Suit.HEARTS),
            c(Rank.QUEEN, Suit.DIAMONDS), c(Rank.JACK, Suit.CLUBS),
            c(Rank.TEN, Suit.SPADES)
        )
        // This has Ace = 14, so it's a normal straight — but NOT a royal flush (mixed suits).
        assertEquals(HandCategory.STRAIGHT, category(hand))
    }

    @Test
    fun evaluate_straightWheelAceLow_returnsStraight() {
        val hand = listOf(
            c(Rank.ACE, Suit.SPADES), c(Rank.TWO, Suit.HEARTS),
            c(Rank.THREE, Suit.DIAMONDS), c(Rank.FOUR, Suit.CLUBS),
            c(Rank.FIVE, Suit.SPADES)
        )
        assertEquals(HandCategory.STRAIGHT, category(hand))
    }

    @Test
    fun evaluate_wheelStraightHighCardIsFive() {
        val wheel = HandEvaluator.evaluate(
            listOf(
                c(Rank.ACE, Suit.SPADES), c(Rank.TWO, Suit.HEARTS),
                c(Rank.THREE, Suit.DIAMONDS), c(Rank.FOUR, Suit.CLUBS),
                c(Rank.FIVE, Suit.SPADES)
            )
        )
        // tiebreaker[0] should be 5, not 14.
        assertEquals(5, wheel.tiebreakers[0])
    }

    @Test
    fun evaluate_wheelStraightIsLowerThanSixHighStraight() {
        val wheel = HandEvaluator.evaluate(
            listOf(
                c(Rank.ACE, Suit.SPADES), c(Rank.TWO, Suit.HEARTS),
                c(Rank.THREE, Suit.DIAMONDS), c(Rank.FOUR, Suit.CLUBS),
                c(Rank.FIVE, Suit.SPADES)
            )
        )
        val sixHigh = HandEvaluator.evaluate(
            listOf(
                c(Rank.SIX, Suit.SPADES), c(Rank.TWO, Suit.HEARTS),
                c(Rank.THREE, Suit.DIAMONDS), c(Rank.FOUR, Suit.CLUBS),
                c(Rank.FIVE, Suit.SPADES)
            )
        )
        assertTrue(wheel < sixHigh)
    }

    // ---- Three of a Kind ----

    @Test
    fun evaluate_threeOfAKind_returnsThreeOfAKind() {
        val hand = listOf(
            c(Rank.QUEEN, Suit.SPADES), c(Rank.QUEEN, Suit.HEARTS),
            c(Rank.QUEEN, Suit.DIAMONDS), c(Rank.JACK, Suit.CLUBS),
            c(Rank.NINE, Suit.SPADES)
        )
        assertEquals(HandCategory.THREE_OF_A_KIND, category(hand))
    }

    // ---- Two Pair ----

    @Test
    fun evaluate_twoPair_returnsTwoPair() {
        val hand = listOf(
            c(Rank.ACE, Suit.SPADES), c(Rank.ACE, Suit.HEARTS),
            c(Rank.KING, Suit.DIAMONDS), c(Rank.KING, Suit.CLUBS),
            c(Rank.JACK, Suit.SPADES)
        )
        assertEquals(HandCategory.TWO_PAIR, category(hand))
    }

    @Test
    fun evaluate_twoPair_sameHighPair_kickerBreaksTie() {
        // Both have A-A and K-K, but different kickers.
        val handWithAceKicker = HandEvaluator.evaluate(
            listOf(
                c(Rank.ACE, Suit.SPADES), c(Rank.ACE, Suit.HEARTS),
                c(Rank.KING, Suit.DIAMONDS), c(Rank.KING, Suit.CLUBS),
                c(Rank.JACK, Suit.SPADES)   // J kicker
            )
        )
        val handWithTwoKicker = HandEvaluator.evaluate(
            listOf(
                c(Rank.ACE, Suit.DIAMONDS), c(Rank.ACE, Suit.CLUBS),
                c(Rank.KING, Suit.SPADES), c(Rank.KING, Suit.HEARTS),
                c(Rank.TWO, Suit.SPADES)    // 2 kicker
            )
        )
        assertTrue(handWithAceKicker > handWithTwoKicker)
    }

    @Test
    fun evaluate_twoPair_higherTopPairWins() {
        val acesAndKings = HandEvaluator.evaluate(
            listOf(
                c(Rank.ACE, Suit.SPADES), c(Rank.ACE, Suit.HEARTS),
                c(Rank.KING, Suit.DIAMONDS), c(Rank.KING, Suit.CLUBS),
                c(Rank.TWO, Suit.SPADES)
            )
        )
        val queensAndJacks = HandEvaluator.evaluate(
            listOf(
                c(Rank.QUEEN, Suit.SPADES), c(Rank.QUEEN, Suit.HEARTS),
                c(Rank.JACK, Suit.DIAMONDS), c(Rank.JACK, Suit.CLUBS),
                c(Rank.TWO, Suit.HEARTS)
            )
        )
        assertTrue(acesAndKings > queensAndJacks)
    }

    // ---- One Pair ----

    @Test
    fun evaluate_onePair_returnsOnePair() {
        val hand = listOf(
            c(Rank.JACK, Suit.SPADES), c(Rank.JACK, Suit.HEARTS),
            c(Rank.ACE, Suit.DIAMONDS), c(Rank.NINE, Suit.CLUBS),
            c(Rank.THREE, Suit.SPADES)
        )
        assertEquals(HandCategory.ONE_PAIR, category(hand))
    }

    @Test
    fun evaluate_onePair_kickerBreaksTie() {
        val pairJacksAceKicker = HandEvaluator.evaluate(
            listOf(
                c(Rank.JACK, Suit.SPADES), c(Rank.JACK, Suit.HEARTS),
                c(Rank.ACE, Suit.DIAMONDS), c(Rank.NINE, Suit.CLUBS),
                c(Rank.THREE, Suit.SPADES)
            )
        )
        val pairJacksTwoKicker = HandEvaluator.evaluate(
            listOf(
                c(Rank.JACK, Suit.DIAMONDS), c(Rank.JACK, Suit.CLUBS),
                c(Rank.TWO, Suit.SPADES), c(Rank.NINE, Suit.HEARTS),
                c(Rank.THREE, Suit.DIAMONDS)
            )
        )
        assertTrue(pairJacksAceKicker > pairJacksTwoKicker)
    }

    // ---- High Card ----

    @Test
    fun evaluate_highCard_returnsHighCard() {
        val hand = listOf(
            c(Rank.ACE, Suit.SPADES), c(Rank.TEN, Suit.HEARTS),
            c(Rank.SEVEN, Suit.DIAMONDS), c(Rank.FOUR, Suit.CLUBS),
            c(Rank.TWO, Suit.SPADES)
        )
        assertEquals(HandCategory.HIGH_CARD, category(hand))
    }

    @Test
    fun evaluate_highCard_highestCardBreaksTie() {
        val aceHigh = HandEvaluator.evaluate(
            listOf(
                c(Rank.ACE, Suit.SPADES), c(Rank.TEN, Suit.HEARTS),
                c(Rank.SEVEN, Suit.DIAMONDS), c(Rank.FOUR, Suit.CLUBS),
                c(Rank.TWO, Suit.SPADES)
            )
        )
        val kingHigh = HandEvaluator.evaluate(
            listOf(
                c(Rank.KING, Suit.SPADES), c(Rank.TEN, Suit.HEARTS),
                c(Rank.SEVEN, Suit.DIAMONDS), c(Rank.FOUR, Suit.CLUBS),
                c(Rank.TWO, Suit.HEARTS)
            )
        )
        assertTrue(aceHigh > kingHigh)
    }

    // ---- findWinners ----

    @Test
    fun findWinners_pairBeatsHighCard_pairPlayerWins() {
        val hands = mapOf(
            0 to listOf(
                c(Rank.ACE, Suit.SPADES), c(Rank.ACE, Suit.HEARTS),
                c(Rank.KING, Suit.DIAMONDS), c(Rank.QUEEN, Suit.CLUBS),
                c(Rank.JACK, Suit.SPADES)
            ),
            1 to listOf(
                c(Rank.TEN, Suit.SPADES), c(Rank.NINE, Suit.HEARTS),
                c(Rank.EIGHT, Suit.DIAMONDS), c(Rank.SIX, Suit.CLUBS),
                c(Rank.FOUR, Suit.SPADES)
            )
        )
        assertEquals(listOf(0), HandEvaluator.findWinners(hands))
    }

    @Test
    fun findWinners_identicalFlushes_returnsBothPlayers() {
        // Two players with identical relative hand strength get a split.
        val hand = listOf(
            c(Rank.ACE, Suit.SPADES), c(Rank.TEN, Suit.SPADES),
            c(Rank.SEVEN, Suit.SPADES), c(Rank.FOUR, Suit.SPADES),
            c(Rank.TWO, Suit.SPADES)
        )
        // Technically impossible in real poker (same cards), but we test the evaluator.
        val hands = mapOf(0 to hand, 1 to hand)
        val winners = HandEvaluator.findWinners(hands)
        assertEquals(2, winners.size)
        assertTrue(0 in winners && 1 in winners)
    }

    @Test
    fun findWinners_fullHouseBeatsFlush_fullHouseWins() {
        val fullHouse = listOf(
            c(Rank.KING, Suit.SPADES), c(Rank.KING, Suit.HEARTS),
            c(Rank.KING, Suit.DIAMONDS), c(Rank.QUEEN, Suit.CLUBS),
            c(Rank.QUEEN, Suit.SPADES)
        )
        val flush = listOf(
            c(Rank.ACE, Suit.CLUBS), c(Rank.TEN, Suit.CLUBS),
            c(Rank.SEVEN, Suit.CLUBS), c(Rank.FOUR, Suit.CLUBS),
            c(Rank.TWO, Suit.CLUBS)
        )
        val hands = mapOf(0 to fullHouse, 1 to flush)
        assertEquals(listOf(0), HandEvaluator.findWinners(hands))
    }

    @Test
    fun findWinners_straightFlushBeatsFourOfAKind_straightFlushWins() {
        val sf = listOf(
            c(Rank.NINE, Suit.HEARTS), c(Rank.EIGHT, Suit.HEARTS),
            c(Rank.SEVEN, Suit.HEARTS), c(Rank.SIX, Suit.HEARTS),
            c(Rank.FIVE, Suit.HEARTS)
        )
        val quads = listOf(
            c(Rank.ACE, Suit.SPADES), c(Rank.ACE, Suit.HEARTS),
            c(Rank.ACE, Suit.DIAMONDS), c(Rank.ACE, Suit.CLUBS),
            c(Rank.KING, Suit.SPADES)
        )
        val hands = mapOf(0 to sf, 1 to quads)
        assertEquals(listOf(0), HandEvaluator.findWinners(hands))
    }

    @Test
    fun findWinners_emptyMap_returnsEmptyList() {
        assertTrue(HandEvaluator.findWinners(emptyMap()).isEmpty())
    }

    // ---- Category ordering ----

    @Test
    fun handCategories_areOrderedCorrectly() {
        assertTrue(HandCategory.HIGH_CARD.rank < HandCategory.ONE_PAIR.rank)
        assertTrue(HandCategory.ONE_PAIR.rank < HandCategory.TWO_PAIR.rank)
        assertTrue(HandCategory.TWO_PAIR.rank < HandCategory.THREE_OF_A_KIND.rank)
        assertTrue(HandCategory.THREE_OF_A_KIND.rank < HandCategory.STRAIGHT.rank)
        assertTrue(HandCategory.STRAIGHT.rank < HandCategory.FLUSH.rank)
        assertTrue(HandCategory.FLUSH.rank < HandCategory.FULL_HOUSE.rank)
        assertTrue(HandCategory.FULL_HOUSE.rank < HandCategory.FOUR_OF_A_KIND.rank)
        assertTrue(HandCategory.FOUR_OF_A_KIND.rank < HandCategory.STRAIGHT_FLUSH.rank)
        assertTrue(HandCategory.STRAIGHT_FLUSH.rank < HandCategory.ROYAL_FLUSH.rank)
    }

    // ---- Straight edge cases ----

    @Test
    fun evaluate_aceLowAndAceHighStraight_aceHighIsHigher() {
        val aceLow = HandEvaluator.evaluate(
            listOf(
                c(Rank.ACE, Suit.SPADES), c(Rank.TWO, Suit.HEARTS),
                c(Rank.THREE, Suit.DIAMONDS), c(Rank.FOUR, Suit.CLUBS),
                c(Rank.FIVE, Suit.SPADES)
            )
        )
        val aceHigh = HandEvaluator.evaluate(
            listOf(
                c(Rank.ACE, Suit.HEARTS), c(Rank.KING, Suit.SPADES),
                c(Rank.QUEEN, Suit.DIAMONDS), c(Rank.JACK, Suit.CLUBS),
                c(Rank.TEN, Suit.SPADES)
            )
        )
        // Both are STRAIGHT category; ace-high straight (high card 14) beats wheel (high card 5).
        assertTrue(aceHigh > aceLow)
    }

    // ---- displayName ----

    @Test
    fun displayName_royalFlush_returnsCorrectString() {
        val hand = listOf(
            c(Rank.ACE, Suit.SPADES), c(Rank.KING, Suit.SPADES),
            c(Rank.QUEEN, Suit.SPADES), c(Rank.JACK, Suit.SPADES),
            c(Rank.TEN, Suit.SPADES)
        )
        assertEquals("Royal Flush", HandEvaluator.displayName(hand))
    }
}
