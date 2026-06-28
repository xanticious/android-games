package com.xanticious.androidgames.state.games.morsecode

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

/** High-level Morse Code game phases observed by the composable. */
enum class MorseCodePhase {
    IDLE, SETUP, HOW_TO_PLAY, KEYING, PENALTY, STATS
}

private sealed class MorseNavState : DefaultState() {
    data object Idle       : MorseNavState()
    data object Setup      : MorseNavState()
    data object HowToPlay  : MorseNavState()
    data object Keying     : MorseNavState()
    data object Penalty    : MorseNavState()
    data object Stats      : MorseNavState()
}

/** All events that drive the Morse Code state machine. */
sealed interface MorseCodeEvent : Event {
    /** Settings screen is entered for the first time. */
    data object GameStarted      : MorseCodeEvent
    /** Player confirms settings and moves to How to Play. */
    data object ConfigConfirmed  : MorseCodeEvent
    /** Navigate to How to Play from Settings. */
    data object OpenHowToPlay    : MorseCodeEvent
    /** Return to Settings from How to Play. */
    data object BackToSetup      : MorseCodeEvent
    /** Phrase has been selected; begin keying. */
    data object PhraseLoaded     : MorseCodeEvent
    /** A correct letter was committed; remain in KEYING. */
    data object LetterCommitted  : MorseCodeEvent
    /** Current word was completed; advance to next word (remain KEYING). */
    data object WordCompleted    : MorseCodeEvent
    /** A decoded letter did not match; enter penalty delay. */
    data object MistakeMade      : MorseCodeEvent
    /** Penalty delay finished; resume keying. */
    data object PenaltyElapsed   : MorseCodeEvent
    /** All words in the phrase completed; show stats. */
    data object PhraseCompleted  : MorseCodeEvent
    /** Re-train the hardest words from stats; restart keying. */
    data object RetrainHardest   : MorseCodeEvent
    /** Return to idle from stats (menu / exit flow). */
    data object ReturnToIdle     : MorseCodeEvent
}

/**
 * Drives the Morse Code game's high-level phase transitions.
 * Game logic lives in the controller layer; this machine only tracks phase.
 *
 * [scope] is injectable so the machine can be exercised in plain JVM tests
 * using [kotlinx.coroutines.Dispatchers.Unconfined].
 */
class MorseCodeStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(MorseCodePhase.IDLE)
    val phase: StateFlow<MorseCodePhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(MorseNavState.Idle) {
            transition<MorseCodeEvent.GameStarted> {
                targetState = MorseNavState.Setup
                onTriggered { _phase.value = MorseCodePhase.SETUP }
            }
        }
        addState(MorseNavState.Setup) {
            transition<MorseCodeEvent.ConfigConfirmed> {
                targetState = MorseNavState.HowToPlay
                onTriggered { _phase.value = MorseCodePhase.HOW_TO_PLAY }
            }
            transition<MorseCodeEvent.OpenHowToPlay> {
                targetState = MorseNavState.HowToPlay
                onTriggered { _phase.value = MorseCodePhase.HOW_TO_PLAY }
            }
            // Allow jumping straight to keying when retraining (phrase already set)
            transition<MorseCodeEvent.PhraseLoaded> {
                targetState = MorseNavState.Keying
                onTriggered { _phase.value = MorseCodePhase.KEYING }
            }
        }
        addState(MorseNavState.HowToPlay) {
            transition<MorseCodeEvent.BackToSetup> {
                targetState = MorseNavState.Setup
                onTriggered { _phase.value = MorseCodePhase.SETUP }
            }
            transition<MorseCodeEvent.PhraseLoaded> {
                targetState = MorseNavState.Keying
                onTriggered { _phase.value = MorseCodePhase.KEYING }
            }
        }
        addState(MorseNavState.Keying) {
            // Self-transitions: phase stays KEYING; StateFlow won't re-emit same value.
            transition<MorseCodeEvent.LetterCommitted> {
                targetState = MorseNavState.Keying
                onTriggered { _phase.value = MorseCodePhase.KEYING }
            }
            transition<MorseCodeEvent.WordCompleted> {
                targetState = MorseNavState.Keying
                onTriggered { _phase.value = MorseCodePhase.KEYING }
            }
            transition<MorseCodeEvent.MistakeMade> {
                targetState = MorseNavState.Penalty
                onTriggered { _phase.value = MorseCodePhase.PENALTY }
            }
            transition<MorseCodeEvent.PhraseCompleted> {
                targetState = MorseNavState.Stats
                onTriggered { _phase.value = MorseCodePhase.STATS }
            }
        }
        addState(MorseNavState.Penalty) {
            transition<MorseCodeEvent.PenaltyElapsed> {
                targetState = MorseNavState.Keying
                onTriggered { _phase.value = MorseCodePhase.KEYING }
            }
        }
        addState(MorseNavState.Stats) {
            transition<MorseCodeEvent.RetrainHardest> {
                targetState = MorseNavState.Keying
                onTriggered { _phase.value = MorseCodePhase.KEYING }
            }
            transition<MorseCodeEvent.ReturnToIdle> {
                targetState = MorseNavState.Idle
                onTriggered { _phase.value = MorseCodePhase.IDLE }
            }
        }
    }

    fun startGame()       = machine.processEventByLaunch(MorseCodeEvent.GameStarted)
    fun confirmConfig()   = machine.processEventByLaunch(MorseCodeEvent.ConfigConfirmed)
    fun openHowToPlay()   = machine.processEventByLaunch(MorseCodeEvent.OpenHowToPlay)
    fun backToSetup()     = machine.processEventByLaunch(MorseCodeEvent.BackToSetup)
    fun phraseLoaded()    = machine.processEventByLaunch(MorseCodeEvent.PhraseLoaded)
    fun letterCommitted() = machine.processEventByLaunch(MorseCodeEvent.LetterCommitted)
    fun wordCompleted()   = machine.processEventByLaunch(MorseCodeEvent.WordCompleted)
    fun mistakeMade()     = machine.processEventByLaunch(MorseCodeEvent.MistakeMade)
    fun penaltyElapsed()  = machine.processEventByLaunch(MorseCodeEvent.PenaltyElapsed)
    fun phraseCompleted() = machine.processEventByLaunch(MorseCodeEvent.PhraseCompleted)
    fun retrainHardest()  = machine.processEventByLaunch(MorseCodeEvent.RetrainHardest)
    fun returnToIdle()    = machine.processEventByLaunch(MorseCodeEvent.ReturnToIdle)
}
