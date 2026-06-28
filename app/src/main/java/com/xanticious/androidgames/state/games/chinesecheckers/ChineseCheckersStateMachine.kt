package com.xanticious.androidgames.state.games.chinesecheckers

import com.xanticious.androidgames.controller.games.chinesecheckers.ChineseCheckersController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.chinesecheckers.ChineseCheckersCoordinate
import com.xanticious.androidgames.model.games.chinesecheckers.ChineseCheckersResult
import com.xanticious.androidgames.model.games.chinesecheckers.ChineseCheckersSide
import com.xanticious.androidgames.model.games.chinesecheckers.ChineseCheckersState
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

enum class ChineseCheckersPhase { PLAYER_TURN, AI_TURN, GAME_OVER }

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

class ChineseCheckersStateMachine(
    difficulty: GameDifficulty,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val controller: ChineseCheckersController = ChineseCheckersController(),
    private val random: Random = Random.Default
) {
    private val config = controller.configFor(difficulty)
    private val _phase = MutableStateFlow(ChineseCheckersPhase.PLAYER_TURN)
    val phase: StateFlow<ChineseCheckersPhase> = _phase.asStateFlow()

    private val _state = MutableStateFlow(controller.initialState(config))
    val state: StateFlow<ChineseCheckersState> = _state.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(TurnState.PlayerTurn) {
            transition<TurnEvent.PlayerMoved> {
                targetState = TurnState.AiTurn
                onTriggered { _phase.value = ChineseCheckersPhase.AI_TURN }
            }
            transition<TurnEvent.GameFinished> {
                targetState = TurnState.GameOver
                onTriggered { _phase.value = ChineseCheckersPhase.GAME_OVER }
            }
        }
        addState(TurnState.AiTurn) {
            transition<TurnEvent.AiFinished> {
                targetState = TurnState.PlayerTurn
                onTriggered { _phase.value = ChineseCheckersPhase.PLAYER_TURN }
            }
            transition<TurnEvent.GameFinished> {
                targetState = TurnState.GameOver
                onTriggered { _phase.value = ChineseCheckersPhase.GAME_OVER }
            }
        }
        addState(TurnState.GameOver) {
            transition<TurnEvent.Rematch> {
                targetState = TurnState.PlayerTurn
                onTriggered { _phase.value = ChineseCheckersPhase.PLAYER_TURN }
            }
        }
    }

    fun selectPeg(coordinate: ChineseCheckersCoordinate) {
        if (_phase.value != ChineseCheckersPhase.PLAYER_TURN) return
        _state.value = controller.selectPeg(_state.value, coordinate)
    }

    fun chooseDestination(coordinate: ChineseCheckersCoordinate) {
        if (_phase.value != ChineseCheckersPhase.PLAYER_TURN) return
        val before = _state.value
        val afterPlayer = controller.applySelectedMove(before, coordinate)
        _state.value = afterPlayer
        if (afterPlayer == before || afterPlayer.currentPlayer == ChineseCheckersSide.PLAYER && afterPlayer.result == null) return
        if (afterPlayer.result != null) {
            machine.processEventByLaunch(TurnEvent.GameFinished)
            return
        }
        machine.processEventByLaunch(TurnEvent.PlayerMoved)
        playAiTurn()
    }

    fun legalTargets(): Set<ChineseCheckersCoordinate> =
        _state.value.selectedPeg
            ?.let { controller.legalMovesForPeg(_state.value, it).map { move -> move.to }.toSet() }
            .orEmpty()

    fun reset() {
        _state.value = controller.initialState(config)
        if (_phase.value == ChineseCheckersPhase.GAME_OVER) {
            machine.processEventByLaunch(TurnEvent.Rematch)
        }
    }

    private fun playAiTurn() {
        val move = controller.chooseAiMove(_state.value, random)
        _state.value = if (move != null) {
            controller.applyMove(_state.value, move)
        } else {
            _state.value.copy(
                result = ChineseCheckersResult(
                    winner = ChineseCheckersSide.PLAYER,
                    loser = ChineseCheckersSide.AI,
                    turnsPlayed = _state.value.turnsPlayed
                ),
                message = "AI has no legal move. You win!"
            )
        }
        machine.processEventByLaunch(if (_state.value.result == null) TurnEvent.AiFinished else TurnEvent.GameFinished)
    }
}
