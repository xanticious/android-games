package com.xanticious.androidgames.controller.games.deckbuildersuperhero

import com.xanticious.androidgames.model.games.deckbuildersuperhero.DeckCard
import com.xanticious.androidgames.model.games.deckbuildersuperhero.HeroArchetype

/**
 * All card definitions for Deck Builder Superhero.
 *
 * Starter decks are unique per archetype; recruit cards form the shared market
 * pool that gets shuffled and dealt into the Recruit Row during setup.
 */
object DeckBuilderCards {

    // ── Starter card templates ────────────────────────────────────────────────

    private val copper      = DeckCard("copper",       "Copper",        cost = 0, power = 1, attack = 0, isStarter = true, description = "+1 Power")
    private val strike      = DeckCard("strike",       "Strike",        cost = 0, power = 0, attack = 1, isStarter = true, description = "+1 Attack")
    private val monksClarity = DeckCard("monks_clarity","Monk's Clarity", cost = 0, power = 1, attack = 0, extraDraw = 1, isStarter = true, description = "+1 Power, draw 1")
    private val clawStrike  = DeckCard("claw_strike",  "Claw Strike",   cost = 0, power = 0, attack = 1, isStarter = true, description = "+1 Attack")
    private val roar        = DeckCard("roar",         "Roar",          cost = 0, power = 1, attack = 1, isStarter = true, description = "+1 Power, +1 Attack")
    private val quickCard   = DeckCard("quick_card",   "Quick Card",    cost = 0, power = 0, attack = 0, extraDraw = 1, isStarter = true, description = "Draw 1")
    private val echoChain   = DeckCard("echo_chain",   "Echo Chain",    cost = 0, power = 1, attack = 0, extraDraw = 1, isStarter = true, description = "+1 Power, draw 1")
    private val gold        = DeckCard("gold",         "Gold",          cost = 0, power = 2, attack = 0, isStarter = true, description = "+2 Power")
    private val silver      = DeckCard("silver",       "Silver",        cost = 0, power = 1, attack = 0, isStarter = true, description = "+1 Power")
    private val shieldDraw  = DeckCard("shield_draw",  "Shield Draw",   cost = 0, power = 0, attack = 0, extraDraw = 1, isStarter = true, description = "Draw 1")
    private val boltStrike  = DeckCard("bolt_strike",  "Bolt Strike",   cost = 0, power = 0, attack = 1, isStarter = true, description = "+1 Attack")
    private val surge       = DeckCard("surge",        "Surge",         cost = 0, power = 1, attack = 1, isStarter = true, description = "+1 Power, +1 Attack")

    /**
     * Returns the 10-card starter deck for the given [archetype].
     * Each list is a fresh copy so decks can be shuffled independently.
     */
    fun starterDeck(archetype: HeroArchetype): List<DeckCard> = when (archetype) {
        // Monk / CULL: balanced Power+Attack with draw acceleration.
        HeroArchetype.CULL ->
            listOf(copper, copper, copper, copper, strike, strike, strike, strike, monksClarity, monksClarity)
        // Tigerman / BRAWL: heavy attack skew, minimal draw.
        HeroArchetype.BRAWL ->
            listOf(copper, copper, clawStrike, clawStrike, clawStrike, clawStrike, clawStrike, clawStrike, roar, roar)
        // Echo / SWARM: lots of draw, chaining small cards.
        HeroArchetype.SWARM ->
            listOf(copper, copper, copper, copper, quickCard, quickCard, quickCard, quickCard, echoChain, echoChain)
        // Bling / BANKROLL: 6 Gold — maximum starting Power income.
        HeroArchetype.BANKROLL ->
            listOf(gold, gold, gold, gold, gold, gold, strike, strike, silver, silver)
        // Aegis / DEFENSE: balanced with two shield-draw cards.
        HeroArchetype.DEFENSE ->
            listOf(copper, copper, copper, copper, strike, strike, strike, strike, shieldDraw, shieldDraw)
        // Volt / TEMPO: balanced Power+Attack via Surge pairs.
        HeroArchetype.TEMPO ->
            listOf(copper, copper, copper, copper, boltStrike, boltStrike, boltStrike, boltStrike, surge, surge)
    }

    // ── Recruit Row (market) cards ────────────────────────────────────────────

    /**
     * Full pool of ally cards that feed into the Recruit Row.
     * Cards with duplicate names have unique IDs so set operations work.
     */
    val allRecruitCards: List<DeckCard> = listOf(
        DeckCard("copper_ally_1",   "Copper Ally",   cost = 2, power = 1, attack = 0, description = "+1 Power"),
        DeckCard("copper_ally_2",   "Copper Ally",   cost = 2, power = 1, attack = 0, description = "+1 Power"),
        DeckCard("iron_ally_1",     "Iron Ally",     cost = 3, power = 2, attack = 0, description = "+2 Power"),
        DeckCard("iron_ally_2",     "Iron Ally",     cost = 3, power = 2, attack = 0, description = "+2 Power"),
        DeckCard("power_core_1",    "Power Core",    cost = 2, power = 1, attack = 1, description = "+1 Power, +1 Attack"),
        DeckCard("power_core_2",    "Power Core",    cost = 2, power = 1, attack = 1, description = "+1 Power, +1 Attack"),
        DeckCard("swift_ally",      "Swift Ally",    cost = 2, power = 0, attack = 0, extraDraw = 2, description = "Draw 2"),
        DeckCard("scout",           "Scout",         cost = 2, power = 0, attack = 0, extraDraw = 2, description = "Draw 2"),
        DeckCard("battle_ally_1",   "Battle Ally",   cost = 3, power = 0, attack = 2, description = "+2 Attack"),
        DeckCard("battle_ally_2",   "Battle Ally",   cost = 3, power = 0, attack = 2, description = "+2 Attack"),
        DeckCard("energy_cell_1",   "Energy Cell",   cost = 3, power = 2, attack = 0, description = "+2 Power"),
        DeckCard("energy_cell_2",   "Energy Cell",   cost = 3, power = 2, attack = 0, description = "+2 Power"),
        DeckCard("shadow_agent",    "Shadow Agent",  cost = 3, power = 1, attack = 1, extraDraw = 1, description = "+1P +1A, draw 1"),
        DeckCard("recruiter",       "Recruiter",     cost = 3, power = 2, attack = 0, extraDraw = 1, description = "+2 Power, draw 1"),
        DeckCard("siege_engine",    "Siege Engine",  cost = 4, power = 0, attack = 3, description = "+3 Attack"),
        DeckCard("berserker",       "Berserker",     cost = 4, power = 0, attack = 3, description = "+3 Attack"),
        DeckCard("arsenal",         "Arsenal",       cost = 4, power = 1, attack = 2, description = "+1 Power, +2 Attack"),
        DeckCard("financier",       "Financier",     cost = 4, power = 3, attack = 0, description = "+3 Power"),
        DeckCard("iron_will",       "Iron Will",     cost = 5, power = 1, attack = 2, description = "+1P +2A"),
        DeckCard("treasury",        "Treasury",      cost = 5, power = 3, attack = 0, description = "+3 Power"),
        DeckCard("tactician",       "Tactician",     cost = 5, power = 2, attack = 1, extraDraw = 1, description = "+2P +1A, draw 1"),
        DeckCard("team_leader",     "Team Leader",   cost = 5, power = 1, attack = 1, extraDraw = 2, description = "+1P +1A, draw 2"),
        DeckCard("champion",        "Champion",      cost = 6, power = 0, attack = 4, description = "+4 Attack"),
        DeckCard("heavy_hitter",    "Heavy Hitter",  cost = 6, power = 1, attack = 4, description = "+1P +4A"),
        DeckCard("powerhouse",      "Powerhouse",    cost = 7, power = 2, attack = 3, description = "+2P +3A")
    )
}
