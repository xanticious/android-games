package com.xanticious.androidgames.model.games.deckbuildersuperhero

/**
 * A single card in a hero deck or the recruit market.
 *
 * When played, the card contributes [power] (buying currency), [attack]
 * (combat damage), and [extraDraw] (additional cards drawn immediately).
 * Cards with [cost] > 0 are purchased from the Recruit Row during a turn.
 */
data class DeckCard(
    val id: String,
    val name: String,
    val cost: Int,
    val power: Int,
    val attack: Int,
    val extraDraw: Int = 0,
    val isStarter: Boolean = false,
    val description: String = ""
)
