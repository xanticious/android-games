package com.xanticious.androidgames.state.games.memorylanes

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

enum class MemoryLanesPhase { IDLE, REVEALING, BUILDING, VALIDATING, GAME_OVER }

private sealed class LaneState : DefaultState() {
    data object Idle : LaneState()
    data object Revealing : LaneState()
    data object Building : LaneState()
    data object Validating : LaneState()
    data object GameOver : LaneState()
}

private sealed interface LaneEvent : Event {
    data object StartGame : LaneEvent
    data object RevealComplete : LaneEvent
    data object TileAdded : LaneEvent
    data object TileUndo : LaneEvent
    data object DoneSubmitted : LaneEvent
    data object SequenceCorrect : LaneEvent
    data object SequenceWrong : LaneEvent
    data object PlayAgain : LaneEvent
    data object BackToMenu : LaneEvent
}

class MemoryLanesStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(MemoryLanesPhase.IDLE)
    val phase: StateFlow<MemoryLanesPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(LaneState.Idle) {
            transition<LaneEvent.StartGame> {
                targetState = LaneState.Revealing
                onTriggered { _phase.value = MemoryLanesPhase.REVEALING }
            }
        }
        addState(LaneState.Revealing) {
            transition<LaneEvent.RevealComplete> {
                targetState = LaneState.Building
                onTriggered { _phase.value = MemoryLanesPhase.BUILDING }
            }
        }
        addState(LaneState.Building) {
            transition<LaneEvent.TileAdded> {
                targetState = LaneState.Building
                onTriggered { _phase.value = MemoryLanesPhase.BUILDING }
            }
            transition<LaneEvent.TileUndo> {
                targetState = LaneState.Building
                onTriggered { _phase.value = MemoryLanesPhase.BUILDING }
            }
            transition<LaneEvent.DoneSubmitted> {
                targetState = LaneState.Validating
                onTriggered { _phase.value = MemoryLanesPhase.VALIDATING }
            }
        }
        addState(LaneState.Validating) {
            transition<LaneEvent.SequenceCorrect> {
                targetState = LaneState.Revealing
                onTriggered { _phase.value = MemoryLanesPhase.REVEALING }
            }
            transition<LaneEvent.SequenceWrong> {
                targetState = LaneState.GameOver
                onTriggered { _phase.value = MemoryLanesPhase.GAME_OVER }
            }
        }
        addState(LaneState.GameOver) {
            transition<LaneEvent.PlayAgain> {
                targetState = LaneState.Revealing
                onTriggered { _phase.value = MemoryLanesPhase.REVEALING }
            }
            transition<LaneEvent.BackToMenu> {
                targetState = LaneState.Idle
                onTriggered { _phase.value = MemoryLanesPhase.IDLE }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(LaneEvent.StartGame)
    fun revealComplete() = machine.processEventByLaunch(LaneEvent.RevealComplete)
    fun tileAdded() = machine.processEventByLaunch(LaneEvent.TileAdded)
    fun tileUndo() = machine.processEventByLaunch(LaneEvent.TileUndo)
    fun doneSubmitted() = machine.processEventByLaunch(LaneEvent.DoneSubmitted)
    fun sequenceCorrect() = machine.processEventByLaunch(LaneEvent.SequenceCorrect)
    fun sequenceWrong() = machine.processEventByLaunch(LaneEvent.SequenceWrong)
    fun playAgain() = machine.processEventByLaunch(LaneEvent.PlayAgain)
    fun backToMenu() = machine.processEventByLaunch(LaneEvent.BackToMenu)
}
