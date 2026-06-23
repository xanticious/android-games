package com.xanticious.androidgames.state.games.hiddenobject

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

enum class HiddenObjectPhase { IDLE, SETUP, HOW_TO_PLAY, GENERATING, PLAYING, HINT_COOLDOWN, ROUND_COMPLETE, GAME_OVER }

private sealed class HiddenObjectState : DefaultState() {
    data object Idle : HiddenObjectState()
    data object Setup : HiddenObjectState()
    data object HowToPlay : HiddenObjectState()
    data object Generating : HiddenObjectState()
    data object Playing : HiddenObjectState()
    data object HintCooldown : HiddenObjectState()
    data object RoundComplete : HiddenObjectState()
    data object GameOver : HiddenObjectState()
}

private sealed interface HiddenObjectEvent : Event {
    data object StartGame : HiddenObjectEvent
    data object OpenHowToPlay : HiddenObjectEvent
    data object BackToSetup : HiddenObjectEvent
    data object SettingsConfirmed : HiddenObjectEvent
    data object SceneReady : HiddenObjectEvent
    data object ObjectFound : HiddenObjectEvent
    data object WrongTap : HiddenObjectEvent
    data object HintRequested : HiddenObjectEvent
    data object CooldownExpired : HiddenObjectEvent
    data object TimerExpired : HiddenObjectEvent
    data object NextRound : HiddenObjectEvent
    data object Retry : HiddenObjectEvent
    data object BackToMenu : HiddenObjectEvent
}

class HiddenObjectStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(HiddenObjectPhase.IDLE)
    val phase: StateFlow<HiddenObjectPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(HiddenObjectState.Idle) {
            transition<HiddenObjectEvent.StartGame> {
                targetState = HiddenObjectState.Setup
                onTriggered { _phase.value = HiddenObjectPhase.SETUP }
            }
        }
        addState(HiddenObjectState.Setup) {
            transition<HiddenObjectEvent.OpenHowToPlay> {
                targetState = HiddenObjectState.HowToPlay
                onTriggered { _phase.value = HiddenObjectPhase.HOW_TO_PLAY }
            }
            transition<HiddenObjectEvent.SettingsConfirmed> {
                targetState = HiddenObjectState.Generating
                onTriggered { _phase.value = HiddenObjectPhase.GENERATING }
            }
            transition<HiddenObjectEvent.BackToMenu> {
                targetState = HiddenObjectState.Idle
                onTriggered { _phase.value = HiddenObjectPhase.IDLE }
            }
        }
        addState(HiddenObjectState.HowToPlay) {
            transition<HiddenObjectEvent.BackToSetup> {
                targetState = HiddenObjectState.Setup
                onTriggered { _phase.value = HiddenObjectPhase.SETUP }
            }
        }
        addState(HiddenObjectState.Generating) {
            transition<HiddenObjectEvent.SceneReady> {
                targetState = HiddenObjectState.Playing
                onTriggered { _phase.value = HiddenObjectPhase.PLAYING }
            }
        }
        addState(HiddenObjectState.Playing) {
            transition<HiddenObjectEvent.ObjectFound> {
                targetState = HiddenObjectState.RoundComplete
                onTriggered { _phase.value = HiddenObjectPhase.ROUND_COMPLETE }
            }
            transition<HiddenObjectEvent.WrongTap> {
                targetState = HiddenObjectState.Playing
                onTriggered { _phase.value = HiddenObjectPhase.PLAYING }
            }
            transition<HiddenObjectEvent.HintRequested> {
                targetState = HiddenObjectState.HintCooldown
                onTriggered { _phase.value = HiddenObjectPhase.HINT_COOLDOWN }
            }
            transition<HiddenObjectEvent.TimerExpired> {
                targetState = HiddenObjectState.GameOver
                onTriggered { _phase.value = HiddenObjectPhase.GAME_OVER }
            }
        }
        addState(HiddenObjectState.HintCooldown) {
            transition<HiddenObjectEvent.CooldownExpired> {
                targetState = HiddenObjectState.Playing
                onTriggered { _phase.value = HiddenObjectPhase.PLAYING }
            }
            transition<HiddenObjectEvent.TimerExpired> {
                targetState = HiddenObjectState.GameOver
                onTriggered { _phase.value = HiddenObjectPhase.GAME_OVER }
            }
        }
        addState(HiddenObjectState.RoundComplete) {
            transition<HiddenObjectEvent.NextRound> {
                targetState = HiddenObjectState.Generating
                onTriggered { _phase.value = HiddenObjectPhase.GENERATING }
            }
            transition<HiddenObjectEvent.BackToMenu> {
                targetState = HiddenObjectState.Idle
                onTriggered { _phase.value = HiddenObjectPhase.IDLE }
            }
        }
        addState(HiddenObjectState.GameOver) {
            transition<HiddenObjectEvent.Retry> {
                targetState = HiddenObjectState.Generating
                onTriggered { _phase.value = HiddenObjectPhase.GENERATING }
            }
            transition<HiddenObjectEvent.BackToMenu> {
                targetState = HiddenObjectState.Idle
                onTriggered { _phase.value = HiddenObjectPhase.IDLE }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(HiddenObjectEvent.StartGame)
    fun openHowToPlay() = machine.processEventByLaunch(HiddenObjectEvent.OpenHowToPlay)
    fun backToSetup() = machine.processEventByLaunch(HiddenObjectEvent.BackToSetup)
    fun confirmSettings() = machine.processEventByLaunch(HiddenObjectEvent.SettingsConfirmed)
    fun sceneReady() = machine.processEventByLaunch(HiddenObjectEvent.SceneReady)
    fun objectFound() = machine.processEventByLaunch(HiddenObjectEvent.ObjectFound)
    fun wrongTap() = machine.processEventByLaunch(HiddenObjectEvent.WrongTap)
    fun hintRequested() = machine.processEventByLaunch(HiddenObjectEvent.HintRequested)
    fun cooldownExpired() = machine.processEventByLaunch(HiddenObjectEvent.CooldownExpired)
    fun timerExpired() = machine.processEventByLaunch(HiddenObjectEvent.TimerExpired)
    fun nextRound() = machine.processEventByLaunch(HiddenObjectEvent.NextRound)
    fun retry() = machine.processEventByLaunch(HiddenObjectEvent.Retry)
    fun backToMenu() = machine.processEventByLaunch(HiddenObjectEvent.BackToMenu)
}
