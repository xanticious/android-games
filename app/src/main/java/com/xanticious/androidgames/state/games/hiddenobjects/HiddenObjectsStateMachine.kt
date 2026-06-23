package com.xanticious.androidgames.state.games.hiddenobjects

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

enum class HiddenObjectsPhase { IDLE, SETUP, HOW_TO_PLAY, GENERATING, PLAYING, HINT_COOLDOWN, SCENE_COMPLETE, GAME_OVER }

private sealed class HiddenObjectsState : DefaultState() {
    data object Idle : HiddenObjectsState()
    data object Setup : HiddenObjectsState()
    data object HowToPlay : HiddenObjectsState()
    data object Generating : HiddenObjectsState()
    data object Playing : HiddenObjectsState()
    data object HintCooldown : HiddenObjectsState()
    data object SceneComplete : HiddenObjectsState()
    data object GameOver : HiddenObjectsState()
}

private sealed interface HiddenObjectsEvent : Event {
    data object StartGame : HiddenObjectsEvent
    data object OpenHowToPlay : HiddenObjectsEvent
    data object BackToSetup : HiddenObjectsEvent
    data object SettingsConfirmed : HiddenObjectsEvent
    data object SceneReady : HiddenObjectsEvent
    data object ObjectFound : HiddenObjectsEvent
    data object AllObjectsFound : HiddenObjectsEvent
    data object HintRequested : HiddenObjectsEvent
    data object CooldownExpired : HiddenObjectsEvent
    data object TimerExpired : HiddenObjectsEvent
    data object NextScene : HiddenObjectsEvent
    data object Retry : HiddenObjectsEvent
    data object BackToMenu : HiddenObjectsEvent
}

class HiddenObjectsStateMachine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _phase = MutableStateFlow(HiddenObjectsPhase.IDLE)
    val phase: StateFlow<HiddenObjectsPhase> = _phase.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(HiddenObjectsState.Idle) {
            transition<HiddenObjectsEvent.StartGame> {
                targetState = HiddenObjectsState.Setup
                onTriggered { _phase.value = HiddenObjectsPhase.SETUP }
            }
        }
        addState(HiddenObjectsState.Setup) {
            transition<HiddenObjectsEvent.OpenHowToPlay> {
                targetState = HiddenObjectsState.HowToPlay
                onTriggered { _phase.value = HiddenObjectsPhase.HOW_TO_PLAY }
            }
            transition<HiddenObjectsEvent.SettingsConfirmed> {
                targetState = HiddenObjectsState.Generating
                onTriggered { _phase.value = HiddenObjectsPhase.GENERATING }
            }
            transition<HiddenObjectsEvent.BackToMenu> {
                targetState = HiddenObjectsState.Idle
                onTriggered { _phase.value = HiddenObjectsPhase.IDLE }
            }
        }
        addState(HiddenObjectsState.HowToPlay) {
            transition<HiddenObjectsEvent.BackToSetup> {
                targetState = HiddenObjectsState.Setup
                onTriggered { _phase.value = HiddenObjectsPhase.SETUP }
            }
        }
        addState(HiddenObjectsState.Generating) {
            transition<HiddenObjectsEvent.SceneReady> {
                targetState = HiddenObjectsState.Playing
                onTriggered { _phase.value = HiddenObjectsPhase.PLAYING }
            }
        }
        addState(HiddenObjectsState.Playing) {
            transition<HiddenObjectsEvent.ObjectFound> {
                targetState = HiddenObjectsState.Playing
                onTriggered { _phase.value = HiddenObjectsPhase.PLAYING }
            }
            transition<HiddenObjectsEvent.HintRequested> {
                targetState = HiddenObjectsState.HintCooldown
                onTriggered { _phase.value = HiddenObjectsPhase.HINT_COOLDOWN }
            }
            transition<HiddenObjectsEvent.AllObjectsFound> {
                targetState = HiddenObjectsState.SceneComplete
                onTriggered { _phase.value = HiddenObjectsPhase.SCENE_COMPLETE }
            }
            transition<HiddenObjectsEvent.TimerExpired> {
                targetState = HiddenObjectsState.GameOver
                onTriggered { _phase.value = HiddenObjectsPhase.GAME_OVER }
            }
        }
        addState(HiddenObjectsState.HintCooldown) {
            transition<HiddenObjectsEvent.CooldownExpired> {
                targetState = HiddenObjectsState.Playing
                onTriggered { _phase.value = HiddenObjectsPhase.PLAYING }
            }
            transition<HiddenObjectsEvent.TimerExpired> {
                targetState = HiddenObjectsState.GameOver
                onTriggered { _phase.value = HiddenObjectsPhase.GAME_OVER }
            }
        }
        addState(HiddenObjectsState.SceneComplete) {
            transition<HiddenObjectsEvent.NextScene> {
                targetState = HiddenObjectsState.Generating
                onTriggered { _phase.value = HiddenObjectsPhase.GENERATING }
            }
            transition<HiddenObjectsEvent.BackToMenu> {
                targetState = HiddenObjectsState.Idle
                onTriggered { _phase.value = HiddenObjectsPhase.IDLE }
            }
        }
        addState(HiddenObjectsState.GameOver) {
            transition<HiddenObjectsEvent.Retry> {
                targetState = HiddenObjectsState.Generating
                onTriggered { _phase.value = HiddenObjectsPhase.GENERATING }
            }
            transition<HiddenObjectsEvent.BackToMenu> {
                targetState = HiddenObjectsState.Idle
                onTriggered { _phase.value = HiddenObjectsPhase.IDLE }
            }
        }
    }

    fun startGame() = machine.processEventByLaunch(HiddenObjectsEvent.StartGame)
    fun openHowToPlay() = machine.processEventByLaunch(HiddenObjectsEvent.OpenHowToPlay)
    fun backToSetup() = machine.processEventByLaunch(HiddenObjectsEvent.BackToSetup)
    fun confirmSettings() = machine.processEventByLaunch(HiddenObjectsEvent.SettingsConfirmed)
    fun sceneReady() = machine.processEventByLaunch(HiddenObjectsEvent.SceneReady)
    fun objectFound() = machine.processEventByLaunch(HiddenObjectsEvent.ObjectFound)
    fun allObjectsFound() = machine.processEventByLaunch(HiddenObjectsEvent.AllObjectsFound)
    fun hintRequested() = machine.processEventByLaunch(HiddenObjectsEvent.HintRequested)
    fun cooldownExpired() = machine.processEventByLaunch(HiddenObjectsEvent.CooldownExpired)
    fun timerExpired() = machine.processEventByLaunch(HiddenObjectsEvent.TimerExpired)
    fun nextScene() = machine.processEventByLaunch(HiddenObjectsEvent.NextScene)
    fun retry() = machine.processEventByLaunch(HiddenObjectsEvent.Retry)
    fun backToMenu() = machine.processEventByLaunch(HiddenObjectsEvent.BackToMenu)
}
