package com.xanticious.androidgames.state.games.anagramsarcade

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

enum class AnagramsArcadePhase { IDLE, PLAYING, ROUND_OVER }

private sealed class GameState : DefaultState() {
    data object Idle : GameState()
    data object Playing : GameState()
    data object RoundOver : GameState()
}

sealed interface AnagramsArcadeEvent : Event {
    data object RoundStarted : AnagramsArcadeEvent
    data object LetterTapped : AnagramsArcadeEvent
    data object EntryBackspaced : AnagramsArcadeEvent
    data object WordSubmitted : AnagramsArcadeEvent
    data object TimeExpired : AnagramsArcadeEvent
    data object AllWordsFound : AnagramsArcadeEvent
    data object GaveUp : AnagramsArcadeEvent
    data object NewRound : AnagramsArcadeEvent
}

class AnagramsArcadeStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(AnagramsArcadePhase.IDLE)
    val phase: StateFlow<AnagramsArcadePhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(GameState.Idle) {
            transition<AnagramsArcadeEvent.RoundStarted> {
                targetState = GameState.Playing
                onTriggered { _phase.value = AnagramsArcadePhase.PLAYING }
            }
        }
        addState(GameState.Playing) {
            transition<AnagramsArcadeEvent.LetterTapped> {
                targetState = GameState.Playing
            }
            transition<AnagramsArcadeEvent.EntryBackspaced> {
                targetState = GameState.Playing
            }
            transition<AnagramsArcadeEvent.WordSubmitted> {
                targetState = GameState.Playing
            }
            transition<AnagramsArcadeEvent.TimeExpired> {
                targetState = GameState.RoundOver
                onTriggered { _phase.value = AnagramsArcadePhase.ROUND_OVER }
            }
            transition<AnagramsArcadeEvent.AllWordsFound> {
                targetState = GameState.RoundOver
                onTriggered { _phase.value = AnagramsArcadePhase.ROUND_OVER }
            }
            transition<AnagramsArcadeEvent.GaveUp> {
                targetState = GameState.RoundOver
                onTriggered { _phase.value = AnagramsArcadePhase.ROUND_OVER }
            }
        }
        addState(GameState.RoundOver) {
            transition<AnagramsArcadeEvent.NewRound> {
                targetState = GameState.Idle
                onTriggered { _phase.value = AnagramsArcadePhase.IDLE }
            }
        }
    }

    fun roundStarted() = machine.processEventByLaunch(AnagramsArcadeEvent.RoundStarted)
    fun letterTapped() = machine.processEventByLaunch(AnagramsArcadeEvent.LetterTapped)
    fun entryBackspaced() = machine.processEventByLaunch(AnagramsArcadeEvent.EntryBackspaced)
    fun wordSubmitted() = machine.processEventByLaunch(AnagramsArcadeEvent.WordSubmitted)
    fun timeExpired() = machine.processEventByLaunch(AnagramsArcadeEvent.TimeExpired)
    fun allWordsFound() = machine.processEventByLaunch(AnagramsArcadeEvent.AllWordsFound)
    fun gaveUp() = machine.processEventByLaunch(AnagramsArcadeEvent.GaveUp)
    fun newRound() = machine.processEventByLaunch(AnagramsArcadeEvent.NewRound)
}
