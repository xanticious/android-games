# Sokoban — Design Document

## Overview
- Sokoban is a single-player warehouse puzzle. The player walks a worker around a grid of walls, floor, crates, and goal spots, pushing crates onto the goals.
- Crates can only be **pushed** (never pulled) and only one at a time; a crate cannot be pushed into a wall or another crate.
- A level is solved when every goal spot has a crate on it.
- Levels can become unsolvable (e.g. a crate pushed into a corner), so **Undo** is always available; there is no hard "lose" — the player undoes or restarts.
- Fully offline, single device, local stats only.
- Shared conventions: see [`common/puzzle-grid-board.md`](../../common/puzzle-grid-board.md), [`common/puzzle-controls.md`](../../common/puzzle-controls.md), [`common/puzzle-flow.md`](../../common/puzzle-flow.md).

## Settings
- **Level set**: Starter, Classic (default), Expert — curated, hand-verified solvable levels.
- **Difficulty filter**: by crate count / size.
- **Move style**: Step (tap adjacent, default) / Path (tap a destination, worker auto-walks).
- **Deadlock hint**: on (default) / off — gently warns when a crate reaches a known dead corner.

## Visual Style
- Material 3 surfaces, underwater palette from `ui/theme/Color.kt`.
- Walls `Dark2`, floor `Dark1`, goal spots marked with an `Aqua1` ring.
- Crates are `Aqua3` boxes; a crate sitting on a goal turns `Aqua2` (clearly "done").
- The worker is a bright token; pushes animate a short glide (board-area only).

## Screen Layout
```
┌─────────────────────────────────────┐
│  Sokoban     Lvl 7  Moves 35    ⚙ ? │
├─────────────────────────────────────┤
│   ███████                            │
│   █ . ◎ █     ◎ = goal               │
│   █ □ @ █     □ = crate, @ = worker  │  ← walls/floor/crates/goals
│   █ ◎ □ █                            │
│   ███████                            │
├─────────────────────────────────────┤
│  Undo  Reset        Crates 1/3       │
└─────────────────────────────────────┘
```

## How to Play
- Move the worker up/down/left/right.
- Walk into a crate to push it; you can't pull crates or push two at once.
- Get every crate onto a goal spot to finish the level.

## Controls
- **Tap** an orthogonally adjacent cell or **swipe** a direction: step the worker, pushing a crate if one is ahead (Step mode). See [`common/puzzle-controls.md`](../../common/puzzle-controls.md).
- **Tap** a reachable floor cell (Path mode): the worker auto-walks there (without pushing).
- **Undo**: revert the last move/push (unlimited).
- **Reset**: restart the current level.

## Gameplay Rules
- The worker moves into floor/goal cells; moving toward a crate pushes it one cell if the cell beyond is empty floor/goal.
- Crates never pass through walls or other crates and are never pulled.
- Solved = all goals covered by crates.
- Move/push legality, reachability (for Path mode), deadlock detection, and solved-check are pure functions in `controller/`, unit-tested.

## State Machine
A dedicated `SokobanStateMachine` in `state/` exposes `StateFlow<SokobanState>`.
```
Idle
 └─ StartLevel → Playing
Playing
 ├─ Moved → Playing (worker/crate updated, moves++)
 ├─ UndoRequested → Playing
 ├─ ResetRequested → Playing (level restored)
 └─ AllGoalsCovered → Solved
Solved
 └─ NextLevel / NewGame → Playing
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Levels solved (per set) | yes |
| Best moves / best pushes (per level) | yes |
| Levels solved without reset | yes |
| Set completion % | yes |

## HUD
- Top bar: title, level, move count, settings, help.
- Bottom: Undo, Reset, crates-on-goal status.
- Solve message appears below the board; the completed level stays visible, then offers Next.
