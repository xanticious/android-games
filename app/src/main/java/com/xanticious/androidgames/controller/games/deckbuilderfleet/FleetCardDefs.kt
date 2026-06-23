package com.xanticious.androidgames.controller.games.deckbuilderfleet

import com.xanticious.androidgames.model.games.deckbuilderfleet.FleetCard
import com.xanticious.androidgames.model.games.deckbuilderfleet.FleetCardType
import com.xanticious.androidgames.model.games.deckbuilderfleet.ShipClass
import com.xanticious.androidgames.model.games.deckbuilderfleet.SpecialAbility

/** Canonical definitions of every ship/fort card in the game. */
object FleetCardDefs {

    // ── Starter cards (not buyable from market) ──────────────────────────────

    val ROW_BOAT_STARTER = FleetCardType(
        id = "row_boat_s", name = "Row Boat",
        shipClass = ShipClass.ROW_BOAT, cost = 0,
        tradeValue = 1, combatValue = 0, isStarter = true
    )
    val PATROL_STARTER = FleetCardType(
        id = "patrol_s", name = "Basic Patrol",
        shipClass = ShipClass.ROW_BOAT, cost = 0,
        tradeValue = 0, combatValue = 1, isStarter = true
    )

    // ── Market: Row Boats ─────────────────────────────────────────────────────

    val SKIFF = FleetCardType(
        id = "skiff", name = "Skiff",
        shipClass = ShipClass.ROW_BOAT, cost = 2,
        tradeValue = 2, combatValue = 0
    )
    val MERCHANT_CUTTER = FleetCardType(
        id = "merchant_cutter", name = "Merchant Cutter",
        shipClass = ShipClass.ROW_BOAT, cost = 3,
        tradeValue = 3, combatValue = 0
    )

    // ── Market: Submarines ────────────────────────────────────────────────────

    val SCOUT_SUB = FleetCardType(
        id = "scout_sub", name = "Scout Sub",
        shipClass = ShipClass.SUBMARINE, cost = 3,
        tradeValue = 0, combatValue = 2, special = SpecialAbility.BYPASS_FORTS
    )
    val ATTACK_SUB = FleetCardType(
        id = "attack_sub", name = "Attack Sub",
        shipClass = ShipClass.SUBMARINE, cost = 5,
        tradeValue = 1, combatValue = 3, special = SpecialAbility.BYPASS_FORTS
    )

    // ── Market: Battleships ───────────────────────────────────────────────────

    val DESTROYER = FleetCardType(
        id = "destroyer", name = "Destroyer",
        shipClass = ShipClass.BATTLESHIP, cost = 4,
        tradeValue = 0, combatValue = 4
    )
    val BATTLECRUISER = FleetCardType(
        id = "battlecruiser", name = "Battlecruiser",
        shipClass = ShipClass.BATTLESHIP, cost = 6,
        tradeValue = 1, combatValue = 5
    )

    // ── Market: Aircraft Carriers ─────────────────────────────────────────────

    val LIGHT_CARRIER = FleetCardType(
        id = "light_carrier", name = "Light Carrier",
        shipClass = ShipClass.AIRCRAFT_CARRIER, cost = 4,
        tradeValue = 2, combatValue = 1, special = SpecialAbility.DRAW_CARD
    )
    val FLEET_CARRIER = FleetCardType(
        id = "fleet_carrier", name = "Fleet Carrier",
        shipClass = ShipClass.AIRCRAFT_CARRIER, cost = 7,
        tradeValue = 2, combatValue = 2, special = SpecialAbility.DRAW_TWO
    )

    // ── Market: Forts ─────────────────────────────────────────────────────────

    val COASTAL_BATTERY = FleetCardType(
        id = "coastal_battery", name = "Coastal Battery",
        shipClass = ShipClass.FORT, cost = 3,
        tradeValue = 1, combatValue = 0, fortDefense = 10
    )
    val SEA_FORTRESS = FleetCardType(
        id = "sea_fortress", name = "Sea Fortress",
        shipClass = ShipClass.FORT, cost = 5,
        tradeValue = 2, combatValue = 1, fortDefense = 18
    )

    /** Market pool: (cardType, copies in deck). */
    val MARKET_POOL: List<Pair<FleetCardType, Int>> = listOf(
        SKIFF to 4,
        MERCHANT_CUTTER to 3,
        SCOUT_SUB to 4,
        ATTACK_SUB to 3,
        DESTROYER to 4,
        BATTLECRUISER to 3,
        LIGHT_CARRIER to 3,
        FLEET_CARRIER to 2,
        COASTAL_BATTERY to 3,
        SEA_FORTRESS to 2
    )

    /** Build all market cards as concrete instances with unique ids. */
    fun buildMarketPool(): List<FleetCard> {
        var serial = 0
        return MARKET_POOL.flatMap { (type, count) ->
            (1..count).map { FleetCard(type, "m_${type.id}_${++serial}") }
        }
    }

    /** Build a player's 7-card starter deck (5 row boats + 2 basic patrols). */
    fun buildStarterDeck(playerPrefix: String): List<FleetCard> {
        var serial = 0
        return (1..5).map { FleetCard(ROW_BOAT_STARTER, "${playerPrefix}_rb_${++serial}") } +
               (1..2).map { FleetCard(PATROL_STARTER,   "${playerPrefix}_p_${++serial}") }
    }
}
