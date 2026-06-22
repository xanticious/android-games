package com.xanticious.androidgames.state.games.solitaireklondike

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

/** High-level Klondike game phases observed by the composable. */
enum class KlondikePhase { IDLE, DEALING, PLAYING, AUTO_FINISHING, WON, LOST }

private sealed class PhaseState : DefaultState() {
    data object Idle : PhaseState()
    data object Dealing : PhaseState()
    data object Playing : PhaseState()
    data object AutoFinishing : PhaseState()
    data object Won : PhaseState()
    data object Lost : PhaseState()
}

private sealed interface KlondikeEvent : Event {
    data object DealStarted : KlondikeEvent
    data object Dealt : KlondikeEvent
    data object AllFoundationsComplete : KlondikeEvent
    data object NoMovesLeft : KlondikeEvent
    data object CascadeDone : KlondikeEvent
    data object NewDeal : KlondikeEvent
}

/**
 * Drives Klondike phase transitions. Game state (tableau/foundations/etc.) lives
 * in the composable's remembered [com.xanticious.androidgames.model.games.solitaireklondike.KlondikeState];
 * this machine only tracks which *phase* the deal is in so the view reacts correctly.
 *
 * The [scope] is injectable so the machine can be tested without the Android main dispatcher.
 */
class KlondikeStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(KlondikePhase.IDLE)
    val phase: StateFlow<KlondikePhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(PhaseState.Idle) {
            transition<KlondikeEvent.DealStarted> {
                targetState = PhaseState.Dealing
                onTriggered { _phase.value = KlondikePhase.DEALING }
            }
        }
        addState(PhaseState.Dealing) {
            transition<KlondikeEvent.Dealt> {
                targetState = PhaseState.Playing
                onTriggered { _phase.value = KlondikePhase.PLAYING }
            }
        }
        addState(PhaseState.Playing) {
            transition<KlondikeEvent.AllFoundationsComplete> {
                targetState = PhaseState.AutoFinishing
                onTriggered { _phase.value = KlondikePhase.AUTO_FINISHING }
            }
            transition<KlondikeEvent.NoMovesLeft> {
                targetState = PhaseState.Lost
                onTriggered { _phase.value = KlondikePhase.LOST }
            }
        }
        addState(PhaseState.AutoFinishing) {
            transition<KlondikeEvent.CascadeDone> {
                targetState = PhaseState.Won
                onTriggered { _phase.value = KlondikePhase.WON }
            }
        }
        addState(PhaseState.Won) {
            transition<KlondikeEvent.NewDeal> {
                targetState = PhaseState.Idle
                onTriggered { _phase.value = KlondikePhase.IDLE }
            }
        }
        addState(PhaseState.Lost) {
            transition<KlondikeEvent.NewDeal> {
                targetState = PhaseState.Idle
                onTriggered { _phase.value = KlondikePhase.IDLE }
            }
        }
    }

    fun startDeal() = machine.processEventByLaunch(KlondikeEvent.DealStarted)
    fun dealComplete() = machine.processEventByLaunch(KlondikeEvent.Dealt)
    fun allFoundationsComplete() = machine.processEventByLaunch(KlondikeEvent.AllFoundationsComplete)
    fun noMovesLeft() = machine.processEventByLaunch(KlondikeEvent.NoMovesLeft)
    fun cascadeDone() = machine.processEventByLaunch(KlondikeEvent.CascadeDone)
    fun newDeal() = machine.processEventByLaunch(KlondikeEvent.NewDeal)
}
