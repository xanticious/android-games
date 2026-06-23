package com.xanticious.androidgames.state.games.paircollector

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

enum class PairCollectorPhase { IDLE, DEALING_ROUND, PLAYING_ROUND, ROUND_COMPLETE, GAME_OVER }

private sealed class CollectorState : DefaultState() {
    data object Idle : CollectorState()
    data object DealingRound : CollectorState()
    data object PlayingRound : CollectorState()
    data object RoundComplete : CollectorState()
    data object GameOver : CollectorState()
}

private sealed interface CollectorEvent : Event {
    data object StartGame : CollectorEvent
    data object RoundReady : CollectorEvent
    data object FirstCardTapped : CollectorEvent
    data object CardDeselected : CollectorEvent
    data object SecondCardMatched : CollectorEvent
    data object SecondCardMismatched : CollectorEvent
    data object RoundTimeExpired : CollectorEvent
    data object MoreRoundsRemaining : CollectorEvent
    data object AllRoundsComplete : CollectorEvent
    data object StrikesExhausted : CollectorEvent
    data object Rematch : CollectorEvent
    data object BackToMenu : CollectorEvent
}

class PairCollectorStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(PairCollectorPhase.IDLE)
    val phase: StateFlow<PairCollectorPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(CollectorState.Idle) {
            transition<CollectorEvent.StartGame> {
                targetState = CollectorState.DealingRound
                onTriggered { _phase.value = PairCollectorPhase.DEALING_ROUND }
            }
        }
        addState(CollectorState.DealingRound) {
            transition<CollectorEvent.RoundReady> {
                targetState = CollectorState.PlayingRound
                onTriggered { _phase.value = PairCollectorPhase.PLAYING_ROUND }
            }
        }
        addState(CollectorState.PlayingRound) {
            transition<CollectorEvent.FirstCardTapped> {
                targetState = CollectorState.PlayingRound
                onTriggered { _phase.value = PairCollectorPhase.PLAYING_ROUND }
            }
            transition<CollectorEvent.CardDeselected> {
                targetState = CollectorState.PlayingRound
                onTriggered { _phase.value = PairCollectorPhase.PLAYING_ROUND }
            }
            transition<CollectorEvent.SecondCardMatched> {
                targetState = CollectorState.RoundComplete
                onTriggered { _phase.value = PairCollectorPhase.ROUND_COMPLETE }
            }
            transition<CollectorEvent.SecondCardMismatched> {
                targetState = CollectorState.PlayingRound
                onTriggered { _phase.value = PairCollectorPhase.PLAYING_ROUND }
            }
            transition<CollectorEvent.RoundTimeExpired> {
                targetState = CollectorState.PlayingRound
                onTriggered { _phase.value = PairCollectorPhase.PLAYING_ROUND }
            }
            transition<CollectorEvent.StrikesExhausted> {
                targetState = CollectorState.GameOver
                onTriggered { _phase.value = PairCollectorPhase.GAME_OVER }
            }
        }
        addState(CollectorState.RoundComplete) {
            transition<CollectorEvent.MoreRoundsRemaining> {
                targetState = CollectorState.DealingRound
                onTriggered { _phase.value = PairCollectorPhase.DEALING_ROUND }
            }
            transition<CollectorEvent.AllRoundsComplete> {
                targetState = CollectorState.GameOver
                onTriggered { _phase.value = PairCollectorPhase.GAME_OVER }
            }
        }
        addState(CollectorState.GameOver) {
            transition<CollectorEvent.Rematch> {
                targetState = CollectorState.DealingRound
                onTriggered { _phase.value = PairCollectorPhase.DEALING_ROUND }
            }
            transition<CollectorEvent.BackToMenu> {
                targetState = CollectorState.Idle
                onTriggered { _phase.value = PairCollectorPhase.IDLE }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(CollectorEvent.StartGame)
    fun roundReady() = machine.processEventByLaunch(CollectorEvent.RoundReady)
    fun firstCardTapped() = machine.processEventByLaunch(CollectorEvent.FirstCardTapped)
    fun cardDeselected() = machine.processEventByLaunch(CollectorEvent.CardDeselected)
    fun secondCardMatched() = machine.processEventByLaunch(CollectorEvent.SecondCardMatched)
    fun secondCardMismatched() = machine.processEventByLaunch(CollectorEvent.SecondCardMismatched)
    fun roundTimeExpired() = machine.processEventByLaunch(CollectorEvent.RoundTimeExpired)
    fun moreRoundsRemaining() = machine.processEventByLaunch(CollectorEvent.MoreRoundsRemaining)
    fun allRoundsComplete() = machine.processEventByLaunch(CollectorEvent.AllRoundsComplete)
    fun strikesExhausted() = machine.processEventByLaunch(CollectorEvent.StrikesExhausted)
    fun rematch() = machine.processEventByLaunch(CollectorEvent.Rematch)
    fun backToMenu() = machine.processEventByLaunch(CollectorEvent.BackToMenu)
}
