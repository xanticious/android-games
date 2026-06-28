package com.xanticious.androidgames.state.games.bombergrid

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
 * High-level Bomber Grid match phases observed by the composable.
 * Mirrors the state diagram in `design/board-games/bomber-grid/bomber-grid-design.md`.
 */
enum class BomberGridPhase {
    IDLE,
    GENERATING_TERRAIN,
    SELECTING_ACTOR,
    MOVE_PHASE,
    AIM_FIRE_PHASE,
    RESOLVING_EXPLOSION,
    RESOLVING_FALLS,
    GAME_OVER
}

private sealed class MatchState : DefaultState() {
    data object Idle : MatchState()
    data object GeneratingTerrain : MatchState()
    data object SelectingActor : MatchState()
    data object MovePhase : MatchState()
    data object AimFirePhase : MatchState()
    data object ResolvingExplosion : MatchState()
    data object ResolvingFalls : MatchState()
    data object GameOver : MatchState()
}

private sealed interface BomberGridEvent : Event {
    data object MatchStarted : BomberGridEvent
    data object TerrainReady : BomberGridEvent
    data object ActorSelected : BomberGridEvent
    data object MovementChanged : BomberGridEvent
    data object MoveConfirmed : BomberGridEvent
    data object AimChanged : BomberGridEvent
    data object BombFired : BomberGridEvent
    data object TerrainSettled : BomberGridEvent
    data object CharacterHit : BomberGridEvent
    data object TeamEliminated : BomberGridEvent
    data object TurnAdvanced : BomberGridEvent
    data object Rematch : BomberGridEvent
    data object GoToMenu : BomberGridEvent
}

/**
 * Drives Bomber Grid's match-level phase transitions.  Game state and physics
 * live in the controller layer; this machine only tracks *which phase* the match
 * is in so the composable can react accordingly.
 *
 * The [scope] is injectable so the machine can be exercised in plain JVM unit
 * tests without the Android main dispatcher.
 *
 * State diagram (condensed):
 * ```
 * Idle ──MatchStarted──► GeneratingTerrain ──TerrainReady──► SelectingActor
 * SelectingActor ──ActorSelected──► MovePhase
 * MovePhase ──MovementChanged──► MovePhase (self)
 * MovePhase ──MoveConfirmed──► AimFirePhase
 * AimFirePhase ──AimChanged──► AimFirePhase (self)
 * AimFirePhase ──BombFired──► ResolvingExplosion
 * ResolvingExplosion ──TerrainSettled / CharacterHit──► ResolvingFalls
 * ResolvingFalls ──TurnAdvanced──► SelectingActor
 * ResolvingFalls ──TeamEliminated──► GameOver
 * GameOver ──Rematch / GoToMenu──► Idle
 * ```
 */
class BomberGridStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(BomberGridPhase.IDLE)
    val phase: StateFlow<BomberGridPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(MatchState.Idle) {
            transition<BomberGridEvent.MatchStarted> {
                targetState = MatchState.GeneratingTerrain
                onTriggered { _phase.value = BomberGridPhase.GENERATING_TERRAIN }
            }
        }
        addState(MatchState.GeneratingTerrain) {
            transition<BomberGridEvent.TerrainReady> {
                targetState = MatchState.SelectingActor
                onTriggered { _phase.value = BomberGridPhase.SELECTING_ACTOR }
            }
        }
        addState(MatchState.SelectingActor) {
            transition<BomberGridEvent.ActorSelected> {
                targetState = MatchState.MovePhase
                onTriggered { _phase.value = BomberGridPhase.MOVE_PHASE }
            }
        }
        addState(MatchState.MovePhase) {
            transition<BomberGridEvent.MovementChanged> {
                targetState = MatchState.MovePhase
                onTriggered { _phase.value = BomberGridPhase.MOVE_PHASE }
            }
            transition<BomberGridEvent.MoveConfirmed> {
                targetState = MatchState.AimFirePhase
                onTriggered { _phase.value = BomberGridPhase.AIM_FIRE_PHASE }
            }
        }
        addState(MatchState.AimFirePhase) {
            transition<BomberGridEvent.AimChanged> {
                targetState = MatchState.AimFirePhase
                onTriggered { _phase.value = BomberGridPhase.AIM_FIRE_PHASE }
            }
            transition<BomberGridEvent.BombFired> {
                targetState = MatchState.ResolvingExplosion
                onTriggered { _phase.value = BomberGridPhase.RESOLVING_EXPLOSION }
            }
        }
        addState(MatchState.ResolvingExplosion) {
            transition<BomberGridEvent.TerrainSettled> {
                targetState = MatchState.ResolvingFalls
                onTriggered { _phase.value = BomberGridPhase.RESOLVING_FALLS }
            }
            transition<BomberGridEvent.CharacterHit> {
                targetState = MatchState.ResolvingFalls
                onTriggered { _phase.value = BomberGridPhase.RESOLVING_FALLS }
            }
        }
        addState(MatchState.ResolvingFalls) {
            transition<BomberGridEvent.TurnAdvanced> {
                targetState = MatchState.SelectingActor
                onTriggered { _phase.value = BomberGridPhase.SELECTING_ACTOR }
            }
            transition<BomberGridEvent.TeamEliminated> {
                targetState = MatchState.GameOver
                onTriggered { _phase.value = BomberGridPhase.GAME_OVER }
            }
        }
        addState(MatchState.GameOver) {
            transition<BomberGridEvent.Rematch> {
                targetState = MatchState.Idle
                onTriggered { _phase.value = BomberGridPhase.IDLE }
            }
            transition<BomberGridEvent.GoToMenu> {
                targetState = MatchState.Idle
                onTriggered { _phase.value = BomberGridPhase.IDLE }
            }
        }
    }

    fun startMatch() = machine.processEventByLaunch(BomberGridEvent.MatchStarted)
    fun terrainReady() = machine.processEventByLaunch(BomberGridEvent.TerrainReady)
    fun actorSelected() = machine.processEventByLaunch(BomberGridEvent.ActorSelected)
    fun movementChanged() = machine.processEventByLaunch(BomberGridEvent.MovementChanged)
    fun moveConfirmed() = machine.processEventByLaunch(BomberGridEvent.MoveConfirmed)
    fun aimChanged() = machine.processEventByLaunch(BomberGridEvent.AimChanged)
    fun bombFired() = machine.processEventByLaunch(BomberGridEvent.BombFired)
    fun terrainSettled() = machine.processEventByLaunch(BomberGridEvent.TerrainSettled)
    fun characterHit() = machine.processEventByLaunch(BomberGridEvent.CharacterHit)
    fun teamEliminated() = machine.processEventByLaunch(BomberGridEvent.TeamEliminated)
    fun turnAdvanced() = machine.processEventByLaunch(BomberGridEvent.TurnAdvanced)
    fun rematch() = machine.processEventByLaunch(BomberGridEvent.Rematch)
    fun goToMenu() = machine.processEventByLaunch(BomberGridEvent.GoToMenu)
}
