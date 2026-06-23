package com.xanticious.androidgames.controller.games.deckbuildersuperhero

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.deckbuildersuperhero.DeckBuilderState
import com.xanticious.androidgames.model.games.deckbuildersuperhero.DeckCard
import com.xanticious.androidgames.model.games.deckbuildersuperhero.Hero
import com.xanticious.androidgames.model.games.deckbuildersuperhero.HeroArchetype
import com.xanticious.androidgames.model.games.deckbuildersuperhero.Villain
import kotlin.random.Random

/** Result of [DeckBuilderController.checkEndCondition]. */
enum class EndCondition { ONGOING, WON, LOST }

/**
 * Pure cooperative deck-building game engine.
 *
 * All functions are pure: they accept an immutable [DeckBuilderState] and
 * return a new one. Randomness is always injected via a [Random] parameter so
 * tests can be deterministic.
 *
 * Turn flow (per round):
 * 1. [startRound]     — draw the human hero's opening hand.
 * 2. [playCard]       — human plays a card, gaining Power/Attack/draws.
 * 3. [recruitAlly]    — human spends Power to recruit from the Recruit Row.
 * 4. [attackVillain]  — human commits all accumulated Attack against the villain.
 * 5. [endPlayerTurn]  — discard hand, run all AI hero turns, advance villain scheme.
 * 6. [checkEndCondition] — WON / LOST / ONGOING.
 */
class DeckBuilderController {

    companion object {
        const val HAND_SIZE = 5
        const val RECRUIT_ROW_SIZE = 6

        private val heroNames = mapOf(
            HeroArchetype.CULL     to "Monk",
            HeroArchetype.BRAWL    to "Tigerman",
            HeroArchetype.SWARM    to "Echo",
            HeroArchetype.BANKROLL to "Bling",
            HeroArchetype.DEFENSE  to "Aegis",
            HeroArchetype.TEMPO    to "Volt"
        )
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    /**
     * Constructs a fresh game state.
     *
     * @param humanArchetype Archetype chosen by the human player.
     * @param numBots        0–3 AI teammates.
     */
    fun buildInitialState(
        difficulty: GameDifficulty,
        humanArchetype: HeroArchetype,
        numBots: Int,
        random: Random
    ): DeckBuilderState {
        val human = Hero(
            name = heroNames.getValue(humanArchetype),
            archetype = humanArchetype,
            isHuman = true,
            deck = DeckBuilderCards.starterDeck(humanArchetype).shuffled(random),
            hand = emptyList(),
            discard = emptyList()
        )

        // Assign distinct archetypes to bots (exclude the human's archetype).
        val botArchetypes = HeroArchetype.entries
            .filter { it != humanArchetype }
            .shuffled(random)
            .take(numBots.coerceIn(0, 3))

        val bots = botArchetypes.map { arch ->
            Hero(
                name = heroNames.getValue(arch),
                archetype = arch,
                isHuman = false,
                deck = DeckBuilderCards.starterDeck(arch).shuffled(random),
                hand = emptyList(),
                discard = emptyList()
            )
        }

        val (villainHp, schemeTotal) = villainStats(difficulty)
        val villain = Villain(
            name = "The Hollow",
            maxHp = villainHp,
            hp = villainHp,
            schemeTotal = schemeTotal,
            schemeProgress = 0
        )

        val shuffledSupply = DeckBuilderCards.allRecruitCards.shuffled(random)
        val initialRow: List<DeckCard?> = shuffledSupply.take(RECRUIT_ROW_SIZE)
        val remainingSupply = shuffledSupply.drop(RECRUIT_ROW_SIZE)

        return DeckBuilderState(
            heroes = listOf(human) + bots,
            recruitRow = initialRow,
            recruitSupply = remainingSupply,
            villain = villain,
            currentPower = 0,
            currentAttack = 0,
            playedThisTurn = emptyList(),
            round = 1,
            difficulty = difficulty
        )
    }

    private fun villainStats(difficulty: GameDifficulty): Pair<Int, Int> = when (difficulty) {
        GameDifficulty.EASY   -> Pair(18, 8)
        GameDifficulty.MEDIUM -> Pair(22, 7)
        GameDifficulty.HARD   -> Pair(28, 6)
    }

    // ── Round start ───────────────────────────────────────────────────────────

    /**
     * Draws a fresh hand for the human hero, resetting round resources.
     * Called at the start of each round before the human takes any actions.
     */
    fun startRound(state: DeckBuilderState, random: Random): DeckBuilderState {
        val humanIndex = state.humanHeroIndex
        val (heroWithHand, _) = drawCards(state.heroes[humanIndex], HAND_SIZE, random)
        return state.copy(
            heroes = state.heroes.mapIndexed { i, h -> if (i == humanIndex) heroWithHand else h },
            currentPower = 0,
            currentAttack = 0,
            playedThisTurn = emptyList()
        )
    }

    // ── Human actions ─────────────────────────────────────────────────────────

    /**
     * Plays a card from the human hero's hand.
     *
     * The card is moved to the played-this-turn area; Power/Attack are added to
     * the round totals; any [DeckCard.extraDraw] cards are drawn immediately.
     * Returns [state] unchanged if [cardIndex] is out of range.
     */
    fun playCard(state: DeckBuilderState, cardIndex: Int, random: Random): DeckBuilderState {
        val humanIndex = state.humanHeroIndex
        val hero = state.heroes[humanIndex]
        if (cardIndex !in hero.hand.indices) return state

        val card = hero.hand[cardIndex]
        val newHand = hero.hand.filterIndexed { i, _ -> i != cardIndex }
        var updatedHero = hero.copy(hand = newHand)

        if (card.extraDraw > 0) {
            val (heroAfterDraw, _) = drawCards(updatedHero, card.extraDraw, random)
            updatedHero = heroAfterDraw
        }

        return state.copy(
            heroes = state.heroes.mapIndexed { i, h -> if (i == humanIndex) updatedHero else h },
            currentPower = state.currentPower + card.power,
            currentAttack = state.currentAttack + card.attack,
            playedThisTurn = state.playedThisTurn + card
        )
    }

    /**
     * Recruits an ally from [DeckBuilderState.recruitRow] at [rowIndex].
     *
     * Costs [DeckCard.cost] Power; the ally goes to the human's discard pile.
     * The row slot is refilled from [DeckBuilderState.recruitSupply] if available.
     * Returns [state] unchanged if unaffordable or slot is empty.
     */
    fun recruitAlly(state: DeckBuilderState, rowIndex: Int): DeckBuilderState {
        if (rowIndex !in state.recruitRow.indices) return state
        val card = state.recruitRow[rowIndex] ?: return state
        if (state.currentPower < card.cost) return state

        val humanIndex = state.humanHeroIndex
        val hero = state.heroes[humanIndex]
        val heroWithAlly = hero.copy(discard = hero.discard + card)

        val newSupplyCard = state.recruitSupply.firstOrNull()
        val newRow = state.recruitRow.mapIndexed { i, c -> if (i == rowIndex) newSupplyCard else c }
        val newSupply = state.recruitSupply.drop(1)

        return state.copy(
            heroes = state.heroes.mapIndexed { i, h -> if (i == humanIndex) heroWithAlly else h },
            recruitRow = newRow,
            recruitSupply = newSupply,
            currentPower = state.currentPower - card.cost
        )
    }

    /**
     * Commits all accumulated [DeckBuilderState.currentAttack] against the villain.
     * Attack is zeroed out after spending. Returns [state] unchanged if attack is 0.
     */
    fun attackVillain(state: DeckBuilderState): DeckBuilderState {
        if (state.currentAttack <= 0) return state
        val newHp = (state.villain.hp - state.currentAttack).coerceAtLeast(0)
        return state.copy(
            villain = state.villain.copy(hp = newHp),
            currentAttack = 0
        )
    }

    // ── End of player turn ────────────────────────────────────────────────────

    /**
     * Concludes the human's turn:
     * 1. Discards the human's hand and played cards.
     * 2. Runs each AI hero's turn (draw, play, buy, attack).
     * 3. Advances the villain's scheme by 1 (if not already defeated).
     * 4. Increments the round counter.
     *
     * Does NOT draw a new hand; call [startRound] next to begin a new round.
     */
    fun endPlayerTurn(state: DeckBuilderState, random: Random): DeckBuilderState {
        val humanIndex = state.humanHeroIndex
        val human = state.heroes[humanIndex]
        val humanAfterTurn = human.copy(
            discard = human.discard + human.hand + state.playedThisTurn,
            hand = emptyList()
        )
        var s = state.copy(
            heroes = state.heroes.mapIndexed { i, h -> if (i == humanIndex) humanAfterTurn else h },
            currentPower = 0,
            currentAttack = 0,
            playedThisTurn = emptyList()
        )

        // All AI teammates take their turns in order.
        for (i in s.heroes.indices) {
            if (!s.heroes[i].isHuman) {
                s = runAiTurn(s, i, random)
            }
        }

        // Villain scheme advances once per round (unless already defeated).
        if (!s.villain.isDefeated) {
            s = s.copy(villain = s.villain.copy(schemeProgress = s.villain.schemeProgress + 1))
        }

        return s.copy(round = s.round + 1)
    }

    // ── End-condition check ───────────────────────────────────────────────────

    /**
     * Returns the current end-condition without mutating state.
     * Call after [endPlayerTurn] to decide whether to continue or show results.
     */
    fun checkEndCondition(state: DeckBuilderState): EndCondition = when {
        state.villain.isDefeated     -> EndCondition.WON
        state.villain.schemeCompleted -> EndCondition.LOST
        else                          -> EndCondition.ONGOING
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Draws up to [count] cards for [hero].
     * If the deck runs dry, the discard pile is shuffled back in (uses [random]).
     * Returns (updated hero, drawn cards).
     */
    private fun drawCards(hero: Hero, count: Int, random: Random): Pair<Hero, List<DeckCard>> {
        val deck = hero.deck.toMutableList()
        val discard = hero.discard.toMutableList()
        val drawn = mutableListOf<DeckCard>()

        repeat(count) {
            if (deck.isEmpty()) {
                if (discard.isEmpty()) return@repeat   // truly out of cards
                deck += discard.shuffled(random)
                discard.clear()
            }
            drawn += deck.removeAt(0)
        }

        return Pair(hero.copy(deck = deck, discard = discard, hand = hero.hand + drawn), drawn)
    }

    /**
     * Runs a full automated turn for the AI hero at [heroIndex].
     *
     * Strategy scales with [DeckBuilderState.difficulty]:
     * - EASY   → buy the cheapest affordable ally.
     * - MEDIUM → buy the most expensive affordable ally.
     * - HARD   → buy as many allies as possible starting with most expensive.
     */
    private fun runAiTurn(state: DeckBuilderState, heroIndex: Int, random: Random): DeckBuilderState {
        val hero = state.heroes[heroIndex]

        // Draw hand.
        val (heroWithHand, drawnCards) = drawCards(hero, HAND_SIZE, random)

        // Tally resources from drawn cards.
        var aiPower = drawnCards.sumOf { it.power }
        var aiAttack = drawnCards.sumOf { it.attack }

        // Process extra-draw cards.
        var currentHero = heroWithHand
        val bonusCount = drawnCards.sumOf { it.extraDraw }
        if (bonusCount > 0) {
            val (heroAfterBonus, bonusCards) = drawCards(
                currentHero.copy(hand = emptyList(), discard = currentHero.discard + drawnCards),
                bonusCount,
                random
            )
            aiPower  += bonusCards.sumOf { it.power }
            aiAttack += bonusCards.sumOf { it.attack }
            currentHero = heroAfterBonus.copy(
                hand = emptyList(),
                discard = heroAfterBonus.discard + bonusCards
            )
        } else {
            // Discard drawn hand into discard pile.
            currentHero = currentHero.copy(
                hand = emptyList(),
                discard = currentHero.discard + drawnCards
            )
        }

        var s = state.copy(
            heroes = state.heroes.mapIndexed { i, h -> if (i == heroIndex) currentHero else h }
        )

        // Buy allies according to difficulty.
        s = when (state.difficulty) {
            GameDifficulty.EASY   -> aiRecruit(s, heroIndex, aiPower, cheapestFirst = true,  limit = 1)
            GameDifficulty.MEDIUM -> aiRecruit(s, heroIndex, aiPower, cheapestFirst = false, limit = 1)
            GameDifficulty.HARD   -> aiRecruit(s, heroIndex, aiPower, cheapestFirst = false, limit = Int.MAX_VALUE)
        }

        // Attack villain with all accumulated attack.
        if (aiAttack > 0) {
            val newHp = (s.villain.hp - aiAttack).coerceAtLeast(0)
            s = s.copy(villain = s.villain.copy(hp = newHp))
        }

        return s
    }

    /**
     * Recruits allies from the row on behalf of an AI hero.
     * Candidates are sorted cheapest-first or most-expensive-first depending on [cheapestFirst].
     * At most [limit] purchases are made; stops when [aiPower] is exhausted.
     */
    private fun aiRecruit(
        state: DeckBuilderState,
        heroIndex: Int,
        aiPower: Int,
        cheapestFirst: Boolean,
        limit: Int
    ): DeckBuilderState {
        var s = state
        var remaining = aiPower
        var bought = 0

        val candidates = s.recruitRow
            .mapIndexedNotNull { i, card -> if (card != null && card.cost <= remaining) Pair(i, card) else null }
            .let { if (cheapestFirst) it.sortedBy { p -> p.second.cost } else it.sortedByDescending { p -> p.second.cost } }

        for ((rowIdx, card) in candidates) {
            if (bought >= limit) break
            // Re-check: state.recruitRow[rowIdx] may have changed in a prior iteration.
            val currentCard = s.recruitRow[rowIdx] ?: continue
            if (currentCard.cost > remaining) continue

            remaining -= currentCard.cost
            bought++

            val aiHero = s.heroes[heroIndex]
            val heroWithAlly = aiHero.copy(discard = aiHero.discard + currentCard)
            val newSupplyCard = s.recruitSupply.firstOrNull()
            val newRow = s.recruitRow.mapIndexed { i, c -> if (i == rowIdx) newSupplyCard else c }
            s = s.copy(
                heroes = s.heroes.mapIndexed { i, h -> if (i == heroIndex) heroWithAlly else h },
                recruitRow = newRow,
                recruitSupply = s.recruitSupply.drop(1)
            )
        }

        return s
    }
}
