package com.xanticious.androidgames.state.games.roguecaverns

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
 * High-level Rogue Caverns phases observed by the composable.
 *
 * IDLE → META_HUB → EXPLORING ↔ COMBAT → RUN_RESULTS → META_HUB
 */
enum class RogueCavernsPhase {
    IDLE,
    META_HUB,
    EXPLORING,
    COMBAT,
    RUN_RESULTS
}

private sealed class NavState : DefaultState() {
    data object Idle : NavState()
    data object MetaHub : NavState()
    data object Exploring : NavState()
    data object Combat : NavState()
    data object RunResults : NavState()
}

private sealed interface RogueCavernsEvent : Event {
    data object HubOpened : RogueCavernsEvent
    data object UpgradePurchased : RogueCavernsEvent
    data object RunStarted : RogueCavernsEvent
    data object MovedRoom : RogueCavernsEvent
    data object DescendedLevel : RogueCavernsEvent
    data object EncounterStarted : RogueCavernsEvent
    data object BankedAndExited : RogueCavernsEvent
    data object HeroFainted : RogueCavernsEvent
    data object TurnTaken : RogueCavernsEvent
    data object MonsterDefeated : RogueCavernsEvent
    data object Continued : RogueCavernsEvent
}

/**
 * Drives Rogue Caverns phase transitions.  All game state and logic live in the
 * controller layer; this machine only tracks *which phase* is active so the
 * composable can react accordingly.
 *
 * State diagram:
 * ```
 * Idle ──HubOpened──► MetaHub
 * MetaHub ──UpgradePurchased──► MetaHub (self)
 * MetaHub ──RunStarted──► Exploring
 * Exploring ──MovedRoom──► Exploring (self)
 * Exploring ──DescendedLevel──► Exploring (self)
 * Exploring ──EncounterStarted──► Combat
 * Exploring ──BankedAndExited / HeroFainted──► RunResults
 * Combat ──TurnTaken──► Combat (self)
 * Combat ──MonsterDefeated──► Exploring
 * Combat ──HeroFainted──► RunResults
 * RunResults ──Continued──► MetaHub
 * ```
 */
class RogueCavernsStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(RogueCavernsPhase.IDLE)
    val phase: StateFlow<RogueCavernsPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(NavState.Idle) {
            transition<RogueCavernsEvent.HubOpened> {
                targetState = NavState.MetaHub
                onTriggered { _phase.value = RogueCavernsPhase.META_HUB }
            }
        }
        addState(NavState.MetaHub) {
            transition<RogueCavernsEvent.UpgradePurchased> {
                targetState = NavState.MetaHub
                onTriggered { _phase.value = RogueCavernsPhase.META_HUB }
            }
            transition<RogueCavernsEvent.RunStarted> {
                targetState = NavState.Exploring
                onTriggered { _phase.value = RogueCavernsPhase.EXPLORING }
            }
        }
        addState(NavState.Exploring) {
            transition<RogueCavernsEvent.MovedRoom> {
                targetState = NavState.Exploring
                onTriggered { _phase.value = RogueCavernsPhase.EXPLORING }
            }
            transition<RogueCavernsEvent.DescendedLevel> {
                targetState = NavState.Exploring
                onTriggered { _phase.value = RogueCavernsPhase.EXPLORING }
            }
            transition<RogueCavernsEvent.EncounterStarted> {
                targetState = NavState.Combat
                onTriggered { _phase.value = RogueCavernsPhase.COMBAT }
            }
            transition<RogueCavernsEvent.BankedAndExited> {
                targetState = NavState.RunResults
                onTriggered { _phase.value = RogueCavernsPhase.RUN_RESULTS }
            }
            transition<RogueCavernsEvent.HeroFainted> {
                targetState = NavState.RunResults
                onTriggered { _phase.value = RogueCavernsPhase.RUN_RESULTS }
            }
        }
        addState(NavState.Combat) {
            transition<RogueCavernsEvent.TurnTaken> {
                targetState = NavState.Combat
                onTriggered { _phase.value = RogueCavernsPhase.COMBAT }
            }
            transition<RogueCavernsEvent.MonsterDefeated> {
                targetState = NavState.Exploring
                onTriggered { _phase.value = RogueCavernsPhase.EXPLORING }
            }
            transition<RogueCavernsEvent.HeroFainted> {
                targetState = NavState.RunResults
                onTriggered { _phase.value = RogueCavernsPhase.RUN_RESULTS }
            }
        }
        addState(NavState.RunResults) {
            transition<RogueCavernsEvent.Continued> {
                targetState = NavState.MetaHub
                onTriggered { _phase.value = RogueCavernsPhase.META_HUB }
            }
        }
    }

    fun hubOpened() = machine.processEventByLaunch(RogueCavernsEvent.HubOpened)
    fun upgradePurchased() = machine.processEventByLaunch(RogueCavernsEvent.UpgradePurchased)
    fun runStarted() = machine.processEventByLaunch(RogueCavernsEvent.RunStarted)
    fun movedRoom() = machine.processEventByLaunch(RogueCavernsEvent.MovedRoom)
    fun descendedLevel() = machine.processEventByLaunch(RogueCavernsEvent.DescendedLevel)
    fun encounterStarted() = machine.processEventByLaunch(RogueCavernsEvent.EncounterStarted)
    fun bankedAndExited() = machine.processEventByLaunch(RogueCavernsEvent.BankedAndExited)
    fun heroFainted() = machine.processEventByLaunch(RogueCavernsEvent.HeroFainted)
    fun turnTaken() = machine.processEventByLaunch(RogueCavernsEvent.TurnTaken)
    fun monsterDefeated() = machine.processEventByLaunch(RogueCavernsEvent.MonsterDefeated)
    fun continued() = machine.processEventByLaunch(RogueCavernsEvent.Continued)
}
