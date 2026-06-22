package com.xanticious.androidgames.state.games.idlecombattraining

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.DefaultState
import ru.nsk.kstatemachine.state.addInitialState
import ru.nsk.kstatemachine.state.addState
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.statemachine.createStateMachineBlocking
import ru.nsk.kstatemachine.statemachine.processEventByLaunch
import ru.nsk.kstatemachine.transition.onTriggered

enum class IdleCombatPhase { IDLE, SETUP, HOW_TO_PLAY, TRAINING, UPGRADE_MENU_OPEN, DUMMY_DESTROYED }

private sealed class NavState : DefaultState() {
    data object Idle : NavState()
    data object Setup : NavState()
    data object HowToPlay : NavState()
    data object Training : NavState()
    data object UpgradeMenuOpen : NavState()
    data object DummyDestroyed : NavState()
}

private sealed interface IdleCombatSmEvent : Event {
    data object GameStarted : IdleCombatSmEvent
    data object OpenHowToPlay : IdleCombatSmEvent
    data object BackToSetup : IdleCombatSmEvent
    data object ConfirmStart : IdleCombatSmEvent
    data object UpgradeMenuOpened : IdleCombatSmEvent
    data object UpgradeMenuClosed : IdleCombatSmEvent
    data object UpgradePurchased : IdleCombatSmEvent
    data object DummyDefeated : IdleCombatSmEvent
    data object NextDummy : IdleCombatSmEvent
}

class IdleCombatTrainingStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(IdleCombatPhase.IDLE)
    val phase: StateFlow<IdleCombatPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(NavState.Idle) {
            transition<IdleCombatSmEvent.GameStarted> {
                targetState = NavState.Setup
                onTriggered { _phase.value = IdleCombatPhase.SETUP }
            }
        }
        addState(NavState.Setup) {
            transition<IdleCombatSmEvent.OpenHowToPlay> {
                targetState = NavState.HowToPlay
                onTriggered { _phase.value = IdleCombatPhase.HOW_TO_PLAY }
            }
            transition<IdleCombatSmEvent.ConfirmStart> {
                targetState = NavState.Training
                onTriggered { _phase.value = IdleCombatPhase.TRAINING }
            }
        }
        addState(NavState.HowToPlay) {
            transition<IdleCombatSmEvent.BackToSetup> {
                targetState = NavState.Setup
                onTriggered { _phase.value = IdleCombatPhase.SETUP }
            }
        }
        addState(NavState.Training) {
            transition<IdleCombatSmEvent.UpgradeMenuOpened> {
                targetState = NavState.UpgradeMenuOpen
                onTriggered { _phase.value = IdleCombatPhase.UPGRADE_MENU_OPEN }
            }
            transition<IdleCombatSmEvent.DummyDefeated> {
                targetState = NavState.DummyDestroyed
                onTriggered { _phase.value = IdleCombatPhase.DUMMY_DESTROYED }
            }
        }
        addState(NavState.UpgradeMenuOpen) {
            transition<IdleCombatSmEvent.UpgradeMenuClosed> {
                targetState = NavState.Training
                onTriggered { _phase.value = IdleCombatPhase.TRAINING }
            }
            transition<IdleCombatSmEvent.UpgradePurchased> {
                targetState = NavState.Training
                onTriggered { _phase.value = IdleCombatPhase.TRAINING }
            }
        }
        addState(NavState.DummyDestroyed) {
            transition<IdleCombatSmEvent.NextDummy> {
                targetState = NavState.Training
                onTriggered { _phase.value = IdleCombatPhase.TRAINING }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(IdleCombatSmEvent.GameStarted)
    fun openHowToPlay() = machine.processEventByLaunch(IdleCombatSmEvent.OpenHowToPlay)
    fun backToSetup() = machine.processEventByLaunch(IdleCombatSmEvent.BackToSetup)
    fun confirmStart() = machine.processEventByLaunch(IdleCombatSmEvent.ConfirmStart)
    fun openUpgradeMenu() = machine.processEventByLaunch(IdleCombatSmEvent.UpgradeMenuOpened)
    fun closeUpgradeMenu() = machine.processEventByLaunch(IdleCombatSmEvent.UpgradeMenuClosed)
    fun upgradePurchased() = machine.processEventByLaunch(IdleCombatSmEvent.UpgradePurchased)
    fun dummyDefeated() = machine.processEventByLaunch(IdleCombatSmEvent.DummyDefeated)
    fun nextDummy() = machine.processEventByLaunch(IdleCombatSmEvent.NextDummy)
}
