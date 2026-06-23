package com.xanticious.androidgames.state.games.solitairetripeaks

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

/** Observable phase of the TIMED TriPeaks run. */
enum class TriPeaksTimedPhase {
    IDLE,
    DEALING,
    PLAYING,
    PAUSED,
    RUN_OVER,
}

private sealed class TimedState : DefaultState() {
    data object Idle : TimedState()
    data object Dealing : TimedState()
    data object Playing : TimedState()
    data object Paused : TimedState()
    data object RunOver : TimedState()
}

private sealed interface TimedEvent : Event {
    data object RunStarted : TimedEvent
    data object Dealt : TimedEvent
    data object CardPlayed : TimedEvent
    data object StockDrawn : TimedEvent
    data object TimerExpired : TimedEvent
    data object BoardCleared : TimedEvent
    data object PauseRequested : TimedEvent
    data object Resumed : TimedEvent
    data object Retry : TimedEvent
    data object Menu : TimedEvent
}

/**
 * Drives the phase transitions for TIMED TriPeaks Solitaire.
 *
 * Board clearing starts a new deal immediately (continuous mode) rather than
 * ending the run — the timer keeps running throughout, so the only terminal
 * state is [TriPeaksTimedPhase.RUN_OVER] when the timer hits zero.
 *
 * The [scope] is injectable for plain JVM unit tests.
 */
class TriPeaksTimedStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(TriPeaksTimedPhase.IDLE)
    val phase: StateFlow<TriPeaksTimedPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(TimedState.Idle) {
            transition<TimedEvent.RunStarted> {
                targetState = TimedState.Dealing
                onTriggered { _phase.value = TriPeaksTimedPhase.DEALING }
            }
        }
        addState(TimedState.Dealing) {
            transition<TimedEvent.Dealt> {
                targetState = TimedState.Playing
                onTriggered { _phase.value = TriPeaksTimedPhase.PLAYING }
            }
        }
        addState(TimedState.Playing) {
            transition<TimedEvent.CardPlayed> {
                targetState = TimedState.Playing
                onTriggered { _phase.value = TriPeaksTimedPhase.PLAYING }
            }
            transition<TimedEvent.StockDrawn> {
                targetState = TimedState.Playing
                onTriggered { _phase.value = TriPeaksTimedPhase.PLAYING }
            }
            // Board cleared → re-deal instantly (continuous mode)
            transition<TimedEvent.BoardCleared> {
                targetState = TimedState.Dealing
                onTriggered { _phase.value = TriPeaksTimedPhase.DEALING }
            }
            transition<TimedEvent.TimerExpired> {
                targetState = TimedState.RunOver
                onTriggered { _phase.value = TriPeaksTimedPhase.RUN_OVER }
            }
            transition<TimedEvent.PauseRequested> {
                targetState = TimedState.Paused
                onTriggered { _phase.value = TriPeaksTimedPhase.PAUSED }
            }
        }
        addState(TimedState.Paused) {
            transition<TimedEvent.Resumed> {
                targetState = TimedState.Playing
                onTriggered { _phase.value = TriPeaksTimedPhase.PLAYING }
            }
        }
        addState(TimedState.RunOver) {
            transition<TimedEvent.Retry> {
                targetState = TimedState.Dealing
                onTriggered { _phase.value = TriPeaksTimedPhase.DEALING }
            }
            transition<TimedEvent.Menu> {
                targetState = TimedState.Idle
                onTriggered { _phase.value = TriPeaksTimedPhase.IDLE }
            }
        }
    }

    fun startRun() = machine.processEventByLaunch(TimedEvent.RunStarted)
    fun dealt() = machine.processEventByLaunch(TimedEvent.Dealt)
    fun cardPlayed() = machine.processEventByLaunch(TimedEvent.CardPlayed)
    fun stockDrawn() = machine.processEventByLaunch(TimedEvent.StockDrawn)
    fun boardCleared() = machine.processEventByLaunch(TimedEvent.BoardCleared)
    fun timerExpired() = machine.processEventByLaunch(TimedEvent.TimerExpired)
    fun pauseRequested() = machine.processEventByLaunch(TimedEvent.PauseRequested)
    fun resumed() = machine.processEventByLaunch(TimedEvent.Resumed)
    fun retry() = machine.processEventByLaunch(TimedEvent.Retry)
    fun menu() = machine.processEventByLaunch(TimedEvent.Menu)
}
