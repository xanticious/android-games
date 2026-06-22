package com.xanticious.androidgames.games.planetexplorer

import com.xanticious.androidgames.state.games.planetexplorer.PlanetExplorerPhase
import com.xanticious.androidgames.state.games.planetexplorer.PlanetExplorerStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanetExplorerStateMachineTest {
    private fun machine() = PlanetExplorerStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun startGame_movesToLoadingWorld() {
        val m = machine()
        m.startGame()
        assertEquals(PlanetExplorerPhase.LOADING_WORLD, m.phase.value)
    }

    @Test
    fun worldReady_movesToExploring() {
        val m = machine()
        m.startGame()
        m.worldReady()
        assertEquals(PlanetExplorerPhase.EXPLORING, m.phase.value)
    }

    @Test
    fun openFieldBook_movesToFieldBook() {
        val m = machine()
        m.startGame()
        m.worldReady()
        m.openFieldBook()
        assertEquals(PlanetExplorerPhase.FIELD_BOOK, m.phase.value)
    }

    @Test
    fun selectBiome_returnsToExploring() {
        val m = machine()
        m.startGame()
        m.worldReady()
        m.openBiomeSwitcher()
        m.selectBiome()
        assertEquals(PlanetExplorerPhase.EXPLORING, m.phase.value)
    }

    @Test
    fun toolFound_movesToDiscovery() {
        val m = machine()
        m.startGame()
        m.worldReady()
        m.toolFound()
        assertEquals(PlanetExplorerPhase.DISCOVERY, m.phase.value)
    }
}
