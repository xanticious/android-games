package com.xanticious.androidgames.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.LobbyController
import com.xanticious.androidgames.model.GameCategory
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyView(
    games: List<GameDefinition>,
    controller: LobbyController,
    onOpenProfiles: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGame: (String) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }
    var selectedCategories by remember { mutableStateOf(emptySet<GameCategory>()) }

    val visibleGames = controller.visibleGames(
        games,
        LobbyFilter(
            searchQuery = searchQuery,
            onlyFavorites = onlyFavorites,
            categories = selectedCategories
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenProfiles) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Game Collection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { searchExpanded = !searchExpanded }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (searchExpanded) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search games…") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    } else null
                )
            }

            CategoryFilterRow(
                selectedCategories = selectedCategories,
                onlyFavorites = onlyFavorites,
                onToggleFavorites = { onlyFavorites = !onlyFavorites },
                onToggleCategory = { category ->
                    selectedCategories = if (category in selectedCategories)
                        selectedCategories - category
                    else
                        selectedCategories + category
                },
                onClearAll = {
                    selectedCategories = emptySet()
                    onlyFavorites = false
                }
            )

            if (visibleGames.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No games match the selected filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(visibleGames) { game ->
                        GameCard(game = game, onClick = { onOpenGame(game.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategories: Set<GameCategory>,
    onlyFavorites: Boolean,
    onToggleFavorites: () -> Unit,
    onToggleCategory: (GameCategory) -> Unit,
    onClearAll: () -> Unit
) {
    val hasActiveFilters = selectedCategories.isNotEmpty() || onlyFavorites
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = hasActiveFilters,
            onClick = onClearAll,
            label = {
                Text(
                    text = if (hasActiveFilters) "Clear" else "Filter",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        )
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = onlyFavorites,
                    onClick = onToggleFavorites,
                    label = { Text("Favorites") }
                )
            }
            items(GameCategory.entries.toList()) { category ->
                FilterChip(
                    selected = category in selectedCategories,
                    onClick = { onToggleCategory(category) },
                    label = { Text(category.displayName()) },
                    trailingIcon = if (category in selectedCategories) {
                        {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Remove ${category.displayName()} filter",
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun GameCard(game: GameDefinition, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.aspectRatio(1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(game.category.cardColor())
            )
            Text(
                text = game.category.initial(),
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White.copy(alpha = 0.15f),
                fontWeight = FontWeight.Black
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .padding(start = 4.dp, end = 4.dp, bottom = 4.dp, top = 16.dp)
            ) {
                Column {
                    Text(
                        text = game.category.shortLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                    Text(
                        text = game.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun GameCategory.displayName(): String = when (this) {
    GameCategory.ACTION -> "Action"
    GameCategory.BOARD -> "Board"
    GameCategory.CARD -> "Card"
    GameCategory.DICE -> "Dice"
    GameCategory.EDUCATIONAL -> "Educational"
    GameCategory.EXPLORATION_CREATIVE -> "Exploration"
    GameCategory.IDLE -> "Idle"
    GameCategory.MEMORY -> "Memory"
    GameCategory.PUZZLE -> "Puzzle"
    GameCategory.QUEUE_STRATEGY_ACTION -> "Strategy Action"
    GameCategory.RHYTHM -> "Rhythm"
    GameCategory.RTS -> "RTS"
    GameCategory.SEARCH -> "Search"
    GameCategory.STRATEGY -> "Strategy"
    GameCategory.TOWER_DEFENSE -> "Tower Defense"
    GameCategory.WORD -> "Word"
}

private fun GameCategory.shortLabel(): String = when (this) {
    GameCategory.ACTION -> "ACT"
    GameCategory.BOARD -> "BOARD"
    GameCategory.CARD -> "CARD"
    GameCategory.DICE -> "DICE"
    GameCategory.EDUCATIONAL -> "EDU"
    GameCategory.EXPLORATION_CREATIVE -> "EXPL"
    GameCategory.IDLE -> "IDLE"
    GameCategory.MEMORY -> "MEM"
    GameCategory.PUZZLE -> "PZL"
    GameCategory.QUEUE_STRATEGY_ACTION -> "QSA"
    GameCategory.RHYTHM -> "RHY"
    GameCategory.RTS -> "RTS"
    GameCategory.SEARCH -> "SRCH"
    GameCategory.STRATEGY -> "STRAT"
    GameCategory.TOWER_DEFENSE -> "TD"
    GameCategory.WORD -> "WORD"
}

private fun GameCategory.initial(): String = when (this) {
    GameCategory.ACTION -> "A"
    GameCategory.BOARD -> "B"
    GameCategory.CARD -> "C"
    GameCategory.DICE -> "D"
    GameCategory.EDUCATIONAL -> "E"
    GameCategory.EXPLORATION_CREATIVE -> "X"
    GameCategory.IDLE -> "I"
    GameCategory.MEMORY -> "M"
    GameCategory.PUZZLE -> "P"
    GameCategory.QUEUE_STRATEGY_ACTION -> "Q"
    GameCategory.RHYTHM -> "RY"
    GameCategory.RTS -> "RT"
    GameCategory.SEARCH -> "SC"
    GameCategory.STRATEGY -> "ST"
    GameCategory.TOWER_DEFENSE -> "T"
    GameCategory.WORD -> "W"
}

private fun GameCategory.cardColor(): Color = when (this) {
    GameCategory.ACTION -> Color(0xFFC92A2A)               // Open Color Red 9
    GameCategory.BOARD -> Color(0xFF364FC7)                // Open Color Indigo 9
    GameCategory.CARD -> Color(0xFF862E9C)                 // Open Color Grape 8
    GameCategory.DICE -> Color(0xFF1864AB)                 // Open Color Blue 9
    GameCategory.EDUCATIONAL -> Color(0xFF2B8A3E)          // Open Color Green 9
    GameCategory.EXPLORATION_CREATIVE -> Color(0xFFD9480F) // Open Color Orange 9
    GameCategory.IDLE -> Color(0xFF087F5B)                 // Open Color Teal 9
    GameCategory.MEMORY -> Color(0xFFA61E4D)               // Open Color Pink 9
    GameCategory.PUZZLE -> Color(0xFF0B7285)               // Open Color Cyan 9
    GameCategory.QUEUE_STRATEGY_ACTION -> Color(0xFF5F3DC4) // Open Color Violet 9
    GameCategory.RHYTHM -> Color(0xFFC2255C)               // Open Color Pink 8
    GameCategory.RTS -> Color(0xFF343A40)                  // Open Color Gray 8
    GameCategory.SEARCH -> Color(0xFF099268)               // Open Color Teal 8
    GameCategory.STRATEGY -> Color(0xFF1971C2)             // Open Color Blue 8
    GameCategory.TOWER_DEFENSE -> Color(0xFF2F9E44)        // Open Color Green 8
    GameCategory.WORD -> Color(0xFF6741D9)                 // Open Color Violet 8
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
