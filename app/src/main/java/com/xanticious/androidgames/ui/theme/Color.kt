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
// Card-game palette tokens. Shared by every game in the Card category so card
// rendering stays consistent. Per AGENTS.md, no hex values may live outside
// this file, so card composables reference these tokens only.
// ---------------------------------------------------------------------------
val CardFace = Color(0xFFF8F9FA)       // card face background (Open Color Gray 0)
val CardFaceShadow = Color(0xFFDEE2E6) // subtle inner edge / pip wash (Gray 3)
val CardBack = Aqua4                    // face-down card back fill
val CardBackPattern = Aqua2            // face-down card back accent
val CardBorder = Color(0xFF495057)     // card outline (Open Color Gray 7)
val CardRed = Color(0xFFE03131)        // hearts / diamonds pips (Open Color Red 8)
val CardBlack = Color(0xFF212529)      // clubs / spades pips (Open Color Gray 9)
val CardSlot = Dark2                    // empty pile / foundation outline fill
val CardHighlight = GameAccent          // selected / legal-move highlight
val CardTableFelt = Color(0xFF0B7285)  // card table background (Open Color Cyan 9)

