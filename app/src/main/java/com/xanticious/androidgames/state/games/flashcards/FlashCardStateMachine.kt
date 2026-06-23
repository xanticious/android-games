package com.xanticious.androidgames.state.games.flashcards

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

/** High-level Flash Cards session phases observed by the composable. */
enum class FlashCardPhase {
    IDLE,
    PACK_PICKER,
    SETTINGS,
    DRAWING_CARD,
    SHOWING_FRONT,
    SHOWING_BACK,
    RECORDING_RESULT,
    RESULTS
}

private sealed class SessionState : DefaultState() {
    data object Idle : SessionState()
    data object PackPicker : SessionState()
    data object Settings : SessionState()
    data object DrawingCard : SessionState()
    data object ShowingFront : SessionState()
    data object ShowingBack : SessionState()
    data object RecordingResult : SessionState()
    data object Results : SessionState()
}

private sealed interface SessionEvent : Event {
    data object OpenGame : SessionEvent
    data object PackSelected : SessionEvent
    data object Back : SessionEvent
    data object StartSession : SessionEvent
    data object CardReady : SessionEvent
    data object FlipTapped : SessionEvent
    data object SessionEnded : SessionEvent
    data object GotIt : SessionEvent
    data object Oops : SessionEvent
    data object MoreCards : SessionEvent
    data object SessionComplete : SessionEvent
    data object PlayAgain : SessionEvent
    data object ReviewMissed : SessionEvent
    data object BackToSettings : SessionEvent
}

/**
 * Drives Flash Cards screen-level phase transitions.
 *
 * Card drawing, result recording, and progress tracking live in the controller layer; this
 * machine only tracks *which phase* the session is in so the composable can react accordingly.
 *
 * The [scope] is injectable so the machine can be exercised in plain JVM unit tests without the
 * Android main dispatcher.
 *
 * State diagram (condensed):
 * ```
 * Idle ──OpenGame──► PackPicker
 * PackPicker ──PackSelected──► Settings | Back──► Idle
 * Settings ──StartSession──► DrawingCard | Back──► PackPicker
 * DrawingCard ──CardReady──► ShowingFront
 * ShowingFront ──FlipTapped──► ShowingBack | SessionEnded──► Results
 * ShowingBack ──GotIt/Oops──► RecordingResult
 * RecordingResult ──MoreCards──► DrawingCard | SessionComplete──► Results
 * Results ──PlayAgain/ReviewMissed──► DrawingCard | BackToSettings──► Settings
 * ```
 */
class FlashCardStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(FlashCardPhase.IDLE)
    val phase: StateFlow<FlashCardPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(SessionState.Idle) {
            transition<SessionEvent.OpenGame> {
                targetState = SessionState.PackPicker
                onTriggered { _phase.value = FlashCardPhase.PACK_PICKER }
            }
        }
        addState(SessionState.PackPicker) {
            transition<SessionEvent.PackSelected> {
                targetState = SessionState.Settings
                onTriggered { _phase.value = FlashCardPhase.SETTINGS }
            }
            transition<SessionEvent.Back> {
                targetState = SessionState.Idle
                onTriggered { _phase.value = FlashCardPhase.IDLE }
            }
        }
        addState(SessionState.Settings) {
            transition<SessionEvent.StartSession> {
                targetState = SessionState.DrawingCard
                onTriggered { _phase.value = FlashCardPhase.DRAWING_CARD }
            }
            transition<SessionEvent.Back> {
                targetState = SessionState.PackPicker
                onTriggered { _phase.value = FlashCardPhase.PACK_PICKER }
            }
        }
        addState(SessionState.DrawingCard) {
            transition<SessionEvent.CardReady> {
                targetState = SessionState.ShowingFront
                onTriggered { _phase.value = FlashCardPhase.SHOWING_FRONT }
            }
        }
        addState(SessionState.ShowingFront) {
            transition<SessionEvent.FlipTapped> {
                targetState = SessionState.ShowingBack
                onTriggered { _phase.value = FlashCardPhase.SHOWING_BACK }
            }
            transition<SessionEvent.SessionEnded> {
                targetState = SessionState.Results
                onTriggered { _phase.value = FlashCardPhase.RESULTS }
            }
        }
        addState(SessionState.ShowingBack) {
            transition<SessionEvent.GotIt> {
                targetState = SessionState.RecordingResult
                onTriggered { _phase.value = FlashCardPhase.RECORDING_RESULT }
            }
            transition<SessionEvent.Oops> {
                targetState = SessionState.RecordingResult
                onTriggered { _phase.value = FlashCardPhase.RECORDING_RESULT }
            }
        }
        addState(SessionState.RecordingResult) {
            transition<SessionEvent.MoreCards> {
                targetState = SessionState.DrawingCard
                onTriggered { _phase.value = FlashCardPhase.DRAWING_CARD }
            }
            transition<SessionEvent.SessionComplete> {
                targetState = SessionState.Results
                onTriggered { _phase.value = FlashCardPhase.RESULTS }
            }
        }
        addState(SessionState.Results) {
            transition<SessionEvent.PlayAgain> {
                targetState = SessionState.DrawingCard
                onTriggered { _phase.value = FlashCardPhase.DRAWING_CARD }
            }
            transition<SessionEvent.ReviewMissed> {
                targetState = SessionState.DrawingCard
                onTriggered { _phase.value = FlashCardPhase.DRAWING_CARD }
            }
            transition<SessionEvent.BackToSettings> {
                targetState = SessionState.Settings
                onTriggered { _phase.value = FlashCardPhase.SETTINGS }
            }
        }
    }

    fun openGame() = machine.processEventByLaunch(SessionEvent.OpenGame)
    fun packSelected() = machine.processEventByLaunch(SessionEvent.PackSelected)
    fun back() = machine.processEventByLaunch(SessionEvent.Back)
    fun startSession() = machine.processEventByLaunch(SessionEvent.StartSession)
    fun cardReady() = machine.processEventByLaunch(SessionEvent.CardReady)
    fun flipTapped() = machine.processEventByLaunch(SessionEvent.FlipTapped)
    fun sessionEnded() = machine.processEventByLaunch(SessionEvent.SessionEnded)
    fun gotIt() = machine.processEventByLaunch(SessionEvent.GotIt)
    fun oops() = machine.processEventByLaunch(SessionEvent.Oops)
    fun moreCards() = machine.processEventByLaunch(SessionEvent.MoreCards)
    fun sessionComplete() = machine.processEventByLaunch(SessionEvent.SessionComplete)
    fun playAgain() = machine.processEventByLaunch(SessionEvent.PlayAgain)
    fun reviewMissed() = machine.processEventByLaunch(SessionEvent.ReviewMissed)
    fun backToSettings() = machine.processEventByLaunch(SessionEvent.BackToSettings)
}
