package com.xanticious.androidgames.state.games.basedefense

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
 * High-level Base Defense match phases observed by the composable.
 *
 * ```
 * IDLE → BUILDING → WAVE → BUILDING (repeat) → VICTORY / DEFEAT
 * VICTORY / DEFEAT → IDLE (retry or menu)
 * ```
 */
enum class BaseDefensePhase {
    IDLE,
    BUILDING,
    WAVE,
    VICTORY,
    DEFEAT
}

private sealed class BdState : DefaultState() {
    data object Idle : BdState()
    data object Building : BdState()
    data object Wave : BdState()
    data object Victory : BdState()
    data object Defeat : BdState()
}

private sealed interface BdEvent : Event {
    data object LevelStarted : BdEvent
    data object WaveStarted : BdEvent
    data object WaveCleared : BdEvent
    data object AllWavesCleared : BdEvent
    data object BaseOverrun : BdEvent
    data object Retry : BdEvent
    data object Menu : BdEvent
}

/**
 * Drives Base Defense phase transitions via KStateMachine.
 *
 * The [scope] is injectable so the machine can be exercised in plain JVM
 * unit tests without the Android main dispatcher.
 *
 * State diagram:
 * ```
 * Idle ──LevelStarted──► Building
 * Building ──WaveStarted──► Wave
 * Wave ──WaveCleared──► Building
 * Wave ──AllWavesCleared──► Victory
 * Wave ──BaseOverrun──► Defeat
 * Victory ──Retry / Menu──► Idle
 * Defeat ──Retry / Menu──► Idle
 * ```
 */
class BaseDefenseStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(BaseDefensePhase.IDLE)
    val phase: StateFlow<BaseDefensePhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(BdState.Idle) {
            transition<BdEvent.LevelStarted> {
                targetState = BdState.Building
                onTriggered { _phase.value = BaseDefensePhase.BUILDING }
            }
        }
        addState(BdState.Building) {
            transition<BdEvent.WaveStarted> {
                targetState = BdState.Wave
                onTriggered { _phase.value = BaseDefensePhase.WAVE }
            }
        }
        addState(BdState.Wave) {
            transition<BdEvent.WaveCleared> {
                targetState = BdState.Building
                onTriggered { _phase.value = BaseDefensePhase.BUILDING }
            }
            transition<BdEvent.AllWavesCleared> {
                targetState = BdState.Victory
                onTriggered { _phase.value = BaseDefensePhase.VICTORY }
            }
            transition<BdEvent.BaseOverrun> {
                targetState = BdState.Defeat
                onTriggered { _phase.value = BaseDefensePhase.DEFEAT }
            }
        }
        addState(BdState.Victory) {
            transition<BdEvent.Retry> {
                targetState = BdState.Idle
                onTriggered { _phase.value = BaseDefensePhase.IDLE }
            }
            transition<BdEvent.Menu> {
                targetState = BdState.Idle
                onTriggered { _phase.value = BaseDefensePhase.IDLE }
            }
        }
        addState(BdState.Defeat) {
            transition<BdEvent.Retry> {
                targetState = BdState.Idle
                onTriggered { _phase.value = BaseDefensePhase.IDLE }
            }
            transition<BdEvent.Menu> {
                targetState = BdState.Idle
                onTriggered { _phase.value = BaseDefensePhase.IDLE }
            }
        }
    }

    fun levelStarted() = machine.processEventByLaunch(BdEvent.LevelStarted)
    fun waveStarted() = machine.processEventByLaunch(BdEvent.WaveStarted)
    fun waveCleared() = machine.processEventByLaunch(BdEvent.WaveCleared)
    fun allWavesCleared() = machine.processEventByLaunch(BdEvent.AllWavesCleared)
    fun baseOverrun() = machine.processEventByLaunch(BdEvent.BaseOverrun)
    fun retry() = machine.processEventByLaunch(BdEvent.Retry)
    fun menu() = machine.processEventByLaunch(BdEvent.Menu)
}
