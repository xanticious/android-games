package com.xanticious.androidgames.controller.games.spades

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.Decks
import com.xanticious.androidgames.model.games.cards.deal
import com.xanticious.androidgames.model.games.cards.Rank
import com.xanticious.androidgames.model.games.cards.Suit
import com.xanticious.androidgames.model.games.spades.NilResult
import com.xanticious.androidgames.model.games.spades.SpadesBid
import com.xanticious.androidgames.model.games.spades.SpadesGameState
import com.xanticious.androidgames.model.games.spades.SpadesHandResult
import com.xanticious.androidgames.model.games.spades.SpadesPlayer
import com.xanticious.androidgames.model.games.spades.SpadesTeam
import com.xanticious.androidgames.model.games.spades.SpadesTeamScore
import com.xanticious.androidgames.model.games.spades.SpadesTrick
import com.xanticious.androidgames.model.games.spades.SpadesTrickCard
import kotlin.random.Random

/**
 * Pure Spades rules engine.
 *
 * Every function is a pure transformation: model in → model/value out.
 * No Android, Compose, coroutines, or I/O. Randomness is injected via
 * [kotlin.random.Random] so AI decisions are fully deterministic under a
 * fixed seed (required for unit tests).
 */
object SpadesController {

    // ─────────────────────────── SETUP ───────────────────────────

    /**
     * Create a fresh game state (round 1, zero scores) and deal the first hand.
     */
    fun initGame(
        random: Random,
        gameEndScore: Int = 500,
        nilAllowed: Boolean = true,
        bagPenaltyEnabled: Boolean = true
    ): SpadesGameState = dealNewHand(
        SpadesGameState(
            gameEndScore      = gameEndScore,
            nilAllowed        = nilAllowed,
            bagPenaltyEnabled = bagPenaltyEnabled
        ),
        random
    )

    /**
     * Shuffle the deck, deal 13 cards to each player, and reset all
     * within-hand state while preserving cumulative scores and round number.
     *
     * SOUTH's hand is sorted (clubs → diamonds → hearts → spades, high-to-low
     * within suit) for readability; AI hands are left in deal order.
     */
    fun dealNewHand(state: SpadesGameState, random: Random): SpadesGameState {
        val deck           = Decks.shuffled(random)
        val (south, rest1) = deck.deal(13)
        val (west,  rest2) = rest1.deal(13)
        val (north, rest3) = rest2.deal(13)
        val (east,  _)     = rest3.deal(13)
        return state.copy(
            hands = mapOf(
                SpadesPlayer.SOUTH to south.sortedWith(handSortOrder),
                SpadesPlayer.WEST  to west,
                SpadesPlayer.NORTH to north,
                SpadesPlayer.EAST  to east
            ),
            bids            = emptyMap(),
            currentTrick    = null,
            completedTricks = emptyList(),
            tricksWon       = SpadesPlayer.entries.associateWith { 0 },
            spadesBroken    = false,
            currentBidder   = SpadesPlayer.SOUTH,
            lastHandResult  = null
        )
    }

    /** Display order for SOUTH's hand: clubs, diamonds, hearts, spades; high-to-low within suit. */
    private val handSortOrder: Comparator<Card> =
        compareBy<Card> {
            when (it.suit) {
                Suit.CLUBS    -> 0
                Suit.DIAMONDS -> 1
                Suit.HEARTS   -> 2
                Suit.SPADES   -> 3
            }
        }.thenByDescending { it.rank.highValue }

    // ─────────────────────────── BIDDING ───────────────────────────

    /**
     * Record [bid] for [player] and advance [SpadesGameState.currentBidder] to the
     * next seat (or null when all four have bid).
     */
    fun placeBid(
        state: SpadesGameState,
        player: SpadesPlayer,
        bid: SpadesBid
    ): SpadesGameState {
        val newBids     = state.bids + (player to bid)
        val nextBidder  = if (newBids.size < 4) player.next else null
        return state.copy(bids = newBids, currentBidder = nextBidder)
    }

    fun isAllBidsIn(state: SpadesGameState): Boolean = state.bids.size == 4

    /**
     * Set [SpadesGameState.currentTrick] to an empty trick led by SOUTH,
     * starting the play phase.
     */
    fun startPlayPhase(state: SpadesGameState): SpadesGameState =
        state.copy(currentTrick = SpadesTrick(leadPlayer = SpadesPlayer.SOUTH))

    // ─────────────────────────── LEGAL PLAYS ───────────────────────────

    /**
     * Return the subset of [hand] cards that may legally be played.
     *
     * Leading rules:
     * - Spades may NOT be led until "broken" (a spade played as an off-suit
     *   discard), unless only spades remain.
     *
     * Following rules:
     * - Must follow the led suit if possible.
     * - If void in the led suit, any card is legal (including spades).
     */
    fun legalPlays(
        hand: List<Card>,
        trick: SpadesTrick?,
        spadesBroken: Boolean
    ): List<Card> {
        if (hand.isEmpty()) return emptyList()
        if (trick == null || trick.plays.isEmpty()) {
            // Leading: block spades unless broken or hand is all spades
            return if (!spadesBroken && hand.any { it.suit != Suit.SPADES }) {
                hand.filter { it.suit != Suit.SPADES }
            } else {
                hand
            }
        }
        val ledSuit = trick.ledSuit ?: return hand
        val suited  = hand.filter { it.suit == ledSuit }
        return if (suited.isNotEmpty()) suited else hand
    }

    // ─────────────────────────── CARD PLAY ───────────────────────────

    /**
     * Add [card] to the current trick for [player], remove it from their hand,
     * and update [SpadesGameState.spadesBroken] when a spade is played as a
     * non-led-suit discard.
     */
    fun playCard(
        state: SpadesGameState,
        player: SpadesPlayer,
        card: Card
    ): SpadesGameState {
        val trick   = state.currentTrick ?: return state
        val newHand = (state.hands[player] ?: emptyList()) - card
        val newTrick = trick.copy(plays = trick.plays + SpadesTrickCard(player, card))
        // A spade played while following a non-spade suit breaks spades.
        val newSpadesBroken = state.spadesBroken ||
            (card.suit == Suit.SPADES &&
                trick.ledSuit != null &&
                trick.ledSuit != Suit.SPADES)
        return state.copy(
            hands        = state.hands + (player to newHand),
            currentTrick = newTrick,
            spadesBroken = newSpadesBroken
        )
    }

    /**
     * Determine which player wins a completed trick.
     * Spades always trump; within a suit the highest rank (Ace high) wins.
     */
    fun trickWinner(trick: SpadesTrick): SpadesPlayer {
        val ledSuit = trick.plays.first().card.suit
        return trick.plays
            .maxWithOrNull(compareBy { cardStrength(it.card, ledSuit) })!!
            .player
    }

    /**
     * Resolve the completed trick: credit the winner, archive the trick, and
     * start a fresh empty trick led by the winner (or set [currentTrick] to
     * null when this was the 13th trick, signalling hand completion).
     */
    fun resolveTrick(state: SpadesGameState): SpadesGameState {
        val trick = state.currentTrick ?: return state
        if (!trick.isComplete) return state
        val winner       = trickWinner(trick)
        val newTricksWon = state.tricksWon + (winner to (state.tricksWon[winner] ?: 0) + 1)
        val newCompleted = state.completedTricks + trick
        val totalTricks  = newTricksWon.values.sum()
        val nextTrick    = if (totalTricks < 13) SpadesTrick(leadPlayer = winner) else null
        return state.copy(
            tricksWon       = newTricksWon,
            completedTricks = newCompleted,
            currentTrick    = nextTrick
        )
    }

    /** True once 13 tricks have been played and won. */
    fun isHandComplete(state: SpadesGameState): Boolean =
        state.tricksWon.values.sum() == 13

    // ─────────────────────────── SCORING ───────────────────────────

    /**
     * Score the completed hand:
     * - Each team's tricks-won vs non-nil bid determines the contract result.
     * - Each Nil bidder earns ±100 independently of the team contract.
     * - Overtricks accumulate as bags; every 10 bags costs 100 points (if enabled).
     *
     * Returns an updated state with new [SpadesGameState.teamScores],
     * [SpadesGameState.lastHandResult], [SpadesGameState.round] + 1, and
     * [SpadesGameState.winner] if the game has ended.
     */
    fun scoreHand(state: SpadesGameState): SpadesGameState {
        val nilResults   = mutableListOf<NilResult>()
        val teamDeltas   = mutableMapOf(SpadesTeam.US to 0, SpadesTeam.THEM to 0)
        val teamBagDeltas = mutableMapOf(SpadesTeam.US to 0, SpadesTeam.THEM to 0)

        for (team in SpadesTeam.entries) {
            val players    = SpadesPlayer.entries.filter { it.team == team }
            val teamTricks = players.sumOf { state.tricksWon[it] ?: 0 }
            val nilPlayers = players.filter { state.bids[it]?.isNil == true }
            val nonNilBid  = players
                .filterNot { state.bids[it]?.isNil == true }
                .sumOf { state.bids[it]?.amount ?: 0 }

            // Nil bonuses / penalties are independent of the contract.
            for (nilPlayer in nilPlayers) {
                val tricks  = state.tricksWon[nilPlayer] ?: 0
                val success = tricks == 0
                nilResults += NilResult(nilPlayer, success)
                teamDeltas[team] = teamDeltas.getValue(team) + if (success) 100 else -100
            }

            // Contract scoring: make the bid → 10 per bid trick + 1 per overtrick (bag).
            val contractDelta = if (teamTricks >= nonNilBid) {
                val bags = teamTricks - nonNilBid
                teamBagDeltas[team] = bags
                10 * nonNilBid + bags
            } else {
                -10 * nonNilBid
            }
            teamDeltas[team] = teamDeltas.getValue(team) + contractDelta
        }

        val newTeamScores = state.teamScores.mapValues { (team, score) ->
            val delta    = teamDeltas.getValue(team)
            val bagDelta = teamBagDeltas.getOrDefault(team, 0)
            var newBags  = score.bags + bagDelta
            var newScore = score.score + delta
            // 10-bag penalty (if enabled): -100, bags reset modulo 10.
            if (state.bagPenaltyEnabled && newBags >= 10) {
                newScore -= 100
                newBags  -= 10
            }
            SpadesTeamScore(score = newScore, bags = newBags)
        }

        val handResult = SpadesHandResult(
            usScoreDelta   = teamDeltas.getValue(SpadesTeam.US),
            themScoreDelta = teamDeltas.getValue(SpadesTeam.THEM),
            nilResults     = nilResults
        )

        val winner = determineWinner(newTeamScores, state.gameEndScore)
        return state.copy(
            teamScores     = newTeamScores,
            lastHandResult = handResult,
            winner         = winner,
            round          = state.round + 1
        )
    }

    fun isGameOver(state: SpadesGameState): Boolean = state.winner != null

    private fun determineWinner(
        scores: Map<SpadesTeam, SpadesTeamScore>,
        target: Int
    ): SpadesTeam? {
        val us   = scores[SpadesTeam.US]?.score   ?: 0
        val them = scores[SpadesTeam.THEM]?.score ?: 0
        return when {
            // Both cross the line on the same hand → higher score wins.
            us >= target && them >= target -> if (us >= them) SpadesTeam.US else SpadesTeam.THEM
            us   >= target                -> SpadesTeam.US
            them >= target                -> SpadesTeam.THEM
            else                          -> null
        }
    }

    /**
     * Spades beat every other suit; within a suit the highest rank (Ace = 14)
     * wins; off-suit non-spade cards score 0 and cannot win.
     */
    private fun cardStrength(card: Card, ledSuit: Suit): Int = when {
        card.suit == Suit.SPADES -> 100 + card.rank.highValue
        card.suit == ledSuit     -> card.rank.highValue
        else                     -> 0
    }

    // ─────────────────────────── AI — BIDDING ───────────────────────────

    /**
     * Return an AI bid for [player]. Deterministic given [random] for
     * reproducible unit tests. [existingBids] lets Hard difficulty account for
     * partner's already-declared bid.
     */
    fun aiBid(
        player: SpadesPlayer,
        hand: List<Card>,
        existingBids: Map<SpadesPlayer, SpadesBid>,
        nilAllowed: Boolean,
        difficulty: GameDifficulty,
        random: Random
    ): SpadesBid {
        val amount = when (difficulty) {
            GameDifficulty.EASY   -> easyBidAmount(hand, random)
            GameDifficulty.MEDIUM -> mediumBidAmount(hand, random)
            GameDifficulty.HARD   -> hardBidAmount(player, hand, existingBids, random)
        }
        if (nilAllowed && amount == 0) return SpadesBid(0, isNil = true)
        return SpadesBid(amount.coerceIn(1, 13))
    }

    /** Count aces and kings; add a small random adjustment (0–2). */
    private fun easyBidAmount(hand: List<Card>, random: Random): Int =
        (hand.count { it.rank == Rank.ACE || it.rank == Rank.KING } +
            random.nextInt(0, 3)).coerceIn(0, 13)

    /**
     * Count high cards and trump winners; weight long side-suits;
     * add a small ±1 jitter.
     */
    private fun mediumBidAmount(hand: List<Card>, random: Random): Int {
        var est = hand.count { it.rank == Rank.ACE }
        est    += hand.count { it.rank == Rank.KING }
        est    += hand.count { it.suit == Suit.SPADES && it.rank.highValue >= Rank.QUEEN.highValue }
        Suit.entries.forEach { s -> if (hand.count { it.suit == s } >= 5) est++ }
        return (est + random.nextInt(-1, 2)).coerceIn(0, 13)
    }

    /**
     * Full heuristic: count expected winners, add void-suit trumping potential,
     * and add a 0–1 jitter.
     */
    private fun hardBidAmount(
        player: SpadesPlayer,
        hand: List<Card>,
        existingBids: Map<SpadesPlayer, SpadesBid>,
        random: Random
    ): Int {
        var est = hand.count { it.rank == Rank.ACE }
        est    += hand.count { it.rank == Rank.KING }
        est    += hand.filter { it.suit == Suit.SPADES }
                     .count { it.rank.highValue >= Rank.QUEEN.highValue }
        // Each void in a non-spade suit is a likely trumping opportunity.
        Suit.entries.filter { it != Suit.SPADES }.forEach { s ->
            if (hand.none { it.suit == s }) est++
        }
        @Suppress("UNUSED_VARIABLE")
        val partnerBid = existingBids[player.partner]?.amount ?: 0  // available for future tuning
        return (est + random.nextInt(0, 2)).coerceIn(1, 13)
    }

    // ─────────────────────────── AI — CARD PLAY ───────────────────────────

    /**
     * Choose a card for the AI [player] to play. Always returns a card from
     * [SpadesGameState.legalPlays]. Deterministic under a fixed [random].
     */
    fun aiPlay(
        player: SpadesPlayer,
        state: SpadesGameState,
        difficulty: GameDifficulty,
        random: Random
    ): Card {
        val hand  = state.hands[player] ?: return state.hands.values.flatten().first()
        val trick = state.currentTrick  ?: return hand.first()
        val legal = legalPlays(hand, trick, state.spadesBroken)
        if (legal.isEmpty()) return hand.first()
        return when (difficulty) {
            GameDifficulty.EASY   -> legal[random.nextInt(legal.size)]
            GameDifficulty.MEDIUM -> mediumPlay(player, legal, trick)
            GameDifficulty.HARD   -> hardPlay(player, legal, trick, state)
        }
    }

    /**
     * Medium AI: lead high non-spade; when following, duck if partner is
     * currently winning, otherwise play the lowest card that beats the current
     * best.
     */
    private fun mediumPlay(
        player: SpadesPlayer,
        legal: List<Card>,
        trick: SpadesTrick
    ): Card {
        if (trick.plays.isEmpty()) {
            // Leading: prefer highest non-spade; fall back to lowest spade.
            return legal.filter { it.suit != Suit.SPADES }
                .maxByOrNull { it.rank.highValue }
                ?: legal.minByOrNull { it.rank.highValue }!!
        }
        val ledSuit = trick.ledSuit!!
        return if (isPartnerCurrentlyWinning(player, trick)) {
            // Partner is ahead — play lowest-strength card to avoid over-trumping.
            legal.minByOrNull { cardStrength(it, ledSuit) }!!
        } else {
            // Try to win with the cheapest winning card; otherwise dump lowest.
            legal.filter { canBeatCurrentBest(it, trick) }
                .minByOrNull { it.rank.highValue }
                ?: legal.minByOrNull { cardStrength(it, ledSuit) }!!
        }
    }

    /**
     * Hard AI: as Medium but additionally avoids over-helping a partner whose
     * Nil bid has already failed, and prefers protecting its own Nil (not used
     * here — if this seat is AI and bid Nil, the bug must be in the caller).
     */
    private fun hardPlay(
        player: SpadesPlayer,
        legal: List<Card>,
        trick: SpadesTrick,
        state: SpadesGameState
    ): Card {
        if (trick.plays.isEmpty()) {
            return legal.filter { it.suit != Suit.SPADES }
                .maxByOrNull { it.rank.highValue }
                ?: legal.maxByOrNull { it.rank.highValue }!!
        }
        val ledSuit         = trick.ledSuit!!
        val partner         = player.partner
        val partnerBid      = state.bids[partner]
        val partnerTricks   = state.tricksWon[partner] ?: 0
        // If partner bid Nil and already took tricks, their Nil has failed;
        // don't bother ducking — just try to win.
        val partnerNilFailed = partnerBid?.isNil == true && partnerTricks > 0

        return if (isPartnerCurrentlyWinning(player, trick) && !partnerNilFailed) {
            legal.minByOrNull { cardStrength(it, ledSuit) }!!
        } else {
            legal.filter { canBeatCurrentBest(it, trick) }
                .minByOrNull { it.rank.highValue }
                ?: legal.minByOrNull { cardStrength(it, ledSuit) }!!
        }
    }

    private fun isPartnerCurrentlyWinning(player: SpadesPlayer, trick: SpadesTrick): Boolean {
        if (trick.plays.isEmpty()) return false
        val ledSuit = trick.plays.first().card.suit
        val best    = trick.plays.maxWithOrNull(compareBy { cardStrength(it.card, ledSuit) })
            ?: return false
        return best.player == player.partner
    }

    private fun canBeatCurrentBest(card: Card, trick: SpadesTrick): Boolean {
        val ledSuit     = trick.plays.firstOrNull()?.card?.suit ?: return true
        val currentBest = trick.plays.maxOfOrNull { cardStrength(it.card, ledSuit) } ?: 0
        return cardStrength(card, ledSuit) > currentBest
    }
}
