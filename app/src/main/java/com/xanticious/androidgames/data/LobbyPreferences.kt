package com.xanticious.androidgames.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.xanticious.androidgames.model.LobbyViewMode

/**
 * Local, offline-only persistence for lobby preferences: the favorite game ids
 * and the last-used view mode. Backed by [SharedPreferences] so the data never
 * leaves the device.
 */
class LobbyPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadViewMode(): LobbyViewMode =
        when (prefs.getString(KEY_VIEW_MODE, null)) {
            LobbyViewMode.LIST.name -> LobbyViewMode.LIST
            else -> LobbyViewMode.TILES
        }

    fun saveViewMode(mode: LobbyViewMode) {
        prefs.edit { putString(KEY_VIEW_MODE, mode.name) }
    }

    fun loadFavorites(): Set<String> =
        prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()

    fun saveFavorites(ids: Set<String>) {
        prefs.edit { putStringSet(KEY_FAVORITES, ids.toSet()) }
    }

    private companion object {
        const val PREFS_NAME = "lobby_prefs"
        const val KEY_VIEW_MODE = "view_mode"
        const val KEY_FAVORITES = "favorites"
    }
}
