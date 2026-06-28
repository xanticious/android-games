package com.xanticious.androidgames.games.deckbuildersuperhero

import com.xanticious.androidgames.controller.games.deckbuildersuperhero.DeckBuilderCards
import com.xanticious.androidgames.controller.games.deckbuildersuperhero.DeckBuilderController
import com.xanticious.androidgames.controller.games.deckbuildersuperhero.EndCondition
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.deckbuildersuperhero.DeckBuilderState
import com.xanticious.androidgames.model.games.deckbuildersuperhero.DeckCard
import com.xanticious.androidgames.model.games.deckbuildersuperhero.Hero
import com.xanticious.androidgames.model.games.deckbuildersuperhero.HeroArchetype
import com.xanticious.androidgames.model.games.deckbuildersuperhero.Villain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DeckBuilderControllerTest {

    private val controller = DeckBuilderController()

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun baseState(
        difficulty: GameDifficulty = GameDifficulty.EASY,
        humanArchetype: HeroArchetype = HeroArchetype.CULL,
        numBots: Int = 0,
        seed: Long = 1L
    ): DeckBuilderState = controller.buildInitialState(difficulty, humanArchetype, numBots, Random(seed))

    /** Returns a state where the human hero has exactly [cards] as their hand. */
    private fun stateWithHumanHand(base: DeckBuilderState, vararg cards: DeckCard): DeckBuilderState {
        val idx = base.humanHeroIndex
        val hero = base.heroes[idx].copy(hand = cards.toList())
        return base.copy(heroes = base.heroes.mapIndexed { i, h -> if (i == idx) hero else h })
    }

    /** Returns a state with the human's resources set explicitly. */
    private fun stateWithResources(
        base: DeckBuilderState,
        power: Int = 0,
        attack: Int = 0
    ): DeckBuilderState = base.copy(currentPower = power, currentAttack = attack)

    private val copper = DeckCard("copper", "Copper", cost = 0, power = 1, attack = 0, isStarter = true)
    private val strike = DeckCard("strike", "Strike", cost = 0, power = 0, attack = 1, isStarter = true)
    private val echoChain = DeckCard("echo_chain", "Echo Chain", cost = 0, power = 1, attack = 0, extraDraw = 1, isStarter = true)

    // ── Economy: Power from played cards ─────────────────────────────────────

    @Test
    fun playCard_copperCard_increasesPowerByOne() {
        var state = baseState()
        state = stateWithHumanHand(state, copper)
        val result = controller.playCard(state, cardIndex = 0, Random(1))
        assertEquals(1, result.currentPower)
    }

    @Test
    fun playCard_twoCopper_increasesPowerByTwo() {
        var state = baseState()
        state = stateWithHumanHand(state, copper, copper)
        state = controller.playCard(state, 0, Random(1))
        val result = controller.playCard(state, 0, Random(1))
        assertEquals(2, result.currentPower)
    }

    @Test
    fun playCard_strikeCard_doesNotIncreasePower() {
        var state = baseState()
        state = stateWithHumanHand(state, strike)
        val result = controller.playCard(state, 0, Random(1))
        assertEquals(0, result.currentPower)
    }

    // ── Economy: Attack from played cards ────────────────────────────────────

    @Test
    fun playCard_strikeCard_increasesAttackByOne() {
        var state = baseState()
        state = stateWithHumanHand(state, strike)
        val result = controller.playCard(state, 0, Random(1))
        assertEquals(1, result.currentAttack)
    }

    @Test
    fun playCard_copperCard_doesNotIncreaseAttack() {
        var state = baseState()
        state = stateWithHumanHand(state, copper)
        val result = controller.playCard(state, 0, Random(1))
        assertEquals(0, result.currentAttack)
    }

    @Test
    fun playCard_movesCardFromHandToPlayed() {
        var state = baseState()
        state = stateWithHumanHand(state, copper)
        val result = controller.playCard(state, 0, Random(1))
        assertTrue(result.humanHero.hand.isEmpty())
        assertEquals(1, result.playedThisTurn.size)
    }

    @Test
    fun playCard_extraDraw_increasesHandSize() {
        var state = baseState()
        // echoChain has extraDraw=1; deck must have cards to draw from
        val idx = state.humanHeroIndex
        // Ensure the hero's deck has at least one card
        val hero = state.heroes[idx]
        assertTrue("Deck must be non-empty for this test", hero.deck.isNotEmpty())
        state = stateWithHumanHand(state, echoChain)
        val result = controller.playCard(state, 0, Random(1))
        // Hand should have grown by 1 (the extra draw)
        assertEquals(1, result.humanHero.hand.size)
    }

    @Test
    fun playCard_invalidIndex_returnsStateUnchanged() {
        var state = baseState()
        state = stateWithHumanHand(state, copper)
        val result = controller.playCard(state, cardIndex = 99, Random(1))
        assertEquals(state, result)
    }

    // ── Recruiting an ally from the line-up ──────────────────────────────────

    @Test
    fun recruitAlly_affordableCard_addsCardToHumanDiscard() {
        var state = baseState()
        // Find a slot in the recruit row and ensure we have enough power
        val rowIndex = state.recruitRow.indexOfFirst { it != null }
        val card = state.recruitRow[rowIndex]!!
        state = stateWithResources(state, power = card.cost)

        val result = controller.recruitAlly(state, rowIndex)

        assertTrue(result.humanHero.discard.contains(card))
    }

    @Test
    fun recruitAlly_affordableCard_deductsCostFromPower() {
        var state = baseState()
        val rowIndex = state.recruitRow.indexOfFirst { it != null }
        val card = state.recruitRow[rowIndex]!!
        state = stateWithResources(state, power = card.cost + 2)

        val result = controller.recruitAlly(state, rowIndex)

        assertEquals(2, result.currentPower)
    }

    @Test
    fun recruitAlly_unaffordable_doesNotChangeState() {
        val state = baseState()
        val rowIndex = state.recruitRow.indexOfFirst { it != null }
        // With 0 power and a card costing >= 2, should do nothing
        val result = controller.recruitAlly(state, rowIndex)
        assertEquals(state, result)
    }

    @Test
    fun recruitAlly_refillsRowFromSupply() {
        var state = baseState()
        val rowIndex = state.recruitRow.indexOfFirst { it != null }
        val card = state.recruitRow[rowIndex]!!
        state = stateWithResources(state, power = card.cost + 5)

        val supplyTopCard = state.recruitSupply.firstOrNull()
        val result = controller.recruitAlly(state, rowIndex)

        // Slot is refilled with the top of the supply (or null if supply empty)
        assertEquals(supplyTopCard, result.recruitRow[rowIndex])
    }

    @Test
    fun recruitAlly_reducesSupplySizeByOne() {
        var state = baseState()
        val rowIndex = state.recruitRow.indexOfFirst { it != null }
        val card = state.recruitRow[rowIndex]!!
        state = stateWithResources(state, power = card.cost + 5)
        val supplyBefore = state.recruitSupply.size

        val result = controller.recruitAlly(state, rowIndex)

        assertEquals(supplyBefore - 1, result.recruitSupply.size)
    }

    // ── Villain damage + defeat win ───────────────────────────────────────────

    @Test
    fun attackVillain_withAttack_reducesVillainHp() {
        var state = baseState()
        state = stateWithResources(state, attack = 5)
        val hpBefore = state.villain.hp

        val result = controller.attackVillain(state)

        assertEquals(hpBefore - 5, result.villain.hp)
    }

    @Test
    fun attackVillain_zeroAttack_doesNotReduceHp() {
        val state = baseState()
        val hpBefore = state.villain.hp

        val result = controller.attackVillain(state)

        assertEquals(hpBefore, result.villain.hp)
    }

    @Test
    fun attackVillain_zerosOutCurrentAttack() {
        var state = baseState()
        state = stateWithResources(state, attack = 4)

        val result = controller.attackVillain(state)

        assertEquals(0, result.currentAttack)
    }

    @Test
    fun attackVillain_cannotDriveHpBelowZero() {
        var state = baseState()
        state = stateWithResources(state, attack = 9999)

        val result = controller.attackVillain(state)

        assertEquals(0, result.villain.hp)
    }

    @Test
    fun checkEndCondition_villainHpAtZero_returnsWon() {
        var state = baseState()
        state = state.copy(villain = state.villain.copy(hp = 0))

        assertEquals(EndCondition.WON, controller.checkEndCondition(state))
    }

    @Test
    fun checkEndCondition_villainDefeated_returnsWonNotLost() {
        var state = baseState()
        state = stateWithResources(state, attack = state.villain.hp)
        state = controller.attackVillain(state)

        assertNotEquals(EndCondition.LOST, controller.checkEndCondition(state))
    }

    // ── Scheme advancing to completion = loss ────────────────────────────────

    @Test
    fun checkEndCondition_schemeCompleted_returnsLost() {
        var state = baseState()
        state = state.copy(villain = state.villain.copy(schemeProgress = state.villain.schemeTotal))

        assertEquals(EndCondition.LOST, controller.checkEndCondition(state))
    }

    @Test
    fun endPlayerTurn_advancesSchemeByOne() {
        val state = baseState()
        val progressBefore = state.villain.schemeProgress

        val result = controller.endPlayerTurn(state, Random(1))

        assertEquals(progressBefore + 1, result.villain.schemeProgress)
    }

    @Test
    fun endPlayerTurn_schemeAtPenultimate_thenCheckReturnsLost() {
        var state = baseState()
        // Set scheme one step from completion
        state = state.copy(villain = state.villain.copy(schemeProgress = state.villain.schemeTotal - 1))

        val afterTurn = controller.endPlayerTurn(state, Random(1))

        assertEquals(EndCondition.LOST, controller.checkEndCondition(afterTurn))
    }

    @Test
    fun endPlayerTurn_villainAlreadyDefeated_doesNotAdvanceScheme() {
        var state = baseState()
        state = state.copy(villain = state.villain.copy(hp = 0))
        val progressBefore = state.villain.schemeProgress

        val result = controller.endPlayerTurn(state, Random(1))

        assertEquals(progressBefore, result.villain.schemeProgress)
    }

    @Test
    fun endPlayerTurn_incrementsRoundByOne() {
        val state = baseState()
        val roundBefore = state.round

        val result = controller.endPlayerTurn(state, Random(1))

        assertEquals(roundBefore + 1, result.round)
    }

    @Test
    fun endPlayerTurn_discardsHumanHandAndPlayedCards() {
        var state = baseState()
        val idx = state.humanHeroIndex
        state = stateWithHumanHand(state, copper, strike)
        state = state.copy(playedThisTurn = listOf(copper))

        val result = controller.endPlayerTurn(state, Random(1))

        assertTrue(result.heroes[idx].hand.isEmpty())
        // Both hand cards and played card should be in discard
        assertEquals(3, result.heroes[idx].discard.size)
    }

    // ── Ongoing when neither condition met ───────────────────────────────────

    @Test
    fun checkEndCondition_normalGameState_returnsOngoing() {
        val state = baseState()

        assertEquals(EndCondition.ONGOING, controller.checkEndCondition(state))
    }

    // ── Archetype starting-deck differences ──────────────────────────────────

    @Test
    fun starterDeck_cullArchetype_containsMonksClarity() {
        val deck = DeckBuilderCards.starterDeck(HeroArchetype.CULL)
        assertTrue(deck.any { it.id == "monks_clarity" })
    }

    @Test
    fun starterDeck_brawlArchetype_containsClawStrike() {
        val deck = DeckBuilderCards.starterDeck(HeroArchetype.BRAWL)
        assertTrue(deck.any { it.id == "claw_strike" })
    }

    @Test
    fun starterDeck_bankrollArchetype_hasSixGoldCards() {
        val deck = DeckBuilderCards.starterDeck(HeroArchetype.BANKROLL)
        assertEquals(6, deck.count { it.id == "gold" })
    }

    @Test
    fun starterDeck_swarmArchetype_containsEchoChain() {
        val deck = DeckBuilderCards.starterDeck(HeroArchetype.SWARM)
        assertTrue(deck.any { it.id == "echo_chain" })
    }

    @Test
    fun starterDeck_brawlArchetype_hasTotalAttackHigherThanCull() {
        val brawlAttack = DeckBuilderCards.starterDeck(HeroArchetype.BRAWL).sumOf { it.attack }
        val cullAttack  = DeckBuilderCards.starterDeck(HeroArchetype.CULL).sumOf  { it.attack }
        assertTrue(brawlAttack > cullAttack)
    }

    @Test
    fun starterDeck_bankrollArchetype_hasTotalPowerHigherThanBrawl() {
        val bankrollPower = DeckBuilderCards.starterDeck(HeroArchetype.BANKROLL).sumOf { it.power }
        val brawlPower    = DeckBuilderCards.starterDeck(HeroArchetype.BRAWL).sumOf    { it.power }
        assertTrue(bankrollPower > brawlPower)
    }

    @Test
    fun buildInitialState_everyHeroHasTenCardDeck() {
        val state = baseState(numBots = 2, seed = 7)
        for (hero in state.heroes) {
            assertEquals(
                "Hero ${hero.name} should start with 10 cards total",
                10,
                hero.deck.size + hero.hand.size + hero.discard.size
            )
        }
    }

    // ── Deterministic AI hero turn with a fixed seed ─────────────────────────

    @Test
    fun endPlayerTurn_fixedSeed_isDeterministic() {
        val initialState = baseState(numBots = 1, seed = 99)
        val withHand = controller.startRound(initialState, Random(99))

        val result1 = controller.endPlayerTurn(withHand, Random(42))
        val result2 = controller.endPlayerTurn(withHand, Random(42))

        assertEquals(result1, result2)
    }

    @Test
    fun endPlayerTurn_differentSeed_mayProduceDifferentResult() {
        val initialState = baseState(numBots = 1, seed = 99)
        val withHand = controller.startRound(initialState, Random(99))

        // Seeds 1 and 2 might produce the same result if decks don't need
        // reshuffling, but they must not crash and must compile.
        val result1 = controller.endPlayerTurn(withHand, Random(1))
        val result2 = controller.endPlayerTurn(withHand, Random(2))
        // Structural sanity: both produce valid states
        assertTrue(result1.round > initialState.round)
        assertTrue(result2.round > initialState.round)
    }

    @Test
    fun aiTurn_attacksVillain_withPositiveAttackFromHand() {
        // Build a state where the AI hero's deck is full of attack cards
        val initial = baseState(numBots = 1, humanArchetype = HeroArchetype.CULL, seed = 3)
        val aiIndex = initial.heroes.indexOfFirst { !it.isHuman }
        val aiHero = initial.heroes[aiIndex]

        // Force AI deck to be pure strike cards (all-attack)
        val allStrikes = List(10) { i -> DeckCard("s$i", "Strike", cost = 0, power = 0, attack = 1, isStarter = true) }
        val aiWithStrikes = aiHero.copy(deck = allStrikes)
        val stateWithAiStrikes = initial.copy(
            heroes = initial.heroes.mapIndexed { i, h -> if (i == aiIndex) aiWithStrikes else h }
        )
        val villainHpBefore = stateWithAiStrikes.villain.hp

        val result = controller.endPlayerTurn(stateWithAiStrikes, Random(5))

        assertTrue("AI should deal damage with strike cards", result.villain.hp < villainHpBefore)
    }

    // ── Difficulty scaling ────────────────────────────────────────────────────

    @Test
    fun buildInitialState_hardDifficulty_villainHasMoreHpThanEasy() {
        val easy = baseState(difficulty = GameDifficulty.EASY)
        val hard = baseState(difficulty = GameDifficulty.HARD)
        assertTrue(hard.villain.hp > easy.villain.hp)
    }

    @Test
    fun buildInitialState_hardDifficulty_schemeTotalSmallerThanEasy() {
        val easy = baseState(difficulty = GameDifficulty.EASY)
        val hard = baseState(difficulty = GameDifficulty.HARD)
        assertTrue(hard.villain.schemeTotal < easy.villain.schemeTotal)
    }

    @Test
    fun buildInitialState_recruitRowHasSixSlots() {
        val state = baseState()
        assertEquals(DeckBuilderController.RECRUIT_ROW_SIZE, state.recruitRow.size)
    }

    // ── startRound ───────────────────────────────────────────────────────────

    @Test
    fun startRound_humanHandHasFiveCards() {
        val state = baseState()
        val result = controller.startRound(state, Random(1))
        assertEquals(DeckBuilderController.HAND_SIZE, result.humanHero.hand.size)
    }

    @Test
    fun startRound_resetsCurrentPowerToZero() {
        val state = stateWithResources(baseState(), power = 5)
        val result = controller.startRound(state, Random(1))
        assertEquals(0, result.currentPower)
    }

    @Test
    fun startRound_resetsCurrentAttackToZero() {
        val state = stateWithResources(baseState(), attack = 3)
        val result = controller.startRound(state, Random(1))
        assertEquals(0, result.currentAttack)
    }
}
