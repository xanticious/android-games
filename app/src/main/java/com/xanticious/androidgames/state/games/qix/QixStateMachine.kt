package com.xanticious.androidgames.state.games.qix

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

/** High-level Qix game phases observed by the composable. */
enum class QixPhase { IDLE, PLAYING, LIFE_LOST, LEVEL_COMPLETE, GAME_OVER }

private sealed class QixGameState : DefaultState() {
    data object Idle : QixGameState()
    data object Playing : QixGameState()
    data object LifeLost : QixGameState()
    data object LevelComplete : QixGameState()
    data object GameOver : QixGameState()
}

private sealed interface QixGameEvent : Event {
    data object GameStarted : QixGameEvent
    data object CollisionOccurred : QixGameEvent
    data object Respawned : QixGameEvent
    data object LivesExhausted : QixGameEvent
    data object LevelAchieved : QixGameEvent
}

/**
 * Drives Qix's game-lifecycle phase transitions. Board physics and collision
 * detection live in [com.xanticious.androidgames.controller.games.qix.QixController];
 * this machine only tracks which macro-phase the game is in so the view can react.
 *
 * State graph:
 * ```
 * Idle ──[GameStarted]──▶ Playing
 * Playing ──[CollisionOccurred]──▶ LifeLost
 * Playing ──[LevelAchieved]──▶ LevelComplete
 * Playing ──[LivesExhausted]──▶ GameOver   (direct path when last life is lost)
 * LifeLost ──[Respawned]──▶ Playing
 * LifeLost ──[LivesExhausted]──▶ GameOver
 * ```
 * LevelComplete and GameOver are terminal (no outgoing transitions).
 *
 * The [scope] is injectable so the machine can be exercised in plain JVM unit
 * tests without the Android main dispatcher.
 */
class QixStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(QixPhase.IDLE)
    val phase: StateFlow<QixPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(QixGameState.Idle) {
            transition<QixGameEvent.GameStarted> {
                targetState = QixGameState.Playing
                onTriggered { _phase.value = QixPhase.PLAYING }
            }
        }
        addState(QixGameState.Playing) {
            transition<QixGameEvent.CollisionOccurred> {
                targetState = QixGameState.LifeLost
                onTriggered { _phase.value = QixPhase.LIFE_LOST }
            }
            transition<QixGameEvent.LevelAchieved> {
                targetState = QixGameState.LevelComplete
                onTriggered { _phase.value = QixPhase.LEVEL_COMPLETE }
            }
            // Direct game-over path when the last life is consumed.
            transition<QixGameEvent.LivesExhausted> {
                targetState = QixGameState.GameOver
                onTriggered { _phase.value = QixPhase.GAME_OVER }
            }
        }
        addState(QixGameState.LifeLost) {
            transition<QixGameEvent.Respawned> {
                targetState = QixGameState.Playing
                onTriggered { _phase.value = QixPhase.PLAYING }
            }
            transition<QixGameEvent.LivesExhausted> {
                targetState = QixGameState.GameOver
                onTriggered { _phase.value = QixPhase.GAME_OVER }
            }
        }
        addState(QixGameState.LevelComplete)
        addState(QixGameState.GameOver)
    }

    fun startGame() = machine.processEventByLaunch(QixGameEvent.GameStarted)
    fun collisionOccurred() = machine.processEventByLaunch(QixGameEvent.CollisionOccurred)
    fun respawned() = machine.processEventByLaunch(QixGameEvent.Respawned)
    fun livesExhausted() = machine.processEventByLaunch(QixGameEvent.LivesExhausted)
    fun levelAchieved() = machine.processEventByLaunch(QixGameEvent.LevelAchieved)
}
