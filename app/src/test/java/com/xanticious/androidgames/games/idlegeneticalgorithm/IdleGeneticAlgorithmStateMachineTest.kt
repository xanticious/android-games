package com.xanticious.androidgames.games.idlegeneticalgorithm

import com.xanticious.androidgames.state.games.idlegeneticalgorithm.IdleGaPhase
import com.xanticious.androidgames.state.games.idlegeneticalgorithm.IdleGeneticAlgorithmStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class IdleGeneticAlgorithmStateMachineTest {
    private fun machine() = IdleGeneticAlgorithmStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initialPhase_isIdle() {
        assertEquals(IdleGaPhase.IDLE, machine().phase.value)
    }

    @Test
    fun startSimulation_movesToHowToPlay() {
        val machine = machine()
        machine.startSimulation()
        assertEquals(IdleGaPhase.HOW_TO_PLAY, machine.phase.value)
    }

    @Test
    fun dismissHowToPlay_fromHowToPlay_movesToSimulating() {
        val machine = machine()
        machine.startSimulation()
        machine.dismissHowToPlay()
        assertEquals(IdleGaPhase.SIMULATING, machine.phase.value)
    }

    @Test
    fun generationCompleted_fromSimulating_movesToGenerationSummary() {
        val machine = machine()
        machine.startSimulation()
        machine.dismissHowToPlay()
        machine.generationCompleted()
        assertEquals(IdleGaPhase.GENERATION_SUMMARY, machine.phase.value)
    }

    @Test
    fun startNextGeneration_fromGenerationSummary_movesToSimulating() {
        val machine = machine()
        machine.startSimulation()
        machine.dismissHowToPlay()
        machine.generationCompleted()
        machine.startNextGeneration()
        assertEquals(IdleGaPhase.SIMULATING, machine.phase.value)
    }

    @Test
    fun openUpgrades_fromSimulating_movesToUpgradeMenuOpen() {
        val machine = machine()
        machine.startSimulation()
        machine.dismissHowToPlay()
        machine.openUpgrades()
        assertEquals(IdleGaPhase.UPGRADE_MENU_OPEN, machine.phase.value)
    }

    @Test
    fun openUpgrades_fromGenerationSummary_movesToUpgradeMenuOpen() {
        val machine = machine()
        machine.startSimulation()
        machine.dismissHowToPlay()
        machine.generationCompleted()
        machine.openUpgrades()
        assertEquals(IdleGaPhase.UPGRADE_MENU_OPEN, machine.phase.value)
    }

    @Test
    fun closeUpgrades_fromUpgradeMenuOpen_movesToSimulating() {
        val machine = machine()
        machine.startSimulation()
        machine.dismissHowToPlay()
        machine.openUpgrades()
        machine.closeUpgrades()
        assertEquals(IdleGaPhase.SIMULATING, machine.phase.value)
    }

    @Test
    fun newTrackUnlocked_fromGenerationSummary_movesToNewTrackIntro() {
        val machine = machine()
        machine.startSimulation()
        machine.dismissHowToPlay()
        machine.generationCompleted()
        machine.newTrackUnlocked()
        assertEquals(IdleGaPhase.NEW_TRACK_INTRO, machine.phase.value)
    }

    @Test
    fun dismissNewTrack_fromNewTrackIntro_movesToSimulating() {
        val machine = machine()
        machine.startSimulation()
        machine.dismissHowToPlay()
        machine.generationCompleted()
        machine.newTrackUnlocked()
        machine.dismissNewTrack()
        assertEquals(IdleGaPhase.SIMULATING, machine.phase.value)
    }

    @Test
    fun fullFlow_startToFirstGenComplete() {
        val machine = machine()
        machine.startSimulation()
        machine.dismissHowToPlay()
        machine.generationCompleted()
        assertEquals(IdleGaPhase.GENERATION_SUMMARY, machine.phase.value)
    }
}
