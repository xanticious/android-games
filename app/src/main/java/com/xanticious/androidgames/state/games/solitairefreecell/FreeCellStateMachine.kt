package com.xanticious.androidgames.state.games.solitairefreecell

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

/** High-level FreeCell phase observed by the composable. */
enum class FreeCellPhase { IDLE, DEALING, PLAYING, AUTO_FINISHING, WON, LOST }

private sealed class FcState : DefaultState() {
    data object Idle : FcState()
    data object Dealing : FcState()
    data object Playing : FcState()
    data object AutoFinishing : FcState()
    data object Won : FcState()
    data object Lost : FcState()
}

private sealed interface FcEvent : Event {
    data object DealStarted : FcEvent
    data object Dealt : FcEvent
    data object MoveMade : FcEvent
    data object UndoRequested : FcEvent
    data object NoMovesLeft : FcEvent
    data object AllFoundationsComplete : FcEvent
    data object CascadeDone : FcEvent
    data object NewDeal : FcEvent
}

/**
 * Drives FreeCell's game-level phase transitions per the design-doc state machine.
 * Card logic lives in [com.xanticious.androidgames.controller.games.solitairefreecell.FreeCellController];
 * this machine only tracks which phase the deal is in so the view can react.
 *
 * [scope] is injectable so tests can use [kotlinx.coroutines.Dispatchers.Unconfined].
 */
class FreeCellStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(FreeCellPhase.IDLE)
    val phase: StateFlow<FreeCellPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(FcState.Idle) {
            transition<FcEvent.DealStarted> {
                targetState = FcState.Dealing
                onTriggered { _phase.value = FreeCellPhase.DEALING }
            }
        }
        addState(FcState.Dealing) {
            transition<FcEvent.Dealt> {
                targetState = FcState.Playing
                onTriggered { _phase.value = FreeCellPhase.PLAYING }
            }
        }
        addState(FcState.Playing) {
            transition<FcEvent.MoveMade> {
                targetState = FcState.Playing
                onTriggered { _phase.value = FreeCellPhase.PLAYING }
            }
            transition<FcEvent.UndoRequested> {
                targetState = FcState.Playing
                onTriggered { _phase.value = FreeCellPhase.PLAYING }
            }
            transition<FcEvent.NoMovesLeft> {
                targetState = FcState.Lost
                onTriggered { _phase.value = FreeCellPhase.LOST }
            }
            transition<FcEvent.AllFoundationsComplete> {
                targetState = FcState.AutoFinishing
                onTriggered { _phase.value = FreeCellPhase.AUTO_FINISHING }
            }
        }
        addState(FcState.AutoFinishing) {
            transition<FcEvent.CascadeDone> {
                targetState = FcState.Won
                onTriggered { _phase.value = FreeCellPhase.WON }
            }
        }
        addState(FcState.Won) {
            transition<FcEvent.NewDeal> {
                targetState = FcState.Idle
                onTriggered { _phase.value = FreeCellPhase.IDLE }
            }
        }
        addState(FcState.Lost) {
            transition<FcEvent.NewDeal> {
                targetState = FcState.Idle
                onTriggered { _phase.value = FreeCellPhase.IDLE }
            }
        }
    }

    fun startDeal() = machine.processEventByLaunch(FcEvent.DealStarted)
    fun dealComplete() = machine.processEventByLaunch(FcEvent.Dealt)
    fun moveMade() = machine.processEventByLaunch(FcEvent.MoveMade)
    fun undoRequested() = machine.processEventByLaunch(FcEvent.UndoRequested)
    fun noMovesLeft() = machine.processEventByLaunch(FcEvent.NoMovesLeft)
    fun foundationsComplete() = machine.processEventByLaunch(FcEvent.AllFoundationsComplete)
    fun cascadeComplete() = machine.processEventByLaunch(FcEvent.CascadeDone)
    fun newDeal() = machine.processEventByLaunch(FcEvent.NewDeal)
}
