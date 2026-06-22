package com.xanticious.androidgames.state.games.mathquiz

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

/** High-level Math Quiz session phases observed by the composable. */
enum class MathQuizPhase {
    IDLE,
    SETTINGS,
    GENERATING_QUESTION,
    AWAITING_ANSWER,
    SHOWING_FEEDBACK,
    RESULTS
}

private sealed class QuizState : DefaultState() {
    data object Idle : QuizState()
    data object Settings : QuizState()
    data object GeneratingQuestion : QuizState()
    data object AwaitingAnswer : QuizState()
    data object ShowingFeedback : QuizState()
    data object Results : QuizState()
}

private sealed interface QuizEvent : Event {
    data object OpenGame : QuizEvent
    data object Back : QuizEvent
    data object StartSession : QuizEvent
    data object QuestionReady : QuizEvent
    data object AnswerCorrect : QuizEvent
    data object AnswerIncorrect : QuizEvent
    data object CountdownExpired : QuizEvent
    data object FeedbackDoneMoreQuestions : QuizEvent
    data object FeedbackDoneSessionComplete : QuizEvent
    data object PlayAgain : QuizEvent
    data object AdjustSettings : QuizEvent
}

/**
 * Drives Math Quiz's session-level phase transitions.  Question generation and
 * answer validation live in the controller layer; this machine only tracks
 * *which phase* the session is in so the composable can react accordingly.
 *
 * The [scope] is injectable so the machine can be exercised in plain JVM unit
 * tests without the Android main dispatcher.
 *
 * State diagram (condensed):
 * ```
 * Idle ──OpenGame──► Settings
 * Settings ──StartSession──► GeneratingQuestion | Back──► Idle
 * GeneratingQuestion ──QuestionReady──► AwaitingAnswer
 * AwaitingAnswer ──AnswerCorrect/AnswerIncorrect──► ShowingFeedback
 * AwaitingAnswer ──CountdownExpired──► Results
 * ShowingFeedback ──FeedbackDone(hasMore=true)──► GeneratingQuestion
 * ShowingFeedback ──FeedbackDone(hasMore=false)──► Results
 * Results ──PlayAgain──► GeneratingQuestion | AdjustSettings──► Settings
 * ```
 */
class MathQuizStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(MathQuizPhase.IDLE)
    val phase: StateFlow<MathQuizPhase> = _phase.asStateFlow()

    /** Set to `true` when the last submitted answer was correct; `false` otherwise. */
    var lastAnswerCorrect: Boolean = false
        private set

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(QuizState.Idle) {
            transition<QuizEvent.OpenGame> {
                targetState = QuizState.Settings
                onTriggered { _phase.value = MathQuizPhase.SETTINGS }
            }
        }
        addState(QuizState.Settings) {
            transition<QuizEvent.StartSession> {
                targetState = QuizState.GeneratingQuestion
                onTriggered { _phase.value = MathQuizPhase.GENERATING_QUESTION }
            }
            transition<QuizEvent.Back> {
                targetState = QuizState.Idle
                onTriggered { _phase.value = MathQuizPhase.IDLE }
            }
        }
        addState(QuizState.GeneratingQuestion) {
            transition<QuizEvent.QuestionReady> {
                targetState = QuizState.AwaitingAnswer
                onTriggered { _phase.value = MathQuizPhase.AWAITING_ANSWER }
            }
        }
        addState(QuizState.AwaitingAnswer) {
            transition<QuizEvent.AnswerCorrect> {
                targetState = QuizState.ShowingFeedback
                onTriggered {
                    lastAnswerCorrect = true
                    _phase.value = MathQuizPhase.SHOWING_FEEDBACK
                }
            }
            transition<QuizEvent.AnswerIncorrect> {
                targetState = QuizState.ShowingFeedback
                onTriggered {
                    lastAnswerCorrect = false
                    _phase.value = MathQuizPhase.SHOWING_FEEDBACK
                }
            }
            transition<QuizEvent.CountdownExpired> {
                targetState = QuizState.Results
                onTriggered { _phase.value = MathQuizPhase.RESULTS }
            }
        }
        addState(QuizState.ShowingFeedback) {
            transition<QuizEvent.FeedbackDoneMoreQuestions> {
                targetState = QuizState.GeneratingQuestion
                onTriggered { _phase.value = MathQuizPhase.GENERATING_QUESTION }
            }
            transition<QuizEvent.FeedbackDoneSessionComplete> {
                targetState = QuizState.Results
                onTriggered { _phase.value = MathQuizPhase.RESULTS }
            }
        }
        addState(QuizState.Results) {
            transition<QuizEvent.PlayAgain> {
                targetState = QuizState.GeneratingQuestion
                onTriggered { _phase.value = MathQuizPhase.GENERATING_QUESTION }
            }
            transition<QuizEvent.AdjustSettings> {
                targetState = QuizState.Settings
                onTriggered { _phase.value = MathQuizPhase.SETTINGS }
            }
        }
    }

    fun openGame() = machine.processEventByLaunch(QuizEvent.OpenGame)
    fun back() = machine.processEventByLaunch(QuizEvent.Back)
    fun startSession() = machine.processEventByLaunch(QuizEvent.StartSession)
    fun questionReady() = machine.processEventByLaunch(QuizEvent.QuestionReady)
    fun answerCorrect() = machine.processEventByLaunch(QuizEvent.AnswerCorrect)
    fun answerIncorrect() = machine.processEventByLaunch(QuizEvent.AnswerIncorrect)
    fun countdownExpired() = machine.processEventByLaunch(QuizEvent.CountdownExpired)
    fun feedbackDone(hasMore: Boolean) = machine.processEventByLaunch(
        if (hasMore) QuizEvent.FeedbackDoneMoreQuestions else QuizEvent.FeedbackDoneSessionComplete
    )
    fun playAgain() = machine.processEventByLaunch(QuizEvent.PlayAgain)
    fun adjustSettings() = machine.processEventByLaunch(QuizEvent.AdjustSettings)
}
