package com.xanticious.androidgames.state.games.solitairepyramid

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

/** Observable phase of a Pyramid Solitaire game. */
enum class PyramidPhase { IDLE, DEALING, PLAYING, WON, LOST }

private sealed class PyramidMachineState : DefaultState() {
    data object Idle    : PyramidMachineState()
    data object Dealing : PyramidMachineState()
    data object Playing : PyramidMachineState()
    data object Won     : PyramidMachineState()
    data object Lost    : PyramidMachineState()
}

private sealed interface PyramidMachineEvent : Event {
    data object DealStarted     : PyramidMachineEvent
    data object Dealt           : PyramidMachineEvent
    data object PyramidCleared  : PyramidMachineEvent
    data object NoMovesLeft     : PyramidMachineEvent
    data object NewDeal         : PyramidMachineEvent
    data object Menu            : PyramidMachineEvent
}

/**
 * Tracks the high-level phase of Pyramid Solitaire using KStateMachine.
 *
 * The [scope] is injectable so the machine can be exercised in plain JUnit
 * tests without the Android main dispatcher (use [Dispatchers.Unconfined]).
 * All game-rule logic lives in
 * [com.xanticious.androidgames.controller.games.solitairepyramid.PyramidRules];
 * this machine only signals phase changes so the view can react.
 */
class PyramidStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(PyramidPhase.IDLE)
    val phase: StateFlow<PyramidPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(PyramidMachineState.Idle) {
            transition<PyramidMachineEvent.DealStarted> {
                targetState = PyramidMachineState.Dealing
                onTriggered { _phase.value = PyramidPhase.DEALING }
            }
        }
        addState(PyramidMachineState.Dealing) {
            transition<PyramidMachineEvent.Dealt> {
                targetState = PyramidMachineState.Playing
                onTriggered { _phase.value = PyramidPhase.PLAYING }
            }
        }
        addState(PyramidMachineState.Playing) {
            transition<PyramidMachineEvent.PyramidCleared> {
                targetState = PyramidMachineState.Won
                onTriggered { _phase.value = PyramidPhase.WON }
            }
            transition<PyramidMachineEvent.NoMovesLeft> {
                targetState = PyramidMachineState.Lost
                onTriggered { _phase.value = PyramidPhase.LOST }
            }
        }
        addState(PyramidMachineState.Won) {
            transition<PyramidMachineEvent.NewDeal> {
                targetState = PyramidMachineState.Dealing
                onTriggered { _phase.value = PyramidPhase.DEALING }
            }
            transition<PyramidMachineEvent.Menu> {
                targetState = PyramidMachineState.Idle
                onTriggered { _phase.value = PyramidPhase.IDLE }
            }
        }
        addState(PyramidMachineState.Lost) {
            transition<PyramidMachineEvent.NewDeal> {
                targetState = PyramidMachineState.Dealing
                onTriggered { _phase.value = PyramidPhase.DEALING }
            }
            transition<PyramidMachineEvent.Menu> {
                targetState = PyramidMachineState.Idle
                onTriggered { _phase.value = PyramidPhase.IDLE }
            }
        }
    }

    fun startDeal()        = machine.processEventByLaunch(PyramidMachineEvent.DealStarted)
    fun dealt()            = machine.processEventByLaunch(PyramidMachineEvent.Dealt)
    fun pyramidCleared()   = machine.processEventByLaunch(PyramidMachineEvent.PyramidCleared)
    fun noMovesLeft()      = machine.processEventByLaunch(PyramidMachineEvent.NoMovesLeft)
    fun newDeal()          = machine.processEventByLaunch(PyramidMachineEvent.NewDeal)
    fun menu()             = machine.processEventByLaunch(PyramidMachineEvent.Menu)
}
