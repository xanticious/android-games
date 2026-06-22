package com.xanticious.androidgames.state.games.endlessrunner

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

/** High-level phases of an endless-runner run, observed by the composable. */
enum class RunnerPhase { IDLE, RUNNING, DEAD }

private sealed class RunnerMachineState : DefaultState() {
    data object Idle : RunnerMachineState()
    data object Running : RunnerMachineState()
    data object Dead : RunnerMachineState()
}

private sealed interface RunnerMachineEvent : Event {
    data object TapToStart : RunnerMachineEvent
    data object RunnerDied : RunnerMachineEvent
    data object Restart : RunnerMachineEvent
}

/**
 * Drives the endless-runner session through its three phases:
 * **Idle** → tap → **Running** → obstacle hit → **Dead** → restart → **Idle**.
 *
 * Physics and collision logic live in
 * [com.xanticious.androidgames.controller.games.endlessrunner.EndlessRunnerController];
 * this machine only tracks which phase the UI should display.
 *
 * [scope] is injectable so the machine can be exercised in plain JVM unit tests
 * without the Android main dispatcher.
 */
class EndlessRunnerStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(RunnerPhase.IDLE)
    val phase: StateFlow<RunnerPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(RunnerMachineState.Idle) {
            transition<RunnerMachineEvent.TapToStart> {
                targetState = RunnerMachineState.Running
                onTriggered { _phase.value = RunnerPhase.RUNNING }
            }
        }
        addState(RunnerMachineState.Running) {
            transition<RunnerMachineEvent.RunnerDied> {
                targetState = RunnerMachineState.Dead
                onTriggered { _phase.value = RunnerPhase.DEAD }
            }
        }
        addState(RunnerMachineState.Dead) {
            transition<RunnerMachineEvent.Restart> {
                targetState = RunnerMachineState.Idle
                onTriggered { _phase.value = RunnerPhase.IDLE }
            }
        }
    }

    fun startRun() = machine.processEventByLaunch(RunnerMachineEvent.TapToStart)
    fun runnerDied() = machine.processEventByLaunch(RunnerMachineEvent.RunnerDied)
    fun restart() = machine.processEventByLaunch(RunnerMachineEvent.Restart)
}
