package com.xanticious.androidgames.state.games.pong

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

/** High-level Pong match phases observed by the composable. */
enum class PongPhase { IDLE, SERVING, PLAYING, POINT_SCORED, SET_OVER, MATCH_OVER }

private sealed class MatchState : DefaultState() {
    data object Idle : MatchState()
    data object Serving : MatchState()
    data object Playing : MatchState()
    data object PointScored : MatchState()
    data object SetOver : MatchState()
    data object MatchOver : MatchState()
}

private sealed interface MatchEvent : Event {
    data object MatchStarted : MatchEvent
    data object BallServed : MatchEvent
    data object PointEnded : MatchEvent
    data object ContinueSet : MatchEvent
    data object SetEnded : MatchEvent
    data object ContinueMatch : MatchEvent
    data object MatchEnded : MatchEvent
    data object Rematch : MatchEvent
}

/**
 * Drives Pong's match-level phase transitions. Ball physics live in
 * [com.xanticious.androidgames.controller.games.pong.PongController]; this
 * machine only tracks which phase the match is in so the view can react.
 *
 * The [scope] is injectable so the machine can be exercised in plain JVM unit
 * tests without the Android main dispatcher.
 */
class PongStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(PongPhase.IDLE)
    val phase: StateFlow<PongPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(MatchState.Idle) {
            transition<MatchEvent.MatchStarted> {
                targetState = MatchState.Serving
                onTriggered { _phase.value = PongPhase.SERVING }
            }
        }
        addState(MatchState.Serving) {
            transition<MatchEvent.BallServed> {
                targetState = MatchState.Playing
                onTriggered { _phase.value = PongPhase.PLAYING }
            }
        }
        addState(MatchState.Playing) {
            transition<MatchEvent.PointEnded> {
                targetState = MatchState.PointScored
                onTriggered { _phase.value = PongPhase.POINT_SCORED }
            }
        }
        addState(MatchState.PointScored) {
            transition<MatchEvent.ContinueSet> {
                targetState = MatchState.Serving
                onTriggered { _phase.value = PongPhase.SERVING }
            }
            transition<MatchEvent.SetEnded> {
                targetState = MatchState.SetOver
                onTriggered { _phase.value = PongPhase.SET_OVER }
            }
        }
        addState(MatchState.SetOver) {
            transition<MatchEvent.ContinueMatch> {
                targetState = MatchState.Serving
                onTriggered { _phase.value = PongPhase.SERVING }
            }
            transition<MatchEvent.MatchEnded> {
                targetState = MatchState.MatchOver
                onTriggered { _phase.value = PongPhase.MATCH_OVER }
            }
        }
        addState(MatchState.MatchOver) {
            transition<MatchEvent.Rematch> {
                targetState = MatchState.Serving
                onTriggered { _phase.value = PongPhase.SERVING }
            }
        }
    }

    fun startMatch() = machine.processEventByLaunch(MatchEvent.MatchStarted)
    fun ballServed() = machine.processEventByLaunch(MatchEvent.BallServed)
    fun pointEnded() = machine.processEventByLaunch(MatchEvent.PointEnded)
    fun continueSet() = machine.processEventByLaunch(MatchEvent.ContinueSet)
    fun setEnded() = machine.processEventByLaunch(MatchEvent.SetEnded)
    fun continueMatch() = machine.processEventByLaunch(MatchEvent.ContinueMatch)
    fun matchEnded() = machine.processEventByLaunch(MatchEvent.MatchEnded)
    fun rematch() = machine.processEventByLaunch(MatchEvent.Rematch)
}
