package com.xanticious.androidgames.games.idlecombattraining

import com.xanticious.androidgames.state.games.idlecombattraining.IdleCombatPhase
import com.xanticious.androidgames.state.games.idlecombattraining.IdleCombatTrainingStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class IdleCombatTrainingStateMachineTest {
    private fun machine() = IdleCombatTrainingStateMachine(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun initialPhase_isIdle() {
        assertEquals(IdleCombatPhase.IDLE, machine().phase.value)
    }

    @Test
    fun startGame_transitionsToSetup() {
        val machine = machine()
        machine.startGame()
        assertEquals(IdleCombatPhase.SETUP, machine.phase.value)
    }

    @Test
    fun openHowToPlay_fromSetup_transitionsToHowToPlay() {
        val machine = machine()
        machine.startGame()
        machine.openHowToPlay()
        assertEquals(IdleCombatPhase.HOW_TO_PLAY, machine.phase.value)
    }

    @Test
    fun backToSetup_fromHowToPlay_transitionsToSetup() {
        val machine = machine()
        machine.startGame()
        machine.openHowToPlay()
        machine.backToSetup()
        assertEquals(IdleCombatPhase.SETUP, machine.phase.value)
    }

    @Test
    fun confirmStart_transitionsToTraining() {
        val machine = machine()
        machine.startGame()
        machine.confirmStart()
        assertEquals(IdleCombatPhase.TRAINING, machine.phase.value)
    }

    @Test
    fun openUpgradeMenu_fromTraining_transitionsToUpgradeMenuOpen() {
        val machine = machine()
        machine.startGame()
        machine.confirmStart()
        machine.openUpgradeMenu()
        assertEquals(IdleCombatPhase.UPGRADE_MENU_OPEN, machine.phase.value)
    }

    @Test
    fun closeUpgradeMenu_fromUpgradeMenuOpen_transitionsToTraining() {
        val machine = machine()
        machine.startGame()
        machine.confirmStart()
        machine.openUpgradeMenu()
        machine.closeUpgradeMenu()
        assertEquals(IdleCombatPhase.TRAINING, machine.phase.value)
    }

    @Test
    fun upgradePurchased_fromUpgradeMenuOpen_transitionsToTraining() {
        val machine = machine()
        machine.startGame()
        machine.confirmStart()
        machine.openUpgradeMenu()
        machine.upgradePurchased()
        assertEquals(IdleCombatPhase.TRAINING, machine.phase.value)
    }

    @Test
    fun dummyDefeated_fromTraining_transitionsToDummyDestroyed() {
        val machine = machine()
        machine.startGame()
        machine.confirmStart()
        machine.dummyDefeated()
        assertEquals(IdleCombatPhase.DUMMY_DESTROYED, machine.phase.value)
    }

    @Test
    fun nextDummy_fromDummyDestroyed_transitionsToTraining() {
        val machine = machine()
        machine.startGame()
        machine.confirmStart()
        machine.dummyDefeated()
        machine.nextDummy()
        assertEquals(IdleCombatPhase.TRAINING, machine.phase.value)
    }
}
