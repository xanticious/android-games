package com.xanticious.androidgames.state.games.checkers

import com.xanticious.androidgames.controller.games.checkers.CheckersController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.checkers.CheckersConfig
import com.xanticious.androidgames.model.games.checkers.CheckersSide
import com.xanticious.androidgames.model.games.checkers.CheckersSquare
import com.xanticious.androidgames.model.games.checkers.CheckersState
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

enum class CheckersPhase { IDLE, PLAYER_TURN, AI_TURN, GAME_OVER }

private sealed class MatchState : DefaultState() {
    data object Idle : MatchState()
    data object PlayerTurn : MatchState()
    data object AiTurn : MatchState()
    data object GameOver : MatchState()
}

private sealed interface CheckersEvent : Event {
    data object MatchStartedForPlayer : CheckersEvent
    data object MatchStartedForAi : CheckersEvent
    data object PlayerTurnEnded : CheckersEvent
    data object AiTurnEnded : CheckersEvent
    data object GameFinished : CheckersEvent
    data object RematchForPlayer : CheckersEvent
    data object RematchForAi : CheckersEvent
    data object GoToMenu : CheckersEvent
}

class CheckersStateMachine(
    private val controller: CheckersController = CheckersController(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(CheckersPhase.IDLE)
    val phase: StateFlow<CheckersPhase> = _phase.asStateFlow()

    private val _state = MutableStateFlow(CheckersState.initial(controller.configFor(GameDifficulty.MEDIUM)))
    val state: StateFlow<CheckersState> = _state.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(MatchState.Idle) {
            transition<CheckersEvent.MatchStartedForPlayer> {
                targetState = MatchState.PlayerTurn
                onTriggered { _phase.value = CheckersPhase.PLAYER_TURN }
            }
            transition<CheckersEvent.MatchStartedForAi> {
                targetState = MatchState.AiTurn
                onTriggered { _phase.value = CheckersPhase.AI_TURN }
            }
        }
        addState(MatchState.PlayerTurn) {
            transition<CheckersEvent.PlayerTurnEnded> {
                targetState = MatchState.AiTurn
                onTriggered { _phase.value = CheckersPhase.AI_TURN }
            }
            transition<CheckersEvent.GameFinished> {
                targetState = MatchState.GameOver
                onTriggered { _phase.value = CheckersPhase.GAME_OVER }
            }
        }
        addState(MatchState.AiTurn) {
            transition<CheckersEvent.AiTurnEnded> {
                targetState = MatchState.PlayerTurn
                onTriggered { _phase.value = CheckersPhase.PLAYER_TURN }
            }
            transition<CheckersEvent.GameFinished> {
                targetState = MatchState.GameOver
                onTriggered { _phase.value = CheckersPhase.GAME_OVER }
            }
        }
        addState(MatchState.GameOver) {
            transition<CheckersEvent.RematchForPlayer> {
                targetState = MatchState.PlayerTurn
                onTriggered { _phase.value = CheckersPhase.PLAYER_TURN }
            }
            transition<CheckersEvent.RematchForAi> {
                targetState = MatchState.AiTurn
                onTriggered { _phase.value = CheckersPhase.AI_TURN }
            }
            transition<CheckersEvent.GoToMenu> {
                targetState = MatchState.Idle
                onTriggered { _phase.value = CheckersPhase.IDLE }
            }
        }
    }

    fun startMatch(config: CheckersConfig) {
        _state.value = CheckersState.initial(config)
        machine.processEventByLaunch(startEventFor(_state.value))
    }

    fun startMatch(difficulty: GameDifficulty = GameDifficulty.MEDIUM) = startMatch(controller.configFor(difficulty))

    fun tapSquare(square: CheckersSquare) {
        if (_phase.value != CheckersPhase.PLAYER_TURN) return
        val beforePlayer = _state.value.currentPlayer
        val selected = _state.value.selectedSquare
        _state.value = if (selected == null || selected == square) {
            controller.selectSquare(_state.value, square)
        } else {
            controller.applySelectedMove(_state.value, square)
        }
        dispatchAfterTurn(beforePlayer)
    }

    fun clearSelection() {
        _state.value = controller.clearSelection(_state.value)
    }

    fun playAiTurn(random: Random = Random.Default) {
        if (_phase.value != CheckersPhase.AI_TURN) return
        val beforePlayer = _state.value.currentPlayer
        val move = controller.chooseAiMove(_state.value, random)
        _state.value = if (move == null) controller.finishIfGameOver(_state.value) else controller.applyMove(_state.value, move)
        dispatchAfterTurn(beforePlayer)
    }

    fun rematch() {
        val config = _state.value.config
        _state.value = CheckersState.initial(config)
        machine.processEventByLaunch(if (_state.value.currentPlayer == config.playerSide) CheckersEvent.RematchForPlayer else CheckersEvent.RematchForAi)
    }

    fun goToMenu() = machine.processEventByLaunch(CheckersEvent.GoToMenu)

    private fun dispatchAfterTurn(beforePlayer: CheckersSide) {
        val current = _state.value
        if (current.result != null) {
            machine.processEventByLaunch(CheckersEvent.GameFinished)
            return
        }
        if (current.currentPlayer == beforePlayer) return
        if (current.currentPlayer == current.config.playerSide) {
            machine.processEventByLaunch(CheckersEvent.AiTurnEnded)
        } else {
            machine.processEventByLaunch(CheckersEvent.PlayerTurnEnded)
        }
    }

    private fun startEventFor(state: CheckersState): CheckersEvent =
        if (state.currentPlayer == state.config.playerSide) CheckersEvent.MatchStartedForPlayer else CheckersEvent.MatchStartedForAi
}
