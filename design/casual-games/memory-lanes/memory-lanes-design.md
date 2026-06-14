# Memory Lanes — Design Document

## Overview
- Memory Lanes is a single-player sequence-memory game. It is not a traditional Simon-style game; instead of replaying the full accumulated sequence from scratch each round, the player builds the sequence by arranging tiles from a finite tile bank.
- Each round reveals exactly one new tile for 2 seconds. When it disappears, the tile bank updates to show the exact composition of the full sequence so far — that is, each tile type together with how many times it appears in the sequence. The player has unlimited time to arrange those tiles into the correct order in the sequence builder lane. They can undo mistakes before committing.
- The tile bank removes the need to recall *which* tiles are in the sequence or *how often* each appears; the only memory challenge is the *order* in which the tiles were shown.
- When the player taps Done the sequence is validated. A correct sequence advances to the next round (one more tile is shown). An incorrect sequence ends the game.
- The game continues until the player makes a mistake or chooses to stop.
- All stats remain local to the device.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Background: deep `Dark0` surface giving a calm, focused atmosphere.
- Tile bank: a row of large rounded square tiles, each with a distinct color drawn from the aqua/teal/blue/coral accent range. Tiles are labeled 1–N (where N is the bank size from settings). Each tile shows a count badge indicating how many times that tile appears in the current sequence. Tiles with a count of 0 are dimmed and non-tappable. When the player taps a tile, its count badge decrements by 1 and the tile is appended to the sequence builder lane.
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
│  Tile bank  (tap to place)          │
│  [1 ×1][2 ×2][3 ×1][4 ×0]          │  ← Each tile shows how many remain to place
├─────────────────────────────────────┤
│  [Undo]              [Done]         │  ← Action row (Done enabled only when all tiles placed)
└─────────────────────────────────────┘
```
- The tile bank shows every tile type in the bank. A count badge (`×N`) on each tile reflects how many of that tile type still need to be placed in the sequence lane. Tiles with `×0` are dimmed and non-tappable.
- Done is enabled only when the sequence lane is fully filled (all bank counts reach 0), ensuring the player cannot submit a partial sequence.
- The reveal zone and sequence builder lane never overlap.
- Result and game-over panels appear below the sequence lane and action row; they never cover the reveal zone.
- Tablets may show the tile bank as a 2-row grid for larger bank sizes.

## Settings
- **Bank size**: 2–10 tiles (default: 4). Determines how many distinct tile types exist. A larger bank means the sequence can draw from more tile types, making the order harder to remember.
- **Reveal duration**: 1 s, 2 s (default), 3 s. How long each new tile is shown.
- **Tile labels**: Numbers (default), Colors only, Letters.
- **Confirm wrong** (on/off, default on): briefly shows the correct tile at the first wrong position before ending the game.

## How to Play
- Before the game starts, choose your bank size in settings (2–10 tiles).
- Round 1: One tile is shown in the reveal zone for 2 seconds. It then disappears.
- The tile bank updates to show the composition of the sequence so far: the tile you just saw gets a `×1` count badge; all other tiles show `×0`. Since Round 1 has only one tile, the arrangement is trivial — tap that tile once to fill the single slot, then tap Done.
- Tap Undo to return the last placed tile to the bank (its count badge increments back by 1).
- When all slots are filled (all bank counts are 0), tap Done to submit.
  - **Correct**: the game advances to Round 2. A new tile is shown for 2 seconds. The bank now reflects the two-tile sequence — tap to arrange those tiles in the right order.
  - **Wrong**: the game ends, showing how far you got.
- Each round adds one tile to the cumulative sequence you must arrange correctly.
- The game has no end other than a wrong answer; your goal is to beat your longest sequence.

## Controls
- **Tap** a tile in the bank (count > 0): appends that tile to the next open slot in the sequence builder lane and decrements the tile's count badge by 1.
- **Tap** Undo: removes the rightmost tile from the sequence builder lane and returns it to the bank (increments that tile's count badge by 1).
- **Tap** Done: submits the built sequence for validation. Enabled only when all bank counts are 0 (sequence lane is fully filled).
- The bank tiles with `×0` are dimmed and non-tappable. Only tiles with remaining count can be placed.
- The bank tiles remain otherwise tappable at all times during the build phase; there is no time pressure.

## Gameplay Loop

### Reveal Phase
- The state machine enters `Revealing`. The tile for the current round is displayed in the reveal zone with a full-brightness animation.
- After the reveal duration elapses, the tile fades out. The state transitions to `Building`.
- During `Revealing` the sequence lane and bank tiles are non-interactive (greyed out).

### Building Phase
- After the reveal phase, the tile bank is populated with the exact composition of the full sequence so far. Each tile type shows a count badge equal to the number of times it appears in the sequence.
- The player taps tiles from the bank to fill the sequence lane slot by slot. Each tap decrements the chosen tile's count and advances to the next empty slot.
- Undo removes one tile from the lane, returning it to the bank (that tile's count badge increments by 1). Undo can be tapped repeatedly to clear the lane entirely.
- Done becomes enabled only when all bank counts have reached 0, guaranteeing the lane is exactly the right length.
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
- Tile bank: always visible below the lane; each tile shows its remaining count badge. Tiles dim (count = 0) as they are fully placed. During `Revealing` and `Validating` phases the entire bank is non-interactive (greyed out).
- Action row: Undo (disabled when lane is empty) and Done (enabled only when all bank counts are 0).
- Game-over / confirmation panels appear below the action row; they never cover the reveal zone or sequence lane.
