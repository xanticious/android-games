package com.xanticious.androidgames.state.games.idlebounce

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

enum class IdleBouncePhase { IDLE, SETUP, HOW_TO_PLAY, PLAYING, UPGRADE_MENU_OPEN }

private sealed class NavState : DefaultState() {
    data object Idle : NavState()
    data object Setup : NavState()
    data object HowToPlay : NavState()
    data object Playing : NavState()
    data object UpgradeMenuOpen : NavState()
}

private sealed interface IdleBounceSmEvent : Event {
    data object GameStarted : IdleBounceSmEvent
    data object OpenHowToPlay : IdleBounceSmEvent
    data object BackToSetup : IdleBounceSmEvent
    data object ConfirmStart : IdleBounceSmEvent
    data object UpgradeMenuOpened : IdleBounceSmEvent
    data object UpgradeMenuClosed : IdleBounceSmEvent
    data object UpgradePurchased : IdleBounceSmEvent
}

class IdleBounceStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(IdleBouncePhase.IDLE)
    val phase: StateFlow<IdleBouncePhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(NavState.Idle) {
            transition<IdleBounceSmEvent.GameStarted> {
                targetState = NavState.Setup
                onTriggered { _phase.value = IdleBouncePhase.SETUP }
            }
        }
        addState(NavState.Setup) {
            transition<IdleBounceSmEvent.OpenHowToPlay> {
                targetState = NavState.HowToPlay
                onTriggered { _phase.value = IdleBouncePhase.HOW_TO_PLAY }
            }
            transition<IdleBounceSmEvent.ConfirmStart> {
                targetState = NavState.Playing
                onTriggered { _phase.value = IdleBouncePhase.PLAYING }
            }
        }
        addState(NavState.HowToPlay) {
            transition<IdleBounceSmEvent.BackToSetup> {
                targetState = NavState.Setup
                onTriggered { _phase.value = IdleBouncePhase.SETUP }
            }
        }
        addState(NavState.Playing) {
            transition<IdleBounceSmEvent.UpgradeMenuOpened> {
                targetState = NavState.UpgradeMenuOpen
                onTriggered { _phase.value = IdleBouncePhase.UPGRADE_MENU_OPEN }
            }
        }
        addState(NavState.UpgradeMenuOpen) {
            transition<IdleBounceSmEvent.UpgradeMenuClosed> {
                targetState = NavState.Playing
                onTriggered { _phase.value = IdleBouncePhase.PLAYING }
            }
            transition<IdleBounceSmEvent.UpgradePurchased> {
                targetState = NavState.Playing
                onTriggered { _phase.value = IdleBouncePhase.PLAYING }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(IdleBounceSmEvent.GameStarted)
    fun openHowToPlay() = machine.processEventByLaunch(IdleBounceSmEvent.OpenHowToPlay)
    fun backToSetup() = machine.processEventByLaunch(IdleBounceSmEvent.BackToSetup)
    fun confirmStart() = machine.processEventByLaunch(IdleBounceSmEvent.ConfirmStart)
    fun openUpgradeMenu() = machine.processEventByLaunch(IdleBounceSmEvent.UpgradeMenuOpened)
    fun closeUpgradeMenu() = machine.processEventByLaunch(IdleBounceSmEvent.UpgradeMenuClosed)
    fun upgradePurchased() = machine.processEventByLaunch(IdleBounceSmEvent.UpgradePurchased)
}
