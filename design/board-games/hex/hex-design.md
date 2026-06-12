# Hex — Design Document

## Overview
- Hex is a connection strategy game played on a rhombus of hexagonal cells where each side tries to connect its opposite borders.
- The default board is 11×11, with smaller and larger options for pacing and difficulty variation.
- The player always faces one local AI opponent.
- There are no draws in Hex; every valid match eventually produces a winner once a complete path is formed.
- This version includes the optional swap rule, letting the second player claim the first move's position after turn one to balance opening advantage.
- The design makes edge goals unmistakable, keeps touch input simple, and clearly reveals the winning path at match end.
- Player order can be First, Second, or Random, with the assigned connection edges shown before the first move.
- All play is offline against local AI, with stats kept only on the device and navigation handled by the app's state machines.
- Result text and actions remain below the board, preserving a full view of the finished network.

## Visual Style
- Material 3 layout with the underwater palette from `ui/theme/Color.kt`.
- The board frame uses `Dark1` and `Aqua4`, with edge goal bands tinted differently for the two players.
- Blue and red player choices are adapted into the app palette through cool/warm themed fills while still harmonizing with `Aqua2`, `Aqua3`, and `Dark2` UI surfaces.
- Empty cells use a muted `Dark2` face so placed stones read immediately.
- The currently targetable cell can lift slightly with an `Aqua1` outline.
- The last placed cell gets a crisp `Aqua3` border marker.
- When the swap rule is active, the first-move cell stays specially marked until the second player chooses swap or decline.
- Winning connections animate as a glowing linked path using `Aqua3` accents along the connected chain.
- Edge labels remain subtle but always visible so players understand which sides they are trying to join.
- All implementation colors come from `ui/theme/Color.kt` or Material 3 roles built from those tokens.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Hex             11×11   Swap on  ⚙  │  ← Top bar
├─────────────────────────────────────┤
│ You: North-South   AI: West-East     │  ← Goal strip
├─────────────────────────────────────┤
│      N goal                         │
│     ◇ ◇ ◇ ◇ ◇ ◇ ◇ ◇ ◇ ◇ ◇           │
│    ◇ ◇ ◇ ◇ ◇ ◇ ◇ ◇ ◇ ◇ ◇            │
│ W ◇ ◇ ● ◇ ◇ ○ ◇ ◇ ◇ ◇ ◇ E          │  ← Board anchor
│    ◇ ◇ ◇ ● ◇ ◇ ◇ ◇ ◇ ◇ ◇            │
│     ◇ ◇ ◇ ◇ ◎ ◇ ◇ ◇ ◇ ◇ ◇           │
│      S goal                         │
├─────────────────────────────────────┤
│ Your turn. Connect north to south.   │  ← Prompt/result panel
└─────────────────────────────────────┘
```
- Portrait scales the rhombus to fit the available width while preserving large hex cells.
- Tablets can add side panels for move history and path explanation without reordering the board.
- Edge goal bands stay attached to the relevant board sides even when the board is zoomed.
- Result panels appear below the board and never cover the winning chain.

## Settings
- **Board size**: 9×9, 11×11 (default), 13×13, or 15×15.
- **Swap rule**: on/off (default on).
- **Opponent difficulty**: Easy, Medium, Hard.
- **Player order**: First, Second, or Random.
- **Show last move** (on/off, default on).
- **Show connection hints** (on/off, default off): practice helper for bridges, cuts, and near-complete paths.

## How to Play
- Players alternate placing one stone on any empty hex cell.
- One player tries to connect the north and south edges; the other tries to connect the west and east edges.
- Stones never move or capture.
- A player wins immediately when their stones form one continuous connected chain between their two goal edges.
- Hex cannot end in a draw.
- If the swap rule is enabled, after the first move the second player may either keep their assigned side and move normally or swap sides and take over the first stone.

## Controls
- Tap an empty cell to place a stone.
- During the swap decision, tap **Swap** to claim the first stone or **Keep sides** to continue normally.
- Tap the edge labels to briefly highlight your connection goal.
- Pinch to zoom on larger boards; double-tap recenters the rhombus.
- After game over, use Rematch or Menu from the below-board result panel.

## AI Opponents
- **Easy**: extends nearby chains and blocks obvious adjacent links, but misses bridge tactics and weak cuts.
- **Medium**: values virtual connections, bridges, edge templates, and swap-rule balance.
- **Hard**: searches connection races, identifies must-play cells, handles larger board sizes, and makes strong swap decisions.
- AI difficulty changes decision quality only; legal placement, swap timing, and no-draw win detection remain identical.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses per difficulty | yes |
| Results by board size | yes |
| Results with swap on / off | yes |
| Wins by assigned edge pair | yes |
| Average winning path length | yes |
| Swap accepted / declined counts | yes |

## State Machine
- A dedicated `HexStateMachine` in `state/` exposes `StateFlow<HexState>`.
```
Idle
 └─ MatchStarted → AssigningSides
AssigningSides
 └─ SidesAssigned → FirstMove
FirstMove
 └─ FirstStonePlaced → SwapDecision / CheckingResult
SwapDecision
 ├─ SwapAccepted → PlayerTurn / AiThinking
 └─ SwapDeclined → PlayerTurn / AiThinking
PlayerTurn
 ├─ CellPreviewed → PlayerTurn
 ├─ CellChosen → PlacingStone
 └─ Surrendered → GameOver
PlacingStone
 └─ StonePlaced → CheckingResult
CheckingResult
 ├─ ConnectionCompleted → GameOver
 └─ TurnAdvanced → PlayerTurn / AiThinking
AiThinking
 └─ AiMoveChosen → PlacingStone / SwapDecision
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `HexRules` controller validates board dimensions, legal placements, swap ownership, neighbor connectivity, no-draw invariants, and winning path detection; unit tests cover each supported board size without Android imports.

## HUD
- Top bar shows board size, swap setting, and settings access.
- Goal strip shows both edge assignments and highlights the active player.
- Board annotations show last move, legal target feedback, optional hints, and the final winning path.
- Prompt/result panel explains whose turn it is, swap decisions, connection wins, and rematch actions below the board.
- Victory/defeat presentation follows `design/common/victory-defeat.md`: results never cover the board.
