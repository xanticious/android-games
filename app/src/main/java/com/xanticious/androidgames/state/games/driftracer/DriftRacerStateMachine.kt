package com.xanticious.androidgames.state.games.driftracer

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

/** High-level race phases observed by the composable. */
enum class DriftRacerPhase { IDLE, COUNTDOWN, RACING, RACE_FINISHED }

private sealed class RaceState : DefaultState() {
    data object Idle        : RaceState()
    data object Countdown   : RaceState()
    data object Racing      : RaceState()
    data object Finished    : RaceState()
}

private sealed interface RaceEvent : Event {
    data object StartRace         : RaceEvent
    data object CountdownComplete : RaceEvent
    data object FinishRace        : RaceEvent
    data object Retry             : RaceEvent
}

/**
 * Drives Drift Racer's high-level phase transitions.
 * Car physics and lap timing live in
 * [com.xanticious.androidgames.controller.games.driftracer.DriftRacerController];
 * this machine only tracks which phase the race is in.
 *
 * The [scope] is injectable so the machine can be exercised in plain JVM tests
 * without the Android main dispatcher.
 */
class DriftRacerStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(DriftRacerPhase.IDLE)
    val phase: StateFlow<DriftRacerPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(RaceState.Idle) {
            transition<RaceEvent.StartRace> {
                targetState = RaceState.Countdown
                onTriggered { _phase.value = DriftRacerPhase.COUNTDOWN }
            }
        }
        addState(RaceState.Countdown) {
            transition<RaceEvent.CountdownComplete> {
                targetState = RaceState.Racing
                onTriggered { _phase.value = DriftRacerPhase.RACING }
            }
        }
        addState(RaceState.Racing) {
            transition<RaceEvent.FinishRace> {
                targetState = RaceState.Finished
                onTriggered { _phase.value = DriftRacerPhase.RACE_FINISHED }
            }
        }
        addState(RaceState.Finished) {
            transition<RaceEvent.Retry> {
                targetState = RaceState.Countdown
                onTriggered { _phase.value = DriftRacerPhase.COUNTDOWN }
            }
        }
    }

    fun startRace()          = machine.processEventByLaunch(RaceEvent.StartRace)
    fun countdownComplete()  = machine.processEventByLaunch(RaceEvent.CountdownComplete)
    fun raceFinished()       = machine.processEventByLaunch(RaceEvent.FinishRace)
    fun retry()              = machine.processEventByLaunch(RaceEvent.Retry)
}
