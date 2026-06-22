package com.xanticious.androidgames.state.games.wordslices

import com.xanticious.androidgames.controller.games.wordslices.WordSlicesController
import com.xanticious.androidgames.controller.words.WordData
import com.xanticious.androidgames.model.games.wordslices.WordSlicesRoundState
import com.xanticious.androidgames.model.games.wordslices.WordSlicesSettings
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

enum class WordSlicesPhase { SETUP, HOW_TO_PLAY, PLAYING, WON, LOST }

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
    data class LetterGuessed(val letter: Char) : RoundEvent
    data object WordFullyRevealed : RoundEvent
    data object CakeGone : RoundEvent
    data object NewWord : RoundEvent
    data object BackToSetup : RoundEvent
}

class WordSlicesStateMachine(
    private val wordData: WordData,
    private val random: Random = Random.Default,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val controller = WordSlicesController()
    
    private val _phase = MutableStateFlow(WordSlicesPhase.SETUP)
    val phase: StateFlow<WordSlicesPhase> = _phase.asStateFlow()
    
    private val _settings = MutableStateFlow(WordSlicesSettings())
    val settings: StateFlow<WordSlicesSettings> = _settings.asStateFlow()
    
    private val _roundState = MutableStateFlow<WordSlicesRoundState?>(null)
    val roundState: StateFlow<WordSlicesRoundState?> = _roundState.asStateFlow()
    
    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(RoundState.Setup) {
            transition<RoundEvent.ShowHowToPlay> {
                targetState = RoundState.HowToPlay
                onTriggered { _phase.value = WordSlicesPhase.HOW_TO_PLAY }
            }
            transition<RoundEvent.StartPlaying> {
                targetState = RoundState.Playing
                onTriggered {
                    startNewRound()
                    _phase.value = WordSlicesPhase.PLAYING
                }
            }
        }
        
        addState(RoundState.HowToPlay) {
            transition<RoundEvent.BackToSetup> {
                targetState = RoundState.Setup
                onTriggered { _phase.value = WordSlicesPhase.SETUP }
            }
            transition<RoundEvent.StartPlaying> {
                targetState = RoundState.Playing
                onTriggered {
                    startNewRound()
                    _phase.value = WordSlicesPhase.PLAYING
                }
            }
        }
        
        addState(RoundState.Playing) {
            transition<RoundEvent.LetterGuessed> { }
            transition<RoundEvent.WordFullyRevealed> {
                targetState = RoundState.Won
                onTriggered {
                    markWon()
                    _phase.value = WordSlicesPhase.WON
                }
            }
            transition<RoundEvent.CakeGone> {
                targetState = RoundState.Lost
                onTriggered {
                    markLost()
                    _phase.value = WordSlicesPhase.LOST
                }
            }
        }
        
        addState(RoundState.Won) {
            transition<RoundEvent.NewWord> {
                targetState = RoundState.Playing
                onTriggered {
                    startNewRound()
                    _phase.value = WordSlicesPhase.PLAYING
                }
            }
            transition<RoundEvent.BackToSetup> {
                targetState = RoundState.Setup
                onTriggered { _phase.value = WordSlicesPhase.SETUP }
            }
        }
        
        addState(RoundState.Lost) {
            transition<RoundEvent.NewWord> {
                targetState = RoundState.Playing
                onTriggered {
                    startNewRound()
                    _phase.value = WordSlicesPhase.PLAYING
                }
            }
            transition<RoundEvent.BackToSetup> {
                targetState = RoundState.Setup
                onTriggered { _phase.value = WordSlicesPhase.SETUP }
            }
        }
    }
    
    fun updateSettings(settings: WordSlicesSettings) {
        _settings.value = settings
    }
    
    fun showHowToPlay() = machine.processEventByLaunch(RoundEvent.ShowHowToPlay)
    fun startPlaying() = machine.processEventByLaunch(RoundEvent.StartPlaying)
    fun backToSetup() = machine.processEventByLaunch(RoundEvent.BackToSetup)
    fun newWord() = machine.processEventByLaunch(RoundEvent.NewWord)
    
    fun guessLetter(letter: Char) {
        val current = _roundState.value ?: return
        if (current.won || current.lost) return
        
        val result = controller.processGuess(
            letter,
            current.word,
            current.guessedLetters,
            current.wrongGuesses,
            current.slicesRemaining
        )
        
        if (result.alreadyGuessed) return
        
        _roundState.value = current.copy(
            guessedLetters = result.newGuessedLetters,
            wrongGuesses = result.newWrongGuesses,
            slicesRemaining = result.newSlicesRemaining,
            revealedLetters = current.word.filter { 
                it.lowercaseChar() in result.newGuessedLetters.map { g -> g.lowercaseChar() }
            }.toSet()
        )
        
        machine.processEventByLaunch(RoundEvent.LetterGuessed(letter))
        
        val newState = _roundState.value!!
        when {
            controller.isGameWon(newState.word, newState.guessedLetters, newState.slicesRemaining) -> 
                machine.processEventByLaunch(RoundEvent.WordFullyRevealed)
            controller.isGameLost(newState.slicesRemaining) -> 
                machine.processEventByLaunch(RoundEvent.CakeGone)
        }
    }
    
    private fun startNewRound() {
        val word = controller.selectWord(wordData, _settings.value.difficulty, random)
        val definition = if (_settings.value.revealDefinition) {
            wordData.definitionOf(word) ?: WordData.DEFINITION_UNAVAILABLE
        } else null
        
        _roundState.value = WordSlicesRoundState(
            word = word,
            revealedLetters = emptySet(),
            guessedLetters = emptySet(),
            wrongGuesses = emptySet(),
            slicesRemaining = 12,
            definition = definition
        )
    }
    
    private fun markWon() {
        _roundState.value = _roundState.value?.copy(won = true)
    }
    
    private fun markLost() {
        _roundState.value = _roundState.value?.copy(lost = true)
    }
}
