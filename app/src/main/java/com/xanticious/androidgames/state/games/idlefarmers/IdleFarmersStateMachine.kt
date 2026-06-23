package com.xanticious.androidgames.state.games.idlefarmers

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

enum class IdleFarmersPhase {
    IDLE,
    SETUP,
    HOW_TO_PLAY,
    PLAYING,
    UPGRADE_MENU_OPEN,
    EVENT_ACTIVE
}

private sealed class NavState : DefaultState() {
    data object Idle : NavState()
    data object Setup : NavState()
    data object HowToPlay : NavState()
    data object Playing : NavState()
    data object UpgradeMenuOpen : NavState()
    data object EventActive : NavState()
}

private sealed interface IdleFarmersEvent : Event {
    data object StartGame : IdleFarmersEvent
    data object OpenHowToPlay : IdleFarmersEvent
    data object StartPlaying : IdleFarmersEvent
    data object BackToSetup : IdleFarmersEvent
    data object OpenUpgrades : IdleFarmersEvent
    data object CloseUpgrades : IdleFarmersEvent
    data object EventTriggered : IdleFarmersEvent
    data object EventResolved : IdleFarmersEvent
}

class IdleFarmersStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(IdleFarmersPhase.IDLE)
    val phase: StateFlow<IdleFarmersPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(NavState.Idle) {
            transition<IdleFarmersEvent.StartGame> {
                targetState = NavState.Setup
                onTriggered { _phase.value = IdleFarmersPhase.SETUP }
            }
        }
        addState(NavState.Setup) {
            transition<IdleFarmersEvent.OpenHowToPlay> {
                targetState = NavState.HowToPlay
                onTriggered { _phase.value = IdleFarmersPhase.HOW_TO_PLAY }
            }
            transition<IdleFarmersEvent.StartPlaying> {
                targetState = NavState.Playing
                onTriggered { _phase.value = IdleFarmersPhase.PLAYING }
            }
        }
        addState(NavState.HowToPlay) {
            transition<IdleFarmersEvent.BackToSetup> {
                targetState = NavState.Setup
                onTriggered { _phase.value = IdleFarmersPhase.SETUP }
            }
        }
        addState(NavState.Playing) {
            transition<IdleFarmersEvent.OpenUpgrades> {
                targetState = NavState.UpgradeMenuOpen
                onTriggered { _phase.value = IdleFarmersPhase.UPGRADE_MENU_OPEN }
            }
            transition<IdleFarmersEvent.EventTriggered> {
                targetState = NavState.EventActive
                onTriggered { _phase.value = IdleFarmersPhase.EVENT_ACTIVE }
            }
        }
        addState(NavState.UpgradeMenuOpen) {
            transition<IdleFarmersEvent.CloseUpgrades> {
                targetState = NavState.Playing
                onTriggered { _phase.value = IdleFarmersPhase.PLAYING }
            }
        }
        addState(NavState.EventActive) {
            transition<IdleFarmersEvent.EventResolved> {
                targetState = NavState.Playing
                onTriggered { _phase.value = IdleFarmersPhase.PLAYING }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(IdleFarmersEvent.StartGame)
    fun openHowToPlay() = machine.processEventByLaunch(IdleFarmersEvent.OpenHowToPlay)
    fun startPlaying() = machine.processEventByLaunch(IdleFarmersEvent.StartPlaying)
    fun backToSetup() = machine.processEventByLaunch(IdleFarmersEvent.BackToSetup)
    fun openUpgrades() = machine.processEventByLaunch(IdleFarmersEvent.OpenUpgrades)
    fun closeUpgrades() = machine.processEventByLaunch(IdleFarmersEvent.CloseUpgrades)
    fun eventTriggered() = machine.processEventByLaunch(IdleFarmersEvent.EventTriggered)
    fun eventResolved() = machine.processEventByLaunch(IdleFarmersEvent.EventResolved)
}
