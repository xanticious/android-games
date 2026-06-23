package com.xanticious.androidgames.model.games.deckbuildersuperhero

import com.xanticious.androidgames.model.GameDifficulty

/**
 * Complete immutable snapshot of a Deck Builder Superhero match.
 *
 * @param heroes        All heroes in turn order; human is always at index 0.
 * @param recruitRow    The six visible market slots; null means the slot is empty.
 * @param recruitSupply Hidden remaining ally cards; used to refill [recruitRow].
 * @param villain       The active Super Villain.
 * @param currentPower  Power accumulated this turn (spend to recruit allies).
 * @param currentAttack Attack accumulated this turn (spend on the villain).
 * @param playedThisTurn Cards the human has played this turn (discarded at end).
 * @param round         Current round number (1-indexed).
 * @param difficulty    Stored so the AI turn can consult it without a separate param.
 */
data class DeckBuilderState(
    val heroes: List<Hero>,
    val recruitRow: List<DeckCard?>,
    val recruitSupply: List<DeckCard>,
    val villain: Villain,
    val currentPower: Int,
    val currentAttack: Int,
    val playedThisTurn: List<DeckCard>,
    val round: Int,
    val difficulty: GameDifficulty
) {
    val humanHeroIndex: Int get() = heroes.indexOfFirst { it.isHuman }
    val humanHero: Hero get() = heroes[humanHeroIndex]
}
