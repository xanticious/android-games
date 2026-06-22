package com.xanticious.androidgames.state.games.holeswallowing

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

/** High-level game phases observed by the composable. */
enum class HoleSwallowingPhase { IDLE, PLAYING, LEVEL_COMPLETE, GAME_OVER }

private sealed class HoleGameState : DefaultState() {
    data object Idle : HoleGameState()
    data object Playing : HoleGameState()
    data object LevelComplete : HoleGameState()
    data object GameOver : HoleGameState()
}

private sealed interface HoleGameEvent : Event {
    data object LevelStarted : HoleGameEvent
    data object TargetReached : HoleGameEvent
    data object TimerExpired : HoleGameEvent
    data object Replay : HoleGameEvent
}

/**
 * Drives Hole-Swallowing's phase transitions. Game physics live in
 * [com.xanticious.androidgames.controller.games.holeswallowing.HoleSwallowingController];
 * this machine only tracks which phase the level is in.
 *
 * The [scope] is injectable so the machine can be exercised in plain JVM unit
 * tests without the Android main dispatcher.
 */
class HoleSwallowingStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(HoleSwallowingPhase.IDLE)
    val phase: StateFlow<HoleSwallowingPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(HoleGameState.Idle) {
            transition<HoleGameEvent.LevelStarted> {
                targetState = HoleGameState.Playing
                onTriggered { _phase.value = HoleSwallowingPhase.PLAYING }
            }
        }
        addState(HoleGameState.Playing) {
            transition<HoleGameEvent.TargetReached> {
                targetState = HoleGameState.LevelComplete
                onTriggered { _phase.value = HoleSwallowingPhase.LEVEL_COMPLETE }
            }
            transition<HoleGameEvent.TimerExpired> {
                targetState = HoleGameState.GameOver
                onTriggered { _phase.value = HoleSwallowingPhase.GAME_OVER }
            }
        }
        addState(HoleGameState.LevelComplete) {
            transition<HoleGameEvent.Replay> {
                targetState = HoleGameState.Idle
                onTriggered { _phase.value = HoleSwallowingPhase.IDLE }
            }
        }
        addState(HoleGameState.GameOver) {
            transition<HoleGameEvent.Replay> {
                targetState = HoleGameState.Idle
                onTriggered { _phase.value = HoleSwallowingPhase.IDLE }
            }
        }
    }

    fun startLevel() = machine.processEventByLaunch(HoleGameEvent.LevelStarted)
    fun targetReached() = machine.processEventByLaunch(HoleGameEvent.TargetReached)
    fun timerExpired() = machine.processEventByLaunch(HoleGameEvent.TimerExpired)
    fun replay() = machine.processEventByLaunch(HoleGameEvent.Replay)
}
