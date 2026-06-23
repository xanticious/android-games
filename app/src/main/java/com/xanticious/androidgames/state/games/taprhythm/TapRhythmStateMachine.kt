package com.xanticious.androidgames.state.games.taprhythm

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

/** High-level Tap Rhythm game phases observed by the composable. */
enum class TapRhythmPhase {
    IDLE,
    SETTINGS,
    HOW_TO_PLAY,
    COUNT_IN,
    PLAYING,
    RESULTS
}

private sealed class TapRhythmNavState : DefaultState() {
    data object Idle       : TapRhythmNavState()
    data object Settings   : TapRhythmNavState()
    data object HowToPlay  : TapRhythmNavState()
    data object CountIn    : TapRhythmNavState()
    data object Playing    : TapRhythmNavState()
    data object Results    : TapRhythmNavState()
}

private sealed interface TapRhythmEvent : Event {
    /** User opens the game from the lobby. */
    data object GameOpened     : TapRhythmEvent
    /** User confirms settings and wants to start playing. */
    data object ConfigConfirmed : TapRhythmEvent
    /** User taps "How to Play" on the settings screen. */
    data object OpenHowToPlay  : TapRhythmEvent
    /** User taps "Back" on the How to Play screen. */
    data object BackToSettings : TapRhythmEvent
    /** The count-in animation finishes. */
    data object CountInFinished : TapRhythmEvent
    /** Health bar reached zero — death. */
    data object HealthDepleted : TapRhythmEvent
    /** User replays the same seed from the results screen. */
    data object Replay         : TapRhythmEvent
    /** User starts a fresh run (possibly new seed) from the results screen. */
    data object NewRun         : TapRhythmEvent
    /** User returns to the lobby from the results screen. */
    data object Menu           : TapRhythmEvent
}

/**
 * Drives Tap Rhythm's high-level phase transitions.
 *
 * All beam judging, health tracking, and score accumulation live in the
 * controller layer; this machine only tracks which screen is active.
 *
 * [scope] is injectable so the machine can be exercised in plain JVM unit tests
 * without the Android main dispatcher.
 *
 * Transitions:
 * ```
 * Idle → Settings → CountIn → Playing → Results
 *                ↘ HowToPlay ↗
 * Results → CountIn   (Replay — same seed)
 * Results → Settings  (NewRun — pick seed/diff)
 * Results → Idle      (Menu — back to lobby)
 * ```
 */
class TapRhythmStateMachine(
    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(TapRhythmPhase.IDLE)
    val phase: StateFlow<TapRhythmPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {

        addInitialState(TapRhythmNavState.Idle) {
            transition<TapRhythmEvent.GameOpened> {
                targetState = TapRhythmNavState.Settings
                onTriggered { _phase.value = TapRhythmPhase.SETTINGS }
            }
        }

        addState(TapRhythmNavState.Settings) {
            transition<TapRhythmEvent.ConfigConfirmed> {
                targetState = TapRhythmNavState.CountIn
                onTriggered { _phase.value = TapRhythmPhase.COUNT_IN }
            }
            transition<TapRhythmEvent.OpenHowToPlay> {
                targetState = TapRhythmNavState.HowToPlay
                onTriggered { _phase.value = TapRhythmPhase.HOW_TO_PLAY }
            }
        }

        addState(TapRhythmNavState.HowToPlay) {
            transition<TapRhythmEvent.BackToSettings> {
                targetState = TapRhythmNavState.Settings
                onTriggered { _phase.value = TapRhythmPhase.SETTINGS }
            }
        }

        addState(TapRhythmNavState.CountIn) {
            transition<TapRhythmEvent.CountInFinished> {
                targetState = TapRhythmNavState.Playing
                onTriggered { _phase.value = TapRhythmPhase.PLAYING }
            }
        }

        addState(TapRhythmNavState.Playing) {
            transition<TapRhythmEvent.HealthDepleted> {
                targetState = TapRhythmNavState.Results
                onTriggered { _phase.value = TapRhythmPhase.RESULTS }
            }
        }

        addState(TapRhythmNavState.Results) {
            transition<TapRhythmEvent.Replay> {
                targetState = TapRhythmNavState.CountIn
                onTriggered { _phase.value = TapRhythmPhase.COUNT_IN }
            }
            transition<TapRhythmEvent.NewRun> {
                targetState = TapRhythmNavState.Settings
                onTriggered { _phase.value = TapRhythmPhase.SETTINGS }
            }
            transition<TapRhythmEvent.Menu> {
                targetState = TapRhythmNavState.Idle
                onTriggered { _phase.value = TapRhythmPhase.IDLE }
            }
        }
    }

    // ── Public event dispatchers ──────────────────────────────────────────────

    fun openGame()         = machine.processEventByLaunch(TapRhythmEvent.GameOpened)
    fun confirmConfig()    = machine.processEventByLaunch(TapRhythmEvent.ConfigConfirmed)
    fun openHowToPlay()    = machine.processEventByLaunch(TapRhythmEvent.OpenHowToPlay)
    fun backToSettings()   = machine.processEventByLaunch(TapRhythmEvent.BackToSettings)
    fun countInFinished()  = machine.processEventByLaunch(TapRhythmEvent.CountInFinished)
    fun healthDepleted()   = machine.processEventByLaunch(TapRhythmEvent.HealthDepleted)
    fun replay()           = machine.processEventByLaunch(TapRhythmEvent.Replay)
    fun newRun()           = machine.processEventByLaunch(TapRhythmEvent.NewRun)
    fun menu()             = machine.processEventByLaunch(TapRhythmEvent.Menu)
}
