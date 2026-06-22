package com.xanticious.androidgames.state.games.connectfour

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

/** Turn-level phases observed by the Connect Four composable. */
enum class ConnectFourPhase { PLAYER_TURN, AI_TURN, GAME_OVER }

private sealed class MatchState : DefaultState() {
    data object PlayerTurn : MatchState()
    data object AiTurn : MatchState()
    data object GameOver : MatchState()
}

private sealed interface MatchEvent : Event {
    data object PlayerMoved : MatchEvent
    data object AiMoved : MatchEvent
    data object GameEnded : MatchEvent
    data object Reset : MatchEvent
}

/** KStateMachine wrapper for Connect Four turn transitions. */
class ConnectFourStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(ConnectFourPhase.PLAYER_TURN)
    val phase: StateFlow<ConnectFourPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(MatchState.PlayerTurn) {
            transition<MatchEvent.PlayerMoved> {
                targetState = MatchState.AiTurn
                onTriggered { _phase.value = ConnectFourPhase.AI_TURN }
            }
            transition<MatchEvent.GameEnded> {
                targetState = MatchState.GameOver
                onTriggered { _phase.value = ConnectFourPhase.GAME_OVER }
            }
            transition<MatchEvent.Reset> {
                targetState = MatchState.PlayerTurn
                onTriggered { _phase.value = ConnectFourPhase.PLAYER_TURN }
            }
        }
        addState(MatchState.AiTurn) {
            transition<MatchEvent.AiMoved> {
                targetState = MatchState.PlayerTurn
                onTriggered { _phase.value = ConnectFourPhase.PLAYER_TURN }
            }
            transition<MatchEvent.GameEnded> {
                targetState = MatchState.GameOver
                onTriggered { _phase.value = ConnectFourPhase.GAME_OVER }
            }
            transition<MatchEvent.Reset> {
                targetState = MatchState.PlayerTurn
                onTriggered { _phase.value = ConnectFourPhase.PLAYER_TURN }
            }
        }
        addState(MatchState.GameOver) {
            transition<MatchEvent.Reset> {
                targetState = MatchState.PlayerTurn
                onTriggered { _phase.value = ConnectFourPhase.PLAYER_TURN }
            }
        }
    }

    fun playerMoved() = machine.processEventByLaunch(MatchEvent.PlayerMoved)
    fun aiMoved() = machine.processEventByLaunch(MatchEvent.AiMoved)
    fun gameEnded() = machine.processEventByLaunch(MatchEvent.GameEnded)
    fun reset() = machine.processEventByLaunch(MatchEvent.Reset)
}
