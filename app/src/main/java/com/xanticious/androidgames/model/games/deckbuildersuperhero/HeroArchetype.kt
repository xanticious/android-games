package com.xanticious.androidgames.model.games.deckbuildersuperhero

/**
 * Each hero's playstyle archetype — shapes their starting 10-card deck and
 * defines their identity in the cooperative game.
 */
enum class HeroArchetype(val displayName: String, val flavorText: String) {
    /** Monk: trash weak cards to accelerate the deck. */
    CULL("Cull", "Simplify your life"),
    /** Tigerman: pure offense, heavy attack output. */
    BRAWL("Brawl", "Raw power"),
    /** Echo: many cheap cards that chain extra draws. */
    SWARM("Swarm", "Strength in numbers"),
    /** Bling: generate recruit-power income quickly. */
    BANKROLL("Bankroll", "Money is power"),
    /** Aegis: protective, draw-heavy start. */
    DEFENSE("Defense", "Protect the team"),
    /** Volt: balanced power+attack with draw synergy. */
    TEMPO("Tempo", "Seize the moment")
}
