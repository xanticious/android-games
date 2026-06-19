# Jigsaw — Design Document

## Overview
- Jigsaw is a relaxing single-player puzzle: a photo is cut into interlocking pieces and the player rebuilds it on the board.
- Pieces start in a **scrollable piece bank** along the bottom of the screen. The player drags a piece onto the board; it snaps into place **only at its correct location**. Pieces **cannot be rotated**.
- There is no timer pressure and no lose condition — the puzzle is complete when all pieces are placed.
- Fully offline, single device, local stats only. Images ship with the app; no network is used at play time.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Image Collection (50 images)
- The game bundles **50 curated "cute" images** sourced from Unsplash under the royalty-free [Unsplash License](https://unsplash.com/license) (free for commercial and non-commercial use, no permission needed).
- Suggested category mix (≈10 each): **Animals** (kittens, puppies, ducklings), **Scenery** (beaches, forests, mountains), **Animated/illustrated Dragons** (friendly cartoon dragons), **Food/Treats** (cupcakes, fruit), **Aquatic** (fish, coral, sea life — fits the underwater palette).
- Assets live under `app/src/main/res/` (or a packaged assets folder). Each image has:
  - a high-res source (for the board) and a small thumbnail (for selection),
  - a manifest entry: `id`, `title`, `category`, `photographer`, `source URL`.
- An **Attribution / Credits** screen (reachable from Settings) lists each photographer and links per the Unsplash License courtesy guidance. The image manifest is a plain data file in `model/`.
- Images are validated to be landscape-or-square and high enough resolution for the largest piece count without visible blur.

## Settings
- **Image**: pick from the 50-image gallery (thumbnail grid), or "Surprise me" (random).
- **Piece count / difficulty**: 12, 35 (default), 60, 99, 150 — the controller chooses a near-square rows×cols factorization closest to the image aspect ratio.
- **Edge assist**: on (default) / off — when on, the four corner pieces are pre-placed to anchor the picture.
- **Show reference image**: on (default) / off — a faint full image is shown behind the board as a guide.

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt` for chrome; the photo itself provides the board content.
- Board area: the assembled region shows the photo; empty slots show the faint reference (if enabled) over a `Dark1` backing.
- Piece bank: a horizontally scrollable strip on a `Dark0` surface; each piece is a small rounded thumbnail of its cut shape.
- Classic interlocking piece silhouette (tabs and blanks) generated procedurally so edges visually mate.
- Snap feedback: when a dragged piece is near its home, it gently magnetizes in with a short ≤150 ms ease and a soft glow (`Aqua2`); the bank entry is removed.
- Reject feedback: dropping a piece anywhere other than its home cell makes it ease back to the bank — no error flash, just a gentle return.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Jigsaw      18 / 35 placed   ⚙ ?   │  ← Top bar (progress, settings, help)
├─────────────────────────────────────┤
│                                     │
│         ASSEMBLED PHOTO BOARD       │  ← Board with faint reference behind gaps
│         (placed pieces locked)      │
│                                     │
├─────────────────────────────────────┤
│  ◀ [▤][▤][▤][▤][▤][▤][▤] ▶          │  ← Scrollable piece bank (drag a piece up)
└─────────────────────────────────────┘
```

## How to Play
- Drag a piece from the bank up onto the board.
- Each piece only locks into its single correct position; you can't place it wrongly, so just find where it belongs.
- Pieces are never rotated — every piece is already in its correct orientation.
- Keep going until all pieces are placed and the picture is whole.

## Controls
- **Drag** a piece from the bank to the board: if released over (or near) its correct cell, it snaps and locks; otherwise it returns to the bank. See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).
- **Scroll** the piece bank horizontally to browse remaining pieces.
- **Tap** a piece in the bank: briefly highlights its correct region outline on the board (gentle hint; counts as a hint in stats).
- **Pinch / drag on the board**: optional zoom & pan for high piece counts (board content only).

## Gameplay Rules
- Placement is correct **iff** the piece's home cell matches the drop location within a snap tolerance; only then does it lock. Wrong drops never attach (the "prevent wrong spot" rule).
- Locked pieces stay fixed; they cannot be picked back up (no need, since placement is always correct).
- The puzzle is solved when the bank is empty / all pieces are locked.

## State Machine
A dedicated `JigsawStateMachine` in `state/` exposes `StateFlow<JigsawState>`.
```
Idle
 └─ StartGame → Playing (image cut into pieces, bank filled)
Playing
 ├─ PieceDragged → Dragging
 └─ AllPiecesPlaced → Solved
Dragging
 ├─ DroppedOnCorrectCell → Playing (piece locked, removed from bank)
 └─ DroppedElsewhere → Playing (piece returns to bank)
Solved
 └─ NewGame → Playing
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Puzzles completed (count) | yes |
| Completed per image (gallery progress) | yes |
| Best time (per piece count) | yes |
| Largest piece count completed | yes |
| Hint taps used (per puzzle) | yes |

## HUD
- Top bar: title, "placed / total" progress, settings, help.
- Bottom: scrollable piece bank with left/right affordances.
- Completion: a celebratory full-screen shimmer plays around the finished photo without covering it, and a "Complete!" line with time appears below the board.
