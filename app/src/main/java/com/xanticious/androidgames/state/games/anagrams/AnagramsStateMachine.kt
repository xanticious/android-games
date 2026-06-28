package com.xanticious.androidgames.state.games.anagrams

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

enum class AnagramsPhase { IDLE, PLAYING, ROUND_OVER }

private sealed class GameState : DefaultState() {
    data object Idle : GameState()
    data object Playing : GameState()
    data object RoundOver : GameState()
}

sealed interface AnagramsEvent : Event {
    data object RoundStarted : AnagramsEvent
    data object LetterTapped : AnagramsEvent
    data object EntryBackspaced : AnagramsEvent
    data object WordSubmitted : AnagramsEvent
    data object AllWordsFound : AnagramsEvent
    data object GaveUp : AnagramsEvent
    data object NewRound : AnagramsEvent
}

class AnagramsStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(AnagramsPhase.IDLE)
    val phase: StateFlow<AnagramsPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(GameState.Idle) {
            transition<AnagramsEvent.RoundStarted> {
                targetState = GameState.Playing
                onTriggered { _phase.value = AnagramsPhase.PLAYING }
            }
        }
        addState(GameState.Playing) {
            transition<AnagramsEvent.LetterTapped> {
                targetState = GameState.Playing
            }
            transition<AnagramsEvent.EntryBackspaced> {
                targetState = GameState.Playing
            }
            transition<AnagramsEvent.WordSubmitted> {
                targetState = GameState.Playing
            }
            transition<AnagramsEvent.AllWordsFound> {
                targetState = GameState.RoundOver
                onTriggered { _phase.value = AnagramsPhase.ROUND_OVER }
            }
            transition<AnagramsEvent.GaveUp> {
                targetState = GameState.RoundOver
                onTriggered { _phase.value = AnagramsPhase.ROUND_OVER }
            }
        }
        addState(GameState.RoundOver) {
            transition<AnagramsEvent.NewRound> {
                targetState = GameState.Idle
                onTriggered { _phase.value = AnagramsPhase.IDLE }
            }
        }
    }

    fun roundStarted() = machine.processEventByLaunch(AnagramsEvent.RoundStarted)
    fun letterTapped() = machine.processEventByLaunch(AnagramsEvent.LetterTapped)
    fun entryBackspaced() = machine.processEventByLaunch(AnagramsEvent.EntryBackspaced)
    fun wordSubmitted() = machine.processEventByLaunch(AnagramsEvent.WordSubmitted)
    fun allWordsFound() = machine.processEventByLaunch(AnagramsEvent.AllWordsFound)
    fun gaveUp() = machine.processEventByLaunch(AnagramsEvent.GaveUp)
    fun newRound() = machine.processEventByLaunch(AnagramsEvent.NewRound)
}
