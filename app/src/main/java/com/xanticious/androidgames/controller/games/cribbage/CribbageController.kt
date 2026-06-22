package com.xanticious.androidgames.controller.games.cribbage

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.Decks
import com.xanticious.androidgames.model.games.cards.Rank
import com.xanticious.androidgames.model.games.cards.Suit
import com.xanticious.androidgames.model.games.cards.deal
import com.xanticious.androidgames.model.games.cribbage.CribbagePhase
import com.xanticious.androidgames.model.games.cribbage.CribbagePlayer
import com.xanticious.androidgames.model.games.cribbage.CribbageState
import com.xanticious.androidgames.model.games.cribbage.PeggingScore
import com.xanticious.androidgames.model.games.cribbage.PlayedCard
import kotlin.random.Random

/**
 * Pure controller for Cribbage. All functions are stateless: they receive the
 * current [CribbageState] and return a new [CribbageState]. No Android imports,
 * no side effects — randomness is injected via [Random].
 */
object CribbageController {

    // -------------------------------------------------------------------------
    // Card value helpers
    // -------------------------------------------------------------------------

    /** Cribbage value of a card for counting fifteens / running totals (J/Q/K = 10). */
    fun cardValue(card: Card): Int = card.rank.value.coerceAtMost(10)

    /** Ordinal for run detection (A=1 … K=13). */
    private fun runRank(card: Card): Int = card.rank.value

    // -------------------------------------------------------------------------
    // Deal
    // -------------------------------------------------------------------------

    /** Deals 6 cards to each player from a fresh shuffled deck. AI discards immediately. */
    fun deal(state: CribbageState, seed: Long): CribbageState {
        val deck = Decks.shuffled(seed)
        val (humanCards, rest1) = deck.deal(6)
        val (aiCards, _) = rest1.deal(6)
        return state.copy(
            humanHand = humanCards,
            aiHand = aiCards,
            crib = emptyList(),
            starter = null,
            playPile = emptyList(),
            pegCount = 0,
            humanPlayHand = emptyList(),
            aiPlayHand = emptyList(),
            humanSaidGo = false,
            aiSaidGo = false,
            humanDiscardSelection = emptyList(),
            lastScoreBreakdown = "",
            lastScorePoints = 0,
            showBreakdown = emptyList(),
            phase = CribbagePhase.DISCARDING
        )
    }

    /** Deals using an injected Random (testable). */
    fun deal(state: CribbageState, random: Random): CribbageState {
        val deck = Decks.shuffled(random)
        val (humanCards, rest1) = deck.deal(6)
        val (aiCards, _) = rest1.deal(6)
        return state.copy(
            humanHand = humanCards,
            aiHand = aiCards,
            crib = emptyList(),
            starter = null,
            playPile = emptyList(),
            pegCount = 0,
            humanPlayHand = emptyList(),
            aiPlayHand = emptyList(),
            humanSaidGo = false,
            aiSaidGo = false,
            humanDiscardSelection = emptyList(),
            lastScoreBreakdown = "",
            lastScorePoints = 0,
            showBreakdown = emptyList(),
            phase = CribbagePhase.DISCARDING
        )
    }

    // -------------------------------------------------------------------------
    // Discard
    // -------------------------------------------------------------------------

    /** Human toggles a card selection for discard (must select exactly 2). */
    fun toggleHumanDiscard(state: CribbageState, cardIndex: Int): CribbageState {
        val sel = state.humanDiscardSelection.toMutableList()
        if (sel.contains(cardIndex)) sel.remove(cardIndex)
        else if (sel.size < 2) sel.add(cardIndex)
        return state.copy(humanDiscardSelection = sel)
    }

    /**
     * Finalizes the discard step: human sends selected 2 to crib, AI picks its
     * 2 best cards to discard using difficulty heuristics.
     */
    fun confirmDiscard(state: CribbageState, difficulty: GameDifficulty, random: Random): CribbageState {
        require(state.humanDiscardSelection.size == 2) { "Must select exactly 2 cards to discard" }
        val humanDiscard = state.humanDiscardSelection.map { state.humanHand[it] }
        val humanKept = state.humanHand.filterIndexed { i, _ -> i !in state.humanDiscardSelection }

        val dealerOwnsCrib = state.dealer == CribbagePlayer.AI
        val (aiDiscard, aiKept) = aiChooseDiscard(state.aiHand, difficulty, dealerOwnsCrib, random)

        return state.copy(
            humanHand = humanKept,
            aiHand = aiKept,
            crib = humanDiscard + aiDiscard,
            humanDiscardSelection = emptyList(),
            phase = CribbagePhase.CUTTING
        )
    }

    // -------------------------------------------------------------------------
    // Cut / Starter
    // -------------------------------------------------------------------------

    /**
     * Cuts the starter card from the remaining deck (after dealing 6+6=12 cards).
     * Returns updated state: if starter is a Jack dealer scores 2 (His Heels).
     */
    fun cut(state: CribbageState, seed: Long): CribbageState = cut(state, Random(seed))

    fun cut(state: CribbageState, random: Random): CribbageState {
        // Pick any card that's not in hands or crib
        val usedCards = (state.humanHand + state.aiHand + state.crib).toSet()
        val remaining = Decks.standard52.filter { it !in usedCards }.shuffled(random)
        val starter = remaining.first()
        val heels = starter.rank == Rank.JACK
        val dealerBonus = if (heels) 2 else 0
        val (newHumanScore, newAiScore) = if (heels) {
            if (state.dealer == CribbagePlayer.HUMAN)
                state.humanScore + 2 to state.aiScore
            else
                state.humanScore to state.aiScore + 2
        } else {
            state.humanScore to state.aiScore
        }
        // Check for immediate win
        if (newHumanScore >= state.targetScore || newAiScore >= state.targetScore) {
            val winner = if (newHumanScore >= state.targetScore) CribbagePlayer.HUMAN else CribbagePlayer.AI
            return state.copy(
                starter = starter,
                humanScore = newHumanScore,
                aiScore = newAiScore,
                lastScoreBreakdown = if (heels) "His Heels! Dealer scores 2." else "",
                lastScorePoints = dealerBonus,
                winner = winner,
                phase = CribbagePhase.GAME_OVER
            )
        }
        val nonDealer = if (state.dealer == CribbagePlayer.HUMAN) CribbagePlayer.AI else CribbagePlayer.HUMAN
        return state.copy(
            starter = starter,
            humanScore = newHumanScore,
            aiScore = newAiScore,
            humanPlayHand = state.humanHand.toList(),
            aiPlayHand = state.aiHand.toList(),
            pegTurn = nonDealer,
            lastScoreBreakdown = if (heels) "His Heels! Dealer scores 2." else "",
            lastScorePoints = dealerBonus,
            phase = CribbagePhase.PLAYING
        )
    }

    // -------------------------------------------------------------------------
    // Pegging (Play phase)
    // -------------------------------------------------------------------------

    /**
     * Returns the legal cards the given player can play (don't exceed 31).
     */
    fun legalPeggingCards(state: CribbageState, player: CribbagePlayer): List<Card> {
        val hand = if (player == CribbagePlayer.HUMAN) state.humanPlayHand else state.aiPlayHand
        return hand.filter { cardValue(it) + state.pegCount <= 31 }
    }

    /**
     * Human plays a card during pegging. Returns updated state with peg scores applied.
     * Pass null to declare "Go".
     */
    fun humanPlayCard(state: CribbageState, card: Card?): CribbageState {
        require(state.pegTurn == CribbagePlayer.HUMAN) { "Not human's turn" }
        if (card == null) return declareGo(state, CribbagePlayer.HUMAN)
        require(card in state.humanPlayHand) { "Card not in human play hand" }
        require(cardValue(card) + state.pegCount <= 31) { "Card would exceed 31" }
        return playCard(state, card, CribbagePlayer.HUMAN)
    }

    /**
     * AI plays its best card during pegging, or declares Go if unable.
     */
    fun aiPlayCard(state: CribbageState, difficulty: GameDifficulty, random: Random): CribbageState {
        require(state.pegTurn == CribbagePlayer.AI) { "Not AI's turn" }
        val legal = legalPeggingCards(state, CribbagePlayer.AI)
        if (legal.isEmpty()) return declareGo(state, CribbagePlayer.AI)
        val chosen = aiChoosePeggingCard(legal, state, difficulty, random)
        return playCard(state, chosen, CribbagePlayer.AI)
    }

    private fun playCard(state: CribbageState, card: Card, player: CribbagePlayer): CribbageState {
        val newCount = state.pegCount + cardValue(card)
        val newPlayPile = state.playPile + PlayedCard(card, player, newCount)
        val newHumanHand = if (player == CribbagePlayer.HUMAN) state.humanPlayHand - card else state.humanPlayHand
        val newAiHand = if (player == CribbagePlayer.AI) state.aiPlayHand - card else state.aiPlayHand

        val scores = scorePeggingPlay(newPlayPile, newCount)
        val points = scores.sumOf { it.points }
        val breakdown = scores.joinToString("; ") { it.description }

        var humanScore = state.humanScore
        var aiScore = state.aiScore
        if (player == CribbagePlayer.HUMAN) humanScore += points else aiScore += points

        // Check for immediate win
        if (humanScore >= state.targetScore || aiScore >= state.targetScore) {
            val winner = if (humanScore >= state.targetScore) CribbagePlayer.HUMAN else CribbagePlayer.AI
            return state.copy(
                humanPlayHand = newHumanHand,
                aiPlayHand = newAiHand,
                playPile = newPlayPile,
                pegCount = newCount,
                humanScore = humanScore,
                aiScore = aiScore,
                lastScoreBreakdown = breakdown,
                lastScorePoints = points,
                winner = winner,
                phase = CribbagePhase.GAME_OVER
            )
        }

        val nextState = state.copy(
            humanPlayHand = newHumanHand,
            aiPlayHand = newAiHand,
            playPile = newPlayPile,
            pegCount = newCount,
            humanScore = humanScore,
            aiScore = aiScore,
            humanSaidGo = false, // reset — new card was played
            aiSaidGo = false,
            lastScoreBreakdown = breakdown,
            lastScorePoints = points
        )

        // Handle 31
        if (newCount == 31) {
            return resetPeggingRound(nextState, player)
        }

        // Advance turn
        return advancePegTurn(nextState, player)
    }

    private fun declareGo(state: CribbageState, player: CribbagePlayer): CribbageState {
        val newState = if (player == CribbagePlayer.HUMAN)
            state.copy(humanSaidGo = true)
        else
            state.copy(aiSaidGo = true)

        val bothSaidGo = newState.humanSaidGo || newState.aiSaidGo
        val otherLegal = legalPeggingCards(
            newState,
            if (player == CribbagePlayer.HUMAN) CribbagePlayer.AI else CribbagePlayer.HUMAN
        )

        return if (otherLegal.isEmpty() || (newState.humanSaidGo && newState.aiSaidGo)) {
            // Both can't play — award 1 for go/last-card to whoever played last
            val lastPlayer = newState.playPile.lastOrNull()?.playedBy ?: player
            val goPoints = 1
            var humanScore = newState.humanScore
            var aiScore = newState.aiScore
            if (lastPlayer == CribbagePlayer.HUMAN) humanScore += goPoints else aiScore += goPoints

            if (humanScore >= state.targetScore || aiScore >= state.targetScore) {
                val winner = if (humanScore >= state.targetScore) CribbagePlayer.HUMAN else CribbagePlayer.AI
                return newState.copy(
                    humanScore = humanScore,
                    aiScore = aiScore,
                    lastScoreBreakdown = "Go — last card",
                    lastScorePoints = goPoints,
                    winner = winner,
                    phase = CribbagePhase.GAME_OVER
                )
            }
            resetPeggingRound(newState.copy(humanScore = humanScore, aiScore = aiScore, lastScoreBreakdown = "Go — last card", lastScorePoints = goPoints), lastPlayer)
        } else {
            // Other player still has legal cards — switch turn
            newState.copy(
                pegTurn = if (player == CribbagePlayer.HUMAN) CribbagePlayer.AI else CribbagePlayer.HUMAN,
                lastScoreBreakdown = "Go",
                lastScorePoints = 0
            )
        }
    }

    /**
     * Resets the peg pile/count for a new sub-round. Returns to SHOW if both hands empty.
     */
    private fun resetPeggingRound(state: CribbageState, lastPlayer: CribbagePlayer): CribbageState {
        val bothHandsEmpty = state.humanPlayHand.isEmpty() && state.aiPlayHand.isEmpty()
        if (bothHandsEmpty) {
            return beginShow(state)
        }
        // Non-dealer leads the next sub-round
        val nonDealer = if (state.dealer == CribbagePlayer.HUMAN) CribbagePlayer.AI else CribbagePlayer.HUMAN
        val nextToPlay = if (state.humanPlayHand.isEmpty()) CribbagePlayer.AI
        else if (state.aiPlayHand.isEmpty()) CribbagePlayer.HUMAN
        else nonDealer
        return state.copy(
            playPile = emptyList(),
            pegCount = 0,
            humanSaidGo = false,
            aiSaidGo = false,
            pegTurn = nextToPlay
        )
    }

    private fun advancePegTurn(state: CribbageState, justPlayed: CribbagePlayer): CribbageState {
        val next = if (justPlayed == CribbagePlayer.HUMAN) CribbagePlayer.AI else CribbagePlayer.HUMAN
        // Check if next player has legal cards
        val nextLegal = legalPeggingCards(state, next)
        return if (nextLegal.isEmpty() && legalPeggingCards(state, justPlayed).isEmpty()) {
            // Nobody can play — sub-round ends (award last-card 1 pt)
            val goPoints = 1
            var humanScore = state.humanScore
            var aiScore = state.aiScore
            if (justPlayed == CribbagePlayer.HUMAN) humanScore += goPoints else aiScore += goPoints
            resetPeggingRound(state.copy(humanScore = humanScore, aiScore = aiScore, lastScoreBreakdown = state.lastScoreBreakdown + " + Last card", lastScorePoints = state.lastScorePoints + goPoints), justPlayed)
        } else if (nextLegal.isEmpty()) {
            // Next can't play, stay on justPlayed (but mark them as needing to know other said go)
            state.copy(pegTurn = justPlayed)
        } else {
            state.copy(pegTurn = next)
        }
    }

    // -------------------------------------------------------------------------
    // Pegging scoring
    // -------------------------------------------------------------------------

    /**
     * Scores a pegging play based on the current pile. Returns list of score events.
     * Scores: 15 (2pts), 31 (2pts), pair/trips/quads, runs.
     */
    fun scorePeggingPlay(pile: List<PlayedCard>, count: Int): List<PeggingScore> {
        if (pile.isEmpty()) return emptyList()
        val scores = mutableListOf<PeggingScore>()

        // Fifteen
        if (count == 15) scores.add(PeggingScore(2, "Fifteen"))

        // Thirty-one
        if (count == 31) scores.add(PeggingScore(2, "Thirty-one"))

        // Pairs / trips / quads — count matching ranks from the top of the pile
        val topRank = pile.last().card.rank
        var matchCount = pile.takeLastWhile { it.card.rank == topRank }.size
        val pairScore = when (matchCount) {
            2 -> PeggingScore(2, "Pair")
            3 -> PeggingScore(6, "Pair royal")
            4 -> PeggingScore(12, "Double pair royal")
            else -> null
        }
        if (pairScore != null) scores.add(pairScore)

        // Run — longest run ending at the top of the pile
        val runLen = longestRunFromTop(pile)
        if (runLen >= 3) scores.add(PeggingScore(runLen, "Run of $runLen"))

        return scores
    }

    /**
     * Returns the length of the longest run (sequence of consecutive ranks,
     * any order) using the most recent cards in the pile.
     */
    fun longestRunFromTop(pile: List<PlayedCard>): Int {
        if (pile.size < 3) return 0
        for (len in pile.size downTo 3) {
            val slice = pile.takeLast(len).map { runRank(it.card) }.sorted()
            if (isConsecutive(slice)) return len
        }
        return 0
    }

    private fun isConsecutive(sorted: List<Int>): Boolean {
        if (sorted.isEmpty()) return false
        val distinct = sorted.distinct()
        if (distinct.size != sorted.size) return false // duplicate ranks break a run
        return distinct.last() - distinct.first() == distinct.size - 1
    }

    // -------------------------------------------------------------------------
    // Show scoring
    // -------------------------------------------------------------------------

    /**
     * Scores a 4-card hand combined with the starter.
     * Returns a list of scoring lines and the total.
     */
    data class ShowScore(val lines: List<String>, val total: Int)

    fun scoreShow(hand: List<Card>, starter: Card, isInCrib: Boolean): ShowScore {
        require(hand.size == 4) { "Hand must have exactly 4 cards, got ${hand.size}" }
        val all5 = hand + starter
        val lines = mutableListOf<String>()
        var total = 0

        // --- Fifteens ---
        val fifteenCombos = fifteenCombinations(all5)
        if (fifteenCombos > 0) {
            lines.add("Fifteens: $fifteenCombos × 2 = ${fifteenCombos * 2}")
            total += fifteenCombos * 2
        }

        // --- Pairs ---
        val pairs = countPairs(all5)
        if (pairs > 0) {
            val pts = pairs * 2
            val grouped = groupPairsByRank(all5)
            grouped.forEach { (rank, count) ->
                when (count) {
                    2 -> { lines.add("Pair ($rank): 2"); total += 2 }
                    3 -> { lines.add("Pair royal ($rank): 6"); total += 6 }
                    4 -> { lines.add("Double pair royal ($rank): 12"); total += 12 }
                }
            }
        }

        // --- Runs ---
        val runPts = scoreRuns(all5)
        if (runPts.first > 0) {
            lines.add(runPts.second)
            total += runPts.first
        }

        // --- Flush ---
        val flushPts = scoreFlush(hand, starter, isInCrib)
        if (flushPts > 0) {
            lines.add("Flush: $flushPts")
            total += flushPts
        }

        // --- His Nobs ---
        val nobs = hand.any { it.rank == Rank.JACK && it.suit == starter.suit }
        if (nobs) {
            lines.add("His Nobs: 1")
            total += 1
        }

        if (lines.isEmpty()) lines.add("No score")

        return ShowScore(lines, total)
    }

    /** Count how many combinations of cards from [cards] sum to exactly 15 (using cardValue). */
    fun fifteenCombinations(cards: List<Card>): Int {
        var count = 0
        val n = cards.size
        for (mask in 1 until (1 shl n)) {
            var sum = 0
            for (i in 0 until n) {
                if (mask and (1 shl i) != 0) sum += cardValue(cards[i])
            }
            if (sum == 15) count++
        }
        return count
    }

    private fun countPairs(cards: List<Card>): Int {
        var pairs = 0
        for (i in cards.indices) for (j in i + 1 until cards.size) {
            if (cards[i].rank == cards[j].rank) pairs++
        }
        return pairs
    }

    private fun groupPairsByRank(cards: List<Card>): Map<String, Int> {
        return cards.groupBy { it.rank.label }
            .filter { it.value.size >= 2 }
            .mapValues { it.value.size }
    }

    /**
     * Scores runs in a 5-card hand (hand + starter). Returns (points, description).
     * Handles double/triple runs (when a pair extends a run).
     */
    fun scoreRuns(cards: List<Card>): Pair<Int, String> {
        // Try runs of length 5, then 4, then 3
        for (len in 5 downTo 3) {
            val combos = combinations(cards, len)
            val runCombos = combos.filter { combo ->
                val ranks = combo.map { runRank(it) }.sorted()
                ranks.distinct().size == ranks.size && ranks.last() - ranks.first() == ranks.size - 1
            }
            if (runCombos.isNotEmpty()) {
                val pts = runCombos.size * len
                return pts to "Run(s) of $len × ${runCombos.size} = $pts"
            }
        }
        return 0 to ""
    }

    private fun <T> combinations(list: List<T>, k: Int): List<List<T>> {
        if (k == 0) return listOf(emptyList())
        if (list.isEmpty()) return emptyList()
        val head = list.first()
        val tail = list.drop(1)
        return combinations(tail, k - 1).map { listOf(head) + it } + combinations(tail, k)
    }

    /**
     * Scores flush. Hand flush: all 4 cards same suit = 4 pts (5 if starter matches).
     * Crib flush: only 5-card flush counts.
     */
    fun scoreFlush(hand: List<Card>, starter: Card, isInCrib: Boolean): Int {
        val handSuit = hand[0].suit
        val allHandSame = hand.all { it.suit == handSuit }
        if (!allHandSame) return 0
        return if (starter.suit == handSuit) {
            5
        } else if (isInCrib) {
            0 // crib needs 5
        } else {
            4
        }
    }

    // -------------------------------------------------------------------------
    // Show phase transitions
    // -------------------------------------------------------------------------

    private fun beginShow(state: CribbageState): CribbageState {
        val nonDealer = if (state.dealer == CribbagePlayer.HUMAN) CribbagePlayer.AI else CribbagePlayer.HUMAN
        val firstPhase = if (nonDealer == CribbagePlayer.HUMAN) CribbagePhase.SHOW_NON_DEALER else CribbagePhase.SHOW_NON_DEALER
        return state.copy(phase = firstPhase, lastScoreBreakdown = "", lastScorePoints = 0)
    }

    /** Called by UI/state machine to score the non-dealer's hand. */
    fun scoreNonDealerHand(state: CribbageState): CribbageState {
        val starter = state.starter ?: return state
        val nonDealer = if (state.dealer == CribbagePlayer.HUMAN) CribbagePlayer.AI else CribbagePlayer.HUMAN
        val hand = if (nonDealer == CribbagePlayer.HUMAN) state.humanHand else state.aiHand
        val result = scoreShow(hand, starter, isInCrib = false)
        var humanScore = state.humanScore
        var aiScore = state.aiScore
        if (nonDealer == CribbagePlayer.HUMAN) humanScore += result.total else aiScore += result.total

        val breakdown = result.lines.joinToString("\n")
        val label = if (nonDealer == CribbagePlayer.HUMAN) "Your hand" else "AI hand"

        if (humanScore >= state.targetScore || aiScore >= state.targetScore) {
            val winner = if (humanScore >= state.targetScore) CribbagePlayer.HUMAN else CribbagePlayer.AI
            return state.copy(
                humanScore = humanScore, aiScore = aiScore,
                lastScoreBreakdown = "$label: $breakdown\nTotal: ${result.total}",
                lastScorePoints = result.total,
                winner = winner, phase = CribbagePhase.GAME_OVER,
                showBreakdown = state.showBreakdown + "$label (${result.total}): $breakdown"
            )
        }
        return state.copy(
            humanScore = humanScore, aiScore = aiScore,
            lastScoreBreakdown = "$label: $breakdown\nTotal: ${result.total}",
            lastScorePoints = result.total,
            phase = CribbagePhase.SHOW_DEALER,
            showBreakdown = state.showBreakdown + "$label (${result.total}): $breakdown"
        )
    }

    /** Called by UI/state machine to score the dealer's hand. */
    fun scoreDealerHand(state: CribbageState): CribbageState {
        val starter = state.starter ?: return state
        val hand = if (state.dealer == CribbagePlayer.HUMAN) state.humanHand else state.aiHand
        val result = scoreShow(hand, starter, isInCrib = false)
        var humanScore = state.humanScore
        var aiScore = state.aiScore
        if (state.dealer == CribbagePlayer.HUMAN) humanScore += result.total else aiScore += result.total

        val breakdown = result.lines.joinToString("\n")
        val label = if (state.dealer == CribbagePlayer.HUMAN) "Your hand (dealer)" else "AI hand (dealer)"

        if (humanScore >= state.targetScore || aiScore >= state.targetScore) {
            val winner = if (humanScore >= state.targetScore) CribbagePlayer.HUMAN else CribbagePlayer.AI
            return state.copy(
                humanScore = humanScore, aiScore = aiScore,
                lastScoreBreakdown = "$label: $breakdown\nTotal: ${result.total}",
                lastScorePoints = result.total,
                winner = winner, phase = CribbagePhase.GAME_OVER,
                showBreakdown = state.showBreakdown + "$label (${result.total}): $breakdown"
            )
        }
        return state.copy(
            humanScore = humanScore, aiScore = aiScore,
            lastScoreBreakdown = "$label: $breakdown\nTotal: ${result.total}",
            lastScorePoints = result.total,
            phase = CribbagePhase.SHOW_CRIB,
            showBreakdown = state.showBreakdown + "$label (${result.total}): $breakdown"
        )
    }

    /** Called by UI/state machine to score the crib. */
    fun scoreCrib(state: CribbageState): CribbageState {
        val starter = state.starter ?: return state
        require(state.crib.size == 4) { "Crib must have 4 cards, has ${state.crib.size}" }
        val result = scoreShow(state.crib, starter, isInCrib = true)
        var humanScore = state.humanScore
        var aiScore = state.aiScore
        if (state.dealer == CribbagePlayer.HUMAN) humanScore += result.total else aiScore += result.total

        val breakdown = result.lines.joinToString("\n")
        val label = if (state.dealer == CribbagePlayer.HUMAN) "Your crib" else "AI crib"

        if (humanScore >= state.targetScore || aiScore >= state.targetScore) {
            val winner = if (humanScore >= state.targetScore) CribbagePlayer.HUMAN else CribbagePlayer.AI
            return state.copy(
                humanScore = humanScore, aiScore = aiScore,
                lastScoreBreakdown = "$label: $breakdown\nTotal: ${result.total}",
                lastScorePoints = result.total,
                winner = winner, phase = CribbagePhase.GAME_OVER,
                showBreakdown = state.showBreakdown + "$label (${result.total}): $breakdown"
            )
        }
        // Hand complete — swap dealer and go back to Dealing
        val newDealer = if (state.dealer == CribbagePlayer.HUMAN) CribbagePlayer.AI else CribbagePlayer.HUMAN
        return state.copy(
            humanScore = humanScore, aiScore = aiScore,
            dealer = newDealer,
            lastScoreBreakdown = "$label: $breakdown\nTotal: ${result.total}",
            lastScorePoints = result.total,
            phase = CribbagePhase.DEALING,
            showBreakdown = state.showBreakdown + "$label (${result.total}): $breakdown"
        )
    }

    // -------------------------------------------------------------------------
    // AI Discard
    // -------------------------------------------------------------------------

    /**
     * AI chooses 2 cards to discard to the crib.
     * [ownsCrib] = true when the AI is dealer (it benefits from the crib).
     * Returns (discardedCards, keptCards).
     */
    fun aiChooseDiscard(
        hand: List<Card>,
        difficulty: GameDifficulty,
        ownsCrib: Boolean,
        random: Random
    ): Pair<List<Card>, List<Card>> {
        require(hand.size == 6)
        return when (difficulty) {
            GameDifficulty.EASY -> aiDiscardEasy(hand, random)
            GameDifficulty.MEDIUM -> aiDiscardMedium(hand, ownsCrib, random)
            GameDifficulty.HARD -> aiDiscardHard(hand, ownsCrib, random)
        }
    }

    private fun aiDiscardEasy(hand: List<Card>, random: Random): Pair<List<Card>, List<Card>> {
        // Keep obvious pairs and fifteens; otherwise random
        val kept = findBestKeep4Easy(hand, random)
        val discarded = hand.filter { it !in kept }
        return discarded to kept
    }

    private fun findBestKeep4Easy(hand: List<Card>, random: Random): List<Card> {
        // Try to keep a pair if present
        for (rank in Rank.entries) {
            val matches = hand.filter { it.rank == rank }
            if (matches.size >= 2) {
                val keep = matches.take(2) + hand.filter { it !in matches }.take(2)
                if (keep.size == 4) return keep
            }
        }
        // Otherwise keep random 4
        return hand.shuffled(random).take(4)
    }

    private fun aiDiscardMedium(hand: List<Card>, ownsCrib: Boolean, random: Random): Pair<List<Card>, List<Card>> {
        return bestDiscard(hand, ownsCrib, random, useExpectedValue = true, defensivePegging = false)
    }

    private fun aiDiscardHard(hand: List<Card>, ownsCrib: Boolean, random: Random): Pair<List<Card>, List<Card>> {
        return bestDiscard(hand, ownsCrib, random, useExpectedValue = true, defensivePegging = true)
    }

    /**
     * Evaluates all C(6,4) = 15 ways to keep 4 cards, scoring each by its
     * expected show value using a simple static evaluator.
     */
    private fun bestDiscard(
        hand: List<Card>,
        ownsCrib: Boolean,
        random: Random,
        useExpectedValue: Boolean,
        defensivePegging: Boolean
    ): Pair<List<Card>, List<Card>> {
        data class Option(val keep: List<Card>, val discard: List<Card>, val score: Double)

        val options = combinations(hand, 4).map { keep ->
            val discard = hand.filter { it !in keep }
            val handValue = expectedHandValue(keep)
            // Add crib value for discarded cards if we own the crib
            val cribBonus = if (ownsCrib) expectedCribValue(discard) else -expectedCribValue(discard) * 0.5
            val peggingPenalty = if (defensivePegging) peggingRiskPenalty(discard) else 0.0
            Option(keep, discard, handValue + cribBonus - peggingPenalty)
        }
        val best = options.maxByOrNull { it.score } ?: options.random(random)
        return best.discard to best.keep
    }

    /**
     * Very rough expected show value for a 4-card keep (before the starter is known).
     * Counts certain pairs, fifteens already present, and run potential.
     */
    private fun expectedHandValue(cards: List<Card>): Double {
        var score = 0.0
        // Count pairs
        for (i in cards.indices) for (j in i + 1 until cards.size) {
            if (cards[i].rank == cards[j].rank) score += 2.0
        }
        // Count fifteens
        for (mask in 1 until (1 shl cards.size)) {
            var sum = 0
            for (i in cards.indices) if (mask and (1 shl i) != 0) sum += cardValue(cards[i])
            if (sum == 15) score += 2.0
        }
        // Run potential
        val sorted = cards.map { runRank(it) }.sorted()
        val span = sorted.last() - sorted.first()
        if (span == 3 && sorted.distinct().size == 4) score += 1.5  // possible 4-run with starter
        if (span <= 4 && sorted.distinct().size >= 3) score += 0.5  // possible 3-run
        return score
    }

    /** Very rough expected crib value for the 2 discarded cards. */
    private fun expectedCribValue(cards: List<Card>): Double {
        var score = 0.0
        if (cards.size == 2) {
            if (cards[0].rank == cards[1].rank) score += 2.0
            val sum = cardValue(cards[0]) + cardValue(cards[1])
            if (sum == 5 || sum == 10) score += 1.0 // can make a 15 with one more card
            if (sum == 15) score += 2.0
        }
        return score
    }

    /** Penalty for giving the opponent an easy 15/31 play. */
    private fun peggingRiskPenalty(cards: List<Card>): Double {
        var risk = 0.0
        for (c in cards) {
            when (cardValue(c)) {
                5 -> risk += 1.0 // 5s are dangerous: pairs a 10/J/Q/K
                10 -> risk += 0.5
            }
        }
        return risk
    }

    // -------------------------------------------------------------------------
    // AI Pegging play
    // -------------------------------------------------------------------------

    /**
     * AI picks the best legal card to play during pegging.
     */
    fun aiChoosePeggingCard(
        legalCards: List<Card>,
        state: CribbageState,
        difficulty: GameDifficulty,
        random: Random
    ): Card {
        return when (difficulty) {
            GameDifficulty.EASY -> legalCards.random(random)
            GameDifficulty.MEDIUM, GameDifficulty.HARD -> {
                // Score each legal card and pick the one that yields most points
                val best = legalCards.maxByOrNull { card ->
                    val newPile = state.playPile + PlayedCard(card, CribbagePlayer.AI, state.pegCount + cardValue(card))
                    val scores = scorePeggingPlay(newPile, state.pegCount + cardValue(card))
                    var pts = scores.sumOf { it.points }.toDouble()
                    // Hard: slightly prefer not giving opponent a 15 or 31
                    if (difficulty == GameDifficulty.HARD) {
                        val remainingCount = state.pegCount + cardValue(card)
                        if (remainingCount == 21) pts -= 1.0 // leaves 10 for opponent to hit 31
                        if (remainingCount == 10) pts -= 0.5 // leaves 5 for opponent to hit 15
                    }
                    pts
                }
                best ?: legalCards.random(random)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    fun initialState(targetScore: Int = 121): CribbageState =
        CribbageState(targetScore = targetScore, phase = CribbagePhase.DEALING)
}
