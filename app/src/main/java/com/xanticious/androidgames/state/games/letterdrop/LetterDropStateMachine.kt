package com.xanticious.androidgames.state.games.letterdrop

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

enum class LetterDropPhase { SETUP, HOW_TO_PLAY, PLAYING, GAME_OVER }

private sealed class GameState : DefaultState() {
    data object Setup : GameState()
    data object HowToPlay : GameState()
    data object Playing : GameState()
    data object GameOver : GameState()
}

private sealed interface GameEvent : Event {
    data object StartSetup : GameEvent
    data object ShowHowToPlay : GameEvent
    data object StartPlaying : GameEvent
    data object Overflow : GameEvent
    data object Restart : GameEvent
    data object BackToSetup : GameEvent
}

class LetterDropStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(LetterDropPhase.SETUP)
    val phase: StateFlow<LetterDropPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(GameState.Setup) {
            transition<GameEvent.ShowHowToPlay> {
                targetState = GameState.HowToPlay
                onTriggered { _phase.value = LetterDropPhase.HOW_TO_PLAY }
            }
            transition<GameEvent.StartPlaying> {
                targetState = GameState.Playing
                onTriggered { _phase.value = LetterDropPhase.PLAYING }
            }
        }
        addState(GameState.HowToPlay) {
            transition<GameEvent.BackToSetup> {
                targetState = GameState.Setup
                onTriggered { _phase.value = LetterDropPhase.SETUP }
            }
        }
        addState(GameState.Playing) {
            transition<GameEvent.Overflow> {
                targetState = GameState.GameOver
                onTriggered { _phase.value = LetterDropPhase.GAME_OVER }
            }
        }
        addState(GameState.GameOver) {
            transition<GameEvent.Restart> {
                targetState = GameState.Playing
                onTriggered { _phase.value = LetterDropPhase.PLAYING }
            }
            transition<GameEvent.BackToSetup> {
                targetState = GameState.Setup
                onTriggered { _phase.value = LetterDropPhase.SETUP }
            }
        }
    }

    fun startSetup() = machine.processEventByLaunch(GameEvent.StartSetup)
    fun showHowToPlay() = machine.processEventByLaunch(GameEvent.ShowHowToPlay)
    fun startPlaying() = machine.processEventByLaunch(GameEvent.StartPlaying)
    fun overflow() = machine.processEventByLaunch(GameEvent.Overflow)
    fun restart() = machine.processEventByLaunch(GameEvent.Restart)
    fun backToSetup() = machine.processEventByLaunch(GameEvent.BackToSetup)
}
