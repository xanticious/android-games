package com.xanticious.androidgames.state.games.zilch

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

enum class ZilchPhase { IDLE, PLAYER_ROLLING, PLAYER_DECIDING, AI_TURN, GAME_OVER }

private sealed class MatchState : DefaultState() {
    data object Idle : MatchState()
    data object PlayerRolling : MatchState()
    data object PlayerDeciding : MatchState()
    data object AiTurn : MatchState()
    data object GameOver : MatchState()
}

private sealed interface MatchEvent : Event {
    data object MatchStarted : MatchEvent
    data object DiceRolled : MatchEvent
    data object PlayerBanked : MatchEvent
    data object PlayerZilched : MatchEvent
    data object AiTurnResolved : MatchEvent
    data object MatchEnded : MatchEvent
    data object Rematch : MatchEvent
}

class ZilchStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(ZilchPhase.IDLE)
    val phase: StateFlow<ZilchPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(MatchState.Idle) {
            transition<MatchEvent.MatchStarted> {
                targetState = MatchState.PlayerRolling
                onTriggered { _phase.value = ZilchPhase.PLAYER_ROLLING }
            }
        }
        addState(MatchState.PlayerRolling) {
            transition<MatchEvent.DiceRolled> {
                targetState = MatchState.PlayerDeciding
                onTriggered { _phase.value = ZilchPhase.PLAYER_DECIDING }
            }
            transition<MatchEvent.PlayerZilched> {
                targetState = MatchState.AiTurn
                onTriggered { _phase.value = ZilchPhase.AI_TURN }
            }
            transition<MatchEvent.MatchEnded> {
                targetState = MatchState.GameOver
                onTriggered { _phase.value = ZilchPhase.GAME_OVER }
            }
        }
        addState(MatchState.PlayerDeciding) {
            transition<MatchEvent.DiceRolled> {
                targetState = MatchState.PlayerDeciding
                onTriggered { _phase.value = ZilchPhase.PLAYER_DECIDING }
            }
            transition<MatchEvent.PlayerBanked> {
                targetState = MatchState.AiTurn
                onTriggered { _phase.value = ZilchPhase.AI_TURN }
            }
            transition<MatchEvent.PlayerZilched> {
                targetState = MatchState.AiTurn
                onTriggered { _phase.value = ZilchPhase.AI_TURN }
            }
            transition<MatchEvent.MatchEnded> {
                targetState = MatchState.GameOver
                onTriggered { _phase.value = ZilchPhase.GAME_OVER }
            }
        }
        addState(MatchState.AiTurn) {
            transition<MatchEvent.AiTurnResolved> {
                targetState = MatchState.PlayerRolling
                onTriggered { _phase.value = ZilchPhase.PLAYER_ROLLING }
            }
            transition<MatchEvent.MatchEnded> {
                targetState = MatchState.GameOver
                onTriggered { _phase.value = ZilchPhase.GAME_OVER }
            }
        }
        addState(MatchState.GameOver) {
            transition<MatchEvent.Rematch> {
                targetState = MatchState.PlayerRolling
                onTriggered { _phase.value = ZilchPhase.PLAYER_ROLLING }
            }
        }
    }

    fun startMatch() = machine.processEventByLaunch(MatchEvent.MatchStarted)
    fun diceRolled() = machine.processEventByLaunch(MatchEvent.DiceRolled)
    fun playerBanked() = machine.processEventByLaunch(MatchEvent.PlayerBanked)
    fun playerZilched() = machine.processEventByLaunch(MatchEvent.PlayerZilched)
    fun aiTurnResolved() = machine.processEventByLaunch(MatchEvent.AiTurnResolved)
    fun matchEnded() = machine.processEventByLaunch(MatchEvent.MatchEnded)
    fun rematch() = machine.processEventByLaunch(MatchEvent.Rematch)
}
