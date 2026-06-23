package com.xanticious.androidgames.state.games.poker

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

/** Observable phase of a poker session, collected by the composable. */
enum class PokerPhase {
    IDLE,
    ANTEING,
    DEALING,
    BETTING_ROUND_1,
    DRAWING,
    BETTING_ROUND_2,
    SHOWDOWN,
    AWARD_POT,
    SESSION_OVER
}

private sealed class PokerState : DefaultState() {
    data object Idle : PokerState()
    data object Anteing : PokerState()
    data object Dealing : PokerState()
    data object BettingRound1 : PokerState()
    data object Drawing : PokerState()
    data object BettingRound2 : PokerState()
    data object Showdown : PokerState()
    data object AwardPot : PokerState()
    data object SessionOver : PokerState()
}

private sealed interface PokerEvent : Event {
    data object HandStarted : PokerEvent
    data object AntesPosted : PokerEvent
    data object Dealt : PokerEvent
    data object Round1Closed : PokerEvent
    data object AllFoldedRound1 : PokerEvent
    data object DrawsComplete : PokerEvent
    data object Round2Closed : PokerEvent
    data object AllFoldedRound2 : PokerEvent
    data object ShowdownResolved : PokerEvent
    data object HumanBusted : PokerEvent
    data object NextHand : PokerEvent
    data object NewSession : PokerEvent
}

/**
 * KStateMachine tracking the phase transitions of a Five-Card Draw poker game.
 *
 * Mirrors the state diagram in `design/card-games/poker/poker-design.md`:
 * Idle → Anteing → Dealing → BettingRound1 → Drawing → BettingRound2 →
 * Showdown → AwardPot → (Anteing | SessionOver).
 *
 * [scope] is injectable so unit tests can supply [kotlinx.coroutines.Dispatchers.Unconfined]
 * and avoid the Android main dispatcher.
 */
class PokerStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(PokerPhase.IDLE)
    val phase: StateFlow<PokerPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(PokerState.Idle) {
            transition<PokerEvent.HandStarted> {
                targetState = PokerState.Anteing
                onTriggered { _phase.value = PokerPhase.ANTEING }
            }
        }
        addState(PokerState.Anteing) {
            transition<PokerEvent.AntesPosted> {
                targetState = PokerState.Dealing
                onTriggered { _phase.value = PokerPhase.DEALING }
            }
        }
        addState(PokerState.Dealing) {
            transition<PokerEvent.Dealt> {
                targetState = PokerState.BettingRound1
                onTriggered { _phase.value = PokerPhase.BETTING_ROUND_1 }
            }
        }
        addState(PokerState.BettingRound1) {
            transition<PokerEvent.Round1Closed> {
                targetState = PokerState.Drawing
                onTriggered { _phase.value = PokerPhase.DRAWING }
            }
            transition<PokerEvent.AllFoldedRound1> {
                targetState = PokerState.AwardPot
                onTriggered { _phase.value = PokerPhase.AWARD_POT }
            }
        }
        addState(PokerState.Drawing) {
            transition<PokerEvent.DrawsComplete> {
                targetState = PokerState.BettingRound2
                onTriggered { _phase.value = PokerPhase.BETTING_ROUND_2 }
            }
        }
        addState(PokerState.BettingRound2) {
            transition<PokerEvent.Round2Closed> {
                targetState = PokerState.Showdown
                onTriggered { _phase.value = PokerPhase.SHOWDOWN }
            }
            transition<PokerEvent.AllFoldedRound2> {
                targetState = PokerState.AwardPot
                onTriggered { _phase.value = PokerPhase.AWARD_POT }
            }
        }
        addState(PokerState.Showdown) {
            transition<PokerEvent.ShowdownResolved> {
                targetState = PokerState.AwardPot
                onTriggered { _phase.value = PokerPhase.AWARD_POT }
            }
        }
        addState(PokerState.AwardPot) {
            transition<PokerEvent.NextHand> {
                targetState = PokerState.Anteing
                onTriggered { _phase.value = PokerPhase.ANTEING }
            }
            transition<PokerEvent.HumanBusted> {
                targetState = PokerState.SessionOver
                onTriggered { _phase.value = PokerPhase.SESSION_OVER }
            }
        }
        addState(PokerState.SessionOver) {
            transition<PokerEvent.NewSession> {
                targetState = PokerState.Idle
                onTriggered { _phase.value = PokerPhase.IDLE }
            }
        }
    }

    // Public API called by the composable.
    fun handStarted() = machine.processEventByLaunch(PokerEvent.HandStarted)
    fun antesPosted() = machine.processEventByLaunch(PokerEvent.AntesPosted)
    fun dealt() = machine.processEventByLaunch(PokerEvent.Dealt)
    fun round1Closed() = machine.processEventByLaunch(PokerEvent.Round1Closed)
    fun allFoldedRound1() = machine.processEventByLaunch(PokerEvent.AllFoldedRound1)
    fun drawsComplete() = machine.processEventByLaunch(PokerEvent.DrawsComplete)
    fun round2Closed() = machine.processEventByLaunch(PokerEvent.Round2Closed)
    fun allFoldedRound2() = machine.processEventByLaunch(PokerEvent.AllFoldedRound2)
    fun showdownResolved() = machine.processEventByLaunch(PokerEvent.ShowdownResolved)
    fun humanBusted() = machine.processEventByLaunch(PokerEvent.HumanBusted)
    fun nextHand() = machine.processEventByLaunch(PokerEvent.NextHand)
    fun newSession() = machine.processEventByLaunch(PokerEvent.NewSession)
}
