package com.xanticious.androidgames.state.games.solitaireclock

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
import ru.nsk.kstatemachine.statemachine.createStateMachineBlocking
import ru.nsk.kstatemachine.statemachine.processEventByLaunch
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.transition.onTriggered

/** High-level Clock Solitaire game phases observed by the composable. */
enum class ClockSolitairePhase { IDLE, PLAYING, PAUSED, WON, LOST }

private sealed class ClockState : DefaultState() {
    data object Idle : ClockState()
    data object Playing : ClockState()
    data object Paused : ClockState()
    data object Won : ClockState()
    data object Lost : ClockState()
}

private sealed interface ClockEvent : Event {
    data object GameStarted : ClockEvent
    data object PauseRequested : ClockEvent
    data object Resumed : ClockEvent
    data object GameWon : ClockEvent
    data object GameLost : ClockEvent
    data object Replay : ClockEvent
}

/**
 * Drives Clock Solitaire phase transitions. All game rules live in
 * [com.xanticious.androidgames.controller.games.solitaireclock.ClockSolitaireController];
 * this machine only tracks which phase the game is in so the view can react.
 *
 * The [scope] is injectable for plain-JVM unit tests (use [Dispatchers.Unconfined]).
 */
class ClockSolitaireStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(ClockSolitairePhase.IDLE)
    val phase: StateFlow<ClockSolitairePhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(ClockState.Idle) {
            transition<ClockEvent.GameStarted> {
                targetState = ClockState.Playing
                onTriggered { _phase.value = ClockSolitairePhase.PLAYING }
            }
        }
        addState(ClockState.Playing) {
            transition<ClockEvent.PauseRequested> {
                targetState = ClockState.Paused
                onTriggered { _phase.value = ClockSolitairePhase.PAUSED }
            }
            transition<ClockEvent.GameWon> {
                targetState = ClockState.Won
                onTriggered { _phase.value = ClockSolitairePhase.WON }
            }
            transition<ClockEvent.GameLost> {
                targetState = ClockState.Lost
                onTriggered { _phase.value = ClockSolitairePhase.LOST }
            }
        }
        addState(ClockState.Paused) {
            transition<ClockEvent.Resumed> {
                targetState = ClockState.Playing
                onTriggered { _phase.value = ClockSolitairePhase.PLAYING }
            }
        }
        addState(ClockState.Won) {
            transition<ClockEvent.Replay> {
                targetState = ClockState.Playing
                onTriggered { _phase.value = ClockSolitairePhase.PLAYING }
            }
        }
        addState(ClockState.Lost) {
            transition<ClockEvent.Replay> {
                targetState = ClockState.Playing
                onTriggered { _phase.value = ClockSolitairePhase.PLAYING }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(ClockEvent.GameStarted)
    fun pauseGame() = machine.processEventByLaunch(ClockEvent.PauseRequested)
    fun resumeGame() = machine.processEventByLaunch(ClockEvent.Resumed)
    fun gameWon() = machine.processEventByLaunch(ClockEvent.GameWon)
    fun gameLost() = machine.processEventByLaunch(ClockEvent.GameLost)
    fun replay() = machine.processEventByLaunch(ClockEvent.Replay)
}
