package com.xanticious.androidgames.state.games.puzzle

import com.xanticious.androidgames.model.games.puzzle.PuzzlePhase
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
 * Reusable KStateMachine that drives the standard puzzle lifecycle shared by all
 * self-configured puzzle games (`design/common/puzzle-flow.md`):
 *
 * ```
 * Settings ─openHowToPlay→ HowToPlay ─backToSettings→ Settings
 * Settings ─startGame→ Playing
 * Playing  ─solved→ Solved ; ─failed→ Failed ; ─backToSettings→ Settings
 * Solved/Failed ─retry→ Playing ; ─newGame→ Settings
 * ```
 *
 * The machine only tracks the phase so the view can react; all puzzle rules live
 * in the game's `controller/`. The [scope] is injectable so the machine can be
 * exercised in plain JVM unit tests without the Android main dispatcher.
 */
class PuzzleStateMachine(
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(PuzzlePhase.SETTINGS)
    val phase: StateFlow<PuzzlePhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(PhaseState.Settings) {
            transition<PuzzleNavEvent.OpenHowToPlay> {
                targetState = PhaseState.HowToPlay
                onTriggered { _phase.value = PuzzlePhase.HOW_TO_PLAY }
            }
            transition<PuzzleNavEvent.StartGame> {
                targetState = PhaseState.Playing
                onTriggered { _phase.value = PuzzlePhase.PLAYING }
            }
        }
        addState(PhaseState.HowToPlay) {
            transition<PuzzleNavEvent.BackToSettings> {
                targetState = PhaseState.Settings
                onTriggered { _phase.value = PuzzlePhase.SETTINGS }
            }
            transition<PuzzleNavEvent.StartGame> {
                targetState = PhaseState.Playing
                onTriggered { _phase.value = PuzzlePhase.PLAYING }
            }
        }
        addState(PhaseState.Playing) {
            transition<PuzzleNavEvent.Solved> {
                targetState = PhaseState.Solved
                onTriggered { _phase.value = PuzzlePhase.SOLVED }
            }
            transition<PuzzleNavEvent.Failed> {
                targetState = PhaseState.Failed
                onTriggered { _phase.value = PuzzlePhase.FAILED }
            }
            transition<PuzzleNavEvent.BackToSettings> {
                targetState = PhaseState.Settings
                onTriggered { _phase.value = PuzzlePhase.SETTINGS }
            }
        }
        addState(PhaseState.Solved) {
            transition<PuzzleNavEvent.Retry> {
                targetState = PhaseState.Playing
                onTriggered { _phase.value = PuzzlePhase.PLAYING }
            }
            transition<PuzzleNavEvent.NewGame> {
                targetState = PhaseState.Settings
                onTriggered { _phase.value = PuzzlePhase.SETTINGS }
            }
        }
        addState(PhaseState.Failed) {
            transition<PuzzleNavEvent.Retry> {
                targetState = PhaseState.Playing
                onTriggered { _phase.value = PuzzlePhase.PLAYING }
            }
            transition<PuzzleNavEvent.NewGame> {
                targetState = PhaseState.Settings
                onTriggered { _phase.value = PuzzlePhase.SETTINGS }
            }
        }
    }

    fun openHowToPlay() = machine.processEventByLaunch(PuzzleNavEvent.OpenHowToPlay)
    fun backToSettings() = machine.processEventByLaunch(PuzzleNavEvent.BackToSettings)
    fun startGame() = machine.processEventByLaunch(PuzzleNavEvent.StartGame)
    fun solved() = machine.processEventByLaunch(PuzzleNavEvent.Solved)
    fun failed() = machine.processEventByLaunch(PuzzleNavEvent.Failed)
    fun retry() = machine.processEventByLaunch(PuzzleNavEvent.Retry)
    fun newGame() = machine.processEventByLaunch(PuzzleNavEvent.NewGame)
}

private sealed class PhaseState : DefaultState() {
    data object Settings : PhaseState()
    data object HowToPlay : PhaseState()
    data object Playing : PhaseState()
    data object Solved : PhaseState()
    data object Failed : PhaseState()
}

private sealed interface PuzzleNavEvent : Event {
    data object OpenHowToPlay : PuzzleNavEvent
    data object BackToSettings : PuzzleNavEvent
    data object StartGame : PuzzleNavEvent
    data object Solved : PuzzleNavEvent
    data object Failed : PuzzleNavEvent
    data object Retry : PuzzleNavEvent
    data object NewGame : PuzzleNavEvent
}
