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

