package com.xanticious.androidgames.state.games.battleships

import com.xanticious.androidgames.controller.games.battleships.BattleshipsController
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.battleships.BattleshipsCell
import com.xanticious.androidgames.model.games.battleships.BattleshipsConfig
import com.xanticious.androidgames.model.games.battleships.BattleshipsResult
import com.xanticious.androidgames.model.games.battleships.BattleshipsState
import com.xanticious.androidgames.model.games.battleships.BattleshipsTurn
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

enum class BattleshipsPhase { PLAYER_TURN, AI_TURN, GAME_OVER }

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

/**
 * Owns Battleships turn order and firing. Both fleets are placed automatically at
 * match start; each side fires a single shot per turn until one fleet is sunk.
 */
class BattleshipsStateMachine(
    difficulty: GameDifficulty = GameDifficulty.MEDIUM,
    private val controller: BattleshipsController = BattleshipsController(),
    private val random: Random = Random(0),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val config: BattleshipsConfig = controller.configFor(difficulty)
    private val _state = MutableStateFlow(newMatch())
    val state: StateFlow<BattleshipsState> = _state.asStateFlow()

    private val _phase = MutableStateFlow(BattleshipsPhase.PLAYER_TURN)
    val phase: StateFlow<BattleshipsPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(TurnState.PlayerTurn) {
            transition<TurnEvent.AiTurnStarted> {
                targetState = TurnState.AiTurn
                onTriggered { _phase.value = BattleshipsPhase.AI_TURN }
            }
            transition<TurnEvent.GameFinished> {
                targetState = TurnState.GameOver
                onTriggered { _phase.value = BattleshipsPhase.GAME_OVER }
            }
        }
        addState(TurnState.AiTurn) {
            transition<TurnEvent.PlayerTurnStarted> {
                targetState = TurnState.PlayerTurn
                onTriggered { _phase.value = BattleshipsPhase.PLAYER_TURN }
            }
            transition<TurnEvent.GameFinished> {
                targetState = TurnState.GameOver
                onTriggered { _phase.value = BattleshipsPhase.GAME_OVER }
            }
        }
        addState(TurnState.GameOver) {
            transition<TurnEvent.Rematch> {
                targetState = TurnState.PlayerTurn
                onTriggered { _phase.value = BattleshipsPhase.PLAYER_TURN }
            }
        }
    }

    fun playerFireSelected(row: Int, col: Int) {
        if (_phase.value != BattleshipsPhase.PLAYER_TURN) return
        val current = _state.value
        val step = controller.fire(current.aiBoard, BattleshipsCell(row, col))
        if (!step.accepted) return
        val playerWon = step.report.allShipsSunk
        _state.value = current.copy(
            aiBoard = step.board,
            playerShots = current.playerShots + 1,
            lastShot = step.report,
            result = if (playerWon) BattleshipsResult.PLAYER_WIN else current.result,
            currentTurn = if (playerWon) BattleshipsTurn.PLAYER else BattleshipsTurn.AI
        )
        syncPhaseToState()
    }

    fun performAiTurn() {
        if (_phase.value != BattleshipsPhase.AI_TURN) return
        val current = _state.value
        val target = controller.aiTarget(current.playerBoard, config.difficulty, random) ?: return
        val step = controller.fire(current.playerBoard, target)
        if (!step.accepted) return
        val aiWon = step.report.allShipsSunk
        _state.value = current.copy(
            playerBoard = step.board,
            aiShots = current.aiShots + 1,
            turnNumber = current.turnNumber + 1,
            lastShot = step.report,
            result = if (aiWon) BattleshipsResult.AI_WIN else current.result,
            currentTurn = if (aiWon) BattleshipsTurn.AI else BattleshipsTurn.PLAYER
        )
        syncPhaseToState()
    }

    fun reset() {
        _state.value = newMatch()
        when (_phase.value) {
            BattleshipsPhase.PLAYER_TURN -> Unit
            BattleshipsPhase.AI_TURN -> machine.processEventByLaunch(TurnEvent.PlayerTurnStarted)
            BattleshipsPhase.GAME_OVER -> machine.processEventByLaunch(TurnEvent.Rematch)
        }
    }

    private fun newMatch(): BattleshipsState {
        val base = BattleshipsState.initial(config)
        return base.copy(
            playerBoard = controller.randomFleetBoard(config, random),
            aiBoard = controller.randomFleetBoard(config, random)
        )
    }

    private fun syncPhaseToState() {
        val next = when {
            _state.value.isGameOver -> BattleshipsPhase.GAME_OVER
            _state.value.currentTurn == BattleshipsTurn.PLAYER -> BattleshipsPhase.PLAYER_TURN
            else -> BattleshipsPhase.AI_TURN
        }
        if (next == _phase.value) return
        val event = when (next) {
            BattleshipsPhase.PLAYER_TURN -> TurnEvent.PlayerTurnStarted
            BattleshipsPhase.AI_TURN -> TurnEvent.AiTurnStarted
            BattleshipsPhase.GAME_OVER -> TurnEvent.GameFinished
        }
        machine.processEventByLaunch(event)
    }
}
