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
    SPAWNING,
    PLAYING,
    RESPAWNING,
    LEVEL_COMPLETE,
    GAME_OVER
}

private sealed class NavState : DefaultState() {
    data object Idle : NavState()
    data object Spawning : NavState()
    data object Playing : NavState()
    data object Respawning : NavState()
    data object LevelComplete : NavState()
    data object GameOver : NavState()
}

private sealed interface AsteroidsEvent : Event {
    data object GameStarted : AsteroidsEvent
    data object FieldReady : AsteroidsEvent
    data object AllBeaconsCollected : AsteroidsEvent
    data object PlayerHitLivesRemaining : AsteroidsEvent
    data object PlayerHitNoLives : AsteroidsEvent
    data object RespawnComplete : AsteroidsEvent
    data object NextLevel : AsteroidsEvent
    data object Retry : AsteroidsEvent
}

/**
 * Drives Asteroids' high-level phase transitions.
 * Physics and rules live in [com.xanticious.androidgames.controller.games.asteroids.AsteroidsController].
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
            transition<AsteroidsEvent.PlayerHitLivesRemaining> {
                targetState = NavState.Respawning
                onTriggered { _phase.value = AsteroidsPhase.RESPAWNING }
            }
            transition<AsteroidsEvent.PlayerHitNoLives> {
                targetState = NavState.GameOver
                onTriggered { _phase.value = AsteroidsPhase.GAME_OVER }
            }
        }
        addState(NavState.Respawning) {
            transition<AsteroidsEvent.RespawnComplete> {
                targetState = NavState.Playing
                onTriggered { _phase.value = AsteroidsPhase.PLAYING }
            }
        }
        addState(NavState.LevelComplete) {
            transition<AsteroidsEvent.NextLevel> {
                targetState = NavState.Spawning
                onTriggered { _phase.value = AsteroidsPhase.SPAWNING }
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
    fun fieldReady() = machine.processEventByLaunch(AsteroidsEvent.FieldReady)
    fun allBeaconsCollected() = machine.processEventByLaunch(AsteroidsEvent.AllBeaconsCollected)
    fun playerHitWithLives() = machine.processEventByLaunch(AsteroidsEvent.PlayerHitLivesRemaining)
    fun playerDied() = machine.processEventByLaunch(AsteroidsEvent.PlayerHitNoLives)
    fun respawnComplete() = machine.processEventByLaunch(AsteroidsEvent.RespawnComplete)
    fun nextLevel() = machine.processEventByLaunch(AsteroidsEvent.NextLevel)
    fun retry() = machine.processEventByLaunch(AsteroidsEvent.Retry)
}
