package com.xanticious.androidgames.state.games.helicopterdogfight

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

/** High-level Helicopter Dogfight game phases observed by the composable. */
enum class HelicopterDogfightPhase {
    IDLE,
    SPAWNING,
    PLAYING,
    SCREEN_CLEAR,
    SCROLLING,
    RESPAWNING,
    GAME_OVER
}

private sealed class DogfightState : DefaultState() {
    data object Idle : DogfightState()
    data object Spawning : DogfightState()
    data object Playing : DogfightState()
    data object ScreenClear : DogfightState()
    data object Scrolling : DogfightState()
    data object Respawning : DogfightState()
    data object GameOver : DogfightState()
}

private sealed interface DogfightEvent : Event {
    data object GameStart : DogfightEvent
    data object ScreenReady : DogfightEvent
    data object AllEnemiesDestroyed : DogfightEvent
    data object AllClearDelayOver : DogfightEvent
    data object NextScreenReady : DogfightEvent
    data object PlayerCrashed : DogfightEvent
    data object RespawnComplete : DogfightEvent
    data object PlayerDied : DogfightEvent
}

/**
 * Drives Helicopter Dogfight's screen-level phase transitions.
 *
 * State diagram (from design doc):
 *   Idle → Spawning → Playing ↔ ScreenClear → Scrolling → Spawning (loop)
 *                Playing → Respawning → Playing
 *                Playing → GameOver
 *
 * The [scope] is injectable so the machine is exercisable in plain JVM unit tests.
 */
class HelicopterDogfightStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(HelicopterDogfightPhase.IDLE)
    val phase: StateFlow<HelicopterDogfightPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(DogfightState.Idle) {
            transition<DogfightEvent.GameStart> {
                targetState = DogfightState.Spawning
                onTriggered { _phase.value = HelicopterDogfightPhase.SPAWNING }
            }
        }
        addState(DogfightState.Spawning) {
            transition<DogfightEvent.ScreenReady> {
                targetState = DogfightState.Playing
                onTriggered { _phase.value = HelicopterDogfightPhase.PLAYING }
            }
        }
        addState(DogfightState.Playing) {
            transition<DogfightEvent.AllEnemiesDestroyed> {
                targetState = DogfightState.ScreenClear
                onTriggered { _phase.value = HelicopterDogfightPhase.SCREEN_CLEAR }
            }
            transition<DogfightEvent.PlayerCrashed> {
                targetState = DogfightState.Respawning
                onTriggered { _phase.value = HelicopterDogfightPhase.RESPAWNING }
            }
            transition<DogfightEvent.PlayerDied> {
                targetState = DogfightState.GameOver
                onTriggered { _phase.value = HelicopterDogfightPhase.GAME_OVER }
            }
        }
        addState(DogfightState.ScreenClear) {
            transition<DogfightEvent.AllClearDelayOver> {
                targetState = DogfightState.Scrolling
                onTriggered { _phase.value = HelicopterDogfightPhase.SCROLLING }
            }
        }
        addState(DogfightState.Scrolling) {
            transition<DogfightEvent.NextScreenReady> {
                targetState = DogfightState.Spawning
                onTriggered { _phase.value = HelicopterDogfightPhase.SPAWNING }
            }
        }
        addState(DogfightState.Respawning) {
            transition<DogfightEvent.RespawnComplete> {
                targetState = DogfightState.Playing
                onTriggered { _phase.value = HelicopterDogfightPhase.PLAYING }
            }
        }
        addState(DogfightState.GameOver)
    }

    fun startGame() = machine.processEventByLaunch(DogfightEvent.GameStart)
    fun screenReady() = machine.processEventByLaunch(DogfightEvent.ScreenReady)
    fun allEnemiesDestroyed() = machine.processEventByLaunch(DogfightEvent.AllEnemiesDestroyed)
    fun allClearDelayOver() = machine.processEventByLaunch(DogfightEvent.AllClearDelayOver)
    fun nextScreenReady() = machine.processEventByLaunch(DogfightEvent.NextScreenReady)
    fun playerCrashed() = machine.processEventByLaunch(DogfightEvent.PlayerCrashed)
    fun respawnComplete() = machine.processEventByLaunch(DogfightEvent.RespawnComplete)
    fun playerDied() = machine.processEventByLaunch(DogfightEvent.PlayerDied)
}
