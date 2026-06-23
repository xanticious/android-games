package com.xanticious.androidgames.games.cribbage

import com.xanticious.androidgames.controller.games.cribbage.CribbageController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.Rank
import com.xanticious.androidgames.model.games.cards.Suit
import com.xanticious.androidgames.model.games.cribbage.CribbagePhase
import com.xanticious.androidgames.model.games.cribbage.CribbagePlayer
import com.xanticious.androidgames.model.games.cribbage.CribbageState
import com.xanticious.androidgames.model.games.cribbage.PlayedCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.random.Random

/**
 * Unit tests for CribbageController covering show scoring, pegging scoring,
 * His Heels, and AI discard determinism.
 */
class CribbageControllerTest {

    // -------------------------------------------------------------------------
    // Card-value helpers
    // -------------------------------------------------------------------------

    @Test
    fun cardValue_ace_isOne() {
        assertEquals(1, CribbageController.cardValue(Card(Rank.ACE, Suit.SPADES)))
    }

    @Test
    fun cardValue_ten_isTen() {
        assertEquals(10, CribbageController.cardValue(Card(Rank.TEN, Suit.CLUBS)))
    }

    @Test
    fun cardValue_jack_isTen() {
        assertEquals(10, CribbageController.cardValue(Card(Rank.JACK, Suit.HEARTS)))
    }

    @Test
    fun cardValue_queen_isTen() {
        assertEquals(10, CribbageController.cardValue(Card(Rank.QUEEN, Suit.DIAMONDS)))
    }

    @Test
    fun cardValue_king_isTen() {
        assertEquals(10, CribbageController.cardValue(Card(Rank.KING, Suit.SPADES)))
    }

    @Test
    fun cardValue_five_isFive() {
        assertEquals(5, CribbageController.cardValue(Card(Rank.FIVE, Suit.CLUBS)))
    }

    // -------------------------------------------------------------------------
    // Show scoring — Fifteens
    // -------------------------------------------------------------------------

    @Test
    fun scoreShow_singleFifteen_scoresTwoPoints() {
        // 7+8 = 15
        val hand = listOf(
            Card(Rank.SEVEN, Suit.CLUBS),
            Card(Rank.EIGHT, Suit.DIAMONDS),
            Card(Rank.ACE, Suit.SPADES),
            Card(Rank.TWO, Suit.HEARTS)
        )
        val starter = Card(Rank.THREE, Suit.CLUBS)
        val result = CribbageController.scoreShow(hand, starter, isInCrib = false)
        assertTrue("Should contain a fifteen", result.lines.any { it.startsWith("Fifteens:") })
        assertTrue("Should score at least 2", result.total >= 2)
    }

    @Test
    fun fifteenCombinations_fiveNines_countsCorrectly() {
        // 5+T/J/Q/K is classic two fifteens each
        val cards = listOf(
            Card(Rank.FIVE, Suit.CLUBS),
            Card(Rank.TEN, Suit.DIAMONDS),
            Card(Rank.JACK, Suit.HEARTS),
            Card(Rank.QUEEN, Suit.SPADES),
            Card(Rank.KING, Suit.CLUBS)
        )
        // Each of the 4 ten-cards pairs with the 5 = 4 fifteens
        assertEquals(4, CribbageController.fifteenCombinations(cards))
    }

    @Test
    fun scoreShow_classic29Hand_scores29() {
        // The perfect cribbage hand: J5 + three 5s, starter = 5 matching Jack's suit
        val hand = listOf(
            Card(Rank.JACK, Suit.SPADES),
            Card(Rank.FIVE, Suit.CLUBS),
            Card(Rank.FIVE, Suit.DIAMONDS),
            Card(Rank.FIVE, Suit.HEARTS)
        )
        val starter = Card(Rank.FIVE, Suit.SPADES)
        val result = CribbageController.scoreShow(hand, starter, isInCrib = false)
        assertEquals(29, result.total)
    }

    // -------------------------------------------------------------------------
    // Show scoring — Pairs
    // -------------------------------------------------------------------------

    @Test
    fun scoreShow_onePair_scoresTwoPoints() {
        val hand = listOf(
            Card(Rank.KING, Suit.CLUBS),
            Card(Rank.KING, Suit.DIAMONDS),
            Card(Rank.TWO, Suit.HEARTS),
            Card(Rank.FOUR, Suit.SPADES)
        )
        val starter = Card(Rank.NINE, Suit.CLUBS)
        val result = CribbageController.scoreShow(hand, starter, isInCrib = false)
        assertTrue("Should contain a pair line", result.lines.any { it.contains("Pair") })
        assertTrue("Should score at least 2", result.total >= 2)
    }

    @Test
    fun scoreShow_threeOfAKind_scoresSixPoints() {
        val hand = listOf(
            Card(Rank.SIX, Suit.CLUBS),
            Card(Rank.SIX, Suit.DIAMONDS),
            Card(Rank.SIX, Suit.HEARTS),
            Card(Rank.TWO, Suit.SPADES)
        )
        val starter = Card(Rank.ACE, Suit.CLUBS)
        val result = CribbageController.scoreShow(hand, starter, isInCrib = false)
        assertTrue("Should contain pair royal", result.lines.any { it.lowercase().contains("pair royal") })
        assertTrue("Pair royal = 6 pts in total", result.total >= 6)
    }

    @Test
    fun scoreShow_fourOfAKind_scoresTwelvePoints() {
        val hand = listOf(
            Card(Rank.SEVEN, Suit.CLUBS),
            Card(Rank.SEVEN, Suit.DIAMONDS),
            Card(Rank.SEVEN, Suit.HEARTS),
            Card(Rank.SEVEN, Suit.SPADES)
        )
        val starter = Card(Rank.ACE, Suit.CLUBS)
        val result = CribbageController.scoreShow(hand, starter, isInCrib = false)
        assertTrue("Should contain double pair royal", result.lines.any { it.contains("Double pair royal") })
        assertTrue("Double pair royal = 12", result.total >= 12)
    }

    // -------------------------------------------------------------------------
    // Show scoring — Runs
    // -------------------------------------------------------------------------

    @Test
    fun scoreShow_runOfThree_scoresThreePoints() {
        val hand = listOf(
            Card(Rank.ACE, Suit.CLUBS),
            Card(Rank.TWO, Suit.DIAMONDS),
            Card(Rank.THREE, Suit.HEARTS),
            Card(Rank.KING, Suit.SPADES)
        )
        val starter = Card(Rank.NINE, Suit.CLUBS)
        val result = CribbageController.scoreShow(hand, starter, isInCrib = false)
        assertTrue("Should have run line", result.lines.any { it.startsWith("Run") })
        val runPoints = result.lines.filter { it.startsWith("Run") }
            .sumOf { line -> line.substringAfterLast("= ").trim().toIntOrNull() ?: 0 }
        assertTrue("Run of 3 = 3 pts", runPoints >= 3)
    }

    @Test
    fun scoreShow_doubleRun_scoresSixPoints() {
        // A-2-3-3 + any starter: double run of 3 = 6
        val hand = listOf(
            Card(Rank.ACE, Suit.CLUBS),
            Card(Rank.TWO, Suit.DIAMONDS),
            Card(Rank.THREE, Suit.HEARTS),
            Card(Rank.THREE, Suit.SPADES)
        )
        val starter = Card(Rank.KING, Suit.CLUBS)
        val result = CribbageController.scoreShow(hand, starter, isInCrib = false)
        // double run of 3 = 6 pts for runs + 2 for pair = 8 total
        assertTrue("Run score should be >= 6", result.total >= 6)
    }

    @Test
    fun scoreShow_runOfFive_scoresFivePoints() {
        val hand = listOf(
            Card(Rank.ACE, Suit.CLUBS),
            Card(Rank.TWO, Suit.DIAMONDS),
            Card(Rank.THREE, Suit.HEARTS),
            Card(Rank.FOUR, Suit.SPADES)
        )
        val starter = Card(Rank.FIVE, Suit.CLUBS)
        val result = CribbageController.scoreShow(hand, starter, isInCrib = false)
        assertTrue("Run(s) of 5 in lines", result.lines.any { it.contains("5") && it.startsWith("Run") })
    }

    // -------------------------------------------------------------------------
    // Show scoring — Flush
    // -------------------------------------------------------------------------

    @Test
    fun scoreFlush_handFlush_scoresFour() {
        val hand = listOf(
            Card(Rank.ACE, Suit.HEARTS),
            Card(Rank.FOUR, Suit.HEARTS),
            Card(Rank.SEVEN, Suit.HEARTS),
            Card(Rank.NINE, Suit.HEARTS)
        )
        val starter = Card(Rank.KING, Suit.SPADES)
        assertEquals(4, CribbageController.scoreFlush(hand, starter, isInCrib = false))
    }

    @Test
    fun scoreFlush_fiveCardFlush_scoresFive() {
        val hand = listOf(
            Card(Rank.ACE, Suit.HEARTS),
            Card(Rank.FOUR, Suit.HEARTS),
            Card(Rank.SEVEN, Suit.HEARTS),
            Card(Rank.NINE, Suit.HEARTS)
        )
        val starter = Card(Rank.KING, Suit.HEARTS)
        assertEquals(5, CribbageController.scoreFlush(hand, starter, isInCrib = false))
    }

    @Test
    fun scoreFlush_cribFlushFour_scoresZero() {
        val hand = listOf(
            Card(Rank.ACE, Suit.HEARTS),
            Card(Rank.FOUR, Suit.HEARTS),
            Card(Rank.SEVEN, Suit.HEARTS),
            Card(Rank.NINE, Suit.HEARTS)
        )
        val starter = Card(Rank.KING, Suit.SPADES) // different suit
        assertEquals(0, CribbageController.scoreFlush(hand, starter, isInCrib = true))
    }

    @Test
    fun scoreFlush_cribFlushFive_scoresFive() {
        val hand = listOf(
            Card(Rank.ACE, Suit.CLUBS),
            Card(Rank.FOUR, Suit.CLUBS),
            Card(Rank.SEVEN, Suit.CLUBS),
            Card(Rank.NINE, Suit.CLUBS)
        )
        val starter = Card(Rank.KING, Suit.CLUBS)
        assertEquals(5, CribbageController.scoreFlush(hand, starter, isInCrib = true))
    }

    @Test
    fun scoreFlush_mixedSuits_scoresZero() {
        val hand = listOf(
            Card(Rank.ACE, Suit.HEARTS),
            Card(Rank.FOUR, Suit.CLUBS),
            Card(Rank.SEVEN, Suit.HEARTS),
            Card(Rank.NINE, Suit.HEARTS)
        )
        val starter = Card(Rank.KING, Suit.HEARTS)
        assertEquals(0, CribbageController.scoreFlush(hand, starter, isInCrib = false))
    }

    // -------------------------------------------------------------------------
    // Show scoring — His Nobs
    // -------------------------------------------------------------------------

    @Test
    fun scoreShow_hisNobs_scoresOnePoint() {
        val hand = listOf(
            Card(Rank.JACK, Suit.HEARTS), // nobs — matches starter suit
            Card(Rank.TWO, Suit.CLUBS),
            Card(Rank.THREE, Suit.SPADES),
            Card(Rank.KING, Suit.DIAMONDS)
        )
        val starter = Card(Rank.ACE, Suit.HEARTS)
        val result = CribbageController.scoreShow(hand, starter, isInCrib = false)
        assertTrue("Should have Nobs line", result.lines.any { it.contains("Nobs") })
        assertTrue("Nobs adds 1 pt", result.total >= 1)
    }

    @Test
    fun scoreShow_jackWrongSuit_noNobs() {
        val hand = listOf(
            Card(Rank.JACK, Suit.SPADES), // does NOT match starter suit
            Card(Rank.TWO, Suit.CLUBS),
            Card(Rank.THREE, Suit.CLUBS),
            Card(Rank.KING, Suit.DIAMONDS)
        )
        val starter = Card(Rank.ACE, Suit.HEARTS)
        val result = CribbageController.scoreShow(hand, starter, isInCrib = false)
        assertFalse("Should NOT have Nobs", result.lines.any { it.contains("Nobs") })
    }

    // -------------------------------------------------------------------------
    // His Heels
    // -------------------------------------------------------------------------

    @Test
    fun cut_jackStarter_dealerScoresTwoPoints() {
        val state = CribbageState(
            dealer = CribbagePlayer.HUMAN,
            humanHand = listOf(
                Card(Rank.TWO, Suit.CLUBS), Card(Rank.THREE, Suit.CLUBS),
                Card(Rank.FOUR, Suit.CLUBS), Card(Rank.FIVE, Suit.CLUBS)
            ),
            aiHand = listOf(
                Card(Rank.SIX, Suit.CLUBS), Card(Rank.SEVEN, Suit.CLUBS),
                Card(Rank.EIGHT, Suit.CLUBS), Card(Rank.NINE, Suit.CLUBS)
            ),
            crib = listOf(
                Card(Rank.TEN, Suit.CLUBS), Card(Rank.ACE, Suit.SPADES),
                Card(Rank.TWO, Suit.SPADES), Card(Rank.THREE, Suit.SPADES)
            ),
            phase = CribbagePhase.CUTTING
        )
        // Find a seed that cuts a Jack
        var seedToTest = 0L
        while (true) {
            val testState = CribbageController.cut(state, Random(seedToTest))
            if (testState.starter?.rank == Rank.JACK) {
                // Verify dealer scored 2
                assertEquals("Human (dealer) should score 2 for His Heels", 2, testState.humanScore)
                break
            }
            seedToTest++
            if (seedToTest > 1000) break
        }
    }

    @Test
    fun cut_nonJackStarter_noHeelsBonus() {
        val state = CribbageState(
            dealer = CribbagePlayer.HUMAN,
            humanHand = listOf(
                Card(Rank.TWO, Suit.CLUBS), Card(Rank.THREE, Suit.CLUBS),
                Card(Rank.FOUR, Suit.CLUBS), Card(Rank.FIVE, Suit.CLUBS)
            ),
            aiHand = listOf(
                Card(Rank.SIX, Suit.CLUBS), Card(Rank.SEVEN, Suit.CLUBS),
                Card(Rank.EIGHT, Suit.CLUBS), Card(Rank.NINE, Suit.CLUBS)
            ),
            crib = listOf(
                Card(Rank.TEN, Suit.CLUBS), Card(Rank.ACE, Suit.SPADES),
                Card(Rank.TWO, Suit.SPADES), Card(Rank.THREE, Suit.SPADES)
            ),
            phase = CribbagePhase.CUTTING
        )
        // Find a seed that does NOT cut a Jack
        var seedToTest = 0L
        while (true) {
            val testState = CribbageController.cut(state, Random(seedToTest))
            if (testState.starter?.rank != Rank.JACK) {
                assertEquals("No Heels — human score should be 0", 0, testState.humanScore)
                break
            }
            seedToTest++
        }
    }

    // -------------------------------------------------------------------------
    // Pegging scoring
    // -------------------------------------------------------------------------

    @Test
    fun scorePeggingPlay_fifteen_scoresTwoPoints() {
        val pile = listOf(
            PlayedCard(Card(Rank.TEN, Suit.CLUBS), CribbagePlayer.HUMAN, 10),
            PlayedCard(Card(Rank.FIVE, Suit.HEARTS), CribbagePlayer.AI, 15)
        )
        val scores = CribbageController.scorePeggingPlay(pile, 15)
        assertTrue("Should contain Fifteen", scores.any { it.description == "Fifteen" })
        assertEquals(2, scores.filter { it.description == "Fifteen" }.sumOf { it.points })
    }

    @Test
    fun scorePeggingPlay_thirtyOne_scoresTwoPoints() {
        val pile = listOf(
            PlayedCard(Card(Rank.TEN, Suit.CLUBS), CribbagePlayer.HUMAN, 10),
            PlayedCard(Card(Rank.TEN, Suit.DIAMONDS), CribbagePlayer.AI, 20),
            PlayedCard(Card(Rank.ACE, Suit.SPADES), CribbagePlayer.HUMAN, 21),
            PlayedCard(Card(Rank.TEN, Suit.HEARTS), CribbagePlayer.AI, 31)
        )
        val scores = CribbageController.scorePeggingPlay(pile, 31)
        assertTrue("Should contain Thirty-one", scores.any { it.description == "Thirty-one" })
    }

    @Test
    fun scorePeggingPlay_pair_scoresTwoPoints() {
        val pile = listOf(
            PlayedCard(Card(Rank.SEVEN, Suit.CLUBS), CribbagePlayer.HUMAN, 7),
            PlayedCard(Card(Rank.SEVEN, Suit.DIAMONDS), CribbagePlayer.AI, 14)
        )
        val scores = CribbageController.scorePeggingPlay(pile, 14)
        assertTrue("Should contain Pair", scores.any { it.description == "Pair" })
        assertEquals(2, scores.filter { it.description == "Pair" }.sumOf { it.points })
    }

    @Test
    fun scorePeggingPlay_pairRoyal_scoresSixPoints() {
        val pile = listOf(
            PlayedCard(Card(Rank.EIGHT, Suit.CLUBS), CribbagePlayer.HUMAN, 8),
            PlayedCard(Card(Rank.EIGHT, Suit.DIAMONDS), CribbagePlayer.AI, 16),
            PlayedCard(Card(Rank.EIGHT, Suit.HEARTS), CribbagePlayer.HUMAN, 24)
        )
        val scores = CribbageController.scorePeggingPlay(pile, 24)
        assertTrue("Should contain Pair royal", scores.any { it.description == "Pair royal" })
        assertEquals(6, scores.filter { it.description == "Pair royal" }.sumOf { it.points })
    }

    @Test
    fun scorePeggingPlay_quadruplets_scoresTwelvePoints() {
        val pile = listOf(
            PlayedCard(Card(Rank.NINE, Suit.CLUBS), CribbagePlayer.HUMAN, 9),
            PlayedCard(Card(Rank.NINE, Suit.DIAMONDS), CribbagePlayer.AI, 18),
            PlayedCard(Card(Rank.NINE, Suit.HEARTS), CribbagePlayer.HUMAN, 27),
            PlayedCard(Card(Rank.NINE, Suit.SPADES), CribbagePlayer.AI, 27) // still 27+9=36 impossible, but test the scoring
        )
        // Use a count that doesn't matter for this pure scoring test
        val scores = CribbageController.scorePeggingPlay(pile, 27)
        assertTrue("Should contain Double pair royal", scores.any { it.description == "Double pair royal" })
        assertEquals(12, scores.filter { it.description == "Double pair royal" }.sumOf { it.points })
    }

    @Test
    fun scorePeggingPlay_runOfThree_scoresThreePoints() {
        val pile = listOf(
            PlayedCard(Card(Rank.THREE, Suit.CLUBS), CribbagePlayer.HUMAN, 3),
            PlayedCard(Card(Rank.ACE, Suit.DIAMONDS), CribbagePlayer.AI, 4),
            PlayedCard(Card(Rank.TWO, Suit.HEARTS), CribbagePlayer.HUMAN, 6)
        )
        val scores = CribbageController.scorePeggingPlay(pile, 6)
        assertTrue("Should contain Run of 3", scores.any { it.description == "Run of 3" })
        assertEquals(3, scores.filter { it.description == "Run of 3" }.sumOf { it.points })
    }

    @Test
    fun scorePeggingPlay_runOfFour_scoresFourPoints() {
        val pile = listOf(
            PlayedCard(Card(Rank.THREE, Suit.CLUBS), CribbagePlayer.HUMAN, 3),
            PlayedCard(Card(Rank.FOUR, Suit.DIAMONDS), CribbagePlayer.AI, 7),
            PlayedCard(Card(Rank.ACE, Suit.HEARTS), CribbagePlayer.HUMAN, 8),
            PlayedCard(Card(Rank.TWO, Suit.SPADES), CribbagePlayer.AI, 10)
        )
        val scores = CribbageController.scorePeggingPlay(pile, 10)
        assertTrue("Should contain Run of 4", scores.any { it.description == "Run of 4" })
        assertEquals(4, scores.filter { it.description == "Run of 4" }.sumOf { it.points })
    }

    @Test
    fun scorePeggingPlay_nonConsecutiveCards_noRun() {
        val pile = listOf(
            PlayedCard(Card(Rank.THREE, Suit.CLUBS), CribbagePlayer.HUMAN, 3),
            PlayedCard(Card(Rank.KING, Suit.DIAMONDS), CribbagePlayer.AI, 13),
            PlayedCard(Card(Rank.TWO, Suit.HEARTS), CribbagePlayer.HUMAN, 15)
        )
        val scores = CribbageController.scorePeggingPlay(pile, 15)
        assertFalse("Should NOT have a run", scores.any { it.description.startsWith("Run") })
    }

    @Test
    fun longestRunFromTop_pileWithPairInterrupt_noRun() {
        // If a pair interrupts the sequence, no run (pair breaks it)
        val pile = listOf(
            PlayedCard(Card(Rank.ACE, Suit.CLUBS), CribbagePlayer.HUMAN, 1),
            PlayedCard(Card(Rank.TWO, Suit.DIAMONDS), CribbagePlayer.AI, 3),
            PlayedCard(Card(Rank.TWO, Suit.HEARTS), CribbagePlayer.HUMAN, 5)
        )
        // [A,2,2] — duplicate rank, not a run
        assertEquals(0, CribbageController.longestRunFromTop(pile))
    }

    // -------------------------------------------------------------------------
    // AI discard determinism
    // -------------------------------------------------------------------------

    @Test
    fun aiChooseDiscard_fixedSeed_isDeterministic() {
        val hand = listOf(
            Card(Rank.ACE, Suit.CLUBS),
            Card(Rank.FIVE, Suit.DIAMONDS),
            Card(Rank.FIVE, Suit.HEARTS),
            Card(Rank.TEN, Suit.SPADES),
            Card(Rank.JACK, Suit.CLUBS),
            Card(Rank.KING, Suit.HEARTS)
        )
        val seed = 12345L
        val (d1, k1) = CribbageController.aiChooseDiscard(hand, GameDifficulty.HARD, true, Random(seed))
        val (d2, k2) = CribbageController.aiChooseDiscard(hand, GameDifficulty.HARD, true, Random(seed))
        assertEquals("Discards should be same with same seed", d1, d2)
        assertEquals("Keeps should be same with same seed", k1, k2)
    }

    @Test
    fun aiChooseDiscard_discardsExactlyTwoCards() {
        val hand = listOf(
            Card(Rank.TWO, Suit.CLUBS),
            Card(Rank.FIVE, Suit.DIAMONDS),
            Card(Rank.SEVEN, Suit.HEARTS),
            Card(Rank.NINE, Suit.SPADES),
            Card(Rank.JACK, Suit.CLUBS),
            Card(Rank.KING, Suit.HEARTS)
        )
        val (discard, keep) = CribbageController.aiChooseDiscard(hand, GameDifficulty.MEDIUM, false, Random(42))
        assertEquals("Should discard exactly 2", 2, discard.size)
        assertEquals("Should keep exactly 4", 4, keep.size)
    }

    @Test
    fun aiChooseDiscard_hardMode_keepsPairOverRandom() {
        // With a pair in hand, Hard AI should keep it
        val hand = listOf(
            Card(Rank.FIVE, Suit.CLUBS),
            Card(Rank.FIVE, Suit.DIAMONDS),
            Card(Rank.TWO, Suit.HEARTS),
            Card(Rank.THREE, Suit.SPADES),
            Card(Rank.KING, Suit.CLUBS),
            Card(Rank.ACE, Suit.HEARTS)
        )
        val (discard, keep) = CribbageController.aiChooseDiscard(hand, GameDifficulty.HARD, false, Random(99))
        val pairInKeep = keep.count { it.rank == Rank.FIVE }
        assertEquals("Should keep the pair of fives", 2, pairInKeep)
    }

    // -------------------------------------------------------------------------
    // Deal
    // -------------------------------------------------------------------------

    @Test
    fun deal_dealsSixCardsToEachPlayer() {
        val state = CribbageController.initialState()
        val dealt = CribbageController.deal(state, 42L)
        assertEquals("Human should have 6 cards", 6, dealt.humanHand.size)
        assertEquals("AI should have 6 cards", 6, dealt.aiHand.size)
    }

    @Test
    fun deal_noCardDuplicates() {
        val state = CribbageController.initialState()
        val dealt = CribbageController.deal(state, 99L)
        val all = dealt.humanHand + dealt.aiHand
        assertEquals("All 12 dealt cards should be unique", 12, all.toSet().size)
    }

    @Test
    fun deal_phaseBecomesDiscarding() {
        val state = CribbageController.initialState()
        val dealt = CribbageController.deal(state, 1L)
        assertEquals(CribbagePhase.DISCARDING, dealt.phase)
    }

    // -------------------------------------------------------------------------
    // Human discard
    // -------------------------------------------------------------------------

    @Test
    fun toggleHumanDiscard_selectsCard() {
        val state = CribbageController.deal(CribbageController.initialState(), 5L)
        val toggled = CribbageController.toggleHumanDiscard(state, 0)
        assertTrue("Index 0 should be selected", toggled.humanDiscardSelection.contains(0))
    }

    @Test
    fun toggleHumanDiscard_deselectsCard() {
        val state = CribbageController.deal(CribbageController.initialState(), 5L)
        val toggled = CribbageController.toggleHumanDiscard(state, 0)
        val deselected = CribbageController.toggleHumanDiscard(toggled, 0)
        assertFalse("Index 0 should be deselected", deselected.humanDiscardSelection.contains(0))
    }

    @Test
    fun toggleHumanDiscard_maxTwoSelections() {
        val state = CribbageController.deal(CribbageController.initialState(), 5L)
        val s1 = CribbageController.toggleHumanDiscard(state, 0)
        val s2 = CribbageController.toggleHumanDiscard(s1, 1)
        val s3 = CribbageController.toggleHumanDiscard(s2, 2) // should be ignored
        assertEquals("Max 2 selections", 2, s3.humanDiscardSelection.size)
    }

    @Test
    fun confirmDiscard_cribHasFourCards() {
        val dealState = CribbageController.deal(CribbageController.initialState(), 7L)
        val sel = CribbageController.toggleHumanDiscard(
            CribbageController.toggleHumanDiscard(dealState, 0), 1
        )
        val result = CribbageController.confirmDiscard(sel, GameDifficulty.MEDIUM, Random(7))
        assertEquals("Crib should have 4 cards", 4, result.crib.size)
    }

    @Test
    fun confirmDiscard_humanHandHasFourCards() {
        val dealState = CribbageController.deal(CribbageController.initialState(), 7L)
        val sel = CribbageController.toggleHumanDiscard(
            CribbageController.toggleHumanDiscard(dealState, 0), 1
        )
        val result = CribbageController.confirmDiscard(sel, GameDifficulty.MEDIUM, Random(7))
        assertEquals("Human hand should have 4 cards", 4, result.humanHand.size)
    }

    @Test
    fun confirmDiscard_phaseBecomeCutting() {
        val dealState = CribbageController.deal(CribbageController.initialState(), 7L)
        val sel = CribbageController.toggleHumanDiscard(
            CribbageController.toggleHumanDiscard(dealState, 0), 1
        )
        val result = CribbageController.confirmDiscard(sel, GameDifficulty.MEDIUM, Random(7))
        assertEquals(CribbagePhase.CUTTING, result.phase)
    }

    // -------------------------------------------------------------------------
    // Pegging gameplay
    // -------------------------------------------------------------------------

    @Test
    fun humanPlayCard_removesCardFromHand() {
        val card = Card(Rank.FIVE, Suit.CLUBS)
        val state = CribbageState(
            phase = CribbagePhase.PLAYING,
            pegTurn = CribbagePlayer.HUMAN,
            humanPlayHand = listOf(card, Card(Rank.SEVEN, Suit.DIAMONDS)),
            aiPlayHand = listOf(Card(Rank.KING, Suit.SPADES))
        )
        val next = CribbageController.humanPlayCard(state, card)
        assertFalse("Card should be removed from hand", next.humanPlayHand.contains(card))
    }

    @Test
    fun humanPlayCard_updatesCount() {
        val card = Card(Rank.EIGHT, Suit.CLUBS)
        val state = CribbageState(
            phase = CribbagePhase.PLAYING,
            pegTurn = CribbagePlayer.HUMAN,
            pegCount = 7,
            humanPlayHand = listOf(card),
            aiPlayHand = listOf(Card(Rank.TWO, Suit.HEARTS))
        )
        val next = CribbageController.humanPlayCard(state, card)
        assertEquals("Count should be 15", 15, next.pegCount)
    }

    @Test
    fun humanPlayCard_fifteen_scoresTwo() {
        val card = Card(Rank.FIVE, Suit.HEARTS)
        val state = CribbageState(
            phase = CribbagePhase.PLAYING,
            pegTurn = CribbagePlayer.HUMAN,
            pegCount = 10,
            playPile = listOf(PlayedCard(Card(Rank.TEN, Suit.CLUBS), CribbagePlayer.AI, 10)),
            humanPlayHand = listOf(card),
            aiPlayHand = listOf(Card(Rank.TWO, Suit.DIAMONDS))
        )
        val next = CribbageController.humanPlayCard(state, card)
        assertEquals("Should score 2 for fifteen", 2, next.humanScore)
    }

    @Test
    fun legalPeggingCards_filtersCardsExceeding31() {
        val state = CribbageState(
            pegCount = 28,
            humanPlayHand = listOf(
                Card(Rank.TWO, Suit.CLUBS),      // 28+2=30, legal
                Card(Rank.FIVE, Suit.DIAMONDS),   // 28+5=33, illegal
                Card(Rank.THREE, Suit.HEARTS)     // 28+3=31, legal
            )
        )
        val legal = CribbageController.legalPeggingCards(state, CribbagePlayer.HUMAN)
        assertEquals("Should have 2 legal cards", 2, legal.size)
        assertTrue("2 should be legal", legal.any { it.rank == Rank.TWO })
        assertTrue("3 should be legal", legal.any { it.rank == Rank.THREE })
        assertFalse("5 should be illegal", legal.any { it.rank == Rank.FIVE })
    }

    // -------------------------------------------------------------------------
    // Show phase transitions
    // -------------------------------------------------------------------------

    @Test
    fun scoreNonDealerHand_addsPointsToNonDealer() {
        // Non-dealer = AI (dealer = HUMAN)
        val aiHand = listOf(
            Card(Rank.FIVE, Suit.CLUBS),
            Card(Rank.TEN, Suit.DIAMONDS),
            Card(Rank.ACE, Suit.HEARTS),
            Card(Rank.TWO, Suit.SPADES)
        )
        val starter = Card(Rank.THREE, Suit.CLUBS) // A-2-3-5-T: 15(5+10) + 15(A+2+3+... nope), run A23
        val state = CribbageState(
            dealer = CribbagePlayer.HUMAN,
            aiHand = aiHand,
            humanHand = listOf(Card(Rank.KING, Suit.CLUBS), Card(Rank.KING, Suit.DIAMONDS), Card(Rank.NINE, Suit.HEARTS), Card(Rank.FOUR, Suit.SPADES)),
            starter = starter,
            phase = CribbagePhase.SHOW_NON_DEALER
        )
        val result = CribbageController.scoreNonDealerHand(state)
        assertTrue("AI (non-dealer) score should increase", result.aiScore > 0)
        assertEquals(CribbagePhase.SHOW_DEALER, result.phase)
    }

    @Test
    fun scoreDealerHand_addsPointsToDealer() {
        val humanHand = listOf(
            Card(Rank.FIVE, Suit.CLUBS),
            Card(Rank.TEN, Suit.DIAMONDS),
            Card(Rank.JACK, Suit.CLUBS),
            Card(Rank.QUEEN, Suit.SPADES)
        )
        val starter = Card(Rank.FIVE, Suit.HEARTS)
        // dealer = HUMAN
        val state = CribbageState(
            dealer = CribbagePlayer.HUMAN,
            humanHand = humanHand,
            aiHand = listOf(Card(Rank.TWO, Suit.CLUBS), Card(Rank.THREE, Suit.CLUBS), Card(Rank.FOUR, Suit.CLUBS), Card(Rank.SIX, Suit.CLUBS)),
            starter = starter,
            phase = CribbagePhase.SHOW_DEALER
        )
        val result = CribbageController.scoreDealerHand(state)
        assertTrue("Human (dealer) score should increase", result.humanScore > 0)
        assertEquals(CribbagePhase.SHOW_CRIB, result.phase)
    }

    @Test
    fun scoreCrib_swapsDealerAfterHand() {
        val crib = listOf(
            Card(Rank.ACE, Suit.CLUBS), Card(Rank.TWO, Suit.CLUBS),
            Card(Rank.KING, Suit.SPADES), Card(Rank.QUEEN, Suit.HEARTS)
        )
        val starter = Card(Rank.THREE, Suit.DIAMONDS)
        val state = CribbageState(
            dealer = CribbagePlayer.HUMAN,
            humanHand = listOf(Card(Rank.FOUR, Suit.CLUBS), Card(Rank.FIVE, Suit.CLUBS), Card(Rank.SIX, Suit.CLUBS), Card(Rank.SEVEN, Suit.CLUBS)),
            aiHand = listOf(Card(Rank.EIGHT, Suit.CLUBS), Card(Rank.NINE, Suit.CLUBS), Card(Rank.TEN, Suit.CLUBS), Card(Rank.JACK, Suit.CLUBS)),
            crib = crib,
            starter = starter,
            phase = CribbagePhase.SHOW_CRIB
        )
        val result = CribbageController.scoreCrib(state)
        assertEquals("Dealer should swap to AI", CribbagePlayer.AI, result.dealer)
        assertEquals("Phase should return to DEALING", CribbagePhase.DEALING, result.phase)
    }

    // -------------------------------------------------------------------------
    // Game-over conditions
    // -------------------------------------------------------------------------

    @Test
    fun scoreCrib_reachTarget_setsGameOver() {
        val crib = listOf(
            Card(Rank.FIVE, Suit.CLUBS), Card(Rank.FIVE, Suit.DIAMONDS),
            Card(Rank.FIVE, Suit.HEARTS), Card(Rank.JACK, Suit.SPADES)
        )
        val starter = Card(Rank.FIVE, Suit.SPADES)
        val state = CribbageState(
            dealer = CribbagePlayer.HUMAN,
            humanHand = listOf(Card(Rank.TWO, Suit.CLUBS), Card(Rank.THREE, Suit.CLUBS), Card(Rank.FOUR, Suit.CLUBS), Card(Rank.SIX, Suit.CLUBS)),
            aiHand = listOf(Card(Rank.EIGHT, Suit.CLUBS), Card(Rank.NINE, Suit.CLUBS), Card(Rank.TEN, Suit.CLUBS), Card(Rank.QUEEN, Suit.CLUBS)),
            crib = crib,
            starter = starter,
            humanScore = 115, // 29 more = 144, well past 121
            phase = CribbagePhase.SHOW_CRIB,
            targetScore = 121
        )
        val result = CribbageController.scoreCrib(state)
        assertEquals("Should be GAME_OVER", CribbagePhase.GAME_OVER, result.phase)
        assertEquals("Human should be winner", CribbagePlayer.HUMAN, result.winner)
    }
}
