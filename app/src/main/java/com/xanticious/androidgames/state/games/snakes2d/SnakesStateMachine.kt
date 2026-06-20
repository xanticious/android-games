package com.xanticious.androidgames.state.games.snakes2d

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

/** High-level Snakes game phases observed by the composable. */
enum class SnakesPhase { IDLE, PLAYING, DEAD }

private sealed class PhaseState : DefaultState() {
    data object Idle : PhaseState()
    data object Playing : PhaseState()
    data object Dead : PhaseState()
}

private sealed interface SnakesEvent : Event {
    data object GameStarted : SnakesEvent
    data object CollisionOccurred : SnakesEvent
    data object Restarted : SnakesEvent
}

/**
 * Drives Snakes 2D high-level phase transitions.
 *
 * ```
 * Idle  ──GameStarted──►  Playing  ──CollisionOccurred──►  Dead
 *                            ▲                               │
 *                            └──────────Restarted────────────┘
 * ```
 *
 * Game physics live in [com.xanticious.androidgames.controller.games.snakes2d.SnakesController];
 * this machine only tracks which phase the game is in so the view can react.
 *
 * [scope] is injectable so the machine can be exercised in plain JVM unit tests
 * without the Android main dispatcher.
 */
class SnakesStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(SnakesPhase.IDLE)
    val phase: StateFlow<SnakesPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(PhaseState.Idle) {
            transition<SnakesEvent.GameStarted> {
                targetState = PhaseState.Playing
                onTriggered { _phase.value = SnakesPhase.PLAYING }
            }
        }
        addState(PhaseState.Playing) {
            transition<SnakesEvent.CollisionOccurred> {
                targetState = PhaseState.Dead
                onTriggered { _phase.value = SnakesPhase.DEAD }
            }
        }
        addState(PhaseState.Dead) {
            transition<SnakesEvent.Restarted> {
                targetState = PhaseState.Playing
                onTriggered { _phase.value = SnakesPhase.PLAYING }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(SnakesEvent.GameStarted)
    fun collision() = machine.processEventByLaunch(SnakesEvent.CollisionOccurred)
    fun restart() = machine.processEventByLaunch(SnakesEvent.Restarted)
}
