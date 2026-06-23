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

/** Observable phase of the CLASSIC TriPeaks game. */
enum class TriPeaksPhase {
    IDLE,
    DEALING,
    PLAYING,
    WON,
    LOST,
}

private sealed class TriPeaksState : DefaultState() {
    data object Idle : TriPeaksState()
    data object Dealing : TriPeaksState()
    data object Playing : TriPeaksState()
    data object Won : TriPeaksState()
    data object Lost : TriPeaksState()
}

private sealed interface TriPeaksEvent : Event {
    data object DealStarted : TriPeaksEvent
    data object Dealt : TriPeaksEvent
    data object CardPlayed : TriPeaksEvent
    data object StockDrawn : TriPeaksEvent
    data object UndoRequested : TriPeaksEvent
    data object AllPeaksCleared : TriPeaksEvent
    data object NoMovesAndNoStock : TriPeaksEvent
    data object NewDeal : TriPeaksEvent
    data object Menu : TriPeaksEvent
}

/**
 * Drives the phase transitions for CLASSIC TriPeaks Solitaire.
 *
 * The [scope] is injectable so the machine can run in plain JUnit tests
 * without the Android main dispatcher.
 */
class TriPeaksStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(TriPeaksPhase.IDLE)
    val phase: StateFlow<TriPeaksPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(TriPeaksState.Idle) {
            transition<TriPeaksEvent.DealStarted> {
                targetState = TriPeaksState.Dealing
                onTriggered { _phase.value = TriPeaksPhase.DEALING }
            }
        }
        addState(TriPeaksState.Dealing) {
            transition<TriPeaksEvent.Dealt> {
                targetState = TriPeaksState.Playing
                onTriggered { _phase.value = TriPeaksPhase.PLAYING }
            }
        }
        addState(TriPeaksState.Playing) {
            transition<TriPeaksEvent.CardPlayed> {
                targetState = TriPeaksState.Playing
                onTriggered { _phase.value = TriPeaksPhase.PLAYING }
            }
            transition<TriPeaksEvent.StockDrawn> {
                targetState = TriPeaksState.Playing
                onTriggered { _phase.value = TriPeaksPhase.PLAYING }
            }
            transition<TriPeaksEvent.UndoRequested> {
                targetState = TriPeaksState.Playing
                onTriggered { _phase.value = TriPeaksPhase.PLAYING }
            }
            transition<TriPeaksEvent.AllPeaksCleared> {
                targetState = TriPeaksState.Won
                onTriggered { _phase.value = TriPeaksPhase.WON }
            }
            transition<TriPeaksEvent.NoMovesAndNoStock> {
                targetState = TriPeaksState.Lost
                onTriggered { _phase.value = TriPeaksPhase.LOST }
            }
        }
        addState(TriPeaksState.Won) {
            transition<TriPeaksEvent.NewDeal> {
                targetState = TriPeaksState.Dealing
                onTriggered { _phase.value = TriPeaksPhase.DEALING }
            }
            transition<TriPeaksEvent.Menu> {
                targetState = TriPeaksState.Idle
                onTriggered { _phase.value = TriPeaksPhase.IDLE }
            }
        }
        addState(TriPeaksState.Lost) {
            transition<TriPeaksEvent.NewDeal> {
                targetState = TriPeaksState.Dealing
                onTriggered { _phase.value = TriPeaksPhase.DEALING }
            }
            transition<TriPeaksEvent.Menu> {
                targetState = TriPeaksState.Idle
                onTriggered { _phase.value = TriPeaksPhase.IDLE }
            }
        }
    }

    fun startDeal() = machine.processEventByLaunch(TriPeaksEvent.DealStarted)
    fun dealt() = machine.processEventByLaunch(TriPeaksEvent.Dealt)
    fun cardPlayed() = machine.processEventByLaunch(TriPeaksEvent.CardPlayed)
    fun stockDrawn() = machine.processEventByLaunch(TriPeaksEvent.StockDrawn)
    fun undoRequested() = machine.processEventByLaunch(TriPeaksEvent.UndoRequested)
    fun allPeaksCleared() = machine.processEventByLaunch(TriPeaksEvent.AllPeaksCleared)
    fun noMovesAndNoStock() = machine.processEventByLaunch(TriPeaksEvent.NoMovesAndNoStock)
    fun newDeal() = machine.processEventByLaunch(TriPeaksEvent.NewDeal)
    fun menu() = machine.processEventByLaunch(TriPeaksEvent.Menu)
}
