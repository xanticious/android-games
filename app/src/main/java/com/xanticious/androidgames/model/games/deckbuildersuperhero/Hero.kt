package com.xanticious.androidgames.model.games.deckbuildersuperhero

/**
 * Immutable snapshot of one hero's state.
 *
 * @param deck   Face-down draw pile (top = first element).
 * @param hand   Cards in hand for the current turn.
 * @param discard Discard pile; shuffled back into [deck] when [deck] runs out.
 * @param trash  Cards permanently removed from the game (Cull ability).
 */
data class Hero(
    val name: String,
    val archetype: HeroArchetype,
    val isHuman: Boolean,
    val deck: List<DeckCard>,
    val hand: List<DeckCard>,
    val discard: List<DeckCard>,
    val trash: List<DeckCard> = emptyList()
)
