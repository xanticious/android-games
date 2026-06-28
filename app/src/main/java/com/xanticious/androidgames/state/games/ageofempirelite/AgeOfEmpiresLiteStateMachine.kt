package com.xanticious.androidgames.state.games.ageofempirelite

import com.xanticious.androidgames.controller.games.ageofempirelite.AgeOfEmpiresLiteController
import com.xanticious.androidgames.model.games.ageofempirelite.AgeOfEmpiresLiteState
import com.xanticious.androidgames.model.games.ageofempirelite.ArmyComposition
import com.xanticious.androidgames.model.games.ageofempirelite.EconomyBalance
import com.xanticious.androidgames.model.games.ageofempirelite.MatchSettings
import com.xanticious.androidgames.model.games.ageofempirelite.UpgradePriority
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

/** High-level phases observed by the composable. */
enum class AgeOfEmpiresLitePhase { IDLE, PLAYING, VICTORY, DEFEAT }

private sealed class MatchState : DefaultState() {
    data object Idle : MatchState()
    data object Playing : MatchState()
    data object Victory : MatchState()
    data object Defeat : MatchState()
}

private sealed interface MatchEvent : Event {
    data object MatchStarted : MatchEvent
    data object PlayerKingDestroyed : MatchEvent
    data object EnemyKingDestroyed : MatchEvent
    data object PlayerEnlightenmentCompleted : MatchEvent
    data object EnemyEnlightenmentCompleted : MatchEvent
    data object PlayerPloughsharesReached : MatchEvent
    data object EnemyPloughsharesReached : MatchEvent
    data object RestartOrMenu : MatchEvent
}

/**
 * Drives Age of Empires Lite phase transitions.
 *
 * Game state ([AgeOfEmpiresLiteState]) lives in [gameState] and is updated
 * directly by [tick] and [setPolicy] so the real-time game loop does not need
 * to route through KStateMachine for every frame.
 */
class AgeOfEmpiresLiteStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val controller = AgeOfEmpiresLiteController()

    private val _phase = MutableStateFlow(AgeOfEmpiresLitePhase.IDLE)
    val phase: StateFlow<AgeOfEmpiresLitePhase> = _phase.asStateFlow()

    private val _gameState = MutableStateFlow<AgeOfEmpiresLiteState?>(null)
    val gameState: StateFlow<AgeOfEmpiresLiteState?> = _gameState.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(MatchState.Idle) {
            transition<MatchEvent.MatchStarted> {
                targetState = MatchState.Playing
                onTriggered { _phase.value = AgeOfEmpiresLitePhase.PLAYING }
            }
        }
        addState(MatchState.Playing) {
            transition<MatchEvent.EnemyKingDestroyed> {
                targetState = MatchState.Victory
                onTriggered { _phase.value = AgeOfEmpiresLitePhase.VICTORY }
            }
            transition<MatchEvent.PlayerEnlightenmentCompleted> {
                targetState = MatchState.Victory
                onTriggered { _phase.value = AgeOfEmpiresLitePhase.VICTORY }
            }
            transition<MatchEvent.PlayerPloughsharesReached> {
                targetState = MatchState.Victory
                onTriggered { _phase.value = AgeOfEmpiresLitePhase.VICTORY }
            }
            transition<MatchEvent.PlayerKingDestroyed> {
                targetState = MatchState.Defeat
                onTriggered { _phase.value = AgeOfEmpiresLitePhase.DEFEAT }
            }
            transition<MatchEvent.EnemyEnlightenmentCompleted> {
                targetState = MatchState.Defeat
                onTriggered { _phase.value = AgeOfEmpiresLitePhase.DEFEAT }
            }
            transition<MatchEvent.EnemyPloughsharesReached> {
                targetState = MatchState.Defeat
                onTriggered { _phase.value = AgeOfEmpiresLitePhase.DEFEAT }
            }
        }
        addState(MatchState.Victory) {
            transition<MatchEvent.RestartOrMenu> {
                targetState = MatchState.Idle
                onTriggered { _phase.value = AgeOfEmpiresLitePhase.IDLE }
            }
        }
        addState(MatchState.Defeat) {
            transition<MatchEvent.RestartOrMenu> {
                targetState = MatchState.Idle
                onTriggered { _phase.value = AgeOfEmpiresLitePhase.IDLE }
            }
        }
    }

    fun startMatch(settings: MatchSettings) {
        _gameState.value = controller.initialState(settings)
        machine.processEventByLaunch(MatchEvent.MatchStarted)
    }

    /** Advances the simulation by [dt] seconds and fires terminal events when needed. */
    fun tick(dt: Float) {
        val current = _gameState.value ?: return
        if (_phase.value != AgeOfEmpiresLitePhase.PLAYING) return

        val next = controller.tick(current, dt)
        _gameState.value = next

        val outcome = controller.checkVictoryConditions(next)
        when (outcome) {
            AgeOfEmpiresLiteController.Outcome.ENEMY_KING_DEAD ->
                machine.processEventByLaunch(MatchEvent.EnemyKingDestroyed)
            AgeOfEmpiresLiteController.Outcome.PLAYER_KING_DEAD ->
                machine.processEventByLaunch(MatchEvent.PlayerKingDestroyed)
            AgeOfEmpiresLiteController.Outcome.PLAYER_ENLIGHTENMENT ->
                machine.processEventByLaunch(MatchEvent.PlayerEnlightenmentCompleted)
            AgeOfEmpiresLiteController.Outcome.ENEMY_ENLIGHTENMENT ->
                machine.processEventByLaunch(MatchEvent.EnemyEnlightenmentCompleted)
            AgeOfEmpiresLiteController.Outcome.PLAYER_PLOUGHSHARES ->
                machine.processEventByLaunch(MatchEvent.PlayerPloughsharesReached)
            AgeOfEmpiresLiteController.Outcome.ENEMY_PLOUGHSHARES ->
                machine.processEventByLaunch(MatchEvent.EnemyPloughsharesReached)
            null -> Unit
        }
    }

    /** Updates player economy/army/upgrade policy without changing phase. */
    fun setPolicy(
        economy: EconomyBalance,
        army: ArmyComposition,
        upgrades: UpgradePriority,
        additionalCannonQueued: Int = 0
    ) {
        val current = _gameState.value ?: return
        _gameState.value = current.copy(
            settings = current.settings.copy(
                economy = economy,
                army = army,
                upgrades = upgrades
            ),
            playerCannonQueue = current.playerCannonQueue + additionalCannonQueued
        )
    }

    fun returnToIdle() = machine.processEventByLaunch(MatchEvent.RestartOrMenu)
}
