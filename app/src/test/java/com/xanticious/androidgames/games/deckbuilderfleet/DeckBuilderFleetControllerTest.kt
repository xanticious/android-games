package com.xanticious.androidgames.games.deckbuilderfleet

import com.xanticious.androidgames.controller.games.deckbuilderfleet.DeckBuilderFleetController
import com.xanticious.androidgames.controller.games.deckbuilderfleet.FleetCardDefs
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.deckbuilderfleet.FleetCard
import com.xanticious.androidgames.model.games.deckbuilderfleet.FleetConfig
import com.xanticious.androidgames.model.games.deckbuilderfleet.FleetGameState
import com.xanticious.androidgames.model.games.deckbuilderfleet.FortInPlay
import com.xanticious.androidgames.model.games.deckbuilderfleet.PlayerState
import com.xanticious.androidgames.model.games.deckbuilderfleet.Winner
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckBuilderFleetControllerTest {

    private val ctrl = DeckBuilderFleetController

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun emptyPlayer(health: Int = 50) = PlayerState(
        deck     = emptyList(),
        hand     = emptyList(),
        discard  = emptyList(),
        playArea = emptyList(),
        forts    = emptyList(),
        health   = health
    )

    /** A minimal game state with a one-card player hand and the given trade row. */
    private fun stateWithHand(
        hand:      List<FleetCard> = emptyList(),
        tradeRow:  List<FleetCard> = emptyList(),
        tradePool: List<FleetCard> = emptyList(),
        playerHealth: Int = 50,
        botHealth:    Int = 50
    ) = FleetGameState(
        player    = emptyPlayer(playerHealth).copy(hand = hand),
        bot       = emptyPlayer(botHealth),
        tradeRow  = tradeRow,
        tradePool = tradePool,
        isPlayerTurn = true
    )

    private fun card(type: com.xanticious.androidgames.model.games.deckbuilderfleet.FleetCardType, id: String) =
        FleetCard(type, id)

    // ── Trade generation ──────────────────────────────────────────────────────

    @Test
    fun playRowBoat_generatesOneTrade() {
        val rb    = card(FleetCardDefs.ROW_BOAT_STARTER, "rb1")
        val state = stateWithHand(hand = listOf(rb))
        val after = ctrl.playCard(state, "rb1")
        assertEquals(1, after.currentCoins)
    }

    @Test
    fun playMerchantCutter_generatesThreeTrade() {
        val mc    = card(FleetCardDefs.MERCHANT_CUTTER, "mc1")
        val state = stateWithHand(hand = listOf(mc))
        val after = ctrl.playCard(state, "mc1")
        assertEquals(3, after.currentCoins)
    }

    @Test
    fun playTwoRowBoats_synergyAddsOneTrade() {
        val rb1   = card(FleetCardDefs.ROW_BOAT_STARTER, "rb1")
        val rb2   = card(FleetCardDefs.ROW_BOAT_STARTER, "rb2")
        val state = stateWithHand(hand = listOf(rb1, rb2))
        val after = ctrl.playCard(ctrl.playCard(state, "rb1"), "rb2")
        // 1 + 1 (base) + 1 (synergy) = 3
        assertEquals(3, after.currentCoins)
    }

    // ── Combat generation ─────────────────────────────────────────────────────

    @Test
    fun playDestroyer_generatesFourCombat() {
        val d     = card(FleetCardDefs.DESTROYER, "d1")
        val state = stateWithHand(hand = listOf(d))
        val after = ctrl.playCard(state, "d1")
        assertEquals(4, after.currentCombat)
        assertEquals(0, after.currentSubCombat)
    }

    @Test
    fun playScoutSub_generatesSubCombat_notNormalCombat() {
        val sub   = card(FleetCardDefs.SCOUT_SUB, "s1")
        val state = stateWithHand(hand = listOf(sub))
        val after = ctrl.playCard(state, "s1")
        assertEquals(0, after.currentCombat)
        assertEquals(2, after.currentSubCombat)
    }

    @Test
    fun playTwoBattleships_synergyAddsToNormalCombat() {
        val d1    = card(FleetCardDefs.DESTROYER, "d1")
        val d2    = card(FleetCardDefs.DESTROYER, "d2")
        val state = stateWithHand(hand = listOf(d1, d2))
        val after = ctrl.playCard(ctrl.playCard(state, "d1"), "d2")
        // 4 + 4 + 2 (synergy) = 10
        assertEquals(10, after.currentCombat)
    }

    @Test
    fun playTwoSubs_synergyAddsOneSubCombat() {
        val s1    = card(FleetCardDefs.SCOUT_SUB, "s1")
        val s2    = card(FleetCardDefs.SCOUT_SUB, "s2")
        val state = stateWithHand(hand = listOf(s1, s2))
        val after = ctrl.playCard(ctrl.playCard(state, "s1"), "s2")
        // 2 + 2 + 1 (synergy) = 5
        assertEquals(5, after.currentSubCombat)
    }

    // ── Carrier draw ability ──────────────────────────────────────────────────

    @Test
    fun playLightCarrier_drawsOneExtraCard() {
        val carrier = card(FleetCardDefs.LIGHT_CARRIER, "lc1")
        val spare   = card(FleetCardDefs.ROW_BOAT_STARTER, "spare1")
        val state   = stateWithHand(hand = listOf(carrier)).let { s ->
            s.copy(player = s.player.copy(deck = listOf(spare)))
        }
        val after = ctrl.playCard(state, "lc1", Random(0))
        // carrier moves to playArea, spare drawn → hand should have spare
        assertEquals(1, after.player.hand.size)
        assertEquals("spare1", after.player.hand.first().instanceId)
    }

    @Test
    fun playFleetCarrier_drawsTwoExtraCards() {
        val carrier  = card(FleetCardDefs.FLEET_CARRIER, "fc1")
        val spare1   = card(FleetCardDefs.ROW_BOAT_STARTER, "spare1")
        val spare2   = card(FleetCardDefs.ROW_BOAT_STARTER, "spare2")
        val state    = stateWithHand(hand = listOf(carrier)).let { s ->
            s.copy(player = s.player.copy(deck = listOf(spare1, spare2)))
        }
        val after = ctrl.playCard(state, "fc1", Random(0))
        assertEquals(2, after.player.hand.size)
    }

    // ── Buying ────────────────────────────────────────────────────────────────

    @Test
    fun buyCard_spendsCoinsMoveCardToPlayerDiscard() {
        val skiff = card(FleetCardDefs.SKIFF, "sk1")
        val state = stateWithHand(tradeRow = listOf(skiff)).copy(currentCoins = 3)
        val after = ctrl.buyCard(state, 0)
        assertEquals(1, after.currentCoins)       // 3 - 2 = 1
        assertTrue(after.player.discard.any { it.instanceId == "sk1" })
        assertTrue(after.tradeRow.none { it.instanceId == "sk1" })
    }

    @Test
    fun buyCard_refillsTradeRowFromPool() {
        val skiff  = card(FleetCardDefs.SKIFF, "sk1")
        val extra  = card(FleetCardDefs.DESTROYER, "extra1")
        val state  = stateWithHand(tradeRow = listOf(skiff), tradePool = listOf(extra)).copy(currentCoins = 5)
        val after  = ctrl.buyCard(state, 0)
        assertEquals(1, after.tradeRow.size)
        assertEquals("extra1", after.tradeRow.first().instanceId)
        assertTrue(after.tradePool.isEmpty())
    }

    @Test
    fun buyCard_failsWhenInsufficientCoins() {
        val battlecruiser = card(FleetCardDefs.BATTLECRUISER, "bc1")  // cost 6
        val state         = stateWithHand(tradeRow = listOf(battlecruiser)).copy(currentCoins = 3)
        val after         = ctrl.buyCard(state, 0)
        // State unchanged — still has the card in trade row, coins intact
        assertEquals(3, after.currentCoins)
        assertTrue(after.tradeRow.any { it.instanceId == "bc1" })
    }

    @Test
    fun buyCard_outOfRangeIndex_returnsUnchangedState() {
        val state = stateWithHand(tradeRow = emptyList()).copy(currentCoins = 10)
        val after = ctrl.buyCard(state, 5)
        assertEquals(10, after.currentCoins)
    }

    // ── Deck reshuffle ────────────────────────────────────────────────────────

    @Test
    fun drawCards_reshufflesDiscardIntoDeckWhenDeckEmpty() {
        val card1 = card(FleetCardDefs.ROW_BOAT_STARTER, "r1")
        val card2 = card(FleetCardDefs.ROW_BOAT_STARTER, "r2")
        val state = stateWithHand().let { s ->
            s.copy(player = s.player.copy(deck = emptyList(), discard = listOf(card1, card2)))
        }
        val after = ctrl.drawCards(state, 2, isPlayer = true, random = Random(42))
        assertEquals(2, after.player.hand.size)
        assertTrue(after.player.discard.isEmpty())
    }

    @Test
    fun drawCards_doesNotCrashWhenBothDeckAndDiscardEmpty() {
        val state = stateWithHand()   // deck and discard both empty
        val after = ctrl.drawCards(state, 5, isPlayer = true, random = Random(0))
        assertEquals(0, after.player.hand.size)
    }

    // ── Combat & fort resolution ──────────────────────────────────────────────

    @Test
    fun applyAllCombat_reducesBotHealthDirectlyWhenNoForts() {
        val state = stateWithHand(botHealth = 30).copy(currentCombat = 5)
        val after = ctrl.applyAllCombat(state)
        assertEquals(25, after.bot.health)
    }

    @Test
    fun applyAllCombat_destroysFortBeforeHealth() {
        val fortCard  = card(FleetCardDefs.COASTAL_BATTERY, "cb1")
        val fortInPlay = FortInPlay(fortCard, 6)  // 6 HP fort
        val state = stateWithHand(botHealth = 30)
            .copy(
                bot = emptyPlayer(30).copy(forts = listOf(fortInPlay)),
                currentCombat = 10
            )
        val after = ctrl.applyAllCombat(state)
        // 6 absorbed by fort, 4 overflow hits health: 30 - 4 = 26
        assertEquals(26, after.bot.health)
        assertTrue(after.bot.forts.isEmpty())
    }

    @Test
    fun applyAllCombat_subCombatBypassesFort() {
        val fortCard   = card(FleetCardDefs.SEA_FORTRESS, "sf1")
        val fortInPlay = FortInPlay(fortCard, 18)
        val state = stateWithHand(botHealth = 40)
            .copy(
                bot = emptyPlayer(40).copy(forts = listOf(fortInPlay)),
                currentSubCombat = 7
            )
        val after = ctrl.applyAllCombat(state)
        // sub-combat bypasses fort; fort stays intact
        assertEquals(33, after.bot.health)
        assertEquals(18, after.bot.forts.first().remainingDefense)
    }

    @Test
    fun applyAllCombat_destroyedFortMovesToBotDiscard() {
        val fortCard   = card(FleetCardDefs.COASTAL_BATTERY, "cb1")
        val fortInPlay = FortInPlay(fortCard, 4)
        val state = stateWithHand(botHealth = 40)
            .copy(
                bot = emptyPlayer(40).copy(forts = listOf(fortInPlay)),
                currentCombat = 10
            )
        val after = ctrl.applyAllCombat(state)
        assertTrue(after.bot.forts.isEmpty())
        assertTrue(after.bot.discard.any { it.instanceId == "cb1" })
    }

    @Test
    fun applyAllCombat_partiallyDamagesFortWithoutDestroying() {
        val fortCard   = card(FleetCardDefs.SEA_FORTRESS, "sf1")
        val fortInPlay = FortInPlay(fortCard, 18)
        val state = stateWithHand(botHealth = 40)
            .copy(
                bot = emptyPlayer(40).copy(forts = listOf(fortInPlay)),
                currentCombat = 5
            )
        val after = ctrl.applyAllCombat(state)
        assertEquals(1, after.bot.forts.size)
        assertEquals(13, after.bot.forts.first().remainingDefense)
        assertEquals(40, after.bot.health)   // no overflow damage
    }

    // ── Win detection ─────────────────────────────────────────────────────────

    @Test
    fun checkWinner_returnsPlayerWhenBotHealthIsZero() {
        val state = stateWithHand(botHealth = 0)
        assertEquals(Winner.PLAYER, ctrl.checkWinner(state))
    }

    @Test
    fun checkWinner_returnsBotWhenPlayerHealthIsZero() {
        val state = stateWithHand(playerHealth = 0)
        assertEquals(Winner.BOT, ctrl.checkWinner(state))
    }

    @Test
    fun checkWinner_returnsNullWhenBothHealthy() {
        val state = stateWithHand(playerHealth = 20, botHealth = 15)
        assertNull(ctrl.checkWinner(state))
    }

    @Test
    fun applyAllCombat_setsWinnerWhenBotHealthReachesZero() {
        val state = stateWithHand(botHealth = 3).copy(currentCombat = 5)
        val after = ctrl.applyAllCombat(state)
        assertEquals(Winner.PLAYER, after.winner)
        assertEquals(0, after.bot.health)
    }

    // ── End turn ──────────────────────────────────────────────────────────────

    @Test
    fun endTurn_movesPlayedCardsToDiscard() {
        val rb    = card(FleetCardDefs.ROW_BOAT_STARTER, "rb1")
        val state = stateWithHand().copy(
            player = emptyPlayer().copy(playArea = listOf(rb))
        )
        val after = ctrl.endTurn(state)
        assertTrue(after.player.discard.any { it.instanceId == "rb1" })
        assertTrue(after.player.playArea.isEmpty())
    }

    @Test
    fun endTurn_remainingHandGoesToDiscard() {
        val rb    = card(FleetCardDefs.ROW_BOAT_STARTER, "rb1")
        val state = stateWithHand(hand = listOf(rb))
        val after = ctrl.endTurn(state)
        assertTrue(after.player.discard.any { it.instanceId == "rb1" })
        assertTrue(after.player.hand.isEmpty())
    }

    @Test
    fun endTurn_fortCardBecomesActiveFortNotDiscard() {
        val fortCard = card(FleetCardDefs.COASTAL_BATTERY, "cb1")
        val state    = stateWithHand().copy(
            player = emptyPlayer().copy(playArea = listOf(fortCard))
        )
        val after = ctrl.endTurn(state)
        assertTrue(after.player.forts.any { it.card.instanceId == "cb1" })
        assertTrue(after.player.discard.none { it.instanceId == "cb1" })
    }

    @Test
    fun endTurn_switchesTurnToBot() {
        val state = stateWithHand()
        val after = ctrl.endTurn(state)
        assertEquals(false, after.isPlayerTurn)
    }

    @Test
    fun endTurn_incrementsTurnCountAfterBotTurn() {
        // Simulate bot ending their turn
        val botState = stateWithHand().copy(isPlayerTurn = false, turnCount = 0)
        val after    = ctrl.endTurn(botState)
        assertEquals(1, after.turnCount)
    }

    @Test
    fun endTurn_doesNotIncrementTurnCountAfterPlayerTurn() {
        val state = stateWithHand().copy(isPlayerTurn = true, turnCount = 0)
        val after = ctrl.endTurn(state)
        assertEquals(0, after.turnCount)
    }

    // ── Full bot turn (determinism) ───────────────────────────────────────────

    @Test
    fun botTurn_withSameSeed_producesIdenticalPlayerHealth() {
        val config = ctrl.configFor(GameDifficulty.EASY)
        val base   = ctrl.createInitialState(config, Random(1))
        // Flip to bot turn
        val botState = base.copy(isPlayerTurn = false)
        val result1  = ctrl.botTurn(botState, GameDifficulty.EASY, Random(42))
        val result2  = ctrl.botTurn(botState, GameDifficulty.EASY, Random(42))
        assertEquals(result1.player.health, result2.player.health)
    }

    @Test
    fun botTurn_withSameSeed_producesIdenticalBotDiscardSize() {
        val config   = ctrl.configFor(GameDifficulty.MEDIUM)
        val base     = ctrl.createInitialState(config, Random(7))
        val botState = base.copy(isPlayerTurn = false)
        val result1  = ctrl.botTurn(botState, GameDifficulty.MEDIUM, Random(99))
        val result2  = ctrl.botTurn(botState, GameDifficulty.MEDIUM, Random(99))
        assertEquals(result1.bot.discard.size, result2.bot.discard.size)
    }

    @Test
    fun botTurn_endsTurnWithIsPlayerTurnTrue() {
        val config   = ctrl.configFor(GameDifficulty.EASY)
        val base     = ctrl.createInitialState(config, Random(5))
        val botState = base.copy(isPlayerTurn = false)
        val result   = ctrl.botTurn(botState, GameDifficulty.EASY, Random(0))
        assertTrue(result.isPlayerTurn)
    }

    @Test
    fun botTurn_botHandEmptyAfterTurn() {
        val config   = ctrl.configFor(GameDifficulty.EASY)
        val base     = ctrl.createInitialState(config, Random(3))
        val botState = base.copy(isPlayerTurn = false)
        val result   = ctrl.botTurn(botState, GameDifficulty.EASY, Random(0))
        assertTrue(result.bot.hand.isEmpty())
    }

    // ── Config ────────────────────────────────────────────────────────────────

    @Test
    fun configFor_easy_givesPlayerMoreHealth() {
        val config = ctrl.configFor(GameDifficulty.EASY)
        assertTrue(config.playerStartHealth > config.botStartHealth)
    }

    @Test
    fun configFor_hard_givesBotMoreHealth() {
        val config = ctrl.configFor(GameDifficulty.HARD)
        assertTrue(config.botStartHealth > config.playerStartHealth)
    }

    @Test
    fun configFor_medium_equalHealth() {
        val config = ctrl.configFor(GameDifficulty.MEDIUM)
        assertEquals(config.playerStartHealth, config.botStartHealth)
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun createInitialState_tradeRowHasFiveCards() {
        val state = ctrl.createInitialState(FleetConfig(), Random(0))
        assertEquals(5, state.tradeRow.size)
    }

    @Test
    fun createInitialState_playerAndBotStartWithSevenCards() {
        val state     = ctrl.createInitialState(FleetConfig(), Random(0))
        val playerAll = state.player.deck.size + state.player.hand.size + state.player.discard.size
        val botAll    = state.bot.deck.size + state.bot.hand.size + state.bot.discard.size
        assertEquals(7, playerAll)
        assertEquals(7, botAll)
    }

    @Test
    fun startTurn_drawsFiveCardsForActivePlayer() {
        val config = FleetConfig()
        val state  = ctrl.createInitialState(config, Random(0))
        val after  = ctrl.startTurn(state, Random(0))
        assertEquals(5, after.player.hand.size)
    }
}
