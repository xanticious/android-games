package com.xanticious.androidgames.state.games.dotart

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

enum class DotArtPhase { IDLE, CONNECT, FILL, BRUSH, FINISHED }

private sealed class ArtState : DefaultState() {
    data object Idle : ArtState()
    data object Connect : ArtState()
    data object Fill : ArtState()
    data object Brush : ArtState()
    data object Finished : ArtState()
}

private sealed interface ArtEvent : Event {
    data object StartCanvas : ArtEvent
    data object LineDrawn : ArtEvent
    data object LineUndo : ArtEvent
    data object Phase1Complete : ArtEvent
    data object RegionFilled : ArtEvent
    data object FillUndo : ArtEvent
    data object Phase2Complete : ArtEvent
    data object StrokeDrawn : ArtEvent
    data object StrokeErased : ArtEvent
    data object StrokeUndo : ArtEvent
    data object DrawingDone : ArtEvent
    data object NewCanvas : ArtEvent
    data object BackToMenu : ArtEvent
}

class DotArtStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(DotArtPhase.IDLE)
    val phase: StateFlow<DotArtPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(ArtState.Idle) {
            transition<ArtEvent.StartCanvas> {
                targetState = ArtState.Connect
                onTriggered { _phase.value = DotArtPhase.CONNECT }
            }
        }
        addState(ArtState.Connect) {
            transition<ArtEvent.LineDrawn> {
                targetState = ArtState.Connect
                onTriggered { _phase.value = DotArtPhase.CONNECT }
            }
            transition<ArtEvent.LineUndo> {
                targetState = ArtState.Connect
                onTriggered { _phase.value = DotArtPhase.CONNECT }
            }
            transition<ArtEvent.Phase1Complete> {
                targetState = ArtState.Fill
                onTriggered { _phase.value = DotArtPhase.FILL }
            }
        }
        addState(ArtState.Fill) {
            transition<ArtEvent.RegionFilled> {
                targetState = ArtState.Fill
                onTriggered { _phase.value = DotArtPhase.FILL }
            }
            transition<ArtEvent.FillUndo> {
                targetState = ArtState.Fill
                onTriggered { _phase.value = DotArtPhase.FILL }
            }
            transition<ArtEvent.Phase2Complete> {
                targetState = ArtState.Brush
                onTriggered { _phase.value = DotArtPhase.BRUSH }
            }
        }
        addState(ArtState.Brush) {
            transition<ArtEvent.StrokeDrawn> {
                targetState = ArtState.Brush
                onTriggered { _phase.value = DotArtPhase.BRUSH }
            }
            transition<ArtEvent.StrokeErased> {
                targetState = ArtState.Brush
                onTriggered { _phase.value = DotArtPhase.BRUSH }
            }
            transition<ArtEvent.StrokeUndo> {
                targetState = ArtState.Brush
                onTriggered { _phase.value = DotArtPhase.BRUSH }
            }
            transition<ArtEvent.DrawingDone> {
                targetState = ArtState.Finished
                onTriggered { _phase.value = DotArtPhase.FINISHED }
            }
        }
        addState(ArtState.Finished) {
            transition<ArtEvent.NewCanvas> {
                targetState = ArtState.Connect
                onTriggered { _phase.value = DotArtPhase.CONNECT }
            }
            transition<ArtEvent.BackToMenu> {
                targetState = ArtState.Idle
                onTriggered { _phase.value = DotArtPhase.IDLE }
            }
        }
    }

    fun startCanvas() = machine.processEventByLaunch(ArtEvent.StartCanvas)
    fun lineDrawn() = machine.processEventByLaunch(ArtEvent.LineDrawn)
    fun lineUndo() = machine.processEventByLaunch(ArtEvent.LineUndo)
    fun phase1Complete() = machine.processEventByLaunch(ArtEvent.Phase1Complete)
    fun regionFilled() = machine.processEventByLaunch(ArtEvent.RegionFilled)
    fun fillUndo() = machine.processEventByLaunch(ArtEvent.FillUndo)
    fun phase2Complete() = machine.processEventByLaunch(ArtEvent.Phase2Complete)
    fun strokeDrawn() = machine.processEventByLaunch(ArtEvent.StrokeDrawn)
    fun strokeErased() = machine.processEventByLaunch(ArtEvent.StrokeErased)
    fun strokeUndo() = machine.processEventByLaunch(ArtEvent.StrokeUndo)
    fun drawingDone() = machine.processEventByLaunch(ArtEvent.DrawingDone)
    fun newCanvas() = machine.processEventByLaunch(ArtEvent.NewCanvas)
    fun backToMenu() = machine.processEventByLaunch(ArtEvent.BackToMenu)
}
