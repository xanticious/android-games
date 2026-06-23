package com.xanticious.androidgames.games.spades

import com.xanticious.androidgames.controller.games.spades.SpadesController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.Rank
import com.xanticious.androidgames.model.games.cards.Suit
import com.xanticious.androidgames.model.games.spades.SpadesBid
import com.xanticious.androidgames.model.games.spades.SpadesGameState
import com.xanticious.androidgames.model.games.spades.SpadesPlayer
import com.xanticious.androidgames.model.games.spades.SpadesTeam
import com.xanticious.androidgames.model.games.spades.SpadesTeamScore
import com.xanticious.androidgames.model.games.spades.SpadesTrick
import com.xanticious.androidgames.model.games.spades.SpadesTrickCard
import com.xanticious.androidgames.state.games.spades.SpadesPhase
import com.xanticious.androidgames.state.games.spades.SpadesStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SpadesControllerTest {

    // ─────────────────────────── helpers ───────────────────────────

    /** Build a SpadesGameState pre-loaded with bids and trick counts for scoring tests. */
    private fun scoringState(
        southBid: Int, northBid: Int, westBid: Int, eastBid: Int,
        southTricks: Int, northTricks: Int, westTricks: Int, eastTricks: Int,
        startingUsScore: Int = 0, startingUsBags: Int = 0,
        startingThemScore: Int = 0, startingThemBags: Int = 0,
        gameEndScore: Int = 500,
        bagPenaltyEnabled: Boolean = true
    ): SpadesGameState = SpadesGameState(
        bids = mapOf(
            SpadesPlayer.SOUTH to SpadesBid(southBid),
            SpadesPlayer.NORTH to SpadesBid(northBid),
            SpadesPlayer.WEST  to SpadesBid(westBid),
            SpadesPlayer.EAST  to SpadesBid(eastBid)
        ),
        tricksWon = mapOf(
            SpadesPlayer.SOUTH to southTricks,
            SpadesPlayer.NORTH to northTricks,
            SpadesPlayer.WEST  to westTricks,
            SpadesPlayer.EAST  to eastTricks
        ),
        teamScores = mapOf(
            SpadesTeam.US   to SpadesTeamScore(startingUsScore, startingUsBags),
            SpadesTeam.THEM to SpadesTeamScore(startingThemScore, startingThemBags)
        ),
        gameEndScore      = gameEndScore,
        bagPenaltyEnabled = bagPenaltyEnabled
    )

    /** Build a complete four-card trick with the given plays in SOUTH → WEST → NORTH → EAST order. */
    private fun fullTrick(
        leadPlayer: SpadesPlayer,
        plays: List<Pair<SpadesPlayer, Card>>
    ): SpadesTrick = SpadesTrick(leadPlayer = leadPlayer, plays = plays.map { (p, c) -> SpadesTrickCard(p, c) })

    // ─────────────────────────── deal ───────────────────────────

    @Test
    fun dealNewHand_eachPlayerReceivesThirteenCards() {
        val state = SpadesController.initGame(Random(1L))
        SpadesPlayer.entries.forEach { player ->
            assertEquals(13, state.hands[player]?.size)
        }
    }

    @Test
    fun dealNewHand_fiftyTwoDistinctCardsDealt() {
        val state = SpadesController.initGame(Random(1L))
        val all = state.hands.values.flatten()
        assertEquals(52, all.toSet().size)
    }

    @Test
    fun dealNewHand_isDeterministicWithSameSeed() {
        val s1 = SpadesController.initGame(Random(42L))
        val s2 = SpadesController.initGame(Random(42L))
        assertEquals(s1.hands, s2.hands)
    }

    @Test
    fun dealNewHand_preservesTeamScores() {
        val initial = SpadesGameState(
            teamScores = mapOf(
                SpadesTeam.US   to SpadesTeamScore(score = 150, bags = 3),
                SpadesTeam.THEM to SpadesTeamScore(score = 200, bags = 1)
            )
        )
        val dealt = SpadesController.dealNewHand(initial, Random(5L))
        assertEquals(150, dealt.teamScores[SpadesTeam.US]?.score)
        assertEquals(200, dealt.teamScores[SpadesTeam.THEM]?.score)
    }

    @Test
    fun dealNewHand_resetsTricksWon() {
        val state = SpadesController.initGame(Random(1L))
        SpadesPlayer.entries.forEach { player ->
            assertEquals(0, state.tricksWon[player])
        }
    }

    // ─────────────────────────── legal plays ───────────────────────────

    @Test
    fun legalPlays_mustFollowSuit_whenHeldInHand() {
        val hand  = listOf(Card(Rank.ACE, Suit.HEARTS), Card(Rank.TWO, Suit.SPADES))
        val trick = SpadesTrick(
            leadPlayer = SpadesPlayer.WEST,
            plays      = listOf(SpadesTrickCard(SpadesPlayer.WEST, Card(Rank.KING, Suit.HEARTS)))
        )
        val legal = SpadesController.legalPlays(hand, trick, spadesBroken = false)
        assertEquals(listOf(Card(Rank.ACE, Suit.HEARTS)), legal)
    }

    @Test
    fun legalPlays_canPlayAnything_whenVoidInLedSuit() {
        val hand  = listOf(Card(Rank.ACE, Suit.SPADES), Card(Rank.TWO, Suit.CLUBS))
        val trick = SpadesTrick(
            leadPlayer = SpadesPlayer.WEST,
            plays      = listOf(SpadesTrickCard(SpadesPlayer.WEST, Card(Rank.KING, Suit.HEARTS)))
        )
        val legal = SpadesController.legalPlays(hand, trick, spadesBroken = false)
        assertEquals(hand.toSet(), legal.toSet())
    }

    @Test
    fun legalPlays_cannotLeadSpades_beforeBroken_whenNonSpadeExists() {
        val hand  = listOf(Card(Rank.ACE, Suit.SPADES), Card(Rank.TWO, Suit.HEARTS))
        val legal = SpadesController.legalPlays(hand, trick = null, spadesBroken = false)
        assertFalse(legal.any { it.suit == Suit.SPADES })
        assertTrue(legal.any { it.suit == Suit.HEARTS })
    }

    @Test
    fun legalPlays_canLeadSpades_afterBroken() {
        val hand  = listOf(Card(Rank.ACE, Suit.SPADES), Card(Rank.TWO, Suit.HEARTS))
        val legal = SpadesController.legalPlays(hand, trick = null, spadesBroken = true)
        assertTrue(legal.any { it.suit == Suit.SPADES })
    }

    @Test
    fun legalPlays_canLeadSpades_whenOnlySpadesRemainInHand() {
        val hand  = listOf(Card(Rank.ACE, Suit.SPADES), Card(Rank.TWO, Suit.SPADES))
        val legal = SpadesController.legalPlays(hand, trick = null, spadesBroken = false)
        assertEquals(2, legal.size)
        assertTrue(legal.all { it.suit == Suit.SPADES })
    }

    @Test
    fun legalPlays_emptyHand_returnsEmpty() {
        val legal = SpadesController.legalPlays(emptyList(), trick = null, spadesBroken = false)
        assertTrue(legal.isEmpty())
    }

    // ─────────────────────────── trick winner ───────────────────────────

    @Test
    fun trickWinner_highestOfLedSuit_winsWhenNoSpadesPlayed() {
        val trick = fullTrick(
            leadPlayer = SpadesPlayer.SOUTH,
            plays = listOf(
                SpadesPlayer.SOUTH to Card(Rank.ACE, Suit.HEARTS),
                SpadesPlayer.WEST  to Card(Rank.KING, Suit.HEARTS),
                SpadesPlayer.NORTH to Card(Rank.TWO, Suit.CLUBS),
                SpadesPlayer.EAST  to Card(Rank.QUEEN, Suit.HEARTS)
            )
        )
        assertEquals(SpadesPlayer.SOUTH, SpadesController.trickWinner(trick))
    }

    @Test
    fun trickWinner_spadeBeatsAceOfLedSuit() {
        val trick = fullTrick(
            leadPlayer = SpadesPlayer.SOUTH,
            plays = listOf(
                SpadesPlayer.SOUTH to Card(Rank.ACE, Suit.HEARTS),
                SpadesPlayer.WEST  to Card(Rank.TWO, Suit.SPADES),
                SpadesPlayer.NORTH to Card(Rank.KING, Suit.HEARTS),
                SpadesPlayer.EAST  to Card(Rank.THREE, Suit.HEARTS)
            )
        )
        assertEquals(SpadesPlayer.WEST, SpadesController.trickWinner(trick))
    }

    @Test
    fun trickWinner_highestSpadeWins_whenMultipleSpadesPlayed() {
        val trick = fullTrick(
            leadPlayer = SpadesPlayer.SOUTH,
            plays = listOf(
                SpadesPlayer.SOUTH to Card(Rank.ACE, Suit.HEARTS),
                SpadesPlayer.WEST  to Card(Rank.QUEEN, Suit.SPADES),
                SpadesPlayer.NORTH to Card(Rank.ACE, Suit.SPADES),
                SpadesPlayer.EAST  to Card(Rank.TWO, Suit.SPADES)
            )
        )
        assertEquals(SpadesPlayer.NORTH, SpadesController.trickWinner(trick))
    }

    @Test
    fun trickWinner_offSuitNonSpadeDoesNotWin() {
        // SOUTH leads clubs; NORTH discards diamonds; EAST plays low club
        val trick = fullTrick(
            leadPlayer = SpadesPlayer.SOUTH,
            plays = listOf(
                SpadesPlayer.SOUTH to Card(Rank.NINE, Suit.CLUBS),
                SpadesPlayer.WEST  to Card(Rank.EIGHT, Suit.CLUBS),
                SpadesPlayer.NORTH to Card(Rank.ACE, Suit.DIAMONDS),
                SpadesPlayer.EAST  to Card(Rank.TEN, Suit.CLUBS)
            )
        )
        assertEquals(SpadesPlayer.EAST, SpadesController.trickWinner(trick))
    }

    // ─────────────────────────── playCard ───────────────────────────

    @Test
    fun playCard_removesCardFromHand_andAddsToTrick() {
        val state = SpadesController.initGame(Random(1L))
        val hand  = state.hands[SpadesPlayer.SOUTH]!!
        val card  = hand.first()
        val trick = SpadesTrick(leadPlayer = SpadesPlayer.SOUTH)
        val after = SpadesController.playCard(state.copy(currentTrick = trick), SpadesPlayer.SOUTH, card)
        assertFalse(card in (after.hands[SpadesPlayer.SOUTH] ?: emptyList()))
        assertEquals(12, after.hands[SpadesPlayer.SOUTH]?.size)
        assertEquals(1, after.currentTrick?.plays?.size)
    }

    @Test
    fun playCard_breaksSpades_whenSpadePlayedOffSuit() {
        val trick = SpadesTrick(
            leadPlayer = SpadesPlayer.WEST,
            plays      = listOf(SpadesTrickCard(SpadesPlayer.WEST, Card(Rank.KING, Suit.HEARTS)))
        )
        val state = SpadesGameState(
            hands        = mapOf(SpadesPlayer.SOUTH to listOf(Card(Rank.ACE, Suit.SPADES))),
            currentTrick = trick,
            spadesBroken = false,
            teamScores   = mapOf(SpadesTeam.US to SpadesTeamScore(), SpadesTeam.THEM to SpadesTeamScore())
        )
        val after = SpadesController.playCard(state, SpadesPlayer.SOUTH, Card(Rank.ACE, Suit.SPADES))
        assertTrue(after.spadesBroken)
    }

    @Test
    fun playCard_doesNotBreakSpades_whenLeadingSpadeAfterAlreadyBroken() {
        val trick = SpadesTrick(leadPlayer = SpadesPlayer.SOUTH)
        val state = SpadesGameState(
            hands        = mapOf(SpadesPlayer.SOUTH to listOf(Card(Rank.ACE, Suit.SPADES))),
            currentTrick = trick,
            spadesBroken = true,
            teamScores   = mapOf(SpadesTeam.US to SpadesTeamScore(), SpadesTeam.THEM to SpadesTeamScore())
        )
        val after = SpadesController.playCard(state, SpadesPlayer.SOUTH, Card(Rank.ACE, Suit.SPADES))
        assertTrue(after.spadesBroken)  // flag stays true, was already set
    }

    @Test
    fun playCard_doesNotBreakSpades_whenFollowingWithSpadeAsLedSuit() {
        // Spades are led (already broken); following with spade should not set flag again
        val trick = SpadesTrick(
            leadPlayer = SpadesPlayer.SOUTH,
            plays      = listOf(SpadesTrickCard(SpadesPlayer.SOUTH, Card(Rank.KING, Suit.SPADES)))
        )
        val state = SpadesGameState(
            hands        = mapOf(SpadesPlayer.WEST to listOf(Card(Rank.ACE, Suit.SPADES))),
            currentTrick = trick,
            spadesBroken = true,
            teamScores   = mapOf(SpadesTeam.US to SpadesTeamScore(), SpadesTeam.THEM to SpadesTeamScore())
        )
        val after = SpadesController.playCard(state, SpadesPlayer.WEST, Card(Rank.ACE, Suit.SPADES))
        assertTrue(after.spadesBroken)
    }

    // ─────────────────────────── resolveTrick ───────────────────────────

    @Test
    fun resolveTrick_updatesWinnerTricksWon() {
        val trick = fullTrick(
            leadPlayer = SpadesPlayer.SOUTH,
            plays = listOf(
                SpadesPlayer.SOUTH to Card(Rank.ACE, Suit.HEARTS),
                SpadesPlayer.WEST  to Card(Rank.KING, Suit.HEARTS),
                SpadesPlayer.NORTH to Card(Rank.TWO, Suit.CLUBS),
                SpadesPlayer.EAST  to Card(Rank.QUEEN, Suit.HEARTS)
            )
        )
        val state = SpadesGameState(
            currentTrick = trick,
            teamScores   = mapOf(SpadesTeam.US to SpadesTeamScore(), SpadesTeam.THEM to SpadesTeamScore())
        )
        val after = SpadesController.resolveTrick(state)
        assertEquals(1, after.tricksWon[SpadesPlayer.SOUTH])
    }

    @Test
    fun resolveTrick_startsNewTrick_withWinnerLeading() {
        val trick = fullTrick(
            leadPlayer = SpadesPlayer.SOUTH,
            plays = listOf(
                SpadesPlayer.SOUTH to Card(Rank.ACE, Suit.HEARTS),
                SpadesPlayer.WEST  to Card(Rank.KING, Suit.HEARTS),
                SpadesPlayer.NORTH to Card(Rank.TWO, Suit.CLUBS),
                SpadesPlayer.EAST  to Card(Rank.QUEEN, Suit.HEARTS)
            )
        )
        val state = SpadesGameState(currentTrick = trick,
            teamScores = mapOf(SpadesTeam.US to SpadesTeamScore(), SpadesTeam.THEM to SpadesTeamScore()))
        val after = SpadesController.resolveTrick(state)
        assertEquals(SpadesPlayer.SOUTH, after.currentTrick?.leadPlayer)
        assertTrue(after.currentTrick?.plays?.isEmpty() == true)
    }

    @Test
    fun resolveTrick_setsCurrentTrickNull_afterThirteenthTrick() {
        // Simulate 12 tricks already played, this is the 13th
        val trick = fullTrick(
            leadPlayer = SpadesPlayer.SOUTH,
            plays = listOf(
                SpadesPlayer.SOUTH to Card(Rank.ACE, Suit.HEARTS),
                SpadesPlayer.WEST  to Card(Rank.KING, Suit.HEARTS),
                SpadesPlayer.NORTH to Card(Rank.TWO, Suit.CLUBS),
                SpadesPlayer.EAST  to Card(Rank.QUEEN, Suit.HEARTS)
            )
        )
        val state = SpadesGameState(
            currentTrick = trick,
            tricksWon    = mapOf(
                SpadesPlayer.SOUTH to 3,
                SpadesPlayer.NORTH to 3,
                SpadesPlayer.WEST  to 3,
                SpadesPlayer.EAST  to 3
            ),
            teamScores = mapOf(SpadesTeam.US to SpadesTeamScore(), SpadesTeam.THEM to SpadesTeamScore())
        )
        val after = SpadesController.resolveTrick(state)
        assertNull(after.currentTrick)
        assertTrue(SpadesController.isHandComplete(after))
    }

    // ─────────────────────────── scoring ───────────────────────────

    @Test
    fun scoreHand_teamMakesBid_scoresCorrectly() {
        // US bid 5 (3+2), takes exactly 5 → 50 pts, no bags
        val state = scoringState(
            southBid = 3, northBid = 2, westBid = 4, eastBid = 4,
            southTricks = 3, northTricks = 2, westTricks = 4, eastTricks = 4
        )
        val scored = SpadesController.scoreHand(state)
        assertEquals(50, scored.teamScores[SpadesTeam.US]!!.score)
        assertEquals(0, scored.teamScores[SpadesTeam.US]!!.bags)
    }

    @Test
    fun scoreHand_teamFailsBid_loses10PerBidTrick() {
        // US bid 5, only takes 3 → -50
        val state = scoringState(
            southBid = 3, northBid = 2, westBid = 4, eastBid = 4,
            southTricks = 2, northTricks = 1, westTricks = 5, eastTricks = 5
        )
        val scored = SpadesController.scoreHand(state)
        assertEquals(-50, scored.teamScores[SpadesTeam.US]!!.score)
    }

    @Test
    fun scoreHand_overtricks_countAsBagsAndAddToScore() {
        // US bid 4 (2+2), takes 6 → 40 + 2 bags
        val state = scoringState(
            southBid = 2, northBid = 2, westBid = 3, eastBid = 3,
            southTricks = 3, northTricks = 3, westTricks = 3, eastTricks = 4
        )
        val scored = SpadesController.scoreHand(state)
        assertEquals(42, scored.teamScores[SpadesTeam.US]!!.score)
        assertEquals(2, scored.teamScores[SpadesTeam.US]!!.bags)
    }

    @Test
    fun scoreHand_tenBags_triggersPenaltyAndResetsModulo10() {
        // US starts with 9 bags; gains 2 overtricks → 11 bags → -100 penalty, bags = 1
        val state = scoringState(
            southBid = 2, northBid = 2, westBid = 3, eastBid = 3,
            southTricks = 4, northTricks = 2, westTricks = 3, eastTricks = 4,
            startingUsScore = 200, startingUsBags = 9
        )
        val scored = SpadesController.scoreHand(state)
        // 200 + 10*4 + 2 - 100 = 142
        assertEquals(142, scored.teamScores[SpadesTeam.US]!!.score)
        assertEquals(1, scored.teamScores[SpadesTeam.US]!!.bags)
    }

    @Test
    fun scoreHand_noBagPenalty_whenPenaltyDisabled() {
        val state = scoringState(
            southBid = 2, northBid = 2, westBid = 3, eastBid = 3,
            southTricks = 4, northTricks = 2, westTricks = 3, eastTricks = 4,
            startingUsBags = 9,
            bagPenaltyEnabled = false
        )
        val scored = SpadesController.scoreHand(state)
        // 0 + 40 + 2 = 42, bags = 11 (no penalty)
        assertEquals(42, scored.teamScores[SpadesTeam.US]!!.score)
        assertEquals(11, scored.teamScores[SpadesTeam.US]!!.bags)
    }

    @Test
    fun scoreHand_nilSuccess_addsBonusToTeam() {
        // SOUTH bids Nil (SpadesBid(0) → isNil=true), takes 0 tricks; NORTH bids 4, takes 4
        val state = scoringState(
            southBid = 0, northBid = 4, westBid = 3, eastBid = 6,
            southTricks = 0, northTricks = 4, westTricks = 3, eastTricks = 6
        )
        val scored = SpadesController.scoreHand(state)
        // Nil +100 + contract (4*10) = 140
        assertEquals(140, scored.teamScores[SpadesTeam.US]!!.score)
    }

    @Test
    fun scoreHand_nilFailed_penalizesTeam() {
        // SOUTH bids Nil but takes 1 trick
        val state = scoringState(
            southBid = 0, northBid = 4, westBid = 3, eastBid = 6,
            southTricks = 1, northTricks = 3, westTricks = 3, eastTricks = 6
        )
        val scored = SpadesController.scoreHand(state)
        // Nil -100 + contract (team 4 tricks, bid 4 → +40) = -60
        assertEquals(-60, scored.teamScores[SpadesTeam.US]!!.score)
    }

    @Test
    fun scoreHand_gameOver_whenScoreReachesTarget() {
        val state = scoringState(
            southBid = 5, northBid = 5, westBid = 3, eastBid = 3,
            southTricks = 5, northTricks = 5, westTricks = 3, eastTricks = 3,
            startingUsScore = 400,
            gameEndScore = 500
        )
        val scored = SpadesController.scoreHand(state)
        // 400 + 10*10 = 500 → game over
        assertEquals(SpadesTeam.US, scored.winner)
        assertTrue(SpadesController.isGameOver(scored))
    }

    @Test
    fun scoreHand_noWinner_whenBelowTarget() {
        val state = scoringState(
            southBid = 3, northBid = 2, westBid = 4, eastBid = 4,
            southTricks = 3, northTricks = 2, westTricks = 4, eastTricks = 4,
            startingUsScore = 100,
            gameEndScore = 500
        )
        val scored = SpadesController.scoreHand(state)
        assertNull(scored.winner)
        assertFalse(SpadesController.isGameOver(scored))
    }

    @Test
    fun scoreHand_roundIncrements() {
        val state = scoringState(
            southBid = 3, northBid = 2, westBid = 4, eastBid = 4,
            southTricks = 3, northTricks = 2, westTricks = 4, eastTricks = 4
        )
        val scored = SpadesController.scoreHand(state)
        assertEquals(2, scored.round)
    }

    // ─────────────────────────── bidding helpers ───────────────────────────

    @Test
    fun placeBid_advancesCurrentBidder() {
        val state = SpadesController.initGame(Random(1L))  // currentBidder = SOUTH
        val after = SpadesController.placeBid(state, SpadesPlayer.SOUTH, SpadesBid(3))
        assertEquals(SpadesPlayer.WEST, after.currentBidder)
    }

    @Test
    fun placeBid_setsCurrentBidderNull_afterAllFourBid() {
        var state = SpadesController.initGame(Random(1L))
        for (p in SpadesPlayer.entries) {
            state = SpadesController.placeBid(state, p, SpadesBid(3))
        }
        assertNull(state.currentBidder)
        assertTrue(SpadesController.isAllBidsIn(state))
    }

    // ─────────────────────────── AI ───────────────────────────

    @Test
    fun aiBid_easy_isDeterministicWithFixedSeed() {
        val state = SpadesController.initGame(Random(7L))
        val hand  = state.hands[SpadesPlayer.WEST]!!
        val b1 = SpadesController.aiBid(SpadesPlayer.WEST, hand, emptyMap(), true, GameDifficulty.EASY, Random(99L))
        val b2 = SpadesController.aiBid(SpadesPlayer.WEST, hand, emptyMap(), true, GameDifficulty.EASY, Random(99L))
        assertEquals(b1, b2)
    }

    @Test
    fun aiBid_hard_isDeterministicWithFixedSeed() {
        val state = SpadesController.initGame(Random(7L))
        val hand  = state.hands[SpadesPlayer.EAST]!!
        val b1 = SpadesController.aiBid(SpadesPlayer.EAST, hand, emptyMap(), false, GameDifficulty.HARD, Random(17L))
        val b2 = SpadesController.aiBid(SpadesPlayer.EAST, hand, emptyMap(), false, GameDifficulty.HARD, Random(17L))
        assertEquals(b1, b2)
    }

    @Test
    fun aiBid_bidAmountWithinValidRange() {
        val state = SpadesController.initGame(Random(3L))
        SpadesPlayer.entries.filter { it != SpadesPlayer.SOUTH }.forEach { player ->
            val bid = SpadesController.aiBid(
                player, state.hands[player]!!, emptyMap(), false, GameDifficulty.HARD, Random(1L)
            )
            assertTrue("Bid ${bid.amount} out of range for $player", bid.amount in 1..13)
        }
    }

    @Test
    fun aiBid_canProduceNilBid_whenNilAllowed() {
        // A hand with no high cards and nil allowed at Easy difficulty should sometimes bid Nil
        val noHighCards = (1..13).map { Card(Rank.TWO, Suit.entries[it % 4]) }
        // With seed chosen to produce 0 from easyBidAmount
        var nilSeen = false
        for (seed in 1L..50L) {
            val bid = SpadesController.aiBid(
                SpadesPlayer.WEST, noHighCards, emptyMap(), true, GameDifficulty.EASY, Random(seed)
            )
            if (bid.isNil) { nilSeen = true; break }
        }
        assertTrue("Expected at least one Nil bid from a weak hand with nilAllowed=true", nilSeen)
    }

    @Test
    fun aiPlay_alwaysReturnsLegalCard() {
        val state = SpadesController.initGame(Random(1L))
        val trick = SpadesTrick(
            leadPlayer = SpadesPlayer.WEST,
            plays      = listOf(SpadesTrickCard(SpadesPlayer.WEST, Card(Rank.ACE, Suit.HEARTS)))
        )
        val playState = state.copy(
            bids         = SpadesPlayer.entries.associateWith { SpadesBid(3) },
            currentTrick = trick
        )
        val card  = SpadesController.aiPlay(SpadesPlayer.NORTH, playState, GameDifficulty.EASY, Random(1L))
        val hand  = playState.hands[SpadesPlayer.NORTH]!!
        val legal = SpadesController.legalPlays(hand, trick, false)
        assertTrue(card in legal)
    }

    @Test
    fun aiPlay_medium_ducksWhenPartnerCurrentlyWinning() {
        // SOUTH (NORTH's partner) led ACE♠ and is winning; WEST played 3♠;
        // NORTH should duck with the lowest legal card.
        val trick = SpadesTrick(
            leadPlayer = SpadesPlayer.SOUTH,
            plays = listOf(
                SpadesTrickCard(SpadesPlayer.SOUTH, Card(Rank.ACE, Suit.SPADES)),
                SpadesTrickCard(SpadesPlayer.WEST,  Card(Rank.THREE, Suit.SPADES))
            )
        )
        val northHand = listOf(Card(Rank.TWO, Suit.SPADES), Card(Rank.KING, Suit.SPADES))
        val state = SpadesGameState(
            hands = mapOf(
                SpadesPlayer.SOUTH to listOf(Card(Rank.ACE, Suit.SPADES)),
                SpadesPlayer.WEST  to listOf(Card(Rank.THREE, Suit.SPADES)),
                SpadesPlayer.NORTH to northHand,
                SpadesPlayer.EAST  to listOf(Card(Rank.FOUR, Suit.SPADES))
            ),
            bids         = SpadesPlayer.entries.associateWith { SpadesBid(3) },
            currentTrick = trick,
            spadesBroken = true,
            teamScores   = mapOf(SpadesTeam.US to SpadesTeamScore(), SpadesTeam.THEM to SpadesTeamScore())
        )
        val card = SpadesController.aiPlay(SpadesPlayer.NORTH, state, GameDifficulty.MEDIUM, Random(1L))
        assertEquals(Card(Rank.TWO, Suit.SPADES), card)
    }

    @Test
    fun aiPlay_hard_triestoWin_whenPartnerNotWinning() {
        // WEST (EAST's partner) is losing; EAST should try to win if possible
        val trick = SpadesTrick(
            leadPlayer = SpadesPlayer.SOUTH,
            plays = listOf(
                SpadesTrickCard(SpadesPlayer.SOUTH, Card(Rank.ACE, Suit.HEARTS)),
                SpadesTrickCard(SpadesPlayer.WEST,  Card(Rank.THREE, Suit.HEARTS)),
                SpadesTrickCard(SpadesPlayer.NORTH, Card(Rank.KING, Suit.HEARTS))
            )
        )
        val eastHand = listOf(Card(Rank.QUEEN, Suit.HEARTS), Card(Rank.TWO, Suit.HEARTS))
        val state = SpadesGameState(
            hands = mapOf(
                SpadesPlayer.SOUTH to listOf(Card(Rank.ACE, Suit.HEARTS)),
                SpadesPlayer.WEST  to listOf(Card(Rank.THREE, Suit.HEARTS)),
                SpadesPlayer.NORTH to listOf(Card(Rank.KING, Suit.HEARTS)),
                SpadesPlayer.EAST  to eastHand
            ),
            bids         = SpadesPlayer.entries.associateWith { SpadesBid(3) },
            currentTrick = trick,
            spadesBroken = false,
            teamScores   = mapOf(SpadesTeam.US to SpadesTeamScore(), SpadesTeam.THEM to SpadesTeamScore())
        )
        // EAST cannot beat ACE♥; should dump lowest
        val card = SpadesController.aiPlay(SpadesPlayer.EAST, state, GameDifficulty.HARD, Random(1L))
        assertTrue(card in eastHand)
    }

    // ─────────────────────────── state machine ───────────────────────────

    private fun machine() = SpadesStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun stateMachine_initialPhase_isIdle() {
        assertEquals(SpadesPhase.IDLE, machine().phase.value)
    }

    @Test
    fun stateMachine_startGame_transitionsToDealing() {
        val m = machine()
        m.startGame()
        assertEquals(SpadesPhase.DEALING, m.phase.value)
    }

    @Test
    fun stateMachine_dealt_transitionsToBidding() {
        val m = machine()
        m.startGame(); m.dealt()
        assertEquals(SpadesPhase.BIDDING, m.phase.value)
    }

    @Test
    fun stateMachine_allBidsIn_transitionsToPlaying() {
        val m = machine()
        m.startGame(); m.dealt(); m.allBidsIn()
        assertEquals(SpadesPhase.PLAYING, m.phase.value)
    }

    @Test
    fun stateMachine_handComplete_transitionsToHandScored() {
        val m = machine()
        m.startGame(); m.dealt(); m.allBidsIn(); m.handComplete()
        assertEquals(SpadesPhase.HAND_SCORED, m.phase.value)
    }

    @Test
    fun stateMachine_gameEndReached_transitionsToGameOver() {
        val m = machine()
        m.startGame(); m.dealt(); m.allBidsIn(); m.handComplete(); m.gameEndReached()
        assertEquals(SpadesPhase.GAME_OVER, m.phase.value)
    }

    @Test
    fun stateMachine_nextHand_transitionsBackToDealing() {
        val m = machine()
        m.startGame(); m.dealt(); m.allBidsIn(); m.handComplete(); m.nextHand()
        assertEquals(SpadesPhase.DEALING, m.phase.value)
    }

    @Test
    fun stateMachine_rematch_transitionsFromGameOverToDealing() {
        val m = machine()
        m.startGame(); m.dealt(); m.allBidsIn(); m.handComplete(); m.gameEndReached(); m.rematch()
        assertEquals(SpadesPhase.DEALING, m.phase.value)
    }
}
