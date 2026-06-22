package com.xanticious.androidgames.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xanticious.androidgames.controller.LobbyController
import com.xanticious.androidgames.model.GameCategory
import com.xanticious.androidgames.model.GameDefinition
import com.xanticious.androidgames.model.GameDifficulty
import com.xanticious.androidgames.model.LobbyFilter
import com.xanticious.androidgames.model.LobbyViewMode

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
    viewMode: LobbyViewMode,
    onSetViewMode: (LobbyViewMode) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGame: (String) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }
    var onlyReleased by rememberSaveable { mutableStateOf(true) }
    var selectedCategory by rememberSaveable { mutableStateOf<GameCategory?>(null) }

    val visibleGames = controller.visibleGames(
        games,
        LobbyFilter(
            searchQuery = searchQuery,
            onlyFavorites = onlyFavorites,
            onlyReleased = onlyReleased,
            categories = selectedCategory?.let { setOf(it) } ?: emptySet()
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
                    val nextMode =
                        if (viewMode == LobbyViewMode.TILES) LobbyViewMode.LIST else LobbyViewMode.TILES
                    IconButton(onClick = { onSetViewMode(nextMode) }) {
                        if (viewMode == LobbyViewMode.TILES) {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = "Switch to list view"
                            )
                        } else {
                            Icon(
                                gridViewIcon(),
                                contentDescription = "Switch to tile view"
                            )
                        }
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

            LobbyFilterRow(
                selectedCategory = selectedCategory,
                onSelectCategory = { selectedCategory = it },
                onlyFavorites = onlyFavorites,
                onToggleFavorites = { onlyFavorites = !onlyFavorites },
                onlyReleased = onlyReleased,
                onToggleReleased = { onlyReleased = it }
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
            } else when (viewMode) {
                LobbyViewMode.TILES -> LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(visibleGames) { game ->
                        GameCard(
                            game = game,
                            onClick = { onOpenGame(game.id) },
                            onLongClick = { onToggleFavorite(game.id) }
                        )
                    }
                }

                LobbyViewMode.LIST -> GameListView(
                    games = visibleGames,
                    onClick = { onOpenGame(it) },
                    onLongClick = { onToggleFavorite(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LobbyFilterRow(
    selectedCategory: GameCategory?,
    onSelectCategory: (GameCategory?) -> Unit,
    onlyFavorites: Boolean,
    onToggleFavorites: () -> Unit,
    onlyReleased: Boolean,
    onToggleReleased: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GameTypeDropdown(
            modifier = Modifier.weight(1f),
            selectedCategory = selectedCategory,
            onSelectCategory = onSelectCategory
        )
        FilterChip(
            selected = onlyFavorites,
            onClick = onToggleFavorites,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            label = { Text("Favorites") }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = onlyReleased, onCheckedChange = onToggleReleased)
            Text(text = "Released Only", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameTypeDropdown(
    modifier: Modifier = Modifier,
    selectedCategory: GameCategory?,
    onSelectCategory: (GameCategory?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = selectedCategory?.displayName() ?: ALL_TYPES_LABEL
    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            value = label,
            onValueChange = {},
            label = { Text("Game Type") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(ALL_TYPES_LABEL) },
                onClick = {
                    onSelectCategory(null)
                    expanded = false
                }
            )
            GameCategory.entries.sortedBy { it.displayName() }.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayName()) },
                    onClick = {
                        onSelectCategory(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun GameListView(
    games: List<GameDefinition>,
    onClick: (String) -> Unit,
    onLongClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Game Name",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Game Type",
                    modifier = Modifier.width(140.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider()
        }
        items(games) { game ->
            GameListRow(
                game = game,
                onClick = { onClick(game.id) },
                onLongClick = { onLongClick(game.id) }
            )
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameListRow(game: GameDefinition, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (game.favorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(16.dp)
                )
            }
            Text(text = game.name, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = game.category.displayName(),
            modifier = Modifier.width(140.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = game.category.cardColor(),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameCard(game: GameDefinition, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
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
            if (game.favorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp)
                )
            }
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

private const val ALL_TYPES_LABEL = "All Types"

/** Simple 2x2 grid glyph for the tile-view toggle (no extended icon dependency). */
private fun gridViewIcon(): ImageVector =
    ImageVector.Builder(
        name = "GridView",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        val fill = SolidColor(Color.Black)
        fun addSquare(left: Float, top: Float) {
            path(fill = fill) {
                moveTo(left, top)
                lineTo(left + 7f, top)
                lineTo(left + 7f, top + 7f)
                lineTo(left, top + 7f)
                close()
            }
        }
        addSquare(3f, 3f)
        addSquare(14f, 3f)
        addSquare(3f, 14f)
        addSquare(14f, 14f)
    }.build()

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
fun GameSettingsView(gameName: String, onHowToPlay: () -> Unit, onStart: (GameDifficulty) -> Unit, onBack: () -> Unit) {
    var difficulty by rememberSaveable { mutableStateOf(GameDifficulty.MEDIUM) }
    PageScaffold(title = "$gameName - Settings", onBack = onBack) {
        Text("Per-game options (stub): board size, AI players, starter, undo, timer mode.")
        Text("Difficulty", modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GameDifficulty.entries.forEach { level ->
                FilterChip(
                    selected = difficulty == level,
                    onClick = { difficulty = level },
                    label = { Text(level.label) }
                )
            }
        }
        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onHowToPlay) { Text("How to Play") }
            Button(onClick = { onStart(difficulty) }) { Text("Start Game") }
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
