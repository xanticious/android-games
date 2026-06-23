package com.xanticious.androidgames.state.games.empireskirmish

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
 * High-level Empire Skirmish match phases observed by the composable.
 *
 * State diagram (condensed):
 * ```
 * Idle ──BattleStarted──► PlayerTurn
 * PlayerTurn ──TurnEnded──► EnemyTurn
 * PlayerTurn ──KingDied(enemy)──► Victory
 * PlayerTurn ──KingDied(player)──► Defeat
 * EnemyTurn ──EnemyTurnEnded──► PlayerTurn
 * EnemyTurn ──KingDied(player)──► Defeat
 * EnemyTurn ──KingDied(enemy)──► Victory
 * Victory/Defeat ──Replay/Menu──► Idle
 * ```
 */
enum class EmpireSkirmishPhase {
    IDLE,
    PLAYER_TURN,
    ENEMY_TURN,
    VICTORY,
    DEFEAT
}

private sealed class SkirmishState : DefaultState() {
    data object Idle : SkirmishState()
    data object PlayerTurn : SkirmishState()
    data object EnemyTurn : SkirmishState()
    data object Victory : SkirmishState()
    data object Defeat : SkirmishState()
}

private sealed interface SkirmishEvent : Event {
    data object BattleStarted : SkirmishEvent
    data object TurnEnded : SkirmishEvent
    data object EnemyTurnEnded : SkirmishEvent
    data object EnemyKingDied : SkirmishEvent
    data object PlayerKingDied : SkirmishEvent
    data object Replay : SkirmishEvent
    data object GoToMenu : SkirmishEvent
}

/**
 * Drives Empire Skirmish's match-level phase transitions. Game state lives
 * in the controller layer; this machine only tracks which phase the match is in.
 *
 * The [scope] is injectable for plain JVM unit tests.
 */
class EmpireSkirmishStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(EmpireSkirmishPhase.IDLE)
    val phase: StateFlow<EmpireSkirmishPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(SkirmishState.Idle) {
            transition<SkirmishEvent.BattleStarted> {
                targetState = SkirmishState.PlayerTurn
                onTriggered { _phase.value = EmpireSkirmishPhase.PLAYER_TURN }
            }
        }
        addState(SkirmishState.PlayerTurn) {
            transition<SkirmishEvent.TurnEnded> {
                targetState = SkirmishState.EnemyTurn
                onTriggered { _phase.value = EmpireSkirmishPhase.ENEMY_TURN }
            }
            transition<SkirmishEvent.EnemyKingDied> {
                targetState = SkirmishState.Victory
                onTriggered { _phase.value = EmpireSkirmishPhase.VICTORY }
            }
            transition<SkirmishEvent.PlayerKingDied> {
                targetState = SkirmishState.Defeat
                onTriggered { _phase.value = EmpireSkirmishPhase.DEFEAT }
            }
        }
        addState(SkirmishState.EnemyTurn) {
            transition<SkirmishEvent.EnemyTurnEnded> {
                targetState = SkirmishState.PlayerTurn
                onTriggered { _phase.value = EmpireSkirmishPhase.PLAYER_TURN }
            }
            transition<SkirmishEvent.PlayerKingDied> {
                targetState = SkirmishState.Defeat
                onTriggered { _phase.value = EmpireSkirmishPhase.DEFEAT }
            }
            transition<SkirmishEvent.EnemyKingDied> {
                targetState = SkirmishState.Victory
                onTriggered { _phase.value = EmpireSkirmishPhase.VICTORY }
            }
        }
        addState(SkirmishState.Victory) {
            transition<SkirmishEvent.Replay> {
                targetState = SkirmishState.Idle
                onTriggered { _phase.value = EmpireSkirmishPhase.IDLE }
            }
            transition<SkirmishEvent.GoToMenu> {
                targetState = SkirmishState.Idle
                onTriggered { _phase.value = EmpireSkirmishPhase.IDLE }
            }
        }
        addState(SkirmishState.Defeat) {
            transition<SkirmishEvent.Replay> {
                targetState = SkirmishState.Idle
                onTriggered { _phase.value = EmpireSkirmishPhase.IDLE }
            }
            transition<SkirmishEvent.GoToMenu> {
                targetState = SkirmishState.Idle
                onTriggered { _phase.value = EmpireSkirmishPhase.IDLE }
            }
        }
    }

    fun startBattle() = machine.processEventByLaunch(SkirmishEvent.BattleStarted)
    fun endPlayerTurn() = machine.processEventByLaunch(SkirmishEvent.TurnEnded)
    fun endEnemyTurn() = machine.processEventByLaunch(SkirmishEvent.EnemyTurnEnded)
    fun enemyKingDied() = machine.processEventByLaunch(SkirmishEvent.EnemyKingDied)
    fun playerKingDied() = machine.processEventByLaunch(SkirmishEvent.PlayerKingDied)
    fun replay() = machine.processEventByLaunch(SkirmishEvent.Replay)
    fun goToMenu() = machine.processEventByLaunch(SkirmishEvent.GoToMenu)
}
