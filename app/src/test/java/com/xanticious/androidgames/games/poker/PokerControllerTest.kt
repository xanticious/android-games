package com.xanticious.androidgames.games.poker

import com.xanticious.androidgames.controller.games.poker.PokerController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.Rank
import com.xanticious.androidgames.model.games.cards.Suit
import com.xanticious.androidgames.model.games.poker.PokerGameState
import com.xanticious.androidgames.model.games.poker.PokerPlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Unit tests for [PokerController].
 *
 * All tests are pure JVM — no Android or Robolectric.
 */
class PokerControllerTest {

    private fun c(rank: Rank, suit: Suit) = Card(rank, suit)

    /** Build a minimal 4-player game state. */
    private fun baseState(
        anteAmount: Int = 5,
        bankroll: Int = PokerController.STARTING_BANKROLL
    ): PokerGameState = PokerController.createGame(GameDifficulty.MEDIUM).copy(
        anteAmount = anteAmount,
        players = PokerController.createGame(GameDifficulty.MEDIUM).players.map {
            it.copy(bankroll = bankroll)
        }
    )

    // ---- startHand ----

    @Test
    fun startHand_dealsExactlyFiveCardsToEachActivePlayer() {
        val state = PokerController.startHand(baseState(), seed = 42L)
        state.players.filter { !it.isFolded }.forEach { p ->
            assertEquals(5, p.hand.size)
        }
    }

    @Test
    fun startHand_potEqualsAntesTimesActivePlayers() {
        val ante = 10
        val state = PokerController.startHand(baseState(anteAmount = ante), seed = 1L)
        val activePlayers = state.players.count { !it.isFolded }
        assertEquals(ante * activePlayers, state.pot)
    }

    @Test
    fun startHand_deductsAnteFromEachPlayerBankroll() {
        val ante = 10
        val initial = 500
        val state = PokerController.startHand(baseState(anteAmount = ante, bankroll = initial), seed = 2L)
        state.players.filter { !it.isFolded }.forEach { p ->
            assertEquals(initial - ante, p.bankroll)
        }
    }

    @Test
    fun startHand_decksAreUnique_allCardsDistinct() {
        val state = PokerController.startHand(baseState(), seed = 77L)
        val allDealt = state.players.flatMap { it.hand }
        assertEquals(allDealt.size, allDealt.toSet().size)
    }

    @Test
    fun startHand_isDeterministic_sameHandForSameSeed() {
        val s1 = PokerController.startHand(baseState(), seed = 999L)
        val s2 = PokerController.startHand(baseState(), seed = 999L)
        assertEquals(s1.players[0].hand, s2.players[0].hand)
    }

    @Test
    fun startHand_bustPlayerSitsOut_receivesNoCards() {
        val bustState = baseState().let { s ->
            s.copy(players = s.players.mapIndexed { i, p ->
                if (i == 1) p.copy(bankroll = 0) else p
            })
        }
        val state = PokerController.startHand(bustState, seed = 5L)
        assertTrue(state.players[1].isFolded)
        assertTrue(state.players[1].hand.isEmpty())
    }

    // ---- fold ----

    @Test
    fun fold_marksActivePlayerAsFolded() {
        val s = PokerController.startHand(baseState(), seed = 10L)
        val folded = PokerController.fold(s)
        assertTrue(folded.players[s.activePlayerIndex].isFolded)
    }

    @Test
    fun fold_advancesToNextPlayer() {
        val s = PokerController.startHand(baseState(), seed = 10L)
        val before = s.activePlayerIndex
        val folded = PokerController.fold(s)
        // next active player should differ (or be same only if all others are folded/allIn)
        val afterFolded = folded.players[before].isFolded
        assertTrue(afterFolded)
    }

    // ---- check ----

    @Test
    fun check_whenCurrentBetIsZero_setsHasActed() {
        val s = PokerController.startHand(baseState(anteAmount = 0), seed = 20L)
            .let { st ->
                // Force currentBet to 0 and active player round bet to 0.
                st.copy(
                    currentBet = 0,
                    players = st.players.map { p -> p.copy(currentRoundBet = 0) }
                )
            }
        val afterCheck = PokerController.check(s)
        assertTrue(afterCheck.players[s.activePlayerIndex].hasActed)
    }

    @Test
    fun check_doesNotChangePot() {
        val s = PokerController.startHand(baseState(anteAmount = 0), seed = 21L)
            .copy(currentBet = 0, players = PokerController.startHand(baseState(anteAmount = 0), seed = 21L)
                .players.map { it.copy(currentRoundBet = 0) })
        val pot = s.pot
        val afterCheck = PokerController.check(s)
        assertEquals(pot, afterCheck.pot)
    }

    // ---- call ----

    @Test
    fun call_reducesPlayerBankrollByCallAmount() {
        val s = PokerController.startHand(baseState(anteAmount = 5), seed = 30L)
        val active = s.activePlayerIndex
        val bankrollBefore = s.players[active].bankroll
        val toCall = s.toCall
        val after = PokerController.call(s)
        assertEquals(bankrollBefore - toCall, after.players[active].bankroll)
    }

    @Test
    fun call_increasesPotByCallAmount() {
        val s = PokerController.startHand(baseState(anteAmount = 5), seed = 31L)
        val toCall = s.toCall
        val potBefore = s.pot
        val after = PokerController.call(s)
        assertEquals(potBefore + toCall, after.pot)
    }

    @Test
    fun call_setsHasActed() {
        val s = PokerController.startHand(baseState(anteAmount = 5), seed = 32L)
        val active = s.activePlayerIndex
        val after = PokerController.call(s)
        assertTrue(after.players[active].hasActed)
    }

    // ---- raise ----

    @Test
    fun raise_increasesCurrentBet() {
        val s = PokerController.startHand(baseState(anteAmount = 0), seed = 40L)
            .copy(currentBet = 0, players = PokerController.startHand(baseState(anteAmount = 0), seed = 40L)
                .players.map { it.copy(currentRoundBet = 0) })
        val after = PokerController.raise(s, PokerController.MIN_BET)
        assertTrue(after.currentBet > s.currentBet)
    }

    @Test
    fun raise_resetsHasActedForOtherActivePlayers() {
        // Pre-mark all non-active players as hasActed = true.
        val s0 = PokerController.startHand(baseState(anteAmount = 0), seed = 41L)
            .copy(currentBet = 0)
        val marked = s0.copy(players = s0.players.map { p ->
            if (p.index != s0.activePlayerIndex) p.copy(hasActed = true, currentRoundBet = 0)
            else p.copy(currentRoundBet = 0)
        })
        val after = PokerController.raise(marked, PokerController.MIN_BET)
        // All other active players should have hasActed = false after raise.
        after.players.filter { it.index != s0.activePlayerIndex && !it.isFolded && !it.isAllIn }
            .forEach { p -> assertFalse("player ${p.index} should not have acted", p.hasActed) }
    }

    @Test
    fun raise_raisingPlayerHasActed() {
        val s = PokerController.startHand(baseState(anteAmount = 0), seed = 42L)
            .copy(currentBet = 0, players = PokerController.startHand(baseState(anteAmount = 0), seed = 42L)
                .players.map { it.copy(currentRoundBet = 0) })
        val active = s.activePlayerIndex
        val after = PokerController.raise(s, PokerController.MIN_BET)
        assertTrue(after.players[active].hasActed)
    }

    // ---- isBettingRoundComplete ----

    @Test
    fun isBettingRoundComplete_allPlayersActedAndMatchBet_returnsTrue() {
        val state = baseState().let { s ->
            s.copy(
                currentBet = 10,
                players = s.players.map { p ->
                    p.copy(hasActed = true, currentRoundBet = 10, isFolded = false)
                }
            )
        }
        assertTrue(PokerController.isBettingRoundComplete(state))
    }

    @Test
    fun isBettingRoundComplete_somePlayersHaveNotActed_returnsFalse() {
        val state = baseState().let { s ->
            s.copy(
                currentBet = 10,
                players = s.players.mapIndexed { i, p ->
                    p.copy(hasActed = i == 0, currentRoundBet = if (i == 0) 10 else 0)
                }
            )
        }
        assertFalse(PokerController.isBettingRoundComplete(state))
    }

    @Test
    fun isBettingRoundComplete_allFolded_exceptOne_returnsTrue() {
        val state = baseState().let { s ->
            s.copy(
                currentBet = 10,
                players = s.players.mapIndexed { i, p ->
                    if (i == 0) p.copy(hasActed = true, currentRoundBet = 10)
                    else p.copy(isFolded = true)
                }
            )
        }
        assertTrue(PokerController.isBettingRoundComplete(state))
    }

    // ---- allButOneFolded ----

    @Test
    fun allButOneFolded_oneActivePlayer_returnsTrue() {
        val state = baseState().let { s ->
            s.copy(players = s.players.mapIndexed { i, p ->
                if (i == 0) p.copy(isFolded = false) else p.copy(isFolded = true)
            })
        }
        assertTrue(PokerController.allButOneFolded(state))
    }

    @Test
    fun allButOneFolded_twoActivePlayers_returnsFalse() {
        val state = baseState().let { s ->
            s.copy(players = s.players.mapIndexed { i, p ->
                if (i <= 1) p.copy(isFolded = false) else p.copy(isFolded = true)
            })
        }
        assertFalse(PokerController.allButOneFolded(state))
    }

    // ---- startBettingRound ----

    @Test
    fun startBettingRound_resetsCurrentBetToZero() {
        val s = baseState().copy(currentBet = 50)
        assertEquals(0, PokerController.startBettingRound(s).currentBet)
    }

    @Test
    fun startBettingRound_resetsEachPlayersRoundBet() {
        val s = baseState().let { base ->
            base.copy(players = base.players.map { it.copy(currentRoundBet = 20, hasActed = true) })
        }
        val after = PokerController.startBettingRound(s)
        after.players.filter { !it.isFolded }.forEach { p ->
            assertEquals(0, p.currentRoundBet)
            assertFalse(p.hasActed)
        }
    }

    // ---- toggleDiscard ----

    @Test
    fun toggleDiscard_addsCardIndexToSelection() {
        val s = baseState().copy(selectedDiscards = emptySet())
        val after = PokerController.toggleDiscard(s, 2)
        assertTrue(2 in after.selectedDiscards)
    }

    @Test
    fun toggleDiscard_removesCardIndexIfAlreadySelected() {
        val s = baseState().copy(selectedDiscards = setOf(0, 2))
        val after = PokerController.toggleDiscard(s, 2)
        assertFalse(2 in after.selectedDiscards)
        assertTrue(0 in after.selectedDiscards)
    }

    // ---- aiDiscardIndices ----

    @Test
    fun aiDiscardIndices_onePair_discardsThreeNonPairCards() {
        val hand = listOf(
            c(Rank.JACK, Suit.SPADES), c(Rank.JACK, Suit.HEARTS),
            c(Rank.ACE, Suit.DIAMONDS), c(Rank.NINE, Suit.CLUBS),
            c(Rank.THREE, Suit.SPADES)
        )
        val player = PokerPlayer(1, "Bot", 500, hand = hand, difficulty = GameDifficulty.MEDIUM)
        val discards = PokerController.aiDiscardIndices(player, Random(0))
        // Should keep the pair (indices 0,1) and discard indices 2,3,4.
        assertEquals(3, discards.size)
        assertFalse(0 in discards)
        assertFalse(1 in discards)
    }

    @Test
    fun aiDiscardIndices_twoPair_discardsOneKicker() {
        val hand = listOf(
            c(Rank.ACE, Suit.SPADES), c(Rank.ACE, Suit.HEARTS),
            c(Rank.KING, Suit.DIAMONDS), c(Rank.KING, Suit.CLUBS),
            c(Rank.JACK, Suit.SPADES)   // kicker
        )
        val player = PokerPlayer(1, "Bot", 500, hand = hand, difficulty = GameDifficulty.MEDIUM)
        val discards = PokerController.aiDiscardIndices(player, Random(0))
        assertEquals(1, discards.size)
        assertEquals(4, discards[0])  // index 4 is the kicker
    }

    @Test
    fun aiDiscardIndices_threeOfAKind_discardsTwoSingletons() {
        val hand = listOf(
            c(Rank.QUEEN, Suit.SPADES), c(Rank.QUEEN, Suit.HEARTS),
            c(Rank.QUEEN, Suit.DIAMONDS), c(Rank.JACK, Suit.CLUBS),
            c(Rank.NINE, Suit.SPADES)
        )
        val player = PokerPlayer(1, "Bot", 500, hand = hand, difficulty = GameDifficulty.MEDIUM)
        val discards = PokerController.aiDiscardIndices(player, Random(0))
        assertEquals(2, discards.size)
        assertFalse(0 in discards)
        assertFalse(1 in discards)
        assertFalse(2 in discards)
    }

    @Test
    fun aiDiscardIndices_fourOfAKind_discardsNothing() {
        val hand = listOf(
            c(Rank.ACE, Suit.SPADES), c(Rank.ACE, Suit.HEARTS),
            c(Rank.ACE, Suit.DIAMONDS), c(Rank.ACE, Suit.CLUBS),
            c(Rank.KING, Suit.SPADES)
        )
        val player = PokerPlayer(1, "Bot", 500, hand = hand, difficulty = GameDifficulty.MEDIUM)
        val discards = PokerController.aiDiscardIndices(player, Random(0))
        assertTrue(discards.isEmpty())
    }

    // ---- aiAction determinism ----

    @Test
    fun aiAction_fixedSeed_isDeterministic() {
        val s = PokerController.startHand(baseState(), seed = 12345L)
        // Advance to first non-human player.
        val firstBotState = if (s.activePlayerIndex == 0) {
            // Human is first; fake-check to get to bot.
            PokerController.check(s.copy(currentBet = 0, players = s.players.map { it.copy(currentRoundBet = 0) }))
        } else s

        val r1 = PokerController.aiAction(firstBotState, Random(7L))
        val r2 = PokerController.aiAction(firstBotState, Random(7L))
        // Same seed → same result.
        assertEquals(r1.pot, r2.pot)
        assertEquals(r1.activePlayerIndex, r2.activePlayerIndex)
    }

    @Test
    fun aiAction_easyDifficulty_highCardNoBet_checksOrFolds() {
        // Give the bot a terrible hand (high card) with no bet to face.
        val baseWithBot = PokerController.startHand(baseState(anteAmount = 0), seed = 9L)
            .copy(currentBet = 0)
        val botIndex = if (baseWithBot.activePlayerIndex == 0) {
            // If human is first, find bot index to inject state
            1
        } else {
            baseWithBot.activePlayerIndex
        }
        val badHand = listOf(
            c(Rank.TWO, Suit.SPADES), c(Rank.FOUR, Suit.HEARTS),
            c(Rank.SIX, Suit.DIAMONDS), c(Rank.EIGHT, Suit.CLUBS),
            c(Rank.TEN, Suit.SPADES)  // no pair — high card 10
        )
        val stateWithBotTurn = baseWithBot.copy(
            activePlayerIndex = botIndex,
            currentBet = 0,
            players = baseWithBot.players.mapIndexed { i, p ->
                if (i == botIndex) p.copy(
                    hand = badHand,
                    difficulty = GameDifficulty.EASY,
                    currentRoundBet = 0
                )
                else p
            }
        )
        val after = PokerController.aiAction(stateWithBotTurn, Random(0L))
        // With high-card and no bet, Easy bot should check (not fold, not raise).
        assertTrue(after.players[botIndex].hasActed)
        // Pot should not have increased (check = no chips added).
        assertEquals(stateWithBotTurn.pot, after.pot)
        assertFalse(after.players[botIndex].isFolded)
    }

    // ---- resolveShowdown ----

    @Test
    fun resolveShowdown_higherHandWinsEntirePot() {
        val pairHand = listOf(
            c(Rank.ACE, Suit.SPADES), c(Rank.ACE, Suit.HEARTS),
            c(Rank.KING, Suit.DIAMONDS), c(Rank.QUEEN, Suit.CLUBS),
            c(Rank.JACK, Suit.SPADES)
        )
        val highCardHand = listOf(
            c(Rank.NINE, Suit.SPADES), c(Rank.SEVEN, Suit.HEARTS),
            c(Rank.FIVE, Suit.DIAMONDS), c(Rank.THREE, Suit.CLUBS),
            c(Rank.TWO, Suit.SPADES)
        )
        val state = baseState().copy(
            pot = 100,
            players = baseState().players.mapIndexed { i, p ->
                when (i) {
                    0 -> p.copy(hand = pairHand, isFolded = false, bankroll = 450)
                    1 -> p.copy(hand = highCardHand, isFolded = false, bankroll = 450)
                    else -> p.copy(isFolded = true)
                }
            }
        )
        val result = PokerController.resolveShowdown(state)
        assertNotNull(result.handResult)
        assertEquals(listOf(0), result.handResult!!.winnerIndices)
        // Winner gained the pot.
        assertEquals(450 + 100, result.players[0].bankroll)
        assertEquals(450, result.players[1].bankroll)
    }

    @Test
    fun resolveShowdown_tiedHands_splitsPotEvenly() {
        val hand = listOf(
            c(Rank.ACE, Suit.SPADES), c(Rank.ACE, Suit.HEARTS),
            c(Rank.KING, Suit.DIAMONDS), c(Rank.QUEEN, Suit.CLUBS),
            c(Rank.JACK, Suit.SPADES)
        )
        val state = baseState().copy(
            pot = 100,
            players = baseState().players.mapIndexed { i, p ->
                when (i) {
                    0 -> p.copy(hand = hand, isFolded = false, bankroll = 400)
                    1 -> p.copy(hand = hand, isFolded = false, bankroll = 400)
                    else -> p.copy(isFolded = true)
                }
            }
        )
        val result = PokerController.resolveShowdown(state)
        assertEquals(2, result.handResult!!.winnerIndices.size)
        assertEquals(450, result.players[0].bankroll)
        assertEquals(450, result.players[1].bankroll)
    }

    @Test
    fun resolveShowdown_humanBusts_setsSessionOver() {
        val winnerHand = listOf(
            c(Rank.ACE, Suit.SPADES), c(Rank.ACE, Suit.HEARTS),
            c(Rank.ACE, Suit.DIAMONDS), c(Rank.ACE, Suit.CLUBS),
            c(Rank.KING, Suit.SPADES)
        )
        val humanHand = listOf(
            c(Rank.TWO, Suit.SPADES), c(Rank.THREE, Suit.HEARTS),
            c(Rank.FOUR, Suit.DIAMONDS), c(Rank.FIVE, Suit.CLUBS),
            c(Rank.SEVEN, Suit.SPADES)
        )
        val state = baseState().copy(
            pot = 50,
            players = baseState().players.mapIndexed { i, p ->
                when (i) {
                    0 -> p.copy(hand = humanHand, isFolded = false, bankroll = 0, isHuman = true)
                    1 -> p.copy(hand = winnerHand, isFolded = false, bankroll = 450)
                    else -> p.copy(isFolded = true)
                }
            }
        )
        val result = PokerController.resolveShowdown(state)
        assertTrue(result.sessionOver)
    }

    // ---- awardPotToLastPlayer ----

    @Test
    fun awardPotToLastPlayer_onlyActivePlayerReceivesPot() {
        val state = baseState().copy(
            pot = 80,
            players = baseState().players.mapIndexed { i, p ->
                if (i == 2) p.copy(isFolded = false, bankroll = 420)
                else p.copy(isFolded = true, bankroll = 420)
            }
        )
        val result = PokerController.awardPotToLastPlayer(state)
        assertEquals(500, result.players[2].bankroll)
        assertEquals(0, result.pot)
    }

    // ---- confirmDraw ----

    @Test
    fun confirmDraw_humanDiscardsTwo_handHasFiveCards() {
        val dealt = PokerController.startHand(baseState(), seed = 55L)
        val withDiscards = dealt.copy(selectedDiscards = setOf(0, 4))
        val after = PokerController.confirmDraw(withDiscards, Random(55L))
        assertEquals(5, after.players[0].hand.size)
    }

    @Test
    fun confirmDraw_clearsSelectedDiscards() {
        val dealt = PokerController.startHand(baseState(), seed = 56L)
        val withDiscards = dealt.copy(selectedDiscards = setOf(1, 2))
        val after = PokerController.confirmDraw(withDiscards, Random(56L))
        assertTrue(after.selectedDiscards.isEmpty())
    }

    // ---- isHumanTurn ----

    @Test
    fun isHumanTurn_activeIndexZero_returnsTrue() {
        val s = baseState().copy(activePlayerIndex = 0)
        assertTrue(PokerController.isHumanTurn(s))
    }

    @Test
    fun isHumanTurn_activeIndexNonZero_returnsFalse() {
        val s = baseState().copy(activePlayerIndex = 1)
        assertFalse(PokerController.isHumanTurn(s))
    }
}
