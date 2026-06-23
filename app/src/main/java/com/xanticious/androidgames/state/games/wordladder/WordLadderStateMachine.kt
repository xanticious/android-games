package com.xanticious.androidgames.state.games.wordladder

import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.wordladder.WordLadderPuzzle
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
import ru.nsk.kstatemachine.statemachine.createStateMachineBlocking
import ru.nsk.kstatemachine.statemachine.processEventByLaunch
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.transition.onTriggered

enum class WordLadderPhase { SETUP, HOW_TO_PLAY, PLAYING, SOLVED }

private sealed class LadderState : DefaultState() {
    data object Setup : LadderState()
    data object HowToPlay : LadderState()
    data object Playing : LadderState()
    data object Solved : LadderState()
}

private sealed interface LadderEvent : Event {
    data object StartGame : LadderEvent
    data object ShowHowToPlay : LadderEvent
    data object BackToSetup : LadderEvent
    data object TargetReached : LadderEvent
    data object NewPuzzle : LadderEvent
    data object Exit : LadderEvent
}

class WordLadderStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(WordLadderPhase.SETUP)
    val phase: StateFlow<WordLadderPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(LadderState.Setup) {
            transition<LadderEvent.StartGame> {
                targetState = LadderState.Playing
                onTriggered { _phase.value = WordLadderPhase.PLAYING }
            }
            transition<LadderEvent.ShowHowToPlay> {
                targetState = LadderState.HowToPlay
                onTriggered { _phase.value = WordLadderPhase.HOW_TO_PLAY }
            }
        }
        addState(LadderState.HowToPlay) {
            transition<LadderEvent.BackToSetup> {
                targetState = LadderState.Setup
                onTriggered { _phase.value = WordLadderPhase.SETUP }
            }
        }
        addState(LadderState.Playing) {
            transition<LadderEvent.TargetReached> {
                targetState = LadderState.Solved
                onTriggered { _phase.value = WordLadderPhase.SOLVED }
            }
            transition<LadderEvent.Exit> {
                targetState = LadderState.Setup
                onTriggered { _phase.value = WordLadderPhase.SETUP }
            }
        }
        addState(LadderState.Solved) {
            transition<LadderEvent.NewPuzzle> {
                targetState = LadderState.Playing
                onTriggered { _phase.value = WordLadderPhase.PLAYING }
            }
            transition<LadderEvent.Exit> {
                targetState = LadderState.Setup
                onTriggered { _phase.value = WordLadderPhase.SETUP }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(LadderEvent.StartGame)
    fun showHowToPlay() = machine.processEventByLaunch(LadderEvent.ShowHowToPlay)
    fun backToSetup() = machine.processEventByLaunch(LadderEvent.BackToSetup)
    fun targetReached() = machine.processEventByLaunch(LadderEvent.TargetReached)
    fun newPuzzle() = machine.processEventByLaunch(LadderEvent.NewPuzzle)
    fun exit() = machine.processEventByLaunch(LadderEvent.Exit)
}
