# Memory Lanes — Design Document

## Overview
- Memory Lanes is a single-player sequence-memory game. It is not a traditional Simon-style game; instead of replaying the full accumulated sequence from scratch each round, the player builds the sequence by assembling tiles in order from a personal tile bank.
- Each round reveals exactly one new tile for 2 seconds. When it disappears, the player has unlimited time to construct the full sequence seen so far — from tile 1 up to the current round's tile — by tapping tiles from the bank. They can undo mistakes before committing.
- When the player taps Done the sequence is validated. A correct sequence advances to the next round (one more tile is shown). An incorrect sequence ends the game.
- The game continues until the player makes a mistake or chooses to stop.
- All stats remain local to the device.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Background: deep `Dark0` surface giving a calm, focused atmosphere.
- Tile bank: a row of large rounded square tiles, each with a distinct color drawn from the aqua/teal/blue/coral accent range. Tiles are labeled 1–N (where N is the bank size from settings).
- Sequence builder: a horizontal lane of slots showing tiles the player has tapped in order. Empty slots show a faint `Dark2` outline. Filled slots glow with the tile's assigned color.
- The revealed tile display: a large, centered tile flashes on screen for 2 seconds with a subtle pulse animation, then fades out.
- Correct sequence confirmation: a brief green ring sweeps the sequence lane.
- Wrong sequence reveal: the incorrect position shakes and dims; the correct tile at that position is briefly highlighted.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Memory Lanes   Round 4     ⚙       │  ← Top bar
├─────────────────────────────────────┤
│                                     │
│           [  TILE 2  ]              │  ← Reveal zone (tile shown for 2 s, then hidden)
│                                     │
├─────────────────────────────────────┤
│  ← Your sequence so far (4 slots) → │
│  [2] [ ] [ ] [ ]                    │  ← Sequence builder lane (scrollable if > 8 slots)
├─────────────────────────────────────┤
│  Tile bank                          │
│  [1][2][3][4][5][6]                 │  ← Tappable tile bank (bank size from settings)
├─────────────────────────────────────┤
│  [Undo]              [Done]         │  ← Action row
└─────────────────────────────────────┘
```
- The reveal zone and sequence builder lane never overlap.
- Result and game-over panels appear below the sequence lane and action row; they never cover the reveal zone.
- Tablets may show the tile bank as a 2-row grid for larger bank sizes.

## Settings
- **Bank size**: 2–10 tiles (default: 4). Determines how many distinct tiles exist in the bank and therefore how many different tiles can appear in the sequence.
- **Reveal duration**: 1 s, 2 s (default), 3 s. How long each new tile is shown.
- **Tile labels**: Numbers (default), Colors only, Letters.
- **Confirm wrong** (on/off, default on): briefly shows the correct tile at the first wrong position before ending the game.

## How to Play
- Before the game starts, choose your bank size in settings (2–10 tiles).
- Round 1: One tile is shown in the reveal zone for 2 seconds. It then disappears.
- Build the sequence by tapping tiles from the bank. Since it is Round 1, you only need to tap the one tile you just saw.
- Tap Undo to remove the last tile you added to the sequence.
- When your sequence looks correct, tap Done.
  - **Correct**: the game advances to Round 2. A new tile is shown for 2 seconds. Now you must reproduce the full two-tile sequence.
  - **Wrong**: the game ends, showing how far you got.
- Each round adds one tile to the cumulative sequence you must reproduce.
- The game has no end other than a wrong answer; your goal is to beat your longest sequence.

## Controls
- **Tap** a tile in the bank: appends that tile to the end of the sequence builder lane.
- **Tap** Undo: removes the rightmost tile from the sequence builder lane.
- **Tap** Done: submits the built sequence for validation.
- The bank tiles remain tappable at all times during the build phase; there is no time pressure.
- Tapping Done with fewer tiles in the lane than the required sequence length counts as a wrong answer.

## Gameplay Loop

### Reveal Phase
- The state machine enters `Revealing`. The tile for the current round is displayed in the reveal zone with a full-brightness animation.
- After the reveal duration elapses, the tile fades out. The state transitions to `Building`.
- During `Revealing` the sequence lane and bank tiles are non-interactive (greyed out).

### Building Phase
- The player assembles the sequence from the tile bank. They can take as long as they need.
- The sequence lane shows the growing list of tapped tiles in order. Each slot is numbered (`1`, `2`, …) to help the player track position.
- Undo removes one tile from the lane; it can be tapped repeatedly to clear the lane entirely.
- When the player taps Done, the state transitions to `Validating`.

### Validation
- The built sequence is compared element-by-element to the target sequence (all tiles revealed so far, in order).
- If every position matches: emit `SequenceCorrect` → increment round counter → show brief confirmation → back to `Revealing` for the next round.
- If any position is wrong: emit `SequenceWrong` → if "Confirm wrong" is on, highlight the first incorrect position and flash the correct tile for 1 second → transition to `GameOver`.

### Game Over
- The result panel appears below the sequence lane showing: rounds survived, longest sequence, personal best.
- Tap **Play Again** to start a fresh game with the same settings.
- Tap **Menu** to return to the lobby.

## State Machine
- A dedicated `MemoryLanesStateMachine` in `state/` exposes `StateFlow<MemoryLanesState>`.
```
Idle
 └─ StartGame → Revealing
Revealing
 └─ RevealComplete → Building
Building
 ├─ TileAdded → Building (sequence lane updated)
 ├─ TileUndo → Building (last tile removed)
 └─ DoneSubmitted → Validating
Validating
 ├─ SequenceCorrect → Revealing (next round)
 └─ SequenceWrong → GameOver
GameOver
 ├─ PlayAgain → Revealing (round reset to 1)
 └─ BackToMenu → Idle
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Longest sequence reached (all-time) | yes |
| Longest sequence per bank size | yes |
| Total rounds played (lifetime) | yes |
| Total games played | yes |
| Games ended on round 1 | yes |

## HUD
- Top bar: game title, current round number, settings access.
- Reveal zone: centered large tile with countdown ring (optional, configurable in settings).
- Sequence builder lane: numbered slots; filled slots show tile color and label; active (last tapped) slot has a faint highlight ring.
- Tile bank: always visible below the lane; tiles dim during `Revealing` and `Validating` phases.
- Action row: Undo (disabled when lane is empty) and Done (always tappable).
- Game-over / confirmation panels appear below the action row; they never cover the reveal zone or sequence lane.
