package com.xanticious.androidgames.state.games.hearts

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

/**
 * High-level Hearts phase observed by the composable.
 *
 * - IDLE       → before the first deal.
 * - DEALING    → deck being shuffled/distributed (brief intermediate state).
 * - PASSING    → human selects 3 cards to pass (skipped on HOLD hands).
 * - PLAYING    → trick play in progress (human and AI take turns).
 * - TRICK_END  → all 4 cards played; completed trick shown before resolution.
 * - SCORING    → hand scores shown; auto-advances to next deal or game-over.
 * - GAME_OVER  → match finished; winner/loser panel visible.
 */
enum class HeartsPhase {
    IDLE, DEALING, PASSING, PLAYING, TRICK_END, SCORING, GAME_OVER
}

private sealed class HState : DefaultState() {
    data object Idle       : HState()
    data object Dealing    : HState()
    data object Passing    : HState()
    data object Playing    : HState()
    data object TrickEnd   : HState()
    data object Scoring    : HState()
    data object GameOver   : HState()
}

private sealed interface HEvent : Event {
    data object StartGame      : HEvent
    data object DealtPass      : HEvent   // dealt, pass direction ≠ HOLD
    data object DealtHold      : HEvent   // dealt, pass direction = HOLD
    data object PassConfirmed  : HEvent
    data object TrickComplete  : HEvent
    data object TrickResolved  : HEvent
    data object HandComplete   : HEvent
    data object NextHandPass   : HEvent   // next hand has a passing round
    data object NextHandHold   : HEvent   // next hand is HOLD
    data object GameEnded      : HEvent
    data object Rematch        : HEvent
}

/**
 * KStateMachine driving Hearts phase transitions. Exposes [phase] as a
 * [StateFlow] so composables can `collectAsState()` and react to changes.
 *
 * The [scope] is injectable so the machine can be exercised in plain JUnit
 * tests without the Android main dispatcher (pass `CoroutineScope(Dispatchers.Unconfined)`).
 */
class HeartsStateMachine(
    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(HeartsPhase.IDLE)
    val phase: StateFlow<HeartsPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {

        addInitialState(HState.Idle) {
            transition<HEvent.StartGame> {
                targetState = HState.Dealing
                onTriggered { _phase.value = HeartsPhase.DEALING }
            }
        }

        addState(HState.Dealing) {
            transition<HEvent.DealtPass> {
                targetState = HState.Passing
                onTriggered { _phase.value = HeartsPhase.PASSING }
            }
            transition<HEvent.DealtHold> {
                targetState = HState.Playing
                onTriggered { _phase.value = HeartsPhase.PLAYING }
            }
        }

        addState(HState.Passing) {
            transition<HEvent.PassConfirmed> {
                targetState = HState.Playing
                onTriggered { _phase.value = HeartsPhase.PLAYING }
            }
        }

        addState(HState.Playing) {
            transition<HEvent.TrickComplete> {
                targetState = HState.TrickEnd
                onTriggered { _phase.value = HeartsPhase.TRICK_END }
            }
        }

        addState(HState.TrickEnd) {
            transition<HEvent.TrickResolved> {
                targetState = HState.Playing
                onTriggered { _phase.value = HeartsPhase.PLAYING }
            }
            transition<HEvent.HandComplete> {
                targetState = HState.Scoring
                onTriggered { _phase.value = HeartsPhase.SCORING }
            }
        }

        addState(HState.Scoring) {
            transition<HEvent.NextHandPass> {
                targetState = HState.Dealing
                onTriggered { _phase.value = HeartsPhase.DEALING }
            }
            transition<HEvent.NextHandHold> {
                targetState = HState.Dealing
                onTriggered { _phase.value = HeartsPhase.DEALING }
            }
            transition<HEvent.GameEnded> {
                targetState = HState.GameOver
                onTriggered { _phase.value = HeartsPhase.GAME_OVER }
            }
        }

        addState(HState.GameOver) {
            transition<HEvent.Rematch> {
                targetState = HState.Dealing
                onTriggered { _phase.value = HeartsPhase.DEALING }
            }
        }
    }

    fun startGame()     = machine.processEventByLaunch(HEvent.StartGame)
    fun dealtPass()     = machine.processEventByLaunch(HEvent.DealtPass)
    fun dealtHold()     = machine.processEventByLaunch(HEvent.DealtHold)
    fun passConfirmed() = machine.processEventByLaunch(HEvent.PassConfirmed)
    fun trickComplete() = machine.processEventByLaunch(HEvent.TrickComplete)
    fun trickResolved() = machine.processEventByLaunch(HEvent.TrickResolved)
    fun handComplete()  = machine.processEventByLaunch(HEvent.HandComplete)
    fun nextHandPass()  = machine.processEventByLaunch(HEvent.NextHandPass)
    fun nextHandHold()  = machine.processEventByLaunch(HEvent.NextHandHold)
    fun gameEnded()     = machine.processEventByLaunch(HEvent.GameEnded)
    fun rematch()       = machine.processEventByLaunch(HEvent.Rematch)
}
