package com.xanticious.androidgames.state.games.wordsearch

import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.games.wordsearch.WordSearchState
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

enum class WordSearchPhase { SETUP, HOW_TO_PLAY, PLAYING, SOLVED, TIME_UP }

private sealed class GameState : DefaultState() {
    data object Setup : GameState()
    data object HowToPlay : GameState()
    data object Playing : GameState()
    data object Solved : GameState()
    data object TimeUp : GameState()
}

private sealed interface GameEvent : Event {
    data object ShowHowToPlay : GameEvent
    data object StartGame : GameEvent
    data object BackToSetup : GameEvent
    data object AllWordsFound : GameEvent
    data object TimeExpired : GameEvent
    data object NewGame : GameEvent
}

class WordSearchStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(WordSearchPhase.SETUP)
    val phase: StateFlow<WordSearchPhase> = _phase.asStateFlow()

    private val _gameState = MutableStateFlow<WordSearchState?>(null)
    val gameState: StateFlow<WordSearchState?> = _gameState.asStateFlow()

    private val _difficulty = MutableStateFlow(GameDifficulty.MEDIUM)
    val difficulty: StateFlow<GameDifficulty> = _difficulty.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(GameState.Setup) {
            transition<GameEvent.ShowHowToPlay> {
                targetState = GameState.HowToPlay
                onTriggered { _phase.value = WordSearchPhase.HOW_TO_PLAY }
            }
            transition<GameEvent.StartGame> {
                targetState = GameState.Playing
                onTriggered { _phase.value = WordSearchPhase.PLAYING }
            }
        }
        addState(GameState.HowToPlay) {
            transition<GameEvent.BackToSetup> {
                targetState = GameState.Setup
                onTriggered { _phase.value = WordSearchPhase.SETUP }
            }
        }
        addState(GameState.Playing) {
            transition<GameEvent.AllWordsFound> {
                targetState = GameState.Solved
                onTriggered { _phase.value = WordSearchPhase.SOLVED }
            }
            transition<GameEvent.TimeExpired> {
                targetState = GameState.TimeUp
                onTriggered { _phase.value = WordSearchPhase.TIME_UP }
            }
        }
        addState(GameState.Solved) {
            transition<GameEvent.NewGame> {
                targetState = GameState.Setup
                onTriggered {
                    _phase.value = WordSearchPhase.SETUP
                    _gameState.value = null
                }
            }
        }
        addState(GameState.TimeUp) {
            transition<GameEvent.NewGame> {
                targetState = GameState.Setup
                onTriggered {
                    _phase.value = WordSearchPhase.SETUP
                    _gameState.value = null
                }
            }
        }
    }

    fun showHowToPlay() = machine.processEventByLaunch(GameEvent.ShowHowToPlay)
    fun startGame(state: WordSearchState) {
        _gameState.value = state
        machine.processEventByLaunch(GameEvent.StartGame)
    }
    fun backToSetup() = machine.processEventByLaunch(GameEvent.BackToSetup)
    fun allWordsFound() = machine.processEventByLaunch(GameEvent.AllWordsFound)
    fun timeExpired() = machine.processEventByLaunch(GameEvent.TimeExpired)
    fun newGame() = machine.processEventByLaunch(GameEvent.NewGame)

    fun updateGameState(state: WordSearchState) {
        _gameState.value = state
    }

    fun setDifficulty(difficulty: GameDifficulty) {
        _difficulty.value = difficulty
    }
}
