package com.xanticious.androidgames.state.games.idlegeneticalgorithm

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

enum class IdleGaPhase {
    IDLE,
    HOW_TO_PLAY,
    SIMULATING,
    GENERATION_SUMMARY,
    UPGRADE_MENU_OPEN,
    NEW_TRACK_INTRO
}

private sealed class NavState : DefaultState() {
    data object Idle : NavState()
    data object HowToPlay : NavState()
    data object Simulating : NavState()
    data object GenerationSummary : NavState()
    data object UpgradeMenuOpen : NavState()
    data object NewTrackIntro : NavState()
}

private sealed interface IdleGaEvent : Event {
    data object StartSimulation : IdleGaEvent
    data object DismissHowToPlay : IdleGaEvent
    data object GenerationCompleted : IdleGaEvent
    data object StartNextGeneration : IdleGaEvent
    data object OpenUpgrades : IdleGaEvent
    data object CloseUpgrades : IdleGaEvent
    data object NewTrackUnlocked : IdleGaEvent
    data object DismissNewTrack : IdleGaEvent
}

class IdleGeneticAlgorithmStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(IdleGaPhase.IDLE)
    val phase: StateFlow<IdleGaPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(NavState.Idle) {
            transition<IdleGaEvent.StartSimulation> {
                targetState = NavState.HowToPlay
                onTriggered { _phase.value = IdleGaPhase.HOW_TO_PLAY }
            }
        }
        addState(NavState.HowToPlay) {
            transition<IdleGaEvent.DismissHowToPlay> {
                targetState = NavState.Simulating
                onTriggered { _phase.value = IdleGaPhase.SIMULATING }
            }
        }
        addState(NavState.Simulating) {
            transition<IdleGaEvent.GenerationCompleted> {
                targetState = NavState.GenerationSummary
                onTriggered { _phase.value = IdleGaPhase.GENERATION_SUMMARY }
            }
            transition<IdleGaEvent.OpenUpgrades> {
                targetState = NavState.UpgradeMenuOpen
                onTriggered { _phase.value = IdleGaPhase.UPGRADE_MENU_OPEN }
            }
        }
        addState(NavState.GenerationSummary) {
            transition<IdleGaEvent.StartNextGeneration> {
                targetState = NavState.Simulating
                onTriggered { _phase.value = IdleGaPhase.SIMULATING }
            }
            transition<IdleGaEvent.OpenUpgrades> {
                targetState = NavState.UpgradeMenuOpen
                onTriggered { _phase.value = IdleGaPhase.UPGRADE_MENU_OPEN }
            }
            transition<IdleGaEvent.NewTrackUnlocked> {
                targetState = NavState.NewTrackIntro
                onTriggered { _phase.value = IdleGaPhase.NEW_TRACK_INTRO }
            }
        }
        addState(NavState.NewTrackIntro) {
            transition<IdleGaEvent.DismissNewTrack> {
                targetState = NavState.Simulating
                onTriggered { _phase.value = IdleGaPhase.SIMULATING }
            }
        }
        addState(NavState.UpgradeMenuOpen) {
            transition<IdleGaEvent.CloseUpgrades> {
                targetState = NavState.Simulating
                onTriggered { _phase.value = IdleGaPhase.SIMULATING }
            }
        }
    }

    fun startSimulation() = machine.processEventByLaunch(IdleGaEvent.StartSimulation)
    fun dismissHowToPlay() = machine.processEventByLaunch(IdleGaEvent.DismissHowToPlay)
    fun generationCompleted() = machine.processEventByLaunch(IdleGaEvent.GenerationCompleted)
    fun startNextGeneration() = machine.processEventByLaunch(IdleGaEvent.StartNextGeneration)
    fun openUpgrades() = machine.processEventByLaunch(IdleGaEvent.OpenUpgrades)
    fun closeUpgrades() = machine.processEventByLaunch(IdleGaEvent.CloseUpgrades)
    fun newTrackUnlocked() = machine.processEventByLaunch(IdleGaEvent.NewTrackUnlocked)
    fun dismissNewTrack() = machine.processEventByLaunch(IdleGaEvent.DismissNewTrack)
}
