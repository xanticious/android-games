# Falling Blocks — Design Document

## Overview
A high-definition Tetris-style game where the player rotates and places falling tetrominoes to complete horizontal lines. Lines are cleared as they fill, and the game ends when the stack reaches the top. Classic survival gameplay with a polished modern presentation.

## Visual Style
- HD block art: each tetromino type has a distinct solid color with subtle inner glow and beveled edges.
- Ghost piece: a transparent outline at the current drop position shows where the piece will land.
- Line clear: cleared rows flash white and implode with a brief particle burst (debris fragments).
- Grid: subtle grid lines on a dark background, fading toward the edges.
- Background: animated geometric pattern that subtly shifts color as the level increases.
- Level-up event: a brief screen flash and color palette shift for the background.

## Screen Layout
```
┌─────────────────────────────────┐
│  [Score]   [Level]   [Lines]    │  ← HUD (top)
├──────────────────────┬──────────┤
│                      │ [NEXT]   │
│   GAME BOARD         │ [piece]  │  ← Next piece preview (right panel)
│   (10×20 grid)       │         │
│                      │ [HOLD]   │
│                      │ [piece]  │  ← Hold piece (right panel)
│                      │         │
└──────────────────────┴──────────┘
     [Controls overlay — bottom]
```

## Controls
- **Move Left / Right**: swipe left or right on the game board.
- **Rotate Clockwise**: tap the right half of the board.
- **Rotate Counter-Clockwise**: tap the left half of the board.
- **Soft Drop** (speed up descent): swipe down slowly.
- **Hard Drop** (instant drop): swipe down fast (flick gesture).
- **Hold Piece**: tap the hold area in the side panel to hold the current piece (can hold once per piece).
- No on-screen D-pad needed — all controls are gesture-based on the board.

## Gameplay Loop

### Core Loop
1. A new tetromino piece appears at the top center of the board.
2. The piece falls at the current level's drop speed.
3. Player moves, rotates, and places the piece.
4. When the piece locks in place, check for completed rows.
5. Clear completed rows, slide everything above down, award points.
6. Spawn the next piece (shown in the Next preview).
7. Repeat until the stack reaches the top of the board (game over).

### Tetrominoes
Standard 7 tetrominoes: I, O, T, S, Z, J, L.
- Uses the "7-bag" randomization system: each set of 7 pieces contains exactly one of each type, in random order.

### Scoring
| Event | Points |
|-------|--------|
| Single line clear | 100 × level |
| Double line clear | 300 × level |
| Triple line clear | 500 × level |
| Tetris (4 lines at once) | 800 × level |
| Back-to-back Tetris bonus | +50% on second and subsequent consecutive Tetrises |
| Soft drop | 1 per row |
| Hard drop | 2 per row |

### Leveling
- Starts at level 1.
- Level increases every 10 lines cleared.
- Drop speed increases with each level.
- Maximum level: 20 (piece falls nearly instantly; only hard drop play is viable).

### High Score
- Personal best score stored locally per profile.
- Best score displayed on the game over screen.

## State Machine
```
Idle
 └─ GameStart → Spawning
Spawning
 └─ PieceSpawned → Falling
Falling
 ├─ MoveInput → Falling (position updated)
 ├─ RotateInput → Falling (rotation updated)
 ├─ SoftDropInput → Falling (speed increased temporarily)
 ├─ HardDropInput → Locking
 └─ PieceReachedFloor → Locking
Locking
 └─ LockDelay expired → LineClear
LineClear
 ├─ LinesCleared [score + level update] → Spawning
 └─ NoLines → Spawning
Spawning [stack too high]
 └─ TopOut → GameOver
GameOver
 └─ (terminal state)
```

## HUD
- Score: top-left.
- Level: top-center.
- Lines cleared: top-right.
- Next piece preview: right side, upper.
- Hold piece: right side, lower.
- No timer — this is a survival game.
