package com.xanticious.androidgames.state.games.idleanimalmerge

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
 * High-level Animal Merge game phases observed by the composable.
 *
 * - [IDLE]: title/start screen before the first game begins.
 * - [HOW_TO_PLAY]: scrollable instructions screen.
 * - [PLAYING]: normal gameplay — timer running, merges and releases available.
 * - [ANIMAL_ARRIVED]: a new animal has just spawned and is waiting to be placed.
 * - [FIELD_FULL]: both the 20-slot field and the 3-slot queue are full; the player
 *   must merge or release before the next spawn can be accommodated.
 */
enum class IdleAnimalMergePhase {
    IDLE,
    HOW_TO_PLAY,
    PLAYING,
    ANIMAL_ARRIVED,
    FIELD_FULL
}

private sealed class NavState : DefaultState() {
    data object Idle : NavState()
    data object HowToPlay : NavState()
    data object Playing : NavState()
    data object AnimalArrived : NavState()
    data object FieldFull : NavState()
}

private sealed interface MergeEvent : Event {
    data object StartGame : MergeEvent
    data object OpenHowToPlay : MergeEvent
    data object DismissHowToPlay : MergeEvent
    /** A new animal has arrived; field has space (field or queue). */
    data object HourlySpawn : MergeEvent
    /** Field AND queue are both full when an animal tries to arrive. */
    data object FieldCapacityReached : MergeEvent
    /** The pending arrival was placed or otherwise resolved. */
    data object AnimalPlaced : MergeEvent
    /** A merge or release cleared at least one field slot (while FIELD_FULL). */
    data object SpaceFreed : MergeEvent
}

/**
 * Drives Animal Merge's high-level phase transitions.
 *
 * Animal placement, merge logic, and coin ticking are handled by
 * [com.xanticious.androidgames.controller.games.idleanimalmerge.IdleAnimalMergeController].
 *
 * [scope] is injectable for plain-JVM unit testing without the Android dispatcher.
 */
class IdleAnimalMergeStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(IdleAnimalMergePhase.IDLE)
    val phase: StateFlow<IdleAnimalMergePhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {

        addInitialState(NavState.Idle) {
            transition<MergeEvent.StartGame> {
                targetState = NavState.Playing
                onTriggered { _phase.value = IdleAnimalMergePhase.PLAYING }
            }
            transition<MergeEvent.OpenHowToPlay> {
                targetState = NavState.HowToPlay
                onTriggered { _phase.value = IdleAnimalMergePhase.HOW_TO_PLAY }
            }
        }

        addState(NavState.HowToPlay) {
            transition<MergeEvent.DismissHowToPlay> {
                targetState = NavState.Idle
                onTriggered { _phase.value = IdleAnimalMergePhase.IDLE }
            }
        }

        addState(NavState.Playing) {
            transition<MergeEvent.HourlySpawn> {
                targetState = NavState.AnimalArrived
                onTriggered { _phase.value = IdleAnimalMergePhase.ANIMAL_ARRIVED }
            }
            transition<MergeEvent.FieldCapacityReached> {
                targetState = NavState.FieldFull
                onTriggered { _phase.value = IdleAnimalMergePhase.FIELD_FULL }
            }
            transition<MergeEvent.OpenHowToPlay> {
                targetState = NavState.HowToPlay
                onTriggered { _phase.value = IdleAnimalMergePhase.HOW_TO_PLAY }
            }
        }

        addState(NavState.AnimalArrived) {
            transition<MergeEvent.AnimalPlaced> {
                targetState = NavState.Playing
                onTriggered { _phase.value = IdleAnimalMergePhase.PLAYING }
            }
            transition<MergeEvent.FieldCapacityReached> {
                targetState = NavState.FieldFull
                onTriggered { _phase.value = IdleAnimalMergePhase.FIELD_FULL }
            }
        }

        addState(NavState.FieldFull) {
            transition<MergeEvent.SpaceFreed> {
                targetState = NavState.Playing
                onTriggered { _phase.value = IdleAnimalMergePhase.PLAYING }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(MergeEvent.StartGame)
    fun openHowToPlay() = machine.processEventByLaunch(MergeEvent.OpenHowToPlay)
    fun dismissHowToPlay() = machine.processEventByLaunch(MergeEvent.DismissHowToPlay)
    fun hourlySpawn() = machine.processEventByLaunch(MergeEvent.HourlySpawn)
    fun fieldCapacityReached() = machine.processEventByLaunch(MergeEvent.FieldCapacityReached)
    fun animalPlaced() = machine.processEventByLaunch(MergeEvent.AnimalPlaced)
    fun spaceFreed() = machine.processEventByLaunch(MergeEvent.SpaceFreed)
}
