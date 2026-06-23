package com.xanticious.androidgames.state.games.spades

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
 * High-level Spades match phases observed by the composable.
 *
 * Trick-by-trick resolution and card-play sequencing are managed by the view
 * layer reacting to [SpadesGameState] changes — this machine only tracks which
 * coarse phase the game is in.
 */
enum class SpadesPhase { IDLE, DEALING, BIDDING, PLAYING, HAND_SCORED, GAME_OVER }

private sealed class SpadesMachineState : DefaultState() {
    data object Idle        : SpadesMachineState()
    data object Dealing     : SpadesMachineState()
    data object Bidding     : SpadesMachineState()
    data object Playing     : SpadesMachineState()
    data object HandScored  : SpadesMachineState()
    data object GameOver    : SpadesMachineState()
}

private sealed interface SpadesMachineEvent : Event {
    data object StartGame      : SpadesMachineEvent
    data object Dealt          : SpadesMachineEvent
    data object AllBidsIn      : SpadesMachineEvent
    data object HandComplete   : SpadesMachineEvent
    data object NextHand       : SpadesMachineEvent
    data object GameEndReached : SpadesMachineEvent
    data object Rematch        : SpadesMachineEvent
}

/**
 * Drives Spades high-level phase transitions. Game rules live in
 * [com.xanticious.androidgames.controller.games.spades.SpadesController];
 * this machine only tracks which phase the match is in.
 *
 * The [scope] is injectable so the machine can be exercised in plain JVM unit
 * tests without the Android main dispatcher.
 */
class SpadesStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(SpadesPhase.IDLE)
    val phase: StateFlow<SpadesPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(SpadesMachineState.Idle) {
            transition<SpadesMachineEvent.StartGame> {
                targetState = SpadesMachineState.Dealing
                onTriggered { _phase.value = SpadesPhase.DEALING }
            }
        }
        addState(SpadesMachineState.Dealing) {
            transition<SpadesMachineEvent.Dealt> {
                targetState = SpadesMachineState.Bidding
                onTriggered { _phase.value = SpadesPhase.BIDDING }
            }
        }
        addState(SpadesMachineState.Bidding) {
            transition<SpadesMachineEvent.AllBidsIn> {
                targetState = SpadesMachineState.Playing
                onTriggered { _phase.value = SpadesPhase.PLAYING }
            }
        }
        addState(SpadesMachineState.Playing) {
            transition<SpadesMachineEvent.HandComplete> {
                targetState = SpadesMachineState.HandScored
                onTriggered { _phase.value = SpadesPhase.HAND_SCORED }
            }
        }
        addState(SpadesMachineState.HandScored) {
            transition<SpadesMachineEvent.NextHand> {
                targetState = SpadesMachineState.Dealing
                onTriggered { _phase.value = SpadesPhase.DEALING }
            }
            transition<SpadesMachineEvent.GameEndReached> {
                targetState = SpadesMachineState.GameOver
                onTriggered { _phase.value = SpadesPhase.GAME_OVER }
            }
        }
        addState(SpadesMachineState.GameOver) {
            transition<SpadesMachineEvent.Rematch> {
                targetState = SpadesMachineState.Dealing
                onTriggered { _phase.value = SpadesPhase.DEALING }
            }
        }
    }

    fun startGame()      = machine.processEventByLaunch(SpadesMachineEvent.StartGame)
    fun dealt()          = machine.processEventByLaunch(SpadesMachineEvent.Dealt)
    fun allBidsIn()      = machine.processEventByLaunch(SpadesMachineEvent.AllBidsIn)
    fun handComplete()   = machine.processEventByLaunch(SpadesMachineEvent.HandComplete)
    fun nextHand()       = machine.processEventByLaunch(SpadesMachineEvent.NextHand)
    fun gameEndReached() = machine.processEventByLaunch(SpadesMachineEvent.GameEndReached)
    fun rematch()        = machine.processEventByLaunch(SpadesMachineEvent.Rematch)
}
