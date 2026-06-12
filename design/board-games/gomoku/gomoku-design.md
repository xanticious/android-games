# Gomoku — Design Document

## Overview
- Gomoku is a territory-free abstract strategy game where players place stones on intersections to make five in a row.
- The default board is 15×15, with a larger 19×19 option for longer, more spacious matches.
- The player always faces a local AI opponent in a fully offline match.
- Standard rules allow either side to win with any unbroken line of exactly or at least five stones, according to the selected rules profile.
- Optional Renju rules constrain Black to reduce first-player advantage.
- Under Renju, Black cannot play forbidden overlines, double-threes, or double-fours; White remains unconstrained.
- The opening player can be Human, AI, or Random, and Black is always the first player.
- The design prioritizes clean intersection targeting, visible last-move feedback, and easy understanding of forbidden moves.
- All profile and stat data stays local to the device.
- Result messaging, warnings, and rematch actions stay below the board so the completed position is always visible.

## Visual Style
- Material 3 interface framed by `Dark0`, `Dark1`, `Aqua0`, and `Aqua1` from `ui/theme/Color.kt`.
- The wooden-board fantasy is translated into the app palette through layered dark surfaces and aqua edge lighting rather than literal wood tones.
- Grid lines use `Aqua1` on a muted `Dark1` board plate for sharp intersection readability.
- Black stones use `Dark0` with `Aqua2` rim highlights; white stones use `Aqua0` with `Dark2` shadowing.
- Star points, if shown, use small `Aqua2` dots that never compete with the stones.
- The last placed stone gets a thin `Aqua3` ring marker.
- Forbidden intersections in Renju show a subtle crossed indicator using `Aqua2` or a dimmed `Dark2` badge.
- Winning five stones glow and pulse with `Aqua3` while the rest of the board remains static.
- Information cards and buttons below the board keep the same underwater Material 3 identity as the rest of the app.
- All implementation colors come from `ui/theme/Color.kt` or Material 3 roles built from those tokens.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Gomoku        15×15   Renju off  ⚙  │  ← Top bar
├─────────────────────────────────────┤
│ You: Black ●     AI: White ○         │  ← Status strip
├─────────────────────────────────────┤
│  ┌───────────────────────────────┐   │
│  │ + + + + + + + + + + + + + + + │   │
│  │ + + + + + + + + + + + + + + + │   │
│  │ + + + + ● ○ + + + + + + + + + │   │  ← Board anchor
│  │ + + + + + ● ○ + + + + + + + + │   │
│  │ + + + + + + ◎ + + + + + + + + │   │
│  └───────────────────────────────┘   │
├─────────────────────────────────────┤
│ Your turn. Create five in a row.     │  ← Prompt/result panel
└─────────────────────────────────────┘
```
- Portrait centers the square board and preserves large touch targets by allowing pinch zoom or a focused board viewport on 19×19.
- Tablet layouts keep the board square and use side gutters for move history, captures of warnings, and difficulty details.
- The prompt/result panel appears below the board and never covers stones or winning lines.

## Settings
- **Board size**: 15×15 (default) or 19×19.
- **Ruleset**: Standard (default) or Renju.
- **Opponent difficulty**: Easy, Medium, Hard.
- **Player color**: Black, White, or Random.
- **Show last move** (on/off, default on): rings the most recent stone.
- **Show forbidden moves** (on/off, default on when Renju is active): marks Black-only forbidden intersections before selection.
- **Show threat hints** (on/off, default off): practice helper for immediate wins, blocks, and Renju restrictions.

## How to Play
- Players alternate placing one stone on an empty intersection.
- Black moves first.
- Win by forming five connected stones horizontally, vertically, or diagonally.
- In Standard rules, both colors follow the same placement rules.
- In Renju rules, Black is not allowed to play overlines, double-threes, or double-fours.
- A forbidden Renju move is rejected before placement and explained in the prompt panel.
- If the board fills with no winning line, Standard games may draw; Renju games use the selected rules profile's draw handling.

## Controls
- Tap an empty intersection to preview and place a stone.
- Drag across the board to inspect intersections before releasing on the intended point.
- Pinch to zoom on larger boards; double-tap recenters the board.
- Tap a forbidden indicator, when visible, to show the reason below the board.
- After game over, use Rematch or Menu from the below-board result panel.

## AI Opponents
- **Easy**: blocks immediate wins and takes obvious fives, but misses deeper open-four and fork threats.
- **Medium**: evaluates open threes, open fours, double threats, center influence, and Renju legality.
- **Hard**: searches forcing sequences, balances attack and defense, accounts for 15×15 and 19×19 spacing, and avoids illegal Black moves under Renju.
- AI difficulty changes decision quality only; board size, turn order, and ruleset legality remain deterministic.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses / draws per difficulty | yes |
| Results by board size | yes |
| Results by Standard vs Renju | yes |
| Fastest win by move count | yes |
| Wins as Black / White | yes |
| Forbidden Renju moves prevented | yes |

## State Machine
- A dedicated `GomokuStateMachine` in `state/` exposes `StateFlow<GomokuState>`.
```
Idle
 └─ MatchStarted → ChoosingOpening
ChoosingOpening
 └─ OpeningChosen → PlayerTurn / AiThinking
PlayerTurn
 ├─ IntersectionPreviewed → PlayerTurn
 ├─ IntersectionChosen → ValidatingMove
 └─ Surrendered → GameOver
ValidatingMove
 ├─ MoveAccepted → PlacingStone
 └─ ForbiddenMoveRejected → PlayerTurn
PlacingStone
 └─ StonePlaced → CheckingResult
CheckingResult
 ├─ FiveFound → GameOver
 ├─ BoardFull → GameOver
 └─ TurnAdvanced → PlayerTurn / AiThinking
AiThinking
 └─ AiMoveChosen → ValidatingMove
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `GomokuRules` controller validates board size, legal placement, five-in-a-row detection, draw detection, and Renju forbidden move detection; unit tests cover Standard and Renju behavior without Android imports.

## HUD
- Top bar shows board size, ruleset, and settings access.
- Status strip shows player color, AI color, active turn, difficulty, and move count.
- Board annotations show last move, winning line, optional threat hints, and Renju forbidden intersections.
- Prompt/result panel explains turn prompts, rejected moves, winner, draw state, and rematch actions below the board.
- Victory/defeat/draw presentation follows `design/common/victory-defeat.md`: results never cover the grid.
