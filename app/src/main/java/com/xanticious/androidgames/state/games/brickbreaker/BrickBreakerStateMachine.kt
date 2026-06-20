package com.xanticious.androidgames.state.games.brickbreaker

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
 * High-level phases observed by all four brick-breaker composables.
 *
 * Turn-based variants (CLASSIC, CANNON) use:
 *   IDLE → LEVEL_START → AIM_PHASE → FIRE_PHASE → RESOLUTION_PHASE →
 *     CLASSIC:  DROP_PHASE → AIM_PHASE  (or GAME_OVER)
 *     CANNON:   AIM_PHASE              (or GAME_OVER)
 *   → LEVEL_COMPLETE → LEVEL_START
 *
 * Real-time variants (ARCADE, CANNON_ARCADE) use:
 *   IDLE → LEVEL_START → PLAYING →
 *     ARCADE:         LIFE_LOST → PLAYING  (or GAME_OVER)
 *     CANNON_ARCADE:  GAME_OVER
 *   → LEVEL_COMPLETE → LEVEL_START
 */
enum class BrickBreakerPhase {
    IDLE,
    LEVEL_START,
    AIM_PHASE,
    FIRE_PHASE,
    RESOLUTION_PHASE,
    DROP_PHASE,
    PLAYING,
    LIFE_LOST,
    LEVEL_COMPLETE,
    GAME_OVER,
}

private sealed class BBState : DefaultState() {
    data object Idle : BBState()
    data object LevelStart : BBState()
    data object AimPhase : BBState()
    data object FirePhase : BBState()
    data object ResolutionPhase : BBState()
    data object DropPhase : BBState()
    data object Playing : BBState()
    data object LifeLost : BBState()
    data object LevelComplete : BBState()
    data object GameOver : BBState()
}

sealed interface BBEvent : Event {
    data object LevelStarted : BBEvent        // Idle → LevelStart
    data object ReadyForAim : BBEvent         // LevelStart / DropPhase / LevelComplete(turn) → AimPhase
    data object ReadyForPlay : BBEvent        // LevelStart / LevelComplete(realtime) → Playing
    data object FireTapped : BBEvent          // AimPhase → FirePhase
    data object AllBallsLanded : BBEvent      // FirePhase → ResolutionPhase
    data object ClearTapped : BBEvent         // FirePhase → ResolutionPhase (early clear)
    data object BricksRemain : BBEvent        // ResolutionPhase → DropPhase (CLASSIC)
    data object NextTurnReady : BBEvent       // ResolutionPhase → AimPhase (CANNON: turn remains)
    data object TurnsExhausted : BBEvent      // ResolutionPhase → GameOver (CANNON)
    data object FieldCleared : BBEvent        // ResolutionPhase / Playing → LevelComplete
    data object NoBricksAtBottom : BBEvent    // DropPhase → AimPhase (CLASSIC)
    data object BricksAtBottom : BBEvent      // DropPhase → GameOver (CLASSIC)
    data object BrickHitBottom : BBEvent      // Playing → LifeLost (ARCADE)
    data object AllLivesLost : BBEvent        // Playing → GameOver (ARCADE)
    data object TimerExpired : BBEvent        // Playing → GameOver (CANNON_ARCADE)
    data object LevelRowsCleared : BBEvent    // Playing → LevelComplete (ARCADE)
    data object AllTargetsDestroyed : BBEvent // Playing / ResolutionPhase → LevelComplete
    data object RespawnReady : BBEvent        // LifeLost → Playing
    data object NextLevel : BBEvent           // LevelComplete → LevelStart
    data object Restart : BBEvent             // GameOver → Idle
}

/**
 * KStateMachine driving all four brick-breaker variants.
 *
 * The [scope] is injected so plain JVM unit tests can pass
 * `CoroutineScope(Dispatchers.Unconfined)` without the Android main dispatcher.
 */
class BrickBreakerStateMachine(
    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(BrickBreakerPhase.IDLE)
    val phase: StateFlow<BrickBreakerPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {

        addInitialState(BBState.Idle) {
            transition<BBEvent.LevelStarted> {
                targetState = BBState.LevelStart
                onTriggered { _phase.value = BrickBreakerPhase.LEVEL_START }
            }
        }

        addState(BBState.LevelStart) {
            transition<BBEvent.ReadyForAim> {
                targetState = BBState.AimPhase
                onTriggered { _phase.value = BrickBreakerPhase.AIM_PHASE }
            }
            transition<BBEvent.ReadyForPlay> {
                targetState = BBState.Playing
                onTriggered { _phase.value = BrickBreakerPhase.PLAYING }
            }
        }

        addState(BBState.AimPhase) {
            transition<BBEvent.FireTapped> {
                targetState = BBState.FirePhase
                onTriggered { _phase.value = BrickBreakerPhase.FIRE_PHASE }
            }
        }

        addState(BBState.FirePhase) {
            transition<BBEvent.AllBallsLanded> {
                targetState = BBState.ResolutionPhase
                onTriggered { _phase.value = BrickBreakerPhase.RESOLUTION_PHASE }
            }
            transition<BBEvent.ClearTapped> {
                targetState = BBState.ResolutionPhase
                onTriggered { _phase.value = BrickBreakerPhase.RESOLUTION_PHASE }
            }
        }

        addState(BBState.ResolutionPhase) {
            transition<BBEvent.BricksRemain> {
                targetState = BBState.DropPhase
                onTriggered { _phase.value = BrickBreakerPhase.DROP_PHASE }
            }
            transition<BBEvent.NextTurnReady> {
                targetState = BBState.AimPhase
                onTriggered { _phase.value = BrickBreakerPhase.AIM_PHASE }
            }
            transition<BBEvent.TurnsExhausted> {
                targetState = BBState.GameOver
                onTriggered { _phase.value = BrickBreakerPhase.GAME_OVER }
            }
            transition<BBEvent.FieldCleared> {
                targetState = BBState.LevelComplete
                onTriggered { _phase.value = BrickBreakerPhase.LEVEL_COMPLETE }
            }
            transition<BBEvent.AllTargetsDestroyed> {
                targetState = BBState.LevelComplete
                onTriggered { _phase.value = BrickBreakerPhase.LEVEL_COMPLETE }
            }
        }

        addState(BBState.DropPhase) {
            transition<BBEvent.NoBricksAtBottom> {
                targetState = BBState.AimPhase
                onTriggered { _phase.value = BrickBreakerPhase.AIM_PHASE }
            }
            transition<BBEvent.BricksAtBottom> {
                targetState = BBState.GameOver
                onTriggered { _phase.value = BrickBreakerPhase.GAME_OVER }
            }
        }

        addState(BBState.Playing) {
            transition<BBEvent.BrickHitBottom> {
                targetState = BBState.LifeLost
                onTriggered { _phase.value = BrickBreakerPhase.LIFE_LOST }
            }
            transition<BBEvent.AllLivesLost> {
                targetState = BBState.GameOver
                onTriggered { _phase.value = BrickBreakerPhase.GAME_OVER }
            }
            transition<BBEvent.TimerExpired> {
                targetState = BBState.GameOver
                onTriggered { _phase.value = BrickBreakerPhase.GAME_OVER }
            }
            transition<BBEvent.LevelRowsCleared> {
                targetState = BBState.LevelComplete
                onTriggered { _phase.value = BrickBreakerPhase.LEVEL_COMPLETE }
            }
            transition<BBEvent.AllTargetsDestroyed> {
                targetState = BBState.LevelComplete
                onTriggered { _phase.value = BrickBreakerPhase.LEVEL_COMPLETE }
            }
            transition<BBEvent.FieldCleared> {
                targetState = BBState.LevelComplete
                onTriggered { _phase.value = BrickBreakerPhase.LEVEL_COMPLETE }
            }
        }

        addState(BBState.LifeLost) {
            transition<BBEvent.RespawnReady> {
                targetState = BBState.Playing
                onTriggered { _phase.value = BrickBreakerPhase.PLAYING }
            }
            transition<BBEvent.AllLivesLost> {
                targetState = BBState.GameOver
                onTriggered { _phase.value = BrickBreakerPhase.GAME_OVER }
            }
        }

        addState(BBState.LevelComplete) {
            transition<BBEvent.NextLevel> {
                targetState = BBState.LevelStart
                onTriggered { _phase.value = BrickBreakerPhase.LEVEL_START }
            }
        }

        addState(BBState.GameOver) {
            transition<BBEvent.Restart> {
                targetState = BBState.Idle
                onTriggered { _phase.value = BrickBreakerPhase.IDLE }
            }
        }
    }

    // Public API — views call these to drive the machine.
    fun levelStarted() = machine.processEventByLaunch(BBEvent.LevelStarted)
    fun readyForAim() = machine.processEventByLaunch(BBEvent.ReadyForAim)
    fun readyForPlay() = machine.processEventByLaunch(BBEvent.ReadyForPlay)
    fun fireTapped() = machine.processEventByLaunch(BBEvent.FireTapped)
    fun allBallsLanded() = machine.processEventByLaunch(BBEvent.AllBallsLanded)
    fun clearTapped() = machine.processEventByLaunch(BBEvent.ClearTapped)
    fun bricksRemain() = machine.processEventByLaunch(BBEvent.BricksRemain)
    fun nextTurnReady() = machine.processEventByLaunch(BBEvent.NextTurnReady)
    fun turnsExhausted() = machine.processEventByLaunch(BBEvent.TurnsExhausted)
    fun fieldCleared() = machine.processEventByLaunch(BBEvent.FieldCleared)
    fun noBricksAtBottom() = machine.processEventByLaunch(BBEvent.NoBricksAtBottom)
    fun bricksAtBottom() = machine.processEventByLaunch(BBEvent.BricksAtBottom)
    fun brickHitBottom() = machine.processEventByLaunch(BBEvent.BrickHitBottom)
    fun allLivesLost() = machine.processEventByLaunch(BBEvent.AllLivesLost)
    fun timerExpired() = machine.processEventByLaunch(BBEvent.TimerExpired)
    fun levelRowsCleared() = machine.processEventByLaunch(BBEvent.LevelRowsCleared)
    fun allTargetsDestroyed() = machine.processEventByLaunch(BBEvent.AllTargetsDestroyed)
    fun respawnReady() = machine.processEventByLaunch(BBEvent.RespawnReady)
    fun nextLevel() = machine.processEventByLaunch(BBEvent.NextLevel)
    fun restart() = machine.processEventByLaunch(BBEvent.Restart)
}
