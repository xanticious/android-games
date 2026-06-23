package com.xanticious.androidgames.state.games.yahtzee

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

enum class YahtzeePhase { IDLE, PLAYER_ROLLING, PLAYER_SCORING, AI_TURN, GAME_OVER }

private sealed class MatchState : DefaultState() {
    data object Idle : MatchState()
    data object PlayerRolling : MatchState()
    data object PlayerScoring : MatchState()
    data object AiTurn : MatchState()
    data object GameOver : MatchState()
}

private sealed interface YahtzeeEvent : Event {
    data object MatchStarted : YahtzeeEvent
    data object DiceRolledCanContinue : YahtzeeEvent
    data object RollLimitReached : YahtzeeEvent
    data object DiceLocked : YahtzeeEvent
    data object NextAiTurn : YahtzeeEvent
    data object NextHumanTurn : YahtzeeEvent
    data object ScoreAppliedAndGameComplete : YahtzeeEvent
    data object AiCompletedHumanTurn : YahtzeeEvent
    data object AiCompletedGame : YahtzeeEvent
    data object Rematch : YahtzeeEvent
    data object GoToMenu : YahtzeeEvent
}

class YahtzeeStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(YahtzeePhase.IDLE)
    val phase: StateFlow<YahtzeePhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(MatchState.Idle) {
            transition<YahtzeeEvent.MatchStarted> {
                targetState = MatchState.PlayerRolling
                onTriggered { _phase.value = YahtzeePhase.PLAYER_ROLLING }
            }
        }
        addState(MatchState.PlayerRolling) {
            transition<YahtzeeEvent.DiceLocked> {
                targetState = MatchState.PlayerRolling
                onTriggered { _phase.value = YahtzeePhase.PLAYER_ROLLING }
            }
            transition<YahtzeeEvent.DiceRolledCanContinue> {
                targetState = MatchState.PlayerRolling
                onTriggered { _phase.value = YahtzeePhase.PLAYER_ROLLING }
            }
            transition<YahtzeeEvent.RollLimitReached> {
                targetState = MatchState.PlayerScoring
                onTriggered { _phase.value = YahtzeePhase.PLAYER_SCORING }
            }
            transition<YahtzeeEvent.NextAiTurn> {
                targetState = MatchState.AiTurn
                onTriggered { _phase.value = YahtzeePhase.AI_TURN }
            }
            transition<YahtzeeEvent.ScoreAppliedAndGameComplete> {
                targetState = MatchState.GameOver
                onTriggered { _phase.value = YahtzeePhase.GAME_OVER }
            }
        }
        addState(MatchState.PlayerScoring) {
            transition<YahtzeeEvent.NextAiTurn> {
                targetState = MatchState.AiTurn
                onTriggered { _phase.value = YahtzeePhase.AI_TURN }
            }
            transition<YahtzeeEvent.ScoreAppliedAndGameComplete> {
                targetState = MatchState.GameOver
                onTriggered { _phase.value = YahtzeePhase.GAME_OVER }
            }
        }
        addState(MatchState.AiTurn) {
            transition<YahtzeeEvent.AiCompletedHumanTurn> {
                targetState = MatchState.PlayerRolling
                onTriggered { _phase.value = YahtzeePhase.PLAYER_ROLLING }
            }
            transition<YahtzeeEvent.AiCompletedGame> {
                targetState = MatchState.GameOver
                onTriggered { _phase.value = YahtzeePhase.GAME_OVER }
            }
        }
        addState(MatchState.GameOver) {
            transition<YahtzeeEvent.Rematch> {
                targetState = MatchState.PlayerRolling
                onTriggered { _phase.value = YahtzeePhase.PLAYER_ROLLING }
            }
            transition<YahtzeeEvent.GoToMenu> {
                targetState = MatchState.Idle
                onTriggered { _phase.value = YahtzeePhase.IDLE }
            }
        }
    }

    fun startMatch() = machine.processEventByLaunch(YahtzeeEvent.MatchStarted)
    fun diceRolled(rollsLeft: Int) = machine.processEventByLaunch(
        if (rollsLeft <= 0) YahtzeeEvent.RollLimitReached else YahtzeeEvent.DiceRolledCanContinue
    )
    fun diceLocked() = machine.processEventByLaunch(YahtzeeEvent.DiceLocked)
    fun categorySelected(gameOver: Boolean) = machine.processEventByLaunch(
        if (gameOver) YahtzeeEvent.ScoreAppliedAndGameComplete else YahtzeeEvent.NextAiTurn
    )
    fun aiFinished(gameOver: Boolean) = machine.processEventByLaunch(
        if (gameOver) YahtzeeEvent.AiCompletedGame else YahtzeeEvent.AiCompletedHumanTurn
    )
    fun rematch() = machine.processEventByLaunch(YahtzeeEvent.Rematch)
    fun goToMenu() = machine.processEventByLaunch(YahtzeeEvent.GoToMenu)
}
