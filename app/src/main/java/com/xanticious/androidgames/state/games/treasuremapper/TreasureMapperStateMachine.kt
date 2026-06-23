package com.xanticious.androidgames.state.games.treasuremapper

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

enum class TreasureMapperPhase { IDLE, SETTINGS, HOW_TO_PLAY, GENERATING_MAP, WAITING_FOR_GUESS, EVALUATING, ROUND_COMPLETE, ROUND_FAILED }

private sealed class MapperState : DefaultState() {
    data object Idle : MapperState()
    data object Settings : MapperState()
    data object HowToPlay : MapperState()
    data object GeneratingMap : MapperState()
    data object WaitingForGuess : MapperState()
    data object Evaluating : MapperState()
    data object RoundComplete : MapperState()
    data object RoundFailed : MapperState()
}

private sealed interface MapperEvent : Event {
    data object StartGame : MapperEvent
    data object OpenHowToPlay : MapperEvent
    data object BackToSettings : MapperEvent
    data object SettingsConfirmed : MapperEvent
    data object MapReady : MapperEvent
    data object CellSelected : MapperEvent
    data object CellDeselected : MapperEvent
    data object DigSubmitted : MapperEvent
    data object CorrectDig : MapperEvent
    data object WrongDigWithTries : MapperEvent
    data object WrongDigNoTries : MapperEvent
    data object NewMap : MapperEvent
}

class TreasureMapperStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(TreasureMapperPhase.IDLE)
    val phase: StateFlow<TreasureMapperPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(MapperState.Idle) {
            transition<MapperEvent.StartGame> {
                targetState = MapperState.Settings
                onTriggered { _phase.value = TreasureMapperPhase.SETTINGS }
            }
        }
        addState(MapperState.Settings) {
            transition<MapperEvent.OpenHowToPlay> {
                targetState = MapperState.HowToPlay
                onTriggered { _phase.value = TreasureMapperPhase.HOW_TO_PLAY }
            }
            transition<MapperEvent.SettingsConfirmed> {
                targetState = MapperState.GeneratingMap
                onTriggered { _phase.value = TreasureMapperPhase.GENERATING_MAP }
            }
        }
        addState(MapperState.HowToPlay) {
            transition<MapperEvent.BackToSettings> {
                targetState = MapperState.Settings
                onTriggered { _phase.value = TreasureMapperPhase.SETTINGS }
            }
        }
        addState(MapperState.GeneratingMap) {
            transition<MapperEvent.MapReady> {
                targetState = MapperState.WaitingForGuess
                onTriggered { _phase.value = TreasureMapperPhase.WAITING_FOR_GUESS }
            }
        }
        addState(MapperState.WaitingForGuess) {
            transition<MapperEvent.CellSelected> {
                targetState = MapperState.WaitingForGuess
                onTriggered { _phase.value = TreasureMapperPhase.WAITING_FOR_GUESS }
            }
            transition<MapperEvent.CellDeselected> {
                targetState = MapperState.WaitingForGuess
                onTriggered { _phase.value = TreasureMapperPhase.WAITING_FOR_GUESS }
            }
            transition<MapperEvent.DigSubmitted> {
                targetState = MapperState.Evaluating
                onTriggered { _phase.value = TreasureMapperPhase.EVALUATING }
            }
        }
        addState(MapperState.Evaluating) {
            transition<MapperEvent.CorrectDig> {
                targetState = MapperState.RoundComplete
                onTriggered { _phase.value = TreasureMapperPhase.ROUND_COMPLETE }
            }
            transition<MapperEvent.WrongDigWithTries> {
                targetState = MapperState.WaitingForGuess
                onTriggered { _phase.value = TreasureMapperPhase.WAITING_FOR_GUESS }
            }
            transition<MapperEvent.WrongDigNoTries> {
                targetState = MapperState.RoundFailed
                onTriggered { _phase.value = TreasureMapperPhase.ROUND_FAILED }
            }
        }
        addState(MapperState.RoundComplete) {
            transition<MapperEvent.NewMap> {
                targetState = MapperState.GeneratingMap
                onTriggered { _phase.value = TreasureMapperPhase.GENERATING_MAP }
            }
        }
        addState(MapperState.RoundFailed) {
            transition<MapperEvent.NewMap> {
                targetState = MapperState.GeneratingMap
                onTriggered { _phase.value = TreasureMapperPhase.GENERATING_MAP }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(MapperEvent.StartGame)
    fun openHowToPlay() = machine.processEventByLaunch(MapperEvent.OpenHowToPlay)
    fun backToSettings() = machine.processEventByLaunch(MapperEvent.BackToSettings)
    fun confirmSettings() = machine.processEventByLaunch(MapperEvent.SettingsConfirmed)
    fun mapReady() = machine.processEventByLaunch(MapperEvent.MapReady)
    fun cellSelected() = machine.processEventByLaunch(MapperEvent.CellSelected)
    fun cellDeselected() = machine.processEventByLaunch(MapperEvent.CellDeselected)
    fun digSubmitted() = machine.processEventByLaunch(MapperEvent.DigSubmitted)
    fun correctDig() = machine.processEventByLaunch(MapperEvent.CorrectDig)
    fun wrongDigWithTries() = machine.processEventByLaunch(MapperEvent.WrongDigWithTries)
    fun wrongDigNoTries() = machine.processEventByLaunch(MapperEvent.WrongDigNoTries)
    fun newMap() = machine.processEventByLaunch(MapperEvent.NewMap)
}
