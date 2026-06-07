package com.xanticious.androidgames.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.LobbyController
import com.xanticious.androidgames.model.GameDefinition
import com.xanticious.androidgames.model.LobbyFilter

@Composable
fun SplashView(onEnterLobby: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Android Games", style = MaterialTheme.typography.displaySmall)
        Text(text = "Offline single-player collection", modifier = Modifier.padding(top = 8.dp))
        Button(onClick = onEnterLobby, modifier = Modifier.padding(top = 24.dp)) {
            Text("Enter Lobby")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LobbyView(
    games: List<GameDefinition>,
    controller: LobbyController,
    onOpenProfiles: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGame: (String) -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }
    var onlyReleased by rememberSaveable { mutableStateOf(true) }

    val visibleGames = controller.visibleGames(
        games,
        LobbyFilter(searchQuery = search, onlyFavorites = onlyFavorites, onlyReleased = onlyReleased)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lobby") },
                actions = {
                    Button(onClick = onOpenProfiles) { Text("Profiles") }
                    Button(onClick = onOpenSettings, modifier = Modifier.padding(start = 8.dp)) { Text("Settings") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = search,
                onValueChange = { search = it },
                label = { Text("Search games") },
                singleLine = true
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = onlyFavorites,
                    onClick = { onlyFavorites = !onlyFavorites },
                    label = { Text("Only Favorites") }
                )
                FilterChip(
                    selected = onlyReleased,
                    onClick = { onlyReleased = !onlyReleased },
                    label = { Text("Only Released Games") }
                )
            }

            if (visibleGames.isEmpty()) {
                Text(
                    text = "No games match the selected filters. All games are currently unreleased stubs.",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            LazyVerticalGrid(
                modifier = Modifier.padding(top = 16.dp),
                columns = GridCells.Adaptive(minSize = 180.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleGames) { game ->
                    Card(onClick = { onOpenGame(game.id) }) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(game.name, fontWeight = FontWeight.SemiBold)
                            Text("${game.category}", style = MaterialTheme.typography.bodySmall)
                            Text("Status: Unreleased", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfilesView(onBack: () -> Unit) {
    PageScaffold(title = "Profiles", onBack = onBack) {
        Text("Multiple local profiles (stub).")
        Text("Stats per profile: minutes played, sessions played, wins, difficulties.")
    }
}

@Composable
fun AppSettingsView(onBack: () -> Unit) {
    PageScaffold(title = "App Settings", onBack = onBack) {
        Text("Light/Dark mode toggle (stub)")
        Text("SFX volume / mute (stub)")
        Text("Music volume / mute (stub)")
        Text("Offline-only local storage (stub)")
    }
}

@Composable
fun GameSettingsView(gameName: String, onHowToPlay: () -> Unit, onStart: () -> Unit, onBack: () -> Unit) {
    PageScaffold(title = "$gameName - Settings", onBack = onBack) {
        Text("Per-game options (stub): board size, difficulty, AI players, starter, undo, timer mode.")
        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onHowToPlay) { Text("How to Play") }
            Button(onClick = onStart) { Text("Start Stub Game") }
        }
    }
}

@Composable
fun HowToPlayView(gameName: String, onBackToSettings: () -> Unit, onBackToLobby: () -> Unit) {
    PageScaffold(title = "$gameName - How to Play", onBack = onBackToLobby) {
        Text("Rules and controls will be added later.")
        Button(onClick = onBackToSettings, modifier = Modifier.padding(top = 12.dp)) {
            Text("Back to Game Settings")
        }
    }
}

@Composable
fun GameStubView(gameName: String, onBackToSettings: () -> Unit, onBackToLobby: () -> Unit) {
    PageScaffold(title = gameName, onBack = onBackToLobby) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("$gameName board area (stub)")
        }
        Text(
            text = "Victory / Defeat status area (reserved; never overlays the board)",
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Button(onClick = onBackToSettings, modifier = Modifier.padding(top = 12.dp)) {
            Text("Back to Game Settings")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            content = {
                Button(onClick = onBack) {
                    Text("Back")
                }
                Column(modifier = Modifier.padding(top = 12.dp), content = content)
            }
        )
    }
}
