package com.xanticious.androidgames.state.games.wordchain

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
import ru.nsk.kstatemachine.statemachine.createStateMachineBlocking
import ru.nsk.kstatemachine.statemachine.processEventByLaunch
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.transition.onTriggered

enum class WordChainPhase { SETUP, HOW_TO_PLAY, PLAYING, CHAIN_BROKEN }

private sealed class ChainState : DefaultState() {
    data object Setup : ChainState()
    data object HowToPlay : ChainState()
    data object Playing : ChainState()
    data object ChainBroken : ChainState()
}

private sealed interface ChainEvent : Event {
    data object StartGame : ChainEvent
    data object ShowHowToPlay : ChainEvent
    data object BackToSetup : ChainEvent
    data object TimeExpired : ChainEvent
    data object Pass : ChainEvent
    data object NewGame : ChainEvent
    data object Exit : ChainEvent
}

class WordChainStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(WordChainPhase.SETUP)
    val phase: StateFlow<WordChainPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(ChainState.Setup) {
            transition<ChainEvent.StartGame> {
                targetState = ChainState.Playing
                onTriggered { _phase.value = WordChainPhase.PLAYING }
            }
            transition<ChainEvent.ShowHowToPlay> {
                targetState = ChainState.HowToPlay
                onTriggered { _phase.value = WordChainPhase.HOW_TO_PLAY }
            }
        }
        addState(ChainState.HowToPlay) {
            transition<ChainEvent.BackToSetup> {
                targetState = ChainState.Setup
                onTriggered { _phase.value = WordChainPhase.SETUP }
            }
        }
        addState(ChainState.Playing) {
            transition<ChainEvent.TimeExpired> {
                targetState = ChainState.ChainBroken
                onTriggered { _phase.value = WordChainPhase.CHAIN_BROKEN }
            }
            transition<ChainEvent.Pass> {
                targetState = ChainState.ChainBroken
                onTriggered { _phase.value = WordChainPhase.CHAIN_BROKEN }
            }
            transition<ChainEvent.Exit> {
                targetState = ChainState.Setup
                onTriggered { _phase.value = WordChainPhase.SETUP }
            }
        }
        addState(ChainState.ChainBroken) {
            transition<ChainEvent.NewGame> {
                targetState = ChainState.Playing
                onTriggered { _phase.value = WordChainPhase.PLAYING }
            }
            transition<ChainEvent.Exit> {
                targetState = ChainState.Setup
                onTriggered { _phase.value = WordChainPhase.SETUP }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(ChainEvent.StartGame)
    fun showHowToPlay() = machine.processEventByLaunch(ChainEvent.ShowHowToPlay)
    fun backToSetup() = machine.processEventByLaunch(ChainEvent.BackToSetup)
    fun timeExpired() = machine.processEventByLaunch(ChainEvent.TimeExpired)
    fun pass() = machine.processEventByLaunch(ChainEvent.Pass)
    fun newGame() = machine.processEventByLaunch(ChainEvent.NewGame)
    fun exit() = machine.processEventByLaunch(ChainEvent.Exit)
}
