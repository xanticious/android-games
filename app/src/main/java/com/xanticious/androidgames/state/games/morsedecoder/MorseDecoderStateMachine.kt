package com.xanticious.androidgames.state.games.morsedecoder

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

/** High-level Morse Decoder game phases observed by the composable. */
enum class DecoderPhase {
    IDLE,
    SETUP,
    HOW_TO_PLAY,
    LISTENING,
    RESULTS
}

private sealed class DecoderNavState : DefaultState() {
    data object Idle : DecoderNavState()
    data object Setup : DecoderNavState()
    data object HowToPlay : DecoderNavState()
    data object Listening : DecoderNavState()
    data object Results : DecoderNavState()
}

sealed interface DecoderEvent : Event {
    /** Composable initialises — move to Settings. */
    data object GameStarted : DecoderEvent
    /** User opens How to Play from Settings. */
    data object OpenHowToPlay : DecoderEvent
    /** User navigates back to Settings from How to Play. */
    data object BackToSetup : DecoderEvent
    /** Settings confirmed and sentence chosen — begin the first letter. */
    data object SentenceLoaded : DecoderEvent
    /** The current letter's beep schedule finished one pass; loops repeat. */
    data object BeepsFinished : DecoderEvent
    /** Player tapped the correct letter option. */
    data object GuessedCorrect : DecoderEvent
    /** Player tapped a wrong letter option; beeps replay with one less choice. */
    data object GuessedWrong : DecoderEvent
    /** All letters in the sentence have been solved. */
    data object SentenceCompleted : DecoderEvent
    /** Player chose Replay from Results — return to Settings (same seed pre-filled). */
    data object Replay : DecoderEvent
    /** Player chose Menu from Results — return to Idle (the launcher re-opens). */
    data object Menu : DecoderEvent
}

/**
 * Drives Morse Decoder's high-level phase transitions via KStateMachine.
 *
 * Phase is exposed as [StateFlow] so Compose can collect it with zero coupling
 * to this class's internals.  [scope] is injectable so plain JVM unit tests can
 * use [kotlinx.coroutines.Dispatchers.Unconfined] without an Android runtime.
 *
 * [BeepsFinished], [DecoderEvent.GuessedCorrect] and [DecoderEvent.GuessedWrong]
 * are all self-transitions within the Listening state; the composable manages
 * the game data they affect (prompt, outcomes, disabled set, etc.).
 */
class MorseDecoderStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(DecoderPhase.IDLE)
    val phase: StateFlow<DecoderPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(DecoderNavState.Idle) {
            transition<DecoderEvent.GameStarted> {
                targetState = DecoderNavState.Setup
                onTriggered { _phase.value = DecoderPhase.SETUP }
            }
        }
        addState(DecoderNavState.Setup) {
            transition<DecoderEvent.OpenHowToPlay> {
                targetState = DecoderNavState.HowToPlay
                onTriggered { _phase.value = DecoderPhase.HOW_TO_PLAY }
            }
            transition<DecoderEvent.SentenceLoaded> {
                targetState = DecoderNavState.Listening
                onTriggered { _phase.value = DecoderPhase.LISTENING }
            }
        }
        addState(DecoderNavState.HowToPlay) {
            transition<DecoderEvent.BackToSetup> {
                targetState = DecoderNavState.Setup
                onTriggered { _phase.value = DecoderPhase.SETUP }
            }
        }
        addState(DecoderNavState.Listening) {
            // Self-transitions: phase stays LISTENING but the composable updates game data
            transition<DecoderEvent.BeepsFinished> {
                targetState = DecoderNavState.Listening
                onTriggered { _phase.value = DecoderPhase.LISTENING }
            }
            transition<DecoderEvent.GuessedWrong> {
                targetState = DecoderNavState.Listening
                onTriggered { _phase.value = DecoderPhase.LISTENING }
            }
            transition<DecoderEvent.GuessedCorrect> {
                targetState = DecoderNavState.Listening
                onTriggered { _phase.value = DecoderPhase.LISTENING }
            }
            transition<DecoderEvent.SentenceCompleted> {
                targetState = DecoderNavState.Results
                onTriggered { _phase.value = DecoderPhase.RESULTS }
            }
        }
        addState(DecoderNavState.Results) {
            transition<DecoderEvent.Replay> {
                targetState = DecoderNavState.Setup
                onTriggered { _phase.value = DecoderPhase.SETUP }
            }
            transition<DecoderEvent.Menu> {
                targetState = DecoderNavState.Idle
                onTriggered { _phase.value = DecoderPhase.IDLE }
            }
        }
    }

    fun startGame()         = machine.processEventByLaunch(DecoderEvent.GameStarted)
    fun openHowToPlay()     = machine.processEventByLaunch(DecoderEvent.OpenHowToPlay)
    fun backToSetup()       = machine.processEventByLaunch(DecoderEvent.BackToSetup)
    fun sentenceLoaded()    = machine.processEventByLaunch(DecoderEvent.SentenceLoaded)
    fun beepsFinished()     = machine.processEventByLaunch(DecoderEvent.BeepsFinished)
    fun guessedCorrect()    = machine.processEventByLaunch(DecoderEvent.GuessedCorrect)
    fun guessedWrong()      = machine.processEventByLaunch(DecoderEvent.GuessedWrong)
    fun sentenceCompleted() = machine.processEventByLaunch(DecoderEvent.SentenceCompleted)
    fun replay()            = machine.processEventByLaunch(DecoderEvent.Replay)
    fun menu()              = machine.processEventByLaunch(DecoderEvent.Menu)
}
