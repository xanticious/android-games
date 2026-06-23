package com.xanticious.androidgames.state.games.loveletter

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

/** High-level Love Letter match phases observed by the composable. */
enum class LoveLetterPhase {
    IDLE,
    SETUP,
    PLAYING,
    ROUND_OVER,
    GAME_OVER
}

private sealed class LLState : DefaultState() {
    data object Idle : LLState()
    data object Setup : LLState()
    data object Playing : LLState()
    data object RoundOver : LLState()
    data object GameOver : LLState()
}

private sealed interface LLEvent : Event {
    data object GameStarted : LLEvent
    data object RoundSetup : LLEvent
    data object RoundOver : LLEvent
    data object NextRound : LLEvent
    data object GameWon : LLEvent
    data object Rematch : LLEvent
}

/**
 * Drives Love Letter's match-level phase transitions.
 * Card and turn logic live in [com.xanticious.androidgames.controller.games.loveletter.LoveLetterController].
 * The [scope] is injectable for testing without the Android main dispatcher.
 */
class LoveLetterStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(LoveLetterPhase.IDLE)
    val phase: StateFlow<LoveLetterPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(LLState.Idle) {
            transition<LLEvent.GameStarted> {
                targetState = LLState.Setup
                onTriggered { _phase.value = LoveLetterPhase.SETUP }
            }
        }
        addState(LLState.Setup) {
            transition<LLEvent.RoundSetup> {
                targetState = LLState.Playing
                onTriggered { _phase.value = LoveLetterPhase.PLAYING }
            }
        }
        addState(LLState.Playing) {
            transition<LLEvent.RoundOver> {
                targetState = LLState.RoundOver
                onTriggered { _phase.value = LoveLetterPhase.ROUND_OVER }
            }
        }
        addState(LLState.RoundOver) {
            transition<LLEvent.NextRound> {
                targetState = LLState.Playing
                onTriggered { _phase.value = LoveLetterPhase.PLAYING }
            }
            transition<LLEvent.GameWon> {
                targetState = LLState.GameOver
                onTriggered { _phase.value = LoveLetterPhase.GAME_OVER }
            }
        }
        addState(LLState.GameOver) {
            transition<LLEvent.Rematch> {
                targetState = LLState.Setup
                onTriggered { _phase.value = LoveLetterPhase.SETUP }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(LLEvent.GameStarted)
    fun roundSetup() = machine.processEventByLaunch(LLEvent.RoundSetup)
    fun roundOver() = machine.processEventByLaunch(LLEvent.RoundOver)
    fun nextRound() = machine.processEventByLaunch(LLEvent.NextRound)
    fun gameWon() = machine.processEventByLaunch(LLEvent.GameWon)
    fun rematch() = machine.processEventByLaunch(LLEvent.Rematch)
}
