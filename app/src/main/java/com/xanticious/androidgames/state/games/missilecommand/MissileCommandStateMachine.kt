package com.xanticious.androidgames.state.games.missilecommand

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

/** High-level Missile Command game phases observed by the composable. */
enum class MissileCommandPhase {
    IDLE,
    WAVE_INTRO,
    PLAYING,
    WAVE_TALLY,
    GAME_OVER,
}

private sealed class GameState : DefaultState() {
    data object Idle : GameState()
    data object WaveIntro : GameState()
    data object Playing : GameState()
    data object WaveTally : GameState()
    data object GameOver : GameState()
}

private sealed interface GameEvent : Event {
    data object GameStarted : GameEvent
    data object WaveBegun : GameEvent
    data object WaveCleared : GameEvent
    data object AllCitiesDestroyed : GameEvent
    data object GameWon : GameEvent
    data object TallyComplete : GameEvent
    data object Replay : GameEvent
}

/**
 * Drives Missile Command's high-level phase transitions.
 * Physics and rules live in [com.xanticious.androidgames.controller.games.missilecommand.MissileCommandController].
 *
 * The [scope] is injectable so the machine can be exercised in plain JVM unit tests.
 */
class MissileCommandStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    private val _phase = MutableStateFlow(MissileCommandPhase.IDLE)
    val phase: StateFlow<MissileCommandPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(GameState.Idle) {
            transition<GameEvent.GameStarted> {
                targetState = GameState.WaveIntro
                onTriggered { _phase.value = MissileCommandPhase.WAVE_INTRO }
            }
        }
        addState(GameState.WaveIntro) {
            transition<GameEvent.WaveBegun> {
                targetState = GameState.Playing
                onTriggered { _phase.value = MissileCommandPhase.PLAYING }
            }
        }
        addState(GameState.Playing) {
            transition<GameEvent.WaveCleared> {
                targetState = GameState.WaveTally
                onTriggered { _phase.value = MissileCommandPhase.WAVE_TALLY }
            }
            transition<GameEvent.AllCitiesDestroyed> {
                targetState = GameState.GameOver
                onTriggered { _phase.value = MissileCommandPhase.GAME_OVER }
            }
        }
        addState(GameState.WaveTally) {
            transition<GameEvent.TallyComplete> {
                targetState = GameState.WaveIntro
                onTriggered { _phase.value = MissileCommandPhase.WAVE_INTRO }
            }
            transition<GameEvent.GameWon> {
                targetState = GameState.GameOver
                onTriggered { _phase.value = MissileCommandPhase.GAME_OVER }
            }
        }
        addState(GameState.GameOver) {
            transition<GameEvent.Replay> {
                targetState = GameState.WaveIntro
                onTriggered { _phase.value = MissileCommandPhase.WAVE_INTRO }
            }
        }
    }

    fun gameStarted() = machine.processEventByLaunch(GameEvent.GameStarted)
    fun waveBegun() = machine.processEventByLaunch(GameEvent.WaveBegun)
    fun waveCleared() = machine.processEventByLaunch(GameEvent.WaveCleared)
    fun allCitiesDestroyed() = machine.processEventByLaunch(GameEvent.AllCitiesDestroyed)
    fun gameWon() = machine.processEventByLaunch(GameEvent.GameWon)
    fun tallyComplete() = machine.processEventByLaunch(GameEvent.TallyComplete)
    fun replay() = machine.processEventByLaunch(GameEvent.Replay)
}
