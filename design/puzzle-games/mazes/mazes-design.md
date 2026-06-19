# Mazes — Design Document

## Overview
- Mazes is a single-player navigation puzzle. The game procedurally generates a maze; the player guides an avatar from the **start** cell to the **goal** cell.
- Mazes scale in size and complexity. The player can use a top-down view or an optional first-person view.
- There is no failure state — the maze is simply complete when the goal is reached; an optional move/timer tracks performance.
- Fully offline, single device, local stats only.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Settings
- **Size**: Small (11×11), Medium (21×21, default), Large (31×31), Huge (45×45).
- **View**: Top-down (default) / First-person.
- **Generation algorithm**: Recursive Backtracker (default, long winding corridors), Prim's (bushy), Braid (multiple loops / no dead ends).
- **Breadcrumbs**: on (default) / off — trail showing where you've been.
- **Show solution**: a reveal toggle (counts as a hint in stats).

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt`.
- Walls `Dark2` on a `Dark1` floor; start marked `Aqua1`, goal marked `Aqua2`.
- Avatar: a small bright token; breadcrumb trail in a faded `Aqua3`.
- First-person: simple raycast-style corridor walls using the same tokens, with a mini-map inset.
- Reaching the goal triggers a brief path-glow from start to goal (board-area only).

## Screen Layout
```
┌─────────────────────────────────────┐
│  Mazes        00:34   Moves 58  ⚙ ? │
├─────────────────────────────────────┤
│   ┌─┬───┬─────┐                     │
│   │ │   │ ◎   │   ◎ = goal           │  ← top-down maze, avatar (●)
│   │ └─┐ │ ┌─┐ │                     │
│   │ ● │   │ │ │                     │
│   └───┴───┴─┴─┘                     │
├─────────────────────────────────────┤
│   (swipe to move)      Solve: off   │
└─────────────────────────────────────┘
```

## How to Play
- Move your avatar through open corridors toward the goal.
- Walls block movement; plan around dead ends.
- Reach the goal to complete the maze; try to minimize moves and time.

## Controls
- **Swipe** up/down/left/right (top-down): move the avatar one corridor step (continues until a wall/junction if "glide" is desired). See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).
- **Drag**: trace a continuous path the avatar follows along open corridors.
- First-person: swipe left/right to turn, swipe up to move forward.

## Gameplay Rules
- The avatar may move only between cells not separated by a wall.
- The maze is guaranteed to have a path from start to goal (perfect maze for backtracker/Prim's; braid mazes add loops but remain solvable).
- Completion = avatar occupies the goal cell.
- Maze generation and the shortest-path solver are pure functions in `controller/`, unit-tested for connectivity and solvability.

## State Machine
A dedicated `MazeStateMachine` in `state/` exposes `StateFlow<MazeState>`.
```
Idle
 └─ StartGame → Playing (maze generated)
Playing
 ├─ Moved → Playing (position updated)
 ├─ SolutionRevealed → Playing (hint flagged)
 └─ ReachedGoal → Solved
Solved
 └─ NewGame → Playing
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Mazes completed (per size) | yes |
| Best time (per size) | yes |
| Fewest moves vs shortest path (per size) | yes |
| No-reveal completions | yes |

## HUD
- Top bar: title, timer, move count, settings, help.
- Bottom: control hint and the solution-reveal toggle.
- Completion message appears below the maze; the solved maze and traced path stay visible.
