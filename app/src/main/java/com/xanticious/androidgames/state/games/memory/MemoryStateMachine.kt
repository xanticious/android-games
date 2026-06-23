package com.xanticious.androidgames.state.games.memory

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

enum class MemoryPhase { FIRST_FLIP, SECOND_FLIP, RESOLVING, AI_TURN, GAME_OVER }

private sealed class TurnState : DefaultState() {
    data object FirstFlip : TurnState()
    data object SecondFlip : TurnState()
    data object Resolving : TurnState()
    data object AiTurn : TurnState()
    data object GameOver : TurnState()
}

private sealed interface TurnEvent : Event {
    data object FirstCardFlipped : TurnEvent
    data object SecondCardFlipped : TurnEvent
    data object AiCardsChosen : TurnEvent
    data object HumanTurnStarted : TurnEvent
    data object AiTurnStarted : TurnEvent
    data object GameFinished : TurnEvent
    data object Rematch : TurnEvent
}

class MemoryStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(MemoryPhase.FIRST_FLIP)
    val phase: StateFlow<MemoryPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(TurnState.FirstFlip) {
            transition<TurnEvent.FirstCardFlipped> {
                targetState = TurnState.SecondFlip
                onTriggered { _phase.value = MemoryPhase.SECOND_FLIP }
            }
            transition<TurnEvent.AiTurnStarted> {
                targetState = TurnState.AiTurn
                onTriggered { _phase.value = MemoryPhase.AI_TURN }
            }
        }
        addState(TurnState.SecondFlip) {
            transition<TurnEvent.SecondCardFlipped> {
                targetState = TurnState.Resolving
                onTriggered { _phase.value = MemoryPhase.RESOLVING }
            }
        }
        addState(TurnState.Resolving) {
            transition<TurnEvent.HumanTurnStarted> {
                targetState = TurnState.FirstFlip
                onTriggered { _phase.value = MemoryPhase.FIRST_FLIP }
            }
            transition<TurnEvent.AiTurnStarted> {
                targetState = TurnState.AiTurn
                onTriggered { _phase.value = MemoryPhase.AI_TURN }
            }
            transition<TurnEvent.GameFinished> {
                targetState = TurnState.GameOver
                onTriggered { _phase.value = MemoryPhase.GAME_OVER }
            }
        }
        addState(TurnState.AiTurn) {
            transition<TurnEvent.AiCardsChosen> {
                targetState = TurnState.Resolving
                onTriggered { _phase.value = MemoryPhase.RESOLVING }
            }
            transition<TurnEvent.GameFinished> {
                targetState = TurnState.GameOver
                onTriggered { _phase.value = MemoryPhase.GAME_OVER }
            }
        }
        addState(TurnState.GameOver) {
            transition<TurnEvent.Rematch> {
                targetState = TurnState.FirstFlip
                onTriggered { _phase.value = MemoryPhase.FIRST_FLIP }
            }
        }
    }

    fun firstCardFlipped() = machine.processEventByLaunch(TurnEvent.FirstCardFlipped)
    fun secondCardFlipped() = machine.processEventByLaunch(TurnEvent.SecondCardFlipped)
    fun aiCardsChosen() = machine.processEventByLaunch(TurnEvent.AiCardsChosen)
    fun humanTurnStarted() = machine.processEventByLaunch(TurnEvent.HumanTurnStarted)
    fun aiTurnStarted() = machine.processEventByLaunch(TurnEvent.AiTurnStarted)
    fun gameFinished() = machine.processEventByLaunch(TurnEvent.GameFinished)
    fun rematch() = machine.processEventByLaunch(TurnEvent.Rematch)
}
