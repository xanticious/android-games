package com.xanticious.androidgames.ui.theme

import androidx.compose.ui.graphics.Color

val Aqua0 = Color(0xFFE6FCF5)
val Aqua1 = Color(0xFF96F2D7)
val Aqua2 = Color(0xFF20C997)
val Aqua3 = Color(0xFF15AABF)
val Aqua4 = Color(0xFF1864AB)

val Dark0 = Color(0xFF0B1F2A)
val Dark1 = Color(0xFF102A3A)
val Dark2 = Color(0xFF17435E)

// ---------------------------------------------------------------------------
// Action-game canvas palette tokens (Open Color inspired).
// Per AGENTS.md, game composables must reference these tokens — never inline hex.
// ---------------------------------------------------------------------------
val GameCourt = Dark0          // playfield background
val GameCourtLine = Aqua3      // nets, lane lines, grid lines
val GamePlayer = Aqua2         // player ship / paddle / projectiles
val GameEnemy = Color(0xFFE03131)    // enemies, opponent paddle (Open Color Red 8)
val GameAccent = Color(0xFFFFD43B)   // ball, beacons, power-ups (Open Color Yellow 5)
val GameHazard = Color(0xFFF76707)   // hazards, danger zones (Open Color Orange 7)
val GameNeutral = Color(0xFFADB5BD)  // bricks, walls, debris (Open Color Gray 5)
val GameSuccess = Color(0xFF37B24D)  // success / collectibles (Open Color Green 7)

// ---------------------------------------------------------------------------
// Puzzle-game board palette tokens (see design/common/puzzle-grid-board.md).
// Puzzle composables must reference these tokens — never inline hex.
// ---------------------------------------------------------------------------
val PuzzleBoard = Dark1         // board background
val PuzzleCell = Dark0          // empty / default cell fill
val PuzzleGridLine = Dark2      // cell outlines, grid lines
val PuzzleGiven = Aqua4         // given / locked clues, fixed tiles
val PuzzlePlayer = Aqua1        // player-entered content
val PuzzlePlayerAlt = Aqua0     // alternate player content / labels
val PuzzleHighlight = Aqua2     // current selection / highlight / movable tile
val PuzzleSolved = Aqua1        // correctly placed / solved emphasis

// Distinct hues for color-coded puzzle tokens (Flood, Sudoku Colors, Match
// Three, Pipes endpoints, Numberlink pairs). Shape/label always disambiguates
// per design/common/puzzle-controls.md so these never rely on color alone.
val PuzzleHueRed = Color(0xFFE03131)     // Open Color Red 8
val PuzzleHueOrange = Color(0xFFF76707)  // Open Color Orange 7
val PuzzleHueYellow = Color(0xFFFFD43B)  // Open Color Yellow 5
val PuzzleHueGreen = Color(0xFF37B24D)   // Open Color Green 7
val PuzzleHueTeal = Color(0xFF20C997)    // Open Color Teal 5
val PuzzleHueBlue = Color(0xFF1C7ED6)    // Open Color Blue 7
val PuzzleHueViolet = Color(0xFF7048E8)  // Open Color Violet 7
val PuzzleHuePink = Color(0xFFE64980)    // Open Color Pink 6

