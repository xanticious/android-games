package com.xanticious.androidgames.controller.games.loveletter

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.loveletter.LoveLetterCard
import com.xanticious.androidgames.model.games.loveletter.LoveLetterGame
import com.xanticious.androidgames.model.games.loveletter.LoveLetterPlayer
import kotlin.random.Random

/**
 * Pure Love Letter game-logic controller. All functions take model input and return
 * a new model — no Android, no Compose, no side effects.
 *
 * Deck composition (16 cards): Guard×5, Priest×2, Baron×2, Handmaid×2, Prince×2,
 * King×1, Countess×1, Princess×1.
 */
class LoveLetterController {

    val fullDeck: List<LoveLetterCard> = buildList {
        repeat(5) { add(LoveLetterCard.GUARD) }
        repeat(2) { add(LoveLetterCard.PRIEST) }
        repeat(2) { add(LoveLetterCard.BARON) }
        repeat(2) { add(LoveLetterCard.HANDMAID) }
        repeat(2) { add(LoveLetterCard.PRINCE) }
        add(LoveLetterCard.KING)
        add(LoveLetterCard.COUNTESS)
        add(LoveLetterCard.PRINCESS)
    }

    fun tokensToWin(playerCount: Int): Int = when (playerCount) {
        2 -> 7
        3 -> 5
        else -> 4
    }

    fun initialGame(
        playerCount: Int,
        difficulty: GameDifficulty,
        seed: Long = Random.Default.nextLong()
    ): LoveLetterGame {
        require(playerCount in 2..4) { "Player count must be 2–4" }
        val players = buildList {
            add(LoveLetterPlayer(name = "You", isHuman = true))
            for (i in 1 until playerCount) {
                add(LoveLetterPlayer(name = "Bot ${('A' + i - 1)}", isHuman = false))
            }
        }
        return LoveLetterGame(
            players = players,
            deck = emptyList(),
            burnedCard = null,
            revealedBurnCards = emptyList(),
            currentPlayerIndex = 0,
            roundNumber = 1,
            tokensToWin = tokensToWin(playerCount),
            difficulty = difficulty
        )
    }

    /** Shuffle the deck, burn one card, deal one card to each player, reset round state. */
    fun startRound(game: LoveLetterGame, seed: Long): LoveLetterGame {
        val rng = Random(seed)
        val shuffled = fullDeck.shuffled(rng)

        val burnedCard = shuffled[0]
        var remaining = shuffled.drop(1)

        // 2-player: reveal three extra cards face-up (per Love Letter rules)
        val revealedBurns = if (game.players.size == 2) {
            val extra = remaining.take(3)
            remaining = remaining.drop(3)
            extra
        } else {
            emptyList()
        }

        val newPlayers = game.players.mapIndexed { i, player ->
            player.copy(
                hand = listOf(remaining[i]),
                discards = emptyList(),
                isEliminated = false,
                isProtected = false
            )
        }
        remaining = remaining.drop(game.players.size)

        return game.copy(
            players = newPlayers,
            deck = remaining,
            burnedCard = burnedCard,
            revealedBurnCards = revealedBurns,
            currentPlayerIndex = 0,
            roundNumber = game.roundNumber,
            pendingCardPlay = null,
            pendingTargetIndex = null,
            lastEffect = "Round ${game.roundNumber} started."
        )
    }

    /**
     * Draw the top card from the deck into the current player's hand.
     * Also clears Handmaid protection (it expires at the start of your turn).
     */
    fun drawCard(game: LoveLetterGame): LoveLetterGame {
        require(game.deck.isNotEmpty()) { "Deck is empty — cannot draw" }
        val drawn = game.deck.first()
        val player = game.currentPlayer
        val updated = player.copy(hand = player.hand + drawn, isProtected = false)
        return game.copy(
            deck = game.deck.drop(1),
            players = game.players.toMutableList().also { it[game.currentPlayerIndex] = updated }
        )
    }

    /** Returns true if the current player must play Countess (holds it alongside King or Prince). */
    fun mustPlayCountess(hand: List<LoveLetterCard>): Boolean =
        hand.contains(LoveLetterCard.COUNTESS) &&
            (hand.contains(LoveLetterCard.KING) || hand.contains(LoveLetterCard.PRINCE))

    /**
     * Returns valid target player indices for [card] played by [playerId].
     * Guards all require an unprotected, living opponent except Prince which may target self.
     */
    fun validTargets(game: LoveLetterGame, card: LoveLetterCard, playerId: Int): List<Int> =
        when (card) {
            LoveLetterCard.GUARD,
            LoveLetterCard.PRIEST,
            LoveLetterCard.BARON,
            LoveLetterCard.KING ->
                game.players.indices.filter { i ->
                    i != playerId && !game.players[i].isEliminated && !game.players[i].isProtected
                }

            LoveLetterCard.PRINCE ->
                // May target self (ignores own Handmaid protection) or unprotected opponents
                game.players.indices.filter { i ->
                    !game.players[i].isEliminated && (i == playerId || !game.players[i].isProtected)
                }

            else -> emptyList() // Handmaid, Countess, Princess need no target
        }

    /**
     * Play [cardToPlay] for the current player and resolve its effect immediately.
     * Returns updated [LoveLetterGame]; all changes are immutable / returned as a new copy.
     *
     * Precondition: [cardToPlay] must be in the current player's hand.
     */
    fun playCard(
        game: LoveLetterGame,
        cardToPlay: LoveLetterCard,
        targetPlayerIndex: Int? = null,
        guardGuess: LoveLetterCard? = null
    ): LoveLetterGame {
        val playerIdx = game.currentPlayerIndex
        val player = game.players[playerIdx]

        // Remove played card from hand, add to discards
        val updatedHand = player.hand.toMutableList().also { it.remove(cardToPlay) }
        val updatedDiscards = player.discards + cardToPlay

        val players = game.players.toMutableList()
        players[playerIdx] = player.copy(hand = updatedHand, discards = updatedDiscards)

        var deck = game.deck
        var burnedCard = game.burnedCard
        var effectDesc: String

        when (cardToPlay) {
            LoveLetterCard.GUARD -> {
                effectDesc = if (targetPlayerIndex != null && guardGuess != null) {
                    val target = players[targetPlayerIndex]
                    if (!target.isProtected && !target.isEliminated) {
                        if (target.hand.contains(guardGuess)) {
                            players[targetPlayerIndex] = target.copy(
                                isEliminated = true,
                                discards = target.discards + target.hand,
                                hand = emptyList()
                            )
                            "${target.name} had ${guardGuess.displayName} — eliminated!"
                        } else {
                            "${target.name} did not have ${guardGuess.displayName}."
                        }
                    } else {
                        "Guard played — target is protected or eliminated."
                    }
                } else {
                    "Guard played — no valid target."
                }
            }

            LoveLetterCard.PRIEST -> {
                effectDesc = if (targetPlayerIndex != null) {
                    val target = players[targetPlayerIndex]
                    if (!target.isProtected && !target.isEliminated) {
                        "${target.name} holds ${target.hand.firstOrNull()?.displayName ?: "nothing"}."
                    } else {
                        "Priest played — target is protected."
                    }
                } else {
                    "Priest played — no valid target."
                }
            }

            LoveLetterCard.BARON -> {
                effectDesc = if (targetPlayerIndex != null) {
                    val target = players[targetPlayerIndex]
                    if (!target.isProtected && !target.isEliminated) {
                        val myCard = players[playerIdx].hand.firstOrNull()
                        val theirCard = target.hand.firstOrNull()
                        if (myCard != null && theirCard != null) {
                            when {
                                myCard.value < theirCard.value -> {
                                    players[playerIdx] = players[playerIdx].copy(
                                        isEliminated = true,
                                        discards = players[playerIdx].discards + players[playerIdx].hand,
                                        hand = emptyList()
                                    )
                                    "${player.name} (${myCard.displayName}) vs ${target.name} (${theirCard.displayName}) — ${player.name} eliminated!"
                                }
                                myCard.value > theirCard.value -> {
                                    players[targetPlayerIndex] = target.copy(
                                        isEliminated = true,
                                        discards = target.discards + target.hand,
                                        hand = emptyList()
                                    )
                                    "${player.name} (${myCard.displayName}) vs ${target.name} (${theirCard.displayName}) — ${target.name} eliminated!"
                                }
                                else ->
                                    "Baron: ${myCard.displayName} ties ${theirCard.displayName} — no elimination."
                            }
                        } else {
                            "Baron played — empty hand."
                        }
                    } else {
                        "Baron played — target is protected."
                    }
                } else {
                    "Baron played — no valid target."
                }
            }

            LoveLetterCard.HANDMAID -> {
                players[playerIdx] = players[playerIdx].copy(isProtected = true)
                effectDesc = "${player.name} is protected until next turn."
            }

            LoveLetterCard.PRINCE -> {
                val targetIdx = targetPlayerIndex ?: playerIdx
                val target = players[targetIdx]
                val canTarget = targetIdx == playerIdx || !target.isProtected
                effectDesc = if (!target.isEliminated && canTarget) {
                    val discarded = target.hand.firstOrNull()
                    if (discarded == LoveLetterCard.PRINCESS) {
                        players[targetIdx] = target.copy(
                            isEliminated = true,
                            discards = target.discards + discarded,
                            hand = emptyList()
                        )
                        "${target.name} discarded Princess — eliminated!"
                    } else if (discarded != null) {
                        // Draw from deck, or take burned card if deck is empty
                        val newCard = if (deck.isNotEmpty()) {
                            val top = deck.first()
                            deck = deck.drop(1)
                            top
                        } else {
                            val saved = burnedCard
                            burnedCard = null
                            saved
                        }
                        players[targetIdx] = target.copy(
                            hand = listOfNotNull(newCard),
                            discards = target.discards + discarded
                        )
                        "${target.name} discarded ${discarded.displayName} and drew a new card."
                    } else {
                        "Prince played — target has empty hand."
                    }
                } else {
                    "Prince played — no valid target."
                }
            }

            LoveLetterCard.KING -> {
                effectDesc = if (targetPlayerIndex != null) {
                    val target = players[targetPlayerIndex]
                    if (!target.isProtected && !target.isEliminated) {
                        val myCard = players[playerIdx].hand.firstOrNull()
                        val theirCard = target.hand.firstOrNull()
                        if (myCard != null && theirCard != null) {
                            players[playerIdx] = players[playerIdx].copy(hand = listOf(theirCard))
                            players[targetPlayerIndex] = target.copy(hand = listOf(myCard))
                            "${player.name} swapped hands with ${target.name}."
                        } else {
                            "King played — empty hand."
                        }
                    } else {
                        "King played — target is protected."
                    }
                } else {
                    "King played — no valid target."
                }
            }

            LoveLetterCard.COUNTESS -> {
                effectDesc = "${player.name} played Countess."
            }

            LoveLetterCard.PRINCESS -> {
                // Playing Princess always eliminates you
                players[playerIdx] = players[playerIdx].copy(
                    isEliminated = true,
                    hand = emptyList()
                )
                effectDesc = "${player.name} played Princess — eliminated!"
            }
        }

        return game.copy(
            players = players,
            deck = deck,
            burnedCard = burnedCard,
            pendingCardPlay = null,
            pendingTargetIndex = null,
            lastEffect = effectDesc
        )
    }

    /** True when the round should end: one or fewer active players, or the deck is empty. */
    fun checkRoundOver(game: LoveLetterGame): Boolean =
        game.players.count { !it.isEliminated } <= 1 || game.deck.isEmpty()

    /**
     * Determines which player wins the round.
     * If one active player remains, they win.
     * If the deck ran out, the player with the highest hand card wins;
     * ties broken by the highest sum of discards.
     */
    fun roundWinnerIndex(game: LoveLetterGame): Int {
        val active = game.players.withIndex().filter { !it.value.isEliminated }
        if (active.size == 1) return active.first().index
        return active.maxWithOrNull(
            compareBy(
                { it.value.hand.maxOfOrNull { c -> c.value } ?: 0 },
                { it.value.discards.sumOf { c -> c.value } }
            )
        )?.index ?: 0
    }

    /** Award one token to [winnerIndex] and return updated game. */
    fun awardToken(game: LoveLetterGame, winnerIndex: Int): LoveLetterGame {
        val updated = game.players.toMutableList()
        updated[winnerIndex] = updated[winnerIndex].copy(tokens = updated[winnerIndex].tokens + 1)
        return game.copy(players = updated)
    }

    /** Returns the index of the game winner (first to reach tokensToWin), or null if no one has won yet. */
    fun gameWinner(game: LoveLetterGame): Int? =
        game.players.indexOfFirst { it.tokens >= game.tokensToWin }.takeIf { it >= 0 }

    /** Increment round number, preserving tokens. Caller should then call [startRound]. */
    fun nextRound(game: LoveLetterGame): LoveLetterGame =
        game.copy(roundNumber = game.roundNumber + 1)

    /** Advance [currentPlayerIndex] to the next non-eliminated player (wraps around). */
    fun advanceTurn(game: LoveLetterGame): LoveLetterGame {
        val n = game.players.size
        var next = (game.currentPlayerIndex + 1) % n
        var steps = 0
        while (game.players[next].isEliminated && steps < n) {
            next = (next + 1) % n
            steps++
        }
        return game.copy(currentPlayerIndex = next)
    }

    // ── AI decision functions ────────────────────────────────────────────────────

    /**
     * Choose which card the AI at [game.currentPlayerIndex] will play.
     * Respects the Countess forced-play rule. Never willingly plays Princess.
     */
    fun aiChooseCard(game: LoveLetterGame, random: Random): LoveLetterCard {
        val hand = game.currentPlayer.hand
        if (mustPlayCountess(hand)) return LoveLetterCard.COUNTESS

        // Avoid playing Princess unless it's the only card
        val playable = hand.filter { it != LoveLetterCard.PRINCESS }.ifEmpty { hand }

        return when (game.difficulty) {
            GameDifficulty.EASY -> playable.random(random)
            GameDifficulty.MEDIUM -> aiChooseCardMedium(game, playable, random)
            GameDifficulty.HARD -> aiChooseCardHard(game, playable, random)
        }
    }

    private fun aiChooseCardMedium(
        game: LoveLetterGame,
        playable: List<LoveLetterCard>,
        random: Random
    ): LoveLetterCard {
        val opponents = game.players.withIndex()
            .filter { (i, p) -> i != game.currentPlayerIndex && !p.isEliminated && !p.isProtected }

        // Prefer Handmaid when there are multiple unprotected opponents
        if (playable.contains(LoveLetterCard.HANDMAID) && opponents.size > 1) {
            return LoveLetterCard.HANDMAID
        }
        // Use Guard if there's a valid target
        if (playable.contains(LoveLetterCard.GUARD) && opponents.isNotEmpty()) {
            return LoveLetterCard.GUARD
        }
        return playable.random(random)
    }

    private fun aiChooseCardHard(
        game: LoveLetterGame,
        playable: List<LoveLetterCard>,
        random: Random
    ): LoveLetterCard {
        val opponents = game.players.withIndex()
            .filter { (i, p) -> i != game.currentPlayerIndex && !p.isEliminated && !p.isProtected }

        // Baron when holding a high card
        if (playable.contains(LoveLetterCard.BARON) && opponents.isNotEmpty()) {
            val myOther = game.currentPlayer.hand.filter { it != LoveLetterCard.BARON }
                .maxByOrNull { it.value }
            if (myOther != null && myOther.value >= 5) return LoveLetterCard.BARON
        }
        // Guard for deduced elimination
        if (playable.contains(LoveLetterCard.GUARD) && opponents.isNotEmpty()) {
            return LoveLetterCard.GUARD
        }
        // Protect when holding a high card
        if (playable.contains(LoveLetterCard.HANDMAID)) {
            val myOther = game.currentPlayer.hand.filter { it != LoveLetterCard.HANDMAID }
                .maxByOrNull { it.value }
            if (myOther != null && myOther.value >= 6) return LoveLetterCard.HANDMAID
        }
        return playable.random(random)
    }

    /**
     * Choose a target player index for [card] played by the AI.
     * Returns null when there are no valid targets (card resolves with no effect).
     */
    fun aiChooseTarget(game: LoveLetterGame, card: LoveLetterCard, random: Random): Int? {
        val targets = validTargets(game, card, game.currentPlayerIndex)
        if (targets.isEmpty()) return null

        return when (game.difficulty) {
            GameDifficulty.EASY -> targets.random(random)
            GameDifficulty.MEDIUM,
            GameDifficulty.HARD -> aiChooseTargetSmart(game, card, targets, random)
        }
    }

    private fun aiChooseTargetSmart(
        game: LoveLetterGame,
        card: LoveLetterCard,
        targets: List<Int>,
        random: Random
    ): Int = when (card) {
        LoveLetterCard.BARON -> {
            // Target the opponent with the fewest discards (likely lower card)
            targets.minByOrNull { game.players[it].discards.size } ?: targets.random(random)
        }
        LoveLetterCard.PRINCE -> {
            // Prefer an opponent over self; fall back to self if needed
            targets.filter { it != game.currentPlayerIndex }.randomOrNull(random)
                ?: targets.first()
        }
        else -> targets.random(random)
    }

    /**
     * Deduce the best Guard guess for [targetIndex] based on visible discards and difficulty.
     * Never guesses Guard (value 1).
     */
    fun aiGuardGuess(game: LoveLetterGame, targetIndex: Int, random: Random): LoveLetterCard {
        val allVisible = game.players.flatMap { it.discards } + game.revealedBurnCards
        val remaining = LoveLetterCard.values().associateWith { card ->
            fullDeck.count { it == card } - allVisible.count { it == card }
        }
        val guessable = LoveLetterCard.values().filter { it != LoveLetterCard.GUARD && (remaining[it] ?: 0) > 0 }
        if (guessable.isEmpty()) return LoveLetterCard.PRIEST

        return when (game.difficulty) {
            GameDifficulty.EASY -> guessable.random(random)
            GameDifficulty.MEDIUM ->
                // Guess the card most likely still in circulation
                guessable.maxByOrNull { remaining[it] ?: 0 } ?: guessable.random(random)
            GameDifficulty.HARD -> {
                // Narrow by what the target has already discarded (they don't hold those)
                val targetDiscarded = game.players[targetIndex].discards.toSet()
                val likelyCards = guessable.filter { it !in targetDiscarded }
                    .ifEmpty { guessable }
                likelyCards.maxByOrNull { remaining[it] ?: 0 } ?: guessable.random(random)
            }
        }
    }

    /**
     * Execute a complete AI turn for [game.currentPlayerIndex]:
     * draw a card, choose what to play, resolve it, and return the new game state.
     */
    fun takeAiTurn(game: LoveLetterGame, random: Random): LoveLetterGame {
        val afterDraw = drawCard(game)
        val card = aiChooseCard(afterDraw, random)

        val needsTarget = when (card) {
            LoveLetterCard.GUARD,
            LoveLetterCard.PRIEST,
            LoveLetterCard.BARON,
            LoveLetterCard.PRINCE,
            LoveLetterCard.KING -> true
            else -> false
        }

        val targetIdx = if (needsTarget) aiChooseTarget(afterDraw, card, random) else null
        val guess = if (card == LoveLetterCard.GUARD && targetIdx != null) {
            aiGuardGuess(afterDraw, targetIdx, random)
        } else null

        return playCard(afterDraw, card, targetIdx, guess)
    }
}
