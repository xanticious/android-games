package com.xanticious.androidgames.state.games.simcityblocks

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

enum class SimCityBlocksPhase { IDLE, BUILDING, SIMULATING, DISASTER, GAME_OVER }

private sealed class CityMachineState : DefaultState() {
    data object Idle : CityMachineState()
    data object Building : CityMachineState()
    data object Simulating : CityMachineState()
    data object Disaster : CityMachineState()
    data object GameOver : CityMachineState()
}

private sealed interface CityMachineEvent : Event {
    data object StartGame : CityMachineEvent
    data object ActionTaken : CityMachineEvent
    data object BuildResolved : CityMachineEvent
    data object CycleAdvanced : CityMachineEvent
    data object DisasterTriggered : CityMachineEvent
    data object ZoneAbandoned : CityMachineEvent
    data object DisasterAcknowledged : CityMachineEvent
    data object DeficitCritical : CityMachineEvent
    data object NewGame : CityMachineEvent
    data object ContinueCity : CityMachineEvent
}

class SimCityBlocksStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(SimCityBlocksPhase.IDLE)
    val phase: StateFlow<SimCityBlocksPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(CityMachineState.Idle) {
            transition<CityMachineEvent.StartGame> {
                targetState = CityMachineState.Simulating
                onTriggered { _phase.value = SimCityBlocksPhase.SIMULATING }
            }
        }
        addState(CityMachineState.Simulating) {
            transition<CityMachineEvent.ActionTaken> {
                targetState = CityMachineState.Building
                onTriggered { _phase.value = SimCityBlocksPhase.BUILDING }
            }
            transition<CityMachineEvent.CycleAdvanced> {
                targetState = CityMachineState.Simulating
                onTriggered { _phase.value = SimCityBlocksPhase.SIMULATING }
            }
            transition<CityMachineEvent.DisasterTriggered> {
                targetState = CityMachineState.Disaster
                onTriggered { _phase.value = SimCityBlocksPhase.DISASTER }
            }
            transition<CityMachineEvent.ZoneAbandoned> {
                targetState = CityMachineState.Simulating
                onTriggered { _phase.value = SimCityBlocksPhase.SIMULATING }
            }
            transition<CityMachineEvent.DeficitCritical> {
                targetState = CityMachineState.GameOver
                onTriggered { _phase.value = SimCityBlocksPhase.GAME_OVER }
            }
        }
        addState(CityMachineState.Building) {
            transition<CityMachineEvent.BuildResolved> {
                targetState = CityMachineState.Simulating
                onTriggered { _phase.value = SimCityBlocksPhase.SIMULATING }
            }
        }
        addState(CityMachineState.Disaster) {
            transition<CityMachineEvent.DisasterAcknowledged> {
                targetState = CityMachineState.Simulating
                onTriggered { _phase.value = SimCityBlocksPhase.SIMULATING }
            }
            transition<CityMachineEvent.DeficitCritical> {
                targetState = CityMachineState.GameOver
                onTriggered { _phase.value = SimCityBlocksPhase.GAME_OVER }
            }
        }
        addState(CityMachineState.GameOver) {
            transition<CityMachineEvent.NewGame> {
                targetState = CityMachineState.Idle
                onTriggered { _phase.value = SimCityBlocksPhase.IDLE }
            }
            transition<CityMachineEvent.ContinueCity> {
                targetState = CityMachineState.Simulating
                onTriggered { _phase.value = SimCityBlocksPhase.SIMULATING }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(CityMachineEvent.StartGame)
    fun actionTaken() = machine.processEventByLaunch(CityMachineEvent.ActionTaken)
    fun buildResolved() = machine.processEventByLaunch(CityMachineEvent.BuildResolved)
    fun cycleAdvanced() = machine.processEventByLaunch(CityMachineEvent.CycleAdvanced)
    fun disasterTriggered() = machine.processEventByLaunch(CityMachineEvent.DisasterTriggered)
    fun zoneAbandoned() = machine.processEventByLaunch(CityMachineEvent.ZoneAbandoned)
    fun disasterAcknowledged() = machine.processEventByLaunch(CityMachineEvent.DisasterAcknowledged)
    fun deficitCritical() = machine.processEventByLaunch(CityMachineEvent.DeficitCritical)
    fun newGame() = machine.processEventByLaunch(CityMachineEvent.NewGame)
    fun continueCity() = machine.processEventByLaunch(CityMachineEvent.ContinueCity)
}
