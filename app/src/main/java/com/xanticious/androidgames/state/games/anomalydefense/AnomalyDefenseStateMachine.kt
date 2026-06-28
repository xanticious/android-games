package com.xanticious.androidgames.state.games.anomalydefense

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
 * High-level Anomaly Defense phases observed by the composable.
 *
 * IDLE → LEVEL_LOADED → RECON → PLANNING → ASSAULT → VICTORY / DEFEAT → IDLE
 */
enum class AnomalyDefensePhase {
    IDLE,
    LEVEL_LOADED,
    RECON,
    PLANNING,
    ASSAULT,
    VICTORY,
    DEFEAT
}

private sealed class AnomalyState : DefaultState() {
    data object Idle : AnomalyState()
    data object LevelLoaded : AnomalyState()
    data object Recon : AnomalyState()
    data object Planning : AnomalyState()
    data object Assault : AnomalyState()
    data object Victory : AnomalyState()
    data object Defeat : AnomalyState()
}

private sealed interface AnomalyEvent : Event {
    data object LoadLevel : AnomalyEvent
    data object StartRecon : AnomalyEvent
    data object PlanningStarted : AnomalyEvent
    data object UnitBought : AnomalyEvent
    data object UnitAssigned : AnomalyEvent
    data object Committed : AnomalyEvent
    data object AssaultTick : AnomalyEvent
    data object ObjectiveReached : AnomalyEvent
    data object ForceWiped : AnomalyEvent
    data object Retry : AnomalyEvent
    data object Menu : AnomalyEvent
}

/**
 * Drives Anomaly Defense phase transitions. Game state (units, turrets, budget)
 * lives in the controller layer; this machine only tracks the current [phase].
 *
 * State diagram:
 * ```
 * Idle ──LoadLevel──► LevelLoaded ──StartRecon──► Recon
 * Recon ──PlanningStarted──► Planning
 * Planning ──UnitBought──► Planning (self)
 * Planning ──UnitAssigned──► Planning (self)
 * Planning ──Committed──► Assault
 * Assault ──AssaultTick──► Assault (self)
 * Assault ──ObjectiveReached──► Victory
 * Assault ──ForceWiped──► Defeat
 * Victory ──Retry / Menu──► Idle
 * Defeat  ──Retry / Menu──► Idle
 * ```
 */
class AnomalyDefenseStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(AnomalyDefensePhase.IDLE)
    val phase: StateFlow<AnomalyDefensePhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(AnomalyState.Idle) {
            transition<AnomalyEvent.LoadLevel> {
                targetState = AnomalyState.LevelLoaded
                onTriggered { _phase.value = AnomalyDefensePhase.LEVEL_LOADED }
            }
        }
        addState(AnomalyState.LevelLoaded) {
            transition<AnomalyEvent.StartRecon> {
                targetState = AnomalyState.Recon
                onTriggered { _phase.value = AnomalyDefensePhase.RECON }
            }
        }
        addState(AnomalyState.Recon) {
            transition<AnomalyEvent.PlanningStarted> {
                targetState = AnomalyState.Planning
                onTriggered { _phase.value = AnomalyDefensePhase.PLANNING }
            }
        }
        addState(AnomalyState.Planning) {
            transition<AnomalyEvent.UnitBought> {
                targetState = AnomalyState.Planning
                onTriggered { _phase.value = AnomalyDefensePhase.PLANNING }
            }
            transition<AnomalyEvent.UnitAssigned> {
                targetState = AnomalyState.Planning
                onTriggered { _phase.value = AnomalyDefensePhase.PLANNING }
            }
            transition<AnomalyEvent.Committed> {
                targetState = AnomalyState.Assault
                onTriggered { _phase.value = AnomalyDefensePhase.ASSAULT }
            }
        }
        addState(AnomalyState.Assault) {
            transition<AnomalyEvent.AssaultTick> {
                targetState = AnomalyState.Assault
                onTriggered { _phase.value = AnomalyDefensePhase.ASSAULT }
            }
            transition<AnomalyEvent.ObjectiveReached> {
                targetState = AnomalyState.Victory
                onTriggered { _phase.value = AnomalyDefensePhase.VICTORY }
            }
            transition<AnomalyEvent.ForceWiped> {
                targetState = AnomalyState.Defeat
                onTriggered { _phase.value = AnomalyDefensePhase.DEFEAT }
            }
        }
        addState(AnomalyState.Victory) {
            transition<AnomalyEvent.Retry> {
                targetState = AnomalyState.Idle
                onTriggered { _phase.value = AnomalyDefensePhase.IDLE }
            }
            transition<AnomalyEvent.Menu> {
                targetState = AnomalyState.Idle
                onTriggered { _phase.value = AnomalyDefensePhase.IDLE }
            }
        }
        addState(AnomalyState.Defeat) {
            transition<AnomalyEvent.Retry> {
                targetState = AnomalyState.Idle
                onTriggered { _phase.value = AnomalyDefensePhase.IDLE }
            }
            transition<AnomalyEvent.Menu> {
                targetState = AnomalyState.Idle
                onTriggered { _phase.value = AnomalyDefensePhase.IDLE }
            }
        }
    }

    fun loadLevel() = machine.processEventByLaunch(AnomalyEvent.LoadLevel)
    fun startRecon() = machine.processEventByLaunch(AnomalyEvent.StartRecon)
    fun planningStarted() = machine.processEventByLaunch(AnomalyEvent.PlanningStarted)
    fun unitBought() = machine.processEventByLaunch(AnomalyEvent.UnitBought)
    fun unitAssigned() = machine.processEventByLaunch(AnomalyEvent.UnitAssigned)
    fun committed() = machine.processEventByLaunch(AnomalyEvent.Committed)
    fun assaultTick() = machine.processEventByLaunch(AnomalyEvent.AssaultTick)
    fun objectiveReached() = machine.processEventByLaunch(AnomalyEvent.ObjectiveReached)
    fun forceWiped() = machine.processEventByLaunch(AnomalyEvent.ForceWiped)
    fun retry() = machine.processEventByLaunch(AnomalyEvent.Retry)
    fun goToMenu() = machine.processEventByLaunch(AnomalyEvent.Menu)
}
