package com.xanticious.androidgames.state.games.asteroids

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

/** High-level Asteroids game phases observed by the composable. */
enum class AsteroidsPhase {
    IDLE,
    SETUP,
    SPAWNING,
    PLAYING,
    LEVEL_COMPLETE,
    GAME_OVER
}

private sealed class NavState : DefaultState() {
    data object Idle : NavState()
    data object Setup : NavState()
    data object Spawning : NavState()
    data object Playing : NavState()
    data object LevelComplete : NavState()
    data object GameOver : NavState()
}

private sealed interface AsteroidsEvent : Event {
    data object GameStarted : AsteroidsEvent
    data object ConfigConfirmed : AsteroidsEvent
    data object FieldReady : AsteroidsEvent
    data object AllBeaconsCollected : AsteroidsEvent
    data object GameEnded : AsteroidsEvent
    data object NextLevel : AsteroidsEvent
    data object Retry : AsteroidsEvent
}

/**
 * Drives Asteroids' high-level phase transitions.
 * Physics and rules live in [com.xanticious.androidgames.controller.games.asteroids.AsteroidsController].
 *
 * Taking damage no longer triggers a respawn phase: the controller teleports the
 * ship to safety and briefly freezes the asteroids while play continues, so the
 * mode timer keeps running. [GameEnded] covers all terminal outcomes (out of
 * lives, time expired, or a completed level challenge).
 *
 * [scope] is injectable so the machine can be exercised in plain JVM unit tests
 * without the Android main dispatcher.
 */
class AsteroidsStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(AsteroidsPhase.IDLE)
    val phase: StateFlow<AsteroidsPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(NavState.Idle) {
            transition<AsteroidsEvent.GameStarted> {
                targetState = NavState.Setup
                onTriggered { _phase.value = AsteroidsPhase.SETUP }
            }
        }
        addState(NavState.Setup) {
            transition<AsteroidsEvent.ConfigConfirmed> {
                targetState = NavState.Spawning
                onTriggered { _phase.value = AsteroidsPhase.SPAWNING }
            }
        }
        addState(NavState.Spawning) {
            transition<AsteroidsEvent.FieldReady> {
                targetState = NavState.Playing
                onTriggered { _phase.value = AsteroidsPhase.PLAYING }
            }
        }
        addState(NavState.Playing) {
            transition<AsteroidsEvent.AllBeaconsCollected> {
                targetState = NavState.LevelComplete
                onTriggered { _phase.value = AsteroidsPhase.LEVEL_COMPLETE }
            }
            transition<AsteroidsEvent.GameEnded> {
                targetState = NavState.GameOver
                onTriggered { _phase.value = AsteroidsPhase.GAME_OVER }
            }
        }
        addState(NavState.LevelComplete) {
            transition<AsteroidsEvent.NextLevel> {
                targetState = NavState.Spawning
                onTriggered { _phase.value = AsteroidsPhase.SPAWNING }
            }
            transition<AsteroidsEvent.GameEnded> {
                targetState = NavState.GameOver
                onTriggered { _phase.value = AsteroidsPhase.GAME_OVER }
            }
        }
        addState(NavState.GameOver) {
            transition<AsteroidsEvent.Retry> {
                targetState = NavState.Spawning
                onTriggered { _phase.value = AsteroidsPhase.SPAWNING }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(AsteroidsEvent.GameStarted)
    fun confirmConfig() = machine.processEventByLaunch(AsteroidsEvent.ConfigConfirmed)
    fun fieldReady() = machine.processEventByLaunch(AsteroidsEvent.FieldReady)
    fun allBeaconsCollected() = machine.processEventByLaunch(AsteroidsEvent.AllBeaconsCollected)
    fun gameEnded() = machine.processEventByLaunch(AsteroidsEvent.GameEnded)
    fun nextLevel() = machine.processEventByLaunch(AsteroidsEvent.NextLevel)
    fun retry() = machine.processEventByLaunch(AsteroidsEvent.Retry)
}
