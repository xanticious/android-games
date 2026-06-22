package com.xanticious.androidgames.state.games.hex

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

/** High-level Hex turn phases observed by the composable. */
enum class HexPhase { PLAYER_TURN, AI_TURN, GAME_OVER }

private sealed class MatchState : DefaultState() {
    data object PlayerTurn : MatchState()
    data object AiTurn : MatchState()
    data object GameOver : MatchState()
}

private sealed interface MatchEvent : Event {
    data object PlayerStonePlaced : MatchEvent
    data object AiStonePlaced : MatchEvent
    data object GameEnded : MatchEvent
    data object Rematch : MatchEvent
}

class HexStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(HexPhase.PLAYER_TURN)
    val phase: StateFlow<HexPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(MatchState.PlayerTurn) {
            transition<MatchEvent.PlayerStonePlaced> {
                targetState = MatchState.AiTurn
                onTriggered { _phase.value = HexPhase.AI_TURN }
            }
            transition<MatchEvent.GameEnded> {
                targetState = MatchState.GameOver
                onTriggered { _phase.value = HexPhase.GAME_OVER }
            }
        }
        addState(MatchState.AiTurn) {
            transition<MatchEvent.AiStonePlaced> {
                targetState = MatchState.PlayerTurn
                onTriggered { _phase.value = HexPhase.PLAYER_TURN }
            }
            transition<MatchEvent.GameEnded> {
                targetState = MatchState.GameOver
                onTriggered { _phase.value = HexPhase.GAME_OVER }
            }
        }
        addState(MatchState.GameOver) {
            transition<MatchEvent.Rematch> {
                targetState = MatchState.PlayerTurn
                onTriggered { _phase.value = HexPhase.PLAYER_TURN }
            }
        }
    }

    fun playerMoved() = machine.processEventByLaunch(MatchEvent.PlayerStonePlaced)
    fun aiMoved() = machine.processEventByLaunch(MatchEvent.AiStonePlaced)
    fun gameEnded() = machine.processEventByLaunch(MatchEvent.GameEnded)
    fun rematch() = machine.processEventByLaunch(MatchEvent.Rematch)
}
