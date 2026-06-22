package com.xanticious.androidgames.state.games.tictactoe

import com.xanticious.androidgames.controller.games.tictactoe.TicTacToeController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.tictactoe.TicTacToeResult
import com.xanticious.androidgames.model.games.tictactoe.TicTacToeState
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

enum class TicTacToePhase { PLAYER_TURN, AI_TURN, GAME_OVER }

private sealed class TurnState : DefaultState() {
    data object PlayerTurn : TurnState()
    data object AiTurn : TurnState()
    data object GameOver : TurnState()
}

private sealed interface TurnEvent : Event {
    data object PlayerMoved : TurnEvent
    data object AiFinished : TurnEvent
    data object GameFinished : TurnEvent
    data object Rematch : TurnEvent
}

class TicTacToeStateMachine(
    difficulty: GameDifficulty,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val controller: TicTacToeController = TicTacToeController(),
    private val random: Random = Random.Default
) {
    private val config = controller.configFor(difficulty)
    private val _phase = MutableStateFlow(TicTacToePhase.PLAYER_TURN)
    val phase: StateFlow<TicTacToePhase> = _phase.asStateFlow()

    private val _state = MutableStateFlow(TicTacToeState.initial(config))
    val state: StateFlow<TicTacToeState> = _state.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(TurnState.PlayerTurn) {
            transition<TurnEvent.PlayerMoved> {
                targetState = TurnState.AiTurn
                onTriggered { _phase.value = TicTacToePhase.AI_TURN }
            }
            transition<TurnEvent.GameFinished> {
                targetState = TurnState.GameOver
                onTriggered { _phase.value = TicTacToePhase.GAME_OVER }
            }
        }
        addState(TurnState.AiTurn) {
            transition<TurnEvent.AiFinished> {
                targetState = TurnState.PlayerTurn
                onTriggered { _phase.value = TicTacToePhase.PLAYER_TURN }
            }
            transition<TurnEvent.GameFinished> {
                targetState = TurnState.GameOver
                onTriggered { _phase.value = TicTacToePhase.GAME_OVER }
            }
        }
        addState(TurnState.GameOver) {
            transition<TurnEvent.Rematch> {
                targetState = TurnState.PlayerTurn
                onTriggered { _phase.value = TicTacToePhase.PLAYER_TURN }
            }
        }
    }

    fun selectCell(cell: Int) {
        if (_phase.value != TicTacToePhase.PLAYER_TURN) return
        val afterPlayer = controller.applyMove(_state.value, cell)
        if (afterPlayer == _state.value) return
        _state.value = afterPlayer
        if (afterPlayer.result != TicTacToeResult.InProgress) {
            machine.processEventByLaunch(TurnEvent.GameFinished)
            return
        }
        machine.processEventByLaunch(TurnEvent.PlayerMoved)
        playAiTurn()
    }

    fun reset() {
        _state.value = TicTacToeState.initial(config)
        if (_phase.value == TicTacToePhase.GAME_OVER) {
            machine.processEventByLaunch(TurnEvent.Rematch)
        }
    }

    private fun playAiTurn() {
        val move = controller.chooseAiMove(_state.value, random)
        if (move >= 0) {
            _state.value = controller.applyMove(_state.value, move)
        }
        val event = if (_state.value.result == TicTacToeResult.InProgress) TurnEvent.AiFinished else TurnEvent.GameFinished
        machine.processEventByLaunch(event)
    }
}
