package com.xanticious.androidgames.controller

import com.xanticious.androidgames.model.GameDefinition
import com.xanticious.androidgames.model.LobbyFilter

class LobbyController {
    fun visibleGames(games: List<GameDefinition>, filter: LobbyFilter): List<GameDefinition> {
        val query = filter.searchQuery.trim().lowercase()
        return games.asSequence()
            .filter { game -> !filter.onlyFavorites || game.favorite }
            .filter { game -> !filter.onlyReleased || game.released }
            .filter { game -> filter.categories.isEmpty() || game.category in filter.categories }
            .filter { game -> query.isEmpty() || game.name.lowercase().contains(query) }
            .sortedBy { it.name.lowercase() }
            .toList()
    }
}
