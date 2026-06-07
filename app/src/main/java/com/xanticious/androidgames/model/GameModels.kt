package com.xanticious.androidgames.model

enum class GameCategory {
    STRATEGY,
    DICE,
    SEARCH,
    WORD,
    CARD,
    RHYTHM,
    PUZZLE,
    EDUCATIONAL,
    ACTION,
    QUEUE_STRATEGY_ACTION,
    BOARD,
    MEMORY,
    TOWER_DEFENSE,
    RTS,
    IDLE,
    EXPLORATION_CREATIVE
}

data class GameDefinition(
    val id: String,
    val name: String,
    val category: GameCategory,
    val released: Boolean = false,
    val favorite: Boolean = false
)

data class LobbyFilter(
    val searchQuery: String = "",
    val onlyFavorites: Boolean = false,
    val onlyReleased: Boolean = true
)
