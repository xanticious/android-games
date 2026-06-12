# Connect Four — Design Document

## Overview
- Connect Four is a fast, readable vertical alignment game built for short single-player sessions against local AI.
- The default board is the classic 7-column by 6-row grid, with an optional Wide variant for extra strategic space.
- A move is always one tap: choose a column and let the disc fall to the lowest open cell.
- The player always faces one local AI opponent.
- Win by making four connected discs horizontally, vertically, or diagonally.
- If the board fills with no four-in-a-row, the game ends in a draw.
- The design focuses on strong hover previews, clear column targeting, and a satisfying finish when four connected discs win.
- All state remains local to the device, and the game fits the app's simple screen progression of settings, how-to-play, match, and results.
- Because the board resolves quickly, the UI should feel crisp, immediate, and highly legible at phone size.
- No result element may cover the grid; the end state is communicated below the board while the winning pattern remains visible.

## Visual Style
- Material 3 cards and buttons using the underwater palette from `ui/theme/Color.kt`.
- Board shell uses `Dark1` with `Aqua4` edge accents and recessed circular slots.
- Neutral slot backgrounds use `Dark0` so the player pieces remain vivid.
- Red and yellow player choices are stylized as warm/cool themed discs rendered against the underwater UI shell; outlines use `Aqua0` or `Dark0` for contrast.
- Hover preview uses a semi-transparent disc with `Aqua2` glow and subtle bob animation.
- Winning four cells receive a bright `Aqua3` halo plus a soft pulse.
- Text and HUD cards use `Aqua0` on dark surfaces, with `Aqua1` for secondary information.
- Light theme can brighten surrounding surfaces, but the board itself keeps strong slot contrast.
- Disc drop animation should feel weighty, with a quick settle bounce on landing.

## Screen Layout
```
┌─────────────────────────────────────┐
│ Connect Four  Classic  You: First ⚙ │  ← Top bar
├─────────────────────────────────────┤
│ You ●●●   AI ●●    Difficulty Medium │  ← Status strip
├─────────────────────────────────────┤
│   ↓   ↓   ↓   ↓   ↓   ↓   ↓         │  ← Column targets
│  ┌───────────────────────────────┐   │
│  │ ○ ○ ○ ○ ○ ○ ○                 │
│  │ ○ ○ ○ ○ ○ ○ ○                 │  ← Grid anchor
│  │ ○ ○ ○ ○ ○ ○ ○                 │
│  │ ○ ○ ○ ○ ○ ○ ○                 │
│  │ ○ ○ ○ ○ ○ ○ ○                 │
│  │ ○ ○ ○ ○ ○ ○ ○                 │
│  └───────────────────────────────┘   │
├─────────────────────────────────────┤
│ Choose a column.                     │  ← Prompt/result panel
└─────────────────────────────────────┘
```
- Classic uses 7 columns by 6 rows.
- Wide variant uses a wider grid, such as 9 columns by 6 rows, with the same four-in-a-row win condition.
- Portrait centers the grid and keeps column targets large enough for touch; tablets can add side stats without moving the board.
- Result panels appear under the grid, never over the winning pattern.

## Settings
- **Board variant**: Classic 7×6 or Wide 9×6.
- **Player order**: First, Second, or Random.
- **Opponent difficulty**: Easy, Medium, Hard.
- **Show column preview** (on/off, default on): previews where the disc will land before tapping.
- **Show threat hints** (on/off, default off): highlights immediate wins and blocks in a practice-friendly way.
- **Fast AI turns** (on/off, default on): keeps AI moves responsive while preserving a visible disc drop.

## How to Play
- Choose a column to drop one of your discs.
- The disc falls to the lowest empty slot in that column.
- Players alternate turns until someone connects four discs in a row horizontally, vertically, or diagonally.
- Block the AI's threats while building your own connected lines.
- If every slot fills before either side connects four, the game is a draw.
- The Wide board variant adds columns for more open play but does not change the win condition.

## Controls
- Tap a column target or any slot in that column to drop a disc.
- Drag or hover across columns to move the preview before releasing on touch devices that support drag gestures.
- Tap a highlighted winning or blocking hint, when enabled, to explain why that column matters.
- After game over, use Rematch or Menu from the below-board result panel.

## AI Opponents
- **Easy**: takes immediate wins and blocks obvious player wins, but otherwise favors simple center-ish moves.
- **Medium**: recognizes forks, stacked threats, center control, and one-turn tactical blocks.
- **Hard**: searches deeper, creates multi-threat positions, handles Wide-board spacing, and avoids setting up player forks.
- AI difficulty changes decision quality only; board size, legal drops, and win detection remain identical.

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Wins / losses / draws per difficulty | yes |
| Results by Classic and Wide variant | yes |
| Fastest win by moves | yes |
| Win direction counts | yes |
| Games won going first / second | yes |
| Immediate threat blocks made | yes |

## State Machine
- A dedicated `ConnectFourStateMachine` in `state/` exposes `StateFlow<ConnectFourState>`.
```
Idle
 └─ MatchStarted → ChoosingOrder
ChoosingOrder
 └─ OrderChosen → PlayerTurn / AiThinking
PlayerTurn
 ├─ ColumnPreviewed → PlayerTurn
 ├─ ColumnChosen → DiscDropping
 └─ Surrendered → GameOver
DiscDropping
 ├─ DiscSettled → CheckingResult
 └─ ColumnFullExplained → PlayerTurn
CheckingResult
 ├─ FourConnected → GameOver
 ├─ BoardFull → GameOver
 └─ TurnAdvanced → AiThinking / PlayerTurn
AiThinking
 └─ AiColumnChosen → DiscDropping
GameOver
 └─ Rematch / Menu → Idle
```
- A pure `ConnectFourRules` controller validates column drops, full columns, Classic and Wide dimensions, four-in-a-row detection, draw detection, and AI threat evaluation helpers; unit tests cover horizontal, vertical, diagonal, full-board, and Wide-board cases without Android imports.

## HUD
- Top bar shows game name, board variant, player order, and settings access.
- Status strip shows active turn, difficulty, move count, and last column.
- Column target row shows drop previews and disabled full columns.
- Prompt/result panel shows turn prompts, hint explanations, win direction, draw state, and final results below the board.
- Victory/defeat/draw presentation follows `design/common/victory-defeat.md`: results never cover the grid.
