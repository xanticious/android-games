package com.xanticious.androidgames.state.games.cribbage

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
import com.xanticious.androidgames.model.games.cribbage.CribbagePhase

/**
 * KStateMachine driving the phases of a Cribbage hand.
 *
 * Phase progression:
 *   Idle → Dealing → Discarding → Cutting
 *     → Playing → ShowNonDealer → ShowDealer → ShowCrib
 *     → (Dealing again or GameOver)
 *
 * All game-logic transitions happen in the controller; this machine only tracks
 * which UI phase is active and exposes it as [phase]: StateFlow<CribbagePhase>.
 */

private sealed class CribbageState : DefaultState() {
    data object Idle : CribbageState()
    data object Dealing : CribbageState()
    data object Discarding : CribbageState()
    data object Cutting : CribbageState()
    data object Playing : CribbageState()
    data object ShowNonDealer : CribbageState()
    data object ShowDealer : CribbageState()
    data object ShowCrib : CribbageState()
    data object GameOver : CribbageState()
}

sealed interface CribbageEvent : Event {
    data object StartGame : CribbageEvent
    data object CardsDealt : CribbageEvent
    data object CribCompleted : CribbageEvent
    data object StarterCut : CribbageEvent
    data object PlayFinished : CribbageEvent
    data object NonDealerCounted : CribbageEvent
    data object DealerCounted : CribbageEvent
    data object CribCounted : CribbageEvent
    data object GameWon : CribbageEvent
    data object NextHand : CribbageEvent
    data object Rematch : CribbageEvent
}

class CribbageStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(CribbagePhase.DEALING)
    val phase: StateFlow<CribbagePhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(CribbageState.Idle) {
            transition<CribbageEvent.StartGame> {
                targetState = CribbageState.Dealing
                onTriggered { _phase.value = CribbagePhase.DEALING }
            }
        }
        addState(CribbageState.Dealing) {
            transition<CribbageEvent.CardsDealt> {
                targetState = CribbageState.Discarding
                onTriggered { _phase.value = CribbagePhase.DISCARDING }
            }
        }
        addState(CribbageState.Discarding) {
            transition<CribbageEvent.CribCompleted> {
                targetState = CribbageState.Cutting
                onTriggered { _phase.value = CribbagePhase.CUTTING }
            }
        }
        addState(CribbageState.Cutting) {
            transition<CribbageEvent.StarterCut> {
                targetState = CribbageState.Playing
                onTriggered { _phase.value = CribbagePhase.PLAYING }
            }
            transition<CribbageEvent.GameWon> {
                targetState = CribbageState.GameOver
                onTriggered { _phase.value = CribbagePhase.GAME_OVER }
            }
        }
        addState(CribbageState.Playing) {
            transition<CribbageEvent.PlayFinished> {
                targetState = CribbageState.ShowNonDealer
                onTriggered { _phase.value = CribbagePhase.SHOW_NON_DEALER }
            }
            transition<CribbageEvent.GameWon> {
                targetState = CribbageState.GameOver
                onTriggered { _phase.value = CribbagePhase.GAME_OVER }
            }
        }
        addState(CribbageState.ShowNonDealer) {
            transition<CribbageEvent.NonDealerCounted> {
                targetState = CribbageState.ShowDealer
                onTriggered { _phase.value = CribbagePhase.SHOW_DEALER }
            }
            transition<CribbageEvent.GameWon> {
                targetState = CribbageState.GameOver
                onTriggered { _phase.value = CribbagePhase.GAME_OVER }
            }
        }
        addState(CribbageState.ShowDealer) {
            transition<CribbageEvent.DealerCounted> {
                targetState = CribbageState.ShowCrib
                onTriggered { _phase.value = CribbagePhase.SHOW_CRIB }
            }
            transition<CribbageEvent.GameWon> {
                targetState = CribbageState.GameOver
                onTriggered { _phase.value = CribbagePhase.GAME_OVER }
            }
        }
        addState(CribbageState.ShowCrib) {
            transition<CribbageEvent.NextHand> {
                targetState = CribbageState.Dealing
                onTriggered { _phase.value = CribbagePhase.DEALING }
            }
            transition<CribbageEvent.GameWon> {
                targetState = CribbageState.GameOver
                onTriggered { _phase.value = CribbagePhase.GAME_OVER }
            }
        }
        addState(CribbageState.GameOver) {
            transition<CribbageEvent.Rematch> {
                targetState = CribbageState.Dealing
                onTriggered { _phase.value = CribbagePhase.DEALING }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(CribbageEvent.StartGame)
    fun cardsDealt() = machine.processEventByLaunch(CribbageEvent.CardsDealt)
    fun cribCompleted() = machine.processEventByLaunch(CribbageEvent.CribCompleted)
    fun starterCut() = machine.processEventByLaunch(CribbageEvent.StarterCut)
    fun playFinished() = machine.processEventByLaunch(CribbageEvent.PlayFinished)
    fun nonDealerCounted() = machine.processEventByLaunch(CribbageEvent.NonDealerCounted)
    fun dealerCounted() = machine.processEventByLaunch(CribbageEvent.DealerCounted)
    fun cribCounted() = machine.processEventByLaunch(CribbageEvent.CribCounted)
    fun gameWon() = machine.processEventByLaunch(CribbageEvent.GameWon)
    fun nextHand() = machine.processEventByLaunch(CribbageEvent.NextHand)
    fun rematch() = machine.processEventByLaunch(CribbageEvent.Rematch)
}
