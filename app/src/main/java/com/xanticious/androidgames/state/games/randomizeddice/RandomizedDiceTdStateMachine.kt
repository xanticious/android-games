package com.xanticious.androidgames.state.games.randomizeddice

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
 * High-level Randomized Dice TD phase observed by the composable.
 *
 * State diagram:
 * ```
 * Idle ──LevelStarted──► Building
 * Building ──TowerAction──► Building (self — after buy/upgrade)
 * Building ──WaveStarted──► Wave
 * Wave ──Tick──► Wave (self — each combat frame)
 * Wave ──WaveCleared──► Building
 * Wave ──AllWavesCleared──► Victory
 * Wave ──BaseOverrun──► Defeat
 * Victory/Defeat ──Retry/Menu──► Idle
 * ```
 */
enum class DiceTdPhase {
    IDLE,
    BUILDING,
    WAVE,
    VICTORY,
    DEFEAT
}

private sealed class DiceTdNavState : DefaultState() {
    data object Idle : DiceTdNavState()
    data object Building : DiceTdNavState()
    data object Wave : DiceTdNavState()
    data object Victory : DiceTdNavState()
    data object Defeat : DiceTdNavState()
}

private sealed interface DiceTdEvent : Event {
    data object LevelStarted : DiceTdEvent
    data object TowerAction : DiceTdEvent
    data object WaveStarted : DiceTdEvent
    data object Tick : DiceTdEvent
    data object WaveCleared : DiceTdEvent
    data object AllWavesCleared : DiceTdEvent
    data object BaseOverrun : DiceTdEvent
    data object Retry : DiceTdEvent
    data object Menu : DiceTdEvent
}

/**
 * Drives Randomized Dice TD phase transitions.  Game state lives in the controller
 * layer; this machine only tracks *which phase* the session is in.
 *
 * The [scope] is injectable for plain-JVM unit tests.
 */
class RandomizedDiceTdStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(DiceTdPhase.IDLE)
    val phase: StateFlow<DiceTdPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(DiceTdNavState.Idle) {
            transition<DiceTdEvent.LevelStarted> {
                targetState = DiceTdNavState.Building
                onTriggered { _phase.value = DiceTdPhase.BUILDING }
            }
        }
        addState(DiceTdNavState.Building) {
            transition<DiceTdEvent.TowerAction> {
                targetState = DiceTdNavState.Building
                onTriggered { _phase.value = DiceTdPhase.BUILDING }
            }
            transition<DiceTdEvent.WaveStarted> {
                targetState = DiceTdNavState.Wave
                onTriggered { _phase.value = DiceTdPhase.WAVE }
            }
        }
        addState(DiceTdNavState.Wave) {
            transition<DiceTdEvent.Tick> {
                targetState = DiceTdNavState.Wave
                onTriggered { _phase.value = DiceTdPhase.WAVE }
            }
            transition<DiceTdEvent.WaveCleared> {
                targetState = DiceTdNavState.Building
                onTriggered { _phase.value = DiceTdPhase.BUILDING }
            }
            transition<DiceTdEvent.AllWavesCleared> {
                targetState = DiceTdNavState.Victory
                onTriggered { _phase.value = DiceTdPhase.VICTORY }
            }
            transition<DiceTdEvent.BaseOverrun> {
                targetState = DiceTdNavState.Defeat
                onTriggered { _phase.value = DiceTdPhase.DEFEAT }
            }
        }
        addState(DiceTdNavState.Victory) {
            transition<DiceTdEvent.Retry> {
                targetState = DiceTdNavState.Idle
                onTriggered { _phase.value = DiceTdPhase.IDLE }
            }
            transition<DiceTdEvent.Menu> {
                targetState = DiceTdNavState.Idle
                onTriggered { _phase.value = DiceTdPhase.IDLE }
            }
        }
        addState(DiceTdNavState.Defeat) {
            transition<DiceTdEvent.Retry> {
                targetState = DiceTdNavState.Idle
                onTriggered { _phase.value = DiceTdPhase.IDLE }
            }
            transition<DiceTdEvent.Menu> {
                targetState = DiceTdNavState.Idle
                onTriggered { _phase.value = DiceTdPhase.IDLE }
            }
        }
    }

    fun startLevel()      = machine.processEventByLaunch(DiceTdEvent.LevelStarted)
    fun towerAction()     = machine.processEventByLaunch(DiceTdEvent.TowerAction)
    fun startWave()       = machine.processEventByLaunch(DiceTdEvent.WaveStarted)
    fun tick()            = machine.processEventByLaunch(DiceTdEvent.Tick)
    fun waveCleared()     = machine.processEventByLaunch(DiceTdEvent.WaveCleared)
    fun allWavesCleared() = machine.processEventByLaunch(DiceTdEvent.AllWavesCleared)
    fun baseOverrun()     = machine.processEventByLaunch(DiceTdEvent.BaseOverrun)
    fun retry()           = machine.processEventByLaunch(DiceTdEvent.Retry)
    fun goToMenu()        = machine.processEventByLaunch(DiceTdEvent.Menu)
}
