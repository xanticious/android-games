package com.xanticious.androidgames.state.games.piratetreasuremaze

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

enum class PirateTreasureMazePhase { IDLE, SETTINGS, HOW_TO_PLAY, GENERATING, PLAYING, MAZE_COMPLETE }

private sealed class MazeState : DefaultState() {
    data object Idle : MazeState()
    data object Settings : MazeState()
    data object HowToPlay : MazeState()
    data object Generating : MazeState()
    data object Playing : MazeState()
    data object MazeComplete : MazeState()
}

private sealed interface MazeEvent : Event {
    data object StartGame : MazeEvent
    data object OpenHowToPlay : MazeEvent
    data object BackToSettings : MazeEvent
    data object SettingsConfirmed : MazeEvent
    data object MazeReady : MazeEvent
    data object MoveMade : MazeEvent
    data object MazeCompleted : MazeEvent
    data object NewMaze : MazeEvent
    data object BackToMenu : MazeEvent
}

class PirateTreasureMazeStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(PirateTreasureMazePhase.IDLE)
    val phase: StateFlow<PirateTreasureMazePhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(MazeState.Idle) {
            transition<MazeEvent.StartGame> {
                targetState = MazeState.Settings
                onTriggered { _phase.value = PirateTreasureMazePhase.SETTINGS }
            }
        }
        addState(MazeState.Settings) {
            transition<MazeEvent.OpenHowToPlay> {
                targetState = MazeState.HowToPlay
                onTriggered { _phase.value = PirateTreasureMazePhase.HOW_TO_PLAY }
            }
            transition<MazeEvent.SettingsConfirmed> {
                targetState = MazeState.Generating
                onTriggered { _phase.value = PirateTreasureMazePhase.GENERATING }
            }
        }
        addState(MazeState.HowToPlay) {
            transition<MazeEvent.BackToSettings> {
                targetState = MazeState.Settings
                onTriggered { _phase.value = PirateTreasureMazePhase.SETTINGS }
            }
        }
        addState(MazeState.Generating) {
            transition<MazeEvent.MazeReady> {
                targetState = MazeState.Playing
                onTriggered { _phase.value = PirateTreasureMazePhase.PLAYING }
            }
        }
        addState(MazeState.Playing) {
            transition<MazeEvent.MoveMade> {
                targetState = MazeState.Playing
                onTriggered { _phase.value = PirateTreasureMazePhase.PLAYING }
            }
            transition<MazeEvent.MazeCompleted> {
                targetState = MazeState.MazeComplete
                onTriggered { _phase.value = PirateTreasureMazePhase.MAZE_COMPLETE }
            }
        }
        addState(MazeState.MazeComplete) {
            transition<MazeEvent.NewMaze> {
                targetState = MazeState.Generating
                onTriggered { _phase.value = PirateTreasureMazePhase.GENERATING }
            }
            transition<MazeEvent.BackToMenu> {
                targetState = MazeState.Idle
                onTriggered { _phase.value = PirateTreasureMazePhase.IDLE }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(MazeEvent.StartGame)
    fun openHowToPlay() = machine.processEventByLaunch(MazeEvent.OpenHowToPlay)
    fun backToSettings() = machine.processEventByLaunch(MazeEvent.BackToSettings)
    fun confirmSettings() = machine.processEventByLaunch(MazeEvent.SettingsConfirmed)
    fun mazeReady() = machine.processEventByLaunch(MazeEvent.MazeReady)
    fun moveMade() = machine.processEventByLaunch(MazeEvent.MoveMade)
    fun mazeCompleted() = machine.processEventByLaunch(MazeEvent.MazeCompleted)
    fun newMaze() = machine.processEventByLaunch(MazeEvent.NewMaze)
    fun backToMenu() = machine.processEventByLaunch(MazeEvent.BackToMenu)
}
