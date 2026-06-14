# Dot Art — Design Document

## Overview
- Dot Art is a three-phase creative sandbox. There are no win conditions, no timers, and no score.
- **Phase 1 — Connect**: The player is shown a canvas with a set of dots placed randomly. They draw lines between dots to form any graph they like. Every dot must have at least one connection before the player can advance, but the graph does not need to be fully connected.
- **Phase 2 — Fill**: The connected graph partitions the canvas into regions (both enclosed and open). The player uses a paint-bucket tool to flood-fill any region with one of 12 available colors.
- **Phase 3 — Draw**: The player adds freehand brush strokes on top of the filled canvas using the same 12 colors, plus an eraser.
- After Phase 3 the player taps Done and sees their finished piece full-screen with an option to start a new canvas or return to the lobby.
- All artwork stays local to the device and is never shared.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Canvas background: crisp white or an optional cream/dark paper tone chosen in settings.
- Dots: solid filled circles in `Aqua3`, radius ~10 dp, with a subtle drop shadow.
- Lines drawn in Phase 1: 3 dp strokes in `Dark2`; selected/active endpoint highlighted with `Aqua2` glow.
- Phase 2 fill colors: 12 vibrant swatches drawn from the Open Color underwater palette — one row of 12 chips below the canvas.
- Phase 3 brush strokes: semi-transparent (80% opacity) to let fills show through and blend naturally.
- Eraser: renders as a lighter-colored path over the stroke layer.
- All tool icons use `Aqua4` on a `Dark1` chip so the palette never blends into the canvas.

## Screen Layout

### Phase 1 — Connect
```
┌─────────────────────────────────────┐
│  Dot Art   Phase 1 / 3         ⚙   │  ← Top bar
├─────────────────────────────────────┤
│                                     │
│    ●         ●                      │
│         ●             ●             │  ← Canvas (dots placed randomly)
│    ●                  ●             │
│                  ●                  │
│                                     │
├─────────────────────────────────────┤
│  [Undo]              [Next →]        │  ← Action row (Next disabled until every dot connected)
└─────────────────────────────────────┘
```

### Phase 2 — Fill
```
┌─────────────────────────────────────┐
│  Dot Art   Phase 2 / 3         ⚙   │  ← Top bar
├─────────────────────────────────────┤
│                                     │
│    (graph of lines on canvas)       │  ← Canvas with graph from Phase 1
│                                     │
├─────────────────────────────────────┤
│  [■][■][■][■][■][■][■][■][■][■][■][■] │  ← Color palette chips (12 colors)
│  [Undo]              [Next →]        │  ← Action row
└─────────────────────────────────────┘
```

### Phase 3 — Draw
```
┌─────────────────────────────────────┐
│  Dot Art   Phase 3 / 3         ⚙   │  ← Top bar
├─────────────────────────────────────┤
│                                     │
│    (filled canvas)                  │  ← Canvas with fills from Phase 2
│                                     │
├─────────────────────────────────────┤
│  [■][■][■][■][■][■][■][■][■][■][■][■][ ⌫ ] │  ← Color palette + eraser
│  [Brush size ●  ●  ●]  [Undo] [Done]        │  ← Tool row
└─────────────────────────────────────┘
```

- Canvas occupies the majority of vertical space; control rows are compact.
- Portrait and landscape both supported; canvas scales to the shorter dimension to stay square.
- Tablets show a wider palette row with labeled color names on hover.

## Settings
- **Canvas size**: Small (15 dots), Medium (25 dots, default), Large (40 dots).
- **Paper tone**: White (default), Cream, Dark.
- **Grid snap** (on/off, default off): Phase 1 lines snap to the nearest dot within 24 dp.
- **Show region outlines in Phase 2** (on/off, default on): highlights region boundaries as the fill brush approaches them.
- **Brush opacity**: 40%, 60%, 80% (default), or 100%.

## How to Play

### Phase 1 — Connect the Dots
- The canvas shows dots scattered at random positions.
- Drag from any dot to any other dot to draw a straight line between them.
- A dot can be the source or target of any number of lines.
- Every dot must have at least one line before the Next button becomes active.
- Tap Undo to remove the most recently drawn line.
- Tap Next to lock the graph and move to Phase 2.

### Phase 2 — Fill Regions
- The canvas shows the graph from Phase 1 dividing it into regions.
- Tap a color chip to select it, then tap any region on the canvas to flood-fill it with that color.
- Tap the same region again with a different color to change the fill.
- Tap Undo to revert the most recent fill action.
- Tap Next to lock the fills and move to Phase 3.

### Phase 3 — Freehand Draw
- Drag a finger across the canvas to draw a freehand brush stroke in the selected color.
- Tap the eraser chip and drag to erase strokes.
- Adjust brush size with the three-size selector (fine, medium, thick).
- Tap Undo to remove the most recent stroke.
- Tap Done to finish the piece and see the final result full-screen.

## Controls
- **Drag** from dot to dot (Phase 1): draws a line.
- **Tap** a region (Phase 2): flood-fills with selected color.
- **Drag** on canvas (Phase 3): draws a brush stroke.
- **Tap** color chip (Phases 2 and 3): selects that color for the next action.
- **Tap** eraser chip (Phase 3): switches to erase mode.
- **Tap** Undo: reverts the last action in the current phase (max history: 50 actions per phase).
- **Tap** Next: advances to the next phase (disabled in Phase 1 until all dots are connected).
- **Tap** Done (Phase 3): shows the finished artwork full-screen.

## Phase Mechanics

### Phase 1 — Graph Construction
- Dots are placed using a Poisson-disk distribution to avoid clumping; minimum separation is 15% of the shorter canvas dimension.
- The number of dots is determined by the Canvas Size setting.
- A line can be drawn from any dot to any other dot, including lines that cross existing lines.
- Crossing lines create intersection points that Phase 2 treats as additional region boundaries.
- The "all dots connected" check is per-dot: each dot needs degree ≥ 1. The graph itself may be disconnected.
- The Next button shows a count badge (`3 dots not connected yet`) while the constraint is unmet.

### Phase 2 — Flood Fill
- Regions are computed from the graph edges and canvas boundary using a standard planar-graph face algorithm.
- Each region is stored as a polygon; fill colors are applied per-region, not per-pixel, so fills scale cleanly on all display densities.
- The canvas background counts as one fill-able region (the outside face).
- New lines from Phase 1 cannot be added in Phase 2.

### Phase 3 — Freehand Layer
- Strokes are stored as vector paths (lists of `(x, y)` points sampled at ~60 fps) so they scale without loss.
- Strokes are drawn on a separate layer above the fill layer; the eraser removes stroke data from that layer only (fills are never erased).
- Brush sizes: fine (~2 dp), medium (~6 dp, default), thick (~14 dp).

### Finished View
- After Done is tapped, the canvas fills the screen without toolbar or control rows.
- An overlay strip at the bottom shows two actions: **New Canvas** (starts a fresh Phase 1) and **Back to Menu**.
- The finished piece is not saved to device storage in this version.

## State Machine
- A dedicated `DotArtStateMachine` in `state/` exposes `StateFlow<DotArtState>`.
```
Idle
 └─ StartCanvas → Phase1Connect
Phase1Connect
 ├─ LineDraw → Phase1Connect (graph updated)
 ├─ LineUndo → Phase1Connect (last line removed)
 └─ Phase1Complete [all dots connected] → Phase2Fill
Phase2Fill
 ├─ RegionFilled → Phase2Fill (fill applied)
 ├─ FillUndo → Phase2Fill (last fill reverted)
 └─ Phase2Complete → Phase3Draw
Phase3Draw
 ├─ StrokeDrawn → Phase3Draw (stroke added)
 ├─ StrokeErased → Phase3Draw (stroke removed)
 ├─ StrokeUndo → Phase3Draw (last stroke removed)
 └─ DrawingDone → FinishedView
FinishedView
 ├─ NewCanvas → Phase1Connect
 └─ BackToMenu → Idle
```

## Stats (local)
| Stat | Stored |
|------|--------|
| Total canvases completed | yes |
| Total lines drawn (lifetime) | yes |
| Total regions filled (lifetime) | yes |
| Most lines in a single canvas | yes |

## HUD
- Top bar shows phase indicator (Phase 1 / 3, Phase 2 / 3, Phase 3 / 3) and settings access.
- Phase 1: dot-connection count badge on the Next button when dots remain unconnected.
- Phase 2: selected color chip is highlighted with a ring; no score display.
- Phase 3: active color chip and brush-size indicator are visually highlighted.
- No timer, no score, and no progress toward any objective is shown at any time.
