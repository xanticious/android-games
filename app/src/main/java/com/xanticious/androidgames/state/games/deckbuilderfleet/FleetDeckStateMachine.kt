package com.xanticious.androidgames.state.games.deckbuilderfleet

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

/** High-level match phases observed by the composable. */
enum class FleetPhase {
    IDLE, SETUP, TURN_START, PLAYER_ACTIONS, OPPONENT_TURN, CHECK_END, VICTORY, DEFEAT
}

private sealed class FleetState : DefaultState() {
    data object Idle          : FleetState()
    data object Setup         : FleetState()
    data object TurnStart     : FleetState()
    data object PlayerActions : FleetState()
    data object OpponentTurn  : FleetState()
    data object CheckEnd      : FleetState()
    data object Victory       : FleetState()
    data object Defeat        : FleetState()
}

private sealed interface FleetEvent : Event {
    data object MatchStarted       : FleetEvent
    data object DecksBuilt         : FleetEvent
    data object HandDrawn          : FleetEvent
    data object TurnEnded          : FleetEvent
    data object OpponentResolved   : FleetEvent
    data object OpponentHealthZero : FleetEvent
    data object PlayerHealthZero   : FleetEvent
    data object Continue           : FleetEvent
    data object Rematch            : FleetEvent
}

/**
 * Drives Deck Builder Fleet turn-phase transitions.  All game logic lives in
 * [com.xanticious.androidgames.controller.games.deckbuilderfleet.DeckBuilderFleetController];
 * this machine only tracks which phase the match is in so the view can react.
 *
 * The [scope] is injectable for plain JVM unit tests without the Android
 * main dispatcher.
 */
class FleetDeckStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(FleetPhase.IDLE)
    val phase: StateFlow<FleetPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(FleetState.Idle) {
            transition<FleetEvent.MatchStarted> {
                targetState = FleetState.Setup
                onTriggered { _phase.value = FleetPhase.SETUP }
            }
        }
        addState(FleetState.Setup) {
            transition<FleetEvent.DecksBuilt> {
                targetState = FleetState.TurnStart
                onTriggered { _phase.value = FleetPhase.TURN_START }
            }
        }
        addState(FleetState.TurnStart) {
            transition<FleetEvent.HandDrawn> {
                targetState = FleetState.PlayerActions
                onTriggered { _phase.value = FleetPhase.PLAYER_ACTIONS }
            }
        }
        addState(FleetState.PlayerActions) {
            transition<FleetEvent.TurnEnded> {
                targetState = FleetState.OpponentTurn
                onTriggered { _phase.value = FleetPhase.OPPONENT_TURN }
            }
        }
        addState(FleetState.OpponentTurn) {
            transition<FleetEvent.OpponentResolved> {
                targetState = FleetState.CheckEnd
                onTriggered { _phase.value = FleetPhase.CHECK_END }
            }
        }
        addState(FleetState.CheckEnd) {
            transition<FleetEvent.OpponentHealthZero> {
                targetState = FleetState.Victory
                onTriggered { _phase.value = FleetPhase.VICTORY }
            }
            transition<FleetEvent.PlayerHealthZero> {
                targetState = FleetState.Defeat
                onTriggered { _phase.value = FleetPhase.DEFEAT }
            }
            transition<FleetEvent.Continue> {
                targetState = FleetState.TurnStart
                onTriggered { _phase.value = FleetPhase.TURN_START }
            }
        }
        addState(FleetState.Victory) {
            transition<FleetEvent.Rematch> {
                targetState = FleetState.Idle
                onTriggered { _phase.value = FleetPhase.IDLE }
            }
        }
        addState(FleetState.Defeat) {
            transition<FleetEvent.Rematch> {
                targetState = FleetState.Idle
                onTriggered { _phase.value = FleetPhase.IDLE }
            }
        }
    }

    fun startMatch()         = machine.processEventByLaunch(FleetEvent.MatchStarted)
    fun decksBuilt()         = machine.processEventByLaunch(FleetEvent.DecksBuilt)
    fun handDrawn()          = machine.processEventByLaunch(FleetEvent.HandDrawn)
    fun turnEnded()          = machine.processEventByLaunch(FleetEvent.TurnEnded)
    fun opponentResolved()   = machine.processEventByLaunch(FleetEvent.OpponentResolved)
    fun opponentHealthZero() = machine.processEventByLaunch(FleetEvent.OpponentHealthZero)
    fun playerHealthZero()   = machine.processEventByLaunch(FleetEvent.PlayerHealthZero)
    fun continueGame()       = machine.processEventByLaunch(FleetEvent.Continue)
    fun rematch()            = machine.processEventByLaunch(FleetEvent.Rematch)
}
