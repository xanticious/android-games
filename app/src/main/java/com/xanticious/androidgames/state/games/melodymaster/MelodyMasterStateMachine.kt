package com.xanticious.androidgames.state.games.melodymaster

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

/** High-level Melody Master game phases observed by the composable. */
enum class MelodyMasterPhase {
    IDLE,
    SETUP,
    HOW_TO_PLAY,
    COUNT_IN,
    PLAYING,
    RESULTS
}

private sealed class MelodyNavState : DefaultState() {
    data object Idle : MelodyNavState()
    data object Setup : MelodyNavState()
    data object HowToPlay : MelodyNavState()
    data object CountIn : MelodyNavState()
    data object Playing : MelodyNavState()
    data object Results : MelodyNavState()
}

private sealed interface MelodyMasterEvent : Event {
    data object GameStarted : MelodyMasterEvent
    data object ConfigConfirmed : MelodyMasterEvent
    data object OpenHowToPlay : MelodyMasterEvent
    data object BackToSetup : MelodyMasterEvent
    data object CountInFinished : MelodyMasterEvent
    data object TrackFinished : MelodyMasterEvent
    data object Replay : MelodyMasterEvent
    data object NewTrack : MelodyMasterEvent
    data object Menu : MelodyMasterEvent
}

/**
 * Drives Melody Master's high-level phase transitions per the shared state-machine
 * shape in `design/common/rhythm-note-highway.md`:
 *
 *   Idle → Setup → (HowToPlay ↔ Setup) → CountIn → Playing → Results
 *   Results → CountIn  (Replay, same seed)
 *   Results → Setup    (NewTrack, new seed)
 *   Results → Idle     (Menu)
 *
 * Track generation happens in the composable when entering [MelodyMasterPhase.COUNT_IN].
 * [scope] is injectable so the machine can be exercised in plain JVM unit tests
 * without the Android main dispatcher.
 */
class MelodyMasterStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(MelodyMasterPhase.IDLE)
    val phase: StateFlow<MelodyMasterPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(MelodyNavState.Idle) {
            transition<MelodyMasterEvent.GameStarted> {
                targetState = MelodyNavState.Setup
                onTriggered { _phase.value = MelodyMasterPhase.SETUP }
            }
        }
        addState(MelodyNavState.Setup) {
            transition<MelodyMasterEvent.OpenHowToPlay> {
                targetState = MelodyNavState.HowToPlay
                onTriggered { _phase.value = MelodyMasterPhase.HOW_TO_PLAY }
            }
            transition<MelodyMasterEvent.ConfigConfirmed> {
                targetState = MelodyNavState.CountIn
                onTriggered { _phase.value = MelodyMasterPhase.COUNT_IN }
            }
        }
        addState(MelodyNavState.HowToPlay) {
            transition<MelodyMasterEvent.BackToSetup> {
                targetState = MelodyNavState.Setup
                onTriggered { _phase.value = MelodyMasterPhase.SETUP }
            }
        }
        addState(MelodyNavState.CountIn) {
            transition<MelodyMasterEvent.CountInFinished> {
                targetState = MelodyNavState.Playing
                onTriggered { _phase.value = MelodyMasterPhase.PLAYING }
            }
        }
        addState(MelodyNavState.Playing) {
            transition<MelodyMasterEvent.TrackFinished> {
                targetState = MelodyNavState.Results
                onTriggered { _phase.value = MelodyMasterPhase.RESULTS }
            }
        }
        addState(MelodyNavState.Results) {
            transition<MelodyMasterEvent.Replay> {
                targetState = MelodyNavState.CountIn
                onTriggered { _phase.value = MelodyMasterPhase.COUNT_IN }
            }
            transition<MelodyMasterEvent.NewTrack> {
                targetState = MelodyNavState.Setup
                onTriggered { _phase.value = MelodyMasterPhase.SETUP }
            }
            transition<MelodyMasterEvent.Menu> {
                targetState = MelodyNavState.Idle
                onTriggered { _phase.value = MelodyMasterPhase.IDLE }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(MelodyMasterEvent.GameStarted)
    fun confirmConfig() = machine.processEventByLaunch(MelodyMasterEvent.ConfigConfirmed)
    fun openHowToPlay() = machine.processEventByLaunch(MelodyMasterEvent.OpenHowToPlay)
    fun backToSetup() = machine.processEventByLaunch(MelodyMasterEvent.BackToSetup)
    fun countInFinished() = machine.processEventByLaunch(MelodyMasterEvent.CountInFinished)
    fun trackFinished() = machine.processEventByLaunch(MelodyMasterEvent.TrackFinished)
    fun replay() = machine.processEventByLaunch(MelodyMasterEvent.Replay)
    fun newTrack() = machine.processEventByLaunch(MelodyMasterEvent.NewTrack)
    fun menu() = machine.processEventByLaunch(MelodyMasterEvent.Menu)
}
