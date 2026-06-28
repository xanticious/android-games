package com.xanticious.androidgames.controller.games.deckbuilderfleet

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.deckbuilderfleet.FleetCard
import com.xanticious.androidgames.model.games.deckbuilderfleet.FleetConfig
import com.xanticious.androidgames.model.games.deckbuilderfleet.FleetGameState
import com.xanticious.androidgames.model.games.deckbuilderfleet.FortInPlay
import com.xanticious.androidgames.model.games.deckbuilderfleet.PlayerState
import com.xanticious.androidgames.model.games.deckbuilderfleet.ShipClass
import com.xanticious.androidgames.model.games.deckbuilderfleet.SpecialAbility
import com.xanticious.androidgames.model.games.deckbuilderfleet.Winner
import kotlin.math.min
import kotlin.random.Random

/**
 * Pure controller for Deck Builder Fleet. Every function is stateless: it takes
 * immutable model objects and returns new ones. No Android imports, no
 * side-effects; randomness is always injected via [Random].
 */
object DeckBuilderFleetController {

    // ── Config ────────────────────────────────────────────────────────────────

    fun configFor(difficulty: GameDifficulty): FleetConfig = when (difficulty) {
        GameDifficulty.EASY   -> FleetConfig(playerStartHealth = 60, botStartHealth = 40)
        GameDifficulty.MEDIUM -> FleetConfig(playerStartHealth = 50, botStartHealth = 50)
        GameDifficulty.HARD   -> FleetConfig(playerStartHealth = 40, botStartHealth = 60)
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    /** Shuffle decks and deal the initial trade row. */
    fun createInitialState(config: FleetConfig, random: Random = Random.Default): FleetGameState {
        val playerDeck = FleetCardDefs.buildStarterDeck("p").shuffled(random)
        val botDeck    = FleetCardDefs.buildStarterDeck("b").shuffled(random)
        val market     = FleetCardDefs.buildMarketPool().shuffled(random)
        val tradeRow   = market.take(config.tradeRowSize)
        val tradePool  = market.drop(config.tradeRowSize)
        return FleetGameState(
            player       = PlayerState.initial(playerDeck, config.playerStartHealth),
            bot          = PlayerState.initial(botDeck,    config.botStartHealth),
            tradeRow     = tradeRow,
            tradePool    = tradePool,
            isPlayerTurn = true,
            turnCount    = 0
        )
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    /**
     * Draw [n] cards for the active player. When the deck is empty the discard
     * pile is reshuffled into a fresh deck and drawing continues.
     */
    fun drawCards(
        state: FleetGameState,
        n: Int,
        isPlayer: Boolean,
        random: Random = Random.Default
    ): FleetGameState {
        val ps      = if (isPlayer) state.player else state.bot
        var deck    = ps.deck
        var discard = ps.discard
        val drawn   = mutableListOf<FleetCard>()
        var need    = n

        while (need > 0) {
            if (deck.isEmpty()) {
                if (discard.isEmpty()) break   // nothing left to draw
                deck    = discard.shuffled(random)
                discard = emptyList()
            }
            drawn.add(deck.first())
            deck = deck.drop(1)
            need--
        }

        val newPs = ps.copy(deck = deck, discard = discard, hand = ps.hand + drawn)
        return if (isPlayer) state.copy(player = newPs) else state.copy(bot = newPs)
    }

    /** Reset turn resources and draw a hand of 5 for the active player. */
    fun startTurn(state: FleetGameState, random: Random = Random.Default): FleetGameState {
        val reset = state.copy(currentCoins = 0, currentCombat = 0, currentSubCombat = 0)
        return drawCards(reset, 5, reset.isPlayerTurn, random)
    }

    // ── Play ──────────────────────────────────────────────────────────────────

    /**
     * Move one card from the active player's hand into the play area, then
     * recalculate resources.  DRAW_CARD / DRAW_TWO abilities and the carrier
     * synergy bonus draw fire immediately.
     */
    fun playCard(
        state: FleetGameState,
        cardInstanceId: String,
        random: Random = Random.Default
    ): FleetGameState {
        val isPlayer = state.isPlayerTurn
        val ps       = if (isPlayer) state.player else state.bot
        val card     = ps.hand.firstOrNull { it.instanceId == cardInstanceId } ?: return state

        val newHand    = ps.hand.filter { it.instanceId != cardInstanceId }
        val newPlayArea = ps.playArea + card
        val newPs = ps.copy(hand = newHand, playArea = newPlayArea)
        var s = if (isPlayer) state.copy(player = newPs) else state.copy(bot = newPs)

        s = recalculateResources(s, isPlayer)

        // Ability-triggered extra draws
        val abilityDraw = when (card.type.special) {
            SpecialAbility.DRAW_CARD -> 1
            SpecialAbility.DRAW_TWO  -> 2
            else                     -> 0
        }
        // Carrier synergy: fires exactly once when the count crosses from 1 to 2+
        val prevCarriers = ps.playArea.count { it.type.shipClass == ShipClass.AIRCRAFT_CARRIER }
        val newCarriers  = newPlayArea.count { it.type.shipClass == ShipClass.AIRCRAFT_CARRIER }
        val synergyDraw  = if (newCarriers >= 2 && prevCarriers < 2) 1 else 0

        val totalDraw = abilityDraw + synergyDraw
        if (totalDraw > 0) s = drawCards(s, totalDraw, isPlayer, random)

        return s
    }

    // Recompute coins/combat from the full play area every time a card is added.
    private fun recalculateResources(state: FleetGameState, isPlayer: Boolean): FleetGameState {
        val ps          = if (isPlayer) state.player else state.bot
        val playArea    = ps.playArea
        val classCounts = playArea.groupBy { it.type.shipClass }.mapValues { it.value.size }

        var coins     = 0
        var combat    = 0
        var subCombat = 0

        for (card in playArea) {
            // Forts provide no trade/combat when played (their value is their defence)
            if (!card.isFort) {
                coins += card.type.tradeValue
                if (card.type.special == SpecialAbility.BYPASS_FORTS) {
                    subCombat += card.type.combatValue
                } else {
                    combat += card.type.combatValue
                }
            }
        }

        // Class synergy bonuses (trigger once when ≥ 2 of the same class are in play)
        if ((classCounts[ShipClass.ROW_BOAT]  ?: 0) >= 2) coins     += 1
        if ((classCounts[ShipClass.SUBMARINE] ?: 0) >= 2) subCombat += 1
        if ((classCounts[ShipClass.BATTLESHIP] ?: 0) >= 2) combat   += 2

        return state.copy(currentCoins = coins, currentCombat = combat, currentSubCombat = subCombat)
    }

    // ── Buy ───────────────────────────────────────────────────────────────────

    /**
     * Buy the card at [tradeRowIndex] in the trade row. Deducts coins, places
     * the card in the active player's discard, and refills the slot from the
     * pool. Returns the state unchanged when the card is unaffordable or the
     * index is out of range.
     */
    fun buyCard(
        state: FleetGameState,
        tradeRowIndex: Int,
        random: Random = Random.Default
    ): FleetGameState {
        if (tradeRowIndex !in state.tradeRow.indices) return state
        val card = state.tradeRow[tradeRowIndex]
        if (state.currentCoins < card.type.cost) return state

        val isPlayer = state.isPlayerTurn
        val ps       = if (isPlayer) state.player else state.bot
        val newPs    = ps.copy(discard = ps.discard + card)

        val newRow = state.tradeRow.toMutableList().also { it.removeAt(tradeRowIndex) }
        val newPool: List<FleetCard>
        if (state.tradePool.isNotEmpty()) {
            newRow.add(state.tradePool.first())
            newPool = state.tradePool.drop(1)
        } else {
            newPool = emptyList()
        }

        val baseState = if (isPlayer) state.copy(player = newPs) else state.copy(bot = newPs)
        return baseState.copy(
            currentCoins = state.currentCoins - card.type.cost,
            tradeRow     = newRow,
            tradePool    = newPool
        )
    }

    // ── Combat ────────────────────────────────────────────────────────────────

    /**
     * Spend all accumulated combat against the opponent. Normal combat destroys
     * forts in order before dealing health damage; sub-combat skips forts and
     * hits health directly (submarine stealth rule).
     */
    fun applyAllCombat(state: FleetGameState): FleetGameState {
        val isPlayer = state.isPlayerTurn
        val target   = if (isPlayer) state.bot else state.player

        val (newForts, remaining, destroyed) = resolveFortDamage(target.forts, state.currentCombat)
        val healthDamage = remaining + state.currentSubCombat
        val newHealth    = (target.health - healthDamage).coerceAtLeast(0)

        val newTarget = target.copy(
            forts   = newForts,
            health  = newHealth,
            discard = target.discard + destroyed
        )
        val newState = if (isPlayer) state.copy(bot = newTarget) else state.copy(player = newTarget)
        val winnerNow = checkWinner(newState)
        return newState.copy(
            currentCombat    = 0,
            currentSubCombat = 0,
            winner           = winnerNow
        )
    }

    private fun resolveFortDamage(
        forts: List<FortInPlay>,
        normalCombat: Int
    ): Triple<List<FortInPlay>, Int, List<FleetCard>> {
        var remaining = normalCombat
        val surviving = mutableListOf<FortInPlay>()
        val destroyed = mutableListOf<FleetCard>()

        for (fort in forts) {
            if (remaining <= 0) {
                surviving.add(fort)
                continue
            }
            val damage     = min(remaining, fort.remainingDefense)
            remaining     -= damage
            val newDefense = fort.remainingDefense - damage
            if (newDefense <= 0) destroyed.add(fort.card) else surviving.add(fort.copy(remainingDefense = newDefense))
        }
        return Triple(surviving, remaining, destroyed)
    }

    fun checkWinner(state: FleetGameState): Winner? = when {
        state.bot.health    <= 0 -> Winner.PLAYER
        state.player.health <= 0 -> Winner.BOT
        else                     -> null
    }

    // ── End turn ─────────────────────────────────────────────────────────────

    /**
     * Clean up after the active player's turn. Fort cards from the play area
     * become persistent (with a +2 defence bonus if two or more were played
     * this turn). All other played cards and the remaining hand go to the
     * discard. Resources reset and the turn passes to the other side.
     */
    fun endTurn(state: FleetGameState): FleetGameState {
        val isPlayer = state.isPlayerTurn
        val ps       = if (isPlayer) state.player else state.bot

        val fortsPlayed = ps.playArea.filter { it.isFort }
        val fortBonus   = if (fortsPlayed.size >= 2) 2 else 0
        val addedForts  = fortsPlayed.map { FortInPlay(it, it.type.fortDefense + fortBonus) }

        val newDiscard = ps.discard +
            ps.playArea.filter { !it.isFort } +
            ps.hand

        val newPs = ps.copy(
            hand     = emptyList(),
            playArea = emptyList(),
            discard  = newDiscard,
            forts    = ps.forts + addedForts
        )

        val newState = if (isPlayer) state.copy(player = newPs) else state.copy(bot = newPs)
        return newState.copy(
            currentCoins     = 0,
            currentCombat    = 0,
            currentSubCombat = 0,
            isPlayerTurn     = !isPlayer,
            // Full round completes when the bot finishes
            turnCount        = if (!isPlayer) state.turnCount + 1 else state.turnCount
        )
    }

    // ── Bot turn ─────────────────────────────────────────────────────────────

    /**
     * Execute a complete bot turn: draw, play all, buy, attack, end.
     * Fully deterministic when given a seeded [random].
     */
    fun botTurn(
        state: FleetGameState,
        difficulty: GameDifficulty,
        random: Random
    ): FleetGameState {
        require(!state.isPlayerTurn) { "botTurn called on the player's turn" }

        var s = startTurn(state, random)

        // Play every card in hand (order is stable — copy the id list first)
        val handIds = s.bot.hand.map { it.instanceId }
        for (id in handIds) s = playCard(s, id, random)

        s = botBuyPhase(s, difficulty, random)
        s = applyAllCombat(s)
        return endTurn(s)
    }

    private fun botBuyPhase(
        state: FleetGameState,
        difficulty: GameDifficulty,
        random: Random
    ): FleetGameState {
        var s = state
        // Bot may buy multiple cards if it has leftover coins
        repeat(s.tradeRow.size) {
            if (s.currentCoins <= 0) return s
            val choice = selectBotBuy(s, difficulty, random) ?: return s
            val idx = s.tradeRow.indexOfFirst { it.instanceId == choice.instanceId }
            if (idx >= 0) s = buyCard(s, idx, random)
        }
        return s
    }

    private fun selectBotBuy(
        state: FleetGameState,
        difficulty: GameDifficulty,
        @Suppress("UNUSED_PARAMETER") random: Random
    ): FleetCard? {
        val affordable = state.tradeRow.filter { it.type.cost <= state.currentCoins }
        if (affordable.isEmpty()) return null

        return when (difficulty) {
            GameDifficulty.EASY -> {
                // Greedy: buy the most expensive affordable card
                affordable.maxByOrNull { it.type.cost }
            }

            GameDifficulty.MEDIUM -> {
                // Prefer combat when behind on health; otherwise highest total value
                if (state.bot.health < state.player.health) {
                    affordable
                        .filter { it.type.combatValue > 0 || it.type.special == SpecialAbility.BYPASS_FORTS }
                        .maxByOrNull { it.type.combatValue + it.type.cost }
                        ?: affordable.maxByOrNull { it.type.cost }
                } else {
                    affordable.maxByOrNull { it.type.tradeValue + it.type.combatValue }
                }
            }

            GameDifficulty.HARD -> {
                // If current resources already deal lethal, skip buying
                val playerFortHP = state.player.forts.sumOf { it.remainingDefense }
                if (state.currentCombat + state.currentSubCombat >= playerFortHP + state.player.health) {
                    return null
                }
                // Otherwise maximise value + synergy
                val classCounts = state.bot.playArea
                    .groupBy { it.type.shipClass }.mapValues { it.value.size }
                affordable.maxByOrNull { card ->
                    val synergy = if ((classCounts[card.type.shipClass] ?: 0) >= 1) 2 else 0
                    card.type.tradeValue + card.type.combatValue + synergy +
                        if (card.type.special == SpecialAbility.BYPASS_FORTS) 2 else 0
                }
            }
        }
    }
}
