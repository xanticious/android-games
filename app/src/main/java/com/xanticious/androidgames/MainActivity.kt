package com.xanticious.androidgames

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import com.xanticious.androidgames.controller.LobbyController
import com.xanticious.androidgames.model.GameCatalog
import com.xanticious.androidgames.state.AppScreen
import com.xanticious.androidgames.state.AppStateMachine
import com.xanticious.androidgames.ui.theme.AndroidGamesTheme
import com.xanticious.androidgames.view.actionGameRegistry
import com.xanticious.androidgames.view.AppSettingsView
import com.xanticious.androidgames.view.GameSettingsView
import com.xanticious.androidgames.view.GameStubView
import com.xanticious.androidgames.view.HowToPlayView
import com.xanticious.androidgames.view.LobbyView
import com.xanticious.androidgames.view.ProfilesView
import com.xanticious.androidgames.view.SplashView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidGamesTheme {
                val stateMachine = remember { AppStateMachine() }
                val lobbyController = remember { LobbyController() }
                val screen by stateMachine.screen.collectAsState()

                when (val current = screen) {
                    AppScreen.Splash -> SplashView(onEnterLobby = stateMachine::onSplashFinished)
                    AppScreen.Lobby -> LobbyView(
                        games = GameCatalog.allGames,
                        controller = lobbyController,
                        onOpenProfiles = stateMachine::openProfiles,
                        onOpenSettings = stateMachine::openAppSettings,
                        onOpenGame = stateMachine::openGameSettings
                    )

                    AppScreen.Profiles -> ProfilesView(onBack = stateMachine::backToLobby)
                    AppScreen.AppSettings -> AppSettingsView(onBack = stateMachine::backToLobby)
                    is AppScreen.GameSettings -> GameSettingsView(
                        gameName = gameName(current.gameId),
                        onHowToPlay = stateMachine::openHowToPlay,
                        onStart = stateMachine::startGame,
                        onBack = stateMachine::backToLobby
                    )

                    is AppScreen.HowToPlay -> HowToPlayView(
                        gameName = gameName(current.gameId),
                        onBackToSettings = stateMachine::backToGameSettings,
                        onBackToLobby = stateMachine::backToLobby
                    )

                    is AppScreen.GameStub -> {
                        val game = actionGameRegistry[current.gameId]
                        if (game != null) {
                            game(current.difficulty) { stateMachine.backToGameSettings() }
                        } else {
                            GameStubView(
                                gameName = gameName(current.gameId),
                                onBackToSettings = stateMachine::backToGameSettings,
                                onBackToLobby = stateMachine::backToLobby
                            )
                        }
                    }
                }
            }
        }
    }

    private fun gameName(gameId: String): String =
        GameCatalog.allGames.firstOrNull { it.id == gameId }?.name ?: gameId
}
