package com.xanticious.androidgames.state.games.boggle

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

enum class BogglePhase { IDLE, PLAYING, ROUND_OVER }

private sealed class GameState : DefaultState() {
    data object Idle : GameState()
    data object Playing : GameState()
    data object RoundOver : GameState()
}

sealed interface BoggleEvent : Event {
    data object RoundStarted : BoggleEvent
    data object CellTapped : BoggleEvent
    data object EntryBackspaced : BoggleEvent
    data object WordSubmitted : BoggleEvent
    data object TimeExpired : BoggleEvent
    data object GaveUp : BoggleEvent
    data object NewRound : BoggleEvent
}

class BoggleStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(BogglePhase.IDLE)
    val phase: StateFlow<BogglePhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(GameState.Idle) {
            transition<BoggleEvent.RoundStarted> {
                targetState = GameState.Playing
                onTriggered { _phase.value = BogglePhase.PLAYING }
            }
        }
        addState(GameState.Playing) {
            transition<BoggleEvent.CellTapped> {
                targetState = GameState.Playing
            }
            transition<BoggleEvent.EntryBackspaced> {
                targetState = GameState.Playing
            }
            transition<BoggleEvent.WordSubmitted> {
                targetState = GameState.Playing
            }
            transition<BoggleEvent.TimeExpired> {
                targetState = GameState.RoundOver
                onTriggered { _phase.value = BogglePhase.ROUND_OVER }
            }
            transition<BoggleEvent.GaveUp> {
                targetState = GameState.RoundOver
                onTriggered { _phase.value = BogglePhase.ROUND_OVER }
            }
        }
        addState(GameState.RoundOver) {
            transition<BoggleEvent.NewRound> {
                targetState = GameState.Idle
                onTriggered { _phase.value = BogglePhase.IDLE }
            }
        }
    }

    fun roundStarted() = machine.processEventByLaunch(BoggleEvent.RoundStarted)
    fun cellTapped() = machine.processEventByLaunch(BoggleEvent.CellTapped)
    fun entryBackspaced() = machine.processEventByLaunch(BoggleEvent.EntryBackspaced)
    fun wordSubmitted() = machine.processEventByLaunch(BoggleEvent.WordSubmitted)
    fun timeExpired() = machine.processEventByLaunch(BoggleEvent.TimeExpired)
    fun gaveUp() = machine.processEventByLaunch(BoggleEvent.GaveUp)
    fun newRound() = machine.processEventByLaunch(BoggleEvent.NewRound)
}
