package com.xanticious.androidgames.state.games.wordle

import com.xanticious.androidgames.controller.games.wordle.WordleController
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.games.wordle.GuessResult
import com.xanticious.androidgames.model.games.wordle.WordleRoundState
import com.xanticious.androidgames.model.games.wordle.WordleSettings
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
import kotlin.random.Random

enum class WordlePhase { SETUP, HOW_TO_PLAY, PLAYING, WON, LOST }

private sealed class RoundState : DefaultState() {
    data object Setup : RoundState()
    data object HowToPlay : RoundState()
    data object Playing : RoundState()
    data object Won : RoundState()
    data object Lost : RoundState()
}

private sealed interface RoundEvent : Event {
    data object ShowHowToPlay : RoundEvent
    data object StartPlaying : RoundEvent
    data class GuessSubmitted(val word: String) : RoundEvent
    data object HelpRequested : RoundEvent
    data object TargetGuessed : RoundEvent
    data object GuessesExhausted : RoundEvent
    data object NextRound : RoundEvent
    data object BackToSetup : RoundEvent
}

class WordleStateMachine(
    private val wordData: WordData,
    private val random: Random = Random.Default,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val controller = WordleController()
    
    private val _phase = MutableStateFlow(WordlePhase.SETUP)
    val phase: StateFlow<WordlePhase> = _phase.asStateFlow()
    
    private val _settings = MutableStateFlow(WordleSettings())
    val settings: StateFlow<WordleSettings> = _settings.asStateFlow()
    
    private val _roundState = MutableStateFlow<WordleRoundState?>(null)
    val roundState: StateFlow<WordleRoundState?> = _roundState.asStateFlow()
    
    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(RoundState.Setup) {
            transition<RoundEvent.ShowHowToPlay> {
                targetState = RoundState.HowToPlay
                onTriggered { _phase.value = WordlePhase.HOW_TO_PLAY }
            }
            transition<RoundEvent.StartPlaying> {
                targetState = RoundState.Playing
                onTriggered {
                    startNewRound()
                    _phase.value = WordlePhase.PLAYING
                }
            }
        }
        
        addState(RoundState.HowToPlay) {
            transition<RoundEvent.BackToSetup> {
                targetState = RoundState.Setup
                onTriggered { _phase.value = WordlePhase.SETUP }
            }
            transition<RoundEvent.StartPlaying> {
                targetState = RoundState.Playing
                onTriggered {
                    startNewRound()
                    _phase.value = WordlePhase.PLAYING
                }
            }
        }
        
        addState(RoundState.Playing) {
            transition<RoundEvent.GuessSubmitted> { }
            transition<RoundEvent.HelpRequested> { }
            transition<RoundEvent.TargetGuessed> {
                targetState = RoundState.Won
                onTriggered {
                    markWon()
                    _phase.value = WordlePhase.WON
                }
            }
            transition<RoundEvent.GuessesExhausted> {
                targetState = RoundState.Lost
                onTriggered {
                    markLost()
                    _phase.value = WordlePhase.LOST
                }
            }
        }
        
        addState(RoundState.Won) {
            transition<RoundEvent.NextRound> {
                targetState = RoundState.Playing
                onTriggered {
                    startNewRound()
                    _phase.value = WordlePhase.PLAYING
                }
            }
            transition<RoundEvent.BackToSetup> {
                targetState = RoundState.Setup
                onTriggered { _phase.value = WordlePhase.SETUP }
            }
        }
        
        addState(RoundState.Lost) {
            transition<RoundEvent.NextRound> {
                targetState = RoundState.Playing
                onTriggered {
                    startNewRound()
                    _phase.value = WordlePhase.PLAYING
                }
            }
            transition<RoundEvent.BackToSetup> {
                targetState = RoundState.Setup
                onTriggered { _phase.value = WordlePhase.SETUP }
            }
        }
    }
    
    fun updateSettings(settings: WordleSettings) {
        _settings.value = settings
    }
    
    fun showHowToPlay() = machine.processEventByLaunch(RoundEvent.ShowHowToPlay)
    fun startPlaying() = machine.processEventByLaunch(RoundEvent.StartPlaying)
    fun backToSetup() = machine.processEventByLaunch(RoundEvent.BackToSetup)
    fun nextRound() = machine.processEventByLaunch(RoundEvent.NextRound)
    
    fun submitGuess(word: String) {
        val current = _roundState.value ?: return
        if (current.won || current.lost) return
        
        val constraints = controller.deriveConstraints(current.guesses)
        if (!controller.validateGuess(word, wordData, _settings.value, constraints)) {
            return
        }
        
        val hints = controller.computeHints(word, current.targetWord)
        val result = GuessResult(word, hints)
        val newGuesses = current.guesses + result
        
        val isCorrect = word == current.targetWord
        val exhausted = newGuesses.size >= _settings.value.maxGuesses
        
        _roundState.value = current.copy(
            guesses = newGuesses,
            currentInput = "",
            isFirstGuess = false
        )
        
        machine.processEventByLaunch(RoundEvent.GuessSubmitted(word))
        
        when {
            isCorrect -> machine.processEventByLaunch(RoundEvent.TargetGuessed)
            exhausted -> machine.processEventByLaunch(RoundEvent.GuessesExhausted)
        }
    }
    
    fun requestHelp() {
        val current = _roundState.value ?: return
        if (current.won || current.lost) return
        
        val constraints = controller.deriveConstraints(current.guesses)
        val helpWord = controller.findValidGuess(
            wordData, 
            constraints, 
            _settings.value.wordLength, 
            random
        ) ?: return
        
        submitGuess(helpWord)
        machine.processEventByLaunch(RoundEvent.HelpRequested)
    }
    
    private fun startNewRound() {
        val previousTarget = _roundState.value?.targetWord
        val target = controller.selectTarget(wordData, _settings.value, random)
        val firstGuess = controller.getFirstGuess(previousTarget, _settings.value, wordData, random)
        
        _roundState.value = WordleRoundState(
            targetWord = target,
            guesses = emptyList(),
            currentInput = firstGuess,
            previousTarget = previousTarget,
            isFirstGuess = true
        )
    }
    
    private fun markWon() {
        _roundState.value = _roundState.value?.copy(won = true)
    }
    
    private fun markLost() {
        _roundState.value = _roundState.value?.copy(lost = true)
    }
}
