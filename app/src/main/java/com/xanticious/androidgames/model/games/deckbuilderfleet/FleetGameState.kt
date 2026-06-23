package com.xanticious.androidgames.model.games.deckbuilderfleet

enum class Winner { PLAYER, BOT }

/** A fort card that remains in play until its defence is reduced to zero. */
data class FortInPlay(
    val card: FleetCard,
    val remainingDefense: Int
)

data class PlayerState(
    val deck: List<FleetCard>,
    val hand: List<FleetCard>,
    val discard: List<FleetCard>,
    /** Non-fort cards played this turn; cleared at end of turn. */
    val playArea: List<FleetCard>,
    /** Persistent forts in play; survive until destroyed by the opponent. */
    val forts: List<FortInPlay>,
    val health: Int
) {
    companion object {
        fun initial(deck: List<FleetCard>, health: Int) = PlayerState(
            deck = deck,
            hand = emptyList(),
            discard = emptyList(),
            playArea = emptyList(),
            forts = emptyList(),
            health = health
        )
    }
}

data class FleetGameState(
    val player: PlayerState,
    val bot: PlayerState,
    val tradeRow: List<FleetCard>,
    val tradePool: List<FleetCard>,
    val currentCoins: Int = 0,
    val currentCombat: Int = 0,
    /** Combat from submarines — hits health directly, bypassing all forts. */
    val currentSubCombat: Int = 0,
    val isPlayerTurn: Boolean = true,
    val turnCount: Int = 0,
    val winner: Winner? = null
)

data class FleetConfig(
    val playerStartHealth: Int = 50,
    val botStartHealth: Int = 50,
    val tradeRowSize: Int = 5,
    val basicBuyEnabled: Boolean = false
)
