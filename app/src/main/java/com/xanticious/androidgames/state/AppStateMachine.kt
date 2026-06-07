package com.xanticious.androidgames.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.nsk.kstatemachine.statemachine.createStateMachineBlocking
import ru.nsk.kstatemachine.statemachine.processEventByLaunch
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.*
import ru.nsk.kstatemachine.transition.onTriggered

sealed interface AppScreen {
    data object Splash : AppScreen
    data object Lobby : AppScreen
    data object Profiles : AppScreen
    data object AppSettings : AppScreen
    data class GameSettings(val gameId: String) : AppScreen
    data class HowToPlay(val gameId: String) : AppScreen
    data class GameStub(val gameId: String) : AppScreen
}

private sealed class NavState : DefaultState() {
    data object Splash : NavState()
    data object Lobby : NavState()
    data object Profiles : NavState()
    data object AppSettings : NavState()
    data object GameSettings : NavState()
    data object HowToPlay : NavState()
    data object GameStub : NavState()
}

private sealed interface NavEvent : Event {
    data object SplashFinished : NavEvent
    data object OpenProfiles : NavEvent
    data object OpenSettings : NavEvent
    data object OpenGameSettings : NavEvent
    data object OpenHowToPlay : NavEvent
    data object StartGame : NavEvent
    data object BackToLobby : NavEvent
    data object BackToGameSettings : NavEvent
}

class AppStateMachine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var selectedGameId: String = ""

    private val _screen = MutableStateFlow<AppScreen>(AppScreen.Splash)
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()

    private val machine = createStateMachineBlocking(scope = scope) {
        addInitialState(NavState.Splash) {
            transition<NavEvent.SplashFinished> {
                targetState = NavState.Lobby
                onTriggered { _screen.value = AppScreen.Lobby }
            }
        }

        addState(NavState.Lobby) {
            transition<NavEvent.OpenProfiles> {
                targetState = NavState.Profiles
                onTriggered { _screen.value = AppScreen.Profiles }
            }
            transition<NavEvent.OpenSettings> {
                targetState = NavState.AppSettings
                onTriggered { _screen.value = AppScreen.AppSettings }
            }
            transition<NavEvent.OpenGameSettings> {
                targetState = NavState.GameSettings
                onTriggered { _screen.value = AppScreen.GameSettings(selectedGameId) }
            }
        }

        addState(NavState.Profiles) {
            transition<NavEvent.BackToLobby> {
                targetState = NavState.Lobby
                onTriggered { _screen.value = AppScreen.Lobby }
            }
        }

        addState(NavState.AppSettings) {
            transition<NavEvent.BackToLobby> {
                targetState = NavState.Lobby
                onTriggered { _screen.value = AppScreen.Lobby }
            }
        }

        addState(NavState.GameSettings) {
            transition<NavEvent.OpenHowToPlay> {
                targetState = NavState.HowToPlay
                onTriggered { _screen.value = AppScreen.HowToPlay(selectedGameId) }
            }
            transition<NavEvent.StartGame> {
                targetState = NavState.GameStub
                onTriggered { _screen.value = AppScreen.GameStub(selectedGameId) }
            }
            transition<NavEvent.BackToLobby> {
                targetState = NavState.Lobby
                onTriggered { _screen.value = AppScreen.Lobby }
            }
        }

        addState(NavState.HowToPlay) {
            transition<NavEvent.BackToGameSettings> {
                targetState = NavState.GameSettings
                onTriggered { _screen.value = AppScreen.GameSettings(selectedGameId) }
            }
            transition<NavEvent.BackToLobby> {
                targetState = NavState.Lobby
                onTriggered { _screen.value = AppScreen.Lobby }
            }
        }

        addState(NavState.GameStub) {
            transition<NavEvent.BackToGameSettings> {
                targetState = NavState.GameSettings
                onTriggered { _screen.value = AppScreen.GameSettings(selectedGameId) }
            }
            transition<NavEvent.BackToLobby> {
                targetState = NavState.Lobby
                onTriggered { _screen.value = AppScreen.Lobby }
            }
        }
    }

    fun onSplashFinished() = machine.processEventByLaunch(NavEvent.SplashFinished)
    fun openProfiles() = machine.processEventByLaunch(NavEvent.OpenProfiles)
    fun openAppSettings() = machine.processEventByLaunch(NavEvent.OpenSettings)
    fun openGameSettings(gameId: String) {
        selectedGameId = gameId
        machine.processEventByLaunch(NavEvent.OpenGameSettings)
    }

    fun openHowToPlay() = machine.processEventByLaunch(NavEvent.OpenHowToPlay)
    fun startGame() = machine.processEventByLaunch(NavEvent.StartGame)
    fun backToLobby() = machine.processEventByLaunch(NavEvent.BackToLobby)
    fun backToGameSettings() = machine.processEventByLaunch(NavEvent.BackToGameSettings)
}
