package com.xanticious.androidgames.controller.games.poker

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.cards.Decks
import com.xanticious.androidgames.model.games.cards.deal
import com.xanticious.androidgames.model.games.poker.HandResult
import com.xanticious.androidgames.model.games.poker.PokerGameState
import com.xanticious.androidgames.model.games.poker.PokerPlayer
import kotlin.random.Random

/**
 * Pure game-logic controller for Five-Card Draw poker.
 *
 * Every function is a pure transformation: it takes an immutable [PokerGameState]
 * and returns a new one. No Android, Compose, or I/O imports.
 *
 * Randomness is injected via [Random] so callers can pass [Random(seed)] for
 * deterministic tests.
 */
object PokerController {

    const val STARTING_BANKROLL = 500
    const val DEFAULT_ANTE = 5
    const val MIN_BET = 5

    // ---- Game setup ----

    /** Build the initial game state for a fresh session. */
    fun createGame(difficulty: GameDifficulty): PokerGameState {
        val players = listOf(
            PokerPlayer(0, "You", STARTING_BANKROLL, isHuman = true),
            PokerPlayer(1, "Bot A", STARTING_BANKROLL, difficulty = difficulty),
            PokerPlayer(2, "Bot B", STARTING_BANKROLL, difficulty = difficulty),
            PokerPlayer(3, "Bot C", STARTING_BANKROLL, difficulty = difficulty)
        )
        return PokerGameState(players = players, anteAmount = DEFAULT_ANTE)
    }

    /**
     * Shuffle the deck, post antes for all active players, and deal five cards
     * to each. Returns the state ready for Betting Round 1.
     */
    fun startHand(state: PokerGameState, seed: Long): PokerGameState {
        val shuffled = Decks.shuffled(seed)

        // Reset per-hand fields; players with bankroll == 0 sit out (isFolded = true).
        val reset = state.players.map { p ->
            p.copy(
                hand = emptyList(),
                currentRoundBet = 0,
                isFolded = p.bankroll <= 0,
                isAllIn = false,
                hasActed = false
            )
        }

        // Post antes.
        var pot = 0
        val afterAntes = reset.map { p ->
            if (!p.isFolded) {
                val ante = minOf(p.bankroll, state.anteAmount)
                pot += ante
                p.copy(bankroll = p.bankroll - ante, currentRoundBet = ante)
            } else p
        }

        // Deal 5 cards face-up.
        var remaining = shuffled
        val dealt = afterAntes.map { p ->
            if (!p.isFolded) {
                val (hand, rest) = remaining.deal(5)
                remaining = rest
                p.copy(hand = hand.map { it.faceUp() })
            } else p
        }

        val firstToAct = firstActiveAfterDealer(dealt, state.dealerIndex)
        return state.copy(
            players = dealt,
            deck = remaining,
            pot = pot,
            currentBet = state.anteAmount,
            activePlayerIndex = firstToAct,
            selectedDiscards = emptySet(),
            handResult = null,
            sessionOver = false,
            statusMessage = ""
        )
    }

    // ---- Betting actions ----

    /** Fold — the active player surrenders their hand. */
    fun fold(state: PokerGameState): PokerGameState {
        val p = state.players[state.activePlayerIndex].copy(isFolded = true, hasActed = true)
        val updated = state.players.toMutableList().also { it[state.activePlayerIndex] = p }
        val next = nextActivePlayer(updated, state.activePlayerIndex)
        return state.copy(players = updated, activePlayerIndex = next)
    }

    /**
     * Check — only valid when [PokerGameState.toCall] == 0 (no outstanding bet).
     */
    fun check(state: PokerGameState): PokerGameState {
        val p = state.players[state.activePlayerIndex].copy(hasActed = true)
        val updated = state.players.toMutableList().also { it[state.activePlayerIndex] = p }
        val next = nextActivePlayer(updated, state.activePlayerIndex)
        return state.copy(players = updated, activePlayerIndex = next)
    }

    /** Call — match the current bet (or go all-in if bankroll is insufficient). */
    fun call(state: PokerGameState): PokerGameState {
        val player = state.players[state.activePlayerIndex]
        val amount = minOf(state.toCall, player.bankroll)
        val newRoundBet = player.currentRoundBet + amount
        val newPlayer = player.copy(
            bankroll = player.bankroll - amount,
            currentRoundBet = newRoundBet,
            hasActed = true,
            isAllIn = player.bankroll - amount == 0
        )
        val updated = state.players.toMutableList().also { it[state.activePlayerIndex] = newPlayer }
        val next = nextActivePlayer(updated, state.activePlayerIndex)
        return state.copy(players = updated, pot = state.pot + amount, activePlayerIndex = next)
    }

    /**
     * Raise (or open with a first bet) by [raiseAmount] chips above the current
     * call level. All other active non-all-in players have their [PokerPlayer.hasActed]
     * reset so they can respond.
     *
     * [raiseAmount] is clamped to at least [MIN_BET] and at most the player's
     * remaining stack after paying the call portion.
     */
    fun raise(state: PokerGameState, raiseAmount: Int): PokerGameState {
        val player = state.players[state.activePlayerIndex]
        val callPortion = minOf(state.toCall, player.bankroll)
        val maxRaise = player.bankroll - callPortion
        val actualRaise = raiseAmount.coerceIn(MIN_BET, maxRaise.coerceAtLeast(MIN_BET))
        val totalAdd = callPortion + minOf(actualRaise, maxRaise)
        val newRoundBet = player.currentRoundBet + totalAdd
        val newPlayer = player.copy(
            bankroll = player.bankroll - totalAdd,
            currentRoundBet = newRoundBet,
            hasActed = true,
            isAllIn = player.bankroll - totalAdd == 0
        )
        // Everyone else must respond to the raise.
        val updated = state.players.mapIndexed { i, p ->
            when {
                i == state.activePlayerIndex -> newPlayer
                !p.isFolded && !p.isAllIn -> p.copy(hasActed = false)
                else -> p
            }
        }
        val next = nextActivePlayer(updated, state.activePlayerIndex)
        return state.copy(
            players = updated,
            pot = state.pot + totalAdd,
            currentBet = newRoundBet,
            activePlayerIndex = next
        )
    }

    /**
     * True when every active, non-all-in player has acted and matched [PokerGameState.currentBet].
     */
    fun isBettingRoundComplete(state: PokerGameState): Boolean {
        val eligible = state.players.filter { !it.isFolded && !it.isAllIn }
        if (eligible.isEmpty()) return true
        return eligible.all { it.hasActed && it.currentRoundBet == state.currentBet }
    }

    /** True when all players except one have folded. */
    fun allButOneFolded(state: PokerGameState): Boolean =
        state.players.count { !it.isFolded } == 1

    /**
     * Reset per-round fields and advance [PokerGameState.activePlayerIndex] to the
     * first player to act in the new round (left of the dealer).
     */
    fun startBettingRound(state: PokerGameState): PokerGameState {
        val players = state.players.map { p ->
            if (!p.isFolded) p.copy(currentRoundBet = 0, hasActed = false) else p
        }
        val firstToAct = firstActiveAfterDealer(players, state.dealerIndex)
        return state.copy(players = players, currentBet = 0, activePlayerIndex = firstToAct)
    }

    // ---- Draw phase ----

    /** Toggle one of the human's cards in/out of the discard selection. */
    fun toggleDiscard(state: PokerGameState, cardIndex: Int): PokerGameState {
        val updated = if (cardIndex in state.selectedDiscards)
            state.selectedDiscards - cardIndex
        else
            state.selectedDiscards + cardIndex
        return state.copy(selectedDiscards = updated)
    }

    /**
     * Apply the human's confirmed discard selection, then have each AI player
     * draw. Returns the state ready for Betting Round 2.
     */
    fun confirmDraw(state: PokerGameState, random: Random): PokerGameState {
        var s = drawForPlayer(state, 0, state.selectedDiscards.sorted(), random)
        s = s.copy(selectedDiscards = emptySet())
        for (i in 1 until s.players.size) {
            val p = s.players[i]
            if (!p.isFolded) {
                val discards = aiDiscardIndices(p, random)
                s = drawForPlayer(s, i, discards, random)
            }
        }
        return s
    }

    private fun drawForPlayer(
        state: PokerGameState,
        playerIndex: Int,
        discardIndices: List<Int>,
        @Suppress("UNUSED_PARAMETER") random: Random
    ): PokerGameState {
        val player = state.players[playerIndex]
        val kept = player.hand.filterIndexed { i, _ -> i !in discardIndices }
        val (drawn, newDeck) = state.deck.deal(discardIndices.size)
        val newHand = kept + drawn.map { it.faceUp() }
        val newPlayer = player.copy(hand = newHand)
        val updated = state.players.toMutableList().also { it[playerIndex] = newPlayer }
        return state.copy(players = updated, deck = newDeck)
    }

    // ---- Showdown & pot award ----

    /**
     * Compare all active (non-folded) hands, award the pot to the winner(s),
     * and advance the dealer button.
     */
    fun resolveShowdown(state: PokerGameState): PokerGameState {
        val activeHands = state.players
            .filter { !it.isFolded }
            .associate { it.index to it.hand }
        val winners = HandEvaluator.findWinners(activeHands)
        val handNames = activeHands.mapValues { (_, h) -> HandEvaluator.displayName(h) }
        val perWinner = state.pot / winners.size
        val remainder = state.pot % winners.size

        val newPlayers = state.players.mapIndexed { i, p ->
            when {
                i in winners && i == winners.first() -> p.copy(bankroll = p.bankroll + perWinner + remainder)
                i in winners -> p.copy(bankroll = p.bankroll + perWinner)
                else -> p
            }
        }
        val result = HandResult(winners, state.pot, handNames)
        val isSessionOver = newPlayers[0].bankroll <= 0
        return state.copy(
            players = newPlayers,
            pot = 0,
            handResult = result,
            dealerIndex = nextDealerIndex(state),
            sessionOver = isSessionOver,
            statusMessage = buildResultMessage(result, state.players)
        )
    }

    /**
     * Award the pot to the last remaining player when everyone else has folded.
     * No cards are revealed.
     */
    fun awardPotToLastPlayer(state: PokerGameState): PokerGameState {
        val winner = state.players.firstOrNull { !it.isFolded } ?: return state
        val newPlayers = state.players.map { p ->
            if (p.index == winner.index) p.copy(bankroll = p.bankroll + state.pot) else p
        }
        val result = HandResult(listOf(winner.index), state.pot, emptyMap())
        val isSessionOver = newPlayers[0].bankroll <= 0
        return state.copy(
            players = newPlayers,
            pot = 0,
            handResult = result,
            dealerIndex = nextDealerIndex(state),
            sessionOver = isSessionOver,
            statusMessage = "${winner.name} wins \$${state.pot} (all others folded)"
        )
    }

    // ---- AI logic ----

    /**
     * Execute one AI action for the currently active player. Returns the updated
     * game state. [random] is used for bluff/slow-play decisions.
     */
    fun aiAction(state: PokerGameState, random: Random): PokerGameState {
        val player = state.players[state.activePlayerIndex]
        val rank = HandEvaluator.evaluate(player.hand)
        val strength = rank.category.rank  // 0 = HIGH_CARD … 9 = ROYAL_FLUSH
        return when (player.difficulty) {
            GameDifficulty.EASY -> easyAiAction(state, player.bankroll, strength, state.toCall)
            GameDifficulty.MEDIUM -> mediumAiAction(state, player.bankroll, strength, state.toCall, state.pot, random)
            GameDifficulty.HARD -> hardAiAction(state, player.bankroll, strength, state.toCall, state.pot, random)
        }
    }

    private fun easyAiAction(
        state: PokerGameState,
        bankroll: Int,
        strength: Int,
        toCall: Int
    ): PokerGameState = when {
        strength >= 3 ->  // three-of-a-kind or better → bet
            if (toCall == 0) raise(state, MIN_BET) else raise(state, MIN_BET)
        strength >= 1 ->  // pair → call cheap bets
            if (toCall == 0) check(state)
            else if (toCall <= bankroll / 4) call(state)
            else fold(state)
        else ->           // high card → check or fold
            if (toCall == 0) check(state) else fold(state)
    }

    private fun mediumAiAction(
        state: PokerGameState,
        bankroll: Int,
        strength: Int,
        toCall: Int,
        pot: Int,
        random: Random
    ): PokerGameState {
        val bluff = random.nextFloat() < 0.10f
        val potOddsOk = toCall in 1..maxOf(1, pot / 3)
        return when {
            bluff && toCall == 0 -> raise(state, MIN_BET * 2)
            strength >= 6 ->  // full house+
                if (toCall == 0) raise(state, MIN_BET * 3) else raise(state, MIN_BET * 2)
            strength >= 3 ->  // three-of-a-kind+
                if (toCall == 0) raise(state, MIN_BET * 2)
                else if (toCall <= bankroll / 3) call(state)
                else fold(state)
            strength >= 1 ->  // pair
                if (toCall == 0) check(state)
                else if (potOddsOk) call(state)
                else fold(state)
            else ->
                if (toCall == 0) check(state) else fold(state)
        }
    }

    private fun hardAiAction(
        state: PokerGameState,
        bankroll: Int,
        strength: Int,
        toCall: Int,
        pot: Int,
        random: Random
    ): PokerGameState {
        val bluff = random.nextFloat() < 0.15f
        // Slow-play monsters occasionally — just call/check to lure opponents in.
        val slowPlay = strength >= 7 && random.nextFloat() < 0.25f
        val betSize = when {
            strength >= 7 -> MIN_BET * 4
            strength >= 4 -> MIN_BET * 2
            else -> MIN_BET
        }
        return when {
            slowPlay ->
                if (toCall == 0) check(state) else call(state)
            bluff ->
                if (toCall == 0) raise(state, MIN_BET * 2)
                else if (toCall <= bankroll / 2) call(state)
                else fold(state)
            strength >= 4 ->  // straight or better
                raise(state, betSize)
            strength >= 2 ->  // two-pair+
                if (toCall == 0) raise(state, betSize)
                else if (toCall <= pot / 2) call(state)
                else fold(state)
            strength >= 1 ->  // pair
                if (toCall == 0) check(state)
                else if (toCall <= pot / 4) call(state)
                else fold(state)
            else ->
                if (toCall == 0) check(state) else fold(state)
        }
    }

    /**
     * Decide which card indices (0–4) an AI player should discard during the
     * draw phase. Pure function: all non-determinism is via [random].
     */
    fun aiDiscardIndices(player: PokerPlayer, random: Random): List<Int> {
        val hand = player.hand
        if (hand.size != 5) return emptyList()

        val indexed = hand.mapIndexed { i, c -> i to c.rank.highValue }
        val counts: Map<Int, Int> = indexed.groupBy { it.second }.mapValues { it.value.size }
        val maxCount = counts.values.maxOrNull() ?: 1

        return when {
            maxCount >= 4 -> {
                // Four of a kind — keep as-is.
                emptyList()
            }
            maxCount == 3 -> {
                // Three of a kind — discard the two non-matching cards.
                val tripleVal = counts.entries.first { it.value == 3 }.key
                indexed.filter { it.second != tripleVal }.map { it.first }
            }
            maxCount == 2 -> {
                val pairs = counts.entries.filter { it.value == 2 }.map { it.key }
                if (pairs.size >= 2) {
                    // Two pair — discard only the kicker.
                    indexed.filter { it.second !in pairs }.map { it.first }
                } else {
                    // One pair — discard the three singletons.
                    val pairVal = pairs.first()
                    indexed.filter { it.second != pairVal }.map { it.first }
                }
            }
            else -> {
                // No made hand — check for 4-card flush draw.
                val suitCounts = hand.map { it.suit }.groupBy { it }.mapValues { it.value.size }
                val maxSuit = suitCounts.values.maxOrNull() ?: 0
                if (maxSuit == 4) {
                    val flushSuit = suitCounts.entries.first { it.value == 4 }.key
                    val offIdx = hand.indexOfFirst { it.suit != flushSuit }
                    if (offIdx >= 0) listOf(offIdx) else emptyList()
                } else {
                    // High-card hand: discard number depends on difficulty.
                    val sorted = indexed.sortedBy { it.second }
                    when (player.difficulty) {
                        GameDifficulty.EASY -> sorted.take(3).map { it.first }
                        GameDifficulty.MEDIUM -> sorted.take(2).map { it.first }
                        GameDifficulty.HARD ->
                            if (random.nextFloat() < 0.3f) sorted.take(1).map { it.first }
                            else sorted.take(3).map { it.first }
                    }
                }
            }
        }
    }

    // ---- Helpers ----

    /** True when index 0 is the active player (i.e. it is the human's turn). */
    fun isHumanTurn(state: PokerGameState): Boolean = state.activePlayerIndex == 0

    private fun firstActiveAfterDealer(players: List<PokerPlayer>, dealerIndex: Int): Int {
        val n = players.size
        for (offset in 1..n) {
            val idx = (dealerIndex + offset) % n
            if (!players[idx].isFolded && players[idx].bankroll > 0) return idx
        }
        return dealerIndex
    }

    /** Find the next active (non-folded, non-all-in) player after [currentIndex]. */
    fun nextActivePlayer(players: List<PokerPlayer>, currentIndex: Int): Int {
        val n = players.size
        for (offset in 1..n) {
            val idx = (currentIndex + offset) % n
            if (!players[idx].isFolded && !players[idx].isAllIn) return idx
        }
        return currentIndex
    }

    private fun nextDealerIndex(state: PokerGameState): Int {
        val n = state.players.size
        for (offset in 1..n) {
            val idx = (state.dealerIndex + offset) % n
            if (state.players[idx].bankroll > 0) return idx
        }
        return (state.dealerIndex + 1) % n
    }

    private fun buildResultMessage(result: HandResult, players: List<PokerPlayer>): String {
        val names = result.winnerIndices.mapNotNull { players.getOrNull(it)?.name }
        val handStr = result.winnerIndices.firstOrNull()
            ?.let { result.handNames[it] }
            ?.let { " with $it" }
            ?: ""
        return "${names.joinToString(" & ")} wins \$${result.potWon}$handStr"
    }
}
