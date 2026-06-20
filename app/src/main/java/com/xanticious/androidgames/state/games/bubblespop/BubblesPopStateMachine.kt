package com.xanticious.androidgames.state.games.bubblespop

import com.xanticious.androidgames.model.games.bubblespop.BubblesVariant
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
 * High-level gameplay phases shared by all three Bubbles Pop variants.
 * The view collects this flow to decide what to render and which input to accept.
 */
enum class BubblesPopPhase {
    /** Initial: nothing has started yet. */
    IDLE,
    /** Turn-based only: player is dragging to aim the cannon. */
    AIM,
    /** Turn-based only: a bubble is in flight toward the cluster. */
    FIRE,
    /** Arcade / Snake: game running continuously. */
    PLAYING,
    /** Arcade / Snake: one life was lost; brief pause before resuming. */
    LIFE_LOST,
    /** Player cleared the current level / chain. */
    LEVEL_COMPLETE,
    /** All lives exhausted; final state. */
    GAME_OVER,
}

// ─── Internal states ─────────────────────────────────────────────────────────

private sealed class GameState : DefaultState() {
    data object Idle : GameState()
    data object Aim : GameState()
    data object Fire : GameState()
    data object Playing : GameState()
    data object LifeLost : GameState()
    data object LevelComplete : GameState()
    data object GameOver : GameState()
}

// ─── Events ───────────────────────────────────────────────────────────────────

sealed interface BubblesPopEvent : Event {
    /** All variants: start (or restart) from IDLE. */
    data object GameStarted : BubblesPopEvent
    /** Turn-based: player touches the board to start aiming from the AIM phase. */
    data object StartAiming : BubblesPopEvent
    /** Turn-based: player releases / taps Fire. Transitions AIM → FIRE. */
    data object BubbleFired : BubblesPopEvent
    /** Turn-based: flying bubble has attached and been resolved; no level end. */
    data object BubbleResolved : BubblesPopEvent
    /** Any variant: the grid / chain is empty — current level complete. */
    data object ClusterCleared : BubblesPopEvent
    /** Arcade / Snake: a life was lost; game continues. */
    data object LifeLostEvent : BubblesPopEvent
    /** Arcade / Snake: life-lost pause ended; resume play. */
    data object LifeReset : BubblesPopEvent
    /** Any variant: advance to the next level from LEVEL_COMPLETE. */
    data object NextLevel : BubblesPopEvent
    /** Any variant: final game-over condition reached. */
    data object GameOverEvent : BubblesPopEvent
    /** Any variant: player chose to restart from GAME_OVER. */
    data object ResetGame : BubblesPopEvent
}

/**
 * Drives Bubbles Pop phase transitions.
 *
 * The [variant] parameter wires only the transitions relevant to the selected mode:
 * - [BubblesVariant.TURN_BASED] uses IDLE → AIM → FIRE → AIM | LEVEL_COMPLETE | GAME_OVER
 * - [BubblesVariant.ARCADE] and [BubblesVariant.SNAKE_ARCADE] use IDLE → PLAYING → LIFE_LOST → PLAYING | LEVEL_COMPLETE | GAME_OVER
 *
 * The [scope] is injectable so tests can pass [kotlinx.coroutines.CoroutineScope]
 * with [kotlinx.coroutines.Dispatchers.Unconfined].
 */
class BubblesPopStateMachine(
    val variant: BubblesVariant,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    private val _phase = MutableStateFlow(BubblesPopPhase.IDLE)
    val phase: StateFlow<BubblesPopPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(GameState.Idle) {
            transition<BubblesPopEvent.GameStarted> {
                targetState = if (variant == BubblesVariant.TURN_BASED) GameState.Aim else GameState.Playing
                onTriggered {
                    _phase.value = if (variant == BubblesVariant.TURN_BASED) BubblesPopPhase.AIM else BubblesPopPhase.PLAYING
                }
            }
            transition<BubblesPopEvent.ResetGame> {
                targetState = GameState.Idle
                onTriggered { _phase.value = BubblesPopPhase.IDLE }
            }
        }

        // ── Turn-based states ──────────────────────────────────────────────
        addState(GameState.Aim) {
            transition<BubblesPopEvent.BubbleFired> {
                targetState = GameState.Fire
                onTriggered { _phase.value = BubblesPopPhase.FIRE }
            }
            transition<BubblesPopEvent.ClusterCleared> {
                targetState = GameState.LevelComplete
                onTriggered { _phase.value = BubblesPopPhase.LEVEL_COMPLETE }
            }
            transition<BubblesPopEvent.GameOverEvent> {
                targetState = GameState.GameOver
                onTriggered { _phase.value = BubblesPopPhase.GAME_OVER }
            }
        }

        addState(GameState.Fire) {
            transition<BubblesPopEvent.BubbleResolved> {
                targetState = GameState.Aim
                onTriggered { _phase.value = BubblesPopPhase.AIM }
            }
            transition<BubblesPopEvent.ClusterCleared> {
                targetState = GameState.LevelComplete
                onTriggered { _phase.value = BubblesPopPhase.LEVEL_COMPLETE }
            }
            transition<BubblesPopEvent.GameOverEvent> {
                targetState = GameState.GameOver
                onTriggered { _phase.value = BubblesPopPhase.GAME_OVER }
            }
        }

        // ── Arcade / Snake states ──────────────────────────────────────────
        addState(GameState.Playing) {
            transition<BubblesPopEvent.LifeLostEvent> {
                targetState = GameState.LifeLost
                onTriggered { _phase.value = BubblesPopPhase.LIFE_LOST }
            }
            transition<BubblesPopEvent.ClusterCleared> {
                targetState = GameState.LevelComplete
                onTriggered { _phase.value = BubblesPopPhase.LEVEL_COMPLETE }
            }
            transition<BubblesPopEvent.GameOverEvent> {
                targetState = GameState.GameOver
                onTriggered { _phase.value = BubblesPopPhase.GAME_OVER }
            }
        }

        addState(GameState.LifeLost) {
            transition<BubblesPopEvent.LifeReset> {
                targetState = GameState.Playing
                onTriggered { _phase.value = BubblesPopPhase.PLAYING }
            }
            transition<BubblesPopEvent.GameOverEvent> {
                targetState = GameState.GameOver
                onTriggered { _phase.value = BubblesPopPhase.GAME_OVER }
            }
        }

        // ── Shared terminal / completion states ────────────────────────────
        addState(GameState.LevelComplete) {
            transition<BubblesPopEvent.NextLevel> {
                targetState = if (variant == BubblesVariant.TURN_BASED) GameState.Aim else GameState.Playing
                onTriggered {
                    _phase.value = if (variant == BubblesVariant.TURN_BASED) BubblesPopPhase.AIM else BubblesPopPhase.PLAYING
                }
            }
        }

        addState(GameState.GameOver) {
            transition<BubblesPopEvent.ResetGame> {
                targetState = GameState.Idle
                onTriggered { _phase.value = BubblesPopPhase.IDLE }
            }
        }
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    fun startGame() = machine.processEventByLaunch(BubblesPopEvent.GameStarted)
    fun bubbleFired() = machine.processEventByLaunch(BubblesPopEvent.BubbleFired)
    fun bubbleResolved() = machine.processEventByLaunch(BubblesPopEvent.BubbleResolved)
    fun clusterCleared() = machine.processEventByLaunch(BubblesPopEvent.ClusterCleared)
    fun lifeLost() = machine.processEventByLaunch(BubblesPopEvent.LifeLostEvent)
    fun lifeReset() = machine.processEventByLaunch(BubblesPopEvent.LifeReset)
    fun nextLevel() = machine.processEventByLaunch(BubblesPopEvent.NextLevel)
    fun gameOver() = machine.processEventByLaunch(BubblesPopEvent.GameOverEvent)
    fun resetGame() = machine.processEventByLaunch(BubblesPopEvent.ResetGame)
}
