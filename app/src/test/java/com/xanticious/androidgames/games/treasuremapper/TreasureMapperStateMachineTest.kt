package com.xanticious.androidgames.games.treasuremapper

import com.xanticious.androidgames.state.games.treasuremapper.TreasureMapperPhase
import com.xanticious.androidgames.state.games.treasuremapper.TreasureMapperStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class TreasureMapperStateMachineTest {
    private fun machine() = TreasureMapperStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun startGame_movesToSettings() {
        val machine = machine()
        machine.startGame()
        assertEquals(TreasureMapperPhase.SETTINGS, machine.phase.value)
    }

    @Test
    fun mapReady_afterSettings_movesToWaitingForGuess() {
        val machine = machine()
        machine.startGame()
        machine.confirmSettings()
        machine.mapReady()
        assertEquals(TreasureMapperPhase.WAITING_FOR_GUESS, machine.phase.value)
    }

    @Test
    fun correctDig_afterSubmit_movesToRoundComplete() {
        val machine = machine()
        machine.startGame()
        machine.confirmSettings()
        machine.mapReady()
        machine.digSubmitted()
        machine.correctDig()
        assertEquals(TreasureMapperPhase.ROUND_COMPLETE, machine.phase.value)
    }

    @Test
    fun wrongDigNoTries_afterSubmit_movesToRoundFailed() {
        val machine = machine()
        machine.startGame()
        machine.confirmSettings()
        machine.mapReady()
        machine.digSubmitted()
        machine.wrongDigNoTries()
        assertEquals(TreasureMapperPhase.ROUND_FAILED, machine.phase.value)
    }
}
