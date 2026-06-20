package com.xanticious.androidgames.state.games.spacedefender

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

/** High-level Space Defender game phases observed by the composable. */
enum class SpaceDefenderPhase {
    IDLE,
    WAVE_INTRO,
    PLAYING,
    WAVE_COMPLETE,
    GAME_OVER
}

private sealed class GameState : DefaultState() {
    data object Idle : GameState()
    data object WaveIntro : GameState()
    data object Playing : GameState()
    data object WaveComplete : GameState()
    data object GameOver : GameState()
}

sealed interface SpaceDefenderGameEvent : Event {
    data object GameStart : SpaceDefenderGameEvent
    data object IntroComplete : SpaceDefenderGameEvent
    data object AllEnemiesDestroyed : SpaceDefenderGameEvent
    data object IntroDelay : SpaceDefenderGameEvent
    data object PlayerDied : SpaceDefenderGameEvent
}

/**
 * Drives Space Defender's high-level phase transitions.
 *
 * Matches the state machine diagram from the design doc:
 *   Idle → WaveIntro → Playing ↔ WaveComplete → WaveIntro (loop)
 *                              └→ GameOver
 *
 * The [scope] is injectable for plain JVM unit tests.
 */
class SpaceDefenderStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(SpaceDefenderPhase.IDLE)
    val phase: StateFlow<SpaceDefenderPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(GameState.Idle) {
            transition<SpaceDefenderGameEvent.GameStart> {
                targetState = GameState.WaveIntro
                onTriggered { _phase.value = SpaceDefenderPhase.WAVE_INTRO }
            }
        }
        addState(GameState.WaveIntro) {
            transition<SpaceDefenderGameEvent.IntroComplete> {
                targetState = GameState.Playing
                onTriggered { _phase.value = SpaceDefenderPhase.PLAYING }
            }
        }
        addState(GameState.Playing) {
            transition<SpaceDefenderGameEvent.AllEnemiesDestroyed> {
                targetState = GameState.WaveComplete
                onTriggered { _phase.value = SpaceDefenderPhase.WAVE_COMPLETE }
            }
            transition<SpaceDefenderGameEvent.PlayerDied> {
                targetState = GameState.GameOver
                onTriggered { _phase.value = SpaceDefenderPhase.GAME_OVER }
            }
        }
        addState(GameState.WaveComplete) {
            transition<SpaceDefenderGameEvent.IntroDelay> {
                targetState = GameState.WaveIntro
                onTriggered { _phase.value = SpaceDefenderPhase.WAVE_INTRO }
            }
        }
        addState(GameState.GameOver)
    }

    fun startGame() = machine.processEventByLaunch(SpaceDefenderGameEvent.GameStart)
    fun introComplete() = machine.processEventByLaunch(SpaceDefenderGameEvent.IntroComplete)
    fun allEnemiesDestroyed() = machine.processEventByLaunch(SpaceDefenderGameEvent.AllEnemiesDestroyed)
    fun nextWave() = machine.processEventByLaunch(SpaceDefenderGameEvent.IntroDelay)
    fun playerDied() = machine.processEventByLaunch(SpaceDefenderGameEvent.PlayerDied)
}
