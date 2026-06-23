package com.xanticious.androidgames.games.simcityblocks

import com.xanticious.androidgames.state.games.simcityblocks.SimCityBlocksPhase
import com.xanticious.androidgames.state.games.simcityblocks.SimCityBlocksStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class SimCityBlocksStateMachineTest {
    private fun machine() = SimCityBlocksStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun startGame_movesToSimulating() {
        val m = machine()
        m.startGame()
        assertEquals(SimCityBlocksPhase.SIMULATING, m.phase.value)
    }

    @Test
    fun actionTaken_movesToBuilding() {
        val m = machine()
        m.startGame()
        m.actionTaken()
        assertEquals(SimCityBlocksPhase.BUILDING, m.phase.value)
    }

    @Test
    fun buildResolved_returnsToSimulating() {
        val m = machine()
        m.startGame()
        m.actionTaken()
        m.buildResolved()
        assertEquals(SimCityBlocksPhase.SIMULATING, m.phase.value)
    }

    @Test
    fun disasterTriggered_movesToDisaster() {
        val m = machine()
        m.startGame()
        m.disasterTriggered()
        assertEquals(SimCityBlocksPhase.DISASTER, m.phase.value)
    }

    @Test
    fun zoneAbandoned_staysSimulating() {
        val m = machine()
        m.startGame()
        m.zoneAbandoned()
        assertEquals(SimCityBlocksPhase.SIMULATING, m.phase.value)
    }

    @Test
    fun deficitCritical_movesToGameOver() {
        val m = machine()
        m.startGame()
        m.deficitCritical()
        assertEquals(SimCityBlocksPhase.GAME_OVER, m.phase.value)
    }
}
