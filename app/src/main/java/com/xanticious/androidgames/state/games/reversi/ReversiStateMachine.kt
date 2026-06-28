package com.xanticious.androidgames.state.games.reversi

import com.xanticious.androidgames.controller.games.reversi.ReversiController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.reversi.ReversiState
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
import kotlin.random.Random

enum class ReversiPhase { PLAYER_TURN, AI_TURN, GAME_OVER }

private sealed class TurnState : DefaultState() {
    data object PlayerTurn : TurnState()
    data object AiTurn : TurnState()
    data object GameOver : TurnState()
}

private sealed interface TurnEvent : Event {
    data object PlayerTurnStarted : TurnEvent
    data object AiTurnStarted : TurnEvent
    data object GameFinished : TurnEvent
    data object Rematch : TurnEvent
}

class ReversiStateMachine(
    difficulty: GameDifficulty = GameDifficulty.MEDIUM,
    private val controller: ReversiController = ReversiController(),
    private val random: Random = Random(0),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val config = controller.configFor(difficulty)
    private val _state = MutableStateFlow(controller.initialState(config))
    val state: StateFlow<ReversiState> = _state.asStateFlow()

    private val _phase = MutableStateFlow(ReversiPhase.PLAYER_TURN)
    val phase: StateFlow<ReversiPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(TurnState.PlayerTurn) {
            transition<TurnEvent.AiTurnStarted> {
                targetState = TurnState.AiTurn
                onTriggered { _phase.value = ReversiPhase.AI_TURN }
            }
            transition<TurnEvent.GameFinished> {
                targetState = TurnState.GameOver
                onTriggered { _phase.value = ReversiPhase.GAME_OVER }
            }
        }
        addState(TurnState.AiTurn) {
            transition<TurnEvent.PlayerTurnStarted> {
                targetState = TurnState.PlayerTurn
                onTriggered { _phase.value = ReversiPhase.PLAYER_TURN }
            }
            transition<TurnEvent.GameFinished> {
                targetState = TurnState.GameOver
                onTriggered { _phase.value = ReversiPhase.GAME_OVER }
            }
        }
        addState(TurnState.GameOver) {
            transition<TurnEvent.Rematch> {
                targetState = TurnState.PlayerTurn
                onTriggered { _phase.value = ReversiPhase.PLAYER_TURN }
            }
        }
    }

    fun playerMoveSelected(row: Int, col: Int) {
        if (_phase.value != ReversiPhase.PLAYER_TURN || _state.value.currentPlayer != _state.value.config.playerDisc) return
        _state.value = controller.applyMove(_state.value, row, col)
        syncPhaseToState()
    }

    fun performAiTurn() {
        if (_phase.value != ReversiPhase.AI_TURN || _state.value.currentPlayer != _state.value.config.aiDisc) return
        val move = controller.chooseAiMove(_state.value, random)
        _state.value = if (move == null) {
            controller.skipUnavailableTurns(_state.value)
        } else {
            controller.applyMove(_state.value, move.position.row, move.position.col)
        }
        syncPhaseToState()
    }

    fun reset() {
        _state.value = controller.initialState(config)
        when (_phase.value) {
            ReversiPhase.PLAYER_TURN -> Unit
            ReversiPhase.AI_TURN -> machine.processEventByLaunch(TurnEvent.PlayerTurnStarted)
            ReversiPhase.GAME_OVER -> machine.processEventByLaunch(TurnEvent.Rematch)
        }
    }

    private fun syncPhaseToState() {
        val next = when {
            _state.value.result != null -> ReversiPhase.GAME_OVER
            _state.value.currentPlayer == _state.value.config.playerDisc -> ReversiPhase.PLAYER_TURN
            else -> ReversiPhase.AI_TURN
        }
        if (next == _phase.value) return
        val event = when (next) {
            ReversiPhase.PLAYER_TURN -> TurnEvent.PlayerTurnStarted
            ReversiPhase.AI_TURN -> TurnEvent.AiTurnStarted
            ReversiPhase.GAME_OVER -> TurnEvent.GameFinished
        }
        machine.processEventByLaunch(event)
    }
}
