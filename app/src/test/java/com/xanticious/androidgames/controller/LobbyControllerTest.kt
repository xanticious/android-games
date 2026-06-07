package com.xanticious.androidgames.controller

import com.xanticious.androidgames.model.GameCategory
import com.xanticious.androidgames.model.GameDefinition
import com.xanticious.androidgames.model.LobbyFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class LobbyControllerTest {
    private val controller = LobbyController()

    private val games = listOf(
        GameDefinition("2", "Wordle", GameCategory.WORD, released = false, favorite = true),
        GameDefinition("1", "Chess", GameCategory.STRATEGY, released = true, favorite = false),
        GameDefinition("3", "Anagrams", GameCategory.WORD, released = true, favorite = true)
    )

    @Test
    fun visibleGames_sortsAlphabetically() {
        val result = controller.visibleGames(games, LobbyFilter(onlyReleased = false))
        assertEquals(listOf("Anagrams", "Chess", "Wordle"), result.map { it.name })
    }

    @Test
    fun visibleGames_appliesOnlyReleasedFilter() {
        val result = controller.visibleGames(games, LobbyFilter(onlyReleased = true))
        assertEquals(listOf("Anagrams", "Chess"), result.map { it.name })
    }

    @Test
    fun visibleGames_appliesFavoritesAndSearch() {
        val result = controller.visibleGames(
            games,
            LobbyFilter(searchQuery = "ana", onlyFavorites = true, onlyReleased = false)
        )
        assertEquals(listOf("Anagrams"), result.map { it.name })
    }
}
