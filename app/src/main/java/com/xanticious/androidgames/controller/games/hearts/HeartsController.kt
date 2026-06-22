package com.xanticious.androidgames.controller.games.hearts

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.Decks
import com.xanticious.androidgames.model.games.cards.Rank
import com.xanticious.androidgames.model.games.cards.Suit
import com.xanticious.androidgames.model.games.cards.deal
import com.xanticious.androidgames.model.games.hearts.HeartsConfig
import com.xanticious.androidgames.model.games.hearts.HeartsGameState
import com.xanticious.androidgames.model.games.hearts.PassDirection
import com.xanticious.androidgames.model.games.hearts.Seat
import com.xanticious.androidgames.model.games.hearts.TrickCard
import com.xanticious.androidgames.model.games.hearts.acrossSeat
import com.xanticious.androidgames.model.games.hearts.displayName
import com.xanticious.androidgames.model.games.hearts.leftSeat
import com.xanticious.androidgames.model.games.hearts.next
import com.xanticious.androidgames.model.games.hearts.rightSeat
import com.xanticious.androidgames.model.games.hearts.targetFor
import kotlin.random.Random

/**
 * Pure Hearts rules engine. Every function is a pure transformation:
 * model in → model out (or a simple value). No Android, Compose, or I/O.
 */
object HeartsController {

    // Clockwise seat order used for trick-play sequencing.
    private val CLOCKWISE = listOf(Seat.SOUTH, Seat.WEST, Seat.NORTH, Seat.EAST)

    /** Seats in clockwise order starting from [leadSeat]. */
    fun seatOrder(leadSeat: Seat): List<Seat> {
        val idx = CLOCKWISE.indexOf(leadSeat)
        return (0..3).map { CLOCKWISE[(idx + it) % 4] }
    }

    /** Which seat plays the next card into [trick], given [leadSeat] started it. */
    fun nextSeatToPlay(trick: List<TrickCard>, leadSeat: Seat): Seat =
        seatOrder(leadSeat)[trick.size]

    // ── Deal ──────────────────────────────────────────────────────────────────

    /**
     * Deal a fresh hand from a deck shuffled with [seed]. Resets all
     * hand-level state while preserving cumulative [HeartsGameState.scores]
     * and the current [HeartsGameState.passDirection].
     */
    fun deal(state: HeartsGameState, seed: Long): HeartsGameState {
        val deck        = Decks.shuffled(seed)
        val (s, rest1)  = deck.deal(13)
        val (w, rest2)  = rest1.deal(13)
        val (n, rest3)  = rest2.deal(13)
        val (e, _)      = rest3.deal(13)
        return state.copy(
            hands         = mapOf(Seat.SOUTH to s, Seat.WEST to w, Seat.NORTH to n, Seat.EAST to e),
            handScores    = Seat.entries.associateWith { 0 },
            tricksTaken   = Seat.entries.associateWith { emptyList() },
            currentTrick  = emptyList(),
            heartsBroken  = false,
            selectedCards = emptySet(),
            trickNumber   = 1,
            leadSeat      = Seat.SOUTH   // overwritten by prepareForPlay
        )
    }

    /**
     * Finds the holder of 2♣ and sets [HeartsGameState.leadSeat] accordingly.
     * Call after dealing (HOLD hand) or after [executePass].
     */
    fun prepareForPlay(state: HeartsGameState): HeartsGameState {
        val leader = Seat.entries.firstOrNull { seat ->
            state.hands[seat]?.contains(HeartsGameState.TWO_OF_CLUBS) == true
        } ?: Seat.SOUTH
        return state.copy(leadSeat = leader)
    }

    // ── Passing ───────────────────────────────────────────────────────────────

    /**
     * Toggle [card] in the human's pass selection. Removes it if already
     * selected; adds it if fewer than 3 are currently selected; no-ops otherwise.
     */
    fun togglePassSelection(state: HeartsGameState, card: Card): HeartsGameState {
        val sel = state.selectedCards
        return when {
            card in sel    -> state.copy(selectedCards = sel - card)
            sel.size < 3   -> state.copy(selectedCards = sel + card)
            else           -> state
        }
    }

    /**
     * Execute the passing phase:
     * - On a HOLD hand, cards stay; [prepareForPlay] is still called to fix [leadSeat].
     * - Otherwise, SOUTH passes their 3 [HeartsGameState.selectedCards]; each AI
     *   seat selects 3 cards via [aiChoosePassCards].
     */
    fun executePass(
        state: HeartsGameState,
        difficulty: GameDifficulty = GameDifficulty.MEDIUM,
        random: Random = Random.Default
    ): HeartsGameState {
        if (state.passDirection == PassDirection.HOLD) return prepareForPlay(state)

        val passMap: Map<Seat, List<Card>> = buildMap {
            put(Seat.SOUTH, state.selectedCards.toList().take(3))
            for (seat in listOf(Seat.WEST, Seat.NORTH, Seat.EAST)) {
                put(seat, aiChoosePassCards(state.hands[seat] ?: emptyList(), difficulty, random))
            }
        }

        val newHands: Map<Seat, List<Card>> = Seat.entries.associateWith { seat ->
            val mine     = state.hands[seat] ?: emptyList()
            val sending  = passMap[seat] ?: emptyList()
            val incoming = passMap.entries
                .filter { (sender, _) -> state.passDirection.targetFor(sender) == seat }
                .flatMap { (_, cards) -> cards }
            mine - sending.toSet() + incoming
        }

        return prepareForPlay(state.copy(hands = newHands, selectedCards = emptySet()))
    }

    // ── Legal-play validation ─────────────────────────────────────────────────

    /**
     * Returns the subset of [hand] that may legally be played right now.
     *
     * Rules enforced:
     * 1. First card of the whole hand → must be 2♣.
     * 2. Leading (trick empty, not first trick) → cannot lead hearts until broken
     *    (or only hearts remain).
     * 3. Following → must follow led suit if able.
     * 4. Void in led suit on trick 1 → cannot play hearts or Q♠ unless no choice.
     * 5. Void in led suit on later tricks → any card is legal.
     */
    fun legalPlays(
        hand: List<Card>,
        trick: List<TrickCard>,
        heartsBroken: Boolean,
        trickNumber: Int
    ): List<Card> {
        if (hand.isEmpty()) return emptyList()

        // Rule 1: opening lead of the hand must be 2♣
        if (trick.isEmpty() && trickNumber == 1) {
            val twoClubs = hand.firstOrNull { it == HeartsGameState.TWO_OF_CLUBS }
            return if (twoClubs != null) listOf(twoClubs) else hand
        }

        // Leading a trick (not the first of the hand)
        if (trick.isEmpty()) {
            if (!heartsBroken) {
                val nonHearts = hand.filter { it.suit != Suit.HEARTS }
                if (nonHearts.isNotEmpty()) return nonHearts
            }
            return hand  // hearts broken, or only hearts remain
        }

        // Following suit: must match led suit if possible
        val ledSuit = trick.first().card.suit
        val suitCards = hand.filter { it.suit == ledSuit }
        if (suitCards.isNotEmpty()) return suitCards

        // Void in led suit on first trick: no hearts or Q♠ unless unavoidable
        if (trickNumber == 1) {
            val safe = hand.filter { it.suit != Suit.HEARTS && it != HeartsGameState.QUEEN_OF_SPADES }
            return safe.ifEmpty { hand }
        }

        return hand
    }

    /** Convenience wrapper — checks whether [card] is legal for SOUTH right now. */
    fun isLegalPlay(state: HeartsGameState, card: Card): Boolean {
        val hand = state.hands[Seat.SOUTH] ?: return false
        return card in legalPlays(hand, state.currentTrick, state.heartsBroken, state.trickNumber)
    }

    // ── Playing a card ────────────────────────────────────────────────────────

    /**
     * Play [card] from [seat]. Validates legality and presence in hand; returns
     * the unchanged state if the play is invalid. Updates [HeartsGameState.heartsBroken].
     */
    fun playCard(state: HeartsGameState, card: Card, seat: Seat): HeartsGameState {
        val hand = state.hands[seat] ?: return state
        if (card !in hand) return state
        val legal = legalPlays(hand, state.currentTrick, state.heartsBroken, state.trickNumber)
        if (card !in legal) return state

        return state.copy(
            hands        = state.hands + (seat to hand - card),
            currentTrick = state.currentTrick + TrickCard(card, seat),
            heartsBroken = state.heartsBroken || card.suit == Suit.HEARTS
        )
    }

    // ── Trick resolution ──────────────────────────────────────────────────────

    /**
     * Determine the winner of a complete (4-card) trick.
     * The highest card of the led suit wins; other suits cannot win.
     * Uses [Rank.highValue] so Ace (14) beats King (13) — Ace is high in Hearts.
     */
    fun trickWinner(trick: List<TrickCard>): Seat {
        require(trick.size == 4) { "Trick must contain exactly 4 cards (got ${trick.size})" }
        val ledSuit = trick.first().card.suit
        return trick
            .filter { it.card.suit == ledSuit }
            .maxBy { it.card.rank.highValue }
            .seat
    }

    /** Point value of a single card under the given [config]. */
    fun cardPoints(card: Card, config: HeartsConfig): Int = when {
        card.suit == Suit.HEARTS                        -> 1
        card == HeartsGameState.QUEEN_OF_SPADES         -> 13
        config.jackOfDiamonds && card == HeartsGameState.JACK_OF_DIAMONDS -> -10
        else                                            -> 0
    }

    /**
     * Resolve the completed trick: credit the cards to the winner's [tricksTaken],
     * clear [HeartsGameState.currentTrick], advance [HeartsGameState.trickNumber],
     * and set [HeartsGameState.leadSeat] to the winner.
     */
    fun resolveTrick(state: HeartsGameState): HeartsGameState {
        require(state.currentTrick.size == 4) { "Cannot resolve incomplete trick" }
        val winner    = trickWinner(state.currentTrick)
        val wonCards  = state.currentTrick.map { it.card }
        val newTaken  = state.tricksTaken +
            (winner to (state.tricksTaken[winner] ?: emptyList()) + wonCards)
        return state.copy(
            tricksTaken  = newTaken,
            currentTrick = emptyList(),
            leadSeat     = winner,
            trickNumber  = state.trickNumber + 1
        )
    }

    /** Returns `true` when all 13 tricks of the hand have been played and resolved. */
    fun isHandComplete(state: HeartsGameState): Boolean =
        state.hands.values.all { it.isEmpty() } && state.currentTrick.isEmpty()

    // ── Hand scoring ──────────────────────────────────────────────────────────

    /**
     * Detects Shoot the Moon: one seat took all 13 hearts AND the Q♠.
     * Returns that seat, or `null` if no moon was shot.
     */
    fun detectShootTheMoon(tricksTaken: Map<Seat, List<Card>>): Seat? =
        Seat.entries.firstOrNull { seat ->
            val taken = tricksTaken[seat] ?: emptyList()
            taken.count { it.suit == Suit.HEARTS } == 13 &&
                taken.any { it == HeartsGameState.QUEEN_OF_SPADES }
        }

    /**
     * Calculate per-seat hand scores, applying Shoot the Moon if detected.
     * Returns a map of seat → points scored this hand.
     */
    fun calculateHandScores(
        tricksTaken: Map<Seat, List<Card>>,
        config: HeartsConfig
    ): Map<Seat, Int> {
        val shooter = detectShootTheMoon(tricksTaken)
        return if (shooter != null) {
            Seat.entries.associateWith { seat -> if (seat == shooter) 0 else 26 }
        } else {
            Seat.entries.associateWith { seat ->
                (tricksTaken[seat] ?: emptyList()).sumOf { cardPoints(it, config) }
            }
        }
    }

    /**
     * Score the completed hand, update cumulative [HeartsGameState.scores], advance
     * the pass direction, and check for game-end. Returns the new state.
     */
    fun scoreHand(state: HeartsGameState): HeartsGameState {
        val handScores = calculateHandScores(state.tricksTaken, state.config)
        val newScores  = Seat.entries.associateWith { seat ->
            (state.scores[seat] ?: 0) + (handScores[seat] ?: 0)
        }
        val gameOver   = newScores.values.any { it >= state.config.gameEndScore }
        val winner     = if (gameOver) newScores.minByOrNull { it.value }?.key else null

        return state.copy(
            scores        = newScores,
            handScores    = handScores,
            tricksTaken   = Seat.entries.associateWith { emptyList() },
            passDirection = state.passDirection.next(),
            handNumber    = state.handNumber + 1,
            gameOver      = gameOver,
            gameWinner    = winner
        )
    }

    // ── AI — pass selection ───────────────────────────────────────────────────

    /**
     * AI selects 3 cards from [hand] to pass.
     *
     * - EASY: random selection.
     * - MEDIUM: dump the highest-danger cards (Q♠, high spades, high hearts).
     * - HARD: prioritise Q♠, then high spades that expose Q♠, then high hearts.
     */
    fun aiChoosePassCards(
        hand: List<Card>,
        difficulty: GameDifficulty,
        random: Random
    ): List<Card> {
        if (hand.size <= 3) return hand
        return when (difficulty) {
            GameDifficulty.EASY -> hand.shuffled(random).take(3)

            GameDifficulty.MEDIUM -> hand
                .sortedByDescending { card ->
                    when {
                        card == HeartsGameState.QUEEN_OF_SPADES          -> 200
                        card.suit == Suit.HEARTS                         -> 100 + card.rank.value
                        card.suit == Suit.SPADES && card.rank.value >= 11 -> 80 + card.rank.value
                        else                                             -> card.rank.value
                    }
                }.take(3)

            GameDifficulty.HARD -> hand
                .sortedByDescending { card ->
                    when {
                        card == HeartsGameState.QUEEN_OF_SPADES              -> 300
                        card.suit == Suit.SPADES && card.rank.value >= 11    -> 200 + card.rank.value
                        card.suit == Suit.HEARTS && card.rank == Rank.ACE    -> 180
                        card.suit == Suit.HEARTS && card.rank.value >= 10    -> 150 + card.rank.value
                        card.suit == Suit.HEARTS                             -> 100 + card.rank.value
                        else                                                 -> card.rank.value
                    }
                }.take(3)
        }
    }

    // ── AI — card play ────────────────────────────────────────────────────────

    /**
     * AI chooses a card to play from [hand].
     * Always returns a legal card (falls back to `legalPlays.first()` if the
     * heuristic produces nothing).
     *
     * @param tricksTaken  All cards taken so far this hand (for HARD card counting).
     */
    fun aiChooseCard(
        hand: List<Card>,
        trick: List<TrickCard>,
        heartsBroken: Boolean,
        trickNumber: Int,
        tricksTaken: Map<Seat, List<Card>>,
        difficulty: GameDifficulty,
        random: Random,
        seat: Seat
    ): Card {
        val legal = legalPlays(hand, trick, heartsBroken, trickNumber)
        if (legal.size == 1) return legal.first()

        return when (difficulty) {
            GameDifficulty.EASY   -> aiEasyCard(legal, trick, random)
            GameDifficulty.MEDIUM -> aiMediumCard(legal, trick, random)
            GameDifficulty.HARD   -> aiHardCard(legal, trick, tricksTaken, random)
        }
    }

    private fun aiEasyCard(legal: List<Card>, trick: List<TrickCard>, random: Random): Card {
        // Carelessly dumps high cards or plays randomly
        return if (random.nextFloat() < 0.4f) legal.random(random)
        else legal.maxBy { it.rank.value }
    }

    private fun aiMediumCard(legal: List<Card>, trick: List<TrickCard>, random: Random): Card {
        if (trick.isEmpty()) {
            // Leading: play the lowest non-heart
            val nonHearts = legal.filter { it.suit != Suit.HEARTS }
            return (nonHearts.ifEmpty { legal }).minBy { it.rank.value }
        }

        val ledSuit = trick.first().card.suit
        val followingLed = legal.any { it.suit == ledSuit }

        if (followingLed) {
            val ledCards = legal.filter { it.suit == ledSuit }
            val currentWinnerRank = trick.filter { it.card.suit == ledSuit }
                .maxOfOrNull { it.card.rank.value } ?: 0
            // Try to duck under the current winner
            val duckCards = ledCards.filter { it.rank.value < currentWinnerRank }
            return if (duckCards.isNotEmpty()) duckCards.maxBy { it.rank.value }
            else ledCards.minBy { it.rank.value }   // must win; play low winner
        }

        // Void in led suit: dump the most dangerous penalty card
        val qos = legal.firstOrNull { it == HeartsGameState.QUEEN_OF_SPADES }
        if (qos != null) return qos
        val highHeart = legal.filter { it.suit == Suit.HEARTS }.maxByOrNull { it.rank.value }
        if (highHeart != null) return highHeart
        return legal.maxBy { it.rank.value }
    }

    private fun aiHardCard(
        legal: List<Card>,
        trick: List<TrickCard>,
        tricksTaken: Map<Seat, List<Card>>,
        random: Random
    ): Card {
        val playedCards = tricksTaken.values.flatten() + trick.map { it.card }
        val qosPlayed   = HeartsGameState.QUEEN_OF_SPADES in playedCards

        if (trick.isEmpty()) {
            // Leading: prefer a safe low card in a short suit to void it
            val nonDanger = legal.filter { it.suit != Suit.HEARTS && it != HeartsGameState.QUEEN_OF_SPADES }
            return (nonDanger.ifEmpty { legal }).minBy { it.rank.value }
        }

        val ledSuit = trick.first().card.suit
        val followingLed = legal.any { it.suit == ledSuit }

        if (followingLed) {
            val ledCards = legal.filter { it.suit == ledSuit }
            val currentWinnerRank = trick.filter { it.card.suit == ledSuit }
                .maxOfOrNull { it.card.rank.value } ?: 0
            val trickHasPoints = trick.any {
                it.card.suit == Suit.HEARTS || it.card == HeartsGameState.QUEEN_OF_SPADES
            }
            val duckCards = ledCards.filter { it.rank.value < currentWinnerRank }
            return when {
                duckCards.isNotEmpty() && trickHasPoints ->
                    duckCards.maxBy { it.rank.value }    // duck to avoid points
                duckCards.isNotEmpty() ->
                    duckCards.maxBy { it.rank.value }    // duck to stay safe
                else ->
                    ledCards.minBy { it.rank.value }     // must win; take cheaply
            }
        }

        // Void in led suit: dump most dangerous card first
        val qos = legal.firstOrNull { it == HeartsGameState.QUEEN_OF_SPADES }
        if (qos != null) return qos
        val highHeart = legal.filter { it.suit == Suit.HEARTS }.maxByOrNull { it.rank.value }
        if (highHeart != null) return highHeart
        // Dump high spades if Q♠ not yet played (protect ourselves from winning Q♠ later)
        if (!qosPlayed) {
            val highSpade = legal.filter { it.suit == Suit.SPADES && it.rank.value >= 11 }
                .maxByOrNull { it.rank.value }
            if (highSpade != null) return highSpade
        }
        return legal.maxBy { it.rank.value }
    }
}
