package com.xanticious.androidgames.model.games.deckbuilderfleet

enum class ShipClass(val label: String) {
    ROW_BOAT("Row Boat"),
    SUBMARINE("Submarine"),
    BATTLESHIP("Battleship"),
    AIRCRAFT_CARRIER("Carrier"),
    FORT("Fort")
}

/**
 * Special abilities that trigger when a card is played.
 *
 * BYPASS_FORTS: combat from this card deals damage directly to the opponent's
 * health, bypassing any forts in play (submarine stealth rule).
 * DRAW_CARD / DRAW_TWO: draw extra cards immediately when played.
 */
enum class SpecialAbility {
    NONE,
    BYPASS_FORTS,
    DRAW_CARD,
    DRAW_TWO
}

/** Immutable definition of a ship/fort card type shared across all instances. */
data class FleetCardType(
    val id: String,
    val name: String,
    val shipClass: ShipClass,
    val cost: Int,
    val tradeValue: Int,
    val combatValue: Int,
    /** > 0 means this card acts as a persistent fort when played. */
    val fortDefense: Int = 0,
    val special: SpecialAbility = SpecialAbility.NONE,
    val isStarter: Boolean = false
)

/** A concrete card instance in the game; unique instanceId distinguishes copies. */
data class FleetCard(
    val type: FleetCardType,
    val instanceId: String
) {
    val isFort: Boolean get() = type.fortDefense > 0
}
