package com.xanticious.androidgames.state.games.idleplayerpiano

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
 * High-level Player Piano game phases observed by the composable.
 *
 * - [IDLE]: title / start screen.
 * - [HOW_TO_PLAY]: instructions screen.
 * - [PLAYING]: piano is running autonomously; upgrade menu is accessible inline.
 * - [SEQUENCE_MATCHED]: brief celebration state after a full sequence match.
 * - [UPGRADE_MENU_OPEN]: upgrade purchase panel is expanded.
 */
enum class IdlePlayerPianoPhase {
    IDLE,
    HOW_TO_PLAY,
    PLAYING,
    SEQUENCE_MATCHED,
    UPGRADE_MENU_OPEN
}

private sealed class NavState : DefaultState() {
    data object Idle : NavState()
    data object HowToPlay : NavState()
    data object Playing : NavState()
    data object SequenceMatched : NavState()
    data object UpgradeMenuOpen : NavState()
}

private sealed interface PianoEvent : Event {
    data object StartGame : PianoEvent
    data object OpenHowToPlay : PianoEvent
    data object DismissHowToPlay : PianoEvent
    /** The piano successfully completed a full sequence match. */
    data object SequenceCompleted : PianoEvent
    /** Celebration animation finished; resume normal play. */
    data object CelebrationDismissed : PianoEvent
    /** Player opened the upgrade purchase panel. */
    data object UpgradePurchased : PianoEvent
    /** Player opened the upgrade menu. */
    data object OpenUpgradeMenu : PianoEvent
    /** Player closed the upgrade menu. */
    data object CloseUpgradeMenu : PianoEvent
}

/**
 * Drives Player Piano's high-level phase transitions.
 *
 * Note selection, bias math, and coin calculations live in
 * [com.xanticious.androidgames.controller.games.idleplayerpiano.IdlePlayerPianoController].
 *
 * [scope] is injectable for plain-JVM unit testing.
 */
class IdlePlayerPianoStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(IdlePlayerPianoPhase.IDLE)
    val phase: StateFlow<IdlePlayerPianoPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {

        addInitialState(NavState.Idle) {
            transition<PianoEvent.StartGame> {
                targetState = NavState.Playing
                onTriggered { _phase.value = IdlePlayerPianoPhase.PLAYING }
            }
            transition<PianoEvent.OpenHowToPlay> {
                targetState = NavState.HowToPlay
                onTriggered { _phase.value = IdlePlayerPianoPhase.HOW_TO_PLAY }
            }
        }

        addState(NavState.HowToPlay) {
            transition<PianoEvent.DismissHowToPlay> {
                targetState = NavState.Idle
                onTriggered { _phase.value = IdlePlayerPianoPhase.IDLE }
            }
        }

        addState(NavState.Playing) {
            transition<PianoEvent.SequenceCompleted> {
                targetState = NavState.SequenceMatched
                onTriggered { _phase.value = IdlePlayerPianoPhase.SEQUENCE_MATCHED }
            }
            transition<PianoEvent.OpenUpgradeMenu> {
                targetState = NavState.UpgradeMenuOpen
                onTriggered { _phase.value = IdlePlayerPianoPhase.UPGRADE_MENU_OPEN }
            }
            transition<PianoEvent.OpenHowToPlay> {
                targetState = NavState.HowToPlay
                onTriggered { _phase.value = IdlePlayerPianoPhase.HOW_TO_PLAY }
            }
        }

        addState(NavState.SequenceMatched) {
            transition<PianoEvent.CelebrationDismissed> {
                targetState = NavState.Playing
                onTriggered { _phase.value = IdlePlayerPianoPhase.PLAYING }
            }
        }

        addState(NavState.UpgradeMenuOpen) {
            transition<PianoEvent.UpgradePurchased> {
                targetState = NavState.Playing
                onTriggered { _phase.value = IdlePlayerPianoPhase.PLAYING }
            }
            transition<PianoEvent.CloseUpgradeMenu> {
                targetState = NavState.Playing
                onTriggered { _phase.value = IdlePlayerPianoPhase.PLAYING }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(PianoEvent.StartGame)
    fun openHowToPlay() = machine.processEventByLaunch(PianoEvent.OpenHowToPlay)
    fun dismissHowToPlay() = machine.processEventByLaunch(PianoEvent.DismissHowToPlay)
    fun sequenceCompleted() = machine.processEventByLaunch(PianoEvent.SequenceCompleted)
    fun celebrationDismissed() = machine.processEventByLaunch(PianoEvent.CelebrationDismissed)
    fun upgradePurchased() = machine.processEventByLaunch(PianoEvent.UpgradePurchased)
    fun openUpgradeMenu() = machine.processEventByLaunch(PianoEvent.OpenUpgradeMenu)
    fun closeUpgradeMenu() = machine.processEventByLaunch(PianoEvent.CloseUpgradeMenu)
}
