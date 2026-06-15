# Pirate Treasure Maze — Design Document

## Overview
- Pirate Treasure Maze is a single-player maze-navigation game where the player guides a pirate through a procedurally generated top-down maze, finding and opening every treasure chest before exiting through the gangplank goal.
- Mazes are generated using a variety of algorithms (see Maze Algorithms below), each producing a different structural character that rewards different navigation strategies.
- A count-up timer starts when the maze appears and stops when the last chest is opened and the pirate reaches the exit. The player's score is based on how close their actual time was to the theoretical optimal solve time computed for that maze.
- There are no lives and no fail states — the only challenge is navigating efficiently.
- The game is fully offline, single-device, and local-stat only.

## Visual Style
- Material 3 surfaces using the underwater palette from `ui/theme/Color.kt`.
- Top-down maze view with a nautical theme: plank-wood walls, sandy stone floors, barnacle textures.
- Pirate sprite: a small top-down character with a tricorn hat that rotates to face the direction of travel.
- Treasure chests: ornate locked-chest icons that snap open with a brief sparkle when the pirate steps on them.
- Exit tile: a rickety gangplank with a glowing `Aqua2` halo; it is locked (red) until all chests are opened, then unlocked (gold).
- Timer display: digital count-up in the top bar with a subtle `Aqua3` glow.
- Score card: appears below the maze after exit — never overlays the maze itself.
- Maze algorithm label shown in the top bar so the player knows what type they are navigating.

## Screen Layout
```
┌─────────────────────────────────────┐
│  Pirate Treasure   ⏱ 0:42  [Algo]⚙ │  ← Top bar (timer, algorithm tag, settings)
├─────────────────────────────────────┤
│                                     │
│   ╔═╦═══╦═╗                         │
│   ║P║   ║ ║  🪙     🪙             │  ← Maze (scrollable for large sizes)
│   ║ ╚═╗ ║ ║                         │
│   ║   ║ ╚═╝                         │
│   ╚═══╩═⚓╝  ← gangplank exit        │
│                                     │
├─────────────────────────────────────┤
│  [←][↑][↓][→]   (or swipe)         │  ← D-pad controls (toggleable)
└─────────────────────────────────────┘
```
- On small mazes the entire maze fits the screen; large mazes scroll as the pirate approaches the edge.
- Tablets show the full maze at once for small/medium sizes.
- The score card appears below the maze after completion; it never covers the maze.

## Settings
- **Maze size**: Small (10×10), Medium (16×16, default), Large (24×24).
- **Chest count**: 3, 5 (default), 8.
- **Control style**: D-pad buttons (default), Swipe gestures.
- **Show minimap** (on/off, default off): a corner minimap showing explored cells.
- **Algorithm selection**: Random (default), or a specific algorithm from the list below.

## How to Play
- Navigate the pirate using the d-pad or swipe gestures.
- Walk over every treasure chest to open it (no extra action required — stepping on a chest opens it).
- Once all chests are opened, the gangplank exit glows gold and becomes passable.
- Walk onto the exit tile to end the run. The timer stops and your score is calculated.
- Try to beat your best time and best efficiency score on each maze.

## Controls
- **Tap** d-pad arrows (or swipe in that direction): moves the pirate one tile in that direction.
- Movement through walls is blocked silently.
- **Tap** minimap toggle (top bar): shows/hides the corner minimap if enabled in settings.
- After completing the maze, tap **New Maze** or **Menu** from the score card below.

## Maze Algorithms

Each algorithm produces a structurally distinct maze with different navigation characteristics. The algorithm tag shown in the top bar gives the player context for what kind of maze they are in.

| Algorithm | Tag | Character |
|-----------|-----|-----------|
| Randomized Depth-First Search (Recursive Backtracker) | **Winding** | Long, winding corridors; few dead ends; strong main path |
| Prim's Algorithm | **Organic** | Sprawling, tree-like; many short dead ends; open feel |
| Kruskal's Algorithm | **Tangled** | Uniform random texture; no obvious corridors; unpredictable |
| Binary Tree | **Diagonal** | Consistent NE bias; long diagonal corridors; easy to exploit |
| Sidewinder | **Striped** | Horizontal corridors with random vertical breaks; row-by-row feel |
| Aldous-Broder | **Dense** | Maximally random; very high dead-end density; uniform difficulty |
| Recursive Division | **Chambered** | Large rooms divided by walls with narrow openings; room-to-room strategy |
| Wilson's Algorithm | **Sparse** | Uniform spanning tree; no bias; balanced dead ends |

The **Random** setting picks an algorithm from the list uniformly at random each run.

## Optimal Time Calculation
- After maze generation, a BFS / shortest-path computation finds the minimum number of moves required to collect all chests and reach the exit (treating chest-collection order as part of the state).
- This optimal move count is stored with the maze. Optimal time is calculated as: `optimalMoves × avgMoveTime` where `avgMoveTime` is a fixed constant (0.35 seconds per move, calibrated to comfortable navigation pace).
- **Efficiency score** = `(optimalTime / actualTime) × 100`, capped at 100. A score of 100 means the player matched or beat the theoretical optimal; lower scores reflect detours and dead-end explorations.
- Efficiency score and optimal time are displayed on the score card along with the player's raw time and personal best.

## State Machine
- A dedicated `PirateTreasureMazeStateMachine` in `state/` exposes `StateFlow<PirateMazeState>`.
```
Idle
 └─ StartGame → Generating
Generating
 └─ MazeReady → Playing
Playing
 ├─ MoveMade [valid, open cell] → Playing (position updated)
 ├─ MoveMade [valid, chest tile] → Playing (chest opened, counter updated)
 ├─ MoveMade [valid, exit tile, chests remaining] → Playing (exit locked, move ignored)
 ├─ MoveMade [valid, exit tile, all chests open] → MazeComplete
 └─ MoveMade [invalid] → Playing (no change)
MazeComplete
 ├─ NewMaze → Generating
 └─ BackToMenu → Idle
```

## Scoring & Stats (local)
| Stat | Stored |
|------|--------|
| Mazes completed (per size, per algorithm) | yes |
| Best raw time per (size, algorithm) pair | yes |
| Best efficiency score per (size, algorithm) pair | yes |
| Total chests opened (lifetime) | yes |
| Perfect efficiency runs (score = 100) | yes |

## HUD
- Top bar: title, count-up timer, algorithm tag, settings.
- Maze area: pirate sprite, chest sprites (locked/open), exit tile (locked/unlocked).
- Chest counter: `opened / total` shown below the top bar.
- Minimap (if enabled): small overlay in a corner showing explored cells.
- Score card below the maze after completion: raw time, optimal time, efficiency score, personal bests, algorithm used. Never overlays the maze.
